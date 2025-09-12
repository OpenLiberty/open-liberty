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

import java.math.BigDecimal;

import org.junit.Test;

import io.openliberty.mcp.internal.requests.McpRequestId;

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
        BigDecimal num1 = new BigDecimal(5);
        BigDecimal num2 = new BigDecimal(5.0f);
        McpRequestId req1 = new McpRequestId(num1);
        McpRequestId req2 = new McpRequestId(num2);
        assertTrue(req1.equals(req2));
    }

    @Test
    public void testRequestIdNumbersAreNotEqual() {
        BigDecimal num1 = new BigDecimal(5);
        BigDecimal num2 = new BigDecimal(7);
        McpRequestId req1 = new McpRequestId(num1);
        McpRequestId req2 = new McpRequestId(num2);
        assertFalse(req1.equals(req2));
    }

    @Test
    public void testRequestIdStringDoesNotEqualNumber() {
        McpRequestId reqIdInt = new McpRequestId(new BigDecimal(1));
        McpRequestId reqIdString = new McpRequestId("1");
        assertFalse(reqIdString.equals(reqIdInt));
    }

}
