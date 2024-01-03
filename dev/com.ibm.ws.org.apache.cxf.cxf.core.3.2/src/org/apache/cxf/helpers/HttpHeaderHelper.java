/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.helpers;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HttpHeaderHelper {
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_ID = "Content-ID";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String COOKIE = "Cookie";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String CHUNKED = "chunked";
    public static final String CONNECTION = "Connection";
    public static final String CLOSE = "close";
    public static final String AUTHORIZATION = "Authorization";
    static final String ISO88591 = StandardCharsets.ISO_8859_1.name();

    private static Map<String, String> internalHeaders = new HashMap<>();
    private static ConcurrentHashMap<String, String> encodings = new ConcurrentHashMap<>();

    static {
        internalHeaders.put("Accept-Encoding", "accept-encoding");
        internalHeaders.put("Content-Encoding", "content-encoding");
        internalHeaders.put("Content-Type", "content-type");
        internalHeaders.put("Content-ID", "content-id");
        internalHeaders.put("Content-Transfer-Encoding", "content-transfer-encoding");
        internalHeaders.put("Transfer-Encoding", "transfer-encoding");
        internalHeaders.put("Connection", "connection");
        internalHeaders.put("authorization", "Authorization");
        internalHeaders.put("soapaction", "SOAPAction");
        internalHeaders.put("accept", "Accept");
        internalHeaders.put("content-length", "Content-Length");
    }

    private HttpHeaderHelper() {

    }

    public static List<String> getHeader(Map<String, List<String>> headerMap, String key) {
        return headerMap.get(getHeaderKey(key));
    }

    public static String getHeaderKey(final String key) {
        String headerKey = internalHeaders.get(key);
        return headerKey == null ? key : headerKey;
    }

    public static String findCharset(String contentType) {
        if (contentType == null) {
            return null;
        }
        int idx = contentType.indexOf("charset=");
        if (idx != -1) {
            String charset = contentType.substring(idx + 8);
            if (charset.indexOf(';') != -1) {
                charset = charset.substring(0, charset.indexOf(';')).trim();
            }
            if (charset.isEmpty()) {
                return null;
            }
            if (charset.charAt(0) == '\"') {
                charset = charset.substring(1, charset.length() - 1);
            }
            return charset;
        }
        return null;
    }
    public static String mapCharset(String enc) {
        return mapCharset(enc, ISO88591);
    }

    //helper to map the charsets that various things send in the http Content-Type header
    //into something that is actually supported by Java and the Stax parsers and such.
    public static String mapCharset(String enc, String deflt) {
        if (enc == null) {
            return deflt;
        }
        //older versions of tomcat don't properly parse ContentType headers with stuff
        //after charset=StandardCharsets.UTF_8
        int idx = enc.indexOf(';');
        if (idx != -1) {
            enc = enc.substring(0, idx);
        }
        // Charsets can be quoted. But it's quite certain that they can't have escaped quoted or
        // anything like that.
        enc = enc.replace('"', ' ').replace('\'', ' ').trim();
        if (enc.isEmpty()) {
            return deflt;
        }
        String newenc = encodings.get(enc);
        if (newenc == null) {
            try {
                newenc = Charset.forName(enc).name();
            } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                return null;
            }
            String tmpenc = encodings.putIfAbsent(enc, newenc);
            if (tmpenc != null) {
                newenc = tmpenc;
            }
        }
        return newenc;
    }
}
