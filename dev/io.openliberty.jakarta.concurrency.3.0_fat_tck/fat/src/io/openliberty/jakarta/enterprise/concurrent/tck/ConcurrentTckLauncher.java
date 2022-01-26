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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import componenttest.topology.impl.JavaInfo;
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

        try{
            Map<String, String> resultInfo = new HashMap<>();
            JavaInfo javaInfo = JavaInfo.forCurrentVM();
            String productVersion = "";
            resultInfo.put("results_type", "Jakarta EE");
            resultInfo.put("java_info", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") +')');
            resultInfo.put("java_major_version", String.valueOf(javaInfo.majorVersion()));
            resultInfo.put("feature_name", "Concurrency");
            resultInfo.put("feature_version", "3.0");
            resultInfo.put("os_name",System.getProperty("os.name"));
            List<String> matches = server.findStringsInLogs("product =");
            if(!matches.isEmpty()){
                Pattern olVersionPattern = Pattern.compile("Liberty (.*?) \\(", Pattern.DOTALL);
                Matcher nameMatcher =olVersionPattern.matcher(matches.get(0));
                if (nameMatcher.find()) {
                    productVersion = nameMatcher.group(1);
                }
                resultInfo.put("product_version", productVersion);
            }
                MvnUtils.preparePublicationFile(resultInfo);
            }
            finally{
                assertEquals(0, result);
            }
    }
}