/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.netty.MSP;

/**
 * This map bridges between independent dynamic components: HttpEndpoints (with their Chains), and
 * VirtualHosts.
 * <p>
 * Ultimately, a VirtualHostDiscriminator is created for each port that is either configured
 * in a VirtualHost hostAlias, as an HttpEndpoint httpPort or httpsPort or (ideally) both.
 * The port-centric VirtualHostDiscriminator is used by the HttpDispatcher channel to decide
 * which virtual host should be used to handle a request based on the hostname used by the
 * client to make the request.
 * See {@link VirtualHostDiscriminator#selectVirtualHost(String)} <p>
 * So, how do VirtualHostDiscriminators get made? There are two ways, with the addition of
 * a VirtualHost, or when an HttpEndpoint starts listening.
 * <p>
 * When a VirtualHost is activated or modified, {@link #addVirtualHost(VirtualHostImpl, Collection, Collection)} is
 * called to add the virtual host and its collection of aliases to the map. If previous/old
 * aliases are present, those are removed first. Addition/Removal of a VirtualHost alias entails:
 * splitting the alias to get the host and the port, and looking in the list of VirtualHostDiscriminators
 * to see if there is one for the given port. In the case of adding a virtual host, a non-existent
 * VirtualHostDiscriminator would be created at that time.
 * <p>
 * When an HttpChain has finished starting and successfully bound to a port, it
 * calls {@link VirtualHostDiscriminator#addEndpoint(HttpEndpointImpl, String, boolean)} to
 * add a VirtualHostDiscriminator for the listening port (which is created if
 * it did not already exist).
 * <p>
 * If there are no virtual hosts configured, there will be one VirtualHostDiscriminator
 * created per listening port, and that discriminator will always return the default_host.
 *
 *
 * @see VirtualHostDiscriminator
 * @see VirtualHostDiscriminator#addVirtualHost(String, VirtualHostImpl)
 * @see VirtualHostDiscriminator#removeVirtualHost(String, VirtualHostImpl)
 * @see VirtualHostDiscriminator#addEndpoint(HttpEndpointImpl, String, boolean)
 * @see VirtualHostDiscriminator#removeEndpoint(HttpEndpointImpl, String, boolean)
 *
 */
public class VirtualHostMap {
    static final TraceComponent tc = Tr.register(VirtualHostMap.class);

    private static volatile VirtualHostConfig defaultHost = null;
    private static volatile AlternateHostSelector alternateHostSelector = null;

    /**
     * Simple interface to allow deferred retrieval of host/port information
     * from the request (based on Host header, URI, and private headers)
     * until needed.
     */
    public interface RequestHelper {
        String getRequestedHost();

        int getRequestedPort();
    }

    /**
     * Add (or update) a virtual host: If the virtual host was previously registered,
     * oldConfig will be non-null, and will contain the attributes the host was previously
     * registered/added with.
     *
     * @param oldConfig Snapshot of previous config for a virtual host
     * @param newConfig Snapshot of new config for a virtual host
     *
     * @see VirtualHostImpl#modified
     */
    public synchronized static void addVirtualHost(final VirtualHostConfig oldConfig, final VirtualHostConfig newConfig) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Add virtual host: " + newConfig.getVirtualHost(), oldConfig, newConfig, defaultHost, alternateHostSelector);
        }

        // If the "default/catch-all-ness" of the host was modified,
        // the virtual host was removed before calling this method.
        if (newConfig.isDefaultHost()) {
            defaultHost = newConfig;

            if (alternateHostSelector != null) {
                alternateHostSelector.alternateAddDefaultHost(newConfig);
            } else {
                // notify default host of  active listeners
                for (HttpEndpointImpl e : HttpEndpointList.getInstance()) {
                    int ePort = e.getListeningHttpPort();
                    if (ePort > 0) {
                        defaultHost.listenerStarted(e, e.getResolvedHostNameSupplier(), ePort, false);
                    }

                    ePort = e.getListeningSecureHttpPort();
                    if (ePort > 0) {
                        defaultHost.listenerStarted(e, e.getResolvedHostNameSupplier(), ePort, true);
                    }

                }
            }
        } else {
            if (alternateHostSelector == null) {
                alternateHostSelector = new AlternateHostSelector();
            }
            AlternateHostSelector current = alternateHostSelector;
            // Figure out which host aliases should be removed
            List<HostAlias> toRemove = new ArrayList<HostAlias>(oldConfig.getHostAliases());
            toRemove.removeAll(newConfig.getHostAliases());

            if (!toRemove.isEmpty()) {
                current.alternateRemoveVirtualHost(oldConfig, toRemove);
            }

            current.alternateAddVirtualHost(newConfig);
        }
    }

    /**
     * Remove a virtual host: called if a virtual host is deactivated or disabled
     *
     * @param config Virtual host configuration to remove
     *
     * @see VirtualHostImpl#modified(Map)
     * @see VirtualHostImpl#deactivate(org.osgi.service.component.ComponentContext, int)
     */
    public synchronized static void removeVirtualHost(VirtualHostConfig config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Remove virtual host: " + config.getVirtualHost(), config, defaultHost, alternateHostSelector);
        }
        if (config.isDefaultHost()) {
            defaultHost = null;
            if (alternateHostSelector != null) {
                alternateHostSelector.alternateRemoveDefaultHost(config);
            } else {
                // notify default host of  active listeners
                for (HttpEndpointImpl e : HttpEndpointList.getInstance()) {
                    int ePort = e.getListeningHttpPort();
                    if (ePort > 0) {
                        config.listenerStopped(e, e.getResolvedHostName(), ePort, false);
                    }

                    ePort = e.getListeningSecureHttpPort();
                    if (ePort > 0) {
                        config.listenerStopped(e, e.getResolvedHostName(), ePort, true);
                    }

                }
            }
        } else if (alternateHostSelector != null) {
            // remove all configured host aliases.
            alternateHostSelector.alternateRemoveVirtualHost(config, config.getHostAliases());
        }
    }

    /**
     * Add an endpoint that has started listening, and notify associated virtual hosts
     *
     * @param endpoint         The HttpEndpointImpl that owns the started chain/listener
     * @param hostNameResolver A hostname resolver that can be used in messages (based on endpoint configuration, something other than *)
     * @param port             The port the endpoint is listening on
     * @param isHttps          True if this is an SSL port
     * @see HttpChain#chainStarted(com.ibm.websphere.channelfw.ChainData)
     */
    public static synchronized void notifyStarted(HttpEndpointImpl endpoint, Supplier<String> hostNameResolver, int port, boolean isHttps) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Notify endpoint started: " + endpoint, hostNameResolver, port, isHttps, defaultHost, alternateHostSelector);
        }
        if (alternateHostSelector == null) {
            if (defaultHost != null) {
                defaultHost.listenerStarted(endpoint, hostNameResolver, port, isHttps);
            }

        } else {
            alternateHostSelector.alternateNotifyStarted(endpoint, hostNameResolver, port, isHttps);
        }
    }

    /**
     * Remove a port associated with an endpoint that has stopped listening,
     * and notify associated virtual hosts.
     *
     * @param endpoint         The HttpEndpointImpl that owns the stopped chain/listener
     * @param resolvedHostName A hostname that can be used in messages (based on endpoint configuration, something other than *)
     * @param port             The port the endpoint has stopped listening on
     * @param isHttps          True if this is an SSL port
     * @see HttpChain#chainQuiesced(com.ibm.websphere.channelfw.ChainData)
     * @see HttpChain#chainStopped(com.ibm.websphere.channelfw.ChainData)
     */
    public static synchronized void notifyStopped(HttpEndpointImpl endpoint, String resolvedHostName, int port, boolean isHttps) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Notify endpoint stopped: " + endpoint, resolvedHostName, port, isHttps, defaultHost, alternateHostSelector);
        }
        MSP.log("VHOST notifyStopped received");
        if (alternateHostSelector == null) {
            if (defaultHost != null) {
                defaultHost.listenerStopped(endpoint, resolvedHostName, port, isHttps);
            }

        } else {
            alternateHostSelector.alternateNotifyStopped(endpoint, resolvedHostName, port, isHttps);
        }
        MSP.log("VHOST notifyStopped exit");
    }

    /**
     * Find the virtual host that should be used for the given host/port..
     *
     * @param endpointPid The endpoint the request came in on
     * @param helper      a RequestHelper allows deferred processing of request header information.
     *
     * @return the VirtualHostImpl that should service the request
     */
    public static VirtualHostImpl findVirtualHost(String endpointPid,
                                                  RequestHelper helper) {
        // defer retrieval of headers until we need them:
        // if we don't have virtual hosts to select from, we don't care what the
        // headers are, there is only one answer.
        AlternateHostSelector selector = alternateHostSelector;
        if (selector == null) {
            VirtualHostConfig defHostCfg = defaultHost;
            if (defHostCfg != null) {
                return defHostCfg.getVirtualHost();
            }

            return null;
        }

        // Now that we know we have virtual hosts to choose from, do the evaluation of http headers
        // to figure out what the requested host/port were..
        return selector.findVirtualHost(endpointPid, helper.getRequestedHost(), helper.getRequestedPort());
    }

    private static class AlternateHostSelector {

        private static final Map<Integer, VirtualHostDiscriminator> discriminators = new HashMap<Integer, VirtualHostDiscriminator>();

        AlternateHostSelector() {
            // Find any already existing endpoints that we may have missed (late arrival of alternate
            // virtual host.. )
            for (HttpEndpointImpl e : HttpEndpointList.getInstance()) {
                int ePort = e.getListeningHttpPort();
                if (ePort > 0) {
                    VirtualHostDiscriminator d = findOrCreateDiscriminator(ePort);
                    d.addEndpoint(e, e.getResolvedHostNameSupplier(), false);
                }

                ePort = e.getListeningSecureHttpPort();
                if (ePort > 0) {
                    VirtualHostDiscriminator d = findOrCreateDiscriminator(ePort);
                    d.addEndpoint(e, e.getResolvedHostNameSupplier(), true);
                }
            }
        }

        private VirtualHostDiscriminator findOrCreateDiscriminator(int port) {
            VirtualHostDiscriminator d = discriminators.get(port);
            if (d == null) {
                d = new VirtualHostDiscriminator(port);
                discriminators.put(port, d);
            }
            return d;
        }

        /**
         * Find the right virtual host to process a given request
         *
         * @param endpointPid The endpoint/origin of the request
         * @param hostName    The requested hostname, from the Host header..
         * @param port        The requested port, from the Host header..
         *
         * @return the virtual host that should handle the request, or null
         *         if there was no match.
         */
        VirtualHostImpl findVirtualHost(String endpointPid,
                                        String hostName,
                                        int port) {
            VirtualHostConfig target = null;
            VirtualHostDiscriminator d = discriminators.get(port);
            if (d != null) {
                target = d.selectVirtualHost(hostName.toLowerCase(Locale.ENGLISH));
            } else {
                target = defaultHost;
            }

            // Make sure the target is ready to accept request from the selected endpoint..
            if (target != null && target.acceptFromEndpoint(endpointPid))
                return target.getVirtualHost();

            return null;
        }

        /**
         * Synchronous caller...
         *
         * @param endpoint
         * @param hostName
         * @param port
         * @param isHttps
         */
        void alternateNotifyStarted(HttpEndpointImpl endpoint, Supplier<String> hostNameResolver, int port, boolean isHttps) {
            // will not return null: a new discriminator will be created if it doesn't exist already..
            VirtualHostDiscriminator d = findOrCreateDiscriminator(port);
            d.addEndpoint(endpoint, hostNameResolver, isHttps);
        }

        /**
         * Synchronous caller...
         *
         * @param endpoint
         * @param hostName
         * @param port
         * @param isHttps
         */
        void alternateNotifyStopped(HttpEndpointImpl endpoint, String hostName, int port, boolean isHttps) {
            VirtualHostDiscriminator d = discriminators.get(port);
            if (d != null) {
                d.removeEndpoint(endpoint, hostName, isHttps);
                if (d.cleanup())
                    discriminators.remove(port);
            }
        }

        /**
         * Synchronous caller...
         *
         * @param endpoint
         * @param hostName
         * @param port
         * @param isHttps
         */
        void alternateAddDefaultHost(VirtualHostConfig config) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Add default host: " + config.getVirtualHost());
            }
            // this is the default host: notify all discriminators
            for (VirtualHostDiscriminator d : discriminators.values()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "try adding default host as wildcard host for " + d);
                }
                d.addVirtualHost(HttpServiceConstants.WILDCARD, config);
            }
        }

        /**
         * Synchronous caller...
         *
         * @param config
         *
         * @param endpoint
         * @param hostName
         * @param port
         * @param isHttps
         */
        void alternateRemoveDefaultHost(VirtualHostConfig config) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Remove default host: " + config.getVirtualHost());
            }
            // this is the default host: notify all discriminators
            for (Iterator<VirtualHostDiscriminator> i = discriminators.values().iterator(); i.hasNext();) {
                VirtualHostDiscriminator d = i.next();
                d.removeVirtualHost(HttpServiceConstants.WILDCARD, config);
                if (d.cleanup())
                    i.remove();
            }
        }

        /**
         * Synchronous caller...
         *
         * @param endpoint
         * @param hostName
         * @param port
         * @param isHttps
         */
        void alternateAddVirtualHost(VirtualHostConfig config) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Add virtual host: " + config);
            }
            for (HostAlias ha : config.getHostAliases()) {
                VirtualHostDiscriminator d = findOrCreateDiscriminator(ha.port);
                d.addVirtualHost(ha.hostName, config);

                // after we add the virtual host, see if there are active endpoints for the port.
                // We need to make sure that all hosts associated with the discriminator
                // are notified: due to synchronization, we can discover the active endpoint here
                // before its notification comes around (i.e. it gets added to the discriminator's set)
                d.findActiveEndpoints();
            }
        }

        /**
         * Synchronous caller...
         *
         * @param endpoint
         * @param hostName
         * @param port
         * @param isHttps
         */
        void alternateRemoveVirtualHost(VirtualHostConfig config, Collection<HostAlias> toRemove) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Remove virtual host: " + config.getVirtualHost(), config);
            }
            for (HostAlias ha : toRemove) {
                VirtualHostDiscriminator d = discriminators.get(ha.port);
                if (d != null) {
                    d.removeVirtualHost(ha.hostName, config);

                    if (d.cleanup())
                        discriminators.remove(d.port);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getClass().getSimpleName()).append('[');
            sb.append("discriminators(").append(discriminators.size()).append(")=[");
            for (Map.Entry<Integer, VirtualHostDiscriminator> entry : discriminators.entrySet()) {
                sb.append(entry.getKey()).append('=').append(entry.getValue()).append(",");
            }
            sb.append("]]");
            return sb.toString();
        }
    }

    /**
     * Provide a thread-safe view for vhost lookup
     *
     * @see VirtualHostDiscriminator#addEndpoint(HttpEndpointImpl, String, boolean)
     * @see VirtualHostDiscriminator#addVirtualHost(String, VirtualHostImpl)
     * @see VirtualHostDiscriminator#selectVirtualHost(String)
     */
    @Trivial
    private static class HostTuple {
        final boolean hasMoreHosts;
        final Map<String, VirtualHostConfig> otherHosts;
        final VirtualHostConfig wildcardHost;

        @Trivial
        HostTuple(Map<String, VirtualHostConfig> otherHosts, VirtualHostConfig wildcardHost) {
            this.otherHosts = otherHosts;
            this.wildcardHost = wildcardHost;
            this.hasMoreHosts = otherHosts != null || wildcardHost != null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            sb.append(otherHosts);
            sb.append(',');
            sb.append(wildcardHost);
            sb.append('}');
            return sb.toString();
        }
    }

    /**
     * Port-centric discriminator. There is one of these per port mentioned either by a listening
     * endpoint, or a virtual host alias.
     */
    private static class VirtualHostDiscriminator {
        final int port;
        final String portString;

        /** List of associated endpoints: this will usually be only 1. */
        final HashSet<HttpEndpointImpl> endpoints = new HashSet<HttpEndpointImpl>(1);

        /**
         * These are changed together: there is a map of hostnames to VirtualHost, a wildcard-catching VirtualHost,
         * and a boolean indicator summing up whether or not there are any VirtualHosts
         * assigned.
         *
         * Use of the tuple is important because while the add/remove methods are synchronized, {@link #selectVirtualHost(String)} is
         * not, and that path is very performance sensitive, as it is in the main-line request path.
         *
         * @see #selectVirtualHost(String)
         */
        volatile HostTuple currentHosts = new HostTuple(null, defaultHost);

        VirtualHostDiscriminator(int port) {
            this.port = port;
            this.portString = String.valueOf(port);
        }

        /**
         * Synchronized caller...
         * Add a virtual host to the discriminator.
         *
         * Change the tuple all at once: any threads doing lookups will see
         * the most current tuple, rather than partial changes.
         *
         * @param hostName       host portion of the alias: * or somehost.whatever
         * @param vhost
         * @param newDefaultHost - is this the default/catch-all host
         *
         * @see #selectVirtualHost(String)
         */
        void addVirtualHost(String hostName, VirtualHostConfig config) {
            HostTuple snapshot = currentHosts;

            if (HttpServiceConstants.WILDCARD.equals(hostName)) {
                if (snapshot.wildcardHost != null) {
                    // If there is a previously set wildcardHost, we should check for
                    // a duplicate alias.
                    if (snapshot.wildcardHost.isSameVirtualHost(config)) {
                        // Config update for the same virtual host: notifications should go
                        // to make sure the endpoint is correctly associated with the vhost
                    } else if (snapshot.wildcardHost.isDefaultHost()) {
                        // If the old wildcard host is the default host, we need to remove
                        // the default host...
                        removeVirtualHost(hostName, snapshot.wildcardHost);
                    } else {
                        // This is a duplicate alias
                        // Should never be a warning about the default/catch-all host
                        if (!config.isDefaultHost())
                            Tr.warning(tc, "duplicateAlias", hostName + ":" + port, config.getName());
                        return;
                    }
                }

                // Create a new host tuple with the new virtual host as the wildcard host..
                currentHosts = new HostTuple(snapshot.otherHosts, config);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Added/Updated {0} as wildcard virtual host on port {1}: {2}", config.getName(), portString, this);
            } else {
                Map<String, VirtualHostConfig> others = snapshot.otherHosts;
                if (others == null)
                    others = new HashMap<String, VirtualHostConfig>(3);

                VirtualHostConfig prev = others.get(hostName);
                if (prev != null) {
                    if (prev.isSameVirtualHost(config)) {
                        // Config update for the same virtual host: notifications should go
                        // to make sure the endpoint is correctly associated with the vhost
                    } else {
                        Tr.warning(tc, "duplicateAlias", hostName + ":" + port, config.getName());
                        return; // duplicate, nothing to do.
                    }
                }

                // Create a new host tuple with the updated "other hosts" list
                others.put(hostName, config);
                currentHosts = new HostTuple(others, snapshot.wildcardHost);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Added/updated {0} to list of virtual hosts on port {1}: {2}", config.getName(), portString, this);
            }

            // Notify virtual host of existing endpoints
            notifyVHostExistingEndpoints(config, hostName, true);
        }

        /**
         * Synchronized caller...
         * Remove a virtual host from the discriminator.
         *
         * Change the tuple all at once: any threads doing lookups will see
         * the most current tuple, rather than partial changes.
         *
         * @param hostName       host portion of the alias: * or somehost.whatever
         * @param vhost          VirtualHost to remove
         * @param oldDefaultHost - was this the default/catch-all host
         * @see #selectVirtualHost(String)
         */
        void removeVirtualHost(String hostName, VirtualHostConfig config) {
            HostTuple vhosts = currentHosts;

            if (HttpServiceConstants.WILDCARD.equals(hostName)) {
                if (vhosts.wildcardHost.isSameVirtualHost(config)) {
                    // If the wildcard host is the host we're removing, update the tuple
                    currentHosts = new HostTuple(vhosts.otherHosts, null);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Removed {0} as wildcard virtual host on port {1}: {2}", config.getName(), portString, this);

                    // if we're removing a non-default host, and there is a default host around.. add the default host
                    // as the catch-all for this alias
                    if (!config.isDefaultHost() && defaultHost != null) {
                        addVirtualHost(hostName, defaultHost);
                    }
                }
            } else {
                Map<String, VirtualHostConfig> others = vhosts.otherHosts;
                if (others != null) {
                    others.remove(hostName);

                    // clear the other hosts map if it is now empty
                    if (others.isEmpty())
                        currentHosts = new HostTuple(null, vhosts.wildcardHost);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Removed {0} from list of virtual hosts on port {1}: {2}", config.getName(), portString, this);
            }

            // Notify that the virtual host is being removed
            notifyVHostExistingEndpoints(config, hostName, false);
        }

        /**
         * Synchronized caller..
         *
         * Notify the indicated virtual host of existing endpoints
         *
         * @param targetVHost VirtualHost to notify
         * @param added       true if listening/started endpoints should be added (new virtual host arrived),
         *                        false if they should be removed (virtual host removal).
         */
        void notifyVHostExistingEndpoints(VirtualHostConfig config, String aliasHost, boolean added) {
            if (config != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Notify {0} of endpoint {3} on port {1} for {4}: {2}",
                             config.getName(),
                             portString,
                             this,
                             added ? "added" : "removed",
                             aliasHost);

                // Tell the target virtual host about the port this discriminator is for
                for (HttpEndpointImpl e : endpoints) {
                    if (config.acceptFromEndpoint(e.getPid())) {
                        Supplier<String> msgHostName;
                        if (HttpServiceConstants.WILDCARD.equals(aliasHost)) {
                            msgHostName = e.getResolvedHostNameSupplier();
                        } else {
                            msgHostName = () -> aliasHost;
                        }

                        // If the port for this discriminator matches the secure listening port
                        // of the endpoint, then this is an https listener..
                        boolean isHttps = port == e.getListeningSecureHttpPort();

                        if (added)
                            config.listenerStarted(e, msgHostName, port, isHttps);
                        else
                            config.listenerStopped(e, msgHostName.get(), port, isHttps);
                    }
                }
            }
        }

        /**
         * Find any listening endpoints that match this discriminator, and make
         * sure all hosts w/ aliases associated with this port (or the default_host)
         * get notified.
         */
        private void findActiveEndpoints() {
            for (HttpEndpointImpl e : HttpEndpointList.getInstance()) {
                int ePort = e.getListeningHttpPort();
                if (ePort == port) {
                    addEndpoint(e, e.getResolvedHostNameSupplier(), false);
                }

                ePort = e.getListeningSecureHttpPort();
                if (ePort == port) {
                    addEndpoint(e, e.getResolvedHostNameSupplier(), true);
                }
            }
        }

        /**
         * Synchronized caller...
         * Notify virtual hosts that an endpoint is listening on this port..
         *
         * @param endpoint
         * @param hostNameResolver Hostname resolver suitable for use in a message (a real hostname, not *)
         * @param isHttps          true if this is an https port.
         */
        void addEndpoint(HttpEndpointImpl endpoint, Supplier<String> hostNameResolver, boolean isHttps) {
            if (endpoints.add(endpoint)) {
                /** For notifications: make sure we notify hosts only once.. */
                HashSet<VirtualHostConfig> hosts = new HashSet<VirtualHostConfig>();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "addEndpoint: " + this);
                }

                if (currentHosts.otherHosts != null) {
                    // Look at non-wildcard hosts: check for aliases that are reachable by this new listener..
                    for (Map.Entry<String, VirtualHostConfig> entry : currentHosts.otherHosts.entrySet()) {
                        String host = entry.getKey();
                        VirtualHostConfig config = entry.getValue();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "addEndpoint: update existing host", host, config);
                        }

                        // Notify the listener _with the hostAlias's host value_
                        if (hosts.add(config) && config.acceptFromEndpoint(endpoint.getPid())) {
                            config.listenerStarted(endpoint, () -> host, port, isHttps);
                        }
                    }
                }

                // Notify the wildcardHost (which might be the default host)
                // using the endpoint's resolved host name
                VirtualHostConfig wildcard = currentHosts.wildcardHost;
                if (wildcard != null) {
                    wildcard.listenerStarted(endpoint, hostNameResolver, port, isHttps);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Endpoint {0} added for port {1}: {2}", endpoint.getName(), portString, this);
            }
        }

        /**
         * Synchronized caller...
         * Notify virtual hosts that an endpoint has stopped listening on this port..
         *
         * @param endpoint
         * @param resolvedHostName Hostname suitable for use in a message (a real hostname, not *)
         * @param isHttps
         */
        void removeEndpoint(HttpEndpointImpl endpoint, String resolvedHostName, boolean isHttps) {
            if (endpoints.remove(endpoint)) {
                /** For notifications: make sure we notify hosts only once.. */
                HashSet<VirtualHostConfig> hosts = new HashSet<VirtualHostConfig>();

                if (currentHosts.otherHosts != null) {
                    for (Map.Entry<String, VirtualHostConfig> entry : currentHosts.otherHosts.entrySet()) {
                        String host = entry.getKey();
                        VirtualHostConfig config = entry.getValue();
                        // Notify the listener _with the hostAlias's host value_
                        if (hosts.add(config) && config.acceptFromEndpoint(endpoint.getPid())) {
                            config.listenerStopped(endpoint, host, port, isHttps);
                        }
                    }
                }

                // Notify the wildcardHost (which might be the default host)
                VirtualHostConfig wildcard = currentHosts.wildcardHost;
                if (wildcard != null) {
                    wildcard.listenerStopped(endpoint, resolvedHostName, port, isHttps);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Endpoint {0} removed for port {1}: {2}", endpoint.getName(), portString, this);
            }
        }

        /**
         * Synchronized caller...
         *
         * @return true if this element should be cleaned up.
         */
        boolean cleanup() {
            if ((currentHosts.wildcardHost == null || currentHosts.wildcardHost == defaultHost)
                && currentHosts.otherHosts == null && endpoints.isEmpty()) {
                return true;
            }

            return false;
        }

        /**
         * This is called asynchronously when inbound requests are processed. It tries to
         * identify which virtual host should receive the request based on the hostname used
         * to contact the server.
         * <ol>
         * <li> check for the specific hostname: e.g. somehost.whatever:80
         * <li> check for a wildcard: e.g. *:80
         * <li> use default_host
         * <ol>
         *
         * Most of the time, there will be no virtual hosts configured, in which case,
         * all discriminators would use the same emptyInstance tuple, which would skip
         * host checking so the default_host is returned.
         *
         * @param hostName The hostname used by the client to connect to the server
         * @return The virtual host that should handle the request. May be null during shutdown.
         */
        VirtualHostConfig selectVirtualHost(String hostName) {
            // we are not synchronized here, and don't want to be.
            // store the current hostList as local var..
            HostTuple tuple = currentHosts;

            // Streamline for no virtual hosts defined. Simple check..
            if (tuple.hasMoreHosts) {
                // Check specific hosts first
                if (tuple.otherHosts != null) {
                    VirtualHostConfig config = tuple.otherHosts.get(hostName);
                    if (config != null)
                        return config;
                }

                // Check wildcard host next
                if (tuple.wildcardHost != null) {
                    return tuple.wildcardHost;
                }
            }

            // no dice. Return the default host if we have one.
            return defaultHost;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("VirtualHostDiscriminator[");
            sb.append("port=").append(port);
            sb.append(",hosts=").append(currentHosts);
            sb.append(",endpoints=").append(endpoints);
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     *
     */
    protected synchronized static void dump(PrintStream ps) {
        ps.println("Default host: " + defaultHost);
        ps.println("Alternate hosts: " + (alternateHostSelector != null));
        if (alternateHostSelector != null)
            ps.println(alternateHostSelector);
    }

}
