package com.aliyun.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.AliyunOSSProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// AliyunOperator交给IOC容器管理（默认component的）
// component controller service repository(dao层的)

@Slf4j
public class AliyunOSSOperator {

    // private String endpoint = "https://oss-cn-beijing.aliyuncs.com";
    // private String bucketName = "web-tlias-ning";

/**
 *     通过value注解，引入application.yml中的属性
 *    @Value("${aliyun.oss.endpoint}")
 *    private String endpoint;
 *  @Value("${aliyun.oss.bucketName}")
 *    private String bucketName;
 *    value较多时，使用SpringBoot的@ConfigurationProperties(需要实体类,属性名和配置名一致)
 */

    private AliyunOSSProperties aliyunOSSProperties;

    public AliyunOSSOperator(AliyunOSSProperties aliyunOSSProperties) {
        this.aliyunOSSProperties = aliyunOSSProperties;
    }


    public String upload(byte[] content, String originalFilename) throws Exception {
        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketName();

        // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        // 填写Object完整路径，例如202406/1.png。Object完整路径中不能包含Bucket名称。
        //获取当前系统日期的字符串,格式为 yyyy/MM,使文件存储按照年/月的目录进行存储
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        //根据原始文件名originalFilename, 生成一个新的不重复的文件名
        String newFileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = dir + "/" + newFileName;

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, credentialsProvider);

        //文件上传
        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content));
        } catch (Exception e){
            log.error("文件上传失败:{}",e);
        }
        finally {
            if (ossClient != null) {

                ossClient.shutdown();
            }
        }
        //拼接文件访问路径
        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + objectName;
    }

}
