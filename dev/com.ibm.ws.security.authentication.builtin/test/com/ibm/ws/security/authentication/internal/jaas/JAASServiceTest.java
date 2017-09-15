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
package com.ibm.ws.security.authentication.internal.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.CertificateAuthenticator;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.internal.JAASService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.jaas.common.JAASChangeNotifier;
import com.ibm.ws.security.jaas.common.JAASConfiguration;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.ws.security.jaas.common.internal.JAASSecurityConfiguration;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;
import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class JAASServiceTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule output = outputMgr;

    private static final String GOOD_USER = "testuser";
    private static final String GOOD_USER_PWD = "testuserpwd";
    private static final String BAD_USER = "baduser";
    private static final String BAD_USER_PWD = "baduserpwd";
    private static final byte[] GOOD_TOKEN = GOOD_USER.getBytes();
    private static final byte[] BAD_TOKEN = BAD_USER.getBytes();
    private static X509Certificate[] GOOD_CERT_CHAIN;
    private static X509Certificate[] BAD_CERT_CHAIN;
    private static AuthenticationData authenticationData;
    private static AuthenticationData tokenAuthenticationData;
    private static AuthenticationData certAuthenticationData;
    private static Subject partialSubject;

    protected final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    protected final Library sharedLibrary = mock.mock(Library.class);

    final ClassLoader loader1 = Thread.currentThread().getContextClassLoader();
    final ClassLoader loader2 = new ClassLoader() {};

    protected final JAASLoginModuleConfig jaasLoginModuleConfig = mock.mock(JAASLoginModuleConfig.class);
    protected final ServiceReference<JAASLoginModuleConfig> jaasLoginModuleConfigRef = mock.mock(ServiceReference.class, JAASServiceImpl.KEY_JAAS_LOGIN_MODULE_CONFIG + "Ref");
    protected final ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig> jaasLoginModuleConfigs = new ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig>(JAASServiceImpl.KEY_JAAS_LOGIN_MODULE_CONFIG
                                                                                                                                                                           + "s");

    protected final JAASLoginContextEntry jaasLoginContextEntry = mock.mock(JAASLoginContextEntry.class);
    protected final ServiceReference<JAASLoginContextEntry> jaasLoginContextEntryRef = mock.mock(ServiceReference.class, JAASServiceImpl.KEY_JAAS_LOGIN_CONTEXT_ENTRY + "Ref");
    protected final ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries = new ConcurrentServiceReferenceMap<String, JAASLoginContextEntry>(JAASServiceImpl.KEY_JAAS_LOGIN_CONTEXT_ENTRY
                                                                                                                                                                            + "s");
    private final ServiceReference<JAASConfiguration> jaasConfigurationRef = mock.mock(ServiceReference.class, "jaasConfigurationRef");
    private final JAASConfiguration jaasConfiguration = mock.mock(JAASConfiguration.class, "TestJaasConfiguration");

    private final ServiceReference<JAASChangeNotifier> jaasChangeNotifierRef = mock.mock(ServiceReference.class, "jaasChangeNotifierRef");
    private final JAASChangeNotifier jaasChangeNotifier = mock.mock(JAASChangeNotifier.class);

    protected final JAASConfigurationFactory jaasConfigurationFactory = new JAASConfigurationFactory();
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mock.mock(ServiceReference.class, "Test" + JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY
                                                                                                                             + "Ref");
    protected final ConcurrentServiceReferenceMap<String, CertificateAuthenticator> certificateAuthenticators = new ConcurrentServiceReferenceMap<String, CertificateAuthenticator>(JAASServiceImpl.KEY_CERT_AUTHENTICATOR
                                                                                                                                                                                    + "s");

    protected ComponentContext componentContext = null;
    private final static Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries = new HashMap<String, List<AppConfigurationEntry>>();

    private final AuthenticationService authenticationService = mock.mock(AuthenticationService.class);

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        authenticationData = createAuthenticationData(GOOD_USER, GOOD_USER_PWD);
        tokenAuthenticationData = createTokenAuthenticationData(GOOD_TOKEN);
        partialSubject = new Subject();
        Configuration.setConfiguration(null);
        InputStream inStream = new FileInputStream("publish" + File.separator + "certificates" + File.separator + "gooduser.cer");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate goodcert = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();
        GOOD_CERT_CHAIN = new X509Certificate[] { goodcert, null };
        inStream = new FileInputStream("publish" + File.separator + "certificates" + File.separator + "baduser.cer");
        X509Certificate badcert = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();
        BAD_CERT_CHAIN = new X509Certificate[] { badcert, null };
        certAuthenticationData = createCertAuthenticationData(GOOD_CERT_CHAIN);
    }

    @Before
    public void setUp() {
        AppConfigurationEntry appConfigurationEntry = null;
        List<AppConfigurationEntry> appConfigurationEntries = new ArrayList<AppConfigurationEntry>();
        appConfigurationEntries.add(appConfigurationEntry);
        jaasConfigurationEntries.put("pid", appConfigurationEntries);

        componentContext = createComponentContextMock();
        mock.checking(new Expectations() {
            {
                allowing(jaasLoginModuleConfigRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jaasLoginModuleConfigRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        Configuration.setConfiguration(null);
        mock.assertIsSatisfied();
        jaasConfigurationEntries.clear();
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            JAASService jaasService = new JAASServiceImpl();
            assertNotNull("There must be a JAAS Service", jaasService);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testActivateCreatesConfiguration() {
        final String methodName = "testActivateCreatesConfiguration";
        try {
            JAASServiceImpl jaasService = createActivatedJAASService();
            Configuration jaasConfiguration = Configuration.getConfiguration();
            jaasService.deactivate(componentContext);
            assertNotNull("There must be a JAAS Configuration", jaasConfiguration);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testActivateCreatesJAASConfiguration() {
        final String methodName = "testActivateCreatesJAASConfiguration";
        try {
            JAASServiceImpl jaasService = createActivatedJAASService();
            Configuration jaasConfiguration = Configuration.getConfiguration();
            jaasService.deactivate(componentContext);
            assertTrue("The configuration must be a JAASConfiguration", jaasConfiguration instanceof JAASSecurityConfiguration);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateCallbackHandlerForAuthenticationData() {
        final String methodName = "testCreateCallbackHandlerForAuthenticationData";
        try {
            JAASServiceImpl jaasService = createActivatedJAASService();
            CallbackHandler callbackHandler = jaasService.createCallbackHandlerForAuthenticationData(authenticationData);
            jaasService.deactivate(componentContext);
            assertNotNull("There must be a callback handler", callbackHandler);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateCallbackHandlerForTokenAuthenticationData() {
        final String methodName = "testCreateCallbackHandlerForTokenAuthenticationData";
        try {
            JAASServiceImpl jaasService = createActivatedJAASService();
            CallbackHandler callbackHandler = jaasService.createCallbackHandlerForAuthenticationData(tokenAuthenticationData);
            jaasService.deactivate(componentContext);
            assertNotNull("There must be a callback handler", callbackHandler);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateCallbackHandlerForCertAuthenticationData() {
        final String methodName = "testCreateCallbackHandlerForCertAuthenticationData";
        try {
            JAASServiceImpl jaasService = createActivatedJAASService();
            CallbackHandler callbackHandler = jaasService.createCallbackHandlerForAuthenticationData(certAuthenticationData);
            jaasService.deactivate(componentContext);
            assertNotNull("There must be a callback handler", callbackHandler);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateLoginContext() {
        final String methodName = "testCreateLoginContext";
        try {

            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            CallbackHandler callbackHandler = jaasServiceDouble.createCallbackHandlerForAuthenticationData(authenticationData);
            LoginContext loginContext = jaasServiceDouble.createLoginContext(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertNotNull("There must be a login context", loginContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateLoginContextForToken() {
        final String methodName = "testCreateLoginContextForToken";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            CallbackHandler callbackHandler = jaasServiceDouble.createCallbackHandlerForAuthenticationData(tokenAuthenticationData);
            LoginContext loginContext = jaasServiceDouble.createLoginContext(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertNotNull("There must be a login context", loginContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateLoginContextForCert() {
        final String methodName = "testCreateLoginContextForCert";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            CallbackHandler callbackHandler = jaasServiceDouble.createCallbackHandlerForAuthenticationData(certAuthenticationData);
            LoginContext loginContext = jaasServiceDouble.createLoginContext(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertNotNull("There must be a login context", loginContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLogin() {
        final String methodName = "testPerformLogin";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForToken() {
        final String methodName = "testPerformLoginForToken";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForCert() {
        final String methodName = "testPerformLoginForCert";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, certAuthenticationData, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginReturnsSubject() {
        final String methodName = "testPerformLoginReturnsSubject";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertNotNull("There must be an authenticated subject", authenticatedSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForTokenReturnsSubject() {
        final String methodName = "testPerformLoginForTokenReturnsSubject";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertNotNull("There must be an authenticated subject", authenticatedSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForCertReturnsSubject() {
        final String methodName = "testPerformLoginForCertReturnsSubject";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, certAuthenticationData, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertNotNull("There must be an authenticated subject", authenticatedSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginWithPartialSubjectReturnsSameSubject() {
        final String methodName = "testPerformLoginWithPartialSubjectReturnsSameSubject";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertTrue("The authenticated subject must be the same as the partial subject", authenticatedSubject == partialSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForTokenWithPartialSubjectReturnsSameSubject() {
        final String methodName = "testPerformLoginForTokenWithPartialSubjectReturnsSameSubject";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertTrue("The authenticated subject must be the same as the partial subject", authenticatedSubject == partialSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForCertWithPartialSubjectReturnsSameSubject() {
        final String methodName = "testPerformLoginForCertWithPartialSubjectReturnsSameSubject";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, certAuthenticationData, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertTrue("The authenticated subject must be the same as the partial subject", authenticatedSubject == partialSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginWithInvalidDataThrowsAuthenticationException() throws Exception {
        final String methodName = "testPerformLoginWithInvalidDataThrowsAuthenticationException";
        JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
        try {
            AuthenticationData invalidData = createAuthenticationData(BAD_USER, BAD_USER_PWD);
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, invalidData, partialSubject);
            assertNotNull("There must be an authenticated subject", authenticatedSubject);
        } catch (javax.security.auth.login.LoginException le) {
            //success
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        } finally {
            jaasServiceDouble.deactivate(componentContext);
        }
    }

    @Test
    public void testPerformLoginForTokenWithInvalidDataThrowsAuthenticationException() throws Exception {
        final String methodName = "testPerformLoginForTokenWithInvalidDataThrowsAuthenticationException";
        JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
        try {
            AuthenticationData invalidData = createTokenAuthenticationData(BAD_TOKEN);
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, invalidData, partialSubject);
            assertNotNull("There must be an authenticated subject", authenticatedSubject);
        } catch (javax.security.auth.login.LoginException le) {
            //success
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        } finally {
            jaasServiceDouble.deactivate(componentContext);
        }
    }

    @Test
    public void testPerformLoginForCertWithInvalidDataThrowsAuthenticationException() throws Exception {
        final String methodName = "testPerformLoginForCertWithInvalidDataThrowsAuthenticationException";
        JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
        try {
            AuthenticationData invalidData = createCertAuthenticationData(BAD_CERT_CHAIN);
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, invalidData, partialSubject);
            assertNotNull("There must be an authenticated subject", authenticatedSubject);
        } catch (javax.security.auth.login.LoginException le) {
            //success
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        } finally {
            jaasServiceDouble.deactivate(componentContext);
        }
    }

    @Test
    public void testPerformLoginInvokesCreateCallbackHandlerForAuthenticationData() {
        final String methodName = "testPerformLoginInvokesCreateCallbackHandlerForAuthenticationData";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, partialSubject);
            boolean wasInvoked = jaasServiceDouble.createCallbackHandlerForAuthenticationDataWasInvoked;
            jaasServiceDouble.deactivate(componentContext);
            assertTrue("The createCallbackHandlerForAuthenticationData method must be invoked", wasInvoked);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForTokenInvokesCreateCallbackHandlerForAuthenticationData() {
        final String methodName = "testPerformLoginForTokenInvokesCreateCallbackHandlerForAuthenticationData";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
            boolean wasInvoked = jaasServiceDouble.createCallbackHandlerForAuthenticationDataWasInvoked;
            jaasServiceDouble.deactivate(componentContext);
            assertTrue("The createCallbackHandlerForAuthenticationData method must be invoked", wasInvoked);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForCertInvokesCreateCallbackHandlerForAuthenticationData() {
        final String methodName = "testPerformLoginForCertInvokesCreateCallbackHandlerForAuthenticationData";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, certAuthenticationData, partialSubject);
            boolean wasInvoked = jaasServiceDouble.createCallbackHandlerForAuthenticationDataWasInvoked;
            jaasServiceDouble.deactivate(componentContext);
            assertTrue("The createCallbackHandlerForAuthenticationData method must be invoked", wasInvoked);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginInvokesCreateLoginContext() {
        final String methodName = "testPerformLoginInvokesCreateLoginContext";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                    will(returnValue(null));
                }
            });
            JAASServiceTestDoubleForTestingCreateLoginInvocation jaasServiceDouble = new JAASServiceTestDoubleForTestingCreateLoginInvocation();
            jaasServiceDouble.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
            JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
            jaasServiceDouble.activate(componentContext, Collections.<String, Object> emptyMap());
            jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, partialSubject);
            boolean wasInvoked = jaasServiceDouble.createLoginContextWasInvoked;
            jaasServiceDouble.deactivate(componentContext);
            assertTrue("The createLoginContext method must be invoked", wasInvoked);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForTokenInvokesCreateLoginContext() {
        final String methodName = "testPerformLoginForTokenInvokesCreateLoginContext";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                    will(returnValue(null));
                }
            });
            JAASServiceTestDoubleForTestingCreateLoginInvocation jaasServiceDouble = new JAASServiceTestDoubleForTestingCreateLoginInvocation();
            jaasServiceDouble.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
            JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
            jaasServiceDouble.activate(componentContext, Collections.<String, Object> emptyMap());
            jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
            boolean wasInvoked = jaasServiceDouble.createLoginContextWasInvoked;
            jaasServiceDouble.deactivate(componentContext);
            assertTrue("The createLoginContext method must be invoked", wasInvoked);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerformLoginForCertInvokesCreateLoginContext() {
        final String methodName = "testPerformLoginForCertInvokesCreateLoginContext";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                    will(returnValue(null));
                }
            });
            JAASServiceTestDoubleForTestingCreateLoginInvocation jaasServiceDouble = new JAASServiceTestDoubleForTestingCreateLoginInvocation();
            jaasServiceDouble.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
            JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
            jaasServiceDouble.activate(componentContext, Collections.<String, Object> emptyMap());
            jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
            boolean wasInvoked = jaasServiceDouble.createLoginContextWasInvoked;
            jaasServiceDouble.deactivate(componentContext);
            assertTrue("The createLoginContext method must be invoked", wasInvoked);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerfomLoginWithCallbackHandlerReturnsSubject() {
        final String methodName = "testPerfomLoginWithCallbackHandlerReturnsSubject";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            CallbackHandler callbackHandler = createTestCallbackHandler(GOOD_USER, GOOD_USER_PWD.toCharArray());
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertNotNull("There must be an authenticated subject", authenticatedSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerfomLoginForTokenWithCallbackHandlerReturnsSubject() {
        final String methodName = "testPerfomLoginForTokenWithCallbackHandlerReturnsSubject";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            CallbackHandler callbackHandler = createTestTokenCallbackHandler(GOOD_TOKEN);
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertNotNull("There must be an authenticated subject", authenticatedSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testPerfomLoginForCertWithCallbackHandlerReturnsSubject() {
        final String methodName = "testPerfomLoginForCertWithCallbackHandlerReturnsSubject";
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            CallbackHandler callbackHandler = createTestCertCallbackHandler(GOOD_CERT_CHAIN);
            Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
            jaasServiceDouble.deactivate(componentContext);
            assertNotNull("There must be an authenticated subject", authenticatedSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDeactivateRemovesConfiguration() {
        try {
            JAASServiceTestDoubleWithConfiguration jaasServiceDouble = createActivatedJAASServiceTestDouble();
            jaasServiceDouble.deactivate(componentContext);
            Configuration jaasConfiguration = Configuration.getConfiguration();
        } catch (Throwable t) {
            try {
                assertTrue(t.toString(), t instanceof java.lang.SecurityException);
                assertEquals("The message must be 'Unable to locate a login configuration'", "Unable to locate a login configuration", t.getMessage());
            } catch (AssertionError err) {
                if (err.getCause() == null) {
                    err.initCause(t);
                }
                throw err;
            }
        }
    }

    @Test
    public void testGetTokenManager() throws Exception {
        final ServiceReference<TokenManager> tokenManagerServiceReference = mock.mock(ServiceReference.class, "tokenManagerServiceReference");
        JAASServiceImpl jaasService = new JAASServiceImpl();
        jaasService.setTokenManager(tokenManagerServiceReference);

        final TokenManager tokenManager = mock.mock(TokenManager.class);
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));

                allowing(componentContext).locateService(JAASServiceImpl.KEY_TOKEN_MANAGER, tokenManagerServiceReference);
                will(returnValue(tokenManager));
            }
        });

        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasService.activate(componentContext, new HashMap<String, Object>());
        TokenManager actualTokenManager = JAASServiceImpl.getTokenManager();
        jaasService.deactivate(componentContext);
        assertNotNull("There must be a token manager.", actualTokenManager);
    }

    @Test
    public void testUnsetTokenManager() throws Exception {
        final ServiceReference<TokenManager> tokenManagerServiceReference = mock.mock(ServiceReference.class, "tokenManagerServiceReference");
        JAASServiceImpl jaasService = new JAASServiceImpl();
        jaasService.setTokenManager(tokenManagerServiceReference);

        final TokenManager tokenManager = mock.mock(TokenManager.class);
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASServiceImpl.KEY_TOKEN_MANAGER, tokenManagerServiceReference);
                will(returnValue(tokenManager));
                allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));
            }
        });
        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
        jaasService.activate(componentContext, new HashMap<String, Object>());
        jaasService.unsetTokenManager(tokenManagerServiceReference);
        TokenManager actualTokenManager = JAASServiceImpl.getTokenManager();
        jaasService.deactivate(componentContext);
        assertNull("There must not be a token manager.", actualTokenManager);
    }

    @Test
    public void testGetCredentialsService() throws Exception {
        final ServiceReference<CredentialsService> credentialsServiceReference = mock.mock(ServiceReference.class, "credentialsServiceReference");
        JAASServiceImpl jaasService = new JAASServiceImpl();
        jaasService.setCredentialsService(credentialsServiceReference);

        final CredentialsService credentialsService = mock.mock(CredentialsService.class);
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));
                allowing(componentContext).locateService(JAASServiceImpl.KEY_CREDENTIALS_SERVICE, credentialsServiceReference);
                will(returnValue(credentialsService));
            }
        });
        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
        jaasService.activate(componentContext, new HashMap<String, Object>());
        CredentialsService actualCredentialsService = JAASServiceImpl.getCredentialsService();
        jaasService.deactivate(componentContext);
        assertNotNull("There must be a credentials service.", actualCredentialsService);
    }

    @Test
    public void testUnsetCredentialsService() throws Exception {
        final ServiceReference<CredentialsService> credentialsServiceReference = mock.mock(ServiceReference.class, "credentialsServiceReference");
        JAASServiceImpl jaasService = new JAASServiceImpl();
        jaasService.setCredentialsService(credentialsServiceReference);

        final CredentialsService credentialsService = mock.mock(CredentialsService.class);
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASServiceImpl.KEY_CREDENTIALS_SERVICE, credentialsServiceReference);
                will(returnValue(credentialsService));
                allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));
            }
        });
        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
        jaasService.activate(componentContext, new HashMap<String, Object>());
        jaasService.unsetCredentialsService(credentialsServiceReference);
        CredentialsService actualCredentialsService = JAASServiceImpl.getCredentialsService();
        jaasService.deactivate(componentContext);
        assertNull("There must not be a credentials service.", actualCredentialsService);
    }

    @Test
    public void configReady() throws Exception {
        mock.checking(new Expectations() {
            {
                one(jaasChangeNotifier).notifyListeners();
            }
        });
        JAASServiceImpl jaasService = createActivatedJAASServiceWithNotifier();
        jaasService.configReady();
        jaasService.deactivate(componentContext);
    }

    @Test
    public void modified_triggersConfigReady() throws Exception {
        mock.checking(new Expectations() {
            {
                one(jaasChangeNotifier).notifyListeners();
            }
        });
        JAASServiceImpl jaasService = createActivatedJAASServiceWithNotifier();
        jaasService.modified(null);
        jaasService.deactivate(componentContext);
    }

    @Test
    public void deactivate_noNotifications() throws Exception {
        mock.checking(new Expectations() {
            {
                never(jaasChangeNotifier).notifyListeners();
            }
        });
        JAASServiceImpl jaasService = createActivatedJAASServiceWithNotifier();
        jaasService.deactivate(componentContext);
        jaasService.configReady();
    }

    @Test
    public void unsetJaasChangeNotifier_noNotifications() throws Exception {
        mock.checking(new Expectations() {
            {
                never(jaasChangeNotifier).notifyListeners();
            }
        });
        JAASServiceImpl jaasService = createActivatedJAASServiceWithNotifier();
        jaasService.unsetJaasChangeNotifier(jaasChangeNotifierRef);
        jaasService.configReady();
        jaasService.deactivate(componentContext);
    }

    /**
     * During the initialization of the JAASServiceImpl there will not be an
     * authentication service set, therefore there should not be any notifications in order
     * to avoid resolving the service references before they are really needed.
     */
    @Test
    public void noAuthenticationService_noNotifications() throws Exception {
        mock.checking(new Expectations() {
            {
                never(jaasChangeNotifier).notifyListeners();
            }
        });
        JAASServiceImpl jaasService = createActivatedJAASServiceWithNotifier();
        JAASServiceImpl.unsetAuthenticationService(authenticationService);
        jaasService.configReady();
        jaasService.deactivate(componentContext);
    }

    private JAASServiceImpl createActivatedJAASService() throws IOException {
        JAASServiceImpl jaasService = new JAASServiceImpl();
        jaasService.jaasLoginContextEntries = jaasLoginContextEntries;
        JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
        final String custom = "custom";
        final String pid = "pid";
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(jaasConfigurationFactory));

                allowing(jaasLoginModuleConfigRef).getProperty("id");
                will(returnValue(custom));
                allowing(jaasLoginModuleConfigRef).getProperty(Constants.SERVICE_PID);
                will(returnValue(pid));
                allowing(jaasConfiguration).setJaasLoginContextEntries(jaasLoginContextEntries);
//                allowing(jaasConfiguration).setJaasLoginModuleConfigs(jaasLoginModuleConfigs);
                allowing(jaasConfiguration).setJaasLoginContextEntries(jaasLoginContextEntries);
                allowing(jaasConfiguration).getEntries();
                will(returnValue(jaasConfigurationEntries));
            }
        });
        jaasConfigurationFactory.setJAASConfiguration(jaasConfigurationRef);
        jaasConfigurationFactory.activate(componentContext);
        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasService.activate(componentContext, null);
        return jaasService;
    }

    private JAASServiceImpl createActivatedJAASServiceWithNotifier() throws Exception {
        JAASServiceImpl jaasService = new JAASServiceImpl();
        jaasService.jaasLoginContextEntries = jaasLoginContextEntries;
        JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
        // Clear any existing authentication service from the JAASServiceImpl class.
        JAASServiceImpl.unsetAuthenticationService(JAASServiceImpl.getAuthenticationService());
        final String custom = "custom";
        final String pid = "pid";
        final String[] loginModuleIds = new String[] { pid };
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(jaasConfigurationFactory));
                allowing(jaasLoginModuleConfigRef).getProperty("id");
                will(returnValue(custom));
                allowing(jaasLoginModuleConfigRef).getProperty("service.pid");
                will(returnValue(pid));
                allowing(jaasLoginContextEntryRef).getProperty("id");
                will(returnValue(custom));
                allowing(jaasLoginContextEntryRef).getProperty("loginModuleRef");
                will(returnValue(loginModuleIds));
                allowing(jaasLoginContextEntryRef).getProperty("service.id");
                will(returnValue(1L));
                allowing(jaasLoginContextEntryRef).getProperty("service.ranking");
                will(returnValue(1L));
                allowing(jaasConfiguration).setJaasLoginContextEntries(jaasLoginContextEntries);
//                allowing(jaasConfiguration).setJaasLoginModuleConfigs(jaasLoginModuleConfigs);
                allowing(jaasConfiguration).getEntries();
                will(returnValue(jaasConfigurationEntries));
            }
        });
        jaasConfigurationFactory.setJAASConfiguration(jaasConfigurationRef);
        jaasConfigurationFactory.activate(componentContext);
        jaasService.setJaasLoginContextEntry(jaasLoginContextEntryRef);
        jaasService.setJaasChangeNotifier(jaasChangeNotifierRef);
        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasService.activate(componentContext, null);
        JAASServiceImpl.setAuthenticationService(authenticationService);
        return jaasService;
    }

    private JAASServiceTestDoubleWithConfiguration createActivatedJAASServiceTestDouble() throws IOException {
        JAASServiceTestDoubleWithConfiguration jaasServiceDouble = new JAASServiceTestDoubleWithConfiguration();
        jaasServiceDouble.jaasLoginContextEntries = jaasLoginContextEntries;
        JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
        final Map<String, Object> someProps = new Hashtable<String, Object>();
        String[] values = { "value1", "value2" };
        someProps.put("otherProps", values);
        final String lm = "loginModule";
        final String pid = "pid";
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(jaasConfigurationFactory));

                allowing(jaasLoginModuleConfigRef).getProperty("id");
                will(returnValue(lm));
                allowing(jaasLoginModuleConfigRef).getProperty("service.pid");
                will(returnValue(pid));
                allowing(jaasConfiguration).setJaasLoginContextEntries(jaasLoginContextEntries);
//                allowing(jaasConfiguration).setJaasLoginModuleConfigs(jaasLoginModuleConfigs);
                allowing(jaasConfiguration).getEntries();
                will(returnValue(jaasConfigurationEntries));
            }
        });
        jaasConfigurationFactory.setJAASConfiguration(jaasConfigurationRef);
        jaasConfigurationFactory.activate(componentContext);
        jaasServiceDouble.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasServiceDouble.activate(componentContext, someProps);
        return jaasServiceDouble;
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

    private ComponentContext createComponentContextMock() {
        final ComponentContext componentContextMock = mock.mock(ComponentContext.class, "testComponentContext");
        final BundleContext bundleContextMock = mock.mock(BundleContext.class);
        final Bundle bundle = mock.mock(Bundle.class);
        final BundleWiring wiring = mock.mock(BundleWiring.class);

        final Map<String, Object> someProps = new Hashtable<String, Object>();
        String[] values = { "value1", "value2" };
        someProps.put("otherProps", values);

        mock.checking(new Expectations() {
            {
                allowing(componentContextMock).getBundleContext();
                will(returnValue(bundleContextMock));

                allowing(bundleContextMock).getBundle();
                will(returnValue(bundle));

                allowing(bundle).adapt(BundleWiring.class);
                will(returnValue(wiring));

                allowing(wiring).getClassLoader();
                will(returnValue(loader2));

                allowing(componentContextMock).locateService(JAASServiceImpl.KEY_JAAS_LOGIN_MODULE_CONFIG, jaasLoginModuleConfigRef);
                will(returnValue(jaasLoginModuleConfig));

                allowing(componentContextMock).locateService(JAASServiceImpl.KEY_CHANGE_SERVICE, jaasChangeNotifierRef);
                will(returnValue(jaasChangeNotifier));

                allowing(componentContextMock).locateService(JAASConfigurationFactory.KEY_JAAS_CONFIGURATION, jaasConfigurationRef);
                will(returnValue(jaasConfiguration));

                allowing(componentContextMock).getProperties();
                will(returnValue(someProps));
            }
        });
        return componentContextMock;
    }

    private class JAASServiceTestDoubleWithConfiguration extends JAASServiceImpl {
        protected boolean createCallbackHandlerForAuthenticationDataWasInvoked = false;

        @Override
        public CallbackHandler createCallbackHandlerForAuthenticationData(final AuthenticationData authenticationData) {
            createCallbackHandlerForAuthenticationDataWasInvoked = true;
            return super.createCallbackHandlerForAuthenticationData(authenticationData);
        }

        @Override
        public void activate(ComponentContext componentContext, Map<String, Object> ignored) {
            super.activate(componentContext, ignored);

            Configuration testJaasConfiguration = new Configuration() {
                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    AppConfigurationEntry[] appConfigurationEntry = null;
                    appConfigurationEntry = new AppConfigurationEntry[3];
                    Map<String, ?> options = new HashMap<String, Object>();
                    appConfigurationEntry[0] = new AppConfigurationEntry("com.ibm.ws.security.authentication.internal.test.modules.TestLoginModule", LoginModuleControlFlag.REQUIRED, options);
                    appConfigurationEntry[1] = new AppConfigurationEntry("com.ibm.ws.security.authentication.internal.test.modules.TestTokenLoginModule", LoginModuleControlFlag.REQUIRED, options);
                    appConfigurationEntry[2] = new AppConfigurationEntry("com.ibm.ws.security.authentication.internal.test.modules.TestCertificateLoginModule", LoginModuleControlFlag.REQUIRED, options);
                    return appConfigurationEntry;
                }
            };

            Configuration.setConfiguration(testJaasConfiguration);
        }
    }

    private class JAASServiceTestDoubleForTestingCreateLoginInvocation extends JAASServiceTestDoubleWithConfiguration {
        protected boolean createLoginContextWasInvoked = false;

        @Override
        public LoginContext createLoginContext(String jaasEntryName, CallbackHandler callbackHandler, Subject partialSubject) throws LoginException {
            createLoginContextWasInvoked = true;
            return super.createLoginContext(jaasEntryName, callbackHandler, partialSubject);
        }
    }

    private CallbackHandler createTestCallbackHandler(final String name, final char[] password) {
        return new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        ((NameCallback) callback).setName(name);
                    } else if (callback instanceof PasswordCallback) {
                        ((PasswordCallback) callback).setPassword(password);
                    }
                }
            }

        };
    }

    private CallbackHandler createTestTokenCallbackHandler(final byte[] token) {
        return new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof WSCredTokenCallbackImpl) {
                        ((WSCredTokenCallbackImpl) callback).setCredToken(token);
                    }
                }
            }

        };
    }

    private CallbackHandler createTestCertCallbackHandler(final X509Certificate[] certs) {
        return new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof WSX509CertificateChainCallback) {
                        ((WSX509CertificateChainCallback) callback).setX509CertificateChain(certs);
                    }
                }
            }

        };
    }
}
