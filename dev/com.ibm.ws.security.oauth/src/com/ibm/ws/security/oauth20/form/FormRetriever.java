/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.form;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class FormRetriever {
    private static final String CLASS = FormRetriever.class.getName();
    private static TraceComponent tc = Tr.register(FormRetriever.class,
            "OAuth20Provider", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    public static final String HEADER_CONTENT_LANGUAGE = "Content-Language";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    private static Map<String, Item> templateCache = new HashMap<String, Item>();

    // default lifetime of template is 10 minutes if not specified
    public static final int dftTemplateLifeTime = Integer.parseInt(System
            .getProperty(CLASS + ".defaultTemplateLifetime", "600"));

    private final int lifetime = dftTemplateLifeTime;

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
                Tr.debug(tc, "template will expire at "
                        + new Date(retVal.getExpriation()));
            }
        }
        if (retVal == null
                || retVal.getExpriation() < System.currentTimeMillis()) {
            synchronized (templateCache) {
                retVal = templateCache.get(templateUrl);
                if (retVal == null
                        || retVal.getExpriation() < System.currentTimeMillis()) {
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

    private Item getTemplateFromRemote(String templateUrl, String acceptLanguage) throws IOException {
        String methodName = "getTemplateFromRemote";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { templateUrl, acceptLanguage });
        }

        Item retVal = null;
        URL url = new URL(templateUrl);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        URLConnection conn = url.openConnection();
        if (acceptLanguage != null) {
            conn.setRequestProperty(HEADER_ACCEPT_LANGUAGE, acceptLanguage);
        }
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

        retVal = new Item(baos.toByteArray(), getLanguage(contentLanguage), contentType, lifetime);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "retrieved template lang=" + contentLanguage + " ,type=" + contentType + " len=" + retVal.getContent().length);
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, retVal);
        }
        return retVal;
    }

    static class Item {
        private final byte[] content;
        private final long expiration;
        private final String language;
        private final String contentType;

        Item(byte[] content, String language, String contentType, int lifetimeInSecond) {
            this.content = content;
            this.language = language;
            this.expiration = System.currentTimeMillis() + lifetimeInSecond
                    * 1000;
            this.contentType = contentType;
        }

        byte[] getContent() {
            return this.content;
        }

        long getExpriation() {
            return this.expiration;
        }

        String getLanguage() {
            return this.language;
        }

        String getContentType() {
            return this.contentType;
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
}
