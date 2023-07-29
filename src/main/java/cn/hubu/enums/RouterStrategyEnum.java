package cn.hubu.enums;

/**
 * @author xu289
 * @date 2023-07-27 18:34
 * @description 路由策略
 */
public enum RouterStrategyEnum {
    MOD(1, "mod"),
    HASH(2, "hash"),
    TIME(3, "time"),
    CUSTOM(4, "custom")
    ;


    private int code;
    private String strategy;

    RouterStrategyEnum(int code, String strategy) {
        this.code = code;
        this.strategy = strategy;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
