package cn.hubu.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description: 数据路由配置
 * @Author: Xhy
 * @CreateTime: 2023-04-10 16:52
 */
@ConfigurationProperties(prefix = "db-sharding.jdbc.datasource")
public class DBRouterConfig {

    /**
     * 分库数量
     */
    private int dbCount;

    /**
     * 分表数量
     */
    private int tbCount;

    /**
     * 路由字段: 根据表的那个字段应用路由规则
     */
    private String routerKey;

    /**
     * 分库分表的数据库 list:db01,db02
     */
    private String list;

    /**
     * 默认的数据库 defaultDb
     */
    private String defaultDb;

    /**
     * 路由策略：hash, mod..
     */
    private String routerStrategy;

    public DBRouterConfig() {
    }

    public DBRouterConfig(int dbCount, int tbCount, String routerKey) {
        this.dbCount = dbCount;
        this.tbCount = tbCount;
        this.routerKey = routerKey;
    }

    public DBRouterConfig(int dbCount, int tbCount, String routerKey, String routerStrategy) {
        this.dbCount = dbCount;
        this.tbCount = tbCount;
        this.routerKey = routerKey;
        this.routerStrategy = routerStrategy;
    }

    public int getDbCount() {
        return dbCount;
    }

    public void setDbCount(int dbCount) {
        this.dbCount = dbCount;
    }

    public int getTbCount() {
        return tbCount;
    }

    public void setTbCount(int tbCount) {
        this.tbCount = tbCount;
    }

    public String getRouterKey() {
        return routerKey;
    }

    public void setRouterKey(String routerKey) {
        this.routerKey = routerKey;
    }

    public String getRouterStrategy() {
        return routerStrategy;
    }

    public void setRouterStrategy(String routerStrategy) {
        this.routerStrategy = routerStrategy;
    }

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getDefaultDb() {
        return defaultDb;
    }

    public void setDefaultDb(String defaultDb) {
        this.defaultDb = defaultDb;
    }
}
