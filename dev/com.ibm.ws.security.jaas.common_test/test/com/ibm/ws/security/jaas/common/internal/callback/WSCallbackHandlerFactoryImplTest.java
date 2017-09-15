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
package com.ibm.ws.security.jaas.common.internal.callback;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.security.auth.callback.WSAuthMechOidCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;
import com.ibm.wsspi.security.auth.callback.WSCallbackHandlerFactory;
import com.ibm.wsspi.security.auth.callback.WSServletRequestCallback;
import com.ibm.wsspi.security.auth.callback.WSServletResponseCallback;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;

/**
 *
 */
@SuppressWarnings("unchecked")
public class WSCallbackHandlerFactoryImplTest {

    private static Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static String userName = "testUser";
    private static String password = "testUserPwd";
    private static String realmName = "testRealm";
    private static HttpServletRequest httpRequest = mockery.mock(HttpServletRequest.class);
    private static HttpServletResponse httpResponse = mockery.mock(HttpServletResponse.class);
    private static Map appContext = new HashMap();
    private static X509Certificate certificate = mockery.mock(X509Certificate.class);
    private static X509Certificate[] certificateChain = new X509Certificate[1];
    private static final String CRED_TOKEN_STRING = "A test credential token.";
    private static byte[] testCredToken = CRED_TOKEN_STRING.getBytes();
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
        certificateChain[0] = certificate;
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
    public void testGetCallbackHandlerWithUserNameAndPassword() throws Exception {
        final String methodName = "testGetCallbackHandlerWithUserNameAndPassword";
        try {
            Callback[] callbacks = new Callback[2];
            callbacks[0] = new NameCallback("User name:");
            callbacks[1] = new PasswordCallback("User password:", false);
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
            CallbackHandler callbackHandler = factory.getCallbackHandler(userName, password);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { userName, password });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetCallbackHandlerWithUserNameRealmNameAndPassword() throws Exception {
        final String methodName = "testGetCallbackHandlerWithUserNameRealmNameAndPassword";
        try {
            Callback[] callbacks = new Callback[3];
            callbacks[0] = new NameCallback("User name:");
            callbacks[1] = new PasswordCallback("User password:", false);
            callbacks[2] = new WSRealmNameCallbackImpl("Realm name:");
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
            CallbackHandler callbackHandler = factory.getCallbackHandler(userName, realmName, password);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { userName, password, realmName });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetCallbackHandlerWithUserNameRealmNamePasswordRequestResponseAndContext() throws Exception {
        final String methodName = "testGetCallbackHandlerWithUserNameRealmNamePasswordRequestResponseAndContext";
        try {
            Callback[] callbacks = new Callback[6];
            callbacks[0] = new NameCallback("User name:");
            callbacks[1] = new PasswordCallback("User password:", false);
            callbacks[2] = new WSRealmNameCallbackImpl("Realm name:");
            callbacks[3] = new WSServletRequestCallback("Servlet request:");
            callbacks[4] = new WSServletResponseCallback("Servler response:");
            callbacks[5] = new WSAppContextCallback("Application context:");
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
            CallbackHandler callbackHandler = factory.getCallbackHandler(userName, realmName, password, httpRequest, httpResponse, appContext);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { userName, password, realmName, httpRequest, httpResponse, appContext });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetCallbackHandlerWithRealmNameAndX509CertificateChain() throws Exception {
        final String methodName = "testGetCallbackHandlerWithRealmNameAndX509CertificateChain";
        try {
            Callback[] callbacks = new Callback[2];
            callbacks[0] = new WSRealmNameCallbackImpl("Realm name:");
            callbacks[1] = new WSX509CertificateChainCallback("X509 certificate chain array:");
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
            CallbackHandler callbackHandler = factory.getCallbackHandler(realmName, certificateChain);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { realmName, certificateChain });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetCallbackHandlerWithRealmNameX509CertificateChainRequestResponseAndContext() throws Exception {
        final String methodName = "testGetCallbackHandlerWithRealmNameX509CertificateChainRequestResponseAndContext";
        try {
            Callback[] callbacks = new Callback[5];
            callbacks[0] = new WSRealmNameCallbackImpl("Realm name:");
            callbacks[1] = new WSX509CertificateChainCallback("X509 certificate chain array:");
            callbacks[2] = new WSServletRequestCallback("Servlet request:");
            callbacks[3] = new WSServletResponseCallback("Servler response:");
            callbacks[4] = new WSAppContextCallback("Application context:");
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
            CallbackHandler callbackHandler = factory.getCallbackHandler(realmName, certificateChain, httpRequest, httpResponse, appContext);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { realmName, certificateChain, httpRequest, httpResponse, appContext });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetCallbackHandlerWithCredToken() throws Exception {
        final String methodName = "testGetCallbackHandlerWithCredToken";
        try {
            Callback[] callbacks = new Callback[1];
            callbacks[0] = new WSCredTokenCallbackImpl("Credential token:");
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
            CallbackHandler callbackHandler = factory.getCallbackHandler(testCredToken);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { CRED_TOKEN_STRING });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetCallbackHandlerWithCredTokenAndAuthMechOID() throws Exception {
        final String methodName = "testGetCallbackHandlerWithCredTokenAndAuthMechOID";
        try {
            Callback[] callbacks = new Callback[2];
            callbacks[0] = new WSCredTokenCallbackImpl("Credential token:");
            callbacks[1] = new WSAuthMechOidCallbackImpl("Auth mech OID:");
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
            CallbackHandler callbackHandler = factory.getCallbackHandler(testCredToken, TEST_OID);
            callbackHandler.handle(callbacks);
            CallbacksAssertionHelper.assertCallbacksValues(callbacks, new Object[] { CRED_TOKEN_STRING, TEST_OID });
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
