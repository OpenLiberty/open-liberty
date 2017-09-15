/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.metadata;

import java.util.Collection;

/**
 * Common URL matching utilities
 */
public class URLMatchingUtils {

    /**
     * Determine the longest URL pattern.
     * 
     * @param firstUrlPattern
     * @param secondUrlPattern
     * @return the longest url pattern
     */
    public static String getLongestUrlPattern(String firstUrlPattern, String secondUrlPattern) {
        if (secondUrlPattern == null || (firstUrlPattern != null &&
            firstUrlPattern.length() >= secondUrlPattern.length())) {
            return firstUrlPattern;
        } else {
            return secondUrlPattern;
        }
    }

    /**
     * Gets the match for the resource name.
     * <pre>
     * To perform a URL match,
     * 1. For each URL pattern determine if the resource matches it
     * 2. Construct the match object
     * 
     * Exact match has first priority. The longest path match has second priority. The extension match has last priority.
     * </pre>
     * 
     * @param uriName
     * @return
     */
    static String performUrlMatch(String uri, Collection<String> urlPatterns) {
        String match = null;
        String longestUrlPattern = null;
        for (String urlPattern : urlPatterns) {
            if (URLMatchingUtils.isExactMatch(uri, urlPattern)) {
                return urlPattern;
            } else if (URLMatchingUtils.isPathNameMatch(uri, urlPattern)) {
                longestUrlPattern = URLMatchingUtils.getLongestUrlPattern(longestUrlPattern, urlPattern);
            } else if (URLMatchingUtils.isExtensionMatch(uri, urlPattern)) {
                match = urlPattern;
            }
        }
        if (longestUrlPattern != null) {
            match = longestUrlPattern;
        }
        return match;
    }

    /**
     * Determine if the urlPattern is an exact match for the uriName.
     * 
     * @param uriName
     * @param urlPattern
     * @return
     */
    public static boolean isExactMatch(String uriName, String urlPattern) {
        return urlPattern.equals(uriName);
    }

    /**
     * Determine if the urlPattern is a path name match for the uri.
     * 
     * @param uri
     * @param urlPattern
     * @return
     */
    public static boolean isPathNameMatch(String uri, String urlPattern) {
        if (urlPattern.startsWith("/") && urlPattern.endsWith("/*")) {
            String s = urlPattern.substring(0, urlPattern.length() - 1);
            /**
             * First case,urlPattern
             * /a/b/c/ matches /a/b/c/*
             **/
            if (s.equalsIgnoreCase(uri)) {
                return true;
            }
            /**
             * Second case
             * /a/b/c matches /a/b/c/*
             **/
            if (uri.equalsIgnoreCase(s.substring(0, s.length() - 1))) {
                return true;
            }

            /**
             * Third case
             * /a/b/c/d/e matches /a/b/c/*
             **/
            if (uri.startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determine if the urlPattern is an extension match for the uriName.
     * 
     * @param uriName
     * @param urlPattern
     * @return
     */
    public static boolean isExtensionMatch(String uriName, String urlPattern) {
        if (urlPattern.startsWith("*.")) {
            String ext = urlPattern.substring(1);
            if (uriName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
