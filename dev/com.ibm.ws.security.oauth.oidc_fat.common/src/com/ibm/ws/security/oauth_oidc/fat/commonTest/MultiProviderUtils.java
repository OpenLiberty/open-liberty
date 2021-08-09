/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
public class MultiProviderUtils {

    //private static final Class<?> thisClass = MultiProviderUtils.class;
    protected static TestSettings oauthSettings = new TestSettings();
    public ValidationData vData = new ValidationData();
    public static EndpointSettings eSettings = new EndpointSettings();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static CommonMessageTools msgUtils = new CommonMessageTools();

    public List<validationData> addGeneralGoodExpectations(String testName, TestSettings settings) throws Exception {
        return addGeneralGoodExpectations(testName, settings, null);

    }

    public List<validationData> addGeneralGoodExpectations(String testName, TestSettings settings, String providerTypeOverride) throws Exception {

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null, Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null, "false");

        String providerType = null;
        if (providerTypeOverride == null) {
            providerType = eSettings.getProviderType();
        } else {
            providerType = providerTypeOverride;
        }
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, testName, providerType, Constants.PERFORM_LOGIN, settings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, testName, providerType, Constants.PERFORM_LOGIN, settings);

        return expectations;
    }

    public List<String> addOauthStartMsgs() throws Exception {

        List<String> msgs = new ArrayList<String>();
        msgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);

        return msgs;

    }

    public List<String> addOidcStartMsgs() throws Exception {

        List<String> msgs = new ArrayList<String>();
        msgs.add("CWWKS1600I.*" + Constants.OIDCCONFIGSAMPLE_START_APP);
        //msgs.add("CWWKS1631I.*");

        return msgs;

    }

    public TestSettings copyAndOverrideProviderSettings(TestSettings origSettings, String oldProvider, String newProivder) throws Exception {
        return copyAndOverrideProviderSettings(origSettings, oldProvider, newProivder, null);

    }

    public TestSettings copyAndOverrideProviderSettings(TestSettings origSettings, String oldProvider, String newProivder, String newResource) throws Exception {

        TestSettings updatedTestSettings = origSettings.copyTestSettings();
        updatedTestSettings.setAuthorizeEndpt(origSettings.getAuthorizeEndpt().replaceAll(oldProvider, newProivder));
        updatedTestSettings.setTokenEndpt(origSettings.getTokenEndpt().replaceAll(oldProvider, newProivder));
        updatedTestSettings.setIntrospectionEndpt(origSettings.getIntrospectionEndpt().replaceAll(oldProvider, newProivder));
        updatedTestSettings.setRevocationEndpt(origSettings.getRevocationEndpt().replaceAll(oldProvider, newProivder));
        updatedTestSettings.setIssuer(origSettings.getIssuer().replaceAll(oldProvider, newProivder));
        //updatedTestSettings.setDiscoveryEndpt(origSettings.getDiscoveryEndpt().replaceAll(oldProvider, newProivder));
        //updatedTestSettings.setUserinfoEndpt(origSettings.getUserinfoEndpt().replaceAll(oldProvider, newProivder));
        if (newResource != null) {
            updatedTestSettings.setProtectedResource(origSettings.getProtectedResource().replaceAll("snoop", newResource));
        }
        return updatedTestSettings;

    }
}
