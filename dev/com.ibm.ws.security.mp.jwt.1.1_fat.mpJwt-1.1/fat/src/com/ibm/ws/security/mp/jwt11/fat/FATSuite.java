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
package com.ibm.ws.security.mp.jwt11.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_BadIssuerMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_BadJwksUriMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.configInAppTests.MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_Tests;
import com.ibm.ws.security.mp.jwt11.fat.featureSupportTests.MPJwtNoMpJwtConfig;
import com.ibm.ws.security.mp.jwt11.fat.propagationTests.MPJwtPropagationTests_notUsingWebTarget;
import com.ibm.ws.security.mp.jwt11.fat.propagationTests.MPJwtPropagationTests_usingWebTarget;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

        AlwaysPassesTest.class,
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

        // mp-config specified as system properties - run in project com.ibm.ws.security.mp.jwt.1.1_fat.mpJwt-1.1.sysProps

        // mp-config specified as env vars - run in project run in project com.ibm.ws.security.mp.jwt.1.1_fat.mpJwt-1.1.envVars

        MPJwtJwkTokenCacheTests.class

})

public class FATSuite {

    public static String authHeaderPrefix = MPJwt11FatConstants.TOKEN_TYPE_BEARER;

    /**
     * Tests were written to use repeat to run the tests with each version of the mpJwt feature. Now that the project has been
     * split to run each instance of the feature from a different project, I'd like to remove the use of repeat, but, ...
     * The test tooling is expecting the feature version to be set in the repeat variables. The tooling uses that info to
     * copy/use the proper version of some config files.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(MPJwt11FatConstants.MP_JWT_11));

}
