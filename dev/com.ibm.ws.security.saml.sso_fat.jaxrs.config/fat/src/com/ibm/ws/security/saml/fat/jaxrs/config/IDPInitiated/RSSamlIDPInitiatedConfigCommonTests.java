/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.jaxrs.config.IDPInitiated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.config.settings.KeystoreSettings;
import com.ibm.ws.security.fat.common.config.settings.SSLConfigSettings;
import com.ibm.ws.security.fat.common.config.settings.SSLSettings;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlConfigSettings;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlProviderSettings;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestServer;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.security.saml20.fat.commonTest.config.settings.AuthFilterSettings;
import com.ibm.ws.security.saml20.fat.commonTest.config.settings.RequestUrlSettings;
import com.ibm.ws.security.saml20.fat.commonTest.utils.RSCommonUtils;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link to an
 * application installed on a WebSphere SP. When the Web client invokes the SP
 * application, it is redirected to a TFIM IdP which issues a login challenge to
 * the Web client. The Web Client fills in the login form and after a successful
 * login, receives a SAML 2.0 token from the TFIM IdP. The client invokes the SP
 * application by sending the SAML 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class RSSamlIDPInitiatedConfigCommonTests extends SAMLCommonTest {

    private static final Class<?> thisClass = RSSamlIDPInitiatedConfigCommonTests.class;
    protected static String SPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.config.sp";
    protected static String APPServerName = "com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.config.rs";
    protected static RSCommonUtils commonUtils = new RSCommonUtils();
    protected static RSSamlConfigSettings rsConfigSettings = new RSSamlConfigSettings();
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;

    protected final boolean DO_NOT_CHECK_REALM = false;

    protected static final String REGISTRY_WITHOUT_IDP_USERS = "${server.config.dir}/imports/BasicRegistry_withoutIDPUsers.xml";
    protected static final String NO_REGISTRY = null;
    protected static final String DEFAULT_KEYSTORE_ID = "samlKeyStore";
    protected static final String DEFAULT_KEYSTORE_LOCATION = "${server.config.dir}/samlKey.jks";
    protected static final String DEFAULT_KEYSTORE_PASSWORD = "Liberty";
    protected static final String DEFAULT_TRUSTSTORE_ID = "serverStore";
    protected static final String DEFAULT_TRUSTSTORE_LOCATION = "${server.config.dir}/samlSslServerTrust.jks";
    protected static final String DEFAULT_TRUSTSTORE_PASSWORD = "LibertyServer";

    protected static final String APP_SERVER_ORIG_CONFIG = "server_1.xml";

    protected final String SP_ENCRYPTION_AES_128 = "sp_enc_aes128";
    protected final String SP_SIG_ALG_SHA256 = "sp2";
    protected final String DEFAULT_ENCRYPTION_KEY_USER = "CN=new_user2, O=IBM, C=US";

    String default2ServerUser = "testuser";
    String default2ServerUserPw = "security";

    // eanble case insensitive check of "cn" or "CN"
    String defaultIDPRealm = "(?i)cn=testuser";
    String defaultLocalRealm = "BasicRealm";
    String default2ServerCfgRealm = "saml.test";

    String default2ServerGroup = "group:" + defaultLocalRealm + "/group1";
    // String defaultIDPGroup = "group:" + defaultIDPRealm + "/users";
    String defaultIDPGroup = "group:" + defaultIDPRealm;
    String default2ServerCfgGroups = "group:" + default2ServerCfgRealm + "/" + default2ServerUser;
    String defaultNullGroup = "";

    String default2ServerServiceName = "FatSamlC02aService";
    String default2ServerServicePort = "SamlCallerToken02a";
    String default2ServerPolicy = "CallerHttpsPolicy";
    String default2ServerServiceClientTitle = "CXF SAML Caller Service Client";;

    String badValueString = "SomeBadValue";

    String userNameString = "NameID";
    String uniqueUserNameString = "unique nameID";
    String realmString = "realm";

    String testServerConfigFile = null;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.IDP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");

        setActionsForFlowType(flowType);

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraMsgs2 = new ArrayList<String>();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add("helloworld");

        setDefaultConfigSettings();

        copyMetaData = false;
        testIDPServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth", "server_orig.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.IDP_SERVER_TYPE, null, null, SAMLConstants.SKIP_CHECK_FOR_SECURITY_STARTED);
        copyMetaData = true;
        testAppServer = commonSetUp(APPServerName, APP_SERVER_ORIG_CONFIG, SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.SAML_APP_SERVER_TYPE, extraApps2, extraMsgs2, false);
        commonUtils.fixServer2Ports(testAppServer);
        copyMetaData = true;
        testSAMLServer = commonSetUp(SPServerName, "server_1.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.SAML_SERVER_TYPE, extraApps, extraMsgs);

        // now, we need to update the IDP files
        shibbolethHelpers.fixSPInfoInShibbolethServer(testSAMLServer, testIDPServer);
        shibbolethHelpers.fixVarsInShibbolethServerWithDefaultValues(testIDPServer);
        // now, start the shibboleth app with the updated config info
        startShibbolethApp(testIDPServer);

        setActionsForFlowType(flowType);

        // startSPWithIDPServer(SPServerName, "server_1.xml", extraMsgs,
        // extraApps, true);

        testSAMLServer.addIgnoredServerException("CWWKS5207W");

        testSAMLServer.getServer().copyFileToLibertyInstallRoot("lib/features", "internalFeatures/securitylibertyinternals-1.0.mf");
        helpers.setSAMLServer(testSAMLServer);
        servicePort = Integer.toString(testAppServer.getServerHttpPort());
        serviceSecurePort = Integer.toString(testAppServer.getServerHttpsPort());

        // set default values for jaxrs settings
        testSettings.setRSSettings();

        // set test app
        testSettings.setSpTargetApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI);
        testSettings.updatePartnerInSettings("sp1", true);

        // Allow the warning on the ignored attributes of samlWebSso20
        // inboundPropagation true or false
        testAppServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

        // don't restore server config at end of each test - each test
        // reconfigs, so there is no reason to restore the original config
        // between tests
        testSAMLServer.setRestoreServerBetweenTests(false);
        testAppServer.setRestoreServerBetweenTests(false);
        // some of the configs in the class are complex and cause the server to
        // take more than 2 minutes to refresh - allow up to 4 minutes
        testAppServer.setOverrideRestartWaitTime(2 * (120 * 1000)); // 2 x
                                                                    // default
                                                                    // value
    }

    @Before
    public void beforeTest() {
        testServerConfigFile = "server_" + testName.getMethodName() + ".xml";
        // move allow config warnings to overall setup - no need to add the same
        // entry for every single test
    }

    // super class endTest will skip the restoration of the server config
    // because setRestoreServerBetweenTests was set to false
    // No neeed to over-ride endTest

    /**
     * Sets the default config settings to be used. Unless a test specifically
     * overrides a setting, these settings will be used in the server
     * configuration.
     *
     * @throws Exception
     */
    protected static void setDefaultConfigSettings() throws Exception {

        String serverRoot = "${server.config.dir}";
        baseSamlServerConfig = "server_orig.xml.base";

        // need to use servlet31 with jaxrs2.0
        rsConfigSettings.setFeatureFile(serverRoot + "/imports/saml_rs_features.xml");
        rsConfigSettings.setSSLConfigSettingsFile(serverRoot + "/imports/goodSSLSettings.xml");

        // Most of the tests do not require a registry, so the argument is left
        // empty
        rsConfigSettings.setRegistryFiles();

        rsConfigSettings.setApplicationFiles(serverRoot + "/imports/helloworldApplication.xml");
        rsConfigSettings.setMiscFile(serverRoot + "/imports/misc2.xml");

        // Creates default rsSaml elements
        rsConfigSettings.setDefaultRSSamlProviderSettings();

        // Make minor updates to default settings
        RSSamlProviderSettings defaultRsSettings = rsConfigSettings.getDefaultRSSamlProviderSettings();
        defaultRsSettings.setHeaderName("saml_token");
        defaultRsSettings.setSignatureMethodAlgorithm("SHA1");
        defaultRsSettings.setAuthFilterRef(null);
        // Nullify some "pure" SAML attributes
        defaultRsSettings.setIdpMetadata(null);
        defaultRsSettings.setKeyAlias(null);
        defaultRsSettings.setKeyStoreRef(null);
    }

    /**
     * Copies the existing RS SAML config settings and updates the RS SAML
     * provider element to contain default values and settings.
     *
     * @param registryFile
     *            Path to registry file to include. If null, no registry will be
     *            set.
     * @return
     * @throws Exception
     */
    protected RSSamlConfigSettings copyDefaultSettingsForMappingTest(String registryFile) throws Exception {
        RSSamlConfigSettings updatedConfigSettings = rsConfigSettings.copyConfigSettings();

        // Set RS SAML provider settings
        RSSamlProviderSettings updatedRsSamlSettings = updatedConfigSettings.getDefaultRSSamlProviderSettings();
        updatedRsSamlSettings.setUserIdentifier("urn:oid:0.9.2342.19200300.100.1.1");
        updatedRsSamlSettings.setGroupIdentifier("groupHack");
        updatedRsSamlSettings.setUserUniqueIdentifier("urn:oid:0.9.2342.19200300.100.1.3");
        updatedRsSamlSettings.setRealmIdentifier("hackRealm");

        if (registryFile != null) {
            updatedConfigSettings.setRegistryFiles(registryFile);
        }

        return updatedConfigSettings;
    }

    /**
     * Sets the default auth filter settings and sets the default RS SAML
     * provider to have its authFilterRef attribute point to the default auth
     * filter. The auth filter is configured to protect the helloworld resource
     * within the helloworld application.
     *
     * @param testRsSamlSettings
     * @throws Exception
     */
    protected static void setUpDefaultAuthFilterSettings(RSSamlConfigSettings testRsSamlSettings) throws Exception {
        // Create and set the default auth filter settings
        testRsSamlSettings.setDefaultAuthFilterSettings();

        AuthFilterSettings defaultAuthFilterSettings = testRsSamlSettings.getDefaultAuthFilterSettings();
        String authFilterId = defaultAuthFilterSettings.getId();

        setUpAuthFilterSettings(testRsSamlSettings, authFilterId, SAMLConstants.PARTIAL_HELLO_WORLD_URI);
    }

    /**
     * Sets up auth filter settings with the provided id and sets the default RS
     * SAML provider to have its authFilterRef attribute point to the auth
     * filter. The auth filter is configured to protect the urlPattern provided.
     *
     * @param testRsSamlSettings
     * @param authFilterId
     * @param urlPattern
     * @throws Exception
     */
    protected static void setUpAuthFilterSettings(RSSamlConfigSettings testRsSamlSettings, String authFilterId, String urlPattern) throws Exception {
        // Set the authFilterRef in the default RS SAML provider to the provided
        // auth filter id
        AuthFilterSettings authFilterSettings = testRsSamlSettings.getDefaultAuthFilterSettings();
        authFilterSettings.setId(authFilterId);

        RSSamlProviderSettings updatedProviderSettings = testRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setAuthFilterRef(authFilterId);

        // Set up the requestUrl settings so the filter is configured to
        // protected the specified urlPattern
        RequestUrlSettings defaultReqUrlSettings = authFilterSettings.getDefaultRequestUrlSettings();
        defaultReqUrlSettings.setUrlPattern(urlPattern);
    }

    /**
     * Creates default SSL settings: <br/>
     * - sslDefault element with sslRef="goodSSLConfig" <br/>
     * - ssl element with id="goodSSLConfig",
     * keyStoreRef={@value #DEFAULT_KEYSTORE_ID}, trustStoreRef=
     * {@value #DEFAULT_TRUSTSTORE_ID} <br/>
     * - keyStore element with id={@value #DEFAULT_KEYSTORE_ID},
     * password={@value #DEFAULT_KEYSTORE_PASSWORD}, location=
     * {@value #DEFAULT_KEYSTORE_LOCATION} <br/>
     * - keyStore element with id={@value #DEFAULT_TRUSTSTORE_ID},
     * password={@value #DEFAULT_TRUSTSTORE_PASSWORD}, location=
     * {@value #DEFAULT_TRUSTSTORE_LOCATION} <br/>
     *
     * @param testRsSamlSettings
     * @return
     */
    protected RSSamlConfigSettings setDefaultSSLAndKeystoreSettings(RSSamlConfigSettings testRsSamlSettings) {
        SSLConfigSettings sslConfigSettings = testRsSamlSettings.getSSLConfigSettings();
        sslConfigSettings.setSslDefaultRef("goodSSLConfig");

        // We will programmatically create these settings instead of using an
        // included file
        sslConfigSettings.setIsIncludedFile(false);

        SSLSettings sslSettings = new SSLSettings();
        // Set the id to the sslRef value used by the sslDefault element
        sslSettings.setId(sslConfigSettings.getSslDefaultRef());
        sslSettings.setKeyStoreRef(DEFAULT_KEYSTORE_ID);
        sslSettings.setTrustStoreRef(DEFAULT_TRUSTSTORE_ID);

        // Create the keystore used by the configured ssl element
        KeystoreSettings samlKeyStore = new KeystoreSettings();
        // Set the id to the keyStoreRef value used by the ssl element
        samlKeyStore.setId(sslSettings.getKeyStoreRef());
        samlKeyStore.setPassword(DEFAULT_KEYSTORE_PASSWORD);
        samlKeyStore.setLocation(DEFAULT_KEYSTORE_LOCATION);

        // Create the truststore used by the configured ssl element
        KeystoreSettings serverStore = new KeystoreSettings();
        // Set the id to the trustStoreRef value used by the ssl element
        serverStore.setId(sslSettings.getTrustStoreRef());
        serverStore.setPassword(DEFAULT_TRUSTSTORE_PASSWORD);
        serverStore.setLocation(DEFAULT_TRUSTSTORE_LOCATION);

        // Add the SSL, keystore, and truststore settings into the config
        sslConfigSettings.setSSLSettings(sslSettings.getId(), sslSettings);
        sslConfigSettings.setKeystoreSettings(samlKeyStore.getId(), samlKeyStore);
        sslConfigSettings.setKeystoreSettings(serverStore.getId(), serverStore);

        testRsSamlSettings.setSSLConfigSettings(sslConfigSettings);
        return testRsSamlSettings;
    }

    /**
     * Adds the expectation to find the provided message in the specified log.
     * Also adds logMessage to the list of ignored server exceptions for this
     * particular server.
     *
     * @param expected
     * @param step
     * @param log
     * @param checkType
     * @param failureMessage
     * @param logMessage
     * @return
     * @throws Exception
     */
    protected List<validationData> addMessageExpectation(List<validationData> expected, String step, String log, String checkType, String failureMessage, String logMessage) throws Exception {
        if (checkType != null && checkType.equals(SAMLConstants.STRING_DOES_NOT_CONTAIN)) {
            // We expect NOT to find the message, so increase the allowable
            // number of timeout messages in output.txt to account for this
            // missing message
            addToAllowableTimeoutCount(1);
        }

        // Add the message to the ignored exceptions list for when this server
        // shuts down
        SAMLTestServer checkServer = testAppServer;
        if (log != null && log.equals(SAMLConstants.SAML_MESSAGES_LOG)) {
            checkServer = testSAMLServer;
        }

        helpers.addMessageExpectation(checkServer, expected, step, log, checkType, failureMessage, logMessage);

        return expected;
    }

    /**
     * Expects all good status codes, an encrypted assertion in the SAML token,
     * and the "Hello World!" string in the final response.
     *
     * @param settings
     * @return
     * @throws Exception
     */
    protected List<validationData> getExpectationsForEncryptionTest(SAMLTestSettings settings) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Ensure we validate the SAML token content for an EncryptedAssertion
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN_ENCRYPTED, SAMLConstants.STRING_CONTAINS,
                "Did not receive the expected encrypted SAML token content.", null, null);

        // Check for the "Hello World!" string
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS,
                "Did not receive the expected \"" + SAMLConstants.HELLO_WORLD_STRING + "\" string.", null, SAMLConstants.HELLO_WORLD_STRING);

        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected Issuer: " + settings.getIdpIssuer(), null, "SAMLIssuerName:" + settings.getIdpIssuer());
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected user name: " + settings.getIdpUserName(), null, settings.getIdpUserName());
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive the expected Audience Restriction: " + settings.getSpConsumer(), null, "audienceRestriction:[" + settings.getSpConsumer() + "]");

        return expectations;
    }

    /**
     * Build expectations to validate identity tests.
     *
     * @param realmName
     * @param securityName
     *            User/principal to validate
     * @param groupIds
     * @return
     * @throws Exception
     */
    public List<validationData> buildExpectationsWithIdentityInfo(String realmName, String securityName, String groupIds) throws Exception {

        String realmSearchString = ".*realmName=" + realmName + ".*";
        // String realmSearchString = ".*realmName=cn=" + securityName + ".*";
        String securityNameSearchString = ".*securityName=" + securityName + ".*";
        String groupSearchString = ".*groupIds=\\[" + groupIds + ".*\\].*";

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES,
                "Did not receive the expected realm name settings.", null, realmSearchString);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES,
                "Did not receive the expected security name settings.", null, securityNameSearchString);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES,
                "Did not receive the expected group settings.", null, groupSearchString);

        return expectations;
    }

    /**
     * Performs the common steps of a positive test - reconfiguring the server
     * and going through the JAX-RS flow. Caller passes in values specific to
     * its test that should be validated in the resulting subject.
     *
     * @param serverCfgFile
     *            - name of the server 2 config file to reconfig to
     * @param expectedRealm
     * @param expectedUser
     * @param expectedGroup
     * @throws Exception
     */
    public void generalPositiveTest(String serverCfgFile, String expectedRealm, String expectedUser, String expectedGroup) throws Exception {

        testAppServer.reconfigServer(buildSPServerName(serverCfgFile), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        genericSAML(_testName, webClient, testSettings, throughJAXRSGet, buildExpectationsWithIdentityInfo(expectedRealm, expectedUser, expectedGroup));

    }

    /**
     * Performs the common steps of a not found exception type test -
     * reconfiguring the server and going through the JAX-RS flow. These tests
     * expect a failure due to a user or registry not being found.
     *
     * @param serverCfgFile
     *            - name of the server 2 config file to reconfig to
     * @throws Exception
     */
    public void notfoundExceptionTest(String serverCfgFile) throws Exception {

        testAppServer.reconfigServer(buildSPServerName(serverCfgFile), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        List<validationData> expectations = get401ExpectationsForJaxrsGet("Did not get expected message saying no user registry service was available.",
                SAMLMessageConstants.CWWKS3005E_NO_USER_REGISTRY);
        addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, null,
                "Did not get expected message saying authentication failed.", SAMLMessageConstants.CWWKS5072E_AUTHN_UNSUCCESSFUL + ".+\\[" + default2ServerUser + "\\]");

        genericSAML(_testName, webClient, testSettings, throughJAXRSGet, expectations);

    }

    /**
     * Performs the common steps of a bad attribute type test - reconfiguring
     * the server and going through the JAX-RS flow. Caller passes in values
     * specific to its test that should be validated in the resulting subject.
     *
     * @param serverCfgFile
     *            - name of the server 2 config file to reconfig to
     * @param attrName
     *            - the attribute name that is bad (and will be checked for in
     *            the logged message)
     * @throws Exception
     */
    public void badAttrValueTest(String serverCfgFile, String attrName) throws Exception {

        testAppServer.reconfigServer(buildSPServerName(serverCfgFile), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        List<validationData> expectations = get401ExpectationsForJaxrsGet("Did not get expected message saying a required attribute was missing",
                SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE + ".+\\[" + attrName + "\\]");

        genericSAML(_testName, webClient, testSettings, throughJAXRSGet, expectations);
    }

    /**
     * Updates the base server config file using the provided settings,
     * reconfigures the server to use the updated server config, and runs the
     * generic SAML flow using the provided expectations.
     *
     * @param testRsSamlConfigSettings
     * @param expectations
     * @param settings
     * @throws Exception
     */
    public void generalConfigTest(RSSamlConfigSettings testRsSamlConfigSettings, List<validationData> expectations, SAMLTestSettings settings) throws Exception {

        updateConfigFile(testAppServer, baseSamlServerConfig, testRsSamlConfigSettings, testServerConfigFile);

        testAppServer.reconfigServer(testServerConfigFile, testName.getMethodName(), Constants.NO_EXTRA_MSGS, Constants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        genericSAML(_testName, webClient, settings, throughJAXRSGet, expectations);
    }

    /**
     * Returns 200 status code expectations for all actions except the JAX-RS
     * GET invocation. Sets a 401 status code expectation for the JAX-RS GET
     * invocation and will look for logMessage in the app server's messages.log
     * file, outputting failureMessage if logMessage is not found.
     *
     * @param failureMessage
     *            Message to output to JUnit if logMessage isn't found in the
     *            app server's messages.log
     * @param logMessage
     *            Message to find in the app server's messages.log
     * @return
     * @throws Exception
     */
    protected List<validationData> get401ExpectationsForJaxrsGet(String failureMessage, String logMessage) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_JAXRS_GET);

        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.UNAUTHORIZED_STATUS);

        expectations = addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, failureMessage, logMessage);

        return expectations;
    }

    /**
     * Returns 200 status code expectations for all actions except the JAX-RS
     * GET invocation. Sets a 403 status code expectation for the JAX-RS GET
     * invocation and will look for logMessage in the app server's messages.log
     * file, outputting failureMessage if logMessage is not found.
     *
     * @param failureMessage
     *            Message to output to JUnit if logMessage isn't found in the
     *            app server's messages.log
     * @param logMessage
     *            Message to find in the app server's messages.log
     * @return
     * @throws Exception
     */
    protected List<validationData> get403ExpectationsForJaxrsGet(String failureMessage, String logMessage) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_JAXRS_GET);

        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.FORBIDDEN_STATUS);

        expectations = addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, failureMessage, logMessage);

        return expectations;
    }

    /**
     * Returns 200 status code expectations for all actions. Sets a 200 status
     * code expectation for the JAX-RS GET invocation and will look for
     * logMessage in the app server's messages.log file, outputting
     * failureMessage if logMessage is not found. Also check the errorMsg.jsp
     * output
     *
     * @param failureMessage
     *            Message to output to JUnit if logMessage isn't found in the
     *            app server's messages.log
     * @param logMessage
     *            Message to find in the app server's messages.log
     * @return
     * @throws Exception
     */
    protected List<validationData> getSamlErrorExpectationsForJaxrsGet(String failureMessage, String logMessage) throws Exception {
        String action = SAMLConstants.INVOKE_JAXRS_GET;
        List<validationData> expectations = vData.addSuccessStatusCodes(null, action);
        expectations = addMessageExpectation(expectations, action, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, failureMessage, logMessage);

        // once 207838 is fixed, we should be able to add the normal fobidden
        // expectations, plus a check for the
        // server is confused message
        // 207838 List<validationData> expectations =
        // msgUtils.addForbiddenExpectation(actions, null) ;

        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.HTTP_ERROR_FORBIDDEN);
        // expectations = vData.addExpectation(expectations, action,
        // SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not
        // fail to have access.", null, SAMLConstants.HTTP_ERROR_MESSAGE) ;
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.OK_MESSAGE);

        return expectations;
    }

    /**
     * Updates the test settings to invoke the specified app, creates all good
     * expectations, and goes through the full JAX-RS flow.
     *
     * @param appExtension
     * @throws Exception
     */
    protected void successfulJaxRsInvocation(String appExtension) throws Exception {
        SAMLTestSettings updatedTestSettings = commonUtils.changeTestApps(testAppServer, testSettings, appExtension);
        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);
        genericSAML(_testName, SAMLCommonTestHelpers.getWebClient(), updatedTestSettings, throughJAXRSGet, expectations);
    }

    /**
     * Updates the "sp1" partner to the partner specified and the target app
     * eventually invoked to use the specified extension.
     *
     * @param partner
     * @param appExtension
     * @return
     * @throws Exception
     */
    protected SAMLTestSettings setEncryptedTestSettings(String partner, String appExtension) throws Exception {
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        updatedTestSettings.updatePartnerInSettings("sp1", partner, true);
        updatedTestSettings = commonUtils.changeTestApps(testAppServer, updatedTestSettings, appExtension);
        updatedTestSettings.setSpecificIDPChallenge(1);
        updatedTestSettings.setIsEncryptedAssertion(true);
        updatedTestSettings.getSamlTokenValidationData().setEncryptionKeyUser(DEFAULT_ENCRYPTION_KEY_USER);

        return updatedTestSettings;
    }

    /**
     * Invokes the specified app using a normal partner (doesn't encrypt
     * assertions) expecting to get to the protected app, then updates the test
     * settings to use a partner that encrypts assertions and runs through
     * another successful flow.
     *
     * @param appExtension
     * @throws Exception
     */
    protected void successfulKeyStoreTest(String appExtension) throws Exception {
        successfulKeyStoreTest(appExtension, null);
    }

    /**
     * Invokes the specified app using a normal partner (doesn't encrypt
     * assertions) expecting to get to the protected app, then updates the test
     * settings to use a partner that encrypts assertions and runs through
     * another successful flow.
     *
     * @param appExtension
     * @param errorMsgs
     * @throws Exception
     */
    protected void successfulKeyStoreTest(String appExtension, Map<String, String> errorMsgs) throws Exception {
        // Go through flow using partner that doesn't encrypt assertions;
        // keyStoreRef value should have no effect
        successfulJaxRsInvocation(appExtension);

        // Update test settings to use a partner that encrypts SAML assertions
        SAMLTestSettings updatedTestSettings = setEncryptedTestSettings(SP_ENCRYPTION_AES_128, appExtension);

        List<validationData> expectations = getExpectationsForEncryptionTest(updatedTestSettings);
        // Set message expectations (if provided)
        if (errorMsgs != null && !errorMsgs.isEmpty()) {
            for (String logMessage : errorMsgs.keySet()) {
                String failureMessage = errorMsgs.get(logMessage);
                expectations = addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, failureMessage, logMessage);
            }
        }

        genericSAML(_testName, SAMLCommonTestHelpers.getWebClient(), updatedTestSettings, throughJAXRSGet, expectations);
    }

    /**
     * Invokes the specified app using a normal partner (doesn't encrypt
     * assertions) expecting to get to the protected app, then updates the test
     * settings to use a partner that encrypts assertions and runs through the
     * flow again. The flow with the partner that encrypts assertions expects a
     * 401, plus the error messages provided.
     *
     * @param appExtension
     * @param errorMsgs
     *            Map of: Message to search for in RS server log -> Message to
     *            log upon failure
     * @throws Exception
     */
    protected void runKeyStoreTestExpecting401(String appExtension, Map<String, String> errorMsgs) throws Exception {
        // Go through flow using partner that doesn't encrypt assertions;
        // keyStoreRef value should have no effect
        successfulJaxRsInvocation(appExtension);

        // Update test settings to use a partner that encrypts SAML assertions
        SAMLTestSettings updatedTestSettings = setEncryptedTestSettings(SP_ENCRYPTION_AES_128, appExtension);

        // Set 401 expectation
        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_JAXRS_GET);
        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.UNAUTHORIZED_STATUS);

        // Set message expectations (if provided)
        if (errorMsgs != null && !errorMsgs.isEmpty()) {
            // TODO - Can we improve/replace the internal error message?
            for (String logMessage : errorMsgs.keySet()) {
                String failureMessage = errorMsgs.get(logMessage);
                expectations = addMessageExpectation(expectations, SAMLConstants.INVOKE_JAXRS_GET, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, failureMessage, logMessage);
            }
        }
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN_ENCRYPTED, SAMLConstants.STRING_CONTAINS, "Did not receive the expected encrypted SAML token content.", null, null);

        genericSAML(_testName, SAMLCommonTestHelpers.getWebClient(), updatedTestSettings, throughJAXRSGet, expectations);
    }

}
