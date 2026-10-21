/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.classloading.classpath.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.NATIVE_LIBRARY_ACCESS_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB1_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_NATIVE_LIBRARY_WAR;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Verifies that Liberty logs CWWKL0085W when a shared library is configured
 * with native files and the JVM was not started with native access enabled
 * for classpath code on Java 24+.
 *
 * <p>Follows the same pattern as {@link ParentLastLibraryFeatureTests}: the
 * server starts once, native file config is injected before start, and the
 * log is checked after startup. Tests are skipped below Java 24.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 24)
public class NativeLibraryAccessTest {

    @Server(NATIVE_LIBRARY_ACCESS_TEST_SERVER)
    public static LibertyServer server;

    private static final String LIB_ID = "nativeAccessLib";

    @BeforeClass
    public static void setupTestServer() throws Exception {
        ShrinkHelper.exportAppToServer(server, TEST_NATIVE_LIBRARY_WAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/nativeAccess", TEST_LIB1_JAR, DeployOptions.SERVER_ONLY);

        // Add a fake native file entry to the library so the warning fires.
        // File content is irrelevant — the warning fires at registration time.
        File nativeDir = new File(server.getServerRoot(), "nativeAccess");
        nativeDir.mkdirs();
        new File(nativeDir, System.mapLibraryName("accessNative1")).createNewFile();

        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Library> libraries = config.getLibraries();
        for (Library lib : libraries) {
            if (LIB_ID.equals(lib.getId())) {
                com.ibm.websphere.simplicity.config.File nativeEntry =
                        new com.ibm.websphere.simplicity.config.File();
                nativeEntry.setName("nativeAccess/" + System.mapLibraryName("accessNative1"));
                lib.getFiles().add(nativeEntry);
                break;
            }
        }
        server.updateServerConfiguration(config);
        server.startServer();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer("CWWKL0085W");
    }

    @Test
    public void testWarningAppearsWhenNativeAccessNotEnabled() throws Exception {
        // No --enable-native-access=ALL-UNNAMED was set (server.xml / jvm.options
        // are silent). On Java 24+ the warning must appear in messages.log.
        String found = server.waitForStringInLog("CWWKL0085W.*" + LIB_ID);
        assertNotNull("Expected CWWKL0085W warning naming library '" + LIB_ID
                      + "' but it was not found in messages.log", found);
    }
}
