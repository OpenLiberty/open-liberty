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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

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
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.security.token.SingleSignonToken;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class UsernameAndPasswordLoginModuleTest extends LoginModuleTester {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private static final String USER_NAME = "userName";
    private static final String USER_PWD = "userPwd";
    private static final String TEST_REALM = "testRealm";
    private static final String ANOTHER_USER_NAME = "anotherUserName";
    private static final String ACCESS_ID = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, TEST_REALM, USER_NAME);
    private static final String ANOTHER_ACCESS_ID = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, TEST_REALM, ANOTHER_USER_NAME);
    private static final WSCredential wsCredential = new WSCredentialImpl(TEST_REALM, USER_NAME, USER_NAME, "UNAUTHENTICATED", "primaryUniqueGroupAccessId", ACCESS_ID, null, null);

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<UserRegistryService> userRegistryServiceRef = mock.mock(ServiceReference.class, "userRegistryServiceRef");
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final ServiceReference<CredentialsService> credentialsServiceRef = mock.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = mock.mock(CredentialsService.class);
    private final JAASServiceImpl jaasServiceCollab = new JAASServiceImpl();
    private final SingleSignonToken ssoToken = mock.mock(SingleSignonToken.class, "ssoToken");
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mock.mock(ServiceReference.class, "Test" + JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY
                                                                                                                             + "Ref");

    @Rule
    public TestName testName = new TestName();

    public UsernameAndPasswordLoginModuleTest() {
        principalOverride = new WSPrincipal(ANOTHER_USER_NAME, ANOTHER_ACCESS_ID, WSPrincipal.AUTH_METHOD_PASSWORD);
        wsCredentialOverride = new WSCredentialImpl(TEST_REALM, ANOTHER_USER_NAME, ANOTHER_USER_NAME, "UNAUTHENTICATED", "primaryUniqueGroupAccessId", ANOTHER_ACCESS_ID, null, null);
        ssoTokenOverride = mock.mock(SingleSignonToken.class, "ssoTokenOverride");
    }

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
                allowing(cc).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));
                allowing(ssoToken).getExpiration();
                will(returnValue(0L));
                allowing(ssoTokenOverride).getExpiration();
                will(returnValue(0L));
            }
        });
        jaasServiceCollab.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasServiceCollab.setUserRegistryService(userRegistryServiceRef);
        jaasServiceCollab.setCredentialsService(credentialsServiceRef);
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
        mock.assertIsSatisfied();
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

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.UsernameAndPasswordLoginModule#login()}.
     */
    @Test
    public void loginNoUser() throws Exception {
        CallbackHandler callbackHandler = new CallbackHandlerDouble(null, USER_PWD);
        UsernameAndPasswordLoginModule module = new UsernameAndPasswordLoginModule();
        module.initialize(null, callbackHandler, null, null);
        assertFalse(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.UsernameAndPasswordLoginModule#login()}.
     */
    @Test
    public void loginNoPassword() throws Exception {
        CallbackHandler callbackHandler = new CallbackHandlerDouble(USER_NAME, null);
        UsernameAndPasswordLoginModule module = new UsernameAndPasswordLoginModule();
        module.initialize(null, callbackHandler, null, null);
        assertFalse(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.UsernameAndPasswordLoginModule#login()}.
     */
    @Test
    public void loginFailsIssuesAuditMessage() throws Exception {
        mock.checking(new Expectations() {
            {
                one(userRegistry).checkPassword(USER_NAME, USER_PWD);
                will(returnValue(null));
            }
        });

        CallbackHandler callbackHandler = new CallbackHandlerDouble(USER_NAME, USER_PWD);
        UsernameAndPasswordLoginModule module = new UsernameAndPasswordLoginModule();
        module.initialize(null, callbackHandler, null, null);

        try {
            module.login();
        } catch (AuthenticationException e) {
            assertEquals("Recieved wrong exception message",
                         "CWWKS1100A: Authentication did not succeed for user ID userName. An invalid user ID or password was specified.",
                         e.getMessage());

            assertTrue("Expected audit message was not logged",
                       outputMgr.checkForStandardOut("CWWKS1100A: Authentication did not succeed for user ID userName. An invalid user ID or password was specified."));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.UsernameAndPasswordLoginModule#login()}.
     */
    @Test
    public void loginPasses() throws Exception {
        Map<String, Object> sharedState = createSharedState();
        UsernameAndPasswordLoginModule module = createInitializedModule(null, sharedState);
        assertTrue(module.login());
    }

    @Test(expected = com.ibm.ws.security.authentication.PasswordExpiredException.class)
    public void checkPasswordExpiredLogin() throws Exception {

        Map<String, Object> sharedState = createSharedState();
        CallbackHandler callbackHandler = new CallbackHandlerDouble(USER_NAME, USER_PWD);
        final UsernameAndPasswordLoginModule module = new UsernameAndPasswordLoginModule();
        module.initialize(null, callbackHandler, sharedState, null);

        mock.checking(new Expectations() {
            {
                one(userRegistry).checkPassword(USER_NAME, USER_PWD);
                will(throwException(new com.ibm.ws.security.registry.PasswordExpiredException("test")));
            }
        });

        module.login();
    }

    @Test(expected = com.ibm.ws.security.authentication.UserRevokedException.class)
    public void checkUserRevokedLogin() throws Exception {
        Map<String, Object> sharedState = createSharedState();
        CallbackHandler callbackHandler = new CallbackHandlerDouble(USER_NAME, USER_PWD);
        final UsernameAndPasswordLoginModule module = new UsernameAndPasswordLoginModule();
        module.initialize(null, callbackHandler, sharedState, null);

        mock.checking(new Expectations() {
            {
                one(userRegistry).checkPassword(USER_NAME, USER_PWD);
                will(throwException(new com.ibm.ws.security.registry.UserRevokedException("test")));
            }
        });

        module.login();
    }

    @Override
    protected UsernameAndPasswordLoginModule createInitializedModule(Subject subject, Map<String, Object> sharedState) throws Exception {
        CallbackHandler callbackHandler = new CallbackHandlerDouble(USER_NAME, USER_PWD);
        final UsernameAndPasswordLoginModule module = new UsernameAndPasswordLoginModule();
        module.initialize(subject, callbackHandler, sharedState, null);

        mock.checking(new Expectations() {
            {
                one(userRegistry).checkPassword(USER_NAME, USER_PWD);
                will(returnValue(USER_NAME));
                allowing(userRegistry).getRealm();
                will(returnValue(TEST_REALM));
                allowing(userRegistry).getType();
                will(returnValue("mock"));
                allowing(userRegistry).getUniqueUserId(USER_NAME);
                will(returnValue(USER_NAME));
                allowing(userRegistry).getUserSecurityName(USER_NAME);
                will(returnValue(USER_NAME));
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
        return new HashMap<String, Object>();
    }
}
