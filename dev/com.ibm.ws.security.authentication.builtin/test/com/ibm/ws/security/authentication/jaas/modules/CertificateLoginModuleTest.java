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
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.x500.X500Principal;

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

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.collective.CollectiveAuthenticationPlugin;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;
import com.ibm.wsspi.security.token.SingleSignonToken;

import test.common.SharedOutputManager;

/**
 *
 */
public class CertificateLoginModuleTest extends LoginModuleTester {

    private static final String USER_NAME = "userName";
    private static final String TEST_REALM = "testRealm";
    private static final String ANOTHER_USER_NAME = "anotherUserName";
    private static final String ACCESS_ID = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, TEST_REALM, USER_NAME);
    private static final String ANOTHER_ACCESS_ID = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, TEST_REALM, ANOTHER_USER_NAME);
    private static final WSCredential wsCredential = new WSCredentialImpl(TEST_REALM, USER_NAME, USER_NAME, "UNAUTHENTICATED", "primaryUniqueGroupAccessId", ACCESS_ID, null, null);
    private static SharedOutputManager outputMgr;
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<UserRegistryService> userRegistryServiceRef = mock.mock(ServiceReference.class, "userRegistryServiceRef");
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<CredentialsService> credentialsServiceRef = mock.mock(ServiceReference.class, "credentialsServiceRef");
    private final CredentialsService credentialsService = mock.mock(CredentialsService.class);
    @SuppressWarnings("unchecked")
    private final CollectiveAuthenticationPlugin collectiveAuthenticationPlugin = mock.mock(CollectiveAuthenticationPlugin.class);
    private final JAASServiceImpl jaasService = new JAASServiceImpl();
    private final X509Certificate cert = mock.mock(X509Certificate.class);
    private final SingleSignonToken ssoToken = mock.mock(SingleSignonToken.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mock.mock(ServiceReference.class, "Test" + JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY
                                                                                                                             + "Ref");

    public CertificateLoginModuleTest() {
        principalOverride = new WSPrincipal(ANOTHER_USER_NAME, ANOTHER_ACCESS_ID, WSPrincipal.AUTH_METHOD_CERTIFICATE);
        wsCredentialOverride = new WSCredentialImpl(TEST_REALM, ANOTHER_USER_NAME, ANOTHER_USER_NAME, "UNAUTHENTICATED", "primaryUniqueGroupAccessId", ANOTHER_ACCESS_ID, null, null);
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
        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasService.setUserRegistryService(userRegistryServiceRef);
        jaasService.setCredentialsService(credentialsServiceRef);
        jaasService.setCollectiveAuthenticationPlugin(collectiveAuthenticationPlugin);
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
        jaasService.unsetCollectiveAuthenticationPlugin(collectiveAuthenticationPlugin);
        jaasService.deactivate(cc);
        outputMgr.resetStreams();
        mock.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    private class CallbackHandlerDouble implements CallbackHandler {
        private final X509Certificate[] certChain;

        CallbackHandlerDouble(X509Certificate[] certChain) {
            this.certChain = certChain;
        }

        /** {@inheritDoc} */
        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            ((WSX509CertificateChainCallback) callbacks[0]).setX509CertificateChain(certChain);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.modules.CertificateLoginModule#login()}.
     */
    @Test
    public void loginNullCertChain() throws Exception {
        CallbackHandler callbackHandler = new CallbackHandlerDouble(null);
        CertificateLoginModule module = new CertificateLoginModule();
        module.initialize(null, callbackHandler, null, null);
        assertFalse(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.modules.CertificateLoginModule#login()}.
     */
    @Test
    public void loginEmptyCertChain() throws Exception {
        CallbackHandler callbackHandler = new CallbackHandlerDouble(new X509Certificate[] {});
        CertificateLoginModule module = new CertificateLoginModule();
        module.initialize(null, callbackHandler, null, null);
        assertFalse(module.login());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.modules.CertificateLoginModule#login()}.
     */
    @Test
    public void loginFailsCertificateMapFailedException() throws Exception {
        final X509Certificate[] certChain = new X509Certificate[] { cert };
        mock.checking(new Expectations() {
            {
                one(collectiveAuthenticationPlugin).isCollectiveCertificateChain(certChain);
                will(returnValue(false));

                one(collectiveAuthenticationPlugin).isCollectiveCACertificate(certChain);
                will(returnValue(false));

                one(userRegistry).mapCertificate(cert);
                will(throwException(new CertificateMapFailedException("Expected")));

                one(cert).getSubjectX500Principal();
                will(returnValue(new X500Principal("CN=userName")));
            }
        });

        CallbackHandler callbackHandler = new CallbackHandlerDouble(certChain);
        CertificateLoginModule module = new CertificateLoginModule();
        module.initialize(null, callbackHandler, null, null);
        try {
            module.login();
        } catch (AuthenticationException e) {
            assertEquals("Recieved wrong exception message",
                         "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn CN=userName. The dn does not map to a user in the registry.",
                         e.getMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.modules.CertificateLoginModule#login()}.
     */
    @Test
    public void loginFailsUnexpectedException() throws Exception {
        final X509Certificate[] certChain = new X509Certificate[] { cert };
        mock.checking(new Expectations() {
            {
                one(collectiveAuthenticationPlugin).isCollectiveCertificateChain(certChain);
                will(returnValue(false));

                one(collectiveAuthenticationPlugin).isCollectiveCACertificate(certChain);
                will(returnValue(false));

                one(userRegistry).mapCertificate(cert);
                will(throwException(new RegistryException("Expected")));

                one(cert).getSubjectX500Principal();
                will(returnValue(new X500Principal("CN=userName")));
            }
        });

        CallbackHandler callbackHandler = new CallbackHandlerDouble(certChain);
        CertificateLoginModule module = new CertificateLoginModule();
        module.initialize(null, callbackHandler, null, null);
        try {
            module.login();
        } catch (AuthenticationException e) {
            assertEquals("Recieved wrong exception message",
                         "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn CN=userName. The dn does not map to a user in the registry.",
                         e.getMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.modules.CertificateLoginModule#login()}.
     */
    @Test
    public void loginPasses() throws Exception {
        Map<String, Object> sharedState = createSharedState();
        CertificateLoginModule module = createInitializedModule(new Subject(), sharedState);
        assertTrue(module.login());
    }

    @Override
    protected CertificateLoginModule createInitializedModule(Subject subject, Map<String, Object> sharedState) throws Exception {
        final X509Certificate[] certChain = new X509Certificate[] { cert };
        CallbackHandler callbackHandler = new CallbackHandlerDouble(certChain);
        final CertificateLoginModule module = new CertificateLoginModule();
        module.initialize(subject, callbackHandler, sharedState, null);
        mock.checking(new Expectations() {
            {
                one(collectiveAuthenticationPlugin).isCollectiveCertificateChain(certChain);
                will(returnValue(false));

                one(collectiveAuthenticationPlugin).isCollectiveCACertificate(certChain);
                will(returnValue(false));

                allowing(cert).getSubjectX500Principal();
                will(returnValue(new X500Principal("CN=userName")));

                one(userRegistry).mapCertificate(cert);
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

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.modules.CertificateLoginModule#login()}.
     */
    @Test
    public void loginServerCredential_passes() throws Exception {
        final X509Certificate[] certChain = new X509Certificate[] { cert };
        mock.checking(new Expectations() {
            {
                one(collectiveAuthenticationPlugin).isCollectiveCertificateChain(certChain);
                will(returnValue(true));

                one(collectiveAuthenticationPlugin).authenticateCertificateChain(certChain, true);

                one(cert).getSubjectX500Principal();
                will(returnValue(new X500Principal("cn=server,dc=com.ibm.ws.collective")));

                allowing(credentialsService).setCredentials(with(any(Subject.class)));
            }
        });

        CallbackHandler callbackHandler = new CallbackHandlerDouble(certChain);
        CertificateLoginModule module = new CertificateLoginModule();

        Map<String, Object> sharedState = createSharedState();
        module.initialize(null, callbackHandler, sharedState, null);

        assertTrue(module.login());

        module.temporarySubject.toString();
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.jaas.modules.modules.CertificateLoginModule#login()}.
     */
    @Test(expected = AuthenticationException.class)
    public void loginServerCredential_issuerNotAtlas() throws Exception {
        final X509Certificate[] certChain = new X509Certificate[] { cert };
        mock.checking(new Expectations() {
            {
                one(collectiveAuthenticationPlugin).isCollectiveCertificateChain(certChain);
                will(returnValue(true));

                one(collectiveAuthenticationPlugin).authenticateCertificateChain(certChain, true);
                will(throwException(new AuthenticationException("Expected Test Exception")));
            }
        });

        CallbackHandler callbackHandler = new CallbackHandlerDouble(certChain);
        CertificateLoginModule module = new CertificateLoginModule();

        Map<String, Object> sharedState = createSharedState();
        module.initialize(null, callbackHandler, sharedState, null);

        module.login();
    }
}
