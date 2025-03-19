/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package com.ibm.ws.java11_fat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import java11.multirelease.web.MultiReleaseJarTestServlet;

@RunWith(FATRunner.class)
public class MultiReleaseJarTestWithJarURLs extends FATServletClient {

    private static final String REGULAR_APP = "multiReleaseApp";
    private static final String SHARED_LIB_APP = "multiReleaseSharedLibApp";
    private static int EXPECTED_JAVA = -1;

    @Server("server_MultiReleaseJarTest")
    @TestServlets({
                    @TestServlet(servlet = MultiReleaseJarTestServlet.class, contextRoot = REGULAR_APP),
                    @TestServlet(servlet = MultiReleaseJarTestServlet.class, contextRoot = SHARED_LIB_APP)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // This app includes a manually built Multi-Release (MR) jar file with the following structure
        // /java11/multirelease/jar/<classes for all JDKs>
        // /META-INF/versions/8/<classesfor JDK 8+>
        // /META-INF/versions/9/<classesfor JDK 9+>
        // etc...
        WebArchive mrJarInWarApp = ShrinkHelper.buildDefaultApp(REGULAR_APP, "java11.multirelease.web")
                        .addAsLibrary(new File("publish/servers/server_MultiReleaseJarTest/lib/multiRelease.jar"));
        ShrinkHelper.exportAppToServer(server, mrJarInWarApp);
        server.addInstalledAppForValidation(REGULAR_APP);

        ServerConfiguration config = server.getServerConfiguration();
        config.getClassLoadingElement().setUseJarUrls(true);
        server.updateServerConfiguration(config);
        Properties bootStrapProperties = server.getBootstrapProperties();

        // Exempting security and removing all security bootstrap properties.
        // The jar protocol does not protect users of the URL from file permission checks
        // on read; therefore exempting this test from security runs.
        bootStrapProperties.put("websphere.java.security.exempt", "true");
        bootStrapProperties.remove("websphere.java.security");
        bootStrapProperties.remove("websphere.java.security.norethrow");
        bootStrapProperties.remove("websphere.java.security.unique");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(server.getServerBootstrapPropertiesFile().getAbsolutePath()))) {
            for (String key : bootStrapProperties.stringPropertyNames()) {
                String value = bootStrapProperties.getProperty(key);
                writer.write(key + "=" + value);
                writer.newLine();
            }
        }

        // This app includes multiRelease.jar as a shared library via server.xml
        ShrinkHelper.defaultApp(server, SHARED_LIB_APP, "java11.multirelease.web");

        server.startServer();

        EXPECTED_JAVA = JavaInfo.forServer(server).majorVersion();
        if (EXPECTED_JAVA > 17)
            // don't bother updating past Java 17, we get the point after that many releases
            EXPECTED_JAVA = 17;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testOverriddenClass_RegApp() throws Exception {

        FATServletClient.runTest(server, REGULAR_APP + "/MultiReleaseJarTestServlet",
                                 "testOverriddenClass&" + MultiReleaseJarTestServlet.EXPECTED_JAVA_LEVEL + '=' + EXPECTED_JAVA);
    }

    @Test
    public void testOverriddenClass_SharedLibApp() throws Exception {

        FATServletClient.runTest(server, SHARED_LIB_APP + "/MultiReleaseJarTestServlet",
                                 "testOverriddenClass&" + MultiReleaseJarTestServlet.EXPECTED_JAVA_LEVEL + '=' + EXPECTED_JAVA);
    }

}
