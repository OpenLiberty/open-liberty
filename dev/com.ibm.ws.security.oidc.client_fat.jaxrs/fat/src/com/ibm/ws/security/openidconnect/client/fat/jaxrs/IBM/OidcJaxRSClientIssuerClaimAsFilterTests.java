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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests.JaxRSClientIssuerClaimAsFilterTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run basic OpenID Connect RP tests.
 * This test class extends GenericRPTests.
 * GenericRPTests contains common code for all RP tests.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class OidcJaxRSClientIssuerClaimAsFilterTests extends JaxRSClientIssuerClaimAsFilterTests {

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcJaxRSClientIssuerClaimAsFilterTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.HELLOWORLD_SERVLET);
            }
        };
        List<String> rp_apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // these tests will need to run with a JWT access_token - issuer isn't accessible with opaque or JWE's
        // (for JWE's we would need the config content to open the access_token to get the issuer, but, we haven't chosen a config yet)
        // Start the Generic/App Server
        genericTestServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rs", "rs_server_issuerClaimAsFilter.xml", Constants.GENERIC_SERVER, apps,
                Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, Constants.JWT_TOKEN, Constants.X509_CERT);

        // We use a variable insert for the validationMethod config attribute which the config evaluator will think is invalid
        genericTestServer.addIgnoredServerExceptions(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "validationMethod", MessageConstants.CWWKG0033W_CONFIG_REFERENCE_NOT_FOUND, MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        // add possible messages that we'll see when trying to use a JWE token
        genericTestServer.addIgnoredServerExceptions(MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE, MessageConstants.CWWKS6066E_JWE_DECRYPTION_KEY_MISSING);

        //         Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.op", "op_server_issuerClaimAsFilter.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, null, null, true, true, Constants.JWT_TOKEN, Constants.X509_CERT);
        testOPServer.addIgnoredServerException(MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rp", "rp_server_issuerClaimAsFilter.xml", Constants.OIDC_RP, apps,
                Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, Constants.JWT_TOKEN, Constants.X509_CERT);

        testSettings.setFlowType(Constants.RP_FLOW);
        testSettings.setScope("openid profile");

    }

}
