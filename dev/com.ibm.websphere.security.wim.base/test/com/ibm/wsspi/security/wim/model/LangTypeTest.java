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

public class LangTypeTest {
    @Test
    public void testToString() {
        /*
         * Test empty instance.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:LangType " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new LangType().toString());

        /*
         * Test fully configured instance.
         */
        LangType langType = new LangType();
        /*
         * Set Locality fields.
         */
        langType.setLang("lang");
        langType.setValue("value");

        sb = new StringBuffer();
        sb.append("<wim:LangType xml:lang=\"lang\" " + RootTest.WIM_XMLNS + ">value</wim:LangType>");
        assertEquals(sb.toString(), langType.toString());
    }
}
