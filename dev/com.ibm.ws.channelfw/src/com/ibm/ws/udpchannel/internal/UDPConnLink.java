/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.udpchannel.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.OutboundConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.OutboundConnectorLink;
import com.ibm.wsspi.udpchannel.UDPConfigConstants;
import com.ibm.wsspi.udpchannel.UDPContext;
import com.ibm.wsspi.udpchannel.UDPReadRequestContext;
import com.ibm.wsspi.udpchannel.UDPRequestContext;
import com.ibm.wsspi.udpchannel.UDPWriteRequestContext;

/**
 * UDP channel's connection specific link instance.
 * 
 * @author mjohnson
 */
public class UDPConnLink extends OutboundConnectorLink implements ConnectionLink, OutboundConnectionLink, UDPContext {

    private static final TraceComponent tc = Tr.register(UDPConnLink.class, UDPMessages.TR_GROUP, UDPMessages.TR_MSGS);

    private WorkQueueManager workQueueMgr = null;
    private UDPChannel udpChannel = null;
    private UDPChannelConfiguration cfg = null;
    private UDPNetworkLayer udpNetworkLayer = null;
    private UDPReadRequestContextImpl reader = null;
    private UDPWriteRequestContextImpl writer = null;

    /**
     * Constructor.
     * 
     * @param wqm
     * @param vc
     * @param channel
     * @param cfg
     */
    public UDPConnLink(WorkQueueManager wqm, VirtualConnection vc, UDPChannel channel, UDPChannelConfiguration cfg) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "UDPConnLink");
        }

        init(vc);
        this.workQueueMgr = wqm;
        this.udpChannel = channel;
        this.cfg = cfg;

        this.reader = new UDPReadRequestContextImpl(this, workQueueMgr);
        this.writer = new UDPWriteRequestContextImpl(this, workQueueMgr);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "UDPConnLink");
        }
    }

    /**
     * Common connect logic between sync and async connect requests.
     * 
     * @param _udpRequestContextObject
     * @throws IOException
     */
    private void connectCommon(Object _udpRequestContextObject) throws IOException {
        String localAddress = "*";
        int localPort = 0;
        Map<Object, Object> vcStateMap = getVirtualConnection().getStateMap();
        if (vcStateMap != null) {
            //
            // Size of the buffer the channel should use to read.
            //
            String value = (String) vcStateMap.get(UDPConfigConstants.CHANNEL_RCV_BUFF_SIZE);
            if (value != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, UDPConfigConstants.CHANNEL_RCV_BUFF_SIZE + " " + value);
                }
                cfg.setChannelReceiveBufferSize(Integer.parseInt(value));
            }
            //
            // Receive buffer size.
            //
            value = (String) vcStateMap.get(UDPConfigConstants.RCV_BUFF_SIZE);
            if (value != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, UDPConfigConstants.RCV_BUFF_SIZE + " " + value);
                }
                cfg.setReceiveBufferSize(Integer.parseInt(value));
            }
            //
            // Send buffer size
            //
            value = (String) vcStateMap.get(UDPConfigConstants.SEND_BUFF_SIZE);
            if (value != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, UDPConfigConstants.SEND_BUFF_SIZE + " " + value);
                }
                cfg.setSendBufferSize(Integer.parseInt(value));
            }
        }
        //
        // Allow for this to be null. If the requestContext is null, then just
        // allow The NetworkLayer to find the port to listen on.
        //
        if (_udpRequestContextObject != null) {
            final UDPRequestContext udpRequestContext = (UDPRequestContext) _udpRequestContextObject;
            final InetSocketAddress addr = udpRequestContext.getLocalAddress();
            localAddress = addr.getAddress().getHostAddress();
            localPort = addr.getPort();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "connect with local address: " + localAddress + " local port: " + localPort);
            }
        }

        udpNetworkLayer = new UDPNetworkLayer(udpChannel, workQueueMgr, localAddress, localPort);
        udpNetworkLayer.initDatagramSocket(getVirtualConnection());
        udpNetworkLayer.setConnLink(this);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.OutboundConnectionLink#connectAsynch(java.lang.
     * Object)
     */
    @Override
    public void connectAsynch(Object _udpRequestContextObject) {
        try {
            connectCommon(_udpRequestContextObject);
            getApplicationCallback().ready(vc);
        } catch (IOException e) {
            getApplicationCallback().destroy(e);
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.OutboundConnectionLink#connect(java.lang.Object)
     */
    @Override
    public void connect(Object _udpRequestContextObject) throws Exception {
        connectCommon(_udpRequestContextObject);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getChannelAccessor()
     */
    @Override
    public Object getChannelAccessor() {
        return this;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ConnectionReadyCallback#ready(com.ibm.wsspi.channelfw
     * .VirtualConnection)
     */
    @Override
    public void ready(VirtualConnection inVC) {
        // This should not be called because the UDPConnLink is always
        // ready since it is the first in the chain.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Ready should not be called");
        }
    }

    /*
     * @see com.ibm.wsspi.udpchannel.UDPContext#getReadInterface()
     */
    @Override
    public UDPReadRequestContext getReadInterface() {
        return this.reader;
    }

    /*
     * @see com.ibm.wsspi.udpchannel.UDPContext#getLocalAddress()
     */
    @Override
    public InetAddress getLocalAddress() {
        InetAddress address = null;

        if (udpNetworkLayer != null) {
            address = udpNetworkLayer.getDatagramSocket().getLocalAddress();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (address == null) {
                    Tr.debug(tc, "getLocalAddress == null");
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getLocalAddress; udpNetworkLayer == null");
            }
        }
        return address;
    }

    /*
     * @see com.ibm.wsspi.udpchannel.UDPContext#getLocalPort()
     */
    @Override
    public int getLocalPort() {
        int port = 0;

        if (udpNetworkLayer != null) {
            port = udpNetworkLayer.getDatagramSocket().getLocalPort();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (port == 0) {
                    Tr.debug(tc, "getLocalPort == 0");
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getLocalPort; udpNetworkLayer == null");
            }
        }
        return port;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.base.OutboundConnectorLink#destroy(java.lang.Exception
     * )
     */
    @Override
    public void destroy(Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy(exc)");
        }

        if (null != udpNetworkLayer) {
            workQueueMgr.removeChannel(udpNetworkLayer.getDatagramChannel());
            udpNetworkLayer.destroy();
            udpNetworkLayer = null;
        }

        if (udpChannel != null) {
            udpChannel.removeConnLink(this);
        }
        //
        // Cleanup anything else that needs to be cleaned up.
        //
        super.destroy(e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy(exc)");
        }
    }

    /*
     * @see com.ibm.wsspi.udpchannel.UDPContext#getWriteInterface()
     */
    @Override
    public UDPWriteRequestContext getWriteInterface() {
        return this.writer;
    }

    /**
     * Set the UDP network layer reference.
     * 
     * @param layer
     */
    public void setUDPNetworkLayer(UDPNetworkLayer layer) {
        this.udpNetworkLayer = layer;
    }

    /**
     * Access the current UDP network layer reference, null if not yet set.
     * 
     * @return UDPNetworkLayer
     */
    public UDPNetworkLayer getUDPNetworkLayer() {
        return this.udpNetworkLayer;
    }

}
