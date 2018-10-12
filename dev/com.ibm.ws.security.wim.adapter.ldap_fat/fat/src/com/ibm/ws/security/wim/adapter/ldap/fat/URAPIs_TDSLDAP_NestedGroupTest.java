/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * This test covers security registry APIs with Tivoli Directory Server nested groups created on the directory
 * server with server.xml configuration settings for filters and membershipAttribute of nested to allow for nested group search.
 * This test expects the following groups to exist on the directory server:
 *
 * dn: cn=nested_g1,o=ibm,c=us
 * objectclass: groupOfNames
 * objectclass: ibm-nestedGroup
 * cn: nested_g1
 * ibm-memberGroup: cn=embedded_group1,o=ibm,c=us
 * member: cn=topng_user1,o=ibm,c=us
 *
 * dn: cn=embedded_group1,o=ibm,c=us
 * cn: embedded_group1
 * objectclass: groupOfNames
 * objectclass: top
 * member: cn=ng_user1,o=ibm,c=us
 *
 *
 * If this test is failing, check that the nested groups exist on the directory server.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class URAPIs_TDSLDAP_NestedGroupTest extends URAPIs_LDAP_NestedGroupBase {
    protected static UserRegistryServletConnection myServlet;
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.tds.nested");
    protected static Class<?> logClass = URAPIs_TDSLDAP_NestedGroupTest.class;
    protected static BasicAuthClient myClient;

    private final String tdsSuffix = ",o=ibm,c=us";
    private final String tdsRealm = "TDSRealm";
    private final String tdsCN = "cn=";

    public URAPIs_TDSLDAP_NestedGroupTest() {
        super(myServer, logClass, myClient, myServlet);
    }

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(myServer);
        Log.info(logClass, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        myServer.addInstalledAppForValidation("userRegistry");
        myServer.startServer(logClass.getName() + ".log");

        //Make sure the application has come up before proceeding
        assertNotNull("Application userRegistry does not appear to have started.",
                      myServer.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      myServer.waitForStringInLog("CWWKF0011I"));

        Log.info(logClass, "setUp", "Creating servlet connection the server");
        myServlet = new UserRegistryServletConnection(myServer.getHostname(), myServer.getHttpDefaultPort());

        if (myServlet.getRealm() == null) {
            Thread.sleep(5000);
            myServlet.getRealm();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(logClass, "tearDown", "Stopping the server...");
        try {
            myServer.stopServer();
        } finally {
            myServer.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    @Override
    String getSuffix() {

        return tdsSuffix;
    }

    @Override
    String getLDAPRealm() {

        return tdsRealm;
    }

    @Override
    String getCN() {
        return tdsCN;
    }

}