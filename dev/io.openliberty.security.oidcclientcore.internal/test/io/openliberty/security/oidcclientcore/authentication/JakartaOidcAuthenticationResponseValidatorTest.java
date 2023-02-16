/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.StateTimestampException;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import test.common.SharedOutputManager;

public class JakartaOidcAuthenticationResponseValidatorTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWWKS2407E_AUTHENTICATION_RESPONSE_ERROR = "CWWKS2407E";
    private static final String CWWKS2408E_CALLBACK_MISSING_STATE_PARAMETER = "CWWKS2408E";
    private static final String CWWKS2409E_STATE_VALUE_IN_CALLBACK_INCORRECT_LENGTH = "CWWKS2409E";
    private static final String CWWKS2410E_STATE_VALUE_IN_CALLBACK_NOT_STORED = "CWWKS2410E";
    private static final String CWWKS2412E_STATE_VALUE_IN_CALLBACK_OUTSIDE_ALLOWED_TIME_FRAME = "CWWKS2412E";
    private static final String CWWKS2413E_CALLBACK_URL_DOES_NOT_MATCH_REDIRECT_URI = "CWWKS2413E";
    private static final String CWWKS2414E_CALLBACK_URL_INCLUDES_ERROR_PARAMETER = "CWWKS2414E";

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final OidcClientConfig config = mockery.mock(OidcClientConfig.class);
    private final Cookie cookie = mockery.mock(Cookie.class);

    @Rule
    public TestName testName = new TestName();

    private String state;
    private final String clientId = "myOidcClientId";
    private final String clientSecret = "someSuperSecretValue";
    private final String requestUrl = "https://localhost/some/protected/path";
    private final String callbackUrl = "https://localhost/Callback";

    private final AuthorizationRequestUtils requestUtils = new AuthorizationRequestUtils();
    private JakartaOidcAuthenticationResponseValidator validator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        mockery.checking(new Expectations() {
            {
                one(config).isUseSession();
                will(returnValue(false));
                one(request).getMethod();
                will(returnValue("GET"));
            }
        });
        validator = new JakartaOidcAuthenticationResponseValidator(request, response, config);
        state = requestUtils.generateStateValue(request);
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
    public void test_getAndVerifyStateValue_stateParameterMissing() {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(AuthorizationRequestParameters.STATE);
                will(returnValue(null));
                one(config).getClientId();
                will(returnValue(clientId));
            }
        });
        try {
            String result = validator.getAndVerifyStateValue();
            fail("Should have thrown an exception but didn't. Method returned [" + result + "].");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2407E_AUTHENTICATION_RESPONSE_ERROR + ".+" + CWWKS2408E_CALLBACK_MISSING_STATE_PARAMETER);
        }
    }

    @Test
    public void test_getAndVerifyStateValue_stateStringTooShort() {
        String stateParameter = "short";
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(AuthorizationRequestParameters.STATE);
                will(returnValue(stateParameter));
                one(config).getClientSecret();
                will(returnValue(new ProtectedString(clientSecret.toCharArray())));
                allowing(config).getClientId();
                will(returnValue(clientId));
            }
        });
        try {
            String result = validator.getAndVerifyStateValue();
            fail("Should have thrown an exception but didn't. Method returned [" + result + "].");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2407E_AUTHENTICATION_RESPONSE_ERROR + ".+" + CWWKS2409E_STATE_VALUE_IN_CALLBACK_INCORRECT_LENGTH);
        }
    }

    @Test
    public void test_getAndVerifyStateValue_stateIsOld() {
        // Set the timestamp embedded in the stored state to a time significantly in the past
        state = state.replaceFirst("[0-9]{5}", "00000");
        String storageLookupKey = OidcStorageUtils.getStateStorageKey(state);
        String storageValue = OidcStorageUtils.createStateStorageValue(state, clientSecret);
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(AuthorizationRequestParameters.STATE);
                will(returnValue(state));
                one(config).getClientSecret();
                will(returnValue(new ProtectedString(clientSecret.toCharArray())));
                allowing(config).getClientId();
                will(returnValue(clientId));
                one(request).getCookies();
                will(returnValue(new Cookie[] { cookie }));
                one(cookie).getName();
                will(returnValue(storageLookupKey));
                one(cookie).getValue();
                will(returnValue(storageValue));
            }
        });
        try {
            String result = validator.getAndVerifyStateValue();
            fail("Should have thrown an exception but didn't. Method returned [" + result + "].");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2407E_AUTHENTICATION_RESPONSE_ERROR + ".+" + CWWKS2412E_STATE_VALUE_IN_CALLBACK_OUTSIDE_ALLOWED_TIME_FRAME);
        }
    }

    @Test
    public void test_getAndVerifyStateValue_stateMatches() {
        String storageLookupKey = OidcStorageUtils.getStateStorageKey(state);
        String storageValue = OidcStorageUtils.createStateStorageValue(state, clientSecret);
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(AuthorizationRequestParameters.STATE);
                will(returnValue(state));
                one(config).getClientSecret();
                will(returnValue(new ProtectedString(clientSecret.toCharArray())));
                allowing(config).getClientId();
                will(returnValue(clientId));
                one(request).getCookies();
                will(returnValue(new Cookie[] { cookie }));
                one(cookie).getName();
                will(returnValue(storageLookupKey));
                one(cookie).getValue();
                will(returnValue(storageValue));
            }
        });
        try {
            String result = validator.getAndVerifyStateValue();
            assertEquals(state, result);
        } catch (AuthenticationResponseException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_getStoredStateValue_stateNotFound() {
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(new Cookie[0]));
                one(config).getClientId();
                will(returnValue(clientId));
            }
        });
        try {
            String result = validator.getStoredStateValue(state);
            fail("Should have thrown an exception but didn't. Method returned [" + result + "].");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2410E_STATE_VALUE_IN_CALLBACK_NOT_STORED);
        }
    }

    @Test
    public void test_getStoredStateValue_stateFound() {
        String storageLookupKey = OidcStorageUtils.getStateStorageKey(state);
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(new Cookie[] { cookie }));
                one(cookie).getName();
                will(returnValue(storageLookupKey));
                one(cookie).getValue();
                will(returnValue(state));
            }
        });
        try {
            String result = validator.getStoredStateValue(state);
            assertEquals(state, result);
        } catch (AuthenticationResponseException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_verifyStateTimestampWithinClockSkew_responseStateFromPast_withinAllowableTime() {
        long clockSkew = 0;
        long allowHandleTime = 10;

        long now = System.currentTimeMillis();
        long responseStateTime = now - (2 * 1000);

        String responseState = "00" + responseStateTime + state;
        try {
            validator.verifyStateTimestampWithinClockSkew(responseState, clockSkew, allowHandleTime);
        } catch (StateTimestampException e) {
            fail("Response state [" + responseStateTime + "] should have been considered within the allowable upper limit [" + allowHandleTime
                 + "] of current(ish) time [" + System.currentTimeMillis() + "]. Clock skew was [" + clockSkew + "]. Exception was: " + e);
        }
    }

    @Test
    public void test_verifyStateTimestampWithinClockSkew_responseStateFromPast_outsideAllowableTime() {
        long clockSkew = 0;
        long allowHandleTime = 2;

        long now = System.currentTimeMillis();
        long responseStateTime = now - (10 * 1000);

        String responseState = "00" + responseStateTime + state;
        try {
            validator.verifyStateTimestampWithinClockSkew(responseState, clockSkew, allowHandleTime);
            fail("Response state [" + responseStateTime + "] should have been considered outside the allowable upper limit [" + allowHandleTime
                 + "] of current(ish) time [" + System.currentTimeMillis() + "]. Clock skew was [" + clockSkew + "].");
        } catch (StateTimestampException e) {
            verifyException(e, CWWKS2412E_STATE_VALUE_IN_CALLBACK_OUTSIDE_ALLOWED_TIME_FRAME);
        }
    }

    @Test
    public void test_verifyStateTimestampWithinClockSkew_responseStateFromFuture_withinClockSkew() {
        long clockSkew = 10;
        long allowHandleTime = 0;

        long now = System.currentTimeMillis();
        long responseStateTime = now + (2 * 1000);

        String responseState = "00" + responseStateTime + state;
        try {
            validator.verifyStateTimestampWithinClockSkew(responseState, clockSkew, allowHandleTime);
        } catch (StateTimestampException e) {
            fail("Response state [" + responseStateTime + "] should have been considered within the clock skew [" + clockSkew + "] of current(ish) time ["
                 + System.currentTimeMillis() + "]. Allowable upper limit was [" + allowHandleTime + "]. Exception was: " + e);
        }
    }

    @Test
    public void test_verifyStateTimestampWithinClockSkew_responseStateFromFuture_outsideClockSkew() {
        long clockSkew = 2;
        long allowHandleTime = 0;

        long now = System.currentTimeMillis();
        long responseStateTime = now + (10 * 1000);

        String responseState = "00" + responseStateTime + state;
        try {
            validator.verifyStateTimestampWithinClockSkew(responseState, clockSkew, allowHandleTime);
            fail("Response state [" + responseStateTime + "] should have been considered outside the clock skew [" + clockSkew + "] of current(ish) time ["
                 + System.currentTimeMillis() + "]. Allowable upper limit was [" + allowHandleTime + "].");
        } catch (StateTimestampException e) {
            verifyException(e, CWWKS2412E_STATE_VALUE_IN_CALLBACK_OUTSIDE_ALLOWED_TIME_FRAME);
        }
    }

    @Test
    public void test_checkRequestAgainstRedirectUri_isRedirectToOriginalResource_originalReqUrlMissing() {
        mockery.checking(new Expectations() {
            {
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(requestUrl)));
                one(config).getRedirectURI();
                will(returnValue(callbackUrl));
                one(config).isRedirectToOriginalResource();
                will(returnValue(true));
                one(request).getCookies();
                will(returnValue(new Cookie[0]));
                allowing(config).getClientId();
                will(returnValue(clientId));
            }
        });
        try {
            validator.checkRequestAgainstRedirectUri(state);
            fail("Should have thrown an exception but didn't.");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2413E_CALLBACK_URL_DOES_NOT_MATCH_REDIRECT_URI);
        }
    }

    @Test
    public void test_checkRequestAgainstRedirectUri_isRedirectToOriginalResource_urlDoesNotMatchStoredValue() {
        String storageLookupKey = OidcStorageUtils.getOriginalReqUrlStorageKey(state);
        mockery.checking(new Expectations() {
            {
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(requestUrl)));
                one(config).getRedirectURI();
                will(returnValue(callbackUrl));
                one(config).isRedirectToOriginalResource();
                will(returnValue(true));
                one(request).getCookies();
                will(returnValue(new Cookie[] { cookie }));
                one(cookie).getName();
                will(returnValue(storageLookupKey));
                one(cookie).getValue();
                will(returnValue("Some other state value"));
                allowing(config).getClientId();
                will(returnValue(clientId));
            }
        });
        try {
            validator.checkRequestAgainstRedirectUri(state);
            fail("Should have thrown an exception but didn't.");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2413E_CALLBACK_URL_DOES_NOT_MATCH_REDIRECT_URI);
        }
    }

    @Test
    public void test_checkRequestAgainstRedirectUri_isRedirectToOriginalResource_urlMatches() {
        String storageLookupKey = OidcStorageUtils.getOriginalReqUrlStorageKey(state);
        mockery.checking(new Expectations() {
            {
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(requestUrl)));
                one(config).getRedirectURI();
                will(returnValue(callbackUrl));
                one(config).isRedirectToOriginalResource();
                will(returnValue(true));
                one(request).getCookies();
                will(returnValue(new Cookie[] { cookie }));
                one(cookie).getName();
                will(returnValue(storageLookupKey));
                one(cookie).getValue();
                will(returnValue(requestUrl));
            }
        });
        try {
            validator.checkRequestAgainstRedirectUri(state);
        } catch (AuthenticationResponseException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_checkRequestAgainstRedirectUri_isRedirectToOriginalResource_comparesUrlsWithoutTheirQueryParams() {
        String storageLookupKey = OidcStorageUtils.getOriginalReqUrlStorageKey(state);
        mockery.checking(new Expectations() {
            {
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(requestUrl)));
                one(config).getRedirectURI();
                will(returnValue(requestUrl));
                one(config).isRedirectToOriginalResource();
                will(returnValue(true));
                one(request).getCookies();
                will(returnValue(new Cookie[] { cookie }));
                one(cookie).getName();
                will(returnValue(storageLookupKey));
                one(cookie).getValue();
                will(returnValue(requestUrl + "?type=test"));
            }
        });
        try {
            validator.checkRequestAgainstRedirectUri(state);
        } catch (AuthenticationResponseException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_checkRequestAgainstRedirectUri_urlDoesNotMatchConfiguredValue() {
        final String configuredUri = "https://localhost/some/other/path";
        mockery.checking(new Expectations() {
            {
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(requestUrl)));
                one(config).getRedirectURI();
                will(returnValue(configuredUri));
                one(config).isRedirectToOriginalResource();
                will(returnValue(false));
                allowing(config).getClientId();
                will(returnValue(clientId));
            }
        });
        try {
            validator.checkRequestAgainstRedirectUri(state);
            fail("Should have thrown an exception but didn't.");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2413E_CALLBACK_URL_DOES_NOT_MATCH_REDIRECT_URI);
        }
    }

    @Test
    public void test_checkRequestAgainstRedirectUri_urlMatchesConfiguredValue() {
        mockery.checking(new Expectations() {
            {
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(requestUrl)));
                one(config).getRedirectURI();
                will(returnValue(requestUrl));
            }
        });
        try {
            validator.checkRequestAgainstRedirectUri(state);
        } catch (AuthenticationResponseException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_checkForErrorParameter_noParameter() {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter("error");
                will(returnValue(null));
            }
        });
        try {
            validator.checkForErrorParameter();
        } catch (AuthenticationResponseException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_checkForErrorParameter_parameterIsEmptyString() {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter("error");
                will(returnValue(""));
                one(config).getClientId();
                will(returnValue(clientId));
            }
        });
        try {
            validator.checkForErrorParameter();
            fail("Should have thrown an exception but didn't.");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2414E_CALLBACK_URL_INCLUDES_ERROR_PARAMETER);
        }
    }

    @Test
    public void test_checkForErrorParameter_invalidClient() {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter("error");
                will(returnValue("invalid_client"));
                one(config).getClientId();
                will(returnValue(clientId));
            }
        });
        try {
            validator.checkForErrorParameter();
            fail("Should have thrown an exception but didn't.");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2414E_CALLBACK_URL_INCLUDES_ERROR_PARAMETER + ".*" + "invalid_client");
        }
    }

}
