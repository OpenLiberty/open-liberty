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
package com.ibm.ws.security.mp.jwt12.fat.envVarsTests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.mp.jwt12.fat.sharedTests.GenericEnvVarsAndSystemPropertiesTests;

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
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtGooMP12ConfigAsEnvVars_HeaderAuthorization extends GenericEnvVarsAndSystemPropertiesTests {

    public static Class<?> thisClass = MPJwtGooMP12ConfigAsEnvVars_HeaderAuthorization.class;

    @Server("com.ibm.ws.security.mp.jwt.fat")
    public static LibertyServer envVarsResourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        commonSetup(envVarsResourceServer, "rs_server_AltConfigNotInApp_good12ServerXmlConfigWithAudiences.xml", MpJwt12FatConstants.AUTHORIZATION,
                    MpJwt12FatConstants.TOKEN_TYPE_BEARER,
                    "client01, client02", MPConfigLocation.ENV_VAR);

    }

    @Test
    public void MPJwtGooMP12ConfigAsEnvVars_HeaderAuthorization_test() throws Exception {
        genericGoodTest();
    }
}
