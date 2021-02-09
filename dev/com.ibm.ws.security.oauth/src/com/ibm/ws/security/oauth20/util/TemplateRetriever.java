/*******************************************************************************
 * Copyright (c) 1997, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;

public class TemplateRetriever {
    private static final String CLASS = TemplateRetriever.class.getName();
    private static TraceComponent tc = Tr.register(TemplateRetriever.class, "OAuth20Provider", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    public static final String HEADER_CONTENT_LANGUAGE = "Content-Language";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    private static Map<String, Item> templateCache = Collections.synchronizedMap(new HashMap<String, Item>());

    // default lifetime of template is 10 minutes if not specified
    public static final int dftTemplateLifeTime = Integer.parseInt(System.getProperty(CLASS + ".defaultTemplateLifetime", "600")); // 10 mins

    private static int lifetime = dftTemplateLifeTime;

    private static int templateConnectTimeMillis = Integer.parseInt(System.getProperty(CLASS + ".defaultTemplateConnectTimeMillis", "12000"));
    private static int templateReadTimeMillis = Integer.parseInt(System.getProperty(CLASS + ".defaultTemplateReadTimeMillis", "12000"));
    private static int templateCount = Integer.parseInt(System.getProperty(CLASS + ".defaultTemplateCount", "3"));
    private static Semaphore semaphore = null;

    public TemplateRetriever(OAuth20Provider provider) {
        if (semaphore == null) {
            semaphore = new Semaphore(templateCount);
        }
    }

    // @bj1 avoid apichk
    public TemplateRetriever() {
        this(null);
    }

    public Item getTemplate(String templateUrl, String acceptLanguage) throws IOException {
        String methodName = "getTemplate";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { templateUrl, acceptLanguage });
        }

        String key = getKey(templateUrl, getLanguage(acceptLanguage));
        Item retVal = templateCache.get(key);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "retrieve template from cache: " + retVal);
        }
        if (retVal != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "template will expire at " + new Date(retVal.getExpriation()));
            }
        }
        if (retVal == null || retVal.getExpriation() < System.currentTimeMillis()) {
            synchronized (templateCache) {
                retVal = templateCache.get(templateUrl);
                if (retVal == null || retVal.getExpriation() < System.currentTimeMillis()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "retrive form template from " + templateUrl);
                    }
                    retVal = getTemplateFromRemote(templateUrl, acceptLanguage);
                    if (retVal.getLanguage() != null) {
                        key = getKey(templateUrl, retVal.getLanguage());
                    }
                    templateCache.put(key, retVal);
                }
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, retVal);
        }
        return retVal;
    }

    protected URLConnection getConnection(URL url) throws IOException {

        if (url.getProtocol().contains("https")) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Protocol: " + url.getProtocol());
            }
            HttpsURLConnection conns = (HttpsURLConnection) url.openConnection();
            SSLSocketFactory factory = null;
            try {
                JSSEHelper jsseHelper = JSSEHelper.getInstance();
                SSLContext context = jsseHelper.getSSLContext(null, null, null);
                factory = context.getSocketFactory();
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to get SSL socket factory for connection to OP server, exception [" + e + "]");
                }
                throw new IOException(e.getMessage(), e);
            }
            conns.setSSLSocketFactory(factory);
            return conns;
        }

        return url.openConnection();
    }

    private Item getTemplateFromRemote(String templateUrl, String acceptLanguage) throws IOException {
        String methodName = "getTemplateFromRemote";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { templateUrl, acceptLanguage });
        }

        URL url = new URL(templateUrl);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        URLConnection conn = getConnection(url);
        if (acceptLanguage != null) {
            conn.setRequestProperty(HEADER_ACCEPT_LANGUAGE, acceptLanguage);
        }

        conn.setConnectTimeout(templateConnectTimeMillis);
        conn.setReadTimeout(templateReadTimeMillis);

        InputStream is = conn.getInputStream();

        byte[] buf = new byte[1024 * 4];
        int count = -1;
        while ((count = is.read(buf)) != -1) {
            baos.write(buf, 0, count);
        }
        is.close();
        baos.close();
        String contentLanguage = conn.getHeaderField(HEADER_CONTENT_LANGUAGE);
        String contentType = conn.getHeaderField(HEADER_CONTENT_TYPE);

        Item retVal = new Item(baos.toByteArray(), getLanguage(contentLanguage), contentType, lifetime);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "retrieved template lang=" + contentLanguage + " ,type=" + contentType + " len=" + retVal.getContentLength());
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, retVal);
        }
        return retVal;
    }

    public static class Item {
        private final byte[] content;
        private final long expiration;
        private final String language;
        private final String contentType;

        Item(byte[] content, String language, String contentType, int lifetimeInSecond) {
            this.content = content;
            this.language = language;
            this.expiration = System.currentTimeMillis() + lifetimeInSecond * 1000;
            this.contentType = contentType;
        }

        public Item(byte[] content, String contentType) {
            this.content = content.clone();
            this.contentType = contentType;
            this.language = null;
            this.expiration = 0;
        }

        public byte[] getContent() {
            return this.content.clone(); // for immutability
        }

        public long getExpriation() {
            return this.expiration;
        }

        public String getLanguage() {
            return this.language;
        }

        public String getContentType() {
            return this.contentType;
        }

        public int getContentLength() {
            return this.content.length;
        }
    }

    private String getKey(String templateUrl, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append(templateUrl);
        if (language != null) {
            sb.append("@").append(language);
        }
        return sb.toString();
    }

    private String getLanguage(String languageHeader) {
        String methodName = "getLanguage";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { languageHeader });
        }
        String retVal = null;
        if (languageHeader != null) {
            String[] langs = languageHeader.split(",");
            String[] parts = langs[0].split(";");
            retVal = parts[0].trim().toLowerCase();
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, retVal);
        }
        return retVal;
    }

    public static String normallizeTemplateUrl(HttpServletRequest request, String templateUrl) {
        String methodName = "normallizeTemplateUrl";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { templateUrl });
        }
        String retVal = templateUrl;

        if (retVal != null && !"".equals(retVal.trim()) && !retVal.startsWith("http://") && !retVal.startsWith("https://")) {
            String contextPath = request.getContextPath();
            String requestUrl = request.getRequestURL().toString();
            int pos = requestUrl.indexOf(contextPath);
            if (retVal.startsWith("/")) {
                retVal = requestUrl.substring(0, pos) + retVal;
            } else {
                retVal = requestUrl.substring(0, pos) + contextPath + "/" + retVal;
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, retVal);
        }
        return retVal;
    }
}
