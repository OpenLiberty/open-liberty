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
package com.ibm.wsspi.security.common.auth.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
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

import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 *
 */
@SuppressWarnings("unchecked")
public class IdentityAssertionLoginModuleTest {

    private static SharedOutputManager outputMgr;
    private static final String CERTIFICATE_USER_NAME = "certificateUserName";
    private static Principal principal = new Principal() {

        @Override
        public String getName() {
            return "testRealm/testUser";
        }
    };

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final X509Certificate certificate = mockery.mock(X509Certificate.class);
    private final X509Certificate[] certificateChain = new X509Certificate[] { certificate };
    private final X509Certificate[] emptyCertificateChain = new X509Certificate[0];

    private final ComponentContext cc = mockery.mock(ComponentContext.class);
    private final ServiceReference<UserRegistryService> userRegistryServiceRef = mockery.mock(ServiceReference.class, "userRegistryServiceRef");
    private final UserRegistryService userRegistryService = mockery.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mockery.mock(UserRegistry.class);
    private final ServiceReference<CredentialsService> credentialsServiceRef = mockery.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = mockery.mock(CredentialsService.class);
    private final AuthenticationService authenticationService = mockery.mock(AuthenticationService.class);
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mockery.mock(ServiceReference.class, "Test" + JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY
                                                                                                                                + "Ref");
    private final JAASServiceImpl jaasServiceCollab = new JAASServiceImpl();
    private Subject authenticatedSubject;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @SuppressWarnings("static-access")
    @Before
    public void setUp() throws Exception {
        authenticatedSubject = createAuthenticatedSubject();

        mockery.checking(new Expectations() {
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
                allowing(cc).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));
                allowing(authenticationService).authenticate(with(JaasLoginConfigConstants.SYSTEM_DEFAULT), with(new SubjectContainingHashtableMatcher()));
                will(returnValue(authenticatedSubject));
                allowing(authenticationService).isAllowHashTableLoginWithIdOnly();
                will(returnValue(false));
            }
        });
        jaasServiceCollab.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasServiceCollab.setUserRegistryService(userRegistryServiceRef);
        jaasServiceCollab.setCredentialsService(credentialsServiceRef);
        jaasServiceCollab.setAuthenticationService(authenticationService);
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
        jaasServiceCollab.deactivate(cc);
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test(expected = WSLoginFailedException.class)
    public void loginNoMapThrowsWSLoginFailedException() throws Throwable {
        IdentityAssertionLoginModule module = new IdentityAssertionLoginModule();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = null;
        Map sharedState = new HashMap();
        Map options = null;
        module.initialize(subject, callbackHandler, sharedState, options);
        module.login();
    }

    @Test(expected = WSLoginFailedException.class)
    public void loginNotTrustedThrowsWSLoginFailedException() throws Exception {
        IdentityAssertionLoginModule module = new IdentityAssertionLoginModule();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = null;
        Map sharedState = createSharedState(false, principal, certificateChain);
        Map options = null;
        module.initialize(subject, callbackHandler, sharedState, options);
        module.login();
    }

    @Test(expected = WSLoginFailedException.class)
    public void loginTrustedNoPrincipalNoCertChainThrowsWSLoginFailedException() throws Exception {
        IdentityAssertionLoginModule module = new IdentityAssertionLoginModule();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = null;
        Map sharedState = createSharedState(true, null, null);
        Map options = null;
        module.initialize(subject, callbackHandler, sharedState, options);
        module.login();
    }

    @Test(expected = WSLoginFailedException.class)
    public void loginTrustedEmptyCertChainThrowsWSLoginFailedException() throws Exception {
        IdentityAssertionLoginModule module = new IdentityAssertionLoginModule();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = null;
        Map sharedState = createSharedState(true, null, emptyCertificateChain);
        Map options = null;
        module.initialize(subject, callbackHandler, sharedState, options);
        module.login();
    }

    @Test
    public void loginTrustedWithPrincipalPasses() throws Exception {
        final String methodName = "loginTrustedWithPrincipalPasses";
        try {
            IdentityAssertionLoginModule module = new IdentityAssertionLoginModule();
            Subject subject = new Subject();
            CallbackHandler callbackHandler = null;
            Map sharedState = createSharedState(true, principal, null);
            Map options = null;
            module.initialize(subject, callbackHandler, sharedState, options);
            assertTrue(module.login());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void loginTrustedWithCertificateChainPasses() throws Exception {
        final String methodName = "loginTrustedWithCertificateChainPasses";
        try {
            mockery.checking(new Expectations() {
                {
                    one(userRegistry).mapCertificate(certificateChain[0]);
                    will(returnValue(CERTIFICATE_USER_NAME));
                }
            });
            IdentityAssertionLoginModule module = new IdentityAssertionLoginModule();
            Subject subject = new Subject();
            CallbackHandler callbackHandler = null;
            Map sharedState = createSharedState(true, null, certificateChain);
            Map options = null;
            module.initialize(subject, callbackHandler, sharedState, options);
            assertTrue(module.login());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void commitSetsSubject() throws Exception {
        final String methodName = "commitSetsSubject";
        try {
            Subject subject = new Subject();
            Map sharedState = createSharedState(true, principal, null);
            IdentityAssertionLoginModule module = createInitializedModule(subject, sharedState);
            module.login();
            module.commit();
            assertTrue("The subject must contain all the principals in the temporary subject",
                       subject.getPrincipals().containsAll(module.temporarySubject.getPrincipals()));
            assertTrue("The subject must contain all the public credentials in the temporary subject",
                       subject.getPublicCredentials().containsAll(module.temporarySubject.getPublicCredentials()));
            assertTrue("The subject must contain all the private credentials in the temporary subject",
                       subject.getPrivateCredentials().containsAll(module.temporarySubject.getPrivateCredentials()));
            assertSubjectPrincipalsAndCredentialsSize(subject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void abort() throws Exception {
        final String methodName = "abort";
        try {
            Subject subject = new Subject();
            Map sharedState = createSharedState(true, principal, null);
            IdentityAssertionLoginModule module = createInitializedModule(subject, sharedState);
            module.login();
            module.commit();
            module.abort();

            assertEquals("Principals were not removed but should have been",
                         0, subject.getPrincipals().size());
            assertEquals("Public credentials were nt removed but should have been",
                         0, subject.getPublicCredentials().size());
            assertEquals("Private credentials were not removed but should have been",
                         0, subject.getPrivateCredentials().size());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void logout() throws Exception {
        final String methodName = "logout";
        try {
            Subject subject = new Subject();
            Map sharedState = createSharedState(true, principal, null);
            IdentityAssertionLoginModule module = createInitializedModule(subject, sharedState);
            module.login();
            module.commit();
            module.logout();

            assertEquals("Principals were not removed but should have been",
                         0, subject.getPrincipals().size());
            assertEquals("Public credentials were nt removed but should have been",
                         0, subject.getPublicCredentials().size());
            assertEquals("Private credentials were not removed but should have been",
                         0, subject.getPrivateCredentials().size());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private void assertSubjectPrincipalsAndCredentialsSize(Subject subject) {
        assertEquals("The subject must contain only one WSPrincipal",
                     1, subject.getPrincipals(WSPrincipal.class).size());
        assertEquals("The subject must contain only one WSCredential",
                     1, subject.getPublicCredentials(WSCredential.class).size());
    }

    private Map createSharedState(Boolean trusted, Principal principal, X509Certificate[] certificateChain) {
        Map trustState = createTrustState(trusted, principal, certificateChain);
        Map sharedState = new HashMap();
        sharedState.put("com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.state", trustState);
        return sharedState;
    }

    private Map createTrustState(Boolean trusted, Principal principal, X509Certificate[] certificateChain) {
        Map trustState = new HashMap();
        trustState.put("com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.trusted", trusted);
        trustState.put("com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.principal", principal);
        trustState.put("com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.certificates", certificateChain);
        return trustState;
    }

    private IdentityAssertionLoginModule createInitializedModule(Subject subject, Map sharedState) {
        IdentityAssertionLoginModule module = new IdentityAssertionLoginModule();
        module.initialize(subject, null, sharedState, null);
        return module;
    }

    private Subject createAuthenticatedSubject() {
        Subject authenticatedSubject = new Subject();
        authenticatedSubject.getPrincipals().add(new WSPrincipal("userName", "userName", WSPrincipal.AUTH_METHOD_HASH_TABLE));
        authenticatedSubject.getPublicCredentials().add(
                                                        new WSCredentialImpl("realmName", "userName", "userName", "UNAUTHENTICATED", "primaryUniqueGroupAccessId", "userName", null, null));
        authenticatedSubject.getPrivateCredentials().add(mockery.mock(SingleSignonToken.class));
        return authenticatedSubject;
    }

    class SubjectContainingHashtableMatcher extends TypeSafeMatcher<Subject> {

        private final String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                                           AttributeNameConstants.WSCREDENTIAL_USERID,
                                                           AttributeNameConstants.WSCREDENTIAL_SECURITYNAME,
                                                           AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };

        /** {@inheritDoc} */
        @Override
        public boolean matchesSafely(Subject subject) {
            boolean matches = false;
            SubjectHelper subjectHelper = new SubjectHelper();
            Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(subject, hashtableLoginProperties);
            if (customProperties != null && customProperties.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID) &&
                customProperties.containsKey(AuthenticationConstants.INTERNAL_ASSERTION_KEY)) {
                matches = true;
            }
            return matches;
        }

        /** {@inheritDoc} */
        @Override
        public void describeTo(Description description) {}
    }

}
