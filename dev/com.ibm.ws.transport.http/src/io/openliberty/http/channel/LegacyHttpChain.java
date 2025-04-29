/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.http.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherConfig;
import com.ibm.ws.http.internal.HttpEndpointImpl;
import com.ibm.ws.http.internal.HttpServiceConstants;
import com.ibm.ws.http.internal.VirtualHostMap;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.InvalidRuntimeStateException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * A legacy chain implementation that uses the older ChannelFramework APIs.
 * It implements {@link Chain} so it can be substituted for newer chain 
 * implementations without changing {@link HttpEndpointImpl}.
 */
public class LegacyHttpChain extends AbstractHttpChain implements ChainEventListener {

    private static final TraceComponent tc = Tr.register(LegacyHttpChain.class);

    /** Channel framework references needed by the legacy approach. */
    private ChannelFramework framework;

    protected final StopWait stopWait = new StopWait();

   /**
     * Constructs the legacy chain tied to a specific endpoint.
     * 
     * @param endpoint The endpoint that owns this chain.
     * @param isHttps Whether this chain is for HTTPS rather than HTTP.
     */
    public LegacyHttpChain(HttpEndpointImpl endpoint, boolean isHttps) {
        super(endpoint, isHttps);
    }

    /**
     * Initializes references to ChannelFramework. Typically called
     * by {@link HttpEndpointImpl} once.
     * 
     * @param endpointId The ID used for naming the chain.
     * @param cfBundle The CHFWBundle that provides ChannelFramework references.
     */
    public void init(String endpointId, CHFWBundle cfBundle) {
        this.framework = cfBundle.getFramework();
        this.endpointManager = cfBundle.getEndpointManager();
        
        final String root = endpointId + (isHttps() ? "-ssl" : "");

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
            ChainData cd = framework.getChain(chainName);
            if (cd != null) {
                framework.stopChain(cd, 0L); // no timeout: FORCE the stop.
                framework.destroyChain(cd);
                framework.removeChain(cd);
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
        endpointManager.removeEndPoint(endpointName);

        // We don't have to check enabled/disabled here: chains are always allowed to stop.
        if (config() == null || state().get().value() <= ChainState.QUIESCED.value())
            return;

        // Quiesce and then stop the chain. The CFW internally uses a StopTimer for
        // the quiesce/stop operation-- the listener method will be called when the chain
        // has stopped. So to see what happens next, visit chainStopped
        try {
            ChainData cd = framework.getChain(chainName);
            if (cd != null) {
                framework.stopChain(cd, framework.getDefaultChainQuiesceTimeout());
                stopWait.waitForStop(framework.getDefaultChainQuiesceTimeout()); // BLOCK
                try {
                    framework.destroyChain(cd);
                    framework.removeChain(cd);
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
    public synchronized void update(String host) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update chain " + this);
        }

        // Don't update or start the chain if it is disabled or the framework is stopping..
        if (!enabled() || FrameworkState.isStopping())
            return;

        final ChainConfiguration oldConfig = config();

        // The old configuration was "valid" if it existed, and if it was correctly configured
        final boolean validOldConfig = oldConfig == null ? false : oldConfig.isValid();

        Map<String, Object> tcpOptions = endpoint().getTcpOptions();
        Map<String, Object> sslOptions = (isHttps()) ? endpoint().getSslOptions() : null;
        Map<String, Object> httpOptions = endpoint().getHttpOptions();
        Map<String, Object> endpointOptions = endpoint().getEndpointOptions();
        Map<String, Object> remoteIpOptions = endpoint().getRemoteIpConfig();
        Map<String, Object> compressionOptions = endpoint().getCompressionConfig();
        Map<String, Object> samesiteOptions = endpoint().getSamesiteConfig();
        Map<String, Object> headersOptions = endpoint().getHeadersConfig();

        final ChainConfiguration newConfig = new ChainConfiguration(isHttps(), 
                                                    tcpOptions, 
                                                    sslOptions, 
                                                    httpOptions,
                                                    endpointOptions,
                                                    remoteIpOptions, 
                                                    compressionOptions, 
                                                    samesiteOptions, 
                                                    headersOptions
                                                    );

        if (!newConfig.isComplete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Stopping chain due to configuration " + newConfig);
            }

            // save the new/changed configuration before we start setting up the new chain
            this.config = newConfig;
            this.host = host;

            stop();
        } else {
            Map<Object, Object> chanProps;

            try {
                boolean sameConfig = !newConfig.requiresRestart(oldConfig);
                if (validOldConfig) {
                    if (sameConfig) {
                        if (state().get() == ChainState.STARTED) {
                            // If configurations are identical, see if the listening port is also the same
                            // which would indicate that the chain is running with the unchanged configuration
                            // toggle start/stop of chain if we are somehow active on a different port..
                            sameConfig = validateActivePort();
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
                        } else if (state().get() == ChainState.QUIESCED) {
                            // Chain is in the process of stopping.. we need to wait for it
                            // to finish stopping before we start it again
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Configuration is unchanged, chain is quiescing, wait for stop: " + newConfig);
                            }
                            stopWait.waitForStop(framework.getDefaultChainQuiesceTimeout()); // BLOCK
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
                    ChainData cd = framework.getChain(chainName);
                    if (cd != null) {
                        framework.stopChain(cd, framework.getDefaultChainQuiesceTimeout());
                        stopWait.waitForStop(framework.getDefaultChainQuiesceTimeout()); // BLOCK
                        framework.destroyChain(cd);
                        framework.removeChain(cd);
                    }
                        removeChannel(tcpName);
                        removeChannel(sslName);
                        removeChannel(httpName);
                        removeChannel(dispatcherName);
                }

                // save the new/changed configuration before we start setting up the new chain
                config = newConfig;
                this.host = host;

                // Define and register an EndPoint to represent this chain
                EndPointInfo ep = endpointManager.defineEndPoint(endpointName, newConfig.host(), newConfig.port());

                // TCP Channel
                ChannelData tcpChannel = framework.getChannel(tcpName);
                if (tcpChannel == null) {
                    String typeName = (String) tcpOptions.get("type");
                    chanProps = new HashMap<Object, Object>(tcpOptions);
                    chanProps.put("endPointName", endpointName);
                    chanProps.put("hostname", ep.getHost());
                    chanProps.put("port", String.valueOf(ep.getPort()));

                    tcpChannel = framework.addChannel(tcpName, framework.lookupFactory(typeName), chanProps);
                }

                // SSL Channel
                if (isHttps()) {
                    ChannelData sslChannel = framework.getChannel(sslName);
                    if (sslChannel == null) {
                        chanProps = new HashMap<Object, Object>(sslOptions);
                        // Put the protocol version, which allows the http channel to dynamically
                        // know what http version it will use.
                        if (endpoint().getProtocolVersion() != null) {
                            chanProps.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, endpoint().getProtocolVersion());
                        }
                        sslChannel = framework.addChannel(sslName, framework.lookupFactory("SSLChannel"), chanProps);
                    }
                }

                // HTTP Channel
                ChannelData httpChannel = framework.getChannel(httpName);
                if (httpChannel == null) {
                    chanProps = new HashMap<Object, Object>(httpOptions);
                    // Put the endpoint id, which allows us to find the registered access log
                    // dynamically
                    chanProps.put(HttpConfigConstants.PROPNAME_ACCESSLOG_ID, endpoint().getName());
                    // Put the protocol version, which allows the http channel to dynamically
                    // know what http version it will use.
                    if (endpoint().getProtocolVersion() != null) {
                        chanProps.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, endpoint().getProtocolVersion());
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
                        chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES_INTERNAL, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM_INTERNAL, null);
                    }

                    else {
                        chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION, "true");
                        //Check if the compression is configured to use content-type filter
                        if (compressionOptions.containsKey("types")) {
                            chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES_INTERNAL, compressionOptions.get("types"));

                        }
                        if (compressionOptions.containsKey("serverPreferredAlgorithm")) {
                            chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM_INTERNAL, compressionOptions.get("serverPreferredAlgorithm"));
                        }
                    }

                    if (samesiteOptions.get("id").equals("defaultSameSite")) {
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE, "false");
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_LAX, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_NONE, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_STRICT, null);
                        chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_PARTITIONED, "false");
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
                        if (samesiteOptions.containsKey("partitioned")) {
                            enableSameSite = true;
                            chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_PARTITIONED, samesiteOptions.get("partitioned"));
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
                    httpChannel = framework.addChannel(httpName, framework.lookupFactory("HTTPInboundChannel"), chanProps);
                }

                // HTTPDispatcher Channel
                ChannelData httpDispatcher = framework.getChannel(dispatcherName);
                if (httpDispatcher == null) {
                    chanProps = new HashMap<Object, Object>();
                    chanProps.put(HttpDispatcherConfig.PROP_ENDPOINT, endpoint().getPid());

                    httpDispatcher = framework.addChannel(dispatcherName, framework.lookupFactory("HTTPDispatcherChannel"), chanProps);
                }

                // Add chain
                ChainData cd = framework.getChain(chainName);
                if (null == cd) {
                    final String[] chanList;
                    if (isHttps())
                        chanList = new String[] { tcpName, sslName, httpName, dispatcherName };
                    else
                        chanList = new String[] { tcpName, httpName, dispatcherName };

                    cd = framework.addChain(chainName, FlowType.INBOUND, chanList);
                    cd.setEnabled(enabled());
                    framework.addChainEventListener(this, chainName);

                    // initialize the chain: this will find/create the channels in the chain,
                    // initialize each channel, and create the chain. If there are issues with any
                    // channel properties, they will surface here
                    // THIS INCLUDES ATTEMPTING TO BIND TO THE PORT
                    framework.initChain(chainName);
                }

                // We configured the chain successfully
                newConfig.setValidity(true); 
            } catch (ChannelException e) {
                handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
            } catch (ChainException e) {
                handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
            } catch (Exception e) {
                // The exception stack for this is all internals and does not belong in messages.log.
                Tr.error(tc, "config.httpChain.error", tcpName, e.toString());
                handleStartupError(e, newConfig);
            }

            if (newConfig.isValid()) {
                try {
                    // Start the chain: follow along to chainStarted method (CFW callback)
                    framework.startChain(chainName);
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
            framework.removeChannel(name);
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

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainInitialized(ChainData chainData) {
        state().set(ChainState.INITIALIZED);
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public synchronized void chainStarted(ChainData chainData) {
        state().set(ChainState.STARTED);
        port = activatePort();

        if (port > 0) {
            // HOORAY! we have a bound listener.
            // Notify listeners that the chain was started.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "New configuration started " + config());
            }

            VirtualHostMap.notifyStarted(endpoint(), () -> host, port, isHttps());

            // Post an endpoint started event to anyone listening
            String topic = endpoint().getEventTopic() + HttpServiceConstants.ENDPOINT_STARTED;
            postEvent(topic, config(), null);
        }
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainStopped(ChainData chainData) {

        int oldState = state().getAndSet(ChainState.STOPPED).value();
        if (oldState > ChainState.QUIESCED.value()) {
            quiesceChain();
        }

        // Wake up anything waiting for the chain to stop
        // (see the update method for one example)
        stopWait.notifyStopped();

        // Post an endpoint stopped event to anyone listening
        String topic = endpoint().getEventTopic() + HttpServiceConstants.ENDPOINT_STOPPED;
        postEvent(topic, config(), null);
        this.port = -1;
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainQuiesced(ChainData chainData) {
        int oldState = state().getAndSet(ChainState.QUIESCED).value();
        if (oldState > ChainState.QUIESCED.value()) {
            quiesceChain();
        }
    }

    private void quiesceChain() {
        // Notify the owner (which notifies the virtual hosts) that
        // we have stopped (or are in the process of stopping) listening..
        VirtualHostMap.notifyStopped(endpoint(), host, config.port(), isHttps());
    }

    /**
     * ChainEventListener method.
     * This method can not be synchronized (deadlock with update/stop).
     * Rely on CFW synchronization of chain operations.
     */
    @Override
    public void chainDestroyed(ChainData chainData) {
        state().set(ChainState.DESTROYED);
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
     * @return true if the active port matches the listening port. False otherwise (not listening or no match)
     */
    public boolean validateActivePort() {
        try {
            return port == framework.getListeningPort(chainName);
        } catch (ChainException ce) {
        }
        return false;
    }

    /**
     * @return the active port, if it can be determined, or -1.
     */
    @FFDCIgnore(ChainException.class)
    public int activatePort() {
        if (config== null || config.port() < 0)
            return -1;

        if (port == -1) {
            try {
                port = framework.getListeningPort(chainName);
            } catch (ChainException ce) {
                port = -1;
            }
        }
        return port;
    }


    private class StopWait {

        @Trivial
        StopWait() {
        }

        public synchronized void waitForStop(long timeout) {
            // HttpChain parameter helps with debug..

            // wait for the configured timeout (the parameter) + a smidgen of time
            // to allow the cfw to stop the chain after that configured quiesce
            // timeout expires
            long interval = timeout + 2345L;
            long waited = 0;

            // If, as far as we know, the chain hasn't been stopped yet, wait for
            // the stop notification for at most the timeout amount of time.
            while (state().get().value() > ChainState.STOPPED.value() && waited < interval) {
                long start = System.nanoTime();
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(LegacyHttpChain.this, tc, "Waiting for chain stop", waited, interval);
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