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
package com.ibm.ws.security.jaas.common.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
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

import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.jaas.common.callback.CallbackHandlerAuthenticationData;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;
import com.ibm.wsspi.security.auth.callback.WSCallbackHandlerFactory;
import com.ibm.wsspi.security.auth.callback.WSServletRequestCallback;
import com.ibm.wsspi.security.auth.callback.WSServletResponseCallback;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;

/**
 *
 */
public class CallbackHandlerAuthenticationDataTest {
    private static Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private static SharedOutputManager outputMgr;
    private static final String USER = "user";
    private static final String USER_PWD = "userpwd";
    private static final String REALM_NAME = "testRealm";
    private static X509Certificate[] CERT_CHAIN;
    private static Map testContext = new HashMap();
    private static final String CRED_TOKEN_STRING = "A test credential token.";
    private static byte[] testCredToken = CRED_TOKEN_STRING.getBytes();

    private static X509Certificate[] certificateChain = new X509Certificate[1];

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
        InputStream inStream = new FileInputStream("publish" + File.separator + "certificates" + File.separator + "gooduser.cer");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate goodcert = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();
        CERT_CHAIN = new X509Certificate[] { goodcert, null };
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
            CallbackHandler callbackHandler = createCallbackHandler(USER, USER_PWD);
            CallbackHandlerAuthenticationData callbackHandlerAuthenticationData = new CallbackHandlerAuthenticationData(callbackHandler);
            AuthenticationData authenticationData = callbackHandlerAuthenticationData.createAuthenticationData();
            assertNotNull("There must be an AuthenticationData", authenticationData);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAuthDataForUserAndPWD() {
        final String methodName = "testAuthDataForUserAndPWD";
        try {
            CallbackHandler callbackHandler = createCallbackHandler(USER, USER_PWD);
            CallbackHandlerAuthenticationData callbackHandlerAuthenticationData = new CallbackHandlerAuthenticationData(callbackHandler);
            AuthenticationData authenticationData = callbackHandlerAuthenticationData.createAuthenticationData();
            assertEquals("User name is user", USER, authenticationData.get(AuthenticationData.USERNAME));
            assertNotNull(authenticationData.get(AuthenticationData.PASSWORD).toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAuthDataForToken() {
        final String methodName = "testAuthDataForToken";
        try {
            CallbackHandler callbackHandler = createCallbackHandler(testCredToken);
            CallbackHandlerAuthenticationData callbackHandlerAuthenticationData = new CallbackHandlerAuthenticationData(callbackHandler);
            AuthenticationData authenticationData = callbackHandlerAuthenticationData.createAuthenticationData();
            assertNotNull(authenticationData.get(AuthenticationData.TOKEN));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAuthDataForCert() {
        final String methodName = "testAuthDataForCert";
        try {
            CallbackHandler callbackHandler = createCertCallbackHandler();
            CallbackHandlerAuthenticationData callbackHandlerAuthenticationData = new CallbackHandlerAuthenticationData(callbackHandler);
            AuthenticationData authenticationData = callbackHandlerAuthenticationData.createAuthenticationData();
            assertNotNull(authenticationData.get(AuthenticationData.CERTCHAIN));
            assertEquals("Realm name is testRealm", REALM_NAME, authenticationData.get(AuthenticationData.REALM));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAuthDataForOthersCallback() {
        final String methodName = "testAuthDataForWSServletRequest";
        try {
            Mockery mockery = new JUnit4Mockery();
            HttpServletRequest httpRequest = mockery.mock(HttpServletRequest.class);
            HttpServletResponse httpResponse = mockery.mock(HttpServletResponse.class);

            Callback[] callbacks = new Callback[6];
            callbacks[0] = new NameCallback("User name:");
            callbacks[1] = new WSRealmNameCallbackImpl("Realm name:");
            callbacks[2] = new PasswordCallback("User password:", false);
            callbacks[3] = new WSServletRequestCallback("Servlet request:");
            callbacks[4] = new WSServletResponseCallback("Servler response:");
            callbacks[5] = new WSAppContextCallback("Application context:");
            WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
            CallbackHandler callbackHandler = factory.getCallbackHandler(USER, REALM_NAME, USER_PWD, httpRequest, httpResponse, testContext);
            callbackHandler.handle(callbacks);

            CallbackHandlerAuthenticationData callbackHandlerAuthenticationData = new CallbackHandlerAuthenticationData(callbackHandler);
            AuthenticationData authenticationData = callbackHandlerAuthenticationData.createAuthenticationData();
            assertEquals("User name is user", USER, authenticationData.get(AuthenticationData.USERNAME));
            assertEquals("Realm name is testRealm", REALM_NAME, authenticationData.get(AuthenticationData.REALM));
            assertNotNull(authenticationData.get(AuthenticationData.PASSWORD).toString());
            assertEquals(httpRequest, authenticationData.get(AuthenticationData.HTTP_SERVLET_REQUEST));
            assertNotNull(authenticationData.get(AuthenticationData.HTTP_SERVLET_RESPONSE));
            assertNotNull(authenticationData.get(AuthenticationData.APPLICATION_CONTEXT));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private static CallbackHandler createCallbackHandler(String username, String password) throws Exception {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("User name:");
        callbacks[1] = new PasswordCallback("User password:", false);
        WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
        CallbackHandler callbackHandler = factory.getCallbackHandler(username, password);
        callbackHandler.handle(callbacks);
        return callbackHandler;
    }

    private static CallbackHandler createCallbackHandler(byte[] token) throws Exception {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new WSCredTokenCallbackImpl("Credential token:");
        WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
        CallbackHandler callbackHandler = factory.getCallbackHandler(testCredToken);
        callbackHandler.handle(callbacks);
        return callbackHandler;
    }

    private static CallbackHandler createCertCallbackHandler() throws Exception {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new WSRealmNameCallbackImpl("Realm name:");
        callbacks[1] = new WSX509CertificateChainCallback("X509 certificate chain array:");
        WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
        CallbackHandler callbackHandler = factory.getCallbackHandler(REALM_NAME, certificateChain);
        callbackHandler.handle(callbacks);
        return callbackHandler;
    }
}
