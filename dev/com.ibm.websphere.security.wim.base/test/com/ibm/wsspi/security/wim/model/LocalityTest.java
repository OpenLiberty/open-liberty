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

public class LocalityTest {

    @Test
    public void isMultiValuedProperty() {
        Locality entity = new Locality();
        assertFalse(entity.isMultiValuedProperty("l"));
        assertFalse(entity.isMultiValuedProperty("localityName"));
        assertTrue(entity.isMultiValuedProperty("st"));
        assertTrue(entity.isMultiValuedProperty("stateOrProvinceName"));
        assertTrue(entity.isMultiValuedProperty("street"));
        assertTrue(entity.isMultiValuedProperty("seeAlso"));
        assertTrue(entity.isMultiValuedProperty("description"));

        /*
         * Check super class properties.
         */
        GeographicLocationTest.isMultiValuedProperty(entity);
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
        Locality locality = new Locality();
        /*
         * Set Locality fields.
         */
        locality.setL("l");
        locality.setLocalityName("localityName");
        locality.set("st", "st");
        locality.set("stateOrProvinceName", "stateOrProvinceName1");
        locality.set("stateOrProvinceName", "stateOrProvinceName2");
        locality.set("street", "street1");
        locality.set("street", "street2");
        locality.set("seeAlso", "seeAlso1");
        locality.set("seeAlso", "seeAlso2");
        locality.set("description", "description1");
        locality.set("description", "description2");
        EntityTest.configureProperties(locality);

        sb = new StringBuffer();
        sb.append("<wim:Locality " + RootTest.WIM_XMLNS + ">\n");
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
        sb.append("    <wim:l>l</wim:l>\n");
        sb.append("    <wim:localityName>localityName</wim:localityName>\n");
        sb.append("    <wim:st>st</wim:st>\n");
        sb.append("    <wim:stateOrProvinceName>stateOrProvinceName1</wim:stateOrProvinceName>\n");
        sb.append("    <wim:stateOrProvinceName>stateOrProvinceName2</wim:stateOrProvinceName>\n");
        sb.append("    <wim:street>street1</wim:street>\n");
        sb.append("    <wim:street>street2</wim:street>\n");
        sb.append("    <wim:seeAlso>seeAlso1</wim:seeAlso>\n");
        sb.append("    <wim:seeAlso>seeAlso2</wim:seeAlso>\n");
        sb.append("    <wim:description>description1</wim:description>\n");
        sb.append("    <wim:description>description2</wim:description>\n");
        sb.append("</wim:Locality>");
        assertEquals(sb.toString(), locality.toString());
    }
}
