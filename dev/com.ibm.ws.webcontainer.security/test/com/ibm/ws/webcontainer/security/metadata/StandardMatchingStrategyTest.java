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
package com.ibm.ws.webcontainer.security.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

import com.ibm.ws.webcontainer.security.metadata.CollectionMatch;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.MatchingStrategy;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollectionImpl;
import com.ibm.ws.webcontainer.security.metadata.StandardMatchingStrategy;

/**
 *
 */
public class StandardMatchingStrategyTest {

    private static StandardMatchingStrategy matchingStrategy;
    private static SharedOutputManager outputMgr;
    private static final String STANDARD_GET = "GET";
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
    @SuppressWarnings("unchecked")
    private final List<String> emptyRoles = Collections.EMPTY_LIST;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        matchingStrategy = new StandardMatchingStrategy();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    @Test
    public void performMatch_Exact() {
        final String methodName = "performMatch_Exact";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXACT_URL, oneStandardMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Exact_PrecludedMatchTakesPrecedence() {
        final String methodName = "performMatch_Exact_PrecludedMatchTakesPrecedence";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSameOnePrecluded(ONE_EXACT_URL, oneStandardMethod, testRoles, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);

            assertTrue("There must be a match with access precluded.", matchResponse.isAccessPrecluded());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Exact_NoRolesMatchTakesNextPrecedence() {
        final String methodName = "performMatch_Exact_NoRolesMatchTakesNextPrecedence";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXACT_URL, oneStandardMethod, emptyRoles, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertTrue("There must be a match with no roles.", actualRoles.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_MultipleExact_RolesAreCombined() {
        final String methodName = "performMatch_MultipleExact_RolesAreCombined";
        try {
            List<String> expectedCombinedRoles = createExpectedCombinedRoles();
            String resourceName = ONE_EXACT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXACT_URL, oneStandardMethod, testRoles, developerRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertEquals("There must be a match with combined roles.", expectedCombinedRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Path() {
        final String methodName = "performMatch_Path";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_PATH_URL_PATTERN, oneStandardMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_PATH_URL, STANDARD_GET);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Path_PrecludedMatchTakesFirstPrecedence() {
        final String methodName = "performMatch_Path_PrecludedMatchTakesFirstPrecedence";
        try {
            String resourceName = ONE_PATH_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSameOnePrecluded(ONE_PATH_URL_PATTERN, oneStandardMethod, testRoles, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);

            assertTrue("There must be a match with access precluded.", matchResponse.isAccessPrecluded());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Path_NoRolesMatchTakesSecondPrecedence() {
        final String methodName = "performMatch_Path_NoRolesMatchTakesSecondPrecedence";
        try {
            String resourceName = ONE_PATH_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_PATH_URL_PATTERN, oneStandardMethod, emptyRoles, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertTrue("There must be a match with no roles.", actualRoles.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_MultipleSamePaths_RolesAreCombined() {
        final String methodName = "performMatch_MultipleSamePaths_RolesAreCombined";
        try {
            List<String> expectedCombinedRoles = createExpectedCombinedRoles();
            String resourceName = ONE_PATH_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_PATH_URL_PATTERN, oneStandardMethod, testRoles, developerRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertEquals("There must be a match with combined roles.", expectedCombinedRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_LongestPath_Overrides() {
        final String methodName = "performMatch_LongestPath_Overrides";
        try {
            String resourceName = PATH_NESTED_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = createSecurityConstraintsStandardThreePathWithDifferentRoles();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertEquals("There must be a match with developer roles.", developerRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Extension_PrecludedMatchTakesFirstPrecedence() {
        final String methodName = "performMatch_Extension_PrecludedMatchTakesFirstPrecedence";
        try {
            String resourceName = ONE_EXT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSameOnePrecluded(ONE_EXT_URL_PATTERN, oneStandardMethod, testRoles, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);

            assertTrue("There must be a match with access precluded.", matchResponse.isAccessPrecluded());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Extension_NoRolesMatchTakesSecondPrecedence() {
        final String methodName = "performMatch_Extension_NoRolesMatchTakesSecondPrecedence";
        try {
            String resourceName = ONE_EXT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXT_URL_PATTERN, oneStandardMethod, emptyRoles, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertTrue("There must be a match with no roles.", actualRoles.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_MultipleSameExtension_RolesAreCombined() {
        final String methodName = "performMatch_MultipleSameExtension_RolesAreCombined";
        try {
            List<String> expectedCombinedRoles = createExpectedCombinedRoles();
            String resourceName = ONE_EXT_URL;
            String method = STANDARD_GET;
            List<SecurityConstraint> securityConstraints = constraintsTwoSamePatternWithDifferentRoles(ONE_EXT_URL_PATTERN, oneStandardMethod, testRoles, developerRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);
            List<String> actualRoles = matchResponse.getRoles();

            assertEquals("There must be a match with combined roles.", expectedCombinedRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Exact_NoSSLTakesPrecedence() {
        final String methodName = "performMatch_Exact_NoSSLTakesPrecedence";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoWithDifferentSSL(ONE_EXACT_URL, oneStandardMethod, false, true);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_EXACT_URL, STANDARD_GET);

            assertFalse("SSL must not be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Path_SSLTakesPrecedence() {
        final String methodName = "performMatch_Path_SSLTakesPrecedence";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoWithDifferentSSL(ONE_PATH_URL_PATTERN, oneStandardMethod, false, true);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_PATH_URL, STANDARD_GET);

            assertTrue("SSL must be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Path_SSLRequired() {
        final String methodName = "performMatch_Path_SSLRequired";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoWithDifferentSSL(ONE_PATH_URL_PATTERN, oneStandardMethod, true, true);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_PATH_URL, STANDARD_GET);

            assertTrue("SSL must be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_LongestPath_OverridesSSLRequiredValue() {
        final String methodName = "performMatch_LongestPath_OverridesSSLRequiredValue";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraintsStandardThreePathWithDifferentSSL();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, PATH_NESTED_URL, STANDARD_GET);

            assertFalse("SSL must not be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Extension_SSLTakesPrecedence() {
        final String methodName = "performMatch_Extension_SSLTakesPrecedence";
        try {
            List<SecurityConstraint> securityConstraints = constraintsTwoWithDifferentSSL(ONE_EXT_URL_PATTERN, oneStandardMethod, false, true);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_EXT_URL, STANDARD_GET);

            assertTrue("SSL must be required.", matchResponse.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_ExactAndPathReturnsExact() {
        final String methodName = "performMatch_ExactAndPathReturnsExact";
        try {
            boolean accessPrecluded = false;
            boolean fromHttpConstraint = false;
            boolean accessUncovered = false;
            boolean sslRequired = true;
            String method = "GET";
            // Retain the constraints order to validate that the right constraint is used.
            SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
            securityConstraintsBuilder.buildWebResourceCollection(oneStandardMethod, ONE_PATH_URL);
            securityConstraintsBuilder.buildWebResourceCollection(oneStandardMethod, ONE_EXACT_URL);
            securityConstraintsBuilder.buildSecurityConstraint(testRoles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
            List<SecurityConstraint> securityConstraints = securityConstraintsBuilder.getSecurityConstraints();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_EXACT_URL, method);
            CollectionMatch match = matchResponse.getCollectionMatch();
            assertNotNull("There must be a collection match.", match);
            assertTrue("The collection match must be exact.", match.isExactMatch());
            assertEquals("The URIs must be the same.", ONE_EXACT_URL, match.getUrlPattern());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void isStandardHttpMethod() {
        final String methodName = "isStandardHttpMethod";
        try {
            String standardMethods[] = { "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE" };
            String nonStandardMethods[] = { "CUSTOM", "ABC123ABC", "POST1", "OPTION" };

            for (int i = 0; i < standardMethods.length; i++) {
                assertTrue("The HTTP method must be standard.", MatchingStrategy.isStandardHttpMethod(standardMethods[i]));
            }
            for (int i = 0; i < nonStandardMethods.length; i++) {
                assertFalse("The HTTP method must not be standard.", MatchingStrategy.isStandardHttpMethod(nonStandardMethods[i]));
            }
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

    private List<String> createExpectedCombinedRoles() {
        Set<String> tempRoles = new HashSet<String>();
        tempRoles.addAll(testRoles);
        tempRoles.addAll(developerRoles);
        List<String> expectedCombinedRoles = new ArrayList<String>();
        expectedCombinedRoles.addAll(tempRoles);
        return expectedCombinedRoles;
    }
}
