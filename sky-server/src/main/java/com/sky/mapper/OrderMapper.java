package com.sky.mapper;


import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("insert into sky_take_out.orders(number, status, user_id, address_book_id, order_time," +
            "checkout_time, pay_method, pay_status, amount, remark," +
            "phone, address, consignee, estimated_delivery_time," +
            "delivery_status, pack_amount, tableware_number," +
            "tableware_status) values " +
            " (#{number}, #{status}, #{userId}, #{addressBookId},#{orderTime}, " +
            "#{checkoutTime}, #{payMethod},#{payStatus}, #{amount}, #{remark}," +
            " #{phone}, #{address},#{consignee}, #{estimatedDeliveryTime}," +
            " #{deliveryStatus}, #{packAmount},#{tablewareNumber}, " +
            "#{tablewareStatus})")
    @Options(useGeneratedKeys = true,keyProperty = "id")
    void insert(Orders order);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from sky_take_out.orders where number = #{number}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    void updateStatus(Orders order);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from sky_take_out.orders where id = #{id}")
    Orders getById(Long id);

    @Select("select count(id) from sky_take_out.orders where status = #{status}") // 计算当前状态的订单数量(即id个数)
    Integer countStatus(Integer status);

    /**
     * 根据订单状态和下单时间查询订单
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from sky_take_out.orders where status = #{status} and order_time < #{orderTime} ;")
    List<Orders> getByStatusAndOrderTime(Integer status, LocalDateTime orderTime);
}
