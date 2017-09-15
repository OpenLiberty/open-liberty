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

package com.ibm.ws.webcontainer31.srt.http;

import javax.servlet.ReadListener;

/**
 * This class implements a buffered input stream for reading servlet request
 * data. It also keeps track of the number of bytes that have been read, and
 * allows the specification of an optional byte limit to ensure that the
 * content length has not been exceeded.
 *
 * @version	1.13, 10/13/97
 */
public class HttpInputStream31 extends com.ibm.ws.webcontainer.srt.http.HttpInputStream
{

    //private static TraceNLS nls = TraceNLS.getTraceNLS(HttpInputStream31.class, "com.ibm.ws.webcontainer.resources.Messages");

    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#setReadListener(javax.servlet.ReadListener)
     */
    @Override
    public void setReadListener(ReadListener arg0) {
        return;      
    }
}
