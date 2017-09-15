/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.client.internal.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.CredentialException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.internal.CredentialsServiceImpl;
import com.ibm.ws.security.credentials.wscred.BasicAuthCredentialProvider;
import com.ibm.wsspi.security.auth.callback.WSCallbackHandlerFactory;

/**
 *
 */
public class ClientAuthenticationServiceTest {
    protected static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    ClientAuthenticationService authnClientService;
    final String user = "user1";
    final String password = "password";
    final ProtectedString passwordProtected = new ProtectedString(password.toCharArray());

    private final ComponentContext componentContext = mock.mock(ComponentContext.class);
    private final ServiceReference<CredentialsService> credentialsServiceReference = mock.mock(ServiceReference.class, "credentialsServiceReference");
    private CredentialsServiceImpl credentialsService;
    private final CredentialsService credentialsServiceMock = mock.mock(CredentialsService.class);
    private final ServiceReference<CredentialProvider> basicAuthCredentialProviderRef = mock.mock(ServiceReference.class, "basicAuthCredentialProviderRef");
    private final CredentialProvider basicAuthCredentialProvider = new BasicAuthCredentialProvider();

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

    }

    @Before
    public void setUp() {
        authnClientService = new ClientAuthenticationService();

        credentialsService = new CredentialsServiceImpl();
        credentialsService.setBasicAuthCredentialProvider(basicAuthCredentialProviderRef);
        credentialsService.activate(componentContext);
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
        mock.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.client.internal.authentication.ClientAuthenticationService#authenticate(javax.security.auth.callback.CallbackHandler, javax.security.auth.Subject)}
     * .
     * 
     * @throws Exception
     */
    @Test
    public void testAuthenticate() throws Exception {
        final String pwd = "user1pwd";
        CallbackHandler callbackHandler = createCallbackHandler(user, pwd);

        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(ClientAuthenticationService.KEY_CREDENTIALS_SERVICE, credentialsServiceReference);
                will(returnValue(credentialsService));
                allowing(componentContext).locateService(CredentialsServiceImpl.KEY_BASIC_AUTH_CREDENTIAL_PROVIDER, basicAuthCredentialProviderRef);
                will(returnValue(basicAuthCredentialProvider));
            }
        });

        authnClientService.setCredentialsService(credentialsServiceReference);
        credentialsService.setBasicAuthCredentialProvider(basicAuthCredentialProviderRef);
        authnClientService.activate(componentContext);

        Subject subject = authnClientService.authenticate(callbackHandler, null);
        Set<Object> publicCreds = subject.getPublicCredentials();
        Iterator<Object> credIter = publicCreds.iterator();
        WSCredential wsCred = (WSCredential) credIter.next();

        Set<WSPrincipal> principals = subject.getPrincipals(WSPrincipal.class);
        WSPrincipal wsPrincipal = principals.iterator().next();

        assertTrue("The WSCredential in the client subject should be basic auth", wsCred.isBasicAuth());
        assertEquals("The WSCredential's security name  in the client subject should be " + user, user, wsCred.getSecurityName());
        assertTrue("The WSPrincipal's access Id in the client subject should be null", wsPrincipal.getAccessId() == null);
        assertEquals("The WSPrincipal's name in the client subject should be " + user, user, wsPrincipal.getName());
    }

    @Test(expected = CredentialException.class)
    public void testCreateBasicAuthSubject_invalidCredential() throws CredentialException, WSLoginFailedException {
        final Subject emptySubject = new Subject();
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, user);
        authenticationData.set(AuthenticationData.PASSWORD, passwordProtected);
        final CredentialException ce = new CredentialException();
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(ClientAuthenticationService.KEY_CREDENTIALS_SERVICE, credentialsServiceReference);
                will(returnValue(credentialsServiceMock));
                allowing(credentialsServiceMock).setBasicAuthCredential(emptySubject, null, user, password.toString());
                will(throwException(ce));
            }
        });

        authnClientService.setCredentialsService(credentialsServiceReference);
        authnClientService.activate(componentContext);

        authnClientService.createBasicAuthSubject(authenticationData, emptySubject);
    }

    @Test(expected = WSLoginFailedException.class)
    public void testCreateBasicAuthSubject_nullUser() throws CredentialException, WSLoginFailedException {
        final Subject emptySubject = new Subject();
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, null);

        authnClientService.activate(componentContext);
        authnClientService.createBasicAuthSubject(authenticationData, emptySubject);
    }

    @Test(expected = WSLoginFailedException.class)
    public void testCreateBasicAuthSubject_nullPassword() throws CredentialException, WSLoginFailedException {
        final Subject emptySubject = new Subject();
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, user);

        authnClientService.activate(componentContext);
        authnClientService.createBasicAuthSubject(authenticationData, emptySubject);
    }

    @Test(expected = WSLoginFailedException.class)
    public void testCreateBasicAuthSubject_noCallback() throws WSLoginFailedException, CredentialException {
        final Subject emptySubject = new Subject();
        authnClientService.authenticate(null, emptySubject);
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
}
