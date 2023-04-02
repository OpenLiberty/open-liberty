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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import test.common.SharedOutputManager;

public class MetadataUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    public static final String CWWKS2403E_DISCOVERY_EXCEPTION = "CWWKS2403E";
    public static final String CWWKS2405E_DISCOVERY_METADATA_MISSING_VALUE = "CWWKS2405E";

    private final OidcClientConfig oidcClientConfig = mockery.mock(OidcClientConfig.class);
    private final OidcMetadataService oidcMetadataService = mockery.mock(OidcMetadataService.class);
    private final OidcProviderMetadata providerMetadata = mockery.mock(OidcProviderMetadata.class);

    private final String clientId = "myClientId";
    private final String discoveryUrl = "https://localhost/OP/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
    private final String userInfoEndpoint = "https://localhost/OP/userinfo";
    private final String sampleStringValue = "some string value";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        mockery.checking(new Expectations() {
            {
                allowing(oidcClientConfig).getProviderURI();
                will(returnValue(discoveryUrl));
                allowing(oidcClientConfig).getClientId();
                will(returnValue(clientId));
            }
        });
        MetadataUtils utils = new MetadataUtils();
        utils.setOidcMetadataService(oidcMetadataService);
    }

    @After
    public void tearDown() {
        MetadataUtils utils = new MetadataUtils();
        utils.unsetOidcMetadataService(oidcMetadataService);
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_getValueFromProviderOrDiscoveryMetadata_providerMetadataHasValue() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getUserinfoEndpoint();
                will(returnValue(userInfoEndpoint));
            }
        });
        String result = MetadataUtils.getValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                                              metadata -> metadata.getUserinfoEndpoint(),
                                                                              OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT);
        assertEquals(userInfoEndpoint, result);
    }

    @Test
    public void test_getValueFromProviderOrDiscoveryMetadata_providerMetadataHasEmptyValue() throws Exception {
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_ISSUER, sampleStringValue);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getIssuer();
                will(returnValue(""));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });
        String result = MetadataUtils.getValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                                              metadata -> metadata.getIssuer(),
                                                                              OidcDiscoveryConstants.METADATA_KEY_ISSUER);
        assertEquals(sampleStringValue, result);
    }

    @Test
    public void test_getValueFromProviderOrDiscoveryMetadata_noProviderMetadata_discoveryMissingEntry() throws Exception {
        JSONObject discoveryData = new JSONObject();
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(null));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });
        try {
            String result = MetadataUtils.getValueFromProviderOrDiscoveryMetadata(oidcClientConfig,
                                                                                  metadata -> metadata.getTokenEndpoint(),
                                                                                  OidcDiscoveryConstants.METADATA_KEY_TOKEN_ENDPOINT);
            fail("Should have thrown an exception but got: [" + result + "].");
        } catch (OidcDiscoveryException e) {
            verifyException(e, CWWKS2403E_DISCOVERY_EXCEPTION + ".*" + CWWKS2405E_DISCOVERY_METADATA_MISSING_VALUE + ".*" + OidcDiscoveryConstants.METADATA_KEY_TOKEN_ENDPOINT);
        }
    }

    @Test
    public void test_getUserInfoSigningAlgorithmsSupported_discoveryMissingEntry() throws Exception {
        JSONObject discoveryData = new JSONObject();
        JSONArray algsSupported = new JSONArray();
        algsSupported.add("ES256");
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, algsSupported);
        mockery.checking(new Expectations() {
            {
                allowing(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });
        try {
            String[] result = MetadataUtils.getUserInfoSigningAlgorithmsSupported(oidcClientConfig);
            fail("Should have thrown an exception but got: [" + result + "].");
        } catch (OidcDiscoveryException e) {
            verifyException(e, CWWKS2403E_DISCOVERY_EXCEPTION + ".*" + CWWKS2405E_DISCOVERY_METADATA_MISSING_VALUE + ".*"
                               + OidcDiscoveryConstants.METADATA_KEY_USER_INFO_SIGNING_ALG_VALUES_SUPPORTED);
        }
    }

    @Test
    public void test_getUserInfoSigningAlgorithmsSupported_emptyArray() throws Exception {
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_USER_INFO_SIGNING_ALG_VALUES_SUPPORTED, new JSONArray());
        mockery.checking(new Expectations() {
            {
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });
        String[] result = MetadataUtils.getUserInfoSigningAlgorithmsSupported(oidcClientConfig);
        assertNotNull("Signing algorithm list should not have been null but was.", result);
        assertEquals("Signing algorithm list should have been empty but was " + Arrays.toString(result), 0, result.length);
    }

    @Test
    public void test_getUserInfoSigningAlgorithmsSupported() throws Exception {
        JSONObject discoveryData = new JSONObject();
        JSONArray algsSupported = new JSONArray();
        algsSupported.add("HS256");
        algsSupported.add("RS256");
        algsSupported.add("ES256");
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_USER_INFO_SIGNING_ALG_VALUES_SUPPORTED, algsSupported);
        mockery.checking(new Expectations() {
            {
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });
        String[] result = MetadataUtils.getUserInfoSigningAlgorithmsSupported(oidcClientConfig);
        assertNotNull("Signing algorithm list should not have been null but was.", result);
        assertEquals("Signing algorithm list did not have the expected number of entries: " + Arrays.toString(result), 3, result.length);
        List<String> resultAsList = Arrays.asList(result);
        for (int i = 0; i < algsSupported.size(); i++) {
            assertTrue("Result is missing " + algsSupported.get(i) + ".", resultAsList.contains(algsSupported.get(i)));
        }

    }

    public void test_getProviderMetadata_providerMetadataHasValue() throws Exception {
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_ISSUER, sampleStringValue);
        JSONObject result = MetadataUtils.getProviderDiscoveryMetaData(oidcClientConfig);

        assertNotNull("Should have returned providerMetadata JSONObject", result);
        assertTrue("Expected " + OidcDiscoveryConstants.METADATA_KEY_ISSUER + " key in JSONObject", result.containsKey(OidcDiscoveryConstants.METADATA_KEY_ISSUER));
        assertEquals(sampleStringValue, result.get(OidcDiscoveryConstants.METADATA_KEY_ISSUER));

    }

    @Test
    public void test_getProviderMetadata_providerMetadataNull() throws Exception {
        JSONObject discoveryData = null;
        mockery.checking(new Expectations() {
            {
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });
        JSONObject result = MetadataUtils.getProviderDiscoveryMetaData(oidcClientConfig);

        assertNull("Should not have returned providerMetadata JSONObject", result);

    }

    @Test
    public void test_getProviderMetadata_providerMetadataIsEmpty() throws Exception {
        JSONObject discoveryData = new JSONObject();
        mockery.checking(new Expectations() {
            {
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });

        JSONObject result = MetadataUtils.getProviderDiscoveryMetaData(oidcClientConfig);

        assertNotNull("Should have returned providerMetadata JSONObject", result);
        assertTrue("Expected empty JSONObject", result.isEmpty());

    }

}
