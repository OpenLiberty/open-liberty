/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.internal.utils.OAuthClientUtil;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class TAIJwtUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private static String ACCESS_TOKEN = "EAANQIE2J5nMBAErWBIFfkmu9r6yQeGoIMg39mHRJrZA7L0jbiD7GEpLSZBm96tgqvvlbQI3UIgQXSJaO6sRJGaFEZCwn5kolWgSjs5q71rrNg0GdbHk5yxrtsZAWsZBv3XV1xFmJ4reZBKA6sx5PqQJejg5RtTWKPg4jJoP0zk1AZDZD";

    final SocialLoginConfig clientConfig = mockery.mock(SocialLoginConfig.class);
    final OAuthClientUtil oauthClientUtil = mockery.mock(OAuthClientUtil.class);
    final UserApiConfig userApiConfig = mockery.mock(UserApiConfig.class);
    final JwtToken jwt = mockery.mock(JwtToken.class);
    final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);

    TAIJwtUtils utils = null;

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new TAIJwtUtils();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();

        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /**************************************** createJwtToken ****************************************/

    @Test
    public void createJwtToken_nullIdToken() throws Exception {
        final UserApiConfig[] apiConfigs = new UserApiConfig[] { userApiConfig };
        final String userApi = "userApi";

        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getUserApis();
                will(returnValue(apiConfigs));
                one(userApiConfig).getApi();
                will(returnValue(userApi));
                one(oauthClientUtil).getUserApiAsJwtToken(userApi, ACCESS_TOKEN, sslSocketFactory, false, clientConfig);
            }
        });
        JwtToken result = utils.createJwtToken(oauthClientUtil, null, clientConfig, ACCESS_TOKEN, sslSocketFactory);
        assertNotNull("Result was null when it should not have been.", result);
    }

    @Test
    public void createJwtToken_throwsException() throws Exception {
        final UserApiConfig[] apiConfigs = new UserApiConfig[] { userApiConfig };
        final String userApi = "userApi";
        final String eMsg = "This is an exception message.";

        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getUserApis();
                will(returnValue(apiConfigs));
                one(userApiConfig).getApi();
                will(returnValue(userApi));
                one(oauthClientUtil).getUserApiAsJwtToken(userApi, null, null, false, clientConfig);
                will(throwException(new Exception(eMsg)));
            }
        });
        try {
            JwtToken result = utils.createJwtToken(oauthClientUtil, null, clientConfig, null, null);
            fail("Should have thrown exception creating JWT but did not. Token result: " + result);
        } catch (Exception e) {
            verifyException(e, CWWKS5497E_FAILED_TO_CREATE_JWT_FROM_USER_API + ".+" + Pattern.quote(eMsg));
        }
    }

    /**************************************** createJwtTokenFromIdToken ****************************************/

    @Test
    public void createJwtTokenFromIdToken_nullArgs() throws Exception {
        try {
            JwtToken result = utils.createJwtTokenFromIdToken(null, null);
            fail("Should have thrown exception creating JWT but did not. Token result: " + result);
        } catch (Exception e) {
            // "null" will be used as the insert value for the JWT consumer config ID
            verifyExceptionWithInserts(e, CWWKS5498E_FAILED_TO_CREATE_JWT_FROM_ID_TOKEN, new String[] { null });
        }
    }

}