/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.social.fat.okdServiceLogin;

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

        // run with a fake/stubbed OpenShift server (tests all the affected function within Liberty)
        OKDServiceLogin_MinimumConfigTests_StubbedServer.class,
        OKDServiceLogin_BasicTests_StubbedServer.class,
        OKDServiceLogin_SSLTests_StubbedServer.class,
// run with a real OpenShift
// these tests are commented out for automated runs
// to run locally:
// 1) uncomment the following tests
// 2) update the properties in bootstrap.properties to reflect the OpenShift instance that you'll be using
//        OKDServiceLogin_MinimumConfigTests_OpenShiftServer.class,
//        OKDServiceLogin_BasicTests_OpenShiftServer.class,
//        OKDServiceLogin_SSLTests_OpenShiftServer.class
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    public static String OKDService = "Stub";

    /*
     * Run EE10 tests in LITE mode and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = LargeProjectRepeatActions.createEE9OrEE10Repeats("servlet-5.0", "servlet-6.0");

}
