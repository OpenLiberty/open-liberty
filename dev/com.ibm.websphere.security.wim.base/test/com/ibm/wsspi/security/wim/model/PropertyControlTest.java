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

import com.ibm.wsspi.security.wim.model.PropertyControl.ContextProperties;

public class PropertyControlTest {

    /** Test {@link ContextProperties} instance 1. */
    public static final ContextProperties TEST_CONTEXT_PROPERTY_1 = new PropertyControl.ContextProperties();

    /** Test {@link ContextProperties} instance 2. */
    public static final ContextProperties TEST_CONTEXT_PROPERTY_2 = new PropertyControl.ContextProperties();

    static {
        TEST_CONTEXT_PROPERTY_1.setLang("lang1");
        TEST_CONTEXT_PROPERTY_1.setValue("value1");

        TEST_CONTEXT_PROPERTY_2.setLang("lang2");
        TEST_CONTEXT_PROPERTY_2.setValue("value2");
    }

    @Test
    public void testToString() {
        /*
         * Test empty PropertyControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:PropertyControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new PropertyControl().toString());

        /*
         * Set all fields on the PropertyControl.
         */
        PropertyControl control = new PropertyControl();
        control.set("properties", "properties1");
        control.set("properties", "properties2");
        control.set("contextProperties", TEST_CONTEXT_PROPERTY_1);
        control.set("contextProperties", TEST_CONTEXT_PROPERTY_2);

        sb = new StringBuffer();
        sb.append("<wim:PropertyControl " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:properties>properties1</wim:properties>\n");
        sb.append("    <wim:properties>properties2</wim:properties>\n");
        sb.append("    <wim:contextProperties xml:lang=\"lang1\">value1</wim:contextProperties>\n");
        sb.append("    <wim:contextProperties xml:lang=\"lang2\">value2</wim:contextProperties>\n");
        sb.append("</wim:PropertyControl>");
        assertEquals(sb.toString(), control.toString());
    }
}
