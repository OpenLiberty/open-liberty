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

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.ibm.io.async.IAsyncProvider.AsyncIOHelper;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;

/**
 * Wrapper for HeaderField compatibility within the transport
 */
public class NettyHeader implements HeaderField {

    private final HttpHeaders nettyHeaders;
    private final HeaderKeys key;
    private final AsciiString name;
    private volatile CharSequence value;

    public NettyHeader(String name, HttpHeaders headers) {

        this(HttpHeaderKeys.find(name, true), headers,null);
    }

    public NettyHeader(HeaderKeys key, HttpHeaders headers) {
        this(key, headers, null);
    }

    public NettyHeader(String name, String value) {
        this(HttpHeaderKeys.find(name, true), null, value);
    }

    private NettyHeader(HeaderKeys key, HttpHeaders headers, String value){
        this.key = Objects.requireNonNull(key, "key");
        this.name = AsciiString.cached(key.getName());
        this.nettyHeaders = headers;
        this.value = value;
    }


    @Override
    public String getName() {
        return key.toString();
    }

    @Override
    public HeaderKeys getKey() {
        return key;
    }

    @Override
    public String asString() {
        CharSequence value = lazyValue();
        return value == null ? null : value.toString();
    }

    @Override
    public byte[] asBytes() {
        CharSequence value = lazyValue();
        if(value == null) return null;

        if(value instanceof AsciiString){
            return ((AsciiString)value).array();
        }
        return value.toString().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public Date asDate() throws ParseException {

        return HttpDispatcher.getDateFormatter().parseTime(asString());

    }

    @Override
    public int asInteger() throws NumberFormatException {
        
        CharSequence value = lazyValue();
        if(value == null || value.length() == 0){
            throw new NumberFormatException("Header value is null or empty");
        }
        if (value instanceof AsciiString) {
            AsciiString v = (AsciiString) value;
            return v.parseInt(0, v.length(), 10);
        }
        return Integer.parseInt(value.toString().trim());
    }

    @Override
    public List<byte[]> asTokens(byte delimiter) {
        throw new UnsupportedOperationException("Unused in Netty Context");

    }

    /**
     * Cache the value only after the first time it is requested
     */
    private CharSequence lazyValue() {
        CharSequence v = value;
        if (v == null && nettyHeaders != null) {
            v = nettyHeaders.get(name);
            value = v; 
        }
        return v;
    }

}
