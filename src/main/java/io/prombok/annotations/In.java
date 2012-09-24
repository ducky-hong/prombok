package io.prombok.annotations;

import io.prombok.codec.DefaultStringByteCodec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface In {
    Class<?> reader() default DefaultStringByteCodec.class;
}
