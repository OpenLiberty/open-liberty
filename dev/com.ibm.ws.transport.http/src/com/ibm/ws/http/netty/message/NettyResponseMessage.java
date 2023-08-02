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

import java.util.HashMap;
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpResponseMessageImpl;
import com.ibm.ws.http.channel.internal.WsByteBuffer;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;

/**
 *
 */
public class NettyResponseMessage extends HttpResponseMessageImpl {

    /** RAS trace variable */
    private static final TraceComponent tc = Tr.register(NettyResponseMessage.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    HttpResponse nettyResponse;
    HttpHeaders headers;

    public NettyResponseMessage(HttpResponse response, HttpInboundServiceContext isc) {
        Objects.requireNonNull(isc);
        Objects.requireNonNull(response);

        setOwner(isc);
        this.nettyResponse = response;
        this.headers = nettyResponse.headers();

    }

    @Override
    protected void setPseudoHeaders(HashMap<String, String> pseudoHeaders) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setPseudoHeaders", "HTTP/2 delegated to Netty pipeline");
        }
        //No-op HTTP/2 handled by Netty Pipeline
    }

    @Override
    protected H2HeaderTable getH2HeaderTable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getH2HeaderTable", "HTTP/2 delegated to Netty pipeline, returning null");
        }
        return null;
    }

    @Override
    protected boolean isValidPseudoHeader(H2HeaderField pseudoHeader) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isValidPseudoHeader", "HTTP/2 delegated to Netty pipeline, returning false");
        }

        return Boolean.FALSE;
    }

    @Override
    protected boolean checkMandatoryPseudoHeaders(HashMap<String, String> pseudoHeaders) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "checkMandatoryPseudoHeaders", "HTTP/2 delegated to Netty pipeline, returning false");
        }
        return Boolean.FALSE;
    }

    @Override
    public WsByteBuffer[] encodePseudoHeaders() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "encodePseudoHeaders", "HTTP/2 delegated to Netty pipeline, returning null");
        }
        return null;
    }

    /**
     * Query of the value of the status code as an integer.
     *
     * @return int
     */
    @Override
    final public int getStatusCodeAsInt() {
        return this.nettyResponse.status().code();
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

        this.nettyResponse.setStatus(HttpResponseStatus.valueOf(code));

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
        super.setStatusCode(code);

        this.nettyResponse.setStatus(HttpResponseStatus.valueOf(code.getIntCode()));
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
        HttpUtil.setContentLength(nettyResponse, length);

    }

    /**
     * Query the value of the Content-Length header as a byte number.
     *
     * @return int
     */
    @Override
    public long getContentLength() {
        return HttpUtil.getContentLength(nettyResponse);

    }

    @Override
    public void setHeader(HeaderKeys key, String value) {
        if (Objects.isNull(key) || Objects.isNull(value)) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader(h,s): " + key.getName());
        }
        headers.set(key.getName(), value);
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#setHeaderIfAbsent(HeaderKeys, String)
     */
    @Override
    public HeaderField setHeaderIfAbsent(HeaderKeys key, String value) {
        if (Objects.isNull(key) || Objects.isNull(value)) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeaderIfAbsent(h,s): " + key.getName());
        }

        if (!this.nettyResponse.headers().contains(key.getName())) {
            headers.set(key.getName(), value);
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
        this.nettyResponse.headers().set(header, value);

    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#removeHeader(HeaderKeys)
     */
    @Override
    public void removeHeader(HeaderKeys key) {
        if (Objects.isNull(key)) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHeader(h): " + key.getName());
        }
        this.nettyResponse.headers().remove(key.getName());
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderStorage#removeHeader(String)
     */
    @Override
    public void removeHeader(String header) {
        if (Objects.isNull(header)) {
            throw new IllegalArgumentException("Null input provided");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHeader(s): " + header);
        }
        this.nettyResponse.headers().remove(header);
        super.removeAllHeaders()
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#removeAllHeaders()
     */
    @Override
    public void removeAllHeaders() {
        this.cookieCache = null;
        this.cookie2Cache = null;
        this.setCookieCache = null;
        this.setCookie2Cache = null;
        this.message.removeAllHeaders();
    }

    /*
     * 
     * //
     */
//   @Override
//   public void setHeader(String name, String value) {
//       this.message.setHeader(name, value);
//   }
//
//   /*
//    * @see com.ibm.websphere.http.HttpResponseExt#setHeader(com.ibm.wsspi.http.channel.values.HttpHeaderKeys, java.lang.String)
//    */
//   @Override
//   public void setHeader(HttpHeaderKeys key, String value) {
//       this.message.setHeader(key, value);
//   }
//
//   /*
//    * @see com.ibm.websphere.http.HttpResponseExt#setHeaderIfAbsent(com.ibm.wsspi.http.channel.values.HttpHeaderKeys, java.lang.String)
//    */
//   @Override
//   public String setHeaderIfAbsent(HttpHeaderKeys key, String value) {
//       HeaderField oldValue = this.message.setHeaderIfAbsent(key, value);
//       return oldValue == null ? null : oldValue.asString();
//   }

}
