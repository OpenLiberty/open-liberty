/*******************************************************************************
 * Copyright (c) 2004, 2023 IBM Corporation and others.
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.h2internal.H2HttpInboundLinkWrap;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpBaseMessageImpl;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.http.channel.internal.HttpResponseMessageImpl;
import com.ibm.ws.http.channel.internal.HttpServiceContextImpl;
import com.ibm.ws.http.channel.internal.values.ReturnCodes;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.message.NettyResponseMessage;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.genericbnf.exception.IllegalResponseObjectException;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.HttpBaseMessage;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.HttpPlatformUtils;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.error.HttpError;
import com.ibm.wsspi.http.channel.exception.BodyCompleteException;
import com.ibm.wsspi.http.channel.exception.HttpInvalidMessageException;
import com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException;
import com.ibm.wsspi.http.channel.exception.MessageTooLargeException;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.TransferEncodingValues;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.http.logging.DebugLog;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;

/**
 * Service context specific to an inbound HTTP message.
 *
 */
public class HttpInboundServiceContextImpl extends HttpServiceContextImpl implements HttpInboundServiceContext {
    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpInboundServiceContextImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Static class name reference for FFDCs */
    private static final String CLASS_NAME = HttpInboundServiceContextImpl.class.getName();

    /** Identifier string used while sending an error */
    private static final String HTTP_ERROR_IDENTIFIER = "Http Error ID";

    /** Link associated with this SC instance */
    private HttpInboundLink myLink = null;
    /** Flag on whether the request is a "large" one over the standard limit */
    private boolean bContainsLargeMessage = false;
    /** Start time of the request (when access logging is enabled) */
    private long startTime = 0;
    /** Remote user of the request, as set by WebContainer */
    private String remoteUser = "";

    private boolean forwardedHeaderInitialized = false;
    private int forwardedRemotePort = -1;
    private String forwardedRemoteAddress = null;
    private String forwardedProto = null;
    private String forwardedHost = null;
    private boolean suppress0ByteChunk = false;
    private long bytesWritten;

    private ChannelHandlerContext nettyContext;
    private FullHttpRequest nettyRequest;
    private io.netty.handler.codec.http.HttpResponse nettyResponse;
    private HttpResponseMessage response;

    /**
     * Constructor for an HTTP inbound service context object.
     *
     * @param tsc
     * @param link
     * @param vc
     * @param hcc
     */
    public HttpInboundServiceContextImpl(TCPConnectionContext tsc, HttpInboundLink link, VirtualConnection vc, HttpChannelConfig hcc) {
        super();
        init(tsc, link, vc, hcc);
    }

    public HttpInboundServiceContextImpl(ChannelHandlerContext context) {
        super();
        nettyContext = context;

        super.init(context);

        boolean isSecure = context.channel().attr(NettyHttpConstants.IS_SECURE).get();
        this.setHeadersParsed();

        if (isSecure) {
            // getRequest().setScheme(SchemeValues.HTTPS);
        } else {
            // getRequest().setScheme(SchemeValues.HTTP);
        }

    }

    /**
     * Initialize this object.
     *
     * @param tsc
     * @param link
     * @param vc
     * @param hcc
     */
    public void init(TCPConnectionContext tsc, ConnectionLink link, VirtualConnection vc, HttpChannelConfig hcc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Initializing ISC: " + this);
        }
        super.init(tsc, hcc);
        setBodyRC(HttpISCBodyReadCallback.getRef());
        this.myLink = (HttpInboundLink) link;
        if (link instanceof H2HttpInboundLinkWrap) {
            super.setH2Connection(true);
            super.setPushPromise(((H2HttpInboundLinkWrap) link).isPushPromise());
        }
        setVC(vc);
        vc.getStateMap().put(CallbackIDs.CALLBACK_HTTPISC, this);
        // during discrimination, this is skipped so do it now
        getRequestImpl().initScheme();
    }

    public void reinit(TCPConnectionContext tcc, VirtualConnection vc, HttpInboundLink wrapper) {
        setVC(vc);
        vc.getStateMap().put(CallbackIDs.CALLBACK_HTTPISC, this);
        this.myLink = wrapper;
        if (wrapper instanceof H2HttpInboundLinkWrap) {
            super.setH2Connection(true);
            super.setPushPromise(((H2HttpInboundLinkWrap) wrapper).isPushPromise());
        }
        super.reinit(tcc);
    }

    @Override
    public void setNettyRequest(FullHttpRequest request) {
        this.nettyRequest = request;
        super.setNettyRequest(request);
    }

    @Override
    public void setNettyResponse(HttpResponse response) {
        this.nettyResponse = response;
        super.setNettyResponse(response);
    }

    public FullHttpRequest getNettyRequest() {
        return this.nettyRequest;
    }

    public HttpResponse getNettyResponse() {
        return this.nettyResponse;
    }

    public ChannelHandlerContext getNettyContext() {
        return this.nettyContext;
    }

    /*
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#destroy()
     */
    @Override
    public void destroy() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroying ISC: " + this);
        }
        // 291714 - clean up the statemap
        getVC().getStateMap().remove(CallbackIDs.CALLBACK_HTTPISC);
        getVC().getStateMap().remove(HTTP_ERROR_IDENTIFIER);
        this.myLink = null;

        super.destroy();
    }

    /*
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#clear()
     */
    @Override
    public void clear() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Clearing ISC: " + this);
        }
        super.clear();
        this.bContainsLargeMessage = false;
        this.remoteUser = "";
        this.forwardedHeaderInitialized = false;
        this.forwardedHost = null;
        this.forwardedProto = null;
        this.forwardedRemoteAddress = null;
        this.forwardedRemotePort = -1;
        this.suppress0ByteChunk = false;

        if (getHttpConfig().runningOnZOS()) {
            // @311734 - clean the statemap of the final write mark
            getVC().getStateMap().remove(HttpConstants.FINAL_WRITE_MARK);
        }
    }

    /*
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#updatePersistence(com.ibm.ws.http.channel.internal.HttpBaseMessageImpl)
     */
    @Override
    protected void updatePersistence(HttpBaseMessageImpl msg) {

        // see if the maximum number of inbound requests have been served
        // otherwise check the msg headers
        if (this.myLink.maxRequestsServed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Max requests already processed on this connection");
            }
            setPersistent(false);
        } else if (getResponse().getStatusCode().isErrorCode()) {
            // 346196.4 - error code turns off persistence
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error status code disabling persistence.");
            }
            setPersistent(false);
        } else {
            super.updatePersistence(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updatePersistence(inbound) updated: " + isPersistent());
        }
    }

    /**
     * When a body is not allowed to be sent on an outgoing message, certain
     * headers need modification -- except for an outgoing Response to a HEAD
     * request.
     *
     * @param msg
     */
    protected void updateBodyLengthHeaders(HttpBaseMessage msg) {

        // don't change headers if this was in response to a HEAD request
        if (!getRequestImpl().getMethodValue().equals(MethodValues.HEAD)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Body not valid on response, fixing headers");
            }
            if (0 < msg.getContentLength()) {
                // 366388
                msg.removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);
            }
            msg.setTransferEncoding(TransferEncodingValues.NOTSET);
        }
    }

    /**
     * Skip whitespace found in the input data starting at the given index. It
     * will return an index value that points to the first non-space byte found
     * or end of data if it ran out.
     *
     * @param data
     * @param start
     * @return int
     */
    private int skipWhiteSpace(byte[] data, int start) {
        int index = start;
        while (index < data.length && (' ' == data[index] || '\t' == data[index])) {
            index++;
        }
        return index;
    }

    /**
     * Parse the qvalue out from a stream of data.
     *
     * @param data
     * @param start
     * @return ReturnCodes
     */
    private ReturnCodes parseQValue(byte[] data, int start) {
        // 414433 - redo the qvalue parsing to handle more error conditions

        ReturnCodes rc = new ReturnCodes(false);
        int len = data.length;
        int index = skipWhiteSpace(data, start);
        // we should be pointing at "q=X"
        // technically it's supposed to be just 'q', but check uppercase too
        if (index >= len || ('q' != data[index] && 'Q' != data[index])) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Non qvalue found");
            }
            rc.setIntValue(index);
            return rc;
        }
        index++;
        index = skipWhiteSpace(data, index);
        if (index >= len || HttpBaseMessage.EQUALS != data[index]) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Qvalue missing equals");
            }
            rc.setIntValue(index);
            return rc;
        }
        index++;
        index = skipWhiteSpace(data, index);
        // now we should be pointing at a float value
        if (index < len && ('1' == data[index] || '0' == data[index])) {
            // default the "approval" flag based on leading digit
            boolean leadingOne = ('1' == data[index]);
            rc.setBooleanValue(leadingOne);
            if (++index >= len || ',' == data[index]) {
                // reached end of data, single digit
            } else if (' ' == data[index] || '\t' == data[index]) {
                // whitespace, scan for the comma
                index = skipWhiteSpace(data, index);
                if (index < len && ',' != data[index]) {
                    // nonwhitespace found [q=1 q] invalid
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid char after trailing whitespace (1) ["
                                     + data[index] + "]");
                    }
                    rc.setBooleanValue(false);
                }
            } else if ('.' != data[index]) {
                // required to be a period for the float
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Non-period found after leading digit");
                }
                rc.setBooleanValue(false);
            } else {
                // now we must only have up to 3 digits, or 1.000
                // stop at eol, whitespace, or comma
                int numDigits = 0;
                while (++index < len && ',' != data[index]) {
                    if ('0' <= data[index] && '9' >= data[index]) {
                        numDigits++;
                        if ('0' != data[index]) {
                            if (leadingOne) {
                                // 1.000 is the only valid 1* value
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Non-zero after a leading one");
                                }
                                rc.setBooleanValue(false);
                                break; // out of while
                            }
                            rc.setBooleanValue(true);
                        }
                    } else {
                        // non-digit found
                        break; // out of while
                    }
                } // while not end of data
                if (3 < numDigits) {
                    // too many digits
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Too many digits in float (" + numDigits + ")");
                    }
                    rc.setBooleanValue(false);
                } else if (index >= len || ',' == data[index]) {
                    // end of qvalue found
                } else if (' ' == data[index] || '\t' == data[index]) {
                    // whitespace, scan for the comma
                    index = skipWhiteSpace(data, ++index);
                    if (index < len && ',' != data[index]) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Invalid char after trailing whitespace (2) ["
                                         + data[index] + "]");
                        }
                        rc.setBooleanValue(false);
                    }
                } else {
                    // invalid character found
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid char after number [" + data[index] + "]");
                    }
                    rc.setBooleanValue(false);
                }
            }
        } // starts with a digit

        // index is pointing to the last char looked at
        rc.setIntValue(index);
        return rc;
    }

    /*
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#isCompressionAllowed()
     */
    @Override
    protected boolean isCompressionAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "isCompressionAllowed");
        }

        boolean rc = false;

        if (getRequest().getHeader(HttpHeaderKeys.HDR_ACCEPT_ENCODING).asString() == null) {
            //If no accept-encoding field is present in a request, the server MAY assume
            //that the client will accept any content coding.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "isCompressionAllowed(1)", Boolean.TRUE);
            }
            return true;
        }

        if (acceptableEncodings.containsKey(this.preferredEncoding)) {
            //found encoding, verify if value is non-zero
            rc = (acceptableEncodings.get(this.preferredEncoding) > 0f);
        }

        else if (ContentEncodingValues.GZIP.getName().equals(preferredEncoding)) {
            //gzip and x-gzip are functionally the same
            if (acceptableEncodings.containsKey(ContentEncodingValues.XGZIP.getName())) {
                rc = (acceptableEncodings.get(ContentEncodingValues.XGZIP.getName()) > 0f);
            }
        }

        else if (ContentEncodingValues.IDENTITY.getName().equals(preferredEncoding)) {
            //Identity is always acceptable unless specifically set to 0. Since it
            //wasn't found in acceptableEncodings, return true

            rc = true;
        }

        else {
            //The special symbol "*" in an Accept-Encoding field matches any available
            //content-coding not explicitly listed in the header field.

            rc = this.bStarEncodingParsed;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "isCompressionAllowed(2): " + rc);
        }
        return rc;
    }

    /*
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#isIncomingMessageFullyRead()
     */
    @Override
    public boolean isIncomingMessageFullyRead() {

        // if we haven't already read the body, check to see if
        // we expect one to exist
        if (!isBodyComplete()) {
            // this doesn't give a definitive answer but it's the best
            // we can do without actually trying to read the body
            // if a body is expected then return false as we haven't read
            // it yet... for an inbound request use the "expected" check
            // instead of "allowed" to avoid non-length delimited request
            // bodies (which are just going to throw an exception)
            // @314871 - use the cached value
            return !super.isIncomingBodyExpected();
            // return !getMessageBeingParsed().isBodyExpected();
        }

        // if body flag is true, then another channel has already
        // read the entire body (thus the message is fully read)
        return true;
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
    protected HttpRequestMessageImpl getRequestImpl() {
        if (null == getMyRequest()) {
            if (getHttpConfig().useNetty()) {

                setMyRequest(new HttpRequestMessageImpl());
                getMyRequest().init(this);

            } else {
                setMyRequest(getObjectFactory().getRequest(this));
                getMyRequest().setHeaderChangeLimit(getHttpConfig().getHeaderChangeLimit());

            }
        }
        setStartTime();
        HttpRequestMessageImpl req = getMyRequest();

        // if applicable set the HTTP/2 specific content length
        //TODO: Netty H2
        if (!getHttpConfig().useNetty() && myLink instanceof H2HttpInboundLinkWrap) {
            int len = ((H2HttpInboundLinkWrap) myLink).getH2ContentLength();
            if (len != -1) {
                req.setContentLength(len);
            }
        }
        return req;
    }

    /**
     * Gets the response message associated with this service context.
     *
     * @return HttpResponseMessage
     */
    @Override
    public HttpResponseMessage getResponse() {
        if (Objects.isNull(this.response) && Objects.nonNull(this.nettyContext)) {
            this.response = new NettyResponseMessage(this.nettyResponse, this);
        }

        return Objects.nonNull(this.nettyContext) ? this.response : getResponseImpl();
    }

    /**
     * Get access to the response message imp for internal usage.
     *
     * @return HttpResponseMessageImpl
     */
    final protected HttpResponseMessageImpl getResponseImpl() {
        if (Objects.isNull(getMyResponse())) {
            if (Objects.nonNull(nettyContext)) {

                throw new UnsupportedOperationException("HttpResponseMessageImpl is not valid in Netty context, use NettyResponseMessage instead.");

            } else {

                if (getObjectFactory() == null) {
                    return null;
                }
                setMyResponse(getObjectFactory().getResponse(this));
            }
        }
        return getMyResponse();
    }

    /**
     * Set the response object in the service context for usage.
     *
     * @param msg
     * @throws IllegalResponseObjectException
     */
    @Override
    public void setResponse(HttpResponseMessage msg) throws IllegalResponseObjectException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setResponse: " + msg);
        }

        // null message isn't allowed
        if (null == msg) {
            throw new IllegalResponseObjectException("Illegal null message");
        }

        // save new object as our response object
        HttpResponseMessageImpl temp = null;
        try {
            temp = (HttpResponseMessageImpl) msg;
        } catch (ClassCastException cce) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Non msg impl passed to setResponse");
            }
            throw new IllegalResponseObjectException("Invalid message provided");
        }

        // possibly clean up any existing response object
        if (null != getMyResponse() && isResponseOwner()) {
            if (!getMyResponse().equals(temp)) {
                getMyResponse().destroy();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Caller overlaying same message");
                }
            }
        }
        setMyResponse(temp);
        getMyResponse().init(this);
        // moving the response to the inbound side will use the inbound sides
        // configured header change limit
        getMyResponse().setHeaderChangeLimit(getHttpConfig().getHeaderChangeLimit());
        updatePersistence(getMyResponse());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setResponse");
        }
    }

    // ********************************************************
    // Sending body buffer methods
    // ********************************************************

    /**
     * Send the headers for the outgoing response synchronously.
     *
     * @throws IOException          -- if a socket exception occurs
     * @throws MessageSentException -- if a finishMessage API was already used
     */
    @Override
    public void sendResponseHeaders() throws IOException, MessageSentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sendResponseHeaders(sync)");
        }
        if (!headersParsed()) {
            // request message must have the headers parsed prior to sending
            // any data out (this is a completely invalid state in the channel
            // above)
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe,
                                        CLASS_NAME + ".sendResponseHeaders", "594");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to send response without a request msg");
            }
            throw ioe;
        }

        if (headersSent()) {
            throw new MessageSentException("Message already sent");
        }

        if (getHttpConfig().useNetty()) {
            sendHeaders(this.nettyResponse);
        } else {
            sendHeaders(getResponseImpl());
            if (getResponseImpl().isTemporaryStatusCode()) {
                // allow multiple temporary responses to be sent out
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Temp response sent, resetting send flags.");
                }
                resetWrite();
            }
        }
    }

    /**
     * Send the headers for the outgoing response asynchronously. The
     * callback will be called when finished.
     * <p>
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible
     * for handling that situation in their code. A null return code means
     * that the async write is in progress.
     * <p.>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return
     * code will always be null and the callback always used.
     *
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException -- if a finishMessage API was already used
     */
    @Override
    public VirtualConnection sendResponseHeaders(InterChannelCallback callback, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendResponseHeaders(async)");
        }
        if (!headersParsed()) {
            // request message must have the headers parsed prior to sending
            // any data out (this is a completely invalid state in the channel
            // above)
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe, CLASS_NAME + ".sendResponseHeaders", "639");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to send response without a request msg");
            }
            callback.error(getVC(), ioe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "sendReponseHeaders(async): no request");
            }
            return null;
        }

        if (headersSent()) {
            throw new MessageSentException("Message already sent");
        }

        setForceAsync(bForce);
        setAppWriteCallback(callback);
        VirtualConnection vc = sendHeaders(getResponseImpl(), HttpISCWriteCallback.getRef());
        if (null != vc) {
            // Note: if forcequeue is true, then we will not get a VC object as
            // the lower layer will use the callback and return null
            if (getResponseImpl().isTemporaryStatusCode()) {
                // allow multiple temporary responses to be sent
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Temp response sent, resetting send flags.");
                }
                resetWrite();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendResponseHeaders(async): " + vc);
        }
        return vc;
    }

    /**
     * Send the given body buffers for the outgoing response synchronously.
     * If chunked encoding is set or if the partialbody flag is set, then
     * each call to this method will be considered a "chunk" and encoded
     * as such. If the message is Content-Length defined, then the buffers
     * will simply be sent out without modifications.
     * <p>
     * Note: if headers have not already been sent, then the first call to this
     * method will send the headers.
     *
     * @param body
     * @throws IOException
     *                                  -- if a socket exception occurs
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    @Override
    public void sendResponseBody(WsByteBuffer[] body) throws IOException, MessageSentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sendResponseBody(body)");
        }

        if (!headersParsed()) {
            // request message must have the headers parsed prior to sending
            // any data out (this is a completely invalid state in the channel
            // above)
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe, CLASS_NAME + ".sendResponseBody", "684");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to send response without a request msg");
            }
            throw ioe;
        }

        if (isMessageSent()) {
            throw new MessageSentException("Message already sent");
        }

        // if headers haven't been sent, then set for partial body transfer
        if (!headersSent()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sendBody() setting partial body true");
            }
            setPartialBody(true);
        }

        MSP.log("sendResponseBody buffer size: " + GenericUtils.sizeOf(body));

        if (getHttpConfig().useNetty()) {
            sendOutgoing(body);

        } else {
            sendOutgoing(body, getResponseImpl());
        }
    }

    /**
     * Send the given body buffers for the outgoing response asynchronously.
     * If chunked encoding is set or if the partialbody flag is set, then
     * each call to this method will be considered a "chunk" and encoded
     * as such. If the message is Content-Length defined, then the buffers
     * will simply be sent out with no modifications.
     * <p>
     * Note: if headers have not already been sent, then the first call to this
     * method will send the headers.
     * <p>
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible for
     * handling that situation in their code. A null return code means that the
     * async write is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return code will always
     * be null and the callback always used.
     *
     * @param body
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    @Override
    public VirtualConnection sendResponseBody(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sendResponseBody(body,cb)");
        }
        if (!headersParsed()) {
            // request message must have the headers parsed prior to sending
            // any data out (this is a completely invalid state in the channel
            // above)
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe, CLASS_NAME + ".sendResponseBody", "746");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to send response without a request msg");
            }
            callback.error(getVC(), ioe);
            return null;
        }

        if (isMessageSent()) {
            throw new MessageSentException("Message already sent");
        }

        // if headers haven't been sent, then set for partial body transfer
        if (!headersSent()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sendBody(cb) setting partial body true");
            }
            setPartialBody(true);
        }

        setForceAsync(bForce);
        setAppWriteCallback(callback);
        // Note: if forcequeue is true, then we will not get a VC object as
        // the lower layer will use the callback and return null
        return sendOutgoing(body, getResponseImpl(), HttpISCWriteCallback.getRef());
    }

    /**
     * Send an array of raw body buffers out synchronously. This method will
     * avoid any body modifications, such as compression or chunked-encoding.
     *
     * @param body
     * @throws IOException
     *                                  -- if a socket error occurs
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    @Override
    public void sendRawResponseBody(WsByteBuffer[] body) throws IOException, MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendRawResponseBody(sync)");
        }
        setRawBody(true);
        sendResponseBody(body);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendRawResponseBody(sync)");
        }
    }

    /**
     * Send an array of raw body buffers out asynchronously. This method will
     * avoid any body modifications, such as compression or chunked-encoding.
     * <p>
     * This will return null if the data is being written asynchronously and the
     * provided callback will be used when finished. However, if the write could
     * complete automatically, then the callback will not be used and instead a
     * non-null VC will be returned back.
     * <p>
     * The force parameter allows the caller to force the asynchronous call and to
     * always have the callback used, thus the return code will always be null.
     *
     * @param body
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    @Override
    public VirtualConnection sendRawResponseBody(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendRawResponseBody(async)");
        }
        setRawBody(true);
        VirtualConnection vc = sendResponseBody(body, callback, bForce);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendRawResponseBody(async): " + vc);
        }
        return vc;
    }

    /**
     * Once the response message has been fully written, perform any last checks
     * for correctness. This will return null if the response was valid,
     * otherwise it will return the HttpInvalidMessageException that should be
     * handed off to the application channel above.
     *
     * @return HttpInvalidMessageException (null if valid)
     */
    protected HttpInvalidMessageException checkResponseValidity() {
        // if this wasn't a HEAD request, then check to make sure we sent the
        // same amount of bytes that were in the content-length header
        if (!MethodValues.HEAD.equals(getRequest().getMethodValue())) {
            long len = getResponse().getContentLength();
            long num = getNumBytesWritten();
            if (HeaderStorage.NOTSET != len && num != len) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Response had content-length of " + Long.toString(len) + " but sent " + Long.toString(num));
                }
                if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.ERROR)) {
                    getHttpConfig().getDebugLog().log(DebugLog.Level.ERROR, HttpMessages.MSG_CONN_INVALID_LENGTHS + Long.toString(len) + " but sent " + Long.toString(num), this);

                }
                setPersistent(false);
                return new HttpInvalidMessageException("Response length: " + Long.toString(len) + " " + Long.toString(num));
            }
        }
        return null;
    }

    /**
     * Method to consolidate the access logging message into one spot. This
     * pulls all the pieces together for the appropriate line to save.
     *
     * Note if the signature of this method is changed, the signature in
     * HttpInboundServiceContextImplLogFinalResponseTransformDescriptor.java
     * needs to be updated. Passed as a parameter to avoid needing to reference
     * the class in getContextInfo.
     *
     */
    protected void logFinalResponse(long numBytesWritten) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            HttpChannelConfig c = getHttpConfig();
            Tr.debug(tc, "logFinal", c, c.getAccessLog(), c.getAccessLog().isStarted(), numBytesWritten);
        }

        if (this.getHttpConfig().useNetty()) {
            this.bytesWritten = numBytesWritten;
            this.nettyContext.write(this);
        } else {

            // exit if access logging is disabled
            if (!getHttpConfig().getAccessLog().isStarted()) {
                return;
            }
            if (MethodValues.UNDEF.equals(getRequest().getMethodValue())) {
                // don't log anything if there wasn't a real request
                return;
            }
            getHttpConfig().getAccessLog().log(getRequest(), getResponse(), getRequestVersion().getName(), null, getRemoteAddr().getHostAddress(), numBytesWritten);

        }
    }

    /**
     * Send the given body buffers for the outgoing response synchronously.
     * If chunked encoding is set or if the partialbody flag is set, then
     * these buffers will be considered a "chunk" and encoded
     * as such. If the message is Content-Length defined, then the buffers
     * will simply be sent out with no modifications. This marks the end
     * of the outgoing message.
     * <p>
     * Note: if headers have not already been sent, then the first call to this
     * method will send the headers. If this was a chunked encoded message, then
     * the zero-length chunk is automatically appended.
     *
     * @param body
     *                 (last set of buffers to send, null if no body data)
     * @throws IOException
     *                                  -- if a socket exception occurs
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    @Override
    public void finishResponseMessage(WsByteBuffer[] body) throws IOException, MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "finishResponseMessage(body)");
        }
        if (!getHttpConfig().useNetty() && !headersParsed()) {
            // request message must have the headers parsed prior to sending
            // any data out (this is a completely invalid state in the channel
            // above)
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe, CLASS_NAME + ".finishResponseMessage", "941");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to send response without a request msg");
            }
            throw ioe;
        }

        if (isMessageSent()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Message already sent");
            }

            throw new MessageSentException("Message already sent");
        }

        // if headers haven't been sent and chunked encoding is not explicitly
        // configured, then set this up for Content-Length
        if (!headersSent()) {
            boolean shouldContentLengthBeSet = Boolean.TRUE;

            if (Objects.nonNull(nettyContext)) {
                if (HttpUtil.isTransferEncodingChunked(nettyResponse)) {
                    shouldContentLengthBeSet = Boolean.FALSE;
                }
            } else {
                if (getResponseImpl().isChunkedEncodingSet()) {

                    shouldContentLengthBeSet = Boolean.FALSE;
                }
            }

            if (shouldContentLengthBeSet) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "finishMessage() setting partial body false");
                }
                setPartialBody(false);
                MSP.log("Bytes to write: " + GenericUtils.sizeOf(body));
                HttpUtil.setContentLength(nettyResponse, GenericUtils.sizeOf(body));
            }

        }

        if (getHttpConfig().runningOnZOS()) {
            // @311734 - add this to notify the xmem channel of our final write
            getVC().getStateMap().put(HttpConstants.FINAL_WRITE_MARK, "true");
        }
        try {
            if (Objects.nonNull(nettyContext)) {
                sendFullOutgoing(body);
            } else {
                sendFullOutgoing(body, getResponseImpl());
            }
        } finally {
            logFinalResponse(getNumBytesWritten());
        }

        if (!getHttpConfig().useNetty()) {
            HttpInvalidMessageException inv = checkResponseValidity();
            if (null != inv) {
                throw inv;
            }
        }
    }

    private void nettyFinishResponseMessage(WsByteBuffer[] body) {
    }

    /**
     * Send the given body buffers for the outgoing response asynchronously.
     * If chunked encoding is set or if the partialbody flag is set, then
     * these buffers will be considered a "chunk" and encoded
     * as such. If the message is Content-Length defined, then the buffers
     * will simply be sent out with no modifications. This marks the end
     * of the outgoing message.
     * <p>
     * Note: if headers have not already been sent, then the first call to this
     * method will send the headers. If this was a chunked encoded message, then
     * the zero-length chunk is automatically appended.
     * <p>
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible for
     * handling that situation in their code. A null return code means that the
     * async write is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return code will always
     * be null and the callback always used.
     *
     * @param body
     *                     (last set of body data, null if no body information)
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    @Override
    public VirtualConnection finishResponseMessage(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "finishResponseMessage(body,cb)");
        }
        // H2 doesn't support asych writes, if we got here and this is H2, switch over to sync
        if (isH2Connection()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "finishResponseMessage: This is H2, calling sync finishResponseMessage(body)");
            }
            // Send the error response synchronously for H2
            try {
                finishResponseMessage(body);
                return getVC();
            } catch (IOException e) {
                return null;
            }
        }
        if (!headersParsed()) {
            // request message must have the headers parsed prior to sending
            // any data out (this is a completely invalid state in the channel
            // above)
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe, CLASS_NAME + ".finishResponseMessage", "1010");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to send response without a request msg");
            }
            callback.error(getVC(), ioe);
            return null;
        }

        if (isMessageSent()) {
            throw new MessageSentException("Message already sent");
        }

        // if headers haven't been sent and chunked encoding is not explicitly
        // configured, then set this up for Content-Length
        if (!headersSent() && !getResponseImpl().isChunkedEncodingSet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "finishMessage(cb) setting partial body false");
            }
            setPartialBody(false);
        }

        if (getHttpConfig().runningOnZOS()) {
            // @311734 - add this to notify the xmem channel of our final write
            getVC().getStateMap().put(HttpConstants.FINAL_WRITE_MARK, "true");
        }
        setForceAsync(bForce);
        setAppWriteCallback(callback);
        VirtualConnection vc = sendFullOutgoing(body, getResponseImpl(), HttpISCWriteCallback.getRef());
        if (null != vc) {
            // Note: if forcequeue is true, then we will not get a VC object as
            // the lower layer will use the callback and return null
            logFinalResponse(getNumBytesWritten());
            HttpInvalidMessageException inv = checkResponseValidity();
            if (null != inv) {
                // invalid response
                callback.error(vc, inv);
                return null;
            }
        }
        return vc;
    }

    /**
     * Finish sending the response message with the optional input body buffers.
     * These can be null if there is no more actual body data to send. This
     * method will avoid any body modification, such as compression or chunked
     * encoding and simply send the buffers as-is. If the headers have not
     * been sent yet, then they will be prepended to the input data.
     *
     * @param body
     *                 -- null if there is no body data
     * @throws IOException
     *                                  -- if a socket exception occurs
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    @Override
    public void finishRawResponseMessage(WsByteBuffer[] body) throws IOException, MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "finishRawResponseMessage(sync)");
        }
        setRawBody(true);
        finishResponseMessage(body);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "finishRawResponseMessage(sync)");
        }
    }

    /**
     * Finish sending the response message asynchronously. The body buffers can
     * be null if there is no more actual body. This method will avoid any
     * body modifications, such as compression or chunked-encoding. If the
     * headers have not been sent yet, then they will be prepended to the input
     * data.
     * <p>
     * If the asynchronous write can be done immediately, then this will return a
     * VirtualConnection and the caller's callback will not be used. If this
     * returns null, then the callback will be used when complete.
     * <p>
     * The force flag allows the caller to force the asynchronous communication
     * such that the callback is always used.
     *
     * @param body
     *                   -- null if there is no more body data
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    @Override
    public VirtualConnection finishRawResponseMessage(WsByteBuffer[] body, InterChannelCallback cb, boolean bForce) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "finishRawResponseMessage(async)");
        }
        setRawBody(true);
        VirtualConnection vc = finishResponseMessage(body, cb, bForce);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "finishRawResponseMessage(async): " + vc);
        }
        return vc;
    }

    /**
     * Sends an error code and page back to the client asynchronously and
     * closes the connection.
     *
     * @param error
     * @throws MessageSentException
     */
    @Override
    public void sendError(HttpError error) throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Called sendError with: " + error);
        }
        if (!headersParsed()) {
            // request message must have the headers parsed prior to sending
            // any data out (this is a completely invalid state in the channel
            // above)
            // In this case, the app should not be re-using this so just close it
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe, CLASS_NAME + ".sendError", "1116");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to send response without a request msg");
            }
            finishSendError(ioe);
            return;
        }

        // bail if the headers have already been sent
        if (headersSent()) {
            throw new MessageSentException("Message already sent");
        }

        // ensure error is legal
        if (null == error) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reset null error to Internal Server Error");
            }
            error = StatusCodes.INTERNAL_ERROR.getHttpError();
        }

        // set up the response message and then send it out
        getResponse().setStatusCode(error.getErrorCode());
        if (getResponse().getStatusCode().isErrorCode()) {
            // PK30169 - some "error" status codes do not disable persistence
            setPersistent(false);
        }
        getVC().getStateMap().put(HTTP_ERROR_IDENTIFIER, error);
        if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.ERROR)) {
            getHttpConfig().getDebugLog().log(DebugLog.Level.ERROR, HttpMessages.MSG_CONN_SENDERROR + error.getErrorCode(), this);
        }
        // now figure out what buffers, if any, to send as the response body
        WsByteBuffer[] body = loadErrorBody(error, getMyRequest(), getResponse());
        VirtualConnection rc = finishResponseMessage(body, HttpISCWriteErrorCallback.getRef(), false);
        if (null != rc) {
            finishSendError(error.getClosingException());
        }
    }

    /**
     * Finish the send error path by closing the connection with the exception
     * from the HttpError. This is used by the callbacks.
     *
     */
    protected void finishSendError() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "finishSendError: " + getVC());
        }
        HttpError error = (HttpError) getVC().getStateMap().get(HTTP_ERROR_IDENTIFIER);
        WsByteBuffer[] body = (WsByteBuffer[]) getVC().getStateMap().remove(EPS_KEY);
        if (null != body) {
            for (int i = 0; i < body.length && null != body[i]; i++) {
                body[i].release();
            }
        }
        if (null != error) {
            this.myLink.close(getVC(), error.getClosingException());
        } else {
            this.myLink.close(getVC(), null);
        }
    }

    /**
     * Finish the send error path by closing the connection with the exception
     * from the HttpError.
     *
     * @param e
     */
    protected void finishSendError(Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "finishSendError(exception): " + getVC());
        }
        WsByteBuffer[] body = (WsByteBuffer[]) getVC().getStateMap().remove(EPS_KEY);
        if (null != body) {
            for (int i = 0; i < body.length && null != body[i]; i++) {
                body[i].release();
            }
        }
        this.myLink.close(getVC(), e);
    }

    /**
     * Check if the request contained the Expect: 100-continue header and if so,
     * send back an automatic 100 Continue response. Once done, clean up the
     * response message for the application channel above to send the "final"
     * response.
     *
     * @return boolean -- false means an async write is taking place and the
     *         caller cannot continue until that callback is used.
     */
    protected boolean check100Continue() {
        // if the request wasn't expecting the 100 continue, just return now
        if (!getRequest().isExpect100Continue()) {
            return true;
        }
        // if the channel stopped while we parsed this Expect request, we want
        // to send an error to close the connection and avoid the body transfer
        // PK12235, check for a full stop only
        // PH41928 check also for stopping. WC will reject this with 503 if the server
        // is stopping.  cfw should send the 503 at this point so the body of the
        // request is not lost.
        if (!this.myLink.getChannel().isRunning()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Channel " + (this.myLink.getChannel().isStopped() ? "stopped" : "stopping") + ", sending error instead of 100-continue");
            }
            try {
                sendError(StatusCodes.UNAVAILABLE.getHttpError());
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".check100Continue", "1206");
            }
            return false;
        }
        // if we're running on the SR for z/OS, never send the 100-continue
        // response no matter what the request says
        if (getHttpConfig().isServantRegion()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "100 continue not sent on SR");
            }
            return true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Request contains [Expect: 100-continue]");
        }
        if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.DEBUG)) {
            getHttpConfig().getDebugLog().log(DebugLog.Level.DEBUG, HttpMessages.MSG_CONN_EXPECT100, this);
        }
        // send the 100 Continue response synchronously since it should not
        // take that long to dump a single buffer out
        HttpResponseMessageImpl msg = getResponseImpl();
        msg.setStatusCode(StatusCodes.CONTINUE);
        msg.setContentLength(0);
        VirtualConnection vc = sendHeaders(msg, Http100ContWriteCallback.getRef());
        if (null == vc) {
            // writing async
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Async write of 100 continue still going");
            }
            return false;
        }
        // reset the values on the response message
        resetMsgSentState();
        msg.setStatusCode(StatusCodes.OK);
        // 366388
        msg.removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);
        return true;
    }

    /**
     * If we are purging the incoming request body during a close sequence, then
     * this method should be called once the entire body is successfully read.
     * This will discard those body buffers and then restart the close process
     * now that we're ready to read the next inbound request.
     *
     * @param callClose
     *                      (should this method call the close API itself)
     */
    public void purgeBodyBuffers(boolean callClose) {

        // the async read for the entire incoming body has finished, discard
        // the buffers and pick up with the close sequence
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Discarding body buffers...");
        }
        // call the clear storage methods as they will release any buffers
        // currently held for the body
        super.clearStorage();
        super.clearTempStorage();
        if (callClose) {
            this.myLink.close(getVC(), null);
        }
    }

    /**
     * Get all of the remaining body buffers for the request synchronously.
     * If the body is encoded or compressed, then that is removed and
     * unencoded buffers returned.
     * <p>
     * A null response means that there is no body left to read.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users
     * responsibility to release it.
     *
     * @return WsByteBuffer[]
     * @throws IOException
     *                                      -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *                                      -- if a malformed request body is
     *                                      present such that the server should send an HTTP 400 Bad Request
     *                                      back to the client.
     */

    @Override
    public WsByteBuffer[] getRequestBodyBuffers() throws IOException, IllegalHttpBodyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRequestBodyBuffers(sync)");
        }
        if (!headersParsed()) {
            // request message must have the headers parsed prior to attempting
            // to read a body (this is a completely invalid state in the channel
            // above)
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe, CLASS_NAME + ".getRequestBodyBuffers", "1281");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to read a body without headers");
            }
            throw ioe;
        }

        // check for an HTTP/2 specific content length
        int h2ContentLength = -1;
        if (myLink instanceof H2HttpInboundLinkWrap) {
            h2ContentLength = ((H2HttpInboundLinkWrap) myLink).getH2ContentLength();
        }

        // check to see if a body is allowed before reading for one
        if (!isIncomingBodyValid() && h2ContentLength == -1) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getRequestBodyBuffers(sync): No body allowed");
            }
            return null;
        }

        WsByteBuffer[] buffers = null;
        setMultiRead(true);

        // read the buffers if need be
        if (!isBodyComplete()) {
            try {
                readBodyBuffers(getRequestImpl(), false);
            } catch (BodyCompleteException e) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getRequestBodyBuffers(sync): BodyCompleteException");
                }
                return null;
            }
        }
        buffers = getAllStorageBuffers();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRequestBodyBuffers(sync): " + ((null == buffers) ? 0 : buffers.length));
        }
        return buffers;
    }

    /**
     * Read in all of the body buffers for the request asynchronously.
     * If the body is encoded or compressed, then that is removed and
     * unencoded buffers returned.
     * <p>
     * If the asynchronous request is fulfilled on the same thread, then this
     * connection's VirtualConnection will be returned and the callback will not
     * be used. A null return code means that an asynchronous read is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return code will always
     * be null and the callback always used.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users
     * responsibility to release it.
     *
     * @param callback
     * @param bForce
     * @return VirtualConnection (null if an async read is in progress,
     *         non-null if data is ready)
     * @throws BodyCompleteException
     *                                   -- if the entire body has already been read
     */
    @Override
    public VirtualConnection getRequestBodyBuffers(InterChannelCallback callback, boolean bForce) throws BodyCompleteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRequestBodyBuffers(async)");
        }
        if (!headersParsed()) {
            // request message must have the headers parsed prior to attempting
            // to read a body (this is a completely invalid state in the channel
            // above)
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe, CLASS_NAME + ".getRequestBodyBuffers", "1355");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to read a body without headers");
            }
            callback.error(getVC(), ioe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getRequestBodyBuffers(async): no hdrs yet");
            }
            return null;
        }

        // check to see if a read is even necessary
        if (!isIncomingBodyValid() || incomingBuffersReady()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getRequestBodyBuffers(async): read not needed");
            }
            if (bForce) {
                callback.complete(getVC());
                return null;
            }
            return getVC();
        }

        if (isBodyComplete()) {
            // throw new BodyCompleteException("No more body to read");
            // instead of throwing an exception, just return the VC as though
            // data is immediately ready and the caller will switch to their
            // sync block and then get a null buffer back
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getRequestBodyBuffers(async): body complete");
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
            if (!readBodyBuffers(getRequestImpl(), true)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getRequestBodyBuffers(async): read finished");
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
                Tr.exit(tc, "getRequestBodyBuffers(async): exception:" + ioe);
            }
            callback.error(getVC(), ioe);
            return null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRequestBodyBuffers(async): null");
        }
        return null;
    }

    /**
     * This gets the next body buffer. If the body is encoded/compressed,
     * then the encoding is removed and the "next" buffer returned.
     * <p>
     * A null response means that there is no body left to read.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users
     * responsibility to release it.
     *
     * @return WsByteBuffer
     * @throws IOException
     *                                      -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *                                      -- if a malformed request body is
     *                                      present such that the server should send an HTTP 400 Bad Request
     *                                      back to the client.
     */
    @Override
    public WsByteBuffer getRequestBodyBuffer() throws IOException, IllegalHttpBodyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRequestBodyBuffer(sync)");
        }
        if (!headersParsed()) {
            // request message must have the headers parsed prior to attempting
            // to read a body (this is a completely invalid state in the channel
            // above)
            IOException ioe = new IOException("Request not read yet");
            FFDCFilter.processException(ioe, CLASS_NAME + ".getRequestBodyBuffer", "1436");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt to read a body without headers");
            }
            throw ioe;
        }

        // check to see if a body is allowed before reading for one
        if (!isIncomingBodyValid()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getRequestBodyBuffer(sync): No body allowed");
            }
            return null;
        }

        setMultiRead(false);
        // check for any already read buffer
        WsByteBuffer buffer = getNextBuffer();
        if (null == buffer && !isBodyComplete()) {
            // read a buffer
            try {
                readBodyBuffer(getRequestImpl(), false);
            } catch (BodyCompleteException e) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getRequestBodyBuffer(sync): BodyCompleteException");
                }
                return null;
            }
            buffer = getNextBuffer();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRequestBodyBuffer(sync): " + buffer);
        }
        return buffer;
    }

    /**
     * This gets the next body buffer asynchronously. If the body is encoded
     * or compressed, then the encoding is removed and the "next" buffer
     * returned. Null callbacks are not allowed and will trigger a null pointer
     * exception.
     * <p>
     * If the asynchronous request is fulfilled on the same thread, then this
     * connection's VirtualConnection will be returned and the callback will not
     * be used. A null return code means that an asynchronous read is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return code will always
     * be null and the callback always used.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users
     * responsibility to release it.
     *
     * @param callback
     * @param bForce
     * @return VirtualConnection (null if an async read is in progress,
     *         non-null if data is ready)
     * @throws BodyCompleteException
     *                                   -- if the entire body has already been read
     */
    @Override
    public VirtualConnection getRequestBodyBuffer(InterChannelCallback callback, boolean bForce) throws BodyCompleteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRequestBodyBuffer(async) hc: " + this.hashCode());
        }
        boolean isError = false;

        try {
            if (!headersParsed()) {
                // request message must have the headers parsed prior to attempting
                // to read a body (this is a completely invalid state in the channel
                // above)
                IOException ioe = new IOException("Request not read yet");
                FFDCFilter.processException(ioe, CLASS_NAME + ".getRequestBodyBuffer", "1511");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Attempt to read a body without headers");
                }
                callback.error(getVC(), ioe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getRequestBodyBuffer(async): no hdrs yet");
                }
                isError = true;
                return null;
            }

            // check to see if a read is even necessary
            if (!isIncomingBodyValid() || incomingBuffersReady()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getRequestBodyBuffer(async): read not needed");
                }
                if (bForce) {
                    callback.complete(getVC());
                    return null;
                }
                return getVC();
            }

            if (isBodyComplete()) {
                // throw new BodyCompleteException("No more body to read");
                // instead of throwing an exception, just return the VC as though
                // data is immediately ready and the caller will switch to their
                // sync block and then get a null buffer back
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getRequestBodyBuffer(async): body complete");
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
                if (!readBodyBuffer(getRequestImpl(), true)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "getRequestBodyBuffer(async): read finished");
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
                    Tr.exit(tc, "getRequestBodyBuffer(async): exception: " + ioe);
                }
                isError = true;
                callback.error(getVC(), ioe);
                return null;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getRequestBodyBuffer(async): null");
            }
            return null;
        } finally {
            countDownFirstReadLatch(isError);
        }
    }

    public void countDownFirstReadLatch(boolean force) {
        if (this.myLink instanceof H2HttpInboundLinkWrap) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "countDownFirstReadLatch. count down. force: " + force + " HISCI hc: " + this.hashCode());
            }
            ((H2HttpInboundLinkWrap) myLink).countDownFirstReadLatch(force);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " can not count down countDownFirstReadLatch. HISCI hc: " + this.hashCode());
            }
        }
    }

    /**
     * Retrieve the next buffer of the request message's body. This will give
     * the buffer without any modifications, avoiding decompression or chunked
     * encoding removal.
     * <p>
     * A null buffer will be returned if there is no more data to get.
     * <p>
     * The caller is responsible for releasing these buffers when complete as the
     * HTTP Channel does not keep track of them.
     *
     * @return WsByteBuffer
     * @throws IOException
     *                                      -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *                                      -- if a malformed request body is
     *                                      present such that the server should send an HTTP 400 Bad Request
     *                                      back to the client.
     */
    @Override
    public WsByteBuffer getRawRequestBodyBuffer() throws IOException, IllegalHttpBodyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRawRequestBodyBuffer(sync)");
        }
        setRawBody(true);
        WsByteBuffer buffer = getRequestBodyBuffer();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRawRequestBodyBuffer(sync): " + buffer);
        }
        return buffer;
    }

    /**
     * Retrieve all remaining buffers of the request message's body. This will
     * give the buffers without any modifications, avoiding decompression or
     * chunked encoding removal.
     * <p>
     * A null buffer array will be returned if there is no more data to get.
     * <p>
     * The caller is responsible for releasing these buffers when complete as the
     * HTTP Channel does not keep track of them.
     *
     * @return WsByteBuffer[]
     * @throws IOException
     *                                      -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *                                      -- if a malformed request body is
     *                                      present such that the server should send an HTTP 400 Bad Request
     *                                      back to the client.
     */
    @Override
    public WsByteBuffer[] getRawRequestBodyBuffers() throws IOException, IllegalHttpBodyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRawRequestBodyBuffers(sync)");
        }
        setRawBody(true);
        WsByteBuffer[] list = getRequestBodyBuffers();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRawRequestBodyBuffers(sync): " + list);
        }
        return list;
    }

    /**
     * Retrieve the next buffer of the body asynchronously. This will avoid any
     * body modifications, such as decompression or removal of chunked-encoding
     * markers.
     * <p>
     * If the read can be performed immediately, then a VirtualConnection will be
     * returned and the provided callback will not be used. If the read is being
     * done asychronously, then null will be returned and the callback used when
     * complete. The force input flag allows the caller to force the asynchronous
     * read to always occur, and thus the callback to always be used.
     * <p>
     * The caller is responsible for releasing these buffers when finished with
     * them as the HTTP Channel keeps no reference to them.
     *
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws BodyCompleteException
     *                                   -- if the entire body has already been read
     */
    @Override
    public VirtualConnection getRawRequestBodyBuffer(InterChannelCallback cb, boolean bForce) throws BodyCompleteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRawRequestBodyBuffer(async)");
        }
        setRawBody(true);
        VirtualConnection vc = getRequestBodyBuffer(cb, bForce);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRawRequestBodyBuffer(async): " + vc);
        }
        return vc;
    }

    /**
     * Retrieve any remaining buffers of the body asynchronously. This will
     * avoid any body modifications, such as decompression or removal of
     * chunked-encoding markers.
     * <p>
     * If the read can be performed immediately, then a VirtualConnection will be
     * returned and the provided callback will not be used. If the read is being
     * done asychronously, then null will be returned and the callback used when
     * complete. The force input flag allows the caller to force the asynchronous
     * read to always occur, and thus the callback to always be used.
     * <p>
     * The caller is responsible for releasing these buffers when finished with
     * them as the HTTP Channel keeps no reference to them.
     *
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws BodyCompleteException
     *                                   -- if the entire body has already been read
     */
    @Override
    public VirtualConnection getRawRequestBodyBuffers(InterChannelCallback cb, boolean bForce) throws BodyCompleteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRawRequestBodyBuffers(async)");
        }
        setRawBody(true);
        VirtualConnection vc = getRequestBodyBuffers(cb, bForce);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRawRequestBodyBuffers(async): " + vc);
        }
        return vc;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#getMessageBeingParsed
     * ()
     */
    @Override
    protected HttpBaseMessageImpl getMessageBeingParsed() {
        return getRequestImpl();
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#getMessageBeingSent
     * ()
     */
    @Override
    protected HttpBaseMessageImpl getMessageBeingSent() {
        return getResponseImpl();
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#parsingComplete()
     */
    @Override
    final protected void parsingComplete() throws Exception {
        super.parsingComplete();
        // if Content-Length, then we know how many bytes are supposed to be
        // coming in the body... chunked encoding, we have to wait until we
        // know the chunk sizes.
        if (super.isContentLength()) {
            checkIncomingMessageLimit(super.getContentLength());
        }
    }

    /**
     * Query whether or not this particular connection has a "large" message
     * that is over the standard message size limit.
     *
     * @return boolean (false if under the standard limit or if not limited at
     *         all)
     */
    protected boolean containsLargeMessage() {
        return this.bContainsLargeMessage;
    }

    /*
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#
     * checkIncomingMessageLimit(int)
     */
    @Override
    protected void checkIncomingMessageLimit(long addition) throws MessageTooLargeException {
        super.addToIncomingMsgSize(addition);
        // check if we even need to bother comparing sizes against limits
        if (HttpConfigConstants.UNLIMITED == getHttpConfig().getMessageSizeLimit()) {
            return;
        }
        if (queryIncomingMsgSize() > getHttpConfig().getMessageSizeLimit()) {
            // over channel limit, see if the factory has a system wide "large" limit
            HttpInboundChannelFactory factory = this.myLink.getChannel().getFactory();
            if (factory.getConfig().areMessagesLimited() && factory.allowLargeMessage(queryIncomingMsgSize())) {
                this.bContainsLargeMessage = true;
            } else {
                throw new MessageTooLargeException("Size=" + queryIncomingMsgSize());
            }
        }
    }

    /**
     * Query the channel connection link for this service context.
     *
     * @return HttpInboundLink
     */
    final public HttpInboundLink getLink() {
        return this.myLink;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#reconnectAllowed()
     */
    @Override
    protected boolean reconnectAllowed() {
        // inbound does not allow a reconnect since we didn't create the
        // connection
        return false;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#getObjectFactory()
     */
    @Override
    public HttpObjectFactory getObjectFactory() {
        return (null == this.myLink) ? null : this.myLink.getObjectFactory();
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#isInboundConnection
     * ()
     */
    @Override
    public boolean isInboundConnection() {
        return true;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.HttpServiceContextImpl#logLegacyMessage()
     */
    @Override
    public void logLegacyMessage() {
        // 366860 - if we're on the z/os CR, use the legacy message service
        if (getHttpConfig().isControlRegion() && writingHeaders()) {
            HttpPlatformUtils utils = (HttpPlatformUtils) HttpDispatcher.getFramework().lookupService(HttpPlatformUtils.class);
            if (null != utils) {
                utils.logLegacyMessage(this);
            }
        }
    }

    /**
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#getStartNanoTime()
     */
    @Override
    public long getStartNanoTime() {
        return startTime;
    }

    /**
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#resetStartTime()
     */
    @Override
    public void resetStartTime() {
        startTime = 0;
    }

    /**
     * @see com.ibm.ws.http.channel.internal.HttpServiceContextImpl#setStartTime()
     */
    @Override
    public void setStartTime() {
        if (0 == startTime) {
            if (getHttpConfig().isAccessLoggingEnabled()) {

                if (Objects.nonNull(nettyContext) &&
                    nettyContext.channel().hasAttr(NettyHttpConstants.REQUEST_START_TIME)) {
                    this.startTime = nettyContext.channel().attr(NettyHttpConstants.REQUEST_START_TIME).get();
                } else {
                    this.startTime = System.nanoTime();
                }
            }
        }
    }

    /**
     * Send a HTTP/1.1 101 Switching Protocols in response to a http/2 upgrade request. The following headers will be added:
     *
     * connection: Upgrade, HTTP2-Settings
     * upgrade: h2c
     */
    public boolean send101SwitchingProtocol(String protocol) {
        // if the channel stopped while we parsed this Expect request, we want
        // to send an error to close the connection and avoid the body transfer
        // PK12235, check for a full stop only
        if (this.myLink.getChannel().isStopped()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Channel stopped, sending error instead of 100-continue");
            }
            try {
                sendError(StatusCodes.UNAVAILABLE.getHttpError());
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".check100Continue", "1206");
            }
            return false;
        }
        // if we're running on the SR for z/OS, never send the 100-continue
        // response no matter what the request says
        if (getHttpConfig().isServantRegion()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "100 continue not sent on SR");
            }
            return true;
        }

        // send the 101 response synchronously since it should not
        // take that long to dump a single buffer out
        HttpResponseMessageImpl msg = getResponseImpl();
        msg.setStatusCode(StatusCodes.SWITCHING_PROTOCOLS);

        msg.setHeader(HttpHeaderKeys.HDR_UPGRADE, protocol);
        msg.setSpecialHeader(HttpHeaderKeys.HDR_CONNECTION, "Upgrade");

        msg.setContentLength(0);
        VirtualConnection vc = sendHeaders(msg, Http100ContWriteCallback.getRef());
        if (null == vc) {
            // writing async
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Async write of 100 continue still going");
            }
            return false;
        }
        // reset the values on the response message
        resetMsgSentState();
        msg.setStatusCode(StatusCodes.OK);
        msg.setVersion(VersionValues.V20);
        // 366388
        msg.removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);

        // this msg will be re-used after the connection is upgraded, so we need to remove these two headers
        msg.removeHeader(HttpHeaderKeys.HDR_UPGRADE);
        msg.removeHeader(HttpHeaderKeys.HDR_CONNECTION);
        return true;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    public String getRemoteUser() {
        return this.remoteUser;
    }

    public void initForwardedValues() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "initForwardedValues");
        }

        this.forwardedHeaderInitialized = Boolean.TRUE;

        MSP.log("initForwardedValues, useNetty? " + Objects.nonNull(nettyRequest));

        if (Objects.nonNull(nettyRequest)) {
            nettyInitForwardedValues();
        } else {
            legacyInitForwardedValues();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "initForwardedValues");
        }

    }

    private void legacyInitForwardedValues() {
        //Obtain the Regular Expression either from the configuration or default
        Pattern pattern = getHttpConfig().getForwardedProxiesRegex();
        Matcher matcher = null;

        //First Check if connected endpoint IP addresses matches the regular expression
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Verifying connected endpoint matches proxy regex");
        }

        String remoteIp = getTSC().getRemoteAddress().getHostAddress();

        matcher = pattern.matcher(remoteIp);
        if (matcher.matches()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Connected endpoint matched, verifying forwarded FOR list addresses");
            }
            //fetch from the legacy base message
            String[] forwardedForList = this.getMessageBeingParsed().getForwardedForList();

            if (forwardedForList == null || forwardedForList.length == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No forwarded FOR addresses provided, forwarded values will not be used");
                    Tr.exit(tc, "initForwardedValues");
                }
                return;
            }
            for (int i = forwardedForList.length - 1; i > 0; i--) {
                matcher = pattern.matcher(forwardedForList[i]);
                if (!matcher.matches()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found address not defined in proxy regex, forwarded values will not be used");
                        Tr.exit(tc, "initForwardedValues");
                    }
                    return;
                }
            }
            //if we get to the end, set the forwarded fields with correct values

            //First check that the last node identifier is not an obfuscated address or
            //unknown token
            if (forwardedForList[0] == null || "unknown".equals(forwardedForList[0]) || forwardedForList[0].startsWith("_")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Client address is unknown or obfuscated, forwarded values will not be used");
                    Tr.exit(tc, "initForwardedValues");
                }
                return;
            }

            //Check if a port was included
            if (this.getMessageBeingParsed().getForwardedPort() != null) {
                //If this port does not resolve to an integer, because it is obfuscated,
                //malformed, or otherwise, then the address cannot be verified as being
                //the client. If so, exit now.
                try {
                    this.forwardedRemotePort = Integer.parseInt(getMessageBeingParsed().getForwardedPort());
                } catch (NumberFormatException e) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Remote port provided was either obfuscated or malformed, forwarded values will not be used.");
                        Tr.exit(tc, "initForwardedValues");
                    }
                    return;
                }

            }

            this.forwardedRemoteAddress = forwardedForList[0];
            this.forwardedHost = this.getMessageBeingParsed().getForwardedHost();
            this.forwardedProto = this.getMessageBeingParsed().getForwardedProto();

        }
    }

    private void nettyInitForwardedValues() {
        //Obtain the Regular Expression either from the configuration or default
        Pattern pattern = getHttpConfig().getForwardedProxiesRegex();
        Matcher matcher = null;

        System.out.println("MSP: netty forwarded start");

        String remoteIp = nettyContext.channel().remoteAddress().toString();
        remoteIp = remoteIp.substring(1, remoteIp.indexOf(':'));

        String attribute;

        System.out.println("MSP: remote IP -> remoteIp : " + remoteIp);

        matcher = pattern.matcher(remoteIp);

        MSP.log("nettyInitForwardedValues 1");
        if (matcher.matches()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Connected endpoint matched, verifying forwarded FOR list addresses");
            }
            MSP.log("nettyInitForwardedValues 2");
            String[] forwardedForList = this.nettyContext.channel().attr(NettyHttpConstants.FORWARDED_FOR_KEY).get();
            if (Objects.isNull(forwardedForList) || forwardedForList.length == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No forwarded FOR addresses provided, forwarded values will not be used");
                    Tr.exit(tc, "initForwardedValues");
                }
                return;
            }
            MSP.log("nettyInitForwardedValues 3");
            for (int i = forwardedForList.length - 1; i > 0; i--) {
                MSP.log("Matching For List Element -> " + forwardedForList[i]);
                matcher = pattern.matcher(forwardedForList[i]);
                if (!matcher.matches()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found address not defined in proxy regex, forwarded values will not be used");
                        Tr.exit(tc, "initForwardedValues");
                    }
                    return;
                }
            }
            MSP.log("nettyInitForwardedValues 4");
            //if we get to the end, set the forwarded fields with correct values

            //First check that the last node identifier is not an obfuscated address or
            //unknown token
            if (Objects.isNull(forwardedForList[0]) || "unknown".equals(forwardedForList[0]) || forwardedForList[0].startsWith("_")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Client address is unknown or obfuscated, forwarded values will not be used");
                    Tr.exit(tc, "initForwardedValues");
                }
                return;
            }
            MSP.log("nettyInitForwardedValues 5");

            //Check if a port was included
            attribute = this.nettyContext.channel().attr(NettyHttpConstants.FORWARDED_PORT_KEY).get();
            if (Objects.nonNull(attribute)) {
                //If this port does not resolve to an integer, because it is obfuscated,
                //malformed, or otherwise, then the address cannot be verified as being
                //the client. If so, exit now.
                try {
                    this.forwardedRemotePort = Integer.parseInt(attribute);
                } catch (NumberFormatException e) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Remote port provided was either obfuscated or malformed, forwarded values will not be used.");
                        Tr.exit(tc, "initForwardedValues");
                    }
                    return;
                }
                MSP.log("nettyInitForwardedValues 6");
            }
            MSP.log("nettyInitForwardedValues 7");

            this.forwardedRemoteAddress = forwardedForList[0];
            this.forwardedHost = this.nettyContext.channel().attr(NettyHttpConstants.FORWARDED_HOST_KEY).get();
            this.forwardedProto = this.nettyContext.channel().attr(NettyHttpConstants.FORWARDED_PROTO_KEY).get();

        }

    }

    public int getForwardedRemotePort() {
        if (!forwardedHeaderInitialized)
            initForwardedValues();

        return this.forwardedRemotePort;
    }

    public String getForwardedRemoteAddress() {
        if (!forwardedHeaderInitialized)
            initForwardedValues();

        return this.forwardedRemoteAddress;
    }

    public String getForwardedRemoteProto() {
        if (!forwardedHeaderInitialized)
            initForwardedValues();

        return this.forwardedProto;
    }

    public String getForwardedRemoteHost() {
        if (!forwardedHeaderInitialized)
            initForwardedValues();

        return this.forwardedHost;
    }

    public boolean useForwardedHeaders() {
        return getHttpConfig().useForwardingHeaders();
    }

    public boolean useForwardedHeadersInAccessLog() {
        return getHttpConfig().useForwardingHeadersInAccessLog();
    }

    /**
     * Check if HTTP/2 is enabled for this context
     *
     * @return true if HTTP/2 is enabled for this link
     */
    public boolean isHttp2Enabled() {

        boolean isHTTP2Enabled = false;

        Boolean defaultSetting = CHFWBundle.getHttp2DefaultSetting();

        if (defaultSetting != null) {
            Boolean configSetting = getHttpConfig().getUseH2ProtocolAttribute();

            //If servlet-3.1 is enabled, HTTP/2 is optional and by default off.
            if (Boolean.FALSE == defaultSetting) {
                //If so, check if the httpEndpoint was configured for HTTP/2
                isHTTP2Enabled = configSetting != null && configSetting.booleanValue();
            } else {
                //If servlet-4.0 is enabled, HTTP/2 is optional and by default on.
                //If not configured as an attribute, getUseH2ProtocolAttribute will be null, which returns true
                //to use HTTP/2.
                isHTTP2Enabled = configSetting == null || configSetting.booleanValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Has HTTP/2 been enabled on this port: " + isHTTP2Enabled);
        }
        return isHTTP2Enabled;
    }

    /**
     * @param suppress0ByteChunk
     */
    public void setSuppress0ByteChunk(boolean suppress0ByteChunk) {
        this.suppress0ByteChunk = suppress0ByteChunk;
    }

    public boolean getSuppress0ByteChunk() {
        return this.suppress0ByteChunk;
    }
}