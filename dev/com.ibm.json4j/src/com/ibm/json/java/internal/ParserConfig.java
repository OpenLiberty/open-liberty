/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.json.java.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Configuration class for JSON parser security limits.
 * <p>
 * This class loads parser-related configuration from system properties using the
 * {@code com.ibm.json4j} prefix and caches the resolved values during class
 * initialization. Each value is validated once and exposed through static getter
 * methods for fast, thread-safe access by parser components.
 * </p>
 * <p>
 * Invalid, empty, negative, zero, or otherwise unsupported property values are
 * rejected and the documented default value is used instead. When fallback
 * occurs, a warning is written to {@code System.err}.
 * </p>
 */
public final class ParserConfig {

    private static final String PROP_MAX_ARRAY_SIZE = "com.ibm.json4j.max.array.size";
    private static final String PROP_MAX_OBJECT_MEMBERS = "com.ibm.json4j.max.object.members";
    private static final String PROP_MAX_NESTING_DEPTH = "com.ibm.json4j.max.nesting.depth";
    private static final String PROP_MAX_STRING_LENGTH = "com.ibm.json4j.max.string.length";
    private static final String PROP_MAX_NUMBER_LENGTH = "com.ibm.json4j.max.number.length";
    private static final String PROP_MAX_TOTAL_SIZE = "com.ibm.json4j.max.total.size";
    private static final String PROP_DUPLICATE_KEY_BEHAVIOR = "com.ibm.json4j.duplicate.key.behavior";

    private static final int DEFAULT_MAX_ARRAY_SIZE = 100000;
    private static final int DEFAULT_MAX_OBJECT_MEMBERS = 10000;
    private static final int DEFAULT_MAX_NESTING_DEPTH = 500;
    private static final int DEFAULT_MAX_STRING_LENGTH = 1048576;
    private static final int DEFAULT_MAX_NUMBER_LENGTH = 100;
    private static final long DEFAULT_MAX_TOTAL_SIZE = 104857600L; // 100 MB
    private static final DuplicateKeyBehavior DEFAULT_DUPLICATE_KEY_BEHAVIOR = DuplicateKeyBehavior.SILENT;

    private static final int MAX_ARRAY_SIZE;
    private static final int MAX_OBJECT_MEMBERS;
    private static final int MAX_NESTING_DEPTH;
    private static final int MAX_STRING_LENGTH;
    private static final int MAX_NUMBER_LENGTH;
    private static final long MAX_TOTAL_SIZE;
    private static final DuplicateKeyBehavior DUPLICATE_KEY_BEHAVIOR;

    static {
        MAX_ARRAY_SIZE = loadIntProperty(PROP_MAX_ARRAY_SIZE, DEFAULT_MAX_ARRAY_SIZE);
        MAX_OBJECT_MEMBERS = loadIntProperty(PROP_MAX_OBJECT_MEMBERS, DEFAULT_MAX_OBJECT_MEMBERS);
        MAX_NESTING_DEPTH = loadIntProperty(PROP_MAX_NESTING_DEPTH, DEFAULT_MAX_NESTING_DEPTH);
        MAX_STRING_LENGTH = loadIntProperty(PROP_MAX_STRING_LENGTH, DEFAULT_MAX_STRING_LENGTH);
        MAX_NUMBER_LENGTH = loadIntProperty(PROP_MAX_NUMBER_LENGTH, DEFAULT_MAX_NUMBER_LENGTH);
        MAX_TOTAL_SIZE = loadLongProperty(PROP_MAX_TOTAL_SIZE, DEFAULT_MAX_TOTAL_SIZE);
        DUPLICATE_KEY_BEHAVIOR = loadDuplicateKeyBehavior();
    }

    /**
     * Enum describing how duplicate object keys should be handled during
     * parsing.
     */
    public enum DuplicateKeyBehavior {
        /**
         * Silently overwrite the previous value for a duplicate key. This
         * preserves the historic behavior of the parser.
         */
        SILENT,

        /**
         * Log a warning and then overwrite the previous value for a duplicate
         * key.
         */
        WARN,

        /**
         * Treat duplicate keys as an error.
         */
        ERROR
    }

    /**
     * Prevent instantiation.
     */
    private ParserConfig() {
    }

    /**
     * Returns the maximum permitted number of elements in a JSON array.
     *
     * @return the configured maximum array size
     */
    public static int getMaxArraySize() {
        return MAX_ARRAY_SIZE;
    }

    /**
     * Returns the maximum permitted number of members in a JSON object.
     *
     * @return the configured maximum object member count
     */
    public static int getMaxObjectMembers() {
        return MAX_OBJECT_MEMBERS;
    }

    /**
     * Returns the maximum permitted nesting depth for parsed JSON content.
     *
     * @return the configured maximum nesting depth
     */
    public static int getMaxNestingDepth() {
        return MAX_NESTING_DEPTH;
    }

    /**
     * Returns the maximum permitted length of a JSON string token.
     *
     * @return the configured maximum string length
     */
    public static int getMaxStringLength() {
        return MAX_STRING_LENGTH;
    }

    /**
     * Returns the maximum permitted length of a JSON number token.
     *
     * @return the configured maximum number length
     */
    public static int getMaxNumberLength() {
        return MAX_NUMBER_LENGTH;
    }

    /**
     * Returns the maximum permitted total size (in bytes) for all parsed content.
     * This limit prevents memory exhaustion from many moderately-sized values.
     *
     * @return the configured maximum total size in bytes
     */
    public static long getMaxTotalSize() {
        return MAX_TOTAL_SIZE;
    }

    /**
     * Returns the configured duplicate-key handling behavior.
     *
     * @return the duplicate-key behavior
     */
    public static DuplicateKeyBehavior getDuplicateKeyBehavior() {
        return DUPLICATE_KEY_BEHAVIOR;
    }

    /**
     * Returns a diagnostic string describing the resolved parser
     * configuration.
     *
     * @return a string containing all resolved configuration values
     */
    public static String toDebugString() {
        return "ParserConfig[maxArraySize=" + MAX_ARRAY_SIZE
               + ", maxObjectMembers=" + MAX_OBJECT_MEMBERS
               + ", maxNestingDepth=" + MAX_NESTING_DEPTH
               + ", maxStringLength=" + MAX_STRING_LENGTH
               + ", maxNumberLength=" + MAX_NUMBER_LENGTH
               + ", maxTotalSize=" + MAX_TOTAL_SIZE
               + ", duplicateKeyBehavior=" + DUPLICATE_KEY_BEHAVIOR
               + "]";
    }

    /**
     * Loads a positive integer property.
     *
     * @param propertyName the system property name
     * @param defaultValue the default value to use when the property is absent
     *                         or invalid
     * @return the validated configured value, or the default when invalid
     */
    private static int loadIntProperty(String propertyName, int defaultValue) {
        String value = getSystemProperty(propertyName);

        if (value == null) {
            return defaultValue;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() == 0) {
            logInvalidProperty(propertyName, value, "value is empty", Integer.toString(defaultValue));
            return defaultValue;
        }

        try {
            int parsedValue = Integer.parseInt(trimmedValue);
            if (parsedValue <= 0) {
                logInvalidProperty(propertyName, value, "value must be a positive integer", Integer.toString(defaultValue));
                return defaultValue;
            }
            return parsedValue;
        } catch (NumberFormatException ex) {
            logInvalidProperty(propertyName, value, "value is not a valid integer", Integer.toString(defaultValue));
            return defaultValue;
        }
    }

    /**
     * Loads a positive long property.
     *
     * @param propertyName the system property name
     * @param defaultValue the default value to use when the property is absent
     *                         or invalid
     * @return the validated configured value, or the default when invalid
     */
    private static long loadLongProperty(String propertyName, long defaultValue) {
        String value = getSystemProperty(propertyName);

        if (value == null) {
            return defaultValue;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() == 0) {
            logInvalidProperty(propertyName, value, "value is empty", Long.toString(defaultValue));
            return defaultValue;
        }

        try {
            long parsedValue = Long.parseLong(trimmedValue);
            if (parsedValue <= 0) {
                logInvalidProperty(propertyName, value, "value must be a positive long", Long.toString(defaultValue));
                return defaultValue;
            }
            return parsedValue;
        } catch (NumberFormatException ex) {
            logInvalidProperty(propertyName, value, "value is not a valid long", Long.toString(defaultValue));
            return defaultValue;
        }
    }

    /**
     * Loads the duplicate-key behavior property.
     *
     * @return the configured duplicate-key behavior, or the default when
     *         invalid
     */
    private static DuplicateKeyBehavior loadDuplicateKeyBehavior() {
        String value = getSystemProperty(PROP_DUPLICATE_KEY_BEHAVIOR);

        if (value == null) {
            return DEFAULT_DUPLICATE_KEY_BEHAVIOR;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() == 0) {
            logInvalidProperty(PROP_DUPLICATE_KEY_BEHAVIOR, value, "value is empty", DEFAULT_DUPLICATE_KEY_BEHAVIOR.name());
            return DEFAULT_DUPLICATE_KEY_BEHAVIOR;
        }

        try {
            return DuplicateKeyBehavior.valueOf(trimmedValue.toUpperCase());
        } catch (IllegalArgumentException ex) {
            logInvalidProperty(PROP_DUPLICATE_KEY_BEHAVIOR, value,
                               "value must be one of SILENT, WARN, or ERROR",
                               DEFAULT_DUPLICATE_KEY_BEHAVIOR.name());
            return DEFAULT_DUPLICATE_KEY_BEHAVIOR;
        }
    }

    /**
     * Reads a system property using a privileged action for compatibility with
     * secured runtime environments.
     *
     * @param propertyName the system property name
     * @return the system property value, or {@code null} if not set or not
     *         accessible
     */
    private static String getSystemProperty(final String propertyName) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                try {
                    return System.getProperty(propertyName);
                } catch (SecurityException ex) {
                    logInvalidProperty(propertyName, null, "access denied by security manager", null);
                    return null;
                }
            }
        });
    }

    /**
     * Writes a warning for an invalid property value.
     *
     * @param propertyName  the property name
     * @param propertyValue the supplied property value
     * @param reason        the reason the value was rejected
     * @param fallbackValue the fallback value, or {@code null} when not
     *                          applicable
     */
    private static void logInvalidProperty(String propertyName, String propertyValue, String reason, String fallbackValue) {
        StringBuffer sb = new StringBuffer();
        sb.append("Warning: Invalid value for system property ");
        sb.append(propertyName);
        sb.append(": ");

        if (propertyValue == null) {
            sb.append("<null>");
        } else {
            sb.append('\'');
            sb.append(propertyValue);
            sb.append('\'');
        }

        sb.append(" (");
        sb.append(reason);
        sb.append(')');

        if (fallbackValue != null) {
            sb.append(". Using default ");
            sb.append(fallbackValue);
            sb.append('.');
        }

        System.err.println(sb.toString());
    }
}
