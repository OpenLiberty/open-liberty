/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt12.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.utils.MpJwtMessageConstants;
import com.ibm.ws.security.mp.jwt12.fat.sharedTests.MPJwt12MPConfigTests;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run test for mpJwt 1.2 config attributes.
 * <OL>
 * <LI>Setup
 * <OL>
 * <LI>a builder server is started and runs an app that will use the jwt builder to generate jwt tokens
 * The server config contains multiple builder configs to allow generation of tokens with different content.
 * <LI>a resource server will be started with a generic mpJwt 1.2 config
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

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
public class MPJwt12ConfigUsingBuilderTests extends MPJwt12MPConfigTests {

    protected static Class<?> thisClass = MPJwt12ConfigUsingBuilderTests.class;

    @Server("com.ibm.ws.security.mp.jwt.fat")
    public static LibertyServer resourceServer;

//    @Server("com.ibm.ws.security.mp.jwt.1.2.fat.builder")
//    public static LibertyServer jwtBuilderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private final TestValidationUtils validationUtils = new TestValidationUtils();

    String[] rsAlgList = { MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.SIGALG_RS384, MpJwt12FatConstants.SIGALG_RS512 };

//    private static final String BothHeaderGood = "HeaderGood";
//    private static final String BothCookieGood = "CookieGood";

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

        setUpAndStartRSServerForTests(resourceServer, "rs_server_orig_1_2.xml", false);

        //TODO remove next line when additional signature algorithms added
        jwtBuilderServer.addIgnoredErrors(Arrays.asList("CWWKG0032W"));

    }

    /**
     * Don't restore between tests
     * Almost all of the tests in this class will reconfigure the server (we're testing mpJwt 1.2 config and
     * only 1 may exist in a server at a time, so, we need to reconfigure for each test)
     * A few tests do use the configuration that the server starts with - they'll do a normal config restore.
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
        // TODO
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
    public void genericConfigTest(String builder) throws Exception {
        genericConfigTest(builder, null);
    }

    public void genericConfigTest() throws Exception {
        genericConfigTest(MpJwt12FatConstants.SIGALG_RS256);
    }

    /**
     * Run a test using the default token location (Bearer in the Authorization Header)
     *
     * @param builtToken - The token built to reflect the goal of the calling test
     * @param expectations - the expected behavior that we need to validate (null if using the standard good expectations)
     * @throws Exception
     */
    public void genericConfigTest(String builder, Expectations expectations) throws Exception {
        genericConfigTest(builder, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER, expectations);

    }

    public void genericConfigTest(Expectations expectations) throws Exception {
        genericConfigTest(MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER, expectations);

    }

    public void genericConfigTest(String location, String name, Expectations expectations) throws Exception {
        genericConfigTest(MpJwt12FatConstants.SIGALG_RS256, location, name, expectations);
    }

    public void genericConfigTest(String builder, String location, String name, Expectations expectations) throws Exception {

        String thisMethod = "genericConfigTest";
        loggingUtils.printMethodName(thisMethod);

//        JwtTokenForTest jwtTokenTools = new JwtTokenForTest(builtToken);

//        WebClient webClient = actions.createWebClient();
        // If we're setting good expectations, they have to be unique for each app that we're testing with
//        boolean setGoodExpectations = false;
//        if (expectations == null) {
//            setGoodExpectations = true;
//        }
        for (List<String> app : getTestAppArray()) {
            standardTestFlow(builder, app.get(0), app.get(1), location, name, expectations);

//            if (setGoodExpectations) {
//                expectations = goodTestExpectations(jwtTokenTools, app.get(0), app.get(1));
//            }
//
//            Page response = null;
//            if (MpJwt12FatConstants.AUTHORIZATION.equals(location)) {
//                if (MpJwt12FatConstants.TOKEN_TYPE_BEARER.equals(name)) {
//                    response = actions.invokeUrlWithBearerToken(_testName, webClient, app.get(0), builtToken);
//                } else {
//                    response = invokeUrlWithOtherHeader(webClient, app.get(0), location, name, builtToken);
//                }
//            } else {
//                if (MpJwt12FatConstants.COOKIE.equals(location)) {
//                    response = actions.invokeUrlWithCookie(_testName, app.get(0), name, builtToken);
//                } else {
//                    // if we haven't requested Authorization, or Cookie, we want to test passing both - but, there can
//                    // be 2 varieties of both - 1) header has good value and cookie is bad and 2) header is bad and cookie is good
//                    // Headers and cookies are added as additional headers, so create a map of "headers" and pass that on the request of the url.
//                    if (BothHeaderGood.equals(location)) {
//                        Map<String, String> headers = addToMap(null, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER + " " + builtToken);
//                        headers = addToMap(headers, MpJwt12FatConstants.COOKIE, name + "=badTokenString");
//                        response = actions.invokeUrlWithParametersAndHeaders(_testName, webClient, app.get(0), null, headers);
//                    } else {
//                        if (BothCookieGood.equals(location)) {
//                            Map<String, String> headers = addToMap(null, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER + " " + "badTokenString");
//                            headers = addToMap(headers, MpJwt12FatConstants.COOKIE, name + "=" + builtToken);
//                            response = actions.invokeUrlWithParametersAndHeaders(_testName, webClient, app.get(0), null, headers);
//                        } else {
//                            throw new Exception("Test code does not understand request");
//                        }
//                    }
//                }
//            }
//            validationUtils.validateResult(response, expectations);
        }

    }

    /**
     * Set expectations for tests that are missing the token
     *
     * @param server - server whose logs will be searched
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setMissingTokenExpectations(LibertyServer server) throws Exception {

        Expectations expectations = badAppExpectations(MpJwt12FatConstants.UNAUTHORIZED_MESSAGE);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5522E_MPJWT_TOKEN_NOT_FOUND, "Messagelog did not contain an error indicating that the JWT token was not found."));

        return expectations;

    }

    /**
     * Set expectations for tests that have bad Signature Algorithms
     *
     * @param server - server whose logs will be searched
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setBadEncryptExpectations(LibertyServer server) throws Exception {

        Expectations expectations = badAppExpectations(MpJwt12FatConstants.UNAUTHORIZED_MESSAGE);

        //TODO - correct failure msgs....
//        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
//        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an exception indicating that the Signature Algorithm is NOT valid."));

        return expectations;

    }

    /**
     * Runtime finds the Authorization header, but the name within the token is not recognized
     *
     * @param server - server whose logs will be searched
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setMissingTokenBadNameExpectations(LibertyServer server) throws Exception {

        Expectations expectations = badAppExpectations(MpJwt12FatConstants.UNAUTHORIZED_MESSAGE);

        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Messagelog did not contain an error indicating a problem processing the request."));

        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain and exception indicating that the JWT feature cannot authenticate the request."));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an exception indicating that the Signature Algorithm is NOT valid."));

        return expectations;

    }

    /***************************************************** Tests ****************************************************/

    /****************************** Start Header & Cookie **********************************/
    /**
     * Tests header set to Authorization. Pass the token in the Authorization header using Bearer.
     * The request should succeed.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Authorization_passTokenInAuthHeaderUsingBearer() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Authorization.xml");
//        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256);
        // by default the test tooling puts the token in Authorization header using "Bearer"
        genericConfigTest();

    }

    /**
     * Tests header set to Authorization. Pass the token in the Authorization header using something other than Bearer.
     * The request should fail.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Authorization_passTokenInAuthHeaderNotUsingBearer() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Authorization.xml");
//        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256);
        // put the token in the authorization header, but don't use "Bearer"
        genericConfigTest(MpJwt12FatConstants.AUTHORIZATION, "notBearer", setMissingTokenBadNameExpectations(resourceServer));
    }

    // Testing using Header=Authorization and not passing the token si the same as other tests...

    /**
     * Tests header set to Authorization. Pass the token as a cookie using the name "Bearer".
     * The request should fail. Even though we use the default name for the cookie, the header setting is
     * telling the runtime to look for the token in the authorization header.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Authorization_passTokenAsCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Authorization.xml");
//        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256);
        // put the token in the authorization header, but don't use "Bearer"
        genericConfigTest(MpJwt12FatConstants.COOKIE, MpJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
    }

    /**
     * Tests header set to Authorization. cookie with non-default value is specified in the server.xml. Pass the token as a cookie using the default name.
     * The request should fail. Even though we configured the cookie name and set it to myJwt and then pass a cookie with name myJwt, the header
     * setting is telling the runtime to look for the token in the authorization header.
     *
     * @throws Exception
     *
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Authorization_setCookieName_passTokenAsCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Authorization_withCookie.xml");
//        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256);
        // put the token in the authorization header, but don't use "Bearer"
        genericConfigTest(MpJwt12FatConstants.COOKIE, "myJwt", setMissingTokenExpectations(resourceServer));
    }

    /**
     * Tests header set to Cookie. Do not configure the cookie (name). Pass the token as a cookie using the default name of "Bearer".
     * The request should succeed.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Cookie_doNotSetCookieName_passTokenAsCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Cookie.xml");
//        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256);
        // put the token in the authorization header, but don't use "Bearer"
        genericConfigTest(MpJwt12FatConstants.COOKIE, MpJwt12FatConstants.TOKEN_TYPE_BEARER, null);
    }

    /**
     * Tests header set to Cookie. Configure the cookie (use a name other than the default). Pass the token as a cookie using the configured value.
     * The request should succeed.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Cookie_setCookieName_passTokenAsCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Cookie_withCookie.xml");
//        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256);
        // put the token in the authorization header, but don't use "Bearer"
        genericConfigTest(MpJwt12FatConstants.COOKIE, "myJwt", null);
    }

    /**
     * Tests header set to Cookie. Configure the cookie (use a name other than the default). Pass the token as a cookie using a different name.
     * The request should fail. The runtime can't find the cookie
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Cookie_setCookieName_passTokenAsCookieUsingDifferentName() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Cookie_withCookie.xml");
//        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256);
        // put the token in the authorization header, but don't use "Bearer"
        genericConfigTest(MpJwt12FatConstants.COOKIE, "otherName", setMissingTokenExpectations(resourceServer));
    }

    // pass both - one with header=Auth, one with header=cookie
    /**
     * Tests header set to Cookie. Configure the cookie (use a name other than the default). Pass the token as a cookie using the configured value.
     * The request should succeed.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Authorization_passTokenInAuthHeaderAndCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Authorization_withCookie.xml");
//        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256);
        // put the token in the authorization header, but don't use "Bearer"
        genericConfigTest(BothHeaderGood, "myJwt", null);
    }

    /**
     * Tests header set to Cookie. Configure the cookie (use a name other than the default). Pass the token as a cookie using the configured value.
     * The request should succeed.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Cookie_passTokenInAuthHeaderAndCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Cookie_withCookie.xml");
//        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, MpJwt12FatConstants.SIGALG_RS256);
        // put the token in the authorization header, but don't use "Bearer"
        genericConfigTest(BothCookieGood, "myJwt", null);
    }

    /****************************** End Header & Cookie **********************************/

    /******************************** Start audience ***************************************/
    /**
     * 1.2 added audience support in the mp config - we already have it in our server.xml
     * and it is being tested by the 1.1 tests - don't need to test that the runtime behaves
     * properly with an audience value - other tests will validate that we can pick up
     * the audience value from the mp config. Yet other tests will validate that if
     * multiple values are specified from multiple locations, we pick up the correct instance
     */
    /********************************* End audience ****************************************/

    /******************************** Start xxx (Encrypted token) ***************************************/

    // encrypt the token, omit key from config
    // don't encrypt the token, but do include a key in the config
    // encrypt with each supported and use both matching and non-matching keys (may only use rs256)
    // have tests that use both the sslRef in the mpJwt config and the server wide config
    // sign with one - encrypt with another
    /******************************** End xxx (Encrypted token) ***************************************/
    /**
     * Code to loop through encryption keys of all types and
     * validate behavior (success if they match, failure if they do not)
     *
     * @param privateKey - the private key that'll match the config
     * @throws Exception
     */
    public void genericEncryption(String privateKeyAlg) throws Exception {

        // TODO
        // may need unique expectations for conflicts between types vs conflicts between "size"
        // ie HS256 and RS256 vs RS256 and RS512
        Expectations badExpectations = setBadEncryptExpectations(resourceServer);

        for (String encKeyAlg : rsAlgList) { // RS256, RS384, RS512
            // note the build name must match the name in the list
            // thought about creating the tokens once, but:
            // 1) that makes it harder to reference
            // 2) this puts more stress on our builder...
            Log.info(thisClass, "genericEncryption", "********************************************");
            Log.info(thisClass, "genericEncryption", "* Config: " + privateKeyAlg + "      Token: " + encKeyAlg + "          *");
            Log.info(thisClass, "genericEncryption", "********************************************");

//            String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "enc_" + encKeyAlg);

            if (encKeyAlg.equals(privateKeyAlg)) {
                genericConfigTest();
            } else {
                genericConfigTest(badExpectations);
            }
        }
    }

    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJWTusingRS256() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_encrypt_RS256.xml");
        genericEncryption(MpJwt12FatConstants.ENCRYPT_RS256);

    }

    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJWTusingRS384_tokenRS384() throws Exception {

    }

    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJWTusingRS512_tokenRS512() throws Exception {

    }

    public void MPJwt12ConfigUsingBuilderTests_encrypt_keyRS256_signUsingRS384() throws Exception {

    }

    public void MPJwt12ConfigUsingBuilderTests_encrypt_keyNotEncrypted_signUsingRS256() throws Exception {

    }

    public void MPJwt12ConfigUsingBuilderTests_encrypt_keyRS256_mpJWTMissingKey() throws Exception {

    }
}
