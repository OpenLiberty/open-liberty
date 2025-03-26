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

import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_EAR_LIB;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_EAR_LIB2;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_EAR_LIB2_NAME;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_EAR_LOOSE_TEST_SERVER;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_EAR_PATH;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_EJB;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_RAR;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_RAR_NAME;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_RESOURCE_ADAPTOR;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_RESOURCE_ADAPTOR_NAME;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR_LIB;
import static io.openliberty.classloading.sharedclasses.fat.FATSuite.SHARED_CLASSES_WAR_NAME;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.Archive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.classloading.sharedclasses.fat.SharedClassTestRule.ServerMode;
import io.openliberty.classloading.sharedclasses.war.TestSharedClassesWar;

/**
 *
 */
@RunWith(FATRunner.class)
@OnlyIfSysProp("com.ibm.oti.shared.enabled")
public class SharedClassesEarLooseTest extends FATServletClient {


    @Server(SHARED_CLASSES_EAR_LOOSE_TEST_SERVER)
    @TestServlet(servlet = TestSharedClassesWar.class, contextRoot = SHARED_CLASSES_WAR_NAME)
    public static LibertyServer server;

    @ClassRule
    public static SharedClassTestRule sharedClassTestRule = new SharedClassTestRule()
                        .setConsoleLogName(SharedClassesEarLooseTest.class.getSimpleName())
                        .setRunAutoExpand(false) // no need to run auto expand for look application test
                        .setIsClassModified((s) -> s.endsWith(".A"))
                        .setServerSetup(SharedClassesEarLooseTest::setupTestApp);

    public static LibertyServer setupTestApp(ServerMode mode) throws Exception {
        if (mode == ServerMode.storeInCache) {
            setupLooseContent(SHARED_CLASSES_WAR);
            setupLooseContent(SHARED_CLASSES_WAR_LIB);
            setupLooseContent(SHARED_CLASSES_EJB);
            setupLooseContent(SHARED_CLASSES_RAR);
            setupLooseContent(SHARED_CLASSES_RESOURCE_ADAPTOR);
            setupLooseContent(SHARED_CLASSES_EAR_LIB);
            setupLooseContent(SHARED_CLASSES_EAR_LIB2);

            RemoteFile warLib = server.getFileFromLibertyServerRoot("/looseContent/" + SHARED_CLASSES_WAR_NAME + "/WEB-INF/lib");
            warLib.delete();
            RemoteFile raJar = server.getFileFromLibertyServerRoot("/looseContent/" + SHARED_CLASSES_RAR_NAME + "/" + SHARED_CLASSES_RESOURCE_ADAPTOR_NAME + ".jar");
            raJar.delete();

            // Split apart the WAR classes to create 4 different locations on disk that contribute to WEB-INF/classes
            // Keep the test servlet implementations in the original WEB-INF/classes directory

            // Move the "a" package to "warPkgA"; keeping the package directory structure
            String warAPackageDir = io.openliberty.classloading.sharedclasses.war.a.A.class.getPackage().getName().replace('.', '/');
            RemoteFile warAPackage = server.getFileFromLibertyServerRoot("/looseContent/" + SHARED_CLASSES_WAR_NAME + "/WEB-INF/classes/" + warAPackageDir);
            RemoteFile warAPackageDest = server.getMachine().getFile(server.getFileFromLibertyServerRoot("looseContent"), "warPkgA/" + warAPackageDir);
            new File(warAPackageDest.getAbsolutePath()).getParentFile().mkdirs();
            assertTrue("Could not rename package directory: " + warAPackageDest.getAbsolutePath(), warAPackage.rename(warAPackageDest));

            // Move the "b" package to "warPkgB"; keeping the package directory structure
            String warBPackageDir = io.openliberty.classloading.sharedclasses.war.b.B.class.getPackage().getName().replace('.', '/');
            RemoteFile warBPackage = server.getFileFromLibertyServerRoot("/looseContent/" + SHARED_CLASSES_WAR_NAME + "/WEB-INF/classes/" + warBPackageDir);
            RemoteFile warBPackageDest = server.getMachine().getFile(server.getFileFromLibertyServerRoot("looseContent"), "warPkgB/" + warBPackageDir);
            new File(warBPackageDest.getAbsolutePath()).getParentFile().mkdirs();
            assertTrue("Could not rename package directory: " + warBPackageDest.getAbsolutePath(), warBPackage.rename(warBPackageDest));

            // Move the "c" package to "warPkgC"; remove the package directory structure
            String warCPackageDir = io.openliberty.classloading.sharedclasses.war.c.C.class.getPackage().getName().replace('.', '/');
            RemoteFile warCPackage = server.getFileFromLibertyServerRoot("/looseContent/" + SHARED_CLASSES_WAR_NAME + "/WEB-INF/classes/" + warCPackageDir);
            RemoteFile warCPackageDest = server.getMachine().getFile(server.getFileFromLibertyServerRoot("looseContent"), "warPkgC");
            assertTrue("Could not rename package directory: " + warCPackageDest.getAbsolutePath(), warCPackage.rename(warCPackageDest));

            // Need another test with no package directory structure for A and keeps package directory structure for B
            // This forces a single root URL for the loose container for B
            String earLib2APackageDir = io.openliberty.classloading.sharedclasses.earlib2.a.A.class.getPackage().getName().replace('.', '/');
            RemoteFile earLib2APackage = server.getFileFromLibertyServerRoot("/looseContent/" + SHARED_CLASSES_EAR_LIB2_NAME + "/" + earLib2APackageDir);
            RemoteFile earLib2APackageDest = server.getMachine().getFile(server.getFileFromLibertyServerRoot("looseContent"), "earLib2PkgA");
            assertTrue("Could not rename package directory: " + earLib2APackageDest.getAbsolutePath(), earLib2APackage.rename(earLib2APackageDest));

            // Move the "b" package to "earLib2PkgB"; keeping the package directory structure
            String earLib2BPackageDir = io.openliberty.classloading.sharedclasses.earlib2.b.B.class.getPackage().getName().replace('.', '/');
            RemoteFile earLib2BPackage = server.getFileFromLibertyServerRoot("/looseContent/" + SHARED_CLASSES_EAR_LIB2_NAME + "/" + earLib2BPackageDir);
            RemoteFile earLib2BPackageDest = server.getMachine().getFile(server.getFileFromLibertyServerRoot("looseContent"), "earLib2PkgB/" + earLib2BPackageDir);
            new File(earLib2BPackageDest.getAbsolutePath()).getParentFile().mkdirs();
            assertTrue("Could not rename package directory: " + earLib2BPackageDest.getAbsolutePath(), earLib2BPackage.rename(earLib2BPackageDest));
        }
        if (mode == ServerMode.modifyAppClasses) {
            Thread.sleep(5000);
            // touch every A.class to mimic changes to invalidate cache
            Path dir = Paths.get(server.getInstallRoot() + "/usr/servers/" + SHARED_CLASSES_EAR_LOOSE_TEST_SERVER + "/looseContent");
            try (Stream<Path> stream = Files.walk(dir)) {
                stream.forEach(p -> {
                   File f = p.toFile();
                   if (f.isFile() && "A.class".equals(f.getName())) {
                       f.setLastModified(System.currentTimeMillis());
                   }
                });
            } catch (IOException e) {
                Log.error(SharedClassesEarLooseTest.class, "setupTestApp", e);
                fail(e.getMessage());
            }
        }
        return server;
    }

    private static void setupLooseContent(Archive<?> archive) throws Exception {
        ShrinkHelper.exportArtifact(archive, "publish/looseContent", true, true, true);
        String archiveName = archive.getName();
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/usr/servers/" + SHARED_CLASSES_EAR_LOOSE_TEST_SERVER + "/looseContent",
                                               archiveName.substring(0, archiveName.length() - 4), "publish/looseContent/" + archiveName, true, server.getServerRoot());
    }

    private void runTest() throws Exception {
        sharedClassTestRule.runSharedClassTest(SHARED_CLASSES_EAR_PATH, getTestMethodSimpleName());
    }

    @Test
    public void testWarClassesA() throws Exception {
        runTest();
    }

    @Test
    public void testWarClassesB() throws Exception {
        runTest();
    }

    @Test
    public void testWarClassesC() throws Exception {
        runTest();
    }

    @Test
    public void testWarLibA() throws Exception {
        runTest();
    }

    @Test
    public void testWarLibB() throws Exception {
        runTest();
    }

    @Test
    public void testEjbClassesA() throws Exception {
        runTest();
    }

    @Test
    public void testEjbClassesB() throws Exception {
        runTest();
    }

    @Test
    public void testEarLibA() throws Exception {
        runTest();
    }

    @Test
    public void testEarLibB() throws Exception {
        runTest();
    }

    @Test
    public void testEarLib2A() throws Exception {
        runTest();
    }

    @Test
    public void testEarLib2B() throws Exception {
        runTest();
    }

    @Test
    public void testResoureAdaptorClassesA() throws Exception {
        runTest();
    }

    @Test
    public void testResoureAdaptorClassesB() throws Exception {
        runTest();
    }

    @Test
    public void testRarClassesA() throws Exception {
        runTest();
    }

    @Test
    public void testRarClassesB() throws Exception {
        runTest();
    }
}
