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

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.DiscoveryUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This new class "OidcClientDiscoveryJVMPropsTests.java" is created to test
 * issue# 19832. 
 * Below lists the reasons why this new test is not updated to the existing class
 * (e.g., OidcClientDiscoveryErrorTests.java): 
 *
 * 1.com.ibm.ws.config.xml.internal.ConfigRefresher issues CWWKG0027W for the
 * timeout message after a 1 minute delay of waiting related to the
 * Notifications sent out, for example, CWWKG0017I "The server configuration was
 * successfully updated" 
 * 
 * 2. When the original server xml (e.g., rp_server_orig.xml) contains the include of new xml with bad proxy IP 
 * (e.g., 5.6.7.8) in discoveryEndpointUrl, during the reconfigServer invocation in
 * some existing test methods such as OidcClientTestUserIdNotInRegistry(),
 * ConfigRefresher will take on its duty to issue timeout when that happens. 
 * 
 * 3. The bad proxy IP with "useSystemPropertiesForHttpClientConnections=true" in discoveryEndpointUrl
 * can take more than 1 minute to attempt the connection when the test running in liberty build framework,
 * which then results CWWKG0027W in messages.log and the AssertionError in junit
 * frmework due to search failure for CWWKG0017I. 
 * (FYI - the connection attempt in a local run takes about 20 seconds.)
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OidcClientDiscoveryJVMPropsTests extends CommonTest {
	
	public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;

	@SuppressWarnings("serial")
	@BeforeClass
	public static void setUp() throws Exception {

		List<String> apps = new ArrayList<String>() {
			{
				add(Constants.OPENID_APP);
			}
		};

		testSettings = new TestSettings();

		// Set config parameters for Access token with X509 Certificate
		String tokenType = Constants.ACCESS_TOKEN_KEY;
		String certType = Constants.X509_CERT;

		// Start the OIDC OP server
		testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_orig.xml",
				Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS,
				Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

		DiscoveryUtils.waitForOPDiscoveryToBeReady(testSettings);

		// Start the OIDC RP server and setup default values
		testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rpd2",
				"rp_server_jvmprops.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS,
				Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

		testRPServer.addIgnoredServerException("CWWKS1524E"); // Ignore message indicating unsuccessful response from
																// endpoint
		testRPServer.addIgnoredServerException("CWWKS1525E"); // Ignore message indicating discovery failed
		
		testSettings.setFlowType(Constants.RP_FLOW);

		DiscoveryUtils.waitForRPDiscoveryToBeReady(testSettings);

	}

	/**
	 * issue# 19832 
         * This tests the configuration attribute useSystemPropertiesForHttpClientConnections. 
         * A proxy host and port are defined in usr/(server)/jvm.options, but won't take effect until this
	 * attribute is set to true. When the attribute is set to true, we expect a
	 * failure because the token retrieval call should be redirected to the
	 * non-existent proxy server.
	 *
	 * Testing the full path would require a proxy server, which the FAT framework
	 * does not have, but it has been done manually.
	 *
	 * Note that the proxy properties DO NOT TAKE EFFECT FOR LOCALHOST, so if you
	 * are debugging, you cannot configure the OP to be "localhost" and expect it to
	 * work.
	 *
	 */

	/**
	 * Test Purpose: With discovery configured at the RP, attempting to access a
	 * test servlet specifying invalid OP url with wrong proxy host/port results in
	 * 401 error with messages logged.
	 * 
	 * Expected Results: The discovery should be attempted and should fail with 401
	 * when the RP tries processing the first authentication request and the
	 * following messages should be logged: 
         * CWWKS1525E: A successful response was not returned from the URL <discovery endpoint>. This is the [0] response
	 * status and the [IOException: Connect to 5.6.7.8:8920 [/5.6.7.8] failed:
	 * Connection timed out: connect java.net.ConnectException: Connection timed out: connect] error from the discovery request. 
         * CWWKS1524E: The OpenID Connect client [jvmProps] failed to obtain Open ID Connect Provider endpoint
	 * information through the discovery endpoint URL ... 
         * CWWKS1534E: The OpenID Connect client [client01] requires an authorization endpoint URL, but it is not set.
	 */

	@Mode(TestMode.LITE)
	@AllowedFFDC({ "org.apache.http.conn.HttpHostConnectException" })
	@Test
	public void OidcClientDiscoveryTestWithJvmProps() throws Exception {

		TestSettings updatedTestSettings = testSettings.copyTestSettings();
		updatedTestSettings.setScope("openid profile");
		updatedTestSettings
				.setTestURL(updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/jvmProps"));

		List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.GET_LOGIN_PAGE);
		expectations = vData.addResponseStatusExpectation(expectations, Constants.GET_LOGIN_PAGE,
				Constants.UNAUTHORIZED_STATUS);

		testRPServer.addIgnoredServerException("CWWKS1534E");

		WebConversation wc = new WebConversation();
		genericRP(_testName, wc, updatedTestSettings, test_LOGIN_PAGE_ONLY, expectations);

	}

}
