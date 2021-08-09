/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

public class OidcBaseClientProvider implements OidcOAuth20ClientProvider {
    Map<String, OidcBaseClient> _clients = new HashMap<String, OidcBaseClient>();

    @Override
    public void initialize() {}

    public OidcBaseClientProvider() {
        OidcBaseClient myClient = new OidcBaseClient("key",
                        "secret",
                        OidcOAuth20Util.initJsonArray("http://localhost:9080/oauth/client.jsp"),
                        "My Client",
                        null,
                        true);

        OidcBaseClient myClient2 = new OidcBaseClient("key2",
                        "secret2",
                        OidcOAuth20Util.initJsonArray("http://localhost:9080/fimivt/oauth/oauth2Client.jsp"),
                        "My Client2",
                        null,
                        true);

        OidcBaseClient myClient3 = new OidcBaseClient("key3",
                        "secret3",
                        null,
                        "My Client3",
                        null,
                        true);

        OidcBaseClient myClient4 = new OidcBaseClient("key4",
                        "secret4",
                        OidcOAuth20Util.initJsonArray(""),
                        "My Client4",
                        null,
                        true);

        OidcBaseClient myClient5 = new OidcBaseClient("key5",
                        "secret5",
                        OidcOAuth20Util.initJsonArray(new String[] { "http://localhost/other/redirect1", "http://localhost/other/redirect2" }),
                        "My Client5",
                        null,
                        true);

        _clients.put(myClient.getClientId(), myClient);
        _clients.put(myClient2.getClientId(), myClient2);
        _clients.put(myClient3.getClientId(), myClient3);
        _clients.put(myClient4.getClientId(), myClient4);
        _clients.put(myClient5.getClientId(), myClient5);
    }

    @Override
    public boolean exists(String clientIdentifier) {
        return _clients.containsKey(clientIdentifier);
    }

    @Override
    public OidcBaseClient get(String clientIdentifier) {
        return _clients.get(clientIdentifier);
    }

    @Override
    public boolean validateClient(String clientIdentifier, String clientSecret) {
        boolean result = false;
        if (clientIdentifier != null && clientSecret != null) {
            OidcBaseClient client = _clients.get(clientIdentifier);
            if (client != null) {
                if (client.getClientSecret().equals(clientSecret)) {
                    result = true;
                }
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider#delete(java.lang.String)
     */
    @Override
    public boolean delete(String clientIdentifier) throws OidcServerException {
        return _clients.remove(clientIdentifier) != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider#getAll()
     */
    @Override
    public Collection<OidcBaseClient> getAll() throws OidcServerException {
        return new ArrayList<OidcBaseClient>(_clients.values());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider#getAll(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public Collection<OidcBaseClient> getAll(HttpServletRequest request) throws OidcServerException {

        return getAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider#put(com.ibm.ws.security.oauth20.plugins.OidcBaseClient)
     */
    @Override
    public OidcBaseClient put(OidcBaseClient newClient) throws OidcServerException {
        return update(newClient);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider#update(com.ibm.ws.security.oauth20.plugins.OidcBaseClient)
     */
    @Override
    public OidcBaseClient update(OidcBaseClient newClient) throws OidcServerException {
        String clientIdentifier = newClient.getClientId();
        return _clients.put(clientIdentifier, newClient);
    }

    @Override
    public void init(OAuthComponentConfiguration config) {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider#initialize()
     */

}
