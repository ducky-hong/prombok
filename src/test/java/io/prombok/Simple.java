package io.prombok;

import io.prombok.annotations.If;
import io.prombok.annotations.In;
import io.prombok.annotations.Out;
import io.prombok.annotations.Packet;
import io.prombok.codec.DefaultStringByteCodec;
import io.prombok.codec.SwappedLongByteCodec;

@Packet
public class Simple {

    @In @Out public int fieldInt;
    @In @Out public long fieldLong;
    @In @Out(writer = DefaultStringByteCodec.class) public String fieldString;
    @If("fieldInt > 0")
    @In @Out public byte fieldByte;
    @In @Out public SimpleReference fieldReference;

}
