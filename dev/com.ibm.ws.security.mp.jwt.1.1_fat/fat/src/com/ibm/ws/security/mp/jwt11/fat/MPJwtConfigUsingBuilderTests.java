/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants.StringCheckType;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.HeaderConstants;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.jwt.expectations.JwtTokenHeaderExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.utils.CommonMpJwtFat;
import com.ibm.ws.security.mp.jwt11.fat.utils.MpJwtMessageConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run test for mpJwt config attributes.
 * <OL>
 * <LI>Setup
 * <OL>
 * <LI>a builder server is started and runs an app that will use the jwt builder to generate jwt tokens
 * The server config contains multiple builder configs to allow generation of tokens with different content.
 * <LI>a resource server will be started with a generic mpJwt config
 * </OL>
 * <LI>All of the tests follow the same "flow".
 * <OL>
 * <LI>the resource server will be re-configured to suit the needs of the test case
 * <LI>any extra/unique claims (IF NEEDED) will be created
 * <LI>a token will be created using the builder app (passing the extra/unique claims if they exist - for inclusion
 * in the token)
 * <LI>if test has set up a negative condition, expectations specific to the test will be created
 * <LI>test will invoke genericConfigTest to:
 * <OL>
 * <LI>initialize some jwt token processing tooling and log the contents of the JWT Token (in a human readable format)
 * <LI>if expectations were not passed in, generate expectations to validate output from the test apps
 * (validates we got the correct app, and that the runtime processed/sees the correct token content)
 * <LI>Loop through 3 different test apps (each using injection in a different way)
 * <OL>
 * <LI>Invoke the app
 * <LI>Validate the response/log contents against the expectations
 * </OL>
 * </OL>
 * </OL>
 * </OL>
 *
 **/

@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
public class MPJwtConfigUsingBuilderTests extends CommonMpJwtFat {

    protected static Class<?> thisClass = MPJwtBasicTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat.builder")
    public static LibertyServer jwtBuilderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private final TestValidationUtils validationUtils = new TestValidationUtils();

    /**
     * Startup the builder and resource servers
     * Set flag to tell the code that runs between tests NOT to restore the server config between tests
     * (very few of the tests use the config that the server starts with - this setting will save run time)
     * Build the list of app urls and class names that each test will use
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml", false);

        setUpAndStartRSServerForTests(resourceServer, "rs_server_orig_withAudience.xml", false);

    }

    /**
     * Don't restore between tests
     * Almost all of the tests in this class will reconfigure the server (we're testing mpJwt config and
     * only 1 may exist in a server at a time, so, we need to reconfigure for each test)
     * A few tests do use the configuration that the server starts with - they're do a normal config restore.
     * All other will skip the restore by overriding the restoreTestServers method
     */
    @Override
    public void restoreTestServers() {
        Log.info(thisClass, "restoreTestServersWithCheck", "* Skipping server restore **");
        logTestCaseInServerLogs("** Skipping server restore **");
    }

    /**
     * Gets the resource server up and running.
     * Sets properties in bootstrap.properties that will affect server behavior
     * Sets up and installs the test apps
     * Adds the server to the serverTracker (used for server restore and test class shutdown)
     * Starts the server using the provided configuration file
     * Saves the port info for this server (allows tests with multiple servers to know what ports each server uses)
     * Allow some failure messages that occur during startup (they're ok and doing this prevents the test framework from failing)
     *
     * @param server
     *            - the server to process
     * @param configFile
     *            - the config file to start the server with
     * @param jwkEnabled
     *            - do we want jwk enabled (sets properties in bootstrap.properties that the configs will use)
     * @throws Exception
     */
    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile, boolean jwkEnabled) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        if (jwkEnabled) {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "\"" + SecurityFatHttpUtils.getServerSecureUrlBase(jwtBuilderServer) + "jwt/ibm/api/defaultJWT/jwk\"");
        } else {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        }
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_authHeaderPrefix", MpJwtFatConstants.TOKEN_TYPE_BEARER + " ");
        deployRSServerApiTestApps(server);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile, commonStartMsgs);
        SecurityFatHttpUtils.saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH));
    }

    /**
     * Initialize the list of test application urls and their associated classNames
     *
     * @throws Exception
     */
    protected List<List<String>> getTestAppArray() throws Exception {

        List<List<String>> testApps = new ArrayList<List<String>>();
        testApps.add(Arrays.asList(buildAppUrl(resourceServer, MpJwtFatConstants.MICROPROFILE_SERVLET, MpJwtFatConstants.MPJWT_APP_SEC_CONTEXT_REQUEST_SCOPE),
                                   MpJwtFatConstants.MPJWT_APP_CLASS_SEC_CONTEXT_REQUEST_SCOPE));
        testApps.add(Arrays.asList(buildAppUrl(resourceServer, MpJwtFatConstants.MICROPROFILE_SERVLET, MpJwtFatConstants.MPJWT_APP_TOKEN_INJECT_REQUEST_SCOPE),
                                   MpJwtFatConstants.MPJWT_APP_CLASS_TOKEN_INJECT_REQUEST_SCOPE));
        testApps.add(Arrays.asList(buildAppUrl(resourceServer, MpJwtFatConstants.MICROPROFILE_SERVLET, MpJwtFatConstants.MPJWT_APP_CLAIM_INJECT_REQUEST_SCOPE),
                                   MpJwtFatConstants.MPJWT_APP_CLASS_CLAIM_INJECT_REQUEST_SCOPE));

        return testApps;

    }

    /**
     * All of the tests in this class follow the same flow. The differences between them are which builder they use to create a
     * token, the config they use in the resource server
     * and then whether they expect a failure (mainly due to a mis-match between the token and the servers config).
     * We'll put the common steps in this method so we're not duplicating steps/code over and over.
     *
     * @param builtToken
     *            - the token built to reflect the goal of the calling test
     * @param expectations
     *            - the expected behavior that we need to validate
     * @throws Exception
     */
    public void genericConfigTest(String builtToken) throws Exception {
        genericConfigTest(builtToken, null);
    }

    public void genericConfigTest(String builtToken, Expectations expectations) throws Exception {

        JwtTokenForTest jwtTokenTools = new JwtTokenForTest(builtToken);

        WebClient webClient = actions.createWebClient();
        // If we're setting good expectations, they have to be unique for each app that we're testing with
        boolean setGoodExpectations = false;
        if (expectations == null) {
            setGoodExpectations = true;
        }
        for (List<String> app : getTestAppArray()) {
            if (setGoodExpectations) {
                expectations = goodTestExpectations(jwtTokenTools, app.get(0), app.get(1));
            }

            Page response = actions.invokeUrlWithBearerToken(_testName, webClient, app.get(0), builtToken);
            validationUtils.validateResult(response, expectations);
        }

    }

    /***************************************************** Tests ****************************************************/

    /******************************** issuer ***************************************/

    /**
     * Default issuer is valid in the default mpJwt config
     *
     * @throws Exception
     */
    //    @Mode(TestMode.LITE)
    @Test
    public void MPJwtConfigUsingBuilderTests_Issuer_Valid() throws Exception {

        // restore the server just in case a previous test changed the config (forcing a restore in tests that do NOT reconfig is quicker than having all tests restore when then finish)
        super.restoreTestServers();

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer);
        genericConfigTest(builtToken);

    }

    /**
     * Test that an unrecognized issuer will not be accepted
     * (test uses a mis-match between the builder and the configured mpjwt config to test this)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_Issuer_Invalid() throws Exception {

        // restore the server just in case a previous test changed the config (forcing a restore in tests that do NOT reconfig is quicker than having all tests restore when then finish)
        super.restoreTestServers();

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "noUniqueIssuer");
        genericConfigTest(builtToken, setBadIssuerExpectations(resourceServer));

    }

    /**
     * Don't specify an issuer - config won't load, all we can do is check for the message
     *
     * @throws Exception
     */
    //    @Test
    public void MPJwtConfigUsingBuilderTests_Issuer_NotSpecifiedInRS() throws Exception {

        //            ArrayList<String>();
        String msg = MpJwtMessageConstants.CWWKG0058E_CONFIG_MISSING_REQUIRED_ATTRIBUTE + ".*" + "issuer";
        resourceServer.addIgnoredErrors(Arrays.asList(msg));

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_noIssuer.xml", msg);

        // Because the required "issuer" attribute is required, the new config will not be loaded.
        // Thus, whichever config was loaded for the previously run test will still be in effect.
        // Since we don't know which config that might be, we have no idea what the behavior might be if we try to invoke the protected app.
    }

    /******************************** authFilterRef ***************************************/

    /**
     * Test that mpJwt is used for an app that matches the authFilter that the mpJwt config specifies.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtConfigUsingBuilderTests_authFilter() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_authFilter_true.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer);
        genericConfigTest(builtToken);

    }

    /**
     * Test that we get the login page for an app that does NOT match the authFilter that the mpJwt config specifies.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_authFilter_false() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_authFilter_false.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(MpJwtFatConstants.STRING_CONTAINS, MpJwtFatConstants.FORM_LOGIN_HEADING, "Did NOT land on the base security form login page"));

        genericConfigTest(builtToken, expectations);

    }

    /******************************** audience ***************************************/

    /**
     * Test that uses a builder that puts the aud claim in the JWT Token. The resource server specifies
     * the audiences config attribute with the same values that are in the token. Expect success
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_Audience_Valid() throws Exception {

        super.restoreTestServers();

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer);
        genericConfigTest(builtToken);

    }

    /**
     * Test that uses a builder that puts the aud claim in the JWT Token. The resource server does not
     * specify the audiences config attribute. Expect a 401 and appropriate error messages in the server side log.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_Audience_NotSpecifiedInRS() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_orig.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6023E_AUDIENCE_NOT_TRUSTED, "Messagelog did not contain an exception indicating that the audience is NOT valid."));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * Test that uses a builder that does not put the aud claim in the JWT Token. The resource server does
     * specify the audiences config attribute. Expect a 401 and appropriate error messages in the server side log.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_Audience_NotSpecifiedInJwt() throws Exception {
        super.restoreTestServers();

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT");

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6023E_AUDIENCE_NOT_TRUSTED, "Messagelog did not contain an exception indicating that the audience is NOT valid."));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * Test that uses a builder that puts the aud claim in the JWT Token. The resource server specifies
     * the audiences config attribute with different values than those that are in the token. Expect a 401
     * and appropriate error messages in the server side log
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException" })
    public void MPJwtConfigUsingBuilderTests_Audience_Mismatch() throws Exception {

        super.restoreTestServers();

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "audience_mismatch");

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6023E_AUDIENCE_NOT_TRUSTED, "Messagelog did not contain an exception indicating that the audience is NOT valid."));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * Test that uses a builder that puts the aud claim in the JWT Token. The resource server specifies
     * the audiences config attribute with a superset of the values that are in the token. Expect success
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_Audience_SuperSet() throws Exception {

        super.restoreTestServers();

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "audience_superset");
        genericConfigTest(builtToken);

    }

    /**
     * Test that uses a builder that puts the aud claim in the JWT Token. The resource server specifies
     * the audiences config attribute with a subset of the values that are in the token. Expect success
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_Audience_Subset() throws Exception {

        super.restoreTestServers();

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "audience_subset");
        genericConfigTest(builtToken);

    }

    /******************************** jwksUri/keyName ***************************************/

    // Testing jwksUri and keyName in tandem
    // The overall SSL config of the builder and mpJwt servers use key/trust that is paired
    // So, we can use a mix of the following using what would be valid values

    /**
     * builder issues token with JWK, mpJwt specifies a valid jwksUri
     * Expect success
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_buildUsingJWK_mpJWTusingJWK() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_jwk.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JWKEnabled");

        genericConfigTest(builtToken);

    }

    /**
     * builder issues token with JWK, mpJwt omits the jwksUri, but, specifies a valid keyName
     * Expect failure
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_buildUsingJWK_mpJWTusingX509() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_noJwk.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JWKEnabled");

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request using the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, "Invalid JWS Signature", "Message log did not contain an exception indicating that the signature was NOT valid."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID, "Message log did not contain an exception indicating that the signature was NOT valid."));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * builder issues token with x509, mpJwt specifies a valid jwksUri
     * Expect expect failure
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException", "com.ibm.websphere.security.jwt.InvalidClaimException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_buildUsingX509_mpJWTusingJWK() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_jwk.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JWKNotEnabled");

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request using the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem creating a JWT."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6029E_SIGNING_KEY_CANNOT_BE_FOUND, "Message log did not contain an error indicating that the signing key could not be found."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Message log did not contain an error indicating that the token could not be processed."));
        genericConfigTest(builtToken, expectations);

    }

    /**
     * builder issues token with x509, mpJwt omits the jwksUri, but, specifies a valid keyName
     * Expect success
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_buildUsingX509_mpJWTusingX509() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_noJwk.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JWKNotEnabled");

        genericConfigTest(builtToken);

    }

    /******************************** jwksUri specific ***************************************/

    /**
     * Tests that uses builder "JWKEnabled2" to generate a JWT Token and has a resource server that
     * specifies the jwksUri that points to JWKEnabled (not JWKEnabled2). Expect a 401 and appropriate error messages.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException",
                   "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_JwksUri_JWTHasJWK_mpJwtMisMatchJWK() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_jwk.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JWKEnabled2");

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request using the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem creating a JWT."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6029E_SIGNING_KEY_CANNOT_BE_FOUND, "Message log did not contain an error indicating that the signing key could not be found."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Message log did not contain an error indicating that the token could not be processed."));
        genericConfigTest(builtToken, expectations);

    }

    /******************************** keyName specific ***************************************/

    /**
     * Test that uses builder that does NOT use JWK to generate a JWT Token and has a resource server that
     * specifies the some other key name. Expect a 401 and appropriate error messages.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.security.cert.CertificateException", "com.ibm.websphere.security.jwt.KeyException" })
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_KeyName_invalidKeyName() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_invalidKeyName.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JWKNotEnabled");

        String invalidKeyName = "someKeyName";
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request using the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Message log did not contain an error indicating that the consumer can not process the string."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6007E_BAD_KEY_ALIAS + ".*"
                                                                                 + invalidKeyName, "Message log did not indicate that the signing key is NOT available."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED + ".*"
                                                                                 + invalidKeyName
                                                                                 + ".*rsa_trust", "Message log did not indicate that the signing key is NOT available."));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * exercise the hs256 config attribute, confirm it works.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtConfigUsingBuilderTests_hs256() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_audience_hs256.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "hs256");
        // make sur we issued the correct token
        Expectations builderExpectations = new Expectations();
        builderExpectations.addExpectation(new JwtTokenHeaderExpectation(HeaderConstants.ALGORITHM, StringCheckType.CONTAINS, MpJwtFatConstants.SIGALG_HS256));
        validationUtils.validateResult(builtToken, builderExpectations);

        genericConfigTest(builtToken);

    }

    /**
     * exercise the hs256 config attribute, sharedKey doesn't match
     * between builder and consumer, 401 should occur.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    public void MPJwtConfigUsingBuilderTests_hs256_mismatchSharedKey() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_audience_hs256.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "hs256_keyMisMatch");
        // make sur we issued the correct token
        Expectations builderExpectations = new Expectations();
        builderExpectations.addExpectation(new JwtTokenHeaderExpectation(HeaderConstants.ALGORITHM, StringCheckType.CONTAINS, MpJwtFatConstants.SIGALG_HS256));
        validationUtils.validateResult(builtToken, builderExpectations);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request using the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem creating a JWT."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Message log did not contain an error indicating that the consumer can not process the string."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID, "Message log did not contain an exception indicating that the signature was NOT valid."));

        genericConfigTest(builtToken, expectations);

    }

    /******************************** sslRef ***************************************/

    /**
     * Test that uses builder that does NOT use JWK to generate a JWT Token and has a resource server that
     * specifies the a valid SSLRef. Expect success.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_SSLRef_Valid_usingX509() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_validSSLRef.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JWKNotEnabled");

        genericConfigTest(builtToken);

    }

    /**
     * Test that uses builder that does NOT use JWK to generate a JWT Token and has a resource server that
     * specifies the an in-valid SSLRef. Expect a 401 and the appropriate error messages.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException", "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.KeyException", "java.security.cert.CertificateException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_SSLRef_invalid_usingX509() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_inValidSSLRef.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JWKNotEnabled");

        String invalidKeyName = "rsacert";
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN
                                                                                 + ".*mpJwt_1", "Message log did not indicate that the consumer can not process the token"));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED + ".*"
                                                                                 + invalidKeyName
                                                                                 + ".*configServerDefault", "Message log did not indicate that the signing key is NOT available."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6007E_BAD_KEY_ALIAS + ".*"
                                                                                 + invalidKeyName, "Message log did not indicate that the signing key is NOT available."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, invalidKeyName
                                                                                 + ".*is not present in the KeyStore as a certificate", "Message log did not a nessage statubg that the alias was NOT found in the keystore."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED + ".*"
                                                                                 + invalidKeyName, "Message log did not indicate that the signing key is NOT available."));

        genericConfigTest(builtToken, expectations);

    }

    /******************************** tokenReuse ***************************************/

    /**
     * The test uses a builder that includes jti in the JWT token. The resource server specifies the
     * tokenReuse config attribute set to true. Use the token a second time and expect success
     *
     * @throws Exception
     */
    //   This is the default case...
    @Test
    public void MPJwtConfigUsingBuilderTests_TokenReuse_True() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_tokenReuse_true.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JTIEnabled");

        genericConfigTest(builtToken);
        // try to use the token again (should succeed)
        genericConfigTest(builtToken);

    }

    /**
     * The test uses a builder that includes jti in the JWT token. The resource server specifies the
     * tokenReuse config attribute set to false. Use the token a second time and expect a 401 and the appropriate error messages
     * (test uses steps similar to genericConfigTest, but doesn't use genericConfigTest because it invokes multiple apps (the
     * first
     * app would work, and 2 & 3 would fail)
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_TokenReuse_False() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_tokenReuse_false.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "JTIEnabled");

        JwtTokenForTest jwtTokenTools = new JwtTokenForTest(builtToken);

        String testUrl = buildAppUrl(resourceServer, MpJwtFatConstants.MICROPROFILE_SERVLET, MpJwtFatConstants.MPJWT_APP_SEC_CONTEXT_REQUEST_SCOPE);
        String className = MpJwtFatConstants.MPJWT_APP_CLASS_SEC_CONTEXT_REQUEST_SCOPE;

        WebClient webClient = actions.createWebClient();

        Expectations expectations = goodTestExpectations(jwtTokenTools, testUrl, className);

        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
        validationUtils.validateResult(response, expectations);

        expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN
                                                                                 + ".*mpJwt_1", "Message log did not indicate that the consumer can not process the token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6045E_JTI_REUSED, "Message log did not indicate that the token has been illegally reused."));

        // Try to use the token again - in the same conversation
        response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
        validationUtils.validateResult(response, expectations);

        // Try to use the token again - this time, in a different conversation
        genericConfigTest(builtToken, expectations);

    }

    /******************************** userNameAttribute ***************************************/

    /**
     * Test that uses the generic builder (requesting that it adds the sub claim) The resource server specifies
     * the userNameAttribute set to sub. Expect success
     *
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_userNameAttribute_Exists_standardClaim() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_userNameAttribute_good.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(PayloadConstants.SUBJECT, defaultUser));
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "subject_claim_included", extraClaims);

        genericConfigTest(builtToken);

    }

    /**
     * Test that uses the generic builder to generate a JWT. The resource server specifies the usernNameAttribute set
     * to "other" "other" is not a claim added by the builder, so, expect a 401 and the appropriate error messages
     * in the server log
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_userNameAttribute_DoesNotExist() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_userNameAttribute_different.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT");

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5519E_PRINCIPAL_MAPPING_MISSING_ATTR + ".*"
                                                                                 + "other", "Message log did not contain an error indicating that the token does not contain the claim specified by userNameAttribute."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5508E_ERROR_CREATING_RESULT
                                                                                 + ".*mpJwt_1", "Message log did not indicate that a subject for the user could not be created."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5506E_USERNAME_NOT_FOUND, "Message log did not indicate that the user name couldn't be found."));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * The test includes a claim called "other" in the token with a value of "someuser".
     * The resource server includes the userNameAttribute set to "other".
     * The test shows that the "other" value of "someuser" is used in the subject/credentials.
     * (side note: mapToUserRegistry is not set in the RS config, therefore, it takes the default value of "false" and
     * the value "someuser" will NOT be checked against the list of users in the registry)
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_userNameAttribute_Exists_uniqueClaim() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_userNameAttribute_different.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("other", "someuser"));
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        // set-up successful expectations here because we want to check for something extra in this case
        // we want to make sure that we get the name someuser from the subject
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(MpJwtFatConstants.STRING_CONTAINS, "com.ibm.wsspi.security.cred.securityName=someuser", "Response did NOT contain \"com.ibm.wsspi.security.cred.securityName=someuser\" to indicate that the user in the credential was \"someuser\" "));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * The test includes a claim called "other" in the token with a value of "someuser".
     * It also includes the upn claim.
     * The resource server includes the userNameAttribute set to "other".
     * The test shows that the "other" value of "someuser" is used in the subject/credentials (not the value in upn)
     * (side note: mapToUserRegistry is not set in the RS config, therefore, it takes the default value of "false" and
     * the value "someuser" will NOT be checked against the list of users in the registry)
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_userNameAttribute_Exists_WithUPNClaim() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_userNameAttribute_different.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("other", "someuser"));
        extraClaims.add(new NameValuePair("upn", defaultUser));
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(MpJwtFatConstants.STRING_CONTAINS, "com.ibm.wsspi.security.cred.securityName=someuser", "Response did NOT contain \"com.ibm.wsspi.security.cred.securityName=someuser\" to indicate that the user in the credential was \"someuser\" "));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * The test includes upn claim, but does NOT contain the claim "other".
     * The resource server includes the userNameAttribute set to "other".
     * Expect a 401 and the appropriate error messages in the server log
     * (claim other doesn't exist, runtime won't try to use upn value)
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_userNameAttribute_DoesNotExist_WithUPNClaimAlso() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_userNameAttribute_different.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", defaultUser));
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5519E_PRINCIPAL_MAPPING_MISSING_ATTR + ".*"
                                                                                 + "other", "Message log did not contain an error indicating that the token does not contain the claim specified by userNameAttribute."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5508E_ERROR_CREATING_RESULT
                                                                                 + ".*mpJwt_1", "Message log did not indicate that a subject for the user could not be created."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5506E_USERNAME_NOT_FOUND, "Message log did not indicate that the user name couldn't be found."));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * The test includes a claim called "other" in the token with a value of "someuser".
     * The resource server includes the userNameAttribute set to "other".
     * The resource server also include mapToUserRegistry=true. The user "someuser" is NOT
     * a valid entry in the registry. Expect a 401 and the appropriate error messages in the server log
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.EntryNotFoundException" })
    @AllowedFFDC({ "com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_userNameAttribute_UserNotInRegistry_WithMapToUserRegistryTrue() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_userNameAttribute_mapToUserRegistryTrue.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("other", "someuser"));
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        //TODO update once 5280 is fixed
        // log contains the error message about not finding the user in the registry
        // response is the basic login page
        Expectations expectations = new Expectations();
        // TODO - fix in the works to not proceed to the login page and return a 401 (issue 5280)
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        //        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS1106A_AUTHENTICATION_FAILED, "Message log did not contain an error indicating a problem authenticating the request the provided token."));
        // TODO - remove check for login page when issue 5280 is fixed
        expectations.addExpectation(new ResponseFullExpectation(MpJwtFatConstants.STRING_CONTAINS, MpJwtFatConstants.FORM_LOGIN_HEADING, "Did NOT land on the base security form login page"));

        genericConfigTest(builtToken, expectations);

//        Expectations expectations = new Expectations();
//        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
//        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5519E_PRINCIPAL_MAPPING_MISSING_ATTR + ".*"
//                                                                                             + "other", "Message log did not contain an error indicating that the token does not contain the claim specified by userNameAttribute."));
//        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5508E_ERROR_CREATING_RESULT
//                                                                                             + ".*mpJwt_1", "Message log did not indicate that a subject for the user could not be created."));
//        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5506E_USERNAME_NOT_FOUND, "Message log did not indicate that the user name couldn't be found."));
//
//        genericConfigTest(builtToken, expectations);

    }

    /******************************** clockSkew ***************************************/

    /**
     * The test uses a builder that includes iat (issuedAtTime) set to the current time, exp (expiration time) set to 5 seconds in
     * the future.
     * The resource server specifies a clockSkew of 3 minutes. The test creates a token, and sleeps for 15 seconds. The expiration
     * time has passed, but new current time is not beyond expiration time plus clockSkew. Expect success
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_clockSkew_useTokenWithinClockSkew() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_clockSkew_normal.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", defaultUser));
        long currentTime = System.currentTimeMillis() / 1000;
        long expTime = currentTime + 5;
        extraClaims.add(new NameValuePair(PayloadConstants.ISSUED_AT, String.valueOf(currentTime)));
        extraClaims.add(new NameValuePair(PayloadConstants.EXPIRATION_TIME, String.valueOf(expTime)));

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "subject_claim_included", extraClaims);

        // Sleep beyond the lifetime of the token but within the configured clockSkew
        Thread.sleep(15 * 1000);
        genericConfigTest(builtToken);

    }

    /**
     * The test uses a builder that includes iat (issuedAtTime) set to the current time, exp (expiration time) set to 5 seconds in
     * the future.
     * The resource server specifies a clockSkew of 5 seconds. The test creates a token, and sleeps for 20 seconds. The expiration
     * time has passed, and the new current time is beyond expiration time plus clockSkew. Expect a 401 and the appropriate error
     * messages in the server log
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_clockSkew_useTokenOutsideClockSkew() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_clockSkew_short.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", defaultUser));
        long currentTime = System.currentTimeMillis() / 1000;
        long expTime = currentTime + 5;
        extraClaims.add(new NameValuePair(PayloadConstants.ISSUED_AT, String.valueOf(currentTime)));
        extraClaims.add(new NameValuePair(PayloadConstants.EXPIRATION_TIME, String.valueOf(expTime)));

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "subject_claim_included", extraClaims);

        // Sleep beyond the lifetime of the token plus the configured clockSkew
        Thread.sleep(20 * 1000);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN + ".*"
                                                                                 + "consumer.*mpJwt_1", "Message log did not contain an exception indicating that the JWT could not be processed."));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS6025E_TOKEN_EXPIRED, "Message log did not contain message saying the token has expired."));

        genericConfigTest(builtToken, expectations);

    }

    /*********************** ignoreApplicationAuthMethod ******************************/
    /* tests are performed by the test classes: */
    /* MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests.java */
    /* MPJwtLoginConfig_ignoreApplicationAuthMethodFalseTests.java */
    /**********************************************************************************/

    /**************************** mapToUserRegistry ***********************************/
    /**
     * The test uses a builder that creates a token with the upn set to a user in the resource servers
     * registry.
     * The resource server specifies a mapToUserRegistry=false. Do not specify userNameAttribute. Expect success
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_mapToUserRegistryFalse() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_mapToUserRegistry_false.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT");

        genericConfigTest(builtToken);

    }

    /**
     * The test uses a builder that creates a token with the upn set to a user in the resource servers
     * registry.
     * The resource server specifies a mapToUserRegistry=true. Do not specify userNameAttribute. Expect success
     *
     * @throws Exception
     */
    @Test
    public void MPJwtConfigUsingBuilderTests_mapToUserRegistryTrue_userInRegistry() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_mapToUserRegistry_true.xml");

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT");

        genericConfigTest(builtToken);

    }

    /**
     * The test uses a builder that creates a token with the upn set to a user that is NOT in the resource servers
     * registry.
     * The resource server specifies a mapToUserRegistry=true. Do not specify userNameAttribute. Expect 401 and
     * the appropriate error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.EntryNotFoundException" })
    @Test
    public void MPJwtConfigUsingBuilderTests_mapToUserRegistryTrue_userNotInRegistry() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_mapToUserRegistry_true.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", "userNotThere"));

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        // log contains the error message about not finding the user in the registry
        // response is the basic login page
        Expectations expectations = new Expectations();
        // TODO - fix in the works to not proceed to the login page and return a 401 (issue 5280)
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        //        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(resourceServer, MpJwtMessageConstants.CWWKS1106A_AUTHENTICATION_FAILED, "Message log did not contain an error indicating a problem authenticating the request the provided token."));
        // TODO - remove check for login page when issue 5280 is fixed
        expectations.addExpectation(new ResponseFullExpectation(MpJwtFatConstants.STRING_CONTAINS, MpJwtFatConstants.FORM_LOGIN_HEADING, "Did NOT land on the base security form login page"));

        genericConfigTest(builtToken, expectations);

    }
    // test name "MPJwtConfigUsingBuilderTests_userNameAttribute_UserNotInRegistry_WithMapToUserRegistryTrue" will verify that using userNameAttribute and mapToUserRegistry=true will behave in the same way.
}
