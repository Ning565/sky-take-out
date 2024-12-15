package com.sky.controller.user;


import com.sky.result.Result;
import com.sky.service.IVoucherService;
import com.sky.utils.CacheClient;
import com.sky.vo.VoucherVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userVoucherController")
@RequestMapping("/user/voucher")
@Slf4j
@Api(tags = "C端-优惠券接口")
public class VoucherController {
    @Autowired
    private IVoucherService voucherService;


    /**
     * 查询优惠券
     * Redis来缓存优惠券信息，减少数据库查询操作
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("查询优惠券内容")
    public Result<VoucherVO> queryVoucher(@PathVariable  Long id) throws InterruptedException {
        // 1.调用CacheClient利用Redis查询——实现防止缓存穿透和缓存击穿
        // 2.封装返回结果
        VoucherVO voucherVO = voucherService.queryByID(id);
        return Result.success(voucherVO);
    }


}
