package com.easybbs.aspect;

import com.easybbs.annotation.GlobalInterceptor;
import com.easybbs.annotation.VerifyParam;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.enums.ResponseCodeEnum;
import com.easybbs.exception.BusinessException;
import com.easybbs.utils.JsonUtils;
import com.easybbs.utils.StringTools;
import com.easybbs.utils.VerifyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.condition.RequestConditionHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Component//spring管理
@Aspect//切面
//切面定义
public class OperactionAspect {
    private static final Logger logger = LoggerFactory.getLogger(OperactionAspect.class);

    private static final  String[] TYPE_BASE = {"java.lang.String","java.lang.Integer","java.lang.Long"};

    //加入注解时拦截
    @Pointcut("@annotation(com.easybbs.annotation.GlobalInterceptor)")//切点
    private void requestInterceptor(){

    }
    //AOP拦截
    @Around("requestInterceptor()")
    public Object interceptorDo(ProceedingJoinPoint point){
        try{
            //获取目标
            Object target = point.getTarget();
            //获取参数中的值
            Object[] arguments = point.getArgs();
            //获取方法名
            String methodName = point.getSignature().getName();

            Class<?>[] prameterTypes =((MethodSignature)point.getSignature()).getMethod().getParameterTypes();
            Method method = target.getClass().getMethod(methodName,prameterTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);

            if(interceptor == null){
                return null;
            }
            //校验登录
            if(interceptor.checkLogin()){
                checkLogin();
            }
            //校验参数
            if(interceptor.checkParams()){
                validateParams(method,arguments);
            }
            //在切面中继续执行被拦截的方法，并返回
            return point.proceed();
        }catch (BusinessException e) {
            logger.error("全局拦截器异常",e);
           throw e;
        }catch (Exception e){
            logger.error("全局拦截器异常",e);
            throw new BusinessException((ResponseCodeEnum.CODE_500));
        }catch (Throwable e){
            logger.error("全局拦截器异常",e);
            throw new BusinessException((ResponseCodeEnum.CODE_500));
        }
    }
    //校验登录
    private void checkLogin(){
        //获取session
        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        Object obj = session.getAttribute(Constants.SESSION_KEY);
        if(obj == null)
        {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
    }

    private void validateParams(Method method,Object[] arguments){
        //获取参数
        Parameter[] parameters = method.getParameters();
        for(int i = 0; i < parameters.length;i++){
            Parameter parameter = parameters[i];
            Object value = arguments[i];

            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if(verifyParam == null){
                continue;
            }
            if(ArrayUtils.contains(TYPE_BASE,parameter.getParameterizedType().getTypeName())){
                checkValue(value,verifyParam);
            }
        }
    }
    private void checkObjValue(Parameter parameter,Object value){

    }
    //校验参数
    private void checkValue(Object value,VerifyParam verifyParam){
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();
        //校验空
        if(isEmpty&&verifyParam.required()){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //校验长度
        if(!isEmpty && verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1 && verifyParam.min() > length){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //校验正则
        if(!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(),String.valueOf(value))){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

}
