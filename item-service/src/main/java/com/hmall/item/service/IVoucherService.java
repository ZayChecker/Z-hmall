package com.hmall.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.item.domain.po.Voucher;

public interface IVoucherService extends IService<Voucher> {

    boolean deductStockById(Long voucherId);

    void saveVoucher(Voucher voucher);
}
