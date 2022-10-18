/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.discovery.DiscoveryHandler;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.http.EndpointRequest;
import test.common.SharedOutputManager;

public class MetadataUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    public static final String CWWKS2403E_DISCOVERY_EXCEPTION = "CWWKS2403E";
    public static final String CWWKS2405E_DISCOVERY_METADATA_MISSING_VALUE = "CWWKS2405E";

    private final OidcClientConfig oidcClientConfig = mockery.mock(OidcClientConfig.class);
    private final EndpointRequest endpointRequestClass = mockery.mock(EndpointRequest.class);
    private final OidcProviderMetadata providerMetadata = mockery.mock(OidcProviderMetadata.class);
    private final DiscoveryHandler discoveryHandler = mockery.mock(DiscoveryHandler.class);

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
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_getValueFromProviderOrDiscoveryMetadata_providerMetadataHasValue() throws OidcDiscoveryException {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getUserinfoEndpoint();
                will(returnValue(userInfoEndpoint));
            }
        });
        String result = MetadataUtils.getValueFromProviderOrDiscoveryMetadata(endpointRequestClass,
                                                                              oidcClientConfig,
                                                                              metadata -> metadata.getUserinfoEndpoint(),
                                                                              OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT);
        assertEquals(userInfoEndpoint, result);
    }

    @Test
    public void test_getValueFromProviderOrDiscoveryMetadata_providerMetadataHasEmptyValue() throws OidcDiscoveryException {
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_ISSUER, sampleStringValue);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getIssuer();
                will(returnValue(""));
                one(endpointRequestClass).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });
        String result = MetadataUtils.getValueFromProviderOrDiscoveryMetadata(endpointRequestClass,
                                                                              oidcClientConfig,
                                                                              metadata -> metadata.getIssuer(),
                                                                              OidcDiscoveryConstants.METADATA_KEY_ISSUER);
        assertEquals(sampleStringValue, result);
    }

    @Test
    public void test_getValueFromProviderOrDiscoveryMetadata_noProviderMetadata_discoveryMissingEntry() {
        JSONObject discoveryData = new JSONObject();
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(null));
                one(endpointRequestClass).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });
        try {
            String result = MetadataUtils.getValueFromProviderOrDiscoveryMetadata(endpointRequestClass,
                                                                                  oidcClientConfig,
                                                                                  metadata -> metadata.getTokenEndpoint(),
                                                                                  OidcDiscoveryConstants.METADATA_KEY_TOKEN_ENDPOINT);
            fail("Should have thrown an exception but got: [" + result + "].");
        } catch (OidcDiscoveryException e) {
            verifyException(e, CWWKS2403E_DISCOVERY_EXCEPTION + ".*" + CWWKS2405E_DISCOVERY_METADATA_MISSING_VALUE + ".*" + OidcDiscoveryConstants.METADATA_KEY_TOKEN_ENDPOINT);
        }
    }

}
