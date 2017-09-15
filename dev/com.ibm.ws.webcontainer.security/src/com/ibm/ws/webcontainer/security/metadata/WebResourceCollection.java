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

import java.util.Collections;
import java.util.List;

import com.ibm.ws.webcontainer.security.metadata.CollectionMatch.MatchType;

/**
 * Represents a web-resource-collection element in web.xml.
 * <p/>
 * <pre>
 * &lt;web-resource-collection&gt;
 * &lt;url-pattern&gt;url-pattern&lt;/url-pattern&gt;
 * &lt;http-method&gt;http-method&lt;/http-method&gt;
 * &lt;http-method-omission&gt;http-method-omission&lt;/http-method-omission&gt;
 * &lt;/web-resource-collection&gt;
 * </pre>
 */
public class WebResourceCollection {

    private final List<String> urlPatterns;
    private final List<String> methods;
    private final List<String> omissionMethods;
    private final boolean matchAllMethods;
    private final boolean denyUncoveredHttpMethods;

    /**
     * Constructs a WebResourceCollection object. The user of this class guarantees that there are no null
     * urlPatterns or null methods. They are guaranteed to be empty lists if they do not contain any value.
     * 
     * @param urlPatterns The URL patterns. Cannot be <code>null</code>.
     * @param methods The HTTP methods. Cannot be <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public WebResourceCollection(List<String> urlPatterns, List<String> methods) {
        this(urlPatterns, methods, Collections.EMPTY_LIST);
    }

    /**
     * Constructs a WebResourceCollection object. The user of this class guarantees that there are no null
     * urlPatterns, null methods, or null omissionMethods. They are guaranteed to be empty lists if they do not contain any value.
     * 
     * @param urlPatterns The URL patterns. Cannot be <code>null</code>.
     * @param methods The HTTP methods. Cannot be <code>null</code>.
     * @param omissionMethods The HTTP omission methods. Cannot be <code>null</code>.
     */
    public WebResourceCollection(List<String> urlPatterns, List<String> methods, List<String> omissionMethods) {
        this(urlPatterns, methods, omissionMethods, false);
    }

    /**
     * Constructs a WebResourceCollection object. The user of this class guarantees that there are no null
     * urlPatterns, null methods, or null omissionMethods. They are guaranteed to be empty lists if they do not contain any value.
     * 
     * @param urlPatterns The URL patterns. Cannot be <code>null</code>.
     * @param methods The HTTP methods. Cannot be <code>null</code>.
     * @param omissionMethods The HTTP omission methods. Cannot be <code>null</code>.
     */
    public WebResourceCollection(List<String> urlPatterns, List<String> methods, List<String> omissionMethods, boolean denyUncoveredHttpMethods) {
        this.urlPatterns = urlPatterns;
        this.methods = methods;
        this.omissionMethods = omissionMethods;
        this.matchAllMethods = methods.isEmpty() && omissionMethods.isEmpty();
        this.denyUncoveredHttpMethods = denyUncoveredHttpMethods;

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
    public CollectionMatch performUrlMatch(String resourceName) {
        CollectionMatch match = null;
        String longestUrlPattern = null;
        for (String urlPattern : urlPatterns) {
            if (URLMatchingUtils.isExactMatch(resourceName, urlPattern)) {
                return new CollectionMatch(resourceName, MatchType.EXACT_MATCH);
            } else if (URLMatchingUtils.isPathNameMatch(resourceName, urlPattern)) {
                longestUrlPattern = URLMatchingUtils.getLongestUrlPattern(longestUrlPattern, urlPattern);
            } else if (URLMatchingUtils.isExtensionMatch(resourceName, urlPattern)) {
                match = new CollectionMatch(urlPattern, MatchType.EXTENSION_MATCH);
            }
        }
        if (longestUrlPattern != null) {
            match = new CollectionMatch(longestUrlPattern, MatchType.PATH_MATCH);
        }
        return match;
    }

    /**
     * Determines if the specified method is matched by this web resource collection.
     * A method is matched if all methods are allowed (empty HTTP methods and empty omission methods)
     * or if the method is listed in the HTTP methods, or not listed in the omission methods.
     * 
     * @param method The HTTP method to match.
     * @return A boolean value of <code>true</code> if the method is matched by this web resource collection.
     */
    public boolean isMethodMatched(String method) {

        return matchAllMethods || isValidMethod(method) || isNotAnOmissionMethod(method);
    }

    public boolean deniedDueToDenyUncoveredHttpMethods(String method) {
        if (denyUncoveredHttpMethods == false) {
            return false;
        } else {
            if (!isMethodMatched(method)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean isValidMethod(String method) {
        return methods.isEmpty() == false && methods.contains(method);
    }

    private boolean isNotAnOmissionMethod(String method) {
        return omissionMethods.isEmpty() == false && omissionMethods.contains(method) == false;
    }

    public boolean isSpecifiedOmissionMethod(String method) {
        return omissionMethods.isEmpty() == false && omissionMethods.contains(method) == true;
    }

    //add setter and getter methods for url patterns, methods, etc as needed
    public List<String> getUrlPatterns() {
        return urlPatterns;
    }

    public List<String> getHttpMethods() {
        return methods;
    }

    public List<String> getOmissionMethods() {
        return omissionMethods;
    }

    public boolean getDenyUncoveredHttpMethods() {
        return denyUncoveredHttpMethods;
    }

    /**
     * Determines if the specified method is listed by this web resource collection.
     * A method is listed if it is one of the http-method entries or one of the http-method-omission entries.
     * 
     * @param method The HTTP method.
     * @return A boolean value of <code>true</code> if the method is listed by this web resource collection.
     */
    public boolean isMethodListed(String method) {
        return methods.contains(method) || omissionMethods.contains(method);
    }

}
