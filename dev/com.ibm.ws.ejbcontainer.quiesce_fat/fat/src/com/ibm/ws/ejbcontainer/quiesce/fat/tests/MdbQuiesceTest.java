/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.quiesce.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that message-driven beans and singleton session beans with deactivateOnQuiesce/destroyOnQuiesce
 * configured are properly deactivated/destroyed during server quiesce / server stop.
 */
@RunWith(FATRunner.class)
public class MdbQuiesceTest extends FATServletClient {

    @Server("ejbcontainer.MdbQuiesceEarServer")
    public static LibertyServer mdbServer;

    @BeforeClass
    public static void setUp() throws Exception {

        // -------------- MdbQuiesceApp.ear ------------
        JavaArchive MdbQuiesceEjb = ShrinkHelper.buildJavaArchive("MdbQuiesceEjb.jar", "io.openliberty.ejbcontainer.fat.mdb.quiesce.ejb");
        ShrinkHelper.addDirectory(MdbQuiesceEjb, "test-applications/MdbQuiesceEjb.jar/resources");
        WebArchive MdbQuiesceWeb = ShrinkHelper.buildDefaultApp("MdbQuiesceWeb.war", "io.openliberty.ejbcontainer.fat.mdb.quiesce.web");
        ShrinkHelper.addDirectory(MdbQuiesceWeb, "test-applications/MdbQuiesceWeb.war/resources");
        EnterpriseArchive MdbQuiesceApp = ShrinkWrap.create(EnterpriseArchive.class, "MdbQuiesceApp.ear");
        MdbQuiesceApp.addAsModules(MdbQuiesceEjb, MdbQuiesceWeb);
        ShrinkHelper.exportAppToServer(mdbServer, MdbQuiesceApp, DeployOptions.SERVER_ONLY);
        mdbServer.addInstalledAppForValidation("MdbQuiesceApp");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (mdbServer != null && mdbServer.isStarted()) {
            mdbServer.stopServer();
        }
    }

    /**
     * Test that message-driven beans and singleton beans in an EAR are properly deactivated/destroyed during quiesce.
     * Verifies that all activation and deactivation messages are logged in the correct order.
     */
    @Test
    public void testMdbQuiesceInEar() throws Exception {
        try {
            mdbServer.startServer();

            // Wait for all MDBs to be bound to their activation specifications
            assertNotNull("MDB MdbQuiesceDefault in MdbQuiesceEjb not bound to activation specification",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceDefault.*MdbQuiesceEjb"));
            assertNotNull("MDB MdbQuiesceBnd in MdbQuiesceEjb not bound to activation specification",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceBnd.*MdbQuiesceEjb"));
            assertNotNull("MDB MdbQuiesceServer in MdbQuiesceEjb not bound to activation specification",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceServer.*MdbQuiesceEjb"));
            assertNotNull("MDB MdbQuiesceDefault in MdbQuiesceWeb not bound to activation specification",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceDefault.*MdbQuiesceWeb"));
            assertNotNull("MDB MdbQuiesceBnd in MdbQuiesceWeb not bound to activation specification",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceBnd.*MdbQuiesceWeb"));
            assertNotNull("MDB MdbQuiesceServer in MdbQuiesceWeb not bound to activation specification",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceServer.*MdbQuiesceWeb"));

            // Wait for all MDBs to activate (CNTR0180I with module and bean name)
            assertNotNull("MDB MdbQuiesceDefault in MdbQuiesceEjb did not activate",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceEjb.*MdbQuiesceDefault"));
            assertNotNull("MDB MdbQuiesceBnd in MdbQuiesceEjb did not activate",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceEjb.*MdbQuiesceBnd"));
            assertNotNull("MDB MdbQuiesceServer in MdbQuiesceEjb did not activate",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceEjb.*MdbQuiesceServer"));
            assertNotNull("MDB MdbQuiesceDefault in MdbQuiesceWeb did not activate",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceWeb.*MdbQuiesceDefault"));
            assertNotNull("MDB MdbQuiesceBnd in MdbQuiesceWeb did not activate",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceWeb.*MdbQuiesceBnd"));
            assertNotNull("MDB MdbQuiesceServer in MdbQuiesceWeb did not activate",
                          mdbServer.waitForStringInLog("CNTR0180I.*MdbQuiesceWeb.*MdbQuiesceServer"));

            // Expected order of postconstruct messages for startup beans
            List<String> expectedPostConstructs = Arrays.asList //
            ("PostConstruct:MdbQuiesceApp:MdbQuiesceEjb:StartupSingletonQuiesceDefault:",
             "PostConstruct:MdbQuiesceApp:MdbQuiesceEjb:StartupSingletonQuiesceBnd:",
             "PostConstruct:MdbQuiesceApp:MdbQuiesceWeb:StartupSingletonQuiesceDefault:",
             "PostConstruct:MdbQuiesceApp:MdbQuiesceWeb:StartupSingletonQuiesceBnd:");

            // Find all postconstruct messages for MdbQuiesceApp
            List<String> actualPostConstructs = mdbServer.findStringsInLogsUsingMark(".*PostConstruct:MdbQuiesceApp.*", mdbServer.getDefaultLogFile());

            // Verify all expected postconstructs are present in order
            verifyMessagesInOrder(expectedPostConstructs, actualPostConstructs, "PostConstruct", expectedPostConstructs.size());

            // Set mark in log before sending messages to MDBs
            mdbServer.setMarkToEndOfLog();

            // Call servlet to send messages to all MDBs
            runTest(mdbServer, "MdbQuiesceWeb/MdbQuiesceServlet", "sendMessages");

            // Wait for all MDB PostConstruct messages (can occur in any order)
            assertNotNull("MDB MdbQuiesceDefault in MdbQuiesceEjb did not call PostConstruct",
                          mdbServer.waitForStringInLog("PostConstruct:MdbQuiesceApp:MdbQuiesceEjb:MdbQuiesceDefault:"));
            assertNotNull("MDB MdbQuiesceBnd in MdbQuiesceEjb did not call PostConstruct",
                          mdbServer.waitForStringInLog("PostConstruct:MdbQuiesceApp:MdbQuiesceEjb:MdbQuiesceBnd:"));
            assertNotNull("MDB MdbQuiesceServer in MdbQuiesceEjb did not call PostConstruct",
                          mdbServer.waitForStringInLog("PostConstruct:MdbQuiesceApp:MdbQuiesceEjb:MdbQuiesceServer:"));
            assertNotNull("MDB MdbQuiesceDefault in MdbQuiesceWeb did not call PostConstruct",
                          mdbServer.waitForStringInLog("PostConstruct:MdbQuiesceApp:MdbQuiesceWeb:MdbQuiesceDefault:"));
            assertNotNull("MDB MdbQuiesceBnd in MdbQuiesceWeb did not call PostConstruct",
                          mdbServer.waitForStringInLog("PostConstruct:MdbQuiesceApp:MdbQuiesceWeb:MdbQuiesceBnd:"));
            assertNotNull("MDB MdbQuiesceServer in MdbQuiesceWeb did not call PostConstruct",
                          mdbServer.waitForStringInLog("PostConstruct:MdbQuiesceApp:MdbQuiesceWeb:MdbQuiesceServer:"));

            mdbServer.setMarkToEndOfLog();
            mdbServer.stopServer(false);

            // Make sure stop has completed
            assertNotNull("Server " + mdbServer.getServerName() + " FAILED to stop", mdbServer.waitForStringInLog("CWWKE0036I"));

            // Expected JCA deactivation messages (common + J2CA8804I)
            List<String> expectedJcaDeactivation = Arrays.asList //
            ("CWWKE1100I", // waiting for server quiesce
             "J2CA8804I:", // JCA deactivation
             "J2CA8804I:", // JCA deactivation
             "CWWKE1101I", // server quiesce complete
             // Order varies after this point
             "J2CA8804I:", // JCA deactivation
             "J2CA8804I:", // JCA deactivation
             "J2CA8804I:", // JCA deactivation
             "J2CA8804I:", // JCA deactivation
             "CWWKZ0009I"); // application stopped

            // Expected EJB deactivation messages (common + CNTR4014I)
            List<String> expectedEjbDeactivation = Arrays.asList //
            ("CWWKE1100I", // waiting for server quiesce
             "CNTR4014I: The message endpoint for the MdbQuiesceDefault",
             "CNTR4014I: The message endpoint for the MdbQuiesceDefault",
             "CWWKE1101I", // server quiesce complete
             // Order varies after this point
             "CNTR4014I: The message endpoint for the MdbQuiesceServer",
             "CNTR4014I: The message endpoint for the MdbQuiesceBnd",
             "CNTR4014I: The message endpoint for the MdbQuiesceBnd",
             "CNTR4014I: The message endpoint for the MdbQuiesceServer",
             "CWWKZ0009I"); // application stopped

            // Expected PreDestroy messages (common + PreDestroy:)
            List<String> expectedPreDestroy = Arrays.asList //
            ("CWWKE1100I", // waiting for server quiesce
             "PreDestroy:MdbQuiesceApp:MdbQuiesceWeb:StartupSingletonQuiesceBnd:",
             "PreDestroy:MdbQuiesceApp:MdbQuiesceEjb:StartupSingletonQuiesceBnd:",
             "CWWKO0220I: TCP Channel defaultHttpEndpoint", // standard quiesce listener defaultHttpEndpoint
             "CWWKE1101I", // server quiesce complete
             "PreDestroy:MdbQuiesceApp:MdbQuiesceWeb:StartupSingletonQuiesceDefault:",
             "PreDestroy:MdbQuiesceApp:MdbQuiesceEjb:StartupSingletonQuiesceDefault:",
             // Order varies after this point
             "PreDestroy:MdbQuiesceApp:MdbQuiesceWeb:MdbQuiesceDefault:",
             "PreDestroy:MdbQuiesceApp:MdbQuiesceEjb:MdbQuiesceDefault:",
             "PreDestroy:MdbQuiesceApp:MdbQuiesceWeb:MdbQuiesceServer:",
             "PreDestroy:MdbQuiesceApp:MdbQuiesceWeb:MdbQuiesceBnd:",
             "PreDestroy:MdbQuiesceApp:MdbQuiesceEjb:MdbQuiesceBnd:",
             "PreDestroy:MdbQuiesceApp:MdbQuiesceEjb:MdbQuiesceServer:",
             "CWWKZ0009I"); // application stopped

            // Find JCA deactivation messages
            List<String> actualJcaDeactivation = mdbServer.findStringsInLogsUsingMark(".*CWWKE1100I|CWWKE1101I|J2CA8804I|CWWKZ0009I.*",
                                                                                      mdbServer.getDefaultLogFile());

            // Find EJB deactivation messages
            List<String> actualEjbDeactivation = mdbServer.findStringsInLogsUsingMark(".*CWWKE1100I|CWWKE1101I|CNTR4014I|CWWKZ0009I.*",
                                                                                      mdbServer.getDefaultLogFile());

            // Find PreDestroy messages
            List<String> actualPreDestroy = mdbServer.findStringsInLogsUsingMark(".*CWWKE1100I|CWWKO0220I:.*defaultHttpEndpoint|CWWKE1101I|PreDestroy:MdbQuiesceApp|CWWKZ0009I.*",
                                                                                 mdbServer.getDefaultLogFile());

            // Verify all three message sequences are present in order
            verifyMessagesInOrder(expectedJcaDeactivation, actualJcaDeactivation, "JCA Deactivation", 4);
            verifyMessagesInOrder(expectedEjbDeactivation, actualEjbDeactivation, "EJB Deactivation", 4);
            verifyMessagesInOrder(expectedPreDestroy, actualPreDestroy, "PreDestroy", 8);

        } finally {
            if (mdbServer.isStarted()) {
                mdbServer.stopServer();
            } else {
                mdbServer.postStopServerArchive();
            }
        }
    }

    /**
     * Helper method to verify that expected messages appear in the actual messages list in the correct order.
     * Logs diagnostic information if verification fails.
     *
     * @param expectedMessages List of expected message substrings in order
     * @param actualMessages List of actual log messages found
     * @param messageType Description of the message type for error reporting
     */
    private void verifyMessagesInOrder(List<String> expectedMessages, List<String> actualMessages, String messageType, int orderLength) {
        // Extract just the relevant parts from actual messages for comparison
        List<String> extractedMessages = new java.util.ArrayList<>();
        for (String actualMessage : actualMessages) {
            boolean matched = false;
            for (String expected : expectedMessages) {
                if (actualMessage.contains(expected)) {
                    extractedMessages.add(expected);
                    matched = true;
                    break;
                }
            }
            // did not match an expected message; needs to be accounted for anyway
            if (!matched) {
                extractedMessages.add(actualMessage);
            }
        }

        // Check if counts match and log diagnostic info if not
        if (expectedMessages.size() != extractedMessages.size()) {
            System.out.println("========================================");
            System.out.println(messageType + " message count mismatch!");
            System.out.println("Expected " + expectedMessages.size() + " messages but found " + extractedMessages.size());
            System.out.println("========================================");
            System.out.println("Expected messages:");
            for (int i = 0; i < expectedMessages.size(); i++) {
                System.out.println("  [" + i + "] " + expectedMessages.get(i));
            }
            System.out.println("========================================");
            System.out.println("Actual messages found:");
            for (int i = 0; i < extractedMessages.size(); i++) {
                System.out.println("  [" + i + "] " + extractedMessages.get(i));
            }
            System.out.println("========================================");
            System.out.println("All log lines matching pattern:");
            for (int i = 0; i < actualMessages.size(); i++) {
                System.out.println("  [" + i + "] " + actualMessages.get(i));
            }
            System.out.println("========================================");
        }

        // Verify all expected messages were found
        assertEquals(messageType + " messages: Expected " + expectedMessages.size() + " messages but found " + extractedMessages.size(),
                     expectedMessages.size(), extractedMessages.size());

        // Check order and log diagnostic info if mismatch found
        boolean orderMismatch = false;
        for (int i = 0; i < orderLength; i++) {
            if (!expectedMessages.get(i).equals(extractedMessages.get(i))) {
                orderMismatch = true;
                break;
            }
        }

        if (orderMismatch) {
            System.out.println("========================================");
            System.out.println(messageType + " message order mismatch!");
            System.out.println("========================================");
            System.out.println("Expected order:");
            for (int i = 0; i < expectedMessages.size(); i++) {
                System.out.println("  [" + i + "] " + expectedMessages.get(i));
            }
            System.out.println("========================================");
            System.out.println("Actual order:");
            for (int i = 0; i < extractedMessages.size(); i++) {
                System.out.println("  [" + i + "] " + extractedMessages.get(i));
            }
            System.out.println("========================================");
        }

        // Verify the order matches
        for (int i = 0; i < orderLength; i++) {
            assertEquals(messageType + " message at position " + i + " does not match expected order",
                         expectedMessages.get(i), extractedMessages.get(i));
        }

        for (int i = orderLength; i < expectedMessages.size() - 1; i++) {
            assertTrue(messageType + " message at position " + i + " does not match expected PreDestroy, CNTR4014I, or J2CA8804I",
                       (expectedMessages.get(i).startsWith("PreDestroy") ||
                        expectedMessages.get(i).startsWith("CNTR4014I") ||
                        expectedMessages.get(i).startsWith("J2CA8804I")));
        }
    }

}
