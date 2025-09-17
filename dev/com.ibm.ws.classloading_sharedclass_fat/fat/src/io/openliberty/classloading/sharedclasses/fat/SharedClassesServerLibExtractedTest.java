/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.sharedclasses.fat;

import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_LIB_TEST_SERVER;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_SERVER_LIB;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_SERVER_LIB_PATH;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR_NAME;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.classloading.sharedclasses.fat.SharedClassTestRule.ServerMode;
import io.openliberty.classloading.sharedclasses.war.TestSharedClassesServerLib;

/**
 *
 */
@RunWith(FATRunner.class)
@OnlyIfSysProp("com.ibm.oti.shared.enabled")
public class SharedClassesServerLibExtractedTest extends FATServletClient {


    @Server(SHARED_CLASSES_LIB_TEST_SERVER)
    @TestServlet(servlet = TestSharedClassesServerLib.class, contextRoot = SHARED_CLASSES_WAR_NAME)
    public static LibertyServer server;

    @ClassRule
    public static SharedClassTestRule sharedClassTestRule = new SharedClassTestRule()
                        .setConsoleLogName(SharedClassesServerLibExtractedTest.class.getSimpleName())
                        .setServerSetup(SharedClassesServerLibExtractedTest::setupTestApp)
                        .setRunAutoExpand(false) // no need to expand the app for this test on server libraries
                        .setIsClassModified((s) -> s.endsWith(".A"));

    public static LibertyServer setupTestApp(ServerMode mode) throws Exception {
        if (mode == ServerMode.storeInCache) {
            ShrinkHelper.exportAppToServer(server, SHARED_CLASSES_WAR, DeployOptions.SERVER_ONLY);
            setupLibraryFolder(SHARED_CLASSES_SERVER_LIB);
        }
        if (mode == ServerMode.modifyAppClasses) {
            Thread.sleep(5000);
            // touch every A.class to mimic changes to invalidate cache
            Path dir = Paths.get(server.getInstallRoot() + "/usr/servers/" + SHARED_CLASSES_LIB_TEST_SERVER + "/libs/" + SHARED_CLASSES_SERVER_LIB.getName());
            try (Stream<Path> stream = Files.walk(dir)) {
                stream.forEach(p -> {
                   File f = p.toFile();
                   if (f.isFile() && "A.class".equals(f.getName())) {
                       f.setLastModified(System.currentTimeMillis());
                   }
                });
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
        return server;
    }

    private static void setupLibraryFolder(JavaArchive library) throws Exception {
        ShrinkHelper.exportArtifact(library, "publish/libs", true, true, true);
        String libJarName = library.getName();
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/usr/servers/" + SHARED_CLASSES_LIB_TEST_SERVER + "/libs",
                                               libJarName, "publish/libs/" + libJarName, true, server.getServerRoot());
    }

    private void runTest() throws Exception {
        sharedClassTestRule.runSharedClassTest(SHARED_CLASSES_SERVER_LIB_PATH, getTestMethodSimpleName());
    }

    @Test
    public void testServerLibClassesA() throws Exception {
        runTest();
    }

    @Test
    public void testServerLibClassesB() throws Exception {
        runTest();
    }
}
