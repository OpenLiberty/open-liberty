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

package com.ibm.ws.security.oauth20.jwt;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.util.BoundedCommonCache;

public class JwtGrantTypeHandlerConfig {
    private static final TraceComponent tc = Tr.register(JwtGrantTypeHandlerConfig.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);
    OAuth20Provider _config = null;
    Map<String, Object> _customizedAttribs = new HashMap<String, Object>();
    private String _providerId = null;
    static HashMap<String, BoundedCommonCache<String>> _mapCaches = new HashMap<String, BoundedCommonCache<String>>();

    /**
     * Collect the config info of customized GrantTypeHandler during configuration time
     *
     * @param configAdmin - configuration admin
     * @param config      - the customizedGrantTypeHandler config data
     */
    public JwtGrantTypeHandlerConfig(String providerId, OAuth20Provider config) {
        _providerId = providerId;
        _config = config;
    }

    public String getProviderId() {
        return _providerId;
    }

    public SecurityService getSecurityService() {
        //return ConfigUtils.getSecurityService(_providerId);
        return _config.getSecurityService();
    }

    public long getJwtClockSkew() {
        long lJwtSkew = _config.getJwtClockSkew();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "jwt skew seconds: " + lJwtSkew);
        }
        return lJwtSkew;
    }

    public long getJwtMaxJtiCacheSize() {
        long cacheSize = _config.getJwtMaxJtiCacheSize();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JwtMaxJtiCacheSiz: " + cacheSize);
        }
        return cacheSize;
    }

    public long getJwtTokenMaxLifetime() {
        long lLifetime = _config.getJwtTokenMaxLifetime();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JwtMaxLifetimeAllowed seconds: " + lLifetime);
        }
        return lLifetime;
    }

    public boolean isJwtIatRequired() {
        boolean iatRequired = _config.getJwtIatRequired();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isJwtIatRequired: " + iatRequired);
        }
        return iatRequired;
    }

    public BoundedCommonCache<String> getJtiCache() {
        long lMaxSize = _config.getJwtMaxJtiCacheSize();
        BoundedCommonCache<String> jtiCache = _mapCaches.get(_providerId);

        if (jtiCache == null) {
            jtiCache = new BoundedCommonCache<String>((int) lMaxSize);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "JwtMaxJtiCacheSize(89) created: " + lMaxSize);
            }
            _mapCaches.put(_providerId, jtiCache);
        } else {
            long lOriginalSize = jtiCache.getCapacity();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "JwtMaxJtiCacheSize(95) original:" + lOriginalSize + " newSize:" + lMaxSize);
            }
            if (lOriginalSize != lMaxSize) {
                jtiCache.updateCapacity((int) lMaxSize);
            }
        }
        return jtiCache;
    }

    /**
     * @return
     */
    public boolean isAutoAuthorize() {
        return _config.isAutoAuthorize();
    }

    /**
     * @param clientId
     * @return
     */
    public boolean isAutoAuthorizeClient(String clientId) {
        String[] clients = _config.getAutoAuthorizeClients();
        for (String client : clients) {
            if (clientId.equals(client))
                return true;
        }
        return false;
    }
}