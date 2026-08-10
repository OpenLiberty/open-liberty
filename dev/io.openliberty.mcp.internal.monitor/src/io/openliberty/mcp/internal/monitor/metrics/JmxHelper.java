/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor.metrics;

/**
 * Helper class for JMX-related operations, particularly for creating JMX-safe identifiers.
 */
public class JmxHelper {

    /**
     * Escapes special characters in a string for use within a JMX ObjectName quoted key property value.
     * This prevents issues with JMX ObjectName parsing and ensures user-entered data
     * doesn't break the monitoring system.
     *
     * <p>According to the JMX ObjectName specification, only the following characters
     * can be escaped within quoted values:
     * <ul>
     * <li>\ (backslash) - must be escaped first to avoid double-escaping</li>
     * <li>" (quote) - used to delimit the property value</li>
     * <li>* (asterisk) - wildcard character in JMX queries</li>
     * <li>? (question mark) - wildcard character in JMX queries</li>
     * <li>n (newline) - represented as \n in the escaped form</li>
     * </ul>
     *
     * @param value the value to escape, may be null
     * @return the escaped value, or empty string if value is null
     * @see <a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.management/javax/management/ObjectName.html">JMX ObjectName Specification</a>
     */
    public static String escapeJmxValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")  // Must be first to avoid double-escaping
                    .replace("\"", "\\\"")
                    .replace("*", "\\*")
                    .replace("?", "\\?")
                    .replace("\n", "\\n");
    }
    

}

// Made with Bob
