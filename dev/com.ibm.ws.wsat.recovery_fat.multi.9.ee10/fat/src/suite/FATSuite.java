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
import tests.ReroutePeerRecoveryTest;

@RunWith(Suite.class)
@SuiteClasses({
	ReroutePeerRecoveryTest.class,
})
public class FATSuite extends TxTestContainerSuite {

	static {
	    testContainer = new PostgreSQLContainer(TxTestContainerSuite.POSTGRES_SSL)
	                    .withDatabaseName(TxTestContainerSuite.POSTGRES_DB)
	                    .withUsername(TxTestContainerSuite.POSTGRES_USER)
	                    .withPassword(TxTestContainerSuite.POSTGRES_PASS)
	                    .withSSL()
	                    .withLogConsumer(new SimpleLogConsumer(ReroutePeerRecoveryTest.class, "postgre-ssl"));

        beforeSuite(DatabaseContainerType.Postgres);
	}

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE10_FEATURES());
}
