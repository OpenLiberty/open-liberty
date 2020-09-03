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

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.mp.jwt12.fat.sharedTests.MPJwt12MPConfigTests;
import com.ibm.ws.security.mp.jwt12.fat.utils.MP12ConfigSettings;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will test the placement of mp jwt config settings inside of an app. They can be found in multiple
 * places within the application. We will test with a server.xml that doens't have the attributes that we're placing in the
 * microprofile-config.properties file as well as server.xml's that do. When the values are also in the server.xml file, we'll
 * show that those values override what's in the microprofile-config.properties file.
 * The tests will do one of the following:
 * - request use of app that has placement of config in "resources/META-INF/microprofile-config.properties"
 * - request use of app that has placement of config in "resources/WEB-INF/classes/META-INF/microprofile-config.properties"
 *
 **/

@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
public class MPJwt12MPConfigInApp_Tests extends MPJwt12MPConfigTests {

    public static Class<?> thisClass = MPJwt12MPConfigInApp_Tests.class;

    @Server("com.ibm.ws.security.mp.jwt.fat")
    public static LibertyServer resourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // let's pick a default mp config - we'll allow most of the settings (mpJwt 1.1 attrs) to come from the server.xml
        MP12ConfigSettings mpConfigSettings = new MP12ConfigSettings(MP12ConfigSettings.PublicKeyLocationNotSet, MP12ConfigSettings.PublicKeyNotSet, MP12ConfigSettings.IssuerNotSet, MpJwt12FatConstants.X509_CERT, MP12ConfigSettings.DefaultHeader, MP12ConfigSettings.DefaultCookieName, "client01, client02");
        setupBootstrapPropertiesForMPTests(resourceServer, mpConfigSettings);

        deployRSServerMPConfigInAppHeaderApps(resourceServer, mpConfigSettings);

        startRSServerForMPTests(resourceServer, "rs_server_orig_1_2_withOtherApps.xml");

    }

    /******************************************** tests **************************************/

    /********************************* Header & Cookie tests *********************************/
    /* Simple negative tests like not passing a token at all are covered under the base config attribute tests */

    /**
     * Test shows that we'll pick up the header setting from mp config properties
     * The app has the header set to Authorization in META-INF.
     * The test case passes the token in the Authorization Header using Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsAuthorizationInMPConfig_InMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties
     * The app has the header set to Authorization under WEB-INF.
     * The test case passes the token in the Authorization Header using Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsAuthorizationInMPConfig_UnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties
     * The app has the header set to Cookie in META-INF. No cookie name is set.
     * The test case passes the token as a Cookie using the name Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieInMPConfig_InMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties
     * The app has the header set to Cookie under WEB-INF. No Cookie name is set.
     * The test case passes the token as a Cookie using the name Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieInMPConfig_UnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header and cookie settings from mp config properties
     * The app has the header set to Cookie in META-INF. The Cookie name is set to the default value of Bearer.
     * The test case passes the token as a Cookie using the name Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieWithCookieNameInMPConfig_InMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header and cookie settings from mp config properties
     * The app has the header set to Cookie under WEB-INF. The Cookie name is set to the default value of Bearer.
     * The test case passes the token as a Cookie using the name Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieWithCookieNameInMPConfig_UnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header and cookie settings from mp config properties
     * The app has the header set to Cookie in META-INF. The Cookie name is set to a value OTHER THAN Bearer.
     * The test case passes the token as a Cookie using the name "OtherCookieName".
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieWithOtherCookieNameInMPConfig_InMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.COOKIE,
                         "OtherCookieName");
    }

    /**
     * Test shows that we'll pick up the header and cookie settings from mp config properties
     * The app has the header set to Cookie under WEB-INF. The Cookie name is set to a value OTHER THAN Bearer.
     * The test case passes the token as a Cookie using the name "OtherCookieName".
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieWithOtherCookieNameInMPConfig_UnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.COOKIE,
                         "OtherCookieName");
    }

    /**
     * Test shows that we'll pick up the header and cookie settings from mp config properties
     * The app has the header set to Cookie in META-INF. The Cookie name is set to a value OTHER THAN Bearer.
     * The test case passes the token as a Cookie using the name "Bearer" - the runtime won't find the token.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieWithOtherCookieNameInMPConfig_InMetaInf_PassCookieWithNameBearer_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
    }

    /**
     * Test shows that we'll pick up the header and cookie settings from mp config properties
     * The app has the header set to Cookie under WEB-INF. The Cookie name is set to a value OTHER THAN Bearer.
     * The test case passes the token as a Cookie using the name "Bearer" - the runtime won't find the token.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieWithOtherCookieNameInMPConfig_UnderWebInf_PassCookieWithNameBearer_test() throws Exception {

        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties detecting that it is bad, and use the default value of Authorization.
     * The app has the header set to "badHeader" in META-INF.
     * The test case passes the token in the Authorization header using Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsBadInMPConfig_InMetaInf_test() throws Exception {

        String urlCalled = buildAppUrl(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP);
        Expectations warningExpectations = setBadHeaderValueExpectations(resourceServer, urlCalled, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

        standardTestFlow(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP,
                         MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER,
                         warningExpectations);
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties detecting that it is bad, and use the default value of Authorization.
     * The app has the header set to "badHeader" under WEB-INF.
     * The test case passes the token in the Authorization header using Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsBadInMPConfig_UnderWebInf_test() throws Exception {

        String urlCalled = buildAppUrl(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP);
        Expectations warningExpectations = setBadHeaderValueExpectations(resourceServer, urlCalled, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

        // the value in the mp config is bad - we'll still pass the default values in the request
        standardTestFlow(resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP,
                         MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER,
                         warningExpectations);
    }

    /****************/
    /**
     * Test shows that we'll pick up the header setting from server.xml instead of mp config properties
     * The app has the header set to Authorization in META-INF - server.xml has it set to Cookie.
     * The test case passes the token in the Authorization Header using Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_HeaderIsCookieInServerXml_HeaderIsAuthorizationInMPConfig_InMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Header_Cookie.xml");
        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
        // TODO fails this way
    }

    /**
     * Test shows that we'll pick up the header setting from server.xml instead of mp config properties
     * The app has the header set to Authorization uder WEB-INF - server.xml has it set to Cookie.
     * The test case passes the token in the Authorization Header using Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_HeaderIsCookieInServerXml_HeaderIsAuthorizationInMPConfig_UnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Header_Cookie.xml");
        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
    }

    /**
     * Test shows that we'll pick up the header setting from server.xml instead of mp config properties
     * The app has the header set to Cookie in META-INF - server.xml has it set to Authorization.
     * The test case passes the token as a Cookie using Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_HeaderIsAuthorizationInServerXml_HeaderIsCookieInMPConfig_InMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Header_Authorization.xml");
        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
    }

    /**
     * Test shows that we'll pick up the header setting from server.xml instead of mp config properties
     * The app has the header set to Cookie under WEB-INF - server.xml has it set to Authorization.
     * The test case passes the token as a Cookie using Bearer.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_HeaderIsAuthorizationInServerXml_HeaderIsCookieInMPConfig_UnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Header_Authorization.xml");
        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.COOKIE,
                         MpJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
    }

    /**
     * Test shows that we'll pick up the header and cookie settings from server.xml instead of mp config properties
     * The app has the header set to Cookie and the cookie name set to "OtherCookieName" in META-INF - server.xml has header set to cookie and the cookie name set to "Bearer"
     * The test case passes the token as a Cookie using "OtherCookieName".
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_HeaderIsCookieCookieNameSetInServerXml_HeaderIsCookieWithOtherCookieNameInMPConfig_InMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Header_Authorization_withCookie.xml");
        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.COOKIE,
                         "OtherCookieName", setMissingTokenExpectations(resourceServer));
    }

    /**
     * Test shows that we'll pick up the header and cookie settings from server.xml instead of mp config properties
     * The app has the header set to Cookie and the cookie name set to "OtherCookieName" under WEB-INF - server.xml has header set to cookie and the cookie name set to "Bearer"
     * The test case passes the token as a Cookie using "OtherCookieName".
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_HeaderIsCookieCookieNameSetConfigInServerXml_HeaderIsCookieWithOtherCookieNameInMPConfig_UnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Header_Authorization_withCookie.xml");
        standardTestFlow(resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.COOKIE,
                         "OtherCookieName", setMissingTokenExpectations(resourceServer));
    }

    /****************/
// TODO - add audiences tests
    /************************************ Audiences tests ************************************/
    /******************************* Signature Algorithm tests *******************************/
// TODO - add publickey.algorithm tests
// TODO - add encryption tests
    /*********************************** Encryption tests ************************************/

}
