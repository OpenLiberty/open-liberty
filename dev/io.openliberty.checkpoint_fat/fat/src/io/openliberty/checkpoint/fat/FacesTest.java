/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class FacesTest {

    public static final String FRONTEND_SERVER_NAME = "checkpointFaces-frontendUI";
    public static final String BACKEND_SERVER_NAME = "checkpointFaces-backendServices";

    @Rule
    public TestName testName = new TestName();

    //@ClassRule
    //public static RepeatTests repeatTest = MicroProfileActions.repeat(FRONTEND_SERVER_NAME, TestMode.LITE,
    //                                                                  MicroProfileActions.MP41, MicroProfileActions.MP50);

    public static LibertyServer frontendUI;
    public static LibertyServer backendServices;

    /**
     * Deploy the applications
     */
    @BeforeClass
    public static void setUpClass() throws Exception {

        frontendUI = LibertyServerFactory.getLibertyServer(FRONTEND_SERVER_NAME);
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
        ShrinkHelper.exportAppToServer(frontendUI, frontendUIWar);

        backendServices = LibertyServerFactory.getLibertyServer(BACKEND_SERVER_NAME);
        WebArchive backendServicesWar = ShrinkWrap.create(WebArchive.class, "backendServices.war")
                        .addPackages(true, "io.openliberty.guides.event.dao")
                        .addPackages(true, "io.openliberty.guides.event.models")
                        .addPackages(true, "io.openliberty.guides.event.resources")
                        .add(new FileAsset(new File("test-applications/eventMgr/backendServices/resources/META-INF/persistence.xml")),
                             "/WEB-INF/classes/META-INF/persistence.xml")
                        .add(new FileAsset(new File("test-applications/eventMgr/backendServices/resources/META-INF/MANIFEST.MF")),
                             "/META-INF/MANIFEST.MF");
        ShrinkHelper.exportAppToServer(backendServices, backendServicesWar);
        // Skip this. Use derby provided in ${shared.resources.dir}
        //backendServices.copyFileToLibertyServerRoot("publish/shared/resources/derby", "derbyLib", "derby.jar");
    }

    @Before
    public void setUp() throws Exception {
        frontendUI.setCheckpoint(CheckpointPhase.APPLICATIONS, true,
                                 server -> {
                                     assertNotNull("'SRVE0169I: Loading Web Module: frontendUI' message not found in log before restore",
                                                   server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: frontendUI", 0));
                                     assertNotNull("'CWWKZ0001I: Application frontendUI started' message not found in log.",
                                                   server.waitForStringInLogUsingMark("CWWKZ0001I: Application frontendUI started", 0));
                                 });
        frontendUI.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    @After
    public void tearDown() throws Exception {
        frontendUI.stopServer();
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

    public static URL createURL(LibertyServer server, String path) throws MalformedURLException {
        if (!path.startsWith("/"))
            path = "/" + path;
        return new URL("http://" + server.getHostname() + ":" + server.getHttpSecondaryPort() + path);
    }
}
