/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package com.ibm.ws.wsoc.outbound;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.exception.NettyException;

/**
 * Encapsulation of steps for starting/stopping an http chain in a controlled/predictable
 * manner with a minimum of synchronization.
 */
public class WsocChain {
    private static final TraceComponent tc = Tr.register(WsocChain.class);

    private final WsocOutboundChain owner;
    private final boolean isHttps;

    private String endpointName;
    private String tcpName;
    private String sslName;
    private String httpName;
    private String chainName;
    private ChannelFramework cfw;

    // Netty items
    private NettyFramework nettyBundle;
    private boolean useNettyTransport;
    private BootstrapExtended nettyBootstrap;

    /**
     * Will set the chain to enabled after a custoemr needs a wsoc outbound chain - so when they use the JSR 356 API
     *
     */
    private volatile boolean enabled = false;

    /**
     * We'll use the configured flag to see if ssl is available.
     */
    private volatile boolean configured = false;

    /**
     * A snapshot of the configuration (collection of properties objects) last used
     * for a start/update operation.
     */
    private volatile ActiveConfiguration currentConfig = null;

    /**
     * Create the new chain with it's parent endpoint
     *
     * @param httpEndpointImpl the owning endpoint: used for notifications
     * @param isHttps          true if this is to be an https chain.
     */
    public WsocChain(WsocOutboundChain owner, boolean isHttps) {
        this.owner = owner;
        this.isHttps = isHttps;
        if (!isHttps) {
            configured = true;
        }
    }

    /**
     * Initialize this chain manager: Channel and chain names shouldn't fluctuate as config changes,
     * so come up with names associated with this set of channels/chains that will be reused regardless
     * of start/stop/enable/disable/modify
     *
     * @param endpointId The id of the httpEndpoint
     * @param cfw        Channel framework
     */
    public void init(String chainId, ChannelFramework cfw) {

        tcpName = "TCP-" + chainId;
        sslName = "SSL-" + chainId;
        httpName = "HTTP-" + chainId;
        chainName = chainId;
        this.cfw = cfw;
        useNettyTransport = false;

        // If there is a chain that is in the CFW with this name, it was potentially
        // left over from a previous instance of the endpoint. There is no way to get
        // the state of the existing (old) CFW chain to set our chainState accordingly...
        // (in addition to the old chain pointing to old services and things.. )
        // *IF* there is an old chain, stop, destroy, and remove it.
        try {
            ChainData cd = cfw.getChain(chainName);
            if (cd != null) {
                cfw.removeChain(cd);

            }

        } catch (ChainException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error stopping chain " + chainName, this, e);
            }
        }
    }

    public void init(String chainId, NettyFramework nettyBundle) {

        tcpName = "TCP-" + chainId;
        sslName = "SSL-" + chainId;
        httpName = "HTTP-" + chainId;
        chainName = chainId;
        this.nettyBundle = nettyBundle;
        useNettyTransport = true;

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
     * Enable this chain: this happens automatically for the http chain,
     * but is delayed on the ssl chain until ssl support becomes available.
     * This does not change the chain's state. The caller should
     * make subsequent calls to perform actions on the chain.
     */
    public void setConfigured(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "enable chain " + this);
        }
        configured = value;
    }

    /**
     * Stop this chain
     */
    public synchronized void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "stop chain " + this);
        }
        if (useNettyTransport)
            nettyStop();
        else
            legacyStop();
    }

    public synchronized void legacyStop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "stop legacy chain " + this);
        }

        // We don't have to check enabled/disabled here: chains are always allowed to stop.
        if (currentConfig == null)
            return;

        // Quiesce and then stop the chain. The CFW internally uses a StopTimer for
        // the quiesce/stop operation-- the listener method will be called when the chain
        // has stopped. So to see what happens next, visit chainStopped
        try {
            ChainData cd = cfw.getChain(chainName);
            if (cd != null) {
                cfw.removeChain(cd);
            }

        } catch (ChainException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error stopping chain " + chainName, this, e);
            }
        }
    }

    public synchronized void nettyStop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "stop netty chain " + this);
        }

        // We don't have to check enabled/disabled here: chains are always allowed to stop.
        if (currentConfig == null)
            return;
        // Do we need to do anything here?

    }

    /**
     * Update/start the chain configuration.
     */
//    @FFDCIgnore({ ChannelException.class, ChainException.class })
    public synchronized void update() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update chain " + this);
        }

        // Don't update or start the chain if it is disabled or the framework is stopping..
        if (!enabled || !configured || FrameworkState.isStopping())
            return;

        final ActiveConfiguration oldConfig = currentConfig;

        // The old configuration was "valid" if it existed, and if it was correctly configured
        final boolean validOldConfig = oldConfig == null ? false : oldConfig.validConfiguration;

        Map<String, Object> tcpOptions = owner.getTcpOptions();
        Map<String, Object> sslOptions = (isHttps) ? owner.getSslOptions() : null;
        Map<String, Object> httpOptions = owner.getHttpOptions();

        final ActiveConfiguration newConfig = new ActiveConfiguration(isHttps, tcpOptions, sslOptions, httpOptions);

        if (!newConfig.complete()) {

            // save the new/changed configuration before we start setting up the new chain
            currentConfig = newConfig;

            if (useNettyTransport) {
                // Do we need to do anything here?
            } else {
                // Stop the chain-- will have to be recreated when port is updated
                // notification/follow-on of stop operation is in the chainStopped listener method
                try {
                    ChainData cd = cfw.getChain(chainName);
                    if (cd != null) {
                        cfw.removeChain(cd);
                    }

                } catch (ChainException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Error stopping chain " + chainName, oldConfig, e);
                    }
                }
            }
        } else {

            boolean sameConfig = newConfig.unchanged(oldConfig);
            if (validOldConfig) {
                if (sameConfig) {
                    return;
                }
            }

            if (useNettyTransport) {
                nettyUpdate(newConfig, tcpOptions, sslOptions, httpOptions);
            } else {
                legacyUpdate(newConfig, tcpOptions, sslOptions, httpOptions);
            }
        }

    }

    @FFDCIgnore({ ChannelException.class, ChainException.class })
    public synchronized void legacyUpdate(ActiveConfiguration newConfig, Map<String, Object> tcpOptions, Map<String, Object> sslOptions, Map<String, Object> httpOptions) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update chain " + this);
        }

        final ActiveConfiguration oldConfig = currentConfig;

        try {
            ChainData cd = cfw.getChain(chainName);
            if (cd != null) {
                cfw.removeChain(cd);
            }

            // Remove any channels that have to be rebuilt..
            if (newConfig.tcpChanged(oldConfig)) {
                removeChannel(tcpName);
            }

            if (newConfig.sslChanged(oldConfig)) {
                removeChannel(sslName);
            }

            if (newConfig.httpChanged(oldConfig)) {
                removeChannel(httpName);
            }

        } catch (ChainException e) {
            e.printStackTrace();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error stopping chain " + chainName, oldConfig, e);
            }
        }

        // save the new/changed configuration before we start setting up the new chain
        currentConfig = newConfig;
        try {
            Map<Object, Object> chanProps;

            // TCP Channel
            ChannelData tcpChannel = cfw.getChannel(tcpName);
            if (tcpChannel == null) {
                String typeName = (String) tcpOptions.get("type");
                chanProps = new HashMap<Object, Object>(tcpOptions);

                tcpChannel = cfw.addChannel(tcpName, cfw.lookupFactory(typeName), chanProps);
            }

            // SSL Channel
            if (isHttps) {
                ChannelData sslChannel = cfw.getChannel(sslName);
                if (sslChannel == null) {
                    sslChannel = cfw.addChannel(sslName, cfw.lookupFactory("SSLChannel"), new HashMap<Object, Object>(sslOptions));
                }
            }

            // HTTP Channel
            ChannelData httpChannel = cfw.getChannel(httpName);
            if (httpChannel == null) {
                chanProps = new HashMap<Object, Object>(httpOptions);
                // Put the endpoint id, which allows us to find the registered access log
                // dynamically
                httpChannel = cfw.addChannel(httpName, cfw.lookupFactory("HTTPOutboundChannel"), chanProps);
            }

            // Add chain
            ChainData cd = cfw.getChain(chainName);
            if (null == cd) {
                final String[] chanList;
                if (isHttps)
                    chanList = new String[] { httpName, sslName, tcpName };
                else
                    chanList = new String[] { httpName, tcpName };

                cd = cfw.addChain(chainName, FlowType.OUTBOUND, chanList);
                cd.setEnabled(enabled);

            }

            // We configured the chain successfully
            newConfig.validConfiguration = true;

        } catch (ChannelException e) {
            // handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
        } catch (ChainException e) {
            // handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
        } catch (Exception e) {
            // The exception stack for this is all internals and does not belong in messages.log.
            //  Question: need error message here?
            //  Tr.error(tc, "config.httpChain.error", tcpName, e.toString());
            //  handleStartupError(e, newConfig);
        }

    }

    public synchronized void nettyUpdate(ActiveConfiguration newConfig, Map<String, Object> tcpOptions, Map<String, Object> sslOptions, Map<String, Object> httpOptions) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update chain " + this);
        }

        // TODO Work with all other options

        try {
            nettyBootstrap = nettyBundle.createTCPBootstrapOutbound(tcpOptions);
//            nettyBootstrap.handler(new WsocClientInitializer(nettyBootstrap.getBaseInitializer(), target));
            if (isHttps) {
                owner.secureBootstrap = nettyBootstrap;
                owner.currentSSL = sslOptions;
            }
            else
                owner.unsecureBootstrap = nettyBootstrap;
        } catch (NettyException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();

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

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[@=" + System.identityHashCode(this)
               + ",enabled=" + enabled
               + ",configured=" + configured
               + ",chainName=" + chainName
               + ",config=" + currentConfig + "]";
    }

    private final class ActiveConfiguration {
        final boolean isHttps;

        final Map<String, Object> tcpOptions;
        final Map<String, Object> sslOptions;
        final Map<String, Object> httpOptions;

        volatile int activePort = -1;
        boolean validConfiguration = false;

        ActiveConfiguration(boolean isHttps,
                            Map<String, Object> tcp,
                            Map<String, Object> ssl,
                            Map<String, Object> http) {
            this.isHttps = isHttps;
            tcpOptions = tcp;
            sslOptions = ssl;
            httpOptions = http;

            String attribute = isHttps ? "httpsPort" : "httpPort";

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
         * CHeck to see if all of the maps are the same as they
         * were the last time: ConfigurationAdmin returns unmodifiable
         * maps: if the map instances are the same, there have been no
         * updates.
         */
        protected boolean unchanged(ActiveConfiguration other) {
            if (other == null)
                return false;

            // Only look at ssl options if this is an https chain
            if (isHttps) {
                return tcpOptions == other.tcpOptions &&
                       sslOptions == other.sslOptions &&
                       httpOptions == other.httpOptions;

            } else {
                return tcpOptions == other.tcpOptions &&
                       httpOptions == other.httpOptions;
            }
        }

        protected boolean tcpChanged(ActiveConfiguration other) {
            if (other == null)
                return false;

            return tcpOptions != other.tcpOptions;
        }

        protected boolean sslChanged(ActiveConfiguration other) {
            if (other == null)
                return false;

            return sslOptions != other.sslOptions;
        }

        protected boolean httpChanged(ActiveConfiguration other) {
            if (other == null)
                return false;

            return httpOptions != other.httpOptions;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                   + ",complete=" + complete()
                   + ",tcpOptions=" + System.identityHashCode(tcpOptions)
                   + ",httpOptions=" + System.identityHashCode(httpOptions)
                   + ",sslOptions=" + (isHttps ? System.identityHashCode(sslOptions) : "0")
                   + "]";
        }
    }

}