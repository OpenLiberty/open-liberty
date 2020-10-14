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
package com.ibm.ws.security.mp.jwt12.fat.systemPropertiesTests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.mp.jwt12.fat.sharedTests.GenericEnvVarsAndSystemPropertiesTests;
import com.ibm.ws.security.mp.jwt12.fat.utils.MP12ConfigSettings;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as system properties
 * We'll test with a server.xml that will NOT have a mpJwt config, the app will NOT have mp-config specified
 * Therefore, we'll be able to show that the config is coming from the system properties
 * We also test with a conflicting config in server.xml - we'll show that this value overrides the system properties
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtGoodMP12ConfigAsSystemProperties_HeaderAuthorization extends GenericEnvVarsAndSystemPropertiesTests {

    public static Class<?> thisClass = MPJwtGoodMP12ConfigAsSystemProperties_HeaderAuthorization.class;

    @Server("com.ibm.ws.security.mp.jwt.1.2.fat.jvmOptions")
    public static LibertyServer sysPropResourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        commonMpJwt12Setup(sysPropResourceServer, "rs_server_AltConfigNotInApp_good12ServerXmlConfigWithAudiences.xml", MpJwt12FatConstants.AUTHORIZATION,
                           MpJwt12FatConstants.TOKEN_TYPE_BEARER, MP12ConfigSettings.AudiencesNotSet, MP12ConfigSettings.AlgorithmNotSet, MPConfigLocation.SYSTEM_PROP);

    }

    @Test
    public void MPJwtGoodMP12ConfigAsSystemProperties_HeaderAuthorization_test() throws Exception {
        genericGoodTest();
    }

    @Test
    public void MPJwtGoodMP12ConfigAsSystemProperties_HeaderAuthorization_overriddenByServerXml_test() throws Exception {
        genericBadTest("rs_server_AltConfigNotInApp_Header_Cookie.xml", setMissingTokenExpectations(resourceServer));
    }
}
