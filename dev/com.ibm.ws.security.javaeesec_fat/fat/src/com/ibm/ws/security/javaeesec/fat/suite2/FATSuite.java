/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat.suite2;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.*;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                CustomIdentityStoreHandlerTest.class,
                EJBModuleRealmTest.class,
                MultipleModuleDBRunAsTest.class,
                MultipleModuleGlobalClientCertTest.class,
                MultipleModuleRunAsTest.class
                /* This test list is short, but they are long running in FULL mode. */
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    /*
     * Run EE9 tests in LITE mode if Java 8, EE10 tests in LITE mode if >= Java 11, EE11 tests in LITE mode if >= Java 17 and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new JakartaEE9Action().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11)).andWith(new JakartaEE10Action().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17)).andWith(FeatureReplacementAction.EE11_FEATURES());
}
