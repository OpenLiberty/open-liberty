/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * 
 */
public class PackageCommandTest {

    /**
     * The package command requires that the lib/extract directory exists, as this directory
     * contains a required manifest, self extractable classes, etc. If this directory does
     * not exist, then the command should print this message:
     * <br/>
     * <em>CWWKE0922E: The package command cannot complete because the installation is missing the lib/extract directory.</em>
     * <br/>
     * This test verifies that this error message is printed.
     * <br/>
     * <b>Note:</b> This test assumes that the lib/extract directory is already missing. As of today, the
     * FAT environment's installation of WLP does not include this directory. If we end up creating that
     * directory in the FAT environment, then we'll need to find another way to run this test - as we
     * probably don't want to delete a directory in the FAT environment's installation.
     * 
     * @throws Exception
     */
    @Test
    public void testCorrectErrorMessageWhenLibExtractDirIsMissing() throws Exception {
        // Pick any server, doesn't matter.
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.bootstrap.fat");
        // Only run the test if the lib/extract directory does not exist
        try {
            server.getFileFromLibertyInstallRoot("lib/extract");
            assumeTrue(false); // the directory exists, so we skip this test.
        } catch (FileNotFoundException ex) {
            //expected - the directory does not exist - so proceed.
        }

        String stdout = server.executeServerScript("package", new String[] { "--include=minify" }).getStdout();

        assertTrue("Did not find expected failure message, CWWKE0922E", stdout.contains("CWWKE0922E"));
    }

    /**
     * Packages --include=minify,runnable jar and verifies correct content.
     * This test is not run if:
     * 1) platform is z/OS (jar archive not supported)
     * 2) wlp/lib/extract directory does not exist
     * 
     * @throws Exception
     */
    @Test
    public void testCreateRunnableJar() throws Exception {
        // Pick any server, doesn't matter.
        String serverName = "com.ibm.ws.kernel.bootstrap.fat";
        String jarFileName = serverName + ".jar";
        String mainClass = "wlp.lib.extract.SelfExtractRun";

        // Doesn't work on z/OS (because you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

        // Only run the test if the lib/extract directory exists
        try {
            server.getFileFromLibertyInstallRoot("lib/extract");

            String stdout = server.executeServerScript("package", new String[] { "--archive=" + jarFileName, "--include=runnable" }).getStdout();

            assertTrue("Could not package server " + serverName, stdout.contains("Server " + serverName + " package complete"));

            JarFile jarFile = new JarFile(server.getFileFromLibertyServerRoot(jarFileName).getAbsolutePath());

            // Check the manifest for headers that should be in there.
            Manifest mf = jarFile.getManifest();
            assertNotNull("There should be a manifest in the jar file", mf);

            // make sure it'a a runnable jar 
            assertEquals(mainClass, mf.getMainAttributes().getValue("Main-Class"));

            // Check that the self-extract and the server's entries are in the jar
            Enumeration<JarEntry> entries = jarFile.entries();
            assertTrue(entries.hasMoreElements());

            boolean foundSelfExtractRun = false;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                foundSelfExtractRun |= entry.getName().startsWith("wlp/lib/extract/SelfExtractRun.class");
            }

            assertTrue(foundSelfExtractRun);

        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * Make sure that when packaging a jar archive using --include=usr,
     * the resulting jar files does NOT contain the self-extract files.
     */
    @Test
    public void testPackageJarArchiveWithIncludeEqualsUsr() throws Exception {

        // NOTE: This test won't work cuz the necessary MANIFEST files and
        // self-extract files (wlp/lib/extract/*) aren't present in our test environment.
        assumeTrue(false);

        // Doesn't work on z/OS (cuz you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        // Pick any server, doesn't matter.
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.bootstrap.fat");

        String jarFileName = "package.usr.jar";

        String stdout = server.executeServerScript("package",
                                                   new String[] { "--archive=" + jarFileName,
                                                                 "--include=usr" }).getStdout();

        JarFile jarFile = new JarFile(server.getFileFromLibertyServerRoot(jarFileName).getAbsolutePath());

        // Check the manifest for headers that should be in there.
        Manifest mf = jarFile.getManifest();
        assertNotNull("There should be a manifest in the jar file", mf);

        assertTrue(mf.getMainAttributes().containsKey("Applies-To"));
        assertEquals("com.ibm.websphere.appserver", mf.getMainAttributes().getValue("Applies-To"));
        assertEquals("false", mf.getMainAttributes().getValue("Extract-Installer"));

        // Check that the self-extract and the server's entries are in the jar
        Enumeration<JarEntry> entries = jarFile.entries();
        assertTrue(entries.hasMoreElements());

        boolean foundServerEntry = false;
        boolean foundSelfExtractEntry = false;
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            foundServerEntry |= entry.getName().startsWith("wlp/usr/servers/com.ibm.ws.kernel.bootstrap.fat");
            foundSelfExtractEntry |= entry.getName().startsWith("wlp/lib/extract");
        }

        assertTrue(foundServerEntry);
        assertTrue(foundSelfExtractEntry);
    }

}
