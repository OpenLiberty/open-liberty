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
package com.ibm.ws.config.xml.internal;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.annotation.Trivial;

public class StringUtils {

    /**
     * Replace non-alphanumeric characters in a string with underscores.
     *
     * @param name
     * @return modified name
     */
    public String replaceNonAlpha(String name) {
        String modifiedName = null;
        if (p != null)
            modifiedName = p.matcher(name).replaceAll("_");
        return modifiedName;
    }

    private static final String ALLOWABLE_CHARS = "[^A-Za-z0-9_]";

    private static Pattern p = null;

    static {
        p = Pattern.compile(ALLOWABLE_CHARS);
    }

    int getNextLocation(int start, String list) {
        int size = list.length();
        for (int i = start; i < size; i++) {
            char ch = list.charAt(i);
            if (ch == '\\' || ch == ',') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Escape slashes and/or commas in the given value.
     *
     * @param value
     * @return
     */
    @Trivial
    String escapeValue(String value) {
        int start = 0;
        int pos = getNextLocation(start, value);
        if (pos == -1) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        while (pos != -1) {
            builder.append(value, start, pos);
            builder.append('\\');
            builder.append(value.charAt(pos));
            start = pos + 1;
            pos = getNextLocation(start, value);
        }
        builder.append(value, start, value.length());
        return builder.toString();
    }

    /*
     * Convert evaluated attribute value into a String.
     */
    @Trivial
    String convertToString(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof List) {
            List<?> list = ((List<?>) value);
            if (list.size() == 0) {
                return EvaluationContext.EMPTY_STRING;
            } else if (list.size() == 1) {
                String strValue = String.valueOf(list.get(0));
                return escapeValue(strValue);
            } else {
                StringBuilder builder = new StringBuilder();
                Iterator<?> iterator = list.iterator();
                while (iterator.hasNext()) {
                    String strValue = String.valueOf(iterator.next());
                    strValue = escapeValue(strValue);
                    builder.append(strValue);
                    if (iterator.hasNext()) {
                        builder.append(", ");
                    }
                }
                return builder.toString();
            }
        } else if (value instanceof String[]) {
            String[] array = (String[]) value;
            if (array.length == 0) {
                return EvaluationContext.EMPTY_STRING;
            } else if (array.length == 1) {
                return escapeValue(array[0]);
            } else {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < array.length; i++) {
                    String strValue = escapeValue(array[i]);
                    builder.append(strValue);
                    if (i + 1 < array.length) {
                        builder.append(", ");
                    }
                }
                return builder.toString();
            }
        } else if (value.getClass().isArray()) {
            int size = Array.getLength(value);
            if (size == 0) {
                return EvaluationContext.EMPTY_STRING;
            } else if (size == 1) {
                String strValue = String.valueOf(Array.get(value, 0));
                return escapeValue(strValue);
            } else {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    String strValue = String.valueOf(Array.get(value, i));
                    strValue = escapeValue(strValue);
                    builder.append(strValue);
                    if (i + 1 < size) {
                        builder.append(", ");
                    }
                }
                return builder.toString();
            }
        } else {
            return value.toString();
        }
    }

}