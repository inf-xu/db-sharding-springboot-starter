package cn.hubu.annotation;

import java.lang.annotation.*;

/**
 * @description: 分库分表注解
 * @Author: Xhy
 * @CreateTime: 2023-04-10 16:41
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DBRouter {

    /** 分库分表字段 */
    String key() default "";

}

