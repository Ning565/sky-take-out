package com.star.service;

import com.star.dto.*;
import com.star.result.PageResult;
import com.star.vo.OrderPaymentVO;
import com.star.vo.OrderStatisticsVO;
import com.star.vo.OrderSubmitVO;
import com.star.vo.OrderVO;

public interface OrderService {

    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);
    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    PageResult page(int page,int pageSize,Integer status);

    OrderVO getDetailsById(Long id);

    void cancel(Long id);

    void repetiton(Long id);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO countStatusNum();

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    void cancelAdmin(OrdersCancelDTO ordersCancelDTO);
    void delivery(Long id);

    void complete(Long id);

    Long getOrderIdByNum(String orderNumber);

    void reminder(Long id);
}
