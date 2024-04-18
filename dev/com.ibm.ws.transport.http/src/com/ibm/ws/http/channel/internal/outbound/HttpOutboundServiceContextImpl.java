/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpBaseMessageImpl;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.http.channel.internal.HttpResponseMessageImpl;
import com.ibm.ws.http.channel.internal.HttpServiceContextImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.BNFHeaders;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.genericbnf.exception.IllegalRequestObjectException;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.HttpBaseMessage;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.exception.BodyCompleteException;
import com.ibm.wsspi.http.channel.exception.ExpectationFailedException;
import com.ibm.wsspi.http.channel.exception.HttpInvalidMessageException;
import com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException;
import com.ibm.wsspi.http.channel.outbound.HttpAddress;
import com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * This is the interface for an outbound Http request (sendRequest and then
 * getResponse).
 *
 */
public class HttpOutboundServiceContextImpl extends HttpServiceContextImpl implements HttpOutboundServiceContext {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpOutboundServiceContextImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** State representing no read-ahead activity */
    protected static final int CALLBACK_STATE_IDLE = 0;
    /** State representing the read-ahead is still pending */
    protected static final int CALLBACK_STATE_PENDING = 1;
    /** State representing the read-ahead callback finished with an error */
    protected static final int CALLBACK_STATE_ERROR = 2;
    /** State representing the read-ahead callback finished with data */
    protected static final int CALLBACK_STATE_COMPLETE = 4;

    /** State representing no read-ahead activity */
    protected static final int READ_STATE_IDLE = 0;
    /** State representing the time has been restarted on the read */
    protected static final int READ_STATE_TIME_RESET = 1;
    /** State representing a sync read for the response has started */
    protected static final int READ_STATE_SYNC = 3;
    /** State representing an async read for the response has started */
    protected static final int READ_STATE_ASYNC = 4;

    /** link specific to this particular connection */
    private HttpOutboundLink myLink = null;
    /** When writing buffers, keep a backup list of original positions */
    private int[] positionList = null;
    /** Current state of the read-ahead callback thread */
    private int callback_state = CALLBACK_STATE_IDLE;
    /** Current state of the read-ahead read for response thread */
    private int read_state = READ_STATE_IDLE;
    /** Sync object for the synchronous reads */
    private final Object readAheadSyncer = new Object() {};
    /** Object used to synchronize getting/setting read-ahead states */
    protected Object stateSyncObject = new Object() {};
    /** Save-spot for any error found during the read-ahead callback */
    private IOException readException = null;
    /** Simple flag on whether read-ahead is enabled or not */
    private boolean bReadAheadEnabled = false;
    /** Flag on whether the immediate response read is enabled or not */
    private boolean bImmediateRead = false;
    /** Flag on whether the app channel above cares about any temp response */
    private boolean bTempResponsesUsed = false;
    /** Flag on whether early reads have been enabled */
    private boolean bEarlyReads = false;
    /** Counter for the number of async responses received so far */
    private int numResponsesReceived = 0;

    /**
     * Constructor for an HTTP outbound service context object.
     *
     * @param tsc
     * @param link
     * @param vc
     * @param hcc
     */
    public HttpOutboundServiceContextImpl(TCPConnectionContext tsc, HttpOutboundLink link, VirtualConnection vc, HttpChannelConfig hcc) {
        super();
        init(tsc, link, vc, hcc);
        setBodyRC(HttpOSCBodyReadCallback.getRef());
        this.positionList = new int[getPendingBuffers().length];
    }

    /**
     * Initialize this object.
     *
     * @param tsc
     * @param link
     * @param vc
     * @param hcc
     */
    public void init(TCPConnectionContext tsc, HttpOutboundLink link, VirtualConnection vc, HttpChannelConfig hcc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Initializing OSC: " + this);
        }
        init(tsc, hcc);
        setBodyRC(HttpOSCBodyReadCallback.getRef());
        setLink(link);
        setVC(vc);
        getVC().getStateMap().put(CallbackIDs.CALLBACK_HTTPOSC, this);
    }

    /*
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#destroy()
     */
    @Override
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroying OSC: " + this);
        }
        // 291714 - clean up the VC statemap
        getVC().getStateMap().remove(CallbackIDs.CALLBACK_HTTPOSC);
        super.destroy();
        this.myLink = null;
        this.readException = null;
    }

    /*
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#clear()
     */
    @Override
    public void clear() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Clearing OSC: " + this);
        }
        if (null != getLink()) {
            getLink().clear();
        }

        //PI81572
        if (this.getHttpConfig().shouldPurgeRemainingResponseBody()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempting to purge remaining response body");
            }
            try {
                WsByteBuffer remainingData = getResponseBodyBuffer();

                while (remainingData != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found data remaining on the response: " + remainingData);
                    }
                    remainingData.release();
                    remainingData = null;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Remaining response data released, reading for more");
                    }
                    remainingData = getResponseBodyBuffer();
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Encountered an exception while reading for remaining response data: " + e);
                }
            }
        }

        super.clear();
        setReadBuffer(null);
        // in case the read buffers still sit on the TCP layer, null them out
        getTSC().getReadInterface().setBuffers(null);
        this.callback_state = CALLBACK_STATE_IDLE;
        this.read_state = READ_STATE_IDLE;
        this.readException = null;
        this.bReadAheadEnabled = false;
        this.bImmediateRead = false;
        this.bTempResponsesUsed = false;
        this.bEarlyReads = false;
        this.numResponsesReceived = 0;
        if (getHttpConfig().runningOnZOS()) {
            // @LIDB3187-27.1 clean the statemap of the final write mark
            getVC().getStateMap().remove(HttpConstants.FINAL_WRITE_MARK);
        }
    }

    /**
     * If an error occurs during an attempted write of an outgoing request
     * message, this method will either reconnect for another try or pass the
     * error up the channel chain.
     *
     * @param inVC
     * @param ioe
     */
    protected void reConnect(VirtualConnection inVC, IOException ioe) {

        if (getLink().isReconnectAllowed()) {
            // start the reconnect
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempting reconnect: " + getLink().getVirtualConnection());
            }
            // 359362 - null out the JIT read buffers
            getTSC().getReadInterface().setBuffer(null);
            getLink().reConnectAsync(ioe);
        } else {
            callErrorCallback(inVC, ioe);
        }
    }

    /**
     * Call the error callback of the app above.
     *
     * @param inVC
     * @param ioe
     */
    void callErrorCallback(VirtualConnection inVC, IOException ioe) {
        // otherwise pass the error along to the channel above us, or close
        // the connection if nobody is above
        setPersistent(false);
        if (this.bEarlyReads && null != getAppReadCallback()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Early read failure calling error() on appside");
            }
            getAppReadCallback().error(inVC, ioe);
        } else if (null != getAppWriteCallback()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calling write.error() on appside");
            }
            getAppWriteCallback().error(inVC, ioe);
        } else {
            // nobody above us, just close the connection
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No appside, closing connection");
            }
            getLink().getDeviceLink().close(inVC, ioe);
        }
    }

    /**
     * During a synchronous write of the request message, if an IOException
     * occurs we will attempt to reconnect/rewrite the data once only. Any
     * subsequent failure (including during this method) will cause the throw
     * of the original IOException up the stack.
     *
     * @param originalExcep
     * @throws IOException
     */
    protected void reConnect(IOException originalExcep) throws IOException {

        if (getLink().isReconnectAllowed()) {
            // start the reconnect
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempting synchronous reconnect");
            }
            // 359362 - null out the JIT read buffers
            getTSC().getReadInterface().setBuffer(null);
            getLink().reConnectSync(originalExcep);
            nowReconnectedSync(originalExcep);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reconnect sync not allowed");
            }
            setPersistent(false);
            throw originalExcep;
        }
    }

    /*
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#getBuffList()
     */
    @Override
    protected WsByteBuffer[] getBuffList() {

        if (!getLink().isReconnectAllowed()) {
            // reconnects not allowed, skip the special logic below
            return super.getBuffList();
        }

        int stop = getPendingStop();
        int start = getPendingStart();
        int size = stop - start;
        if (0 == size) {
            return null;
        }
        WsByteBuffer[] buffs = getPendingBuffers();
        WsByteBuffer[] list = new WsByteBuffer[size];
        // expand the position list array if we need to
        if (this.positionList.length < buffs.length) {
            int[] newList = new int[buffs.length];
            System.arraycopy(this.positionList, 0, newList, 0, this.positionList.length);
            this.positionList = newList;
        }
        for (int x = 0, i = start; x < size; i++, x++) {
            list[x] = buffs[i];
            this.positionList[i] = buffs[i].position();
        }
        setPendingStart(stop);
        return list;
    }

    /**
     * Reset the position on all of the existing write buffers back so we can
     * resend them all, as we don't know what actually made it out before the
     * error occurred.
     *
     * @return boolean (true means success on resetting them all)
     */
    private boolean resetWriteBuffers() {

        int stop = getPendingStop();
        WsByteBuffer[] list = getPendingBuffers();
        // verify we can actually attempt the re-write
        if (null == this.positionList || null == list) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error in resetBuffers: posList: " + this.positionList + " list: " + list);
            }
            getTSC().getWriteInterface().setBuffer(null);
            return false;
        }
        // reset positions in each of the buffers
        for (int i = 0; i < stop; i++) {
            list[i].position(this.positionList[i]);
        }
        getTSC().getWriteInterface().setBuffers(list);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Reset positions on (" + stop + ") write buffers");
        }
        return true;
    }

    /**
     * Once we know we are reconnected to the target server, reset the TCP
     * buffers and start the async resend.
     */
    protected void nowReconnectedAsync() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Reconnected async for " + this);
        }
        // reset the data buffers first
        if (!resetWriteBuffers()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Resetting buffers (async) failed");
            }
            // otherwise pass the error along to the channel above us, or close
            // the connection if nobody is above
            IOException ioe = new IOException("Failed reconnect");
            if (null != getAppWriteCallback()) {
                getAppWriteCallback().error(getVC(), ioe);
            } else {
                // nobody above us, just close the connection
                getLink().getDeviceLink().close(getVC(), ioe);
            }
            return;
        }

        // now that we've reconnected, we should reset the "broken" flag. First
        // we set it to the default and then recheck the request msg
        setPersistent(true);
        updatePersistence(getRequestImpl());

        // in case we previously read any partial data, clean out the response
        resetRead();

        // attempt to write the data
        VirtualConnection rc = getTSC().getWriteInterface().write(TCPWriteRequestContext.WRITE_ALL_DATA, HttpOSCWriteCallback.getRef(), isForceAsync(), getWriteTimeout());
        if (null != rc) {
            // if we've finished writing part of a request, let the channel
            // above know that it can write more, otherwise start the read
            // for the response
            if (!isMessageSent()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling callback.complete of app channel.");
                }
                getAppWriteCallback().complete(getLink().getVirtualConnection());
            } else {
                if (isReadAheadEnabled()) {
                    // after a reconnect, there is no more read-ahead active
                    this.bReadAheadEnabled = false;
                }
                // force an async read for the response now. Avoid trying to
                // re-use any existing read buffer to skip complexity with
                // adjusting for partial reads before the reconnect.
                setupJITRead(getHttpConfig().getIncomingHdrBufferSize());
                getTSC().getReadInterface().read(1, HttpOSCReadCallback.getRef(), true, getReadTimeout());
            }
        }
    }

    /**
     * Once we've reconnected to the target server, attempt to redo the sync
     * write of the buffers. If another error happens, simply pass that back
     * up the stack.
     *
     * @param originalExcep
     * @throws IOException
     */
    protected void nowReconnectedSync(IOException originalExcep) throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Reconnected sync for " + this);
        }
        if (!resetWriteBuffers()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Resetting buffers (sync) failed");
            }
            throw originalExcep;
        }
        // now that we've reconnected, we should reset the "broken" flag. First
        // we set it to the default and then recheck the request msg
        setPersistent(true);
        updatePersistence(getRequestImpl());

        // Note: during a sync path, we don't actually enable reconnects once
        // we start reading and parsing the response (only async path)
        // in case we read any partial data, we need to clean out the response
        // resetRead();

        if (isReadAheadEnabled()) {
            // after a reconnect, there is no more read-ahead active
            this.bReadAheadEnabled = false;
        }
        try {
            getTSC().getWriteInterface().write(TCPWriteRequestContext.WRITE_ALL_DATA, getWriteTimeout());
        } catch (IOException ioe) {
            // no FFDC required
            // just set the "broken" connection flag
            setPersistent(false);
            throw ioe;
        }
    }

    /**
     * If the headers, plus optional body buffers, are being written and an
     * error occurs, the channel will attempt a reconnect to the target and
     * resend of those buffers. This will avoid the overhead of the caller
     * performing the reconnect and the outbound request being re-marshalled.
     * The rewrite will only be attempted once per send call, and only when
     * the headers are present in the buffers. If the headers have already
     * been sent, then the reconnect is impossible without the re-marshalling
     * of the request headers.
     * <p>
     * This method will let the caller prevent the reconnect/rewrite if they choose. This affects this single connection only and not any other outbound request connection.
     *
     * @return boolean (true means success)
     */
    @Override
    public boolean disallowRewrites() {
        boolean rc = getLink().disallowRewrites();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Rewrites disabled: " + rc);
        }
        return rc;
    }

    /**
     * If the headers, plus optional body buffers, are being written and an
     * error occurs, the channel will attempt a reconnect to the target and
     * resend of those buffers. This will avoid the overhead of the caller
     * performing the reconnect and the outbound request being re-marshalled.
     * The rewrite will only be attempted once per send call, and only when
     * the headers are present in the buffers. If the headers have already
     * been sent, then the reconnect is impossible without the re-marshalling
     * of the request headers.
     * <p>
     * This method will let the caller enable the reconnect/rewrites if they were previously disabled through the disallowRewrites() API. This affects this single connection only
     * and
     * will stay in effect until turned off at a later point. This will also allow the caller to override the channel configuration option for this single connection.
     *
     * @return boolean (true means success)
     */
    @Override
    public boolean allowRewrites() {
        boolean rc = getLink().allowRewrites();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Rewrites enabled: " + rc);
        }
        return rc;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#isInboundConnection
     * ()
     */
    @Override
    public boolean isInboundConnection() {
        return false;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#isCompressionAllowed
     * ()
     */
    @Override
    protected boolean isCompressionAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "isCompressionAllowed");
        }
        // 1.1 will always allow compression, but 1.0 will only work with one
        // single finishRequest call since we cannot do streaming compression
        // with the request message
        boolean rc = true;
        if (!getRequest().getVersionValue().equals(VersionValues.V11)) {
            rc = !isPartialBody();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "isCompressionAllowed: " + rc);
        }
        return rc;
    }

    /**
     * Common utility method to start the response read now, regardless of the
     * request message state.
     *
     * @param cb
     *            - app side read callback
     * @param forceQueue
     * @return VirtualConnection - non null if the final response has already
     *         arrived
     */
    private VirtualConnection startEarlyRead(InterChannelCallback cb, boolean forceQueue) {
        // disallow rewrites once we start mixing the request and temp responses
        getLink().disallowRewrites();
        setAppReadCallback(cb);

        // check for an existing final response
        if (headersParsed() && !getResponseImpl().isTemporaryStatusCode()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "earlyRead: Final response already received.");
            }
            if (forceQueue) {
                cb.complete(getVC());
                return null;
            }
            return getVC();
        }

        // otherwise, if a message is already parsed, then the assumption is
        // that the app channel above has already looked at it and is now asking
        // for the next, so clear out the previous message and start a read
        if (headersParsed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "earlyRead: Message exists, isOwner: " + isResponseOwner());
            }
            resetMsgParsedState();
            if (!isResponseOwner()) {
                // null our reference as not being owner means another SC has this
                // message already (i.e. the proxy passed it to the inbound
                // side) and then create the new object
                setMyResponse(null);
                getResponseImpl();
            } else {
                // otherwise clear the existing response
                getResponseImpl().clear();
            }
        }
        // start the read/parse cycle for the next response message
        VirtualConnection vc = parseResponseMessageAsync();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "earlyRead: return vc=" + vc);
        }
        if (null != vc && forceQueue) {
            cb.complete(getVC());
            return null;
        }
        return vc;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext#readNextResponse
     * (com.ibm.wsspi.channelfw.InterChannelCallback, boolean)
     */
    @Override
    public VirtualConnection readNextResponse(InterChannelCallback cb, boolean forceQueue) {
        if (null == cb) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "readNext: Invalid null callback as input.");
            }
            throw new NullPointerException("Null callback");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "App channel requesting next response on " + getVC());
            Tr.debug(tc, "Async forcequeue flag is " + forceQueue);
        }
        this.bTempResponsesUsed = true;
        return startEarlyRead(cb, forceQueue);
    }

    /*
     * @see com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext#
     * registerEarlyRead(com.ibm.wsspi.channelfw.InterChannelCallback)
     */
    @Override
    public void registerEarlyRead(InterChannelCallback cb) {
        // LI4335.2
        if (null == cb) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "registerEarlyRead: Invalid null callback as input.");
            }
            throw new IllegalArgumentException("Callback is null");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "App channel requesting early response read on " + getVC());
        }
        this.bEarlyReads = true;
        startEarlyRead(cb, true);
    }

    /*
     * @see com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext#
     * deregisterEarlyRead()
     */
    @Override
    public boolean deregisterEarlyRead() {
        // LI4335.2
        if (!this.bEarlyReads) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "User tried to deregister non-existent early read");
            }
            return false;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "User deregistering early read interest");
        }
        this.bEarlyReads = false;
        // not sure if any read is actually outstanding but just in case...
        super.cancelOutstandingRead();

        if (isMessageSent()) {
            // finishRequest has been called already
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error: full request already sent");
            }
            return false;
        }
        return true;
    }

    /**
     * Query whether or not the early read is active.
     *
     * @return boolean
     */
    protected boolean isEarlyRead() {
        return this.bEarlyReads;
    }

    /**
     * Method to encapsulate the act of reading, parsing, and deciding whether
     * to keep a response message (asynchronously). This will return if a read
     * is being performed asynchronously, or if a response was fully parsed and
     * handed off to the application channel. The caller of this method should
     * perform no more work after this method.
     *
     */
    public void readAsyncResponse() {

        // if read data is available or sitting on socket, then start the parse
        // now otherwise setup for a socket read
        VirtualConnection vc = null;
        if (!isReadDataAvailable() && null == getNextReadBuffer()) {
            setupReadBuffers(getHttpConfig().getIncomingHdrBufferSize(), false);
            vc = getTSC().getReadInterface().read(1, HttpOSCReadCallback.getRef(), true, getReadTimeout());
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "readAsyncResponse found existing data");
            }
            vc = getVC();
        }

        // if we had data or the read completed already, try to parse it
        if (null != vc) {
            if (null != parseResponseMessageAsync()) {
                handleParsedMessage();
            }
        }
    }

    /**
     * Method to encapsulate the act of reading, parsing, and deciding whether
     * to keep a response message. When this exits, then we have either thrown
     * an exception due to an error condition on the parsed response, or have
     * decided to keep the last response... either it was a final response or
     * a temporary one but the situation requires we keep it (reading for 100-
     * continue or reading for all temporary responses).
     *
     * @throws IOException
     */
    public void readSyncResponse() throws IOException {

        // read the next response (up to the configured max) and decide whether
        // to hand it to the application channel or continue on for the next
        do {
            parseResponseMessageSync();
            int code = getResponse().getStatusCodeAsInt();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "readSyncReponse: code is " + code);
            }
            if (isImmediateReadEnabled()) {
                // this scenario means we just need a "handshake" so continue
                return;
            }
            if (!getResponseImpl().isTemporaryStatusCode()) {
                // received a final response
                return;
            }
            // this is a temporary response (1xx)
            if (this.bTempResponsesUsed) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "readSyncResponse: using temp response");
                }
                return;
            }
            if (getRequestImpl().isExpect100Continue()) {
                if (100 != code) {
                    // if the Expectation failed, then this connection is
                    // finished, so no need to reset any values.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Expect 100-continue failed with " + code);
                    }
                    setPersistent(false);
                    throw new ExpectationFailedException(code + " " + getResponseImpl().getReasonPhrase());
                } else if (1 == numberResponsesReceived() && isHeadersSentState()) {
                    // if we've only sent the headers then the caller wants to
                    // be notified when the first 100-continue has been received
                    resetRead();
                    return;
                }
            }
            resetRead();

        } while (numberResponsesReceived() <= getHttpConfig().getLimitOnNumberOfResponses());

        // if we're out of the loop, then we hit the maximum number of temporary
        // responses allowed... send an error to the app channel
        setPersistent(false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readSyncResponse: too many responses: " + numberResponsesReceived());
        }
        throw new IOException("Max temp responses received: " + numberResponsesReceived());
    }

    // ********************************************************
    // accessing/setting the two messages for the connection
    // ********************************************************

    /**
     * Gets the request message associated with this service context.
     *
     * @return HttpRequestMessage
     */
    @Override
    public HttpRequestMessage getRequest() {
        return getRequestImpl();
    }

    /**
     * Get access to the request message impl for internal use.
     *
     * @return HttpRequestMessageImpl
     */
    final private HttpRequestMessageImpl getRequestImpl() {
        if (null == getMyRequest()) {
            setMyRequest(getObjectFactory().getRequest(this));
        }
        return getMyRequest();
    }

    /**
     * Gets the response message associated with this service context.
     *
     * @return HttpResponseMessage
     */
    @Override
    public HttpResponseMessage getResponse() {
        return getResponseImpl();
    }

    /**
     * Get access to the response message imp for internal usage.
     *
     * @return HttpResponseMessageImpl
     */
    final private HttpResponseMessageImpl getResponseImpl() {
        if (null == getMyResponse()) {
            setMyResponse(getObjectFactory().getResponse(this));
            // use limits from the request (not config)
            getMyResponse().setLimitOfTokenSize(getMyRequest().getLimitOfTokenSize());
            getMyResponse().setLimitOnNumberOfHeaders(getMyRequest().getLimitOnNumberOfHeaders());
        }
        return getMyResponse();
    }

    /**
     * Set the request message in this service context.
     *
     * @param msg
     * @throws IllegalRequestObjectException
     */
    @Override
    public void setRequest(HttpRequestMessage msg) throws IllegalRequestObjectException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setRequest: " + msg);
        }

        // null message isn't allowed
        if (null == msg) {
            throw new IllegalRequestObjectException("Illegal null message");
        }

        HttpRequestMessageImpl temp = null;
        try {
            temp = (HttpRequestMessageImpl) msg;
        } catch (ClassCastException cce) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Non msg impl passed to setRequest");
            }
            throw new IllegalRequestObjectException("Invalid message provided");
        }
        // possibly clean up any existing request object
        if (null != getMyRequest() && isRequestOwner()) {
            if (!getMyRequest().equals(temp)) {
                getMyRequest().destroy();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Caller overlaying same message");
                }
            }
        }
        // set the new object in place
        setMyRequest(temp);
        // 335554 : the message init() will overwrite the version based on config
        // and needs to be reset back to whatever we started with here.
        VersionValues version = temp.getVersionValue();
        getMyRequest().init(this);
        getMyRequest().setVersion(version);
        updatePersistence(getMyRequest());
        getResponseImpl().setHeaderChangeLimit(getMyRequest().getHeaderChangeLimit());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setRequest");
        }
    }

    /**
     * Send the headers for the outgoing request synchronously.
     *
     * @throws IOException
     *             -- if a socket error occurs
     * @throws MessageSentException
     *             -- if the headers have already been sent
     */
    @Override
    public void sendRequestHeaders() throws IOException, MessageSentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendRequestHeaders(sync)");
        }
        if (headersSent()) {
            throw new MessageSentException("Headers already sent");
        }

        setPartialBody(true);
        getLink().setAllowReconnect(true);
        try {
            sendHeaders(getRequestImpl());
        } catch (IOException e) {
            // no FFDC required
            reConnect(e);
        }
        // check to see if we need to read the response now
        if (shouldReadResponseImmediately()) {
            startResponseReadSync();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendRequestHeaders(sync)");
        }
    }

    /**
     * Send the headers for the outgoing request asynchronously.
     *
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible
     * for handling that situation in their code. A null return code means
     * that the async write is in progress.
     *
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return
     * code will always be null and the callback always used.
     *
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if the headers have already been sent
     */
    @Override
    public VirtualConnection sendRequestHeaders(InterChannelCallback callback, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendRequestHeaders(async)");
        }
        if (headersSent()) {
            throw new MessageSentException("Headers already sent");
        }

        setPartialBody(true);
        getLink().setAllowReconnect(true);
        setForceAsync(bForce);
        setAppWriteCallback(callback);
        VirtualConnection vc = sendHeaders(getRequestImpl(), HttpOSCWriteCallback.getRef());
        // Note: if forcequeue is true, then we will not get a VC object as
        // the lower layer will use the callback and return null
        if (null != vc && shouldReadResponseImmediately()) {
            // write worked already and we need to read the response headers now
            vc = startResponseRead();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendRequestHeaders(async): " + vc);
        }
        return vc;
    }

    /**
     * Send the given body buffers for the outgoing request synchronously.
     * If chunked encoding is set, then each call to this method will be
     * considered a "chunk" and encoded as such. If the message is
     * Content-Length defined, then the buffers will simply be sent out with no
     * modifications.
     *
     * Note: if headers have not already been sent, then the first call to
     * this method will send the headers.
     *
     * @param body
     * @throws IOException
     *             -- if a socket error occurs
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    @Override
    public void sendRequestBody(WsByteBuffer[] body) throws IOException, MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendRequestBody(sync)");
        }

        if (isMessageSent()) {
            throw new MessageSentException("Message already sent");
        }

        // if headers haven't been sent, set for partial body transfer
        if (!headersSent()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting partial body true");
            }
            setPartialBody(true);
        }

        getLink().setAllowReconnect(true);
        try {
            sendOutgoing(body, getRequestImpl());
        } catch (IOException e) {
            // no FFDC necessary
            reConnect(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendRequestBody(sync)");
        }
    }

    /**
     * Send the given body buffers for the outgoing request asynchronously.
     * If chunked encoding is set, then each call to this method will be
     * considered a "chunk" and encoded as such. If the message is
     * Content-Length defined, then the buffers will simply be sent out with no
     * modifications.
     *
     * Note: if headers have not already been sent, then the first call to
     * this method will send the headers.
     *
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible
     * for handling that situation in their code. A null return code means
     * that the async write is in progress.
     *
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return
     * code will always be null and the callback always used.
     *
     * @param body
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    @Override
    public VirtualConnection sendRequestBody(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendRequestBody(async)");
        }

        if (isMessageSent()) {
            throw new MessageSentException("Message already sent");
        }

        // if headers haven't been sent, then set for partial body transfer
        if (!headersSent()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting partial body true");
            }
            setPartialBody(true);
        }

        getLink().setAllowReconnect(true);
        setForceAsync(bForce);
        setAppWriteCallback(callback);
        VirtualConnection vc = sendOutgoing(body, getRequestImpl(), HttpOSCWriteCallback.getRef());
        // Note: if forcequeue is true, then we will not get a VC object as
        // the lower layer will use the callback and return null

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendRequestBody(async): " + vc);
        }
        return vc;
    }

    /**
     * Send an array of raw body buffers out synchronously. This method will
     * avoid any body modifications, such as compression or chunked-encoding.
     *
     * @param body
     * @throws IOException
     *             -- if a socket error occurs
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    @Override
    public void sendRawRequestBody(WsByteBuffer[] body) throws IOException, MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendRawRequestBody(sync)");
        }
        setRawBody(true);
        sendRequestBody(body);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendRawRequestBody(sync)");
        }
    }

    /**
     * Send an array of raw body buffers out asynchronously. This method will
     * avoid any body modifications, such as compression or chunked-encoding.
     * <p>
     * This will return null if the data is being written asynchronously and the provided callback will be used when finished. However, if the write could complete automatically,
     * then the callback will not be used and instead a non-null VC will be returned back.
     * <p>
     * The force parameter allows the caller to force the asynchronous call and to always have the callback used, thus the return code will always be null.
     *
     * @param body
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    @Override
    public VirtualConnection sendRawRequestBody(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendRawRequestBody(async)");
        }
        setRawBody(true);
        VirtualConnection vc = sendRequestBody(body, callback, bForce);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendRawRequestBody(async): " + vc);
        }
        return vc;
    }

    /**
     * Send the given body buffers for the outgoing request synchronously.
     * If chunked encoding is set, then these buffers will be considered a
     * "chunk" and encoded as such. If the message is Content-Length defined,
     * then the buffers will simply be sent out with no modifications. This
     * marks the end of the outgoing message. This method will return when the
     * response has been received and parsed.
     *
     * Note: if headers have not already been sent, then the first call to
     * this method will send the headers. If this was a chunked encoded
     * message, then the zero-length chunk is automatically appended.
     *
     * @param body
     *            (last set of buffers to send, null if no body data)
     * @throws IOException
     *             -- if a socket error occurs
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    @Override
    public void finishRequestMessage(WsByteBuffer[] body) throws IOException, MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "finishRequestMessage(sync)");
        }

        if (isMessageSent()) {
            throw new MessageSentException("Message already sent");
        }

        // if headers haven't been sent and chunked encoding is not explicitly
        // configured, then set this up for Content-Length
        if (!headersSent()) {
            if (!getRequestImpl().isChunkedEncodingSet()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Setting partial body false");
                }
                setPartialBody(false);
            }
        }

        if (getHttpConfig().runningOnZOS()) {
            // @LIDB3187-27.1
            // add this to notify the xmem channel of our final request write
            getVC().getStateMap().put(HttpConstants.FINAL_WRITE_MARK, "true");
        }
        getLink().setAllowReconnect(true);
        try {
            sendFullOutgoing(body, getRequestImpl());
        } catch (IOException e) {
            // no FFDC required
            reConnect(e);
        }

        // LI4335 - if early reads are going, then do not bother with response
        // message information here
        if (this.bEarlyReads) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "finishRequestMessage(sync): early read is active");
            }
            return;
        }

        // if the headers are already parsed, then check to see if we should
        // immediately notify the app channel or start a read for the next
        // response.
        if (headersParsed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Response headers already parsed");
            }
            if (this.bTempResponsesUsed || !getResponseImpl().isTemporaryStatusCode()) {
                // app channel wants to see all the responses
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "finishRequestMessage(sync): already parsed");
                }
                return;
            }
            // the app channel does not want to see the previously parsed msg
            resetRead();
            readSyncResponse();
        } else {
            // read for the first response
            startResponseReadSync();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "finishRequestMessage(sync)");
        }
    }

    /**
     * Send the given body buffers for the outgoing request asynchronously.
     * If chunked encoding is set, then these buffers will be considered a
     * "chunk" and encoded as such. If the message is Content-Length defined,
     * then the buffers will simply be sent out with no modifications. This
     * marks the end of the outgoing message. The callback will be called when
     * the response has been received and parsed.
     *
     * Note: if headers have not already been sent, then the first call to
     * this method will send the headers. If this was a chunked encoded
     * message, then the zero-length chunk is automatically appended.
     *
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible
     * for handling that situation in their code. A null return code means
     * that the async write is in progress.
     *
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return
     * code will always be null and the callback always used.
     *
     * @param body
     *            (last set of body data, null if no body information)
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    @Override
    public VirtualConnection finishRequestMessage(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "finishRequestMessage(async)");
        }

        if (isMessageSent()) {
            throw new MessageSentException("Message already sent");
        }

        // if headers haven't been sent and chunked encoding is not explicitly
        // configured, then set this up for Content-Length
        if (!headersSent()) {
            if (!getRequestImpl().isChunkedEncodingSet()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Setting partial body false");
                }
                setPartialBody(false);
            }
        }

        if (getHttpConfig().runningOnZOS()) {
            // @LIDB3187-27.1
            // add this to notify the xmem channel of our final request write
            getVC().getStateMap().put(HttpConstants.FINAL_WRITE_MARK, "true");
        }
        setForceAsync(bForce);
        getLink().setAllowReconnect(true);
        setAppWriteCallback(callback);
        VirtualConnection vc = sendFullOutgoing(body, getRequestImpl(), HttpOSCWriteCallback.getRef());
        if (null != vc) {
            // Note: if forcequeue is true, then we will not get a VC object as
            // the lower layer will use the callback and return null
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Request write completed immediately.");
            }
            // LI4335 - if early reads are going, then do not bother with response
            // message information here
            if (this.bEarlyReads) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "finishRequestMessage(async): early read is active");
                }
                return getVC();
            }

            // if headers are parsed and this is a final response, exit out.
            // if a temp response and using temps, return out. If temp and not
            // using temps, keep reading for final
            if (headersParsed()) {
                if (this.bTempResponsesUsed || !getResponseImpl().isTemporaryStatusCode()) {
                    // app channel wants to see all the responses
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "finishRequestMessage(async): already parsed");
                    }
                    return getVC();
                }
                // the app channel does not want to see the previously parsed msg
                resetRead();
                readAsyncResponse();
                vc = null;
            } else {
                // start the read for the first response
                vc = startResponseRead();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "finishRequestMessage(async): " + vc);
        }
        return vc;
    }

    /**
     * Finish sending the request message with the optional input body buffers.
     * These can be null if there is no more actual body data to send. This
     * method will avoid any body modification, such as compression or chunked
     * encoding and simply send the buffers as-is. If the headers have not
     * been sent yet, then they will be prepended to the input data.
     * <p>
     * This method will return when the response has been received and the headers parsed.
     *
     * @param body
     *            -- null if there is no body data
     * @throws IOException
     *             -- if a socket exception occurs
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    @Override
    public void finishRawRequestMessage(WsByteBuffer[] body) throws IOException, MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "finishRawRequestMessage(sync)");
        }
        setRawBody(true);
        finishRequestMessage(body);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "finishRawRequestMessage(sync)");
        }
    }

    /**
     * Finish sending the request message asynchronously. The body buffers can
     * be null if there is no more actual body. This method will avoid any
     * body modifications, such as compression or chunked-encoding. If the
     * headers have not been sent yet, then they will be prepended to the input
     * data.
     * <p>
     * If the asynchronous write and the read of the response can be done immediately, then this will return a VirtualConnection and the caller's callback will not be used. If this
     * returns null, then the callback will be used when the response is received and parsed.
     * <p>
     * The force flag allows the caller to force the asynchronous communication such that the callback is always used.
     *
     * @param body
     *            -- null if there is no more body data
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    @Override
    public VirtualConnection finishRawRequestMessage(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "finishRawRequestMessage(async)");
        }
        setRawBody(true);
        VirtualConnection vc = finishRequestMessage(body, callback, bForce);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "finishRawRequestMessage(async)");
        }
        return vc;
    }

    /**
     * Once we've fully written the request message, do any final checks to
     * verify it's correctness. If something was incorrect, then an exception
     * will be handed to the caller and that should be passed along to the
     * application channel above.
     *
     * @return HttpInvalidMessageException (null if it was valid)
     */
    private HttpInvalidMessageException checkRequestValidity() {
        if (shouldReadResponseImmediately()) {
            // ignore body validation as this might happen after sending only
            // the headers if 100-continue, Upgrade, immediate-read, etc.
            return null;
        }
        long len = getRequest().getContentLength();
        long num = getNumBytesWritten();
        if (HeaderStorage.NOTSET != len && num != len) {
            // content-length does not match the number of bytes sent, have to
            // close the connection since the other end won't be able to read
            // the body properly
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Request had content-length of " + len + " but sent " + num);
            }
            setPersistent(false);
            return new HttpInvalidMessageException("Request length " + len + " but sent " + num);
        }
        return null;
    }

    /**
     * Once a response has been read and parsed asynchronously, this method will
     * decide what to do next, either starting another read or notifying the
     * application channel of the parsed message.
     */
    protected void handleParsedMessage() {

        InterChannelCallback cb = null;
        // use the read callback for early reads or temp messages requested
        // LI4335 - handle early reads too
        if (this.bEarlyReads || this.bTempResponsesUsed) {
            cb = getAppReadCallback();
        } else {
            cb = getAppWriteCallback();
        }
        VirtualConnection vc = null;
        do {
            this.numResponsesReceived++;
            if (!getResponseImpl().isTemporaryStatusCode()) {
                // a final response message was received
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Notifying app channel of final response.");
                }
                cb.complete(getVC());
                return;
            }

            // received a temporary response
            int code = getResponseImpl().getStatusCodeAsInt();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received response (#" + numberResponsesReceived() + "): " + code);
            }
            if (numberResponsesReceived() > getHttpConfig().getLimitOnNumberOfResponses()) {
                // too many temp responses
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Too many async temp responses received.");
                }
                cb.error(getVC(), new IOException("Max temp responses received: " + numberResponsesReceived()));
                return;
            }
            if (this.bTempResponsesUsed) {
                // call the app channel with this new response
                cb.complete(getVC());
                return;
            }
            if (getRequestImpl().isExpect100Continue()) {
                if (100 != code) {
                    // if the Expectation failed, then this connection is finished, so
                    // no need to reset any values.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Expect 100-continue failed with " + code);
                    }
                    setPersistent(false);
                    cb.error(getVC(), new ExpectationFailedException(code + " " + getResponseImpl().getReasonPhrase()));
                    return;
                } else if (1 == numberResponsesReceived() && isHeadersSentState()) {
                    // first 100-continue only, return to app channel immed but
                    // only if sendHeaders was used (meaning they want 100-continue)
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Notifying channel of first 100-continue");
                    }
                    cb.complete(getVC());
                    return;
                }
            }
            // otherwise, this was a valid temporary response but the app channel
            // doesn't want to see it
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring temporary response...");
            }
            resetRead();
            if (setupReadBuffers(getHttpConfig().getIncomingHdrBufferSize(), false)) {
                // data is already available
                vc = getVC();
            } else {
                // read for the next bit of data
                vc = getTSC().getReadInterface().read(1, HttpOSCReadCallback.getRef(), false, getReadTimeout());
            }
            if (null != vc) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Attempting a parse of response data.");
                }
                vc = parseResponseMessageAsync();
            }
        } while (null != vc);
    }

    /**
     * Method to start an asynchronous read for the first response message on
     * this exchange. It must be only called once per request/response. This
     * contains the logic to handle request verification, read-ahead handling,
     * etc.
     *
     * @return VirtualConnection
     */
    protected VirtualConnection startResponseRead() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startResponseRead");
        }
        HttpInvalidMessageException inv = checkRequestValidity();
        if (null != inv) {
            getAppWriteCallback().error(getVC(), inv);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "startResponseRead: error");
            }
            return null;
        }
        // check whether read-ahead is enabled. Bypass this code once the
        // init() method has been called (d264854) and continue as normal
        if (isReadAheadEnabled() && READ_STATE_TIME_RESET != getReadState()) {
            // read ahead is active

            int state = CALLBACK_STATE_IDLE;
            // grab the read-ahead states in a sync block to avoid timing windows
            // with the callback kicking in at the same time
            synchronized (this.stateSyncObject) {
                state = getCallbackState();
                setReadState(READ_STATE_ASYNC);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Async response read, callback state: " + state);
            }
            switch (state) {
                case (CALLBACK_STATE_COMPLETE):
                    // read-ahead has already completed successfully, start the
                    // parse logic now
                    readAsyncResponse();
                    break;
                case (CALLBACK_STATE_ERROR):
                    // read-ahead already hit an error, trigger that path now
                    setPersistent(false);
                    reConnect(getVC(), this.readException);
                    break;
                case (CALLBACK_STATE_PENDING):
                    // read-ahead is still going on
                    break;
                default:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unexpected state (" + state + ") during async readahead");
                    }
                    setPersistent(false);
                    reConnect(getVC(), new IOException("Read-ahead state failure"));
                    break;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "startResponseRead: read-ahead");
            }
            return null;
        }

        // When we get here, then the final write of the request has finished
        // and we need to tell the app channel of that IF the temp response
        // reads have been triggered. We will not read for a response here as
        // the app channel is using readNextResponse() for that path.
        // LI4335 - early reads also avoid starting read here
        if (this.bEarlyReads || this.bTempResponsesUsed) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "startResponseRead: temp resp env");
            }
            getAppWriteCallback().complete(getVC());
            return null;
        }

        // otherwise start the read for the response now...
        getResponseImpl();
        readAsyncResponse();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startResponseRead");
        }
        return null;
    }

    /**
     * Method to clean the service context and read another response message
     * asynchronously. This will return a non-null virtual connection object
     * if the new response message is fully parsed with no async reads needed.
     * Otherwise, it will return null and a callback will be used later.
     *
     * @return VirtualConnection
     */
    VirtualConnection parseResponseMessageAsync() {

        VirtualConnection readVC = null;
        try {
            do {
                if (parseMessage()) {
                    // finished parsing the message
                    return getVC();
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Reading for more data to parse");
                }
                // configure the buffers for reading
                setupReadBuffers(getHttpConfig().getIncomingHdrBufferSize(), false);
                readVC = getTSC().getReadInterface().read(1, HttpOSCReadCallback.getRef(), false, getReadTimeout());
            } while (null != readVC);
        } catch (Exception e) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while parsing response: " + e);
            }
            setPersistent(false);
            // LI4335 - early reads also use read callback
            if (this.bEarlyReads || this.bTempResponsesUsed) {
                getAppReadCallback().error(getVC(), e);
            } else {
                getAppWriteCallback().error(getVC(), e);
            }
            return null;
        }

        // getting here means an async read is in-progress
        return null;
    }

    /**
     * Method to read for a response message synchronously. This will return
     * when the message headers are completely parsed, or throw an exception
     * if an error occurs. This method does not contain any logic on what to
     * do with the response, it just wraps the reading and parsing stage.
     *
     * @throws IOException
     */
    private void parseResponseMessageSync() throws IOException {

        // if read buffers are available, then attempt a parse otherwise go
        // into the "read data then parse" loop
        if (isReadDataAvailable()) {
            try {
                // if data is already available, don't modify the buffer
                if (parseMessage()) {
                    this.numResponsesReceived++;
                    return;
                }
            } catch (IOException ioe) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IOException while parsing response: " + ioe);
                }
                throw ioe;
            } catch (Exception e) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception while parsing response: " + e);
                }
                throw new IOException(e.getMessage(), e);
            }
        }

        // keep reading and parsing until we're done
        while (true) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reading for data to parse");
            }
            try {
                // configure the buffers for reading
                setupReadBuffers(getHttpConfig().getIncomingHdrBufferSize(), false);
                getTSC().getReadInterface().read(1, getReadTimeout());
            } catch (IOException ioe) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception while reading response: " + ioe);
                }
                reConnect(ioe);
            }
            try {
                if (parseMessage()) {
                    // finished parsing the message
                    this.numResponsesReceived++;
                    return;
                }
            } catch (IOException ioe) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IOException while parsing response: " + ioe);
                }
                throw ioe;
            } catch (Exception e) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception while parsing response: " + e);
                }
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    /**
     * Method to begin the very first read for a response message on this
     * connection. This contains logic for verifying the correctness of the
     * request message, handling read-ahead logic, etc. It must only be called
     * once per request/response exchange.
     *
     * @throws IOException
     */
    private void startResponseReadSync() throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startResponseReadSync");
        }
        HttpInvalidMessageException inv = checkRequestValidity();
        if (null != inv) {
            // invalid request message
            throw inv;
        }
        // check whether read-ahead is enabled. Bypass this code once the
        // init() method has been called (d264854) and continue as normal
        if (isReadAheadEnabled() && READ_STATE_TIME_RESET != getReadState()) {
            // read ahead is active

            int state = CALLBACK_STATE_IDLE;
            // grab the read-ahead states in a sync block to avoid timing windows
            // with the callback kicking in at the same time
            synchronized (this.stateSyncObject) {
                state = getCallbackState();
                setReadState(READ_STATE_SYNC);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Sync response read, callback state: " + state);
            }
            switch (state) {
                case (CALLBACK_STATE_COMPLETE):
                    // use regular message parsing logic below
                    break;
                case (CALLBACK_STATE_ERROR):
                    // read-ahead has already hit an error, throw that to caller
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Read-ahead reports previous failure");
                    }
                    if (null == this.readException) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Callback indicates error but no exception");
                        }
                        throw new IOException("Bad read-ahead state");
                    }
                    throw this.readException;

                case (CALLBACK_STATE_PENDING):
                    // read-ahead is still going on so we need to block here waiting
                    // on that to complete (or fail)
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Waiting for read-ahead to finish");
                    }

                    // we can't do anything except keep waiting otherwise
                    // there's going to be a read outstanding while the
                    // application channel thinks it can close the connection
                    while (CALLBACK_STATE_PENDING == getCallbackState()) {
                        try {
                            synchronized (this.readAheadSyncer) {
                                this.readAheadSyncer.wait(2 * getHttpConfig().getReadTimeout());
                            }
                        } catch (InterruptedException ie) {
                            // no FFDC required
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Read-ahead block wait timed out");
                            }
                        }
                    }
                    // check for an error from the callback
                    if (null != this.readException) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Read-ahead reports new failure");
                        }
                        throw this.readException;
                    }
                    // otherwise use the regular parsing logic below
                    break;
                default:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unexpected state (" + state + ") during sync readahead");
                    }
                    throw new IOException("Read-ahead state failure: " + state);
            }
        }

        // Make sure the response message exists
        getResponseImpl();
        readSyncResponse();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startResponseReadSync");
        }
    }

    // ********************************************************
    // Retrieving body buffer methods
    // ********************************************************

    /**
     * Utility method to check whether the upcoming read for the response body
     * is either valid at this point or even necessary.
     *
     * @return boolean -- false means there is no need to read for a body
     * @throws IOException
     *             -- if this not a valid time to get the body
     */
    private boolean checkBodyValidity() throws IOException {
        // LI4335 - allow response body reading if early reads are in place
        if (isImmediateReadEnabled() || this.bEarlyReads) {
            if (!headersParsed()) {
                // this means they are requesting body buffers prior to sending
                // the minimum request headers
                IOException ioe = new IOException("Request headers not sent yet");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Attempt to read response prior to sendRequest");
                }
                throw ioe;
            }
            // otherwise continue as normal
        } else if (!isMessageSent()) {
            // request message must be fully sent prior to reading any part of
            // the response body
            IOException ioe = new IOException("Request not finished yet");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to read response prior to finishRequest");
            }
            throw ioe;
        }

        // check to see if we should be reading for data
        return isIncomingBodyValid();
    }

    /**
     * Get all of the remaining body buffers for the response synchronously.
     * If the body is encoded or compressed, then that is removed and
     * unencoded buffers returned.
     * <p>
     * A null response means that there is no body left to read.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users responsibility to release it.
     *
     * @return WsByteBuffer[]
     * @throws IOException
     *             -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *             -- if the body was malformed
     */
    @Override
    public WsByteBuffer[] getResponseBodyBuffers() throws IOException, IllegalHttpBodyException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getResponseBodyBuffers(sync)");
        }

        if (!checkBodyValidity()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResponseBodyBuffers(sync): No body allowed");
            }
            return null;
        }

        setMultiRead(true);

        // read the buffers
        if (!isBodyComplete()) {
            try {
                readBodyBuffers(getResponseImpl(), false);
            } catch (BodyCompleteException e) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getResponseBodyBuffers(sync): BodyCompleteException");
                }
                return null;
            } catch (IllegalHttpBodyException ihe) {
                // no FFDC required
                // not possible with response messages
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected exception: " + ihe);
                }
            }
        }
        WsByteBuffer[] buffers = getAllStorageBuffers();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getResponseBodyBuffers(sync): " + ((null == buffers) ? 0 : buffers.length));
        }
        return buffers;
    }

    /**
     * Read in all of the body buffers for the response asynchronously.
     * If the body is encoded or compressed, then that is removed and
     * unencoded buffers returned. Null callbacks are not allowed and
     * will trigger a NullPointerException.
     * <p>
     * If the asynchronous request is fulfilled on the same thread, then this connection's VirtualConnection will be returned and the callback will not be used. A null return code
     * means that an asynchronous read is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous action even if it could be handled immediately. The return code will always be null and the callback
     * always used.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users responsibility to release it.
     *
     * @param callback
     * @param bForce
     * @return VirtualConnection (null if an async read is in progress,
     *         non-null if data is ready)
     * @throws BodyCompleteException
     *             -- if the entire body has already been read
     */
    @Override
    public VirtualConnection getResponseBodyBuffers(InterChannelCallback callback, boolean bForce) throws BodyCompleteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getResponseBodyBuffers(async)");
        }

        try {
            if (!checkBodyValidity() || incomingBuffersReady()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getResponseBodyBuffers(async): read not needed");
                }
                if (bForce) {
                    callback.complete(getVC());
                    return null;
                }
                return getVC();
            }
        } catch (IOException ioe) {
            // no FFDC required
            callback.error(getVC(), ioe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResponseBodyBuffers(async): error=" + ioe);
            }
            return null;
        }

        // check to see if we've already read the full body
        if (isBodyComplete()) {
            // throw new BodyCompleteException("No more body to read");
            // instead of throwing an exception, just return the VC as though
            // data is immediately ready and the caller will switch to their
            // sync block and then get a null buffer back
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResponseBodyBuffers(async): body complete");
            }
            if (bForce) {
                callback.complete(getVC());
                return null;
            }
            return getVC();
        }

        setAppReadCallback(callback);
        setForceAsync(bForce);
        setMultiRead(true);
        try {
            if (!readBodyBuffers(getResponseImpl(), true)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getResponseBodyBuffers(async): read already");
                }
                if (bForce) {
                    callback.complete(getVC());
                    return null;
                }
                return getVC();
            }
        } catch (IOException ioe) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResponseBodybuffers(async): exception: " + ioe);
            }
            callback.error(getVC(), ioe);
            return null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getResponseBodyBuffers(async): null");
        }
        return null;
    }

    /**
     * This gets the next body buffer. If the body is encoded/compressed,
     * then the encoding is removed and the "next" buffer returned.
     * <p>
     * A null response means that there is no body left to read.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users responsibility to release it.
     * <p>
     *
     * @return WsByteBuffer
     * @throws IOException
     *             -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *             -- if the body was malformed
     */
    @Override
    public WsByteBuffer getResponseBodyBuffer() throws IOException, IllegalHttpBodyException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getResponseBodyBuffer(sync)");
        }

        if (!checkBodyValidity()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResponseBodyBuffer(sync): No body allowed");
            }
            return null;
        }

        setMultiRead(false);
        // check for any already read buffer
        WsByteBuffer retVal = getNextBuffer();
        if (null == retVal && !isBodyComplete()) {
            // read a buffer
            try {
                readBodyBuffer(getResponseImpl(), false);
            } catch (BodyCompleteException e) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getResponseBodyBuffer(sync): BodyCompleteException");
                }
                return null;
            } catch (IllegalHttpBodyException ihe) {
                // no FFDC required
                // not possible with response messages
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected exception: " + ihe);
                }
            }
            retVal = getNextBuffer();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getResponseBodyBuffer(sync): " + retVal);
        }
        return retVal;
    }

    /**
     * This gets the next body buffer asynchronously. If the body is encoded
     * or compressed, then the encoding is removed and the "next" buffer
     * returned. Null callbacks are not allowed and will trigger a
     * NullPointerException.
     * <p>
     * If the asynchronous request is fulfilled on the same thread, then this connection's VirtualConnection will be returned and the callback will not be used. A null return code
     * means that an asynchronous read is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous action even if it could be handled immediately. The return code will always be null and the callback
     * always used.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users responsibility to release it.
     *
     * @param callback
     * @param bForce
     * @return VirtualConnection (null if an async read is in progress,
     *         non-null if data is ready)
     * @throws BodyCompleteException
     *             -- if the entire body has already been read
     */
    @Override
    public VirtualConnection getResponseBodyBuffer(InterChannelCallback callback, boolean bForce) throws BodyCompleteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getResponseBodyBuffer(async)");
        }

        try {
            if (!checkBodyValidity() || incomingBuffersReady()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getResponseBodyBuffer(async): read not needed");
                }
                if (bForce) {
                    callback.complete(getVC());
                    return null;
                }
                return getVC();
            }
        } catch (IOException ioe) {
            // no FFDC required
            callback.error(getVC(), ioe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResponseBodyBuffer(async): error " + ioe);
            }
            return null;
        }

        if (isBodyComplete()) {
            // throw new BodyCompleteException("No more body to read");
            // instead of throwing an exception, just return the VC as though
            // data is immediately ready and the caller will switch to their
            // sync block and then get a null buffer back
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResponseBodyBuffer(async): body complete");
            }
            if (bForce) {
                callback.complete(getVC());
                return null;
            }
            return getVC();
        }

        setAppReadCallback(callback);
        setForceAsync(bForce);
        setMultiRead(false);
        try {
            if (!readBodyBuffer(getResponseImpl(), true)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getResponseBodyBuffer(async): read finished");
                }
                if (bForce) {
                    callback.complete(getVC());
                    return null;
                }
                return getVC();
            }
        } catch (IOException ioe) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getResponseBodyBuffer(async): exception: " + ioe);
            }
            callback.error(getVC(), ioe);
            return null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getResponseBodyBuffer(async): null");
        }
        return null;
    }

    /**
     * Retrieve the next buffer of the response message's body. This will give
     * the buffer without any modifications, avoiding decompression or chunked
     * encoding removal.
     * <p>
     * A null buffer will be returned if there is no more data to get.
     * <p>
     * The caller is responsible for releasing these buffers when complete as the HTTP Channel does not keep track of them.
     *
     * @return WsByteBuffer
     * @throws IOException
     *             -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *             -- if the body was malformed
     */
    @Override
    public WsByteBuffer getRawResponseBodyBuffer() throws IOException, IllegalHttpBodyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRawResponseBodyBuffer(sync)");
        }
        setRawBody(true);
        WsByteBuffer buffer = getResponseBodyBuffer();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRawResponseBodyBuffer(sync): " + buffer);
        }
        return buffer;
    }

    /**
     * Retrieve all remaining buffers of the response message's body. This will
     * give the buffers without any modifications, avoiding decompression or
     * chunked encoding removal.
     * <p>
     * A null buffer array will be returned if there is no more data to get.
     * <p>
     * The caller is responsible for releasing these buffers when complete as the HTTP Channel does not keep track of them.
     *
     * @return WsByteBuffer[]
     * @throws IOException
     *             -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *             -- if the body was malformed
     */
    @Override
    public WsByteBuffer[] getRawResponseBodyBuffers() throws IOException, IllegalHttpBodyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRawResponseBodyBuffers(sync)");
        }
        setRawBody(true);
        WsByteBuffer[] list = getResponseBodyBuffers();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRawResponseBodyBuffers(sync): " + list);
        }
        return list;
    }

    /**
     * Retrieve the next buffer of the body asynchronously. This will avoid any
     * body modifications, such as decompression or removal of chunked-encoding
     * markers.
     * <p>
     * If the read can be performed immediately, then a VirtualConnection will be returned and the provided callback will not be used. If the read is being done asychronously, then
     * null will be returned and the callback used when complete. The force input flag allows the caller to force the asynchronous read to always occur, and thus the callback to
     * always be used.
     * <p>
     * The caller is responsible for releasing these buffers when finished with them as the HTTP Channel keeps no reference to them.
     *
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws BodyCompleteException
     *             -- if the entire body has already been read
     */
    @Override
    public VirtualConnection getRawResponseBodyBuffer(InterChannelCallback callback, boolean bForce) throws BodyCompleteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRawResponseBodyBuffer(async)");
        }
        setRawBody(true);
        VirtualConnection vc = getResponseBodyBuffer(callback, bForce);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRawResponseBodyBuffer(async): " + vc);
        }
        return vc;
    }

    /**
     * Retrieve any remaining buffers of the body asynchronously. This will
     * avoid any body modifications, such as decompression or removal of
     * chunked-encoding markers.
     * <p>
     * If the read can be performed immediately, then a VirtualConnection will be returned and the provided callback will not be used. If the read is being done asychronously, then
     * null will be returned and the callback used when complete. The force input flag allows the caller to force the asynchronous read to always occur, and thus the callback to
     * always be used.
     * <p>
     * The caller is responsible for releasing these buffers when finished with them as the HTTP Channel keeps no reference to them.
     *
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws BodyCompleteException
     *             -- if the entire body has already been read
     */
    @Override
    public VirtualConnection getRawResponseBodyBuffers(InterChannelCallback callback, boolean bForce) throws BodyCompleteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRawResponseBodyBuffers(async)");
        }
        setRawBody(true);
        VirtualConnection vc = getResponseBodyBuffers(callback, bForce);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRawResponseBodyBuffers(async): " + vc);
        }
        return vc;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#readUntilEnd(boolean
     * )
     */
    @Override
    @SuppressWarnings("unused")
    protected boolean readUntilEnd(boolean async) throws IllegalHttpBodyException, BodyCompleteException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Called readUntilEnd");
        }

        if (!isReadDataAvailable()) {
            // attempt to read whatever data is available
            try {
                if (fillABuffer(1, async, false)) {
                    return true;
                }
            } catch (IOException ioe) {
                // no FFDC required as this should not be thrown
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected exception: " + ioe);
                }
                return false;
            }
            if (isBodyComplete()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "End of body found in new buffer");
                }
                return false;
            }
        }

        int position = getReadBuffer().position();
        int limit = getReadBuffer().limit();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Current body buffer " + getReadBuffer());
        }

        if (0 == position && limit == getReadBuffer().capacity()) {
            // this is a full buff we can return
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Return a full buffer");
            }
            storeTempBuffer(returnLastBuffer());
            setReadBuffer(null);
        } else {
            // slice up a return
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Slice the return buffer");
            }
            // slice from the position set above
            storeTempBuffer(getReadBuffer().slice());
            getReadBuffer().position(limit);
        }
        return false;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#getMessageBeingParsed
     * ()
     */
    @Override
    final protected HttpBaseMessageImpl getMessageBeingParsed() {
        return getResponseImpl();
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#getMessageBeingSent
     * ()
     */
    @Override
    final protected HttpBaseMessageImpl getMessageBeingSent() {
        return getRequestImpl();
    }
    
    @Override
    protected HttpBaseMessage getCurrentMessage() {
        return getRequest();
    }

    /**
     * Query the HTTP outbound connection link associated with this service
     * context
     *
     * @return HttpOutboundLink
     */
    @Override
    final public HttpOutboundLink getLink() {
        return this.myLink;
    }

    /**
     * Set the conn link to the input object
     *
     * @param link
     */
    final private void setLink(HttpOutboundLink link) {
        this.myLink = link;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#reconnectAllowed()
     */
    @Override
    protected boolean reconnectAllowed() {
        return getLink().isReconnectAllowed();
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#getObjectFactory()
     */
    @Override
    public HttpObjectFactory getObjectFactory() {
        return (null == getLink()) ? null : getLink().getObjectFactory();
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#createChunkHeader
     * (byte[])
     */
    @Override
    protected WsByteBuffer createChunkHeader(byte[] length) {
        if (!getLink().isReconnectAllowed()) {
            // use the "shared" chunk header object
            return super.createChunkHeader(length);
        }

        // must make a unique buffer to avoid data corruption during a
        // reconnect pass
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "OSC creating chunk length buffer");
        }
        WsByteBuffer header = allocateBuffer(32);
        header.put(length);
        header.put(BNFHeaders.EOL);
        header.flip();
        return header;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#createChunkTrailer
     * ()
     */
    @Override
    protected WsByteBuffer createChunkTrailer() {
        if (!getLink().isReconnectAllowed()) {
            // use the "shared" chunk trailer object
            return super.createChunkTrailer();
        }

        // if reconnects are allowed, then these buffers must be unique
        // per chunk, otherwise we get data corruption
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "OSC creating chunk trailer");
        }
        WsByteBuffer buffer = allocateBuffer(32);
        buffer.put(CHUNK_TRAILER_DATA);
        buffer.flip();
        return buffer;
    }

    /*
     * @see com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext#
     * registerReadAhead(com.ibm.wsspi.channelfw.InterChannelCallback, int)
     */
    @Override
    public boolean registerReadAhead(InterChannelCallback cb, int timeout) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Read-ahead requested (timeout=" + timeout + ") " + getVC());
        }
        if (-1 > timeout) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid timeout used by caller.");
            }
            return false;
        }
        int state;
        int myState;
        // grab the read-ahead states in a sync block to avoid timing windows
        // with the callback kicking in at the same time
        synchronized (this.stateSyncObject) {
            state = getCallbackState();
            myState = getReadState();
            setCallbackState(CALLBACK_STATE_PENDING, null);
        }
        if (CALLBACK_STATE_IDLE != state) {
            // invalid state to start this read-ahead from
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Callback state is invalid: " + state);
            }
            return false;
        }
        if (READ_STATE_IDLE != myState) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Read for response state is invalid: " + myState);
            }
            return false;
        }

        // start the read now
        this.bReadAheadEnabled = true;
        int time = (-1 == timeout) ? getReadTimeout() : timeout;
        setAppReadCallback(cb);
        setupJITRead(getHttpConfig().getIncomingHdrBufferSize());
        getTSC().getReadInterface().read(1, HttpOSCReadAhead.getRef(), true, time);
        return true;
    }

    /*
     * @see com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext#init()
     */
    @Override
    public boolean init() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "App channel called init: " + this);
        }
        int cbState = CALLBACK_STATE_IDLE;
        int myState = READ_STATE_IDLE;
        // grab the read-ahead states in a sync block to avoid timing windows
        // with the callback kicking in at the same time
        if (isReadAheadEnabled()) {
            getReadCancel().init();
            if (!super.cancelOutstandingRead()) {
                // failed to start (not supported at TCP layer perhaps)
                getReadCancel().clear();
                return false;
            } else if (!getReadCancel().block(2000L)) {
                // failed to succeed on the cancel attempt
                return false;
            }
            synchronized (this.stateSyncObject) {
                cbState = getCallbackState();
                myState = getReadState();
                setReadState(READ_STATE_TIME_RESET);
            }
        }
        if (CALLBACK_STATE_IDLE == cbState) {
            // no read ahead involved here
            return true;
        }
        if (CALLBACK_STATE_PENDING != cbState) {
            // read-ahead callback has already been triggered, this must fail
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Callback state indicates error: " + cbState);
            }
            return false;
        }
        if (READ_STATE_IDLE != myState) {
            // read-for-response thread state isn't valid for this method
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Read for response state indicates error: " + myState);
            }
            return false;
        }
        return true;
    }

    /**
     * Query what the current state is of the read-ahead callback.
     *
     * @return int
     */
    protected int getCallbackState() {
        return this.callback_state;
    }

    /**
     * Set the state of the read-ahead callback to the input value, along with
     * saving the exception (if non-null) in an error situation.
     *
     * @param state
     * @param ioe
     */
    protected void setCallbackState(int state, IOException ioe) {
        this.callback_state = state;
        this.readException = ioe;
    }

    /**
     * Query what the current state is of the read for response thread.
     *
     * @return int
     */
    protected int getReadState() {
        return this.read_state;
    }

    /**
     * Set the state of the read for response thread to the input value.
     *
     * @param state
     */
    protected void setReadState(int state) {
        this.read_state = state;
    }

    /**
     * Method used by the read-ahead callback thread to notify this service
     * context that the read has completed.
     *
     */
    protected void wakeupReadAhead() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Received synchronous read-ahead wake-up call.");
        }
        synchronized (this.readAheadSyncer) {
            this.readAheadSyncer.notify();
        }
    }

    /**
     * Query the number of temporary responses received on this connection so
     * far.
     *
     * @return int
     */
    private int numberResponsesReceived() {
        return this.numResponsesReceived;
    }

    /**
     * Query whether or not the read-ahead logic is currently enabled.
     *
     * @return boolean
     */
    private boolean isReadAheadEnabled() {
        return this.bReadAheadEnabled;
    }

    /**
     * Query whether or not the immediate read for the response is enabled.
     *
     * @return boolean
     */
    protected boolean isImmediateReadEnabled() {
        return this.bImmediateRead;
    }

    /*
     * @see com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext#
     * enableImmediateResponseRead()
     */
    @Override
    public boolean enableImmediateResponseRead() {
        if (headersSent()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Request hdrs already sent, too late for immediate read");
            }
            return false;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Immediate response read requested: " + getVC());
        }
        this.bImmediateRead = true;
        return true;
    }

    /**
     * Once the requests headers have been written out, this utility method
     * will inform the caller whether a read for the response message should
     * start.
     *
     * @return boolean
     */
    protected boolean shouldReadResponseImmediately() {
        return (isImmediateReadEnabled() || getRequestImpl().isExpect100Continue() || getRequestImpl().containsHeader(HttpHeaderKeys.HDR_UPGRADE));
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#cancelOutstandingRead
     * ()
     */
    @Override
    public boolean cancelOutstandingRead() {
        // @325312 - add cancel apis. Turn off rewrite logic first.
        disallowRewrites();
        return super.cancelOutstandingRead();
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#cancelOutstandingWrite
     * ()
     */
    @Override
    public boolean cancelOutstandingWrite() {
        // @325312 - add cancel apis. Turn off rewrite logic first.
        disallowRewrites();
        return super.cancelOutstandingWrite();
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext#getTargetAddress
     * ()
     */
    @Override
    public HttpAddress getTargetAddress() {
        // 335003 - required for request message to do header compliance with
        // the interface and not the impl.
        return this.myLink.getTargetAddress();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#setStartTime()
     */
    @Override
    public void setStartTime() {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#getStartNanoTime()
     */
    @Override
    public long getStartNanoTime() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#resetStartTime()
     */
    @Override
    public void resetStartTime() {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext#getRemainingData()
     */
    @Override
    public WsByteBuffer getRemainingData() {

        if (!isReadDataAvailable()) {
            return null;
        }

        WsByteBuffer retBuf = null;

        int position = getReadBuffer().position();
        int limit = getReadBuffer().limit();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Current body buffer " + getReadBuffer());
        }

        if (0 == position && limit == getReadBuffer().capacity()) {
            // this is a full buff we can return
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Return a full buffer");
            }
            retBuf = returnLastBuffer();

        } else {
            // slice up a return
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Slice the return buffer");
            }
            // slice from the position set above
            retBuf = getReadBuffer().slice();

        }

        return retBuf;

    }
}
