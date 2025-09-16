package com.star.controller.user;


import com.alibaba.fastjson.JSON;
import com.star.dto.OrdersDTO;
import com.star.dto.OrdersPaymentDTO;
import com.star.dto.OrdersSubmitDTO;
import com.star.entity.Orders;
import com.star.result.PageResult;
import com.star.service.OrderService;
import com.star.vo.OrderPaymentVO;
import com.star.vo.OrderSubmitVO;
import com.star.vo.OrderVO;
import com.star.websocket.WebSocketServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import com.star.result.Result;

import java.util.HashMap;
import java.util.Map;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
@Api(tags = "C端订单操作相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */

    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        // 支付完成以后，通过websocket 向客户端浏览器推送消息(json:type/orderID/content)
        Map info  = new HashMap<>();
        info.put("type",1); // 1表示来电提醒 2表示客户催单
        // 获取orderID
        Long id = orderService.getOrderIdByNum(ordersPaymentDTO.getOrderNumber());
        info.put("orderId",id);
        info.put("content","订单号："+ordersPaymentDTO.getOrderNumber());

        String json = JSON.toJSONString(info);

        // 推送到客户端浏览器
        webSocketServer.sendToAllClient(json);
        return Result.success(orderPaymentVO);
    }

    /**
     * 分页查询历史订单
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> getHistoryOrders(int page,int pageSize,Integer status){
        log.info("查询历史订单");
        PageResult pageResult = orderService.page(page,pageSize,status);
        return Result.success(pageResult);
    }

    /**
     * 查看历史订单详情
     * @param id
     * @return
     */
    @GetMapping("orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id){
        log.info("查询历史订单：{}的详情信息",id);
        OrderVO orderVO = orderService.getDetailsById(id);
        return Result.success(orderVO);
    }

    /**
     * 取消订单
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable Long id){
        log.info("取消订单，订单id = {}",id);
        orderService.cancel(id);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result againOrder(@PathVariable Long id){
        log.info("再来一单，订单id = {}",id);
        orderService.repetiton(id);
        return Result.success();
    }

    @GetMapping("reminder/{id}")
    @ApiOperation("客户催单")
    public Result reminder(@PathVariable Long id){
        log.info("客户催单");
        orderService.reminder(id);
        return Result.success();
    }
}
