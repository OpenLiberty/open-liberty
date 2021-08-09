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

import java.util.LinkedList;
import java.util.List;
import java.util.zip.DataFormatException;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * Handler that represents the Identity compression, which is in fact no
 * compression at all.
 * 
 */
public class IdentityInputHandler implements DecompressionHandler {

    /** amount of data run through this handler */
    private long size = 0L;

    /**
     * Create an identity decompression handler.
     */
    public IdentityInputHandler() {
        // nothing to do
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#decompress(
     * com.ibm.wsspi.bytebuffer.WsByteBuffer)
     */
    @SuppressWarnings("unused")
    public List<WsByteBuffer> decompress(WsByteBuffer buffer) throws DataFormatException {
        List<WsByteBuffer> output = new LinkedList<WsByteBuffer>();
        output.add(buffer);
        this.size += buffer.remaining();
        return output;
    }

    /*
     * @see com.ibm.wsspi.http.channel.compression.DecompressionHandler#close()
     */
    public void close() {
        // nothing to do
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#isEnabled()
     */
    public boolean isEnabled() {
        return false;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#isFinished()
     */
    public boolean isFinished() {
        return false;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#getBytesRead()
     */
    public long getBytesRead() {
        return this.size;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#getBytesWritten
     * ()
     */
    public long getBytesWritten() {
        return this.size;
    }

}
