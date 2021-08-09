/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Helps you log a map of properties.
 * 
 * @author Tim Burns
 */
public class PropertyMap {

    private static final Pattern LINE_SEPS = Pattern.compile("[\n\r]");
    private static final Logger LOG = Logger.getLogger(PropertyMap.class.getName());

    private final Map<String, Object> map = new LinkedHashMap<String, Object>();

    /**
     * Maps a property key to a value
     * 
     * @param key property key, null is allowed
     * @param value property value, null is allowed
     */
    public <T> void put(String key, T value) {
        this.map.put(key, value);
    }

    /**
     * Maps a property key to an array of values; a unique number is used as a suffix on each individual key.
     * 
     * @param key property key, null is allowed
     * @param values property values, null array and null values are allowed
     */
    public <T> void put(String key, T[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                this.map.put(key + " " + (i + 1), values[i]);
            }
        }
    }

    /**
     * Maps a property key to an array of values; a unique number is used as a suffix on each individual key.
     * 
     * @param key property key, null is allowed
     * @param values property values, null array and null values are allowed
     */
    public <T> void put(String key, Collection<T> values) {
        if (values != null) {
            int i = 0;
            for (T value : values) {
                this.map.put(key + " " + (++i), value);
            }
        }
    }

    /**
     * Prints the encapsulated map of key/value pairs at the INFO level. The
     * log messages are formatted so that keys and values appear in distinct
     * columns. Assumes that the log formatter prints each log entry to a single line.
     * 
     * @param level
     *            the Logger level to use
     * @param prefix
     *            a String that prefixes all messages logged by this method
     * @param alphabetize
     *            true to alphabetize the list by key, false to use the input order
     */
    public void log(Level level, String prefix, boolean alphabetize) {
        if (!LOG.isLoggable(level)) {
            return;
        }
        if (this.map == null) {
            return;
        }
        TreeSet<String> keys = new TreeSet<String>(this.map.keySet()); // alphabetize keys
        int length = maxLength(keys) + 2; // leave one character for a colon, and one character for some space between keys and values
        String p = (prefix == null) ? "" : prefix;
        String format = "%1$-" + length + "s %2$-1s";
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object value = this.map.get(key);
            String v = (value == null) ? "(null)" : value.toString().trim();
            String[] parts = LINE_SEPS.split(v);
            String k = key.trim() + ":"; // having the colon on the end of the key looks better than having it in the middle
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].length() > 0) { // ignore lines with no data
                    LOG.log(level, p + String.format(format, k, parts[i]));
                    k = ""; // only print the key once
                }
            }
        }
    }

    /**
     * Calculates the length of the longest trimmed String in the input Set
     * 
     * @param set
     *            the set of strings you want to inspect
     * @return the length of the longest trimmed String in the input Set, or -1
     *         if the input Set is null, or -2 if every item in the Set is null
     */
    protected int maxLength(Set<String> set) {
        if (set == null) {
            return -1;
        }
        int maxLength = -2;
        for (String item : set) {
            if (item == null) {
                continue;
            }
            String k = item.trim();
            int length = k.length();
            if (length > maxLength) {
                maxLength = length;
            }
        }
        return maxLength;
    }

}
