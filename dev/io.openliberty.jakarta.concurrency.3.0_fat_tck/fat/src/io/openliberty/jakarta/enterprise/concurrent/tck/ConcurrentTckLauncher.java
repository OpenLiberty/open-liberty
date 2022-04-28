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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlSuite.FailurePolicy;
import org.testng.xml.XmlTest;

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

    private static String suiteXmlFile = "tck-suite.xml"; //Default value

    @Server("ConcurrentTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        //UNCOMMENT - To test against a local snapshot of TCK
        //additionalProps.put("jakarta.concurrent.tck.groupid", "jakarta.enterprise.concurrent");
        //additionalProps.put("jakarta.concurrent.tck.version", "3.0.0-SNAPSHOT");

        suiteXmlFile = createSuiteXML();

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

    /**
     * Programmatically create a tck-suite-programmatic.xml file based on environment variables
     *
     * @return String suite file name
     * @throws IOException if we are unable to write contents of tck-suite-programmatic.xml to filesystem
     */
    public static String createSuiteXML() throws IOException {
        XmlSuite suite = new XmlSuite();
        suite.setFileName("tck-suite-programmatic.xml");
        suite.setName("jakarta-concurrency");
        suite.setVerbose(2);
        suite.setConfigFailurePolicy(FailurePolicy.CONTINUE);

        XmlTest test = new XmlTest(suite);
        test.setName("jakarta-concurrency-programmatic");

        XmlPackage apiPackage = new XmlPackage();
        XmlPackage specPackage = new XmlPackage();

        apiPackage.setName("ee.jakarta.tck.concurrent.api.*");
        specPackage.setName("ee.jakarta.tck.concurrent.spec.*");

        List<String> apiExcludes = new ArrayList<>();
        List<String> specExcludes = new ArrayList<>();

        /**
         * Exclude certain tests when running in lite mode
         */
        if (TestModeFilter.FRAMEWORK_TEST_MODE != Mode.TestMode.FULL) {
            Log.info(ConcurrentTckLauncher.class, "createSuiteXML", "Modifying API and Spec packages to exclude specific tests for lite mode.");
            apiExcludes.add("ee.jakarta.tck.concurrent.api.Trigger");
            specExcludes.addAll(Arrays.asList("ee.jakarta.tck.concurrent.spec.ManagedScheduledExecutorService.inheritedapi",
                                              "ee.jakarta.tck.concurrent.spec.ManagedScheduledExecutorService.inheritedapi_servlet"));
        }

        /**
         * Skip signature testing on Windows
         * So far as I can tell the signature test plugin is not supported on windows
         * Opened an issue against jsonb tck https://github.com/eclipse-ee4j/jsonb-api/issues/327
         */
        if (System.getProperty("os.name").contains("Windows")) {
            Log.info(ConcurrentTckLauncher.class, "createSuiteXML", "Skipping Signature Tests on Windows");
            specExcludes.add("ee.jakarta.tck.concurrent.spec.signature");
        }

        apiPackage.setExclude(apiExcludes);
        specPackage.setExclude(specExcludes);

        test.setPackages(Arrays.asList(apiPackage, specPackage));

        suite.setTests(Arrays.asList(test));

        Log.info(ConcurrentTckLauncher.class, "createSuiteXML", suite.toXml());

        //When this code runs it is running as part of an ant task already in the autoFVT directory.
        //Therefore, use a relative path to this file.
        String suiteXmlFileLocation = "publish/tckRunner/tck/" + suite.getFileName();
        try (FileWriter suiteXmlWriter = new FileWriter(suiteXmlFileLocation);) {
            suiteXmlWriter.write(suite.toXml());
            Log.info(ConcurrentTckLauncher.class, "createSuiteXML", "Wrote to " + suiteXmlFileLocation);
        }

        return suite.getFileName();
    }
}