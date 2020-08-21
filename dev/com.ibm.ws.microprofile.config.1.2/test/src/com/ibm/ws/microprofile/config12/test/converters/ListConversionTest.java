/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.test.converters;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ListConversionTest extends AbstractConfigTest {

    @SuppressWarnings("unused")
    private List<Integer> listIntField;

    @Test
    public void testList() {
        String one = "One";
        String two = "Two";
        String three = "Three";
        String four = "Four";

        String input = one + "," + two + "," + three + "," + four;
        List<String> expected = Arrays.asList(one, two, three, four);

        ConversionUtil.assertConversion("testList", input, expected, List.class);
    }

    @Test
    public void testSingleElementList() {
        String input = "One";
        List<String> expected = Arrays.asList("One");

        ConversionUtil.assertConversion("testSingleElementList", input, expected, List.class);
    }

    @Test
    public void testEmptyList() {
        String input = "";
        List<String> expected = Arrays.asList("");

        ConversionUtil.assertConversion("testEmptyList", input, expected, List.class);
    }

    @Test
    public void testNonStringList() throws NoSuchFieldException, SecurityException {
        int one = 1;
        int two = 2;
        int three = 3;
        int four = 4;

        String input = one + "," + two + "," + three + "," + four;
        List<Integer> expected = Arrays.asList(1, 2, 3, 4);

        Type listIntType = ListConversionTest.class.getDeclaredField("listIntField").getGenericType();

        ConversionUtil.assertTypedConversion("testNonStringList", input, expected, listIntType);
    }

}
