/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.mdb.ra.checkpoint.fat.tests;

import static io.openliberty.ejbcontainer.mdb.checkpoint.fat.FATSuite.getTestMethod;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class ConfigChangesTest extends FATServletClient {

    final static String SERVER_NAME = "checkpointMsgEndpointServer";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers(SERVER_NAME)).andWith(new JakartaEE9Action().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers(SERVER_NAME)).andWith(new JakartaEE10Action().forServers(SERVER_NAME));

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    static public void setupClass() throws Exception {
        server.saveServerConfiguration();
    }

    static final String AUTHDATA_USER = "ACTV1USER";
    static final String AUTHDATA_PASSWORD = "{xor}HhwLCW4PCBs="; // "ACTV1PWD"

    static final String[] IGNORE_REGEX = new String[] { "J2CA8501E:.*Property propertyJ", // Bean property cannot be set
                                                        "CNTR0067W:.*MsgEndpointApp#MsgEndpointEJB.jar#MDBTimedBMTBean", // Mixed transaction types
                                                        "CNTR0067W:.*MsgEndpointApp#MsgEndpointEJB.jar#MDBTimedBMTFailBean",
                                                        "CNTR4015W:.*EndpointBMTNonJMSNoActSpec" }; // Missing activationSpec

    TestMethod testMethod;
    List<String> testMsgs;

    @Before
    public void setupTest() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        testMsgs = null;

        server.removeAllInstalledAppsForValidation(); // Added by assembleAndDeployEarsWarsRars()
        ShrinkHelper.cleanAllExportedArchives();
        // No need to remove server.env file

        server.setArchiveMarker(testMethod + ".marker");
        server.restoreServerConfiguration();
        assembleAndDeployEarsWarsRars();

        switch (testMethod) {
            case testMEPsActivateOnlyDuringRestoreBAS:
                server.setCheckpoint(CheckpointPhase.BEFORE_APP_START, false, null);
                server.addCheckpointRegexIgnoreMessages(IGNORE_REGEX);
                server.startServer();
                break;
            case testMEPsActivateOnlyDuringRestoreAAS:
                server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
                server.addCheckpointRegexIgnoreMessages(IGNORE_REGEX);
                server.startServer();
                break;
            case testAuthDataUpdatesDuringRestoreAAS:
            case testJMSAuthDataUpdatesDuringRestoreAAS:
                // Override the endpoint's activationSpec authData at restore
                server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, checkpointServer -> {
                    File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        serverEnvWriter.println("AUTHDATA_USER=" + AUTHDATA_USER);
                        serverEnvWriter.println("AUTHDATA_PASSWORD=" + AUTHDATA_PASSWORD);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                server.addCheckpointRegexIgnoreMessages(IGNORE_REGEX);
                server.startServer();
                break;
            default:
                throw new Exception("Missing configuration for " + testName);
        }
    }

    void assembleAndDeployEarsWarsRars() throws Exception {
        //#################### MsgEndpointApp.ear
        JavaArchive MsgEndpointEJB = ShrinkHelper.buildJavaArchive("MsgEndpointEJB.jar", "io.openliberty.ejbcontainer.fat.msgendpoint.ejb.");
        MsgEndpointEJB = (JavaArchive) ShrinkHelper.addDirectory(MsgEndpointEJB, "test-applications/MsgEndpointEJB.jar/resources");
        WebArchive MsgEndpointWeb = ShrinkHelper.buildDefaultApp("MsgEndpointWeb.war", "io.openliberty.ejbcontainer.fat.msgendpoint.web.");

        EnterpriseArchive MsgEndpointApp = ShrinkWrap.create(EnterpriseArchive.class, "MsgEndpointApp.ear");
        MsgEndpointApp.addAsModule(MsgEndpointEJB).addAsModule(MsgEndpointWeb);
        MsgEndpointApp = (EnterpriseArchive) ShrinkHelper.addDirectory(MsgEndpointApp, "test-applications/MsgEndpointApp.ear/resources");

        ShrinkHelper.exportAppToServer(server, MsgEndpointApp, DeployOptions.SERVER_ONLY);

        server.addInstalledAppForValidation("MsgEndpointApp");

        //#################### AdapterForEJB.jar (RAR implementation)
        JavaArchive AdapterForEJBJar = ShrinkHelper.buildJavaArchive("AdapterForEJB.jar", "com.ibm.ws.ejbcontainer.fat.rar.*");
        ShrinkHelper.exportToServer(server, "ralib", AdapterForEJBJar, DeployOptions.SERVER_ONLY);

        //#################### AdapterForEJB.rar
        ResourceAdapterArchive AdapterForEJBRar = ShrinkWrap.create(ResourceAdapterArchive.class, "AdapterForEJB.rar");
        ShrinkHelper.addDirectory(AdapterForEJBRar, "test-resourceadapters/AdapterForEJB.rar/resources");
        ShrinkHelper.exportToServer(server, "connectors", AdapterForEJBRar, DeployOptions.SERVER_ONLY);
    }

    @After
    public void teardownTest() throws Exception {
        if (server.isStarted()) {
            server.stopServer(IGNORE_REGEX);
        }
    }

    void runTest(String servlet) throws Exception {
        FATServletClient.runTest(server, servlet, getTestMethodSimpleName());
    }

    /**
     * Verify message endpoints do not activate during checkpoint at beforeAppStart.
     *
     * This test verifies some JCA internals behavior, which may change as completing
     * RAR installation at restore is not ideal.
     */
    @Test
    public void testMEPsActivateOnlyDuringRestoreBAS() throws Exception {

        // RAR installation started during checkpoint BAS
        testMsgs = server.findStringsInLogsUsingMark("J2CA7018I: .*AdapterForEJB", server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // RAR installation did not complete
        testMsgs = server.findStringsInLogsUsingMark("J2CA7001I: .*AdapterForEJB", server.getDefaultLogFile());
        assertTrue(testMsgs.isEmpty());

        // Application is not starting ==> Checkpoint launched when the connector (JCA)
        // runtime loaded RAR binaries, and endpoints are not initialized nor activated
        testMsgs = server.findStringsInLogsUsingMark("CWWKZ0018I: .*MsgEndpointApp", server.getDefaultLogFile());
        assertTrue(testMsgs.isEmpty());

        server.checkpointRestore();

        // Server resumed from checkpoint
        testMsgs = server.findStringsInLogsUsingMark("CWWKC0452I", server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // RAR installation completed during restore
        testMsgs = server.findStringsInLogsUsingMark("J2CA7001I: .*AdapterForEJB", server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // Application is starting
        testMsgs = server.findStringsInLogsUsingMark("CWWKZ0018I: .*MsgEndpointApp", server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // Default HTTP endpoint is listening and ready to accept requests
        testMsgs = server.findStringsInLogsUsingMark("CWWKO0219I: .*defaultHttpEndpoint", server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // Message endpoints are activated and ready to receive messages
        testMsgs = server.findStringsInLogsUsingMark("J2CA8801I", server.getDefaultLogFile());
        assertTrue(testMsgs.size() > 5);
    }

    /**
     * Verify endpoints do not activate during checkpoint at afterAppStart.
     */
    @Test
    public void testMEPsActivateOnlyDuringRestoreAAS() throws Exception {

        // RAR installation completed during checkpoint AAS
        testMsgs = server.findStringsInLogsUsingMark("J2CA7001I: .*AdapterForEJB", server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // Application started
        testMsgs = server.findStringsInLogsUsingMark("CWWKZ0001I: .*MsgEndpointApp", server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // Message endpoints did not activate during checkpoint
        testMsgs = server.findStringsInLogsUsingMark("J2CA8801I", server.getDefaultLogFile());
        assertTrue(testMsgs.isEmpty());

        server.checkpointRestore();

        // Server resumed (started) from checkpoint
        testMsgs = server.findStringsInLogsUsingMark("CWWKC0452I", server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // Default HTTP endpoint is listening and ready to accept requests
        testMsgs = server.findStringsInLogsUsingMark("CWWKO0219I: .*defaultHttpEndpoint", server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // Message endpoints are activated and ready to receive messages
        testMsgs = server.findStringsInLogsUsingMark("J2CA8801I", server.getDefaultLogFile());
        assertTrue(testMsgs.size() > 5);
    }

    /**
     * Verify message endpoint activates during restore using updated authData.
     */
    @Test
    public void testAuthDataUpdatesDuringRestoreAAS() throws Exception {

        // Updating authData referenced by an activationSpec causes endpoints bound to the activationSpec
        // to deactivate; then the activationSpec service (EndpointActivationService) will rebind using new
        // authData, and the endpoints will reactivate. Since endpoint activation defers to checkpoint
        // restore, endpoints should activate for the first time during restore using the updated authData.

        server.checkpointRestore();

        // Verify activationSpec authData updates to the values in server.env. This message emits when
        // the JCA runtime invokes RA.endpointActivate(), after the EndpointActivationService has rebinded.
        testMsgs = server.findStringsInLogsUsingMark("JCA activation authData for endpoint named EndpointRestoreAuthDataNonJMS is user=ACTV1USER, password=ACTV1PW",
                                                     server.getDefaultLogFile());
        assertFalse("testMsgs: " + testMsgs, testMsgs.isEmpty());

        // Verify activation occurs exactly once
        testMsgs = server.findStringsInLogsUsingMark("J2CA8801I: .*ejb/EndpointRestoreAuthDataNonJMS .*MsgEndpointApp#MsgEndpointEJB.jar#EndpointRestoreAuthDataNonJMS",
                                                     server.getDefaultLogFile());
        assertTrue(testMsgs.size() == 1);

        // Verify transactional delivery: CMT + required
        runTest("MsgEndpointWeb/NonJMS_MDServlet");
        testMsgs = server.findStringsInLogsUsingMark("EndpointRestoreAuthDataNonJMS is in a global transaction",
                                                     server.getDefaultLogFile());
        assertFalse("testMsgs: " + testMsgs, testMsgs.isEmpty());
    }

    /**
     * Verify message endpoint activates during restore using updated JMS authData.
     */
    @Test
    public void testJMSAuthDataUpdatesDuringRestoreAAS() throws Exception {

        server.checkpointRestore();

        // AuthData referenced by jmsActivationSpec updated before RA.endpointActivation()
        testMsgs = server.findStringsInLogsUsingMark("JMS activation authData for endpoint named EndpointRestoreAuthDataJMS is user=ACTV1USER, password=ACTV1PW",
                                                     server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());

        // Endpoint activated
        testMsgs = server.findStringsInLogsUsingMark("J2CA8801I: .*ejb/EndpointRestoreAuthDataJMS .*MsgEndpointApp#MsgEndpointEJB.jar#EndpointRestoreAuthDataJMS",
                                                     server.getDefaultLogFile());
        assertTrue(testMsgs.size() == 1);

        // Deliver non-transactional message: BMT
        runTest("MsgEndpointWeb/JMS_MDServlet");
        testMsgs = server.findStringsInLogsUsingMark("EndpointRestoreAuthDataJMS is in a local transaction",
                                                     server.getDefaultLogFile());
        assertFalse(testMsgs.isEmpty());
    }

    static enum TestMethod {
        testMEPsActivateOnlyDuringRestoreBAS,
        testMEPsActivateOnlyDuringRestoreAAS,
        testAuthDataUpdatesDuringRestoreAAS,
        testJMSAuthDataUpdatesDuringRestoreAAS,
        unknown;
    }

}