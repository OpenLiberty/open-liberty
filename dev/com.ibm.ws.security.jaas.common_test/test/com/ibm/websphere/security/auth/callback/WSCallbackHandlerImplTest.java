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

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.jaas.common.internal.callback.CallbacksAssertionHelper;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;

/**
 *
 */
@SuppressWarnings("unchecked")
public class WSCallbackHandlerImplTest {

    private static final String DEFAULT_REALM_NAME = "defaultRealm";
    private static final String CRED_TOKEN_STRING = "A test credential token.";
    private static byte[] testCredToken = CRED_TOKEN_STRING.getBytes();
    private static String userName = "testUser";
    private static String password = "testUserPwd";
    private static String realmName = "testRealm";
    private static Map context = new HashMap();
    private static Callback[] callbacks = createCallbacks();
    private static SharedOutputManager outputMgr;

    private static Callback[] createCallbacks() {
        Callback[] callbacks = new Callback[5];
        callbacks[0] = new NameCallback("User name:");;
        callbacks[1] = new PasswordCallback("User password", false);
        callbacks[2] = new WSRealmNameCallbackImpl("Realm name:");
        callbacks[3] = new WSAppContextCallback("Application context:");
        callbacks[4] = new WSCredTokenCallbackImpl("Credential token:");
        return callbacks;
    }

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
    public void testConstructorWithUserNameAndPassword() throws Exception {
        final String methodName = "testConstructorWithUserNameAndPassword";
        try {
            WSCallbackHandlerImpl callbackHandler = new WSCallbackHandlerImpl(userName, password);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { userName, password, DEFAULT_REALM_NAME, null, null });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testConstructorWithUserNameRealmNameAndPassword() throws Exception {
        final String methodName = "testConstructorWithUserNameRealmNameAndPassword";
        try {
            WSCallbackHandlerImpl callbackHandler = new WSCallbackHandlerImpl(userName, realmName, password);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { userName, password, realmName, null, null });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testConstructorWithUserNameRealmNamePasswordAndContext() throws Exception {
        final String methodName = "testConstructorWithUserNameRealmNamePasswordAndContext";
        try {
            WSCallbackHandlerImpl callbackHandler = new WSCallbackHandlerImpl(userName, realmName, password, context);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { userName, password, realmName, context, null });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testConstructorWithCredentialToken() throws Exception {
        final String methodName = "testConstructorWithCredentialToken";
        try {
            WSCallbackHandlerImpl callbackHandler = new WSCallbackHandlerImpl(testCredToken);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { null, null, DEFAULT_REALM_NAME, null, CRED_TOKEN_STRING });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
