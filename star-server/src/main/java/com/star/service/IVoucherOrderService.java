package com.star.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.star.dto.VoucherDTO;
import com.star.dto.VoucherPageQueryDTO;
import com.star.entity.Voucher;
import com.star.entity.VoucherOrder;
import com.star.result.PageResult;
import com.star.result.Result;
import com.star.vo.VoucherVO;

import java.util.List;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result purchase(Long id);
}
