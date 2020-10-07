/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.java11_fat;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import java11.multirelease.web.MultiReleaseJarTestServlet;

@RunWith(FATRunner.class)
public class MultiReleaseJarTest extends FATServletClient {

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
