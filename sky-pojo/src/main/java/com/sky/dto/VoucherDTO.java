package com.sky.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
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
public class VoucherDTO implements Serializable {
    /**
     * 主键
     */
    private Long id;
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
    // 劵类型 1:普通 2：秒杀
    private Integer type;
    // 优惠券状态 1：起售 2：停售 3：即将发行
    private String status;
    // 库存
    private Integer stock;
    // 券的开始时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime beginTime;
    // 券的结束时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
}
