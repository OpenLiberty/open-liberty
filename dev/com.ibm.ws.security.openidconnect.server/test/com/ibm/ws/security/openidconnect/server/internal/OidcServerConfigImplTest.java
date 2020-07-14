/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

import test.common.SharedOutputManager;

public class OidcServerConfigImplTest {

    private static final Object MY_OIDC_SERVER = "myOidcServer";

    private static final Object PROVIDER_REF = "OAuthConfigPublic";

    private static final Object UNIQUE_USER_IDENTIFIER = "username";

    private static final Object ISSUER_IDENTIFIER = "https://MyTestServer";

    private static final Object AUDIENCE = "audience";

    private static final Object USER_IDENTITY = "userIdentity";

    private static final Object GROUP_IDENTIFIER = "groupIds";

    private static final Object TEST_SCOPE = "authorization_code";

    private static final String HS256 = "HS256";
    private static final String RS256 = "RS256";
    private static final String OP_KEYSTORE = OidcServerConfigImpl.CFG_KEYSTORE_REF_DEFAULT;
    private static final String OP_TRUSTSTORE = "opTrustStore";
    private static final String OP_RSA_KEY_ALIAS = "opRSAPrivateKey";
    private static final long ID_TOKEN_LIFETIME = 7200;
    private static final String CHECK_SESSION_IFRAME_ENDPOINT_URL = "/oidc/sessionMgmt.jsp";

    private static String TEST_CERT_ALIAS = "alien";

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    protected final org.osgi.service.cm.Configuration config = mock.mock(org.osgi.service.cm.Configuration.class);

    private final ComponentContext cc = mock.mock(ComponentContext.class);

    @SuppressWarnings("unchecked")
    private final ServiceReference<ConfigurationAdmin> configAdminRef = mock.mock(ServiceReference.class, "configAdmin");
    private final ConfigurationAdmin configAdmin = mock.mock(ConfigurationAdmin.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<KeyStoreService> keyStoreServiceRef = mock.mock(ServiceReference.class, "keyStoreServiceRef");
    private final KeyStoreService keyStoreService = mock.mock(KeyStoreService.class);
    private final Certificate cert = mock.mock(Certificate.class);
    private final PublicKey publicKey = mock.mock(PublicKey.class);
    private final X509Certificate x509 = mock.mock(X509Certificate.class);
    private OidcServerConfigImpl oidcServerConfig = null;

    final Map<String, Object> interceptor = new Hashtable<String, Object>();

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService("configurationAdmin", configAdminRef);
                will(returnValue(configAdmin));
                allowing(cc).locateService("keyStoreService", keyStoreServiceRef);
                will(returnValue(keyStoreService));
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @Test
    public void testConstructor() {
        oidcServerConfig = new OidcServerConfigImpl();
        assertNotNull("There must be a OidcServerConfigImpl", oidcServerConfig);
    }

    @Test
    public void testActivate() throws Exception {
        final Map<String, Object> oidcProps = createOIDCTestProps();
        createExpectationsForConfigProcessing(createOAuth20Props());

        OidcServerConfigImpl oidcServerConfig = createActivatedOidcServerConfig(oidcProps);

        assertEquals("provider id should be " + MY_OIDC_SERVER, MY_OIDC_SERVER, oidcServerConfig.getProviderId());
        assertEquals("The OAuth20 provider name should be set.", PROVIDER_REF, oidcServerConfig.getOauthProviderName());
        assertEquals("The OAuth20 provider pid should be set.", PROVIDER_REF, oidcServerConfig.getOauthProviderPid());
        assertEquals("user identifier should be " + UNIQUE_USER_IDENTIFIER, UNIQUE_USER_IDENTIFIER, oidcServerConfig.getUserIdentifier());
        assertEquals("unique user identifier should be " + UNIQUE_USER_IDENTIFIER, UNIQUE_USER_IDENTIFIER, oidcServerConfig.getUniqueUserIdentifier());
        assertEquals("issuer identifier should be " + ISSUER_IDENTIFIER, ISSUER_IDENTIFIER, oidcServerConfig.getIssuerIdentifier());
        assertEquals("audience should be " + AUDIENCE, AUDIENCE, oidcServerConfig.getAudience());
        assertEquals("user identity should be " + USER_IDENTITY, USER_IDENTITY, oidcServerConfig.getUserIdentity());
        assertEquals("group identifier should be " + GROUP_IDENTIFIER, GROUP_IDENTIFIER, oidcServerConfig.getGroupIdentifier());
        assertEquals("The default scope should be set.", TEST_SCOPE, oidcServerConfig.getDefaultScope());
        assertNotNull("There must be a scope to claim map.", oidcServerConfig.getScopeToClaimMap());
        assertEquals("id token sign algorithm should be " + HS256, HS256, oidcServerConfig.getSignatureAlgorithm());
        assertTrue("custom claims should be enabled.", oidcServerConfig.isCustomClaimsEnabled());
        assertEquals("jwk rotation should be " + (10L * 60 * 1000), (10L * 60 * 1000), oidcServerConfig.getJwkRotationTime());
        assertEquals("jwk signing key size should be " + 2000L, 2000L, oidcServerConfig.getJwkSigningKeySize());
        assertFalse("jti claim should be disabled.", oidcServerConfig.isJTIClaimEnabled());
        assertNotNull("There must be a private key.", oidcServerConfig.getPrivateKey());
        assertNull("No publickey for HS256", oidcServerConfig.getPublicKey(TEST_CERT_ALIAS));
        assertFalse("sessionManaged should be false.", oidcServerConfig.isSessionManaged());
        assertEquals("id token lifetime should be " + ID_TOKEN_LIFETIME, ID_TOKEN_LIFETIME, oidcServerConfig.getIdTokenLifetime());
        assertEquals("There must be a check session iframe endpoint URL.", CHECK_SESSION_IFRAME_ENDPOINT_URL, oidcServerConfig.getCheckSessionIframeEndpointUrl());
        assertNotNull("claimToUserRegistryAttributeMappings object should not be null.", oidcServerConfig.getClaimToUserRegistryMap());
        assertTrue("Expected message was not logged",
                   outputMgr.checkForMessages("CWWKS1600I: The OpenID Connect provider " + MY_OIDC_SERVER + " configuration has been successfully processed."));
    }

    private OidcServerConfigImpl createActivatedOidcServerConfig(final Map<String, Object> props) {
        OidcServerConfigImpl oidcServerConfig = new OidcServerConfigImpl();
        oidcServerConfig.setConfigurationAdmin(configAdminRef);
        oidcServerConfig.setKeyStoreService(keyStoreServiceRef);
        oidcServerConfig.activate(cc, props);
        return oidcServerConfig;
    }

    private void createExpectationsForConfigProcessing(final Map<String, Object> props) throws IOException, KeyStoreException, CertificateException {
        final PrivateKey privateKey = mock.mock(PrivateKey.class);
        mock.checking(new Expectations() {
            {
                allowing(configAdmin).getConfiguration("OAuthConfigPublic", null);
                will(returnValue(config));
                allowing(config).getProperties();
                will(returnValue(props));

                allowing(keyStoreService).getPrivateKeyFromKeyStore(OP_KEYSTORE, OP_RSA_KEY_ALIAS, null);
                will(returnValue(privateKey));

                allowing(keyStoreService).getCertificateFromKeyStore(OP_TRUSTSTORE, TEST_CERT_ALIAS);
                will(returnValue(cert));

                allowing(keyStoreService).getKeyStoreLocation(OP_KEYSTORE);
                will(returnValue("mypath"));

                allowing(cert).getPublicKey();
                will(returnValue(publicKey));
            }
        });
    }

    @Test
    public void testActivateWithRSA() throws Exception {
        final Map<String, Object> oidcProps = createPropsWithRS256();
        createExpectationsForConfigProcessing(createOAuth20Props());

        OidcServerConfigImpl oidcServerConfig = createActivatedOidcServerConfig(oidcProps);

        assertEquals("provider id should be " + MY_OIDC_SERVER, MY_OIDC_SERVER, oidcServerConfig.getProviderId());
        assertEquals("user identitfier should be " + UNIQUE_USER_IDENTIFIER, UNIQUE_USER_IDENTIFIER, oidcServerConfig.getUserIdentifier());
        assertEquals("unique user identitfier should be " + UNIQUE_USER_IDENTIFIER, UNIQUE_USER_IDENTIFIER, oidcServerConfig.getUniqueUserIdentifier());
        assertEquals("issuer identifier should be " + ISSUER_IDENTIFIER, ISSUER_IDENTIFIER, oidcServerConfig.getIssuerIdentifier());
        assertEquals("audience should be " + AUDIENCE, AUDIENCE, oidcServerConfig.getAudience());
        assertEquals("user identity should be " + USER_IDENTITY, USER_IDENTITY, oidcServerConfig.getUserIdentity());
        assertEquals("group identifier should be " + GROUP_IDENTIFIER, GROUP_IDENTIFIER, oidcServerConfig.getGroupIdentifier());
        assertEquals("id token sign algorithm should be " + RS256, RS256, oidcServerConfig.getSignatureAlgorithm());
        assertTrue("custom claims should be enabled.", oidcServerConfig.isCustomClaimsEnabled());
        assertFalse("jti claim should be disabled.", oidcServerConfig.isJTIClaimEnabled());
        assertEquals("The trustStoreRef must be set.", OP_TRUSTSTORE, oidcServerConfig.getTrustStoreRef());
        assertNotNull("There must be a private key.", oidcServerConfig.getPrivateKey());
        assertNotNull("There must be a public key", oidcServerConfig.getPublicKey(TEST_CERT_ALIAS));
        assertFalse("sessionManaged should be false.", oidcServerConfig.isSessionManaged());
        assertEquals("id token lifetime should be " + ID_TOKEN_LIFETIME, ID_TOKEN_LIFETIME, oidcServerConfig.getIdTokenLifetime());
        assertNotNull("claimToUserRegistryAttributeMappings object should not be null.", oidcServerConfig.getClaimToUserRegistryMap());
        assertTrue("Expected message was not logged",
                   outputMgr.checkForMessages("CWWKS1600I: The OpenID Connect provider " + MY_OIDC_SERVER + " configuration has been successfully processed."));
    }

    @Test
    public void testModifyNullProps() {
        OidcServerConfigImpl oidcServerConfig = new OidcServerConfigImpl();
        oidcServerConfig.modify(null);
    }

    @Test
    public void testModifyEmptyProps() {
        OidcServerConfigImpl oidcServerConfig = new OidcServerConfigImpl();
        final Map<String, Object> props = new Hashtable<String, Object>();
        oidcServerConfig.modify(props);
    }

    @Test
    public void testDeactivate() throws Exception {
        final Map<String, Object> oidcProps = createOIDCTestProps();
        createExpectationsForConfigProcessing(createOAuth20Props());
        OidcServerConfigImpl oidcServerConfig = createActivatedOidcServerConfig(oidcProps);
        oidcServerConfig.unsetConfigurationAdmin(configAdminRef);
        oidcServerConfig.unsetKeyStoreService(keyStoreServiceRef);
        oidcServerConfig.deactivate(cc);
    }

    @Test
    public void testBuildJwk_JwkDisabled() throws KeyStoreException, CertificateException, IOException {
        final Map<String, Object> oidcProps = createPropsWithRS256();

        oidcProps.put(OidcServerConfigImpl.CFG_KEY_JWK_ENABLED, false);
        oidcProps.put(OidcServerConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, HS256); //default value and OP will not build any jwk with this setting
        createExpectationsForConfigProcessing(createOAuth20Props());

        OidcServerConfigImpl oidcServerConfig = createActivatedOidcServerConfig(oidcProps);

        assertFalse("Jwk is enabled, but it should be disabled.", oidcServerConfig.isJwkEnabled());
        assertNull("Expected to receive a null value but was received: " + oidcServerConfig.getJwkJsonString(), oidcServerConfig.getJwkJsonString());
        assertNull("Expected to receive a null value but was received: " + oidcServerConfig.getJSONWebKey(), oidcServerConfig.getJSONWebKey());
    }

    @Test
    public void testBuildJwk_JwkDisabled_X509() throws KeyStoreException, CertificateException, IOException {
        final Map<String, Object> oidcProps = createPropsWithRS256();

        oidcProps.put(OidcServerConfigImpl.CFG_KEY_JWK_ENABLED, false);
        createExpectationsForConfigProcessing(createOAuth20Props());
        mock.checking(new Expectations() {
            {
                allowing(keyStoreService).getX509CertificateFromKeyStore(OP_KEYSTORE, OP_RSA_KEY_ALIAS);
                will(returnValue(x509));

                allowing(x509).getPublicKey();
                will(returnValue(publicKey));
            }
        });
        OidcServerConfigImpl oidcServerConfig = createActivatedOidcServerConfig(oidcProps);

        assertFalse("Jwk is enabled, but it should be disabled.", oidcServerConfig.isJwkEnabled());
    }

    @Test
    public void testBuildJwk_JwkEnabled() throws KeyStoreException, CertificateException, IOException {
        final long KEY_SIZE = 4096l;
        final long ROTATION_TIME = 60l;
        final Map<String, Object> oidcProps = createPropsWithRS256();

        oidcProps.put(OidcServerConfigImpl.CFG_KEY_JWK_ENABLED, true);
        oidcProps.put(OidcServerConfigImpl.CFG_KEY_JWK_SIGNING_KEY_SIZE, KEY_SIZE);
        oidcProps.put(OidcServerConfigImpl.CFG_KEY_JWK_ROTATION, ROTATION_TIME);
        oidcProps.put(OidcServerConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, RS256);

        createExpectationsForConfigProcessing(createOAuth20Props());
        OidcServerConfigImpl oidcServerConfig = createActivatedOidcServerConfig(oidcProps);

        // check for correct values
        assertTrue("Jwk is disabled, but it should be enabled.", oidcServerConfig.isJwkEnabled());
        assertEquals("Invalid JWK signing key size, expected: " + KEY_SIZE + " but received: " + oidcServerConfig.getJwkSigningKeySize() + ".",
                     KEY_SIZE, oidcServerConfig.getJwkSigningKeySize());
        assertEquals("Invalid JWK rotation time, expected: " + ROTATION_TIME * 60 * 1000 + " but received: " + oidcServerConfig.getJwkRotationTime() + ".",
                     (ROTATION_TIME * 60 * 1000), oidcServerConfig.getJwkRotationTime());

        // get JsonWebKey
        JSONWebKey jwk = oidcServerConfig.getJSONWebKey();
        assertNotNull("Expected a good value for Jwk but received a null value.", jwk);
        String alg = "RS256";
        assertEquals("Invalid algorithm, expected: " + alg + " but received: " + jwk.getAlgorithm() + ".", alg, jwk.getAlgorithm());

        // get JsonWebKey as String
        String jwkString = oidcServerConfig.getJwkJsonString();
        assertNotNull("Expected a good value for Jwk but received a null value.", jwkString);
        assertTrue("String resulted does not contain the JWS algorithm " + RS256 + " for 'alg'. JsonWebKey: " + jwkString, jwkString.contains(RS256));
    }

    public Map<String, Object> createOIDCTestProps() {
        final Map<String, Object> props = new Hashtable<String, Object>();

        props.put(OidcServerConfigImpl.CFG_KEY_ID, MY_OIDC_SERVER);
        props.put(OidcServerConfigImpl.CFG_KEY_OAUTH_PROVIDER_REF, PROVIDER_REF);
        props.put(OidcServerConfigImpl.CFG_KEY_UNIQUE_USER_IDENTIFIER, UNIQUE_USER_IDENTIFIER);
        props.put(OidcServerConfigImpl.CFG_KEY_ISSUER_IDENTIFIER, ISSUER_IDENTIFIER);
        props.put(OidcServerConfigImpl.CFG_KEY_AUDIENCE, AUDIENCE);
        props.put(OidcServerConfigImpl.CFG_KEY_USER_IDENTITY, USER_IDENTITY);
        props.put(OidcServerConfigImpl.CFG_KEY_GROUP_IDENTIFIER, GROUP_IDENTIFIER);
        props.put(OidcServerConfigImpl.CFG_KEY_DEFAULT_SCOPE, TEST_SCOPE);
        //props.put(OidcServerConfigImpl.CFG_KEY_MAP_SCOPE_TO_CLAIMS_REF, CFG_KEY_MAP_SCOPE_TO_CLAIMS_REF);
        //props.put(OidcServerConfigImpl.CFG_KEY_CLAIM_TO_USER_REGISTRY_ATTRIBUTE_MAPPING_REF, CLAIM_TO_USER_REGISTRY_ATTRIBUTE_MAPPING_REF);
        props.put(OidcServerConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, HS256);
        props.put(OidcServerConfigImpl.CFG_KEY_CUSTOM_CLAIMS_ENABLED, true);
        props.put(OidcServerConfigImpl.CFG_KEY_JTI_CLAIM_ENABLED, false);
        props.put(OidcServerConfigImpl.CFG_KEY_SESSION_MANAGED, false);
        props.put(OidcServerConfigImpl.CFG_KEY_ID_TOKEN_LIFETIME, Long.valueOf(ID_TOKEN_LIFETIME));
        props.put(OidcServerConfigImpl.CFG_KEY_CHECK_SESSION_IFRAME_ENDPOINT_URL, CHECK_SESSION_IFRAME_ENDPOINT_URL);
        props.put(OidcServerConfigImpl.CFG_KEY_TRUSTSTORE_REF, OP_TRUSTSTORE);
        props.put(OidcServerConfigImpl.CFG_KEY_KEYSTORE_REF, OP_KEYSTORE);
        props.put(OidcServerConfigImpl.CFG_KEY_KEY_ALIAS_NAME, OP_RSA_KEY_ALIAS);
        props.put(OidcServerConfigImpl.CFG_KEY_PROTECTED_ENDPOINTS, "authorize registration");
        props.put(OidcServerConfigImpl.CFG_KEY_JWK_ENABLED, true);
        props.put(OidcServerConfigImpl.CFG_KEY_CACHE_IDTOKEN, true);
        props.put(OidcServerConfigImpl.CFG_KEY_JWK_ROTATION, 10L);
        props.put(OidcServerConfigImpl.CFG_KEY_JWK_SIGNING_KEY_SIZE, 2000L);
        return props;
    }

    public Map<String, Object> createOAuth20Props() {
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(OidcServerConfigImpl.CFG_KEY_ID, PROVIDER_REF);
        props.put(OidcServerConfigImpl.KEY_HTTPS_REQUIRED, true);
        return props;
    }

    public Map<String, Object> createPropsWithRS256() {
        final Map<String, Object> props = createOIDCTestProps();
        props.put(OidcServerConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, RS256);
        return props;
    }
}
