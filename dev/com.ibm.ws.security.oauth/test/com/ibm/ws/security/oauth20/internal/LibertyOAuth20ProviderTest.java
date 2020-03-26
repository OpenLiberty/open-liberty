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
package com.ibm.ws.security.oauth20.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonArray;
import org.osgi.service.cm.Configuration;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientProvider;
import com.ibm.ws.security.oauth20.plugins.db.CachedDBOidcClientProvider;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;

import test.common.SharedOutputManager;

public class LibertyOAuth20ProviderTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ComponentContext cc = mockery.mock(ComponentContext.class);
    private final ConfigurationAdmin configAdmin = mockery.mock(ConfigurationAdmin.class);
    private final Configuration config = mockery.mock(Configuration.class);
    private final ServiceReference factoryRef = mockery.mock(ServiceReference.class);

    private static Map<String, Object> properties;
    private static final Dictionary<String, Object> jwtGrantTypeProps = new Hashtable<String, Object>();
    private static final long authorizationGrantLifetime = 604800;
    private static final long authorizationCodeLifetime = 60;
    private static final long authorizationCodeLength = 30;
    private static final long accessTokenLifetime = 3600;
    private static final long accessTokenLength = 40;
    private static final boolean issueRefreshToken = false;
    private static final long refreshTokenLength = 50;
    private static final boolean allowPublicClients = false;
    private static final String[] grantType = { "authorization_code", "implicit", "refresh_token", "client_credentials", "password" };
    private static final String authorizationFormTemplate = "template.html";
    private static final String authorizationErrorTemplate = "";
    private static final String customLoginURL = "login.jsp";
    private static final String autoAuthorizeParam = "autoauthz";
    private static final boolean autoAuthorize = false;
    private static final boolean oauthOnly = true;
    private static final boolean includeTokenInSubject = true;
    private static final long consentCacheEntryLifetime = 1800;
    private static final long consentCacheSize = 1000;
    private static final boolean httpsRequired = true;
    private static final boolean certAuthentication = false;
    private static final boolean skipUserValidation = false;
    private static final String defaultJDBCSchema = "OAuthDBSchema";
    private static final long appPasswordLifetime = 1000 * 60 * 60 * 24 * 90;
    private static final long appTokenLifetime = 1000 * 60 * 60 * 24 * 90;
    private static final long appTokenOrPasswordLimit = 100;
    // Discovery attributes?
    private static final String[] responseTypesSupported = { "code", "id_token token", "token" };
    private static final String[] subjectTypesSupported = { "public" };
    private static final String idTokenSigningAlgValuesSupported = "HS256";
    private static final String[] scopesSupported = { "openid", "profile", "email", "address", "phone", "general" };
    private static final String[] claimsSupported = { "sub", "name", "preferred_username", "profile", "picture", "email", "locale", "groupIds" };
    private static final String[] responseModesSupported = { "query", "fragment" };
    private static final String[] grantTypesSupported = { "authorization_code", "implicit", "refresh_token", "client_credentials", "password",
            "urn:ietf:params:oauth:grant-type:jwt-bearer" };
    private static final String[] tokenEndpointAuthMethodsSupported = { "client_secret_post", "client_secret_basic" };
    private static final String[] displayValuesSupported = { "page" };
    private static final String[] claimTypesSupported = { "normal" };
    private static final boolean claimsParameterSupported = false;
    private static final boolean requestParameterSupported = false;
    private static final boolean requestUriParameterSupported = false;
    private static final boolean requireRequestUriRegistration = false;

    private static final String PROP_NAME_LOCAL_STORE = "localStore";
    private static final String LOCAL_STORE_VALUE = "1234";
    private static final String[] PROP_VALUE_LOCAL_STORE = { LOCAL_STORE_VALUE };
    private static final String PROP_NAME_CLIENT = "client";
    private static final String CLIENT_PID_VALUE = "5678";
    private static final String[] PROP_VALUE_CLIENT = { CLIENT_PID_VALUE };

    private static final String PROP_NAME_DB_STORE = "databaseStore";
    private static final String DB_STORE_VALUE = "db1234";
    private static final String[] PROP_VALUE_DB_STORE = { DB_STORE_VALUE };

    private static final String JNDI_NAME_VALUE = "jdbc/OIDC_DB";

    private final LibertyOAuth20Provider providerForOidcBaseClientTests = new LibertyOAuth20Provider();

    // JSA config attributes
    private static final String JWT_GRANT_TYPE = "jwtGrantType";
    private static final String jwtGrantTypeConfig = "jwtGrantTypeConfig";
    private static final long coverageMapSessionMaxAge = 600;

    private static final long jwtMaxJtiCacheSize = 10000;
    private static final long jwtSkew = 300;
    private static final long jwtMaxLifetimeAllowed = 7200;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        // outputMgr.captureStreams();

        properties = createDefaultProperties();
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void defaults() {
        LibertyOAuth20Provider provider = new LibertyOAuth20Provider();
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                }
            });

            provider.activate(cc, properties);
            assertEquals("The authorizationGrantLifetime value must be set.", authorizationGrantLifetime, provider.getAuthorizationGrantLifetime());
            assertEquals("The authorizationCodeLifetime value must be set.", authorizationCodeLifetime, provider.getAuthorizationCodeLifetime());
            assertEquals("The authorizationCodeLength value must be set.", authorizationCodeLength, provider.getAuthorizationCodeLength());
            assertEquals("The accessTokenLifetime value must be set.", accessTokenLifetime, provider.getAccessTokenLifetime());
            assertEquals("The accessTokenLength value must be set.", accessTokenLength, provider.getAccessTokenLength());
            assertEquals("The issueRefreshToken value must be set.", issueRefreshToken, provider.isIssueRefreshToken());
            assertEquals("The refreshTokenLength value must be set.", refreshTokenLength, provider.getRefreshTokenLength());
            assertEquals("The allowPublicClients value must be set.", allowPublicClients, provider.isAllowPublicClients());
            assertEquals("The grantType value must be set.", grantType, provider.getGrantTypesAllowed());
            assertEquals("The authorizationFormTemplate value must be set.", authorizationFormTemplate, provider.getAuthorizationFormTemplate());
            assertEquals("The authorizationErrorTemplate value must be set.", authorizationErrorTemplate, provider.getAuthorizationErrorTemplate());
            assertEquals("The customLoginURL value must be set.", customLoginURL, provider.getCustomLoginURL());
            assertEquals("The autoAuthorizeParam value must be set.", autoAuthorizeParam, provider.getAutoAuthorizeParam());
            assertEquals("The autoAuthorize value must be set.", autoAuthorize, provider.isAutoAuthorize());
            assertNull("The autoAuthorizeClient value must not be set.", provider.getAutoAuthorizeClients());
            assertNull("The clientURISubstitutions value must not be set.", provider.getClientURISubstitutions());
            assertEquals("The clientTokenCacheSize value must be set to 0.", 0, provider.getClientTokenCacheSize());
            assertNull("The filter value must not be set.", provider.getFilter());
            assertNull("The characterEncoding value must not be set.", provider.getCharacterEncoding());
            assertEquals("The oauthOnly value must be set.", oauthOnly, provider.isOauthOnly());
            assertEquals("The includeTokenInSubject value must be set.", includeTokenInSubject, provider.isIncludeTokenInSubject());
            assertEquals("The consentCacheEntryLifetime value must be set.", consentCacheEntryLifetime, provider.getConsentCacheEntryLifetime());
            assertEquals("The consentCacheSize value must be set.", consentCacheSize, provider.getConsentCacheSize());
            assertEquals("The httpsRequired value must be set.", httpsRequired, provider.isHttpsRequired());
            assertEquals("The certAuthentication must be set.", certAuthentication, provider.isCertAuthentication());
            assertEquals("The coverageMapSessionMaxAge value must be set.", coverageMapSessionMaxAge, provider.getCoverageMapSessionMaxAge());
            assertEquals("The skipUserValidation value must be set.", skipUserValidation, provider.isSkipUserValidation());
            assertEquals("The databaseSchemaName value must be set.", defaultJDBCSchema, provider.getSchemaName());
            assertEquals("The appPasswordLifetime value must be set.", appPasswordLifetime, provider.getAppPasswordLifetime());
            assertEquals("The appTokenLifetime value must be set.", appTokenLifetime, provider.getAppTokenLifetime());
            assertEquals("The appPasswordLifetime value must be set.", appTokenOrPasswordLimit, provider.getAppTokenOrPasswordLimit());
            assertFalse("The ropcPreferUserSecurityName value must be set", provider.isROPCPreferUserSecurityName());

        } catch (Throwable t) {
            outputMgr.failWithThrowable("default", t);
        }
    }

    @Test
    public void testJwtGrantType() {
        String methodName = "testJwtGrantType";
        LibertyOAuth20Provider providerTests = new LibertyOAuth20Provider();

        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdmin).getConfiguration(jwtGrantTypeConfig, "");
                    will(returnValue(config));
                    oneOf(config).getProperties();
                    will(returnValue(jwtGrantTypeProps));
                }
            });

            providerTests.setConfigurationAdmin(configAdmin);

            Map<String, Object> defaultPropertiesWithTestJwtGrantType = createDefaultPropertiesWithJwtGrantType();
            providerTests.activate(cc, defaultPropertiesWithTestJwtGrantType);

            assertEquals("The getJwtMaxJtiCacheSize() is not 10000", jwtMaxJtiCacheSize, providerTests.getJwtMaxJtiCacheSize());
            assertEquals("The getJwtClockSkew() is not 300", jwtSkew, providerTests.getJwtClockSkew());
            assertEquals("The getJwtTokenMaxLifetime() is not 7200", jwtMaxLifetimeAllowed, providerTests.getJwtTokenMaxLifetime());
            assertTrue("iatRequired is not true", providerTests.getJwtIatRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testTokenFormatJwt() {
        String methodName = "testTokenFormatJwt";
        LibertyOAuth20Provider provider = new LibertyOAuth20Provider();
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdmin).getConfiguration(jwtGrantTypeConfig, "");
                    will(returnValue(config));
                    oneOf(config).getProperties();
                    will(returnValue(null));
                }
            });
            Map<String, Object> defaultProperties = createDefaultProperties();
            defaultProperties.put(LibertyOAuth20Provider.KEY_TOKEN_FORMAT, new String[] { "jwt" });
            provider.activate(cc, defaultProperties);

            assertFalse("expected isMpjwt to be false", provider.isMpJwt());
            assertTrue("expected isJwtAccessToken to be true", provider.isJwtAccessToken());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @Test
    public void testRevokeAccessTokensWithRefreshTokens() {
        String methodName = "RevokeAccessTokensWithRefreshTokens";
        LibertyOAuth20Provider provider = new LibertyOAuth20Provider();
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdmin).getConfiguration(jwtGrantTypeConfig, "");
                    will(returnValue(config));
                    oneOf(config).getProperties();
                    will(returnValue(null));
                }
            });
            Map<String, Object> defaultProperties = createDefaultProperties();
            defaultProperties.put(LibertyOAuth20Provider.KEY_REVOKE_ACCESSTOK_W_REFRESHTOK, false);
            provider.activate(cc, defaultProperties);

            assertFalse("expected getRevokeAccessTokensWithRefreshTokens to return false", provider.getRevokeAccessTokensWithRefreshTokens());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @Test
    public void testTokenFormatMpJwt() {
        String methodName = "testTokenFormatMpJwt";
        LibertyOAuth20Provider provider = new LibertyOAuth20Provider();
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdmin).getConfiguration(jwtGrantTypeConfig, "");
                    will(returnValue(config));
                    oneOf(config).getProperties();
                    will(returnValue(null));
                }
            });

            provider.setConfigurationAdmin(configAdmin);
            Map<String, Object> defaultProperties = createDefaultProperties();
            defaultProperties.put(LibertyOAuth20Provider.KEY_TOKEN_FORMAT, new String[] { "mpjwt" });

            provider.activate(cc, defaultProperties);
            assertTrue("expected isMpjwt to be true", provider.isMpJwt());
            assertTrue("expected isJwtAccessToiekn to be true", provider.isJwtAccessToken());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }

    }

    @Test
    public void testTokenFormatOpaque() {
        String methodName = "TestTokenFormatOpaque";
        Map<String, Object> defaultProperties = createDefaultProperties();
        LibertyOAuth20Provider provider = new LibertyOAuth20Provider();
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdmin).getConfiguration(jwtGrantTypeConfig, "");
                    will(returnValue(config));
                    oneOf(config).getProperties();
                    will(returnValue(null));
                }
            });

            provider.activate(cc, defaultProperties);
            assertFalse("expected isMpjwt to be false", provider.isMpJwt());
            assertFalse("expected isJwtAccessToken to be false", provider.isJwtAccessToken());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        // TODO: add test logic
    }

    @Test
    public void testJwtGrantTypeNull() {
        String methodName = "testJwtGrantType";
        LibertyOAuth20Provider providerTests = new LibertyOAuth20Provider();

        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdmin).getConfiguration(jwtGrantTypeConfig, "");
                    will(returnValue(config));
                    oneOf(config).getProperties();
                    will(returnValue(null));
                }
            });

            providerTests.setConfigurationAdmin(configAdmin);

            Map<String, Object> defaultPropertiesWithTestJwtGrantType = createDefaultPropertiesWithJwtGrantType();
            providerTests.activate(cc, defaultPropertiesWithTestJwtGrantType);

            // test default value
            assertEquals("The getJwtMaxJtiCacheSize() is not 10000", 10000, providerTests.getJwtMaxJtiCacheSize());
            assertEquals("The getJwtClockSkew() is not 300", 300, providerTests.getJwtClockSkew());
            assertEquals("The getJwtTokenMaxLifetime() is not 7200", 7200, providerTests.getJwtTokenMaxLifetime());
            assertFalse("iatRequired is not false(default)", providerTests.getJwtIatRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testLocalStoreConfig() {
        String methodName = "testLocalStoreConfig";

        final Dictionary<String, Object> localStoreProps = new Hashtable<String, Object>();
        localStoreProps.put(PROP_NAME_CLIENT, PROP_VALUE_CLIENT);

        final Dictionary<String, Object> clientProps = getSampleOidcBaseClientProperties();

        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdmin).getConfiguration(with(any(String.class)), with(any(String.class)));
                    will(returnValue(config));
                    oneOf(config).getProperties();
                    will(returnValue(localStoreProps));
                    atLeast(2).of(config).getProperties();
                    will(returnValue(clientProps));

                }
            });

            providerForOidcBaseClientTests.setConfigurationAdmin(configAdmin);

            Map<String, Object> defaultPropertiesWithTestLocalStore = createDefaultPropertiesWithTestLocalStore();
            defaultPropertiesWithTestLocalStore.put(LibertyOAuth20Provider.KEY_PROVIDER_ID, AbstractOidcRegistrationBaseTest.COMPONENT_ID);

            providerForOidcBaseClientTests.activate(cc, defaultPropertiesWithTestLocalStore);
            providerForOidcBaseClientTests.createCoreClasses();
            OidcOAuth20ClientProvider clientProvider = providerForOidcBaseClientTests.getClientProvider();

            assertTrue(clientProvider instanceof OidcBaseClientProvider);
            assertFalse(clientProvider instanceof CachedDBOidcClientProvider);

            Collection<OidcBaseClient> clients = clientProvider.getAll();

            // We only seeded it with one client
            Collection<OidcBaseClient> oidcBaseClients = clientProvider.getAll();
            assertEquals(clients.size(), 1);

            OidcBaseClient retrievedOidcBaseClient = extractFirstPossbileOidcBaseClient(oidcBaseClients);

            OidcBaseClient expectedOidcBaseClient = getSampleOidcBaseClientObject(getSampleOidcBaseClientProperties());

            // Ensure object return matches object seeded
            AbstractOidcRegistrationBaseTest.assertEqualsOidcBaseClients(expectedOidcBaseClient, retrievedOidcBaseClient);

            // Deactivation will invoke removeClients indirectly
            providerForOidcBaseClientTests.deactivate(null, null); // Parameters do nothing in the method, anyways

            // Verify clients are removed from this instance
            assertEquals(clientProvider.getAll().size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDatabaseStoreConfig() {
        String methodName = "testDatabaseStoreConfig";

        final Dictionary<String, Object> dbStoreProps = new Hashtable<String, Object>();
        dbStoreProps.put(PROP_NAME_CLIENT, PROP_VALUE_CLIENT);
        final ResourceConfigFactory rcf = mockery.mock(ResourceConfigFactory.class);
        final ResourceFactory rf = mockery.mock(ResourceFactory.class);
        final ResourceConfig rc = mockery.mock(ResourceConfig.class);
        final DataSource ds = mockery.mock(DataSource.class);
        final Connection conn = mockery.mock(Connection.class);
        final Statement sm = mockery.mock(Statement.class);
        final DatabaseMetaData dmd = mockery.mock(DatabaseMetaData.class);
        final ResultSet resultSet = mockery.mock(ResultSet.class);
        final ExecutorService execSvc = mockery.mock(ExecutorService.class);

        Map<String, Object> defaultPropertiesWithTestDbStore = createDefaultPropertiesWithTestDbStore();

        try {
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdmin).getConfiguration(with(any(String.class)), with(any(String.class)));
                    will(returnValue(config));
                    oneOf(config).getProperties();
                    will(returnValue(dbStoreProps));
                    atLeast(2).of(config).getProperties();
                    will(returnValue(dbStoreProps));
                    allowing(rcf).createResourceConfig(DataSource.class.getName());
                    will(returnValue(rc));
                    allowing(rc).setResAuthType(ResourceConfig.AUTH_CONTAINER);
                    // allowing(rc).setSharingScope(ResourceConfig.SHARING_SCOPE_UNSHAREABLE);
                    allowing(rf).createResource(rc);
                    will(returnValue(ds));
                    allowing(dmd).getDriverName();
                    will(returnValue("DERBY"));
                    allowing(conn).isClosed();

                    allowing(ds).getConnection();
                    will(returnValue(conn));
                    allowing(conn).setAutoCommit(false);
                    allowing(conn).getAutoCommit();
                    will(returnValue(false));
                    allowing(conn).commit();
                    allowing(conn).createStatement();
                    will(returnValue(sm));
                    allowing(conn).getMetaData();
                    will(returnValue(dmd));
                    allowing(dmd).getDatabaseProductName();
                    will(returnValue(DB_STORE_VALUE));
                    allowing(dmd).getTables(null, null, "%", null);
                    will(returnValue(resultSet));
                    allowing(resultSet).next();
                    will(returnValue(false));
                    allowing(resultSet).close();
                    allowing(sm).close();
                    allowing(conn).close();
                    allowing(execSvc).submit(with(any(Thread.class)));

                    // allowing(sm.executeUpdate(with((String.class))));
                    // will(returnValue(1));
                }
            });

            providerForOidcBaseClientTests.setConfigurationAdmin(configAdmin);
            providerForOidcBaseClientTests.setDataSourceFactory(rf);
            providerForOidcBaseClientTests.setResourceConfigFactory(rcf);
            providerForOidcBaseClientTests.setExecutorService(execSvc);

            defaultPropertiesWithTestDbStore.put(LibertyOAuth20Provider.KEY_PROVIDER_ID, AbstractOidcRegistrationBaseTest.COMPONENT_ID);

            providerForOidcBaseClientTests.activate(cc, defaultPropertiesWithTestDbStore);
            providerForOidcBaseClientTests.createCoreClasses();
            OidcOAuth20ClientProvider clientProvider = providerForOidcBaseClientTests.getClientProvider();

            assertFalse(clientProvider instanceof OidcBaseClientProvider);
            assertTrue(clientProvider instanceof CachedDBOidcClientProvider);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetProperty() {
        String methodName = "testGetProperty";
        LibertyOAuth20Provider providerTests = new LibertyOAuth20Provider();

        try {

            final Map<String, Object> myProps = createDefaultProperties();
            mockery.checking(new Expectations() {
                {
                    allowing(cc).getBundleContext();
                    allowing(configAdmin).getConfiguration(jwtGrantTypeConfig, "");
                    will(returnValue(config));
                    oneOf(config).getProperties();
                    will(returnValue(myProps));
                }
            });

            providerTests.setConfigurationAdmin(configAdmin);
            providerTests.activate(cc, myProps);

            // Also test getProperty()
            String keyName = "KEY_NAME";
            myProps.put(keyName, 60L);
            assertEquals("Did not get back expected 60", 60, providerTests.getProperty(keyName, 30, 2048));
            myProps.put(keyName, 10L);
            assertEquals("Did not get back minimum - 30", 30, providerTests.getProperty(keyName, 30, 2048));
            myProps.put(keyName, 3000L);
            assertEquals("Did not get back maximum - 2048", 2048, providerTests.getProperty(keyName, 30, 2048));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private OidcBaseClient extractFirstPossbileOidcBaseClient(Collection<OidcBaseClient> oidcBaseClients) {
        if (oidcBaseClients == null | oidcBaseClients.size() == 0) {
            return null;
        }

        OidcBaseClient firstOidcBaseClient = null;

        for (OidcBaseClient client : oidcBaseClients) {
            firstOidcBaseClient = client;
            break;
        }

        return firstOidcBaseClient;
    }

    private Dictionary<String, Object> getSampleOidcBaseClientProperties() {
        final Dictionary<String, Object> clientProps = new Hashtable<String, Object>();
        String[] redirectUris = { AbstractOidcRegistrationBaseTest.REDIRECT_URI_1, AbstractOidcRegistrationBaseTest.REDIRECT_URI_2 };

        String[] grantTypes = { AbstractOidcRegistrationBaseTest.GRANT_TYPES_1, AbstractOidcRegistrationBaseTest.GRANT_TYPES_2 };
        String[] responseTypes = { AbstractOidcRegistrationBaseTest.RESPONSE_TYPES_1, AbstractOidcRegistrationBaseTest.RESPONSE_TYPES_2 };
        String[] postLogoutRedirectUris = { AbstractOidcRegistrationBaseTest.POST_LOGOUT_REDIRECT_URI_1, AbstractOidcRegistrationBaseTest.POST_LOGOUT_REDIRECT_URI_2 };
        String[] trustedUriPrefixes = { AbstractOidcRegistrationBaseTest.TRUSTED_URI_PREFIX_1, AbstractOidcRegistrationBaseTest.TRUSTED_URI_PREFIX_2 };

        clientProps.put(LibertyOAuth20Provider.KEY_PROVIDER_ID, AbstractOidcRegistrationBaseTest.COMPONENT_ID);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_ID, AbstractOidcRegistrationBaseTest.CLIENT_ID);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_SECRET, AbstractOidcRegistrationBaseTest.CLIENT_SECRET);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_REDIRECT, redirectUris);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_DISPLAYNAME, AbstractOidcRegistrationBaseTest.CLIENT_NAME);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_ENABLED, AbstractOidcRegistrationBaseTest.IS_ENABLED);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_TOKEN_EP_AUTH_METHOD, AbstractOidcRegistrationBaseTest.TOKEN_ENDPOINT_AUTH_METHOD);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_SCOPE, AbstractOidcRegistrationBaseTest.SCOPE);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_GRANT_TYPES, grantTypes);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_RESPONSE_TYPES, responseTypes);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_APP_TYPE, AbstractOidcRegistrationBaseTest.APPLICATION_TYPE);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_SUBJECT_TYPE, AbstractOidcRegistrationBaseTest.SUBJECT_TYPE);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_POST_LOGOUT_REDIRECT_URIS, postLogoutRedirectUris);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_PREAUTHORIZED_SCOPE, AbstractOidcRegistrationBaseTest.PREAUTHORIZED_SCOPE);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_INTROSPECT_TOKENS, AbstractOidcRegistrationBaseTest.INTROSPECT_TOKENS);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_TRUSTED_URI_PREFIXES, trustedUriPrefixes);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_FUNCTIONAL_USER_ID, AbstractOidcRegistrationBaseTest.FUNCTIONAL_USER_ID);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_allowRegexpRedirects, Boolean.FALSE);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_APP_PASSWORD_ALLOWED, Boolean.FALSE);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_APP_TOKEN_ALLOWED, Boolean.FALSE);
        clientProps.put(LibertyOAuth20Provider.KEY_CLIENT_PROOF_KEY_FOR_CODE_EXCHANGE, Boolean.FALSE);

        return clientProps;
    }

    private OidcBaseClient getSampleOidcBaseClientObject(Dictionary<String, Object> clientProps) {
        JsonArray redirectUris = OidcOAuth20Util.initJsonArray((String[]) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_REDIRECT));
        JsonArray grantTypes = OidcOAuth20Util.initJsonArray((String[]) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_GRANT_TYPES));
        JsonArray responseTypes = OidcOAuth20Util.initJsonArray((String[]) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_RESPONSE_TYPES));
        JsonArray postLogoutRedirectUris = OidcOAuth20Util.initJsonArray((String[]) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_POST_LOGOUT_REDIRECT_URIS));
        JsonArray trustedUriPrefixes = OidcOAuth20Util.initJsonArray((String[]) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_TRUSTED_URI_PREFIXES));

        OidcBaseClient testOidcBaseClient = new OidcBaseClient((String) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_ID), (String) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_SECRET), redirectUris, (String) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_DISPLAYNAME), (String) clientProps.get(LibertyOAuth20Provider.KEY_PROVIDER_ID), (Boolean) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_ENABLED));

        // Use modifiers to set remaining properties
        testOidcBaseClient.setTokenEndpointAuthMethod((String) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_TOKEN_EP_AUTH_METHOD));
        testOidcBaseClient.setScope((String) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_SCOPE));
        testOidcBaseClient.setGrantTypes(grantTypes);
        testOidcBaseClient.setResponseTypes(responseTypes);
        testOidcBaseClient.setApplicationType((String) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_APP_TYPE));
        testOidcBaseClient.setSubjectType((String) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_SUBJECT_TYPE));
        testOidcBaseClient.setPostLogoutRedirectUris(postLogoutRedirectUris);
        testOidcBaseClient.setPreAuthorizedScope((String) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_PREAUTHORIZED_SCOPE));
        testOidcBaseClient.setIntrospectTokens((Boolean) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_INTROSPECT_TOKENS));
        testOidcBaseClient.setTrustedUriPrefixes(trustedUriPrefixes);
        testOidcBaseClient.setFunctionalUserId((String) clientProps.get(LibertyOAuth20Provider.KEY_CLIENT_FUNCTIONAL_USER_ID));

        return testOidcBaseClient;
    }

    /*
     * Create default properties as defined in metatype.xml
     */
    private static Map<String, Object> createDefaultProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(LibertyOAuth20Provider.KEY_MAX_AUTHGRANT_LT_SECS, authorizationGrantLifetime);
        properties.put(LibertyOAuth20Provider.KEY_CODE_LT_SECS, authorizationCodeLifetime);
        properties.put(LibertyOAuth20Provider.KEY_CODE_LEN, authorizationCodeLength);
        properties.put(LibertyOAuth20Provider.KEY_TOK_LT_SECS, accessTokenLifetime);
        properties.put(LibertyOAuth20Provider.KEY_ACCESS_TOK_LEN, accessTokenLength);
        properties.put(LibertyOAuth20Provider.KEY_ISSUE_REFRESH_TOK, issueRefreshToken);
        properties.put(LibertyOAuth20Provider.KEY_REFRESH_TOK_LEN, refreshTokenLength);
        properties.put(LibertyOAuth20Provider.KEY_ALLOW_PUBLIC_CLIENTS, allowPublicClients);
        properties.put(LibertyOAuth20Provider.KEY_GRANT_TYPE, grantType);
        properties.put(LibertyOAuth20Provider.KEY_AUTHZ_FORM_TEMP, authorizationFormTemplate);
        properties.put(LibertyOAuth20Provider.KEY_AUTHZ_ERR_TEMP, authorizationErrorTemplate);
        properties.put(LibertyOAuth20Provider.KEY_AUTHZ_LOGIN_URL, customLoginURL);
        properties.put(LibertyOAuth20Provider.KEY_AUTO_AUTHZ_PARAM, autoAuthorizeParam);
        properties.put(LibertyOAuth20Provider.KEY_AUTO_AUTHORIZE, autoAuthorize);
        properties.put(LibertyOAuth20Provider.KEY_OUATH_ONLY, oauthOnly);
        properties.put(LibertyOAuth20Provider.KEY_INCLUDE_TOKEN, includeTokenInSubject);
        properties.put(LibertyOAuth20Provider.KEY_CONSENT_CACHE_ENTRY_LIFETIME, consentCacheEntryLifetime);
        properties.put(LibertyOAuth20Provider.KEY_CONSENT_CACHE_SIZE, consentCacheSize);
        properties.put(LibertyOAuth20Provider.KEY_HTTPS_REQUIRED, httpsRequired);
        properties.put(LibertyOAuth20Provider.KEY_CERT_AUTHENTICATION, certAuthentication);

        properties.put(LibertyOAuth20Provider.KEY_COVERAGE_MAP_SESSION_MAX_AGE, coverageMapSessionMaxAge);
        properties.put(LibertyOAuth20Provider.KEY_SKIP_USER_VALIDATION, skipUserValidation);

        properties.put(LibertyOAuth20Provider.KEY_JDBC_SCHEMA, defaultJDBCSchema);
        properties.put(LibertyOAuth20Provider.KEY_JWT_ACCESS_TOKEN, Boolean.FALSE);
        properties.put(LibertyOAuth20Provider.KEY_TOKEN_FORMAT, new String[] { "opaque" });
        properties.put(LibertyOAuth20Provider.KEY_CACHE_ACCESSTOKEN, Boolean.TRUE);
        properties.put(LibertyOAuth20Provider.KEY_REVOKE_ACCESSTOK_W_REFRESHTOK, Boolean.TRUE);
        properties.put(LibertyOAuth20Provider.KEY_APP_PASSWORD_LIFETIME, appPasswordLifetime);
        properties.put(LibertyOAuth20Provider.KEY_APP_TOKEN_LIFETIME, appTokenLifetime);
        properties.put(LibertyOAuth20Provider.KEY_APP_TOKEN_OR_PASSWORD_LIMIT, 100L);
        properties.put(LibertyOAuth20Provider.KEY_STORE_ACCESSTOKEN_ENCODING, "plain");
        properties.put(LibertyOAuth20Provider.KEY_ROPC_PREFER_USERSECURITYNAME, Boolean.FALSE);
        properties.put(LibertyOAuth20Provider.KEY_TRACK_RELYING_PARTIES, Boolean.FALSE);

        return properties;
    }

    private static Map<String, Object> createDefaultPropertiesWithTestLocalStore() {
        Map<String, Object> properties = createDefaultProperties();
        properties.put(PROP_NAME_LOCAL_STORE, PROP_VALUE_LOCAL_STORE);
        return properties;
    }

    private static Map<String, Object> createDefaultPropertiesWithJwtGrantType() {
        Map<String, Object> properties = createDefaultProperties();
        // JWT Metatype
        properties.put(JWT_GRANT_TYPE, new String[] { jwtGrantTypeConfig });
        jwtGrantTypeProps.put(LibertyOAuth20Provider.KEY_JWT_MAX_JTI_CACHE_SIZE, jwtMaxJtiCacheSize);
        jwtGrantTypeProps.put(LibertyOAuth20Provider.KEY_JWT_SKEW, jwtSkew);
        jwtGrantTypeProps.put(LibertyOAuth20Provider.KEY_JWT_TOKEN_MAX_LIFETIME, jwtMaxLifetimeAllowed);
        jwtGrantTypeProps.put(LibertyOAuth20Provider.KEY_JWT_IAT_REQUIRED, true);

        return properties;
    }

    private static Map<String, Object> createDefaultPropertiesWithTestDbStore() {
        Map<String, Object> properties = createDefaultProperties();
        properties.put(PROP_NAME_DB_STORE, PROP_VALUE_DB_STORE);
        properties.put("databaseStore.0.dummy", "bob");
        return properties;
    }
}
