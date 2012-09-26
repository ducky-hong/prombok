package io.prombok.buffer;

import io.netty.buffer.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class GenericByteBuf implements WrappedByteBuf {

    private final ByteBuf buf;
    
    private static final Map<Class<? extends GenericByteReader>, GenericByteReader> readers
            = new HashMap<Class<? extends GenericByteReader>, GenericByteReader>();
    private static final Map<Class<? extends GenericByteWriter>, GenericByteWriter> writers
            = new HashMap<Class<? extends GenericByteWriter>, GenericByteWriter>();

    public GenericByteBuf(ByteBuf buf) {
        if (buf == null) {
            throw new NullPointerException("buf");
        }
        this.buf = buf;
    }

    public static <T> GenericByteReader<T> getReader(Class<? extends GenericByteReader<T>> cls) {
        GenericByteReader reader = readers.get(cls);
        if (reader == null) {
            try {
                reader = cls.newInstance();
                readers.put(cls, reader);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return reader;
    }

    public static <T> GenericByteWriter<T> getWriter(Class<? extends GenericByteWriter<T>> cls) {
        GenericByteWriter writer = writers.get(cls);
        if (writer == null) {
            try {
                writer = cls.newInstance();
                writers.put(cls, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return writer;
    }

    public <T> T readGeneric(Class<? extends GenericByteReader<T>> cls) {
        return getReader(cls).read(buf);
    }
    
    public <T> void writeGeneric(T obj, Class<? extends GenericByteWriter<T>> cls) {
        getWriter(cls).write(buf, obj);
    }

    @Override
    public ByteBuf unwrap() {
        return buf;
    }

    @Override
    public int capacity() {
        return buf.capacity();
    }

    @Override
    public void capacity(int newCapacity) {
        buf.capacity(newCapacity);
    }

    @Override
    public int maxCapacity() {
        return buf.maxCapacity();
    }

    @Override
    public ByteOrder order() {
        return buf.order();
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        return buf.order(endianness);
    }

    @Override
    public boolean isDirect() {
        return buf.isDirect();
    }

    @Override
    public int readerIndex() {
        return buf.readerIndex();
    }

    @Override
    public void readerIndex(int readerIndex) {
        buf.readerIndex(readerIndex);
    }

    @Override
    public int writerIndex() {
        return buf.writerIndex();
    }

    @Override
    public void writerIndex(int writerIndex) {
        buf.writerIndex(writerIndex);
    }

    @Override
    public void setIndex(int readerIndex, int writerIndex) {
        buf.setIndex(readerIndex, writerIndex);
    }

    @Override
    public int readableBytes() {
        return buf.readableBytes();
    }

    @Override
    public int writableBytes() {
        return buf.writableBytes();
    }

    @Override
    public boolean readable() {
        return buf.readable();
    }

    @Override
    public boolean writable() {
        return buf.writable();
    }

    @Override
    public void clear() {
        buf.clear();
    }

    @Override
    public void markReaderIndex() {
        buf.markReaderIndex();
    }

    @Override
    public void resetReaderIndex() {
        buf.resetReaderIndex();
    }

    @Override
    public void markWriterIndex() {
        buf.markWriterIndex();
    }

    @Override
    public void resetWriterIndex() {
        buf.resetWriterIndex();
    }

    @Override
    public void discardReadBytes() {
        buf.discardReadBytes();
    }

    @Override
    public void ensureWritableBytes(int minWritableBytes) {
        buf.ensureWritableBytes(minWritableBytes);
    }

    @Override
    public int ensureWritableBytes(int minWritableBytes, boolean force) {
        return buf.ensureWritableBytes(minWritableBytes, force);
    }

    @Override
    public boolean getBoolean(int index) {
        return buf.getBoolean(index);
    }

    @Override
    public byte getByte(int index) {
        return buf.getByte(index);
    }

    @Override
    public short getUnsignedByte(int index) {
        return buf.getUnsignedByte(index);
    }

    @Override
    public short getShort(int index) {
        return buf.getShort(index);
    }

    @Override
    public int getUnsignedShort(int index) {
        return buf.getUnsignedShort(index);
    }

    @Override
    public int getMedium(int index) {
        return buf.getMedium(index);
    }

    @Override
    public int getUnsignedMedium(int index) {
        return buf.getUnsignedMedium(index);
    }

    @Override
    public int getInt(int index) {
        return buf.getInt(index);
    }

    @Override
    public long getUnsignedInt(int index) {
        return buf.getUnsignedInt(index);
    }

    @Override
    public long getLong(int index) {
        return buf.getLong(index);
    }

    @Override
    public char getChar(int index) {
        return buf.getChar(index);
    }

    @Override
    public float getFloat(int index) {
        return buf.getFloat(index);
    }

    @Override
    public double getDouble(int index) {
        return buf.getDouble(index);
    }

    @Override
    public void getBytes(int index, ByteBuf dst) {
        buf.getBytes(index, dst);
    }

    @Override
    public void getBytes(int index, ByteBuf dst, int length) {
        buf.getBytes(index, dst, length);
    }

    @Override
    public void getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        buf.getBytes(index, dst, dstIndex, length);
    }

    @Override
    public void getBytes(int index, byte[] dst) {
        buf.getBytes(index, dst);
    }

    @Override
    public void getBytes(int index, byte[] dst, int dstIndex, int length) {
        buf.getBytes(index, dst, dstIndex, length);
    }

    @Override
    public void getBytes(int index, ByteBuffer dst) {
        buf.getBytes(index, dst);
    }

    @Override
    public void getBytes(int index, OutputStream out, int length) throws IOException {
        buf.getBytes(index, out, length);
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        return buf.getBytes(index, out, length);
    }

    @Override
    public void setBoolean(int index, boolean value) {
        buf.setBoolean(index, value);
    }

    @Override
    public void setByte(int index, int value) {
        buf.setByte(index, value);
    }

    @Override
    public void setShort(int index, int value) {
        buf.setShort(index, value);
    }

    @Override
    public void setMedium(int index, int value) {
        buf.setMedium(index, value);
    }

    @Override
    public void setInt(int index, int value) {
        buf.setInt(index, value);
    }

    @Override
    public void setLong(int index, long value) {
        buf.setLong(index, value);
    }

    @Override
    public void setChar(int index, int value) {
        buf.setChar(index, value);
    }

    @Override
    public void setFloat(int index, float value) {
        buf.setFloat(index, value);
    }

    @Override
    public void setDouble(int index, double value) {
        buf.setDouble(index, value);
    }

    @Override
    public void setBytes(int index, ByteBuf src) {
        buf.setBytes(index, src);
    }

    @Override
    public void setBytes(int index, ByteBuf src, int length) {
        buf.setBytes(index, src, length);
    }

    @Override
    public void setBytes(int index, ByteBuf src, int srcIndex, int length) {
        buf.setBytes(index, src, srcIndex, length);
    }

    @Override
    public void setBytes(int index, byte[] src) {
        buf.setBytes(index, src);
    }

    @Override
    public void setBytes(int index, byte[] src, int srcIndex, int length) {
        buf.setBytes(index, src, srcIndex, length);
    }

    @Override
    public void setBytes(int index, ByteBuffer src) {
        buf.setBytes(index, src);
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        return buf.setBytes(index, in, length);
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        return buf.setBytes(index, in, length);
    }

    @Override
    public void setZero(int index, int length) {
        buf.setZero(index, length);
    }

    @Override
    public boolean readBoolean() {
        return buf.readBoolean();
    }

    @Override
    public byte readByte() {
        return buf.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return buf.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return buf.readShort();
    }

    @Override
    public int readUnsignedShort() {
        return buf.readUnsignedShort();
    }

    @Override
    public int readMedium() {
        return buf.readMedium();
    }

    @Override
    public int readUnsignedMedium() {
        return buf.readUnsignedMedium();
    }

    @Override
    public int readInt() {
        return buf.readInt();
    }

    @Override
    public long readUnsignedInt() {
        return buf.readUnsignedInt();
    }

    @Override
    public long readLong() {
        return buf.readLong();
    }

    @Override
    public char readChar() {
        return buf.readChar();
    }

    @Override
    public float readFloat() {
        return buf.readFloat();
    }

    @Override
    public double readDouble() {
        return buf.readDouble();
    }

    @Override
    public ByteBuf readBytes(int length) {
        return buf.readBytes(length);
    }

    @Override
    public ByteBuf readSlice(int length) {
        return buf.readSlice(length);
    }

    @Override
    public void readBytes(ByteBuf dst) {
        buf.readBytes(dst);
    }

    @Override
    public void readBytes(ByteBuf dst, int length) {
        buf.readBytes(dst, length);
    }

    @Override
    public void readBytes(ByteBuf dst, int dstIndex, int length) {
        buf.readBytes(dst, dstIndex, length);
    }

    @Override
    public void readBytes(byte[] dst) {
        buf.readBytes(dst);
    }

    @Override
    public void readBytes(byte[] dst, int dstIndex, int length) {
        buf.readBytes(dst, dstIndex, length);
    }

    @Override
    public void readBytes(ByteBuffer dst) {
        buf.readBytes(dst);
    }

    @Override
    public void readBytes(OutputStream out, int length) throws IOException {
        buf.readBytes(out, length);
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        return buf.readBytes(out, length);
    }

    @Override
    public void skipBytes(int length) {
        buf.skipBytes(length);
    }

    @Override
    public void writeBoolean(boolean value) {
        buf.writeBoolean(value);
    }

    @Override
    public void writeByte(int value) {
        buf.writeByte(value);
    }

    @Override
    public void writeShort(int value) {
        buf.writeShort(value);
    }

    @Override
    public void writeMedium(int value) {
        buf.writeMedium(value);
    }

    @Override
    public void writeInt(int value) {
        buf.writeInt(value);
    }

    @Override
    public void writeLong(long value) {
        buf.writeLong(value);
    }

    @Override
    public void writeChar(int value) {
        buf.writeChar(value);
    }

    @Override
    public void writeFloat(float value) {
        buf.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) {
        buf.writeDouble(value);
    }

    @Override
    public void writeBytes(ByteBuf src) {
        buf.writeBytes(src);
    }

    @Override
    public void writeBytes(ByteBuf src, int length) {
        buf.writeBytes(src, length);
    }

    @Override
    public void writeBytes(ByteBuf src, int srcIndex, int length) {
        buf.writeBytes(src, srcIndex, length);
    }

    @Override
    public void writeBytes(byte[] src) {
        buf.writeBytes(src);
    }

    @Override
    public void writeBytes(byte[] src, int srcIndex, int length) {
        buf.writeBytes(src, srcIndex, length);
    }

    @Override
    public void writeBytes(ByteBuffer src) {
        buf.writeBytes(src);
    }

    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        return buf.writeBytes(in, length);
    }

    @Override
    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        return buf.writeBytes(in, length);
    }

    @Override
    public void writeZero(int length) {
        buf.writeZero(length);
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, byte value) {
        return buf.indexOf(fromIndex, toIndex, value);
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, ByteBufIndexFinder indexFinder) {
        return buf.indexOf(fromIndex, toIndex, indexFinder);
    }

    @Override
    public int bytesBefore(byte value) {
        return buf.bytesBefore(value);
    }

    @Override
    public int bytesBefore(ByteBufIndexFinder indexFinder) {
        return buf.bytesBefore(indexFinder);
    }

    @Override
    public int bytesBefore(int length, byte value) {
        return buf.bytesBefore(length, value);
    }

    @Override
    public int bytesBefore(int length, ByteBufIndexFinder indexFinder) {
        return buf.bytesBefore(length, indexFinder);
    }

    @Override
    public int bytesBefore(int index, int length, byte value) {
        return buf.bytesBefore(index, length, value);
    }

    @Override
    public int bytesBefore(int index, int length, ByteBufIndexFinder indexFinder) {
        return buf.bytesBefore(index, length, indexFinder);
    }

    @Override
    public ByteBuf copy() {
        return buf.copy();
    }

    @Override
    public ByteBuf copy(int index, int length) {
        return buf.copy(index, length);
    }

    @Override
    public ByteBuf slice() {
        return buf.slice();
    }

    @Override
    public ByteBuf slice(int index, int length) {
        return buf.slice(index, length);
    }

    @Override
    public ByteBuf duplicate() {
        return buf.duplicate();
    }

    @Override
    public boolean hasNioBuffer() {
        return buf.hasNioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return buf.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        return buf.nioBuffer(index, length);
    }

    @Override
    public boolean hasNioBuffers() {
        return buf.hasNioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return buf.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int offset, int length) {
        return buf.nioBuffers(offset, length);
    }

    @Override
    public boolean hasArray() {
        return buf.hasArray();
    }

    @Override
    public byte[] array() {
        return buf.array();
    }

    @Override
    public int arrayOffset() {
        return buf.arrayOffset();
    }

    @Override
    public String toString(Charset charset) {
        return buf.toString(charset);
    }

    @Override
    public String toString(int index, int length, Charset charset) {
        return buf.toString(index, length, charset);
    }

    @Override
    public int hashCode() {
        return buf.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ByteBuf) {
            return ByteBufUtil.equals(this, (ByteBuf) obj);
        }
        return false;
    }

    @Override
    public int compareTo(ByteBuf buffer) {
        return ByteBufUtil.compare(this, buffer);
    }

    @Override
    public String toString() {
        return "Generic(" + buf.toString() + ")";
    }

    @Override
    public Unsafe unsafe() {
        return buf.unsafe();
    }

    @Override
    public ChannelBufType type() {
        return buf.type();
    }

    @Override
    public boolean isPooled() {
        return buf.isPooled();
    }
}
