package com.star.annotation;

import com.star.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 创建一个注解，只能加在方法上，实现自动填充
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    // 指定当前数据库操作的类型 枚举类型的属性（insert update），自定义的在common/enum
    OperationType value();
}
