/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Array;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import test.LoggingTestUtils;
import test.TestConstants;

public class FileLogSetTest {
    private static final File dir = new File(TestConstants.BUILD_TMP, "dir");
    private static final File dir2 = new File(TestConstants.BUILD_TMP, "dir2");
    private static final String NAME = "test";
    private static final String NAME2 = "test2";
    private static final String EXT = ".log";
    private static final String EXT2 = ".log2";

    @AfterClass
    public static void afterClass() {
        LoggingTestUtils.deepClean(dir);
    }

    @Before
    public void before() {
        LoggingTestUtils.deepClean(dir);
    }

    private static Matcher<File> exists() {
        return new TypeSafeMatcher<File>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("existing");
            }

            @Override
            public boolean matchesSafely(File item) {
                return item.exists();
            }
        };
    }

    private static File[] createN(int n, FileLogSet fls) throws Exception {
        File[] files = new File[n];
        for (int i = 0; i < n; i++) {
            files[i] = fls.createNewFile();
        }

        return files;
    }

    private static File[] createN(int n) throws Exception {
        return createN(n, newDefaultFileLogSet(false));
    }

    private static FileLogSet newFileLogSet(boolean rolling, File dir, String name, String ext, int max) {
        FileLogSet fls = new FileLogSet(rolling);
        assertTrue(fls.update(dir, name, ext, max));
        return fls;
    }

    private static FileLogSet newDefaultFileLogSet(boolean rolling) {
        return newFileLogSet(rolling, dir, NAME, EXT, 0);
    }

    private static FileLogSet newFileLogSet(boolean rolling, final String... dateStrings) {
        return new FileLogSet(rolling) {
            private int index;

            @Override
            protected String getDateString() {
                return dateStrings[index++];
            }
        };
    }

    private static FileLogSet newFileLogSet(boolean rolling, File directory, String name, String ext, int max, String... dateStrings) {
        FileLogSet fls = newFileLogSet(rolling, dateStrings);
        assertTrue(fls.update(dir, name, ext, max));
        return fls;
    }

    private static FileLogSet newDefaultFileLogSet(boolean rolling, final String... dateStrings) {
        return newFileLogSet(rolling, dir, NAME, EXT, 0, dateStrings);
    }

    private static String ss(String ss) {
        return "_00.00.00_00.00." + ss;
    }

    private static <T> T[] repeatN(int n, T o) {
        @SuppressWarnings("unchecked")
        T[] a = (T[]) Array.newInstance(o.getClass(), n);
        for (int i = 0; i < n; i++) {
            a[i] = o;
        }
        return a;
    }

    @Test
    public void testUpdateGetters() throws Exception {
        FileLogSet fls = newDefaultFileLogSet(false);
        assertEquals(dir, fls.getDirectory());
        assertEquals(NAME, fls.getFileName());
        assertEquals(EXT, fls.getFileExtension());
        assertEquals(0, fls.getMaxFiles());

        assertTrue(fls.update(dir2, NAME2, EXT2, 1));
        assertEquals(dir2, fls.getDirectory());
        assertEquals(NAME2, fls.getFileName());
        assertEquals(EXT2, fls.getFileExtension());
        assertEquals(1, fls.getMaxFiles());
    }

    @Test
    public void testUpdateDeleteOld() throws Exception {
        File[] files = createN(2);

        newFileLogSet(false, dir, NAME, EXT, 1);
        assertThat(files[0], not(exists()));
        assertThat(files[1], exists());
    }

    @Test
    public void testUpdateDeleteOldNaturalSort() throws Exception {
        File[] files = createN(11, newDefaultFileLogSet(false, repeatN(11, ss("00"))));

        FileLogSet fls = new FileLogSet(false);
        assertTrue(fls.update(dir, NAME, EXT, 2));
        for (int i = 0; i < 9; i++) {
            assertThat("" + i, files[i], not(exists()));
        }
        assertThat(files[9], exists());
        assertThat(files[10], exists());
    }

    @Test
    public void testUpdateDirDeleteOld() throws Exception {
        File[] files = createN(2);

        FileLogSet fls = new FileLogSet(false);
        assertTrue(fls.update(dir2, NAME, EXT, 1));
        assertTrue(fls.update(dir, NAME, EXT, 1));
        assertThat(files[0], not(exists()));
        assertThat(files[1], exists());
    }

    @Test
    public void testUpdateNameDeleteOld() throws Exception {
        File[] files = createN(2);

        FileLogSet fls = new FileLogSet(false);
        assertTrue(fls.update(dir, NAME2, EXT, 1));
        assertTrue(fls.update(dir, NAME, EXT, 1));
        assertThat(files[0], not(exists()));
        assertThat(files[1], exists());
    }

    @Test
    public void testUpdateExtDeleteOld() throws Exception {
        File[] files = createN(2);

        FileLogSet fls = new FileLogSet(false);
        assertTrue(fls.update(dir, NAME, EXT2, 1));
        assertTrue(fls.update(dir, NAME, EXT, 1));
        assertThat(files[0], not(exists()));
        assertThat(files[1], exists());
    }

    @Test
    public void testUpdateMaxDeleteOld() throws Exception {
        File[] files = createN(2);

        FileLogSet fls = new FileLogSet(false);
        assertTrue(fls.update(dir, NAME, EXT, 0));
        assertFalse(fls.update(dir, NAME, EXT, 1));
        assertThat(files[0], not(exists()));
        assertThat(files[1], exists());
    }

    @Test
    public void testCreateNewFileCounter() throws Exception {
        File[] files = createN(2, newDefaultFileLogSet(false, ss("00"), ss("00")));
        assertThat(files[0].getName(), endsWith(".0.log"));
        assertThat(files[1].getName(), endsWith(".1.log"));
    }

    @Test
    public void testUpdateCounter() throws Exception {
        createN(2, newDefaultFileLogSet(false, ss("00"), ss("00")));
        // This FLS should see the existing .0.log and .1.log
        File[] files = createN(2, newDefaultFileLogSet(false, ss("00"), ss("00")));
        assertThat(files[0].getName(), endsWith(".2.log"));
        assertThat(files[1].getName(), endsWith(".3.log"));
    }

    @Test
    public void testUpdateCounterNaturalSort() throws Exception {
        createN(11, newDefaultFileLogSet(false, repeatN(11, ss("00"))));
        // This FLS should not be confused by .10.log and .1.log.
        File[] files = createN(2, newDefaultFileLogSet(false, ss("00"), ss("00")));
        assertThat(files[0].getName(), endsWith(".11.log"));
        assertThat(files[1].getName(), endsWith(".12.log"));
    }

    @Test
    public void testCreateNewFileDeleteOld() throws Exception {
        File[] files = createN(4, newFileLogSet(false, dir, NAME, EXT, 2));
        assertThat(files[0], not(exists()));
        assertThat(files[1], not(exists()));
        assertThat(files[2], exists());
        assertThat(files[3], exists());
    }

    @Test
    public void testCreateNewFileDeleteNew() throws Exception {
        File[] filesOrig = createN(2, newDefaultFileLogSet(false, ss("01"), ss("01")));
        File[] files = createN(1, newFileLogSet(false, dir, NAME, EXT, 2, ss("00")));
        assertThat(filesOrig[0], not(exists()));
        assertThat(filesOrig[1], not(exists()));
        assertThat(files[0], exists());
    }

    @Test
    public void testCreateNewFileDeleteOldAndNew() throws Exception {
        File filePast = newDefaultFileLogSet(false, ss("00")).createNewFile();
        File[] filesPresent = createN(2, newDefaultFileLogSet(false, ss("01"), ss("01")));
        File fileFuture = newDefaultFileLogSet(false, ss("02")).createNewFile();
        FileLogSet fls = newFileLogSet(false, dir, NAME, EXT, 4, repeatN(4, ss("01")));

        File file2 = fls.createNewFile();
        assertThat(filePast, exists());
        assertThat(filesPresent[0], not(exists()));
        assertThat(filesPresent[1], not(exists()));
        assertThat(fileFuture, not(exists()));
        assertThat(file2.getName(), endsWith(".2.log"));

        File file3 = fls.createNewFile();
        assertThat(filePast, exists());
        assertThat(file2, exists());
        assertThat(file3.getName(), endsWith(".3.log"));

        File file4 = fls.createNewFile();
        assertThat(filePast, exists());
        assertThat(file2, exists());
        assertThat(file3, exists());
        assertThat(file4.getName(), endsWith(".4.log"));

        File file5 = fls.createNewFile();
        assertThat(filePast, not(exists()));
        assertThat(file2, exists());
        assertThat(file3, exists());
        assertThat(file4, exists());
        assertThat(file5.getName(), endsWith(".5.log"));
    }

    @Test
    public void testCreateNewFileResumeCounter() throws Exception {
        File[] files = createN(6, newDefaultFileLogSet(false, ss("00"), ss("01"), ss("00"), ss("01"), ss("00"), ss("01")));
        assertThat(files[0].getName(), endsWith("00.0.log"));
        assertThat(files[1].getName(), endsWith("01.0.log"));
        assertThat(files[2].getName(), endsWith("00.1.log"));
        assertThat(files[3].getName(), endsWith("01.1.log"));
        assertThat(files[4].getName(), endsWith("00.2.log"));
        assertThat(files[5].getName(), endsWith("01.2.log"));
    }
}
