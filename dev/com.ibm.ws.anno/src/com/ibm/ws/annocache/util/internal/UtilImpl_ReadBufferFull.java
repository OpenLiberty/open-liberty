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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Simple read buffer.  This implementation reads
 * the entire contents of the target file.
 */
public final class UtilImpl_ReadBufferFull implements UtilImpl_ReadBuffer {
    private final File file;

    public File getFile() {
        return file;
    }

    public String getPath() {
        return getFile().getPath();
    }

    //

    private final byte[] buffer;

    private int bufferFill;
    private int bufferPos;
    private int bufferAvail;

    //

    private final String encoding;
    private final Charset charset;

    @Trivial
    public String getEncoding() {
        return encoding;
    }

    //

    public static final String UTF_8 = UtilImpl_WriteBuffer.UTF_8;

    public UtilImpl_ReadBufferFull(String path, String encoding)
        throws IOException {

        this.file = new File(path);

        this.buffer = UtilImpl_FileUtils.readFully(this.file);
        this.bufferFill = this.buffer.length;

        this.encoding = encoding;
        this.charset = Charset.forName(encoding);

        this.bufferPos = 0;
        this.bufferAvail = this.bufferFill;
    }

    public void close() throws IOException {
        // NO-OP
    }

    @Trivial
    public static int asInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value <<= 8;
            value |= (b[i] & 0xFF);
        }
        return value;
    }

    private final byte[] smallIntBytes = new byte[2];

    /**
     * Read a small (two byte) integer from the buffer.
     *
     * @return The small integer which was read from the buffer.
     *
     * @throws IOException Thrown if the read failed.
     */
    @Trivial
    public int readSmallInt() throws IOException {
        read(smallIntBytes, 0, 2);
        return ( ((smallIntBytes[0] & 0xFF) << 8) |
                 ((smallIntBytes[1] & 0xFF) << 0) );
    }

    @Trivial
    public static int convertLargeInt(byte[] bytes) {
        return ( ((bytes[0] & 0xFF) << 24) |
                 ((bytes[1] & 0xFF) << 16) |
                 ((bytes[2] & 0xFF) <<  8) |
                 ((bytes[3] & 0xFF) <<  0) );
    }

    private final byte[] largeIntBytes = new byte[4];

    /**
     * Read a large (two byte) integer from the buffer.
     *
     * @return The large integer which was read from the buffer.
     *
     * @throws IOException Thrown if the read failed.
     */
    @Trivial
    public int readLargeInt() throws IOException {
        read(largeIntBytes, 0, 4);
        return convertLargeInt(largeIntBytes);
    }

    @Trivial
    public int read() throws IOException {
        if ( bufferAvail == 0 ) {
            throw new IOException("Read past end of file on [ " + getPath() + " ]");
        }

        byte readByte = buffer[bufferPos];
        bufferPos++;
        bufferAvail--;

        return readByte;
    }

    @Trivial
    public void read(byte[] bytes) throws IOException {
        read(bytes, 0, bytes.length);
    }

    @Trivial
    public void read(byte[] bytes, int offset, int len) throws IOException {
        // Quick checks ...

        if ( len == 0 ) {
            return; // Nothing to do
        } else if ( len < 0 ) {
            throw new IOException(
                "Disallowed read of [ " + len + " ] bytes " +
                " from [ " + getPath() + " ]");
        } else if ( len > bufferAvail ) {
            throw new IOException(
                "Read past end of file." +
                " Requested [ " + len + " ] bytes ; available [ " + bufferAvail + " ] bytes " +
                " from [ " + getPath() + " ]");
        }

        System.arraycopy(buffer, bufferPos, bytes, offset, len);
        bufferPos += len;
        bufferAvail -= len;
        return;
    }

    @Trivial
    public int getFileLength() {
        return bufferFill;
    }

    public void seekEnd(int offset) throws IOException {
        seek( getFileLength() + offset );
    }

    public void seek(int offset) throws IOException {
        if ( offset < 0 ) {
            throw new IOException(
                "Offset [ " + offset + " ]" +
                " past beginning" +
                " of [ " + getPath() + " ]");
        } else if ( offset > bufferFill ) {
            throw new IOException(
                "Offset [ " + offset + " ]" +
                " past end [ " + bufferFill + " ]" +
                " of [ " + getPath() + " ]");
        }

        bufferPos = offset;
        bufferAvail = bufferFill - bufferPos;
    }

    //

    private final int MAX_STRING = UtilImpl_WriteBuffer.MAX_STRING;
    private final int MAX_WIDTH = UtilImpl_WriteBuffer.MAX_WIDTH;
    private final byte[] stringBytes = new byte[MAX_STRING];
    private final byte[] nullBytes = new byte[MAX_WIDTH];

    @Trivial
    public int validSmallInt() throws IOException {
        read(smallIntBytes, 0, 2);

        int smallInt = ( ((smallIntBytes[0] & 0xFF) << 8) |
                         ((smallIntBytes[1] & 0xFF) << 0) );

        if ( smallInt > MAX_STRING ) {
            throw new IOException(
                "String width [ " + smallInt + " ]" +
                " greater than allowed [ " + MAX_STRING + " ]" +
                " for [ " + getPath() + " ]");
        }

        return smallInt;
    }

    @Trivial
    public String readString() throws IOException {
        int valueLength = validSmallInt();
        read(stringBytes, 0, valueLength);
        return new String(stringBytes, 0, valueLength, charset);
    }

    @Trivial
    public String readString(int width) throws IOException {
        int valueLength = validSmallInt();
        read(stringBytes, 0, valueLength);

        int nullCount = (width - 3) - valueLength;
        if ( nullCount > 0 ) {
            // TODO: Implement seek on the reader.            
            read(nullBytes, 0, nullCount);
        }

        return new String(stringBytes, 0, valueLength, charset);
    }

    @Trivial
    public void requireByte(byte fieldByte) throws IOException {
        byte actualByte = (byte) read();

        if ( actualByte != fieldByte ) {
            throw new IOException(
                "Failed read of [ " + getPath() + " ]:" +
                " Expected field byte [ " + fieldByte + " ]" +
                " actual field byte [ " + actualByte + " ]");
        }
    }

    @Trivial
    public String requireField(byte fieldByte) throws IOException {
        requireByte(fieldByte);
        return readString();
    }

    @Trivial
    public String requireField(byte fieldByte, int width) throws IOException {
        requireByte(fieldByte);
        return readString(width);
    }
}
