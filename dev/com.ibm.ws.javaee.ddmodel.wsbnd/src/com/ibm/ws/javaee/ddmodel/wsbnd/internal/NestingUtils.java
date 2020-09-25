/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.internal;

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
 * Copied from "com.ibm.ws.config.xml.internal.nester.Nester".  Referencing
 * the class creates an extra, unnecessary dependency.
 *
 * Utility for nesting configuration elements.  Elements are initially stored
 * in a flat table which expresses nesting relationships through the key values.
 * Key values are expected to have the form:
 * <pre>
 *     <em>prefix</em> + '.' +
 *     zero or more digits (<em>digits</em>) + '.' +
 *     zero or more characters (<em>suffix</em>)
 * </pre>
 *
 * The utility places values matching a specified prefix into a list of mappings:
 * For a particular combination of <em>prefix</em> plus <em>digits</em> plus <em>suffix</em>,
 * the list is null filled to the position specified by the <em>digits</em> value.
 * A table is placed at the <em>digits</em> position, and original matching value from
 * the flat mapping is stored in that table under <em>suffix</em> as the key value.
 *
 * The utility provides three variations of the nesting API:
 * 
 * <ul><li>{@link NestingUtils#nest(String, Map)}</li>
 *     <li>{@link NestingUtils#nest(String, Dictionary)}</li>
 * </ul>
 *
 * And:
 * 
 * <ul><li>{@link NestingUtils#nest(Map, String...)}</li>
 * </ul>
 * 
 * The {@link Map} and {@link Dictionary} variations perform the same operations,
 * with minor adjustments to perform iteration appropriately for the source type.
 * 
 * The third variation performs processing for multiple prefixes in parallel, placing
 * matches for each prefix in its own list, and answering a table mapping prefixes to
 * the matches for that prefix.
 */
public class NestingUtils {

    /** The group corresponding to the initial prefix region obtained by the prefix matcher. */
    @SuppressWarnings("unused")
    private static final int PREFIX_GROUP = 1;

    /** The group corresponding to the middle digits region obtained by the prefix matcher. */
    private static final int DIGITS_GROUP = 2;

    /** The group corresponding to the final characters region obtained by the prefix matcher. */
    private static final int SUFFIX_GROUP = 3;

    /**
     * Answer the pattern text for a prefix (or collection of prefixes).
     *
     * The pattern matches text of the form:
     * 
     * <pre>
     *     prefix + '.' + zero or more digits + '.' + zero or characters
     * </pre>
     * 
     * The pattern generates three groups:
     *
     * <pre>
     *     Group 1 is the prefix
     *     Group 2 is the digits
     *     Group 3 is the characters
     * </pre>
     *
     * @param prefixes The prefixes to compile into the pattern.
     *
     * @return Pattern text matching the prefixes.
     */
    private static Pattern getPrefixPattern(String... prefixes) {
        String escapedPrefixes;
        if ( prefixes.length == 0 ) {
            escapedPrefixes = Pattern.quote(prefixes[0]);
        } else {
            StringBuilder escapedPrefixesBuilder = new StringBuilder();
            for ( String prefix : prefixes ) {
                if ( escapedPrefixesBuilder.length() != 0 ) {
                    escapedPrefixesBuilder.append('|');
                }
                escapedPrefixesBuilder.append( Pattern.quote(prefix) );
            }
            escapedPrefixes = escapedPrefixesBuilder.toString();
        }

        return Pattern.compile( "^(" + escapedPrefixes + ")\\.([0-9]*)\\.(.*)" );
    }

    /**
     * Store a prefix match to a prefix bucket.
     *
     * @param matchingKey The key which was matched.
     * @param prefixMatcher The matcher which processed the matching key,
     *     and which provides the decomposition of the key value.
     * @param prefixBucket The bucket specific to the prefix which was matched.
     * @param matchingValue The value which was stored in association to the 
     */
    private static void storeMatch(
        String matchingKey,
        Matcher prefixMatcher,
        List<Map<String, Object>> prefixBucket,
        Object matchingValue) {

        String matchIndexText = prefixMatcher.group(DIGITS_GROUP);
        int matchIndex = Integer.parseInt(matchIndexText);
        if ( prefixBucket.size() < matchIndex + 1 ) {
            for ( int bucketNo = prefixBucket.size(); bucketNo < matchIndex + 1; bucketNo++ ) {
                prefixBucket.add(null);
            }
        }

        Map<String, Object> subMap = prefixBucket.get(matchIndex);
        if ( subMap == null ) {
            subMap = new Hashtable<String, Object>();
            prefixBucket.set(matchIndex, subMap);
        }

        String subKey = prefixMatcher.group(SUFFIX_GROUP);
        subMap.put(subKey, matchingValue);
    }
    
    /**
     * Collect sub-configurations which start with a specified prefix.
     * 
     * @param prefix The prefix used to select sub-configurations.
     * @param map Table from which to collect the sub-configurations.
     *
     * @return Sub-configurations which start with the specified prefix.
     */
    public static List<Map<String, Object>> nest(String prefix, Map<String, Object> map) {
        Pattern prefixPattern = getPrefixPattern(prefix);

        List<Map<String, Object>> collectedMatches = new ArrayList<Map<String, Object>>();
        for ( Map.Entry<String, Object> entry : map.entrySet() ) {
            String key = entry.getKey();
            Matcher prefixMatcher = prefixPattern.matcher(key);
            if ( prefixMatcher.matches() ) {
                storeMatch(key, prefixMatcher, collectedMatches, entry.getValue());
            }
        }
        return collectedMatches;
    }

    /**
     * Collect sub-configurations which start with a specified prefix.
     *
     * @param prefix The prefix used to select sub-configurations.
     * @param dict Table from which to collect the sub-configurations.
     *
     * @return Sub-configurations which start with the specified prefix.
     */
    public static List<Map<String, Object>> nest(String prefix, Dictionary<String, Object> dict) {
        Pattern prefixPattern = getPrefixPattern(prefix);        

        List<Map<String, Object>> collectedMatches = new ArrayList<Map<String, Object>>();

        Enumeration<String> keys = dict.keys();
        while ( keys.hasMoreElements() ) {
            String key = keys.nextElement();
            Matcher prefixMatcher = prefixPattern.matcher(key);
            if ( prefixMatcher.matches() ) {
                storeMatch(key, prefixMatcher, collectedMatches, dict.get(key));
            }
        }

        return collectedMatches;
    }

    /**
     * Collect sub-configurations which start with one of specified prefixes.
     * 
     * @param map Table from which to collect the sub-configurations.
     * @param prefixes The prefixes used to select sub-configurations.
     *
     * @return Table of sub-configurations which start with one of the specified prefixes.
     *     The table is keyed to each prefix.
     */
    public static Map<String, List<Map<String, Object>>> nest(Map<String, Object> map, String... prefixes) {
        Map<String, List<Map<String, Object>>> collectedMatches =
            new HashMap<String, List<Map<String, Object>>>(prefixes.length);
        for ( String prefix : prefixes ) {
            collectedMatches.put( prefix, new ArrayList<Map<String, Object>>() );
        }

        Pattern prefixPattern = getPrefixPattern(prefixes);

        for ( Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Matcher prefixMatcher = prefixPattern.matcher(key);
            if ( prefixMatcher.matches() ) {
                String matchingPrefix = prefixMatcher.group(1);
                List<Map<String, Object>> prefixBucket = collectedMatches.get(matchingPrefix);

                storeMatch(key, prefixMatcher, prefixBucket, entry.getValue());
            }
        }
        return collectedMatches;
    }
}
