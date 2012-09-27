package io.prombok;

import io.prombok.annotations.In;
import io.prombok.annotations.Out;
import io.prombok.annotations.Packet;
import lombok.EqualsAndHashCode;

@Packet
@EqualsAndHashCode // for unit tests, should have an equals method.
public class PrimitiveObject {

    @In @Out public byte fieldByte;
    @In @Out public short fieldShort;
    @In @Out public char fieldChar;
    @In @Out public int fieldInt;
    @In @Out public long fieldLong;
    @In @Out public float fieldFloat;
    @In @Out public double fieldDouble;

}
