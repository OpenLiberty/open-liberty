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

public class PageControlTest {

    @Test
    public void testToString() {

        /*
         * Test empty PageControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:PageControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new PageControl().toString());

        /*
         * Set all fields on the PageControl.
         */
        PageControl control = new PageControl();
        control.setSize(0);
        control.setStartIndex(1);

        sb = new StringBuffer();
        sb.append("<wim:PageControl size=\"0\" startIndex=\"1\" " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), control.toString());
    }
}
