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
import java.io.IOException;
import java.nio.file.Files;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.GenericContainer;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.ExternalTestService;

@RunWith(Suite.class)
@SuiteClasses({
                PostgreSQLTest.class
})
public class FATSuite {

    @BeforeClass
    public static void verifyDockerAvailable() throws Exception {
        final String m = "verifyDockerAvailable";
        // TODO: Once this mechanism has matured a bit, move it to fattest.simplicity for more general use

        File testcontainersConfigFile = new File(System.getProperty("user.home"), ".testcontainers.properties");
        Log.info(c, m, "Removing testcontainers property file at: " + testcontainersConfigFile.getAbsolutePath());
        Files.deleteIfExists(testcontainersConfigFile.toPath());

        if (FATRunner.FAT_TEST_LOCALRUN && !Boolean.getBoolean("fat.test.use.remote.docker")) {
            Log.info(c, m, "Using local Docker Host for this FAT.");
        } else {
            Log.info(c, m, "Using remote Docker Host for this FAT.");
            ExternalTestService.getService("docker-engine", FATSuite::tryDockerHost);
        }
    }

    private static final Class<?> c = FATSuite.class;

    private static boolean tryDockerHost(ExternalTestService dockerService) {
        String m = "tryDockerHost";
        String dockerHostURL = "tcp://" + dockerService.getAddress() + ":" + dockerService.getPort();
        System.setProperty("DOCKER_HOST", dockerHostURL);
        File certDir = new File("docker-certificates");
        certDir.mkdirs();
        writeFile(new File(certDir, "ca.pem"), dockerService.getProperties().get("ca.pem"));
        writeFile(new File(certDir, "cert.pem"), dockerService.getProperties().get("cert.pem"));
        writeFile(new File(certDir, "key.pem"), dockerService.getProperties().get("key.pem"));
        System.setProperty("DOCKER_TLS_VERIFY", "1");
        System.setProperty("DOCKER_CERT_PATH", certDir.getAbsolutePath());
        Log.info(FATSuite.class, "tryDockerHost", "Checking if Docker host " + dockerHostURL + " is available and healthy...");
        try (GenericContainer<?> helloWorldContianer = new GenericContainer<>("alpine:3.5")
                        .withCommand("sh", "-c", "while true; do nc -lp 8080; done")) {
            helloWorldContianer.start();
            String containerIp = helloWorldContianer.getContainerIpAddress();
            Log.info(c, m, "Container hostname is: " + containerIp);
            if (containerIp.contains("localhost") || containerIp.contains("127.0.0.1")) {
                throw new RuntimeException("Should not be using a local docker container address: " + containerIp);
            }
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

    private static void writeFile(File outFile, String content) {
        try {
            Files.deleteIfExists(outFile.toPath());
            Files.write(outFile.toPath(), content.getBytes());
        } catch (IOException e) {
            Log.error(c, "writeFile", e);
            throw new RuntimeException(e);
        }
        Log.info(c, "writeFile", "Wrote property to: " + outFile.getAbsolutePath());
    }

}
