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

import javax.net.ssl.SSLSocketFactory;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.http.HttpUtils;

public class DiscoveryHandler {

    public static final TraceComponent tc = Tr.register(DiscoveryHandler.class);

    private final SSLSocketFactory sslSocketFactory;
    public HttpUtils httpUtils;
    private JSONObject discoveryjson;

    public DiscoveryHandler(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        this.httpUtils = new HttpUtils();
        this.discoveryjson = null;
    }

    public JSONObject fetchDiscoveryData(String discoveryUrl, boolean hostNameVerificationEnabled) throws Exception {
        if (!isValidDiscoveryUrl(discoveryUrl)) {
            String errorMsg = Tr.formatMessage(tc, "DISCOVERY_URL_NOT_VALID", discoveryUrl);
            throw new Exception(errorMsg);
        }
        String jsonString = httpUtils.getHttpRequest(sslSocketFactory, discoveryUrl, hostNameVerificationEnabled, null, null);
        parseJsonResponse(jsonString);
        return discoveryjson;
    }

    private boolean isValidDiscoveryUrl(String discoveryUrl) {
        return discoveryUrl != null && discoveryUrl.startsWith("https");
    }

    protected void parseJsonResponse(String jsonString) {
        try {
            this.discoveryjson = JSONObject.parse(jsonString);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception parsing JSON string [" + jsonString + "]: " + e);
            }
        }
    }

}
