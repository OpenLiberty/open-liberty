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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.http.HttpUtils;

public class DiscoveryHandler {

    public static final TraceComponent tc = Tr.register(DiscoveryHandler.class);

    SSLSocketFactory sslSocketFactory;

    public DiscoveryHandler(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public void fetchDiscoveryData(String discoveryUrl, boolean hostNameVerificationEnabled) {
        if (!isValidDiscoveryUrl(discoveryUrl)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid discovery URL.");
                return;
            }
        }
        String jsonString = null;
        if (hostNameVerificationEnabled) {
            HttpUtils httpUtils = new HttpUtils();
            try {
                jsonString = httpUtils.getHttpRequest(sslSocketFactory, discoveryUrl, hostNameVerificationEnabled, null, null);
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to perform the HTTP Request successfully : ", e.getCause());
                }
            }
        }
    }

    private boolean isValidDiscoveryUrl(String discoveryUrl) {
        return discoveryUrl != null && discoveryUrl.startsWith("https");
    }

}
