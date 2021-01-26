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

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;
import componenttest.topology.database.container.PostgreSQLContainer;

@RunWith(Suite.class)
@SuiteClasses({
                DualServerDynamicPostgreSQLTest.class,
                PostgreSQLTest.class,
})
public class FATSuite {

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    static final String POSTGRES_DB = "testdb";
    static final String POSTGRES_USER = "postgresUser";
    static final String POSTGRES_PASS = "superSecret";

    @ClassRule
    public static PostgreSQLContainer postgre = new PostgreSQLContainer("postgres:11.2-alpine")
                    .withDatabaseName(POSTGRES_DB)
                    .withUsername(POSTGRES_USER)
                    .withPassword(POSTGRES_PASS)
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "postgre"));

}
