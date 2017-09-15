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
package com.ibm.websphere.servlet31.response;
import java.io.OutputStream;

import javax.servlet.WriteListener;

import com.ibm.websphere.servlet.response.ServletOutputStreamAdapter;

/**
 * @ibm-api
 * Adapter class creates a ServletOutputStream from a java.io.OutputStream.
 * This class will proxy all method calls to the underlying stream.
 */
public class ServletOutputStreamAdapter31 extends ServletOutputStreamAdapter {

    //protected static final TraceNLS nls = TraceNLS.getTraceNLS(ServletOutputStreamAdapter31.class, "com.ibm.ws.webcontainer.resources.Messages");

    /**
     * Creates a ServletOutputStream31 from an OutputStream.
     */
    public ServletOutputStreamAdapter31(OutputStream out){
        super(out);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletOutputStream#isReady()
     */
    @Override
    public boolean isReady() {
        return false;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletOutputStream#setWriteListener(javax.servlet.WriteListener)
     */
    @Override
    public void setWriteListener(WriteListener arg0) {
        return;        
    }
}
