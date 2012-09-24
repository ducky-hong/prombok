package io.prombok.codec;

import io.netty.buffer.ByteBuf;
import io.prombok.buffer.GenericByteReader;
import io.prombok.buffer.GenericByteWriter;

import java.io.UnsupportedEncodingException;

public class DefaultStringByteCodec implements GenericByteReader<String>, GenericByteWriter<String> {
    
    private static final String ENCODING = "UTF-16LE";
    
    @Override
    public String read(ByteBuf src) {
        final int start = src.readerIndex();
        while (src.readChar() != 0);
        byte[] data = src.copy(start, src.readerIndex() - start).array();
        try {
            return new String(src.copy(start, src.readerIndex() - start).array(), ENCODING);
        } catch (UnsupportedEncodingException e) {
            return new String(data);
        }
    }

    @Override
    public void write(ByteBuf buf, String obj) {
        try {
            buf.writeBytes(obj.getBytes(ENCODING));
        } catch (UnsupportedEncodingException e) {
            buf.writeBytes(obj.getBytes());
        }
        buf.writeChar(0);
    }

}
