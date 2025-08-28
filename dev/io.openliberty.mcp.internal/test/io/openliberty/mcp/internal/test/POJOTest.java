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

import java.util.ArrayList;

import org.junit.Test;

import io.openliberty.mcp.internal.ToolResponseResult;

/**
 *
 */
public class POJOTest {
    private record City(String name, String country, int population, boolean isCapital) {};

    @Test
    public void testValidPojo() {
        City city = new City("London", "England", 8000, false);
        assertTrue(ToolResponseResult.isPojo(city));
    }

    @Test
    public void testJavaObjectIsNotPojo() {
        String str = "test";
        Integer num = 3;
        ArrayList<Integer> list = new ArrayList<>();
        assertFalse(ToolResponseResult.isPojo(num));
        assertFalse(ToolResponseResult.isPojo(str));
        assertFalse(ToolResponseResult.isPojo(list));
    }

    @Test
    public void testArrayIsNotPojo() {
        int[] intArry = { 1, 2, 3 };
        char[] charAray = { '1', '2', '3' };
        assertFalse(ToolResponseResult.isPojo(intArry));
        assertFalse(ToolResponseResult.isPojo(charAray));
    }

    @Test
    public void testPrimitiveIsNotPojo() {
        int num = 3;
        char c = 'h';
        double doub = 2.5;
        boolean bool = false;
        assertFalse(ToolResponseResult.isPojo(num));
        assertFalse(ToolResponseResult.isPojo(c));
        assertFalse(ToolResponseResult.isPojo(doub));
        assertFalse(ToolResponseResult.isPojo(bool));
    }

}
