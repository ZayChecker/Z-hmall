package com.hmall.common.interceptors;

import cn.hutool.core.util.StrUtil;
import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//它不需要做登录校验，一律放行
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取用户登录信息
        String userInfo = request.getHeader("user-info");
        //2.判断是否获取了用户，如果有，存入ThreadLocal
        if(StrUtil.isNotBlank(userInfo)){   //字符串不为null也不为空
            UserContext.setUser(Long.valueOf(userInfo));
        }
        //3.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清理用户
        UserContext.removeUser();
    }
}
