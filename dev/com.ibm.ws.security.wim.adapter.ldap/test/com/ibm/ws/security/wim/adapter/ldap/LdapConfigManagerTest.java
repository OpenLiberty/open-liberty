/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.wsspi.security.wim.exception.WIMException;

import test.common.SharedOutputManager;

/**
 * Test LdapConfigManager
 *
 */
public class LdapConfigManagerTest {

    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final LdapConfigManager ldapConfigManager = new LdapConfigManager();
    private final Map<String, Object> configProps = new HashMap<String, Object>();

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setup() throws WIMException {

        initialize(configProps);
        try {
            ldapConfigManager.initialize(configProps);

        } catch (WIMException e) {
            fail(e.toString());
        }
    }

    @After
    public void tearDown() {}

    @Test
    public void testGetAttributeName() {
        LdapEntity ldapEnt = new LdapEntity("PersonAccount");
        String attribute = ldapConfigManager.getAttributeName(ldapEnt, "userAccountControl:1.2.840.113556.1.4.803");
        assertEquals("userAccountControl:1.2.840.113556.1.4.803", attribute);
    }

    @Test
    public void testGetUserFilter() {
        assertEquals("(&(objectcategory=Person)(samaccountname=IBMuser*))", ldapConfigManager.getUserFilter().toString());
    }

    @Test
    public void testGetGroupFilter() {
        assertEquals("(&(objectcategory=group)(cn=IBM*))", ldapConfigManager.getGroupFilter().toString());
    }

    @Test
    public void testGetGroupIdMap() {
        assertEquals("uniqueMemberOf:uniqueMember", ldapConfigManager.getGroupMemberIdMap());
    }

    private void initialize(Map<String, Object> configProps) {
        configProps.put("ldapType", "Microsoft Active Directory");
        configProps.put("activedFilters.0.groupFilter", "(&(objectcategory=group)(cn=IBM*))");
        configProps.put("activedFilters.0.userFilter", "(&(objectcategory=Person)(samaccountname=IBMuser*))");
        configProps.put("userIdMap", "user:sn");
        configProps.put("activedFilters.0.groupMemberIdMap", "uniqueMemberOf:uniqueMember");
        configProps.put("baseDN", "cn=users,dc=vmm,dc=com");
        configProps.put("loginProperty", "uid;sn");

    }

}
