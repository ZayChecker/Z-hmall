package com.hmall.item.controller;

import cn.hutool.core.bean.BeanUtil;
import com.hmall.api.dto.VoucherDTO;
import com.hmall.item.domain.po.Voucher;
import com.hmall.item.service.IVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final IVoucherService voucherService;

    //新增秒杀品
    @PostMapping("seckill")
    public void saveVoucher(@RequestBody Voucher voucher){
        voucherService.saveVoucher(voucher);
    }

    //查询秒杀品
    @GetMapping("{id}")
    public VoucherDTO queryVoucherById(@PathVariable("id") Long voucherId){
        Voucher voucher =  voucherService.getById(voucherId);
        return BeanUtil.copyProperties(voucher, VoucherDTO.class);
    }

    //扣减秒杀品
    @PutMapping("{id}")
    public boolean deductStockById(@PathVariable("id") Long voucherId){
        return voucherService.deductStockById(voucherId);
    }


}
