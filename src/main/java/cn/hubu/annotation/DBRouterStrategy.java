package cn.hubu.annotation;

import java.lang.annotation.*;

/**
 * @description: 分表注解
 * @Author: Xhy
 * @CreateTime: 2023-04-10 16:42
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DBRouterStrategy {
    boolean splitTable() default false;
}