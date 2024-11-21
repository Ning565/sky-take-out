package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Api 用在类上，例如Controller，表示对类的说明
 * @ApiModel 用在类上，例如entity、DTO、VO
 * @ApiModelProperty 用在属性上，描述属性信息
 * @ApiOperation 用在方法上，例如Controller的方法，说明方法的用途、作用
 */
@Data
@ApiModel(description = "员工登录时传递的数据模型")
public class EmployeeLoginDTO implements Serializable {

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码")
    private String password;

}
