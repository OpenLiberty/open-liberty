/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherConfig;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.InvalidRuntimeStateException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 * Encapsulation of steps for starting/stopping an http chain in a controlled/predictable
 * manner with a minimum of synchronization.
 */
public class HttpChain implements ChainEventListener {
    private static final TraceComponent tc = Tr.register(HttpChain.class);

    enum ChainState {
        UNINITIALIZED(0, "UNINITIALIZED"),
        DESTROYED(1, "DESTROYED"),
        INITIALIZED(2, "INITIALIZED"),
        STOPPED(3, "STOPPED"),
        QUIESCED(4, "QUIESCED"),
        STARTED(5, "STARTED");

        final int val;
        final String name;

        @Trivial
        ChainState(int val, String name) {
            this.val = val;
            this.name = "name";
        }

        @Trivial
        public static final String printState(int state) {
            switch (state) {
                case 0:
                    return "UNINITIALIZED";
                case 1:
                    return "DESTROYED";
                case 2:
                    return "INITIALIZED";
                case 3:
                    return "STOPPED";
                case 4:
                    return "QUIESCED";
                case 5:
                    return "STARTED";
            }
            return "UNKNOWN";
        }
    }

    protected final StopWait stopWait = new StopWait();
    protected final HttpEndpointImpl owner;
    protected final boolean isHttps;

    protected String endpointName;
    protected String tcpName;
    protected String sslName;
    protected String httpName;
    protected String dispatcherName;
    protected String chainName;
    private ChannelFramework cfw;
    protected EndPointMgr endpointMgr;

    /**
     * The state of the chain according to values from {@link ChainState}.
     * Aside from the initial value assignment, new values are only assigned from
     * within {@link ChainEventListener} methods.
     */
    protected final AtomicInteger chainState = new AtomicInteger(ChainState.UNINITIALIZED.val);

    /**
     * Toggled by enable/disable methods. This serves only to block activity
     * of some operations (start/update on disabled chain should no-op).
     */
    private volatile boolean enabled = false;

    /**
     * A snapshot of the configuration (collection of properties objects) last used
     * for a start/update operation.
     */
    protected volatile ActiveConfiguration currentConfig = null;

    /**
     * Create the new chain with it's parent endpoint
     *
     * @param httpEndpointImpl the owning endpoint: used for notifications
     * @param isHttps          true if this is to be an https chain.
     */
    public HttpChain(HttpEndpointImpl owner, boolean isHttps) {
        this.owner = owner;
        this.isHttps = isHttps;
    }

    /**
     * Initialize this chain manager: Channel and chain names shouldn't fluctuate as config changes,
     * so come up with names associated with this set of channels/chains that will be reused regardless
     * of start/stop/enable/disable/modify
     *
     * @param endpointId  The id of the httpEndpoint
     * @param componentId The DS component id
     * @param cfw         Channel framework
     */
    public void init(String endpointId, Object componentId, CHFWBundle cfBundle) {
        final String root = endpointId + (isHttps ? "-ssl" : "");

        cfw = cfBundle.getFramework();
        endpointMgr = cfBundle.getEndpointManager();

        endpointName = root;
        tcpName = root;
        sslName = "SSL-" + root;
        httpName = "HTTP-" + root;
        dispatcherName = "HTTPD-" + root;
        chainName = "CHAIN-" + root;

        // If there is a chain that is in the CFW with this name, it was potentially
        // left over from a previous instance of the endpoint. There is no way to get
        // the state of the existing (old) CFW chain to set our chainState accordingly...
        // (in addition to the old chain pointing to old services and things.. )
        // *IF* there is an old chain, stop, destroy, and remove it.
        try {
            ChainData cd = cfw.getChain(chainName);
            if (cd != null) {
                cfw.stopChain(cd, 0L); // no timeout: FORCE the stop.
                cfw.destroyChain(cd);
                cfw.removeChain(cd);
            }
        } catch (ChannelException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error stopping chain " + chainName, this, e);
            }
        } catch (ChainException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error stopping chain " + chainName, this, e);
            }
        }
    }

    public HttpEndpointImpl getOwner() {
        return this.owner;
    }

    public boolean isHttps() {
        return this.isHttps;
    }

    /**
     * Enable this chain: this happens automatically for the http chain,
     * but is delayed on the ssl chain until ssl support becomes available.
     * This does not change the chain's state. The caller should
     * make subsequent calls to perform actions on the chain.
     */
    public void enable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "enable chain " + this);
        }
        enabled = true;
    }

    /**
     * Disable this chain. This does not change the chain's state. The caller should
     * make subsequent calls to perform actions on the chain.
     */
    public void disable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "disable chain " + this);
        }
        enabled = false;
    }

    /**
     * Stop this chain. The chain will have to be recreated when port is updated
     * notification/follow-on of stop operation is in the chainStopped listener method.
     */
    @FFDCIgnore(InvalidRuntimeStateException.class)
    public synchronized void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "stop chain " + this);
        }

        // When the chain is being stopped, remove the previously
        // registered EndPoint created in update
        endpointMgr.removeEndPoint(endpointName);

        // We don't have to check enabled/disabled here: chains are always allowed to stop.
        if (currentConfig == null || chainState.get() <= ChainState.QUIESCED.val)
            return;

        // Quiesce and then stop the chain. The CFW internally uses a StopTimer for
        // the quiesce/stop operation-- the listener method will be called when the chain
        // has stopped. So to see what happens next, visit chainStopped
        try {
            ChainData cd = cfw.getChain(chainName);
            if (cd != null) {
                cfw.stopChain(cd, cfw.getDefaultChainQuiesceTimeout());
                stopWait.waitForStop(cfw.getDefaultChainQuiesceTimeout(), this); // BLOCK
                try {
                    cfw.destroyChain(cd);
                    cfw.removeChain(cd);
                } catch (InvalidRuntimeStateException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Error destroying or removing chain " + chainName, this, e);
                    }
                }
            }
        } catch (ChannelException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error stopping chain " + chainName, this, e);
            }
        } catch (ChainException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error stopping chain " + chainName, this, e);
            }
        }
    }

    /**
     * Update/start the chain configuration.
     */
    @FFDCIgnore({ ChannelException.class, ChainException.class })
    public synchronized void update(String resolvedHostName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update chain " + this);
        }

        // Don't update or start the chain if it is disabled or the framework is stopping..
        if (!enabled || FrameworkState.isStopping())
            return;

        final ActiveConfiguration oldConfig = currentConfig;

        // The old configuration was "valid" if it existed, and if it was correctly configured
        final boolean validOldConfig = oldConfig == null ? false : oldConfig.validConfiguration;

        Map<String, Object> tcpOptions = owner.getTcpOptions();
        Map<String, Object> sslOptions = (isHttps) ? owner.getSslOptions() : null;
        Map<String, Object> httpOptions = owner.getHttpOptions();
        Map<String, Object> endpointOptions = owner.getEndpointOptions();
        Map<String, Object> remoteIpOptions = owner.getRemoteIpConfig();
        Map<String, Object> compressionOptions = owner.getCompressionConfig();
        Map<String, Object> samesiteOptions = owner.getSamesiteConfig();
        Map<String, Object> headersOptions = owner.getHeadersConfig();

        final ActiveConfiguration newConfig = new ActiveConfiguration(isHttps, tcpOptions, sslOptions, httpOptions, remoteIpOptions, compressionOptions, samesiteOptions, headersOptions, endpointOptions, resolvedHostName);

        if (newConfig.configPort < 0 || !newConfig.complete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Stopping chain due to configuration " + newConfig);
            }

            // save the new/changed configuration before we start setting up the new chain
            currentConfig = newConfig;

            stop();
        } else {
            Map<Object, Object> chanProps;

            try {
                boolean sameConfig = newConfig.unchanged(oldConfig);
                if (validOldConfig) {
                    if (sameConfig) {
                        int state = chainState.get();
                        if (state == ChainState.STARTED.val) {
                            // If configurations are identical, see if the listening port is also the same
                            // which would indicate that the chain is running with the unchanged configuration
                            // toggle start/stop of chain if we are somehow active on a different port..
                            sameConfig = oldConfig.validateActivePort();
                            if (sameConfig) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Configuration is unchanged, and chain is already started: " + oldConfig);
                                }
                                // EARLY EXIT: we have nothing else to do here: "new configuration" not saved
                                return;
                            } else {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(this, tc, "Configuration is unchanged, but chain is running with a mismatched configuration: " + oldConfig);
                                }
                            }
                        } else if (state == ChainState.QUIESCED.val) {
                            // Chain is in the process of stopping.. we need to wait for it
                            // to finish stopping before we start it again
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Configuration is unchanged, chain is quiescing, wait for stop: " + newConfig);
                            }
                            stopWait.waitForStop(cfw.getDefaultChainQuiesceTimeout(), this); // BLOCK
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Configuration is unchanged, chain must be started: " + newConfig);
                            }
                        }
                    }
                }

                if (!sameConfig) {
                    // Note that one path in the above block can change the value of sameConfig:
                    // if the started chain is actually running on a different port than we expect,
                    // something strange happened, and the whole thing should be stopped and restarted.
                    // We come through this block for the stop/teardown...

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "New/changed chain configuration " + newConfig);
                    }

                    // We've been through channel configuration before...
                    // We have to destroy/rebuild the chains because the channels don't
                    // really support dynamic updates. *sigh*
                    ChainData cd = cfw.getChain(chainName);
                    if (cd != null) {
                        cfw.stopChain(cd, cfw.getDefaultChainQuiesceTimeout());
                        stopWait.waitForStop(cfw.getDefaultChainQuiesceTimeout(), this); // BLOCK
                        cfw.destroyChain(cd);
                        cfw.removeChain(cd);
                    }
                    // Remove any channels that have to be rebuilt..
                    if (newConfig.tcpChanged(oldConfig))
                        removeChannel(tcpName);

                    if (newConfig.sslChanged(oldConfig))
                        removeChannel(sslName);

                    if (newConfig.httpChanged(oldConfig))
                        removeChannel(httpName);

                    if (newConfig.endpointChanged(oldConfig))
                        removeChannel(dispatcherName);
                }

                // save the new/changed configuration before we start setting up the new chain
                currentConfig = newConfig;

                // Define and register an EndPoint to represent this chain
                EndPointInfo ep = endpointMgr.defineEndPoint(endpointName, newConfig.configHost, newConfig.configPort);

                // TCP Channel
                ChannelData tcpChannel = cfw.getChannel(tcpName);
                if (tcpChannel == null) {
                    String typeName = (String) tcpOptions.get("type");
                    chanProps = new HashMap<Object, Object>(tcpOptions);
                    chanProps.put("endPointName", endpointName);
                    chanProps.put("hostname", ep.getHost());
                    chanProps.put("port", String.valueOf(ep.getPort()));

                    tcpChannel = cfw.addChannel(tcpName, cfw.lookupFactory(typeName), chanProps);
                }

                // SSL Channel
                if (isHttps) {
                    ChannelData sslChannel = cfw.getChannel(sslName);
                    if (sslChannel == null) {
                        chanProps = new HashMap<Object, Object>(sslOptions);
                        // Put the protocol version, which allows the http channel to dynamically
                        // know what http version it will use.
                        if (owner.getProtocolVersion() != null) {
                            chanProps.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, owner.getProtocolVersion());
                        }
                        sslChannel = cfw.addChannel(sslName, cfw.lookupFactory("SSLChannel"), chanProps);
                    }
                }

                // HTTP Channel
                ChannelData httpChannel = cfw.getChannel(httpName);
                if (httpChannel == null) {
                    chanProps = new HashMap<Object, Object>(httpOptions);
                    // Put the endpoint id, which allows us to find the registered access log
                    // dynamically
                    chanProps.put(HttpConfigConstants.PROPNAME_ACCESSLOG_ID, owner.getName());
                    // Put the protocol version, which allows the http channel to dynamically
                    // know what http version it will use.
                    if (owner.getProtocolVersion() != null) {
                        chanProps.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, owner.getProtocolVersion());
                    }
                    if (remoteIpOptions.get("id").equals("defaultRemoteIp")) {
                        //Put the internal remoteIp set to false since the element was not configured to be used
                        chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_IP, "false");
                        chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_PROXIES, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG, null);
                    } else {
                        chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_IP, "true");
                        //Check if the remoteIp is configured to use the remoteIp in the access log or if
                        //a custom proxy regex was provided
                        if (remoteIpOptions.containsKey("proxies")) {
                            chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_PROXIES, remoteIpOptions.get("proxies"));
                        }
                        if (remoteIpOptions.containsKey("useRemoteIpInAccessLog")) {
                            chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG, remoteIpOptions.get("useRemoteIpInAccessLog"));
                        }
                    }

                    if (compressionOptions.get("id").equals("defaultCompression")) {
                        //Put the internal compression set to false since the element was not configured to be used
                        chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION, "false");
                        chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM, null);
                    }

                    else {
                        chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION, "true");
                        //Check if the compression is configured to use content-type filter
                        if (compressionOptions.containsKey("types")) {
                            chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES, compressionOptions.get("types"));

                        }
                        if (compressionOptions.containsKey("serverPreferredAlgorithm")) {
                            chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM, compressionOptions.get("serverPreferredAlgorithm"));
                        }
                    }

                    if (samesiteOptions.get("id").equals("defaultSameSite")) {
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE, "false");
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_LAX_INTERNAL, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_NONE_INTERNAL, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_STRICT_INTERNAL, null);
                    }

                    else {

                        boolean enableSameSite = false;
                        if (samesiteOptions.containsKey("lax")) {
                            enableSameSite = true;
                            chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_LAX_INTERNAL, samesiteOptions.get("lax"));
                        }
                        if (samesiteOptions.containsKey("none")) {
                            enableSameSite = true;
                            chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_NONE_INTERNAL, samesiteOptions.get("none"));
                        }
                        if (samesiteOptions.containsKey("strict")) {
                            enableSameSite = true;
                            chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_STRICT_INTERNAL, samesiteOptions.get("strict"));
                        }
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE, enableSameSite);
                    }

                    if (headersOptions.get("id").equals("defaultHeaders")) {
                        chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS, "false");
                        chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE, null);
                    }

                    else {
                        boolean enableHeadersFeature = false;
                        if (headersOptions.containsKey("add")) {
                            enableHeadersFeature = true;
                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD, headersOptions.get("add"));
                        }
                        if (headersOptions.containsKey("set")) {
                            enableHeadersFeature = true;
                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET, headersOptions.get("set"));
                        }
                        if (headersOptions.containsKey("setIfMissing")) {
                            enableHeadersFeature = true;
                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING, headersOptions.get("setIfMissing"));
                        }
                        if (headersOptions.containsKey("remove")) {
                            enableHeadersFeature = true;
                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE, headersOptions.get("remove"));
                        }
                        chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS, enableHeadersFeature);
                    }

                    httpChannel = cfw.addChannel(httpName, cfw.lookupFactory("HTTPInboundChannel"), chanProps);
                }

                // HTTPDispatcher Channel
                ChannelData httpDispatcher = cfw.getChannel(dispatcherName);
                if (httpDispatcher == null) {
                    chanProps = new HashMap<Object, Object>();
                    chanProps.put(HttpDispatcherConfig.PROP_ENDPOINT, owner.getPid());

                    httpDispatcher = cfw.addChannel(dispatcherName, cfw.lookupFactory("HTTPDispatcherChannel"), chanProps);
                }

                // Add chain
                ChainData cd = cfw.getChain(chainName);
                if (null == cd) {
                    final String[] chanList;
                    if (isHttps)
                        chanList = new String[] { tcpName, sslName, httpName, dispatcherName };
                    else
                        chanList = new String[] { tcpName, httpName, dispatcherName };

                    cd = cfw.addChain(chainName, FlowType.INBOUND, chanList);
                    cd.setEnabled(enabled);
                    cfw.addChainEventListener(this, chainName);

                    // initialize the chain: this will find/create the channels in the chain,
                    // initialize each channel, and create the chain. If there are issues with any
                    // channel properties, they will surface here
                    // THIS INCLUDES ATTEMPTING TO BIND TO THE PORT
                    cfw.initChain(chainName);
                }

                // We configured the chain successfully
                newConfig.validConfiguration = true;
            } catch (ChannelException e) {
                handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
            } catch (ChainException e) {
                handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
            } catch (Exception e) {
                // The exception stack for this is all internals and does not belong in messages.log.
                Tr.error(tc, "config.httpChain.error", tcpName, e.toString());
                handleStartupError(e, newConfig);
            }

            if (newConfig.validConfiguration) {
                try {
                    // Start the chain: follow along to chainStarted method (CFW callback)
                    cfw.startChain(chainName);
                } catch (ChannelException e) {
                    handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
                } catch (ChainException e) {
                    handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
                } catch (Exception e) {
                    // The exception stack for this is all internals and does not belong in messages.log.
                    Tr.error(tc, "start.httpChain.error", tcpName, e.toString());
                    handleStartupError(e, newConfig);
                }
            }
        }
    }

    @FFDCIgnore({ ChannelException.class, ChainException.class })
    private void removeChannel(String name) {
        // Neither of the thrown exceptions are permanent failures:
        // they usually indicate that we're the victim of a race.
        // If the CFW is also tearing down the chain at the same time
        // (for example, the SSL feature was removed), then this could
        // fail.
        try {
            cfw.removeChannel(name);
        } catch (ChannelException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error removing channel " + name, this, e);
            }
        } catch (ChainException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error removing channel " + name, this, e);
            }
        }
    }

    private void handleStartupError(Exception e, ActiveConfiguration cfg) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Error starting chain " + chainName, this, e);
        }

        if (owner.onError() == OnError.FAIL) {
            // Stop the server if something bad happened starting the chain
            owner.shutdownFramework();
        } else {
            // Post an endpoint failed to start event to anyone listening
            String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_FAILED;
            postEvent(topic, cfg, e);

            // TODO: schedule a task to try again later..
        }
    }

    public int getActivePort() {
        ActiveConfiguration cfg = currentConfig;
        if (cfg != null)
            return cfg.getActivePort();
        return -1;
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainInitialized(ChainData chainData) {
        chainState.set(ChainState.INITIALIZED.val);
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public synchronized void chainStarted(ChainData chainData) {
        chainState.set(ChainState.STARTED.val);

        final ActiveConfiguration cfg = currentConfig;
        final int port = cfg.getActivePort();

        if (port > 0) {
            // HOORAY! we have a bound listener.
            // Notify listeners that the chain was started.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "New configuration started " + cfg);
            }

            VirtualHostMap.notifyStarted(owner, () -> cfg.resolvedHost, port, isHttps);

            // Post an endpoint started event to anyone listening
            String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STARTED;
            postEvent(topic, cfg, null);
        }
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainStopped(ChainData chainData) {
        final ActiveConfiguration cfg = currentConfig;

        int oldState = chainState.getAndSet(ChainState.STOPPED.val);
        if (oldState > ChainState.QUIESCED.val) {
            quiesceChain();
        }

        // Wake up anything waiting for the chain to stop
        // (see the update method for one example)
        stopWait.notifyStopped();

        // Post an endpoint stopped event to anyone listening
        String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STOPPED;
        postEvent(topic, cfg, null);
        cfg.clearActivePort();
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainQuiesced(ChainData chainData) {
        int oldState = chainState.getAndSet(ChainState.QUIESCED.val);
        if (oldState > ChainState.QUIESCED.val) {
            quiesceChain();
        }
    }

    private void quiesceChain() {
        // Notify the owner (which notifies the virtual hosts) that
        // we have stopped (or are in the process of stopping) listening..
        final ActiveConfiguration cfg = currentConfig;
        VirtualHostMap.notifyStopped(owner, cfg.resolvedHost, cfg.activePort, isHttps);
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainDestroyed(ChainData chainData) {
        chainState.set(ChainState.DESTROYED.val);
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainUpdated(ChainData chainData) {
        // Not Applicable: this method is only called when the channels comprising the
        // chain change. We're using fixed chain configurations (in terms of channel
        // elements).
    }

    /**
     * Publish an event relating to a chain starting/stopping with the
     * given properties set about the chain.
     */
    private void postEvent(String t, ActiveConfiguration c, Exception e) {
        Map<String, Object> eventProps = new HashMap<String, Object>(4);

        eventProps.put(HttpServiceConstants.ENDPOINT_NAME, endpointName);
        eventProps.put(HttpServiceConstants.ENDPOINT_ACTIVE_PORT, c.activePort);
        eventProps.put(HttpServiceConstants.ENDPOINT_CONFIG_HOST, c.configHost);
        eventProps.put(HttpServiceConstants.ENDPOINT_CONFIG_PORT, c.configPort);
        eventProps.put(HttpServiceConstants.ENDPOINT_IS_HTTPS, isHttps);

        if (e != null) {
            eventProps.put(HttpServiceConstants.ENDPOINT_EXCEPTION, e.toString());
        }

        EventAdmin engine = owner.getEventAdmin();
        if (engine != null) {
            Event event = new Event(t, eventProps);
            engine.postEvent(event);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[@=" + System.identityHashCode(this)
               + ",enabled=" + enabled
               + ",state=" + ChainState.printState(chainState.get())
               + ",chainName=" + chainName
               + ",config=" + currentConfig + "]";
    }

    /**
     * Get the state of the chain.
     *
     * @return An integer representation of the state.
     */
    public int getChainState() {
        return chainState.get();
    }

    public final class ActiveConfiguration {
        final boolean isHttps;
        public final int configPort;
        public final String configHost;
        final String resolvedHost;

        final Map<String, Object> tcpOptions;
        final Map<String, Object> sslOptions;
        final Map<String, Object> httpOptions;
        final Map<String, Object> remoteIp;
        final Map<String, Object> compression;
        final Map<String, Object> samesite;
        final Map<String, Object> headers;
        final Map<String, Object> endpointOptions;

        volatile int activePort = -1;
        public boolean validConfiguration = false;

        public ActiveConfiguration(boolean isHttps,
                                   Map<String, Object> tcp,
                                   Map<String, Object> ssl,
                                   Map<String, Object> http,
                                   Map<String, Object> remoteIp,
                                   Map<String, Object> compression,
                                   Map<String, Object> samesite,
                                   Map<String, Object> headers,
                                   Map<String, Object> endpoint,
                                   String resolvedHostName) {
            this.isHttps = isHttps;
            tcpOptions = tcp;
            sslOptions = ssl;
            httpOptions = http;
            this.remoteIp = remoteIp;
            this.compression = compression;
            this.samesite = samesite;
            this.headers = headers;
            endpointOptions = endpoint;

            String attribute = isHttps ? "httpsPort" : "httpPort";
            configPort = MetatypeUtils.parseInteger(HttpServiceConstants.ENPOINT_FPID_ALIAS, attribute,
                                                    endpointOptions.get(attribute),
                                                    -1);
            configHost = (String) endpointOptions.get("host");
            resolvedHost = resolvedHostName;
        }

        /**
         * Reset the active port to -1 (not actively listening)
         */
        public void clearActivePort() {
            activePort = -1;
        }

        /**
         * @return true if the active port matches the listening port. False otherwise (not listening or no match)
         */
        public boolean validateActivePort() {
            try {
                return activePort == cfw.getListeningPort(chainName);
            } catch (ChainException ce) {
            }
            return false;
        }

        /**
         * @return the active port, if it can be determined, or -1.
         */
        @FFDCIgnore(ChainException.class)
        public int getActivePort() {
            if (configPort < 0)
                return -1;

            if (activePort == -1) {
                try {
                    activePort = cfw.getListeningPort(chainName);
                } catch (ChainException ce) {
                    activePort = -1;
                }
            }
            return activePort;
        }

        /**
         * @return true if the ActiveConfiguration contains the required
         *         configuration to start the http chains. The base http
         *         chain needs both tcp and http options. The https chain
         *         additionally needs ssl options.
         */
        @Trivial
        public boolean complete() {
            if (tcpOptions == null || httpOptions == null)
                return false;

            if (isHttps && sslOptions == null)
                return false;

            return true;
        }

        /**
         * Check to see if all of the maps are the same as they
         * were the last time: ConfigurationAdmin returns unmodifiable
         * maps: if the map instances are the same, there have been no
         * updates.
         */
        public boolean unchanged(ActiveConfiguration other) {
            if (other == null)
                return false;

            // Only look at ssl options if this is an https chain
            if (isHttps) {
                return configHost.equals(other.configHost) &&
                       configPort == other.configPort &&
                       tcpOptions == other.tcpOptions &&
                       sslOptions == other.sslOptions &&
                       httpOptions == other.httpOptions &&
                       remoteIp == other.remoteIp &&
                       compression == other.compression &&
                       samesite == other.samesite &&
                       headers == other.headers &&
                       !endpointChanged(other);
            } else {
                return configHost.equals(other.configHost) &&
                       configPort == other.configPort &&
                       tcpOptions == other.tcpOptions &&
                       httpOptions == other.httpOptions &&
                       remoteIp == other.remoteIp &&
                       compression == other.compression &&
                       samesite == other.samesite &&
                       headers == other.headers &&
                       !endpointChanged(other);
            }
        }

        protected boolean tcpChanged(ActiveConfiguration other) {
            if (other == null)
                return true;

            return !configHost.equals(other.configHost) ||
                   configPort != other.configPort ||
                   tcpOptions != other.tcpOptions;
        }

        protected boolean sslChanged(ActiveConfiguration other) {
            if (other == null)
                return true;

            return sslOptions != other.sslOptions;
        }

        protected boolean httpChanged(ActiveConfiguration other) {
            if (other == null)
                return true;

            return (httpOptions != other.httpOptions) || (remoteIp != other.remoteIp) || (compression != other.compression) || (samesite != other.samesite)
                   || (headers != other.headers);

        }

        protected boolean endpointChanged(ActiveConfiguration other) {
            if (other == null)
                return true;

            // Instance equality doesn't work for this one, because the endpoint options
            // are the httpEndpoint's service properties, and they will change for reasons
            // that shouldn't cause a chain to restart
            return !endpointOptions.get(Constants.SERVICE_PID).equals(other.endpointOptions.get(Constants.SERVICE_PID));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                   + "[host=" + configHost
                   + ",resolvedHost=" + resolvedHost
                   + ",port=" + configPort
                   + ",listening=" + activePort
                   + ",complete=" + complete()
                   + ",tcpOptions=" + System.identityHashCode(tcpOptions)
                   + ",httpOptions=" + System.identityHashCode(httpOptions)
                   + ",remoteIp=" + System.identityHashCode(remoteIp)
                   + ",compression=" + System.identityHashCode(compression)
                   + ",samesite=" + System.identityHashCode(samesite)
                   + ",headers=" + System.identityHashCode(headers)
                   + ",sslOptions=" + (isHttps ? System.identityHashCode(sslOptions) : "0")
                   + ",endpointOptions=" + endpointOptions.get(Constants.SERVICE_PID)
                   + "]";
        }
    }

    private class StopWait {

        @Trivial
        StopWait() {
        }

        synchronized void waitForStop(long timeout, HttpChain chain) {
            // HttpChain parameter helps with debug..

            // wait for the configured timeout (the parameter) + a smidgen of time
            // to allow the cfw to stop the chain after that configured quiesce
            // timeout expires
            long interval = timeout + 2345L;
            long waited = 0;

            // If, as far as we know, the chain hasn't been stopped yet, wait for
            // the stop notification for at most the timeout amount of time.
            while (chainState.get() > ChainState.STOPPED.val && waited < interval) {
                long start = System.nanoTime();
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(HttpChain.this, tc, "Waiting for chain stop", waited, interval);
                    }
                    wait(interval - waited);
                } catch (InterruptedException ie) {
                    // ignore
                }
                waited += System.nanoTime() - start;
            }
        }

        synchronized void notifyStopped() {
            notifyAll();
        }
    }
}
