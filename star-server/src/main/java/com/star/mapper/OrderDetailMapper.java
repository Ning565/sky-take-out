package com.star.mapper;

import com.star.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    void insertBatch(List<OrderDetail> orderDetails);

    @Select("select * from star_food_chain.order_detail where order_id  = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
