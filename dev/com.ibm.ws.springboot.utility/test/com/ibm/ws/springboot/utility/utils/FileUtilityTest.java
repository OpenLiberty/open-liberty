/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.utility.utils;

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

import com.ibm.ws.springboot.utility.IFileUtility;

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
    private final IFileUtility fileUtil = new FileUtility();

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @Test
    public void mkDirs_yesExistsIsDir() {

        mock.checking(new Expectations() {
            {
                one(file).exists();
                will(returnValue(true));
                one(file).isDirectory();
                will(returnValue(true));
            }
        });
        assertTrue("If we exist and we are a directory return true",
                   fileUtil.mkDirs(file, stdout));
    }

    @Test
    public void mkDirs_yesExistsIsFileFail() {

        mock.checking(new Expectations() {
            {
                one(file).exists();
                will(returnValue(true));
                one(file).isDirectory();
                will(returnValue(false));
                one(file).getAbsolutePath();
                will(returnValue("testPath"));
                one(stdout).println("Failed to create directory testPath");
            }
        });
        assertFalse("If we exist but we are not a directory return false",
                    fileUtil.mkDirs(file, stdout));
    }

    @Test
    public void mkDirs_noParentNoExistFail() {

        mock.checking(new Expectations() {
            {
                one(file).exists();
                will(returnValue(false));
                one(file).getParentFile();
                will(returnValue(null));
                one(file).mkdir();
                will(returnValue(false));
                one(file).getAbsolutePath();
                will(returnValue("testFile"));
                one(stdout).println("Failed to create directory testFile");
            }
        });
        assertFalse("If we don't have a parent, we should return false if we can create the dir",
                    fileUtil.mkDirs(file, stdout));
    }

    @Test
    public void mkDirs_parentExists() {

        mock.checking(new Expectations() {
            {
                one(file).exists();
                will(returnValue(false));
                one(file).getParentFile();
                will(returnValue(parent));
                one(parent).exists();
                will(returnValue(true));
                one(parent).isDirectory();
                will(returnValue(true));
                one(file).mkdir();
                will(returnValue(true));
            }
        });
        assertTrue("If the parent exists, return true if we can create the directory",
                   fileUtil.mkDirs(file, stdout));
    }

    @Test
    public void mkDirs_succeedParentCreate() {
        mock.checking(new Expectations() {
            {
                one(file).exists();
                will(returnValue(false));
                one(file).getParentFile();
                will(returnValue(parent));
                one(parent).exists();
                will(returnValue(false));
                one(parent).getParentFile();
                will(returnValue(grandParent));
                one(grandParent).exists();
                will(returnValue(true));
                one(grandParent).isDirectory();
                will(returnValue(true));
                one(parent).mkdir();
                will(returnValue(true));
                one(file).mkdir();
                will(returnValue(true));
            }
        });
        assertTrue("If parent's parent exists and parent is created, return true",
                   fileUtil.mkDirs(file, stdout));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#createParentDirectory(java.io.PrintStream, java.io.File)}.
     */
    @Test
    public void mkDirs_failParentCreate() throws Exception {
        mock.checking(new Expectations() {
            {
                one(file).exists();
                will(returnValue(false));
                one(file).getParentFile();
                will(returnValue(parent));
                one(parent).exists();
                will(returnValue(false));
                one(parent).getParentFile();
                will(returnValue(grandParent));
                one(grandParent).exists();
                will(returnValue(true));
                one(grandParent).isDirectory();
                will(returnValue(true));
                one(parent).mkdir();
                will(returnValue(false));
                one(parent).getAbsolutePath();
                will(returnValue("testParent"));
                one(stdout).println("Failed to create directory testParent");
            }
        });
        assertFalse("If parent can not be created, return false",
                    fileUtil.mkDirs(file, stdout));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.FileUtility#createParentDirectory(java.io.PrintStream, java.io.File)}.
     *
     * Verify that a deep failure (grandParent fails to create) does not try to create all
     * subsequent directories.
     */
    @Test
    public void mkdirs_deepFailCreate() throws Exception {
        mock.checking(new Expectations() {
            {
                one(file).exists();
                will(returnValue(false));
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
                one(grandParent).getAbsolutePath();
                will(returnValue("testGrandParent"));
                one(stdout).println("Failed to create directory testGrandParent");

                never(parent).mkdir();
            }
        });
        assertFalse("If parent can not be created, return false",
                    fileUtil.mkDirs(file, stdout));
    }

}
