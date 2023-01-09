/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpaContainer.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Bell;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.JPA;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class JPAContainerModifyConfigTest extends FATServletClient {

    @Server("com.ibm.ws.jpa.container.fat.modifyconfig")
    public static LibertyServer server;

    private static ServerConfiguration originalConfig;

    private static final String SERVLET_NAME = "JPAContainerModifyConfigTestServlet";
    private static final String JEE_APP = "jpaContainerApp";
    private static final String MSG_NO_PROVIDER_FOUND = "CWWJP0051E";
    private static final String MSG_APP_FAIL = "CWWKZ0004E.*" + JEE_APP;
    private static final String MSG_APP_READY = "CWWKZ0003I.*" + JEE_APP;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a normal Java EE application and export to server
        WebArchive app = ShrinkWrap.create(WebArchive.class, JEE_APP + ".war")//
                        .addPackage("jpa.ecl.web")//
                        .addPackage("jpa.entity");//
//                        .addAsWebInfResource(new File("test-applications/" + JEE_APP + "/resources/META-INF/persistence.xml"));
        ShrinkHelper.addDirectory(app, "test-applications/" + JEE_APP + "/resources/");
        ShrinkHelper.exportToServer(server, "apps", app);

        originalConfig = server.getServerConfiguration();
        server.addInstalledAppForValidation(JEE_APP);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWJP0051E", // no JPA provider found
                          "CWWJP0050E.*BogusProvider", // JPA provider class could not be loaded
                          "CWWKG0033W.*ecl", // Referenced library 'ecl' could not be found (for testRemoveLibrary)
                          "CWWKL0055W.*ecl"); // Library 'ecl' is empty (for testModifyLibraryFiles)
    }

    @Before
    public void beforeEach() throws Exception {
        // Ensure that log mark doesn't extend beyond the scope of a test
        server.setMarkToEndOfLog();
    }

    private void runJPATest(int id) throws Exception {
        runTest(server, JEE_APP + '/' + SERVLET_NAME, "testInsertDelete&id=" + id + "&invokedBy=" + testName.getMethodName());
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testRemoveBell() throws Exception {
        runJPATest(1);

        // Remove <bell id="bell_ecl" libraryRef="ecl"/>
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Bell> bells = config.getBells();
        assertNotNull(bells.removeById("bell_ecl"));
        server.updateServerConfiguration(config);

        // App should fail to start and be unusable
        assertNotNull(MSG_NO_PROVIDER_FOUND + " message not found", server.waitForStringInLogUsingMark(MSG_NO_PROVIDER_FOUND));
        assertNotNull(JEE_APP + " did not fail to start", server.waitForStringInLogUsingMark(MSG_APP_FAIL));
        assertEquals("App should fail to start and be unusable since JPA Provider has gone away",
                     HttpURLConnection.HTTP_NOT_FOUND,
                     runTestForResponseCode(server, JEE_APP + '/' + SERVLET_NAME, "testInsertDelete"));

        // Put the <bell> back in, JPA should work again
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalConfig);
        assertNotNull(JEE_APP + " failed to start", server.waitForStringInLogUsingMark(MSG_APP_READY));
        runJPATest(2);
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testRemoveLibrary() throws Exception {
        runJPATest(3);

        // Remove <library id="ecl"> by changing its ID
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Library> libs = config.getLibraries();
        Library eclLib = libs.getById("ecl");
        eclLib.setId("modified-ecl");
        server.updateServerConfiguration(config);

        // App should fail to start and be unusable
        assertNotNull(MSG_NO_PROVIDER_FOUND + " message not found", server.waitForStringInLogUsingMark(MSG_NO_PROVIDER_FOUND));
        assertNotNull(JEE_APP + " did not fail to start", server.waitForStringInLogUsingMark(MSG_APP_FAIL));
        assertEquals("App should fail to start and be unusable since JPA Provider has gone away",
                     HttpURLConnection.HTTP_NOT_FOUND,
                     runTestForResponseCode(server, JEE_APP + '/' + SERVLET_NAME, "testInsertDelete"));

        // Put the original config back, JPA should work again
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalConfig);
        assertNotNull(JEE_APP + " failed to start", server.waitForStringInLogUsingMark(MSG_APP_READY));
        runJPATest(4);
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "java.lang.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testModifyLibraryFiles() throws Exception {
        runJPATest(5);

        // Move the eclipselink jar that has META-INF/services/javax.persistence.spi.PersistenceProvider
        // in it to a different folder
        RemoteFile origJar = null;
        RemoteFile eclRoot = server.getFileFromLibertySharedDir("resources/ecl"); //  server.getFileFromLibertyServerRoot("ecl");
        RemoteFile[] eclFiles = eclRoot.list(false);
        for (RemoteFile remoteFile : eclFiles) {
            if (remoteFile.getName().startsWith("com.ibm.websphere.appserver.thirdparty.eclipselink")) {
                origJar = remoteFile;
                break;
            }
        }
        assertNotNull("Unable to find the eclipselink jar in " + eclRoot.getAbsolutePath(), origJar);

        RemoteFile copyJar = new RemoteFile(origJar.getMachine(), origJar.getParentFile().getParent() + "/copylib/" + origJar.getName());
        LibertyFileManager.moveLibertyFile(origJar, copyJar);

        try {
            // App should fail to start and be unusable
            assertNotNull(MSG_NO_PROVIDER_FOUND + " message not found", server.waitForStringInLogUsingMark(MSG_NO_PROVIDER_FOUND));
            assertNotNull(JEE_APP + " did not fail to start", server.waitForStringInLogUsingMark(MSG_APP_FAIL));
            assertEquals("App should fail to start and be unusable since JPA Provider has gone away",
                         HttpURLConnection.HTTP_NOT_FOUND,
                         runTestForResponseCode(server, JEE_APP + '/' + SERVLET_NAME, "testInsertDelete"));
        } finally {
            // Put the eclipselink jar back
            server.setMarkToEndOfLog();
            LibertyFileManager.moveLibertyFile(copyJar, origJar);
        }

        // JPA should work again
        assertNotNull(JEE_APP + " failed to start", server.waitForStringInLogUsingMark(MSG_APP_READY));
        runJPATest(6);
    }

    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException",
                    "com.ibm.wsspi.injectionengine.InjectionException",
                    "javax.servlet.UnavailableException" })
    public void testModifyJPAElement() throws Exception {
        // First add <jpa defaultPersistenceProvider="org.eclipse.persistence.jpa.PersistenceProvider"/>
        ServerConfiguration config = server.getServerConfiguration();
        JPA jpaElement = new JPA();
        jpaElement.setDefaultPersistenceProvider("org.eclipse.persistence.jpa.PersistenceProvider");
        config.getJPAs().add(jpaElement);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        assertNotNull(JEE_APP + " failed to start", server.waitForStringInLogUsingMark(MSG_APP_READY));

        // JPA should work with the <jpa> element added dynamically
        runJPATest(7);

        // Now set <jpa defaultPersistenceProvider="BogusProvider"/> and app should restart, but be unusable
        jpaElement.setDefaultPersistenceProvider("BogusProvider");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        assertNotNull(JEE_APP + " failed to start", server.waitForStringInLogUsingMark(MSG_APP_READY));
        assertEquals("App should start, but be unusable since JPA Provider has gone away",
                     HttpURLConnection.HTTP_NOT_FOUND,
                     runTestForResponseCode(server, JEE_APP + '/' + SERVLET_NAME, "testInsertDelete"));

        // Now remove the <jpa> element.  App should restart and be usable
        config.getJPAs().remove(jpaElement);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        assertNotNull(JEE_APP + " failed to start", server.waitForStringInLogUsingMark(MSG_APP_READY));
        runJPATest(8);
    }
}