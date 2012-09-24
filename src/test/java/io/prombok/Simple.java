package io.prombok;

import io.prombok.annotations.In;
import io.prombok.annotations.Out;
import io.prombok.annotations.Packet;
import io.prombok.codec.DefaultStringByteCodec;

@Packet
public class Simple {
    
    @In @Out public int fieldInt;
    @In @Out public long fieldLong;
    @In @Out(writer = DefaultStringByteCodec.class) public String fieldString;
    
}
