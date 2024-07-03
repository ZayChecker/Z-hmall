package com.hmall.item.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.hmall.item.domain.po.Voucher;
import com.hmall.item.mapper.VoucherMapper;
import com.hmall.item.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void saveVoucher(Voucher voucher) {
        save(voucher);
        stringRedisTemplate.opsForValue().set("seckill:stock" + voucher.getId(), voucher.getStock().toString());
    }

    @Override
    @Transactional
    public boolean deductStockById(Long voucherId) {
        //baseMapper.deductStockById(voucherId);
        Voucher voucher = getById(voucherId);
        int stock = voucher.getStock();
        boolean success = lambdaUpdate()
                .set(Voucher::getStock, stock - 1)
                .eq(Voucher::getId, voucherId)
                .ge(Voucher::getStock, 0)
                .update();
        return success;
    }


}
