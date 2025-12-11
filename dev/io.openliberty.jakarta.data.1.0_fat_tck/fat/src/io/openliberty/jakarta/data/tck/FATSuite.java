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

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.impl.JavaInfo;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //Need to have a passing test for Java 8, 11
                DataCoreTckLauncher.class,
                DataWebTckLauncher.class,
                DataFullTckLauncher.class, //full mode
                DataStandaloneTckLauncher.class
})
public class FATSuite extends TestContainerSuite {

    private static final DockerImageName mongoDBImage = DockerImageName.parse("public.ecr.aws/docker/library/mongo:6.0.6")
                    .asCompatibleSubstituteFor("mongo:6.0.6");

    @ClassRule
    public static JdbcDatabaseContainer<?> relationalDatabase = DatabaseContainerFactory.create();

    @ClassRule
    public static MongoDBContainer noSQLDatabase = new MongoDBContainer(mongoDBImage);

    public static boolean shouldRunSignatureTests() {
        boolean result = false;
        String reason = "";

        try {
            if (TestModeFilter.shouldRun(TestMode.FULL)) {
                reason = "Signature test is not run in " + TestModeFilter.FRAMEWORK_TEST_MODE + " mode.";
                return result = false;
            }

            if (System.getProperty("os.name", "unknown").toLowerCase().contains("windows")) {
                reason = "signature test plugin not supported on Windows.";
                return result = false;
            }

            if (JavaInfo.JAVA_VERSION != 17 && JavaInfo.JAVA_VERSION != 21) {
                reason = "signature test not supported on non-LTS java versions: " + JavaInfo.JAVA_VERSION;
                return result = false;
            }

            //default option
            reason = "signature test can run as configured";
            return result = true;

        } finally {
            Log.info(FATSuite.class, "shouldRunSignatureTests", "Return: " + result + ", because " + reason);
        }
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
                //TODO testInsertEntityThatAlreadyExists PostgreSQL throws org.postgresql.util.PSQLException which is not a subclass of SQLIntegrityConstraintViolationException
                exclude.add("ee.jakarta.tck.data.standalone.persistence.PersistenceEntityTests");
                break;
            case SQLServer:
                //TODO testInsertEntityThatAlreadyExists SQLServer throws com.microsoft.sqlserver.jdbc.SQLServerException which is not a subclass of SQLIntegrityConstraintViolationException
                exclude.add("ee.jakarta.tck.data.standalone.persistence.PersistenceEntityTests");
                break;
            default:
                break;
        }

        return String.join(", ", exclude);
    }
}
