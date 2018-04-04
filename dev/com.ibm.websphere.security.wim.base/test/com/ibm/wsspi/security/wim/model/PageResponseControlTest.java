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

public class PageResponseControlTest {

    @Test
    public void testToString() {

        /*
         * Test empty PageResponseControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:PageResponseControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new PageResponseControl().toString());

        /*
         * Set all fields on the PageResponseControl.
         */
        PageResponseControl control = new PageResponseControl();
        control.setTotalSize(0);

        sb = new StringBuffer();
        sb.append("<wim:PageResponseControl totalSize=\"0\" " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), control.toString());
    }
}
