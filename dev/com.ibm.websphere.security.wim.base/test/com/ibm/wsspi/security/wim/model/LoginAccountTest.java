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

public class LoginAccountTest {

    @Test
    public void isMultiValuedProperty() {
        LoginAccount entity = new LoginAccount();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for LoginAccount entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(LoginAccount entity) {
        assertFalse(entity.isMultiValuedProperty("principalName"));
        assertFalse(entity.isMultiValuedProperty("password"));
        assertFalse(entity.isMultiValuedProperty("realm"));
        assertTrue(entity.isMultiValuedProperty("certificate"));

        /*
         * Check super class properties.
         */
        PartyTest.isMultiValuedProperty(entity);
    }

    @Test
    public void testToString() {

        /*
         * Test empty entity.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:LoginAccount " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new LoginAccount().toString());

        /*
         * LoginAccount properties.
         */
        LoginAccount loginAccount = new LoginAccount();
        configureProperties(loginAccount);

        /*
         * Create the expected response.
         */
        sb = new StringBuffer();
        sb.append("<wim:LoginAccount " + RootTest.WIM_XMLNS + ">\n");
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
        sb.append("    <wim:principalName>principalName</wim:principalName>\n");
        sb.append("    <wim:password>*****</wim:password>\n");
        sb.append("    <wim:realm>realm</wim:realm>\n");
        sb.append("    <wim:certificate>Y2VydGlmaWNhdGUx</wim:certificate>\n");
        sb.append("    <wim:certificate>Y2VydGlmaWNhdGUy</wim:certificate>\n");
        sb.append("</wim:LoginAccount>");
        assertEquals(sb.toString(), loginAccount.toString());
    }

    /**
     * Method to configure all properties of a {@link LoginAccount} instance.
     *
     * @param entity The {@link LoginAccount} to configure the properties on.
     */
    static void configureProperties(LoginAccount account) {
        account.setPassword("password".getBytes());
        account.setPrincipalName("principalName");
        account.setRealm("realm");
        account.set("certificate", "certificate1".getBytes());
        account.set("certificate", "certificate2".getBytes());
        RolePlayerTest.configureProperties(account);
    }
}