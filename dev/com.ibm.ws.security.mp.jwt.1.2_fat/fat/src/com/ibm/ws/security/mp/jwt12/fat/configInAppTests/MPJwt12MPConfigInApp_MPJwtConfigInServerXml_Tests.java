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
package com.ibm.ws.security.mp.jwt12.fat.configInAppTests;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.mp.jwt12.fat.sharedTests.MPJwt12MPConfigTests;
import com.ibm.ws.security.mp.jwt12.fat.utils.MP12ConfigSettings;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class that will test the placement of mp jwt config settings inside of an app. They can be found in multiple
 * places within the application. The server.xml will have an invalid issuer configured - these tests will show that the
 * configuration specified in the server will override the values in the mp-config in the app.
 * The tests will do one of the following:
 * - request use of app that has placement of config in "resources/META-INF/microprofile-config.properties"
 * - request use of app that has placement of config in "resources/WEB-INF/classes/META-INF/microprofile-config.properties"
 *
 **/

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
public class MPJwt12MPConfigInApp_MPJwtConfigInServerXml_Tests extends MPJwt12MPConfigTests {

    public static Class<?> thisClass = MPJwt12MPConfigInApp_MPJwtConfigInServerXml_Tests.class;

    @Server("com.ibm.ws.security.mp.jwt.fat")
    public static LibertyServer resourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // let's pick a default config - use a pemFile located in the app
        MP12ConfigSettings mpConfigSettings = new MP12ConfigSettings(MP12ConfigSettings.PublicKeyLocationNotSet, MP12ConfigSettings.PublicKeyNotSet, MP12ConfigSettings.IssuerNotSet, MpJwt12FatConstants.X509_CERT, MP12ConfigSettings.Header, MP12ConfigSettings.Cookie, "client01, client02");
        setupBootstrapPropertiesForMPTests(resourceServer, mpConfigSettings);

        deployRSServerMP12ConfigInAppHeaderApps(resourceServer, mpConfigSettings);

        // startup server with no header, or cookie
//        setUpAndStart12RSServerForTests(resourceServer, "rs_server_orig_1_2_withOtherApps.xml", mpConfigSettings, MPConfigLocation.IN_APP);
//        Log.info(thisClass, "setUpAndStartRSServerFortTests", "enteredOverride method");

//        setupMP12Config(resourceServer, mpConfigSettings, mpConfigLocation);

        startRSServerForMPTests(resourceServer, "rs_server_orig_1_2_withOtherApps.xml");

    }

    /******************************************** tests **************************************/

    /*
     * Negative tests that do not show/prove relationship between values from server.xml and mp config properties are skipped
     * in this test class as they're covered by the base config attribute testing.
     *
     * Part of what this means is that we won't test not passing a token when the location/type/name is obvious.
     * We will include "negative" type tests where the value used may not be obvious
     */

    /**
     * Test shows that we'll pick up the header setting from mp config properties
     * The app has the header set to Authorization in META-INF.
     *
     * @throws Exception
     */
    //chc  @Test
    public void MPJwtMPConfigInApp_Variations_NoMPJwtConfigInServerXml_HeaderIsAuthorizationInMPConfig_InMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties
     * The app has the header set to Authorization under WEB-INF.
     *
     * @throws Exception
     */
    //chc   @Test
    public void MPJwtMPConfigInApp_Variations_NoMPJwtConfigInServerXml_HeaderIsAuthorizationInMPConfig_UnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    @Test
    public void MPJwtMPConfigInApp_Variations_NoMPJwtConfigInServerXml_HeaderIsCookieInMPConfig_InMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties
     * The app has the header set to Cookie under WEB-INF.
     *
     * @throws Exception
     */
//chc    @Test
    public void MPJwtMPConfigInApp_Variations_NoMPJwtConfigInServerXml_HeaderIsCookieInMPConfig_UnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties realize is bad, and use the default value of Authorization
     * The app has the header set to Authorization in META-INF.
     *
     * @throws Exception
     */
    //chc   @Test
    public void MPJwtMPConfigInApp_Variations_NoMPJwtConfigInServerXml_HeaderIsBadInMPConfig_InMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP,
                         MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER,
                         setBadHeaderValueExpectations(resourceServer,
                                                       buildAppUrl(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                                   MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP),
                                                       MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF));
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties realize is bad, and use the default value of Authorization
     * The app has the header set to Authorization under WEB-INF.
     *
     * @throws Exception
     */
    //chc   @Test
    public void MPJwtMPConfigInApp_Variations_NoMPJwtConfigInServerXml_HeaderIsBadInMPConfig_UnderWebInf_test() throws Exception {

        // the value in the mp config is bad - we'll still pass the default values in the request
        standardTestFlow(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP,
                         MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER,
                         setBadHeaderValueExpectations(resourceServer,
                                                       buildAppUrl(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                   MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP),
                                                       MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF));
    }

//    /**
//     * Test shows that we'll pick up the header setting from mp config properties
//     * The app has the header set to Cookie in META-INF.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void MPJwtMPConfigInApp_Variations_NoMPJwtConfigInServerXml_HeaderIsCookieInMPConfig_InMetaInf_test() throws Exception {
//
//    }
//
//    /**
//     * Test shows that we'll pick up the header and cookie setting from mp config properties
//     * The app has the header set to Cookie and the cookieName set in META-INF.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void MPJwtMPConfigInApp_Variations_NoMPJwtConfigInServerXml_HeaderIsCookiePlusCookieName_InMPConfigInMetaInf_test() throws Exception {
//
//    }

//
//
//
//    /**
//     * The server.xml has an mp_jwt config specified that includes an invalid issuer. The app has a valid mp-config (in the
//     * META-INF directory of the app).
//     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void MPJwtMPConfigInApp_BadHeaderMPJwtConfigInServerXml_GoodMPConfigInMetaInf_test() throws Exception {
//
//        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
//                         setBadHeaderExpectations(resourceServer));
//
//    }
//
//    /**
//     * The server.xml has an mp_jwt config specified that includes an invalid issuer. The app has a valid mp-config (under the
//     * WEB-INF directory of the app).
//     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void MPJwtMPConfigInApp_BadHeaderMPJwtConfigInServerXml_GoodMPConfigUnderWebInf_test() throws Exception {
//
//        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
//                         setBadHeaderExpectations(resourceServer));
//
//    }
//
//    /**
//     * The server.xml has an mp_jwt config specified that includes an invalid issuer . The app has a valid issuer in mp-config (in
//     * the META-INF directory of the app).
//     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void MPJwtMPConfigInApp_BadHeaderMPJwtConfigInServerXml_GoodHeaderInMPConfigInMetaInf_test() throws Exception {
//
//        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
//                         setBadHeaderExpectations(resourceServer));
//
//    }
//
//    /**
//     * The server.xml has an mp_jwt config specified that has an invalid issuer. The app has a valid issuer in mp-config (under
//     * the WEB-INF directory of the app).
//     * This test shows that the runtime uses the value from the server.xml instead of whats in the mp-config in the app.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void MPJwtMPConfigInApp_BadHeaderMPJwtConfigInServerXml_GoodHeaderInMPConfigUnderWebInf_test() throws Exception {
//
//        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
//                         setBadHeaderExpectations(resourceServer));
//
//    }
//
//    /**
//     * The server.xml has an mp_jwt config specified that has an invalid issuer. The app has an invalid issuer in mp-config (in
//     * the META-INF directory of the app).
//     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void MPJwtMPConfigInApp_BadHeaderMPJwtConfigInServerXml_BadHeaderInMPConfigInMetaInf_test() throws Exception {
//
//        standardTestFlow(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
//                         setBadHeaderExpectations(resourceServer));
//
//    }
//
//    /**
//     * The server.xml has an mp_jwt config specified that has an invalid issuer. The app has an invalid issuer in mp-config (under
//     * the WEB-INF directory of the app).
//     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void MPJwtMPConfigInApp_BadHeaderMPJwtConfigInServerXml_BadHeaderInMPConfigUnderWebInf_test() throws Exception {
//
//        standardTestFlow(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
//                         setBadHeaderExpectations(resourceServer));
//
//    }

}
