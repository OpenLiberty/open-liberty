/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.config.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.config.internal.FilesetImpl;

public class FilesetTest extends AbstractFilesetTestHelper {

    // the basedir we will use
    private static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    private static final String DIR_NAME = testBuildDir + "/tmp/filesetTest";
    private static final File DIR = new File(DIR_NAME);
    private static final String NODIR_NAME = testBuildDir + "/tmp/notADir";
    private static final File TEST_FILE = new File(DIR, "testfile.txt");
    private static final File TEST_FILE_2 = new File(DIR, "testfile2");
    private static final Collection<File> TEST_FILES = new ArrayList<File>();
    private static final Collection<File> EMPTY_FILESET = Collections.emptySet();

    static {
        TEST_FILES.add(TEST_FILE);
        TEST_FILES.add(TEST_FILE_2);
    }

    // create a Fileset with no attributes as we will set them as required in
    // the tests
    FilesetImpl fset;

    private final Mockery context = new JUnit4Mockery();
    private ComponentContext mockComponentContext;

    private static void recursiveDelete(File dir) {
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    recursiveDelete(f);
                } else {
                    f.delete();
                }
            }
            dir.delete();
        }
    }

    @BeforeClass
    public static void setUpFiles() throws Exception {
        // clean up first, just in case clean up failed
        // or was interrupted previously
        cleanUpFiles();
        // set up a directory with some test files in
        DIR.mkdirs();
        for (File test_file : TEST_FILES) {
            test_file.createNewFile();
        }
    }

    @AfterClass
    public static void cleanUpFiles() throws Exception {
        // recursively remove the test directory
        recursiveDelete(DIR);
    }

    @Before
    public void setUp() throws Exception {
        fset = new FilesetImpl();
        setLocationService(fset);
        mockComponentContext = context.mock(ComponentContext.class);
        final BundleContext mockBundleContext = context.mock(BundleContext.class);
        context.checking(new Expectations() {
            {
                allowing(mockComponentContext).getBundleContext();
                will(returnValue(mockBundleContext));
                allowing(mockComponentContext).getProperties();
                ignoring(mockBundleContext);
            }
        });

        fset.activate(mockComponentContext, getAttributes(DIR_NAME, null, null, null, null));
        setAttributes(fset, DIR_NAME, null, null, null);
//        fset.initComplete(Collections.singleton(DIR));
    }

    private String getFilterFromCollection(Collection<File> collection) {
        Collection<String> names = new ArrayList<String>(collection.size());
        for (File f : collection) {
            names.add(f.getName());
        }

        // turn the names collection into a ", " separated String
        String filter = names.toString();
        // trim the [ and ] from the String
        filter = filter.substring(1, filter.length() - 1);
        return filter;
    }

    private Collection<String> getResolvedAndSortedNames(Collection<File> files) throws Exception {
        List<String> fileAbsolutePaths = new ArrayList<String>(files.size());
        for (File f : files) {
            fileAbsolutePaths.add(f.getCanonicalPath());
        }
        Collections.sort(fileAbsolutePaths);
        return fileAbsolutePaths;
    }

    private void assertFilesetsEqual(String msg, File expected, Collection<File> actual) throws Exception {
        assertFilesetsEqual(msg, Arrays.asList(new File[] { expected }), actual);
    }

    private void assertFilesetsEqual(String msg, Collection<File> expected, Collection<File> actual) throws Exception {
        assertEquals("Unexpected files returned", getResolvedAndSortedNames(expected),
                     getResolvedAndSortedNames(actual));
    }

    @Test
    public void testSetDir() throws Exception {
        // check that setDir has an effect and returns the expected files
        setAttributes(fset, DIR_NAME, null, null, null);
        assertFilesetsEqual("Unexpected files returned", TEST_FILES, fset.getFileset());
        // check that changing setDir changes the returned collection
        setAttributes(fset, NODIR_NAME, null, null, null);
        assertFilesetsEqual("Unexpected files returned", EMPTY_FILESET, fset.getFileset());
    }

    @Test
    public void testSetCaseSensitive() throws Exception {
        // the default filter is *, which isn't going to be case sensitive
        // either way
        // so set the includes filter to be the list of TEST_FILES
        setAttributes(fset, DIR_NAME, true, getFilterFromCollection(TEST_FILES), null);
        // check that it works as is
        assertFilesetsEqual("Unexpected files returned", TEST_FILES, fset.getFileset());
        Collection<File> wrongcaseFiles = Arrays.asList(new File[] { new File(DIR, "testFile.txt"),
                                                                    new File(DIR, "testFile2") });
        // apply the wrong case filter
        setAttributes(fset, DIR_NAME, true, getFilterFromCollection(wrongcaseFiles), null);
        // check that no files come back for the wrong case
        assertFilesetsEqual("Was not case sensitive when should have been", EMPTY_FILESET, fset.getFileset());
        // make it insensitive and check result
        setAttributes(fset, DIR_NAME, false, getFilterFromCollection(wrongcaseFiles), null);
        assertFilesetsEqual("Was case sensitive when should not have been", TEST_FILES, fset.getFileset());
        // return to sensitive and check
        setAttributes(fset, DIR_NAME, true, getFilterFromCollection(wrongcaseFiles), null);
        assertFilesetsEqual("Was not case sensitive when should have been", EMPTY_FILESET, fset.getFileset());
    }

    @Test
    public void testSetIncludesAttribute() throws Exception {
        setAttributes(fset, DIR_NAME, null, TEST_FILE.getName(), null);
        assertFilesetsEqual("Inlcude filter was incorrectly applied", TEST_FILE, fset.getFileset());
    }

    @Test
    public void testSetExcludesAttribute() throws Exception {
        setAttributes(fset, DIR_NAME, null, null, TEST_FILE.getName());
        assertFilesetsEqual("Exlcude filter was incorrectly applied", TEST_FILE_2, fset.getFileset());
    }

    @Test
    public void testGetFileset() throws Exception {
        Collection<File> files = fset.getFileset();
        assertNotNull(files);
        assertFilesetsEqual("Unexpected files returned", TEST_FILES, fset.getFileset());
    }

    @Test
    public void testNotifyFileChanged() throws Exception {
        // check that nothing is returned for testfile3
        setAttributes(fset, DIR_NAME, null, "testfile3", null);
        Collection<File> retrieved = fset.getFileset();
        assertFilesetsEqual("No files should have been returned", EMPTY_FILESET, retrieved);
        // change the contents of the DIR and notify that it was changed
        File testFile3 = new File(DIR, "testfile3");
        testFile3.createNewFile();
        // when the disk contents change the FileMonitor service will call
        // scanComplete(...)
        // mock that call here with a new listing of the files in the DIR and
        // remove the existing retrieved files
        fset.onChange(recursivelyListFiles(new File(DIR_NAME)), retrieved, EMPTY_FILESET);
        // check that the fileset now contains the new file
        /*
         * assertNotSame("The cached fileset should have been refreshed",
         * EMPTY_FILESET, fset.getFileset());
         */
        retrieved = fset.getFileset();
        assertFilesetsEqual("The new file should have been returned", testFile3, retrieved);

        testFile3.setLastModified(testFile3.lastModified() + 5000);
        fset.onChange(EMPTY_FILESET, retrieved, EMPTY_FILESET);
        retrieved = fset.getFileset();
        assertFilesetsEqual("The same file should have been returned", testFile3, retrieved);

        testFile3.delete();
        fset.onChange(EMPTY_FILESET, EMPTY_FILESET, retrieved);
        retrieved = fset.getFileset();
        assertFilesetsEqual("No files should have been returned", EMPTY_FILESET, retrieved);
    }

    @Test
    public void testRecursion() throws Exception {
        try {
            File sub = new File(DIR, "some/sub/dir");
            sub.mkdirs();
            File subFile = new File(sub, "subfile.txt");
            subFile.createNewFile();

            setAttributes(fset, DIR_NAME, null, "**/subfile.txt", null);

            assertFilesetsEqual("The sub directory file should have been returned", subFile, fset.getFileset());

        } finally {
            // delete the file we made so it doesn't influence
            // other tests
            recursiveDelete(new File(DIR, "some"));
        }
    }

    @Test
    public void verifyProperties() {
        setAttributes(fset, DIR_NAME, null, null, null, 0L);
        setAttributes(fset, DIR_NAME, null, null, null, 1L);
        setAttributes(fset, DIR_NAME, null, null, null, -5L);
    }
}
