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

package com.ibm.ws.security.openidconnect.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
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
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class OidcClientConfigImplTest extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ComponentContext cc = mock.mock(ComponentContext.class);
    final String ID = "myOpenID";
    final String AUTHORIZATION_CODE = "authorization_code";
    final String SCOPE = "openid profile";
    final String CLIENT_ID = "clientId";
    final String CLIENT_SECRET = "clientSecret";
    final String REDIRECT_URL = "redirectUrl";
    final String GROUP_IDENTIFIER = "groupIdentifier";
    final String USER_NAME = "username";
    final String SHARED_KEY = "sharedKey";
    final String TRUST_ALIAS_NAME = "trustAliasName";
    final String MY_SSL_REF = "mySSLRef";
    private final String SSL_CONFIGURATION_NAME = "mySSLConfig";
    final String SHA256 = "HS256";//"SHA256";
    //final String DISCOVERY_ENDPOINT_URL = "authorizationEndpointUrl";
    final String AUTHORIZATION_ENDPOINT_URL = "authorizationEndpointUrl";
    final String TOKEN_ENDPOINT_URL = "tokenEndpointUrl";
    final String VALIDATION_ENDPOINT_URL = "validationEndpointUrl";
    final int MAX_STATE_CACHE_SIZE = 1000;
    final String AUTO_AUTHORIZE_PARAM = "autoAuthorizeParam";
    final String ACR_VALUES = "urn:mace:incommon:iap:silver urn:mace:incommon:iap:bronze";
    final String EMPTY_ACR_VALUES = null;
    final String REDIRECT_HOSTPORT_VALID_NOPORT = "http://localhost";
    final String REDIRECT_HOSTPORT_VALID = "http://localhost:8080";
    final String REDIRECT_HOSTPORT_INVALID = "httpsss://localhost:8080";
    final String REDIRECT_URL_VALID_NOPORT = REDIRECT_HOSTPORT_VALID_NOPORT + "/oidcclient/redirect/" + ID;
    final String REDIRECT_URL_VALID = REDIRECT_HOSTPORT_VALID + "/oidcclient/redirect/" + ID;

    final String ISSUER_IDENTIFIER = "https://localhost:8020/oidc/op";
    final String TRUST_KEYSTORE_REF = "rpkeystore";
    final boolean HOSTNAME_VERIFICATION = true;

    @SuppressWarnings("unchecked")
    private final ServiceReference<ConfigurationAdmin> configAdminRef = mock.mock(ServiceReference.class, "configAdmin");
    private final ConfigurationAdmin configAdmin = mock.mock(ConfigurationAdmin.class);
    private OidcClientConfigImpl oidcClientConfig = null;
    private final Configuration sslConfiguration = mock.mock(Configuration.class);
    private final Map<String, Object> sslConfigProps = new Hashtable<String, Object>();

    private final Configuration authzRequestParam = mock.mock(Configuration.class, "authzRequestParam");
    private final static Map<String, Object> authzRequestParamProps = new Hashtable<String, Object>();
    static {
        authzRequestParamProps.put("name", "name1");
        authzRequestParamProps.put("value", "value1");
    }

    private final Configuration authzRequestParam2 = mock.mock(Configuration.class, "authzRequestParam2");
    private final static Map<String, Object> authzRequestParamProps2 = new Hashtable<String, Object>();

    final Map<String, Object> interceptor = new Hashtable<String, Object>();

    static final String authFilterId = "myAuthFilterId";
    static final boolean createsession = true;
    static final Hashtable<String, Object> adminProps = new Hashtable<String, Object>();
    static {
        adminProps.put("id", authFilterId);
    }
    final Configuration config = mock.mock(Configuration.class, "config");
    final String discoveryjsonString = "{\"introspection_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/introspect\",\"coverage_map_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/coverage_map\",\"issuer\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample\",\"authorization_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/authorize\",\"token_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/token\",\"jwks_uri\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/jwk\",\"response_types_supported\":[\"code\",\"token\",\"id_token token\"],\"subject_types_supported\":[\"public\"],\"id_token_signing_alg_values_supported\":[\"ES256\",\"RS256\"],\"userinfo_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/userinfo\",\"registration_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/registration\",\"scopes_supported\":[\"openid\",\"general\",\"profile\",\"email\",\"address\",\"phone\"],\"claims_supported\":[\"sub\",\"groupIds\",\"name\",\"preferred_username\",\"picture\",\"locale\",\"email\",\"profile\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\"],\"grant_types_supported\":[\"authorization_code\",\"implicit\",\"refresh_token\",\"client_credentials\",\"password\",\"urn:ietf:params:oauth:grant-type:jwt-bearer\"],\"token_endpoint_auth_methods_supported\":[\"client_secret_basic\"],\"display_values_supported\":[\"page\"],\"claim_types_supported\":[\"normal\"],\"claims_parameter_supported\":false,\"request_parameter_supported\":false,\"request_uri_parameter_supported\":false,\"require_request_uri_registration\":false,\"check_session_iframe\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/check_session_iframe\",\"end_session_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/end_session\"}";
    final String discoveryjsonString_2 = "{\"introspection_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/introspect\",\"coverage_map_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/coverage_map\",\"issuer\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample\",\"authorization_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/authorize\",\"token_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/token\",\"jwks_uri\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/jwk\",\"response_types_supported\":[\"code\",\"token\",\"id_token token\"],\"subject_types_supported\":[\"public\"],\"id_token_signing_alg_values_supported\":[\"ES256\"],\"userinfo_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/userinfo\",\"registration_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/registration\",\"scopes_supported\":[\"openid\",\"general\",\"email\",\"address\",\"phone\"],\"claims_supported\":[\"sub\",\"groupIds\",\"name\",\"preferred_username\",\"picture\",\"locale\",\"email\",\"profile\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\"],\"grant_types_supported\":[\"authorization_code\",\"implicit\",\"refresh_token\",\"client_credentials\",\"password\",\"urn:ietf:params:oauth:grant-type:jwt-bearer\"],\"token_endpoint_auth_methods_supported\":[\"client_secret_post\"],\"display_values_supported\":[\"page\"],\"claim_types_supported\":[\"normal\"],\"claims_parameter_supported\":false,\"request_parameter_supported\":false,\"request_uri_parameter_supported\":false,\"require_request_uri_registration\":false,\"check_session_iframe\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/check_session_iframe\",\"end_session_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/end_session\"}";
    final String discoveryjsonString_3 = "{\"introspection_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/introspect\",\"coverage_map_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/coverage_map\",\"issuer\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample\",\"authorization_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/authorize\",\"token_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/token\",\"jwks_uri\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/jwk\",\"response_types_supported\":[\"code\",\"token\",\"id_token token\"],\"subject_types_supported\":[\"public\"],\"id_token_signing_alg_values_supported\":[\"ES256\"],\"userinfo_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/userinfo\",\"registration_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/registration\",\"scopes_supported\":[\"profile\",\"general\",\"email\",\"address\",\"phone\"],\"claims_supported\":[\"sub\",\"groupIds\",\"name\",\"preferred_username\",\"picture\",\"locale\",\"email\",\"profile\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\"],\"grant_types_supported\":[\"authorization_code\",\"implicit\",\"refresh_token\",\"client_credentials\",\"password\",\"urn:ietf:params:oauth:grant-type:jwt-bearer\"],\"token_endpoint_auth_methods_supported\":[\"client_secret_somethingelse\"],\"display_values_supported\":[\"page\"],\"claim_types_supported\":[\"normal\"],\"claims_parameter_supported\":false,\"request_parameter_supported\":false,\"request_uri_parameter_supported\":false,\"require_request_uri_registration\":false,\"check_session_iframe\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/check_session_iframe\",\"end_session_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/end_session\"}";
    final String discoveryjsonString_4 = "{\"introspection_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/introspect\",\"coverage_map_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/coverage_map\",\"issuer\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample\",\"authorization_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/authorize\",\"token_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/token\",\"jwks_uri\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/jwk\",\"response_types_supported\":[\"code\",\"token\",\"id_token token\"],\"subject_types_supported\":[\"public\"],\"id_token_signing_alg_values_supported\":[\"ES256\"],\"userinfo_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/userinfo\",\"registration_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/registration\",\"scopes_supported\":[\"general\",\"email\",\"address\",\"phone\"],\"claims_supported\":[\"sub\",\"groupIds\",\"name\",\"preferred_username\",\"picture\",\"locale\",\"email\",\"profile\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\"],\"grant_types_supported\":[\"authorization_code\",\"implicit\",\"refresh_token\",\"client_credentials\",\"password\",\"urn:ietf:params:oauth:grant-type:jwt-bearer\"],\"token_endpoint_auth_methods_supported\":[\"client_secret_post\",\"client_secret_basic\"],\"display_values_supported\":[\"page\"],\"claim_types_supported\":[\"normal\"],\"claims_parameter_supported\":false,\"request_parameter_supported\":false,\"request_uri_parameter_supported\":false,\"require_request_uri_registration\":false,\"check_session_iframe\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/check_session_iframe\",\"end_session_endpoint\":\"http://localhost:8940/oidc/endpoint/OidcConfigSample/end_session\"}";

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        try {
            createComponentContextExpectations();
            oidcClientConfig = new OidcClientConfigImpl();
            oidcClientConfig.setConfigurationAdmin(configAdminRef);
            createSSLExpectations();
            final Map<String, Object> props = createProps(true);
            oidcClientConfig.activate(cc, props);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }

    }

    @After
    public void afterTest() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mock.assertIsSatisfied();
    }

    private void createComponentContextExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService("configurationAdmin", configAdminRef);
                will(returnValue(configAdmin));

                allowing(cc).getBundleContext();

            }
        });
    }

    private void createSSLExpectations() throws IOException {
        sslConfigProps.put("id", MY_SSL_REF);
        authzRequestParamProps.put("id", "authzParameter1");
        mock.checking(new Expectations() {
            {
                one(configAdmin).getConfiguration(authFilterId, null);
                will(returnValue(config));
                one(config).getProperties();
                will(returnValue(adminProps));
                allowing(configAdmin).getConfiguration(MY_SSL_REF, "");
                will(returnValue(sslConfiguration));
                allowing(sslConfiguration).getProperties();
                will(returnValue(sslConfigProps));
                allowing(configAdmin).getConfiguration("authzParameter1", "");
                will(returnValue(authzRequestParam));
                allowing(authzRequestParam).getProperties();
                will(returnValue(authzRequestParamProps));
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @Test
    public void testActivate() throws Exception {
        try {
            assertEquals("ID should be " + ID, ID, oidcClientConfig.getId());
            assertEquals("Grant type should be " + AUTHORIZATION_CODE, AUTHORIZATION_CODE, oidcClientConfig.getGrantType());
            assertEquals("Cient id should be " + CLIENT_ID, CLIENT_ID, oidcClientConfig.getClientId());
            assertEquals("Client secret should be " + CLIENT_SECRET, CLIENT_SECRET, oidcClientConfig.getClientSecret());
            assertEquals("Group identifier should be " + GROUP_IDENTIFIER, GROUP_IDENTIFIER, oidcClientConfig.getGroupIdentifier());
            assertEquals("User identity to identifier to create a subject should be " + USER_NAME, USER_NAME, oidcClientConfig.getUserIdentityToCreateSubject());
            assertEquals("Scope should be " + SCOPE, SCOPE, oidcClientConfig.getScope());
            assertTrue("Map identity to registry user should be true", oidcClientConfig.isMapIdentityToRegistryUser());
            assertTrue("Validate access token locally should be true", oidcClientConfig.isValidateAccessTokenLocally());
            assertEquals("Shared key should be " + SHARED_KEY, SHARED_KEY, oidcClientConfig.getSharedKey());
            assertEquals("Key alias name should be " + TRUST_ALIAS_NAME, TRUST_ALIAS_NAME, oidcClientConfig.getTrustAliasName());
            assertTrue("Https required should be true", oidcClientConfig.isHttpsRequired());
            assertEquals("SSL ref should be " + MY_SSL_REF, MY_SSL_REF, oidcClientConfig.getSslRef());
            assertEquals("SSL configuration name should be " + MY_SSL_REF, MY_SSL_REF, oidcClientConfig.getSSLConfigurationName());
            assertEquals("Signature algorithm should be " + SHA256, SHA256, oidcClientConfig.getSignatureAlgorithm());
            assertEquals("Shared key should be " + SHARED_KEY, SHARED_KEY, oidcClientConfig.getSharedKey());
            assertEquals("Clock skew should be 300", 300, oidcClientConfig.getClockSkewInSeconds());
            assertEquals("Authorization end point URL should be " + AUTHORIZATION_ENDPOINT_URL, AUTHORIZATION_ENDPOINT_URL, oidcClientConfig.getAuthorizationEndpointUrl());
            assertEquals("Token end point URL should be " + TOKEN_ENDPOINT_URL, TOKEN_ENDPOINT_URL, oidcClientConfig.getTokenEndpointUrl());
            assertEquals("Access token validation end point URL should be " + AUTHORIZATION_ENDPOINT_URL, AUTHORIZATION_ENDPOINT_URL,
                    oidcClientConfig.getAuthorizationEndpointUrl());
            assertEquals("Authorization end point URL should be " + VALIDATION_ENDPOINT_URL, VALIDATION_ENDPOINT_URL,
                    oidcClientConfig.getValidationEndpointUrl());
            assertEquals("Max state cache size should be " + MAX_STATE_CACHE_SIZE, MAX_STATE_CACHE_SIZE, oidcClientConfig.getInitialStateCacheCapacity());
            assertEquals("Keystore reference should be " + TRUST_KEYSTORE_REF, TRUST_KEYSTORE_REF, oidcClientConfig.getTrustStoreRef());
            assertEquals("auth filter id is supposed to be " + authFilterId, authFilterId, oidcClientConfig.getAuthFilterId());
            assertTrue("Hostname Verification enabled should be true", oidcClientConfig.isHostNameVerificationEnabled());
            assertTrue("disableIssChecking is set to true", oidcClientConfig.disableIssChecking());
            //assertEquals("Auto authorize parameters should be " + AUTO_AUTHORIZE_PARAM, AUTO_AUTHORIZE_PARAM, oidcClientConfig.getAutoAuthorizeParam());
            assertTrue("Expected message was not logged",
                    outputMgr.checkForMessages("CWWKS1700I: OpenID Connect client " + ID + " configuration successfully processed."));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testModify() throws Exception {
        try {
            final Map<String, Object> props = createProps(false);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(adminProps));
                }
            });
            oidcClientConfig.modify(props);
            assertFalse("Map identity to registry user should be false", oidcClientConfig.isMapIdentityToRegistryUser());
            assertFalse("disableIssChecking should be false now", oidcClientConfig.disableIssChecking());
            assertFalse("Validate access token locally should be false", oidcClientConfig.isValidateAccessTokenLocally());
            assertEquals("auth filter id is supposed to be " + authFilterId, authFilterId, oidcClientConfig.getAuthFilterId());
            assertTrue("Expected message was not logged",
                    outputMgr.checkForMessages("CWWKS1701I: OpenID Connect client " + oidcClientConfig.getId() + " configuration change successfully processed."));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetAuthzRequestParams() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);
            String[] authzParameters = { "authzParameter1", "authzParameter2" };
            props.put(OidcClientConfigImpl.CFG_KEY_AUTHZ_PARAM, authzParameters);

            authzRequestParamProps.put("name", "name1");
            authzRequestParamProps.put("value", "value1");

            authzRequestParamProps2.put("name", "name2");
            authzRequestParamProps2.put("value", "value2");

            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(adminProps));
                    allowing(configAdmin).getConfiguration("authzParameter1", "");
                    will(returnValue(authzRequestParam));
                    allowing(configAdmin).getConfiguration("authzParameter2", "");
                    will(returnValue(authzRequestParam2));
                    allowing(authzRequestParam2).getProperties();
                    will(returnValue(authzRequestParamProps2));
                }
            });
            oidcClientConfig.modify(props);

            HashMap authzParamMap = oidcClientConfig.getAuthzRequestParams();

            assertEquals("Authz param, name1 value should be  " + "value1", "value1", authzParamMap.get("name1"));
            assertEquals("Authz param, name2 value should be  " + "value2", "value2", authzParamMap.get("name2"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has HS256 (default), supports HS256, RS256 and NONE and op supports RS256 and ES256, adjust should set algorithm to RS256 which is supported by both in this case
    @Test
    public void testAdjustSignatureAlgorithm() throws Exception {
        try {

            oidcClientConfig.parseJsonResponse(discoveryjsonString);
            oidcClientConfig.adjustSignatureAlgorithm();

            assertEquals("Signature Algorithm should be  " + "RS256", "RS256", oidcClientConfig.getSignatureAlgorithm());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has NONE (not default), supports HS256, RS256 and NONE and op supports RS256 and ES256, adjust should not change algorithm in this case
    @Test
    public void testNoAdjustSignatureAlgorithm() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, "NONE");
            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString);
            oidcClientConfig2.adjustSignatureAlgorithm();

            assertEquals("Signature Algorithm should be  " + "NONE", "NONE", oidcClientConfig2.getSignatureAlgorithm());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has HS256 (default), supports HS256, RS256 and NONE and op supports ES256 only, adjust should not set algorithm to ES256 since rp cannot support this
    @Test
    public void testNoAdjustSignatureAlgorithm_es() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, "HS256");
            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString_2);
            oidcClientConfig2.adjustSignatureAlgorithm();

            assertEquals("Signature Algorithm should be  " + "HS256", "HS256", oidcClientConfig2.getSignatureAlgorithm());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has openid profile (default), supports any scope and op supports "openid","general","profile","email","address","phone", adjust should not alter scope since op supports rp default
    @Test
    public void testNoAdjustScopes() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);

            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString);
            oidcClientConfig2.adjustScopes();

            assertEquals("Scope should be  " + "openid profile", "openid profile", oidcClientConfig2.getScope());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has openid profile (default), supports any scope and op supports "openid", "general","email","address","phone", adjust should alter scope to just have openid
    @Test
    public void testAdjustScopesOpenid() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);

            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString_2);
            oidcClientConfig2.adjustScopes();

            assertEquals("Scope should be  " + "openid", "openid", oidcClientConfig2.getScope());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has openid profile (default), supports any scope and op supports "profile", "general","email","address","phone", adjust should alter scope to just have profile
    @Test
    public void testAdjustScopesProfile() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);

            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString_3);
            oidcClientConfig2.adjustScopes();

            assertEquals("Scope should be  " + "profile", "profile", oidcClientConfig2.getScope());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has openid profile (default), supports any scope and op supports "general","email","address","phone", adjust should not alter scope since op does not support rp
    @Test
    public void testNoAdjustScopesOpRpMismatch() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);

            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString_4);
            oidcClientConfig2.adjustScopes();

            assertEquals("Scope should be  " + "openid profile", "openid profile", oidcClientConfig2.getScope());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has something_else (not default), supports any scope and op supports "openid","general","profile","email","address","phone", adjust should not alter scope in this case since rp is not using the default
    @Test
    public void testNoAdjustScopesRpNotusingDefault() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_SCOPE, "something_else");
            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString);
            oidcClientConfig2.adjustScopes();

            assertEquals("Scope should be  " + "something_else", "something_else", oidcClientConfig2.getScope());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has openid (not default), supports any scope and op supports "general","profile","email","address","phone", adjust should not alter scope in this case since rp is not using the default
    @Test
    public void testNoAdjustScopesRpNotusingProfile() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_SCOPE, "openid");
            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString_3);
            oidcClientConfig2.adjustScopes();

            assertEquals("Scope should be  " + "openid", "openid", oidcClientConfig2.getScope());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has profile (not default), supports any scope and op supports "openid", "general","email","address","phone", adjust should not alter scope since rp is not using the default
    @Test
    public void testNoAdjustScopesRpNotusingOpenid() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_SCOPE, "profile");
            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString_2);
            oidcClientConfig2.adjustScopes();

            assertEquals("Scope should be  " + "profile", "profile", oidcClientConfig2.getScope());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has post (default), supports post and basic and op supports "basic", adjust should alter and change it to basic
    @Test
    public void testAdjustTokenepAuthMethod() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_TOKEN_ENDPOINT_AUTH_METHOD, "post");
            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString);
            oidcClientConfig2.adjustTokenEndpointAuthMethod();

            assertEquals("Scope should be  " + "basic", "basic", oidcClientConfig2.getTokenEndpointAuthMethod());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has basic (not default), supports post and basic and op supports "post", adjust should not alter
    @Test
    public void testNoAdjustTokenepAuthMethod() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_TOKEN_ENDPOINT_AUTH_METHOD, "basic");
            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString_2);
            oidcClientConfig2.adjustTokenEndpointAuthMethod();

            assertEquals("Scope should be  " + "basic", "basic", oidcClientConfig2.getTokenEndpointAuthMethod());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // rp has post (default), supports post and basic and op supports "somethingelse", adjust should not alter
    @Test
    public void testNoAdjustTokenepAuthMethodMismatch() throws Exception {
        try {

            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_TOKEN_ENDPOINT_AUTH_METHOD, "post");
            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            oidcClientConfig2.parseJsonResponse(discoveryjsonString_3);
            oidcClientConfig2.adjustTokenEndpointAuthMethod();

            assertEquals("Scope should be  " + "post", "post", oidcClientConfig2.getTokenEndpointAuthMethod());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testAdjustTokenepAuthMethod_discoveryInfoMissingAuthMethod() throws Exception {
        try {
            String defaultAuthMethod = "post";
            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_TOKEN_ENDPOINT_AUTH_METHOD, defaultAuthMethod);
            OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
            oidcClientConfig2.modify(props);

            String discoveryInfo = "{}";
            oidcClientConfig2.parseJsonResponse(discoveryInfo);
            oidcClientConfig2.adjustTokenEndpointAuthMethod();

            assertEquals("Auth method should have defaulted to " + defaultAuthMethod + " but did not.", defaultAuthMethod, oidcClientConfig2.getTokenEndpointAuthMethod());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testJunctionPath() throws Exception {
        final Map<String, Object> props = createProps(true);
        props.put(OidcClientConfigImpl.CFG_KEY_JUNCTION_PATH, "somewhere_else");
        OidcClientConfigImpl oidcClientConfig2 = new OidcClientConfigImpl();
        oidcClientConfig2.modify(props);
        String result = oidcClientConfig2.getRedirectUrlWithJunctionPath("http://foo:12345/redirect_me");
        String expected = "http://foo:12345/somewhere_else/redirect_me";
        assertTrue("redirect with junction path: " + result + " did not match expected: " + expected,
                result.compareTo(expected) == 0);
    }

    @Test
    public void testSharedKey_null() throws Exception {
        try {
            final Map<String, Object> props = createProps(false);
            props.remove(OidcClientConfigImpl.CFG_KEY_SHARED_KEY);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(adminProps));
                }
            });
            oidcClientConfig.modify(props);

            assertEquals("Client secret should be " + CLIENT_SECRET, CLIENT_SECRET, oidcClientConfig.getClientSecret());
            assertEquals("Shared key should be " + CLIENT_SECRET, CLIENT_SECRET, oidcClientConfig.getSharedKey());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testClientSecret_encode() throws Exception {
        try {
            final Map<String, Object> props = createProps(false);
            props.remove(OidcClientConfigImpl.CFG_KEY_CLIENT_SECRET);
            String secretEncode = PasswordUtil.encode(CLIENT_SECRET);
            props.put(OidcClientConfigImpl.CFG_KEY_CLIENT_SECRET, secretEncode);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(adminProps));
                }
            });
            oidcClientConfig.modify(props);

            assertEquals("Client secret should be " + CLIENT_SECRET, CLIENT_SECRET, oidcClientConfig.getClientSecret());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testNoProps() throws Exception {
        try {
            oidcClientConfig.modify(null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetIssuerIdentifier_valid() throws Exception {
        try {
            final Map<String, Object> props = createProps(true);
            props.put(OidcClientConfigImpl.CFG_KEY_ISSUER_IDENTIFIER, ISSUER_IDENTIFIER);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(adminProps));
                }
            });
            oidcClientConfig.modify(props);

            assertEquals("ISSUER URL should be " + ISSUER_IDENTIFIER, ISSUER_IDENTIFIER, oidcClientConfig.getIssuerIdentifier());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetRedirectUrlFromServerToClient_valid() throws Exception {
        try {
            final Map<String, Object> props = createProps(false);
            props.put(OidcClientConfigImpl.CFG_KEY_REDIRECT_TO_RP_HOST_AND_PORT, REDIRECT_HOSTPORT_VALID);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(adminProps));
                }
            });
            oidcClientConfig.modify(props);

            assertEquals("Redirect URL should be " + REDIRECT_URL_VALID, REDIRECT_URL_VALID, oidcClientConfig.getRedirectUrlFromServerToClient());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetRedirectUrlFromServerToClient_validNoPort() throws Exception {
        try {
            final Map<String, Object> props = createProps(false);
            props.put(OidcClientConfigImpl.CFG_KEY_REDIRECT_TO_RP_HOST_AND_PORT, REDIRECT_HOSTPORT_VALID_NOPORT);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(adminProps));
                }
            });
            oidcClientConfig.modify(props);

            assertEquals("Redirect URL should be " + REDIRECT_URL_VALID_NOPORT, REDIRECT_URL_VALID_NOPORT, oidcClientConfig.getRedirectUrlFromServerToClient());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetRedirectUrlFromServerToClient_emptyHostPort() throws Exception {
        try {
            final Map<String, Object> props = createProps(false);
            props.put(OidcClientConfigImpl.CFG_KEY_REDIRECT_TO_RP_HOST_AND_PORT, "");
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(null));
                }
            });
            oidcClientConfig.modify(props);

            assertEquals("Redirect URL should be null", null, oidcClientConfig.getRedirectUrlFromServerToClient());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetRedirectUrlFromServerToClient_invalidHostPort() throws Exception {
        try {
            final Map<String, Object> props = createProps(false);
            props.put(OidcClientConfigImpl.CFG_KEY_REDIRECT_TO_RP_HOST_AND_PORT, REDIRECT_HOSTPORT_INVALID);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(adminProps));
                }
            });
            oidcClientConfig.modify(props);

            assertEquals("Redirect URL should be null", null, oidcClientConfig.getRedirectUrlFromServerToClient());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetAuthContextClassReference() {
        try {
            final Map<String, Object> props = createProps(false);
            //props.put(OidcClientConfigImpl.CFG_KEY_REDIRECT_TO_RP_HOST_AND_PORT, REDIRECT_HOSTPORT_INVALID);
            //oidcClientConfig.modify(props);
            String s = "urn:mace:incommon:iap:silver urn:mace:incommon:iap:bronze";//Arrays.asList(ACR_VALUES).toString();

            assertEquals("ACR values should not be null", s, oidcClientConfig.getAuthContextClassReference());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetAuthContextClassReference_EmptyACR() {
        try {
            final Map<String, Object> props = createProps(false);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(null));
                }
            });
            //props.put(OidcClientConfigImpl.CFG_KEY_AUTH_CONTEXT_CLASS_REFERENCE, EMPTY_ACR_VALUES);
            oidcClientConfig.modify(props);
            String s = ACR_VALUES;

            assertEquals("ACR values should not be null", s, oidcClientConfig.getAuthContextClassReference());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetUseSystemProps() {
        try {
            final Map<String, Object> props = createProps(false);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(null));
                }
            });
            props.put(OidcClientConfigImpl.CFG_KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS, new Boolean(true));

            // test default
            assertFalse("getUseSystemPropertiesForHttpClientConnections should be false", oidcClientConfig.getUseSystemPropertiesForHttpClientConnections());
            oidcClientConfig.modify(props);
            // test non-default
            assertTrue("getUseSystemPropertiesForHttpClientConnections should be true", oidcClientConfig.getUseSystemPropertiesForHttpClientConnections());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetAuthContextClassReference_NullACR() {
        try {
            final Map<String, Object> props = createProps(false);
            props.remove(OidcClientConfigImpl.CFG_KEY_AUTH_CONTEXT_CLASS_REFERENCE);
            mock.checking(new Expectations() {
                {
                    one(configAdmin).getConfiguration(authFilterId, null);
                    will(returnValue(config));
                    one(config).getProperties();
                    will(returnValue(adminProps));
                }
            });
            oidcClientConfig.modify(props);

            assertEquals("ACR values should be empty", "", oidcClientConfig.getAuthContextClassReference());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    public Map<String, Object> createProps(boolean value) {
        final Map<String, Object> props = new Hashtable<String, Object>();

        props.put(OidcClientConfigImpl.CFG_KEY_ID, ID);
        props.put(OidcClientConfigImpl.CFG_KEY_GRANT_TYPE, AUTHORIZATION_CODE);
        props.put(OidcClientConfigImpl.CFG_KEY_SCOPE, SCOPE);
        props.put(OidcClientConfigImpl.CFG_KEY_CLIENT_ID, CLIENT_ID);
        props.put(OidcClientConfigImpl.CFG_KEY_CLIENT_SECRET, CLIENT_SECRET);
        props.put(OidcClientConfigImpl.CFG_KEY_GROUP_IDENTIFIER, GROUP_IDENTIFIER);
        props.put(OidcClientConfigImpl.CFG_KEY_USER_IDENTITY_TO_CREATE_SUBJECT, USER_NAME);
        props.put(OidcClientConfigImpl.CFG_KEY_MAP_IDENTITY_TO_REGISTRY_USER, value);
        props.put(OidcClientConfigImpl.CFG_KEY_VALIDATE_ACCESS_TOKEN_LOCALLY, value);
        props.put(OidcClientConfigImpl.CFG_KEY_SHARED_KEY, SHARED_KEY);
        props.put(OidcClientConfigImpl.CFG_KEY_TRUST_ALIAS_NAME, TRUST_ALIAS_NAME);
        props.put(OidcClientConfigImpl.CFG_KEY_HTTPS_REQUIRED, value);
        props.put(OidcClientConfigImpl.CFG_KEY_CLIENTSIDE_REDIRECT, value);
        props.put(OidcClientConfigImpl.CFG_KEY_NONCE_ENABLED, false); // default is false
        props.put(OidcClientConfigImpl.CFG_KEY_SSL_REF, MY_SSL_REF);
        props.put(OidcClientConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, SHA256);
        props.put(OidcClientConfigImpl.CFG_KEY_CLOCK_SKEW, 300000L);
        //props.put(OidcClientConfigImpl.CFG_KEY_DISCOVERY_ENDPOINT_URL, DISCOVERY_ENDPOINT_URL);
        props.put(OidcClientConfigImpl.CFG_KEY_DISCOVERY_POLLING_RATE, 400000L);
        props.put(OidcClientConfigImpl.CFG_KEY_AUTHORIZATION_ENDPOINT_URL, AUTHORIZATION_ENDPOINT_URL);
        props.put(OidcClientConfigImpl.CFG_KEY_TOKEN_ENDPOINT_URL, TOKEN_ENDPOINT_URL);
        props.put(OidcClientConfigImpl.CFG_KEY_VALIDATION_ENDPOINT_URL, VALIDATION_ENDPOINT_URL);
        props.put(OidcClientConfigImpl.CFG_KEY_INITIAL_STATE_CACHE_CAPACITY, MAX_STATE_CACHE_SIZE);
        props.put(OidcClientConfigImpl.CFG_KEY_ISSUER_IDENTIFIER, ISSUER_IDENTIFIER);
        props.put(OidcClientConfigImpl.CFG_KEY_TRUSTSTORE_REF, TRUST_KEYSTORE_REF);
        props.put(OidcClientConfigImpl.CFG_KEY_AUTO_AUTHORIZE_PARAM, AUTO_AUTHORIZE_PARAM);
        props.put(OidcClientConfigImpl.CFG_KEY_HOST_NAME_VERIFICATION_ENABLED, value);
        props.put(OidcClientConfigImpl.CFG_KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT, value);
        props.put(OidcClientConfigImpl.CFG_KEY_INCLUDE_ID_TOKEN_IN_SUBJECT, value);
        props.put(OidcClientConfigImpl.CFG_KEY_AUTH_CONTEXT_CLASS_REFERENCE, ACR_VALUES);
        props.put(OidcClientConfigImpl.CFG_KEY_AUTH_FILTER_REF, authFilterId);
        props.put(OidcClientConfigImpl.CFG_KEY_CREATE_SESSION, value);
        props.put(OidcClientConfigImpl.CFG_KEY_disableLtpaCookie, false);
        props.put(OidcClientConfigImpl.CFG_KEY_propagation_authnSessionDisabled, false);
        props.put(OidcClientConfigImpl.CFG_KEY_reAuthnOnAccessTokenExpire, false);
        props.put(OidcClientConfigImpl.CFG_KEY_reAuthnCushionMilliseconds, 300000l);
        props.put(OidcClientConfigImpl.CFG_KEY_DISABLE_ISS_CHECKING, value);
        props.put(OidcClientConfigImpl.CFG_KEY_OidcclientRequestParameterSupported, true);
        props.put(OidcClientConfigImpl.CFG_KEY_AUTHENTICATION_TIME_LIMIT, 420000L);
        props.put(OidcClientConfigImpl.CFG_KEY_accessTokenInLtpaCookie, value);
        props.put(OidcClientConfigImpl.CFG_KEY_USERINFO_ENDPOINT_ENABLED, true);
        return props;
    }

}
