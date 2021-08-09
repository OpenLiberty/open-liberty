/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests for {@link FileUtils}.
 */
public class FileUtilsTest {

    private static final String testClassesDir = System.getProperty("test.classesDir", "bin_test");
    private static final File directory = new File(testClassesDir + "/resources");

    private static final File file = new File(directory, "AFile.txt");

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.FileUtils#fileIsFile(java.io.File)}.
     */
    @Test
    public void testFileIsFile() {
        Assert.assertTrue("The file should return that it is a file", FileUtils.fileIsFile(file));
        Assert.assertFalse("The directory should return that it is not a file", FileUtils.fileIsFile(directory));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.FileUtils#fileIsDirectory(java.io.File)}.
     */
    @Test
    public void testFileIsDirectory() {
        Assert.assertFalse("The file should return that it is not a directory", FileUtils.fileIsDirectory(file));
        Assert.assertTrue("The directory should return that it is a directory", FileUtils.fileIsDirectory(directory));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.FileUtils#fileExists(java.io.File)}.
     */
    @Test
    public void testFileExists() {
        Assert.assertTrue("The file should exist", FileUtils.fileExists(file));
        Assert.assertTrue("The directory should exist", FileUtils.fileExists(directory));
        Assert.assertFalse("The made up file should not exist", FileUtils.fileExists(new File("resources/madeup.txt")));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.FileUtils#fileLength(java.io.File)}.
     */
    @Test
    public void testFileLength() {
        Assert.assertTrue("The file length should be greater than 0", FileUtils.fileLength(file) > 0);
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.FileUtils#listFiles(java.io.File)}.
     */
    @Test
    public void testListFiles() {
        File[] files = FileUtils.listFiles(directory);
        Assert.assertEquals("There should be one file in the resources directory", 1, files.length);
        Assert.assertEquals("The file in the resources directory should be our test file", file, files[0]);
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.FileUtils#getInputStream(java.io.File)}.
     * 
     * @throws IOException
     */
    @Test
    public void testGetInputStream() throws IOException {
        Assert.assertNotNull("Unable to create input stream for file", FileUtils.getInputStream(file));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.FileUtils#fileLastModified(File)}.
     */
    @Test
    public void testFileLastModified() {
        Assert.assertTrue("The last modified for the file should be greater than 0", FileUtils.fileLastModified(file) > 0);
        Assert.assertTrue("The last modified for the directory should be greater than 0", FileUtils.fileLastModified(directory) > 0);
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.FileUtils#canRead(java.io.File)}.
     */
    @Test
    public void testCanRead() {
        Assert.assertTrue("Should be able to read the file", FileUtils.fileExists(file));
        Assert.assertTrue("Should be able to read the directory", FileUtils.fileExists(directory));
    }

}
