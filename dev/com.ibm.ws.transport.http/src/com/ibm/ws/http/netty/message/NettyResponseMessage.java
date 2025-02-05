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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpTrailersImpl;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.exception.UnsupportedProtocolVersionException;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.ExpectValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.TransferEncodingValues;
import com.ibm.wsspi.http.channel.values.VersionValues;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.openliberty.http.netty.channel.utils.HeaderValidator;
import io.openliberty.http.netty.channel.utils.HeaderValidator.FieldType;
import io.openliberty.http.netty.cookie.CookieEncoder;

/**
 *
 */
public class NettyResponseMessage extends NettyBaseMessage implements HttpResponseMessage {

    /** RAS trace variable */
    private static final TraceComponent tc = Tr.register(NettyResponseMessage.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    HttpResponse nettyResponse;
    HttpHeaders headers;
    HttpHeaders trailers;
    NettyTrailers nettyTrailerWrapper;
    HttpInboundServiceContext context;
    HttpChannelConfig config;

    public NettyResponseMessage(HttpResponse response, HttpInboundServiceContext isc, HttpRequest request) {
        Objects.requireNonNull(isc);
        Objects.requireNonNull(response);

        this.context = isc;
        this.nettyResponse = response;
        this.headers = nettyResponse.headers();
        this.trailers = new DefaultHttpHeaders().clear();
        this.nettyTrailerWrapper = new NettyTrailers(this.trailers);

        if (request.headers().contains(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text())) {
            String streamId = request.headers().get(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
            nettyResponse.headers().set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);

        }

        if (isc instanceof HttpInboundServiceContextImpl) {
            incoming(((HttpInboundServiceContextImpl) isc).isInboundConnection());
            this.config = ((HttpInboundServiceContextImpl) isc).getHttpConfig();
        }

        super.init(response, context, config);
        setMessageType(MessageType.RESPONSE);

    }

    public void update(HttpResponse response) {
        this.nettyResponse = response;
        this.headers = response.headers();
    }

    @Override
    public void clear() {
        super.clear();
        this.setStatusCode(HttpResponseStatus.OK.code());
        this.nettyResponse.setProtocolVersion(HttpVersion.HTTP_1_1);

    }

    @Override
    public void destroy() {
        super.destroy();

    }

    @Override
    public boolean isBodyExpected() {

        if (VersionValues.V10.equals(getVersionValue())) {
            return isBodyAllowed();
        }

        if (HttpMethod.HEAD.toString().equals(getServiceContext().getRequest().getMethod())) {
            return false;
        }

        boolean bodyExpected = super.isBodyExpected();

        if (!bodyExpected) {
            bodyExpected = containsHeader(HttpHeaderNames.CONTENT_ENCODING.array()) || containsHeader(HttpHeaderNames.CONTENT_RANGE.array());
        }

        return bodyExpected && isBodyAllowedForStatusCode();

    }

    @Override
    public boolean isBodyAllowed() {
        if (super.isBodyAllowed()) {

            if (HttpMethod.HEAD.toString().equals(getServiceContext().getRequest().getMethod())) {
                return false;
            }
            return isBodyAllowedForStatusCode();
        }
        return false;
    }

    @Override
    public void setConnection(ConnectionValues value) {
        if (value.getName().equalsIgnoreCase(HttpHeaderValues.CLOSE.toString()))
            nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        else if (value.getName().equalsIgnoreCase(HttpHeaderValues.KEEP_ALIVE.toString()))
            nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }

    @Override
    public void setConnection(ConnectionValues[] values) {
        nettyResponse.headers().set(HttpHeaderNames.CONNECTION, values);
    }

    @Override
    public ConnectionValues[] getConnection() {
        List<String> test = nettyResponse.headers().getAll(HttpHeaderNames.CONNECTION);
        List<ConnectionValues> values = new ArrayList<ConnectionValues>();
        for (String header : test) {
            values.add(ConnectionValues.match(header, 0, header.length()));
        }
        return (ConnectionValues[]) values.toArray();
    }

    @Override
    public boolean isKeepAliveSet() {
        return HttpUtil.isKeepAlive(nettyResponse);
    }

    @Override
    public boolean isConnectionSet() {
        return this.containsHeader(HttpHeaderKeys.HDR_CONNECTION);
    }

    @Override
    public void setContentEncoding(ContentEncodingValues value) {
        //TODO delegate to pipeline

    }

    @Override
    public void setContentEncoding(ContentEncodingValues[] values) {
        // TODO delegate to pipeline

    }

    @Override
    public ContentEncodingValues[] getContentEncoding() {
        // TODO delegate to pipeline
        return null;
    }

    @Override
    public void setTransferEncoding(TransferEncodingValues value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTransferEncoding(TransferEncodingValues[] values) {
        // TODO Auto-generated method stub

    }

    @Override
    public TransferEncodingValues[] getTransferEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isChunkedEncodingSet() {
        return HttpUtil.isTransferEncodingChunked(nettyResponse);
    }

    @Override
    public void setCurrentDate() {

        setHeader(HttpHeaderKeys.HDR_DATE, HttpDispatcher.getDateFormatter().getRFC1123TimeAsBytes(this.config.getDateHeaderRange()));

    }

    @Override
    public void setExpect(ExpectValues value) {

    }

    @Override
    public byte[] getExpect() {
        return null;
    }

    @Override
    public boolean isExpect100Continue() {
        return HttpUtil.is100ContinueExpected(nettyResponse);
    }

    @Override
    public String getMIMEType() {
        return HttpUtil.getMimeType(nettyResponse).toString();
    }

    @Override
    public void setMIMEType(String type) {
        //TODO

    }

    @Override
    public Charset getCharset() {
        return null;
    }

    @Override
    public void setCharset(Charset set) {

    }

    @Override
    public HttpTrailers getTrailers() {
        return nettyTrailerWrapper;
    }

    public HttpHeaders getNettyTrailers() {
        // TODO Auto-generated method stub)
        return trailers;
    }

    @Override
    public void setVersion(VersionValues version) {

    }

    @Override
    public void setVersion(String version) throws UnsupportedProtocolVersionException {

    }

    @Override
    public void setVersion(byte[] version) throws UnsupportedProtocolVersionException {

    }

    @Override
    public HttpTrailersImpl createTrailers() {
        return null;
    }

    @Override
    public void setDebugContext(Object o) {

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

    }

    @Override
    public void appendHeader(byte[] header, String value) {
        appendHeader(new String(header, StandardCharsets.UTF_8), value);

    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value) {
        appendHeader(header.getName(), new String(value, StandardCharsets.UTF_8));

    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value, int offset, int length) {

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

    }

    @Override
    public void appendHeader(String header, String value) {
        String normalizedName = HeaderValidator.process(header, FieldType.NAME, config);
        String normalizedValue = HeaderValidator.process(value, FieldType.VALUE, config);

        headers.add(normalizedName, normalizedValue);

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
        return this.getNumberOfHeaderInstances(header.toString());
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

    }

    @Override
    public void setHeader(byte[] header, byte[] value, int offset, int length) {

    }

    @Override
    public void setHeader(byte[] header, String value) {

    }

    @Override
    public void setHeader(HeaderKeys header, byte[] value) {

    }

    @Override
    public void setHeader(HeaderKeys header, byte[] value, int offset, int length) {

    }

    @Override
    public void setHeader(HeaderKeys header, String value) {
        setHeader(header.getName(), value);

    }

    @Override
    public HeaderField setHeaderIfAbsent(HeaderKeys header, String value) {
        HeaderField hf;
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

    }

    @Override
    public void setHeader(String header, byte[] value, int offset, int length) {

    }

    @Override
    public void setHeader(String header, String value) {
        headers.set(header.trim(), value.trim());

    }

    @Override
    public void setLimitOnNumberOfHeaders(int number) {

    }

    @Override
    public int getLimitOnNumberOfHeaders() {

        return 0;
    }

    @Override
    public void setLimitOfTokenSize(int size) {

    }

    @Override
    public int getLimitOfTokenSize() {
        return 0;
    }

    @Override
    public int getStatusCodeAsInt() {
        return this.nettyResponse.status().code();
    }

    @Override
    public StatusCodes getStatusCode() {
        // TODO Auto-generated method stub
        return StatusCodes.getByOrdinal(getStatusCodeAsInt());
    }

    @Override
    public void setStatusCode(int code) {
        this.nettyResponse.setStatus(HttpResponseStatus.valueOf(code));

    }

    @Override
    public void setStatusCode(StatusCodes code) {
        setStatusCode(code.getIntCode());

    }

    @Override
    public String getReasonPhrase() {
        return null;
    }

    @Override
    public byte[] getReasonPhraseBytes() {
        return this.nettyResponse.status().reasonPhrase().getBytes();
    }

    @Override
    public void setReasonPhrase(String reason) {

    }

    @Override
    public void setReasonPhrase(byte[] reason) {

    }

    @Override
    public HttpResponseMessage duplicate() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public HttpServiceContext getServiceContext() {
        return this.context;
    }

    protected void processCookie(HttpCookie cookie, HeaderKeys header) {
        String result = null;
        if (Objects.nonNull(cookie) && Objects.nonNull(header)) {
            String userAgent = getServiceContext().getRequest().getHeader(HttpHeaderKeys.HDR_USER_AGENT).asString();
            result = CookieEncoder.encode(cookie, header, config, userAgent);

            if (Objects.nonNull(result)) {
                if (config.doNotAllowDuplicateSetCookies() && header.equals(HttpHeaderKeys.HDR_SET_COOKIE)) {
                    if (this.headers.contains(HttpHeaderKeys.HDR_SET_COOKIE.getName())) {
                        headers.set(header.getName(), result);
                    }
                } else {
                    headers.add(header.getName(), result);
                }
            }
        }

    }

    @Override
    public long getBytesWritten() {
        return this.getServiceContext().getNumBytesWritten();
    }

    private boolean isBodyAllowedForStatusCode() {
        //ixx (except 101), 204 and 304 responses must not include a message body
        int statusCode = getStatusCodeAsInt();
        if ((statusCode >= 100 && statusCode < 200) || statusCode == 204 || statusCode == 304) {
            return false;
        }
        return true;
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
        if (SERIALIZATION_V2 == deserializationVersion) {
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
        writeByteArray(output, this.getReasonPhraseBytes());
    }

    public HttpResponse getResponse() {
        return nettyResponse;
    }

}
