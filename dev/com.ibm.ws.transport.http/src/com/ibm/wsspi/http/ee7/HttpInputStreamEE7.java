/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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
package com.ibm.wsspi.http.ee7;

import java.io.EOFException;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.NettyVirtualConnectionImpl;
import com.ibm.ws.http.netty.pipeline.inbound.read.ReadFlowHandler;
import com.ibm.ws.http2.GrpcServletServices;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.channel.exception.BodyCompleteException;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 *
 */
public class HttpInputStreamEE7 extends HttpInputStreamImpl {

    /** trace variable */
    private static final TraceComponent tc = Tr.register(HttpInputStreamEE7.class,
                                                         HttpMessages.HTTP_TRACE_NAME,
                                                         HttpMessages.HTTP_BUNDLE);

    public HttpInputStreamEE7(HttpInboundServiceContext context) {
        super(context);
    }

    // public HttpInputStreamEE7(HttpInboundServiceContext context, FullHttpRequest request) {
    //     super(context, request);
    // }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpInputStream#close()
     */
    @Override
    public void close() throws IOException {
        super.close();
    }

    //Return the HttpInboundServiceContext associated with this HttpInputStream
    public HttpInboundServiceContext getISC() {
        return this.isc;
    }

    //Issue the first blocking read after the async read has completed. This seeds the buffer
    //with the current set of data
    public void initialRead() {
        try {
            this.buffer = this.isc.getRequestBodyBuffer();
            if (null != this.buffer) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Buffer returned from getRequestBodyBuffer : " + this.buffer);
                }
                // record the new amount of data read from the channel
                this.bytesRead += this.buffer.remaining();
            }
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception encountered during initialRead : " + e);
            }
        }
    }

    @FFDCIgnore(BodyCompleteException.class)
    public boolean asyncCheckBuffers(InterChannelCallback callback) {
        // Prefer streaming buffer first
        try {
            if (streaming) {
                return asyncCheckStreamingNettyBuffers(callback);
            }
        } catch (IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "asyncCheckBuffers(streaming) checkBuffer exception: " + ioe);
            }
        }

        try {
            VirtualConnection vc = isc.getRequestBodyBuffer(callback, false);

            if (vc != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Async body read worked immediately");
                }
                return checkBuffer();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Async body read returned null");
            }
        } catch (BodyCompleteException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "We have read the entire body, returning false : " + e);
            }
        } catch (IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "We should never get to this point, exception was : " + ioe);
            }
        }
        return false;
    }

    public boolean asyncCheckStreamingNettyBuffers(InterChannelCallback callback) throws IOException {
        if (!streaming) {
            return false;
        }
        if (isStreamingReadReady()){
            return true;
        }
        return armStreamingReadCallback(callback);
    }

    private boolean armStreamingReadCallback(InterChannelCallback callback) throws IOException{
        if (context == null || callback == null){
            return false;
        }

        AtomicBoolean delivered = new AtomicBoolean();
        Runnable[] successRef = new Runnable[1];
        successRef[0] = () -> {
            if (delivered.get()) {
                return;
            }
            try {
                if (!isStreamingReadReady()){
                    context.channel().attr(NettyHttpConstants.ASYNC_READ_CALLBACK).set(successRef[0]);
                    if (!isStreamingReadReady()) {
                        ReadFlowHandler.setBodyReadWanted(context, true);
                        return;
                    }
                }
                if (delivered.compareAndSet(false, true)) {
                    clearStreamingReadCallback();
                    callback.complete(NettyVirtualConnectionImpl.SHARED_NETTY_CALLBACK_VC);
                }
            } catch (IOException ioe) {
                if (delivered.compareAndSet(false, true)) {
                    clearStreamingReadCallback();
                    callback.error(NettyVirtualConnectionImpl.SHARED_NETTY_CALLBACK_VC, ioe);
                }
            } catch (RuntimeException rte) {
               if (delivered.compareAndSet(false, true)){
                    clearStreamingReadCallback();
                    callback.error(NettyVirtualConnectionImpl.SHARED_NETTY_CALLBACK_VC, rte);
                }
                throw rte; 
            }
        };
        Runnable error = () -> {
            if (!delivered.compareAndSet(false, true)) {
                return;
            }
            try {
                callback.error(NettyVirtualConnectionImpl.SHARED_NETTY_CALLBACK_VC, 
                    new EOFException("Peer input shutdown before request body completed"));
            } finally {
                clearStreamingReadCallback();
            }
        };
        context.channel().attr(NettyHttpConstants.ASYNC_READ_CALLBACK).set(successRef[0]);
        context.channel().attr(NettyHttpConstants.ASYNC_READ_ERROR_CALLBACK).set(error);
        
        if (isStreamingReadReady()){
            if (delivered.compareAndSet(false, true)){
                clearStreamingReadCallback();
                return true;
            }
            return false;
        }
        ReadFlowHandler.setBodyReadWanted(context, true);
        return false;
    }

    private boolean isStreamingReadReady() throws IOException {
        if (isStreamingReadCompleteForCallback()) {
            return true;
        }
        if (isMultiReadOfPostDataEnabled()) {
            return checkMultiReadBufferIfAvailable();
        }
        return isStreamingReadReadyForCallback();
    }

    private boolean isStreamingReadCompleteForCallback() throws IOException {
        return streaming && readChannelComplete && available() <= 0 && isStreamingEndReadyForCallback();
    }

    private void clearStreamingReadCallback() {
        context.channel().attr(NettyHttpConstants.ASYNC_READ_CALLBACK).set(null);
        context.channel().attr(NettyHttpConstants.ASYNC_READ_ERROR_CALLBACK).set(null);
    }

    public boolean isFinished() {
        boolean isFinished = false;

        //There are three levels of checking here
        //1) Is the connection closed?
        //2) Is there anything immediately available in the local buffers?
        //3) Does the channel believe it has read everything available?
        //4) If the channel believes it has read everything available then call checkBuffer, which will call into the channel and check if there is any more data available
        try {
            if (isClosed()) {
                return true;
            }
            if (streaming) {
                // If we still have buffered bytes, we are not finished.
                if (available() > 0) {
                    return false;
                }
                if (isStreamingEndReadyForCallback()) {
                    this.readChannelComplete = true;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "isFinished(streaming): queue EOS and no buffered data; returning true");
                    }
                    return true;
                }
                if (readChannelComplete) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "isFinished(streaming): EOS reached and no buffered data; returning true");
                    }
                    return true;
                }
                if (!autoRead && queue != null && queue.wantsInput() && context != null) {
                    context.channel().read();
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "isFinished(streaming): more input expected; returning false");
                }
                return false;
            }


            if (available() <= 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "There is no data currently available in the buffer");
                }

                if ((GrpcServletServices.grpcInUse) && (isc != null) && (isc instanceof HttpInboundServiceContextImpl)
                    && Objects.isNull(((HttpInboundServiceContextImpl) isc).getNettyContext())) {
                    int eos = ((HttpInboundServiceContextImpl) isc).getGRPCEndStream();
                    if (eos == 1) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "isFinished(): GRPC detected, stream not ended, return false");
                        }
                        return false;
                    } else if (eos == 2) {
                        // check that all the buffers have been drained
                        if (isc != null && isc.isIncomingMessageFullyRead()) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "HTTP Channel believes it has read all the data, checking to ensure it has given us all the data");
                            }
                            //If checkBuffer returns false, we want to return true since it means all the data has been read
                            isFinished = !checkBuffer();
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "HTTP Channel does not believe it has read all the data");
                            }
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "isFinished returning : " + isFinished);
                        }
                        return isFinished;
                    }
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "isFinished(): GRPC not detected");
                }

                if (isc != null && isc.isIncomingMessageFullyRead()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "HTTP Channel believes it has read all the data, checking to ensure it has given us all the data");
                    }
                    //If checkBuffer returns false, we want to return true since it means all the data has been read
                    isFinished = !checkBuffer();
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "HTTP Channel does not believe it has read all the data");
                    }
                }
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Encountered an exception while attempting to determine if the stream is finished : " + e);
            }

            //Return true here since we were unable to properly determine if the stream is finished. Most likely means the connection is closed
            return true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isFinished returning : " + isFinished);
        }
        return isFinished;
    }
}
