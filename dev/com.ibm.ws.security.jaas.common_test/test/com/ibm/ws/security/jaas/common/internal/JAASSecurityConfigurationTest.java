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
package com.ibm.ws.security.jaas.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.junit.After;
import org.junit.AfterClass;
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

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.CertificateAuthenticator;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.internal.JAASService;
import com.ibm.ws.security.authentication.internal.jaas.JAASServiceImpl;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.jaas.common.JAASConfiguration;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.ws.security.jaas.common.callback.TokenCallback;
import com.ibm.ws.security.jaas.config.JAASLoginConfig;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;

/**
 *
 */
@SuppressWarnings("unchecked")
public class JAASSecurityConfigurationTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule checkOutput = outputMgr;

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

    protected final Mockery mock = new JUnit4Mockery();

    protected final Library sharedLib = mock.mock(Library.class);
    final ClassLoader loader1 = Thread.currentThread().getContextClassLoader();
    final ClassLoader loader2 = new ClassLoader() {};

    private final ServiceReference<ClassLoadingService> classLoadingRef = mock.mock(ServiceReference.class, JAASLoginModuleConfig.KEY_CLASSLOADING_SVC);
    private final ClassLoadingService classLoadSvc = mock.mock(ClassLoadingService.class);

    protected final ServiceReference<JAASLoginConfig> jaasLoginConfigRef = mock.mock(ServiceReference.class, "jaasLoginConfigRef");
    private final JAASLoginConfig jaasLoginConfig = mock.mock(JAASLoginConfig.class, "jaasLoginConfig");

    protected final JAASLoginContextEntry jaasLoginContextEntry = mock.mock(JAASLoginContextEntry.class);
    protected final ServiceReference<JAASLoginContextEntry> jaasLoginContextEntryRef = mock.mock(ServiceReference.class, JAASServiceImpl.KEY_JAAS_LOGIN_CONTEXT_ENTRY + "Ref");
    protected final ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries = new ConcurrentServiceReferenceMap<String, JAASLoginContextEntry>(JAASServiceImpl.KEY_JAAS_LOGIN_CONTEXT_ENTRY
                                                                                                                                                                            + "s");

    protected final JAASLoginModuleConfig jaasLoginModuleConfig = mock.mock(JAASLoginModuleConfig.class);
    protected final ServiceReference<JAASLoginModuleConfig> jaasLoginModuleConfigRef = mock.mock(ServiceReference.class, JAASServiceImpl.KEY_JAAS_LOGIN_MODULE_CONFIG + "Ref");
    protected final ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig> jaasLoginModuleConfigs = new ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig>(JAASServiceImpl.KEY_JAAS_LOGIN_MODULE_CONFIG
                                                                                                                                                                           + "s");
    protected final JAASConfigurationFactory jaasConfigurationFactory = new JAASConfigurationFactory();
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mock.mock(ServiceReference.class, "Test" + JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY
                                                                                                                             + "Ref");

    private final ServiceReference<JAASConfiguration> jaasConfigurationRef = mock.mock(ServiceReference.class, "jaasConfigurationRef");
    private final JAASConfiguration jaasConfiguration = mock.mock(JAASConfiguration.class, "TestJaasConfiguration");
    private final static Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries = new HashMap<String, List<AppConfigurationEntry>>();

    protected final ConcurrentServiceReferenceMap<String, CertificateAuthenticator> certificateAuthenticators =
                    new ConcurrentServiceReferenceMap<String, CertificateAuthenticator>(JAASServiceImpl.KEY_CERT_AUTHENTICATOR + "s");

    protected final org.osgi.service.cm.Configuration config = mock.mock(org.osgi.service.cm.Configuration.class);

    protected ComponentContext componentContext = null;

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
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
        Configuration.setConfiguration(null);
        mock.assertIsSatisfied();
        jaasConfigurationEntries.clear();
    }

    @Test
    public void testConstructor() {
        JAASService jaasService = new JAASServiceImpl();
        assertNotNull("There must be a JAAS Service", jaasService);
    }

    @Test
    public void testActivateCreatesConfiguration() throws Exception {
        createActivatedJAASService();
        Configuration jaasConfiguration = Configuration.getConfiguration();
        assertNotNull("There must be a JAAS Configuration", jaasConfiguration);
    }

    @Test
    public void testActivateCreatesJAASConfiguration() throws Exception {
        createActivatedJAASService();
        Configuration jaasConfiguration = Configuration.getConfiguration();
        assertTrue("The configuration must be a JAASConfiguration", jaasConfiguration instanceof JAASSecurityConfiguration);
    }

    @Test
    public void testCreateCallbackHandlerForAuthenticationData() throws Exception {
        JAASServiceImpl jaasService = createActivatedJAASService();
        CallbackHandler callbackHandler = jaasService.createCallbackHandlerForAuthenticationData(authenticationData);
        assertNotNull("There must be a callback handler", callbackHandler);
    }

    @Test
    public void testCreateCallbackHandlerForTokenAuthenticationData() throws Exception {
        JAASServiceImpl jaasService = createActivatedJAASService();
        CallbackHandler callbackHandler = jaasService.createCallbackHandlerForAuthenticationData(tokenAuthenticationData);
        assertNotNull("There must be a callback handler", callbackHandler);
    }

    @Test
    public void testCreateCallbackHandlerForCertAuthenticationData() throws Exception {
        JAASServiceImpl jaasService = createActivatedJAASService();
        CallbackHandler callbackHandler = jaasService.createCallbackHandlerForAuthenticationData(certAuthenticationData);
        assertNotNull("There must be a callback handler", callbackHandler);
    }

    @Test
    public void testCreateLoginContext() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        CallbackHandler callbackHandler = jaasServiceDouble.createCallbackHandlerForAuthenticationData(authenticationData);
        LoginContext loginContext = jaasServiceDouble.createLoginContext(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
        assertNotNull("There must be a login context", loginContext);
    }

    @Test
    public void testCreateLoginContextForToken() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        CallbackHandler callbackHandler = jaasServiceDouble.createCallbackHandlerForAuthenticationData(tokenAuthenticationData);
        LoginContext loginContext = jaasServiceDouble.createLoginContext(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
        assertNotNull("There must be a login context", loginContext);
    }

    @Test
    public void testCreateLoginContextForCert() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        CallbackHandler callbackHandler = jaasServiceDouble.createCallbackHandlerForAuthenticationData(certAuthenticationData);
        LoginContext loginContext = jaasServiceDouble.createLoginContext(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
        assertNotNull("There must be a login context", loginContext);
    }

    @Test
    public void testPerformLogin() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, partialSubject);
    }

    @Test
    public void testPerformLoginForToken() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
    }

    @Test
    public void testPerformLoginForCert() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, certAuthenticationData, partialSubject);
    }

    @Test
    public void testPerformLoginReturnsSubject() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, partialSubject);
        assertNotNull("There must be an authenticated subject", authenticatedSubject);
    }

    @Test
    public void testPerformLoginForTokenReturnsSubject() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
        assertNotNull("There must be an authenticated subject", authenticatedSubject);
    }

    @Test
    public void testPerformLoginForCertReturnsSubject() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, certAuthenticationData, partialSubject);
        assertNotNull("There must be an authenticated subject", authenticatedSubject);
    }

    @Test
    public void testPerformLoginWithPartialSubjectReturnsSameSubject() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, partialSubject);
        assertTrue("The authenticated subject must be the same as the partial subject", authenticatedSubject == partialSubject);
    }

    @Test
    public void testPerformLoginForTokenWithPartialSubjectReturnsSameSubject() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
        assertTrue("The authenticated subject must be the same as the partial subject", authenticatedSubject == partialSubject);
    }

    @Test
    public void testPerformLoginForCertWithPartialSubjectReturnsSameSubject() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, certAuthenticationData, partialSubject);
        assertTrue("The authenticated subject must be the same as the partial subject", authenticatedSubject == partialSubject);
    }

    @Test(expected = javax.security.auth.login.LoginException.class)
    public void testPerformLoginWithInvalidDataThrowsAuthenticationException() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        AuthenticationData invalidData = createAuthenticationData(BAD_USER, BAD_USER_PWD);
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, invalidData, partialSubject);
        assertNotNull("There must be an authenticated subject", authenticatedSubject);
    }

    @Test(expected = javax.security.auth.login.LoginException.class)
    public void testPerformLoginForTokenWithInvalidDataThrowsAuthenticationException() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        AuthenticationData invalidData = createTokenAuthenticationData(BAD_TOKEN);
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, invalidData, partialSubject);
        assertNotNull("There must be an authenticated subject", authenticatedSubject);
    }

    @Test(expected = javax.security.auth.login.LoginException.class)
    public void testPerformLoginForCertWithInvalidDataThrowsAuthenticationException() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        AuthenticationData invalidData = createCertAuthenticationData(BAD_CERT_CHAIN);
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, invalidData, partialSubject);
        assertNotNull("There must be an authenticated subject", authenticatedSubject);
    }

    @Test
    public void testPerformLoginInvokesCreateCallbackHandlerForAuthenticationData() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, partialSubject);
        boolean wasInvoked = jaasServiceDouble.createCallbackHandlerForAuthenticationDataWasInvoked;
        assertTrue("The createCallbackHandlerForAuthenticationData method must be invoked", wasInvoked);
    }

    @Test
    public void testPerformLoginForTokenInvokesCreateCallbackHandlerForAuthenticationData() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tokenAuthenticationData, partialSubject);
        boolean wasInvoked = jaasServiceDouble.createCallbackHandlerForAuthenticationDataWasInvoked;
        assertTrue("The createCallbackHandlerForAuthenticationData method must be invoked", wasInvoked);
    }

    @Test
    public void testPerformLoginForCertInvokesCreateCallbackHandlerForAuthenticationData() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, certAuthenticationData, partialSubject);
        boolean wasInvoked = jaasServiceDouble.createCallbackHandlerForAuthenticationDataWasInvoked;
        assertTrue("The createCallbackHandlerForAuthenticationData method must be invoked", wasInvoked);
    }

    @Test
    public void testPerformLoginInvokesCreateLoginContext() throws Exception {
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
        assertTrue("The createLoginContext method must be invoked", wasInvoked);
    }

    @Test
    public void testPerformLoginForTokenInvokesCreateLoginContext() throws Exception {
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
        assertTrue("The createLoginContext method must be invoked", wasInvoked);
    }

    @Test
    public void testPerformLoginForCertInvokesCreateLoginContext() throws Exception {
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
        assertTrue("The createLoginContext method must be invoked", wasInvoked);
    }

    @Test
    public void testPerfomLoginWithCallbackHandlerReturnsSubject() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        CallbackHandler callbackHandler = createTestCallbackHandler(GOOD_USER, GOOD_USER_PWD.toCharArray());
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
        assertNotNull("There must be an authenticated subject", authenticatedSubject);
    }

    @Test
    public void testPerfomLoginForTokenWithCallbackHandlerReturnsSubject() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        CallbackHandler callbackHandler = createTestTokenCallbackHandler(GOOD_TOKEN);
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
        assertNotNull("There must be an authenticated subject", authenticatedSubject);
    }

    @Test
    public void testPerfomLoginForCertWithCallbackHandlerReturnsSubject() throws Exception {
        JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
        CallbackHandler callbackHandler = createTestCertCallbackHandler(GOOD_CERT_CHAIN);
        Subject authenticatedSubject = jaasServiceDouble.performLogin(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, callbackHandler, partialSubject);
        assertNotNull("There must be an authenticated subject", authenticatedSubject);
    }

    @Test
    public void testDeactivateRemovesConfiguration() {
        try {
            JAASServiceTestDouble jaasServiceDouble = createActivatedJAASServiceTestDouble();
            jaasServiceDouble.deactivate(componentContext);
            Configuration.getConfiguration();
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

    private JAASServiceImpl createActivatedJAASService() throws IOException {
        JAASServiceImpl jaasService = new JAASServiceImpl();
        jaasService.jaasLoginContextEntries = jaasLoginContextEntries;
        JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
        final String custom = "custom";
        final String pid = "pid";
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASConfigurationFactory.KEY_JAAS_LOGIN_CONFIG, jaasLoginConfigRef);
                will(returnValue(jaasLoginConfig));
                allowing(jaasLoginConfig).getEntries();
                will(returnValue(null));

                allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(jaasConfigurationFactory));
                allowing(jaasLoginModuleConfigRef).getProperty("id");
                will(returnValue(custom));
                allowing(jaasLoginModuleConfigRef).getProperty("service.pid");
                will(returnValue(pid));
                allowing(jaasConfiguration).setJaasLoginContextEntries(jaasLoginContextEntries);
                allowing(jaasConfiguration).getEntries();
                will(returnValue(jaasConfigurationEntries));
            }
        });
        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasConfigurationFactory.setJAASLoginConfig(jaasLoginConfigRef);
        jaasConfigurationFactory.setJAASConfiguration(jaasConfigurationRef);
        jaasConfigurationFactory.activate(componentContext);
        jaasService.activate(componentContext, null);
        return jaasService;
    }

    private JAASServiceTestDouble createActivatedJAASServiceTestDouble() throws IOException {
        JAASServiceTestDouble jaasServiceDouble = new JAASServiceTestDouble();
        jaasServiceDouble.jaasLoginContextEntries = jaasLoginContextEntries;
        JAASServiceImpl.certificateAuthenticators = certificateAuthenticators;
        final Map<String, Object> someProps = new Hashtable<String, Object>();
        String[] values = { "value1", "value2" };
        someProps.put("otherProps", values);
        final String lm = "loginModule";
        final String pid = "pid";
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASConfigurationFactory.KEY_JAAS_LOGIN_CONFIG, jaasLoginConfigRef);
                will(returnValue(jaasLoginConfig));
                allowing(jaasLoginConfig).getEntries();
                will(returnValue(null));

                allowing(componentContext).locateService(JAASServiceImpl.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(jaasConfigurationFactory));

                allowing(jaasLoginModuleConfigRef).getProperty("id");
                will(returnValue(lm));
                allowing(jaasLoginModuleConfigRef).getProperty("service.pid");
                will(returnValue(pid));
                allowing(jaasConfiguration).setJaasLoginContextEntries(jaasLoginContextEntries);
                allowing(jaasConfiguration).getEntries();
                will(returnValue(jaasConfigurationEntries));
            }
        });
        jaasConfigurationFactory.setJAASLoginConfig(jaasLoginConfigRef);
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
        final ComponentContext componentContextMock = mock.mock(ComponentContext.class);
        final BundleContext bundleContextMock = mock.mock(BundleContext.class);
        final Bundle bundle = mock.mock(Bundle.class);
        final BundleWiring wiring = mock.mock(BundleWiring.class);

        final Map<String, Object> someProps = new Hashtable<String, Object>();
        String[] values = { "value1", "value2" };
        someProps.put("otherProps", values);
        mock.checking(new Expectations() {
            {
                allowing(componentContextMock).locateService(JAASLoginModuleConfig.KEY_CLASSLOADING_SVC, classLoadingRef);
                will(returnValue(classLoadSvc));

                allowing(componentContextMock).locateService(JAASServiceImpl.KEY_JAAS_LOGIN_MODULE_CONFIG, jaasLoginModuleConfigRef);
                will(returnValue(jaasLoginModuleConfig));

                allowing(componentContextMock).locateService(JAASConfigurationFactory.KEY_JAAS_CONFIGURATION, jaasConfigurationRef);
                will(returnValue(jaasConfiguration));

                allowing(jaasLoginModuleConfigRef).getProperty("id");
                will(returnValue("testConfigRef"));

                allowing(classLoadSvc).getSharedLibraryClassLoader(sharedLib);
                will(returnValue(loader1));

                allowing(componentContextMock).getBundleContext();
                will(returnValue(bundleContextMock));

                allowing(bundleContextMock).getBundle();
                will(returnValue(bundle));

                allowing(bundle).adapt(BundleWiring.class);
                will(returnValue(wiring));

                allowing(wiring).getClassLoader();
                will(returnValue(loader2));
            }
        });
        return componentContextMock;
    }

    private class JAASServiceTestDouble extends JAASServiceImpl {
        protected boolean createCallbackHandlerForAuthenticationDataWasInvoked = false;

        @Override
        public CallbackHandler createCallbackHandlerForAuthenticationData(final AuthenticationData authenticationData) {
            createCallbackHandlerForAuthenticationDataWasInvoked = true;
            return super.createCallbackHandlerForAuthenticationData(authenticationData);
        }

        @Override
        public void activate(ComponentContext componentContext, Map<String, Object> properties) {
            super.activate(componentContext, properties);

            Configuration testJaasConfiguration = new Configuration() {
                @Override
                public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                    AppConfigurationEntry[] appConfigurationEntry = null;
                    appConfigurationEntry = new AppConfigurationEntry[3];
                    Map<String, ?> options = new HashMap<String, Object>();
                    appConfigurationEntry[0] = new AppConfigurationEntry("com.ibm.ws.security.jaas.common.internal.test.modules.TestLoginModule", LoginModuleControlFlag.REQUIRED, options);
                    appConfigurationEntry[1] = new AppConfigurationEntry("com.ibm.ws.security.jaas.common.internal.test.modules.TestTokenLoginModule", LoginModuleControlFlag.REQUIRED, options);
                    appConfigurationEntry[2] = new AppConfigurationEntry("com.ibm.ws.security.jaas.common.internal.test.modules.TestCertificateLoginModule", LoginModuleControlFlag.REQUIRED, options);
                    return appConfigurationEntry;
                }
            };

            Configuration.setConfiguration(testJaasConfiguration);
        }
    }

    private class JAASServiceTestDoubleForTestingCreateLoginInvocation extends JAASServiceTestDouble {
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
                    if (callback instanceof TokenCallback) {
                        ((TokenCallback) callback).setToken(token);
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
