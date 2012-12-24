package io.prombok.buffer;

import io.netty.buffer.ByteBuf;

public interface GenericByteReader<T> {
    
    public T read(ByteBuf src);

}
