/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerType;
import tests.DBRotationTest;

@RunWith(Suite.class)
@SuiteClasses({
	//Ensure failures in @BeforeClass do not result in zero tests run
	AlwaysPassesTest.class,
	DBRotationTest.class,
})
public class FATSuite extends TxTestContainerSuite {

	static {
		DBRotationTest.setDerby();
		beforeSuite(DatabaseContainerType.Derby);
	}

	@ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers(DBRotationTest.serverNames))
    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers(DBRotationTest.serverNames))
    .andWith(FeatureReplacementAction.EE10_FEATURES().forServers(DBRotationTest.serverNames));
}
