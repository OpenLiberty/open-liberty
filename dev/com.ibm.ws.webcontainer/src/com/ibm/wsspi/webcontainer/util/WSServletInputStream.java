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
import java.io.InputStream;

import javax.servlet.ServletInputStream;


public abstract class WSServletInputStream extends ServletInputStream {
   
    /**
     * Sets the content length for this input stream. This should be called
     * once the headers have been read from the input stream.
     * @param len the content length
     */
    public abstract void setContentLength( long contentLength);
    
    /**
     * Initializes the servlet input stream with the specified raw input stream.
     * @param in the raw input stream
     */
    public abstract void init (InputStream in) throws IOException;
    
    /**
     * Finishes reading the request without closing the underlying stream.
     * @exception IOException if an I/O error has occurred
     */
    public abstract void finish () throws IOException;
    
    /**
     * Sets an observer for this output stream. The observer will be
     * notified when the stream is first written to.
     * @param obs the IOutpuStreamObserver associated with this response
     */
    public void setObserver(IInputStreamObserver obs) {};
    
    /**
     * called to enable the data to be re-read from the beginning
     */
    public void restart() {};

}
