/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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
package com.ibm.ws.request.timing.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                SlowRequestTiming.class,
                TimingRequestTiming.class
})

public class FATSuite {
    // Using the RepeatTests @ClassRule in FATSuite will cause all tests in the FAT to be run twice.
    // First without any modifications, then again with all features in all server.xml's upgraded to their EE8/EE9 equivalents.
    // Some corner case tests are skipped for repeating, as its not necessary to repeat these tests using the newer features, since
    // some basic functionality test cases that use common code are already being repeated.
    // Ensure, the corner case scenario test cases are skipped for repeat, when adding another RepeatAction below, to save build and test time.
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly()).andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly());
}