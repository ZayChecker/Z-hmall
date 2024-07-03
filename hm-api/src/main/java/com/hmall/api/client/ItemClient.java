package com.hmall.api.client;


import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.dto.VoucherDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@FeignClient("item-service")
public interface ItemClient {
    @GetMapping("/items")
    List<ItemDTO> queryItemByIds(@RequestParam Collection<Long> ids);

    @PutMapping("/items/stock/deduct")
    void deductStock(@RequestBody List<OrderDetailDTO> items);

    @GetMapping("/voucher/{id}")
    VoucherDTO queryVoucherById(@PathVariable("id") Long voucherId);

    @PutMapping("/voucher/{id}")
    boolean deductStockById(@PathVariable("id") Long voucherId);

    @PutMapping("/items/stock/increment/{id}/{num}")
    void incrementStock(@PathVariable("id") Long id, @PathVariable("num") int num);
}
