/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import tests.DualServerDynamicFSTest2;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                DualServerDynamicFSTest2.class,
})
public class FATSuite {
//    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers(DualServerDynamicFSTest2.serverNames))
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11)
                                    .forServers(DualServerDynamicFSTest2.serverNames))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().forServers(DualServerDynamicFSTest2.serverNames));
}
