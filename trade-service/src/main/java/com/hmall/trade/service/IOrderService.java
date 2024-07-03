package com.hmall.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.api.dto.VoucherDTO;
import com.hmall.trade.domain.po.Result;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
public interface IOrderService extends IService<Order> {

    Long createOrder(OrderFormDTO orderFormDTO);

    void markOrderPaySuccess(Long orderId);

    void cancelOrder(Long orderId);

    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(VoucherDTO voucher);

}
