/*******************************************************************************
 * Copyright (c) 2014,2018 IBM Corporation and others.
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

public class GroupTest {

    @Test
    public void testCaseSentitive() {
        Group group = new Group();
        assertEquals("String", group.getDataType("cn"));
    }

    @Test
    public void testCaseInSentitive() {
        Group group = new Group();
        assertEquals(null, group.getDataType("CN"));
    }

    @Test
    public void isMultiValuedProperty() {
        Group entity = new Group();
        Group.addExtendedProperty("extendedProperty1", "String", false, null);
        Group.addExtendedProperty("extendedProperty2", "String", true, null);

        /*
         * Test standard properties.
         */
        assertFalse(entity.isMultiValuedProperty("cn"));
        assertTrue(entity.isMultiValuedProperty("members"));
        assertTrue(entity.isMultiValuedProperty("displayName"));
        assertTrue(entity.isMultiValuedProperty("description"));
        assertTrue(entity.isMultiValuedProperty("businessCategory"));
        assertTrue(entity.isMultiValuedProperty("seeAlso"));

        /*
         * Check extended properties.
         */
        assertFalse(entity.isMultiValuedProperty("extendedProperty1"));
        assertTrue(entity.isMultiValuedProperty("extendedProperty2"));

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
        sb.append("<wim:Group " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new Group().toString());

        /*
         * Group properties.
         */
        /** Test {@link Group} instance 1. */
        Group group = new Group();
        PersonAccount member1 = new PersonAccount();
        member1.setCn("member1");
        PersonAccount member2 = new PersonAccount();
        member2.setCn("member2");

        group.setCn("cn");
        group.set("members", member1);
        group.set("members", member2);
        group.set("displayName", "displayName1");
        group.set("displayName", "displayName2");
        group.set("initials", "initials");
        group.set("description", "description1");
        group.set("description", "description2");
        group.set("businessCategory", "businessCategory1");
        group.set("businessCategory", "businessCategory2");
        group.set("seeAlso", "seeAlso1");
        group.set("seeAlso", "seeAlso2");

        /*
         * Extended properties.
         */
        Group.addExtendedProperty("multiValuedExtProp", "String", true, null);
        Group.addExtendedProperty("multiValuedExtByte", "byte[]", true, null);
        Group.addExtendedProperty("singleValuedExtProp", "String", false, null);
        Group.addExtendedProperty("singleValuedExtByte", "byte[]", false, null);
        group.set("multiValuedExtProp", "multiValue1");
        group.set("multiValuedExtProp", "multiValue2");
        group.set("multiValuedExtByte", "multi1".getBytes());
        group.set("multiValuedExtByte", "multi2".getBytes());
        group.set("singleValuedExtProp", "singleValue");
        group.set("singleValuedExtByte", "single".getBytes());

        RolePlayerTest.configureProperties(group);

        /*
         * Create the expected response.
         */
        sb = new StringBuffer();
        sb.append("<wim:Group " + RootTest.WIM_XMLNS + ">\n");
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
        sb.append("    <wim:cn>cn</wim:cn>\n");
        sb.append("    <wim:members xsi:type=\"wim:PersonAccount\">\n");
        sb.append("        <wim:cn>member1</wim:cn>\n");
        sb.append("    </wim:members>\n");
        sb.append("    <wim:members xsi:type=\"wim:PersonAccount\">\n");
        sb.append("        <wim:cn>member2</wim:cn>\n");
        sb.append("    </wim:members>\n");
        sb.append("    <wim:displayName>displayName1</wim:displayName>\n");
        sb.append("    <wim:displayName>displayName2</wim:displayName>\n");
        sb.append("    <wim:description>description1</wim:description>\n");
        sb.append("    <wim:description>description2</wim:description>\n");
        sb.append("    <wim:businessCategory>businessCategory1</wim:businessCategory>\n");
        sb.append("    <wim:businessCategory>businessCategory2</wim:businessCategory>\n");
        sb.append("    <wim:seeAlso>seeAlso1</wim:seeAlso>\n");
        sb.append("    <wim:seeAlso>seeAlso2</wim:seeAlso>\n");
        sb.append("    <wim:extendedProperties>\n");
        sb.append("        <item>\n");
        sb.append("            <key>multiValuedExtByte</key>\n");
        sb.append("            <values xsi:type=\"xs:base64Binary\">bXVsdGkx</values>\n");
        sb.append("            <values xsi:type=\"xs:base64Binary\">bXVsdGky</values>\n");
        sb.append("        </item>\n");
        sb.append("        <item>\n");
        sb.append("            <key>multiValuedExtProp</key>\n");
        sb.append("            <values xsi:type=\"xs:string\">multiValue1</values>\n");
        sb.append("            <values xsi:type=\"xs:string\">multiValue2</values>\n");
        sb.append("        </item>\n");
        sb.append("        <item>\n");
        sb.append("            <key>singleValuedExtByte</key>\n");
        sb.append("            <value xsi:type=\"xs:base64Binary\">c2luZ2xl</value>\n");
        sb.append("        </item>\n");
        sb.append("        <item>\n");
        sb.append("            <key>singleValuedExtProp</key>\n");
        sb.append("            <value xsi:type=\"xs:string\">singleValue</value>\n");
        sb.append("        </item>\n");
        sb.append("    </wim:extendedProperties>\n");
        sb.append("</wim:Group>");
        assertEquals(sb.toString(), group.toString());
    }
}
