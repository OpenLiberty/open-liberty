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

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

        AlwaysPassesTest.class,

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
