package com.hmall.trade.listener;

import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeckillOrderListener {

    private final IOrderService iOrderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.seckill.order"),
            exchange = @Exchange(name = "seckill.order.direct", type = ExchangeTypes.DIRECT),
            key = "seckill.success"
    ))
    public void preserveOrder(Order voucherOrder){
        iOrderService.save(voucherOrder);
    }
}
