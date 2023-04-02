/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MangleJWTTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test class for general JAXRS OAuth tests
 *
 * @author chrisc
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class NoOPAudiences1ServerTests extends MangleJWTTestTools {

    private static final Class<?> thisClass = NoOPAudiences1ServerTests.class;
    protected static final String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static final String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";

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

        List<String> extraMsgs2 = new ArrayList<String>();

        List<String> extraApps2 = new ArrayList<String>();

        TestServer.addTestApp(extraApps2, null, Constants.OP_TAI_APP, Constants.OIDC_OP);

        // set token and cert types - hard code JWT and then select the token type to use...
        String tokenType = Constants.JWT_TOKEN;
        //		String certType = rsTools.chooseCertType(tokenType) ;
        String certType = Constants.X509_CERT;

        testSettings = new TestSettings();
        // We don't need an OP server, but, we do need commonSetup to run through some steps normally done for the OP
        // set skipServerStart = true to skip the actual start (but do everything else) - skipServerStart will
        // be reset to false by the skip method - that allows the RS to be started in the following step.
        skipServerStart = true;
        testOPServer = commonSetUp(OPServerName, "server_audiences.xml", Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
        genericTestServer = commonSetUp(RSServerName, "server_audiences.xml", Constants.GENERIC_SERVER, extraApps2, Constants.DO_NOT_USE_DERBY, extraMsgs2, null, Constants.OIDC_OP, true, true, tokenType, certType);

        // We use a variable insert for the validationMethod config attribute which the config evaluator will think is invalid
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "validationMethod");

        targetProvider = Constants.OIDCCONFIGSAMPLE_APP;
        flowType = Constants.WEB_CLIENT_FLOW;
        goodActions = RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS;

        // set RS protected resource to point to second server.
        testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/oauth2tai/snoop");
        testSettings = helpers.fixProviderInUrls(testSettings, "providers", "endpoint");
    }

    /************************************* General token mangling ***************************************/

    /**
     * Create a JWT token with a valid "aud" in the token. The audiences in the config is configured, but does NOT contain the app
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_MisMatch_otherValidUrlInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudOtherApp");

        negativeTest(updatedTestSettings, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with a valid "aud" in the token. The audiences in the config is configured, but only contains some
     * random string
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_MisMatch_InvalidStringInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudSomeString");

        negativeTest(updatedTestSettings, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with NO "aud" in the token. The audiences in the config is NOT configured
     * We should get access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_No_inTokenOrCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/noAud");
        String origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, null);

        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with NO "aud" in the token. The audiences in the config is configured, and does contain the app
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_notInToken_InCfg() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, null);

        negativeTest(testSettings, badJwtToken, new String[] { MessageConstants.CWWKS1779E_MISSING_AUD, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with a valid "aud" in the token. The audiences in the config is NOT configured
     * We should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_AppUrlInToken_NotInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/noAud");

        positiveTest(updatedTestSettings);

    }

    /**
     * Create a JWT token with some string in "aud" in the token. The audiences in the config is NOT configured
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_StringInToken_NotInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/noAud");
        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, someString);

        negativeTest(updatedTestSettings, badJwtToken, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with a valid "aud" in the token. The audiences in the config is configured, but contains a substring of
     * the app
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_cfgIsSubstringOfToken() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, testSettings.getRSProtectedResource() + "/someAPp");

        negativeTest(testSettings, badJwtToken, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with an "aud" in the token that is a substring of the app. The audiences in the config is configured,
     * and does contain the app
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_tokenIsSubstringOfCfg() throws Exception {

        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, genericTestServer.getHttpsString());

        negativeTest(testSettings, badJwtToken, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with an "aud" in the token that is a substring of the app. The audiences in the config is NOT configured
     * We should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_tokenIsSubstringOfApp_NotInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/noAud");
        String origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, genericTestServer.getHttpsString());

        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with an "aud" in the token that contains some string. The audiences in the config is configured, but
     * contains some string
     * (the string in the token and config are the same)
     * We should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_StringInToken_StringInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudSomeString");
        String origJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, updatedTestSettings.getClientID());

        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with a valid "aud" in the token. The audiences in the config is configured. It contains multiple urls -
     * including the app.
     * Test with the app being the first, second, or third entry in the audiences of the config
     * In all cases, we should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_TokenIsOneOfMultipleInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudMultiple");
        String origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, genericTestServer.getHttpsString() + "/oauth2tai/snoop/xyz");
        positiveTest(updatedTestSettings, origJwtToken);
        origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, genericTestServer.getHttpsString() + "/oauth2tai/snoop/abc");
        positiveTest(updatedTestSettings, origJwtToken);
        origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, genericTestServer.getHttpsString() + "/oauth2tai/snoop/def");
        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with a valid "aud" in the token - the aud contains multiple urls - one of which is the app. The
     * audiences in the config is configured, and does contain the app
     * Test using "," ", " as separators and with the app being in different locations in the aud of the token
     * In all cases, we should have access to the app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_MultipleInTokenOneInCfg() throws Exception {

        // multiple entries in the aud - last one matches - split by ", "
        String audString = genericTestServer.getHttpsString() + "/oauth2tai/snoop/abc," + testSettings.getRSProtectedResource();
        String origJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, audString);
        positiveTest(testSettings, origJwtToken);

        // multiple entries in the aud - last one matches - split by ","
        audString = genericTestServer.getHttpsString() + "/oauth2tai/snoop/abc, " + testSettings.getRSProtectedResource();
        origJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, audString);
        positiveTest(testSettings, origJwtToken);

        // multiple entries in the aud - first one matches - split by ", "
        audString = testSettings.getRSProtectedResource() + "," + genericTestServer.getHttpsString() + "/oauth2tai/snoop/abc";
        origJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, audString);
        positiveTest(testSettings, origJwtToken);

    }

    /**
     * Create a JWT token with a valid "aud" in the token - the aud contains multiple urls - one of which is the app. The
     * audiences in the config is configured. It contains multiple urls - including the app.
     * Test using "," ", " as separators and with the app being in different locations in the aud of the token
     * In all cases, we should have access to the app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_MultipleInTokenMultipleInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudMultiple");

        // multiple entries in the aud - last one matches - split by ", "
        String audString = genericTestServer.getHttpsString() + "/oauth2tai/snoop/abc," + updatedTestSettings.getRSProtectedResource();
        String origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, audString);
        positiveTest(updatedTestSettings, origJwtToken);

        // multiple entries in the aud - last one matches - split by ","
        audString = genericTestServer.getHttpsString() + "/oauth2tai/snoop/abc, " + updatedTestSettings.getRSProtectedResource();
        origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, audString);
        positiveTest(updatedTestSettings, origJwtToken);

        // multiple entries in the aud - first one matches - split by ", "
        audString = updatedTestSettings.getRSProtectedResource() + "," + genericTestServer.getHttpsString() + "/oauth2tai/snoop/abc";
        origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, audString);
        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with a valid "aud" in the token. The audiences in the config is configured and set to ALL_AUDIENCES
     * We should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_UrlInToken_ALLAudiencesInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudAll");

        positiveTest(updatedTestSettings);

    }

    /**
     * Create a JWT token with soem string in "aud" in the token. The audiences in the config is configured and set to
     * ALL_AUDIENCES
     * We should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_StringInToken_ALLAudiencesInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudAll");
        String origJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, badString);

        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with NO "aud" in the token. The audiences in the config is configured and set to ALL_AUDIENCES
     * We should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_NotInToken_ALLAudiencesInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudAll");
        String origJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, null);

        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with an "aud" in the token that is missing the port. We are NOT using the default ports. The audiences
     * in the config is NOT configured.
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_PortMissingInToken_NotInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/noAud");
        String badJwtToken = buildAJWTToken(testSettings, Constants.PAYLOAD_AUDIENCE, "https://localhost");

        negativeTest(updatedTestSettings, badJwtToken, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with a valid "aud" in the token. The audiences in the config is configured, but set to ""
     * We should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_AppUrlInToken_EmptyInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudEmpty");

        positiveTest(updatedTestSettings);

    }

    /**
     * Create a JWT token with "aud" set to some other host/port in the token. The audiences in the config is configured, but set
     * to ""
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_OtherHostUrlInToken_EmptyInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudEmpty");
        String badJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, "https://localhost:9999");

        negativeTest(updatedTestSettings, badJwtToken, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with "aud" set to some other app (on the same host/port) in the token. The audiences in the config is
     * configured, but set to ""
     * We should have access to our app (we'll just compare to https://localhost:<port> )
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_OtherAppForHostInToken_EmptyInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudEmpty");
        String origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, genericTestServer.getHttpsString() + "/someother/app");

        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with "aud" set to "https://localhost:<port>" in the token. The audiences in the config is configured,
     * but set to ""
     * We should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_SubStringMatchUrlInToken_EmptyInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudEmpty");
        String origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, genericTestServer.getHttpsString());

        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with "aud" set to more of the app root in the token. The audiences in the config is configured, but set
     * to ""
     * (similar to NoOPAudiences1ServerTests_OtherAppForHostInToken_EmptyInCfg, but the url has more in common with the real app
     * we're using)
     * We should have access to our app
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_SuperStringMatchUrlInToken_EmptyInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudEmpty");
        String audString = genericTestServer.getHttpsString() + "/oauth2tai/snoop/abc";
        String origJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, audString);

        positiveTest(updatedTestSettings, origJwtToken);

    }

    /**
     * Create a JWT token with a string in "aud" in the token. The audiences in the config is configured, but set to ""
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_StringInToken_EmptyInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudEmpty");
        String badJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, someString);

        negativeTest(updatedTestSettings, badJwtToken, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with a string in "aud" in the token. The audiences in the config is configured, but set to ""
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_JustAppNameInToken_EmptyInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/AudEmpty");
        String badJwtToken = buildAJWTToken(updatedTestSettings, Constants.PAYLOAD_AUDIENCE, "AudEmpty");

        negativeTest(updatedTestSettings, badJwtToken, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

    /**
     * Create a JWT token with a valid "aud" in the token. The audiences in the config is configured, but set to "justAppName"
     * The request should fail with a 401 exception
     *
     * @throws Exception
     */
    @Test
    public void NoOPAudiences1ServerTests_AppUrlInToken_JustAppNameInCfg() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/JustAppName");

        negativeTest(updatedTestSettings, new String[] { MessageConstants.CWWKS1774E_AUD_INVALID, MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });

    }

}
