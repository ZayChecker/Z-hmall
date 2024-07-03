package com.hmall.item.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.hmall.item.domain.po.Voucher;
import feign.Param;
import org.apache.ibatis.annotations.Update;

public interface VoucherMapper extends BaseMapper<Voucher> {
    @Update("Update voucher SET STOCK = STOCK - 1 WHERE id = #{id}")
    void deductStockById(@Param("id") Long voucherId);
}
