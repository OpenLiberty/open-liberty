/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.security.javaeesec.fat_helper;

import static org.junit.Assert.assertNotNull;

import org.apache.directory.api.ldap.model.entry.Entry;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;

import componenttest.topology.impl.LibertyServer;

/**
 * Server Helper Methods
 */
public class ServerHelper {

    protected static Class<?> logClass = ServerHelper.class;

    private static EmbeddedApacheDS ldapServer = null;

    public static void commonStopServer(LibertyServer myServer) throws Exception {
        commonStopServer(myServer, false);
    }

    public static void commonStopServer(LibertyServer myServer, boolean stopLdapServer) throws Exception {

        myServer.stopServer();
        myServer.setServerConfigurationFile("server.xml");
        if (stopLdapServer) {
            stopldapServer();
        }
    }

    public static void setupldapServer() throws Exception {
        ldapServer = new EmbeddedApacheDS("HTTPAuthLDAP");
        ldapServer.addPartition("test", "o=ibm,c=us");
        ldapServer.startServer(Integer.parseInt(System.getProperty("ldap.1.port")));

        Entry entry = ldapServer.newEntry("o=ibm,c=us");
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("uid=jaspildapuser1,o=ibm,c=us");
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", "jaspildapuser1");
        entry.add("sn", "jaspildapuser1sn");
        entry.add("cn", "jaspiuser1");
        entry.add("userPassword", "s3cur1ty");
        ldapServer.add(entry);

    }

    public static void stopldapServer() {
        if (ldapServer != null) {
            try {
                ldapServer.stopService();
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
