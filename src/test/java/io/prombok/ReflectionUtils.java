package io.prombok;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.Method;

public class ReflectionUtils {

    static <T> T from(ByteBuf data, Class<T> cls) {
        try {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals("from")) {
                    return (T) m.invoke(null, data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static ByteBuf toByteBuf(Object obj) {
        try {
            for (Method m : obj.getClass().getDeclaredMethods()) {
                if (m.getName().equals("toByteBuf")) {
                    return (ByteBuf) m.invoke(obj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
