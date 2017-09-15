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
package com.ibm.ws.security.authentication.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.internal.cache.keyproviders.BasicAuthCacheKeyProvider;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.security.auth.callback.WSCallbackHandlerFactory;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@SuppressWarnings("unchecked")
public class AuthenticationServiceTest {

    private static final String REALM = "REALM";
    private static final String DOWNSTREAM_REALM = "downstreamRealm";
    private static final String GOOD_USER = "testuser";
    private static final String GOOD_USER_PWD = "testuserpwd";

    private static SharedOutputManager outputMgr;
    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private AuthenticationServiceImpl authenticationServiceImpl;
    private final JAASServiceImpl jaasService = mockery.mock(JAASServiceImpl.class);
    private final String jaasEntryName = "TestLogin";
    private AuthenticationData authenticationData;
    private CallbackHandler callbackHandler;
    private final ComponentContext componentContext = mockery.mock(ComponentContext.class);
    private final ServiceReference<AuthCacheService> authCacheServiceReference = mockery.mock(ServiceReference.class, "authCacheServiceReference");
    private final AuthCacheService authCacheService = mockery.mock(AuthCacheService.class);
    private final ServiceReference<UserRegistryService> userRegistryServiceReference = mockery.mock(ServiceReference.class, "userRegistryServiceReference");
    private final ServiceReference<CredentialsService> credentialsServiceReference = mockery.mock(ServiceReference.class, "credentialsServiceReference");
    private final UserRegistryService userRegistryService = mockery.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mockery.mock(UserRegistry.class);
    private final CredentialsService credentialsService = mockery.mock(CredentialsService.class);

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        authenticationServiceImpl = createActivatedAuthenticationServiceImpl(new HashMap<String, Object>());
        authenticationData = createAuthenticationData(GOOD_USER, GOOD_USER_PWD);
        callbackHandler = createCallbackHandler(GOOD_USER, GOOD_USER_PWD);
        setTestJaasConfiguration();
        createCommonMockery();
        createRegistryExpectations();
    }

    private void createCommonMockery() {
        mockery.checking(new Expectations() {
            {
                allowing(componentContext).locateService(AuthenticationServiceImpl.KEY_AUTH_CACHE_SERVICE, authCacheServiceReference);
                will(returnValue(authCacheService));
                allowing(componentContext).locateService(AuthenticationServiceImpl.KEY_USER_REGISTRY_SERVICE, userRegistryServiceReference);
                will(returnValue(userRegistryService));
                allowing(componentContext).locateService(AuthenticationServiceImpl.KEY_CREDENTIALS_SERVICE, credentialsServiceReference);
                will(returnValue(credentialsService));
            }
        });
    }

    private void createRegistryExpectations() throws RegistryException {
        mockery.checking(new Expectations() {
            {
                allowing(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
                allowing(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
                allowing(userRegistry).getRealm();
                will(returnValue(REALM));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            AuthenticationService authenticationService = new AuthenticationServiceImpl();
            assertNotNull(authenticationService);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * If the JAAS service is unavailable, throw an AuthenticationException
     * and log the error.
     */
    @Test
    public void authenticateNoJAASService() throws Exception {
        AuthenticationServiceImpl authenticationServiceImpl = new AuthenticationServiceImpl();

        try {
            try {
                authenticationServiceImpl.authenticate(null, (AuthenticationData) null, null);
                fail("Expected an AuthenticationException as there is no JAAS service available");
            } catch (AuthenticationException e) {
                assertEquals("Recieved wrong exception message",
                             "CWWKS1000E: The JAAS Service is unavailable.",
                             e.getMessage());
                assertTrue("Expected error message was not logged", outputMgr.checkForStandardErr("CWWKS1000E: The JAAS Service is unavailable."));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * If the JAAS service is available, invoke it to authenticate the request.
     */
    @Test
    public void authenticateJAASServiceAvailableUsingAuthenticationData() throws Exception {
        noSubjectInAuthCache();
        subjectInsertedAfterAuthentication();
        mustDoJaasLogin(jaasEntryName, authenticationData, null);

        assertNotNull("There must be an authenticated subject",
                      authenticationServiceImpl.authenticate(jaasEntryName, authenticationData, null));
    }

    private void noSubjectInAuthCache() {
        mockery.checking(new Expectations() {
            {
                one(authCacheService).getSubject(with(any(Object.class)));
                will(returnValue(null));
            }
        });
    }

    private void subjectInsertedAfterAuthentication() {
        mockery.checking(new Expectations() {
            {
                allowing(authCacheService).insert(with(any(Subject.class)), with(GOOD_USER), with(GOOD_USER_PWD));
                allowing(authCacheService).insert(with(any(Subject.class)));
            }
        });
    }

    private void mustDoJaasLogin(final String jaasEntryName, final AuthenticationData authenticationData, final Subject partialSubject) throws Exception {
        final Subject authenticatedSubject = new Subject();
        mockery.checking(new Expectations() {
            {
                one(jaasService).performLogin(jaasEntryName, authenticationData, partialSubject);
                will(returnValue(authenticatedSubject));
            }
        });
    }

    private void mustDoJaasLogin(final String jaasEntryName, final CallbackHandler callbackHandler, final Subject partialSubject) throws Exception {
        final Subject authenticatedSubject = new Subject();
        mockery.checking(new Expectations() {
            {
                one(jaasService).performLogin(jaasEntryName, callbackHandler, partialSubject);
                will(returnValue(authenticatedSubject));
            }
        });
    }

    /**
     * If the JAAS service is available, invoke it to authenticate the request.
     */
    @Test
    public void authenticateJAASServiceAvailableUsingCallbackHandler() throws Exception {
        noSubjectInAuthCache();
        subjectInsertedAfterAuthentication();
        mustDoJaasLogin(jaasEntryName, callbackHandler, null);

        assertNotNull("There must be an authenticated subject",
                      authenticationServiceImpl.authenticate(jaasEntryName, callbackHandler, null));
    }

    @Test
    public void authenticateAssertedUsingAuthenticationData() {
        final String methodName = "authenticateAssertedUsingAuthenticationData";
        try {
            subjectInsertedAfterAuthentication();

            final Subject partialSubject = new Subject();
            final Subject authenticatedSubject = new Subject();
            mockery.checking(new Expectations() {
                {
                    one(jaasService).performLogin(with(jaasEntryName), with(any(AuthenticationData.class)), with(partialSubject));
                    will(returnValue(authenticatedSubject));
                }
            });

            assertNotNull("There must be an authenticated subject", authenticationServiceImpl.authenticate(jaasEntryName, partialSubject));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void authenticateAssertedUsingCallbackHandler() {
        final String methodName = "authenticateAssertedUsingCallbackHandler";
        try {
            noSubjectInAuthCache();
            subjectInsertedAfterAuthentication();

            final Subject partialSubject = new Subject();
            final Subject authenticatedSubject = new Subject();
            mockery.checking(new Expectations() {
                {
                    one(jaasService).performLogin(with(jaasEntryName), with(any(CallbackHandler.class)), with(partialSubject));
                    will(returnValue(authenticatedSubject));
                }
            });

            assertNotNull("There must be an authenticated subject", authenticationServiceImpl.authenticate(jaasEntryName, callbackHandler, partialSubject));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void authenticateAssertedVerifySubjectNotFromAuthCache() {
        final String methodName = "authenticateAssertedVerifySubjectNotFromAuthCache";
        try {
            subjectInsertedAfterAuthentication();
            final Subject partialSubject = new Subject();
            final Subject authenticatedSubject = new Subject();
            mockery.checking(new Expectations() {
                {
                    one(jaasService).performLogin(with(jaasEntryName), with(any(AuthenticationData.class)), with(partialSubject));
                    will(returnValue(authenticatedSubject));
                }
            });

            assertNotNull("There must be an authenticated subject", authenticationServiceImpl.authenticate(jaasEntryName, partialSubject));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void authenticateAssertedInCache() {
        final String methodName = "authenticateAssertedInCache";
        try {
            final String cacheKey = BasicAuthCacheKeyProvider.createLookupKey(REALM, GOOD_USER);
            final Subject partialSubject = createPartialSubject(GOOD_USER, null);
            final Subject authenticatedSubject = new Subject();
            mockery.checking(new Expectations() {
                {
                    one(authCacheService).getSubject(cacheKey);
                    will(returnValue(authenticatedSubject));
                }
            });
            assertNotNull("The subject must be found from the cache.", authenticationServiceImpl.authenticate(jaasEntryName, partialSubject));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void authenticateSubjectUseridPasswordInCache() {
        final String methodName = "authenticateSubjectUseridPasswordInCache";
        try {
            final String cacheKey = BasicAuthCacheKeyProvider.createLookupKey(REALM, GOOD_USER, GOOD_USER_PWD);
            final Subject partialSubject = createPartialSubject(GOOD_USER, GOOD_USER_PWD);
            final Subject authenticatedSubject = new Subject();
            mockery.checking(new Expectations() {
                {
                    one(authCacheService).getSubject(cacheKey);
                    will(returnValue(authenticatedSubject));
                }
            });

            assertNotNull("The subject must be found from the cache.", authenticationServiceImpl.authenticate(jaasEntryName, partialSubject));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private Subject createPartialSubject(String username, String password) {
        Subject partialSubject = new Subject();
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, username);
        if (password == null) {
            hashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        } else {
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_PASSWORD, password);
        }

        partialSubject.getPublicCredentials().add(hashtable);
        return partialSubject;
    }

    @Test
    public void testSetJaasServiceSetsReferenceToAuthenticationService() throws Exception {
        final String methodName = "testSetJaasServiceSetsReferenceToAuthenticationService";
        try {
            assertSame("The authentication service must be set in the JAAS Service.", authenticationServiceImpl, JAASServiceImpl.getAuthenticationService());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testUnsetJaasServiceRemovesReferenceToAuthenticationService() throws Exception {
        final String methodName = "testUnsetJaasServiceRemovesReferenceToAuthenticationService";
        try {
            authenticationServiceImpl.unsetJaasService(jaasService);
            assertNull("The authentication service must not be set in the JAAS Service.", JAASServiceImpl.getAuthenticationService());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDeactivateRemovesReferenceToAuthenticationService() throws Exception {
        final String methodName = "testDeactivateRemovesReferenceToAuthenticationService";
        try {
            authenticationServiceImpl.deactivate();
            assertNull("The authentication service must not be set in the JAAS Service.", JAASServiceImpl.getAuthenticationService());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAuthCacheService() throws Exception {
        final String methodName = "testGetAuthCacheService";
        try {
            AuthCacheService actualAuthCacheService = authenticationServiceImpl.getAuthCacheService();

            assertNotNull("There must be an auth cache service.", actualAuthCacheService);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private AuthenticationServiceImpl createActivatedAuthenticationServiceImpl(Map<String, Object> props) {
        AuthenticationServiceImpl authenticationServiceImpl = new AuthenticationServiceImpl();
        authenticationServiceImpl.setAuthCacheService(authCacheServiceReference);
        authenticationServiceImpl.setJaasService(jaasService);
        authenticationServiceImpl.setUserRegistryService(userRegistryServiceReference);
        authenticationServiceImpl.setCredentialsService(credentialsServiceReference);
        authenticationServiceImpl.activate(componentContext, props);

        return authenticationServiceImpl;
    }

    @Test
    public void testGetAuthCacheService_Disabled() throws Exception {
        final String methodName = "testGetAuthCacheService_Disabled";
        try {
            Map<String, Object> newProperties = new HashMap<String, Object>();
            newProperties.put("cacheEnabled", false);
            AuthenticationServiceImpl authenticationServiceImpl = createActivatedAuthenticationServiceImpl(newProperties);

            AuthCacheService actualAuthCacheService = authenticationServiceImpl.getAuthCacheService();

            assertNull("There must not be an auth cache service.", actualAuthCacheService);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testModified() {
        final String methodName = "testModified";
        try {
            Map<String, Object> newProperties = new HashMap<String, Object>();
            AuthenticationServiceImpl authenticationServiceImpl = createActivatedAuthenticationServiceImpl(newProperties);
            AuthCacheService actualAuthCacheService = authenticationServiceImpl.getAuthCacheService();

            assertNotNull("There must be an auth cache service.", actualAuthCacheService);

            newProperties.put("cacheEnabled", false);
            authenticationServiceImpl.modified(newProperties);

            actualAuthCacheService = authenticationServiceImpl.getAuthCacheService();
            assertNull("There must not be an auth cache service.", actualAuthCacheService);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAuthenticationGuard() throws Exception {
        final String methodName = "testAuthenticationGuard";
        try {
            final Subject authenticatedSubject = new Subject();
            final String cacheKey = BasicAuthCacheKeyProvider.createLookupKey(REALM, GOOD_USER, GOOD_USER_PWD);
            final Action firstGetSubjectAction = new Action() {

                @Override
                public Object invoke(Invocation arg0) throws Throwable {
                    return null;
                }

                @Override
                public void describeTo(Description arg0) {}
            };

            final Action secondGetSubjectAction = new Action() {

                @Override
                public Object invoke(Invocation arg0) throws Throwable {
                    return authenticatedSubject;
                }

                @Override
                public void describeTo(Description arg0) {}
            };

            mockery.checking(new Expectations() {
                {
                    allowing(authCacheService).getSubject(cacheKey);
                    will(onConsecutiveCalls(firstGetSubjectAction, secondGetSubjectAction));
                    one(jaasService).performLogin(jaasEntryName, authenticationData, null);
                    will(returnValue(authenticatedSubject));
                    one(authCacheService).insert(with(any(Subject.class)), with(GOOD_USER), with(GOOD_USER_PWD));
                }
            });

            FutureTask<Subject> firstThreadTask = createStartedFutureTaskForAuthenticationServiceAccessor(authenticationServiceImpl);
            FutureTask<Subject> secondThreadTask = createStartedFutureTaskForAuthenticationServiceAccessor(authenticationServiceImpl);
            Subject firstThreadSubject = firstThreadTask.get();
            Subject secondThreadSubject = secondThreadTask.get();

            assertNotNull("There must be an authenticated subject.", firstThreadSubject);
            assertSame("The subject must be found in the authentication cache for one of the threads.", firstThreadSubject, secondThreadSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    // TODO: This might not be a valid test.  The login must return a subject or throw an Authentication Exception.
    @Test
    public void authenticateInvalidLTPAToken() throws Exception {
        noSubjectInAuthCache();
        subjectInsertedAfterAuthentication();
        final AuthenticationData badAuthData = new WSAuthenticationData();
        mockery.checking(new Expectations() {
            {
                one(jaasService).performLogin(jaasEntryName, badAuthData, null);
                will(returnValue(null));

            }
        });

        badAuthData.set(AuthenticationData.TOKEN64, "ABCD");

        assertNull("There must not be an authenticated subject",
                   authenticationServiceImpl.authenticate(jaasEntryName, badAuthData, null));
    }

    @Test
    public void authenticateBasicAuth() throws Exception {
        createCredentialsServiceExpectations();
        noJaasLoginAndNoCaching();

        WSCallbackHandlerFactory factory = WSCallbackHandlerFactory.getInstance();
        CallbackHandler callbackHandler = factory.getCallbackHandler(GOOD_USER, DOWNSTREAM_REALM, GOOD_USER_PWD);

        Subject subject = authenticationServiceImpl.authenticate(jaasEntryName, callbackHandler, new Subject());
        assertNotNull("There must be a basic auth subject", subject);
    }

    private void createCredentialsServiceExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(credentialsService).setBasicAuthCredential(with(any(Subject.class)), with(DOWNSTREAM_REALM), with(GOOD_USER), with(GOOD_USER_PWD));
            }
        });
    }

    private void noJaasLoginAndNoCaching() throws LoginException {
        mockery.checking(new Expectations() {
            {
                never(jaasService).performLogin(with(jaasEntryName), with(any(CallbackHandler.class)), with(any(Subject.class)));
                never(authCacheService).insert(with(any(Subject.class)));
                never(authCacheService).insert(with(any(Subject.class)), with(any(String.class)), with(any(String.class)));
            }
        });
    }

    private static void setTestJaasConfiguration() {
        Configuration testJaasConfiguration = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                AppConfigurationEntry[] appConfigurationEntry = null;
                if ("TestLogin".equals(name)) {
                    appConfigurationEntry = new AppConfigurationEntry[1];
                    Map<String, ?> options = new HashMap<String, Object>();
                    appConfigurationEntry[0] = new AppConfigurationEntry("com.ibm.ws.security.authentication.internal.modules.TestLoginModule", LoginModuleControlFlag.REQUIRED, options);
                }
                return appConfigurationEntry;
            }
        };
        Configuration.setConfiguration(testJaasConfiguration);
    }

    private static AuthenticationData createAuthenticationData(String username, String password) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, username);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        return authenticationData;
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

    private FutureTask<Subject> createStartedFutureTaskForAuthenticationServiceAccessor(AuthenticationService authenticationService) {
        Callable<Subject> authenticationServiceAccessor = new AuthenticationServiceAccessor(authenticationService);
        FutureTask<Subject> futureTask = new FutureTask<Subject>(authenticationServiceAccessor);
        Thread thread = new Thread(futureTask);
        thread.start();
        return futureTask;
    }

    private class AuthenticationServiceAccessor implements Callable<Subject> {

        AuthenticationService authenticationService;

        public AuthenticationServiceAccessor(AuthenticationService authenticationService) {
            this.authenticationService = authenticationService;
        }

        /** {@inheritDoc} */
        @Override
        public Subject call() throws Exception {
            AuthenticationData authenticationData = createAuthenticationData(GOOD_USER, GOOD_USER_PWD);
            return authenticationService.authenticate(jaasEntryName, authenticationData, null);
        }

    }

}