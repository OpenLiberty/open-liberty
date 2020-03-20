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
 * An in memory UnboundID LDAP server designed to contain the same data as the Tivoli Directory Server instance of the ApacheDS stand-alone LDAP Servers.
 *
 * This InMemoryTDSLDAPServer is currently using a null schema to host the data, this could be replaced in the future with a robust TDS schema design
 */
public class InMemoryTDSLDAPServer extends InMemoryLDAPServer {
    protected static final Class<?> c = InMemoryTDSLDAPServer.class;
    public static final String BASE_DN = "o=ibm,c=us";

    /**
     * Creates a new instance of the in memory LDAP server. It initializes the directory
     * service.
     *
     * @throws Exception If something went wrong
     */
    public InMemoryTDSLDAPServer() throws Exception {
        super(false, BASE_DN);

        /*
         * Load the TDS data LDIF. This contains users and groups that were originally
         * in the ApacheDS Tivoli stand-alone LDAP instance.
         */
        int entriesAdded = this.importFromLDIF(true, extractResourceToFile("/resources/TDS.ldif", "tdsdata", "ldif").getAbsolutePath());
        Log.info(c, "setupLdapServer", "Adding " + entriesAdded + " changes to LDAP Server");
    }

}