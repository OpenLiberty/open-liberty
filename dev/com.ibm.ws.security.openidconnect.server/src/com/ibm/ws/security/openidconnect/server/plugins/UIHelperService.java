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
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;



import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.openidconnect.web.TraceConstants;

/**
 * This class is called by AdminCenter to get a list of providers and their applicable URL's. 
 * Returns an  arraylist  of maps.
 * Each map represents a single provider and some properties about it, specifically:
 *    name - name of the provider
 *    accountManager = url path to accountManager admin endpoint, if available for this provider
 *    tokenManager   = url path to tokenManager admin endpoint, if available for this provider.
 *    clientAdmin  = url path to clientAdmin endpoint, if available for this provider.
 */
public class UIHelperService {
    private static TraceComponent tc = Tr.register(UIHelperService.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
     static final String epPrefix = "/oidc/endpoint/";
    
     @SuppressWarnings("unchecked")
    public static List<Map<String,String>> getProviderInfo(){
         ArrayList<Map<String,String>> providerData = new ArrayList<Map<String,String>>();
         ArrayList<ProviderInfo> pinfo = OIDCProvidersConfig.getProviderNames();
         UIHelperService me = new UIHelperService();
         for(ProviderInfo p : pinfo) {
             providerData.add(me.determineProviderConfig(p));
         }         
         return providerData;
     }
     
     Map<String,String> determineProviderConfig(ProviderInfo info){
         HashMap<String,String> map = new HashMap<String,String>();
         map.put("name", info.getName());
         boolean apppw = false;
         boolean apptok = false;
         boolean clientadmin = false;
         OidcBaseClient client = null;
         
         OAuth20Provider provider = ProvidersService.getOAuth20Provider(info.getOauthProviderName());
         
         // internalClientId has to be defined for the admin center or endpoint ui's to manipulate anything.
         try {
            client = provider.getClientProvider().get(provider.getInternalClientId());
         } catch (OidcServerException e) {
          //ffdc
         }
//         apppw = client != null && client.isAppPasswordAllowed();
//         apptok = client != null && client.isAppTokenAllowed();
         clientadmin = ! provider.isLocalStoreUsed();
         
//         if(apppw || apptok) {
        if (client != null) {
             map.put("personalTokenManagement", epPrefix + info.getName() + "/personalTokenManagement");
             map.put("usersTokenManagement", epPrefix + info.getName() + "/usersTokenManagement");
         }
         if(clientadmin) {
             map.put("clientManagement", epPrefix + info.getName() + "/clientManagement");
         }
         return map;
         
     }
}
