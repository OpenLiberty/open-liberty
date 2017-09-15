/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * NIO specific TCP channel instance.
 */
public class NioTCPChannel extends TCPChannel {

    private static WorkQueueManager workQueueManager = null;

    private static final TraceComponent tc = Tr.register(NioTCPChannel.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     */
    public NioTCPChannel() {
        super();
    }

    public ChannelTermination setup(ChannelData runtimeConfig, TCPChannelConfiguration tcpConfig, TCPChannelFactory factory) throws ChannelException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setup");
        }

        super.setup(runtimeConfig, tcpConfig, factory);
        // create WorkQueueMgr if this is the first NonBlocking Channel that
        // is being created.

        if (workQueueManager == null) {
            workQueueManager = new WorkQueueManager();
        }

        if (!config.isInbound()) {
            connectionManager = new ConnectionManager(this, workQueueManager);
        }

        workQueueManager.startSelectors(config.isInbound());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setup");
        }
        return getWorkQueueManager();
    }

    /**
     * Returns the WorkQueueManager reference.
     * 
     * @return WorkQueueManager
     */
    protected WorkQueueManager getWorkQueueManager() {
        return workQueueManager;
    }

    // LIDB3618-2 add method
    public SocketIOChannel createOutboundSocketIOChannel() throws IOException {
        SocketChannel channel = SocketChannel.open();
        Socket socket = channel.socket();
        return NioSocketIOChannel.createIOChannel(socket, this);
    }

    public SocketIOChannel createInboundSocketIOChannel(SocketChannel sc) {
        return NioSocketIOChannel.createIOChannel(sc.socket(), this);
    }

    public TCPReadRequestContextImpl createReadInterface(TCPConnLink connLink) {
        return new NioTCPReadRequestContextImpl(connLink);
    }

    public TCPWriteRequestContextImpl createWriteInterface(TCPConnLink connLink) {
        return new NioTCPWriteRequestContextImpl(connLink);
    }

}
