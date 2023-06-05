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
package com.ibm.ws.security.mp.jwt21.fat.envVarsTests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP21ConfigSettings;
import com.ibm.ws.security.mp.jwt21.fat.sharedTests.GenericEnvVarsAndSystemPropertiesTests;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as environment variables
 * We'll test with a server.xml that will NOT have a mpJwt config, the app will NOT have mp-config specified
 * Therefore, we'll be able to show that the config is coming from the system properties
 * We also test with a conflicting config in server.xml - we'll show that this value overrides the environment variables
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@SkipForRepeat({ EmptyAction.ID })
public class MPJwtGoodMP21ConfigAsEnvVars_DecryptAlg extends GenericEnvVarsAndSystemPropertiesTests {

    public static Class<?> thisClass = MPJwtGoodMP21ConfigAsEnvVars_DecryptAlg.class;

    @Server("com.ibm.ws.security.mp.jwt.2.1.fat")
    public static LibertyServer envVarsResourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        commonMpJwt21Setup(envVarsResourceServer, "rs_server_AltConfigNotInApp_allowDecryptAlgOverrideServerXmlConfig.xml", MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew, JwtConstants.KEY_MGMT_KEY_ALG_256, MPConfigLocation.ENV_VAR);

    }

    /**
     * show that we'll pick up the system property value for decrypt alg and use that.
     * We'll expect a success since failure since the value of the system property will not match the token used.
     * Showing failure proves that we're using the system property since not coding a value in server.xml would have succeeded
     *
     * @throws Exception
     */
    @Test
    public void MPJwtGoodMP21ConfigAsEnvVars_DecryptAlg_test() throws Exception {
        genericUsePropTest("sign_RS256_enc_RS256", 0, setEncryptAlgMismatchExpectations(resourceServer, JwtConstants.KEY_MGMT_KEY_ALG_256));
    }

    @Test
    public void MPJwtGoodMP21ConfigAsEnvVars_DecryptAlg_overriddenByServerXml_test() throws Exception {
        genericServerXmlOverridePropTest("sign_RS256_enc_RS256", "rs_server_AltConfigNotInApp_disallowDecryptAlgOverrideServerXmlConfig.xml", 0, null);
    }

}
