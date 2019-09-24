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

package com.ibm.ws.security.oauth20.plugins;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.util.ClientUtils;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.web.RegistrationEndpointServices;

public class OidcBaseClientProvider implements OidcOAuth20ClientProvider {

    private static TraceComponent tc = Tr.register(OidcBaseClientProvider.class, "OAuth20Provider", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");
    private final Logger logger = Logger.getLogger(OidcBaseClientProvider.class.getName());
    private static final String ERROR_DESCRIPTION_UNIMPLEMENTED = "This method is unimplemented for non-database client stores.";

    protected static HashMap<String, OidcBaseClient> clientMap = new HashMap<String, OidcBaseClient>();
    protected String providerID;
    protected boolean hasRewrites; // URI redirect token substitution
    protected static final List<OidcBaseClient> clientsList = new ArrayList<OidcBaseClient>();
    private final String[] providerRewrites;

    public OidcBaseClientProvider(String providerId, String[] providerRewrites) {
        this.providerID = providerId;
        this.providerRewrites = providerRewrites != null ? providerRewrites.clone() : null;
    }

    @Override
    public void initialize() {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "initialize");
        }

        hasRewrites = ClientUtils.initRewrites(providerID, providerRewrites);
        loadClients();

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "initialize");
        }
    }

    @Override
    public void init(OAuthComponentConfiguration config) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "init");
        }

        providerID = config.getUniqueId();
        hasRewrites = ClientUtils.initRewrites(config);
        loadClients();

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "init");
        }
    }

    private void loadClients() {
        try {
            List<OidcBaseClient> clientsList = new ArrayList<OidcBaseClient>();
            clientsList.addAll(ConfigUtils.getClients());
            synchronized (clientMap) {
                for (OidcBaseClient client : clientsList) {
                    String key = getKey(client.getClientId(), client.getComponentId());
                    clientMap.put(key, client);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    @Override
    public boolean exists(String clientIdentifier) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "exists");
        }

        boolean result = clientMap.containsKey(getKey(clientIdentifier));

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "exists", result);
        }

        return result;
    }

    @Override
    public OidcBaseClient get(String clientIdentifier) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "get");
        }

        OidcBaseClient result = getClient(getKey(clientIdentifier));

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "get", result);
        }
        return result;
    }

    @Override
    public Collection<OidcBaseClient> getAll() throws OidcServerException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getAll");
        }

        Collection<OidcBaseClient> results = getAll(null);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getAll");
        }

        return results;
    }

    @Override
    public Collection<OidcBaseClient> getAll(HttpServletRequest request) throws OidcServerException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getAll(request)");
        }

        ArrayList<OidcBaseClient> results = new ArrayList<OidcBaseClient>();
        for (String key : clientMap.keySet()) {
            if (providerOwns(key)) {
                results.add(getClient(key, request));
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getAll(request)");
        }

        return results;
    }

    @Override
    public boolean validateClient(String clientIdentifier, String clientSecret) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "validateClient");
        }

        boolean result = false;
        if (clientIdentifier != null && clientSecret != null) {
            OidcBaseClient client = getClient(getKey(clientIdentifier));
            if (client != null) {
                String secret = client.getClientSecret();
                if (secret != null && secret.equals(clientSecret)) {
                    result = true;
                }
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "validateClient", result + "");
        }

        return result;
    }

    @Override
    public OidcBaseClient update(OidcBaseClient newClient) throws OidcServerException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "update");
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "update");
        }
        throw new OidcServerException(ERROR_DESCRIPTION_UNIMPLEMENTED, OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected String getKey(String clientId) {
        return getKey(clientId, providerID);
    }

    protected String getKey(String clientId, String providerId) {
        return clientId + "_" + providerId;
    }

    protected boolean providerOwns(String key) {
        return key.endsWith("_" + providerID);
    }

    protected OidcBaseClient getClient(String key) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getClient " + key);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "clientMap ", clientMap);
            }
        }

        OidcBaseClient result = getClient(key, null);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getClient", result);
        }

        return result;
    }

    protected OidcBaseClient getClient(String key, HttpServletRequest request) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getClient " + key);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "clientMap ", clientMap);
            }
        }

        OidcBaseClient result = null;
        OidcBaseClient retrieved = clientMap.get(key);
        if (retrieved != null) {
            result = OidcBaseClientValidator.getInstance(retrieved).setDefaultsForOmitted();
        }

        // Add client registration URI
        if (request != null && result != null/** && (OidcOAuth20Util.isNullEmpty(result.getRegistrationClientUri())) **/
        ) {
            RegistrationEndpointServices.processClientRegistationUri(result, request);
        }

        if (hasRewrites && result != null) {
            result = ClientUtils.uriRewrite(result);
        }

        if (result != null) {
            try {
                if (result.getClientName() != null) {
                    result.setClientName(URLDecoder.decode(result.getClientName(), "UTF-8"));
                }
            } catch (UnsupportedEncodingException ex) {
                // keep the existing client name
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getClient", result);
        }

        return result;
    }

    @Override
    public boolean delete(String clientIdentifier) throws OidcServerException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "delete");
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "delete");
        }
        throw new OidcServerException(ERROR_DESCRIPTION_UNIMPLEMENTED, OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    public boolean deleteOverride(String clientIdentifier) throws OidcServerException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "deleteOverride");
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "deleteOverride");
        }

        return clientMap.remove(getKey(clientIdentifier)) != null;
    }

    @Override
    public OidcBaseClient put(OidcBaseClient newClient) throws OidcServerException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "put");
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "put");
        }
        throw new OidcServerException(ERROR_DESCRIPTION_UNIMPLEMENTED, OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
