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
package com.ibm.websphere.security.auth.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class WSCredTokenCallbackImplTest {

    private static final String TEST_PROMPT = "testPrompt";
    private static final String CRED_TOKEN_STRING = "A test credential token.";
    private static final String ANOTHER_CRED_TOKEN_STRING = "Another credential token.";
    private static byte[] testCredToken = CRED_TOKEN_STRING.getBytes();
    private static byte[] anotherTestCredToken = ANOTHER_CRED_TOKEN_STRING.getBytes();
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
            WSCredTokenCallbackImpl callback = new WSCredTokenCallbackImpl(TEST_PROMPT);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSCredTokenCallbackImpl callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetCredToken() throws Exception {
        final String methodName = "testGetCredToken";
        try {
            WSCredTokenCallbackImpl callback = new WSCredTokenCallbackImpl(TEST_PROMPT);
            byte[] currentCredToken = callback.getCredToken();
            assertNull("The cred token obtained from the WSCredTokenCallbackImpl callback must be null.", currentCredToken);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetDefaultCredToken() throws Exception {
        final String methodName = "testGetDefaultCredToken";
        try {
            WSCredTokenCallbackImpl callback = new WSCredTokenCallbackImpl(TEST_PROMPT);
            byte[] currentDefaultCredToken = callback.getDefaultCredToken();
            assertNull("The default cred token obtained from the WSCredTokenCallbackImpl callback must be null.", currentDefaultCredToken);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetCredToken() throws Exception {
        final String methodName = "testSetCredToken";
        try {
            WSCredTokenCallbackImpl callback = new WSCredTokenCallbackImpl(TEST_PROMPT);
            callback.setCredToken(testCredToken);
            byte[] currentCredToken = callback.getCredToken();
            assertEquals("The cred token obtained from the WSCredTokenCallbackImpl callback must be the same as the one used in setCredToken.",
                         CRED_TOKEN_STRING, new String(currentCredToken));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetPromptWhenUsingConstructorWithCredToken() throws Exception {
        final String methodName = "testGetPromptWhenUsingConstructorWithCredToken";
        try {
            WSCredTokenCallbackImpl callback = new WSCredTokenCallbackImpl(TEST_PROMPT, testCredToken);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSCredTokenCallbackImpl callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetDefaultCredTokenWhenUsingConstructorWithCredToken() throws Exception {
        final String methodName = "testGetDefaultCredTokenWhenUsingConstructorWithCredToken";
        try {
            WSCredTokenCallbackImpl callback = new WSCredTokenCallbackImpl(TEST_PROMPT, testCredToken);
            byte[] currentDefaultCredToken = callback.getDefaultCredToken();
            assertEquals("The default cred token obtained from the WSCredTokenCallbackImpl callback must be the same as the one used in the constructor.",
                         CRED_TOKEN_STRING, new String(currentDefaultCredToken));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /*
     * The behavior in tWAS is to return null cred token if no setCredToken is invoked with a token.
     * This is counter intuitive since a default cred token is being passed in the constructor and it
     * would have been expected that getCredToken() returns the default cred token if no setCredToken is invoked.
     */
    @Test
    public void testGetCredTokenWhenUsingConstructorWithCredToken() throws Exception {
        final String methodName = "testGetCredTokenWhenUsingConstructorWithCredToken";
        try {
            WSCredTokenCallbackImpl callback = new WSCredTokenCallbackImpl(TEST_PROMPT, testCredToken);
            byte[] currentCredToken = callback.getCredToken();
            assertNull("The cred token obtained from the WSCredTokenCallbackImpl callback must be null.", currentCredToken);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetCredTokenWhenUsingConstructorWithCredToken() throws Exception {
        final String methodName = "testSetCredTokenWhenUsingConstructorWithCredToken";
        try {
            WSCredTokenCallbackImpl callback = new WSCredTokenCallbackImpl(TEST_PROMPT, testCredToken);
            callback.setCredToken(anotherTestCredToken);
            byte[] currentCredToken = callback.getCredToken();
            assertEquals("The cred token obtained from the WSCredTokenCallbackImpl callback must be the same as the one used in setCredToken.",
                         ANOTHER_CRED_TOKEN_STRING, new String(currentCredToken));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
