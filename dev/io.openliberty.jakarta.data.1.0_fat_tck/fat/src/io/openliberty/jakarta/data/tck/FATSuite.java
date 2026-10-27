/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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
package io.openliberty.jakarta.data.tck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //Need to have a passing test for Java 8, 11
                DataCoreTckLauncher.class,
                DataWebTckLauncher.class,
                DataFullTckLauncher.class, //full mode
                DataStandaloneTckLauncher.class
})
public class FATSuite extends TestContainerSuite {

    private static final DockerImageName mongoDBImage = DockerImageName //
                    .parse("public.ecr.aws/docker/library/mongo:8.0")
                    .asCompatibleSubstituteFor("mongo:8.0");

    public static String JNOSQL_VERSION = "1.1.12";

    @ClassRule
    public static JdbcDatabaseContainer<?> relationalDatabase = DatabaseContainerFactory.create();

    @ClassRule
    public static MongoDBContainer noSQLDatabase = new MongoDBContainer(mongoDBImage);

    public static boolean shouldRunSignatureTests() {
        /*
         * The Jakarta Data 1.0 TCK uses org.jboss.shrinkwrap.resolver.api.maven.Maven
         * resolver to append the signature plugin to the signature test artifact
         * before it is deployed to the server.
         * The Shrinkwrap Maven resolver does not honor custom repository settings.xml
         * and pulls directly from Maven Central leading to failures due to rate limits.
         *
         * Signature testings is covered in the 1.1 TCK where this resolver was removed.
         */
        return false;
    }

    public static Map<String, Level> getLoggingConfig() {
        return Map.of("ee.jakarta.tck.data", Level.ALL,
                      "org.jboss.arquillian", Level.ALL, //TODO reduce logging once GA
                      "org.eclipse.jnosql", Level.ALL, //TODO reduce logging once GA
                      "io.openliberty.arquillian", Level.ALL); //TODO reduce logging once GA
    }

    /**
     * While in development we may need to skip some tests based on Database
     *
     * @param type - database type
     * @return - Empty string if no tests need to be excluded, otherwise test names to be inserted into <exclude> config
     */
    public static String getExcludedTestByDatabase(DatabaseContainerType type) {
        List<String> exclude = new ArrayList<>();

        switch (type) {
            case DB2:
                return ""; //All tests passing on DB2
            case Derby:
                return ""; // All tests passing on Derby
            case DerbyClient:
                return ""; // Derby client currently not tested during DB Rotation
            case Oracle:
                return ""; // All tests passing on Oracle
            case Postgres:
                return ""; // All tests passing on PostgreSQL
            case SQLServer:
                return ""; // All tests passing on Microsoft SQL Server
            default:
                break;
        }

        return String.join(", ", exclude);
    }
}
