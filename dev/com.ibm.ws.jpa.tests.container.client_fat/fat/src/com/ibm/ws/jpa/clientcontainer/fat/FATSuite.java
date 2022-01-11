/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.clientcontainer.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

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
