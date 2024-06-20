/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 * ByteBufferVector provides a layer of abstraction over a set of byte arrays read in from
 * the local comm channel. It provides ByteBuffer-like methods for seamlessly reading data
 * from the backing byte arrays.
 *
 * Only a portion of the data between each byte array is included in the ByteBufferVector.
 * This functions similarly to the position and limit of a java.nio.ByteBuffer.
 *
 * The ByteBufferVector shares the content of the byte arrays (i.e the data is not copied);
 * however the ByteBufferVector's internal state (position, limit, etc) is independent of the
 * given byte arrays.
 *
 * The capacity of the ByteBufferVector is equal to the sum of the useable bytes in each
 * of the given byte arrays at the time the ByteBufferVector is created.
 *
 * The limit of the ByteBufferVector is initially equal to its capacity.
 *
 * The initial position of the ByteBufferVector is 0.
 *
 * (Note: Cannot extend ByteBuffer. ByteBuffer has package protected abstract methods, making it
 * impossible to extend outside of java.nio.)
 */
@Trivial
public final class ByteBufferVector {

    /**
     * The size of a java short, in bytes.
     */
    private static final int SIZEOF_SHORT = Short.SIZE / Byte.SIZE;

    /**
     * The size of a java int, in bytes.
     */
    private static final int SIZEOF_INT = Integer.SIZE / Byte.SIZE;

    /**
     * The size of a java long, in bytes.
     */
    private static final int SIZEOF_LONG = Long.SIZE / Byte.SIZE;

    /**
     * The backing array of byte buffers.
     *
     * The backing buffers start off "normalized" - i.e. every buffer initially
     * has position = 0 and limit = capacity (this is accomplished using slice()
     * on the ByteBuffers passed to the CTOR).
     *
     * If a new limit is set, then every ByteBuffer before the limit will have
     * limit = capacity, the ByteBuffer in which the new limit falls will be
     * set with the new limit (thus, limit <= capacity for that buffer), and
     * all subsequent buffers will have limit = 0.
     *
     * If a new position is set, then every ByteBuffer before the position will
     * have position = limit, the buffer in which the new position falls will
     * be set with the new position (thus, position >= 0), and all subsequent
     * buffers will have position = 0.
     *
     */
    private final List<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>(5);

    /**
     * The viewable length of the ByteBuffer collection.
     */
    private int viewableLength = 0;

    /**
     * CTOR.
     *
     * @param byteArray The data buffer to wrap.
     */
    public ByteBufferVector(byte[] byteArray) {
        byteBuffers.add(new ByteBuffer(byteArray, 0, byteArray.length));
        viewableLength = byteArray.length;
    }

    /**
     * Addition CTOR
     *
     * @param base      The existing data
     * @param byteArray The new data
     */
    public ByteBufferVector(ByteBufferVector base, byte[] byteArray) {
        this.byteBuffers.addAll(base.byteBuffers);
        viewableLength = base.getLength();
        append(byteArray);
    }

    /**
     * Copy constructor, used by split().
     */
    private ByteBufferVector(ByteBufferVector base, int startingIndex, int length) {
        int bytesLeftToSkip = startingIndex;
        int bytesLeftToCopy = length;

        List<ByteBuffer> oldBuffers = base.byteBuffers;
        for (ByteBuffer buffer : oldBuffers) {
            if (bytesLeftToSkip >= buffer.viewableLength) {
                bytesLeftToSkip -= buffer.viewableLength;
            } else {
                int currentBufferStartingIndex = buffer.firstByteIndex + bytesLeftToSkip;
                int currentBufferBytesAvailable = buffer.viewableLength - bytesLeftToSkip;

                // Add the new buffer to the vector
                if (bytesLeftToCopy >= currentBufferBytesAvailable) {
                    this.byteBuffers.add(new ByteBuffer(buffer.data, currentBufferStartingIndex, currentBufferBytesAvailable));
                    bytesLeftToCopy -= currentBufferBytesAvailable;
                    bytesLeftToSkip = 0;
                } else {
                    this.byteBuffers.add(new ByteBuffer(buffer.data, currentBufferStartingIndex, bytesLeftToCopy));
                    bytesLeftToCopy = 0;
                    bytesLeftToSkip = 0;
                }

                if (bytesLeftToCopy == 0) {
                    break;
                }
            }
        }

        this.viewableLength = length;
    }

    /**
     * Get the total viewable length (of all the buffers).
     */
    public int getLength() {
        return viewableLength;
    }

    /**
     * Append some more bytes to the byte buffer vector.
     */
    public ByteBufferVector append(byte[] bytes) {
        if ((bytes != null) && (bytes.length > 0)) {
            byteBuffers.add(new ByteBuffer(bytes, 0, bytes.length));
            viewableLength += bytes.length;
        }
        return this;
    }

    /**
     * Absolute get.
     *
     * @param index
     *
     * @return the byte at the given index.
     *
     * @throws IndexOutOfBoundsException if the given index is out of range.
     */
    public byte get(int index) {
        int i = index;

        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            if (i < byteBuffer.viewableLength) {
                return byteBuffer.get(i);
            }
            i -= byteBuffer.viewableLength;
        }
        throw new IndexOutOfBoundsException("Tried to get byte at index " + index + " from " + this);
    }

    /**
     * Absolute put.
     *
     * @param index
     * @param value
     *
     * @return this
     *
     * @throws IndexOutOfBoundsException if the given index is out of range.
     */
    public ByteBufferVector put(int index, byte value) {
        int i = index;
        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            if (i < byteBuffer.viewableLength) {
                byteBuffer.put(i, value);
                return this;
            }
            i -= byteBuffer.viewableLength;
        }
        throw new IndexOutOfBoundsException("Tried to put byte at index " + index + " with value " + value + " into " + this);
    }

    /**
     * Absolute put with byte[].
     *
     * @param index
     * @param value  - the bytes to put
     * @param offset -
     *
     * @return this
     *
     * @throws IndexOutOfBoundsException if the given index + value.length is out of range.
     */
    public ByteBufferVector put(int index, byte[] value) {

        if (value == null) {
            return this;
        }

        int offset = 0;
        int length = value.length;

        if (index + length > getLength()) {
            throw new IndexOutOfBoundsException("Tried to put byte[] of length " + length + " at index " + index + " into " + this);
        }

        // "index" is the absolute index within the entire ByteBufferVector.
        // "i" is used as a relative index for each ByteBuffer in the vector.
        // So i starts equal to index, then as we iterate thru each ByteBuffer
        // we subtract ByteBuffer.limit() from i, to keep track of where the
        // absolute "index" falls within the current ByteBuffer.
        int i = index;

        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            if (i < byteBuffer.viewableLength) {
                // The bytes at least *start* in this buffer.

                if (i + length <= byteBuffer.viewableLength) {
                    // The bytes are contained in this byteBuffer.
                    byteBuffer.put(i, value, offset, length);
                    return this;
                } else {
                    // The bytes span multiple byteBuffers. Put as much as we can in
                    // this buffer, then adjust offset/length and loop
                    int lengthToPut = byteBuffer.viewableLength - i; // how much room we have in this buffer
                    byteBuffer.put(i, value, offset, lengthToPut);

                    i += lengthToPut; // same as i = byteBuffer.limit()
                    offset += lengthToPut;
                    length -= lengthToPut;
                }
            }

            // Subtract this buffer's limit from the index and move on to the next buffer.
            i -= byteBuffer.viewableLength;
        }

        // Should never get here.
        throw new IndexOutOfBoundsException("Tried to put byte[] of length " + length + " at index " + index + " into " + this);
    }

    /**
     * @return The unsigned byte value (as an int) at the given index.
     */
    private int getub(int index) {
        return (get(index) & 0xff);
    }

    /**
     * Absolute get, into the given byte[].
     *
     * @return this
     *
     * @throws BufferUnderflowException if there are fewer than dst.length bytes remaining
     *                                      in this ByteBufferVector from the given index.
     */
    public ByteBufferVector get(int index, byte[] dst) {
        int bytesLeftToCopy = dst.length;
        if ((index + bytesLeftToCopy) > this.viewableLength) {
            throw new BufferUnderflowException();
            //IndexOutOfBoundsException("Tried to get bytes at index " + index + " from " + this);
        }

        int i = index;

        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            int currentBufferLimit = byteBuffer.viewableLength;
            if (i < currentBufferLimit) {
                // There are bytes in this buffer to copy.
                int bytesAvailableThisRound = Math.min(currentBufferLimit - i, bytesLeftToCopy);
                byteBuffer.get(i, dst, dst.length - bytesLeftToCopy, bytesAvailableThisRound);
                bytesLeftToCopy -= bytesAvailableThisRound;
                if (bytesLeftToCopy == 0) {
                    break;
                }
            }

            // Subtract this buffer's limit from the index and move on to the next buffer.
            i = Math.max(i - currentBufferLimit, 0);
        }

        return this;
    }

    /**
     * Absolute get.
     *
     * @param index
     *
     * @return the short at the given index (returns an int since
     *         Java does not have unsigned shorts).
     *
     * @throws IndexOutOfBoundsException if the given index is out of range.
     */
    public int getUnsignedShort(int index) {

        // We removed the 'fail fast' code here as it was too expensive.  We'll fail later.
        int i = index;

        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            if (i < byteBuffer.viewableLength) {
                // The short at least *starts* in this buffer.

                if (i + (SIZEOF_SHORT - 1) < byteBuffer.viewableLength) {
                    // The short is contained in this byteBuffer.
                    return toUnsignedShort(byteBuffer.getShort(i));
                } else {
                    // The short spans two byteBuffers. Oy vey!
                    return (getub(index) << 8) + getub(index + 1);
                }
            }

            // Subtract this buffer's limit from the index and move on to the next buffer.
            i -= byteBuffer.viewableLength;
        }

        // Should never get here.
        throw new IndexOutOfBoundsException("Tried to get short at index " + index + " from " + this);
    }

    /**
     * Convert the given signed short value to an unsigned int.
     *
     * @return the unsigned value of the given short.
     */
    protected static int toUnsignedShort(short s) {
        return (s < 0) ? 0x10000 + s : s;
    }

    /**
     * Absolute put.
     *
     * @param index
     * @param value
     *
     * @return this
     *
     * @throws IndexOutOfBoundsException if the given index is out of range.
     */
    public ByteBufferVector putShort(int index, short value) {

        if (index + SIZEOF_SHORT > viewableLength) {
            throw new IndexOutOfBoundsException("Tried to put short at index " + index + " with value " + value + " into " + this);
        }

        int i = index;

        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            if (i < byteBuffer.viewableLength) {
                // The short at least *starts* in this buffer.

                if (i + (SIZEOF_SHORT - 1) < byteBuffer.viewableLength) {
                    // The short is contained in this byteBuffer.
                    byteBuffer.putShort(i, value);
                    return this;
                } else {
                    // The short spans two byteBuffers. Oy vey!
                    put(index, shortAsBytes(value));
                    return this;
                }
            }

            // Subtract this buffer's limit from the index and move on to the next buffer.
            i -= byteBuffer.viewableLength;
        }

        // Should never get here.
        throw new IndexOutOfBoundsException("Tried to put short at index " + index + " with value " + value + " into " + this);
    }

    /**
     * Absolute get.
     *
     * @param index
     *
     * @return the int at the given index.
     *
     * @throws IndexOutOfBoundsException if the given index is out of range.
     */
    public int getInt(int index) {

        // We removed the 'fail fast' code here as it was too expensive.  We'll fail later.
        int i = index;

        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            if (i < byteBuffer.viewableLength) {
                // The int at least *starts* in this buffer.

                if (i + (SIZEOF_INT - 1) < byteBuffer.viewableLength) {
                    // The int is contained in this byteBuffer.
                    return byteBuffer.getInt(i);
                } else {
                    // The int spans two byteBuffers. Oy vey!
                    return ((getub(index) << 24) + (getub(index + 1) << 16) + (getub(index + 2) << 8) + getub(index + 3));
                }
            }

            // Subtract this buffer's limit from the index and move on to the next buffer.
            i -= byteBuffer.viewableLength;
        }

        // Should never get here.
        throw new IndexOutOfBoundsException("Tried to get int at index " + index + " from " + this);
    }

    /**
     * Absolute put.
     *
     * @param index
     * @param value
     *
     * @return this
     *
     * @throws IndexOutOfBoundsException if the given index is out of range.
     */
    public ByteBufferVector putInt(int index, int value) {

        // We removed the 'fail fast' code here as it was too expensive.  We'll fail later.
        int i = index;

        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            if (i < byteBuffer.viewableLength) {
                // The int at least *starts* in this buffer.

                if (i + (SIZEOF_INT - 1) < byteBuffer.viewableLength) {
                    // The int is contained in this byteBuffer.
                    byteBuffer.putInt(i, value);
                    return this;
                } else {
                    // The int spans two byteBuffers. Oy vey!
                    put(index, intAsBytes(value));
                    return this;
                }
            }

            // Subtract this buffer's limit from the index and move on to the next buffer.
            i -= byteBuffer.viewableLength;
        }

        // Should never get here.
        throw new IndexOutOfBoundsException("Tried to put int at index " + index + " with value " + value + " into " + this);
    }

    /**
     * @return The given short converted to a byte[] (BIG ENDIAN)
     */
    protected static byte[] shortAsBytes(short value) {
        // Note: ByteBuffer by default encodes in BIG ENDIAN.
        return java.nio.ByteBuffer.allocate(SIZEOF_SHORT).putShort(value).array();
    }

    /**
     * @return The given int converted to a byte[] (BIG ENDIAN)
     */
    protected static byte[] intAsBytes(int value) {
        // Note: ByteBuffer by default encodes in BIG ENDIAN.
        return java.nio.ByteBuffer.allocate(SIZEOF_INT).putInt(value).array();
    }

    /**
     * @return The given long converted to a byte[] (BIG ENDIAN)
     */
    protected static byte[] longAsBytes(long value) {
        // Note: ByteBuffer by default encodes in BIG ENDIAN.
        return java.nio.ByteBuffer.allocate(SIZEOF_LONG).putLong(value).array();
    }

    /**
     * Absolute put.
     *
     * @param index
     * @param value
     *
     * @return this
     *
     * @throws IndexOutOfBoundsException if the given index is out of range.
     */
    public ByteBufferVector putLong(int index, long value) {

        if (index + SIZEOF_LONG > viewableLength) {
            throw new IndexOutOfBoundsException("Tried to put long at index " + index + " with value " + value + " into " + this);
        }

        int i = index;

        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            if (i < byteBuffer.viewableLength) {
                // The long at least *starts* in this buffer.

                if (i + (SIZEOF_LONG - 1) < byteBuffer.viewableLength) {
                    // The long is contained in this byteBuffer.
                    byteBuffer.putLong(i, value);
                    return this;
                } else {
                    // The long spans more than 1 byteBuffer. Oy vey!
                    put(index, longAsBytes(value));
                    return this;
                }
            }

            // Subtract this buffer's limit from the index and move on to the next buffer.
            i -= byteBuffer.viewableLength;
        }

        // Should never get here.
        throw new IndexOutOfBoundsException("Tried to put long at index " + index + " with value " + value + " into " + this);
    }

    /**
     * Absolute get.
     *
     * @param index
     *
     * @return The long value at the given index.
     *
     * @throws IndexOutOfBoundsException if the given index is out of range.
     */
    public long getLong(int index) {
        if (index + SIZEOF_LONG > viewableLength) {
            throw new IndexOutOfBoundsException("Tried to get long at index " + index + " from " + this);
        }

        int i = index;

        // Don't use the for(object:collection) notation, it creates an iterator, too expensive.  Most
        // of the data we're looking for is in the 1st or 2nd buffer.
        for (int x = 0; x < byteBuffers.size(); x++) {
            ByteBuffer byteBuffer = byteBuffers.get(x);
            if (i < byteBuffer.viewableLength) {
                // The long at least *starts* in this buffer.

                if (i + (SIZEOF_LONG - 1) < byteBuffer.viewableLength) {
                    // The long is contained in this byteBuffer.
                    return byteBuffer.getLong(i);
                } else {
                    // The long spans two (or more) byteBuffers. Oy vey!
                    return (((long) getub(index) << 56) + ((long) getub(index + 1) << 48) + ((long) getub(index + 2) << 40) + ((long) getub(index + 3) << 32) +
                            ((long) getub(index + 4) << 24) + ((long) getub(index + 5) << 16) + ((long) getub(index + 6) << 8) + getub(index + 7));
                }
            }

            // Subtract this buffer's limit from the index and move on to the next buffer.
            i -= byteBuffer.viewableLength;
        }

        // Should never get here.
        throw new IndexOutOfBoundsException("Tried to get long at index " + index + " from " + this);
    }

    /**
     * Read a string from the buffer.
     *
     * @param index   - the offset to read from
     * @param length  - the length of the String to read
     * @param charset - the name of the charset that the bytes are encoded in
     *
     * @return The String
     *
     * @throws BufferUnderflowException if the String is not completely contained by the buffer.
     * @throws IllegalArgumentException if the charset is unsupported.
     */
    public String getString(int index, int length, String charset) {

        byte[] bytes = new byte[length];
        get(index, bytes);

        try {
            return truncateAtNull(new String(bytes, charset));

        } catch (UnsupportedEncodingException uee) {
            throw new IllegalArgumentException(uee);
        }
    }

    /**
     * @return the given String truncated at the first null byte in the String.
     *         E.g. "abc\0def" would return "abc".
     */
    private String truncateAtNull(String s) {
        int firstNull = s.indexOf(0);
        return (firstNull < 0) ? s : s.substring(0, firstNull);
    }

    /**
     * Put a string into the buffer.
     *
     * @param index   - the offset at which to insert the string
     * @param str     - the string to insert
     * @param charset - the name of the charset for encoding the string bytes
     *
     * @return this
     *
     * @throws BufferUnderflowException if the String is not completely contained by the buffer.
     * @throws IllegalArgumentException if the charset is unsupported.
     */
    public ByteBufferVector putString(int index, String str, String charset) {

        str = (str == null) ? "" : str; // Convert null to "" to avoid NPE.

        try {
            put(index, str.getBytes(charset));
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalArgumentException(uee);
        }

        return this;
    }

    /**
     * Put a String into the buffer at the given index, for the maximum given
     * fieldLen, encoded in the given charset.
     *
     * If the string is longer than the fieldLen it will be truncated.
     *
     * @param index    - The offset into the buffer
     * @param fieldLen - The max length of the string
     * @param str      - The String to put
     * @param charset  - The encoding to use
     *
     * @return this
     */
    public ByteBufferVector putStringField(int index, int fieldLen, String str, String charset) {
        str = (str != null) ? str : "";
        str = (str.length() <= fieldLen) ? str : str.substring(0, fieldLen); // truncate if necessary.
        return putString(index, str, charset);
    }

    /**
     * Put a String into the buffer at the given index, blank-padded to the given
     * length, encoded in the give charset
     *
     * @param index    - The offset into the buffer
     * @param fieldLen - Blank pad/truncate the given string to this length
     * @param str      - The String to put
     * @param charset  - The encoding to use
     *
     * @return this
     */
    public ByteBufferVector putStringFieldPadded(int index, int fieldLen, String str, String charset) {
        return put(index, CodepageUtils.getBytesPadded(str, fieldLen, charset));
    }

    /**
     * Gets a byte array copy of this ByteBufferVector. This is expensive and should only
     * be used for debugging.
     */
    public byte[] toByteArray() {
        byte[] newBytes = new byte[this.viewableLength];
        get(0, newBytes);
        return newBytes;
    }

    /**
     * Split the ByteBufferVector into two pieces.
     *
     * @param firstLength the new length of the first ByteBufferVector.
     *
     * @return A list containing the two ByteBufferVector objects.
     */
    public List<ByteBufferVector> split(int firstLength) {
        List<ByteBufferVector> splitList = new ArrayList<ByteBufferVector>(2);
        splitList.add(new ByteBufferVector(this, 0, firstLength));
        splitList.add(new ByteBufferVector(this, firstLength, this.viewableLength - firstLength));
        return splitList;
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        StringBuilder retMe = new StringBuilder();
        retMe.append("[ByteBufferVector[len=" + viewableLength + "]-");
        int i = 0;
        for (ByteBuffer byteBuffer : byteBuffers) {
            retMe.append("[" + i++ + "]" + byteBuffer.toString());
        }
        retMe.append("]");
        return retMe.toString();
    }

    /**
     * Inner class taking a byte array and abstracting it out in a ByteBuffer-like way.
     */
    @Trivial
    private final static class ByteBuffer {
        /** The index of the first useable byte in the byte array. */
        private int firstByteIndex = 0;

        /** The number of bytes that we can use in the byte array. */
        private int viewableLength = 0;

        /** The byte array we are mapping onto. */
        private byte[] data = null;

        /** Constructor */
        ByteBuffer(byte[] data, int firstByteIndex, int viewableLength) {
            this.data = data;
            this.firstByteIndex = firstByteIndex;
            this.viewableLength = viewableLength;
        }

        /** Absolute get (byte). Caller must verify index is valid. */
        byte get(int index) {
            return data[firstByteIndex + index];
        }

        /** Absolute put (byte). Caller must verify index is valid. */
        void put(int index, byte value) {
            data[firstByteIndex + index] = value;
        }

        /** Absolute get (byte[]). Caller must verify index/length is valid. */
        void get(int index, byte[] dst, int offset, int length) {
            System.arraycopy(data, firstByteIndex + index, dst, offset, length);
        }

        /** Absolute put (byte[]). Caller must verify index/length is valid. */
        void put(int index, byte[] value, int offset, int length) {
            System.arraycopy(value, offset, data, firstByteIndex + index, length);
        }

        /** Absolute get (short). Caller must verify index/length is valid. */
        short getShort(int i) {
            int index = i + firstByteIndex;
            return (short) (((short) ((data[index + 1] & 0xFF) << 0)) + ((short) ((data[index] & 0xFF) << 8)));
        }

        /** Absolute put (short). Caller must verify index/length is valid. */
        void putShort(int i, short value) {
            int index = i + firstByteIndex;
            data[index + 1] = (byte) (value >>> 0);
            data[index + 0] = (byte) (value >>> 8);
        }

        /** Absolute get (int). Caller must verify index/length is valid. */
        int getInt(int i) {
            int index = i + firstByteIndex;
            return ((data[index + 3] & 0xFF) << 0) +
                   ((data[index + 2] & 0xFF) << 8) +
                   ((data[index + 1] & 0xFF) << 16) +
                   ((data[index + 0]) << 24);
        }

        /** Absoulte put (int). Caller must verify index/length is valid. */
        void putInt(int i, int value) {
            int index = i + firstByteIndex;
            data[index + 3] = (byte) (value >>> 0);
            data[index + 2] = (byte) (value >>> 8);
            data[index + 1] = (byte) (value >>> 16);
            data[index + 0] = (byte) (value >>> 24);
        }

        /** Absolute get (long). Caller must verify index/length is valid. */
        long getLong(int i) {
            int index = i + firstByteIndex;
            return (((long) (data[index + 7] & 0xFF)) << 0) +
                   (((long) (data[index + 6] & 0xFF)) << 8) +
                   (((long) (data[index + 5] & 0xFF)) << 16) +
                   (((long) (data[index + 4] & 0xFF)) << 24) +
                   (((long) (data[index + 3] & 0xFF)) << 32) +
                   (((long) (data[index + 2] & 0xFF)) << 40) +
                   (((long) (data[index + 1] & 0xFF)) << 48) +
                   (((long) (data[index + 0])) << 56);
        }

        /** Absolute put (long). Caller must verify index/length is valid. */
        void putLong(int i, long value) {
            int index = i + firstByteIndex;
            data[index + 7] = (byte) (value >>> 0);
            data[index + 6] = (byte) (value >>> 8);
            data[index + 5] = (byte) (value >>> 16);
            data[index + 4] = (byte) (value >>> 24);
            data[index + 3] = (byte) (value >>> 32);
            data[index + 2] = (byte) (value >>> 40);
            data[index + 1] = (byte) (value >>> 48);
            data[index + 0] = (byte) (value >>> 56);
        }
    };
}
