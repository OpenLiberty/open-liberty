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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
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

@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
public class MPJwt12ConfigUsingBuilderTests extends MPJwt12MPConfigTests {

    protected static Class<?> thisClass = MPJwt12ConfigUsingBuilderTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.2.fat")
    public static LibertyServer resourceServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    String[] rsAlgList = { MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.SIGALG_RS384, MpJwt12FatConstants.SIGALG_RS512 };

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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER, null);

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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.AUTHORIZATION, "notBearer", setMissingTokenBadNameExpectations(resourceServer));
    }

    // Testing using Header=Authorization and not passing the token is the same as other tests...

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
        // pass the token as a cookie using "Bearer"
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.COOKIE, MpJwt12FatConstants.TOKEN_TYPE_BEARER,
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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.COOKIE, "myJwt", setMissingTokenExpectations(resourceServer));
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
        // pass the token as a cookie using the default name "Bearer"
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.COOKIE, MpJwt12FatConstants.TOKEN_TYPE_BEARER, null);
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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.COOKIE, "myJwt", null);
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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.COOKIE, "otherName", setMissingTokenExpectations(resourceServer));
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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.COOKIE, MpJwt12FatConstants.TOKEN_TYPE_BEARER,
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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, BothHeaderGood, "myJwt", null);
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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, BothCookieGood, "myJwt", null);
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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, BothCookieGood, "myJwt", setMissingTokenBadNameExpectations(resourceServer));
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
        genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, BothHeaderGood, "myJwt", setMissingTokenBadNameExpectations(resourceServer));
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
                genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER, null);
            } else {
                genericConfigTest(resourceServer, MpJwt12FatConstants.SIGALG_RS256, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER, badExpectations);
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
