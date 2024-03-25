/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.backchannelLogout.fat.CommonTests;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.AfterClass;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.backchannelLogout.fat.utils.AfterLogoutStates;
import com.ibm.ws.security.backchannelLogout.fat.utils.AfterLogoutStates.BCL_FORM;
import com.ibm.ws.security.backchannelLogout.fat.utils.BackChannelLogout_RegisterClients;
import com.ibm.ws.security.backchannelLogout.fat.utils.Constants;
import com.ibm.ws.security.backchannelLogout.fat.utils.TokenKeeper;
import com.ibm.ws.security.backchannelLogout.fat.utils.VariationSettings;
import com.ibm.ws.security.fat.common.Utils;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.utils.JweHelper;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings.StoreType;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.utils.ServerFileUtils;

/**
 * This class supplies support methods to the back channel logout tests.
 */

public class BackChannelLogoutCommonTests extends CommonTest {

    protected final static String localStore = "LOCALSTORE";

    protected static Class<?> thisClass = BackChannelLogoutCommonTests.class;
    public static ServerFileUtils serverFileUtils = new ServerFileUtils();
    public static EndpointSettings eSettings = new EndpointSettings();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    public static CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();
    protected static boolean defaultUseLdap = useLdap;
    public String testClient = null;
    public static TestServer clientServer = null;
    public static TestServer clientServer2 = null;
    protected static String currentRepeatAction = null;
    protected static String tokenType = null;
    protected static String httpSessionEnabled = null;
    protected static String bclRoot = null;

    protected static VariationSettings vSettings = null;

    protected static BackChannelLogout_RegisterClients regClients = null;

    protected static String logoutApp = null;

    protected boolean debug = false;
    protected static String testResultString = "";
    protected static boolean starIt = false;
    protected static String printString = "";
    protected static String savedContinueMsg = "";

    public static class SkipIfSocialClient extends MySkipRule {

        protected static Class<?> thisClass = SkipIfSocialClient.class;

        @Override
        public Boolean callSpecificCheck() {
            boolean flag = currentRepeatAction.contains(SocialConstants.SOCIAL);
            Log.info(thisClass, "callSpecificCheck", "Uses a Social Client: " + Boolean.toString(flag));
            recordTestIfSkipped(flag);
            return flag;
        }
    }

    public static class SkipIfUsesMongoDB extends MySkipRule {

        protected static Class<?> thisClass = SkipIfUsesMongoDB.class;

        @Override
        public Boolean callSpecificCheck() {
            boolean flag = currentRepeatAction.contains(Constants.MONGODB);
            Log.info(thisClass, "callSpecificCheck", "Uses MongoDB:" + Boolean.toString(flag));
            recordTestIfSkipped(flag);
            return flag;
        }
    }

    public static class SkipIfUsesMongoDBOrSocialClient extends MySkipRule {

        protected static Class<?> thisClass = SkipIfUsesMongoDBOrSocialClient.class;

        @Override
        public Boolean callSpecificCheck() {
            Boolean usesMongoDB = currentRepeatAction.contains(Constants.MONGODB);
            Boolean socialClient = currentRepeatAction.contains(SocialConstants.SOCIAL);
            Log.info(thisClass, "callSpecificCheck", "Uses MongoDB:" + Boolean.toString(usesMongoDB));
            Log.info(thisClass, "callSpecificCheck", "Uses a Social Client: " + Boolean.toString(socialClient));
            recordTestIfSkipped(usesMongoDB || socialClient);
            return usesMongoDB || socialClient;
        }

    }

    public static class SkipIfUsingJustReqLogout extends MySkipRule {

        protected static Class<?> thisClass = SkipIfUsingJustReqLogout.class;

        @Override
        public Boolean callSpecificCheck() {

            boolean flag = currentRepeatAction.contains(Constants.HTTP_SESSION) && vSettings.sessionLogoutEndpoint == null;
            Log.info(thisClass, "callSpecificCheck", "Is using just req.logout() without either end_session or logout on the OP: " + Boolean.toString(flag));
            recordTestIfSkipped(flag);
            return flag;
        }

    }

    public static class SkipIfUsingSAMLIDPInitiatedLogout extends MySkipRule {

        protected static Class<?> thisClass = SkipIfUsingSAMLIDPInitiatedLogout.class;

        @Override
        public Boolean callSpecificCheck() {

            boolean flag = currentRepeatAction.contains(Constants.SAML_IDP_INITIATED_LOGOUT);
            Log.info(thisClass, "callSpecificCheck", "Is using " + Constants.SAML_IDP_INITIATED_LOGOUT + ": " + Boolean.toString(flag));
            recordTestIfSkipped(flag);
            return flag;
        }

    }

    @AfterClass
    public static void afterClass() {
        Log.info(thisClass, "afterClass", "Resetting useLdap to: " + defaultUseLdap);
        useLdap = defaultUseLdap;
    }

    public static RepeatTests addRepeat(RepeatTests rTests, SecurityTestRepeatAction currentRepeat) {
        Log.info(thisClass, "addRepeat", "Adding repeat: " + currentRepeat);
        if (rTests == null) {
            return RepeatTests.with(currentRepeat);
        } else {
            return rTests.andWith(currentRepeat);
        }
    }

    public static void printRandom(String method) {
        Log.info(thisClass, method, "********************************************************************************************************************");
        Log.info(thisClass, method, "                 RRRRRRRR AAAAAAAA N      N DDDDDDD   OOOOOO  MMMM MMM");
        Log.info(thisClass, method, "                 R      R A      A NN     N D      D O      O M  MM  M");
        Log.info(thisClass, method, "                 R      R A      A N N    N D      D O      O M  MM  M");
        Log.info(thisClass, method, "                 RRRRRRRR A      A N  N   N D      D O      O M      M");
        Log.info(thisClass, method, "                 RR       AAAAAAAA N   N  N D      D O      O M      M");
        Log.info(thisClass, method, "                 R R      A      A N    N N D      D O      O M      M");
        Log.info(thisClass, method, "                 R  R     A      A N     NN D      D O      O M      M");
        Log.info(thisClass, method, "                 R   R    A      A N      N D      D O      O M      M");
        Log.info(thisClass, method, "                 R    R   A      A N      N DDDDDDD   OOOOOO  M      M");
        Log.info(thisClass, method, "********************************************************************************************************************");

    }

    public static void makeRandomSettingSelections() throws Exception {

        String thisMethod = "makeRandomSettingSelections";
        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        tokenType = propagationTokenTypes[0];

        // Randomly enable/disable securityIntegration for this class instance to give more coverage
        httpSessionEnabled = Utils.getRandomSelection("true", "false");
        httpSessionEnabled = "true";

        printRandom(thisMethod);
        Log.info(thisClass, "makeRandomSettingSelections", "inited tokenType to: " + tokenType);
        Log.info(thisClass, "makeRandomSettingSelections", "inited securityIntegrationEnabled to: " + httpSessionEnabled);
        printRandom(thisMethod);

    }

    public static String adjustServerConfig(String configFileName) throws Exception {

        return configFileName.replace(".xml", "_" + httpSessionEnabled + ".xml");
    }

    public String buildContextRoot() throws Exception {

        String contextRoot = null;
        if (currentRepeatAction.contains(SocialConstants.SOCIAL)) {
            contextRoot = SocialConstants.DEFAULT_CONTEXT_ROOT;
        } else {
            contextRoot = Constants.OIDC_CLIENT_DEFAULT_CONTEXT_ROOT;
        }

        return contextRoot;
    }

    public static void sharedSetUp() throws Exception {

        currentRepeatAction = RepeatTestFilter.getRepeatActionsAsString();

        makeRandomSettingSelections();

        vSettings = new VariationSettings(currentRepeatAction);

        // Start a normal OP, or an OP that uses SAML to authorize (in this case, we need to fire up a server running Shibboleth
        sharedStartProviderBasedOnRepeat(tokenType);

        // start an OIDC RP or a Social oidc client
        sharedStartClientBasedOnRepeat(tokenType);

        sharedRegisterClientsIfNeeded();

    }

    /**
     * If the current repeat is a SAML variation, start a Shibboleth IDP and an OP with a samlWebSso20 client. That client will be
     * used to authorize using the SAML IDP.
     * Otherwise, start a standard OIDC OP.
     *
     * @param tokenType
     *            flag to be passed to common tooling to set config settings in the OP to have it create opaque or jwt
     *            access_tokens
     * @throws Exception
     */
    @SuppressWarnings("serial")
    public static void sharedStartProviderBasedOnRepeat(String tokenType) throws Exception {

        List<String> serverApps = new ArrayList<String>() {
            {
                add(Constants.OAUTHCLIENT_APP); // need this app to get the refresh forms
            }
        };

        // For tests using httpsessionlogout, we need an intermediate app to perform the logout (including making calls to individual bcl endpoints on the RPs)
        if (currentRepeatAction.contains(Constants.HTTP_SESSION)) {
            serverApps.add(Constants.simpleLogoutApp);
        }

        if (vSettings.loginMethod.equals(Constants.SAML)) {
            Log.info(thisClass, "setUp", "pickAnIDP: " + pickAnIDP);
            testIDPServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth", "server_orig.xml", Constants.IDP_SERVER_TYPE, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.SKIP_CHECK_FOR_SECURITY_STARTED, true);
            pickAnIDP = true; // tells commonSetup to update the OP server files with current/this instance IDP server info
            testIDPServer.setRestoreServerBetweenTests(false);
            testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op.saml", adjustServerConfig("op_server_basicTests.xml"), Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            // now, we need to update the IDP files
            shibbolethHelpers.fixSPInfoInShibbolethServer(testOPServer, testIDPServer);
            shibbolethHelpers.fixVarsInShibbolethServerWithDefaultValues(testIDPServer);
            // now, start the shibboleth app with the updated config info
            shibbolethHelpers.startShibbolethApp(testIDPServer);
            testOPServer.addIgnoredServerException(MessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
        } else {
            useLdap = false;
            Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);
            if (currentRepeatAction.contains(Constants.MONGODB)) {
                List<String> extraMsgs = new ArrayList<String>();
                extraMsgs.add("CWWKZ0001I.*" + Constants.OAUTHCONFIGMONGO_START_APP);
                testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op.mongo", adjustServerConfig("op_server_basicTests.xml"), Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.USE_MONGODB, extraMsgs, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT, Constants.JUNIT_REPORTING);
                // register clients after all servers are started and we know everyone's ports
                testSettings.setStoreType(StoreType.CUSTOM);
            } else {
                testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op", adjustServerConfig("op_server_basicTests.xml"), Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            }
        }

        Map<String, String> vars = new HashMap<String, String>();

        if (currentRepeatAction.contains(SocialConstants.SOCIAL)) {
            // social client
            bclRoot = "ibm/api/social-login/backchannel_logout";
        } else {
            // openidconnect client
            bclRoot = "oidcclient/backchannel_logout";
        }
        vars.put("bclRoot", bclRoot);

        updateServerSettings(testOPServer, vars);

        testOPServer.setRestoreServerBetweenTests(false);
        SecurityFatHttpUtils.saveServerPorts(testOPServer.getServer(), Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        testOPServer.addIgnoredServerExceptions(MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI, MessageConstants.CWWKS5215E_NO_AVAILABLE_SP, "CWWKE0702E", "CWWKF0029E");

    }

    /**
     * If the current repeat is an OIDC or SAML variation, start an OIDC RP client, otherwise, start a Social oidc client
     *
     * @param tokenType
     *            flag to be passed to common tooling to set config settings in the OP to have it create opaque or jwt
     *            access_tokens
     * @throws Exception
     */
    @SuppressWarnings("serial")
    public static void sharedStartClientBasedOnRepeat(String tokenType) throws Exception {

        List<String> clientApps = new ArrayList<String>() {
            {
                add(Constants.APP_FORMLOGIN);
                add(Constants.backchannelLogoutApp);
            }
        };

        // For tests using httpsessionlogout, we need an intermediate app to perform the logout (including making calls to individual bcl endpoints on the RPs)
        if (currentRepeatAction.contains(Constants.HTTP_SESSION)) {
            clientApps.add(Constants.simpleLogoutApp);
        }

        Map<String, String> vars = new HashMap<String, String>();
        if (vSettings.loginMethod.equals(Constants.OIDC) || vSettings.loginMethod.equals(Constants.SAML)) {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", adjustServerConfig("rp_server_basicTests.xml"), Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            vars = updateClientCookieNameAndPort(clientServer, "clientCookieName", Constants.clientCookieName);
            testSettings.setFlowType(Constants.RP_FLOW);
        } else {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", adjustServerConfig("social_server_basicTests.xml"), Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            vars = updateClientCookieNameAndPort(clientServer, "clientCookieName", Constants.clientCookieName);
            testSettings.setFlowType(SocialConstants.SOCIAL);
            clientServer.addIgnoredServerExceptions(MessageConstants.CWWKG0058E_CONFIG_MISSING_REQUIRED_ATTRIBUTE, MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP, MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED); // the social client isn't happy with the public client not having a secret
        }

        updateServerSettings(clientServer, vars);

        SecurityFatHttpUtils.saveServerPorts(clientServer.getServer(), Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        clientServer.addIgnoredServerExceptions(MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR, MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);

        if (currentRepeatAction.contains(Constants.HTTP_SESSION)) {
            if (currentRepeatAction.contains(Constants.OIDC_RP) || currentRepeatAction.contains(SocialConstants.SOCIAL)) {
                logoutApp = clientServer.getHttpsString() + "/simpleLogoutTestApp/simpleLogout";
            } else {
                logoutApp = testOPServer.getHttpsString() + "/simpleLogoutTestApp/simpleLogout";
            }
        }

    }

    public static void sharedRegisterClientsIfNeeded() throws Exception {

        if (currentRepeatAction.contains(Constants.MONGODB)) {
            Log.info(thisClass, "registerClientsIfNeeded", "Setting up mongo clients");
            regClients = new BackChannelLogout_RegisterClients(testOPServer, clientServer, null);
            regClients.registerClientsForBasicBCLTests();

        }
    }

    /**
     * Build the backchannel logout url based on the rp server hostinfo and the client to be "logged out"
     *
     * @param client
     *            client name
     * @return - the logout url
     * @throws Exception
     */
    public String buildBackchannelLogoutUri(String client) throws Exception {
        return buildBackchannelLogoutUri(clientServer, client);
    }

    public String buildBackchannelLogoutUri(TestServer server, String client) throws Exception {

        String contextRoot = buildContextRoot();

        String part2 = (contextRoot + Constants.OIDC_BACK_CHANNEL_LOGOUT_ROOT + client).replace("//", "/");
        String uri = server.getHttpsString() + part2;
        Log.info(thisClass, "_testName", "backchannelLogouturi: " + uri);
        testClient = client;

        return uri;
    }

    public List<endpointSettings> createParmFromBuilder(JWTTokenBuilder builder) throws Exception {
        return createParmFromBuilder(builder, false, "JOSE", "jwt");
    }

    /**
     * Creates a list of parameters that will be added to the logout request. In our case, the list only contains one pair.
     * { "logout_token", built_logout_token}
     * This method tells the method that actually builds the parms to encrypt the token that it builds.
     *
     * @param builder
     *            the populated builder to use to generate the actual logout token
     * @param encrypt
     *            flag indicating if the token will be encrypted (or just signed)
     * @return - the "list" of parms that can be added to the logout request - in this case, just one pair { "logout_token",
     *         built_logout_token}
     * @throws Exception
     */
    public List<endpointSettings> createParmFromBuilder(JWTTokenBuilder builder, boolean encrypt) throws Exception {
        return createParmFromBuilder(builder, encrypt, "JOSE", "jwt");
    }

    /**
     * Creates a list of parameters that will be added to the logout request. In our case, the list only contains one pair.
     * This method will build either a signed token, or a signed and encrypted token
     *
     *
     * @param builder
     *            the populated builder to use to generate the actual logout token
     * @param encrypt
     *            flag indicating if the token should be signed and encrypted, or just signed
     * @param type
     *            if the token is to be encrypted, this is the value of the "typ" claim
     * @param contentType
     *            if the token is to be encrypted, this is the value of the "cty" claim
     * @return - the "list" of parms that can be added to the logout request - in this case, just one pair { "logout_token",
     *         built_logout_token}
     * @throws Exception
     */
    public List<endpointSettings> createParmFromBuilder(JWTTokenBuilder builder, boolean encrypt, String type, String contentType) throws Exception {

        String thisMethod = "createParmFromBuilder";

        Log.info(thisClass, thisMethod, "claims: " + builder.getJsonClaims());

        String logoutToken = null;
        if (encrypt) {
            logoutToken = builder.buildJWE(type, contentType);
        } else {
            logoutToken = builder.buildAsIs();
        }

        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        if (logoutToken == null) {
            fail("Test failed to create the logout token");
        }

        return parms;

    }

    public boolean isUsingSaml() throws Exception {
        if (vSettings != null) {
            return vSettings.loginMethod.equals(Constants.SAML);
        } else {
            // some test classes don't use vSettings
            return currentRepeatAction.toUpperCase().contains(Constants.SAML);
        }
    }

    /**
     * Updates a JWTBuilder with the signature settings for the HS algorithm specified
     *
     * @param builder
     *            the JWTTokenBuilder to update signature settings
     * @param alg
     *            - the HS algorithm to use for signing
     * @return - return an updated builder with HS signature settings based on alg
     * @throws Exception
     */
    public JWTTokenBuilder updateLogoutTokenBuilderWithHSASignatureSettings(JWTTokenBuilder builder, String alg) throws Exception {

        String thisMethod = "updateLogoutTokenBuilderWithHSASignatureSettings";

        Log.info(thisClass, thisMethod, "HS alg: " + alg + " HSAKey: " + Constants.sharedHSSharedKey);
        builder = builder.setAlorithmHeaderValue(alg);

        builder = builder.setHSAKey(Constants.sharedHSSharedKey); // using the same secret in all of our HS configs
        Log.info(thisClass, thisMethod, "claims: " + builder.getJsonClaims());

        return builder;

    }

    /**
     * Updates a JWTBuilder with the signature settings for the non HS algorithm specified (RS or ES)
     *
     * @param builder
     *            the JWTTokenBuilder to update signature settings
     * @param alg
     *            - the non-HS algorithm to use for signing
     * @return - return an updated builder with non-HS signature settings based on alg
     * @throws Exception
     */
    public JWTTokenBuilder updateLogoutTokenBuilderWithNonHSASignatureSettings(JWTTokenBuilder builder, String alg) throws Exception {

        String thisMethod = "setNonHSASignedBuilderForLogoutToken";

        String fullPathKeyFile = serverFileUtils.getServerFileLoc(clientServer.getServer()) + "/" + alg + "private-key.pem";
        Log.info(thisClass, thisMethod, "Using private key from: " + fullPathKeyFile);
        builder.setAlorithmHeaderValue(alg);
        if (alg.contains("RS")) {
            builder.setRSAKey(fullPathKeyFile);
        } else {
            builder.setECKey(fullPathKeyFile.replace("key", "key-pkcs#8"));
        }

        Log.info(thisClass, thisMethod, "claims: " + builder.getJsonClaims());

        return builder;

    }

    /**
     * Calls updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings with the appropriate JWE header values based on the
     * alg (header values are different for RS/ES).
     * updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings will update the JWTBuilder with the signature settings for
     * the RS or
     * ES algorithm specified - sets the encryption header claims alg and enc to default test values as well as setting encryption
     * key based on the algorithm passed in.
     *
     * @param builder
     *            the JWTTokenBuilder to update signature/encryption settings
     * @param alg
     *            the alg to use for signing and encrypting
     * @return - return a default populated builder signed and encrypted based on alg
     * @throws Exception
     */
    public JWTTokenBuilder updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(JWTTokenBuilder builder, String alg) throws Exception {
        if (alg.startsWith("ES")) {
            return updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(builder, alg, JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_ES);
        } else {
            return updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(builder, alg, JwtConstants.DEFAULT_CONTENT_ENCRYPT_ALG, JwtConstants.DEFAULT_KEY_MGMT_KEY_ALG);
        }

    }

    /**
     * Update the JWTBuilder with the signature settings for the RS or ES algorithm specified - sets the encryption header claims
     * alg and enc to default test values as well as setting encryption key based on the algorithm passed in.
     *
     * @param builder
     *            the JWTTokenBuilder to update signature/encryption settings
     * @param alg
     *            the alg to use for signing and encrypting
     * @param encryptAlg
     *            the value to set the "alg" header claim to
     * @param keyMgmtKeyAlg
     *            the value to set the "enc" header claim to
     * @return - return a populated builder signed and encrypted based on alg
     * @throws Exception
     */
    public JWTTokenBuilder updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings(JWTTokenBuilder builder, String alg, String encryptAlg, String keyMgmtKeyAlg) throws Exception {

        String thisMethod = "updateLogoutTokenBuilderWithNonHSASignatureAndEncryptionSettings";

        // update the signature settings
        builder = updateLogoutTokenBuilderWithNonHSASignatureSettings(builder, alg);

        Log.info(thisClass, thisMethod, "alg: " + alg + " encryptAlg: " + encryptAlg + " keyMgmtKeyAlg: " + keyMgmtKeyAlg);

        // get the key
        String rawKey = JwtKeyTools.getComplexPublicKeyForSigAlg(clientServer.getServer(), alg);

        // update the builder's encryption settings
        builder.setKeyManagementKey(JwtKeyTools.getPublicKeyFromPem(rawKey));
        builder = builder.setContentEncryptionAlg(encryptAlg);
        builder = builder.setKeyManagementKeyAlg(keyMgmtKeyAlg);

        Log.info(thisClass, thisMethod, "claims: " + builder.getJsonClaims());

        return builder;

    }

    /**
     * Creates and returns a JWTTokenBuilder with the values found in an id_token (caller has parsed the id_token and saved the
     * values in a JwtTokenForTest object)
     *
     * @param idTokenData
     *            the JwtTokenForTest containing the id_token content
     * @return a JWTTokenBuilder object containing the values from the id_token
     * @throws Exception
     */
    public JWTTokenBuilder createBuilderFromIdToken(JwtTokenForTest idTokenData) throws Exception {

        String thisMethod = "createBuilderFromIdToken";
        msgUtils.printMethodName(thisMethod);

        JWTTokenBuilder builder = new JWTTokenBuilder();

        Map<String, Object> idTokenDataHeaders = idTokenData.getMapHeader();
        String alg = getStringValue(idTokenDataHeaders, Constants.HEADER_ALGORITHM);
        if (alg == null) {
            fail("Signature algorithm was missing in the id_token - that shouldn't happen");
        }
        builder = updateLogoutTokenBuilderWithHSASignatureSettings(builder, alg);

        Map<String, Object> idTokenDataClaims = idTokenData.getMapPayload();

        String issuer = getStringValue(idTokenDataClaims, Constants.PAYLOAD_ISSUER); // required
        builder.setIssuer((issuer != null) ? issuer : "IssuerNotInIdToken");

        String subject = getStringValue(idTokenDataClaims, Constants.PAYLOAD_SUBJECT); // optional (our id_token should always have it)
        if (subject != null) {
            builder.setSubject(subject);
        }

        String sessionId = getStringValue(idTokenDataClaims, Constants.PAYLOAD_SESSION_ID); // optional (our id_token should always have it)
        if (subject != null) {
            builder.setClaim(Constants.PAYLOAD_SESSION_ID, sessionId);
        }

        String audience = getStringValue(idTokenDataClaims, Constants.PAYLOAD_AUDIENCE); // required
        builder.setAudience((audience != null) ? audience : "AudienceNotInIdToken");

        builder.setIssuedAtToNow(); // required
        builder.setExpirationTimeMinutesIntheFuture(2); // required
        builder.setGeneratedJwtId(); // will ensure a unique jti for each test

        JSONObject events = new JSONObject();
        events.put(Constants.logoutEventKey, new JSONObject());
        builder.setClaim("events", events); // required

        return builder;
    }

    public void restoreAppMap(String client) throws Exception {
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutUri/" + client,
                Constants.PUTMETHOD, "resetBCLLogoutTokenMap", null, null, vData.addSuccessStatusCodes(), testSettings);
    }

    public String getLogoutToken(String client) throws Exception {
        Object logoutResponse = genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutUri/" + client + "_postLogout",
                Constants.GETMETHOD, "getLogoutTokens", null, null, vData.addSuccessStatusCodes(), testSettings);

        String logoutToken = getLogoutTokenFromOutput(client + " - " + Constants.LOGOUT_TOKEN + ": ", logoutResponse);
        Log.info(thisClass, _testName, "Logout token: " + logoutToken);

        return logoutToken;
    }

    public Object accessProtectedApp(String client) throws Exception {
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(clientServer.getHttpsString() + "/formlogin/simple/" + client);

        return accessProtectedApp(webClient, updatedTestSettings);

    }

    public Object accessProtectedApp(WebClient webClient, TestSettings settings) throws Exception {
        return accessProtectedApp(webClient, null, settings);
    }

    public Object accessProtectedApp(WebClient webClient, Object previousResponse, TestSettings settings) throws Exception {
        // Access a protected app - using a normal RP flow
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getTestURL());

        return accessProtectedApp(webClient, previousResponse, settings, expectations);
    }

    public Object accessProtectedApp(WebClient webClient, Object previousResponse, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "accessProtectedApp";
        msgUtils.printMethodName(thisMethod);

        Object response = null;

        if (isUsingSaml()) {
            response = genericRP(_testName, webClient, settings, previousResponse, Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT_WITH_SAML, expectations);
        } else {
            response = genericRP(_testName, webClient, settings, previousResponse, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
        }
        testHelpers.testSleep(1); // on some of the fast systems, we're getting identical tokens created because the iat is only down to the second - this causes issues with logout tests.
        return response;
    }

    /**
     * Attempt to access the protected app after we've run logout/end_session.
     * We'll only attempt the first step of invoking the protected app. If the webClient still has cookies, we'll get to the app,
     * otherwise, we'll be prompted to log in.
     *
     * @param webClient
     *            the context to use
     * @param settings
     *            test case settings to use to make the request
     * @param alreadyLoggedIn
     *            flag indicating that if we should or should not have access to protected app without having to log in
     * @throws Exception
     */
    public Object accessAppAfterLogout(WebClient webClient, TestSettings settings, AfterLogoutStates states) throws Exception {

        String thisMethod = "accessAppAfterLogout";
        validationLogger("Starting", thisMethod);

        webClient.getOptions().setJavaScriptEnabled(true);

        List<validationData> postLogoutExpectations = vData.addSuccessStatusCodes();
        // make sure we landed on the app if any of the cookies exist
        //        if (states.getOpCookieExists() || states.getOpJSessionIdExists() || states.getClientCookieExists() || states.getClientJSessionIdExists()) {
        //        if (states.getIsAppSessionAccess() || (loginMethod.equals(Constants.SAML) && logoutMethodTested.equals(Constants.LOGOUT_ENDPOINT))) {
        if (states.getIsAppSessionAccess()) {
            if (states.getIsTokenLimitExceeded()) {
                //                if (states.getIsTokenLimitExceeded() && isUsingSaml()) {
                postLogoutExpectations = setTooManyLoginsExpectations(false, Constants.GET_LOGIN_PAGE);
            } else {
                postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getTestURL());
                postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find the FormLoginServlet output in the response", null, Constants.FORMLOGIN_SERVLET);
            }
        } else {
            states.setClientJSessionIdMatchesPrevious(false); // client JSEssionId will exist (and match the value from the login) before this attempt, it will exist but be a new value after
            if (!isUsingSaml()) {
                //                postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not Redirect to OP", null, "Redirect To OP");
                postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on the login page", null, "Login");
                postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Landed on the test app after a logout and should NOT have", null, settings.getTestURL());
            } else {
                postLogoutExpectations = vData.addExpectation(postLogoutExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on Web Login Service.", null, "Web Login Service");
            }
        }

        // If we're getting access, it'll be through the cookies and not propagation, so there should be no difference in behavior for Social clients
        Object response = genericRP(_testName, webClient, settings, Constants.GET_LOGIN_PAGE_ONLY, postLogoutExpectations);
        testHelpers.testSleep(1); // on some of the fast systems, we're getting identical tokens created because the iat is only down to the second - this causes issues with logout tests.

        validationLogger("Ending", thisMethod);

        return response;
    }

    /**
     * Attempt to use the access_token from the original login - verify that it is valid or invalid based on the state value
     * passed
     *
     * @param settings
     *            test case settings to use to make the requests
     * @param tokenKeeper
     *            the TokenKeeper object containing the access_token to check
     * @param states
     *            the AfterLogoutStates object containing the flag indicating if the calling test expects the access_token to be
     *            valid at this time
     * @throws Exception
     */
    public void validateAccessToken(TestSettings settings, TokenKeeper tokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateAccessToken";
        validationLogger("Starting", thisMethod);

        String action = "POST_LOGOUT_ACCESS_TOKEN_CHECK";

        List<validationData> accessTokenExpectations = null;

        TestSettings accessTokenSettings = settings.copyTestSettings();
        if (accessTokenSettings.getFlowType().equals(SocialConstants.SOCIAL)) {
            accessTokenSettings.setProtectedResource(settings.getProtectedResource().replace("simple/", "simple/oidc_"));
        }

        if (states.getIsAccessTokenValid()) {
            accessTokenExpectations = vData.addExpectation(accessTokenExpectations, action, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, accessTokenSettings.getProtectedResource());
        } else {
            accessTokenExpectations = vData.addExpectation(accessTokenExpectations, action, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on the login page", null, "Login");
            accessTokenExpectations = vData.addExpectation(accessTokenExpectations, action, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Landed on the test app after a logout and should NOT have", null, accessTokenSettings.getProtectedResource());
            // Social doesn't support propagation, so we need to use an openidConnectClient proetected app to test the access token
            //            if (accessTokenSettings.getFlowType().equals(SocialConstants.SOCIAL)) {
            //
            //            } else {
            if (states.getIsUsingIntrospect()) {
                if (states.getIsUsingInvalidIntrospect()) {
                    accessTokenExpectations = validationTools.addMessageExpectation(clientServer, accessTokenExpectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find a message indicating that the access token could not be recognized by the validation end point.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID);
                    accessTokenExpectations = validationTools.addMessageExpectation(clientServer, accessTokenExpectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find a message indicating that the access token could not be recognized by the validation end point.", accessTokenSettings.getIntrospectionEndpt());
                } else {
                    accessTokenExpectations = validationTools.addMessageExpectation(clientServer, accessTokenExpectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token could not be validated (using introspection).", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
                    accessTokenExpectations = validationTools.addMessageExpectation(testOPServer, accessTokenExpectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token could not be validated (token not valid or expired).", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
                }
            } else {
                accessTokenExpectations = validationTools.addMessageExpectation(clientServer, accessTokenExpectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token could not be validated (using introspection).", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);
            }
            accessTokenExpectations = validationTools.addMessageExpectation(clientServer, accessTokenExpectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the inbound request was invalid.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);
            //            }
        }

        helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), tokenKeeper.getAccessToken(), Constants.HEADER, accessTokenSettings, accessTokenExpectations, action);

        validationLogger("Ending", thisMethod);

    }

    public void accessProtectedAppViaPropagation(String accessToken, TestSettings settings, List<validationData> expectations, String action) throws Exception {

        TestSettings accessTokenSettings = settings.copyTestSettings();
        if (accessTokenSettings.getFlowType().equals(SocialConstants.SOCIAL)) {
            String updatedProtectedApp = accessTokenSettings.getProtectedResource();
            accessTokenSettings.setProtectedResource(updatedProtectedApp.replace("simple/", "simple/oidc_"));
        }

        Log.info(thisClass, "accessProtectedAppViaPropagation", "Protected App: " + accessTokenSettings.getProtectedResource());
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, accessTokenSettings.getProtectedResource());
        helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), accessToken, Constants.HEADER, accessTokenSettings, expectations, action);
    }

    /**
     * Invoke the refresh_token endpoint after logging out - check that the request succeeds or fails based on the flag passed in
     * (it should fail when the BCL succeeded)
     *
     * @param settings
     *            test case settings to use to make the requests
     * @param tokenKeeper
     *            the TokenKeeper object containing the refresh_token to check
     * @param states
     *            the AfterLogoutStates object containing the flag indicating if the calling test expects the refresh_token to be
     *            valid at this time
     * @throws Exception
     */
    public void validateRefreshToken(TestSettings settings, TokenKeeper tokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateRefreshToken";
        validationLogger("Starting", thisMethod);

        List<validationData> refreshTokenExpectations = null;

        if (states.getIsRefreshTokenValid()) {
            refreshTokenExpectations = vData.addSuccessStatusCodes();
            refreshTokenExpectations = vData.addExpectation(refreshTokenExpectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);
        } else {
            // set expectations for refresh token no longer valid
            refreshTokenExpectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_REFRESH_ENDPOINT);
            refreshTokenExpectations = vData.addResponseStatusExpectation(refreshTokenExpectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.BAD_REQUEST_STATUS);
            refreshTokenExpectations = vData.addExpectation(refreshTokenExpectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not receive error message trying to refresh token", null, ".*" + Constants.ERROR_RESPONSE_DESCRIPTION + ".*" + MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE);
        }
        invokeGenericForm_refreshToken(_testName, getAndSaveWebClient(true), settings, tokenKeeper.getRefreshToken(), refreshTokenExpectations);

        validationLogger("Ending", thisMethod);

    }

    /**
     * Invokes a protected client on the RP/Social client and logs in to get access.
     * Grab the id_token from that response.
     * Use the values from that id_token to populate a jwt builder which will later be used to create a logout_token
     *
     * @param client
     *            - the RP client that we'll be using - the app contains this name and our filters direct us to use this client
     * @return a jwt builder populated with values from the generated id_token
     * @throws Exception
     */
    public JWTTokenBuilder loginAndReturnIdTokenData(String client) throws Exception {
        Object response = accessProtectedApp(client);
        // grab the id_token that was created and store its contents in a JwtTokenForTest object
        String id_token = null;
        if (currentRepeatAction.contains(Constants.OIDC)) {
            id_token = validationTools.getIDTokenFromOutput(response);
        } else {
            id_token = validationTools.getTokenFromResponse(response, "ID token:");
        }
        Log.info(thisClass, _testName, "id token: " + id_token);
        JwtTokenForTest idTokenData = gatherDataFromToken(id_token, testSettings); // right now none of the tests need a diff sig alg, if they do, we'll need to pass an updated testSettings into this method

        JWTTokenBuilder builder = createBuilderFromIdToken(idTokenData);

        return builder;

    }

    /**
     * Invoke the back channel logout endpoint validating the response
     *
     * @param bclEndpoint
     *            the endpoint to invoke
     * @param parms
     *            the parms to pass (the logout_token parm and value)
     * @param expectations
     *            what to expect (status code and possible messages in the server side log)
     * @throws Exception
     */
    public void invokeBcl(String bclEndpoint, List<endpointSettings> parms, List<validationData> expectations) throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        genericInvokeEndpoint(_testName, webClient, null, bclEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    public String getStringValue(Map<String, Object> claims, String key) throws Exception {

        Object objValue = claims.get(key);
        if (objValue != null) {
            Log.info(thisClass, "getStringValue", "value: " + objValue.toString() + " value type: " + objValue.getClass());
            String value = objValue.toString().replace("\"", "");
            return value;
        }
        return null;
    }

    public List<validationData> initLogoutExpectations(String logoutPage) throws Exception {
        return initLogoutExpectations(BCL_FORM.VALID, logoutPage, true);
    }

    public List<validationData> initLogoutExpectations(BCL_FORM bclForm, String logoutPage) throws Exception {
        return initLogoutExpectations(bclForm, logoutPage, true);
    }

    public List<validationData> initLogoutExpectations(BCL_FORM bclForm, String logoutPage, boolean usingLoginClient) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();

        if (vSettings.logoutMethodTested.equals(Constants.SAML_IDP_INITIATED_LOGOUT)) {
            expectations = vData.addExpectation(expectations, Constants.PROCESS_LOGOUT_PROPAGATE_YES, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not land on the SAML logout confirmation page", null, "\"result\":  \"Success\"");
        } else {
            if (isUsingSaml() && !vSettings.logoutMethodTested.equals(Constants.HTTP_SESSION)) {
                if (usingLoginClient) {
                    if (bclForm == BCL_FORM.OMITTED && vSettings.isLogoutEndpointInvoked) {
                        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on Logout page.", null, Constants.LOGOUT_TITLE);
                        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the successful logout message.", null, "Logout successful");
                    } else {
                        if (vSettings.logoutMethodTested.equals(Constants.IBM_SECURITY_LOGOUT)) {
                            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on Web Login Service.", null, Constants.ibm_security_logout_default_page);
                        } else {
                            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on Web Login Service.", null, "Web Login Service");
                        }
                        if (bclForm == BCL_FORM.TEST_BCL && vSettings.isLogoutEndpointInvoked) {
                            expectations = vData.addExpectation(expectations, Constants.PROCESS_LOGOUT_PROPAGATE_YES, Constants.RESPONSE_URL, Constants.STRING_MATCHES, "Did not land on the post back channel logout test app", null, "https://localhost:" + testOPServer.getServer().getBvtSecurePort() + "/ibm/saml20/spOP/slo");
                        } else {
                            expectations = vData.addExpectation(expectations, Constants.PROCESS_LOGOUT_PROPAGATE_YES, Constants.RESPONSE_URL, Constants.STRING_MATCHES, "Did not land on the post back channel logout test app", null, logoutPage);
                        }
                    }
                } else {
                    expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_MATCHES, "Did not land on the post back channel logout test app", null, logoutPage);
                }
            } else {
                expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_MATCHES, "Did not land on the post back channel logout test app", null, logoutPage);
            }
        }
        return expectations;
    }

    /**
     * Create the general expectations for a logout token that contains something invalid. Then add an expectation for a missing
     * claim message.
     *
     * @return standard expectations for an invalid login token + a missing claim error message
     * @throws Exception
     */
    public List<validationData> setMissingBCLRequestClaimExpectations(String claim) throws Exception {

        List<validationData> expectations = setInvalidBCLRequestExpectations();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a claim [" + claim + "] was missing.", MessageConstants.CWWKS1545E_BACK_CHANNEL_LOGOUT_MISSING_REQUIRED_CLAIM + ".*" + claim);
        return expectations;
    }

    /**
     * Create the general expectations for a logout token that contains something invalid. Then add an expectation for an error
     * that the caller passes in.
     *
     * @return standard expectations for an invalid login token + the caller's error
     * @throws Exception
     */
    public List<validationData> setInvalidBCLRequestExpectations(String specificMsg) throws Exception {

        List<validationData> expectations = setInvalidBCLRequestExpectations();
        if (specificMsg != null && !specificMsg.equals("")) {
            expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain expected message [" + specificMsg + "].", specificMsg);
        }

        return expectations;

    }

    /**
     * Create the general expectations for a logout token that contains something invalid.
     * We expect a status code of 400, with a response message of "Bad Request". We'll also receive msgs CWWKS1541E and CWWKS1543E
     * in the RP's message log.
     *
     * @return standard expectations for an invalid login token
     * @throws Exception
     */
    public List<validationData> setInvalidBCLRequestExpectations() throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.BAD_REQUEST + "\" was not found in the reponse message", null, Constants.BAD_REQUEST);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout encountered an error", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the back channel logout request could not be validated", MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);
        return expectations;

    }

    public List<validationData> addRPLogoutCookieExpectations(List<validationData> expectations, String clientCookie, String clientJSessionCookie, boolean shouldNotExist) throws Exception {

        if (shouldNotExist) {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_CONTAIN, "Cookie \"" + clientCookie + "\" was found in the response and should not have been.", null, clientCookie);
            if (clientJSessionCookie == null) {
                fail("addRPLogoutCookieExpectations failure: Could not set up expectation for JSESSIONID check - no " + Constants.clientJSessionIdName + " value found/passed in");
            }
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_MATCH, "Cookie " + Constants.clientJSessionIdName + " was found in the response with a value that should have been updated.", null, Constants.clientJSessionIdName + ".*" + clientJSessionCookie + ".*");
            // previous JSession should not match current jSession
        } else {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_CONTAINS, "Cookie \"" + clientCookie + "\" was NOT found in the response and should have been.", null, clientCookie);
        }
        return expectations;
    }

    public List<validationData> addOPLogoutCookieExpectations(List<validationData> expectations, boolean shouldNotExist) throws Exception {

        if (shouldNotExist) {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_DOES_NOT_CONTAIN, "Cookie \"" + Constants.opCookieName + "\" was found in the response and should not have been.", null, Constants.opCookieName);
        } else {
            expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_COOKIE, Constants.STRING_CONTAINS, "Cookie \"" + Constants.opCookieName + "\" was NOT found in the response and should have been.", null, Constants.opCookieName);
        }
        return expectations;
    }

    public List<validationData> initLogoutWithPublicClientFailureExpectations(String logoutPage, String client) throws Exception {

        String logoutStep = Constants.LOGOUT;

        List<validationData> expectations = initLogoutExpectations(logoutPage);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, logoutStep, Constants.TRACE_LOG, Constants.STRING_MATCHES, "Trace log did not contain message indicating that the back channel logout uri could not be invoked.", client + ".*" + MessageConstants.CWWKS2300E_HTTP_WITH_PUBLIC_CLIENT);

        return expectations;
    }

    public String getLogoutTokenFromOutput(String tokenName, Object response) throws Exception {

        String thisMethod = "getIDTokenFromOutput";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, " Searching for key:  " + tokenName);

        String respReceived = AutomationTools.getResponseText(response);

        if (!respReceived.contains(tokenName)) {
            throw new Exception("logout_token is missing");
        }

        int start = respReceived.indexOf(tokenName);
        String theValue = respReceived.substring(start + tokenName.length(), respReceived.length()).split(System.getProperty("line.separator"))[0];
        Log.info(thisClass, thisMethod, tokenName + " " + theValue);
        if (!theValue.isEmpty()) {
            return theValue;
        }

        throw new Exception("logout_token is missing");

    }

    public String getLogoutTokenFromMessagesLog(TestServer server, String tokenName) throws Exception {

        String thisMethod = "getLogoutTokenFromMessagesLog";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, " Searching for key:  " + tokenName);

        String searchResult = server.getServer().waitForStringInLogUsingMark(tokenName, server.getServer().getMatchingLogFile(Constants.MESSAGES_LOG));
        Log.info(thisClass, thisMethod, "DEBUG: ********************************************************************");
        Log.info(thisClass, thisMethod, searchResult);
        Log.info(thisClass, thisMethod, "DEBUG: ********************************************************************");
        if (searchResult != null) {
            int start = searchResult.indexOf(tokenName);
            int len = tokenName.length();
            if (start == -1) {
                start = searchResult.indexOf("logout_token: ");
                len = "logout_token: ".length();
            }
            Log.info(thisClass, thisMethod, "start: " + start + " length: " + searchResult.length());
            String theValue = searchResult.substring(start + len, searchResult.length() - 1);
            Log.info(thisClass, thisMethod, tokenName + " " + theValue);
            if (!theValue.isEmpty()) {
                return theValue;
            }
        }
        return searchResult;

    }

    public List<String> getLogoutTokensFromMessagesLog(TestServer server, String tokenName) throws Exception {

        String thisMethod = "getLogoutTokensFromMessagesLog";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, " Searching for key:  " + tokenName);

        List<String> tokens = new ArrayList<String>();
        List<String> searchResults = server.getServer().findStringsInLogsUsingMark(tokenName, server.getServer().getMatchingLogFile(Constants.MESSAGES_LOG));
        Log.info(thisClass, thisMethod, "DEBUG: ********************************************************************");
        for (String searchResult : searchResults) {
            Log.info(thisClass, thisMethod, searchResult);
        }
        Log.info(thisClass, thisMethod, "DEBUG: ********************************************************************");
        if (searchResults.size() != 0) {
            for (String searchResult : searchResults) {
                int start = searchResult.indexOf(tokenName);
                int len = tokenName.length();
                if (start == -1) {
                    start = searchResult.indexOf("logout_token: ");
                    len = "logout_token: ".length();
                }
                Log.info(thisClass, thisMethod, "start: " + start + " length: " + searchResult.length());
                String theValue = searchResult.substring(start + len, searchResult.length() - 1);
                Log.info(thisClass, thisMethod, tokenName + " " + theValue);
                if (!theValue.isEmpty()) {
                    tokens.add(theValue);
                }
            }
        }
        return tokens;

    }

    public JwtTokenForTest gatherDataFromToken(String token, TestSettings settings) throws Exception {

        String thisMethod = "gatherDataFromIdToken";

        try {
            validationTools.setExpectedSigAlg(settings);

            String decryptKey = settings.getDecryptKey();

            JwtTokenForTest jwtToken;
            if (JweHelper.isJwe(token) && decryptKey != null) {
                jwtToken = new JwtTokenForTest(token, decryptKey);
            } else {
                jwtToken = new JwtTokenForTest(token);
            }

            jwtToken.printJwtContent();

            return jwtToken;

        } catch (

        Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating id_token in response");
            throw e;
        }
    }

    public String getServerConfigVar(TestServer server, String varToGet) throws Exception {

        String thisMethod = "getServerConfigVar";
        ServerConfiguration config = server.getServer().getServerConfiguration();
        Variable var = config.getVariables().getBy("name", varToGet);
        String value = var.getValue();
        Log.info(thisClass, thisMethod, "Returning var: " + varToGet + " value of: " + value);
        return value;

    }

    /**
     * Update/Set config variables for a server and push the updates to the server.
     * Method waits for server to update or indicate that no update in needed
     *
     * @param server
     *            - ref to server that will be updated
     * @param valuesToSet
     *            - a map of the variables and their values to set
     * @throws Exception
     */
    public static void updateServerSettings(TestServer server, Map<String, String> valuesToSet) throws Exception {
        updateServerSettings(server, valuesToSet, true);
    }

    public static void updateServerSettings(TestServer server, Map<String, String> valuesToSet, boolean serverAlreadyStarted) throws Exception {

        String thisMethod = "updateServerSettings";
        ServerConfiguration config = server.getServer().getServerConfiguration();
        ConfigElementList<Variable> configVars = config.getVariables();

        for (Variable variableEntry : configVars) {
            Log.info(thisClass, thisMethod, "Already set configVar: " + variableEntry.getName() + " configVarValue: " + variableEntry.getValue());
        }

        for (Entry<String, String> variableEntry : valuesToSet.entrySet()) {
            updateConfigVariable(configVars, variableEntry.getKey(), variableEntry.getValue());
        }

        server.getServer().updateServerConfiguration(config);
        if (serverAlreadyStarted) {
            server.getServer().waitForConfigUpdateInLogUsingMark(null);
        }
        //        helpers.testSleep(5);
    }

    /**
     * Update test settings with test case specific values - caller assumes user/password will be testuser/testuserpwd and that we
     * will be using a post logout
     *
     * @param provider
     *            the OP provider that the openidconnect client belongs to
     * @param client
     *            the openidconnect client that the test uses - we're using this as part of the test app names
     * @return updated test settings
     * @throws Exception
     */
    protected TestSettings updateTestSettingsProviderAndClient(String provider, String client) throws Exception {

        return updateTestSettingsProviderAndClient(provider, client, true);

    }

    /**
     * Update test settings with test case specific values - caller assumes user/password will be testuser/testuserpwd
     *
     * @param provider
     *            the OP provider that the openidconnect client belongs to
     * @param client
     *            the openidconnect client that the test uses - we're using this as part of the test app names
     * @param usePostLogout
     *            flag indicating if OP Provider config that the test uses specifies a post logout url this will (used to tell the
     *            logout test method to pass the post logout url in its request - it has to match whats in the config - the values
     *            set based on the flag also tells our validation code where we would finally land)
     * @return updated test settings
     * @throws Exception
     */
    protected TestSettings updateTestSettingsProviderAndClient(String provider, String client, boolean usePostLogout) throws Exception {

        return updateTestSettingsProviderAndClient(clientServer, provider, client, Constants.TESTUSER, Constants.TESTUSERPWD, usePostLogout);

    }

    /**
     * Update test settings with test case specific values
     *
     * @param server
     *            the clientServer instance that the test will use (there may be multiple RPs/Social clients
     * @param provider
     *            the OP provider that the openidconnect client belongs to
     * @param client
     *            the openidconnect client that the test uses - we're using this as part of the test app names
     * @param user
     *            the test user to use
     * @param passwd
     *            the password for the test user
     * @param usePostLogout
     *            flag indicating if OP Provider config that the test uses specifies a post logout url this will (used to tell the
     *            logout test method to pass the post logout url in its request - it has to match whats in the config - the values
     *            set based on the flag also tells our validation code where we would finally land)
     * @return updated test settings
     * @throws Exception
     */
    protected TestSettings updateTestSettingsProviderAndClient(TestServer server, String provider, String client, String user, String passwd, boolean usePostLogout) throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();

        updatedTestSettings.setProvider(provider);
        updatedTestSettings.setTestURL(server.getHttpsString() + "/formlogin/simple/" + client);
        updatedTestSettings.setProtectedResource(server.getHttpsString() + "/formlogin/simple/" + client);
        // set logout url - end_session
        if (vSettings.logoutMethodTested.equals(Constants.SAML_IDP_INITIATED_LOGOUT)) {
            updatedTestSettings.setEndSession(testIDPServer.getHttpsString() + "/idp/profile/Logout");
        } else {
            if (vSettings.logoutMethodTested.equals(Constants.SAML_SP_INITIATED_LOGOUT)) {
                updatedTestSettings.setEndSession(testOPServer.getHttpsString() + "/ibm/saml20/defaultSP/logout");
            } else {
                updatedTestSettings.setEndSession(updatedTestSettings.getEndSession().replace("OidcConfigSample", provider));
                if (vSettings.logoutMethodTested.equals(Constants.IBM_SECURITY_LOGOUT)) {
                    updatedTestSettings.setEndSession(updatedTestSettings.getEndSession().replace(Constants.END_SESSION_ENDPOINT, Constants.IBM_SECURITY_LOGOUT));
                } else {
                    if (vSettings.logoutMethodTested.equals(Constants.LOGOUT_ENDPOINT)) {
                        updatedTestSettings.setEndSession(updatedTestSettings.getEndSession().replace(Constants.END_SESSION_ENDPOINT, Constants.LOGOUT_ENDPOINT));
                    }
                }
            }
        }
        updatedTestSettings.setRevocationEndpt(updatedTestSettings.getRevocationEndpt().replace("OidcConfigSample", provider));
        updatedTestSettings.setTokenEndpt(updatedTestSettings.getTokenEndpt().replace("OidcConfigSample", provider));
        updatedTestSettings.setClientID(client);
        updatedTestSettings.setClientSecret("mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger"); // all of the clients are using the same secret
        updatedTestSettings.setUserName(user);
        updatedTestSettings.setUserPassword(passwd);
        updatedTestSettings.setAdminUser(user);
        updatedTestSettings.setAdminPswd(passwd);
        updatedTestSettings.setScope("openid profile");
        if (usePostLogout) {
            updatedTestSettings.setPostLogoutRedirect(server.getHttpString() + Constants.postLogoutJSessionIdApp);
        } else {
            updatedTestSettings.setPostLogoutRedirect(null);
        }

        return updatedTestSettings;

    }

    /**
     * Update a servers variable map with the key/value passed in.
     *
     * @param vars
     *            - map of existing variables
     * @param name
     *            - the key to add/update
     * @param value
     *            - the value for the key specified
     */
    protected static void updateConfigVariable(ConfigElementList<Variable> vars, String name, String value) {

        String thisMethod = "updateConfigVariable";
        Variable var = vars.getBy("name", name);
        if (var == null) {
            Log.info(thisClass, "updateConfigVariable", name + " doesn't appear to exist, so no update is needed.");
        } else {
            Log.info(thisClass, thisMethod, "Updating var: " + name + " to value: " + value);
            var.setValue(value);
        }
    }

    public static Map<String, String> updateClientCookieNameAndPort(TestServer server, String cookieName, String cookieNameValue) throws Exception {
        return updateClientCookieNameAndPort(server, cookieName, cookieNameValue, true);
    }

    public static Map<String, String> updateClientCookieNameAndPort(TestServer server, String cookieName, String cookieNameValue, boolean isServerStarted) throws Exception {

        Map<String, String> vars = new HashMap<String, String>();

        Log.info(thisClass, "updateClientCookieNameAndPort", "server: " + server.getServer().getServerName());
        Log.info(thisClass, "updateClientCookieNameAndPort", "secure port: " + server.getServerHttpsPort());

        vars.put(cookieName, cookieNameValue);
        vars.put("client2Port", Integer.toString(server.getServerHttpsPort()));

        updateServerSettings(server, vars, isServerStarted);

        return vars;

    }

    protected List<validationData> addDidInvokeBCLExpectation(List<validationData> expectations, TestSettings settings, String theNum) throws Exception {

        String theInstance = ".*";

        if (theNum != null) {
            theInstance = " - " + theNum;
        }
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + settings.getClientID() + theInstance);

        return expectations;
    }

    protected List<validationData> addDidNotInvokeBCLExpectation(List<validationData> expectations, TestSettings settings, String theNum) throws Exception {
        // We expect NOT to find the message, so increase the allowable
        // number of timeout messages in output.txt to account for this
        // missing message
        addToAllowableTimeoutCount(1);

        String theInstance = ".*";

        if (theNum != null) {
            theInstance = " - " + theNum;
        }
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.MSG_NOT_LOGGED, "Message log contained a message indicating that a bcl request was made for client " + settings.getClientID(), ".*BackChannelLogout_logMsg_Servlet: " + settings.getClientID() + theInstance);

        return expectations;
    }

    protected void validationLogger(String action, String method) throws Exception {

        msgUtils.printMethodNameBlock(action + " " + method);
        loggingUtils.logTestStepInServerLog(clientServer.getServer(), action, method);
        loggingUtils.logTestStepInServerLog(testOPServer.getServer(), action, method);

    }

    /**
     * Invoke validateLogoutResult passing the default client cookie name (assumes no second client server is being used) and
     * passing along all of the parms passed into it
     *
     * @param webClient
     *            the context to use check if we can still access the protected resource
     * @param settings
     *            the test case settings to use to make requests
     * @param previousTokenKeeper
     *            this object stores the cookies/tokens from the successful login
     * @param states
     *            an object that contains flags indicating what should/should not exist or be valid after the logout/end_session
     * @throws Exception
     */
    public void validateLogoutResult(WebClient webClient, TestSettings settings, TokenKeeper previousTokenKeeper, AfterLogoutStates states) throws Exception {
        // just use the default client cookie names (for what would be RP1/social client1 )
        validateLogoutResult(webClient, settings, Constants.clientCookieName, Constants.clientJSessionIdName, previousTokenKeeper, states);
    }

    /**
     * Validates tokens/app access/cookies after logout/end_session completes.
     *
     * @param webClient
     *            the context to use check if we can still access the protected resource
     * @param settings
     *            the test case settings to use to make requests
     * @param clientCookieName
     *            the client cookie name to use (in the future, we may use 2 client servers and this will let us know which one
     *            we're using
     * @param previousTokenKeeper
     *            this object stores the cookies/tokens from the successful login
     * @param states
     *            an object that contains flags indicating what should/should not exist or be valid after the logout/end_session
     * @throws Exception
     */
    public void validateLogoutResult(WebClient webClient, TestSettings settings, String clientCookieName, String clientJSessionIdName, TokenKeeper previousTokenKeeper, AfterLogoutStates states) throws Exception {

        String thisMethod = "validateLogoutResult";
        validationLogger("Starting", thisMethod);

        states.printStates();
        generateHtml(states);

        // show that we can/can't access the protected app using just the webClient
        // This request will help flush client cookies that were invalidated, but not removed from the
        // browser session/webClient
        accessAppAfterLogout(webClient, settings, states);

        // populate token keeper after trying to access app so that all cookies are flushed
        TokenKeeper currentTokenKeeper = setTokenKeeperFromUnprotectedApp(webClient, settings, 2);

        validateOPCookies(previousTokenKeeper, currentTokenKeeper, states, settings);
        validateClientCookies(previousTokenKeeper, currentTokenKeeper, clientCookieName, clientJSessionIdName, states, settings);

        // show that can/can't access the protected app using the previously created access_token
        validateAccessToken(settings, previousTokenKeeper, states);
        // show that can/can't use the previously created refresh_token
        validateRefreshToken(settings, previousTokenKeeper, states);

        validationLogger("Ending", thisMethod);

    }

    public TokenKeeper setTokenKeeperFromUnprotectedApp(WebClient webClient, TestSettings settings, int numAccesses) throws Exception {

        List<validationData> unprotectedExpectations = vData.addSuccessStatusCodes();
        TestSettings upprotectedTestSettings = settings.copyTestSettings();
        upprotectedTestSettings.setTestURL(settings.getTestURL().replace(Constants.APP_FORMLOGIN, "unprotected" + Constants.APP_FORMLOGIN).replace("simple/", "simple/unProtected"));

        Object response = null;
        for (int cnt = 0; cnt < numAccesses; cnt++) {
            response = genericRP(_testName, webClient, upprotectedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, unprotectedExpectations);
        }

        TokenKeeper tokenKeeper = new TokenKeeper(webClient, response, settings.getFlowType());

        return tokenKeeper;
    }

    public void validateOPCookies(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states, TestSettings settings) throws Exception {

        String thisMethod = "validateOPCookies";
        validationLogger("Starting", thisMethod);

        validateOPCookie(beforeLogoutTokenKeeper, currentTokenKeeper, states, settings);
        validateSPCookie(beforeLogoutTokenKeeper, currentTokenKeeper, states, settings);
        validateIDPCookie(beforeLogoutTokenKeeper, currentTokenKeeper, states, settings);

        validationLogger("Ending", thisMethod);

    }

    public void validateOPCookie(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states, TestSettings settings) throws Exception {

        String thisMethod = "validateOPCookie";
        validationLogger("Starting", thisMethod);

        genericCookieValidator(Constants.opCookieName, beforeLogoutTokenKeeper.getOPCookie(), currentTokenKeeper.getOPCookie(), states.getOpCookieExists(), states.getOpCookieMatchesPrevious(), states.getOpCookieStillValid(), settings, states.getIsTokenLimitExceeded());

        validationLogger("Ending", thisMethod);

    }

    public void validateOPJSessionId(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states, TestSettings settings) throws Exception {

        String thisMethod = "validateOPJSessionId";
        validationLogger("Starting", thisMethod);

        genericCookieValidator(Constants.opJSessionIdName, beforeLogoutTokenKeeper.getOPJSessionId(), currentTokenKeeper.getOPJSessionId(), states.getOpJSessionIdExists(), states.getOpJSessionIdMatchesPrevious(), states.getOpJSessionIdStillValid(), settings, states.getIsTokenLimitExceeded());

        validationLogger("Ending", thisMethod);

    }

    public void validateClientCookies(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, String clientCookieName, String clientJSessionIdName, AfterLogoutStates states, TestSettings settings) throws Exception {

        String thisMethod = "validateClientCookies";
        validationLogger("Starting", thisMethod);

        validateClientCookie(beforeLogoutTokenKeeper, currentTokenKeeper, clientCookieName, states, settings);
        // TODO -      validateClientJSessionId(beforeLogoutTokenKeeper, currentTokenKeeper, clientJSessionIdName, states);

        validationLogger("Ending", thisMethod);

    }

    public void validateClientCookie(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, String clientCookieName, AfterLogoutStates states, TestSettings settings) throws Exception {

        String thisMethod = "validateClientCookie";
        validationLogger("Starting", thisMethod);

        if (clientCookieName.equals(Constants.clientCookieName)) {
            genericCookieValidator(Constants.clientCookieName, beforeLogoutTokenKeeper.getClientCookie(), currentTokenKeeper.getClientCookie(), states.getClientCookieExists(), states.getClientCookieMatchesPrevious(), states.getClientCookieStillValid(), settings, states.getIsTokenLimitExceeded());
        } else {
            // needed if we ever use 2 clients in testing - also need to add client2 methods to the states class
            genericCookieValidator(Constants.client2CookieName, beforeLogoutTokenKeeper.getClient2Cookie(), currentTokenKeeper.getClient2Cookie(), states.getClient2CookieExists(), states.getClient2CookieMatchesPrevious(), states.getClient2CookieStillValid(), settings, states.getIsTokenLimitExceeded());
        }

        validationLogger("Ending", thisMethod);

    }

    public void validateClientJSessionId(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, String clientJSessionIdName, AfterLogoutStates states, TestSettings settings) throws Exception {

        String thisMethod = "validateClientJSessionId";
        validationLogger("Starting", thisMethod);

        if (clientJSessionIdName.equals(Constants.clientJSessionIdName)) {
            genericCookieValidator(Constants.clientJSessionIdName, beforeLogoutTokenKeeper.getClientJSessionId(), currentTokenKeeper.getClientJSessionId(), states.getClientJSessionIdExists(), states.getClientJSessionIdMatchesPrevious(), states.getClientJSessionIdStillValid(), settings, states.getIsTokenLimitExceeded());
        } else {
            // needed if we ever use 2 clients in testing - also need to add client2 methods to the states class
            genericCookieValidator(Constants.client2JSessionIdName, beforeLogoutTokenKeeper.getClient2JSessionId(), currentTokenKeeper.getClient2JSessionId(), states.getClient2JSessionIdExists(), states.getClient2JSessionIdMatchesPrevious(), states.getClient2JSessionIdStillValid(), settings, states.getIsTokenLimitExceeded());
        }

        validationLogger("Ending", thisMethod);

    }

    public void validateSPCookie(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states, TestSettings settings) throws Exception {

        String thisMethod = "validateSPCookie";
        msgUtils.printMethodName("Start - " + thisMethod);
        if (isUsingSaml()) {
            genericCookieValidator(Constants.spCookieName, beforeLogoutTokenKeeper.getSPCookie(), currentTokenKeeper.getSPCookie(), states.getSpCookieExists(), states.getSpCookieMatchesPrevious(), states.getSpCookieStillValid(), settings, states.getIsTokenLimitExceeded());
        } else {
            Log.info(thisClass, thisMethod, "Skipping checks since they are only valid with SAML and this instance does NOT use SAML");
        }
    }

    public void validateIDPCookie(TokenKeeper beforeLogoutTokenKeeper, TokenKeeper currentTokenKeeper, AfterLogoutStates states, TestSettings settings) throws Exception {

        String thisMethod = "validateIDPCookie";
        msgUtils.printMethodName("Start - " + thisMethod);
        if (isUsingSaml()) {
            // TODO
            Log.info(thisClass, thisMethod, "NO IMPLEMENTED YET");
        } else {
            Log.info(thisClass, thisMethod, "Skipping checks since they are only valid with SAML and this instance does NOT use SAML");
        }

    }

    public void genericCookieValidator(String cookieName, String beforeCookie, String afterCookie, boolean afterCookieShouldExist, boolean afterCookieShouldMatchBeforeCookie, boolean beforeCookieIsStillValid, TestSettings settings, boolean isTokenLimitExceeded) throws Exception {

        String thisMethod = "genericCookieValidator";
        validationLogger("Start - ", thisMethod);
        Log.info(thisClass, thisMethod, "Before: " + beforeCookie);
        Log.info(thisClass, thisMethod, "After : " + afterCookie);

        if (afterCookieShouldExist) {
            if (afterCookie == null || afterCookie.equals(Constants.NOT_FOUND)) {
                fail("genericCookieValidator failure: Cookie \"" + cookieName + "\" was NOT found in the response and should have been.");
            }
            Log.info(thisClass, thisMethod, "The cookie [" + cookieName + "] was found as it should have been.");
            if (afterCookieShouldMatchBeforeCookie) {
                if (beforeCookie == null) {
                    fail("genericCookieValidator failure: Could not validate the cookie [" + cookieName + "] - the previous value of the cookie was not provided.");
                }
                if (!beforeCookie.equals(afterCookie)) {
                    fail("genericCookieValidator failure: The cookie [" + cookieName + "] with value [" + afterCookie + "] does not match the previous value [" + beforeCookie + "].");
                }
                Log.info(thisClass, thisMethod, "The cookie [" + cookieName + "] value was valid.");
            } else {
                if (beforeCookie == null) { // we know that the current cookie is NOT null
                    Log.info(thisClass, thisMethod, "The cookie [" + cookieName + "] value was valid.");
                } else {
                    if (beforeCookie.equals(afterCookie)) {
                        fail("genericCookieValidator failure: The before and after cookies should not match, but they do - Both cookie instances for [" + cookieName + "] - are set to: [" + afterCookie + "].");
                    }
                }
                Log.info(thisClass, thisMethod, "The after logout cookie [" + cookieName + "] did not match the cookie used during login.");
            }
        } else {
            if (afterCookie != null && !afterCookie.equals(Constants.NOT_FOUND)) {
                fail("genericCookieValidator failure: Cookie \"" + cookieName + "\" was found in the response and should not have been.");
            }
            Log.info(thisClass, thisMethod, "The cookie [" + cookieName + "] was NOT found as it should NOT have been.");
        }

        validationLogger("Cookie validation - ", thisMethod);

        if (beforeCookie != null) {
            // check if the original cookie is still valid
            WebClient webClient = getAndSaveWebClient(true);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getCookieManager().addCookie(new Cookie("localhost", cookieName, beforeCookie));

            List<validationData> cookieExpectations = vData.addSuccessStatusCodes();
            // make sure we landed on the app if any of the cookies exist
            //        if (states.getOpCookieExists() || states.getOpJSessionIdExists() || states.getClientCookieExists() || states.getClientJSessionIdExists()) {
            //        if (states.getIsAppSessionAccess() || (loginMethod.equals(Constants.SAML) && logoutMethodTested.equals(Constants.LOGOUT_ENDPOINT))) {
            if (beforeCookieIsStillValid) {
                if (isTokenLimitExceeded) {
                    cookieExpectations = setTooManyLoginsExpectations(false, Constants.GET_LOGIN_PAGE);
                } else {
                    cookieExpectations = vData.addExpectation(cookieExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getTestURL());
                    cookieExpectations = vData.addExpectation(cookieExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find the FormLoginServlet output in the response", null, Constants.FORMLOGIN_SERVLET);
                }
            } else {
                if (!isUsingSaml()) {
                    //                cookieExpectations = vData.addExpectation(cookieExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not Redirect to OP", null, "Redirect To OP");
                    cookieExpectations = vData.addExpectation(cookieExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on the login page", null, "Login");
                    cookieExpectations = vData.addExpectation(cookieExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Landed on the test app after a logout and should NOT have", null, settings.getTestURL());
                } else {
                    cookieExpectations = vData.addExpectation(cookieExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on Web Login Service.", null, "Web Login Service");
                }
            }

            // If we're getting access, it'll be through the cookies and not propagation, so there should be no difference in behavior for Social clients
            genericRP(_testName, webClient, settings, Constants.GET_LOGIN_PAGE_ONLY, cookieExpectations);
            testHelpers.testSleep(1); // on some of the fast systems, we're getting identical tokens created because the iat is only down to the second - this causes issues with logout tests.

        }
        validationLogger("Ending", thisMethod);

    }

    public Object invokeLogout(WebClient webClient, TestSettings settings, List<validationData> logoutExpectations, Object previousResponse) throws Exception {
        return invokeLogout(webClient, settings, logoutExpectations, previousResponse, true);
    }

    public Object invokeLogout(WebClient webClient, TestSettings settings, List<validationData> logoutExpectations, Object previousResponse, boolean multiStepLogout) throws Exception {
        String id_token = validationTools.getIDToken(settings, previousResponse);
        return invokeLogout(webClient, settings, logoutExpectations, id_token, multiStepLogout);
    }

    public Object invokeLogout(WebClient webClient, TestSettings settings, List<validationData> logoutExpectations, String token) throws Exception {
        return invokeLogout(webClient, settings, logoutExpectations, token, true);
    }

    public Object invokeLogout(WebClient webClient, TestSettings settings, List<validationData> logoutExpectations, String token, boolean multiStepLogout) throws Exception {
        String thisMethod = "invokeLogout";
        validationLogger("Start -", thisMethod);

        String opLogoutEndpoint = null;

        // Debug
        Log.info(thisClass, thisMethod, "Debug logoutMethodTested: " + vSettings.logoutMethodTested);
        Log.info(thisClass, thisMethod, "Debug finalApp: " + vSettings.finalAppWithPostRedirect);
        Log.info(thisClass, thisMethod, "Debug defaultApp: " + vSettings.finalAppWithoutPostRedirect);
        Log.info(thisClass, thisMethod, "Debug logoutApp: " + logoutApp);
        Log.info(thisClass, thisMethod, "Debug sessionLogoutEndpoint: " + vSettings.sessionLogoutEndpoint);
        Log.info(thisClass, thisMethod, "Debug token: " + token);

        //        for (validationData e : logoutExpectations) {
        //            e.printValidationData();
        //        }

        List<endpointSettings> parms = null;
        switch (vSettings.logoutMethodTested) {
        case Constants.SAML_IDP_INITIATED_LOGOUT: // update for idp/sp initiated
            return genericOP(_testName, webClient, settings, Constants.IDP_INITIATED_LOGOUT, logoutExpectations, null, token);
        case Constants.SAML_SP_INITIATED_LOGOUT: // update for idp/sp initiated
            return genericOP(_testName, webClient, settings, Constants.SP_INITIATED_LOGOUT, logoutExpectations, null, token);
        case Constants.REVOCATION_ENDPOINT:
            parms = eSettings.addEndpointSettings(parms, "client_id", settings.getClientID());
            parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
            parms = eSettings.addEndpointSettings(parms, "token", token);
            return genericInvokeEndpoint(_testName, webClient, null, settings.getRevocationEndpt(), Constants.POSTMETHOD, Constants.INVOKE_REVOCATION_ENDPOINT, parms, null, logoutExpectations, testSettings);
        case Constants.END_SESSION:
        case Constants.LOGOUT_ENDPOINT:
            // invoke end_session on the op - test controls if the id_token is passed as the id_token_hint by either passing or not passing the previous response
            String[] logoutActions = Constants.LOGOUT_ONLY_ACTIONS;
            if (isUsingSaml() && multiStepLogout) {
                logoutActions = new String[] { Constants.LOGOUT, Constants.PROCESS_LOGOUT_PROPAGATE_YES };
            }
            return genericOP(_testName, webClient, settings, logoutActions, logoutExpectations, null, token);
        case Constants.HTTP_SESSION:
            //            String id_token = null;
            if (vSettings.sessionLogoutEndpoint != null) {
                if (vSettings.sessionLogoutEndpoint.equals(Constants.LOGOUT_ENDPOINT)) {
                    opLogoutEndpoint = testOPServer.getHttpsString() + "/oidc/endpoint/" + settings.getProvider() + "/" + Constants.LOGOUT_ENDPOINT;
                } else {
                    //                    id_token = validationTools.getIDToken(settings, previousResponse);
                    //                }
                    opLogoutEndpoint = settings.getEndSession();
                } // else we want the endpoint to be null
            }

            if (opLogoutEndpoint != null) {
                parms = eSettings.addEndpointSettingsIfNotNull(parms, "opLogoutUri", opLogoutEndpoint);
            }
            if (token != null) {
                parms = eSettings.addEndpointSettingsIfNotNull(parms, "id_token_hint", token);
            }
            return genericInvokeEndpoint(_testName, webClient, null, logoutApp, Constants.POSTMETHOD, Constants.LOGOUT, parms, null, logoutExpectations, testSettings);
        case Constants.IBM_SECURITY_LOGOUT:
            return genericOP(_testName, webClient, settings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, null, token);
        default:
            fail("Logout method wasn't specified");
            return null;
        }

    }

    public List<validationData> setTooManyLoginsExpectations(boolean firstTime) throws Exception {
        return setTooManyLoginsExpectations(firstTime, null);
    }

    public List<validationData> setTooManyLoginsExpectations(boolean firstTime, String specifiedLoginStep) throws Exception {

        String loginStep = ((vSettings.loginMethod.equals(Constants.SAML) ? Constants.PERFORM_IDP_LOGIN : Constants.LOGIN_USER));
        if (specifiedLoginStep != null) {
            loginStep = specifiedLoginStep;
        }
        Log.info(thisClass, "setTooManyLoginsExpectations", "loginStep: " + loginStep);

        List<validationData> tooManyLoginsExpectations = vData.addSuccessStatusCodes(null, loginStep);
        tooManyLoginsExpectations = vData.addResponseStatusExpectation(tooManyLoginsExpectations, loginStep, Constants.BAD_REQUEST_STATUS);
        tooManyLoginsExpectations = vData.addExpectation(tooManyLoginsExpectations, loginStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive a message stating that there are too many logins for the user.", null, MessageConstants.CWOAU0066E_EXCEEDED_MAX_USER_REQUESTS);
        tooManyLoginsExpectations = validationTools.addMessageExpectation(testOPServer, tooManyLoginsExpectations, loginStep, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there are too many logins for the user and client id.", MessageConstants.CWOAU0054E_EXCEEDED_USER_CLIENT_TOKEN_LIMIT);
        if (firstTime) { // an ffdc is issued the first time we hit this - in some cases, we may hit this failure multiple times - subsequent calls won't get the ffdc
            tooManyLoginsExpectations = validationTools.addMessageExpectation(testOPServer, tooManyLoginsExpectations, loginStep, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there are too many login requests for the user.", MessageConstants.CWOAU0066E_EXCEEDED_MAX_USER_REQUESTS);
        }

        return tooManyLoginsExpectations;
    }

    public String greenCheckMark() throws Exception {
        return "<td>&#9989;</td>";
    }

    public String redX() throws Exception {
        return "<td>&#10060;</td>";
    }

    public static String skipIt() throws Exception {
        return "<td>Skip</td>";
    }

    public String notImplemented() throws Exception {
        return "<td>Not Implemented</td>";
    }

    public String NA() throws Exception {
        return "<td>N/A</td>";
    }

    public static String starIt() throws Exception {
        return "&#x2a;";
    }

    @After
    public void testReporting() throws Exception {

        Log.info(thisClass, "cleanUpReporting", _testName);
        recordTest(false, false, null);

    }

    public static void recordTestIfSkipped(boolean skipIt) {

        if (skipIt) {
            recordTest(skipIt, false, null);
        }
    }

    public static void recordTest(boolean skipIt, boolean continuation, String continueMsg) {

        if (continueMsg != null) {
            savedContinueMsg = continueMsg;
        }

        try {
            String theTestName = _testName;
            if (skipIt) {
                theTestName = "<td align=\"left\" style=\"background-color:LightSkyBlue;\">[Skip Test] " + _testName + "</td>";
            } else {
                if (starIt) {
                    theTestName = "<td align=\"left\">" + starIt() + " " + theTestName + "</td>";
                } else {
                    theTestName = "<td align=\"left\">" + theTestName + "</td>";
                }
            }

            int count = testResultString.split("<td>").length;
            int i = count;
            while (i < 8) {
                i++;
                testResultString = testResultString + skipIt();
            }

            if (printString.length() == 0) {
                Log.info(thisClass, "recordTest", "empty printString");
                printString = theTestName + testResultString;
            } else {
                Log.info(thisClass, "recordTest", "existing printString: " + printString);
                printString = printString + "</tr><tr><td align=\"center\">Continuation - " + savedContinueMsg + "</td>" + testResultString;
            }

            if (!continuation) { // when called from the "aftertest", continuation will be false
                Log.info(thisClass, "recordTest", "<recordTest><tr>" + printString + "</tr>");
                printString = "";
                savedContinueMsg = "";
            }
            testResultString = "";
            starIt = false;
            skipIt = false;
        } catch (Exception e) {
            Log.info(thisClass, "recordTest", "Exception thrown: " + e.getMessage());
        }
    }

    public void addStateToHtml(boolean exists) throws Exception {
        if (exists) {
            testResultString = testResultString + greenCheckMark();
        } else {
            testResultString = testResultString + redX();
        }
    }

    public void addStateToHtml(boolean exists, boolean doesItMatch, boolean isItValid) throws Exception {
        if (exists) {
            testResultString = testResultString + "<td>&#9989; / ";
        } else {
            testResultString = testResultString + "<td>&#10060; / ";
        }
        if (doesItMatch) {
            testResultString = testResultString + "<br>&#9989; / ";
        } else {
            testResultString = testResultString + "<br>&#10060; / ";
        }
        if (isItValid) {
            testResultString = testResultString + "<br>&#9989;</td>";
        } else {
            testResultString = testResultString + "<br>&#10060;</td>";
        }
    }

    public void generateHtml(AfterLogoutStates states) throws Exception {

        addStateToHtml(states.getIsAppSessionAccess());
        if (!isUsingSaml()) {
            addStateToHtml(states.getOpCookieExists(), states.getOpCookieMatchesPrevious(), states.getOpCookieStillValid());
            testResultString = testResultString + NA();
            testResultString = testResultString + NA();
        } else {
            testResultString = testResultString + NA();
            addStateToHtml(states.getSpCookieExists(), states.getSpCookieMatchesPrevious(), states.getSpCookieStillValid());
            //        addStateToHtml(states.getIdpCookieExists()) ;
            testResultString = testResultString + notImplemented();

        }
        addStateToHtml(states.getClientCookieExists(), states.getClientCookieMatchesPrevious(), states.getClientCookieStillValid());
        addStateToHtml(states.getIsAccessTokenValid());
        addStateToHtml(states.getIsRefreshTokenValid());
    }

    /**
     * Invoke the back channel logout app that logs a message and counts the number of times it is invoked
     * Each time the app is called (during a logout), it logs a message and increments a counter.
     * When we have tests that expect multiple bcl logouts to occur, we check the count created by this app to verify that the
     * correct number of logouts occurred.
     * This method causes that counter to be reset.
     *
     * @throws Exception
     */
    protected void resetBCLAppCounter() throws Exception {
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutLogMsg",
                Constants.PUTMETHOD, "resetBCLCounter", null, null, vData.addSuccessStatusCodes(), testSettings);

    }

}
