/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.saml.error.SamlException;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 * Unit test the {@link FileInfo} class.
 */
public class FileInfoTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static File file;
    private static FileInfo fileInfo;

    /**
     * Creates a temporal file and a {@link FileInfo} object to be used for the tests.
     * Fail the test case if an exception is thrown.
     */
    @BeforeClass
    public static void setUp() throws SamlException, IOException {
        outputMgr.trace("*=all");
        try {
            file = File.createTempFile("tempfile", ".tmp");

            fileInfo = FileInfo.getFileInfo(file);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    /**
     * Test if {@link FileInfo#equals(Object)} method return false if the provided object is not an instance of the {@link FileInfo} class.
     * Fails if return other value.
     */
    @Test
    public void equalsMethodShouldReturnFalseIfProvidedObjectIsNotInstanceOfFileInfo() {
        Assert.assertFalse(fileInfo.equals(file));
    }

    /**
     * Test if {@link FileInfo#equals(Object)} method return false if the provided object is null.
     * Fails if return other value.
     */
    @Test
    public void equalsMethodShouldReturnFalseIfProvidedObjectIsNull() {
        Assert.assertFalse(fileInfo.equals(null));
    }

    /**
     * Test if {@link FileInfo#equals(Object)} method return true if the provided object pass the next conditions:
     * <li>Object is not null.</li>
     * <li>Object is an instance of {@link FileInfo}.</li>
     * <li>The file object of the instance equals the file object of the provided object.</li>
     * <li>The {@link File#lastModified()} value equals in both cases (this instance and the provided object of the method).</li>
     * <li>The {@link File#length()} value equals in both cases (this instance and the provided object of the method).</li>
     */
    @Test
    public void equalsMethodShouldReturnTrueIfFilePassConditions() {
        Assert.assertTrue(fileInfo.equals(fileInfo));
    }

    /**
     * Test if {@link FileInfo#getHostName()} method retrieve the host name of the machine which execute this test.
     * Fails if an exception is thrown or the returned value is not equals as the expected.
     */
    @Test
    public void shouldRetrieveHostName() {
        try {
            Assert.assertEquals(java.net.InetAddress.getLocalHost().getCanonicalHostName().toLowerCase(), FileInfo.getHostName());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }
    }

    /**
     * Test if {@link FileInfo#exists()} method retrieve if the specified file provided in the {@link FileInfo#getFileInfo(File)} method
     * exists.
     */
    @Test
    public void shouldRetrieveIfFileExists() {
        Assert.assertTrue(fileInfo.exists());
    }

    /**
     * Test if {@link FileInfo#getPath()} method retrieve the path of an {@link FileInfo} instance.
     * Fails if it's not the same path as the expected.
     */
    @Test
    public void shouldReturnPathOfFile() {
        Assert.assertEquals(file.getPath(), fileInfo.getPath());
    }
}
