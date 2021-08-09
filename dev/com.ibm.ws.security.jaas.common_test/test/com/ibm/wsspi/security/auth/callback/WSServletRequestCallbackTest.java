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
package com.ibm.wsspi.security.auth.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class WSServletRequestCallbackTest {

    private static final String TEST_PROMPT = "testPrompt";
    private static Mockery mockery = new JUnit4Mockery();
    private static HttpServletRequest testHttpServletRequest = mockery.mock(HttpServletRequest.class);
    private static HttpServletRequest anotherHttpServletRequest = mockery.mock(HttpServletRequest.class, "anotherHttpServletRequest");
    private static SharedOutputManager outputMgr;

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
    public void testGetPrompt() throws Exception {
        final String methodName = "testGetPrompt";
        try {
            WSServletRequestCallback callback = new WSServletRequestCallback(TEST_PROMPT);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSServletRequestCallback callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHttpServletRequest() throws Exception {
        final String methodName = "testGetHttpServletRequest";
        try {
            WSServletRequestCallback callback = new WSServletRequestCallback(TEST_PROMPT);
            HttpServletRequest currentRequest = callback.getHttpServletRequest();
            assertNull("The request obtained from the WSServletRequestCallback callback must be null.", currentRequest);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetHttpServletRequest() throws Exception {
        final String methodName = "testSetHttpServletRequest";
        try {
            WSServletRequestCallback callback = new WSServletRequestCallback(TEST_PROMPT);
            callback.setHttpServletRequest(testHttpServletRequest);
            HttpServletRequest currentRequest = callback.getHttpServletRequest();
            assertEquals("The request obtained from the WSServletRequestCallback callback must be the same one used in setHttpServletRequest.",
                         testHttpServletRequest, currentRequest);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetPromptWhenUsingConstructorWithRequest() throws Exception {
        final String methodName = "testGetPromptWhenUsingConstructorWithRequest";
        try {
            WSServletRequestCallback callback = new WSServletRequestCallback(TEST_PROMPT, testHttpServletRequest);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSServletRequestCallback callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHttpServletRequestWhenUsingConstructorWithRequest() throws Exception {
        final String methodName = "testGetHttpServletRequestWhenUsingConstructorWithRequest";
        try {
            WSServletRequestCallback callback = new WSServletRequestCallback(TEST_PROMPT, testHttpServletRequest);
            HttpServletRequest currentRequest = callback.getHttpServletRequest();
            assertEquals("The request obtained from the WSServletRequestCallback callback must be the same one used in the constructor.",
                         testHttpServletRequest, currentRequest);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetHttpServletRequestWhenUsingConstructorWithRequest() throws Exception {
        final String methodName = "testSetHttpServletRequestWhenUsingConstructorWithRequest";
        try {
            WSServletRequestCallback callback = new WSServletRequestCallback(TEST_PROMPT, testHttpServletRequest);
            callback.setHttpServletRequest(anotherHttpServletRequest);
            HttpServletRequest currentRequest = callback.getHttpServletRequest();
            assertEquals("The request obtained from the WSServletRequestCallback callback must be the same one used in setHttpServletRequest.",
                         anotherHttpServletRequest, currentRequest);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
