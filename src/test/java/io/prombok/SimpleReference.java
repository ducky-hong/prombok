package io.prombok;

import io.prombok.annotations.In;
import io.prombok.annotations.Out;
import io.prombok.annotations.Packet;

@Packet
public class SimpleReference {
    
    @In @Out public int fieldInt;
    @In @Out public String fieldString;

}
