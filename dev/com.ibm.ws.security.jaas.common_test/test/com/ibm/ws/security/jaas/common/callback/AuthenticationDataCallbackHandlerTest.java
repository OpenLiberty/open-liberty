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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.security.auth.callback.WSAuthMechOidCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;
import com.ibm.wsspi.security.auth.callback.WSServletRequestCallback;
import com.ibm.wsspi.security.auth.callback.WSServletResponseCallback;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;

import test.common.SharedOutputManager;

/**
 *
 */
public class AuthenticationDataCallbackHandlerTest {
    private static SharedOutputManager outputMgr;
    private static final String GOOD_USER = "testuser";
    private static final String GOOD_USER_PWD = "testuserpwd";
    private static final String REALM_NAME = "testRealm";
    private static final byte[] GOOD_TOKEN = GOOD_USER.getBytes();
    private static X509Certificate[] GOOD_CERT_CHAIN;
    private static Map testContext = new HashMap();
    private static final String CRED_TOKEN_STRING = "A test credential token.";
    private static byte[] testCredToken = CRED_TOKEN_STRING.getBytes();
    private static final String TEST_OID = "testOID";

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
        GOOD_CERT_CHAIN = new X509Certificate[] { goodcert, null };
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
            AuthenticationData authenticationData = createAuthenticationData(GOOD_USER, GOOD_USER_PWD);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            assertNotNull("There must be a callback handler", callbackHandler);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testImplementsCallbackHandler() {
        final String methodName = "testImplementsCallbackHandler";
        try {
            AuthenticationData authenticationData = createAuthenticationData(GOOD_USER, GOOD_USER_PWD);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            assertTrue("It must be an instance of CallbackHandler", callbackHandler instanceof CallbackHandler);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsNameCallback() {
        final String methodName = "testHandleSetsNameCallback";
        try {
            AuthenticationData authenticationData = createAuthenticationData(GOOD_USER, GOOD_USER_PWD);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            NameCallback nameCallback = new NameCallback("Username: ");
            Callback[] callbacks = new Callback[10];
            callbacks[0] = nameCallback;
            callbackHandler.handle(callbacks);
            String nameRetrieved = nameCallback.getName();
            assertEquals("The name retrieved must be the same as the test name", GOOD_USER, nameRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsPasswordCallback() {
        final String methodName = "testHandleSetsPasswordCallback";
        try {
            AuthenticationData authenticationData = createAuthenticationData(GOOD_USER, GOOD_USER_PWD);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);
            Callback[] callbacks = new Callback[10];
            callbacks[0] = passwordCallback;
            callbackHandler.handle(callbacks);
            char[] passwordRetrieved = passwordCallback.getPassword();
            assertEquals("The password retrieved must be equals to the test password", GOOD_USER_PWD, String.valueOf(passwordRetrieved));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsTokenCallback() {
        final String methodName = "testHandleSetsTokenCallback";
        try {
            AuthenticationData authenticationData = createTokenAuthenticationData(GOOD_TOKEN);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            TokenCallback tokenCallback = new TokenCallback();
            Callback[] callbacks = new Callback[10];
            callbacks[0] = tokenCallback;
            callbackHandler.handle(callbacks);
            byte[] tokenRetrieved = tokenCallback.getToken();
            assertEquals("The token retrieved must be equals to the test token", GOOD_USER, new String(tokenRetrieved));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsCertCallback() {
        final String methodName = "testHandleSetsCertCallback";
        try {
            AuthenticationData authenticationData = createCertAuthenticationData(GOOD_CERT_CHAIN);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            WSX509CertificateChainCallback certCallback = new WSX509CertificateChainCallback(null);
            Callback[] callbacks = new Callback[10];
            callbacks[0] = certCallback;
            callbackHandler.handle(callbacks);
            X509Certificate[] certChainRetrieved = certCallback.getX509CertificateChain();
            assertArrayEquals("The certificate chain retrieved must be equals to the input chain", GOOD_CERT_CHAIN, certChainRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsRealmNameCallback() {
        final String methodName = "testHandleSetsRealmNameCallback";
        try {
            AuthenticationData authenticationData = new WSAuthenticationData();
            authenticationData.set(AuthenticationData.REALM, REALM_NAME);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            WSRealmNameCallbackImpl realmNameCallback = new WSRealmNameCallbackImpl("Realm name: ");
            Callback[] callbacks = new Callback[1];
            callbacks[0] = realmNameCallback;
            callbackHandler.handle(callbacks);
            String realmNameRetrieved = realmNameCallback.getRealmName();
            assertEquals("The realm name retrieved must be the same as the test realm name", REALM_NAME, realmNameRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsWSServletRequestCallback() {
        final String methodName = "testHandleSetsWSServletRequestCallback";
        try {
            Mockery mockery = new JUnit4Mockery();
            HttpServletRequest httpRequest = mockery.mock(HttpServletRequest.class);
            AuthenticationData authenticationData = new WSAuthenticationData();
            authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, httpRequest);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            WSServletRequestCallback servletRequestCallback = new WSServletRequestCallback("HTTP servlet request: ");
            Callback[] callbacks = new Callback[1];
            callbacks[0] = servletRequestCallback;
            callbackHandler.handle(callbacks);
            HttpServletRequest httpServletRequestRetrieved = servletRequestCallback.getHttpServletRequest();
            assertEquals("The servlet request retrieved must be the same as the test servlet request", httpRequest, httpServletRequestRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsWSServletResponseCallback() {
        final String methodName = "testHandleSetsWSServletResponseCallback";
        try {
            Mockery mockery = new JUnit4Mockery();
            HttpServletResponse httpResponse = mockery.mock(HttpServletResponse.class);
            AuthenticationData authenticationData = new WSAuthenticationData();
            authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, httpResponse);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            WSServletResponseCallback servletResponseCallback = new WSServletResponseCallback("HTTP servlet response: ");
            Callback[] callbacks = new Callback[1];
            callbacks[0] = servletResponseCallback;
            callbackHandler.handle(callbacks);
            HttpServletResponse httpServletResponseRetrieved = servletResponseCallback.getHttpServletResponse();
            assertEquals("The servlet response retrieved must be the same as the test servlet response", httpResponse, httpServletResponseRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleSetsWSAppContextCallback() {
        final String methodName = "testHandleSetsWSAppContextCallback";
        try {
            AuthenticationData authenticationData = new WSAuthenticationData();
            authenticationData.set(AuthenticationData.APPLICATION_CONTEXT, testContext);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            WSAppContextCallback appContextCallback = new WSAppContextCallback("Application context: ");
            Callback[] callbacks = new Callback[1];
            callbacks[0] = appContextCallback;
            callbackHandler.handle(callbacks);
            Map contextRetrieved = appContextCallback.getContext();
            assertEquals("The app context retrieved must be the same as the test app context", testContext, contextRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsWSCredTokenCallbackImpl() {
        final String methodName = "testHandleSetsWSCredTokenCallbackImpl";
        try {
            AuthenticationData authenticationData = new WSAuthenticationData();
            authenticationData.set(AuthenticationData.TOKEN, testCredToken);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            WSCredTokenCallbackImpl credTokenCallback = new WSCredTokenCallbackImpl("Credential token: ");
            Callback[] callbacks = new Callback[1];
            callbacks[0] = credTokenCallback;
            callbackHandler.handle(callbacks);
            byte[] credTokenRetrieved = credTokenCallback.getCredToken();
            assertEquals("The credential token retrieved must be the same as the test credential token", CRED_TOKEN_STRING, new String(credTokenRetrieved));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsWSCredToken64CallbackImpl() {
        final String methodName = "testHandleSetsWSCredTokenCallbackImpl";
        try {
            AuthenticationData authenticationData = new WSAuthenticationData();
            authenticationData.set(AuthenticationData.TOKEN64, Base64Coder.encode(testCredToken));
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            WSCredTokenCallbackImpl credTokenCallback = new WSCredTokenCallbackImpl("Credential token: ");
            Callback[] callbacks = new Callback[1];
            callbacks[0] = credTokenCallback;
            callbackHandler.handle(callbacks);
            byte[] credTokenRetrieved = credTokenCallback.getCredToken();
            assertEquals("The credential token retrieved must be the same as the test credential token", CRED_TOKEN_STRING, new String(credTokenRetrieved));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleSetsWSAuthMechOidCallbackImpl() {
        final String methodName = "testHandleSetsWSAuthMechOidCallbackImpl";
        try {
            AuthenticationData authenticationData = new WSAuthenticationData();
            authenticationData.set(AuthenticationData.AUTHENTICATION_MECH_OID, TEST_OID);
            AuthenticationDataCallbackHandler callbackHandler = new AuthenticationDataCallbackHandler(authenticationData);
            WSAuthMechOidCallbackImpl authMechCallback = new WSAuthMechOidCallbackImpl("Authentication mechanism OID: ");
            Callback[] callbacks = new Callback[1];
            callbacks[0] = authMechCallback;
            callbackHandler.handle(callbacks);
            String authMechOID = authMechCallback.getAuthMechOid();
            assertEquals("The authentication mechanism retrieved must be the same as the test authentication mechanism", TEST_OID, authMechOID);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private static AuthenticationData createAuthenticationData(String username, String password) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, username);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        return authenticationData;
    }

    private static AuthenticationData createTokenAuthenticationData(byte[] token) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.TOKEN, token);
        return authenticationData;
    }

    private static AuthenticationData createCertAuthenticationData(X509Certificate[] certs) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.CERTCHAIN, certs);
        return authenticationData;
    }
}
