package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.CacheConstant;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.VoucherDTO;
import com.sky.dto.VoucherPageQueryDTO;
import com.sky.entity.Voucher;
import com.sky.entity.VoucherSeckill;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.VoucherMapper;
import com.sky.mapper.VoucherSeckillMapper;
import com.sky.result.PageResult;
import com.sky.service.IVoucherSeckillService;
import com.sky.service.IVoucherService;
import com.sky.vo.VoucherVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Constants;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {
    // 调用IVoucherSeckillService，专门处理VoucherSeckill类型的，SeckillService的不要实现类，因为没有什么业务逻辑需要实现
    //    @Autowired
    //    IVoucherSeckillService voucherSeckillService;
    // 现在采用Db实现，避免 Service 层之间的依赖，考虑使用 MyBatis-Plus 提供的 Db 类
    // Db通过传入的对象来找到对应的表，或者通过@TableName("voucher") 注解找到对应的表
    @Autowired
    VoucherMapper voucherMapper;
    @Autowired
    VoucherSeckillMapper voucherSeckillMapper;
    @Override
    @Transactional
    public Long saveSeckill(VoucherDTO voucherDTO) {
        Long voucherId = voucherDTO.getId();
        // 将优惠券信息存储到voucher表
        Voucher voucher = new Voucher();
        BeanUtils.copyProperties(voucherDTO, voucher);
        voucher.setCreateTime(LocalDateTime.now());
        voucher.setUpdateTime(LocalDateTime.now());
        //save(voucher);
        // 使用 Db 插入到 voucher 表, 这里传入的是 Voucher 实体类，Db 会根据 @TableName 注解将其映射到 voucher 表
        Db.save(voucher);
        // 提取出秒杀券信息，存储到voucher_seckill表
        VoucherSeckill voucherSeckill = VoucherSeckill.builder().
                 voucherId(voucherId).createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                 .beginTime(voucherDTO.getBeginTime()).endTime(voucherDTO.getEndTime()).stock(voucherDTO.getStock()).
                 build();
        //voucherSeckillService.save(voucherSeckill);
        Db.save(voucherSeckill);
        // 返回ID
        return voucherId;
    }

    @Override
    public void deleteByIds(List<Long> ids) {
     // 判断不能删的
        for (Long id : ids){
            Voucher voucher = getById(id);
            // 起售状态中的不能删除
            if (voucher.getStatus() ==  StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.VOUCHER_ON_SALE);
            }
        }
        // 按照IDS批量删除秒杀券表，再批量删除优惠券表，就算秒杀券没有也没事
        // 删除秒杀券数据，利用wrapper构造条件 id in ids
        // 删除 voucher_seckill 表中的数据
        LambdaUpdateWrapper<VoucherSeckill> seckillWrapper = new LambdaUpdateWrapper<>();
        seckillWrapper.in(VoucherSeckill::getVoucherId, ids); // 根据 VoucherId 构造条件
        voucherSeckillMapper.delete(seckillWrapper);

        // 删除 voucher 表中的数据
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.in(Voucher::getId, ids); // 根据 Id 构造条件
        voucherMapper.delete(voucherWrapper);
    }

    @Override
    public void updateStatusById(Integer status, Long id) {
        // 设置一个新的实体类，然后传入状态更新
        Voucher voucher  = Voucher.builder().id(id).status(status).build();
        // 更新条件复杂用wrapper，普通不用
        boolean b = updateById(voucher);
        if (b == false) throw new RuntimeException(MessageConstant.UPDATE_FAILED);
    }

    @Override
    public PageResult pageQuery(VoucherPageQueryDTO voucherPageQueryDTO) {
        // 标题查询,按照券的类型,按照券的状态查询,页码,每页显示记录数
        String title = voucherPageQueryDTO.getTitle();
        Integer type = voucherPageQueryDTO.getType();
        Integer status = voucherPageQueryDTO.getStatus();

        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<Voucher>().
                like(title != null ,Voucher::getTitle,title).eq(status != null,Voucher::getStatus,status)
                .eq(type != null,Voucher::getType,type);
        Page<Voucher> pageVoucher = page(new Page<>(voucherPageQueryDTO.getPage(),voucherPageQueryDTO.getPageSize()),wrapper);
        Page<VoucherVO> page = new Page<>();
        BeanUtils.copyProperties(pageVoucher,page);
        return new PageResult(page.getTotal(),page.getRecords());
    }
}
