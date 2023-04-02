/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.mp.jwt12.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE9RepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
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

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,

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

@SuppressWarnings("restriction")
public class FATSuite {

    public static String authHeaderPrefix = MPJwt11FatConstants.TOKEN_TYPE_BEARER;
    //    private static final Set<String> REMOVE = new HashSet<String>();
    //    private static final Set<String> INSERT = new HashSet<String>();

    /**
     * Tests were written to use repeat to run the tests with each version of the mpJwt feature. Now that the project has been
     * split to run each instance of the feature from a different project, I'd like to remove the use of repeat, but, ...
     * The test tooling is expecting the feature version to be set in the repeat variables. The tooling uses that info to
     * copy/use the proper version of some config files.
     */
    /**
     * mpJwt-2.0 needs EE9 enabled
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestFeatureEE9RepeatAction(MPJwt11FatConstants.MP_JWT_20).alwaysAddFeature("servlet-5.0").forServerConfigPaths("publish/servers", "publish/shared/config"));

}
