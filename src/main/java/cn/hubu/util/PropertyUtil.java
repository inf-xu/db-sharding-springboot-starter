package cn.hubu.util;

import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @description: 属性工具类
 * @Author: Xhy
 * @CreateTime: 2023-04-10 16:51
 */
public class PropertyUtil {

    private static int springBootVersion = 1;

    static {
        try {
            Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
        } catch (ClassNotFoundException e) {
            springBootVersion = 2;
        }
    }

    /**
     * Spring Boot 1.x is compatible with Spring Boot 2.x by Using Java Reflect.
     * @param environment : the environment context
     * @param prefix : the prefix part of property key
     * @param targetClass : the target class type of result
     * @param <T> : refer to @param targetClass
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T handle(final Environment environment, final String prefix, final Class<T> targetClass) {
        switch (springBootVersion) {
            case 1:
                return (T) v1(environment, prefix);
            default:
                return (T) v2(environment, prefix, targetClass);
        }
    }

    private static Object v1(final Environment environment, final String prefix) {
        try {
            Class<?> resolverClass = Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
            Constructor<?> resolverConstructor = resolverClass.getDeclaredConstructor(PropertyResolver.class);
            Method getSubPropertiesMethod = resolverClass.getDeclaredMethod("getSubProperties", String.class);
            Object resolverObject = resolverConstructor.newInstance(environment);
            // String prefix = "db-sharding.jdbc.datasource.";
            // prefix = prefix + dbInfo
            String prefixParam = prefix.endsWith(".") ? prefix : prefix + ".";
            return getSubPropertiesMethod.invoke(resolverObject, prefixParam);
        } catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * 使用反射调用 Spring 框架的 Binder 类的 get 和 bind 方法，实现了动态绑定配置属性并返回绑定结果。
     * @param environment
     * @param prefix
     * @param targetClass
     * @return
     */
    private static Object v2(final Environment environment, final String prefix, final Class<?> targetClass) {
        try {
            /**
             * 通过 Class.forName 方法获取了 Binder 的类对象，并将其赋值给 binderClass 变量。
             * Class.forName 方法是通过类的全限定名来动态加载类。
             */
            Class<?> binderClass = Class.forName("org.springframework.boot.context.properties.bind.Binder");
            /**
             * 使用 getDeclaredMethod 方法从 binderClass 中获取了名为 "get" 和 "bind" 的两个方法对象，并分别赋值给 getMethod 和 bindMethod 变量。
             * getDeclaredMethod 方法可以通过方法名和参数类型来获取指定类中声明的方法对象。
             */
            Method getMethod = binderClass.getDeclaredMethod("get", Environment.class);
            /**
             * 在Binder类中，bind(String name, Class<T> target)方法用于将一个名称（name）与一个目标类（target）进行绑定。
             * 该方法通常用于依赖注入或依赖关系管理的场景。
             * 具体来说，将名称（name）与目标类（target）进行绑定意味着当需要获取该名称对应的对象时，会返回目标类的一个实例。
             * 这种绑定关系可以在后续的代码中使用，以解析和获取相应的实例。
             */
            Method bindMethod = binderClass.getDeclaredMethod("bind", String.class, Class.class);
            // invoke 方法用于动态调用方法，第一个参数是要调用的对象（对于静态方法，可以传入 null），后面的参数是方法的实际参数。
            Object binderObject = getMethod.invoke(null, environment);
            // 如果 prefix 结尾有点号（.），则截取除最后一个字符以外的部分，否则保持不变。
            String prefixParam = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
            // binderObject 就是对象（Binder类的）, 调用 binderObject 对象中的 bind(String, Class) 方法
            // 这里调用的是 Binder 类的 bind 方法，用于绑定配置属性。
            Object bindResultObject = bindMethod.invoke(binderObject, prefixParam, targetClass);
            // 通过 getClass 方法获取 bindResultObject 的类对象，并使用 getDeclaredMethod 方法获取其名为 "get" 的方法对象，赋值给 resultGetMethod 变量
            Method resultGetMethod = bindResultObject.getClass().getDeclaredMethod("get");
            // 使用 invoke 方法调用 resultGetMethod 对象表示的方法，传入 bindResultObject 参数，返回绑定结果。
            return resultGetMethod.invoke(bindResultObject);
        } catch (final ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

}

