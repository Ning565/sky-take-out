package com.sky.mapper;

import com.sky.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */

    @Select("select * from sky_take_out.user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入用户数据
     * @param user
     */
    @Insert("insert into sky_take_out.user (openid, name, phone, sex, id_number, avatar, create_time) " +
            "values (#{openid}, #{name}, #{phone}, #{sex}, #{idNumber}, #{avatar},#{createTime})")
    @Options(useGeneratedKeys = true,keyProperty = "id")
    void insert(User user);
    @Select("select * from sky_take_out.user where id = #{userId}")
    User getById(Long userId);

    Integer countByMap(Map map);
}
