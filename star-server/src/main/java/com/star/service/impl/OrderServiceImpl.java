package com.star.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.star.constant.MessageConstant;
import com.star.context.BaseContext;
import com.star.dto.*;
import com.star.entity.*;
import com.star.exception.AddressBookBusinessException;
import com.star.exception.OrderBusinessException;
import com.star.exception.ShoppingCartBusinessException;
import com.star.mapper.*;
import com.star.result.PageResult;
import com.star.service.OrderService;
import com.star.utils.WeChatPayUtil;
import com.star.vo.OrderPaymentVO;
import com.star.vo.OrderStatisticsVO;
import com.star.vo.OrderSubmitVO;
import com.star.vo.OrderVO;
import com.star.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;
    /**
     * 提交订单信息
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        // 1.判断可行性：购物车和地址是否为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (CollectionUtils.isEmpty(list)){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 地址
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if (addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 2.为order表添加数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        // 补充基本信息：订单状态，支付状态，订单号，下单用户ID，下单时间，手机号，地址，收货人
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        // 插入新增数据
        orderMapper.insert(order);
        // 3.为order_detail添加数据 ,orderdetail对象基本与购物车相同，一对多关系，批量插入
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart index: list){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(index,orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        // 4.清空购物车数据
        shoppingCartMapper.deletByUserId(userId);
        // 5.封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().
                id(order.getId()).orderNumber(order.getNumber()).orderAmount(order.getAmount()).orderTime(order.getOrderTime()).
                build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */

    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        // 为替代微信支付成功后的数据订单状态更新，多定义一个方法进行 修改
        Integer orderPaidStatus = Orders.PAID;
        Integer orderStatus = Orders.TO_BE_CONFIRMED;
        // 为支付时间赋值
        LocalDateTime check_out_time = LocalDateTime.now();
        // 获取订单号
        String orderNum = ordersPaymentDTO.getOrderNumber();
        log.info("调用updateStatus,更新为支付后数据库状态");
        Orders order = Orders.builder().status(orderStatus).payStatus(orderPaidStatus).checkoutTime(check_out_time).number(orderNum).build();
        orderMapper.updateStatus(order);
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 查询历史订单
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult page(int page, int pageSize, Integer status) {
        // 1.PageHelper设置分页
        PageHelper.startPage(page, pageSize);
        // 2.分页条件查询，Mapper接受DTO对象，返回Page对象
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> ordersPage = orderMapper.pageQuery(ordersPageQueryDTO);

        // 3.因为查询到的每个订单包含很多菜品/套餐，每个菜品都是一个detail信息，将List<detail>存入该订单中
        // 封装成VO，返回给前端
        List<OrderVO> list = new ArrayList<>();
        // 4.对ordersPage对象的每个order，查询出订单明细，封装入VO响应
        if (!CollectionUtils.isEmpty(ordersPage)){
            for (Orders order : ordersPage){
                Long id = order.getId(); // 得到订单ID
                // new一个vo对象，进行封装返回
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order,orderVO); // 封装orderDishes
                // 对VO对象的List detail进行封装，根据订单ID查询详情信息
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        // 5.返回
        return new PageResult(ordersPage.getTotal(),list);
    }

    @Override
    public OrderVO getDetailsById(Long id) {
        // 1.按照订单id查询订单信息
        Orders order = orderMapper.getById(id);
        // 2.查询订单详情信息
        List<OrderDetail> list = orderDetailMapper.getByOrderId(id);
        // 3.封装成VO对象返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order,orderVO);
        orderVO.setOrderDetailList(list);

        return orderVO;
    }

    @Override
    public void cancel(Long id) {
        // 1.判断订单存在性和订单状态： 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        // 除1和2外均不能直接取消
        Orders order = orderMapper.getById(id);
        if (order == null) throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        Integer status = order.getStatus();
        if (status > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if (status == 2) {
            // 待接单状态下要退款，并且修改支付状态
//            weChatPayUtil.refund(
//                    order.getNumber(), //商户订单号
//                    order.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额
            order.setPayStatus(Orders.REFUND);
        }
        // 2.设置订单状态、取消原因、取消时间，更新数据库
        order.setStatus(Orders.CANCELLED);
        order.setCancelReason("用户取消");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    /**
     * 再来一单，为同样的用户再添加一个一模一样的订单
     * @param id
     */
    @Override
    public void repetiton(Long id) {
        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        // 1.获取当前订单detail信息
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        // 2.创建购物车list，将当前订单详情list中的菜品/套餐detail 挨个加入到购物车中
        List<ShoppingCart> shoppingCarts = new ArrayList<>();
        for (OrderDetail orderDetail :orderDetails){
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart,"id"); //需要排除id字段
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCarts.add(shoppingCart);
        }
        // 3.批量插入，更新购物车数据表
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    /**
     * 条件查询，分页查询订单信息
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        // 查询基本页面
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        // 部分订单状态，需要额外返回订单菜品信息，Orders没有菜品信息属性，OrderVO有，将Orders转化为OrderVO
        // 调用getOrderVOList取得结果,page.getResult是Orders对象的List
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(page.getTotal(),orderVOList);
    }

    /**
     * 计算各个状态的数量
     * @return
     */
    @Override
    public OrderStatisticsVO countStatusNum() {
       // 计算待派送数量
        Integer confirmedNum = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryNum = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer tobeConfirmNum = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        // 构建VO对象返回
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        // 为对象设置数量
        orderStatisticsVO.setConfirmed(confirmedNum);
        orderStatisticsVO.setToBeConfirmed(tobeConfirmNum);
        orderStatisticsVO.setDeliveryInProgress(deliveryNum);
        return orderStatisticsVO;
    }

    /**
     * 接单，本质上是修改订单状态为confirmed
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders order = Orders.builder().status(Orders.CONFIRMED).id(ordersConfirmDTO.getId()).build();
        orderMapper.update(order);
    }

    /**
     * 拒绝订单，与用户取消订单逻辑一样，本质都是设置状态为取消，增加原因
     * @param ordersRejectionDTO
     */
    @Override

    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 1.判断当前订单状态，只能是待接单的才能拒
        Long orderId = ordersRejectionDTO.getId();
        Orders order = orderMapper.getById(orderId);
        if (!(order.getStatus() == Orders.TO_BE_CONFIRMED) || order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 2.拒绝以后要退款
        Integer payStatus = order.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("拒单后退款....");
            order.setPayStatus(Orders.REFUND);
        }
        // 3.更新订单状态、取消原因、取消时间，并将对象传入数据库更新
        order.setCancelReason(ordersRejectionDTO.getRejectionReason());
        order.setCancelTime(LocalDateTime.now());
        order.setStatus(Orders.CANCELLED);
        orderMapper.update(order);
    }

    /**
     * 商家取消订单,只有在待派送和派送中)的才能取消
     */
    @Override
    public void cancelAdmin(OrdersCancelDTO ordersCancelDTO) {
        // 查询当前订单状态
        Orders order = orderMapper.getById(ordersCancelDTO.getId());
        if (order == null || (order.getStatus() != Orders.CONFIRMED && order.getStatus() != Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 退款
        if (order.getPayStatus() == Orders.PAID){
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("订单取消后退款....");
            order.setPayStatus(Orders.REFUND);
        }
        // 设置取消原因，时间，订单状态，并更新数据库
        order.setCancelReason(ordersCancelDTO.getCancelReason());
        order.setCancelTime(LocalDateTime.now());
        order.setStatus(Orders.CANCELLED);
        orderMapper.update(order);
    }

    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 根据page信息增加额外的Dishes信息
     * @param page
     * @return
     */
    public List<OrderVO> getOrderVOList(Page<Orders> page){
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> list = new ArrayList<>();
        List<Orders> orders = page.getResult();
        for (Orders order : orders){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order,orderVO);
            // 利用get函数得到String类型的orderDishes信息
            String orderDishes = getOrderDishesStr(order);
            // 将订单菜品信息封装到orderVO中，并添加到orderVOList
            orderVO.setOrderDishes(orderDishes);
            list.add(orderVO);
        }
        return list;
    }

    /**
     * 根据order对象获取dishes详细信息
     * @param order
     * @return
     */
    public String getOrderDishesStr(Orders order){
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());
        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；），list存储结果
        List<String> dishesStr = new ArrayList<>();
        for (OrderDetail orderDetail:orderDetailList){
            String dishStr = orderDetail.getName() + "*" + orderDetail.getNumber() + ";";
            dishesStr.add(dishStr);
        }
        // 将该订单对应的所有菜品信息拼接在一起: ""（空字符串），意味着不使用任何字符来分隔这些元素。
        return String.join("",dishesStr);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    @Override
    public Long getOrderIdByNum(String orderNumber) {
        return orderMapper.getByNumber(orderNumber).getId();
    }

    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.getById(id);
        // 校验订单是否存在
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 催单
        Map info = new HashMap<>();
        info.put("type",2); // 1来单提醒，2客户催单
        info.put("orderId",id);
        info.put("content","订单号："+orders.getNumber());
        String json = JSON.toJSONString(info);
        webSocketServer.sendToAllClient(json);
    }


}
