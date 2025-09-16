package com.star.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DishTag {
    private Long id; // 标签ID
    private Long dishId; // 菜品ID
    private String tagName; // 标签名称
    private String tagType; // 标签类型
    private String tagValue; // 标签值
    private Double weight; // 标签权重
    private Double confidence; // 标签置信度
    private String source; // 标签来源
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
} 