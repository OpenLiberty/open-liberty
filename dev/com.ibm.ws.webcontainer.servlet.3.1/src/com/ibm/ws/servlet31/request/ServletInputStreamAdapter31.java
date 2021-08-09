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
package com.ibm.ws.servlet31.request;
import java.io.InputStream;

import javax.servlet.ReadListener;

import com.ibm.websphere.servlet.request.ServletInputStreamAdapter;

/**
 * 
 * Adapter class creates a ServletInputStream from a java.io.InputStream.
 * This class will proxy all method calls to the underlying stream.
 *
 * @ibm-api 
 */
public class ServletInputStreamAdapter31 extends ServletInputStreamAdapter {
    //protected static final TraceNLS nls = TraceNLS.getTraceNLS(ServletInputStreamAdapter31.class, "com.ibm.ws.webcontainer.resources.Messages");

    /**
     * Creates a ServletInputStream from an InputStream.
     */
    public ServletInputStreamAdapter31(InputStream in){
        super(in);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#isFinished()
     */
    @Override
    public boolean isFinished() {
        return false;
    }
    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#isReady()
     */
    @Override
    public boolean isReady() {
        return false;
    }
    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#setReadListener(javax.servlet.ReadListener)
     */
    @Override
    public void setReadListener(ReadListener arg0) {
        return;        
    }
}
