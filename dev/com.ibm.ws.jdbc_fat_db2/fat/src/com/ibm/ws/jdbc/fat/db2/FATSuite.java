/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;
import componenttest.topology.utils.SimpleLogConsumer;

@RunWith(Suite.class)
@SuiteClasses({
                DB2Test.class,
                SQLJTest.class
})
public class FATSuite {

    public static Db2Container db2 = new Db2Container("aguibert/db2-ssl:1.0")
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

    @BeforeClass
    public static void beforeSuite() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client.
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();

        // Filter out any external docker servers in the 'libhpike' cluster
        ExternalTestServiceDockerClientStrategy.serviceFilter = (svc) -> {
            return !svc.getAddress().contains("libhpike-dockerengine");
        };

        db2.start();
    }

    @AfterClass
    public static void afterSuite() {
        db2.stop();
    }
}