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
import com.ibm.ws.security.mp.jwt11.fat.utils.MP11ConfigSettings;

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
public class MPJwtGoodMPConfigAsSystemProperties_UseRS384PublicKey_NoKeyLoc extends MPJwtWithGoodAltSigAlgMPConfig {

    public static Class<?> thisClass = MPJwtGoodMPConfigAsSystemProperties_UseRS384PublicKey_NoKeyLoc.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat.jvmOptions")
    public static LibertyServer sysPropResourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        String sigAlg = MpJwtFatConstants.SIGALG_RS384;
        commonSetup(sysPropResourceServer, sigAlg, MP11ConfigSettings.PublicKeyLocationNotSet, MP11ConfigSettings.getComplexKeyForSigAlg(sysPropResourceServer, sigAlg),
                    MPConfigLocation.SYSTEM_PROP);

    }

    @Test
    public void MPJwtGoodMPConfigAsSystemProperties_UseRS384PublicKey_NoKeyLoc_test() throws Exception {
        genericGoodTest();
    }
}
