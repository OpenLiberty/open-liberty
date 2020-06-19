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

package org.apache.cxf.jaxrs.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class CacheControlHeaderProvider implements HeaderDelegate<CacheControl> {

    public static final String CACHE_CONTROL_SEPARATOR_PROPERTY =
        "org.apache.cxf.http.cache-control.separator";
    private static final String DEFAULT_SEPARATOR = ",";

    private static final String COMPLEX_HEADER_EXPRESSION =
        "(([\\w-]+=\"[^\"]*\")|([\\w-]+=[\\w]+)|([\\w-]+))";
    private static final Pattern COMPLEX_HEADER_PATTERN =
        Pattern.compile(COMPLEX_HEADER_EXPRESSION);

    private static final String PUBLIC = "public";
    private static final String PRIVATE = "private";
    private static final String NO_CACHE = "no-cache";
    private static final String NO_STORE = "no-store";
    private static final String NO_TRANSFORM = "no-transform";
    private static final String MUST_REVALIDATE = "must-revalidate";
    private static final String PROXY_REVALIDATE = "proxy-revalidate";
    private static final String MAX_AGE = "max-age";
    private static final String SMAX_AGE = "s-maxage";
    private static final Message message = PhaseInterceptorChain.getCurrentMessage();  // Liberty change

    public CacheControl fromString(String c) {
        boolean isPrivate = false;
        List<String> privateFields = new ArrayList<>();
        boolean noCache = false;
        List<String> noCacheFields = new ArrayList<>();
        boolean noStore = false;
        boolean noTransform = false;
        boolean mustRevalidate = false;
        boolean proxyRevalidate = false;
        int maxAge = -1;
        int sMaxAge = -1;
        Map<String, String> extensions = new HashMap<>();

        String[] tokens = getTokens(c);
        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.startsWith(MAX_AGE)) {
                maxAge = Integer.parseInt(token.substring(MAX_AGE.length() + 1));
            } else if (token.startsWith(SMAX_AGE)) {
                sMaxAge = Integer.parseInt(token.substring(SMAX_AGE.length() + 1));
            } else if (token.startsWith(PUBLIC)) {
                // ignore
            } else if (token.startsWith(NO_STORE)) {
                noStore = true;
            } else if (token.startsWith(NO_TRANSFORM)) {
                noTransform = true;
            } else if (token.startsWith(MUST_REVALIDATE)) {
                mustRevalidate = true;
            } else if (token.startsWith(PROXY_REVALIDATE)) {
                proxyRevalidate = true;
            } else if (token.startsWith(PRIVATE)) {
                isPrivate = true;
                addFields(privateFields, token);
            }  else if (token.startsWith(NO_CACHE)) {
                noCache = true;
                addFields(noCacheFields, token);
            } else {
                String[] extPair = token.split("=");
                String value = extPair.length == 2 ? extPair[1] : "";
                extensions.put(extPair[0], value);
            }
        }

        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAge);
        cc.setSMaxAge(sMaxAge);
        cc.setPrivate(isPrivate);
        cc.getPrivateFields().addAll(privateFields);
        cc.setMustRevalidate(mustRevalidate);
        cc.setProxyRevalidate(proxyRevalidate);
        cc.setNoCache(noCache);
        cc.getNoCacheFields().addAll(noCacheFields);
        cc.setNoStore(noStore);
        cc.setNoTransform(noTransform);
        cc.getCacheExtension().putAll(extensions);

        return cc;
    }

    private String[] getTokens(String c) {
        if (c == null) {
            throw new IllegalArgumentException();
        }
        if (c.contains("\"")) {
            List<String> values = new ArrayList<>(4);
            Matcher m = COMPLEX_HEADER_PATTERN.matcher(c);
            while (m.find()) {
                String val = m.group().trim();
                if (val.length() > 0) {
                    values.add(val);
                }
            }
            return values.toArray(new String[0]);
        }
        String separator = getSeparator();
        return c.split(separator);
    }
    
    public String toString(CacheControl c) {       
        String separator = getSeparator();

        StringBuilder sb = new StringBuilder();
        if (c.isPrivate()) {
            sb.append(PRIVATE);
            handleFields(c.getPrivateFields(), sb);
            sb.append(separator);
        }
        if (c.isNoCache()) {
            sb.append(NO_CACHE);
            handleFields(c.getNoCacheFields(), sb);
            sb.append(separator);
        }
        if (c.isNoStore()) {
            sb.append(NO_STORE).append(separator);
        }
        if (c.isNoTransform()) {
            sb.append(NO_TRANSFORM).append(separator);
        }
        if (c.isMustRevalidate()) {
            sb.append(MUST_REVALIDATE).append(separator);
        }
        if (c.isProxyRevalidate()) {
            sb.append(PROXY_REVALIDATE).append(separator);
        }
        if (c.getMaxAge() != -1) {
            sb.append(MAX_AGE).append('=').append(c.getMaxAge()).append(separator);
        }
        if (c.getSMaxAge() != -1) {
            sb.append(SMAX_AGE).append('=').append(c.getSMaxAge()).append(separator);
        }
        Map<String, String> exts = c.getCacheExtension();
        for (Map.Entry<String, String> entry : exts.entrySet()) {
            sb.append(entry.getKey());
            String v = entry.getValue();
            if (v != null) {
                sb.append('=');
                if (v.indexOf(' ') != -1) {
                    sb.append('\"').append(v).append('\"');
                } else {
                    sb.append(v);
                }
            }
            sb.append(separator);
        }
        String s = sb.toString();
        return s.endsWith(separator) ? s.substring(0, s.length() - 1) : s;
    }

    private static void addFields(List<String> fields, String token) {
        int i = token.indexOf('=');
        if (i != -1) {
            String f = i == token.length() + 1 ? "" : token.substring(i + 1);
            if (f.length() < 2 || !f.startsWith("\"") || !f.endsWith("\"")) {
                return;
            }
            f = f.length() == 2 ? "" : f.substring(1, f.length() - 1);
            if (f.length() > 0) {
                String[] values = f.split(",");
                for (String v : values) {
                    fields.add(v.trim());
                }
            }
        }

    }

    private static void handleFields(List<String> fields, StringBuilder sb) {
        if (fields.isEmpty()) {
            return;
        }
        sb.append('=');
        sb.append('\"');
        for (Iterator<String> it = fields.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append('\"');
    }

    protected String getSeparator() { 

        String separator = DEFAULT_SEPARATOR;

     // Liberty change
        //Message message = getCurrentMessage();
        
        if (message != null) {
            Object sepProperty = message.getContextualProperty(CACHE_CONTROL_SEPARATOR_PROPERTY);
            if (sepProperty != null) {
                separator = sepProperty.toString().trim();
                if (separator.length() != 1) {
                    throw ExceptionUtils.toInternalServerErrorException(null, null);
                }
            }
        }
        return separator;
    }

    protected Message getCurrentMessage() {
        return PhaseInterceptorChain.getCurrentMessage();
    }
}
