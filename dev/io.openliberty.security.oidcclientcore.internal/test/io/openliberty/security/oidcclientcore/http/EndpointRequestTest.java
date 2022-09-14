/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.net.ssl.SSLSocketFactory;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.discovery.DiscoveryHandler;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import test.common.SharedOutputManager;

public class EndpointRequestTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final OidcClientConfig config = mockery.mock(OidcClientConfig.class);
    private final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);
    private final HttpUtils mockedHttpUtils = mockery.mock(HttpUtils.class);

    private final String CWWKS2401E_OIDC_CLIENT_CONFIGURATION_ERROR = "CWWKS2401E";
    private final String CWWKS2403E_DISCOVERY_EXCEPTION = "CWWKS2403E";
    private final String CWWKS2404E_OIDC_CLIENT_MISSING_PROVIDER_URI = "CWWKS2404E";

    private final String clientId = "myOidcClientId";
    private final String authorizationEndpointUrl = "https://localhost:8020/oidc/op/authorize";
    private final String providerUrlBase = "https://localhost:8020/oidc/";

    private String providerUrl = "https://localhost:8020/oidc/";

    private DiscoveryHandler discoveryHandler = null;

    private EndpointRequest endpointRequest;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        mockery.checking(new Expectations() {
            {
                allowing(config).getClientId();
                will(returnValue(clientId));
            }
        });
        discoveryHandler = new DiscoveryHandler(sslSocketFactory) {
            @Override
            protected HttpUtils getHttpUtils() {
                return mockedHttpUtils;
            }
        };
        endpointRequest = createEndpointRequest();
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

    private EndpointRequest createEndpointRequest() {
        return new EndpointRequest() {
            @Override
            public DiscoveryHandler getDiscoveryHandler() {
                return discoveryHandler;
            }
        };
    }

    @Test
    public void test_getProviderDiscoveryMetadata_missingProviderUri() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderURI();
                    will(returnValue(null));
                }
            });
            JSONObject result = endpointRequest.getProviderDiscoveryMetadata(config);
            assertNull("Result should not have been null but got " + result, result);
            verifyLogMessage(outputMgr, CWWKS2401E_OIDC_CLIENT_CONFIGURATION_ERROR + ".+" + CWWKS2404E_OIDC_CLIENT_MISSING_PROVIDER_URI);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getProviderDiscoveryMetadata_providerURIMissingWellKnownSuffix() throws OidcDiscoveryException {
        final String expectedDiscoveryUrl = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        final JSONObject discoveryData = new JSONObject();
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderURI();
                    will(returnValue(providerUrl));
                    one(mockedHttpUtils).getHttpJsonRequest(sslSocketFactory, expectedDiscoveryUrl, true, false);
                    will(returnValue(discoveryData.toString()));
                }
            });
            JSONObject result = endpointRequest.getProviderDiscoveryMetadata(config);
            assertEquals(discoveryData, result);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getProviderDiscoveryMetadata_discoveryThrowsException() throws OidcDiscoveryException {
        providerUrl = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderURI();
                    will(returnValue(providerUrl));
                    one(mockedHttpUtils).getHttpJsonRequest(sslSocketFactory, providerUrl, true, false);
                    will(returnValue("Not JSON"));
                }
            });
            JSONObject result = endpointRequest.getProviderDiscoveryMetadata(config);
            assertNull("Result should not have been null but got " + result, result);
            verifyLogMessage(outputMgr, CWWKS2403E_DISCOVERY_EXCEPTION + ".+" + "Unexpected character");
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getProviderDiscoveryMetadata_goldenPath() throws OidcDiscoveryException {
        providerUrl = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        final JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_AUTHORIZATION_ENDPOINT, authorizationEndpointUrl);
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getProviderURI();
                    will(returnValue(providerUrl));
                    one(mockedHttpUtils).getHttpJsonRequest(sslSocketFactory, providerUrl, true, false);
                    will(returnValue(discoveryData.toString()));
                }
            });
            JSONObject result = endpointRequest.getProviderDiscoveryMetadata(config);
            assertEquals(discoveryData, result);

            // Should get metadata from the cache, so shouldn't need another discoveryHandler call
            JSONObject result2 = endpointRequest.getProviderDiscoveryMetadata(config);
            assertEquals(discoveryData, result2);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_noSuffix_noTrailingSlash() throws OidcDiscoveryException {
        String input = providerUrl;
        String result = endpointRequest.addWellKnownSuffixIfNeeded(input);
        String expectedValue = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        assertEquals(expectedValue, result);
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_noSuffix_withTrailingSlash() throws OidcDiscoveryException {
        String input = providerUrl + "/";
        String result = endpointRequest.addWellKnownSuffixIfNeeded(input);
        String expectedValue = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        assertEquals(expectedValue, result);
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_includesSuffix_notAsSeparatePath() throws OidcDiscoveryException {
        String input = providerUrl + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        String result = endpointRequest.addWellKnownSuffixIfNeeded(input);
        assertEquals(input, result);
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_includesSuffix() throws OidcDiscoveryException {
        String input = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        String result = endpointRequest.addWellKnownSuffixIfNeeded(input);
        assertEquals(input, result);
    }

}
