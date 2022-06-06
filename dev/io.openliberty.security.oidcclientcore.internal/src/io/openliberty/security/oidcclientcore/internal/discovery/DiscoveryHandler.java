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

/**
 *
 */
public class DiscoveryHandler {

    SSLSocketFactory sslSocketFactory;

    private long nextDiscoveryTime;
    private final long discoveryPollingRate = 5 * 60 * 1000;
    private String discoveryDocumentHash;
    private String issuerIdentifier;
    private String authorizationEndpointUrl;
    private String tokenEndpointUrl;
    private String validationMethod;

    public DiscoveryHandler(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public void fetchDiscoveryData(String discoveryUrl, boolean hostNameVerificationEnabled) {
        if (hostNameVerificationEnabled) {

        } else {

        }
        if (!isValidDiscoveryUrl(discoveryUrl)) {
            // log error
        }

    }

    private boolean isValidDiscoveryUrl(String discoveryUrl) {
        return discoveryUrl != null && discoveryUrl.startsWith("https");
    }

    public void setNextDiscoveryTime() {
        this.nextDiscoveryTime = System.currentTimeMillis() + discoveryPollingRate;
    }

    public long getNextDiscoveryTime() {
        return this.nextDiscoveryTime;
    }

    // TODO: Integrate HashUtils functions locally in here to avoid circular dependencies
    private boolean calculateDiscoveryDocumentHash(JSONObject json) {
        return false;
    }

    public String getDiscoveryDocumentHash() {
        return this.discoveryDocumentHash;
    }

    private boolean invalidIssuer() {
        // TODO Auto-generated method stub
        return this.issuerIdentifier == null;
    }

    private boolean invalidEndpoints() {
        //TODO check other information also and make sure that we have valid values
        return (this.authorizationEndpointUrl == null && this.tokenEndpointUrl == null);
    }

    // TODO: Integrate discoveryUtils locally
    private void handleValidationEndpoint(JSONObject json) {
    }

    private boolean isIntrospectionValidation() {
        return "introspect".equals(this.validationMethod);
    }

    // TODO: Borrow Trace object to access logging tools
    protected void parseJsonResponse(String jsonString) {
    }

    // TODO: Use HttpUtils for HttpClient methods

}
