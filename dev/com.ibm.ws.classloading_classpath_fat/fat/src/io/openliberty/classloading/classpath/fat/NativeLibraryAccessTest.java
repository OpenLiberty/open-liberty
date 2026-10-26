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
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Verifies CWWKL0085W behaviour when a shared library is configured with
 * native files:
 * <ul>
 *   <li>On Java 24+ without the JVM flag: warning must appear.</li>
 *   <li>On Java older than 24: warning must NOT appear (no restriction).</li>
 *   <li>On Java 24+ with --enable-native-access=ALL-UNNAMED: warning must NOT appear.</li>
 * </ul>
 */
@RunWith(FATRunner.class)
public class NativeLibraryAccessTest {

    @Server(NATIVE_LIBRARY_ACCESS_TEST_SERVER)
    public static LibertyServer server;

    private static final String LIB_ID = "nativeAccessLib";

    @BeforeClass
    public static void setupTestServer() throws Exception {
        ShrinkHelper.exportAppToServer(server, TEST_NATIVE_LIBRARY_WAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/nativeAccess", TEST_LIB1_JAR, DeployOptions.SERVER_ONLY);

        // Create a zero-byte placeholder native file. Its content is irrelevant —
        // the warning fires at registration time based on the file extension alone.
        String nativeFileName = System.mapLibraryName("accessNative1");
        File nativeDir = new File(server.getServerRoot(), "nativeAccess");
        nativeDir.mkdirs();
        new File(nativeDir, nativeFileName).createNewFile();

        // Add the native file entry to the library config programmatically,
        // before the server starts, so the library is registered with the file.
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Library> libraries = config.getLibraries();
        for (Library lib : libraries) {
            if (LIB_ID.equals(lib.getId())) {
                com.ibm.websphere.simplicity.config.File nativeEntry =
                        new com.ibm.websphere.simplicity.config.File();
                nativeEntry.setName("nativeAccess/" + nativeFileName);
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

    /**
     * On Java 24+, without --enable-native-access=ALL-UNNAMED, CWWKL0085W must
     * appear naming both the library id and the detected native file name.
     */
    @Test
    @MinimumJavaLevel(javaLevel = 24)
    public void testWarningAppearsWhenNativeAccessNotEnabled() throws Exception {
        String nativeFileName = System.mapLibraryName("accessNative1");
        String found = server.waitForStringInLog("CWWKL0085W.*" + LIB_ID + ".*" + nativeFileName);
        assertNotNull("Expected CWWKL0085W warning naming library '" + LIB_ID
                      + "' and file '" + nativeFileName + "' but it was not found in messages.log", found);
    }

    /**
     * On Java older than 24, there is no native access restriction so CWWKL0085W
     * must NOT appear regardless of JVM options.
     */
    @Test
    @MaximumJavaLevel(javaLevel = 23)
    public void testNoWarningOnOlderJava() throws Exception {
        String found = server.waitForStringInLog("CWWKL0085W", 3000);
        assertNull("CWWKL0085W must NOT appear on Java older than 24, but was found: " + found, found);
    }

    /**
     * On Java 24+, when --enable-native-access=ALL-UNNAMED is set in jvm.options,
     * CWWKL0085W must NOT appear.
     */
    @Test
    @MinimumJavaLevel(javaLevel = 24)
    public void testNoWarningWhenNativeAccessEnabled() throws Exception {
        // Restart the server with the native access flag set.
        server.stopServer("CWWKL0085W");
        server.setJvmOptions(Arrays.asList("--enable-native-access=ALL-UNNAMED"));
        server.startServer();

        // Reset the mark to the beginning of this fresh server run so we only
        // search messages written after the restart, not the previous startup.
        server.resetLogMarks();

        String found = server.waitForStringInLog("CWWKL0085W", 3000);
        assertNull("CWWKL0085W must NOT appear when --enable-native-access=ALL-UNNAMED is set, "
                   + "but was found: " + found, found);

        // Clear jvm.options so the flag does not persist to subsequent test runs.
        server.stopServer();
        server.setJvmOptions(Collections.emptyList());
        server.startServer();
    }
}
