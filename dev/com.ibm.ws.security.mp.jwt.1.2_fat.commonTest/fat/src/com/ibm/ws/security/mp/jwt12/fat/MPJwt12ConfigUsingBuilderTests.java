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
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt12MPConfigTests;

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

    @Server("com.ibm.ws.security.mp.jwt.1.2.fat")
    public static LibertyServer resourceServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    String[] rsAlgList = { MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.SIGALG_RS384, MPJwt12FatConstants.SIGALG_RS512 };
    private static final boolean ExpectExtraMsgs = true;

    public static final JwtTokenBuilderUtils builderHelpers = new JwtTokenBuilderUtils();

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

        setUpAndStartRSServerForApiTests(resourceServer, jwtBuilderServer, "rs_server_orig_1_2.xml", false);

        skipRestoreServerTracker.addServer(resourceServer);

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
        // by default the test tooling puts the token in Authorization header using "Bearer"
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER, null);

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
        // put the token in the authorization header, but don't use "Bearer"
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.AUTHORIZATION, "notBearer", setMissingTokenBadNameExpectations(resourceServer));
    }

    // Testing using Header=Authorization and not passing the token is the same as other tests...

    /**
     * Tests header set to Authorization. Pass the token as a cookie using the name "Bearer".
     * The request should fail. Even though we use the default name for the cookie, the header setting is
     * telling the runtime to look for the token in the authorization header.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Authorization_passTokenAsCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Authorization.xml");
        // pass the token as a cookie using "Bearer"
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.COOKIE, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
                          setMissingTokenExpectations(resourceServer));
    }

    /**
     * Tests header set to Authorization. cookie with non-default value is specified in the server.xml. Pass the token as a cookie using the configured name.
     * The request should fail. Even though we configured the cookie name and set it to myJwt and then pass a cookie with name myJwt, the header
     * setting is telling the runtime to look for the token in the authorization header.
     * THis shows that we won't allow someone to configure the token name in the header
     *
     * @throws Exception
     *
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Authorization_setCookieName_passTokenAsCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Authorization_withCookie.xml");
        // pass the token as a cookie, but don't use "Bearer"
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.COOKIE, "myJwt", setMissingTokenExpectations(resourceServer));
    }

    /**
     * Tests header set to Cookie. Do not configure the cookie (name). Pass the token as a cookie using the default name of "Bearer".
     * The request should succeed.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Cookie_doNotSetCookieName_passTokenAsCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Cookie.xml");
        // pass the token as a cookie using the default name "Bearer"
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.COOKIE, MPJwt12FatConstants.TOKEN_TYPE_BEARER, null);
    }

    /**
     * Tests header set to Cookie. Configure the cookie (use a name other than the default). Pass the token as a cookie using the configured name.
     * The request should succeed.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Cookie_setCookieName_passTokenAsCookie() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Cookie_withCookie.xml");
        // pass the token as a cookie using the configured cookie name
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.COOKIE, "myJwt", null);
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
        // pass the token as a cookie using a name other than the configured name
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.COOKIE, "otherName", setMissingTokenExpectations(resourceServer));
    }

    /**
     * Tests header set to Cookie. Configure the cookie (use a name other than the default). Pass the token as a cookie using the default name "Bearer".
     * The request should fail. The runtime can't find the cookie
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Cookie_setCookieName_passTokenAsCookieUsingDefaultName() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Cookie_withCookie.xml");
        // put the token in the Cookie - use the cookie name "Bearer" instead of the configured name
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.COOKIE, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
                          setMissingTokenExpectations(resourceServer));
    }

    // pass both - one with header=Auth, one with header=cookie
    /**
     * Tests header set to Authorization. Configure the cookie name (use a name other than the default). Pass a valid token in the Auth Header using "Bearer" and pass a bad value
     * via a cookie using the configured cookie name.
     * The request should succeed as it only looks in the header for the value.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Authorization_passTokenInAuthHeaderAndCookie_HeaderGood() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Authorization_withCookie.xml");
        // put the token in the authorization header using "Bearer"
        // put some string in a cookie with the configured cookie name
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, BothHeaderGood, "myJwt", null);
    }

    /**
     * Tests header set to Cookie. Configure the cookie (use a name other than the default). Pass a bad value in the Auth Header using "Bearer" and pass a good token via a cookie
     * using the configured cookie name.
     * The request should succeed as it only looks for the cookie for the value.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Cookie_passTokenInAuthHeaderAndCookie_CookieGood() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Cookie_withCookie.xml");
        // put some string in the authorization header using "Bearer"
        // put the token in the cookie using the configured name
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, BothCookieGood, "myJwt", null);
    }

    /**
     * Tests header set to Authorization. Configure the cookie name (use a name other than the default). Pass a bad value in the Auth Header using "Bearer" and pass a good token
     * via a cookie using the configured cookie name.
     * The request should fail as it only looks in the header for the value.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Authorization_passTokenInAuthHeaderAndCookie_HeaderBad() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Authorization_withCookie.xml");
        // put some string in the authorization header using "Bearer"
        // put the token in the cookie using the configured name
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, BothCookieGood, "myJwt", setMissingTokenBadNameExpectations(resourceServer));
    }

    /**
     * Tests header set to Cookie. Configure the cookie name (use a name other than the default). Pass a valid token in the Auth Header using "Bearer" and pass a bad value via a
     * cookie using the configured cookie name.
     * The request should fail as it only looks for the cookie for the value.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_Header_Cookie_passTokenInAuthHeaderAndCookie_CookieBad() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_Header_Cookie_withCookie.xml");
        // put the token in the authorization header using "Bearer"
        // put some string in a cookie with the configured cookie name
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, BothHeaderGood, "myJwt", setMissingTokenBadNameExpectations(resourceServer));
    }

    /****************************** End Header & Cookie **********************************/

    /******************************** Start audience ***************************************/
    /**
     * 1.2 is adding audience support in the mp config - we already have it in our server.xml
     * and it is being tested by the 1.1 tests - don't need to test that the runtime behaves
     * properly with an audience value - other tests will validate that we can pick up
     * the audience value from the mp config. Yet other tests will validate that if
     * multiple values are specified from multiple locations, we pick up the correct instance
     */
    /********************************* End audience ****************************************/

    /******************************** Start publickey.algorithm ***************************************/
    /**
     * 1.2 is adding publickkey.algorithm support in the mp config - we already have it in our server.xml
     * and it is being tested by the 1.1 tests - don't need to test that the runtime behaves
     * properly with a publickey.algorithm value - other tests will validate that we can pick up
     * the value from the mp config. Yet other tests will validate that if
     * multiple values are specified from multiple locations, we pick up the correct instance
     */
    /********************************* End publickey.algorithm ****************************************/

    /******************************** Start Encryption ***************************************/

    /**
     * Encrypt token with RS256 (also sign with RS256) - mpJwt config specifies the appropriate RS256 keys for decypt and signing
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS256_tokenRS256() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_RS256.xml");
        genericConfigTest(resourceServer, "sign_RS256_enc_RS256", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER, null);

    }

    /**
     * Encrypt token with RS384 (also sign with RS384) - mpJwt config specifies the appropriate RS384 keys for decypt and signing
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS384_tokenRS384() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS384_encrypt_RS384.xml");
        genericConfigTest(resourceServer, "sign_RS384_enc_RS384", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER, null);
    }

    /**
     * Encrypt token with RS512 (also sign with RS512) - mpJwt config specifies the appropriate RS512 keys for decypt and signing
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS512_tokenRS512() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS512_encrypt_RS512.xml");
        genericConfigTest(resourceServer, "sign_RS512_enc_RS512", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER, null);
    }

    /**
     * Encrypt the token with a RS512 key, try to decrypt with an RS384 key
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS384_tokenRS512() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS384_encrypt_RS384.xml");
        genericConfigTest(resourceServer, "sign_RS512_enc_RS512", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
                          setEncryptMismatchExpectations(resourceServer, ExpectExtraMsgs));
    }

    /**
     * This is another mis-match, but the configured alias uses a type that we don't support (ES can't be used for encryption)
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtES384_tokenRS256() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_ES384.xml");
        genericConfigTest(resourceServer, "sign_RS256_enc_RS256", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
                          setEncryptMismatchKeyTypeExpectations(resourceServer, ExpectExtraMsgs));
    }

    /**
     * show that encryption and signing are separate - show that we can use different algorithms (keys/certs) for
     * encrypting and signing
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJWtRS256_signUsingRS384() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS384_encrypt_RS256.xml");
        genericConfigTest(resourceServer, "sign_RS384_enc_RS256", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER, null);

    }

    /**
     * Test that we not accept a JWS when the config specifies a keyManagementAlias (basically, the config accepts/expects JWE)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS256_tokenNotEncrypted() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_RS256.xml");
        genericConfigTest(resourceServer, MPJwt12FatConstants.SIGALG_RS256, MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
                          setEncryptNotJWETokenExpectations(resourceServer, ExpectExtraMsgs));

    }

    /**
     * Test that we do not accept a JWE token when the config does not specify a keyManagementKeyAlias (basically, the config accepts/expects JWS)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtNoEncryption_tokenEncrypted() throws Exception {

        // use the original server config which has no encryption
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_orig_1_2.xml");
        genericConfigTest(resourceServer, "sign_RS256_enc_RS256", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
                          setNoEncryptNotJWSTokenExpectations(resourceServer, ExpectExtraMsgs));

    }

    /**
     * Test that we can accept/process a token built specifying RSA_OAEP_256 for the key management algorithm.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS256_token_RSA_OAEP_256_RS256_publicKey_A256GCM() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_RS256.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, defaultUser));
        // add more args to create a JWE with a different key management algorithm
        String encryptKey = JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256);

        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_KEY_MGMT_ALG, MPJwt12FatConstants.KEY_MGMT_KEY_ALG_256));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_ENCRYPT_KEY, encryptKey));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_CONTENT_ENCRYPT_ALG, MPJwt12FatConstants.DEFAULT_CONTENT_ENCRYPT_ALG));
        String token = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "sign_RS256_enc_RS256", extraClaims);

        for (TestApps app : setTestAppArray(resourceServer)) {
            WebClient webClient = actions.createWebClient();

            Page response = actions.invokeUrlWithBearerToken(_testName, webClient, app.getUrl(), token);

            validationUtils.validateResult(response, setGoodAppExpectations(app.getUrl(), app.getClassName()));

            actions.destroyWebClient(webClient);

        }
    }

    /**
     * Test that we can accept/process a token built specifying A192GCM for the content encryption algorithm.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS256_token_RSA_OAEP_RS256_publicKey_A192GCM() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_RS256.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, defaultUser));
        // add more args to create a JWE with a different content encryption alg
        String encryptKey = JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256);

        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_KEY_MGMT_ALG, MPJwt12FatConstants.DEFAULT_KEY_MGMT_KEY_ALG));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_ENCRYPT_KEY, encryptKey));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_CONTENT_ENCRYPT_ALG, MPJwt12FatConstants.CONTENT_ENCRYPT_ALG_192));
        String token = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "sign_RS256_enc_RS256", extraClaims);

        for (TestApps app : setTestAppArray(resourceServer)) {
            WebClient webClient = actions.createWebClient();

            Page response = actions.invokeUrlWithBearerToken(_testName, webClient, app.getUrl(), token);

            validationUtils.validateResult(response, setGoodAppExpectations(app.getUrl(), app.getClassName()));

            actions.destroyWebClient(webClient);

        }
    }

    /**
     * Config specifies a keyManagementKeyAlias that does not exist - make sure we don't try to use a "default" key instead.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtinvalid_signUsingRS256() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_invalid.xml");
        genericConfigTest(resourceServer, "sign_RS256_enc_RS256", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
                          setEncryptInvalidKeyTypeExpectations(resourceServer, "badAlias", ExpectExtraMsgs));

    }

    /**
     * Try to use the public key instead of the private key to decrypt
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtpublicKey_signUsingRS256() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_publicKey.xml");
        genericConfigTest(resourceServer, "sign_RS256_enc_RS256", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
                          setEncryptInvalidKeyTypeExpectations(resourceServer, "rs256", ExpectExtraMsgs));

    }

    /**
     * Test that we do not allow use of a key that is too short - the minimum we allow is 2048. Try to use a key that is only 1024 in length
     * ( the check on the key length occurs before we try to use the key to decrypt (which would in this case fail too)
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtShortPrivateKey_signUsingRS256() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_shortPrivateKey.xml");
        genericConfigTest(resourceServer, "sign_RS256_enc_RS256", MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
                          setEncryptShortKeyTypeExpectations(resourceServer, ExpectExtraMsgs));

    }

    /**
     * Test that we do not allow a JWE that only contains Json in the payload
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS256_simpleJsonPayload() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_RS256.xml");

        // build a jwt token whose payload only contains json data - make sure that we do not allow this format (it's not supported at this time)
        String jwtToken = builderHelpers
                        .buildAlternatePayloadJWEToken(JwtKeyTools
                                        .getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256)));
        Log.info(thisClass, _testName, "Funky token: " + jwtToken);
        for (TestApps app : setTestAppArray(resourceServer)) {
            WebClient webClient = actions.createWebClient();

            Page response = actions.invokeUrlWithBearerToken(_testName, webClient, app.getUrl(), jwtToken);

            validationUtils.validateResult(response, setEncryptInvalidPayloadExpectations(resourceServer, ExpectExtraMsgs));

            actions.destroyWebClient(webClient);

        }
    }

    /**
     * Test that we do not allow a JWE that only contains Json in the payload
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS256_JWETypeNotJose() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_RS256.xml");

        // build a jwe token that has "typ" set to "notJOSE" instead of "JOSE".  The token will be encrypted with RS256 and signed with HS256.
        String jwtToken = builderHelpers
                        .buildJWETokenWithAltHeader(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256)),
                                                    "notJOSE", "jwt");
// for debug       new JwtTokenForTest(jwtToken, JwtKeyTools.getComplexPrivateKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256));
        Log.info(thisClass, _testName, "Funky token: " + jwtToken);
        for (TestApps app : setTestAppArray(resourceServer)) {
            WebClient webClient = actions.createWebClient();

            Page response = actions.invokeUrlWithBearerToken(_testName, webClient, app.getUrl(), jwtToken);

            // The test code generates a token that is encrypted with RS256, but signed using HS256
            // the code that checks the JWE type runs before the signature is checked, so if we get far enough to fail on
            // on the signature, we haven't failed checking the JWE Type :)
            validationUtils.validateResult(response, setBadCertExpectations(resourceServer, KeyMismatch));

            actions.destroyWebClient(webClient);

        }
    }

    /**
     * Test that we do not allow a JWE that only contains Json in the payload
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12ConfigUsingBuilderTests_encrypt_mpJwtRS256_JWEContentTypeNotjwt() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_sigAlg_RS256_encrypt_RS256.xml");

        // build a jwe token that has "cty" set to "not_jwt" instead of "jwt".  The token will be encrypted with RS256 and signed with HS256.
        String jwtToken = builderHelpers
                        .buildJWETokenWithAltHeader(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256)),
                                                    "JOSE", "not_jwt");
        Log.info(thisClass, _testName, "Funky token: " + jwtToken);
        for (TestApps app : setTestAppArray(resourceServer)) {
            WebClient webClient = actions.createWebClient();

            Page response = actions.invokeUrlWithBearerToken(_testName, webClient, app.getUrl(), jwtToken);

            validationUtils.validateResult(response, setEncryptBadCtyExpectations(resourceServer, ExpectExtraMsgs));

            actions.destroyWebClient(webClient);

        }
    }

    /******************************** End Encryption ***************************************/

}
