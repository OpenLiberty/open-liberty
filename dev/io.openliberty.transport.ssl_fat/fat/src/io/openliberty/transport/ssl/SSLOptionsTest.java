/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Paths;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to verify the SSL options are correctly parsed and used in
 * the server http endpoint. We test updating sslOptions in an endpoint
 * correctly works and we verify suppressHandhake is properly set.
 */
@RunWith(FATRunner.class)
public class SSLOptionsTest{

    private static final Logger LOG = Logger.getLogger(SSLOptionsTest.class.getName());

    private static final String SUPPRESS_HANDSHAKE_FAILURE_FALSE_CONFIG = "suppressHandshakeFailureFalse.xml";
    private static final String SUPPRESS_HANDSHAKE_FAILURE_TRUE_CONFIG = "suppressHandshakeFailureTrue.xml";
    private static final String SUPPRESS_HANDSHAKE_FAILURE_LOW_COUNT_CONFIG = "suppressHandshakeFailureLowCount.xml";
    private static final String DEFAULT_SSLOPTIONS_CONFIG = "defaultSSLOptions.xml";
    private static final String SSLOPTIONS_CONFIG = "SSLOptions.xml";

    private static final String DEFAULT_REALM = "Basic Authentication";
    private static final String DEFAULT_SERVLET_NAME = "ServletName: BasicAuthServlet";
    private static final String DEFAULT_CONTEXT_ROOT = "/basicauth";

    private static final String KEYSTORE = "sslOptions.jks";
    private static final String TRUSTSTORE = "trust.jks";
    private static final String ALTERNATE_TRUSTSTORE = "optionsTrust.jks";
    private static final String PASSWORD = "Liberty";

    @Server("SSLOptionsServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        /*
         * Transform apps for EE9+.
         */
        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(Paths.get(server.getServerRoot() + "/apps/basicauth.war"));
        }

        // The app basicauth is added in the config and we validate it was actually installed
        server.addInstalledAppForValidation("basicauth");

        server.startServer(SSLOptionsTest.class.getSimpleName() + ".log");
        // Wait for SSL endpoint to start
        assertNotNull("We need to wait for the SSL port to open",
                      server.waitForStringInLog("CWWKO0219I:.*-ssl"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Ignoring unrelated warning due to app install SRVE0272W: JSP Processor not defined. Skipping : BasicAuthJSP.jsp
        // Ignore CWWKO0801E because it is expected for handshake failures
        // Server occasionally fails on stop with CWPKI0024E due to default certificate alias specified by the attribute serverKeyAlias 
        // is either not found in KeyStore or is invalid. The trust store contains the alias and this doesn't affect the tests
        // since the logic still functions appropriately so we can ignore this.
        server.stopServer("SRVE0272W", "CWWKO0801E", "CWPKI0024E");
    }

    /**
     * Save the server configuration before each test, this should be the default server
     * configuration.
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);
        
    }

    /**
     * Restore the server configuration to the default state after each test.
     *
     * @throws Exception
     */
    @After
    public void afterTest() throws Exception {
        // Restore the server to the default state.
        server.setMarkToEndOfLog();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(null);
        if(server.findStringsInLogsUsingMark("CWWKG0018I", server.getDefaultLogFile()).size() == 0) { // Server configuration was updated so need to wait for ssl port
            assertNotNull("We need to wait for the SSL port to open after config update",
                      server.waitForStringInLogUsingMark("CWWKO0219I:.*-ssl"));
        }
        
    }

    /**
     * Test that an SSL handshake failure does get logged with the default
     * sslOptions value suppressHandshakeErrors="false".
     */
    @Test
    public void handshakeFailureGetsLogged() throws Exception {
        LOG.info("Entering handshakeFailureGetsLogged");

        // Hit the servlet on the SSL port
        hitServerWithBadHandshake();
        assertNotNull("Handshake error failure unexpectedly not logged",
                      server.waitForStringInLogUsingMark("CWWKO0801E"));

        LOG.info("Exiting handshakeFailureGetsLogged");
    }

    /**
     * Test that an SSL handshake failure does not get logged when
     * suppressHandshakeErrors="true".
     */
    @Test
    public void handshakeFailureIsNotLogged() throws Exception {
        LOG.info("Entering handshakeFailureIsNotLogged");

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(SUPPRESS_HANDSHAKE_FAILURE_TRUE_CONFIG);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Requires info trace
        assertNotNull("We need to wait for the SSL port to open",
                      server.waitForStringInLogUsingMark("CWWKO0219I:.*-ssl"));

        // Hit the servlet on the SSL port
        hitServerWithBadHandshake();
        assertNull("Handshake error failure was unexpectedly logged after 5 seconds",
                   server.waitForStringInLogUsingMark("CWWKO0801E", 5000));

        LOG.info("Exiting handshakeFailureIsNotLogged");
    }

    /**
     * Test that suppression of the SSL handshake logging setting is properly
     * updated dynamically.
     */
    @Test
    public void dynamicUpdateToSuppression() throws Exception {
        LOG.info("Entering dynamicUpdateToSuppression");
        int saveCnt = 0;

        // SUPPRESS OFF BY DEFAULT

        // Hit the servlet on the SSL port
        server.setMarkToEndOfLog();
        hitServerWithBadHandshake();
        assertNotNull("Handshake error failure unexpectedly not logged (first time)",
                      server.waitForStringInLogUsingMark("CWWKO0801E"));

        // SUPPRESS ON
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(SUPPRESS_HANDSHAKE_FAILURE_TRUE_CONFIG);
        server.waitForConfigUpdateInLogUsingMark(null);

        assertNotNull("We need to wait for the SSL port to start (again)",
                      server.waitForStringInLogUsingMark("CWWKO0219I:.*-ssl"));
        saveCnt = server.findStringsInLogs("CWWKO0801E").size();

        // Hit the servlet on the SSL port
        server.setMarkToEndOfLog();
        hitServerWithBadHandshake();
        assertEquals("We should not see any new failure messages logged",
                     saveCnt, server.findStringsInLogs("CWWKO0801E").size());

        // SUPPRESS OFF
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(SUPPRESS_HANDSHAKE_FAILURE_FALSE_CONFIG);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Requires info trace
        assertNotNull("We need to wait for the SSL port to start (again)",
                      server.waitForStringInLogUsingMark("CWWKO0219I:.*-ssl"));

        // Hit the servlet on the SSL port
        server.setMarkToEndOfLog();
        hitServerWithBadHandshake();
        assertNotNull("Handshake error failure unexpectedly not logged (second time)",
                      server.waitForStringInLogUsingMark("CWWKO0801E"));

        LOG.info("Exiting dynamicUpdateToSuppression");
    }

    /**
     * Test that repeated SSL handshake failures have a log cap,
     * and that the attribute is honored.
     */
    @Test
    public void handshakeFailuresHaveLogCap() throws Exception {
        LOG.info("Entering handshakeFailuresHaveLogCap");

        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(SUPPRESS_HANDSHAKE_FAILURE_LOW_COUNT_CONFIG);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Requires info trace
        assertNotNull("We need to wait for the SSL port to open",
                      server.waitForStringInLogUsingMark("CWWKO0219I:.*-ssl"));
        server.setMarkToEndOfLog();

        // Hit the servlet on the SSL port
        hitServerWithBadHandshake();
        hitServerWithBadHandshake();
        hitServerWithBadHandshake();
        hitServerWithBadHandshake();
        hitServerWithBadHandshake();
        hitServerWithBadHandshake();
        assertNotNull("Handshake error failure unexpectedly not logged",
                      server.waitForStringInLogUsingMark("CWWKO0801E"));
        assertEquals("Should only find 2 CWWKO0801E log entries",
                     2, server.findStringsInLogsUsingMark("CWWKO0801E", server.getDefaultLogFile()).size());
        assertNotNull("Expected informational message that logging has stopped was not logged",
                      server.waitForStringInLogUsingMark("CWWKO0804I"));
        assertEquals("Should only find 1 CWWKO0804I log entry",
                     1, server.findStringsInLogsUsingMark("CWWKO0804I", server.getDefaultLogFile()).size());

        LOG.info("Exiting handshakeFailuresHaveLogCap");
    }

    /**
     * Test that SSL Configuration dynamically updated on the SSLOption is
     * used. Start with an SSLOption set to the default SSL Config and make
     * sure it can be accessed. Dynamically update the SSLOption to point
     * to an alternate SSL Configuration and make sure you access it with
     * the appropriate trust.
     * 
     * We allow an IllegalArgumentException FFDC because occasionally the tests
     * produce an error CWPKI0024E loading the keystore which do not affect the
     * test at all.
     */
    @Test
    @AllowedFFDC("java.lang.IllegalArgumentException")
    public void dynamicUpdateToSSLOption() throws Exception {
        LOG.info("Entering dynamicUpdateToSSLOption");

        // Use default SSL Config
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(DEFAULT_SSLOPTIONS_CONFIG);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Requires info trace
        assertNotNull("We need to wait for the SSL port to open (first time)",
                      server.waitForMultipleStringsInLogUsingMark(2, "CWWKO0219I:.*-ssl"));

        // Hit the servlet on the SSL port
        hitServer(KEYSTORE, PASSWORD, TRUSTSTORE, PASSWORD);

        // Swith to SSLConfig specified on the SSLOptions
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(SSLOPTIONS_CONFIG);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Requires info trace
        assertNotNull("We need to wait for the SSL port to start (again)",
                      server.waitForMultipleStringsInLogUsingMark(2, "CWWKO0219I:.*-ssl"));

        // Hit the servlet on the SSL port
        hitServer(KEYSTORE, PASSWORD, ALTERNATE_TRUSTSTORE, PASSWORD);

        LOG.info("Exiting dynamicUpdateToSSLOption");
    }

    /**
     * Hit the server and make sure its not authenticated. This will trigger
     * a handshake error which is what we're testing for.
     * 
     * @throws Exception
     */
    private void hitServerWithBadHandshake() throws Exception {
        // Hit the servlet with HTTPS on the SSL port and validate it fails
        SSLBasicAuthClient sslClient = new SSLBasicAuthClient(server);
        try {
            sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
            fail("Should not be able to connect to HTTPS port as there is no trust");
        } catch (java.lang.AssertionError t) {
            assertTrue("Failure message did not contain expected 'peer not " +
                       "authenticated' message, message was: " + t.getMessage(),
                       t.getMessage().matches(".*: peer not authenticated"));
        }
    }

    /**
     * Hit the server and make sure its not authenticated. This will trigger
     * a handshake error which is what we're testing for.
     */
    private void hitServer(String ksFile, String ksPassword, String tsFile, String tsPassword) {
        String keystore = server.getPathToAutoFVTNamedServer() + "resources" + File.separator + "security" + File.separator + ksFile;
        String truststore = server.getPathToAutoFVTNamedServer() + "resources" + File.separator + "security" + File.separator + tsFile;
        // Hit the servlet with HTTPS on the SSL port and validate it fails
        SSLBasicAuthClient sslClient = new SSLBasicAuthClient(server, DEFAULT_REALM, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT,
                        keystore, ksPassword, truststore, tsPassword);
        String response = sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        assertTrue("Did not get the expected response",
                   sslClient.verifyUnauthenticatedResponse(response));
    }


}
