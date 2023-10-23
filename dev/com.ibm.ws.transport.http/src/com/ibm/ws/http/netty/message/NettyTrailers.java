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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import com.ibm.ws.genericbnf.internal.BNFHeadersImpl;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.channel.HttpTrailerGenerator;
import com.ibm.wsspi.http.channel.HttpTrailers;

import io.netty.handler.codec.http.HttpHeaders;

/**
 *
 */
public class NettyTrailers implements HttpTrailers {

    private final HttpHeaders trailers;

    /**
     * Store all the known headers(key) and
     * their respective trailer generators(value)
     */
    private transient Map<String, HttpTrailerGenerator> knownTGs = new HashMap<String, HttpTrailerGenerator>();

    /**
     *
     */
    public NettyTrailers(HttpHeaders trailers) {
        this.trailers = trailers;
    }

    @Override
    public void setDebugContext(Object o) {
        throw new UnsupportedOperationException("setDebugContext cannot be called under a Netty perspective");
    }

    @Override
    public HeaderField getHeader(String name) {
        // TODO Auto-generated method stub
        if (Objects.isNull(name))
            throw new IllegalArgumentException("Null input provided");
        if (this.trailers.contains(name)) {
            return new NettyHeader(name, this.trailers.get(name));
        }
        return BNFHeadersImpl.NULL_HEADER;
    }

    @Override
    public HeaderField getHeader(byte[] name) {
        // TODO Auto-generated method stub
        if (Objects.isNull(name))
            throw new IllegalArgumentException("Null input provided");
        String nameString = new String(name);
        if (this.trailers.contains(nameString)) {
            return new NettyHeader(nameString, this.trailers.get(nameString));
        }
        return BNFHeadersImpl.NULL_HEADER;
    }

    @Override
    public HeaderField getHeader(HeaderKeys name) {
        // TODO Auto-generated method stub
        if (Objects.isNull(name))
            throw new IllegalArgumentException("Null input provided");
        if (this.trailers.contains(name.getName())) {
            return new NettyHeader(name.getName(), this.trailers.get(name.getName()));
        }
        return BNFHeadersImpl.NULL_HEADER;
    }

    @Override
    public List<HeaderField> getHeaders(String name) {
        // TODO Auto-generated method stub
        if (Objects.isNull(name))
            throw new IllegalArgumentException("Null input provided");
        List<String> values = this.trailers.getAll(name);
        List<HeaderField> trailerHeaderList = new ArrayList<HeaderField>(values.size());
        for (String value : values) {
            trailerHeaderList.add(new NettyHeader(name, value));
        }
        return trailerHeaderList;
    }

    @Override
    public List<HeaderField> getHeaders(byte[] name) {
        // TODO Auto-generated method stub
        if (Objects.isNull(name))
            throw new IllegalArgumentException("Null input provided");
        String nameString = new String(name);
        List<String> values = this.trailers.getAll(nameString);
        List<HeaderField> trailerHeaderList = new ArrayList<HeaderField>(values.size());
        for (String value : values) {
            trailerHeaderList.add(new NettyHeader(nameString, value));
        }
        return trailerHeaderList;
    }

    @Override
    public List<HeaderField> getHeaders(HeaderKeys name) {
        // TODO Auto-generated method stub
        if (Objects.isNull(name))
            throw new IllegalArgumentException("Null input provided");
        List<String> values = this.trailers.getAll(name.getName());
        List<HeaderField> trailerHeaderList = new ArrayList<HeaderField>(values.size());
        for (String value : values) {
            trailerHeaderList.add(new NettyHeader(name.getName(), value));
        }
        return trailerHeaderList;
    }

    @Override
    public List<HeaderField> getAllHeaders() {
        // TODO Auto-generated method stub
        List<HeaderField> trailerList = new ArrayList<HeaderField>(this.trailers.size());
        for (Entry<String, String> header : this.trailers) {
            trailerList.add(new NettyHeader(header.getKey(), header.getValue()));
        }
        return trailerList;
    }

    @Override
    public List<String> getAllHeaderNames() {
        // TODO Auto-generated method stub
        return new ArrayList<String>(this.trailers.names());
    }

    @Override
    public Set<String> getAllHeaderNamesSet() {
        // TODO Auto-generated method stub
        return this.trailers.names();
    }

    @Override
    public void appendHeader(byte[] header, byte[] value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.add(new String(header), value);
    }

    @Override
    public void appendHeader(byte[] header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("appendHeader with offset not supported yet!!");
//        if (Objects.isNull(header) || Objects.isNull(value))
//            throw new IllegalArgumentException("Null input provided");
//        this.trailers.add(new String(header),value);
    }

    @Override
    public void appendHeader(byte[] header, String value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.add(new String(header), value);
    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.add(header.getName(), value);
    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("appendHeader with offset not supported yet!!");
//        if (Objects.isNull(header) || Objects.isNull(value))
//            throw new IllegalArgumentException("Null input provided");
//        this.trailers.add(header.getName(),value);
    }

    @Override
    public void appendHeader(HeaderKeys header, String value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.add(header.getName(), value);
    }

    @Override
    public void appendHeader(String header, byte[] value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.add(header, value);
    }

    @Override
    public void appendHeader(String header, byte[] value, int offset, int length) {
        throw new UnsupportedOperationException("appendHeader with offset not supported yet!!");
        // TODO Auto-generated method stub
//        if (Objects.isNull(header) || Objects.isNull(value))
//            throw new IllegalArgumentException("Null input provided");
//        this.trailers.add(header,value);
    }

    @Override
    public void appendHeader(String header, String value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.add(header, value);
    }

    @Override
    public int getNumberOfHeaderInstances(String header) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header))
            throw new IllegalArgumentException("Null input provided");
        return this.trailers.getAll(header).size();
    }

    @Override
    public boolean containsHeader(byte[] header) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header))
            throw new IllegalArgumentException("Null input provided");
        return this.trailers.contains(new String(header));
    }

    @Override
    public boolean containsHeader(HeaderKeys header) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header))
            throw new IllegalArgumentException("Null input provided");
        return this.trailers.contains(header.getName());
    }

    @Override
    public boolean containsHeader(String header) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header))
            throw new IllegalArgumentException("Null input provided");
        return this.trailers.contains(header);
    }

    @Override
    public int getNumberOfHeaderInstances(byte[] header) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header))
            throw new IllegalArgumentException("Null input provided");
        return this.trailers.getAll(new String(header)).size();
    }

    @Override
    public int getNumberOfHeaderInstances(HeaderKeys header) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header))
            throw new IllegalArgumentException("Null input provided");
        return this.trailers.getAll(header.getName()).size();
    }

    @Override
    public void removeHeader(byte[] header) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.remove(new String(header));
    }

    @Override
    public void removeHeader(byte[] header, int instance) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("removeHeader with instance not supported yet!!");
//        if(Objects.isNull(header))
//            throw new IllegalArgumentException("Null input provided");
//        this.trailers.remove(new String(header));
    }

    @Override
    public void removeHeader(HeaderKeys header) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.remove(header.getName());
    }

    @Override
    public void removeHeader(HeaderKeys header, int instance) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("removeHeader with instance not supported yet!!");
//        if(Objects.isNull(header))
//            throw new IllegalArgumentException("Null input provided");
//        this.trailers.remove(header.getName());
    }

    @Override
    public void removeHeader(String header) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.remove(header);
    }

    @Override
    public void removeHeader(String header, int instance) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("removeHeader with instance not supported yet!!");
//        if(Objects.isNull(header))
//            throw new IllegalArgumentException("Null input provided");
//        this.trailers.remove(header);
    }

    @Override
    public void removeAllHeaders() {
        // TODO Auto-generated method stub
        this.trailers.clear();
    }

    @Override
    public void setHeader(byte[] header, byte[] value) {
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.set(new String(header), value);
    }

    @Override
    public void setHeader(byte[] header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("removeHeader with offset not supported yet!!");
//        if(Objects.isNull(header) || Objects.isNull(value))
//            throw new IllegalArgumentException("Null input provided");
//        this.trailers.set(new String(header), value);
    }

    @Override
    public void setHeader(byte[] header, String value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.set(new String(header), value);
    }

    @Override
    public void setHeader(HeaderKeys header, byte[] value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.set(header.getName(), value);
    }

    @Override
    public void setHeader(HeaderKeys header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("removeHeader with offset not supported yet!!");
//        if(Objects.isNull(header) || Objects.isNull(value))
//            throw new IllegalArgumentException("Null input provided");
//        this.trailers.set(header.getName(), value);
    }

    @Override
    public void setHeader(HeaderKeys header, String value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.set(header.getName(), value);
    }

    @Override
    public HeaderField setHeaderIfAbsent(HeaderKeys header, String value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        if (!this.trailers.contains(header.getName()))
            this.trailers.set(header.getName(), value);
        return new NettyHeader(header.getName(), trailers);
    }

    @Override
    public void setHeader(String header, byte[] value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.set(header, value);
    }

    @Override
    public void setHeader(String header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("removeHeader with offset not supported yet!!");
//        if(Objects.isNull(header) || Objects.isNull(value))
//            throw new IllegalArgumentException("Null input provided");
//        this.trailers.set(header, value);
    }

    @Override
    public void setHeader(String header, String value) {
        // TODO Auto-generated method stub
        if (Objects.isNull(header) || Objects.isNull(value))
            throw new IllegalArgumentException("Null input provided");
        this.trailers.set(header, value);
    }

    @Override
    public void setLimitOnNumberOfHeaders(int number) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("setLimitOnNumberOfHeaders not supported yet!!");
    }

    @Override
    public int getLimitOnNumberOfHeaders() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("getLimitOnNumberOfHeaders not supported yet!!");
    }

    @Override
    public void setLimitOfTokenSize(int size) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("setLimitOfTokenSize not supported yet!!");
    }

    @Override
    public int getLimitOfTokenSize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("getLimitOfTokenSize not supported yet!!");
    }

    @Override
    public boolean containsDeferredTrailer(String target) {
        // TODO Auto-generated method stub
        if (Objects.isNull(target))
            throw new IllegalArgumentException("Null input provided");
        return this.knownTGs.containsKey(target);
    }

    @Override
    public boolean containsDeferredTrailer(HeaderKeys target) {
        // TODO Auto-generated method stub
        if (Objects.isNull(target))
            throw new IllegalArgumentException("Null input provided");
        return this.knownTGs.containsKey(target.getName());
    }

    @Override
    public void setDeferredTrailer(HeaderKeys hdr, HttpTrailerGenerator htg) {
        // TODO Auto-generated method stub
        if (Objects.isNull(hdr) || Objects.isNull(htg))
            throw new IllegalArgumentException("Null input provided");
        this.knownTGs.put(hdr.getName(), htg);
    }

    @Override
    public void setDeferredTrailer(String hdr, HttpTrailerGenerator htg) {
        // TODO Auto-generated method stub
        if (Objects.isNull(hdr) || Objects.isNull(htg))
            throw new IllegalArgumentException("Null input provided");
        this.knownTGs.put(hdr, htg);
    }

    @Override
    public void removeDeferredTrailer(String hdr) {
        // TODO Auto-generated method stub
        if (Objects.isNull(hdr))
            throw new IllegalArgumentException("Null input provided");
        this.knownTGs.remove(hdr);
    }

    @Override
    public void removeDeferredTrailer(HeaderKeys hdr) {
        // TODO Auto-generated method stub
        if (Objects.isNull(hdr))
            throw new IllegalArgumentException("Null input provided");
        this.knownTGs.remove(hdr.getName());
    }

    @Override
    public void computeRemainingTrailers() {
        // TODO Auto-generated method stub
        Iterator<String> knowns = this.knownTGs.keySet().iterator();
        while (knowns.hasNext()) {
            String key = knowns.next();
            setHeader(key, this.knownTGs.get(key).generateTrailerValue(key, this));
        }
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        this.trailers.clear();
    }

}
