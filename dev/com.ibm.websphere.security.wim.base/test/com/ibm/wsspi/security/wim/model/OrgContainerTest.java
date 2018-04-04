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

public class OrgContainerTest {

    static {
        /*
         * Set Locality fields.
         */

    }

    @Test
    public void isMultiValuedProperty() {
        OrgContainer entity = new OrgContainer();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for OrgContainer entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(OrgContainer entity) {
        assertFalse(entity.isMultiValuedProperty("o"));
        assertFalse(entity.isMultiValuedProperty("ou"));
        assertFalse(entity.isMultiValuedProperty("dc"));
        assertFalse(entity.isMultiValuedProperty("cn"));
        assertTrue(entity.isMultiValuedProperty("telephoneNumber"));
        assertTrue(entity.isMultiValuedProperty("facsimileTelephoneNumber"));
        assertTrue(entity.isMultiValuedProperty("postalAddress"));
        assertTrue(entity.isMultiValuedProperty("l"));
        assertTrue(entity.isMultiValuedProperty("localityName"));
        assertTrue(entity.isMultiValuedProperty("st"));
        assertTrue(entity.isMultiValuedProperty("stateOrProvinceName"));
        assertTrue(entity.isMultiValuedProperty("street"));
        assertTrue(entity.isMultiValuedProperty("postalCode"));
        assertFalse(entity.isMultiValuedProperty("businessAddress"));
        assertTrue(entity.isMultiValuedProperty("description"));
        assertTrue(entity.isMultiValuedProperty("businessCategory"));
        assertTrue(entity.isMultiValuedProperty("seeAlso"));

        /*
         * Check super class properties.
         */
        PartyTest.isMultiValuedProperty(entity);
    }

    @Test
    public void testToString() {
        /*
         * Test empty instance.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:Locality " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new Locality().toString());

        /*
         * Test fully configured instance.
         */
        OrgContainer orgContainer = new OrgContainer();
        orgContainer.setO("o");
        orgContainer.setOu("ou");
        orgContainer.setDc("dc");
        orgContainer.setCn("cn");
        orgContainer.set("telephoneNumber", "telephoneNumber1");
        orgContainer.set("telephoneNumber", "telephoneNumber2");
        orgContainer.set("facsimileTelephoneNumber", "facsimileTelephoneNumber1");
        orgContainer.set("facsimileTelephoneNumber", "facsimileTelephoneNumber2");
        orgContainer.set("postalAddress", "postalAddress1");
        orgContainer.set("postalAddress", "postalAddress2");
        orgContainer.set("l", "l1");
        orgContainer.set("l", "l2");
        orgContainer.set("localityName", "localityName1");
        orgContainer.set("localityName", "localityName2");
        orgContainer.set("st", "st1");
        orgContainer.set("st", "st2");
        orgContainer.set("stateOrProvinceName", "stateOrProvinceName1");
        orgContainer.set("stateOrProvinceName", "stateOrProvinceName2");
        orgContainer.set("street", "street1");
        orgContainer.set("street", "street2");
        orgContainer.set("postalCode", "postalCode1");
        orgContainer.set("postalCode", "postalCode2");
        orgContainer.set("businessAddress", AddressTypeTest.TEST_ADDRESS_TYPE);
        orgContainer.set("description", "description1");
        orgContainer.set("description", "description2");
        orgContainer.set("businessCategory", "businessCategory1");
        orgContainer.set("businessCategory", "businessCategory2");
        orgContainer.set("seeAlso", "seeAlso1");
        orgContainer.set("seeAlso", "seeAlso2");
        RolePlayerTest.configureProperties(orgContainer);

        sb = new StringBuffer();
        sb.append("<wim:OrgContainer " + RootTest.WIM_XMLNS + ">\n");
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
        sb.append("    <wim:o>o</wim:o>\n");
        sb.append("    <wim:ou>ou</wim:ou>\n");
        sb.append("    <wim:dc>dc</wim:dc>\n");
        sb.append("    <wim:cn>cn</wim:cn>\n");
        sb.append("    <wim:telephoneNumber>telephoneNumber1</wim:telephoneNumber>\n");
        sb.append("    <wim:telephoneNumber>telephoneNumber2</wim:telephoneNumber>\n");
        sb.append("    <wim:facsimileTelephoneNumber>facsimileTelephoneNumber1</wim:facsimileTelephoneNumber>\n");
        sb.append("    <wim:facsimileTelephoneNumber>facsimileTelephoneNumber2</wim:facsimileTelephoneNumber>\n");
        sb.append("    <wim:postalAddress>postalAddress1</wim:postalAddress>\n");
        sb.append("    <wim:postalAddress>postalAddress2</wim:postalAddress>\n");
        sb.append("    <wim:l>l1</wim:l>\n");
        sb.append("    <wim:l>l2</wim:l>\n");
        sb.append("    <wim:localityName>localityName1</wim:localityName>\n");
        sb.append("    <wim:localityName>localityName2</wim:localityName>\n");
        sb.append("    <wim:st>st1</wim:st>\n");
        sb.append("    <wim:st>st2</wim:st>\n");
        sb.append("    <wim:stateOrProvinceName>stateOrProvinceName1</wim:stateOrProvinceName>\n");
        sb.append("    <wim:stateOrProvinceName>stateOrProvinceName2</wim:stateOrProvinceName>\n");
        sb.append("    <wim:street>street1</wim:street>\n");
        sb.append("    <wim:street>street2</wim:street>\n");
        sb.append("    <wim:postalCode>postalCode1</wim:postalCode>\n");
        sb.append("    <wim:postalCode>postalCode2</wim:postalCode>\n");
        sb.append("    <wim:businessAddress>\n");
        sb.append("        <wim:nickName>nickName</wim:nickName>\n");
        sb.append("        <wim:street>street1</wim:street>\n");
        sb.append("        <wim:street>street2</wim:street>\n");
        sb.append("        <wim:city>city</wim:city>\n");
        sb.append("        <wim:stateOrProvinceName>stateOrProvinceName</wim:stateOrProvinceName>\n");
        sb.append("        <wim:postalCode>postalCode</wim:postalCode>\n");
        sb.append("        <wim:countryName>countryName</wim:countryName>\n");
        sb.append("    </wim:businessAddress>\n");
        sb.append("    <wim:description>description1</wim:description>\n");
        sb.append("    <wim:description>description2</wim:description>\n");
        sb.append("    <wim:businessCategory>businessCategory1</wim:businessCategory>\n");
        sb.append("    <wim:businessCategory>businessCategory2</wim:businessCategory>\n");
        sb.append("    <wim:seeAlso>seeAlso1</wim:seeAlso>\n");
        sb.append("    <wim:seeAlso>seeAlso2</wim:seeAlso>\n");
        sb.append("</wim:OrgContainer>");
        assertEquals(sb.toString(), orgContainer.toString());
    }
}
