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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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
import com.ibm.ws.webcontainer.security.metadata.CollectionMatch.MatchType;

/**
 *
 */
public class MatchResponseTest {

    private static SharedOutputManager outputMgr;
    private static MatchResponse matchResponse;
    private static MatchResponse developerMatchResponse;
    private static List<String> testRoles;
    private static List<String> developerRoles;

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

        testRoles = createTestRoles();
        developerRoles = createDeveloperRoles();
        matchResponse = createTesterMatchResponse();
        developerMatchResponse = createDeveloperMatchResponse();
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
            List<String> roles = new ArrayList<String>();
            boolean sslRequired = false;
            boolean accessPrecluded = false;
            MatchResponse matchResponse = new MatchResponse(roles, sslRequired, accessPrecluded);
            assertNotNull("There must be a security constraints match.", matchResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetRoles() {
        final String methodName = "testGetRoles";
        try {
            List<String> actualRoles = matchResponse.getRoles();
            assertEquals("Roles must be the same as the ones used in the constructor.", testRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsSSLRequired() {
        final String methodName = "testIsSSLRequired";
        try {
            List<String> roles = new ArrayList<String>();
            boolean accessPrecluded = false;
            boolean sslRequired = true;
            MatchResponse matchResponse = new MatchResponse(roles, sslRequired, accessPrecluded);
            boolean actualSSLRequired = matchResponse.isSSLRequired();
            assertEquals("The sslRequired field must be the same as the ones used in the constructor.",
                         sslRequired, actualSSLRequired);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsAccessPrecluded() {
        final String methodName = "testIsAccessPrecluded";
        try {
            List<String> emptyRoles = new ArrayList<String>();
            boolean accessPrecluded = true;
            boolean sslRequired = false;
            MatchResponse matchResponse = new MatchResponse(emptyRoles, sslRequired, accessPrecluded);
            boolean actualAccessPrecluded = matchResponse.isAccessPrecluded();
            assertTrue("Access must be precluded", actualAccessPrecluded);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void testIsAccessPrecludedWithRolesThrowsIllegalArgumentException() throws Throwable {
        try {
            boolean accessPrecluded = true;
            boolean sslRequired = false;
            new MatchResponse(testRoles, sslRequired, accessPrecluded);
        } catch (Throwable t) {
            assertEquals("The roles must be empty when access is precluded.", t.getLocalizedMessage());
            throw t;
        }
    }

    @Test
    public void testMerge() {
        final String methodName = "testMerge";
        try {
            MatchResponse mergedResponse = matchResponse.merge(matchResponse);
            List<String> actualRoles = mergedResponse.getRoles();
            assertEquals("Roles must be the same as the ones used in the constructor.", testRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMerge_CombinedRoles() {
        final String methodName = "testMerge";
        try {
            List<String> expectedCombinedRoles = createExpectedCombinedRoles();

            MatchResponse mergedResponse = matchResponse.merge(developerMatchResponse);
            List<String> actualRoles = mergedResponse.getRoles();

            assertEquals("Roles must be the same as the ones used in the constructor.", expectedCombinedRoles, actualRoles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMerge_AccessPrecluded() {
        final String methodName = "testMerge_AccessPrecluded";
        try {
            List<String> emptyRoles = new ArrayList<String>();
            boolean accessPrecluded = true;
            boolean sslRequired = false;
            CollectionMatch collectionMatch = new CollectionMatch("/*", MatchType.EXACT_MATCH);
            MatchResponse matchResponse = new MatchResponse(emptyRoles, sslRequired, accessPrecluded, collectionMatch);

            MatchResponse mergedResponse = matchResponse.merge(developerMatchResponse);
            boolean actualAccessPrecluded = mergedResponse.isAccessPrecluded();

            assertTrue("Access must be precluded", actualAccessPrecluded);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private static MatchResponse createTesterMatchResponse() {
        boolean accessPrecluded = false;
        boolean sslRequired = false;
        CollectionMatch collectionMatch = new CollectionMatch("/*", MatchType.EXACT_MATCH);
        MatchResponse matchResponse = new MatchResponse(testRoles, sslRequired, accessPrecluded, collectionMatch);
        return matchResponse;
    }

    private static MatchResponse createDeveloperMatchResponse() {
        boolean accessPrecluded = false;
        boolean sslRequired = false;
        MatchResponse matchResponse = new MatchResponse(developerRoles, sslRequired, accessPrecluded);
        return matchResponse;
    }

    private static List<String> createTestRoles() {
        List<String> roles = new ArrayList<String>();
        roles.add("tester");
        return roles;
    }

    private static List<String> createDeveloperRoles() {
        List<String> roles = new ArrayList<String>();
        roles.add("developer");
        return roles;
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
