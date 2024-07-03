package com.hmall.api.dto;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class VoucherDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 秒杀品id
     */
    private Long id;

    /**
     * 秒杀品名称
     */
    private String name;

    /**
     * 价格（分）
     */
    private Integer price;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 生效时间
     */
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    private LocalDateTime endTime;


}