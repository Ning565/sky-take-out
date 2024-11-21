package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面类(Aspect)，实现公共字段自动填充逻辑
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     *  指定切入点和通知
     *  组合切点表达式,更加灵活和精准,只匹配execution扫描范围内使用AutoFill注解的，进行拦截
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){

    }
    /**
     * 连接点，用于处理业务逻辑
     * @param joinPoint
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("公共字段自动填充开始执行");
        // 1.获取当前被拦截的方法上的数据库操作类型 insert / update
        // 方法签名对象，Signature转型为MethodSignature S:提供的接口一般性，可以获得方法名，无法获得方法参数/返回类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取操作类型
        AutoFill autofill = signature.getMethod().getAnnotation(AutoFill.class); // 获取方法上的注解对象
        OperationType type = autofill.value(); // 数据库操作类型
        // 2.获取被拦截方法参数，实体对象Emp
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        // 约定实体类型（Employee等，TODOO 后期可能是分类实体等）放在第一位
        Object entity = args[0];
        // 3.为实体对象公共属性通过"反射"赋值
        LocalDateTime localDateTime = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();
        // getClass获取当前对象 entity 的运行时类  getDeclaredMethod 动态获取类中名称，方法类型的方法，最终setCreateTime等保存了这些方法
        // 实体 -> 获取实体当前类 -> 获取类中方法 -> invoke反射,传参动态调用该方法
        try {
            if (type == OperationType.INSERT){
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                // invoke反射调用这些方法 对象，参数
                setCreateTime.invoke(entity,localDateTime);
                setUpdateTime.invoke(entity,localDateTime);
                setUpdateUser.invoke(entity,currentId);
                setCreateUser.invoke(entity,currentId);
            }else if (type == OperationType.UPDATE){
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateTime.invoke(entity,localDateTime);
                setUpdateUser.invoke(entity,currentId);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
