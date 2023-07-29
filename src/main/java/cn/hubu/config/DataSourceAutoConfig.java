package cn.hubu.config;

import cn.hubu.DBRouterJoinPoint;
import cn.hubu.dynamic.DynamicDataSource;
import cn.hubu.dynamic.DynamicMybatisPlugin;
import cn.hubu.enums.RouterStrategyEnum;
import cn.hubu.properties.DBRouterConfig;
import cn.hubu.strategy.IDBRouterStrategy;
import cn.hubu.strategy.impl.DBRouterStrategyHashCode;
import cn.hubu.strategy.impl.DBRouterStrategyMod;
import cn.hubu.util.PropertyUtil;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @description: 数据源配置解析以及注册一些bean
 * @Author: Xhy
 * @CreateTime: 2023-04-10 16:43
 */
@EnableConfigurationProperties(DBRouterConfig.class)
@Configuration
public class DataSourceAutoConfig implements EnvironmentAware {

    /**
     * 数据源配置组
     * value：数  据源详细信息
     * {
     * db01={driver-class-name=com.mysql.jdbc.Driver, url=jdbc:mysql://127.0.0.1:3306/test_01?useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&serverTimezone=UTC&useSSL=true, username=root, password=root},
     * db02={driver-class-name=com.mysql.jdbc.Driver, url=jdbc:mysql://127.0.0.1:3306/test_02?useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&serverTimezone=UTC&useSSL=true, username=root, password=root}
     * }
     */
    private Map<String, Map<String, Object>> dataSourceMap = new HashMap<>();

    /**
     * 默认数据源配置
     */
    private Map<String, Object> defaultDataSourceConfig;

    /**
     * 分库数量
     */
    private int dbCount;

    /**
     * 分表数量
     */
    private int tbCount;

    /**
     * 路由字段
     */
    private String routerKey;

    /**
     * 路由策略：hash, mod..
     */
    private String routerStrategy;


    /**
     * AOP，用于分库
     *
     * @param dbRouterConfig
     * @param dbRouterStrategy
     * @return
     */
    @Bean(name = "db-router-point")
    @ConditionalOnMissingBean
    public DBRouterJoinPoint point(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
        return new DBRouterJoinPoint(dbRouterConfig, dbRouterStrategy);
    }

    /**
     * 将DB的信息注入到spring中，供后续获取
     *
     * @return
     */
    @Bean
    public DBRouterConfig dbRouterConfig() {
        return new DBRouterConfig(dbCount, tbCount, routerKey, routerStrategy);
    }

    /**
     * 配置插件bean,用于动态的决定表信息
     *
     * @return
     */
    @Bean
    public Interceptor plugin() {
        return new DynamicMybatisPlugin();
    }

    /**
     * 用于配置 TargetDataSources 以及 DefaultTargetDataSource
     * TargetDataSources: 额外的数据源
     * 可以用指定的key获取其他的数据源来达到动态切换数据源
     * DefaultTargetDataSource: 默认的数据源
     * 如果没有要用的数据源就会使用默认的数据源
     *
     * @return
     */
    @Bean
    public DataSource dataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        for (String dbInfo : dataSourceMap.keySet()) {
            Map<String, Object> objMap = dataSourceMap.get(dbInfo);
            targetDataSources.put(dbInfo, new DriverManagerDataSource(objMap.get("url").toString(), objMap.get("username").toString(), objMap.get("password").toString()));
        }

        // 设置数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(new DriverManagerDataSource(defaultDataSourceConfig.get("url").toString(), defaultDataSourceConfig.get("username").toString(), defaultDataSourceConfig.get("password").toString()));

        return dynamicDataSource;
    }


    /**
     * 依赖注入
     *
     * @param dbRouterConfig
     * @return
     */
    @Bean
    public IDBRouterStrategy dbRouterStrategy(DBRouterConfig dbRouterConfig) {
        if (RouterStrategyEnum.HASH.getStrategy().equals(routerStrategy)) {
            return new DBRouterStrategyHashCode(dbRouterConfig);
        } else if (RouterStrategyEnum.MOD.getStrategy().equals(routerStrategy)) {
            return new DBRouterStrategyMod(dbRouterConfig);
        } else {
            throw new RuntimeException("specify a correct routing policy");
        }
    }


    /**
     * 配置事务
     *
     * @param dataSource
     * @return
     */
    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(dataSourceTransactionManager);
        transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return transactionTemplate;
    }

    /**
     * 读取yml中的数据源信息
     *
     * @param environment
     */
    @Override
    public void setEnvironment(Environment environment) {
        String prefix = "db-sharding.jdbc.datasource.";

        dbCount = Integer.valueOf(environment.getProperty(prefix + "dbCount"));
        tbCount = Integer.valueOf(environment.getProperty(prefix + "tbCount"));
        routerKey = environment.getProperty(prefix + "routerKey");
        routerStrategy = Optional.ofNullable(environment.getProperty(prefix + "routerStrategy")).orElse(RouterStrategyEnum.HASH.getStrategy());

        // 分库分表数据源
        String dataSources = environment.getProperty(prefix + "list");
        assert dataSources != null;
        for (String dbInfo : dataSources.split(",")) {
            Map<String, Object> dataSourceProps = PropertyUtil.handle(environment, prefix + dbInfo, Map.class);
            dataSourceMap.put(dbInfo, dataSourceProps);
        }

        // 默认数据源
        String defaultData = environment.getProperty(prefix + "defaultDb");
        defaultDataSourceConfig = PropertyUtil.handle(environment, prefix + defaultData, Map.class);

    }

}

