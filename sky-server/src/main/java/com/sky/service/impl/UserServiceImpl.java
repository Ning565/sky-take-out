package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    /**
     * 用户登录
     * @param userLoginDTO
     * @return
     */
    // 微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    // 微信配置属性类的数值
    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;
    @Override
    public User wechatLogin(UserLoginDTO userLoginDTO) {
        String openId = getOpenid(userLoginDTO.getCode());
        // 2.判断openid有效性
        if (openId == null ) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        // 3.判断用户是否为新用户（此ID是否在用户表中）
        User user = userMapper.getByOpenid(openId);
        // 4.新用户->完成注册，返回对象
        if (user == null ){
            user = User.builder()
                    .openid(openId)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        return user;
    }
    // 比较固定的方式获取OpenId
    private String getOpenid(String code){
        // 1.调用微信接口服务，获取当前用户openid
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("appid",weChatProperties.getAppid());
        paramMap.put("secret",weChatProperties.getSecret());
        paramMap.put("js_code",code); //这是用户登录时的暂时认证code，由DTO传入
        paramMap.put("grant_type","authorization_code");
        String response = HttpClientUtil.doGet(WX_LOGIN,paramMap); //返回的是json字符串

        JSONObject jsonObject = JSON.parseObject(response);
        String openId = jsonObject.getString("openid");
        return openId;
    }
}
