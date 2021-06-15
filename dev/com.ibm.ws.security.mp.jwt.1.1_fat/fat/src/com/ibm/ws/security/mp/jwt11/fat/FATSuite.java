/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;
import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureRepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
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
import com.ibm.ws.security.mp.jwt11.fat.propagationTests.MPJwtPropagationTests_notUsingWebTarget;
import com.ibm.ws.security.mp.jwt11.fat.propagationTests.MPJwtPropagationTests_usingWebTarget;
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

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                // Ported list of tests (some already renamed)
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
                MPJwtPropagationTests_usingWebTarget.class,
                MPJwtPropagationTests_notUsingWebTarget.class,
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

                MPJwtJwkTokenCacheTests.class

// the mpJwt-1.2 project contains tests that validate the behavior of 1.2
// it will run these same tests, but enable the 1.2 feature instead of the 1.1 feature
})

public class FATSuite {

    public static String authHeaderPrefix = MPJwt11FatConstants.TOKEN_TYPE_BEARER;

    /*
     * Run the mpJwt 1.2 tests in lite mode ONLY
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestFeatureRepeatAction(MPJwt11FatConstants.MP_JWT_11))
                    .andWith(new SecurityTestFeatureRepeatAction(MPJwt11FatConstants.MP_JWT_12).liteFATOnly());

}
