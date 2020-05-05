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

public class HierarchyControlTest {
    @Test
    public void testToString() {
        /*
         * Test empty HierarchyControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:HierarchyControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new HierarchyControl().toString());

        /*
         * Set all fields on the HierarchyControl.
         */
        HierarchyControl control = new HierarchyControl();
        control.setLevel(1);
        control.setTreeView(true);

        /*
         * Set all fields on the SearchControl.
         */
        control.set("searchBases", "searchBases1");
        control.set("searchBases", "searchBases2");
        control.setCountLimit(2);
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
        sb.append("<wim:HierarchyControl level=\"1\" treeView=\"true\" countLimit=\"2\" searchLimit=\"3\" expression=\"expression\" returnSubType=\"true\" " + RootTest.WIM_XMLNS
                  + ">\n");
        sb.append("    <wim:properties>properties1</wim:properties>\n");
        sb.append("    <wim:properties>properties2</wim:properties>\n");
        sb.append("    <wim:contextProperties/>\n");
        sb.append("    <wim:contextProperties/>\n");
        sb.append("    <wim:searchBases>searchBases1</wim:searchBases>\n");
        sb.append("    <wim:searchBases>searchBases2</wim:searchBases>\n");
        sb.append("</wim:HierarchyControl>");
        assertEquals(sb.toString(), control.toString());
    }
}
