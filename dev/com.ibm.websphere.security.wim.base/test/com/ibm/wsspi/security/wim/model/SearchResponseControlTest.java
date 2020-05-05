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

public class SearchResponseControlTest {
    @Test
    public void testToString() {
        SearchResponseControl control = new SearchResponseControl();

        /*
         * Test empty SearchResponseControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:SearchResponseControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), control.toString());

        /*
         * Set all fields on SearchResponseControl.
         */
        control.setHasMoreResults(true);

        sb = new StringBuffer();
        sb.append("<wim:SearchResponseControl hasMoreResults=\"true\" " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), control.toString());
    }

    @Test
    public void getSubTypes() {
        Set<String> types = SearchResponseControl.getSubTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains("ChangeResponseControl"));
    }

    @Test
    public void getSuperTypes() {
        List<String> types = new SearchResponseControl().getSuperTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains("Control"));
    }
}
