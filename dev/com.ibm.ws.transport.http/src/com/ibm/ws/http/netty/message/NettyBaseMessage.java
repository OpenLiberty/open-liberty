/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.message;

import static com.ibm.ws.http.netty.message.NettyBaseMessage.MessageType.REQUEST;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.exception.UnsupportedMethodException;
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

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.AsciiString;
import io.openliberty.http.constants.HttpGenerics;
import io.openliberty.http.netty.channel.utils.HeaderValidator;
import io.openliberty.http.netty.channel.utils.HeaderValidator.FieldType;
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
    private final Map<HttpHeaderKeys, CookieCacheData> cookieCacheMap = new HashMap<>();
    /** Reference to the cookie parser */
    private transient CookieHeaderByteParser cookieParser;

    /** Reference to the service context */
    private HttpServiceContext serviceContext;

    private int limitOfTokenSize;

    public enum MessageType {REQUEST, RESPONSE;}

    private MessageType messageType;

    private final Map<String, AsciiString> headerNameCache =  new HashMap<>(32);
    

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

    public MessageType messageType(){
        return this.messageType;
    }

    public MessageType setMessageType(MessageType messageType){
        return this.messageType = messageType;
    }

    
    @Override
    public void setDebugContext(Object o) {
        throw new UnsupportedOperationException("setDebugContext unsupported in Netty context");

    }

    private AsciiString processHeaderName(String name){
        String processedName = HeaderValidator.process(name, FieldType.NAME, config);
        return headerNameCache.computeIfAbsent(processedName, n -> AsciiString.cached(processedName));
    }

    private String processHeaderValue(String value){
        return HeaderValidator.process(value, FieldType.VALUE, config);
    }

    private void assertTotalHeaderCount(){
        if(limitOnNumberOfHeaders > 0 && headers.size() >= limitOnNumberOfHeaders){
            throw new IllegalStateException("Maximum header count (" + limitOnNumberOfHeaders + ") exceeded");
        }
    }

    private AsciiString toAsciiString(byte[] bytes){
        return new AsciiString(bytes, false);
    }

    @Override
    public HeaderField getHeader(String name) {
        return new NettyHeader(name, headers);
    }

    @Override
    public HeaderField getHeader(byte[] name) {
        return getHeader(toAsciiString(name).toString());
    }

    @Override
    public HeaderField getHeader(HeaderKeys name) {
        return new NettyHeader(name, headers);
    }

    @Override
    public List<HeaderField> getHeaders(String name) {

        List<String> values = headers.getAll(processHeaderName(name));
        if (values.isEmpty()) {
            return Collections.emptyList();
        }

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
    public void appendHeader(byte[] name, byte[] value) {
        appendHeader(toAsciiString(name), toAsciiString(value));

    }

    @Override
    public void appendHeader(byte[] name, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("appendHeader(2) not supported in Netty context");

    }

    @Override
    public void appendHeader(byte[] name, String value) {
        appendHeader(toAsciiString(name), processHeaderValue(value));

    }

    @Override
    public void appendHeader(HeaderKeys name, byte[] value) {
        appendHeader(processHeaderName(name.getName()), toAsciiString(value));

    }

    @Override
    public void appendHeader(HeaderKeys name, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("appendHeader(5) not supported in Netty context");

    }

    @Override
    public void appendHeader(HeaderKeys name, String value) {
        appendHeader(processHeaderName(name.getName()), processHeaderValue(value));

    }

    @Override
    public void appendHeader(String name, byte[] value) {
        appendHeader(processHeaderName(name), toAsciiString(value));

    }

    @Override
    public void appendHeader(String name, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("appendHeader(8) not supported in Netty context");

    }

    @Override
    public void appendHeader(String name, String value) {
        appendHeader(processHeaderName(name), processHeaderValue(value));
    }

    private void appendHeader(AsciiString name, CharSequence value){
        assertTotalHeaderCount();
        headers.add(name, value);
    }

    @Override
    public int getNumberOfHeaderInstances(String name) {

        return headers.getAll(processHeaderName(name)).size();
    }

    @Override
    public boolean containsHeader(byte[] name) {
        return headers.contains(toAsciiString(name));

    }

    @Override
    public boolean containsHeader(HeaderKeys name) {
        return containsHeader(name.getName());
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.contains(processHeaderName(name));
    }

    @Override
    public int getNumberOfHeaderInstances(byte[] name) {
        return headers.getAll(toAsciiString(name)).size();

    }

    @Override
    public int getNumberOfHeaderInstances(HeaderKeys name) {
        return this.getNumberOfHeaderInstances(name.getName());
    }

    @Override
    public void removeHeader(byte[] name) {
        headers.remove(toAsciiString(name));

    }

    @Override
    public void removeHeader(byte[] name, int instance) {
        removeHeader(toAsciiString(name).toString(), instance);
    }

    @Override
    public void removeHeader(HeaderKeys name) {
        removeHeader(name.getName());

    }

    @Override
    public void removeHeader(HeaderKeys name, int instance) {
        removeHeader(name.getName(), instance);

    }

    @Override
    public void removeHeader(String name) {
        headers.remove(processHeaderName(name));

    }

    @Override
    public void removeHeader(String name, int instance) {
        //Note: currently not a direct way to remove header by instance, 
        //so we do so manually. 

        if (instance < 0) {
            return;
        }

        AsciiString headerName = processHeaderName(name);

        List<String> all = headers.getAll(headerName);
        if (instance >= all.size()) {
            return;
        }
        headers.remove(headerName);
        for (int i = 0; i < all.size(); i++) {
            if (i != instance) {
                headers.add(headerName, all.get(i));
            }
        }

    }

    @Override
    public void removeAllHeaders() {
        headers.clear();

    }

    @Override
    public void setHeader(byte[] name, byte[] value) {
        setHeader(toAsciiString(name), toAsciiString(value));

    }

    @Override
    public void setHeader(byte[] name, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("This method is unsupported in the Netty implementation");

    }

    @Override
    public void setHeader(byte[] name, String value) {
        setHeader(toAsciiString(name), processHeaderValue(value));

    }

    @Override
    public void setHeader(HeaderKeys name, byte[] value) {
        setHeader(processHeaderName(name.getName()), toAsciiString(value));

    }

    @Override
    public void setHeader(HeaderKeys name, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("This method is unsupported in the Netty implementation");

    }

    @Override
    public void setHeader(HeaderKeys name, String value) {
        
        setHeader(processHeaderName(name.getName()), processHeaderValue(value));

    }

    @Override
    public HeaderField setHeaderIfAbsent(HeaderKeys name, String value) {

        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        AsciiString n = processHeaderName(name.getName());
        if (!headers.contains(n)) {
            assertTotalHeaderCount();
            headers.set(n, processHeaderValue(value));
        }
        return null;

    }

    @Override
    public void setHeader(String name, byte[] value) {
        setHeader(processHeaderName(name), toAsciiString(value));

    }

    @Override
    public void setHeader(String namer, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("This method is unsupported in the Netty implementation");

    }

    @Override
    public void setHeader(String name, String value) {
        setHeader(processHeaderName(name), processHeaderValue(value));

    }

    private void setHeader(AsciiString name, CharSequence value){
        if (!headers.contains(name)) {
            assertTotalHeaderCount();
        }
        headers.set(name, value);
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
        if(cache.isDirty()){
            parseAllCookies(cache, header);
        }
        
        cache.getAllCookieValues(name, list);
    }


    @Override
    public HttpCookie getCookie(String name) {
        if (name == null) {
            return null;
        }
        HttpCookie cookie = null;

        if (messageType == MessageType.REQUEST) {
            cookie = getCookie(name, HttpHeaderKeys.HDR_COOKIE);
            if (cookie == null) {
                cookie = getCookie(name, HttpHeaderKeys.HDR_COOKIE2);
            }
        } else if (messageType == MessageType.RESPONSE){
            cookie = getCookie(name, HttpHeaderKeys.HDR_SET_COOKIE);
            if (cookie == null) {
                cookie = getCookie(name, HttpHeaderKeys.HDR_SET_COOKIE2);
            }
        }

        return cookie;
    }


    protected HttpCookie getCookie(String name, HttpHeaderKeys header) {
        
        if(name == null) { return null;}

        if(!isValidCookieHeader(header) && !containsHeader(header)){
            return null;
        }

        CookieCacheData cache = getCookieCache(header);
        HttpCookie cookie = cache.getCookie(name);
        if(cookie != null){
            return cookie;
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCookie --> " + name + " of type " + header.getName() + " not found");
        }
        return null;
    }

    @Override
    public List<HttpCookie> getAllCookies() {
        List<HttpCookie> list = new LinkedList<HttpCookie>();

        if(messageType == MessageType.REQUEST){
            addAllCookies(HttpHeaderKeys.HDR_COOKIE, list);
            addAllCookies(HttpHeaderKeys.HDR_COOKIE2, list);
        }else if(messageType == MessageType.RESPONSE){
            addAllCookies(HttpHeaderKeys.HDR_SET_COOKIE, list);
            addAllCookies(HttpHeaderKeys.HDR_SET_COOKIE2, list);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAllCookies: Found " + list.size() + " cookie(s). Is incoming: "+isIncoming());
        }
        return list;
    }

    @Override
    public List<HttpCookie> getAllCookies(String name) {
        if(name==null) return Collections.emptyList();

        List<HttpCookie> results = new LinkedList<>();
        if(messageType == MessageType.REQUEST){
            addAllCookies(name, HttpHeaderKeys.HDR_COOKIE, results);
            addAllCookies(name, HttpHeaderKeys.HDR_COOKIE2, results);
        } else {
            addAllCookies(name, HttpHeaderKeys.HDR_SET_COOKIE, results);
            addAllCookies(name, HttpHeaderKeys.HDR_SET_COOKIE2, results);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAllCookies: Found " + results.size() + 
                " cookie(s) matching [" + name + "]. Is incoming: "+ isIncoming());
        }
        return results;
    }

    @Override
    public boolean setCookie(HttpCookie cookie, HttpHeaderKeys cookieType) {
        if (isCommitted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Not adding cookie to committed message: " + cookie.getName() + " " + cookieType.getName());
            }
            return false;
        }
        getCookieCache(cookieType).addNewCookie(cookie.clone());
        return true;
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
        if (isCommitted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Not removing committed cookie: " + name);
            }
            return false;
        }
        
        HttpCookie cookie = getCookie(name, cookieHeader);
        if(cookie == null){
            return false;
        }

        return getCookieCache(cookieHeader).removeCookie(cookie);
    }

    @Override
    public boolean containsCookie(String name, HttpHeaderKeys cookieHeader) {
        boolean result = Boolean.FALSE;

        if (Objects.nonNull(name) && Objects.nonNull(cookieHeader)) {
            result = (Objects.nonNull(getCookie(name, cookieHeader)));
        }

        return result;
    }

    
    protected CookieCacheData getCookieCache(HttpHeaderKeys header) {

        if(!isValidCookieHeader(header)){
            throw new IllegalArgumentException("Not a recognized cookie header: " + header.getName());
        }

        CookieCacheData cache = cookieCacheMap.computeIfAbsent(header, h -> new CookieCacheData(h));
        parseNewCookieLinesIfNeeded(cache);
        return cache;
    }


    private boolean isValidCookieHeader(HttpHeaderKeys header){
        return (header == HttpHeaderKeys.HDR_COOKIE || 
                header == HttpHeaderKeys.HDR_COOKIE2||
                header == HttpHeaderKeys.HDR_SET_COOKIE|| 
                header == HttpHeaderKeys.HDR_SET_COOKIE2);
    }

  
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
    public List<String> getAllCookieValues(String name) {
        // TODO Auto-generated method stub
        return null;
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

        cookieCacheMap.clear();
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
        if (value.getName().equalsIgnoreCase(HttpHeaderValues.CLOSE.toString())){
            message.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }else if (value.getName().equalsIgnoreCase(HttpHeaderValues.KEEP_ALIVE.toString())){
            message.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }     
    }

    @Override
    public void setConnection(ConnectionValues[] values) {
        message.headers().set(HttpHeaderNames.CONNECTION, values);
    }

    @Override
    public ConnectionValues[] getConnection() {
        List<String> connectionHeaders = message.headers().getAll(HttpHeaderNames.CONNECTION);
        ConnectionValues[] values = new ConnectionValues[connectionHeaders.size()];
        for (int i = 0; i < connectionHeaders.size(); i++) {
            String current = connectionHeaders.get(i);
            values[i] = ConnectionValues.match(current, 0, current.length());
        }
        return values;
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
        Objects.requireNonNull(value, "content-encoding");
        headers.set(HttpHeaderNames.CONTENT_ENCODING, value.getName());
    }

    @Override
    public void setContentEncoding(ContentEncodingValues[] values) {
        Objects.requireNonNull(values, "content-encoding");
        headers.remove(HttpHeaderNames.CONTENT_ENCODING);
        for (ContentEncodingValues value : values) {
            headers.add(HttpHeaderNames.CONTENT_ENCODING, value.getName());
        }
    }

    @Override
    public ContentEncodingValues[] getContentEncoding() {
        List<String> values = headers.getAll(HttpHeaderNames.CONTENT_ENCODING);
        ContentEncodingValues[] result = new ContentEncodingValues[values.size()];
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            result[i] = ContentEncodingValues.match(value, 0, value.length());
        }
        return result;
    }

    @Override
    public void setTransferEncoding(TransferEncodingValues value) {
        Objects.requireNonNull(value, "transfer-encoding");
        headers.set(HttpHeaderNames.TRANSFER_ENCODING, value.getName());

    }

    @Override
    public void setTransferEncoding(TransferEncodingValues[] values) {
        Objects.requireNonNull(values, "transfer-encoding");
        headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
        for (TransferEncodingValues value : values) {
            headers.add(HttpHeaderNames.TRANSFER_ENCODING, value.getName());
        }

    }

    @Override
    public TransferEncodingValues[] getTransferEncoding() {
        List<String> values = headers.getAll(HttpHeaderNames.TRANSFER_ENCODING);
        TransferEncodingValues[] result = new TransferEncodingValues[values.size()];
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            result[i] = TransferEncodingValues.match(value, 0, value.length());
        }
        return result;
    }

    @Override
    public boolean isChunkedEncodingSet() {
        return HttpUtil.isTransferEncodingChunked(message);
    }

    @Override
    public void setCurrentDate() {
         setHeader(HttpHeaderKeys.HDR_DATE, HttpDispatcher.getDateFormatter().getRFC1123TimeAsBytes(this.config.getDateHeaderRange()));

    }

    @Override
    public void setExpect(ExpectValues value) {
        Objects.requireNonNull(value, "expect");
        headers.set(HttpHeaderNames.EXPECT, value.getName());

    }

    @Override
    public byte[] getExpect() {
        String value = headers.get(HttpHeaderNames.EXPECT);
        return value == null ? null : value.getBytes(StandardCharsets.US_ASCII);
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
        Objects.requireNonNull(type, "mime type");
        headers.set(HttpHeaderNames.CONTENT_TYPE, type);

    }

    @Override
    public Charset getCharset() {
        CharSequence ct = headers.get(HttpHeaderNames.CONTENT_TYPE);
        return HttpUtil.getCharset(ct);
    }

    @Override
    public void setCharset(Charset charset) {
       if (charset == null) {
            return;
        }
        CharSequence ct = headers.get(HttpHeaderNames.CONTENT_TYPE);
        CharSequence mime = (ct == null ? HttpHeaderValues.TEXT_PLAIN : HttpUtil.getMimeType(ct));
        String newContentType = mime + "; charset=" + charset.name();
        headers.set(HttpHeaderNames.CONTENT_TYPE, newContentType);

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
        Objects.requireNonNull(version, "version");
        message.setProtocolVersion(HttpVersion.valueOf(version.getName()));

    }

    @Override
    public void setVersion(String version) throws UnsupportedProtocolVersionException {
        setVersion(VersionValues.find(version));

    }

    @Override
    public void setVersion(byte[] version) throws UnsupportedProtocolVersionException {
        setVersion(new String(version, StandardCharsets.US_ASCII));

    }

    @Override
    public HttpTrailersImpl createTrailers() {
        throw new UnsupportedOperationException("The createTrailers() method is unused in the Netty implementation");
    }

    

    public void processCookies() {

        if(MessageType.REQUEST == messageType){
            marshallCookieCache(cookieCacheMap.get(HttpHeaderKeys.HDR_COOKIE));
            marshallCookieCache(cookieCacheMap.get(HttpHeaderKeys.HDR_COOKIE2));
        }

        else if(MessageType.RESPONSE == messageType){
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
            marshallCookieCache(cookieCacheMap.get(HttpHeaderKeys.HDR_SET_COOKIE));
            marshallCookieCache(cookieCacheMap.get(HttpHeaderKeys.HDR_SET_COOKIE2));

        }
        

        

    }

    protected void marshallCookieCache(CookieCacheData cache) {

        if (cache == null || !cache.isDirty()){
            return;
        }

        HttpHeaderKeys type = cache.getHeaderType();
        parseNewCookieLinesIfNeeded(cache);
        removeHeader(type);
        marshallCookies(cache.getParsedList(), type);
        cache.setIsDirty(false);
    }

    private void marshallCookies(List<HttpCookie> list, HeaderKeys header) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "marshallCookies");
        }

        HashMap<String, String> setCookieNames = null;
        boolean filterDuplicates = config.doNotAllowDuplicateSetCookies() && 
            (header == HttpHeaderKeys.HDR_SET_COOKIE || header == HttpHeaderKeys.HDR_SET_COOKIE2);

        if(filterDuplicates){
            setCookieNames = new HashMap<String, String>();
        }

        String userAgent = null;
        if(getServiceContext() != null && getServiceContext().getRequest() != null){
            userAgent = getServiceContext().getRequest().getHeader(HttpHeaderKeys.HDR_USER_AGENT).asString();
        }

        for (HttpCookie cookie : list) {
            String value = CookieEncoder.encodeCookie(cookie, header, config, userAgent);
            
            if (null != value) {
                if(filterDuplicates){
                    setCookieNames.put(cookie.getName(), value);
                } else {
                    appendHeader(header, value);
                }
            }
        }

        if(filterDuplicates && setCookieNames != null){
            removeHeader((header.getName()));
            for(String headerValue: setCookieNames.values()){
                appendHeader(header, headerValue);
            }
        }

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

    /**
     * Decides if a cookie header should be parsed based on the message
     * type and whether it is inbound or outbound. The incoming is used in the
     * traditional sense, implying reading from the wire. This results in parse 
     * operations. When the incoming flag is false, the transport is expected
     * to write (marshall) instead. Therefore the conditions for parsing are:
     * 
     *  -> REQUEST: When the transport acts as a server, parse "Cookie" headers
     *  -> RESPONSE: When the transport acts as a client, parse "Set-Cookie" headers
     */
    private boolean shouldParseCookieHeader(HttpHeaderKeys header){

        if(header == HttpHeaderKeys.HDR_COOKIE || header == HttpHeaderKeys.HDR_COOKIE2){
            return (messageType == MessageType.REQUEST && isIncoming());
        }
        else{
            return (messageType == MessageType.RESPONSE && isIncoming());
        }
    }

    protected boolean cookieCacheExists(HttpHeaderKeys header) {

        return (cookieCacheMap.containsKey(header));

    }

    private void parseNewCookieLinesIfNeeded(CookieCacheData cache){
        if(!shouldParseCookieHeader(cache.getHeaderType())){
            return;
        }
        HttpHeaderKeys header = cache.getHeaderType();
        List<HeaderField> fields = getHeaders(header);
        int size = fields.size();
        for(int i = cache.getHeaderIndex(); i < size; i++){
            String lineValue = fields.get(i).asString();
            List<HttpCookie> decoded = CookieDecoder.decode(lineValue, header);
            cache.addParsedCookies(decoded);
            cache.incrementHeaderIndex();
        }
    }

    protected void addAllCookies(HttpHeaderKeys header, List<HttpCookie> list) {
        if (!containsHeader(header) && !cookieCacheExists(header)) {
            return;
        }
        CookieCacheData cache = getCookieCache(header);
        cache.getAllCookies(list);
    }

    
    protected void addAllCookies(String name, HttpHeaderKeys header, List<HttpCookie> list) {
        if (!containsHeader(header) && !cookieCacheExists(header)) {
            return;
        }
        CookieCacheData cache = getCookieCache(header);
        cache.getAllCookies(name, list);
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
        marshallCookieCache(cookieCacheMap.get(HttpHeaderKeys.HDR_COOKIE));
        marshallCookieCache(cookieCacheMap.get(HttpHeaderKeys.HDR_COOKIE2));
        marshallCookieCache(cookieCacheMap.get(HttpHeaderKeys.HDR_SET_COOKIE));
        marshallCookieCache(cookieCacheMap.get(HttpHeaderKeys.HDR_SET_COOKIE2));

        output.writeInt(SERIALIZATION_V2);

        int count = headers.size();
        output.writeInt(count);
        output.writeInt(count);

        for (Map.Entry<String, String> e : headers) { // ← String,String
            writeByteArray(output, e.getKey().getBytes(StandardCharsets.US_ASCII));
            writeByteArray(output, e.getValue().getBytes(StandardCharsets.US_ASCII));
        }

        writeByteArray(output, getVersionValue().getByteArray());

        //Trailers
        output.writeBoolean(false);
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
    
}
