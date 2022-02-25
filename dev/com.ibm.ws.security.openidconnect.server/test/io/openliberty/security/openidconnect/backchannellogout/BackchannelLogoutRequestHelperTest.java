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

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import io.openliberty.security.openidconnect.backchannellogout.BackchannelLogoutRequestHelper;
import test.common.SharedOutputManager;

public class BackchannelLogoutRequestHelperTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.openidconnect.*=all:com.ibm.ws.security.openidconnect*=all");

    private final OAuth20Provider oauth20provider = mockery.mock(OAuth20Provider.class);
    private final OidcServerConfig oidcServerConfig = mockery.mock(OidcServerConfig.class);

    private BackchannelLogoutRequestHelper helper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        helper = new BackchannelLogoutRequestHelper(oauth20provider, oidcServerConfig);
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
    public void test_sendBackchannelLogoutRequests_nullToken() throws IOException {
        String idTokenString = null;
        helper.sendBackchannelLogoutRequests(idTokenString);
    }

}
