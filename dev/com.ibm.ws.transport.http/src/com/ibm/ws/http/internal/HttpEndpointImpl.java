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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.EventAdmin;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.internal.HttpChain.ChainState;
import com.ibm.ws.http.logging.internal.AccessLogger;
import com.ibm.ws.http.logging.internal.DisabledLogger;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyChain;
import com.ibm.ws.kernel.launch.service.PauseableComponent;
import com.ibm.ws.kernel.launch.service.PauseableComponentException;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.channelfw.ChannelConfiguration;
import com.ibm.wsspi.channelfw.utils.HostNameUtils;
import com.ibm.wsspi.http.logging.AccessLog;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.impl.NettyConstants;

@Component(configurationPid = "com.ibm.ws.http",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           service = { HttpEndpointImpl.class, RuntimeUpdateListener.class, PauseableComponent.class },
           property = { "service.vendor=IBM" })
public class HttpEndpointImpl implements RuntimeUpdateListener, PauseableComponent {

    public static AtomicReference<AccessLog> getAccessLogger(String name) {
        HttpEndpointImpl h = HttpEndpointList.findEndpoint(name);
        if (h != null)
            return h.accessLogger;
        return null;
    }

    /** Trace service */
    private static final TraceComponent tc = Tr.register(HttpEndpointImpl.class);

    private static final int DEACTIVATED = 1;
    private static final int ENABLED = 2;
    private static final int DISABLED = 4;

    private static String defaultHostName = HttpServiceConstants.LOCALHOST;
    private static String resolvedDefaultHostName = HttpServiceConstants.LOCALHOST;

    /**
     * Both this endpoint and the managed chains use something like a state machine to control/
     * order their operations: these flags ensure any activity queued to other threads does
     * not proceed if the target state changed in the meanwhile.
     */
    private final AtomicInteger endpointState = new AtomicInteger(DISABLED);

    /** Required, static Channel framework reference */
    private CHFWBundle chfw = null;
    /** Required, static Netty framework reference */
    private NettyFramework netty = null;

    /** Required, dynamic tcpOptions: unmodifiable map */
    private volatile ChannelConfiguration tcpOptions = null;
    /** Required, dynamic httpOptions: unmodifiable map */
    private volatile ChannelConfiguration httpOptions = null;
    /** Required, dynamic reference to an executor service to schedule chain operations */
    private final AtomicServiceReference<ExecutorService> executorService = new AtomicServiceReference<ExecutorService>("executorService");

    /** Optional, dynamic reference to an SSL channel factory provider: used to start/stop SSL chains */
    private final AtomicServiceReference<ChannelFactoryProvider> sslFactoryProvider = new AtomicServiceReference<ChannelFactoryProvider>("sslSupport");
    /** Optional, dynamic reference to sslOptions. */
    private final AtomicServiceReference<ChannelConfiguration> sslOptions = new AtomicServiceReference<ChannelConfiguration>("sslOptions");

    /** Required, dynamic Event service reference */
    private final AtomicServiceReference<EventAdmin> eventService = new AtomicServiceReference<EventAdmin>("eventService");

    /** Current endpoint configuration */
    private volatile Map<String, Object> endpointConfig = null;

    /** Current remoteIp configuration */
    private volatile ChannelConfiguration remoteIpConfig = null;

    /** Current compression configuration */
    private volatile ChannelConfiguration compressionConfig = null;

    /** Current samesite configuration */
    private volatile ChannelConfiguration samesiteConfig = null;

    /** Current headers configuration */
    private volatile ChannelConfiguration headersConfig = null;

    private volatile boolean endpointStarted = false;
    private volatile String resolvedHostName = null;
    private volatile String host = HttpServiceConstants.LOCALHOST;
    private volatile int httpPort = -1;
    private volatile int httpsPort = -1;
    private volatile String protocolVersion = null;
    private volatile String topicString = null;
    private volatile String name = null;
    private volatile String pid = null;
    private volatile boolean useNetty = false;

    private BundleContext bundleContext = null;

    private final Supplier<String> resolvedHostNameSupplier = new Supplier<String>() {
        @Override
        public String get() {
            return resolvedHostName;
        }
    };

    /**
     * Used to specify whether to stop the server for problems encountered
     * during install/start of feature bundles
     */
    protected volatile OnError onError = OnError.WARN;

    private HttpChain httpChain;
    private HttpChain httpSecureChain;

    private final AtomicReference<AccessLog> accessLogger = new AtomicReference<AccessLog>(DisabledLogger.getRef());

    private final Object actionLock = new Object() {
    };
    private final LinkedList<Runnable> actionQueue = new LinkedList<Runnable>();
    private Future<?> actionFuture = null;

    Object cid = null;

    private final Runnable actionsRunner = new Runnable() {
        @Override
        @Trivial
        public void run() {
            Runnable r;
            for (;;) {
                synchronized (actionQueue) {
                    r = actionQueue.poll();
                    if (r == null) {
                        actionFuture = null;
                        return;
                    }
                }

                r.run();
            }
        }
    };

    private final Runnable stopAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (actionLock) {
                // Always allow stops.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "EndpointAction: stopping chains " + HttpEndpointImpl.this, httpChain, httpSecureChain);

                httpChain.stop();
                httpSecureChain.stop();
            }
        }
    };

    private final Runnable stopHttpsOnlyAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (actionLock) {
                // Always allow stops.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "EndpointAction: stopping https chain " + HttpEndpointImpl.this, httpSecureChain);

                httpSecureChain.stop();
            }
        }
    };

    private final Runnable updateAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (actionLock) {
                // only try to update the chains if the endpoint is enabled/started and framework is good
                
                if (endpointStarted && endpointState.get() == ENABLED && FrameworkState.isValid()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "EndpointAction: updating chains " + HttpEndpointImpl.this, httpChain, httpSecureChain);

                    String resolvedHost = resolvedHostName;
                    httpChain.update(resolvedHost);
                    httpSecureChain.update(resolvedHost);
                }
            }
        }
    };

    @Activate
    protected void activate(ComponentContext ctx, Map<String, Object> config) {
        cid = config.get(ComponentConstants.COMPONENT_ID);
        name = (String) config.get("id");
        pid = (String) config.get(Constants.SERVICE_PID);

        bundleContext = ctx.getBundleContext();

        if (name == null)
            name = "httpEndpoint-" + cid;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "activate HttpEndpoint " + this);
        }

        HttpEndpointList.registerEndpoint(this);
        endpointStarted = true;

        executorService.activate(ctx);
        sslFactoryProvider.activate(ctx);
        sslOptions.activate(ctx);
        eventService.activate(ctx);

        //useNetty = ProductInfo.getBetaEdition() ? 
        //                MetatypeUtils.parseBoolean(config, NettyConstants.USE_NETTY, config.get(NettyConstants.USE_NETTY), false);

        useNetty = MetatypeUtils.parseBoolean(config, NettyConstants.USE_NETTY, config.get(NettyConstants.USE_NETTY), false);
        if (useNetty) {
            httpChain = new NettyChain(this, false);
            httpSecureChain = new NettyChain(this, true);
            
            ((NettyChain) httpChain).initNettyChain(pid, config, netty);
            ((NettyChain) httpSecureChain).initNettyChain(pid, config, netty);
           
            
        } else {
            httpChain = new HttpChain(this, false);
            httpSecureChain = new HttpChain(this, true);
            
            httpChain.init(name, cid, chfw);
            httpSecureChain.init(name, cid, chfw);
        }
        
        
        modified(config);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "deactivate HttpEndpoint " + this + ", reason=" + reason);
        }

        endpointStarted = false;
        HttpEndpointList.unregisterEndpoint(this);

        // the component is being deactivated.
        endpointState.set(DEACTIVATED);

        // Try to get the activity off of the scr deactivate thread
        // but do not add it to the action queue - schedule independently
        performAction(stopAction, false);

        executorService.deactivate(ctx);
        sslFactoryProvider.deactivate(ctx);
        sslOptions.deactivate(ctx);
        eventService.deactivate(ctx);
    }

    private void registerCheckResolvedHostHook(final Map<String, Object> configAtCheckpoint, String cfgDefaultHost) {
        if (!CheckpointPhase.getPhase().restored()) {
            // This is the checkpoint side; register a hook that will
            // confirm the resolved host name has not changed.
            final String checkpointResolvedHost = resolvedHostName;
            CheckpointPhase.getPhase().addMultiThreadedHook(new CheckpointHook() {
                @Override
                public void restore() {
                    if (configAtCheckpoint == endpointConfig) {
                        // Only verify the restore resolved hostname hasn't changed if the
                        // config hasn't changed since checkpoint time.
                        String restoredResolvedHost = resolveHostName(host, cfgDefaultHost);
                        if (!Objects.equals(checkpointResolvedHost, restoredResolvedHost)) {
                            // The resolved hostname on restore is different, force a modify to take effect
                            modified(configAtCheckpoint);
                        }
                    }
                }
            });
        }
    }

    /**
     * Process new configuration: call updateChains to push out
     * new configuration.
     *
     * @param config
     */
    @Modified
    protected void modified(Map<String, Object> config) {
        boolean endpointEnabled = MetatypeUtils.parseBoolean(HttpServiceConstants.ENPOINT_FPID_ALIAS,
                                                             HttpServiceConstants.ENABLED,
                                                             config.get(HttpServiceConstants.ENABLED),
                                                             true);

        onError = (OnError) config.get(OnErrorUtil.CFG_KEY_ON_ERROR);

        // Find and resolve host names.
        host = ((String) config.get("host")).toLowerCase(Locale.ENGLISH);
        String cfgDefaultHost = ((String) config.get(HttpServiceConstants.DEFAULT_HOSTNAME)).toLowerCase(Locale.ENGLISH);
        resolvedHostName = resolveHostName(host, cfgDefaultHost);

        registerCheckResolvedHostHook(config, cfgDefaultHost);
        

        if (resolvedHostName == null) {
            if (HttpServiceConstants.WILDCARD.equals(host)) {
                // On some platforms, or if the networking stack is disabled:
                // getNetworkInterfaces will not be able to return a useful value.
                // If the wildcard is specified (without a fully qualified value for
                // the defaultHostName), we have to be able to fall back to localhost
                // without throwing an error.
                resolvedHostName = HttpServiceConstants.LOCALHOST;
            } else {
                // If the host name was configured to something other than the wildcard, and
                // that host name value can not be resolved back to a NIC on this machine,
                // issue an error message. The endpoint will not be started until the
                // configuration is corrected (the bind would have failed, anyway..)

                // Z changes to support VIPARANGE and DVIPA. yeah, me neither.
                // endpointEnabled = false;
                // Tr.error(tc, "unresolveableHost", host, name);
                // if (onError == OnError.FAIL) {
                //    shutdownFramework();
                // }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "unresolved hostname right now: " + host + "setting resolvedHostName to this host for VIPA");
                }
                resolvedHostName = host;
            }
        }

        //Find and resolve the protocolVersion if has been defined.
        protocolVersion = (String) config.get("protocolVersion");

        httpPort = MetatypeUtils.parseInteger(HttpServiceConstants.ENPOINT_FPID_ALIAS, "httpPort",
                                              config.get("httpPort"),
                                              -1);

        httpsPort = MetatypeUtils.parseInteger(HttpServiceConstants.ENPOINT_FPID_ALIAS, "httpsPort",
                                               config.get("httpsPort"),
                                               -1);
        

        String id = (String) config.get("id");
        Object cid = config.get("component.id");

        // Notification Topics for chain start/stop
        topicString = HttpServiceConstants.TOPIC_PFX + id + "/" + cid;

        if (httpPort < 0 && httpsPort < 0) {
            endpointEnabled = false;
            Tr.warning(tc, "missingPorts.endpointDisabled", id);
        }

        // Store the configuration
        endpointConfig = config;

        if ((CHFWBundle.isServerCompletelyStarted() != true) && (endpointEnabled == true)) {
            // SplitStartUp. Enabling during startup need this to stay on the same thread,
            // or else the port may listen after the server says it is ready
            processHttpChainWork(endpointEnabled, true);
        } else {
            processHttpChainWork(endpointEnabled, false);
        }
    }

    /**
     * Process HTTP chain work.
     *
     * @param enableEndpoint True to enable the associated HTTP chain. False, to disable it.
     * @param isPause        True if this call is being made for pause endpoint processing.
     */
    public void processHttpChainWork(boolean enableEndpoint, boolean isPause) {
        if (enableEndpoint) {
            // enable the endpoint if it is currently disabled
            // it's ok if the endpoint is stopped, the config update will occur @ next start
            endpointState.compareAndSet(DISABLED, ENABLED);

            if (httpPort >= 0) {
                httpChain.enable();
            }
            if (httpsPort >= 0 && sslFactoryProvider.getService() != null) {
                httpSecureChain.enable();
            }

            if (!isPause) {
                // Use an update action so they pick up the new settings
                performAction(updateAction);
            } else {
                updateAction.run();
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "endpoint disabled: " + (String) endpointConfig.get("id"));
            }

            // The endpoint has been disabled-- stop it now
            endpointState.set(DISABLED);
            if (!isPause) {
                performAction(stopAction);
            } else {
                stopAction.run();
            }
        }
    }

    public String getEventTopic() {
        return topicString;
    }

    OnError onError() {
        return onError;
    }

    /**
     * When an error occurs during startup and the config variable
     * fail.on.error.enabled is true,
     * then this method is used to stop the root bundle thus bringing down the
     * OSGi framework.
     */
    @FFDCIgnore(Exception.class)
    final void shutdownFramework() {
        Tr.audit(tc, "httpChain.error.shutdown", name);

        try {
            Bundle bundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);

            if (bundle != null)
                bundle.stop();
        } catch (Exception e) {
            // do not FFDC this.
            // exceptions during bundle stop occur if framework is already stopping or stopped
        }
    }

    public Map<String, Object> getEndpointOptions() {
        return endpointConfig;
    }

    /**
     * @return the configured hostname, may be '*'
     */
    public String getHostName() {
        return host;
    }

    /**
     * @return a real/resolved hostname
     */
    public String getResolvedHostName() {
        return resolvedHostName;
    }

    public Supplier<String> getResolvedHostNameSupplier() {
        return resolvedHostNameSupplier;
    }

    /**
     * @return active http port, or -1 if the port is unconfigured,
     *         or not yet listening
     */
    public int getListeningHttpPort() {
        return httpChain.getActivePort();
    }

    /**
     * @return active https port, or -1 if the port is unconfigured,
     *         or not yet listening
     */
    public int getListeningSecureHttpPort() {
        return httpSecureChain.getActivePort();
    }

    public String getProtocolVersion() {
        return this.protocolVersion;
    }

    /**
     * Set/store the bound ConfigurationAdmin service.
     * Also ensure that a default endpoint configuration exists.
     *
     * "type=SSLChannel" means this will only
     * match SSL channel factory providers
     *
     * @param ref
     */
    @Reference(name = "sslSupport",
               service = ChannelFactoryProvider.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL,
               target = "(type=SSLChannel)")
    protected void setSslSupport(ServiceReference<ChannelFactoryProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "enable ssl support " + ref.getProperty("type"), this);
        }
        sslFactoryProvider.setReference(ref);
        httpSecureChain.enable();

        if (endpointConfig != null) {
            // If this is post-activate, drive the update action
            performAction(updateAction);
        }
    }

    /**
     * This is an optional/dynamic reference: if this goes away, the CFW will
     * eventually stop the SSL chain (factory will be removed)
     *
     * @param ref ConfigurationAdmin instance to unset
     */
    protected void unsetSslSupport(ServiceReference<ChannelFactoryProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "disable ssl support " + ref.getProperty("type"), this);
        }
        if (sslFactoryProvider.unsetReference(ref)) {
            httpSecureChain.disable();
            // removal of ssl support includes removal of the ssl channel factory
            // the CFW is going to disable this chain once the factory goes away.
        }
    }

    /**
     * The specific sslOptions is selected by a filter set through metatype that matches a specific
     * user-configured option set or falls back to a default.
     *
     * @param service
     */
    @Trivial
    @Reference(name = "sslOptions",
               service = ChannelConfiguration.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSslOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set ssl options " + service.getProperty("id"), this);
        }
        this.sslOptions.setReference(service);
        if (endpointConfig != null) {
            performAction(updateAction);
        }
    }

    @Trivial
    protected void updatedSslOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update ssl options " + service.getProperty("id"), this);
        }
        if (endpointConfig != null) {
            performAction(updateAction);
        }
    }

    @Trivial
    protected void unsetSslOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "unset ssl options " + service.getProperty("id"), this);
        }
        if (this.sslOptions.unsetReference(service))
            performAction(stopHttpsOnlyAction);
    }

    public Map<String, Object> getSslOptions() {
        ChannelConfiguration c = sslOptions.getService();
        return c == null ? null : c.getConfiguration();
    }

    /**
     * The specific tcpOptions is selected by a filter set through metatype that matches a
     * specific user-configured option set or falls back to a default.
     *
     * @param service
     */
    @Trivial
    @Reference(name = "tcpOptions",
               service = ChannelConfiguration.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setTcpOptions(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set tcp options " + config.getProperty("id"), this);
        }
        this.tcpOptions = config;
        if (endpointConfig != null) {
            performAction(updateAction);
        }
    }

    @Trivial
    protected void updatedTcpOptions(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update tcp options " + config.getProperty("id"), this);
        }
        if (endpointConfig != null) {
            performAction(updateAction);
        }
    }

    protected void unsetTcpOptions(ChannelConfiguration config) {
    }

    public Map<String, Object> getTcpOptions() {
        ChannelConfiguration c = tcpOptions;
        return c == null ? null : c.getConfiguration();
    }

    /**
     * The specific httpOptions is selected by a filter set through metatype that matches a specific user-configured option set or falls back to a default.
     *
     * @param service
     */
    @Trivial
    @Reference(name = "httpOptions",
               service = ChannelConfiguration.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setHttpOptions(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set http options " + config.getProperty("id"), this);
        }
        this.httpOptions = config;
        if (endpointConfig != null) {
            performAction(updateAction);
        }
    }

    @Trivial
    protected void updatedHttpOptions(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update http options " + config.getProperty("id"), this);
        }
        if (endpointConfig != null) {
            performAction(updateAction);
        }
    }

    protected void unsetHttpOptions(ChannelConfiguration config) {
    }

    public Map<String, Object> getHttpOptions() {
        ChannelConfiguration c = httpOptions;
        return c == null ? null : c.getConfiguration();
    }

    /**
     * The specific compressionOptions is selected by a filter et through metatype that matches a specific user-configured
     * option set or falls back to a default.
     *
     * @param service
     */
    @Trivial
    @Reference(name = "compression",
               service = ChannelConfiguration.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setCompression(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set compression " + config.getProperty("id"), this);
        }
        this.compressionConfig = config;
        if (compressionConfig != null) {
            performAction(updateAction);
        }
    }

    @Trivial
    protected void updatedCompression(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update auto compression " + config.getProperty("id"), this);
        }

        if (compressionConfig != null) {
            performAction(updateAction);
        }
    }

    protected void unsetCompression(ChannelConfiguration config) {
    }

    public Map<String, Object> getCompressionConfig() {
        ChannelConfiguration c = compressionConfig;
        return c == null ? null : c.getConfiguration();
    }

    /**
     * The specific remoteIpOptions is selected by a filter set through metatype that matches a specific user-configured option set or falls back to a default.
     *
     * @param service
     */
    @Trivial
    @Reference(name = "remoteIp",
               service = ChannelConfiguration.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setRemoteIp(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set remote ip " + config.getProperty("id"), this);
        }
        MSP.log("Remote IP config set called");
        this.remoteIpConfig = config;
        if (remoteIpConfig != null) {
            performAction(updateAction);
        }
    }

    @Trivial
    protected void updatedRemoteIp(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update remote ip " + config.getProperty("id"), this);
        }
        if (remoteIpConfig != null) {
            performAction(updateAction);
        }
    }

    protected void unsetRemoteIp(ChannelConfiguration config) {
    }

    public Map<String, Object> getRemoteIpConfig() {
        ChannelConfiguration c = remoteIpConfig;
        return c == null ? null : c.getConfiguration();
    }

    /**
     * The specific samesite configuration is selected by a filter through metatype that matches a specific user-configured
     * option set or falls back to a default.
     *
     * @param service
     */
    @Trivial
    @Reference(name = "samesite",
               service = ChannelConfiguration.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setSamesite(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set samesite " + config.getProperty("id"), this);
        }
        this.samesiteConfig = config;
        if (samesiteConfig != null) {
            performAction(updateAction);
        }
    }

    @Trivial
    protected void updatedSamesite(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update samesite configuration " + config.getProperty("id"), this);
        }

        if (samesiteConfig != null) {
            performAction(updateAction);
        }
    }

    protected void unsetSamesite(ChannelConfiguration config) {
    }

    public Map<String, Object> getSamesiteConfig() {
        ChannelConfiguration c = samesiteConfig;
        return c == null ? null : c.getConfiguration();
    }

    /**
     * The specific header configuration is selected by a filter through metatype that matches a specific user-configured
     * option set or falls back to a default.
     *
     * @param service
     */
    @Trivial
    @Reference(name = "headers",
               service = ChannelConfiguration.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setHeaders(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set <headers> " + config.getProperty("id"), this);
        }
        this.headersConfig = config;
        if (headersConfig != null) {
            performAction(updateAction);
        }
    }

    @Trivial
    protected void updatedHeaders(ChannelConfiguration config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update <headers> configuration " + config.getProperty("id"), this);
        }

        if (headersConfig != null) {
            performAction(updateAction);
        }
    }

    protected void unsetHeaders(ChannelConfiguration config) {
    }

    public Map<String, Object> getHeadersConfig() {
        ChannelConfiguration c = headersConfig;
        return c == null ? null : c.getConfiguration();
    }

    /**
     * DS method for setting the required channel framework service.
     *
     * @param bundle
     */
    @Reference(name = "chfwBundle")
    protected void setChfwBundle(CHFWBundle bundle) {
        chfw = bundle;
    }

    /**
     * This is a required static reference, this won't
     * be called until the component has been deactivated
     *
     * @param bundle CHFWBundle instance to unset
     */
    protected void unsetChfwBundle(CHFWBundle bundle) {
    }

    protected CHFWBundle getChfwBundle() {
        return chfw;
    }

    @Reference(name = "nettyBundle")
    protected void setNettyBundle(NettyFramework bundle) {
        netty = bundle;
    }

    protected void unsetNettyBundle(NettyFramework bundle) {

    }
    
    protected NettyFramework getNettyBundle() {
        return netty;
    }

    /**
     * DS method for setting the required dynamic executor service reference.
     *
     * @param bundle
     */
    @Reference(name = "executorService", service = ExecutorService.class, policy = ReferencePolicy.DYNAMIC)
    protected void setExecutorService(ServiceReference<ExecutorService> executorService) {
        this.executorService.setReference(executorService);
    }

    /**
     * DS method for clearing the required dynamic event admin reference.
     * This is a required reference, but will be called if the dynamic reference is replaced
     */
    protected void unsetExecutorService(ServiceReference<ExecutorService> executorService) {
        this.executorService.unsetReference(executorService);
    }

    @Trivial
    @Reference(name = "accessLogging", service = AccessLogger.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setAccessLogging(AccessLogger alConfig) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set access log " + alConfig);
        }

        accessLogger.set(alConfig);
    }

    protected void unsetAccessLogging(AccessLogger alConfig) {
        // stop only if we started
        accessLogger.set(DisabledLogger.getRef());
    }

    @Reference(name = "eventService", service = EventAdmin.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY)
    protected void setEventAdmin(ServiceReference<EventAdmin> reference) {
        eventService.setReference(reference);
    }

    protected void unsetEventAdmin(ServiceReference<EventAdmin> reference) {
        eventService.unsetReference(reference);
    }

    public EventAdmin getEventAdmin() {
        return eventService.getService();
    }

    /**
     * DS method for setting the required HttpDispatcher service.
     * This ensures the dispatcher is ready before endpoints are created.
     *
     * @param bundle
     */
    @Reference(name = "httpDispatcher")
    protected void setHttpDispatcher(HttpDispatcher dispatcher) {
    }

    protected void unsetHttpDispatcher(HttpDispatcher dispatcher) {
    }

    /**
     * If we can get the chain activity off the SCR action thread, we should
     *
     * @param action Runnable action to execute
     */
    @Trivial
    private void performAction(Runnable action) {
        performAction(action, true);
    }

    /**
     * Schedule an activity to run off the SCR action thread,
     * if the ExecutorService is available
     *
     * @param action     Runnable action to execute
     * @param addToQueue Set to false if the action should be scheduled independently of the actionQueue
     */
    @Trivial
    private void performAction(Runnable action, boolean addToQueue) {
        ExecutorService exec = executorService.getService();

        if (exec == null) {
            // If we can't find the executor service, we have to run it in place.
            action.run();
        } else {
            // If we can find the executor service, we'll add the action to the queue.
            // If the actionFuture is null (no pending actions) and the configFuture is null (no
            // pending configuration updates), we'll submit the actionsRunner to the executor
            // service to drain the queue
            //
            // configFuture is used to avoid bouncing the endpoint multiple times because of a
            // single configuration update.
            //
            // actionFuture is only set to a non-null value by kicking off the executor service here.
            // actionsRunner syncs on actionQueue, so we can't add any new actions while we are
            // draining the queue. When the queue is empty, actionFuture is explicitly set to null.
            //
            // Long story short, it prevents us from kicking off multiple executors which could run in
            // random order.
            if (addToQueue) {
                synchronized (actionQueue) {
                    actionQueue.add(action);
                    if ((actionFuture == null) && (configFuture == null)) {
                        actionFuture = exec.submit(actionsRunner);
                    }
                }
            } else {
                // Schedule immediately
                exec.submit(action);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + "[@" + System.identityHashCode(this)
               + ",name=" + name
               + ",host=" + host
               + ",http=" + httpPort
               + ",https=" + httpsPort
               + ",state=" + endpointState.get()
               + "]";
    }

    /**
     * @return The name/id of this endpoint
     */
    @Override
    @Trivial
    public String getName() {
        return name;
    }

    /**
     * @return The pid of this endpoint
     */
    @Trivial
    public String getPid() {
        return pid;
    }

    protected String resolveHostName(String cfgHost, String cfgDefaultHost) {
        // In the past, if someone specified the wildcard for their hostname, we would find
        // and use an arbitrary non-loopback address. We now want to fall back to using the
        // value of the defaultHost variable, but.. the default value of that variable
        // is localhost, and suddenly using localhost when * was specified as the listening
        // address would break things. SO.  We're only going to use the defaultHostName
        // if it was explicitly set to something OTHER than localhost.
        // Note: it is wildly unlikely that cfgDefaultHost would be null...
        if (HttpServiceConstants.WILDCARD.equals(cfgHost) &&
            !HttpServiceConstants.LOCALHOST.equals(cfgDefaultHost)) {
            return resolveDefaultHostName(cfgDefaultHost);
        }

        // This might return null... in which case, we should not attempt to start the chains
        // until the host configuration is corrected.
        return HostNameUtils.tryResolveHostName(cfgHost);
    }

    /**
     * Resolve the default hostname (based on the defaultHostName variable).
     * The defaultHostName and its resolved value are both cached for use
     * by multiple endpoints. The defaultHostName value is only consulted
     * if '*' is configured as the host for the endpoint.
     *
     * @param cfgDefaultHost
     * @return
     */
    protected static synchronized String resolveDefaultHostName(String cfgDefaultHost) {
        if (resolvedDefaultHostName == null || !cfgDefaultHost.equals(defaultHostName)) {
            // remember what was configured. This might be replaced by localhost if
            // the specified address was unresolvable.
            defaultHostName = cfgDefaultHost;

            if (HostNameUtils.validLocalHostName(defaultHostName)) {
                resolvedDefaultHostName = defaultHostName;
            } else {
                // If the default host name can not be resolved back to a NIC on this machine,
                // issue a warning message, and use localhost.
                Tr.warning(tc, "unresolveableDefaultHost", defaultHostName);
                resolvedDefaultHostName = HttpServiceConstants.LOCALHOST;
            }
        }
        return resolvedDefaultHostName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.runtime.update.RuntimeUpdateListener#notificationCreated(com.ibm.ws.runtime.update.RuntimeUpdateManager, com.ibm.ws.runtime.update.RuntimeUpdateNotification)
     */
    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {

        // CONFIG_UPDATES_DELIVERED notification actually indicates that a config update is starting. The update
        // won't be complete until the future we get from the notification is complete.
        if (RuntimeUpdateNotification.CONFIG_UPDATES_DELIVERED.equals(notification.getName())) {

            configFuture = notification.getFuture();
            _futureMonitor.onCompletion(notification.getFuture(), new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    // We're done with the update. Set the configFuture to null, and submit a dummy task
                    // so that performAction knows it can kick off the executor.
                    configFuture = null;
                    submit();
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    // The update failed, but we still want to process any queued actions. Set the
                    // configFuture to null, and submit a dummy task so that performAction knows
                    // it can kick off the executor.
                    configFuture = null;
                    submit();
                }

                // Submit a dummy job so that performAction() will kick off the executor for the actions queue
                private void submit() {
                    synchronized (actionQueue) {
                        performAction(new Runnable() {

                            @Override
                            public void run() {
                                return;
                            }
                        });
                    }
                }
            });
        }

    }

    // configFuture is received from a notification event that indicates a config update has started. It's set to
    // null again when the config update ends.
    private Future<Boolean> configFuture = null;

    // futureMonitor is needed to track the outcome of the configFuture
    private volatile FutureMonitor _futureMonitor;

    @Reference(service = FutureMonitor.class)
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        _futureMonitor = futureMonitor;
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonitor) {
        _futureMonitor = null;
    }

    /** {@inheritDoc} */
    @Override
    public void pause() throws PauseableComponentException {
        try {
            // Stop the HTTP and HTTPS chains.
            // Some of the process that stops the chain is asynchronous and will not complete until all active
            // work completes; however, the pause function only requires that new inbound work going through this
            // endpoint be rejected. By the time this method exits, requests that target this
            // endpoint will no longer be accepted (CWWKO0220I: TCP Channel ***(-ssl) has stopped listening for
            // requests on host ****  (IPv6) port ****.).
            processHttpChainWork(false, true);

            // Check the state of the HTTP chains. The expectation is that the HTTP chains' states are NOT STARTED
            // (UNITIALIZED, DESTROYED, QUIESCED or STOPPED).
            if (httpChain.getChainState() == ChainState.STARTED.val || httpSecureChain.getChainState() == ChainState.STARTED.val) {
                throw new PauseableComponentException("The request to pause HTTP endpoint " + name + " did not complete successfully.");
            }
        } catch (Throwable t) {
            throw new PauseableComponentException(t);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void resume() throws PauseableComponentException {
        try {
            // Start the HTTP and HTTPS chains.
            // By the time this method exits, requests that target this endpoint will be accepted (CWWKO0219I:
            // TCP Channel *** has been started and is now listening for requests on host ***  (IPv6) port ***.).
            processHttpChainWork(true, true);

            // Check the state of the HTTP chains. The expectation is that the HTTP and HTTPS chains states are either STARTED
            // or UNINITIALIZED (disabled).
            int httpChainState = httpChain.getChainState();
            int httpsChainState = httpSecureChain.getChainState();
            if (!(httpChainState == ChainState.STARTED.val && httpsChainState == ChainState.UNINITIALIZED.val ||
                  httpChainState == ChainState.UNINITIALIZED.val && httpsChainState == ChainState.STARTED.val ||
                  httpChainState == ChainState.STARTED.val && httpsChainState == ChainState.STARTED.val)) {
                throw new PauseableComponentException("The request to resume HTTP endpoint " + name + " did not complete successfully. HTTPChain: " + httpChain.toString()
                                                      + ". HTTPSChain: " + httpSecureChain.toString());
            }
        } catch (Throwable t) {
            throw new PauseableComponentException(t);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPaused() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "endpoint and chain data: " + HttpEndpointImpl.this, httpChain, httpSecureChain);

        // Return true if any of these states apply: UNITIALIZED, DESTROYED, QUIESCED or STOPPED.
        return (httpChain.getChainState() != ChainState.STARTED.val && httpSecureChain.getChainState() != ChainState.STARTED.val);
    }

    /** {@inheritDoc} */
    @Override
    public HashMap<String, String> getExtendedInfo() {
        LinkedHashMap<String, String> info = new LinkedHashMap<String, String>();
        info.put("host", host);
        info.put("httpPort", String.valueOf(httpPort));
        info.put("httpsPort", String.valueOf(httpsPort));

        return info;
    }
}
