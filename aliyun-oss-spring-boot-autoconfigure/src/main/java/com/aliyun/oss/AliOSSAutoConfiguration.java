package com.aliyun.oss;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 新建配置类，声明 AliOSSOperator 的bean对象
 */
@Configuration
@EnableConfigurationProperties(AliyunOSSProperties.class)
public class AliOSSAutoConfiguration {
    @Bean
    public AliyunOSSOperator aliyunOSSOperator(AliyunOSSProperties aliyunOSSProperties){
            return new AliyunOSSOperator(aliyunOSSProperties);
    }

}
