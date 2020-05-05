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

public class ChangeResponseControlTest {
    @Test
    public void testToString() {

        /*
         * Test empty ChangeResponseControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:ChangeResponseControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new ChangeResponseControl().toString());

        /*
         * Set all fields on ChangeResponseControl.
         */
        ChangeResponseControl control = new ChangeResponseControl();
        control.set("checkPoint", CheckPointTypeTest.TEST_CHECK_POINT_TYPE_1);
        control.set("checkPoint", CheckPointTypeTest.TEST_CHECK_POINT_TYPE_2);

        /*
         * Set all fields on SearchResponseControl.
         */
        control.setHasMoreResults(true);

        sb = new StringBuffer();
        sb.append("<wim:ChangeResponseControl hasMoreResults=\"true\" " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:checkPoint>\n");
        sb.append("        <wim:repositoryId>repositoryId1</wim:repositoryId>\n");
        sb.append("        <wim:repositoryCheckPoint>repositoryCheckPoint1</wim:repositoryCheckPoint>\n");
        sb.append("    </wim:checkPoint>\n");
        sb.append("    <wim:checkPoint>\n");
        sb.append("        <wim:repositoryId>repositoryId2</wim:repositoryId>\n");
        sb.append("        <wim:repositoryCheckPoint>repositoryCheckPoint2</wim:repositoryCheckPoint>\n");
        sb.append("    </wim:checkPoint>\n");
        sb.append("</wim:ChangeResponseControl>");
        assertEquals(sb.toString(), control.toString());
    }
}
