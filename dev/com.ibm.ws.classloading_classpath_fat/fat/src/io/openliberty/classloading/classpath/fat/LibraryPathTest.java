/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.LIB_FILESET_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB6_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB7_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB8_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB9_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB_FILESET_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB_FILESET_WAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.classloading.lib.path.test.app.LibPathTestServlet;

/**
 *
 */
@RunWith(FATRunner.class)
public class LibraryPathTest extends FATServletClient {

    @Server(LIB_FILESET_TEST_SERVER)
    @TestServlet(servlet = LibPathTestServlet.class, contextRoot = TEST_LIB_FILESET_APP)
    public static LibertyServer server;

    @BeforeClass
    public static void setupTestServer() throws Exception {
        ShrinkHelper.exportAppToServer(server, TEST_LIB_FILESET_WAR, DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB6_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB7_JAR, DeployOptions.SERVER_ONLY);
        setupLibraryFolder(TEST_LIB8_JAR);
        setupLibraryFolder(TEST_LIB9_JAR);

        server.startServer();
    }

    private static void setupLibraryFolder(JavaArchive library) throws Exception {
        ShrinkHelper.exportArtifact(library, "publish/libs", true, false, true);
        String libJarName = library.getName();
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/usr/servers/" + LIB_FILESET_TEST_SERVER + "/libs",
                                               libJarName.substring(0, libJarName.length() - 4),
                                               "publish/libs/" + libJarName, true, server.getServerRoot());
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer("CWWKL0019W");
    }

    @Test
    public void testWarning() throws Exception {
        List<String> warnings = server.findStringsInLogs("CWWKL0019W");
        assertEquals("Wrong number of warnings: " + warnings, 1, warnings.size());
        assertTrue("Unexpected warning content: " + warnings.get(0), warnings.get(0).contains("DOES_NOT_EXIST"));
    }
}
