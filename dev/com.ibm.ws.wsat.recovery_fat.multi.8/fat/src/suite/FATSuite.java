/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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

import componenttest.containers.SimpleLogConsumer;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.PostgreSQLContainer;
import tests.DBRerouteRecoveryTest;

@RunWith(Suite.class)
@SuiteClasses({
	DBRerouteRecoveryTest.class,
})
public class FATSuite extends TxTestContainerSuite {

	static {
	    testContainer = new PostgreSQLContainer(TxTestContainerSuite.POSTGRES_SSL)
	                    .withDatabaseName(TxTestContainerSuite.POSTGRES_DB)
	                    .withUsername(TxTestContainerSuite.POSTGRES_USER)
	                    .withPassword(TxTestContainerSuite.POSTGRES_PASS)
	                    .withSSL()
	                    .withLogConsumer(new SimpleLogConsumer(DBRerouteRecoveryTest.class, "postgre-ssl"));

        beforeSuite(DatabaseContainerType.Postgres);
	}

	@ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE8_FEATURES())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());
}
