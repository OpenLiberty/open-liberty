/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.compression;

import java.util.List;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;

/**
 * Handler interface that allows different compression instances to apply to
 * outbound HTTP messages.
 * 
 */
public interface CompressionHandler {

    /**
     * Query the proper Content-Encoding value for this particular handler.
     * 
     * @return ContentEncodingValues
     */
    ContentEncodingValues getContentEncoding();

    /**
     * Compress the input buffer. The caller is responsible for releasing the
     * new buffers that are returned.
     * 
     * @param buffer
     * @return List<WsByteBuffer>
     */
    List<WsByteBuffer> compress(WsByteBuffer buffer);

    /**
     * Compress the list of input buffers into a list of output buffers. The
     * caller is responsible for releasing the list of new buffers returned.
     * 
     * @param buffers
     * @return List<WsByteBuffer>
     */
    List<WsByteBuffer> compress(WsByteBuffer[] buffers);

    /**
     * Called when the input data is complete, this will trigger any final
     * compression output and return any remaining data to write out. It is
     * the caller's responsibility to release these buffers.
     * 
     * @return List<WsByteBuffer> - any final data not yet passed back
     */
    List<WsByteBuffer> finish();

    /**
     * Query whether the finish() api has already been called.
     * 
     * @return boolean
     */
    boolean isFinished();

    /**
     * Query the number of raw bytes handed to this handler for compression so
     * far.
     * 
     * @return long
     */
    long getBytesRead();

    /**
     * Query the number of compressed bytes handed out by this handler.
     * 
     * @return long
     */
    long getBytesWritten();
}
