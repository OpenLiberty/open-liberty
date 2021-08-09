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

import javax.servlet.http.HttpServletResponse;

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
public class WSServletResponseCallbackTest {

    private static final String TEST_PROMPT = "testPrompt";
    private static Mockery mockery = new JUnit4Mockery();
    private static HttpServletResponse testHttpServletResponse = mockery.mock(HttpServletResponse.class);
    private static HttpServletResponse anotherHttpServletResponse = mockery.mock(HttpServletResponse.class, "anotherHttpServletResponse");
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
            WSServletResponseCallback callback = new WSServletResponseCallback(TEST_PROMPT);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSServletResponseCallback callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHttpServletResponse() throws Exception {
        final String methodName = "testGetHttpServletResponse";
        try {
            WSServletResponseCallback callback = new WSServletResponseCallback(TEST_PROMPT);
            HttpServletResponse currentResponse = callback.getHttpServletResponse();
            assertNull("The response obtained from the WSServletResponseCallback callback must be null.", currentResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetHttpServletResponse() throws Exception {
        final String methodName = "testSetHttpServletResponse";
        try {
            WSServletResponseCallback callback = new WSServletResponseCallback(TEST_PROMPT);
            callback.setHttpServletResponse(testHttpServletResponse);
            HttpServletResponse currentResponse = callback.getHttpServletResponse();
            assertEquals("The response obtained from the WSServletResponseCallback callback must be the same one used in setHttpServletResponse.",
                         testHttpServletResponse, currentResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetPromptWhenUsingConstructorWithResponse() throws Exception {
        final String methodName = "testGetPromptWhenUsingConstructorWithResponse";
        try {
            WSServletResponseCallback callback = new WSServletResponseCallback(TEST_PROMPT, testHttpServletResponse);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSServletResponseCallback callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHttpServletResponseWhenUsingConstructorWithResponse() throws Exception {
        final String methodName = "testGetHttpServletResponseWhenUsingConstructorWithResponse";
        try {
            WSServletResponseCallback callback = new WSServletResponseCallback(TEST_PROMPT, testHttpServletResponse);
            HttpServletResponse currentResponse = callback.getHttpServletResponse();
            assertEquals("The response obtained from the WSServletResponseCallback callback must be the same one used in the constructor.",
                         testHttpServletResponse, currentResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetHttpServletResponseWhenUsingConstructorWithResponse() throws Exception {
        final String methodName = "testSetHttpServletResponseWhenUsingConstructorWithResponse";
        try {
            WSServletResponseCallback callback = new WSServletResponseCallback(TEST_PROMPT, testHttpServletResponse);
            callback.setHttpServletResponse(anotherHttpServletResponse);
            HttpServletResponse currentResponse = callback.getHttpServletResponse();
            assertEquals("The response obtained from the WSServletResponseCallback callback must be the same one used in setHttpServletResponse.",
                         anotherHttpServletResponse, currentResponse);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
