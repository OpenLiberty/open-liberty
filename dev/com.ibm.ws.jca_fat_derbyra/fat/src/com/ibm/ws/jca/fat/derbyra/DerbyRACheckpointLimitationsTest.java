/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.jca.fat.derbyra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Verify the Connectors feature fails checkpoint whenever a timer is created or
 * work is submitted during application startup within a server checkpoint request.
 * Also, verify the Connectors feature supports these behaviors when the checkpoint
 * occurs before the applications are started.
 */
@CheckpointTest
@RunWith(FATRunner.class)
public class DerbyRACheckpointLimitationsTest extends FATServletClient {

    private static final String SERVER_NAME = "com.ibm.ws.jca.fat.derbyra.checkpoint";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    static final String APP = "derbyRAApp";
    static final String WAR_NAME = "fvtweb";
    static final String RAR_NAME = "DerbyRA";

    static final String DerbyRAAnnoServlet = "fvtweb/DerbyRAAnnoServlet";
    static final String DerbyRACheckpointServlet = "fvtweb/DerbyRACheckpointServlet";
    static final String DerbyRACFDServlet = "fvtweb/DerbyRACFDServlet";
    static final String DerbyRAServlet = "fvtweb/DerbyRAServlet";

    @BeforeClass
    public static void setUpClass() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_NAME + ".war");
        war.addPackage("web");
        war.addPackage("web.cfd");
        war.addPackage("web.mdb");
        war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-web-bnd.xml"));
        war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/web.xml"));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP + ".ear");
        ear.addAsModule(war);
        ShrinkHelper.addDirectory(ear, "lib/LibertyFATTestFiles/derbyRAApp");
        ShrinkHelper.exportAppToServer(server, ear, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);

        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, RAR_NAME + ".rar");
        rar.as(JavaArchive.class).addPackage("fat.derbyra.resourceadapter");
        rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/ra.xml"));
        rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/wlp-ra.xml"));
        rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/permissions.xml"));
        rar.addAsLibrary(new File("publish/shared/resources/derby/derby.jar"));

        ShrinkHelper.exportToServer(server, "connectors", rar);

        server.addEnvVar("PERMISSION",
                         (JakartaEEAction.isEE9OrLaterActive()) ? "jakarta.resource.spi.security.PasswordCredential" : "javax.resource.spi.security.PasswordCredential");

    }

    TestMethod testMethod;

    @Before
    public void setupTest() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        //switch (testMethod) {
        //    case testCreateTimerBAS:
        //    case testSubmitWorkBAS:
        //        break;
        //    case testCreateTimerAAS:
        //    case testSubmitWorkAAS:
        //        break;
        //    default:
        //        break;
        //}
    }

    @After
    public void tearDownTest() throws Exception {
        switch (testMethod) {
            case testCreateTimerBAS:
            case testSubmitWorkBAS:
                server.stopServer(new String[] { "CWWKG0027W", // Possible timeout while updating server configuration
                                                 "SRVE9967W" }); // Manifest class path derbyLocale_cs.jar not found in Derby archives
                break;
            case testCreateTimerAAS:
            case testSubmitWorkAAS:
                // Server should fail checkpoint and stop
                if (server.isStarted()) {
                    server.stopServer("SRVE9967W");
                } else {
                    // save the logs from the failed checkpoint
                    server.postStopServerArchive();
                }
                break;
            default:
                break;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (server.isStarted())
            server.stopServer("SRVE9967W");
    }

    // FYI: These tests include assertions regarding RA lifecycle events during checkpoint and restore.
    //      The assertions may be removed if considered superfluous.

    @Test
    public void testCreateTimerBAS() throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.put("-Dunsupported.action", "derbyra.start.create.timer");
        //jvmOptions.put("-Dunsupported.action", "servlet.init.create.timer"); // FAILS NORMAL AND INSTANTON SERVERS
        //jvmOptions.put("-Dunsupported.action", "servlet.init.create.timer.async"); // WORKS
        server.setJvmOptions(jvmOptions);

        // Server checkpoint BAS performs CRIU dump when triggered by RA metadata processing during
        // RA install. Resources and applications must not create timers nor submit work.
        server.setCheckpoint(CheckpointPhase.BEFORE_APP_START, false, null);
        server.addCheckpointRegexIgnoreMessages("SRVE9967W");
        server.startServer(getTestMethodNameOnly(testName) + ".log");

        assertNotNull("The connector runtime did not log message J2CA7018I indicating the server is installing the RA.",
                      server.waitForStringInLogUsingMark("J2CA7018I: .* DerbyRA"));

        assertNull("The connector runtime unexpectedly logged message J2CA8512E indicating createTimer() failed with exception.",
                   server.waitForStringInLogUsingMark("J2CA8512E: .*J2CA8512E: .*DerbyRA", 1000L));

        assertNull("The connector runtime unexpectedly logged message J2CA8511E indicating the checkpoint request failed because the RA created a timer.",
                   server.waitForStringInLogUsingMark("J2CA8511E: .* DerbyRA", 1000L));

        server.checkpointRestore();

        assertNotNull("The connector runtime did not log message J2CA7001I indicating the server installed the RA",
                      server.waitForStringInLogUsingMark("J2CA7001I: .* DerbyRA"));

        assertNotNull("The test work scheduled during did not execute during servlet init()",
                      server.waitForStringInLogUsingMark("--- DerbyRA start createTimer: "));

        // Verify application has started before running tests
        server.addInstalledAppForValidation(APP);

        // Verify some connector function
        runTest(server, DerbyRAServlet, "initDatabaseTables");
        runTest(server, DerbyRAAnnoServlet, "testActivationSpec");
    }

    @Test
    public void testSubmitWorkBAS() throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.put("-Dunsupported.action", "servlet.init.submit.work.async");
        //jvmOptions.put("-Dunsupported.action", "servlet.init.submit.work"); // FAILS NORMAL AND INSTANTON SERVERS
        server.setJvmOptions(jvmOptions);

        server.setCheckpoint(CheckpointPhase.BEFORE_APP_START, false, null);
        server.addCheckpointRegexIgnoreMessages("SRVE9967W");
        server.startServer(getTestMethodNameOnly(testName) + ".log");

        assertNotNull("The connector runtime did not log message J2CA7018I indicating the server is installing the RA.",
                      server.waitForStringInLogUsingMark("J2CA7018I: .* DerbyRA"));

        assertNull("The work manager unexpectedly logged message J2CA8602E indicating the submitted work was rejected with exception.",
                   server.waitForStringInLogUsingMark("J2CA8602E: .* DerbyRA", 1000L));

        assertNull("The connector runtime unexpectedly logged message J2CA8601E indicating the checkpoint request failed because the RA submitted work.",
                   server.waitForStringInLogUsingMark("J2CA8601E:  .*DerbyRA .*TestWork", 1000L));

        server.checkpointRestore();

        assertNotNull("The connector runtime did not log message J2CA7001I indicating the server installed the RA",
                      server.waitForStringInLogUsingMark("J2CA7001I: .* DerbyRA"));

        assertNotNull("The test work scheduled during did not execute during servlet init()",
                      server.waitForStringInLogUsingMark("--- DerbyRACheckpointServlet TestWork run"));

        // Verify application has started before running tests
        server.addInstalledAppForValidation(APP);

        runTest(server, DerbyRAServlet, "initDatabaseTables");
        runTest(server, DerbyRAAnnoServlet, "testActivationSpec");
    }

    @Test
    @ExpectedFFDC({ "io.openliberty.checkpoint.internal.criu.CheckpointFailedException",
                    "java.security.PrivilegedActionException" }) // Wraps J2CA8512E javax.resource.spi.UnavailableException
    @AllowedFFDC({ "java.lang.RuntimeException", // JMX local connector unavailable during shutdown of failed checkpoint
                   "javax.transaction.SystemException", "jakarta.transaction.SystemException", "com.ibm.ws.uow.embeddable.SystemException", //
                   "java.lang.IllegalStateException" })
    public void testCreateTimerAAS() throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.put("-Dunsupported.action", "servlet.init.create.timer");
        server.setJvmOptions(jvmOptions);

        // Expect server checkpoint to fail; note required CheckpointInfo()
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, true, true, null));
        server.addCheckpointRegexIgnoreMessages("SRVE9967W");
        ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int returnCode = output.getReturnCode();
        assertEquals("The server checkpoint request did not return failure code 72 indicating checkpoint failed.",
                     72, returnCode);

        assertNotNull("The connector runtime did not log message J2CA8512E indicating createTimer() failed with exception.",
                      server.waitForStringInLogUsingMark("J2CA8512E: .* DerbyRA"));

        assertNotNull("The connector runtime did not log message J2CA8511E indicating the checkpoint request failed because the RA created a timer.",
                      server.waitForStringInLogUsingMark("J2CA8511E: .* DerbyRA"));
    }

    @Test
    @ExpectedFFDC({ "io.openliberty.checkpoint.internal.criu.CheckpointFailedException",
                    "javax.resource.spi.work.WorkRejectedException" })
    @AllowedFFDC({ "java.lang.RuntimeException", // JMX local connector unavailable during shutdown of failed checkpoint
                   "javax.transaction.SystemException", "jakarta.transaction.SystemException", "com.ibm.ws.uow.embeddable.SystemException", //
                   "java.lang.IllegalStateException" })
    public void testSubmitWorkAAS() throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.put("-Dunsupported.action", "servlet.init.submit.work");
        server.setJvmOptions(jvmOptions);

        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, true, true, null));
        server.addCheckpointRegexIgnoreMessages("SRVE9967W");
        ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int returnCode = output.getReturnCode();
        assertEquals("The server checkpoint request did not return failure code 72 indicating checkpoint failed.",
                     72, returnCode);

        assertNotNull("The work manager did not log message J2CA8602E indicating the submitted work was rejected with exception.",
                      server.waitForStringInLogUsingMark("J2CA8602E: .*TestWork.* DerbyRA"));

        assertNotNull("The work manager did not log message J2CA8601E indicating the checkpoint request failed because the RA submitted work.",
                      server.waitForStringInLogUsingMark("J2CA8601E: .*DerbyRA .*TestWork"));
    }

    //@Test
    public void testActivationSpecAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testActivationSpec");
    }

    static enum TestMethod {
        testCreateTimerBAS,
        testCreateTimerAAS,
        testSubmitWorkBAS,
        testSubmitWorkAAS,
        unknown;
    }

    /**
     * @return the TestMethod enum value for the current testName (see FATServletClient).
     */
    static public <T extends Enum<T>> T getTestMethod(Class<T> type, TestName testName) {
        String simpleName = getTestMethodNameOnly(testName);
        try {
            T t = Enum.valueOf(type, simpleName);
            Log.info(FATSuite.class, testName.getMethodName(), "got test method: " + t);
            return t;
        } catch (IllegalArgumentException e) {
            Log.info(type, simpleName, "No configuration enum: " + testName);
            fail("Unknown test name: " + testName);
            return null;
        }
    }

    /**
     * @return the testName string without the class name and RepeatTest suffix.
     */
    static String getTestMethodNameOnly(TestName testName) {
        String testMethodSimpleName = getTestMethodSimpleName(testName);
        // Sometimes the method name includes the class name; remove the class name.
        int dot = testMethodSimpleName.indexOf('.');
        if (dot != -1) {
            testMethodSimpleName = testMethodSimpleName.substring(dot + 1);
        }
        return testMethodSimpleName;
    }
}
