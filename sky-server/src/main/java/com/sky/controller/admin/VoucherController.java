package com.sky.controller.admin;


import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.VoucherDTO;
import com.sky.dto.VoucherPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.Voucher;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.IVoucherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/voucher")
@Slf4j
@Api(tags = "管理端-优惠券相关接口")
public class VoucherController {
    @Autowired
    private IVoucherService voucherService;
    /**
     * 添加普通优惠券，返回优惠券ID
     * @param voucherDTO
     * @return
     */
    @PostMapping
    @ApiOperation("添加优惠券")
    public Result insertVoucher(@RequestBody VoucherDTO voucherDTO){
        log.info("添加普通优惠券:{}",voucherDTO);
        // DTO对象转为PO对象，直接利用mp添加新的
        Voucher voucher = new Voucher();
        BeanUtils.copyProperties(voucherDTO,voucher);
        voucher.setCreateTime(LocalDateTime.now());
        voucher.setUpdateTime(LocalDateTime.now());
        // 直接mybatis-plus调用IService的save方法
        voucherService.save(voucher);
        return Result.success(voucherDTO.getId());
    }

    /**
     * 添加秒杀券
     * @param voucherDTO
     * @return
     */
    @PostMapping("/seckill")
    @ApiOperation("添加秒杀券")
    public Result insertVoucherSeckill(@RequestBody VoucherDTO voucherDTO){
        log.info("Received request to add seckill voucher");
        log.info("添加秒杀优惠券:{}",voucherDTO);
        Long voucherId = voucherService.saveSeckill(voucherDTO);
        return Result.success(voucherId);
    }

    /**
     * 根据传入的ids批量删除优惠券
     * @param ids
     * @return
     */
    @DeleteMapping("/delete")
    @ApiOperation("删除优惠券")
    public Result deleteVoucher(@RequestParam List<Long> ids){
        log.info("删除优惠券ids:{}",ids);
        voucherService.deleteByIds(ids);
        return Result.success();
    }
    /**
     * 编辑优惠券起售状态
     */
    @PostMapping("/status/{status}")
    @ApiOperation("修改优惠券状态")
    public Result updateStatus(@PathVariable Integer status,Long id){
        log.info("按照ID修改优惠券状态，ID:{}",id);
        voucherService.updateStatusById(status,id);
        return Result.success();
    }

    /**
     * 在未引入分页插件的情况下， MybatisPlus 是不支持分页功能的，IService 和 BaseMapper 中的分页方法都无法生效
     *
     * @param voucherPageQueryDTO
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("分页批量查询")
    public Result<PageResult> queryVoucherList(VoucherPageQueryDTO voucherPageQueryDTO){
        log.info("分页查询优惠券:Page:{}，PageSize:{}",voucherPageQueryDTO.getPage(),voucherPageQueryDTO.getPageSize());
        PageResult pageResult = voucherService.pageQuery(voucherPageQueryDTO);
        return Result.success(pageResult);
    }
}
