package io.prombok;

import io.prombok.annotations.In;
import io.prombok.annotations.Out;
import io.prombok.annotations.Packet;
import lombok.EqualsAndHashCode;

@Packet
@EqualsAndHashCode
public class ByteArrayObject {

    @In @Out public byte[] fieldByteArray;

}
