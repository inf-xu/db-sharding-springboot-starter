package cn.hubu.strategy.impl;

import cn.hubu.DBContextHolder;
import cn.hubu.exception.TypeConversionException;
import cn.hubu.properties.DBRouterConfig;
import cn.hubu.strategy.IDBRouterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xu289
 * @date 2023-07-27 18:27
 * @description 基于取模的路由策略
 */
public class DBRouterStrategyMod implements IDBRouterStrategy {

    private final Logger logger = LoggerFactory.getLogger(DBRouterStrategyMod.class);

    private final DBRouterConfig dbRouterConfig;

    public DBRouterStrategyMod(DBRouterConfig dbRouterConfig) {
        this.dbRouterConfig = dbRouterConfig;
    }

    /**
     * 计算方式：
     *    dbIdx = dbKeyAttr mod dbCount
     *    dbIdx = dbKeyAttr mod tbCount
     * @param dbKeyAttr 路由字段
     */
    @Override
    public void doRouter(String dbKeyAttr) {
        long dkKey;
        try {
            dkKey = Long.parseLong(dbKeyAttr);
        } catch (NumberFormatException e) {
            throw new TypeConversionException(dbKeyAttr, Long.class);
        }

        int dbIdx = (int) (dkKey % dbRouterConfig.getDbCount()) + 1;
        int tbIdx = (int) (dkKey % dbRouterConfig.getTbCount());

        DBContextHolder.setDBKey(String.format("%02d", dbIdx));
        DBContextHolder.setTBKey(String.format("%03d", tbIdx));
        logger.debug("数据库路由 dbIdx：{} tbIdx：{}", dbIdx, tbIdx);
    }

    @Override
    public void setDBKey(int dbIdx) {
        DBContextHolder.setDBKey(String.format("%02d", dbIdx));
    }

    @Override
    public void setTBKey(int tbIdx) {
        DBContextHolder.setTBKey(String.format("%03d", tbIdx));
    }

    @Override
    public int dbCount() {
        return dbRouterConfig.getDbCount();
    }

    @Override
    public int tbCount() {
        return dbRouterConfig.getTbCount();
    }

    @Override
    public void clear() {
        DBContextHolder.clearDBKey();
        DBContextHolder.clearTBKey();
    }
}
