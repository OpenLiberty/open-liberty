/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.userinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.jmock.Expectations;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.security.test.common.jwt.utils.JwtUnitTestUtils;

import io.openliberty.security.common.jwt.exceptions.SignatureAlgorithmNotInAllowedList;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.config.OidcMetadataService;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoEndpointNotHttpsException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseNot200Exception;
import io.openliberty.security.oidcclientcore.http.HttpConstants;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;
import test.common.SharedOutputManager;

public class UserInfoRequestorTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String clientId = "myOidcClientId";
    private static final String clientSecretString = "secret";
    private static final ProtectedString clientSecret = new ProtectedString(clientSecretString.toCharArray());
    private static final String userInfoEndpoint = "https://localhost/path/userinfo";
    private static final String jwksUri = "https://localhost/path/jwk";
    private static final String accessToken = "qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw";

    private static final String sub = "testsub";
    private static final String iss = "https://superfoo";
    private static final String name = "testname";

    private static final String userInfoJSONResponseEntity = "{\"sub\":\"" + sub + "\",\"iss\":\"" + iss + "\",\"name\":\"" + name + "\"}";

    private final OidcClientConfig oidcClientConfig = mockery.mock(OidcClientConfig.class);
    private final OidcClientHttpUtil oidcClientHttpUtil = mockery.mock(OidcClientHttpUtil.class);
    private final HttpResponse httpResponse = mockery.mock(HttpResponse.class);
    private final HttpGet httpGet = mockery.mock(HttpGet.class);
    private final StatusLine statusLine = mockery.mock(StatusLine.class);
    private final OidcProviderMetadata providerMetadata = mockery.mock(OidcProviderMetadata.class);
    private final OidcMetadataService oidcMetadataService = mockery.mock(OidcMetadataService.class);

    private List<NameValuePair> params;
    private Map<String, Object> userInfoResponseMap;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        params = new ArrayList<NameValuePair>();

        userInfoResponseMap = new HashMap<String, Object>();
        userInfoResponseMap.put(HttpConstants.RESPONSEMAP_CODE, httpResponse);
        userInfoResponseMap.put(HttpConstants.RESPONSEMAP_METHOD, httpGet);

        MetadataUtils metadataUtils = new MetadataUtils();
        metadataUtils.setOidcMetadataService(oidcMetadataService);
    }

    @After
    public void tearDown() {
        MetadataUtils metadataUtils = new MetadataUtils();
        metadataUtils.unsetOidcMetadataService(oidcMetadataService);

        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_requestUserInfo() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, null, false, false);
                will(returnValue(userInfoResponseMap));
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(statusLine).getStatusCode();
                will(returnValue(200));
            }
        });

        UserInfoResponse userInfoResponse = userInfoRequestor.requestUserInfo();

        verifyUserInfoResponse(userInfoResponse);
    }

    public void test_requestUserInfo_withHostnameVerification() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).hostnameVerification(true).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, null, true, false);
                will(returnValue(userInfoResponseMap));
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(statusLine).getStatusCode();
                will(returnValue(200));
            }
        });

        UserInfoResponse userInfoResponse = userInfoRequestor.requestUserInfo();

        verifyUserInfoResponse(userInfoResponse);
    }

    public void test_requestUserInfo_useSystemPropertiesForHttpClientConnections() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).useSystemPropertiesForHttpClientConnections(true).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, null, false, true);
                will(returnValue(userInfoResponseMap));
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(statusLine).getStatusCode();
                will(returnValue(200));
            }
        });

        UserInfoResponse userInfoResponse = userInfoRequestor.requestUserInfo();

        verifyUserInfoResponse(userInfoResponse);
    }

    public void test_requestUserInfo_withHostnameVerificationAndUseSystemPropertiesForHttpClientConnections() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).hostnameVerification(true).useSystemPropertiesForHttpClientConnections(true).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, null, true, true);
                will(returnValue(userInfoResponseMap));
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(statusLine).getStatusCode();
                will(returnValue(200));
            }
        });

        UserInfoResponse userInfoResponse = userInfoRequestor.requestUserInfo();

        verifyUserInfoResponse(userInfoResponse);
    }

    @Test(expected = UserInfoEndpointNotHttpsException.class)
    public void test_requestUserInfo_urlNotHTTPS() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, "http://superfoo", accessToken).build();
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getClientId();
                will(returnValue(clientId));
            }
        });

        userInfoRequestor.requestUserInfo();
    }

    @Test(expected = UserInfoResponseException.class)
    public void test_requestUserInfo_httpResponseIsNull() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        userInfoResponseMap.put(HttpConstants.RESPONSEMAP_CODE, null);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, null, false, false);
                will(returnValue(userInfoResponseMap));
            }
        });

        userInfoRequestor.requestUserInfo();
    }

    @Test(expected = UserInfoResponseException.class)
    public void test_requestUserInfo_responseHasMalformedJSON() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity("notJSON", HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, null, false, false);
                will(returnValue(userInfoResponseMap));
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
            }
        });

        userInfoRequestor.requestUserInfo();
    }

    @Test(expected = UserInfoResponseNot200Exception.class)
    public void test_requestUserInfo_responseStatusCodeNot200() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, null, false, false);
                will(returnValue(userInfoResponseMap));
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(statusLine).getStatusCode();
                will(returnValue(500));
            }
        });

        userInfoRequestor.requestUserInfo();
    }

    @Test
    public void test_requestUserInfo_contentTypeUnknown() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, "unknownContentType");
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, null, false, false);
                will(returnValue(userInfoResponseMap));
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(statusLine).getStatusCode();
                will(returnValue(200));
            }
        });

        UserInfoResponse userInfoResponse = userInfoRequestor.requestUserInfo();

        assertNull("Expected UserInfoResponse map to be null since we don't know how to process the content type.", userInfoResponse.asMap());
    }

    @Test(expected = InvalidJwtException.class)
    public void test_extractClaimsFromJwtResponse_responseIsEmptyString() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();

        String jwtResponse = "";

        JSONObject claims = userInfoRequestor.extractClaimsFromJwtResponse(jwtResponse);
        fail("Should have thrown an exception but got: " + claims);
    }

    @Test(expected = InvalidJwtException.class)
    public void test_extractClaimsFromJwtResponse_responseIsJson() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();

        String jwtResponse = "{\"key\":\"value\"}";

        JSONObject claims = userInfoRequestor.extractClaimsFromJwtResponse(jwtResponse);
        fail("Should have thrown an exception but got: " + claims);
    }

    @Test
    public void test_extractClaimsFromJwtResponse_emptyClaims() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_USER_INFO_SIGNING_ALG_VALUES_SUPPORTED, getUserInfoSigningAlgsSupported("HS256"));
        mockery.checking(new Expectations() {
            {
                allowing(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(oidcClientConfig).getClientId();
                will(returnValue(clientId));
                one(oidcClientConfig).getClientSecret();
                will(returnValue(clientSecret));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });

        JSONObject claims = new JSONObject();
        String jwtResponse = JwtUnitTestUtils.getHS256Jws(claims, clientSecretString);

        JSONObject extractedClaims = userInfoRequestor.extractClaimsFromJwtResponse(jwtResponse);
        assertNotNull("Claims should not have been null but were.", extractedClaims);
        assertTrue("Claims set should have been empty but was: " + extractedClaims, extractedClaims.isEmpty());
    }

    @Test
    public void test_extractClaimsFromJwtResponse_signatureAlgorithmNotAllowed() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_USER_INFO_SIGNING_ALG_VALUES_SUPPORTED, getUserInfoSigningAlgsSupported("RS256"));
        mockery.checking(new Expectations() {
            {
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });

        JSONObject claims = new JSONObject();
        String jwtResponse = JwtUnitTestUtils.getHS256Jws(claims, clientSecretString);

        try {
            JSONObject extractedClaims = userInfoRequestor.extractClaimsFromJwtResponse(jwtResponse);
            fail("Should have thrown an exception but got claims: " + extractedClaims);
        } catch (SignatureAlgorithmNotInAllowedList e) {
            verifyException(e, "CWWKS2520E");
        }
    }

    @Test
    public void test_extractClaimsFromJwtResponse_withClaims() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken).build();
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_USER_INFO_SIGNING_ALG_VALUES_SUPPORTED, getUserInfoSigningAlgsSupported("HS256"));
        mockery.checking(new Expectations() {
            {
                allowing(oidcClientConfig).getProviderMetadata();
                will(returnValue(providerMetadata));
                one(oidcClientConfig).getClientId();
                will(returnValue(clientId));
                one(oidcClientConfig).getClientSecret();
                will(returnValue(clientSecret));
                one(oidcMetadataService).getProviderDiscoveryMetadata(oidcClientConfig);
                will(returnValue(discoveryData));
            }
        });

        JSONObject claims = new JSONObject();
        claims.put("sub", "testuser");
        claims.put("iss", "https://localhost/op");
        claims.put("access_token", "xxx.yyy.zzz");
        claims.put("iat", (System.currentTimeMillis() / 1000) - 10);
        claims.put("exp", (System.currentTimeMillis() / 1000) + 10);
        JSONArray groups = new JSONArray();
        groups.add("group1");
        groups.add("group2");
        groups.add("group3");
        claims.put("groupIds", groups);
        String jwtResponse = JwtUnitTestUtils.getHS256Jws(claims.toString(), clientSecretString);

        JSONObject extractedClaims = userInfoRequestor.extractClaimsFromJwtResponse(jwtResponse);
        assertNotNull("Claims should not have been null but were.", extractedClaims);
        assertEquals(claims, extractedClaims);
    }

    private BasicHttpEntity createBasicHttpEntity(String string, String contentType) {
        BasicHttpEntity entity = createBasicHttpEntity(string);
        entity.setContentType(contentType);
        return entity;
    }

    private BasicHttpEntity createBasicHttpEntity(String string) {
        InputStream input = new ByteArrayInputStream(string.getBytes());
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        return entity;
    }

    private void verifyUserInfoResponse(UserInfoResponse userInfoResponse) {
        Map<String, Object> claims = userInfoResponse.asMap();
        assertEquals("Expected sub claim to be " + claims.get("sub") + ", but was " + sub + ".", claims.get("sub"), sub);
        assertEquals("Expected iss claim to be " + claims.get("iss") + ", but was " + iss + ".", claims.get("iss"), iss);
        assertEquals("Expected name claim to be " + claims.get("name") + ", but was " + name + ".", claims.get("name"), name);
    }

    private JSONArray getUserInfoSigningAlgsSupported(String... algs) {
        JSONArray algsSupported = new JSONArray();
        for (String alg : algs) {
            algsSupported.add(alg);
        }
        return algsSupported;
    }

}
