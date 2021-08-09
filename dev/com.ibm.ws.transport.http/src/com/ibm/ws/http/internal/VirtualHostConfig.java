/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.ws.http.internal.VirtualHostImpl.EndpointState;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

/**
 * Snapshot of configuration. This is the item added to and removed from the VirtualHostMap.
 * It maintains consistency between certain settings (allowedEndpoints and hostAliases in particular)
 * when adding and removing hosts, where you need to remove using the old cnofiguration while adding
 * with the new configuration.
 */
class VirtualHostConfig {
    private final VirtualHostImpl owner;

    /** Set to true in activate if this is the default host (which has unique resolution behavior.. ) */
    final boolean isDefaultHost;

    final boolean enabled;

    /** Collection of allowed endpoints */
    final Collection<String> allowedEndpointPids;

    /** List of configured host aliases */
    private volatile Collection<HostAlias> hostAliases;

    public VirtualHostConfig(VirtualHostImpl owner) {
        this.owner = owner;
        enabled = false;
        isDefaultHost = false;
        allowedEndpointPids = Collections.emptyList();
        hostAliases = Collections.emptyList();
        // The hostAlias collection should be considered immutable. Making changes to the collections could result
        // in ConcurrentModificationExceptions in toString().
        hostAliases = Collections.unmodifiableCollection(hostAliases);
    }

    /**
     * Create immutable object based on configuration properties
     *
     * @param properties
     */
    public VirtualHostConfig(VirtualHostImpl owner, Map<String, Object> properties) {
        this.owner = owner;
        boolean newEnabled = MetatypeUtils.parseBoolean(HttpServiceConstants.VHOST_FPID_ALIAS,
                                                        HttpServiceConstants.ENABLED,
                                                        properties.get(HttpServiceConstants.ENABLED),
                                                        true);

        Collection<String> newAliasStrings = MetatypeUtils.parseStringCollection(HttpServiceConstants.VHOST_FPID_ALIAS,
                                                                                 HttpServiceConstants.VHOST_HOSTALIAS,
                                                                                 properties.get(HttpServiceConstants.VHOST_HOSTALIAS),
                                                                                 null);

        boolean newDefaultHost = false;
        Collection<HostAlias> newAliases = Collections.emptyList();

        if (owner.name.equals(HttpServiceConstants.DEFAULT_VHOST)) {
            // force the default_host to be enabled: serious weirdness if it is disabled!
            if (!newEnabled)
                Tr.warning(VirtualHostImpl.tc, "defaultHostDisabled");

            newEnabled = true;

            if (newAliasStrings == null || newAliasStrings.isEmpty()) {
                // If there are no explicitly defined aliases, this behaves as the
                // true default host: host aliases are generated based on configured
                // endpoints.
                newDefaultHost = true;
                newAliases = new HashSet<HostAlias>();
                newAliases.add(new HostAlias(HttpServiceConstants.WILDCARD, -1));
            }
        }

        if (newEnabled && !newDefaultHost) {
            // If this isn't going to be the "default host" that uses generated
            // aliases to catch all incoming requests, we need to process the configured
            // hostAliases
            if (newAliasStrings != null && !newAliasStrings.isEmpty()) {
                newAliases = new HashSet<HostAlias>();
                // Validate the inbound aliases..
                for (String alias : newAliasStrings) {
                    // split into host/port -- check validity
                    HostAlias hs = new HostAlias(alias, owner.name);
                    if (!hs.isValid) {
                        // This alias is invalid!! The warning message will already have been issued,
                        // but we're going to remove this alias from the list --> skip it
                        continue;
                    }

                    newAliases.add(hs);
                }
            }

            // So. there might have been aliases in the list... but what if they were all invalid?
            // After we've gone through trying to use what they've defined, warn if there are no
            // valid ones remaining
            if (newAliases.isEmpty()) {
                Tr.warning(VirtualHostImpl.tc, "noHostAliases", owner.name);
            }
        }

        Collection<String> endpoints = processAllowedEndpoints(MetatypeUtils.parseStringCollection(HttpServiceConstants.VHOST_FPID_ALIAS,
                                                                                                   HttpServiceConstants.VHOST_ALLOWED_ENDPOINT,
                                                                                                   properties.get(HttpServiceConstants.VHOST_ALLOWED_ENDPOINT),
                                                                                                   null));

        enabled = newEnabled;
        isDefaultHost = newDefaultHost;
        hostAliases = newAliases;
        if (endpoints == null)
            allowedEndpointPids = Collections.emptyList();
        else
            allowedEndpointPids = endpoints;

    }

    /**
     * @return true if this is the default/catch-all host
     */
    public boolean isDefaultHost() {
        return isDefaultHost;
    }

    public boolean isSameVirtualHost(VirtualHostConfig other) {
        if (other == null)
            return false;

        return this == other ||
               this.getVirtualHost() == other.getVirtualHost();
    }

    public String getName() {
        return owner.name;
    }

    /**
     * @param endpointPid
     * @return true if access to this virtual host is permitted from the specified endpoint.
     */
    public boolean acceptFromEndpoint(String endpointPid) {
        Collection<String> endpoints = allowedEndpointPids;
        if (!endpoints.isEmpty()) {
            return endpoints.contains(endpointPid);
        }
        return true;
    }

    /**
     * @return
     */
    public Collection<HostAlias> getHostAliases() {
        return Collections.unmodifiableCollection(hostAliases);
    }

    public Collection<String> getAllowedEndpoints() {
        return Collections.unmodifiableCollection(allowedEndpointPids);
    }

    public synchronized Collection<HostAlias> regenerateAliases() {
        if (isDefaultHost) {
            Set<HostAlias> newAliases = new HashSet<HostAlias>();

            // The default host is * on all listening/associated endpoints.
            for (EndpointState es : owner.myEndpoints.values()) {
                if (es.httpPort > 0)
                    newAliases.add(new HostAlias(HttpServiceConstants.WILDCARD, es.httpPort));
                if (es.httpsPort > 0)
                    newAliases.add(new HostAlias(HttpServiceConstants.WILDCARD, es.httpsPort));
            }

            // Only add the wildcard if at least one endpoint is listening!
            if (!newAliases.isEmpty()) {
                newAliases.add(new HostAlias(HttpServiceConstants.WILDCARD, -1));
            }
            hostAliases = newAliases;
            return Collections.unmodifiableCollection(newAliases);
        }

        return Collections.unmodifiableCollection(hostAliases);
    }

    @Override
    public String toString() {
        return "config[name=" + owner.name
               + ",enabled=" + enabled
               + ",hostAliases=" + hostAliases
               + ",isDefaultHost=" + isDefaultHost
               + ",allowedEndpoints=" + allowedEndpointPids
               + "]";
    }

    /**
     * Check the incoming list of endpoint pids (from configuration). If the list is null
     * or empty, return null. Otherwise return the list.
     *
     * @param endpointIds
     * @return null (all endpoints), or trimmed list of allowed endpoints
     * @see #acceptFromEndpoint(String)
     */
    private Collection<String> processAllowedEndpoints(Collection<String> endpointIds) {
        if (endpointIds == null || endpointIds.isEmpty())
            return null;

        return endpointIds;
    }

    /**
     * @return
     */
    public VirtualHostImpl getVirtualHost() {
        return owner;
    }

    /**
     * @param e
     * @param resolvedHostName
     * @param ePort
     * @param b
     */
    public void listenerStarted(HttpEndpointImpl e, String resolvedHostName, int port, boolean isHttps) {
        owner.listenerStarted(e, this, resolvedHostName, port, isHttps);
    }

    /**
     * @param e
     * @param resolvedHostName
     * @param ePort
     * @param b
     */
    public void listenerStopped(HttpEndpointImpl e, String resolvedHostName, int port, boolean isHttps) {
        owner.listenerStopped(e, this, resolvedHostName, port, isHttps);
    }
}