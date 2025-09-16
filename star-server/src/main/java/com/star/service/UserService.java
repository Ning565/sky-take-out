package com.star.service;

import com.star.dto.UserLoginDTO;
import com.star.entity.User;

public interface UserService {

    User wechatLogin(UserLoginDTO userLoginDTO);
}
