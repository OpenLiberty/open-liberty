/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

public class OpenAPIUtils {
    private static final TraceComponent tc = Tr.register(OpenAPIUtils.class);

    /**
     * The getSerializedJsonDocument method is generates an OpenAPI document from the specified model in the specified
     * format.
     *
     * @param openapi
     *     The OpenAPI model
     * @param format
     *     The format of the generated document
     * @return String
     * The generated OpenAPI document in JSON format
     */
    @Trivial
    @FFDCIgnore(IOException.class)
    public static String getOpenAPIDocument(final OpenAPI openAPIModel, final Format format) {
        // Create the variable to return
        String oasResult = null;

        // Make sure that we have a valid document
        if (openAPIModel != null) {
            try {
                oasResult = OpenApiSerializer.serialize(openAPIModel, format);
            } catch (IOException e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Failed to serialize OpenAPI document: " + e.getMessage());
                }
            }
        }

        return oasResult;
    }

    /**
     * The containsServersDefinition method checks whether the specified OpenAPI model defines any servers.
     *
     * @param openAPI
     *     The OpenAPI model
     * @return boolean
     * True iff the OpenAPI model already defines servers
     */
    @Trivial
    public static boolean containsServersDefinition(final OpenAPI openAPIModel) {
        // Create the variable to return
        boolean containsServers = false;

        // Return true if the model contains at least one server definition
        if (openAPIModel != null && openAPIModel.getServers() != null && openAPIModel.getServers().size() > 0) {
            containsServers = true;
        }

        return containsServers;
    }

    /**
     * The getOpenAPIModelServers method creates a list of server defintions based on the specified ServerInfo object
     * and application path.
     *
     * @param serverInfo
     *     The ServerInfo object to use when creating the new servers model
     * @param contextRoot
     *     The contextRoot for the application that is being processed
     * @return List<Server>
     * The list of Server objects
     */
    public static List<Server> getOpenAPIModelServers(final ServerInfo serverInfo, final String applicationPath) {
        // Create the variable to return
        List<Server> servers = new ArrayList<>();

        final int httpPort = serverInfo.getHttpPort();
        final int httpsPort = serverInfo.getHttpsPort();
        final String host = serverInfo.getHost();

        if (httpPort > 0) {
            String port = httpPort == 80 ? Constants.STRING_EMPTY : (Constants.STRING_COLON + httpPort);
            String url = Constants.SCHEME_HTTP + host + port;
            if (applicationPath != null) {
                url += applicationPath;
            }
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Adding OpenAPI model server: " + url);
            }
            Server server = OASFactory.createServer();
            server.setUrl(url);
            servers.add(server);
        }

        if (httpsPort > 0) {
            String port = httpsPort == 443 ? Constants.STRING_EMPTY : (Constants.STRING_COLON + httpsPort);
            String secureUrl = Constants.SCHEME_HTTPS + host + port;
            if (applicationPath != null) {
                secureUrl += applicationPath;
            }
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Adding OpenAPI model server: " + secureUrl);
            }
            Server secureServer = OASFactory.createServer();
            secureServer.setUrl(secureUrl);
            servers.add(secureServer);
        }

        return servers;
    }

    private OpenAPIUtils() {
        // This class is not meant to be instantiated.
    }

    /**
     * Check whether all elements of {@code collection} are equal to each other using the given equality function
     * <p>
     * Actually assumes that equals is implemented properly and just checks that the first element is equal to all others
     * <p>
     * If {@code collection} contains less than two elements, this method will always return {@code true}.
     *
     * @param <T> the element type
     * @param collection the collection of elements to test for equality
     * @param comparator the function to use to test equality
     * @return {@code true} if all elements of {@code collection} are equal, {@code false} otherwise
     */
    public static <T> boolean allEqual(Collection<? extends T> collection, BiPredicate<? super T, ? super T> comparator) {

        Iterator<? extends T> i = collection.iterator();
        if (!i.hasNext()) {
            return true;
        }

        T first = i.next();
        while (i.hasNext()) {
            T next = i.next();

            if (!equals(first, next, comparator)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests if two objects are equal, using {@code comparator} to test their equality if both {@code a} and {@code b} are not {@code null}.
     *
     * @param <T> the type of {@code a} and {@code b}
     * @param a the first object
     * @param b the second object
     * @param comparator the comparison function
     * @return {@code true} if {@code a} and {@code b} are equal, {@code false} otherwise
     */
    public static <T> boolean equals(T a, T b, BiPredicate<? super T, ? super T> comparator) {
        if (a == null) {
            return b == null ? true : false;
        } else {
            return b == null ? false : comparator.test(a, b);
        }
    }

    /**
     * Converts {@code null} to an empty map
     *
     * @param in a map, or {@code null}
     * @return an empty map if {@code in} is {@code null}, otherwise {@code in}
     */
    @Trivial
    public static <K, V> Map<K, V> notNull(Map<K, V> in) {
        if (in == null) {
            return Collections.emptyMap();
        } else {
            return in;
        }
    }

    /**
     * Converts {@code null} to an empty list
     *
     * @param in a list, or {@code null}
     * @return an empty list if {@code in} is {@code null}, otherwise {@code in}
     */
    @Trivial
    public static <V> List<V> notNull(List<V> in) {
        if (in == null) {
            return Collections.emptyList();
        } else {
            return in;
        }
    }
}
