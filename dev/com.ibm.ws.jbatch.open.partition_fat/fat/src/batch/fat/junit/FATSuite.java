/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package batch.fat.junit;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.H2Database;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 * s
 */
@SuiteClasses({
                AlwaysPassesTest.class,
                ParallelContextPropagationTest.class,
                PartitionMetricsTest.class,
                PartitionReducerTest.class
})
public class FATSuite extends TestContainerSuite {
   @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES());

    private static H2Database h2Database = H2Database.create("dbuser1", "dbpwd1")
                    .withDatabaseName("BatchDB")
                    .withConfig("INIT", "CREATE SCHEMA IF NOT EXISTS JBATCH")
                    .withConfig("NON_KEYWORDS", "VALUE");

    @ClassRule
    public static JdbcDatabaseContainer<?> jdbcContainer = DatabaseContainerFactory.createH2(Optional.of(h2Database));

    @BeforeClass
    public static void setupDatabase() {
        // Create JBATCH schema for Postgres (H2 uses INIT config in URL)
        if (DatabaseContainerType.valueOf(jdbcContainer) == DatabaseContainerType.Postgres) {
            try (Connection con = jdbcContainer.createConnection(""); Statement stmt = con.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS JBATCH");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
