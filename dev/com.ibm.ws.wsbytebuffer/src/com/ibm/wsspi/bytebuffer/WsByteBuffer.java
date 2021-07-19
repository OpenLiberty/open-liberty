/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.bytebuffer;

import java.io.Serializable;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This interface contains methods for manipuating a WsByteBuffer.
 * WsByteBuffer is an enhancement to the Java ByteBuffer functionality.
 * <p>
 * Most of the APIs should be the same as the java.nio.ByteBuffer.
 * <p>
 * WsByteBuffers should be created through the WsByteBufferPoolManager and
 * should be released when no longer in use.
 * 
 * @see java.nio.ByteBuffer
 */
public interface WsByteBuffer extends Serializable {

    // Settings that the buffer action can be set to by user code, the
    // variable that keeps track of the buffer action is in the "root"
    // PooledWsByteBufferImpl object. Constants are here to make them available
    // to user code via the WsByteBuffer.
    int BUFFER_MGMT_COPY_ALL = 0;
    int BUFFER_MGMT_COPY_WHEN_NEEDED = 1;
    int BUFFER_MGMT_COPY_ALL_FINAL = 2;

    /**
     * Buffer action refers to how data is handled when crossing the JNI layer
     * as it can copy everything at once, partial data, etc.
     * 
     * @param value
     * @return boolean - whether the attempt worked
     */
    boolean setBufferAction(int value);

    /**
     * If the buffer has a backing byte[], this will provide direct access to
     * that.
     * If there is no array, then an exception is thrown.
     * 
     * @return byte[]
     * @see ByteBuffer#array()
     * @see ByteBuffer#hasArray()
     */
    byte[] array();

    /**
     * If the buffer has a backing byte[], then this will return the current
     * offset into that array.
     * 
     * @return int
     * @see ByteBuffer#arrayOffset()
     */
    int arrayOffset();

    /**
     * Compact this buffer by moving current data between position and limit to
     * begin at position zero. Position will now point to the next open spot and
     * limit is set to capacity to allow immediate puts.
     * 
     * @return WsByteBuffer
     * @see ByteBuffer#compact()
     */
    WsByteBuffer compact();

    /**
     * Compares this buffer to another object.
     * 
     * @param obj
     * @return int
     */
    int compareTo(Object obj);

    /**
     * Access the next two bytes of the buffer as a char.
     * 
     * @return char
     * @see ByteBuffer#getChar()
     */
    char getChar();

    /**
     * Access the two bytes starting at the input offset as a char.
     * 
     * @param index
     * @return char
     * @see ByteBuffer#getChar(int)
     */
    char getChar(int index);

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putChar(char)
     */
    WsByteBuffer putChar(char value);

    /**
     * Put the input value into the buffer at the input position.
     * 
     * @param index
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putChar(int, char)
     */
    WsByteBuffer putChar(int index, char value);

    /**
     * Put the input array of chars into the buffer at the current starting
     * position.
     * 
     * @param values
     * @return WsByteBuffer
     */
    WsByteBuffer putChar(char[] values);

    /**
     * Put the input array of chars into the buffer at the input starting
     * position.
     * 
     * @param values
     * @param off
     * @param len
     * @return WsByteBuffer
     */
    WsByteBuffer putChar(char[] values, int off, int len);

    /**
     * Access the next block of data from the current position as a double.
     * 
     * @return double
     * @see ByteBuffer#getDouble()
     */
    double getDouble();

    /**
     * Access the block of data at the input starting position as a double.
     * 
     * @param index
     * @return double
     * @see ByteBuffer#getDouble(int)
     */
    double getDouble(int index);

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putDouble(double)
     */
    WsByteBuffer putDouble(double value);

    /**
     * Put the input value into the buffer at the input position.
     * 
     * @param index
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putDouble(int, double)
     */
    WsByteBuffer putDouble(int index, double value);

    /**
     * Access the next block of data at the current position as a float.
     * 
     * @return float
     * @see ByteBuffer#getFloat()
     */
    float getFloat();

    /**
     * Access the next block of data at the input position as a float.
     * 
     * @param index
     * @return float
     * @see ByteBuffer#getFloat(int)
     */
    float getFloat(int index);

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putFloat(float)
     */
    WsByteBuffer putFloat(float value);

    /**
     * Put the input value into the buffer at the input position.
     * 
     * @param index
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putFloat(int, float)
     */
    WsByteBuffer putFloat(int index, float value);

    /**
     * Access the next block of data at the current position as an int.
     * 
     * @return int
     * @see ByteBuffer#getInt()
     */
    int getInt();

    /**
     * Access the next block of data at the input starting position as an int.
     * 
     * @param index
     * @return int
     * @see ByteBuffer#getInt(int)
     */
    int getInt(int index);

    /**
     * Put the input value into the buffer at the current starting position.
     * 
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putInt(int)
     */
    WsByteBuffer putInt(int value);

    /**
     * Put the input value into the buffer at the input starting position.
     * 
     * @param index
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putInt(int, int)
     */
    WsByteBuffer putInt(int index, int value);

    /**
     * Access the next block of data at the current position as a long.
     * 
     * @return long
     * @see ByteBuffer#getLong()
     */
    long getLong();

    /**
     * Access the next block of data at the input starting position as a long.
     * 
     * @param index
     * @return long
     * @see ByteBuffer#getLong(int)
     */
    long getLong(int index);

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putLong(long)
     */
    WsByteBuffer putLong(long value);

    /**
     * Put the input value into the buffer at the input starting position.
     * 
     * @param index
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putLong(int, long)
     */
    WsByteBuffer putLong(int index, long value);

    /**
     * Access the next block of data at the current position as a short.
     * 
     * @return short
     * @see ByteBuffer#getShort()
     */
    short getShort();

    /**
     * Access the next block of data at the input starting position as a short.
     * 
     * @param index
     * @return short
     * @see ByteBuffer#getShort(int)
     */
    short getShort(int index);

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putShort(short)
     */
    WsByteBuffer putShort(short value);

    /**
     * Put the input value into the buffer at the input starting position.
     * 
     * @param index
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#putShort(int, short)
     */
    WsByteBuffer putShort(int index, short value);

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param value
     * @return WsByteBuffer
     */
    WsByteBuffer putString(String value);

    /**
     * Check whether this buffer has a backing byte[].
     * 
     * @return boolean
     * @see ByteBuffer#hasArray()
     */
    boolean hasArray();

    /**
     * Query the current byte order in the buffer.
     * 
     * @return ByteOrder (BIG_ENDIAN vs LITTLE_ENDIAN)
     * @see ByteBuffer#order()
     */
    ByteOrder order();

    /**
     * Modifies this buffer's byte order to the input value.
     * 
     * @param bo
     * @return WsByteBuffer
     * @see ByteBuffer#order(ByteOrder)
     */
    WsByteBuffer order(ByteOrder bo);

    /**
     * Clear this buffer, which places limit at capacity and position at zero. Any
     * mark is discarded.
     * 
     * @return WsByteBuffer object after clear
     * @see Buffer#clear()
     */
    WsByteBuffer clear();

    /**
     * Query the upper capacity of this buffer.
     * 
     * @return int
     * @see Buffer#capacity()
     */
    int capacity();

    /**
     * Flip this buffer, which places the limit at the current position and the
     * position at zero. Any mark defined is discarded
     * 
     * @return WsByteBuffer after flipped
     * @see Buffer#flip()
     */
    WsByteBuffer flip();

    /**
     * Access the next byte at the current position.
     * 
     * @return byte
     * @see ByteBuffer#get()
     */
    byte get();

    /**
     * Query what the current position marker is for the buffer.
     * 
     * @return int
     * @see Buffer#position()
     */
    int position();

    /**
     * Sets the position mark in the ByteBuffer. This must be greater than
     * or equal to zero but less than or equal to the current limit mark.
     * 
     * @param index
     * @return the WsByteBuffer object
     * @throws IllegalArgumentException
     *             - if outside the proper range
     * @see Buffer#position(int)
     */
    WsByteBuffer position(int index);

    /**
     * Sets the limit mark in the ByteBuffer to the input value. This must be
     * greater than or equal to the current position but less than or equal
     * to the capacitiy or an exception will be thrown.
     * 
     * @param index
     * @return the WsByteBuffer object
     * @throws IllegalArgumentException
     *             - if outside the proper range
     * @see Buffer#limit(int)
     */
    WsByteBuffer limit(int index);

    /**
     * Query the current limit of this buffer. It will be greater than or
     * equals to the position but less than or equal to the capacity.
     * 
     * @return int
     * @see Buffer#limit()
     */
    int limit();

    /**
     * Return the current amount of space between the position and limit of
     * the buffer.
     * 
     * @return int
     * @see Buffer#remaining()
     */
    int remaining();

    /**
     * Sets the mark to the current position.
     * 
     * @return WsByteBuffer
     * @see Buffer#mark()
     */
    WsByteBuffer mark();

    /**
     * Resets the position to the previously saved mark.
     * 
     * @return WsByteBuffer
     * @see Buffer#reset()
     */
    WsByteBuffer reset();

    /**
     * Rewinds this buffer by setting position to zero and discarding any
     * saved mark.
     * 
     * @return WsByteBuffer
     * @see Buffer#rewind()
     */
    WsByteBuffer rewind();

    /**
     * Returns whether this buffer is read only or not. Note that this is not
     * the WsByteBuffer itself, but the internal buffer - use
     * getReadOnly/setReadOnly
     * for the WsByteBuffer wrapper.
     * 
     * @return boolean
     * @see Buffer#isReadOnly()
     */
    boolean isReadOnly();

    /**
     * Query whether there is any current data between position and limit.
     * 
     * @return boolean
     * @see Buffer#hasRemaining()
     */
    boolean hasRemaining();

    /**
     * Create a second buffer that points to the same entire content. The
     * position, limit,
     * etc are independent between the buffers but the content is shared.
     * 
     * @return WsByteBuffer
     * @see ByteBuffer#duplicate()
     */
    WsByteBuffer duplicate();

    /**
     * Slice a second buffer which has access to the content between the current
     * position and limit. The new buffer's capacity will be the amount of space
     * sliced. Slices of buffers that have backing arrays must be careful to use
     * the array-offset when using that byte[] as the sliced buffer's position
     * is array-offset+position as the actual offset into the array.
     * 
     * @return WsByteBuffer
     * @see ByteBuffer#slice()
     */
    WsByteBuffer slice();

    /**
     * Extract data into the output array from the current position.
     * 
     * @param dst
     *            destination byte array
     * @return WsByteBuffer
     * @see ByteBuffer#get(byte[])
     */
    WsByteBuffer get(byte[] dst);

    /**
     * Extract data into the output array from the current position and using
     * the input output offset and overall length.
     * 
     * @param dst
     *            destination byte array
     * @param offset
     *            into the output array
     * @param length
     *            to extract
     * @return WsByteBuffer
     * @see ByteBuffer#get(byte[], int, int)
     */
    WsByteBuffer get(byte[] dst, int offset, int length);

    /**
     * Extract one byte from the buffer using the input position.
     * 
     * @param index
     * @return byte
     * @see ByteBuffer#get(int)
     */
    byte get(int index);

    /**
     * Tells if the buffer is direct or not.
     * 
     * @return boolean
     * @see ByteBuffer#isDirect()
     */
    boolean isDirect();

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#put(byte)
     */
    WsByteBuffer put(byte value);

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#put(byte[])
     */
    WsByteBuffer put(byte[] value);

    /**
     * Put the input value into the buffer at the current position using the input
     * source offset and length.
     * 
     * @param src
     *            byte array
     * @param offset
     *            into the source value
     * @param length
     *            to copy
     * @return WsByteBuffer
     * @see ByteBuffer#put(byte[], int, int)
     */
    WsByteBuffer put(byte[] src, int offset, int length);

    /**
     * Put the input value into the buffer at the input position.
     * 
     * @param index
     * @param value
     * @return WsByteBuffer
     * @see ByteBuffer#put(int, byte)
     */
    WsByteBuffer put(int index, byte value);

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param src
     *            ByteBuffer
     * @return WsByteBuffer
     * @see ByteBuffer#put(ByteBuffer)
     */
    WsByteBuffer put(ByteBuffer src);

    /**
     * Put the input value into the buffer at the current position.
     * 
     * @param src
     *            WsByteBuffer
     * @return WsByteBuffer
     * @see ByteBuffer#put(ByteBuffer)
     */
    WsByteBuffer put(WsByteBuffer src);

    /**
     * Put the input array of buffers into this one at the current position.
     * 
     * @param src
     * @return WsByteBuffer
     */
    WsByteBuffer put(WsByteBuffer[] src);

    /**
     * Get the real ByteBuffer associated with this wrapper.
     * 
     * @return ByteBuffer
     */
    ByteBuffer getWrappedByteBuffer();

    /**
     * Get the real ByteBuffer associated with this wrapper.
     * 
     * @return ByteBuffer
     */
    ByteBuffer getWrappedByteBufferNonSafe();

    /**
     * Mark this buffer as read only. This only applies to the WsByteBuffer
     * wrapper
     * and not the internal ByteBuffer
     * 
     * @param value
     */
    void setReadOnly(boolean value);

    /**
     * Query the read only setting for this WsByteBuffer.
     * 
     * @return boolean
     */
    boolean getReadOnly();

    /**
     * If this buffer is known to be held for a long time, then it can
     * be removed from the leak detection logic, so as not to create
     * a false-positive leak detection hit against this buffer.
     */
    void removeFromLeakDetection();

    /**
     * Release the buffer by telling the pool manager that we are done with it.
     * safeguard that this release will not be called twice per instance of this
     * object
     */
    void release();

    int TYPE_WsByteBuffer = 0;
    int TYPE_FCWsByteBuffer = 1;

    /**
     * Return the buffer type. Used internally for optimizing performance.
     * 
     * @return int
     */
    int getType();

    // status should be a treated as binary flags, so it can be expanded later
    /** Indicates filechannel buffer is already converted to regular buffer */
    int STATUS_BUFFER = 0x1;
    /** Indicates filechannel buffer still able to use transferTo api */
    int STATUS_TRANSFER_TO = 0x2;

    /**
     * Return the buffer status. Used internally for optimizing performance.
     * 
     * @return int
     */
    int getStatus();

    /**
     * Set the buffer status. Used internally for optimizing performance.
     * 
     * @param value
     *            new buffer status
     */
    void setStatus(int value);

}
