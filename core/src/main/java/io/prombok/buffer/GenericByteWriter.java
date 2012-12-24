package io.prombok.buffer;

import io.netty.buffer.ByteBuf;

import java.io.UnsupportedEncodingException;

public interface GenericByteWriter<T> {

    public void write(ByteBuf buf, T obj);

}
