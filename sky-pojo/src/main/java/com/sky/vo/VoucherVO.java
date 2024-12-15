package com.sky.vo;


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
public class VoucherVO implements Serializable {
    // 优惠券标题
    private String title;
    // 副标题
    private String subTitle;
    // 使用规则
    private String rules;
    // 支付金额
    private BigDecimal payValue;
    // 抵扣金额
    private BigDecimal actualValue;
    // 劵类型,
    private Integer type;
    // 优惠券状态
    private String status;
}
