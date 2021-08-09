package com.ibm.ws.security.client.jaas.modules;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
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

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.clientcontainer.metadata.CallbackHandlerProvider;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.client.internal.authentication.ClientAuthenticationService;
import com.ibm.ws.security.client.internal.jaas.JAASClientConfigurationImpl;
import com.ibm.ws.security.client.internal.jaas.JAASClientService;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;

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

/**
 *
 */
public class WSClientLoginModuleImplTest {

    protected static SharedOutputManager outputMgr;

    private static final String USER_NAME = "userName";
    private static final String USER_PWD = "userPwd";
    private static final String TEST_REALM = "testRealm";
    private static final WSCredential wsCredential = new WSCredentialImpl(TEST_REALM, USER_NAME, USER_PWD);
    protected final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    protected JAASClientService jaasClientService = null;

    protected final Subject emptySubject = new Subject();
    protected WSCredential wsCredentialOverride;
    protected final ComponentContext cc = mock.mock(ComponentContext.class, "loginModuleCC");
    protected final ServiceReference<CallbackHandlerProvider> callbackHandlerProviderRef = mock.mock(ServiceReference.class, "myCallbackHandlerProviderRef");
    protected final CallbackHandlerProvider callbackHandlerProvider = mock.mock(CallbackHandlerProvider.class);
    protected final ServiceReference<ClientAuthenticationService> clientAuthenticationServiceRef = mock.mock(ServiceReference.class, "myClientAuthenticationServiceRef");
    protected final ClientAuthenticationService clientAuthenticationService = mock.mock(ClientAuthenticationService.class);
    CallbackHandler callbackHandlerFromLoginContextEntry = mock.mock(CallbackHandler.class, "callbackHandlerFromLoginContextEntry");

    private final ServiceReference<CredentialsService> credentialsServiceRef = mock.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = mock.mock(CredentialsService.class);
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mock.mock(ServiceReference.class, "my" + JAASClientService.KEY_JAAS_CONFIG_FACTORY
                                                                                                                             + "Ref");

    private WSClientLoginModuleImpl loginModule;

    CallbackHandler callbackHandlerFromDD = mock.mock(CallbackHandler.class, "callbackHandlerFromDD");

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
    public void setUp() throws AuthenticationException {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(JAASClientService.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));
                allowing(cc).locateService(JAASClientService.KEY_CLIENT_AUTHN_SERVICE, clientAuthenticationServiceRef);
                will(returnValue(clientAuthenticationService));
                allowing(cc).locateService(JAASClientService.KEY_CALLBACK_PROVIDER, callbackHandlerProviderRef);
                will(returnValue(callbackHandlerProvider));
            }
        });
        jaasClientService = new JAASClientService();
        jaasClientService.setClientAuthenticationService(clientAuthenticationServiceRef);
        jaasClientService.setCallbackHandlerProvider(callbackHandlerProviderRef);
        jaasClientService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasClientService.activate(cc, null);
        loginModule = new WSClientLoginModuleImpl();
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

    @Test
    public void testLogin_noCallbackHandlerInDD() throws LoginException {
        Map<String, ?> sharedState = new HashMap<String, Object>();
        Map<String, ?> options = new HashMap<String, Object>();
        mock.checking(new Expectations() {
            {
                one(callbackHandlerProvider).getCallbackHandler();
                will(returnValue(null));
                one(clientAuthenticationService).authenticate(callbackHandlerFromLoginContextEntry, emptySubject);
                will(returnValue(emptySubject));
            }
        });
        loginModule.initialize(emptySubject, callbackHandlerFromLoginContextEntry, sharedState, options);
        assertTrue(loginModule.login());
    }

    @Test
    public void testLogin_CallbackHandlerInDD() throws LoginException {
        Map<String, ?> sharedState = new HashMap<String, Object>();
        Map<String, ?> options = new HashMap<String, Object>();
        mock.checking(new Expectations() {
            {
                one(callbackHandlerProvider).getCallbackHandler();
                will(returnValue(callbackHandlerFromDD));
                one(clientAuthenticationService).authenticate(callbackHandlerFromDD, emptySubject);
                will(returnValue(emptySubject));
            }
        });
        loginModule.initialize(emptySubject, callbackHandlerFromLoginContextEntry, sharedState, options);
        assertTrue(loginModule.login());
    }

    @Test
    public void testLogin_IgnoreCallbackHandlerInDD() throws LoginException {
        Map<String, ?> sharedState = new HashMap<String, Object>();
        Map<String, Object> options = new HashMap<String, Object>();
        options.put(JAASClientConfigurationImpl.WAS_IGNORE_CLIENT_CONTAINER_DD, true);
        mock.checking(new Expectations() {
            {
                never(callbackHandlerProvider).getCallbackHandler();
                one(clientAuthenticationService).authenticate(callbackHandlerFromLoginContextEntry, emptySubject);
                will(returnValue(emptySubject));
            }
        });
        loginModule.initialize(emptySubject, callbackHandlerFromLoginContextEntry, sharedState, options);
        assertTrue(loginModule.login());
    }

    @Test
    public void abortCleansSubject() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = createSharedState();
        createInitializedModule(subject, sharedState);
        loginModule.login();
        loginModule.commit();
        loginModule.abort();

        assertTrue("The subject must not contain any principals", subject.getPrincipals().isEmpty());
        assertTrue("The subject must not contain any public credentials", subject.getPublicCredentials().isEmpty());
        assertTrue("The subject must not contain any private credentials", subject.getPrivateCredentials().isEmpty());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.client.jaas.modules.LoginModuleTester#createInitializedModule(javax.security.auth.Subject, java.util.Map)
     */
    protected void createInitializedModule(final Subject subject, Map<String, Object> sharedState) throws Exception {
        final CallbackHandler callbackHandler = new CallbackHandlerDouble(USER_NAME, USER_PWD);

        mock.checking(new Expectations() {
            {
                one(callbackHandlerProvider).getCallbackHandler();
                will(returnValue(callbackHandler));
                allowing(clientAuthenticationService).authenticate(callbackHandler, subject);
                will(returnValue(emptySubject));
                allowing(credentialsService).setCredentials(with(any(Subject.class)));
                will(new Action() {

                    @Override
                    public Object invoke(Invocation arg0) throws Throwable {
                        loginModule.temporarySubject.getPublicCredentials().add(wsCredential);
                        return null;
                    }

                    @Override
                    public void describeTo(Description arg0) {}
                });
            }
        });
        loginModule.initialize(subject, callbackHandler, sharedState, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.client.jaas.modules.LoginModuleTester#createSharedState()
     */
    private Map<String, Object> createSharedState() {
        return new HashMap<String, Object>();
    }

    private class CallbackHandlerDouble implements CallbackHandler {
        private final String name;
        private final String pwd;

        CallbackHandlerDouble(String name, String pwd) {
            this.name = name;
            this.pwd = pwd;
        }

        /** {@inheritDoc} */
        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            ((NameCallback) callbacks[0]).setName(name);
            if (pwd == null) {
                ((PasswordCallback) callbacks[1]).setPassword(null);
            } else {
                ((PasswordCallback) callbacks[1]).setPassword(pwd.toCharArray());
            }
        }

    }
}
