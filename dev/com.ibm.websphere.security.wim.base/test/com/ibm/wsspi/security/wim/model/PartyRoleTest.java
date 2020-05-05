/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PartyRoleTest {

    /** Test {@link PartyRole} instance 1. */
    public static final PartyRole TEST_PARTY_ROLE_1 = new PartyRole();

    /** Test {@link PartyRole} instance 2. */
    public static final PartyRole TEST_PARTY_ROLE_2 = new PartyRole();

    static {
        Container parent1 = new Container();
        parent1.setCn("parent1");

        Container parent2 = new Container();
        parent2.setCn("parent2");

        Person rolePlayer1 = new Person();
        rolePlayer1.setCn("rolePlayer1");
        Person rolePlayer2 = new Person();
        rolePlayer2.setCn("rolePlayer2");

        TEST_PARTY_ROLE_1.setParent(parent1);
        TEST_PARTY_ROLE_1.setPrimaryRolePlayer(rolePlayer1);

        TEST_PARTY_ROLE_2.setParent(parent2);
        TEST_PARTY_ROLE_2.setPrimaryRolePlayer(rolePlayer2);
    }

    @Test
    public void isMultiValuedProperty() {
        PartyRole entity = new PartyRole();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for PartyRole entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(PartyRole entity) {
        assertFalse(entity.isMultiValuedProperty("primaryRolePlayer"));
        assertTrue(entity.isMultiValuedProperty("relatedRolePlayer"));

        /*
         * Check super class properties.
         */
        RolePlayerTest.isMultiValuedProperty(entity);
    }

    @Test
    public void testToString() {

        /*
         * Test empty entity.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:PartyRole " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new PartyRole().toString());

        /*
         * RolePlayer Properties
         */
        PartyRole partyRole = new PartyRole();
        configureProperties(partyRole);

        /*
         * Create the expected response.
         */
        sb = new StringBuffer();
        sb.append("<wim:PartyRole " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:identifier uniqueId=\"uniqueId1\" uniqueName=\"uniqueName1\" externalId=\"externalId1\" externalName=\"externalName1\" repositoryId=\"repositoryId1\"/>\n");
        sb.append("    <wim:viewIdentifiers viewName=\"viewName1\" viewEntryUniqueId=\"viewEntryUniqueId1\" viewEntryName=\"viewEntryName1\"/>\n");
        sb.append("    <wim:viewIdentifiers viewName=\"viewName2\" viewEntryUniqueId=\"viewEntryUniqueId2\" viewEntryName=\"viewEntryName2\"/>\n");
        sb.append("    <wim:parent xsi:type=\"wim:Container\">\n");
        sb.append("        <wim:cn>parent</wim:cn>\n");
        sb.append("    </wim:parent>\n");
        sb.append("    <wim:children xsi:type=\"wim:Container\">\n");
        sb.append("        <wim:cn>child1</wim:cn>\n");
        sb.append("    </wim:children>\n");
        sb.append("    <wim:children xsi:type=\"wim:Container\">\n");
        sb.append("        <wim:cn>child2</wim:cn>\n");
        sb.append("    </wim:children>\n");
        sb.append("    <wim:groups>\n");
        sb.append("        <wim:cn>group1</wim:cn>\n");
        sb.append("    </wim:groups>\n");
        sb.append("    <wim:groups>\n");
        sb.append("        <wim:cn>group2</wim:cn>\n");
        sb.append("    </wim:groups>\n");
        sb.append("    <wim:createTimestamp>" + RootTest.NOW_STRING + "</wim:createTimestamp>\n");
        sb.append("    <wim:modifyTimestamp>" + RootTest.NOW_STRING + "</wim:modifyTimestamp>\n");
        sb.append("    <wim:entitlementInfo>\n");
        sb.append("        <wim:roles>role1</wim:roles>\n");
        sb.append("        <wim:roles>role2</wim:roles>\n");
        sb.append("        <wim:entitlements method=\"method1\" object=\"object1\" attribute=\"attribute1\"/>\n");
        sb.append("        <wim:entitlements method=\"method2\" object=\"object2\" attribute=\"attribute2\"/>\n");
        sb.append("        <wim:entitlementCheckResult>true</wim:entitlementCheckResult>\n");
        sb.append("    </wim:entitlementInfo>\n");
        sb.append("    <wim:changeType>changeType</wim:changeType>\n");
        sb.append("    <wim:partyRoles>\n");
        sb.append("        <wim:parent xsi:type=\"wim:Container\">\n");
        sb.append("            <wim:cn>parent</wim:cn>\n");
        sb.append("        </wim:parent>\n");
        sb.append("    </wim:partyRoles>\n");
        sb.append("    <wim:primaryRolePlayer/>\n");
        sb.append("    <wim:relatedRolePlayer/>\n");
        sb.append("    <wim:relatedRolePlayer/>\n");
        sb.append("</wim:PartyRole>");
        assertEquals(sb.toString(), partyRole.toString());
    }

    /**
     * Method to configure all properties of a {@link PartyRole} instance.
     *
     * @param entity The {@link RolePlayer} to configure the properties on.
     */
    static void configureProperties(PartyRole partyRole) {
        RolePlayer primaryRolePlayer = new RolePlayer();
        RolePlayer relatedRolePlayer1 = new RolePlayer();
        RolePlayer relatedRolePlayer2 = new RolePlayer();

        partyRole.set("primaryRolePlayer", primaryRolePlayer);
        partyRole.set("relatedRolePlayer", relatedRolePlayer1);
        partyRole.set("relatedRolePlayer", relatedRolePlayer2);

        RolePlayerTest.configureProperties(partyRole);
    }
}
