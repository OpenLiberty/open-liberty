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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Versionless Feature "minimal" Packaging Tests
 *
 * This bucket includes the versionless features <platform> or PREFERRED_PLATFORM_VERSIONS
 * environment variable with several of the normal, minify, and runnable --include options of the
 * package command.
 *
 * TODO: At a later point support for .pax and .tar.gz files should be added.
 *
 */
@RunWith(FATRunner.class)
public class PackageVersionlessFeatures {

    private static final String versionlessServerName = "com.ibm.ws.kernel.versionless.fat";
    private static final String archiveNameZip = "VersionlessPackage.zip";
    private static final String archiveNameJar = "VersionlessPackage.jar";
    private static final String SELF_EXTRACT_CLASS_NAME = "wlp.lib.extract.SelfExtractRun";
    private static final String SELF_EXTRACT_RESOURCE_NAME = "wlp/lib/extract/SelfExtractRun.class";
    private static final String PPV_KEY = "PREFERRED_PLATFORM_VERSIONS";
    private static final String PPV_VALUE = "jakartaee-10.0, microProfile-4.0";
    private static final String[] ARCHIVE_NAMES = { archiveNameJar,
                                                    archiveNameZip
    };

    private LibertyServer versionlessFatServer;
    private String versionlessFatServerPath;

    // Features to be tested
    private static final String[] JAKARTAEE_FEATURES = { "jaxrs", "servlet", "jdbc" };
    private static final String[] MICROPROFILE_FEATURES = { "mpHealth", "mpMetrics", "mpConfig" };

    // Java Platforms
    private static final String[] JAVA_PLATFORM_EE6 = { "javaee-6.0" };
    private static final String[] JAVA_PLATFORM_EE7 = { "javaee-7.0" };
    private static final String[] JAVA_PLATFORM_EE8 = { "javaee-8.0" };
    private static final String[] JAKARTA_PLATFORM_EE91 = { "jakartaee-9.1" };
    private static final String[] JAKARTA_PLATFORM_EE10 = { "jakartaee-10.0" };

    // MicroProfile Platforms
    private static final String[] MICROPROFILE_PLATFORM_14 = { "microProfile-1.4" };
    private static final String[] MICROPROFILE_PLATFORM_20 = { "microProfile-2.0" };
    private static final String[] MICROPROFILE_PLATFORM_30 = { "microProfile-3.0" };
    private static final String[] MICROPROFILE_PLATFORM_40 = { "microProfile-4.0" };
    private static final String[] MICROPROFILE_PLATFORM_50 = { "microProfile-5.0" };
    private static final String[] MICROPROFILE_PLATFORM_61 = { "microProfile-6.1" };

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

    //
    //
    // Server package tests utilizing both the server.xml and the environment variable (PPV) for
    // different --include options.  These are only looking for failures in creating the respective
    // archive file or errors/warnings from the package command.
    //
    //

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
        verifyZipPackageFolders(packagePath);
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
        verifyZipPackageFolders(packagePath);
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
        verifyZipPackageFolders(packagePath);
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
        verifyZipPackageFolders(packagePath);
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
        verifyJarPackage(packagePath);
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
        verifyJarPackage(packagePath);
    }

    //
    //
    // Server package tests checking the content of the /lib/features folder for the respective
    // feature.mf files included for the package
    //
    //

    /**
     * Packages the server and verifies content with <platform> set to javaee-6.0
     */
    @Test
    public void verifyPackageContentJavaEE6_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(JAVA_PLATFORM_EE6), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to javaee-6.0
     */
    @Test
    public void verifyPackageContentJavaEE6_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, JAVA_PLATFORM_EE6.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to javaee-7.0
     */
    @Test
    public void verifyPackageContentJavaEE7_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(JAVA_PLATFORM_EE7), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to javaee-7.0
     */
    @Test
    public void verifyPackageContentJavaEE7_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, JAVA_PLATFORM_EE7.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to javaee-8.0
     */
    @Test
    public void verifyPackageContentJavaEE8_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(JAVA_PLATFORM_EE8), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to javaee-8.0
     */
    @Test
    public void verifyPackageContentJavaEE8_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, JAVA_PLATFORM_EE8.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to jakartaee-9.1
     */
    @Test
    public void verifyPackageContentJakartaee9_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(JAKARTA_PLATFORM_EE91), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to jakartaee-9.1
     */
    @Test
    public void verifyPackageContentJakartaee9_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, JAKARTA_PLATFORM_EE91.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to jakartaee-10.0
     */
    @Test
    public void verifyPackageContentJakartaee10_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(JAKARTA_PLATFORM_EE10), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to jakartaee-10.0
     */
    @Test
    public void verifyPackageContentJakartaee10_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(JAKARTAEE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, JAKARTA_PLATFORM_EE10.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, JAKARTAEE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to microprofile-1.4
     */
    @Test
    public void verifyPackageContentMicroProfile14_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(MICROPROFILE_PLATFORM_14), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to microprofile-1.4
     */
    @Test
    public void verifyPackageContentMicroProfile14_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, MICROPROFILE_PLATFORM_14.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to microprofile-2.0
     */
    @Test
    public void verifyPackageContentMicroProfile20_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(MICROPROFILE_PLATFORM_20), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to microprofile-2.0
     */
    @Test
    public void verifyPackageContentMicroProfile20_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, MICROPROFILE_PLATFORM_20.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to microprofile-3.0
     */
    @Test
    public void verifyPackageContentMicroProfile30_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(MICROPROFILE_PLATFORM_30), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to microprofile-3.0
     */
    @Test
    public void verifyPackageContentMicroProfile30_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, MICROPROFILE_PLATFORM_30.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to microprofile-4.0
     */
    @Test
    public void verifyPackageContentMicroProfile40_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(MICROPROFILE_PLATFORM_40), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to microprofile-4.0
     */
    @Test
    public void verifyPackageContentMicroProfile40_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, MICROPROFILE_PLATFORM_40.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to microprofile-5.0
     */
    @Test
    public void verifyPackageContentMicroProfile50_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(MICROPROFILE_PLATFORM_50), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to microprofile-5.0
     */
    @Test
    public void verifyPackageContentMicroProfile50_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, MICROPROFILE_PLATFORM_50.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with <platform> set to microprofile-6.1
     */
    @Test
    public void verifyPackageContentMicroProfile61_xml() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Arrays.asList(MICROPROFILE_PLATFORM_61), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    /**
     * Packages the server and verifies content with PPV env variable set to microprofile-6.1
     */
    @Test
    public void verifyPackageContentMicroProfile61_ppv() throws Exception {
        LibertyServer server = versionlessFatServer;
        server.changePlatformsAndFeatures(Collections.<String> emptyList(), Arrays.asList(MICROPROFILE_FEATURES));

        ensureProductExt(server);
        assumeSelfExtractExists(server);

        // Add PPV Environment variable to server.env
        server.addEnvVar(PPV_KEY, MICROPROFILE_PLATFORM_61.toString());

        String packageName = archiveNameZip;
        String packagePath = versionlessFatServerPath + File.separator + packageName;
        String[] packageCmd = { "--archive=" + archiveNameZip, "--include=minify" };
        verifyPackage(server, packageCmd, packageName, packagePath);
        verifyZipPackageFolders(packagePath);
        verifyPackageContent(packagePath, MICROPROFILE_FEATURES);
    }

    //
    //
    // Utility methods to deal with packaging and package verification.
    //
    //

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
     * Verifies the server package is valid and completes without errors or warnings
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
     * Performs the physical server packaging
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
     * Deletes files on the file system
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

    /**
     * Verifies the required highlevel folders for a server package .zip image
     *
     * @param packagePath
     * @throws Exception
     */
    private static void verifyZipPackageFolders(String packagePath) throws Exception {

        try (ZipFile zipFile = new ZipFile(packagePath)) {
            boolean foundDefaultRootEntry = false;
            boolean foundUsrEntry = false;
            boolean foundBinEntry = false;
            boolean foundLibEntry = false;
            boolean foundDevEntry = false;
            boolean foundAll = false;
            boolean foundLibFeaturesEntry = false;

            Enumeration<? extends ZipEntry> en = zipFile.entries();
            while (!foundAll && en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String entryName = entry.getName();

                if (!foundDefaultRootEntry) {
                    foundDefaultRootEntry = entryName.startsWith("wlp/");
                }
                if (!foundUsrEntry) {
                    foundUsrEntry = entryName.contains("/usr/");
                }
                if (!foundBinEntry) {
                    foundBinEntry = entryName.contains("/bin/");
                }
                if (!foundLibEntry) {
                    foundLibEntry = entryName.contains("/lib/");
                }
                if (!foundLibFeaturesEntry) {
                    foundLibFeaturesEntry = entryName.contains("/lib/features/");
                }
                if (!foundDevEntry) {
                    foundDevEntry = entryName.contains("/dev/");
                }

                foundAll = (foundDefaultRootEntry && foundUsrEntry && foundBinEntry && foundLibEntry && foundDevEntry && foundLibFeaturesEntry);
            }

            if (!foundDefaultRootEntry) {
                System.out.println("Package [ " + packagePath + " ] is missing default root entry [ /wlp ]");
            }
            if (!foundUsrEntry) {
                System.out.println("Package [ " + packagePath + " ] is missing user entry [ /usr ]");
            }
            if (!foundBinEntry) {
                System.out.println("Package [ " + packagePath + " ] is missing bin entry [ /bin ]");
            }
            if (!foundLibEntry) {
                System.out.println("Package [ " + packagePath + " ] is missing lib entry [ /lib ]");
            }
            if (!foundDevEntry) {
                System.out.println("Package [ " + packagePath + " ] is missing dev entry [ /dev ]");
            }
            if (!foundLibFeaturesEntry) {
                System.out.println("Package [ " + packagePath + " ] is missing dev entry [ /lib/features]");
            }

            if (!foundDefaultRootEntry) {
                fail("Package [ " + packagePath + " ] did not contain /wlp/.");
            }
            if (!foundUsrEntry) {
                fail("Package [ " + packagePath + " ] did not contain /usr/.");
            }
            if (!foundBinEntry) {
                fail("Package [ " + packagePath + " ] did not contain /bin/.");
            }
            if (!foundLibEntry) {
                fail("Package [ " + packagePath + " ] did not contain /lib/.");
            }
            if (!foundDevEntry) {
                fail("Package [ " + packagePath + " ] did not contain /dev/.");
            }
            if (!foundLibFeaturesEntry) {
                fail("Package [ " + packagePath + " ] did not contain /lib/features/.");
            }
        }
    }

    /**
     * Verifies the required individual versionless feature manifests for a server package .zip image
     *
     * @param packagePath
     * @param features
     * @throws Exception
     */
    private static void verifyPackageContent(String packagePath, String[] features) throws Exception {

        try (ZipFile zipFile = new ZipFile(packagePath)) {
            boolean foundAll = false;
            boolean foundLibFeaturesEntry = false;
            boolean foundFeature1 = false;
            boolean foundFeature2 = false;
            boolean foundFeature3 = false;

            Enumeration<? extends ZipEntry> en = zipFile.entries();
            while (!foundAll && en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String entryName = entry.getName();

                if (!foundLibFeaturesEntry) {
                    foundLibFeaturesEntry = entryName.contains("/lib/features/");
                }
                if (!foundFeature1) {
                    foundFeature1 = entryName.contains("io.openliberty.versionless." + features[0] + ".mf");
                }
                if (!foundFeature2) {
                    foundFeature2 = entryName.contains("io.openliberty.versionless." + features[1] + ".mf");
                }
                if (!foundFeature3) {
                    foundFeature3 = entryName.contains("io.openliberty.versionless." + features[2] + ".mf");
                }

                foundAll = (foundLibFeaturesEntry && foundFeature1 && foundFeature2 && foundFeature3);
            }

            if (!foundLibFeaturesEntry) {
                System.out.println("Package [ " + packagePath + " ] is missing dev entry [ /lib/features]");
                fail("Package [ " + packagePath + " ] did not contain /lib/features/.");
            }
            if (!foundFeature1) {
                System.out.println("Package [ " + packagePath + " ] is missing entry [" + features[0] + "]");
                fail("Package [ " + packagePath + " ] did not contain " + features[0]);
            }
            if (!foundFeature2) {
                System.out.println("Package [ " + packagePath + " ] is missing entry [" + features[1] + "]");
                fail("Package [ " + packagePath + " ] did not contain " + features[1]);
            }
            if (!foundFeature3) {
                System.out.println("Package [ " + packagePath + " ] is missing entry [" + features[2] + "]");
                fail("Package [ " + packagePath + " ] did not contain " + features[2]);
            }
        }
    }

    /**
     * Verifies the required manifest headers and the self-extract / server entries are contained in a .jar image
     *
     * @param packagePath
     * @throws Exception
     */
    private static void verifyJarPackage(String packagePath) throws Exception {
        try (JarFile jarFile = new JarFile(packagePath)) {
            Manifest mf = jarFile.getManifest();
            assertNotNull("Package [ " + packagePath + " ] is missing its manifest", mf);

            String mainClass = SELF_EXTRACT_CLASS_NAME;
            assertEquals("Package [ " + packagePath + " ] has incorrect main class",
                         mainClass, mf.getMainAttributes().getValue("Main-Class"));

            boolean foundSelfExtractRun = (jarFile.getEntry(SELF_EXTRACT_RESOURCE_NAME) != null);
            if (!foundSelfExtractRun) {
                fail("Package [ " + packagePath + " ] missing self-extract class [ " + SELF_EXTRACT_RESOURCE_NAME + " ]");
            }
        }
    }
}
