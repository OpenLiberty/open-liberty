/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.common.TestFile;

/**
 *
 */
public class MapBasedSelfExtractorTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    private static File outputUploadDir;
    private static boolean disableTestSuite;
    private static final List<File> testJars = new ArrayList<File>();
    private static final File dummyInstallerWithExtension = new File("build/dummyInstallerWithExtension/output/dummyInstallerWithExtension.jar");
    private static final File dummySampleJar = new File("build/dummySample/output/dummySample.jar");
    private static final int dummySampleJarSize = 251222;
    private static final File coreJar = findCoreJar();
    private static Map<File, Class<Map<String, Object>>> clazzes = new HashMap<File, Class<Map<String, Object>>>();
    private static File installDirForSamplesServer;

    static {
        testJars.add(dummySampleJar);
        testJars.add(coreJar);
    }

    private static File findCoreJar() {
        String uploadDir = System.getProperty("image.output.upload.dir");

        if (uploadDir == null) {
            uploadDir = "../build.image/output/upload/externals/installables/";
        }

        outputUploadDir = new File(uploadDir);
        if (!outputUploadDir.exists()) {
            disableTestSuite = true;
        }

        // Source License Edition (SLE) also disables this test.
        if (System.getProperty("is.sle") != null) {
            System.out.println("Skipping test because this is running in SLE");
            disableTestSuite = true;
        }

        if (disableTestSuite) {
            return null;
        }

        File wlpJar = null;
        File[] files = outputUploadDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                // Hunt out the wlp-developers, but NOT the IPLA version
                if (file.isFile() && name.startsWith("wlp-developers-runtime-") && name.endsWith(".jar") && !!!name.startsWith("wlp-developers-ipla-")) {
                    wlpJar = file;
                }
            }
        }

        if (wlpJar == null) {
            throw new IllegalStateException("Failed to find wlp-developers-runtime-*.jar in " + outputUploadDir.getAbsolutePath());
        }
        return wlpJar;
    }

    private static Map<String, Object> getMapBasedExtractorFromJar(File targetJar) throws Exception {
        //This would be a nice place to do some caching, but to test properly, we need a new copy of the class each time,
        //otherwise statics end up with stale values from previous test runs :-(
        ClassLoader loader = new URLClassLoader(new URL[] { targetJar.toURI().toURL() }, null);
        JarFile jarFile = new JarFile(targetJar);
        String extractorClass = jarFile.getManifest().getMainAttributes().getValue("Map-Based-Self-Extractor");
        jarFile.close();
        Class<Map<String, Object>> clazz = (Class<Map<String, Object>>) loader.loadClass(extractorClass);
        clazzes.put(targetJar, clazz);

        return clazz.newInstance();
    }

    @BeforeClass
    public static void setup() throws Exception {
        if (disableTestSuite)
            return;
        //Extract a server that will be used for testing addons against
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        installDirForSamplesServer = TestFile.createTempFile("liberty", "install");
        installDirForSamplesServer.delete();

        extractor.put("install.dir", installDirForSamplesServer);
        extractor.put("license.accept", Boolean.TRUE);
        assertEquals("correctly installed", 0, extractor.get("install.code"));
        validateInstall(installDirForSamplesServer);
    }

    @AfterClass
    public static void cleanUp() {
        if (disableTestSuite)
            return;
        delete(installDirForSamplesServer);
    }

    /**
     * We allow for version negotiation so in theory we can change the protocol used. Currently we support exactly
     * one version so this makes sure we can negotiation to this correctly.
     *
     * @throws Exception
     */
    @Test
    public void versionNegotiationTest() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);

        Integer val = Integer.valueOf(1);
        assertEquals("Negotiation using single version failed", val, extractor.put("install.version", val));
        List<Integer> version = new ArrayList<Integer>();
        version.add(val);
        assertEquals("Negotiation using list of single version failed", val, extractor.put("install.version", version));
        version.add(Integer.valueOf(2));
        assertEquals("Negotiation using list with multiple versions failed", val, extractor.put("install.version", version));
        version.remove(val);
        assertNull("Somehow managed to negociate to a verison when no agreement was valid", extractor.put("install.version", version));
    }

    /**
     * Make sure we can get the init return code. We expect it to be 0, ideally we would do error cases too, not sure how though.
     *
     * @throws Exception
     */
    @Test
    public void initTest() throws Exception {
        if (disableTestSuite)
            return;

        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        assertEquals("installer was not created", 0, extractor.get("installer.init.code"));
    }

    /**
     * Make sure if we don't do anything if install.code is got but no install.dir is installed.
     *
     * @throws Exception
     */
    @Test
    public void installNoDirTest() throws Exception {
        if (disableTestSuite)
            return;

        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        assertNull("product was installed when no install directory was provided.", extractor.get("install.code"));
    }

    /**
     * Attempts to install having provided no install status monitor.
     *
     * @throws Exception
     */
    @Test
    public void installWithNoProgress() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        try {
            extractor.put("install.dir", dir);
            extractor.put("license.accept", Boolean.TRUE);
            assertEquals("correctly installed", 0, extractor.get("install.code"));
            validateInstall(dir);
        } finally {
            delete(dir);
        }
    }

    /**
     * Attempts to install having provided not indicated license acceptance.
     *
     * @throws Exception
     */
    @Test
    public void installWithNoLicenseAcceptance() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        try {
            extractor.put("install.dir", dir);
            assertNull("Installed when it shouldn't have been", extractor.get("install.code"));
            assertFalse("The install dir exists and it should not", dir.exists());
        } finally {
            delete(dir);
        }
    }

    /**
     * Attempts to install having provided an install status monitor. Checks we call add enough times.
     *
     * @throws Exception
     */
    @Test
    public void installWithProgress() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        try {
            extractor.put("install.dir", dir);
            extractor.put("license.accept", Boolean.TRUE);
            int count = (Integer) extractor.get("install.monitor.size");
            List<String> monitor = new ArrayList<String>();
            extractor.put("install.monitor", monitor);
            assertEquals("correctly installed", 0, extractor.get("install.code"));
            assertEquals("did not install all files", count, monitor.size());
            validateInstall(dir);
        } finally {
            delete(dir);
        }
    }

    /**
     * Attempts to install with an extension who's root is a peer to the install dir.
     *
     * @throws Exception
     */
    @Test
    public void installWithExtension() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(dummyInstallerWithExtension);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        File installDir = new File(dir, "liberty");
        installDir.mkdirs();
        try {
            extractor.put("install.dir", installDir);
            extractor.put("license.accept", Boolean.TRUE);
            assertEquals("correctly installed", 0, extractor.get("install.code"));
            File[] files = dir.listFiles();
            assertEquals("Directory has the wrong number of files in it: " + Arrays.toString(files), 2, files.length);
            for (int i = 0; i < files.length; i++) {
                if ("liberty".equals(files[i].getName())) {
                    continue;
                } else if ("PFS".equals(files[i].getName())) {
                    continue;
                } else {
                    fail("Unexpected directory: " + files[i].getAbsolutePath());
                }
            }
        } finally {
            delete(dir);
        }
    }

    /**
     * @param dir check that the install is valid.
     */
    private static void validateInstall(File dir) {
        if (disableTestSuite)
            return;
        assertTrue("server script not found, install probably failed", new File(dir, "bin/server").exists());

        // check the empty dirs exist cause we keep breaking this and then we fix only a random subset
        assertTrue("server template apps dir does not exist", new File(dir, "templates/servers/defaultServer/apps").exists());
        assertTrue("server template dropins dir does not exist", new File(dir, "templates/servers/defaultServer/dropins").exists());
        assertTrue("shared apps dir does not exist", new File(dir, "usr/shared/apps").exists());
        assertTrue("shared config dir does not exist", new File(dir, "usr/shared/config").exists());
        assertTrue("shared resources dir does not exist", new File(dir, "usr/shared/resources").exists());
    }

    @Test
    public void installIntoEmpty() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        assertTrue("Unable to create directory: " + dir, dir.mkdirs());
        try {
            extractor.put("install.dir", dir);
            extractor.put("license.accept", Boolean.TRUE);
            assertEquals("Product was not installed", 0, extractor.get("install.code"));
            validateInstall(dir);
        } finally {
            delete(dir);
        }
    }

    @Test
    public void installIntoDirWithFiles() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        assertTrue("Unable to create directory: " + dir, dir.mkdirs());
        String testFile = "test.txt";
        String testData = "Some dummy data";
        PrintStream out = new PrintStream(new File(dir, testFile));
        out.println(testData);
        try {
            extractor.put("install.dir", dir);
            extractor.put("license.accept", Boolean.TRUE);
            assertEquals("Oops product was installed", 4, extractor.get("install.code"));
            assertTrue("Directory has been removed by mistake.", dir.exists());
            File[] files = dir.listFiles();
            assertEquals("Directory has the wrong number of files in it", 1, files.length);
            assertEquals("File is the test file", testFile, files[0].getName());
            BufferedReader reader = new BufferedReader(new FileReader(files[0]));
            assertEquals("First line in the file is incorrect", testData, reader.readLine());
            assertNull("There are too many lines in the test file, it was corrupted.", reader.readLine());
        } finally {
            delete(dir);
        }
    }

    /**
     * Attempts to install having provided an install status monitor which cancels a short way in
     *
     * @throws Exception
     */
    @Test
    public void installWithCancelation() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        try {
            extractor.put("install.dir", dir);
            extractor.put("license.accept", Boolean.TRUE);
            final int count = (Integer) extractor.get("install.monitor.size");
            @SuppressWarnings("serial")
            List<Object> monitor = new ArrayList<Object>() {
                @Override
                public boolean add(Object e) {
                    if (size() == (count / 2))
                        return false;
                    return super.add(e);
                }
            };
            extractor.put("install.monitor", monitor);
            assertEquals("correctly installed", 0, extractor.get("install.code"));
            assertEquals("installed too many files", count / 2, monitor.size());
            assertFalse("Directory should not exist", dir.exists());
        } finally {
            delete(dir);
        }
    }

    /**
     * Checks that we can get the beta license name correctly. This will change as the license changes.
     *
     * @throws Exception
     */
    @Test
    public void productName() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        assertEquals("The product name was not correct", "IBM WebSphere Application Server for Developers", extractor.get("product.name"));
    }

    /**
     * Checks that we can get the license name correctly. This will change as the license changes.
     *
     * @throws Exception
     */
    @Test
    public void licenseName() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        assertEquals("The product name was not correct", "International License Agreement for Non-Warranted Programs", extractor.get("license.name"));
    }

    /**
     * Checks we can get a Reader to the license agreement, and each call generates a new reader.
     *
     * @throws Exception
     */
    @Test
    public void getLicenseAgreement() throws Exception {
        if (disableTestSuite)
            return;
        testLicenseFile("license.agreement", "International License Agreement for Non-Warranted Programs");
    }

    /**
     * Checks we can get a Reader to the license info, and each call generates a new reader.
     *
     * @throws Exception
     */
    @Test
    public void getLicenseInfo() throws Exception {
        if (disableTestSuite)
            return;
        testLicenseFile("license.info", "LICENSE INFORMATION");
    }

    private void testLicenseFile(String key, String lineOne) throws Exception {
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        BufferedReader reader = new BufferedReader((Reader) extractor.get(key));
        assertNotNull("The reader to the license was not found", reader);
        String line = reader.readLine();
        assertEquals("The first line of the license was not as expected", lineOne, line);
        reader.close();
        reader = new BufferedReader((Reader) extractor.get(key));
        assertNotNull("The reader to the license was not found", reader);
        line = reader.readLine();
        assertEquals("We should have had a new reader at the start", lineOne, line);
        reader.close();
    }

    /**
     * @param dir recursive delete
     */
    private static void delete(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    delete(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    @Test
    public void testSampleOverrideUserDir() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(dummySampleJar);

        File userDir = TestFile.createTempFile("liberty", "samplesUserDir");
        userDir.delete();
        try {
            extractor.put("install.dir", installDirForSamplesServer);
            extractor.put("target.user.directory", userDir);
            assertEquals("Install should give RC 0", 0, extractor.get("install.code"));

            File sampleReadmeLocation = new File(userDir, "servers/dummySampleServer/README.html");
            File sampleDummyLibLocation = new File(userDir, "shared/resources/dummylibs/dummy-dep-1.0.jar");
            Assert.assertTrue("The location " + sampleReadmeLocation.getAbsolutePath() + " should exist.", sampleReadmeLocation.exists());
            Assert.assertTrue("The location " + sampleDummyLibLocation.getAbsolutePath() + " should exist.", sampleDummyLibLocation.exists());

        } finally {
            delete(userDir);
        }
    }

    @Test
    public void testSampleDownloadMonitorBytes() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(dummySampleJar);

        File userDir = TestFile.createTempFile("liberty", "samplesUserDir");
        userDir.delete();

        List<Integer> downloadedBytesCallback = new ArrayList<Integer>();
        try {
            extractor.put("install.dir", installDirForSamplesServer);
            extractor.put("target.user.directory", userDir);
            extractor.put("download.monitor", downloadedBytesCallback);

            assertEquals("Install should give RC 0", 0, extractor.get("install.code"));

            int totalBytes = 0;
            for (Integer i : downloadedBytesCallback) {
                totalBytes += i.intValue();
            }

            assertEquals("Total bytes recorded via the read callback should sum to the total download size", dummySampleJarSize, totalBytes);

        } finally {
            delete(userDir);
        }
    }

    @Test
    public void testSampleRejectDependencies() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(dummySampleJar);

        File userDir = TestFile.createTempFile("liberty", "samplesUserDir");
        userDir.delete();
        try {
            extractor.put("install.dir", installDirForSamplesServer);
            extractor.put("target.user.directory", userDir);
            extractor.put("download.deps", Boolean.valueOf(false));
            assertEquals("Install should give RC 0", 0, extractor.get("install.code"));

            File sampleReadmeLocation = new File(userDir, "servers/dummySampleServer/README.html");
            File sampleDummyLibLocation = new File(userDir, "shared/resources/dummylibs/dummy-dep-1.0.jar");
            assertTrue("The location " + sampleReadmeLocation.getAbsolutePath() + " should exist.", sampleReadmeLocation.exists());
            assertFalse("The location " + sampleDummyLibLocation.getAbsolutePath() + " should not exist.", sampleDummyLibLocation.exists());

        } finally {
            delete(userDir);
        }
    }

    @Test
    public void testSampleDownloadFileMonitorEntries() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(dummySampleJar);

        File userDir = TestFile.createTempFile("liberty", "samplesUserDir");
        userDir.delete();
        try {
            List<Map<String, Object>> myDownloadMonitor = new ArrayList<Map<String, Object>>();
            extractor.put("install.dir", installDirForSamplesServer);
            extractor.put("target.user.directory", userDir);
            extractor.put("download.file.monitor", myDownloadMonitor);
            assertEquals("Install should give RC 0", 0, extractor.get("install.code"));

            File sampleReadmeLocation = new File(userDir, "servers/dummySampleServer/README.html");
            File sampleDummyLibLocation = new File(userDir, "shared/resources/dummylibs/dummy-dep-1.0.jar");
            assertTrue("The location " + sampleReadmeLocation.getAbsolutePath() + " should exist.", sampleReadmeLocation.exists());
            assertTrue("The location " + sampleDummyLibLocation.getAbsolutePath() + " should exist.", sampleDummyLibLocation.exists());
            assertEquals("Number of entries in the download monitor should match the expected value", 1, myDownloadMonitor.size());

            Map<String, Object> myDownloadMonitorEntry = myDownloadMonitor.get(0);
            URL monitorDownloadUrl = (URL) myDownloadMonitorEntry.get("download.url");
            File monitorDownloadFile = (File) myDownloadMonitorEntry.get("download.target.file");

            URL expectedDownloadUrl = new File("samples/dummySample/libs/dummy-dep-1.0.jar").toURI().toURL();
            File expectedDownloadFile = new File(userDir, "shared/resources/dummylibs/dummy-dep-1.0.jar");
            assertTrue("The download URL file name in the monitor should match the download XML", expectedDownloadUrl.sameFile(monitorDownloadUrl));
            assertEquals("Expected download target File should match the File in the monitor", expectedDownloadFile, monitorDownloadFile);

        } finally {
            delete(userDir);
        }
    }

    @Test
    public void testSampleDownloadFileMonitorSize() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(dummySampleJar);
        assertEquals("download.monitor.size operation should return the expect value", Integer.valueOf(dummySampleJarSize), extractor.get("download.monitor.size"));
    }

    @Test
    public void testSampleGetArguments() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(dummySampleJar);

        File userDir = TestFile.createTempFile("liberty", "samplesUserDir");
        userDir.delete();
        try {
            Boolean licensePresent = (Boolean) extractor.get("license.present");
            Boolean hasExternalDeps = (Boolean) extractor.get("has.external.deps");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> listExternalDeps = (List<Map<String, Object>>) extractor.get("list.external.deps");
            String archiveContentType = (String) extractor.get("archive.content.type");
            String dependenciesDescription = (String) extractor.get("external.deps.description");

            assertFalse("License present should be false", licensePresent);
            assertTrue("Has external deps should be true", hasExternalDeps);
            assertEquals("External deps list size should match expected size", 1, listExternalDeps.size());
            assertTrue("Archive type should be 'sample'", "sample".equals(archiveContentType));
            assertEquals("Description of dependencies should match the value in the xml file", "Dummy Sample Test Dependencies", dependenciesDescription);

        } finally {
            delete(userDir);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetExternalDeps() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(dummySampleJar);

        List<Map<String, Object>> allDeps = (List<Map<String, Object>>) extractor.get("list.external.deps");
        Map<String, Object> myOnlyDep = allDeps.get(0);

        URL downloadURL = (URL) myOnlyDep.get("download.url");
        URL expectedDownloadUrl = new File("samples/dummySample/libs/dummy-dep-1.0.jar").toURI().toURL();
        assertTrue("The download URL file name in the dependency map should match the download XML", expectedDownloadUrl.sameFile(downloadURL));

        String depDownloadPath = (String) myOnlyDep.get("download.target");
        String expectedDownloadPath = "shared/resources/dummylibs/dummy-dep-1.0.jar";
        assertEquals("Target download path string in the dependency map should match the download XML", depDownloadPath, expectedDownloadPath);

    }

    @Test
    public void installNonEmptyDirectory() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        File usrDir = new File(dir, "usr");
        File etcDir = new File(dir, "etc");
        assertTrue("Unable to create directory: " + usrDir, usrDir.mkdirs());
        assertTrue("Unable to create directory: " + etcDir, etcDir.mkdirs());
        try {
            extractor.put("install.dir", dir);
            extractor.put("license.accept", Boolean.TRUE);
            assertEquals("Product was installed", 4, extractor.get("install.code"));
            extractor.put("allow.non.empty.install.directory", Boolean.FALSE);
            assertEquals("Product was installed", 4, extractor.get("install.code"));
            extractor.put("allow.non.empty.install.directory", Boolean.TRUE);
            assertEquals("Product was not installed", 0, extractor.get("install.code"));
            validateInstall(dir);
        } finally {
            delete(dir);
        }
    }

    @Test
    public void installNonEmptyDirectoryFileExist() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        File binDir = new File(dir, "bin");
        assertTrue("Unable to create directory: " + binDir, binDir.mkdirs());
        // create server file
        File serverFile = new File(binDir, "server");
        PrintWriter w = new PrintWriter(new FileOutputStream(serverFile));
        w.println(new Date() + "server\n\n");
        w.flush();
        w.close();
        assertTrue("Unable to create file: " + serverFile, serverFile.exists());
        try {
            extractor.put("install.dir", dir);
            extractor.put("license.accept", Boolean.TRUE);
            extractor.put("allow.non.empty.install.directory", Boolean.TRUE);
            assertEquals("Product was not installed", 4, extractor.get("install.code"));
            String errMsg = (String) extractor.get("install.error.message");
            assertTrue("Error message does not start with \"The file already exists:\"", errMsg.startsWith("The file already exists:"));
            assertTrue("Error message does not end with \"featureManager\"", errMsg.endsWith("server"));
            assertTrue("bin directory contains more than 1 file", !binDir.exists() || binDir.listFiles().length == 1);
        } finally {
            delete(dir);
        }
    }

    @Test
    public void testOverrideProductInstallType() throws Exception {
        if (disableTestSuite)
            return;
        Map<String, Object> extractor = getMapBasedExtractorFromJar(coreJar);
        File dir = TestFile.createTempFile("liberty", "install");
        dir.delete();
        assertTrue("Unable to create directory: " + dir, dir.mkdirs());
        try {
            extractor.put("install.dir", dir);
            extractor.put("license.accept", Boolean.TRUE);
            extractor.put("product.install.type", "InstallationManager");
            assertEquals("Product was not installed", 0, extractor.get("install.code"));
            validateInstall(dir);
            File wlpPropsFile = new File(dir, "lib/versions/WebSphereApplicationServer.properties");
            FileInputStream fIn = null;
            Properties wlpProps = new Properties();
            try {
                fIn = new FileInputStream(wlpPropsFile);
                wlpProps.load(fIn);
                assertEquals("productInstallType was not InstallationManager", "InstallationManager", wlpProps.getProperty("com.ibm.websphere.productInstallType"));
            } finally {
                if (fIn != null) {
                    try {
                        fIn.close();
                    } catch (IOException e) {
                    }
                }
            }
        } finally {
            delete(dir);
        }
    }
}