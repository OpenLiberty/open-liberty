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
package io.openliberty.security.oidcclientcore.userinfo;

import static org.junit.Assert.assertNotNull;
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
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.config.OidcMetadataService;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseException;
import test.common.SharedOutputManager;

public class UserInfoHandlerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    public static final String CWWKS2403E_DISCOVERY_EXCEPTION = "CWWKS2403E";
    public static final String CWWKS2405E_DISCOVERY_METADATA_MISSING_VALUE = "CWWKS2405E";
    public static final String CWWKS2418W_USERINFO_RESPONSE_ERROR = "CWWKS2418W";

    private final OidcClientConfig oidcClientConfig = mockery.mock(OidcClientConfig.class);
    private final UserInfoRequestor userInfoRequestor = mockery.mock(UserInfoRequestor.class);
    private final UserInfoResponse userInfoResponse = mockery.mock(UserInfoResponse.class);
    private final OidcMetadataService oidcMetadataService = mockery.mock(OidcMetadataService.class);

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
            UserInfoRequestor createUserInfoRequestor(String userInfoEndpoint, OidcClientConfig oidcClientConfig, String accessToken) {
                return userInfoRequestor;
            }
        };
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
    public void test_getUserInfoClaims_noUserInfoEndpoint() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(null));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(new JSONObject()));
            }
        });
        try {
            Map<String, Object> result = handler.getUserInfoClaims(oidcClientConfig, accessToken);
            fail("Should have thrown an exception but got: [" + result + "].");
        } catch (OidcDiscoveryException e) {
            verifyException(e, CWWKS2403E_DISCOVERY_EXCEPTION + ".*" + CWWKS2405E_DISCOVERY_METADATA_MISSING_VALUE + ".*" + OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT);
        }
    }

    @Test
    public void test_getUserInfoClaims_requestThrowsException() throws Exception {
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT, userInfoEndpoint);
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getProviderMetadata();
                will(returnValue(null));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
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
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
                one(userInfoRequestor).requestUserInfo();
                will(returnValue(userInfoResponse));
                one(userInfoResponse).asMap();
            }
        });
        Map<String, Object> result = handler.getUserInfoClaims(oidcClientConfig, accessToken);
        assertNotNull("Should have gotten back a non-null map.", result);
    }

}
