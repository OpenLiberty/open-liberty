/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * This test covers nested LDAP groups and roles based authorization decisions based on the nested global groups
 * with Active Directory LDAP. The LDAP configuration in server.xml uses recursiveSearch="true" to search nested groups on AD.
 * This test assumes that the nested global groups have already been defined in the AD LDAP registry as follows:
 *
 * Top level group: nested_g1 (Mapped to Employee role)
 * User in top group: topng_user1
 *
 * SubGroup: embedded_group1 (Mapped to Manager role)
 * User: ng_user1
 *
 * SubGroup: embedded_group2 (not mapped to a role)
 * User: ng_user2
 *
 * The server.xml file maps nested_g1 to Employee and subgroup, embedded_group1, to Manager role.
 * The subgroup embedded_group1 should have both Employee and Manager role as it is part of top group.
 * The subgroup embedded_group2 does not have a role mapping and should inherit Employee Role from top group.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LDAPADNestedGroupsTest extends LDAPNestedGroupsBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauth.ldap.ad.nested");
    protected static Class<?> logClass = LDAPNestedGroupsBase.class;
    protected static BasicAuthClient myClient;

    public LDAPADNestedGroupsTest() {
        super(myServer, logClass, myClient);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        LDAPUtils.addLDAPVariables(myServer);

        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "basicauth.war");

        myServer.addInstalledAppForValidation("basicauth");
        myServer.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2850I"));
        assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2851I"));

        myClient = new BasicAuthClient(myServer);
        myClient.setJaccValidation(true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JACCFatUtils.uninstallCustomRegistryFeature(myServer);
        }
    }

    @After
    public void resetConnection() {
        myClient.resetClientState();
    }

}
