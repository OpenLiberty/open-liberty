/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.fat;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class WebsphereUserRegistryUsingBasicTest extends WebsphereUserRegistryComplianceTests {
    private static final Class<?> c = WebsphereUserRegistryUsingBasicTest.class;
    private static WebsphereUserRegistryServletConnection myServlet;
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.websphere.security.fat.registry");

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        SecurityFatUtils.transformApps(myServer, "WebsphereUserRegistry.war");
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/webspheresecuritylibertyinternals-1.0.mf");
        myServer.addInstalledAppForValidation("WebsphereUserRegistry");
        myServer.startServer(c.getName() + ".log");

        myServlet = new WebsphereUserRegistryServletConnection(myServer.getHostname(), myServer.getHttpDefaultPort(), "BasicRegistry");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        myServer.stopServer();
        myServer.deleteFileFromLibertyInstallRoot("lib/features/webspheresecuritylibertyinternals-1.0.mf");
        myServlet = null;
    }

    public WebsphereUserRegistryUsingBasicTest() {
        server = myServer;
        servlet = myServlet;

        realmName = "BasicRegistry";

        invalidUserName = "invalidUser";
        validUserSecurityName = "user0";
        validUserDisplayName = validUserSecurityName;
        validUserUniqueId = validUserSecurityName;
        validUserPassword = "user0pwd";
        validUserInvalidPassword = "invalidPassword";
        validUserName0WithNoGroups = "user0";
        validUserName1WithOneGroup = "user1";
        validUser1Group = "group2";
        validUserName2WithManyGroups = "user2";
        validUser2Groups = new ArrayList<String>();
        validUser2Groups.add("group1");
        validUser2Groups.add("group2");

        invalidGroupName = "invalidGroup";
        validGroupSecurityName = "group0";
        validGroupDisplayName = validGroupSecurityName;
        validGroupUniqueId = validGroupSecurityName;
        validGroupName0WithNoMembers = "group0";
        validGroupName1WithOneMember = "group1";
        validGroup1Memeber = "user2";
        validGroupName2WithManyMembers = "group2";
        validGroup2Members = new ArrayList<String>();
        validGroup2Members.add("user1");
        validGroup2Members.add("user2");
    }
}