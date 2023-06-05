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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt21.fat.configInAppTests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt21FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt21MPConfigTests;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP21ConfigSettings;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
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
@RunWith(FATRunner.class)
@SkipForRepeat({ EmptyAction.ID })
public class MPJwt21MPConfigInApp_Tests extends MPJwt21MPConfigTests {

    public static Class<?> thisClass = MPJwt21MPConfigInApp_Tests.class;

    @Server("com.ibm.ws.security.mp.jwt.2.1.fat")
    public static LibertyServer resourceServer;

    public static final JwtTokenBuilderUtils builderHelpers = new JwtTokenBuilderUtils();

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // let's pick a default mp config - we'll allow most of the settings (mpJwt 1.1/2.1 attrs) to come from the server.xml
        MP21ConfigSettings mpConfigSettings = new MP21ConfigSettings(MP21ConfigSettings.PublicKeyLocationNotSet, JwtKeyTools
                .getComplexPublicKeyForSigAlg(resourceServer, MPJwt21FatConstants.SIGALG_RS256),
                MP21ConfigSettings.IssuerNotSet, MPJwt21FatConstants.X509_CERT,
                MP21ConfigSettings.DefaultHeader, MP21ConfigSettings.DefaultCookieName, MP21ConfigSettings.AudiencesNotSet, MP21ConfigSettings.AlgorithmNotSet,
                MP21ConfigSettings.DecryptKeyLocNotSet, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew, MP21ConfigSettings.DefaultKeyMgmtKeyAlg);

        setupBootstrapPropertiesForMPTests(resourceServer, MP21ConfigSettings.jwksUri, mpConfigSettings.getCertType().equals(MPJwt21FatConstants.JWK_CERT));

        deployRSServerMPConfigInAppVariationApps(resourceServer, mpConfigSettings);

        startRSServerForMPTests(resourceServer, "rs_server_orig_withOtherApps.xml");

    }

    /******************************************** tests **************************************/

    /********************************* Start Token Age tests *********************************/
    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_DefaultTokenAgeInMPConfig_InMetaInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.DEFAULT_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_LongTokenAgeInMPConfig_InMetaInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.LONG_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_ShortTokenAgeInMPConfig_InMetaInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.SHORT_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, 5, setShortTokenAgeExpectations(resourceServer));
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_DefaultTokenAgeInMPConfig_UnderWebInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.DEFAULT_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_LongTokenAgeInMPConfig_UnderWebInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.LONG_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_ShortTokenAgeInMPConfig_UnderWebInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.SHORT_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, 5, setShortTokenAgeExpectations(resourceServer));
    }

    /********************************** End Token Age tests **********************************/

    /********************************* Start Clock Skew tests ********************************/
    // set tokenAge to a short value since tokenAge and clockSkew together will be used to determine if the token is no longer valid - setting an exp value that will be expired does not help with testing clockSkew - clockSkew is not used used in validating exp
    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_DefaultClockSkewInMPConfig_InMetaInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.DEFAULT_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, 5, null);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_LongClockSkewInMPConfig_InMetaInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.LONG_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, 5, null);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_ShortClockSkewInMPConfig_InMetaInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.SHORT_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, 10, setShortTokenAgeExpectations(resourceServer));
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_DefaultClockSkewInMPConfig_UnderWebInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.DEFAULT_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, 5, null);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_LongClockSkewInMPConfig_UnderWebInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.LONG_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, 5, null);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_ShortClockSkewInMPConfig_UnderWebInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.SHORT_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, 10, setShortTokenAgeExpectations(resourceServer));
    }

    /********************************** End Clock Skew tests *********************************/
    /********************************* Start (new encrypt attr) tests *********************************/
    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_DefaultKeyMgmtKeyAlg_inMetaInf_test() throws Exception {

        standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.DEFAULT_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_DefaultKeyMgmtKeyAlg_underWebInf_test() throws Exception {

        standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.DEFAULT_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_MatchKeyMgmtKeyAlg_inMetaInf_test() throws Exception {

        standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.MATCH_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_MatchKeyMgmtKeyAlg_underWebInf_test() throws Exception {

        standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.MATCH_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_MismatchKeyMgmtKeyAlg_inMetaInf_test() throws Exception {

        standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.MISMATCH_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, setEncryptAlgMismatchExpectations(resourceServer, MPJwt12FatConstants.KEY_MGMT_KEY_ALG_256));
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_MismatchKeyMgmtKeyAlg_underWebInf_test() throws Exception {

        standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.MISMATCH_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, setEncryptAlgMismatchExpectations(resourceServer, MPJwt12FatConstants.KEY_MGMT_KEY_ALG_256));
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_InvalidKeyMgmtKeyAlg_inMetaInf_test() throws Exception {

        standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.INVALID_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, setEncryptAlgMismatchExpectations(resourceServer, "SomeString"));
    }

    @Test
    public void MPJwt21MPConfigInApp_NoMPJwt21ConfigInServerXml_InvalidKeyMgmtKeyAlg_underWebInf_test() throws Exception {

        standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.INVALID_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, setEncryptAlgMismatchExpectations(resourceServer, "SomeString"));
    }

    /********************************* End (new encrypt attr) tests ***********************************/

}
