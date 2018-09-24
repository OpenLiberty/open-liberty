/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.net.ssl.SSLSocketFactory;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import test.common.SharedOutputManager;

public class UserInfoHelperTest {

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
    private static final String USERINFO = "{\"sub\":\"testuser\",\"iss\":\"https:superfoo\",\"name\":\"testuser\"}";

    class UserInfoHelperMock extends UserInfoHelper {
        String mockURLResponse = USERINFO;
        String mockUpdateResult = null;
        boolean updateAuthResultCalled = false;

        public UserInfoHelperMock(ConvergedClientConfig config) {
            super(config);
        }

        @Override
        protected String getUserInfoFromURL(ConvergedClientConfig config, SSLSocketFactory sslsf, String accessToken) {
            return mockURLResponse;
        }

        @Override
        protected void updateAuthenticationResultPropertiesWithUserInfo(ProviderAuthenticationResult oidcResult, String userInfoStr) {
            mockUpdateResult = userInfoStr;
            updateAuthResultCalled = true;
        }

    }

    @Before
    public void before() {

    }

    @After
    public void after() {
        mock.assertIsSatisfied();
    }

    void setExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(convClientConfig).getUserInfoEndpointUrl();
                will(returnValue("somebogusurl"));
                allowing(convClientConfig).isUserInfoEnabled();
                will(returnValue(true));
            }
        });
    }

    @Test
    public void testRetrieveValidInfo() {
        setExpectations();
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig);
        boolean result = uihm.getUserInfo(null, null, "accesstoken", "testuser");
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
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig);
        // put the wrong sub in the userinfo, should not get the userinfo back.
        uihm.mockURLResponse = uihm.mockURLResponse.replace("testuser", "bogususer");
        boolean result = uihm.getUserInfo(null, null, "accesstoken", "testuser");
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
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig);
        boolean result = uihm.getUserInfo(null, null, "accesstoken", null);
        assertFalse("method return should be false", result);
        assertFalse("updateAuthResult should not have been called",
                uihm.updateAuthResultCalled);
    }

    @Test
    public void testGetUserInfoSubClaim() {
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig);
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
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig);
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
        UserInfoHelperMock uihm = new UserInfoHelperMock(convClientConfig);
        assertFalse("willRetreive should be false", uihm.willRetrieveUserInfo());

    }
}
