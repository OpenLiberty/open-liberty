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
