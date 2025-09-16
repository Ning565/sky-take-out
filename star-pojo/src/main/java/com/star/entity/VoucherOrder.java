package com.star.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherOrder implements Serializable {
    /**
     * 主键
     */
    private Long id;
    // 用户ID
    private Long userId;
    // 优惠券ID
    private Long voucherId;
    // 使用规则
    private String rules;
    // 支付方式 1：余额支付；2：支付宝；3：微信
    private Integer payType;
    // 订单状态：1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
    private Integer status;
    // 创建时间
    private LocalDateTime createTime;
    // 支付时间
    private LocalDateTime payTime;
    // 使用时间
    private LocalDateTime useTime;
    // 退款时间
    private LocalDateTime refundTime;
    // 更新时间
    private LocalDateTime updateTime;
}
