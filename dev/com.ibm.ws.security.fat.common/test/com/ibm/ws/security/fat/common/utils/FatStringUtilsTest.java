/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class FatStringUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    FatStringUtils utils = new FatStringUtils();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new FatStringUtils();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** extractRegexGroup **************************************/

    /**
     * Tests:
     * - Content: null
     * - Regex: null
     * Expects:
     * - Exception should be thrown saying the regex is null
     */
    @Test
    public void test_extractRegexGroup_nullContent_nullRegex() {
        try {
            String fromContent = null;
            String regex = null;
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, regex);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "regular expression is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: null
     * Expects:
     * - Exception should be thrown saying the regex is null
     */
    @Test
    public void test_extractRegexGroup_nonEmptyContent_nullRegex() {
        try {
            String fromContent = "some content";
            String regex = null;
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, regex);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "regular expression is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Empty string
     * - Regex: Empty string
     * Expects:
     * - Exception should be thrown saying no matching groups were found
     */
    @Test
    public void test_extractRegexGroup_emptyContent_emptyRegex() {
        try {
            String fromContent = "";
            String regex = "";
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, regex);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyExceptionWithInserts(e, "Found 0 matching groups", Pattern.quote(regex), fromContent);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Empty string
     * - Regex: Non-empty
     * Expects:
     * - Exception should be thrown saying no matches were found
     */
    @Test
    public void test_extractRegexGroup_emptyContent_nonEmptyRegex() {
        try {
            String fromContent = "";
            String regex = "match me";
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, regex);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyExceptionWithInserts(e, "Did not find any matches", Pattern.quote(regex), fromContent);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: Empty string
     * Expects:
     * - Exception should be thrown saying no matching groups were found
     */
    @Test
    public void test_extractRegexGroup_nonEmptyContent_emptyRegex() {
        try {
            String fromContent = "some content";
            String regex = "";
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, regex);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyExceptionWithInserts(e, "Found 0 matching groups", Pattern.quote(regex), fromContent);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: Non-empty, no groups defined
     * Expects:
     * - Exception should be thrown saying no matching groups were found
     */
    @Test
    public void test_extractRegexGroup_nonEmptyContent_nonEmptyRegexWithoutGroups() {
        try {
            String fromContent = "some content";
            String regex = "some";
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, regex);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyExceptionWithInserts(e, "Found 0 matching groups", Pattern.quote(regex), fromContent);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: Non-empty with groups, but none will match the content
     * Expects:
     * - Exception should be thrown saying no matches were found
     */
    @Test
    public void test_extractRegexGroup_nonEmptyContent_nonEmptyRegexWithNoMatchingGroups() {
        try {
            String fromContent = "some content";
            String regex = "(match) (me)";
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, regex);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyExceptionWithInserts(e, "Did not find any matches", Pattern.quote(regex), fromContent);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: Non-empty with groups that match the content
     * Expects:
     * - Result should match the first matching group
     */
    @Test
    public void test_extractRegexGroup_nonEmptyContent_nonEmptyRegexWithMatchingGroup() {
        try {
            String fromContent = "some content";
            String regex = "(some) (content)";
            String result = FatStringUtils.extractRegexGroup(fromContent, regex);
            assertEquals("Result did not match expected value.", "some", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: Non-empty with groups that match the content
     * - Group: Negative int
     * Expects:
     * - Exception should be thrown saying the group number to extract must be non-negative
     */
    @Test
    public void test_extractRegexGroup_nonEmptyContent_nonEmptyRegex_negativeGroupNumber() {
        try {
            String fromContent = "some content";
            String regex = "(some) (content)";
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, regex, -1);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Group number.+must be non-negative");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: Non-empty with groups that match the content
     * - Group: 0
     * Expects:
     * - Result should match the segment of the original content that matches the entire regex (not just one of the groups)
     */
    @Test
    public void test_extractRegexGroup_nonEmptyContent_nonEmptyRegex_groupZero() {
        try {
            String fromContent = "some content";
            String regex = "(s)(o)([a-z]{2})";
            String result = FatStringUtils.extractRegexGroup(fromContent, regex, 0);
            assertEquals("Result did not match expected value.", "some", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: Non-empty with groups that match the content
     * - Group: 1
     * Expects:
     * - Result should match the first matching group
     */
    @Test
    public void test_extractRegexGroup_nonEmptyContent_nonEmptyRegex_groupOne() {
        try {
            String fromContent = "some content";
            String regex = "(s)(o)([a-z]{2})";
            String result = FatStringUtils.extractRegexGroup(fromContent, regex, 1);
            assertEquals("Result did not match expected value.", "s", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: Non-empty with groups that match the content
     * - Group: 10
     * Expects:
     * - Exception should be thrown saying fewer matching groups were found than expected
     */
    @Test
    public void test_extractRegexGroup_nonEmptyContent_nonEmptyRegex_groupNumberLarge() {
        try {
            String fromContent = "some content";
            String regex = "(some)";
            int groupNumber = 10;
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, regex, groupNumber);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyExceptionWithInserts(e, "Found 1 matching groups.+expected.+" + groupNumber, Pattern.quote(regex), fromContent);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: Custom pattern that ignores casing
     * Expects:
     * - Result should match the matching group since case insensitivity is turned on
     */
    @Test
    public void test_extractRegexGroup_customPattern_caseInsensitive() {
        try {
            String matchingString = "Text-Only";
            String fromContent = "Some " + matchingString + " Content Is Presented Here.";
            String regex = "([a-z-]+) content";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

            String result = FatStringUtils.extractRegexGroup(fromContent, pattern);
            assertEquals("Result did not match the expected value.", matchingString, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: null
     * - Regex: Custom pattern
     * Expects:
     * - Exception should be thrown saying the content string is null
     */
    @Test
    public void test_extractRegexGroup_customPattern_nullContent() {
        try {
            String fromContent = null;
            Pattern pattern = Pattern.compile("[a-z]");
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, pattern);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "content string is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty
     * - Regex: null
     * Expects:
     * - Exception should be thrown saying the regex is null
     */
    @Test
    public void test_extractRegexGroup_customPattern_nullPattern() {
        try {
            String fromContent = "Some Text-Only Content Is Presented Here.";
            Pattern pattern = null;
            try {
                String result = FatStringUtils.extractRegexGroup(fromContent, pattern);
                fail("Should have thrown an exception but got result: [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "regular expression is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content: Non-empty, multiline string
     * - Regex: Custom pattern that spans lines
     * Expects:
     * - Result should match the matching group that spans lines
     */
    @Test
    public void test_extractRegexGroup_customPattern_multipleLines() {
        try {
            String headHtml = "\n\r<title>\n\rPage Title\n\r</title>\n\r";
            String fromContent = "<html>\n\r<head>" + headHtml + "</head>\n\r<body>Hello, world!</body></html>";
            String regex = "<head>(.+)</head>";
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

            String result = FatStringUtils.extractRegexGroup(fromContent, pattern);
            assertEquals("Result did not match the expected value.", headHtml, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
