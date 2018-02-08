/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.Constants.Direction;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameContinuation;
import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundChannel;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundLink;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;

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

    private HashMap<String, String> pseudoHeaders = null;
    private ArrayList<H2HeaderField> headers = null;
    private int headersLength = 0;

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(H2HttpInboundLinkWrap.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public H2HttpInboundLinkWrap(HttpInboundChannel channel, VirtualConnection v, Integer id, H2InboundLink link) {
        super(channel, v);
        streamID = id;
        muxLink = link;
        vc = v;
        h2TCPConnectionContext = new H2TCPConnectionContext(streamID, muxLink, v);
        h2ConnectionProxy = new H2ConnectionLinkProxy(this);
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
//        // if this object is not active, then just return out
//        synchronized (this) {
//            if (!this.bIsActive) {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "Ignoring destroy on an inactive object");
//                }
//                return;
//            }
//            this.bIsActive = false;
//        }
//        // 291714 - clean up the statemap
//        getVirtualConnection().getStateMap().remove(CallbackIDs.CALLBACK_HTTPICL);
//        // 363633 - remove the buffer size value if present
//        getVirtualConnection().getStateMap().remove(HttpConstants.HTTPReadBufferSize);
//        // now clean out any other app connlinks we may have picked up
//        if (null != this.appSides) {
//            // the super.destroy without an exception just nulls out values
//            // the list of appside connlinks includes the current one
//
//            // let mux handle propagating destroys
//            // muxLink.destroyLinkWrap(streamID);    // replacing super.destroy();
//
//            //for (ConnectionReadyCallback appside : this.appSides) {
//            //    appside.destroy(e);
//            //}
//
//            this.appSides = null;
//        } else {
//            // if we only ever got one connlink above, then call the standard
//            // destroy to pass the sequence along
//
//            // let mux handle propagating destroys
//            // muxLink.destroyLinkWrap(streamID);    // replacing super.destroy(e);
//
//        }
//        this.myInterface.clear();
//        this.myInterface.destroy();
//        // these are no longer pooled, dereference now
//        this.myInterface = null;
//        this.myTSC = null;
//        this.filterExceptions = false;
//        this.numRequestsProcessed = 0;
//        this.myChannel = null;
        super.destroy(e);

        vc = null;
        streamID = null;
        muxLink = null;
        h2TCPConnectionContext = null;
        h2ConnectionProxy = null;
        vc = null;
    }

    public ArrayList<Frame> prepareHeaders(byte[] marshalledHeaders, boolean complete) {

        //Create the HeaderFrame
        //Passback the header frame

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "prepareHeaders entry: stream: marked as complete: " + complete);
        }
        int maxFrameSize = muxLink.getConnectionSettings().maxFrameSize;
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

    public ArrayList<Frame> prepareBody(byte[] body, boolean isFinalWrite) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "prepareBody entry : final write: " + isFinalWrite);
        }
        ArrayList<Frame> dataFrames = new ArrayList<Frame>();

        boolean endStream = isFinalWrite;
        int maxFrameSize = muxLink.getConnectionSettings().maxFrameSize;
        FrameData dataFrame;

        if (body == null) {
            body = new byte[0];
        }

        /**
         * If this stream's data block is larger than the maximum frame size, we need to break it up into multiple data frames
         */
        if (body.length > maxFrameSize) {
            endStream = false;
            int remaining = body.length;
            int frameSize = maxFrameSize;
            int position = 0;

            byte[] dataBytes = Arrays.copyOfRange(body, position, frameSize);
            dataFrame = new FrameData(streamID, dataBytes, endStream);
            dataFrames.add(dataFrame);
            remaining = remaining - frameSize;

            while (remaining > 0) {
                position = position + frameSize;
                if (remaining >= maxFrameSize) {
                    frameSize = maxFrameSize;
                } else {
                    frameSize = remaining;
                }
                remaining = remaining - frameSize;

                dataBytes = Arrays.copyOfRange(body, position, position + frameSize);

                boolean lastData = remaining == 0 ? true : false;
                endStream = lastData && isFinalWrite ? true : false;

                dataFrame = new FrameData(streamID, dataBytes, endStream);
                dataFrames.add(dataFrame);

                if (lastData) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "prepareBody exit : " + dataFrames);
                    }
                    return dataFrames;
                }
            }
        }
        dataFrame = new FrameData(streamID, body, isFinalWrite);
        dataFrames.add(dataFrame);;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "prepareBody exit : " + dataFrames);
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
            Tr.debug(tc, "close() called H2InboundLink: " + this + " " + inVC);
        }

        //At this point our side should be in the close state, as we have sent out our data
        //Should probably check if the connection is closed, if so issue the destroy up the chain
        //Then call the close on the underlying muxLink so we can close the connection if everything has been closed
        //Additionally, don't close the underlying link if this is a push stream
        if (streamID == 0 || streamID % 2 == 1) {
            this.muxLink.close(inVC, e);
        }
    }

    public void writeFramesSync(CopyOnWriteArrayList<Frame> frames) {
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

            } catch (Http2Exception e) {
                //  send out a connection error.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRead an error occurred processing a frame: " + e.getErrorString());
                }
                muxLink.close(vc, e);

            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeFramesSync, Exception occurred while writing the data : " + e);
                }
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

    // Initial survey of HTTPInboundLink methods
    // close                 - no override (getDeviceLink.close gets routed to the H2ConnectionLinkProxy)
    //                       -   need to verify connection closes when using wrapper, and does not return without closing by detecting "upgraded" in VC map
    // complete              - no override.
    // destroy               - Needs some override - done except for add muxLink methods
    // error                 - no override, mostly calls close
    // getChannel            - no override
    // getChannelAccessor    - no override - getDeviceLink.getChannelAccessor routed to H2ConnectionLinkProxy
    // getHTTPContext        - no override
    // getObjectFactory      - no override
    // handleDiscrimination  - no override -may need attention, but seems ok to just leave as is
    // handleGenericHNIError - no override
    // handleNewInformation  - no override
    // handleNewRequest      - no override
    // handlePipeLining      - not sure    - leave alone for now
    // init                  - no override
    // isFirstRequest        - no override
    // isHTTP2UpgradeRequest - no override - but does need to be updated and not hard-coded!
    // isPartiallyParsed     - no override
    // maxRequestsServed     - no override
    // processRequest        - no override - as long as myTSC is an H2TCPConnectionContext
    // ready                 - no override
    // sendErrorMessage(SC)  - no override
    // sendErrorMessage(T)   - no override
    // setFilterCloseExceptions - no overide
    // setPartiallyParsed    - no override
}
