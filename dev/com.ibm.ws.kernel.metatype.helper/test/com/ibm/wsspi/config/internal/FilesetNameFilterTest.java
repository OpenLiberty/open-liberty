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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.config.internal.FilesetImpl;
import com.ibm.wsspi.config.internal.FilesetImpl.FilterType;

/**
 * The fileset configuration element uses include and exclude filters to provide
 * a set of file names. This class provides tests to check the behaviour of the
 * filters is correct for many different cases.
 * 
 * The implementation of the {@link FilenameFilter} is a private class inside
 * the {@link FilesetImpl} so reflection is used to access the methods required
 * to test them, primarily the accept method.
 */
public class FilesetNameFilterTest extends AbstractFilesetTestHelper {

    // the basedir we will use
    private static final File DIR = new File("test" + File.separator + "path");
    // a sub directory of basedir
    private static final File SUB_DIR = new File(DIR, "sub");
    // another level of sub directory
    private static final File SUB2_DIR = new File(SUB_DIR, "sub2");
    // an alternate dir to test files from a different dir
    private static final File ALT_DIR = new File("some" + File.separator + "otherpath");

    // create a Fileset with the default attributes as we will set them as
    // required in the tests
    static FilesetImpl fset = new FilesetImpl();

    // The reflective filter which delegates to the real (private) object
    // inside the FilesetImpl
    private static ReflectiveFilter filter;

    private final Mockery context = new JUnit4Mockery();
    private ComponentContext mockComponentContext;

    @BeforeClass
    public static void setupReflectiveFields() throws Exception {
        Field filterField = fset.getClass().getDeclaredField("filter");
        filterField.setAccessible(true);
        filter = new ReflectiveFilter(filterField.get(fset));

    }

    @Before
    public void setup() throws Exception {
        setLocationService(fset);
        // clear the filters before each test
        filter.clearFilters();
        // set the dir on the fileset
        mockComponentContext = context.mock(ComponentContext.class);
        fset.activate(mockComponentContext, getAttributes(DIR.toString(), null, null, null, null));

        final BundleContext mockBundleContext = context.mock(BundleContext.class);
        context.checking(new Expectations() {
            {
                allowing(mockComponentContext).getBundleContext();
                will(returnValue(mockBundleContext));
                allowing(mockComponentContext).getProperties();
                ignoring(mockBundleContext);
            }
        });

    }

    @Test
    public void testDefaultAccept() throws Exception {
        // the default include is "*" which applies to anything in the same
        // directory, we have to add it here even though it is the default,
        // because we are just testing a filter object not a Fileset one
        filter.addFilter(FilterType.INCLUDE, "*");
        assertTrue(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(DIR, "testFile2"));
        assertTrue(filter.accept(DIR, "anything"));
        // if the directory is different it shouldn't be included
        assertFalse(filter.accept(ALT_DIR, "anything"));
        // even a sub folder shouldn't be accepted
        assertFalse(filter.accept(SUB_DIR, "anything"));
    }

    @Test
    public void testDotEscape() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "testFile.txt");
        // check that we get true for a file name with a .
        assertTrue(filter.accept(DIR, "testFile.txt"));
        // check that we get false for a file name with another
        // character in place of the . (because regex . is any character)
        assertFalse(filter.accept(DIR, "testFile,txt"));
    }

    @Test
    public void testCaseSensitiveDefault() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "testfile");
        // lower case OK
        assertTrue(filter.accept(DIR, "testfile"));
        // upper case F should fail
        assertFalse(filter.accept(DIR, "testFile"));
    }

    @Test
    public void testCaseInsensitive() throws Exception {
        try {
            setAttributes(fset, DIR.toString(), false, null, null);
            filter.addFilter(FilterType.INCLUDE, "testfile");
            // lower case OK
            assertTrue(filter.accept(DIR, "testfile"));
            // upper case F should pass
            assertTrue(filter.accept(DIR, "testFile"));
        } finally {
            // return back to the default for other tests
            setAttributes(fset, DIR.toString(), true, null, null);
        }
    }

    @Test
    public void testWildcardIncludeAccept() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "*");
        // should get anything in the dir
        assertTrue(filter.accept(DIR, "testFile"));
        // should not get a file from the sub dir
        assertFalse(filter.accept(SUB_DIR, "testFile"));
    }

    @Test
    public void testNameIncludeAccept() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "testFile");
        assertTrue(filter.accept(DIR, "testFile"));
    }

    @Test
    public void testFullpathIncludeAccept() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "sub/testFile");
        assertTrue(filter.accept(SUB_DIR, "testFile"));
    }

    @Test
    public void testWildcardExcludeAccept() throws Exception {
        filter.addFilter(FilterType.EXCLUDE, "*");
        assertFalse(filter.accept(DIR, "testFile"));
    }

    @Test
    public void testNameExcludeAccept() throws Exception {
        filter.addFilter(FilterType.EXCLUDE, "testFile");
        assertFalse(filter.accept(DIR, "testFile"));
    }

    @Test
    public void testFullpathExcludeAccept() throws Exception {
        filter.addFilter(FilterType.EXCLUDE, "sub/testFile");
        assertFalse(filter.accept(SUB_DIR, "testFile"));
    }

    @Test
    public void testSpaceSeparatedAccept() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "testFile testFile2");
        assertTrue(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(DIR, "testFile2"));
    }

    @Test
    public void testCommaSeparatedAccept() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "testFile,testFile2");
        assertTrue(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(DIR, "testFile2"));
    }

    @Test
    public void testCommaAndSpaceSeparatedAccept() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "testFile , testFile2");
        assertTrue(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(DIR, "testFile2"));
    }

    @Test
    public void testCommaAndSpaceSeparatedAccept2() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "testFile    ,  testFile2");
        assertTrue(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(DIR, "testFile2"));
    }

    @Test
    public void testExcessSpacesAccept() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "     testFile         ");
        assertTrue(filter.accept(DIR, "testFile"));
    }

    @Test
    public void testIncludeAndExcludeAccept() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "testFile");
        filter.addFilter(FilterType.EXCLUDE, "testFile2");
        assertTrue(filter.accept(DIR, "testFile"));
        assertFalse(filter.accept(DIR, "testFile2"));
    }

    @Test
    public void testNameEmbeddedWildCard() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "test*");
        assertTrue(filter.accept(DIR, "testFile"));
        assertFalse(filter.accept(DIR, "filetestFile"));
        assertFalse(filter.accept(DIR, ""));
    }

    @Test
    public void testNameLeadingWildCard() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "*TestFile");
        assertTrue(filter.accept(DIR, "fileTestFile"));
        // require at least one character for star
        assertFalse(filter.accept(DIR, "TestFile"));
        // check that path boundaries aren't crossed
        assertFalse(filter.accept(SUB_DIR, "fileTestFile"));
        assertFalse(filter.accept(SUB2_DIR, "fileTestFile"));
        assertFalse(filter.accept(DIR, "filetestFile"));
    }

    @Test
    public void testPathEmbeddedWildCard() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "./*/testFile");
        // /test/path/testFile (will be false because there is no folder between
        // . and testFile
        assertFalse(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(SUB_DIR, "testFile"));
        assertFalse(filter.accept(ALT_DIR, "testFile"));
        assertFalse(filter.accept(SUB2_DIR, "testFile"));
    }

    @Test
    public void testLeadingPathEmbeddedWildCard() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "*/testFile");
        assertFalse(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(SUB_DIR, "testFile"));
        assertFalse(filter.accept(ALT_DIR, "testFile"));
        assertFalse(filter.accept(SUB2_DIR, "testFile"));
    }

    @Test
    public void testMultiplePathEmbeddedWildCards() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "./*/*/testFile");
        assertFalse(filter.accept(DIR, "testFile"));
        assertFalse(filter.accept(SUB_DIR, "testFile"));
        assertFalse(filter.accept(ALT_DIR, "testFile"));
        assertTrue(filter.accept(SUB2_DIR, "testFile"));
    }

    @Test
    public void testPathAndNameEmbeddedWildCards() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "./*/test*");
        assertFalse(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(SUB_DIR, "testFile"));
        assertFalse(filter.accept(DIR, "filetestFile"));
        assertFalse(filter.accept(SUB_DIR, "filetestFile"));
    }

    @Test
    public void testRecursiveWildCard() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "./**/testFile");
        assertFalse(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(SUB_DIR, "testFile"));
        assertTrue(filter.accept(SUB2_DIR, "testFile"));
        assertFalse(filter.accept(ALT_DIR, "testFile"));
    }

    @Test
    public void testLeadingRecursiveWildCard() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "**/testFile");
        assertFalse(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(SUB_DIR, "testFile"));
        assertTrue(filter.accept(SUB2_DIR, "testFile"));
        assertFalse(filter.accept(ALT_DIR, "testFile"));
    }

    @Test
    public void testOnlyRecursiveWildCard() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "**");
        assertTrue(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(SUB_DIR, "testFile"));
        assertTrue(filter.accept(SUB2_DIR, "testFile"));
        assertFalse(filter.accept(ALT_DIR, "testFile"));
    }

    @Test
    public void testComplexWildCards() throws Exception {
        filter.addFilter(FilterType.INCLUDE, "**/some/**/path/*/*test*.*");
        assertFalse(filter.accept(DIR, "testFile"));
        assertTrue(filter.accept(new File("test/path/sub1/sub2/some/sub3/sub4/sub5/path/dir"), "AtestfileB.txt"));
        assertFalse(filter.accept(new File("test/path/sub1/sub2/some/sub3/sub4/sub5/path/dir"), "testfileB"));
        assertFalse(filter.accept(new File("test/path/sub1/sub2/some/sub3/sub4/sub5/path/dir"), "AtestfileB"));
        assertFalse(filter.accept(new File("some/path/dir"), "AtestfileB.txt"));
        assertFalse(filter.accept(new File("test/path/sub1/sub2/some/sub3/sub4/sub5/path"), "AtestfileB.txt"));
    }

    /**
     * This class uses reflection to delegate to the real filter Object which is
     * privately constructed and used inside the FilesetImpl.
     * 
     */
    private static final class ReflectiveFilter {
        private final Object realFilter;
        private final Method clearFiltersMethod;
        private final Method addFilterMethod;
        private final Method acceptMethod;

        ReflectiveFilter(Object realFilter) throws SecurityException, NoSuchMethodException {
            this.realFilter = realFilter;
            clearFiltersMethod = realFilter.getClass().getDeclaredMethod("clearFilters", new Class[0]);
            clearFiltersMethod.setAccessible(true);
            addFilterMethod = realFilter.getClass().getDeclaredMethod("addFilter", FilterType.class, String.class);
            addFilterMethod.setAccessible(true);
            acceptMethod = realFilter.getClass().getDeclaredMethod("accept", File.class, String.class);
            acceptMethod.setAccessible(true);

        }

        public void clearFilters() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            clearFiltersMethod.invoke(realFilter, new Object[0]);
        }

        public void addFilter(FilterType type, String filter) throws IllegalArgumentException, IllegalAccessException,
                        InvocationTargetException {
            addFilterMethod.invoke(realFilter, type, filter);
        }

        public boolean accept(File dir, String name) throws IllegalArgumentException, IllegalAccessException,
                        InvocationTargetException {
            return (Boolean) acceptMethod.invoke(realFilter, dir, name);
        }
    }
}
