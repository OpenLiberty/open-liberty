/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

package com.ibm.ws.com.unboundid;

import com.ibm.websphere.simplicity.log.Log;

/**
 * An in memory UnboundID LDAP server designed to contain the same data
 * as the Active Directory instance of the ApacheDS stand-alone LDAP Servers.
 * <p/>
 * This server is currently using a null schema to host the data, this
 * could be replaced in the future with a robust AD schema design.
 */
public class InMemoryADLDAPServer extends InMemoryLDAPServer {
    protected static final Class<?> c = InMemoryADLDAPServer.class;
    public static final String BASE_DN = "DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM";

    /**
     * Allow running this instance as an executable.
     *
     * @param args Parameters passed in from the command.
     * @throws Exception if the instance fails to start.
     */
    public static void main(String[] args) throws Exception {
        InMemoryLDAPServer ldapServer = new InMemoryADLDAPServer();

        /*
         * Wait until the process has been cancelled via ctrl-c.
         */
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        ldapServer.shutDown();
    }

    /**
     * Creates a new instance of the in memory LDAP server. It initializes the directory
     * service and pre-populates it with data that was in the ApacheDS AD instance.
     *
     * @throws Exception If something went wrong
     */
    public InMemoryADLDAPServer() throws Exception {
        this(0, 0);
    }

    /**
     * Creates a new instance of the in memory LDAP server. It initializes the directory
     * service and pre-populates it with data that was in the ApacheDS AD instance.
     *
     * @param ldapPort  The LDAP port to use. 0 to indicate the server should choose an available port.
     * @param ldapsPort The LDAPS port to use. 0 to indicate the server should choose an available port.
     * @throws Exception If something went wrong
     */
    public InMemoryADLDAPServer(int ldapPort, int ldapsPort) throws Exception {
        super(false, ldapPort, ldapsPort, BASE_DN);

        /*
         * Load the Active Directory data LDIF. This contains users and groups that were originally
         * in the ApacheDS AD stand-alone LDAP instance.
         */
        int entriesAdded = this.importFromLDIF(true, extractResourceToFile("/resources/AD.ldif", "addata", "ldif").getAbsolutePath());
        Log.info(c, "setupLdapServer", "Adding " + entriesAdded + " changes to LDAP Server");
    }

    /**
     * The port defined by the testing framework to use for LDAP connections that was used by the
     * ApacheDS AD instance. This port can be used instead of using an ephemeral port when it is
     * necessary or beneficial to use the well-known port assigned for FATS.
     *
     * @return The well-known port to use for LDAP connections.
     */
    public static int getWellKnownLdapPort() {
        return Integer.getInteger("ldap.2.port", 20389); // From testports.properties
    }

    /**
     * The port defined by the testing framework to use for LDAPS connections that was used by the
     * ApacheDS AD instance. This port can be used instead of using an ephemeral port when it is
     * necessary or beneficial to use the well-known port assigned for FATS.
     *
     * @return The well-known port to use for LDAPS connections.
     */
    public static int getWellKnownLdapsPort() {
        return Integer.getInteger("ldap.2.ssl.port", 20636); // From testports.properties
    }
}