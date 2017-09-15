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
package com.ibm.ws.webcontainer.security.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollectionImpl;

/**
 *
 */
@SuppressWarnings("unchecked")
public class SecurityConstraintCollectionTest {
    private static final String STANDARD_GET = "GET";
    private static final String CUSTOM = "CUSTOM";
    private static final String CUSTOM_NOT_LISTED = "NOTLISTED";
    private static SharedOutputManager outputMgr;
    private static List<String> testRoles = Arrays.asList("tester");
    private static List<String> developerRoles = Arrays.asList("developer");
    private static String ONE_EXACT_URL = "/oneExactURL/oneExactURL.oneExactURL";
    private static String ONE_PATH_URL_PATTERN = "/onePathURL/*";
    private static String ONE_PATH_URL = "/onePathURL/match";
    private static String PATH_NESTED_URL = "/twoPathURL/nested/morenested/match";
    private static String PATH_NESTED_URL_PATTERN = "/twoPathURL/nested/*";
    private static String PATH_LONGEST_NESTED_URL_PATTERN = "/twoPathURL/nested/morenested/*";
    private static String ONE_EXT_URL_PATTERN = "*.oneExtension";
    private static String ONE_EXT_URL = "matches.oneExtension";
    private final String[] oneStandardMethod = { STANDARD_GET };
    private final String[] oneCustomMethod = { "CUSTOM" };
    private final List<String> emptyRoles = Collections.EMPTY_LIST;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            List<SecurityConstraint> securityConstraints = new ArrayList<SecurityConstraint>();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            assertNotNull("There must be a security constraint collection.", securityConstraintCollection);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetConstraintMatch() {
        final String methodName = "testGetConstraintMatch";
        try {
            String resourceName = "test.jsp";
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = new ArrayList<SecurityConstraint>();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertNotNull("There must be a default security constraint match.", matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAddSecurityConstraints() {
        List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXACT_URL, oneStandardMethod, testRoles);
        SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
        List<SecurityConstraint> moreSecurityConstraints = createSecurityConstraints(ONE_PATH_URL_PATTERN, oneStandardMethod, testRoles);

        securityConstraintCollection.addSecurityConstraints(moreSecurityConstraints);
        List<SecurityConstraint> expectedConstraints = new ArrayList<SecurityConstraint>();
        expectedConstraints.addAll(securityConstraints);
        expectedConstraints.addAll(moreSecurityConstraints);
        assertTrue("The security constraints should get updated after adding new constraints.",
                   securityConstraintCollection.getSecurityConstraints().containsAll(expectedConstraints));
    }

    @Test
    public void testGetMatchResponse_Standard_Exact() {
        final String methodName = "testGetConstraintMatch_Standard_Exact";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXACT_URL, oneStandardMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_Exact_PrecludedMatchTakesPrecedence() {
        final String methodName = "testGetConstraintMatch_Standard_Exact_PrecludedMatchTakesPrecedence";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSameOnePrecluded(ONE_EXACT_URL, oneStandardMethod, testRoles, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertTrue("There must be a match with access precluded.", matchResponse.isAccessPrecluded());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_Exact_NoRolesMatchTakesNextPrecedence() {
        final String methodName = "testGetConstraintMatch_Standard_Exact_NoRolesMatchTakesNextPrecedence";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXACT_URL, oneStandardMethod, emptyRoles, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertTrue("There must be a match with no roles.", actualRoles.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_MultipleExact_RolesAreCombined() {
        final String methodName = "testGetConstraintMatch_Standard_MultipleExact_RolesAreCombined";
        try {
            List<String> expectedCombinedRoles = createExpectedCombinedRoles();
            String resourceName = ONE_EXACT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXACT_URL, oneStandardMethod, testRoles, developerRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertEquals("There must be a match with combined roles.", expectedCombinedRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_Path() {
        final String methodName = "testGetConstraintMatch_Standard_Path";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_PATH_URL_PATTERN, oneStandardMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_PATH_URL, STANDARD_GET);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_Path_PrecludedMatchTakesFirstPrecedence() {
        final String methodName = "testGetConstraintMatch_Standard_Extension_PrecludedMatchFirstPrecedence";
        try {
            String resourceName = ONE_PATH_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSameOnePrecluded(ONE_PATH_URL_PATTERN, oneStandardMethod, testRoles, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertTrue("There must be a match with access precluded.", matchResponse.isAccessPrecluded());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_Path_NoRolesMatchTakesSecondPrecedence() {
        final String methodName = "testGetConstraintMatch_Standard_Extension_NoRolesMatchTakesSecondPrecedence";
        try {
            String resourceName = ONE_PATH_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_PATH_URL_PATTERN, oneStandardMethod, emptyRoles, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertTrue("There must be a match with no roles.", actualRoles.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_MultipleSamePaths_RolesAreCombined() {
        final String methodName = "testGetConstraintMatch_Standard_MultipleExact_RolesAreCombined";
        try {
            List<String> expectedCombinedRoles = createExpectedCombinedRoles();
            String resourceName = ONE_PATH_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_PATH_URL_PATTERN, oneStandardMethod, testRoles, developerRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertEquals("There must be a match with combined roles.", expectedCombinedRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_LongestPath_Overrides() {
        final String methodName = "testGetMatchResponse_Standard_LongestPath_Overrides";
        try {
            String resourceName = PATH_NESTED_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = createSecurityConstraintsStandardThreePathWithDifferentRoles();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertEquals("There must be a match with developer roles.", developerRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_Extension_PrecludedMatchTakesFirstPrecedence() {
        final String methodName = "testGetConstraintMatch_Standard_Extension_PrecludedMatchFirstPrecedence";
        try {
            String resourceName = ONE_EXT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSameOnePrecluded(ONE_EXT_URL_PATTERN, oneStandardMethod, testRoles, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertTrue("There must be a match with access precluded.", matchResponse.isAccessPrecluded());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_Extension_NoRolesMatchTakesSecondPrecedence() {
        final String methodName = "testGetConstraintMatch_Standard_Extension_NoRolesMatchTakesSecondPrecedence";
        try {
            String resourceName = ONE_EXT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXT_URL_PATTERN, oneStandardMethod, emptyRoles, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertTrue("There must be a match with no roles.", actualRoles.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Standard_MultipleSameExtension_RolesAreCombined() {
        final String methodName = "testGetConstraintMatch_Standard_MultipleExact_RolesAreCombined";
        try {
            List<String> expectedCombinedRoles = createExpectedCombinedRoles();
            String resourceName = ONE_EXT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXT_URL_PATTERN, oneStandardMethod, testRoles, developerRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertEquals("There must be a match with combined roles.", expectedCombinedRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Custom_Exact() {
        final String methodName = "testGetMatchResponse_Custom_Exact";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = "CUSTOM";
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXACT_URL, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Custom_Path() {
        final String methodName = "testGetMatchResponse_Custom_Path";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_PATH_URL_PATTERN, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_PATH_URL, CUSTOM);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Custom_Extension() {
        final String methodName = "testGetMatchResponse_Custom_Extension";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXT_URL_PATTERN, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_EXT_URL, CUSTOM);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_Exact() {
        final String methodName = "testGetMatchResponse_CustomNotListed_Exact";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = CUSTOM_NOT_LISTED;
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXACT_URL, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_Exact_Precluded() {
        final String methodName = "testGetMatchResponse_CustomNotListed_Exact_Precluded";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = CUSTOM_NOT_LISTED;
            List<SecurityConstraint> securityConstraints = constraintsTwoSameOnePrecluded(ONE_EXACT_URL, oneCustomMethod, testRoles, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_Exact_Roles() {
        final String methodName = "testGetMatchResponse_CustomNotListed_Exact_Roles";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = CUSTOM_NOT_LISTED;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXACT_URL, oneCustomMethod, emptyRoles, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_MultipleExact() {
        final String methodName = "testGetConstraintMatch_Standard_MultipleExact";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = CUSTOM_NOT_LISTED;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXACT_URL, oneCustomMethod, testRoles, developerRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(resourceName, method);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_Path() {
        final String methodName = "testGetMatchResponse_CustomNotListed_Path";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_PATH_URL_PATTERN, oneStandardMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_PATH_URL, CUSTOM_NOT_LISTED);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_Path_Precluded() {
        final String methodName = "testGetMatchResponse_CustomNotListed_Path_Precluded";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoSameOnePrecluded(ONE_PATH_URL_PATTERN, oneCustomMethod, testRoles, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_PATH_URL, CUSTOM_NOT_LISTED);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_Path_Roles() {
        final String methodName = "testGetMatchResponse_CustomNotListed_Path_Roles";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_PATH_URL_PATTERN, oneCustomMethod, emptyRoles, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_PATH_URL, CUSTOM_NOT_LISTED);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_MultiplePathSamePaths() {
        final String methodName = "testGetMatchResponse_CustomNotListed_MultiplePathSamePaths";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_PATH_URL_PATTERN, oneCustomMethod, testRoles, developerRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_PATH_URL, CUSTOM_NOT_LISTED);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_LongestPath() {
        final String methodName = "testGetMatchResponse_CustomNotListed_LongestPath";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraintsStandardThreePathWithDifferentRoles();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(PATH_NESTED_URL, CUSTOM_NOT_LISTED);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_Extension_Precluded() {
        final String methodName = "testGetMatchResponse_CustomNotListed_Extension_Precluded";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoSameOnePrecluded(ONE_EXT_URL_PATTERN, oneStandardMethod, testRoles, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_EXT_URL, CUSTOM_NOT_LISTED);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_Extension() {
        final String methodName = "testGetMatchResponse_CustomNotListed_Extension";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXT_URL_PATTERN, oneStandardMethod, emptyRoles, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_EXT_URL, CUSTOM_NOT_LISTED);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_CustomNotListed_MultipleSameExtension() {
        final String methodName = "testGetMatchResponse_CustomNotListed_MultipleSameExtension";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXT_URL_PATTERN, oneStandardMethod, testRoles, developerRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_EXT_URL, CUSTOM_NOT_LISTED);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Exact_NoSSLTakesPrecedence() {
        final String methodName = "testGetMatchResponse_Exact_NoSSLTakesPrecedence";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoWithDifferentSSL(ONE_EXACT_URL, oneStandardMethod, false, true);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_EXACT_URL, STANDARD_GET);

            assertFalse("SSL must not be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Path_SSLTakesPrecedence() {
        final String methodName = "testGetMatchResponse_Path_SSLTakesPrecedence";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoWithDifferentSSL(ONE_PATH_URL_PATTERN, oneStandardMethod, false, true);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_PATH_URL, STANDARD_GET);

            assertTrue("SSL must be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Path_SSLRequired() {
        final String methodName = "testGetMatchResponse_Path_SSLRequired";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoWithDifferentSSL(ONE_PATH_URL_PATTERN, oneStandardMethod, true, true);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_PATH_URL, STANDARD_GET);

            assertTrue("SSL must be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_LongestPath_OverridesSSLRequiredValue() {
        final String methodName = "testGetMatchResponse_LongestPath_OverridesSSLRequiredValue";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraintsStandardThreePathWithDifferentSSL();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(PATH_NESTED_URL, STANDARD_GET);

            assertFalse("SSL must not be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMatchResponse_Extension_SSLTakesPrecedence() {
        final String methodName = "testGetMatchResponse_Extension_SSLTakesPrecedence";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoWithDifferentSSL(ONE_EXT_URL_PATTERN, oneStandardMethod, false, true);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);
            MatchResponse matchResponse = securityConstraintCollection.getMatchResponse(ONE_EXT_URL, STANDARD_GET);

            assertTrue("SSL must be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private List<SecurityConstraint> createSecurityConstraints(String pattern, String[] methods, List<String> roles) {
        boolean sslRequired = false;
        boolean accessPrecluded = false;
        boolean fromHttpConstraint = false;
        boolean accessUncovered = false;
        SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
        securityConstraintsBuilder.buildWebResourceCollection(methods, pattern);
        securityConstraintsBuilder.buildSecurityConstraint(roles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        return securityConstraintsBuilder.getSecurityConstraints();
    }

    private List<SecurityConstraint> createSecurityConstraintsStandardThreePathWithDifferentRoles() {
        // Retain the constraints order to validate that the right constraint is used.
        boolean sslRequired = false;
        boolean accessPrecluded = false;
        boolean fromHttpConstraint = false;
        boolean accessUncovered = false;
        SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
        securityConstraintsBuilder.buildWebResourceCollection(oneStandardMethod, PATH_NESTED_URL_PATTERN);
        securityConstraintsBuilder.buildSecurityConstraint(emptyRoles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        securityConstraintsBuilder.buildWebResourceCollection(oneStandardMethod, PATH_NESTED_URL_PATTERN);
        securityConstraintsBuilder.buildSecurityConstraint(testRoles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        securityConstraintsBuilder.buildWebResourceCollection(oneStandardMethod, PATH_LONGEST_NESTED_URL_PATTERN);
        securityConstraintsBuilder.buildSecurityConstraint(developerRoles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        return securityConstraintsBuilder.getSecurityConstraints();
    }

    private List<SecurityConstraint> createSecurityConstraintsStandardThreePathWithDifferentSSL() {
        // Retain the constraints order to validate that the right constraint is used.
        boolean accessPrecluded = false;
        boolean fromHttpConstraint = false;
        boolean accessUncovered = false;
        SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
        securityConstraintsBuilder.buildWebResourceCollection(oneStandardMethod, PATH_NESTED_URL_PATTERN);
        securityConstraintsBuilder.buildSecurityConstraint(emptyRoles, false, accessPrecluded, fromHttpConstraint, accessUncovered);
        securityConstraintsBuilder.buildWebResourceCollection(oneStandardMethod, PATH_NESTED_URL_PATTERN);
        securityConstraintsBuilder.buildSecurityConstraint(testRoles, true, accessPrecluded, fromHttpConstraint, accessUncovered);
        securityConstraintsBuilder.buildWebResourceCollection(oneStandardMethod, PATH_LONGEST_NESTED_URL_PATTERN);
        securityConstraintsBuilder.buildSecurityConstraint(developerRoles, false, accessPrecluded, fromHttpConstraint, accessUncovered);
        return securityConstraintsBuilder.getSecurityConstraints();
    }

    private List<SecurityConstraint> constraintsTwoSameOnePrecluded(String pattern, String[] methods, List<String> firstRoles, List<String> secondRoles) {
        boolean sslRequired = false;
        boolean accessPrecluded = false;
        boolean fromHttpConstraint = false;
        boolean accessUncovered = false;
        SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
        securityConstraintsBuilder.buildWebResourceCollection(methods, pattern);
        securityConstraintsBuilder.buildSecurityConstraint(firstRoles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        securityConstraintsBuilder.buildWebResourceCollection(methods, pattern);
        securityConstraintsBuilder.buildSecurityConstraint(secondRoles, sslRequired, true, fromHttpConstraint, accessUncovered);
        return securityConstraintsBuilder.getSecurityConstraints();
    }

    private List<SecurityConstraint> constraintsTwoSamePatternWithDifferentRoles(String pattern, String[] methods, List<String> firstRoles, List<String> secondRoles) {
        // Retain the constraints order to validate that the right constraint is used.
        boolean sslRequired = false;
        boolean accessPrecluded = false;
        boolean fromHttpConstraint = false;
        boolean accessUncovered = false;
        SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
        securityConstraintsBuilder.buildWebResourceCollection(methods, pattern);
        securityConstraintsBuilder.buildSecurityConstraint(firstRoles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        securityConstraintsBuilder.buildWebResourceCollection(methods, pattern);
        securityConstraintsBuilder.buildSecurityConstraint(secondRoles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        return securityConstraintsBuilder.getSecurityConstraints();
    }

    private List<SecurityConstraint> constraintsTwoWithDifferentSSL(String pattern, String[] methods, boolean firstSSLRequired, boolean secondSSLRequired) {
        // Retain the constraints order to validate that the right constraint is used.
        boolean accessPrecluded = false;
        boolean fromHttpConstraint = false;
        boolean accessUncovered = false;
        SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
        securityConstraintsBuilder.buildWebResourceCollection(methods, pattern);
        securityConstraintsBuilder.buildSecurityConstraint(testRoles, firstSSLRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        securityConstraintsBuilder.buildWebResourceCollection(methods, pattern);
        securityConstraintsBuilder.buildSecurityConstraint(testRoles, secondSSLRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        return securityConstraintsBuilder.getSecurityConstraints();
    }

    private List<String> createExpectedCombinedRoles() {
        Set<String> tempRoles = new HashSet<String>();
        tempRoles.addAll(testRoles);
        tempRoles.addAll(developerRoles);
        List<String> expectedCombinedRoles = new ArrayList<String>();
        expectedCombinedRoles.addAll(tempRoles);
        return expectedCombinedRoles;
    }

}
