/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.microprofile.config.Config;

import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

public class TestUtils {

    public static void assertContains(Iterable<String> iterable, String value) {
        StringBuilder strb = new StringBuilder();
        boolean first = true;
        for (String str : iterable) {
            if (str.equals(value)) {
                return;
            } else {
                if (!first) {
                    strb.append(", ");
                } else {
                    first = false;
                }
                strb.append(str);
            }
        }
        fail("Iterable (" + strb + ") did not contain: " + value);
    }

    public static void assertNotContains(Iterable<String> iterable, String value) {
        StringBuilder strb = new StringBuilder();
        boolean contains = false;
        boolean first = true;
        for (String str : iterable) {
            if (str.equals(value)) {
                contains = true;
            }

            if (!first) {
                strb.append(", ");
            } else {
                first = false;
            }
            strb.append(str);
        }
        if (contains) {
            fail("Iterable (" + strb + ") DID contain: " + value);
        }
    }

    public static void assertContent(Config config, String key1, String value1, String key2) {
        Iterable<String> keys = config.getPropertyNames();

        assertContains(keys, key1);

        String value = config.getValue(key1, String.class);
        assertEquals(value1, value);

        assertNotContains(keys, key2);
    }

    public static void assertValue(Config config, String key1, String value1) {
        Iterable<String> keys = config.getPropertyNames();

        assertContains(keys, key1);

        String value = config.getValue(key1, String.class);
        assertEquals(value1, value);
    }

    /**
     * @param config
     * @param string
     * @param string2
     * @param string3
     */
    public static void assertSource(WebSphereConfig config, String key, String value, String source) {
        SourcedValue sourcedValue = config.getSourcedValue(key, String.class);
        String actual = (String) sourcedValue.getValue();
        assertEquals(value, actual);
        String actualSource = sourcedValue.getSource();
        assertEquals(source, actualSource);
    }
}
