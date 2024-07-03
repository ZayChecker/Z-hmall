package com.hmall.trade.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    //线程在每个JVM内部都是递增的，多个JVM可能会出现线程id冲突
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;   //RedisScript接口的实现类
    static {    //静态的在静态代码块里做初始化
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("main.lua"));  //会去classpath下面找资源，resource就是classpath
        UNLOCK_SCRIPT.setResultType(Long.class);   //设置返回值类型
    }//因为是静态常量和静态代码块，这个类一加载，这个脚本就初始化完成了

    //构造函数，用户传递参数
    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate){
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取线程标识(线程id)
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        stringRedisTemplate.execute(   //调用Lua脚本
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),   //要集合
                ID_PREFIX + Thread.currentThread().getId()
        );
    }

//    @Override
//    public void unlock() {
//        //获取线程标识(线程id)
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        //获取锁中的标识
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        //判断标识是否一致
//        if(threadId.equals(id)){
//            //通过del删除锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//        //不一致就不管
//    }
}
