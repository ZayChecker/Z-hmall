package com.hmall.api.config;

import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

//这个配置类想要生效要把它加在对应启动类里
public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }

    //通过匿名内部类来声明拦截器，自己开个新类也行
    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Long userId = UserContext.getUser();
                if(userId != null){
                    template.header("user-info", userId.toString());
                }
            }
        };
    }
}
