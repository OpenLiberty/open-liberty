/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal.fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.reporting.internal.fat.response.CVEReportingResponseEndpoints;

/*
 * These tests are run using two different servers.
 * APP_SERVER runs the app (CVEReporting feature is disabled for this server)
 * TEST_SERVER runs the tests (CVEReporting feature is enabled for this server)
 *
 * The reason for this is that because the CVE Reporting feature runs in the kernel the app was not
 * starting quick enough for the endpoints to be available to POST the data.
 */

@RunWith(FATRunner.class)
public class CVEResponseTest extends FATServletClient {

    public static final String APP_SERVER_NAME = "io.openliberty.reporting.response.app.server";
    public static final String TEST_SERVER_NAME = "io.openliberty.reporting.response.test.server";
    public static final String APP_NAME = "CVEReportingResponseEndpoints";

    private static final String STORE_PATH = "resources/security";
    private static final String KEYSTORE_FILENAME = "key.p12";
    private static final String NEW_KEYSTORE_FILENAME = "NewKeyStore.jks";
    private static final String NEW_TRUSTSTORE_FILENAME = "NewTrustStore.jks";
    private static final String STORE_PASSWORD = "Liberty";

    protected static final Class<?> c = CVEResponseTest.class;

    @Server(APP_SERVER_NAME)
    public static LibertyServer server;

    @Server(TEST_SERVER_NAME)
    public static LibertyServer testServer;

    /*
     * You must set the following in the server.xml when calling
     * useSecondaryHTTPPort()
     *
     * <httpEndpoint id="defaultHttpEndpoint" host="*"
     * httpPort="${bvt.prop.HTTP_secondary}"
     * httpsPort="${bvt.prop.HTTP_secondary.secure}"/>
     */

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackage(CVEReportingResponseEndpoints.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, app, SERVER_ONLY);
        testServer.useSecondaryHTTPPort();
        testServer.saveServerConfiguration();

    }

    @After
    public void tearDown() throws Exception {

        try {
            if (server.isStarted()) {
                server.stopServer();
            }
        } finally {
            if (testServer.isStarted()) {
                testServer.stopServer();
            }
        }
        testServer.restoreServerConfiguration();
    }

    public static void copyTrustStore(LibertyServer server, LibertyServer testServer) throws Exception {
        testServer.copyFileToLibertyServerRoot(server.getServerRoot() + "/" + STORE_PATH, STORE_PATH,
                                               KEYSTORE_FILENAME);
    }

    /**
     * This test makes a call to the /getResponse after the CVE reporting feature
     * runs
     *
     * @throws Exception
     */
    @Test
    public void testConnectionAndDataRecorded() throws Exception {
        server.startServer();
        server.waitForSSLStart();
        copyTrustStore(server, testServer);
        testServer.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true", "-Dcve.insight.enabled=true",
                                               "-Djavax.net.ssl.trustStore=" + testServer.getServerRoot() + "/resources/security/key.p12",
                                               "-Djavax.net.ssl.trustStorePassword=password", "-Djavax.net.ssl.trustStoreType=PKCS12"));
        testServer.startServer();
        testServer.addIgnoredErrors(Collections.singletonList("CWWKF1702W"));

        assertNotNull("CVE Reporting checks not been carried out", testServer.waitForStringInLog("CWWKF1700I"));

        server.waitForStringInLog("POST COMPLETED");

        JsonObject json = new HttpRequest(server, "/CVEReportingResponseEndpoints/endpoints/getResponse").run(JsonObject.class);

        List<String> features = new ArrayList<>();

        for (int i = 0; i < json.getJsonArray("features").size(); i++) {
            features.add(json.getJsonArray("features").getString(i));
        }

        String featuresAsString = Arrays.toString(features.toArray());

        assertTrue("The property 'id' is not null or empty",
                   json.getString("id") != null && !json.getString("id").isEmpty());

        String[] productEdition = new String[] { "Core", "CORE", "BASE", "DEVELOPERS", "EXPRESS", "BLUEMIX",
                                                 "EARLY_ACCESS", "zOS", "ND", "BASE_ILAN", "Open" };

        assertThat("The property 'productEdition' did not match", json.getString("productEdition"),
                   Matchers.isIn(productEdition));

        assertTrue("The property 'productVersion' did not match at the start",
                   json.getString("productVersion").matches("^\\d\\d\\..*"));

        assertTrue("The property 'productVersion' did not match at the end",
                   json.getString("productVersion").matches("^\\d\\d.0.0.([1-9]|1[0123])$"));

        String javaVendor = System.getProperty("java.vendor").toLowerCase();

        if (javaVendor == null) {
            javaVendor = System.getProperty("java.vm.name", "unknown").toLowerCase();
        }

        assertEquals("The property 'os' did not match", System.getProperty("os.name"), json.getString("os"));

        assertEquals("The property 'osArch' did not match", System.getProperty("os.arch"), json.getString("osArch"));

        assertEquals("The property 'javaVendor' did not match", javaVendor, json.getString("javaVendor"));

        assertEquals("The property 'javaVersion' did not match", System.getProperty("java.runtime.version"),
                     json.getString("javaVersion"));

        assertTrue("The property 'features' are incorrect", featuresAsString.contains("componenttest-")
                                                            && featuresAsString.contains("jsonb-") && featuresAsString.contains("mpRestClient-"));

        assertTrue("The property 'iFixes' is not empty", json.getJsonArray("iFixes").isEmpty());

    }

    /**
     * Testing that the CVE Reporting feature successfully receives the correct
     * response from the cloud service and is the able to successfully parse that
     * information and output it into the logs for the users.
     *
     * @throws Exception
     */
    @Test
    public void testResponse() throws Exception {
        server.startServer();
        server.waitForSSLStart();
        copyTrustStore(server, testServer);
        testServer.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true", "-Dcve.insight.enabled=true",
                                               "-Djavax.net.ssl.trustStore=" + testServer.getServerRoot() + "/resources/security/key.p12",
                                               "-Djavax.net.ssl.trustStorePassword=password", "-Djavax.net.ssl.trustStoreType=PKCS12"));
        testServer.startServer();
        testServer.addIgnoredErrors(Collections.singletonList("CWWKF1702W"));

        assertNotNull("CVE Reporting checks not been carried out", testServer.waitForStringInLog("CWWKF1700I"));

        server.waitForStringInLog("POST COMPLETED");

        assertNotNull("Response message for Open Liberty is incorrect",
                      testServer.waitForStringInTrace("CWWKF1702W: Based on an assessment of Open Liberty,"));
        assertNotNull("First CVE for Open Liberty is incorrect",
                      testServer.waitForStringInTrace("https://www.ibm.com/support/pages/node/7125527 - CVE-2023-50312"));
        assertNotNull("Second CVE for Open Liberty is incorrect",
                      testServer.waitForStringInTrace("https://www.ibm.com/support/pages/node/7125528 - CVE-2023-50313"));
        assertNotNull("Response message for Open Liberty is incorrect",
                      testServer.waitForStringInTrace("CWWKF1702W: Based on an assessment of IBM Semeru Java,"));
        assertNotNull("CVE for IBM Semeru Java is incorrect", testServer.waitForStringInTrace(
                                                                                              "https://www.ibm.com/support/pages/node/7125529 - CVE-2023-50314, CVE-2023-50315"));
    }

    /**
     * Testing that we will be able to make a successful connection with the cloud service if the SSL feature is enabled
     *
     * @throws Exception
     */
    @Test
    public void testWithSslFeatureEnabled() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        features.add("ssl-1.0");
        server.updateServerConfiguration(config);
        server.startServer();
        server.waitForSSLStart();
        copyTrustStore(server, testServer);
        Process createKeyStore = Runtime.getRuntime().exec(getCertGenerationCommand(testServer.getServerRoot() + "/" + STORE_PATH + "/" + NEW_KEYSTORE_FILENAME, STORE_PASSWORD));
        createKeyStore.waitFor();
        Process createTrustStore = Runtime.getRuntime().exec(getCertGenerationCommand(testServer.getServerRoot() + "/" + STORE_PATH + "/" + NEW_TRUSTSTORE_FILENAME,
                                                                                      STORE_PASSWORD));
        createTrustStore.waitFor();
        testServer.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true", "-Dcve.insight.enabled=true",
                                               "-Djavax.net.ssl.trustStore=" + testServer.getServerRoot() + "/resources/security/key.p12",
                                               "-Djavax.net.ssl.trustStorePassword=password", "-Djavax.net.ssl.trustStoreType=PKCS12"));
        testServer.startServer();
        testServer.addIgnoredErrors(Collections.singletonList("CWWKF1702W"));

        assertNotNull("CVE Reporting checks not been carried out", testServer.waitForStringInLog("CWWKF1700I"));

        server.waitForStringInLog("POST COMPLETED");

        assertNotNull("The connection was not successful with a customer user trust store", testServer.waitForStringInLog("CWWKF1702W:.*"));
    }

    /**
     * Testing that we will not be able to make successful connection if the user specifies their own cert store
     *
     * @throws Exception
     */
    @Test
    public void testTrustWithInvalidCertificate() throws Exception {
        server.startServer();
        server.waitForSSLStart();
        testServer.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true", "-Dcve.insight.enabled=true"));
        testServer.startServer();
        List<String> warnings = Arrays.asList("CWWKF1702W", "CWWKF1705W");
        testServer.addIgnoredErrors(warnings);

        assertNotNull("CVE Reporting checks not been carried out", testServer.waitForStringInLog("CWWKF1700I"));

        assertNotNull("The connection was made without a trust store", testServer.waitForStringInLog("CWWKF1705W:.*"));

    }

    /**
     * Test that we will be able to make a successful connection with the cloud if transportSecurity feature is enabled.
     *
     * @throws Exception
     */
    @Test
    public void testWithTransportSecurityEnabled() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        features.add("transportSecurity-1.0");
        server.updateServerConfiguration(config);
        server.startServer();
        server.waitForSSLStart();
        copyTrustStore(server, testServer);
        Process createKeyStore = Runtime.getRuntime().exec(getCertGenerationCommand(testServer.getServerRoot() + "/" + STORE_PATH + "/" + NEW_KEYSTORE_FILENAME, STORE_PASSWORD));
        createKeyStore.waitFor();
        Process createTrustStore = Runtime.getRuntime().exec(getCertGenerationCommand(testServer.getServerRoot() + "/" + STORE_PATH + "/" + NEW_TRUSTSTORE_FILENAME,
                                                                                      STORE_PASSWORD));
        createTrustStore.waitFor();
        testServer.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true", "-Dcve.insight.enabled=true",
                                               "-Djavax.net.ssl.trustStore=" + testServer.getServerRoot() + "/resources/security/key.p12",
                                               "-Djavax.net.ssl.trustStorePassword=password", "-Djavax.net.ssl.trustStoreType=PKCS12"));
        testServer.startServer();
        testServer.addIgnoredErrors(Collections.singletonList("CWWKF1702W"));

        assertNotNull("CVE Reporting checks not been carried out", testServer.waitForStringInLog("CWWKF1700I"));

        server.waitForStringInLog("POST COMPLETED");

        assertNotNull("The connection was not successful with a customer user trust store", testServer.waitForStringInLog("CWWKF1702W:.*"));

    }

    /**
     * Testing that if a server that does not exist is provided there will be no
     * connection made and the correct warning message will be outputted to the logs
     *
     * @throws Exception
     */
    @Test
    public void testNonExistingServer() throws Exception {
        ServerConfiguration config = testServer.getServerConfiguration();
        config.getCVEReporting().setUrlLink("https://localhost:65535/does/not/exist");
        testServer.updateServerConfiguration(config);
        testServer.startServer();
        testServer.addIgnoredErrors(Collections.singletonList("CWWKF1705W"));

        assertNotNull("A connection has been made", testServer.waitForStringInLog("CWWKF1705W:.*"));

    }

    /**
     * testing that the feature wont be able to make a connection to services with
     * incorrect protocols.
     *
     * @throws Exception
     */
    @Test
    public void testIncorrectProtocol() throws Exception {
        ServerConfiguration config = testServer.getServerConfiguration();
        config.getCVEReporting().setUrlLink("http://localhost:65535/does/not/exist");
        testServer.updateServerConfiguration(config);
        testServer.startServer();
        testServer.addIgnoredErrors(Collections.singletonList("CWWKF1704W"));

        assertNotNull("The MalformedURLException has not been thrown", testServer.waitForStringInLog("CWWKF1704W:.*"));

    }

    private static String getCertGenerationCommand(String filepath, String password) {
        List<String> cmd = Arrays.asList("keytool",
                                         "-genkey",
                                         "-keystore", filepath,
                                         "-storetype", "jks",
                                         "-storepass", password,
                                         "-keypass", password,
                                         "-validity", "30",
                                         "-dname", "CN=testCVEReporting",
                                         "-sigalg", "SHA256withRSA",
                                         "-keyalg", "RSA",
                                         "-keysize", "2048");

        return String.join(" ", cmd);
    }

}
