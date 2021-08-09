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
package com.ibm.ws.jbatch.rest.utils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A lite-weight impl of apache.commons.lang3.StringUtils/ObjectUtils.
 */
public class StringUtils {

    public static final String EbcdicCharsetName = "IBM-1047";
    
    /**
     * @return the ebcdic bytes for the given string
     * 
     * @throws RuntimeException if an UnsupportedEncodingException occurs (very unexpected).
     */
    public static byte[] getEbcdicBytes(String s) {
        try {
            return (s != null) ? s.getBytes(EbcdicCharsetName) : new byte[0];
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }
    
    /**
     * @return true if the string is null or "" or only whitespace
     */
    public static boolean isEmpty(String str) {
        return (str == null) || str.trim().length() == 0;
    }

    /**
     * @return e.printStackTrace() as a String.
     */
    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace( new PrintWriter( sw ) );
        return sw.toString();
    }
    
    /**
     * @return the given string wrapped in quotes ""
     */
    public static String enquote(String str) {
        return "\"" + str + "\"" ;
    }
    
    /**
     * @return the string with any surrounding quotes "" removed
     */
    public static String trimQuotes(String str) {
        return trimSuffix( trimPrefix(str, "\""), "\"");
    }
    
    /**
     * @return str, with prefix stripped away from the beginning of it.
     */
    public static String trimPrefix(String str, String prefix) {
        return (str.startsWith(prefix)) ? str.substring(prefix.length()) : str;
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
     * @return All strings in the collection joined with the given delim.
     */
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
     * 
     * @param path a list of path segments, e.g. "path", "to", "myfile.txt"
     * 
     * @return the path with platform-specific path separator.
     *         Note: does not prepend the path with a path separator.
     */
    public static String platformPath(String... path) {
        return join(Arrays.asList(path), File.separator);
    }
    
    /**
     * @return the path with all file separators normalized to "/".
     */
    public static String normalizePath(String path) {
        return (path != null) ? path.replaceAll("\\\\", "/") : path;
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
     * Null safe equals() method
     * @return true if the two strs are equal, or if they're both null; false otherwise.
     */
    public static boolean areEqual(String s1, String s2) {
        return (s1 == null) ? s2 == null : s1.equals(s2);
    }

    /**
     * @return true if all strs in the given collections are equal, or the collection is empty/null.
     *         false otherwise.
     */
    public static boolean areEqual(List<String> strs) {
        
        if (strs == null || strs.isEmpty()) {
            return true;
        }
        
        String compareStr = strs.get(0);
        for (String str : strs) {
            if ( ! areEqual(compareStr, str) ) {
                return false;
            }
        }
            
        // If we got here they must all be equal.
        return true;
    }
}
