/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.request;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

import com.ibm.ejs.ras.TraceNLS;

/**
 * 
 * Adapter class creates a ServletInputStream from a java.io.InputStream.
 * This class will proxy all method calls to the underlying stream.
 *
 * @ibm-api 
 */
public class ServletInputStreamAdapter extends ServletInputStream{
    private InputStream _in;
    protected static final TraceNLS nls = TraceNLS.getTraceNLS(ServletInputStreamAdapter.class, "com.ibm.ws.webcontainer.resources.Messages");

    /**
     * Creates a ServletInputStream from an InputStream.
     */
    public ServletInputStreamAdapter(InputStream in){
        _in = in;
    }
    public int read() throws IOException{
        return _in.read();
    }
    public int available() throws IOException{
        return _in.available();
    }
    public int read(byte b[]) throws IOException{
        return _in.read(b);
    }
    public int read(byte b[], int off, int len) throws IOException{
        return _in.read(b, off, len);
    }
    public synchronized void mark(int readlimit){
        _in.mark(readlimit);
    }
    public long skip(long n) throws IOException{
        return _in.skip(n);
    }
    public boolean markSupported() {
        return _in.markSupported();
    }
    public void close() throws IOException{
        _in.close();
    }
    public synchronized void reset() throws IOException{
        _in.reset();
    }
}
