package com.star.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "voucher",schema = "star_food_chain")
public class Voucher implements Serializable {
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
    // 劵类型
    private Integer type;
    // 优惠券状态
    private Integer status;
    // 创建时间
    private LocalDateTime createTime;
    // 更新时间
    private LocalDateTime updateTime;
}
