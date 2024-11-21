package com.sky.controller.admin;

import com.aliyun.oss.AliyunOSSOperator;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {
        @Autowired
        private AliyunOSSOperator aliyunOSSOperator;
        @PostMapping("/upload")
        @ApiOperation("文件上传")
        public Result<String> upload(MultipartFile file) throws Exception {
            log.info("文件上传:{}",file);
            //上传文件到阿里云 OSS
            String filepath = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
            return Result.success(filepath);
    }
}
