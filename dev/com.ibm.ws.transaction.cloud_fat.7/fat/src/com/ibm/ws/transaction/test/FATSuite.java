/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({
                DualServerDynamicPostgreSQLTest.class,
                PostgreSQLTest.class,
})
public class FATSuite {

    static final String POSTGRES_DB = "testdb";
    static final String POSTGRES_USER = "postgresUser";
    static final String POSTGRES_PASS = "superSecret";

    public static CustomPostgreSQLContainer<?> postgre = new CustomPostgreSQLContainer<>("postgres:11.2-alpine")
                    .withDatabaseName(POSTGRES_DB)
                    .withUsername(POSTGRES_USER)
                    .withPassword(POSTGRES_PASS)
                    .withConfigOption("max_prepared_transactions", "2")
                    .withLogConsumer(FATSuite::log);

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Allows local tests to switch between using a local docker client, to using a
        // remote docker client.
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();

        postgre.start();
    }

    @AfterClass
    public static void afterSuite() {
        postgre.stop();
    }

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(FATSuite.class, "[postgresql]", msg);
    }

}
