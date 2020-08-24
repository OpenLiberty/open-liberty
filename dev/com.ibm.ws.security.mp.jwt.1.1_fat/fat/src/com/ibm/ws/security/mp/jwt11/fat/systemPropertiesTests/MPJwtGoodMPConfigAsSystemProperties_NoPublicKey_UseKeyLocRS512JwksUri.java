/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat.systemPropertiesTests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.sharedTests.MPJwtWithGoodAltSigAlgMPConfig;
import com.ibm.ws.security.mp.jwt11.fat.utils.MPConfigSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as environment variables.
 * We'll test with a server.xml that will NOT have a mpJwt config, the app will NOT have mp-config specified
 * Therefore, we'll be able to show that the config is coming from the environment variables
 *
 * (we're just proving that we can obtain the mp-config via environment variables. It's easier/quicker
 * to test the behavior of each config attribute by setting them in an app (environment variables would
 * require a different server for each config setting).
 **/

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocRS512JwksUri extends MPJwtWithGoodAltSigAlgMPConfig {

    public static Class<?> thisClass = MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocRS512JwksUri.class;

    @BeforeClass
    public static void setUp() throws Exception {

        String sigAlg = MpJwtFatConstants.SIGALG_RS512;
        commonSetup(sigAlg, JwksUriFlag, MPConfigSettings.PublicKeyNotSet, MPConfigLocation.ENV_VAR);

    }

    @Test
    public void MPJwtGoodMPConfigAsEnvVars_NoPublicKey_UseKeyLocES512JwksUri_test() throws Exception {
        genericGoodTest();
    }
}
