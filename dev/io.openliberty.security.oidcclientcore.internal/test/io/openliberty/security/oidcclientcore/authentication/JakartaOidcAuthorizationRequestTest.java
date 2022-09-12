/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.discovery.DiscoveryHandler;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import test.common.SharedOutputManager;

public class JakartaOidcAuthorizationRequestTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final OidcClientConfig config = mockery.mock(OidcClientConfig.class);
    private final OidcProviderMetadata providerMetadata = mockery.mock(OidcProviderMetadata.class);
    private final DiscoveryHandler discoveryHandler = mockery.mock(DiscoveryHandler.class);
    private final AuthorizationRequestParameters authzParameters = mockery.mock(AuthorizationRequestParameters.class);

    private final String CWWKS2401E_OIDC_CLIENT_CONFIGURATION_ERROR = "CWWKS2401E";
    private final String CWWKS2403E_DISCOVERY_EXCEPTION = "CWWKS2403E";
    private final String CWWKS2404E_OIDC_CLIENT_MISSING_PROVIDER_URI = "CWWKS2404E";
    private final String CWWKS2405E_DISCOVERY_METADATA_MISSING_VALUE = "CWWKS2405E";

    private final String clientId = "myOidcClientId";
    private final String authorizationEndpointUrl = "https://localhost:8020/oidc/op/authorize";
    private final String providerUrlBase = "https://localhost:8020/oidc/";

    private String providerUrl = "https://localhost:8020/oidc/";

    private JakartaOidcAuthorizationRequest authzRequest;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        authzRequest = createJakartaOidcAuthorizationRequest(providerMetadata);
        // Ensure provider URLs are unique for each test to avoid cache contamination between tests
        providerUrl = providerUrlBase + testName.getMethodName();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    private JakartaOidcAuthorizationRequest createJakartaOidcAuthorizationRequest(OidcProviderMetadata providerMetadata) {
        mockery.checking(new Expectations() {
            {
                one(config).getClientId();
                will(returnValue(clientId));
                one(config).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(config).isUseSession();
                will(returnValue(true));
            }
        });
        return new JakartaOidcAuthorizationRequest(request, response, config) {
            @Override
            public ProviderAuthenticationResult sendRequest() {
                try {
                    discoveryData = getProviderMetadata();
                    return null;
                } catch (Exception e) {
                    Tr.error(tc, "ERROR_SENDING_AUTHORIZATION_REQUEST", clientId, e.getMessage());
                    return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
                }
            }

            @Override
            DiscoveryHandler getDiscoveryHandler() {
                return discoveryHandler;
            }
        };
    }

    @Test
    public void test_getAuthorizationEndpoint_providerMetadataContainsValidAuthzEndpoint() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(providerMetadata).getAuthorizationEndpoint();
                    will(returnValue(authorizationEndpointUrl));
                }
            });
            String result = authzRequest.getAuthorizationEndpoint();
            assertEquals(authorizationEndpointUrl, result);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getAuthorizationEndpoint_noProviderMetadata_discoveryMetadataMissingAuthzEndpoint() {
        authzRequest = createJakartaOidcAuthorizationRequest(null);

        final String expectedDiscoveryUrl = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        final JSONObject discoveryData = new JSONObject();
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getProviderURI();
                    will(returnValue(providerUrl));
                    one(discoveryHandler).fetchDiscoveryDataJson(expectedDiscoveryUrl, clientId);
                    will(returnValue(discoveryData));
                }
            });
            String result = authzRequest.getAuthorizationEndpoint();
            fail("Should have thrown an exception but got " + result);
        } catch (OidcDiscoveryException e) {
            verifyException(e, CWWKS2403E_DISCOVERY_EXCEPTION + ".+" + CWWKS2405E_DISCOVERY_METADATA_MISSING_VALUE + ".+"
                               + OidcDiscoveryConstants.METADATA_KEY_AUTHORIZATION_ENDPOINT);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getAuthorizationEndpoint_noProviderMetadata_discoveryMetadataContainsAuthzEndpoint() {
        authzRequest = createJakartaOidcAuthorizationRequest(null);

        final String expectedDiscoveryUrl = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        final JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_AUTHORIZATION_ENDPOINT, authorizationEndpointUrl);
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderURI();
                    will(returnValue(providerUrl));
                    one(discoveryHandler).fetchDiscoveryDataJson(expectedDiscoveryUrl, clientId);
                    will(returnValue(discoveryData));
                }
            });
            // Have to call sendRequest() to populate the discovery data
            authzRequest.sendRequest();

            String result = authzRequest.getAuthorizationEndpoint();
            assertEquals(authorizationEndpointUrl, result);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getProviderMetadata_missingProviderUri() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderURI();
                    will(returnValue(null));
                }
            });
            JSONObject result = authzRequest.getProviderMetadata();
            fail("Should have thrown an exception but didn't. Got: " + result);
        } catch (OidcClientConfigurationException e) {
            verifyException(e, CWWKS2401E_OIDC_CLIENT_CONFIGURATION_ERROR + ".+" + CWWKS2404E_OIDC_CLIENT_MISSING_PROVIDER_URI);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getProviderMetadata_providerURIMissingWellKnownSuffix() throws OidcDiscoveryException {
        final String expectedDiscoveryUrl = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        final JSONObject discoveryData = new JSONObject();
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderURI();
                    will(returnValue(providerUrl));
                    one(discoveryHandler).fetchDiscoveryDataJson(expectedDiscoveryUrl, clientId);
                    will(returnValue(discoveryData));
                }
            });
            JSONObject result = authzRequest.getProviderMetadata();
            assertEquals(discoveryData, result);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getProviderMetadata_discoveryThrowsException() throws OidcDiscoveryException {
        providerUrl = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderURI();
                    will(returnValue(providerUrl));
                    one(discoveryHandler).fetchDiscoveryDataJson(providerUrl, clientId);
                    will(throwException(new OidcDiscoveryException(clientId, providerUrl, defaultExceptionMsg)));
                }
            });
            JSONObject result = authzRequest.getProviderMetadata();
            fail("Should have thrown an exception but didn't. Got: " + result);
        } catch (OidcDiscoveryException e) {
            verifyException(e, CWWKS2403E_DISCOVERY_EXCEPTION + ".+" + Pattern.quote(defaultExceptionMsg));
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getProviderMetadata_goldenPath() throws OidcDiscoveryException {
        providerUrl = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        final JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_AUTHORIZATION_ENDPOINT, authorizationEndpointUrl);
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getProviderURI();
                    will(returnValue(providerUrl));
                    one(discoveryHandler).fetchDiscoveryDataJson(providerUrl, clientId);
                    will(returnValue(discoveryData));
                }
            });
            JSONObject result = authzRequest.getProviderMetadata();
            assertEquals(discoveryData, result);

            // Should get metadata from the cache, so shouldn't need another discoveryHandler call
            JSONObject result2 = authzRequest.getProviderMetadata();
            assertEquals(discoveryData, result2);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_noSuffix_noTrailingSlash() throws OidcDiscoveryException {
        String input = providerUrl;
        String result = authzRequest.addWellKnownSuffixIfNeeded(input);
        String expectedValue = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        assertEquals(expectedValue, result);
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_noSuffix_withTrailingSlash() throws OidcDiscoveryException {
        String input = providerUrl + "/";
        String result = authzRequest.addWellKnownSuffixIfNeeded(input);
        String expectedValue = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        assertEquals(expectedValue, result);
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_includesSuffix_notAsSeparatePath() throws OidcDiscoveryException {
        String input = providerUrl + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        String result = authzRequest.addWellKnownSuffixIfNeeded(input);
        assertEquals(input, result);
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_includesSuffix() throws OidcDiscoveryException {
        String input = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        String result = authzRequest.addWellKnownSuffixIfNeeded(input);
        assertEquals(input, result);
    }

    @Test
    public void test_addConditionalParameters_noParameters() throws OidcDiscoveryException {
        mockery.checking(new Expectations() {
            {
                one(config).getExtraParameters();
                will(returnValue(null));
            }
        });
        authzRequest.addExtraParameters(authzParameters);
    }

    @Test
    public void test_addConditionalParameters_emptyParameters() throws OidcDiscoveryException {
        final String[] extraParams = new String[0];
        mockery.checking(new Expectations() {
            {
                one(config).getExtraParameters();
                will(returnValue(extraParams));
            }
        });
        authzRequest.addExtraParameters(authzParameters);
    }

    @Test
    public void test_addConditionalParameters_oneParameter_keyWithNoValue() throws OidcDiscoveryException {
        String key = "some_parameter";
        final String[] extraParams = new String[] { key };
        mockery.checking(new Expectations() {
            {
                one(config).getExtraParameters();
                will(returnValue(extraParams));
                one(authzParameters).addParameter(key, "");
            }
        });
        authzRequest.addExtraParameters(authzParameters);
    }

    @Test
    public void test_addConditionalParameters_oneParameter_valueIsEmptyString() throws OidcDiscoveryException {
        String key = "some_parameter";
        String value = "";
        final String[] extraParams = new String[] { key + "=" + value };
        mockery.checking(new Expectations() {
            {
                one(config).getExtraParameters();
                will(returnValue(extraParams));
                one(authzParameters).addParameter(key, "");
            }
        });
        authzRequest.addExtraParameters(authzParameters);
    }

    @Test
    public void test_addConditionalParameters_oneParameter_goldenPath() throws OidcDiscoveryException {
        String key = "some_parameter";
        String value = "value";
        final String[] extraParams = new String[] { key + "=" + value };
        mockery.checking(new Expectations() {
            {
                one(config).getExtraParameters();
                will(returnValue(extraParams));
                one(authzParameters).addParameter(key, value);
            }
        });
        authzRequest.addExtraParameters(authzParameters);
    }

    @Test
    public void test_addConditionalParameters_multipleParameters() throws OidcDiscoveryException {
        String key1 = "some_parameter";
        String value1 = "value";
        String key2 = "i've got a space in my name";
        String value2 = "and some !@#$ weird characters";
        String key3 = "here&there";
        String value3 = "every%3Dwhere";
        final String[] extraParams = new String[] { key1 + "=" + value1, key2 + "=" + value2, key3 + "=" + value3, };
        mockery.checking(new Expectations() {
            {
                one(config).getExtraParameters();
                will(returnValue(extraParams));
                one(authzParameters).addParameter(key1, value1);
                one(authzParameters).addParameter(key2, value2);
                one(authzParameters).addParameter(key3, value3);
            }
        });
        authzRequest.addExtraParameters(authzParameters);
    }

}
