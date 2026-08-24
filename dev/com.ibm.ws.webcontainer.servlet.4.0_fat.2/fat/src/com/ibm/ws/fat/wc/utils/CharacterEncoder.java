/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

/**
 * This class defines the encoding half of character encoders. A character encoder is an algorithim for transforming 8
 * bit binary data into text (generally 7 bit ASCII or 8 bit ISO-Latin-1 text) for transmition over text channels such
 * as e-mail and network news.
 *
 * The character encoders have been structured around a central theme that, in general, the encoded text has the form:
 *
 * <pre>
 *      [Buffer Prefix]
 *      [Line Prefix][encoded data atoms][Line Suffix]
 *      [Buffer Suffix]
 * </pre>
 *
 * In the CharacterEncoder and CharacterDecoder classes, one complete chunk of data is referred to as a <i>buffer</i>.
 * Encoded buffers are all text, and decoded buffers (sometimes just referred to as buffers) are binary octets.
 *
 * To create a custom encoder, you must, at a minimum, overide three abstract methods in this class.
 * <DL>
 * <DD>bytesPerAtom which tells the encoder how many bytes to send to encodeAtom
 * <DD>encodeAtom which encodes the bytes sent to it as text.
 * <DD>bytesPerLine which tells the encoder the maximum number of bytes per line.
 * </DL>
 *
 * Several useful encoders have already been written and are referenced in the See Also list below.
 *
 * @version 1.38, 11/17/05
 * @author Chuck McManis
 * @see CharacterDecoder
 * @see BASE64Encoder
 */
public abstract class CharacterEncoder {

    /** Stream that understands "printing" */
    protected PrintStream pStream;

    /** Return the number of bytes per atom of encoding */
    abstract protected int bytesPerAtom();

    /** Return the number of bytes that can be encoded per line */
    abstract protected int bytesPerLine();

    /**
     * Encode the prefix for the entire buffer. By default is simply opens the PrintStream for use by the other functions.
     */
    protected void encodeBufferPrefix(OutputStream aStream) throws IOException {
        pStream = new PrintStream(aStream);
    }

    /**
     * Encode the suffix for the entire buffer.
     */
    protected void encodeBufferSuffix(OutputStream aStream) throws IOException {
    }

    /**
     * Encode the prefix that starts every output line.
     */
    protected void encodeLinePrefix(OutputStream aStream, int aLength) throws IOException {
    }

    /**
     * Encode the suffix that ends every output line. By default this method just prints a <newline> into the output stream.
     */
    protected void encodeLineSuffix(OutputStream aStream) throws IOException {
        pStream.println();
    }

    /** Encode one "atom" of information into characters. */
    abstract protected void encodeAtom(OutputStream aStream, byte someBytes[], int anOffset, int aLength) throws IOException;

    /**
     * This method works around the bizarre semantics of BufferedInputStream's read method.
     */
    protected int readFully(InputStream in, byte buffer[]) throws java.io.IOException {
        for (int i = 0; i < buffer.length; i++) {
            int q = in.read();
            if (q == -1)
                return i;
            buffer[i] = (byte) q;
        }
        return buffer.length;
    }

    /**
     * Encode bytes from the input stream, and write them as text characters to the output stream. This method will run
     * until it exhausts the input stream, but does not print the line suffix for a final line that is shorter than
     * bytesPerLine().
     */
    public void encode(InputStream inStream, OutputStream outStream) throws IOException {
        int j;
        int numBytes;
        byte tmpbuffer[] = new byte[bytesPerLine()];

        encodeBufferPrefix(outStream);

        while (true) {
            numBytes = readFully(inStream, tmpbuffer);
            if (numBytes == 0) {
                break;
            }
            encodeLinePrefix(outStream, numBytes);
            for (j = 0; j < numBytes; j += bytesPerAtom()) {

                if ((j + bytesPerAtom()) <= numBytes) {
                    encodeAtom(outStream, tmpbuffer, j, bytesPerAtom());
                } else {
                    encodeAtom(outStream, tmpbuffer, j, (numBytes) - j);
                }
            }
            if (numBytes < bytesPerLine()) {
                break;
            } else {
                encodeLineSuffix(outStream);
            }
        }
        encodeBufferSuffix(outStream);
    }

    /**
     * Encode the buffer in <i>aBuffer</i> and write the encoded result to the OutputStream <i>aStream</i>.
     */
    public void encode(byte aBuffer[], OutputStream aStream) throws IOException {
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
        encode(inStream, aStream);
    }

    /**
     * A 'streamless' version of encode that simply takes a buffer of bytes and returns a string containing the encoded
     * buffer.
     */
    public String encode(byte aBuffer[]) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
        String retVal = null;
        try {
            encode(inStream, outStream);
            // explicit ascii->unicode conversion
            retVal = outStream.toString("8859_1");
        } catch (Exception IOException) {
            // This should never happen.
            throw new Error("CharacterEncoder.encode internal error");
        }
        return (retVal);
    }

    /**
     * Return a byte array from the remaining bytes in this ByteBuffer.
     *
     * The ByteBuffer's position will be advanced to ByteBuffer's limit.
     *
     * To avoid an extra copy, the implementation will attempt to return the byte array backing the ByteBuffer. If this is
     * not possible, a new byte array will be created.
     */
    private byte[] getBytes(ByteBuffer bb) {
        /*
         * This should never return a BufferOverflowException, as we're careful to allocate just the right amount.
         */
        byte[] buf = null;

        /*
         * If it has a usable backing byte buffer, use it. Use only if the array exactly represents the current ByteBuffer.
         */
        if (bb.hasArray()) {
            byte[] tmp = bb.array();
            if ((tmp.length == bb.capacity()) && (tmp.length == bb.remaining())) {
                buf = tmp;
                bb.position(bb.limit());
            }
        }

        if (buf == null) {
            /*
             * This class doesn't have a concept of encode(buf, len, off), so if we have a partial buffer, we must reallocate space.
             */
            buf = new byte[bb.remaining()];

            /*
             * position() automatically updated
             */
            bb.get(buf);
        }

        return buf;
    }

    /**
     * Encode the <i>aBuffer</i> ByteBuffer and write the encoded result to the OutputStream <i>aStream</i>.
     *
     * The ByteBuffer's position will be advanced to ByteBuffer's limit.
     */
    public void encode(ByteBuffer aBuffer, OutputStream aStream) throws IOException {
        byte[] buf = getBytes(aBuffer);
        encode(buf, aStream);
    }

    /**
     * A 'streamless' version of encode that simply takes a ByteBuffer and returns a string containing the encoded buffer.
     *
     * The ByteBuffer's position will be advanced to ByteBuffer's limit.
     */
    public String encode(ByteBuffer aBuffer) {
        byte[] buf = getBytes(aBuffer);
        return encode(buf);
    }

    /**
     * Encode bytes from the input stream, and write them as text characters to the output stream. This method will run
     * until it exhausts the input stream. It differs from encode in that it will add the line at the end of a final line
     * that is shorter than bytesPerLine().
     */
    public void encodeBuffer(InputStream inStream, OutputStream outStream) throws IOException {
        int j;
        int numBytes;
        byte tmpbuffer[] = new byte[bytesPerLine()];

        encodeBufferPrefix(outStream);

        while (true) {
            numBytes = readFully(inStream, tmpbuffer);
            if (numBytes == 0) {
                break;
            }
            encodeLinePrefix(outStream, numBytes);
            for (j = 0; j < numBytes; j += bytesPerAtom()) {
                if ((j + bytesPerAtom()) <= numBytes) {
                    encodeAtom(outStream, tmpbuffer, j, bytesPerAtom());
                } else {
                    encodeAtom(outStream, tmpbuffer, j, (numBytes) - j);
                }
            }
            encodeLineSuffix(outStream);
            if (numBytes < bytesPerLine()) {
                break;
            }
        }
        encodeBufferSuffix(outStream);
    }

    /**
     * Encode the buffer in <i>aBuffer</i> and write the encoded result to the OutputStream <i>aStream</i>.
     */
    public void encodeBuffer(byte aBuffer[], OutputStream aStream) throws IOException {
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
        encodeBuffer(inStream, aStream);
    }

    /**
     * A 'streamless' version of encode that simply takes a buffer of bytes and returns a string containing the encoded
     * buffer.
     */
    public String encodeBuffer(byte aBuffer[]) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
        try {
            encodeBuffer(inStream, outStream);
        } catch (Exception IOException) {
            // This should never happen.
            throw new Error("CharacterEncoder.encodeBuffer internal error");
        }
        return (outStream.toString());
    }

    /**
     * Encode the <i>aBuffer</i> ByteBuffer and write the encoded result to the OutputStream <i>aStream</i>.
     *
     * The ByteBuffer's position will be advanced to ByteBuffer's limit.
     */
    public void encodeBuffer(ByteBuffer aBuffer, OutputStream aStream) throws IOException {
        byte[] buf = getBytes(aBuffer);
        encodeBuffer(buf, aStream);
    }

    /**
     * A 'streamless' version of encode that simply takes a ByteBuffer and returns a string containing the encoded buffer.
     *
     * The ByteBuffer's position will be advanced to ByteBuffer's limit.
     */
    public String encodeBuffer(ByteBuffer aBuffer) {
        byte[] buf = getBytes(aBuffer);
        return encodeBuffer(buf);
    }
}