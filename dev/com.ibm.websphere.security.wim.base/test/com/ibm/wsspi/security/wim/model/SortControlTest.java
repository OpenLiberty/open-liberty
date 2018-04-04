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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.Test;

public class SortControlTest {
    @Test
    public void testToString() {
        /*
         * Test empty SortControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:SortControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new SortControl().toString());

        /*
         * Set all fields on the PropertyControl.
         */
        SortControl control = new SortControl();
        control.set("sortKeys", SortKeyTypeTest.TEST_SORT_KEY_TYPE_1);
        control.set("sortKeys", SortKeyTypeTest.TEST_SORT_KEY_TYPE_2);
        control.setLocale("locale");

        sb = new StringBuffer();
        sb.append("<wim:SortControl " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:sortKeys>\n");
        sb.append("        <wim:propertyName>propertyName1</wim:propertyName>\n");
        sb.append("        <wim:ascendingOrder>true</wim:ascendingOrder>\n");
        sb.append("    </wim:sortKeys>\n");
        sb.append("    <wim:sortKeys>\n");
        sb.append("        <wim:propertyName>propertyName2</wim:propertyName>\n");
        sb.append("        <wim:ascendingOrder>false</wim:ascendingOrder>\n");
        sb.append("    </wim:sortKeys>\n");
        sb.append("    <wim:locale>locale</wim:locale>\n");
        sb.append("</wim:SortControl>");
        assertEquals(sb.toString(), control.toString());
    }

    @Test
    public void getSubTypes() {
        Set<String> types = SortControl.getSubTypes();
        assertNotNull(types);
        assertTrue(types.isEmpty());
    }

    @Test
    public void getSuperTypes() {
        List<String> types = new SortControl().getSuperTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains("Control"));
    }
}
