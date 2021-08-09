/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.jaas.modules;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

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
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.SingleSignonToken;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class TokenLoginModuleTest extends LoginModuleTester {

    private static final String USER_NAME = "userName";
    private static final String TEST_REALM = "testRealm";
    private static final String ANOTHER_USER_NAME = "anotherUserName";
    private static final String ACCESS_ID = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, TEST_REALM, USER_NAME);
    private static final WSCredential wsCredential = new WSCredentialImpl(TEST_REALM, USER_NAME, USER_NAME, "UNAUTHENTICATED", "primaryUniqueGroupAccessId", ACCESS_ID, null, null);
    private static final byte[] ssoTokenBytes = "These are the mock SSO token bytes".getBytes();
    private final String[] removeAttrs = new String[] {};
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<UserRegistryService> userRegistryServiceRef = mock.mock(ServiceReference.class, "userRegistryServiceRef");
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final ServiceReference<TokenManager> tokenManagerRef = mock.mock(ServiceReference.class, "tokenManagerRef");
    private final TokenManager tokenManager = mock.mock(TokenManager.class);
    private final ServiceReference<CredentialsService> credentialsServiceRef = mock.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = mock.mock(CredentialsService.class);
    private final JAASServiceImpl jaasService = new JAASServiceImpl();
    private final SingleSignonToken ssoToken = mock.mock(SingleSignonToken.class);
    private final Token token = mock.mock(Token.class);
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mock.mock(ServiceReference.class, "Test" + JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY
                                                                                                                             + "Ref");

    public TokenLoginModuleTest() {
        principalOverride = new WSPrincipal(ANOTHER_USER_NAME, ANOTHER_USER_NAME, WSPrincipal.AUTH_METHOD_TOKEN);
        wsCredentialOverride = new WSCredentialImpl(TEST_REALM, ANOTHER_USER_NAME, ANOTHER_USER_NAME, "UNAUTHENTICATED", "primaryUniqueGroupAccessId", ANOTHER_USER_NAME, null, null);
        ssoTokenOverride = mock.mock(SingleSignonToken.class, "ssoTokenOverride");
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
                allowing(cc).getProperties();
                will(returnValue(new Hashtable()));
                allowing(cc).locateService(JAASServiceImpl.KEY_USER_REGISTRY_SERVICE, userRegistryServiceRef);
                will(returnValue(userRegistryService));
                allowing(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
                allowing(cc).locateService(JAASServiceImpl.KEY_TOKEN_MANAGER, tokenManagerRef);
                will(returnValue(tokenManager));
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
        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasService.setUserRegistryService(userRegistryServiceRef);
        jaasService.setCredentialsService(credentialsServiceRef);
        jaasService.setTokenManager(tokenManagerRef);
        jaasService.activate(cc, null);
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
        jaasService.unsetUserRegistryService(userRegistryServiceRef);
        jaasService.deactivate(cc);
        outputMgr.resetStreams();
        mock.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    private class CallbackHandlerDouble implements CallbackHandler {

        private final byte[] ssoTokenBytes;

        CallbackHandlerDouble(byte[] ssoTokenBytes) {
            this.ssoTokenBytes = ssoTokenBytes;
        }

        /** {@inheritDoc} */
        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            ((WSCredTokenCallbackImpl) callbacks[0]).setCredToken(ssoTokenBytes);
        }

    }

    @Override
    protected TokenLoginModule createInitializedModule(Subject subject, Map<String, Object> sharedState) throws Exception {
        CallbackHandler callbackHandler = new CallbackHandlerDouble(ssoTokenBytes);
        final TokenLoginModule module = new TokenLoginModule();
        module.initialize(subject, callbackHandler, sharedState, null);

        mock.checking(new Expectations() {
            {
                allowing(userRegistry).getRealm();
                will(returnValue(TEST_REALM));
                allowing(userRegistry).getType();
                will(returnValue("mock"));
                allowing(userRegistry).getUserSecurityName(USER_NAME);
                will(returnValue(USER_NAME));
                allowing(tokenManager).recreateTokenFromBytes(with(ssoTokenBytes), with(removeAttrs));
                will(returnValue(token));
                allowing(token).getAttributes("u");
                will(returnValue(new String[] { ACCESS_ID }));
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
