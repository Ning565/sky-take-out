package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from sky_take_out.setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    @Insert("    insert into sky_take_out.setmeal" +
            "    (category_id, name, price, status, description, image)" +
            "    values (#{categoryId}, #{name}, #{price}, #{status}, #{description}, #{image})")
    @Options(useGeneratedKeys = true,keyProperty = "id")
    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    Page<SetmealVO> pageQuery(Setmeal setmeal);

    @Select("select * from sky_take_out.setmeal where id = #{id};")
    Setmeal getById(Long id);

    void deleteByIds(List<Long> setmealIds);

    void update(Setmeal setmeal);
}
