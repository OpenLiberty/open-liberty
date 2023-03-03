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
package com.ibm.ws.security.mp.jwt21.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE10RepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt21FatConstants;
import com.ibm.ws.security.mp.jwt21.fat.configInAppTests.MPJwt21MPConfigInApp_Tests;
import com.ibm.ws.security.mp.jwt21.fat.envVarsTests.MPJwtGoodMP21ConfigAsEnvVars_ClockSkew;
import com.ibm.ws.security.mp.jwt21.fat.envVarsTests.MPJwtGoodMP21ConfigAsEnvVars_DecryptAlg;
import com.ibm.ws.security.mp.jwt21.fat.envVarsTests.MPJwtGoodMP21ConfigAsEnvVars_TokenAge;
import com.ibm.ws.security.mp.jwt21.fat.systemPropertiesTests.MPJwtGoodMP21ConfigAsSystemProperties_ClockSkew;
import com.ibm.ws.security.mp.jwt21.fat.systemPropertiesTests.MPJwtGoodMP21ConfigAsSystemProperties_DecryptAlg;
import com.ibm.ws.security.mp.jwt21.fat.systemPropertiesTests.MPJwtGoodMP21ConfigAsSystemProperties_TokenAge;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@SuppressWarnings({ "restriction" })
@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,

        // 2.1 tests
        MPJwt21ConfigUsingBuilderTests.class,
        // mp-config specified in applications
        MPJwt21MPConfigInApp_Tests.class,
        //         mp-config specified as system properties
        MPJwtGoodMP21ConfigAsSystemProperties_TokenAge.class,
        MPJwtGoodMP21ConfigAsSystemProperties_ClockSkew.class,
        MPJwtGoodMP21ConfigAsSystemProperties_DecryptAlg.class,
        // mp-config specified as env vars
        MPJwtGoodMP21ConfigAsEnvVars_TokenAge.class,
        MPJwtGoodMP21ConfigAsEnvVars_ClockSkew.class,
        MPJwtGoodMP21ConfigAsEnvVars_DecryptAlg.class,

})

public class FATSuite {

    public static String authHeaderPrefix = MPJwt21FatConstants.TOKEN_TYPE_BEARER;

    /**
     * Tests were written to use repeat to run the tests with each version of the mpJwt feature. Now that the project has been
     * split to run each instance of the feature from a different project, I'd like to remove the use of repeat, but, ...
     * The test tooling is expecting the feature version to be set in the repeat variables. The tooling uses that info to
     * copy/use the proper version of some config files.
     */
    /**
     * mpJwt 1.1 and-1.2 can NOT have EE9/EE10 enabled (and we need to test behavior of new attrs with the old feature levels)
     * mpJwt 2.0 needs EE9 enabled
     * mpJwt 2.0 needs EE10 enabled
     * I will create new projects to handle testing with previous versions of mpJwt and the new attributes (since the EE*
     * transforms cause problems with 1.1 and 1.2)
     */
    /**
     * Adding a repeat with no modification - when we're running on Java 8, we can't enable EE10 which this project needs. No
     * tests will run and the tooling
     * will report 0 tests run which is not allowed. Adding the no modification pass so that AlwaysRunAndPass will at least run
     * once. All of the other
     * tests listed above will use a skip rule to NOT run in the no modification repeat.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestFeatureEE10RepeatAction(MPJwt21FatConstants.MP_JWT_21).forServerConfigPaths("publish/servers", "publish/shared/config").alwaysAddFeature("servlet-6.0")).andWithoutModification();

}
