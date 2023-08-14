/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

// TODO - Client tests
package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.SSO.clientTests.PrivateKeyJwt.PrivateKeyJwtClientTests;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run Private Key Jwt client tests using openidConnectClient and an OP to provide authorization
 * functionality. The tests stub out the token endpoint which will just log the request used to invoke it. The tests will then
 * search for the different request parameters recorded in the log. They will
 * validate that the proper parms were passed and that the content of those parms is what was expected (based on the config used
 * by that test). This includes parsing the private key jwt and making sure that all of the required claims are included and
 * are correct.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class OidcClientPrivateKeyJwtTests extends PrivateKeyJwtClientTests {

    public static Class<?> thisClass = OidcClientPrivateKeyJwtTests.class;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        useLdap = false;

        Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op.privateKeyJwt", "op_server_PrivateKeyJwt.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        clientServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp.privateKeyJwt", "rp_server_PrivateKeyJwt.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        testSettings.setFlowType(Constants.RP_FLOW);
        testSettings.setTokenEndpt(clientServer.getHttpString() + "/PrivateKeyJwtTokenEndpoint/token");

        clientServer.addIgnoredServerExceptions(MessageConstants.SRVE8094W_CANNOT_SET_HEADER_RESPONSE_COMMITTED, MessageConstants.SRVE8115W_CANNOT_SET_HEADER_RESPONSE_COMMITTED);

        usingSocialClient = false;
    }

}
