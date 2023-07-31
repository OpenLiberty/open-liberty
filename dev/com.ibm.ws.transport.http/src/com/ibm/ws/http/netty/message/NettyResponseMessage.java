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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import com.ibm.ws.http.channel.internal.HttpTrailersImpl;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.exception.UnsupportedProtocolVersionException;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.ExpectValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.TransferEncodingValues;
import com.ibm.wsspi.http.channel.values.VersionValues;

/**
 *
 */
public class NettyResponseMessage implements HttpResponseMessage {

    @Override
    public boolean isIncoming() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCommitted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCommitted() {
        // TODO Auto-generated method stub

    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isBodyExpected() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isBodyAllowed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setContentLength(long length) {
        // TODO Auto-generated method stub

    }

    @Override
    public long getContentLength() {
        // TODO Auto-generated method stub
        return 0;
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isConnectionSet() {
        // TODO Auto-generated method stub
        return false;
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
        // TODO Auto-generated method stub
        return false;
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getMIMEType() {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return null;
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

    @Override
    public void setDebugContext(Object o) {
        // TODO Auto-generated method stub

    }

    @Override
    public HeaderField getHeader(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HeaderField getHeader(byte[] name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HeaderField getHeader(HeaderKeys name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HeaderField> getHeaders(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HeaderField> getHeaders(byte[] name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HeaderField> getHeaders(HeaderKeys name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HeaderField> getAllHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getAllHeaderNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getAllHeaderNamesSet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void appendHeader(byte[] header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(byte[] header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(byte[] header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(HeaderKeys header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(String header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(String header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(String header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getNumberOfHeaderInstances(String header) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean containsHeader(byte[] header) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsHeader(HeaderKeys header) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsHeader(String header) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getNumberOfHeaderInstances(byte[] header) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getNumberOfHeaderInstances(HeaderKeys header) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void removeHeader(byte[] header) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(byte[] header, int instance) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(HeaderKeys header) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(HeaderKeys header, int instance) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(String header) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(String header, int instance) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAllHeaders() {
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub

    }

    @Override
    public HeaderField setHeaderIfAbsent(HeaderKeys header, String value) {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub

    }

    @Override
    public void setLimitOnNumberOfHeaders(int number) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getLimitOnNumberOfHeaders() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setLimitOfTokenSize(int size) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getLimitOfTokenSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte[] getCookieValue(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getAllCookieValues(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpCookie getCookie(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HttpCookie> getAllCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HttpCookie> getAllCookies(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setCookie(HttpCookie cookie, HttpHeaderKeys cookieHeader) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setCookie(String name, String value, HttpHeaderKeys cookieHeader) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeCookie(String name, HttpHeaderKeys cookieHeader) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsCookie(String name, HttpHeaderKeys cookieHeader) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getStatusCodeAsInt() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public StatusCodes getStatusCode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setStatusCode(int code) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setStatusCode(StatusCodes code) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getReasonPhrase() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getReasonPhraseBytes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setReasonPhrase(String reason) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setReasonPhrase(byte[] reason) {
        // TODO Auto-generated method stub

    }

    @Override
    public HttpResponseMessage duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

}
