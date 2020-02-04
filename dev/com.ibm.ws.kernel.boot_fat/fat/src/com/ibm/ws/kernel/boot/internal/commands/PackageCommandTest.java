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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class PackageCommandTest {
    private static final Class<?> c = PackageCommandTest.class;

    private static String serverName = "com.ibm.ws.kernel.boot.root.fat";
    private static String archivePackage = "MyPackage.zip";
    private static String archivePackageNoExtension = "MyPackage";
    private static String archivePackageTarGzExtension = "MyPackage.tar.gz";
    private static String archivePackageJarExtension = "MyPackage.jar";

    @Before
    public void before() throws Exception {

        // Delete previous archive file if it exists
        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
        server.deleteFileFromLibertyServerRoot(archivePackage);
    }

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

    @Test
    public void testCorrectErrorMessageWhenProductExtensionsInstalledAndServerRootSpecified() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

        try {
            server.getFileFromLibertyInstallRoot("lib/extract");

            // Make sure we have the /wlp/etc/extension directory which indicates Product Extensions are installed
            File prodExtensionDir = null;
            try {
                server.getFileFromLibertyInstallRoot("etc/extension/");
            } catch (FileNotFoundException ex) {
                // The /etc/extension directory does not exist - so create it for this test.
                String pathToProdExt = server.getInstallRoot() + "/etc" + "/extension/";
                prodExtensionDir = new File(pathToProdExt);
                prodExtensionDir.mkdirs();
            }

            String[] cmd = new String[] { "--archive=" + archivePackage,
                                          "--include=minify",
                                          "--server-root= " };
            String stdout = server.executeServerScript("package", cmd).getStdout();

            assertTrue("Did not find expected failure message, CWWKE0947W.  STDOUT = " + stdout, stdout.contains("CWWKE0947W"));

        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that --include=usr outputs the correct information in
     * the archive file
     *
     */
    @Test
    public void testMinifyInclude() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
        try {

            server.getFileFromLibertyInstallRoot("lib/extract");

            String[] cmd = new String[] { "--archive=" + archivePackage,
                                          "--include=minify" };
            // Ensure package completes
            String stdout = server.executeServerScript("package", cmd).getStdout();
            assertTrue("The package command did not complete as expected. STDOUT = " + stdout, stdout.contains("package complete"));

            // Ensure root is correct in the .zip
            ZipFile zipFile = new ZipFile(server.getServerRoot() + "/" + archivePackage);
            try {
                boolean foundDefaultRootEntry = false;
                boolean foundUsrEntry = false;
                boolean foundBinEntry = false;
                boolean foundLibEntry = false;
                boolean foundDevEntry = false;

                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    foundDefaultRootEntry |= entry.getName().startsWith("wlp");
                    foundUsrEntry |= entry.getName().contains("/usr");
                    foundBinEntry |= entry.getName().contains("/bin");
                    foundLibEntry |= entry.getName().contains("/lib");
                    foundDevEntry |= entry.getName().contains("/dev");
                }
                assertTrue("The package did not contain /wlp root structure as expected.", foundDefaultRootEntry);
                assertTrue("The package did not contain /usr/ as expected.", foundUsrEntry);
                assertTrue("The package did not contain /bin/ as expected.", foundBinEntry);
                assertTrue("The package did not contain /lib/ as expected.", foundLibEntry);
                assertTrue("The package did not contain /dev/ as expected.", foundDevEntry);

            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that --include=usr outputs the correct information in
     * the archive file
     *
     */
    @Test
    public void testUsrInclude() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
        try {

            server.getFileFromLibertyInstallRoot("lib/extract");

            String[] cmd = new String[] { "--archive=" + archivePackage,
                                          "--include=usr" };
            // Ensure package completes
            String stdout = server.executeServerScript("package", cmd).getStdout();
            assertTrue("The package command did not complete as expected. STDOUT = " + stdout, stdout.contains("package complete"));

            // Ensure root is correct in the .zip
            ZipFile zipFile = new ZipFile(server.getServerRoot() + "/" + archivePackage);
            try {
                boolean foundDefaultRootEntry = false;
                boolean foundUsrEntry = true;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    foundDefaultRootEntry |= entry.getName().startsWith("wlp");
                    foundUsrEntry |= entry.getName().contains("/usr/");
                }
                assertTrue("The package did not contain /wlp root structure as expected.", foundDefaultRootEntry);
                assertTrue("The package did not contain /usr/ as expected.", foundUsrEntry);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that when --server-root is supplied, that the value supplied
     * shows up as the root of the archive
     *
     */
    @Test
    public void testServerRootSpecified() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
        try {

            server.getFileFromLibertyInstallRoot("lib/extract");

            String[] cmd = new String[] { "--archive=" + archivePackage,
                                          "--include=minify",
                                          "--server-root=MyRoot" };
            // Ensure package completes
            String stdout = server.executeServerScript("package", cmd).getStdout();
            assertTrue("The package command did not complete as expected. STDOUT = " + stdout, stdout.contains("package complete"));

            // Ensure root is correct in the .zip
            ZipFile zipFile = new ZipFile(server.getServerRoot() + "/" + archivePackage);
            try {
                boolean foundMyRootEntry = false;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    foundMyRootEntry |= entry.getName().startsWith("MyRoot");
                }
                assertTrue("The package did not contain /MyRoot as expected.", foundMyRootEntry);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that when --server-root is supplied, and --include=usr that the
     * /shared folder is also placed at the root of the archive.
     *
     */
    @Test
    public void testSharedFolderWithServerRootandUsrSpecified() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
        try {

            server.getFileFromLibertyInstallRoot("lib/extract");

            String[] cmd = new String[] { "--archive=" + archivePackage,
                                          "--include=usr",
                                          "--server-root=MyRoot" };
            // Ensure package completes
            String stdout = server.executeServerScript("package", cmd).getStdout();
            assertTrue("The package command did not complete as expected. STDOUT = " + stdout, stdout.contains("package complete"));

            // Ensure root is correct in the .zip
            ZipFile zipFile = new ZipFile(server.getServerRoot() + "/" + archivePackage);
            try {
                boolean foundMyRootSharedEntry = false;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    foundMyRootSharedEntry |= entry.getName().contains("MyRoot/shared");
                }
                assertTrue("The package did not contain MyRoot/shared/ as expected.", foundMyRootSharedEntry);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that when --server-root is supplied and --include=minify that /usr
     * does show up in the archive file.
     */
    @Test
    public void testServerFoundWithServerRootSpecified() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
        try {

            server.getFileFromLibertyInstallRoot("lib/extract");

            String[] cmd = new String[] { "--archive=" + archivePackage,
                                          "--include=minify",
                                          "--server-root=MyRoot" };
            // Ensure package completes
            String stdout = server.executeServerScript("package", cmd).getStdout();
            assertTrue("The package command did not complete as expected. STDOUT = " + stdout, stdout.contains("package complete"));

            // Ensure root is correct in the .zip
            ZipFile zipFile = new ZipFile(server.getServerRoot() + "/" + archivePackage);
            try {
                boolean foundServerEntry = false;
                boolean foundWarFileEntry = false;
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
                    ZipEntry entry = en.nextElement();
                    // For Minify, there should be /usr in the structure with server-root option
                    foundServerEntry |= entry.getName().contains("MyRoot/usr/servers/com.ibm.ws.kernel.boot.root.fat");
                    foundWarFileEntry |= entry.getName().contains("MyRoot/usr/servers/com.ibm.ws.kernel.boot.root.fat/apps/AppsLooseWeb.war");
                }
                assertTrue("The package did not contain MyRoot/usr/servers/com.ibm.ws.kernel.boot.root.fat as expected.", foundServerEntry);
                assertTrue("The package did not contain MyRoot/usr/servers/com.ibm.ws.kernel.boot.root.fat/apps/AppsLooseWeb.war as expected.", foundWarFileEntry);
            } finally {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                }
            }
        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that when --include=runnable is supplied, and --archive is supplied with
     * a file extension not ending with .jar that an error is returned.
     *
     */
    @Test
    public void testCorrectErrorMessageWhenRunnableAndNonJARArchiveSpecified() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

        try {
            server.getFileFromLibertyInstallRoot("lib/extract");

            // Make sure we have the /wlp/etc/extension directory which indicates Product Extensions are installed
            File prodExtensionDir = null;
            try {
                server.getFileFromLibertyInstallRoot("etc/extension/");
            } catch (FileNotFoundException ex) {
                // The /etc/extension directory does not exist - so create it for this test.
                String pathToProdExt = server.getInstallRoot() + "/etc" + "/extension/";
                prodExtensionDir = new File(pathToProdExt);
                prodExtensionDir.mkdirs();
            }

            String[] cmd = new String[] { "--archive=" + archivePackage,
                                          "--include=runnable" };
            String stdout = server.executeServerScript("package", cmd).getStdout();

            assertTrue("Did not find expected failure message, CWWKE0950E.  STDOUT = " + stdout, stdout.contains("CWWKE0950E"));

        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that when the --archive value has no extension, and --include=runnable
     * that a .jar archive is created by default.
     */
    @Test
    public void testDefaultingToJar() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

        try {
            server.getFileFromLibertyInstallRoot("lib/extract");

            // Make sure we have the /wlp/etc/extension directory which indicates Product Extensions are installed
            File prodExtensionDir = null;
            try {
                server.getFileFromLibertyInstallRoot("etc/extension/");
            } catch (FileNotFoundException ex) {
                // The /etc/extension directory does not exist - so create it for this test.
                String pathToProdExt = server.getInstallRoot() + "/etc" + "/extension/";
                prodExtensionDir = new File(pathToProdExt);
                prodExtensionDir.mkdirs();
            }

            String[] cmd = new String[] { "--archive=" + archivePackageNoExtension,
                                          "--include=runnable" };
            String stdout = server.executeServerScript("package", cmd).getStdout();

            assertTrue("Did not find expected 'package complete' success message.  STDOUT = " + stdout, stdout.contains("package complete"));
            assertTrue("Did not find expected .jar archive.  STDOUT = " + stdout, stdout.contains(archivePackageNoExtension + ".jar"));

        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that when the --archive value has no extension, and --include != runnable
     * that a .zip archive is created by default.
     */
    @Test
    public void testDefaultingToZip() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

        try {
            server.getFileFromLibertyInstallRoot("lib/extract");

            // Make sure we have the /wlp/etc/extension directory which indicates Product Extensions are installed
            File prodExtensionDir = null;
            try {
                server.getFileFromLibertyInstallRoot("etc/extension/");
            } catch (FileNotFoundException ex) {
                // The /etc/extension directory does not exist - so create it for this test.
                String pathToProdExt = server.getInstallRoot() + "/etc" + "/extension/";
                prodExtensionDir = new File(pathToProdExt);
                prodExtensionDir.mkdirs();
            }

            String[] cmd = new String[] { "--archive=" + archivePackageNoExtension,
                                          "--include=usr" };
            String stdout = server.executeServerScript("package", cmd).getStdout();

            assertTrue("Did not find expected 'package complete' success message.  STDOUT = " + stdout, stdout.contains("package complete"));
            assertTrue("Did not find expected .zip archive.  STDOUT = " + stdout, stdout.contains(archivePackageNoExtension + ".zip"));

        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that a .tar.gz file type is created when specified by --archive.
     */
    @Test
    public void testTarGz() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

        try {
            server.getFileFromLibertyInstallRoot("lib/extract");

            // Make sure we have the /wlp/etc/extension directory which indicates Product Extensions are installed
            File prodExtensionDir = null;
            try {
                server.getFileFromLibertyInstallRoot("etc/extension/");
            } catch (FileNotFoundException ex) {
                // The /etc/extension directory does not exist - so create it for this test.
                String pathToProdExt = server.getInstallRoot() + "/etc" + "/extension/";
                prodExtensionDir = new File(pathToProdExt);
                prodExtensionDir.mkdirs();
            }

            String[] cmd = new String[] { "--archive=" + archivePackageTarGzExtension,
                                          "--include=usr" };
            String stdout = server.executeServerScript("package", cmd).getStdout();

            assertTrue("Did not find expected 'package complete' success message.  STDOUT = " + stdout, stdout.contains("package complete"));
            assertTrue("Did not find expected .tar.gz archive.  STDOUT = " + stdout, stdout.contains(archivePackageTarGzExtension));

        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * This tests that when --include=usr is supplied, and --archive is supplied with
     * a file extension ending with .jar that an error is returned.
     *
     */
    @Test
    public void testCorrectErrorMessageWhenUsrandJARArchiveSpecified() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

        try {
            server.getFileFromLibertyInstallRoot("lib/extract");

            // Make sure we have the /wlp/etc/extension directory which indicates Product Extensions are installed
            File prodExtensionDir = null;
            try {
                server.getFileFromLibertyInstallRoot("etc/extension/");
            } catch (FileNotFoundException ex) {
                // The /etc/extension directory does not exist - so create it for this test.
                String pathToProdExt = server.getInstallRoot() + "/etc" + "/extension/";
                prodExtensionDir = new File(pathToProdExt);
                prodExtensionDir.mkdirs();
            }

            String[] cmd = new String[] { "--archive=" + archivePackageJarExtension,
                                          "--include=usr" };
            String stdout = server.executeServerScript("package", cmd).getStdout();

            assertTrue("Did not find expected failure message, CWWKE0951E.  STDOUT = " + stdout, stdout.contains("CWWKE0951E"));

        } catch (FileNotFoundException ex) {
            assumeTrue(false); // the directory does not exist, so we skip this test.
        }
    }

    /**
     * Verify the embedded server instance launched by the package command does not corrupt
     * the feature cache of the packaged (target) server.
     */
    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testMinifyDoesNotCorruptServerFeatureCache() throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.bootstrap.fat");
        String jarFileName = server.getServerName() + ".jar";
        ServerConfiguration config = null;
        Set<String> features = null;
        try {
            config = server.getServerConfiguration();
            features = config.getFeatureManager().getFeatures();
            features.addAll(new HashSet<>(Arrays.asList("mpFaultTolerance-1.1", "mpMetrics-1.1", "jsp-2.3")));
            server.updateServerConfiguration(config);

            server.startServer(true); // --clean
            String installedFeaturesBefore = server.findStringsInLogs("CWWKF0012I:.*").get(0);
            System.out.println("installedFeaturesBefore: " + installedFeaturesBefore);
            server.stopServer();

            String stdout = server.executeServerScript("package", new String[] { "--archive=" + jarFileName, "--include=minify" }).getStdout();
            System.out.println("Server package command output: " + stdout);
            assertTrue("Server package command launched an embedded server that computed a feature set : ", stdout.contains("CWWKF0012I:"));

            server.startServer(); // Not --clean
            String installedFeaturesAfter = server.findStringsInLogs("CWWKF0012I:.*").get(0);
            System.out.println("installedFeaturesAfter: " + installedFeaturesAfter);

            int i = installedFeaturesBefore.indexOf("CWWKF0012I:"); // Ignore the timestamp
            int j = installedFeaturesAfter.indexOf("CWWKF0012I:");
            assertTrue("Server package command did not change the packaged server's feature cache: ",
                       i > 0 && j > 0 && installedFeaturesBefore.substring(i).equals(installedFeaturesAfter.substring(j)));
            //Expected: "CWWKF0012I: The server installed the following features: [cdi-1.2, concurrent-1.0, distributedMap-1.0, el-3.0, jndi-1.0, json-1.0, jsp-2.3, mpConfig-1.3, mpFaultTolerance-1.1, mpMetrics-1.1, servlet-3.1, ssl-1.0, timedExit-1.0]";
        } finally {
            if (server.isStarted()) {
                try {
                    server.stopServer();
                } catch (Exception e1) {
                    e1.printStackTrace(System.out);
                }
            }
            // Help tidy up, but likely redundant as getLibertyServer() will reset the server configuration.
            if (!features.isEmpty() && features.contains("mpFaultTolerance-1.1")) {
                features.removeAll(new HashSet<>(Arrays.asList("mpFaultTolerance-1.1", "mpMetrics-1.1", "jsp-2.3")));
                try {
                    server.updateServerConfiguration(config);
                } catch (Exception e2) {
                    e2.printStackTrace(System.out);
                }
            }

        }
    }

}
