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
package com.ibm.websphere.servlet.response;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

import com.ibm.ejs.ras.TraceNLS;

/**
 * @ibm-api
 * Adapter class creates a ServletOutputStream from a java.io.OutputStream.
 * This class will proxy all method calls to the underlying stream.
 */
public class ServletOutputStreamAdapter extends ServletOutputStream{
    private OutputStream _out;
    protected static final TraceNLS nls = TraceNLS.getTraceNLS(ServletOutputStreamAdapter.class, "com.ibm.ws.webcontainer.resources.Messages");

    /**
     * Creates a ServletOutputStream from an OutputStream.
     */
    public ServletOutputStreamAdapter(OutputStream out){
        _out = out;
    }

    public void write(int b) throws IOException{
        _out.write(b);
    }

    public void close() throws IOException{
        _out.close();
    }

    public void write(byte b[]) throws IOException{
        _out.write(b);
    }

    public void write(byte b[], int off, int len) throws IOException{
        _out.write(b, off, len);
    }

    public void flush() throws IOException{
        _out.flush();
    }

}
