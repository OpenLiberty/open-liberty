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

package com.ibm.ws.security.backchannelLogout.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,
        HttpMethodsTests.class,
        LogoutTokenValidationTests.class,
        BasicBCLTests.class,
        MultiServerBCLTests.class

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    /*
     * Run EE9 tests in only FULL mode and run EE7/EE8 tests only in LITE mode.
     *
     * This was done to increase coverage of EE9 while not adding a large amount of of test runtime.
     */
    @ClassRule
    public static RepeatTests repeat = LargeProjectRepeatActions.createEE9OrEE10Repeats("servlet-5.0", "servlet-6.0");

}
