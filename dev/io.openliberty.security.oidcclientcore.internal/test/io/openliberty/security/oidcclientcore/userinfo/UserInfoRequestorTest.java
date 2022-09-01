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
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.exceptions.UserInfoEndpointNotHttpsException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseNot200Exception;
import io.openliberty.security.oidcclientcore.http.HttpConstants;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;
import test.common.SharedOutputManager;

public class UserInfoRequestorTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String userInfoEndpoint = "https://some-domain.com/path/userinfo";
    private static final String accessToken = "qOuZdH6Anmxclul5d71AXoDbFVmRG2dPnHn9moaw";

    private static final String sub = "testsub";
    private static final String iss = "https://superfoo";
    private static final String name = "testname";

    private static final String userInfoJSONResponseEntity = "{\"sub\":\"" + sub + "\",\"iss\":\"" + iss + "\",\"name\":\"" + name + "\"}";

    private final OidcClientHttpUtil oidcClientHttpUtil = mockery.mock(OidcClientHttpUtil.class);
    private final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);
    private final HttpResponse httpResponse = mockery.mock(HttpResponse.class);
    private final HttpGet httpGet = mockery.mock(HttpGet.class);
    private final StatusLine statusLine = mockery.mock(StatusLine.class);

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

    @Test
    public void test_requestUserInfo() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(userInfoEndpoint, accessToken, sslSocketFactory).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, sslSocketFactory, false, false);
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
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(userInfoEndpoint, accessToken, sslSocketFactory).hostnameVerification(true).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, sslSocketFactory, true, false);
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
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(userInfoEndpoint, accessToken, sslSocketFactory).useSystemPropertiesForHttpClientConnections(true).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, sslSocketFactory, false, true);
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
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(userInfoEndpoint, accessToken, sslSocketFactory).hostnameVerification(true).useSystemPropertiesForHttpClientConnections(true).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, sslSocketFactory, true, true);
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
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder("http://superfoo", accessToken, sslSocketFactory).build();

        userInfoRequestor.requestUserInfo();
    }

    @Test(expected = UserInfoResponseException.class)
    public void test_requestUserInfo_httpResponseIsNull() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(userInfoEndpoint, accessToken, sslSocketFactory).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        userInfoResponseMap.put(HttpConstants.RESPONSEMAP_CODE, null);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, sslSocketFactory, false, false);
                will(returnValue(userInfoResponseMap));
            }
        });

        userInfoRequestor.requestUserInfo();
    }

    @Test(expected = UserInfoResponseException.class)
    public void test_requestUserInfo_responseHasMalformedJSON() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(userInfoEndpoint, accessToken, sslSocketFactory).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity("notJSON", HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, sslSocketFactory, false, false);
                will(returnValue(userInfoResponseMap));
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
                one(httpResponse).getStatusLine();
                will(returnValue(statusLine));
                one(statusLine).getStatusCode();
                will(returnValue(200));
            }
        });

        userInfoRequestor.requestUserInfo();
    }

    @Test(expected = UserInfoResponseNot200Exception.class)
    public void test_requestUserInfo_responseStatusCodeNot200() throws Exception {
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(userInfoEndpoint, accessToken, sslSocketFactory).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, HttpConstants.APPLICATION_JSON);
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, sslSocketFactory, false, false);
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
        UserInfoRequestor userInfoRequestor = new UserInfoRequestor.Builder(userInfoEndpoint, accessToken, sslSocketFactory).build();
        userInfoRequestor.oidcClientHttpUtil = oidcClientHttpUtil;

        BasicHttpEntity httpEntity = createBasicHttpEntity(userInfoJSONResponseEntity, "unknownContentType");
        mockery.checking(new Expectations() {
            {
                one(oidcClientHttpUtil).getFromEndpoint(userInfoEndpoint, params, null, null, accessToken, sslSocketFactory, false, false);
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

}
