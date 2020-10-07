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
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openidconnect.web.TraceConstants;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

public class OIDCProvidersConfig {
    private static TraceComponent tc = Tr.register(OIDCProvidersConfig.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    private static final Map<String, OidcServerConfig> oidcServerConfigMap = Collections.synchronizedMap(new HashMap<String, OidcServerConfig>());
    private static boolean bOidcUpdated = false;
    private static Map<String, OidcServerConfig> oidcMap = Collections.synchronizedMap(new HashMap<String, OidcServerConfig>());

    /**
     * Gets the OidcServerConfig object.
     * 
     * @param oauth20providerName The OAuth20 provider name.
     * @return The OidcServerConfig object. If there is not a match, the code returns null.
     */
    public static OidcServerConfig getOidcServerConfigForOAuth20Provider(String oauth20providerName) {
        OidcServerConfig oidcServerConfig = null;
        synchronized (oidcServerConfigMap) {
            if (bOidcUpdated) {
                oidcMap = checkDuplicateOAuthProvider(oidcServerConfigMap);
                bOidcUpdated = false;
            }
        }
        Iterator<String> oidcMapKeyIterator = oidcMap.keySet().iterator();
        while (oidcMapKeyIterator.hasNext()) {
            String key = oidcMapKeyIterator.next();
            OidcServerConfig entry = oidcMap.get(key);
            String providerName = entry.getOauthProviderName();
            if (oauth20providerName.equals(providerName)) {
                oidcServerConfig = entry;
                break;
            }
        }
        return oidcServerConfig;
    }

    public static void putOidcServerConfig(String providerId, OidcServerConfig oidcServerConfig) {
        synchronized (oidcServerConfigMap) {
            oidcServerConfigMap.put(providerId, oidcServerConfig);
            bOidcUpdated = true;
        }
    }

    public static void removeOidcServerConfig(String providerId) {
        synchronized (oidcServerConfigMap) {
            oidcServerConfigMap.remove(providerId);
            bOidcUpdated = true;
        }
    }
   
    public static ArrayList<ProviderInfo> getProviderNames() {
        ArrayList<ProviderInfo> list = new ArrayList<ProviderInfo>();     
        synchronized(oidcServerConfigMap) {
            Iterator<String> oidcMapKeyIterator = oidcServerConfigMap.keySet().iterator();
            while (oidcMapKeyIterator.hasNext()) {
                String key = oidcMapKeyIterator.next();
                String id = oidcServerConfigMap.get(key).getProviderId();    
                String oauthId = oidcServerConfigMap.get(key).getOauthProviderName();
                list.add(new ProviderInfo(id, oauthId));
            }      
        }
        return list;
    }
    
    public static HashMap<String, OidcServerConfig> checkDuplicateOAuthProvider(Map<String, OidcServerConfig> oidcServerConfigRef) {
        HashMap<String, OidcServerConfig> result = new HashMap<String, OidcServerConfig>();
        HashMap<String, String> mapProviderId = new HashMap<String, String>();
        HashMap<String, String> mapConfigId = new HashMap<String, String>();
        Set<String> configIDs = (Set<String>) oidcServerConfigRef.keySet();
        for (String configId : configIDs) {
            OidcServerConfig oidcServerConfig = oidcServerConfigRef.get(configId);
            String oidcProviderId = oidcServerConfig.getProviderId();
            String oauthProviderName = oidcServerConfig.getOauthProviderName();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "oidcConfigId: " + oidcProviderId + " oauthProviderName: " + oauthProviderName);
            }
            String oldOidcProviderId = mapProviderId.get(oauthProviderName);
            if (oldOidcProviderId != null) {
                //Tr.error(tc, "OIDC_SERVER_MULTI_OIDC_TO_ONE_OAUTH", new Object[] {oldOidcProviderId, oidcProviderId, oauthProviderName});
                //The error message had been handled by OidcEndpointService
                String oldConfigId = mapConfigId.get(oauthProviderName);
                result.remove(oldConfigId);
            } else {
                mapProviderId.put(oauthProviderName, oidcProviderId);
                mapConfigId.put(oauthProviderName, configId);
                result.put(configId, oidcServerConfig);
            }
        }
        return result;
    }
}
