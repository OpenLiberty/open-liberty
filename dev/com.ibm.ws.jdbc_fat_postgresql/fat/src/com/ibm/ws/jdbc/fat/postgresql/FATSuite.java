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
package com.ibm.ws.jdbc.fat.postgresql;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.GenericContainer;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.ExternalTestService;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                // PostgreSQLTest.class // TODO: intentionally leave this disabled until we get remote docker support in our build machines
})
public class FATSuite {

    // @BeforeClass // TODO: intentionally leave this disabled until we get remote docker support in our build machines
    public static void verifyDockerAvailable() throws Exception {
        // TODO: Once this mechanism has matured a bit, move it to fattest.simplicity for more general use

        // Use local Docker install if running locally.
        // Devs can override this by specifying -Dfat.test.DOCKER_HOST=tcp://example.com:2345 when they launch the FAT
        if (FATRunner.FAT_TEST_LOCALRUN) {
            if (!System.getProperty("fat.test.DOCKER_HOST", "").isEmpty())
                System.setProperty("DOCKER_HOST", System.getProperty("fat.test.DOCKER_HOST"));
            boolean dockerHostSet = !System.getProperty("DOCKER_HOST", "").isEmpty() ||
                                    (System.getenv("DOCKER_HOST") != null && !System.getenv("DOCKER_HOST").isEmpty());
            File testcontainersConfigFile = new File(System.getProperty("user.home"), ".testcontainers.properties");
            if (!testcontainersConfigFile.exists())
                return;
            Properties testcontainerProps = new Properties();
            try (FileInputStream fis = new FileInputStream(testcontainersConfigFile)) {
                testcontainerProps.load(fis);
            }
            String currentStrategy = testcontainerProps.getProperty("testcontainers.properties");
            if (!dockerHostSet && "org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy".equals(currentStrategy)) {
                Log.info(c, "verifyDockerAvailable", "No DOCKER_HOST set in env or sysprops, but env/sysprop Docker client strategy found in ~/.testcontainers.properties.");
                Log.info(c, "verifyDockerAvailable", "Attempting to reset Testcontainers properties file by deleting " + testcontainersConfigFile.getAbsolutePath());
                Files.delete(testcontainersConfigFile.toPath());
            } else if (dockerHostSet && !"org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy".equals(currentStrategy)) {
                Log.info(c, "verifyDockerAvailable", "DOCKER_HOST set in env/sysprops, but env/sysprop strategy not set in ~/.testcontainers.properties.");
                Log.info(c, "verifyDockerAvailable", "Attempting to reset Testcontainers properties file by deleting " + testcontainersConfigFile.getAbsolutePath());
                Files.delete(testcontainersConfigFile.toPath());
            }
            return;
        } else {
            // Running in a remote build, check Consul for an external Docker service
            for (ExternalTestService dockerService : ExternalTestService.getServices(10, "aguibert-test-docker-host")) {
                if (tryDockerHost(dockerService))
                    return; // got a healthy instance
            }
            throw new IllegalStateException("Unable to locate any external Docker host services");
        }
    }

    private static final Class<?> c = FATSuite.class;

    private static boolean tryDockerHost(ExternalTestService dockerService) {
        String m = "tryDockerHost";
        String dockerHostURL = "tcp://" + dockerService.getAddress() + ":" + dockerService.getPort();
        System.setProperty("DOCKER_HOST", dockerHostURL);
        Log.info(FATSuite.class, "tryDockerHost", "Checking if Docker host " + dockerHostURL + " is available and healthy...");
        try (GenericContainer<?> helloWorldContianer = new GenericContainer<>("alpine:3.5")
                        .withCommand("sh", "-c", "while true; do nc -lp 8080; done")) {
            helloWorldContianer.start();
            if (helloWorldContianer.isCreated() && helloWorldContianer.isRunning()) {
                Log.info(c, m, "Docker host was able to create and start a test container. Will use this instance");
                return true;
            } else {
                String msg = "Container at " + dockerHostURL + " was not created or started properly: " + helloWorldContianer.getLogs();
                Log.info(c, m, msg);
                dockerService.reportUnhealthy(msg);
                return false;
            }
        } catch (Throwable e) {
            Log.error(c, m, e);
            dockerService.reportUnhealthy("Container at " + dockerHostURL + " encountered an issue while starting: " + e.getMessage());
            return false;
        }
    }

}
