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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.security.Result;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class WebSphereUserRegistryUsingCustomTest extends WebsphereUserRegistryComplianceTests {
    private static final Class<?> c = WebSphereUserRegistryUsingCustomTest.class;
    private static WebsphereUserRegistryServletConnection myServlet;
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.websphere.security.fat.registry.custom");

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        SecurityFatUtils.transformApps(myServer, "WebsphereUserRegistry.war");
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/webspheresecuritylibertyinternals-1.0.mf");
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/customRegistrySample-1.0.mf");
        myServer.copyFileToLibertyInstallRoot("lib", "internalfeatures/com.ibm.ws.security.registry.custom.sample_1.0.jar");
        myServer.addInstalledAppForValidation("WebsphereUserRegistry");
        myServer.startServer(c.getName() + ".log");

        myServlet = new WebsphereUserRegistryServletConnection(myServer.getHostname(), myServer.getHttpDefaultPort(), "customRealm");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        myServer.stopServer();
        myServer.deleteFileFromLibertyInstallRoot("lib/features/webspheresecuritylibertyinternals-1.0.mf");
        myServer.deleteFileFromLibertyInstallRoot("lib/features/customRegistrySample-1.0.mf");
        myServer.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.security.registry.custom.sample_1.0.jar");
        myServlet = null;
    }

    public WebSphereUserRegistryUsingCustomTest() {
        server = myServer;
        servlet = myServlet;

        realmName = "customRealm";

        invalidUserName = "invalidUser";
        validUserSecurityName = "user0";
        validUserDisplayName = validUserSecurityName;
        validUserUniqueId = "500";
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
        validGroupUniqueId = "000";
        validGroupName0WithNoMembers = "group0";
        validGroupName1WithOneMember = "group1";
        validGroup1Memeber = "user2";
        validGroupName2WithManyMembers = "group2";
        validGroup2Members = new ArrayList<String>();
        validGroup2Members.add("user1");
        validGroup2Members.add("user2");
    }

    /**
     * The custom user registry uses the start "*" character in the search pattern.
     */
    @Override
    @Test
    public void getGroupsAllMatches() throws Exception {
        Result result = servlet.getGroups("*", 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().contains(validGroupSecurityName));
        assertTrue(result.getList().contains(validGroupName0WithNoMembers));
        assertTrue(result.getList().contains(validGroupName1WithOneMember));
        assertTrue(result.getList().contains(validGroupName2WithManyMembers));
    }
}