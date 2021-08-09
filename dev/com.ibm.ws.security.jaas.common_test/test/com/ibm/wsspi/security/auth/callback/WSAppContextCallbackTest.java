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
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class WSAppContextCallbackTest {

    private static final String TEST_PROMPT = "testPrompt";
    private static Map testContext = new HashMap();
    private static Map anotherContext = new HashMap();
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
            WSAppContextCallback callback = new WSAppContextCallback(TEST_PROMPT);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSAppContextCallback callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetContext() throws Exception {
        final String methodName = "testGetContext";
        try {
            WSAppContextCallback callback = new WSAppContextCallback(TEST_PROMPT);
            Map currentContext = callback.getContext();
            assertNull("The context obtained from the WSAppContextCallback callback must null.", currentContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetContext() throws Exception {
        final String methodName = "testSetContext";
        try {
            WSAppContextCallback callback = new WSAppContextCallback(TEST_PROMPT);
            callback.setContext(testContext);
            Map currentContext = callback.getContext();
            assertSame("The context obtained from the WSAppContextCallback callback must be the same on used in setContext.",
                       testContext, currentContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetPromptWhenUsingConstructorWithContext() throws Exception {
        final String methodName = "testGetPromptWhenUsingConstructorWithContext";
        try {
            WSAppContextCallback callback = new WSAppContextCallback(TEST_PROMPT, testContext);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSAppContextCallback callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetContextWhenUsingConstructorWithContext() throws Exception {
        final String methodName = "testGetContextWhenUsingConstructorWithContext";
        try {
            WSAppContextCallback callback = new WSAppContextCallback(TEST_PROMPT, testContext);
            Map currentContext = callback.getContext();
            assertEquals("The context obtained from the WSAppContextCallback callback must be the same one used in the constructor.",
                         testContext, currentContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetContextWhenUsingConstructorWithContext() throws Exception {
        final String methodName = "testSetContextWhenUsingConstructorWithContext";
        try {
            WSAppContextCallback callback = new WSAppContextCallback(TEST_PROMPT, testContext);
            callback.setContext(anotherContext);
            Map currentContext = callback.getContext();
            assertSame("The context obtained from the WSAppContextCallback callback must be the same on used in setContext.",
                       anotherContext, currentContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
