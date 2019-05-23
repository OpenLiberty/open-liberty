/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.messaging.tck;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs the whole messaging TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 */
@RunWith(FATRunner.class)
public class ReactiveStreamsMessageingGitTckLauncher {

    /**  */
    private static String GIT_REPO_PARENT_DIR = "publish/gitRepos/";
    private static final String GIT_REPO_NAME = "microprofile-reactive-messaging";

    private static final String API_JAR_DIR_REL_PATH = "publish/gitRepos/microprofile-reactive-messaging/api/target/";

    @Server("ReactiveStreamsTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        //server.startServer();
    }

    /**
     * Various TCK tests test for Deployment, Definition and other Exceptions and
     * these will cause the test suite to be marked as FAILED if found in the logs
     * when the server is shut down. So we tell Simplicity to allow for the message
     * ID's below.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tcl/tck-suite.html)
     *
     * @throws Exception
     */
    @Mode(TestMode.EXPERIMENTAL)
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void runFreshMasterBranchTck() throws Exception {

        File repoParent = new File(GIT_REPO_PARENT_DIR);
        File repo = new File(repoParent, GIT_REPO_NAME);

        MvnUtils.mvnCleanInstall(repo);

        String apiJar = findAPIJar();
        server.copyFileToLibertyServerRoot(API_JAR_DIR_REL_PATH, "lib", apiJar);

        server.startServer();

        HashMap<String, String> addedProps = new HashMap<String, String>();

        String apiVersion = MvnUtils.getApiSpecVersionAfterClone(repo);
        System.out.println("Queried api.version is : " + apiVersion);
        addedProps.put(MvnUtils.API_VERSION, apiVersion);

        String tckVersion = MvnUtils.getTckVersionAfterClone(repo);
        System.out.println("Queried tck.version is : " + tckVersion);
        addedProps.put(MvnUtils.TCK_VERSION, tckVersion);

        // A command line -Dprop=value actually gets to here as a environment variable...
        String implVersion = System.getenv("impl.version");
        System.out.println("Passed in impl.version is : " + implVersion);
        addedProps.put(MvnUtils.IMPL_VERSION, implVersion);

        // We store a set of keys that we want the system to add "1.1" or "1.2" etc to
        // depending on the pom.xml contents.
        HashSet<String> versionedLibraries = new HashSet<>(Arrays.asList("org.eclipse.microprofile.reactive.messaging"));
        String backStopImpl = "1.0"; // Used if there is no impl matching the spec/pom.xml <version> AND impl.version is not set
        addedProps.put(MvnUtils.BACKSTOP_VERSION, backStopImpl);

        MvnUtils.runTCKMvnCmd(server, "org.eclipse.microprofile.reactive.messaging.tck", this
                        .getClass() + ":launchReactiveMessagingTCK", MvnUtils.DEFAULT_SUITE_FILENAME, addedProps, versionedLibraries);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testThatDoesNothingAndCanAlwaysRunAndPass() {
        if (TestModeFilter.FRAMEWORK_TEST_MODE != TestMode.EXPERIMENTAL) {
            System.out.println("\n\n\n");
            System.out.println("TCK MASTER BRANCH RUN NOT REQUESTED: fat.test.mode=" + TestModeFilter.FRAMEWORK_TEST_MODE
                               + ", run with '-Dfat.test.mode=experimental' to run the TCK");
            System.out.println("\n\n\n");
            // In FATs System.out is captured to a file, we try to be kind to any developer and give it to them straight
            if (Boolean.valueOf(System.getProperty("fat.test.localrun"))) {
                try (PrintStream screen = new PrintStream(new FileOutputStream(FileDescriptor.out))) {
                    screen.println("\n\n\n");
                    screen.println("TCK MASTER BRANCH RUN NOT REQUESTED: fat.test.mode=" + TestModeFilter.FRAMEWORK_TEST_MODE
                                   + ", run with '-Dfat.test.mode=experimental' to run the TCK");
                    screen.println("\n\n\n");
                }
            }
        }
    }

    private String findAPIJar() throws IOException {
        Path apiPath = Paths.get(API_JAR_DIR_REL_PATH);
        Stream<Path> stream =
                Files.find(apiPath, 1,
                        (path, basicFileAttributes) -> {
                            File file = path.toFile();
                            return !file.isDirectory() &&
                                    file.getName().contains("microprofile-reactive-messaging-api") &&
                                    ! file.getName().contains("sources") &&
                                    ! file.getName().contains("javadoc");
                        });
        return stream.findAny().get().getFileName().toString(); //Only one entry to find. 
    }
}
