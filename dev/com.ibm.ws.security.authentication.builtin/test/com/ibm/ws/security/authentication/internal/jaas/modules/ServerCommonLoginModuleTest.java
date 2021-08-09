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
package com.ibm.ws.security.authentication.internal.jaas.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.CertificateAuthenticator;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * Test the common login module logic
 */
@SuppressWarnings("unchecked")
public class ServerCommonLoginModuleTest {
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<UserRegistryService> userRegistryServiceRef = mock.mock(ServiceReference.class, "userRegistryServiceRef");
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final ServiceReference<CredentialsService> credentialsServiceRef = mock.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = new TestCredentialsService();
    private final ServiceReference<TokenManager> tokenManagerRef = mock.mock(ServiceReference.class, "tokenManagerRef");
    private final TokenManager tokenManager = mock.mock(TokenManager.class);
    private final AuthenticationService authenticationService = mock.mock(AuthenticationService.class);
    private final JAASServiceImpl jaasServiceCollab = new JAASServiceImpl();
    private final CallbackHandler callbackHandler = mock.mock(CallbackHandler.class);
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mock.mock(ServiceReference.class, "Test" + JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY
                                                                                                                             + "Ref");
    protected final ConcurrentServiceReferenceMap<String, CertificateAuthenticator> certificateAuthenticators =
                    new ConcurrentServiceReferenceMap<String, CertificateAuthenticator>(JAASServiceImpl.KEY_CERT_AUTHENTICATOR + "s");

    class TestCredentialsService implements CredentialsService {

        /** {@inheritDoc} */
        @Override
        public void setCredentials(Subject subject) throws CredentialException {
            if ("THROW_EXCEPTION".equals(subject.getPrincipals().iterator().next().getName()))
                throw new CredentialException("Expected");
            subject.getPublicCredentials().add(new Object());
            subject.getPrivateCredentials().add(new Object());
        }

        /** {@inheritDoc} */
        @Override
        public void setUnauthenticatedUserid(String unauthenticatedUser) {}

        /** {@inheritDoc} */
        @Override
        public String getUnauthenticatedUserid() {
            return "UNAUTHENTICATED";
        }

        /** {@inheritDoc} */
        @Override
        public boolean isSubjectValid(Subject subject) {
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.security.credentials.CredentialsService#setBasicAuthCredential(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void setBasicAuthCredential(Subject subject, String realm, String username, String password) throws CredentialException {
            // TODO Auto-generated method stub
        }
    }

    class TestLoginModule extends ServerCommonLoginModule {

        /** {@inheritDoc} */
        @Override
        public Callback[] getRequiredCallbacks(CallbackHandler callbackHandler) throws IOException, UnsupportedCallbackException {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean login() throws LoginException {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean commit() throws LoginException {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean abort() throws LoginException {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean logout() throws LoginException {
            return false;
        }
    }

    @SuppressWarnings("static-access")
    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                // This expectation is to allow the JAAS stuff to activate
                // with no fuss.
                allowing(cc).getBundleContext();

                allowing(cc).getProperties();
                will(returnValue(new Hashtable<String, Object>()));
                allowing(cc).locateService(JAASServiceImpl.KEY_USER_REGISTRY_SERVICE, userRegistryServiceRef);
                will(returnValue(userRegistryService));
                allowing(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
                allowing(cc).locateService(JAASServiceImpl.KEY_CREDENTIALS_SERVICE, credentialsServiceRef);
                will(returnValue(credentialsService));
                allowing(cc).locateService(JAASServiceImpl.KEY_TOKEN_MANAGER, tokenManagerRef);
                will(returnValue(tokenManager));
                allowing(cc).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));
            }
        });
        jaasServiceCollab.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasServiceCollab.setUserRegistryService(userRegistryServiceRef);
        jaasServiceCollab.setCredentialsService(credentialsServiceRef);
        jaasServiceCollab.setTokenManager(tokenManagerRef);
        jaasServiceCollab.setAuthenticationService(authenticationService);
        JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
        jaasServiceCollab.activate(cc, null);
    }

    /**
     * The cleanup of the JAASServiceImpl is necessary as the
     * references it holds are static, and if not cleaned up, will spill
     * over into the next test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        jaasServiceCollab.unsetUserRegistryService(userRegistryServiceRef);
        jaasServiceCollab.unsetCredentialsService(credentialsServiceRef);
        jaasServiceCollab.unsetTokenManager(tokenManagerRef);
        jaasServiceCollab.deactivate(cc);
        mock.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)}
     * .
     */
    @Test
    public void initialize() {
        TestLoginModule module = new TestLoginModule();
        Subject subject = new Subject();
        Map<String, Object> sharedState = new HashMap<String, Object>();
        Map<String, Object> options = new HashMap<String, Object>();
        module.initialize(subject, callbackHandler, sharedState, options);
        assertSame("The subject was not the same subject set during initialize",
                   subject, module.subject);
        assertSame("The callbackHandler was not the same callbackHandler set during initialize",
                   callbackHandler, module.callbackHandler);
        assertSame("The sharedState was not the same sharedState set during initialize",
                   sharedState, module.sharedState);
        assertSame("The optoins was not the same options set during initialize",
                   options, module.options);
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule#getUserRegistry()}.
     */
    @Test
    public void getUserRegistry() throws Exception {
        TestLoginModule module = new TestLoginModule();
        assertSame("Did not get back the expected userRegistry object",
                   userRegistry, module.getUserRegistry());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule#getTokenManager()}.
     */
    @Test
    public void getTokenManager() {
        TestLoginModule module = new TestLoginModule();
        assertSame("Did not get back the expected tokenManager object",
                   tokenManager, module.getTokenManager());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule#getCredentialsService()}.
     */
    @Test
    public void getCredentialsService() {
        TestLoginModule module = new TestLoginModule();
        assertSame("Did not get back the expected credentialsService object",
                   credentialsService, module.getCredentialsService());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule#getAuthenticationService()}.
     */
    @Test
    public void getAuthenticationService() {
        TestLoginModule module = new TestLoginModule();
        assertSame("Did not get back the expected authenticationService object",
                   authenticationService, module.getAuthenticationService());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule#setUpSubject(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void setUpSubject() throws Exception {
        Subject subject = new Subject();
        TestLoginModule module = new TestLoginModule();
        module.initialize(subject, null, null, null);

        module.setUpSubject("userName", "accessId", "authMethod");
        assertEquals("Principals were not set, but should have been",
                     1, subject.getPrincipals().size());
        assertEquals("Public credentials were not set, but should have been",
                     1, subject.getPublicCredentials().size());
        assertEquals("Prviate credentials were not set, but should have been",
                     1, subject.getPrivateCredentials().size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule#setUpSubject(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test(expected = LoginException.class)
    public void setUpSubject_error() throws Exception {
        Subject subject = new Subject();
        TestLoginModule module = new TestLoginModule();
        module.initialize(subject, null, null, null);

        module.setUpSubject("THROW_EXCEPTION", "accessId", "authMethod");
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule#cleanUpSubject()}.
     */
    @Test
    public void cleanUpSubject_notSetup() {
        Subject subject = new Subject();
        subject.getPrincipals().add(new WSPrincipal("userName", "accessId", "authMethod"));
        subject.getPublicCredentials().add(new Object());
        subject.getPrivateCredentials().add(new Object());
        TestLoginModule module = new TestLoginModule();
        module.initialize(subject, null, null, null);

        module.cleanUpSubject();
        assertEquals("Principals were removed but should not have been",
                     1, subject.getPrincipals().size());
        assertEquals("Public credentials were removed but should not have been",
                     1, subject.getPublicCredentials().size());
        assertEquals("Prviate credentials were removed but should not have been",
                     1, subject.getPrivateCredentials().size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.internal.jaas.modules.ServerCommonLoginModule#cleanUpSubject()}.
     */
    @Test
    public void testCleanUpSubject_setup() throws Exception {
        Subject subject = new Subject();
        TestLoginModule module = new TestLoginModule();
        module.initialize(subject, null, new HashMap<String, Object>(), null);

        module.setUpSubject("userName", "accessId", "authMethod");
        assertEquals("Principals were not set, but should have been",
                     1, subject.getPrincipals().size());
        assertEquals("Public credentials were not set, but should have been",
                     1, subject.getPublicCredentials().size());
        assertEquals("Prviate credentials were not set, but should have been",
                     1, subject.getPrivateCredentials().size());

        module.cleanUpSubject();
        assertEquals("Principals were not removed but should have been",
                     0, subject.getPrincipals().size());
        assertEquals("Public credentials were nt removed but should have been",
                     0, subject.getPublicCredentials().size());
        assertEquals("Prviate credentials were not removed but should have been",
                     0, subject.getPrivateCredentials().size());
    }

}
