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
package componenttest.topology.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * A reader similar to InputStreamReader but that does not buffer (only consumes
 * bytes from the underlying input stream as needed).
 */
class UnbufferedInputStreamReader extends Reader {
    /**
     * The input stream to read.
     */
    private final InputStream in;

    /**
     * The decoder used to transform bytes to chars.
     */
    private final CharsetDecoder decoder;

    /**
     * The input buffer to pass to the decoder. Because we only read one byte
     * at a time, this only needs to be one byte.
     */
    private final ByteBuffer inb = ByteBuffer.allocate(1);

    /**
     * The output buffer to pass to the decoder. The {@link #read(char[], int, int)} implementation
     * currently depends on this being of size 1, though in theory it could be
     * larger, which might improve performance for encodings that translate a
     * single input byte into multiple output chars (should be rare anyway).
     */
    private final CharBuffer outb = CharBuffer.allocate(1);

    /**
     * True if end the end of the input stream has been reached.
     */
    private boolean eof;

    UnbufferedInputStreamReader(InputStream in, Charset charset) {
        this.in = in;
        this.decoder = charset.newDecoder();
        inb.position(inb.limit());
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }

        for (;;) {
            CoderResult result = eof ? decoder.flush(outb) : decoder.decode(inb, outb, false);

            if (outb.position() == outb.limit()) {
                outb.position(0);
                cbuf[off] = outb.get();
                outb.position(0);
                return 1;
            }

            if (eof && result == CoderResult.UNDERFLOW) {
                return -1;
            }

            int b = in.read();
            if (b == -1) {
                decoder.decode(inb, outb, true);
                eof = true;
            } else {
                inb.position(0);
                inb.put((byte) b);
                inb.position(0);
            }
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    // Unit test.
    public static void main(String[] args) throws Exception {
        Reader r = new UnbufferedInputStreamReader(new ByteArrayInputStream("abcd".getBytes()), Charset.forName("UTF-8"));
        assertEquals((int) 'a', r.read());
        assertEquals((int) 'b', r.read());
        assertEquals((int) 'c', r.read());
        assertEquals((int) 'd', r.read());
        assertEquals(-1, r.read());
        System.out.println("PASS");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!(expected == null ? actual == null : expected.equals(actual))) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
    }
}
