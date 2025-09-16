package com.star.mapper;

import com.github.pagehelper.Page;
import com.star.annotation.AutoFill;
import com.star.dto.DishDTO;
import com.star.dto.DishPageQueryDTO;
import com.star.entity.Dish;
import com.star.enumeration.OperationType;
import com.star.vo.DishVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from star_food_chain.dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品，并且主键回显得到当前插入的菜品id
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    @Options(useGeneratedKeys = true,keyProperty = "id")
    @Insert("insert into star_food_chain.dish (name, category_id, price, image, description, create_time, update_time, create_user, update_user, status) values " +
            "(#{name}, #{categoryId}, #{price}, #{image}, #{description}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser}, #{status})")
    void insert(Dish dish);

    /**
     * 分页查询
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    @Select("select * from star_food_chain.dish where id = #{id} ")
    Dish getById(Long id);

    void deleteByIds(List<Long> ids);
    @AutoFill(OperationType.UPDATE)
    void update(Dish dish);

    @Select("select * from star_food_chain.dish where category_id = #{cateId}")
    List<Dish> getByCateId(Long cateId);


    List<Dish> list(Dish dish);

    // 按照套餐ID查询套餐所关联的菜品
    @Select("select  d.* from star_food_chain.dish as d left join star_food_chain.setmeal_dish as s " +
            "on d.id =s.dish_id where s.setmeal_id = #{setmealId}")
    List<Dish> getBySetmealId(Long setmealId);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
