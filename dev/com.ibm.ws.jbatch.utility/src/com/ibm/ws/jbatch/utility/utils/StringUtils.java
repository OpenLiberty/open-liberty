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
package com.ibm.ws.jbatch.utility.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Lite-weight impl of apache.commons.lang3.StringUtils
 */
public class StringUtils {

    /**
     * @return true if the string is null or "" or nothing but whitespace.
     */
    public static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static String join(Collection<String> strs, String delim) {
        StringBuilder sb = new StringBuilder();
        String d = "";
        for (String str : nullSafeIterable(strs)) {
            sb.append(d).append(str);
            d = delim;
        }
        return sb.toString();
    }

    /**
     * @return a subset of the given collection, filtered for strings that start with the given prefix.
     */
    public static List<String> filterPrefix(Collection<String> strs, String prefix) {
        
        List<String> retMe = new ArrayList<String>();
 
        for (String str : nullSafeIterable(strs)) {
            if (str.startsWith(prefix)) {
                retMe.add(str);
            }
        }
        
        return retMe;
    }

    /**
     * @return a list containing all Strings from the given collection, with the given
     *         prefix trimmed off the front of each String.
     */
    public static List<String> trimPrefix(Collection<String> strs, String prefix) {
        List<String> retMe = new ArrayList<String>();
        
        for (String str : nullSafeIterable(strs)) {
            retMe.add( trimPrefix( str, prefix ) );
        }
        
        return retMe;
    }
    
    /**
     * @return str, with prefix stripped away from the beginning of it.
     */
    public static String trimPrefix(String str, String prefix) {
        return (str.startsWith(prefix)) ? str.substring(prefix.length()) : str;
    }
    
    /**
     * @return str, with the given suffix trimmed away from the end
     */
    public static String trimSuffix(String str, String suffix) {
        return (str.endsWith(suffix)) ? str.substring(0, str.length() - suffix.length()) : str;
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
     * @return the given string, with any surrounding quotes "" removed.
     */
    public static String dequote(String str) {
        return trimPrefix( trimSuffix(str, "\""), "\"");
    }

    /**
     * @return the given str split on the given delim
     */
    public static List<String> split(String str, String delim) {
        return ( str == null ) ? new ArrayList<String>() : Arrays.asList(str.split(delim)); 
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
}
