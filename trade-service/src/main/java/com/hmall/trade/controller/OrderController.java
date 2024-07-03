package com.hmall.trade.controller;

import com.hmall.common.utils.BeanUtils;

import com.hmall.trade.domain.po.Result;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.vo.OrderVO;
import com.hmall.trade.service.IOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

@Api(tags = "订单管理接口")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final IOrderService orderService;

    @ApiOperation("根据id查询订单")
    @GetMapping("{id}")
    public OrderVO queryOrderById(@Param ("订单id")@PathVariable("id") Long orderId) {
        return BeanUtils.copyBean(orderService.getById(orderId), OrderVO.class);
    }

    @ApiOperation("创建订单")
    @PostMapping
    public Long createOrder(@RequestBody OrderFormDTO orderFormDTO){
        return orderService.createOrder(orderFormDTO);
    }

    @ApiOperation("标记订单已支付")
    @ApiImplicitParam(name = "orderId", value = "订单id", paramType = "path")
    @PutMapping("/{orderId}")
    public void markOrderPaySuccess(@PathVariable("orderId") Long orderId) {
        orderService.markOrderPaySuccess(orderId);
    }

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId){
        return orderService.seckillVoucher(voucherId);
    }
}
/*
317578
RIMOWA 21寸托运箱拉杆箱 SALSA AIR系列果绿色 820.70.36.4
28900
https://m.360buyimg.com/mobilecms/s720x720_jfs/t6934/364/1195375010/84676/e9f2c55f/597ece38N0ddcbc77.jpg!q70.jpg.webp
{"颜色": "红色", "尺码": "21寸"}
*/

