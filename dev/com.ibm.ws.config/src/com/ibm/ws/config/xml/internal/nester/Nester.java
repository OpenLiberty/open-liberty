/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.nester;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the client side helper for flat configurations. It is not used in config itself.
 * To use it in your project containing a component with a flattened configuration, include
 * <classpathentry combineaccessrules="false" kind="src" path="/com.ibm.ws.config"/>
 * in the project's .classpath file and
 * Private-Package: \
 * com.ibm.ws.config.xml.internal.nester, \
 * as part of the Private-Package section of the project's bnd.bnd file.
 * 
 * Both methods nest the top level of the supplied map. If you have multiple levels of flattened configuration
 * you will need to call the appropriate methods at each level on the results from the previous level.
 */

/*
 * See also:
 *     open-liberty\dev\com.ibm.ws.javaee.ddmodel.wsbnd\src\
 *         com\ibm\ws\javaee\ddmodel\wsbnd\internal\
 *             NestingUtils.java
 *
 * Which implements the same operations.  If any updates are made
 * here, parallel updates should be made to NestingUtils.
 */
public class Nester {

    /**
     * Extracts all sub configurations starting with a specified key
     * 
     * @param key key prefix of interest
     * @param map flattened properties
     * @return list of extracted nested configurations for the key
     */
    public static List<Map<String, Object>> nest(String key, Map<String, Object> map) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        String patternString = MessageFormat.format("^{0}\\.([0-9]*)\\.(.*)", Pattern.quote(key));
        Pattern pattern = Pattern.compile(patternString);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String k = entry.getKey();
            Matcher matcher = pattern.matcher(k);
            if (matcher.matches()) {
                int base = 0;
                processMatch(result, entry.getValue(), matcher, base);
            }
        }
        return result;
    }

    /**
     * Extracts all sub configurations starting with a specified key
     * 
     * @param key key prefix of interest
     * @param map flattened properties
     * @return list of extracted nested configurations for the key
     */
    public static List<Map<String, Object>> nest(String key, Dictionary<String, Object> map) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        String patternString = MessageFormat.format("^{0}\\.([0-9]*)\\.(.*)", Pattern.quote(key));
        Pattern pattern = Pattern.compile(patternString);

        for (Enumeration<String> e = map.keys(); e.hasMoreElements();) {
            String k = e.nextElement();
            Matcher matcher = pattern.matcher(k);
            if (matcher.matches()) {
                int base = 0;
                processMatch(result, map.get(k), matcher, base);
            }
        }
        return result;
    }

    /**
     * Extracts all sub configurations starting with any of the specified keys
     * 
     * @param map flattened properties
     * @param keys keys of interest
     * @return map keyed by (supplied) key of lists of extracted nested configurations
     */
    public static Map<String, List<Map<String, Object>>> nest(Map<String, Object> map, String... keys) {
        Map<String, List<Map<String, Object>>> result = new HashMap<String, List<Map<String, Object>>>(keys.length);
        String keyMatch = "";
        for (String key : keys) {
            result.put(key, new ArrayList<Map<String, Object>>());
            keyMatch = MessageFormat.format("{0}{1}|", keyMatch, Pattern.quote(key));
        }
        String patternString = MessageFormat.format("^({0})\\.([0-9]*)\\.(.*)", keyMatch.substring(0, keyMatch.length() - 1));
        Pattern pattern = Pattern.compile(patternString);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String k = entry.getKey();
            Matcher matcher = pattern.matcher(k);
            if (matcher.matches()) {
                int base = 1;
                String key = matcher.group(1);
                processMatch(result.get(key), entry.getValue(), matcher, base);
            }
        }
        return result;
    }

    private static void processMatch(List<Map<String, Object>> result, Object value, Matcher matcher, int base) {
        int i = Integer.parseInt(matcher.group(base + 1));
        String subKey = matcher.group(base + 2);
        if (result.size() < i + 1) {
            for (int j = result.size(); j < i + 1; j++) {
                result.add(null);
            }
        }
        Map<String, Object> subMap = result.get(i);
        if (subMap == null) {
            subMap = new Hashtable<String, Object>();
            result.set(i, subMap);
        }
        subMap.put(subKey, value);
    }

}
