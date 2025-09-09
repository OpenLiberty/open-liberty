/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openliberty.mcp.internal.McpRequestId;

/**
 *
 */
public class McpRequestIdTest {

    @Test
    public void testRequestIdStringsAreEqual() {
        McpRequestId reqId1 = new McpRequestId("Dog");
        McpRequestId reqId2 = new McpRequestId("Dog");
        assertTrue(reqId1.equals(reqId2));
    }

    @Test
    public void testRequestIdStringsAreNotEqual() {
        McpRequestId reqId1 = new McpRequestId("Dog");
        McpRequestId reqId2 = new McpRequestId("Cat");
        assertFalse(reqId1.equals(reqId2));
    }

    @Test
    public void testRequestIdNumbersAreEqual() {
        Integer int1 = 1;
        Long long1 = 1L;
        Short short1 = 1;
        Byte byte1 = 1;
        McpRequestId intId = new McpRequestId(int1);
        McpRequestId longId = new McpRequestId(long1);
        McpRequestId shortId = new McpRequestId(short1);
        McpRequestId byteId = new McpRequestId(byte1);
        assertTrue(intId.equals(longId));
        assertTrue(shortId.equals(byteId));
    }

    @Test
    public void testRequestIdNumbersAreNotEqual() {
        McpRequestId reqId1 = new McpRequestId(1);
        McpRequestId reqId2 = new McpRequestId(2);
        assertFalse(reqId1.equals(reqId2));
    }

    @Test
    public void testRequestIdStringDoesNotEqualNumber() {
        McpRequestId reqIdInt = new McpRequestId(1);
        McpRequestId reqIdString = new McpRequestId("1");
        assertFalse(reqIdString.equals(reqIdInt));
    }

    @Test
    public void testRequestIdStringEqualsString() {
        McpRequestId reqIdString = new McpRequestId("dog");
        assertTrue(reqIdString.equals("dog"));
    }

    @Test
    public void testRequestIdNumberEqualsNumber() {
        McpRequestId reqIdInt = new McpRequestId(1);
        assertTrue(reqIdInt.equals(Integer.valueOf(1)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRequestIdFloat() {
        new McpRequestId(1.5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRequestIdObj() {
        Object obj = false;
        new McpRequestId(obj);
    }

}
