/*******************************************************************************
 * Copyright (c) 2004, 2023 IBM Corporation and others.
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
package com.ibm.ws.http.channel.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.zip.DataFormatException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.genericbnf.internal.GenericConstants;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.h2internal.H2HttpInboundLinkWrap;
import com.ibm.ws.http.channel.h2internal.H2StreamProcessor;
import com.ibm.ws.http.channel.h2internal.H2VirtualConnectionImpl;
import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FramePPHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FramePushPromise;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.h2internal.hpack.H2Headers;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants.LiteralIndexType;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundLink;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http2.GrpcServletServices;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.BNFHeaders;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.http.EncodingUtils;
import com.ibm.wsspi.http.HttpDateFormat;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.channel.compression.CompressionHandler;
import com.ibm.wsspi.http.channel.compression.DecompressionHandler;
import com.ibm.wsspi.http.channel.compression.DeflateInputHandler;
import com.ibm.wsspi.http.channel.compression.DeflateOutputHandler;
import com.ibm.wsspi.http.channel.compression.GzipInputHandler;
import com.ibm.wsspi.http.channel.compression.GzipOutputHandler;
import com.ibm.wsspi.http.channel.compression.IdentityInputHandler;
import com.ibm.wsspi.http.channel.error.HttpError;
import com.ibm.wsspi.http.channel.error.HttpErrorPageProvider;
import com.ibm.wsspi.http.channel.error.HttpErrorPageService;
import com.ibm.wsspi.http.channel.exception.BodyCompleteException;
import com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException;
import com.ibm.wsspi.http.channel.exception.MessageTooLargeException;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.TransferEncodingValues;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.http.logging.DebugLog;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;

/**
 * Common code shared between both the Inbound and Outbound HTTP service
 * context classes.
 *
 */
public abstract class HttpServiceContextImpl implements HttpServiceContext, FFDCSelfIntrospectable {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpServiceContextImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** used for chunk length information */
    private static final int NOT_ENOUGH_DATA = -1;
    /** used for unparsedDataRemaining representing default left to read */
    private static final int NO_MORE_DATA = -1;
    /** HEX character list */
    private static final byte[] HEX_BYTES = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a',
                                              (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };
    /** Data that gets placed in the chunk trailers */
    protected static final byte[] CHUNK_TRAILER_DATA = { BNFHeaders.CR, BNFHeaders.LF, '0', BNFHeaders.CR, BNFHeaders.LF, BNFHeaders.CR, BNFHeaders.LF };
    /** starting size of the pending buffer array */
    private static final int PENDING_BUFFER_INITIAL_SIZE = 10;
    /** starting min growth size of the pending buffer array */
    private static final int PENDING_BUFFER_MIN_GROWTH_SIZE = 4;

    /** State variable representing nothing complete yet */
    private static final int STATE_NONE = 0;
    /** State variable representing when headers are finished */
    private static final int STATE_FULL_HEADERS = 1;
    /** State variable representing that part of the body has been finished */
    private static final int STATE_PARTIAL_BODY = 2;
    /** State variable representing when the entire message is finished */
    private static final int STATE_FULL_MESSAGE = 3;

    /** Default remote port to initialize the value to */
    private static final int DEFAULT_REMOTE_PORT = 1024;
    /** Default local port to initialize the value to */
    private static final int DEFAULT_LOCAL_PORT = 9080;

    /** Default message encoding */
    private static final ContentEncodingValues DEFAULT_ENCODING = ContentEncodingValues.IDENTITY;

    /** Key used when sending EPS body */
    protected static final String EPS_KEY = "HttpChannel_ErrorPageService_Body";

    /** TCP service context for this connection */
    private TCPConnectionContext myTSC = null;
    /** The virtual connection object */
    private VirtualConnection myVC = null;
    /** Application side write callback used for the send/finishX methods */
    private InterChannelCallback appWriteCB = null;
    /** Application side read callback used for the getX methods */
    private InterChannelCallback appReadCB = null;
    /** List of buffers created while making the outgoing message */
    private WsByteBuffer[] myPendingBuffers = new WsByteBuffer[PENDING_BUFFER_INITIAL_SIZE];
    /** Start position of the buffers going outbound */
    private int pendingBufferStart = 0;
    /** Position to put next outgoing buffer */
    private int pendingBufferStop = 0;
    /** List of read buffers pulled from the channel below */
    private WsByteBuffer[] myParseBuffers = null;
    /** Index pointing to the next parse buffer to use */
    private int parseBufferIndex = -1;
    /** Flag on whether the current read was a JIT one */
    private boolean bIsJITRead = false;
    /** State variable for how much we've fully parsed from the incoming msg */
    private int msgParsedState = STATE_NONE;
    /** State variable for how much we've sent on the outgoing msg */
    private int msgSentState = STATE_NONE;
    /** Are we writing headers on this pass? */
    private boolean writingHeaders = false;
    /** Is this the final write of the message? */
    private boolean isFinalWrite = false;
    /** Flag on whether this connection is a persistent HTTP connection */
    private boolean bIsPersistent = true;
    /** Flag on whether the body being sent out is in partial chunks */
    private boolean bIsPartialBody = false;
    /** Flag on whether it is valid to send a body with this outgoing message */
    private boolean bIsOutgoingBodyValid = true;
    /** Flag on whether the incoming msg could have a body */
    private boolean bIsIncomingBodyValid = false;
    /** Flag on whether the body should be changed or not */
    private boolean bIsRawBody = false;
    /** Flag on whether we own the response and can safely clean it up */
    private boolean bIsResponseOwner = true;
    /** Flag on whether we own the request and can safely clean it up */
    private boolean bIsRequestOwner = true;
    /** Flag on whether to force the async read/writes or not */
    private boolean bForceAsync = false;
    /** Flag on whether we are currently parsing the trailer headers */
    private boolean bParsingTrailers = false;
    /** When reading the body, how much data is left to be read? */
    private long unparsedDataRemaining = NO_MORE_DATA;
    /** Flag on whether the incoming body is chunked or not */
    private boolean bIsChunked = false;
    /** Keep track of the message content-length value */
    private long myContentLength = HeaderStorage.NOTSET;
    /** Read callback used while reading the incoming body */
    private TCPReadCompletedCallback myBodyRC = null;
    /** array of buffers that are ready to hand out to users */
    private final LinkedList<WsByteBuffer> storage = new LinkedList<WsByteBuffer>();
    /** buffers that must be decoded prior to moving to storageBuffers */
    private final LinkedList<WsByteBuffer> tempBuffers = new LinkedList<WsByteBuffer>();
    /** Flag on whether this is a multibuffer read or not */
    private boolean bIsMultiRead = false;
    /** amount being request during an async read */
    private int amountBeingRead = 0;
    /** Counter for the number of body bytes sent with the outbound msg */
    private long numBytesWritten = 0L;
    /** Counter for the number of raw message bytes read so far (hdrs + body) */
    private long incomingMsgSize = 0L;
    /** Saved chunk length if we run out of buffer data while parsing */
    private int savedChunkLength = HeaderStorage.NOTSET;
    /** Keep track of the previous limit value */
    private int oldLimit = 0;
    /** Starting buffer position during the parsing of bodies */
    private int oldPosition = -1;
    /** Flag on whether we should touch read buffers at all */
    private boolean shouldModify = true;
    /** Stored buffer used for sending out chunk length CRLF */
    private WsByteBuffer buffChunkHeader = null;
    /** Stored buffer used for sending out the trailing CRLF after a chunk */
    private WsByteBuffer buffChunkTrailer = null;
    /** HTTP channel's configuration object */
    private HttpChannelConfig myChannelConfig = null;
    /** Timeout specific to all reads on this connection */
    private int myReadTimeout = 0;
    /** Timeout specific to all writes on this connection */
    private int myWriteTimeout = 0;
    /** Request message tied to this SC */
    private HttpRequestMessageImpl myRequest = null;
    /** Response message tied to this SC */
    private HttpResponseMessageImpl myResponse = null;
    /** Reference to the current buffer being read/parsed */
    private WsByteBuffer currentReadBB = null;
    /** Keep track of the request version as it might change in proxy env */
    private VersionValues reqVersion = null;
    /** Keep track of the request method as it might change in proxy env */
    private MethodValues reqMethod = null;
    /** 314871 - cache whether an incoming body is expected */
    private boolean bIsBodyExpected = false;
    /** Parsing state variable used while parsing the chunk length */
    private int chunkLengthParseState = GenericConstants.PARSING_NOTHING;
    /** Encoding of the incoming msg that can be automatically stripped off */
    private ContentEncodingValues incomingMsgEncoding = DEFAULT_ENCODING;
    /** Encoding of the outgoing msg */
    private ContentEncodingValues outgoingMsgEncoding = DEFAULT_ENCODING;
    /** Wrapper for the cancel logic surrounding reads */
    private CancelIOWrapper cancelRead = null;
    /** Wrapper for the cancel logic surrounding writes */
    private CancelIOWrapper cancelWrite = null;
    /** Address on the remote side of the connection */
    private InetAddress myRemoteAddr = null;
    /** Address on the local side of the connection */
    private InetAddress myLocalAddr = null;
    /** Port number of the remote side of the connection */
    private int myRemotePort = DEFAULT_REMOTE_PORT;
    /** Port number of the local side of the connection */
    private int myLocalPort = DEFAULT_LOCAL_PORT;
    /** List of buffers used during parsing of headers */
    private LinkedList<WsByteBuffer> allocatedBuffers = null;
    /** Index of last buffer which contains header information */
    private int lastHeaderBuffer = HeaderStorage.NOTSET;
    /** Compression handler used for the outbound body */
    private CompressionHandler compressHandler = null;
    /** Decompression handler used for the inbound body */
    private DecompressionHandler decompressHandler = null;
    /** Decompression tolerance counter */
    private int cyclesAboveDecompressionRatio = 0;

    protected Map<String, Float> acceptableEncodings = new HashMap<String, Float>();
    protected Set<String> unacceptedEncodings = new HashSet<String>();
    protected boolean bStarEncodingParsed = false;
    protected String preferredEncoding = null;

    /** Record the end time of the request if access logging is enabled */
    private long responseStartTime = 0;

    private boolean isPushPromise = false;
    private boolean isH2Connection = false;

    private final CopyOnWriteArrayList<Frame> framesToWrite = new CopyOnWriteArrayList<Frame>();

    private ChannelHandlerContext nettyContext;
    private FullHttpRequest nettyRequest;
    private io.netty.handler.codec.http.HttpResponse nettyResponse;

    /**
     * Constructor for this base service context class.
     */
    protected HttpServiceContextImpl() {
        // the first buffer on creation is a JIT buffer by default (or we will
        // override this flag explicitly)
        this.bIsJITRead = true;
        this.allocatedBuffers = new LinkedList<WsByteBuffer>();
    }

    public void setNettyContext(ChannelHandlerContext ctx) {
        this.nettyContext = ctx;
    }

    public void setNettyRequest(FullHttpRequest request) {
        this.nettyRequest = request;
    }

    public void setNettyResponse(io.netty.handler.codec.http.HttpResponse response) {
        this.nettyResponse = response;
    }

    // ********************************************************
    // Methods specific to reading/writing of bodies
    // ********************************************************

    /**
     * Reset the variable on the outoing message sent state to be the default.
     *
     */
    final public void resetMsgSentState() {
        this.msgSentState = STATE_NONE;
    }

    /**
     * Reset the variable on the incoming message parsed state to the default
     *
     */
    final public void resetMsgParsedState() {
        this.msgParsedState = STATE_NONE;
    }

    /**
     * Query whether or not the body has been completed read yet.
     *
     * @return boolean
     */
    final protected boolean isBodyComplete() {
        return STATE_FULL_MESSAGE == this.msgParsedState;
    }

    /**
     * Set the body complete flag to true when we've finished reading it.
     *
     */
    private void setBodyComplete() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setBodyComplete() called");
        }
        this.msgParsedState = STATE_FULL_MESSAGE;
    }

    /**
     * Query whether we're in the middle of sending the message body.
     *
     * @return boolean
     */
    final public boolean isPartialBodySendState() {
        return STATE_PARTIAL_BODY == this.msgSentState;
    }

    /**
     * Query whether or not the entire outgoing message has been sent already.
     *
     * @return boolean
     */
    @Override
    final public boolean isMessageSent() {
        return STATE_FULL_MESSAGE == this.msgSentState;
    }

    /**
     * Set the flag to true that indicates this message has been fully sent.
     *
     */
    final protected void setMessageSent() {
        this.msgSentState = STATE_FULL_MESSAGE;
    }

    /**
     * Query whether the entire incoming message (headers and body) has
     * been completely read yet.
     *
     * @return boolean
     */
    @Override
    public boolean isIncomingMessageFullyRead() {

        // PK26519 - we may have temp buffers ready, check prior to looking at
        // the isBodyComplete method.
        if (!this.storage.isEmpty()) {
            return false;
        }

        // if we haven't already read the body, check to see if
        // we expect one to exist
        if (!isBodyComplete()) {
            // this doesn't give a definitive answer but it's the best
            // we can do without actually trying to read the body
            // if a body is expected then return false as we haven't read
            // it yet
            // @314871 - use the cached value
            return !isIncomingBodyValid();
        }

        // if body flag is true, then another channel has already
        // read the entire body (thus the message is fully read)
        return true;
    }

    /**
     * Returns whether or not the outgoing headers have been sent on this
     * connection.
     *
     * @return boolean
     */
    final public boolean headersSent() {
        return STATE_FULL_HEADERS <= this.msgSentState;
    }

    /**
     * Query whether the current write call is writing headers.
     *
     * @return boolean
     */
    public boolean writingHeaders() {
        return this.writingHeaders;
    }

    /**
     * Set the flag that we've sent the entire outgoing headers.
     *
     */
    final protected void setHeadersSent() {
        this.msgSentState = STATE_FULL_HEADERS;
    }

    /**
     * Query whether we've only sent just the headers (no body data).
     *
     * @return boolean
     */
    final public boolean isHeadersSentState() {
        return STATE_FULL_HEADERS == this.msgSentState;
    }

    /**
     * Query whether or not the incoming headers have been parsed completely.
     *
     * @return boolean
     */
    final public boolean headersParsed() {
        return STATE_FULL_HEADERS <= this.msgParsedState;
    }

    /**
     * Set the flag that we've parsed the entire incoming headers.
     *
     */
    final public void setHeadersParsed() {
        this.msgParsedState = STATE_FULL_HEADERS;
    }

    /**
     * Is a buffer ready to get (for async).
     *
     * @return boolean
     */
    protected boolean incomingBuffersReady() {
        return !this.storage.isEmpty();
    }

    /**
     * Set the readcallback to the one designated by the subclass.
     *
     * @param rc
     */
    final protected void setBodyRC(TCPReadCompletedCallback rc) {
        this.myBodyRC = rc;
    }

    /**
     * Query the body read callback.
     *
     * @return TCPReadCompletedCallback
     */
    private TCPReadCompletedCallback getBodyRC() {
        return this.myBodyRC;
    }

    // ********************************************************
    // connection specific methods
    // ********************************************************

    /**
     * Query whether this is part of a persistent connection (or
     * wants to be possibly). Returns false if connection header equals
     * CLOSE and true otherwise. Use the getConnection() or getHeader()
     * APIs for more specific information.
     *
     * @return boolean
     */
    @Override
    final public boolean isPersistent() {
        return this.bIsPersistent;
    }

    /**
     * Setter for subclasses to control persistence.
     *
     * @param flag
     */
    final public void setPersistent(boolean flag) {
        this.bIsPersistent = flag;
    }

    /**
     * Query whether this is part of a secure connection or not.
     *
     * @return boolean
     */
    @Override
    public boolean isSecure() {
        return (null != getSSLContext());
    }

    /**
     * Utility method to check whether this service context is part of an
     * inbound connection or not.
     *
     * @return boolean
     */
    abstract public boolean isInboundConnection();

    /**
     * Set the information about the incoming message for the auto-applied
     * decompression algorithm. That this should be set to IDENTITIY if no
     * change will be applied.
     *
     * @param val
     */
    private void setIncomingMsgEncoding(ContentEncodingValues val) {
        this.incomingMsgEncoding = val;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Incoming msg encoding: " + val.getName());
        }
    }

    /**
     * Set the information about the outgoing message for the auto-applied
     * compression algorithm. This should be set to IDENTITIY if no
     * change will be applied.
     *
     * @param val
     */
    private void setOutgoingMsgEncoding(ContentEncodingValues val) {
        this.outgoingMsgEncoding = val;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Outgoing msg encoding: " + val.getName());
        }
    }

    /**
     * Query whether or not the outgoing message has an encoding that should be
     * automatically applied to body buffers.
     *
     * @return boolean
     */
    private boolean isOutgoingMsgEncoded() {
        return !DEFAULT_ENCODING.equals(this.outgoingMsgEncoding);
    }

    /**
     * Query whether or not the outgoing message is configured for auto zlib
     * encoding by the service context. If this is true, then the body buffers
     * provided by the caller are expected to be plain text and the channel
     * will call the compression methods to encode them. If this is false,
     * which is the default, then the body buffers will be sent out as-is with
     * no compression applied.
     *
     * @return boolean
     */
    @Override
    public boolean isZlibEncoded() {
        return ContentEncodingValues.DEFLATE.equals(this.outgoingMsgEncoding);
    }

    /**
     * This method will signify whether the caller wishes the service context to
     * apply zlib compression to the outgoing buffers. The default is false,
     * which means that no compression will be applied to the outgoing buffers
     * regardless of the Content-Encoding header value. If this is set to
     * true, then the service context will take the plain text buffers provided
     * by the caller and apply the zlib compression to them prior to sending
     * them out. It will verify the Content-Encoding header is correctly set
     * to match the encoding.
     *
     * @param flag
     * @return boolean -- true means zlib compression was successfully enabled
     *         and false means it was not (i.e. it is not supported)
     */
    @Override
    public boolean setZlibEncoded(boolean flag) {
        if (headersSent()) {
            // once headers are sent, we can't change this
            return false;
        }
        if (flag) {
            setOutgoingMsgEncoding(ContentEncodingValues.DEFLATE);
        } else if (isZlibEncoded()) {
            // remove a previously set flag
            setOutgoingMsgEncoding(DEFAULT_ENCODING);
        }
        return true;
    }

    /**
     * Query whether this service context supports the automatic compression
     * of outgoing buffers using the zlib encoding format. If this returns
     * false, then any call to setZlibEncoded() will also return false.
     *
     * @return boolean
     */
    @Override
    public boolean isZlibEncodingSupported() {
        return true;
    }

    /**
     * Query whether or not the outgoing message is configured for auto gzip
     * encoding by the service context. If this is true, then the body buffers
     * provided by the caller are expected to be plain text and the channel
     * will call the compression methods to encode them. If this is false,
     * which is the default, then the body buffers will be sent out as-is with
     * no compression applied.
     *
     * @return boolean
     */
    @Override
    public boolean isGZipEncoded() {
        return ContentEncodingValues.GZIP.equals(this.outgoingMsgEncoding);
    }

    /**
     * This method will signify whether the caller wishes the service context to
     * apply gzip compression to the outgoing buffers. The default is false,
     * which means that no compression will be applied to the outgoing buffers
     * regardless of the Content-Encoding header value. If this is set to
     * true, then the service context will take the plain text buffers provided
     * by the caller and apply the gzip compression to them prior to sending
     * them out. It will verify the Content-Encoding header is correctly set
     * to match the encoding.
     *
     * @param flag
     * @return boolean -- true means gzip compression was successfully enabled
     *         and false means it was not (i.e. it is not supported)
     */
    @Override
    public boolean setGZipEncoded(boolean flag) {
        if (headersSent()) {
            // once headers have been sent, we can't change this
            return false;
        }
        if (flag) {
            setOutgoingMsgEncoding(ContentEncodingValues.GZIP);
        } else if (isGZipEncoded()) {
            // turn off previously enabled setting
            setOutgoingMsgEncoding(DEFAULT_ENCODING);
        }
        return true;
    }

    /**
     * Query whether this service context supports the automatic compression
     * of outgoing buffers using the gzip encoding format. If this returns
     * false, then any call to setGZipEncoded() will also return false.
     *
     * @return boolean
     */
    @Override
    public boolean isGZipEncodingSupported() {
        return true;
    }

    /**
     * Query whether or not the outgoing message is configured for auto x-gzip
     * encoding by the service context. If this is true, then the body buffers
     * provided by the caller are expected to be plain text and the channel
     * will call the compression methods to encode them. If this is false,
     * which is the default, then the body buffers will be sent out as-is with
     * no compression applied.
     *
     * @return boolean
     */
    @Override
    public boolean isXGZipEncoded() {
        return ContentEncodingValues.XGZIP.equals(this.outgoingMsgEncoding);
    }

    /**
     * This method will signify whether the caller wishes the service context to
     * apply x-gzip compression to the outgoing buffers. The default is false,
     * which means that no compression will be applied to the outgoing buffers
     * regardless of the Content-Encoding header value. If this is set to
     * true, then the service context will take the plain text buffers provided
     * by the caller and apply the x-gzip compression to them prior to sending
     * them out. It will verify the Content-Encoding header is correctly set
     * to match the encoding.
     *
     * @param flag
     * @return boolean -- true means x-gzip compression was successfully enabled
     *         and false means it was not (i.e. it is not supported)
     */
    @Override
    public boolean setXGZipEncoded(boolean flag) {
        if (headersSent()) {
            // once headers have been sent, we can't change this
            return false;
        }
        if (flag) {
            setOutgoingMsgEncoding(ContentEncodingValues.XGZIP);
        } else if (isXGZipEncoded()) {
            // turn off previously enabled setting
            setOutgoingMsgEncoding(DEFAULT_ENCODING);
        }
        return true;
    }

    /**
     * Query whether this service context supports the automatic compression
     * of outgoing buffers using the x-gzip encoding format. If this returns
     * false, then any call to setXGZipEncoded() will also return false.
     *
     * @return boolean
     */
    @Override
    public boolean isXGZipEncodingSupported() {
        return true;
    }

    /**
     * Check whether the current body request was for the raw data or the
     * modified (uncompressed, etc) data.
     *
     * @return boolean
     */
    final protected boolean isRawBody() {
        return this.bIsRawBody;
    }

    /**
     * Set the flag on whether the current request is for the raw unedited
     * body data.
     *
     * @param flag
     */
    final protected void setRawBody(boolean flag) {
        this.bIsRawBody = flag;
    }

    /**
     * Check whether the outgoing body is being sent in pieces (chunked).
     *
     * @return boolean
     */
    final public boolean isPartialBody() {
        return this.bIsPartialBody;
    }

    /**
     * Setter for subclasses to set the partial body flag.
     *
     * @param flag
     */
    final protected void setPartialBody(boolean flag) {
        this.bIsPartialBody = flag;
    }

    /**
     * Query whether an outgoing body is valid for this particular message.
     *
     * @return boolean
     */
    final protected boolean isOutgoingBodyValid() {
        return this.bIsOutgoingBodyValid;
    }

    /**
     * Set the flag on whether a body is allowed with the outgoing message.
     *
     * @param flag
     */
    private void setOutgoingBodyValid(boolean flag) {
        this.bIsOutgoingBodyValid = flag;
    }

    /**
     * Query whether this particular read/write is a forced async call.
     *
     * @return boolean
     */
    final protected boolean isForceAsync() {
        return this.bForceAsync;
    }

    /**
     * Set the flag on whether the next read/write should be forced async.
     *
     * @param flag
     */
    final protected void setForceAsync(boolean flag) {
        this.bForceAsync = flag;
    }

    /**
     * Query whether this service context owns the response message in the
     * connection.
     *
     * @return boolean
     */
    final protected boolean isResponseOwner() {
        return this.bIsResponseOwner;
    }

    /**
     * Set the flag on whether this service context owns the response message
     * or not.
     *
     * @param flag
     */
    final protected void setResponseOwner(boolean flag) {
        this.bIsResponseOwner = flag;
    }

    /**
     * Set the local response object value to the input message.
     *
     * @param msg
     */
    final protected void setMyResponse(HttpResponseMessageImpl msg) {
        this.myResponse = msg;
    }

    /**
     * Query the local response object value.
     *
     * @return HttpResponseMessageImpl
     */
    final protected HttpResponseMessageImpl getMyResponse() {
        return this.myResponse;
    }

    /**
     * Query whether this service context owns the request message in the
     * connection.
     *
     * @return boolean
     */
    final protected boolean isRequestOwner() {
        return this.bIsRequestOwner;
    }

    /**
     * Set the flag on whether this service context owns the request message
     * or not.
     *
     * @param flag
     */
    final protected void setRequestOwner(boolean flag) {
        this.bIsRequestOwner = flag;
    }

    /**
     * Set the local request object value to the input message.
     *
     * @param msg
     */
    final protected void setMyRequest(HttpRequestMessageImpl msg) {
        this.myRequest = msg;
    }

    /**
     * Query the local request object value.
     *
     * @return HttpRequestMessageImpl
     */
    final protected HttpRequestMessageImpl getMyRequest() {
        return this.myRequest;
    }

    /**
     * This method will inform this service context to re-take ownership of
     * the request message. This is useful in a proxy-type environment where
     * individual messages may be passed from service context to service
     * context and at some point need to be reset back to the originator.
     */
    @Override
    public void resetRequestOwnership() {
        if (null != getMyRequest()) {
            getMyRequest().setOwner(this);
        }
    }

    /**
     * This method will inform this service context to re-take ownership of
     * the response message. This is useful in a proxy-type environment where
     * individual messages may be passed from service context to service
     * context and at some point need to be reset back to the originator.
     */
    @Override
    public void resetResponseOwnership() {
        if (null != getMyResponse()) {
            getMyResponse().setOwner(this);
        }
    }

    /**
     * Query the current parsing state variable for reading chunked encoding
     * lengths.
     *
     * @return int
     */
    private int getChunkLengthParsingState() {
        return this.chunkLengthParseState;
    }

    /**
     * Set the chunk length parsing state variable to the input value.
     *
     * @param val
     */
    private void setChunkLengthParsingState(int val) {
        this.chunkLengthParseState = val;
    }

    // ********************************************************
    // service context specific methods
    // ********************************************************

    /**
     * Initialize local variables.
     *
     * @param tsc
     * @param hcc
     */
    protected void init(TCPConnectionContext tsc, HttpChannelConfig hcc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "init");
        }
        // always store the configuration object
        setHttpConfig(hcc);

        // during discrimination, this will be null and we just skip
        // the rest of init
        if (null != tsc) {
            this.myTSC = tsc;
            // calling getLocalPort() primes the local address on
            // the Socket object, do it first
            try {
                setLocalPort(getTSC().getLocalPort());
                setRemotePort(getTSC().getRemotePort());
                setLocalAddr(getTSC().getLocalAddress());
                setRemoteAddr(getTSC().getRemoteAddress());
            } catch (Throwable t) {
                // check if we're on an HTTP2 connection in the closed state
                HttpInboundServiceContextImpl context = (HttpInboundServiceContextImpl) this;
                HttpInboundLink link = context.getLink();
                boolean h2Closing = false;
                if (link instanceof H2HttpInboundLinkWrap) {
                    H2HttpInboundLinkWrap h2link = (H2HttpInboundLinkWrap) link;
                    if (h2link.muxLink.checkIfGoAwaySendingOrClosing()) {
                        h2Closing = true;
                    }
                }
                if (!h2Closing) {
                    FFDCFilter.processException(t, getClass().getName() + ".init", "1", this);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Received exception from JDK socket calls; " + t);
                }
            }
            // set the default timeouts
            this.myReadTimeout = getHttpConfig().getReadTimeout();
            this.myWriteTimeout = getHttpConfig().getWriteTimeout();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "init");
        }
    }

    public void init(ChannelHandlerContext context) {
        this.setNettyContext(context);
        InetSocketAddress local = (InetSocketAddress) context.channel().localAddress();
        InetSocketAddress remote = (InetSocketAddress) context.channel().remoteAddress();

        setLocalPort(local.getPort());
        setRemotePort(remote.getPort());
        setLocalAddr(local.getAddress());
        setRemoteAddr(remote.getAddress());

        this.myReadTimeout = getHttpConfig().getReadTimeout();
        this.myWriteTimeout = getHttpConfig().getWriteTimeout();

    }

    public void reinit(TCPConnectionContext tcc) {
        this.myTSC = tcc;

        try {
            parsingComplete();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception caught during reinit: " + e);
            }
        }
    }

    /**
     * Perform necessary cleanup on this object when it is no longer needed.
     *
     */
    public void destroy() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy");
        }
        while (!this.allocatedBuffers.isEmpty()) {
            this.allocatedBuffers.removeFirst().release();
        }
        this.lastHeaderBuffer = HeaderStorage.NOTSET;

        // destroy the request message only if we're the "owner"
        if (null != getMyRequest() && isRequestOwner()) {
            getMyRequest().destroy();
        }
        setMyRequest(null);
        // destroy the response message only if we're the "owner"
        if (null != getMyResponse() && isResponseOwner()) {
            getMyResponse().destroy();
        }
        setMyResponse(null);

        this.myTSC = null;
        setVC(null);
        setAppWriteCallback(null);
        setAppReadCallback(null);
        releaseReadBuffers();
        if (null != this.buffChunkHeader) {
            this.buffChunkHeader.release();
            this.buffChunkHeader = null;
        }
        if (null != this.buffChunkTrailer) {
            this.buffChunkTrailer.release();
            this.buffChunkTrailer = null;
        }
        if (null != this.acceptableEncodings) {
            this.acceptableEncodings.clear();
            this.acceptableEncodings = null;
        }

        if (null != this.unacceptedEncodings) {
            this.unacceptedEncodings.clear();
            this.unacceptedEncodings = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy");
        }
    }

    /**
     * Clear this service context for re-use.
     *
     */
    @Override
    public void clear() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "clear");
        }

        while (!this.allocatedBuffers.isEmpty()) {
            this.allocatedBuffers.removeFirst().release();
        }
        this.lastHeaderBuffer = HeaderStorage.NOTSET;

        // clear the request message if we're the "owner" otherwise clear
        // our reference to it
        if (null != this.myRequest) {
            if (this.bIsRequestOwner) {
                this.myRequest.clear();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Request not mine, skipping clear()");
                }
                this.myRequest = null;
            }
        }
        this.bIsRequestOwner = true;
        // clear the response message if we're the "owner" otherwise clear
        // our reference to it
        if (null != this.myResponse) {
            if (this.bIsResponseOwner) {
                this.myResponse.clear();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Response not mine, skipping clear()");
                }
                this.myResponse = null;
            }
        }
        this.bIsResponseOwner = true;

        this.msgSentState = STATE_NONE;
        this.msgParsedState = STATE_NONE;
        this.writingHeaders = false;
        this.bIsPersistent = true;
        this.bIsOutgoingBodyValid = true;
        this.bIsIncomingBodyValid = false;
        this.bIsBodyExpected = false;
        this.bIsPartialBody = false;
        this.outgoingMsgEncoding = DEFAULT_ENCODING;
        this.incomingMsgEncoding = DEFAULT_ENCODING;
        this.bIsRawBody = false;
        this.unparsedDataRemaining = NO_MORE_DATA;
        this.bIsChunked = false;
        this.myContentLength = HeaderStorage.NOTSET;
        clearStorage();
        clearTempStorage();
        this.amountBeingRead = 0;
        this.numBytesWritten = 0L;
        this.incomingMsgSize = 0L;
        this.savedChunkLength = HeaderStorage.NOTSET;
        this.oldLimit = 0;
        this.shouldModify = true;
        clearPendingByteBuffers();
        this.bIsJITRead = false;
        this.reqMethod = null;
        this.reqVersion = null;
        this.chunkLengthParseState = GenericConstants.PARSING_NOTHING;
        this.bParsingTrailers = false;
        this.cyclesAboveDecompressionRatio = 0;
        if (null != this.decompressHandler) {
            this.decompressHandler.close();
            this.decompressHandler = null;
        }
        if (null != this.compressHandler) {
            Iterator<WsByteBuffer> it = this.compressHandler.finish().iterator();
            while (it.hasNext()) {
                it.next().release();
            }
            this.compressHandler = null;
        }

        if (this.acceptableEncodings != null) {
            this.acceptableEncodings.clear();

        }
        if (this.unacceptedEncodings != null) {
            this.unacceptedEncodings.clear();
        }
        this.bStarEncodingParsed = false;
        this.preferredEncoding = null;

        this.isFinalWrite = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "clear");
        }
    }

    /**
     * Query the stored value of the request version of this service context.
     *
     * @return VersionValues
     */
    public VersionValues getRequestVersion() {
        if (null == this.reqVersion) {
            return getRequest().getVersionValue();
        }
        return this.reqVersion;
    }

    /**
     * Get the start time of response in milliseconds
     *
     * @return long
     */
    public long getResponseStartTime() {
        return this.responseStartTime;
    }

    /**
     * Set the stored request version.
     *
     * @param val
     */
    final public void setRequestVersion(VersionValues val) {
        this.reqVersion = val;
    }

    /**
     * Query the stored request method value.
     *
     * @return MethodValues
     */
    public MethodValues getRequestMethod() {
        if (null == this.reqMethod) {
            return getRequest().getMethodValue();
        }
        return this.reqMethod;
    }

    /**
     * Store the request method information.
     *
     * @param val
     */
    final public void setRequestMethod(MethodValues val) {
        this.reqMethod = val;
    }

    /**
     * The timeout used when reading data from the incoming message
     * can be changed from the default channel timeout by using this
     * method. This stays in effect until changed again or the
     * connection is closed.
     * <p>
     * Input time is expected to be in milliseconds.
     *
     * @param time
     *                 (must not be less than HttpChannelConfig.MIN_TIMEOUT)
     * @throws IllegalArgumentException
     *                                      (if too low)
     */
    @Override
    public void setReadTimeout(int time) throws IllegalArgumentException {
        if (time < HttpConfigConstants.MIN_TIMEOUT) {
            throw new IllegalArgumentException("Timeout too low (" + time + ")");
        }

        this.myReadTimeout = time;
    }

    /**
     * The timeout used when writing data for the outgoing message
     * can be changed from the default channel timeout by using this
     * method. This stays in effect until changed again or the
     * connection is closed.
     * <p>
     * Input time is expected to be in milliseconds.
     *
     * @param time
     *                 (must not be less than HttpChannelConfig.MIN_TIMEOUT)
     * @throws IllegalArgumentException
     *                                      (if too low)
     */
    @Override
    public void setWriteTimeout(int time) throws IllegalArgumentException {
        if (time < HttpConfigConstants.MIN_TIMEOUT) {
            throw new IllegalArgumentException("Timeout too low (" + time + ")");
        }

        this.myWriteTimeout = time;
    }

    /**
     * Query the current value of the read timeout for incoming message data.
     * The integer returned is the timeout in milliseconds.
     *
     * @return int
     */
    @Override
    final public int getReadTimeout() {
        return this.myReadTimeout;
    }

    /**
     * Query the current value of the write timeout for outgoing message data.
     * The integer returned is the timeout in milliseconds.
     *
     * @return int
     */
    @Override
    final public int getWriteTimeout() {
        return this.myWriteTimeout;
    }

    /**
     * Query the given message to determine how it affects the connection's
     * persistence flag.
     *
     * @param msg
     */
    protected void updatePersistence(HttpBaseMessageImpl msg) {

        if (!isPersistent()) {
            // if it's already false, then you can't change it to true
            // default on new connection is true
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "updatePersistence: already false");
            }
            return;
        }

        if (this.myVC instanceof H2VirtualConnectionImpl) {
            // if it's an HTTP2 connection, do not persist.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "HTTP/2.0 connection: setting persistance to false");
            }
            this.setPersistent(false);
            return;
        }

        if (msg.isIncoming() && getHttpConfig().isServantRegion()) {
            // on z/OS check the incoming metadata to see if the CR wants to
            // force the response closed or not, but only check when incoming
            String value = (String) getVC().getStateMap().get(HttpConstants.SESSION_PERSISTENCE);
            if ("false".equalsIgnoreCase(value)) {
                setPersistent(false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "z/OS CR forced non-persistence");
                }
                return;
            }
        }
        // check the connection header for "close" first
        if (msg.isCloseSet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Message contains Close value");
            }
            setPersistent(false);
        }
        // if connection header not present, check version and the config
        // for default outgoing keep-alive
        else if (!msg.isKeepAliveSet()) {

            // Incoming messages ignore the configuration
            if (msg.isIncoming()) {
                if (msg.getVersionValue().equals(VersionValues.V10)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Incoming 1.0 msg has no connection header");
                    }
                    setPersistent(false);
                }
                // default is true, so no need to change it for 1.1 msgs
                // as the lack of Connection means Keep-Alive in 1.1
            } else {
                // outgoing messages base their persistence on the configuration
                setPersistent(getHttpConfig().isKeepAliveEnabled());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Setting persistence based on configuration: " + isPersistent());
                }
            }
        }

        if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.DEBUG)) {
            getHttpConfig().getDebugLog().log(DebugLog.Level.DEBUG, HttpMessages.MSG_CONN_PERSIST + isPersistent(), this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updatePersistence updated: " + isPersistent());
        }
    }

    /**
     * Update the "content-length" vs "chunked encoding" flags for the inbound
     * message by querying the given message headers.
     *
     * @param msg
     */
    private void updateBodyFlags(HttpBaseMessageImpl msg) {

        // only valid for an inbound message
        if (!msg.isIncoming()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "updateBodyFlags skipping outgoing message: " + msg);
            }
            return;
        }

        setIsChunkedEncoding(msg.isChunkedEncodingSet());
        if (isChunkedEncoding()) {
            // 231634.1 - guard against someone sending both CL and chunked
            // RFC 2616 (4.4) says Transfer-Encoding chunked takes precedence
            if (HeaderStorage.NOTSET != msg.getContentLength()) {
                // remove the header that is present and only use the chunked
                // encoding marker
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Removing Content-Length header of " + msg.getContentLength() + " and only using chunked-encoding");
                }
                msg.removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);
                // PK12319: turn off persistence to avoid any security attacks
                // through malformed length headers
                // PK53193 - check whether the config has this option enabled or not
                if (getHttpConfig().isRequestSmugglingProtectionEnabled()) {
                    setPersistent(false);
                }
            }
        }
        // set the reference CL based on the msg (could have been changed above)
        setContentLength(msg.getContentLength());

        if (0 == getContentLength()) {
            // not really needed since bodyValid will be false
            setBodyComplete();
        }
        // save whether a body is allowed, as proxy env might be changing the
        // message itself
        this.bIsIncomingBodyValid = msg.isBodyAllowed();
        // @314871 - save this information now
        this.bIsBodyExpected = msg.isBodyExpected();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateBodyFlags: CL: " + getContentLength() + " isChunked: " + isChunkedEncoding() + " bodyValid: " + isIncomingBodyValid() + " bodyExpected: "
                         + this.bIsBodyExpected);
        }
    }

    /**
     * When a body is not allowed to be sent on an outgoing message, certain
     * headers need modification -- except for an outgoing Response to a HEAD
     * request.
     *
     * @param msg
     */
    protected void updateBodyLengthHeaders(HttpBaseMessageImpl msg) {
        if (!msg.shouldUpdateBodyHeaders()) {
            // message does not want us to update the length headers
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Msg not allowing body header changes: " + msg);
            }
            return;
        }
        // if no body is allowed then set the content-length so that no
        // client, like Mozilla, will try to read for one
        if (0 != msg.getContentLength()) {
            msg.setContentLength(0);
        }
        if (msg.isChunkedEncodingSet()) {
            msg.removeTransferEncoding(TransferEncodingValues.CHUNKED);
            msg.commitTransferEncoding();
        }
        //Start PI35277
        if (getHttpConfig().shouldRemoveCLHeaderInTempStatusRespRFC7230compat() && msg instanceof HttpResponseMessageImpl) {
            if (((HttpResponseMessageImpl) msg).isTemporaryStatusCode() || ((HttpResponseMessageImpl) msg).getStatusCode() == StatusCodes.NO_CONTENT) {

                msg.removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Status code 1xx or 204 found, not sending content-length");
                }
            }
        } //End PI35277
    }

    /**
     * When the incoming message headers have been fully parsed, we need to
     * figure out if any content encoding is set.
     *
     * @param msg
     */
    private void updateIncomingEncodingFlags(HttpBaseMessageImpl msg) {
        ContentEncodingValues enc = msg.getOutermostEncoding();
        if (null != enc) {
            // check for the ones we can handle
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Msg outermost encoding: " + enc);
            }
            if (ContentEncodingValues.DEFLATE.equals(enc) || ContentEncodingValues.GZIP.equals(enc) || ContentEncodingValues.XGZIP.equals(enc)) {
                setIncomingMsgEncoding(enc);
            } else {
                setIncomingMsgEncoding(DEFAULT_ENCODING);
            }
        } else {
            setIncomingMsgEncoding(DEFAULT_ENCODING);
        }
    }

    /**
     * Query whether the incoming message has the chunked transfer-encoding
     * header set.
     *
     * @return boolean
     */
    public boolean isChunkedEncoding() {
        // PK18799 - expose for callback usage
        return this.bIsChunked;
    }

    /**
     * Set the flag on whether the incoming message body is chunked encoded
     * or not.
     *
     * @param flag
     */
    private void setIsChunkedEncoding(boolean flag) {
        this.bIsChunked = flag;
    }

    /**
     * Query whether the incoming message has the content-length header set.
     *
     * @return boolean
     */
    public boolean isContentLength() {
        // PK18799 - expose for callback usage
        return (HeaderStorage.NOTSET != getContentLength());
    }

    /**
     * Query the content-length value for the incoming message.
     *
     * @return int
     */
    final protected long getContentLength() {
        return this.myContentLength;
    }

    /**
     * Set the local content-length value to the input.
     *
     * @param length
     */
    private void setContentLength(long length) {
        this.myContentLength = length;
    }

    /**
     * Query whether or not the incoming message could have a body on it as
     * well.
     *
     * @return boolean
     */
    final protected boolean isIncomingBodyValid() {
        return this.bIsIncomingBodyValid;
    }

    /**
     * Query whether the incoming message is expected to have a body. This is
     * not quite the same as whether one is allowed, but rather whether one
     * seems to be present.
     *
     * @return boolean
     */
    final protected boolean isIncomingBodyExpected() {
        return this.bIsBodyExpected;
    }

    /**
     * Method to grab any existing buffers from the device channel below us
     * and set up for the next access of those buffers.
     *
     * @return boolean (true means buffers were loaded)
     */
    private boolean loadReadBuffers() {
        boolean rc = false;
        WsByteBuffer[] list = getTSC().getReadInterface().getBuffers();
        if (null != list && 0 != list.length) {
            // ensure we have a list of 1 or more elements
            this.parseBufferIndex = 0;
            this.myParseBuffers = list;
            rc = true;
        } else {
            this.parseBufferIndex = -1;
            this.myParseBuffers = null;
        }
        getTSC().getReadInterface().setBuffers(null);
        return rc;
    }

    /**
     * Get access to the next read buffer from the device channel. This will
     * return null if no buffer is available.
     *
     * @return WsByteBuffer
     */
    protected WsByteBuffer getNextReadBuffer() {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (-1 == this.parseBufferIndex) {
            // nothing in the list or we've handed them all out. Check to see
            // if any are sitting ready on the readSC
            if (!loadReadBuffers()) {
                setReadBuffer(null);
                return null;
            }
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Read " + this.myParseBuffers.length + " buffers from device.");
            }
        }
        // otherwise return the one we're pointing to and update the index
        setReadBuffer(this.myParseBuffers[this.parseBufferIndex]);
        configurePostReadBuffer(getReadBuffer());

        if (isJITRead()) {
            // was a JIT read, save "ownership" of the buffer
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Saving JIT buffer");
            }
            this.allocatedBuffers.add(getReadBuffer());
        }
        this.parseBufferIndex++;
        if (this.parseBufferIndex == this.myParseBuffers.length) {
            // just handed out the last one
            this.parseBufferIndex = -1;
        }
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "Read buffer: " + getReadBuffer());
        }
        return getReadBuffer();
    }

    /**
     * Query whether or not there is ready data available to parse.
     *
     * @return true
     */
    public boolean isReadDataAvailable() {
        if (null != getReadBuffer() && getReadBuffer().hasRemaining()) {
            // existing data in current buffer
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Data exists in buffer: " + getReadBuffer());
            }
            disableBufferModification();
            return true;
        }
        // check to see if there are other buffers at all
        if (-1 == this.parseBufferIndex) {
            // there are no buffers yet
            return false;
        }
        // otherwise try to get the next one on the list
        if (this.parseBufferIndex < this.myParseBuffers.length) {
            // other buffers exist
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Secondary read buffers exist.");
            }
            getNextReadBuffer();
            return true;
        }
        return false;
    }

    /**
     * When about to start a read, check to see whether we have data available
     * or empty space in the current buffer.
     *
     * @param size
     * @return boolean
     */
    public boolean isReadSpaceAvailable(int size) {
        if (null != getReadBuffer()) {
            int cap = getReadBuffer().capacity();
            int availSpace = cap - getReadBuffer().limit();
            if (0 == availSpace) {
                // no free space at all
                return false;
            }
            if (cap < size) {
                // this buffer isn't worth using as it is too small and multiple
                // reads is more expensive than the buffer allocation
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Ignoring smaller buffer, capacity=" + cap + " target size=" + size);
                }
                return false;
            }

            // test to see if we have a certain minimum amount of free space to
            // decide whether it is worthwhile re-using this buffer
            int min = (HttpConfigConstants.MIN_BUFFER_SIZE > size) ? size : HttpConfigConstants.MIN_BUFFER_SIZE;
            if (min > availSpace) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Ignoring buffer with too little free: " + availSpace);
                }
                return false;
            }
            // current buffer has the minimum requirements for re-use
            return true;
        }
        // no current buffer
        return false;
    }

    /**
     * Get access to the current read buffer, but do not increment past it yet.
     *
     * @return WsByteBuffer, null if no buffer is ready
     */
    public WsByteBuffer getReadBuffer() {
        return this.currentReadBB;
    }

    /**
     * Set the current read buffer to the input one.
     *
     * @param buffer
     */
    public void setReadBuffer(WsByteBuffer buffer) {
        this.currentReadBB = buffer;
    }

    /**
     * After a read has completed into the given buffer, this method is used to
     * prepare it for handling of the new data. It will set positions and limits
     * according to the scenario currently running.
     *
     * @param buffer
     */
    public void configurePostReadBuffer(WsByteBuffer buffer) {
        if (!this.shouldModify) {
            return;
        }
        if (0 < getOldLimit()) {
            buffer.limit(buffer.position());
            buffer.position(getOldLimit());
            setOldLimit(0);
        } else {
            buffer.flip();
        }
        // we just updated the buffer so don't let any other code do it again
        disableBufferModification();
    }

    /**
     * Using the given buffer, this will configure the service context to pick
     * up with the buffer once the read has finished.
     *
     * @param buffer
     */
    public void configurePreReadBuffer(WsByteBuffer buffer) {
        setOldLimit(buffer.limit());
        buffer.position(getOldLimit());
        buffer.limit(buffer.capacity());
    }

    /**
     * Method to release any unused read buffers left over.
     */
    private void releaseReadBuffers() {
        if (-1 != this.parseBufferIndex) {
            for (int i = this.parseBufferIndex; i < this.myParseBuffers.length; i++) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Releasing read buffer: " + this.myParseBuffers[i]);
                }
                this.myParseBuffers[i].release();
            }
            this.myParseBuffers = null;
            this.parseBufferIndex = -1;
        }
    }

    /**
     * Query whether the last read done was a JIT one.
     *
     * @return boolean
     */
    final public boolean isJITRead() {
        return this.bIsJITRead;
    }

    /**
     * Set up the environment for a JIT read of the input size.
     *
     * @param size
     */
    public void setupJITRead(int size) {
        this.bIsJITRead = true;
        enableBufferModification();
        setOldLimit(0);
        getTSC().getReadInterface().setJITAllocateSize(size);
        getTSC().getReadInterface().setBuffers(null);
        setReadBuffer(null);
    }

    /**
     * Set up the environment for a non-JIT read.
     */
    public void setupNonJITRead() {
        this.bIsJITRead = false;
        enableBufferModification();
        getTSC().getReadInterface().setJITAllocateSize(0);
        getTSC().getReadInterface().setBuffer(getReadBuffer());
    }

    /**
     * Method used during pipeling to indicate that we should not modify the
     * read buffer once the read "finishs".
     */
    final public void disableBufferModification() {
        this.shouldModify = false;
    }

    /**
     * Reset the buffer modification allowed flag to the default value.
     */
    final public void enableBufferModification() {
        this.shouldModify = true;
    }

    /**
     * Clear the array of pending byte buffers.
     *
     */
    private void clearPendingByteBuffers() {

        for (int i = 0; i < this.pendingBufferStop; i++) {
            this.myPendingBuffers[i] = null;
        }
        this.pendingBufferStart = 0;
        this.pendingBufferStop = 0;
    }

    /**
     * Get access to the pending buffer list.
     *
     * @return WsByteBuffer[]
     */
    final protected WsByteBuffer[] getPendingBuffers() {
        return this.myPendingBuffers;
    }

    /**
     * Set the pending buffer list to the input.
     *
     * @param list
     */
    final protected void setPendingBuffers(WsByteBuffer[] list) {
        this.myPendingBuffers = list;
    }

    /**
     * Query the index in the outgoing buffers of the starting position.
     *
     * @return int
     */
    final protected int getPendingStart() {
        return this.pendingBufferStart;
    }

    /**
     * Set the start point in the outgoing buffers to the input index.
     *
     * @param start
     */
    final protected void setPendingStart(int start) {
        this.pendingBufferStart = start;
    }

    /**
     * Query the index in the outgoing buffers for the "next" empty spot.
     *
     * @return int
     */
    final protected int getPendingStop() {
        return this.pendingBufferStop;
    }

    /**
     * Set the stop position in the outgoing buffers to the input value.
     *
     * @param stop
     */
    final protected void setPendingStop(int stop) {
        this.pendingBufferStop = stop;
    }

    /**
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#getNumBytesWritten()
     */
    @Override
    public long getNumBytesWritten() {
        return this.numBytesWritten;
    }

    /**
     * Add the input number to the running counter of the number of bytes
     * written out on this message.
     *
     * @param num
     */
    final protected void addBytesWritten(long num) {
        this.numBytesWritten += num;
    }

    /**
     * Reset the counter keeping track of the number of bytes written out.
     *
     */
    final protected void resetBytesWritten() {
        this.numBytesWritten = 0;
    }

    /**
     * Query the size of the incoming message as discovered so far. This would
     * include any headers parsed so far, plus any knowledge of the body at this
     * time (i.e a Content-Length header was parsed so we know it now, or a
     * chunk block length has been read, etc).
     *
     * @return long
     */
    protected long queryIncomingMsgSize() {
        return this.incomingMsgSize;
    }

    /**
     * Add the specified size to the ongoing counter of number bytes read for
     * the incoming message.
     *
     * @param num
     */
    protected void addToIncomingMsgSize(long num) {
        this.incomingMsgSize += num;
    }

    /**
     * Once we've added the input additional bytes to the size of the message,
     * perform any checks necessary.
     *
     * @param addition
     * @throws MessageTooLargeException
     */
    protected void checkIncomingMessageLimit(long addition) throws MessageTooLargeException {
        // nothing by default
    }

    /**
     * Grow and copy the existing the pending output list of buffers to the new
     * input size.
     *
     * @param size
     */
    private void growPendingArray(int size) {
        WsByteBuffer[] tempNew = new WsByteBuffer[size];
        System.arraycopy(this.myPendingBuffers, 0, tempNew, 0, this.pendingBufferStop);
        this.myPendingBuffers = tempNew;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Increased pending list to " + size);
        }
    }

    /**
     * Add this buffer to the pending byte buffer array.
     *
     * @param wsbb
     */
    private void addToPendingByteBuffer(WsByteBuffer wsbb) {

        if (this.pendingBufferStop == this.myPendingBuffers.length) {
            growPendingArray(this.pendingBufferStop + PENDING_BUFFER_MIN_GROWTH_SIZE);
        }
        this.myPendingBuffers[this.pendingBufferStop] = wsbb;
        this.pendingBufferStop++;
    }

    /**
     * Add the list of outgoing buffers, stopping at the input length of
     * that list.
     *
     * @param list
     * @param length
     */
    private void addToPendingByteBuffer(WsByteBuffer[] list, int length) {
        int newsize = this.pendingBufferStop + length;
        if (newsize >= this.myPendingBuffers.length) {
            if (length < PENDING_BUFFER_MIN_GROWTH_SIZE) {
                newsize = this.myPendingBuffers.length + PENDING_BUFFER_MIN_GROWTH_SIZE;
            }
            growPendingArray(newsize);
        }
        System.arraycopy(list, 0, this.myPendingBuffers, this.pendingBufferStop, length);
        this.pendingBufferStop += length;
    }

    /**
     * Adds body buffers to the queue to be written...inserts chunk length
     * markers if appropriate.
     *
     * @param wsbb
     * @param msg
     */
    private void formatBody(WsByteBuffer[] wsbb, HttpBaseMessageImpl msg) {

        if (null == wsbb || null == msg) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Leaving formatBody, wsbb: " + wsbb + " msg: " + msg);
            }
            return;
        }
        // determine the amount of data going out in this list of buffers
        int length = 0;
        int index = 0;
        for (; index < wsbb.length && null != wsbb[index]; index++) {
            length += wsbb[index].remaining();
        }
        // 313074: handle empty buffers as well as null ones
        if (0 == length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring empty body buffers");
            }
            return;
        }

        if (msg instanceof HttpResponseMessageImpl) {
            HttpInboundServiceContextImpl context = (HttpInboundServiceContextImpl) this;

            if (context.getLink() instanceof H2HttpInboundLinkWrap) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "formatBody: On an HTTP/2.0 connection, creating DATA frames");
                }
                H2HttpInboundLinkWrap link = (H2HttpInboundLinkWrap) context.getLink();

                // if all expected body bytes will be written out, set the end of stream flag
                addBytesWritten(length);
                boolean addEndOfStream = false;
                if (msg.getContentLength() == getNumBytesWritten()) {
                    addEndOfStream = true;
                }

                ArrayList<Frame> bodyFrames = link.prepareBody(wsbb, length, addEndOfStream);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "formatBody: On an HTTP/2.0 connection, adding DATA frames to be written");
                }

                framesToWrite.addAll(bodyFrames);
                // save the amount of data written inside actual body

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "formatBody: total bytes now : " + getNumBytesWritten());
                }

                return;
            }
        }

        boolean doChunkWork = !isRawBody() && msg.isChunkedEncodingSet();
        if (doChunkWork) {
            // prepend "chunk length CRLF" before their data
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating a chunk of length " + length);
            }
            byte[] encodedLength = asChunkedLength(length);
            addToPendingByteBuffer(createChunkHeader(encodedLength));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "formatBody: Adding " + index + " app buffers to write queue");
        }
        // save their non-null data buffers
        addToPendingByteBuffer(wsbb, index);

        if (doChunkWork) {
            // need to append a CRLF after their data
            WsByteBuffer trailer = createChunkTrailer();
            trailer.limit(BNFHeaders.EOL.length);
            addToPendingByteBuffer(trailer);
        }

        // save the amount of data written inside actual body
        addBytesWritten(length);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "formatBody: total bytes now : " + getNumBytesWritten());
        }
    }

    /**
     * Format the headers for this object. This adds the header buffers to
     * the list to be written.
     *
     * @param msg
     * @throws IOException
     */
    private void formatHeaders(HttpBaseMessageImpl msg, boolean complete) throws IOException {
        // PI36010 Start
        // Get the start time of the response only if access logging is enabled
        // and if it is an inbound connection
        if (getHttpConfig().isAccessLoggingEnabled() && isInboundConnection()) {

            this.responseStartTime = System.nanoTime();

        }
        // PI36010 End

        // check compression and set up the Content-Encoding header if need be
        if (null != this.compressHandler) {
            ContentEncodingValues ce = this.compressHandler.getContentEncoding();
            if (!ce.equals(msg.getOutermostEncoding())) {
                msg.appendContentEncoding(ce);
            }
        }
        if (!getHttpConfig().useNetty()) {
            // when formatting headers, update the "persistence" flag for the
            // connection so that it reads the header information in the outgoing
            // message
            updatePersistence(msg);
        }

        // once headers are in place, we can run the checks to find
        // out if a body is valid to send out with the message
        setOutgoingBodyValid(msg.isBodyAllowed());
        if (!isOutgoingBodyValid()) {
            updateBodyLengthHeaders(msg);
        }

        // get marshalled header buffers
        WsByteBuffer[] headerBuffers = null;
        try {
            // Contingent on the type of message, call the appropriate
            // marshalling method

            if (this.isH2Connection) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "formatHeaders: On an HTTP/2.0 connection, encoding the headers");
                }

                headerBuffers = msg.encodeH2Message();
            } else if (!getHttpConfig().useNetty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "formatHeaders: On an non-HTTP/2.0 connection, marshalling the headers");
                }

                headerBuffers = (getHttpConfig().isBinaryTransportEnabled()) ? msg.marshallBinaryMessage() : msg.marshallMessage();
                addToPendingByteBuffer(headerBuffers, headerBuffers.length);
            }
            //If this is a response then we need to do something
            //Check if this is an H2InboundLink - If it is then it means we need to format with a frame
            //Call down into H2InboundLink(which eventually calls the stream processor) to format the frame and return it
            //Create a new WsByteBuffer
            //Fill that byte buffer with the contents of the created frame
            //Release the original marshalled data
            //Null out any other buffers in the headerBuffers array
            //Set the new frame buffer as the sole buffer into the headerBuffers array

            if (isH2Connection && msg instanceof HttpResponseMessageImpl) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "formatHeaders: On an HTTP/2.0 connection, converting the headers into a frame");
                }

                HttpInboundServiceContextImpl context = (HttpInboundServiceContextImpl) this;
                H2HttpInboundLinkWrap link = (H2HttpInboundLinkWrap) context.getLink();

                //Call into the LinkWrap to process the header buffer
                //That will pass back a WsByteBuffer with the new data in it
                //We will then continue on from there
                if (msg.isBodyExpected()) {
                    complete = false;
                }
                // if the method is HEAD we know no body will be written out; we need to mark the headers as end of stream
                if (this.getRequestMethod().equals(MethodValues.HEAD)) {
                    complete = true;
                }
                ArrayList<Frame> headerFrames = link.prepareHeaders(WsByteBufferUtils.asByteArray(headerBuffers), complete);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "formatHeaders: On an HTTP/2.0 connection, adding header frames to be written : " + headerFrames);
                }

                framesToWrite.addAll(headerFrames);

                // the code that allocated headerBuffers, should ensure they get released, we will not do that here
            }

        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName(), "formatHeaders", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Marshalling headers failed; " + t);
            }
            setPersistent(false);
            logLegacyMessage();
            if (isInboundConnection()) {
                // response marshalling failed, try to send a 500 error instead
                setOutgoingBodyValid(false);
                try {
                    HttpResponseMessage res = (HttpResponseMessage) msg;
                    res.clear();
                    res.setStatusCode(StatusCodes.INTERNAL_ERROR);
                    if (isH2Connection) {
                        headerBuffers = msg.encodeH2Message();
                    } else {
                        headerBuffers = (getHttpConfig().isBinaryTransportEnabled()) ? msg.marshallBinaryMessage() : msg.marshallMessage();
                    }

                } catch (Throwable t2) {
                    // just going to close the socket at this point with no response...
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Attempt to marshall 500 Error failed; " + t2);
                    }
                    throw new IOException("Marshall header failure", t);
                }
            } else {
                // request marshalling failure, let the user know
                throw new IOException("Marshall header failure", t);
            }
        }
        // add them to list
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (headerBuffers != null) {
                Tr.debug(tc, "formatHeaders: Adding " + headerBuffers.length + " buffers to be written");
            } else {
                Tr.debug(tc, "formatHeaders: headerBuffers is null");
            }
        }

        this.writingHeaders = true;
        setHeadersSent();
        if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.DEBUG)) {
            getHttpConfig().getDebugLog().log(DebugLog.Level.DEBUG, HttpMessages.MSG_CONN_SENDING_HEADERS, this);
        }
    }

    /**
     * Prepare and send just the headers.
     *
     * @param msg
     * @throws IOException
     */
    final protected void sendHeaders(HttpBaseMessageImpl msg) throws IOException {
        if (headersSent()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Invalid call to sendHeaders after already sent");
            }
            return;
        }
        setupCompressionHandler(msg);
        formatHeaders(msg, false);
        synchWrite();
    }

    final protected void sendHeaders(HttpResponse response) {
        if (headersSent()) {
            Tr.event(tc, "Invalid call to sendHeaders after already sent");
            return;
        }
        this.nettyContext.channel().write(this.nettyResponse);
        this.setHeadersSent();

    }

    /**
     * Prepare and send just the headers.
     *
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible
     * for handling that situation in their code. A null return code means
     * that the async write is in progress.
     *
     * @param msg
     * @param wc
     * @return VirtualConnection
     */
    final protected VirtualConnection sendHeaders(HttpBaseMessageImpl msg, TCPWriteCompletedCallback wc) {
        if (headersSent()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Invalid call to sendHeaders after already sent");
            }
            return null;
        }
        setupCompressionHandler(msg);
        try {
            formatHeaders(msg, false);
        } catch (IOException ioe) {
            wc.error(getVC(), getTSC().getWriteInterface(), ioe);
            return null;
        }
        return asynchWrite(wc);
    }

    /**
     * Query whether the outgoing message body is allowed to be compressed.
     * Default behavior is to allow this; however, subclasses should override
     * this with more detailed logic if required.
     *
     * @return boolean
     */
    protected boolean isCompressionAllowed() {
        return true;
    }

    protected void parseAcceptEncodingHeader() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "parseAcceptEncodingHeader");
        }

        String headerValue = getRequest().getHeader(HttpHeaderKeys.HDR_ACCEPT_ENCODING).asString();
        if (headerValue == null) {
            return;
        }
        //Strip header of all spaces
        headerValue = headerValue.replaceAll("\\s+", "");
        headerValue = headerValue.toLowerCase();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseAcceptEncodingHeader: parsing [" + headerValue + "]");
        }

        String[] codingParts = null;
        String encodingName = null;
        String qualityValueString = null;
        float qualityValue = 1f;
        int indexOfQValue = -1;

        //As defined by section 14.3 Accept-Encoding, the header's possible
        //values constructed as a comma delimited list:
        // 1#( codings[ ";" "q" "=" qvalue ])
        // = ( content-coding | "*" )
        //Therefore, parse this header value by all defined codings

        for (String coding : headerValue.split(",")) {

            if (coding.endsWith(";")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Encoding token was malformed with semicolon delimiter but no quality value. Skipping [" + coding + "]");
                }
                continue;
            }

            //If this coding contains a qvalue, it will be delimited by a semicolon
            codingParts = coding.split(";");

            if (codingParts.length < 1 || codingParts.length > 2) {
                //If the codingParts contain less than 1 part or more than 2, it is a
                //malformed coding.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Encoding token was malformed with multiple semicolon delimeters. Skipping [" + coding + "]");
                }
                continue;
            }

            if (codingParts.length == 2) {
                indexOfQValue = codingParts[1].indexOf("q=");
                if (indexOfQValue != 0) {
                    //coding section was delimited by semicolon but had no quality value
                    //or did not start with the quality value identifier.
                    //Malformed section, ignoring and continue parsing
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Encoding token was malformed with a bad quality value location. Skipping [" + coding + "]");
                    }
                    continue;
                }

                //skip past "q=" to obtain the quality value and try to parse
                //first evaluate the value against the rules defined by section 5.3.1 on quality values
                //'The weight is normalized to a real number in the range 0 through 1.
                //' ... A sender of qvalue MUST NOT generate more than three digits after the decimal
                // point'.
                qualityValueString = codingParts[1].substring(indexOfQValue + 2);
                Matcher matcher = getHttpConfig().getCompressionQValueRegex().matcher(qualityValueString);

                if (!matcher.matches()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Encoding token was malformed with a bad quality value. Must be normalized between 0 and 1 with no more than three digits. Skipping ["
                                     + coding + "]");
                    }
                    continue;
                }

                try {

                    qualityValue = Float.parseFloat(qualityValueString);
                    if (qualityValue < 0) {
                        //Quality values should never be negative, but if a malformed negative
                        //value is parsed, set it as 0 (disallowed)
                        qualityValue = 0;
                    }
                } catch (NumberFormatException e) {
                    //Malformed quality value, ignore this coding and continue parsing
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Encoding token was malformed with a malformed quality value number. Skipping [" + coding + "]");
                    }
                    continue;
                }

            } else {
                //Following the convention for section 14.1 Accept, qvalue scale ranges from 0 to 1 with
                //the default value being q=1;
                qualityValue = 1f;
            }

            encodingName = codingParts[0];

            if (qualityValue == 0) {
                this.unacceptedEncodings.add(encodingName);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parsed a non accepted content-encoding: [" + encodingName + "]");
                }
            }

            else if ("*".equals(encodingName)) {
                this.bStarEncodingParsed = (qualityValue > 0f);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parsed Wildcard - * with value: " + bStarEncodingParsed);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parsed Encoding - name: [" + encodingName + "] value: [" + qualityValue + "]");
                }
                //Save to key-value pair accept-encoding map
                acceptableEncodings.put(encodingName, qualityValue);
            }

        }
        //Sort map in decreasing order
        acceptableEncodings = sortAcceptableEncodings(acceptableEncodings);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "parseAcceptEncodingHeader");
        }

    }

    /**
     * Method used to sort the accept-encoding parsed encodings in descending order
     * of their quality values (qv)
     */
    private Map<String, Float> sortAcceptableEncodings(Map<String, Float> encodings) {
        //List of map elements
        List<Map.Entry<String, Float>> list = new LinkedList<Map.Entry<String, Float>>(encodings.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> e1, Map.Entry<String, Float> e2) {
                return -(Float.compare(e1.getValue(), e2.getValue()));
            }
        });

        //put sorted list to a linkedHashMap
        Map<String, Float> result = new LinkedHashMap<String, Float>();
        for (Map.Entry<String, Float> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Used to determine if the chosen encoding is supported by the product
     */
    private boolean isSupportedEncoding() {
        boolean result = true;

        switch (preferredEncoding.toLowerCase()) {
            case ("gzip"):
                break;
            case ("x-gzip"):
                break;
            case ("zlib"):
                break;
            case ("deflate"):
                break;
            case ("identity"):
                break;

            default:
                result = false;
        }
        return result;
    }

    /**
     * Set preferred compression flag so that the appropriate compression handler is started
     */
    private void setCompressionFlags() {

        if ("gzip".equalsIgnoreCase(preferredEncoding)) {
            setGZipEncoded(true);
        } else if ("x-gzip".equalsIgnoreCase(preferredEncoding)) {
            setXGZipEncoded(true);
        } else if ("zlib".equalsIgnoreCase(preferredEncoding) || "deflate".equalsIgnoreCase(preferredEncoding)) {
            // zlib is our keyword, but deflate is the actual compression
            // algorithm so allow both inputs
            setZlibEncoded(true);
        } else if ("identity".equalsIgnoreCase(preferredEncoding)) {
            setOutgoingMsgEncoding(ContentEncodingValues.IDENTITY);

        } else {
            // invalid compression, disable further attempts
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid compression: " + preferredEncoding);
            }
            setOutgoingMsgEncoding(ContentEncodingValues.IDENTITY);
        }
    }

    private boolean isCompressionCompliant() {

        boolean isCompliant = true;
        String responseMimeType = getResponse().getMIMEType();
        //Mime Types are composed of Type and SubType, for instance application/xml
        //Grab just the Type and add wildcard
        String responseMimeTypeWildCard = null;
        if (responseMimeType != null) {
            responseMimeTypeWildCard = responseMimeType.split("/")[0] + "/*";
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.debug(tc, "isCompressionCompliant", "Response MimeType wildcard set as: " + responseMimeTypeWildCard);
            }
        }

        //Don't compress if less than 2048 bytes
        long contentLength = getResponse().getContentLength();
        if (contentLength != HeaderStorage.NOTSET && contentLength < 2048) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.debug(tc, "isCompressionCompliant", "Response body CL is less than 2048 bytes, do not attempt to compress.");
            }
            isCompliant = false;
        }

        else if (responseMimeType == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.debug(tc, "isCompressionCompliant", "No content type defined for this response, do not attempt to compress.");
            }
            isCompliant = false;
        }

        //Don't compress if the content-type of this response is explicitly configured not to be
        //compressed.
        else if (this.getHttpConfig().getExcludedCompressionContentTypes().contains(responseMimeType) ||
                 this.getHttpConfig().getExcludedCompressionContentTypes().contains(responseMimeTypeWildCard)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.debug(tc, "isCompressionCompliant", "The Content-Type: " + responseMimeType + " is configured to be excluded from compression.");
            }
            isCompliant = false;
        }

        //Don't compress if the content-type of this response message is not configured to be
        //compressed. Check for wildcard too. By default, this is text-only content-types
        else if (!this.getHttpConfig().getCompressionContentTypes().contains(responseMimeType) &&
                 !this.getHttpConfig().getCompressionContentTypes().contains(responseMimeTypeWildCard)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.debug(tc, "isCompressionCompliant", "The Content-Type: " + getResponse().getMIMEType() + " is not configured as a compressable content type");
            }
            isCompliant = false;
        }

        return isCompliant;
    }

    /**
     * Method to check on whether autocompression is requested for this outgoing
     * message.
     *
     * @param msg
     * @return boolean
     */
    private boolean isAutoCompression(HttpBaseMessageImpl msg) {

        if (this.getHttpConfig().useAutoCompression()) {
            //set the Vary header
            if (msg.containsHeader(HttpHeaderKeys.HDR_VARY) && !msg.getHeader(HttpHeaderKeys.HDR_VARY).asString().isEmpty()) {
                String varyHeader = msg.getHeader(HttpHeaderKeys.HDR_VARY).asString().toLowerCase();
                //if the vary header already contains Accept-Encoding, don't add it again
                if (!varyHeader.contains(HttpHeaderKeys.HDR_ACCEPT_ENCODING.getName().toLowerCase())) {
                    String updatedVaryValue = new StringBuilder().append(msg.getHeader(HttpHeaderKeys.HDR_VARY).asString()).append(", ").append(HttpHeaderKeys.HDR_ACCEPT_ENCODING.getName()).toString();

                    msg.setHeader(HttpHeaderKeys.HDR_VARY, updatedVaryValue);
                }

            } else {
                msg.appendHeader(HttpHeaderKeys.HDR_VARY, HttpHeaderKeys.HDR_ACCEPT_ENCODING.getName());
            }
        }

        //check and set highest priority compression encoding if set
        //on the Accept-Encoding header
        parseAcceptEncodingHeader();
        boolean rc = isOutgoingMsgEncoded();

        //Check if the message has the appropriate type and size before attempting compression
        if (this.getHttpConfig().useAutoCompression() && !this.isCompressionCompliant()) {
            rc = false;
        }

        else if (msg.containsHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING) && !"identity".equalsIgnoreCase(msg.getHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING).asString())) {
            //Body has already been marked as compressed above the channel, do not attempt to compress
            rc = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Response already contains Content-Encoding: [" + msg.getHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING).asString() + "]");
            }

        }

        else if (rc) {
            preferredEncoding = outgoingMsgEncoding.getName();
            if (!this.isSupportedEncoding() || !isCompressionAllowed()) {

                rc = false;
            }
        }

        else {

            // check private compression header
            preferredEncoding = msg.getHeader(HttpHeaderKeys.HDR_$WSZIP).asString();
            if (null != preferredEncoding) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Header requests compression: [" + preferredEncoding + "]");
                }

                msg.removeSpecialHeader(HttpHeaderKeys.HDR_$WSZIP);

                if (this.isSupportedEncoding() && isCompressionAllowed() && !this.unacceptedEncodings.contains(preferredEncoding)) {
                    rc = true;
                    setCompressionFlags();

                }

            }

            //if the private header didn't provide a valid compression
            //target, use the encodings from the accept-encoding header
            if (this.getHttpConfig().useAutoCompression() && !rc) {

                String serverPreferredEncoding = getHttpConfig().getPreferredCompressionAlgorithm().toLowerCase(Locale.ENGLISH);

                //if the compression element has a configured preferred compression
                //algorithm, check that the client accepts it and the server supports it.
                //If so, set this to be the compression algorithm.
                if (!"none".equalsIgnoreCase(serverPreferredEncoding) &&
                    (acceptableEncodings.containsKey(serverPreferredEncoding) || (bStarEncodingParsed && !this.unacceptedEncodings.contains(serverPreferredEncoding)))) {

                    this.preferredEncoding = serverPreferredEncoding;
                    if (this.isSupportedEncoding() && isCompressionAllowed()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Setting server preferred encoding");
                        }
                        rc = true;
                        setCompressionFlags();
                    }

                }

                //At this point, find the compression algorithm by finding the first
                //algorithm that is both supported by the client and server by iterating
                //through the sorted list of compression algorithms specified by the
                //Accept-Encoding header. This returns the first match or gzip in the case
                //that gzip is tied with other algorithms for the highest quality value.
                if (!rc) {

                    float gZipQV = 0F;
                    boolean checkedGZipCompliance = false;

                    //check if GZIP (preferred encoding) was provided
                    if (acceptableEncodings.containsKey(ContentEncodingValues.GZIP.getName())) {
                        gZipQV = acceptableEncodings.get(ContentEncodingValues.GZIP.getName());
                    } else {
                        checkedGZipCompliance = true;
                    }

                    for (String encoding : acceptableEncodings.keySet()) {
                        //if gzip has the same qv and we have yet to evaluate gzip,
                        //prioritize gzip over any other encoding.
                        if (acceptableEncodings.get(encoding) == gZipQV && !checkedGZipCompliance) {
                            preferredEncoding = ContentEncodingValues.GZIP.getName();
                            checkedGZipCompliance = true;
                            if (this.isSupportedEncoding() && isCompressionAllowed()) {
                                rc = true;
                                setCompressionFlags();
                                break;
                            }
                        }

                        preferredEncoding = encoding;
                        if (this.isSupportedEncoding() && isCompressionAllowed()) {
                            rc = true;
                            setCompressionFlags();
                            break;
                        }
                    }
                    //If there aren't any explicit matches of acceptable encodings,
                    //check if the '*' character was set as acceptable. If so, default
                    //to gzip encoding. If not allowed, try deflate. If neither are allowed,
                    //disable further attempts.
                    if (bStarEncodingParsed) {
                        if (!this.unacceptedEncodings.contains(ContentEncodingValues.GZIP.getName())) {
                            preferredEncoding = ContentEncodingValues.GZIP.getName();
                            rc = true;
                            setCompressionFlags();
                        } else if (!this.unacceptedEncodings.contains(ContentEncodingValues.DEFLATE.getName())) {

                            preferredEncoding = ContentEncodingValues.DEFLATE.getName();
                            rc = true;
                            setCompressionFlags();
                        }

                    }
                }
            }

        }

        if (!rc) {
            //compression not allowed, disable further attempts
            this.setGZipEncoded(false);
            this.setXGZipEncoded(false);
            this.setZlibEncoded(false);
            setOutgoingMsgEncoding(ContentEncodingValues.IDENTITY);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "Outgoing Encoding: [" + this.outgoingMsgEncoding + "]");
        }

        return rc;
    }

    /**
     * Prepares an outgoing http object and associated buffers for writing.
     *
     * First, it calls the correct methods to add the headers and then the
     * body to the queues to be written.
     *
     * @param wsbb
     * @param msg
     * @throws IOException
     */
    private void prepareOutgoing(WsByteBuffer[] wsbb, HttpBaseMessageImpl msg) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Preparing to send message");
        }

        System.out.println("MSP prepareOutgoing. Content length should be set to: " + GenericUtils.sizeOf(wsbb));

        WsByteBuffer[] buffers = wsbb;
        this.writingHeaders = false;
        if (!getHttpConfig().useNetty()) {
            // if a valid body is outgoing, check the encoding flags to see if we
            // need to automatically change the buffers
            if (!isRawBody() && !headersSent()) {
                setupCompressionHandler(msg);
            }
            // check whether we need to pass data through the compression handler
            if (null != this.compressHandler) {

                List<WsByteBuffer> list = this.compressHandler.compress(buffers);
                if (this.isFinalWrite) {
                    list.addAll(this.compressHandler.finish());
                }

                // put any created buffers onto the release list
                if (0 < list.size()) {
                    buffers = new WsByteBuffer[list.size()];
                    list.toArray(buffers);
                    storeAllocatedBuffers(buffers);
                } else {
                    buffers = null;
                }
            }
        }

        if (!headersSent() && !getHttpConfig().useNetty()) {
            // header compliance is checked by formatHeaders so check for either
            // the partial body flag or explicit chunked encoding here
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "prepareOutgoing: partial: " + isPartialBody() + " chunked: " + msg.isChunkedEncodingSet() + " cl: " + msg.getContentLength());
            }

            boolean complete = false;

            // if a finishMessage started this write, then always set the
            // Content-Length header to the input size... removes chunked
            // encoding if only one chunk and also can correct malformed
            // Content-Length values by caller
            // PK48697 - only update these if the message allows it
            if (!isPartialBody() && msg.shouldUpdateBodyHeaders()) {
                complete = true;
                if (!myChannelConfig.useNetty()) {
                    msg.setContentLength(GenericUtils.sizeOf(buffers));
                    if (msg.isChunkedEncodingSet()) {
                        msg.removeTransferEncoding(TransferEncodingValues.CHUNKED);
                        msg.commitTransferEncoding();
                    }
                }
            }

            // H2 push_promise
            // If we have a link header with rel=preload, start push_promise sequence
            // If this is an HTTP2 connection
            // If the client accepts HTTP2 push_promise frames

            HttpInboundLink link = ((HttpInboundServiceContextImpl) this).getLink();

            if ((link instanceof H2HttpInboundLinkWrap) &&
                (((H2HttpInboundLinkWrap) link).muxLink != null) &&
                (((H2HttpInboundLinkWrap) link).muxLink.getRemoteConnectionSettings() != null) &&
                (((H2HttpInboundLinkWrap) link).muxLink.getRemoteConnectionSettings().getEnablePush() == 1)) {

                // Loop through the headers in this message, check for
                // link header
                // rel=preload
                // and not nopush
                List<HeaderField> headers = msg.getAllHeaders();
                for (HeaderField header : headers) {
                    if (header.getName().equalsIgnoreCase("link") &&
                        header.asString().toLowerCase().contains("rel=preload") &&
                        !header.asString().toLowerCase().contains("nopush")) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "prepareOutgoing: Link header rel=preload found, push_promise will be sent");
                        }
                        handleH2LinkPreload(header, link);
                    }
                }
            }
            formatHeaders(msg, complete);
        } else {
            System.out.println("MSP set netty CL");
            HttpUtil.setContentLength(nettyResponse, GenericUtils.sizeOf(buffers));
        }

        // if it is valid to send a body, then format it and queue it up,
        // otherwise ignore the body buffers
        if (null != buffers) {
            if (isOutgoingBodyValid()) {
                formatBody(buffers, msg);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Ignoring " + buffers.length + " body buffers");
                }
            }
        }
    }

    /**
     * Prepares the outgoing buffers and possibly the headers and writes them
     * out on the connection synchronously.
     *
     * @param wsbb
     * @param msg
     * @throws IOException
     */
    final protected void sendOutgoing(WsByteBuffer[] wsbb, HttpBaseMessageImpl msg) throws IOException {
        prepareOutgoing(wsbb, msg);
        synchWrite();
    }

    /**
     * Prepares the outgoing buffers and possibly the headers and writes
     * them out on the connection asynchronously, using the given callback. If
     * the write can be done immediately, the VirtualConnection will be returned
     * and the callback will not be used. A null return code means that the
     * async write is in progress.
     *
     * @param wsbb
     * @param msg
     * @param wc
     * @return VirtualConnection
     */
    final protected VirtualConnection sendOutgoing(WsByteBuffer[] wsbb, HttpBaseMessageImpl msg, TCPWriteCompletedCallback wc) {
        try {
            prepareOutgoing(wsbb, msg);
        } catch (IOException ioe) {
            wc.error(getVC(), getTSC().getWriteInterface(), ioe);
            return null;
        }
        return asynchWrite(wc);
    }

    /**
     * Send a full message out. If headers have not already been sent, they will
     * be queued in front of the given body buffers, plus the "zero chunk" will
     * be tacked on the end if this is chunked encoding.
     *
     * @param wsbb
     * @param msg
     * @throws IOException
     */
    final protected void sendFullOutgoing(WsByteBuffer[] wsbb, HttpBaseMessageImpl msg) throws IOException {
        this.isFinalWrite = true;
        prepareOutgoing(wsbb, msg);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sendFullOutgoing : " + isOutgoingBodyValid() + ", " + wsbb + ", " + this);
        }
        if (isOutgoingBodyValid()) {

            System.out.println("MSP send full outgoing 1");
            HttpInboundServiceContextImpl hisc = null;
            boolean needH2EOS = true;
            HttpInboundLink link = ((HttpInboundServiceContextImpl) this).getLink();

            if (this instanceof HttpInboundServiceContextImpl) {
                if (link instanceof H2HttpInboundLinkWrap) {
                    if (framesToWrite != null && framesToWrite.size() > 0) {
                        Frame lastFrame = framesToWrite.get(framesToWrite.size() - 1);
                        if (lastFrame != null && lastFrame.flagEndStreamSet()) {
                            needH2EOS = false;
                        }
                    }
                } else {
                    needH2EOS = false;
                }
                if (wsbb == null || needH2EOS) {
                    hisc = (HttpInboundServiceContextImpl) this;
                }
            }
            System.out.println("MSP send full outgoing 2");

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && hisc != null && hisc.getLink() != null) {
                Tr.debug(tc, "sendFullOutgoing : " + hisc + ", " + hisc.getLink().toString());
            }

            if (hisc != null && link instanceof H2HttpInboundLinkWrap) {
                H2HttpInboundLinkWrap h2Link = (H2HttpInboundLinkWrap) link;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "sendFullOutgoing : preparing the final write");
                }
                if (msg.getTrailers() != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "sendFullOutgoing : creating trailers");
                    }
                    WsByteBuffer[] trailers = marshallOutgoingH2Trailers(h2Link.getWriteTable());
                    if (trailers != null) {
                        framesToWrite.addAll(h2Link.prepareHeaders(WsByteBufferUtils.asByteArray(trailers), true));
                    }
                } else if (needH2EOS) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "sendFullOutgoing : adding HTTP/2 EOS flag");
                    }
                    framesToWrite.addAll(h2Link.prepareBody(null, 0, this.isFinalWrite));
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "sendFullOutgoing : final write prepared : " + framesToWrite);
                }

            } else if (msg.isChunkedEncodingSet()) {
                System.out.println("MSP send full outgoing 3");
                HttpInboundServiceContextImpl localHisc = null;
                if (this instanceof HttpInboundServiceContextImpl) {
                    localHisc = (HttpInboundServiceContextImpl) this;
                }
                if (localHisc != null && localHisc.getSuppress0ByteChunk()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Suppressing Zero Byte Chunk and setting persistence to false.");
                    }
                    localHisc.setPersistent(false);
                } else {
                    createEndOfBodyChunk();
                }
            }
        }
        System.out.println("MSP send full outgoing 4");
        setMessageSent();
        MSP.log("set message");
        synchWrite();
        MSP.log("wrote");
    }

    /**
     * Send the full outgoing asynch with this body, possibly headers, and end
     * of body.
     *
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible
     * for handling that situation in their code. A null return code means
     * that the async write is in progress.
     *
     * @param wsbb
     * @param msg
     * @param wc
     * @return VirtualConnection - actual VC if this was done syncronously, null
     *         if it is being done asynchronously
     */
    final protected VirtualConnection sendFullOutgoing(WsByteBuffer[] wsbb, HttpBaseMessageImpl msg, TCPWriteCompletedCallback wc) {
        this.isFinalWrite = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sendFullOutgoing : " + wsbb + ", " + this);
        }

        try {
            prepareOutgoing(wsbb, msg);
        } catch (IOException ioe) {
            wc.error(getVC(), getTSC().getWriteInterface(), ioe);
            return null;
        }
        if (isOutgoingBodyValid() && msg.isChunkedEncodingSet()) {
            HttpInboundServiceContextImpl hisc = null;
            if (this instanceof HttpInboundServiceContextImpl) {
                hisc = (HttpInboundServiceContextImpl) this;
            }
            if (hisc != null && !(hisc.getLink() instanceof H2HttpInboundLinkWrap)) {
                if (hisc.getSuppress0ByteChunk()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Suppressing Zero Byte Chunk and setting persistence to false.");
                    }
                    hisc.setPersistent(false);
                } else {
                    createEndOfBodyChunk();
                }

            }
        }
        setMessageSent();
        return asynchWrite(wc);
    }

    /**
     * Create and fill in the chunk length header with the given length value.
     *
     * @param length
     * @return WsByteBuffer
     */
    protected WsByteBuffer createChunkHeader(byte[] length) {
        if (null == this.buffChunkHeader) {
            this.buffChunkHeader = newBuffer(32);
            // we're keeping this for the life of the connection so remove it
            // from the leak detection, as it'll show as a false-positive leak
            this.buffChunkHeader.removeFromLeakDetection();
        } else {
            this.buffChunkHeader.clear();
        }
        this.buffChunkHeader.put(length);
        this.buffChunkHeader.put(BNFHeaders.EOL);
        this.buffChunkHeader.flip();
        return this.buffChunkHeader;
    }

    /**
     * Create and populate the buffer that is used for the trailing CRLF after
     * a chunk as well as the final 0 chunk marking the end of the message.
     *
     * @return WsByteBuffer
     */
    protected WsByteBuffer createChunkTrailer() {
        if (null == this.buffChunkTrailer) {
            this.buffChunkTrailer = newBuffer(32);
            // we're keeping this for the life of the connection so remove it
            // from the leak detection, as it'll show as a false-positive leak
            this.buffChunkTrailer.removeFromLeakDetection();
            this.buffChunkTrailer.put(CHUNK_TRAILER_DATA);
            this.buffChunkTrailer.flip();
        } else {
            // reset for the next write
            this.buffChunkTrailer.position(0);
        }
        return this.buffChunkTrailer;
    }

    /**
     * Create the last chunk of the body (the "0" chunk) and add any trailer
     * header information that might be present.
     *
     */
    private void createEndOfBodyChunk() {

        // if this is raw outgoing body data, then all we need to do here is
        // append any existing trailer information.
        if (isRawBody()) {
            WsByteBuffer[] trailers = marshallOutgoingTrailers();
            if (null != trailers) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding trailers after raw body.");
                }
                addToPendingByteBuffer(trailers, trailers.length);
            }
            return;
        }
        // otherwise continue with the regular 0 chunk creation step
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Creating end of body chunk");
        }
        WsByteBuffer trailer = null;
        if (null == this.buffChunkTrailer) {
            trailer = createChunkTrailer();
            // write out from the 0
            trailer.position(2);
            addToPendingByteBuffer(trailer);
        } else {
            // non-null means that we are using the shared buffers
            // reset for the next write
            trailer = this.buffChunkTrailer;
            int pos = trailer.position();
            if (0 != pos) {
                // buffer is not currently being used (for trailing CRLF)
                if (2 != pos) {
                    trailer.position(2);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Zero chunk adding to pending list");
                }
                addToPendingByteBuffer(trailer);
            }
        }

        // Do we have trailers that need to be added? Technically, trailers are
        // the end of the last chunk when Chunked-Encoding is employed.
        WsByteBuffer[] trailerData = marshallOutgoingTrailers();
        if (null != trailerData) {
            // add 0 chunk with trailer data
            // CRLF + 0 + CRLF
            trailer.limit(5);
            addToPendingByteBuffer(trailerData, trailerData.length);

        } else {
            // add just the 0 chunk
            // CRLF + 0 + CRLF + CRLF
            trailer.limit(7);
        }
    }

    /**
     * Convert the outgoing trailers, if any exist, to one or more <code>WsByteBuffer</code> objects.
     *
     * @return the trailers as an array of <code>WsByteBuffer</code> objects. NULL
     *         will be returned if no trailers exist and/or
     *         need to be marshalled.
     */
    private WsByteBuffer[] marshallOutgoingTrailers() {

        HttpTrailersImpl trailers = getMessageBeingSent().getTrailersImpl();
        WsByteBuffer[] buffers = null;
        if (null != trailers) {
            trailers.computeRemainingTrailers();
            if (0 < trailers.getNumberOfHeaders()) {
                // we do have headers to marshall
                if (getHttpConfig().isBinaryTransportEnabled()) {
                    buffers = trailers.marshallBinaryHeaders(null);
                } else {
                    buffers = trailers.marshallHeaders(null);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Trailers marshalled into " + buffers.length + " buffers.");
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Warning: no actual trailers to marshall.");
                }
            }
        }
        return buffers;
    }

    /**
     * Convert the outgoing trailers, if any exist, to one or more <code>WsByteBuffer</code> objects.
     *
     * @return the trailers as an array of <code>WsByteBuffer</code> objects. NULL
     *         will be returned if no trailers exist and/or
     *         need to be marshalled.
     */
    private WsByteBuffer[] marshallOutgoingH2Trailers(H2HeaderTable table) {

        HttpTrailersImpl trailers = getMessageBeingSent().getTrailersImpl();
        WsByteBuffer[] buffers = null;
        if (null != trailers) {
            trailers.computeRemainingTrailers();
            if (0 < trailers.getNumberOfHeaders()) {
                // we do have headers to marshall
                buffers = trailers.marshallHeaders(null, table);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Trailers marshalled into " + buffers.length + " buffers.");
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Warning: no actual trailers to marshall.");
                }
            }
        }
        return buffers;
    }

    /**
     * write out all the buffers asynchronously. If the TCP channel can
     * write the data immediately, it will return the VirtualConnection.
     * This method will pass that up the stack and will not call the
     * callback in that scenario. The caller is responsible for handling
     * the immediate write situation. If the write is actually being
     * performed asynchronously, then null will be returned and the
     * callback will be called when the write is complete.
     *
     * @param callback
     * @return VirtualConnection - actual VC if write was performed
     *         synchronously, null if it is being done asynchronously
     */
    final protected VirtualConnection asynchWrite(TCPWriteCompletedCallback callback) {
        WsByteBuffer[] writeBuffers = getBuffList();
        if (null != writeBuffers) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Writing (async) " + writeBuffers.length + " buffers.");
            }
            getTSC().getWriteInterface().setBuffers(writeBuffers);
            return getTSC().getWriteInterface().write(TCPWriteRequestContext.WRITE_ALL_DATA, callback, isForceAsync(), getWriteTimeout());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Async write has no data to send.");
        }
        if (isForceAsync()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "App channel forcing callback usage.");
            }
            // 499118.1 - use our callback, not the app one
            callback.complete(getVC(), getTSC().getWriteInterface());
            return null;
        }
        // otherwise return out with the immediate success marker
        return getVC();
    }

    /**
     * Write out all the buffers synchronously.
     *
     * @throws IOException
     */
    private void synchWrite() throws IOException {

        MSP.log("getting buff list");
        WsByteBuffer[] writeBuffers = getBuffList();
        MSP.log("is buff list null: " + Objects.isNull(writeBuffers));

        if (myChannelConfig.useNetty()) {
            MSP.log("sync write");
            this.nettyContext.channel().write(this.nettyResponse);
            DefaultHttpContent content;
            if (Objects.nonNull(writeBuffers)) {
                for (WsByteBuffer buffer : writeBuffers) {

                    this.nettyContext.channel().write(buffer);
                    //content = new DefaultHttpContent(Unpooled.wrappedBuffer(buffer.getWrappedByteBuffer()));

                }
                this.nettyContext.channel().flush();
            }
            return;
        }

        if (null != writeBuffers) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Writing (sync) " + writeBuffers.length + " buffers.");
            }

            getTSC().getWriteInterface().setBuffers(writeBuffers);
            try {
                getTSC().getWriteInterface().write(TCPWriteRequestContext.WRITE_ALL_DATA, getWriteTimeout());
            } catch (IOException ioe) {
                // no FFDC required
                // just need to set the "broken" connection flag
                // 313642 - print the message as well
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IOException during sync write: " + ioe.getMessage());
                }
                setPersistent(false);
                logLegacyMessage();
                //Additional throw IOE for inbound connections check added for PI57542
                if (isInboundConnection() && !(getHttpConfig().throwIOEForInboundConnections())) {
                    // This is a server response and the request originator (the remote client) is no longer reachable.
                    // Swallow this exception: nothing useful can be done on the server and no further work can come in.
                    return;
                }
                throw ioe;
            } finally {
                // 457369 - disconnect write buffers in TCP when done
                getTSC().getWriteInterface().setBuffers(null);
            }
        }

        if (this.isH2Connection && !framesToWrite.isEmpty()) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Writing out H2 Frames");
            }
            HttpInboundServiceContextImpl context = (HttpInboundServiceContextImpl) this;

            if (context.getLink() instanceof H2HttpInboundLinkWrap) {
                H2HttpInboundLinkWrap link = (H2HttpInboundLinkWrap) context.getLink();

                try {
                    link.writeFramesSync(framesToWrite);
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "IOException during HTTP/2 write: " + ioe.getMessage());
                    }
                    if (isInboundConnection() && !(getHttpConfig().throwIOEForInboundConnections())) {
                        // This is a server response and the request originator (the remote client) is no longer reachable.
                        // Swallow this exception: nothing useful can be done on the server and no further work can come in.
                        return;
                    }
                    //throw back IOException so http channel can deal correctly with the app/servlet facing output stream
                    throw ioe;
                } finally {
                    framesToWrite.clear();
                }
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Sync write has no data to send.");
            }
        }
    }

    /**
     * Log the z/os legacy error message if appropriate.
     *
     */
    public void logLegacyMessage() {
        // 366860 - nothing in the base class
    }

    /**
     * Collect all the buffers that need to be written out this call.
     *
     * @return WsByteBuffer[] (null if there are no buffers)
     */
    protected WsByteBuffer[] getBuffList() {

        int size = this.pendingBufferStop - this.pendingBufferStart;
        if (0 == size) {
            return null;
        }
        WsByteBuffer[] list = new WsByteBuffer[size];
        System.arraycopy(this.myPendingBuffers, this.pendingBufferStart, list, 0, size);
        clearPendingByteBuffers();
        return list;
    }

    /**
     * Query whether the conn link object allows a reconnect and rewrite of
     * data if a socket exception happens.
     *
     * @return boolean
     */
    protected abstract boolean reconnectAllowed();

    /**
     * Provide the message currently being parsed. This is so that the base
     * servicecontext can have generic parsing code, since it does not know
     * whether it is the request or response being parsed.
     *
     * @return HttpBaseMessageImpl
     */
    protected abstract HttpBaseMessageImpl getMessageBeingParsed();

    /**
     * Query the message that is being sent out in this connection.
     *
     * @return HttpBaseMessageImpl
     */
    protected abstract HttpBaseMessageImpl getMessageBeingSent();

    /**
     * Method to cycle through a list of buffers provided by the TCP channel.
     * This calls an abstract method to determine which message (request or
     * response) is being parsed, then sends the buffers one at a time
     * through the standard parsing code path. If the msg finishes parsing,
     * then save any remaining buffers back onto the TCP read SC for any
     * later reads against the message body. The buffer where the headers end
     * is now the currentReadBB so do not put that back on the SC.
     *
     * @return boolean (true means complete, false if need more data)
     * @throws Exception
     */
    public boolean parseMessage() throws Exception {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (null == this.currentReadBB) {
            // load the next read buffer
            if (null == getNextReadBuffer()) {
                setupJITRead(this.myChannelConfig.getIncomingHdrBufferSize());
                return false;
            }
        }

        final HttpBaseMessageImpl msg = getMessageBeingParsed();
        boolean rc = false;
        boolean newBuffer = false;
        if (-1 == msg.getBuffersIndex()) {
            // this is the initial pass through the parsing of this message
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "First buffer of message: " + this.currentReadBB);
            }
            if (isSecure()) {
                this.myChannelConfig.getDebugLog().log(DebugLog.Level.INFO, HttpMessages.MSG_CONN_SSL, this);
            }
            this.myChannelConfig.getDebugLog().log(DebugLog.Level.DEBUG, HttpMessages.MSG_PARSE_STARTING, this);
            newBuffer = true;
        } else if (isJITRead()) {
            newBuffer = true;
        }

        do {
            if (newBuffer) {
                // save the first buffer or any JIT buffer to the parse list. If
                // either is not true then we are re-using a buffer.
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Msg saving buffer: " + this.currentReadBB);
                }
                msg.addParseBuffer(this.currentReadBB);
            }

            // configure the first buffer on the list
            configurePostReadBuffer(this.currentReadBB);
            rc = msg.parseMessage(this.currentReadBB, this.myChannelConfig.shouldExtractValue());
            if (rc) {
                parsingComplete();
            } else {
                // not fully parsed, check for more data
                if (!isReadDataAvailable()) {
                    // break out and read for more
                    break;
                }
                newBuffer = true;
            }

        } while (!rc);

        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseMessage() returning " + rc + " for " + msg);
        }
        return rc;
    }

    /**
     * Once the parsing of the incoming message headers has completed, this
     * method is used to implement any special case logic at that point.
     *
     * @throws Exception
     */
    protected void parsingComplete() throws Exception {
        final HttpBaseMessageImpl msg = getMessageBeingParsed();
        setHeadersParsed();
        setLastHeaderBuffer();
        updatePersistence(msg);
        updateBodyFlags(msg);
        updateIncomingEncodingFlags(msg);
        if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.DEBUG)) {
            getHttpConfig().getDebugLog().log(DebugLog.Level.DEBUG, HttpMessages.MSG_PARSE_FINISHED, this);
        }
    }

    /**
     * Method to consolidate the read buffer handling when starting reads for
     * a response message.
     *
     * @param size
     * @param bAllocate
     * @return boolean (is data available right now)
     */
    public boolean setupReadBuffers(int size, boolean bAllocate) {
        if (isReadDataAvailable()) {
            return true;
        }
        if (getHttpConfig().isJITOnlyReads()) {
            // config is forcing JIT reads
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config forcing a JIT read.");
            }
            setupJITRead(size);
        } else if (isReadSpaceAvailable(size)) {
            // read into the existing space
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reading into existing buffer: " + getReadBuffer());
            }
            configurePreReadBuffer(getReadBuffer());
            setupNonJITRead();
        } else if (bAllocate) {
            // no space available but we need to allocate a new buffer now
            // instead of doing a JIT read
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Allocating read buffer, size=" + size);
            }
            setOldLimit(0);
            setReadBuffer(newBuffer(size));
            // setReadBuffer(allocateBuffer(size));
            setupNonJITRead();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting up a JIT read, size=" + size);
            }
            setupJITRead(size);
        }
        return false;
    }

    /**
     * Reset variables related to parsing an incoming message.
     *
     */
    public void resetRead() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Resetting parsing variables");
        }
        resetMsgParsedState();
        getMessageBeingParsed().clear();
    }

    /**
     * Method to reset the outbound write state variables as well as clearing
     * the message that was just sent. This might be used after sending a
     * temporary response for example.
     */
    public void resetWrite() {
        resetMsgSentState();
        VersionValues version = getMessageBeingSent().getVersionValue();
        getMessageBeingSent().clear();
        // reset the version based on previous message, not default from clear()
        getMessageBeingSent().setVersion(version);
    }

    /**
     * Dumps the debug information needed to determine the cause of
     * the failure.
     *
     * @return String: The FFDC dump information used for diagnostics/debugging
     */
    @Override
    public String[] introspectSelf() {
        List<String> output = new ArrayList<String>();

        // header debug information
        final HttpBaseMessageImpl msg = getMessageBeingParsed();
        if (null != msg) {
            output.add("Message parsed: " + msg.toString());
            Iterator<HeaderField> it = msg.getAllHeaders().iterator();
            while (it.hasNext()) {
                HeaderField header = it.next();
                output.add(header.getName() + "=" + header.asString());
            }
        } else {
            output.add("Message being parsed is null");
        }

        if (null != getReadBuffer()) {
            output.add("read buffer=" + getReadBuffer());
        }

        // fillBuffers debug information
        output.add("ReadBufferSize=" + getHttpConfig().getIncomingBodyBufferSize());
        output.add("ReadTimeout=" + getReadTimeout());
        output.add("WriteTimeout=" + getWriteTimeout());
        output.add("unparsedDataRemaining=" + getDataLength());
        return output.toArray(new String[output.size()]);
    }

    /**
     * Query the application side callback value.
     *
     * @return InterChannelCallback
     */
    final public InterChannelCallback getAppWriteCallback() {
        return this.appWriteCB;
    }

    /**
     * Query the application side read callback.
     *
     * @return InterChannelCallback
     */
    final public InterChannelCallback getAppReadCallback() {
        return this.appReadCB;
    }

    /**
     * Set the application side write callback.
     *
     * @param cb
     */
    final protected void setAppWriteCallback(InterChannelCallback cb) {
        this.appWriteCB = cb;
    }

    /**
     * Set the application side read callback.
     *
     * @param cb
     */
    final protected void setAppReadCallback(InterChannelCallback cb) {
        this.appReadCB = cb;
    }

    /**
     * Query the HTTP object factory.
     *
     * @return HttpObjectFactory
     */
    public abstract HttpObjectFactory getObjectFactory();

    /**
     * Set the channel configuration object to the input value.
     *
     * @param hcc
     */
    final public void setHttpConfig(HttpChannelConfig hcc) {
        this.myChannelConfig = hcc;
    }

    /**
     * Query the channel config object.
     *
     * @return HttpChannelConfig
     */
    final public HttpChannelConfig getHttpConfig() {
        System.out.println("Null? " + this.myChannelConfig == null);
        if (this.myChannelConfig == null) {
            this.myChannelConfig = new HttpChannelConfig();
        }

        return this.myChannelConfig;
    }

    /**
     * Query the TCP service context value.
     *
     * @return TCPConnectionContext
     */
    final public TCPConnectionContext getTSC() {
        return this.myTSC;
    }

    /**
     * Provide access to the VirtualConnection object on this connection.
     *
     * @return VirtualConnectcion
     */
    final public VirtualConnection getVC() {
        return this.myVC;
    }

    /**
     * Set the VirtualConnection for this connection to the input object.
     *
     * @param vc
     */
    final protected void setVC(VirtualConnection vc) {
        this.myVC = vc;
    }

    /**
     * Get a byte array representation of a chunked length...as stolen from the
     * old internal http transport.
     *
     * @param length
     * @return byte[] -- representation of the chunk length in bytes
     */
    private byte[] asChunkedLength(int length) {

        int initSize = 16;
        int count = length;
        byte[] chunkBuf = new byte[initSize];
        int off = chunkBuf.length;
        int digit;
        while (0 < count) {
            digit = count & 0xf;
            chunkBuf[--off] = HEX_BYTES[digit];
            count >>= 4;
        }
        byte[] retVal = new byte[initSize - off];
        System.arraycopy(chunkBuf, off, retVal, 0, retVal.length);
        return retVal;
    }

    // *****************************************************************
    // Methods to read the body of a message
    // *****************************************************************

    /**
     * Allocate a new buffer based on the configuration. This buffer is
     * not stored immediately on the "free later" list, the caller is
     * expected to handle the buffer release.
     *
     * @param size
     * @return WsByteBuffer
     */
    private WsByteBuffer newBuffer(int size) {
        return (getHttpConfig().isDirectBufferType()) ? HttpDispatcher.getBufferManager().allocateDirect(size) : HttpDispatcher.getBufferManager().allocate(size);

    }

    /**
     * Allocate a buffer according to the requested input size.
     *
     * @param size
     * @return WsByteBuffer
     */
    protected WsByteBuffer allocateBuffer(int size) {
        WsByteBuffer wsbb = newBuffer(size);
        this.allocatedBuffers.add(wsbb);
        return wsbb;
    }

    /**
     * Set the length of data that we need to read to the input value.
     *
     * @param len
     */
    private void setDataLength(long len) {
        this.unparsedDataRemaining = len;
    }

    /**
     * Query the remaining data length to read.
     *
     * @return int (0 if done, -1 if not yet set)
     */
    private long getDataLength() {
        return this.unparsedDataRemaining;
    }

    /**
     * Utility method to determine if trailer headers are in this buffer and
     * need to be parsed out.
     *
     * @return boolean
     * @throws IllegalHttpBodyException
     */
    private boolean doTrailersFollow() throws IllegalHttpBodyException {

        // at this point, we are either going to parse the blank line after the
        // zero chunk (end of body) or we are about to start parsing the trailer
        // headers. Note: we must handle "surprise" trailer headers that do not
        // have any indication that they may exist yet so we must peek-ahead at
        // the next byte to find out. If we see a CRLF, then just parse the full
        // CRLF line now.

        if (getMessageBeingParsed().containsHeader(HttpHeaderKeys.HDR_TRAILER)) {
            return true;
        }
        // otherwise check the next byte to see if it is a CRLF or text
        int data_len = getReadBuffer().remaining();
        if (0 == data_len) {
            // don't have any data, assume they're not present at this point
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No data present, assuming no trailers exist");
            }
            return false;
        }
        byte b = getReadBuffer().get();
        boolean rc = true;
        if (BNFHeaders.CR == b) {
            // count this CR towards the size and check for the following LF
            addToIncomingMsgSize(1L);
            if (1 < data_len) {
                b = getReadBuffer().get();
                if (BNFHeaders.LF == b) {
                    addToIncomingMsgSize(1L);
                } else {
                    // CR <x>, invalid
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Error: Received " + b + " after the CR");
                    }
                    throw new IllegalHttpBodyException("Missing chunk LF: " + b);
                }
            }
            rc = false;
        } else if (BNFHeaders.LF == b) {
            // done with blank line, no extra work
            addToIncomingMsgSize(1L);
            rc = false;
        } else {
            // trailer headers DO exist, reset the position so that header
            // parsing starts on the right information
            int pos = getReadBuffer().position() - 1;
            getReadBuffer().position(pos);
            rc = true;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Peek ahead for trailers rc->" + rc);
        }
        return rc;
    }

    /**
     * Parse past the CRLF trailing after a single chunk from the HTTP body.
     *
     * @param excess
     * @throws IllegalHttpBodyException
     *                                      (if the CRLF is invalid or missing)
     */
    private void parseChunkCRLF(int excess) throws IllegalHttpBodyException {
        if (0 == excess) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsing past chunk CRLF, excess=" + excess);
        }
        byte b = getReadBuffer().get();
        addToIncomingMsgSize(1L);
        if (BNFHeaders.CR == b) {
            if (1 == excess) {
                // just return out now... the LF will have to be parsed
                // on the next readBody pass
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "LF char not read yet.");
                }
                return;
            }
            // read next char which must be an LF
            b = getReadBuffer().get();
            addToIncomingMsgSize(1L);
        }
        if (BNFHeaders.LF != b) {
            // must be an LF char at this point
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Char expected to be LF but is ->" + b);
            }
            throw new IllegalHttpBodyException("Missing chunk LF: " + b);
        }
    }

    /**
     * Configure the global target body length marker based on the incoming
     * message body type.
     *
     * @param msg
     * @param async
     * @return boolean (true means async read in progress)
     * @throws BodyCompleteException
     * @throws IllegalHttpBodyException
     *                                      -- invalid body lengths
     * @throws IOException
     *                                      -- error reading data to determine lengths
     */
    private boolean findBodyLength(HttpBaseMessageImpl msg, boolean async) throws BodyCompleteException, IllegalHttpBodyException, IOException {

        // check through the types of incoming body data and set the global
        // "target length" marker appropriately
        if (isChunkedEncoding()) {

            // read in the chunk length
            if (GenericConstants.PARSING_NOTHING == getChunkLengthParsingState()) {
                // not currently parsing anything, set the default state
                // otherwise we need to continue where we left off (parsing CRLF
                // or extension)
                setChunkLengthParsingState(HttpInternalConstants.PARSING_CHUNK_LENGTH);
            }
            setDataLength(readChunkLength(getReadBuffer()));
            while (NOT_ENOUGH_DATA == getDataLength()) {

                // we're only looking for a few bytes, but read based on the
                // overall async vs sync flag.
                if (!isReadDataAvailable()) {
                    if (getHttpConfig().shouldWaitForEndOfMessage() &&
                        HttpInternalConstants.PARSING_END_OF_MESSAGE == getChunkLengthParsingState()) {
                        if (fillABuffer(1, async, true)) {
                            return true;
                        }
                    } else {
                        if (fillABuffer(3, async, true)) {
                            return true;
                        }
                    }
                    if (isBodyComplete()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Failed to read chunk length");
                        }
                        throw new BodyCompleteException("Failed to read chunk length");
                    }
                }
                setDataLength(readChunkLength(getReadBuffer()));
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Read chunk size " + getDataLength());
            }
            checkIncomingMessageLimit(getDataLength());

            if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.DEBUG)) {
                getHttpConfig().getDebugLog().log(DebugLog.Level.DEBUG, HttpMessages.MSG_PARSE_CHUNK_LEN + getDataLength(), this);
            }
            if (0 == getDataLength()) {
                setBodyComplete();
                // check for the existance of trailer headers
                if (doTrailersFollow()) {
                    return parseTrailers(msg, async);
                }
                return false;
            }

        } else if (isContentLength()) {

            // starting at the beginning
            setDataLength(getContentLength());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Got CL size " + getDataLength());
            }
            if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.DEBUG)) {
                getHttpConfig().getDebugLog().log(DebugLog.Level.DEBUG, HttpMessages.MSG_PARSE_CONTENT_LEN + getDataLength(), this);
            }

            // if CL is 0 bytes to start with then nothing to read, or
            // zero bytes left, then nothing more to read
            if (0 == getDataLength()) {
                setBodyComplete();
                throw new BodyCompleteException("No more body to read");
            }
        }
        if (0 > getDataLength()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid negative body length: " + getDataLength());
            }
            setDataLength(0);
            throw new IllegalHttpBodyException("Invalid body length");
        }
        return false;
    }

    /**
     * Method to read the chunk length for a raw buffer.
     *
     * @param msg
     * @param async
     * @return boolean (whether async read is going on in the background)
     * @throws IllegalHttpBodyException
     * @throws IOException
     */
    private boolean findRawChunkLength(HttpBaseMessageImpl msg, boolean async) throws IllegalHttpBodyException, IOException {

        if (GenericConstants.PARSING_NOTHING == getChunkLengthParsingState()) {
            // not currently parsing anything, set the default state
            // otherwise we need to continue where we left off (parsing CRLF
            // or extension)
            setChunkLengthParsingState(HttpInternalConstants.PARSING_CHUNK_LENGTH);
        }
        if (null == getReadBuffer()) {
            if (fillABuffer(3, async, true)) {
                return true;
            }
        }
        setDataLength(readChunkLength(getReadBuffer()));
        while (NOT_ENOUGH_DATA == getDataLength()) {
            // we're only looking for a few bytes, but read based on the
            // overall async vs sync flag.
            if (!isReadDataAvailable()) {
                if (fillABuffer(3, async, true)) {
                    return true;
                }
                if (isBodyComplete()) {
                    // indicates an IOException happened
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Failed to read chunk length");
                    }
                    throw new IllegalHttpBodyException("IOException while reading chunk");
                }
            }
            setDataLength(readChunkLength(getReadBuffer()));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Read raw chunk size " + getDataLength());
        }
        checkIncomingMessageLimit(getDataLength());

        if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.DEBUG)) {
            getHttpConfig().getDebugLog().log(DebugLog.Level.DEBUG, HttpMessages.MSG_PARSE_CHUNK_LEN + getDataLength(), this);
        }
        if (0 == getDataLength()) {
            // found the zero chunk
            boolean bTrailers = doTrailersFollow();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Slicing body up to zero chunk, trailers=" + bTrailers);
            }
            int position = getReadBuffer().position();
            int limit = getReadBuffer().limit();
            getReadBuffer().position(this.oldPosition);
            if (position == limit) {
                // we don't have any excess data at the end
                storeBuffer(getReadBuffer().slice());
            } else {
                // we have more data at the end that we don't want in the slice
                getReadBuffer().limit(position);
                storeBuffer(getReadBuffer().slice());
                getReadBuffer().limit(limit);
            }
            // reset the position in the buffer where we stopped
            getReadBuffer().position(position);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Post-slice: " + getReadBuffer());
            }
            setBodyComplete();
            if (bTrailers) {
                // parse the trailer headers now that we've saved all of the
                // body information
                return parseTrailers(msg, async);
            }
            return false;
        }
        return false;
    }

    /**
     * Method to read a buffer's worth of raw chunked encoded data.
     *
     * @param msg
     * @param async
     * @return boolean (whether an async read is going on)
     * @throws BodyCompleteException
     * @throws IllegalHttpBodyException
     * @throws IOException
     *                                      -- error reading data
     */
    private boolean readRawChunk(HttpBaseMessageImpl msg, boolean async) throws BodyCompleteException, IllegalHttpBodyException, IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Reading raw chunk buffer, len->" + getDataLength());
        }
        // if we've previously read the last of the body, then stop coming here
        if (isBodyComplete()) {
            return false;
        }

        // if we don't have a buffer, check to see if we've parsed body
        // data in the last buffer with header information or from any buffers
        // sitting on the TCP read service context
        if (null == getReadBuffer()) {
            getNextReadBuffer();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "current buffer: " + getReadBuffer());
        }

        // set the starting position if need be...
        // 372777/PK26487 - handle a full buffer too
        // TODO: we're still exposed if the chunk length straddles buffers
        if (null == getReadBuffer() || (getReadBuffer().position() == getReadBuffer().capacity())) {
            // will be creating a buffer soon
            this.oldPosition = 0;
        } else if (-1 == this.oldPosition) {
            // not already set, do so now
            this.oldPosition = getReadBuffer().position();
        }

        // if we don't already know the length, go figure that out
        if (NO_MORE_DATA == getDataLength()) {
            if (findRawChunkLength(msg, async)) {
                return true;
            }
        }

        // if there is no more data to get, then just exit back up
        if (0 == getDataLength()) {
            // data including the zero chunk is parsed out by findLength method
            return false;
        }
        // we now know the target length to get, fill in the read buffer
        if (!isReadDataAvailable()) {
            if (fillABuffer(getDataLength(), async, true)) {
                return true;
            }
            if (isBodyComplete()) {
                // indicates an IOException occurred
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error while reading body data");
                }
                throw new IllegalHttpBodyException("Error while reading chunk body.");
            }
        }
        int position = getReadBuffer().position();
        int limit = getReadBuffer().limit();
        int amountAvail = (limit - position);
        long excess = amountAvail - getDataLength();
        if (0 < excess) {
            amountAvail = (int) getDataLength();
            this.unparsedDataRemaining = 0;
        } else {
            // got either the asked for amount or less
            this.unparsedDataRemaining -= amountAvail;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Unparsed data remaining: " + getDataLength());
        }

        // check whether we've stopped at the end of the buffer and can save
        // data now or if we need to continue using this buffer for further
        // parses
        if (0 >= excess && limit == getReadBuffer().capacity()) {
            // [data] or [extra data]
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Buffer is full, position: " + this.oldPosition);
            }
            getReadBuffer().position(this.oldPosition);
            if (0 == this.oldPosition) {
                storeBuffer(returnLastBuffer());
            } else {
                storeBuffer(getReadBuffer().slice());
            }
            setReadBuffer(null);
            this.oldPosition = -1;
            if (0 == getDataLength()) {
                setDataLength(NO_MORE_DATA);
            }
        } else {
            int excessInt = (int) excess;
            // slice up a return buffer
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Slicing part of a buffer");
            }
            getReadBuffer().position(this.oldPosition);
            if (excessInt > 0) {
                // 372777/PK26487 - should be CRLF, include with this raw data
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parsing CRLF for raw chunk, excess=" + excess);
                }
                byte b = getReadBuffer().get(limit - excessInt);
                if (BNFHeaders.CR == b) {
                    excessInt--;
                    if (0 < excessInt) {
                        b = getReadBuffer().get(limit - excessInt);
                    }
                }
                if (BNFHeaders.LF == b) {
                    excessInt--;
                }
                getReadBuffer().limit(limit - excessInt);
                storeBuffer(getReadBuffer().slice());
                getReadBuffer().limit(limit);
                getReadBuffer().position(limit - excessInt);
            } else {
                storeBuffer(getReadBuffer().slice());
                getReadBuffer().position(limit);
            }
            this.oldPosition = getReadBuffer().position();
            if (0 == getDataLength()) {
                setDataLength(NO_MORE_DATA);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Post-slice: " + getReadBuffer());
            }
        }
        return false;
    }

    /**
     * Method to read a single buffer of data from the body of the incoming
     * message. This handles both chunked-encoding and content-length bodies.
     *
     * @param msg
     * @param async
     * @return boolean (true means an async read is in progress)
     * @throws BodyCompleteException
     * @throws IllegalHttpBodyException
     * @throws IOException
     *                                      -- error reading data
     */
    private boolean readSingleBlock(HttpBaseMessageImpl msg, boolean async) throws BodyCompleteException, IllegalHttpBodyException, IOException {
        // check if tempBuffer is already set, unless we're reading the entire
        // body
        if (!isMultiRead() && !this.tempBuffers.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Buffer already available");
            }
            return false;
        }

        // if we've previously read the last of the body, then stop coming here
        if (isBodyComplete()) {
            throw new BodyCompleteException("No more body to read");
        }

        // if we don't have a buffer, check to see if we've parsed body
        // data in the last buffer with header information or from any buffers
        // sitting on the TCP read service context
        if (null == getReadBuffer()) {
            getNextReadBuffer();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "current buffer: " + getReadBuffer());
        }

        // if we don't already know the length, go figure that out
        if (NO_MORE_DATA == getDataLength()) {
            if (findBodyLength(msg, async)) {
                return true;
            }
        }

        // if there is no more data to get, then just exit back up
        if (0 == getDataLength()) {
            return false;
        }
        // we now know the target length to get, fill in the read buffer
        if (!isReadDataAvailable()) {
            if (fillABuffer(getDataLength(), async, true)) {
                return true;
            }
            if (isBodyComplete()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "End of body found during fillABuffer");
                }
                return false;
            }
        }
        int position = getReadBuffer().position();
        int limit = getReadBuffer().limit();
        int amountAvail = (limit - position);
        long excess = amountAvail - getDataLength();
        if (0 < excess) {
            if (isContentLength()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Excess data received: " + excess);
                }
            }
            amountAvail = (int) getDataLength();
            this.unparsedDataRemaining = 0;
        } else {
            // got either the asked for amount or less
            this.unparsedDataRemaining -= amountAvail;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Unparsed data remaining: " + getDataLength());
        }
        if (amountAvail == getReadBuffer().capacity()) {
            // this is a full buff we can send
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning full buffer");
            }
            storeTempBuffer(returnLastBuffer());
            getNextReadBuffer();
        } else {
            // slice up a return
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Slicing part of a buffer");
            }
            int newPosition = position + amountAvail;
            if (newPosition == limit) {
                // we don't have any excess data at the end
                storeTempBuffer(getReadBuffer().slice());
            } else {
                // we have more data at the end that we don't want in the slice
                getReadBuffer().limit(newPosition);
                storeTempBuffer(getReadBuffer().slice());
                getReadBuffer().limit(limit);
            }
            // now set position to where we stopped pulling data
            getReadBuffer().position(newPosition);
            if (isChunkedEncoding() && 0 < excess) {
                parseChunkCRLF((int) excess);
            }
        }
        if (isContentLength() && 0 == getDataLength()) {
            // we've fully read the content-length body, set the complete flag
            setBodyComplete();
        }
        return false;
    }

    /**
     * Method to read a single wsbb from the HTTP body. This will return a
     * boolean flag, with true meaning that an asynch read is in progress
     * (come back later) or false otherwise (we have a buffer in storage or
     * there was nothing to read).
     *
     * @param msg
     * @param async
     * @return boolean (true if asynch read in progress, false otherwise)
     * @throws IllegalHttpBodyException
     * @throws BodyCompleteException
     * @throws IOException
     *                                      -- error reading data
     */
    final protected boolean readBodyBuffer(HttpBaseMessageImpl msg, boolean async) throws IllegalHttpBodyException, BodyCompleteException, IOException {
        boolean bAsyncInProgress = false;

        // make sure the decompression handler is in place
        setupDecompressionHandler();
        if (this.decompressHandler.isEnabled() && VersionValues.V10.equals(msg.getVersionValue())) {
            // decompressing a 1.0 request, must buffer the entire thing
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Reading encoded 1.0 body");
            }
            setMultiRead(true);
            return readBodyBuffers(msg, async);
        }

        // HTTP/1.0 Request bodies must have a Content-Length header or trigger
        // a 400 Bad Request response. HTTP/1.1 Requests can have either the
        // Content-Length header or the "Transfer-Encoding: chunked" header or
        // get the same 400 response. HTTP Responses, however, can specify
        // Content-Length, Transfer-Encoding, or the server can signify the end
        // of the body by closing the connection.

        if (isContentLength()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reading body, content-length");
            }
            bAsyncInProgress = readSingleBlock(msg, async);
        } else if (isChunkedEncoding()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reading body, chunked");
            }
            // if unparsed==0 at this point then it's the tail end of an async
            // read of a chunk, this means we're done reading the chunk
            if (0 == getDataLength()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Reached end of chunk previously");
                }
                setDataLength(NO_MORE_DATA);
            }
            if (!isRawBody()) {
                bAsyncInProgress = readSingleBlock(msg, async);
            } else {
                // @263187 - correctly read raw chunks
                bAsyncInProgress = readRawChunk(msg, async);
            }
        } else {
            // Read data until the connection closes
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reading until closure");
            }
            bAsyncInProgress = readUntilEnd(async);
        }
        // if data is present in the temp storage, move it to the user storage
        if (!bAsyncInProgress) {
            if (!moveBuffers()) {
                bAsyncInProgress = readBodyBuffer(msg, async);
            }
        }
        return bAsyncInProgress;
    }

    /**
     * Method to actually start the read of the buffers based on identifiers
     * such as content-length, chunked encoding, etc.
     *
     * @param msg
     * @param async
     * @return boolean (whether an async read is in progress or not)
     * @throws IllegalHttpBodyException
     * @throws BodyCompleteException
     * @throws IOException
     *                                      -- error reading data
     */
    final protected boolean readBodyBuffers(HttpBaseMessageImpl msg, boolean async) throws IllegalHttpBodyException, BodyCompleteException, IOException {

        // if the bodycomplete flag is true, then don't try to read more
        if (isBodyComplete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "BodyComplete is true, exiting");
            }
            return false;
        }

        boolean bAsyncInProgress = false;

        // make sure the decompression handler is in place
        setupDecompressionHandler();

        // HTTP/1.0 Request bodies must have a Content-Length header or trigger
        // a 400 Bad Request response. HTTP/1.1 Requests can have either the
        // Content-Length header or the "Transfer-Encoding: chunked" header or
        // get the same 400 response. HTTP Responses, however, can specify
        // Content-Length, Transfer-Encoding, or the server can signify the end
        // of the body by closing the connection.

        if (isChunkedEncoding()) {
            if (isRawBody()) {
                // @263187 - correctly read raw chunk body
                bAsyncInProgress = readRawChunk(msg, async);
                while (!bAsyncInProgress && !isBodyComplete()) {
                    bAsyncInProgress = readRawChunk(msg, async);
                }
            } else {
                // read all of the chunks. True RC from readFullChunk means that an
                // async read is going on, so just pass the rc back up the stack.
                bAsyncInProgress = readFullChunk(msg, async);
                while (!bAsyncInProgress && !isBodyComplete()) {
                    bAsyncInProgress = readFullChunk(msg, async);
                }
            }
        } else if (isContentLength()) {
            bAsyncInProgress = readFullCL(msg, async);
        } else {
            // Read data until the connection closes
            bAsyncInProgress = readFullBody(async);
        }

        // if there is no further read taking place, then we are done
        // reading the body
        if (!bAsyncInProgress) {
            setBodyComplete();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readBodyBuffers returning " + bAsyncInProgress);
        }
        return bAsyncInProgress;
    }

    /**
     * Inbound request bodies must be length delimited otherwise the server
     * should send the HTTP 400 "Bad Request" error back. When reading the
     * response however, the body can be delimited by the socket closing
     * thus the Outbound ServiceContext must override this method.
     *
     * @param async
     * @return boolean (is there more data to read?)
     * @throws IllegalHttpBodyException
     * @throws BodyCompleteException
     */
    protected boolean readUntilEnd(boolean async) throws IllegalHttpBodyException, BodyCompleteException {
        // server should send the HTTP 400 "Bad Request" response in this
        // scenario
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Inbound request sending non-length delimited" + " body, async:" + async + ", throwing exception");
        }
        if (getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.WARN)) {
            getHttpConfig().getDebugLog().log(DebugLog.Level.WARN, HttpMessages.MSG_INVALID_BODY, this);
        }
        prepareClosure();
        throw new IllegalHttpBodyException("Non-length delimited body on request");
    }

    /**
     * Move the buffers from temporary storage over to main return storage. It
     * will unencode buffers if necessary.
     *
     * @return boolean - is there output to return to app channels?
     * @throws IllegalHttpBodyException
     *                                      if decryption fails
     */
    private boolean moveBuffers() throws IllegalHttpBodyException {
        if (this.tempBuffers.isEmpty()) {
            return true;
        }
        boolean rc = false;
        if (this.decompressHandler.isEnabled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Removing encoding...");
            }
            while (!this.tempBuffers.isEmpty()) {
                WsByteBuffer buffer = this.tempBuffers.removeFirst();
                while (buffer.hasRemaining()) {
                    try {
                        List<WsByteBuffer> list = this.decompressHandler.decompress(buffer);
                        if (!list.isEmpty()) {
                            if (this.decompressHandler.getBytesRead() > 0
                                && (this.decompressHandler.getBytesWritten() / this.decompressHandler.getBytesRead()) > getHttpConfig().getDecompressionRatioLimit()) {
                                this.cyclesAboveDecompressionRatio++;
                                if (this.cyclesAboveDecompressionRatio > getHttpConfig().getDecompressionTolerance()) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "Decompression ratio tolerance reached. Number of cycles above configured ratio: " + this.cyclesAboveDecompressionRatio);
                                    }
                                    String s = Tr.formatMessage(tc, "decompression.tolerance.reached");
                                    throw new DataFormatException(s);
                                }

                            }
                            this.storage.addAll(list);
                            rc = true;
                        }
                    } catch (DataFormatException dfe) {
                        FFDCFilter.processException(dfe, getClass().getName() + ".moveBuffers", "1");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Received exception during decompress; " + dfe);
                        }
                        throw new IllegalHttpBodyException(dfe.getMessage());
                    }
                }
                buffer.release();
            }
            final HttpBaseMessageImpl msg = getMessageBeingParsed();
            // the first time through, we need to remove the "encoding" from
            // the message itself and add the $WSZIP header. If the $WSZIP
            // header already exists, then we don't need to modify this
            // again... each body chunk goes through this method for example.
            if (!msg.containsHeader(HttpHeaderKeys.HDR_$WSZIP)) {
                msg.removeOutermostEncoding();
                // save the original content-length
                if (isContentLength()) {
                    msg.setHeader(HttpHeaderKeys.HDR_$WSORIGCL, Long.toString(getContentLength()));
                }
                if (isBodyComplete()) {
                    // if we have the entire body, set the new content-length
                    // Note: http/1.0 will read and buffer the entire thing
                    int newlen = (int) this.decompressHandler.getBytesWritten();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Setting decompressed Content-Length old: " + getContentLength() + " new: " + newlen);
                    }
                    msg.setContentLength(newlen);
                } else {
                    // HTTP/1.1, we will change to chunked if we do not have it all
                    msg.removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);
                    msg.setTransferEncoding(TransferEncodingValues.CHUNKED);
                }
            }
        } else {
            // no autodecompression on these buffers... shouldn't get here but
            // just in case
            this.storage.addAll(this.tempBuffers);
            this.tempBuffers.clear();
            rc = true;
        }
        return rc;
    }

    /**
     * Read an entire content-length delimited body. This will stop early if
     * an asynchronous read is being done in the background though.
     *
     * @param msg
     * @param async
     * @return boolean (true means async read in progress)
     * @throws BodyCompleteException
     * @throws IllegalHttpBodyException
     * @throws IOException
     *                                      -- error reading data
     */
    private boolean readFullCL(HttpBaseMessageImpl msg, boolean async) throws BodyCompleteException, IllegalHttpBodyException, IOException {
        boolean bAsyncInProgress = false;

        // if we haven't figured out the target length yet, then get that and
        // then start the read(s) for whatever is missing
        if (NO_MORE_DATA == getDataLength()) {
            bAsyncInProgress = readSingleBlock(msg, async);
        }
        // read until the full body is ready or an async read is in progress
        while (0 < getDataLength() && !bAsyncInProgress) {
            bAsyncInProgress = readSingleBlock(msg, async);
        }
        // take care of any temp buffers (unencode, etc)
        if (!bAsyncInProgress) {
            if (!moveBuffers()) {
                bAsyncInProgress = readFullCL(msg, async);
            }
        }
        return bAsyncInProgress;
    }

    /**
     * Method to read the entire body when the body end is marked by the socket
     * closing. This will strip any encoding as well.
     *
     * @param async
     * @return boolean
     * @throws IllegalHttpBodyException
     * @throws BodyCompleteException
     */
    private boolean readFullBody(boolean async) throws IllegalHttpBodyException, BodyCompleteException {
        boolean bAsyncInProgress = false;

        while (!bAsyncInProgress && !isBodyComplete()) {
            bAsyncInProgress = readUntilEnd(async);
        }
        // if rc is true, then an async read is in progress, otherwise we have
        // data to work with.
        if (!bAsyncInProgress && !isBodyComplete()) {
            if (!moveBuffers()) {
                bAsyncInProgress = readFullBody(async);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readFullBody returning " + bAsyncInProgress);
        }
        return bAsyncInProgress;
    }

    /**
     * Method to read one single chunk from a body. It will gather all the
     * buffers, decode them if there is encoding, and place them in storage.
     *
     * @param msg
     * @param async
     * @return boolean
     * @throws BodyCompleteException
     * @throws IllegalHttpBodyException
     * @throws IOException
     *                                      -- error reading data
     */
    private boolean readFullChunk(HttpBaseMessageImpl msg, boolean async) throws BodyCompleteException, IllegalHttpBodyException, IOException {
        boolean bAsyncInProgress = false;

        // if unparsed==0 at this point then it's the tail end of an async
        // read of a chunk, this means we're done reading the chunk
        if (0 == getDataLength()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reached end of chunk");
            }
            setDataLength(NO_MORE_DATA);
        }

        // if we haven't figured out the target length yet, then get that and
        // then start the read(s) for whatever is missing
        if (NO_MORE_DATA == getDataLength()) {
            bAsyncInProgress = readSingleBlock(msg, async);
        }
        // read until the full chunk is ready or an async read is in progress
        while (0 < getDataLength() && !bAsyncInProgress) {
            bAsyncInProgress = readSingleBlock(msg, async);
        }
        // take care of any temp buffers (unencode, etc)
        if (!bAsyncInProgress) {
            if (!moveBuffers()) {
                bAsyncInProgress = readFullChunk(msg, async);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readFullChunk returning " + bAsyncInProgress);
        }
        return bAsyncInProgress;
    }

    /**
     * When reading the zero chunk marking the end of the chunked body,
     * this method is then called to parse the trailer headers.
     *
     * @param msg
     * @param async
     * @return boolean (whether an async read is in progress)
     */
    private boolean parseTrailers(HttpBaseMessageImpl msg, boolean async) {

        try {
            this.bParsingTrailers = true;
            boolean rc = false;
            while (!rc) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parsing a buffer for trailer headers");
                }
                HttpTrailersImpl trailers = msg.createTrailers();
                addToIncomingMsgSize((getReadBuffer().remaining()));
                if (getHttpConfig().isBinaryTransportEnabled()) {
                    rc = trailers.parseBinaryHeaders(getReadBuffer(), HttpHeaderKeys.HDR_$WSAT);
                } else {
                    // always extract trailer header values
                    rc = trailers.parseHeaders(getReadBuffer(), true);
                }
                if (!rc && !isReadDataAvailable()) {
                    // use the appropriate callback class now
                    setBodyRC(TrailerCallback.getRef());
                    if (fillABuffer(1, async, false)) {
                        // async read taking place
                        return true;
                    }
                }
            }
            // once we've finished parsing the trailer headers, there may be more
            // data after it (pipelined request), so discount whatever is there
            long extra = getReadBuffer().remaining();
            if (0 < extra) {
                addToIncomingMsgSize(-extra);
            }

        } catch (Exception mhe) {
            // we have no way of communicating back to the application channel
            // that the parse error was encountered, so just log it and return
            // back out
            com.ibm.ws.ffdc.FFDCFilter.processException(mhe, getClass().getName() + ".parseTrailers", "1915", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "parseTrailers caught exception: " + mhe);
            }
            setPersistent(false);
        }
        this.bParsingTrailers = false;
        return false;
    }

    /**
     * Take the given character value and add it to the current "length" being
     * calculated.
     *
     * @param ch
     * @param inLen
     * @return int
     * @throws IllegalHttpBodyException
     */
    private int convertCharToLength(int ch, int inLen) throws IllegalHttpBodyException {

        // if we're just starting, then init the value
        int length = inLen;
        if (HeaderStorage.NOTSET == length) {
            length = 0;
        }

        // if we're dealing with a valid character, we need to multiply the
        // current length by 16 (2^4) and then add the current char
        int mod;
        if ('0' <= ch && '9' >= ch) {
            mod = ch - '0';
        } else if ('a' <= ch && 'f' >= ch) {
            mod = ch - 'a' + 10;
        } else if ('A' <= ch && 'F' >= ch) {
            mod = ch - 'A' + 10;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Client sent a bad chunk: " + ch);
            }
            throw new IllegalHttpBodyException("Illegal chunk length digit: " + ch);
        }
        length <<= 4;
        length += mod;
        return length;
    }

    /**
     * Query what the current value is for the saved chunk length.
     *
     * @return int
     */
    private int getSavedChunkLength() {
        return this.savedChunkLength;
    }

    /**
     * Set the saved chunk length value to the input number.
     *
     * @param length
     */
    private void setSavedChunkLength(int length) {
        this.savedChunkLength = length;
    }

    /**
     * Read and parse the chunk length marker from the input WsByteBuffer. If
     * the buffer runs out of data, then this will save the "currently parsed"
     * length and build on that the next time through.
     *
     * @param buff
     * @return int (NOT_ENOUGH_DATA if we ran out of data)
     * @throws IllegalHttpBodyException
     */
    private int readChunkLength(WsByteBuffer buff) throws IllegalHttpBodyException {

        if (null == buff) {
            return NOT_ENOUGH_DATA;
        }

        // default to whatever might be saved (-1 if no previous data)
        int length = getSavedChunkLength();
        int position = buff.position();
        int limit = buff.limit();
        byte ch = 0;

        if (HttpInternalConstants.PARSING_CHUNK_LENGTH == getChunkLengthParsingState()) {
            // parse the length until CRLF, semi-colon, or end of buffer
            for (; position < limit; position++) {
                ch = buff.get();
                addToIncomingMsgSize(1L);
                // possible delimiters include CRLF and a semi-colon (if there is
                // a chunk-extension it's "hexSize ; name < = value >"
                if (BNFHeaders.CR == ch || BNFHeaders.LF == ch) {
                    // if we haven't seen a valid length character yet, just
                    // ignore the CRLF
                    if (HeaderStorage.NOTSET == length) {
                        continue;
                    }
                    // need to skip past the crlfs now
                    setChunkLengthParsingState(HttpInternalConstants.PARSING_CRLF);
                    break;
                } else if (BNFHeaders.SEMICOLON == ch || BNFHeaders.SPACE == ch || BNFHeaders.TAB == ch) {
                    // need to skip past the extension and find the crlfs
                    // treat whitespace as the end-length marker too
                    setChunkLengthParsingState(HttpInternalConstants.PARSING_CHUNK_EXTENSION);
                    break;
                }
                length = convertCharToLength(ch, length);
            }
            position++;
        }

        if (HttpInternalConstants.PARSING_CHUNK_EXTENSION == getChunkLengthParsingState()) {
            // if we don't currently have a length, then this is an invalid
            // chunk length marker
            if (HeaderStorage.NOTSET == length) {
                throw new IllegalHttpBodyException("Missing chunk length");
            }
            // parse past the bytes until the CRLF or end of buffer
            for (; position < limit; position++) {
                ch = buff.get();
                addToIncomingMsgSize(1L);
                if (BNFHeaders.CR == ch || BNFHeaders.LF == ch) {
                    // now skip past the CRLFS
                    setChunkLengthParsingState(HttpInternalConstants.PARSING_CRLF);
                    break;
                }
            }
            position++;
        }

        if (HttpInternalConstants.PARSING_CRLF == getChunkLengthParsingState()) {
            // read a byte if it's available. We've already hit one CRLF char
            // in order to be in this block so we're just looking for the 2nd
            if (position < limit) {
                ch = buff.get();
                addToIncomingMsgSize(1L);
                if (BNFHeaders.CR != ch && BNFHeaders.LF != ch) {
                    // if it wasn't a CRLF then reset position back one
                    buff.position(position);
                    //PI33453
                    position++;
                }
                if (getHttpConfig().shouldWaitForEndOfMessage() && length == 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Parsing End of Message");
                    }
                    setChunkLengthParsingState(HttpInternalConstants.PARSING_END_OF_MESSAGE);
                    position++;
                } else {
                    //PI33453 End

                    setChunkLengthParsingState(GenericConstants.PARSING_NOTHING);
                    setSavedChunkLength(HeaderStorage.NOTSET);
                    return length;
                }
            }
        }

        //PI33453 Begin
        if (HttpInternalConstants.PARSING_END_OF_MESSAGE == getChunkLengthParsingState()) {
            // see if there are additional bytes available. We've already hit one CRLF
            // sequence after the 0 byte chunk, so we're just looking to see if there is
            // any more data available.
            if (position < limit) {
                setChunkLengthParsingState(GenericConstants.PARSING_NOTHING);
                setSavedChunkLength(HeaderStorage.NOTSET);
                return length;
            }
            // if data is not available then go to the layer below us to see if
            // anything new has been provided

        }
        //PI33453 End

        // if we're here then we ran out of data in the buffer
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readChunkLength: Not enough data, storing [" + length + "]");
        }
        setSavedChunkLength(length);
        return NOT_ENOUGH_DATA;
    }

    /**
     * Query what the stored limit value is.
     *
     * @return int
     */
    final public int getOldLimit() {
        return this.oldLimit;
    }

    /**
     * Set the stored limit value to the input number.
     *
     * @param limit
     */
    final public void setOldLimit(int limit) {
        this.oldLimit = limit;
    }

    /**
     * Query how much data was requested to be read.
     *
     * @return int
     */
    private int getAmountBeingRead() {
        return this.amountBeingRead;
    }

    /**
     * Set the global variable of the amount requested to be read.
     *
     * @param amount
     */
    private void setAmountBeingRead(int amount) {
        this.amountBeingRead = amount;
    }

    /**
     * When a read is necessary, this method will read into the current
     * read bytebuffer if there is space, or it will allocate a new buffer
     * and read into that.
     *
     * @param amount
     * @param async
     * @param throwException
     *                           - if an IOException hits, should it be swallowed
     *                           quietly or thrown back to the caller
     * @return boolean -- true means that an async read is in progress,
     *         false means that there is new data in the currentReadBB to use
     */
    final protected boolean fillABuffer(long amount, boolean async, boolean throwException) throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "fillABuffer(i,b) " + amount + " " + async);
        }
        // default the request size to the config size
        int size = getHttpConfig().getIncomingBodyBufferSize();
        if (amount < HttpConfigConstants.MIN_BUFFER_SIZE) {
            // if we only want a small amount of data, then request the
            // minimum buffer size
            size = HttpConfigConstants.MIN_BUFFER_SIZE;
        } else if (amount < size) {
            // if the amount is less that the default buffer size then
            // request just enough for the target amount
            size = (int) amount;
        }

        // The configuration can force a JIT allocate read, which is necessary
        // on z/OS channels for instance.
        if (getHttpConfig().isJITOnlyReads()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config forcing JIT read");
            }

            // If we had an old buffer, then free it if it doesn't contain headers
            if (null != getReadBuffer() && !lastBufferContainsHeaders()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Returning non-header buffer before JIT read: " + getReadBuffer());

                returnLastBuffer().release();
            }

            setupJITRead(size);

        } else {

            WsByteBuffer currentBuffer = getReadBuffer();
            // otherwise prepare to read into a buffer we create (or re-use)
            if (null == currentBuffer || currentBuffer.limit() == currentBuffer.capacity()) {
                // no buffer or no space left in buffer

                // If the old buffer was full, then free it if it doesn't contain
                // headers
                if (null != currentBuffer && !lastBufferContainsHeaders()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Returning non-header buffer: " + currentBuffer);

                    returnLastBuffer().release();
                }

                setReadBuffer(allocateBuffer(size));
                // WSBB pooling will set limit equal to "size" and not necessarily
                // the capacity... clear() it so we can use the entire buffer
                // i.e. requesting 28K bytes gives a 32K buffer but limit is 28K
                getReadBuffer().clear();
                setOldLimit(0);
            } else {
                // re-use this buffer
                configurePreReadBuffer(currentBuffer);
            }
            // reset the buffer "available size" based on the results above
            size = getReadBuffer().capacity() - getOldLimit();
            setupNonJITRead();
        }
        setAmountBeingRead((amount > size) ? size : (int) amount);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Filling buffer " + getReadBuffer() + " with amount " + getAmountBeingRead());
        }

        // handle the special async reads first
        if (async) {
            VirtualConnection vc = getTSC().getReadInterface().read(getAmountBeingRead(), getBodyRC(), isForceAsync(), getReadTimeout());
            if (null == vc) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "fillABuffer(i,b): async read in progress");
                }
                return true;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "fillABuffer(i,b) read() returned immediately");
            }
        } else {
            // now try the sync reads
            try {
                getTSC().getReadInterface().read(getAmountBeingRead(), getReadTimeout());
                setAmountBeingRead(0);
            } catch (IOException ioe) {
                // no FFDC required
                // an IOException while reading the body probably means an HTTP/1.0
                // server closed the connection after sending the body
                // (no CL or chunk)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IOException, closing the reads: " + ioe);
                }
                prepareClosure();
                if (throwException) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Throwing exception back to caller.");
                    }
                    throw ioe;
                }
                return false;
            }
        }
        // if the async worked immediately or we did a sync read, then get data
        // reset our "current" read buffer link
        getNextReadBuffer();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "fillABuffer(i,b) data ready in " + getReadBuffer());
        }
        return false;
    }

    /**
     * Empty out the array of buffers in storage.
     *
     */
    protected void clearStorage() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Clearing buffer storage; size=" + this.storage.size());
        }
        while (!this.storage.isEmpty()) {
            WsByteBuffer buffer = this.storage.removeFirst();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Releasing: " + buffer);
            }
            buffer.release();
        }
    }

    /**
     * Empty out the array of temporary buffers in storage.
     *
     */
    protected void clearTempStorage() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Clearing temp storage; size=" + this.tempBuffers.size());
        }
        while (!this.tempBuffers.isEmpty()) {
            WsByteBuffer buffer = this.tempBuffers.removeFirst();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Releasing: " + buffer);
            }
            buffer.release();
        }
    }

    /**
     * Print out the storage buffers for debugging.
     *
     * @param buffers
     */
    final protected void debugPrintStorage(WsByteBuffer[] buffers) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            for (int i = 0; i < buffers.length; i++) {
                Tr.debug(tc, "debug: buffers[" + i + "]: " + ((null != buffers[i]) ? WsByteBufferUtils.asString(buffers[i]) : "null"));
            }
        }
    }

    /**
     * Method to pull all the non-null buffers out of storage and return them
     * in a clean array.
     *
     * @return WsByteBuffer[] (null if no buffers ready)
     */
    protected WsByteBuffer[] getAllStorageBuffers() {
        if (this.storage.isEmpty()) {
            return null;
        }
        WsByteBuffer[] output = new WsByteBuffer[this.storage.size()];
        this.storage.toArray(output);
        this.storage.clear();
        return output;
    }

    /**
     * Method to access all the non-null buffers in storage and return them
     * in a clean array. This does not remove them from storage like the
     * get API version does.
     *
     * @return WsByteBuffer[] (null if no buffers ready)
     */
    protected WsByteBuffer[] queryAllStorageBuffers() {
        if (this.storage.isEmpty()) {
            return null;
        }
        WsByteBuffer[] output = new WsByteBuffer[this.storage.size()];
        this.storage.toArray(output);
        return output;
    }

    /**
     * Setup the decompression handler for the incoming.
     */
    protected void setupDecompressionHandler() {
        if (null != this.decompressHandler) {
            // already done
            return;
        }
        if (!getHttpConfig().isAutoDecompressionEnabled() || isRawBody()) {
            this.decompressHandler = new IdentityInputHandler();
        } else {
            if (ContentEncodingValues.GZIP.equals(this.incomingMsgEncoding)) {
                this.decompressHandler = new GzipInputHandler();
            } else if (ContentEncodingValues.XGZIP.equals(this.incomingMsgEncoding)) {
                this.decompressHandler = new GzipInputHandler();
            } else if (ContentEncodingValues.DEFLATE.equals(this.incomingMsgEncoding)) {
                this.decompressHandler = new DeflateInputHandler();
            } else {
                // unknown encoding
                this.decompressHandler = new IdentityInputHandler();
            }
        }
    }

    /**
     * Setup the compression handler for the outgoing body if one is required.
     *
     * @param msg
     */
    private void setupCompressionHandler(HttpBaseMessageImpl msg) {
        if (!isOutgoingBodyValid() && isOutgoingMsgEncoded()) {
            // turn off the requested flag if we can't encode
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Removing disallowed encoding request flag: " + this.outgoingMsgEncoding);
            }
            this.outgoingMsgEncoding = DEFAULT_ENCODING;
            return;
        }

        // if auto-compression is enabled, create the appropriate handler
        if (isAutoCompression(msg)) {
            // If this is http2, make sure the output buffer size is <= the http2 max frame size
            Integer bufferSize = 32768;
            if (this.getVC() instanceof H2VirtualConnectionImpl) {
                Integer maxFrameSize = (Integer) this.getVC().getStateMap().get("h2_frame_size");
                if (maxFrameSize != null && maxFrameSize < bufferSize) {
                    bufferSize = maxFrameSize;
                }
            }
            if (isGZipEncoded() || isXGZipEncoded()) {
                this.compressHandler = new GzipOutputHandler(isXGZipEncoded(), bufferSize);
            } else if (isZlibEncoded()) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Buffer size used for compression is: " + bufferSize);
                }
                if (isInboundConnection()) {
                    // inbound connection, check the client User-Agent header
                    this.compressHandler = new DeflateOutputHandler(getRequest().getHeader(HttpHeaderKeys.HDR_USER_AGENT).asBytes(), bufferSize);
                } else {
                    this.compressHandler = new DeflateOutputHandler(bufferSize);
                }
            }
        }
        if (null != this.compressHandler && msg.shouldUpdateBodyHeaders()) {
            // remove Content-Length header as we are going to be changing data
            msg.removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);
        }
    }

    /**
     * Once we are done receiving the body, this method will set all of
     * the various temporary variables correctly to signify that.
     */
    final public void prepareClosure() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Preparing connection for closure");
        }
        // an exception had to occur for us to get here so make sure we close
        // the connection after this.
        setPersistent(false);
        setBodyComplete();
        setDataLength(0);
        setAmountBeingRead(0);
        getTSC().getReadInterface().setBuffers(null);
        try {
            moveBuffers();
        } catch (IllegalHttpBodyException exc) {
            // no FFDC required, just ignore the exceptions since we're closing
        }
    }

    /**
     * Buffers being read from the TCP Channel can either be gotten one at a
     * time (getBodyBuffer API) or might need to be read multiple buffers at
     * a time (getBodyBuffers API or if we need to strip encoding off prior
     * to returning one single buffer). The buffer array storage handles
     * keeping those possibilities contained. Any buffer present in this
     * storage has been parsed and already unencoded if necessary.
     *
     * @return WsByteBuffer (null if no buffer ready)
     */
    protected WsByteBuffer getNextBuffer() {
        WsByteBuffer buffer = (!this.storage.isEmpty()) ? this.storage.removeFirst() : null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getNextBuffer returning " + buffer);
        }
        return buffer;
    }

    /**
     * Move the temporary buffer used by the single readX() methods into the
     * temporary array storage. This is used when the array of buffers must
     * be decoded prior to returning any of them to callers of the
     * getBodyBuffer(s) methods.
     *
     * @param buffer
     */
    protected void storeTempBuffer(WsByteBuffer buffer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Storing buffer: " + buffer);
        }
        if (null == this.decompressHandler || !this.decompressHandler.isEnabled()) {
            this.storage.add(buffer);
        } else {
            this.tempBuffers.add(buffer);
        }
    }

    /**
     * Store a single buffer into main return storage.
     *
     * @param buffer
     */
    public void storeBuffer(WsByteBuffer buffer) {
        this.storage.add(buffer);
    }

    /**
     * For asynchronous reads, we need some way of knowing whether we're
     * doing a read for multiple buffers (getBodyBuffersAsynch) or just
     * one single buffer (getBodyBufferAsynch).
     *
     * @param bRead
     */
    final protected void setMultiRead(boolean bRead) {
        this.bIsMultiRead = bRead;
    }

    /**
     * Query whether we're doing an asynch read for one or multiple buffers.
     *
     * @return boolean (true=multiple buffer read)
     */
    private boolean isMultiRead() {
        return this.bIsMultiRead;
    }

    /**
     * This continues the body processing code after an asynchronous read
     * has completed.
     *
     */
    final public void continueRead() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Continuing read...");
        }
        final HttpBaseMessageImpl msg = getMessageBeingParsed();
        if (this.bParsingTrailers) {
            if (!parseTrailers(msg, true)) {
                // finished parsing the trailers... pass back to app channel now
                getAppReadCallback().complete(getVC());
            }
            return;
        }
        if (!incomingBuffersReady() && isBodyComplete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Nothing to read");
            }
            return;
        }

        // read the buffers
        getNextReadBuffer();
        try {
            boolean rc = (isMultiRead()) ? readBodyBuffers(msg, true) : readBodyBuffer(msg, true);
            if (!rc) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling complete on application channel.");
                }
                getAppReadCallback().complete(getVC());
            }
        } catch (IOException ioe) {
            // no FFDC required
            getAppReadCallback().error(getVC(), ioe);
            return;
        } catch (BodyCompleteException bce) {
            // No FFDC required
            // not possible
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception: " + bce);
            }
            getAppReadCallback().error(getVC(), bce);
            return;
        }
    }

    /**
     * Query the SSL context information. This returns null if there is no
     * SSL information.
     *
     * @return SSLConnectionContext
     */
    @Override
    public SSLConnectionContext getSSLContext() {
        return this.myTSC.getSSLContext();
    }

    /*
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#abort()
     */
    @Override
    public void abort() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Attempting to abort current and future IO: " + getVC());
        }
        try {
            this.myTSC.getReadInterface().read(1, TCPRequestContext.ABORT_TIMEOUT);
        } catch (Throwable t) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error aborting read: " + t);
            }
        }
        try {
            this.myTSC.getWriteInterface().write(1, TCPRequestContext.ABORT_TIMEOUT);
        } catch (Throwable t) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error aborting write: " + t);
            }
        }
    }

    /**
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#cancelOutstandingRead()
     */
    @Override
    public boolean cancelOutstandingRead() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Attempting to cancel an outstanding read: " + getVC());
        }
        try {
            this.myTSC.getReadInterface().read(1, null, false, TCPRequestContext.IMMED_TIMEOUT);
        } catch (IllegalArgumentException iae) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cancelOutstandingRead: tcp layer does not support");
            }
            return false;
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".cancelOutstandingRead", "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cancelOutstandingRead: unexpected exception from tcp: " + t);
            }
            return false;
        }
        return true;
    }

    /**
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#cancelOutstandingWrite()
     */
    @Override
    public boolean cancelOutstandingWrite() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Attempting to cancel an outstanding write: " + getVC());
        }
        try {
            this.myTSC.getWriteInterface().write(1, null, false, TCPRequestContext.IMMED_TIMEOUT);
        } catch (IllegalArgumentException iae) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cancelOutstandingWrite: tcp layer does not support");
            }
            return false;
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName() + ".cancelOutstandingWrite", "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cancelOutstandingWrite: unexpected exception from tcp: " + t);
            }
            return false;
        }
        return true;
    }

    /**
     * Access the read cancel wrapper object.
     *
     * @return CancelIOWrapper
     */
    public CancelIOWrapper getReadCancel() {
        if (null == this.cancelRead) {
            this.cancelRead = new CancelIOWrapper();
        }
        return this.cancelRead;
    }

    /**
     * Access the write cancel wrapper object.
     *
     * @return CancelIOWrapper
     */
    public CancelIOWrapper getWriteCancel() {
        if (null == this.cancelWrite) {
            this.cancelWrite = new CancelIOWrapper();
        }
        return this.cancelWrite;
    }

    /**
     * Try to set the read-cancel attempt to success. This may fail if it is
     * being attempted too late (already completed for example)
     *
     * @return boolean on whether the state change worked
     */
    public boolean markReadCancelSuccess() {
        if (getReadCancel().success()) {
            if (null != getReadBuffer()) {
                // set the limit back to position so that if a future read is attempted
                // we don't mistakenly think data is already available.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Reset after canceled read is updating buffer: " + getReadBuffer());
                }
                getReadBuffer().limit(getReadBuffer().position());
            }
            return true;
        }
        return false;
    }

    /**
     * Try to set the write-cancel attempt to success. This may fail if it is
     * being attempted too late (already completed for example).
     *
     * @return boolean on whether the state change worked
     */
    public boolean markWriteCancelSuccess() {
        return getWriteCancel().success();
    }

    /**
     * Record the failure to cancel a read IO request, assuming a cancel was
     * in progress.
     */
    public void markReadCancelFailure() {
        getReadCancel().failure();
    }

    /**
     * Record the failure to cancel a write IO request, assuming a cancel was
     * in progress.
     */
    public void markWriteCancelFailure() {
        getWriteCancel().failure();
    }

    /**
     * Load any appropriate body for this HTTP error scenario.
     *
     * @param error
     * @param request
     * @param response
     * @return WsByteBuffer[]
     */
    protected WsByteBuffer[] loadErrorBody(HttpError error, HttpRequestMessage request, HttpResponseMessage response) {

        WsByteBuffer[] body = error.getErrorBody();
        if (null != body) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "HttpError returned body of length=" + body.length);
            }
            getVC().getStateMap().put(EPS_KEY, body);
            return body;
        }
        HttpErrorPageService eps = (HttpErrorPageService) HttpDispatcher.getFramework().lookupService(HttpErrorPageService.class);
        if (null == eps) {
            return null;
        }
        // found the error page service, load the pieces we need and then
        // query for any configured body
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Querying service for port=" + getLocalPort());
        }
        HttpErrorPageProvider provider = eps.access(getLocalPort());
        if (null != provider) {
            String host = getLocalAddr().getHostName();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Querying provider for host=" + host);
            }
            try {
                body = provider.accessPage(host, getLocalPort(), request, response);
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName() + ".loadErrorBody", "1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception while calling into provider, t=" + t);
                }
            }
            if (null != body) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received body of length=" + body.length);
                }
                getVC().getStateMap().put(EPS_KEY, body);
            }
        }
        return body;
    }

    /**
     * Returns the address of the remote client that created this inbound
     * request.
     *
     * @return InetAddress
     */
    @Override
    final public InetAddress getRemoteAddr() {
        return this.myRemoteAddr;
    }

    /**
     * Set the remote address value.
     *
     * @param addr
     */
    final public void setRemoteAddr(InetAddress addr) {
        this.myRemoteAddr = addr;
    }

    /**
     * Returns the local address that is the target of the request. This will
     * come from the Host header if present, otherwise from the requested URL if
     * present, or from the TCP socket itself if nothing else exists.
     *
     * @return InetAddress
     */
    @Override
    final public InetAddress getLocalAddr() {
        return this.myLocalAddr;
    }

    /**
     * Set the local address variable.
     *
     * @param addr
     */
    final public void setLocalAddr(InetAddress addr) {
        this.myLocalAddr = addr;
    }

    /**
     * Query the value of the port at the remote end of the connection, relative
     * to this server.
     *
     * @return int
     */
    @Override
    final public int getRemotePort() {
        return this.myRemotePort;
    }

    /**
     * Set the remote port variable.
     *
     * @param port
     */
    final public void setRemotePort(int port) {
        this.myRemotePort = port;
    }

    /**
     * Returns the local server port (9080, etc) that was the target of the
     * request.
     *
     * @return int
     */
    @Override
    final public int getLocalPort() {
        return this.myLocalPort;
    }

    /**
     * Set the local port variable.
     *
     * @param port
     */
    final public void setLocalPort(int port) {
        this.myLocalPort = port;
    }

    /**
     * Take the last allocated buffer off of the "free later" list and give it
     * to the caller.
     *
     * @return WsByteBuffer (null if there is no buffer)
     */
    public WsByteBuffer returnLastBuffer() {
        WsByteBuffer buffer = this.currentReadBB;
        this.allocatedBuffers.remove(buffer);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Returning " + buffer);
        }
        return buffer;
    }

    /**
     * Mark that we have reached the last buffer that contains HTTP header
     * information.
     */
    protected void setLastHeaderBuffer() {
        this.lastHeaderBuffer = this.allocatedBuffers.size();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Last header buffer: " + this.lastHeaderBuffer);
        }
    }

    /**
     * Check if the last buffer contains headers. If not, then it can be released.
     *
     * @return boolean - true if last buffer contains headers
     */
    protected boolean lastBufferContainsHeaders() {
        // if we haven't reached the last header buffer or if the size of the
        // list still matches what it was on the last buffer, return true
        return (HeaderStorage.NOTSET == this.lastHeaderBuffer || this.allocatedBuffers.size() <= this.lastHeaderBuffer);
    }

    /**
     * Store this buffer on the allocated list to free later.
     *
     * @param buffer
     */
    public void storeAllocatedBuffer(WsByteBuffer buffer) {
        this.allocatedBuffers.add(buffer);
    }

    /**
     * Store a list of new buffers onto the allocated list to free later.
     *
     * @param list
     */
    public void storeAllocatedBuffers(WsByteBuffer[] list) {
        for (int i = 0; i < list.length; i++) {
            this.allocatedBuffers.add(list[i]);
        }
    }

    /*
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#getDateFormatter()
     */
    @Override
    public HttpDateFormat getDateFormatter() {
        return HttpDispatcher.getDateFormatter();
    }

    /*
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#getEncodingUtils()
     */
    @Override
    public EncodingUtils getEncodingUtils() {
        return HttpDispatcher.getEncodingUtils();
    }

    /**
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#getStartNanoTime()
     */
    @Override
    abstract public long getStartNanoTime();

    /**
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#resetStartTime()
     */
    @Override
    abstract public void resetStartTime();

    /**
     * @see com.ibm.wsspi.http.channel.HttpServiceContext#setStartTime()
     */
    @Override
    abstract public void setStartTime();

    /**
     * @return
     */
    public boolean isPushPromise() {
        return this.isPushPromise;
    }

    public void setPushPromise(boolean isPushPromise) {
        this.isPushPromise = isPushPromise;
    }

    public boolean isH2Connection() {
        return this.isH2Connection;
    }

    public void setH2Connection(boolean isH2Connection) {
        this.isH2Connection = isH2Connection;
    }

    /*
     * Try to push a request, created from the link header rel=preload to the wc.
     */
    private void handleH2LinkPreload(HeaderField header, HttpInboundLink link) {

        /*
         * This method has two main actions:
         * 1. Create and send a push promise frame to the client on the original stream
         * 2. Create and send a headers frame up to webcontainer as if it came in on the
         * promised stream
         *
         * Example link header:
         * Link: </app/script.js>; rel=preload; as=script
         */
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "handleH2LinkPreload()");
        }

        // Get the existing stream id
        int streamId = ((H2HttpInboundLinkWrap) link).getStreamId();
        // Get the uri from the link header
        String uri = header.asString().substring(header.asString().indexOf('<') + 1, header.asString().indexOf('>'));

        // Encode headers for the push_promise frame, add them to the headerBlockFragment
        H2HeaderTable h2WriteTable = ((H2HttpInboundLinkWrap) link).muxLink.getWriteTable();
        ByteArrayOutputStream ppHb = new ByteArrayOutputStream();
        try {
            // Add the four required pseudo headers to the push_promise frame header block fragment
            // :method
            ppHb.write(H2Headers.encodeHeader(h2WriteTable, HpackConstants.METHOD, "GET", LiteralIndexType.NOINDEXING));

            // :scheme
            String scheme = new String("https");
            if (!this.isSecure()) {
                scheme = new String("http");
            }
            ppHb.write(H2Headers.encodeHeader(h2WriteTable, HpackConstants.SCHEME, scheme, LiteralIndexType.NOINDEXING));

            // :path
            ppHb.write(H2Headers.encodeHeader(h2WriteTable, HpackConstants.PATH, uri, LiteralIndexType.NOINDEXING));

            // :authority
            // If the :authority header was sent in the request, get the information from there
            // If it was not, use getLocalAddr and and getLocalPort to create it
            // If it's still null, we have to bail, since it's a required header in a push_promise frame
            String auth = ((H2HttpInboundLinkWrap) link).muxLink.getAuthority();
            if (null == auth) {
                auth = getLocalAddr().getHostName();
                if (null != auth) {
                    if (0 <= getLocalPort()) {
                        auth = auth + ":" + Integer.toString(getLocalPort());
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "handleH2LinkPreload(): Cannot find hostname for required :authority pseudo header");
                    }
                    return;
                }
            }
            ppHb.write(H2Headers.encodeHeader(h2WriteTable, HpackConstants.AUTHORITY, auth, LiteralIndexType.NOINDEXING));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.debug(tc, "handleH2LinkPreload(): Method is GET, authority is " + auth + ", scheme is " + scheme);
            }

        }
        // Either IOException from write, or CompressionException from encodeHeader
        catch (IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "handleH2LinkPreload(): The attempt to write an HTTP/2 Push Promise frame resulted in an IOException. Exception {0}" + ioe);
            }
            return;
        } catch (CompressionException ce) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "handleH2LinkPreload(): The attempt to encode an HTTP/2 Push Promise frame resulted in a CompressionException. Exception {0}" + ce);
            }
            return;
        }

        // Get the next available even numbered promised stream id
        int promisedStreamId = ((H2HttpInboundLinkWrap) link).muxLink.getNextPromisedStreamId();

        // Create the push_promise frame to send to the client
        FramePushPromise pushPromiseFrame = new FramePushPromise(streamId, ppHb.toByteArray(), promisedStreamId, 0, true, false, false);

        // Create a headers frame to send to wc
        FramePPHeaders headersFrame = new FramePPHeaders(streamId, ppHb.toByteArray());

        // createNewInboundLink creates new:
        // - H2VirtualConnectionImpl
        // - H2HttpInboundLinkWrap
        // - H2StreamProcessor in Idle state
        // It puts the new SP into the SPTable
        H2StreamProcessor promisedSP = ((H2HttpInboundLinkWrap) link).muxLink.createNewInboundLink(promisedStreamId);
        if (promisedSP == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                if (((H2HttpInboundLinkWrap) link).muxLink.isClosing()) {
                    Tr.exit(tc, "handleH2LinkPreload exit; cannot create new push stream - "
                                + "server is shutting down, closing link: " + link);
                } else {
                    Tr.exit(tc, "handleH2LinkPreload exit; cannot create new push stream - "
                                + "the max number of concurrent streams has already been reached on link: " + link);
                }

            }
            return;
        }
        ((H2HttpInboundLinkWrap) link).setPushPromise(true);
        // Update the promised stream state to Localreserved
        promisedSP.initializePromisedStream();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleH2LinkPreload(): push_promise stream-id is " + promisedStreamId);
        }

        // Send the push_promise frame on the existing stream
        H2StreamProcessor existingSP = ((H2HttpInboundLinkWrap) link).muxLink.getStreamProcessor(streamId);
        if (existingSP != null) {
            try {
                existingSP.processNextFrame(pushPromiseFrame, com.ibm.ws.http.channel.h2internal.Constants.Direction.WRITING_OUT);
            } catch (Http2Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "handleH2LinkPreload(): Protocol exception when sending the push_promise frame: " + e);
                }
                return;
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "handleH2LinkPreload(): The push_promise stream-id " + streamId + " has been closed.");
            }
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleH2LinkPreload(): Push promise frame sent on stream-id " + streamId);
        }

        // Kick off a new thread to handle this request
        promisedSP.sendRequestToWc(headersFrame);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "handleH2LinkPreload()");
        }

    }

    // differentiate if grpc has been pass through the normal request path already or is streaming
    private boolean firstGrpcReadComplete = false;

    // return:
    // 0 - GRPC not being used,
    // 1 - GRPC using request path first time through,
    // 2 - GRPC has finished first path and is now in streaming mode for this H2 stream
    public int getGRPCEndStream() {
        int ret = 0;
        H2HttpInboundLinkWrap link = null;

        if (GrpcServletServices.grpcInUse == false) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getGRPCEndStream(): returning: 0 - GrpcServletServices.grpcInUse is false");
            }
            return 0;
        }

        HttpInboundServiceContextImpl context = (HttpInboundServiceContextImpl) this;
        if (context.getLink() instanceof H2HttpInboundLinkWrap) {
            link = (H2HttpInboundLinkWrap) context.getLink();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getGRPCEndStream(): returning: 0 - LinkWrap not detected");
            }
            return 0;
        }

        if (link instanceof H2HttpInboundLinkWrap) {
            int streamId = link.getStreamId();
            H2StreamProcessor hsp = link.muxLink.getStreamProcessor(streamId);
            if (hsp.getEndStream()) {
                if (!firstGrpcReadComplete) {
                    firstGrpcReadComplete = true;
                    ret = 1;
                } else {
                    ret = 2;
                }
            } else {
                ret = 1;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getGRPCEndStream(): returning: " + ret);
        }
        return ret;
    }

}