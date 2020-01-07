/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.token.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.wsspi.security.oauth20.token.WSOAuth20Token;

public class WSOAuth20TokenImpl implements WSOAuth20Token, Serializable {

    private static final long serialVersionUID = -6131956027925854971L;

    static final TraceComponent tc = Tr.register(WSOAuth20TokenImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private boolean isReadOnly = false;
    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    private long expiration = 0l;
    private String principal = null;
    private boolean isvalid = true;
    private String cacheKey = null;
    private String tokenString = null;
    private String clientId = null;
    private String[] scopes = null;
    private String provider = null;

    public String getTokenString() {
        return this.tokenString;
    }

    public void setTokenString(String token) {
        this.tokenString = token;
    }

    public String getClientID() {
        return this.clientId;
    }

    public void setClientID(String client) {
        this.clientId = client;
    }

    public String[] getScope() {
        return this.scopes;
    }

    public void setScope(String[] scopes) {
        this.scopes = scopes;
    }

    public void addAttribute(String key, Object value) {
        if (key.startsWith("com.ibm.wsspi.security.oauth20")) {
            java.lang.SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                    Tr.debug(tc, "Expecting : " + WSOAuth20Token.UPDATE_OAUTH_PERM.toString());
                }
                sm.checkPermission(WSOAuth20Token.UPDATE_OAUTH_PERM);
            }
        }

        if (!isReadOnly) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Setting attribute with key: " + key + ", value: " + value);
            attributes.put(key, value);

        }
    }

    @SuppressWarnings("rawtypes")
    public Map getAttributes() {
        return this.attributes;
    }

    public String getProperty(String key) {
        String value = (String) attributes.get(key);
        return value;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);

    }

    public long getExpirationTime() {
        return this.expiration;
    }

    public void setExpirationTime(long t) {
        this.expiration = t;
    }

    public String getUser() {
        return this.principal;
    }

    public void setUser(String user) {
        this.principal = user;
    }

    public String getCacheKey() {
        return this.cacheKey;
    }

    public void setCacheKey(String key) {
        this.cacheKey = key;
    }

    public boolean isValid() {
        long now = (new Date()).getTime();
        return this.isvalid && (now > this.expiration);
    }

    public void inValidate() {
        this.isvalid = false;
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Provider:").append(this.provider).append(",");
        sb.append("user:").append(this.principal).append(",");
        sb.append("client:").append(this.clientId).append(",");
        sb.append("expiration:").append(new Date(this.expiration)).append(",");
        sb.append("token:").append(this.tokenString).append(",");
        if (this.scopes != null) {
            sb.append("scopes:");
            int size = this.scopes.length;
            for (int i = 0; i < size; i++) {
                sb.append(scopes[i]).append(",");
            }
        }
        return sb.toString();
    }

}
