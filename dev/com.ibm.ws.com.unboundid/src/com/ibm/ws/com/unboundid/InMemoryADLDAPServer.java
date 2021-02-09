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

package com.ibm.ws.com.unboundid;

import com.ibm.websphere.simplicity.log.Log;

/**
 * An in memory UnboundID LDAP server designed to contain the same data as the Active Directory instance of the ApacheDS stand-alone LDAP Servers.
 *
 * This InMemoryADLDAPServer is currently using a null schema to host the data, this could be replaced in the future with a robust activeDirectory schema design
 */
public class InMemoryADLDAPServer extends InMemoryLDAPServer {
    protected static final Class<?> c = InMemoryADLDAPServer.class;
    public static final String BASE_DN = "DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM";

    /**
     * Creates a new instance of the in memory LDAP server. It initializes the directory
     * service.
     *
     * @throws Exception If something went wrong
     */
    public InMemoryADLDAPServer() throws Exception {
        super(false, BASE_DN);

        /*
         * Load the Active Directory data LDIF. This contains users and groups that were originally
         * in the ApacheDS AD stand-alone LDAP instance.
         */
        int entriesAdded = this.importFromLDIF(true, extractResourceToFile("/resources/AD.ldif", "addata", "ldif").getAbsolutePath());
        Log.info(c, "setupLdapServer", "Adding " + entriesAdded + " changes to LDAP Server");
    }

}