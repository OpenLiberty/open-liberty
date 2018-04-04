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

public class EntityTest {

    /** Test {@link Entity} instance. */
    @Test
    public void isMultiValuedProperty() {
        Entity entity = new Entity();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for Entity entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(Entity entity) {
        assertFalse(entity.isMultiValuedProperty("identifier"));
        assertTrue(entity.isMultiValuedProperty("viewIdentifiers"));
        assertFalse(entity.isMultiValuedProperty("parent"));
        assertTrue(entity.isMultiValuedProperty("children"));
        assertTrue(entity.isMultiValuedProperty("groups"));
        assertFalse(entity.isMultiValuedProperty("createTimestamp"));
        assertFalse(entity.isMultiValuedProperty("modifyTimestamp"));
        assertFalse(entity.isMultiValuedProperty("entitlementInfo"));
        assertFalse(entity.isMultiValuedProperty("changeType"));
    }

    @Test
    public void testToString() {

        /*
         * Test an empty instance.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:Entity " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new Entity().toString());

        /*
         * Test a fully configured instance.
         */
        Entity entity = new Entity();
        configureProperties(entity);

        /*
         * Create the expected response.
         */
        sb = new StringBuffer();
        sb.append("<wim:Entity " + RootTest.WIM_XMLNS + ">\n");
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
        sb.append("</wim:Entity>");
        assertEquals(sb.toString(), entity.toString());
    }

    /**
     * Method to configure all properties of an {@link Entity} instance.
     *
     * @param entity The {@link Entity} to configure the properties on.
     */
    static void configureProperties(Entity entity) {
        Container parent = new Container();
        parent.setCn("parent");
        Container child1 = new Container();
        child1.setCn("child1");
        Container child2 = new Container();
        child2.setCn("child2");
        Group group1 = new Group();
        group1.setCn("group1");
        Group group2 = new Group();
        group2.setCn("group2");

        entity.setIdentifier(IdentifierTypeTest.TEST_IDENTIFIER_1);
        entity.set("viewIdentifiers", ViewIdentifierTypeTest.TEST_VIEW_IDENTIFIER_1);
        entity.set("viewIdentifiers", ViewIdentifierTypeTest.TEST_VIEW_IDENTIFIER_2);
        entity.setParent(parent);
        entity.set("children", child1);
        entity.set("children", child2);
        entity.set("groups", group1);
        entity.set("groups", group2);
        entity.setCreateTimestamp(RootTest.NOW);
        entity.setModifyTimestamp(RootTest.NOW);
        entity.setEntitlementInfo(EntitlementInfoTypeTest.TEST_ENTITLEMENT_INFO_TYPE);
        entity.setChangeType("changeType");
    }
}
