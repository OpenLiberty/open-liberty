/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;

public class CacheEntry implements Serializable {
    private static transient TraceComponent tc = Tr.register(CacheEntry.class,
            "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");
    final static transient String CLASS = CacheEntry.class.getName();
    private transient Logger _log = Logger.getLogger(CLASS);

    private static final long serialVersionUID = 5802154139964139458L;
    public long _expiryTime;
    public OAuth20Token _token;

    public CacheEntry(OAuth20Token token, int lifetime) {
        if (!(token instanceof Serializable)) {
            _token = new OAuth20TokenImpl(token);
        } else {
            _token = token;
        }
        _expiryTime = (new Date()).getTime() + (lifetime * 1000L);
        // trace GK
        if (tc.isDebugEnabled()) { // @GK1
            Tr.debug(tc, "OAuth20Token CacheEntry:" + _token + " will expire at:" + _expiryTime); // @GK1
        } // @GK1
    }

    public boolean isExpired() {
        Date now = new Date();
        long nowTime = now.getTime();
        // trace @GK1
        if (nowTime >= _expiryTime) { // @GK1
            if (tc.isDebugEnabled()) { // @GK1
                Tr.debug(tc, "token: " + _token + " expired"); // @GK1
            } // @GK1
        } // @GK1
        return nowTime >= _expiryTime;
    }
}
