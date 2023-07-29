package cn.hubu.dynamic;

import cn.hubu.DBContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @description: 动态数据源获取，获取数据源时，都从这个里面进行获取
 * @Author: Xhy
 * @CreateTime: 2023-04-10 16:44
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        // 就是在yml中配置的 db01, db00 数据源
        return "db" + DBContextHolder.getDBKey();
    }

}
