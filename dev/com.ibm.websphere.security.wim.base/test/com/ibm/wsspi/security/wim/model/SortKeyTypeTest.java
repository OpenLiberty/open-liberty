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
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;

import org.junit.Test;

public class SortKeyTypeTest {

    /** Test {@link SortKeyType} instance 1. */
    public static final SortKeyType TEST_SORT_KEY_TYPE_1 = new SortKeyType();

    /** Test {@link SortKeyType} instance 2. */
    public static final SortKeyType TEST_SORT_KEY_TYPE_2 = new SortKeyType();

    static {
        TEST_SORT_KEY_TYPE_1.setPropertyName("propertyName1");
        TEST_SORT_KEY_TYPE_1.setAscendingOrder(true);

        TEST_SORT_KEY_TYPE_2.setPropertyName("propertyName2");
        TEST_SORT_KEY_TYPE_2.setAscendingOrder(false);
    }

    @Test
    public void testToString() {
        /*
         * Test empty SortKeyType.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:SortKeyType " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:ascendingOrder>false</wim:ascendingOrder>\n");
        sb.append("</wim:SortKeyType>");
        assertEquals(sb.toString(), new SortKeyType().toString());

        /*
         * Test fully set instance.
         */
        sb = new StringBuffer();
        sb.append("<wim:SortKeyType " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:propertyName>propertyName1</wim:propertyName>\n");
        sb.append("    <wim:ascendingOrder>true</wim:ascendingOrder>\n");
        sb.append("</wim:SortKeyType>");
        assertEquals(sb.toString(), TEST_SORT_KEY_TYPE_1.toString());

        sb = new StringBuffer();
        sb.append("<wim:SortKeyType " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:propertyName>propertyName2</wim:propertyName>\n");
        sb.append("    <wim:ascendingOrder>false</wim:ascendingOrder>\n");
        sb.append("</wim:SortKeyType>");
        assertEquals(sb.toString(), TEST_SORT_KEY_TYPE_2.toString());
    }

    @Test
    public void getSubTypes() {
        Set<String> types = SortKeyType.getSubTypes();
        assertNotNull(types);
        assertEquals(0, types.size());
    }

    @Test
    public void getSuperTypes() {
        List<String> types = new SortKeyType().getSuperTypes();
        assertNotNull(types);
        assertEquals(0, types.size());
    }
}
