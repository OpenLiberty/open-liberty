/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
import com.ibm.ws.security.mp.jwt21.fat.sharedTests.GenericEnvVarsAndSystemPropertiesTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config defined as system properties
 * We'll test with a server.xml that will NOT have a mpJwt config, the app will NOT have mp-config specified
 * Therefore, we'll be able to show that the config is coming from the system properties
 * We also test with a conflicting config in server.xml - we'll show that this value overrides the system properties
 *
 **/

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OlderMpJwtFeatures_GenericEnvVarsAndSystemPropertiesTests_DecryptAlg extends GenericEnvVarsAndSystemPropertiesTests {

    public static Class<?> thisClass = OlderMpJwtFeatures_GenericEnvVarsAndSystemPropertiesTests_DecryptAlg.class;

    public static LibertyServer resourceServer;

    /**
     * show that we'll pick up the system property value for decrypt alg and use that.
     * We'll expect a success since failure since the value of the system property will not match the token used.
     * Showing failure proves that we're using the system property since not coding a value in server.xml would have succeeded
     *
     * @throws Exception
     */
    @Test
    public void OlderMpJwtFeatures_GenericEnvVarsAndSystemPropertiesTests_DecryptAlg_test() throws Exception {

        if (RepeatTestFilter.getRepeatActionsAsString().contains(MPJwt11FatConstants.MP_JWT_11)) {
            genericUsePropTest("sign_RS256_enc_RS256", 0, setOnlyJWSAcceptedExpectations(resourceServer));
        } else {
            genericUsePropTest("sign_RS256_enc_RS256", 0, null);
        }
    }

}
