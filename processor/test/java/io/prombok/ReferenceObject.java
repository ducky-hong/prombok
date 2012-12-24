package io.prombok;

import io.prombok.annotations.In;
import io.prombok.annotations.Out;
import io.prombok.annotations.Packet;
import lombok.EqualsAndHashCode;

@Packet
@EqualsAndHashCode
public class ReferenceObject {
    
    @In @Out public PrimitiveObject primitiveObject;
    @In @Out public StringObject stringObject;
    @In @Out public ListObject listObject;
    
}
