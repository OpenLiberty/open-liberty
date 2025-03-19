/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.social.fat.delegated;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.delegated.common.CommonDelegatedTestClass;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OidcDelegatedSocialLoginWithLibertyOPTests extends CommonDelegatedTestClass {

    public static Class<?> thisClass = OidcDelegatedSocialLoginWithLibertyOPTests.class;

    public static TestServer externalOPServer = null;

    @BeforeClass
    public static void setUp() throws Exception {

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        createAndStartRpServer();
        createAndStartOpServer();
        createAndStartExternalOpServer();

        setGenericVSSpeicificProviderFlags(GenericConfig);
    }

    static void createAndStartRpServer() throws Exception {
        List<String> rpStartMsgs = new ArrayList<String>();
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.HELLOWORLD_SERVLET);
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OAUTH_ROOT);
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OIDC_CLIENT_DEFAULT_CONTEXT_ROOT);

        List<String> rpApps = new ArrayList<String>();
        rpApps.add(SocialConstants.HELLOWORLD_SERVLET);

        testRPServer = commonSetUp(SocialConstants.SERVER_NAME + ".delegated.rp", "rp_server_orig.xml", SocialConstants.GENERIC_SERVER, rpApps, SocialConstants.DO_NOT_USE_DERBY, rpStartMsgs);
    }

    static void createAndStartOpServer() throws Exception {
        List<String> opStartMsgs = new ArrayList<String>();
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.JWT_DEFAULT_CONTEXT_ROOT);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OAUTH_ROOT);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OIDC_DEFAULT_CONTEXT_ROOT);

        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".delegated.op", "op_server_orig.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP);

        // Each test will have to reconfigure, so don't bother restoring in between tests
        testOPServer.setRestoreServerBetweenTests(false);
    }

    static void createAndStartExternalOpServer() throws Exception {
        List<String> externalOpStartMsgs = new ArrayList<String>();
        externalOpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OAUTH_ROOT);
        externalOpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OIDC_DEFAULT_CONTEXT_ROOT);

        externalOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".delegated.op.external", "external_op_server_orig.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, externalOpStartMsgs, null, SocialConstants.OIDC_OP);

        //        externalOPServer.setServerHttpPort(Integer.getInteger("HTTP_secondary"));
        //        externalOPServer.setServerHttpsPort(Integer.getInteger("HTTP_secondary.secure"));
    }

    /*************************************************** Tests ***************************************************/

    @Mode(TestMode.LITE)
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void OidcDelegatedSocialLoginTests_LibertyOP_oauth() throws Exception {

        testOPServer.reconfigServer("server_provider_LibertyOP_oauth.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = getUpdatedSettingsForExternalOpTests(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OAUTH_OP, DOES_NOT_USE_SELECTION_PAGE);

        List<validationData> expectations = getGoodDelegatedLoginExpectations(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    @Test
    public void OidcDelegatedSocialLoginTests_LibertyOP_wrongUser() throws Exception {

        testOPServer.reconfigServer("server_provider_LibertyOP_oauth.xml", _testName);

        // Use the default credentials that are in the first Liberty OP's user registry, not the delegated OP's registry
        SocialTestSettings updatedSocialTestSettings = getUpdatedTestSettings(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OAUTH_OP, DOES_NOT_USE_SELECTION_PAGE);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        // Make sure we get to the login page for the external OP server
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_URL, SocialConstants.STRING_CONTAINS, "Did not reach the login page from the expected external OP server and port.", null, externalOPServer.getServerHttpsString() + "/oidc/login");

        // Make sure we're re-prompted with the login page after the invalid credentials are submitted
        String finalAction = invoke_social_login_actions[invoke_social_login_actions.length - 1];
        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, finalAction);
        expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_URL, SocialConstants.STRING_CONTAINS, "Did not reach the login page from the expected external OP server and port.", null, externalOPServer.getServerHttpsString() + "/oidc/login");
        expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did not find the username/password mismatch error message in the response.", null, "username and password doesn't match");
        expectations = validationTools.addMessageExpectation(externalOPServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in log for a username that could not be found in the registry.", SocialMessageConstants.CWIML4537E_PRINCIPAL_NOT_FOUND);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcDelegatedSocialLoginTests_LibertyOP_oidc() throws Exception {

        testOPServer.reconfigServer("server_provider_LibertyOP_oidc.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = getUpdatedSettingsForExternalOpTests(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP, DOES_NOT_USE_SELECTION_PAGE);

        List<validationData> expectations = getGoodDelegatedLoginExpectations(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    @Test
    public void OidcDelegatedSocialLoginTests_LibertyOP_oidc_jwk() throws Exception {

        testOPServer.reconfigServer("server_provider_LibertyOP_oidc_jwk.xml", _testName);
        externalOPServer.reconfigServer("external_op_server_jwk.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = getUpdatedSettingsForExternalOpTests(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP, DOES_NOT_USE_SELECTION_PAGE);

        List<validationData> expectations = getGoodDelegatedLoginExpectations(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /*************************************************** Helper methods ***************************************************/

    SocialTestSettings getUpdatedSettingsForExternalOpTests(String provider, String style, boolean usesSelectionPage) throws Exception {
        SocialTestSettings settings = getUpdatedTestSettings(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP, DOES_NOT_USE_SELECTION_PAGE);
        settings.setUserName(EXTERNAL_USER);
        settings.setUserPassword(EXTERNAL_USER_PWD);
        return settings;
    }

    List<validationData> getGoodDelegatedLoginExpectations(SocialTestSettings settings) throws Exception {
        List<validationData> expectations = setGoodSocialExpectations(settings, doNotAddJWTTokenValidation);

        // Make sure we get to the login page for the external OP server
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_URL, SocialConstants.STRING_CONTAINS, "Did not reach the login page from the expected external OP server and port.", null, externalOPServer.getServerHttpsString() + "/oidc/login");

        return expectations;
    }

}
