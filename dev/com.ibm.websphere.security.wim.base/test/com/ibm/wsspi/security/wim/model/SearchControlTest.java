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

public class SearchControlTest {

    @Test
    public void testToString() {

        /*
         * Test empty SearchControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:SearchControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new SearchControl().toString());

        /*
         * Set all fields on the SearchControl.
         */
        SearchControl control = new SearchControl();
        control.set("searchBases", "searchBases1");
        control.set("searchBases", "searchBases2");
        control.setCountLimit(1);
        control.setExpression("expression");
        control.setReturnSubType(true);
        control.setSearchLimit(3);

        /*
         * Set all fields on the PropertyControl.
         */
        control.set("properties", "properties1");
        control.set("properties", "properties2");
        control.set("contextProperties", new PropertyControl.ContextProperties());
        control.set("contextProperties", new PropertyControl.ContextProperties());

        sb = new StringBuffer();
        sb.append("<wim:SearchControl countLimit=\"1\" searchLimit=\"3\" expression=\"expression\" returnSubType=\"true\" " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:properties>properties1</wim:properties>\n");
        sb.append("    <wim:properties>properties2</wim:properties>\n");
        sb.append("    <wim:contextProperties/>\n");
        sb.append("    <wim:contextProperties/>\n");
        sb.append("    <wim:searchBases>searchBases1</wim:searchBases>\n");
        sb.append("    <wim:searchBases>searchBases2</wim:searchBases>\n");
        sb.append("</wim:SearchControl>");
        assertEquals(sb.toString(), control.toString());
    }
}
