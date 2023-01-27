/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.security.mp.jwt21.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;
import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE10RepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt21FatConstants;
import com.ibm.ws.security.mp.jwt21.fat.configInAppTests.MPJwt21MPConfigInApp_Tests;
import com.ibm.ws.security.mp.jwt21.fat.envVarsTests.MPJwtGoodMP21ConfigAsEnvVars_ClockSkew;
import com.ibm.ws.security.mp.jwt21.fat.envVarsTests.MPJwtGoodMP21ConfigAsEnvVars_TokenAge;
import com.ibm.ws.security.mp.jwt21.fat.systemPropertiesTests.MPJwtGoodMP21ConfigAsSystemProperties_ClockSkew;
import com.ibm.ws.security.mp.jwt21.fat.systemPropertiesTests.MPJwtGoodMP21ConfigAsSystemProperties_TokenAge;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        AlwaysRunAndPassTest.class,

        // 2.1 tests
        MPJwt21ConfigUsingBuilderTests.class,
        // mp-config specified in applications
        MPJwt21MPConfigInApp_Tests.class,
        //         mp-config specified as system properties
        MPJwtGoodMP21ConfigAsSystemProperties_TokenAge.class,
        MPJwtGoodMP21ConfigAsSystemProperties_ClockSkew.class,
        // TODO keyManagementKeyAlias
        // mp-config specified as env vars
        MPJwtGoodMP21ConfigAsEnvVars_TokenAge.class,
        MPJwtGoodMP21ConfigAsEnvVars_ClockSkew.class,
// TODO keyManagementKeyAlias tests
//        //
//        // Ensure 2.1 function not available with only 1.1 Feature enabled
//        Feature11Enabled_ConfigInAppTests.class,
//        Feature11Enabled_ConfigInServerXmlTests.class,
//        Feature11Enabled_MpConfigAsEnvVars.class,
//        Feature11Enabled_MpConfigAsSystemProperties.class

//      // Ensure 2.1 function not available with only 1.2 Feature enabled
//      Feature12Enabled_ConfigInAppTests.class,
//      Feature12Enabled_ConfigInServerXmlTests.class,
//      Feature12Enabled_MpConfigAsEnvVars.class,
//      Feature12Enabled_MpConfigAsSystemProperties.class

//      // Ensure 2.1 function not available with only 2.0 Feature enabled
//      Feature20Enabled_ConfigInAppTests.class,
//      Feature20Enabled_ConfigInServerXmlTests.class,
//      Feature20Enabled_MpConfigAsEnvVars.class,
//      Feature20Enabled_MpConfigAsSystemProperties.class

})

public class FATSuite {

    public static String authHeaderPrefix = MPJwt21FatConstants.TOKEN_TYPE_BEARER;
    //    private static final Set<String> REMOVE = new HashSet<String>();
    //    private static final Set<String> INSERT = new HashSet<String>();

    /**
     * Tests were written to use repeat to run the tests with each version of the mpJwt feature. Now that the project has been
     * split to run each instance of the feature from a different project, I'd like to remove the use of repeat, but, ...
     * The test tooling is expecting the feature version to be set in the repeat variables. The tooling uses that info to
     * copy/use the proper version of some config files.
     */
    /**
     * mpJwt 1.1 and-1.2 can NOT have EE9 enabled (and we need to test behavior of new attrs with the old feature levels)
     * mpJwt 2.0 and-2.1 need EE9 enabled
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestFeatureEE10RepeatAction(MPJwt21FatConstants.MP_JWT_21).forServerConfigPaths("publish/servers", "publish/shared/config"));

}
