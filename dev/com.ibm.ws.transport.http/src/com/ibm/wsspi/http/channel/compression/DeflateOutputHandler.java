/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
import java.util.zip.Deflater;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;

/**
 * Handler for the deflate compression method.
 */
public class DeflateOutputHandler implements CompressionHandler {
    /** RAS variable */
    private static final TraceComponent tc = Tr.register(DeflateOutputHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Deflater used for this output stream */
    private Deflater deflater = null;
    /** Output buffer used during the compression stage */
    private final byte[] buf = new byte[32768];

    /**
     * Create this deflate compression method output handler.
     *
     */
    public DeflateOutputHandler() {
        this.deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created a deflate output handler; " + this);
        }
    }

    /**
     * Create this deflate compression method output handler. This will check
     * the provided User-Agent value to test for an IE browser client as IE
     * requires that there is no deflate wrapper around the compressed bytes.
     *
     * @param useragent
     *            - header from request
     */
    public DeflateOutputHandler(byte[] useragent) {
        if (null != useragent && isIEBrowser(useragent)) {
            // IE cannot handle the compressed bytes wrapped with encryption
            // markers, so we need to create a Deflater with the nowrap enabled
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "User-Agent indicates IE browser [" + GenericUtils.getEnglishString(useragent) + "]");
            }
            this.deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        } else {
            this.deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created a deflate output handler; " + this);
        }
    }

    /**
     * Check the user-agent input header to see if it contains the IE browser
     * marker.
     *
     * @param agent
     * @return boolean
     */
    private boolean isIEBrowser(byte[] agent) {
        int end = agent.length - 4;
        for (int i = 0; i < end;) {
            // check for MSIE
            if ('M' == agent[i]) {
                if ('S' == agent[++i] && 'I' == agent[++i] && 'E' == agent[++i]) {
                    return true;
                }
                // i now points to the failing char, loop and see if it was another M
            } else {
                i++;
            }
        }
        return false;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.CompressionHandler#compress(com.
     * ibm.wsspi.bytebuffer.WsByteBuffer)
     */
    @Override
    public List<WsByteBuffer> compress(WsByteBuffer buffer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "compress, input=" + buffer);
        }
        List<WsByteBuffer> list = new LinkedList<WsByteBuffer>();
        list = compress(list, buffer);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "compress, return list of size " + list.size());
        }
        return list;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.CompressionHandler#compress(com.
     * ibm.wsspi.bytebuffer.WsByteBuffer[])
     */
    @Override
    public List<WsByteBuffer> compress(WsByteBuffer[] buffers) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "compress, input=" + buffers);
        }
        List<WsByteBuffer> list = new LinkedList<WsByteBuffer>();
        if (null != buffers) {
            for (int i = 0; i < buffers.length; i++) {

                list = compress(list, buffers[i]);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "compress, return list of size " + list.size());
        }
        return list;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.CompressionHandler#getContentEncoding
     * ()
     */
    @Override
    public ContentEncodingValues getContentEncoding() {
        return ContentEncodingValues.DEFLATE;
    }

    /*
     * @see com.ibm.wsspi.http.channel.compression.CompressionHandler#finish()
     */
    @Override
    public List<WsByteBuffer> finish() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "finish");
        }
        List<WsByteBuffer> list = new LinkedList<WsByteBuffer>();
        if (isFinished()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "finish, previously finished");
            }
            return list;
        }
        WsByteBuffer buffer = null;
        this.deflater.finish();
        while (!this.deflater.finished()) {
            int num = this.deflater.deflate(this.buf, 0, this.buf.length);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Compressed amount=" + num + " read=" + this.deflater.getBytesRead() + " written=" + this.deflater.getBytesWritten());
            }

            if (0 < num) {
                buffer = makeBuffer(num);
                list.add(buffer);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "finish, return list of size " + list.size());
        }
        return list;
    }

    /*
     * @see com.ibm.wsspi.http.channel.compression.CompressionHandler#isFinished()
     */
    @Override
    public boolean isFinished() {
        return this.deflater.finished();
    }

    /**
     * Compress the given input buffer onto the output list.
     *
     * @param list
     * @param buffer
     * @return List<WsByteBuffer>
     */
    protected List<WsByteBuffer> compress(List<WsByteBuffer> list, WsByteBuffer buffer) {
        int dataSize = (null != buffer) ? buffer.remaining() : 0;

        if (0 == dataSize) {
            return list;
        }
        byte[] input = null;
        int initOffset = 0;
        if (buffer.hasArray()) {
            input = buffer.array();
            initOffset = buffer.arrayOffset() + buffer.position();
            buffer.position(buffer.limit());
        } else {
            input = new byte[dataSize];
            buffer.get(input);
        }
        this.deflater.setInput(input, initOffset, dataSize);

        // keep compressing data until we use up the input amount
        int offset = 0;
        while (!this.deflater.needsInput()) {
            int len = this.deflater.deflate(this.buf, offset, this.buf.length - offset);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Compressed amount=" + len + " read=" + this.deflater.getBytesRead() + " written=" + this.deflater.getBytesWritten());
            }
            if (0 == len) {
                break; // out of while
            }
            offset += len;
            if (offset == this.buf.length) {
                list.add(makeBuffer(offset));
                offset = 0;
            }
        }
        // if we generated output, put it into a buffer on the list
        if (0 < offset) {
            list.add(makeBuffer(offset));
        }
        return list;
    }

    /**
     * Create the output bytebuffer based on the output compressed storage.
     *
     * @param len
     * @return WsByteBuffer
     */
    private WsByteBuffer makeBuffer(int len) {
        WsByteBuffer buffer = HttpDispatcher.getBufferManager().allocateDirect(len);
        buffer.put(this.buf, 0, len);
        buffer.flip();
        return buffer;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.CompressionHandler#getBytesRead()
     */
    @Override
    public long getBytesRead() {
        return this.deflater.getBytesRead();
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.CompressionHandler#getBytesWritten()
     */
    @Override
    public long getBytesWritten() {
        return this.deflater.getBytesWritten();
    }

}
