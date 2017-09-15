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
package com.ibm.ws.security.authentication.jaas.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class HashtableLoginModuleTest extends LoginModuleTester {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private static final String USER_ID = "userId";
    private static final String PASSWORD = "password";
    private static final String UNIQUE_ID = "uniqueId";
    private static final String SECURITY_NAME = "securityName";
    private static final String TEST_REALM = "realm";
    private static final String ANOTHER_SECURITY_NAME = "securityName";
    private static final String CUSTOM_CACHE_KEY = "customCacheKey";
    private static final String UNAUTHENTICATED = "UNAUTHENTICATED";

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mock.mock(ServiceReference.class, "Test" + JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY
                                                                                                                             + "Ref");
    private final ServiceReference<AuthenticationService> authenticationServiceRef = mock.mock(ServiceReference.class, "authenticationServiceRef");
    private final ServiceReference<UserRegistryService> userRegistryServiceRef = mock.mock(ServiceReference.class, "userRegistryServiceRef");
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final ServiceReference<CredentialsService> credentialsServiceRef = mock.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = mock.mock(CredentialsService.class);
    private final JAASServiceImpl jaasServiceCollab = new JAASServiceImpl();
    private final CallbackHandler callbackHandler = mock.mock(CallbackHandler.class);
    private final WSCredential wsCredential = new WSCredentialImpl(TEST_REALM, SECURITY_NAME, SECURITY_NAME, "UNAUTHENTICATED", "primaryUniqueGroupAccessId", SECURITY_NAME, null, null);
    private final SingleSignonToken ssoToken = mock.mock(SingleSignonToken.class, "ssoToken");
    private final AuthenticationService authenticationService = mock.mock(AuthenticationService.class);

    private final String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                                        AttributeNameConstants.WSCREDENTIAL_USERID,
                                                        AttributeNameConstants.WSCREDENTIAL_SECURITYNAME,
                                                        AttributeNameConstants.WSCREDENTIAL_CACHE_KEY,
                                                        AuthenticationConstants.INTERNAL_ASSERTION_KEY };

    public HashtableLoginModuleTest() {
        principalOverride = new WSPrincipal(ANOTHER_SECURITY_NAME, ANOTHER_SECURITY_NAME, WSPrincipal.AUTH_METHOD_HASH_TABLE);
        wsCredentialOverride = new WSCredentialImpl(TEST_REALM, ANOTHER_SECURITY_NAME, ANOTHER_SECURITY_NAME, "UNAUTHENTICATED", "primaryUniqueGroupAccessId", ANOTHER_SECURITY_NAME, null, null);
        ssoTokenOverride = mock.mock(SingleSignonToken.class, "ssoTokenOverride");
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
                allowing(ssoToken).getExpiration();
                will(returnValue(0L));
                allowing(ssoTokenOverride).getExpiration();
                will(returnValue(0L));
                allowing(cc).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));
            }
        });
        jaasServiceCollab.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasServiceCollab.setUserRegistryService(userRegistryServiceRef);
        jaasServiceCollab.setCredentialsService(credentialsServiceRef);
        jaasServiceCollab.setAuthenticationService(authenticationService);
        jaasServiceCollab.activate(cc, null);

        outputMgr.trace("*=all");
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
        jaasServiceCollab.deactivate(cc);
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#getRequiredCallbacks(javax.security.auth.callback.CallbackHandler)}.
     *
     * This module uses no callbacks.
     */
    @Test
    public void getRequiredCallbacks() throws Exception {
        HashtableLoginModule module = new HashtableLoginModule();
        assertNull(module.getRequiredCallbacks(callbackHandler));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * If there is no Hashtable in either the shared state OR the subject,
     * skip this login module.
     */
    @Test
    public void login_ignoredNoHashtableInSharedStateNullSubject() throws Exception {
        HashtableLoginModule module = new HashtableLoginModule();
        Subject subject = null;
        Map<String, Object> sharedState = new HashMap<String, Object>();
        module.initialize(subject, callbackHandler, sharedState, null);
        assertFalse(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * If there is no Hashtable in either the shared state OR the subject,
     * skip this login module.
     */
    @Test
    public void login_ignoredNoHashtableInSharedStateOrSubject() throws Exception {
        HashtableLoginModule module = new HashtableLoginModule();
        Subject subject = new Subject();
        Map<String, Object> sharedState = new HashMap<String, Object>();
        module.initialize(subject, callbackHandler, sharedState, null);
        assertFalse(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * If there is no Hashtable in either the shared state OR the subject,
     * skip this login module.
     */
    @Test
    public void login_ignoredNoUniqueIdOrSecurityName() throws Exception {
        HashtableLoginModule module = new HashtableLoginModule();
        Subject subject = null;
        Map<String, Object> sharedState = new HashMap<String, Object>();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        sharedState.put(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, hashtable);

        module.initialize(subject, callbackHandler, sharedState, null);
        assertFalse(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * If there is a Hashtable that contains the unique ID and user ID properties,
     * in either the shared state OR the subject, assert the identity.
     */
    @Test
    public void login_succeedFromSharedState() throws Exception {
        Subject subject = null;
        Map<String, Object> sharedState = new HashMap<String, Object>();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, UNIQUE_ID);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, SECURITY_NAME);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, CUSTOM_CACHE_KEY);
        sharedState.put(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, hashtable);

        HashtableLoginModule module = createInitializedModule(subject, sharedState);
        assertTrue(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * If there is a Hashtable that contains the unique ID and user ID properties,
     * in either the shared state OR the subject, assert the identity.
     */
    @Test
    public void login_succeedFromSubjectPublicCredentials() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = new HashMap<String, Object>();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, UNIQUE_ID);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, SECURITY_NAME);
        subject.getPublicCredentials().add(new Hashtable<String, Object>());
        subject.getPublicCredentials().add(hashtable);
        subject.getPublicCredentials().add(new Hashtable<String, Object>());

        HashtableLoginModule module = createInitializedModule(subject, sharedState);
        assertTrue(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * If there is a Hashtable that contains the unique ID and security name properties,
     * in either the shared state OR the subject, assert the identity.
     */
    @Test
    public void login_succeedFromSubjectPrivateCredentials() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = new HashMap<String, Object>();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, UNIQUE_ID);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, SECURITY_NAME);
        subject.getPublicCredentials().add(new Hashtable<String, Object>());
        subject.getPrivateCredentials().add(hashtable);
        subject.getPublicCredentials().add(new Hashtable<String, Object>());

        HashtableLoginModule module = createInitializedModule(subject, sharedState);
        assertTrue(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * test identity assertion succesful login (userId without password) with the INTERNAL_ASSERTION_KEY set to true
     */
    @Test
    public void login_succeedIdentityAssertion_internal_assertion_key() throws Exception {
        HashtableLoginModule module = new HashtableLoginModule();
        Subject subject = new Subject();
        Map<String, Object> sharedState = new HashMap<String, Object>();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, true);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, USER_ID);
        subject.getPublicCredentials().add(hashtable);

        module.initialize(subject, callbackHandler, sharedState, null);
        mock.checking(new Expectations() {
            {
                allowing(credentialsService).setCredentials(with(any(Subject.class)));
                one(userRegistry).getUniqueUserId(USER_ID);
                will(returnValue(UNIQUE_ID));
                allowing(userRegistry).getRealm();
                will(returnValue(TEST_REALM));
                allowing(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                allowing(authenticationService).isAllowHashTableLoginWithIdOnly();
                will(returnValue(false));
            }
        });
        assertTrue(module.login());
        SubjectHelper subjectHelper = new SubjectHelper();
        Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(subject, hashtableLoginProperties);
        assertTrue(customProperties == null || customProperties.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * test identity assertion succesful login (userId without password) with allowHashTableLoginWIthIdOnly set to true
     */
    @Test
    public void login_succeedIdentityAssertion_allowHashTableLoginWithIdOnly() throws Exception {
        HashtableLoginModule module = new HashtableLoginModule();
        Subject subject = new Subject();
        Map<String, Object> sharedState = new HashMap<String, Object>();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, USER_ID);
        sharedState.put(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, hashtable);

        module.initialize(subject, callbackHandler, sharedState, null);
        mock.checking(new Expectations() {
            {
                allowing(credentialsService).setCredentials(with(any(Subject.class)));
                one(userRegistry).getUniqueUserId(USER_ID);
                will(returnValue(UNIQUE_ID));
                allowing(userRegistry).getRealm();
                will(returnValue(TEST_REALM));
                allowing(credentialsService).getUnauthenticatedUserid();
                will(returnValue(UNAUTHENTICATED));
                allowing(authenticationService).isAllowHashTableLoginWithIdOnly();
                will(returnValue(true));
            }
        });
        assertTrue(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * If there is a Hashtable that contains the user ID and password properties,
     * in either the shared state OR the subject, assert the identity.
     */
    @Test
    public void login_succeedOnlyUserIdWithPassword() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = createSharedState();
        HashtableLoginModule module = createInitializedModule(subject, sharedState);

        assertTrue(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * If there is a Hashtable that contains the user ID and password properties,
     * in either the shared state OR the subject, assert the identity.
     */
    @Test
    public void login_failedPasswordLogin() throws Exception {
        HashtableLoginModule module = new HashtableLoginModule();
        Subject subject = new Subject();
        Map<String, Object> sharedState = createSharedState();

        module.initialize(subject, callbackHandler, sharedState, null);

        mock.checking(new Expectations() {
            {
                one(userRegistry).checkPassword(USER_ID, PASSWORD);
                will(returnValue(null));
            }
        });

        try {
            module.login();
            fail("");
        } catch (AuthenticationException e) {
            assertEquals("Recieved wrong exception message",
                         "CWWKS1100A: Authentication did not succeed for user ID userId. An invalid user ID or password was specified.",
                         e.getMessage());
            assertTrue("Expected audit message was not logged",
                       outputMgr.checkForStandardOut("CWWKS1100A: Authentication did not succeed for user ID userId. An invalid user ID or password was specified."));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#login()}.
     *
     * If there is a Hashtable that contains the user ID and password properties,
     * in either the shared state OR the subject, assert the identity.
     */
    @Test(expected = LoginException.class)
    public void login_errorPasswordLogin() throws Exception {
        HashtableLoginModule module = new HashtableLoginModule();
        Subject subject = new Subject();
        Map<String, Object> sharedState = createSharedState();

        module.initialize(subject, callbackHandler, sharedState, null);

        mock.checking(new Expectations() {
            {
                one(userRegistry).checkPassword(USER_ID, PASSWORD);
                will(throwException(new RegistryException("Expected")));
            }
        });

        module.login();
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#commit()}.
     */
    @Test
    public void commit_noLogin() throws Exception {
        HashtableLoginModule module = new HashtableLoginModule();
        assertFalse(module.commit());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#commit()}.
     */
    @Test
    public void commit_successfulLoginUsingUniqueIdAndSecurityName() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = new HashMap<String, Object>();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, UNIQUE_ID);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, SECURITY_NAME);
        sharedState.put(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, hashtable);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, CUSTOM_CACHE_KEY);
        HashtableLoginModule module = createInitializedModule(subject, sharedState);

        assertTrue(module.login());
        assertTrue(module.commit());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#commit()}.
     * In the case of just uniqueId only, the hashtableLoginModule returns false for
     * login() and commit() methods.
     */
    @Test
    public void loginUsingUniqueIdOnly() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = new HashMap<String, Object>();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, UNIQUE_ID);
        sharedState.put(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, hashtable);
        HashtableLoginModule module = createInitializedModule(subject, sharedState);

        assertFalse(module.login());
        assertFalse(module.commit());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#commit()}.
     */
    @Test
    public void commit_successfulLoginUsingUserId() throws Exception {
        Subject subject = new Subject();
        Map<String, Object> sharedState = createSharedState();
        HashtableLoginModule module = createInitializedModule(subject, sharedState);

        assertTrue(module.login());
        assertTrue(module.commit());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#abort()}.
     */
    @Test
    public void abort() {
        HashtableLoginModule module = new HashtableLoginModule();
        assertTrue(module.abort());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule#logout()}.
     */
    @Test
    public void logout() {
        HashtableLoginModule module = new HashtableLoginModule();
        assertTrue(module.logout());
    }

    /** {@inheritDoc} */
    @Override
    protected HashtableLoginModule createInitializedModule(Subject subject, Map<String, Object> sharedState) throws Exception {
        final HashtableLoginModule module = new HashtableLoginModule();
        module.initialize(subject, callbackHandler, sharedState, null);

        mock.checking(new Expectations() {
            {
                allowing(ssoToken).addAttribute(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, CUSTOM_CACHE_KEY);
                allowing(userRegistry).getRealm();
                will(returnValue(TEST_REALM));
                allowing(userRegistry).getType();
                will(returnValue("mock"));
                allowing(userRegistry).getUniqueUserId(SECURITY_NAME);
                will(returnValue(UNIQUE_ID));
                allowing(userRegistry).getUniqueUserId(UNIQUE_ID);
                will(returnValue(UNIQUE_ID));
                allowing(userRegistry).checkPassword(USER_ID, PASSWORD);
                will(returnValue(UNIQUE_ID));
                allowing(userRegistry).getUserSecurityName(UNIQUE_ID);
                will(returnValue(UNIQUE_ID));

                allowing(credentialsService).setCredentials(with(any(Subject.class)));
                will(new Action() {

                    @Override
                    public Object invoke(Invocation arg0) throws Throwable {
                        module.temporarySubject.getPublicCredentials().add(wsCredential);
                        module.temporarySubject.getPrivateCredentials().add(ssoToken);
                        return null;
                    }

                    @Override
                    public void describeTo(Description arg0) {}
                });
            }
        });

        return module;
    }

    /** {@inheritDoc} */
    @Override
    protected Map<String, Object> createSharedState() {
        Map<String, Object> sharedState = new HashMap<String, Object>();
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, USER_ID);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_PASSWORD, PASSWORD);
        sharedState.put(AttributeNameConstants.WSCREDENTIAL_PROPERTIES_KEY, hashtable);
        return sharedState;
    }

}
