/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Note utils can be used by WAR as well due to special build logic
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

    public static List<String> split(String s, String delim) {
        return (isEmpty(s)) ? new ArrayList<String>() : Arrays.asList(s.split(delim));
    }

    public static String readAsString(InputStream inputStream) throws IOException {
        return join(read(inputStream), "");
    }

    public static List<String> read(InputStream inputStream) throws IOException {
        return read(new BufferedReader(new InputStreamReader(inputStream)));
    }

    /**
     * @return the given buffer as a List<String>, one String per line.
     */
    public static List<String> read(BufferedReader reader) throws IOException {
        List<String> retMe = new ArrayList<String>();
        String lineIn = null;
        while ((lineIn = reader.readLine()) != null) {
            // Filter out SQL comments
            if (!lineIn.startsWith("--")) {
                retMe.add(lineIn);
            }
        }
        return retMe;
    }
}