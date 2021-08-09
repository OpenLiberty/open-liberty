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
public class WSAuthMechOidCallbackImplTest {

    private static final String TEST_PROMPT = "testPrompt";
    private static final String LTPA_OID = "1.3.18.0.2.30.2";
    private static final String TEST_OID = "testOID";
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
            WSAuthMechOidCallbackImpl callback = new WSAuthMechOidCallbackImpl(TEST_PROMPT);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSAuthMechOidCallbackImpl callback must be the same one used in the constructor.", TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetdefaultAuthMechOid() throws Exception {
        final String methodName = "testGetdefaultAuthMechOid";
        try {
            WSAuthMechOidCallbackImpl callback = new WSAuthMechOidCallbackImpl(TEST_PROMPT);
            String currentDefaultAuthMechOid = callback.getdefaultAuthMechOid();
            assertEquals("The default OID obtained from the WSAuthMechOidCallbackImpl callback must be 1.3.18.0.2.30.2", LTPA_OID, currentDefaultAuthMechOid);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAuthMechOid() throws Exception {
        final String methodName = "testGetAuthMechOid";
        try {
            WSAuthMechOidCallbackImpl callback = new WSAuthMechOidCallbackImpl(TEST_PROMPT);
            String defaultOID = callback.getdefaultAuthMechOid();
            String currentAuthMechOid = callback.getAuthMechOid();
            assertEquals("The OID obtained from the WSAuthMechOidCallbackImpl callback must be same as the default OID", defaultOID, currentAuthMechOid);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetAuthMechOid() throws Exception {
        final String methodName = "testSetAuthMechOid";
        try {
            WSAuthMechOidCallbackImpl callback = new WSAuthMechOidCallbackImpl(TEST_PROMPT);
            callback.setAuthMechOid(TEST_OID);
            String currentAuthMechOid = callback.getAuthMechOid();
            assertEquals("The OID obtained from the WSAuthMechOidCallbackImpl callback must be same as the one used in setAuthMechOid.", TEST_OID, currentAuthMechOid);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetPromptWhenUsingConstructorWithOID() throws Exception {
        final String methodName = "testGetPromptWhenUsingConstructorWithOID";
        try {
            WSAuthMechOidCallbackImpl callback = new WSAuthMechOidCallbackImpl(TEST_PROMPT, TEST_OID);
            String currentPrompt = callback.getPrompt();
            assertEquals("The prompt obtained from the WSAuthMechOidCallbackImpl callback must be the same one used in the constructor.", TEST_PROMPT, currentPrompt);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAuthMechOidWhenUsingConstructorWithOID() throws Exception {
        final String methodName = "testGetAuthMechOidWhenUsingConstructorWithOID";
        try {
            WSAuthMechOidCallbackImpl callback = new WSAuthMechOidCallbackImpl(TEST_PROMPT, TEST_OID);
            String currentAuthMechOid = callback.getAuthMechOid();
            assertEquals("The OID obtained from the WSAuthMechOidCallbackImpl callback must be same one used in the constructor.", TEST_OID, currentAuthMechOid);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAuthMechOidWhenUsingConstructorWithNullOID() throws Exception {
        final String methodName = "testGetAuthMechOidWhenUsingConstructorWithNullOID";
        try {
            WSAuthMechOidCallbackImpl callback = new WSAuthMechOidCallbackImpl(TEST_PROMPT, null);
            String defaultOID = callback.getdefaultAuthMechOid();
            String currentAuthMechOid = callback.getAuthMechOid();
            assertEquals("The OID obtained from the WSAuthMechOidCallbackImpl callback must be same as the default OID", defaultOID, currentAuthMechOid);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSetAuthMechOidWhenUsingConstructorWithOID() throws Exception {
        final String methodName = "testSetAuthMechOidWhenUsingConstructorWithOID";
        try {
            WSAuthMechOidCallbackImpl callback = new WSAuthMechOidCallbackImpl(TEST_PROMPT, TEST_OID);
            callback.setAuthMechOid(LTPA_OID);
            String currentAuthMechOid = callback.getAuthMechOid();
            assertEquals("The OID obtained from the WSAuthMechOidCallbackImpl callback must be same as the one used in setAuthMechOid.", LTPA_OID, currentAuthMechOid);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
