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
 * Example test class showing how to setup a testcontainer that programmatically creates a docker image.
 */
@Mode(FULL)
@RunWith(FATRunner.class)
public class ProgrammaticImageTest {

    public static final String APP_NAME = "app";

    @Server("build.example.testcontainers")
    @TestServlet(servlet = ContainersTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public static final String POSTGRES_DB = "test";
    public static final String POSTGRES_USER = "test";
    public static final String POSTGRES_PASSWORD = "test";
    public static final int POSTGRE_PORT = 5432;

    /**
     * <pre>
     * There are times where we might need to extend a base docker image for our own testing needs.
     * For example, using a docker image that already has a startup script, or custom libraries.
     *
     * It is possible with testcontainers to programmatically build a docker image at runtime instead of using
     * a pre-built community image which are consider unsafe.
     *
     * To accomplish this goal the recommended way is to create a Dockerfile in the io.openliberty.org.testcontainers project.
     * These Dockerfiles are stored using the following directory structure:
     *
     * - io.openliberty.org.testcontainers/resources/openliberty/testcontainers/[image-name]/[image-version]/
     *   - Dockerfile
     *   - [supporting-files]
     *   - [supporting-directories]
     *
     * A custom builder class is available in fattest.simplicity {@link componenttest.containers.ImageBuilder}
     * which is required to build am image from a Dockerfile in io.openliberty.org.testcontainers using the syntax:
     *
     * ImageBuilder.build("[image-name]/[image-version]").with("[BASE_IMAGE]").get()
     *
     * Where "BASE_IMAGE" is the name of the base image for the FROM line of the Dockerfile.
     * This is necessary to provide since we will substitute the image name at runtime on our build systems
     * to avoid pulling from registries outside of our internal mirrors.
     *
     * NOTE: If the image name is available on a docker host, we will not attempt to re-build the image.
     * Therefore, any updates to a Dockerfile MUST result in a new version of the image.
     * </pre>
     */
    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(ImageBuilder //
                    .build("postgres-init:1.0")
                    .with("public.ecr.aws/docker/library/postgres:17.0-alpine")
                    .get())
                    .withExposedPorts(POSTGRE_PORT)
                    .withEnv("POSTGRES_DB", POSTGRES_DB)
                    .withEnv("POSTGRES_USER", POSTGRES_USER)
                    .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
                    .withLogConsumer(new SimpleLogConsumer(ProgrammaticImageTest.class, "postgres-init"))
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
