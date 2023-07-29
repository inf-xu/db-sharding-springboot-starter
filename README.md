# db-sharding-springboot-starter

> 基于xhy-db-router-springboot-starter基础上实现简单的分库分表组件，支持yml配置文件指定路由策略（hash、mod）。
>
> 特别鸣谢：https://gitee.com/XhyQAQ

#### 1. 如何使用

##### 1.1 安装

```powershell
# clone this repository
git clone https://github.com/inf-xu/db-sharding-springboot-starter

# cd
cd db-sharding-springboot-starter
```

##### 1.2 打包

```powershell
# install this module
mvn insatll
```

##### 1.3 导入依赖

```xml
<!--Use this module in your project-->
 <dependency>
     <groupId>cn.hubu</groupId>
     <artifactId>db-sharding-springboot-starter</artifactId>
     <version>1.0-SNAPSHOT</version>
</dependency>
```

##### 1.4 配置文件设置

```yml
db-sharding:
  jdbc:
    datasource:
      dbCount: 2 # 分库分表的数据库数量，必填
      tbCount: 4 # 每个数据库中表的数量，必填
      list: db01,db02 # 分库分表的两个数据源名字，必填
      defaultDb: db00 # 不进行分库分表的默认的数据源，必填
      router-strategy: mod # 路由策略，mod和hash，选填
      routerKey: id # 全局路由字段，不建议填写
      db00: # 默认的数据源，由于代码中写死了db开头，因此都这样配置
        driver-class-name: com.mysql.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
        username: root
        password: root
      db01:
        driver-class-name: com.mysql.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/test_01?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
        username: root
        password: root
      db02:
        driver-class-name: com.mysql.jdbc.Driver
        url: jdbc:mysql://127.0.0.1:3306/test_02?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
        username: root
        password: root
```

##### 1.5 正式使用

在你的需要分库分表的mapper方法上添加 `@DBRouterStrategy(splitTable = true)` 和 `@DBRouter(key = "id")` 注解

```java
@Mapper
@DBRouterStrategy(splitTable = true) // 是否分表
public interface UserSplitTableMapper {
    // 根据实体类中的成员变量 id 进行分库分表
    @DBRouter(key = "id")
    void insert(User user);
}
```



#### 2. 流程讲解

由于 SPI 机制，因此我们自己可以很方便的开发 starter，只需要在 `resources\META-INF\` 创建文件 `spring.factories` ，内容如下：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=cn.hubu.config.DataSourceAutoConfig
```

其中 `DataSourceAutoConfig` 全限定名，这个类就是 SpringBoot 启动是会自动加载的类。我们一般注入 `Bean` 就是在这个类中，简略内容如下：

```java
@EnableConfigurationProperties(DBRouterConfig.class) // 配置yml文件中字段的格式
@Configuration
public class DataSourceAutoConfig implements EnvironmentAware {
    
    @Bean(name = "db-router-point")
    @ConditionalOnMissingBean
    public DBRouterJoinPoint point(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
        return new DBRouterJoinPoint(dbRouterConfig, dbRouterStrategy);
    }
    
    ...
}
```

类 `DataSourceAutoConfig` 实现了 `EnvironmentAware` 接口，我们知道在启动 Spring 的时候会加载各种 `Aware`。此时重写 `EnvironmentAware` 的 ` setEnvironment(Environment environment)` 方法，就可以将 yml 文件中配置的属性绑定在 `DBRouterConfig` 上了。

其它就是正常的切换数据源思路，实现`AbstractRoutingDataSource`代替 `mybatis` 中数据源 `bean`，重写`determineCurrentLookupKey()` 方法，设置数据源策略。

利用 AOP+注解的方式环绕通知，选择不同的路由策略，代码如下：

```
@Around("aopPoint() && @annotation(dbRouter)")
public Object doRouter(ProceedingJoinPoint jp, DBRouter dbRouter) throws Throwable {
    // 拿到路由规则作用在哪个字段上 dbkey
    String dbKey = dbRouter.key();

    // 如果 dbkey 为空 并且没有指定默认的路由字段 则报错
    if (StringUtils.isBlank(dbKey) && StringUtils.isBlank(dbRouterConfig.getRouterKey())) 
    	throw new RuntimeException("annotation DBRouter key is null！");
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
```

其中 `doRouer()` 方法会对数据库和表进行映射，即分库分表。

```java
DBContextHolder.setDBKey(String.format("%02d", dbIdx));
DBContextHolder.setTBKey(String.format("%03d", tbIdx));
```

经过上述的织入之后，创建 `DynamicMybatisPlugin ` 拦截器，用于动态修改 sql 操作哪张表，是从 `DBContextHolder.getDBKey()` 和 ``DBContextHolder.getTBKey()``取出库和表，然后修改sql文件，并且执行后续操作。



#### 3. 待完成

- [ ] 自定义路由规则
- [ ] 分库分表产生的数据库事务问题



#### 4. 鸣谢

本项目基于项目 https://gitee.com/XhyQAQ/xhy-db-router-springboot-starter 进行简单扩展，实现指定分库分表策略，优化代码写法，校验配置文件字段。

为方便大家学习，代码已经标注了大段注释，同时可以观看原项目讲解视频以及徐庶老师的多数据源教学视频：

https://www.bilibili.com/video/BV1Gm4y1m7Qp

https://www.bilibili.com/video/BV1Ds4y167hr