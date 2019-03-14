/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.com.unboundid;

import java.io.InputStream;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.schema.Schema;

/**
 * An in memory UnboundID LDAP server. See ContextPoolTimeoutTest for an example on how to use this.
 *
 * <br><br>
 * Basic steps: Create an instance of this class passing in your base DNs, eg. o=ibm,c=us. Add users
 * and groups via entries. Note you will need to build the LDAP tree, so if you want to create
 * uid=user1,ou=users,o=ibm,c=us, then you need an entry for ou=users first.
 *
 * <br><br>
 * In addition to the standard classes, the schema has been extended with the following classes:
 * <ul>
 * <li>wimInetOrgPerson - an extension to the inetOrgPerson class that contains WIM PersonAccount attributes</li>
 * <li>wimGroupOfNames - an extension to the groupofNames class that contains WIM Group attributes.</li>
 * <li>simulatedMicrosoftSecurityPrincipal - a simulated Active Directory user containing samAccountName and memberOf</li>
 * </ul>
 *
 * You can view the WIM schema in wimschema.ldif.
 */
public class InMemoryLDAPServer {

    private static final Class<?> c = InMemoryLDAPServer.class;

    private InMemoryDirectoryServerConfig config = null;
    private InMemoryDirectoryServer ds = null;

    /**
     * Creates a new instance of the in memory LDAP server. It initializes the directory
     * service.
     *
     * @param bases The base entries to create for this in-memory LDAP server.
     * @throws Exception
     *             If something went wrong
     */
    public InMemoryLDAPServer(String... bases) throws Exception {

        config = new InMemoryDirectoryServerConfig(bases);
        config.addAdditionalBindCredentials(getBindDN(), getBindPassword());
        config.setListenerConfigs(
                                  InMemoryListenerConfig.createLDAPConfig("LDAP", // Listener name
                                                                          null, // Listen address. (null = listen on all interfaces)
                                                                          0, // Listen port (0 = automatically choose an available port)
                                                                          null) // StartTLS factory
        ); // Client factory

        /*
         * Merge the default schema with our WIM schema. The WIM schema adds wimInetOrgPerson,
         * wimGroupOfNames, and simulatedMicrosoftSecurityPrincipal. The wimInetOrgPerson
         * adds WIM properties as LDAP attributes so that we do not need to set up a mapping
         * between the WIM properties and other existing LDAP attributes.
         */

        InputStream in = getClass().getResourceAsStream("/resources/wimschema.ldif");
        Schema wimschema = Schema.getSchema(in);
        Schema schema = Schema.mergeSchemas(Schema.getDefaultStandardSchema(), wimschema);
        config.setSchema(schema);
        ds = new InMemoryDirectoryServer(config);
        ds.startListening();
    }

    /**
     * Get the default administrative bind distinguished name.
     *
     * @return The default administrative bind distinguished name.
     * @see #getBindPassword()
     */
    public static String getBindDN() {
        return "uid=admin,ou=system";
    }

    /**
     * Get the default administrative bind password.
     *
     * @return The default administrative bind password.
     * @see #getBindDN()
     */
    public static String getBindPassword() {
        return "secret";
    }

    /**
     * Get the {@link InMemoryDirectoryServer} instance.
     *
     * @return The {@link InMemoryDirectoryServer} instance.
     */
    public InMemoryDirectoryServer getLdapServer() {
        return ds;
    }

    /**
     * Add an entry to the InMemoryDirectoryServer.
     *
     * @param entry The entry to be added.
     */
    public void add(Entry entry) throws LDAPException {
        ds.add(entry);
    }

    /**
     * Shut down the InMemoryDirectoryServer.
     */
    public void shutDown() {
        ds.shutDown(true);
    }

    /**
     * Shut down the InMemoryDirectoryServer.
     *
     * @param b true to close all existing connections, or false to stop accepting new connections.
     */
    public void shutDown(boolean b) {
        ds.shutDown(b);

    }

    /**
     * Get the port the server is listening to.
     *
     * @return the port this directory server is listening to
     */
    public int getListenPort() {
        return ds.getListenPort();
    }

    /**
     * @param userDn
     * @param modification
     * @throws LDAPException
     */
    public void modify(String userDn, Modification modification) throws LDAPException {
        ds.modify(userDn, modification);

    }

    /**
     * Delete an entry from the LDAP server, eg. delete("uid=user1,ou=users,o=ibm,c=us")
     *
     * @param dn the DN to remove from the directory server
     * @throws LDAPException
     */
    public void delete(String dn) throws LDAPException {
        DeleteRequest deleteRequest = new DeleteRequest(dn);
        delete(deleteRequest);
    }

    /**
     * Delete an entry from the LDAP server.
     *
     * @param dr the DeleteRequest to issue on the directory server
     * @throws LDAPException
     */
    public void delete(DeleteRequest dr) throws LDAPException {
        ds.delete(dr);
    }
}
