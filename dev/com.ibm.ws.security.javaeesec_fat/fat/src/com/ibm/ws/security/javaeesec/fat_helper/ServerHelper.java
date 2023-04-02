/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.javaeesec.fat_helper;

import static org.junit.Assert.assertNotNull;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.unboundid.ldap.sdk.Entry;

import componenttest.topology.impl.LibertyServer;

/**
 * Server Helper Methods
 */
public class ServerHelper {

    protected static Class<?> logClass = ServerHelper.class;

    private static InMemoryLDAPServer ldapServer = null;

    public static void commonStopServer(LibertyServer myServer) throws Exception {
        commonStopServer(myServer, false);
    }

    public static void commonStopServer(LibertyServer myServer, boolean stopLdapServer) throws Exception {
        try {
            myServer.stopServer();
        } finally {
            if (stopLdapServer) {
                stopldapServer();
            }
            myServer.setServerConfigurationFile("server.xml");
        }
    }

    public static void setupldapServer() throws Exception {
        ldapServer = new InMemoryLDAPServer(false, Integer.getInteger("ldap.1.port", 10389), 0, "o=ibm,c=us");

        Entry entry = new Entry("o=ibm,c=us");
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm");
        ldapServer.add(entry);

        entry = new Entry("uid=jaspildapuser1,o=ibm,c=us");
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("objectclass", "person");
        entry.addAttribute("uid", "jaspildapuser1");
        entry.addAttribute("sn", "jaspildapuser1sn");
        entry.addAttribute("cn", "jaspiuser1");
        entry.addAttribute("userPassword", "s3cur1ty");
        ldapServer.add(entry);

    }

    public static void stopldapServer() {
        if (ldapServer != null) {
            try {
                ldapServer.shutDown();
            } catch (Exception e) {
                Log.error(logClass, "teardown", e, "LDAP server threw error while stopping. " + e.getMessage());
            }
        }
    }

    public static void verifyServerStarted(LibertyServer server) {
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLogUsingMark("CWWKS0008I"));
    }

    public static void verifyServerUpdated(LibertyServer server) {
        assertNotNull("Feature update wasn't complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("The server configuration wasn't updated.",
                      server.waitForStringInLogUsingMark("CWWKG0017I:.*"));

    }

    public static void verifyServerUpdatedWithJaspi(LibertyServer server) {
        verifyServerUpdated(server);
        assertNotNull("The JASPI user feature did not report it was ready",
                      server.waitForStringInLogUsingMark(MessagesConstants.MSG_JASPI_PROVIDER_ACTIVATED));
        assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                      server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-1.0"));
    }

    public static void verifyServerStartedWithJaspiFeatureAndJacc(LibertyServer server) {
        verifyServerStartedWithJaspiFeature(server);
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog(MessagesConstants.MSG_JACC_SERVICE_STARTING));
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog(MessagesConstants.MSG_JACC_SERVICE_STARTED));

    }

    public static void verifyServerRemovedJaspi(LibertyServer server) {
        verifyServerUpdated(server);
        assertNotNull("The JASPI user feature did not report it was ready",
                      server.waitForStringInLogUsingMark(MessagesConstants.MSG_JASPI_PROVIDER_DEACTIVATED));
    }

    public static void verifyServerStartedWithJaspiFeature(LibertyServer server) {
        verifyServerStarted(server);
        assertNotNull("The JASPI user feature did not report it was ready",
                      server.waitForStringInLogUsingMark(MessagesConstants.MSG_JASPI_PROVIDER_ACTIVATED));
        assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                      server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-1.0"));

    }

}
