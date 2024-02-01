/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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

package com.ibm.ws.jpa.clientcontainer.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                BasicFieldInjectTest.class,
                BasicMethodInjectTest.class,
                BasicJNDIInjectTest.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {
    private static final Class<?> c = FATSuite.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES())
                    .andWith(FeatureReplacementAction.EE11_FEATURES());
//    @ClassRule
//    public static ExternalResource testRule = new ExternalResource() {
//        /**
//         * Creates a client and runs its application
//         */
//        @Override
//        protected void before() throws Exception {
//
//        };
//
//        @Override
//        protected void after() {
//
//        };
//    };

}
