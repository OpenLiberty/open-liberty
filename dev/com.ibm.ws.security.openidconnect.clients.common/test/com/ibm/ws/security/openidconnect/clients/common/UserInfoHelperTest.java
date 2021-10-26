/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Key;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class UserInfoHelperTest extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final ConvergedClientConfig convClientConfig = mock.mock(ConvergedClientConfig.class, "convClientConfig");
    private final SSLSupport sslSupport = mock.mock(SSLSupport.class, "sslSupport");
    private final HttpEntity httpEntity = mock.mock(HttpEntity.class);
    private final HttpResponse httpResponse = mock.mock(HttpResponse.class, "httpResponse");
    private final OidcClientConfig clientConfig = mock.mock(OidcClientConfig.class, "clientConfig");
    private final OidcClientRequest clientRequest = mock.mock(OidcClientRequest.class, "clientRequest");
    private final Key decryptionKey = mock.mock(Key.class);

    private final ReferrerURLCookieHandler referrerURLCookieHandler = new ReferrerURLCookieHandler(webAppSecConfig);

    private static final String USERINFO = "{\"sub\":\"testuser\",\"iss\":\"https:superfoo\",\"name\":\"testuser\"}";

    class UserInfoHelperMock extends UserInfoHelper {
        String mockURLResponse = USERINFO;
        String mockUpdateResult = null;
        boolean updateAuthResultCalled = false;

        public UserInfoHelperMock(ConvergedClientConfig config, SSLSupport sslSupport) {
            super(config, sslSupport);
        }

        @Override
        protected String getUserInfoFromURL(ConvergedClientConfig config, SSLSocketFactory sslsf, String accessToken, OidcClientRequest oidcClientRequest) {
            return mockURLResponse;
        }

        @Override
        protected void updateAuthenticationResultPropertiesWithUserInfo(ProviderAuthenticationResult oidcResult, String userInfoStr) {
            mockUpdateResult = userInfoStr;
            updateAuthResultCalled = true;
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig); // for MockOidcClientRequest
    }

    @After
    public void after() {
        outputMgr.resetStreams();
        mock.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    void setExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(convClientConfig).getUserInfoEndpointUrl();
                will(returnValue("somebogusurl"));
                allowing(convClientConfig).isUserInfoEnabled();
                will(returnValue(true));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));
            }
        });
    }

    @Test
    public void testRetrieveValidInfo() {
        setExpectations();
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        boolean result = uihm.getUserInfo(null, null, "accesstoken", "testuser", new MockOidcClientRequest(referrerURLCookieHandler));
        assertTrue("method return should be true", result);
        assertTrue("userinfo result" + uihm.mockUpdateResult + " did not match expected value: " + uihm.mockURLResponse,
                uihm.mockUpdateResult.equals(uihm.mockURLResponse));
    }

    /**
     * retrieve userinfo where subject from id token doesn't match what's in userinfo.
     * userinfo should be ignored, updateAuthResult should not be called
     */
    @Test
    public void testRetrieveInvalidInfo() {
        setExpectations();
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        // put the wrong sub in the userinfo, should not get the userinfo back.
        uihm.mockURLResponse = uihm.mockURLResponse.replace("testuser", "bogususer");
        boolean result = uihm.getUserInfo(null, null, "accesstoken", "testuser", new MockOidcClientRequest(referrerURLCookieHandler));
        assertFalse("method return should be false", result);
        assertFalse("updateAuthResult should not have been called",
                uihm.updateAuthResultCalled);
    }

    /**
     * retrieve userinfo where no subject from id token is present.
     * userinfo should be ignored, since id token subject and userinfo subject don't match.
     */
    @Test
    public void testRetrieveNoSujbectFromIdToken() {
        setExpectations();
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        boolean result = uihm.getUserInfo(null, null, "accesstoken", null, new MockOidcClientRequest(referrerURLCookieHandler));
        assertFalse("method return should be false", result);
        assertFalse("updateAuthResult should not have been called",
                uihm.updateAuthResultCalled);
    }

    @Test
    public void testGetUserInfoSubClaim() {
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        String result = uihm.getUserInfoSubClaim(USERINFO);
        assertTrue("should have extracted testuser sub from userinfo but instead got: " + result, result.equals("testuser"));
    }

    @Test
    public void testWillRetrieveUserInfoNeg1() {
        mock.checking(new Expectations() {
            {
                allowing(convClientConfig).getUserInfoEndpointUrl();
                will(returnValue(null));
                allowing(convClientConfig).isUserInfoEnabled();
                will(returnValue(true));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        assertFalse("willRetreive should be false", uihm.willRetrieveUserInfo());
    }

    @Test
    public void testWillRetrieveUserInfoNeg2() {
        mock.checking(new Expectations() {
            {
                allowing(convClientConfig).getUserInfoEndpointUrl();
                will(returnValue("http://somebogusthing"));
                allowing(convClientConfig).isUserInfoEnabled();
                will(returnValue(false));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        assertFalse("willRetreive should be false", uihm.willRetrieveUserInfo());

    }

    @Test
    public void testExtractClaimsFromResponse_responseMissingEntity() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(null));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        String result = uihm.extractClaimsFromResponse(httpResponse, clientConfig, clientRequest);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void testExtractClaimsFromResponse_emptyString() throws Exception {
        final InputStream input = new ByteArrayInputStream(("").getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");
        mock.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        String result = uihm.extractClaimsFromResponse(httpResponse, clientConfig, clientRequest);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void testExtractClaimsFromResponse_missingContentType() throws Exception {
        String inputString = "This is not JSON";
        final InputStream input = new ByteArrayInputStream(inputString.getBytes());
        mock.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(httpEntity));
                one(httpEntity).getContent();
                will(returnValue(input));
                allowing(httpEntity).getContentLength();
                will(returnValue((long) inputString.length()));
                allowing(httpEntity).getContentType();
                will(returnValue(null));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        String result = uihm.extractClaimsFromResponse(httpResponse, clientConfig, clientRequest);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void testExtractClaimsFromResponse_notJson() throws Exception {
        final InputStream input = new ByteArrayInputStream(("This is not JSON").getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("text/plain");
        mock.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        String result = uihm.extractClaimsFromResponse(httpResponse, clientConfig, clientRequest);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void testExtractClaimsFromResponse_emptyJson() throws Exception {
        JSONObject responseJson = new JSONObject();
        String responseStr = new String(responseJson.toString());
        final InputStream input = new ByteArrayInputStream(responseStr.getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");
        mock.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        String result = uihm.extractClaimsFromResponse(httpResponse, clientConfig, clientRequest);
        assertNotNull("Result should not have been null but was.", result);
        assertTrue("Result should have been empty, but was " + result + ".", result.equals(responseStr));
    }

    @Test
    public void testExtractClaimsFromResponse_validNonEmptyJson() throws Exception {
        JSONObject responseJson = new JSONObject();
        responseJson.put("key1", "value1");
        responseJson.put("key2", "value2");
        String responseStr = new String(responseJson.toString());
        final InputStream input = new ByteArrayInputStream(responseStr.getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/json");
        mock.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        String result = uihm.extractClaimsFromResponse(httpResponse, clientConfig, clientRequest);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Result did not match the expected value.", responseStr, result);
    }

    @Test
    public void testExtractClaimsFromResponse_validNonEmptyJson_contentTypeJwt() throws Exception {
        JSONObject responseJson = new JSONObject();
        responseJson.put("key1", "value1");
        responseJson.put("key2", "value2");
        final InputStream input = new ByteArrayInputStream(new String(responseJson.toString()).getBytes());
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentType("application/jwt");
        mock.checking(new Expectations() {
            {
                one(httpResponse).getEntity();
                will(returnValue(entity));
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        try {
            String result = uihm.extractClaimsFromResponse(httpResponse, clientConfig, clientRequest);
            fail("Should have thrown an exception, but got [" + result + "].");
        } catch (Exception e) {
            verifyException(e, "CWWKS1533E" + ".+" + "CWWKS1539E");
        }
    }

    @Test
    public void testExtractClaimsFromJwtResponse_responseStringEmpty() throws Exception {
        String rawResponse = "";

        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        String result = uihm.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
        assertNull("Result should have been null but was " + result + ".", result);
    }

    @Test
    public void testExtractClaimsFromJwtResponse_notJwt() throws Exception {
        String rawResponse = "This is not in JWT format";
        mock.checking(new Expectations() {
            {
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });

        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        try {
            String result = uihm.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
            fail("Should have thrown an exception, but got [" + result + "].");
        } catch (Exception e) {
            verifyException(e, "CWWKS1533E" + ".+" + "CWWKS1539E");
        }
    }

    @Test
    public void testExtractClaimsFromJwtResponse_jwsMalformed() throws Exception {
        String rawResponse = "aaa.bbb.ccc";
        mock.checking(new Expectations() {
            {
                one(clientConfig).getId();
                will(returnValue("configId"));
            }
        });

        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        try {
            String result = uihm.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
            fail("Should have thrown an exception, but got [" + result + "].");
        } catch (Exception e) {
            verifyException(e, "CWWKS1533E");
        }
    }

    @Test
    public void testExtractClaimsFromJwtResponse_jweMalformed() throws Exception {
        String rawResponse = "";
        for (int i = 1; i <= 4; i++) {
            rawResponse += Base64Coder.base64Encode("part" + i) + ".";
        }
        rawResponse += Base64Coder.base64Encode("part" + 5);

        mock.checking(new Expectations() {
            {
                one(clientConfig).getJweDecryptionKey();
                will(returnValue(decryptionKey));
                allowing(clientConfig).getId();
                will(returnValue("configId"));
            }
        });
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig, sslSupport);
        try {
            String result = uihm.extractClaimsFromJwtResponse(rawResponse, clientConfig, clientRequest);
            fail("Should have thrown an exception, but got [" + result + "].");
        } catch (Exception e) {
            verifyException(e, "CWWKS1533E" + ".+" + "CWWKS6056E");
        }
    }
}
