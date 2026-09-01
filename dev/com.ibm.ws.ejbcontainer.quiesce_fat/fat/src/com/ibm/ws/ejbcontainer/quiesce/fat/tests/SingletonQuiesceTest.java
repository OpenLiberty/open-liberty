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
import static org.junit.Assert.assertNull;
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
 * Test that singleton session beans with destroyOnQuiesce configured are properly
 * destroyed during server quiesce / server stop.
 */
@RunWith(FATRunner.class)
public class SingletonQuiesceTest extends FATServletClient {

    @Server("ejbcontainer.SingletonQuiesceEarServer")
    public static LibertyServer earServer;

    @Server("ejbcontainer.SingletonQuiesceWarServer")
    public static LibertyServer warServer;

    @BeforeClass
    public static void setUp() throws Exception {

        // -------------- SingletonQuiesce.war ------------
        WebArchive SingletonQuiesceWar = ShrinkHelper.buildDefaultApp("SingletonQuiesce.war", "io.openliberty.ejbcontainer.fat.singleton.quiesce.war");
        ShrinkHelper.addDirectory(SingletonQuiesceWar, "test-applications/SingletonQuiesce.war/resources");
        ShrinkHelper.exportAppToServer(warServer, SingletonQuiesceWar, DeployOptions.SERVER_ONLY);
        warServer.addInstalledAppForValidation("SingletonQuiesce");

        // -------------- SingletonQuiesceApp.ear ------------
        JavaArchive SingletonQuiesceEjb = ShrinkHelper.buildJavaArchive("SingletonQuiesceEjb.jar", "io.openliberty.ejbcontainer.fat.singleton.quiesce.ejb");
        ShrinkHelper.addDirectory(SingletonQuiesceEjb, "test-applications/SingletonQuiesceEjb.jar/resources");
        WebArchive SingletonQuiesceWeb = ShrinkHelper.buildDefaultApp("SingletonQuiesceWeb.war", "io.openliberty.ejbcontainer.fat.singleton.quiesce.web");
        ShrinkHelper.addDirectory(SingletonQuiesceWeb, "test-applications/SingletonQuiesceWeb.war/resources");
        EnterpriseArchive SingletonQuiesceApp = ShrinkWrap.create(EnterpriseArchive.class, "SingletonQuiesceApp.ear");
        SingletonQuiesceApp.addAsModules(SingletonQuiesceEjb, SingletonQuiesceWeb);
        ShrinkHelper.exportAppToServer(earServer, SingletonQuiesceApp, DeployOptions.SERVER_ONLY);
        earServer.addInstalledAppForValidation("SingletonQuiesceApp");
    }

    // CNTR0120W: invalid boolean value in ibm-ejb-jar-bnd.xml or server.xml (StartupSingletonQuiesceInvalidBnd)
    // CWNEN0014W: invalid boolean value in ejb-jar.xml (StartupSingletonQuiesceInvalid)
    private static final String[] EAR_EXPECTED_WARNINGS = { "CNTR0120W", "CWNEN0014W" };

    @AfterClass
    public static void cleanUp() throws Exception {
        if (earServer != null && earServer.isStarted()) {
            earServer.stopServer(EAR_EXPECTED_WARNINGS);
        }
        if (warServer != null && warServer.isStarted()) {
            warServer.stopServer();
        }
    }

    /**
     * Test that singleton beans in an EAR are properly destroyed during quiesce.
     * Verifies that all PostConstruct and PreDestroy methods are called in the correct order.
     */
    @Test
    public void testSingletonQuiesceInEar() throws Exception {
        try {
            earServer.startServer();

            // Verify that invalid boolean values in binding files produce the expected warnings,
            // each with a distinct invalid value to confirm the correct configuration was read:
            // - CNTR0120W for StartupSingletonQuiesceInvalidBnd (ibm-ejb-jar-bnd.xml, value=notABooleanBnd)
            // - CWNEN0014W for StartupSingletonQuiesceInvalid (ejb-jar.xml, value=notABooleanDD)
            assertNotNull("CNTR0120W not logged for invalid boolean in ibm-ejb-jar-bnd.xml (notABooleanBnd)",
                          earServer.waitForStringInLog("CNTR0120W.*notABooleanBnd"));
            assertNotNull("CWNEN0014W not logged for invalid boolean in ejb-jar.xml (notABooleanDD)",
                          earServer.waitForStringInLog("CWNEN0014W.*notABooleanDD"));

            // Expected order of PostConstruct messages for startup beans
            List<String> expectedStartupPostConstructs = Arrays.asList //
            ("PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDefault:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDD:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBnd:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBndOverride:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServer:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServerOverride:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalidBnd:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalid:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDefault:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDD:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceBnd:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServer:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServerOverride:");

            // Find all PostConstruct messages for SingletonQuiesceApp
            List<String> actualPostConstructs = earServer.findStringsInLogsUsingMark(".*PostConstruct:SingletonQuiesceApp.*", earServer.getDefaultLogFile());

            // Verify all expected startup PostConstructs are present in order
            verifyMessagesInOrder(expectedStartupPostConstructs, actualPostConstructs, "Startup PostConstruct");

            // Call servlet to initialize non-startup beans
            earServer.setMarkToEndOfLog();
            runTest(earServer, "SingletonQuiesceWeb/SingletonQuiesceServlet", "testBeans");

            // Expected order of PostConstruct messages for non-startup beans
            List<String> expectedNonStartupPostConstructs = Arrays.asList //
            ("PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceDefault:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceDD:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceBnd:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceBndOverride:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceServer:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceServerOverride:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceDefault:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceDD:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceBnd:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceServer:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceServerOverride:");

            // Find all PostConstruct messages for non-startup beans
            actualPostConstructs = earServer.findStringsInLogsUsingMark(".*PostConstruct:SingletonQuiesceApp.*", earServer.getDefaultLogFile());

            // Verify all expected non-startup PostConstructs are present in order
            verifyMessagesInOrder(expectedNonStartupPostConstructs, actualPostConstructs, "Non-startup PostConstruct");

            earServer.setMarkToEndOfLog();
            earServer.stopServer(false, EAR_EXPECTED_WARNINGS);

            // Make sure stop has completed
            assertNotNull("Server " + earServer.getServerName() + " FAILED to stop", earServer.waitForStringInLog("CWWKE0036I"));

            // Expected order of PreDestroy messages
            List<String> expectedPreDestroys = Arrays.asList //
            ("CWWKE1100I", // waiting for server quiesce
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceBndOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDD:",
             "CWWKO0220I", // first standard quiesce listener
             "CWWKE1101I", // server quiesce complete
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceDefault:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceDefault:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDefault:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalid:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalidBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBndOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDefault:",
             "CWWKZ0009I");

            // Find all PreDestroy messages for SingletonQuiesceApp and:
            // - CWWKE1100I: Waiting for up to 30 seconds for the server to quiesce.
            // - CWWKO0220I: TCP Channel defaultHttpEndpoint has stopped listening (a normal quiesce listener)
            // - CWWKE1101I: Server quiesce complete.
            // - CWWKZ0009I: The application SingletonQuiesceApp has stopped successfully.
            List<String> actualPreDestroys = earServer.findStringsInLogsUsingMark(".*(PreDestroy:SingletonQuiesceApp|CWWKE1100I|CWWKO0220I|CWWKE1101I|CWWKZ0009I).*",
                                                                                  earServer.getDefaultLogFile());

            // Verify all expected PreDestroys are present in order
            verifyMessagesInOrder(expectedPreDestroys, actualPreDestroys, "PreDestroy");

        } finally {
            if (earServer.isStarted()) {
                earServer.stopServer(EAR_EXPECTED_WARNINGS);
            } else {
                earServer.postStopServerArchive();
            }
        }
    }

    /**
     * Test that startup singleton beans in an EAR are properly destroyed during quiesce
     * and unused non-startup singletons are neither created nor destroyed.
     * Verifies that all PostConstruct and PreDestroy methods are called for @Startup beans in the correct order.
     */
    @Test
    public void testSingletonQuiesceStartupInEar() throws Exception {
        try {
            earServer.startServer();

            // Verify that invalid boolean values in binding files produce the expected warnings,
            // each with a distinct invalid value to confirm the correct configuration was read:
            // - CNTR0120W for StartupSingletonQuiesceInvalidBnd (ibm-ejb-jar-bnd.xml, value=notABooleanBnd)
            // - CWNEN0014W for StartupSingletonQuiesceInvalid (ejb-jar.xml, value=notABooleanDD)
            assertNotNull("CNTR0120W not logged for invalid boolean in ibm-ejb-jar-bnd.xml (notABooleanBnd)",
                          earServer.waitForStringInLog("CNTR0120W.*notABooleanBnd"));
            assertNotNull("CWNEN0014W not logged for invalid boolean in ejb-jar.xml (notABooleanDD)",
                          earServer.waitForStringInLog("CWNEN0014W.*notABooleanDD"));

            // Expected order of PostConstruct messages for startup beans
            List<String> expectedPostConstructs = Arrays.asList //
            ("PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDefault:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDD:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBnd:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBndOverride:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServer:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServerOverride:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalidBnd:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalid:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDefault:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDD:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceBnd:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServer:",
             "PostConstruct:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServerOverride:");

            // Find all PostConstruct messages for SingletonQuiesceApp
            List<String> actualPostConstructs = earServer.findStringsInLogsUsingMark(".*PostConstruct:SingletonQuiesceApp.*", earServer.getDefaultLogFile());

            // Verify all expected PostConstructs are present in order
            verifyMessagesInOrder(expectedPostConstructs, actualPostConstructs, "PostConstruct");

            earServer.setMarkToEndOfLog();
            earServer.stopServer(false, EAR_EXPECTED_WARNINGS);

            // Make sure stop has completed
            assertNotNull("Server " + earServer.getServerName() + " FAILED to stop", earServer.waitForStringInLog("CWWKE0036I"));

            // Expected order of PreDestroy messages for startup beans with destroyOnQuiesce configured
            List<String> expectedPreDestroys = Arrays.asList //
            ("CWWKE1100I", // waiting for server quiesce
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDD:",
             "CWWKO0220I", // first standard quiesce listener
             "CWWKE1101I", // server quiesce complete
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDefault:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalid:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalidBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBndOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDefault:",
             "CWWKZ0009I");

            // Find all PreDestroy messages for SingletonQuiesceApp and:
            // - CWWKE1100I: Waiting for up to 30 seconds for the server to quiesce.
            // - CWWKO0220I: TCP Channel defaultHttpEndpoint has stopped listening (a normal quiesce listener)
            // - CWWKE1101I: Server quiesce complete.
            // - CWWKZ0009I: The application SingletonQuiesceApp has stopped successfully.
            List<String> actualPreDestroys = earServer.findStringsInLogsUsingMark(".*(PreDestroy:SingletonQuiesceApp|CWWKE1100I|CWWKO0220I|CWWKE1101I|CWWKZ0009I).*",
                                                                                  earServer.getDefaultLogFile());

            // Verify all expected PreDestroys are present in order
            verifyMessagesInOrder(expectedPreDestroys, actualPreDestroys, "PreDestroy");
        } finally {
            if (earServer.isStarted()) {
                earServer.stopServer(EAR_EXPECTED_WARNINGS);
            } else {
                earServer.postStopServerArchive();
            }
        }
    }

    /**
     * Test that singleton beans in an EAR are properly destroyed at application stop when the
     * server is stopped with --force (skipping quiesce). Verifies that all beans are still
     * destroyed regardless of their destroyOnQuiesce configuration, and that the quiesce
     * phase messages (CWWKE1100I, CWWKE1101I) are absent from the log.
     */
    @Test
    public void testSingletonQuiesceForceStopInEar() throws Exception {
        try {
            earServer.startServer();

            // Verify that invalid boolean values in binding files produce the expected warnings,
            // each with a distinct invalid value to confirm the correct configuration was read:
            // - CNTR0120W for StartupSingletonQuiesceInvalidBnd (ibm-ejb-jar-bnd.xml, value=notABooleanBnd)
            // - CWNEN0014W for StartupSingletonQuiesceInvalid (ejb-jar.xml, value=notABooleanDD)
            assertNotNull("CNTR0120W not logged for invalid boolean in ibm-ejb-jar-bnd.xml (notABooleanBnd)",
                          earServer.waitForStringInLog("CNTR0120W.*notABooleanBnd"));
            assertNotNull("CWNEN0014W not logged for invalid boolean in ejb-jar.xml (notABooleanDD)",
                          earServer.waitForStringInLog("CWNEN0014W.*notABooleanDD"));

            // Call servlet to initialize all non-startup beans
            runTest(earServer, "SingletonQuiesceWeb/SingletonQuiesceServlet", "testBeans");

            earServer.setMarkToEndOfLog();
            // Stop with --force: quiesce phase is skipped entirely
            earServer.stopServer(false, true, EAR_EXPECTED_WARNINGS);

            // Make sure stop has completed
            assertNotNull("Server " + earServer.getServerName() + " FAILED to stop", earServer.waitForStringInLog("CWWKE0036I"));

            // Quiesce phase must NOT have started - these messages must be absent
            assertNull("CWWKE1100I (quiesce start) should not appear after --force stop",
                       earServer.verifyStringNotInLogUsingMark("CWWKE1100I", 0));
            assertNull("CWWKE1101I (quiesce complete) should not appear after --force stop",
                       earServer.verifyStringNotInLogUsingMark("CWWKE1101I", 0));

            // All beans must still have their @PreDestroy called at application stop,
            // regardless of destroyOnQuiesce configuration.
            // Startup beans with destroyOnQuiesce=true are NOT called during quiesce (quiesce was
            // skipped) so they all fire here together with the default beans.
            List<String> allPreDestroys = Arrays.asList //
            ("PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceBndOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:SingletonQuiesceDefault:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBndOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalidBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceInvalid:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceEjb:StartupSingletonQuiesceDefault:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:SingletonQuiesceDefault:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesceApp:SingletonQuiesceWeb:StartupSingletonQuiesceDefault:");

            List<String> actualPreDestroys = earServer.findStringsInLogsUsingMark(".*PreDestroy:SingletonQuiesceApp.*",
                                                                                  earServer.getDefaultLogFile());

            // Verify all PreDestroy messages are present (order within app stop is not guaranteed)
            for (String expected : allPreDestroys) {
                assertTrue("Expected PreDestroy message not found: " + expected,
                           actualPreDestroys.stream().anyMatch(msg -> msg.contains(expected)));
            }
            assertEquals("Expected " + allPreDestroys.size() + " PreDestroy messages but found " + actualPreDestroys.size(),
                         allPreDestroys.size(), actualPreDestroys.size());

        } finally {
            if (earServer.isStarted()) {
                earServer.stopServer(EAR_EXPECTED_WARNINGS);
            } else {
                earServer.postStopServerArchive();
            }
        }
    }

    /**
     * Test that singleton beans in a WAR are properly destroyed at application stop when the
     * server is stopped with --force (skipping quiesce). Verifies that all beans are still
     * destroyed regardless of their destroyOnQuiesce configuration, and that the quiesce
     * phase messages (CWWKE1100I, CWWKE1101I) are absent from the log.
     */
    @Test
    public void testSingletonQuiesceForceStopInWar() throws Exception {
        try {
            warServer.startServer();

            // Call servlet to initialize all non-startup beans
            runTest(warServer, "SingletonQuiesce/SingletonQuiesceServlet", "testBeans");

            warServer.setMarkToEndOfLog();
            // Stop with --force: quiesce phase is skipped entirely
            warServer.stopServer(false, true);

            // Make sure stop has completed
            assertNotNull("Server " + warServer.getServerName() + " FAILED to stop", warServer.waitForStringInLog("CWWKE0036I"));

            // Quiesce phase must NOT have started - these messages must be absent
            assertNull("CWWKE1100I (quiesce start) should not appear after --force stop",
                       warServer.verifyStringNotInLogUsingMark("CWWKE1100I", 0));
            assertNull("CWWKE1101I (quiesce complete) should not appear after --force stop",
                       warServer.verifyStringNotInLogUsingMark("CWWKE1101I", 0));

            // All beans must still have their @PreDestroy called at application stop,
            // regardless of destroyOnQuiesce configuration.
            List<String> allPreDestroys = Arrays.asList //
            ("PreDestroy:SingletonQuiesce:SingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesce:SingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesce:SingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesce:SingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesce:SingletonQuiesceDefault:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceDefault:");

            List<String> actualPreDestroys = warServer.findStringsInLogsUsingMark(".*PreDestroy:SingletonQuiesce:.*",
                                                                                  warServer.getDefaultLogFile());

            // Verify all PreDestroy messages are present (order within app stop is not guaranteed)
            for (String expected : allPreDestroys) {
                assertTrue("Expected PreDestroy message not found: " + expected,
                           actualPreDestroys.stream().anyMatch(msg -> msg.contains(expected)));
            }
            assertEquals("Expected " + allPreDestroys.size() + " PreDestroy messages but found " + actualPreDestroys.size(),
                         allPreDestroys.size(), actualPreDestroys.size());

        } finally {
            if (warServer.isStarted()) {
                warServer.stopServer();
            } else {
                warServer.postStopServerArchive();
            }
        }
    }

    /**
     * Test that singleton beans in a WAR are properly destroyed during quiesce.
     * Verifies that all PostConstruct and PreDestroy methods are called in the correct order.
     */
    @Test
    public void testSingletonQuiesceInWar() throws Exception {
        try {
            warServer.startServer();

            // Call servlet to initialize all beans
            runTest(warServer, "SingletonQuiesce/SingletonQuiesceServlet", "testBeans");

            // Expected order of PostConstruct messages
            List<String> expectedPostConstructs = Arrays.asList //
            ("PostConstruct:SingletonQuiesce:StartupSingletonQuiesceDefault:",
             "PostConstruct:SingletonQuiesce:StartupSingletonQuiesceDD:",
             "PostConstruct:SingletonQuiesce:StartupSingletonQuiesceBnd:",
             "PostConstruct:SingletonQuiesce:StartupSingletonQuiesceServer:",
             "PostConstruct:SingletonQuiesce:StartupSingletonQuiesceServerOverride:",
             "PostConstruct:SingletonQuiesce:SingletonQuiesceDefault:",
             "PostConstruct:SingletonQuiesce:SingletonQuiesceDD:",
             "PostConstruct:SingletonQuiesce:SingletonQuiesceBnd:",
             "PostConstruct:SingletonQuiesce:SingletonQuiesceServer:",
             "PostConstruct:SingletonQuiesce:SingletonQuiesceServerOverride:");

            // Find all PostConstruct messages for SingletonQuiesce
            List<String> actualPostConstructs = warServer.findStringsInLogsUsingMark(".*PostConstruct:SingletonQuiesce.*", warServer.getDefaultLogFile());

            // Verify all expected PostConstructs are present in order
            verifyMessagesInOrder(expectedPostConstructs, actualPostConstructs, "PostConstruct");

            warServer.setMarkToEndOfLog();
            warServer.stopServer(false);

            // Make sure stop has completed
            assertNotNull("Server " + warServer.getServerName() + " FAILED to stop", warServer.waitForStringInLog("CWWKE0036I"));

            // Expected order of PreDestroy messages for beans with destroyOnQuiesce configured
            List<String> expectedPreDestroys = Arrays.asList //
            ("CWWKE1100I", // waiting for server quiesce
             "PreDestroy:SingletonQuiesce:SingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesce:SingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesce:SingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesce:SingletonQuiesceDD:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceDD:",
             "CWWKO0220I", // first standard quiesce listener
             "CWWKE1101I", // server quiesce complete
             "PreDestroy:SingletonQuiesce:SingletonQuiesceDefault:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceDefault:",
             "CWWKZ0009I");

            // Find all PreDestroy messages for SingletonQuiesce and:
            // - CWWKE1100I: Waiting for up to 30 seconds for the server to quiesce.
            // - CWWKO0220I: TCP Channel defaultHttpEndpoint has stopped listening (a normal quiesce listener)
            // - CWWKE1101I: Server quiesce complete.
            // - CWWKZ0009I: The application SingletonQuiesceApp has stopped successfully.
            List<String> actualPreDestroys = warServer.findStringsInLogsUsingMark(".*(PreDestroy:SingletonQuiesce|CWWKE1100I|CWWKO0220I|CWWKE1101I|CWWKZ0009I).*",
                                                                                  warServer.getDefaultLogFile());

            // Verify all expected PreDestroys are present in order
            verifyMessagesInOrder(expectedPreDestroys, actualPreDestroys, "PreDestroy");
        } finally {
            if (warServer.isStarted()) {
                warServer.stopServer();
            } else {
                warServer.postStopServerArchive();
            }
        }
    }

    /**
     * Test that startup singleton beans in a WAR are properly destroyed during quiesce.
     * Verifies that all PostConstruct and PreDestroy methods are called for @Startup beans in the correct order.
     */
    @Test
    public void testSingletonQuiesceStartupInWar() throws Exception {
        try {
            warServer.startServer();

            // Expected order of PostConstruct messages for startup beans
            List<String> expectedPostConstructs = Arrays.asList //
            ("PostConstruct:SingletonQuiesce:StartupSingletonQuiesceDefault:",
             "PostConstruct:SingletonQuiesce:StartupSingletonQuiesceDD:",
             "PostConstruct:SingletonQuiesce:StartupSingletonQuiesceBnd:",
             "PostConstruct:SingletonQuiesce:StartupSingletonQuiesceServer:",
             "PostConstruct:SingletonQuiesce:StartupSingletonQuiesceServerOverride:");

            // Find all PostConstruct messages for SingletonQuiesce
            List<String> actualPostConstructs = warServer.findStringsInLogsUsingMark(".*PostConstruct:SingletonQuiesce.*", warServer.getDefaultLogFile());

            // Verify all expected PostConstructs are present in order
            verifyMessagesInOrder(expectedPostConstructs, actualPostConstructs, "PostConstruct");

            warServer.setMarkToEndOfLog();
            warServer.stopServer(false);

            // Make sure stop has completed
            assertNotNull("Server " + warServer.getServerName() + " FAILED to stop", warServer.waitForStringInLog("CWWKE0036I"));

            // Expected order of PreDestroy messages for beans with destroyOnQuiesce configured
            List<String> expectedPreDestroys = Arrays.asList //
            ("CWWKE1100I", // waiting for server quiesce
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceServerOverride:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceServer:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceBnd:",
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceDD:",
             "CWWKO0220I", // first standard quiesce listener
             "CWWKE1101I", // server quiesce complete
             "PreDestroy:SingletonQuiesce:StartupSingletonQuiesceDefault:",
             "CWWKZ0009I");

            // Find all PreDestroy messages for SingletonQuiesce and:
            // - CWWKE1100I: Waiting for up to 30 seconds for the server to quiesce.
            // - CWWKO0220I: TCP Channel defaultHttpEndpoint has stopped listening (a normal quiesce listener)
            // - CWWKE1101I: Server quiesce complete.
            // - CWWKZ0009I: The application SingletonQuiesceApp has stopped successfully.
            List<String> actualPreDestroys = warServer.findStringsInLogsUsingMark(".*(PreDestroy:SingletonQuiesce|CWWKE1100I|CWWKO0220I|CWWKE1101I|CWWKZ0009I).*",
                                                                                  warServer.getDefaultLogFile());

            // Verify all expected PreDestroys are present in order
            verifyMessagesInOrder(expectedPreDestroys, actualPreDestroys, "PreDestroy");
        } finally {
            if (warServer.isStarted()) {
                warServer.stopServer();
            } else {
                warServer.postStopServerArchive();
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
    private void verifyMessagesInOrder(List<String> expectedMessages, List<String> actualMessages, String messageType) {
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
        for (int i = 0; i < expectedMessages.size(); i++) {
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
        for (int i = 0; i < expectedMessages.size(); i++) {
            assertEquals(messageType + " message at position " + i + " does not match expected order",
                         expectedMessages.get(i), extractedMessages.get(i));
        }
    }

}
