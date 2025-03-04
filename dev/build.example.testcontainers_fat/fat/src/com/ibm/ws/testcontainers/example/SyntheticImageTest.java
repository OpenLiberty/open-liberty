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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.ImageHelper;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

/**
 * !!!!! DO NOT COPY, RUN, OR USE THIS CLASS !!!!!
 *
 * This test class is used to test hypothetical synthetic images to ensure that
 * our test infrastructure will correctly handle Synthetic Images and throw the
 * correct warning if one was created.
 *
 * Synthetic images have poor performance, and will do not allow us to use our
 * Artifactory image cache
 *
 * If you need the level of customization that a synthetic image would provide
 * then you should follow the DockerfileTest or ProgrammaticImageTest
 *
 * Most of this test is commented out on purpose so we don't actually run a
 * synthetic container on our build systems.
 *
 * @see DockerfileTest
 * @see ProgrammaticImageTest
 */
@Mode(FULL)
@RunWith(FATRunner.class)
public class SyntheticImageTest {

//    public static final String APP_NAME = "app";
//
//    @Server("build.example.testcontainers")
//    @TestServlet(servlet = ContainersTestServlet.class, contextRoot = APP_NAME)
//    public static LibertyServer server;
//
//    public static final String POSTGRES_DB = "test";
//    public static final String POSTGRES_USER = "test";
//    public static final String POSTGRES_PASSWORD = "test";
//    public static final int POSTGRE_PORT = 5432;
//
//
//    public static ImageFromDockerfile synteticImage = new ImageFromDockerfile()
//                    .withDockerfile(Paths.get("lib/LibertyFATTestFiles/postgres/Dockerfile"));
//
//    @ClassRule
//    public static GenericContainer<?> container = new GenericContainer<>(synteticImage) //
//                    .withExposedPorts(POSTGRE_PORT)
//                    .withEnv("POSTGRES_DB", POSTGRES_DB)
//                    .withEnv("POSTGRES_USER", POSTGRES_USER)
//                    .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
//                    .withLogConsumer(new SimpleLogConsumer(ContainersTest.class, "postgres"))
//                    .waitingFor(new LogMessageWaitStrategy()
//                                    .withRegEx(".*database system is ready to accept connections.*\\s")
//                                    .withTimes(2)
//                                    .withStartupTimeout(Duration.ofSeconds(60)));

    @BeforeClass
    public static void setUp() throws Exception {
//        ShrinkHelper.defaultApp(server, APP_NAME, "web.generic");
//
//        //Execute a command within container after it has started
//        container.execInContainer("echo \"This is executed after container has started\"");
//
//        server.addEnvVar("PS_URL", "jdbc:postgresql://" + container.getHost() //
//                                   + ":" + container.getMappedPort(POSTGRE_PORT)
//                                   + "/" + POSTGRES_DB);
//        server.addEnvVar("PS_USER", POSTGRES_USER);
//        server.addEnvVar("PS_PASSWORD", POSTGRES_PASSWORD);
//
//        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
//        server.stopServer();
    }

    /**
     * If testcontainers ever changes the way that it names synthetic images we need
     * to change the way we determine an image is synthetic in the ImageHelper
     *
     * NOTE: this test was written due to a BUG: https://github.com/testcontainers/testcontainers-java/issues/5194
     * Expect this to be fixed in later versions
     *
     * @see ImageHelper#isSynthetic()
     */
//    @Test
//    public void ensureSyntheticImageNameWithRealContainer() {
//        DockerImageName name = DockerImageName.parse(container.getDockerImageName());
//
//        Log.info(SyntheticImageTest.class, "ensureSyntheticImageName", name.asCanonicalNameString());
//
//        //Example: localhost/testcontainers/jxbqdhfgsomphxr5:latest
//        assertEquals("localhost", name.getRegistry()); //localhost
//        assertEquals("testcontainers", name.getRepository().split("/")[0]); //testcontainers/jxbqdhfgsomphxr5
//        assertEquals("latest", name.getVersionPart()); //latest
//    }

    /**
     * If docker-java ever changes the way that it names committed images we need
     * to change the way we determine an image is committed in the ImageHelper
     *
     * NOTE: This model of committing a container to an image, just to turn around and start it as a container
     * will result in VERY poor test performance. Please consider using a DockerFile or Programmatic image instead!
     */
//    @Test
//    public void ensureCommittedImageWithRealContainer() {
//
//        GenericContainer duplicateContainer = null;
//        try {
//            String commitedImage = container.getDockerClient().commitCmd(container.getContainerId()).exec();
//            duplicateContainer = new GenericContainer(commitedImage);
//            duplicateContainer.start();
//            DockerImageName name = DockerImageName.parse(duplicateContainer.getDockerImageName());
//
//            Log.info(SyntheticImageTest.class, "ensureCommittedImage", name.asCanonicalNameString());
//
//            //Example: sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b
//            assertEquals("", name.getRegistry());
//            assertEquals("sha256", name.getRepository()); //sha256
//            assertNotNull(name.getRepository()); //5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b
//        } catch (Exception e) {
//            fail(e.toString());
//        } finally {
//            if (duplicateContainer != null) {
//                duplicateContainer.close();
//            }
//        }
//    }

    /**
     * If testcontainers ever changes the way that it names synthetic images we need
     * to change the way we determine an image is synthetic in the ImageHelper
     *
     * NOTE: this test was written due to a BUG: https://github.com/testcontainers/testcontainers-java/issues/5194
     * Expect this to be fixed in later versions
     *
     * @see ImageHelper#isSynthetic()
     */
    @Test
    public void ensureSyntheticImageName() {
        DockerImageName name = DockerImageName.parse("localhost/testcontainers/jxbqdhfgsomphxr5:latest");

        //Example: localhost/testcontainers/jxbqdhfgsomphxr5:latest
        assertEquals("localhost", name.getRegistry()); //localhost
        assertEquals("testcontainers", name.getRepository().split("/")[0]); //testcontainers/jxbqdhfgsomphxr5
        assertEquals("latest", name.getVersionPart()); //latest
    }

    /**
     * If docker-java ever changes the way that it names committed images we need
     * to change the way we determine an image is committed in the ImageHelper
     *
     * NOTE: This model of committing a container to an image, just to turn around and start it as a container
     * will result in VERY poor test performance. Please consider using a DockerFile or Programmatic image instead!
     */
    @Test
    public void ensureCommittedImage() {

        DockerImageName name = DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b");

        //Example: sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b
        assertEquals("", name.getRegistry());
        assertEquals("sha256", name.getRepository()); //sha256
        assertNotNull(name.getRepository()); //5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b

    }
}
