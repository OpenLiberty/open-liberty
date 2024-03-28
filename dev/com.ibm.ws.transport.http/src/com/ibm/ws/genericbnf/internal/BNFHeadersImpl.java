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
package com.ibm.ws.genericbnf.internal;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.h2internal.hpack.H2Headers;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants.LiteralIndexType;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.genericbnf.BNFHeaders;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.genericbnf.exception.MalformedMessageException;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * Generic class implementing an Augmented BNF Header/Value storage. This
 * will handle header keys that are previously defined, or handle new values
 * on the fly as required. Values can be represented as either byte[]s, Strings,
 * or if they were parsed from WsByteBuffers then simply references back to the
 * parsed buffer.
 *
 */
public abstract class BNFHeadersImpl implements BNFHeaders, Externalizable {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(BNFHeadersImpl.class,
                                                         GenericConstants.GENERIC_TRACE_NAME,
                                                         null);

    /** Serialization UID */
    private static final long serialVersionUID = -4154557451251031540L;

    /** Serialization format for v6.0 and v6.1 */
    protected static final int SERIALIZATION_V1 = 0xBEEF0001;
    /** Serialization format for v7 and higher */
    protected static final int SERIALIZATION_V2 = 0xBEEF0002;

    /** Identifier that we're parsing a header name */
    private static final int PARSING_HEADER = 0;
    /** Identifier that we're parsing a header value */
    private static final int PARSING_VALUE = 1;
    /** Identifer that we're parsing the CRLF marker */
    private static final int PARSING_CRLF = 2;
    /** Local default size of buffers */
    private static final int DEFAULT_BUFFERSIZE = 1024;
    /** Local default size of byte cache */
    private static final int DEFAULT_CACHESIZE = 512;
    /** Default maximum size of any individual token */
    private static final int DEFAULT_LIMIT_TOKENSIZE = 16384;
    /** Default maximum number of headers allowed in this message */
    private static final int DEFAULT_LIMIT_NUMHEADERS = 500;
    /** starting size of the pending buffer array */
    private static final int BUFFERS_INITIAL_SIZE = 2;
    /** starting min growth size of the pending buffer array */
    private static final int BUFFERS_MIN_GROWTH = 5;
    /** Identifier for triggering the calling of the header filter methods */
    private static final boolean FILTER_YES = true;
    /** Identifier to prevent the calling of the header filter methods */
    private static final boolean FILTER_NO = false;

    // when logging tokens during parsing, we may want to block parts of it
    // from the log... either just "password=xxx" or the entire contents
    /** Log the entire contents in the clear */
    protected static final int LOG_FULL = 0;
    /** Log none of the contents */
    protected static final int LOG_NONE = 1;
    /** Scan for and block "password=xxxx" but leave the rest */
    protected static final int LOG_PARTIAL = 2;

    /** Static block of whitespace used by the new scribble marshalling */
    private static byte[] whitespace = null;

    /** Empty object used when a header is not present */
    public static final HeaderField NULL_HEADER = new EmptyHeaderField();

    private static final String FOR = "for";

    private static final String BY = "by";

    private static final String PROTO = "proto";

    private static final String HOST = "host";

    private static final String X_FORWARDED_FOR = "x-forwarded-for";

    private static final String X_FORWARDED_BY = "x-forwarded-by";

    private static final String X_FORWARDED_PROTO = "x-forwarded-proto";

    private static final String X_FORWARDED_HOST = "x-forwarded-host";

    private static final String X_FORWARDED_PORT = "x-forwarded-port";

    // ********************************************************************
    // Header storage related items
    // ********************************************************************

    /** Storage for the header/value pairs */
    private transient HashMap<Integer, HeaderElement> storage = new HashMap<Integer, HeaderElement>();

    /**
     * This array stores the names of the headers in the list they were
     * either parsed or set by the user (depending on scenario)
     */
    private transient HeaderElement hdrSequence = null;
    /** reference to the last header in the sequence */
    private transient HeaderElement lastHdrInSequence = null;
    /** Factory for local HeaderElement objects */
    private transient HeaderElement headerElements = null;
    /** List of buffers used during parsing of headers */
    private transient WsByteBuffer[] parseBuffers = null;
    /** Ref to the starting position of each parse buffer */
    private transient int[] parseBuffersStartPos = null;
    /** Index of next item to be added in "parsing headers" buffer list */
    private transient int parseIndex = HeaderStorage.NOTSET;
    /** List of created buffers that must be released later */
    private transient WsByteBuffer[] myCreatedBuffers = null;
    /** Index into the created buffer list */
    private transient int createdIndex = HeaderStorage.NOTSET;
    /** Current count of the number of headers in storage */
    private transient int numberOfHeaders = 0;
    /** Flag on whether to perform header validation or not */
    private transient boolean bHeaderValidation = true;
    /** Flag on whether to perform character validation in the header or not */
    private transient static boolean bCharacterValidation = true; //PI45266
    /** Flag on whether to use the channel is configured to use the remote Ip, Forwarded/X-Forwarded headers */
    private transient static boolean bRemoteIp = false;

    // ********************************************************************
    // Header parsing related items
    // ********************************************************************

    /** Index into the parse buffers of the last CRLF position */
    private transient int lastCRLFBufferIndex = HeaderStorage.NOTSET;
    /** Position in a parse buffer of the last found CRLF */
    private transient int lastCRLFPosition = HeaderStorage.NOTSET;
    /** Did we stop on a CR or an LF? */
    private transient boolean lastCRLFisCR = false;
    /** Limit on the number of changes or removals for remarshalling */
    private transient int headerChangeLimit = HeaderStorage.NOTSET;
    /** Counter of number of changes and removals of headers */
    private transient int headerChangeCount = 0;
    /** Counter of number of new headers added */
    private transient int headerAddCount = 0;
    /** Flag on whether we're over the change limit */
    private transient boolean bOverChangeLimit = false;
    /** Maximum size of a token -- any parsed token (name, value, etc) */
    private transient int limitTokenSize = DEFAULT_LIMIT_TOKENSIZE;
    /** Maximum number of headers allowed in this message */
    private transient int limitNumHeaders = DEFAULT_LIMIT_NUMHEADERS;
    /** position in last buffers of the end of headers mark */
    private transient int eohPosition = HeaderStorage.NOTSET;
    /** current wsbb being parsed/read */
    private transient WsByteBuffer currentReadBB = null;
    /** flag on whether to use direct byte buffers or not */
    private transient boolean useDirectBuffer = true;
    /** size of buffers for outgoing marshalled headers */
    private transient int outgoingHdrBufferSize = DEFAULT_BUFFERSIZE;
    /** size of buffers to allocate when parsing incoming headers */
    private transient int incomingBufferSize = DEFAULT_BUFFERSIZE;
    /** size of the byte cache to use */
    private transient int byteCacheSize = DEFAULT_CACHESIZE;
    /** parsed out token */
    private transient byte[] parsedToken = null;
    /** length of the token being parsed */
    private transient int parsedTokenLength = 0;
    /** byte cache which is reusable by all */
    private transient byte[] byteCache = new byte[this.byteCacheSize];
    /** position in the byte cache */
    private transient int bytePosition = 0;
    /** limit in the byte cache */
    private transient int byteLimit = 0;
    /** Flag keeping track of the current parsing identifier */
    private transient int stateOfParsing = PARSING_CRLF;
    /** state of parsing for a binary http message */
    private transient int binaryParsingState = GenericConstants.PARSING_HDR_FLAG;
    /** current header being parsed */
    private transient HeaderElement currentElem = null;
    /** Flag on whether the current header is a multiline value or not */
    private transient boolean bIsMultiLine = false;
    /** Number of CRLFs currently found during parsing */
    private transient int numCRLFs = 0;
    /** Object used for additional debug data, defaults to just "this" */
    private transient Object debugContext = this;
    /** Flag used for SIP compact headers support */
    private transient boolean compactHeaderFlag = false;
    /** Version used during deserialization step (if msg came that path) */
    private transient int deserializationVersion = SERIALIZATION_V1;
    /** PI13987 - Did we find any trailing whitespace in the header name */
    private boolean foundTrailingWhitespace = false;
    /** Defined if it is an HTTP/2.0 connection when encoding headers */
    private H2HeaderTable table = null;
    /** Defined if this is an HTTP/2.0 connection servicing a Push Promise response */
    private boolean isPushPromise = false;
    /** Flag used to identify if an X-Forwarded-* header has been added */
    private boolean processedXForwardedHeader = false;
    /** Flag used to identify if a Forwarded header has been added */
    private boolean processedForwardedHeader = false;
    /**
     * Flag used to identify if there was an error parsing the Forwarded header and it should
     * not be further parsed.
     */
    private boolean forwardHeaderErrorState = false;
    /**
     * String Builder representing a comma delimited list of processed X-Forwarded-For / Forwarded "for"
     * node identifiers.
     */
    private ArrayList<String> forwardedForList = null;
    /**
     * String Builder representing a comma delimited list of processed X-Forwarded-By / Forwarded "by"
     * node identifiers.
     */
    private ArrayList<String> forwardedByList = null;
    /** Identifies the original client request's used protocol, as defined by X-Forwarded-Proto / Forwarded "proto" */
    private String forwardedProto = null;
    /** Identifies the original client request's host used as defined by the Forwarded "host" parameter. */
    private String forwardedHost = null;
    /**
     * Identifies the original client requet's port as defined by X-Fowarded-Port / or the inclusion of the port in
     * the first address of the Forwarded "for" list.
     */
    private String forwardedPort = null;

    /**
     * Records whether a particular header has been added to this BNFHeaderImpl instance.
     * Used to speed up getHeader calls for headers that have not been added, by avoiding
     * unnecessary calls to findHeaders.
     *
     * Note: headers may be removed, after being added. In this case we do not reset the
     * corresponding bit, to limit the implementation complexity. Therefore in cases where
     * headers have been removed, we may get a false positive from headersAdded. This is OK
     * because the next step is always findHeaders; we do not rely solely on headersAdded.
     *
     * Index: headerKey ordinal
     * Value: true if header was added prior to checking the bit
     */
    private BitSet headersAdded = new BitSet();
    /**
     * The number of possible headers is virtually unlimited. We limit this fail-fast technique
     * to the first N header keys registered in the system, to limit the complexity.
     */
    private static final int maxHeadersAdded = 256;

    /**
     * Identifies between the forwarded for and by lists
     */
    private enum ListType {
        FOR, BY
    };

    /**
     * Constructor for the headers storage object.
     */
    public BNFHeadersImpl() {
        // nothing
    }

    /**
     * Initialize this class instance with the chosen parse configuration
     * options.
     *
     * @param useDirect -- use direct ByteBuffers or indirect
     * @param outSize   -- size of buffers to use while marshalling headers
     * @param inSize    -- size of buffers to use while parsing headers
     * @param cacheSize -- byte cache size of optimized parsing
     */
    protected void init(boolean useDirect, int outSize, int inSize, int cacheSize) {

        this.useDirectBuffer = useDirect;
        this.outgoingHdrBufferSize = outSize;
        this.incomingBufferSize = inSize;
        // if cache size has increased, then allocate the larger bytecache
        // array, but don't change to a smaller array
        if (cacheSize > this.byteCacheSize) {
            this.byteCacheSize = cacheSize;
            this.byteCache = new byte[cacheSize];
        }
    }

    // ***********************************************************************
    // External APIs
    // ***********************************************************************

    /**
     * Save a reference to a new buffer with header parse information. This is
     * not part of the "created list" and will not be released by this message
     * class.
     *
     * @param buffer
     */
    public void addParseBuffer(WsByteBuffer buffer) {

        // increment where we're about to put the new buffer in
        int index = ++this.parseIndex;
        if (null == this.parseBuffers) {
            // first parse buffer to track
            this.parseBuffers = new WsByteBuffer[BUFFERS_INITIAL_SIZE];
            this.parseBuffersStartPos = new int[BUFFERS_INITIAL_SIZE];
            for (int i = 0; i < BUFFERS_INITIAL_SIZE; i++) {
                this.parseBuffersStartPos[i] = HeaderStorage.NOTSET;
            }
        } else if (index == this.parseBuffers.length) {
            // grow the array
            int size = index + BUFFERS_MIN_GROWTH;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Increasing parse buffer array size to " + size);
            }
            WsByteBuffer[] tempNew = new WsByteBuffer[size];
            System.arraycopy(this.parseBuffers, 0, tempNew, 0, index);
            this.parseBuffers = tempNew;

            int[] posNew = new int[size];
            System.arraycopy(this.parseBuffersStartPos, 0, posNew, 0, index);
            for (int i = index; i < size; i++) {
                posNew[i] = HeaderStorage.NOTSET;
            }
            this.parseBuffersStartPos = posNew;
        }
        this.parseBuffers[index] = buffer;
    }

    /**
     * Add a buffer on the list that will be manually released later.
     *
     * @param buffer
     */
    public void addToCreatedBuffer(WsByteBuffer buffer) {
        // increment where we're about to put the new buffer in
        int index = ++this.createdIndex;
        if (null == this.myCreatedBuffers) {
            // first allocation
            this.myCreatedBuffers = new WsByteBuffer[BUFFERS_INITIAL_SIZE];
        } else if (index == this.myCreatedBuffers.length) {
            // grow the array
            int size = index + BUFFERS_MIN_GROWTH;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Increasing created buffer array size to " + size);
            }
            WsByteBuffer[] tempNew = new WsByteBuffer[size];
            System.arraycopy(this.myCreatedBuffers, 0, tempNew, 0, index);
            this.myCreatedBuffers = tempNew;
        }
        this.myCreatedBuffers[index] = buffer;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#appendHeader(String, byte[])
     */
    @Override
    public void appendHeader(String header, byte[] value) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "appendHeader(s,b): " + header);
        }
        if (this.bHeaderValidation) {
            checkHeaderValue(value, 0, value.length);
        }
        HeaderElement elem = getElement(findKey(header, false));
        elem.setByteArrayValue(value);
        addHeader(elem, FILTER_YES);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#appendHeader(java.lang.String, byte[], int, int)
     */
    @Override
    public void appendHeader(String header, byte[] value, int offset, int length) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "appendHeader(s,b,i,i): " + header);
        }
        if (this.bHeaderValidation) {
            checkHeaderValue(value, offset, length);
        }
        HeaderElement elem = getElement(findKey(header, false));
        elem.setByteArrayValue(value, offset, length);
        addHeader(elem, FILTER_YES);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#appendHeader(byte[], byte[])
     */
    @Override
    public void appendHeader(byte[] header, byte[] value) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "appendHeader(b,b): " + GenericUtils.getEnglishString(header));
        }
        if (this.bHeaderValidation) {
            checkHeaderValue(value, 0, value.length);
        }
        HeaderElement elem = getElement(findKey(header, false));
        elem.setByteArrayValue(value);
        addHeader(elem, FILTER_YES);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#appendHeader(byte[], byte[], int, int)
     */
    @Override
    public void appendHeader(byte[] header, byte[] value, int offset, int length) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "appendHeader(b,b,i,i): " + GenericUtils.getEnglishString(header));
        }
        if (this.bHeaderValidation) {
            checkHeaderValue(value, offset, length);
        }
        HeaderElement elem = getElement(findKey(header, false));
        elem.setByteArrayValue(value, offset, length);
        addHeader(elem, FILTER_YES);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#appendHeader(HeaderKeys, byte[])
     */
    @Override
    public void appendHeader(HeaderKeys key, byte[] value) {
        if (null == key || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "appendHeader(h,b): " + key.getName());
        }
        if (this.bHeaderValidation) {
            checkHeaderValue(value, 0, value.length);
        }
        HeaderElement elem = getElement(key);
        elem.setByteArrayValue(value);
        addHeader(elem, FILTER_YES);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#appendHeader(com.ibm.wsspi.genericbnf.HeaderKeys, byte[], int, int)
     */
    @Override
    public void appendHeader(HeaderKeys key, byte[] value, int offset, int length) {
        if (null == key || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "appendHeader(h,b,i,i): " + key.getName());
        }
        if (this.bHeaderValidation) {
            checkHeaderValue(value, offset, length);
        }
        HeaderElement elem = getElement(key);
        elem.setByteArrayValue(value, offset, length);
        addHeader(elem, FILTER_YES);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#appendHeader(String, String)
     */
    @Override
    public void appendHeader(String header, String value) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided: " + header + " " + value);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "appendHeader(s,s): " + header);
        }
        if (this.bHeaderValidation) {
            if (getCharacterValidation()) //PI45266
                value = getValidatedCharacters(value); //PI57228
            else
                checkHeaderValue(value);
        }
        HeaderElement elem = getElement(findKey(header, false));
        elem.setStringValue(value);
        addHeader(elem, FILTER_YES);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#appendHeader(byte[], String)
     */
    @Override
    public void appendHeader(byte[] header, String value) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "appendHeader(b,s): " + GenericUtils.getEnglishString(header));
        }
        if (this.bHeaderValidation) {
            if (getCharacterValidation()) //PI45266
                value = getValidatedCharacters(value); //PI57228
            else
                checkHeaderValue(value);
        }
        HeaderElement elem = getElement(findKey(header, false));
        elem.setStringValue(value);
        addHeader(elem, FILTER_YES);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#appendHeader(HeaderKeys, String)
     */
    @Override
    public void appendHeader(HeaderKeys key, String value) {
        if (null == key || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "appendHeader(h,s): " + key.getName());
        }
        if (this.bHeaderValidation) {
            if (getCharacterValidation()) //PI45266
                value = getValidatedCharacters(value); //PI57228
            else
                checkHeaderValue(value);
        }
        HeaderElement elem = getElement(key);
        elem.setStringValue(value);
        addHeader(elem, FILTER_YES);
    }

    /**
     * Clear out information on this object so that it can be re-used.
     */
    public void clear() {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEntryEnabled()) {
            Tr.entry(tc, "clear");
        }

        clearAllHeaders();
        this.eohPosition = HeaderStorage.NOTSET;
        this.currentElem = null;
        this.stateOfParsing = PARSING_CRLF;
        this.binaryParsingState = GenericConstants.PARSING_HDR_FLAG;
        this.parsedToken = null;
        this.parsedTokenLength = 0;
        this.bytePosition = 0;
        this.byteLimit = 0;
        this.currentReadBB = null;
        clearBuffers();
        this.debugContext = this;
        this.numCRLFs = 0;
        this.bIsMultiLine = false;
        this.lastCRLFBufferIndex = HeaderStorage.NOTSET;
        this.lastCRLFPosition = HeaderStorage.NOTSET;
        this.lastCRLFisCR = false;
        this.headerChangeCount = 0;
        this.headerAddCount = 0;
        this.bOverChangeLimit = false;
        this.compactHeaderFlag = false;
        this.table = null;
        this.isPushPromise = false;
        this.processedXForwardedHeader = false;
        this.processedForwardedHeader = false;
        this.forwardHeaderErrorState = false;
        this.forwardedByList = null;
        this.forwardedForList = null;
        this.forwardedHost = null;
        this.forwardedPort = null;
        this.forwardedProto = null;

        if (bTrace && tc.isEntryEnabled()) {
            Tr.exit(tc, "clear");
        }
    }

    /**
     * Clear the array of buffers used during the parsing or marshalling of
     * headers.
     */
    private void clearBuffers() {
        // simply null out the parse buffers list, then release all the created buffers
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        for (int i = 0; i <= this.parseIndex; i++) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Removing reference to parse buffer: " + this.parseBuffers[i]);
            }
            this.parseBuffers[i] = null;
            this.parseBuffersStartPos[i] = HeaderStorage.NOTSET;
        }
        this.parseIndex = HeaderStorage.NOTSET;
        for (int i = 0; i <= this.createdIndex; i++) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Releasing marshall buffer: " + this.myCreatedBuffers[i]);
            }
            this.myCreatedBuffers[i].release();
            this.myCreatedBuffers[i] = null;
        }
        this.createdIndex = HeaderStorage.NOTSET;
    }

    /**
     * Print debug information on the headers to the RAS tracing log.
     */
    public void debug() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "*** Begin Header Debug ***");
            HeaderElement elem = this.hdrSequence;
            while (null != elem) {
                Tr.debug(tc, elem.getName() + ": " + elem.getDebugValue());
                elem = elem.nextSequence;
            }

            Tr.debug(tc, "*** End Header Debug ***");
        }
    }

    /**
     * Completely clear out all the information on this object when it
     * is no longer used.
     */
    protected void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroying these headers: " + this);
        }
        // if we have headers present, or reference parse buffers (i.e.
        // the first header parsed threw an error perhaps), then clear
        // the message now
        if (null != this.hdrSequence || HeaderStorage.NOTSET != this.parseIndex) {
            clear();
        }
        this.byteCacheSize = DEFAULT_CACHESIZE;
        this.incomingBufferSize = DEFAULT_BUFFERSIZE;
        this.outgoingHdrBufferSize = DEFAULT_BUFFERSIZE;
        this.useDirectBuffer = true;
        this.limitNumHeaders = DEFAULT_LIMIT_NUMHEADERS;
        this.limitTokenSize = DEFAULT_LIMIT_TOKENSIZE;
        this.headerChangeLimit = HeaderStorage.NOTSET;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.BNFHeaders#duplicate(BNFHeaders)
     */
    @Override
    public void duplicate(BNFHeaders msg) {
        duplicate((BNFHeadersImpl) msg);
    }

    /**
     * @param msg
     * @see com.ibm.wsspi.genericbnf.BNFHeaders#duplicate(BNFHeaders)
     */
    protected void duplicate(BNFHeadersImpl msg) {
        if (null == msg) {
            throw new NullPointerException("Null object passed to duplicate");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Duplicating the headers");
        }
        HeaderElement elem = this.hdrSequence;
        while (null != elem) {
            if (!elem.wasRemoved()) {
                msg.appendHeader(elem.getKey(), elem.asBytes());
            }
            elem = elem.nextSequence;
        }

        // misc settings
        msg.init(this.useDirectBuffer, this.outgoingHdrBufferSize, this.incomingBufferSize, this.byteCacheSize);
        msg.setDebugContext(this.debugContext);
        msg.setHeaderValidation(this.bHeaderValidation);
        msg.setLimitOfTokenSize(this.limitTokenSize);
        msg.setLimitOnNumberOfHeaders(this.limitNumHeaders);
    }

    /**
     * If this message was deserialized, what version was used?
     *
     * @return int
     */
    protected int getDeserializationVersion() {
        return this.deserializationVersion;
    }

    /**
     * Read the next byte[] from the input stream instance.
     *
     * @param input
     * @return byte[] -- value read, or null if length marker indicates no byte[]
     * @throws IOException
     */
    protected byte[] readByteArray(ObjectInput input) throws IOException {
        int len = input.readInt();
        if (-1 == len) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "read byte[] found -1 length marker");
            }
            return null;
        }
        byte[] value = new byte[len];
        input.readFully(value);
        return value;
    }

    /**
     * Write information for the input data to the output stream. If the input
     * data is null or empty, this will write a -1 length marker.
     *
     * @param output
     * @param data
     * @throws IOException
     */
    protected void writeByteArray(ObjectOutput output, byte[] data) throws IOException {
        if (null == data || 0 == data.length) {
            output.writeInt(-1);
        } else {
            output.writeInt(data.length);
            output.write(data);
        }
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

        // recreate the local header storage
        int len = input.readInt();
        if (SERIALIZATION_V2 == len) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Deserializing a V2 object");
            }
            this.deserializationVersion = SERIALIZATION_V2;
            len = input.readInt();
        }
        this.storage = new HashMap<Integer, HeaderElement>();

        // now read all of the headers
        int number = input.readInt();
        if (SERIALIZATION_V2 == this.deserializationVersion) {
            // this is the new format
            for (int i = 0; i < number; i++) {
                appendHeader(readByteArray(input), readByteArray(input));
            }
        } else {
            // this is the old format
            for (int i = 0; i < number; i++) {
                appendHeader((String) input.readObject(), (String) input.readObject());
            }
        }
    }

    /**
     * Write this object instance to the output stream.
     *
     * @param output
     * @throws IOException
     */
    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        output.writeInt(SERIALIZATION_V2);
        output.writeInt(this.storage.size());
        output.writeInt(this.numberOfHeaders);
        int count = 0;
        HeaderElement elem = this.hdrSequence;
        while (null != elem) {
            if (!elem.wasRemoved()) {
                count++;
                writeByteArray(output, elem.getKey().getByteArray());
                writeByteArray(output, elem.asBytes());
            }
            elem = elem.nextSequence;
        }
        // double check the counter value
        if (count != this.numberOfHeaders) {
            throw new IOException("Expected " + this.numberOfHeaders
                                  + " headers but wrote " + count);
        }
    }

    /**
     * Query whether or not the end of the headers have been parsed.
     *
     * @return boolean
     */
    public boolean isEOHFound() {
        return (HeaderStorage.NOTSET != this.eohPosition);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getAllHeaders()
     */
    @Override
    public List<HeaderField> getAllHeaders() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getAllHeaders");
        }
        List<HeaderField> vals = new ArrayList<HeaderField>();
        if (0 != this.numberOfHeaders) {
            HeaderElement elem = this.hdrSequence;
            while (null != elem) {
                if (!elem.wasRemoved()) {
                    vals.add(elem);
                }
                elem = elem.nextSequence;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getAllHeaders: size=" + vals.size());
        }
        return vals;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getAllHeaderNames()
     */
    @Override
    public List<String> getAllHeaderNames() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getAllHeaderNames");
        }
        List<String> vals;
        if (0 == this.numberOfHeaders) {
            vals = Collections.emptyList();
        } else {
            vals = new ArrayList<>(numberOfHeaders);
            HeaderElement elem = this.hdrSequence;
            while (null != elem) {
                if (!elem.wasRemoved() && !vals.contains(elem.getName())) {
                    vals.add(elem.getName());
                }
                elem = elem.nextSequence;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getAllHeaderNames: size=" + vals.size());
        }
        return vals;
    }

    /**
     * This method is the same as getAllHeaderNames, but returns a Set instead.
     * This was done in order to avoid calling contains on every header name.
     *
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getAllHeaderNamesSet()
     */
    @Override
    public Set<String> getAllHeaderNamesSet() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getAllHeaderNamesSet");
        }
        Set<String> vals;
        if (0 == this.numberOfHeaders) {
            vals = Collections.emptySet();
        } else {
            vals = new LinkedHashSet<>(numberOfHeaders);
            HeaderElement elem = this.hdrSequence;
            while (null != elem) {
                if (!elem.wasRemoved()) {
                    vals.add(elem.getName());
                }
                elem = elem.nextSequence;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getAllHeaderNamesSet: size=" + vals.size());
        }
        return vals;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getHeader(HeaderKeys)
     */
    @Override
    public HeaderField getHeader(HeaderKeys key) {
        if (null == key) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderElement elem = null;
        int ord = key.getOrdinal();
        if ((ord > maxHeadersAdded) || headersAdded.get(ord)) {
            elem = findHeader(key);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getHeader(h): " + key.getName() + " " + elem);
        }
        if (null == elem) {
            return NULL_HEADER;
        }
        return elem;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getHeader(String)
     */
    @Override
    public HeaderField getHeader(String header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, true);
        HeaderElement elem = key == null ? null : findHeader(key);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getHeader(s): " + header + " " + elem);
        }
        if (null == elem) {
            return NULL_HEADER;
        }
        return elem;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getHeader(byte[])
     */
    @Override
    public HeaderField getHeader(byte[] header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, true);
        HeaderElement elem = key == null ? null : findHeader(key);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getHeader(b): " + new String(header) + " " + elem);
        }
        if (null == elem) {
            return NULL_HEADER;
        }
        return elem;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getHeaders(byte[])
     */
    @Override
    public List<HeaderField> getHeaders(byte[] header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        List<HeaderField> list = new ArrayList<HeaderField>();
        HeaderKeys key = findKey(header, true);
        HeaderElement elem = key == null ? null : findHeader(key);
        while (null != elem) {
            if (!elem.wasRemoved()) {
                list.add(elem);
            }
            elem = elem.nextInstance;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getHeaders(b): " + new String(header) + " " + list.size());
        }
        return list;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getHeaders(HeaderKeys)
     */
    @Override
    public List<HeaderField> getHeaders(HeaderKeys key) {
        if (null == key) {
            throw new IllegalArgumentException("Null input provided");
        }
        List<HeaderField> list;
        int ord = key.getOrdinal();
        if ((ord > maxHeadersAdded) || headersAdded.get(ord)) {
            HeaderElement elem = findHeader(key);
            if (elem == null) {
                list = Collections.emptyList();
            } else if (elem.nextInstance == null) {
                list = Collections.singletonList(elem);
            } else {
                list = new ArrayList<HeaderField>();
                // The first one returned will not be marked removed.
                list.add(elem);
                elem = elem.nextInstance;
                while (null != elem) {
                    if (!elem.wasRemoved()) {
                        list.add(elem);
                    }
                    elem = elem.nextInstance;
                }
            }
        } else {
            list = Collections.emptyList();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getHeaders(h): " + key.getName() + " " + list.size());
        }
        return list;

    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getHeaders(String)
     */
    @Override
    public List<HeaderField> getHeaders(String header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        List<HeaderField> list;
        HeaderKeys key = findKey(header, true);
        HeaderElement elem = key == null ? null : findHeader(key);
        if (null == elem) {
            list = Collections.emptyList();
        } else if (elem.nextInstance == null) {
            list = Collections.singletonList(elem);
        } else {
            list = new ArrayList<HeaderField>();
            // The first one returned will not be marked removed.
            list.add(elem);
            elem = elem.nextInstance;
            while (null != elem) {
                if (!elem.wasRemoved()) {
                    list.add(elem);
                }
                elem = elem.nextInstance;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getHeaders(s): " + header + " " + list.size());
        }
        return list;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getNumberOfHeaderInstances(String)
     */
    @Override
    public int getNumberOfHeaderInstances(String header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, true);
        return key == null ? 0 : countInstances(findHeader(key));
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getNumberOfHeaderInstances(byte[])
     */
    @Override
    public int getNumberOfHeaderInstances(byte[] header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, true);
        return key == null ? 0 : countInstances(findHeader(key));
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getNumberOfHeaderInstances(HeaderKeys)
     */
    @Override
    public int getNumberOfHeaderInstances(HeaderKeys key) {
        if (null == key) {
            throw new IllegalArgumentException("Null input provided");
        }
        int instances = 0;
        int ord = key.getOrdinal();
        if ((ord > maxHeadersAdded) || headersAdded.get(ord)) {
            instances = countInstances(findHeader(key));
        }
        return instances;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#containsHeader(String)
     */
    @Override
    public boolean containsHeader(String header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, true);
        return (key != null && null != findHeader(key));
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#containsHeader(byte[])
     */
    @Override
    public boolean containsHeader(byte[] header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, true);
        return key != null && (null != findHeader(key));
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#containsHeader(HeaderKeys)
     */
    @Override
    public boolean containsHeader(HeaderKeys key) {
        if (null == key) {
            throw new IllegalArgumentException("Null input provided");
        }
        int ord = key.getOrdinal();
        if (ord <= maxHeadersAdded && !headersAdded.get(ord)) {
            return false;
        }
        return (null != findHeader(key));
    }

    /**
     * @see com.ibm.wsspi.genericbnf.BNFHeaders#marshallBinaryHeaders(WsByteBuffer[])
     */
    @Override
    public WsByteBuffer[] marshallBinaryHeaders(WsByteBuffer[] src) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "marshallBinaryHeaders");
        }

        preMarshallHeaders();
        WsByteBuffer[] buffers = src;
        if (null == buffers) {
            buffers = new WsByteBuffer[1];
            buffers[0] = allocateBuffer(this.outgoingHdrBufferSize);
            this.bytePosition = 0;
        }

        HeaderElement elem = this.hdrSequence;
        while (null != elem) {
            buffers = marshallBinaryHeader(buffers, elem);
            elem = elem.nextSequence;
        }

        buffers = putInt(GenericConstants.END_OF_HEADERS, buffers);
        buffers = flushCache(buffers);

        // flip the last buffer now that we're done
        buffers[buffers.length - 1].flip();
        postMarshallHeaders();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "marshallBinaryHeaders");
        }
        return buffers;
    }

    protected void setTable(H2HeaderTable table) {
        this.table = table;
    }

    public WsByteBuffer[] marshallHeaders(WsByteBuffer[] src, H2HeaderTable table) {
        this.table = table;
        return marshallHeaders(src);
    }

    /**
     * @throws Exception
     * @see com.ibm.wsspi.genericbnf.BNFHeaders#marshallHeaders(WsByteBuffer[])
     */
    @Override
    public WsByteBuffer[] marshallHeaders(WsByteBuffer[] src) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEntryEnabled()) {
            Tr.entry(tc, "marshallHeaders");
        }

        preMarshallHeaders();
        WsByteBuffer[] buffers = src;

        //If table is defined, this is an HTTP/2.0 connection, so skip over and to
        //iterate all elements and have them encoded.
        if (HeaderStorage.NOTSET != this.parseIndex && !overHeaderChangeLimit() && this.table == null) {
            // existing parse buffers, go into the special logic marshalling
            buffers = marshallReuseHeaders(src);
        } else {
            // otherwise go through the regular marshall logic
            if (null == buffers) {
                buffers = new WsByteBuffer[1];
                buffers[0] = allocateBuffer(this.outgoingHdrBufferSize);
                this.bytePosition = 0;
            }
            HeaderElement elem = this.hdrSequence;
            for (; null != elem; elem = elem.nextSequence) {
                //If H2HeaderTable is not null, this is an H2 connection so use encodeHeader
                //instead of marshallHeader
                if (this.table != null) {
                    try {
                        if (!H2Headers.checkIsValidH2WriteHeader(elem.getName(), elem.asString())) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "On an HTTP/2 connection - will not encode this header header: " + elem.getName());
                            }
                            // this is a connection-specific header; don't encode it
                            continue;
                        }
                        buffers = encodeHeader(buffers, elem);
                    } catch (Exception e) {
                        // Three possible scenarios -
                        // 1.) unsupported encoding used when converting string to bytes on
                        // Hpack encoding. This should never happen as it is set to always use
                        // US-ASCII.
                        // 2.) Decompression exception for invalid Hpack decode scenario
                        // Show error and return null, so caller can invalidate the table
                        // and close the stream.
                        // 3.) IOException for not being able to write into Byte Array stream
                        if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                            Tr.error(tc, e.getMessage());
                        }
                        // Release all allocated buffers of this message
                        for (WsByteBuffer buffer : buffers) {
                            buffer.release();
                            buffer = null;
                        }

                        return null;
                    }
                } else {
                    buffers = marshallHeader(buffers, elem);
                }
            }

            // only add EOL if not HTTP/2.0
            // second EOL
            if (this.table == null) {
                buffers = putBytes(BNFHeaders.EOL, buffers);
            }
            buffers = flushCache(buffers);
            // flip the last buffer now that we're done
            buffers[buffers.length - 1].flip();
        }
        postMarshallHeaders();

        if (bTrace && tc.isEntryEnabled()) {
            Tr.exit(tc, "marshallHeaders");
        }
        return buffers;
    }

    /**
     * Filter method called when a given key uses filters. Subclasses will over-
     * ride this method and handle any key specific logic necessary for the act
     * of adding this key/value.
     *
     * @param key
     * @param value
     * @return boolean (false means the key is not allowed -- incorrect value for example)
     */
    @SuppressWarnings("unused")
    protected boolean filterAdd(HeaderKeys key, byte[] value, boolean isWASPrivateHeader) {
        return true;
    }

    /**
     * Filter method called when a given key uses filters. Subclasses will over-
     * ride this method and handle any key specific logic necessary for the remove
     * action.
     *
     * @param key
     * @param value (keys may exist multiple times so value may be specific ones)
     */
    @SuppressWarnings("unused")
    protected void filterRemove(HeaderKeys key, byte[] value) {
        // nothing to do by default
    }

    /**
     * Overlay whitespace into the input buffer using the provided starting and
     * stopping positions.
     *
     * @param buffer
     * @param start
     * @param stop
     */
    private void scribbleWhiteSpace(WsByteBuffer buffer, int start, int stop) {
        if (buffer.hasArray()) {
            // buffer has a backing array so directly update that
            final byte[] data = buffer.array();
            final int offset = buffer.arrayOffset();
            int myStart = start + offset;
            int myStop = stop + offset;
            for (int i = myStart; i < myStop; i++) {
                data[i] = BNFHeaders.SPACE;
            }
        } else {
            // overlay whitespace into the buffer
            byte[] localWhitespace = whitespace;
            if (null == localWhitespace) {
                localWhitespace = getWhiteSpace();
            }
            buffer.position(start);
            int len = stop - start;
            while (len > 0) {
                if (localWhitespace.length >= len) {
                    buffer.put(localWhitespace, 0, len);
                    break; // out of while
                }
                int partial = localWhitespace.length;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Scribbling " + partial + " bytes of whitespace");
                }
                buffer.put(localWhitespace, 0, partial);
                len -= partial;
            }
        }
    }

    /**
     * Method to completely erase the input header from the parse buffers.
     *
     * @param elem
     */
    private void eraseValue(HeaderElement elem) {
        // wipe out the removed value
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Erasing existing header: " + elem.getName());
        }
        int next_index = this.lastCRLFBufferIndex;
        int next_pos = this.lastCRLFPosition;
        if (null != elem.nextSequence && !elem.nextSequence.wasAdded()) {
            next_index = elem.nextSequence.getLastCRLFBufferIndex();
            next_pos = elem.nextSequence.getLastCRLFPosition();
        }
        int start = elem.getLastCRLFPosition();
        // if it's only in one buffer, this for loop does nothing
        for (int x = elem.getLastCRLFBufferIndex(); x < next_index; x++) {
            // wiping out this buffer from start to limit
            this.parseBuffers[x].position(start);
            this.parseBuffers[x].limit(start);
            start = 0;
        }
        // last buffer, scribble from start until next_pos
        scribbleWhiteSpace(this.parseBuffers[next_index], start, next_pos);
    }

    /**
     * Utility method to overlay the input bytes into the parse buffers,
     * starting at the input index and moving forward as needed.
     *
     * @param data
     * @param inOffset
     * @param inLength
     * @param inIndex
     * @return index of last buffer updated
     */
    private int overlayBytes(byte[] data, int inOffset, int inLength, int inIndex) {
        int length = inLength;
        int offset = inOffset;
        int index = inIndex;
        WsByteBuffer buffer = this.parseBuffers[index];
        if (-1 == length) {
            length = data.length;
        }
        while (index <= this.parseIndex) {
            int remaining = buffer.remaining();
            if (remaining >= length) {
                // it all fits now
                buffer.put(data, offset, length);
                return index;
            }
            // put what we can, loop through the next buffer
            buffer.put(data, offset, remaining);
            offset += remaining;
            length -= remaining;
            buffer = this.parseBuffers[++index];
            buffer.position(0);
        }
        return index;
    }

    /**
     * Method to overlay the new header value onto the older value in the parse
     * buffers.
     *
     * @param elem
     */
    private void overlayValue(HeaderElement elem) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Overlaying existing header: " + elem.getName());
        }
        int next_index = this.lastCRLFBufferIndex;
        int next_pos = this.lastCRLFPosition;
        if (null != elem.nextSequence && !elem.nextSequence.wasAdded()) {
            next_index = elem.nextSequence.getLastCRLFBufferIndex();
            next_pos = elem.nextSequence.getLastCRLFPosition();
        }
        WsByteBuffer buffer = this.parseBuffers[elem.getLastCRLFBufferIndex()];
        buffer.position(elem.getLastCRLFPosition() + (elem.isLastCRLFaCR() ? 2 : 1));
        if (next_index == elem.getLastCRLFBufferIndex()) {
            // all in one buffer
            buffer.put(elem.getKey().getMarshalledByteArray(foundCompactHeader()));
            buffer.put(elem.asRawBytes(), elem.getOffset(), elem.getValueLength());
        } else {
            // header straddles buffers
            int index = elem.getLastCRLFBufferIndex();
            index = overlayBytes(elem.getKey().getMarshalledByteArray(foundCompactHeader()), 0, -1, index);
            index = overlayBytes(elem.asRawBytes(), elem.getOffset(), elem.getValueLength(), index);
            buffer = this.parseBuffers[index];
        }
        // pad trailing whitespace if we need it
        int start = buffer.position();
        if (start < next_pos) {
            scribbleWhiteSpace(buffer, start, next_pos);
        }
    }

    /**
     * Marshall the newly added headers from the sequence list to the output
     * buffers starting at the input index on the list.
     *
     * @param inBuffers
     * @param index
     * @return WsByteBuffer[]
     */
    private WsByteBuffer[] marshallAddedHeaders(WsByteBuffer[] inBuffers, int index) {
        WsByteBuffer[] buffers = inBuffers;
        buffers[index] = allocateBuffer(this.outgoingHdrBufferSize);
        for (HeaderElement elem = this.hdrSequence; null != elem; elem = elem.nextSequence) {
            if (elem.wasAdded()) {
                buffers = marshallHeader(buffers, elem);
            }
        }
        // add second EOL
        buffers = putBytes(BNFHeaders.EOL, buffers);
        buffers = flushCache(buffers);
        // flip the last buffer now that we're done
        buffers[buffers.length - 1].flip();
        return buffers;
    }

    /**
     * Method to marshall the current set of headers but use the existing parse
     * buffers they were originally found in. This might require deleting some
     * headers from those buffers, as well as allocating new buffers to handle
     * additional headers.
     *
     * @param inBuffers
     * @return WsByteBuffer[]
     */
    private WsByteBuffer[] marshallReuseHeaders(WsByteBuffer[] inBuffers) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        WsByteBuffer[] src = inBuffers;
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "Marshalling headers and re-using buffers, change="
                         + this.headerChangeCount + ", add=" + this.headerAddCount
                         + ", src=" + src);
        }

        HeaderElement elem = this.hdrSequence;
        WsByteBuffer[] buffers = src;
        int size = this.parseIndex + (0 < this.headerAddCount ? 2 : 1);
        int output = 0;
        int input = 0;
        if (null == src || 0 == src.length) {
            // the first line has not changed
            buffers = new WsByteBuffer[size];
        } else {
            // first line has been remarshalled. We need to update the parse
            // buffers to trim off the first line data. Dump any first line data
            // from the cache and flip the last buffer.
            src = flushCache(src);
            src[src.length - 1].flip();

            int firstHeaderBuffer = elem.getLastCRLFBufferIndex();
            for (int i = 0; i < firstHeaderBuffer; i++) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Trimming first line data from " + this.parseBuffers[i]);
                }
                this.parseBuffersStartPos[i] = this.parseBuffers[i].limit();
            }
            int firstHeaderPos = elem.getLastCRLFPosition() + (elem.isLastCRLFaCR() ? 2 : 1);
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting first buffer with headers pos to " + firstHeaderPos);
            }
            this.parseBuffersStartPos[firstHeaderBuffer] = firstHeaderPos;
            size = size - firstHeaderBuffer + src.length;
            buffers = new WsByteBuffer[size];
            System.arraycopy(src, 0, buffers, 0, src.length);
            output = src.length;
            input = firstHeaderBuffer;
        }

        // handle any changed/removed headers
        if (0 < this.headerChangeCount) {
            elem = this.hdrSequence;
            for (int i = 0; i < this.headerChangeCount && null != elem && -1 != elem.getLastCRLFBufferIndex();) {
                if (elem.wasRemoved()) {
                    eraseValue(elem);
                    i++;
                } else if (elem.wasChanged()) {
                    overlayValue(elem);
                    i++;
                }
                elem = elem.nextSequence;
            }
        }

        // copy the existing parse buffers to the output list, fixing positions
        // as we go, up until the last header buffer
        for (; input < this.parseIndex; input++, output++) {
            buffers[output] = this.parseBuffers[input];
            buffers[output].position(this.parseBuffersStartPos[input]);
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Copying existing parse buffer: " + buffers[output]);
            }
        }

        // now slice the last header buffer. If no additional headers are there,
        // then leave the double EOL, otherwise trim one of them off
        int endPos = this.eohPosition;
        if (0 < this.headerAddCount) {
            endPos = this.lastCRLFPosition + 1;
            if (this.lastCRLFisCR) {
                endPos++;
            }
        }

        WsByteBuffer buffer = this.parseBuffers[input];
        int pos = buffer.position();
        int lim = buffer.limit();
        buffer.position(this.parseBuffersStartPos[input]);
        buffer.limit(endPos);
        buffers[output] = buffer.slice();
        addToCreatedBuffer(buffers[output]);
        buffer.limit(lim);
        buffer.position(pos);
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "Sliced last header buffer: " + buffers[output]);
        }

        // check whether we need to marshall any new headers
        if (0 < this.headerAddCount) {
            buffers = marshallAddedHeaders(buffers, ++output);
        }

        return buffers;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.BNFHeaders#parseBinaryHeaders(WsByteBuffer, HeaderKeys)
     */
    @Override
    public boolean parseBinaryHeaders(WsByteBuffer buff, HeaderKeys keys) throws MalformedMessageException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsing binary headers with input buff: " + buff);
        }
        if (null == this.parsedToken) {
            createCacheToken(4);
        }

        boolean complete = false;
        int value;
        while (!complete) {
            // attempt to fill in the target token
            if (!fillCacheToken(buff)) {
                return false;
            }
            // check on the state identifier to figure out what we're parsing
            switch (this.binaryParsingState) {
                case GenericConstants.PARSING_HDR_FLAG:
                    // parsing the flag to know what type of header is coming next
                    // or to find the end-of-headers markers
                    value = GenericUtils.asInt(this.parsedToken);
                    if (GenericConstants.END_OF_HEADERS == value) {
                        complete = true;
                    } else if (GenericConstants.KNOWN_HEADER == value) {
                        this.binaryParsingState = GenericConstants.PARSING_HDR_KNOWN;
                        resetCacheToken(4);
                    } else if (GenericConstants.UNKNOWN_HEADER == value) {
                        this.binaryParsingState = GenericConstants.PARSING_HDR_NAME_LEN;
                        resetCacheToken(4);
                    }
                    break;

                case GenericConstants.PARSING_HDR_KNOWN:
                    // parsing the known header ordinal
                    HeaderKeys key = (HeaderKeys) keys.getEnumByOrdinal(GenericUtils.asInt(this.parsedToken));
                    this.currentElem = getElement(key);
                    this.binaryParsingState = GenericConstants.PARSING_HDR_VALUE_LEN;
                    resetCacheToken(4);
                    break;

                case GenericConstants.PARSING_HDR_NAME_LEN:
                    // parsing the length of the unknown header name
                    this.binaryParsingState = GenericConstants.PARSING_HDR_NAME_VALUE;
                    resetCacheToken(GenericUtils.asInt(this.parsedToken));
                    break;

                case GenericConstants.PARSING_HDR_NAME_VALUE:
                    // parse the unknown header name
                    this.currentElem = getElement(findKey(this.parsedToken, false));
                    this.binaryParsingState = GenericConstants.PARSING_HDR_VALUE_LEN;
                    resetCacheToken(4);
                    break;

                case GenericConstants.PARSING_HDR_VALUE_LEN:
                    // parse the length of the header value
                    this.binaryParsingState = GenericConstants.PARSING_HDR_VALUE;
                    resetCacheToken(GenericUtils.asInt(this.parsedToken));
                    break;

                case GenericConstants.PARSING_HDR_VALUE:
                    // parse the header value
                    setHeaderValue();
                    this.binaryParsingState = GenericConstants.PARSING_HDR_FLAG;
                    createCacheToken(4);
                    break;

                default:
                    throw new MalformedMessageException("Invalid state in headers: " + this.binaryParsingState);
            } // end of state-machine
        } // end of while (not done parsing)

        // reset the byte cache to avoid any potential problems later with
        // pointing to indirect buffer's arrays
        this.eohPosition = findCurrentBufferPosition(buff);
        buff.position(this.eohPosition);
        resetByteCache();
        clearCacheToken();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "End of binary headers at pos: " + this.eohPosition);
        }
        return true;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.BNFHeaders#parseHeaders(WsByteBuffer, boolean)
     */
    @Override
    public boolean parseHeaders(WsByteBuffer buff, boolean bExtractValue) throws MalformedMessageException {

        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsing headers with input buff: " + buff);
        }

        boolean rc = false;

        // keep going until we've found the end of headers or we need more
        // data (break out below)
        while (HeaderStorage.NOTSET == this.eohPosition) {

            // check on the state identifier to figure out what we're parsing
            switch (this.stateOfParsing) {
                case (PARSING_HEADER):
                    // we're currently parsing the header name
                    rc = parseHeaderName(buff);
                    break;
                case (PARSING_VALUE):
                    // parse the header "value" now
                    rc = (bExtractValue || this.bIsMultiLine) ? parseHeaderValueExtract(buff) : parseHeaderValueNonExtract(buff);
                    break;
                case (PARSING_CRLF):
                    // read until either we hit 2 LF chars or something else
                    rc = parseCRLFs(buff);
                    break;
                default:
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found invalid parsing ID of " + this.stateOfParsing);
                    }
                    break;
            } // end of state machine

            // if any of the above methods reported that they need more data
            // then return out to get that
            if (!rc) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Need more data");
                }
                // reset now so that entering back with more data doesn't get
                // confused with previous buffer information
                resetByteCache();
                return false;
            }

        } // end of while (not done parsing)

        // reset the parsing bytecache to avoid any potential problems with
        // indirect buffers (pointing to the backing array)
        resetByteCache();
        // reset the change count now that we've finished parsing
        this.headerChangeCount = 0;
        this.headerAddCount = 0;

        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "End of headers found at position " + this.eohPosition);
        }
        return true;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.BNFHeaders#postMarshallHeaders()
     */
    @Override
    public void postMarshallHeaders() {
        // nothing here
    }

    /**
     * @see com.ibm.wsspi.genericbnf.BNFHeaders#preMarshallHeaders()
     */
    @Override
    public void preMarshallHeaders() {
        // nothing here
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#removeAllHeaders()
     */
    @Override
    public void removeAllHeaders() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "removeAllHeaders()");
        }

        HeaderElement elem = this.hdrSequence;
        while (null != elem) {
            if (elem.getKey().useFilters()) {
                filterRemove(elem.getKey(), null);
            }
            elem.remove();
            elem = elem.nextSequence;
        }
        this.numberOfHeaders = 0;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "removeAllHeaders()");
        }
        headersAdded = new BitSet();
    }

    /**
     * Clear all traces of the headers from storage.
     *
     */
    private void clearAllHeaders() {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEntryEnabled()) {
            Tr.entry(tc, "clearAllHeaders()");
        }

        HeaderElement elem = this.hdrSequence;
        while (null != elem) {
            final HeaderElement next = elem.nextSequence;
            final HeaderKeys key = elem.getKey();
            final int ord = key.getOrdinal();
            if (storage.containsKey(ord)) {
                // first instance being removed
                if (key.useFilters()) {
                    filterRemove(key, null);
                }
                storage.remove(ord);
            }
            elem.destroy();
            elem = next;
        }
        this.hdrSequence = null;
        this.lastHdrInSequence = null;
        this.numberOfHeaders = 0;
        if (bTrace && tc.isEntryEnabled()) {
            Tr.exit(tc, "clearAllHeaders()");
        }
        headersAdded = new BitSet();
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#removeHeader(HeaderKeys)
     */
    @Override
    public void removeHeader(HeaderKeys key) {
        if (null == key) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHeader(h): " + key.getName());
        }
        removeHdrInstances(findHeader(key), FILTER_YES);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#removeHeader(String)
     */
    @Override
    public void removeHeader(String header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHeader(s): " + header);
        }
        HeaderKeys key = findKey(header, true);
        if (key != null) {
            removeHdrInstances(findHeader(key), FILTER_YES);
        }
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#removeHeader(byte[])
     */
    @Override
    public void removeHeader(byte[] header) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, true);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHeader(b): " + new String(header));
        }
        if (key != null) {
            removeHdrInstances(findHeader(key), FILTER_YES);
        }
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#removeHeader(HeaderKeys, int)
     */
    @Override
    public void removeHeader(HeaderKeys key, int instance) {
        if (null == key) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHeader(h,i): " + key.getName() + " " + instance);
        }
        removeHdr(findHeader(key, instance));
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#removeHeader(String, int)
     */
    @Override
    public void removeHeader(String header, int instance) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHeader(s,i): " + header + " " + instance);
        }
        HeaderKeys key = findKey(header, true);
        if (key != null) {
            removeHdr(findHeader(key, instance));
        }
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#removeHeader(byte[], int)
     */
    @Override
    public void removeHeader(byte[] header, int instance) {
        if (null == header) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, true);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHeader(b,i): " + new String(header) + " " + instance);
        }
        if (key != null) {
            removeHdr(findHeader(key, instance));
        }
    }

    /**
     * Remove all instances of a special header that does
     * not require the headerkey filterRemove method to be called.
     *
     * @param key
     */
    public void removeSpecialHeader(HeaderKeys key) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeSpecialHeader(h): " + key.getName());
        }
        removeHdrInstances(findHeader(key), FILTER_NO);
    }

    /**
     * Method to remove the current parsing buffer from this object's
     * ownership so it can be used by others.
     *
     * @return WsByteBuffer (null if there is no current buffer)
     */
    public WsByteBuffer returnCurrentBuffer() {
        WsByteBuffer buff = null;
        if (HeaderStorage.NOTSET != this.parseIndex) {
            buff = this.parseBuffers[this.parseIndex];
            this.parseIndex--;
        }
        return buff;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeader(String, byte[])
     */
    @Override
    public void setHeader(String header, byte[] value) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(s,b): " + header);
        }
        setHeader(findKey(header, false), value);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeader(java.lang.String, byte[], int, int)
     */
    @Override
    public void setHeader(String header, byte[] value, int offset, int length) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(s,b,i,i): " + header);
        }
        setHeader(findKey(header, false), value, offset, length);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeader(byte[], byte[])
     */
    @Override
    public void setHeader(byte[] header, byte[] value) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(b,b): " + key.getName());
        }
        setHeader(key, value);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeader(byte[], byte[], int, int)
     */
    @Override
    public void setHeader(byte[] header, byte[] value, int offset, int length) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(b,b,i,i): " + key.getName());
        }
        setHeader(key, value, offset, length);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeader(HeaderKeys, byte[])
     */
    @Override
    public void setHeader(HeaderKeys key, byte[] value) {
        if (null == key || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(h,b): " + key.getName());
        }
        if (this.bHeaderValidation) {
            checkHeaderValue(value, 0, value.length);
        }
        // check validity of the new value first
        if (key.useFilters()) {
            // if this header already exists, then wipe out existing values and
            // make sure the new one is allowed.
            HeaderElement elem = findHeader(key);
            if (null != elem) {
                filterRemove(key, null);
            }
            if (!filterAdd(key, value, false)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "New value disallowed: "
                                 + GenericUtils.getEnglishString(value));
                }
                // we can't reset every value so clean it out
                if (null != elem) {
                    removeHdrInstances(elem, FILTER_NO);
                }
                return;
            }
        }
        createSingleHeader(key, value, 0, value.length);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeader(com.ibm.wsspi.genericbnf.HeaderKeys, byte[], int, int)
     */
    @Override
    public void setHeader(HeaderKeys key, byte[] value, int offset, int length) {
        if (null == key || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(h,b,i,i): " + key.getName());
        }
        if (this.bHeaderValidation) {
            checkHeaderValue(value, offset, length);
        }
        // check validity of the new value first
        if (key.useFilters()) {
            // if this header already exists, then wipe out existing values and
            // make sure the new one is allowed.
            HeaderElement elem = findHeader(key);
            if (null != elem) {
                filterRemove(key, null);
            }
            // extract the bits we need from the larger array
            byte[] temp = new byte[length];
            System.arraycopy(value, offset, temp, 0, length);
            if (!filterAdd(key, temp, false)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "New value disallowed: "
                                 + GenericUtils.getEnglishString(temp));
                }
                // we can't reset every value so clean it out
                if (null != elem) {
                    removeHdrInstances(elem, FILTER_NO);
                }
                return;
            }
        }
        createSingleHeader(key, value, offset, length);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeader(HeaderKeys, String)
     */
    @Override
    public void setHeader(HeaderKeys key, String value) {
        if (null == key || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(h,s): " + key.getName());
        }
        if (this.bHeaderValidation) {
            if (getCharacterValidation()) //PI45266
                value = getValidatedCharacters(value); //PI57228
            else
                checkHeaderValue(value);
        }
        // check validity of the new value first
        if (key.useFilters()) {
            // if this header already exists, then wipe out existing values and
            // make sure the new one is allowed.
            HeaderElement elem = findHeader(key);
            if (null != elem) {
                filterRemove(key, null);
            }
            if (!filterAdd(key, GenericUtils.getEnglishBytes(value), false)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "New value disallowed: " + value);
                }
                // we can't reset every value so clean it out
                if (null != elem) {
                    removeHdrInstances(elem, FILTER_NO);
                }
                return;
            }
        }
        HeaderElement elem = findHeader(key);
        if (null != elem) {
            // delete all secondary instances first
            if (null != elem.nextInstance) {
                HeaderElement temp = elem.nextInstance;
                while (null != temp) {
                    temp.remove();
                    temp = temp.nextInstance;
                }
            }
            if (HeaderStorage.NOTSET != this.headerChangeLimit) {
                // parse buffer reuse is enabled, see if we can use existing obj
                if (value.length() <= elem.getValueLength()) {
                    this.headerChangeCount++;
                    elem.setStringValue(value);
                } else {
                    elem.remove();
                    elem = null;
                }
            } else {
                // parse buffer reuse is disabled
                elem.setStringValue(value);
            }
        }
        if (null == elem) {
            // either it didn't exist or we chose not to re-use the object
            elem = getElement(key);
            elem.setStringValue(value);
            addHeader(elem, FILTER_NO);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Replacing header " + key.getName() + " [" + elem.getDebugValue() + "]");
        }
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeaderIfAbsent(HeaderKeys, String)
     */
    @Override
    public HeaderField setHeaderIfAbsent(HeaderKeys key, String value) {
        if (null == key || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeaderIfAbsent(h,s): " + key.getName());
        }

        // if this header already exists with a value that is not null, then return it.
        HeaderElement elem = findHeader(key);
        if (elem != null && elem.asString() != null) {
            return elem;
        }
        if (this.bHeaderValidation) {
            if (getCharacterValidation()) //PI45266
                value = getValidatedCharacters(value); //PI57228
            else
                checkHeaderValue(value);
        }
        // check validity of the new value first
        if (key.useFilters()) {
            // if this header already exists, then wipe out existing values and
            // make sure the new one is allowed.
            if (null != elem) {
                filterRemove(key, null);
            }
            if (!filterAdd(key, GenericUtils.getEnglishBytes(value), false)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "New value disallowed: " + value);
                }
                // we can't reset every value so clean it out
                if (null != elem) {
                    removeHdrInstances(elem, FILTER_NO);
                }
                return null;
            }
            if (null != elem) {
                elem = findHeader(key);
            }
        }
        if (null != elem) {
            // delete all secondary instances first
            if (null != elem.nextInstance) {
                HeaderElement temp = elem.nextInstance;
                while (null != temp) {
                    temp.remove();
                    temp = temp.nextInstance;
                }
            }
            if (HeaderStorage.NOTSET != this.headerChangeLimit) {
                // parse buffer reuse is enabled, see if we can use existing obj
                if (value.length() <= elem.getValueLength()) {
                    this.headerChangeCount++;
                    elem.setStringValue(value);
                } else {
                    elem.remove();
                    elem = null;
                }
            } else {
                // parse buffer reuse is disabled
                elem.setStringValue(value);
            }
        }
        if (null == elem) {
            // either it didn't exist or we chose not to re-use the object
            elem = getElement(key);
            elem.setStringValue(value);
            addHeader(elem, FILTER_NO);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Replacing header " + key.getName() + " [" + elem.getDebugValue() + "]");
        }
        return null;
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeader(String, String)
     */
    @Override
    public void setHeader(String header, String value) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(s,s): " + header);
        }
        setHeader(findKey(header, false), value);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeader(byte[], String)
     */
    @Override
    public void setHeader(byte[] header, String value) {
        if (null == header || null == value) {
            throw new IllegalArgumentException("Null input provided");
        }
        HeaderKeys key = findKey(header, false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(b,s): " + key.getName());
        }
        setHeader(key, value);
    }

    /**
     * Utility method to create a single header instance with the given
     * information. If elements already exist, this will delete secondary
     * ones and overlay the value on the first element.
     *
     * @param key
     * @param value
     * @param offset
     * @param length
     */
    private void createSingleHeader(HeaderKeys key, byte[] value, int offset, int length) {
        HeaderElement elem = findHeader(key);
        if (null != elem) {
            // delete all secondary instances first
            if (null != elem.nextInstance) {
                HeaderElement temp = elem.nextInstance;
                while (null != temp) {
                    temp.remove();
                    temp = temp.nextInstance;
                }
            }
            if (HeaderStorage.NOTSET != this.headerChangeLimit) {
                // parse buffer reuse is enabled, see if we can use existing obj
                if (length <= elem.getValueLength()) {
                    this.headerChangeCount++;
                    elem.setByteArrayValue(value, offset, length);
                } else {
                    elem.remove();
                    elem = null;
                }
            } else {
                // parse buffer reuse is disabled
                elem.setByteArrayValue(value, offset, length);
            }
        }
        if (null == elem) {
            // either it didn't exist or we chose not to re-use the object
            elem = getElement(key);
            elem.setByteArrayValue(value, offset, length);
            addHeader(elem, FILTER_NO);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Replacing header " + key.getName() + " [" + elem.getDebugValue() + "]");
        }
    }

    /**
     * Add this new instance of a header to storage.
     *
     * @param elem
     * @param bFilter - call filter on add?
     */
    private void addHeader(HeaderElement elem, boolean bFilter) {
        final HeaderKeys key = elem.getKey();
        final String name = elem.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Adding header [" + name
                         + "] with value [" + elem.getDebugValue() + "]");
        }

        if (getRemoteIp() && !forwardHeaderErrorState) {
            String lowerCaseName = name.toLowerCase();
            if (lowerCaseName.startsWith("x-forwarded")) {
                processForwardedHeader(elem, true);
            } else if (lowerCaseName.startsWith("forwarded")) {
                processForwardedHeader(elem, false);
            }
        }

        if (bFilter) {
            if (key.useFilters() && !filterAdd(key, elem.asBytes(), false)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "filter disallowed: " + elem.getDebugValue());
                }
                return;
            }
        }
        if (HttpHeaderKeys.isWasPrivateHeader(name)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "checking to see if private header is allowed: " + name);
            }
            if (!filterAdd(key, elem.asBytes(), true)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, name + " is not trusted for this host; not adding header");
                }
                return;
            }
        }

        incrementHeaderCounter();
        HeaderElement root = findHeader(key);
        boolean rc = addInstanceOfElement(root, elem);
        // did we change the root node?
        if (rc) {
            final int ord = key.getOrdinal();
            storage.put(ord, elem);
            if (ord <= maxHeadersAdded)
                headersAdded.set(ord);
        }
    }

    /**
     * Get an empty object for the new header name/value instance.
     *
     * @param key
     * @return HeaderElement
     */
    private HeaderElement getElement(HeaderKeys key) {
        HeaderElement elem = this.headerElements;
        if (null != elem) {
            // disconnect it from the rest of the free list
            this.headerElements = elem.nextInstance;
            elem.nextInstance = null;
            elem.init(key);
        } else {
            elem = new HeaderElement(key, this);
        }
        return elem;
    }

    /**
     * Return an element object back to the free list.
     *
     * @param elem
     */
    protected void freeElement(HeaderElement elem) {
        elem.nextInstance = this.headerElements;
        this.headerElements = elem;
    }

    /**
     * Subclasses will provide the match of the input name against a defined key.
     * This must return a non-null HeaderKeys object.
     *
     * @param name
     * @return HeaderKeys
     */
    protected abstract HeaderKeys findKey(String name, boolean returnNullForInvalidName);

    /**
     * Subclasses will provide the match of the input name against a defined key.
     * This must return a non-null HeaderKeys object.
     *
     * @param name
     * @return HeaderKeys
     */
    protected abstract HeaderKeys findKey(byte[] name, boolean returnNullForInvalidName);

    /**
     * Subclasses will provide the match of the input name against a defined key.
     * This must return a non-null HeaderKeys object.
     *
     * @param data
     * @param offset - starting point in the data
     * @param length - length from that offset
     * @return HeaderKeys
     */
    protected abstract HeaderKeys findKey(byte[] data, int offset, int length, boolean returnNullForInvalidName);

    /**
     * Find the specific instance of this header in storage.
     *
     * @param key
     * @param instance
     * @return HeaderElement
     */
    private HeaderElement findHeader(HeaderKeys key, int instance) {
        final int ord = key.getOrdinal();

        HeaderElement elem = null;

        if (ord <= HttpHeaderKeys.ORD_MAX) {
            elem = storage.get(ord);
        } else {
            //If the ordinal created for this key is larger than 1024, the header key
            //storage has been capped. As such, search the internal header storage
            //to see if we have a header with this name already added.
            HeaderElement headerCand = storage.get(ord);
            // check to see if the ordinal matches and skip the loop below.
            if (headerCand != null && headerCand.getKey().getName().equalsIgnoreCase(key.getName())) {
                elem = headerCand;
            } else {
                for (HeaderElement header : storage.values()) {
                    if (header.getKey().getName().equalsIgnoreCase(key.getName())) {
                        elem = header;
                        break;
                    }
                }
            }
        }

        int i = -1;
        while (null != elem) {
            if (!elem.wasRemoved()) {
                if (++i == instance) {
                    return elem;
                }
            }
            elem = elem.nextInstance;
        }
        return null;
    }

    /**
     * Find the first instance of this header in storage.
     *
     * @param key
     * @return HeaderElement
     */
    private HeaderElement findHeader(HeaderKeys key) {
        final int ord = key.getOrdinal();

        HeaderElement elem = null;

        if (ord <= HttpHeaderKeys.ORD_MAX) {
            elem = storage.get(ord);
        } else {
            //If the ordinal created for this key is larger than 1024, the header key
            //storage has been capped. As such, search the internal header storage
            //to see if we have a header with this name already added.
            HeaderElement headerCand = storage.get(ord);
            // check to see if the ordinal matches and skip the loop below.
            if (headerCand != null && headerCand.getKey().getName().equalsIgnoreCase(key.getName())) {
                elem = headerCand;
            } else {
                for (HeaderElement header : storage.values()) {
                    if (header.getKey().getName().equalsIgnoreCase(key.getName())) {
                        elem = header;
                        break;
                    }
                }
            }
        }

        while (null != elem && elem.wasRemoved()) {
            elem = elem.nextInstance;
        }
        return elem;
    }

    /**
     * Remove this single instance of a header.
     *
     * @param elem
     */
    private void removeHdr(HeaderElement elem) {
        if (null == elem) {
            return;
        }
        HeaderKeys key = elem.getKey();
        elem.remove();
        if (key.useFilters()) {
            filterRemove(key, elem.asBytes());
        }
    }

    /**
     * Remove all instances of this header.
     *
     * @param root
     * @param bFilter
     */
    private void removeHdrInstances(HeaderElement root, boolean bFilter) {
        if (null == root) {
            return;
        }
        HeaderKeys key = root.getKey();
        if (bFilter && key.useFilters()) {
            filterRemove(key, null);
        }
        HeaderElement elem = root;
        while (null != elem) {
            elem.remove();
            elem = elem.nextInstance;
        }
    }

    /**
     * Set one of the special headers that does not require the headerkey
     * filterX methods to be called.
     *
     * @param key
     * @param value
     */
    protected void setSpecialHeader(HeaderKeys key, byte[] value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setSpecialHeader(h,b[]): " + key.getName());
        }
        removeHdrInstances(findHeader(key), FILTER_NO);
        HeaderElement elem = getElement(key);
        elem.setByteArrayValue(value);
        addHeader(elem, FILTER_NO);
    }

    /**
     * Special header set method used by subclasses to avoid the use of the
     * filterX methods.
     *
     * @param key
     * @param value
     */
    public void setSpecialHeader(HeaderKeys key, String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setSpecialHeader(h,s): " + key.getName());
        }
        removeHdrInstances(findHeader(key), FILTER_NO);
        HeaderElement elem = getElement(key);
        elem.setStringValue(value);
        addHeader(elem, FILTER_NO);
    }

    /**
     * Query whether the current headers are over the allowed number of changes.
     *
     * @return boolean
     */
    protected boolean overHeaderChangeLimit() {
        // if we've already figured out it's over the limit or something else
        // forces the remarshalling behavior then send that back now
        if (this.bOverChangeLimit || -1 == this.parseIndex) {
            return true;
        }
        this.bOverChangeLimit = (this.headerChangeCount >= this.headerChangeLimit);
        return this.bOverChangeLimit;
    }

    /**
     * Set the limit on the number of allowed header changes before this message
     * must be remarshalled.
     *
     * @param limit
     */
    public void setHeaderChangeLimit(int limit) {
        this.headerChangeLimit = limit;
        this.bOverChangeLimit = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Setting header change limit to " + limit);
        }
    }

    /**
     * Query the currently configured header change limit.
     *
     * @return int
     */
    public int getHeaderChangeLimit() {
        return this.headerChangeLimit;
    }

    /**
     * Method to marshall all instances of a particular header into the
     * input buffers (expanding them if need be).
     *
     * @param inBuffers
     * @param elem
     * @return WsByteBuffer[]
     */
    protected WsByteBuffer[] marshallHeader(WsByteBuffer[] inBuffers, HeaderElement elem) {
        if (elem.wasRemoved()) {
            return inBuffers;
        }
        WsByteBuffer[] buffers = inBuffers;
        final byte[] value = elem.asRawBytes();
        if (null != value) {
            buffers = putBytes(elem.getKey().getMarshalledByteArray(foundCompactHeader()), buffers);
            buffers = putBytes(value, elem.getOffset(), elem.getValueLength(), buffers);
            buffers = putBytes(BNFHeaders.EOL, buffers);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Marshalling: " + elem.getKey()
                             + " [" + elem.getDebugValue() + "]");
            }
        }

        return buffers;
    }

    protected WsByteBuffer[] encodeHeader(WsByteBuffer[] inBuffers, HeaderElement elem) throws CompressionException, IOException {

        if (elem.wasRemoved()) {
            return inBuffers;
        }
        WsByteBuffer[] buffers = inBuffers;
        final String name = elem.getKey().getName();
        final String value = elem.asString();
        LiteralIndexType indexType = LiteralIndexType.NOINDEXING;
        //For the time being, there will be no indexing on the responses to guarantee
        //the write context is concurrent to the remote endpoint's read context. Remote
        //intermediaries could index if they so desire, so setting NoIndexing (as
        //opposed to NeverIndexing).
        //TODO: investigate how streams and priority can work together with indexing on
        //responses.
        //LiteralIndexType indexType = isPushPromise ? LiteralIndexType.NOINDEXING : LiteralIndexType.INDEX;

        if (null != value) {
            buffers = putBytes(H2Headers.encodeHeader(table, name, value, indexType), buffers);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Encoding: " + elem.getKey()
                         + " [" + elem.getDebugValue() + "]");
        }
        return buffers;
    }

    /**
     * Method to marshall a header out in binary mode into the input
     * buffers (expanding them if necessary).
     *
     * @param inBuffers
     * @param elem
     * @return WsByteBuffer[]
     */
    protected WsByteBuffer[] marshallBinaryHeader(WsByteBuffer[] inBuffers, HeaderElement elem) {
        if (elem.wasRemoved()) {
            return inBuffers;
        }
        WsByteBuffer[] buffers = inBuffers;
        final byte[] value = elem.asRawBytes();
        if (null != value) {
            HeaderKeys key = elem.getKey();
            if (!key.isUndefined()) {
                buffers = putInt(GenericConstants.KNOWN_HEADER, buffers);
                buffers = putInt(elem.getKey().getOrdinal(), buffers);
            } else {
                buffers = putInt(GenericConstants.UNKNOWN_HEADER, buffers);
                buffers = putInt(key.getByteArray().length, buffers);
                buffers = putBytes(key.getByteArray(), buffers);
            }
            buffers = putInt(elem.getValueLength(), buffers);
            buffers = putBytes(value, elem.getOffset(), elem.getValueLength(), buffers);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Marshalling: " + elem.getName()
                             + " [" + elem.getDebugValue() + "]");
            }
        }
        return buffers;
    }

    // ***********************************************************************
    // General internal methods
    // ***********************************************************************

    /**
     * Query what the current binary parsing state is set to.
     *
     * @return int
     */
    final protected int getBinaryParseState() {
        return this.binaryParsingState;
    }

    /**
     * Set the binary parsing state to the input value.
     *
     * @param state
     */
    final protected void setBinaryParseState(int state) {
        this.binaryParsingState = state;
    }

    /**
     * Allocate a buffer according to the requested input size.
     *
     * @param size
     * @return WsByteBuffer
     */
    public WsByteBuffer allocateBuffer(int size) {
        WsByteBufferPoolManager mgr = HttpDispatcher.getBufferManager();
        WsByteBuffer wsbb = (this.useDirectBuffer) ? mgr.allocateDirect(size) : mgr.allocate(size);
        addToCreatedBuffer(wsbb);
        return wsbb;
    }

    /**
     * Get access to the last buffer used while parsing headers. If the end of
     * the headers has been reached, then this is the last buffer and might
     * contain body data.
     * <p>
     * Possibly null, depending on the situation.
     *
     * @return WsByteBuffer
     */
    final public WsByteBuffer getCurrentBuffer() {
        return this.currentReadBB;
    }

    /**
     * Set the current parsing buffer to the input buffer.
     *
     * @param b
     */
    final public void setCurrentBuffer(WsByteBuffer b) {
        this.currentReadBB = b;
    }

    /**
     * Allow the debug context object to be set to the input Object for more
     * specialized debugging. A null input object will be ignored.
     *
     * @param o
     */
    @Override
    public void setDebugContext(Object o) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "debugContext set to " + o + " for " + this);
        }
        if (null != o) {
            this.debugContext = o;
        }
    }

    /**
     * Query the current debug context object for this message.
     *
     * @return Object
     */
    final protected Object getDebugContext() {
        return this.debugContext;
    }

    /**
     * Query what the current incoming buffer size is for this message.
     *
     * @return int
     */
    final protected int getIncomingBufferSize() {
        return this.incomingBufferSize;
    }

    /**
     * Query the intended size of buffers to use when marshalling outgoing
     * headers.
     *
     * @return int
     */
    final protected int getOutgoingBufferSize() {
        return this.outgoingHdrBufferSize;
    }

    /**
     * Query whether allocation should be for direct buffers or not.
     *
     * @return boolean
     */
    final protected boolean shouldAllocateDirectBuffer() {
        return this.useDirectBuffer;
    }

    /**
     * Set the temporary parsed token variable to the input value.
     *
     * @param token
     */
    final protected void setParsedToken(byte[] token) {
        this.parsedToken = token;
    }

    /**
     * Query what the current parsed token variable is.
     *
     * @return byte[]
     */
    final protected byte[] getParsedToken() {
        return this.parsedToken;
    }

    /**
     * Query the size of the byte cache.
     *
     * @return int
     */
    final protected int getByteCacheSize() {
        return this.byteCacheSize;
    }

    /**
     * Query the entry from the "parsed header" buffer list at the
     * given index.
     *
     * @param index
     * @return WsByteBuffer (null if index is invalid)
     */
    protected WsByteBuffer getParseBuffer(int index) {
        if (0 > index || index >= this.parseIndex) {
            return null;
        }
        return this.parseBuffers[index];
    }

    /**
     * Query the current index of list that keeps track of the buffers allocated
     * by this message.
     *
     * @return int (-1 if no buffers yet)
     */
    final public int getBuffersIndex() {
        return this.parseIndex;
    }

    /*
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setLimitOfTokenSize(int)
     */
    @Override
    public void setLimitOfTokenSize(int size) {
        if (0 >= size) {
            throw new IllegalArgumentException("Invalid limit on token size: " + size);
        }
        this.limitTokenSize = size;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Limit on token size now: " + this.limitTokenSize);
        }
    }

    /*
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getLimitOfTokenSize()
     */
    @Override
    public int getLimitOfTokenSize() {
        return this.limitTokenSize;
    }

    /**
     * Query the number of headers in storage at this moment in time.
     *
     * @return int
     */
    public int getNumberOfHeaders() {
        return this.numberOfHeaders;
    }

    /**
     * Increment the number of headers in storage counter by one. If this puts
     * it over the limit for the message, then an exception is thrown.
     *
     * @throws IllegalArgumentException if there are now too many headers
     */
    private void incrementHeaderCounter() {
        this.numberOfHeaders++;
        this.headerAddCount++;
        if (this.limitNumHeaders < this.numberOfHeaders) {
            String msg = "Too many headers in storage: " + this.numberOfHeaders;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, msg);
            }
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Decrement the number of headers in storage counter by one. If the number
     * goes negative, then reset it back to 0.
     */
    protected void decrementHeaderCounter() {
        this.numberOfHeaders--;
        this.headerChangeCount++;
    }

    /*
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setLimitOnNumberOfHeaders(int)
     */
    @Override
    public void setLimitOnNumberOfHeaders(int size) {
        if (0 >= size) {
            throw new IllegalArgumentException("Invalid limit on number headers: " + size);
        }
        this.limitNumHeaders = size;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Limit on number of headers now: " + this.limitNumHeaders);
        }
    }

    /*
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#getLimitOnNumberOfHeaders()
     */
    @Override
    public int getLimitOnNumberOfHeaders() {
        return this.limitNumHeaders;
    }

    /**
     * Set the header validation option to the input flag.
     *
     * @param flag
     */
    final protected void setHeaderValidation(boolean flag) {
        this.bHeaderValidation = flag;
    }

    /**
     * Check the input header value for validity, starting at the offset and
     * continuing for the input length of characters.
     *
     * @param data
     * @param offset
     * @param length
     */
    private void checkHeaderValue(byte[] data, int offset, int length) {
        // if the last character is a CR or LF, then this fails
        int index = (offset + length) - 1;
        if (index < 0) {
            // empty data, quit now with success
            return;
        }
        String error = null;
        if (BNFHeaders.LF == data[index] || BNFHeaders.CR == data[index]) {
            error = "Illegal trailing EOL";
        }

        // scan through the data now for invalid CRLF presence. Note that CRLFs
        // may be followed by whitespace for valid multiline headers.
        for (int i = offset; null == error && i < index; i++) {
            if (BNFHeaders.CR == data[i]) {
                // next char must be an LF
                if (BNFHeaders.LF != data[i + 1]) {
                    error = "Invalid CR not followed by LF";
                } else if (getCharacterValidation()) {
                    data[i] = BNFHeaders.SPACE;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found a CR replacing it with a SP");
                    }
                }
            } else if (BNFHeaders.LF == data[i]) {
                // if it is not followed by whitespace then this value is bad
                if (BNFHeaders.TAB != data[i + 1] && BNFHeaders.SPACE != data[i + 1]) {
                    error = "Invalid LF not followed by whitespace";
                } else if (getCharacterValidation()) {
                    data[i] = BNFHeaders.SPACE;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found a LF replacing it with a SP");
                    }
                }
            }
        }

        // if we found an error, throw the exception now
        if (null != error) {
            IllegalArgumentException iae = new IllegalArgumentException(error);
            FFDCFilter.processException(iae, getClass().getName() + ".checkHeaderValue(byte[])", "1", this);
            throw iae;
        }
    }

    public static void setCharacterValidation(boolean value) { //PI45266
        bCharacterValidation = value;
    }

    public boolean getCharacterValidation() { //PI45266
        return bCharacterValidation;
    }

    private String getValidatedCharacters(String data) { //PI57228
        if (isGoodCharacters(data))
            return data;
        else
            return checkHeaderCharacters(data);
    }

    public static void setRemoteIp(boolean value) {
        bRemoteIp = value;
    }

    public boolean getRemoteIp() {
        return bRemoteIp;
    }

    /**
     * Check the input header value for CRLF and non ascii char that can result in crlfs.
     * checkHeaderCharacters
     *
     * @param data
     * @exception IllegalArgumentException if invalid
     * @return boolean
     */
    private boolean isGoodCharacters(String data) { //PI57228
        // if the last character is a CR or LF, then this fails
        int index = data.length() - 1;
        if (index < 0) {
            // empty string, quit now with success
            return true;
        }
        char c = data.charAt(index);
        if (BNFHeaders.LF == c || BNFHeaders.CR == c) {
            IllegalArgumentException iae = new IllegalArgumentException("Illegal trailing EOL");
            FFDCFilter.processException(iae, getClass().getName() + ".isGoodCharacters(String)", "1", this);
            throw iae;
        }

        // scan through the data now for non ascii characters.
        for (int i = 0; i <= index; i++) {
            c = data.charAt(i);
            // if is not a good Character
            if (c < 32 || c >= 127) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (c == BNFHeaders.LF || c == BNFHeaders.CR) {
                        Tr.debug(tc, "Found a CR or LF");
                    } else {
                        Tr.debug(tc, "The Character: " + c + " is not printable");
                        final int maskedCodePoint = c & 0xFF;
                        if (maskedCodePoint == BNFHeaders.LF || maskedCodePoint == BNFHeaders.CR) {
                            Tr.debug(tc, "Character: " + c + " unicode ends with a 0a or 0d");
                            Tr.debug(tc, "The Unicode is: " + (char) maskedCodePoint);
                        }
                    }
                }
                return false;
            }
        }

        return true; //If we get here without returning it means all characters are good.
    }

    /**
     * Check the input header value for CRLF and non ascii char that can retult in crlfs.
     * checkHeaderCharacters
     *
     * @param data
     * @exception IllegalArgumentException if invalid
     * @return String
     */
    private String checkHeaderCharacters(String data) { //PI45266
        // if the last character is a CR or LF, then this fails
        int index = data.length() - 1;
        if (index < 0) {
            // empty string, quit now with success
            return data;
        }
        String error = null;
        char c = data.charAt(index);
        if (BNFHeaders.LF == c || BNFHeaders.CR == c) {
            error = "Illegal trailing EOL";
        }

        // scan through the data now for invalid CRLF presence. Note that CRLFs
        // may be followed by whitespace for valid multiline headers.
        StringBuilder sb = new StringBuilder(index + 1); //PI57228
        for (int i = 0; null == error && i <= index; i++) {
            c = data.charAt(i);
            if (i < index) {
                if (BNFHeaders.CR == c) {
                    // next char must be an LF
                    if (BNFHeaders.LF != data.charAt(i + 1)) {
                        error = "Invalid CR not followed by LF";
                    }
                } else if (BNFHeaders.LF == c) {
                    char x = data.charAt(i + 1);
                    // if it is not followed by whitespace then this value is bad
                    if (BNFHeaders.TAB != x && BNFHeaders.SPACE != x) {
                        error = "Invalid LF not followed by whitespace";
                    }
                }
            }
            if (c >= 32 && c < 127) {
                sb.append(c);
            } else if (c == BNFHeaders.LF || c == BNFHeaders.CR) {
                sb.append(' ');
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found a CR or LF, replacing it with SP");
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The Character: " + c + " is not printable");
                }
                final int maskedCodePoint = c & 0xFF;
                if (maskedCodePoint == BNFHeaders.LF || maskedCodePoint == BNFHeaders.CR) {
                    sb.append('?');
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Character: " + c + " unicode ends with a 0a or 0d, replacing it with ?");
                        Tr.debug(tc, "The Unicode is: " + (char) maskedCodePoint);
                    }
                } else
                    sb.append(c);
            }
        }

        // if we found an error, throw the exception now
        if (null != error) {
            IllegalArgumentException iae = new IllegalArgumentException(error);
            FFDCFilter.processException(iae, getClass().getName() + ".checkHeaderValue(String)", "1", this);
            throw iae;
        }
        return sb.toString();
    }

    /**
     * Check the input header value for validity.
     *
     * @param data
     * @exception IllegalArgumentException if invalid
     */
    private void checkHeaderValue(String data) {
        // if the last character is a CR or LF, then this fails
        int index = data.length() - 1;
        if (index < 0) {
            // empty string, quit now with success
            return;
        }
        String error = null;
        char c = data.charAt(index);
        if (BNFHeaders.LF == c || BNFHeaders.CR == c) {
            error = "Illegal trailing EOL";
        }

        // scan through the data now for invalid CRLF presence. Note that CRLFs
        // may be followed by whitespace for valid multiline headers.
        for (int i = 0; null == error && i < index; i++) {
            c = data.charAt(i);
            if (BNFHeaders.CR == c) {
                // next char must be an LF
                if (BNFHeaders.LF != data.charAt(i + 1)) {
                    error = "Invalid CR not followed by LF";
                }
            } else if (BNFHeaders.LF == c) {
                c = data.charAt(++i);
                // if it is not followed by whitespace then this value is bad
                if (BNFHeaders.TAB != c && BNFHeaders.SPACE != c) {
                    error = "Invalid LF not followed by whitespace";
                }
            }
        }

        // if we found an error, throw the exception now
        if (null != error) {
            IllegalArgumentException iae = new IllegalArgumentException(error);
            FFDCFilter.processException(iae, getClass().getName() + ".checkHeaderValue(String)", "1", this);
            throw iae;
        }
    }

    /**
     * Count the number of instances of this header starting at the given
     * element.
     *
     * @param root
     * @return int
     */
    private int countInstances(HeaderElement root) {
        int count = 0;
        HeaderElement elem = root;
        while (null != elem) {
            if (!elem.wasRemoved()) {
                count++;
            }
            elem = elem.nextInstance;
        }
        return count;
    }

    /**
     * Skip any whitespace that might be at the start of this buffer.
     *
     * @param buff
     * @return boolean (true if found non whitespace, false if end
     *         of buffer found)
     */
    private boolean skipWhiteSpace(WsByteBuffer buff) {
        // keep reading until we hit the end of the buffer or a non-space char
        byte b;
        do {
            if (this.bytePosition >= this.byteLimit) {
                if (!fillByteCache(buff)) {
                    // not filled
                    return false;
                }
            }
            b = this.byteCache[this.bytePosition++];
        } while (BNFHeaders.SPACE == b || BNFHeaders.TAB == b);

        // move byte position back one.
        this.bytePosition--;
        return true;
    }

    // **********************************************************************
    // Internal linked-list methods
    // **********************************************************************

    /**
     * Helper method to add a new instance of a HeaderElement to
     * root's internal list. This might be the first instance, or an
     * additional instance in which case it will be added at the end
     * of the list.
     *
     * @param root
     * @param elem
     * @return boolean
     */
    private boolean addInstanceOfElement(HeaderElement root, HeaderElement elem) {
        // first add to the overall sequence list
        if (null == this.hdrSequence) {
            this.hdrSequence = elem;
            this.lastHdrInSequence = elem;
        } else {
            // find the end of the list and append this new element
            this.lastHdrInSequence.nextSequence = elem;
            elem.prevSequence = this.lastHdrInSequence;
            this.lastHdrInSequence = elem;
        }
        if (null == root) {
            return true;
        }
        HeaderElement prev = root;
        while (null != prev.nextInstance) {
            prev = prev.nextInstance;
        }
        prev.nextInstance = elem;
        return false;
    }

    /**
     * Place the input int value into the outgoing cache. This will return
     * the buffer array as it may have changed if the cache need to be flushed.
     *
     * @param data
     * @param buffers
     * @return WsByteBuffer[]
     */
    protected WsByteBuffer[] putInt(int data, WsByteBuffer[] buffers) {
        return putBytes(GenericUtils.asBytes(data), buffers);
    }

    /**
     * Place the input information into the outgoing cache. If the cache is
     * full, then it will be flushed out into the input buffers. The list of
     * buffers is returned back to the caller as they may have been changed
     * (extended) when the cache is flushed.
     *
     * @param data
     * @param inBuffers
     * @return WsByteBuffer[]
     */
    protected WsByteBuffer[] putByte(byte data, WsByteBuffer[] inBuffers) {
        WsByteBuffer[] buffers = inBuffers;
        this.byteCache[this.bytePosition] = data;
        this.bytePosition++;

        if (this.bytePosition >= this.byteCacheSize) {
            // full cache, flush it
            buffers = flushFullCache(buffers);
        }
        return buffers;
    }

    /**
     * Place the input information into the outgoing cache. If the cache is
     * full, then it will be flushed out into the input buffers. The list of
     * buffers is returned back to the caller as they may have been changed
     * (extended) when the cache was flushed.
     *
     * @param data
     * @param inBuffers
     * @return WsByteBuffer[]
     */
    protected WsByteBuffer[] putBytes(byte[] data, WsByteBuffer[] inBuffers) {
        WsByteBuffer[] buffers = inBuffers;
        int space_left = this.byteCacheSize - this.bytePosition;
        if (data.length <= space_left) {
            // put it into the byte cache
            System.arraycopy(data, 0, this.byteCache, this.bytePosition, data.length);
            this.bytePosition += data.length;
        } else {
            // doesn't fit entirely, empty the cache and then the data to buffer
            buffers = flushCache(buffers);
            return GenericUtils.putByteArray(buffers, data, 0, data.length, this);
        }

        if (this.bytePosition == this.byteCacheSize) {
            // full cache, flush it
            buffers = flushFullCache(buffers);
        }
        return buffers;
    }

    /**
     * Place the input information into the outgoing cache. If the cache is
     * full, then it will be flushed out into the input buffers. The list of
     * buffers is returned back to the caller as they may have been changed
     * (extended) when the cache was flushed.
     *
     * @param data
     * @param offset    (into data to start at)
     * @param length    (to copy from the offset into data)
     * @param inBuffers
     * @return WsByteBuffer[]
     */
    protected WsByteBuffer[] putBytes(byte[] data, int offset, int length, WsByteBuffer[] inBuffers) {
        WsByteBuffer[] buffers = inBuffers;
        int space_left = this.byteCacheSize - this.bytePosition;
        if (length <= space_left) {
            // put it into the byte cache
            System.arraycopy(data, offset, this.byteCache, this.bytePosition, length);
            this.bytePosition += length;
        } else {
            // doesn't fit entirely, empty the cache and then the data to buffer
            buffers = flushCache(buffers);
            return GenericUtils.putByteArray(buffers, data, offset, length, this);
        }

        if (this.bytePosition == this.byteCacheSize) {
            // full cache, flush it
            buffers = flushFullCache(buffers);
        }
        return buffers;
    }

    /**
     * When we know the cache is full, this method will flush it to the input
     * buffers. Those buffers are then returned to the caller as the flushing of
     * data may have expanded the buffer list.
     *
     * @param buffers
     * @return WsByteBuffer[]
     */
    protected WsByteBuffer[] flushFullCache(WsByteBuffer[] buffers) {
        // just dump the whole cache and reset position
        this.bytePosition = 0;
        return GenericUtils.putByteArray(buffers, this.byteCache, this);
    }

    /**
     * Method to flush whatever is in the cache into the input buffers. These
     * buffers are then returned to the caller as the flush may have needed to
     * expand the list.
     *
     * @param buffers
     * @return WsByteBuffer[]
     */
    protected WsByteBuffer[] flushCache(WsByteBuffer[] buffers) {
        // PK13351 - use the offset/length version to write only what we need
        // to and avoid the extra memory allocation
        int pos = this.bytePosition;
        if (0 == pos) {
            // nothing to write
            return buffers;
        }
        this.bytePosition = 0;
        return GenericUtils.putByteArray(buffers, this.byteCache, 0, pos, this);
    }

    /**
     * Utility method to reset the byte cache back to the global array instead
     * of potentially pointing to an indirect buffers backing array. This
     * should be called after parsing of headers is completed, and when the
     * marshalling of outgoing headers has started.
     *
     */
    final protected void resetByteCache() {
        this.bytePosition = 0;
        this.byteLimit = 0;
    }

    /**
     * Decrement the byte position unless it points to an LF character, in which
     * case just leave the byte position alone.
     *
     */
    final protected void decrementBytePositionIgnoringLFs() {
        // PK15898 - added for just LF after first line
        this.bytePosition--;
        if (BNFHeaders.LF == this.byteCache[this.bytePosition]) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "decrementILF found an LF character");
            }
            this.bytePosition++;
        }
    }

    /**
     * Method to simply clear out the parse token information.
     */
    final protected void clearCacheToken() {
        this.parsedToken = null;
        this.parsedTokenLength = 0;
    }

    /**
     * Method to create a brand new parse token based on the input length. This
     * is intended to be used when any previous array cannot be re-used, meaning
     * that the contents cannot be changed.
     *
     * @param len
     */
    final protected void createCacheToken(int len) {
        this.parsedToken = new byte[len];
        this.parsedTokenLength = 0;
    }

    /**
     * Reset the parse byte token based on the input length. If the existing
     * array is the same size, then this is a simple reset. This is intended
     * to only be used when the contents have already been extracted and can
     * be overwritten with new data.
     *
     * @param len
     */
    final protected void resetCacheToken(int len) {
        if (null == this.parsedToken || len != this.parsedToken.length) {
            this.parsedToken = new byte[len];
        }
        this.parsedTokenLength = 0;
    }

    /**
     * Method to fill the parse token from the given input buffer. The token
     * array must have been created prior to this attempt to fill it.
     *
     * @param buff
     * @return boolean (true means success)
     */
    final protected boolean fillCacheToken(WsByteBuffer buff) {
        // figure out how much we have left to copy out, append to any existing
        // parsed token (multiple passes through here).
        int curr_len = this.parsedTokenLength;
        int need_len = this.parsedToken.length - curr_len;
        int copy_len = need_len;

        // keep going until we have all we need or we run out of buffer data
        while (0 < need_len) {
            if (this.bytePosition >= this.byteLimit) {
                if (!fillByteCache(buff)) {
                    // save a reference to how much we've pulled so far
                    this.parsedTokenLength = curr_len;
                    return false;
                }
            }
            // byte cache is now prepped
            int available = this.byteLimit - this.bytePosition;
            if (available < need_len) {
                // copy what we can from the current cache
                copy_len = available;
            } else {
                copy_len = need_len;
            }
            // copy new data into the existing space
            System.arraycopy(this.byteCache, this.bytePosition, this.parsedToken, curr_len, copy_len);
            need_len -= copy_len;
            curr_len += copy_len;
            this.bytePosition += copy_len;
        }

        return true;
    }

    /**
     * Fills the byte cache.
     *
     * @param buff
     * @return true on success and false on failure.
     */
    protected boolean fillByteCache(WsByteBuffer buff) {
        if (this.bytePosition < this.byteLimit) {
            return false;
        }

        int size = buff.remaining();
        if (size > this.byteCacheSize) {
            // truncate to just fill up the cache
            size = this.byteCacheSize;
        }
        this.bytePosition = 0;
        this.byteLimit = size;
        if (0 == this.byteLimit) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "fillByteCache: no data");
            }
            return false;
        }
        if (HeaderStorage.NOTSET != this.headerChangeLimit && -1 != this.parseIndex
            && -1 == this.parseBuffersStartPos[this.parseIndex]) {
            // first occurrance of this buffer and we're keeping track of changes
            this.parseBuffersStartPos[this.parseIndex] = buff.position();
        }
        buff.get(this.byteCache, this.bytePosition, this.byteLimit);

        return true;
    }

    /**
     * Calculate where the current position in the buffer really is, allowing
     * for offset based on the current byte cache information.
     *
     * @param buffer
     * @return int
     */
    private int findCurrentBufferPosition(WsByteBuffer buffer) {
        return buffer.position() - (this.byteLimit - this.bytePosition);
    }

    /**
     * Parse a CRLF delimited token and return the length of the token.
     *
     * @param buff
     * @return TokenCodes (global length variable is set to parsed length)
     * @throws MalformedMessageException
     */
    protected TokenCodes findCRLFTokenLength(WsByteBuffer buff) throws MalformedMessageException {

        TokenCodes rc = TokenCodes.TOKEN_RC_MOREDATA;

        if (null == buff) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Null buffer provided");
            }
            return rc;
        }

        // start with any pre-existing data
        int length = this.parsedTokenLength;
        byte b;
        while (true) {
            if (this.bytePosition >= this.byteLimit) {
                if (!fillByteCache(buff)) {
                    // no more data
                    break;
                }
            }
            b = this.byteCache[this.bytePosition++];

            // check for a CRLF
            if (BNFHeaders.CR == b) {
                rc = TokenCodes.TOKEN_RC_DELIM;
                if (HeaderStorage.NOTSET != this.headerChangeLimit) {
                    this.lastCRLFPosition = findCurrentBufferPosition(buff) - 1;
                    this.lastCRLFBufferIndex = this.parseIndex;
                    this.lastCRLFisCR = true;
                }
                break; // out of while
            } else if (BNFHeaders.LF == b) {
                // update counter if linefeed found
                rc = TokenCodes.TOKEN_RC_DELIM;
                this.numCRLFs = 1;
                if (HeaderStorage.NOTSET != this.headerChangeLimit) {
                    this.lastCRLFPosition = findCurrentBufferPosition(buff) - 1;
                    this.lastCRLFBufferIndex = this.parseIndex;
                    this.lastCRLFisCR = false;
                }
                break; // out of while
            }

            length++;
            // check the limit on a token size
            if (length > this.limitTokenSize) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "findCRLFTokenLength: length is too big: " + length);
                }
                throw new MalformedMessageException("Token length: " + length);
            }
        } // end of the while

        this.parsedTokenLength = length;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "findCRLFTokenLength returning " + rc.getName() + "; len=" + length);
        }
        return rc;
    }

    /**
     * Parse a byte delimited token and return the length of the token.
     *
     * @param buff
     * @param delimiter
     * @param bApproveCRLF
     * @return TokenCodes (global length variable is set to parsed length)
     * @throws MalformedMessageException
     */
    protected TokenCodes findTokenLength(WsByteBuffer buff, byte delimiter, boolean bApproveCRLF) throws MalformedMessageException {

        TokenCodes rc = TokenCodes.TOKEN_RC_MOREDATA;

        if (null == buff) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "findTokenLength: null buffer provided");
            }
            return rc;
        }

        byte b;
        // start with any pre-existing data
        int length = this.parsedTokenLength;
        while (true) {
            if (this.bytePosition >= this.byteLimit) {
                if (!fillByteCache(buff)) {
                    // no more data
                    break;
                }
            }
            b = this.byteCache[this.bytePosition++];

            // check delimiter
            if (delimiter == b) {
                rc = TokenCodes.TOKEN_RC_DELIM;
                break;
            }

            // check for possible CRLF
            if (BNFHeaders.CR == b) {
                // check if a CRLF is okay to be the delimiter
                if (!bApproveCRLF) {
                    throw new MalformedMessageException("Invalid CR found in token");
                }
                rc = TokenCodes.TOKEN_RC_CRLF;
                if (HeaderStorage.NOTSET != this.headerChangeLimit) {
                    this.lastCRLFPosition = findCurrentBufferPosition(buff) - 1;
                    this.lastCRLFBufferIndex = this.parseIndex;
                    this.lastCRLFisCR = true;
                }
                break; // out of while
            }

            if (BNFHeaders.LF == b) {
                // check if a CRLF is okay to be the delimiter
                if (!bApproveCRLF) {
                    throw new MalformedMessageException("Invalid LF found in token");
                }
                rc = TokenCodes.TOKEN_RC_CRLF;
                this.numCRLFs = 1;
                if (HeaderStorage.NOTSET != this.headerChangeLimit) {
                    this.lastCRLFPosition = findCurrentBufferPosition(buff) - 1;
                    this.lastCRLFBufferIndex = this.parseIndex;
                    this.lastCRLFisCR = false;
                }
                break; // out of while
            }

            length++;
            // check the limit on a token size
            if (length > this.limitTokenSize) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "findTokenLength: length is too big: " + length);
                }
                throw new MalformedMessageException("Token length: " + length);
            }
        } // end of the while

        this.parsedTokenLength = length;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "findTokenLength: [" + (char) delimiter + "] " + rc.getName() + "; len=" + length);
        }
        return rc;
    }

    /**
     * This method is used to skip leading CRLF characters. It will stop when
     * it finds a non-CRLF character, runs out of data, or finds too many CRLFs
     *
     * @param buffer
     * @return TokenCodes -- MOREDATA means it ran out of buffer information,
     *         DELIM means it found a non-CRLF character, and CRLF means it found
     *         too many CRLFs
     */
    protected TokenCodes skipCRLFs(WsByteBuffer buffer) {

        int maxCRLFs = 33;
        // limit is the max number of CRLFs to skip
        if (this.bytePosition >= this.byteLimit) {
            if (!fillByteCache(buffer)) {
                // no more data
                return TokenCodes.TOKEN_RC_MOREDATA;
            }
        }
        byte b = this.byteCache[this.bytePosition++];

        for (int i = 0; i < maxCRLFs; i++) {
            if (-1 == b) {
                // ran out of data
                return TokenCodes.TOKEN_RC_MOREDATA;
            }
            if (BNFHeaders.CR != b && BNFHeaders.LF != b) {
                // stopped on non-CRLF character, reset position
                this.bytePosition--;
                return TokenCodes.TOKEN_RC_DELIM;
            }
            // keep going otherwise
            if (this.bytePosition >= this.byteLimit) {
                return TokenCodes.TOKEN_RC_MOREDATA;
            }
            b = this.byteCache[this.bytePosition++];
        }
        // found too many CRLFs... invalid
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Too many leading CRLFs found");
        }
        return TokenCodes.TOKEN_RC_CRLF;
    }

    /**
     * Utility method to parse CRLFs and find out if we've reached the end
     * of the headers (4 CRLFs). The global endOfHeaders flag will
     * be true if we did find 4 CRLFs. Throws a MalformedMessageException
     * if a multiline header value is parsed without the corresponding
     * header name.
     *
     * @param buff
     * @return boolean (false if need more data, true otherwise)
     * @throws MalformedMessageException
     */
    private boolean parseCRLFs(WsByteBuffer buff) throws MalformedMessageException {

        byte b;
        // scan through up to 4 characters
        for (int i = 0; i < 4; i++) {
            if (this.bytePosition >= this.byteLimit) {
                if (!fillByteCache(buff)) {
                    // no more data
                    return false;
                }
            }
            b = this.byteCache[this.bytePosition++];

            if (BNFHeaders.CR == b) {
                // ignore CR characters
                continue;
            } else if (BNFHeaders.LF == b) {
                // line feed found
                this.numCRLFs++;
            } else if (BNFHeaders.SPACE == b || BNFHeaders.TAB == b) {
                // Check for multi-line header values
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Multiline header follows");
                }
                this.bIsMultiLine = true;
                if (null == this.lastHdrInSequence) {
                    // can't start off with a multiline value
                    throw new MalformedMessageException("Incorrect multiline header value");
                }
                this.currentElem = this.lastHdrInSequence;
                this.stateOfParsing = PARSING_VALUE;
                this.numCRLFs = 0;
                return true;
            } else {
                // found end...move pointer back one
                this.bytePosition--;
                break; // out of for loop
            }
            if (2 <= this.numCRLFs) {
                // found double LFs, end of headers
                this.eohPosition = findCurrentBufferPosition(buff);
                buff.position(this.eohPosition);
                break; // out of for loop
            }
        } // end of for loop

        // we're about to either start parsing another header or
        // we've reached the end of the headers
        this.bIsMultiLine = false;
        this.stateOfParsing = PARSING_HEADER;
        this.numCRLFs = 0;
        return true;
    }

    /**
     * Parse and extract a CRLF delimited token.
     * <p>
     * Returns either the TOKEN_RC_DELIM or the TOKEN_RC_MOREDATA return codes.
     *
     * @param buff
     * @param log  - whether to debug log contents or not
     * @return TokenCodes
     * @throws MalformedMessageException
     */
    protected TokenCodes parseCRLFTokenExtract(WsByteBuffer buff, int log) throws MalformedMessageException {

        // if we're just starting, then skip leading white space characters
        // otherwise ignore them (i.e we might be in the middle of
        // "Mozilla/5.0 (Win"
        if (null == this.parsedToken) {
            if (!skipWhiteSpace(buff)) {
                return TokenCodes.TOKEN_RC_MOREDATA;
            }
        }
        int start = findCurrentBufferPosition(buff);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseCRLFTokenExtract: start:" + start
                         + " lim:" + this.byteLimit
                         + " pos:" + this.bytePosition);
        }
        TokenCodes rc = findCRLFTokenLength(buff);
        // Set the parsedToken from the token parsed from this ByteBuffer
        saveParsedToken(buff, start, TokenCodes.TOKEN_RC_DELIM.equals(rc), log);
        return rc;
    }

    /**
     * Parse a CRLF delimited token and return the length of the token. This
     * does not trigger the "extraction" or saving of the token.
     *
     * @param buff
     * @return int (parsed token length)
     * @throws MalformedMessageException
     */
    protected int parseCRLFTokenNonExtract(WsByteBuffer buff) throws MalformedMessageException {

        findCRLFTokenLength(buff);
        return this.parsedTokenLength;
    }

    /**
     * Parse a byte delimited token and return the length of the token.
     *
     * @param buff
     * @return TokenCodes (global length variable is set to parsed length)
     * @throws MalformedMessageException
     */
    protected TokenCodes findHeaderLength(WsByteBuffer buff) throws MalformedMessageException {

        TokenCodes rc = TokenCodes.TOKEN_RC_MOREDATA;

        if (null == buff) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "findHeaderLength: null buffer provided");
            }
            return rc;
        }

        byte b;
        int numSpaces = 0;
        // start with any pre-existing data
        int length = this.parsedTokenLength;
        while (true) {
            if (this.bytePosition >= this.byteLimit) {
                if (!fillByteCache(buff)) {
                    // no more data
                    break;
                }
            }
            b = this.byteCache[this.bytePosition++];

            // look for the colon marking the end
            if (BNFHeaders.COLON == b) {
                length -= numSpaces; // remove any "trailing" white space

                if (numSpaces > 0) {
                    //PI13987
                    //found trailing whitespace
                    this.foundTrailingWhitespace = true;
                }

                rc = TokenCodes.TOKEN_RC_DELIM;
                break;
            }

            // if we hit whitespace, then keep track of the number of spaces so
            // that we can easily trim that off at the end. This will end up
            // ignoring whitespace that is inside the header name if that does
            // happen

            if (BNFHeaders.SPACE == b || BNFHeaders.TAB == b) {
                numSpaces++;
            } else {
                // reset the counter on any non-space or colon
                numSpaces = 0;

                // check for possible CRLF
                if (BNFHeaders.CR == b || BNFHeaders.LF == b) {
                    // Note: would be nice to print the failing data but would need
                    // to keep track of where we started inside here, then what about
                    // data straddling bytecaches, etc?
                    throw new MalformedMessageException("Invalid CRLF found in header name");
                }

                // PH52074 Check for other invalid chars
                if (!HttpHeaderKeys.isValidTchar((char) (b & 0xFF))) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        final int maskedCodePoint = b & 0xFF;
                        Tr.debug(tc, "Invalid character found in http header name.  The Unicode is: " + String.format("%04x", maskedCodePoint));
                    }
                    throw new MalformedMessageException("Invalid character found in header name");
                }
            }

            length++;
            // check the limit on a token size
            if (length > this.limitTokenSize) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "findTokenLength: length is too big: " + length);
                }
                throw new MalformedMessageException("Token length: " + length);
            }
        } // end of the while

        this.parsedTokenLength = length;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "findHeaderLength: " + rc.getName() + "; len=" + length);
        }
        return rc;
    }

    /**
     * Utility method to parse the header name from the input buffer.
     *
     * @param buff
     * @return boolean (false means it needs more data, true otherwise)
     * @throws MalformedMessageException
     */
    private boolean parseHeaderName(WsByteBuffer buff) throws MalformedMessageException {

        // if we're just starting, then skip leading white space characters
        // otherwise ignore them (i.e we might be in the middle of
        // "Mozilla/5.0 (Win"
        if (null == this.parsedToken) {
            if (!skipWhiteSpace(buff)) {
                return false;
            }
        }

        int start = findCurrentBufferPosition(buff);
        int cachestart = this.bytePosition;
        TokenCodes rc = findHeaderLength(buff);
        if (TokenCodes.TOKEN_RC_MOREDATA.equals(rc)) {
            // ran out of data
            saveParsedToken(buff, start, false, LOG_FULL);
            return false;
        }
        // could be in one single bytecache, otherwise we have to extract from
        // buffer
        byte[] data;
        int length = this.parsedTokenLength;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "length=" + length
                         + " pos=" + this.bytePosition + ", cachestart=" + cachestart
                         + ", start=" + start + ", trailingWhitespace=" + this.foundTrailingWhitespace);
        }

        //PI13987 - Added the first argument to the if statement
        if (!this.foundTrailingWhitespace && null == this.parsedToken && length < this.bytePosition) {
            // it's all in the bytecache
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                //PI13987 - Modified the message being printed as we now print the same thing above
                Tr.debug(tc, "Using bytecache");
            }
            data = this.byteCache;
            start = cachestart;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                //PI13987
                Tr.debug(tc, "Using bytebuffer");
            }
            saveParsedToken(buff, start, true, LOG_FULL);
            data = this.parsedToken;
            start = 0;
            length = data.length;
        }
        // otherwise we found the entire length of the name
        this.currentElem = getElement(findKey(data, start, length, false));

        // Reset all the global variables once HeaderElement has been instantiated
        if (HeaderStorage.NOTSET != this.headerChangeLimit) {
            this.currentElem.updateLastCRLFInfo(this.lastCRLFBufferIndex, this.lastCRLFPosition, this.lastCRLFisCR);
        }
        this.stateOfParsing = PARSING_VALUE;
        this.parsedToken = null;
        this.parsedTokenLength = 0;
        this.foundTrailingWhitespace = false; //PI13987
        return true;
    }

    /**
     * Utility method for parsing a header value out of the input buffer.
     *
     * @param buff
     * @return boolean (false if need more data, true otherwise)
     * @throws MalformedMessageException
     */
    private boolean parseHeaderValueExtract(WsByteBuffer buff) throws MalformedMessageException {

        // 295178 - don't log sensitive information
        // log value contents based on the header key (if known)
        int log = LOG_FULL;
        HeaderKeys key = this.currentElem.getKey();
        if (null != key && !key.shouldLogValue()) {
            // this header key wants to block the entire thing
            log = LOG_NONE;
        }
        TokenCodes tcRC = parseCRLFTokenExtract(buff, log);
        if (!tcRC.equals(TokenCodes.TOKEN_RC_MOREDATA)) {
            setHeaderValue();
            this.parsedToken = null;
            this.currentElem = null;
            this.stateOfParsing = PARSING_CRLF;
            return true;
        }

        // otherwise we need more data in order to read the value
        return false;
    }

    /**
     * Utility method for parsing the header value and storing the buffer
     * parse information, and NOT extracting the value as a byte[] until
     * someone requests it from storage.
     *
     * @param buff
     * @return boolean (false if we need more data, true otherwise)
     * @throws MalformedMessageException
     */
    private boolean parseHeaderValueNonExtract(WsByteBuffer buff) throws MalformedMessageException {

        if (0 == this.parsedTokenLength) {
            // save the start position when we first start parsing the value
            if (!skipWhiteSpace(buff)) {
                return false;
            }
            int start = findCurrentBufferPosition(buff);
            this.currentElem.setParseInformation(this.parseIndex, start);
        }
        // if we ran out of data, then return false from here
        if (TokenCodes.TOKEN_RC_MOREDATA.equals(findCRLFTokenLength(buff))) {
            return false;
        }

        this.currentElem.setValueLength(this.parsedTokenLength);
        addHeader(this.currentElem, FILTER_YES);
        this.parsedTokenLength = 0;
        this.currentElem = null;
        this.stateOfParsing = PARSING_CRLF;
        return true;
    }

    /**
     * Method for parsing a token from a given WSBB. This will stop when the
     * byte delimeter is reached, or when a CRLF is found. If it sees the CRLF,
     * then it checks the boolean input on whether CRLF is valid and will throw
     * an exception if it's not valid.
     * <p>
     * Returns a TOKEN_RC code as to whether a CRLF or delimiter was reached,
     * or MOREDATA if no delimiter was found and more data needs to be read.
     *
     * @param buff
     * @param bDelimiter
     * @param bApproveCRLF
     * @param log          - control how much of the token to debug log
     * @return TokenCodes
     * @throws MalformedMessageException
     */
    protected TokenCodes parseTokenExtract(WsByteBuffer buff, byte bDelimiter, boolean bApproveCRLF, int log) throws MalformedMessageException {

        // if we're just starting, then skip leading white space characters
        // otherwise ignore them (i.e we might be in the middle of
        // "Mozilla/5.0 (Win"
        if (null == this.parsedToken) {
            if (!skipWhiteSpace(buff)) {
                return TokenCodes.TOKEN_RC_MOREDATA;
            }
        }
        int start = findCurrentBufferPosition(buff);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseTokenExtract: start:" + start
                         + " lim:" + this.byteLimit
                         + " pos:" + this.bytePosition);
        }
        TokenCodes rc = findTokenLength(buff, bDelimiter, bApproveCRLF);
        // 273923: need to account for stopping on the delimiter or stopping on
        // a CRLF when those are acceptable
        saveParsedToken(buff, start, !TokenCodes.TOKEN_RC_MOREDATA.equals(rc), log);
        return rc;
    }

    /**
     * Standard parsing of a token; however, instead of saving the data into
     * the global parsedToken variable, this merely returns the length of the
     * token. Used for occasions where we just need to find the length of
     * the token.
     *
     * @param buff
     * @param bDelimiter
     * @param bApproveCRLF
     * @return int (-1 means we need more data)
     * @throws MalformedMessageException
     */
    protected int parseTokenNonExtract(WsByteBuffer buff, byte bDelimiter, boolean bApproveCRLF) throws MalformedMessageException {

        TokenCodes rc = findTokenLength(buff, bDelimiter, bApproveCRLF);
        return (TokenCodes.TOKEN_RC_MOREDATA.equals(rc)) ? -1 : this.parsedTokenLength;
    }

    /**
     * Utility method used for adding the header(name and value) to
     * the primary header data storage data structures.
     *
     * @throws MalformedMessageException
     */
    protected void setHeaderValue() throws MalformedMessageException {

        // Empty header values valid, we'll store a space character for that
        if (null == this.parsedToken) {
            this.parsedToken = new byte[] { BNFHeaders.SPACE };
        }
        if (this.bIsMultiLine) {
            // must append the new value to the old
            byte[] oldValue = this.currentElem.asBytes();
            int size = oldValue.length + this.parsedToken.length + 1;
            if (size > this.limitTokenSize) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Multiline header value too large: " + size);
                }
                throw new MalformedMessageException("Multiline value length: " + size);
            }
            byte[] newValue = new byte[oldValue.length + this.parsedToken.length + 1];
            System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
            newValue[oldValue.length] = BNFHeaders.SPACE;
            System.arraycopy(this.parsedToken, 0,
                             newValue, oldValue.length + 1, this.parsedToken.length);
            this.currentElem.setByteArrayValue(newValue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Saved multiline header value [" + this.currentElem.getDebugValue() + "]");
            }
            // note: header is already saved in storage, we're just updating the
            // value with more data. Don't call add header API.
        } else {
            // just save this value itself
            this.currentElem.setByteArrayValue(this.parsedToken);
            addHeader(this.currentElem, FILTER_YES);
        }
        // now that we have the parsed value saved, start tracking changes/removes
        this.currentElem.startTracking();
    }

    /**
     * Sets the temporary parse token from the input buffer.
     *
     * @param buff  The current WsByteBuffer being parsed
     * @param start The start position of the token
     * @param delim Did we stop on the delimiter or not?
     * @param log   Whether to log the contents or not
     */
    private void saveParsedToken(WsByteBuffer buff, int start, boolean delim, int log) {

        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        // local copy of the length
        int length = this.parsedTokenLength;
        this.parsedTokenLength = 0;
        if (0 > length) {
            throw new IllegalArgumentException("Negative token length: " + length);
        }
        if (bTrace && tc.isDebugEnabled()) {
            // 295178 - don't log sensitive information
            String value = GenericUtils.getEnglishString(this.parsedToken);
            if (null != value) {
                if (LOG_PARTIAL == log) {
                    value = GenericUtils.nullOutPasswords(value, LF);
                } else if (LOG_NONE == log) {
                    value = GenericUtils.blockContents(value);
                }
            }
            Tr.debug(tc, "Saving token: "
                         + value
                         + " len:" + length
                         + " start:" + start + " pos:" + this.bytePosition
                         + " delim:" + delim);
        }

        byte[] temp;
        int offset;
        if (null != this.parsedToken) {
            // concat to the existing value
            offset = this.parsedToken.length;
            temp = new byte[offset + length];
            System.arraycopy(this.parsedToken, 0, temp, 0, offset);
        } else {
            offset = 0;
            temp = new byte[length];
        }

        //PI13987 - Added the first argument
        if (!this.foundTrailingWhitespace && this.bytePosition > length) {
            // pull from the bytecache
            if (bTrace && tc.isDebugEnabled()) {
                //PI13987 - Print out this new trace message
                Tr.debug(tc, "savedParsedToken - using bytecache");
            }
            int cacheStart = this.bytePosition - length;
            if (delim) {
                cacheStart--;
            }
            System.arraycopy(this.byteCache, cacheStart, temp, offset, length);
        } else {
            // must pull from the buffer
            if (bTrace && tc.isDebugEnabled()) {
                //PI13987 - Print this new trace message
                Tr.debug(tc, "savedParsedToken - pulling from buffer");
            }
            int orig = buff.position();
            buff.position(start);
            buff.get(temp, offset, length);
            buff.position(orig);
        }
        this.parsedToken = temp;
        if (bTrace && tc.isDebugEnabled()) {
            // 295178 - don't log sensitive information
            String value = GenericUtils.getEnglishString(this.parsedToken);
            if (LOG_PARTIAL == log) {
                value = GenericUtils.nullOutPasswords(value, LF);
            } else if (LOG_NONE == log) {
                value = GenericUtils.blockContents(value);
            }
            Tr.debug(tc, "Saved token [" + value + "]");
        }
    }

    /**
     * Sets the flag indicating that a SIP compact header has been parsed.
     *
     * @param flag Whether or not a header is in compact for or not
     */
    public void parsedCompactHeader(boolean flag) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "parsedCompactHeader: " + flag);
        }

        this.compactHeaderFlag = flag;
    }

    /**
     * Gets the flag indicating that a SIP compact header has been parsed.
     *
     * @return compactHeaderFlag Whether or not a compact header has been parsed or not.
     */
    public boolean foundCompactHeader() {
        return this.compactHeaderFlag;
    }

    /**
     * Simple class to use when returning an empty header field on getters.
     */
    private static class EmptyHeaderField implements HeaderField {
        /**
         * Constructor.
         */
        protected EmptyHeaderField() {
            // nothing to do
        }

        /*
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "null header";
        }

        /*
         * @see com.ibm.wsspi.genericbnf.HeaderField#asBytes()
         */
        @Override
        public byte[] asBytes() {
            return null;
        }

        /*
         * @see com.ibm.wsspi.genericbnf.HeaderField#asDate()
         */
        @Override
        @SuppressWarnings("unused")
        public Date asDate() throws ParseException {
            return null;
        }

        /*
         * @see com.ibm.wsspi.genericbnf.HeaderField#asInteger()
         */
        @Override
        public int asInteger() throws NumberFormatException {
            return 0;
        }

        /*
         * @see com.ibm.wsspi.genericbnf.HeaderField#asString()
         */
        @Override
        public String asString() {
            return null;
        }

        /*
         * @see com.ibm.wsspi.genericbnf.HeaderField#asTokens(byte)
         */
        @Override
        @SuppressWarnings("unused")
        public List<byte[]> asTokens(byte delimiter) {
            return new ArrayList<byte[]>();
        }

        /*
         * @see com.ibm.wsspi.genericbnf.HeaderField#getKey()
         */
        @Override
        public HeaderKeys getKey() {
            return null;
        }

        /*
         * @see com.ibm.wsspi.genericbnf.HeaderField#getName()
         */
        @Override
        public String getName() {
            return null;
        }
    }

    private static synchronized byte[] getWhiteSpace() {
        byte[] localWhitespace = whitespace;
        if (localWhitespace == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Allocating static whitespace data");
            }
            localWhitespace = new byte[1024];
            for (int i = 0; i < 1024; i++) {
                localWhitespace[i] = BNFHeaders.SPACE;
            }
            whitespace = localWhitespace;
        }
        return localWhitespace;
    }

    public WsByteBuffer[] encodeHeaders(H2HeaderTable table, WsByteBuffer[] encodedMessage, boolean isPushPromise) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "encodeHeaders");
        }
        //Set the H2 header that will be used when encoding headers
        this.table = table;
        //Set a flag specifying that this context belongs to a Push Promise response.
        //No indexing should be done if true;
        this.isPushPromise = isPushPromise;

        //Call the typical marshall header code to get all headers encoded and
        //marshalled into the message buffers. Returns null if there was
        //an exception encoding the headers into the buffers.
        WsByteBuffer[] buffers = marshallHeaders(encodedMessage);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "encodeHeaders");
        }
        return buffers;

    }

    private void processForwardedHeader(HeaderElement header, boolean isDeFacto) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processForwardedHeader");
        }

        //Since we favor Forwarded headers, only process an X-Forwarded header if
        //we have not parsed a Forwarded header up to this point

        if (isDeFacto && !processedForwardedHeader) {
            //If this is the first time parsing an X-Forwarded header, turn
            //on the internal flag and initialize the instances
            if (!processedXForwardedHeader) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "First X-Forwarded-* header found, enabling de facto tracking");
                }
                processedXForwardedHeader = true;
                initForwarding();
            }

            //process the X-Forwarded-* header
            processXForwardedHeader(header);

        } else if (!isDeFacto) {
            //Have we processed X-Forwarded-* headers up to this point?
            //If so, turn off the X-Forwarding flag

            if (processedXForwardedHeader) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Processing Forwarded header and found previously processed de facto tracking, disabling de facto tracking");
                }
                processedXForwardedHeader = false;

            }
            //Turn our Forwarded processed flag on, so we know not to process
            //any X-Forwarded-* headers. If there were values on the
            //forwarded instances by previously processed X-Forwarding headers,
            //the reinitialize will clear them up.
            if (!processedForwardedHeader) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Processing Forwarded header, enabling Forwarded header tracking");
                }
                processedForwardedHeader = true;
                initForwarding();
            }
            //process the Forwarded Header
            processSpecForwardedHeader(header);

        }

        else {
            //We received an X-Forwarded-* header, while having already processed
            //a Forwarded header. In this case, just exit this processing and
            //do not change the state of the internal instances. The X-Forwarding
            //information is cleared out on the first Forwarded processed header,
            //so it should be clear at this point.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "X-Forwarded header received after Forwarded header tracking has been enabled, "
                             + "X-Forwarded header will not be processed");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processForwardedHeader");
        }

    }

    private void initForwarding() {

        if (forwardedByList == null) {
            forwardedByList = new ArrayList<String>();
        } else {
            //Already created, clear out its contents
            forwardedByList.clear();
        }

        if (forwardedForList == null) {
            forwardedForList = new ArrayList<String>();
        } else {
            //Already created, clear out its contents
            forwardedForList.clear();
        }

        forwardedProto = null;
        forwardedHost = null;
        forwardedPort = null;

    }

    private void processXForwardedHeader(HeaderElement header) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processXForwardedHeader");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.debug(tc, "processing [" + header.getName() + ":" + header.getDebugValue() + "]");
        }

        //If this is an X-Forwarded-For or X-Forwarded-By, append the value to
        //a comma delimited StringBuilder
        if (X_FORWARDED_FOR.equalsIgnoreCase(header.getName())) {
            processXForwardedAddressExtract(header.getDebugValue(), forwardedForList);
        } else if (X_FORWARDED_BY.equalsIgnoreCase(header.getName())) {
            processXForwardedAddressExtract(header.getDebugValue(), forwardedByList);
        } else if (X_FORWARDED_PROTO.equalsIgnoreCase(header.getName())) {
            forwardedProto = header.getDebugValue();
            if (!validateProto(forwardedProto)) {
                forwardedProto = null;
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.debug(tc, "X-Forwarded-Proto value is invalid: " + header.getDebugValue());
                }
            }

        } else if (X_FORWARDED_PORT.equalsIgnoreCase(header.getName())) {
            forwardedPort = header.getDebugValue();
        } else if (X_FORWARDED_HOST.equalsIgnoreCase(header.getName())) {
            forwardedHost = header.getDebugValue();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processXForwardedHeader");
        }

    }

    private void processXForwardedAddressExtract(String nodeExtract, ArrayList<String> list) {
        //The X-Forwarded By and For headers can contain multiple
        //comma delimited addresses. Split the extract by this delimiter
        //and add each to their respective list
        String[] addresses = nodeExtract.split(",");
        for (String address : addresses) {
            list.add(address.trim());
        }
    }

    private void processSpecForwardedHeader(HeaderElement header) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processSpecForwardedHeader");
        }

        //Each Forwarded header may consist of a combination of the four
        //spec defined parameters: by, for, host, proto. When more than
        //one parameter is present, the header value will use the semi-
        //colon character to delimit between them.
        String[] parameters = header.getDebugValue().split(";");
        String[] nodes = null;
        String node = null;
        String nodeExtract = null;

        for (String param : parameters) {

            //The "for" and "by" parameters could be comma delimited
            //lists. As such, lets split this again to save the
            //data in the same format as X-Forwarding
            nodes = param.split(",");

            for (String value : nodes) {

                //Note that HTTP list allows white spaces between the identifiers, as such,
                //trim the string before evaluating.
                node = value.replaceAll("\\s", "");;
                try {
                    nodeExtract = node.substring(node.indexOf("=") + 1);
                } catch (IndexOutOfBoundsException e) {
                    processForwardedErrorState();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.debug(tc, "Forwarded header node value was malformed.");
                        Tr.exit(tc, "processSpecForwardedHeader");
                    }
                    return;
                }

                if (node.toLowerCase().startsWith(FOR)) {

                    processForwardedAddressExtract(nodeExtract, ListType.FOR);

                } else if (node.toLowerCase().startsWith(BY)) {

                    processForwardedAddressExtract(nodeExtract, ListType.BY);

                } else if (node.toLowerCase().startsWith(PROTO)) {
                    forwardedProto = nodeExtract;
                    boolean validProto = validateProto(forwardedProto);
                    if (!validProto) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.debug(tc, "Forwarded header proto value was malformed: " + forwardedProto);
                        }
                        processForwardedErrorState();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "processSpecForwardedHeader");
                        }
                        return;
                    }
                } else if (node.toLowerCase().startsWith(HOST)) {
                    forwardedHost = nodeExtract;
                    forwardedHost = validateForwardedHost(forwardedHost);
                    if (forwardedHost == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.debug(tc, "Forwarded header host value was malformed: " + nodeExtract);
                        }
                        processForwardedErrorState();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "processSpecForwardedHeader");
                        }
                        return;
                    }
                }
                //Unrecognized parameter
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.debug(tc, "Unrecognized parameter if Forwarded header: " + node);
                    }
                    processForwardedErrorState();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "processSpecForwardedHeader");
                    }
                    return;
                }

                //Check that processing of this node has not entered error state
                if (forwardHeaderErrorState) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "processSpecForwardedHeader");
                    }
                    return;
                }
            }

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processSpecForwardedHeader");
        }

    }

    /**
     * Returns a String representation of the provided address node delimiter
     * which is used to update the Forwarded for/by builders. This
     * method will remove any provided port in the process, with the exception
     * of the very first element added to the list. The first element would
     * correspond to the client address, and as such, the port is saved off
     * to be referenced as the remote port.
     *
     * @param nodeExtract
     * @return
     */
    private void processForwardedAddressExtract(String nodeExtract, ListType type) {

        ArrayList<String> list = null;
        if (type == ListType.BY) {
            list = this.forwardedByList;
        }
        if (type == ListType.FOR) {
            list = this.forwardedForList;
        }

        //The node identifier is defined by the ABNF syntax as
        //        node     = nodename [ ":" node-port ]
        //                   nodename = IPv4address / "[" IPv6address "]" /
        //                             "unknown" / obfnode
        //As such, to make it equivalent to the de-facto headers, remove the quotations
        //and possible port
        String extract = nodeExtract.replaceAll("\"", "").trim();
        String nodeName = null;

        //obfnodes are only allowed to contain ALPHA / DIGIT / "." / "_" / "-"
        //so if the token contains "[", it is an IPv6 address
        int openBracket = extract.indexOf("[");
        int closedBracket = extract.indexOf("]");

        if (openBracket > -1) {
            //This is an IPv6address
            //The nodename is enclosed in "[ ]", get it now

            //If the first character isn't the open bracket or if a close bracket
            //is not provided, this is a badly formed header
            if (openBracket != 0 || !(closedBracket > -1)) {
                processForwardedErrorState();
                //badly formated header
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.debug(tc, "Forwarded header IPv6 was malformed.");
                    Tr.exit(tc, "processForwardedHeader");
                }
                return;
            }

            nodeName = extract.substring(openBracket + 1, closedBracket);

            //If this extract contains a port, there will be a ":" after
            //the closing bracket. Only get it if this is the first address
            //being added to the "for" list
            if ((type == ListType.FOR) && list.isEmpty() && extract.contains("]:")) {
                try {
                    this.forwardedPort = extract.substring(closedBracket + 2);
                } catch (IndexOutOfBoundsException e) {
                    processForwardedErrorState();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.debug(tc, "Forwarded header IPv6 port was malformed.");
                        Tr.exit(tc, "processForwardedHeader");
                    }
                    return;
                }
            }

        }

        //Simply delimit by ":" on other node types to separate nodename and node-port
        else {

            if (extract.contains(":")) {
                int index = extract.indexOf(":");
                nodeName = extract.substring(0, index);
                //Record the port if this is the first address being added to the
                //"for" list, corresponding to the client
                if ((type == ListType.FOR) && list.isEmpty()) {
                    try {
                        this.forwardedPort = extract.substring(index + 1);
                    } catch (IndexOutOfBoundsException e) {
                        processForwardedErrorState();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.debug(tc, "Forwarded header node-port was malformed.");
                            Tr.exit(tc, "processForwardedHeader");
                        }
                        return;
                    }

                }
            }
            //No port or "[ ]" characters, the nodename is the entire provided extract
            else {
                nodeName = extract;
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.debug(tc, "Forwarded address [" + nodeName + "] being tracked in " + type.toString() + " list.");
            Tr.exit(tc, "processForwardedHeader");
        }
        list.add(nodeName);
    }

    /*
     * A valid proto may start with an alpha followed by any number of chars that are
     * - alpha
     * - numeric
     * - "+" or "-" or "."
     */

    private boolean validateProto(String forwardedProto) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateProto");
        }
        char[] a = forwardedProto.toCharArray();
        boolean valid = true;
        char c = a[0];
        valid = ((c >= 'a') && (c <= 'z')) ||
                ((c >= 'A') && (c <= 'Z'));
        if (valid) {

            for (int i = 1; i < a.length; i++) {
                c = a[i];
                valid = ((c >= 'a') && (c <= 'z')) ||
                        ((c >= 'A') && (c <= 'Z')) ||
                        ((c >= '0') && (c <= '9')) ||
                        (c == '+') || (c == '-') || (c == '.');
                if (!valid) {
                    break;
                }
            }

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.debug(tc, "ValidateProto value is valid: " + valid);
            Tr.exit(tc, "validateProto");
        }
        return valid;

    }

    /*
     * Valid hostname can be a bracketed ipv6 address, or
     * any other string.
     * Validate the ipv6 address has opening and closing brackets.
     */

    private String validateForwardedHost(String forwardedHost) {
        int openBracket = forwardedHost.indexOf("[");
        int closedBracket = forwardedHost.indexOf("]");
        String nodename = forwardedHost;

        if (openBracket > -1) {
            //This is an IPv6address
            //The nodename is enclosed in "[ ]", get it now

            //If the first character isn't the open bracket or if close bracket
            //is missing, this is a badly formed header
            if (openBracket != 0 || !(closedBracket > -1)) {
                nodename = null;
            }
        }
        return nodename;
    }

    private void processForwardedErrorState() {
        this.forwardedByList = null;
        this.forwardedForList = null;
        this.forwardedHost = null;
        this.forwardedPort = null;
        this.forwardedProto = null;
        this.forwardHeaderErrorState = true;
    }

    public String[] getForwardedByList() {
        return (forwardedByList == null) ? null : forwardedByList.toArray(new String[forwardedByList.size()]);
    }

    public String[] getForwardedForList() {
        return (forwardedForList == null) ? null : forwardedForList.toArray(new String[forwardedForList.size()]);
    }

    public String getForwardedProto() {
        return forwardedProto;
    }

    public String getForwardedHost() {
        return forwardedHost;
    }

    public String getForwardedPort() {
        return forwardedPort;
    }

}
