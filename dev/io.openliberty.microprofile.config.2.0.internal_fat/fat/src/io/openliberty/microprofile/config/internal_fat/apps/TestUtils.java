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
package io.openliberty.microprofile.config.internal_fat.apps;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

public class TestUtils {

    public static void assertContains(Config config, Map<String, String> props) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            assertContains(config, entry.getKey(), entry.getValue());
        }
    }

    public static <T> void assertContains(Config config, String key, T expected) {
        //a bit of a naughty cast but I know it's a set and that's easier to work on
        Set<String> keys = (Set<String>) config.getPropertyNames();
        if (!keys.contains(key)) {
            throw new AssertionError("Key '" + key + "' was not found");
        }
        @SuppressWarnings("unchecked")
        Optional<T> opt = config.getOptionalValue(key, (Class<T>) expected.getClass());
        T actual = opt.orElse(null);
        if (actual == null) {
            throw new AssertionError("Value for key '" + key + "' was null");
        }
        if (!expected.equals(actual)) {
            throw new AssertionError("Value for key '" + key + "' was '" + actual + "'. Expected: '" + expected + "'");
        }
    }

    public static void assertNotContains(Config config, String key) {
        assertNotContains(config, key, String.class);
    }

    public static <T> void assertNotContains(Config config, String key, Class<T> clazz) {
        //a bit of a naughty cast but I know it's a set and that's easier to work on
        Set<String> keys = (Set<String>) config.getPropertyNames();
        if (keys.contains(key)) {
            T opt = config.getValue(key, clazz);
            throw new AssertionError("Key '" + key + "' was found. Value was '" + opt + "'");
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (expected != null && !expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + ". Actual: " + actual);
        }
        if (expected == null && actual != null) {
            throw new AssertionError("Expected: " + expected + ". Actual: " + actual);
        }
    }

    public static void fail(Throwable t) {
        t.printStackTrace();
        StringBuilder builder = new StringBuilder("FAILED: ");
        builder.append(t);
        builder.append(" at ");
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < 5 && i < stack.length; i++) {
            builder.append(stack[i]);
            builder.append("\n");
        }
        throw new AssertionError(builder.toString());
    }

    public static void fail(String message) {
        throw new AssertionError("FAILED: " + message);
    }
}
