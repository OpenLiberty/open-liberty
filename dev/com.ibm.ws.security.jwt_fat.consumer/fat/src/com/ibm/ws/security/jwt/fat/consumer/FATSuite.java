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
package com.ibm.ws.security.jwt.fat.consumer;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE10RepeatAction;
import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE9RepeatAction;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        // Ported list of tests (some already renamed)
        AlwaysPassesTest.class,
        JwtConsumerApiBasicTests.class,
        JwtConsumerApiConfigTests.class,
        JwtConsumerApiConfigBlankIdTests.class,
        JwtConsumerApiConfigWithGlobalTrustTests.class,
        JwtConsumerAPIMinimumHSARunnableConfigTests.class,
        JwtConsumerAPIMinimumSSLConsumerConfigTests.class,
        JwtConsumerAPIMinimumSSLGlobalConfigTests.class,

})

public class FATSuite {

    /*
     * Run EE9 and EE10 tests in LITE mode (but not on Windows) and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
            .andWith(new SecurityTestRepeatAction().onlyOnWindows().liteFATOnly())
            .andWith(new SecurityTestFeatureEE9RepeatAction().notOnWindows().forServerConfigPaths("publish/servers", "publish/shared/config").liteFATOnly())
            .andWith(new SecurityTestFeatureEE10RepeatAction().notOnWindows().forServerConfigPaths("publish/servers", "publish/shared/config").liteFATOnly());

}
