package com.hmall.trade;

import com.hmall.api.config.DefaultFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//ItemService的bean是动态代理实现的，实现的前提是被扫描到由spring处理
//启动类所在的包叫com.hmall.cart，启动时扫描的包就是这个
//而hm-api的扫描包叫com.hmall.api，所以尽管引了依赖，有了item的那俩个类，由于包不一样，所以不会被扫描到
@EnableFeignClients(basePackages = "com.hmall.api.client", defaultConfiguration = DefaultFeignConfig.class)
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.hmall.trade.mapper")
@SpringBootApplication
public class TradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }

}