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
package com.ibm.ws.security.saml20.fat.commonTest.utils;

import java.util.List;

import com.ibm.ws.security.fat.common.ValidationData;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageTools;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestServer;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

public class RSCommonUtils {

    //	private static final Class<?> thisClass = RSCommonUtils.class ;
    protected static SAMLCommonTestHelpers helpers = new SAMLCommonTestHelpers();
    protected ValidationData vData = new ValidationData(SAMLConstants.ALL_TEST_ACTIONS);
    protected static SAMLMessageTools msgUtils = new SAMLMessageTools();

    public void fixServer2Ports(SAMLTestServer theServer) throws Exception {
        msgUtils.printMethodName("fixServer2Ports");

        // we need to override the ports that are stored in the secondary server (first servers ports are set by default)
        theServer.setServerHttpPort(theServer.getServer().getHttpSecondaryPort());
        theServer.setServerHttpsPort(theServer.getServer().getHttpSecondarySecurePort());

    }

    /**
     * Sets good expectations for all default SAML steps, 200 status codes for all steps, and that the "Hello World!" string
     * appears in the full response of the JAX-RS GET invocation. Also checks that the appropriate realm name based on the
     * selected IDP server is checked in the public and private credentials of the returned subject.
     * 
     * @return
     * @throws Exception
     */
    public List<validationData> getGoodExpectationsForJaxrsGet(String flowType, SAMLTestSettings testSettings) throws Exception {
        return getGoodExpectationsForJaxrsGet(flowType, testSettings, true);
    }

    /**
     * Sets good expectations for all default SAML steps, 200 status codes for all steps, and that the "Hello World!" string
     * appears in the full response of the JAX-RS GET invocation. If checkRealm is true, the appropriate realm name is also
     * checked in the public and private credentials of the returned subject.
     * 
     * @return
     * @throws Exception
     */
    public List<validationData> getGoodExpectationsForJaxrsGet(String flowType, SAMLTestSettings testSettings, boolean checkRealm) throws Exception {

        msgUtils.printMethodName("getGoodExpectationsForJaxrsGet");

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, testSettings);

        String realm = testSettings.getIdpIssuer();
        if (checkRealm) {
            // Check the realm in the credentials
            String chosenIdp = testSettings.getSelectedIDPServerName();
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected realm in the public credentials.", null, "realmName=" + realm);
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected realm in the private credentials.", null, "com.ibm.wsspi.security.cred.realm=" + realm);
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not receive the expected realmName in RunAs subject:" + realm, null, SAMLCommonTestHelpers.assembleRegExRealmNameInRunAsSubject(testSettings.getIdpIssuer()));
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected uniqueSecurityName without the realm in the public credentials: " + testSettings.getIdpUserName(), null, "uniqueSecurityName=" + testSettings.getIdpUserName());

        }

        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected Issuer: " + testSettings.getIdpIssuer(), null, "SAMLIssuerName:" + testSettings.getIdpIssuer());
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected user name: " + testSettings.getIdpUserName(), null, testSettings.getIdpUserName());
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected Audience Restriction: " + testSettings.getSpConsumer(), null, "audienceRestriction:[" + testSettings.getSpConsumer() + "]");

        // Check for the "Hello World!" string
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected \"" + SAMLConstants.HELLO_WORLD_STRING + "\" string.", null, SAMLConstants.HELLO_WORLD_STRING);

        return expectations;
    }

    public List<validationData> getGoodExpectationsForJaxrsAIPTests(String flowType, SAMLTestSettings testSettings) throws Exception {
        return getGoodExpectationsForJaxrsAIPTests(flowType, testSettings, true);
    }

    public List<validationData> getGoodExpectationsForJaxrsAIPTests(String flowType, SAMLTestSettings testSettings, boolean checkRealm) throws Exception {

        msgUtils.printMethodName("getGoodExpectationsForJaxrsAIPTests");

        List<validationData> expectations = vData.addSuccessStatusCodes();

        String realm = testSettings.getIdpIssuer();
        if (checkRealm) {
            // Check the realm in the credentials
            String chosenIdp = testSettings.getSelectedIDPServerName();
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected realm in the public credentials.", null, "realmName=" + realm);
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected realm in the private credentials.", null, "com.ibm.wsspi.security.cred.realm=" + realm);
        }

        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected Issuer: ." + testSettings.getIdpIssuer(), null, "SAMLIssuerName:" + testSettings.getIdpIssuer());
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected user name: ." + testSettings.getIdpUserName(), null, testSettings.getIdpUserName());
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected Audience Restriction: ." + testSettings.getSpConsumer(), null, "audienceRestriction:[" + testSettings.getSpConsumer() + "]");

        // Check for the "Hello World!" string
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected \"" + SAMLConstants.HELLO_WORLD_STRING + "\" string.", null, SAMLConstants.HELLO_WORLD_STRING);

        return expectations;
    }

    public SAMLTestSettings changeTestApps(SAMLTestServer testAppServer, SAMLTestSettings inSettings, String appExtension) throws Exception {

        msgUtils.printMethodName("changeTestApps");
        SAMLTestSettings updatedTestSettings = inSettings.copyTestSettings();

        updatedTestSettings.setSpTargetApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI + "_" + appExtension);

        return updatedTestSettings;
    }

}