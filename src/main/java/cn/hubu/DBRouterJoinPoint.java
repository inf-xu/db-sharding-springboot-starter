package cn.hubu;

import cn.hubu.annotation.DBRouter;
import cn.hubu.exception.ValueNullException;
import cn.hubu.properties.DBRouterConfig;
import cn.hubu.strategy.IDBRouterStrategy;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @description: 数据路由切面，通过自定义注解的方式，拦截被切面的方法，进行数据库路由
 * @Author: Xhy
 * @CreateTime: 2023-04-10 16:53
 */
@Aspect
public class DBRouterJoinPoint {

    private DBRouterConfig dbRouterConfig;

    private IDBRouterStrategy dbRouterStrategy;

    public DBRouterJoinPoint(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
        this.dbRouterConfig = dbRouterConfig;
        this.dbRouterStrategy = dbRouterStrategy;
    }

    @Pointcut("@annotation(cn.hubu.annotation.DBRouter)")
    public void aopPoint() {
    }

    /**
     * 所有需要分库分表的操作，都需要使用自定义注解进行拦截，拦截后读取方法中的入参字段，根据字段进行路由操作。
     * 1. dbRouter.key() 确定根据哪个字段进行路由
     * 2. getAttrValue 根据数据库路由字段，从入参中读取出对应的值。比如路由 key 是 uId，那么就从入参对象 Obj 中获取到 uId 的值。
     * 3. dbRouterStrategy.doRouter(dbKeyAttr) 路由策略根据具体的路由值进行处理
     * 4. 路由处理完成后放行。 jp.proceed();
     * 5. 最后 dbRouterStrategy 需要执行 clear 因为这里用到了 ThreadLocal 需要手动清空。关于 ThreadLocal 内存泄漏介绍 https://t.zsxq.com/027QF2fae
     */
    @Around("aopPoint() && @annotation(dbRouter)")
    public Object doRouter(ProceedingJoinPoint jp, DBRouter dbRouter) throws Throwable {
        // 拿到路由规则作用在哪个字段上 dbkey
        String dbKey = dbRouter.key();

        // 如果 dbkey 为空 并且没有指定默认的路由字段 则报错
        if (StringUtils.isBlank(dbKey) && StringUtils.isBlank(dbRouterConfig.getRouterKey())) {
            throw new RuntimeException("annotation DBRouter key is null！");
        }
        // dbkey 为空的话，使用默认路由字段
        dbKey = StringUtils.isNotBlank(dbKey) ? dbKey : dbRouterConfig.getRouterKey();
        // 路由属性
        String dbKeyAttr = getAttrValue(dbKey, jp.getArgs());
        // 路由策略
        dbRouterStrategy.doRouter(dbKeyAttr);
        // 返回结果
        try {
            return jp.proceed();
        } finally {
            dbRouterStrategy.clear();
        }
    }

    /**
     * 这段代码的作用是获取当前正在执行的方法对象，以便后续进行一些操作或者分析
     * @param jp
     * @return
     * @throws NoSuchMethodException
     */
    private Method getMethod(JoinPoint jp) throws NoSuchMethodException {
        /**
         * 首先，jp.getSignature() 返回一个 Signature 对象，代表了当前正在执行的方法的签名信息。
         * 然后，将这个 Signature 对象强制转换为 MethodSignature 对象，因为我们需要访问具体的方法信息。
         * 接下来，通过调用 jp.getTarget() 方法获取当前执行方法所属的目标对象（即方法所在的类实例）。
         * 最后，使用 getClass().getMethod() 方法从目标对象的类中获取指定方法名和参数类型的 Method 对象，并返回该对象。这里通过 methodSignature.getName() 获取方法名，通过 methodSignature.getParameterTypes() 获取方法的参数类型。
         */
        Signature sig = jp.getSignature();
        MethodSignature methodSignature = (MethodSignature) sig;
        return jp.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
    }

    /**
     * TODO 好好优化一下
     */
    public String getAttrValue(String attr, Object[] args) {
        if (1 == args.length) {
            // arg =  User = (1, "xhy", "123");
            Object arg = args[0];
            // false
            if (arg instanceof String) {
                return arg.toString();
            }
        }

        String filedValue = null;
        for (Object arg : args) {
            try {
                if (StringUtils.isNotBlank(filedValue)) {
                    break;
                }
                // 在 arg 对象中获取 attr 属性的属性值
                // 即 user 中获取 id 的值
                filedValue = BeanUtils.getProperty(arg, attr);
            } catch (Exception e) {
                throw new ValueNullException(arg.getClass(), attr);
            }
        }
        return filedValue;
    }

}
