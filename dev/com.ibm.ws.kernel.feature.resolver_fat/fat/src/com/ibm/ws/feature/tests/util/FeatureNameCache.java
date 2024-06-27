/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache of parsed name and version values.
 *
 * Keys are versioned feature names, for example, "servlet-4.0".
 * Values are the pair of the parsed base feature name plus the
 * text of the version. Continuing the example, "servlet" and "4.0".
 * A value which does not have a version will be returned with
 * null version text. That is, "servlet" is stored as { "servlet", null }.
 *
 * The version text is validated: A version value which is not value
 * is stored as null.
 */
public class FeatureNameCache {

    public static class VersionComparator implements Comparator<String> {
        @Override
        public int compare(String arg0, String arg1) {
            int arg0Dot = arg0.indexOf('.');
            int arg1Dot = arg1.indexOf('.');

            if (arg0Dot < arg1Dot) {
                return -1;
            } else if (arg0Dot > arg1Dot) {
                return +1;
            } else {
                return arg0.compareTo(arg1);
            }
        }
    }

    public static final VersionComparator VERSION_COMPARATOR = new VersionComparator();

    //

    public static boolean isAnomalous(String version) {
        int vLen = version.length();
        int cNo = 0;
        while ((cNo < vLen) && isVersionChar(version.charAt(cNo))) {
            cNo++;
        }
        return (cNo < vLen);
    }

    public static boolean isVersionChar(char c) {
        return ((c == '.') || Character.isDigit(c));
    }

    //

    public FeatureNameCache() {
        this.cache = new HashMap<>();
    }

    public static final int NAME_OFFSET = 0;
    public static final int VERSION_OFFSET = 1;

    private final Map<String, String[]> cache;

    private String[] cachePut(String symName, String[] parts) {
        return cache.put(symName, parts);
    }

    private String[] cacheGet(String symName) {
        return cache.get(symName);
    }

    /**
     * Answer the base name of a versioned feature name.
     *
     * Use the parse cache to do this quickly.
     *
     * @see {@link #parseNameAndVersion(String)}.
     *
     * @param symName The versioned feature name which is to be parsed.
     *
     * @return The base name of the feature name.
     */
    public String parseName(String symName) {
        return parseNameAndVersion(symName)[NAME_OFFSET];
    }

    /**
     * Answer the version text of a versioned feature name.
     * Answer null if the feature name does not have a version,
     * or if the version text is not a valid version.
     *
     * Valid versions are expected to be dotted pairs of integers,
     * for example, "10.0".
     *
     * Use the parse cache to do this quickly.
     *
     * @see {@link #parseNameAndVersion(String)}.
     *
     * @param symName The versioned feature name which is to be parsed.
     *
     * @return The version text of the feature name.
     */
    public String parseVersion(String symName) {
        return parseNameAndVersion(symName)[VERSION_OFFSET];
    }

    /**
     * Answer the base name and the version text of a versioned
     * feature name.
     *
     * Store null for the version text if the feature name does
     * not have a version, or if the version text is not a valid
     * version.
     *
     * Valid versions are expected to be dotted pairs of integers,
     * for example, "10.0".
     *
     * Use the parse cache to do this quickly.
     *
     * @param symName The versioned feature name which is to be parsed.
     *
     * @return The base name and version text of the feature name.
     */
    public String[] parseNameAndVersion(String symName) {
        String[] parts = cacheGet(symName);
        if (parts != null) {
            return parts;
        }

        String baseName;
        String version;

        int lastDash;
        if ((lastDash = symName.lastIndexOf('-')) >= 0) {
            baseName = symName.substring(0, lastDash);
            version = symName.substring(lastDash + 1);
        } else {
            baseName = symName;
            version = null;
        }

        parts = new String[] { baseName, version };

        cachePut(symName, parts);

        return parts;
    }
}
