/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.UriBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A class to collect the properties that the Client config services have read from server.xml,
 * so the JAX-RS code can access them in a convenient and performant way.
 *
 */
public class JAXRSClientConfigHolder {
    private static final TraceComponent tc = Tr.register(JAXRSClientConfigHolder.class);

    // a map of configuration properties keyed on  uri.
    // note that treemap sorts by key order, which we need to properly deal with the wildcards
    private static volatile Map<String, Map<String, String>> configInfo = new TreeMap<>();

    // a map of service object id's to uri, so we can track which service added which uri.
    private static volatile Map<String, String> uriMap = new HashMap<>();

    // a cached map of search results
    // that have been processed to look up the best match for the uri strings
    private static volatile Map<String, Map<String, String>> resolvedConfigInfo = new HashMap<>();

    private static boolean wildcardsPresentInConfigInfo = false;

    /**
     * add a configuration for a URL
     * We'd like a set of hashmaps keyed by URL, however when osgi calls
     * deactivate, we have no arguments, so we have to associate a url with
     * the object id of the service. That allows us to remove the right one.
     *
     * @param id - the object id of the service instance that added this.
     * @param url - the uri of the record being added. Might end with *
     * @param params - the properties applicable to this url.
     */
    public static synchronized void addConfig(String id, String uri, Map<String, String> params) {
        uriMap.put(id, uri);
        configInfo.put(uri, params);
        resolvedConfigInfo.clear();
        if (uri.endsWith("*")) {
            wildcardsPresentInConfigInfo = true;
        }
    }

    /**
     * remove a configuration (we'll look up the uri from the objectId)
     *
     * @param id - the object id of the service that called us
     */
    public static synchronized void removeConfig(String objectId) {
        String uri = uriMap.get(objectId);
        if (uri != null) {
            configInfo.remove(uri);
        }
        uriMap.remove(objectId);
        resolvedConfigInfo.clear();
    }

    /**
     * Find the applicable set of properties for a given URI, and return them.
     * Note that config uri's ending with * might match more than one uri.
     *
     * First we look for exact matches, then if applicable wildcard matches are found,
     * we merge those into the results in order of most general to most specific.
     *
     * There is some interaction with jax-rs that can produce some unexpected results:
     * ClientBuilder.newClient().target("http://cartoons").path("/bugs").path("/daffy")
     * would be an exact match for http://cartoons, and http://cartoons/bugs and
     * http://cartoons/bugs/daffy, because of the way jax-rs builds webtargets from prior targets.
     * (The webtarget ctor gets called 3 times in the above case, and the matches are evaluated
     * each time, and passed along the chain.) If someone has an exact match for cartoons
     * and complains it's being applied to cartoons/bugs/daffy
     * in the above case, that's working as designed.
     *
     *
     * @param uri - the uri to retrieve the props for, probably of form http(s)://something...(*)
     * @return - map of merged applicable properties, or null if no applicable props found
     */
    public static Map<String, String> getURIProps(UriBuilder uriBuilder) {
        boolean debug = tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled();
        if (configInfo.isEmpty()) {
            if (debug) {
                Tr.debug(tc, "getUriprops is empty, return null");
            }
            return null;
        }

        String uri = uriBuilder.build().toString();
        // look in the cache in case we've resolved this before
        synchronized (resolvedConfigInfo) {
            Map<String, String> props = resolvedConfigInfo.get(uri);
            if (props != null) {
                if (debug) {
                    Tr.debug(tc, "getUriprops cache hit, uri: " + uri + "props: " + props);
                }
                return (props.isEmpty() ? null : props);
            }
        }

        // at this point we might have to merge something, set up a new hashmap to hold the results
        HashMap<String, String> mergedProps = new HashMap<>();
        synchronized (JAXRSClientConfigHolder.class) {
            // try for exact match
            Map<String, String> props = configInfo.get(uri);
            if (props != null) {
                if (debug) {
                    Tr.debug(tc, "getUriprops exact match: " + uri);
                }
                mergedProps.putAll(props);
            }
        }

        if (!wildcardsPresentInConfigInfo) {
            if (debug) {
                Tr.debug(tc, "getUriprops no wildcards, cache and return what we've got: " + mergedProps);
            }
            synchronized (JAXRSClientConfigHolder.class) {
                resolvedConfigInfo.put(uri, mergedProps);
            }
            return (mergedProps.isEmpty() ? null : mergedProps);
        }

        // if a wildcard match exists, merge it to what we have already.
        if (debug) {
            Tr.debug(tc, "begin wildcard search");
        }
        // try to find a match from wildcards
        synchronized (JAXRSClientConfigHolder.class) {
            Iterator<String> it = configInfo.keySet().iterator();
            String trimmedKey = null;
            while (it.hasNext()) {
                String key = it.next();
                if (key.endsWith("*")) {
                    trimmedKey = key.substring(0, key.length() - 1);
                } else {
                    continue;
                }
                // if we got here, we have a wildcard
                // since configinfo is a sorted treemap,
                // general results should come first, followed by more specific results
                // if we have two keys of same length and both match, last one wins
                if (uri.startsWith(trimmedKey)) {
                    if (debug) {
                        Tr.debug(tc, "getUriprops match = " + key + ", will merge these props: " + configInfo.get(key));
                    }
                    mergedProps.putAll(configInfo.get(key)); // merge props for this wildcard.
                }
            }
            // at this point everything has been merged,  cache and return what we have.
            // if there was no match anywhere, this is an empty map.
            resolvedConfigInfo.put(uri, mergedProps);
            if (debug) {
                Tr.debug(tc, "getUriprops final result for uri: " + uri + "values: " + mergedProps);
            }
            return (mergedProps.isEmpty() ? null : mergedProps);
        } // end sync block
    }
}
