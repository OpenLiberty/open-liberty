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

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * Wrapper for HeaderField compatibility within the transport
 */
public class NettyHeader implements HeaderField {

    HttpHeaders nettyHeaders;
    String name;
    String value;
    HeaderKeys key;

    public NettyHeader(String name, HttpHeaders nettyHeaders) {

        Objects.nonNull(name);
        this.name = name;

        Objects.nonNull(nettyHeaders);
        this.nettyHeaders = nettyHeaders;

        this.key = HttpHeaderKeys.find(name, Boolean.TRUE);
    }

    public NettyHeader(HeaderKeys key, HttpHeaders headers) {
        Objects.nonNull(key);
        this.key = key;
        this.name = key.getName();

        Objects.nonNull(headers);
        this.nettyHeaders = headers;

    }

    public NettyHeader(String name, String value) {
        Objects.nonNull(name);
        this.name = name;

        this.value = Objects.isNull(value) ? "" : value;
        this.key = HttpHeaderKeys.find(name, Boolean.TRUE);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HeaderKeys getKey() {
        return key;
    }

    @Override
    public String asString() {

        return (Objects.nonNull(value)) ? this.value : nettyHeaders.get(name);
    }

    @Override
    public byte[] asBytes() {
        String header = asString();
        if (Objects.nonNull(header)) {
            return header.getBytes();
        }
        return null;
    }

    @Override
    public Date asDate() throws ParseException {

        return HttpDispatcher.getDateFormatter().parseTime(asString());

    }

    @Override
    public int asInteger() throws NumberFormatException {
        return nettyHeaders.getInt(name);
    }

    @Override
    public List<byte[]> asTokens(byte delimiter) {
        throw new UnsupportedOperationException("Unused in Netty Context");

    }

}
