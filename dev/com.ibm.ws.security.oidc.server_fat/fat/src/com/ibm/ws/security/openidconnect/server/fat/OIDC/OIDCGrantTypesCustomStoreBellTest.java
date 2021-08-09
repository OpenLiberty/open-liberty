/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.OIDC;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.CommonTests.GenericGrantTypesTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCGrantTypesCustomStoreBellTest extends GenericGrantTypesTests {
	
	@Override
	public void testGrantType_NoGrantTypesInConfig() throws Exception {
    	Log.info(thisClass, "testGrantType_NoGrantTypesInConfig", "Needs to be implemented");
    	Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_authCode_match() throws Exception {
		Log.info(thisClass, "testGrantType_authCode_match", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_authCode_misMatch() throws Exception {
		Log.info(thisClass, "testGrantType_authCode_misMatch", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_implicit_match() throws Exception {
		Log.info(thisClass, "testGrantType_implicit_match", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_implicit_misMatch() throws Exception {
		Log.info(thisClass, "testGrantType_implicit_misMatch", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_implicit_match_provider_misMatch() throws Exception {
		Log.info(thisClass, "testGrantType_implicit_match_provider_misMatch", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_password_match() throws Exception {
		Log.info(thisClass, "testGrantType_password_match", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_password_with_mediator() throws Exception {
		Log.info(thisClass, "testGrantType_password_with_mediator", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_password_with_bad_creds() throws Exception {
		Log.info(thisClass, "testGrantType_password_with_bad_creds", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_password_with_default_skipValidation() throws Exception {
		Log.info(thisClass, "testGrantType_password_with_default_skipValidation", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_password_with_skipValidation() throws Exception {
		Log.info(thisClass, "testGrantType_password_with_skipValidation", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_password_misMatch() throws Exception {
		Log.info(thisClass, "testGrantType_password_misMatch", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_password_match_provider_misMatch() throws Exception {
		Log.info(thisClass, "testGrantType_password_match_provider_misMatch", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_refreshToken_misMatch() throws Exception {
		Log.info(thisClass, "testGrantType_refreshToken_misMatch", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_clientCred_match() throws Exception {
		Log.info(thisClass, "testGrantType_clientCred_match", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

	@Override
	public void testGrantType_clientCred_misMatch() throws Exception {
		Log.info(thisClass, "testGrantType_clientCred_misMatch", "Needs to be implemented");
		Assume.assumeTrue(false);
	}

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    private static final Class<?> thisClass = OIDCGrantTypesCustomStoreBellTest.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for 
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, extraMsgs, Constants.OP_TAI_APP, Constants.OIDC_OP);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        testSettings = new TestSettings();
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.server-1.0_fat", "server_grantTypesCustomStoreBell.xml", Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY,  Constants.USE_MONGODB, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType, Constants.JUNIT_REPORTING);
        targetProvider = Constants.OIDCCONFIGSAMPLE_APP;
        flowType = Constants.WEB_CLIENT_FLOW;
        goodActions = Constants.BASIC_PROTECTED_RESOURCE_ACTIONS;
        
        testOPServer.addIgnoredServerExceptions("CWWKG0032W");

    }

}
