/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import test.common.SharedOutputManager;

public class BackchannelLogoutRequestHelperTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.openidconnect.*=all:com.ibm.ws.security.openidconnect*=all");

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final OidcServerConfig oidcServerConfig = mockery.mock(OidcServerConfig.class);
    private final OAuth20Provider provider = mockery.mock(OAuth20Provider.class);
    private final OidcOAuth20ClientProvider clientProvider = mockery.mock(OidcOAuth20ClientProvider.class);

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
    public void hasClientWithBackchannelLogoutUri_nullClientProvider() {
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(null));
            }
        });
        boolean result = BackchannelLogoutRequestHelper.hasClientWithBackchannelLogoutUri(provider);
        assertFalse("Expected false because there is no client provider, so the clients cannot be checked.", result);
    }

    @Test
    public void hasClientWithBackchannelLogoutUri_errorGettingClients() throws OidcServerException {
        OidcServerException oidcServerException = new OidcServerException("some description", "some code", 500);
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(throwException(oidcServerException));
            }
        });
        boolean result = BackchannelLogoutRequestHelper.hasClientWithBackchannelLogoutUri(provider);
        assertFalse("Expected false because there was an error getting the clients, so the clients cannot be checked.", result);
    }

    @Test
    public void hasClientWithBackchannelLogoutUri_noClients() throws OidcServerException {
        Collection<OidcBaseClient> clients = new ArrayList<>();
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(clients));
            }
        });
        boolean result = BackchannelLogoutRequestHelper.hasClientWithBackchannelLogoutUri(provider);
        assertFalse("Expected false because there are no clients.", result);
    }

    @Test
    public void hasClientWithBackchannelLogoutUri_noClientsHaveBackchannelLogoutUri_null() throws OidcServerException {
        Collection<OidcBaseClient> clients = new ArrayList<>();
        OidcBaseClient client1 = mockery.mock(OidcBaseClient.class, "oidcBaseClient1");
        clients.add(client1);
        OidcBaseClient client2 = mockery.mock(OidcBaseClient.class, "oidcBaseClient2");
        clients.add(client2);
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(clients));
                one(client1).getBackchannelLogoutUri();
                will(returnValue(null));
                one(client2).getBackchannelLogoutUri();
                will(returnValue(null));
            }
        });
        boolean result = BackchannelLogoutRequestHelper.hasClientWithBackchannelLogoutUri(provider);
        assertFalse("Expected false because the clients' back-channel logout uri's were null.", result);
    }

    @Test
    public void hasClientWithBackchannelLogoutUri_noClientsHaveBackchannelLogoutUri_empty() throws OidcServerException {
        Collection<OidcBaseClient> clients = new ArrayList<>();
        OidcBaseClient client1 = mockery.mock(OidcBaseClient.class, "oidcBaseClient1");
        clients.add(client1);
        OidcBaseClient client2 = mockery.mock(OidcBaseClient.class, "oidcBaseClient2");
        clients.add(client2);
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(clients));
                one(client1).getBackchannelLogoutUri();
                will(returnValue(""));
                one(client2).getBackchannelLogoutUri();
                will(returnValue(""));
            }
        });
        boolean result = BackchannelLogoutRequestHelper.hasClientWithBackchannelLogoutUri(provider);
        assertFalse("Expected false because the clients' back-channel logout uri's were empty.", result);
    }

    @Test
    public void hasClientWithBackchannelLogoutUri_atLeastOneClientHasBackchannelLogoutUri_first() throws OidcServerException {
        Collection<OidcBaseClient> clients = new ArrayList<>();
        OidcBaseClient client1 = mockery.mock(OidcBaseClient.class, "oidcBaseClient1");
        clients.add(client1);
        OidcBaseClient client2 = mockery.mock(OidcBaseClient.class, "oidcBaseClient2");
        clients.add(client2);
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(clients));
                one(client1).getBackchannelLogoutUri();
                will(returnValue("http://localhost:8941/oidcclient/backchannel_logout/client01"));
            }
        });
        boolean result = BackchannelLogoutRequestHelper.hasClientWithBackchannelLogoutUri(provider);
        assertTrue("Expected true because at least one client had a back-channel logout uri configured.", result);
    }

    @Test
    public void hasClientWithBackchannelLogoutUri_atLeastOneClientHasBackchannelLogoutUri_second() throws OidcServerException {
        Collection<OidcBaseClient> clients = new ArrayList<>();
        OidcBaseClient client1 = mockery.mock(OidcBaseClient.class, "oidcBaseClient1");
        clients.add(client1);
        OidcBaseClient client2 = mockery.mock(OidcBaseClient.class, "oidcBaseClient2");
        clients.add(client2);
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).getAll();
                will(returnValue(clients));
                one(client1).getBackchannelLogoutUri();
                will(returnValue(null));
                one(client2).getBackchannelLogoutUri();
                will(returnValue("http://localhost:8941/oidcclient/backchannel_logout/client02"));
            }
        });
        boolean result = BackchannelLogoutRequestHelper.hasClientWithBackchannelLogoutUri(provider);
        assertTrue("Expected true because at least one client had a back-channel logout uri configured.", result);
    }

}
