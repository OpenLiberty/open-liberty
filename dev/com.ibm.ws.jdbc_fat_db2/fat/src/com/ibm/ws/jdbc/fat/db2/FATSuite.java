/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.db2;

import java.time.Duration;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.SimpleLogConsumer;
import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(Suite.class)
@SuiteClasses({
                DB2Test.class,
                SQLJTest.class
})
public class FATSuite extends TestContainerSuite {

    // Updated docker image to use TLS1.2 for secure communication
    static final DockerImageName db2Image = DockerImageName.parse("kyleaure/db2-ssl:3.0")
                    .asCompatibleSubstituteFor("ibmcom/db2"); //TODO update .asCompatibleSubstituteFor("icr.io/db2_community/db2")

    @ClassRule
    public static Db2Container db2 = new Db2Container(db2Image)
                    .acceptLicense()
                    .withUsername("db2inst1") // set in Dockerfile
                    .withPassword("password") // set in Dockerfile
                    .withDatabaseName("testdb") // set in Dockerfile
                    .withExposedPorts(50000, 50001) // 50k is regular 50001 is secure
                    // Use 5m timeout for local runs, 35m timeout for remote runs (extra time since the DB2 container can be slow to start)
                    .waitingFor(new LogMessageWaitStrategy()
                                    .withRegEx(".*DB2 SSH SETUP DONE.*")
                                    .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN && !FATRunner.ARM_ARCHITECTURE ? 5 : 35)))
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "db2-ssl"))
                    .withReuse(true);
}