/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.custom.junit.runner.FATRunner;

/**
 * Test the push method of PushBuilder from a secured and an unsecured servlet
 *
 * NOTE: We need a client that supports HTTP2 with ALPN. The current HttpClient and HttpCore 5
 * that we have in artifactory is an Alpha version. The latest version is in Beta. We should
 * wait until they release the official version to implement the actual automation.
 */
@RunWith(FATRunner.class)
public class WCPushBuilderSecurityTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCPushBuilderSecurityTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_PushBuilderSecurity");

    @BeforeClass
    public static void setUp() throws Exception {

        LOG.info("Setup : add TestPushBuilderSecurity to the server if not already present.");

        WCApplicationHelper.addWarToServerApps(SHARED_SERVER.getLibertyServer(), "TestPushBuilderSecurity.war", true, "testpushbuildersecurity.war.servlets");

        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("TestPushBuilderSecurity", WCPushBuilderSecurityTest.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {

        SHARED_SERVER.getLibertyServer().stopServer(null);
    }

    /**
     * Verify that a secured Servlet can push a secured Resource to the client.
     *
     * Drive a request to: https://localhost:9443/TestPushBuilderSecurity/PushBuilderSecuredServlet
     * Provide the credentials:
     * username: admin
     * password: adminpass
     *
     * You should be able to see something like this in the traces:
     *
     * "ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl > determineWebReply Entry
     * null
     * /images/logo_horizontal_light_navy.png
     * com.ibm.ws.webcontainer.security.WebRequestImpl@1ae7b3ab
     * AuthenticationResult status=SUCCESS"
     *
     * @throws Exception
     */
    //@Test
    public void testPushBuilderPushMethodSecured() throws Exception {

    }

    /**
     * Verify that an unsecured Servlet can not push a secured Resource to the client.
     *
     * Drive a request to: https://localhost:9443/TestPushBuilderSecurity/PushBuilderUnsecuredServlet
     * Do not provide the credentials
     *
     * You should be able to see something like this in the traces:
     *
     * "ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl > determineWebReply Entry
     * null
     * /images/logo_horizontal_light_navy.png
     * com.ibm.ws.webcontainer.security.WebRequestImpl@ffbb5c85
     * AuthenticationResult status=SEND_401 realm=defaultRealm"
     *
     * @throws Exception
     */
    //@Test
    public void testPushBuilderPushMethodUnsecured() throws Exception {

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

}
