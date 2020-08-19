/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.jpa;

import java.io.File;
import java.util.HashSet;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.PrivHelper;
import io.openliberty.jpa.cev.web.TestConcurrentEnhancementVerificationServlet;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class TestConcurrentEnhancement extends JPAFATServletClient {
    private final static String CONTEXT_ROOT = "cev";
    private final static String RESOURCE_ROOT = "test-applications/ConcurrentEnhancementVerification/";
    private final static String appFolder = "web";
    private final static String appName = "cev";
    private final static String appNameEar = appName + ".ear";

    private static long timestart = 0;
    private static final Class<?> c = TestConcurrentEnhancement.class;

    @Server("ConcurrentEnhancementVerification")
    @TestServlets({
                    @TestServlet(servlet = TestConcurrentEnhancementVerificationServlet.class, path = CONTEXT_ROOT + "/" + "TCEVS")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LibertyServer serverTest = LibertyServerFactory.getLibertyServer("ConcurrentEnhancementVerification");

        File jpa20FeatureMF = new File(serverTest.getInstallRoot() + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
        Log.info(c, "isEnabled", "Before class1: Does the jpa-2.0 feature exist? " + jpa20FeatureMF.exists());

        PrivHelper.generateCustomPolicy(server, FATSuite.JAXB_PERMS);
        bannerStart(TestConcurrentEnhancement.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server.setConfigUpdateTimeout(120 * 1000);
        }

        server.startServer();

        setupTestApplication();

        File jpa20FeatureMF2 = new File(serverTest.getInstallRoot() + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
        Log.info(c, "isEnabled", "Before class2: Does the jpa-2.0 feature exist? " + jpa20FeatureMF2.exists());

    }

    private static void setupTestApplication() throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, "io.openliberty.jpa.cev.model");
        webApp.addPackages(true, "io.openliberty.jpa.cev.web");
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

        server.setMarkToEndOfLog();
        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            LibertyServer serverTest = LibertyServerFactory.getLibertyServer("ConcurrentEnhancementVerification");

            File jpa20FeatureMF = new File(serverTest.getInstallRoot() + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
            Log.info(c, "isEnabled", "Before class1: Does the jpa-2.0 feature exist? " + jpa20FeatureMF.exists());

            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            try {
                ServerConfiguration sc = server.getServerConfiguration();
                sc.getApplications().clear();
                server.updateServerConfiguration(sc);
                server.saveServerConfiguration();

                server.deleteFileFromLibertyServerRoot("apps/" + appNameEar);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(TestConcurrentEnhancement.class, timestart);
        }
    }
}
