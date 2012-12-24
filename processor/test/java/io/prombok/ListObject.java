package io.prombok;

import io.prombok.annotations.In;
import io.prombok.annotations.Out;
import io.prombok.annotations.Packet;
import lombok.EqualsAndHashCode;

import java.util.List;

@Packet
@EqualsAndHashCode
public class ListObject {

    @In @Out public List<Integer> integerList;
    @In @Out public List<String> stringList;
    @In @Out public List<PrimitiveObject> objectList;

}
