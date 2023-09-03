package com.easybbs.utils;

import com.easybbs.entity.enums.VerifyRegexEnum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//校验正则
public class VerifyUtils {
    public static Boolean verify(String regs, String value){
        if(StringTools.isEmpty(value)){
            return false;
        }
        //将一个正则表达式字符串编译成一个 Pattern 对象,使用 matcher 方法应用于字符串,判断是否匹配成功
        Pattern pattern = Pattern.compile(regs);
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }
    public static Boolean verify(VerifyRegexEnum regs, String value){
        return verify(regs.getRegex(), value);
    }
}
