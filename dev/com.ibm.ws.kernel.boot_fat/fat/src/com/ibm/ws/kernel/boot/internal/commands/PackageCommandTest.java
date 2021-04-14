/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class PackageCommandTest {
    private static final String rootFatServerName = "com.ibm.ws.kernel.boot.root.fat";
    private static final String bootstrapFatServerName = "com.ibm.ws.kernel.bootstrap.fat";

    private static final String archiveName = "MyPackage";
    private static final String archiveNameJar = "MyPackage.jar";
    private static final String archiveNameZip = "MyPackage.zip";
    private static final String archiveNameTarGz = "MyPackage.tar.gz";
    private static final String archiveNameZOSPax = "MyPackage.pax";

    private static final String[] ARCHIVE_NAMES = {
                                                    archiveNameJar,
                                                    archiveNameZip,
                                                    archiveNameTarGz,
                                                    archiveNameZOSPax
    };

    private LibertyServer rootFatServer;
    @SuppressWarnings("unused")
    private String rootFatInstallPath;
    private String rootFatServerPath;

    private LibertyServer bootstrapFatServer;
    private String bootstrapFatInstallPath;
    private String bootstrapFatServerPath;

    @Before
    public void before() throws Exception {
        rootFatServer = LibertyServerFactory.getLibertyServer(rootFatServerName);
        rootFatInstallPath = rootFatServer.getInstallRoot();
        rootFatServerPath = rootFatServer.getServerRoot();
        System.out.println("Root FAT server: " + rootFatServerName);
        System.out.println("Root FAT server path: " + rootFatServerPath);

        for (String archiveName : ARCHIVE_NAMES) {
            delete(rootFatServerPath, archiveName);
        }

        bootstrapFatServer = LibertyServerFactory.getLibertyServer(bootstrapFatServerName);
        bootstrapFatInstallPath = bootstrapFatServer.getInstallRoot();
        bootstrapFatServerPath = bootstrapFatServer.getServerRoot();
        System.out.println("Bootstrap FAT server: " + bootstrapFatServerName);
        System.out.println("Bootstrap FAT server path: " + bootstrapFatServerPath);
        for (String archiveName : ARCHIVE_NAMES) {
            delete(bootstrapFatServerPath, archiveName);
        }
    }

    private void delete(String rootPath, String childPath) {
        (new File(rootPath + '/' + childPath)).delete();
    }

    //

    /**
     * The package command requires that the lib/extract directory exists,
     * as this directory contains a required manifest, self extractable
     * classes, etc. If this directory does not exist, then the command
     * should print this message:
     *
     * <em>CWWKE0922E: The package command cannot complete because the
     * installation is missing the lib/extract directory.</em>
     *
     * This test requires that the lib/extract directory be already missing,
     * and does not run.
     */
    @Test
    public void testMinify_Error_MissingLibExtract() throws Exception {
        LibertyServer server = bootstrapFatServer;

        String extractPath = bootstrapFatInstallPath + "/lib/extract";
        assumeTrue(!(new File(extractPath).exists()));

        String[] packageCmd = { "--include=minify" };
        verifyPackageError(server, packageCmd, "CWWKE0922E");
    }

    private static final String SELF_EXTRACT_CLASS_NAME = "wlp.lib.extract.SelfExtractRun";
    private static final String SELF_EXTRACT_RESOURCE_NAME = "wlp/lib/extract/SelfExtractRun.class";

    /**
     * Thrown an {@link org.junit.AssumptionViolatedException}
     * (a runtime exception) if self-extract files do not exist
     * in a target server.
     *
     * Any jar type packaging requires self extract files.
     * Don't run any tests which require these files if the
     * server image doesn't have them. This is the case when
     * running the tests in commercial liberty.
     *
     * @param server The server to test.
     */
    public static void assumeSelfExtractExists(LibertyServer server) {
        File installRoot = new File(server.getInstallRoot());
        File selfExtractClass = new File(installRoot, SELF_EXTRACT_RESOURCE_NAME);
        assumeTrue(selfExtractClass.exists());
    }

    /**
     * Packages --include=minify,runnable jar and verifies correct content.
     */
    @Test
    @SkipIfSysProp("os.name=z/OS") // Jar not supported on Z/OS
    public void testRunnable() throws Exception {
        String serverName = bootstrapFatServerName;
        LibertyServer server = bootstrapFatServer;

        // '--include=runnable' requires that the 'lib/extract' folder exists.
        assumeSelfExtractExists(bootstrapFatServer);

        String packageName = serverName + ".jar";
        String packagePath = bootstrapFatServerPath + '/' + packageName;
        String[] packageCmd = {
                                "--archive=" + packageName,
                                "--include=runnable"
        };
        verifyPackage(server, packageCmd, packageName, packagePath);

        // Check the manifest for headers that should be in there.
        // make sure it'a a runnable jar
        // Check that the self-extract and the server's entries are in the jar

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

    // This test has been removed.  The intent of the test is not
    // valid.  'include=usr' is not allowed with extension '.jar'.
    //
    // See 'testCorrectErrorMessageWhenUsrandJARArchiveSpecified',
    // which performs the same test, and which correctly verifies
    // that an error occurs.

//    /**
//     * Make sure that when packaging a jar archive using --include=usr,
//     * the resulting jar files does NOT contain the self-extract files.
//     */
//    @Test
//    @SkipIfSysProp("os.name=z/OS") // Jar not supported on Z/OS
//    public void testPackageJarArchiveWithIncludeEqualsUsr() throws Exception {
//        LibertyServer server = bootstrapFatServer;
//
//        String packageName = "packageUsr.jar";
//        String packagePath = bootstrapFatServerPath + '/' + packageName;
//        String[] packageCmd = {
//            "--archive=" + packageName,
//            "--include=usr" };
//        verifyPackage(server, packageCmd, packageName, packagePath);
//
//        try ( JarFile jarFile = new JarFile(packagePath) ) {
//            Manifest mf = jarFile.getManifest();
//            assertNotNull("Package [ " + packagePath + " ] is missing its manifest", mf);
//
//            assertTrue(mf.getMainAttributes().containsKey("Applies-To"));
//            assertEquals("Package [ " + packagePath + " has incorrect 'Applies-To'",
//                "com.ibm.websphere.appserver", mf.getMainAttributes().getValue("Applies-To"));
//            assertEquals("Package [ " + packagePath + " has incorrect 'Extract-Installer'",
//                "false", mf.getMainAttributes().getValue("Extract-Installer"));
//
//            // Check that the self-extract and the server's entries are in the jar
//            Enumeration<JarEntry> entries = jarFile.entries();
//            assertTrue("Package [ " + packagePath + " ] is empty", entries.hasMoreElements());
//
//            boolean foundServerEntry = false;
//            boolean foundSelfExtractEntry = false;
//            while ( (!foundServerEntry || !foundSelfExtractEntry) && entries.hasMoreElements()) {
//                JarEntry entry = entries.nextElement();
//                String entryName = entry.getName();
//                if ( !foundServerEntry ) {
//                    foundServerEntry = entryName.startsWith("wlp/usr/servers/com.ibm.ws.kernel.bootstrap.fat");
//                }
//                if ( !foundSelfExtractEntry ) {
//                    foundSelfExtractEntry = entryName.startsWith("wlp/lib/extract");
//                }
//            }
//            if ( !foundServerEntry ) {
//                fail("Package [ " + packagePath + " ] is missing entries [ wlp/usr/servers/com.ibm.ws.kernel.bootstrap.fat ]");
//            }
//            if ( !foundSelfExtractEntry ) {
//                fail("Package [ " + packagePath + " ] is missing entries [ wlp/lib/extract ]");
//            }
//        }
//    }

    @Test
    public void testMinify_Error_ProductExtensions_EmptyServerRoot() throws Exception {
        LibertyServer server = rootFatServer;

        ensureProductExt(server);

        String[] packageCmd = {
                                "--archive=" + archiveNameZip,
                                "--include=minify",
                                "--server-root="
        };
        verifyPackageError(server, packageCmd, "CWWKE0947W");
    }

    /**
     * This tests that --include=usr outputs the correct information in
     * the archive file.
     */
    @Test
    public void testMinify() throws Exception {
        LibertyServer server = rootFatServer;

        // '--include=minify' requires that the 'lib/extract' folder exists.
        assumeSelfExtractExists(rootFatServer);

        String packageName = archiveNameZip;
        String packagePath = rootFatServerPath + '/' + packageName;
        String[] packageCmd = {
                                "--archive=" + packageName,
                                "--include=minify"
        };
        verifyPackage(server, packageCmd, packageName, packagePath);

        try (ZipFile zipFile = new ZipFile(packagePath)) {
            boolean foundDefaultRootEntry = false;
            boolean foundUsrEntry = false;
            boolean foundBinEntry = false;
            boolean foundLibEntry = false;
            boolean foundDevEntry = false;
            boolean foundAll = false;

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
                if (!foundDevEntry) {
                    foundDevEntry = entryName.contains("/dev/");
                }

                foundAll = (foundDefaultRootEntry && foundUsrEntry &&
                            foundBinEntry && foundLibEntry && foundDevEntry);
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
            if (!foundLibEntry) {
                System.out.println("Package [ " + packagePath + " ] is missing dev entry [ /dev ]");
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
        }
    }

    @Test
    public void testUsr() throws Exception {
        LibertyServer server = rootFatServer;

        String packageName = archiveNameZip;
        String packagePath = rootFatServerPath + '/' + packageName;
        String[] packageCmd = {
                                "--archive=" + packageName,
                                "--include=usr"
        };
        verifyPackage(server, packageCmd, packageName, packagePath);

        try (ZipFile zipFile = new ZipFile(packagePath)) {
            boolean foundDefaultRootEntry = false;
            boolean foundUsrEntry = false;
            Enumeration<? extends ZipEntry> en = zipFile.entries();
            while ((!foundDefaultRootEntry || !foundUsrEntry) && en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String entryName = entry.getName();
                if (!foundDefaultRootEntry) {
                    foundDefaultRootEntry = entryName.startsWith("wlp");
                }
                if (!foundUsrEntry) {
                    foundUsrEntry = entryName.contains("/usr/");
                }
            }
            if (!foundDefaultRootEntry) {
                fail("Package [ " + packagePath + " ] missing root entries [ wlp ]");
            }
            if (!foundUsrEntry) {
                fail("Package [ " + packagePath + " ] missing user entries [ /usr/ ]");
            }
        }
    }

    /**
     * This tests that when --server-root is supplied, and --include=usr that the
     * /shared folder is also placed at the root of the archive.
     */
    @Test
    public void testUsr_SharedFolder_ServerRoot() throws Exception {
        LibertyServer server = rootFatServer;

        Path sharedPath = Paths.get(server.getServerSharedPath());
        File sharedFile = sharedPath.toFile();
        sharedFile.mkdirs();
        if (!sharedFile.exists()) {
            fail("Shared location [ " + sharedFile.getAbsolutePath() + " ] does not exists");
        }
        if (!sharedFile.isDirectory()) {
            fail("Shared location [ " + sharedFile.getAbsolutePath() + " ] is not a directory");
        }

        String packageName = archiveNameZip;
        String packagePath = rootFatServerPath + '/' + packageName;
        String[] packageCmd = {
                                "--archive=" + packageName,
                                "--include=usr",
                                "--server-root=MyRoot"
        };
        verifyPackage(server, packageCmd, packageName, packagePath);

        // Ensure root is correct in the .zip
        try (ZipFile zipFile = new ZipFile(packagePath)) {
            boolean foundMyRootSharedEntry = false;
            Enumeration<? extends ZipEntry> en = zipFile.entries();
            while (!foundMyRootSharedEntry && en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                foundMyRootSharedEntry = entry.getName().contains("MyRoot/shared");
            }
            if (!foundMyRootSharedEntry) {
                fail("Package [ " + packagePath + " ] missing shared entries [ MyRoot/shared/ ]");
            }
        }
    }

    /**
     * This tests that when --server-root is supplied and --include=minify that /usr
     * does show up in the archive file.
     */
    @Test
    public void testMinify_ServerRoot() throws Exception {
        LibertyServer server = rootFatServer;

        // '--include=minify' requires that the 'lib/extract' folder exists.
        assumeSelfExtractExists(rootFatServer);

        String packageName = archiveNameZip;
        String packagePath = rootFatServerPath + '/' + packageName;
        String[] packageCmd = {
                                "--archive=" + packageName,
                                "--include=minify",
                                "--server-root=MyRoot"
        };
        verifyPackage(server, packageCmd, packageName, packagePath);

        // For minify, there should be /usr in the structure with server-root option

        try (ZipFile zipFile = new ZipFile(packagePath)) {
            boolean foundServerEntry = false;
            boolean foundWarFileEntry = false;

            Enumeration<? extends ZipEntry> en = zipFile.entries();
            while ((!foundServerEntry || !foundWarFileEntry) && en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String entryName = entry.getName();

                if (!foundServerEntry) {
                    foundServerEntry = entryName.contains("MyRoot/usr/servers/com.ibm.ws.kernel.boot.root.fat");
                }
                if (!foundWarFileEntry) {
                    foundWarFileEntry = entryName.contains("MyRoot/usr/servers/com.ibm.ws.kernel.boot.root.fat/apps/AppsLooseWeb.war");
                }
            }

            if (!foundServerEntry) {
                fail("Package [ " + packagePath + " ] missing [ MyRoot/usr/servers/com.ibm.ws.kernel.boot.root.fat ]");
            }
            if (!foundWarFileEntry) {
                fail("Package [ " + packagePath + " ] missing [ MyRoot/usr/servers/com.ibm.ws.kernel.boot.root.fat/apps/AppsLooseWeb.war ]");
            }
        }
    }

    /**
     * This tests that when --include=runnable is supplied, and --archive is supplied with
     * a file extension not ending with .jar that an error is returned.
     */
    @Test
    public void testRunnable_Error_NonJar() throws Exception {
        LibertyServer server = rootFatServer;

        ensureProductExt(server);

        String[] packageCmd = {
                                "--archive=" + archiveNameZip,
                                "--include=runnable"
        };
        verifyPackageError(server, packageCmd, "CWWKE0950E");
    }

    /**
     * This tests that when the --archive value has no extension, and --include=runnable
     * that a .jar archive is created by default.
     */
    @Test
    @SkipIfSysProp("os.name=z/OS") // Jar not supported on Z/OS
    public void testRunnable_DefaultToJar() throws Exception {
        LibertyServer server = rootFatServer;

        // '--include=runnable' requires that the 'lib/extract' folder exists.
        assumeSelfExtractExists(rootFatServer);

        ensureProductExt(server);

        String packageName = archiveNameJar;
        String packagePath = rootFatServerPath + '/' + packageName;
        String[] packageCmd = {
                                "--archive=" + archiveName, // Use of 'archiveName' is correct
                                "--include=runnable"
        };
        verifyPackage(server, packageCmd, packageName, packagePath);
    }

    /**
     * This tests that when the --archive value has no extension, and --include != runnable
     * that a .zip archive is created by default if non-ZOS, else a .pax archive for ZOS.
     */
    @Test
    public void testUsr_DefaultToZip() throws Exception {
        LibertyServer server = rootFatServer;

        ensureProductExt(server);

        String packageName = null;
        String os = System.getProperty("os.name");
        if (os.equalsIgnoreCase("z/OS")) {
            packageName = archiveNameZOSPax;
        } else {
            packageName = archiveNameZip;
        }

        String packagePath = rootFatServerPath + '/' + packageName;
        String[] packageCmd = {
                                "--archive=" + archiveName, // Use of 'archiveName' is correct.
                                "--include=usr"
        };

        verifyPackage(server, packageCmd, packageName, packagePath);
    }

    /**
     * This tests that a .tar.gz file type is created when specified by --archive.
     */
    @Test
    public void testUsr_TarGz() throws Exception {
        LibertyServer server = rootFatServer;

        ensureProductExt(server);

        String packageName = archiveNameTarGz;
        String packagePath = rootFatServerPath + '/' + packageName;
        String[] packageCmd = {
                                "--archive=" + packageName,
                                "--include=usr"
        };
        verifyPackage(server, packageCmd, packageName, packagePath);
    }

    /**
     * This tests that when --include=usr is supplied, and --archive
     * is supplied with a file extension ending with .jar that an
     * error is returned.
     */
    @Test
    @SkipIfSysProp("os.name=z/OS") // Jar not supported on Z/OS
    public void testUsr_Error_Jar() throws Exception {
        LibertyServer server = rootFatServer;

        ensureProductExt(server);

        String[] packageCmd = {
                                "--archive=" + archiveNameJar,
                                "--include=usr"
        };
        verifyPackageError(server, packageCmd, "CWWKE0951E");

    }

    /**
     * Verify the embedded server instance launched by the package command does
     * not corrupt the feature cache of the packaged (target) server.
     */

    private static final List<String> cacheFeatures;
    static {
        cacheFeatures = new ArrayList<String>(3);
        cacheFeatures.add("mpFaultTolerance-1.1");
        cacheFeatures.add("mpMetrics-1.1");
        cacheFeatures.add("jsp-2.3");
    }

    // Expected: "CWWKF0012I: The server installed the following features:
    // [cdi-1.2, concurrent-1.0, distributedMap-1.0, el-3.0, jndi-1.0,
    //  json-1.0, jsp-2.3, mpConfig-1.3, mpFaultTolerance-1.1, mpMetrics-1.1,
    //  servlet-3.1, ssl-1.0, timedExit-1.0]";

    private String collectFeatures(LibertyServer server, String tag) throws Exception {
        List<String> matches;
        try (CloseableServer closeableServer = new CloseableServer(server)) {
            matches = server.findStringsInLogs("CWWKF0012I:.*");
        }
        if (matches.isEmpty()) {
            fail("Missing features [ CWWKF0012I ] in server [ " + server.getInstallRoot() + " ]");
        }

        String rawFeatures = matches.get(0);;
        System.out.println("Raw " + tag + " features: " + rawFeatures);
        int offset = rawFeatures.indexOf("CWWKF0012I:");
        if (offset == -1) {
            fail("Missing " + tag + " features [ CWWKF0012I ]: " + rawFeatures);
        }
        return rawFeatures.substring(offset);
    }

    @Test
    public void testMinify_PreserveFeatureCache() throws Exception {
        LibertyServer server = bootstrapFatServer;
        String serverPath = bootstrapFatServerPath;

        // '--include=minify' requires that the 'lib/extract' folder exists.
        assumeSelfExtractExists(rootFatServer);

        try (ServerFeatures serverFeatures = new ServerFeatures(server, cacheFeatures)) {
            String initialFeatures = collectFeatures(server, "initial");

            String packageName = server.getServerName() + ".jar";
            String packagePath = bootstrapFatServerPath + '/' + packageName;
            String[] packageCmd = {
                                    "--archive=" + packageName,
                                    "--include=minify"
            };
            verifyPackage(server, packageCmd, packageName, packagePath);

            String finalFeatures = collectFeatures(server, "final");

            assertEquals("Server [ " + serverPath + " ] cached features were changed", initialFeatures, finalFeatures);
        }
    }

    private static class CloseableServer implements Closeable {
        private final LibertyServer server;

        public CloseableServer(LibertyServer server) throws Exception {
            this.server = server;
            server.startServer(true);
        }

        private void stopServer() throws Exception {
            if (!server.isStarted()) {
                return;
            }

            server.stopServer();
        }

        @Override
        public void close() throws IOException {
            try {
                stopServer();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    private static class ServerFeatures implements Closeable {
        private final LibertyServer server;
        private final Collection<String> features;

        public ServerFeatures(LibertyServer server, Collection<String> features) throws Exception {
            this.server = server;
            this.features = features;

            setFeatures();
        }

        private void setFeatures() throws Exception {
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> serverFeatures = config.getFeatureManager().getFeatures();
            serverFeatures.addAll(features);
            server.updateServerConfiguration(config);
        }

        private void unsetFeatures() throws Exception {
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> serverFeatures = config.getFeatureManager().getFeatures();
            serverFeatures.removeAll(features);
            server.updateServerConfiguration(config);
        }

        @Override
        public void close() throws IOException {
            try {
                unsetFeatures();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    // Make sure we have the /wlp/etc/extension directory which
    // indicates Product Extensions are installed.

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

    private String packageServer(LibertyServer server, String[] packageCmd) throws Exception {
        return server.executeServerScript("package", packageCmd).getStdout();
    }

    private void verifyPackage(
                               LibertyServer server,
                               String[] packageCmd, String packageName, String packagePath) throws Exception {

        System.out.println("Packaging server [ " + server.getInstallRoot() + " ]");
        System.out.println("Package [ " + packagePath + " ]");

        String stdout = packageServer(server, packageCmd);

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

    private void verifyPackageError(LibertyServer server, String[] packageCmd, String errorText) throws Exception {
        String stdout = packageServer(server, packageCmd);
        if (!stdout.contains(errorText)) {
            fail("Packaging output missing error " + errorText + ". STDOUT = " + stdout);
        }
    }
}
