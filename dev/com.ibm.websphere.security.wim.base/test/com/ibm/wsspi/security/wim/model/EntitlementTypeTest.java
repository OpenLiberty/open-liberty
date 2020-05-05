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

public class EntitlementTypeTest {
    /** Test {@link EntitlementInfo} instance 1. */
    public static final EntitlementType TEST_ENTITLEMENT_TYPE_1 = new EntitlementType();

    /** Test {@link EntitlementInfo} instance 2. */
    public static final EntitlementType TEST_ENTITLEMENT_TYPE_2 = new EntitlementType();

    static {
        TEST_ENTITLEMENT_TYPE_1.setMethod("method1");
        TEST_ENTITLEMENT_TYPE_1.setObject("object1");
        TEST_ENTITLEMENT_TYPE_1.setAttribute("attribute1");

        TEST_ENTITLEMENT_TYPE_2.setMethod("method2");
        TEST_ENTITLEMENT_TYPE_2.setObject("object2");
        TEST_ENTITLEMENT_TYPE_2.setAttribute("attribute2");
    }

    @Test
    public void testToString() {
        /*
         * Test empty object.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:EntitlementType " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new EntitlementType().toString());

        /*
         * Test fully set object.
         */
        sb = new StringBuffer();
        sb.append("<wim:EntitlementType method=\"method1\" object=\"object1\" attribute=\"attribute1\" " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), TEST_ENTITLEMENT_TYPE_1.toString());

        sb = new StringBuffer();
        sb.append("<wim:EntitlementType method=\"method2\" object=\"object2\" attribute=\"attribute2\" " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), TEST_ENTITLEMENT_TYPE_2.toString());
    }
}
