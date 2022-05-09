/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakarta.jsonp.tck;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

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

    final static Map<String, String> additionalProps = new HashMap<>();

    //This is a standalone test no server needed
    @Server
    public static LibertyServer DONOTSTART;

    @BeforeClass
    public static void setUp() throws Exception {
        // Skip signature testing on Windows and Semeru JDK.
        // So far as I can tell the signature test plugin is not supported on this configuration
        //Opened an issue against jsonb tck https://github.com/eclipse-ee4j/jsonb-api/issues/327
        // Also skip the JsonProviderTest, because if any other test has already run before, the provider has been set
        // and the test fails because you can't then set a different provider
        // Opened an issue against the jsonp tck https://github.com/eclipse-ee4j/jsonp/issues/376
        if (System.getProperty("os.name").contains("Windows") &&
            System.getProperty("java.runtime.name").contains("Semeru")) {
            Log.info(JsonpTckLauncher.class, "setUp", "Skipping JSONP Signature Test on Windows and Semeru JDK");
            additionalProps.put("exclude.tests", "ee.jakarta.tck.jsonp.signaturetest.jsonp.JSONPSigTest.java,ee.jakarta.tck.jsonp.api.provider.JsonProviderTest.java");
        } else {
            additionalProps.put("exclude.tests", "ee.jakarta.tck.jsonp.api.provider.JsonProviderTest.java");
        }

    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchJsonpTCK() throws Exception {

        Map<String, String> resultInfo = MvnUtils.getResultInfo(DONOTSTART);

        /**
         * The runTCKMvnCmd will set the following properties for use by arquillian
         * [ wlp, tck_server, tck_port, tck_failSafeUndeployment, tck_appDeployTimeout, tck_appUndeployTimeout ]
         * and then run the mvn test command.
         */
        int result = MvnUtils.runTCKMvnCmd(
                                           DONOTSTART, //server to run on
                                           "io.openliberty.jakarta.jsonp.2.1_fat_tck", //bucket name
                                           this.getClass() + ":launchJsonpTCK", //launching method
                                           null, //suite file to run
                                           additionalProps, //additional props
                                           Collections.emptySet() //additional jars
        );

        resultInfo.put("results_type", "Jakarta");
        resultInfo.put("feature_name", "jsonp");
        resultInfo.put("feature_version", "2.1");
        MvnUtils.preparePublicationFile(resultInfo);
        assertEquals(0, result);
    }
}