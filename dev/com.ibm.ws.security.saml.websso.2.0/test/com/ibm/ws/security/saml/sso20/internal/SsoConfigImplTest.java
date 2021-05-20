/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.Constants.SignatureMethodAlgorithm;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

import test.common.SharedOutputManager;

/**
 * Unit test the {@link com.ibm.ws.security.saml.sso20.internal.Config20Impl} class.
 */
public class SsoConfigImplTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final ConfigurationAdmin confAdmin = mockery.mock(ConfigurationAdmin.class, "confAdmin");
    private static final Configuration config = mockery.mock(Configuration.class, "config");
    @SuppressWarnings("unchecked")
    private static final Dictionary<String, Object> dictionary = mockery.mock(Dictionary.class, "dictionary");
    @SuppressWarnings("unchecked")
    private static final ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef = mockery.mock(ConcurrentServiceReferenceMap.class,
                                                                                                                         "authFilterServiceRef");
    private static final HashMap<String, String> filterIdMap = new HashMap<String, String>();
    private static final AuthenticationFilter authenticationFilter = mockery.mock(AuthenticationFilter.class, "authenticationFilter");
    private static final WsLocationAdmin locationAdmin = mockery.mock(WsLocationAdmin.class, "locationAdmin");

    private static final Map<String, Object> SAML_CONFIG_PROPS = new HashMap<String, Object>();
    private static final HashMap<String, String> SAML_FILTER_ID = new HashMap<String, String>();
    private static final String AUTH_FILTER_ID = "authFilterId";
    private static final HashMap<String, String> FILTER_ID_MAP = new HashMap<String, String>();
    private static final SsoSamlService parentSsoService = mockery.mock(SsoSamlService.class, "parentSsoService");

    private static SsoConfigImpl ssoConfig;

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        SAML_CONFIG_PROPS.clear();
        SAML_FILTER_ID.clear();
        setSamlConfigProps();
        setFilterIdMapProps();
        mockery.checking(new Expectations() {
            {
                try {
                    allowing(authFilterServiceRef).getService((String) SAML_CONFIG_PROPS.get(SsoConfigImpl.CFG_KEY_AUTH_FILTER_REF));
                    will(returnValue(authenticationFilter));
                    allowing(dictionary).get(SsoConfigImpl.KEY_ID);
                    will(returnValue(AUTH_FILTER_ID));
                    allowing(dictionary).get(SsoConfigImpl.KEY_trustEngine_path);
                    will(returnValue("trust_engine_path"));
                    allowing(config).getProperties();
                    will(returnValue(dictionary));
                    allowing(confAdmin).getConfiguration(SsoConfigImpl.CFG_KEY_AUTH_FILTER_REF, null);
                    will(returnValue(config));
                    allowing(parentSsoService).searchTrustAnchors(with(any(Collection.class)), with(any(String.class)));
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Unexpected exception was thrown: " + e);
                }
            }
        });

        ssoConfig = new SsoConfigImpl();
        ssoConfig.parentSsoService = parentSsoService;
    }

    private void setSamlConfigProps() {
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_PROVIDER_ID.toString(), "providerId");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_clockSkew, 10l);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_wantAssertionsSigned, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_includeX509InSPMetadata, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_signatureMethodAlgorithm, SignatureMethodAlgorithm.SHA1.toString());
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_authnRequestsSigned, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_forceAuthn, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_isPassive, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_nameIDFormat, "customize");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_customizeNameIDFormat, "customNameIdFormat");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_authnContextClassRef, new String[] { "authnContextClassRef" });
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_idpMetadata, "./files/idpMetadata.xml");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_keyStoreRef, "SAMLKeyStore");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_keyAlias, "SAMLKeyAlias");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_loginPageURL, "https://ibm.com/login");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_errorPageURL, "https://ibm.com/error");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_tokenReplayTimeout, 10 * 60 * 1000l);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_sessionNotOnOrAfter, 120 * 60 * 1000l);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_userIdentifier, "NameId assertion");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_groupIdentifier, "groupIdentifier");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_userUniqueIdentifier, "uniqueIdentifier");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_realmIdentifier, "realmIdentifier");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_includeTokenInSubject, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_mapToUserRegistry, "User");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_disableLtpaCookie, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.CFG_KEY_AUTH_FILTER_REF, "authFilterRef");
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_authnRequestTime, Long.valueOf(600000L));
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_enabled, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_httpsRequired, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_allowCustomCacheKey, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_createSession, false);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_reAuthnOnAssertionExpire, false);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_reAuthnCushion, 0L);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_useRelayStateForTarget, true);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_servletRequestLogoutPerformsSamlLogout, true);
    }

    private void setFilterIdMapProps() {
        FILTER_ID_MAP.put(SsoConfigImpl.CFG_KEY_AUTH_FILTER_REF, null);
    }

    @Test
    public void testPropessProps() {

        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
        }

        //Verify if all props were set correctly
        boolean wantAssertionSigned = (Boolean) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_wantAssertionsSigned);
        assertTrue("WantAssertionsSigned should be " + wantAssertionSigned + " and got " + ssoConfig.isWantAssertionsSigned(),
                   wantAssertionSigned == ssoConfig.isWantAssertionsSigned());
        boolean includeX509InSPMetadata = (Boolean) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_includeX509InSPMetadata);
        assertTrue("includeX509InSPMetadata should be " + includeX509InSPMetadata + " and got " + ssoConfig.isIncludeX509InSPMetadata(),
                   includeX509InSPMetadata == ssoConfig.isIncludeX509InSPMetadata());

        String signatureMethodAlgorithm = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_signatureMethodAlgorithm);
        String samlSsoConfigStr = ssoConfig.toString();
        assertTrue("SignatureMethodAlgorithm should be " + signatureMethodAlgorithm,
                   samlSsoConfigStr.contains(signatureMethodAlgorithm));

        boolean authnRequestsSigned = (Boolean) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_authnRequestsSigned);
        assertTrue("AuthnRequestsSigned should be " + authnRequestsSigned + " and got " + ssoConfig.isAuthnRequestsSigned(),
                   authnRequestsSigned == ssoConfig.isAuthnRequestsSigned());

        boolean forceAuthn = (Boolean) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_forceAuthn);
        assertTrue("ForceAuthn should be " + forceAuthn + " and got " + ssoConfig.isForceAuthn(),
                   forceAuthn == ssoConfig.isForceAuthn());

        boolean isPassive = (Boolean) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_isPassive);
        assertTrue("IsPassive should be " + isPassive + " and got " + ssoConfig.isPassive(),
                   isPassive == ssoConfig.isPassive());

        String[] authContextcClassRef = (String[]) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_authnContextClassRef);
        assertArrayEquals(authContextcClassRef, ssoConfig.getAuthnContextClassRef());

        String idpMetadata = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_idpMetadata);
        assertTrue("IdpMetadata should be " + idpMetadata + " and got " + ssoConfig.getIdpMetadata(),
                   idpMetadata.equals(ssoConfig.getIdpMetadata()));

        String keyStoreRef = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_keyStoreRef);
        assertTrue("KeyStoreRef should be " + keyStoreRef + " and got " + ssoConfig.getKeyStoreRef(),
                   keyStoreRef.equals(ssoConfig.getKeyStoreRef()));

        String keyAlias = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_keyAlias);
        assertTrue("KeyAlias should be " + keyAlias + " and got " + ssoConfig.getKeyAlias(),
                   keyAlias.equals(ssoConfig.getKeyAlias()));

        String loginPageURL = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_loginPageURL);
        assertTrue("LoginPageURL should be " + loginPageURL + " and got " + ssoConfig.getLoginPageURL(),
                   loginPageURL.equals(ssoConfig.getLoginPageURL()));

        String errorPageURL = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_errorPageURL);
        assertTrue("ErrorPageURL should be " + errorPageURL + " and got " + ssoConfig.getErrorPageURL(),
                   errorPageURL.equals(ssoConfig.getErrorPageURL()));

        long tokenReplayTimeout = (Long) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_tokenReplayTimeout);
        assertTrue("TokenReplayTimeout should be " + tokenReplayTimeout + " and got " + ssoConfig.getTokenReplayTimeout(),
                   tokenReplayTimeout == ssoConfig.getTokenReplayTimeout());

        String userIdentifier = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_userIdentifier);
        assertTrue("UserIdentifier should be " + userIdentifier + " and got " + ssoConfig.getTokenReplayTimeout(),
                   userIdentifier.equals(ssoConfig.getUserIdentifier()));

        String groupIdentifier = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_groupIdentifier);
        assertTrue("GroupIdentifier should be " + groupIdentifier + " and got " + ssoConfig.getGroupIdentifier(),
                   groupIdentifier.equals(ssoConfig.getGroupIdentifier()));

        String userUniqueIdentifier = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_userUniqueIdentifier);
        assertTrue("UserUniqueIdentifier should be " + userUniqueIdentifier + " and got " + ssoConfig.getUserUniqueIdentifier(),
                   userUniqueIdentifier.equals(ssoConfig.getUserUniqueIdentifier()));

        String realmIdentifier = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_realmIdentifier);
        assertTrue("RealmIdentifier should be " + realmIdentifier + " and got " + ssoConfig.getRealmIdentifier(),
                   realmIdentifier.equals(ssoConfig.getRealmIdentifier()));

        boolean includeTokenInSubject = (Boolean) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_includeTokenInSubject);
        assertTrue("IncludeTokenInSubject should be " + includeTokenInSubject + " and got " + ssoConfig.isIncludeTokenInSubject(),
                   includeTokenInSubject == ssoConfig.isIncludeTokenInSubject());

        String mapToUserReguistry = SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_mapToUserRegistry).toString();
        assertTrue("MapToUserReguistry should be " + mapToUserReguistry + " and got " + ssoConfig.getMapToUserRegistry(),
                   mapToUserReguistry.equals(ssoConfig.getMapToUserRegistry().toString()));

        String samlSso20ProviderId = (String) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_PROVIDER_ID);
        assertTrue("SamlSso20ProviderId should be " + samlSso20ProviderId + " and got " + ssoConfig.getProviderId(),
                   samlSso20ProviderId.equals(ssoConfig.getProviderId()));

        long clockSkewMilliSeconds = (Long) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_clockSkew);
        assertTrue("ClockSkewMilliSeconds should be " + clockSkewMilliSeconds + " and got " + ssoConfig.getClockSkew(),
                   clockSkewMilliSeconds == ssoConfig.getClockSkew());

        boolean disableLtpaCookie = (Boolean) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_disableLtpaCookie);
        assertTrue("DisableLtpaCookie should be " + disableLtpaCookie + " and got " + ssoConfig.isDisableLtpaCookie(),
                   disableLtpaCookie == ssoConfig.isDisableLtpaCookie());

        boolean useRelayStateForTarget = (Boolean) SAML_CONFIG_PROPS.get(SsoConfigImpl.KEY_useRelayStateForTarget);
        assertTrue("useRelayStateForTarget should be " + useRelayStateForTarget + " and got " + ssoConfig.getUseRelayStateForTarget(),
                   useRelayStateForTarget == ssoConfig.getUseRelayStateForTarget());
    }

    @Test
    public void testGetAuthFilter() {

        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        AuthenticationFilter authFilter = ssoConfig.getAuthFilter(authFilterServiceRef);

        assertFalse("The authFilter should not be null", authFilter == null);
    }

    private void setupConfig() {
        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }
    }

    @Test
    public void testIsServletRequestLogoutPerformsSamlLogout() {
        setupConfig();
        assertTrue("Expected true value for SRLPerformsSL", ssoConfig.isServletRequestLogoutPerformsSamlLogout());
    }

    @Test
    public void testGetSamlVersion() {

        Constants.SamlSsoVersion samlVersion = ssoConfig.getSamlVersion();

        assertTrue("SamlVersion should be " + Constants.SamlSsoVersion.SAMLSSO20 + " and got " + samlVersion,
                   samlVersion.equals(Constants.SamlSsoVersion.SAMLSSO20));
    }

    @Test
    public void testGetAuthFilterId() {
        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, FILTER_ID_MAP);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        String authFilterId = ssoConfig.getAuthFilterId();

        assertTrue("AuthFilterId should be " + AUTH_FILTER_ID + " and got " + authFilterId, authFilterId.equals(AUTH_FILTER_ID));

    }

    @Test
    public void testGetSignatureMethodAlgorithm() {

        String signatureAlgorithm = ssoConfig.getSignatureMethodAlgorithm();

        assertTrue("The signature method algorithm should be " + SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256 + " and got " + signatureAlgorithm,
                   SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256.equals(signatureAlgorithm));
    }

    @Test
    public void testProcessNameIDFormat_Unspecified() {
        String result = ssoConfig.processNameIDFormat(null, Constants.NAME_ID_SHORT_UNSPECIFIED);

        assertTrue("Name ID Format should be " + Constants.NAME_ID_FORMAT_UNSPECIFIED + " and got " + result,
                   result.equals(Constants.NAME_ID_FORMAT_UNSPECIFIED));
    }

    @Test
    public void testProcessTrustEngine_NullTrustEngineType() {
        mockery.checking(new Expectations() {
            {
                one(dictionary).get(SsoConfigImpl.KEY_pkixTrustEngine);
                will(returnValue(null));
            }
        });
        String type = null;
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_userUniqueIdentifier, null);
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_pkixTrustEngine, type);

        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }
    }

    @Test
    public void testProcessTrustEngine_NullCertificate_NullCRL() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(confAdmin).getConfiguration("pkixTrustEngine", null);
                    will(returnValue(config));
                    one(dictionary).get(SsoConfigImpl.KEY_trustEngine_trustAnchor);
                    will(returnValue("trustStore"));
                    one(dictionary).get(SsoConfigImpl.KEY_trustedIssuers);
                    will(returnValue(new String[] { "https://ibm.com/login" }));
                    one(dictionary).get(SsoConfigImpl.KEY_trustEngine_x509cert);
                    will(returnValue(null));
                    one(dictionary).get(SsoConfigImpl.KEY_trustEngine_crl);
                    will(returnValue(null));
                }
            });
        } catch (IOException e1) {
            e1.printStackTrace();
            fail("Unexpected exception was thrown: " + e1);
        }
        String[] value = new String[] { "pkixTrustEngine" };
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_pkixTrustEngine, value);

        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }
    }

    @Test
    public void testProcessTrustEngine_GoodCertificateConfig_GoodCRLConfig() {
        final String[] certs = { SsoConfigImpl.KEY_trustEngine_x509cert };
        final String[] crls = { SsoConfigImpl.KEY_trustEngine_crl };
        final String path = "trust_engine_path";

        try {
            mockery.checking(new Expectations() {
                {
                    one(confAdmin).getConfiguration("pkixTrustEngine", null);
                    will(returnValue(config));
                    one(dictionary).get(SsoConfigImpl.KEY_trustEngine_trustAnchor);
                    will(returnValue("trustStore"));
                    one(dictionary).get(SsoConfigImpl.KEY_trustedIssuers);
                    will(returnValue(new String[] { "https://ibm.com/login" }));
                    one(dictionary).get(SsoConfigImpl.KEY_trustEngine_x509cert);
                    will(returnValue(certs));
                    one(confAdmin).getConfiguration(SsoConfigImpl.KEY_trustEngine_x509cert, null);
                    will(returnValue(config));
                    one(dictionary).get(SsoConfigImpl.KEY_trustEngine_crl);
                    will(returnValue(crls));
                    one(confAdmin).getConfiguration(SsoConfigImpl.KEY_trustEngine_crl, null);
                    will(returnValue(config));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        String[] value = new String[] { "pkixTrustEngine" };
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_pkixTrustEngine, value);

        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        assertTrue("The String " + path + " is not contained in the list x509List.",
                   ssoConfig.getPkixX509CertificateList().contains(path));
        assertTrue("The String " + path + " is not contained in the list crlList.",
                   ssoConfig.getPkixCrlList().contains(path));
    }

    @Test
    public void testProcessTrustEngine_NullCertificateConfig_NullCRLConfig() {
        final String[] certs = { SsoConfigImpl.KEY_trustEngine_x509cert };
        final String[] crls = { SsoConfigImpl.KEY_trustEngine_crl };
        try {
            mockery.checking(new Expectations() {
                {
                    one(confAdmin).getConfiguration("pkixTrustEngine", null);
                    will(returnValue(config));
                    one(dictionary).get(SsoConfigImpl.KEY_trustEngine_trustAnchor);//
                    will(returnValue("trustStore"));
                    one(dictionary).get(SsoConfigImpl.KEY_trustedIssuers);
                    will(returnValue(new String[] { "https://ibm.com/login" }));
                    one(dictionary).get(SsoConfigImpl.KEY_trustEngine_x509cert);
                    will(returnValue(certs));
                    one(confAdmin).getConfiguration(SsoConfigImpl.KEY_trustEngine_x509cert, null);
                    will(returnValue(null));
                    one(dictionary).get(SsoConfigImpl.KEY_trustEngine_crl);
                    will(returnValue(crls));
                    one(confAdmin).getConfiguration(SsoConfigImpl.KEY_trustEngine_crl, null);
                    will(returnValue(null));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }
        String[] type = new String[] { "pkixTrustEngine" };
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_pkixTrustEngine, type);

        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }
    }

    @Test
    public void testGetAuthFilter_NotNull() {
        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);

            AuthenticationFilter result = ssoConfig.getAuthFilter(authFilterServiceRef);
            assertEquals("Expected to receive the correct Authentication Filter object but it was not received.",
                         authenticationFilter, result);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }
    }

    @Test
    public void testGetAuthFilter_NullProperties() {
        AuthenticationFilter result = ssoConfig.getAuthFilter(authFilterServiceRef);
        assertNull("Expected to receive a null value but it was not received.", result);
    }

    @Test
    public void testGetAuthFilter_NullAuthenticationFilter() throws IOException, SamlException {
        final String NULL_FILTER = "null_authentication_filter";

        mockery.checking(new Expectations() {
            {
                one(authFilterServiceRef).getService(NULL_FILTER);
                will(returnValue(null));

                allowing(confAdmin).getConfiguration(NULL_FILTER, null);
                will(returnValue(config));
            }
        });

        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, null);
            SAML_CONFIG_PROPS.put(SsoConfigImpl.CFG_KEY_AUTH_FILTER_REF, NULL_FILTER);

            ssoConfig.getAuthFilter(authFilterServiceRef);
            fail("NullPointerException was not thrown");
        } catch (NullPointerException e) {
            //Do nothing, expected exception.
        }

    }

    @Test
    public void testGetAuthFilterId_NullProperties() {
        String result = ssoConfig.getAuthFilterId();
        assertNull("Expected to receive a null value but it was not received.", result);
    }

    @Test
    public void testGetAuthFilterId_EmptyAuthFilterRef() {
        String result = ssoConfig.getAuthFilterId("");
        assertEquals("Expected to receive the value a null valuebut it was not received.",
                     null, result);
    }

    @Test
    public void testGetAuthFilterId_ExistingAuthFilterRef() {
        final String key = "key";
        final String value = "value";

        SAML_FILTER_ID.put(key, value);
        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, SAML_FILTER_ID);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        String result = ssoConfig.getAuthFilterId(key);
        assertEquals("Expected to receive the value " + value + "but it was not received.",
                     value, result);
    }

    @Test
    public void testGetAuthFilterId_NullConfigAdmin() {
        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, null, SAML_FILTER_ID);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        String result = ssoConfig.getAuthFilterId("non-existent");
        assertEquals("Expected to receive the a null value but it was not received.",
                     null, result);
    }

    @Test
    public void testGetAuthFilterId_PropertiesEqualsNull() throws IOException {
        final String NON_EXISTENT = "non-existent";
        final Configuration configuration = mockery.mock(Configuration.class, "configuration");

        mockery.checking(new Expectations() {
            {
                one(confAdmin).getConfiguration(NON_EXISTENT, null);
                will(returnValue(configuration));
                one(configuration).getProperties();
                will(returnValue(null));
            }
        });
        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, SAML_FILTER_ID);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        String result = ssoConfig.getAuthFilterId(NON_EXISTENT);
        assertEquals("Expected to receive the a null value but it was not received.",
                     null, result);
    }

    @Test
    public void testGetSignatureMethodAlgorithm_SHA1Algorithm() {
        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_signatureMethodAlgorithm, "SHA1");

        String result = ssoConfig.getSignatureMethodAlgorithm();

        assertTrue("Algorithm should be " + SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1 + " and got " + result,
                   result.equals(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1));
    }

    @Test
    public void testGetSignatureMethodAlgorithm_NoAlgorithm() {
        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_signatureMethodAlgorithm, "");

        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        String result = ssoConfig.getSignatureMethodAlgorithm();

        assertTrue("Algorithm should be " + SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256 + " and got " + result,
                   result.equals(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256));
    }

    @Test
    public void testGetSpCookieName() {
        final String spCookieName = "spCookieName";

        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_spCookieName, spCookieName);
        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        String result = ssoConfig.getSpCookieName(null);

        assertTrue("Sp Cookie Name should be " + spCookieName + " and got " + result,
                   result.equals(spCookieName));
    }

    @Test
    public void testGetSpCookieName_NullSpCookieName() {
        mockery.checking(new Expectations() {
            {
                one(locationAdmin).resolveString(Constants.WLP_USER_DIR);
                will(returnValue("/Users/IBM_ADMIN/"));
                one(locationAdmin).getServerName();
                will(returnValue("guadalajara"));
            }
        });

        SAML_CONFIG_PROPS.put(SsoConfigImpl.KEY_spCookieName, null);
        try {
            ssoConfig.setConfig(SAML_CONFIG_PROPS, confAdmin, filterIdMap);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e);
        }

        String result = ssoConfig.getSpCookieName(locationAdmin);

        assertTrue("The correct Sp Cookie Name was not received. But recived a '" + result + "'",
                   result.startsWith(Constants.COOKIE_NAME_SP_PREFIX));
    }
}
