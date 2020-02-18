/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.rs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.opensaml.xml.signature.SignatureConstants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.PkixTrustEngineConfig;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import test.common.SharedOutputManager;

public class RsSamlConfigImplTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    @Rule
    public TestRule managerRule = outputMgr;

    @Rule
    public final TestName testName = new TestName();

    private static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static RsSamlConfigImpl rsSamlConfigImpl = null;

    private static final States stateMachine = mockery.states("states");
    private static ComponentContext cc = mockery.mock(ComponentContext.class, "cc");
    private static Configuration certConfig = mockery.mock(Configuration.class, "certConfig");
    private static Configuration config = mockery.mock(Configuration.class, "config");
    private static ConfigurationAdmin configAdminn = mockery.mock(ConfigurationAdmin.class, "configAdminn");
    @SuppressWarnings("unchecked")
    private static Dictionary<String, Object> trustEngineProps = mockery.mock(Dictionary.class, "trustEngineProps");
    private static final SsoSamlService parentSsoService = mockery.mock(SsoSamlService.class, "parentSsoService");

    private static Map<String, Object> props = new HashMap<String, Object>();
    private final String FILTER_SERVICE_REF = "filter.service.ref";
    private static final String SIGN_ALG_SHA1 = "SHA1";
    private static final String SIGN_ALG_SHA128 = "SHA128";
    private static final String SIGN_ALG_SHA256 = "SHA256";
    private static final String SETUP = "setUp";
    static HashMap<String, String> filterIdMap = new HashMap<String, String>();

    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        stateMachine.startsAs(SETUP);
        activate();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        stateMachine.become(SETUP);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    private static void activate() throws IOException {
        final long CLOCK = 100000000;
        final String[] certs = { "conf", "c" };
        final String AUDIENCE = "audience";
        final String[] audiences = { AUDIENCE };
        final String ID = "id";
        final String USER_ID = "user.id";
        final String GROUP_ID = "group.id";
        final String UID = "uid";
        final String REALM_ID = "realm.id";
        final String PKIX_TRUST_ENGINE = "pkixTrustEngine";
        final String REALM_NAME = "realm.name";
        final String HEADER_NAME = "header.name";
        final String kEYSTORE_REF = "keystore.ref";
        final String KEY_ALIAS = "key.alias";
        final String TRUST_ENGINE_PATH = "trust.engine.path";
        final String TRUST_ENGINE_TRUSTANCHOR = "trust.engine.trust.anchor";
        final String urlEncoded = java.net.URLEncoder.encode("http://example.ibm.com", "UTF-8");

        props.clear();
        props.put(RsSamlConfigImpl.KEY_ID, ID);
        props.put(RsSamlConfigImpl.KEY_clockSkew, CLOCK);
        props.put(RsSamlConfigImpl.KEY_userIdentifier, USER_ID);
        props.put(RsSamlConfigImpl.KEY_groupIdentifier, GROUP_ID);
        props.put(RsSamlConfigImpl.KEY_userUniqueIdentifier, UID);
        props.put(RsSamlConfigImpl.KEY_realmIdentifier, REALM_ID);
        props.put(RsSamlConfigImpl.KEY_mapToUserRegistry, "User"); //
        props.put(RsSamlConfigImpl.KEY_disableLtpaCookie, false); //
        props.put(RsSamlConfigImpl.KEY_wantAssertionsSigned, true);
        props.put(RsSamlConfigImpl.KEY_includeX509InSPMetadata, true);
        props.put(RsSamlConfigImpl.KEY_pkixTrustEngine, "key");
        props.put(RsSamlConfigImpl.KEY_audiences, audiences);
        props.put(RsSamlConfigImpl.KEY_signatureMethodAlgorithm, SIGN_ALG_SHA128);
        props.put(RsSamlConfigImpl.KEY_realmName, REALM_NAME);
        props.put(RsSamlConfigImpl.KEY_headerName, new String[] { HEADER_NAME });
        props.put(RsSamlConfigImpl.KEY_pkixTrustEngine, new String[] { PKIX_TRUST_ENGINE });
        props.put(RsSamlConfigImpl.KEY_keyStoreRef, kEYSTORE_REF);
        props.put(RsSamlConfigImpl.KEY_keyAlias, KEY_ALIAS);
        props.put(RsSamlConfigImpl.KEY_enabled, true);
        props.put(RsSamlConfigImpl.KEY_servletRequestLogoutPerformsSamlLogout, true);

        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdminn).getConfiguration("key");
                    will(returnValue(config));
                    allowing(configAdminn).getConfiguration(certs[0], "");
                    will(returnValue(certConfig));
                    allowing(configAdminn).getConfiguration(certs[1], "");
                    will(returnValue(certConfig));
                    allowing(configAdminn).getConfiguration(PKIX_TRUST_ENGINE, "");
                    will(returnValue(config));

                    allowing(config).getProperties();
                    will(returnValue(trustEngineProps));

                    allowing(trustEngineProps).get(RsSamlConfigImpl.KEY_trustEngine_trustAnchor);
                    will(returnValue(TRUST_ENGINE_TRUSTANCHOR));
                    allowing(trustEngineProps).get(RsSamlConfigImpl.KEY_trustEngine_x509cert);
                    will(returnValue(certs));
                    when(stateMachine.is(SETUP));
                    allowing(trustEngineProps).get(RsSamlConfigImpl.KEY_trustEngine_path);
                    will(returnValue(TRUST_ENGINE_PATH));
                    allowing(trustEngineProps).get(RsSamlConfigImpl.KEY_trustEngine_crl);
                    will(returnValue(certs));
                    when(stateMachine.is(SETUP));
                    allowing(trustEngineProps).get(PkixTrustEngineConfig.KEY_trustedIssuers);
                    will(returnValue(new String[] { urlEncoded }));

                    allowing(certConfig).getProperties();
                    will(returnValue(trustEngineProps));

                    allowing(parentSsoService).searchTrustAnchors(with(any(Collection.class)), with(any(String.class)));
                }
            });
        } catch (SamlException e) {
            e.printStackTrace();
        }

        rsSamlConfigImpl = new RsSamlConfigImpl(cc, props, configAdminn, filterIdMap, parentSsoService);
        assertEquals("Expected " + ID + " but received " + rsSamlConfigImpl.getProviderId() + ".", ID, rsSamlConfigImpl.getProviderId());
        assertEquals("Expected " + CLOCK + " but received " + rsSamlConfigImpl.getClockSkew() + ".", CLOCK, rsSamlConfigImpl.getClockSkew());
        assertEquals("Expected " + USER_ID + " but received " + rsSamlConfigImpl.getUserIdentifier() + ".", USER_ID, rsSamlConfigImpl.getUserIdentifier());
        assertEquals("Expected " + GROUP_ID + " but received " + rsSamlConfigImpl.getGroupIdentifier() + ".", GROUP_ID, rsSamlConfigImpl.getGroupIdentifier());
        assertEquals("Expected " + UID + " but received " + rsSamlConfigImpl.getUserUniqueIdentifier() + ".", UID, rsSamlConfigImpl.getUserUniqueIdentifier());
        assertEquals("Expected " + REALM_ID + " but received " + rsSamlConfigImpl.getRealmIdentifier() + ".", REALM_ID, rsSamlConfigImpl.getRealmIdentifier());
        assertEquals("Expected " + AUDIENCE + " but received " + rsSamlConfigImpl.getAudiences()[0] + ".", AUDIENCE, rsSamlConfigImpl.getAudiences()[0]);
        assertEquals("Expected " + SignatureConstants.MORE_ALGO_NS + "rsa-sha128" + " but received " + rsSamlConfigImpl.getSignatureMethodAlgorithm() + ".",
                     SignatureConstants.MORE_ALGO_NS + "rsa-sha128", rsSamlConfigImpl.getSignatureMethodAlgorithm());
        assertEquals("Expected " + REALM_NAME + " but received " + rsSamlConfigImpl.getRealmName() + ".", REALM_NAME, rsSamlConfigImpl.getRealmName());
        assertEquals("Expected " + HEADER_NAME + " but received " + rsSamlConfigImpl.getHeaderName() + ".", HEADER_NAME, rsSamlConfigImpl.getHeaderName());
        assertEquals("Expected " + kEYSTORE_REF + " but received " + rsSamlConfigImpl.getKeyStoreRef() + ".", kEYSTORE_REF, rsSamlConfigImpl.getKeyStoreRef());
        assertEquals("Expected " + KEY_ALIAS + " but received " + rsSamlConfigImpl.getKeyAlias() + ".", KEY_ALIAS, rsSamlConfigImpl.getKeyAlias());
        assertEquals("Expected User but get '" + rsSamlConfigImpl.getMapToUserRegistry() + "'", com.ibm.ws.security.saml.Constants.MapToUserRegistry.User,
                     rsSamlConfigImpl.getMapToUserRegistry());
        assertTrue(rsSamlConfigImpl.isWantAssertionsSigned());
        assertFalse(rsSamlConfigImpl.isDisableLtpaCookie());
        assertTrue(rsSamlConfigImpl.toString().contains(ID));
        assertEquals("Expected " + TRUST_ENGINE_PATH + " but received " + rsSamlConfigImpl.getPkixX509CertificateList().get(0) + ".", TRUST_ENGINE_PATH,
                     rsSamlConfigImpl.getPkixX509CertificateList().get(0));
        assertEquals("Expected " + TRUST_ENGINE_PATH + " but received " + rsSamlConfigImpl.getPkixCrlList().get(0) + ".", TRUST_ENGINE_PATH,
                     rsSamlConfigImpl.getPkixCrlList().get(0));
        assertEquals("Expected " + TRUST_ENGINE_TRUSTANCHOR + " but received " + rsSamlConfigImpl.getPkixTrustAnchorName() + ".", TRUST_ENGINE_TRUSTANCHOR,
                     rsSamlConfigImpl.getPkixTrustAnchorName());
    }

    @Test
    public void testGetSignatureAlgotithm() {
        props.put(RsSamlConfigImpl.KEY_signatureMethodAlgorithm, SIGN_ALG_SHA1);
        rsSamlConfigImpl = new RsSamlConfigImpl(cc, props, configAdminn, filterIdMap, parentSsoService);
        assertEquals("Expected " + SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1 + " but received " + rsSamlConfigImpl.getSignatureMethodAlgorithm() + ".",
                     SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1, rsSamlConfigImpl.getSignatureMethodAlgorithm());

        props.put(RsSamlConfigImpl.KEY_signatureMethodAlgorithm, SIGN_ALG_SHA128);
        rsSamlConfigImpl = new RsSamlConfigImpl(cc, props, configAdminn, filterIdMap, parentSsoService);
        assertEquals("Expected " + SignatureConstants.MORE_ALGO_NS + "rsa-sha128" + " but received " + rsSamlConfigImpl.getSignatureMethodAlgorithm() + ".",
                     SignatureConstants.MORE_ALGO_NS + "rsa-sha128", rsSamlConfigImpl.getSignatureMethodAlgorithm());

        props.put(RsSamlConfigImpl.KEY_signatureMethodAlgorithm, SIGN_ALG_SHA256);
        rsSamlConfigImpl = new RsSamlConfigImpl(cc, props, configAdminn, filterIdMap, parentSsoService);
        assertEquals("Expected " + SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256 + " but received " + rsSamlConfigImpl.getSignatureMethodAlgorithm() + ".",
                     SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256, rsSamlConfigImpl.getSignatureMethodAlgorithm());

        props.put(RsSamlConfigImpl.KEY_signatureMethodAlgorithm, "no-alg");
        rsSamlConfigImpl = new RsSamlConfigImpl(cc, props, configAdminn, filterIdMap, parentSsoService);
        assertEquals("Expected " + SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256 + " but received " + rsSamlConfigImpl.getSignatureMethodAlgorithm() + ".",
                     SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256, rsSamlConfigImpl.getSignatureMethodAlgorithm());
    }

    @Test
    public void testGetPassword() {
        final String PWD = "security";
        final String pwdEncoded = PasswordUtil.passwordEncode(PWD);
        final SerializableProtectedString keyPassword = new SerializableProtectedString(pwdEncoded.toCharArray());

        props.put(RsSamlConfigImpl.KEY_keyPassword, keyPassword);
        rsSamlConfigImpl = new RsSamlConfigImpl(cc, props, configAdminn, filterIdMap, parentSsoService);

        assertEquals("Expected " + PWD + " but received " + rsSamlConfigImpl.getKeyPassword() + ".", PWD, rsSamlConfigImpl.getKeyPassword());
    }

    @Test
    public void testGetAuthFilterId_NullConfiguration() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(configAdminn).getConfiguration(FILTER_SERVICE_REF, null);
                will(returnValue(null));
            }
        });

        String authFilter = rsSamlConfigImpl.getAuthFilterId(FILTER_SERVICE_REF);
        assertNull(authFilter);
    }

    @Test
    public void testGetAuthFilterId_NullProps() throws IOException {
        final Configuration config2 = mockery.mock(Configuration.class, "config2");
        mockery.checking(new Expectations() {
            {
                one(configAdminn).getConfiguration(FILTER_SERVICE_REF, null);
                will(returnValue(config2));

                one(config2).getProperties();
                will(returnValue(null));
            }
        });

        String authFilter = rsSamlConfigImpl.getAuthFilterId(FILTER_SERVICE_REF);
        assertNull(authFilter);
    }

    @Test
    public void testGetAuthFilterId_ThrowsException() throws IOException {
        final IOException ex = new IOException();
        mockery.checking(new Expectations() {
            {
                one(configAdminn).getConfiguration(FILTER_SERVICE_REF, null);
                will(throwException(ex));
            }
        });

        String authFilter = rsSamlConfigImpl.getAuthFilterId(FILTER_SERVICE_REF);
        assertNull(authFilter);
    }

    @Test
    public void testGetPkixTrustAnchors() throws IOException {
        ArrayList<X509Certificate> listCertificates = (ArrayList<X509Certificate>) rsSamlConfigImpl.getPkixTrustAnchors();
        assertTrue(listCertificates.size() == 0);
    }

    @Test
    public void testProcessPkixTrustEngineData() {
        stateMachine.become("runtime");

        mockery.checking(new Expectations() {
            {
                allowing(trustEngineProps).get(RsSamlConfigImpl.KEY_trustEngine_x509cert);
                will(returnValue(null));

                allowing(trustEngineProps).get(RsSamlConfigImpl.KEY_trustEngine_crl);
                will(returnValue(null));
            }
        });

        rsSamlConfigImpl = new RsSamlConfigImpl(cc, props, configAdminn, filterIdMap, parentSsoService);
        assertTrue(rsSamlConfigImpl.getPkixX509CertificateList().size() == 0);
        assertTrue(rsSamlConfigImpl.getPkixCrlList().size() == 0);
    }
}