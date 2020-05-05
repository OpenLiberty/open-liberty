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

public class CacheControlTest {
    @Test
    public void testToString() {
        /*
         * Test empty CacheControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:CacheControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new CacheControl().toString());

        /*
         * Set all fields on the CacheControl.
         */
        CacheControl control = new CacheControl();
        control.setMode("mode");

        sb = new StringBuffer();
        sb.append("<wim:CacheControl " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:mode>mode</wim:mode>\n");
        sb.append("</wim:CacheControl>");
        assertEquals(sb.toString(), control.toString());
    }
}
