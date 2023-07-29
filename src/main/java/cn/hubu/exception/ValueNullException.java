package cn.hubu.exception;


/**
 * @author xu289
 * @date 2023-07-27 18:28
 * @description ValueNullException
 */
public class ValueNullException extends RuntimeException {
    public ValueNullException(String e) {
        super(e);
    }

    public ValueNullException(Class<?> c, String name) {
        super("[" + c + "." + name + "] this value can't be null");
    }
}
