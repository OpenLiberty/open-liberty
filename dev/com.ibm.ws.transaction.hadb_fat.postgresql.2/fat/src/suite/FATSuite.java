/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
import tests.FailoverTest2;

@RunWith(Suite.class)
@SuiteClasses({ FailoverTest2.class })
public class FATSuite extends TxTestContainerSuite {
    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";

    static {
        databaseContainerType = DatabaseContainerType.Postgres;

        /*
         * The image here is generated using the Dockerfile in com.ibm.ws.jdbc_fat_postgresql/publish/files/postgresql-ssl
         * The command used in that directory was: docker build -t jonhawkes/postgresql-ssl:1.0 .
         * With the resulting image being pushed to docker hub.
         */
        testContainer = new PostgreSQLContainer("jonhawkes/postgresql-ssl:1.0")
                        .withDatabaseName(POSTGRES_DB)
                        .withUsername(POSTGRES_USER)
                        .withPassword(POSTGRES_PASS)
                        .withSSL()
                        .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "postgre-ssl"));

        beforeSuite();
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers(FailoverTest2.serverNames))
                    .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers(FailoverTest2.serverNames))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().forServers(FailoverTest2.serverNames));
}