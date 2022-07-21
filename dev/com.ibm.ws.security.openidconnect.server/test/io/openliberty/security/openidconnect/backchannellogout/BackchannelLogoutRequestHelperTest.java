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
package io.openliberty.security.openidconnect.backchannellogout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import test.common.SharedOutputManager;

public class BackchannelLogoutRequestHelperTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.openidconnect.*=all:com.ibm.ws.security.openidconnect*=all");

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final OidcServerConfig oidcServerConfig = mockery.mock(OidcServerConfig.class);
    private final OidcBaseClient client = mockery.mock(OidcBaseClient.class);

    private BackchannelLogoutRequestHelper helper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        helper = new BackchannelLogoutRequestHelper(request, oidcServerConfig);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_isValidClientForBackchannelLogout_noLogoutUri() {
        mockery.checking(new Expectations() {
            {
                one(client).getBackchannelLogoutUri();
                will(returnValue(null));
            }
        });
        assertFalse("Client without a back-channel logout URI should not be considered valid for BCL.", helper.isValidClientForBackchannelLogout(client));
    }

    @Test
    public void test_isValidClientForBackchannelLogout_logoutUriNotHttp() {
        mockery.checking(new Expectations() {
            {
                one(client).getBackchannelLogoutUri();
                will(returnValue("scp://localhost"));
                allowing(client).getClientId();
                will(returnValue("myOidcClient"));
            }
        });
        assertFalse("Client with non-HTTP back-channel logout URI should not be considered valid for BCL.", helper.isValidClientForBackchannelLogout(client));
    }

    @Test
    public void test_isValidClientForBackchannelLogout_httpPublicClient() {
        mockery.checking(new Expectations() {
            {
                one(client).getBackchannelLogoutUri();
                will(returnValue("http://localhost"));
                one(client).isPublicClient();
                will(returnValue(true));
                allowing(client).getClientId();
                will(returnValue("myOidcClient"));
            }
        });
        assertFalse("Public client with HTTP back-channel logout URI should not be considered valid for BCL.", helper.isValidClientForBackchannelLogout(client));
    }

    @Test
    public void test_isValidClientForBackchannelLogout_httpConfidentialClient() {
        mockery.checking(new Expectations() {
            {
                one(client).getBackchannelLogoutUri();
                will(returnValue("http://localhost"));
                one(client).isPublicClient();
                will(returnValue(false));
            }
        });
        assertTrue("Confidential client with HTTP back-channel logout URI should be considered valid for BCL.", helper.isValidClientForBackchannelLogout(client));
    }

    @Test
    public void test_isValidClientForBackchannelLogout_httpsUri() {
        mockery.checking(new Expectations() {
            {
                one(client).getBackchannelLogoutUri();
                will(returnValue("https://localhost"));
            }
        });
        assertTrue("HTTPS back-channel logout URI should be considered valid for BCL.", helper.isValidClientForBackchannelLogout(client));
    }

}
