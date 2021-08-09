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
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.webcontainer.security.metadata.CustomMatchingStrategy;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollectionImpl;

/**
 *
 */
public class CustomMatchingStrategyTest {

    private static CustomMatchingStrategy matchingStrategy;
    private static SharedOutputManager outputMgr;

    private static final String CUSTOM = "CUSTOM";
    private static final String CUSTOM_NOT_LISTED = "NOTLISTED";
    private static List<String> testRoles = Arrays.asList("tester");
    private static List<String> developerRoles = Arrays.asList("developer");
    private static String ONE_EXACT_URL = "/oneExactURL/oneExactURL.oneExactURL";
    private static String ONE_PATH_URL_PATTERN = "/onePathURL/*";
    private static String ONE_PATH_URL = "/onePathURL/match";
    private static String ONE_EXT_URL_PATTERN = "*.oneExtension";
    private static String ONE_EXT_URL = "matches.oneExtension";
    private final String[] oneCustomMethod = { "CUSTOM" };
    @SuppressWarnings("unchecked")
    private final List<String> emptyRoles = Collections.EMPTY_LIST;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        matchingStrategy = new CustomMatchingStrategy();
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
            String method = "CUSTOM";
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXACT_URL, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Path() {
        final String methodName = "performMatch_Path";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_PATH_URL_PATTERN, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_PATH_URL, CUSTOM);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Extension() {
        final String methodName = "performMatch_Extension";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXT_URL_PATTERN, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_EXT_URL, CUSTOM);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Omission() {
        final String methodName = "performMatch_Omission";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = "CUSTOM";
            List<SecurityConstraint> securityConstraints = createSecurityConstraintsOmission(ONE_EXACT_URL, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);

            assertSame("There must not be a match. The response must be NO_MATCH_RESPONSE.",
                       MatchResponse.NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_CustomMethodInOmissionOfFirstConstraint_Exact() {
        final String methodName = "performMatch_CustomMethodInOmissionOfFirstConstraint_Exact";
        try {
            String resourceName = "/OverlapCustomServlet";
            String method = CUSTOM;
            SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
            securityConstraintsBuilder.buildWebResourceCollection(new String[] {}, new String[] { "GET", "CUSTOM" }, "/AnotherCustomServlet");
            securityConstraintsBuilder.buildSecurityConstraint(developerRoles, false, false, false, false);
            securityConstraintsBuilder.buildWebResourceCollection(new String[] {}, new String[] { "GET", "POST" }, "/OverlapCustomServlet");
            securityConstraintsBuilder.buildSecurityConstraint(testRoles, false, false, false, false);
            List<SecurityConstraint> securityConstraints = securityConstraintsBuilder.getSecurityConstraints();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_NotListed_Exact() {
        final String methodName = "performMatch_NotListed_Exact";
        try {
            String resourceName = ONE_EXACT_URL;
            String method = CUSTOM_NOT_LISTED;
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXACT_URL, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_UrlNotListed_Exact() {
        final String methodName = "performMatch_UrlNotListed_Exact";
        try {
            String resourceName = "/AnotherServletThatIsNotInTheConstraints";
            String method = CUSTOM;
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXACT_URL, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);

            assertSame("There must not be a match. The response must be NO_MATCH_RESPONSE.",
                       MatchResponse.NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Exact_NoRoles() {
        final String methodName = "performMatch_Exact_NoRoles";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraints(ONE_EXACT_URL, new String[] { "POST" }, emptyRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_EXACT_URL, CUSTOM);

            assertSame("There must not be a match. The response must be NO_MATCH_RESPONSE.",
                       MatchResponse.NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_OmissionNotListed() {
        final String methodName = "performMatch_OmissionNotListed";
        try {
            List<SecurityConstraint> securityConstraints = createSecurityConstraintsOmission(ONE_EXACT_URL, oneCustomMethod, testRoles);
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_EXACT_URL, CUSTOM_NOT_LISTED);

            assertEquals("There must be a match.", testRoles, matchResponse.getRoles());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void performMatch_Exact_Precluded() {
        final String methodName = "performMatch_Exact_Precluded";
        try {
            SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
            securityConstraintsBuilder.buildWebResourceCollection(new String[] { "GET" }, new String[] {}, ONE_EXACT_URL);
            securityConstraintsBuilder.buildSecurityConstraint(emptyRoles, false, true, false, false);
            List<SecurityConstraint> securityConstraints = securityConstraintsBuilder.getSecurityConstraints();
            SecurityConstraintCollection securityConstraintCollection = new SecurityConstraintCollectionImpl(securityConstraints);

            MatchResponse matchResponse = matchingStrategy.performMatch(securityConstraintCollection, ONE_EXACT_URL, CUSTOM);

            assertSame("There must not be a match. The response must be CUSTOM_NO_MATCH_RESPONSE.",
                       MatchResponse.CUSTOM_NO_MATCH_RESPONSE, matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private List<SecurityConstraint> createSecurityConstraints(String pattern, String[] methods, List<String> roles) {
        boolean sslRequired = false;
        boolean accessPrecluded = false;
        SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
        securityConstraintsBuilder.buildWebResourceCollection(methods, pattern);
        securityConstraintsBuilder.buildSecurityConstraint(roles, sslRequired, accessPrecluded, false, false);
        return securityConstraintsBuilder.getSecurityConstraints();
    }

    private List<SecurityConstraint> createSecurityConstraintsOmission(String pattern, String[] omissionMethods, List<String> roles) {
        boolean sslRequired = false;
        boolean accessPrecluded = false;
        boolean accessUncovered = false;
        SecurityConstraintsBuilder securityConstraintsBuilder = new SecurityConstraintsBuilder();
        securityConstraintsBuilder.buildWebResourceCollection(new String[] {}, omissionMethods, pattern);
        securityConstraintsBuilder.buildSecurityConstraint(roles, sslRequired, accessPrecluded, false, accessUncovered);
        return securityConstraintsBuilder.getSecurityConstraints();
    }

}
