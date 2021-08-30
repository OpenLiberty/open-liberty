/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.client.fat.CommonTests.LTPACookieOidcClientTests;

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
public class OidcClientLTPACookieTests extends LTPACookieOidcClientTests {

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcClientLTPACookieTests.class;

        List<String> rs_apps = new ArrayList<String>() {
            {
                add(Constants.HELLOWORLD_SERVLET);
            }
        };

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        //         Tests should run with either access token or jwt as access token - to start, let's just use an access token
        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        //        String tokenType = Constants.ACCESS_TOKEN_KEY;
        //        String tokenType = Constants.JWT_TOKEN;
        //        String certType = Constants.X509_CERT;
        //                String certType = Constants.JWK_CERT;

        // the JWK validation URL's are too complex to build in the xml
        HashMap<String, String> jwkMap = new HashMap<String, String>();
        jwkMap.put("oidcJWKValidationURL_NoIntroSpectOIDC", "oidcJWKValidationURL_NoIntroSpectOIDC");
        setJWKValidationMap(jwkMap);

        // Start the Generic/App Server
        genericTestServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rs", "rs_server_orig_LTPA.xml", Constants.GENERIC_SERVER, rs_apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_orig_LTPA.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_orig_LTPA.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // The way we setup the variables in bootstrap/rs config can lead to msg CWWKG0032W being issued, but it really is ok
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE);

        testSettings.setFlowType(Constants.RP_FLOW);

        testSettings.setScope("openid profile");
        testSettings.addRequestParms();
    }

}
