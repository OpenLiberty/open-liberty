/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.social.MessageConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class SocialLoginFeatureOnlyTests extends CommonSecurityFat {

    public static Class<?> thisClass = SocialLoginFeatureOnlyTests.class;

    @Server("com.ibm.ws.security.social_fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        serverTracker.addServer(server);
    }

    /**
     * Tests:
     * - Start the server with just the socialLogin-1.0 feature configured
     * Expected Results:
     * - Should start and stop the server without any errors.
     */
    @Test
    public void SocialLoginFeatureOnlyTests_featureOnly() throws Exception {
        // Ensure the social login web application becomes available
        List<String> waitForMessages = new ArrayList<String>();
        waitForMessages.add(MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*/ibm/api/social-login/");

        server.startServerUsingExpandedConfiguration("server_featureOnly.xml", waitForMessages);
        server.stopServer();
    }

    /**
     * Test Purpose:
     * -Start the server with just the socialLogin-1.0 feature configured and alternate context root specified in server.xml
     * Expected Results:
     * -Should start and stop the server without any errors, and app should log that it's started at the alternate context root.
     */
    @Test
    public void SocialLoginFeatureOnlyTests_contextRoot() throws Exception {
        // Ensure the social login web application becomes available
        List<String> waitForMessages = new ArrayList<String>();
        waitForMessages.add(MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*/some/place");

        server.startServerUsingExpandedConfiguration("server_featureOnly_contextroot.xml", waitForMessages);
        server.stopServer();
    }

}
