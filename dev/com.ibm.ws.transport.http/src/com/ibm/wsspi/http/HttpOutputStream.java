/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

/**
 *
 */
public abstract class HttpOutputStream extends OutputStream {

    public abstract void setIsClosing(boolean b);

    /**
     * Query the amount of data this stream is configured to buffer before an
     * automatic write happens.
     * 
     * @return int
     */
    public abstract int getBufferSize();

    /**
     * Set the amount of data to buffer internally before the stream itself
     * initiates a flush. A zero size means no buffer is done, each write
     * call will flush data.
     * 
     * @param size
     * @throws IllegalStateException if already writing data or closed
     */
    public abstract void setBufferSize(int size);

    /**
     * Clear any current buffer content in the stream.
     */
    public abstract void clear();

    /**
     * Query the amount of bytes written so far.
     * 
     * @return long
     */
    public abstract long getBytesWritten();

    /**
     * Query the amount of bytes currently buffered so far.
     * 
     * @return long
     */
    public abstract long getBufferedCount();

    /**
     * Test whether this stream has any current data buffered, waiting to
     * be written out.
     * 
     * @return boolean
     */
    public abstract boolean hasBufferedContent();

    /**
     * Write a file channel onto the output stream.
     * 
     * @param fc
     * @throws IOException
     */
    public abstract void writeFile(FileChannel fc) throws IOException;

    /**
     * Write the current set of response headers. If the headers have already
     * been sent, this is a no-op.
     * 
     * @throws IOException
     */
    public abstract void flushHeaders() throws IOException;

    /**
     * Flush the output array of buffers to the network below.
     * 
     * @throws IOException
     */
    public abstract void flushBuffers() throws IOException;

    /**
     * Query whether this stream is closed already or not.
     * 
     * @return boolean
     */
    public abstract boolean isClosed();

    /**
     * @param ignoreFlag
     * @throws IOException
     */
    public abstract void flush(boolean ignoreFlag) throws IOException;

    /**
     * @param length
     */
    public abstract void setContentLength(long length);

}