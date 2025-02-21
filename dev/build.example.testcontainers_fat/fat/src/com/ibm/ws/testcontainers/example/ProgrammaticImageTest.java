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

import java.io.File;
import java.time.Duration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
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
     * There are times where we might want to extend a base docker image for our
     * own testing needs. For example, using a docker image that already uses a startup script.
     * It is possible with testcontainers to programmatically build a docker image instead of using
     * a prebuilt custom image or building from a dockerfile.
     *
     * NOTE: building from a Dockerfile should be avoided at all costs.
     *
     * Here we are pulling from a base image postgres:17.0-alpine
     *
     * However, we use special processing for the image name to ensure that when testing locally we pull
     * from DockerHub, and when testing against a remote docker image we use a subsituted image name to pull
     * from artifactory.
     *
     * Example:
     * ImageNameSubstitutor.instance().apply(DockerImageName.parse("public.ecr.aws/docker/library/postgres:17.0-alpine")).asCanonicalNameString()
     *
     * When testing locally a DefaultImageNameSubstitutor will be used and postgres:17.0-alpine will be returned as normal.
     * When testing on a remote docker host, our internal ArtifactoryImageNameSubstitutor will be used and
     *   [ARTIFACTORY_REGISTRY]/wasliberty-docker-remote/postgres:17.0-alpine will be returned
     *
     * </pre>
     *
     * @see DockerfileTest
     */

    private static final DockerImageName postgresql = ImageNameSubstitutor
                    .instance() //
                    .apply(DockerImageName.parse("public.ecr.aws/docker/library/postgres:17.0-alpine"));

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(//
                    new ImageFromDockerfile().withDockerfileFromBuilder(builder -> builder.from(postgresql.asCanonicalNameString())//
                                    .copy("/docker-entrypoint-initdb.d/initDB.sql", "/docker-entrypoint-initdb.d/initDB.sql")
                                    .build())
                                    .withFileFromFile("/docker-entrypoint-initdb.d/initDB.sql", new File("lib/LibertyFATTestFiles/postgres/initDB.sql"), 644))
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
