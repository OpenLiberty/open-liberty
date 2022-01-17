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
public class ConcurrentTckLauncher {

    final static Map<String, String> additionalProps = new HashMap<>();

    @Server("ConcurrentTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        //username and password for Arquillian to authenticate to restConnect
        additionalProps.put("tck_username", "arquillian");
        additionalProps.put("tck_password", "arquillianPassword");

        //Keystore and Truststore for Arquillian to perform TLS handshake with Liberty server
        additionalProps.put("javax.net.ssl.keyStore", server.getServerRoot() + "/resources/security/arquillian.p12");
        additionalProps.put("javax.net.ssl.keyStorePassword", "arquillianPassword");
        additionalProps.put("javax.net.ssl.keyStoreType", "pkcs12");
        additionalProps.put("javax.net.ssl.trustStore", server.getServerRoot() + "/resources/security/arquillian.p12");
        additionalProps.put("javax.net.ssl.trustStorePassword", "arquillianPassword");
        additionalProps.put("javax.net.ssl.trustStoreType", "pkcs12");
        //additionalProps.put("javax.net.debug", "ssl:handshake:verbose:keymanager:trustmanager");

        //Logging properties for java.util.logging to use for mvn output
        additionalProps.put("java.util.logging.config.file", server.getServerRoot() + "/resources/logging/logging.properties");

        //username and password to set on quickStartSecurity
        server.addEnvVar("tck_username", "arquillian");
        server.addEnvVar("tck_password", "arquillianPassword");

        //Keystore and Trustore for Liberty to perform TLS handshake with Liberty server
        server.addEnvVar("tck_tls_store", server.getServerRoot() + "/resources/security/liberty.p12");
        server.addEnvVar("tck_tls_password", "libertyPassword");

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
    //@Test //TODO Enable once TCK is published on maven central
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchConcurrentTCK() throws Exception {
        String suiteXmlFile;
        System.out.println("HELLO");
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
        

        MvnUtils.prepareJakartaPublicationFile();
        assertEquals(0, result);
    }
}
