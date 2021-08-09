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

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.wsspi.http.HttpInputStream;

/**
 * HttpService implementation of a servlet input stream, wrapping the HTTP dispatcher
 * provided input stream object.
 */
public class RequestBody extends ServletInputStream {
    private HttpInputStream input = null;

    /**
     * Constructor wrapping a providing input stream.
     * 
     * @param stream
     */
    public RequestBody(HttpInputStream stream) {
        this.input = stream;
    }

    /*
     * @see javax.servlet.ServletInputStream#readLine(byte[], int, int)
     */
    @Override
    public int readLine(@Sensitive byte[] b, int off, int len) throws IOException {
        return super.readLine(b, off, len);
    }

    /*
     * @see java.io.InputStream#available()
     */
    @Override
    public int available() throws IOException {
        return this.input.available();
    }

    /*
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        this.input.close();
    }

    /*
     * @see java.io.InputStream#mark(int)
     */
    @Override
    public synchronized void mark(int readlimit) {
        this.input.mark(readlimit);
    }

    /*
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return this.input.markSupported();
    }

    /*
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        return this.input.read();
    }

    /*
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(@Sensitive byte[] b, int off, int len) throws IOException {
        return this.input.read(b, off, len);
    }

    /*
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read(@Sensitive byte[] b) throws IOException {
        return this.input.read(b);
    }

    /*
     * @see java.io.InputStream#reset()
     */
    @Override
    public synchronized void reset() throws IOException {
        this.input.reset();
    }

    /*
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(long n) throws IOException {
        return this.input.skip(n);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletInputStream#isFinished()
     */
    @Override
    public boolean isFinished() {
        // TODO Servlet3.1 updates
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletInputStream#isReady()
     */
    @Override
    public boolean isReady() {
        // TODO Servlet3.1 updates
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletInputStream#setReadListener(javax.servlet.ReadListener)
     */
    @Override
    public void setReadListener(ReadListener readListener) {
        // TODO Servlet3.1 updates        
    }

}
