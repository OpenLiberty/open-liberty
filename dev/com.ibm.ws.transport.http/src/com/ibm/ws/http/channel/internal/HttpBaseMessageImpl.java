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
package com.ibm.ws.http.channel.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap; //PI31734
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.genericbnf.internal.GenericMessageImpl;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.h2internal.H2HttpInboundLinkWrap;
import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.internal.cookies.CookieCacheData;
import com.ibm.ws.http.channel.internal.cookies.CookieHeaderByteParser;
import com.ibm.ws.http.channel.internal.cookies.CookieUtils;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundLink;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.genericbnf.BNFHeaders;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.genericbnf.exception.MalformedMessageException;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.genericbnf.exception.UnsupportedProtocolVersionException;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpBaseMessage;
import com.ibm.wsspi.http.channel.HttpChannelUtils;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.ExpectValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.TransferEncodingValues;
import com.ibm.wsspi.http.channel.values.VersionValues;

/**
 * Class representing all of the common data to every HTTP message. This
 * controls
 * the header storage, the trailer header storage, etc. It is extended by the
 * Request and Response messages that add their specifics to the base message.
 *
 */
public abstract class HttpBaseMessageImpl extends GenericMessageImpl implements HttpBaseMessage {

    /** RAS trace variable */
    private static final TraceComponent tc = Tr.register(HttpBaseMessageImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Serialization ID */
    private static final long serialVersionUID = -5538352173755638799L;

    /** Seperator used in header list values */
    private static final String HDR_LIST_SEPARATOR = ", ";
    /** Size to increase any of the internal arrays if necessary */
    private static final int GROWTH_SIZE = 2;
    /** Default size to the local arrays */
    private static final int DEFAULT_SIZE = 5;
    /** Static representation of a quote symbol */
    private static final byte QUOTE = '"';
    /** Default charset for ISO-8859-1 */
    private static Charset DEF_CHARSET = null;
    /** Delimiters used while parsing cookies */
    private static final byte[] COOKIE_DELIMS = { COMMA, SEMICOLON };

    /** Special variable for the Content-Length header */
    private transient long myContentLength = HeaderStorage.NOTSET;
    /** Special list for the Connection header */
    private transient ConnectionValues[] myConnectionVals = null;
    /** Index pointer into the next position of the Connection list */
    private transient int indexConnection = 0;
    /** Special list for the Transfer-Encoding header */
    private transient TransferEncodingValues[] myTransferVals = null;
    /** Index pointer into the next position of the Transfer-Encoding list */
    private transient int indexTransfer = 0;
    /** Special list of the parsed Content-Encoding values */
    private transient ContentEncodingValues[] myContentEncodingVals = null;
    /** Index pointer into the next position of the Content-Encoding list */
    private transient int indexContentEncoding = 0;

    /** Flag as to whether this message is incoming or not */
    private transient boolean bIsIncoming = false;
    /** Flag as to whether or not Keep-Alive is in the Connection header */
    private transient boolean bIsKeepAliveSet = true;
    /** Flag as to whether or not Close is in the Connection header */
    private transient boolean bIsCloseSet = false;
    /** Flag as to whether or not Chunked is in the Transfer-Encoding header */
    private transient boolean bIsChunkedEncodingSet = false;
    /** Flag whether or not the "Expect: 100-continue" header is present */
    private transient boolean bIs100Continue = false;
    /** Flag on whether this message is "committed/read-only" */
    private transient boolean bIsCommitted = false;

    /** Link to any trailer headers on the message */
    private HttpTrailersImpl myTrailers = null;
    /** HTTP version of this message */
    private VersionValues myVersion = VersionValues.V11;
    /** HTTP service context for this message */
    private transient HttpServiceContextImpl myHSC = null;

    /** Cache of those "Cookie" headers */
    protected transient CookieCacheData cookieCache = null;
    /** Cache of the "Cookie2" headers */
    protected transient CookieCacheData cookie2Cache = null;
    /** Cache of those "Set-Cookie" headers */
    protected transient CookieCacheData setCookieCache = null;
    /** Cache of those "Set-Cookie2" headers */
    protected transient CookieCacheData setCookie2Cache = null;
    /** Reference to the cookie parser */
    private transient CookieHeaderByteParser cookieParser;
    /**
     * Token delimiter object for parsing some header values - single threaded
     * only
     */
    private transient TokenDelimiter myTokenDelimiter = new TokenDelimiter();

    /**
     * Constructor for the Request/Response messages to use.
     *
     */
    protected HttpBaseMessageImpl() {
        super();
        this.myConnectionVals = new ConnectionValues[DEFAULT_SIZE];
        this.myTransferVals = new TransferEncodingValues[DEFAULT_SIZE];
        this.myContentEncodingVals = new ContentEncodingValues[DEFAULT_SIZE];
    }

    // ***********************************************************************
    // Generic utility methods for this message
    // ***********************************************************************

    /**
     * Initialize this message with a service context.
     *
     * @param hsc
     */
    protected void init(HttpServiceContext hsc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Initializing message: " + this + " with " + hsc);
        }
        if (null == hsc) {
            return;
        }

        this.myHSC = (HttpServiceContextImpl) hsc;
        HttpChannelConfig config = this.myHSC.getHttpConfig();
        super.init(config.isDirectBufferType(), config.getOutgoingHdrBufferSize(), config.getIncomingHdrBufferSize(), config.getByteCacheSize());
        setLimitOfTokenSize(config.getLimitOfFieldSize());
        setLimitOnNumberOfHeaders(config.getLimitOnNumberOfHeaders());
        if (!config.shouldPreventResponseSplit()) { //PI45266
            setCharacterValidation(false);
        }
        setRemoteIp(config.useForwardingHeaders());
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.GenericMessageImpl#clear()
     */
    @Override
    public void clear() {
        super.clear();
        // 313642 - if the individual headers are set (content-length, age, etc)
        // then the base clear() layer will trigger the resets when the header
        // is removed there
        this.myVersion = VersionValues.V11;
        this.cookieCache = null;
        this.cookie2Cache = null;
        this.setCookieCache = null;
        this.setCookie2Cache = null;
        this.bIsCommitted = false;
        // 347066 - these connection related flags do not require the Connection
        // header be present so must be explicitly cleared
        this.bIsKeepAliveSet = true;
        this.bIsCloseSet = false;
        setBinaryParseState(HttpInternalConstants.PARSING_BINARY_VERSION);
        if (null != this.myTrailers) {
            this.myTrailers.destroy();
            this.myTrailers = null;
        }
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.GenericMessageImpl#destroy()
     */
    @Override
    public void destroy() {
        super.destroy();
        // 291714 - clean up the VC statemap
        if (null != this.myHSC) {
            final HttpChannelConfig cfg = this.myHSC.getHttpConfig();
            if (null != cfg && cfg.isServantRegion()) {
                // only need to do this on the z/OS SR
                this.myHSC.getVC().getStateMap().remove(HttpConstants.SESSION_PERSISTENCE);
            }
            this.myHSC = null;
        }
        this.bIsIncoming = false;
        if (null != this.myTrailers) {
            this.myTrailers.destroy();
            this.myTrailers = null;
        }
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#findKey(byte[], int,
     * int, boolean)
     */
    @Override
    protected HeaderKeys findKey(byte[] data, int offset, int length, boolean returnNullForInvalidName) {
        return HttpHeaderKeys.find(data, offset, length, returnNullForInvalidName);
    }

    /*
     * see com.ibm.ws.genericbnf.impl.BNFHeadersImpl#findKey(byte[], boolean)
     */
    @Override
    protected HeaderKeys findKey(byte[] name, boolean returnNullForInvalidName) {
        return HttpHeaderKeys.find(name, 0, name.length, returnNullForInvalidName);
    }

    /*
     * @see
     * com.ibm.ws.genericbnf.internal.BNFHeadersImpl#findKey(java.lang.String, boolean)
     */
    @Override
    protected HeaderKeys findKey(String name, boolean returnNullForInvalidName) {
        return HttpHeaderKeys.find(name, returnNullForInvalidName);
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#filterAdd(com.ibm.wsspi.
     * genericbnf.HeaderKeys, byte[])
     */
    @Override
    protected boolean filterAdd(HeaderKeys key, byte[] value, boolean isWASPrivateHeader) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Adding: " + key.getName() + ":" + GenericUtils.getEnglishString(value));
        }
        boolean rc = true;
        if (!isWASPrivateHeader) {
            if (HttpHeaderKeys.HDR_CONTENT_LENGTH.equals(key)) {
                rc = setContentLength(value);
            } else if (HttpHeaderKeys.HDR_CONNECTION.equals(key)) {
                matchAndParseConnection(value);
            } else if (HttpHeaderKeys.HDR_TRANSFER_ENCODING.equals(key)) {
                matchAndParseTransfer(value);
            } else if (HttpHeaderKeys.HDR_CONTENT_ENCODING.equals(key)) {
                matchAndParseContent(value);
            } else if (HttpHeaderKeys.HDR_EXPECT.equals(key)) {
                matchAndParseExpect(value);
            }
        }
        return rc;
    }

    /*
     * @see
     * com.ibm.ws.genericbnf.internal.BNFHeadersImpl#filterRemove(com.ibm.wsspi
     * .genericbnf.HeaderKeys, byte[])
     */
    @Override
    protected void filterRemove(HeaderKeys key, byte[] value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Removing: " + key.getName() + ":" + GenericUtils.getEnglishString(value));
        }
        if (HttpHeaderKeys.HDR_CONTENT_LENGTH.equals(key)) {
            this.myContentLength = HeaderStorage.NOTSET;
        } else if (HttpHeaderKeys.HDR_CONNECTION.equals(key)) {
            removeConnectionHeader(value);
        } else if (HttpHeaderKeys.HDR_TRANSFER_ENCODING.equals(key)) {
            removeTransferEncodingHeader(value);
        } else if (HttpHeaderKeys.HDR_CONTENT_ENCODING.equals(key)) {
            removeContentEncodingHeader(value);
        } else if (HttpHeaderKeys.HDR_EXPECT.equals(key)) {
            removeExpectHeader(value);
        }
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.GenericMessageImpl#debug()
     */
    @Override
    public void debug() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Version: " + getVersion());
            Tr.debug(tc, "ContentLength: " + getContentLength());
            Tr.debug(tc, "CookieCache:  " + getCookieCache(HttpHeaderKeys.HDR_COOKIE));
            Tr.debug(tc, "Cookie2Cache:  " + getCookieCache(HttpHeaderKeys.HDR_COOKIE2));
            Tr.debug(tc, "SetCookieCache:  " + getCookieCache(HttpHeaderKeys.HDR_SET_COOKIE));
            Tr.debug(tc, "Set2CookieCache:  " + getCookieCache(HttpHeaderKeys.HDR_SET_COOKIE2));
            Tr.debug(tc, "isIncoming: " + isIncoming());
            Tr.debug(tc, "isKeepAliveSet: " + isKeepAliveSet());
            Tr.debug(tc, "isCloseSet: " + isCloseSet());
            Tr.debug(tc, "isConnectionSet: " + isConnectionSet());
            Tr.debug(tc, "isChunkedEncodingSet: " + isChunkedEncodingSet());
            Tr.debug(tc, "isExpect100: " + isExpect100Continue());
            Tr.debug(tc, "isBodyAllowed: " + isBodyAllowed());
            Tr.debug(tc, "isBodyExcepted: " + isBodyExpected());
            Tr.debug(tc, "isBodyEncoded: " + isBodyEncoded());
            Tr.debug(tc, "Trailers: " + getTrailersImpl());
            if (null != getTrailersImpl()) {
                getTrailersImpl().debug();
            }
            super.debug();
        }
    }

    /**
     * Duplicate the cookie caches into the given object.
     *
     * @param msg
     */
    private void duplicateCookieCaches(HttpBaseMessageImpl msg) {
        if (null != this.cookieCache) {
            msg.cookieCache = this.cookieCache.duplicate();
        }
        if (null != this.cookie2Cache) {
            msg.cookie2Cache = this.cookie2Cache.duplicate();
        }
        if (null != this.setCookieCache) {
            msg.setCookieCache = this.setCookieCache.duplicate();
        }
        if (null != this.setCookie2Cache) {
            msg.setCookie2Cache = this.setCookie2Cache.duplicate();
        }
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#removeAllHeaders()
     */
    @Override
    public void removeAllHeaders() {
        // PK47516 - clean out the Cookie caches too
        this.cookieCache = null;
        this.cookie2Cache = null;
        this.setCookieCache = null;
        this.setCookie2Cache = null;
        super.removeAllHeaders();
    }

    /**
     * Take the given object and copy local values into it, passing it
     * on to super classes. A null input message will trigger a
     * NullPointerException.
     *
     * @param msg
     */
    protected void duplicate(HttpBaseMessageImpl msg) {
        if (null == msg) {
            throw new NullPointerException("Null message");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Duplicating the base message");
        }
        // duplicate the headers before the bits below
        super.duplicate(msg);

        // common version to all messages
        msg.setVersion(getVersionValue());

        // duplicate any trailer headers
        if (null != getTrailersImpl()) {
            msg.myTrailers = getTrailersImpl().duplicate();
        }

        // Duplicate cookie caches to improve performance for
        // the second object.
        duplicateCookieCaches(msg);
    }

    /*
     * @see
     * com.ibm.ws.genericbnf.internal.GenericMessageImpl#readExternal(java.io.
     * ObjectInput)
     */
    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {

        super.readExternal(input);
        // BNFHeaders reading of the headers will trigger all the parsed/temp
        // values at this layer
        try {
            if (SERIALIZATION_V2 == getDeserializationVersion()) {
                setVersion(readByteArray(input));
            } else {
                setVersion((String) input.readObject());
            }
        } catch (UnsupportedProtocolVersionException exc) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unknown HTTP version");
            }
            // malformed version, can't make an "undefined" version
            IOException ioe = new IOException("Failed deserialization of version");
            ioe.initCause(exc);
            throw ioe;
        }
        // V2 uses a boolean, while V1 used a byte... SHOULD be the same, but...
        boolean isTrailer = (SERIALIZATION_V2 == getDeserializationVersion()) ? input.readBoolean() : (1 == input.readByte());
        if (isTrailer) {
            this.myTrailers = (HttpTrailersImpl) input.readObject();
        }
    }

    /*
     * @see
     * com.ibm.ws.genericbnf.internal.GenericMessageImpl#writeExternal(java.io
     * .ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        // convert any temporary Cookies into header storage
        marshallCookieCache(this.cookieCache);
        marshallCookieCache(this.cookie2Cache);
        marshallCookieCache(this.setCookieCache);
        marshallCookieCache(this.setCookie2Cache);

        super.writeExternal(output);
        // BNFHeaders will write out headers, so don't do hdr specifics here
        writeByteArray(output, getVersionValue().getByteArray());
        if (null != this.myTrailers) {
            output.writeBoolean(true);
            if (output instanceof ObjectOutputStream) {
                ((ObjectOutputStream) output).writeUnshared(this.myTrailers);
            } else {
                output.writeObject(this.myTrailers);
            }
        } else {
            // save a marker that the trailers don't exist
            output.writeBoolean(false);
        }
    }

    /**
     * Query whether or not this particular message is incoming.
     *
     * @return boolean
     */
    @Override
    public boolean isIncoming() {
        return this.bIsIncoming;
    }

    /**
     * Set the flag as to whether or not this message is incoming.
     *
     * @param b
     */
    public void setIncoming(boolean b) {
        this.bIsIncoming = b;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Incoming flag now " + b + " on " + this);
        }
    }

    /**
     * Query whether or not this message is considered "committed" by the
     * application channel.
     *
     * @return boolean
     */
    @Override
    public boolean isCommitted() {
        return this.bIsCommitted;
    }

    /**
     * Allow an application channel to set this message as committed which
     * they can then check later in their code with the isCommitted() method.
     * The message cannot be "uncommitted" without being cleared entirely.
     *
     */
    @Override
    public void setCommitted() {
        this.bIsCommitted = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Committed flag set on " + this);
        }
    }

    /**
     * Allow internal channel code to set the committed flag to a specific
     * boolean value.
     *
     * @param b
     */
    protected void setCommitted(boolean b) {
        this.bIsCommitted = b;
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

        // check for chunked encoding header
        if (isChunkedEncodingSet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Msg says chunked encoding: " + this);
            }
            return true;
        }

        // check for content length header
        if (0 < getContentLength()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Msg says content-length: " + getContentLength() + " " + this);
            }
            return true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "No body expected at base layer: " + this);
        }
        return false;
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
        // check for content length header of 0 bytes
        return (0 != getContentLength());
    }

    /**
     * Query whether or not this message should allow the body length headers
     * to be updated when a body is not allowed with the message. For example,
     * a response does not allow a body when the request was a HEAD method but
     * the length delimiters are not updated in that case.
     *
     * @return boolean (whether the caller should modify body headers)
     */
    public boolean shouldUpdateBodyHeaders() {
        return true;
    }

    /**
     * Set the local variable to the input value.
     *
     * @param value
     */
    protected void setMyHSC(HttpServiceContextImpl value) {
        this.myHSC = value;
    }

    /**
     * Query the owning service context.
     *
     * @return HttpServiceContextImpl
     */
    public HttpServiceContextImpl getServiceContext() {
        return this.myHSC;
    }

    /**
     * Query the HTTP object factory.
     *
     * @return HttpObjectFactory
     */
    protected HttpObjectFactory getObjectFactory() {
        if (null == getServiceContext()) {
            return null;
        }
        return getServiceContext().getObjectFactory();
    }

    /**
     * Get access to the trailers object.
     *
     * @return HttpTrailersImpl
     */
    public HttpTrailersImpl getTrailersImpl() {
        return this.myTrailers;
    }

    /**
     * Utility method to create the appropriate trailer headers object.
     *
     * @return HttpTrailersImpl
     */
    @Override
    public HttpTrailersImpl createTrailers() {
        if (null == this.myTrailers) {
            this.myTrailers = (null == getObjectFactory()) ? new HttpTrailersImpl() : getObjectFactory().getTrailers();
            this.myTrailers.init(shouldAllocateDirectBuffer(), getOutgoingBufferSize(), getIncomingBufferSize(), getByteCacheSize());
        }
        return this.myTrailers;
    }

    /**
     * @see HttpBaseMessage#getTrailers()
     */
    @Override
    public HttpTrailers getTrailers() {
        if (null == getTrailersImpl() && containsHeader(HttpHeaderKeys.HDR_TRAILER)) {
            // only create the object if the headers indicate we can send it
            createTrailers();
        }
        return getTrailersImpl();
    }

    // ***********************************************************************
    // Block of methods for the HTTP version of this message
    // ***********************************************************************

    /**
     * Query the value of the HTTP version.
     *
     * @return VersionValues
     */
    @Override
    public VersionValues getVersionValue() {
        return this.myVersion;
    }

    /**
     * Query the value of the HTTP version.
     *
     * @return String
     */
    @Override
    public String getVersion() {
        return (null != this.myVersion) ? this.myVersion.getName() : null;
    }

    /**
     * Set the version of this message.
     *
     * @param version
     * @throws NullPointerException
     *                                  if the input version is null
     */
    @Override
    public void setVersion(VersionValues version) {
        if (!version.equals(this.myVersion)) {
            this.myVersion = version;
            super.setFirstLineChanged();
            if (!isConnectionSet() && getVersionValue().equals(VersionValues.V10)) {
                this.bIsKeepAliveSet = false;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "setVersion(v): set version to " + getVersion());
        }
    }

    /**
     * Set the version of this message.
     *
     * @param version
     * @throws UnsupportedProtocolVersionException
     * @throws NullPointerException
     *                                                 if input version is null
     */
    @Override
    public void setVersion(String version) throws UnsupportedProtocolVersionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Called setVersion(S): " + version);
        }
        try {
            setVersion(VersionValues.find(version));
        } catch (IllegalArgumentException e) {
            // no FFDC required
            throw new UnsupportedProtocolVersionException("Unsupported: " + version);
        }
    }

    /**
     * Set the version of this message.
     *
     * @param version
     * @throws UnsupportedProtocolVersionException
     * @throws NullPointerException
     *                                                 if input version is null
     */
    @Override
    public void setVersion(byte[] version) throws UnsupportedProtocolVersionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Called setVersion(b): " + HttpChannelUtils.getEnglishString(version));
        }
        try {
            setVersion(VersionValues.find(version));
        } catch (IllegalArgumentException e) {
            // no FFDC required
            throw new UnsupportedProtocolVersionException("Unsupported: " + HttpChannelUtils.getEnglishString(version));
        }
    }

    // ************************************************************************
    // Block of methods for the various "special case" HTTP headers
    // ************************************************************************

    /**
     * Set the Date header to the current timestamp.
     */
    @Override
    public void setCurrentDate() {
        setSpecialHeader(HttpHeaderKeys.HDR_DATE, HttpDispatcher.getDateFormatter().getRFC1123TimeAsBytes(getServiceContext().getHttpConfig().getDateHeaderRange()));
    }

    /**
     * Set the Content-Length header to the given number of bytes.
     *
     * @param length
     * @throws IllegalArgumentException
     *                                      if input length is invalid
     */
    @Override
    public void setContentLength(long length) {
        if (0 > length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid Content-Length input [" + Long.toString(length) + "]");
            }
            // PK24115 - throw new IllegalArgumentException("Length: " + length);
            return;
        }

        // if we already match, then just exit
        if (getContentLength() == length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ContentLength already set to " + Long.toString(length));
            }
            return;
        }

        this.myContentLength = length;
        if (HeaderStorage.NOTSET != length) {
            setSpecialHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH, GenericUtils.asByteArray(length));
        } else {
            removeSpecialHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ContentLength set to " + Long.toString(length));
        }
    }

    /**
     * Sets the Content-Length header's integer representation to the input
     * bytes; however, it is not committed to storage.
     *
     * @param value
     * @return boolean (true if success)
     */
    private boolean setContentLength(byte[] value) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setContentLength(b): " + GenericUtils.getEnglishString(value));
        }
        if (null == value) {
            // removing the header
            this.myContentLength = HeaderStorage.NOTSET;
            return true;
        }

        try {
            long length = GenericUtils.asLongValue(value);
            // PK24115 - don't allow -1 to be set explicitly
            if (0 > length) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Invalid Content-Length: " + Long.toString(length));
                }
                return false;
            }
            // check for existing Content-Length header
            if (HeaderStorage.NOTSET != getContentLength()) {
                // if we already match, then just exit
                if (getContentLength() == length) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "setContentLength(b) already: " + Long.toString(length));
                    }
                    return true;
                }
                // PK12319: turn off persistence to avoid any security attacks
                // through malformed length headers
                // PK53193: check the new config option as well
                // 411712 - block possible NPE on duplicate() path
                if (null != this.myHSC && this.myHSC.getHttpConfig().isRequestSmugglingProtectionEnabled()) {
                    this.myHSC.setPersistent(false);
                }

                // otherwise we have different values for multiple instances
                // so don't allow this secondary header instance
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "setContentLength(b) mismatch: " + Long.toString(getContentLength()));
                }
                return false;
            }

            this.myContentLength = length;
        } catch (NumberFormatException nfe) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Format exception in value");
            }
            return false;
        }
        return true;
    }

    /**
     * Query the value of the Content-Length header as a byte number.
     *
     * @return int
     */
    @Override
    public long getContentLength() {
        return this.myContentLength;
    }

    // -----------------------------------------------------------------------
    // Connection header specific methods
    // -----------------------------------------------------------------------

    /**
     * Configure this message for non-persistent behavior. This will remove
     * Keep-Alive if present from the Connection header and add Close if
     * necessary.
     */
    private void setupConnectionClose() {
        if (this.bIsKeepAliveSet) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Replacing Keep-Alive with Close in msg");
            }
            replaceConnection(ConnectionValues.KEEPALIVE, ConnectionValues.CLOSE);
            this.bIsKeepAliveSet = false;
        } else if (!VersionValues.V10.equals(getVersionValue())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding Close to non-persistent msg");
            }
            addConnection(ConnectionValues.CLOSE);
            commitConnection();
        }
        removeSpecialHeader(HttpHeaderKeys.HDR_KEEP_ALIVE);
        this.bIsCloseSet = true;
    }

    /**
     * Utility method to search the list of Connection header values for one
     * that needs to be replaced by a new value. If the old one does not exist,
     * then this simply adds the new one on the end of any existing values.
     *
     * @param target
     * @param item
     */
    private void replaceConnection(ConnectionValues target, ConnectionValues item) {

        for (int i = 0; i < this.indexConnection; i++) {
            if (this.myConnectionVals[i].equals(target)) {
                // found the target, overwrite it
                this.myConnectionVals[i] = item;
                commitConnection();
                return;
            } else if (this.myConnectionVals[i].equals(item)) {
                // item already present
                return;
            }
        }
        // didn't find the target to replace, just append the new one
        addConnection(item);
        commitConnection();
    }

    /**
     * Add another item onto the private list of Connection header values. This
     * will grow the array if necessary.
     *
     * @param item
     */
    private void addConnection(ConnectionValues item) {

        if (null == item || ConnectionValues.NOTSET.equals(item)) {
            // do nothing in this scenario
            return;
        }

        // check if we need to increase the array size
        if (this.indexConnection == this.myConnectionVals.length) {
            ConnectionValues[] old = this.myConnectionVals;
            this.myConnectionVals = new ConnectionValues[old.length + GROWTH_SIZE];
            System.arraycopy(old, 0, this.myConnectionVals, 0, old.length);
        }
        // check if the new value affects the "keep-alive present" flag
        if (item.equals(ConnectionValues.KEEPALIVE)) {
            this.bIsKeepAliveSet = true;
            this.bIsCloseSet = false;
        } else if (item.equals(ConnectionValues.CLOSE)) {
            this.bIsKeepAliveSet = false;
            this.bIsCloseSet = true;
        }
        this.myConnectionVals[this.indexConnection] = item;
        this.indexConnection++;
    }

    /**
     * Set the Connection header to a specific constant (i.e. CLOSE).
     * If you want more specific information (token=field) then you
     * must use the setHeader() API with some character representation.
     *
     * @param value
     */
    @Override
    public void setConnection(ConnectionValues value) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setConnection(v): " + value);
        }
        // reset the local variables
        removeLocalConnection();
        // save the new
        addConnection(value);
        commitConnection();
    }

    /**
     * Set the Connection header to the input list of defined values.
     * These will be set in the order of the array.
     *
     * @param values
     */
    @Override
    public void setConnection(ConnectionValues[] values) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setConnection(v[]): " + values);
        }
        // reset the local variables
        removeLocalConnection();

        if (null != values) {
            for (int i = 0; i < values.length; i++) {
                addConnection(values[i]);
            }
        }
        commitConnection();
    }

    /**
     * Method to remove a single value from the Connection header. If
     * that was the only existing value, then the entire header is removed.
     * Caller must call commitConnection() when they are done modifying values.
     *
     * @param value
     */
    void removeConnection(ConnectionValues value) {
        for (int i = 0; i < this.indexConnection; i++) {
            if (value.equals(this.myConnectionVals[i])) {
                // move everybody else down one
                for (int x = i + 1; x < this.indexConnection; x++, i++) {
                    this.myConnectionVals[i] = this.myConnectionVals[x];
                }
                // last one becomes null
                this.myConnectionVals[i] = null;
                this.indexConnection--;

                if (ConnectionValues.CLOSE.equals(value)) {
                    this.bIsCloseSet = false;
                } else if (ConnectionValues.KEEPALIVE.equals(value)) {
                    // default based on version
                    this.bIsKeepAliveSet = VersionValues.V11.equals(this.myVersion);
                }
                return;
            }
        }
    }

    /**
     * Configures the local storage to indicate an empty Connection header.
     */
    private void removeLocalConnection() {

        // if we're 1.1 then default keep-alive to true, otherwise it's false
        this.bIsKeepAliveSet = VersionValues.V11.equals(this.myVersion);
        this.bIsCloseSet = false;
        for (int i = 0; i < this.indexConnection; i++) {
            this.myConnectionVals[i] = null;
        }
        this.indexConnection = 0;
    }

    /**
     * Commit the local changes to the Connection header to storage.
     *
     */
    private void commitConnection() {

        if (0 == this.indexConnection) {
            // no values stored, delete the header if it exists
            removeSpecialHeader(HttpHeaderKeys.HDR_CONNECTION);
        } else if (1 == this.indexConnection) {
            // save the single item directly
            setSpecialHeader(HttpHeaderKeys.HDR_CONNECTION, this.myConnectionVals[0].getName());
        } else {

            // walk through the list of values and create the header
            // string to save in BNFHeaders
            StringBuilder buff = new StringBuilder(this.myConnectionVals[0].getName());
            for (int i = 1; i < this.indexConnection; i++) {
                buff.append(HDR_LIST_SEPARATOR);
                buff.append(this.myConnectionVals[i].getName());
            }
            setSpecialHeader(HttpHeaderKeys.HDR_CONNECTION, buff.toString());
        }
    }

    /**
     * When an instance of the Connection header is removed, this method
     * is used to remove the information stored at the base message layer.
     * Users calling this method should be aware that the removal is not
     * at the header storage layer, but only at the base message layer where
     * the data is converted.
     *
     * @param data
     */
    public void removeConnectionHeader(byte[] data) {
        if (null == data) {
            removeLocalConnection();
            return;
        }
        TokenDelimiter td = this.myTokenDelimiter.clear();
        while (td.next < data.length && findToken(data, td)) {
            removeConnection(ConnectionValues.find(data, td.start, td.length));
        }
    }

    /**
     * This method will find the next token in the input byte[] data.
     *
     * @param data
     * @param td
     * @return boolean
     */
    private boolean findToken(byte[] data, TokenDelimiter td) {
        int start = (HeaderStorage.NOTSET == td.next) ? 0 : td.next;
        // check boundaries
        if (start >= data.length) {
            return false;
        }
        // skip leading whitespace if necessary
        while (BNFHeaders.SPACE == data[start]) {
            start++;
            if (start >= data.length) {
                return false;
            }
            if (HttpBaseMessage.COMMA == data[start]) {
                // we had an empty token ",  ,"
                start++;
                if (start >= data.length) {
                    return false;
                }
            }
        }

        // find end of token (end or comma).
        int end = start + 1;
        int next = end;
        for (; end < data.length && HttpBaseMessage.COMMA != data[end]; end++, next++) {

            if (BNFHeaders.SPACE == data[end]) {
                // skip any space at the end of the data, exit on next
                // non-space char
                next = GenericUtils.skipWhiteSpace(data, end);
                if (next >= data.length) {
                    break;
                }
                if (HttpBaseMessage.COMMA == data[next]) {
                    break;
                }
                end = next;
            }
        }
        td.start = start;
        td.next = next + 1;
        td.length = end - start;
        return true;
    }

    /**
     * Take the input data and parse out all possible "tokens" from it. This
     * ignores leading and trailing spaces around each "token word".
     *
     * @param data
     */
    private void matchAndParseConnection(byte[] data) {
        if (null == data || 0 == data.length) {
            return;
        }
        TokenDelimiter td = this.myTokenDelimiter.clear();
        while (td.next < data.length && findToken(data, td)) {
            addConnection(ConnectionValues.find(data, td.start, td.length));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsed out " + this.indexConnection + " Connection header values");
        }
    }

    /**
     * Query the Connection header and receive an array representing
     * the types found in the header. If CONN_UNDEF is returned, check
     * the getName() method to find out what the "undefined"
     * value was exactly. This array is returned in the order of the
     * tokens found in the header, thus "Connection: TE, Keep-Alive"
     * would return { CONN_TE, CONN_KEEPALIVE }. A CONN_NOTSET means
     * that the header is not present.
     *
     * @return ConnectionValues[]
     */
    @Override
    public ConnectionValues[] getConnection() {

        if (0 == this.indexConnection) {
            return new ConnectionValues[] { ConnectionValues.NOTSET };
        }
        ConnectionValues[] list = new ConnectionValues[this.indexConnection];
        System.arraycopy(this.myConnectionVals, 0, list, 0, list.length);
        return list;
    }

    /**
     * Quick method to check whether the Connection header contains
     * the "Keep-Alive" token currently.
     *
     * @return boolean
     */
    @Override
    public boolean isKeepAliveSet() {
        return this.bIsKeepAliveSet;
    }

    /**
     * Query whether the Connection header has Close in it.
     *
     * @return boolean
     */
    public boolean isCloseSet() {
        return this.bIsCloseSet;
    }

    /**
     * Quick method to check whether the Connection header has been
     * set to any value yet or not.
     *
     * @return boolean
     */
    @Override
    public boolean isConnectionSet() {
        return (0 < this.indexConnection);
    }

    // -----------------------------------------------------------------------
    // Content-Encoding header specific methods
    // -----------------------------------------------------------------------

    /**
     * Query what the outermost encoding is currently set to for this message's
     * Content-Encoding header value. This will return null if there is no
     * header present.
     *
     * @return ContentEncodingValues
     */
    public ContentEncodingValues getOutermostEncoding() {
        if (0 == this.indexContentEncoding) {
            // nothing is set
            return null;
        }
        return this.myContentEncodingVals[this.indexContentEncoding - 1];
    }

    /**
     * Utility method to remove only the outermost encoding.
     *
     */
    public void removeOutermostEncoding() {
        if (0 == this.indexContentEncoding) {
            // nothing is present
            return;
        }
        this.indexContentEncoding--;
        ContentEncodingValues val = this.myContentEncodingVals[this.indexContentEncoding];
        this.myContentEncodingVals[this.indexContentEncoding] = null;
        commitContentEncoding();

        // create the $WSZIP header
        if (ContentEncodingValues.GZIP.equals(val)) {
            setSpecialHeader(HttpHeaderKeys.HDR_$WSZIP, val.getName());
        } else if (ContentEncodingValues.XGZIP.equals(val)) {
            setSpecialHeader(HttpHeaderKeys.HDR_$WSZIP, val.getName());
        } else if (ContentEncodingValues.DEFLATE.equals(val)) {
            setSpecialHeader(HttpHeaderKeys.HDR_$WSZIP, val.getName());
        }
    }

    /**
     * Query whether or not this message contains an encoded body.
     *
     * @return boolean
     */
    public boolean isBodyEncoded() {
        return (0 < this.indexContentEncoding);
    }

    /**
     * Query whether the outermost encoding (if any) is the Deflate encoding.
     *
     * @return boolean
     */
    public boolean isDeflateOutermostEncoding() {
        return ContentEncodingValues.DEFLATE.equals(getOutermostEncoding());
    }

    /**
     * Query whether the outermost encoding (if any) is the GZip encoding.
     *
     * @return boolean
     */
    public boolean isGZipOutermostEncoding() {
        return ContentEncodingValues.GZIP.equals(getOutermostEncoding());
    }

    /**
     * Query whether the outermost encoding (if any) is the X-GZip encoding.
     *
     * @return boolean
     */
    public boolean isXGZipOutermostEncoding() {
        return ContentEncodingValues.XGZIP.equals(getOutermostEncoding());
    }

    /**
     * Add a particular content encoding to the stored list.
     *
     * @param value
     */
    private void addContentEncoding(ContentEncodingValues value) {
        if (null == value || ContentEncodingValues.NOTSET.equals(value)) {
            // do nothing
            return;
        }
        // check if we need to increase the array size
        if (this.indexContentEncoding == this.myContentEncodingVals.length) {
            ContentEncodingValues[] old = this.myContentEncodingVals;
            this.myContentEncodingVals = new ContentEncodingValues[old.length + GROWTH_SIZE];
            System.arraycopy(old, 0, this.myContentEncodingVals, 0, old.length);
        }
        this.myContentEncodingVals[this.indexContentEncoding] = value;
        this.indexContentEncoding++;
    }

    /**
     * Remove a particular content encoding value from the list.
     *
     * @param value
     */
    private void removeContentEncoding(ContentEncodingValues value) {
        for (int i = 0; i < this.indexContentEncoding; i++) {
            if (value.equals(this.myContentEncodingVals[i])) {
                // move everybody else down one
                for (int x = i + 1; x < this.indexContentEncoding; x++, i++) {
                    this.myContentEncodingVals[i] = this.myContentEncodingVals[x];
                }
                // last one becomes null
                this.myContentEncodingVals[i] = null;
                this.indexContentEncoding--;
                return;
            }
        }
    }

    /**
     * Add the input value to the end of any existing header value.
     *
     * @param value
     */
    protected void appendContentEncoding(ContentEncodingValues value) {
        addContentEncoding(value);
        commitContentEncoding();
    }

    /**
     * Set the Content-Encoding header to the given encoding identifier.
     *
     * @param value
     */
    @Override
    public void setContentEncoding(ContentEncodingValues value) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setContentEncoding(v): " + value);
        }
        // reset the local information
        removeLocalContentEncoding();
        // save the new
        addContentEncoding(value);
        commitContentEncoding();
    }

    /**
     * Set the Content-Encoding header to the given list of values,
     * in the same order as the array.
     *
     * @param values
     */
    @Override
    public void setContentEncoding(ContentEncodingValues[] values) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setContentEncoding(v[]): " + values);
        }
        // remove whatever is currently stored
        removeLocalContentEncoding();

        if (null != values) {
            for (int i = 0; i < values.length; i++) {
                addContentEncoding(values[i]);
            }
        }
        commitContentEncoding();
    }

    /**
     * Commit the local changes to the Content-Encoding header to storage.
     *
     */
    private void commitContentEncoding() {

        if (0 == this.indexContentEncoding) {
            // no values stored, delete the header if it exists
            removeSpecialHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING);
        } else if (1 == this.indexContentEncoding) {
            // save the single value
            setSpecialHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, this.myContentEncodingVals[0].getName());
        } else {

            // walk through the list of values and create the header
            // string to save in BNFHeaders
            StringBuilder buff = new StringBuilder(this.myContentEncodingVals[0].getName());
            for (int i = 1; i < this.indexContentEncoding; i++) {
                buff.append(HDR_LIST_SEPARATOR);
                buff.append(this.myContentEncodingVals[i].getName());
            }
            setSpecialHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING, buff.toString());
        }
    }

    /**
     * When an instance of the Content-Encoding header is removed, this method
     * is used to remove the information stored at the base message layer.
     * Users calling this method should be aware that the removal is not
     * at the header storage layer, but only at the base message layer where
     * the data is converted.
     *
     * @param data
     */
    public void removeContentEncodingHeader(byte[] data) {
        if (null == data) {
            removeLocalContentEncoding();
            return;
        }
        TokenDelimiter td = this.myTokenDelimiter.clear();
        while (td.next < data.length && findToken(data, td)) {
            removeContentEncoding(ContentEncodingValues.find(data, td.start, td.length));
        }
    }

    /**
     * Reset the Content-Encoding information.
     *
     */
    private void removeLocalContentEncoding() {
        for (int i = 0; i < this.indexContentEncoding; i++) {
            this.myContentEncodingVals[i] = null;
        }
        this.indexContentEncoding = 0;
    }

    /**
     * Take the input data and parse out all possible "tokens" from it. This
     * ignores leading and trailing spaces around each "token word".
     *
     * @param data
     */
    private void matchAndParseContent(byte[] data) {
        if (null == data || 0 == data.length) {
            return;
        }
        TokenDelimiter td = this.myTokenDelimiter.clear();
        while (td.next < data.length && findToken(data, td)) {
            addContentEncoding(ContentEncodingValues.find(data, td.start, td.length));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsed out " + this.indexContentEncoding + " Content-Encoding header values");
        }
    }

    /**
     * Query the value of the Content-Encoding header. This will
     * be returned as an array of values that were found, or
     * CONTENT_ENCODING_NOTSET if the header is not present. If
     * the value found does not match one of the existing values,
     * a CONTENT_ENCODING_UNDEF value will be returned with the
     * actual data being reported through the getName() method.
     * The array is given in the order in which values were
     * found in the header.
     *
     * @return ContentEncodingValues[]
     */
    @Override
    public ContentEncodingValues[] getContentEncoding() {
        if (0 == this.indexContentEncoding) {
            // nothing on the list so return NOTSET
            return new ContentEncodingValues[] { ContentEncodingValues.NOTSET };
        }

        ContentEncodingValues[] list = new ContentEncodingValues[this.indexContentEncoding];
        System.arraycopy(this.myContentEncodingVals, 0, list, 0, list.length);
        return list;
    }

    // ----------------------------------------------------------------------
    // Transfer-Encoding header specific methods
    // ----------------------------------------------------------------------

    /**
     * Add another item onto the private list of TransferEncoding header values.
     * This will grow the array if necessary.
     *
     * @param item
     */
    private void addTransferEncoding(TransferEncodingValues item) {
        if (null == item || TransferEncodingValues.NOTSET.equals(item)) {
            // do nothing
            return;
        }
        // check if we need to increase the array size
        if (this.indexTransfer == this.myTransferVals.length) {
            TransferEncodingValues[] old = this.myTransferVals;
            this.myTransferVals = new TransferEncodingValues[old.length + GROWTH_SIZE];
            System.arraycopy(old, 0, this.myTransferVals, 0, old.length);
        }
        // if chunked-encoding flag is not already set, then check this input
        if (!isChunkedEncodingSet()) {
            this.bIsChunkedEncodingSet = item.equals(TransferEncodingValues.CHUNKED);
        }
        this.myTransferVals[this.indexTransfer] = item;
        this.indexTransfer++;
    }

    /**
     * Append the input Transfer-Encoding item to the header and commit it to
     * storage.
     *
     * @param item
     */
    protected void appendTransferEncoding(TransferEncodingValues item) {
        addTransferEncoding(item);
        commitTransferEncoding();
    }

    /**
     * Set the Transfer-Encoding header to one of the encoding
     * identifiers.
     *
     * @param value
     */
    @Override
    public void setTransferEncoding(TransferEncodingValues value) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setTransferEncoding(v): " + value);
        }
        // reset the local variables
        removeLocalTransferEncoding();
        // save the new
        addTransferEncoding(value);
        commitTransferEncoding();
    }

    /**
     * Set the Transfer-Encoding header to the input list of values,
     * in the same order as the array.
     *
     * @param values
     */
    @Override
    public void setTransferEncoding(TransferEncodingValues[] values) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setTransferEncoding(v[]): " + values);
        }
        // reset the local variables
        removeLocalTransferEncoding();

        if (null != values) {
            for (int i = 0; i < values.length; i++) {
                addTransferEncoding(values[i]);
            }
        }
        commitTransferEncoding();
    }

    /**
     * Commit the local changes to the Transfer-Encoding header to storage.
     *
     */
    protected void commitTransferEncoding() {

        if (0 == this.indexTransfer) {
            // no values stored, delete the header if it exists
            removeSpecialHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING);
        } else if (1 == this.indexTransfer) {
            // save the single value
            setSpecialHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, this.myTransferVals[0].getName());
        } else {

            // walk through the list of values and create the header
            // string to save in BNFHeaders
            StringBuilder buff = new StringBuilder(this.myTransferVals[0].getName());
            for (int i = 1; i < this.indexTransfer; i++) {
                buff.append(HDR_LIST_SEPARATOR);
                buff.append(this.myTransferVals[i].getName());
            }
            setSpecialHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, buff.toString());
        }
    }

    /**
     * Method to remove a single value from the Transfer-Encoding header. If
     * that was the only existing value, then the entire header is removed.
     *
     * @param value
     */
    protected void removeTransferEncoding(TransferEncodingValues value) {
        for (int i = 0; i < this.indexTransfer; i++) {
            if (value.equals(this.myTransferVals[i])) {
                // move everybody else down one
                for (int x = i + 1; x < this.indexTransfer; x++, i++) {
                    this.myTransferVals[i] = this.myTransferVals[x];
                }
                // last one becomes null
                this.myTransferVals[i] = null;
                this.indexTransfer--;

                if (TransferEncodingValues.CHUNKED.equals(value)) {
                    this.bIsChunkedEncodingSet = false;
                }
                return;
            }
        }
    }

    /**
     * Configure the local storage to indicate an empty Transfer-Encoding
     * header.
     */
    private void removeLocalTransferEncoding() {

        this.bIsChunkedEncodingSet = false;
        for (int i = 0; i < this.indexTransfer; i++) {
            this.myTransferVals[i] = null;
        }
        this.indexTransfer = 0;
    }

    /**
     * When an instance of the Transfer-Encoding header is removed, this method
     * is used to remove the information stored at the base message layer.
     * Users calling this method should be aware that the removal is not
     * at the header storage layer, but only at the base message layer where
     * the data is converted.
     *
     * @param data
     */
    public void removeTransferEncodingHeader(byte[] data) {
        if (null == data) {
            removeLocalTransferEncoding();
            return;
        }
        TokenDelimiter td = this.myTokenDelimiter.clear();
        while (td.next < data.length && findToken(data, td)) {
            removeTransferEncoding(TransferEncodingValues.find(data, td.start, td.length));
        }
    }

    /**
     * Take the input data and parse out all possible "tokens" from it. This
     * ignores leading and trailing spaces around each "token word".
     *
     * @param data
     */
    private void matchAndParseTransfer(byte[] data) {
        if (null == data || 0 == data.length) {
            return;
        }
        TokenDelimiter td = this.myTokenDelimiter.clear();
        while (td.next < data.length && findToken(data, td)) {
            addTransferEncoding(TransferEncodingValues.find(data, td.start, td.length));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsed out " + this.indexTransfer + " Transfer-Encoding header values");
        }
    }

    /**
     * Query the value of the Transfer-Encoding header. This will
     * be returned as an array of values that were found, or
     * TRANSFER_ENCODING_NOTSET if the header is not present. If
     * the value found does not match one of the existing values,
     * a TRANSFER_ENCODING_UNDEF value will be returned with the
     * actual data being reported through the getName() method.
     * The array is returned in the same order as the values found
     * in the header.
     *
     * @return TransferEncodingValues[]
     */
    @Override
    public TransferEncodingValues[] getTransferEncoding() {

        if (0 == this.indexTransfer) {
            return new TransferEncodingValues[] { TransferEncodingValues.NOTSET };
        }
        TransferEncodingValues[] list = new TransferEncodingValues[this.indexTransfer];
        System.arraycopy(this.myTransferVals, 0, list, 0, list.length);
        return list;
    }

    /**
     * Quick method to check whether the Transfer-Encoding header
     * contains the "chunked" token currently.
     *
     * @return boolean
     */
    @Override
    public boolean isChunkedEncodingSet() {
        return this.bIsChunkedEncodingSet;
    }

    // ----------------------------------------------------------------------
    // Expect header specific methods
    // ----------------------------------------------------------------------

    /**
     * When we've received an expect header value, either from parsing or from
     * the user, this method encapsulates any special logic we need to perform.
     *
     * @param value
     */
    private void addExpect(ExpectValues value) {
        if (ExpectValues.CONTINUE.equals(value)) {
            this.bIs100Continue = true;
        }
    }

    /**
     * Take the input data and parse out all possible "tokens" from it. This
     * ignores leading and trailing spaces around each "token word".
     *
     * @param data
     */
    private void matchAndParseExpect(byte[] data) {
        if (null == data || 0 == data.length) {
            return;
        }
        TokenDelimiter td = this.myTokenDelimiter.clear();
        while (td.next < data.length && findToken(data, td)) {
            addExpect(ExpectValues.find(data, td.start, td.length));
        }
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.HttpBaseMessage#setExpect(com.ibm.wsspi.http
     * .channel.values.ExpectValues)
     */
    @Override
    public void setExpect(ExpectValues value) {

        // reset the local value
        this.bIs100Continue = false;

        if (null == value) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Removing Expect(v) header");
            }
            removeSpecialHeader(HttpHeaderKeys.HDR_EXPECT);
        } else {
            addExpect(value);
            setSpecialHeader(HttpHeaderKeys.HDR_EXPECT, value.getByteArray());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setExpect(v): 100-continue is " + isExpect100Continue());
            }
        }
    }

    /**
     * When an instance of the Expect header is removed, this method
     * is used to remove the information stored at the base message layer.
     * Users calling this method should be aware that the removal is not
     * at the header storage layer, but only at the base message layer where
     * the data is converted.
     *
     * @param data
     */
    public void removeExpectHeader(byte[] data) {
        if (null == data) {
            this.bIs100Continue = false;
            return;
        }
        TokenDelimiter td = this.myTokenDelimiter.clear();
        while (td.next < data.length && findToken(data, td)) {
            if (ExpectValues.CONTINUE.equals(ExpectValues.find(data, td.start, td.length))) {
                this.bIs100Continue = false;
            }
        }
    }

    /**
     * Query the current value of the HTTP Expect header.
     *
     * @return byte[]
     */
    @Override
    public byte[] getExpect() {
        int size = getNumberOfHeaderInstances(HttpHeaderKeys.HDR_EXPECT);
        if (0 == size) {
            return null;
        }
        if (1 == size) {
            return getHeader(HttpHeaderKeys.HDR_EXPECT).asBytes();
        }

        // multiple existing values
        Iterator<HeaderField> it = getHeaders(HttpHeaderKeys.HDR_EXPECT).iterator();
        StringBuilder buffer = new StringBuilder(it.next().asString());
        while (it.hasNext()) {
            buffer.append(HDR_LIST_SEPARATOR);
            buffer.append(it.next().asString());
        }
        return GenericUtils.getBytes(buffer);
    }

    /**
     * Query whether or not the current HTTP Expect header on the message
     * contains the sequence "100-continue".
     *
     * @return boolean (true if present)
     */
    @Override
    public boolean isExpect100Continue() {
        return this.bIs100Continue;
    }

    /**
     * Query the MIME type of the HTTP body (e.g. text/html). This will return
     * null if the type is not known.
     *
     * @return String
     */
    @Override
    public String getMIMEType() {
        return parseMIMEType();
    }

    /**
     * Parses MIME type from a <code>Content-Type</code> header.
     *
     * @return Returns the MIME type of the flow, or null if not known.
     */
    private String parseMIMEType() {

        // Obtain Content-Type header as bytes for performance
        byte[] contentTypeBytes = getHeader(HttpHeaderKeys.HDR_CONTENT_TYPE).asBytes();

        if (null == contentTypeBytes) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "parseMIMEType: no Content-Type header present");
            }
            return null;
        }
        // Return everything prior to the first semicolon (if any).
        int length = contentTypeBytes.length;
        int end = -1;
        for (int i = 0; i < length; i++) {
            // Semi-colon exists, <type>;<charset>
            if (BNFHeaders.SEMICOLON == contentTypeBytes[i]) {
                end = i;
                break;
            }
        }
        // check to see if the semi-colon was not found (just <type>)
        if (-1 == end) {
            end = length;
        }
        return HttpChannelUtils.getEnglishString(contentTypeBytes, 0, end);
    }

    /**
     * Set the MIME type of the HTTP body to the input string.
     *
     * @param type
     * @throws NullPointerException
     *                                  if input string is null
     */
    @Override
    public void setMIMEType(String type) {

        int index = type.indexOf(";");
        StringBuilder buff = new StringBuilder();
        if (-1 != index) {
            buff.append(type.substring(0, index));
        } else {
            buff.append(type);
        }
        buff.append(";charset=");
        buff.append(parseCharset());
        setSpecialHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, buff.toString());
    }

    /**
     * Returns a named mapping between sequences of sixteen-bit Unicode
     * characters and sequences of bytes in the body of the flow. If this is
     * not explicitly set, then it will return the mapping for ISO-8859-1.
     *
     * @return Charset
     */
    @Override
    public Charset getCharset() {
        return parseCharset();
    }

    /**
     * Parses a named mapping between sequences of sixteen-bit Unicode
     * characters and sequences of bytes in the body of the flow from a <code>Content-Type</code> header.
     *
     * @return Returns the appropriate mapping for the flow, or a mapping for <code>ISO-8859-1</code> (Latin-1) if the flow does not specify
     *         a character encoding.
     */
    private Charset parseCharset() {

        String encoding = parseCharacterEncoding(getHeader(HttpHeaderKeys.HDR_CONTENT_TYPE).asString());
        if (null != encoding) {
            try {
                return AccessController.doPrivileged(new privCharsetLookup(encoding));
            } catch (UnsupportedCharsetException exc) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unknown charset name: " + encoding);
                }
                // continue below and return the default
            }
        }
        if (null == DEF_CHARSET) {
            // lazily instantiate the default charset if need be
            DEF_CHARSET = StandardCharsets.ISO_8859_1;
        }
        return DEF_CHARSET;
    }

    /**
     * Parse the character encoding from the specified content type header.
     * If the content type is null, or there is no explicit character encoding, <code>null</code> is returned.
     *
     * @param contentType
     *                        a content type header.
     * @return Returns the character encoding for this flow, or null if the given
     *         content-type header is null or if no character enoding is present
     *         in the content-type header.
     */
    private String parseCharacterEncoding(String contentType) {

        if (null == contentType) {
            return null;
        }

        int start = contentType.indexOf("charset=");
        if (-1 == start) {
            return null;
        }
        start += "charset=".length();
        boolean bQuoted = (QUOTE == contentType.charAt(start));
        if (bQuoted) {
            start++;
        }
        int end = contentType.indexOf(BNFHeaders.SEMICOLON, start);
        if (-1 == end) {
            end = contentType.length();
        }

        if (bQuoted && QUOTE == contentType.charAt(end - 1)) {
            end--;
        }
        return contentType.substring(start, end).trim();
    }

    /**
     * Sets the named mapping between sequences of sixteen-bit Unicode
     * characters and sequences of bytes in the body of the flow.
     *
     * @param set
     * @throws NullPointerException
     *                                  if the input Charset is null
     */
    @Override
    public void setCharset(Charset set) {
        if (null == set) {
            throw new NullPointerException("charset");
        }
        StringBuilder buff = new StringBuilder();
        String mime = parseMIMEType();
        if (null == mime) {
            mime = "text/html";
        }
        buff.append(mime);
        buff.append(";charset=");
        buff.append(set.toString());
        setSpecialHeader(HttpHeaderKeys.HDR_CONTENT_TYPE, buff.toString());
    }

    // ***********************************************************************
    // Block of methods for parsing and marshalling of an HTTP message
    // ***********************************************************************

    /**
     * Before marshalling headers into a buffer, this will run the data
     * through a compliancy check and take appropriate action (throw
     * errors, add missing headers, etc).
     *
     * @throws MessageSentException
     */
    @Override
    protected void headerComplianceCheck() throws MessageSentException {

        final HttpServiceContextImpl sc = getServiceContext();
        if (sc.headersSent()) {
            throw new MessageSentException("Headers already sent.");
        }

        try {
            /*
             * *
             * Http/1.1 Headers *
             */
            if (getVersionValue().equals(VersionValues.V11)) {
                // service context should be setting Content-Length if possible,
                // so if that is empty, then configure for chunked encoding
                if (NOTSET == getContentLength() && !isChunkedEncodingSet()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Partial bodied 1.1 output");
                    }
                    if (sc.isOutgoingBodyValid()) {
                        addTransferEncoding(TransferEncodingValues.CHUNKED);
                        commitTransferEncoding();
                    }
                }
                // determine what state the connection header should be in
                // persistent current header change required
                // false close no change
                // false keep replace with close
                // false nothing add close
                // true close no change (leave closing)
                // true keep no change
                // true nothing no change
                if (!sc.isPersistent() && !isCloseSet()) {
                    setupConnectionClose();
                }
            }
            /*
             * *
             * Http/1.0 Headers *
             */
            else if (getVersionValue().equals(VersionValues.V10)) {
                // Remove incorrect Transfer Encoding header.
                if (isChunkedEncodingSet()) {
                    removeSpecialHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING);
                    removeLocalTransferEncoding();
                    // if we got here then we need to be closing the connection
                    // after sending the now non-delimited body
                    sc.setPersistent(false);
                }
                // determine what state the connection header should be in
                // persistent current header change required
                // false close no change
                // false keep replace with close
                // false anything else no change (default is close)
                // true close no change (leave closing)
                // true keep no change
                // true anything else add keep-alive

                // service context should be setting the Content-Length where
                // possible, so if that is missing, configure for closure
                if (!sc.isPersistent() || NOTSET == getContentLength()) {
                    if (!isCloseSet()) {
                        setupConnectionClose();
                    }
                    sc.setPersistent(false);
                } else if (sc.isPersistent()) {
                    // add Keep-Alive if we need to
                    if (!isKeepAliveSet() && !isCloseSet()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Adding Keep-Alive to persistent 1.0 msg");
                        }
                        addConnection(ConnectionValues.KEEPALIVE);
                        commitConnection();
                    }
                }
            }

            else if (getVersionValue().equals(VersionValues.V20)) {
                //If chunked-encoding is set, remove this header
                if (isChunkedEncodingSet()) {
                    removeTransferEncoding(TransferEncodingValues.CHUNKED);
                }
            }

            /*
             * *
             * Common Headers *
             */

            // always remove the special $WSZIP header
            removeSpecialHeader(HttpHeaderKeys.HDR_$WSZIP);
            // PK17960 - remove this autodecompression related hdr too
            removeSpecialHeader(HttpHeaderKeys.HDR_$WSORIGCL);

        } catch (IllegalArgumentException iae) {
            FFDCFilter.processException(iae, getClass().getName() + ".headerComplianceCheck", "1996", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Too many headers in message for compliance to finish");
            }
        } finally {
            // now that we have all changes finished, update the statemap if
            // we are on z/OS
            if (sc.getHttpConfig().isServantRegion()) {
                // save whether the connection is currently persistent or not
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Running on z/OS SR, persistence: " + sc.isPersistent());
                }
                sc.getVC().getStateMap().put(HttpConstants.SESSION_PERSISTENCE, (sc.isPersistent()) ? "true" : "false");
                sc.setPersistent(false);
            }
        }
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.GenericMessageImpl#parsingComplete()
     */
    @SuppressWarnings("unused")
    @Override
    protected void parsingComplete() throws MalformedMessageException {
        // PK20069: let subclasses handle the logic
    }

    /**
     * Called for marshalling the first line of binary HTTP responses.
     *
     * @return WsByteBuffer[]
     */
    public abstract WsByteBuffer[] marshallBinaryFirstLine();

    /**
     * Marshall the binary HTTP message into WsByteBuffers to be sent out.
     *
     * @return WsByteBuffer[]
     * @throws MessageSentException
     */
    public WsByteBuffer[] marshallBinaryMessage() throws MessageSentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "marshallBinaryMessage");
        }

        preMarshallMessage();
        WsByteBuffer[] marshalledObj = marshallBinaryFirstLine();
        headerComplianceCheck();
        marshalledObj = marshallBinaryHeaders(marshalledObj);
        postMarshallMessage();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "marshallBinaryMessage");
        }
        return marshalledObj;
    }

    /*
     * @see
     * com.ibm.ws.genericbnf.internal.GenericMessageImpl#parseMessage(com.ibm.
     * wsspi.bytebuffer.WsByteBuffer, boolean)
     */
    @Override
    public boolean parseMessage(WsByteBuffer buffer, boolean bExtract) throws Exception {

        this.myHSC.checkIncomingMessageLimit(buffer.remaining());
        boolean rc;
        //Check if it is an H2 Message we are parsing
        if (this.myHSC.isH2Connection()) {
            rc = decodeH2Message(buffer);
        } else {
            rc = this.myHSC.getHttpConfig().isBinaryTransportEnabled() ? parseBinaryMessage(buffer) : super.parseMessage(buffer, bExtract);
        }
        if (rc) {
            // end of headers found, buffer position is set to where they
            // finished, so remove whatever body data might be in this buffer
            // but after the headers from the "size so far" counter
            long excess = buffer.remaining();
            if (0 < excess) {
                this.myHSC.addToIncomingMsgSize(-excess);
            }
        }
        return rc;
    }

    public WsByteBuffer[] encodeH2Message() throws MessageSentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "encodeH2Message");
        }

        preMarshallMessage();

        //Start of by encoding the first line

        WsByteBuffer[] encodedMessage = encodePseudoHeaders();

        //If encodeHeaders returns null buffers, there was an error during the encoding
        //of these headers. Set the dynamic table context to invalid and get ready to
        //close down.
        if (encodedMessage == null) {
            getH2HeaderTable().setDynamicTableValidity(false);
            return null;
        }

        //Remove non required HTTP/2.0 headers?
        headerComplianceCheck();

        //Encode all defined headers
        encodedMessage = encodeHeaders(getH2HeaderTable(), encodedMessage, getServiceContext().isPushPromise());

        //If encodeHeaders returns null buffers, there was an error during the encoding
        //of these headers. Set the dynamic table context to invalid and get ready to
        //close down.
        if (encodedMessage == null) {
            getH2HeaderTable().setDynamicTableValidity(false);
            return null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "encodeH2Message");
        }
        return encodedMessage;
    }

    public boolean decodeH2Message(WsByteBuffer buffer) throws Exception {

        //Set HTTP version on the Message
        setVersion(VersionValues.V20.getName());

        //PI34161 - Record the start of the request at the time of parsing
        if ((this instanceof HttpRequestMessageImpl) && ((HttpRequestMessageImpl) this).getServiceContext().getHttpConfig().isAccessLoggingEnabled()) {
            this.startTime = System.nanoTime();
        }

        //Decode headers until we reach the end of the buffer
        HttpInboundLink wrapper = null;

        if (this.getServiceContext() instanceof HttpInboundServiceContextImpl) {
            wrapper = ((HttpInboundServiceContextImpl) this.getServiceContext()).getLink();

            HashMap<String, String> pseudoHeaders = null;
            ArrayList<H2HeaderField> headers = null;

            if (wrapper != null && wrapper instanceof H2HttpInboundLinkWrap) {
                pseudoHeaders = ((H2HttpInboundLinkWrap) wrapper).getReadPseudoHeaders();
                headers = ((H2HttpInboundLinkWrap) wrapper).getReadHeaders();
            }

            if (pseudoHeaders != null) {

                setH2FirstLineComplete(pseudoHeaders);

            }
            if (isFirstLineComplete() && headers != null) {
                for (H2HeaderField header : headers) {

                    appendHeader(header.getName(), header.getValue());
                }
            }
        }

        return true;
    }

    private void setH2FirstLineComplete(HashMap<String, String> pseudoHeaders) throws Exception {
        if (!checkMandatoryPseudoHeaders(pseudoHeaders)) {
            throw new CompressionException("Not all mandatory pseudo-header fields were provided.");
        }
        setPseudoHeaders(pseudoHeaders);
        this.setFirstLineComplete(true);
    }

    protected abstract void setPseudoHeaders(HashMap<String, String> pseudoHeaders) throws Exception;

    protected abstract boolean checkMandatoryPseudoHeaders(HashMap<String, String> pseudoHeaders);

    protected abstract boolean isValidPseudoHeader(H2HeaderField pseudoHeader);

    protected abstract H2HeaderTable getH2HeaderTable();

    public abstract WsByteBuffer[] encodePseudoHeaders();

    /**
     * During discrimination, we are only attempting to parse the first line so
     * that we can verify the HTTP protocol information.
     *
     * @param buffer
     * @return boolean (true means first line fully parsed)
     * @throws Exception
     */
    public boolean parseLineDiscrim(WsByteBuffer buffer) throws Exception {
        getServiceContext().checkIncomingMessageLimit(buffer.remaining());
        if (getServiceContext().getHttpConfig().isBinaryTransportEnabled()) {
            return parseBinaryFirstLine(buffer);
        }
        return parseLine(buffer);
    }

    /**
     * Begin parsing line out from a given buffer. Returns boolean
     * as to whether it has found the end of the first line.
     *
     * @param buff
     * @return boolean
     * @throws MalformedMessageException
     */
    public abstract boolean parseBinaryFirstLine(WsByteBuffer buff) throws MalformedMessageException;

    /**
     * Parses the binary HTTP message. Returns whether or not it has
     * successfully parsed the full message. There is no delayed
     * extraction of header values.
     *
     * @param buff
     * @return boolean
     * @throws Exception
     */
    public boolean parseBinaryMessage(WsByteBuffer buff) throws Exception {

        boolean rc = false;
        if (!isFirstLineComplete()) {
            rc = parseBinaryFirstLine(buff);
        }

        // if we've read the first line, then parse the headers
        if (isFirstLineComplete()) {
            // keep parsing headers until that returns the "finished" response
            rc = parseBinaryHeaders(buff, HttpHeaderKeys.HDR_$WSAT);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseBinaryMessage returning " + rc);
        }
        return rc;
    }

    /**
     * Marshall the given cookie cache object into base header storage.
     *
     * @param cache
     */
    private void marshallCookieCache(CookieCacheData cache) {

        if (null != cache && cache.isDirty()) {
            HttpHeaderKeys type = cache.getHeaderType();
            parseAllCookies(cache, type);
            removeSpecialHeader(type);
            marshallCookies(cache.getParsedList(), type);
            cache.setIsDirty(false);
        }
    }

    /**
     * Query whether the cookie cache for the given header is dirty or not.
     *
     * @param key
     * @return boolean
     */
    protected boolean isCookieCacheDirty(HeaderKeys key) {
        // @338148 - add Cache-Control header
        if (HttpHeaderKeys.HDR_SET_COOKIE.equals(key)) {
            return (null != this.setCookieCache) ? this.setCookieCache.isDirty() : false;
        }
        if (HttpHeaderKeys.HDR_SET_COOKIE2.equals(key)) {
            return (null != this.setCookie2Cache) ? this.setCookie2Cache.isDirty() : false;
        }
        if (HttpHeaderKeys.HDR_COOKIE.equals(key)) {
            return (null != this.cookieCache) ? this.cookieCache.isDirty() : false;
        }
        if (HttpHeaderKeys.HDR_COOKIE2.equals(key)) {
            return (null != this.cookie2Cache) ? this.cookie2Cache.isDirty() : false;
        }
        return false;
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.GenericMessageImpl#preMarshallMessage()
     */
    @Override
    protected void preMarshallMessage() throws MessageSentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "preMarshallMessage");
        }

        super.preMarshallMessage();
        /**
         * Only if the cookies in storage(primary list of all headers) have been
         * modified do we reserialize/transform before marshalling otherwise we
         * leave the primary list untouched. After marshalling the cache is
         * flushed
         */
        marshallCookieCache(this.cookieCache);
        marshallCookieCache(this.cookie2Cache);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Checking to see if we should mark the cookie cache as dirty - samesite is " + getServiceContext().getHttpConfig().useSameSiteConfig()
                         + " doNotAllowDuplicateSetCookie is " + getServiceContext().getHttpConfig().doNotAllowDuplicateSetCookies());
        }
        if (getServiceContext().getHttpConfig().useSameSiteConfig() || getServiceContext().getHttpConfig().doNotAllowDuplicateSetCookies()) {
            //If there are set-cookie and set-cookie2 headers and the respective cache hasn't been initialized,
            //do so and set it as dirty so the cookie parsing logic is run.
            if (this.containsHeader(HttpHeaderKeys.HDR_SET_COOKIE) && (this.setCookieCache == null)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Marking set-cookie cache dirty");
                }
                getCookieCache(HttpHeaderKeys.HDR_SET_COOKIE).setIsDirty(true);
            }

            if (this.containsHeader(HttpHeaderKeys.HDR_SET_COOKIE2) && (this.setCookie2Cache == null)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Marking set-cookie2 cache dirty");
                }
                getCookieCache(HttpHeaderKeys.HDR_SET_COOKIE2).setIsDirty(true);
            }

        }
        marshallCookieCache(this.setCookieCache);
        marshallCookieCache(this.setCookie2Cache);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "preMarshallMessage");
        }
    }

    // ***********************************************************************
    // Block of methods for the special J2EE Cookie manipulation
    // ***********************************************************************

    /**
     * When the only required bit of information from a cookie is the value
     * from the name=value parameter, this API can be used to get just that.
     * It will avoid any extra object creation such as Strings or Cookies
     * themselves.
     *
     * Any leading or trailing whitespace will be removed from the value;
     * however, no error checking is performed to see if any whitespace in the
     * middle is encapsulated with quotes... whatever is found will be returned.
     * Thus [Cookie: id=12 34] would return "12 34" as the value for "id".
     *
     * @param name
     * @param hdr
     * @return byte[] (null if target cookie was not found)
     */
    protected byte[] getCookieValue(String name, HttpHeaderKeys hdr) {

        // would be nice to check the cache for any existing parsed cookies
        // but current architecture doesn't allow that
        int num = getNumberOfHeaderInstances(hdr);
        if (0 == num) {
            // header not present
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Header not present (" + hdr.getName() + ")");
            }
            return null;
        }
        if (null == name || 0 == name.length()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getCookieValue: invalid name [" + name + "]");
            }
            return null;
        }
        String targetName = name;
        if (QUOTE == name.charAt(0) && QUOTE == name.charAt(name.length() - 1)) {
            targetName = name.substring(1, name.length() - 1);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCookieValue: [" + targetName + "] num: " + num);
        }
        byte[] data;
        int index = 0;
        int x;
        boolean bQuoted = false;
        Iterator<HeaderField> it = getHeaders(hdr).iterator();
        while (it.hasNext()) {
            index = 0;
            data = it.next().asBytes();
            while (index < data.length) {
                // search each name token (skip leading white space)
                if (BNFHeaders.SPACE == data[index]) {
                    index = GenericUtils.skipWhiteSpace(data, index);
                    if (index >= data.length) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Ran out of data");
                        }
                        // continue to next cookie header instance
                        continue;
                    }
                }
                // we're at the start of the name of this cookie token
                // check for quotes around the name inside the cookie data
                bQuoted = (QUOTE == data[index]);
                //PI51523 - Check it is not the last element before increasing counter
                if (bQuoted && (index < data.length - 1)) {
                    index++;
                }
                // compare the name
                for (x = 0; x < targetName.length(); x++) {
                    if (targetName.charAt(x) != data[index]) {
                        // despite RFC2109, cookie names are case-sensitive
                        break;
                    }
                    index++;
                    if (index >= data.length) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Only the cookie name found");
                        }
                        return null;
                    }
                }

                // possible conditions now are that we didn't match enough
                // chars, continue on. If we're comparing a quoted name, then
                // the next char must be a quote. Otherwise there can only be
                // whitespace before the equals sign
                if (x != targetName.length()) {
                    // didn't find a match, skip to the next semi-colon
                    index = GenericUtils.skipToChars(data, index, COOKIE_DELIMS) + 1;
                    continue;
                }
                if (bQuoted) {
                    if (QUOTE != data[index]) {
                        index = GenericUtils.skipToChars(data, index, COOKIE_DELIMS) + 1;
                        continue;
                    }
                    index++;
                }
                // name match so far, look for the equals sign but fail if a
                // non-space is found along the way
                if (index < data.length && BNFHeaders.SPACE == data[index]) {
                    index = GenericUtils.skipWhiteSpace(data, index);
                }
                if (index < data.length && HttpBaseMessage.EQUALS != data[index]) {
                    index = GenericUtils.skipToChars(data, index, COOKIE_DELIMS) + 1;
                    continue;
                }
                // skip past the equals sign
                index++;
                if (index >= data.length) {
                    // ran out of data ... "name=" with no value
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Cookie name found but no value after =");
                    }
                    return null;
                }

                // ---------------------------
                // now start parsing the value
                // ---------------------------

                // check for leading white space
                if (BNFHeaders.SPACE == data[index]) {
                    index = GenericUtils.skipWhiteSpace(data, index);
                    if (index >= data.length) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Cookie name with just whitespace val");
                        }
                        return null;
                    }
                }
                bQuoted = (QUOTE == data[index]);
                int start = index;
                if (bQuoted) {
                    index++;
                    while (index < data.length && QUOTE != data[index] && COMMA != data[index] && SEMICOLON != data[index]) {
                        index++;
                    }
                    // TODO quotes are NOT part of the value and should be stripped off
                    if (index < data.length && QUOTE == data[index]) {
                        // the quote is part of the value
                        index++;
                    }
                } else {
                    // find the end of data or delimiter (ignoring trailing
                    // whitespace)
                    while (index < data.length && SEMICOLON != data[index] && COMMA != data[index]) {
                        if (BNFHeaders.SPACE == data[index]) {
                            int i2 = GenericUtils.skipWhiteSpace(data, index);
                            if (i2 >= data.length || SEMICOLON == data[i2] || COMMA == data[i2]) {
                                // found the end of data, don't update index
                                break;
                            }
                            index = i2;
                        }
                        index++;
                    }
                }
                if (start >= index) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "No actual value found");
                    }
                    return null;
                }
                byte[] value = new byte[index - start];
                System.arraycopy(data, start, value, 0, value.length);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getCookieValue: returning [" + GenericUtils.getEnglishString(value) + "]");
                }
                return value;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Cookie name not found");
        }
        return null;
    }

    /**
     * Gather the list of all cookie values present in the message for the given
     * header key and input cookie name. Add these values to the input list.
     *
     * @param name
     * @param header
     * @param list
     */
    protected void getAllCookieValues(String name, HttpHeaderKeys header, List<String> list) {
        if (!cookieCacheExists(header) && !containsHeader(header)) {
            return;
        }
        CookieCacheData cache = getCookieCache(header);
        parseAllCookies(cache, header);
        cache.getAllCookieValues(name, list);
    }

    /**
     * Search for a cookie under the input header name that matches the target
     * cookie name.
     *
     * @param name
     * @param header
     * @return the <code>Cookie</code> for the specified name.
     *         NULL will be returned if it does not exist.
     */
    protected HttpCookie getCookie(String name, HttpHeaderKeys header) {
        if (cookieCacheExists(header) || containsHeader(header)) {
            CookieCacheData cache = getCookieCache(header);
            HttpCookie cookie = cache.getCookie(name);
            if (null != cookie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found " + name + " in cache");
                }
                return cookie;
            }

            // Now search the cookie header instances in storage and add them
            // to the parsed list
            List<HeaderField> vals = getHeaders(header);
            int size = vals.size();
            if (size != 0) {
                for (int i = cache.getHeaderIndex(); i < size; i++) {
                    List<HttpCookie> list = getCookieParser().parse(vals.get(i).asBytes(), header);
                    cache.addParsedCookies(list);
                    cache.incrementHeaderIndex();
                    // search the list of new cookies from this header instance
                    Iterator<HttpCookie> it = list.iterator();
                    while (it.hasNext()) {
                        cookie = it.next();
                        // cookie names are case-sensitive
                        if (cookie.getName().equals(name)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Found parsed Cookie-->" + name);
                            }
                            return cookie;
                        }
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCookie --> " + name + " of type " + header.getName() + " not found");
        }
        return null;
    }

    /**
     * Method to parse all of the unparsed header instances for the given input
     * type into Cookie objects to store in the cache.
     *
     * @param cache
     * @param header
     */
    private void parseAllCookies(CookieCacheData cache, HttpHeaderKeys header) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsing all cookies for " + header.getName());
        }

        // Iterate through the unparsed cookie header instances
        // in storage and add them to the list to be returned
        List<HeaderField> vals = getHeaders(header);
        int size = vals.size();
        if (size != 0) {
            for (int i = cache.getHeaderIndex(); i < size; i++) {
                cache.addParsedCookies(getCookieParser().parse(vals.get(i).asBytes(), header));
                cache.incrementHeaderIndex();
            }
        }
    }

    /**
     * Add all cookies from this message under the input header into the input
     * list.
     *
     * @param header
     * @param list
     */
    protected void getAllCookies(HttpHeaderKeys header, List<HttpCookie> list) {
        if (!cookieCacheExists(header) && !containsHeader(header)) {
            return;
        }
        CookieCacheData cache = getCookieCache(header);
        parseAllCookies(cache, header);
        cache.getAllCookies(list);
    }

    /**
     * Find all instances of a cookie under the given header with the input name
     * and place a clone of that object into the input list.
     *
     * @param name
     * @param header
     * @param list
     */
    protected void getAllCookies(String name, HttpHeaderKeys header, List<HttpCookie> list) {
        if (!cookieCacheExists(header) && !containsHeader(header)) {
            return;
        }
        CookieCacheData cache = getCookieCache(header);
        parseAllCookies(cache, header);
        cache.getAllCookies(name, list);
    }

    /**
     * Adds the cookie to the parsedCookieList of the appropriate cookie cache
     * data. The set operation does not have the semantics of the replace
     * operation. This is allowed on an outgoing message only.
     *
     * @param cookie
     *                       the <code>HttpCookie</code> to add.
     * @param cookieType
     * @return TRUE if the cookie was set successfully otherwise returns FALSE.
     *         if setcookie constraints are violated.
     **/
    protected boolean addCookie(HttpCookie cookie, HttpHeaderKeys cookieType) {
        // Set is only permitted on non-committed messages
        if (isCommitted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Not adding cookie to committed message: " + cookie.getName() + " " + cookieType.getName());
            }
            return false;
        }
        // Note: store a clone to avoid corruption by the caller
        getCookieCache(cookieType).addNewCookie(cookie.clone());
        return true;
    }

    /**
     * Get access to the cookie parser for this message.
     *
     * @return An instance of the Cookie header parser
     */
    private CookieHeaderByteParser getCookieParser() {
        if (null == this.cookieParser) {
            this.cookieParser = new CookieHeaderByteParser();
        }
        return this.cookieParser;
    }

    /**
     * Check whether the cookie cache already exists for this particular header.
     *
     * @param header
     * @return boolean
     */
    protected boolean cookieCacheExists(HttpHeaderKeys header) {
        if (header == HttpHeaderKeys.HDR_COOKIE) {
            return (null != this.cookieCache);
        }
        if (header == HttpHeaderKeys.HDR_COOKIE2) {
            return (null != this.cookie2Cache);
        }
        if (header == HttpHeaderKeys.HDR_SET_COOKIE) {
            return (null != this.setCookieCache);
        }
        if (header == HttpHeaderKeys.HDR_SET_COOKIE2) {
            return (null != this.setCookie2Cache);
        }
        return false;
    }

    /**
     * Return the set of objects for effectively caching Cookies as they
     * are processed.
     *
     * @param header
     * @return the caching data for the particular set of Cookies.
     * @throws IllegalArgumentException
     *                                      if the header is not a cookie header
     */
    private CookieCacheData getCookieCache(HttpHeaderKeys header) {
        // 347066 - removed sync because we only allow 1 thread to be working
        // on a message a time anyways

        // For outgoing messages, parse the cookies out immediately so that we
        // don't have to worry about people changing header storage in the
        // middle (which throws off the parse cookie logic)
        if (header.equals(HttpHeaderKeys.HDR_COOKIE)) {
            if (null == this.cookieCache) {

                this.cookieCache = new CookieCacheData(header);
                if (!isIncoming()) {
                    parseAllCookies(this.cookieCache, header);
                }
            }
            return this.cookieCache;

        } else if (header.equals(HttpHeaderKeys.HDR_COOKIE2)) {
            if (null == this.cookie2Cache) {
                this.cookie2Cache = new CookieCacheData(header);
                if (!isIncoming()) {
                    parseAllCookies(this.cookie2Cache, header);
                }
            }
            return this.cookie2Cache;

        } else if (header.equals(HttpHeaderKeys.HDR_SET_COOKIE)) {
            if (null == this.setCookieCache) {
                this.setCookieCache = new CookieCacheData(header);
                if (!isIncoming()) {
                    parseAllCookies(this.setCookieCache, header);
                }
            }
            return this.setCookieCache;

        } else if (header.equals(HttpHeaderKeys.HDR_SET_COOKIE2)) {
            if (null == this.setCookie2Cache) {
                this.setCookie2Cache = new CookieCacheData(header);
                if (!isIncoming()) {
                    parseAllCookies(this.setCookie2Cache, header);
                }
            }
            return this.setCookie2Cache;
        }
        throw new IllegalArgumentException(header.getName());
    }

    /**
     * Remove a cookie with a specific name from a message. Requests can remove
     * ones of type "Cookie" or "Cookie2", while response messages can remove
     * "Set-Cookie" or "Set-Cookie2" cookies.
     *
     * @param name
     * @param header
     * @return TRUE if a cookie with this name was removed, FALSE if the
     *         cookie could not be deleted. The removal would fail if
     *         the cookie with the specified name does not exist or if the
     *         constraints were not satisfied.
     */
    protected boolean deleteCookie(String name, HttpHeaderKeys header) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "deleteCookie: " + name);
        }
        boolean rc = false;
        if (isCommitted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Not removing committed cookie: " + name);
            }
        } else {
            // call getCookie in case we need to still parse anything
            HttpCookie cookie = getCookie(name, header);
            if (null != cookie) {
                rc = getCookieCache(header).removeCookie(cookie);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "deleteCookie: " + rc);
        }
        return rc;
    }

    /**
     * @see com.ibm.wsspi.http.channel.cookies.CookieHandler#containsCookie(java.lang.String, com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */
    @Override
    public boolean containsCookie(String name, HttpHeaderKeys header) {
        if (null == name || null == header) {
            return false;
        }
        return (null != getCookie(name, header));
    }

    /**
     * Marshall the list of Cookies into the base header storage area.
     *
     * @param list
     *                   the list of new cookies.
     * @param header
     *                   the type of header the new cookies are intended for.
     */
    private void marshallCookies(List<HttpCookie> list, HeaderKeys header) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "marshallCookies");
        }

        HashMap<String, String> setCookieNames = null; //PI31734

        // convert each individual cookie into it's own header for clarity
        // Note: Set-Cookie header has comma separated cookies instead of semi-
        // colon separation (if cookies were to go into one single header instead
        // of multiple)
        for (HttpCookie cookie : list) {
            //Add Samesite default config
            if (getServiceContext().getHttpConfig().useSameSiteConfig() && cookie.getAttribute("samesite") == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No SameSite value has been added for [" + cookie.getName() + "], checking configuration for a match");
                }
                String sameSiteAttributeValue = null;
                Matcher m = null;

                //First attempt to match the name explicitly.
                if (getServiceContext().getHttpConfig().getSameSiteCookies().containsKey(cookie.getName())) {
                    sameSiteAttributeValue = getServiceContext().getHttpConfig().getSameSiteCookies().get(cookie.getName());
                }
                //If the only pattern is a standalone '*' avoid regex cost
                else if (getServiceContext().getHttpConfig().onlySameSiteStar()) {
                    sameSiteAttributeValue = getServiceContext().getHttpConfig().getSameSiteCookies().get(HttpConfigConstants.WILDCARD_CHAR);
                }

                else {
                    //Attempt to find a match amongst the configured SameSite patterns
                    for (Pattern p : getServiceContext().getHttpConfig().getSameSitePatterns().keySet()) {
                        m = p.matcher(cookie.getName());
                        if (m.matches()) {
                            sameSiteAttributeValue = getServiceContext().getHttpConfig().getSameSitePatterns().get(p);
                            break;
                        }
                    }

                }

                if (sameSiteAttributeValue != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "SameSite configuration found, value set to: " + sameSiteAttributeValue);
                    }
                    cookie.setAttribute("samesite", sameSiteAttributeValue);
                    //If SameSite has been defined, and it's value is set to 'none', ensure the cookie is set to secure
                    if (!cookie.isSecure() && sameSiteAttributeValue.equalsIgnoreCase(HttpConfigConstants.SameSite.NONE.getName())) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Setting the Secure attribute for SameSite=None");
                        }
                        cookie.setSecure(true);
                    }

                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "No SameSite configuration found");
                    }
                }
            }

            String value = CookieUtils.toString(cookie, header, getServiceContext().getHttpConfig().isv0CookieDateRFC1123compat(),
                                                getServiceContext().getHttpConfig().shouldSkipCookiePathQuotes());
            if (null != value) {

                //PI31734 start
                if (getServiceContext().getHttpConfig().doNotAllowDuplicateSetCookies() && (header.getName().equals("Set-Cookie"))) {
                    if (setCookieNames == null)
                        setCookieNames = new HashMap<String, String>();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && setCookieNames.containsKey(cookie.getName())) {
                        Tr.debug(tc, "Found Duplicated Set-Cookie, replacing it for the newest one: [Set-Cookie: " + value + "]");
                    }
                    setCookieNames.put(cookie.getName(), value);
                } else {
                    appendHeader(header, value);
                }
            }

        }

        if (getServiceContext().getHttpConfig().doNotAllowDuplicateSetCookies() && setCookieNames != null) {
            //Loop here to append all the cookies from the HashMap
            Iterator<String> keyIt = setCookieNames.keySet().iterator();
            while (keyIt.hasNext()) {
                appendHeader(header, setCookieNames.get(keyIt.next()));
            }
        }
        //PI31734 end

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "marshallCookies");
        }
    }

    /**
     * Get the start time of this request in nanoseconds
     *
     * @return long
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * Private class used while parsing through header value tokens. Instances
     * of this class will provide the caller with information on where the
     * token is in the overall array.
     *
     */
    protected class TokenDelimiter {
        /** Start index of this token */
        protected int start = HeaderStorage.NOTSET;
        /** Length of this token */
        protected int length = HeaderStorage.NOTSET;
        /** Index of where to start parsing again after this token */
        protected int next = HeaderStorage.NOTSET;

        /**
         * Clear the information back to defaults.
         *
         * @return TokenDelimiter (this)
         */
        protected TokenDelimiter clear() {
            this.start = HeaderStorage.NOTSET;
            this.length = HeaderStorage.NOTSET;
            this.next = HeaderStorage.NOTSET;
            return this;
        }
    }

    /**
     * Java security requires that the Charset interaction be performed inside
     * a privileged block of code. This class encapsulates running a Charset
     * lookup based on an input string name.
     *
     */
    private class privCharsetLookup implements PrivilegedAction<Charset> {
        /** Encoding string to lookup */
        private final String encoding;

        /**
         * Constructor.
         *
         * @param enc
         */
        public privCharsetLookup(String enc) {
            this.encoding = enc;
        }

        /**
         * Perform the privileged action now.
         *
         * @return Object (Charset)
         */
        @Override
        public Charset run() {
            return Charset.forName(this.encoding);
        }
    }
}
