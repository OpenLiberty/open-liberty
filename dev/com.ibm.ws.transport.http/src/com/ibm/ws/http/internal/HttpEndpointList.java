/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class HttpEndpointList implements Iterable<HttpEndpointImpl> {

    static final class Singleton {
        private static final HttpEndpointList instance = new HttpEndpointList();
    }

    /**
     * @return the singleton HttpEndpointMap instance.
     */
    @Trivial
    public static HttpEndpointList getInstance() {
        return Singleton.instance;
    }

    public static void registerEndpoint(HttpEndpointImpl h) {
        Singleton.instance.add(h);
    }

    public static void unregisterEndpoint(HttpEndpointImpl h) {
        Singleton.instance.remove(h);
    }

    /**
     * Return the endpoint for the given id.
     *
     * @param endpointId
     *
     * @return The endpoint associated with the id, or null.
     */
    public static HttpEndpointImpl findEndpoint(String endpointId) {
        for (HttpEndpointImpl i : Singleton.instance) {
            if (i.getName().equals(endpointId))
                return i;
        }
        return null;
    }

    // Use an array list because we won't have many so its less memory
    private final CopyOnWriteArrayList<HttpEndpointImpl> httpEndpoints = new CopyOnWriteArrayList<HttpEndpointImpl>();

    /**
     * @param h HttpEndpointImpl to add
     */
    private void add(HttpEndpointImpl h) {
        httpEndpoints.add(h);
    }

    /**
     * @param h HttpEndpointImpl to remove
     */
    private void remove(HttpEndpointImpl h) {
        httpEndpoints.remove(h);
    }

    /**
     * Return an iterator for the list of endpoints
     */
    @Trivial
    @Override
    public Iterator<HttpEndpointImpl> iterator() {
        return httpEndpoints.iterator();
    }
}
