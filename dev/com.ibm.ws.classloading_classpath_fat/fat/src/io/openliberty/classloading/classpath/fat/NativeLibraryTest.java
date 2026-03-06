/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.NATIVE_LIBRARY_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB1_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB2_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB3_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB4_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB5_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB6_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_NATIVE_LIBRARY_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_NATIVE_LIBRARY_WAR;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.function.BiConsumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.Path;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.nativelib.test.app.NativeLibraryTestServlet;

/**
 *
 */
@RunWith(FATRunner.class)
public class NativeLibraryTest extends FATServletClient {

    @Server(NATIVE_LIBRARY_TEST_SERVER)
    @TestServlet(servlet = NativeLibraryTestServlet.class, contextRoot = TEST_NATIVE_LIBRARY_APP)
    public static LibertyServer server;

    @BeforeClass
    public static void setupTestServer() throws Exception {
        ShrinkHelper.exportAppToServer(server, TEST_NATIVE_LIBRARY_WAR, DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportToServer(server, "/lib1", TEST_LIB1_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/lib2", TEST_LIB2_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/lib3", TEST_LIB3_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/lib4", TEST_LIB4_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/lib5", TEST_LIB5_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/lib6", TEST_LIB6_JAR, DeployOptions.SERVER_ONLY);

        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        ConfigElementList<Library> libraries = serverConfiguration.getLibraries();
        File serverRoot = new File(server.getServerRoot());

        BiConsumer<String, Library> paths = (n, l) -> {
            Path nativePath = new Path();
            nativePath.setName(n);
            l.getPaths().add(nativePath);
        };
        configureNativeFile(libraries, serverRoot, "lib1", "testPrivateLib1", "privateNative1", paths);
        configureNativeFile(libraries, serverRoot, "lib2", "testCommonLib2", "commonNative2", paths);

        BiConsumer<String, Library> files = (n, l) -> {
            com.ibm.websphere.simplicity.config.File nativeFile = new com.ibm.websphere.simplicity.config.File();
            nativeFile.setName(n);
            l.getFiles().add(nativeFile);
        };
        configureNativeFile(libraries, serverRoot, "lib3", "testPrivateLib3", "privateNative3", files);
        configureNativeFile(libraries, serverRoot, "lib4", "testCommonLib4", "commonNative4", files);

        configureNativeFile(libraries, serverRoot, "lib5", "testPrivateLib5", "privateNative5", null);
        configureNativeFile(libraries, serverRoot, "lib6", "testCommonLib6", "commonNative6", null);

        server.updateServerConfiguration(serverConfiguration);
        server.startServer();
    }

    private static void configureNativeFile(ConfigElementList<Library> libraries, File serverRoot, String libDirName, String libId, String nativeName, BiConsumer<String, Library> addNative) throws Exception {
        File libDir = new File(serverRoot, libDirName);
        String nativeFileName = System.mapLibraryName(nativeName);
        new File(libDir, nativeFileName).createNewFile();
        if (addNative != null) {
            Library library = null;
            for (Library l : libraries) {
                if (libId.equals(l.getId())) {
                    library = l;
                    break;
                }
            }
            if (library == null) {
                fail("Could not find library: " + libId);
            }
            addNative.accept(libDirName + '/' + nativeFileName, library);
        }
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer();
    }
}
