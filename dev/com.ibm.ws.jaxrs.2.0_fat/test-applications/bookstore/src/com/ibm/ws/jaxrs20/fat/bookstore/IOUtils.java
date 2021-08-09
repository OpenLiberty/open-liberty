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
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

public final class IOUtils {
    public static final Charset UTF8_CHARSET = Charset.forName("utf-8");
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private IOUtils() {

    }

    /**
     * Use this function instead of new String(byte[], String) to avoid surprises from
     * non-standard default encodings.
     *
     * @param bytes
     * @param charsetName
     */
    public static String newStringFromBytes(byte[] bytes, String charsetName) {
        try {
            return new String(bytes, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Impossible failure: Charset.forName(\""
                                       + charsetName + "\") returns invalid name.");

        }
    }

    /**
     * Use this function instead of new String(byte[]) to avoid surprises from non-standard default encodings.
     *
     * @param bytes
     */
    public static String newStringFromBytes(byte[] bytes) {
        return newStringFromBytes(bytes, UTF8_CHARSET.name());
    }

    /**
     * Use this function instead of new String(byte[], int, int, String)
     * to avoid surprises from non-standard default encodings.
     *
     * @param bytes
     * @param charsetName
     * @param start
     * @param length
     */
    public static String newStringFromBytes(byte[] bytes, String charsetName, int start, int length) {
        try {
            return new String(bytes, start, length, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Impossible failure: Charset.forName(\""
                                       + charsetName + "\") returns invalid name.");

        }
    }

    /**
     * Use this function instead of new String(byte[], int, int)
     * to avoid surprises from non-standard default encodings.
     *
     * @param bytes
     * @param start
     * @param length
     */
    public static String newStringFromBytes(byte[] bytes, int start, int length) {
        return newStringFromBytes(bytes, UTF8_CHARSET.name(), start, length);
    }

    public static int copyAndCloseInput(final InputStream input,
                                        final OutputStream output, int bufferSize) throws IOException {
        try {
            return copy(input, output, bufferSize);
        } finally {
            input.close();
        }
    }

    public static void copyAndCloseInput(final Reader input,
                                         final Writer output) throws IOException {
        try {
            copy(input, output, DEFAULT_BUFFER_SIZE);
        } finally {
            input.close();
        }
    }

    public static void copyAndCloseInput(final Reader input,
                                         final Writer output, int bufferSize) throws IOException {
        try {
            copy(input, output, bufferSize);
        } finally {
            input.close();
        }
    }

    public static int copy(final InputStream input, final OutputStream output,
                           int bufferSize) throws IOException {
        int avail = input.available();
        if (avail > 262144) {
            avail = 262144;
        }
        if (avail > bufferSize) {
            bufferSize = avail;
        }
        final byte[] buffer = new byte[bufferSize];
        int n = 0;
        n = input.read(buffer);
        int total = 0;
        while (-1 != n) {
            if (n == 0) {
                throw new IOException("0 bytes read in violation of InputStream.read(byte[])");
            }
            output.write(buffer, 0, n);
            total += n;
            n = input.read(buffer);
        }
        return total;
    }

    /**
     * Copy at least the specified number of bytes from the input to the output
     * or until the inputstream is finished.
     *
     * @param input
     * @param output
     * @param atLeast
     * @throws IOException
     */
    public static void copyAtLeast(final InputStream input,
                                   final OutputStream output,
                                   int atLeast) throws IOException {
        final byte[] buffer = new byte[4096];
        int n = atLeast > buffer.length ? buffer.length : atLeast;
        n = input.read(buffer, 0, n);
        while (-1 != n) {
            if (n == 0) {
                throw new IOException("0 bytes read in violation of InputStream.read(byte[])");
            }
            output.write(buffer, 0, n);
            atLeast -= n;
            if (atLeast <= 0) {
                return;
            }
            n = atLeast > buffer.length ? buffer.length : atLeast;
            n = input.read(buffer, 0, n);
        }
    }

    public static void copy(final Reader input, final Writer output,
                            final int bufferSize) throws IOException {
        final char[] buffer = new char[bufferSize];
        int n = 0;
        n = input.read(buffer);
        while (-1 != n) {
            output.write(buffer, 0, n);
            n = input.read(buffer);
        }
    }

    public static String toString(final InputStream input) throws IOException {
        return toString(input, DEFAULT_BUFFER_SIZE);
    }

    public static String toString(final InputStream input, String charset) throws IOException {
        return toString(input, DEFAULT_BUFFER_SIZE, charset);
    }

    public static String toString(final InputStream input, int bufferSize)
                    throws IOException {
        return toString(input, bufferSize, null);
    }

    public static String toString(final InputStream input, int bufferSize, String charset)
                    throws IOException {

        int avail = input.available();
        if (avail > bufferSize) {
            bufferSize = avail;
        }
        Reader reader = charset == null ? new InputStreamReader(input, UTF8_CHARSET)
                        : new InputStreamReader(input, charset);
        return toString(reader, bufferSize);
    }

    public static String toString(final Reader input) throws IOException {
        return toString(input, DEFAULT_BUFFER_SIZE);
    }

    public static String toString(final Reader input, int bufSize) throws IOException {

        StringBuilder buf = new StringBuilder();
        final char[] buffer = new char[bufSize];
        int n = 0;
        n = input.read(buffer);
        while (-1 != n) {
            if (n == 0) {
                throw new IOException("0 bytes read in violation of InputStream.read(byte[])");
            }
            buf.append(new String(buffer, 0, n));
            n = input.read(buffer);
        }
        input.close();
        return buf.toString();
    }

    public static String readStringFromStream(InputStream in)
                    throws IOException {

        StringBuilder sb = new StringBuilder(1024);

        for (int i = in.read(); i != -1; i = in.read()) {
            sb.append((char) i);
        }

        in.close();

        return sb.toString();
    }

    public static void consume(InputStream in) throws IOException {
        int i = in.available();
        if (i == 0) {
            //if i is 0, then we MAY have already hit the end of the stream
            //so try a read and return rather than allocate a buffer and such
            int i2 = in.read();
            if (i2 == -1) {
                return;
            }
            //reading the byte may have caused a buffer to fill
            i = in.available();
        }
        if (i < DEFAULT_BUFFER_SIZE) {
            i = DEFAULT_BUFFER_SIZE;
        }
        if (i > 65536) {
            i = 65536;
        }
        byte bytes[] = new byte[i];
        while (in.read(bytes) != -1) {
            //nothing - just discarding
        }
    }

    /**
     * Consumes at least the given number of bytes from the input stream
     *
     * @param input
     * @param atLeast
     * @throws IOException
     */
    public static void consume(final InputStream input,
                               int atLeast) throws IOException {
        final byte[] buffer = new byte[4096];
        int n = atLeast > buffer.length ? buffer.length : atLeast;
        n = input.read(buffer, 0, n);
        while (-1 != n) {
            if (n == 0) {
                throw new IOException("0 bytes read in violation of InputStream.read(byte[])");
            }
            atLeast -= n;
            if (atLeast <= 0) {
                return;
            }
            n = atLeast > buffer.length ? buffer.length : atLeast;
            n = input.read(buffer, 0, n);
        }
    }
}
