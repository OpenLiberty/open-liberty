/*******************************************************************************
 * Copyright (c) 1997, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution,  and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.h2internal.Constants.Direction;
import com.ibm.ws.http.channel.h2internal.exceptions.FlowControlException;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.exceptions.StreamClosedException;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameContinuation;
import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FrameRstStream;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundChannel;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundLink;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http2.GrpcServletServices;
import com.ibm.ws.http2.upgrade.H2Exception;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 *
 */
public class H2HttpInboundLinkWrap extends HttpInboundLink {

    Integer streamID = null;
    public H2InboundLink muxLink = null;
    H2TCPConnectionContext h2TCPConnectionContext = null;
    H2ConnectionLinkProxy h2ConnectionProxy = null;
    VirtualConnection vc = null;
    boolean isPushPromise = false;
    private Boolean isGrpc = null;

    private HashMap<String, String> pseudoHeaders = null;
    private ArrayList<H2HeaderField> headers = null;
    private int headersLength = 0;
    private int h2ContentLength = -1;

    private HttpInboundServiceContextImpl httpInboundServiceContextImpl = null;

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(H2HttpInboundLinkWrap.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public H2HttpInboundLinkWrap(HttpInboundChannel channel, VirtualConnection v, Integer id, H2InboundLink link) {
        super(channel, v);
        streamID = id;
        muxLink = link;
        vc = v;
        h2TCPConnectionContext = new H2TCPConnectionContext(streamID, muxLink, v);
        h2ConnectionProxy = new H2ConnectionLinkProxy(this);

        httpInboundServiceContextImpl = (HttpInboundServiceContextImpl) this.getChannelAccessor();

    }

    /**
     * Using the pseudo headers set on this link, check to see if there is a matching gRPC service registered with
     * the server. If a match is found, the PATH pseudo header will be updated with the correct application context.
     *
     * @return true if the request for this link maps to a registered gRPC service
     */
    public boolean setAndGetIsGrpc() {
        if (isGrpc == null) {
            if (GrpcServletServices.getServletGrpcServices() != null) {
                Map<String, GrpcServletServices.ServiceInformation> servicePaths = GrpcServletServices.getServletGrpcServices();
                if (servicePaths != null && !servicePaths.isEmpty()) {
                    isGrpc = routeGrpcServletRequest(servicePaths);
                    setIsGrpcInParentLink(isGrpc);
                } else {
                    isGrpc = false;
                }
            } else {
                isGrpc = false;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            String currentURL = this.pseudoHeaders == null ? "UNSET_HEADERS" : this.pseudoHeaders.get(HpackConstants.PATH);
            Tr.exit(tc, "setAndGetIsGrpc returning " + isGrpc + " for request path " + currentURL);
        }
        return isGrpc;
    }

    /**
     * Existing gRPC clients don't know anything about application context roots. For example, a request
     * might come in to "/helloworld.Greeter/SayHello"; so as a convenience, we will automatically append
     * the correct application context root to the request. For this example, the URL will change from
     * "/helloworld.Greeter/SayHello" -> "/app_context_root/helloworld.Greeter/SayHello"
     *
     * @return true if the request for this link maps to a gRPC service regustered in servicePaths
     */
    private boolean routeGrpcServletRequest(Map<String, GrpcServletServices.ServiceInformation> servicePaths) {
        String requestContentType = getContentType();
        if (requestContentType != null && servicePaths != null) {
            requestContentType = requestContentType.toLowerCase();
            if ("application/grpc".equalsIgnoreCase(requestContentType)) {

                String currentURL = this.pseudoHeaders.get(HpackConstants.PATH);

                String searchURL = currentURL;
                searchURL = searchURL.substring(1);
                int index = searchURL.lastIndexOf('/');
                searchURL = searchURL.substring(0, index);

                GrpcServletServices.ServiceInformation info = servicePaths.get(searchURL);
                if (info != null) {
                    String contextRoot = info.getContextRoot();
                    if (contextRoot != null && !!!"/".equals(contextRoot)) {
                        String newPath = contextRoot + currentURL;
                        this.pseudoHeaders.put(HpackConstants.PATH, newPath);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Inbound gRPC request translated from " + currentURL + " to " + newPath);
                        }
                    }
                    return true;
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Inbound gRPC request URL did not match any registered services: " + currentURL);
                }
            }
        }
        return false;
    }

    /**
     * Keep track of the content length
     */
    public void setH2ContentLength(int len) {
        h2ContentLength = len;
    }

    /**
     * Get the HTTP/2 content length
     */
    public int getH2ContentLength() {
        return h2ContentLength;
    }

    // implement the methods that HttpInboundLink will need to have changed/overridden so that it will work "as-is"

    public H2TCPConnectionContext getConnectionContext() {
        return h2TCPConnectionContext;
    }

    // called from the link proxy (H2ConnectionLinkProxy) when this http inbound link needs to close the channel/link below it
    public void closeDeviceLink(Exception e) {
        // muxLink.closeLinkWrap(streamID);
    }

    @Override
    public ConnectionLink getDeviceLink() {
        return h2ConnectionProxy;
    }

    @Override
    public void destroy(Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroying wrapped H2 inbound link: " + this + " " + getVirtualConnection());
        }
        super.destroy(e);

        vc = null;
        streamID = null;
        muxLink = null;
        h2TCPConnectionContext = null;
        h2ConnectionProxy = null;
        vc = null;
    }

    /**
     * Create Header frames corresponding to a byte array of http headers
     *
     * @param byte[]  marshalledHeaders
     * @param boolean complete
     * @return ArrayList<Frame> of FrameHeader objects containing the headers
     */
    public ArrayList<Frame> prepareHeaders(byte[] marshalledHeaders, boolean complete) {

        //Create the HeaderFrame
        //Passback the header frame

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "prepareHeaders entry: stream: marked as complete: " + complete);
        }
        int maxFrameSize = muxLink.getRemoteConnectionSettings().getMaxFrameSize();
        ArrayList<Frame> frameList = new ArrayList<Frame>();
        boolean endHeaders = true;

        /**
         * If this stream's header block is larger than the maximum frame size, we need to break it up into a header frame
         * followed by continuation frames as necessary.
         */
        if (marshalledHeaders.length > maxFrameSize) {
            int remaining = marshalledHeaders.length; // how many bytes we still need to process in this header block
            int frameSize = maxFrameSize; // the current frame size
            int position = 0; // our position in the original header block
            endHeaders = false;

            // create the first header block fragment, of size maxFrameSize, put it in a header frame,
            // then add the bytes for that frame to a list
            byte[] firstFragment = Arrays.copyOfRange(marshalledHeaders, position, frameSize);
            FrameHeaders fh = new FrameHeaders(streamID, firstFragment, complete, endHeaders);
            frameList.add(fh);
            remaining = remaining - frameSize;

            // continue creating continuation frames until we've completely copied the original header block
            while (remaining > 0) {
                position = position + frameSize;

                // figure out the frame size for this frame and set how many bytes remain
                if (remaining >= maxFrameSize) {
                    frameSize = maxFrameSize;
                } else {
                    frameSize = remaining;
                }
                remaining = remaining - frameSize;

                // copy the data for the corresponding header block fragment
                byte[] fragment = Arrays.copyOfRange(marshalledHeaders, position, position + frameSize);

                // if this is the last continuation frame we need, set endHeaders to false for this frame
                endHeaders = remaining == 0 ? true : false;
                FrameContinuation continued = new FrameContinuation(streamID, fragment, endHeaders, false, false);
                frameList.add(continued);

                // if this is the last continuation frame, we can go ahead and return the bytes for the entire collection of
                // necessary headers and continuation frames
                if (endHeaders) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "prepareHeaders exit : " + frameList);
                    }
                    return frameList;
                }
            }
        }
        // if the current headers fit into the max frame size, then return a single header frame
        FrameHeaders fh = new FrameHeaders(streamID, marshalledHeaders, complete, endHeaders);
        frameList.add(fh);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "prepareHeaders exit : " + frameList);
        }
        return frameList;
    }

    /**
     * Create Data frames to contain the http body payload
     * The buffers passed in must not exceed the http2 max frame size
     *
     * @param WsByteBuffer[]
     * @param int            length
     * @param boolean        isFinalWrite
     * @return ArrayList<Frame> of FrameData objects containing the buffered payload data
     */
    public ArrayList<Frame> prepareBody(WsByteBuffer[] wsbb, int length, boolean isFinalWrite) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "prepareBody entry : final write: " + isFinalWrite);

        }
        ArrayList<Frame> dataFrames = new ArrayList<Frame>();
        FrameData dataFrame;

        if (wsbb == null || length == 0) {
            // this empty data frame will have an end of stream flag set, signalling stream closure
            dataFrame = new FrameData(streamID, null, 0, isFinalWrite);
            dataFrames.add(dataFrame);
            return dataFrames;
        }
        boolean endStream = isFinalWrite;
        boolean lastData = false;
        int lengthWritten = 0;

        // if there's more than one buffer passed in we can't assume it will end the stream
        if (wsbb.length > 1) {
            endStream = false;
        }
        // create a data frame for every buffer in the array
        for (int i = 0; i < wsbb.length; i++) {
            WsByteBuffer b = wsbb[i];

            if (b == null) {
                continue;
            }

            lengthWritten += b.remaining();

            if (b.remaining() != 0) {
                if (lengthWritten >= length) {
                    // the current buffer meets the expected total write length,
                    // so we'll mark this as the last data frame on the stream
                    lastData = true;
                    endStream = lastData && isFinalWrite ? true : false;
                }

                dataFrame = new FrameData(streamID, b, b.remaining(), endStream);
                dataFrames.add(dataFrame);

                if (lastData) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "prepareBody exit : " + dataFrames);
                    }
                    return dataFrames;
                }
            }
        }
        return dataFrames;
    }

    /**
     * @return
     */
    public int getStreamId() {

        return streamID;
    }

    public H2HeaderTable getReadTable() {
        return this.muxLink.getReadTable();
    }

    public H2HeaderTable getWriteTable() {
        return this.muxLink.getWriteTable();
    }

    public void setPushPromise(boolean isPushPromise) {
        this.isPushPromise = isPushPromise;
    }

    public boolean isPushPromise() {
        return this.isPushPromise;
    }

    public void setHeadersLength(int len) {
        this.headersLength = len;
    }

    public int getHeadersLength() {
        return this.headersLength;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.inbound.HttpInboundLink#close(com.ibm.wsspi.channelfw.VirtualConnection, java.lang.Exception)
     */
    @Override
    public void close(VirtualConnection inVC, Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "close(vc, e) called H2InboundLink: " + this + " " + inVC);
        }

        //At this point our side should be in the close state, as we have sent out our data
        //Should probably check if the connection is closed, if so issue the destroy up the chain
        //If a non-connection-error Exception was passed in, we'll reset the stream.
        //Otherwise, call the close on the underlying muxLink so we can close the connection if everything has been closed
        //Additionally, don't close the underlying link if this is a push stream
        if (streamID == 0 || streamID % 2 == 1) {
            // if this isn't an http/2 exception, don't pass it down, since that will cause a GOAWAY to be sent immediately
            if (e == null || e instanceof Http2Exception) {
                Http2Exception h2ex = (Http2Exception) e;
                if (e != null && !h2ex.isConnectionError()) {
                    H2StreamProcessor h2sp = muxLink.getStreamProcessor(streamID);
                    if (h2sp != null && !h2sp.isStreamClosed()) {
                        try {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "close: attempting to reset stream: " + streamID + " with exception: " + e);
                            }
                            Frame reset = new FrameRstStream(streamID, h2ex.getErrorCode(), false);
                            h2sp.processNextFrame(reset, Constants.Direction.WRITING_OUT);
                        } catch (Http2Exception h2e) {
                            // if we can't write out RST frame, throw the original exception
                            if (httpInboundServiceContextImpl != null) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "close: (1)httpInboundServiceContextImpl.clear()");
                                }
                                httpInboundServiceContextImpl.clear();
                                httpInboundServiceContextImpl = null;
                            }
                            this.muxLink.close(inVC, e);
                        }
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "close: closing with exception: " + e);
                    }
                    if (httpInboundServiceContextImpl != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "close: (2)httpInboundServiceContextImpl.clear()");
                        }
                        httpInboundServiceContextImpl.clear();
                        httpInboundServiceContextImpl = null;
                    }
                    this.muxLink.close(inVC, e);
                }
            } else {
                H2StreamProcessor h2sp = muxLink.getStreamProcessor(streamID);
                if (h2sp != null && !h2sp.isStreamClosed()) {
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "close: attempting to reset stream: " + streamID);
                        }
                        int PROTOCOL_ERROR = 0x1;
                        Frame reset = new FrameRstStream(streamID, PROTOCOL_ERROR, false);
                        h2sp.processNextFrame(reset, Constants.Direction.WRITING_OUT);
                    } catch (Http2Exception h2e) {
                        // if we can't write out RST frame, throw the original exception
                        if (httpInboundServiceContextImpl != null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "close: (3)httpInboundServiceContextImpl.clear()");
                            }
                            httpInboundServiceContextImpl.clear();
                            httpInboundServiceContextImpl = null;
                        }
                        this.muxLink.close(inVC, e);
                    }
                }
                if (httpInboundServiceContextImpl != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "close: (4)httpInboundServiceContextImpl.clear()");
                    }
                    httpInboundServiceContextImpl.clear();
                    httpInboundServiceContextImpl = null;
                }
                this.muxLink.close(inVC, null);
            }
        } else { // try to send an RST_STREAM to let the client know the push promise has been canceled
            H2StreamProcessor h2sp = muxLink.getStreamProcessor(streamID);
            if (h2sp != null && !h2sp.isStreamClosed() && !h2sp.isHalfClosed()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "close: attempting to reset stream: " + streamID);
                }
                int PROTOCOL_ERROR = 0x1;
                Frame reset = new FrameRstStream(streamID, PROTOCOL_ERROR, false);
                try {
                    h2sp.processNextFrame(reset, Constants.Direction.WRITING_OUT);
                } catch (Http2Exception h2e) {
                    // don't close
                }
            }
        }
    }

    @FFDCIgnore(IOException.class)
    public void writeFramesSync(CopyOnWriteArrayList<Frame> frames) throws IOException {

        if (frames == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeFramesSync entry: # of frames: 0 - returning");
            }
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "writeFramesSync entry: # of frames: " + frames.size());
        }

        Iterator<Frame> i = frames.iterator();
        while (i.hasNext()) {
            try {
                Frame currentFrame = i.next();

                H2StreamProcessor streamProcessor = muxLink.getStreamProcessor(streamID);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeFramesSync processing frame ID: " + currentFrame.getFrameType());
                }

                if (streamProcessor != null) {
                    streamProcessor.processNextFrame(currentFrame, Direction.WRITING_OUT);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "writeFramesSync stream " + streamID + " was already closed; cannot write");
                    }
                }

            }

            catch (FlowControlException | StreamClosedException e) {
                // Throw IOE so channel code knows the write failed and can deal with the app/servlet facing output stream.
                // The FFDC will be suppressed since the IEO is rethrown by the caller, depending on the value of
                // ThrowIOEForInboundConnections
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "write failed with a FlowControlException: " + e.getErrorString());
                }
                IOException ioe = new IOException(new H2Exception(e.getMessage()));
                throw ioe;

            } catch (Http2Exception e) {
                //  send out a connection error.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRead an error occurred processing a frame: " + e.getErrorString());
                }
                muxLink.close(vc, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "writeFramesSync exit");
        }
    }

    public void setReadPseudoHeaders(HashMap<String, String> pseudoHeaders) {
        this.pseudoHeaders = pseudoHeaders;
    }

    public HashMap<String, String> getReadPseudoHeaders() {
        return this.pseudoHeaders;
    }

    public void setReadHeaders(ArrayList<H2HeaderField> headers) {
        this.headers = headers;
    }

    public ArrayList<H2HeaderField> getReadHeaders() {
        return this.headers;
    }

    public HttpRequestMessage getRequest() {
        return this.httpInboundServiceContextImpl.getRequest();
    }

    private String getContentType() {
        for (H2HeaderField header : this.headers) {
            if (header.getName().equalsIgnoreCase(HttpHeaderKeys.HDR_CONTENT_TYPE.getName())) {
                return header.getValue();
            }
        }
        return null;
    }

    public void setAndStoreNewBodyBuffer(WsByteBuffer buffer) {
        this.httpInboundServiceContextImpl.storeBuffer(buffer);
    }

    // attempt to invoke complete() on what is most likely the AsyncReadCallback.
    // this will tell it that more data is available (via the previous call to storeBuffer),
    // and the AsyncReadCallback will trigger onDataAvailable
    public void invokeAppComplete() {
        if (this.httpInboundServiceContextImpl != null && this.httpInboundServiceContextImpl.getAppReadCallback() != null) {
            this.httpInboundServiceContextImpl.getAppReadCallback().complete(vc);
        }
    }

    public void countDownFirstReadLatch(boolean force) {
        H2StreamProcessor h2sp = muxLink.getStreamProcessor(streamID);
        if (h2sp != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "calling h2sp to count down firstReadLatch; force: " + force);
            }
            h2sp.countDownFirstReadLatch(force);
        }
    }
}
