/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.ws.microprofile.config14.converters.Config14DefaultConverters;

public class ConverterTest {

    @SuppressWarnings("unchecked")
    private static <T> T defaultConversion(Class<T> type, String value) {
        return (T) Config14DefaultConverters.getDefaultConverters().getConverter(type).convert(value);
    }

    /**
     * @param raw
     * @param characterValue
     * @param class1
     */
    private static <T> void assertConversion(String raw, T expected, Class<T> type) {
        T converted = defaultConversion(type, raw);
        assertEquals(expected, converted);
    }

    @Test
    public void testCharacter() {
        Character characterValue = 'x';
        String raw = "x";
        assertConversion(raw, characterValue, Character.class);
    }

    @Test
    public void testCharUnicode1() {
        Character characterValue = '\u00F6';
        String raw = "\u00F6";
        assertConversion(raw, characterValue, Character.class);
    }

    @Test
    public void testCharUnicode2() {
        Character characterValue = '\u0000';
        String raw = "\u0000";
        assertConversion(raw, characterValue, Character.class);
    }

    @Test
    public void testCharUnicode3() {
        Character characterValue = '\uFFFF';
        String raw = "\uFFFF";
        assertConversion(raw, characterValue, Character.class);
    }
}
