/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static componenttest.annotation.SkipIfSysProp.OS_ZOS;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Versionless Feature minimal Packaging Tests
 *
 * This bucket includes the versionless features <platform> or PREFERRED_PLATFORM_VERSIONS
 * environment variable with several of the normal, minify, and runnable --include options of the
 * package command.
 *
 */
@RunWith(FATRunner.class)
public class PackageVersionlessFeatures {

    private static final String versionlessServerName = "com.ibm.ws.kernel.versionless.fat";
    private static final String archiveNameZip = "VersionlessPackage.zip";
    private static final String archiveNameJar = "VersionlessPackage.jar";
    private static final String SELF_EXTRACT_RESOURCE_NAME = "wlp/lib/extract/SelfExtractRun.class";
    private static final String PPV_KEY = "PREFERRED_PLATFORM_VERSIONS";
    private static final String PPV_VALUE = "jakartaee-10.0, microProfile-4.0";
    private static final String[] ARCHIVE_NAMES = { archiveNameJar,
                                                    archiveNameZip
    };

    private LibertyServer versionlessFatServer;
    private String versionlessFatServerPath;

    @Before
    public void before() throws Exception {
        versionlessFatServer = LibertyServerFactory.getLibertyServer(versionlessServerName);
        versionlessFatServerPath = versionlessFatServer.getServerRoot();
        System.out.println("Versionless FAT server: " + versionlessServerName);
        System.out.println("Versionless FAT server path: " + versionlessFatServerPath);

        // Clean up old archive files if they exist
        for (String archiveName : ARCHIVE_NAMES) {
            delete(versionlessFatServerPath, archiveName);
        }
    }

    /**
     * Packages the server which has <platform> in the server.xml with the archive parameter
     */
    @Test
    public void testPackagePlatformFromServerXML() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.setServerConfigurationFile("versionlessServer.xml");

        ensureProductExt(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip };
        verifyPackage(server, packageCmd, packageName, packagePath);
    }

    /**
     * Packages the server which has the PREFERRED_PLATFORM_VERSIONS env variable with the archive parameter
     *
     * @throws Exception
     */
    @Test
    public void testPackagePlatformFromEnvVariable() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.setServerConfigurationFile("normalServer.xml");

        ensureProductExt(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, PPV_VALUE);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip };
        verifyPackage(server, packageCmd, packageName, packagePath);

    }

    /**
     * Packages the server which has <platform> in the server.xml with the minify parameter
     *
     * @throws Exception
     */
    @Test
    public void testPackagePlatformFromServerXMLMinify() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.setServerConfigurationFile("versionlessServer.xml");

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
    }

    /**
     * Packages the server which has the PREFERRED_PLATFORM_VERSIONS env variable with the minify parameter
     *
     * @throws Exception
     */
    @Test
    public void testPackagePlatformFromEnvVariableMinify() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.setServerConfigurationFile("normalServer.xml");

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, PPV_VALUE);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);

    }

    /**
     * Packages the server which has <platform> in the server.xml with the minify parameter
     *
     * @throws Exception
     */
    @Test
    @SkipIfSysProp(OS_ZOS) // Jar not supported on Z/OS
    public void testPackagePlatformFromServerXMLRunnable() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.setServerConfigurationFile("versionlessServer.xml");

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameJar;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameJar, "--include=runnable" };
        verifyPackage(server, packageCmd, packageName, packagePath);
    }

    /**
     * Packages the server which has the PREFERRED_PLATFORM_VERSIONS env variable with the runnable parameter
     *
     * @throws Exception
     */
    @Test
    @SkipIfSysProp(OS_ZOS) // Jar not supported on Z/OS
    public void testPackagePlatformFromEnvVariableRunnable() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.setServerConfigurationFile("normalServer.xml");

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, PPV_VALUE);

        String packageName = archiveNameJar;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameJar, "--include=runnable" };
        verifyPackage(server, packageCmd, packageName, packagePath);

    }

    /**
     * Ensures we have the product extensions installed
     *
     * @param server
     * @throws Exception
     */
    private void ensureProductExt(LibertyServer server) throws Exception {
        String prodExtPath = server.getInstallRoot() + "/etc/extension/";
        File prodExt = new File(prodExtPath);

        if (!prodExt.exists()) {
            prodExt.mkdirs();
            if (!prodExt.exists()) {
                throw new FileNotFoundException(prodExtPath);
            }
        }

        if (!prodExt.isDirectory()) {
            throw new IOException("Product extension location is not a directory [ " + prodExtPath + " ]");
        }
    }

    /**
     * Verifies the server package is valid.
     *
     * @param server
     * @param packageCmd
     * @param packageName
     * @param packagePath
     * @throws Exception
     */
    private void verifyPackage(
                               LibertyServer server,
                               String[] packageCmd, String packageName, String packagePath) throws Exception {

        System.out.println("Packaging server [ " + server.getInstallRoot() + " ]");
        System.out.println("Package [ " + packagePath + " ]");

        String stdout = packageServer(server, packageCmd);

        System.out.println("Server package output [" + stdout + "]");

        if (!stdout.contains("package complete")) {
            fail("Packaging did not complete. STDOUT = " + stdout);
        } else {
            System.out.println("Packaging completed; found [ package complete ]");
        }
        if (!stdout.contains(packageName)) {
            fail("Packaging did not show archive [ " + packageName + " ].  STDOUT = " + stdout);
        } else {
            System.out.println("Packaging displays archive [ " + packageName + " ]");
        }
        if (!(new File(packagePath)).exists()) {
            fail("Package [ " + packagePath + " ] does not exist.  STDOUT = " + stdout);
        } else {
            System.out.println("Package file was created [ " + packagePath + " ]");
        }
    }

    /**
     * Performs the physical server packaging.
     *
     * @param server
     * @param packageCmd
     * @return
     * @throws Exception
     */
    private String packageServer(LibertyServer server, String[] packageCmd) throws Exception {
        return server.executeServerScript("package", packageCmd).getStdout();
    }

    /**
     * Deletes files on the file system.
     *
     * @param rootPath
     * @param fileName
     */
    private void delete(String rootPath, String fileName) {
        (new File(rootPath + '/' + fileName)).delete();
    }

    /**
     * Ensures we have the /lib/extract folder
     *
     * @param server
     */
    public static void assumeSelfExtractExists(LibertyServer server) {
        File installRoot = new File(server.getInstallRoot());
        File selfExtractClass = new File(installRoot, SELF_EXTRACT_RESOURCE_NAME);
        assumeTrue(selfExtractClass.exists());
    }
}
