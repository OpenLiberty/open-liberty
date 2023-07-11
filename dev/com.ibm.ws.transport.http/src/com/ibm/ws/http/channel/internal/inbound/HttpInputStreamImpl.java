/*******************************************************************************
 * Copyright (c) 2009, 2024 IBM Corporation and others.
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
package com.ibm.ws.http.channel.internal.inbound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.inputstream.HttpInputStreamConnectWeb;
import com.ibm.ws.http.channel.inputstream.HttpInputStreamObserver;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Wrapper for an incoming HTTP request message body that provides the input
 * stream interface.
 */
public class HttpInputStreamImpl extends HttpInputStreamConnectWeb {
    /** trace variable */
    private static final TraceComponent tc = Tr.register(HttpInputStreamImpl.class,
                                                         HttpMessages.HTTP_TRACE_NAME,
                                                         HttpMessages.HTTP_BUNDLE);

    protected HttpInboundServiceContext isc = null;
    protected WsByteBuffer buffer = null;
    private IOException error = null;
    protected boolean closed = false;
    protected long bytesRead = 0L;
    private long bytesToCaller = 0L;

    // 25279 - Required for reading from Channel during an HTTP2 upgrade. True means the buffer contains all data and shouldn't go down to channel on next call
    private boolean dataAlreadyReadFromChannel = false;

    //Following are required to support MultiRead
    private boolean enableMultiReadofPostData = false; // custom property
    private ArrayList<WsByteBuffer> postDataBuffer;
    protected boolean firstReadCompleteforMulti = false;
    private boolean readChannelComplete = false;
    private int postDataIndex = 0;
    protected long bytesReadFromStore = 0L;

    private HttpInputStreamObserver obs = null;
    private FullHttpRequest nettyRequest = null;
    private ByteBuf nettyBody = null;

    /**
     * Constructor.
     *
     * @param context
     */
    public HttpInputStreamImpl(HttpInboundServiceContext context) {
        this.isc = context;
    }

    public HttpInputStreamImpl(HttpInboundServiceContext context, FullHttpRequest request) {
        this.isc = context;
        this.nettyRequest = request;
        this.nettyBody = nettyRequest.content();
        buffer = ChannelFrameworkFactory.getBufferManager().wrap(nettyBody.nioBuffer()).position(nettyBody.readerIndex());
        this.bytesRead += buffer.remaining();
    }

    /*
     * @see java.lang.Object#toString()
     */
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.inbound.HttpInputStreamX#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(getClass().getSimpleName());
        sb.append('@').append(Integer.toHexString(hashCode()));
        sb.append(" closed=").append(this.closed);
        sb.append(" error=").append(this.error);
        sb.append(" received=").append(this.bytesRead);
        sb.append(" given=").append(this.bytesToCaller);
        sb.append(" buffer=").append(this.buffer);
        return sb.toString();
    }

    /**
     * Perform validation of the stream before processing external requests
     * to read data.
     *
     * @throws IOException
     */
    protected void validate() throws IOException {
        if (isClosed()) {
            throw new IOException("Stream is closed");
        }
        if (null != this.error) {
            throw this.error;
        }
    }

    /**
     * Check the input buffer for data. If necessary, attempt a read for a new
     * buffer.
     *
     * @return boolean - true means data is ready
     * @throws IOException
     */
    protected boolean checkBuffer() throws IOException {
        if (!enableMultiReadofPostData && !dataAlreadyReadFromChannel) {
            if (null != this.buffer) {
                if (this.buffer.hasRemaining()) {
                    return true;
                }
                this.buffer.release();
                this.buffer = null;
            }
            try {
                this.buffer = this.isc.getRequestBodyBuffer();
                if (null != this.buffer) {
                    // record the new amount of data read from the channel
                    this.bytesRead += this.buffer.remaining();
                    // Tr.debug(tc, "Buffer=" + WsByteBufferUtils.asString(this.buffer));
                    return true;
                }
                return false;
            } catch (IOException e) {
                this.error = e;
                throw e;
            }
        } else {
            return checkMultiReadBuffer();
        }
    }

    /**
     * Check the input buffer for data. If necessary, attempt a read for a new
     * buffer and store it.
     *
     * @return
     * @throws IOException
     */
    private boolean checkMultiReadBuffer() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "checkMultiReadBuffer", " firstReadCompleteforMulti [" + firstReadCompleteforMulti + "] " + this);
        }
        //first check existing buffer
        if (null != this.buffer) {
            if (this.buffer.hasRemaining()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "checkMultiReadBuffer Exit | return true ; has remaining ->" + this);
                }
                return true;
            }
            if (firstReadCompleteforMulti) { // multiRead enabled and subsequent read
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "checkMultiReadBuffer, buffer is completely read, ready this buffer for the subsequent read ->" + this);
                }
                postDataBuffer.get(postDataIndex).flip(); // make position 0 , to read it again from start
                postDataIndex++;
            } else {
                this.buffer.release();
            }
            this.buffer = null;
        }
        // no buffer read from store or first read of buffer
        if (firstReadCompleteforMulti) {
            // first read from what we have stored, if need more than go to channel
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "checkMultiReadBuffer ,index ->" + postDataIndex + " ,storage.size ->" + postDataBuffer.size());
            }
            if (postDataBuffer.size() <= postDataIndex && !dataAlreadyReadFromChannel) { // Don't need to read if data was already stored
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "checkMultiReadBuffer, requires more data, checking readRemainingFromChannel.");
                }
                //get remaining from channel now as read needs more than the stored
                readRemainingFromChannel();
            }
            if (postDataBuffer.size() > postDataIndex) {
                this.buffer = postDataBuffer.get(postDataIndex);
            }

            if (null != this.buffer) {
                // record the new amount of data read from the store
                this.bytesReadFromStore += this.buffer.remaining();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "checkMultiReadBuffer Exit | bytes read from store [" + this.bytesReadFromStore + "] , return true" + this);
                }
                return true;
            }
        } else { // multiRead enabled and first read
            if (getBufferFromChannel()) {
                // store the channel buffer
                postDataBuffer.add(postDataIndex, this.buffer.duplicate());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "checkMultiReadBuffer, buffer ->" + postDataBuffer.get(postDataIndex)
                                 + " ,buffersize ->" + postDataBuffer.size() + " ,index ->" + postDataIndex);
                }
                postDataIndex++;

                // record the new amount of data read from the channel
                this.bytesRead += this.buffer.remaining();
                return true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "checkMultiReadBuffer:  no more buffer ->" + this);
        }
        return false;
    }

    /**
     * @throws IOException
     */
    private void readRemainingFromChannel() throws IOException {
        if (!readChannelComplete) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "readRemainingFromChannel, data not completely read during first read");
            }
            int localIx = postDataIndex;
            while (getBufferFromChannel()) {

                postDataBuffer.add(postDataIndex, this.buffer.duplicate());

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "readRemainingFromChannel, buffer ->" + postDataBuffer.get(postDataIndex)
                                 + " ,size ->" + postDataBuffer.size()
                                 + " ,index ->" + postDataIndex);
                }
                postDataIndex++;

                this.bytesRead += this.buffer.remaining(); // record the new amount of data read from the channel

                this.buffer.release(); // release any buffers read
                this.buffer = null;
            }
            postDataIndex = localIx;
            readChannelComplete = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "readRemainingFromChannel, all buffer read and stored from channel , now read it from store");
            }
        }
    }

    /**
     * @return
     * @throws IOException
     */
    private boolean getBufferFromChannel() throws IOException {

        try {
            this.buffer = this.isc.getRequestBodyBuffer();
            if (null != this.buffer) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getBufferFromChannel: new buffer from channel ->" + this);
                }
                return true;
            }

        } catch (IllegalHttpBodyException e) {
            this.error = e;
            throw e;
        } catch (IOException e) {
            this.error = e;
            throw e;
        }
        return false;
    }

//    @Override
//    public int available() throws IOException {
//        validate();
//        int rc = (null == this.buffer) ? 0 : this.buffer.remaining();
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "available: " + rc);
//        }
//        return rc;
//    }

    @Override
    public int available() throws IOException {
        validate();
        int rc = 0;
        if (null != this.buffer) {
            rc = this.buffer.remaining();
        } else {
            if (!enableMultiReadofPostData && !dataAlreadyReadFromChannel) {
                rc = 0;
            } else {
                // read the stored buffer(s) and return what all can be read in non-blocking way
                int localIndex = postDataIndex;
                WsByteBuffer localBuffer = null;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "available ,index ->" + postDataIndex + " ,storage.size ->" + postDataBuffer.size());
                }

                while (postDataBuffer.size() > localIndex) {
                    localBuffer = postDataBuffer.get(localIndex);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "available " + this + ", local position ->" + localBuffer.position());
                    }
                    if (null != localBuffer) {
                        localIndex++;
                        rc += localBuffer.remaining();
                    }
                }
                localBuffer = null;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "available: " + rc);
        }
        return rc;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        if (isClosed()) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Closing stream: " + this);
        }
        //adding MultiRead option
        if (!this.enableMultiReadofPostData) {
            if(dataAlreadyReadFromChannel){
                // Read happened from channel so on close we need to clean up all remaning data
                cleanupforMultiRead();
            }
            if (null != this.buffer) {
                this.buffer.release();
                this.buffer = null;
            }
            validate();
        }else {
            if (null != this.buffer) {
                if (firstReadCompleteforMulti) {
                    this.buffer.rewind(); // make position 0, the buffer is ready for next read
                } else {
                    this.buffer.release();
                }
                this.buffer = null;
            }
            validate();
            if (obs != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Alert for close , obs -->" + obs + " buffer ->" + this);
                }
                obs.alertISClose();
            }
            this.firstReadCompleteforMulti = true;
            this.postDataIndex = 0;
        }
        this.closed = true;
    }

    @Override
    final public boolean isClosed() {
        return this.closed;
    }

    @Override
    public synchronized void mark(int readlimit) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Ignoring call to mark()");
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        validate();
        int rc = -1;
        if (checkBuffer()) {
            rc = this.buffer.get() & 0x000000FF;
            this.bytesToCaller++;
        }
        // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        // Tr.debug(tc, "read() rc=" + rc);
        // }
        return rc;
    }

    @Override
    public int read(byte[] output, int offset, int length) throws IOException {
        validate();
        if (0 == length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "read(byte[],int,int), target length was 0");
            }
            return 0;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "read(byte[],int,int)", "length-->" + length + " , buffer->" + this);
        }
        if (!checkBuffer()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.exit(tc, "read(byte[],int,int), EOF");
            }
            return -1;
        }
        int avail = this.buffer.remaining();
        int amount = (length > avail) ? avail : length;
        this.buffer.get(output, offset, amount);
        // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        // Tr.debug(tc, "read(byte[],int,int) rc=" + amount);
        // }
        this.bytesToCaller += amount;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "read(byte[],int,int)", this);
        }
        return amount;
    }

    @Override
    public int read(byte[] output) throws IOException {
        return read(output, 0, output.length);
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("Mark not supported");
    }

    @Override
    public long skip(long target) throws IOException {
        validate();
        // otherwise cycle through buffers until we reach the target or EOF
        long total = 0L;
        long remaining = target;
        while (total < target) {
            if (!checkBuffer()) {
                // reached EOF
                break; // out of while
            }
            int avail = this.buffer.remaining();
            if (avail > remaining) {
                // this buffer satisfies the remaining length
                this.buffer.position(this.buffer.position() + (int) remaining);
                total += remaining;
            } else {
                // we're skipping the entire contents of this buffer
                this.buffer.release();
                this.buffer = null;
                total += avail;
                remaining -= avail;
            }

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "skip(" + target + ") rc=" + total);
        }
        // while we didn't actually give them to the caller, we have "used"
        // these bytes from the input stream
        this.bytesToCaller += total;
        return total;
    }

    @Override
    public void restart() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "restart", "Start re-read of data");
        }
        bytesToCaller = 0;

        this.closed = false;
        if (obs != null) {
            obs.alertISOpen();
        }
    }

    public void setReadFromChannelComplete() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setReadFromChannelComplete", "Reseting indexes of data and setting all data read");
        }
        this.enableMultiReadofPostData = false;
        dataAlreadyReadFromChannel = true;
        firstReadCompleteforMulti = true;
        bytesToCaller = 0;
        postDataIndex = 0;
    }

    public void setupChannelMultiRead() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setupChannelMultiRead", "Adding everything necessary for reading from HTTP channel.");
        }
        this.enableMultiReadofPostData = true;
        postDataBuffer = new ArrayList<WsByteBuffer>();
        firstReadCompleteforMulti = false;
        dataAlreadyReadFromChannel = false;
    }

    @Override
    public void setISObserver(HttpInputStreamObserver obs) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "obs  ->" + obs);
        }
        this.obs = obs;
    }

    @Override
    public void if_enableMultiReadofPostData_set(boolean set) {
        this.enableMultiReadofPostData = set;
    }

    @Override
    public void setupforMultiRead(boolean set) {
        this.enableMultiReadofPostData = set;

        if (this.enableMultiReadofPostData) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "init", "create postData arrayList");
            }
            postDataBuffer = new ArrayList<WsByteBuffer>();
            firstReadCompleteforMulti = false;
            readChannelComplete = false;
            dataAlreadyReadFromChannel = false;
        }
    }

    @Override
    public void cleanupforMultiRead() {

        postDataIndex = 0;
        bytesReadFromStore = 0L;
        bytesRead = 0L;
        bytesToCaller = 0L;
        firstReadCompleteforMulti = false;
        dataAlreadyReadFromChannel = false;

        if (this.buffer != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cleanupforMultiRead", "remove buffer ->" + this.buffer);
            }
            this.buffer = null;
        }
        if (postDataBuffer != null) {
            for (Iterator<WsByteBuffer> i = postDataBuffer.iterator(); i.hasNext();) {

                WsByteBuffer postbuffer = i.next();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "cleanupforMultiRead", "postbuffer released ->" + postbuffer);
                }
                postbuffer.release();
            }
            postDataBuffer = null;

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "init", "cleanupforMultiRead, postDataBuffer is not available");
            }
        }
    }

}