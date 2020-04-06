/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.Map;
import java.util.Properties;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.cache.DistributedMap;
import com.ibm.websphere.cache.EntryInfo;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.jaas.common.callback.AuthenticationHelper;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.cache.DistributedObjectCacheFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.ltpa.Token;

/**
 * Methods to create, add, and get logged out tokens from the LoggedOutTokenMap DistributedMap
 */
public class LoggedOutTokenCacheImpl implements LoggedOutTokenCache {
    private static final TraceComponent tc = Tr.register(LoggedOutTokenCacheImpl.class);

    private DistributedMap dmns = null;

    private static final AtomicServiceReference<TokenManager> tokenManager = new AtomicServiceReference<TokenManager>("tokenManager");

    protected void setTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.setReference(ref);
    }

    protected void unsetTokenManager(ServiceReference<TokenManager> ref) {
        tokenManager.setReference(ref);
    }

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        tokenManager.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        tokenManager.deactivate(cc);
    }

    static final class Singleton {
        private static final LoggedOutTokenCache instance = new LoggedOutTokenCacheImpl();
    }

    @Trivial
    public static LoggedOutTokenCache getInstance() {
        return Singleton.instance;
    }

    /*
     * Look up the key from the DistributedMap if it exists
     */
    @Override
    public Object getDistributedObjectLoggedOutToken(Object key) {
        //check to see if the token is in the distributed map
        if (dmns != null) {
            DistributedMap map = dmns;
            Object dist_object = map.get(key);
            return dist_object;
        }

        // if dmns does not exist there are no entries in the DistributedMap
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The LoggedOutTokenMap DistributedMap does not exist.");
        }
        return null;
    }

    /*
     * Add the token to the DistributedMap
     *
     * key is the token string
     * value is the subject
     * timeToLive is the about of time left before the token expires, to become the expiring time of the distributed map entry
     */
    @Override
    public Object putDistributedObjectLoggedOutToken(Object key, Object value, int timeToLive) {

        DistributedMap map = getDMLoggedOutTokenMap();

        if (map != null) {
            Object dist_object = map.put(key, value, 1, timeToLive, EntryInfo.SHARED_PUSH, null);
            return dist_object;
        }
        return null;
    }

    /*
     * Add the token to the DistributedMap, will create the loggedOutTokenMap if it does not exist.
     * Calculates the about of time left on the token so the DistributedMap will expire the entry at
     * time.
     */
    @Override
    public Object addTokenToDistributedMap(Object key, Object value) {
        String keyStr = (String) key;
        TokenManager tm = LoggedOutTokenCacheImpl.tokenManager.getService();
        int timeOut = -1;
        try {
            byte[] tokenBytes = AuthenticationHelper.copyCredToken(Base64Coder.base64DecodeString(keyStr));
            Token token = tm.recreateTokenFromBytes(tokenBytes);
            if (token != null) {
                long tokenExp = token.getExpiration();
                long calcTimeOut = tokenExp - System.currentTimeMillis();
                timeOut = (int) calcTimeOut / 1000;
                String userName = token.getAttributes("u")[0];
                if (userName != null)
                    value = userName;
            }

        } catch (InvalidTokenException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Token is not valid so do not cache it " + e.getMessage());
            }
            return null;
        } catch (TokenExpiredException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Token is expired so do not cache it " + e.getMessage());
            }
            return null;
        }

        return putDistributedObjectLoggedOutToken(key, value, timeOut);
    }

    /*
     * Get the LoggedOutTokenMap
     */
    private DistributedMap getDMLoggedOutTokenMap() {

        if (dmns == null) {
            dmns = getDistrubedMap("LoggedOutTokenMap");
        }

        return dmns;

    }

    /*
     * Creates the LoggedOutTokeMap if it does not exist.
     */
    private DistributedMap getDistrubedMap(String mapName) {
        DistributedMap dm = null;

        dm = DistributedObjectCacheFactory.getMap(mapName, new Properties());

        return dm;
    }

}
