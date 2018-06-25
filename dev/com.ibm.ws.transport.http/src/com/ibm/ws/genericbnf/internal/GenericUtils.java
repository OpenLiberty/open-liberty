/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.genericbnf.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.PasswordNullifier;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.genericbnf.BNFHeaders;

/**
 * Class for various utility methods.
 */
public class GenericUtils {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(GenericUtils.class,
                                                         GenericConstants.GENERIC_TRACE_NAME,
                                                         null);

    /** Representation of a zero character */
    private static final byte ZERO = '0';
    /** Representation of a dash character */
    private static final byte DASH = '-';
    /** byte[] representation of a zero int */
    private static final byte[] ZERO_BYTEARRAY = { ZERO };
    /** How many digits are possible with a maximum int value */
    private static final int SIZE_MAXLONG = Long.toString(Long.MAX_VALUE).length();
    /** HEX character list */
    private static final byte[] HEX_BYTES = {
                                             (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4',
                                             (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9',
                                             (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e',
                                             (byte) 'f'
    };

    /**
     * Private constructor as all util methods are static.
     * 
     */
    private GenericUtils() {
        // nothing to do
    }

    /**
     * Given a wsbb[], we're adding a byte value to the <b>last</b> buffer. If
     * that buffer fills up, then we will allocate a new one, by expanding
     * the last WsByteBuffer in wsbb[].
     * <p>
     * Returns the new wsbb[] (expanded if needed).
     * 
     * @param buffers
     * @param value
     * @param bnfObj
     * @return WsByteBuffer[]
     */
    static public WsByteBuffer[] putByte(WsByteBuffer[] buffers, byte value, BNFHeadersImpl bnfObj) {

        // verify input buffer information
        if (null == buffers) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Null buffers sent to putByte");
            }
            return null;
        }

        // get the last buffer
        WsByteBuffer buffer = buffers[buffers.length - 1];
        try {
            buffer.put(value);
        } catch (BufferOverflowException boe) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "putByte overflow: " + buffer);
            }

            // allocate another buffer and put the byte into it
            buffer.flip();
            buffer = bnfObj.allocateBuffer(bnfObj.getOutgoingBufferSize());
            buffer.put(value);
            return WsByteBufferUtils.expandBufferArray(buffers, buffer);
        }
        return buffers;
    }

    /**
     * Given a wsbb[], we're adding a int value to the <b>last</b> buffer. If
     * that buffer fills up, then we will allocate a new one, by expanding
     * the last WsByteBuffer in wsbb[].
     * <p>
     * Returns the new wsbb[] (expanded if needed).
     * 
     * @param buffers
     * @param value
     * @param bnfObj
     * @return WsByteBuffer[]
     */
    static public WsByteBuffer[] putInt(WsByteBuffer[] buffers, int value, BNFHeadersImpl bnfObj) {
        // verify input buffer information
        if (null == buffers) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Null buffers sent to putInt");
            }
            return null;
        }

        // Last buffer
        WsByteBuffer buffer = buffers[buffers.length - 1];
        byte[] data = asBytes(value);

        try {
            buffer.put(data);
        } catch (BufferOverflowException boe) {
            // no FFDC required
            // use existing method to put what bytes we can, allocate a new
            // buffer and put the rest
            return putByteArrayKnownOverflow(buffers, data, bnfObj);
        }

        return buffers;
    }

    /**
     * Given a wsbb[], we're adding a byte[] value to the last buffer. If
     * that buffer fills up, then we will allocate a new one, expand the
     * wsbb[] and keep going until the entire byte[] is added.
     * <p>
     * Returns the new wsbb[] (expanded if needed).
     * 
     * @param buffers
     * @param value
     * @param bnfObj
     * @return WsByteBuffer[]
     */
    static public WsByteBuffer[] putByteArray(WsByteBuffer[] buffers, byte[] value, BNFHeadersImpl bnfObj) {
        // LIDB2356-41: byte[]/offset/length support
        return putByteArray(buffers, value, 0, value.length, bnfObj);
    }

    /**
     * Given a wsbb[], we're adding a byte[] value to the last buffer. If
     * that buffer fills up, then we will allocate a new one, expand the
     * wsbb[] and keep going until the entire byte[] is added.
     * <p>
     * Returns the new wsbb[] (expanded if needed).
     * 
     * @param buffers
     * @param value
     * @param inOffset
     * @param length
     * @param bnfObj
     * @return WsByteBuffer[]
     */
    static public WsByteBuffer[] putByteArray(WsByteBuffer[] buffers, byte[] value, int inOffset, int length, BNFHeadersImpl bnfObj) {
        // LIDB2356-41: byte[]/offset/length support
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            // verify the input buffer information
            if (null == buffers) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Null buffers sent to putByteArray");
                }
                return null;
            }
            // verify the input value information
            if (null == value || 0 == value.length) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Empty value provided to putByteArray: " + value);
                }
                // return no changes
                return buffers;
            }
        }

        int offset = inOffset;
        WsByteBuffer buffer = buffers[buffers.length - 1];
        try {
            buffer.put(value, offset, length);
        } catch (BufferOverflowException boe) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "putByteArray overflow: " + buffer);
            }

            // put what we can and get more space
            // available data in current buffer
            int avail = buffer.capacity() - buffer.position();
            buffer.put(value, offset, avail);
            buffer.flip();
            offset += avail;
            int remaining = length - offset;
            int numBuffers = (remaining / bnfObj.getOutgoingBufferSize());
            // see if there are "leftover" bytes that need another buffer
            if (0 != (remaining % bnfObj.getOutgoingBufferSize())) {
                numBuffers++;
            }
            // allocate new buffers and put data in as we go along
            WsByteBuffer[] newBuffers = new WsByteBuffer[numBuffers];
            for (int i = 0; i < numBuffers; i++) {
                newBuffers[i] = bnfObj.allocateBuffer(bnfObj.getOutgoingBufferSize());
                avail = newBuffers[i].capacity();
                // if the available space is enough for the rest of the data,
                // add it and we're done
                if (remaining <= avail) {
                    newBuffers[i].put(value, offset, remaining);
                    break;
                }
                // if it's not, then we need to put what we can and then
                // expand the buffer[]
                newBuffers[i].put(value, offset, avail);
                newBuffers[i].flip();

                offset += avail;
                remaining -= avail;
            }

            return WsByteBufferUtils.expandBufferArray(buffers, newBuffers);
        }

        return buffers;
    }

    /**
     * Given a wsbb[], we're adding a byte[] value to the last buffer. If
     * that buffer fills up, then we will allocate a new one, expand the
     * wsbb[] and keep going until the entire byte[] is added.
     * <p>
     * Returns the new wsbb[] (expanded if needed)
     * 
     * @param inBuffers
     * @param value
     * @param bnfObj
     * @return WsByteBuffer[]
     */
    static private WsByteBuffer[] putByteArrayKnownOverflow(WsByteBuffer[] inBuffers, byte[] value, BNFHeadersImpl bnfObj) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Known buffer overflow in put.");
        }

        WsByteBuffer[] buffers = inBuffers;
        boolean bDone = false;
        int remaining = value.length;
        int offset = 0; // current offset into the value
        WsByteBuffer buffer = buffers[buffers.length - 1];
        int avail = buffer.capacity() - buffer.position();

        while (!bDone) {
            // if the available space is enough for the rest of the data,
            // add it and we're done
            if (remaining <= avail) {
                buffer.put(value, offset, remaining);
                bDone = true;
            }
            // if it's not, then we need to put what we can and then
            // expand the buffer[]

            else {
                buffer.put(value, offset, avail);
                buffer.flip();

                offset += avail;
                remaining -= avail;

                // allocate a new buffer and expand the array
                buffer = bnfObj.allocateBuffer(bnfObj.getOutgoingBufferSize());
                buffers = WsByteBufferUtils.expandBufferArray(buffers, buffer);
                avail = buffer.capacity();
            }
        }
        return buffers;
    }

    /**
     * This method reads data from the WsByteBuffer and writes it
     * to the byte array. The number of the bytes read is the length
     * of the passed byte array. if the data in the WsByteBuffer is not
     * sufficient then it reads the max. no. of bytes possible.
     * It returns the <b>total</b> number of bytes read into the byte
     * array.
     * 
     * @param wsbb Read WsBytebuffer
     * @param dst Write byte array
     * @param offset start of write position in the byte array
     * @return (offset + no. of bytes read)
     */
    static public int getBytes(WsByteBuffer wsbb, byte[] dst, int offset) {
        try {
            // Use the buffers bulk get method
            wsbb.get(dst, offset, dst.length - offset);
            return dst.length;
        } catch (BufferUnderflowException bue) {
            // no FFDC required
            int numOfBytesAvail = wsbb.remaining();
            wsbb.get(dst, offset, numOfBytesAvail);
            return (offset + numOfBytesAvail);
        }
    }

    /**
     * Takes an array of 4 bytes and returns an integer that is
     * represented by them.
     * 
     * @param array
     * @return int that represents the 4 bytes
     * @throws IllegalArgumentException for invalid arguments
     */
    static public int asInt(byte[] array) {

        if (null == array || 4 != array.length) {
            throw new IllegalArgumentException("Length of the byte array should be 4");
        }

        return ((array[0] << 24)
                + ((array[1] & 255) << 16)
                + ((array[2] & 255) << 8) + (array[3] & 255));
    }

    /**
     * Take an input byte[] and return the int translation. For example, the
     * byte[] '0053' would return 53.
     * 
     * Taken from WsByteBufferUtils.asInt(byte[])
     * 
     * @param array
     * @return int
     * @throws NumberFormatException (if the data contains invalid digits)
     */
    static public int asIntValue(byte[] array) {
        return asIntValue(array, 0, array.length);
    }

    /**
     * Take an input byte[] and return the int translation. For example, the
     * byte[] '0053' would return 53.
     * 
     * Taken from WsByteBufferUtils.asInt(byte[])
     * 
     * @param array
     * @param offset into the array to start at
     * @param length number of bytes to review
     * @return int
     * @throws NumberFormatException (if the data contains invalid digits)
     */
    static public int asIntValue(byte[] array, int offset, int length) {

        if (null == array || array.length <= offset) {
            return -1;
        }

        int intVal = 0;
        int mark = 1;
        int digit;
        int i = offset + length - 1;
        // PK16337 - ignore trailing whitespace
        for (; offset <= i; i--) {
            char c = (char) array[i];
            if (BNFHeaders.SPACE != c && BNFHeaders.TAB != c) {
                break;
            }
        }
        for (; offset <= i; i--) {
            digit = array[i] - ZERO;
            if (0 > digit || 9 < digit) {
                // stop on any nondigit, if it's not a DASH then throw an exc
                if (DASH != array[i]) {
                    throw new NumberFormatException("Invalid digit: " + array[i]);
                }
                break;
            }
            intVal += digit * mark;
            mark *= 10;
        }

        // check for negative numbers
        if (offset <= i && array[i] == DASH) {
            intVal = -intVal;
        }

        return intVal;
    }

    /**
     * Take an input byte[] and return the int translation. For example, the
     * byte[] '0053' would return 53L.
     * 
     * @param array
     * @return long
     * @throws NumberFormatException (if the data contains invalid digits)
     */
    static public long asLongValue(byte[] array) {
        return asLongValue(array, 0, array.length);
    }

    /**
     * Take an input byte[] and return the long translation. For example, the
     * byte[] '0053' would return 53.
     * 
     * @param array
     * @param offset into the array to start at
     * @param length number of bytes to review
     * @return long
     * @throws NumberFormatException (if the data contains invalid digits)
     */

    static public long asLongValue(byte[] array, int offset, int length) {
        if (null == array || array.length <= offset) {
            return -1L;
        }

        long longVal = 0;
        long mark = 1;
        int digit;
        int i = offset + length - 1;
        // ignore trailing whitespace
        for (; offset <= i; i--) {
            char c = (char) array[i];
            if (BNFHeaders.SPACE != c && BNFHeaders.TAB != c) {
                break;
            }
        }
        for (; offset <= i; i--) {
            digit = array[i] - ZERO;
            if (0 > digit || 9 < digit) {
                // stop on any nondigit, if it's not a DASH then throw an exc
                if (DASH != array[i]) {
                    throw new NumberFormatException("Invalid digit: " + array[i]);
                }
                break;
            }
            longVal += digit * mark;
            mark *= 10;
        }

        // check for negative numbers
        if (offset <= i && array[i] == DASH) {
            longVal = -longVal;
        }

        return longVal;

    }

    /**
     * Utility method to convert a long (positive or negative) into the
     * byte[] representation. So "50" would return a byte[2] of "50", "-100"
     * would return a byte[3] of "-100".
     * 
     * @param inValue
     * @return byte[]
     */
    static public byte[] asByteArray(long inValue) {

        long value = inValue;
        // check for 0
        if (0 == value) {
            return ZERO_BYTEARRAY;
        }

        // make space for the largest long number
        byte[] bytes = new byte[SIZE_MAXLONG];
        // check for negative ints
        boolean bNegative = false;
        if (0 > value) {
            // force it positive for parsing
            bNegative = true;
            value = -value;
        }
        // now loop back through each digit in the long
        int index = SIZE_MAXLONG - 1;
        for (; 0 <= index && 0 != value; index--) {
            bytes[index] = HEX_BYTES[(int) (value % 10)];
            value /= 10;
        }

        // length is how ever many digits there were + a possible negative sign
        int len = (SIZE_MAXLONG - 1 - index);
        if (bNegative) {
            len++;
        }

        // now copy out the "real bytes" for returning
        byte[] realBytes = new byte[len];
        for (int i = len - 1, x = SIZE_MAXLONG - 1; 0 <= i; i--, x--) {
            realBytes[i] = bytes[x];
        }
        // add negative sign if we need to
        if (bNegative) {
            realBytes[0] = DASH;
        }
        return realBytes;
    }

    /**
     * Returns an array of 4 bytes that represent the integer.
     * 
     * @param value the integer
     * @return the 4 byte array
     * @throws IllegalArgumentException if the argument is less than 0
     */
    static public byte[] asBytes(int value) {

        if (0 > value) {
            throw new IllegalArgumentException("value cannot be less than zero");
        }

        byte[] result = new byte[4];
        result[0] = (byte) ((value >>> 24) & 0xFF);
        result[1] = (byte) ((value >>> 16) & 0xFF);
        result[2] = (byte) ((value >>> 8) & 0xFF);
        result[3] = (byte) ((value) & 0xFF);
        return result;
    }

    /**
     * Utility method to take a list of buffers and convert their data into
     * an English encoded string. These buffers are expected to be flipped
     * already, in that position is 0 and limit is the end of data in each
     * one.
     * 
     * @param list
     * @return String (null if input is null or no data is present in them)
     */
    static public String getEnglishString(WsByteBuffer[] list) {
        if (null == list) {
            return null;
        }
        int size = 0;
        int i;
        for (i = 0; i < list.length && null != list[i]; i++) {
            size += list[i].remaining();
        }
        if (0 == size) {
            return null;
        }
        byte[] value = new byte[size];
        int offset = 0;
        for (int x = 0; x < i; x++) {
            size = list[x].remaining();
            list[x].get(value, offset, size);
            offset += size;
            list[x].position(0);
        }
        return getEnglishString(value);
    }

    /**
     * Convert a byte[] to a lower-case String and skip the StringEncoder
     * overhead by passing through a char[] first.
     * 
     * @param bytes
     * @return String (lowercase)
     */
    static public String asLowerCaseString(byte[] bytes) {

        char chars[] = new char[bytes.length];
        char c;
        for (int i = 0; i < bytes.length; i++) {
            c = (char) (bytes[i] & 0xff);
            if ('A' <= chars[i] && 'Z' >= chars[i]) {
                chars[i] = (char) (c + 32);
            } else {
                chars[i] = c;
            }
        }
        return new String(chars);
    }

    /**
     * Writes the contents of the array to the trace log.
     * 
     * @param arr
     */
    static public void dumpArrayToTraceLog(byte[] arr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "[ ");
            if (null == arr) {
                Tr.debug(tc, "null");
            } else {
                for (int i = 0; i < arr.length; i++) {
                    Tr.debug(tc, arr[i] + " ");
                }
            }
            Tr.debug(tc, "]");
        }
    }

    /**
     * Helper function for byte[]s to simulate the String.indexOf() method.
     * 
     * @param bytes
     * @param target
     * @param start_index
     * @return int (-1 if not found)
     */
    static public int byteIndexOf(byte[] bytes, byte target, int start_index) {

        int rc = -1; // simulate String.indexOf rc
        // stop when we are out of data or hit the target byte
        for (int index = start_index; index < bytes.length; index++) {
            if (target == bytes[index]) {
                rc = index;
                break;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "byteIndexOf returning [" + rc + "]");
        }
        return rc;
    }

    /**
     * Helper function for byte[]s to simulate the String.indexOf() method.
     * max_length allows the caller to specify the maximum number of bytes
     * to compare (i.e. only check the first 10 bytes for the target).
     * 
     * @param bytes
     * @param target
     * @param start_index
     * @param max_length
     * @return int (-1 if not found)
     */
    static public int byteIndexOf(byte[] bytes, byte target, int start_index, int max_length) {
        int rc = -1; // simulate String.indexOf rc
        int length = 0;
        // stop when we are out of data, hit the target byte, or hit the user
        // defined "maximum number of bytes to search"
        for (int index = start_index; index < bytes.length; index++, length++) {
            if (length >= max_length) {
                break;
            }
            if (target == bytes[index]) {
                rc = index;
                break;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "byteIndexOf returning [" + rc + "]");
        }
        return rc;
    }

    /**
     * Helper function for byte[]s to simulate the String.substring() method.
     * 
     * @param bytes
     * @param start
     * @param end
     * @return byte[] (null if invalid input)
     */
    static public byte[] byteSubstring(byte[] bytes, int start, int end) {

        byte[] rc = null;

        if (0 > start || start > bytes.length || start > end) {
            // invalid start position
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid start position in byteSubstring: " + start);
            }
        } else if (0 > end || end > bytes.length) {
            // invalid end position
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid end position in byteSubstring: " + end);
            }
        } else {
            // now pull the substring
            int len = end - start;
            rc = new byte[len];
            System.arraycopy(bytes, start, rc, 0, len);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "byteSubstring returning: [" + getEnglishString(rc) + "]");
        }
        return rc;
    }

    /**
     * Generic method to copy the entire length of each buffer.
     * 
     * @param src
     * @param dst
     * @return byte[]
     */
    static public byte[] expandByteArray(byte[] src, byte[] dst) {
        int srcLength = (null != src) ? src.length : 0;
        int dstLength = (null != dst) ? dst.length : 0;
        return expandByteArray(src, dst, 0, srcLength, 0, dstLength);
    }

    /**
     * Helper function to concat two byte[]s into a new, larger one.
     * 
     * @param src
     * @param dst
     * @param srcPos
     * @param srcLength
     * @param dstPos
     * @param dstLength
     * @return byte[] (null if any errors)
     */
    static public byte[] expandByteArray(byte[] src, byte[] dst, int srcPos, int srcLength, int dstPos, int dstLength) {
        byte[] rc = null;
        int totalLen = 0;
        if (null != src) {
            totalLen += srcLength;
        }
        if (null != dst) {
            totalLen += dstLength;
        }
        if (0 < totalLen) {
            rc = new byte[totalLen];
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Created byte[] of size " + totalLen);
            }
            try {
                if (null != src) {
                    System.arraycopy(src, srcPos, rc, 0, srcLength);
                }
                if (null != dst) {
                    System.arraycopy(dst, dstPos, rc, srcLength, dstLength);
                }
            } catch (Exception e) {
                // no FFDC required
                // any error from arraycopy, we'll just return null
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception " + e + " while copying.");
                }
                rc = null;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "expandByteArray returning: [" + getEnglishString(rc) + "]");
        }
        return rc;
    }

    /**
     * Helper function to concat two byte[]s into a new, larger one.
     * 
     * @param src
     * @param dst
     * @param dstPos
     * @param dstLength
     * @return byte[] (null if any errors)
     */
    static public byte[] expandByteArray(byte[] src, byte[] dst, int dstPos, int dstLength) {
        byte[] rc = null;
        int srcLength = (null != src) ? src.length : 0;
        int totalLen = srcLength;
        if (null != dst) {
            totalLen += dstLength;
        }
        if (0 < totalLen) {
            rc = new byte[totalLen];
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Created byte[] of size " + totalLen);
            }
            try {
                if (null != src) {
                    System.arraycopy(src, 0, rc, 0, srcLength);
                }
                if (null != dst) {
                    System.arraycopy(dst, dstPos, rc, srcLength, dstLength);
                }
            } catch (Exception e) {
                // no FFDC required
                // any error from arraycopy, we'll just return null
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception " + e + " while copying.");
                }
                rc = null;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "expandByteArray returning: [" + getEnglishString(rc) + "]");
        }
        return rc;
    }

    /**
     * Helper method to append a byte to a byte array.
     * 
     * @param src byte array
     * @param b byte to be appended to the src byte array
     * @return target byte array
     */
    static public byte[] expandByteArray(byte[] src, byte b) {

        int srcLength = (null != src) ? src.length : 0;
        int totalLen = srcLength + 1;
        byte[] rc = new byte[totalLen];
        try {
            if (null != src) {
                System.arraycopy(src, 0, rc, 0, srcLength);
            }
            rc[srcLength] = b;
        } catch (Exception e) {
            // no FFDC required
            // any error from arraycopy, we'll just return null
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception " + e + " while copying.");
            }
            rc = null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc,
                     "expandByteArray returning: [" + getEnglishString(rc) + "]");
        }
        return rc;
    }

    /**
     * Utility method to get the bytes from a StringBuffer. These bytes will
     * be in whatever encoding was in the original chars put into the string
     * buffer object.
     * 
     * @param data
     * @return byte[]
     */
    static public byte[] getBytes(StringBuffer data) {
        if (null == data) {
            return null;
        }
        int len = data.length();
        char[] chars = new char[len];
        data.getChars(0, len, chars, 0);
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    /**
     * Utility method to get the bytes from a StringBuilder. These bytes will
     * be in whatever encoding was in the original chars put into the string
     * builder object.
     * 
     * @param data
     * @return byte[]
     */
    static public byte[] getBytes(StringBuilder data) {
        if (null == data) {
            return null;
        }
        int len = data.length();
        char[] chars = new char[len];
        data.getChars(0, len, chars, 0);
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    /**
     * Simple utility to get the bytes from a String but handle a
     * null String as well.
     * 
     * @param input
     * @return byte[]
     */
    static public byte[] getBytes(String input) {

        if (null != input) {
            int length = input.length();
            byte[] output = new byte[length];
            for (int i = 0; i < length; i++) {
                output[i] = (byte) input.charAt(i);
            }
            return output;
        }
        return null;
    }

    /**
     * Utility method to get ISO english encoded bytes from the input string.
     * If an unsupported encoding error is thrown by the conversion, then an
     * IllegalArgumentException will be thrown
     * 
     * @param data
     * @return byte[]
     * @exception IllegalArgumentException
     */
    static public byte[] getEnglishBytes(String data) {
        return getBytes(data);
    }

    /**
     * Utility method to get the ISO English string from the given bytes. If
     * this an unsupported encoding exception is thrown by the conversion, then
     * an IllegalArgumentException will be thrown.
     * 
     * @param data
     * @return String
     * @exception IllegalArgumentException
     */
    static public String getEnglishString(byte[] data) {
        if (null == data) {
            return null;
        }
        char chars[] = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            chars[i] = (char) (data[i] & 0xff);
        }
        return new String(chars);
    }

    /**
     * Convert the give character into a proper hex character.
     * 
     * @param value
     * @return char
     */
    static private char convertHex(int value) {
        return (char) (HEX_BYTES[value & 0xf]);
    }

    /**
     * Format the input value as a four digit hex number, padding with zeros.
     * 
     * @param buffer
     * @param value
     * @return StringBuilder
     */
    static private StringBuilder formatLineId(StringBuilder buffer, int value) {
        int id = value;
        char[] chars = new char[4];
        for (int i = 3; i >= 0; i--) {
            chars[i] = (char) HEX_BYTES[(id % 16) & 0xF];
            id >>= 4;
        }
        return buffer.append(chars);
    }

    /**
     * Format the next 8 bytes of the input data starting from the offset as
     * 2 digit hex characters.
     * 
     * @param buffer
     * @param data
     * @param inOffset
     * @return StringBuilder
     */
    static private StringBuilder formatHexData(StringBuilder buffer, byte[] data, int inOffset) {
        int offset = inOffset;
        int end = offset + 8;
        if (offset >= data.length) {
            // have nothing, just print empty chars
            buffer.append("                       ");
            return buffer;
        }
        buffer.append(convertHex((0xff & data[offset]) / 16));
        buffer.append(convertHex((0xff & data[offset]) % 16));
        for (++offset; offset < end; offset++) {
            if (offset >= data.length) {
                buffer.append("   ");
                continue;
            }
            buffer.append(' ');
            buffer.append(convertHex((0xff & data[offset]) / 16));
            buffer.append(convertHex((0xff & data[offset]) % 16));
        }

        return buffer;
    }

    /**
     * Format the next 16 bytes of the input data starting from the input offset
     * as ASCII characters. Non-ASCII bytes will be printed as a period symbol.
     * 
     * @param buffer
     * @param data
     * @param inOffset
     * @return StringBuilder
     */
    static private StringBuilder formatTextData(StringBuilder buffer, byte[] data, int inOffset) {
        int offset = inOffset;
        int end = offset + 16;
        for (; offset < end; offset++) {
            if (offset >= data.length) {
                buffer.append(" ");
                continue;
            }
            if (Character.isLetterOrDigit(data[offset])) {
                buffer.append((char) data[offset]);
            } else {
                buffer.append('.');
            }
        }
        return buffer;
    }

    /**
     * Debug print the entire input byte[]. This will be a sequence of 16 byte
     * lines, starting with a line indicator, the hex bytes and then the ASCII
     * representation.
     * 
     * @param data
     * @return String
     */
    static public String getHexDump(byte[] data) {
        return (null == data) ? null : getHexDump(data, data.length);
    }

    /**
     * Debug print the input byte[] up to the input maximum length. This will be
     * a sequence of 16 byte lines, starting with a line indicator, the hex bytes
     * and then the ASCII representation.
     * 
     * @param data
     * @param inLength
     * @return String
     */
    static public String getHexDump(byte[] data, int inLength) {
        // boundary checks....
        if (null == data || 0 > inLength) {
            return null;
        }
        // if we have less than the target amount, just print what we have
        int length = inLength;
        if (data.length < length) {
            length = data.length;
        }
        int numlines = (length / 16) + (((length % 16) > 0) ? 1 : 0);
        StringBuilder buffer = new StringBuilder(73 * numlines);
        for (int i = 0, line = 0; line < numlines; line++, i += 16) {
            buffer = formatLineId(buffer, i);
            buffer.append(": ");
            // format the first 8 bytes as hex data
            buffer = formatHexData(buffer, data, i);
            buffer.append("  ");
            // format the second 8 bytes as hex data
            buffer = formatHexData(buffer, data, i + 8);
            buffer.append("  ");
            // now print the ascii version, filtering out non-ascii chars
            buffer = formatTextData(buffer, data, i);
            buffer.append('\n');
        }
        return buffer.toString();
    }

    /**
     * Utility method to get the ISO English string from the given bytes. If
     * this an unsupported encoding exception is thrown by the conversion, then
     * an IllegalArgumentException will be thrown.
     * 
     * @param data
     * @param start
     * @param end
     * @return String
     * @exception IllegalArgumentException
     */
    static public String getEnglishString(byte[] data, int start, int end) {
        int len = end - start;
        if (null == data || 0 > len) {
            return null;
        }
        char chars[] = new char[len];
        for (int x = 0, i = start; i < end; x++, i++) {
            chars[x] = (char) (data[i] & 0xff);
        }
        return new String(chars);
    }

    /**
     * Utility method to skip past data in an array until it runs out of space
     * or finds the target character.
     * 
     * @param data
     * @param start
     * @param target
     * @return int (return index, equals data.length if not found)
     */
    static public int skipToChar(byte[] data, int start, byte target) {
        int index = start;
        while (index < data.length && target != data[index]) {
            index++;
        }
        return index;
    }

    /**
     * Utility method to skip past data in an array until it runs out of space
     * or finds one of the the target characters.
     * 
     * @param data
     * @param start
     * @param targets
     * @return int (return index, equals data.length if not found)
     */
    static public int skipToChars(byte[] data, int start, byte[] targets) {
        int index = start;
        int y = 0;
        byte current;
        for (; index < data.length; index++) {
            current = data[index];
            for (y = 0; y < targets.length; y++) {
                if (current == targets[y]) {
                    return index;
                }
            }
        }
        return index;
    }

    /**
     * Simple method to skip past any space characters from the starting
     * index onwards, until it finds a non-space character or the end of the
     * buffer. Returns the index it stopped on.
     * 
     * @param data
     * @param start
     * @return int
     */
    static public int skipWhiteSpace(byte[] data, int start) {
        int index = start + 1;
        while (index < data.length && BNFHeaders.SPACE == data[index]) {
            index++;
        }
        return index;
    }

    /**
     * Get the reverse case of the input character.
     * 
     * @param c
     * @return byte
     */
    static public byte reverseCase(byte c) {
        return (byte) (c + (('A' <= c && 'Z' >= c) ? 32 : -32));
    }

    /**
     * Get the reverse case of the input character.
     * 
     * @param c
     * @return char
     */
    static public char reverseCase(char c) {
        return (char) (c + (('A' <= c && 'Z' >= c) ? 32 : -32));
    }

    /**
     * Utility method to count the total number of "used" bytes in the list of
     * buffers. This would represent the size of the data if it was printed
     * out.
     * 
     * @param list
     * @return int
     */
    static public int sizeOf(WsByteBuffer[] list) {
        if (null == list) {
            return 0;
        }
        int size = 0;
        for (int i = 0; i < list.length; i++) {
            if (null != list[i]) {
                size += list[i].remaining();
            }
        }
        return size;
    }

    /**
     * Encapsulate the logic to read an entire byte array from the input stream.
     * 
     * @param in
     * @param len
     * @return new array read from stream
     * @throws IOException
     */
    static public byte[] readValue(ObjectInput in, int len) throws IOException {
        int bytesRead = 0;
        byte[] data = new byte[len];
        for (int offset = 0; offset < len; offset += bytesRead) {
            bytesRead = in.read(data, offset, len - offset);
            if (bytesRead == -1) {
                throw new IOException("Could not retrieve ");
            }
        }
        return data;
    }

    /**
     * Scan the input value for the "password=" and "client_secret" key markers and convert the
     * password value to a series of *s.
     * 
     * @param value
     * @param delimiter
     * @return String
     */
    static public String nullOutPasswords(byte[] value, byte delimiter) {
        return nullOutPasswords(getEnglishString(value), delimiter);
    }

    /**
     * Scan the input value for the "password=" and "client_secret" key markers and convert the
     * password value to a series of *s. The delimiter value can be used if
     * the search string is a sequence like "key=value<delim>key2=value2".
     * 
     * @param value
     * @param delimiter
     * @return String
     */
    static public String nullOutPasswords(String value, byte delimiter) {
        return PasswordNullifier.nullify(value, delimiter);
    }

    /**
     * Create a string that is the same length as the input, but filled with
     * characters.
     * 
     * @param value
     * @return String
     */
    static public String blockContents(byte[] value) {
        if (null == value) {
            return null;
        }
        char[] data = new char[value.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = '*';
        }
        return new String(data);
    }

    /**
     * Create a string that is the same length as the input, but filled with
     * characters.
     * 
     * @param value
     * @return String
     */
    static public String blockContents(String value) {
        if (null == value) {
            return null;
        }
        char[] data = new char[value.length()];
        for (int i = 0; i < data.length; i++) {
            data[i] = '*';
        }
        return new String(data);
    }
}