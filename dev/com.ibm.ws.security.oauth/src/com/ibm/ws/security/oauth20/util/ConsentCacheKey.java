/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.io.Serializable;
import java.util.Date;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ConsentCacheKey implements Serializable {

    private static final long serialVersionUID = 3321880892411013256L;
    private final static transient TraceComponent tc = Tr.register(ConsentCacheKey.class);

    private String clientId;
    private String redirectUri;
    private String scope;
    private String resourceId;
    private Date validSince;
    private int keyLifetimeSeconds;

    public ConsentCacheKey(String clientId, String redirectUri, String scope, String resourceId, int keyLifetimeSeconds) {
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.resourceId = resourceId;
        if (keyLifetimeSeconds < 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Key lifetime cannot be negative. Key lifetime will be set to 0.");
            }
            this.keyLifetimeSeconds = 0;
        } else {
            this.keyLifetimeSeconds = keyLifetimeSeconds;
        }
        validSince = new Date();
    }

    public int getLifetime() {
        return keyLifetimeSeconds;
    }

    public boolean isValid() {
        long currentTime = new Date().getTime();
        long expiration = getExpiration();
        long timeleft = expiration - currentTime;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Time left for key: " + timeleft);
        }

        if (timeleft > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Key is valid.");
            }
            return true;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Key is invalid.");
            }
            return false;
        }
    }

    public long getExpiration() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Key valid since: " + validSince.getTime());
        }
        return (validSince.getTime() + (keyLifetimeSeconds * 1000L));
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result + ((redirectUri == null) ? 0 : redirectUri.hashCode());
        result = prime * result + ((resourceId == null) ? 0 : resourceId.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConsentCacheKey other = (ConsentCacheKey) obj;
        if (clientId == null) {
            if (other.clientId != null)
                return false;
        } else if (!clientId.equals(other.clientId))
            return false;
        if (redirectUri == null) {
            if (other.redirectUri != null)
                return false;
        } else if (!redirectUri.equals(other.redirectUri))
            return false;
        if (resourceId == null) {
            if (other.resourceId != null)
                return false;
        } else if (!resourceId.equals(other.resourceId))
            return false;
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        return true;
    }

}
