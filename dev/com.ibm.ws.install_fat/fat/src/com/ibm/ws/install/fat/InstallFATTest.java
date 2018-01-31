/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import test.utils.TestUtils;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.internal.InstallUtils;
import componenttest.topology.utils.FileUtils;

/**
 *
 */
public abstract class InstallFATTest {

    protected static File imageDir;
    protected static String orginialTmpDir;
    protected static File tempDir;
    private static Properties wlpVersionProps;
    private static String originalWlpVersion;
    private static String originalWlpEdition;
    private static String originalWlpInstallType;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        imageDir = TestUtils.wlpDir.getAbsoluteFile();
        Log.info(InstallFATTest.class, "setUpBeforeClass", "***********************************************************************************************");
        Log.info(InstallFATTest.class, "setUpBeforeClass", "imageDir set to " + imageDir);
        if (TestUtils.wlpDir == null || !TestUtils.wlpDir.exists())
            throw new IllegalArgumentException("Test requires an existing root directory, but it could not be found: " + imageDir);

        tempDir = new File(orginialTmpDir, "cik");
        if (!tempDir.exists()) {
            tempDir.mkdir();
            if (!tempDir.exists())
                throw new IllegalArgumentException("Test requires an existing temp directory, but it could not be found: " + tempDir.getAbsolutePath());
        }

        orginialTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", tempDir.getAbsolutePath());
        File f = null;
        try {
            f = File.createTempFile("test", "txt");
        } catch (IOException e) {
            tempDir = new File(orginialTmpDir);
            System.setProperty("java.io.tmpdir", tempDir.getAbsolutePath());
            Log.info(InstallFATTest.class, "setUpBeforeClass", "java.io.tmpdir set to orginial tmp dir");
        } finally {
            InstallUtils.delete(f);
        }
        Log.info(InstallFATTest.class, "setUpBeforeClass", "java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));

        System.setProperty("cik.test.path", new File(".").getCanonicalPath());
        Log.info(InstallFATTest.class, "setUpBeforeClass", "cik.test.path set to " + System.getProperty("cik.test.path"));

        if (TestUtils.repositoryDescriptionUrl == null) {
            Log.info(InstallFATTest.class, "setUpBeforeClass", "not set repo");
        } else {
            System.setProperty("repository.description.url", TestUtils.repositoryDescriptionUrl);
            Log.info(InstallFATTest.class, "setUpBeforeClass", "repository.description.url set to " + System.getProperty("repository.description.url"));
        }

        setOriginalWlpProps();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (orginialTmpDir != null) {
            System.setProperty("java.io.tmpdir", orginialTmpDir);
            Log.info(InstallFATTest.class, "tearDownAfterClass", "java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));
            tempDir = new File(orginialTmpDir, "cik");
            InstallUtils.deleteDirectory(tempDir);
        }
    }

    @After
    public void tearDown() throws Exception {}

    private static void setOriginalWlpProps() throws IOException {

        File wlpVersionPropFile = new File(TestUtils.wlpDir, "lib/versions/WebSphereApplicationServer.properties");
        wlpVersionPropFile.setReadable(true);

        Log.info(InstallFATTest.class, "setOriginalWlpProps", "WebSphereApplicationServer.properties: " + wlpVersionPropFile.getAbsolutePath());

        FileInputStream fIn = null;
        wlpVersionProps = new Properties();
        try {
            fIn = new FileInputStream(wlpVersionPropFile);
            wlpVersionProps.load(fIn);
            originalWlpVersion = wlpVersionProps.getProperty("com.ibm.websphere.productVersion");
            originalWlpEdition = wlpVersionProps.getProperty("com.ibm.websphere.productEdition");
            originalWlpInstallType = wlpVersionProps.getProperty("com.ibm.websphere.productInstallType");
            Log.info(InstallFATTest.class, "setOriginalWlpProps", "Original version of the wlp directory : " + originalWlpVersion);
            Log.info(InstallFATTest.class, "setOriginalWlpProps", "com.ibm.websphere.productId : " +
                                                                  wlpVersionProps.getProperty("com.ibm.websphere.productId"));
            Log.info(InstallFATTest.class, "setOriginalWlpProps", "com.ibm.websphere.productEdition : " + originalWlpEdition);
            Log.info(InstallFATTest.class, "setOriginalWlpProps", "com.ibm.websphere.productInstallType : " + originalWlpInstallType);
        } finally {
            InstallUtils.close(fIn);
        }
    }

    protected static void replaceWlpProperties(String version, String edition, String installType) throws IOException {

        if (version == null && edition == null && installType == null)
            return;

        File wlpVersionPropFile = new File(TestUtils.wlpDir, "lib/versions/WebSphereApplicationServer.properties");
        wlpVersionPropFile.setWritable(true);

        Log.info(InstallFATTest.class, "replaceWlpProperties", "WebSphereApplicationServer.properties: " + wlpVersionPropFile.getAbsolutePath());

        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(wlpVersionPropFile);
            if (version != null) {
                wlpVersionProps.setProperty("com.ibm.websphere.productVersion", version);
                Log.info(InstallFATTest.class, "replaceWlpProperties", "Set the version of the wlp directory to : " + version);
            }
            if (edition != null) {
                wlpVersionProps.setProperty("com.ibm.websphere.productEdition", edition);
                Log.info(InstallFATTest.class, "replaceWlpProperties", "Set the edition of the wlp directory to : " + edition);
            }
            if (installType != null) {
                wlpVersionProps.setProperty("com.ibm.websphere.productInstallType", installType);
                Log.info(InstallFATTest.class, "replaceWlpProperties", "Set the installType of the wlp directory to : " + installType);
            }
            wlpVersionProps.store(fOut, null);
        } finally {
            InstallUtils.close(fOut);
        }
    }

    protected static void resetOriginalWlpProps() throws IOException {
        replaceWlpProperties(originalWlpVersion, originalWlpEdition, originalWlpInstallType);
    }

    protected static void deleteFiles(String methodName, String featureName, String[] filePathsToClear) throws Exception {
        Log.info(InstallFATTest.class, methodName, "If Exists, Deleting files for " + featureName);
        for (String filePath : filePathsToClear) {
            File f = new File(TestUtils.wlpDir, filePath);
            if (f.exists()) {
                if (f.isDirectory())
                    FileUtils.recursiveDelete(f);
                else
                    InstallUtils.delete(f);
            }
            Log.info(InstallFATTest.class, methodName, "Finished deleting file: " + f.getAbsolutePath());
        }
        File f = new File(TestUtils.wlpDir, "lafiles");
        if (f.exists()) {
            if (f.isDirectory())
                FileUtils.recursiveDelete(f);
            else
                InstallUtils.delete(f);
        }
        Log.info(InstallFATTest.class, methodName, "Finished deleting file: " + f.getAbsolutePath());
        Log.info(InstallFATTest.class, methodName, "Finished deleting files associated with " + featureName);
    }

    protected static void assertFilesExist(String[] filePaths) throws Exception {
        for (String filePath : filePaths) {
            File f = new File(TestUtils.wlpDir, filePath);
            assertTrue(filePath + " does not exist.", f.exists());
        }
    }

    protected static void assertFilesNotExist(String[] filePaths) throws Exception {
        for (String filePath : filePaths) {
            File f = new File(TestUtils.wlpDir, filePath);
            assertFalse(filePath + " does not exist.", f.exists());
        }
    }
}
