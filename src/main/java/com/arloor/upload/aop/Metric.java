package com.arloor.upload.aop;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)// 只能注解到方法上
@Retention(RetentionPolicy.RUNTIME) //运行时生效
public @interface Metric {
}
