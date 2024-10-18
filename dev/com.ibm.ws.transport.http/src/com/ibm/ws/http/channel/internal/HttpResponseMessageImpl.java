/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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
package com.ibm.ws.http.channel.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.internal.GenericConstants;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.genericbnf.internal.HeaderHandler;
import com.ibm.ws.http.channel.h2internal.H2HttpInboundLinkWrap;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.h2internal.hpack.H2Headers;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants.LiteralIndexType;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.genericbnf.BNFHeaders;
import com.ibm.wsspi.genericbnf.exception.MalformedMessageException;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.http.logging.DebugLog;

/**
 * Class that contains all the code and logic specific to an HTTP response
 * message.
 *
 */
public class HttpResponseMessageImpl extends HttpBaseMessageImpl implements HttpResponseMessage {

    /** RAS trace variable */
    private static final TraceComponent tc = Tr.register(HttpResponseMessageImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Serialization ID field */
    private static final long serialVersionUID = 7629585068712704258L;
    /** PK20531 - default string for Cache-Control */
    private static final byte[] NOCACHE_VALUE = GenericUtils.getEnglishBytes("no-cache=\"set-cookie, set-cookie2\"");
    /** PK20531 - Date long ago for Expires header */
    private static final byte[] LONG_AGO = GenericUtils.getEnglishBytes("Thu, 01 Dec 1994 16:00:00 GMT");

    /** Status code */
    protected transient StatusCodes myStatusCode = StatusCodes.OK;
    /** Reason phrase as a byte[] */
    private byte[] myReasonBytes = null;
    /** Reason phrase as a String */
    private transient String myReason = null;

    /**
     * Default constructor with no service context.
     * <P>
     * Warning: this object will be prone to NPEs if used prior to the init with a real service context.
     *
     */
    public HttpResponseMessageImpl() {
        super();
        setOwner(null);
        setBinaryParseState(HttpInternalConstants.PARSING_BINARY_VERSION);
    }

    /**
     * Constructor for an outgoing response.
     *
     * @param isc
     */
    public HttpResponseMessageImpl(HttpInboundServiceContext isc) {
        super();
        setOwner(isc);
        setBinaryParseState(HttpInternalConstants.PARSING_BINARY_VERSION);
        initVersion();
    }

    /**
     * Constructor for an incoming response.
     *
     * @param osc
     */
    public HttpResponseMessageImpl(HttpOutboundServiceContext osc) {
        super();
        setOwner(osc);
        setBinaryParseState(HttpInternalConstants.PARSING_BINARY_VERSION);
    }

    /**
     * Initialize this outgoing HTTP response message.
     *
     * @param sc
     */
    public void init(HttpInboundServiceContext sc) {
        setOwner(sc);
        setBinaryParseState(HttpInternalConstants.PARSING_BINARY_VERSION);
        initVersion();
    }

    /**
     * Initialize this incoming HTTP response message.
     *
     * @param sc
     */
    public void init(HttpOutboundServiceContext sc) {
        setOwner(sc);
        setBinaryParseState(HttpInternalConstants.PARSING_BINARY_VERSION);
    }

    /**
     * Initialize this outgoing response message with specific headers,
     * ie. ones stored in a cache perhaps.
     *
     * @param sc
     * @param hdrs
     */
    public void init(HttpInboundServiceContext sc, BNFHeaders hdrs) {
        setOwner(sc);
        setBinaryParseState(HttpInternalConstants.PARSING_BINARY_VERSION);
        if (null != hdrs) {
            hdrs.duplicate(this);
        }
        initVersion();
    }

    /**
     * Initialize this incoming response message with specific headers,
     * ie. ones stored in a cache perhaps.
     *
     * @param sc
     * @param hdrs
     */
    public void init(HttpOutboundServiceContext sc, BNFHeaders hdrs) {
        setOwner(sc);
        setBinaryParseState(HttpInternalConstants.PARSING_BINARY_VERSION);
        if (null != hdrs) {
            hdrs.duplicate(this);
        }
    }

    /**
     * Initialize the response version to either match the request version or
     * to the lower 1.0 version based on the channel configuration.
     */
    private void initVersion() {
        VersionValues ver = getServiceContext().getRequestVersion();
        VersionValues configVer = getServiceContext().getHttpConfig().getOutgoingVersion();
        if (VersionValues.V10.equals(configVer) && VersionValues.V11.equals(ver)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Configuration forcing 1.0 instead of 1.1");
            }
            setVersion(configVer);
        } else {
            setVersion(ver);
        }
    }

    /**
     * Set the "ownership" of this message to either a new servicecontext, i.e.
     * a message is being transfered from an inbound SC to and outbound SC, or
     * a channel wants to cache this message and would pass in null to this
     * method.
     *
     * @param hsc
     */
    public void setOwner(HttpServiceContext hsc) {
        // if we have an SC owner, set it's flag to false
        if (null != getServiceContext()) {
            getServiceContext().setResponseOwner(false);
        }
        // if the owner is a new HSC, then init the flags
        if (null != hsc) {
            super.init(hsc);
            getServiceContext().setResponseOwner(true);
            setIncoming(!getServiceContext().isInboundConnection());
            if (!isIncoming()) {
                // outgoing responses need header validation checks.. if config
                // hasn't disabled them
                setHeaderValidation(getServiceContext().getHttpConfig().isHeaderValidationEnabled());
            }
        }
    }

    /**
     * Query whether a body is expected to be present with this message. Note
     * that this is only an expectation and not a definitive answer. This will
     * check the necessary headers, status codes, etc, to see if any indicate
     * a body should be present. Without actually reading for a body, this
     * cannot be sure however.
     *
     * @return boolean (true -- a body is expected to be present)
     */
    @Override
    public boolean isBodyExpected() {

        if (VersionValues.V10.equals(getVersionValue())) {
            // if 1.0 then check the isBodyAllowed because the scenario of 1.0
            // servers dumping bodies and closing the socket without any body
            // indicator throws this logic off
            return isBodyAllowed();
        }

        // sending a body with the response is not valid for a HEAD request
        if (getServiceContext().getRequestMethod().equals(MethodValues.HEAD)) {
            return false;
        }

        // base layer checks explicit length markers (chunked, content-length);
        boolean rc = super.isBodyExpected();
        if (!rc) {
            // if content-length or chunked encoding don't explicitly mark a body
            // we could still have one if certain content headers are present since
            // a response can be sent until socket closure with no length delimiters
            rc = containsHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING) || containsHeader(HttpHeaderKeys.HDR_CONTENT_RANGE);
        }
        if (rc) {
            // if we think a body exists, then check the status code flag
            rc = this.myStatusCode.isBodyAllowed();
        }

        return rc;
    }

    /**
     * Query whether or not a body is allowed to be present for this
     * message. This is not whether a body is present, but rather only
     * whether it is allowed to be present.
     *
     * @return boolean (true if allowed)
     */
    @Override
    public boolean isBodyAllowed() {

        if (super.isBodyAllowed()) {

            // sending a body with the response is not valid for a HEAD request
            if (getServiceContext().getRequestMethod().equals(MethodValues.HEAD)) {
                return false;
            }

            // if that worked, then check the status code flag
            return this.myStatusCode.isBodyAllowed();
        }

        // no body allowed on this message
        return false;
    }

    /**
     * Query whether or not this message should allow the body length headers
     * to be updated when a body is not allowed with the message. For example,
     * a response does not allow a body when the request was a HEAD method but
     * the length delimiters are not updated in that case.
     *
     * @return boolean (whether the caller should modify body headers)
     */
    @Override
    public boolean shouldUpdateBodyHeaders() {
        // we only care whether the request was a HEAD or not
        return !MethodValues.HEAD.equals(getServiceContext().getRequestMethod());
    }

    /**
     * Clear this message for re-use.
     */
    @Override
    public void clear() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Clearing this response: " + this);
        }
        super.clear();
        this.myStatusCode = StatusCodes.OK;
        this.myReason = null;
        this.myReasonBytes = null;
    }

    /**
     * Destroy this response and return it to the factory.
     */
    @Override
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Destroying this response: " + this);
        }
        HttpObjectFactory tempFactory = getObjectFactory();
        super.destroy();
        if (null != tempFactory) {
            tempFactory.releaseResponse(this);
        }
    }

    /**
     * Set the Version and Status for a HTTP/2.0 response.
     *
     * @param pseudoHeader
     */
    @Override
    protected void setPseudoHeaders(HashMap<String, String> pseudoHeaders) throws Exception {
        //Only defined pseudo-header for a response is :status.

        for (Entry<String, String> entry : pseudoHeaders.entrySet()) {
            H2HeaderField header = new H2HeaderField(entry.getKey(), entry.getValue());
            if (!isValidPseudoHeader(header)) {
                ProtocolException pe = new ProtocolException("Invalid pseudo-header for decompression context: " + header.toString());
                pe.setConnectionError(false); // mark this as a stream error so we'll generate an RST_STREAM
                throw pe;
            }
        }
        setStatusCode(Integer.getInteger(pseudoHeaders.get(HpackConstants.STATUS)));
    }

    @Override
    protected H2HeaderTable getH2HeaderTable() {
        //Currently, Only inbound connections are supported, return null if we are
        //not inbound.
        if (getServiceContext() instanceof HttpInboundServiceContextImpl) {
            HttpInboundServiceContextImpl context = (HttpInboundServiceContextImpl) getServiceContext();
            return ((H2HttpInboundLinkWrap) context.getLink()).getWriteTable();
        }
        return null;
    }

    @Override
    protected boolean isValidPseudoHeader(H2HeaderField pseudoHeader) {
        //If the H2HeaderField being evaluated has an empty value, it is not a valid
        //pseudo-header.
        if (!pseudoHeader.getValue().isEmpty()) {
            //Evaluate if input is a valid response pseudo-header by comparing to the
            //status pseudo-header hash.
            int hash = pseudoHeader.getNameHash();
            return (hash == HpackConstants.STATUS_HASH);
        }

        return false;
    }

    @Override
    protected boolean checkMandatoryPseudoHeaders(HashMap<String, String> pseudoHeaders) {
        //All HTTP/2.0 responses MUST include one valid value for
        //':status'.
        return (pseudoHeaders.get(HpackConstants.STATUS) != null);
    }

    /**
     * Set the response line first token.
     *
     * @param token
     * @throws Exception
     */
    @Override
    protected void setParsedFirstToken(byte[] token) throws Exception {
        setVersion(token);
    }

    /**
     * Set the response line second token.
     *
     * @param token
     * @throws Exception
     */
    @Override
    protected void setParsedSecondToken(byte[] token) throws Exception {
        setStatusCode(GenericUtils.asIntValue(token));
    }

    /**
     * Set the response line third token.
     *
     * @param token
     * @throws Exception
     */
    @Override
    protected void setParsedThirdToken(byte[] token) throws Exception {
        setReasonPhrase(token);
    }

    /**
     * Query the value of the first response token (version).
     *
     * @return byte[]
     */
    @Override
    protected byte[] getMarshalledFirstToken() {
        return getVersionValue().getByteArray();
    }

    /**
     * Query the value of the second response token (status code).
     *
     * @return byte[]
     */
    @Override
    protected byte[] getMarshalledSecondToken() {
        return this.myStatusCode.getByteArray();
    }

    /**
     * Query the value of the third response token (reason phrase).
     *
     * @return byte[]
     */
    @Override
    protected byte[] getMarshalledThirdToken() {
        return getReasonPhraseBytes();
    }

    /**
     * Begin parsing line out from a given buffer. Returns boolean
     * as to whether it has found the end of the first line.
     *
     * @param buff
     * @return boolean
     * @throws MalformedMessageException
     */
    @Override
    public boolean parseBinaryFirstLine(WsByteBuffer buff) throws MalformedMessageException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseBinaryFirstLine for " + this);
            Tr.debug(tc, "Buffer: " + buff);
        }
        // check the version first if we need to
        if (getBinaryParseState() == HttpInternalConstants.PARSING_BINARY_VERSION) {
            if (!buff.hasRemaining()) {
                return false;
            }
            byte version = buff.get();
            if (version != HttpInternalConstants.BINARY_TRANSPORT_V1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unsupported binary version in message: " + version);
                }
                throw new MalformedMessageException("Invalid binary message");
            }
            setBinaryParseState(HttpInternalConstants.PARSING_VERSION_ID_OR_LEN);
            resetCacheToken(4);
        }

        // stop parsing the FirstLine when we have parsed the status or the
        // reason header or we hit the end of the buffer and need to read more
        boolean lineEnds = false;
        int value;
        while (!lineEnds) {
            // at this point, the parsed token array is always set up, so we
            // can try to fill it now
            if (!fillCacheToken(buff)) {
                return false;
            }
            switch (getBinaryParseState()) {
                case HttpInternalConstants.PARSING_VERSION_ID_OR_LEN:
                    value = GenericUtils.asInt(getParsedToken());
                    if (0 == (value & GenericConstants.KNOWN_MASK)) {
                        // known version ordinal
                        setVersion(VersionValues.getByOrdinal(value));
                        setBinaryParseState(HttpInternalConstants.PARSING_STATUS);
                        resetCacheToken(4);
                    } else {
                        // unknown version length
                        setBinaryParseState(HttpInternalConstants.PARSING_UNKNOWN_VERSION);
                        resetCacheToken(value & GenericConstants.UNKNOWN_MASK);
                    }
                    break;

                case HttpInternalConstants.PARSING_UNKNOWN_VERSION:
                    setVersion(VersionValues.find(getParsedToken()));
                    setBinaryParseState(HttpInternalConstants.PARSING_STATUS);
                    createCacheToken(4);
                    break;

                case HttpInternalConstants.PARSING_STATUS:
                    setStatusCode(GenericUtils.asInt(getParsedToken()));
                    setBinaryParseState(HttpInternalConstants.PARSING_REASON_LEN);
                    resetCacheToken(4);
                    break;

                case HttpInternalConstants.PARSING_REASON_LEN:
                    value = GenericUtils.asInt(getParsedToken());
                    if (0 == value) {
                        // end of first line response headers
                        setBinaryParseState(GenericConstants.PARSING_HDR_FLAG);
                        resetCacheToken(4);
                        lineEnds = true;
                    } else {
                        setBinaryParseState(HttpInternalConstants.PARSING_REASON);
                        resetCacheToken(value);
                    }
                    break;

                case HttpInternalConstants.PARSING_REASON:
                    setReasonPhrase(getParsedToken());
                    setBinaryParseState(GenericConstants.PARSING_HDR_FLAG);
                    createCacheToken(4);
                    lineEnds = true;
                    break;

                default:
                    throw new MalformedMessageException("Invalid state in line: " + getBinaryParseState());
            } // end of switch

        } // end of while loop

        setFirstLineComplete(true);
        return true;
    }

    /**
     * Marshall the first line.
     *
     * @return WsByteBuffer[] of line ready to be written.
     */
    @Override
    public WsByteBuffer[] marshallLine() {
        WsByteBuffer[] firstLine = new WsByteBuffer[1];
        firstLine[0] = allocateBuffer(getOutgoingBufferSize());

        firstLine = putBytes(getVersionValue().getByteArray(), firstLine);
        firstLine = putByte(SPACE, firstLine);
        if (null == this.myReasonBytes) {
            // use the default value
            firstLine = putBytes(this.myStatusCode.getStatusWithPhrase(), firstLine);
        } else {
            // put the status code then the phrase
            firstLine = putBytes(this.myStatusCode.getByteArray(), firstLine);
            firstLine = putByte(SPACE, firstLine);
            firstLine = putBytes(getReasonPhraseBytes(), firstLine);
        }
        firstLine = putBytes(BNFHeaders.EOL, firstLine);
        // don't flip the last buffer as headers get tacked on the end
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Marshalling first line: " + getVersion() + " " + getStatusCodeAsInt() + " " + getReasonPhrase());
        }
        return firstLine;
    }

    @Override
    public WsByteBuffer[] encodePseudoHeaders() {

        WsByteBuffer[] firstLine = new WsByteBuffer[1];
        firstLine[0] = allocateBuffer(getOutgoingBufferSize());

        LiteralIndexType indexType = LiteralIndexType.NOINDEXING;
        //For the time being, there will be no indexing on the responses to guarantee
        //the write context is concurrent to the remote endpoint's read context. Remote
        //intermediaries could index if they so desire, so setting NoIndexing (as
        //opposed to NeverIndexing).
        //TODO: investigate how streams and priority can work together with indexing on
        //responses.
        //LiteralIndexType indexType = isPushPromise ? LiteralIndexType.NOINDEXING : LiteralIndexType.INDEX;
        //Corresponding dynamic table

        H2HeaderTable table = this.getH2HeaderTable();
        //Current encoded pseudo-header
        byte[] encodedHeader = null;

        //Encode the Status
        try {
            encodedHeader = H2Headers.encodeHeader(table, HpackConstants.STATUS, getStatusCodeAsInt() + "", indexType);
            firstLine = putBytes(encodedHeader, firstLine);

            // don't flip the last buffer as headers get tacked on the end
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Encoding first line status pseudoheader: " + HpackConstants.STATUS + " :" + getStatusCodeAsInt());
            }
        } catch (Exception e) {
            // Three possible scenarios -
            // 1.) unsupported encoding used when converting string to bytes on
            // Hpack encoding. This should never happen as it is set to always use
            // US-ASCII.
            // 2.) Decompression exception for invalid Hpack decode scenario
            // Show error and return null, so caller can invalidate the table
            // and close the stream.
            // 3.) IOException for not being able to write into Byte Array stream
            Tr.error(tc, e.getMessage());

            // Release the buffer that was just allocated.
            firstLine[0].release();
            firstLine = null;
        }

        return firstLine;
    }

    /**
     * Called for marshalling the first line of binary HTTP responses.
     *
     * @return WsByteBuffer[]
     */
    @Override
    public WsByteBuffer[] marshallBinaryFirstLine() {
        WsByteBuffer[] firstLine = new WsByteBuffer[1];
        firstLine[0] = allocateBuffer(getOutgoingBufferSize());

        // prepend the binary protocol version first
        firstLine = putByte(HttpInternalConstants.BINARY_TRANSPORT_V1, firstLine);
        if (getVersionValue().isUndefined()) {
            // unknown version....marshall (masked(len)+Str)
            byte[] data = getVersionValue().getByteArray();
            firstLine = putInt(data.length | GenericConstants.KNOWN_MASK, firstLine);
            firstLine = putBytes(data, firstLine);
        } else {
            // known version...marshall versionID
            firstLine = putInt(getVersionValue().getOrdinal(), firstLine);
        }

        // Status Code
        firstLine = putInt(this.myStatusCode.getIntCode(), firstLine);

        if (null != this.myReasonBytes) {
            // Reason exists....marshall (len+Str)
            firstLine = putInt(this.myReasonBytes.length, firstLine);
            firstLine = putBytes(this.myReasonBytes, firstLine);
        } else {
            // reason does not exist...marshall 0 as len
            firstLine = putInt(0, firstLine);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Marshalling binary first line: " + getVersion() + " " + getStatusCodeAsInt() + " " + getReasonPhrase());
        }
        // don't flip the last buffer as headers get tacked on the end
        return firstLine;
    }

    /**
     * Called the parsing of the first line is complete. Performs checks
     * on whether all the necessary data has been found.
     *
     * @throws MalformedMessageException
     */
    @Override
    protected void parsingComplete() throws MalformedMessageException {

        // HTTP-Version SP Status-Code SP Reason-Phrase CRLF
        // PK20069: reason-phrase is 0 or more characters

        int num = getNumberFirstLineTokens();
        if (3 != num && 2 != num) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "numFirstLineTokensRead is " + getNumberFirstLineTokens());
            }
            if (getServiceContext().getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.WARN)) {
                getServiceContext().getHttpConfig().getDebugLog().log(DebugLog.Level.WARN, HttpMessages.MSG_PARSE_INVALID_FIRSTLINE, getServiceContext());
            }
            throw new MalformedMessageException("Received " + getNumberFirstLineTokens() + " first line tokens");
        }
    }

    /**
     * Before marshalling headers into a buffer, this will run the data
     * through a compliancy check and take appropriate action (throw
     * errors, add missing headers, etc).
     *
     * @throws MessageSentException
     */
    @Override
    public void headerComplianceCheck() throws MessageSentException {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEntryEnabled()) {
            Tr.entry(tc, "headerComplianceCheck");
        }

        super.headerComplianceCheck();

        // 339972 - responses must have the Date header
        if (!containsHeader(HttpHeaderKeys.HDR_DATE)) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating a missing Date header");
            }
            setCurrentDate();
        }

        if (getServiceContext().getHttpConfig().removeServerHeader()) {
            // @PK15848 - configuration says to remove it if present
            if (containsHeader(HttpHeaderKeys.HDR_SERVER)) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Configuration forcing removal of Server header");
                }
                removeSpecialHeader(HttpHeaderKeys.HDR_SERVER);
            }
        } else if (!containsHeader(HttpHeaderKeys.HDR_SERVER)) {
            // PM87031 (PM75371) Start
            // only add value of the Server Header if ServerHeaderValue property has a value
            byte[] serverHeader = getServiceContext().getHttpConfig().getServerHeaderValue();
            if (null != serverHeader) {
                setSpecialHeader(HttpHeaderKeys.HDR_SERVER, serverHeader);
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding the Server header value: " + getHeader(HttpHeaderKeys.HDR_SERVER).asString());
                }
            } // PM87031 (PM75371) End
        }

        // @PK20531 - add Cache-Control header if config says to
        if (getServiceContext().getHttpConfig().shouldCookiesConfigureNoCache()) {
            updateCacheControl();
        }

        if (getServiceContext().getHttpConfig().useHeadersConfiguration()) {
            //Add all headers configured through the ADD configuration option
            for (List<Map.Entry<String, String>> headers : getServiceContext().getHttpConfig().getConfiguredHeadersToAdd().values()) {
                for (Entry<String, String> header : headers) {
                    this.appendHeader(header.getKey(), header.getValue());
                }
            }

            //Set all headers configured through the SET configuration option
            for (Entry<String, String> header : getServiceContext().getHttpConfig().getConfiguredHeadersToSet().values()) {
                this.setHeader(header.getKey(), header.getValue());
            }

            //Set all headers configured through the SET_IF_MISSING configuration option
            for (Entry<String, String> header : getServiceContext().getHttpConfig().getConfiguredHeadersToSetIfMissing().values()) {
                //Only set if not present
                if (!this.containsHeader(header.getKey())) {
                    this.setHeader(header.getKey(), header.getValue());
                }
            }

            //Remove all headers configured through the REMOVE configuration option
            for (String headerName : getServiceContext().getHttpConfig().getConfiguredHeadersToRemove().values()) {
                if (this.containsHeader(headerName)) {
                    this.removeHeader(headerName);
                }
            }

        }

        if (bTrace && tc.isEntryEnabled()) {
            Tr.exit(tc, "headerComplianceCheck");
        }
    }

    /**
     * Method to update the caching related headers for a response message. This
     * will configure the message such that if Set-Cookie(2) information is
     * present then additional headers will be added to ensure that the message
     * is not cached on any intermediate caches.
     *
     */
    private void updateCacheControl() {
        // regular HTTP path, the Set-Cookie values are already put into BNFHdrs
        // but localhttp they might still be in the cookie cache
        boolean addSet1 = containsHeader(HttpHeaderKeys.HDR_SET_COOKIE) || isCookieCacheDirty(HttpHeaderKeys.HDR_SET_COOKIE);
        boolean addSet2 = containsHeader(HttpHeaderKeys.HDR_SET_COOKIE2) || isCookieCacheDirty(HttpHeaderKeys.HDR_SET_COOKIE2);
        if (!addSet1 && !addSet2) {
            // set-cookie(2) does not exist, nothing to do
            return;
        }

        // make sure the Expires header exists
        if (!containsHeader(HttpHeaderKeys.HDR_EXPIRES)) {
            // add the Expires header
            setSpecialHeader(HttpHeaderKeys.HDR_EXPIRES, LONG_AGO);
        }

        // check whether we need to update an existing Cache-Control header
        // or simply add one
        if (containsHeader(HttpHeaderKeys.HDR_CACHE_CONTROL)) {
            // need to update the existing value
            HeaderHandler handler = new HeaderHandler(this, ',', HttpHeaderKeys.HDR_CACHE_CONTROL);
            if (!handler.contains("no-cache")) {
                boolean updated = false;
                if (addSet1) {
                    updated = handler.add("no-cache", "set-cookie");
                }
                if (addSet2) {
                    updated = handler.add("no-cache", "set-cookie2") | updated;
                }
                if (updated) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Updating Cache-Control for Set-Cookie");
                    }
                    setSpecialHeader(HttpHeaderKeys.HDR_CACHE_CONTROL, handler.marshall());
                }
            }
        } else {
            // not present, just write what we want
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Adding Cache-Control due to Set-Cookie");
            }
            setSpecialHeader(HttpHeaderKeys.HDR_CACHE_CONTROL, NOCACHE_VALUE);
        }
    }

    /**
     * Query of the value of the status code as an integer.
     *
     * @return int
     */
    @Override
    public int getStatusCodeAsInt() {
        return this.myStatusCode.getIntCode();
    }

    /**
     * Query the value of the status code.
     *
     * @return StatusCodes
     */
    @Override
    public StatusCodes getStatusCode() {
        return this.myStatusCode;
    }

    /**
     * Set the status code of the response message. An input code that does
     * not match an existing defined StatusCode will create a new "Undefined"
     * code where the getByteArray() API will return the input code as a
     * byte[].
     *
     * @param code
     */
    @Override
    public void setStatusCode(int code) {
        StatusCodes val = null;
        try {
            val = StatusCodes.getByOrdinal(code);
        } catch (IndexOutOfBoundsException e) {
            // no FFDC required
            // nothing to do, just make the undefined value below
        }

        // this could be null because the ordinal lookup returned an empty
        // status code, or because it was out of bounds
        if (null == val) {
            val = StatusCodes.makeUndefinedValue(code);
        }
        setStatusCode(val);
    }

    /**
     * Set the status code of the response message.
     *
     * @param code
     */
    @Override
    public void setStatusCode(StatusCodes code) {

        if (!code.equals(this.myStatusCode)) {
            this.myStatusCode = code;
            // no matter what, empty out the reason phrase information so that it
            // will default to match this new status code
            this.myReason = null;
            this.myReasonBytes = null;
            super.setFirstLineChanged();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "setStatusCode(sc): " + code);
        }
    }

    /**
     * Query whether this response message's status code represents a temporary
     * status of 1xx.
     *
     * @return boolean
     */
    public boolean isTemporaryStatusCode() {
        int code = this.myStatusCode.getIntCode();
        //We need to check if the WebContainer spec level is greater than 3.0
        //If it is then we don't want to treat the 101 status code as a temp
        //one as it changes our behavior
        if (HttpDispatcher.useEE7Streams() && (code == 101))
            return false;
        return (100 <= code && 200 > code);
    }

    /**
     * Query the value of the reason phrase.
     *
     * @return String
     */
    @Override
    public String getReasonPhrase() {
        if (null == this.myReason) {
            this.myReason = GenericUtils.getEnglishString(getReasonPhraseBytes());
        }
        return this.myReason;
    }

    /**
     * Query the value of the reason phrase.
     *
     * @return byte[]
     */
    @Override
    public byte[] getReasonPhraseBytes() {
        if (null == this.myReasonBytes) {
            return this.myStatusCode.getDefaultPhraseBytes();
        }
        return this.myReasonBytes;
    }

    /**
     * Set the reason phrase of this response message.
     *
     * @param reason
     */
    @Override
    public void setReasonPhrase(String reason) {
        this.myReason = reason;
        this.myReasonBytes = GenericUtils.getEnglishBytes(reason);
        super.setFirstLineChanged();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "setReasonPhrase(String): set to [" + this.myReason + "]");
        }
    }

    /**
     * Set the reason phrase of this response message.
     *
     * @param reason
     */
    @Override
    public void setReasonPhrase(byte[] reason) {
        this.myReasonBytes = reason;
        this.myReason = null;
        super.setFirstLineChanged();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            this.myReason = GenericUtils.getEnglishString(reason);
            Tr.event(tc, "setReasonPhrase(byte[]): set to [" + this.myReason + "]");
        }
    }

    /**
     * @see com.ibm.wsspi.http.channel.cookies.CookieHandler#getCookieValue(java.lang.String)
     */
    @Override
    public byte[] getCookieValue(String name) {
        if (null == name) {
            return null;
        }
        byte[] val = getCookieValue(name, HttpHeaderKeys.HDR_SET_COOKIE);
        if (null == val) {
            val = getCookieValue(name, HttpHeaderKeys.HDR_SET_COOKIE2);
        }
        return val;
    }

    /**
     * @see com.ibm.wsspi.http.channel.cookies.CookieHandler#getAllCookieValues(java.lang.String)
     */
    @Override
    public List<String> getAllCookieValues(String name) {
        List<String> list = new LinkedList<String>();
        if (null != name) {
            getAllCookieValues(name, HttpHeaderKeys.HDR_SET_COOKIE, list);
            getAllCookieValues(name, HttpHeaderKeys.HDR_SET_COOKIE2, list);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAllCookieValues: Found " + list.size() + " instances of " + name);
        }
        return list;
    }

    /**
     * @see com.ibm.wsspi.http.channel.cookies.CookieHandler#getCookie(java.lang.String)
     */
    @Override
    public HttpCookie getCookie(String name) {
        if (null == name) {
            return null;
        }
        HttpCookie cookie = getCookie(name, HttpHeaderKeys.HDR_SET_COOKIE);
        if (null == cookie) {
            cookie = getCookie(name, HttpHeaderKeys.HDR_SET_COOKIE2);
        }
        // Note: return a clone to avoid corruption by the caller
        return (null == cookie) ? null : cookie.clone();
    }

    /**
     * @see com.ibm.wsspi.http.channel.cookies.CookieHandler#getAllCookies()
     */
    @Override
    public List<HttpCookie> getAllCookies() {
        List<HttpCookie> list = new LinkedList<HttpCookie>();
        getAllCookies(HttpHeaderKeys.HDR_SET_COOKIE, list);
        getAllCookies(HttpHeaderKeys.HDR_SET_COOKIE2, list);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAllCookies: Found " + list.size() + " instances");
        }
        return list;
    }

    /**
     * @see com.ibm.wsspi.http.channel.cookies.CookieHandler#getAllCookies(java.lang.String)
     */
    @Override
    public List<HttpCookie> getAllCookies(String name) {
        List<HttpCookie> list = new LinkedList<HttpCookie>();
        if (null != name) {
            getAllCookies(name, HttpHeaderKeys.HDR_SET_COOKIE, list);
            getAllCookies(name, HttpHeaderKeys.HDR_SET_COOKIE2, list);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAllCookies: Found " + list.size() + " instances of " + name);
        }
        return list;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.cookies.CookieHandler#setCookie(com.ibm.websphere
     * .http.HttpCookie, com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */
    @Override
    public boolean setCookie(HttpCookie cookie, HttpHeaderKeys header) {
        if (null == cookie || null == header) {
            return false;
        }

        // only cookie versions 0 and 1 are supported
        if (1 < cookie.getVersion()) {
            throw new IllegalArgumentException("Cookie version is invalid: " + cookie.getVersion());
        }

        if (header.equals(HttpHeaderKeys.HDR_SET_COOKIE) || header.equals(HttpHeaderKeys.HDR_SET_COOKIE2)) {
            return addCookie(cookie, header);
        }
        return false;
    }

    /**
     * @see com.ibm.wsspi.http.channel.cookies.CookieHandler#setCookie(java.lang.String, java.lang.String, com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */
    @Override
    public boolean setCookie(String name, String value, HttpHeaderKeys header) {
        // value can be null, creating the cookie will test null name for us
        if (null == header) {
            return false;
        }
        return setCookie(new HttpCookie(name, value), header);
    }

    /**
     * @see com.ibm.wsspi.http.channel.cookies.CookieHandler#removeCookie(java.lang.String, com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */
    @Override
    public boolean removeCookie(String name, HttpHeaderKeys header) {
        if (null == name || null == header) {
            return false;
        }
        if (header.equals(HttpHeaderKeys.HDR_SET_COOKIE) || header.equals(HttpHeaderKeys.HDR_SET_COOKIE2)) {
            return deleteCookie(name, header);
        }
        return false;
    }

    /**
     * Debug print this response message to the RAS trace log.
     */
    @Override
    public void debug() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Response Message: " + this);
            Tr.debug(tc, "Status: " + getStatusCodeAsInt());
            Tr.debug(tc, "Reason: " + getReasonPhrase());
            super.debug();
        }
    }

    /**
     * Duplicate this entire message.
     *
     * @return HttpResponseMessage
     */
    @Override
    public HttpResponseMessage duplicate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Duplicating the response message: " + this);
        }

        HttpResponseMessageImpl msg = null;
        HttpObjectFactory factory = getObjectFactory();
        if (null == factory) {
            msg = new HttpResponseMessageImpl();
        } else {
            msg = factory.getResponse();
        }
        msg.setIncoming(isIncoming());
        // set the response specifics
        msg.setStatusCode(this.myStatusCode);
        msg.setReasonPhrase(this.myReasonBytes);

        // set the common object values
        super.duplicate(msg);
        return msg;
    }

    /**
     * Read an instance of this object from the input stream.
     *
     * @param input
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "De-serializing into: " + this);
        }
        super.readExternal(input);
        if (SERIALIZATION_V2 == getDeserializationVersion()) {
            setStatusCode(input.readShort());
        } else {
            setStatusCode(input.readInt());
        }
        setReasonPhrase(readByteArray(input));
    }

    /**
     * Write this object instance to the output stream.
     *
     * @param output
     * @throws IOException
     */
    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Serializing: " + this);
        }
        super.writeExternal(output);
        output.writeShort(getStatusCodeAsInt());
        writeByteArray(output, this.myReasonBytes);
    }
    
    @Override
    public long getBytesWritten() {
        return this.getServiceContext().getNumBytesWritten();
    }
    
    @Override
    public long getEndTime() {
       return 0; 
    }

}
