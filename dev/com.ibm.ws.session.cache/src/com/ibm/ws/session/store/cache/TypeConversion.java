/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session.store.cache;

import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A utility class for converting primitives and potentially Object types.
 * 
 */
@Trivial // reveals customer data
public final class TypeConversion {
    /**
     * A utility method to convert the int from the byte array to an int.
     * 
     * @param bytes
     *            The byte array containing the int.
     * @param offset
     *            The index at which the int is located.
     * @return The int value.
     */
    public static int bytesToInt(byte[] bytes, int offset) {
        return ((bytes[offset + 3] & 0xFF) << 0) + ((bytes[offset + 2] & 0xFF) << 8)
                        + ((bytes[offset + 1] & 0xFF) << 16) + ((bytes[offset + 0] & 0xFF) << 24);
    }

    /**
     * A utility method to convert the short from the byte array to a short.
     * 
     * @param bytes
     *            The byte array containing the short.
     * @param offset
     *            The index at which the short is located.
     * @return The short value.
     */
    public static short bytesToShort(byte[] bytes, int offset) {
        short result = 0x0;
        for (int i = offset; i < offset + 2; ++i) {
            result = (short) ((result) << 8);
            result |= (bytes[i] & 0x00FF);
        }
        return result;
    }

    /**
     * A utility method to convert the long from the byte array to a long.
     * 
     * @param bytes
     *            The byte array containing the long.
     * @param offset
     *            The index at which the long is located.
     * @return The long value.
     */
    public static long bytesToLong(byte[] bytes, int offset) {
        long result = 0x0;
        for (int i = offset; i < offset + 8; ++i) {
            result = result << 8;
            result |= (bytes[i] & 0x00000000000000FFl);
        }
        return result;
    }

    /**
     * A utility method to convert the char from the byte array to a char.
     * 
     * @param bytes
     *            The byte array containing the char.
     * @param offset
     *            The index at which the char is located.
     * @return The char value.
     */
    public static char bytesToChar(byte[] bytes, int offset) {
        char result = 0x0;
        for (int i = offset; i < offset + 2; ++i) {
            result = (char) ((result) << 8);
            result |= (bytes[i] & 0x00FF);
        }
        return result;
    }

    public static void charToBytes(char value, byte[] bytes, int offset) {
        for (int i = offset + 1; i >= offset; --i) {
            bytes[i] = (byte) value;
            value = (char) ((value) >> 8);
        }
    }

    /**
     * A utility method to convert an int into bytes in an array.
     * 
     * @param value
     *            An int.
     * @param bytes
     *            The byte array to which the int should be copied.
     * @param offset
     *            The index where the int should start.
     */
    public static void intToBytes(int value, byte[] bytes, int offset) {
        bytes[offset + 3] = (byte) (value >>> 0);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 0] = (byte) (value >>> 24);
    }

    /**
     * A utility method to convert a short into bytes in an array.
     * 
     * @param value
     *            A short.
     * @param bytes
     *            The byte array to which the short should be copied.
     * @param offset
     *            The index where the short should start.
     */
    public static void shortToBytes(short value, byte[] bytes, int offset) {
        for (int i = offset + 1; i >= offset; --i) {
            bytes[i] = (byte) value;
            value = (short) ((value) >> 8);
        }
    }

    /**
     * A utility method to convert the long into bytes in an array.
     * 
     * @param value
     *            The long.
     * @param bytes
     *            The byte array to which the long should be copied.
     * @param offset
     *            The index where the long should start.
     */
    public static void longToBytes(long value, byte[] bytes, int offset) {
        for (int i = offset + 7; i >= offset; --i) {
            bytes[i] = (byte) value;
            value = value >> 8;
        }
    }

    /**
     * Reads a long from the byte array in the Varint format.
     * @param bytes - byte buffer to read
     * @param offset - the offset within the bytes buffer to start reading
     * @return - returns the long value
     */
    public static long varIntBytesToLong(byte[] bytes, int offset) {
        int shift = 0;
        long result = 0;
        while (shift < 64) {
            final byte b = bytes[offset++];
            result |= (long)(b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new IllegalStateException("Varint representation is invalid or exceeds 64-bit value");
    }   

    /**
     * Reads an int from the byte array in the Varint format.
     * @param bytes - byte buffer to read
     * @param offset - the offset within the bytes buffer to start reading
     * @return - returns the int value
     */    
    public static int varIntBytesToInt(byte[] bytes, int offset) {

        byte tmp = bytes[offset++];
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = bytes[offset++]) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = bytes[offset++]) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = bytes[offset++]) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = bytes[offset++]) << 28;
                    if (tmp < 0) {
                        // Discard upper 32 bits.
                        for (int i = 0; i < 5; i++) {
                            if (bytes[offset++] >= 0) {
                                return result;
                            }
                        }
                        //Should never happen since we wrote the varint value. If this occurs due to an internal bug
                        //this exception is caught and wrapped further up the chain.
                        throw new IllegalStateException("Varint representation is invalid or exceeds 32-bit value");
                    }
                }
            }
        }
        return result;
    }

    /**
     * Writes a long to the byte array in the Varint format.
     * @param v - long value to write to the bytes buffer in the Varint format 
     * @param bytes - byte buffer to write to - must contain enough space for maximum 
     *                length which is 10 bytes.
     * @param offset - the offset within the bytes buffer to start writing
     * @return - returns the number of bytes used from the bytes buffer
     */
    public static int writeLongAsVarIntBytes(long v, byte[] bytes, int offest) {
        int pos = offest;

        while (true) {
            if ((v & ~0x7FL) == 0) {
                bytes[pos++] = ((byte)v);
                return pos;
            } else {
                bytes[pos++] = (byte)((v & 0x7F) | 0x80);
                v >>>= 7;
            }
        }    
    }


    /**
     * Writes an integer to the byte array in the Varint format.
     * @param intVal - integer value to write to the bytes buffer in the Varint format 
     * @param bytes - byte buffer to write to - must contain enough space for maximum 
     *                length which is 5 bytes.
     * @param offset - the offset within the bytes buffer to start writing
     * @return - returns the number of bytes used from the bytes buffer
     */
    public static int writeIntAsVarIntBytes(int intVal, byte[] bytes, int offset) {
        int pos = offset;
        int v = intVal;

        if ((v & ~0x7F) == 0) {
            bytes[pos++] = ((byte) v);
            return 1 + offset;
        }

        while (true) {
            if ((v & ~0x7F) == 0) {
                bytes[pos++] = ((byte) v);
                return pos;
            } else {
                bytes[pos++] = (byte) ((v & 0x7F) | 0x80);
                v >>>= 7;
            }
        }
    }
    
    /**
     * If the byte array length is less than 1000, returns the result of calling Arrays.toString on the
     * supplied byte array.  Otherwise returns the result of calling Arrays.toString on a byte array containing
     * the first 1000 bytes in the supplied byte array.
     */
    public static String limitedBytesToString(byte[] bytes) {
        if(bytes.length <= 1000) {
            return Arrays.toString(bytes);
        } else {
            byte[] firstBytes = new byte[1000];
            System.arraycopy(bytes, 0, firstBytes, 0, 1000);
            return Arrays.toString(firstBytes);
        }
    }
}
