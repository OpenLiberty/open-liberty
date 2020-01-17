/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.scim20.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.security.wim.scim20.exceptions.InvalidVersionException;
import com.ibm.websphere.security.wim.scim20.exceptions.NotFoundException;
import com.ibm.wsspi.rest.handler.RESTRequest;

public class RESTUtil {

    public static final String SCIM_VERSION_1_0 = "v1";
    public static final String SCIM_VERSION_2_0 = "v2";

    /**
     * A pattern to parse the SCIM URL of "/scim/{version}/{endpoint}/{id}", where {version} and {id} are optional.
     */
    private static final Pattern URL_PATH_PATTERN = Pattern.compile("/scim/?(v[^/]+)?/?([^/]+)/?(.+)?");

    /**
     * Parse the URL path provided and return the SCIM API version that is targeted.
     *
     * @param requestPath The request path
     * @return The API version for the requested URL path.
     * @throws InvalidVersionException If the requested SCIM API version is invalid.
     *
     * @see {@link #SCIM_VERSION_1_0}
     * @see {@link #SCIM_VERSION_2_0}
     */
    public static String getApiVersion(String requestPath) throws InvalidVersionException {

        String version = null;
        Matcher m = URL_PATH_PATTERN.matcher(requestPath);
        if (m.matches()) {
            version = m.group(1);
        }

        /*
         * Version, if specified, must be one of the supported SCIM versions.
         */
        if (version != null && !SCIM_VERSION_2_0.equals(version)) {
            throw new InvalidVersionException("The URL contained an invalid SCIM version (" + version + ")"); // TODO LOCALIZE
        } else {
            /*
             * Default to the highest version of SCIM that is enabled.
             *
             * TODO When supporting SCIM 1.0 and 2.0 on the same server, we need to check which
             * services are available and set this accordingly.
             */
            version = SCIM_VERSION_2_0;
        }

        return version;
    }

    /**
     * Parse the URL path provided and return the SCIM endpoint that is targeted.
     *
     * @param requestPath The request path
     * @return The SCIM endpoint for the requested URL path.
     * @throws NotFoundException If the requested SCIM endpoint is invalid.
     */
    public static String getEnpoint(String requestPath) throws NotFoundException {

        String endpoint = null;
        Matcher m = URL_PATH_PATTERN.matcher(requestPath);
        if (m.matches()) {
            endpoint = m.group(2);
        }

        return endpoint;
    }

    /**
     * Parse the URL path provided and return the resource ID that is targeted.
     *
     * @param requestPath The request path
     * @return The resource ID for the requested URL path.
     */
    public static String getResourceId(String requestPath) {

        String resourceId = null;
        Matcher m = URL_PATH_PATTERN.matcher(requestPath);
        if (m.matches()) {
            resourceId = m.group(3);
        }

        return resourceId;
    }

    /**
     * Get the set of SCIM attributes to exclude specified by the request.
     *
     * @param request The {@link RESTRequest}.
     * @return The {@link Set} of attributes requested to be excluded.
     */
    public static Set<String> getExcludedAttributes(RESTRequest request) {
        String[] attributes = request.getParameterValues("excludedAttributes");
        if (attributes == null || attributes.length == 0) {
            return Collections.emptySet();
        }

        return new HashSet<String>(Arrays.asList(attributes));
    }

    /**
     * Get the set of SCIM attributes to return specified by the request.
     *
     * @param request The {@link RESTRequest}.
     * @return The {@link Set} of attributes requested.
     */
    public static Set<String> getAttributes(RESTRequest request) {
        String[] attributes = request.getParameterValues("attributes");
        if (attributes == null || attributes.length == 0) {
            return Collections.emptySet();
        }

        return new HashSet<String>(Arrays.asList(attributes));
    }

    /**
     * Get the search filter specified on the request.
     *
     * @param request The {@link RESTRequest}.
     * @return The search filter specified on the request.
     */
    public static String getFilter(RESTRequest request) {
        String[] filter = request.getParameterValues("filter");
        if (filter == null || filter.length == 0 || filter[0] == null || filter[0].trim().isEmpty()) {
            return null;
        }

        return filter[0];
    }
}
