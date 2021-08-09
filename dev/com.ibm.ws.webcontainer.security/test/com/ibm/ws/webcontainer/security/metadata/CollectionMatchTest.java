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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.webcontainer.security.metadata.CollectionMatch;
import com.ibm.ws.webcontainer.security.metadata.CollectionMatch.MatchType;

/**
 *
 */
public class CollectionMatchTest {

    private static SharedOutputManager outputMgr;
    private static String ONE_PATH_URL_PATTERN = "/onePathURL/*";

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
    public void testGetUrlPattern() {
        final String methodName = "testGetUrlPattern";
        try {
            String urlPattern = "/*";
            MatchType matchType = MatchType.PATH_MATCH;
            CollectionMatch match = new CollectionMatch(urlPattern, matchType);
            assertEquals("The URL pattern must be the same used in the constructor.", urlPattern, match.getUrlPattern());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsExactMatch() {
        final String methodName = "testIsExactMatch";
        try {
            String urlPattern = "/index.jsp";
            CollectionMatch match = new CollectionMatch(urlPattern, MatchType.EXACT_MATCH);
            assertTrue("The match must be exact match.", match.isExactMatch());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsPathMatch() {
        final String methodName = "testIsPathMatch";
        try {
            CollectionMatch match = new CollectionMatch(ONE_PATH_URL_PATTERN, MatchType.PATH_MATCH);
            assertTrue("The match must be path match.", match.isPathMatch());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsExtensionMatch() {
        final String methodName = "testIsExtensionMatch";
        try {
            String urlPattern = "*.jsp";
            CollectionMatch match = new CollectionMatch(urlPattern, MatchType.EXTENSION_MATCH);
            assertTrue("The match must be extension match.", match.isExtensionMatch());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
