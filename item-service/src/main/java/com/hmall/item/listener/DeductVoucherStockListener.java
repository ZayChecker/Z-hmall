package com.hmall.item.listener;

import com.hmall.item.service.IVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeductVoucherStockListener {

    private final IVoucherService iVoucherService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.seckill.voucher"),
            exchange = @Exchange(name = "seckill.voucher.direct", type = ExchangeTypes.DIRECT),
            key = "seckill.success"
    ))
    public void deductStock(Long voucherId){
        iVoucherService.deductStockById(voucherId);
    }
}
