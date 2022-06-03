/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.internal.discovery;

/**
 *
 */
public class DiscoveryHandler {

    public DiscoveryHandler() {

    }

//    @FFDCIgnore({ SSLException.class })
//    public boolean handleDiscoveryEndpoint(String discoveryUrl) {
//
//        String jsonString = null;
//        boolean valid = false;
//
//        if (!isValidDiscoveryUrl(discoveryUrl)) {
//            Tr.error(tc, "OIDC_CLIENT_DISCOVERY_SSL_ERROR", getId(), discoveryUrl);
//            return false;
//        }
//        try {
//            setNextDiscoveryTime(); //
//            SSLSocketFactory sslSocketFactory = getSSLSocketFactory(discoveryUrl, sslConfigurationName, sslSupportRef.getService());
//            HttpClient client = createHTTPClient(sslSocketFactory, discoveryUrl, hostNameVerificationEnabled);
//            jsonString = getHTTPRequestAsString(client, discoveryUrl);
//            if (jsonString != null) {
//                parseJsonResponse(jsonString);
//                if (this.discoveryjson != null) {
//                    valid = discoverEndpointUrls(this.discoveryjson);
//                }
//            }
//
//        } catch (SSLException e) {
//            if (tc.isDebugEnabled()) {
//                Tr.debug(tc, "Fail to get successful discovery response : ", e.getCause());
//            }
//
//        } catch (Exception e) {
//            // could be ignored
//            if (tc.isDebugEnabled()) {
//                Tr.debug(tc, "Fail to get successful discovery response : ", e.getCause());
//            }
//        }
//
//        if (!valid) {
//            Tr.error(tc, "OIDC_CLIENT_DISCOVERY_SSL_ERROR", getId(), discoveryUrl);
//        }
//        return valid;
//    }
}
