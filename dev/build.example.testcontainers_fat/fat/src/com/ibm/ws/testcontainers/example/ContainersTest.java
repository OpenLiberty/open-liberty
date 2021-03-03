/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testcontainers.example;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.generic.ContainersTestServlet;

/**
 * Example test class showing how to setup a regular predefined
 * TestContainer for use to test against.
 */
@RunWith(FATRunner.class)
@Mode(FULL)
public class ContainersTest extends FATServletClient {

    public static final String APP_NAME = "containerApp";

    @Server("build.example.testcontainers")
    @TestServlet(servlet = ContainersTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public static final String POSTGRES_DB = "test";
    public static final String POSTGRES_USER = "test";
    public static final String POSTGRES_PASSWORD = "test";
    public static final int POSTGRE_PORT = 5432;

    /*
     * When using a generic container you will need to provide all the information needed
     * to run that container. This is equivalent of constructing a docker run command.
     *
     * This is annotated as a ClassRule which will call start/stop on the container automatically
     *
     * ~~Common settings~~
     * Constructor: accepts image name in form user/container:version
     * - withExposedPorts: what ports does that container use that need to be exposed
     * - withEnv: evironment variables that can be set on the container
     * - withCommand: replace docker CMD with a custom command
     * - withLogConsumer: redirect stout/sterr from container to a log consumer
     * Use the SimpleLogConsumer from fattest.simplicity to redirect those logs to output.txt
     * - waitingFor: defines a wait strategy to know when container has started
     *
     * NOTE: the testcontainers project has a pre-configured PostgreSQLContainer class that could
     * have been used here. This is just an example of how to setup a GenericContainer.
     */
    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("postgres:9.6.12")
                    .withExposedPorts(POSTGRE_PORT)
                    .withEnv("POSTGRES_DB", POSTGRES_DB)
                    .withEnv("POSTGRES_USER", POSTGRES_USER)
                    .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
                    .withLogConsumer(new SimpleLogConsumer(ContainersTest.class, "postgres"))
                    .waitingFor(new LogMessageWaitStrategy()
                                    .withRegEx(".*database system is ready to accept connections.*\\s")
                                    .withTimes(2)
                                    .withStartupTimeout(Duration.ofSeconds(60)));

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "web.generic");

        /*
         * Use server.addEnvVar() to pass any variables from the container that is needed
         * within server.xml, or by the application.
         *
         * Main use:
         * testcontainers always exposes ports onto RANDOM port numbers to avoid port conflicts.
         */
        server.addEnvVar("PS_URL", "jdbc:postgresql://" + container.getContainerIpAddress() //
                                   + ":" + container.getMappedPort(POSTGRE_PORT)
                                   + "/" + POSTGRES_DB);
        server.addEnvVar("PS_USER", POSTGRES_USER);
        server.addEnvVar("PS_PASSWORD", POSTGRES_PASSWORD);

        server.startServer();

        runTest(server, APP_NAME, "setupDatabase");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
