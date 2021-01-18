/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt12.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;
import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureRepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;
import com.ibm.ws.security.mp.jwt12.fat.configInAppTests.MPJwt12MPConfigInApp_Tests;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_Algorithm;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_Audiences;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_HeaderAuthorization;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_HeaderCookie;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_HeaderCookie_withCookieName;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_decryptKeyLoc_AlternateDecryptSettings;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_decryptKeyLoc_RS256File;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_decryptKeyLoc_RS256PlainText;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_decryptKeyLoc_RS512Url;
import com.ibm.ws.security.mp.jwt12.fat.featureSupportTests.feature1_1.Feature11Enabled_ConfigInAppTests;
import com.ibm.ws.security.mp.jwt12.fat.featureSupportTests.feature1_1.Feature11Enabled_ConfigInServerXmlTests;
import com.ibm.ws.security.mp.jwt12.fat.featureSupportTests.feature1_1.Feature11Enabled_MpConfigAsEnvVars;
import com.ibm.ws.security.mp.jwt12.fat.featureSupportTests.feature1_1.Feature11Enabled_MpConfigAsSystemProperties;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_Algorithm;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_Audiences;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_HeaderAuthorization;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_HeaderCookie;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_HeaderCookie_withCookieName;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_decryptKeyLoc_AlternateDecryptSettings;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_decryptKeyLoc_RS256Jwk;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_decryptKeyLoc_RS384RelativeFile;

import componenttest.rules.repeater.RepeatTests;

@SuppressWarnings("restriction")
@RunWith(Suite.class)
@SuiteClasses({

                AlwaysRunAndPassTest.class,

                // 1.2 tests
                MPJwt12ConfigUsingBuilderTests.class,
                // mp-config specified in applications
                MPJwt12MPConfigInApp_Tests.class,
                // mp-config specified as system properties
                MPJwtGoodMP12ConfigAsSystemProperties_HeaderAuthorization.class,
                MPJwtGoodMP12ConfigAsSystemProperties_HeaderCookie.class,
                MPJwtGoodMP12ConfigAsSystemProperties_HeaderCookie_withCookieName.class,
                MPJwtGoodMP12ConfigAsSystemProperties_Audiences.class,
                MPJwtGoodMP12ConfigAsSystemProperties_Algorithm.class,
                MPJwtGoodMP12ConfigAsSystemProperties_decryptKeyLoc_RS384RelativeFile.class,
                MPJwtGoodMP12ConfigAsSystemProperties_decryptKeyLoc_RS256Jwk.class,
                MPJwtGoodMP12ConfigAsSystemProperties_decryptKeyLoc_AlternateDecryptSettings.class,
                // mp-config specified as env vars
                MPJwtGoodMP12ConfigAsEnvVars_HeaderAuthorization.class,
                MPJwtGoodMP12ConfigAsEnvVars_HeaderCookie.class,
                MPJwtGoodMP12ConfigAsEnvVars_HeaderCookie_withCookieName.class,
                MPJwtGoodMP12ConfigAsEnvVars_Audiences.class,
                MPJwtGoodMP12ConfigAsEnvVars_Algorithm.class,
                MPJwtGoodMP12ConfigAsEnvVars_decryptKeyLoc_RS256File.class,
                MPJwtGoodMP12ConfigAsEnvVars_decryptKeyLoc_RS512Url.class,
                MPJwtGoodMP12ConfigAsEnvVars_decryptKeyLoc_RS256PlainText.class,
                MPJwtGoodMP12ConfigAsEnvVars_decryptKeyLoc_AlternateDecryptSettings.class,

                // Ensure 1.2 function not available with only 1.1 Feature enabled
                Feature11Enabled_ConfigInAppTests.class,
                Feature11Enabled_ConfigInServerXmlTests.class,
                Feature11Enabled_MpConfigAsEnvVars.class,
                Feature11Enabled_MpConfigAsSystemProperties.class
})

public class FATSuite {

    public static String authHeaderPrefix = MPJwt12FatConstants.TOKEN_TYPE_BEARER;

    /*
     * Right now we'll run with 1.2, but, setting up for future versions
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestFeatureRepeatAction(MPJwt12FatConstants.MP_JWT_12));

    // Example of how to run mpJwt 1.2 tests with 1.2 and 2.0
//    /*
//     * Run the mpJwt 2.0 tests in lite mode ONLY
//     */
//    @ClassRule
//    public static RepeatTests repeat = RepeatTests.with(new SecurityTestFeatureRepeatAction(MPJwt12FatConstants.MP_JWT_12))
//                    .andWith(new SecurityTestFeatureRepeatAction(MPJwt12FatConstants.MP_JWT_20).liteFATOnly());

}
