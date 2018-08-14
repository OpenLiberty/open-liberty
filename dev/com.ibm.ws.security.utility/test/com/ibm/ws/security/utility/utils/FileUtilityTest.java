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
package com.ibm.ws.security.utility.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.security.utility.IFileUtility;

import test.common.junit.rules.MaximumJavaLevelRule;

/**
 *
 */
public class FileUtilityTest {

    @ClassRule
    public static MaximumJavaLevelRule maxLevel = new MaximumJavaLevelRule(8);

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final PrintStream stdout = mock.mock(PrintStream.class, "stdout");
    private final File file = mock.mock(File.class, "file");
    private final File parent = mock.mock(File.class, "parent");
    private final File grandParent = mock.mock(File.class, "grandParent");
    private final String SLASH = FileUtility.SLASH;
    private final IFileUtility fileUtil = new FileUtility("WLP_USER_DIR", "WLP_OUTPUT_DIR");

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#getServersDirectory()}.
     */
    @Test
    public void getServersDirectory_noSetVariable() {
        IFileUtility fileUtil = new FileUtility(null, null);
        assertEquals("Did not get expected constructed path",
                     System.getProperty("user.dir") + SLASH + "usr" + SLASH + "servers" + SLASH,
                     fileUtil.getServersDirectory());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#getServersDirectory()}.
     */
    @Test
    public void getServersDirectory_WLP_USER_DIR() {
        IFileUtility fileUtil = new FileUtility("WLP_USER_DIR", null);
        assertEquals("Did not get expected value for WLP_USER_DIR",
                     "WLP_USER_DIR" + SLASH + "servers" + SLASH, fileUtil.getServersDirectory());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#getServersDirectory()}.
     */
    @Test
    public void getServersDirectory_WLP_OUTPUT_DIR() {
        IFileUtility fileUtil = new FileUtility(null, "WLP_OUTPUT_DIR");
        assertEquals("Did not get expected value for WLP_OUTPUT_DIR",
                     "WLP_OUTPUT_DIR" + SLASH, fileUtil.getServersDirectory());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#getServersDirectory()}.
     */
    @Test
    public void getServersDirectory_bothVariables() {
        IFileUtility fileUtil = new FileUtility("WLP_USER_DIR", "WLP_OUTPUT_DIR");
        assertEquals("WLP_OUTPUT_DIR takes precedence over WLP_USER_DIR",
                     "WLP_OUTPUT_DIR" + SLASH, fileUtil.getServersDirectory());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#getClientsDirectory()}.
     */
    @Test
    public void getClientsDirectory_noSetVariable() {
        IFileUtility fileUtil = new FileUtility(null, null);
        assertEquals("Did not get expected constructed path",
                     System.getProperty("user.dir") + SLASH + "usr" + SLASH + "clients" + SLASH,
                     fileUtil.getClientsDirectory());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#getClientsDirectory()}.
     */
    @Test
    public void getClientsDirectory_WLP_USER_DIR() {
        IFileUtility fileUtil = new FileUtility("WLP_USER_DIR", null);
        assertEquals("Did not get expected value for WLP_USER_DIR",
                     "WLP_USER_DIR" + SLASH + "clients" + SLASH, fileUtil.getClientsDirectory());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#createParentDirectory(java.io.PrintStream, java.io.File)}.
     */
    @Test
    public void createParentDirectory_noParent() {

        mock.checking(new Expectations() {
            {
                one(file).getParentFile();
                will(returnValue(null));
            }
        });
        assertTrue("If we don't have a parent, we should return null",
                   fileUtil.createParentDirectory(stdout, file));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#createParentDirectory(java.io.PrintStream, java.io.File)}.
     */
    @Test
    public void createParentDirectory_parentExists() {

        mock.checking(new Expectations() {
            {
                one(file).getParentFile();
                will(returnValue(parent));
                one(parent).exists();
                will(returnValue(true));
            }
        });
        assertTrue("If the parent exists, return true",
                   fileUtil.createParentDirectory(stdout, file));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#createParentDirectory(java.io.PrintStream, java.io.File)}.
     */
    @Test
    public void createParentDirectory_succeedCreate() {
        mock.checking(new Expectations() {
            {
                one(file).getParentFile();
                will(returnValue(parent));
                one(parent).exists();
                will(returnValue(false));
                one(parent).getParentFile();
                will(returnValue(grandParent));
                one(grandParent).exists();
                will(returnValue(true));
                one(parent).mkdir();
                will(returnValue(true));
            }
        });
        assertTrue("If parent's parent exists and parent is created, return true",
                   fileUtil.createParentDirectory(stdout, file));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#createParentDirectory(java.io.PrintStream, java.io.File)}.
     */
    @Test
    public void createParentDirectory_failCreate() throws Exception {
        mock.checking(new Expectations() {
            {
                one(file).getParentFile();
                will(returnValue(parent));
                one(parent).exists();
                will(returnValue(false));
                one(parent).getParentFile();
                will(returnValue(grandParent));
                one(grandParent).exists();
                will(returnValue(true));
                one(parent).mkdir();
                will(returnValue(false));
                one(parent).getCanonicalPath();
                will(returnValue("testParent"));
                one(stdout).println("Failed to create directory testParent");
            }
        });
        assertFalse("If parent can not be created, return false",
                    fileUtil.createParentDirectory(stdout, file));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#createParentDirectory(java.io.PrintStream, java.io.File)}.
     *
     * Verify that a deep failure (grandParent fails to create) does not try to create all
     * subsequent directories.
     */
    @Test
    public void createParentDirectory_deepFailCreate() throws Exception {
        mock.checking(new Expectations() {
            {
                one(file).getParentFile();
                will(returnValue(parent));

                one(parent).exists();
                will(returnValue(false));
                one(parent).getParentFile();
                will(returnValue(grandParent));

                one(grandParent).exists();
                will(returnValue(false));
                one(grandParent).getParentFile();
                will(returnValue(null)); // Pretend the grandParent is the root

                one(grandParent).mkdir();
                will(returnValue(false));
                one(grandParent).getCanonicalPath();
                will(returnValue("testGrandParent"));
                one(stdout).println("Failed to create directory testGrandParent");

                never(parent).mkdir();
            }
        });
        assertFalse("If parent can not be created, return false",
                    fileUtil.createParentDirectory(stdout, file));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#resolvePath(java.lang.String)}.
     */
    @Test
    public void resolvePathString_exists() {
        String path = fileUtil.resolvePath(".");
        assertTrue("Should contain the project name in the path, but was " + path,
                   path.contains("com.ibm.ws.security.utility"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#resolvePath(java.lang.String)}.
     */
    @Test
    public void resolvePathString_doesntExists() {
        String path = fileUtil.resolvePath("/i/dont/exist");
        assertTrue("Should end with 'exist'",
                   path.endsWith("exist"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#resolvePath(java.io.File)}.
     */
    @Test
    public void resolvePathFile_exists() {
        File f = new File(".");
        String path = fileUtil.resolvePath(f);
        assertTrue("Should contain the project name in the path, but was " + path,
                   path.contains("com.ibm.ws.security.utility"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#resolvePath(java.io.File)}.
     */
    @Test
    public void resolvePathFile_doesntExist() {
        File f = new File("/i/dont/exist");
        String path = fileUtil.resolvePath(f);
        assertTrue("Should end with 'exist'",
                   path.endsWith("exist"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#exists(String)}.
     */
    @Test
    public void exists_true() {
        assertTrue(fileUtil.exists("."));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#resolvePath(java.io.File)}.
     */
    @Test
    public void exists_false() {
        assertFalse(fileUtil.exists("/i/dont/exist"));
    }

}
