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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

class LineReader {
    private final Reader reader;
    private final StringBuilder builder = new StringBuilder();
    private boolean cr;
    private boolean eof;

    LineReader(Reader reader) {
        this.reader = reader;
    }

    /**
     * Returns the next line from the reader, or null if there are no more
     * lines. {@link #eof} will return true after the last line is returned.
     */
    public String readLine() throws IOException {
        if (eof) {
            return null;
        }

        for (;;) {
            int c = reader.read();
            if (c == -1) {
                eof = true;
                String line = builder.toString();
                builder.setLength(0);
                return line;
            }

            if (c == '\r' || (c == '\n' && !cr)) {
                cr = c == '\r';
                String line = builder.toString();
                builder.setLength(0);
                return line;
            }

            cr = false;
            if (c != '\n') {
                builder.append((char) c);
            }
        }
    }

    /**
     * Returns true if the last line was truncated at EOF.
     */
    public boolean eof() {
        return eof;
    }

    public void close() throws IOException {
        reader.close();
    }

    // Unit test.
    public static void main(String[] args) throws Exception {
        {
            LineReader r = new LineReader(new StringReader(""));
            assertEquals("", r.readLine());
            assertEquals(true, r.eof());
            assertEquals(null, r.readLine());
        }
        {
            LineReader r = new LineReader(new StringReader("a"));
            assertEquals("a", r.readLine());
            assertEquals(true, r.eof());
            assertEquals(null, r.readLine());
        }
        {
            LineReader r = new LineReader(new StringReader("a\r"));
            assertEquals("a", r.readLine());
            assertEquals(false, r.eof());
            assertEquals("", r.readLine());
            assertEquals(true, r.eof());
            assertEquals(null, r.readLine());
        }
        {
            LineReader r = new LineReader(new StringReader("a\rb"));
            assertEquals("a", r.readLine());
            assertEquals(false, r.eof());
            assertEquals("b", r.readLine());
            assertEquals(true, r.eof());
            assertEquals(null, r.readLine());
        }
        {
            LineReader r = new LineReader(new StringReader("a\n"));
            assertEquals("a", r.readLine());
            assertEquals(false, r.eof());
            assertEquals("", r.readLine());
            assertEquals(true, r.eof());
            assertEquals(null, r.readLine());
        }
        {
            LineReader r = new LineReader(new StringReader("a\nb"));
            assertEquals("a", r.readLine());
            assertEquals(false, r.eof());
            assertEquals("b", r.readLine());
            assertEquals(true, r.eof());
            assertEquals(null, r.readLine());
        }
        {
            LineReader r = new LineReader(new StringReader("a\r\n"));
            assertEquals("a", r.readLine());
            assertEquals(false, r.eof());
            assertEquals("", r.readLine());
            assertEquals(true, r.eof());
            assertEquals(null, r.readLine());
        }
        {
            LineReader r = new LineReader(new StringReader("a\r\nb"));
            assertEquals("a", r.readLine());
            assertEquals(false, r.eof());
            assertEquals("b", r.readLine());
            assertEquals(true, r.eof());
            assertEquals(null, r.readLine());
        }
        System.out.println("PASS");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!(expected == null ? actual == null : expected.equals(actual))) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
    }
}
