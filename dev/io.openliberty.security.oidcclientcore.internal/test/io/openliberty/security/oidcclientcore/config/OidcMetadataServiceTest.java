/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import test.common.SharedOutputManager;

public class OidcMetadataServiceTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final OidcClientConfig config = mockery.mock(OidcClientConfig.class);
    private final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);
    private final HttpUtils mockedHttpUtils = mockery.mock(HttpUtils.class);

    private final String CWWKS2401E_OIDC_CLIENT_CONFIGURATION_ERROR = "CWWKS2401E";
    private final String CWWKS2403E_DISCOVERY_EXCEPTION = "CWWKS2403E";
    private final String CWWKS2404W_OIDC_CLIENT_MISSING_PROVIDER_URI = "CWWKS2404W";

    private final String clientId = "myOidcClientId";
    private final String authorizationEndpointUrl = "https://localhost:8020/oidc/op/authorize";
    private final String providerUrlBase = "https://localhost:8020/oidc/";

    private String providerUrl = "https://localhost:8020/oidc/";

    private DiscoveryHandler discoveryHandler = null;

    private OidcMetadataService oidcMetadataService;

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
        oidcMetadataService = createOidcMetadataService();
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

    private OidcMetadataService createOidcMetadataService() {
        return new OidcMetadataService() {
            @Override
            public DiscoveryHandler getDiscoveryHandler() {
                return discoveryHandler;
            }
        };
    }

    @Test
    public void test_getProviderDiscoveryMetadata_missingProviderUri() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(config).getProviderURI();
                will(returnValue(null));
            }
        });
        try {
            JSONObject result = oidcMetadataService.getProviderDiscoveryMetadata(config);
            fail("Should have thrown an exception but got " + result);
        } catch (OidcClientConfigurationException e) {
            verifyException(e, CWWKS2401E_OIDC_CLIENT_CONFIGURATION_ERROR + ".+" + CWWKS2404W_OIDC_CLIENT_MISSING_PROVIDER_URI);
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
            JSONObject result = oidcMetadataService.getProviderDiscoveryMetadata(config);
            assertEquals(discoveryData, result);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getProviderDiscoveryMetadata_discoveryThrowsException() throws Exception {
        providerUrl = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        mockery.checking(new Expectations() {
            {
                one(config).getProviderURI();
                will(returnValue(providerUrl));
                one(mockedHttpUtils).getHttpJsonRequest(sslSocketFactory, providerUrl, true, false);
                will(returnValue("Not JSON"));
            }
        });
        try {
            JSONObject result = oidcMetadataService.getProviderDiscoveryMetadata(config);
            fail("Should have thrown an exception but got " + result);
        } catch (OidcDiscoveryException e) {
            verifyException(e, CWWKS2403E_DISCOVERY_EXCEPTION + ".+" + "Unexpected character");
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
            JSONObject result = oidcMetadataService.getProviderDiscoveryMetadata(config);
            assertEquals(discoveryData, result);

            // Should get metadata from the cache, so shouldn't need another discoveryHandler call
            JSONObject result2 = oidcMetadataService.getProviderDiscoveryMetadata(config);
            assertEquals(discoveryData, result2);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_noSuffix_noTrailingSlash() throws OidcDiscoveryException {
        String input = providerUrl;
        String result = oidcMetadataService.addWellKnownSuffixIfNeeded(input);
        String expectedValue = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        assertEquals(expectedValue, result);
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_noSuffix_withTrailingSlash() throws OidcDiscoveryException {
        String input = providerUrl + "/";
        String result = oidcMetadataService.addWellKnownSuffixIfNeeded(input);
        String expectedValue = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        assertEquals(expectedValue, result);
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_includesSuffix_notAsSeparatePath() throws OidcDiscoveryException {
        String input = providerUrl + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        String result = oidcMetadataService.addWellKnownSuffixIfNeeded(input);
        assertEquals(input, result);
    }

    @Test
    public void test_addWellKnownSuffixIfNeeded_includesSuffix() throws OidcDiscoveryException {
        String input = providerUrl + "/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
        String result = oidcMetadataService.addWellKnownSuffixIfNeeded(input);
        assertEquals(input, result);
    }

}
