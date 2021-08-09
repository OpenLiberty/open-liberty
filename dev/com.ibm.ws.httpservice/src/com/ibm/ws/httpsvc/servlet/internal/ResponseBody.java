/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.httpsvc.servlet.internal;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.wsspi.http.HttpOutputStream;

/**
 * HttpService implementation of a servlet output stream wrapper of the HTTP
 * dispatcher OutputStream.
 */
public class ResponseBody extends ServletOutputStream {
    private static final byte[] CRLF = { '\r', '\n' };

    private HttpOutputStream output = null;

    /**
     * Constructor wrapping the provided stream.
     * 
     * @param stream
     */
    public ResponseBody(HttpOutputStream stream) {
        this.output = stream;
    }

    /**
     * Query the current size of the data buffering.
     * 
     * @return int
     */
    public int getBufferSize() {
        return this.output.getBufferSize();
    }

    /**
     * Set the size of data to buffer prior to an automatic flush.
     * 
     * @param size
     */
    public void setBufferSize(int size) {
        this.output.setBufferSize(size);
    }

    /**
     * Clear any current buffered content.
     */
    public void clear() {
        this.output.clear();
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(boolean)
     */
    @Override
    public void print(boolean b) throws IOException {
        print(Boolean.toString(b));
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(char)
     */
    @Override
    public void print(char c) throws IOException {
        print(String.valueOf(c));
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(double)
     */
    @Override
    public void print(double d) throws IOException {
        print(Double.toString(d));
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(float)
     */
    @Override
    public void print(float f) throws IOException {
        print(Float.toString(f));
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(int)
     */
    @Override
    public void print(int i) throws IOException {
        print(Integer.toString(i));
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(long)
     */
    @Override
    public void print(long l) throws IOException {
        print(Long.toString(l));
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(java.lang.String)
     */
    @Override
    public void print(String s) throws IOException {
        //TODO this is wrong for encoding
        byte[] data = (null == s) ? "null".getBytes() : s.getBytes();
        this.output.write(data, 0, data.length);
    }

    /*
     * @see javax.servlet.ServletOutputStream#println()
     */
    @Override
    public void println() throws IOException {
        this.output.write(CRLF, 0, 2);
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(boolean)
     */
    @Override
    public void println(boolean b) throws IOException {
        println(Boolean.toString(b));
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(char)
     */
    @Override
    public void println(char c) throws IOException {
        println(String.valueOf(c));
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(double)
     */
    @Override
    public void println(double d) throws IOException {
        println(Double.toString(d));
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(float)
     */
    @Override
    public void println(float f) throws IOException {
        println(Float.toString(f));
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(int)
     */
    @Override
    public void println(int i) throws IOException {
        println(Integer.toString(i));
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(long)
     */
    @Override
    public void println(long l) throws IOException {
        println(Long.toString(l));
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(java.lang.String)
     */
    @Override
    public void println(String s) throws IOException {
        print(s);
        this.output.write(CRLF, 0, 2);
    }

    /*
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException {
        this.output.close();
    }

    /**
     * Check whether this stream is closed or not.
     * 
     * @return boolean
     */
    public boolean isClosed() {
        return this.output.isClosed();
    }

    /*
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        this.output.flush();
    }

    /*
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(@Sensitive byte[] b, int off, int len) throws IOException {
        this.output.write(b, off, len);
    }

    /*
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(@Sensitive byte[] b) throws IOException {
        // note: an NPE is appropriate here if input was null
        this.output.write(b, 0, b.length);
    }

    /*
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException {
        this.output.write(b);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletOutputStream#isReady()
     */
    @Override
    public boolean isReady() {
        // TODO Servlet3.1 updates
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletOutputStream#setWriteListener(javax.servlet.WriteListener)
     */
    @Override
    public void setWriteListener(WriteListener writeListener) {
        // TODO Servlet3.1 updates        
    }

}
