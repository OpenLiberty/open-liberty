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
import java.util.Hashtable;

import com.ibm.io.async.AsyncChannelGroup;
import com.ibm.io.async.AsyncException;
import com.ibm.io.async.AsyncLibrary;
import com.ibm.io.async.AsyncSocketChannel;
import com.ibm.io.async.IAsyncProvider;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * TCP channel class that handles AIO logic.
 */
public class AioTCPChannel extends TCPChannel implements ChannelTermination {

    private AsyncChannelGroup asyncChannelGroup;
    private static Hashtable<String, AsyncChannelGroup> groups = new Hashtable<String, AsyncChannelGroup>();
    private static AioReadCompletionListener aioReadCompletionListener = null;
    private static AioWriteCompletionListener aioWriteCompletionListener = null;
    private static boolean jitSupportedByNative;

    private static final TraceComponent tc = Tr.register(AioTCPChannel.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    private static AioWorkQueueManager wqm = null;

    /**
     * Constructor.
     */
    public AioTCPChannel() {
        super();
    }

    /*
     * @see
     * com.ibm.ws.tcpchannel.internal.TCPChannel#setup(com.ibm.websphere.channelfw
     * .ChannelData, com.ibm.ws.tcpchannel.internal.TCPChannelConfiguration,
     * com.ibm.ws.tcpchannel.internal.TCPChannelFactory)
     */
    public ChannelTermination setup(ChannelData chanData, TCPChannelConfiguration oTCPChannelConfig, TCPChannelFactory _f) throws ChannelException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setup");
        }
        super.setup(chanData, oTCPChannelConfig, _f);

        // try to load the AsyncLibrary. It will throw an exception if it can't load
        try {
            IAsyncProvider provider = AsyncLibrary.createInstance();

            if (getConfig().getAllocateBuffersDirect()) {
                jitSupportedByNative = provider.hasCapability(IAsyncProvider.CAP_JIT_BUFFERS);
            } else {
                jitSupportedByNative = false;
            }
        } catch (AsyncException ae) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "AioTCPChannel couldn't load native AIO library: " + ae.getMessage());
            }
            throw new ChannelException(ae);
        }

        if (!getConfig().isInbound()) {
            boolean startSelectors = false;
            if (wqm == null) {
                wqm = new AioWorkQueueManager();
                startSelectors = true;
            }
            super.connectionManager = new ConnectionManager(this, wqm);
            if (startSelectors) {
                wqm.startSelectors(false);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setup");
        }
        return this;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#init()
     */
    public void init() throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "init");
        }
        super.init();

        if (!getConfig().isInbound()) {
            try {
                this.asyncChannelGroup = findOrCreateACG();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Created completion port for outbound connections, completionPort = " + this.asyncChannelGroup.getCompletionPort());
                }
            } catch (AsyncException ae) {
                ChannelException ce = new ChannelException("Error creating async channel group ");
                ce.initCause(ae);
                throw ce;
            }
        }
        // create AIO CompletionListeners
        aioReadCompletionListener = new AioReadCompletionListener();
        aioWriteCompletionListener = new AioWriteCompletionListener();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "init");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#start()
     */
    public void start() throws ChannelException {
        super.start();
        getAsyncChannelGroup().activate();
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelTermination#terminate()
     */
    public void terminate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "terminate");
        }

        AsyncLibrary.shutdown();
        groups.clear();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "terminate");
        }
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.TCPChannel#createEndPoint()
     */
    public TCPPort createEndPoint() throws ChannelException {
        TCPPort tcpPort = super.createEndPoint();
        try {
            this.asyncChannelGroup = findOrCreateACG();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "AioTCPChannel created completion port for inbound connections on host " + getConfig().getHostname() + ", port " + getConfig().getPort()
                             + ", completionPort = " + this.asyncChannelGroup.getCompletionPort());
            }

        } catch (AsyncException ae) {
            ChannelException ce = new ChannelException("Error creating async channel group ");
            ce.initCause(ae);
            throw ce;
        }
        return tcpPort;
    }

    private AsyncChannelGroup findOrCreateACG() throws AsyncException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "findOrCreateACG");
        }

        String groupName = getConfig().getWorkGroupName();
        AsyncChannelGroup group = groups.get(groupName);
        if (group == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "create new AsyncChannelGroup: " + groupName);
            }
            group = new AsyncChannelGroup(groupName);
            groups.put(groupName, group);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "findOrCreateACG");
        }
        return group;
    }

    /*
     * @see
     * com.ibm.ws.tcpchannel.internal.TCPChannel#createReadInterface(com.ibm.ws
     * .tcpchannel.internal.TCPConnLink)
     */
    public TCPReadRequestContextImpl createReadInterface(TCPConnLink connLink) {
        return new AioTCPReadRequestContextImpl(connLink);
    }

    /*
     * @see
     * com.ibm.ws.tcpchannel.internal.TCPChannel#createWriteInterface(com.ibm.
     * ws.tcpchannel.internal.TCPConnLink)
     */
    public TCPWriteRequestContextImpl createWriteInterface(TCPConnLink connLink) {
        return new AioTCPWriteRequestContextImpl(connLink);
    }

    /**
     * @return AioReadCompletionListener
     */
    public static AioReadCompletionListener getAioReadCompletionListener() {
        return aioReadCompletionListener;
    }

    /**
     * @return AioWriteCompletionListener
     */
    public static AioWriteCompletionListener getAioWriteCompletionListener() {
        return aioWriteCompletionListener;
    }

    /**
     * Check whether the native AIO library reported that it supports the
     * use of JIT buffers.
     * 
     * @return boolean
     */
    public static boolean getJitSupportedByNative() {
        return jitSupportedByNative;
    }

    /*
     * @see
     * com.ibm.ws.tcpchannel.internal.TCPChannel#createOutboundSocketIOChannel()
     */
    public SocketIOChannel createOutboundSocketIOChannel() throws IOException {
        AsyncSocketChannel achannel = AsyncSocketChannel.open(getAsyncChannelGroup());
        Socket socket = achannel.socket();
        return AioSocketIOChannel.createIOChannel(socket, achannel, this);
    }

    /*
     * @see
     * com.ibm.ws.tcpchannel.internal.TCPChannel#createInboundSocketIOChannel(
     * java.nio.channels.SocketChannel)
     */
    public SocketIOChannel createInboundSocketIOChannel(SocketChannel sc) throws IOException {
        AsyncSocketChannel asc = new AsyncSocketChannel(sc, getAsyncChannelGroup());
        return AioSocketIOChannel.createIOChannel(sc.socket(), asc, this);
    }

    /**
     * Access the AIO group that this channel belongs to.
     * 
     * @return AsyncChannelGroup
     */
    protected AsyncChannelGroup getAsyncChannelGroup() {
        return this.asyncChannelGroup;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.TCPChannel#dumpStatistics()
     */
    protected void dumpStatistics() {
        super.dumpStatistics();
        this.asyncChannelGroup.dumpStatistics();
    }
}
