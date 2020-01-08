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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class CacheProxyOutputStream extends ServletOutputStream {

    private static TraceComponent tc = Tr.register(CacheProxyOutputStream.class,
                                                   "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    private OutputStream outputStream = null;
    private boolean closed = false;
    private final ByteArrayOutputStream cachingStream = new ByteArrayOutputStream();
    private boolean caching = false;
    boolean flushrequired = false;
    boolean outputStreamFlushed = false;
    boolean delayWrite = false;
    boolean postDelayCachingValue = false;

    public CacheProxyOutputStream(FragmentComposer fc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CacheProxyOutputStream(FragmentComposer fc)");
        }
    }

    public CacheProxyOutputStream(OutputStream outputStream, FragmentComposer fc) {
        this.outputStream = outputStream;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CacheProxyOutputStream(OutputStream outputStream, FragmentComposer fc)" + outputStream);
        }
    }

    public CacheProxyOutputStream() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CacheProxyOutputStream()");
        }
    }

    public CacheProxyOutputStream(OutputStream outputStream) {

        this.outputStream = outputStream;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CacheProxyOutputStream(OutputStream outputStream)" + outputStream);
        }
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setOutputStream this=" + this + " " + outputStream);
        }
        this.outputStream = outputStream;
        closed = false;
        flushrequired = false;
    }

    /**
     * This method resets the writer for future use
     */
    public void reset() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "reset this=" + this);
        }
        cachingStream.reset();
        caching = false;
        outputStream = null;
        closed = false;
        flushrequired = false;
        outputStreamFlushed = false;
        delayWrite = false;
        postDelayCachingValue = false;
    }

    /**
     * This method resets the cachedwriter's buffer only
     */
    public void resetBuffer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "resetBuffer this=" + this);
        }
        cachingStream.reset();
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
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isCaching: this=" + this + " caching=" + caching);
        return caching;
    }

    public void setDelayWrite(boolean flag, boolean pdcv) {
        delayWrite = flag;
        postDelayCachingValue = pdcv;
    }

    /**
     * This method retrieves the cached data
     */
    public byte[] getCachedData() {
        return cachingStream.toByteArray();
    }

    @Override
    public void write(int character) throws IOException {
        flushrequired = true;

        if (caching) {
            cachingStream.write(character);
        }
        if (TraceComponent.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " write(int): " + this + " " + "outputStream=" + outputStream + " " + String.valueOf(character));
        outputStream.write(character);
    }

    @Override
    public void write(byte[] array) throws IOException {
        flushrequired = true;

        if (caching) {
            cachingStream.write(array);
        }

        if (TraceComponent.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " write(byte[]): " + " length=" + array.length + " " + this + " " + "outputStream=" + outputStream + " " + new String(array));
        outputStream.write(array);
    }

    @Override
    public void write(byte[] array, int offset, int length) throws IOException {
        flushrequired = true;

        if (caching) {
            cachingStream.write(array, offset, length);
        }

        if (TraceComponent.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " write(byte[],int,int): " + " length=" + length + " " + this + " " + "outputStream=" + outputStream + " " + new String(array, offset, length));
        outputStream.write(array, offset, length);
    }

    @Override
    public void flush() throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "flush this=" + this + " caching=" + caching + " flushrequired=" + flushrequired);

        outputStreamFlushed = true;

        if (flushrequired) {

            if (outputStream != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "flush outputstream: " + this.outputStream + " content: " + outputStream.toString());
                outputStream.flush();
            }

            flushrequired = false;

            if (delayWrite && (!caching)) {
                caching = postDelayCachingValue;
                delayWrite = false;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "close() this= " + this + " outputStream=" + outputStream + " ");
        }
        closed = true;
        outputStream.close();
    }

    public boolean isClosed() {
        return closed;
    }

    public void initListener(FragmentComposer fc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "initListener with " + fc);
        }
    }
}
