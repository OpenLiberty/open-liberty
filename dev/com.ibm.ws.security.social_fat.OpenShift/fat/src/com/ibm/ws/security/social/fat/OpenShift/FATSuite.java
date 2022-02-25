/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.OpenShift;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,
        // The next test class is used in automated production testing
        // It tests handling of the tokenreview response json (all other interaction
        // can be tested with a Liberty OP
        OpenShift_StubbedTests_usingSocialConfig.class,
// The following tests need to be run against a real OpenShift environment
// update the properties in the servers bootstrap.properties file to reflect
// the OpenShift setup you'll be testing with
//        OpenShift_BasicTests_usingSocialConfig.class,
//        OpenShift_BasicConfigTests_usingSocialConfig.class
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
