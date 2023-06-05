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

import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE9RepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
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

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

        AlwaysPassesTest.class,

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

})

@SuppressWarnings("restriction")
public class FATSuite {

    public static String authHeaderPrefix = MPJwt11FatConstants.TOKEN_TYPE_BEARER;

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
