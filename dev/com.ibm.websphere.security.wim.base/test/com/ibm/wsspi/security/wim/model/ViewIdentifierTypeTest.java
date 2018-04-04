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

public class ViewIdentifierTypeTest {
    /** Test {@link ViewIdentifierType} instance 1. */
    public static final ViewIdentifierType TEST_VIEW_IDENTIFIER_1 = new ViewIdentifierType();

    /** Test {@link ViewIdentifierType} instance 2. */
    public static final ViewIdentifierType TEST_VIEW_IDENTIFIER_2 = new ViewIdentifierType();

    static {
        TEST_VIEW_IDENTIFIER_1.setViewName("viewName1");
        TEST_VIEW_IDENTIFIER_1.setViewEntryUniqueId("viewEntryUniqueId1");
        TEST_VIEW_IDENTIFIER_1.setViewEntryName("viewEntryName1");

        TEST_VIEW_IDENTIFIER_2.setViewName("viewName2");
        TEST_VIEW_IDENTIFIER_2.setViewEntryUniqueId("viewEntryUniqueId2");
        TEST_VIEW_IDENTIFIER_2.setViewEntryName("viewEntryName2");
    }

    @Test
    public void testToString() {
        /*
         * Test empty object.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:ViewIdentifierType " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new ViewIdentifierType().toString());

        /*
         * Test fully set object.
         */
        sb = new StringBuffer();
        sb.append("<wim:ViewIdentifierType viewName=\"viewName1\" viewEntryUniqueId=\"viewEntryUniqueId1\" viewEntryName=\"viewEntryName1\" " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), TEST_VIEW_IDENTIFIER_1.toString());

        sb = new StringBuffer();
        sb.append("<wim:ViewIdentifierType viewName=\"viewName2\" viewEntryUniqueId=\"viewEntryUniqueId2\" viewEntryName=\"viewEntryName2\" " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), TEST_VIEW_IDENTIFIER_2.toString());
    }

    @Test
    public void getSubTypes() {
        Set<String> types = ViewIdentifierType.getSubTypes();
        assertNotNull(types);
        assertEquals(0, types.size());
    }

    @Test
    public void getSuperTypes() {
        List<String> types = new ViewIdentifierType().getSuperTypes();
        assertNotNull(types);
        assertEquals(0, types.size());
    }
}
