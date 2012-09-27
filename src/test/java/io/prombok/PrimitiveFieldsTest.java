package io.prombok;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import java.lang.reflect.Method;

import static junit.framework.Assert.*;
import static io.prombok.ReflectionUtils.*;

public class PrimitiveFieldsTest {

    @Test
    public void testPrimitiveObjectMarshalling() {
        PrimitiveObject obj = createSimplePrimitiveObject();

        ByteBuf marshalled = toByteBuf(obj);
        assertNotNull(marshalled);

        PrimitiveObject recovered = from(marshalled, PrimitiveObject.class);
        assertNotNull(recovered);

        assertEquals(obj, recovered);
    }

    public static  PrimitiveObject createSimplePrimitiveObject() {
        PrimitiveObject obj = new PrimitiveObject();
        obj.fieldByte = 8;
        obj.fieldShort = 16;
        obj.fieldChar = 'a';
        obj.fieldInt = 32;
        obj.fieldLong = 64;
        obj.fieldFloat = 1.0f;
        obj.fieldDouble = 2.0;
        return obj;
    }

}
