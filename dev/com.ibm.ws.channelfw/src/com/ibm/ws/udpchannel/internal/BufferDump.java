/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.udpchannel.internal;

import java.nio.ByteBuffer;

public class BufferDump {
    /** HEX character list */
    private static final byte[] HEX_BYTES = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a',
                                             (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

    /**
     * Convert the give character into a proper hex character.
     * 
     * @param value
     * @return char
     */
    static private char convertHex(int value) {
        return (char) (HEX_BYTES[value & 0xf]);
    }

    static public String getHexDump(ByteBuffer buffer, boolean writeBuffer) {

        int position = buffer.position();
        int limit = buffer.limit();

        //
        // If its a write buffer, we do not need to flip the buffer
        //
        if (!writeBuffer)
            buffer.flip();

        int len = buffer.limit();
        byte data[] = new byte[len];
        buffer.get(data);

        buffer.limit(limit);
        buffer.position(position);

        return getHexDump(data, len);
    }

    /**
     * Debug print the input byte[] up to the input maximum length. This will be
     * a sequence of 16 byte lines, starting with a line indicator, the hex
     * bytes and then the ASCII representation.
     * 
     * @param data
     * @param length
     * @return String
     */
    static public String getHexDump(byte[] data, int length) {
        // boundary checks....
        if (null == data || 0 > length) {
            return null;
        }
        // if we have less than the target amount, just print what we have
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
     * Format the input value as a four digit hex number, padding with zeros.
     * 
     * @param buffer
     * @param value
     * @return StringBuilder
     */
    static private StringBuilder formatLineId(StringBuilder buffer, int value) {
        char[] chars = new char[4];
        for (int i = 3; i >= 0; i--) {
            chars[i] = (char) HEX_BYTES[(value % 16) & 0xF];
            value >>= 4;
        }
        return buffer.append(chars);
    }

    /**
     * Format the next 8 bytes of the input data starting from the offset as 2
     * digit hex characters.
     * 
     * @param buffer
     * @param data
     * @param offset
     * @return StringBuilder
     */
    static private StringBuilder formatHexData(StringBuilder buffer, byte[] data, int offset) {
        int end = offset + 8;
        if (offset >= data.length) {
            // have nothing, just print empty chars
            buffer.append("                       ");
            return buffer;
        }
        buffer.append(convertHex(data[offset] / 16));
        buffer.append(convertHex(data[offset] % 16));
        for (++offset; offset < end; offset++) {
            if (offset >= data.length) {
                buffer.append("   ");
                continue;
            }
            buffer.append(' ');
            buffer.append(convertHex(data[offset] / 16));
            buffer.append(convertHex(data[offset] % 16));
        }

        return buffer;
    }

    /**
     * Format the next 16 bytes of the input data starting from the input offset
     * as ASCII characters. Non-ASCII bytes will be printed as a period symbol.
     * 
     * @param buffer
     * @param data
     * @param offset
     * @return StringBuilder
     */
    static private StringBuilder formatTextData(StringBuilder buffer, byte[] data, int offset) {
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

}
