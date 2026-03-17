/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.examplera.anno;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({ AlwaysPassesTest.class,
                ResourceAdapterExampleTest.class })
public class FATSuite {

    //this tests the EE10 connectors version 2.1 added generic support for MappedRecord, IndexedRecord and should not be repeated for previous versions
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT())
                    .andWith(FeatureReplacementAction.EE11_FEATURES().setSkipTransformation(true));

}
