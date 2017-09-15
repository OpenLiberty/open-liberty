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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class WSRealmNameCallbackImplTest {

    private static final String TEST_PROMPT = "testPrompt";
    private static final String DEFAULT_REALM_NAME = "defaultRealm";
    private static final String TEST_REALM_NAME = "testRealmName";
    private static final String ANOTHER_REALM_NAME = "anotherRealmName";
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
            WSRealmNameCallbackImpl callback = new WSRealmNameCallbackImpl(TEST_PROMPT);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSRealmNameCallbackImpl callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetDefaultRealmName() throws Exception {
        final String methodName = "testGetDefaultRealmName";
        try {
            WSRealmNameCallbackImpl callback = new WSRealmNameCallbackImpl(TEST_PROMPT);
            String currentDefaultRealmName = callback.getDefaultRealmName();
            assertEquals("The default realm name obtained from the WSRealmNameCallbackImpl callback must be the same as <default>.",
                         DEFAULT_REALM_NAME, currentDefaultRealmName);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetRealmName() throws Exception {
        final String methodName = "testGetRealmName";
        try {
            WSRealmNameCallbackImpl callback = new WSRealmNameCallbackImpl(TEST_PROMPT);
            String defaultRealmName = callback.getDefaultRealmName();
            String currentRealmName = callback.getRealmName();
            assertEquals("The realm name obtained from the WSRealmNameCallbackImpl callback must be the same as the default realm name.",
                         defaultRealmName, currentRealmName);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetRealmName() throws Exception {
        final String methodName = "testSetRealmName";
        try {
            WSRealmNameCallbackImpl callback = new WSRealmNameCallbackImpl(TEST_PROMPT);
            callback.setRealmName(TEST_REALM_NAME);
            String currentRealmName = callback.getRealmName();
            assertEquals("The realm name obtained from the WSRealmNameCallbackImpl callback must be the same one used in setRealmName.",
                         TEST_REALM_NAME, currentRealmName);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetPromptWhenUsingConstructorWithRealmName() throws Exception {
        final String methodName = "testGetPromptWhenUsingConstructorWithRealmName";
        try {
            WSRealmNameCallbackImpl callback = new WSRealmNameCallbackImpl(TEST_PROMPT, TEST_REALM_NAME);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSRealmNameCallbackImpl callback must be the same one used in the constructor.",
                         TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetDefaultRealmNameWhenUsingConstructorWithRealmName() throws Exception {
        final String methodName = "testGetDefaultRealmNameWhenUsingConstructorWithRealmName";
        try {
            WSRealmNameCallbackImpl callback = new WSRealmNameCallbackImpl(TEST_PROMPT, TEST_REALM_NAME);
            String currentDefaultRealmName = callback.getDefaultRealmName();
            assertEquals("The default realm name obtained from the WSRealmNameCallbackImpl callback must be the same one used in the constructor.",
                         TEST_REALM_NAME, currentDefaultRealmName);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetRealmNameWhenUsingConstructorWithRealmName() throws Exception {
        final String methodName = "testGetRealmNameWhenUsingConstructorWithRealmName";
        try {
            WSRealmNameCallbackImpl callback = new WSRealmNameCallbackImpl(TEST_PROMPT, TEST_REALM_NAME);
            String currentRealmName = callback.getRealmName();
            assertEquals("The realm name obtained from the WSRealmNameCallbackImpl callback must be the same as the default realm name.",
                         TEST_REALM_NAME, currentRealmName);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetRealmNameWhenUsingConstructorWithRealmName() throws Exception {
        final String methodName = "testSetRealmNameWhenUsingConstructorWithRealmName";
        try {
            WSRealmNameCallbackImpl callback = new WSRealmNameCallbackImpl(TEST_PROMPT, TEST_REALM_NAME);
            callback.setRealmName(ANOTHER_REALM_NAME);
            String currentRealmName = callback.getRealmName();
            assertEquals("The realm name obtained from the WSRealmNameCallbackImpl callback must be the same one used in setRealmName.",
                         ANOTHER_REALM_NAME, currentRealmName);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
