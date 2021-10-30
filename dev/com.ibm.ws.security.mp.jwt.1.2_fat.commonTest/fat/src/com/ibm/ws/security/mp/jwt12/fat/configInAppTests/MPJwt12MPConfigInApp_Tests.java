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

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt12MPConfigTests;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP12ConfigSettings;

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

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
public class MPJwt12MPConfigInApp_Tests extends MPJwt12MPConfigTests {

    public static Class<?> thisClass = MPJwt12MPConfigInApp_Tests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.2.fat")
    public static LibertyServer resourceServer;

    private static final boolean DoNotExpectExtraMsgs = false;

    public static final JwtTokenBuilderUtils builderHelpers = new JwtTokenBuilderUtils();

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // let's pick a default mp config - we'll allow most of the settings (mpJwt 1.1 attrs) to come from the server.xml
        MP12ConfigSettings mpConfigSettings = new MP12ConfigSettings(MP12ConfigSettings.PublicKeyLocationNotSet, JwtKeyTools
                        .getComplexPublicKeyForSigAlg(resourceServer,
                                                      MPJwt12FatConstants.SIGALG_RS256), MP12ConfigSettings.IssuerNotSet, MPJwt12FatConstants.X509_CERT, MP12ConfigSettings.DefaultHeader, MP12ConfigSettings.DefaultCookieName, "client01, client02", MP12ConfigSettings.AlgorithmNotSet, MP12ConfigSettings.DecryptKeyLocNotSet);

        setupBootstrapPropertiesForMPTests(resourceServer, MP12ConfigSettings.jwksUri, mpConfigSettings.getCertType().equals(MPJwt12FatConstants.JWK_CERT));

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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.COOKIE,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the header setting from mp config properties
     * The app has the header set to Cookie under WEB-INF. No Cookie name is set.
     * The test case passes the token as a Cookie using the name Bearer.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieInMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.COOKIE,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.COOKIE,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.COOKIE,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.COOKIE,
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.COOKIE,
                           "OtherCookieName");
    }

    /**
     * Test shows that we'll pick up the header and cookie settings from mp config properties
     * The app has the header set to Cookie in META-INF. The Cookie name is set to a value OTHER THAN Bearer.
     * The test case passes the token as a Cookie using the name "Bearer" - the runtime won't find the token.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_HeaderIsCookieWithOtherCookieNameInMPConfig_InMetaInf_PassCookieWithNameBearer_test() throws Exception {

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.COOKIE,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.COOKIE,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
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

        String urlCalled = buildAppUrl(resourceServer, MPJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP);
        Expectations warningExpectations = setBadHeaderValueExpectations(resourceServer, urlCalled, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP,
                           MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
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

        String urlCalled = buildAppUrl(resourceServer, MPJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP);
        Expectations warningExpectations = setBadHeaderValueExpectations(resourceServer, urlCalled, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

        // the value in the mp config is bad - we'll still pass the default values in the request
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP,
                           MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER,
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.COOKIE,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.COOKIE,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.COOKIE,
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.COOKIE,
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
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_GoodAudiencesInMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setBadAudiencesExpectations(resourceServer));
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setBadAudiencesExpectations(resourceServer));
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the algorithm setting from mp config properties
     * The app has algorithm set to a valid value under WEB-INF.
     * The test case passes a token with an algorithm value that matches what is in the mp config properties.
     * (all of the non-algorithm tests in this class are really testing with good algorithm values - just adding this test for completeness)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_GoodAlgorithmInMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.GOOD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setBadCertExpectations(resourceServer, KeyMismatch));
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

        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setBadCertExpectations(resourceServer, KeyMismatch));
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_RS256, resourceServer, MPJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_ES256, resourceServer, MPJwt12FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
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
        standard12TestFlow(MPJwt12FatConstants.SIGALG_ES256, resourceServer, MPJwt12FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /******************************* End Signature Algorithm tests *******************************/

    /******************************** Start Encryption tests **********************************/
    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS256 private key in META-INF.
     * The token was created using the paired public key - decrypt should be good and we should have access to the app
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocRelativeForRS256InMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                           MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS256 private key in META-INF.
     * The token passed in used an RS384 key to encrypt - decrypt should fail
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_EncryptRS384DecryptRS256InMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS384_enc_RS384", resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMismatchExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS256 private key in META-INF.
     * The token passed in used an RS512 key to encrypt - decrypt should fail
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_EncryptRS5124DecryptRS256InMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS512_enc_RS512", resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMismatchExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the fully qualified file location for the RS256 private key under WEB-INF.
     * The token was created using the paired public key - decrypt should be good and we should have access to the app
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocFileForRS256InMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.GOOD_FILE_DECRYPT_KEY_RS256_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the URL file location for the RS384 private key in META-INF.
     * The token was created using the paired public key - decrypt should be good and we should have access to the app
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocUrlForRS384InMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS384_enc_RS384", resourceServer, MPJwt12FatConstants.GOOD_URL_DECRYPT_KEY_RS384_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS384 private key under WEB-INF.
     * The token was created using the paired public key - decrypt should be good and we should have access to the app
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocRelativeForRS384InMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow("sign_RS384_enc_RS384", resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS384_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS384 private key in META-INF.
     * The token passed in used an RS256 key to encrypt - decrypt should fail
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_EncryptRS2564DecryptRS384InMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS384_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMismatchExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS384 private key in META-INF.
     * The token passed in used an RS512 key to encrypt - decrypt should fail
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_EncryptRS5124DecryptRS384InMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow("sign_RS512_enc_RS512", resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS384_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMismatchExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the fully qualified file location for the RS512 private key in META-INF.
     * The token was created using the paired public key - decrypt should be good and we should have access to the app
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocFileForRS512InMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS512_enc_RS512", resourceServer, MPJwt12FatConstants.GOOD_FILE_DECRYPT_KEY_RS512_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the fully qualified file location for the RS512 private key in META-INF.
     * The token passed in used an RS256 key to encrypt - decrypt should fail
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_EncryptRS2564DecryptRS512InMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.GOOD_FILE_DECRYPT_KEY_RS512_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMismatchExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the fully qualified file location for the RS512 private key in META-INF.
     * The token passed in used an RS384 key to encrypt - decrypt should fail
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_EncryptRS3844DecryptRS512InMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS384_enc_RS384", resourceServer, MPJwt12FatConstants.GOOD_FILE_DECRYPT_KEY_RS512_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMismatchExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the url file location for the RS512 private key under WEB-INF.
     * The token was created using the paired public key - decrypt should be good and we should have access to the app
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocUrlForRS512InMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow("sign_RS512_enc_RS512", resourceServer, MPJwt12FatConstants.GOOD_URL_DECRYPT_KEY_RS512_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the ES256 private key in META-INF.
     * We expect a failure as ES256 is not supported for encryption.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocES256InMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.BAD_DECRYPT_KEY_ES256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMissingKeyExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the ES256 private key under WEB-INF.
     * We expect a failure as ES256 is not supported for encryption.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocES256InMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.BAD_DECRYPT_KEY_ES256_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMissingKeyExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to plain text version of the RS256 private key in META-INF.
     * We expect a failure as we do not allow a plain text key.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocPlainTextInMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.BAD_PLAINTEXT_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptPlainTextKeyExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to jwks uri under WEB-INF.
     * We expect a failure as the jwks uri will not return the private key.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptKeyLocJwksUriInMPConfig_UnderWebInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.BAD_JWKSURI_DECRYPT_KEY_RS256_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMissingKeyExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative key location for the RS256 private key in META-INF.
     * The token was created using the paired public key, but the keyManagementKeyAlgorithm in the token is set to RSA_OAEP-256
     * instead of RSA_OAEP - decrypt should be good and we should have access to the app.
     * The builder doesn't allow that value for keyManagementKeyAlgorithm, but we should accept it.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_encrypt_mpJwtRS256_token_RSA_OAEP_256_RS256_publicKey_A256GCM() throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_UPN, defaultUser));
        // add more args
        String encryptKey = JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256);

        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_KEY_MGMT_ALG, MPJwt12FatConstants.KEY_MGMT_KEY_ALG_256));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_ENCRYPT_KEY, encryptKey));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_CONTENT_ENCRYPT_ALG, MPJwt12FatConstants.DEFAULT_CONTENT_ENCRYPT_ALG));
        String token = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "sign_RS256_enc_RS256", extraClaims);

        useToken(token,
                 buildAppUrl(resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                             MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP),
                 MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.AUTHORIZATION,
                 MPJwt12FatConstants.TOKEN_TYPE_BEARER, null);

    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative key location for the RS256 private key in META-INF.
     * The token was created using the paired public key, but the contentEncryptionAlgorith in the token is set to A192AGM
     * instead of A256AGM - decrypt should be good and we should have access to the app.
     * The builder doesn't allow that value for contentEncryptionAlgorithm, but we should accept it.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_encrypt_mpJwtRS256_token_RSA_OAEP_RS256_publicKey_A192GCM() throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_UPN, defaultUser));
        // add more args
        String encryptKey = JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256);

        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_KEY_MGMT_ALG, MPJwt12FatConstants.DEFAULT_KEY_MGMT_KEY_ALG));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_ENCRYPT_KEY, encryptKey));
        extraClaims.add(new NameValuePair(MPJwt12FatConstants.PARAM_CONTENT_ENCRYPT_ALG, MPJwt12FatConstants.CONTENT_ENCRYPT_ALG_192));
        String token = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "sign_RS256_enc_RS256", extraClaims);

        useToken(token,
                 buildAppUrl(resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                             MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP),
                 MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.AUTHORIZATION,
                 MPJwt12FatConstants.TOKEN_TYPE_BEARER, null);
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to a plain text string in META-INF.
     * We expect a failure as a simple string is not valid.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptStringKeyInMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.BAD_STRING_DECRYPT_KEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMissingKeyExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the same public key that was used to create the token in META-INF.
     * We expect a failure as we need to specify the private key.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptPublicKeyInMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.BAD_DECRYPT_PUBLIC_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptMissingKeyExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to a private key that is too short (less than 2048 in length) in META-INF.
     * We expect a failure as the consuming code with determine that the key is too short. (It will fail for that before we
     * would hit an error where we can't decrypt because the keys don't match (the builder won't allow a short key to be
     * used to encrypt))
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptShortKeyInMPConfig_InMetaInf_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.BAD_SHORT_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptShortKeyTypeExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS256 key in META-INF.
     * The token that is used contains a simple Json Payload and not a full JWS.
     * We expect a failure as we do NOT support this payload at this time.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptRS256InMPConfig_InMetaInf_simpleJsonPayload_test() throws Exception {

        String jwtToken = builderHelpers
                        .buildAlternatePayloadJWEToken(JwtKeyTools
                                        .getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256)));

        useToken(jwtToken,
                 buildAppUrl(resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                             MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP),
                 MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.AUTHORIZATION,
                 MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptInvalidPayloadExpectations(resourceServer, DoNotExpectExtraMsgs));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS256 key in META-INF.
     * The token that is used has the "typ" value in the JWE header set to notJose.
     * We expect a failure as we expect the value to be set to "JOSE".
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptRS256InMPConfig_InMetaInf_JweTypeNotJose_test() throws Exception {

        // build a jwe token that has "typ" set to "notJOSE" instead of "JOSE".  The token will be encrypted with RS256 and signed with HS256.
        String jwtToken = builderHelpers
                        .buildJWETokenWithAltHeader(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256)),
                                                    "notJOSE", "jwt");

        // The test code generates a token that is encrypted with RS256, but signed using HS256
        // the code that checks the JWE type runs before the signature is checked, so if we get far enough to fail on
        // on the signature, we haven't failed checking the JWE Type :)
        useToken(jwtToken,
                 buildAppUrl(resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                             MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP),
                 MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.AUTHORIZATION,
                 MPJwt12FatConstants.TOKEN_TYPE_BEARER, setBadCertExpectations(resourceServer, KeyMismatch));
    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS256 key in META-INF.
     * The token that is used has the "cty" value in the JWE header set to not_jwt.
     * We expect the token to be decrypted and that we have access to the protected app. We shouldn't be checking the content of cty
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptRS256InMPConfig_InMetaInf_JweContentTypeNotJwt_test() throws Exception {

        // build a jwe token that has "cty" set to "not_jwt" instead of "jwt".  The token will be encrypted with RS256 and signed with HS256.
        String jwtToken = builderHelpers
                        .buildJWETokenWithAltHeader(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(jwtBuilderServer, MPJwt12FatConstants.SIGALG_RS256)),
                                                    "JOSE", "not_jwt");

        useToken(jwtToken,
                 buildAppUrl(resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                             MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP),
                 MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.AUTHORIZATION,
                 MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptBadCtyExpectations(resourceServer, DoNotExpectExtraMsgs));

    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the RS256 key in META-INF.
     * The token that is used is a JWS and NOT a JWE
     * We expect a failure as the mpJwt is expecting (and will only accept a JWE)
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_DecryptRS256InMPConfig_InMetaInf_TokenNotEncrypted_test() throws Exception {

        standard12TestFlow("RS256", resourceServer, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                           MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setEncryptNotJWETokenExpectations(resourceServer, DoNotExpectExtraMsgs));

    }

    /**
     * Test shows that we'll pick up the private key setting from mp config properties
     * The app does NOT have mp.jwt.decrypt.key.location set in META-INF.
     * The token that is used is a JWE and NOT a JWS
     * We expect a failure as the mpJwt is expecting (and will only accept a JWS). It has no way of decrypting the token
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_NoMPJwt12ConfigInServerXml_NoDecryptInMPConfig_InMetaInf_TokenEncrypted_test() throws Exception {

        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                           MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER, setNoEncryptNotJWSTokenExpectations(resourceServer, DoNotExpectExtraMsgs));

    }

    /**
     * Test shows that config in server.xml will override the settings in mp config properties
     * The app has mp.jwt.decrypt.key.location set to the relative file location for the ES256 key in META-INF.
     * The server.xml config references the RS256 key.
     * We expect the token to be decrypted and to have access to the protected app as the good server.xml values
     * will override the bad value in the mp config properties file.
     *
     * @throws Exception
     */
    @Test
    public void MPJwt12MPConfigInApp_MPJwt12ConfigInServerXmlOverrides_BadDecryptInMPConfig_InMetaInf_test() throws Exception {

        // config server to use a configuration that has valid decrypt info
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_Good_Decrypt.xml");

        // use an app that has an invalid decrypt location
        standard12TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt12FatConstants.BAD_DECRYPT_KEY_ES256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                           MPJwt12FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt12FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt12FatConstants.AUTHORIZATION,
                           MPJwt12FatConstants.TOKEN_TYPE_BEARER);

    }

    /********************************* End Encryption tests ***********************************/

}
