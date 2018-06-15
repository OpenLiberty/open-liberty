/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.http.DefaultMimeTypes;
import com.ibm.wsspi.http.HttpContainer;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.http.VirtualHostListener;
import com.ibm.wsspi.http.ee7.HttpInboundConnectionExtended;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Representation of VirtualHost configuration, for example:
 * <pre>
 * &lt;virtualHost id="myHost">
 * &lt;hostAlias>*:9080&lt;/hostAlias>
 * &lt;/virtualHost>
 * </pre>
 * <p>
 * This class coordinates with HttpContainers that register context
 * roots, and notifies VirtualHostListeners when context roots are
 * available, meaning both a) a context root is registered, and
 * b) there is at least one open port able to serve it.
 * <p>
 * Most instances of this class will have a collection of host
 * aliases specified in configuration (as shown above). The default_host
 * instance, however, is defined to handle traffic coming in on all
 * available ports, unless another virtual host declares the wildcard
 * (as shown above).
 * <p>
 * The VirtualHostMap bridges VirtualHosts with HttpEndpoints
 * and simplifies runtime virtual host selection.
 * 
 * @see VirtualHostMap
 */
@Component(configurationPid = "com.ibm.ws.http.virtualhost",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           service = { VirtualHostImpl.class },
           property = { "service.vendor=IBM" })
public class VirtualHostImpl implements VirtualHost {
    static final TraceComponent tc = Tr.register(VirtualHostImpl.class);

    String name;

    private volatile boolean activated = false;

    private volatile VirtualHostConfig config = new VirtualHostConfig(this);

    /**
     * Count of listening/active ports. Used for notification that
     * applications are ready.
     */
    private final AtomicInteger listeningPorts = new AtomicInteger(0);

    /** Map of endpoints */
    final ConcurrentHashMap<HttpEndpointImpl, EndpointState> myEndpoints;

    /**
     * Track the containers, rather than mapping a context to each container.
     * Odds are we'll only have one or two containers: each
     * container will already be mapping their own contexts. Those containers
     * will have to handle nested contexts.
     */
    private final CopyOnWriteArraySet<HttpContainerContext> httpContainers;

    /** Lock used when adding/removing contexts and containers. */
    private final Object containerLock = new Object() {};

    /** Required reference to mime types */
    private volatile DefaultMimeTypes defaultMimeTypes = null;

    /**
     * Collection of listeners: these listeners will be notified when a context
     * is added and we have at least one listening port, OR when a context is removed,
     * OR the listening port stops.
     */
    private final ConcurrentServiceReferenceSet<VirtualHostListener> _listeners = new ConcurrentServiceReferenceSet<VirtualHostListener>("listener");

    /** BundleContext: used to manage registration of secondary interface into the service registry */
    private RegistrationHolder osgiService = null;

    public VirtualHostImpl() {
        httpContainers = new CopyOnWriteArraySet<HttpContainerContext>();
        myEndpoints = new ConcurrentHashMap<HttpEndpointImpl, EndpointState>();
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating VirtualHost", properties);
        }
        activated = true;
        osgiService = new RegistrationHolder(context.getBundleContext(), this);
        _listeners.activate(context);

        name = (String) properties.get("id");

        // process initial configuration, including hostAliases
        modified(properties);
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating, reason=" + reason);
        }

        osgiService.clearRegistration();

        // Remove the virtual host from the map 
        // (this will trigger callbacks to listenerStopped for HttpContainer notification)
        VirtualHostMap.removeVirtualHost(config);

        activated = false;

        // clear all context roots
        httpContainers.clear();
        _listeners.deactivate(context);
    }

    @Modified
    protected void modified(Map<String, Object> properties) {

        VirtualHostConfig oldConfig = config;
        VirtualHostConfig newConfig = new VirtualHostConfig(this, properties);

        // If the default/catch-all-ness was modified, we need to remove
        // it from the virtual host map first
        if (newConfig.isDefaultHost() != oldConfig.isDefaultHost()) {
            VirtualHostMap.removeVirtualHost(oldConfig);
        }

        if (osgiService.isRegistered() && !newConfig.enabled) {
            // Remove this virtual host and its previous aliases from the map
            VirtualHostMap.removeVirtualHost(oldConfig);

            // remove the virtual host from the service registry (if registered)
            osgiService.clearRegistration();
        } else if (newConfig.enabled) {
            // Add this virtual host (and any new/changed aliases) to the map of virtual hosts
            // If newDefaultHost=true, new/old aliases are ignored.
            // update registration will be driven by listenerStarted..
            VirtualHostMap.addVirtualHost(oldConfig, newConfig);

            if (newConfig.isDefaultHost)
                newConfig.regenerateAliases();

            // If there were no existing listeners that would have triggered a callback to force
            // service registration, do it now.
            osgiService.updateRegistration(true, newConfig, false);
        }

        // Set the new configuration in place.
        config = newConfig;

        if (!config.getAllowedEndpoints().isEmpty()) {
            Iterator<HttpEndpointImpl> iter = myEndpoints.keySet().iterator();
            while (iter.hasNext()) {
                HttpEndpointImpl ep = iter.next();
                if (!config.getAllowedEndpoints().contains(ep.getPid()))
                    iter.remove();
            }
        }
    }

    /**
     * @return
     */
    public VirtualHostConfig getActiveConfig() {
        return config;
    }

    @Override
    public String getName() {
        return name;
    }

    /** Set required reference to default mime types */
    @Reference(name = "mimeTypeDefaults")
    protected void setMimeTypeDefaults(DefaultMimeTypes mimeTypes) {
        defaultMimeTypes = mimeTypes;
    }

    /** No-op: this is a required reference */
    protected void unsetMimeTypeDefaults(DefaultMimeTypes mimeTypes) {}

    @Override
    public String getMimeType(String extension) {
        return defaultMimeTypes.getType(extension);
    }

    /**
     * Given a new shiny inbound connection, figure out which HttpContainer
     * will handle the inbound request, and return a Runnable that should
     * be used to dispatch the work.
     * 
     * @param inboundConnection
     * @return the Runnable that should be queued for execution to handle the work,
     *         or null if it is an unknown/unmatched context root
     */
    public Runnable discriminate(HttpInboundConnectionExtended inboundConnection) {
        String requestUri = inboundConnection.getRequest().getURI();

        Runnable requestHandler = null;

        // Find the container that can handle this URI. 
        // The first to return a non-null wins
        for (HttpContainerContext ctx : httpContainers) {
            requestHandler = ctx.container.createRunnableHandler(inboundConnection);
            if (requestHandler != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    if (!requestUri.endsWith("/")) {
                        // Strip the query string and append a / to help the best match
                        int pos = requestUri.lastIndexOf('?');
                        if (pos >= 0) {
                            requestUri = requestUri.substring(0, pos);
                        }
                        requestUri += "/";
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "Discriminate " + requestUri, ctx.container);
                    }
                }
                break;
            }
        }

        return requestHandler;
    }

    @Override
    public void addContextRoot(String contextRoot, HttpContainer container) {
        if (contextRoot != null && container != null) {
            synchronized (containerLock) {
                HttpContainerContext found = null;
                for (HttpContainerContext ctx : httpContainers) {
                    if (ctx.sameContainer(container)) {
                        found = ctx;
                        break;
                    }
                }
                if (found == null) {
                    found = new HttpContainerContext(this, container);
                    httpContainers.add(found);
                }
                found.addContextRoot(contextRoot);
            }
        }
    }

    @Override
    public void removeContextRoot(String contextRoot, HttpContainer container) {
        if (httpContainers.isEmpty()) {
            return; // nothing to remove
        }

        synchronized (containerLock) {
            for (HttpContainerContext ctx : httpContainers) {
                if (ctx.sameContainer(container)) {
                    ctx.removeContextRoot(contextRoot);
                    if (ctx.isEmpty()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(this, tc, "Container removed ", container);
                        }
                        httpContainers.remove(ctx);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public String getUrlString(String contextRoot, boolean securedPreferred) {
        // Construct a URL for use in the message. This is not particularly
        // scientific. A virtual host can have more than one host/port defined--
        // we (currently) have no way of knowing if the context root requires security
        // or not, as we are very far away from the application data.
        if (!myEndpoints.isEmpty()) {
            EndpointState httpEndpoint = null;
            EndpointState httpsEndpoint = null;

            for (EndpointState state : myEndpoints.values()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getUrlString -- " + state);
                }
                if (state.httpPort > 0 && state.httpsPort > 0) {
                    // this port has both, which is ideal, as we can listen to preferred and 
                    // support redirects.. 
                    if (securedPreferred) {
                        return getUrlString(true, state.hostName, state.httpsPort, contextRoot);
                    } else {
                        return getUrlString(false, state.hostName, state.httpPort, contextRoot);
                    }
                } else if (httpEndpoint == null && state.httpPort > 0) {
                    httpEndpoint = state;
                } else if (httpsEndpoint == null && state.httpsPort > 0) {
                    httpsEndpoint = state;
                }
            }

            // If a secure port is preferred, and we have one, return that.
            if (securedPreferred && httpsEndpoint != null)
                return getUrlString(true, httpsEndpoint.hostName, httpsEndpoint.httpsPort, contextRoot);

            // otherwise return the non-secure port (unless there is no non-secure port)
            if (httpEndpoint != null)
                return getUrlString(false, httpEndpoint.hostName, httpEndpoint.httpPort, contextRoot);

            // cover virtual hosts that ONLY have https associated with them.. . 
            if (httpsEndpoint != null)
                return getUrlString(true, httpsEndpoint.hostName, httpsEndpoint.httpsPort, contextRoot);
        }

        return "";
    }

    private String getUrlString(boolean isHttps, String host, int port, String contextRoot) {
        final String HTTP = "http://";
        final String HTTPS = "https://";
        String protocol = isHttps ? HTTPS : HTTP;

        if (host.startsWith("[") && host.contains("%")) {
            host = host.replace("%", "%25");
        }
        return protocol + host + ":" + port + contextRoot;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(NumberFormatException.class)
    public int getHttpPort(String hostAlias) {
        int pos = hostAlias.lastIndexOf(':');
        if (pos > -1 && pos < hostAlias.length()) {
            try {
                int port = Integer.valueOf(hostAlias.substring(pos + 1));
                for (EndpointState state : myEndpoints.values()) {
                    if (state.httpPort == port || state.httpsPort == port) {
                        return state.httpPort;
                    }
                }
            } catch (NumberFormatException nfe) {
            }
        }
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(NumberFormatException.class)
    public int getSecureHttpPort(String hostAlias) {
        int pos = hostAlias.lastIndexOf(':');
        if (pos > -1 && pos < hostAlias.length()) {
            try {
                int port = Integer.valueOf(hostAlias.substring(pos + 1));
                for (EndpointState state : myEndpoints.values()) {
                    if (state.httpPort == port || state.httpsPort == port) {
                        return state.httpsPort;
                    }
                }
                // we didn't find a match in httpEndpoints; lets look in httpProxyRedirects
                String host = hostAlias.substring(0, pos);
                Integer httpsPort = HttpProxyRedirect.getRedirectPort(host, port);
                if (httpsPort != null) {
                    return httpsPort;
                }
            } catch (NumberFormatException nfe) {
            }
        }
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(NumberFormatException.class)
    public String getHostName(String hostAlias) {
        int pos = hostAlias.lastIndexOf(':');
        if (pos > -1 && pos < hostAlias.length()) {
            String hostName = hostAlias.substring(0, pos);
            if (hostName.equals("*")) {
                try {
                    int port = Integer.valueOf(hostAlias.substring(pos + 1));
                    for (EndpointState state : myEndpoints.values()) {
                        if (state.httpPort == port || state.httpsPort == port) {
                            return state.hostName;
                        }
                    }
                } catch (NumberFormatException nfe) {
                }
            }
            return hostName;
        }
        return hostAlias;
    }

    /**
     * DS method: add reference for VirtualHostListener
     * 
     * @param reference to add/set
     */
    @Reference(name = "listener", service = VirtualHostListener.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setListener(ServiceReference<VirtualHostListener> reference) {
        _listeners.addReference(reference);
        notifyListener(_listeners.getService(reference));
    }

    /**
     * @param virtualHostListener
     */
    private void notifyListener(VirtualHostListener virtualHostListener) {
        if (activated) {
            for (HttpContainerContext hcc : httpContainers) {
                hcc.notifyContextRoots(virtualHostListener);
            }
        }
    }

    /**
     * DS method: remove reference for VirtualHostListener
     * 
     * @param reference to remove/unset
     */
    protected void unsetListener(ServiceReference<VirtualHostListener> reference) {
        _listeners.removeReference(reference);
    }

    /**
     * Called by the virtual host configuration object in the webcontainer to determine
     * the active/known aliases
     */
    @Override
    public List<String> getAliases() {
        return HostAlias.toStringList(config.getHostAliases());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.http.VirtualHost#getAllowedFromEndpoints()
     */
    @Override
    public Collection<String> getAllowedFromEndpoints() {
        return config.getAllowedEndpoints();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + "[name=" + name
               + ",active=" + activated
               + ",endpoints=" + (myEndpoints == null ? "null" : myEndpoints.size())
               + ",haveCtx=" + (httpContainers == null ? "null" : !httpContainers.isEmpty())
               + ",config=" + config
               + "]";

        // final fields can be empty when debug is on: "this" gets traced during initializtion of nested classes.
    }

    /**
     * This method is called by the VirtualHostMap when an endpoint that applies to
     * this virtual host (matches one of the configured aliases) is started.
     * <p>
     * Note that because host aliases are specified with the virtual host and not
     * with the endpoint that it is possible to split the http and https ports defined
     * with an endpoint across two virtual hosts, which would prevent redirecting.
     * Make sure that the endpoint is used only as an index, and that the parameters
     * passed to this and {@link #listenerStopped(HttpEndpointImpl, String, int, boolean)} are
     * what is used to determine whether or not a redirect is available.
     * <p>
     * Note also that this method can be called as a side-effect
     * of {@link VirtualHostMap#addVirtualHost(VirtualHostConfig, VirtualHostConfig)}. That is
     * why a "target configuration" is passed in as a parameter to this method: the aliases associated
     * with the configuration are being updated before the modified method completes.
     * 
     * @param endpoint The endpoint that was started
     * @param targetConfig The configuration that is being notified
     * @param resolvedHostName A suitable hostname for use in messages
     * @param port The port that is now listening
     * @param isHttps True if this is an https port
     * @see #listenerStopped(HttpEndpointImpl, String, int, boolean)
     */
    synchronized void listenerStarted(HttpEndpointImpl endpoint, VirtualHostConfig targetConfig, String resolvedHostName, int port, boolean isHttps) {
        if (!activated)
            return;

        // If allowed endpoints are specified for the host and this isn't one of them, don't add this endpoint
        Collection<String> allowedEndpoints = targetConfig.getAllowedEndpoints();
        if (!allowedEndpoints.isEmpty() && !allowedEndpoints.contains(endpoint.getPid()))
            return;

        EndpointState oldState = myEndpoints.get(endpoint);
        if (oldState == null) {
            oldState = EndpointState.notStarted;
        }

        int newHttpPort = isHttps ? oldState.httpPort : port;
        int newHttpsPort = isHttps ? port : oldState.httpsPort;

        EndpointState newState = new EndpointState(resolvedHostName, newHttpPort, newHttpsPort);

        //Check if we are changing an already listening port.. 
        boolean updatedPort = (oldState.httpPort > 0 && oldState.httpPort != newHttpPort)
                              || (oldState.httpsPort > 0 && oldState.httpsPort != newHttpsPort);

        boolean addedPort = (oldState.httpPort == 0 && oldState.httpPort != newHttpPort)
                            || (oldState.httpsPort == 0 && oldState.httpsPort != newHttpsPort);

        myEndpoints.put(endpoint, newState);

        int numPorts;
        if (addedPort)
            numPorts = listeningPorts.incrementAndGet();
        else
            numPorts = listeningPorts.get();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "listener started: " + listeningPorts.get(), oldState, newState, addedPort, updatedPort);
        }

        if (addedPort || updatedPort) {
            // Update registration if a port changed.. 
            osgiService.updateRegistration(activated, targetConfig, true);
        }

        // Notify that the endpoint is available only if it is a first port, or a changed port for an endpoint  
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "listener started for " + this + " on host " + resolvedHostName + " on port " + port, endpoint);
        }

        for (HttpContainerContext ctx : httpContainers) {
            ctx.notifyExistingContexts(true, resolvedHostName, port, isHttps, numPorts);
        }
    }

    /**
     * This method is called by the VirtualHostMap when an endpoint that applies to
     * this virtual host (matches one of the configured aliases) has stopped.
     * 
     * @param endpoint The endpoint that was stopped
     * @param resolvedHostName A suitable hostname for use in messages
     * @param port The port that has stopped listening
     * @param isHttps True if this is an https port
     * 
     * @see #listenerStarted(HttpEndpointImpl, String, int, boolean)
     */
    synchronized void listenerStopped(HttpEndpointImpl endpoint, VirtualHostConfig targetConfig, String resolvedHostName, int port, boolean isHttps) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "listener stopped for " + this + " on port " + port, endpoint);
        }

        if (!activated)
            return;

        EndpointState oldState = myEndpoints.get(endpoint);
        if (oldState == null) {
            // we must have been here before.. 
            return;
        }

        int newHttpPort = isHttps ? oldState.httpPort : EndpointState.notStarted.httpPort;
        int newHttpsPort = isHttps ? EndpointState.notStarted.httpsPort : oldState.httpsPort;

        // Check if we actually removed the port 
        boolean removedPort = (oldState.httpPort > 0 && newHttpPort == EndpointState.notStarted.httpPort)
                              || (oldState.httpsPort > 0 && newHttpsPort == EndpointState.notStarted.httpsPort);

        if (newHttpPort == EndpointState.notStarted.httpPort && newHttpsPort == EndpointState.notStarted.httpsPort) {
            // remove the endpoint entirely (see test above..)
            myEndpoints.remove(endpoint);
        } else {
            EndpointState newState = new EndpointState(resolvedHostName, newHttpPort, newHttpsPort);
            myEndpoints.put(endpoint, newState);
        }

        int numPorts;

        if (removedPort)
            numPorts = listeningPorts.decrementAndGet();
        else
            numPorts = listeningPorts.get();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "listener stopped: " + listeningPorts.get(), oldState, myEndpoints.get(endpoint));
        }

        // notify all context roots (test for URL change.. )
        // the notification methods below use a) how many ports are left in combination with
        // b) what host:port they used for the initial notification to determine whether or not
        // to issue additional messages (context removed vs. context moved)
        for (HttpContainerContext ctx : httpContainers) {
            ctx.notifyExistingContexts(false, resolvedHostName, port, isHttps, numPorts);
        }

        osgiService.updateRegistration(false, targetConfig, true);
    }

    /**
     * This is a holder of registered HttpContainers and the context roots that they register.
     * As context roots are added and removed, VirtualHostListeners may get notified
     * if there is an active listening port.
     */
    static final class HttpContainerContext {
        final VirtualHostImpl owner;
        final HttpContainer container;
        final Map<String, String> contextRoots = new HashMap<String, String>();

        @Trivial
        HttpContainerContext(VirtualHostImpl o, HttpContainer c) {
            owner = o;
            container = c;
        }

        /**
         * @return
         */
        public boolean isEmpty() {
            return contextRoots.isEmpty();
        }

        public boolean sameContainer(HttpContainer c) {
            return container == c;
        }

        public synchronized boolean addContextRoot(String contextRoot) {
            if (!contextRoots.containsKey(contextRoot)) {
                int numPorts = owner.listeningPorts.get();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Context root added " + contextRoot, container, numPorts);
                }

                // only notify this kind of addition if ports are listening
                if (contextRoot != null) {
                    String urlString = owner.getUrlString(contextRoot, false);
                    notifyContextRoot(true, urlString, contextRoot);

                    // Add the context root to the map w/ the notification URL
                    contextRoots.put(contextRoot, urlString);
                } else {
                    // Add the context root to the map w/ an empty string to be replaced when endpoint arrives.
                    contextRoots.put(contextRoot, "");
                }
                return true;
            }
            return false;
        }

        public synchronized boolean removeContextRoot(String contextRoot) {
            // Remove the context root from the map
            String urlString = contextRoots.remove(contextRoot);

            // Only attempt to notify for removal if a notification went out for addition.. 
            if (urlString != null && !urlString.isEmpty()) {
                int numPorts = owner.listeningPorts.get();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Context root removed " + contextRoot, container, numPorts);
                }

                // only notify this kind of removal if ports are listening
                if (contextRoot != null) {
                    notifyContextRoot(false, urlString, contextRoot);
                }
                return true;
            }
            return false;
        }

        /**
         * Notify existing contexts that a port has been started or stopped.
         * 
         * @param added True if the port was added/started/updated
         * @param hostName HostName suitable for use in messages
         * @param port Started/stopped port
         * @param isHttps True if the port is associated with an https chain
         * 
         * @see VirtualHostImpl#listenerStarted(HttpEndpointImpl, String, int, boolean)
         * @see VirtualHostImpl#listenerStopped(HttpEndpointImpl, String, int, boolean)
         */
        public synchronized void notifyExistingContexts(boolean added, String hostName, int port, boolean isHttps, int numPorts) {
            for (Entry<String, String> entry : contextRoots.entrySet()) {
                String contextRoot = entry.getKey();

                String oldUrl = entry.getValue();
                String changedUrl = owner.getUrlString(isHttps, hostName, port, contextRoot);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "evaluate URL, changed=" + changedUrl, "old=" + oldUrl, numPorts, added);

                if (added) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "** notify URL addition.. " + changedUrl);

                    // Only notify on addition if the oldUrl is empty (otherwise we've notified before)
                    if (oldUrl.isEmpty()) {
                        notifyContextRoot(added, changedUrl, contextRoot);
                        entry.setValue(changedUrl);
                    }
                } else if (numPorts > 0) {
                    // a port was removed from this virtual host. If it was the port we used for notification, 
                    // issue a message for display in the console.log / WDT console that the URL has changed
                    if (changedUrl.equals(oldUrl)) {
                        String newUrl = owner.getUrlString(contextRoot, isHttps);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "** notify URL change.. " + newUrl, changedUrl);

                        entry.setValue(newUrl);

                        // Don't bother with this notifications if the framework is shutting down.
                        if (FrameworkState.isValid()) {
                            int last = newUrl.length() - 1;
                            if (newUrl.charAt(last) == '*')
                                newUrl = newUrl.substring(0, last);

                            Tr.audit(tc, "context.root.changed", owner.name, newUrl);
                        }
                    }
                } else {
                    // we've removed a port from this virtual host, and it was the last one.
                    // notify that the context root is unreachable.. 
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "** notify URL removal.. " + changedUrl, oldUrl);

                    if (!oldUrl.isEmpty()) {
                        // we removed the last listener, down we go.. use the URL we notified with.
                        notifyContextRoot(added, oldUrl, contextRoot);

                        // clear the URL used to notify so we don't notify again.
                        entry.setValue("");
                    }
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;

            if (o != null
                && o.getClass() == this.getClass()
                && ((HttpContainerContext) o).container == this.container) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return container.hashCode();
        }

        /**
         * While the owner's _listeners is a concurrent structure, this method is not
         * thread safe: To prevent add/remove notifications from interleaving, this method
         * should only be called from synchronized methods on the inner class.
         * 
         * @param added True if this is a new/added context root, false if the context root is being removed
         * @param urlString URL to use for availabilty message
         * @param contextRoot context root for VirtualHostListener notification
         */
        private void notifyContextRoot(boolean added, String urlString, String contextRoot) {
            if (urlString != null && !urlString.isEmpty()) {
                int last = urlString.length() - 1;
                if (urlString.charAt(last) == '*')
                    urlString = urlString.substring(0, last);
                if (added) {
                    Tr.audit(tc, "context.root.added", owner.name, urlString);
                } else {
                    Tr.audit(tc, "context.root.removed", owner.name, urlString);
                }
            }
            for (VirtualHostListener l : owner._listeners.services()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Notifying listener: context root " + (added ? "added" : "removed"), l, contextRoot);
                }
                if (added) {
                    l.contextRootAdded(contextRoot, owner);
                } else {
                    l.contextRootRemoved(contextRoot, owner);
                }
            }
        }

        /**
         * @param virtualHostListener
         */
        public synchronized void notifyContextRoots(VirtualHostListener l) {
            for (String root : contextRoots.keySet()) {
                if (root != null) {
                    l.contextRootAdded(root, owner);
                }
            }
        }

    }

    static final class EndpointState {
        static final EndpointState notStarted = new EndpointState("locahost", 0, 0);
        final String hostName;
        final int httpPort;
        final int httpsPort;

        @Trivial
        EndpointState(String host, int httpPort, int httpsPort) {
            this.hostName = host;
            this.httpPort = httpPort;
            this.httpsPort = httpsPort;
        }

        @Override
        public String toString() {
            return "ES[host=" + hostName + ",http=" + httpPort + ",https=" + httpsPort + "]";
        }
    }

    static final class RegistrationHolder {
        /** Registration in the service registry (aliases added to / removed from service properties ) */
        private ServiceRegistration<VirtualHost> vhostRegistration = null;

        private final BundleContext bContext;
        private final VirtualHostImpl vhost;

        @Trivial
        RegistrationHolder(BundleContext bContext, VirtualHostImpl vhost) {
            this.bContext = bContext;
            this.vhost = vhost;
        }

        /**
         * @return
         */
        synchronized boolean isRegistered() {
            return vhostRegistration != null;
        }

        synchronized void clearRegistration() {
            // Remove the virtual host service registration
            if (vhostRegistration != null) {
                vhostRegistration.unregister();
                vhostRegistration = null;
            }
        }

        /**
         * Update (or create) the service registration
         * 
         * @param oldAliases
         * @param newAliases
         * @see VirtualHostImpl#listenerStarted(HttpEndpointImpl, String, int, boolean)
         * @see VirtualHostImpl#listenerStopped(HttpEndpointImpl, String, int, boolean)
         */
        synchronized void updateRegistration(boolean createIfNull, VirtualHostConfig targetConfig, boolean regenerateAlias) {
            if (vhostRegistration == null && !createIfNull)
                return;

            List<String> newAliasList = Collections.emptyList();
            Collection<String> newHttpEndpointReferenceList = Collections.emptyList();
            if (regenerateAlias) {
                newAliasList = HostAlias.toStringList(targetConfig.regenerateAliases());
            } else {
                newAliasList = HostAlias.toStringList(targetConfig.getHostAliases());
            }

            if (targetConfig.allowedEndpointPids != null) {
                newHttpEndpointReferenceList = targetConfig.getAllowedEndpoints();
            }

            Dictionary<String, ?> props = makeProperties(newAliasList, newHttpEndpointReferenceList);

            if (vhostRegistration == null) {
                vhostRegistration = bContext.registerService(VirtualHost.class, vhost, props);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "New registration", vhost, props, newAliasList);
            } else {
                ServiceReference<?> ref = vhostRegistration.getReference();
                String[] oldAliasList = (String[]) ref.getProperty("aliases");
                @SuppressWarnings("unchecked")
                Collection<String> oldEndpointReferenceList = (Collection<String>) ref.getProperty("endpointReferences");
                String[] oldEpStrings = null;
                boolean updateHttpsAlias = false;
                Object oldHttpsAlias = ref.getProperty("httpsAlias");
                Object newHttpsAlias = props.get("httpsAlias");

                if (newHttpsAlias == null) {
                    updateHttpsAlias = oldHttpsAlias != null;
                } else {
                    updateHttpsAlias = !newHttpsAlias.equals(oldHttpsAlias);
                }

                if (oldEndpointReferenceList != null) {
                    oldEpStrings = oldEndpointReferenceList.toArray(new String[oldEndpointReferenceList.size()]);
                }
                if (updateHttpsAlias
                    || changed(oldAliasList, newAliasList)
                    || changed(oldEpStrings, newHttpEndpointReferenceList)) {
                    vhostRegistration.setProperties(props);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Updated registration", vhost, props,
                                 oldAliasList, newAliasList,
                                 oldEndpointReferenceList, newHttpEndpointReferenceList,
                                 oldHttpsAlias, newHttpsAlias);
                    }
                } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No change to existing registration", vhost, vhost.config, newAliasList, newHttpEndpointReferenceList);
                }
            }
        }

        boolean changed(String[] oldStrings, Collection<String> newStrings) {
            if (oldStrings == null || oldStrings.length == 0)
                return (newStrings != null && !newStrings.isEmpty());
            else if (newStrings == null)
                return oldStrings.length > 0;

            // If the list lengths don't match, there is obviously a difference
            if (oldStrings.length != newStrings.size())
                return true;

            // We know the lists are exactly the same size, 
            // If one list doesn't contain all of the other list, we need to update
            // the registration.
            for (String str : oldStrings) {
                if (!newStrings.contains(str))
                    return true;
            }
            // all strings accounted for.
            return false;
        }

        Dictionary<String, ?> makeProperties(List<String> newAliases, Collection<String> newEPReferences) {
            Hashtable<String, Object> map = new Hashtable<String, Object>();
            map.put(Constants.SERVICE_VENDOR, "IBM");
            map.put("id", vhost.name);
            map.put("enabled", true);

            // Only add the aliases attribute if not-empty
            if (newAliases.size() > 0) {
                map.put("aliases", newAliases.toArray(new String[newAliases.size()]));
            }

            // Only add the endpointReferences attribute if not-empty
            if (newEPReferences.size() > 0) {
                map.put("endpointReferences", newEPReferences);
            }

            // Add an SSL port if we have one... 
            for (Entry<HttpEndpointImpl, EndpointState> entry : vhost.myEndpoints.entrySet()) {
                EndpointState state = entry.getValue();
                if (state.httpsPort != 0) {
                    String cfgHost = entry.getKey().getHostName();
                    map.put("httpsAlias", cfgHost + ":" + state.httpsPort);
                    break;
                }
            }
            return map;
        }
    }

}
