package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherPageQueryDTO implements Serializable {

    // 标题查询
    private String title;
    // 按照券的类型
    private Integer type;
    // 按照券的状态查询
    private Integer status;
    // 页码
    private int page;

    // 每页显示记录数
    private int pageSize;

}
