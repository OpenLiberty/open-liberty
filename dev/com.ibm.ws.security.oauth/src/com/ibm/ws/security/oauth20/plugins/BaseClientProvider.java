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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedClientProvider;
import com.ibm.ws.security.oauth20.exception.OAuthProviderException;
import com.ibm.ws.security.oauth20.platform.PlatformServiceFactory;
import com.ibm.ws.security.oauth20.util.ClientUtils;
import com.ibm.ws.security.oauth20.util.ConfigUtils;

/**
 * This class was imported from tWAS to make only those changes necessary to 
 * run OAuth on Liberty. The mission was not to refactor, restructure, or 
 * generally cleanup the code. 
 */
@SuppressWarnings("deprecation")
public class BaseClientProvider implements OAuth20EnhancedClientProvider {

    private static TraceComponent tc = Tr.register(BaseClientProvider.class,
            "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    private Logger logger = Logger
            .getLogger(BaseClientProvider.class.getName());
    /*
     * private ResourceBundle resBundle = ResourceBundle.getBundle(
     * Constants.RESOURCE_BUNDLE, Locale.getDefault());
     */

    static boolean initializedStatics = false;

    protected static HashMap<String, BaseClient> clientMap = new HashMap<String, BaseClient>();
    protected String providerID;
    protected boolean hasRewrites; // URI redirect token substitution

    protected static final List<BaseClient> clientsList = new ArrayList<BaseClient>();

    private String[] providerRewrites;

    public BaseClientProvider() {
    }

    public BaseClientProvider(String providerID, String[] providerRewrites) {
        this.providerID = providerID;
        this.providerRewrites = providerRewrites != null ? providerRewrites.clone() : null;
    }

    public void initialize() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initialize");

        hasRewrites = ClientUtils.initRewrites(providerID, providerRewrites);
        loadClients();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initialize");
    }

    public void init(OAuthComponentConfiguration config) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "init");

        providerID = config.getUniqueId();
        hasRewrites = ClientUtils.initRewrites(config);
        loadClients();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "init");
    }

    private void loadClients() {
        try {
            List<BaseClient> clientsList = new ArrayList<BaseClient>();
            clientsList.addAll(ConfigUtils.getClients());
            synchronized (clientMap) {
                for (BaseClient client : clientsList) {
                    String key = getKey(client.getClientId(), client.getComponentId());
                    clientMap.put(key, client);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public boolean exists(String clientIdentifier) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "exists");

        boolean result = clientMap.containsKey(getKey(clientIdentifier));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "exists", result);

        return result;
    }

    public OAuth20Client get(String clientIdentifier) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "get");

        BaseClient result = getClient(getKey(clientIdentifier));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "get", result);
        return result;
    }

    // For admin and debug, not a performance bottleneck
    public Collection<BaseClient> getAll() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getAll");

        ArrayList<BaseClient> results = new ArrayList<BaseClient>();
        for (String key : clientMap.keySet()) {
            if (providerOwns(key)) {
                results.add(getClient(key));
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getAll");

        return results;
    }

    public boolean validateClient(String clientIdentifier, String clientSecret) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "validateClient");

        boolean result = false;
        if (clientIdentifier != null && clientSecret != null) {
            BaseClient client = getClient(getKey(clientIdentifier));
            if (client != null) {
                String secret = client.getClientSecret();
                if (secret != null && secret.equals(clientSecret)) {
                    result = true;
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "validateClient", result + "");

        return result;
    }

    public BaseClient update(BaseClient newClient) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "update");

        put(newClient);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "update");

        return newClient;
    }

    // Combine the Client ID and the Provider Name
    protected String getKey(BaseClient newClient) {
        return getKey(newClient.getClientId(), newClient.getComponentId());
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

    protected BaseClient getClient(String key) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getClient " + key);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "clientMap ", clientMap);

        BaseClient result = clientMap.get(key);
        if (hasRewrites && result != null) {
            result = ClientUtils.uriRewrite(result);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getClient", result);

        return result;
    }

    public boolean delete(String clientIdentifier) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "delete");

        if (PlatformServiceFactory.getPlatformService().isDistributedCapable()) {
            // Use an MBean to edit the hashmap
            // OAuth20ClientMBeanImpl.invokeDelete(providerID, clientIdentifier); //TODO
        } else {
            synchronized (clientMap) {
                clientMap.remove(getKey(clientIdentifier, providerID));
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "delete");

        return true;
    }

    public BaseClient put(BaseClient newClient) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "put");

        if (PlatformServiceFactory.getPlatformService().isDistributedCapable()) {
            // Use an MBean to edit the hashmap
            // OAuth20ClientMBeanImpl.invokePut(newClient); //TODO
        } else {
            synchronized (clientMap) {
                clientMap.put(getKey(newClient), newClient);
                storeClients();
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "put");

        return newClient;
    }

    protected void storeClients() {
        try {
            ClientUtils.storeClients(clientMap.values());
        } catch (OAuthProviderException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public int getCount() {
        return clientMap.size();
    }
}
