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

package com.ibm.ws.security.oauth20.plugins;

import java.util.Collection;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;
import com.ibm.oauth.core.api.oauth20.client.OAuth20ClientProvider;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedClientProvider;
import com.ibm.ws.security.oauth20.exception.OAuthDataException;

public class OAuth20EnhancedClientProviderWrapper implements OAuth20EnhancedClientProvider {

    private OAuth20ClientProvider clientProvider;

    public OAuth20EnhancedClientProviderWrapper(OAuth20ClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void init(OAuthComponentConfiguration config) {
        this.clientProvider.init(config);
    }

    public boolean exists(String clientIdentifier) {
        return this.clientProvider.exists(clientIdentifier);
    }

    public OAuth20Client get(String clientIdentifier) {
        return this.clientProvider.get(clientIdentifier);
    }

    public boolean validateClient(String clientIdentifier, String clientSecret) {
        return this.clientProvider.validateClient(clientIdentifier, clientSecret);
    }

    public boolean delete(String clientIdentifier) {
        if (this.clientProvider instanceof OAuth20EnhancedClientProvider) {
            return ((OAuth20EnhancedClientProvider) clientProvider).delete(clientIdentifier);
        } else {
            throw new RuntimeException("delete() is not supported.");
        }
    }

    public Collection<BaseClient> getAll() {
        if (this.clientProvider instanceof OAuth20EnhancedClientProvider) {
            return ((OAuth20EnhancedClientProvider) clientProvider).getAll();
        } else {
            throw new RuntimeException("getAll() is not supported.");
        }
    }

    public BaseClient put(BaseClient newClient) throws OAuthDataException {
        if (this.clientProvider instanceof OAuth20EnhancedClientProvider) {
            return ((OAuth20EnhancedClientProvider) clientProvider).put(newClient);
        } else {
            throw new RuntimeException("put() is not supported.");
        }
    }

    public BaseClient update(BaseClient newClient) throws OAuthDataException {
        if (this.clientProvider instanceof OAuth20EnhancedClientProvider) {
            return ((OAuth20EnhancedClientProvider) clientProvider).update(newClient);
        } else {
            throw new RuntimeException("update() is not supported.");
        }
    }

    public int getCount() {
        if (this.clientProvider instanceof OAuth20EnhancedClientProvider) {
            return ((OAuth20EnhancedClientProvider) clientProvider).getCount();
        } else {
            throw new RuntimeException("getCount() is not supported.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        if (this.clientProvider instanceof OAuth20EnhancedClientProvider) {
            ((OAuth20EnhancedClientProvider) clientProvider).initialize();
        }
    }

}
