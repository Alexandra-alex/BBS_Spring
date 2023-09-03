package com.easybbs.annotation;

import com.easybbs.entity.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

////注解运用在方法和属性上
@Target({ElementType.PARAMETER,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifyParam {
    //必填
    boolean required() default false;
    //最大
    int max() default -1;
    //最小
    int min() default -1;

    //正则表达式
    VerifyRegexEnum regex() default VerifyRegexEnum.NO;
}
