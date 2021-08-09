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
import java.io.RandomAccessFile;
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
 */
public final class UtilImpl_ReadBufferPartial implements UtilImpl_ReadBuffer {
    private final String path;

    @Trivial
    public String getPath() {
        return path;
    }

    private final RandomAccessFile file;
    private final int fileLength;

    private int filePos;

    private final byte[] buffer;

    private int bufferFill;
    private int bufferPos;
    private int bufferAvail;

    private final String encoding;
    private final Charset charset;

    @Trivial
    public String getEncoding() {
        return encoding;
    }

    //

    public static final String UTF_8 = UtilImpl_WriteBuffer.UTF_8;

    public UtilImpl_ReadBufferPartial(String path, int bufferSize)
        throws IOException {
        this(path, bufferSize, UTF_8);
    }

    public UtilImpl_ReadBufferPartial(String path, RandomAccessFile file, int bufferSize)
        throws IOException {
        this(path, file, bufferSize, UTF_8);
    }

    public UtilImpl_ReadBufferPartial(String path, int bufferSize, String encoding)
        throws IOException {

        this( path,
              new RandomAccessFile(path, "r"), // throws IOException
              bufferSize,
              encoding);
    }

    /**
     * Create a read buffer for a specified file.
     * 
     * Immediately fill the buffer from the file.
     *
     * @param path The path associated with the specified file.
     * @param file The file from which to read.
     * @param bufferSize The size of the buffer.
     *
     * @throws IOException Thrown if buffer creation failed.
     *     That usually means the initial read failed.
     */
    public UtilImpl_ReadBufferPartial(
        String path, RandomAccessFile file, int bufferSize,
        String encoding) throws IOException {

        this.path = path;
        this.file = file;

        long rawFileLength = this.file.length();
        if ( rawFileLength > Integer.MAX_VALUE ) {
            throw new IOException(
                "File length [ " + rawFileLength + " ]" +
                " greater than [ " + Integer.MAX_VALUE + " ]" +
                " for [ " + path + " ]");
        }
        this.fileLength = (int) rawFileLength; 

        this.encoding = encoding;
        this.charset = Charset.forName(encoding);

        if ( this.fileLength < bufferSize ) {
            this.bufferFill = this.fileLength;
        } else {
            this.bufferFill = bufferSize;
        }

        this.buffer = new byte[ this.bufferFill ];
        this.bufferPos = 0;
        this.bufferAvail = this.bufferFill;

        this.file.read(buffer, 0, this.bufferFill);
        this.filePos = this.bufferFill;
    }

    /**
     * Close the buffer.  Close the backing file. 
     *
     * @throws IOException Thrown if the close failed.
     */
    public void close() throws IOException {
        file.close();
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
            throw new IOException("Read past end of file on [ " + path + " ]");
        }

        byte readByte = buffer[bufferPos];
        bufferPos++;
        bufferAvail--;

        if ( bufferAvail == 0 ) {
            int fileRemaining = fileLength - filePos;

            if ( fileRemaining >= buffer.length ) {
                bufferFill = buffer.length;
            } else {
                bufferFill = fileRemaining;
            }

            if ( bufferFill != 0 ) {
                file.read(buffer, 0, bufferFill);
                filePos += bufferFill;
            }

            bufferPos = 0;
            bufferAvail = bufferFill;
        }

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
            throw new IOException("Disallowed read of [ " + len + " from [ " + path + " ]");
        }

        // Most expected case ...

        if ( len < bufferAvail ) {
            System.arraycopy(buffer, bufferPos, bytes, offset, len);
            bufferPos += len;
            bufferAvail -= len;
            return;
        }

        // Can't satisfy the read quickly; can it be satisfied at all?

        int fileAvail = (fileLength - filePos) + bufferAvail;
        if ( len > fileAvail ) { 
            throw new IOException(
                "Read [ " + len + " ] at [ " + (fileLength - fileAvail) + " ]" +
                " past end of file [ " + fileLength + " ]" +
                " on [ " + path + " ]");
        }

        // Transfer as much as possible from the current buffer.

        System.arraycopy(buffer, bufferPos, bytes, offset, bufferAvail);
        offset += bufferAvail;
        len -= bufferAvail;
        fileAvail -= bufferAvail;

        // bufferPos and bufferAvail still need to be updated ...

        // If more remains to be read, handle all complete spans at once.

        if ( len > 0 ) {
            int spanLength = (len / buffer.length) * buffer.length;
            if ( spanLength > 0 ) {
                file.read(bytes, offset, spanLength);
                filePos += spanLength;
                offset += spanLength;
                len -= spanLength;

                fileAvail -= spanLength;
            }
        }

        // Refresh the buffer for the remainder of the read.

        if ( fileAvail > buffer.length ) {
            bufferFill = buffer.length;
        } else {
            bufferFill = fileAvail;
        }

        // Either the initial read or the spanned read may have
        // consumed the last bytes of the file, in which case
        // bufferFill will be zero.

        if ( bufferFill != 0 ) {
            file.read(buffer, 0, bufferFill);
            filePos += bufferFill;
        }

        // Per the initial check of the read length against the
        // available file bytes, and because of the spanning read,
        // the remaining read length must be less than or equal to
        // be buffer fill.

        if ( len > 0 ) {
            System.arraycopy(buffer, 0, bytes, offset, len);
        }

        bufferPos = len;
        bufferAvail = bufferFill - len;

//        if ( filePos > fileLength ) {
//            System.out.println("Strange");
//        }
    }

    @Trivial
    public int getFileLength() {
        return fileLength;
    }

    public void seekEnd(int offset) throws IOException {
        seek( getFileLength() + offset );
    }

    public void seek(int offset) throws IOException {
        boolean doReload;
    
        if ( offset > filePos ) {
            if ( offset > fileLength ) {
                throw new IOException(
                    "Offset [ " + offset + " ]" +
                    " past end [ " + fileLength + " ]" +
                    " of [ " + path + " ]");
            } else {
                doReload = true;
            }

        } else {
            int bufferStart = filePos - bufferFill;

            if ( offset < bufferStart ) {
                if ( offset < 0 ) {
                    throw new IOException(
                        "Offset [ " + offset + " ]" +
                        " past beginning  of [ " + path + " ]");
                } else {
                    doReload = true;
                }

            } else {
                bufferPos = offset - bufferStart;
                bufferAvail = bufferFill - bufferPos;
                doReload = false;
            }
        }

        if ( doReload ) {
            file.seek(offset);
            filePos = offset;

            int fileRemaining = fileLength - offset;

            if ( fileRemaining > buffer.length ) {
                bufferFill = buffer.length;
            } else {
                bufferFill = fileRemaining;
            }

            if ( bufferFill > 0 ) {
                file.read(buffer, 0, bufferFill);
                filePos += bufferFill;
            }

            bufferPos = 0;
            bufferAvail = bufferFill;
        }
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
                " for [ " + path + " ]");
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
                "Failed read of [ " + path + " ]:" +
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
