/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class is a proxy for the WebSphere Writer object.
 * It has features added to enable caching.
 */
public class CacheProxyWriter extends PrintWriter {

    private static TraceComponent tc = Tr.register(CacheProxyWriter.class,
                                                   "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    boolean trouble = false;
    Object lock = new Object();
    CharArrayWriter cachingWriter = new CharArrayWriter();
    boolean caching = false;
    boolean flushrequired = false;
    boolean writerFlushed = false;
    boolean delayWrite = false;
    boolean postDelayCachingValue = false;

    /**
     * Constructor with parameters.
     * 
     * @param writer The writer that is to be proxied.
     */
    public CacheProxyWriter() {
        this(new NullWriter());

    }

    public CacheProxyWriter(Writer writer) {
        super(writer);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CTOR " + this.toString());
        }
    }

    public CacheProxyWriter(FragmentComposer fc) {

        this(new NullWriter(), fc);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CacheProxyWriter()");
        }

    }

    public CacheProxyWriter(Writer writer, FragmentComposer fc) {
        super(writer);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, " CacheProxyWriter(Writer writer)");
        }

    }

    /**
     * This returns the writer variable.
     * 
     * @return The writer that is proxied.
     */
    public Writer getWriter() {
        return out;
    }

    /**
     * This sets the writer variable.
     * 
     * @param writer The writer that is proxied.
     */
    public void setWriter(Writer writer) {
        this.out = writer;
    }

    /**
     * This method resets the writer for future use
     */
    public void reset() {

        if (tc.isEntryEnabled())
            Tr.debug(tc, "RESET=" + this);

        cachingWriter.reset();
        caching = false;
        out = null;
        flushrequired = false;
        writerFlushed = false; //seneca
        delayWrite = false; //jstl fix
        postDelayCachingValue = false;
    }

    /**
     * This method resets the cachedwriter's buffer only
     */
    public void resetBuffer() {
        if (tc.isEntryEnabled())
            Tr.debug(tc, "resetBuffer=" + this);

        cachingWriter.reset();
    }

    /**
     * This method enables/disables side-band caching of the stream
     */
    public void setCaching(boolean caching) {
        this.caching = caching;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setCaching: this=" + this + " caching=" + caching);
    }

    public boolean isCaching() {
        return caching;
    }

    /**
     * This method retrieves the cached data
     * @ibm-private-in-use
     */
    public char[] getCachedData() {
        char[] toChar = cachingWriter.toCharArray();
        if (toChar.length > 0) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, this + ": getCachedData " + toChar.length + " " + toChar);
        }
        return toChar;
    }

    public void setDelayWrite(boolean flag, boolean pdcv) //jstl fix begin
    {
        delayWrite = flag;
        postDelayCachingValue = pdcv;
    }

    /** Check to make sure that the stream has not been closed */
    private void ensureOpen() throws IOException {
        if (out == null)
            throw new IOException("Stream closed");
    }

    /** Flush the stream. */
    @Override
    public void flush() {
        writerFlushed = true; //seneca

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "flush this=" + this + " caching=" + caching + " flushrequired=" + flushrequired + " delayWrite=" + delayWrite);
            Tr.debug(tc, "Flusing " + out);
        }

        if (flushrequired) {
            try {
                synchronized (lock) {
                    ensureOpen();
                    out.flush();
                }
            } catch (IOException x) {
                trouble = true;
            }
            flushrequired = false;
        }

        if (delayWrite & (!caching)) {
            caching = true;
            delayWrite = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "flush", "caching=" + caching);
    }

    /** Close the stream. */
    @Override
    public void close() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "close", this);
        flush();
        try {
            out.close();
        } catch (Exception ex) {
            trouble = true;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "close", this);
    }

    /**
     * Flush the stream and check its error state. Errors are cumulative;
     * once the stream encounters an error, this routine will return true on
     * all successive calls.
     * 
     * @return True if the print stream has encountered an error, either on the
     *         underlying output stream or during a format conversion.
     */
    @Override
    public boolean checkError() {
        if (out != null)
            flush();
        return trouble;
    }

    /** Indicate that an error has occurred. */
    @Override
    protected void setError() {
        trouble = true;
    }

    /*
     * Exception-catching, synchronized output operations,
     * which also implement the write() methods of Writer
     */

    /** Write a single character. */
    @Override
    public void write(int c) {
        flushrequired = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, String.valueOf(c));
        try {
            synchronized (lock) {
                ensureOpen();

                if (caching) {
                    cachingWriter.write(c);
                }

                out.write(c);
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            trouble = true;
        }
    }

    /** Write a portion of an array of characters. */
    @Override
    public void write(char buf[], int off, int len) {
        flushrequired = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, String.valueOf(buf, off, len));
        try {
            synchronized (lock) {
                ensureOpen();

                if (caching) {
                    cachingWriter.write(buf, off, len);
                }

                out.write(buf, off, len);
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            trouble = true;
        }
    }

    /**
     * Write an array of characters. This method cannot be inherited from the
     * Writer class because it must suppress I/O exceptions.
     */
    @Override
    public void write(char buf[]) {
        write(buf, 0, buf.length);
    }

    /** Write a portion of a string. */
    @Override
    public void write(String s, int off, int len) {
        flushrequired = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, s.substring(off, off + len));
        try {
            synchronized (lock) {
                ensureOpen();

                if (caching) {
                    cachingWriter.write(s, off, off + len);
                }

                out.write(s, off, len);
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            trouble = true;
        }
    }

    /**
     * Write a string. This method cannot be inherited from the Writer class
     * because it must suppress I/O exceptions.
     */
    @Override
    public void write(String s) {
        write(s, 0, s.length());
    }

    @Override
    public void println() {
        newLine();
    }

    private void newLine() {
        try {
            synchronized (lock) {
                ensureOpen();

                if (caching) {
                    cachingWriter.write(lineSeparator);
                }

                out.write(lineSeparator);
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            trouble = true;
        }
    }

    public static final String lineSeparator = getLineSeparator();

    private static final String getLineSeparator() {
        Object obj = AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return System.getProperty("line.separator");
            }
        });

        return (String) obj;
    }

    static class NullWriter extends Writer {
        public NullWriter() {}

        @Override
        public void close() {}

        @Override
        public void flush() {}

        @Override
        public void write(char[] c, int i, int j) {}
    }

}
