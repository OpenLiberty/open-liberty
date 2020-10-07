/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.joblog.internal.impl;

import java.util.Arrays;
import java.util.Collections;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Convenience utilities.
 */
@Trivial
public class StringUtils {

    /**
     * @return true if s is null or "" or nothing but whitespace.
     */
    public static boolean isEmpty(String s) {
        return (s == null || s.trim().isEmpty());
    }

    /**
     * @return str, with all suffixes trimmed off the end, if any are there.
     */
    public static String trimSuffixes(String str, String... suffixes) {
        String retMe = str;
        for (String suffix : nullSafeIterable(suffixes)) {
            retMe = trimSuffix(retMe, suffix);
        }
        return retMe;
    }

    /**
     * @return str, with 'suffix' trimmed off the end, if it's there.
     */
    public static String trimSuffix(String str, String suffix) {
        return (str.endsWith(suffix))
                        ? str.substring(0, str.length() - suffix.length())
                        : str;
    }

    /**
     * Useful for avoiding NPEs when using for-each on a iterable that may be null.
     * 
     * @return the given iterable, if not null, otherwise an empty list.
     */
    public static <T> Iterable<T> nullSafeIterable(Iterable<T> iterable) {
        return (iterable != null) ? iterable : Collections.EMPTY_LIST;
    }

    /**
     * Useful for avoiding NPEs when using for-each on a iterable that may be null.
     * 
     * @return the given iterable, if not null, otherwise an empty list.
     */
    public static <T> Iterable<T> nullSafeIterable(T[] iterable) {
        return (iterable != null) ? Arrays.asList(iterable) : Collections.EMPTY_LIST;
    }

    /**
     * @param s the string to split
     * @param delim a regex for the delimiter
     * 
     * @return s.split(delim), if s is not null; otherwise an empty String[].
     */
    public static String[] split(String s, String delim) {
        return (s != null) ? s.split(delim) : new String[] {};
    }

    /**
     * @return the first parameter that isn't null, or null if they're all null.
     */
    public static <T> T firstNonNull(T... vals) {
        for (T val : vals) {
            if (val != null) {
                return val;
            }
        }
        return null;
    }

    /**
     * @return the first parameter that isn't null, or null if they're all null.
     */
    public static String firstNonEmpty(String... vals) {
        for (String val : vals) {
            if (!StringUtils.isEmpty(val)) {
                return val;
            }
        }
        return null;
    }
}
