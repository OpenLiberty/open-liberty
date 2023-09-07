/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.jpa.tests.container.checkpoint.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.testtooling.vehicle.web.JPAFATServletClient;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class JPADiagnosticsCheckpointTest extends JPAFATServletClient {

    private final static String RESOURCE_ROOT = "test-applications/datasourceApp/";
    private final static String appFolder = "web";
    private final static String appName = "jpadatasourceWeb";
    private final static String appNameEar = appName + ".ear";

    private final static String MSG_APP_START = "CWWKZ0001I.*" + appName;
    private final static String MSG_CHECKPOINT = "CWWKC0451I.*";

    private static final List<String> CHECKPOINT_INACTIVE = Collections.emptyList();
    private static final List<String> CHECKPOINT_BEFORE_APP_START = Arrays.asList("--internal-checkpoint-at=beforeAppStart");
    private static final List<String> CHECKPOINT_AFTER_APP_START = Arrays.asList("--internal-checkpoint-at=afterAppStart");

    private static long timestart = 0;

    @Server("JPACheckpointServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, AbstractFATSuite.JAXB_PERMS);
        bannerStart(JPADiagnosticsCheckpointTest.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server.setConfigUpdateTimeout(120 * 1000);
        }

        setupTestApplication();
    }

    @Before
    public void configSetUp() throws Exception {
        // Every server start/stop clears environment variables, set on each test run
        server.addEnvVar("repeat_phase", AbstractFATSuite.repeatPhase);
    }

    private static void setupTestApplication() throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.datasource.model");
        webApp.addPackages(true, "com.ibm.ws.jpa.datasource.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.datasource.web");
        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + appFolder + "/" + appName + ".war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(webApp);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + appFolder, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }
        });

        ShrinkHelper.exportToServer(server, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(appNameEar);
        appRecord.setName(appName);

        // setup the thirdparty classloader for Hibernate and OpenJPA
        if (AbstractFATSuite.repeatPhase != null && AbstractFATSuite.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        } else if (AbstractFATSuite.repeatPhase != null && AbstractFATSuite.repeatPhase.contains("openjpa")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("OpenJPALib");
            cel.add(loader);
        }

        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server.updateServerConfiguration(sc);
    }

    private static void setCheckpointPhase(CheckpointPhase phase, LibertyServer server) throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        switch (phase) {
            case BEFORE_APP_START:
                jvmOptions.put("-Dio.openliberty.checkpoint.stub.criu", "true");
                server.setExtraArgs(CHECKPOINT_BEFORE_APP_START);
                break;
            case AFTER_APP_START:
                jvmOptions.put("-Dio.openliberty.checkpoint.stub.criu", "true");
                server.setExtraArgs(CHECKPOINT_AFTER_APP_START);
                break;
            default:
                jvmOptions.remove("-Dio.openliberty.checkpoint.stub.criu");
                server.setExtraArgs(CHECKPOINT_INACTIVE);
        }
        server.setJvmOptions(jvmOptions);
    }

    @Test
    public void jpa_AAS_testCheckpointORMDiagnostic() throws Exception {
        testCheckpointORMDiagnostic(CheckpointPhase.AFTER_APP_START);
    }

    @Test
    public void jpa_BAS_testCheckpointORMDiagnostic() throws Exception {
        testCheckpointORMDiagnostic(CheckpointPhase.BEFORE_APP_START);
    }

    @Test
    public void jpa_INACTIVE_testCheckpointORMDiagnostic() throws Exception {
        testCheckpointORMDiagnostic(CheckpointPhase.INACTIVE);
    }

    // Java makes the MD5 hash algorithm unavailable during CRIU checkpoint, only.
    // In order to prevent FFDC events, the JPA ORM diagnostic -- which uses an MD5
    // MessageDigest -- is temporarily disabled for server checkpoint until java makes
    // new algorithms available. And once available, the diagnostic can be enabled
    // using any suitable algorithm for MessageDigest other than MD5 or SHA1.
    /**
     * Verify ORM diagnostics disable during server checkpoint, only.
     *
     * When the JPMORM trace group is enabled, ORM diagnostics are dumped to trace
     * for each application while processing the applicationStarting event. Thus,
     * the diagnostic should not log to trace (disable) during server checkpoint at
     * AFTER_APP_START. but should log (enable) during normal server operation and
     * during server restore from a checkpoint at BEFDRE_APP_START.
     */
    private void testCheckpointORMDiagnostic(final CheckpointPhase phase) throws Exception {
        Assert.assertFalse("Server is already started!", server.isStarted());

        // Checkpoint and restore the server using stubbed CRIU support in a single JVM.
        // Unlike a real checkpoint-restore scenario, this server JVM always makes the MD5
        // hash algorithm available during checkpoint. The test cannot use unexpected FFDC
        // events for NoSuchAlgorithmException to verify server behavior.
        JPADiagnosticsCheckpointTest.setCheckpointPhase(phase, server);
        server.startServer();

        Assert.assertTrue("The JPAORM trace group should be enabled across checkpoint and restore, but is not",
                          server.findStringsInTrace("JPAORM=all").size() >= 2);

        Assert.assertNotNull("Application " + appName + " should start, but did not",
                             server.waitForStringInLog(MSG_APP_START, server.getDefaultTraceFile()));

        switch (phase) {
            case AFTER_APP_START:
                Assert.assertTrue("JPA ORM diagnostics should disable during server checkpoint AFTER_APP_START, but did not",
                                  server.findStringsInTrace("JPA ORM diagnostics are unavailable during server checkpoint").size() >= 1);

                Assert.assertTrue("A server checkpoint was not requested, but should have",
                                  server.findStringsInTrace(MSG_CHECKPOINT).size() >= 1);
                break;
            case BEFORE_APP_START:
                Assert.assertTrue("JPA ORM diagnostics should log to trace when restoring a server checkpointed BEFORE_APP_START, but did not",
                                  server.findStringsInTrace("Encapsulated JPA Diagnostic Data").size() >= 1);

                Assert.assertTrue("A server checkpoint was not requested, but should have",
                                  server.findStringsInTrace(MSG_CHECKPOINT).size() >= 1);
                break;
            default: // INACTIVE
                Assert.assertTrue("JPA ORM diagnostics should log to trace during normal server operation, but did not",
                                  server.findStringsInTrace("Encapsulated JPA Diagnostic Data").size() >= 1);

                Assert.assertTrue("A server checkpoint was requested unexpectedly",
                                  server.findStringsInTrace(MSG_CHECKPOINT).isEmpty());
                break;
        }
    }

    @After
    public void configTearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        bannerEnd(JPADiagnosticsCheckpointTest.class, timestart);
    }
}
