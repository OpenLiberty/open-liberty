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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.OutboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.RetryableChannelException;
import com.ibm.wsspi.connmgmt.ConnectionHandle;
import com.ibm.wsspi.connmgmt.ConnectionType;
import com.ibm.wsspi.tcpchannel.TCPConfigConstants;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Basic TCP channel class.
 */
@SuppressWarnings("unchecked")
public abstract class TCPChannel implements InboundChannel, OutboundChannel, FFDCSelfIntrospectable {

    volatile protected static NBAccept acceptReqProcessor = null;

    private String channelName = null;
    protected String externalName = null;
    private ChannelData channelData;
    protected TCPChannelConfiguration config;
    protected ConnectionManager connectionManager = null;
    protected VirtualConnectionFactory vcFactory = null;
    private TCPPort endPoint = null;
    private DiscriminationProcess discriminationProcess = null;
    private long lastConnExceededTime = 0;
    private AccessLists alists;

    private final static int SIZE_IN_USE = 128;
    private final Queue<TCPConnLink>[] inUse = new ConcurrentLinkedQueue[SIZE_IN_USE];
    private final AtomicInteger inUseIndex = new AtomicInteger(SIZE_IN_USE);

    protected volatile boolean stopFlag = true;
    private boolean preparingToStop = false;
    private String displayableHostName = null;
    private String chainName = null;

    private static final TraceComponent tc = Tr.register(TCPChannel.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    protected TCPChannelFactory channelFactory = null;

    private int connectionCount = 0; // inbound connection count
    private final Object connectionCountSync = new Object() {
    }; // sync object for above counter

    protected StatisticsLogger statLogger = null;
    protected final AtomicLong totalSyncReads = new AtomicLong(0);
    protected final AtomicLong totalAsyncReads = new AtomicLong(0);
    protected final AtomicLong totalAsyncReadRetries = new AtomicLong(0);
    protected final AtomicLong totalPartialAsyncReads = new AtomicLong(0);
    protected final AtomicLong totalPartialSyncReads = new AtomicLong(0);
    protected final AtomicLong totalSyncWrites = new AtomicLong(0);
    protected final AtomicLong totalAsyncWrites = new AtomicLong(0);
    protected final AtomicLong totalAsyncWriteRetries = new AtomicLong(0);
    protected final AtomicLong totalPartialAsyncWrites = new AtomicLong(0);
    protected final AtomicLong totalPartialSyncWrites = new AtomicLong(0);
    protected final AtomicLong totalConnections = new AtomicLong(0);
    protected final AtomicLong maxConcurrentConnections = new AtomicLong(0);

    /**
     * Constructor.
     */
    public TCPChannel() {
        // nothing to do here
    }

    /**
     * Initialize this channel.
     *
     * @param runtimeConfig
     * @param tcpConfig
     * @throws ChannelException
     */
    public void setup(ChannelData runtimeConfig, TCPChannelConfiguration tcpConfig) throws ChannelException {
        setup(runtimeConfig, tcpConfig, null);
    }

    /**
     * Initialize this channel.
     *
     * @param runtimeConfig
     * @param tcpConfig
     * @param factory
     * @return ChannelTermination
     * @throws ChannelException
     */
    public ChannelTermination setup(ChannelData runtimeConfig, TCPChannelConfiguration tcpConfig, TCPChannelFactory factory) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setup");
        }

        Map propertyBag = runtimeConfig.getPropertyBag();
        this.chainName = (String) propertyBag.get(ChannelFrameworkConstants.CHAIN_NAME_KEY);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getting from PropertyBag chain name of: " + chainName);
        }

        this.channelFactory = factory;
        this.channelData = runtimeConfig;
        this.channelName = runtimeConfig.getName();
        this.externalName = runtimeConfig.getExternalName();
        this.config = tcpConfig;

        for (int i = 0; i < this.inUse.length; i++) {
            this.inUse[i] = new ConcurrentLinkedQueue<TCPConnLink>();
        }

        this.vcFactory = ChannelFrameworkFactory.getChannelFramework().getInboundVCFactory();

        this.alists = AccessLists.getInstance(this.config);

        if (this.config.isInbound() && acceptReqProcessor == null) {
            acceptReqProcessor = new NBAccept(this.config);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setup");
        }
        return null;
    }

    protected AccessLists getAccessLists() {
        return this.alists;
    }

    protected boolean getStopFlag() {
        return this.stopFlag;
    }

    protected String getDisplayableHostName() {
        return this.displayableHostName;
    }

    protected void setDisplayableHostName(String name) {
        this.displayableHostName = name;
    }

    protected void decrementConnectionCount() {
        synchronized (this.connectionCountSync) {
            this.connectionCount--;
        }
    }

    protected void incrementConnectionCount() {
        synchronized (this.connectionCountSync) {
            this.connectionCount++;
        }
        if (getConfig().getDumpStatsInterval() > 0) {
            this.totalConnections.incrementAndGet();
            long oldMax = this.maxConcurrentConnections.get();
            while (this.connectionCount > oldMax) {
                this.maxConcurrentConnections.compareAndSet(oldMax, this.connectionCount);
                oldMax = this.maxConcurrentConnections.get();
            }
        }
    }

    protected int getInboundConnectionCount() {
        return this.connectionCount;
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminatoryType()
     */
    @Override
    public Class<?> getDiscriminatoryType() {
        return WsByteBuffer.class;
    }

    /**
     * Access the configuration for the channel.
     *
     * @return TCPChannelConfiguration
     */
    public TCPChannelConfiguration getConfig() {
        return this.config;
    }

    protected ConnectionManager getConnMgr() {
        return this.connectionManager;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getConnectionLink(VirtualConnection)
     */
    @Override
    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getConnectionLink");
        }

        // add this new connection link to the next in-use list based on
        // the atomic index, the extra add/mod handles the negative values
        int index = ((this.inUseIndex.getAndIncrement() % SIZE_IN_USE) + SIZE_IN_USE) % SIZE_IN_USE;

        TCPConnLink connLink = new TCPConnLink(vc, this, this.config, index);
        this.inUse[index].add(connLink);

        // assign default ConnectionType and unique ConnectionHandle to new inbound
        // connection
        // ConnectionType may seem redundant, but is used on some platforms to
        // identify
        // more than just inbound/outbound flow.
        ConnectionType.setDefaultVCConnectionType(vc);
        ConnectionHandle.getConnectionHandle(vc);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getConnectionLink: " + connLink);
        }
        return connLink;
    }

    abstract protected TCPReadRequestContextImpl createReadInterface(TCPConnLink connLink);

    abstract protected TCPWriteRequestContextImpl createWriteInterface(TCPConnLink connLink);

    /*
     * @see com.ibm.wsspi.channelfw.Channel#start()
     */
    @Override
    public void start() throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "start");
        }

        if (this.stopFlag) {
            // only start once
            this.stopFlag = false;
            if (this.config.isInbound()) {
                // Socket is already open, just need to start accepting connections
                try {
                    // PK60924 - check for a restart path requiring the re-init
                    if (null == this.endPoint.getServerSocket()) {
                        initializePort();
                    }
                    acceptReqProcessor.registerPort(this.endPoint);
                    this.preparingToStop = false;

                    // below code has been superceded by the split up of the port listening into a two step process during startup. With the
                    // port opening not happening until the end of server startup.
                    //String IPvType = "IPv4";
                    //if (this.endPoint.getServerSocket().getInetAddress() instanceof Inet6Address) {
                    //    IPvType = "IPv6";
                    //}

                    //if (this.config.getHostname() == null) {
                    //    this.displayableHostName = "*  (" + IPvType + ")";
                    //} else {
                    //
                    //    this.displayableHostName = this.endPoint.getServerSocket().getInetAddress().getHostName() + "  (" + IPvType + ": "
                    //                                   + this.endPoint.getServerSocket().getInetAddress().getHostAddress() + ")";
                    //}
                    //
                    //Tr.info(tc, TCPChannelMessageConstants.TCP_CHANNEL_STARTED,
                    //        new Object[] { getExternalName(), this.displayableHostName, String.valueOf(this.endPoint.getListenPort()) });

                } catch (IOException e) {
                    FFDCFilter.processException(e, getClass().getName() + ".start", "100", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "TCP Channel: " + getExternalName() + "- Problem occurred while starting TCP Channel: " + e.getMessage());
                    }
                    ChannelException x = new ChannelException("TCP Channel: " + getExternalName() + "- Problem occurred while starting channel: " + e.getMessage());
                    // Adjust flag so follow up attempt is possible.
                    this.stopFlag = true;
                    throw x;
                }
            }
            if (this.config.getDumpStatsInterval() > 0) {
                createStatisticsThread();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "start");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#init()
     */
    @Override
    public void init() throws ChannelException {

        if (this.config.isInbound()) {
            // Customize the TCPChannel configuration object so that it knows
            // what port to use for this chain.
            this.endPoint = createEndPoint();
            initializePort();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " listening port: " + this.endPoint.getListenPort());
            }
        }
    }

    /**
     * Initialize the endpoint listening socket.
     *
     * @throws ChannelException
     */
    private void initializePort() throws ChannelException {
        try {
            this.endPoint.initServerSocket();
        } catch (IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "TCP Channel: " + getExternalName() + "- Problem occurred while initializing TCP Channel: " + ioe.getMessage());
            }
            throw new ChannelException("TCP Channel: " + getExternalName() + "- Problem occurred while starting channel: " + ioe.getMessage());
        } catch (RetryableChannelException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "TCP Channel: " + getExternalName() + "- Problem occurred while starting TCP Channel: " + e.getMessage());
            }
            throw e;
        }

        // add property to config to provide actual port used (could be ephemeral
        // port if '0' passed in)
        this.channelData.getPropertyBag().put(TCPConfigConstants.LISTENING_PORT, String.valueOf(this.endPoint.getListenPort()));
    }

    /**
     * Create the TCP end point for this channel.
     *
     * @return TCPPort
     * @throws ChannelException
     */
    public TCPPort createEndPoint() throws ChannelException {
        return new TCPPort(this, this.vcFactory);
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#destroy()
     */
    @Override
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroy " + getExternalName());
        }
        // Destroy the server socket
        if (this.endPoint != null) {
            this.endPoint.destroyServerSocket();
        }
        // disconnect from the factory
        if (null != this.channelFactory) {
            this.channelFactory.removeChannel(this.channelName);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminator()
     */
    @Override
    public Discriminator getDiscriminator() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "getDiscriminator called erroneously on TCPChannel");
        }
        return null;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#stop(long)
     */
    @Override
    public void stop(long millisec) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "stop (" + millisec + ") " + getExternalName());
        }
        // Stop accepting new connections on the inbound channels
        if (!this.preparingToStop && acceptReqProcessor != null && this.config.isInbound()) {
            acceptReqProcessor.removePort(this.endPoint);
            // PK60924 - stop the listening port now
            this.endPoint.destroyServerSocket();
            Tr.info(tc, TCPChannelMessageConstants.TCP_CHANNEL_STOPPED, getExternalName(), this.displayableHostName, String.valueOf(this.endPoint.getListenPort()));
            this.preparingToStop = true;
            // d247139 don't null acceptReqProcessor here,
            // need it for processing a subsequent "start"

        }

        // only stop the channel if millisec is 0, otherwise ignore.
        if (millisec == 0) {
            this.preparingToStop = false;
            this.stopFlag = true; // don't allow any further processing
            // destroy all the "in use" TCPConnLinks. This should close all
            // the sockets held by these connections.
            destroyConnLinks();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "stop");
        }
    }

    /**
     * Returns the name.
     *
     * @return String
     */
    @Override
    public String getName() {
        return this.channelName;
    }

    /**
     * Returns the appSideClass.
     *
     * @return Class
     */
    @Override
    public Class<?> getApplicationInterface() {
        return TCPConnectionContext.class;
    }

    /**
     * Returns null because there will never be channels on the
     * device side of this channel.
     *
     * @return Class
     */
    @Override
    public Class<?> getDeviceInterface() {
        return null;
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminationProcess()
     */
    @Override
    public DiscriminationProcess getDiscriminationProcess() {
        return this.discriminationProcess;
    }

    /*
     * @seecom.ibm.wsspi.channelfw.InboundChannel#setDiscriminationProcess(
     * DiscriminationProcess)
     */
    @Override
    public void setDiscriminationProcess(DiscriminationProcess dp) {
        this.discriminationProcess = dp;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#update(ChannelData)
     */
    @Override
    public void update(ChannelData cc) {
        synchronized (this) {
            // can't do two updates at the same time
            if (this.config.checkAndSetValues(cc)) {
                this.alists = AccessLists.getInstance(this.config);
            }
        }
    }

    @Override
    public String[] introspectSelf() {
        String[] configFFDC = getConfig().introspectSelf();
        String[] rc = new String[1 + configFFDC.length];
        rc[0] = "TCP Channel: " + getExternalName();
        System.arraycopy(configFFDC, 0, rc, 1, configFFDC.length);
        return rc;
    }

    /**
     * Use this method for coherency checking of address types for connect and
     * connectAsynch.
     * This method will return the type of address object this channel plans to
     * pass down towards
     * the device side.
     *
     * @return Class
     */
    @Override
    public Class<?> getDeviceAddress() {
        throw new IllegalStateException("Not implemented and should not be");
    }

    /**
     * Use this method for coherency checking of address types for connect and
     * connectAsynch.
     * This method will return the type of address objects this channel plans have
     * passed to it
     * from the application side. A channel may accept more than one address
     * object type but
     * passes only one down to the channels below.
     *
     * @return Class[]
     */
    @Override
    public Class<?>[] getApplicationAddress() {
        return new Class<?>[] { TCPConnectRequestContext.class };
    }

    /**
     * call the destroy on all the TCPConnLink objects related to
     * this TCPChannel which are currently "in use".
     *
     */
    private synchronized void destroyConnLinks() {

        // inUse queue is still open to modification
        // during this time. Returned iterator is a "weakly consistent"
        // I don't believe this has (yet) caused any issues.
        for (Queue<TCPConnLink> queue : this.inUse) {
            try {
                TCPConnLink tcl = queue.poll();
                while (tcl != null) {
                    tcl.close(tcl.getVirtualConnection(), null);
                    tcl = queue.poll();
                }
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName(), "destroyConnLinks", new Object[] { this });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error closing connection: " + t + " " + queue);
                }
            }

        }
    }

    protected void releaseConnectionLink(TCPConnLink conn, int index) {
        this.inUse[index].remove(conn);
    }

    /**
     * Access the factory to create connections.
     *
     * @return VirtualConnectionFactory
     */
    protected VirtualConnectionFactory getVcFactory() {
        return this.vcFactory;
    }

    /**
     * Query the external name of the channel.
     *
     * @return String
     */
    public String getExternalName() {
        return this.externalName;
    }

    /**
     * Returns the lastConnExceededTime.
     *
     * @return long
     */
    protected long getLastConnExceededTime() {
        return this.lastConnExceededTime;
    }

    /**
     * Sets the lastConnExceededTime.
     *
     * @param lastConnExceededTime
     *                                 The lastConnExceededTime to set
     */
    protected void setLastConnExceededTime(long lastConnExceededTime) {
        this.lastConnExceededTime = lastConnExceededTime;
    }

    private static boolean checkStartup = true;

    protected boolean verifyConnection(Socket socket) {

        if (config.getWaitToAccept() && checkStartup) {
            if (CHFWBundle.isServerCompletelyStarted() == false) {
                return false;
            } else {
                // once it has started or the waitToAccept tcp option is not set, we don't want to keep checking
                checkStartup = false;
            }
        }

        if (this.alists != null) {
            if (this.alists.accessDenied(socket.getInetAddress())) {
                return false;
            }
        }

        int maxSocketsToUse = this.config.getMaxOpenConnections();

        // see if we are maxed out on connections
        if (getInboundConnectionCount() >= maxSocketsToUse) {
            // notify every 10 minutes if max concurrent conns was hit
            long currentTime = System.currentTimeMillis();
            if (currentTime > (getLastConnExceededTime() + 600000L)) {
                Tr.warning(tc, TCPChannelMessageConstants.MAX_CONNS_EXCEEDED, getExternalName(), Integer.valueOf(maxSocketsToUse));
                setLastConnExceededTime(currentTime);
            }

            return false;
        }

        try {
            socket.setTcpNoDelay(this.config.getTcpNoDelay());

            if (this.config.getSoLinger() >= 0) {
                socket.setSoLinger(true, this.config.getSoLinger());
            } else {
                socket.setSoLinger(false, 0);
            }

            socket.setKeepAlive(this.config.getKeepAlive());

            if ((this.config.getSendBufferSize() >= TCPConfigConstants.SEND_BUFFER_SIZE_MIN) && (this.config.getSendBufferSize() <= TCPConfigConstants.SEND_BUFFER_SIZE_MAX)) {
                socket.setSendBufferSize(this.config.getSendBufferSize());
            }

        } catch (IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "IOException caught while configuring socket: " + ioe);
            }
            return false;
        }

        // made it this far, so we are good to go
        return true;
    }

    abstract protected SocketIOChannel createOutboundSocketIOChannel() throws IOException;

    abstract protected SocketIOChannel createInboundSocketIOChannel(SocketChannel sc) throws IOException;

// code needed for dumping statistics
    protected void createStatisticsThread() {
        this.statLogger = new StatisticsLogger();
        PrivilegedThreadStarter privThread = new PrivilegedThreadStarter();
        AccessController.doPrivileged(privThread);
    }

    protected void dumpStatistics() {
        if (getConfig().isInbound()) {
            System.out.println("Statistics for TCP inbound channel " + getExternalName() + " (port " + getConfig().getPort() + ")");
            System.out.println("   Total connections accepted: " + this.totalConnections);
        } else {
            System.out.println("Statistics for TCP outbound channel " + getExternalName());
            System.out.println("   Total connects processed: " + this.totalConnections);
        }

        System.out.println("   Maximum concurrent connections: " + this.maxConcurrentConnections);
        System.out.println("   Current connection count: " + this.connectionCount);
        System.out.println("   Total Async read requests: " + this.totalAsyncReads.get());
        System.out.println("   Total Async read retries: " + this.totalAsyncReadRetries.get());
        System.out.println("   Total Async read partial reads: " + this.totalPartialAsyncReads.get());
        System.out.println("   Total Sync read requests: " + this.totalSyncReads.get());
        System.out.println("   Total Sync read partial reads: " + this.totalPartialSyncReads.get());
        System.out.println("   Total Async write requests: " + this.totalAsyncWrites.get());
        System.out.println("   Total Async write retries: " + this.totalAsyncWriteRetries.get());
        System.out.println("   Total Async write partial writes: " + this.totalPartialAsyncWrites.get());
        System.out.println("   Total Sync write requests: " + this.totalSyncWrites.get());
        System.out.println("   Total Sync write partial writes: " + this.totalPartialSyncWrites.get());
    }

    class StatisticsLogger implements Runnable {
        /**
         * Constructor.
         */
        public StatisticsLogger() {
            // nothing
        }

        @Override
        public void run() {
            TCPChannel channel = TCPChannel.this;
            boolean interrupted = false;
            if (channel.getConfig().isInbound()) {
                System.out.println("Statistics logging for TCP inbound channel " + channel.externalName + " (port " + channel.getConfig().getPort() + ") is now on");
            } else {
                System.out.println("Statistics logging for TCP outbound channel " + channel.externalName + " is now on");
            }
            // loop until channel is stopped
            while (!channel.getStopFlag() && !interrupted) {
                try {
                    Thread.sleep(channel.getConfig().getDumpStatsInterval() * 1000L);
                } catch (InterruptedException ie) {
                    interrupted = true;
                }
                channel.dumpStatistics();
            }
            System.out.println(" stat thread exiting");
        }

    }

    class PrivilegedThreadStarter implements PrivilegedAction<Object> {
        /** Constructor */
        public PrivilegedThreadStarter() {
            // do nothing
        }

        @Override
        public Object run() {
            String threadName = "Statistics Logging Thread for: " + getExternalName();

            Thread t = new Thread(statLogger);
            t.setName(threadName);

            // all TCPChannel Thread should be daemon threads
            t.setDaemon(false);
            t.start();
            return null;
        }
    }

    /**
     * take down a chain. this was added for when the start code failed to open/bind to a port during startup. This is new with the split of the port
     * opening/listening code during startup.
     */
    public void takeDownChain() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "takeDownChain");
        }

        if (chainName == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "takeDownChain - chainName is null");
            }
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, " chainName is: " + chainName);
        }

        try {
            ChannelFramework cfw = ChannelFrameworkFactory.getChannelFramework();
            ChainData cd = cfw.getChain(chainName);

            if (cd == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "takeDownChain - ChainData is null");
                }
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "stopping chain");
            }
            cfw.stopChain(cd, 0L); // no timeout needed, chain has not really started yet so force stop it

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "destroying chain");
            }
            cfw.destroyChain(cd);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "removing chain");
            }
            cfw.removeChain(cd);

        } catch (Exception x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "takeDownChain caught exception: " + x);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "takeDownChain - method completed ok");
        }
    }

}
