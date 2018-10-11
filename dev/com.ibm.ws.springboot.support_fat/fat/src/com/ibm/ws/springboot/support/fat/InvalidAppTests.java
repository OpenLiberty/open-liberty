/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(FULL)
public class InvalidAppTests extends CommonWebServerTests {
    private final static String PROPERTY_KEY_INSTALL_DIR = "install.dir";

    private final static String SPRING_BOOT_NO_MANIFEST = "noManifest";
    private final static String SPRING_BOOT_NO_START_CLASS = "noStartClass";

    private final static String TEST_NO_MANIFEST = "testNoManifest";
    private final static String TEST_NO_START_CLASS = "testNoStartCLass";

    private static String installDir = null;
    private static RemoteFile appsDir;

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-1.5"));
    }

    @Override
    public boolean expectApplicationSuccess() {
        return false;
    }

    @Override
    public String getApplication() {
        String methodName = testName.getMethodName();
        if (TEST_NO_MANIFEST.equals(methodName)) {
            return SPRING_BOOT_NO_MANIFEST;
        } else if (TEST_NO_START_CLASS.equals(methodName)) {
            return SPRING_BOOT_NO_START_CLASS;
        }
        Assert.fail("Unknown test.");
        return null;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @AfterClass
    public static void resetInstallDir() {
        if (installDir == null) {
            System.clearProperty(PROPERTY_KEY_INSTALL_DIR);
        } else {
            System.setProperty(PROPERTY_KEY_INSTALL_DIR, installDir);
        }
        installDir = null;
    }

    @BeforeClass
    public static void createInvalidApp() throws Exception {
        installDir = System.setProperty(PROPERTY_KEY_INSTALL_DIR, server.getInstallRoot());

        appsDir = server.getFileFromLibertyServerRoot("apps");

        RemoteFile noManifest = new RemoteFile(appsDir, SPRING_BOOT_NO_MANIFEST);
        noManifest.mkdirs();
        RemoteFile noStartClass = new RemoteFile(appsDir, SPRING_BOOT_NO_START_CLASS);
        noStartClass.mkdirs();
        RemoteFile noStartClassManifest = new RemoteFile(noStartClass, "META-INF");
        noStartClassManifest.mkdirs();
        noStartClassManifest = new RemoteFile(noStartClassManifest, "MANIFEST.MF");
        try (PrintWriter pw = new PrintWriter(noStartClassManifest.getAbsolutePath())) {
            pw.println("Manifest-Version: 1.0");
            pw.println();
            pw.println();
        }
    }

    @AfterClass
    public static void deleteAppsAndStopServer() throws Exception {
        server.deleteDirectoryFromLibertyServerRoot("apps/" + SPRING_BOOT_NO_MANIFEST);
        server.deleteDirectoryFromLibertyServerRoot("apps/" + SPRING_BOOT_NO_START_CLASS);
    }

    @After
    public void stopTestServer() throws Exception {
        if (!javaVersion.startsWith("1.")) {
            super.stopServer(true, "CWWKZ0002E", "CWWKC0265W");
        } else {
            super.stopServer(true, "CWWKZ0002E");
        }
    }

    @Override
    public String getLogMethodName() {
        return "-" + testName.getMethodName();
    }

    @Test
    public void testNoManifest() throws Exception {
        assertNotNull("No error message for missing manifest.", server.waitForStringInLog("CWWKC0256E"));
    }

    @Test
    public void testNoStartCLass() throws Exception {
        assertNotNull("No error message for missing start class.", server.waitForStringInLog("CWWKC0257E"));
    }
}
