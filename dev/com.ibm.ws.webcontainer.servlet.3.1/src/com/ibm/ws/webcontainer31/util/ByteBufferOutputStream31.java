/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.util;

import javax.servlet.WriteListener;

import com.ibm.wsspi.webcontainer.util.ByteBufferOutputStream;

public class ByteBufferOutputStream31 extends ByteBufferOutputStream {

    //private static TraceNLS nls = TraceNLS.getTraceNLS(ByteBufferOutputStream31.class, "com.ibm.ws.webcontainer31.resources.Messages");

    public ByteBufferOutputStream31() {
        super();
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
