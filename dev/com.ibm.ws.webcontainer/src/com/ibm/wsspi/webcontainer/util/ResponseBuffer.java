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
package com.ibm.wsspi.webcontainer.util;
import java.io.IOException;

/** 
 * Interface implemented by classes that are capable of buffering
 * data written to a ServletResponse.
 *
 * This interface is implemented by both BufferedServletOutputStream and
 * BufferedWriter.  The ServletResponse object keeps one reference to a
 * response buffer and proxies all of its buffering related methods off
 * to its response buffer (either the BufferedWriter or BufferedServletOutputStream).
 */
public interface ResponseBuffer{
    /**
     * Clear all data written to the response.
     * This method can only be called if the response is uncommitted.
     */
    public void clearBuffer();

    /**
     * Query the response to determine if any response data has been
     * written to the client.  A response is considered committed once
     * any data from the response has been sent to the client.
     */
    public boolean isCommitted();

    /**
     * Flush any buffered data that has been written to the response.
     * After this method is called, the response will be considered
     * committed (if it wasn't already).
     */
    public void flushBuffer() throws IOException;

    /**
     * Set the size of the output buffer for this response.
     */
    public void setBufferSize(int size);

    /**
     * Get the size of the output buffer for this response.
     */
    public int getBufferSize();
}
