/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package suite.derby.suite2;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;
import com.ibm.ws.transaction.fat.util.TxTestDB;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerType;
import tests.DualServerDynamicDBRotationTest2;

@RunWith(Suite.class)
@SuiteClasses({
	//Ensure failures in @BeforeClass don't prevent zero tests run
	AlwaysPassesTest.class,
	DualServerDynamicDBRotationTest2.class,
})
public class FATSuite extends TxTestContainerSuite {

	@ClassRule
    public static TxTestDB p = new TxTestDB(DatabaseContainerType.Derby);

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers(DualServerDynamicDBRotationTest2.serverNames))
    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers(DualServerDynamicDBRotationTest2.serverNames))
    .andWith(FeatureReplacementAction.EE10_FEATURES().forServers(DualServerDynamicDBRotationTest2.serverNames));
}
