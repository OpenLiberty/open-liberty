/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.discovery;

import javax.net.ssl.SSLSocketFactory;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.common.structures.SingleTableCache;

import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;

public class DiscoveryHandler {

    public static final TraceComponent tc = Tr.register(DiscoveryHandler.class);

    // TODO Discovery metadata will be cleared from the cache after 5 minutes
    private static SingleTableCache cachedDiscoveryMetadata = new SingleTableCache(1000 * 60 * 5);

    private final SSLSocketFactory sslSocketFactory;
    protected HttpUtils httpUtils;

    public DiscoveryHandler(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        this.httpUtils = getHttpUtils();
    }

    public JSONObject fetchDiscoveryDataJson(String discoveryUri, String clientId) throws OidcDiscoveryException {
        // See if we already have cached metadata for this endpoint to avoid sending discovery requests too frequently
        JSONObject discoveryData = (JSONObject) cachedDiscoveryMetadata.get(discoveryUri);
        if (discoveryData != null) {
            return discoveryData;
        }
        try {
            String jsonString = fetchDiscoveryDataString(discoveryUri, true, false);
            discoveryData = JSONObject.parse(jsonString);
        } catch (Exception e) {
            throw new OidcDiscoveryException(clientId, discoveryUri, e.toString());
        }
        cachedDiscoveryMetadata.put(discoveryUri, discoveryData);
        return discoveryData;
    }

    public String fetchDiscoveryDataString(String discoveryUrl, boolean hostNameVerificationEnabled, boolean useSystemProperties) throws Exception {
        return httpUtils.getHttpJsonRequest(sslSocketFactory, discoveryUrl, hostNameVerificationEnabled, useSystemProperties);
    }

    protected HttpUtils getHttpUtils() {
        return new HttpUtils();
    }

}
