/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

public class ConversionTest extends AbstractConfigTest {

    @Test
    public void testCharacter() {
        Character characterValue = new Character('x');
        String raw = "" + characterValue;
        assertConversion("testCharacter", raw, characterValue, Character.class);
    }

    @Test
    public void testChar() {
        char characterValue = 'x';
        String raw = "" + characterValue;
        assertConversion("testChar", raw, characterValue, char.class);
    }

    @Test
    public void testCharUnicode() {
        char characterValue = '\u00F6';
        String raw = "\u00F6";
        assertConversion("testCharUnicode", raw, characterValue, char.class);
    }

    @Test
    public void testBadCharUnicode() {
        String raw = "\\u00F6";
        assertBadConversion("testBadCharUnicode", raw, char.class);
    }

    @Test
    public void testConverterCache() throws InterruptedException {
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().withConverter(TestType.class, 100, new TestTypeConverter()).build();
        String key = "testConverterCache";

        String rawString = "This is the RAW String 1";
        String expected = rawString + " - 1";
        System.setProperty(key, rawString);

        Thread.sleep(700); // Let the raw value cache expire

        TestType value = config.getValue(key, TestType.class);
        assertEquals(expected, value.toString());
        //should not be reconverted
        value = config.getValue(key, TestType.class);
        assertEquals(expected, value.toString());

        rawString = "This is the RAW String 2";
        //raw string changed so should now get reconverted
        expected = rawString + " - 2";
        System.setProperty(key, rawString);

        Thread.sleep(700); // Let the raw value cache expire

        value = config.getValue(key, TestType.class);
        assertEquals(expected, value.toString());

        Thread.sleep(700); // Let the raw value cache expire
        //should not be reconverted
        value = config.getValue(key, TestType.class);
        assertEquals(expected, value.toString());

    }

    public <T> void assertConversion(String key, String rawString, T expected, Class<T> type) {
        System.setProperty(key, rawString);
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();

        T value = config.getValue(key, type);
        assertEquals(expected, value);
    }

    public <T> void assertBadConversion(String key, String rawString, Class<T> type) {
        System.setProperty(key, rawString);
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();

        try {
            config.getValue(key, type);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

}
