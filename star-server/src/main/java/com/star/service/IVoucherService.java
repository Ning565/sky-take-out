package com.star.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.star.dto.VoucherDTO;
import com.star.dto.VoucherPageQueryDTO;
import com.star.entity.Voucher;
import com.star.result.PageResult;
import com.star.vo.VoucherVO;

import java.util.List;

public interface IVoucherService extends IService<Voucher> {

    Long saveSeckill(VoucherDTO voucherDTO);

    void deleteByIds(List<Long> ids);

    void updateStatusById(Integer status,Long id);

    PageResult pageQuery(VoucherPageQueryDTO voucherPageQueryDTO);

    VoucherVO queryByID(Long id) throws InterruptedException;
}
