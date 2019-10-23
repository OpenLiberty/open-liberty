/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Simple write buffer.  Provides basic write capability, and
 * a count of bytes written.  See {@link #getTotalWritten()}.
 * 
 * Data is transferred to an internal buffer, which is written
 * to the output stream only when the buffer is full, when the
 * buffer is flushed, or when the buffer is closed.
 * 
 * Closing the buffer flushes buffered data and closes the output
 * stream.
 * 
 * Tracking counts only bytes written since the buffer was created.
 * Tracking counts bytes written to the buffer, including bytes
 * not yet written to the output stream.
 * 
 * This class is marked final as an assist to runtime optimization.
 * 
 * This implementation is not thread safe.
 */
public final class UtilImpl_WriteBuffer {
    private final String path;

    @Trivial
    public String getPath() {
        return path;
    }

    private final OutputStream outputStream;

    private final int bufferSize;
    private final byte[] buffer;

    private int bufferFill;
    private int bufferAvail;

    private int totalWritten;

    private final String encoding;
    private final Charset charset;

    @Trivial
    public String getEncoding() {
        return encoding;
    }

    //

    public static final String UTF_8 = "UTF-8";

    public UtilImpl_WriteBuffer(
            String path, OutputStream outputStream, int bufferSize)
            throws IOException {

        this(path, outputStream, bufferSize, UTF_8);
    }

    @SuppressWarnings("unused") // Allow this to be thrown in the future.
    public UtilImpl_WriteBuffer(
        String path, OutputStream outputStream, int bufferSize,
        String encoding)
        throws IOException {

        this.path = path;
        this.outputStream = outputStream;

        this.bufferSize = bufferSize;
        this.buffer = new byte[bufferSize];

        this.bufferFill = 0;
        this.bufferAvail = bufferSize;

        this.totalWritten = 0;

        this.encoding = encoding;
        this.charset = Charset.forName(encoding);
    }

    /**
     * Answer the total number of bytes written to the buffer.
     * This is not the same as the number of bytes written to the
     * backing file, which will be lower number until the buffer
     * contents are flushed.
     *
     * @return The total number of bytes written to the buffer.
     *
     * @throws IOException Thrown in case of an error.  Not currently
     *     thrown by this implementation, but provided for alternate
     *     implementations.
     */
    public int getTotalWritten() throws IOException {
        return totalWritten;
    }

    /**
     * Write out the buffer contents to the backing file.
     *
     * @throws IOException Thrown if the write failed.
     */
    public void flush() throws IOException {
        if ( bufferFill > 0 ) {
            outputStream.write(buffer,  0,  bufferFill); // throws IOException
            bufferFill = 0;
            bufferAvail = bufferSize;

            outputStream.flush(); // throws IOException
        }
    }

    /**
     * Flush the buffer contents then close the backing file.
     *
     * @throws IOException Thrown if the flush or close failed.
     */
    public void close() throws IOException {
        try {
            flush(); // throws IOException
        } finally {
            outputStream.close(); // throws IOException
        }
    }

    @Trivial
    public static byte[] asBytes(int value) {
        byte[] result = new byte[4];
        for (int i = 4; i >= 0; i--) {
            result[i] = (byte)(value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private final byte[] smallIntBytes = new byte[2];

    /**
     * Write the first two bytes of a small integer to the buffer.
     *
     * @param smallInt A small integer to write to the buffer.
     *
     * @throws IOException Thrown if the write failed.  Usually
     *     because the write to the backing file failed.
     */
    @Trivial
    public void writeSmallInt(int smallInt) throws IOException {
//        if ( smallInt > this.MAX_STRING ) {
//            System.out.println("Too big!");
//        }
        smallIntBytes[0] = (byte) ((smallInt & 0xFF00) >> 8);
        smallIntBytes[1] = (byte) ((smallInt & 0x00FF) >> 0);

        write(smallIntBytes, 0, 2);
    }

    @Trivial
    public static void convertLargeInt(int value, byte[] bytes) {
        bytes[0] = (byte) ((value & 0xFF000000) >> 24);
        bytes[1] = (byte) ((value & 0x00FF0000) >> 16);
        bytes[2] = (byte) ((value & 0x0000FF00) >> 8);
        bytes[3] = (byte) ((value & 0x000000FF) >> 0);
    }

    private final byte[] largeIntBytes = new byte[4];

    /**
     * Write the first two bytes of a large integer to the buffer.
     *
     * @param largeInt A large integer to write to the buffer.
     *
     * @throws IOException Thrown if the write failed.  Usually
     *     because the write to the backing file failed.
     */
    @Trivial
    public void writeLargeInt(int largeInt) throws IOException {
        convertLargeInt(largeInt, largeIntBytes);
        write(largeIntBytes, 0, 4);
    }

    /**
     * Write an large integer field.
     *
     * @param fieldByte The byte of the field.
     * @param value The value of the field.
     *
     * @throws IOException Thrown if the write failed.  Usually
     *     because the write to the backing file failed.
     */
    @Trivial
    public void writeLargeInt(byte fieldByte, int value) throws IOException {
        write(fieldByte);
        writeLargeInt(value);
    }

    /**
     * Write a single byte to the buffer.  If the buffer
     * is completely filled after the write, the buffer
     * is immediately flushed.
     * 
     * @param b The byte to write to the buffer.
     *
     * @throws IOException Thrown if the write failed.  Usually
     *     because the write to the backing file failed.
     */
    @Trivial
    public void write(byte b) throws IOException {
        buffer[bufferFill] = b;
        bufferFill++;
        bufferAvail--;
        totalWritten++;

        if ( bufferAvail == 0 ) {
            outputStream.write(buffer,  0,  bufferSize); // throws IOException
            bufferFill = 0;
            bufferAvail = bufferSize;
        }
    }

    /**
     * Write bytes to the buffer.
     *
     * Implemented as a direct forward to {@link #write(byte[], int, int)},
     * with an offset of zero and a length equal to the size of the
     * byte array.
     *
     * @param byte Bytes to write to the buffer.
     *
     * @throws IOException Thrown if the write failed.  Usually, because
     *     a write to the backing file failed. 
     */
    @Trivial
    public void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    /**
     * Write bytes to the buffer.
     * 
     * Writes which completely fill the buffer are immediately flushed.
     *
     * @param bytes Bytes to write to the buffer.
     * @param offset The offset into the buffer at which to begin the write.
     * @param len The number of bytes to write.
     *
     * @throws IOException Thrown if the write failed.  Usually, because
     *     a write to the backing file failed.
     */
    @Trivial
    public void write(byte[] bytes, int offset, int len) throws IOException {
        if ( offset < 0 ) {
            throw new IllegalArgumentException(
                "Write offset [ " + offset + " ] is less than zero for [ " + path + " ]");
        } else if ( len < 0 ) {
            throw new IllegalArgumentException(
                "Write length [ " + len + " ] is less than zero for [ " + path + " ]");
        }

        if ( len < bufferAvail ) {
            // Case 1: The new bytes fit into the buffer with spare room.
            //         Simply copy in the bytes.  No writes are needed.

            System.arraycopy(bytes, offset, buffer, bufferFill, len);

            bufferFill += len;
            bufferAvail -= len;

            totalWritten += len;

            // offset += len; // Dead variable.
            // len = 0; // Dead variable.

        } else {
            // Case 2: The new bytes overflow the buffer.  One or more
            //         write is needed.

            // If there are bytes already in the buffer, copy in as many
            // new bytes as will fit, then write out the completed buffer.

            if ( bufferFill > 0 ) {
                System.arraycopy(bytes, offset, buffer, bufferFill, bufferAvail);

                offset += bufferAvail;
                len -= bufferAvail; 

                totalWritten += bufferAvail;

                // bufferFill = bufferSize; // Dead assignment: Updated after the write.
                // bufferAvail = 0; // Dead assignment: Updated after the write.

                outputStream.write(buffer, 0, bufferSize);
                bufferFill = 0;
                bufferAvail = bufferSize;
            }

            // Next, put unprocessed input bytes into the buffer.
            //
            // As an optimization, immediately write out all input
            // bytes which would span the buffer.  That avoids a
            // redundant copy to the buffer.
            //
            // Write input bytes for *all* complete traversals of the
            // buffer.

            if ( len >= bufferSize ) {
                int spanLen = (len / bufferSize) * bufferSize;

                outputStream.write(bytes, offset, spanLen);

                // bufferFill is still 0
                // bufferAvail is still bufferSize

                totalWritten += spanLen;

                offset += spanLen;
                len -= spanLen;
            }

            // Put len input bytes into the buffer.  Because
            // all complete buffer spans have been removed from the
            // input bytes, the len bytes must fit into the
            // buffer with space left over.

            if ( len > 0 ) {
                System.arraycopy(bytes, offset, buffer, 0, len);

                bufferFill += len;
                bufferAvail -= len;

                totalWritten += len;

                // offset += len; // Dead variable
                // len = 0; // Dead variable
            }
        }
    }

    //

    public static final int MAX_STRING = 512;

    @Trivial
    public byte[] asBytes(String text) {
        byte[] textBytes = text.getBytes(charset);
        if ( textBytes.length > MAX_STRING ) {
            throw new IllegalArgumentException(
                "String width [ " + textBytes.length + " ]" +
                " too long for width [ " + MAX_STRING + " ]");
        }
        return textBytes;
    }

    @Trivial
    public int write(String value) throws IOException {
        byte[] valueBytes = asBytes(value);
        int numValueBytes = valueBytes.length;

        writeSmallInt(numValueBytes);
        write(valueBytes, 0, numValueBytes);

        return 2 + numValueBytes;
    }

    @Trivial
    public int write(byte fieldByte, String value) throws IOException {
        byte[] valueBytes = asBytes(value);
        int numValueBytes = valueBytes.length;

        write(fieldByte);
        writeSmallInt(numValueBytes);
        write(valueBytes, 0, numValueBytes);

        return 1 + 2 + numValueBytes;
    }

    public static final int MAX_WIDTH = 60;

    private final byte[] nullBytes = new byte[MAX_WIDTH];

    @Trivial
    public void write(byte fieldByte, String value, int width) throws IOException {
        byte[] valueBytes = asBytes(value);
        int numValueBytes = valueBytes.length;

        int missingBytes = (width - 3) - numValueBytes;
        if ( missingBytes < 0 ) {
            throw new IllegalArgumentException("Value [ " + value + " ] too long for width [ " + width + " ]");
        } else if ( missingBytes > nullBytes.length ) {
            throw new IllegalArgumentException("Value [ " + value + " ] too short for width [ " + width + " ]; maximum shortfall [ " + nullBytes.length + " ]");
        }

        write(fieldByte);
        writeSmallInt(numValueBytes);
        write(valueBytes, 0, numValueBytes);

        if ( missingBytes > 0 ) {
            write(nullBytes, 0, missingBytes);
        }
    }
}
