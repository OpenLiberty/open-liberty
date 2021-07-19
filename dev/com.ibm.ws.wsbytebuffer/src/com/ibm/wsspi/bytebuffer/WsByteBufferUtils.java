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

/**
 * This interface contains utility methods for manipulating WsByteBuffer objects
 * and arrays of WsByteBuffer objects.
 */
public abstract class WsByteBufferUtils {

    /**
     * Convert an array of buffers to a byte array. The buffers will remain
     * unchanged
     * by this conversion. This will stop on the first null buffer. A null or
     * empty
     * list will return a null byte[].
     * 
     * @param list
     * @return byte[]
     */
    public static final byte[] asByteArray(WsByteBuffer[] list) {
        if (null == list)
            return null;
        int size = 0;
        for (int i = 0; i < list.length && null != list[i]; i++) {
            size += list[i].limit();
        }
        if (0 == size)
            return null;
        byte[] output = new byte[size];
        int offset = 0;
        int position = 0;
        for (int i = 0; i < list.length && null != list[i]; i++) {
            position = list[i].position();
            list[i].position(0);
            list[i].get(output, offset, list[i].limit());
            offset += list[i].limit();
            list[i].position(position);
        }
        return output;
    }

    /**
     * Converts a list of buffers to a byte[] using the input starting positions
     * and
     * ending limits. Buffers will remaining unchanged after this process. A null
     * or
     * empty list will return a null byte[]. This will also stop on the first null
     * buffer.
     * 
     * @param list
     * @param positions
     * @param limits
     * @return byte[]
     */
    public static final byte[] asByteArray(WsByteBuffer[] list, int[] positions, int[] limits) {
        if (null == list)
            return null;
        int size = 0;
        for (int i = 0; i < list.length && null != list[i]; i++) {
            size += limits[i] - positions[i];
        }
        if (0 == size)
            return null;
        byte[] output = new byte[size];
        int offset = 0;
        int position = 0;
        for (int i = 0; i < list.length && null != list[i]; i++) {
            position = list[i].position();
            list[i].position(positions[i]);
            list[i].get(output, offset, (limits[i] - positions[i]));
            offset += (limits[i] - positions[i]);
            list[i].position(position);
        }
        return output;
    }

    /**
     * Convert an array of buffers to a String.
     * 
     * @param list
     * @return String
     */
    public static final String asString(WsByteBuffer[] list) {
        byte[] data = asByteArray(list);
        return (null != data) ? new String(data) : null;
    }

    /**
     * Convert an array of buffers to a string using the input starting positions
     * and ending limits.
     * 
     * @param list
     * @param positions
     * @param limits
     * @return String
     */
    public static final String asString(WsByteBuffer[] list, int[] positions, int[] limits) {
        byte[] data = asByteArray(list, positions, limits);
        return (null != data) ? new String(data) : null;
    }

    /**
     * Convert a buffer to a String.
     * 
     * @param buff
     * @return String
     */
    public static final String asString(WsByteBuffer buff) {
        byte[] data = asByteArray(buff);
        return (null != data) ? new String(data) : null;
    }

    /**
     * Convert a buffer to a string using the input starting position and ending
     * limit.
     * 
     * @param buff
     * @param position
     * @param limit
     * @return String
     */
    public static final String asString(WsByteBuffer buff, int position, int limit) {
        byte[] data = asByteArray(buff, position, limit);
        return (null != data) ? new String(data) : null;
    }

    /**
     * Convert a buffer into a byte array. A null or empty buffer will return a
     * null
     * byte[].
     * 
     * @param buff
     * @return byte[]
     */
    public static final byte[] asByteArray(WsByteBuffer buff) {
        if (null == buff)
            return null;
        int size = buff.limit();
        if (0 == size) {
            return null;
        }
        byte[] output = new byte[size];
        int position = buff.position();
        buff.position(0);
        buff.get(output);
        buff.position(position);
        return output;
    }

    /**
     * Convert a buffer into a byte array using the input starting position and
     * ending limit. If the buffer is null or empty then a null byte[] is
     * returned.
     * 
     * @param buff
     * @param position
     * @param limit
     * @return byte[]
     */
    public static final byte[] asByteArray(WsByteBuffer buff, int position, int limit) {
        if (null == buff)
            return null;
        int size = limit - position;
        if (0 == size)
            return null;
        byte[] byteBuff = new byte[size];
        int currentPosition = buff.position();
        buff.position(position);
        buff.get(byteBuff, 0, size);
        buff.position(currentPosition);
        return byteBuff;
    }

    /**
     * Convert a list of buffers to a StringBuffer. Null or empty data will result
     * in
     * an empty StringBuffer.
     * 
     * @param list
     * @return StringBuffer
     */
    public static final StringBuffer asStringBuffer(WsByteBuffer[] list) {
        StringBuffer sb = new StringBuffer();
        String data = asString(list);
        if (null != data) {
            sb.append(data);
        }
        return sb;
    }

    /**
     * Convert a buffer to a StringBuffer. Null or empty data will result in
     * an empty StringBuffer.
     * 
     * @param buff
     * @return StringBuffer
     */
    public static final StringBuffer asStringBuffer(WsByteBuffer buff) {
        StringBuffer sb = new StringBuffer();
        String data = asString(buff);
        if (null != data) {
            sb.append(data);
        }
        return sb;
    }

    /**
     * Convert a buffer to an int.
     * 
     * @param buff
     * @return int
     */
    public static final int asInt(WsByteBuffer buff) {
        return asInt(asByteArray(buff));
    }

    /**
     * Convert a buffer to an int using the starting position and ending limit.
     * 
     * @param buff
     * @param position
     * @param limit
     * @return int
     */
    public static final int asInt(WsByteBuffer buff, int position, int limit) {
        return asInt(asByteArray(buff, position, limit));
    }

    /**
     * Convert a list of buffers to an int.
     * 
     * @param list
     * @return int
     */
    public static final int asInt(WsByteBuffer[] list) {
        return asInt(asByteArray(list));
    }

    /**
     * Convert a list of buffers to an int using the starting positions and ending
     * limits.
     * 
     * @param list
     * @param positions
     * @param limits
     * @return int
     */
    public static final int asInt(WsByteBuffer[] list, int[] positions, int[] limits) {
        return asInt(asByteArray(list, positions, limits));
    }

    /**
     * Convert a byte array to an int, trimming any whitespace. Null input
     * will return a -1. Invalid numbers will throw a NumberFormatException.
     * 
     * @param data
     * @return int
     */
    public static final int asInt(byte[] data) {
        if (null == data) {
            return -1;
        }

        int start = 0;
        // skip leading whitespace
        for (; start < data.length; start++) {
            if (' ' != data[start] || '\t' == data[start]) {
                break;
            }
        }
        // skip trailing whitespace
        int i = data.length - 1;
        for (; start <= i; i--) {
            if (' ' != data[i] && '\t' != data[i]) {
                break;
            }
        }
        if (i <= start) {
            // empty content
            return -1;
        }
        int intVal = 0;
        int mark = 1;
        int digit;
        for (; start <= i; i--) {
            digit = data[i] - '0';
            if (0 > digit || 9 < digit) {
                // stop on any nondigit, if it's not a dash then throw an exc
                if ('-' != data[i]) {
                    throw new NumberFormatException("Invalid digit: " + data[i]);
                }
                break;
            }
            intVal += digit * mark;
            mark *= 10;
        }
        if (start < i) {
            // didn't use the whole content...
            throw new NumberFormatException("Invalid data");
        }

        // check for negative numbers
        if (start == i && data[i] == '-') {
            intVal = -intVal;
        }

        return intVal;
    }

    /**
     * Helper function to release the buffers stored in a list. It will null
     * out the buffer references in the list as it releases them.
     * 
     * @param list
     */
    public static void releaseBufferArray(WsByteBuffer[] list) {
        if (null == list) {
            return;
        }
        for (int i = 0; i < list.length; i++) {
            if (null != list[i]) {
                list[i].release();
                list[i] = null;
            }
        }
    }

    /**
     * Calculate the total length of actual content in the buffers in the input
     * list. This will stop on the first null buffer or the end of the list.
     * 
     * @param list
     * @return int
     */
    public static final int lengthOf(WsByteBuffer[] list) {
        if (null == list) {
            return 0;
        }
        int length = 0;
        for (int i = 0; i < list.length && null != list[i]; i++) {
            length += list[i].remaining();
        }
        return length;
    }

    /**
     * Flip all non null buffers in the array.
     * 
     * @param list
     */
    public static void flip(WsByteBuffer[] list) {
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i] != null) {
                    list[i].flip();
                }
            }
        }
    }

    /**
     * Find out the total capacity in an array of buffers. A null or empty
     * list will return 0.
     * 
     * @param list
     * @return int
     */
    public static final int getTotalCapacity(WsByteBuffer[] list) {
        if (null == list) {
            return 0;
        }
        int cap = 0;
        for (int i = 0; i < list.length; i++) {
            if (list[i] != null) {
                cap += list[i].capacity();
            }
        }
        return cap;
    }

    /**
     * Clear each buffer in an array of them.
     * 
     * @param list
     */
    public static void clearBufferArray(WsByteBuffer[] list) {
        if (list != null) {
            for (int i = 0; i < list.length; ++i) {
                if (list[i] != null)
                    list[i].clear();
            }
        }
    }

    /**
     * Store a byte[] into a wsbb[]. This will fill out each wsbb until
     * all of the bytes are written. It will throw an exception if there
     * is not enough space. Users must pass in a boolean to determine
     * whether or not to flip() the last buffer -- i.e. pass in true if
     * this is the only put you're doing, otherwise pass in false. Any
     * intermediate buffers that are filled to capacity will be flip()'d
     * automatically. A null input value will result in a no-op.
     * 
     * @param list
     * @param value
     * @param bFlipLast
     */
    public static void putByteArrayValue(WsByteBuffer[] list, byte[] value, boolean bFlipLast) {
        if (null == list || null == value) {
            return;
        }
        if (value.length > getTotalCapacity(list)) {
            throw new IllegalArgumentException("Buffers not large enough");
        }
        int remaining = value.length;
        int offset = 0; // current offset into the value
        int avail = 0;
        for (int i = 0; i < list.length; i++) {
            avail = list[i].limit() - list[i].position();
            if (remaining <= avail) {
                list[i].put(value, offset, remaining);
                if (bFlipLast) {
                    list[i].flip();
                }
                break;
            }
            list[i].put(value, offset, avail);
            list[i].flip();
            offset += avail;
            remaining -= avail;
        }
    }

    /**
     * Store a String into a wsbb[]. This will fill out each wsbb until
     * all of the bytes are written. It will throw an exception if there
     * is not enough space. Users must pass in a boolean to determine
     * whether or not to flip() the last buffer -- i.e. pass in true if
     * this is the only put you're doing, otherwise pass in false. Any
     * intermediate buffers that are filled to capacity will be flip()'d
     * automatically. A null input string will result in a no-op.
     * 
     * @param buff
     * @param value
     * @param bFlipLast
     */
    public static void putStringValue(WsByteBuffer[] buff, String value, boolean bFlipLast) {
        if (null != value) {
            putByteArrayValue(buff, value.getBytes(), bFlipLast);
        }
    }

    /**
     * Expand an existing wsbb[] to include a new wsbb[]
     * 
     * @param oldList
     * @param newBuffers
     * @return WsByteBuffer[]
     */
    public static WsByteBuffer[] expandBufferArray(WsByteBuffer[] oldList, WsByteBuffer[] newBuffers) {
        if (null == oldList && null == newBuffers) {
            // if both are null then just exit
            return null;
        }

        int oldLen = (null != oldList ? oldList.length : 0);
        int newLen = (null != newBuffers ? newBuffers.length : 0);
        WsByteBuffer[] bb = new WsByteBuffer[oldLen + newLen];
        if (0 < oldLen) {
            System.arraycopy(oldList, 0, bb, 0, oldLen);
        }
        if (0 < newLen) {
            System.arraycopy(newBuffers, 0, bb, oldLen, newLen);
        }
        return bb;
    }

    /**
     * Expand an existing list of buffers to include one new buffer, if that
     * buffer
     * is null then the list is returned as-is.
     * 
     * @param list
     * @param buffer
     * @return WsByteBuffer[]
     */
    public static WsByteBuffer[] expandBufferArray(WsByteBuffer[] list, WsByteBuffer buffer) {
        if (null == buffer)
            return list;
        int len = (null != list ? list.length : 0);
        WsByteBuffer[] bb = new WsByteBuffer[len + 1];
        if (0 < len) {
            System.arraycopy(list, 0, bb, 0, len);
        }
        bb[len] = buffer;
        return bb;
    }
}
