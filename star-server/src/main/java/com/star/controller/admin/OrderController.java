package com.star.controller.admin;

import com.star.dto.OrdersCancelDTO;
import com.star.dto.OrdersConfirmDTO;
import com.star.dto.OrdersPageQueryDTO;
import com.star.dto.OrdersRejectionDTO;
import com.star.mapper.OrderMapper;
import com.star.result.PageResult;
import com.star.result.Result;
import com.star.service.OrderService;
import com.star.vo.OrderStatisticsVO;
import com.star.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Api(tags = "订单管理接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单分页查询：{}",ordersPageQueryDTO);
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 获取不同订单状态的数量
     * @return
     */
    @ApiOperation("获取不同订单状态的数量")
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> getStatusNum(){
        log.info("获取不同订单状态的数量");
        OrderStatisticsVO orderStatisticsVO = orderService.countStatusNum();
        return Result.success(orderStatisticsVO);
    }

    /**
     * "获取订单详细信息"
     * @param id
     * @return
     */
    @GetMapping("details/{id}")
    @ApiOperation("获取订单详细信息")
    public Result<OrderVO> getDetail(@PathVariable Long id){
        log.info("获取订单详细信息");
        OrderVO orderVO = orderService.getDetailsById(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("商家接单，订单:{}",ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     * @return
     * @throws Exception
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        log.info("商家拒单，订单ID={}",ordersRejectionDTO.getId());
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        log.info("商家取消订单，订单={}",ordersCancelDTO);
        orderService.cancelAdmin(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     *
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable("id") Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     *
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable("id") Long id) {
        orderService.complete(id);
        return Result.success();
    }
}