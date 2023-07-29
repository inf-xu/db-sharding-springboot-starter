package cn.hubu.exception;


/**
 * @author xu289
 * @date 2023-07-27 18:30
 * @description ValueNullException
 */
public class TypeConversionException extends RuntimeException {
    public TypeConversionException(String e) {
        super(e + " Can't be converted to type");
    }

    public TypeConversionException(String e, Class<?> c) {
        super(e + " Can't be converted to " + c.getName() + " type");
    }
}
