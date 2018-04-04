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

public class ContextTest {

    @Test
    public void testToString() {

        /*
         * Test empty instance.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:Context " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new Context().toString());

        /*
         * Test fully configured instance.
         */
        Context context = new Context();
        context.setKey("key");
        context.setValue("value");

        sb = new StringBuffer();
        sb.append("<wim:Context " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:key>key</wim:key>\n");
        sb.append("    <wim:value xsi:type=\"xs:string\">value</wim:value>\n");
        sb.append("</wim:Context>");

        assertEquals(sb.toString(), context.toString());
    }
}
