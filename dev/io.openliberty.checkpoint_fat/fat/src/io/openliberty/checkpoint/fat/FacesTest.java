/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class FacesTest {

    public static final String FRONTEND_SERVER_NAME = "checkpointFaces-frontendUI";
    public static final String BACKEND_SERVER_NAME = "checkpointFaces-backendServices";

    @Rule
    public TestName testName = new TestName();

    //@ClassRule
    //public static RepeatTests repeatTest = MicroProfileActions.repeat(FRONTEND_SERVER_NAME, TestMode.LITE,
    //                                                                  MicroProfileActions.MP41, MicroProfileActions.MP50);

    @Server(FRONTEND_SERVER_NAME)
    public static LibertyServer frontendUI;

    @Server(BACKEND_SERVER_NAME)
    public static LibertyServer backendServices;

    TestMethod testMethod;

    static WebArchive frontendUIWar;
    static WebArchive backendServicesWar;

    /**
     * Deploy the applications
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        frontendUI.saveServerConfiguration();
        backendServices.saveServerConfiguration();

        frontendUIWar = assembleFrontendWar();
        backendServicesWar = assembleBackendServicesWar();

        // Use derby provided in ${shared.resources.dir}
        //backendServices.copyFileToLibertyServerRoot("publish/shared/resources/derby", "derbyLib", "derby.jar");
    }

    @Before
    public void setUp() throws Exception {
        cleanupSharedResources();
        ShrinkHelper.cleanAllExportedArchives();

        testMethod = getTestMethod(TestMethod.class, testName);
        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                case testFacesRestore:
                    ShrinkHelper.exportAppToServer(frontendUI, frontendUIWar, new DeployOptions[] { OVERWRITE });
                    ShrinkHelper.exportAppToServer(backendServices, backendServicesWar, new DeployOptions[] { OVERWRITE });
                    frontendUI.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                                             server -> {
                                                 assertNotNull("'SRVE0169I: Loading Web Module: frontendUI' message not found in log before restore",
                                                               server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: frontendUI", 0));
                                                 assertNotNull("'CWWKZ0001I: Application frontendUI started' message not found in log.",
                                                               server.waitForStringInLogUsingMark("CWWKZ0001I: Application frontendUI started", 0));
                                             });
                    // Checkpoint and restore
                    frontendUI.startServer(getTestMethodNameOnly(testName) + ".log");
                    break;
                case testPersistenceRestore:
                    ShrinkHelper.exportAppToServer(frontendUI, frontendUIWar, new DeployOptions[] { OVERWRITE });
                    ShrinkHelper.exportAppToServer(backendServices, backendServicesWar, new DeployOptions[] { OVERWRITE });
                    backendServices.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                                                  server -> {
                                                      assertNotNull("'SRVE0169I: Loading Web Module: backendServices' message not found in log before restore",
                                                                    server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: backendServices", 0));
                                                      assertNotNull("'CWWKZ0001I: Application backendServices started' message not found in log.",
                                                                    server.waitForStringInLogUsingMark("CWWKZ0001I: Application backendServices started", 0));
                                                  });
                    // Checkpoint and restore
                    // At least one server.env var must trigger transaction config update at restore
                    backendServices.startServer(getTestMethodNameOnly(testName) + ".log");
                    break;
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }
        } catch (Exception e) {
            throw new AssertionError("Unexpected error during setupTest.", e);
        }
    }

    void cleanupSharedResources() throws Exception {
        backendServices.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    static WebArchive assembleFrontendWar() throws Exception {
        WebArchive frontendUIWar = ShrinkWrap.create(WebArchive.class, "frontendUI.war")
                        .addPackages(true, "io.openliberty.guides.event.client")
                        .addPackages(true, "io.openliberty.guides.event.ui")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/eventmanager.xhtml")),
                             "/eventmanager.xhtml")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/stylesheet.css")),
                             "/stylesheet.css")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/content/eventForm.xhtml")),
                             "/content/eventForm.xhtml")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/content/mainPage.xhtml")),
                             "/content/mainPage.xhtml")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/content/updateEventForm.xhtml")),
                             "/content/updateEventForm.xhtml")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/header/header.xhtml")),
                             "/header/header.xhtml")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/images/openliberty.png")),
                             "/images/openliberty.png")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/META-INF/microprofile-config.properties")),
                             "/META-INF/microprofile-config.properties")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/META-INF/MANIFEST.MF")),
                             "/META-INF/MANIFEST.MF")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/navBar/leftNav.xhtml")),
                             "/navBar/leftNav.xhtml")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/WEB-INF/faces-config.xml")),
                             "/WEB-INF/faces-config.xml")
                        .add(new FileAsset(new File("test-applications/eventMgr/frontendUI/resources/WEB-INF/web.xml")),
                             "/WEB-INF/web.xml");
        return frontendUIWar;
    }

    static WebArchive assembleBackendServicesWar() throws Exception {
        WebArchive backendServicesWar = ShrinkWrap.create(WebArchive.class, "backendServices.war")
                        .addPackages(true, "io.openliberty.guides.event.dao")
                        .addPackages(true, "io.openliberty.guides.event.models")
                        .addPackages(true, "io.openliberty.guides.event.resources")
                        .add(new FileAsset(new File("test-applications/eventMgr/backendServices/resources/META-INF/persistence.xml")),
                             "/WEB-INF/classes/META-INF/persistence.xml")
                        .add(new FileAsset(new File("test-applications/eventMgr/backendServices/resources/META-INF/MANIFEST.MF")),
                             "/META-INF/MANIFEST.MF");
        return backendServicesWar;
    }

    @After
    public void tearDown() throws Exception {
        TestMethod testMethod = getTestMethod(TestMethod.class, testName);
        try {
            Log.info(getClass(), testName.getMethodName(), "Tearing down: " + testMethod);
            switch (testMethod) {
                case testFacesRestore:
                    if (frontendUI.isStarted()) {
                        frontendUI.stopServer();
                    }
                    break;
                case testPersistenceRestore:
                    if (backendServices.isStarted()) {
                        backendServices.stopServer();
                    }
                    break;
                default:
                    Log.info(getClass(), testName.getMethodName(), "No tear down required: " + testMethod);
                    break;
            }
        } catch (Exception e) {
            throw new AssertionError("Unexpected error during teardownTest.", e);
        }
    }

    @Test
    public void testFacesRestore() throws Exception {
        try {
            backendServices.startServer();

            // Hit the event mgr app at http://localhost:8030/eventmanager.jsf
            URL url = createURL(frontendUI, "eventmanager.jsf");
            String response = HttpUtils.getHttpResponseAsString(url);
            assertNotNull(response);
            assertTrue(response.contains("Event Manager"));
        } finally {
            if (backendServices.isStarted()) {
                backendServices.stopServer();
            }
        }
    }

    @Test
    public void testPersistenceRestore() throws Exception {
        try {
            frontendUI.startServer();

            // Hit the event mgr app at http://localhost:8030/eventmanager.jsf
            URL url = createURL(frontendUI, "eventmanager.jsf");
            String response = HttpUtils.getHttpResponseAsString(url);
            assertNotNull(response);
            assertTrue(response.contains("Event Manager"));
        } finally {
            if (frontendUI.isStarted()) {
                frontendUI.stopServer();
            }
        }
    }

    public static URL createURL(LibertyServer server, String path) throws MalformedURLException {
        if (!path.startsWith("/"))
            path = "/" + path;
        return new URL("http://" + server.getHostname() + ":" + server.getHttpSecondaryPort() + path);
    }

    static enum TestMethod {
        testFacesRestore,
        testPersistenceRestore,
        unknown;
    }
}
