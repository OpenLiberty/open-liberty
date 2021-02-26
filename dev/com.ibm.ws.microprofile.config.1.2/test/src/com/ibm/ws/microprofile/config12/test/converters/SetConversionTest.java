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
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class SetConversionTest extends AbstractConfigTest {

    @SuppressWarnings("unused")
    private Set<Integer> setIntField;

    @Test
    public void testSet() {
        String one = "One";
        String two = "Two";
        String three = "Three";
        String four = "Four";

        String input = one + "," + two + "," + three + "," + four;
        Set<String> expected = new HashSet<>(Arrays.asList(one, two, three, four));

        ConversionUtil.assertConversion("testSet", input, expected, Set.class);
    }

    @Test
    public void testSingleElementSet() {
        String input = "One";
        Set<String> expected = new HashSet<>(Arrays.asList("One"));

        ConversionUtil.assertConversion("testSingleElementSet", input, expected, Set.class);
    }

    @Test
    public void testEmptySet() {
        String input = "";
        Set<String> expected = new HashSet<>(Arrays.asList(""));

        ConversionUtil.assertConversion("testEmptySet", input, expected, Set.class);
    }

    @Test
    public void testNonStringSet() throws NoSuchFieldException, SecurityException {
        int one = 1;
        int two = 2;
        int three = 3;
        int four = 4;

        String input = one + "," + two + "," + three + "," + four;
        Set<Integer> expected = new HashSet<>(Arrays.asList(1, 2, 3, 4));

        Type setIntType = SetConversionTest.class.getDeclaredField("setIntField").getGenericType();

        ConversionUtil.assertTypedConversion("testNonStringSet", input, expected, setIntType);
    }

}
