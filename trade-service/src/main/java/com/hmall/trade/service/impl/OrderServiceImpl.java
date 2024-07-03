package com.hmall.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.CartClient;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.dto.VoucherDTO;
import com.hmall.common.domain.R;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;

import com.hmall.trade.constants.MQConstants;
import com.hmall.trade.domain.po.Result;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.mapper.OrderMapper;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import com.hmall.trade.utils.RedisWorker;
import com.hmall.trade.utils.SimpleRedisLock;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final CartClient cartClient;
    private final RabbitTemplate rabbitTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private final RedisWorker redisWorker;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;   //RedisScript接口的实现类
    static {    //静态的在静态代码块里做初始化
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));  //会去classpath下面找资源，resource就是classpath
        SECKILL_SCRIPT.setResultType(Long.class);   //设置返回值类型
    }//因为是静态常量和静态代码块，这个类一加载，这个脚本就初始化完成了


    @Override
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
        cartClient.deleteCartItemByIds(itemIds);

        // 4.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        //5.发送延时消息，检测订单支付状态
        rabbitTemplate.convertAndSend(
                MQConstants.DELAY_EXCHANGE_NAME,
                MQConstants.DELAY_ORDER_KEY,
                order.getId(),
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setDelay(100000);
                        return message;
                    }
                }
        );

        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    public void cancelOrder(Long orderId) {
        //标记订单已关闭
        lambdaUpdate()
                .set(Order::getStatus, 5)
                .eq(Order::getId, orderId)
                .update();
        //恢复库存
        List<OrderDetail> orderDetailList = detailService.lambdaQuery().eq(OrderDetail::getOrderId, orderId).list();
        for (OrderDetail orderDetail : orderDetailList) {
            Long itemId = orderDetail.getItemId();
            int num = orderDetail.getNum();
            itemClient.incrementStock(itemId, num);
        }
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserContext.getUser();
        long orderId = redisWorker.nextId("order");
        //执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),  //因为没有keys数组
                voucherId.toString(), userId.toString()
        );
        int r = result.intValue();
        //判断结果是否为0
        if(r != 0){
            return Result.fail(r == 1? "库存不足":"不能重复下单");
        }
        //保存到阻塞队列
        Order order = new Order();
        order.setId(orderId);
        order.setTotalFee(5000);
        order.setPaymentType(3); //余额支付
        order.setUserId(userId); //用户随便填
        order.setStatus(1);      //未支付

        rabbitTemplate.convertAndSend("seckill.order.direct", "seckill.success", order);
        rabbitTemplate.convertAndSend("seckill.voucher.direct", "seckill.success", voucherId);

        //返回订单id
        return Result.ok(orderId);
    }

//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //1.查询秒杀品
//        VoucherDTO voucher = itemClient.queryVoucherById(voucherId);
//        //2.判断秒杀是否已经开始
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始!");
//        }
//        //3.判断秒杀是否已经结束
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束!");
//        }
//        //4.判断库存是否充足
//        if(voucher.getStock() < 1){
//            return Result.fail("库存不足!");
//        }
//
//        Long userId = 114514L;
//        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        //获取锁对象
//        boolean isLock = lock.tryLock(1200);
//        //加锁失败
//        if(!isLock){
//            return Result.fail("不允许重复下单!");
//        }
//        try {
//            //获取代理对象(事务)
//            IOrderService proxy = (IOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucher);
//        }finally {
//            //释放锁，不再是synchronized自动释放了
//            lock.unlock();
//        }
//
////        synchronized (userId.toString().intern()){
////            IOrderService proxy = (IOrderService) AopContext.currentProxy();
////            return proxy.createVoucherOrder(voucher);
////        }
//    }

    @GlobalTransactional
    public Result createVoucherOrder(VoucherDTO voucher){
        //5.一人一单逻辑
        Long userId = 114514L;
        int count = query().eq("user_id", userId).count();
        if(count > 0){
            //用户已经购买过了
            return Result.fail("用户已经购买过一次!");
        }

        //5.扣减库存
        boolean success = itemClient.deductStockById(voucher.getId());
        if(!success){
            return Result.fail("库存不足!");
        }
        //6.创建订单
        Order order = new Order();
        Long orderId = redisWorker.nextId("order");
        order.setId(orderId);
        order.setTotalFee(voucher.getPrice());
        order.setPaymentType(3); //余额支付
        order.setUserId(userId);     //用户随便填
        order.setStatus(1);      //未支付
        save(order);
        return Result.ok(orderId);
    }
}
