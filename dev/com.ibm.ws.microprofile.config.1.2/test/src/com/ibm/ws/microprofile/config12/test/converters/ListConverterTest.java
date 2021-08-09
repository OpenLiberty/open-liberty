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
package com.ibm.ws.microprofile.config12.test.converters;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.ibm.ws.microprofile.config12.converters.ListConverter;
import com.ibm.ws.microprofile.config12.converters.SetConverter;

/**
 *
 */
public class ListConverterTest extends AbstractConfigTest {

    @Test
    public void testListConverter() {
        String one = "One";
        String two = "Two";
        String three = "Three";
        String fourA = "FourA";
        String fourB = "FourB";

        String input = one + "," + two + "," + three + "," + fourA + "\\," + fourB;
        List<String> expected = Arrays.asList(new String[] { one, two, three, fourA + "," + fourB });
        System.out.println("String: " + input);

        ListConverter converter = new ListConverter();
        List<String> converted = converter.convert(input);

        assertEquals(expected, converted);
    }

    @Test
    public void testSetConverter() {
        String one = "One";
        String two = "Two";
        String three = "Three";
        String fourA = "FourA";
        String fourB = "FourB";

        String input = one + "," + two + "," + three + "," + fourA + "\\," + fourB;
        Set<String> expected = new HashSet<String>(Arrays.asList(new String[] { one, two, three, fourA + "," + fourB }));
        System.out.println("String: " + input);

        SetConverter converter = new SetConverter();
        Set<String> converted = converter.convert(input);

        assertEquals(expected, converted);
    }

}
