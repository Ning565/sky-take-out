package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.VoucherDTO;
import com.sky.dto.VoucherPageQueryDTO;
import com.sky.entity.Voucher;
import com.sky.entity.VoucherOrder;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.VoucherVO;

import java.util.List;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result purchase(Long id);
}
