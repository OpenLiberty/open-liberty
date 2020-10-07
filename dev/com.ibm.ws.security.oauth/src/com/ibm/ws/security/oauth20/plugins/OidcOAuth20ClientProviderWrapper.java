/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.statistics.OAuthStatisticNames;
import com.ibm.oauth.core.internal.statistics.OAuthStatHelper;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;

/**
 * Wraps a customers ClientProvider with statistics.
 * 
 */
public class OidcOAuth20ClientProviderWrapper implements OidcOAuth20ClientProvider {

    OidcOAuth20ClientProvider _real;
    OAuthStatisticsImpl _stats;

    public OidcOAuth20ClientProviderWrapper(OidcOAuth20ClientProvider real, OAuthStatisticsImpl stats) {
        _real = real;
        _stats = stats;
    }

    public void initialize() {
        if (_real instanceof OidcOAuth20ClientProvider) {
            ((OidcOAuth20ClientProvider) _real).initialize();
        }
    }

    public void init(OAuthComponentConfiguration config) {
        _real.init(config);
    }

    public boolean exists(String clientIdentifier) {
        OAuthStatHelper statHelper = new OAuthStatHelper(OAuthStatisticNames.OAUTH20_CLIENTPROVIDER_EXISTS);

        boolean result = false;
        try {
            result = _real.exists(clientIdentifier);
        } catch (OidcServerException e) {
            result = false;
        }

        _stats.addMeasurement(statHelper);
        return result;
    }

    public OidcBaseClient get(String clientIdentifier) {
        OAuthStatHelper statHelper = new OAuthStatHelper(OAuthStatisticNames.OAUTH20_CLIENTPROVIDER_GETCLIENT);

        OidcBaseClient result = null;
        try {
            result = _real.get(clientIdentifier);
        } catch (OidcServerException e) {
            result = null;
        }

        _stats.addMeasurement(statHelper);
        return result;
    }

    public boolean validateClient(String clientIdentifier, String clientSecret) {
        OAuthStatHelper statHelper = new OAuthStatHelper(OAuthStatisticNames.OAUTH20_CLIENTPROVIDER_VALIDATECLIENT);

        boolean result = false;

        try {
            result = _real.validateClient(clientIdentifier, clientSecret);
        } catch (OidcServerException e) {
            result = false;
        }

        _stats.addMeasurement(statHelper);
        return result;
    }

    public boolean delete(String clientIdentifier) throws OidcServerException {
        if (_real instanceof OidcOAuth20ClientProvider) {
            return ((OidcOAuth20ClientProvider) _real).delete(clientIdentifier);
        } else {
            throw new RuntimeException("delete() is not supported.");
        }
    }

    public Collection<OidcBaseClient> getAll() throws OidcServerException {
        if (_real instanceof OidcOAuth20ClientProvider) {
            return ((OidcOAuth20ClientProvider) _real).getAll(null);
        } else {
            throw new RuntimeException("getAll() is not supported.");
        }
    }

    public Collection<OidcBaseClient> getAll(HttpServletRequest request) throws OidcServerException {
        if (_real instanceof OidcOAuth20ClientProvider) {
            return ((OidcOAuth20ClientProvider) _real).getAll(request);
        } else {
            throw new RuntimeException("getAll(request) is not supported.");
        }
    }

    public OidcBaseClient put(OidcBaseClient newClient) throws OidcServerException {
        if (_real instanceof OidcOAuth20ClientProvider) {
            return ((OidcOAuth20ClientProvider) _real).put(newClient);
        } else {
            throw new RuntimeException("put() is not supported.");
        }
    }

    public OidcBaseClient update(OidcBaseClient newClient) throws OidcServerException {
        if (_real instanceof OidcOAuth20ClientProvider) {
            return ((OidcOAuth20ClientProvider) _real).update(newClient);
        } else {
            throw new RuntimeException("update() is not supported.");
        }
    }
}
