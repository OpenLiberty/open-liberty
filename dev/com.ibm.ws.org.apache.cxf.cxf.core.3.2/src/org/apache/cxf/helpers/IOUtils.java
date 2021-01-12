/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.cxf.io.CopyingOutputStream;
import org.apache.cxf.io.Transferable;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial // Liberty change: line added
public final class IOUtils {
    public static final Charset UTF8_CHARSET = java.nio.charset.StandardCharsets.UTF_8;
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private IOUtils() {

    }

    public static boolean isEmpty(InputStream is) throws IOException {
        if (is == null) {
            return true;
        }
        // if available is 0 it does not mean it is empty
        if (is.available() > 0) {
            return false;
        }

        final byte[] bytes = new byte[1];
        if (is.markSupported()) {
            is.mark(1);
            try {
                return isEof(is.read(bytes));
            } finally {
                is.reset();
            }
        }

        if (!(is instanceof PushbackInputStream)) {
            return false;
        }

        // it may be an attachment stream
        PushbackInputStream pbStream = (PushbackInputStream)is;
        boolean isEmpty = isEof(pbStream.read(bytes));
        if (!isEmpty) {
            pbStream.unread(bytes);
        }

        return isEmpty;
    }
    private static boolean isEof(int result) {
        return result == -1;
    }
    /**
     * Use this function instead of new String(byte[], String) to avoid surprises from
     * non-standard default encodings.
     * @param bytes
     * @param charsetName
     */
    public static String newStringFromBytes(byte[] bytes, String charsetName) {
        try {
            return new String(bytes, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw
                new RuntimeException("Impossible failure: Charset.forName(\""
                                     + charsetName + "\") returns invalid name.");

        }
    }


    /**
     * Use this function instead of new String(byte[]) to avoid surprises from non-standard default encodings.
     * @param bytes
     */
    public static String newStringFromBytes(byte[] bytes) {
        return newStringFromBytes(bytes, UTF8_CHARSET.name());
    }

    /**
     * Use this function instead of new String(byte[], int, int, String)
     * to avoid surprises from non-standard default encodings.
     * @param bytes
     * @param charsetName
     * @param start
     * @param length
     */
    public static String newStringFromBytes(byte[] bytes, String charsetName, int start, int length) {
        try {
            return new String(bytes, start, length, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw
                new RuntimeException("Impossible failure: Charset.forName(\""
                                     + charsetName + "\") returns invalid name.");

        }
    }

    /**
     * Use this function instead of new String(byte[], int, int)
     * to avoid surprises from non-standard default encodings.
     * @param bytes
     * @param start
     * @param length
     */
    public static String newStringFromBytes(byte[] bytes, int start, int length) {
        return newStringFromBytes(bytes, UTF8_CHARSET.name(), start, length);
    }

    public static int copy(final InputStream input, final OutputStream output)
        throws IOException {
        if (input == null) {
            return 0;
        }
        if (output instanceof CopyingOutputStream) {
            return ((CopyingOutputStream)output).copyFrom(input);
        }
        return copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static int copyAndCloseInput(final InputStream input,
            final OutputStream output) throws IOException {
        try (InputStream in = input) {
            return copy(in, output);
        }
    }

    public static int copyAndCloseInput(final InputStream input,
            final OutputStream output, int bufferSize) throws IOException {
        try (InputStream in = input) {
            return copy(in, output, bufferSize);
        }
    }

    public static void copyAndCloseInput(final Reader input,
                                        final Writer output) throws IOException {
        try (Reader r = input) {
            copy(r, output, DEFAULT_BUFFER_SIZE);
        }
    }

    public static void copyAndCloseInput(final Reader input,
            final Writer output, int bufferSize) throws IOException {
        try (Reader r = input) {
            copy(r, output, bufferSize);
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
        int n = input.read(buffer);
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

    public static void copyAtLeast(final Reader input,
                                   final Writer output,
                                   int atLeast) throws IOException {
        final char[] buffer = new char[4096];
        int n = atLeast > buffer.length ? buffer.length : atLeast;
        n = input.read(buffer, 0, n);
        while (-1 != n) {
            if (n == 0) {
                throw new IOException("0 bytes read in violation of Reader.read(char[])");
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
        int n = input.read(buffer);
        while (-1 != n) {
            output.write(buffer, 0, n);
            n = input.read(buffer);
        }
    }

    public static void transferTo(InputStream inputStream, File destinationFile) throws IOException {
        if (Transferable.class.isAssignableFrom(inputStream.getClass())) {
            ((Transferable)inputStream).transferTo(destinationFile);
        } else {
            try (OutputStream out = Files.newOutputStream(destinationFile.toPath())) {
                copyAndCloseInput(inputStream, out);
            }
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
        try (Reader r = input) {
            int n = r.read(buffer);
            while (-1 != n) {
                if (n == 0) {
                    throw new IOException("0 bytes read in violation of InputStream.read(byte[])");
                }
                buf.append(buffer, 0, n);
                n = r.read(buffer);
            }
            return buf.toString();
        }
    }

    public static String readStringFromStream(InputStream in)
        throws IOException {
        return toString(in);
    }

    /**
     * Load the InputStream into memory and return a ByteArrayInputStream that
     * represents it. Closes the in stream.
     *
     * @param in
     * @throws IOException
     */
    public static ByteArrayInputStream loadIntoBAIS(InputStream in)
        throws IOException {
        int i = in.available();
        if (i < DEFAULT_BUFFER_SIZE) {
            i = DEFAULT_BUFFER_SIZE;
        }
        LoadingByteArrayOutputStream bout = new LoadingByteArrayOutputStream(i);
        copy(in, bout);
        in.close();
        return bout.createInputStream();
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
        byte[] bytes = new byte[i];
        while (in.read(bytes) != -1) {
            //nothing - just discarding
        }
    }

    /**
     * Consumes at least the given number of bytes from the input stream
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

    public static byte[] readBytesFromStream(InputStream in) throws IOException {
        int i = in.available();
        if (i < DEFAULT_BUFFER_SIZE) {
            i = DEFAULT_BUFFER_SIZE;
        }
        try (InputStream input = in; ByteArrayOutputStream bos = new ByteArrayOutputStream(i)) {
            copy(input, bos);
            return bos.toByteArray();
        }
    }

}
