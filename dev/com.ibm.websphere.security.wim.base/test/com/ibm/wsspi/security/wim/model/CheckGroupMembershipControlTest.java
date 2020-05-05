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

public class CheckGroupMembershipControlTest {
    @Test
    public void testToString() {

        /*
         * Test empty CheckGroupMembershipControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:CheckGroupMembershipControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new CheckGroupMembershipControl().toString());

        /*
         * Set all fields on CheckGroupMembershipControl.
         */
        CheckGroupMembershipControl control = new CheckGroupMembershipControl();
        control.setLevel(0);
        control.setInGroup(true);

        sb = new StringBuffer();
        sb.append("<wim:CheckGroupMembershipControl level=\"0\" inGroup=\"true\" " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), control.toString());
    }
}
