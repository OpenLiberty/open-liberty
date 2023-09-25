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

import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.tcp.TCPUtils;
import io.openliberty.netty.internal.tls.NettyTlsProvider;

/**
 * Encapsulation of steps for starting/stopping an http chain in a controlled/predictable
 * manner with a minimum of synchronization.
 */
public class NettyWsocChain extends WsocChain {
    private static final TraceComponent tc = Tr.register(NettyWsocChain.class);

    private NettyFramework netty;
    private Wsoc10Address target;

    BootstrapExtended bootstrap = new BootstrapExtended();
    protected EndPointMgr endpointMgr;
    private NettyTlsProvider nettyTlsProvider = null;

    private Channel chan;
    NettyWsocChain parent = null;

    private String chainId;

    /**
     * Create the new chain with it's parent endpoint
     *
     * @param httpEndpointImpl the owning endpoint: used for notifications
     * @param isHttps          true if this is to be an https chain.
     */
    public NettyWsocChain(WsocOutboundChain owner, boolean isHttps) {
        super(owner, isHttps, true);
    }

    @Reference(name = "nettyTlsProvider", cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY, unbind = "unbindTlsProviderService")
    protected void bindNettyTlsProvider(NettyTlsProvider tlsProvider) {
        System.out.println("Setting Netty TLS provider");
        this.nettyTlsProvider = tlsProvider;
    }

    protected void unbindTlsProviderService(NettyTlsProvider bundle) {
        this.nettyTlsProvider = null;
    }

    public NettyTlsProvider getNettyTlsProvider() {
        return this.nettyTlsProvider;
    }

    /**
     * Initialize this chain manager: Channel and chain names shouldn't fluctuate as config changes,
     * so come up with names associated with this set of channels/chains that will be reused regardless
     * of start/stop/enable/disable/modify
     *
     * @param endpointId The id of the httpEndpoint
     * @param cfw        Channel framework
     */

    @Override
    public void init(String chainId, NettyFramework netty) {
        this.chainId = chainId;
        this.netty = netty;
    }

    /**
     * Stop this chain
     */
    @Override
    public synchronized void stop() {

        if (Objects.isNull(chan)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.entry(tc, "Netty channel not initialized, returning from stop");
            }
            return;
        } else {
            //When channel is stopped, remove the previously registered endpoint
            //created in the update
            this.endpointMgr.removeEndPoint(endpointName);
        }

        try {
            ChannelFuture future = this.netty.stop(chan);

            if (!future.isSuccess()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed stopping server channel " + chan);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Successfully stopped server channel " + chan);
                }
            }
            TCPUtils.logChannelStopped(chan);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error stopping chain " + this);
            }
        } finally {
            this.disable();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(this, tc, "stop chain " + this);
        }

    }

    /**
     * Update/start the chain configuration.
     */
    @Override
    //@FFDCIgnore({ ChannelException.class, ChainException.class })
    public synchronized void update() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update chain " + this);
        }

        // Don't update or start the chain if it is disabled or the framework is stopping..
        if (!enabled || !configured || FrameworkState.isStopping())
            return;

        ActiveConfiguration oldConfig = currentConfig;

        // The old configuration was "valid" if it existed, and if it was correctly configured
        final boolean validOldConfig = oldConfig == null ? false : oldConfig.validConfiguration;

        Map<String, Object> tcpOptions = owner.getTcpOptions();
        Map<String, Object> sslOptions = (isHttps) ? owner.getSslOptions() : null;
        Map<String, Object> httpOptions = owner.getHttpOptions();

        final ActiveConfiguration newConfig = new ActiveConfiguration(isHttps, tcpOptions, sslOptions, httpOptions);

        // save the new/changed configuration before we start setting up the new chain
        currentConfig = newConfig;
        // We configured the chain successfully
        newConfig.validConfiguration = true;
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

}
