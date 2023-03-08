/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.utils.internal;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;
import com.ibm.ws.zos.core.utils.NativeUtils;

/**
 * This class is intended to manage a set of overlapping 2GB {@code DirectByteBuffer}s mapped
 * on demand for each GB of native memory that is referenced.
 */
@Trivial
public class DirectBufferHelperImpl implements DirectBufferHelper {

    /**
     * The bit mask to be ANDed with an address to get the segment base.
     */
    protected static final long GIGABYTE_MASK = -1L << 30;

    /**
     * The bit mask to be ANDed with an address to get the segment offset.
     */
    protected static final long GIGABYTE_OFFSET_MASK = GIGABYTE_MASK ^ -1;

    /**
     * Simple key object that implements {@code hashCode} and {@code equals} efficiently.
     */
    @Trivial
    protected final static class BufferKey {
        final long baseAddress;
        final int hashCode;

        BufferKey(long baseAddress) {
            this.baseAddress = baseAddress;
            this.hashCode = (int) (baseAddress >> 30);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof BufferKey) {
                BufferKey that = (BufferKey) o;
                return this.baseAddress == that.baseAddress;
            }
            return false;
        }
    }

    /**
     * Thread specific map of direct buffers that map memory segments. A thread
     * local is used to avoid synchronization of various {@code ByteBuffer} methods
     * during slicing.
     */
    protected final ThreadLocal<Map<BufferKey, ByteBuffer>> segments = new ThreadLocal<Map<BufferKey, ByteBuffer>>() {
        @Override
        @Trivial
        public Map<BufferKey, ByteBuffer> initialValue() {
            return new HashMap<BufferKey, ByteBuffer>();
        }
    };

    /**
     * Simple holder class for a {@code ByteBuffer} that tracks its
     * base address.
     */
    protected final static class BufferHolder {
        long base;
        ByteBuffer buffer;
    }

    /**
     * Thread based reference to the most recently used buffer segment.
     */
    protected final ThreadLocal<BufferHolder> recentBuffer = new ThreadLocal<BufferHolder>();

    /**
     * Native Utility object reference.
     */
    private NativeUtils nativeUtils = null;

    /**
     * Default constructor to enable extension in test and needed for OSGi instantiation
     */
    public DirectBufferHelperImpl() {
    }

    /**
     * DS method to activate this component.
     *
     * @param properties
     *
     * @throws Exception
     */
    protected void activate(Map<String, Object> properties) throws Exception {
    }

    /**
     * DS method to deactivate this component.
     *
     * @param reason The representation of reason the component is stopping
     */
    protected void deactivate() {
    }

    /**
     * Sets the NativeUtils object reference.
     *
     * @param nativeUtils The NativeUtils reference.
     */
    protected void setNativeUtils(NativeUtils nativeUtils) {
        this.nativeUtils = nativeUtils;
    }

    /**
     * Unsets the NativeUtils object reference.
     *
     * @param nativeUtils The NativeUtils reference.
     */
    protected void unsetNativeUtils(NativeUtils nativeUtils) {
        if (this.nativeUtils == nativeUtils) {
            this.nativeUtils = null;
        }
    }

    /**
     * Get a {@code ByteBuffer} based at {@code address} of the specified
     * length. The buffer is returned with the platform's native byte order.
     * This method can be used as an alternative to <code>NewDirectByteBuffer</code>
     * calls in native to create a direct {@code ByteBuffer} to map an address.
     *
     * @param address the base of the buffer
     * @param length  the length of the buffer
     *
     * @return a direct {@code ByteBuffer} that maps the requested region
     */
    @Override
    public ByteBuffer getSlice(long address, int length) {
        if (length > GIGABYTE) {
            throw new IllegalArgumentException("Length must not exceed 1 GB");
        }
        int segmentOffset = getSegmentOffset(address);
        ByteBuffer segment = getSegment(address);
        segment.limit(segment.capacity()).position(segmentOffset).limit(segmentOffset + length);
        return segment.slice();
    }

    /**
     * Read the value of the byte at the specified address.
     *
     * @param address the address of the byte
     *
     * @return the byte at the specified address
     */
    @Override
    public byte get(long address) {
        return getSegment(address).get(getSegmentOffset(address));
    }

    /**
     * Perform a bulk get of the data at the specified address into the
     * provided byte array. The length of the provided array determines
     * the number of bytes to be read.
     *
     * @param address the address of the data to read
     * @param dest    the target byte array
     *
     * @see #get(long, byte[], int, int)
     */
    @Override
    public void get(long address, byte[] dest) {
        get(address, dest, 0, dest.length);
    }

    /**
     * Perform a bulk read of the data at the specified address into the
     * provided byte array at the specified offset for the specified length.
     *
     * @param address the address of the data to read
     * @param dest    the target byte array
     * @param offset  the offset into the target byte array
     * @param length  the number of bytes to transfer
     */
    @Override
    public void get(long address, byte[] dest, int offset, int length) {
        if (dest.length > GIGABYTE) {
            throw new IllegalArgumentException("Copy must not exceed 1 GB");
        }
        ByteBuffer segment = getSegment(address);
        segment.limit(segment.capacity()).position(getSegmentOffset(address));
        segment.get(dest, offset, length);
    }

    /**
     * Get the value of the {@code char} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code char} to read
     *
     * @return the value of the {@code char} at the specified address
     */
    @Override
    public char getChar(long address) {
        return getSegment(address).getChar(getSegmentOffset(address));
    }

    /**
     * Get the value of the {@code double} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code double} to read
     *
     * @return the value of the {@code double} at the specified address
     */
    @Override
    public double getDouble(long address) {
        return getSegment(address).getDouble(getSegmentOffset(address));
    }

    /**
     * Get the value of the {@code float} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code float} to read
     *
     * @return the value of the {@code float} at the specified address
     */
    @Override
    public float getFloat(long address) {
        return getSegment(address).getFloat(getSegmentOffset(address));
    }

    /**
     * Get the value of the {@code int} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code int} to read
     *
     * @return the value of the {@code int} at the specified address
     */
    @Override
    public int getInt(long address) {
        return getSegment(address).getInt(getSegmentOffset(address));
    }

    /**
     * Get the value of the {@code long} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code long} to read
     *
     * @return the value of the {@code long} at the specified address
     */
    @Override
    public long getLong(long address) {
        return getSegment(address).getLong(getSegmentOffset(address));
    }

    /**
     * Get the value of the {@code short} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code short} to read
     *
     * @return the value of the {@code short} at the specified address
     */
    @Override
    public short getShort(long address) {
        return getSegment(address).getShort(getSegmentOffset(address));
    }

    /**
     * Get or create a direct {@code ByteBuffer} segment that is
     * based at the specified address.
     *
     * @param address the base address of the buffer
     *
     * @return the direct {@code ByteBuffer} mapping an area at the
     *         specified address
     */
    @Override
    public ByteBuffer getSegment(long address) {
        BufferHolder holder = recentBuffer.get();
        long base = getSegmentBase(address);
        if (holder != null && holder.base == base) {
            return holder.buffer;
        }

        BufferKey key = new BufferKey(base);
        ByteBuffer buffer = segments.get().get(key);
        if (buffer == null) {
            holder = new BufferHolder();
            holder.base = base;
            holder.buffer = buffer = mapDirectByteBuffer(base, Integer.MAX_VALUE);
            segments.get().put(key, holder.buffer);
            recentBuffer.set(holder);
        }
        return buffer;
    }

    /**
     * Get the segment base of the specified address.
     *
     * @param address the native address
     *
     * @return the base address of the segment
     */
    protected long getSegmentBase(long address) {
        return address & GIGABYTE_MASK;
    }

    /**
     * Get the offset from the segment base of the specified address.
     *
     * @param addres the native address
     *
     * @return the offset from segment base of the address
     */
    protected int getSegmentOffset(long address) {
        return (int) (address & GIGABYTE_OFFSET_MASK);
    }

    protected ByteBuffer mapDirectByteBuffer(final long address, final int size) {
        return nativeUtils.mapDirectByteBuffer(address, size);
    }
}
