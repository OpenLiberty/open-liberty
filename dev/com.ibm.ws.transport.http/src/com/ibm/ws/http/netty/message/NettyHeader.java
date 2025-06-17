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
    private final AsciiString keyAscii;
    private final String cachedValue;

    public NettyHeader(String name, HttpHeaders headers) {

        this(HttpHeaderKeys.find(name, true), Objects.requireNonNull(headers),null);
    }

    public NettyHeader(HeaderKeys key, HttpHeaders headers) {
        this(key, Objects.requireNonNull(headers), null);
    }

    public NettyHeader(String name, String value) {
        this(HttpHeaderKeys.find(name, true), 
                null, 
                value == null? "":value);
    }

    private NettyHeader(HeaderKeys key, HttpHeaders headers, String value){
        this.key = Objects.requireNonNull(key, "key");
        this.keyAscii = AsciiString.cached(key.getName());
        this.nettyHeaders = headers;
        this.cachedValue = value;
    }


    @Override
    public String getName() {
        return key.getName();
    }

    @Override
    public HeaderKeys getKey() {
        return key;
    }

    @Override
    public String asString() {
        if(cachedValue != null){
            return cachedValue;
        }
        return nettyHeaders != null ? nettyHeaders.get(keyAscii) : null;
    }

    @Override
    public byte[] asBytes() {
        String header = asString();
        return header != null ? header.getBytes(StandardCharsets.US_ASCII): null;
    }

    @Override
    public Date asDate() throws ParseException {

        return HttpDispatcher.getDateFormatter().parseTime(asString());

    }

    @Override
    public int asInteger() throws NumberFormatException {
        
        CharSequence sequence;
        if(cachedValue != null){
            sequence = cachedValue;
        } else if(nettyHeaders != null){
            sequence = nettyHeaders.get(keyAscii);
        } else{
            sequence = null;
        }
        if(sequence == null || sequence.length() == 0){
            throw new NumberFormatException("Header value is null or empty");
        }
        if(sequence instanceof AsciiString){
            AsciiString asciiValue = (AsciiString) sequence;

            return asciiValue.parseInt(0, asciiValue.length(),10);
        }
        return Integer.parseInt(sequence.toString().trim());
    }

    @Override
    public List<byte[]> asTokens(byte delimiter) {
        throw new UnsupportedOperationException("Unused in Netty Context");

    }

}
