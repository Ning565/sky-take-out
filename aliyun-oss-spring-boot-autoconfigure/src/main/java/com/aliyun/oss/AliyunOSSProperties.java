package com.aliyun.oss;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "sky.aliyun.oss")
public class AliyunOSSProperties {
    private String endpoint;
    private String bucketName;
}
