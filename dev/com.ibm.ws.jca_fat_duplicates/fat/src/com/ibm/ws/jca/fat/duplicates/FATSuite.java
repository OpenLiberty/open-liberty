/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jca.fat.duplicates;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({ AlwaysPassesTest.class,
                DuplicateResourceAdaptersTest.class })
public class FATSuite {
    //this tests the EE10 connectors version 2.1 resource adapter and should not be repeated for previous versions
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT())
                    .andWith(FeatureReplacementAction.EE11_FEATURES().setSkipTransformation(true).alwaysAddFeature("servlet-6.1"));
}