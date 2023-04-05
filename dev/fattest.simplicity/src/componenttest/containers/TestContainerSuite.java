/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package componenttest.containers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

import com.ibm.websphere.simplicity.log.Log;

public class TestContainerSuite {

    private static final Class<?> c = TestContainerSuite.class;

    private static boolean setupComplete = false;

    /*
     * THIS METHOD CALL IS REQUIRED TO USE TESTCONTAINERS PLEASE READ:
     *
     * Testcontainers caches data in a properties file located at $HOME/.testcontainers.properties
     * The setupTestcontainers() method will clear and reset the values in this property file.
     *
     * By default, testcontainers will attempt to run against a local docker instance and pull from DockerHub.
     * If you want testcontainers to run against a remote docker host to mirror the behavior of an RTC build
     * Then, set property: -Dfat.test.use.remote.docker=true
     * This will only work if you are on the IBM network.
     *
     * We will set the following properties:
     * 1. docker.client.strategy:
     * Default: [Depends on local OS]
     * Custom : componenttest.containers.ExternalDockerClientStrategy
     * Purpose: This is the strategy testcontainers uses to locate and run against a remote docker instance.
     *
     * 2. image.substitutor:
     * Default: [none]
     * Custom : componenttest.containers.ArtifactoryImageNameSubstitutor
     * Purpose: This defines a strategy for substituting image names.
     * This is so that we can use a private docker repository to cache docker images
     * to avoid the docker pull limits.
     * Example: foo/bar:1.0 it will get changed to [ARTIFACTORY_REGISTRY]/wasliberty-docker-remote/foo/bar:1.0
     */
    static {
        Log.info(TestContainerSuite.class, "<init>", "Setting up testcontainers");
        setupTestcontainers();
    }

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void after() {
            Log.info(TestContainerSuite.class, "after", "Assert all container images have been declared");
            ImageVerifier.assertImages();
        }
    };

    /**
     * <pre>
     * By default, Testcontainers will cache the DockerClient strategy in <code>~/.testcontainers.properties</code>.
     *
     * Calling this method in the FATSuite class is REQUIRED for any fat project that uses Testcontainers.
     * This is a safety measure to ensure that we run with the correct docker.client.stategy property
     * for each FATSuite run.
     */
    private static void setupTestcontainers() {
        if (setupComplete)
            return;
        generateTestcontainersConfig();
        setupComplete = true;
    }

    private static void generateTestcontainersConfig() {
        final String m = "generateTestcontainersConfig";

        File testcontainersConfigFile = new File(System.getProperty("user.home"), ".testcontainers.properties");

        Properties tcProps = new Properties();
        try {
            if (testcontainersConfigFile.exists()) {
                Log.info(c, m, "Testcontainers config already exists at: " + testcontainersConfigFile.getAbsolutePath());
                FileInputStream tcPropsInputStream = new FileInputStream(testcontainersConfigFile);
                tcProps.load(tcPropsInputStream);
                tcProps.remove("docker.client.strategy");
                tcPropsInputStream.close(); // avoids delete failing on windows
                Files.delete(testcontainersConfigFile.toPath());
            } else {
                Log.info(c, m, "Testcontainers config being created at: " + testcontainersConfigFile.getAbsolutePath());
            }
            tcProps.setProperty("image.substitutor", ArtifactoryImageNameSubstitutor.class.getCanonicalName().toString());
            tcProps.store(new FileOutputStream(testcontainersConfigFile), "Modified by FAT framework");
            Log.info(c, m, "Testcontainers config properties: " + tcProps.toString());
        } catch (IOException e) {
            Log.error(c, "generateTestcontainersConfig", e);
            throw new RuntimeException(e);
        }
    }
}
