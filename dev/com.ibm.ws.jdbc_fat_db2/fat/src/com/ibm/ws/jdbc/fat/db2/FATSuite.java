/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(Suite.class)
@SuiteClasses({
                DB2Test.class,
                SQLJTest.class
})
public class FATSuite {

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();

        // Filter out any external docker servers in the 'libhpike' cluster
        ExternalTestServiceDockerClientStrategy.serviceFilter = (svc) -> {
            return !svc.getAddress().contains("libhpike-dockerengine");
        };
    }

    // Updated docker image to use TLS1.2 for secure communication
    static final DockerImageName db2Image = DockerImageName.parse("kyleaure/db2-ssl:2.0")
                    .asCompatibleSubstituteFor("ibmcom/db2");

    @ClassRule
    public static Db2Container db2 = new Db2Container(db2Image)
                    .acceptLicense()
                    .withUsername("db2inst1") // set in Dockerfile
                    .withPassword("password") // set in Dockerfile
                    .withDatabaseName("testdb") // set in Dockerfile
                    .withExposedPorts(50000, 50001) // 50k is regular 50001 is secure
                    // Use 5m timeout for local runs, 25m timeout for remote runs (extra time since the DB2 container can be slow to start)
                    .waitingFor(new LogMessageWaitStrategy()
                                    .withRegEx(".*DB2 SSH SETUP DONE.*")
                                    .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 25)))
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "db2-ssl"))
                    .withReuse(true);
}