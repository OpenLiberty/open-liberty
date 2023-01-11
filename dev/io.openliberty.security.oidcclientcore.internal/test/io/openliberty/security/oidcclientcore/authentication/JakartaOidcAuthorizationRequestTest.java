/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.authentication;

import static io.openliberty.security.oidcclientcore.authentication.JakartaOidcAuthorizationRequest.IS_CONTAINER_INITIATED_FLOW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.config.OidcMetadataService;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.storage.SessionBasedStorage;
import test.common.SharedOutputManager;

public class JakartaOidcAuthorizationRequestTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final OidcClientConfig config = mockery.mock(OidcClientConfig.class);
    private final OidcProviderMetadata providerMetadata = mockery.mock(OidcProviderMetadata.class);
    private final AuthorizationRequestParameters authzParameters = mockery.mock(AuthorizationRequestParameters.class);
    private final SessionBasedStorage sessionBasedStorage = mockery.mock(SessionBasedStorage.class);
    private final OidcMetadataService oidcMetadataService = mockery.mock(OidcMetadataService.class);

    private final String CWWKS2403E_DISCOVERY_EXCEPTION = "CWWKS2403E";
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
        authzRequest = createJakartaOidcAuthorizationRequest();
        // Ensure provider URLs are unique for each test to avoid cache contamination between tests
        providerUrl = providerUrlBase + testName.getMethodName();

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

    private JakartaOidcAuthorizationRequest createJakartaOidcAuthorizationRequest() {
        mockery.checking(new Expectations() {
            {
                one(config).getClientId();
                will(returnValue(clientId));
                one(config).isUseSession();
                will(returnValue(true));
            }
        });
        return new JakartaOidcAuthorizationRequest(request, response, config) {

            {
                this.storage = sessionBasedStorage;
            }
        };
    }

    @Test
    public void test_getAuthorizationEndpoint_providerMetadataContainsValidAuthzEndpoint() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderMetadata();
                    will(returnValue(providerMetadata));
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
        authzRequest = createJakartaOidcAuthorizationRequest();

        final JSONObject discoveryData = new JSONObject();
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderMetadata();
                    will(returnValue(null));
                    one(oidcMetadataService).getProviderDiscoveryMetadata(config);
                    will(returnValue(discoveryData));
                    one(config).getClientId();
                    will(returnValue(clientId));
                    one(config).getProviderURI();
                    will(returnValue(providerUrl));
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
        authzRequest = createJakartaOidcAuthorizationRequest();

        final JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_AUTHORIZATION_ENDPOINT, authorizationEndpointUrl);
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getProviderMetadata();
                    will(returnValue(null));
                    one(oidcMetadataService).getProviderDiscoveryMetadata(config);
                    will(returnValue(discoveryData));
                }
            });
            String result = authzRequest.getAuthorizationEndpoint();
            assertEquals(authorizationEndpointUrl, result);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
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

    @Test
    public void test_addConditionalParameters_twoEqualsSigns() throws OidcDiscoveryException {
        String key = "some_parameter";
        String value = "value=1";
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
    public void test_addConditionalParameters_noEqualsSign() throws OidcDiscoveryException {
        String key = "some_parameter";
        String value = "value";
        final String[] extraParams = new String[] { key + value };
        mockery.checking(new Expectations() {
            {
                one(config).getExtraParameters();
                will(returnValue(extraParams));
            }
        });
        authzRequest.addExtraParameters(authzParameters);
    }

    @Test
    public void test_isContainerInitiatedFlow() {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(IS_CONTAINER_INITIATED_FLOW);
                will(returnValue(true));
                one(request).removeAttribute(IS_CONTAINER_INITIATED_FLOW);
            }
        });

        boolean result = authzRequest.isContainerInitiatedFlow();

        assertTrue("It should be return true, indicating that it is a container initiated flow.", result);
    }

    @Test
    public void test_isContainerInitiatedFlow_notContainerInitiated() {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(IS_CONTAINER_INITIATED_FLOW);
                will(returnValue(false));
                one(request).removeAttribute(IS_CONTAINER_INITIATED_FLOW);
            }
        });

        boolean result = authzRequest.isContainerInitiatedFlow();

        assertFalse("It should return false, indiciating that it is not a container initiated flow (caller initiated flow).", result);
    }

    @Test
    public void test_isContainerInitiatedFlow_attributeIsNull() {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(IS_CONTAINER_INITIATED_FLOW);
                will(returnValue(null)); // this should not happen
            }
        });

        boolean result = authzRequest.isContainerInitiatedFlow();

        assertFalse("It should return false, defaulting to not a container initiated flow (caller initiated flow).", result);
    }

}
