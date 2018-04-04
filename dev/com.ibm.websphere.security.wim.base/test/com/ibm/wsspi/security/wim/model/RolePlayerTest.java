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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RolePlayerTest {

    /** Test {@link RolePlayer} instance. */
    public static final RolePlayer TEST_ROLE_PLAYER = new RolePlayer();

    @Test
    public void isMultiValuedProperty() {
        RolePlayer entity = new RolePlayer();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for RolePlayer entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(RolePlayer entity) {
        assertTrue(entity.isMultiValuedProperty("partyRoles"));

        /*
         * Check super class properties.
         */
        EntityTest.isMultiValuedProperty(entity);
    }

    @Test
    public void testToString() {

        /*
         * Test empty entity.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:RolePlayer " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new RolePlayer().toString());

        /*
         * RolePlayer Properties
         */
        RolePlayer rolePlayer = new RolePlayer();
        configureProperties(rolePlayer);

        /*
         * Create the expected response.
         */
        sb = new StringBuffer();
        sb.append("<wim:RolePlayer " + RootTest.WIM_XMLNS + ">\n");
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
        sb.append("</wim:RolePlayer>");
        assertEquals(sb.toString(), rolePlayer.toString());
    }

    /**
     * Method to configure all properties of a {@link RolePlayer} instance.
     *
     * @param entity The {@link RolePlayer} to configure the properties on.
     */
    static void configureProperties(RolePlayer rolePlayer) {
        Container parent = new Container();
        parent.setCn("parent");

        PartyRole partyRole = new PartyRole();
        partyRole.setParent(parent);

        rolePlayer.set("partyRoles", partyRole);
        EntityTest.configureProperties(rolePlayer);
    }
}
