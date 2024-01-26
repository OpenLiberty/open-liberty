/*******************************************************************************
 * Copyright (c) 2020,2024 IBM Corporation and others.
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
package io.openliberty.microprofile.lra.tck;

import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * This is a test class that runs the whole LRA TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 * In normal (lite) mode, just one test from the TCK is run
 * To run the full TCK, the suite must be run in FULL mode
 * gradlew -Dfat.test.mode=FULL io.openliberty.microprofile.lra.1.0.internal_fat_tck:buildandrun
 */
@RunWith(FATRunner.class)
public class LraTckLauncher {

    private static final String SERVER_NAME = "LRATCKServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        // Test fails to start properly on z/OS
        assumeThat(Machine.getLocalMachine().getOperatingSystem(), not(OperatingSystem.ZOS));

        // microprofile config will allow this to be accessed by the application as
        // lra.tck.base.url, which is what the tck is looking for. LibertyServer won't allow
        // '.' to be used in an env var name, as it "isn't cross platform".
        String key = "lra_tck_base_url";
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getHttpDefaultPort());
        String value = protocol + "://" + host + ":" + port;

        server.addEnvVar(key, value);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // server.stopServer("CWMFT5001E"); // Any expected message IDs in the logs could be added here.
        server.stopServer();
    }

    /**
     * Run one test from the TCK
     *
     * @throws Exception
     */
    @Test
    public void launchLRA10TCK() throws Exception {

        // This makes the property lra.tck.base.url available to maven, so that it can pass it on to the
        // arquillian launcher. Not entirely sure if it is needed or not.
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getHttpDefaultPort());
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("lra.tck.base.url", protocol + "://" + host + ":" + port);
        additionalProps.put("lraTestsToRun", "**/TckTests.java");
        // This is the currently passing test methods from TckTests
        additionalProps.put("test", "TckTests#*LRA*+join*");

        String bucketName = "io.openliberty.microprofile.lra.1.0.internal_fat_tck";
        String testName = this.getClass() + ":launchLRA10TCK";
        Type type = Type.MICROPROFILE;
        String specName = "LRA";
        TCKRunner.runTCK(server, bucketName, testName, type, specName, additionalProps);
    }

    /**
     * Run the whole TCK
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    @Mode(TestMode.EXPERIMENTAL)
    public void launchLRA10TCKFull() throws Exception {

        // This makes the property lra.tck.base.url available to maven, so that it can pass it on to the
        // arquillian launcher. Not entirely sure if it is needed or not.
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getHttpDefaultPort());
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("lra.tck.base.url", protocol + "://" + host + ":" + port);
        additionalProps.put("lraTestsToRun", "**/*Test*.java");

        String bucketName = "io.openliberty.microprofile.lra.1.0.internal_fat_tck";
        String testName = this.getClass() + ":launchLRA10TCKFull";
        Type type = Type.MICROPROFILE;
        String specName = "LRA";
        TCKRunner.runTCK(server, bucketName, testName, type, specName, additionalProps);

    }
}
