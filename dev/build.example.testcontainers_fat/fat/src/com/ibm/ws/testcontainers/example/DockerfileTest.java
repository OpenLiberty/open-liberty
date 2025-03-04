/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package com.ibm.ws.testcontainers.example;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.RemoteDockerImage;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.containers.ImageBuilder;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import web.generic.ContainersTestServlet;

/**
 * Example test class showing how to setup a testcontainer that uses a custom dockerfile.
 */
@Mode(FULL)
@RunWith(FATRunner.class)
public class DockerfileTest {

    public static final String APP_NAME = "app";

    @Server("build.example.testcontainers")
    @TestServlet(servlet = ContainersTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public static final String POSTGRES_DB = "test";
    public static final String POSTGRES_USER = "test";
    public static final String POSTGRES_PASSWORD = "test";
    public static final int POSTGRE_PORT = 5432;

    /**
     * There are times where we might want to extend a base docker image for our
     * own testing needs. For example, using a docker image that already uses a startup script.
     * It is possible to provide testcontainers with a Dockerfile and build a new image at runtime.
     *
     * <pre>
     * private static final RemoteDockerImage POSTGRES_INIT = ImageBuilder.build("postgres-init:17-alpine").get();
     * </pre>
     *
     * The ImageBuilder will depend on a Dockerfile being located in:
     * io.openliberty.org.testcontainers/resources/openliberty/testcontainers/postgres-init/17-alpine/Dockerfile
     *
     * The ImageBuilder will get (in this order):
     * - A cached instance of the image from the docker host (local or remote)
     * - otherwise, pull a cached instance from an internal registry (if one is configured)
     * - otherwise, build an instance at test runtime.
     */
    private static final RemoteDockerImage POSTGRES_INIT = ImageBuilder.build("postgres-init:17-alpine").getFuture();

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(POSTGRES_INIT)
                    .withExposedPorts(POSTGRE_PORT)
                    .withEnv("POSTGRES_DB", POSTGRES_DB)
                    .withEnv("POSTGRES_USER", POSTGRES_USER)
                    .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
                    .withLogConsumer(new SimpleLogConsumer(ContainersTest.class, "postgres-init"))
                    .waitingFor(new LogMessageWaitStrategy()
                                    .withRegEx(".*database system is ready to accept connections.*\\s")
                                    .withTimes(2)
                                    .withStartupTimeout(Duration.ofSeconds(60)));

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "web.generic");

        //Execute a command within container after it has started
        container.execInContainer("echo \"This is executed after container has started\"");

        server.addEnvVar("PS_URL", "jdbc:postgresql://" + container.getHost() //
                                   + ":" + container.getMappedPort(POSTGRE_PORT)
                                   + "/" + POSTGRES_DB);
        server.addEnvVar("PS_USER", POSTGRES_USER);
        server.addEnvVar("PS_PASSWORD", POSTGRES_PASSWORD);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
