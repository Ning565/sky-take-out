package com.star.mapper;

import com.github.pagehelper.Page;
import com.star.annotation.AutoFill;
import com.star.entity.Setmeal;
import com.star.enumeration.OperationType;
import com.star.vo.DishItemVO;
import com.star.vo.SetmealVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from star_food_chain.setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    @Insert("    insert into star_food_chain.setmeal" +
            "    (category_id, name, price, status, description, image)" +
            "    values (#{categoryId}, #{name}, #{price}, #{status}, #{description}, #{image})")
    @Options(useGeneratedKeys = true,keyProperty = "id")
    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    Page<SetmealVO> pageQuery(Setmeal setmeal);

    @Select("select * from star_food_chain.setmeal where id = #{id};")
    Setmeal getById(Long id);

    void deleteByIds(List<Long> setmealIds);

    void update(Setmeal setmeal);

    /**
     * 动态条件查询套餐
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据套餐id查询菜品选项
     * @param setmealId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from star_food_chain.setmeal_dish as sd left join star_food_chain.dish as d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);

    /**
     * 根据条件统计套餐数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

}
