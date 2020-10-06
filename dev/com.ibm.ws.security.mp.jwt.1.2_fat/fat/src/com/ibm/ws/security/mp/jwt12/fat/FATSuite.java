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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.mp.jwt11.fat.MPJwtApplicationAndSessionScopedClaimInjectionTests;
import com.ibm.ws.security.mp.jwt11.fat.MPJwtBasicTests;
import com.ibm.ws.security.mp.jwt11.fat.MPJwtConfigUsingBuilderTests;
import com.ibm.ws.security.mp.jwt11.fat.MPJwtLoginConfig_ignoreApplicationAuthMethodFalseTests;
import com.ibm.ws.security.mp.jwt11.fat.MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_BadIssuerMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_BadJwksUriMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtBadMPConfigAsEnvVars;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseJwksUri_JWK;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseJwksUri_X509;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLoc;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLocES256RelativeFile;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLocES512JwksUri;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLocRS384Url;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLocRS512File;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtGoodMPConfigAsEnvVars_UseES384PublicKey_NoKeyLoc;
import com.ibm.ws.security.mp.jwt11.fat.envVarsTests.MPJwtGoodMPConfigAsEnvVars_UsePublicKey_NoKeyLoc;
import com.ibm.ws.security.mp.jwt11.fat.featureSupportTests.MPJwtNoMpJwtConfig;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtBadMPConfigAsSystemProperties;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseJwksUri_JWK;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseJwksUri_X509;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLoc;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocES2564Url;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocES384File;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocES512RelativeFile;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocRS512JwksUri;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtGoodMPConfigAsSystemProperties_UsePublicKey_NoKeyLoc;
import com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests.MPJwtGoodMPConfigAsSystemProperties_UseRS384PublicKey_NoKeyLoc;
import com.ibm.ws.security.mp.jwt12.fat.configInAppTests.MPJwt12MPConfigInApp_Tests;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_Algorithm;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_Audiences;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_HeaderAuthorization;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_HeaderCookie;
import com.ibm.ws.security.mp.jwt12.fat.envVarsTests.MPJwtGoodMP12ConfigAsEnvVars_HeaderCookie_withCookieName;
import com.ibm.ws.security.mp.jwt12.fat.featureSupportTests.feature1_1.Feature11Enabled_ConfigInAppTests;
import com.ibm.ws.security.mp.jwt12.fat.featureSupportTests.feature1_1.Feature11Enabled_ConfigInServerXmlTests;
import com.ibm.ws.security.mp.jwt12.fat.featureSupportTests.feature1_1.Feature11Enabled_MpConfigAsEnvVars;
import com.ibm.ws.security.mp.jwt12.fat.featureSupportTests.feature1_1.Feature11Enabled_MpConfigAsSystemProperties;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_Algorithm;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_Audiences;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_HeaderAuthorization;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_HeaderCookie;
import com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests.MPJwtGoodMP12ConfigAsSystemProperties_HeaderCookie_withCookieName;

@RunWith(Suite.class)
@SuiteClasses({
                // 1.1 tests running with 1.2 feature

                AlwaysRunAndPassTest.class,
                // Basic Functional tests
                // -- These tests will run 3 times - tests that pass the token in the Authorization header
                // -- will run using "Bearer <token>", "Token <token>", and "misc <token>" - the use of
                // -- the config attribute authorizationHeaderPrefix will tell runtime what prefix to look for
                MPJwtBasicTests.class,
                // More targeted tests
                MPJwtConfigUsingBuilderTests.class,
                MPJwtApplicationAndSessionScopedClaimInjectionTests.class,
                MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests.class,
                MPJwtLoginConfig_ignoreApplicationAuthMethodFalseTests.class,
                MPJwtNoMpJwtConfig.class,
                // propagation tests
                //TODO - broken with 1.2 at the moment...
//                MPJwtPropagationTests_usingWebTarget.class,
//                MPJwtPropagationTests_notUsingWebTarget.class,
                // mp-config specified in the applications
                MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_Tests.class,
                MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_Tests.class,
                MPJwtMPConfigInApp_BadIssuerMPJwtConfigInServerXml_Tests.class,
                MPJwtMPConfigInApp_BadJwksUriMPJwtConfigInServerXml_Tests.class,
                MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_Tests.class,
                MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_Tests.class,
                // mp-config specified as system properties
                MPJwtGoodMPConfigAsSystemProperties_UsePublicKey_NoKeyLoc.class,
                MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLoc.class,
                MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseJwksUri_X509.class,
                MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseJwksUri_JWK.class,
                MPJwtBadMPConfigAsSystemProperties.class,
                MPJwtGoodMPConfigAsSystemProperties_UseRS384PublicKey_NoKeyLoc.class,
                MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocES2564Url.class,
                MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocES384File.class,
                MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocES512RelativeFile.class,
                MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocRS512JwksUri.class,
                // mp-config specified as env vars
                MPJwtGoodMPConfigAsEnvVars_UsePublicKey_NoKeyLoc.class,
                MPJwtGoodMPConfigAsEnvVars_UseES384PublicKey_NoKeyLoc.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLocRS384Url.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLocRS512File.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLocES256RelativeFile.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLocES512JwksUri.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLoc.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseJwksUri_X509.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseJwksUri_JWK.class,
                MPJwtBadMPConfigAsEnvVars.class,

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
                // mp-config specified as env vars
                MPJwtGoodMP12ConfigAsEnvVars_HeaderAuthorization.class,
                MPJwtGoodMP12ConfigAsEnvVars_HeaderCookie.class,
                MPJwtGoodMP12ConfigAsEnvVars_HeaderCookie_withCookieName.class,
                MPJwtGoodMP12ConfigAsEnvVars_Audiences.class,
                MPJwtGoodMP12ConfigAsEnvVars_Algorithm.class,

                // Ensure 1.2 function not available with only 1.1 Feature enabled
                Feature11Enabled_ConfigInAppTests.class,
                Feature11Enabled_ConfigInServerXmlTests.class,
                Feature11Enabled_MpConfigAsEnvVars.class,
                Feature11Enabled_MpConfigAsSystemProperties.class
})

public class FATSuite {

    public static String authHeaderPrefix = MpJwt12FatConstants.TOKEN_TYPE_BEARER;

}
