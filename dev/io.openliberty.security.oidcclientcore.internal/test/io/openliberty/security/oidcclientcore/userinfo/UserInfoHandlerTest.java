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
package io.openliberty.security.oidcclientcore.userinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.regex.Pattern;

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
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseException;
import test.common.SharedOutputManager;

public class UserInfoHandlerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    public static final String CWWKS2418W_USERINFO_RESPONSE_ERROR = "CWWKS2418W";

    private final OidcClientConfig oidcClientConfig = mockery.mock(OidcClientConfig.class);
    private final OidcProviderMetadata providerMetadata = mockery.mock(OidcProviderMetadata.class);
    private final DiscoveryHandler discoveryHandler = mockery.mock(DiscoveryHandler.class);
    private final UserInfoRequestor userInfoRequestor = mockery.mock(UserInfoRequestor.class);
    private final UserInfoResponse userInfoResponse = mockery.mock(UserInfoResponse.class);

    private final String clientId = "myClientId";
    private final String discoveryUrl = "https://localhost/OP/" + OidcDiscoveryConstants.WELL_KNOWN_SUFFIX;
    private final String userInfoEndpoint = "https://localhost/OP/userinfo";
    private final String accessToken = "qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw";

    UserInfoHandler handler;

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
        handler = new UserInfoHandler() {
            @Override
            public DiscoveryHandler getDiscoveryHandler() {
                return discoveryHandler;
            }

            @Override
            UserInfoRequestor createUserInfoRequestor(String userInfoEndpoint, String accessToken) {
                return userInfoRequestor;
            }
        };
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
    public void test_getUserInfoClaims_noUserInfoEndpoint() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(null));
                one(discoveryHandler).fetchDiscoveryDataJson(discoveryUrl, clientId);
                will(returnValue(new JSONObject()));
            }
        });
        Map<String, Object> result = handler.getUserInfoClaims(oidcClientConfig, accessToken);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getUserInfoClaims_requestThrowsException() throws Exception {
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT, userInfoEndpoint);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(null));
                one(discoveryHandler).fetchDiscoveryDataJson(discoveryUrl, clientId);
                will(returnValue(discoveryData));
                one(userInfoRequestor).requestUserInfo();
                will(throwException(new UserInfoResponseException(userInfoEndpoint, new Exception(defaultExceptionMsg))));
            }
        });
        try {
            Map<String, Object> result = handler.getUserInfoClaims(oidcClientConfig, accessToken);
            fail("Should have thrown an exception but got: [" + result + "].");
        } catch (UserInfoResponseException e) {
            verifyException(e, CWWKS2418W_USERINFO_RESPONSE_ERROR + ".+" + Pattern.quote(defaultExceptionMsg));
        }
    }

    @Test
    public void test_getUserInfoClaims() throws Exception {
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT, userInfoEndpoint);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(null));
                one(discoveryHandler).fetchDiscoveryDataJson(discoveryUrl, clientId);
                will(returnValue(discoveryData));
                one(userInfoRequestor).requestUserInfo();
                will(returnValue(userInfoResponse));
                one(userInfoResponse).asMap();
            }
        });
        Map<String, Object> result = handler.getUserInfoClaims(oidcClientConfig, accessToken);
        assertNotNull("Should have gotten back a non-null map.", result);
    }

    @Test
    public void test_getUserInfoEndpointFromProviderMetadata_noProviderMetadata() {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(null));
            }
        });
        String result = handler.getUserInfoEndpointFromProviderMetadata(oidcClientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getUserInfoEndpointFromProviderMetadata_emptyValue() {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getUserinfoEndpoint();
                will(returnValue(""));
            }
        });
        String result = handler.getUserInfoEndpointFromProviderMetadata(oidcClientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getUserInfoEndpointFromProviderMetadata() {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(providerMetadata).getUserinfoEndpoint();
                will(returnValue(userInfoEndpoint));
            }
        });
        String result = handler.getUserInfoEndpointFromProviderMetadata(oidcClientConfig);
        assertEquals(userInfoEndpoint, result);
    }

    @Test
    public void test_getUserInfoEndpointFromDiscoveryMetadata_missingUserInfoEndpoint() throws Exception {
        JSONObject discoveryData = new JSONObject();
        mockery.checking(new Expectations() {
            {
                one(discoveryHandler).fetchDiscoveryDataJson(discoveryUrl, clientId);
                will(returnValue(discoveryData));
            }
        });
        String result = handler.getUserInfoEndpointFromDiscoveryMetadata(oidcClientConfig);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getUserInfoEndpointFromDiscoveryMetadata() throws Exception {
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT, userInfoEndpoint);
        mockery.checking(new Expectations() {
            {
                one(discoveryHandler).fetchDiscoveryDataJson(discoveryUrl, clientId);
                will(returnValue(discoveryData));
            }
        });
        String result = handler.getUserInfoEndpointFromDiscoveryMetadata(oidcClientConfig);
        assertEquals(userInfoEndpoint, result);
    }

}
