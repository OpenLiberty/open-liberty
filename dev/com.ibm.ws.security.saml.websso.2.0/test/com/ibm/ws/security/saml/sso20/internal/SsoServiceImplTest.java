/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.keyId;
import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.keyServicePID;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.CFG_KEY_AUTH_FILTER_REF;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_PROVIDER_ID;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_authnRequestTime;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_authnRequestsSigned;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_clockSkew;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_disableLtpaCookie;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_errorPageURL;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_forceAuthn;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_groupIdentifier;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_idpMetadata;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_includeTokenInSubject;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_isPassive;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_keyAlias;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_keyStoreRef;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_loginPageURL;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_mapToUserRegistry;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_realmIdentifier;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_signatureMethodAlgorithm;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_tokenReplayTimeout;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_userIdentifier;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_userUniqueIdentifier;
import static com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl.KEY_wantAssertionsSigned;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants.SignatureMethodAlgorithm;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.UnsolicitedResponseCache;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

/**
 * Unit test {@link SsoServiceImpl} class.
 */
@SuppressWarnings("unchecked")
public class SsoServiceImplTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final Map<String, Object> SAML_CONFIG_PROPS = new HashMap<String, Object>();

    private static final ConfigurationAdmin configAdmin = mockery.mock(ConfigurationAdmin.class, "configAdmin");

    private static final ServiceReference<AuthenticationFilter> authFilter = mockery.mock(ServiceReference.class, "authFilter");
    private static final ServiceReference<AuthenticationFilter> newReference = mockery.mock(ServiceReference.class, "newAuthFilter");
    private static final ServiceReference<KeyStoreService> keyStoreReference = mockery.mock(ServiceReference.class, "securityReference");

    private static final ComponentContext cc = mockery.mock(ComponentContext.class, "componetContext");
    private static final BundleContext bc = mockery.mock(BundleContext.class);
    private static final KeyStoreService keyStoreService = mockery.mock(KeyStoreService.class, "keyStoreService");
    private static final PrivateKey privateKey = mockery.mock(PrivateKey.class, "privateKey");
    private static final Certificate certificate = mockery.mock(Certificate.class, "certificate");
    private static final SsoConfigImpl ssoConfigImpl = mockery.mock(SsoConfigImpl.class, "ssoConfigImpl");
    private static final SsoServiceImpl service = new SsoServiceImpl();
    private static final SSLSupport sslSupport = mockery.mock(SSLSupport.class, "sslSupport");
    private static final JSSEHelper jsseHelper = mockery.mock(JSSEHelper.class, "jsseHelper");
    private static final X509Certificate x509Certificate = mockery.mock(X509Certificate.class, "x509Certificate");
    private static final ServiceReference<SSLSupport> sslSupportRef = mockery.mock(ServiceReference.class, "sslSupportRef");
    private static final ConfigurationAdmin newConfigAdmin = mockery.mock(ConfigurationAdmin.class, "newConfigAdmin");

    private static final String KEY_PWD = "key_pwd";
    private static final String ALIAS = "samlsp";
    private static final String TRUST_ANCHOR_NAME = "trustAnchorName";
    private static final String CERT_NAME = "certName";

    private static final Collection<String> certName = new HashSet<String>();
    private static final Collection<X509Certificate> trustAnchors = new HashSet<X509Certificate>();
    private static final KeyStoreException kse = new KeyStoreException();
    private static final CertificateException ce = new CertificateException();
    private static final SsoConfigImpl samlConfig = new SsoConfigImpl();

    @BeforeClass
    public static void setup() throws Exception {
        outputMgr.trace("*=all");
        setSamlConfigProps();

        mockery.checking(new Expectations() {
            {
                one(authFilter).getProperty(SsoServiceImpl.KEY_SERVICE_PID);
                will(returnValue(keyServicePID));
                one(authFilter).getProperty(SsoServiceImpl.KEY_ID);
                will(returnValue(keyId));
                one(authFilter).getProperty(SERVICE_ID);
                will(returnValue(0l));
                one(authFilter).getProperty(SERVICE_RANKING);
                will(returnValue(0l));

                atMost(2).of(newReference).getProperty(SsoServiceImpl.KEY_SERVICE_PID);
                will(returnValue(keyServicePID));
                atMost(2).of(newReference).getProperty(SsoServiceImpl.KEY_ID);
                will(returnValue(keyId));
                atMost(2).of(newReference).getProperty(SERVICE_ID);
                will(returnValue(0l));
                atMost(2).of(newReference).getProperty(SERVICE_RANKING);
                will(returnValue(0l));

                allowing(bc).registerService(with(any(Class.class)), with(any(FileMonitor.class)), with(any(Dictionary.class)));
                will(returnValue(null));
                allowing(cc).getBundleContext();
                will(returnValue(bc));
                allowing(bc).getBundle();
                allowing(cc).locateService(SsoServiceImpl.KEY_KEYSTORE_SERVICE, keyStoreReference);
                will(returnValue(keyStoreService));
                allowing(cc).locateService(SsoServiceImpl.KEY_SSL_SUPPORT, sslSupportRef);
                will(returnValue(sslSupport));

                one(keyStoreService).getPrivateKeyFromKeyStore((String) SAML_CONFIG_PROPS.get(KEY_keyStoreRef),
                                                               (String) SAML_CONFIG_PROPS.get(KEY_keyAlias), null);
                will(returnValue(privateKey));
                one(keyStoreService).getCertificateFromKeyStore((String) SAML_CONFIG_PROPS.get(KEY_keyStoreRef),
                                                                (String) SAML_CONFIG_PROPS.get(KEY_keyAlias));
                will(returnValue(certificate));

                allowing(ssoConfigImpl).getKeyStoreRef();
                will(returnValue(null));
                allowing(ssoConfigImpl).getKeyAlias();
                will(returnValue(null));
                allowing(ssoConfigImpl).getKeyPassword();
                will(returnValue(KEY_PWD));

                allowing(sslSupport).getJSSEHelper();
                will(returnValue(jsseHelper));

                allowing(configAdmin).getConfiguration("authFilterRef", null);
                will(returnValue(null));

                allowing(sslSupportRef).getProperty("service.pid");
                will(returnValue(null));
            }
        });
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @After
    public void after() {
        service.setSamlConfig(samlConfig);
        certName.clear();
        trustAnchors.clear();
    }

    private static void setSamlConfigProps() {
        SAML_CONFIG_PROPS.put(KEY_PROVIDER_ID.toString(), "providerId");
        SAML_CONFIG_PROPS.put(KEY_clockSkew, 10l);
        SAML_CONFIG_PROPS.put(KEY_wantAssertionsSigned, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_includeX509InSPMetadata, true);
        SAML_CONFIG_PROPS.put(KEY_signatureMethodAlgorithm, SignatureMethodAlgorithm.SHA1.toString());
        SAML_CONFIG_PROPS.put(KEY_authnRequestsSigned, true);
        SAML_CONFIG_PROPS.put(KEY_forceAuthn, true);
        SAML_CONFIG_PROPS.put(KEY_isPassive, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_authnContextClassRef, new String[] { "authnContextClassRef" });
        SAML_CONFIG_PROPS.put(KEY_idpMetadata, "/resources/security/idpMetadata.xml");
        SAML_CONFIG_PROPS.put(KEY_keyStoreRef, "SAMLKeyStore");
        SAML_CONFIG_PROPS.put(KEY_keyAlias, "SAMLKeyAlias");
        SAML_CONFIG_PROPS.put(KEY_loginPageURL, "https://ibm.com/login");
        SAML_CONFIG_PROPS.put(KEY_errorPageURL, "https://ibm.com/error");
        SAML_CONFIG_PROPS.put(KEY_tokenReplayTimeout, 10 * 60 * 1000l);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_sessionNotOnOrAfter, 120 * 60 * 1000l);
        SAML_CONFIG_PROPS.put(KEY_userIdentifier, "NameId assertion");
        SAML_CONFIG_PROPS.put(KEY_groupIdentifier, "groupIdentifier");
        SAML_CONFIG_PROPS.put(KEY_userUniqueIdentifier, "uniqueIdentifier");
        SAML_CONFIG_PROPS.put(KEY_realmIdentifier, "realmIdentifier");
        SAML_CONFIG_PROPS.put(KEY_includeTokenInSubject, true);
        SAML_CONFIG_PROPS.put(KEY_mapToUserRegistry, "User");
        SAML_CONFIG_PROPS.put(CFG_KEY_AUTH_FILTER_REF, "authFilterRef");
        SAML_CONFIG_PROPS.put(KEY_disableLtpaCookie, true);
        SAML_CONFIG_PROPS.put(KEY_authnRequestTime, Long.valueOf(600000L));
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_enabled, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_httpsRequired, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_allowCustomCacheKey, true);
        SAML_CONFIG_PROPS.put(SsoServiceImpl.KEY_inboundPropagation, "none");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_reAuthnOnAssertionExpire, false);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_reAuthnCushion, 0L);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_useRelayStateForTarget, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_servletRequestLogoutPerformsSamlLogout, true);
    }

    @Test
    public void testConfigurationAdmin() {
        ConfigurationAdmin currentConfigAdmin;

        service.setConfigurationAdmin(configAdmin);
        currentConfigAdmin = service.getConfigurationAdmin();
        assertEquals("The configuration admin was not set, expected " + configAdmin + "and got " + currentConfigAdmin,
                     configAdmin, currentConfigAdmin);
        //Verify if the configuration is updated correctly.
        service.updateConfigurationAdmin(newConfigAdmin);
        currentConfigAdmin = service.getConfigurationAdmin();
        assertEquals("Current Configuration Administrator did not change, sould be " + newConfigAdmin + " and got " + currentConfigAdmin,
                     newConfigAdmin, currentConfigAdmin);
        //Verify if the configuration is unset correctly
        service.unsetConfigurationAdmin(null);
        currentConfigAdmin = service.getConfigurationAdmin();
        assertNull("Current Configuration Administrator should be null", currentConfigAdmin);
    }

    @Test
    public void testAtivate() {
        try {
            service.setConfigurationAdmin(configAdmin);
            service.activate(cc, SAML_CONFIG_PROPS);
        } catch (Exception e) {
            fail("Unexpected exception caught " + e.toString());
        }
    }

    @Test
    public void testModified() {
        try {
            service.setConfigurationAdmin(configAdmin);
            SAML_CONFIG_PROPS.remove(SsoServiceImpl.KEY_PROVIDER_ID);
            service.modified(cc, SAML_CONFIG_PROPS);
        } catch (Exception e) {
            fail("Unexpected exception caught " + e.toString());
        }
    }

    @Test
    public void testDeactivate() {
        try {
            service.setConfigurationAdmin(configAdmin);
            service.activate(cc, SAML_CONFIG_PROPS);
            service.deactivate(cc);
        } catch (Exception e) {
            fail("Unexpected exception caught " + e.toString());
        }
    }

    @Test
    public void testGetAcsCookieCache() {
        Cache cache = service.getAcsCookieCache((String) SAML_CONFIG_PROPS.get(KEY_PROVIDER_ID));

        assertNotNull("The cookie cache returned should not be null.", cache);
    }

    @Test
    public void testGetPrivateKey() throws KeyStoreException, CertificateException {
        service.setConfigurationAdmin(configAdmin);

        service.activate(cc, SAML_CONFIG_PROPS);
        service.setKeyStoreService(keyStoreReference);
        PrivateKey result = service.getPrivateKey();
        assertEquals(privateKey, result);

    }

    @Test
    public void testGetSignatureCertificate() throws KeyStoreException, CertificateException {
        service.setConfigurationAdmin(configAdmin);

        service.activate(cc, SAML_CONFIG_PROPS);
        service.setKeyStoreService(keyStoreReference);
        Certificate result = service.getSignatureCertificate();

        assertEquals(certificate, result);

    }

    @Test
    public void testGetDefauldKeyStoreName() throws SSLException {
        final String keyStoreName = "keyStoreName";
        final Properties props = new Properties();
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);

        props.put("com.ibm.ssl.keyStoreName", keyStoreName);
        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties("", connectionInfo, null, true);
                will(returnValue(props));
            }
        });

        service.setConfigurationAdmin(configAdmin);

        service.activate(cc, SAML_CONFIG_PROPS);
        service.setSslSupport(sslSupportRef);

        String result = service.getDefaultKeyStoreProperty("com.ibm.ssl.keyStoreName");

        assertEquals(keyStoreName, result);
    }

    @Test
    public void testGetUnsolicitedResponseCache_NonExistentObject() {
        final String NON_EXISTENT = "non-existent";
        SsoServiceImpl.replayCacheMap.clear();

        UnsolicitedResponseCache responseCache = service.getUnsolicitedResponseCache(NON_EXISTENT);
        assertTrue("The expected UnsolicitedResponseCache object was not received.",
                   (responseCache != null) && (responseCache.equals(SsoServiceImpl.replayCacheMap.get(NON_EXISTENT))));
    }

    @Test
    public void testGetUnsolicitedResponseCache_ExistentObject() {
        final String EXISTENT = "existent";
        final UnsolicitedResponseCache cache = new UnsolicitedResponseCache(0, 0, 0);

        SsoServiceImpl.replayCacheMap.clear();
        SsoServiceImpl.replayCacheMap.put(EXISTENT, cache);

        UnsolicitedResponseCache responseCache = service.getUnsolicitedResponseCache(EXISTENT);
        assertTrue("The expected UnsolicitedResponseCache object was not received.", (responseCache != null) && (responseCache.equals(cache)));
    }

    @Test
    public void testGetPrivateKey_NullKeyAlias() throws SSLException, KeyStoreException, CertificateException {
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties("", connectionInfo, null, true);
                will(returnValue(null));

                one(keyStoreService).getPrivateKeyFromKeyStore(null, ALIAS, KEY_PWD);
                will(returnValue(privateKey));
            }
        });

        service.activate(cc, SAML_CONFIG_PROPS);
        service.setSslSupport(sslSupportRef);
        service.setSamlConfig(ssoConfigImpl);
        service.setKeyStoreService(keyStoreReference);

        PrivateKey key = service.getPrivateKey();
        assertTrue("The expected private key was not received.", key.equals(privateKey));
    }

    @Test
    public void testGetPrivateKey_ThrowsKeyStoreException() throws KeyStoreException, CertificateException, SSLException {
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties("", connectionInfo, null, true);
                will(returnValue(null));

                one(keyStoreService).getPrivateKeyFromKeyStore(null, ALIAS, KEY_PWD);
                will(throwException(kse));
                one(keyStoreService).getPrivateKeyFromKeyStore(null);
                will(returnValue(privateKey));
            }
        });

        service.activate(cc, SAML_CONFIG_PROPS);
        service.setSslSupport(sslSupportRef);
        service.setSamlConfig(ssoConfigImpl);
        service.setKeyStoreService(keyStoreReference);

        PrivateKey key = service.getPrivateKey();
        assertTrue("The expected private key was not received.", key.equals(privateKey));
    }

    @Test
    public void testGetPrivateKey_ThrowsCertificateException() throws KeyStoreException, CertificateException, SSLException {
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties("", connectionInfo, null, true);
                will(returnValue(null));

                one(keyStoreService).getPrivateKeyFromKeyStore(null, ALIAS, KEY_PWD);
                will(throwException(ce));
                one(keyStoreService).getPrivateKeyFromKeyStore(null);
                will(returnValue(privateKey));
            }
        });

        service.activate(cc, SAML_CONFIG_PROPS);
        service.setSslSupport(sslSupportRef);
        service.setSamlConfig(ssoConfigImpl);
        service.setKeyStoreService(keyStoreReference);

        PrivateKey key = service.getPrivateKey();
        assertTrue("The expected private key was not received.", key.equals(privateKey));
    }

    @Test
    public void testGetSignatureCertificate_NullKeyAlias() throws KeyStoreException, CertificateException, SSLException {
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties("", connectionInfo, null, true);
                will(returnValue(null));

                one(keyStoreService).getCertificateFromKeyStore(null, ALIAS);
                will(returnValue(certificate));
            }
        });

        service.activate(cc, SAML_CONFIG_PROPS);
        service.setSslSupport(sslSupportRef);
        service.setSamlConfig(ssoConfigImpl);
        service.setKeyStoreService(keyStoreReference);

        Certificate cert = service.getSignatureCertificate();
        assertTrue("The expected certificate was not received.", cert.equals(certificate));
    }

    @Test
    public void testGetSignatureCertificate_ThrowsKeyStoreException() throws KeyStoreException, CertificateException, SSLException {
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties("", connectionInfo, null, true);
                will(returnValue(null));

                one(keyStoreService).getCertificateFromKeyStore(null, ALIAS);
                will(throwException(kse));
                one(keyStoreService).getX509CertificateFromKeyStore(null);
                will(returnValue(x509Certificate));
            }
        });

        service.activate(cc, SAML_CONFIG_PROPS);
        service.setSslSupport(sslSupportRef);
        service.setSamlConfig(ssoConfigImpl);
        service.setKeyStoreService(keyStoreReference);

        Certificate cert = service.getSignatureCertificate();
        assertTrue("The expected certificate was not received.", cert.equals(x509Certificate));
    }

    @Test
    public void testGetSignatureCertificate_ThrowsCertificateException() throws KeyStoreException, CertificateException, SSLException {
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties("", connectionInfo, null, true);
                will(returnValue(null));

                one(keyStoreService).getCertificateFromKeyStore(null, ALIAS);
                will(throwException(ce));
                one(keyStoreService).getX509CertificateFromKeyStore(null);
                will(returnValue(x509Certificate));
            }
        });

        service.activate(cc, SAML_CONFIG_PROPS);
        service.setSslSupport(sslSupportRef);
        service.setSamlConfig(ssoConfigImpl);
        service.setKeyStoreService(keyStoreReference);

        Certificate cert = service.getSignatureCertificate();
        assertTrue("The expected certificate was not received.", cert.equals(x509Certificate));
    }

    @Test
    public void testSearchTrustAnchors() throws KeyStoreException, CertificateException {
        certName.add(CERT_NAME);

        mockery.checking(new Expectations() {
            {
                one(keyStoreService).getTrustedCertEntriesInKeyStore(TRUST_ANCHOR_NAME);
                will(returnValue(certName));
                one(keyStoreService).getX509CertificateFromKeyStore(TRUST_ANCHOR_NAME, CERT_NAME);
                will(returnValue(x509Certificate));
            }
        });

        try {
            service.activate(cc, SAML_CONFIG_PROPS);
            service.setSslSupport(sslSupportRef);
            service.setSamlConfig(ssoConfigImpl);
            service.setKeyStoreService(keyStoreReference);

            service.searchTrustAnchors(trustAnchors, TRUST_ANCHOR_NAME);
            assertTrue("The Collection trustAnchors must contain the object x509Certificate.", trustAnchors.contains(x509Certificate));
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testSearchTrustAnchors_ThrowsKeyStoreException() throws KeyStoreException, CertificateException, SSLException {
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        certName.add(CERT_NAME);

        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties("", connectionInfo, null, true);
                will(returnValue(null));

                one(keyStoreService).getTrustedCertEntriesInKeyStore(null);
                will(throwException(kse));
            }
        });

        try {
            service.activate(cc, SAML_CONFIG_PROPS);
            service.setSslSupport(sslSupportRef);
            service.setSamlConfig(ssoConfigImpl);
            service.setKeyStoreService(keyStoreReference);

            service.searchTrustAnchors(trustAnchors, null);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
            assertTrue("The Collection trustAnchors mustn't contain the object x509Certificate.", !(trustAnchors.contains(x509Certificate)));
        }
    }

    @Test
    public void testSearchTrustAnchors_ThrowsCertificateException() throws KeyStoreException, CertificateException, SSLException {
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        certName.add(CERT_NAME);

        mockery.checking(new Expectations() {
            {
                one(jsseHelper).getProperties("", connectionInfo, null, true);
                will(returnValue(null));

                one(keyStoreService).getTrustedCertEntriesInKeyStore(null);
                will(returnValue(certName));
                one(keyStoreService).getX509CertificateFromKeyStore(null, CERT_NAME);
                will(throwException(ce));
            }
        });

        try {
            service.activate(cc, SAML_CONFIG_PROPS);
            service.setSslSupport(sslSupportRef);
            service.setSamlConfig(ssoConfigImpl);
            service.setKeyStoreService(keyStoreReference);

            service.searchTrustAnchors(trustAnchors, "");
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
            assertTrue("The Collection trustAnchors mustn't contain the object x509Certificate.", !(trustAnchors.contains(x509Certificate)));
        }
    }
}