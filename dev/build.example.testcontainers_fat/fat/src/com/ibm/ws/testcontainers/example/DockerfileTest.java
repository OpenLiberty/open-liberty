/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
import web.generic.ContainersTestServlet;

/**
 * Example test class showing how to setup a testcontainer that uses a custom dockerfile.
 */
@Mode(FULL)
@RunWith(FATRunner.class)
public class DockerfileTest {

    public static final String APP_NAME = "containerApp";

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
     * It is possible to provide testcontainers with a DockerFile and build a new image at runtime.
     *
     * <pre>
     * public static GenericContainer<?> container = new GenericContainer<>(
     *          new ImageFromDockerfile()
     *          .withDockerfile(Paths.get("lib/LibertyFATTestFiles/postgres/Dockerfile")))
     * </pre>
     *
     * Doing this will result in a warning being logged:
     *
     * <pre>
     * W WARNING: Cannot use private registry for programmatically built image testcontainers/[image-sha]:latest.
     * Consider using a pre-built image instead.
     * </pre>
     *
     * This warning is thrown to alert the developer that we CANNOT use our Artifactory image cache.
     * This could result in hitting our docker pull limits and introduce intermittent failures.
     * <br>
     *
     * Instead, it is best practice to build and push your docker image to docker hub, and reference that instead.
     * You can still keep the Dockerfile and any related files in source control under publish/files.
     * <br>
     *
     * In this case I pushed my custom image to my personal DockerHub repo under kyleaure/postgres-test-table:1.0
     * You will notice that everything else is configured the same as in the regular ContainersTest test class.
     */
    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("kyleaure/postgres-test-table:3.0")
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
