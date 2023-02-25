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
package com.ibm.ws.security.mp.jwt21.fat.featureSupportTests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt21FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt21MPConfigTests;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP21ConfigSettings;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.EmptyAction;
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

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@SkipForRepeat({ EmptyAction.ID })
public class FeatureSupport_ConfigInAppTests extends MPJwt21MPConfigTests {

    public static Class<?> thisClass = FeatureSupport_ConfigInAppTests.class;

    @Server("com.ibm.ws.security.mp.jwt.2.1.fat")
    public static LibertyServer resourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        MP21ConfigSettings mpConfigSettings = new MP21ConfigSettings(MP21ConfigSettings.PublicKeyLocationNotSet, JwtKeyTools
                .getComplexPublicKeyForSigAlg(resourceServer, MPJwt21FatConstants.SIGALG_RS256),
                MP21ConfigSettings.IssuerNotSet, MPJwt21FatConstants.X509_CERT,
                MP21ConfigSettings.DefaultHeader, MP21ConfigSettings.DefaultCookieName, MP21ConfigSettings.AudiencesNotSet, MP21ConfigSettings.AlgorithmNotSet,
                MP21ConfigSettings.DecryptKeyLocNotSet, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew, MP21ConfigSettings.DefaultKeyMgmtKeyAlg);

        setupBootstrapPropertiesForMPTests(resourceServer, MP21ConfigSettings.jwksUri, mpConfigSettings.getCertType().equals(MPJwt21FatConstants.JWK_CERT));

        deployRSServerMPConfigInAppVariationApps(resourceServer, mpConfigSettings);

        startRSServerForMPTests(resourceServer, "rs_server_orig_withOtherApps.xml");

    }

    // The following tests are similar to the negative tests in MPJwt21MPConfigInApp_Tests - these tests result in failures when run with the proper version of mpJwt, but when we don't recognize the attributes, we should get the default behavior and these tests will succeed
    /******************************************** tests **************************************/
    @Test
    public void OlderMpJwtFeature_ConfigInAppTests_NoMPJwt21ConfigInServerXml_ShortTokenAgeInMPConfig_InMetaInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.SHORT_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, 5, null);
    }

    @Test
    public void OlderMpJwtFeature_ConfigInAppTests_NoMPJwt21ConfigInServerXml_ShortTokenAgeInMPConfig_UnderWebInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.SHORT_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, 5, null);
    }

    /********************************** End Token Age tests **********************************/

    /********************************* Start Clock Skew tests ********************************/
    @Test
    public void OlderMpJwtFeature_ConfigInAppTests_NoMPJwt21ConfigInServerXml_ShortClockSkewInMPConfig_InMetaInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.SHORT_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, 10, null);
    }

    @Test
    public void OlderMpJwtFeature_ConfigInAppTests_NoMPJwt21ConfigInServerXml_ShortClockSkewInMPConfig_UnderWebInf_test() throws Exception {

        standard21TestFlow(MPJwt21FatConstants.JWT_BUILDER_DEFAULT_ID, resourceServer, MPJwt21FatConstants.SHORT_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, 10, null);
    }

    /********************************** End Clock Skew tests *********************************/
    /********************************* Start (new encrypt attr) tests *********************************/
    @Test
    public void OlderMpJwtFeature_ConfigInAppTests_NoMPJwt21ConfigInServerXml_MismatchKeyMgmtKeyAlg_inMetaInf_test() throws Exception {

        // Encryption not allowed with 1.1 so, we won't be able to handle a JWE at all - 1.2 will allow a JWE, but won't recognize the decrypt alg
        if (RepeatTestFilter.getRepeatActionsAsString().contains(MPJwt11FatConstants.MP_JWT_11)) {
            standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.MISMATCH_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, setOnlyJWSAcceptedExpectations(resourceServer));
        } else {
            standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.MISMATCH_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);
        }
    }

    @Test
    public void OlderMpJwtFeature_ConfigInAppTests_NoMPJwt21ConfigInServerXml_MismatchKeyMgmtKeyAlg_underWebInf_test() throws Exception {

        // Encryption not allowed with 1.1 so, we won't be able to handle a JWE at all - 1.2 will allow a JWE, but won't recognize the decrypt alg
        if (RepeatTestFilter.getRepeatActionsAsString().contains(MPJwt11FatConstants.MP_JWT_11)) {
            standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.MISMATCH_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, setOnlyJWSAcceptedExpectations(resourceServer));
        } else {
            standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.MISMATCH_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);
        }
    }

    @Test
    public void OlderMpJwtFeature_ConfigInAppTests_NoMPJwt21ConfigInServerXml_InvalidKeyMgmtKeyAlg_inMetaInf_test() throws Exception {

        // Encryption not allowed with 1.1 so, we won't be able to handle a JWE at all - 1.2 will allow a JWE, but won't recognize the decrypt alg
        if (RepeatTestFilter.getRepeatActionsAsString().contains(MPJwt11FatConstants.MP_JWT_11)) {
            standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.INVALID_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, setOnlyJWSAcceptedExpectations(resourceServer));
        } else {
            standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.INVALID_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);
        }
    }

    @Test
    public void OlderMpJwtFeature_ConfigInAppTests_NoMPJwt21ConfigInServerXml_InvalidKeyMgmtKeyAlg_underWebInf_test() throws Exception {

        // Encryption not allowed with 1.1 so, we won't be able to handle a JWE at all - 1.2 will allow a JWE, but won't recognize the decrypt alg
        if (RepeatTestFilter.getRepeatActionsAsString().contains(MPJwt11FatConstants.MP_JWT_11)) {
            standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.INVALID_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF, setOnlyJWSAcceptedExpectations(resourceServer));
        } else {
            standard21TestFlow("sign_RS256_enc_RS256", resourceServer, MPJwt21FatConstants.INVALID_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT, MPJwt21FatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MPJwt21FatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);
        }
    }
}
