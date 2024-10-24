/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.PostgreSQLContainer;
import tests.FailoverTestLease;

@RunWith(Suite.class)
@SuiteClasses({
                //Ensure something runs when failover tests are skipped on IBMi
                AlwaysPassesTest.class,
                FailoverTestLease.class
})
public class FATSuite extends TxTestContainerSuite {

    static {
        testContainer = new PostgreSQLContainer(POSTGRES_IMAGE)
                        .withDatabaseName(POSTGRES_DB)
                        .withUsername(POSTGRES_USER)
                        .withPassword(POSTGRES_PASS)
                        .withSSL()
                        .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "postgre-ssl"));

        beforeSuite(DatabaseContainerType.Postgres);
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers(FailoverTestLease.serverNames))
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11)
                                    .forServers(FailoverTestLease.serverNames))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().forServers(FailoverTestLease.serverNames));
}
