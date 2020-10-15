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

import componenttest.topology.database.container.PostgreSQLContainer;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;
import componenttest.topology.utils.SimpleLogConsumer;

@RunWith(Suite.class)
@SuiteClasses({
                DualServerDynamicPostgreSQLTest.class,
                PostgreSQLTest.class,
})
public class FATSuite {

    static final String POSTGRES_DB = "testdb";
    static final String POSTGRES_USER = "postgresUser";
    static final String POSTGRES_PASS = "superSecret";

    public static PostgreSQLContainer postgre = new PostgreSQLContainer("postgres:11.2-alpine")
                    .withDatabaseName(POSTGRES_DB)
                    .withUsername(POSTGRES_USER)
                    .withPassword(POSTGRES_PASS)
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "postgre"));

    @BeforeClass
    public static void setUp() throws Exception {
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
        postgre.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        postgre.stop();
    }

}
