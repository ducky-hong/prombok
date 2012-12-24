package io.prombok.annotations;

import io.prombok.buffer.GenericByteWriter;
import io.prombok.codec.DefaultStringByteCodec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Out {
    Class<?> writer() default DefaultStringByteCodec.class;
}
