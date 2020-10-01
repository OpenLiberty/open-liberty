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
package com.ibm.ws.security.mp.jwt12.fat.featureSupportTests.feature1_1;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.mp.jwt12.fat.sharedTests.Feature11Enabled_GenericEnvVarsAndSystemPropertiesTests;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as environment variables
 * We'll test that mpJwt-1.2 mp config properties are not used when the mpJwt-1.1 feature is enabled.
 * Test class runs the same tests as the system environment variable tests, so, we'll use a common test
 * class - we'll only use this class to request that the mp properties are set as environment variables
 * and give the test cases unique names.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class Feature11Enabled_MpConfigAsEnvVars extends Feature11Enabled_GenericEnvVarsAndSystemPropertiesTests {

    public static Class<?> thisClass = Feature11Enabled_MpConfigAsEnvVars.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction(MPConfigLocation.ENV_VAR.toString()));

    @Server("com.ibm.ws.security.mp.jwt.1.2.fat")
    public static LibertyServer envVarsResourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        // tell the common tests which server to use and where to put the mp config properties
        // use a server withOUT a jvm.options file and set the mp config properties as environment variables
        commonMpJwt12Setup(envVarsResourceServer, MPConfigLocation.ENV_VAR);

    }

}
