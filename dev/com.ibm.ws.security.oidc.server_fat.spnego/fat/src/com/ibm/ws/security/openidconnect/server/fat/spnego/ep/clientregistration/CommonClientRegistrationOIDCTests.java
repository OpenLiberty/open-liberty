/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.fat.spnego.ep.clientregistration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoOIDCCommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoOIDCConstants;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@RunWith(FATRunner.class)
public class CommonClientRegistrationOIDCTests extends SpnegoOIDCCommonTest {

	//TODO change the create to use common spnego token 
    public static String registrationUri;
    public static final String PROVIDER_URI1 = "http://www.one.com/";
    public static final String PROVIDER_URI2 = "http://www.two.com/";
    public static final String PROVIDER_URI3 = "http://www.three.com/";
    public static final String PROVIDER_URI4 = "http://www.four.com/";
    //public static final String UNAUTHORIZED_MSG_1 = "\"error_description\":\"You must be an administrator to complete this request\"";
    public static final String UNAUTHORIZED_MSG_1 = "\"error_description\":\"The user is not in the role that is required to complete this request\"";
    public static final String UNAUTHORIZED_MSG_2 = "\"error\":\"access_denied\"";
    public static final String ADMIN_USER = "testuser";
    public static final String ADMIN_PASS = "testuserpwd";
    public static final String DEFAULT_CLIENT_ID = "defaultClient";
    public static final String SPECIFIC_CLIENT_ID = "client01";

    protected endpointSettings createSpnegoHeader(boolean sendBadToken) throws Exception {

    	if (sendBadToken){
    		return new EndpointSettings().new endpointSettings("Authorization",
                    SpnegoOIDCConstants.SPNEGO_NEGOTIATE + SpnegoOIDCConstants.SEND_BAD_TOKEN
                    + " User-Agent: Firefox, Host: "+ testHelper.getTestSystemFullyQualifiedDomainName());
    	}
    	else {
        return new EndpointSettings().new endpointSettings("Authorization",
                SpnegoOIDCConstants.SPNEGO_NEGOTIATE + getCommonSpnegoToken()
                + " User-Agent: Firefox, Host: "+ testHelper.getTestSystemFullyQualifiedDomainName());
    	}
    }

    protected List<endpointSettings> setRequestHeaders() throws Exception {
    	return setRequestHeaders(false);
    }
    protected List<endpointSettings> setRequestHeaders(boolean sendBadToken) throws Exception {
        List<endpointSettings> headers = new ArrayList<EndpointSettings.endpointSettings>();
        endpointSettings authorization = createSpnegoHeader(sendBadToken);
        headers = eSettings.addEndpointSettings(headers, "Accept", "application/json");
        headers.add(authorization);
        return headers;
    }
 
    protected List<endpointSettings> setBadAcceptTypeRequestHeaders() throws Exception {
        List<endpointSettings> headers = new ArrayList<EndpointSettings.endpointSettings>();
        endpointSettings authorization = createSpnegoHeader(false);
        headers = eSettings.addEndpointSettings(headers, "Accept", "not/real");
        headers.add(authorization);
        return headers;
    }

    protected void addExpectation(List<validationData> expectations, String expectedValue) throws Exception {
        if (expectations == null) {
            expectations = new ArrayList<validationData>();
        }
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_REGISTRATION_ENDPOINT, "Registration response did not contain the expected value.", expectedValue);
    }

    protected void addExpectationDoesNotContain(List<validationData> expectations, String expectedValue) throws Exception {
        if (expectations == null) {
            expectations = new ArrayList<validationData>();
        }
        expectations = vData.addExpectation(expectations, Constants.INVOKE_REGISTRATION_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                "Registration response contained an unexpected value.", null, expectedValue);
    }

    protected void addExpectationMatches(List<validationData> expectations, String expectedValue) throws Exception {
        if (expectations == null) {
            expectations = new ArrayList<validationData>();
        }
        expectations = vData.addExpectation(expectations, Constants.INVOKE_REGISTRATION_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_MATCHES,
                "Registration response did not match the expected value.", null, expectedValue);
    }

    protected void addPositiveHeaderExpectations(List<validationData> expectations, String hdrValue) throws Exception {
        if (expectations == null) {
            expectations = new ArrayList<validationData>();
        }
        expectations = vData.addExpectation(expectations, Constants.INVOKE_REGISTRATION_ENDPOINT, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                "Header did not contain the expected value  " + hdrValue, null, hdrValue);
    }

    protected WebResponse deleteClientId(String clientId) throws Exception {
        URL url = null;
        String clientRegistrationUri = testSettings.getRegistrationEndpt() + "/" + clientId;
        try {
            url = AutomationTools.getNewUrl(clientRegistrationUri);
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
        }
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();

            endpointSettings authorization = createSpnegoHeader(false);
            httpURLConnection.setRequestProperty(authorization.getKey(), authorization.getValue());

            httpURLConnection.setRequestMethod("DELETE");
            httpURLConnection.getResponseCode();

            return WebResponse.newResponse(httpURLConnection);
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

        return null;
    }

}