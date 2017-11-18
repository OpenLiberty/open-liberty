/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.BaseEntry;
import com.ibm.websphere.simplicity.config.wim.FailoverServers;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.Realm;

import componenttest.topology.impl.LibertyServer;

/**
 * LDAP FAT test utility methods.
 */
public class LDAPFatUtils {

    /**
     * Assert that two distinguished names are equal.
     *
     * @param msg Message to print on failure.
     * @param dn1 The first distinguished name.
     * @param dn2 The second distinguished name.
     * @throws InvalidNameException If either of the names
     */
    public static void assertDNsEqual(String msg, String dn1, String dn2) {

        LdapName ln1 = null;
        LdapName ln2 = null;

        assertNotNull("First distinguished name passed into assertDNsEqual is null.", dn1);
        assertNotNull("Second distinguished name passed into assertDNsEqual is null.", dn2);

        try {
            ln1 = new LdapName(dn1);
        } catch (Exception e) {
            fail("Distinguished name 1 was invalid: " + dn1);
        }
        try {
            ln2 = new LdapName(dn2);
        } catch (Exception e) {
            fail("Distinguished name 2 was invalid: " + dn2);
        }

        assertEquals(msg, ln1, ln2);
    }

    /**
     * Convenience method to create an LdapRegistry configuration object for Active Directory LDAP server
     * and if provided a {@link ServerConfiguration} instance add it to the list of LDAP registries.
     *
     * @param serverConfiguration The {@link ServerConfiguration} instance. Can be null.
     * @param id The registry ID. Can be null.
     * @param realm The realm name. Can be null.
     * @return The LdapRegistry instance.
     */
    public static LdapRegistry createADLdapRegistry(ServerConfiguration serverConfiguration, String id, String realm) {
        LdapRegistry ldap = new LdapRegistry();
        ldap.setId(id);
        ldap.setRealm(realm);
        ldap.setLdapType("Microsoft Active Directory");
        ldap.setBaseDN("cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com");
        ldap.setHost("${ldap.server.2.name}");
        ldap.setPort("${ldap.server.2.port}");
        ldap.setBindDN("cn=testuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com");
        ldap.setBindPassword("testuserpwd");
        ldap.setSearchTimeout("8m");
        ldap.setFailoverServer(new FailoverServers("failoverLdapServers", new String[][] { { "${ldap.server.6.name}", "${ldap.server.6.port}" } }));

        if (serverConfiguration != null) {
            serverConfiguration.getLdapRegistries().add(ldap);
        }

        return ldap;
    }

    /**
     * Convenience method to create an FederatedRepository configuration object with the specified
     * realm name and participating base entries and if provided a {@link ServerConfiguration}
     * set it as the FederatedRepository on the {@link ServerConfiguration} instance.
     *
     * @param serverConfiguration The {@link ServerConfiguration} instance. Can be null.
     * @param primaryRealmName The primary realm name.
     * @param participatingBaseEntries The participating base entries in an array of Strings.
     * @return The FederatedRepository instance.
     */
    public static FederatedRepository createFederatedRepository(ServerConfiguration serverConfiguration, String primaryRealmName, String[] participatingBaseEntries) {

        ConfigElementList<BaseEntry> pbes = null;
        if (participatingBaseEntries != null && participatingBaseEntries.length > 0) {
            pbes = new ConfigElementList<BaseEntry>();
            for (String pbe : participatingBaseEntries) {
                pbes.add(new BaseEntry(pbe));
            }
        }

        FederatedRepository federatedRepository = new FederatedRepository();
        federatedRepository.setPrimaryRealm(new Realm(primaryRealmName, pbes));

        if (serverConfiguration != null) {
            serverConfiguration.setFederatedRepositoryElement(federatedRepository);
        }

        return federatedRepository;
    }

    /**
     * Convenience method to create an LdapRegistry configuration object for Oracle / Sun LDAP server
     * and if provided a {@link ServerConfiguration} instance add it to the list of LDAP registries.
     *
     * @param serverConfiguration The {@link ServerConfiguration} instance. Can be null.
     * @param id The registry ID. Can be null.
     * @param realm The realm name. Can be null.
     * @return The LdapRegistry instance.
     */
    public static LdapRegistry createSunLdapRegistry(ServerConfiguration serverConfiguration, String id, String realm, String name) {
        LdapRegistry ldap = new LdapRegistry();
        ldap.setId(id);
        ldap.setRealm(realm);
        ldap.setName(name);
        ldap.setLdapType("Sun Java System Directory Server");
        ldap.setBaseDN("dc=rtp,dc=raleigh,dc=ibm,dc=com");
        ldap.setHost("${ldap.server.13.name}");
        ldap.setPort("${ldap.server.13.port}");
        ldap.setSearchTimeout("8m");

        ldap.setFailoverServer(new FailoverServers("failoverLdapServers", new String[][] { { "${ldap.server.3.name}", "${ldap.server.3.port}" } }));

        if (serverConfiguration != null) {
            serverConfiguration.getLdapRegistries().add(ldap);
        }

        return ldap;
    }

    /**
     * Convenience method to create an LdapRegistry configuration object for TDS LDAP server
     * and if provided a {@link ServerConfiguration} instance add it to the list of LDAP registries.
     *
     * @param serverConfiguration The {@link ServerConfiguration} instance. Can be null.
     * @param id The registry ID. Can be null.
     * @param realm The realm name. Can be null.
     * @return The LdapRegistry instance.
     */
    public static LdapRegistry createTDSLdapRegistry(ServerConfiguration serverConfiguration, String id, String realm) {
        LdapRegistry ldap = new LdapRegistry();
        ldap.setId(id);
        ldap.setRealm(realm);
        ldap.setLdapType("IBM Tivoli Directory Server");
        ldap.setBaseDN("o=ibm,c=us");
        ldap.setHost("${ldap.server.1.name}");
        ldap.setPort("${ldap.server.1.port}");
        ldap.setSearchTimeout("8m");

        ldap.setFailoverServer(new FailoverServers("failoverLdapServers", new String[][] { { "${ldap.server.4.name}", "${ldap.server.4.port}" },
                                                                                           { "${ldap.server.5.name}", "${ldap.server.5.port}" } }));

        if (serverConfiguration != null) {
            serverConfiguration.getLdapRegistries().add(ldap);
        }

        return ldap;
    }

    /**
     * This method will the reset the log and trace marks for log and trace searches, update the
     * configuration and then wait for the server to re-initialize.
     *
     * @param server The server to update.
     * @param config The configuration to use.
     * @throws Exception If there was an issue updating the server configuration.
     */
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config) throws Exception {
        updateConfigDynamically(server, config, false);
    }

    /**
     * This method will the reset the log and trace marks for log and trace searches, update the
     * configuration and then wait for the server to re-initialize. Optionally it will then wait for the application to start.
     *
     * @param server The server to update.
     * @param config The configuration to use.
     * @param waitForAppToStart Wait for the application to start.
     * @throws Exception If there was an issue updating the server configuration.
     */
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config, boolean waitForAppToStart) throws Exception {
        resetMarksInLogs(server);
        server.updateServerConfiguration(config);
        server.waitForStringInLogUsingMark("CWWKG001[7-8]I");
        if (waitForAppToStart) {
            server.waitForStringInLogUsingMark("CWWKZ0003I"); //CWWKZ0003I: The application userRegistry updated in 0.020 seconds.
        }
    }

    /**
     * Reset the marks in all Liberty logs.
     *
     * @param server The server for the logs to reset the marks.
     * @throws Exception If there was an error resetting the marks.
     */
    public static void resetMarksInLogs(LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());
    }
}
