/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal.netty;

import com.ibm.ws.http.channel.h2internal.Constants;
import com.ibm.ws.http.internal.HttpEndpointImpl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.openliberty.netty.http.NettyVirtualConnectionFactoryImpl;
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;
import io.openliberty.netty.internal.tcp.TCPUtils;

/**
 * Encapsulation of steps for starting/stopping an http chain in a controlled/predictable
 * manner with a minimum of synchronization.
 */
public class NettyChain {

    private static final TraceComponent tc = Tr.register(NettyChain.class);

    private final HttpEndpointImpl owner;
    private final boolean isHttps;

    private String endpointName;
    private String tcpName;
    private String sslName;
    private String httpName;
    private String dispatcherName;
    private String chainName;

    private NettyFrameworkImpl nettyFramework;

    /**
     * Toggled by enable/disabled methods. This serves only to block activity
     * of some operations (start/update on disabled chain should no-op)
     */
    private volatile boolean enabled = false;

    /**
     * A snapshot of the configuration (collection of properties objects) last used
     * for a start/update operation.
     */

    private ChainConfiguration currentConfig;

    ServerBootstrap bootstrap = null;
    private Channel serverChannel;

    NettyChain(HttpEndpointImpl owner, boolean isHttps) {
        this.owner = owner;
        this.isHttps = isHttps;
        bootstrap = (ServerBootstrap) this.nettyFramework.createTCPBootstrap(owner.getTcpOptions());

    }

    public void init(String endpointId, Object componentId, NettyFrameworkImpl netty) {
        final String root = endpointId + (isHttps ? "-ssl" : "");

        nettyFramework = netty;

        endpointName = root;
        tcpName = root;
        sslName = "SSL-" + root;
        httpName = "HTTP-" + root;
        dispatcherName = "HTTPD-" + root;
        chainName = "CHAIN-" + root;

        enable(true);
    }

    public void enable(boolean enabled) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "enable chain " + this);
        }
        this.enabled = enabled;
    }

    /**
     * Disable this chain. The caller should make subsequent calls to perform actions
     * on the chain.
     */
    public void disable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "disable chain " + this);
        }
        enabled = false;
    }

    /**
     * Stop this chain. This chain will have to be recreated when port is updated
     * notifications/follow-on of stop operation is in the chainStopped listener method.
     */
    public void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "stop chain " + this);
        }

        if (serverChannel == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.entry(tc, "Netty channel not initialized, returning from stop");
            }
            return;
        } else {
            //When channel is stopped, remove the previously registered
            //Endpoint created in update
            this.nettyFramework.getEndpointManager().endpointMgr.removeEndPoint(endpointName);
        }

        //We don't have to check enabled/disabled here: chains are always allowed to stop

        if (currentConfig == null) {
            return;
        }

        try {
            ChannelFuture future = this.nettyFramework.stop(serverChannel);

            if (!future.isSuccess()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed stopping server channel " + serverChannel);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Successfully stopped server channel: " + serverChannel);
                }
            }

            TCPUtils.logChannelStopped(serverChannel);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error stopping chain " + this);
            }
        } finally {
            this.enabled = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "stop chain " + this);
        }
    }

    /**
     * Update/start the chain configurations.
     */
    public void update() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.event(this, tc, "update chain " + this);
        }
        // restart the chain by first shutting down active channel and then remaking the chain
        stop();
        init();

    }

    public ServerBootstrapExtended getBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        return this.nettyFramework.createTCPBootstrap(owner.getTcpOptions());
    }

    public VirtualConnection processNewConnection() {
        VirtualConnectionFactory factory = new NettyVirtualConnectionFactoryImpl();
        VirtualConnection vc;

        try {
            vc = factory.createConnection();
            return vc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
