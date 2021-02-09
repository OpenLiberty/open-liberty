/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.infra.depchain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class SingleBucketTest {

    static final String SERVLET_FAT = "test-resources/fat-using-servlet.json";
    static final String JEE_FAT = "test-resources/fat-using-javaee.json";
    static final String ALL_FEATURES = "test-resources/fat-using-all-features.json";
    static final String NO_FEATURES = "test-resources/fat-using-no-features.json";

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setup() throws Exception {
        ChangeDetectorTest.extractLibertySnapshot();
    }

    @Before
    public void beforeEach() {
        System.out.println("### " + testName.getMethodName());
    }

    @Test
    public void testModifySingleFAT() throws Exception {
        Set<String> changedFiles = set("dev/com.ibm.ws.example_fat/fat/src/Foo.java");

        assertFATShouldRun(true, "com.ibm.ws.example_fat", SERVLET_FAT, changedFiles);
        assertFATShouldRun(false, "com.ibm.ws.another_fat", SERVLET_FAT, changedFiles);
    }

    @Test
    public void testModifyTwoFATs() throws Exception {
        Set<String> changedFiles = set("dev/com.ibm.ws.bucket1_fat/fat/src/Foo.java",
                                       "dev/com.ibm.ws.bucket2_fat/fat/src/Foo.java");

        assertFATShouldRun(true, "com.ibm.ws.bucket1_fat", SERVLET_FAT, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.bucket2_fat", SERVLET_FAT, changedFiles);
        assertFATShouldRun(false, "com.ibm.ws.another_fat", SERVLET_FAT, changedFiles);
        assertFATShouldRun(false, "com.ibm.ws.another2_fat", ALL_FEATURES, changedFiles);
    }

    @Test
    public void testModifyServlet() throws Exception {
        Set<String> changedFiles = set("dev/com.ibm.ws.webcontainer/src/Foo.java");

        assertFATShouldRun(true, "com.ibm.ws.bucket1_fat", SERVLET_FAT, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.bucket2_fat", JEE_FAT, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.bucket3_fat", ALL_FEATURES, changedFiles);
        assertFATShouldRun(false, "com.ibm.ws.another_fat", NO_FEATURES, changedFiles);
    }

    @Test
    public void testModifyJDBC() throws Exception {
        Set<String> changedFiles = set("dev/com.ibm.ws.jdbc/src/Foo.java");

        assertFATShouldRun(false, "com.ibm.ws.bucket1_fat", SERVLET_FAT, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.bucket2_fat", JEE_FAT, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.bucket3_fat", ALL_FEATURES, changedFiles);
        assertFATShouldRun(false, "com.ibm.ws.another_fat", NO_FEATURES, changedFiles);
    }

    @Test
    public void testModifyUnknown() throws Exception {
        Set<String> changedFiles = set("bogus");

        assertFATShouldRun(true, "com.ibm.ws.bucket1_fat", SERVLET_FAT, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.bucket2_fat", JEE_FAT, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.bucket3_fat", ALL_FEATURES, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.another_fat", NO_FEATURES, changedFiles);
    }

    @Test
    public void testModifyInfra() throws Exception {
        Set<String> changedFiles = set("dev/fattest.simplicity/foo.txt");

        assertFATShouldRun(true, "com.ibm.ws.bucket1_fat", SERVLET_FAT, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.bucket2_fat", JEE_FAT, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.bucket3_fat", ALL_FEATURES, changedFiles);
        assertFATShouldRun(true, "com.ibm.ws.another_fat", NO_FEATURES, changedFiles);
    }

    private static void assertFATShouldRun(boolean shouldRun, String fatName, String fatDeps, Set<String> changedFiles) throws Exception {
        if (shouldRun) {
            assertTrue("FAT bucket " + fatName + " SHOULD run with changed files: " + changedFiles,
                       detector(fatDeps, fatName).shouldFatRun(changedFiles));
        } else {
            assertFalse("FAT bucket " + fatName + " should NOT run with changed files: " + changedFiles,
                        detector(fatDeps, fatName).shouldFatRun(changedFiles));
        }
    }

    private static Set<String> set(String... strings) {
        Set<String> set = new TreeSet<>();
        for (String string : strings)
            set.add(string);
        return set;
    }

//    args '--wlp', project(':build.image').projectDir.toString() + '/wlp',
//    '--git-diff', System.getProperty('git_diff', 'UNSET'),
//    '--deps', autoFvtDir.toString() + '/fat-metadata.json',
//    '--fat-name', project.name,
//    '--output', autoFvtDir.toString() + '/canSkipFat'

    static MockChangeDetector detector(String depsPath, String fatName) {
        MainArgs args = new MainArgs(new String[] {
                                                    "--wlp", ChangeDetectorTest.WLP_DIR,
                                                    "--git-diff", "bogus",
                                                    "--deps", depsPath,
                                                    "--fat-name", fatName
        });
        return new MockChangeDetector(args);
    }

    private static class MockChangeDetector extends ChangeDetector {

        MockChangeDetector(MainArgs args) {
            super(args);
        }

        @Override
        Set<String> getModifiedFilesFromDiff(String ignore) throws IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

    }

}
