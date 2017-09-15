/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.internal;

import java.util.Enumeration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebSphereRuntimePermission;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public abstract class AbstractTokenImpl implements com.ibm.wsspi.security.token.Token {

    private final static TraceComponent tc = Tr.register(AbstractTokenImpl.class);
    private final static WebSphereRuntimePermission UPDATE_TOKEN = new WebSphereRuntimePermission("updateToken");
    private final static WebSphereRuntimePermission GET_TOKEN = new WebSphereRuntimePermission("getToken");

    private com.ibm.wsspi.security.ltpa.Token token = null;
    private boolean isReadOnly = false;
    private final short version = 1;
    private long change_counter = 0;

    public AbstractTokenImpl() {}

    public void setToken(com.ibm.wsspi.security.ltpa.Token _token) {
        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + UPDATE_TOKEN.toString());
            }
            sm.checkPermission(UPDATE_TOKEN);
        }

        change_counter++;
        token = _token;
    }

    public com.ibm.wsspi.security.ltpa.Token getToken() {
        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + GET_TOKEN.toString());
            }
            sm.checkPermission(GET_TOKEN);
        }

        return token;
    }

    /**
     * Validates the token including expiration, signature, etc.
     * 
     * @return boolean
     */
    public boolean isValid() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Checking validity of token " + this.getClass().getName());

        if (token != null) {
            // return if this token is still valid
            long currentTime = System.currentTimeMillis();
            long expiration = getExpiration();
            long timeleft = expiration - currentTime;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Time left for token: " + timeleft);

            if (timeleft > 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "token is valid.");
                return true;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "token is invalid.");
                return false;
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "token is null, returning invalid.");
            return false;
        }
    }

    /**
     * Gets the expiration as a long.
     * 
     * @return long
     */
    public long getExpiration() {
        if (token != null)
            return token.getExpiration();
        else
            return -1;
    }

    /**
     * Returns if this token should be forwarded/propagated downstream.
     * 
     * @return boolean
     */
    public boolean isForwardable() {
        return true;
    }

    /**
     * Gets the principal that this Token belongs to. If this is an authorization token,
     * this principal string must match the authentication token principal string or the
     * message will be rejected.
     * 
     * @return String
     */
    public String getPrincipal() {
        String[] accessIDArray = getAttributes("u");

        if (accessIDArray != null && accessIDArray.length > 0)
            return accessIDArray[0];
        else
            return null;
    }

    /**
     * Returns a unique identifier of the token based upon information the provider
     * considers makes this a unique token. This will be used for caching purposes
     * and may be used in combination with other token unique IDs that are part of
     * the same Subject.
     * 
     * This method should return null if you want the accessID of the user to represent
     * uniqueness. This is the typical scenario.
     * 
     * @return String
     */
    public String getUniqueID() {
        // return null so that this token does not change the uniqueness, 
        // all static attributes from the default tokens.
        String[] cacheKeyArray = getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);

        if (cacheKeyArray != null && cacheKeyArray[0] != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Found cache key in Authz token: " + cacheKeyArray[0]);
            return cacheKeyArray[0];
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "No unique cache key found in token.");
        return null;
    }

    /**
     * Gets the bytes to be sent across the wire. The information in the byte[]
     * needs to be enough to recreate the Token object at the target server.
     * 
     * @return byte[]
     */
    public byte[] getBytes() {
        if (token != null) {
            try {
                return token.getBytes();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception occurred getting bytes[] from token.", new Object[] { e });
                return null;
            }
        } else
            return new byte[0];
    }

    /**
     * Gets the name of the token, used to identify the byte[] in the protocol message.
     * 
     * @return String
     */
    public String getName() {
        return this.getClass().getName();
    }

    /**
     * Gets the version of the token as an short. This is also used to identify the
     * byte[] in the protocol message.
     * 
     * @return short
     */
    public short getVersion() {
        return version;
    }

    /**
     * If the authentication token is a basic auth token (contains userid/password),
     * then the boolean should return true, otherwise return false.
     * 
     * @return boolean
     */
    public boolean isBasicAuth() {
        return false;
    }

    /**
     * When called, the token becomes irreversibly read-only. The implementation
     * needs to ensure any setter methods check that this has been set.
     */
    public void setReadOnly() {
        isReadOnly = true;
    }

    /**
     * Gets the attribute value based on the named value.
     * 
     * @param String key
     * @return String[]
     */
    public String[] getAttributes(String key) {
        if (token != null)
            return token.getAttributes(key);
        else
            return null;
    }

    /**
     * Sets the attribute name/value pair. Returns the previous value set for key,
     * or null if not previously set.
     * 
     * @param String key
     * @param String value
     * @returns String[];
     */
    public String[] addAttribute(String key, String value) {
        if (key.startsWith("com.ibm.wsspi.security") || key.startsWith("com.ibm.websphere.security")) {
            java.lang.SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                    Tr.debug(tc, "Expecting : " + UPDATE_TOKEN.toString());
                }
                sm.checkPermission(UPDATE_TOKEN);
            }
        }

        if (!isReadOnly && token != null) {
            // change_counter is used by PropagationToken to determine uniqueness
            change_counter++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Setting attribute with key: " + key + ", value: " + value);
            return token.addAttribute(key, value);
        } else
            return null;
    }

    /**
     * Gets the List of all attribute names present in the token.
     * 
     * @return java.util.Enumeration
     */
    @SuppressWarnings("unchecked")
    public Enumeration getAttributeNames() {
        if (token != null)
            return token.getAttributeNames();
        else
            return null;
    }

    @Override
    public abstract Object clone();
}
