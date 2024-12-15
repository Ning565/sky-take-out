package com.sky.entity;


import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName(value = "voucher_seckill",schema = "sky_take_out")
public class VoucherSeckill implements Serializable {
    /**
     * 主键即为优惠券ID
     */
    private Long voucherId;
    // 库存
    private Integer stock;

    // 创建时间
    private LocalDateTime createTime;
    // 更新时间
    private LocalDateTime updateTime;

    // 券的开始时间
    private LocalDateTime beginTime;
    // 券的结束时间
    private LocalDateTime endTime;
}
