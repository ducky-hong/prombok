package io.prombok.codec;

import io.netty.buffer.ByteBuf;
import io.prombok.buffer.GenericByteReader;
import io.prombok.buffer.GenericByteWriter;

public class SwappedLongByteCodec implements GenericByteReader<Long>, GenericByteWriter<Long> {
    @Override
    public Long read(ByteBuf src) {
        return null;
    }

    @Override
    public void write(ByteBuf buf, Long obj) {
    }
}
