/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import com.ibm.wsspi.webcontainer.util.BufferedServletOutputStream;
/**
 * This class adds Servlet 3.1 methods to its 3.0-specific counterpart that
 * implements a buffered output stream for writing servlet response data.
 */
public class BufferedServletOutputStream31 extends BufferedServletOutputStream
{

//    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer31.util");
//    private static TraceNLS nls = TraceNLS.getTraceNLS(BufferedServletOutputStream31.class, "com.ibm.ws.webcontainer.resources.Messages");

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
