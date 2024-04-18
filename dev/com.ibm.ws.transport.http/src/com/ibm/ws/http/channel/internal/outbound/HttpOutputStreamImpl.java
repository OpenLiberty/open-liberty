/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.outbound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.h2internal.exceptions.FlowControlException;
import com.ibm.ws.http.channel.h2internal.exceptions.StreamClosedException;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamConnectWeb;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamObserver;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.exception.WriteBeyondContentLengthException;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

/**
 * HTTP transport output stream that wraps the bytebuffer usage and the HTTP
 * channel write logic with an outputstream interface.
 */
public class HttpOutputStreamImpl extends HttpOutputStreamConnectWeb {
    /** trace variable */
    private static final TraceComponent tc = Tr.register(HttpOutputStreamImpl.class,
                                                         HttpMessages.HTTP_TRACE_NAME,
                                                         HttpMessages.HTTP_BUNDLE);

    /** HTTP connection reference */
    protected HttpInboundServiceContext isc = null;
    /** Reference to the connection object */
    protected VirtualConnection vc = null;
    /** Flag on whether to ignore first flush call or not */
    protected boolean ignoreFlush = true;
    /** Size of the ByteBuffers to allocate */
    private int bbSize;
    /** Total amount to buffer internally before triggering an auto-flush */
    protected int amountToBuffer = 0;
    /** Current amount buffered internally */
    protected int bufferedCount = 0;
    /** Array of buffers used for the content buffering */
    protected WsByteBuffer[] output = null;
    /** Index into the output array for the current writes */
    protected int outputIndex = 0;
    /** Flag on whether the stream is writing content */
    protected boolean writing = false;
    /** Flag on whether the stream has been closed */
    protected boolean closed = false;
    /** Possible error that may have been seen during IO requests */
    protected IOException error = null;
    /** Bytes written through this stream */
    protected long bytesWritten = 0L;
    /* if the contentLength is set, we can only write bytesRemaining bytes */
    protected boolean contentLengthSet = false;
    protected long bytesRemaining = -1;

    protected boolean isClosing = false;
    protected boolean hasFinished = false;

    /**
     * The observer that will be notified when the stream is first written.
     */
    protected HttpOutputStreamObserver obs = null;
    protected boolean WCheadersWritten = false;

    /**
     * Constructor of an output stream for a given service context.
     *
     * @param context
     */
    public HttpOutputStreamImpl(HttpInboundServiceContext context) {
        this.isc = context;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#setIsClosing(boolean)
     */
    @Override
    public void setIsClosing(boolean b) {
        isClosing = b;
    }

    /**
     * Set the reference to the virtual connection.
     *
     * @param inVC
     */
    public void setVirtualConnection(VirtualConnection inVC) {
        this.vc = inVC;
    }

    /**
     * @return the vc
     */
    public VirtualConnection getVc() {
        return vc;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#getBufferSize()
     */
    @Override
    public int getBufferSize() {
        return this.amountToBuffer;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#setBufferSize(int)
     */
    @Override
    public void setBufferSize(int size) {
        if (this.writing || isClosed()) {
            throw new IllegalStateException("Stream unable to set size");
        }
        this.amountToBuffer = size;
        this.bbSize = (49152 < size) ? 32768 : 8192;

        if ((isc != null) && (isc instanceof HttpInboundServiceContextImpl)) {
            if (!((HttpInboundServiceContextImpl) isc).getHttpConfig().useNetty()) {
                // make sure we never create larger frames than the max http2 frame size
                Integer h2size = (Integer) this.getVc().getStateMap().get("h2_frame_size");
                if (h2size != null && h2size < bbSize) {
                    this.bbSize = h2size;
                }
            }
        }

        int numBuffers = (size / this.bbSize);
        if (0 == size || 0 != (size % this.bbSize)) {
            numBuffers++;
        }
        this.output = new WsByteBuffer[numBuffers];
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setBufferSize=" + size + "; " + this);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#clear()
     */
    @Override
    public void clear() {
        // CLear the output buffers if Netty is not in use. Netty will clear when buffer conversion is used
//        if (null != this.output && !((HttpInboundServiceContextImpl) isc).getHttpConfig().useNetty()) {
        if (null != this.output) {
            for (int i = 0; i < this.output.length; i++) {
                if (null != this.output[i]) {
                    // Will only release the buffers if Netty is not in use
//                    if (!((HttpInboundServiceContextImpl) isc).getHttpConfig().useNetty())
                    this.output[i].release();
                    this.output[i] = null;
                }
            }
        }
        this.outputIndex = 0;
        this.bufferedCount = 0;
        this.bytesWritten = 0L;
        this.isClosing = false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#getBytesWritten()
     */
    @Override
    public long getBytesWritten() {
        return this.bytesWritten;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#getBufferedCount()
     */
    @Override
    public long getBufferedCount() {
        return this.bufferedCount;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#hasBufferedContent()
     */
    @Override
    final public boolean hasBufferedContent() {
        return (0 < this.bufferedCount);
    }

    /**
     * Perform validation of the stream before processing external requests
     * to write data.
     *
     * @throws IOException
     */
    protected void validate() throws IOException {
        if (null != this.error) {
            throw this.error;
        }
        if (isClosed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "validate - is closed: hc: " + this.hashCode() + " details: " + this);
            }

            throw new IOException("Stream is closed");
        }
        // There's an H2 timing window where the server could be working on a response, it gets interrupted,
        // the frame that comes in has an error or is a close frame, and the connection gets shutdown and all resources
        // including the response are cleaned up.  Then we come back here after the interruption.
        // Handle this case.
        // WebSocket has this same timing window

        if ((isc != null) && (isc instanceof HttpInboundServiceContextImpl) &&
            (null == this.isc.getResponse())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "validate response is cleaned up hc: " + this.hashCode() + " details: " + this);
            }

            throw new IOException("Response already destroyed on error condition or close request from the client");

        }

        if (null == this.output) {
            setBufferSize(32768);
        }
    }

    /**
     * Access the proper output buffer for the current write attempt.
     *
     * @return WsByteBuffer
     */
    protected WsByteBuffer getBuffer() {
        WsByteBuffer buffer = this.output[this.outputIndex];
        if (null == buffer) {
            buffer = HttpDispatcher.getBufferManager().allocate(this.bbSize);
            buffer.clear();
            this.output[this.outputIndex] = buffer;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getBuffer, buffer -->" + buffer + " ,outputIndex -->" + this.outputIndex);
            }
        } else if (!buffer.hasRemaining()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getBuffer, buffer  -->" + buffer + " ,outputIndex -->" + this.outputIndex + " output length -->" + this.output.length);
            }
            buffer.flip();
            this.outputIndex++;
            // next array spot may or may not exist from previous iterations
            if (null == this.output[this.outputIndex]) {
                this.output[this.outputIndex] = HttpDispatcher.getBufferManager().allocate(this.bbSize);
            }
            buffer = this.output[this.outputIndex];
            buffer.clear();
        }
        return buffer;
    }

    /**
     * Write the given information to the output buffers.
     * If it went async during flush , save the remaining data and stop.
     * When callback on complete, write the remaining data.
     *
     * @param value
     * @param start - offset into value
     * @param len   - length from that offset to write
     * @throws IOException
     */
    private void writeToBuffers(byte[] value, int start, int len) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Writing " + len + ", buffered=" + this.bufferedCount);
        }
        if (value.length < (start + len)) {
            throw new IllegalArgumentException("Length outside value range");
        }
        this.writing = true;
        int remaining = len;
        int offset = start;
        while (0 < remaining) {
            WsByteBuffer buffer = getBuffer();
            int avail = buffer.remaining();
            if (contentLengthSet && bytesRemaining < bufferedCount + remaining) {
                //write what we can and throw an exception - it will be caught in servletWrapper
                int numberToWrite = (int) bytesRemaining - bufferedCount;
                boolean throwExceptionThisTime = true;
                if (numberToWrite > avail) {
                    numberToWrite = avail;
                    throwExceptionThisTime = false;
                }
                this.bufferedCount += numberToWrite;
                buffer.put(value, offset, numberToWrite);
                remaining = remaining - numberToWrite;
                if (throwExceptionThisTime) {
                    throw new WriteBeyondContentLengthException();
                }
            }
            if (avail >= remaining) {
                // write all remaining data
                this.bufferedCount += remaining;
                buffer.put(value, offset, remaining);
                remaining = 0;
            } else {
                // write what we can
                this.bufferedCount += avail;
                buffer.put(value, offset, avail);
                offset += avail;
                remaining -= avail;
            }
            if (this.bufferedCount >= this.amountToBuffer) {
                this.ignoreFlush = false;
                flushBuffers();
            }
        }
    }

    /**
     * User attempted to use the optimized FileChannel writing; however, that
     * path is currently disabled. This method will read the file and do repeated
     * write calls until done. This avoids TCP channel doing it in one big block.
     *
     * @param fc
     * @throws IOException
     */
    @FFDCIgnore({ IOException.class })
    private void convertFile(FileChannel fc) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Converting FileChannel to buffers");
        }
        final WsByteBuffer[] body = new WsByteBuffer[1];
        final WsByteBufferPoolManager mgr = HttpDispatcher.getBufferManager();
        final long max = fc.size();
        final long blocksize = (1048576L < max) ? 1048576L : max;
        long offset = 0;
        while (offset < max) {
            ByteBuffer bb = fc.map(MapMode.READ_ONLY, offset, blocksize);
            offset += blocksize;
            WsByteBuffer wsbb = mgr.wrap(bb);
            body[0] = wsbb;
            try {
                this.isc.sendResponseBody(body);
            } catch (MessageSentException mse) {
                FFDCFilter.processException(mse, getClass().getName(),
                                            "convertFile", new Object[] { this, this.isc });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Invalid state, message-sent-exception received; " + this.isc);
                }
                this.error = new IOException("Invalid state");
                throw this.error;
            } catch (IOException ioe) {
                // no FFDC required
                this.error = ioe;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received exception during write: " + ioe);
                }
                throw ioe;
            } finally {
                wsbb.release();
            }
        } // end-while
    }

    /**
     * Check whether we can actually write using file channels.
     *
     * @return boolean - true means we CANNOT write using FileChannels
     */
    private boolean cannotWriteFC() {
        if (null == this.vc) {
            // assume we can
            return true;
        }
        // check status of the VC
        return !this.vc.isFileChannelCapable();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#writeFile(java.nio.channels.FileChannel)
     */
    @Override
    @FFDCIgnore({ IOException.class })
    public void writeFile(FileChannel fc) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "writeFile: " + fc);
        }
        if (cannotWriteFC()) {
            // not file channel capable
            convertFile(fc);
            return;
        }
        // make sure the headers are written separately from the file buffer
        flushHeaders();
        WsByteBuffer fb = HttpDispatcher.getBufferManager().allocateFileChannelBuffer(fc);
        try {
            // TODO should adjust write timeout based on file size. Large files
            // can only be written so fast so a 1Gb file should have larger
            // timeout than a 100K file
            this.isc.sendResponseBody(new WsByteBuffer[] { fb });
            this.bytesWritten += fc.size();
        } catch (MessageSentException mse) {
            FFDCFilter.processException(mse, getClass().getName(),
                                        "writeFile", new Object[] { this, this.isc });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Invalid state, message-sent-exception received; " + this.isc);
            }
            this.error = new IOException("Invalid state");
            throw this.error;
        } catch (IOException ioe) {
            // no FFDC required
            this.error = ioe;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received exception during write: " + ioe);
            }
            throw ioe;
        } finally {
            fb.release();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#flushHeaders()
     */
    @Override
    @FFDCIgnore({ IOException.class })
    public void flushHeaders() throws IOException {
        // Extra NPE tracing
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (null == this.isc) {
                Tr.debug(tc, "flushHeaders: isc is null");
            } else if (null == this.isc.getResponse()) {
                Tr.debug(tc, "flushHeaders: isc.getResponse() is null, isc is " + this.isc);
            } else {
                Tr.debug(tc, "flushHeaders: committed=" + this.isc.getResponse().isCommitted(), this.isc, this.isc.getResponse());
            }
        }

        // Run validate to check if the stream is already closed, if so exit
        validate();

        this.ignoreFlush = false;
        if ((null != this.isc.getResponse()) && !this.isc.getResponse().isCommitted()) {
            this.isc.getResponse().setCommitted();
        } else {
            // response headers already committed (written)
            // or response has been freed on previous error
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.debug(tc, "Response headers already committed or response cleared; " + this.isc);
            }
            return;
        }
        try {
            this.isc.sendResponseHeaders();
        } catch (MessageSentException mse) {
            FFDCFilter.processException(mse, getClass().getName(),
                                        "flushHeaders", new Object[] { this, this.isc });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Invalid state, message-sent-exception received; " + this.isc);
            }
            this.error = new IOException("Invalid state");
            throw this.error;
        } catch (IOException ioe) {
            // no FFDC required
            this.error = ioe;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received exception during write: " + ioe);
            }
            throw ioe;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#flushBuffers()
     */
    @Override
    @FFDCIgnore({ IOException.class })
    public void flushBuffers() throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Flushing buffers: " + this);
        }

        if ((isc != null) && (isc instanceof HttpInboundServiceContextImpl)) {
           // if (!((HttpInboundServiceContextImpl) isc).getHttpConfig().useNetty()) {
                if (this.isc.getResponse() == null) {
                    IOException x = new IOException("response Object(s) (e.g. getObjectFactory()) are null");
                    throw x;
                }

                if (!this.isc.getResponse().isCommitted()) {
                    if (obs != null && !this.WCheadersWritten) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "obs  ->" + obs);
                        }
                        obs.alertOSFirstFlush();
                    }

                    this.isc.getResponse().setCommitted();
                }
           // }
        }
        
        System.out.println(" Done setting response commited, should we write? ");

        if (this.ignoreFlush) {
            this.ignoreFlush = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring first flush attempt");
            }
            System.out.println(" First flush ignored ... should this be happening?");
            return;
        }

        final boolean writingBody = (hasBufferedContent());
        
        System.out.println( " Writing body? -> " + hasBufferedContent());
        // flip the last buffer for the write...
        if (writingBody && null != this.output[this.outputIndex]) {
            this.output[this.outputIndex].flip();
        }
        try {
            WsByteBuffer[] content = (writingBody) ? this.output : null;
            
            System.out.println( "isClosed -> " + isClosed() + " | isClosing -> " + isClosing);
            
            if (isClosed() || this.isClosing) {
                if (!hasFinished) { //if we've already called finishResponseMessage - don't call again
                    // on a closed stream, use the final write api
                    
                    System.out.println(" should be calling finish Response message -> ");
                    
                    
                    this.isc.finishResponseMessage(content);
                    this.isClosing = false;
                    this.hasFinished = true;
                }
            } else {
                // else use the partial body api
                System.out.println(" sending response instead? ");
                this.isc.sendResponseBody(content);
            }
        } catch (MessageSentException mse) {
            FFDCFilter.processException(mse, getClass().getName(),
                                        "flushBuffers", new Object[] { this, this.isc });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Invalid state, message-sent-exception received; " + this.isc);
            }
            this.error = new IOException("Invalid state");
            throw this.error;
        } catch (IOException ioe) {
            // no FFDC required
            this.error = ioe;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received exception during write: " + ioe);
            }
            Throwable th = ioe.getCause();
            if (th instanceof FlowControlException || th instanceof StreamClosedException) {
                // http/2 stream write failed - we don't want to pass this back up to FFDC
                Tr.debug(tc, "HTTP/2 stream could not write; setting error on this output stream");
                return;
            }
            throw ioe;
        } finally {
            this.bytesWritten += this.bufferedCount;
            if (this.contentLengthSet) {
                this.bytesRemaining -= this.bufferedCount;
            }
            this.bufferedCount = 0;
            this.outputIndex = 0;
            // Note: this logic only works for sync writes
//            if (writingBody && !((HttpInboundServiceContextImpl) isc).getHttpConfig().useNetty()) {
            if (writingBody) {
                this.output[0].clear();
                for (int i = 1; i < this.output.length; i++) {
                    if (null != this.output[i]) {
                        // mark them empty so later writes don't mistake them
                        // as having content
                        this.output[i].limit(0);
                    }
                }
            }
        }
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(getClass().getSimpleName());
        sb.append('@').append(Integer.toHexString(hashCode()));
        sb.append(" writing=").append(this.writing);
        sb.append(" closed=").append(this.closed);
        sb.append(" bufferedCount=").append(this.bufferedCount);
        sb.append(" bytesWritten=").append(this.bytesWritten);
        sb.append(" error=").append(this.error);
        if (null != this.output) {
            sb.append(" outindex=").append(this.outputIndex);
            for (int i = 0; i < this.output.length; i++) {
                sb.append("\r\n\t").append(this.output[i]);
            }
        }
        return sb.toString();
    }

    /*
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException {
        if (isClosed()) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Closing stream: hc: " + this.hashCode() + " details: " + this);
        }
        // Run validate here to catch a race condition when two threads are both handling shutdown
        try {
            validate();
            this.closed = true;
            this.ignoreFlush = false;
            flushBuffers();
        } catch (IOException ioe) {
            this.closed = true;
            this.ignoreFlush = false;
            throw ioe;
        } finally {
            // must release the buffers even if the flush fails
            clear();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#isClosed()
     */
    @Override
    final public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void flush() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Flushing stream: " + this);
        }
        validate();
        if (!this.hasFinished) {
            flushBuffers();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "flush hasFinished=true; skipping flushBuffers() on " + this.hashCode() + " details: " + this);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#flush(boolean)
     */
    @Override
    public void flush(boolean ignoreFlag) throws IOException {
        ignoreFlush = ignoreFlag;
        flush();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.outbound.HttpOutputStream#setContentLength(long)
     */
    @Override
    public void setContentLength(long length) {
        contentLengthSet = true;
        bytesRemaining = length;
    }

    /*
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] value, int start, int len) throws IOException {
        validate();
        writeToBuffers(value, start, len);

    }

    /*
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(byte[] value) throws IOException {
        validate();
        writeToBuffers(value, 0, value.length);

    }

    /*
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int value) throws IOException {
        validate();
        byte[] buf = new byte[1];
        buf[0] = (byte) value;
        writeToBuffers(buf, 0, 1);

    }

    /**
     * Sets an observer for this output stream. The observer will be
     * notified when the stream is first written to.
     */
    @Override
    public void setObserver(HttpOutputStreamObserver obs) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "obs  ->" + obs);
        }
        this.obs = obs;
    }

    @Override
    public void setWebC_headersWritten(boolean headersWritten) {
        this.WCheadersWritten = headersWritten;
    }

    @Override
    public void setWC_remoteUser(String remoteUser) {
        //Set the given user here to the ISC or the response message
        if (isc instanceof HttpInboundServiceContextImpl) {
            ((HttpInboundServiceContextImpl) isc).setRemoteUser(remoteUser);
        }
    }

}