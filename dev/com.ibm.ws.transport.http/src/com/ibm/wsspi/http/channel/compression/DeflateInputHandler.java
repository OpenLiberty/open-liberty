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
import java.util.zip.Inflater;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * Handler class for the deflate decompression method.
 * 
 */
public class DeflateInputHandler implements DecompressionHandler {

    /** RAS variable */
    private static final TraceComponent tc = Tr.register(DeflateInputHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Inflater used for this handler instance */
    private Inflater inflater = null;
    /** Output buffer to decompress data into */
    private final byte[] buf = new byte[16384];

    // running counts including resets
    private long countRead = 0;
    private long countWritten = 0;

    /**
     * Create a handler to apply the deflate decompression method.
     * 
     */
    public DeflateInputHandler() {
        this.inflater = new Inflater(false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created a deflate input handler; " + this);
        }
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#isEnabled()
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Release a list of buffers.
     * 
     * @param list
     */
    private void release(List<WsByteBuffer> list) {
        while (!list.isEmpty()) {
            list.remove(0).release();
        }
    }

    private void reset() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "reset");
        }
        this.countRead += this.inflater.getBytesRead();
        this.countWritten += this.inflater.getBytesWritten();
        this.inflater.reset();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "reset done on inflater, counters updated");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "reset");
        }
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#decompress(
     * com.ibm.wsspi.bytebuffer.WsByteBuffer)
     */
    public List<WsByteBuffer> decompress(WsByteBuffer inputBuffer) throws DataFormatException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "decompress, input=" + inputBuffer);
        }
        List<WsByteBuffer> list = new LinkedList<WsByteBuffer>();
        int dataSize = inputBuffer.remaining();
        byte[] input = new byte[dataSize];
        inputBuffer.get(input, 0, dataSize);
        int inOffset = 0;

        //if the inflater is finished, then this is a repeat call
        //to decompress with the same handler and a reset is required
        if (this.inflater.finished())
            reset();

        if (!this.inflater.finished()) {
            this.inflater.setInput(input, inOffset, dataSize - inOffset);
        }
        long initialBytesRead = this.inflater.getBytesRead();
        int outOffset = 0;

        // keep decompressing until we've used up the entire input buffer or reached
        // the end of the compressed stream
        int len = -1;
        while (inOffset < input.length && !this.inflater.finished() && 0 != len) {
            try {
                len = this.inflater.inflate(this.buf, outOffset, this.buf.length - outOffset);
            } catch (DataFormatException dfe) {
                // no FFDC required, clean up any buffers we've allocated so far
                release(list);
                throw dfe;
            }
            long bytesRead = this.inflater.getBytesRead();
            inOffset += (bytesRead - initialBytesRead);
            initialBytesRead = bytesRead;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Decompressed amount=" + len + " inOffset=" + inOffset + " read=" + this.inflater.getBytesRead() + " written=" + this.inflater.getBytesWritten()
                             + " finished=" + this.inflater.finished());
            }
            outOffset += len;
            if (outOffset == this.buf.length) {
                WsByteBuffer buffer = HttpDispatcher.getBufferManager().allocate(this.buf.length);
                buffer.put(this.buf, 0, this.buf.length);
                buffer.flip();
                list.add(buffer);
                outOffset = 0;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Storing decompressed buffer; " + buffer);
                }
            }
        }
        // save off any extra output that is ready
        if (0 < outOffset) {
            WsByteBuffer buffer = HttpDispatcher.getBufferManager().allocate(outOffset);
            buffer.put(this.buf, 0, outOffset);
            buffer.flip();
            list.add(buffer);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Stored final decompressed buffer; " + buffer);
            }
        }
        if (inOffset < dataSize) {
            // did not use the entire buffer
            inputBuffer.position(inputBuffer.position() - (dataSize - inOffset));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "input buffer has unused data; " + inputBuffer);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "decompress, output=" + list.size());
        }
        return list;
    }

    /*
     * @see com.ibm.wsspi.http.channel.compression.DecompressionHandler#close()
     */
    public void close() {
        this.inflater.end();
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#isFinished()
     */
    public boolean isFinished() {
        return this.inflater.finished();
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#getBytesRead()
     */
    public long getBytesRead() {
        return this.countRead + this.inflater.getBytesRead();
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#getBytesWritten
     * ()
     */
    public long getBytesWritten() {
        return this.countWritten + this.inflater.getBytesWritten();
    }
}
