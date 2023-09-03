package com.easybbs.annotation;

import java.lang.annotation.*;

//注解运用在方法和类上
@Target({ElementType.METHOD,ElementType.TYPE})
//服务运行时
@Retention(RetentionPolicy.RUNTIME)
@Documented
//子类继承
@Inherited
public @interface GlobalInterceptor {
    //是否需要登录
    boolean checkLogin() default false;

    //是否需要校验参数
    boolean checkParams() default false;

    //校验频次
}
