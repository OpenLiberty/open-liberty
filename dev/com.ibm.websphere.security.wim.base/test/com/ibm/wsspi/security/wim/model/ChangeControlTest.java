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

public class ChangeControlTest {
    @Test
    public void testToString() {

        /*
         * Test empty ChangeControl.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:ChangeControl " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new ChangeControl().toString());

        /*
         * Set all fields on ChangeControl.
         */
        ChangeControl control = new ChangeControl();
        control.set("checkPoint", CheckPointTypeTest.TEST_CHECK_POINT_TYPE_1);
        control.set("checkPoint", CheckPointTypeTest.TEST_CHECK_POINT_TYPE_2);
        control.set("changeTypes", "changeTypes1");
        control.set("changeTypes", "changeTypes2");

        /*
         * Set all fields on the SearchControl.
         */
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
        control.set("contextProperties", PropertyControlTest.TEST_CONTEXT_PROPERTY_1);
        control.set("contextProperties", PropertyControlTest.TEST_CONTEXT_PROPERTY_2);

        sb = new StringBuffer();
        sb.append("<wim:ChangeControl countLimit=\"1\" searchLimit=\"3\" expression=\"expression\" returnSubType=\"true\" " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:properties>properties1</wim:properties>\n");
        sb.append("    <wim:properties>properties2</wim:properties>\n");
        sb.append("    <wim:contextProperties xml:lang=\"lang1\">value1</wim:contextProperties>\n");
        sb.append("    <wim:contextProperties xml:lang=\"lang2\">value2</wim:contextProperties>\n");
        sb.append("    <wim:searchBases>searchBases1</wim:searchBases>\n");
        sb.append("    <wim:searchBases>searchBases2</wim:searchBases>\n");
        sb.append("    <wim:checkPoint>\n");
        sb.append("        <wim:repositoryId>repositoryId1</wim:repositoryId>\n");
        sb.append("        <wim:repositoryCheckPoint>repositoryCheckPoint1</wim:repositoryCheckPoint>\n");
        sb.append("    </wim:checkPoint>\n");
        sb.append("    <wim:checkPoint>\n");
        sb.append("        <wim:repositoryId>repositoryId2</wim:repositoryId>\n");
        sb.append("        <wim:repositoryCheckPoint>repositoryCheckPoint2</wim:repositoryCheckPoint>\n");
        sb.append("    </wim:checkPoint>\n");
        sb.append("    <wim:changeTypes>changeTypes1</wim:changeTypes>\n");
        sb.append("    <wim:changeTypes>changeTypes2</wim:changeTypes>\n");
        sb.append("</wim:ChangeControl>");
        assertEquals(sb.toString(), control.toString());
    }
}
