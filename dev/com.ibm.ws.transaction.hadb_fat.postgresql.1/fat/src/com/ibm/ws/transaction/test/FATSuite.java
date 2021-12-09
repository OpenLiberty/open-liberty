/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.test.tests.FailoverTest1;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.PostgreSQLContainer;

@RunWith(Suite.class)
@SuiteClasses({ FailoverTest1.class })
public class FATSuite {
    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";
    // Using the RepeatTests @ClassRule will cause all tests to be run three times.
    // First without any modifications, then again with all features upgraded to
    // their EE8 equivalents and finally with the Jakarta EE9 features.
    //
    // In this test we allow one of the flavours of supported database to be selected either through
    // specifying the fat.bucket.db.type property or it is chosen based on the date. That database is
    // used in all 3 runs of the tests against the different version of EE.
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                    .andWith(new JakartaEE9Action().fullFATOnly());

    public static DatabaseContainerType type = DatabaseContainerType.Postgres;
    public static JdbcDatabaseContainer<?> testContainer;

    public static void beforeSuite() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client.
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
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
        Log.info(FATSuite.class, "beforeSuite", "starting test container of type: " + type);
        testContainer.withStartupTimeout(FATUtils.TESTCONTAINER_STARTUP_TIMEOUT).waitingFor(Wait.forLogMessage(".*database system is ready.*", 2)).start();
        Log.info(FATSuite.class, "beforeSuite", "started test container of type: " + type);
    }

    public static void afterSuite() {
        Log.info(FATSuite.class, "afterSuite", "stop test container");
        testContainer.stop();
    }
}