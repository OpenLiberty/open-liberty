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
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;

/**
 * Handler for the gzip compression method.
 */
public class GzipOutputHandler implements CompressionHandler {
    /** RAS variable */
    private static final TraceComponent tc = Tr.register(GzipOutputHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Gzip header information */
    private final static byte[] GZIP_Header = { (byte) 0x1f, // ID1
                                                (byte) 0x8b, // ID2
                                                Deflater.DEFLATED, // Compression
                                                // method (CM)
                                                0, // FLaGs
                                                0, // MTIME (Modification TIME)
                                                0, // Modification time MTIME (int)
                                                0, // Modification time MTIME (int)
                                                0, // Modification time MTIME (int)
                                                0, // Extra flags (XFLG)
                                                0 // Unknown OS, java writes like
                                                  // MSDOS
    };

    /** Deflater used by this handler */
    private Deflater deflater = null;
    /** Flag on whether we have written out the gzip header information */
    private boolean haveWrittenHeader = false;
    /** Flag on whether this is the specialized x-gzip type */
    private boolean bIsXGzip = false;
    /** Checksum utility for this stream */
    private final CRC32 checksum = new CRC32();
    /** Output buffer used during the compression stage */
    private final byte[] buf = new byte[32768];

    /**
     * Create a gzip compression method output handler.
     *
     * @param isXGzip
     *            - boolean flag on whether this is an x-gzip handler
     */
    public GzipOutputHandler(boolean isXGzip) {
        this.deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        this.bIsXGzip = isXGzip;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created " + (isXGzip ? "x-gzip" : "gzip") + " output handler; " + this);
        }
    }

    /**
     * Write the gzip header onto the output list.
     *
     * @param list
     * @return List<WsByteBuffer>
     */
    private List<WsByteBuffer> writeHeader(List<WsByteBuffer> list) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Writing gzip header information");
        }
        WsByteBuffer hdr = HttpDispatcher.getBufferManager().allocateDirect(GZIP_Header.length);
        hdr.put(GZIP_Header);
        hdr.flip();
        list.add(hdr);
        this.haveWrittenHeader = true;
        return list;
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
        if (!this.haveWrittenHeader) {
            list = writeHeader(list);
        }
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
        if (!this.haveWrittenHeader) {
            list = writeHeader(list);
        }
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
        this.checksum.update(input, initOffset, dataSize);
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
     * com.ibm.wsspi.http.channel.compression.CompressionHandler#getContentEncoding
     * ()
     */
    @Override
    public ContentEncodingValues getContentEncoding() {
        if (this.bIsXGzip) {
            return ContentEncodingValues.XGZIP;
        }
        return ContentEncodingValues.GZIP;
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
        // write the gzip trailer information out
        writeInt((int) this.checksum.getValue(), this.buf, 0);
        writeInt((int) this.deflater.getBytesRead(), this.buf, 4);
        this.deflater.end();
        if (null != buffer && (buffer.capacity() - buffer.limit()) >= 8) {
            buffer.position(buffer.limit());
            buffer.limit(buffer.capacity());
            buffer.put(this.buf, 0, 8);
            buffer.flip();
        } else {
            WsByteBuffer trailer = HttpDispatcher.getBufferManager().allocateDirect(8);
            trailer.put(this.buf, 0, 8);
            trailer.flip();
            list.add(trailer);
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

    /**
     * Write an integer into the output space, starting at the input offset.
     *
     * @param value
     * @param data
     * @param offset
     */
    private void writeInt(int value, byte[] data, int offset) {
        int index = offset;
        data[index++] = (byte) (value & 0xff);
        data[index++] = (byte) ((value >> 8) & 0xff);
        data[index++] = (byte) ((value >> 16) & 0xff);
        data[index++] = (byte) ((value >> 24) & 0xff);
    }
}
