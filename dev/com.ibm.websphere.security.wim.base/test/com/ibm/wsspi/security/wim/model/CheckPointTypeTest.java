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

public class CheckPointTypeTest {

    /** Test {@link CheckPointType} instance 1. */
    public static final CheckPointType TEST_CHECK_POINT_TYPE_1 = new CheckPointType();

    /** Test {@link CheckPointType} instance 2. */
    public static final CheckPointType TEST_CHECK_POINT_TYPE_2 = new CheckPointType();

    static {
        TEST_CHECK_POINT_TYPE_1.setRepositoryCheckPoint("repositoryCheckPoint1");
        TEST_CHECK_POINT_TYPE_1.setRepositoryId("repositoryId1");

        TEST_CHECK_POINT_TYPE_2.setRepositoryCheckPoint("repositoryCheckPoint2");
        TEST_CHECK_POINT_TYPE_2.setRepositoryId("repositoryId2");
    }

    @Test
    public void testToString() {

        /*
         * Test empty CheckPointType.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:CheckPointType " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new CheckPointType().toString());

        /*
         * Check fully set instance.
         */
        sb = new StringBuffer();
        sb.append("<wim:CheckPointType " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:repositoryId>repositoryId1</wim:repositoryId>\n");
        sb.append("    <wim:repositoryCheckPoint>repositoryCheckPoint1</wim:repositoryCheckPoint>\n");
        sb.append("</wim:CheckPointType>");
        assertEquals(sb.toString(), TEST_CHECK_POINT_TYPE_1.toString());

        sb = new StringBuffer();
        sb.append("<wim:CheckPointType " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:repositoryId>repositoryId2</wim:repositoryId>\n");
        sb.append("    <wim:repositoryCheckPoint>repositoryCheckPoint2</wim:repositoryCheckPoint>\n");
        sb.append("</wim:CheckPointType>");
        assertEquals(sb.toString(), TEST_CHECK_POINT_TYPE_2.toString());
    }
}
