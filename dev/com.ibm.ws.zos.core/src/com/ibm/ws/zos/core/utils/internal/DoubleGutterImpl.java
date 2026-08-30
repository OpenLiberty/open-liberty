/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import com.ibm.ws.zos.core.utils.DoubleGutter;

/**
 * Helper to format byte[] in a double gutter format
 */
public class DoubleGutterImpl implements DoubleGutter {

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
     * @param reason
     *                   int representation of reason the component is stopping
     */
    protected void deactivate(int reason) {

    }

    /**
     * The expected EBCDIC encoding of character sequences is native main memory.
     */
    private final Charset ebcdicCharset = Charset.forName("IBM-1047");

    /**
     * An ASCII based character set that is equivalent to IBM-1047.
     */
    private final Charset asciiCharset = Charset.forName("ISO8859-1");

    /**
     * Determine if a character is a printable ASCII character.
     *
     * @param ch the character to test
     *
     * @return true if the character is a code point below 128 and it is not
     *         an ISO control character
     */
    boolean isPrintable(char ch) {
        return (ch < 128) && !Character.isISOControl(ch);
    }

    /**
     * Format a byte array into a double-gutter hex dump that's more
     * suitable for human consumption. This format closely resembles
     * the one used in traditional WAS for z/OS.
     *
     * @param address the address of the buffer
     * @param data    the contents of the area to be traced
     * @return String representing formatted byte[] with ASCII/EBCDIC Gutters
     */
    @Override
    public String asDoubleGutter(long address, byte[] data) {

        if (data == null) {
            data = new byte[0];
        }

        StringBuilder sb = new StringBuilder();

        // Build a description that includes the address and length
        sb.append(String.format("  data_address=%08x_%08x, data_length=%d", (int) (address >>> 32), (int) address, data.length));

        // Bail early if there's nothing to trace
        if (data.length == 0 || address == 0) {
            return sb.toString();
        }

        // Print the double gutter header
        sb.append("\n");
        sb.append("  +--------------------------------------------------------------------------+\n");
        sb.append(String.format("  |OSet| A=%016x Length=%07x |     EBCDIC     |     ASCII      |\n", address, data.length));
        sb.append("  +----+-----------------------------------+----------------+----------------+\n");

        int i;
        StringBuilder binary = new StringBuilder();
        StringBuilder ebcdic = new StringBuilder();
        StringBuilder ascii = new StringBuilder();

        CharBuffer asciiBuffer = asciiCharset.decode(ByteBuffer.wrap(data));
        CharBuffer ebcdicBuffer = ebcdicCharset.decode(ByteBuffer.wrap(data));
        String digits = "0123456789ABCDEF";

        // Limit the amount of data to trace
        int maxLength = (data.length <= 0x4000) ? data.length : 0x4000;

        // Iterate over the bytes
        for (i = 0; i < maxLength; i++) {
            binary.append(digits.charAt((data[i] & 0xf0) >>> 4));
            binary.append(digits.charAt(data[i] & 0x0f));

            // Add a space between words
            if (i % 4 == 3 && i % 16 != 15) {
                binary.append(' ');
            }

            // Update EBCDIC gutter
            char ebcdicChar = ebcdicBuffer.get(i);
            if (isPrintable(ebcdicChar)) {
                ebcdic.append(ebcdicChar);
            } else {
                ebcdic.append('.');
            }

            // Update ASCII gutter
            char asciiChar = asciiBuffer.get(i);
            if (isPrintable(asciiChar)) {
                ascii.append(asciiChar);
            } else {
                ascii.append('.');
            }

            // End of line
            if (i % 16 == 15) {
                String offset = String.format("%04x", i & (-1 & ~0xF));
                String line = String.format("  |%s|%s|%s|%s|\n", offset, binary, ebcdic, ascii);
                sb.append(line);

                // Reset the strings
                binary = new StringBuilder();
                ebcdic = new StringBuilder();
                ascii = new StringBuilder();
            }
        }

        // Finish the last line if needed
        if (i % 16 != 0) {
            String offset = String.format("%04x", i & (-1 & ~0xF));
            String line = String.format("  |%s|%-35s|%-16s|%-16s|\n", offset, binary, ebcdic, ascii);
            sb.append(line);
        }

        sb.append("  +--------------------------------------------------------------------------+");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String asDoubleGutter(long address, ByteBuffer data) {

        byte[] byteData = null;

        if (data != null) {
            byteData = new byte[data.remaining()];
            data.duplicate().get(byteData);
        }

        return asDoubleGutter(address, byteData);
    }

}
