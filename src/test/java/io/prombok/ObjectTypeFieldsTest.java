package io.prombok;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import java.util.ArrayList;

import static io.prombok.ReflectionUtils.from;
import static io.prombok.ReflectionUtils.toByteBuf;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class ObjectTypeFieldsTest {

    @Test
    public void testStringObjectMarshalling() {
        StringObject obj = createStringObject();

        ByteBuf data = toByteBuf(obj);
        assertNotNull(data);

        StringObject recovered = from(data, StringObject.class);
        assertNotNull(recovered);

        assertEquals(obj, recovered);
    }

    @Test
    public void testListObjectMarshalling() {
        ListObject obj = createListObject();

        ByteBuf data = toByteBuf(obj);
        assertNotNull(data);

        ListObject recovered = from(data, ListObject.class);
        assertNotNull(recovered);

        assertEquals(obj, recovered);
    }

    @Test
    public void testReferenceObjectMarshalling() {
        ReferenceObject obj = createRefereceObject();

        ByteBuf data = toByteBuf(obj);
        assertNotNull(data);

        ReferenceObject recovered = from(data, ReferenceObject.class);
        assertNotNull(recovered);

        assertEquals(obj, recovered);
    }

    public static StringObject createStringObject() {
        StringObject obj = new StringObject();
        obj.fieldString = "fieldString";
        return obj;
    }

    public static ListObject createListObject() {
        ListObject obj = new ListObject();
        obj.integerList = new ArrayList<Integer>();
        obj.integerList.add(1);
        obj.integerList.add(2);
        obj.integerList.add(3);
        obj.stringList = new ArrayList<String>();
        obj.stringList.add("a");
        obj.stringList.add("b");
        obj.stringList.add("c");
        obj.objectList = new ArrayList<PrimitiveObject>();
        obj.objectList.add(PrimitiveFieldsTest.createSimplePrimitiveObject());
        return obj;
    }

    public static ReferenceObject createRefereceObject() {
        ReferenceObject obj = new ReferenceObject();
        obj.stringObject = createStringObject();
        obj.primitiveObject = PrimitiveFieldsTest.createSimplePrimitiveObject();
        obj.listObject = createListObject();
        return obj;
    }

}
