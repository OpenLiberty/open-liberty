/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.jakarta.jsonp.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * This is a test class that runs the whole Jakarta JSON-P TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class JsonpTckLauncher {

    //This is a standalone test no server needed
    @Server
    public static LibertyServer DONOTSTART;

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchJsonp21TCK() throws Exception {
        Map<String, String> additionalProps = new HashMap<>();

        // Skip signature testing on Windows and Semeru JDK.
        // So far as I can tell the signature test plugin is not supported on this configuration
        //Opened an issue against jsonb tck https://github.com/eclipse-ee4j/jsonb-api/issues/327
        // Also skip the JsonProviderTest, because if any other test has already run before, the provider has been set
        // and the test fails because you can't then set a different provider
        // Opened an issue against the jsonp tck https://github.com/eclipse-ee4j/jsonp/issues/376
        if (System.getProperty("os.name").contains("Windows")) {
            Log.info(JsonpTckLauncher.class, "launchJsonp21TCK", "Skipping JSONP Signature Test on Windows and Semeru JDK");
            additionalProps.put("exclude.tests", "ee.jakarta.tck.jsonp.signaturetest.jsonp.JSONPSigTest.java");
        }

        String bucketName = "io.openliberty.jakarta.jsonp.2.1_fat_tck";
        String testName = this.getClass() + ":launchJsonp21TCK";
        Type type = Type.JAKARTA;
        String specName = "JSON Processing";
        TCKRunner.runTCK(DONOTSTART, bucketName, testName, type, specName, additionalProps);
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchJsonp21PluggabilityTCK() throws Exception {

        /**
         * The runTCKMvnCmd will set the following properties for use by arquillian
         * [ wlp, tck_server, tck_port, tck_failSafeUndeployment, tck_appDeployTimeout, tck_appUndeployTimeout ]
         * and then run the mvn test command.
         */
        // Including jakarta.json-tck-tests and jakarta.json-tck-tests-pluggability together causes
        // exceptions due to collisions, so created 2 separate profiles which are then
        // run individually
        Map<String, String> additionalPluggabilityProps = new HashMap<>();
        additionalPluggabilityProps.put("run-tck-tests-pluggability", "true");

        String bucketName = "io.openliberty.jakarta.jsonp.2.1_fat_tck";
        String testName = this.getClass() + ":launchJsonp21PluggabilityTCK";
        Type type = Type.JAKARTA;
        String specName = "JSON Processing";
        TCKRunner.runTCK(DONOTSTART, bucketName, testName, type, specName, additionalPluggabilityProps);
    }
}