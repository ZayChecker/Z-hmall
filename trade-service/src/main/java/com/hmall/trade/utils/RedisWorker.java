package com.hmall.trade.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

//唯一id生成器
@Component
public class RedisWorker {
    //开始时间戳
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    //序列号的位数
    private static final int COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix){  //不同业务对应不同的key
        //1.生成时间戳，当前时间减去起始时间
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        //2.生成序列号
        //2.1获取当天日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //2.2自增长，一个业务下一天的单
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + date);

        //3.拼接并返回
        return timestamp << COUNT_BITS | count;
    }
}
