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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Assume;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.topology.impl.LibertyServer;
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
public abstract class URAPIs_LDAP_NestedGroupBase {

    // Keys to help readability of the test

    private final String topGroup = "nested_g1";
    private final String topGroupUser = "topng_user1";
    private final String embeddedGroup = "embedded_group1";
    private final String nestedUser = "ng_user1";

    protected UserRegistryServletConnection myServlet;
    protected LibertyServer myServer;
    protected Class<?> logClass;
    protected BasicAuthClient myClient;

    abstract String getSuffix();

    abstract String getLDAPRealm();

    abstract String getCN();

    public URAPIs_LDAP_NestedGroupBase(LibertyServer server, Class<?> clazz, BasicAuthClient client, UserRegistryServletConnection servlet) {
        myServer = server;
        logClass = clazz;
        myClient = client;
        myServlet = servlet;
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user in top level nested group.
     * This test expects that only the top level nested group, nested_g1, will be returned.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserInTopGroup() throws Exception {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = topGroupUser;
        Log.info(logClass, "getGroupsForUserInTopGroup", "Checking with a valid user.");
        List<String> list = myServlet.getGroupsForUser(user);
        System.out.println("List of groups : " + list.toString());
        assertTrue(list.contains(getCN() + topGroup + getSuffix()));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user in a nested group.
     * This test expects that both the top level and nested group will be returned.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getGroupsForUserInNestedGroup() throws Exception {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = nestedUser;
        Log.info(logClass, "getGroupsForUserInNestedGroup", "Checking with a valid user.");
        List<String> list = myServlet.getGroupsForUser(user);
        System.out.println("List of groups : " + list.toString());
        assertTrue(list.contains(getCN() + topGroup + getSuffix()) && list.contains(getCN() + embeddedGroup + getSuffix()));
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user in a nested group.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsForUserInNestedGroup() throws Exception {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = getCN() + nestedUser + getSuffix();
        Log.info(logClass, "getUniqueGroupIdsForUserInNestedGroup", "Checking with a valid user.");
        List<String> list = myServlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.contains(getCN() + topGroup + getSuffix()) && list.contains(getCN() + embeddedGroup + getSuffix()));
        assertEquals("There should be two entries", 2, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUniqueGroupIdsForTopNestedGroup() throws Exception {
        // This test will only be executed when using physical LDAP server
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = getCN() + topGroupUser + getSuffix();
        Log.info(logClass, "getUniqueGroupIdsForTopNestedGroup", "Checking with a valid user.");
        List<String> list = myServlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.contains(getCN() + topGroup + getSuffix()));
        assertEquals("There should only be one entry", 1, list.size());
    }

}