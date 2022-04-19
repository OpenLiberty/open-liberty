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
package io.openliberty.jakarta.enterprise.concurrent.tck;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs the whole Jakarta Concurrency TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ConcurrentTckLauncher {

    final static Map<String, String> additionalProps = new HashMap<>();

    @Server("ConcurrentTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        //UNCOMMENT - To test against a local snapshot of TCK
        //additionalProps.put("jakarta.concurrent.tck.groupid", "jakarta.enterprise.concurrent");
        //additionalProps.put("jakarta.concurrent.tck.version", "3.0.0-SNAPSHOT");

        //username and password for Arquillian to authenticate to restConnect
        additionalProps.put("tck_username", "arquillian");
        additionalProps.put("tck_password", "arquillianPassword");

        //Logging properties for java.util.logging to use for mvn output
        additionalProps.put("java.util.logging.config.file", server.getServerRoot() + "/resources/logging/logging.properties");

        //username and password to set on quickStartSecurity
        server.addEnvVar("tck_username", "arquillian");
        server.addEnvVar("tck_password", "arquillianPassword");

        //Ports liberty should be using for testing
        server.addEnvVar("tck_port", "" + server.getPort(PortType.WC_defaulthost));
        server.addEnvVar("tck_port_secure", "" + server.getPort(PortType.WC_defaulthost_secure));

        Map<String, String> opts = server.getJvmOptionsAsMap();
        //Path that jimage will output modules for signature testing
        opts.put("-Djimage.dir", server.getServerSharedPath() + "jimage/output/");
        server.setJvmOptions(opts);

        //Finally start the server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "WLTC0032W", //Transaction rollback warning.
                          "WLTC0033W", //Transaction rollback warning.
                          "CWWKS0901E" //Quickstart security
        );
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchConcurrentTCK() throws Exception {
        String suiteXmlFile;
        if (TestModeFilter.FRAMEWORK_TEST_MODE == Mode.TestMode.FULL) {
            Log.info(getClass(), "launchConcurrentTCK", "Running full tests");
            suiteXmlFile = "tck-suite-full.xml";
        } else {
            Log.info(getClass(), "launchConcurrentTCK", "Running lite tests");
            suiteXmlFile = "tck-suite-lite.xml";
        }

        //UNCOMMENT - To perform signature testing only
        //suiteXmlFile = "tck-suite-signature.xml";

        Map<String, String> resultInfo = MvnUtils.getResultInfo(server);

        /**
         * The runTCKMvnCmd will set the following properties for use by arquillian
         * [ wlp, tck_server, tck_port, tck_failSafeUndeployment, tck_appDeployTimeout, tck_appUndeployTimeout ]
         * and then run the mvn test command.
         */
        int result = MvnUtils.runTCKMvnCmd(
                                           server, //server to run on
                                           "io.openliberty.jakarta.concurrency.3.0_fat_tck", //bucket name
                                           this.getClass() + ":launchConcurrentTCK", //launching method
                                           suiteXmlFile, //tck suite
                                           additionalProps, //additional props
                                           Collections.emptySet() //additional jars
        );

        resultInfo.put("results_type", "Jakarta");
        resultInfo.put("feature_name", "Concurrency");
        resultInfo.put("feature_version", "3.0");
        MvnUtils.preparePublicationFile(resultInfo);
        assertEquals(0, result);
    }
}