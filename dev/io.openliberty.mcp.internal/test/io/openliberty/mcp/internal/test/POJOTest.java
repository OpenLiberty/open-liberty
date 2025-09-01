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

import java.util.List;

import org.junit.Test;

import io.openliberty.mcp.internal.ToolResponseResult;

/**
 *
 */
public class POJOTest {
    private record City(String name, String country, int population, boolean isCapital) {};

    @Test
    public void testObjectIsValidPojo() {
        City city = new City("London", "England", 8000, false);
        assertTrue(ToolResponseResult.isPojo(city));
    }

    @Test
    public void testArrayIsValidPojo() {
        int[] intArr = { 1, 2, 3, 4, 5 };
        char[] charArr = { '1', '2', '3' };
        assertTrue(ToolResponseResult.isPojo(intArr));
        assertTrue(ToolResponseResult.isPojo(charArr));
    }

    @Test
    public void testListIsValidPojo() {
        City city1 = new City("London", "England", 8000, true);
        City city2 = new City("Manchester", "England", 15000, false);
        List<City> cityList = List.of(city1, city2);
        List<Integer> intList = List.of(1, 2, 3, 4, 5);
        List<String> StringList = List.of("1", "2", "3");

        assertTrue(ToolResponseResult.isPojo(cityList));
        assertTrue(ToolResponseResult.isPojo(intList));
        assertTrue(ToolResponseResult.isPojo(StringList));
    }

    @Test
    public void testStringIsNotPojo() {
        String str = "test";
        Integer num = 3;
        assertFalse(ToolResponseResult.isPojo(num));
        assertFalse(ToolResponseResult.isPojo(str));
    }

    @Test
    public void testPrimitiveIsNotPojo() {
        boolean bool = false;
        byte byt = 1;
        char chr = 'h';
        short sh = 1;
        int num = 3;
        long lng = 10;
        float flt = 2.8f;
        double dbl = 2.5;

        assertFalse(ToolResponseResult.isPojo(num));
        assertFalse(ToolResponseResult.isPojo(chr));
        assertFalse(ToolResponseResult.isPojo(dbl));
        assertFalse(ToolResponseResult.isPojo(bool));
        assertFalse(ToolResponseResult.isPojo(byt));
        assertFalse(ToolResponseResult.isPojo(sh));
        assertFalse(ToolResponseResult.isPojo(lng));
        assertFalse(ToolResponseResult.isPojo(flt));
    }

}
