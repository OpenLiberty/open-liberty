/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;

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
                MPJwtJDKTests.class,
                MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests.class,
                MPJwtLoginConfig_ignoreApplicationAuthMethodFalseTests.class,
                MPJwtNoMpJwtConfig.class,
                MPJwtPropagationTests_usingWebTarget.class,
                MPJwtPropagationTests_notUsingWebTarget.class,
                // mp-config specified in the applications
                MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_Tests.class,
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
                // mp-config specified as env vars
                MPJwtGoodMPConfigAsEnvVars_UsePublicKey_NoKeyLoc.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLoc.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseJwksUri_X509.class,
                MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseJwksUri_JWK.class,
                MPJwtBadMPConfigAsEnvVars.class

})

public class FATSuite {

    public static String authHeaderPrefix = MpJwtFatConstants.TOKEN_TYPE_BEARER;

}
