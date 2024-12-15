package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.VoucherDTO;
import com.sky.dto.VoucherPageQueryDTO;
import com.sky.entity.Voucher;
import com.sky.result.PageResult;
import com.sky.vo.VoucherVO;

import java.util.List;

public interface IVoucherService extends IService<Voucher> {

    Long saveSeckill(VoucherDTO voucherDTO);

    void deleteByIds(List<Long> ids);

    void updateStatusById(Integer status,Long id);

    PageResult pageQuery(VoucherPageQueryDTO voucherPageQueryDTO);

    VoucherVO queryByID(Long id) throws InterruptedException;
}
