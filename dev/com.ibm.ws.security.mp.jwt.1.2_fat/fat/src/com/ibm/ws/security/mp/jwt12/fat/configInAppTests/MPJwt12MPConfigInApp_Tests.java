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
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
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

    @Server("com.ibm.ws.security.mp.jwt.1.2.fat")
    public static LibertyServer resourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // let's pick a default mp config - we'll allow most of the settings (mpJwt 1.1 attrs) to come from the server.xml
        MP12ConfigSettings mpConfigSettings = new MP12ConfigSettings(MP12ConfigSettings.PublicKeyLocationNotSet, MP12ConfigSettings.PublicKeyNotSet, MP12ConfigSettings.IssuerNotSet, MpJwt12FatConstants.X509_CERT, MP12ConfigSettings.DefaultHeader, MP12ConfigSettings.DefaultCookieName, "client01, client02", MP12ConfigSettings.AlgorithmNotSet);

        setupBootstrapPropertiesForMPTests(resourceServer, MP12ConfigSettings.jwksUri, mpConfigSettings.getCertType().equals(MpJwtFatConstants.JWK_CERT));

        deployRSServerMPConfigInAppHeaderApps(resourceServer, mpConfigSettings);

        startRSServerForMPTests(resourceServer, "rs_server_orig_1_2_withOtherApps.xml");

    }

    /******************************************** tests **************************************/

    /********************************* Start Header & Cookie tests *********************************/
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
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

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP,
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
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP,
                           MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER,
                           warningExpectations);
    }

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
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
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
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
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
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.COOKIE,
                           "OtherCookieName", setMissingTokenExpectations(resourceServer));
    }

    /********************************* End Header & Cookie tests *********************************/

    /************************************ Start Audiences tests ************************************/
    /**
     * Test shows that we'll pick up the audiences setting from mp config properties
     * The app has audiences set to a valid value in META-INF.
     * The test case passes a token with an audience value that matches what is in the mp config properites.
     * (all of the non-Audience tests in this class are really testing with good Audiences values - just adding this test for completeness)
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_GoodAudiencesInMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the audiences setting from mp config properties
     * The app has audiences set to a valid value under WEB-INF.
     * The test case passes a token with an audience value that matches what is in the mp config properties.
     * (all of the non-Audience tests in this class are really testing with good Audiences values - just adding this test for completeness)
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_GoodAudiencesInMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the audiences setting from mp config properties
     * The app has audiences set to an invalid value in META-INF.
     * The test case passes a token with an audience value that does not match what is in the mp config properites.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_BadAudiencesInMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER, setBadAudiencesExpectations(resourceServer));
    }

    /**
     * Test shows that we'll pick up the audiences setting from mp config properties
     * The app has audiences set to an invalid value under WEB-INF.
     * The test case passes a token with an audience value that does not match what is in the mp config properites.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_BadAudiencesInMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER, setBadAudiencesExpectations(resourceServer));
    }

    /**
     * Test shows that we'll pick up the audiences setting from mp config properties
     * The app has audiences set to an invalid value in META-INF.
     * The server.xml has a good audiences value - this will override the values in the mp config.
     * The test case passes a token with an audience value that does not match what is in the mp config properites, but will match what's in the server.xml
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_GoodAudiencesInServerXml_BadAudiencesInMPConfig_InMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Good_Audiences.xml");
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the audiences setting from mp config properties
     * The app has audiences set to an invalid value under WEB-INF.
     * The server.xml has a good audiences value - this will override the values in the mp config.
     * The test case passes a token with an audience value that does not match what is in the mp config properites, but will match what's in the server.xml
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_GoodAudiencesInServerXml_BadAudiencesInMPConfig_UnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Good_Audiences.xml");
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /******************************* End Audiences tests *******************************/

    /******************************* Start Signature Algorithm tests *******************************/
    /*
     * NOTE: the mp.jwt.verify.publickey and mp.jwt.verify.publickey.location attributes were supported with mpJwt 1.1.
     * We're adding mp.jwt.verify.publickey.algorithm as part of the mpJwt 1.2 support, so, we'll just test that.
     * We need to make sure that we can pick up the values in the microprofile-config.properties file (and that
     * a value specified in server.xml will over-ride it.
     */
    /**
     * Test shows that we'll pick up the algorithm setting from mp config properties
     * The app has algorithm set to a valid value in META-INF.
     * The test case passes a token with an algorithm value that matches what is in the mp config properites.
     * (all of the non-algorithm tests in this class are really testing with good algorithms values - just adding this test for completeness)
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_GoodAlgorithmInMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the algorithm setting from mp config properties
     * The app has algorithm set to a valid value under WEB-INF.
     * The test case passes a token with an algorithm value that matches what is in the mp config properties.
     * (all of the non-algorithm tests in this class are really testing with good algorithm values - just adding this test for completeness)
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_GoodAlgorithmInMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.GOOD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the algorithm setting from mp config properties
     * The app has algorithm set to an invalid value in META-INF.
     * The test case passes a token with an algorithm value that does not match what is in the mp config properites.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_BadAlgorithmInMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER, setBadCertExpectations(resourceServer, KeyMismatch));
    }

    /**
     * Test shows that we'll pick up the algorithm setting from mp config properties
     * The app has algorithm set to an invalid value under WEB-INF.
     * The test case passes a token with an algorithm value that does not match what is in the mp config properites.
     *
     * @throws Exception
     */
    @Test

    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_BadAlgorithmInMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER, setBadCertExpectations(resourceServer, KeyMismatch));
    }

    /**
     * Test shows that we'll pick up the algorithm setting from mp config properties
     * The app has algorithm set to an invalid value in META-INF.
     * The server.xml has a good algorithm value - this will override the values in the mp config.
     * The test case passes a token with an algorithm value that does not match what is in the mp config properites, but will match what's in the server.xml
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_GoodAlgorithmInServerXml_BadAlgorithmInMPConfig_InMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Good_Algorithm.xml");
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the algorithm setting from mp config properties
     * The app has algorithm set to an invalid value under WEB-INF.
     * The server.xml has a good algorithm value - this will override the values in the mp config.
     * The test case passes a token with an algorithm value that does not match what is in the mp config properites, but will match what's in the server.xml
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_GoodAlgorithmInServerXml_BadAlgorithmInMPConfig_UnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Good_Algorithm.xml");
        standard12TestFlow(MpJwt12FatConstants.SIGALG_RS256, resourceServer, MpJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the key and algorithm setting from mp config properties
     * The app has the key and algorithm set to a valid NON-default value in META-INF.
     * The server.xml does NOT have a key or algorithm defined, but does have an internal default for the algorithm - this test shows that the mp config will over-ride what's
     * internal to the server.
     * The test case needs to build a token using a different algorithm.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_GoodNonDefaultKeyAndAlgorithmInMPConfig_InMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_No_Key_Algorithm.xml");
        standard12TestFlow(MpJwt12FatConstants.SIGALG_ES256, resourceServer, MpJwt12FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the key and algorithm setting from mp config properties
     * The app has the key and algorithm set to a valid NON-default value under WEB-INF.
     * The server.xml does NOT have a key or algorithm defined, but does have an internal default for the algorithm - this test shows that the mp config will over-ride what's
     * internal to the server.
     * The test case needs to build a token using a different algorithm.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_GoodNonDefaultKeyAndAlgorithmInMPConfig_UnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_No_Key_Algorithm.xml");
        standard12TestFlow(MpJwt12FatConstants.SIGALG_ES256, resourceServer, MpJwt12FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MpJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /******************************* End Signature Algorithm tests *******************************/

// TODO - add encryption tests
    /******************************** Start Encryption tests **********************************/
    /********************************* End Encryption tests ***********************************/

}
