/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.postgresql;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.containers.TestContainerSuite;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;

@RunWith(Suite.class)
@SuiteClasses({
                PostgreSQLTest.class,
                PostgreSQLSSLTest.class,
                PostgreSQLAWSTest.class,
//              ThreadLocalConnectionTest.class
})
public class FATSuite extends TestContainerSuite {

    private static final String POSTGRES_DB = "testdb";
    private static final String POSTGRES_USER = "postgresUser";
    private static final String POSTGRES_PASS = "superSecret";

    @ClassRule
    public static JdbcDatabaseContainer<?> postgre = DatabaseContainerFactory.createType(DatabaseContainerType.Postgres)
                    .withDatabaseName(POSTGRES_DB)
                    .withUsername(POSTGRES_USER)
                    .withPassword(POSTGRES_PASS);
}
