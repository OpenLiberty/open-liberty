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

import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP11ConfigSettings;
import com.ibm.ws.security.mp.jwt11.fat.sharedTests.MPJwtWithGoodAltSigAlgMPConfig;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

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

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocES512RelativeFile extends MPJwtWithGoodAltSigAlgMPConfig {

    public static Class<?> thisClass = MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocES512RelativeFile.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat.jvmOptions")
    public static LibertyServer sysPropResourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        String sigAlg = MPJwt11FatConstants.SIGALG_ES512;
        commonSetup(sysPropResourceServer, sigAlg, JwtKeyTools.getPublicKeyFileNameForAlg(sigAlg), MP11ConfigSettings.PublicKeyNotSet, MPConfigLocation.SYSTEM_PROP);

    }

    @Test
    public void MPJwtGoodMPConfigAsSystemProperties_NoPublicKey_UseKeyLocES512RelativeFile_test() throws Exception {
        genericGoodTest();
    }
}
