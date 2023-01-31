/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.feature1_1;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.mp.jwt.MPJwt21FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt21MPConfigTests;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP21ConfigSettings;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class that will test the placement of mp jwt config settings inside of an app. They can be found in multiple
 * places within the application. The server.xml will only specify the signature algorithm (to match each test case)
 * and audience (just so we can re-use the same builders) - everything else will come from the application.
 * * The tests will do one of the following:
 * - request use of app that has placement of config in "resources/META-INF/microprofile-config.properties"
 * - request use of app that has placement of config in "resources/WEB-INF/classes/META-INF/microprofile-config.properties"
 *
 **/

@SkipForRepeat({ SkipForRepeat.EE9_FEATURES + "_" + MPJwt21FatConstants.MP_JWT_21 }) // tests purposely skipped as we want the server.xml using 1.1 to prove that the function isn't accidentally allowed.
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class Feature11Enabled_ConfigInAppTests extends MPJwt21MPConfigTests {

    public static Class<?> thisClass = Feature11Enabled_ConfigInAppTests.class;

    @Server("com.ibm.ws.security.mp.jwt.2.1.fat")
    public static LibertyServer resourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        MP21ConfigSettings mpConfigSettings = new MP21ConfigSettings(MP21ConfigSettings.PublicKeyLocationNotSet, MP21ConfigSettings.PublicKeyNotSet,
                MP21ConfigSettings.IssuerNotSet, MPJwt21FatConstants.X509_CERT, MP21ConfigSettings.DefaultHeader, MP21ConfigSettings.DefaultCookieName,
                "client01, client02", MP21ConfigSettings.AlgorithmNotSet, MP21ConfigSettings.DecryptKeyLocNotSet, MP21ConfigSettings.DefaultTokenAge,
                MP21ConfigSettings.DefaultClockSkew, MP21ConfigSettings.DefaultKeyMgmtKeyAlg);

        setupBootstrapPropertiesForMPTests(resourceServer, MP21ConfigSettings.jwksUri, mpConfigSettings.getCertType().equals(MPJwt21FatConstants.JWK_CERT));

        deployRSServerMPConfigInAppVariationApps(resourceServer, mpConfigSettings);

        startRSServerForMPTests(resourceServer, "rs_server_orig_1_2_withOtherApps.xml");

    }

    /******************************************** tests **************************************/
    //
    //    /********************************* End Header & Cookie tests *********************************/
    //    /**
    //     * Test sets the mp config property "mp.jwt.token.header" to "Authorization" in META-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the default behavior
    //     * which is to look for the token in the Auth Header.
    //     * The test will pass the token in the Auth header, so, we expect to successfully access our protected app.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_HeaderIsAuthorizationInMPConfig_InMetaInf_test() throws Exception {
    //
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_RS256, resourceServer, MPJwt21FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER);
    //    }
    //
    //    /**
    //     * Test sets the mp config property "mp.jwt.token.header" to "Authorization" under WEB-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the default behavior
    //     * which is to look for the token in the Auth Header.
    //     * The test will pass the token in the Auth header, so, we expect to successfully access our protected app.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_HeaderIsAuthorizationInMPConfig_UnderWebInf_test() throws Exception {
    //
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_RS256, resourceServer, MPJwt21FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER);
    //    }
    //
    //    /**
    //     * Test sets the mp config property "mp.jwt.token.header" to "Cookie" in META-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the default behavior
    //     * which is to look for the token in the Auth Header.
    //     * The test will pass the token as a Cookie name Bearer, so, we expect a failure indicating that the token was not provided.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_HeaderIsCookieInMPConfig_InMetaInf_test() throws Exception {
    //
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_RS256, resourceServer, MPJwt21FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt21FatConstants.COOKIE,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
    //    }
    //
    //    /**
    //     * Test sets the mp config property "mp.jwt.token.header" to "Cookie" under WEB-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the default behavior
    //     * which is to look for the token in the Auth Header.
    //     * The test will pass the token as a Cookie name Bearer, so, we expect a failure indicating that the token was not provided.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_HeaderIsCookieInMPConfig_UnderWebInf_test() throws Exception {
    //
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_RS256, resourceServer, MPJwt21FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt21FatConstants.COOKIE,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER, setMissingTokenExpectations(resourceServer));
    //    }
    //
    //    // Can't really test the cookie name config attribute - the runtime only uses that when the token is passed in a cookie and the above tests just showed that with
    //    //  the mpJWT-1.1 feature, we won't look for the token in a cookie - so, ...
    //
    //    /********************************* End Header & Cookie tests *********************************/
    //
    //    /************************************ Start Audiences tests ************************************/
    //    /**
    //     * Test sets the mp config property "mp_jwt_verify_audiences" to "client03, client04" in META-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the default behavior
    //     * which is to pick the value up from the server.xml. The server.xml has the valid value.
    //     * The test expects to successfully access our protected app.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_GoodAudiencesInServerXml_BadAudiencesInMPConfig_InMetaInf_test() throws Exception {
    //
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_RS256, resourceServer, MPJwt21FatConstants.BAD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER);
    //    }
    //
    //    /**
    //     * Test sets the mp config property "mp_jwt_verify_audiences" to "client03, client04" in META-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the default behavior
    //     * which is to pick the value up from the server.xml. The server.xml has the valid value.
    //     * The test expects to successfully access our protected app.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_GoodAudiencesInServerXml_BadAudiencesInMPConfig_UnderWebInf_test() throws Exception {
    //
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_RS256, resourceServer, MPJwt21FatConstants.BAD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER);
    //    }
    //
    //    /**
    //     * Test sets the mp config property "mp_jwt_verify_audiences" to "client03, client04" in META-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the default behavior
    //     * which is no audience set.
    //     * The test will fail as the runtime can not validate the audiences passed.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_NoAudiencesInServerXml_GoodAudiencesInMPConfig_InMetaInf_test() throws Exception {
    //
    //        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_orig_1_1_withOtherApps_noAudiences.xml");
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_RS256, resourceServer, MPJwt21FatConstants.GOOD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER, setBadAudiencesExpectations(resourceServer));
    //    }
    //
    //    /**
    //     * Test sets the mp config property "mp_jwt_verify_audiences" to "client03, client04" under WEB-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the default behavior
    //     * which is no audience set.
    //     * The test will fail as the runtime can not validate the audiences passed.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_NoAudiencesInServerXml_GoodAudiencesInMPConfig_UnderWebInf_test() throws Exception {
    //
    //        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_orig_1_1_withOtherApps_noAudiences.xml");
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_RS256, resourceServer, MPJwt21FatConstants.GOOD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER, setBadAudiencesExpectations(resourceServer));
    //    }
    //
    //    /******************************* End Audiences tests *******************************/
    //
    //    /******************************* Start Signature Algorithm tests *******************************/
    //    /**
    //     * Test sets the mp config property "mp.jwt.verify.publickey.algorithm" to "ES256" in META-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the "default" value
    //     * since we do NOT have any value coded in the server.xml. The default is RS256 and we should get a signing failure.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_MatchingNonDefaultKeyAndAlgorithmInMPConfig_InMetaInf_test() throws Exception {
    //
    //        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_orig_1_1_withOtherApps_noKeyName.xml");
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_ES256, resourceServer, MPJwt21FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER, setBadCertExpectations(resourceServer, KeyMismatch));
    //    }
    //
    //    /**
    //     * Test sets the mp config property "mp.jwt.verify.publickey.algorithm" to "ES256" under WEB-INF.
    //     * The mpJwt-1.1 feature does not support this property, so, we should see the runtime use the "default" value
    //     * since we do NOT have any value coded in the server.xml. The default is RS256 and we should get a signing failure.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_MatchingNonDefaultKeyAndAlgorithmInMPConfig_UnderWebInf_test() throws Exception {
    //
    //        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_orig_1_1_withOtherApps_noKeyName.xml");
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_ES256, resourceServer, MPJwt21FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER, setBadCertExpectations(resourceServer, KeyMismatch));
    //    }
    //
    //    /******************************* End Signature Algorithm tests *******************************/
    //
    //    /******************************* Start Encryption tests ************************************/
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_DecryptKeyLocRelativeForRS256InMPConfig_InMetaInf_JWEToken_test() throws Exception {
    //
    //        standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
    //                MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER, setNoEncryptNotJWSTokenExpectations(resourceServer, false));
    //    }
    //
    //    @Test
    //    public void Feature11Enabled_ConfigInAppTests_DecryptKeyLocRelativeForRS256InMPConfig_InMetaInf_JWSToken_test() throws Exception {
    //
    //        standard21TestFlow(MPJwt21FatConstants.SIGALG_RS256, resourceServer, MPJwt21FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
    //                MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
    //                MPJwt21FatConstants.AUTHORIZATION,
    //                MPJwt21FatConstants.TOKEN_TYPE_BEARER);
    //    }
    //
    //    /******************************* End Encryption tests ************************************/

}
