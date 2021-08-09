/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;
import test.shared.Constants;
import test.shared.TestUtils;

public class FileUtilsTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() {
        TestUtils.cleanTempFiles();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        TestUtils.cleanTempFiles();
    }

    @Test
    public void testNormalizePathDrive() {
        Assume.assumeTrue(File.separatorChar == '\\');
        Assert.assertEquals("", FileUtils.normalizePathDrive(""));
        Assert.assertEquals("c", FileUtils.normalizePathDrive("c"));
        Assert.assertEquals("C", FileUtils.normalizePathDrive("C"));
        Assert.assertEquals("C:", FileUtils.normalizePathDrive("c:"));
        Assert.assertEquals("C:", FileUtils.normalizePathDrive("C:"));
        Assert.assertEquals("C:\\", FileUtils.normalizePathDrive("c:\\"));
        Assert.assertEquals("C:\\", FileUtils.normalizePathDrive("C:\\"));
    }

    @Test
    public void testGetFile() throws MalformedURLException {
        final String m = "testGetFile";

        URL url;
        File result;
        String expected;

        try {
            // The following strings were printed out from getBootstrapJar
            // during debugging... Need to make sure all end up legible...
            // (i.e. these strings construct equivalent URLs to what is returned
            // from Utils.class.getProtectionDomain().getCodeSource().getLocation())

            // UNC path with a space.
            // The URL for a UNC path is file:////server/path, but the
            // deprecated File.toURL() as used by java -jar/-cp incorrectly
            // returns file://server/path/, which has an invalid authority
            // component.
            url = new URL("file://HOST/with%20space/lib/com.ibm.liberty_1.0.jar");
            expected = "//HOST/with space/lib/com.ibm.liberty_1.0.jar";
            result = FileUtils.getFile(url);
            testResult(url, expected, result);

            // UNC path without a space.
            // The URL for a UNC path is file:////server/path, but the
            // deprecated File.toURL() as used by java -jar/-cp incorrectly
            // returns file://server/path/, which has an invalid authority
            // component.
            url = new URL("file://HOST/nospace/lib/com.ibm.liberty_1.0.jar");
            expected = "//HOST/nospace/lib/com.ibm.liberty_1.0.jar";
            result = FileUtils.getFile(url);
            testResult(url, expected, result);

            // Windows path with space
            url = new URL("file:/C:/with%20space/lib/com.ibm.liberty_1.0.jar");
            expected = "/C:/with space/lib/com.ibm.liberty_1.0.jar";
            result = FileUtils.getFile(url);
            testResult(url, expected, result);

            // Windows path without space
            url = new URL("file:/C:/nospace/lib/com.ibm.liberty_1.0.jar");
            expected = "/C:/nospace/lib/com.ibm.liberty_1.0.jar";
            result = FileUtils.getFile(url);
            testResult(url, expected, result);

            // Windows path with a literal +
            url = new URL("file:/C:/with+plus/lib/com.ibm.liberty_1.0.jar");
            expected = "/C:/with+plus/lib/com.ibm.liberty_1.0.jar";
            result = FileUtils.getFile(url);
            testResult(url, expected, result);

            // *nix path with space
            url = new URL("file:/with%20space/lib/com.ibm.liberty_1.0.jar");
            expected = "/with space/lib/com.ibm.liberty_1.0.jar";
            result = FileUtils.getFile(url);
            testResult(url, expected, result);

            // *nix path without space
            url = new URL("file:/nospace/lib/com.ibm.liberty_1.0.jar");
            expected = "/nospace/lib/com.ibm.liberty_1.0.jar";
            result = FileUtils.getFile(url);
            testResult(url, expected, result);

            // *nix path with a literal +
            url = new URL("file:/with+plus/lib/com.ibm.liberty_1.0.jar");
            expected = "/with+plus/lib/com.ibm.liberty_1.0.jar";
            result = FileUtils.getFile(url);
            testResult(url, expected, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            outputMgr.restoreStreams();
        }
    }

    private void testResult(URL url, String expected, File resultFile) {
        File expectedFile = new File(expected);

        System.out.println("---   URL: " + url.toString());
        System.out.println(" expected:\t" + expectedFile);
        System.out.println("   result:\t" + resultFile);

        assertEquals(url.toString() + " not converted to path as expected ", expectedFile, resultFile);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.internal.KernelUtils.Utils#recursiveClean(java.io.File)} .
     * 
     * @throws IOException
     */
    @Test
    public void testRecursiveClean() throws IOException {

        // These shouldn't blow up
        FileUtils.recursiveClean(null);
        FileUtils.recursiveClean(new File("notexist"));

        // Test recursive clean
        File file1, dir2, file3, dir4, file5;
        String prefix = "FileTestTmp";

        file1 = TestUtils.createTempFile(prefix, ".tmp");
        file1.deleteOnExit();

        dir2 = new File(file1.getParentFile(), prefix);
        dir2.mkdirs();
        dir2.deleteOnExit();

        file3 = TestUtils.createTempFile(prefix, ".tmp", dir2);

        dir4 = new File(dir2, prefix);
        dir4.mkdirs();
        dir4.deleteOnExit();

        file5 = TestUtils.createTempFile(prefix, ".tmp", dir4);

        assertTrue(file1.exists());
        assertTrue(dir2.exists());
        assertTrue(file3.exists());
        assertTrue(file3.getParent().equals(dir2.getPath()));
        assertTrue(dir4.exists());
        assertTrue(file5.exists());
        assertTrue(file5.getParent().equals(dir4.getPath()));

        FileUtils.recursiveClean(file1);
        assertFalse("File1 should not exist (deleted)", file1.exists());
        assertTrue("Dir2 should exist (untouched)", dir2.exists());
        assertTrue("File3 should exist (untouched)", file3.exists());
        assertTrue("Dir4 should exist (untouched)", dir4.exists());
        assertTrue("File5 should exist (untouched)", file5.exists());

        FileUtils.recursiveClean(dir2);
        assertFalse("Dir2 should not exist (deleted)", dir2.exists());
        assertFalse("File3 should not exist(deleted recursively)", file3.exists());
        assertFalse("Dir4 should exist (deleted)", dir4.exists());
        assertFalse("File5 should exist (deleted)", file5.exists());
    }

    /**
     * Test that recursiveClean doesn't blow up with an NPE when a directory can't be read
     * 
     * @throws IOException
     */
    @Test
    public void testRecursiveCleanNoReadPerms() throws IOException {
        File file1, dir2 = null;
        try {
            // These shouldn't blow up
            FileUtils.recursiveClean(null);
            FileUtils.recursiveClean(new File("notexist"));

            // Test recursive clean

            String prefix = "FileTestTmp";

            dir2 = new File(Constants.TEST_TMP_ROOT_FILE, prefix);
            dir2.mkdirs();
            dir2.deleteOnExit();
            if (!dir2.setReadable(false)) {
                // Platform doesn't support setting readable to false, just return
                return;
            }
            if ("root".equals(System.getProperty("user.name"))) {
                // The super-user can't set a file not readable to itself, so just return
                return;
            }

            file1 = TestUtils.createTempFile(prefix, ".tmp", dir2);
            file1.deleteOnExit();

            assertTrue(file1.exists());
            assertTrue(dir2.exists());

            boolean cleaned = FileUtils.recursiveClean(dir2);
            assertFalse("recursiveClean should report failure", cleaned);
            assertTrue("Dir2 should exist (not deleted)", dir2.exists());
            assertTrue("File1 should exist (not deleted)", file1.exists());

            assertTrue(dir2.setReadable(true));
            assertTrue(dir2.setWritable(true));
            cleaned = FileUtils.recursiveClean(dir2);
            assertTrue("recursiveClean should succeed", cleaned);
            assertFalse("Dir2 should not exist (deleted)", dir2.exists());
            assertFalse("File1 should not exist (deleted)", file1.exists());
        } finally {
            if (dir2 != null) {
                dir2.setReadable(true);
                dir2.setWritable(true);
            }
        }

    }
}
