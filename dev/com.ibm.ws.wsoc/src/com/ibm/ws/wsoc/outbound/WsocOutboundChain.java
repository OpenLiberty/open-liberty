/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsoc.outbound;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.wsspi.channelfw.ChannelConfiguration;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.tls.NettyTlsProvider;

public class WsocOutboundChain {

    private static final TraceComponent tc = Tr.register(WsocOutboundChain.class);

    /** use _tcpOptions service direct instead of reference as _tcpOptions is a required service */
    private ChannelConfiguration tcpOptions = null;

    /** use _tcpOptions service direct instead of reference as _httpOptions is a required service */
    private ChannelConfiguration httpOptions = null;

    /** Optional,dynamic reference to sslOptions */
    private final AtomicServiceReference<ChannelConfiguration> sslOptions = new AtomicServiceReference<ChannelConfiguration>("sslOptions");

    /** Optional, dynamic reference to an SSL channel factory provider: could be used to start/stop SSL chains */
    private final AtomicServiceReference<ChannelFactoryProvider> sslFactoryProvider = new AtomicServiceReference<ChannelFactoryProvider>("sslSupport");

    /** Required, static Channel framework reference */
    private static CHFWBundle chfw = null;

    private final WsocChain wsocChain = new WsocChain(this, false);
    private final WsocChain wsocSecureChain = new WsocChain(this, true);

    private volatile boolean outboundCalled = true;

    public static final String WS_CHAIN_NAME = "WsocOutboundHttp";
    public static final String WSS_CHAIN_NAME = "WsocOutboundHttpSecure";

    /** Required, static Netty framework reference */
    private static NettyFramework nettyBundle;
    private static NettyTlsProvider nettyTlsProvider;

    private static boolean useNettyTransport = false;
    protected static BootstrapExtended unsecureBootstrap;
    protected static BootstrapExtended secureBootstrap;
    protected static Map<String, Object> currentSSL;

    public static VirtualConnection getVCFactory(WsocAddress addr) throws ChainException, ChannelException {
        if (addr.isSecure()) {
            return getCfw().getOutboundVCFactory(WSS_CHAIN_NAME).createConnection();
        } else {
            return getCfw().getOutboundVCFactory(WS_CHAIN_NAME).createConnection();
        }

    }

    public static BootstrapExtended getBootstrap(WsocAddress addr) {
        if (addr.isSecure()) {
            return secureBootstrap;
        }
        return unsecureBootstrap;
    }

    public static boolean isUsingNetty() {
        return useNettyTransport;
    }

    public static Map<String, Object> getCurrentSslOptions() {
        return currentSSL;
    }

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param properties : Map containing service & config properties
     *                       populated/provided by config admin
     */
    protected void activate(Map<String, Object> properties, ComponentContext context) {
        sslOptions.activate(context);
        sslFactoryProvider.activate(context);

        // TODO: Updated this to use constants
        useNettyTransport = ProductInfo.getBetaEdition() &&
                            MetatypeUtils.parseBoolean(WS_CHAIN_NAME, "useNettyTransport", properties.get("useNettyTransport"), true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "activate, Netty bundle: " + nettyBundle);
        }

        if (useNettyTransport) {
            wsocChain.init(WS_CHAIN_NAME, nettyBundle);
            wsocSecureChain.init(WSS_CHAIN_NAME, nettyBundle);
        } else {
            wsocChain.init(WS_CHAIN_NAME, chfw.getFramework());
            wsocSecureChain.init(WSS_CHAIN_NAME, chfw.getFramework());
        }

        modified(properties);

    }

    @Modified
    protected void modified(Map<String, Object> config) {
        boolean usingNetty = ProductInfo.getBetaEdition() &&
                             MetatypeUtils.parseBoolean(WS_CHAIN_NAME, "useNettyTransport", config.get("useNettyTransport"), true);
        boolean unchangedTransport = useNettyTransport && usingNetty;
        useNettyTransport = usingNetty;
        modified(unchangedTransport);
    }

    private void modified(boolean unchangedTransport) {
        if (sslFactoryProvider.getService() != null) {
            wsocSecureChain.setConfigured(true);
        }
        if (!unchangedTransport) {
            if (useNettyTransport) {
                wsocChain.init(WS_CHAIN_NAME, nettyBundle);
                wsocSecureChain.init(WSS_CHAIN_NAME, nettyBundle);
            } else {
                wsocChain.init(WS_CHAIN_NAME, chfw.getFramework());
                wsocSecureChain.init(WSS_CHAIN_NAME, chfw.getFramework());
            }
        }
        wsocSecureChain.enable();
        wsocChain.enable();
        performAction(updateAction);

    }

    protected void deactivate(ComponentContext context) {

        performAction(stopAction);
        sslOptions.deactivate(context);
        sslFactoryProvider.deactivate(context);
    }

    @Trivial
    protected void setTcpOptions(ChannelConfiguration service) {

        tcpOptions = service;
        performAction(updateAction);

    }

    @Trivial
    protected void unsetTcpOptions(ServiceReference<ChannelConfiguration> service) {
    }

    @Trivial
    public Map<String, Object> getTcpOptions() {

        ChannelConfiguration c = tcpOptions;
        return c == null ? null : c.getConfiguration();
    }

    protected void setHttpOptions(ChannelConfiguration service) {

        httpOptions = service;
        performAction(updateAction);

    }

    @Trivial
    protected void unsetHttpOptions(ServiceReference<ChannelConfiguration> service) {
    }

    public Map<String, Object> getHttpOptions() {

        ChannelConfiguration c = httpOptions;
        return c == null ? null : c.getConfiguration();

    }

    //SslOption related functions
    @Trivial
    protected void setSslOptions(ServiceReference<ChannelConfiguration> service) {

        sslOptions.setReference(service);
        wsocSecureChain.setConfigured(true);
        performAction(updateAction);
    }

    @Trivial
    protected void unsetSslOptions(ServiceReference<ChannelFactoryProvider> ref) {

        if (sslFactoryProvider.unsetReference(ref)) {
            wsocSecureChain.setConfigured(false);
            // removal of ssl support includes removal of the ssl channel factory
            // the CFW is going to disable this chain once the factory goes away.
        }

    }

    public Map<String, Object> getSslOptions() {

        ChannelConfiguration c = sslOptions.getService();
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

    /**
     * @return ChannelFramework associated with the CHFWBundle service.
     */
    public static ChannelFramework getCfw() {
        if (null == chfw) {
            return ChannelFrameworkFactory.getChannelFramework();
        }
        return chfw.getFramework();

    }

    @Reference(name = "nettyTlsProvider")
    protected void setNettyTlsProvider(NettyTlsProvider bundle) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting nettyTlsBundle " + bundle);
        nettyTlsProvider = bundle;
    }

    /**
     * @return ChannelFramework associated with the CHFWBundle service.
     */
    public static NettyTlsProvider getNettyTlsProvider() {
        return nettyTlsProvider;
    }

    /**
     * DS method for setting the required netty service.
     *
     * @param bundle
     */
    @Reference(name = "nettyBundle")
    protected void setNettyBundle(NettyFramework bundle) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting nettyBundle " + bundle);
        nettyBundle = bundle;
    }

    /**
     * This is a required static reference, this won't
     * be called until the component has been deactivated
     *
     * @param bundle NettyBundle instance to unset
     */
    protected void unsetNettyBundle(NettyFramework bundle) {
    }

    /**
     * @return ChannelFramework associated with the CHFWBundle service.
     */
    public static NettyFramework getNettyFramework() {
        return nettyBundle;
    }

    private void performAction(Runnable action) {
        action.run();
    }

    private final Object actionLock = new Object() {
    };

    private final Runnable stopAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (actionLock) {
                // Always allow stops.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "EndpointAction: stopping chains " + WsocOutboundChain.this, wsocChain, wsocSecureChain);

                wsocChain.stop();
                wsocSecureChain.stop();
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
                    Tr.debug(this, tc, "EndpointAction: stopping https chain " + WsocOutboundChain.this, wsocSecureChain);

                wsocSecureChain.stop();
            }
        }
    };

    private final Runnable updateAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (actionLock) {
                // only try to update the chains if the endpoint is enabled/started and framework is good
                if (FrameworkState.isValid()) {
                    if (outboundCalled) {
                        wsocChain.update();
                        wsocSecureChain.update();
                    }
                }
            }
        }
    };

}