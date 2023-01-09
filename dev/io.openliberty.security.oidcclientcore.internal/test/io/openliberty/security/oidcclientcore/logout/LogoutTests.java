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
package io.openliberty.security.oidcclientcore.logout;

import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import test.common.SharedOutputManager;

public class LogoutTests extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String endSessionEndpoint = "http://some-domain.com/path/endSession";
    private static final String redirectURI = "http://redirect-uri.com/some/path";
    private static final String idToken = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMS9vYXV0aDIvZW5kcG9pbnQvT0F1dGhDb25maWdTYW1wbGUvdG9rZW4iLCJpYXQiOjEzODczODM5NTMsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg3Mzg3NTUzLCJhdWQiOiJjbGllbnQwMSJ9.ottD3eYa6qrnItRpL_Q9UaKumAyo14LnlvwnyF3Kojk";

    private final OidcClientConfig oidcClientConfigMock = null; // mockery.mock(OidcClientConfig.class);
    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
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
    public void test_RPInitiatedLogoutStrategy() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(request).setAttribute("id_token_hint", idToken);
                allowing(request).setAttribute("post_logout_redirect_uri", null);
                allowing(request).setAttribute("post_logout_redirect_uri", null);
            }
        });
        RPInitiatedLogoutStrategy rpInitiatedLogoutStrategy = new RPInitiatedLogoutStrategy(request, oidcClientConfigMock, endSessionEndpoint, idToken);
        ProviderAuthenticationResult result = rpInitiatedLogoutStrategy.logout();

        assertTrue(result.getStatus().equals(AuthResult.REDIRECT_TO_PROVIDER));
        assertTrue(result.getHttpStatusCode() == 200);
        assertTrue(result.getRedirectUrl().equals(endSessionEndpoint));
    }

    @Test
    public void test_CustomLogoutStrategy() throws Exception {
        CustomLogoutStrategy customLogoutStrategy = new CustomLogoutStrategy(redirectURI);
        ProviderAuthenticationResult result = customLogoutStrategy.logout();

        assertTrue(result.getStatus().equals(AuthResult.REDIRECT_TO_PROVIDER));
        assertTrue(result.getHttpStatusCode() == 200);
        assertTrue(result.getRedirectUrl().equals(redirectURI));
    }
}
