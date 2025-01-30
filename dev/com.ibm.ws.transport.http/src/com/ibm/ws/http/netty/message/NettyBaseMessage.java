/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpTrailersImpl;
import com.ibm.ws.http.channel.internal.cookies.CookieCacheData;
import com.ibm.ws.http.channel.internal.cookies.CookieHeaderByteParser;
import com.ibm.ws.http.channel.internal.cookies.CookieUtils;
import com.ibm.ws.http.channel.internal.cookies.SameSiteCookieUtils;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.exception.UnsupportedProtocolVersionException;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpBaseMessage;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.ExpectValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.TransferEncodingValues;
import com.ibm.wsspi.http.channel.values.VersionValues;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.openliberty.http.constants.HttpGenerics;
import io.openliberty.http.netty.cookie.CookieDecoder;
import io.openliberty.http.netty.cookie.CookieEncoder;

/**
 *
 */
public class NettyBaseMessage implements HttpBaseMessage, Externalizable {

    private static final TraceComponent tc = Tr.register(NettyBaseMessage.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Serialization format for v6.0 and v6.1 */
    protected static final int SERIALIZATION_V1 = 0xBEEF0001;
    /** Serialization format for v7 and higher */
    protected static final int SERIALIZATION_V2 = 0xBEEF0002;
    /** Version used during deserialization step (if msg came that path) */
    protected transient int deserializationVersion = SERIALIZATION_V1;

    private boolean inbound = Boolean.FALSE;
    private boolean committed = Boolean.FALSE;

    private int limitOnNumberOfHeaders;

    protected HttpChannelConfig config;
    protected HttpMessage message;
    protected HttpHeaders headers;
    boolean initialized = Boolean.FALSE;

    protected long startTime = 0;
    protected long endTime = 0;

    /** Cookie Caches */
    protected transient CookieCacheData cookieCache;
    protected transient CookieCacheData cookie2Cache;
    protected transient CookieCacheData setCookieCache;
    protected transient CookieCacheData setCookie2Cache;
    /** Reference to the cookie parser */
    private transient CookieHeaderByteParser cookieParser;

    /** Reference to the service context */
    private HttpServiceContext serviceContext;

    private int limitOfTokenSize;

    private Map<String, String> headersMap = new HashMap<>();

    public NettyBaseMessage() {
    }

    protected void init(HttpMessage message, HttpServiceContext serviceContext, HttpChannelConfig config) {
        if (!initialized) {

            initialized = Boolean.TRUE;
            this.message = message;
            this.headers = message.headers();
            this.config = config;
            this.serviceContext = serviceContext;

            this.limitOnNumberOfHeaders = config.getLimitOnNumberOfHeaders();
            this.limitOfTokenSize = config.getLimitOfFieldSize();

        }
    }

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
        this.headersMap = new HashMap<>();

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
        // BNFHeaders reading of the headers will trigger all the parsed/temp
        // values at this layer
        try {
            if (SERIALIZATION_V2 == this.deserializationVersion) {
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
        boolean isTrailer = (SERIALIZATION_V2 == this.deserializationVersion) ? input.readBoolean() : (1 == input.readByte());
        if (isTrailer) {
            //TODO update with Netty Trailers

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

        output.writeInt(SERIALIZATION_V2);
        output.writeInt(this.headersMap.size());
        output.writeInt(this.headersMap.size());

        for (Map.Entry<String, String> entry : headersMap.entrySet()) {
            writeByteArray(output, entry.getKey().getBytes());
            writeByteArray(output, entry.getValue().getBytes());
        }

        writeByteArray(output, getVersionValue().getByteArray());

    }

    protected byte[] readByteArray(ObjectInput input) throws IOException {
        int length = input.readInt();
        byte[] data = new byte[length];
        input.readFully(data);
        return data;

    }

    protected void writeByteArray(ObjectOutput output, byte[] data) throws IOException {
        output.writeInt(data.length);
        output.write(data);
    }

    @Override
    public void setDebugContext(Object o) {
        throw new UnsupportedOperationException("setDebugContext unsupported in Netty context");

    }

    @Override
    public HeaderField getHeader(String name) {
        return new NettyHeader(name, headers);
    }

    @Override
    public HeaderField getHeader(byte[] name) {
        return getHeader(new String(name, StandardCharsets.UTF_8));
    }

    @Override
    public HeaderField getHeader(HeaderKeys name) {
        return new NettyHeader(name, headers);
    }

    @Override
    public List<HeaderField> getHeaders(String name) {
        List<String> values = headers.getAll(name);
        List<HeaderField> result = new ArrayList<HeaderField>();
        for (String value : values) {
            result.add(new NettyHeader(name, value));
        }

        return result;
    }

    @Override
    public List<HeaderField> getHeaders(byte[] name) {
        return getHeaders(new String(name, StandardCharsets.UTF_8));
    }

    @Override
    public List<HeaderField> getHeaders(HeaderKeys name) {
        return getHeaders(name.getName());
    }

    @Override
    public List<HeaderField> getAllHeaders() {
        List<Entry<String, String>> entries = headers.entries();
        List<HeaderField> headers = new ArrayList<HeaderField>();
        for (Entry<String, String> entry : entries) {
            headers.add(new NettyHeader(entry.getKey(), entry.getValue()));
        }
        return headers;
    }

    @Override
    public List<String> getAllHeaderNames() {
        return new ArrayList<String>(headers.names());
    }

    @Override
    public Set<String> getAllHeaderNamesSet() {
        return headers.names();
    }

    @Override
    public void appendHeader(byte[] header, byte[] value) {
        appendHeader(new String(header, StandardCharsets.UTF_8), new String(value, StandardCharsets.UTF_8));

    }

    @Override
    public void appendHeader(byte[] header, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("appendHeader(2) not supported in Netty context");

    }

    @Override
    public void appendHeader(byte[] header, String value) {
        appendHeader(new String(header, StandardCharsets.UTF_8), value);

    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value) {
        appendHeader(header, new String(value, StandardCharsets.UTF_8));

    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("appendHeader(5) not supported in Netty context");

    }

    @Override
    public void appendHeader(HeaderKeys header, String value) {
        appendHeader(header.getName(), value);

    }

    @Override
    public void appendHeader(String header, byte[] value) {
        appendHeader(header, new String(value, StandardCharsets.UTF_8));

    }

    @Override
    public void appendHeader(String header, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("appendHeader(8) not supported in Netty context");

    }

    @Override
    public void appendHeader(String header, String value) {
        headers.add(header, value);

    }

    @Override
    public int getNumberOfHeaderInstances(String header) {

        return headers.getAll(header).size();
    }

    @Override
    public boolean containsHeader(byte[] header) {
        return containsHeader(new String(header, StandardCharsets.UTF_8));

    }

    @Override
    public boolean containsHeader(HeaderKeys header) {
        return containsHeader(header.getName());
    }

    @Override
    public boolean containsHeader(String header) {
        return headers.contains(header);
    }

    @Override
    public int getNumberOfHeaderInstances(byte[] header) {
        return this.getNumberOfHeaderInstances(new String(header, StandardCharsets.UTF_8));

    }

    @Override
    public int getNumberOfHeaderInstances(HeaderKeys header) {
        return this.getNumberOfHeaderInstances(header.getName());
    }

    @Override
    public void removeHeader(byte[] header) {
        removeHeader(new String(header, StandardCharsets.UTF_8));

    }

    @Override
    public void removeHeader(byte[] header, int instance) {
        //TODO

    }

    @Override
    public void removeHeader(HeaderKeys header) {
        removeHeader(header.getName());

    }

    @Override
    public void removeHeader(HeaderKeys header, int instance) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(String header) {
        headers.remove(header);

    }

    @Override
    public void removeHeader(String header, int instance) {
        //TODO

    }

    @Override
    public void removeAllHeaders() {
        headers.clear();

    }

    @Override
    public void setHeader(byte[] header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(byte[] header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(byte[] header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(HeaderKeys header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(HeaderKeys header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(HeaderKeys header, String value) {
        setHeader(header.getName(), value);

    }

    @Override
    public HeaderField setHeaderIfAbsent(HeaderKeys header, String value) {

        Objects.requireNonNull(header);
        Objects.requireNonNull(value);

        if (!headers.contains(header.getName())) {
            headers.set(header.getName(), value);
        }
        //TODO HeaderField not used for netty, can we avoid creating an object here?
        return null;
    }

    @Override
    public void setHeader(String header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(String header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(String header, String value) {
        headers.set(header, value);

    }

    @Override
    public void setLimitOnNumberOfHeaders(int number) {
        this.limitOnNumberOfHeaders = number;

    }

    @Override
    public int getLimitOnNumberOfHeaders() {
        return this.limitOnNumberOfHeaders;
    }

    @Override
    public void setLimitOfTokenSize(int size) {
        this.limitOfTokenSize = size;

    }

    @Override
    public int getLimitOfTokenSize() {
        return this.limitOfTokenSize;
    }

    @Override
    public byte[] getCookieValue(String name) {
        if (name == null) { return null;}

        HttpCookie cookie = getCookie(name);
        if(cookie == null){ return null;}

        String value = cookie.getValue();
        if(value == null || value.isEmpty()){
            return null;
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Populates the provided {@code list} with all values associated to the 
     * given cookie {@code name} for the specific {@code header}.
     * 
     * This method checks the local cache for the specified header. If the header 
     * has not been parsed yet, the {@link #parseAllCookies(CookieCacheData, HttpHeaderKeys)}
     * is used to decode cookie values for that header. All matching cookie values
     * are returned and added to the provided list. 
     * 
     * @param name the name of the cookie whose value should be retrieved
     * @param header the {@link HttpHeaderKeys} indicating desired header name
     * @param list a modifiable {@link List} where matching cookie values will be added
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
     * This method is not supported in the base class. It is expected that 
     * children of this class should override it to handle specific
     * cookie headers. For instance, requests typically will use "Cookie";
     * responses typically "Set-Cookie". Calling this default will result 
     * in a {@link UnsupportedOperationException}
     * 
     * If needing to use the base class, use {@code #getCookie(String, HttpHeaderKeys)}
     * instead.
     * 
     * @param name the cookie name
     * @throws UnsupportedOperationException always if called from the 
     * base class.
     */
    @Override
    public HttpCookie getCookie(String name) {
        if (name == null) {
            return null;
        }

        HttpCookie cookie = null;
        if (isIncoming()) {
            cookie = getCookie(name, HttpHeaderKeys.HDR_COOKIE);
            if (cookie == null) {
                cookie = getCookie(name, HttpHeaderKeys.HDR_COOKIE2);
            }
        } else {
            cookie = getCookie(name, HttpHeaderKeys.HDR_SET_COOKIE);
            if (cookie == null) {
                cookie = getCookie(name, HttpHeaderKeys.HDR_SET_COOKIE2);
            }
        }

        return cookie;
    }

    /**
     * Retrieves a cookie with the specified {@code name} from the specified cookie header
     * type. This method checks whether the cookie has already been cached. If not cached,
     * it parses an unprocessed header field using the {@link CookieDecoder} and updates the local
     * cache. If a matching cookie is found, however, it returns it immediately.
     * 
     * If no cookie by the given name is found, if parsing results in no matches, or a {@code null} is
     * provided for the name, null is returned. 
     * 
     * Note: Cookies are cached depending on the header type. For instance, request cookies are 
     * typically associated to "Cookie"; response cookies with "Set-Cookie". Ensure the appropriate 
     * {@link HttpHeaderKeys} is utilized.
     * 
     * @param name the cookie name to look up
     * @param header the header key indicating which cookie header to parse
     * @return a matching {@link HttpCookie} if found, or {@code null} otherwise
     */
    protected HttpCookie getCookie(String name, HttpHeaderKeys header) {
        
        if(name == null) { return null;}

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
            List<HeaderField> headerFields = getHeaders(header);
            int size = headerFields.size();
            if (size != 0) {
                for (int i = cache.getHeaderIndex(); i < size; i++) {
                    String headerValue = headerFields.get(i).asString();
                    List<HttpCookie> decodedCookies = CookieDecoder.decode(headerValue, header);
                    cache.addParsedCookies(decodedCookies);
                    cache.incrementHeaderIndex();
                    // search the list of new cookies from this header instance
                    for(HttpCookie c: decodedCookies){
                        if (c.getName().equals(name)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Found parsed Cookie-->" + name);
                            }
                            return c;
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

    @Override
    public List<HttpCookie> getAllCookies() {
        List<HttpCookie> list = new LinkedList<HttpCookie>();

        if(isIncoming()){
            getAllCookies(HttpHeaderKeys.HDR_COOKIE, list);
            getAllCookies(HttpHeaderKeys.HDR_COOKIE2, list);
        }else{
            getAllCookies(HttpHeaderKeys.HDR_SET_COOKIE, list);
            getAllCookies(HttpHeaderKeys.HDR_SET_COOKIE2, list);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAllCookies: Found " + list.size() + " cookie(s). Is incoming: "+isIncoming());
        }
        return list;
    }

    @Override
    public List<HttpCookie> getAllCookies(String name) {
        List<HttpCookie> list = new LinkedList<HttpCookie>();
        if(name == null){ return list;}

        
        if (isIncoming()) {
            getAllCookies(name, HttpHeaderKeys.HDR_COOKIE, list);
            getAllCookies(name, HttpHeaderKeys.HDR_COOKIE2, list);
        } else {
            getAllCookies(name, HttpHeaderKeys.HDR_SET_COOKIE, list);
            getAllCookies(name, HttpHeaderKeys.HDR_SET_COOKIE2, list);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAllCookies: Found " + list.size() + 
                " cookie(s) matching [" + name + "]. Is incoming: "+ isIncoming());
        }
        return list;
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

    @Override
    public boolean setCookie(HttpCookie cookie, HttpHeaderKeys cookieType) {

        boolean result = Boolean.FALSE;
        if (isCommitted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Not adding cookie to committed message: " + cookie.getName() + " " + cookieType.getName());
            }

        } else {
            getCookieCache(cookieType).addNewCookie(cookie.clone());
            result = Boolean.TRUE;
        }
        return result;
    }

    @Override
    public boolean setCookie(String name, String value, HttpHeaderKeys cookieHeader) {
        return setCookie(new HttpCookie(name, value), cookieHeader);
    }

    @Override
    public boolean removeCookie(String name, HttpHeaderKeys cookieHeader) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "removeCookie: " + name);
        }
        boolean rc = Boolean.FALSE;
        if (isCommitted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Not removing committed cookie: " + name);
            }
        } else {
            // call getCookie in case we need to still parse anything
            HttpCookie cookie = getCookie(name, cookieHeader);
            if (null != cookie) {
                rc = getCookieCache(cookieHeader).removeCookie(cookie);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "deleteCookie: " + rc);
        }
        return rc;
    }

    @Override
    public boolean containsCookie(String name, HttpHeaderKeys cookieHeader) {
        boolean result = Boolean.FALSE;

        if (Objects.nonNull(name) && Objects.nonNull(cookieHeader)) {
            result = (Objects.nonNull(getCookie(name, cookieHeader)));
        }

        return result;
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
        List<HeaderField> headerList = getHeaders(header);
        int size = headerList.size();

        if(size == 0) {return;}

        for (int i = cache.getHeaderIndex(); i < size; i++) {
            String headerValue = headerList.get(i).asString();

                cache.addParsedCookies(CookieDecoder.decode(headerValue, header));
                cache.incrementHeaderIndex();
        }
    }

    

    @Override
    public boolean isIncoming() {
        return this.inbound;
    }

    public void incoming(boolean isInbound) {
        this.inbound = isInbound;
    }

    @Override
    public boolean isCommitted() {
        return this.committed;

    }

    @Override
    public void setCommitted() {
        this.committed = Boolean.TRUE;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    @Override
    public void clear() {

        this.cookieCache = null;
        this.cookie2Cache = null;
        this.setCookieCache = null;
        this.setCookie2Cache = null;
        this.committed = false;

    }

    @Override
    public void destroy() {
        clear();

    }

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

    @Override
    public boolean isBodyAllowed() {
        return (0 != getContentLength());
    }

    @Override
    public void setContentLength(long length) {
        if (HttpUtil.isTransferEncodingChunked(message))
            HttpUtil.setTransferEncodingChunked(message, false);
        HttpUtil.setContentLength(message, length);

    }

    @Override
    public long getContentLength() {

        return HttpUtil.isContentLengthSet(message) ? HttpUtil.getContentLength(message) : HttpGenerics.NOT_SET;

    }

    @Override
    public void setConnection(ConnectionValues value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConnection(ConnectionValues[] values) {
        // TODO Auto-generated method stub

    }

    @Override
    public ConnectionValues[] getConnection() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isKeepAliveSet() {
        return HttpUtil.isKeepAlive(message);
    }

    @Override
    public boolean isConnectionSet() {
        return this.containsHeader(HttpHeaderKeys.HDR_CONNECTION);
    }

    @Override
    public void setContentEncoding(ContentEncodingValues value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setContentEncoding(ContentEncodingValues[] values) {
        // TODO Auto-generated method stub

    }

    @Override
    public ContentEncodingValues[] getContentEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTransferEncoding(TransferEncodingValues value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Tried setting transfer encoding in Netty!");
    }

    @Override
    public void setTransferEncoding(TransferEncodingValues[] values) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Tried setting transfer encoding in Netty!");
    }

    @Override
    public TransferEncodingValues[] getTransferEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isChunkedEncodingSet() {
        return HttpUtil.isTransferEncodingChunked(message);
    }

    @Override
    public void setCurrentDate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setExpect(ExpectValues value) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] getExpect() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isExpect100Continue() {
        return HttpUtil.is100ContinueExpected(message);
    }

    @Override
    public String getMIMEType() {
        return HttpUtil.getMimeType(message).toString();
    }

    @Override
    public void setMIMEType(String type) {
        // TODO Auto-generated method stub

    }

    @Override
    public Charset getCharset() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCharset(Charset set) {
        // TODO Auto-generated method stub

    }

    @Override
    public HttpTrailers getTrailers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VersionValues getVersionValue() {
        if (message.headers().contains(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text())) {
            return VersionValues.V20;
        }
        return VersionValues.find(message.protocolVersion().text());
    }

    @Override
    public String getVersion() {
        if (message.headers().contains(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text())) {
            return VersionValues.V20.getName();
        }
        return this.message.protocolVersion().text();
    }

    @Override
    public void setVersion(VersionValues version) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setVersion(String version) throws UnsupportedProtocolVersionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setVersion(byte[] version) throws UnsupportedProtocolVersionException {
        // TODO Auto-generated method stub

    }

    @Override
    public HttpTrailersImpl createTrailers() {
        // TODO Auto-generated method stub
        return null;
    }

    protected boolean cookieCacheExists(HttpHeaderKeys header) {
        if (header == HttpHeaderKeys.HDR_COOKIE) {
            return (Objects.nonNull(cookieCache));
        }
//        if (header == HttpHeaderKeys.HDR_COOKIE2) {
//            return (Objects.nonNull(cookie2Cache));
//        }
        if (header == HttpHeaderKeys.HDR_SET_COOKIE) {
            return (Objects.nonNull(setCookieCache));
        }
//        if (header == HttpHeaderKeys.HDR_SET_COOKIE2) {
//            return (Objects.nonNull(setCookie2Cache));
//        }
        return Boolean.FALSE;
    }

    public void processCookies() {

        marshallCookieCache(this.cookieCache);
        marshallCookieCache(this.cookie2Cache);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Checking to see if we should mark the cookie cache as dirty - samesite is " + config.useSameSiteConfig()
                         + " doNotAllowDuplicateSetCookie is " + config.doNotAllowDuplicateSetCookies());
        }
        if (config.useSameSiteConfig() || config.doNotAllowDuplicateSetCookies()) {
            //If there are set-cookie and set-cookie2 headers and the respective cache hasn't been initialized,
            //do so and set it as dirty so the cookie parsing logic is run.
            if (this.containsHeader(HttpHeaderKeys.HDR_SET_COOKIE)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Marking set-cookie cache dirty");
                }
                getCookieCache(HttpHeaderKeys.HDR_SET_COOKIE).setIsDirty(true);
            }

            if (this.containsHeader(HttpHeaderKeys.HDR_SET_COOKIE2)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Marking set-cookie2 cache dirty");
                }
                getCookieCache(HttpHeaderKeys.HDR_SET_COOKIE2).setIsDirty(true);
            }

        }
        marshallCookieCache(this.setCookieCache);
        marshallCookieCache(this.setCookie2Cache);

    }

    private void marshallCookieCache(CookieCacheData cache) {

        if (null != cache && cache.isDirty()) {
            HttpHeaderKeys type = cache.getHeaderType();
            parseAllCookies(cache, type);
            removeHeader(type);
            marshallCookies(cache.getParsedList(), type);
            cache.setIsDirty(false);
        }
    }

    private void marshallCookies(List<HttpCookie> list, HeaderKeys header) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "marshallCookies");
        }

        HashMap<String, String> setCookieNames = new HashMap<String,String>();

        // convert each individual cookie into it's own header for clarity
        // Note: Set-Cookie header has comma separated cookies instead of semi-
        // colon separation (if cookies were to go into one single header instead
        // of multiple)
        for (HttpCookie cookie : list) {
            //Default version
            cookie.setVersion(0);
            //Add Samesite default config
            if (config.useSameSiteConfig() && cookie.getAttribute("samesite") == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No SameSite value has been added for [" + cookie.getName() + "], checking configuration for a match");
                }
                String sameSiteAttributeValue = null;
                Matcher m = null;

                //First attempt to match the name explicitly.
                if (config.getSameSiteCookies().containsKey(cookie.getName())) {
                    sameSiteAttributeValue = config.getSameSiteCookies().get(cookie.getName());
                }
                //If the only pattern is a standalone '*' avoid regex cost
                else if (config.onlySameSiteStar()) {
                    sameSiteAttributeValue = config.getSameSiteCookies().get(HttpConfigConstants.WILDCARD_CHAR);
                }

                else {
                    //Attempt to find a match amongst the configured SameSite patterns
                    for (Pattern p : config.getSameSitePatterns().keySet()) {
                        m = p.matcher(cookie.getName());
                        if (m.matches()) {
                            sameSiteAttributeValue = config.getSameSitePatterns().get(p);
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

                    // Set Partitioned Flag for SameSite=None Cookie
                    if (config.getPartitioned()
                        && sameSiteAttributeValue.equalsIgnoreCase(HttpConfigConstants.SameSite.NONE.getName())) {
                        if (cookie.getAttribute("partitioned") == null) { // null means no value has been set yet
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "[1] Setting the Partitioned attribute for SameSite=None");
                            }
                            cookie.setAttribute("partitioned", "");
                        }
                    }

                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "No SameSite configuration found");
                    }
                }
            }

            // If SameSite=None is set programmatically, but partitioned is set via server.xml, then add the partitioned attribute
            if (config.useSameSiteConfig() && cookie.getAttribute("samesite") != null) {
                boolean sameSiteNoneUsed = cookie.getAttribute("samesite").equalsIgnoreCase(HttpConfigConstants.SameSite.NONE.getName());
                if (config.getPartitioned() && sameSiteNoneUsed) {
                    if (cookie.getAttribute("partitioned") == null) { // null means no value has been set yet
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "[2] Setting the Partitioned attribute for SameSite=None");
                        }
                        cookie.setAttribute("partitioned", "");
                    }
                }
            }

            String partitionedValue = cookie.getAttribute("partitioned");
            if(partitionedValue != null && !partitionedValue.equalsIgnoreCase("false")){
                boolean sameSiteIsNotNone = true;
                if(cookie.getAttribute("samesite") != null){
                    sameSiteIsNotNone = !cookie.getAttribute("samesite").equalsIgnoreCase(HttpConfigConstants.SameSite.NONE.getName());
                }
                if(sameSiteIsNotNone){
                    cookie.setAttribute("partitioned", "false");
                }
            }

            if(cookie.getAttribute("samesite") != null && cookie.getAttribute("samesite").equals(HttpConfigConstants.SameSite.NONE.getName())){
                String userAgent = getServiceContext().getRequest().getHeader(HttpHeaderKeys.HDR_USER_AGENT).asString();
                if(userAgent != null && SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent)){
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && setCookieNames.containsKey(cookie.getName())) {
                        Tr.debug(tc, "Incompatible client for SameSite=None found with the following User-Agent: " + userAgent);
                    }
                    cookie.setAttribute("samesite", null);
                    if(partitionedValue != null){
                        cookie.setAttribute("partitioned", null);
                    }
                }
            }

            String value = CookieUtils.toString(cookie, header, config.isv0CookieDateRFC1123compat(),
                                                config.shouldSkipCookiePathQuotes());
            if (null != value) {

                //PI31734 start
                if (config.doNotAllowDuplicateSetCookies() && (header.getName().equals("Set-Cookie"))) {
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

        if (config.doNotAllowDuplicateSetCookies() && setCookieNames != null) {
            setCookieNames.values().forEach(value -> appendHeader(header, value));

        }
        //PI31734 end

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "marshallCookies");
        }
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public long getEndTime() {
        return this.endTime;
    }

    @Override
    public HttpServiceContext getServiceContext() {
        return this.serviceContext;
    }

    @Override
    public List<String> getAllCookieValues(String name) {
        // TODO Auto-generated method stub
        return null;
    }
}
