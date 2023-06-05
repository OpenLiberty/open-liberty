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
package com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.sharedTests;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.mp.jwt21.fat.sharedTests.GenericEnvVarsAndSystemPropertiesTests;

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
public class OlderMpJwtFeatures_GenericEnvVarsAndSystemPropertiesTests_TokenAge extends GenericEnvVarsAndSystemPropertiesTests {

    public static Class<?> thisClass = OlderMpJwtFeatures_GenericEnvVarsAndSystemPropertiesTests_TokenAge.class;

    @Server("com.ibm.ws.security.mp.jwt.2.1.fat.jvmOptions")
    public static LibertyServer sysPropResourceServer;

    /**
     * show that we'll pick up the system property value for token age and use that.
     * We'll expect a failure trying to access the app since we only allow a token to be valid for 1 second and we sleep for 5
     * seconds before we try to use it.
     *
     * @throws Exception
     */
    @Test
    public void OlderMpJwtFeatures_GenericEnvVarsAndSystemPropertiesTests_TokenAge_test() throws Exception {
        genericUsePropTest(5, null);
    }

}
