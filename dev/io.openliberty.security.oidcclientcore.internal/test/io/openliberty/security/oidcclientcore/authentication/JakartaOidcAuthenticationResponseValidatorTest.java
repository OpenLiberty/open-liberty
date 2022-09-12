/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import static org.junit.Assert.fail;

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

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.StateTimestampException;
import test.common.SharedOutputManager;

public class JakartaOidcAuthenticationResponseValidatorTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWWKS2407E_AUTHENTICATION_RESPONSE_ERROR = "CWWKS2407E";
    private static final String CWWKS2409E_STATE_VALUE_IN_CALLBACK_INCORRECT_LENGTH = "CWWKS2409E";
    private static final String CWWKS2412E_STATE_VALUE_IN_CALLBACK_OUTSIDE_ALLOWED_TIME_FRAME = "CWWKS2412E";

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final OidcClientConfig config = mockery.mock(OidcClientConfig.class);

    @Rule
    public TestName testName = new TestName();

    private String state;
    private final String clientId = "myOidcClientId";
    private final String clientSecret = "someSuperSecretValue";

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
                will(returnValue(true));
            }
        });
        validator = new JakartaOidcAuthenticationResponseValidator(request, response, config);
        state = RandomUtils.getRandomAlphaNumeric(AuthorizationRequestUtils.STATE_LENGTH);
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
    public void test_verifyState_stateStringTooShort() {
        String responseState = "short";
        try {
            validator.verifyState(responseState, clientId, clientSecret, 0, 0);
            fail("Should have thrown an exception but didn't.");
        } catch (AuthenticationResponseException e) {
            verifyException(e, CWWKS2407E_AUTHENTICATION_RESPONSE_ERROR + ".+" + CWWKS2409E_STATE_VALUE_IN_CALLBACK_INCORRECT_LENGTH);
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

}
