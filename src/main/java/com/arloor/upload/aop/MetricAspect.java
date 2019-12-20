package com.arloor.upload.aop;


import lombok.extern.apachecommons.CommonsLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;

@Component
@Aspect
@CommonsLog
public class MetricAspect {


    //切点定义：指定被SomeAnnotation注解的方法
    @Pointcut("@annotation(com.arloor.upload.aop.Metric)")
    public void test() {
    }

    //Around上面的切点
    @Around("test()")
    public Object around(ProceedingJoinPoint jp) throws Throwable {
       Object[] vars= jp.getArgs();
        Arrays.stream(vars).filter((var)->var instanceof HttpServletRequest).forEach(
                var->{
                    HttpServletRequest request=(HttpServletRequest)var;
                    try {
                        log.info(request.getRemoteAddr()+" >>>> "+ URLDecoder.decode(request.getRequestURI(),"UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
        );
        return jp.proceed();// 指定被代理对象的原方法
    }
}
