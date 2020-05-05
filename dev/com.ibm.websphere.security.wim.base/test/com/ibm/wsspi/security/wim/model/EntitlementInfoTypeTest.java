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

import org.junit.Test;

public class EntitlementInfoTypeTest {

    /** Test {@link EntitlementInfoType} instance. */
    public static final EntitlementInfoType TEST_ENTITLEMENT_INFO_TYPE = new EntitlementInfoType();

    static {
        TEST_ENTITLEMENT_INFO_TYPE.set("roles", "role1");
        TEST_ENTITLEMENT_INFO_TYPE.set("roles", "role2");
        TEST_ENTITLEMENT_INFO_TYPE.set("entitlements", EntitlementTypeTest.TEST_ENTITLEMENT_TYPE_1);
        TEST_ENTITLEMENT_INFO_TYPE.set("entitlements", EntitlementTypeTest.TEST_ENTITLEMENT_TYPE_2);
        TEST_ENTITLEMENT_INFO_TYPE.setEntitlementCheckResult(true);
    }

    @Test
    public void testToString() {
        /*
         * Test empty object.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:IdentifierType " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new IdentifierType().toString());

        /*
         * Test fully set object.
         */
        sb = new StringBuffer();
        sb.append("<wim:EntitlementInfoType " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:roles>role1</wim:roles>\n");
        sb.append("    <wim:roles>role2</wim:roles>\n");
        sb.append("    <wim:entitlements method=\"method1\" object=\"object1\" attribute=\"attribute1\"/>\n");
        sb.append("    <wim:entitlements method=\"method2\" object=\"object2\" attribute=\"attribute2\"/>\n");
        sb.append("    <wim:entitlementCheckResult>true</wim:entitlementCheckResult>\n");
        sb.append("</wim:EntitlementInfoType>");
        assertEquals(sb.toString(), TEST_ENTITLEMENT_INFO_TYPE.toString());
    }
}
