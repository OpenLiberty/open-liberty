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
    @ClassRule
    public static JdbcDatabaseContainer<?> relationalDatabase = DatabaseContainerFactory.create();

    @ClassRule
    public static MongoDBContainer noSQLDatabase = new MongoDBContainer(DockerImageName.parse("mongo:6.0.6"));

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
}
