/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ErrorPathsTest extends FATServletClient {

    public static final String JSF_APP_BAD_API = "jsfApp_badApi";
    public static final String JSF_APP_BAD_IMPL = "jsfApp_badImpl";

    @Server("jsf.container.2.2_fat.errorpaths")
    public static LibertyServer server;

    /**
     * Verify that the jsf-2.2 and jsfContainer-2.2 features cannot be loaded at the same time
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testFeatureConflict() throws Exception {
        ServerConfiguration originalConfig = server.getServerConfiguration().clone();
        server.setServerConfigurationFile("server_" + testName.getMethodName() + ".xml");
        try {
            server.startServer(testName.getMethodName() + ".log");
            assertNotNull(server.waitForStringInLog(".* CWWKF0033E: " +
                                                    ".* com.ibm.websphere.appserver.jsfProvider-2.2.0.[MyFaces|Container]" +
                                                    ".* com.ibm.websphere.appserver.jsfProvider-2.2.0.[MyFaces|Container].*"));
        } finally {
            server.stopServer("CWWKF0033E");
            server.updateServerConfiguration(originalConfig);
        }
    }

    /**
     * Verify that JSF Container rejects a non-matching 'Specification-Version'
     * manifest header in the JSF API jar
     */
    @Test
    @AllowedFFDC
    public void testBadApiVersion() throws Exception {
        // Build test app with that has JSF spec API Specification-Version of 2.1
        JavaArchive badApiJar = ShrinkWrap.create(JavaArchive.class)
                        .as(ZipImporter.class)
                        .importFrom(new File(FATSuite.MOJARRA_API))
                        .as(JavaArchive.class)
                        .setManifest(new File("lib/LibertyFATTestFiles/MANIFEST_badApi.MF"));

        WebArchive jsfApp = ShrinkHelper.buildDefaultApp(JSF_APP_BAD_API, "jsf.container.bean");
        jsfApp = (WebArchive) ShrinkHelper.addDirectory(jsfApp, "test-applications/jsfApp/resources/");
        jsfApp = jsfApp.addAsLibraries(badApiJar);
        jsfApp = jsfApp.addAsLibrary(new File(FATSuite.MOJARRA_IMPL));
        ShrinkHelper.exportAppToServer(server, jsfApp);
        setAppInConfig(JSF_APP_BAD_API);

        server.startServer(testName.getMethodName() + ".log");
        try {
            assertNotNull(server.waitForStringInLog(".*JSFG0103E:.*"));
        } finally {
            server.stopServer(".*"); // lots of stuff will go wrong in this error path test
        }
    }

    /**
     * Verify that JSF Container rejects a non-matching 'Specification-Version'
     * manifest header in the JSF impl jar
     */
    @Test
    @AllowedFFDC
    public void testBadImplVersion() throws Exception {
        // Build test app with that has JSF spec API Specification-Version of 2.1
        JavaArchive badImplJar = ShrinkWrap.create(JavaArchive.class)
                        .as(ZipImporter.class)
                        .importFrom(new File(FATSuite.MOJARRA_IMPL))
                        .as(JavaArchive.class)
                        .setManifest(new File("lib/LibertyFATTestFiles/MANIFEST_badImpl.MF"));

        WebArchive jsfApp = ShrinkHelper.buildDefaultApp(JSF_APP_BAD_IMPL, "jsf.container.bean");
        jsfApp = (WebArchive) ShrinkHelper.addDirectory(jsfApp, "test-applications/jsfApp/resources/");
        jsfApp = jsfApp.addAsLibraries(badImplJar);
        jsfApp = jsfApp.addAsLibrary(new File(FATSuite.MOJARRA_API));
        ShrinkHelper.exportAppToServer(server, jsfApp);
        setAppInConfig(JSF_APP_BAD_IMPL);

        server.startServer(testName.getMethodName() + ".log");
        try {
            assertNotNull(server.waitForStringInLog(".*JSFG0104E:.*"));
        } finally {
            server.stopServer(".*"); // lots of stuff will go wrong in this error path test
        }
    }

    private static void setAppInConfig(String appName) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getApplications().clear();
        config.addApplication(appName, appName + ".war", "war");
        server.updateServerConfiguration(config);
    }
}
