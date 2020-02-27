/*******************************************************************************
 * Copyright (c) 2005, 2006, 2020 IBM Corporation and others.
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
import java.io.EOFException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.tcpchannel.internal.ConnectionManager.ConnectInfo;
import com.ibm.ws.tcpchannel.internal.SocketIOChannel.IOResult;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * A simple implementation of a work queue policy.
 * This class keeps ahold of an array of worker threads and
 * round robins new requests between them.
 */
public class WorkQueueManager implements ChannelTermination, FFDCSelfIntrospectable {
    private static final TraceComponent tc = Tr.register(WorkQueueManager.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    protected int maxChannelSelectorsPerFlow = 200;

    protected SocketRWChannelSelector[] readInbound = null;
    protected SocketRWChannelSelector[] readOutbound = null;
    protected SocketRWChannelSelector[] writeInbound = null;
    protected SocketRWChannelSelector[] writeOutbound = null;
    protected ConnectChannelSelector[] connect = null;

    protected int[] readInboundCount = null;
    protected int[] readOutboundCount = null;
    protected int[] writeInboundCount = null;
    protected int[] writeOutboundCount = null;
    protected int[] connectCount = null;

    protected static final int CS_READ_INBOUND = 0;
    protected static final int CS_READ_OUTBOUND = 1;
    protected static final int CS_WRITE_INBOUND = 2;
    protected static final int CS_WRITE_OUTBOUND = 3;
    protected static final int CS_CONNECTOR = 4;

    protected static final int CS_OK = 0;
    // All CS values which are not OK, must be less than CS_OK
    protected static final int CS_NULL = -1;
    protected static final int CS_DELETE_IN_PROGRESS = -2;

    protected final Object findOpenIndexSync = new Object() {};
    protected final Object shutdownSync = new Object() {};

    protected int maxKeysPerSelector;

    // Some variables and methods have been scoped at the "protected" level rather
    // than
    // "private" to allow extended classes to access them.
    protected boolean checkCancel;
    private final boolean combineSelectors;
    protected int wakeupOption;
    private final ThreadGroup tGroup;

    /**
     * Constructor.
     */
    protected WorkQueueManager() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "WorkQueueManager");
        }

        this.maxKeysPerSelector = TCPFactoryConfiguration.getMaxKeysPerSelector();
        this.checkCancel = TCPFactoryConfiguration.getCancelKeyOnClose();
        this.wakeupOption = TCPFactoryConfiguration.getSelectorWakeup();
        this.combineSelectors = TCPFactoryConfiguration.getCombineSelectors();
        this.tGroup = new PrivGroupCreation("TCP WQM").run();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "WorkQueueManager");
        }
    }

    protected void startSelectors(boolean inBound) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startSelectors " + inBound);
        }

        // This routine is called when the channel gets created via the
        // TCPChannelFactory.createChannel method. That method does not need
        // to be thread safe, since the framework will not have multiple calls
        // outstanding. Therefore we don't have to be thread safe looking at
        // these objects to determine if we have created them or not.

        // check if we can leave quickly
        if ((!combineSelectors && readInbound != null && readOutbound != null) || (combineSelectors && readInbound != null && connect != null)) {
            return;
        }

        try {
            // Start Read Inbound if 1) the channel is inbound and this is the first
            // time it has been called,
            // or 2) the channel is outbound, this is the first time it has been
            // called, and
            // the combineSelector option is true.
            // Start Write Inbound if 1) read inbound was started
            // Start Read Outbound if 1) the channel is outbound, first time,
            // combineSelector is false.
            // Start Write Outbound if 1) read outbound was started.
            // Start Connect if 1) the channel is outbound and first time.

            if ((inBound && readInbound == null) || (!inBound && readInbound == null && combineSelectors)) {

                readInbound = new SocketRWChannelSelector[maxChannelSelectorsPerFlow];
                writeInbound = new SocketRWChannelSelector[maxChannelSelectorsPerFlow];

                readInboundCount = new int[maxChannelSelectorsPerFlow];
                writeInboundCount = new int[maxChannelSelectorsPerFlow];

                // initialize all CS count values to mark that CSs are null
                for (int i = 1; i < maxChannelSelectorsPerFlow; i++) {
                    readInboundCount[i] = CS_NULL;
                    writeInboundCount[i] = CS_NULL;
                }

                readInbound[0] = new SocketRWChannelSelector(wakeupOption, this, 0, CS_READ_INBOUND, checkCancel);
                // to start a selector thread with privilege credentials for J2EE
                // security, do this.
                createNewThread(readInbound[0], CS_READ_INBOUND, 1);
                // selector is created and initialized when instantiated, so once start
                // returns we can send work to it
                readInboundCount[0] = CS_OK;

                writeInbound[0] = new SocketRWChannelSelector(wakeupOption, this, 0, CS_WRITE_INBOUND, checkCancel);
                createNewThread(writeInbound[0], CS_WRITE_INBOUND, 1);
                writeInboundCount[0] = CS_OK;

                if (!inBound) {
                    // for outbound so start Connect

                    connect = new ConnectChannelSelector[maxChannelSelectorsPerFlow];
                    connectCount = new int[maxChannelSelectorsPerFlow];
                    for (int i = 1; i < maxChannelSelectorsPerFlow; i++) {
                        connectCount[i] = CS_NULL;
                    }

                    connect[0] = new ConnectChannelSelector(this, 0, CS_CONNECTOR);
                    createNewThread(connect[0], CS_CONNECTOR, 1);
                    connectCount[0] = CS_OK;
                }

            } else if (!combineSelectors && !inBound && readOutbound == null) {

                readOutbound = new SocketRWChannelSelector[maxChannelSelectorsPerFlow];
                writeOutbound = new SocketRWChannelSelector[maxChannelSelectorsPerFlow];
                connect = new ConnectChannelSelector[maxChannelSelectorsPerFlow];

                readOutboundCount = new int[maxChannelSelectorsPerFlow];
                writeOutboundCount = new int[maxChannelSelectorsPerFlow];
                connectCount = new int[maxChannelSelectorsPerFlow];

                readOutbound[0] = new SocketRWChannelSelector(wakeupOption, this, 0, CS_READ_OUTBOUND, checkCancel);
                createNewThread(readOutbound[0], CS_READ_OUTBOUND, 1);
                writeOutbound[0] = new SocketRWChannelSelector(wakeupOption, this, 0, CS_WRITE_OUTBOUND, checkCancel);
                createNewThread(writeOutbound[0], CS_WRITE_OUTBOUND, 1);

                connect[0] = new ConnectChannelSelector(this, 0, CS_CONNECTOR);
                createNewThread(connect[0], CS_CONNECTOR, 1);
                for (int i = 1; i < maxChannelSelectorsPerFlow; i++) {
                    readOutboundCount[i] = CS_NULL;
                    writeOutboundCount[i] = CS_NULL;
                    connectCount[i] = CS_NULL;
                }
                readOutboundCount[0] = CS_OK;
                writeOutboundCount[0] = CS_OK;
                connectCount[0] = CS_OK;

            } else if (combineSelectors && !inBound && connect == null) {

                connect = new ConnectChannelSelector[maxChannelSelectorsPerFlow]; // 269309
                // add
                connectCount = new int[maxChannelSelectorsPerFlow]; // 269309 add

                connect[0] = new ConnectChannelSelector(this, 0, CS_CONNECTOR);
                createNewThread(connect[0], CS_CONNECTOR, 1);

                for (int i = 0; i < maxChannelSelectorsPerFlow; i++) {
                    connectCount[i] = CS_NULL;
                }
                connectCount[0] = CS_OK;
            }

        } catch (IOException ioe) {
            FFDCFilter.processException(ioe, getClass().getName(), "100", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Caught IOException while trying to create selector: " + ioe);

            ChannelException ce = new ChannelException("Unable to start the TCP Channel", ioe);
            throw ce;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startSelectors");
        }
    }

    protected void updateCount(int index, int value, int channelType) {
        // should only be called by the selector threads
        if (channelType == CS_READ_INBOUND) {
            this.readInboundCount[index] = value;
        } else if (channelType == CS_READ_OUTBOUND) {
            this.readOutboundCount[index] = value;
        } else if (channelType == CS_WRITE_INBOUND) {
            this.writeInboundCount[index] = value;
        } else if (channelType == CS_WRITE_OUTBOUND) {
            this.writeOutboundCount[index] = value;
        } else if (channelType == CS_CONNECTOR) {
            this.connectCount[index] = value;
        }
    }

    /**
     * Introspect this object for FFDC output.
     *
     * @return List<String>
     */
    public List<String> introspect() {
        List<String> rc = new LinkedList<String>();
        rc.add(dumpChannelSelectorCounts(this.readInboundCount, "Read Inbound"));
        rc.add(dumpChannelSelectorCounts(this.readOutboundCount, "Read Outbound"));
        rc.add(dumpChannelSelectorCounts(this.writeInboundCount, "Write Inbound"));
        rc.add(dumpChannelSelectorCounts(this.writeOutboundCount, "Write Outbound"));
        rc.add(dumpChannelSelectorCounts(this.connectCount, "Connect"));
        return rc;
    }

    /*
     * @see com.ibm.ws.ffdc.FFDCSelfIntrospectable#introspectSelf()
     */
    @Override
    public String[] introspectSelf() {
        List<String> rc = introspect();
        return rc.toArray(new String[rc.size()]);
    }

    private String dumpChannelSelectorCounts(int[] channelCounts, String channelType) {
        StringBuilder sb = new StringBuilder();
        sb.append(channelType);
        if (null != channelCounts) {
            for (int i = 0; i < channelCounts.length; i++) {
                if (channelCounts[i] != CS_NULL) {
                    sb.append(' ');
                    sb.append(i).append(':').append(channelCounts[i]);
                }
            }
        } else {
            sb.append(" none");
        }
        return sb.toString();
    }

    /**
     * Processes the request. If the request is already associated with
     * a work queue - send it there. Otherwise round robin requests
     * amongst our set of queues.
     *
     * @param req
     * @param options
     * @return VirtualConnections
     */
    protected VirtualConnection processWork(TCPBaseRequestContext req, int options) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processWork");
        }

        TCPConnLink conn = req.getTCPConnLink();
        VirtualConnection vc = null;

        if (options != 1) {
            // initialize the action to false, set to true if we do the allocate
            if (req.isRequestTypeRead()) {
                ((TCPReadRequestContextImpl) req).setJITAllocateAction(false);
            }
        }

        if (attemptIO(req, false)) {
            vc = conn.getVirtualConnection();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processWork");
        }
        return vc;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelTermination#terminate()
     */
    @Override
    public void terminate() {
        shutdown();
    }

    /**
     *
     */
    protected void shutdown() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "shutdown");
        }
        if (readInboundCount != null) {
            shutdownFlow(readInboundCount, readInbound);
        }
        if (readOutboundCount != null) {
            shutdownFlow(readOutboundCount, readOutbound);
        }
        if (writeInboundCount != null) {
            shutdownFlow(writeInboundCount, writeInbound);
        }
        if (writeOutboundCount != null) {
            shutdownFlow(writeOutboundCount, writeOutbound);
        }
        if (connectCount != null) {
            shutdownFlow(connectCount, connect);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "shutdown");
        }
    }

    /**
     *
     * @param channelCounts
     * @param CS
     */
    private void shutdownFlow(int[] channelCounts, ChannelSelector[] CS) {
        // For each of the ChannelSelectors which are not already "closed", tell
        // them to terminate.
        for (int i = 0; i < maxChannelSelectorsPerFlow; i++) {
            synchronized (shutdownSync) {
                if (channelCounts[i] != CS_NULL) {
                    CS[i].shutDown();
                }
            }
        }
    }

    private void queueIO(TCPBaseRequestContext req) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "queueIO");
        }
        final TCPConnLink conn = req.getTCPConnLink();
        final ChannelSelector channelSelector;

        if (req.isRequestTypeRead()) {
            channelSelector = ((NioSocketIOChannel) conn.getSocketIOChannel()).getChannelSelectorRead();
        } else {
            channelSelector = ((NioSocketIOChannel) conn.getSocketIOChannel()).getChannelSelectorWrite();
        }
        if (channelSelector != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Adding work to selector");
            }
            channelSelector.addWork(req);
        } else {
            // Figure out which ChannelSelector to queue this work
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Finding selector for IO");
            }

            if (req.isRequestTypeRead()) {
                // Read IO, determine if this req is inbound or outbound.
                if (conn.getConfig().isInbound() || combineSelectors) {
                    moveIntoPosition(readInboundCount, readInbound, req, CS_READ_INBOUND);
                } else {
                    moveIntoPosition(readOutboundCount, readOutbound, req, CS_READ_OUTBOUND);
                }
            } else {
                // Write IO, determine if this req is inbound or outbound.
                if (conn.getConfig().isInbound() || combineSelectors) {
                    moveIntoPosition(writeInboundCount, writeInbound, req, CS_WRITE_INBOUND);
                } else {
                    moveIntoPosition(writeOutboundCount, writeOutbound, req, CS_WRITE_OUTBOUND);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "queueIO");
        }
    }

    protected void moveIntoPosition(int[] channelCounts, ChannelSelector[] CS, Object req, int channelType) throws IOException {
        for (int i = 0; i < maxChannelSelectorsPerFlow; i++) {
            // Look for a CS that has room for another channel
            if ((channelCounts[i] >= CS_OK) && (channelCounts[i] < maxKeysPerSelector)) {
                CS[i].addWork(req);
                return;
            }
        }

        // All active selectors are full or busy, so create a new CS
        synchronized (findOpenIndexSync) {
            int nextOpen = 0;
            for (; nextOpen < maxChannelSelectorsPerFlow; nextOpen++) {
                if (channelCounts[nextOpen] == CS_NULL) {
                    break;
                }
            }

            if (nextOpen < maxChannelSelectorsPerFlow) {
                // instantiate and start a new CS
                try {
                    if (channelType == CS_CONNECTOR) {
                        CS[nextOpen] = new ConnectChannelSelector(this, nextOpen, CS_CONNECTOR);
                    } else {
                        CS[nextOpen] = new SocketRWChannelSelector(wakeupOption, this, nextOpen, channelType, checkCancel);
                    }
                } catch (IOException x) {
                    FFDCFilter.processException(x, getClass().getName(), "120", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught IOException creating new selector; " + x);
                    }
                    throw x;
                }

                // to start a selector thread with privilege credentials for J2EE
                // security, do this.
                createNewThread(CS[nextOpen], channelType, nextOpen + 1);

                channelCounts[nextOpen] = CS_OK;
                CS[nextOpen].addWork(req);
            } else {

                String sChannelType = "";
                if (channelType == CS_READ_INBOUND) {
                    sChannelType = "readInbound";
                } else if (channelType == CS_READ_OUTBOUND) {
                    sChannelType = "readOutbound";
                } else if (channelType == CS_WRITE_INBOUND) {
                    sChannelType = "writeInbound";
                } else if (channelType == CS_WRITE_OUTBOUND) {
                    sChannelType = "writeOutbound";
                } else if (channelType == CS_CONNECTOR) {
                    sChannelType = "connect";
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "All selectors full, can not handle new request on TCP Channel type: " + sChannelType);
                }
                IOException ioe = new IOException("All selectors full, can not handle new request on TCP Channel type: " + sChannelType);
                FFDCFilter.processException(ioe, getClass().getName(), "130", this);
                throw ioe;
            }
        } // end-sync
    }

    private void requestComplete(TCPBaseRequestContext req, IOException t) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "requestComplete");
        }
        TCPConnLink conn = req.getTCPConnLink();

        if (req.blockedThread) {
            if (t != null) {
                req.blockingIOError = t;
            }
            req.blockWait.simpleNotify();
        } else {

            if (req.isRequestTypeRead()) {
                TCPReadCompletedCallback cc = ((TCPReadRequestContextImpl) req).getReadCompletedCallback();
                if (cc != null) {
                    if (t != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "calling cc.error callback");
                        }
                        cc.error(conn.getVirtualConnection(), (TCPReadRequestContext) req, t);
                    } else {
                        // check if we have been told to stop. Makes the timing
                        // window smaller for sending unwanted requests.
                        if (conn.getTCPChannel().getStopFlag() == false) {
                            try {
                                cc.complete(conn.getVirtualConnection(), (TCPReadRequestContextImpl) req);
                            } catch (RuntimeException err) {
                                // If channel is not now stopped, rethrow whatever we hit, else
                                // we tried to access a stopped channel, ignore exceptions
                                if (conn.getTCPChannel().getStopFlag() == false)
                                    throw err;
                            }
                        }
                    }
                }
            } else {
                TCPWriteCompletedCallback cc = ((TCPWriteRequestContextImpl) req).getWriteCompletedCallback();

                if (cc != null) {
                    if (t != null) {
                        cc.error(conn.getVirtualConnection(), (TCPWriteRequestContext) req, t);
                    } else {
                        if (conn.getTCPChannel().getStopFlag() == false) {
                            try {
                                cc.complete(conn.getVirtualConnection(), (TCPWriteRequestContext) req);
                            } catch (RuntimeException err) {
                                // If channel is not now stopped, rethrow whatever we hit, else
                                // we tried to access a stopped channel, ignore exceptions
                                if (conn.getTCPChannel().getStopFlag() == false)
                                    throw err;
                            }
                        }
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "requestComplete");
        }
    }

    protected boolean attemptIO(TCPBaseRequestContext req, boolean fromSelector) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "attemptIO");
        }
        IOResult status = IOResult.NOT_COMPLETE;
        TCPConnLink conn = req.getTCPConnLink();
        SocketIOChannel ioChannel = conn.getSocketIOChannel();
        if (ioChannel == null || conn.isClosed()) {
            // connection is closed, framework is stopping, or
            // the channel has been destroyed (ioChannel set to null)
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "attemptIO", "Closed");
            }
            return false;
        }

        try {
            req.setLastIOAmt(0);
            if (req.isRequestTypeRead()) {

                // Try to complete the read, queue it if not done
                if (!req.isForceQueue()) {
                    status = ioChannel.attemptReadFromSocket(req, fromSelector);
                    // see if operation is complete, without ever going to selector
                    if (status == IOResult.COMPLETE && !fromSelector) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "attemptIO");
                        }
                        return true;
                    }
                }

                if (status == IOResult.COMPLETE) {
                    requestComplete(req, null);
                } else if (status == IOResult.NOT_COMPLETE) {
                    // increment statistic if enabled
                    if (!req.isForceQueue() && req.getConfig().getDumpStatsInterval() > 0) {
                        if (req.blockedThread) {
                            conn.getTCPChannel().totalPartialSyncReads.incrementAndGet();
                        } else {
                            if (!fromSelector && req.getLastIOAmt() == 0) {
                                conn.getTCPChannel().totalAsyncReadRetries.incrementAndGet();
                            } else {
                                conn.getTCPChannel().totalPartialAsyncReads.incrementAndGet();
                            }
                        }
                    }
                    // reset flag so next time through doesn't force requeue again
                    req.setForceQueue(false);
                    queueIO(req);
                }
            } else {
                // Try to complete the write, queue it if not done
                if (!req.isForceQueue()) {
                    status = ioChannel.attemptWriteToSocket(req);

                    // see if operation is complete, without ever going to selector
                    if (status == IOResult.COMPLETE && !fromSelector) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "attemptIO");
                        }
                        return true;
                    }
                }

                if (status == IOResult.COMPLETE) {
                    requestComplete(req, null);
                } else if (status == IOResult.NOT_COMPLETE) {
                    // increment statistic if enabled
                    if (!req.isForceQueue() && req.config.getDumpStatsInterval() > 0) {
                        if (req.blockedThread) {
                            conn.getTCPChannel().totalPartialSyncWrites.incrementAndGet();
                        } else {
                            if (!fromSelector && req.getLastIOAmt() == 0) {
                                conn.getTCPChannel().totalAsyncWriteRetries.incrementAndGet();
                            } else {
                                conn.getTCPChannel().totalPartialAsyncWrites.incrementAndGet();
                            }
                        }
                    }
                    // reset flag so next time through doesn't force requeue again
                    req.setForceQueue(false);
                    queueIO(req);
                }
            }
        } catch (IOException e) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "IOException while doing IO requested on local: " + conn.getSocketIOChannel().getSocket().getLocalSocketAddress() + " remote: "
                             + conn.getSocketIOChannel().getSocket().getRemoteSocketAddress());
                Tr.event(tc, "Exception is: " + e);
            }
            // the following block of commented code was to handle temporary IO
            // exceptions. Those
            // shouldn't be thrown anymore since the JDK fixed the defect, but
            // but leave the code here just in case we have the same
            /*
             * // a 'temporary' IO exception can happen for example, when a wireless
             * connection
             * // temporarily loses association with an access point. In this case, we
             * get
             * // a SocketException: Resource temporarily unavailable exception. If we
             * simply
             * // put this back in the selector, we will re-try again
             *
             * if ((e.getMessage()!= null) &&
             * (e.getMessage().startsWith(TEMP_IO_ERROR))) {
             * // (TO DO)this exception shouldn't happen. We should get successful io
             * of 0 bytes
             * // instead. The JTC team needs stack trace for this, so write error on
             * // console to get stack. This code should be removed after the next
             * // JDK build (02/09 or later)
             * try {
             * queueIO(req);
             * } catch (IOException x) {
             *
             * if ((x.getMessage()!= null) &&
             * (x.getMessage().startsWith(TEMP_IO_ERROR))) {
             * if ((req.isRequestTypeRead()) && (((TCPReadRequestContextImpl)
             * req).getJITAllocateAction() == true)) {
             * req.getBuffer().release();
             * req.setBuffer(null);
             * ((TCPReadRequestContextImpl) req).setJITAllocateAction(false);
             * }
             * // callback the error method
             * requestComplete(req, x);
             * }
             * }
             * } else
             */
            {
                // unrecoverable error, cleanup and finish request
                if (req.isRequestTypeRead() && ((TCPReadRequestContextImpl) req).getJITAllocateAction()) {
                    req.getBuffer().release();
                    req.setBuffer(null);
                    ((TCPReadRequestContextImpl) req).setJITAllocateAction(false);
                }

                // callback the error method
                requestComplete(req, e);
            }
        }

        if (status == IOResult.FAILED) {
            IOException ioe = null;

            // "-1" bytes of data was read/written.
            // construct an IOException to pass to error callback
            // do not use static as the stacks are not the same
            if (req.isRequestTypeRead()) {
                // Add local and remote address information
                String s = "Connection closed: Read failed.  Possible end of stream encountered.";
                try {
                    SocketAddress aLocal = conn.getSocketIOChannel().getSocket().getLocalSocketAddress();
                    SocketAddress aRemote = conn.getSocketIOChannel().getSocket().getRemoteSocketAddress();
                    s = s + " local=" + aLocal + " remote=" + aRemote;
                } catch (Exception x) {
                    // do not alter the message if the socket got nuked while we tried to look at it
                }
                if (-1==req.getLastIOAmt()) {
                  ioe = new EOFException(s);
                } else {
                  ioe = new IOException(s);
                }
            } else {
                // Add local and remote address information
                String s = "Connection closed: Write failed.";
                try {
                    SocketAddress aLocal = conn.getSocketIOChannel().getSocket().getLocalSocketAddress();
                    SocketAddress aRemote = conn.getSocketIOChannel().getSocket().getRemoteSocketAddress();
                    s = s + " local=" + aLocal + " remote=" + aRemote;
                } catch (Exception x) {
                    // do not alter the message if the socket got nuked while we tried to look at it
                }
                ioe = new IOException(s);
            }

            // callback the error method
            requestComplete(req, ioe);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "attemptIO");
        }

        // return, signal that we need to use a selector to complete the operation
        return false;
    }

    /**
     * Dispatches requests to workrer threds, or notifies waiting thread.
     *
     * @param req
     * @param ioe
     * @return boolean, true if request was dispatched
     */
    protected boolean dispatch(TCPBaseRequestContext req, IOException ioe) {

        if (req.blockedThread) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "dispatcher notifying waiting synch request ");
            }
            if (ioe != null) {
                req.blockingIOError = ioe;
            }
            req.blockWait.simpleNotify();
            return true;
        }
        // dispatch the async work
        return dispatchWorker(new Worker(req, ioe));
    }

    /**
     * Dispatch a work item.
     *
     * @param worker
     * @return boolean, true if dispatched
     */
    private boolean dispatchWorker(Worker worker) {
        ExecutorService executorService = CHFWBundle.getExecutorService();
        if (null == executorService) {
            if (FrameworkState.isValid()) {
                Tr.error(tc, "EXECUTOR_SVC_MISSING");
                throw new RuntimeException("Missing executor service");
            } else {
                // The framework is shutting down: the executor service may be
                // missing by the time the async work is dispatched.
                return false;
            }
        }

        executorService.execute(worker);

        return true;
    }

    /**
     * This method is called when work must be added to the connect selector.
     *
     * @param connectInfo
     */
    protected void queueConnectForSelector(ConnectInfo connectInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "queueConnectForSelector");
        }

        try {
            moveIntoPosition(connectCount, connect, connectInfo, CS_CONNECTOR);
        } catch (IOException x) {
            FFDCFilter.processException(x, getClass().getName(), "140", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught IOException...throwing RuntimeException");
            }
            throw new RuntimeException(x);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "queueConnectForSelector");
        }
    }

    /**
     * Create a new reader thread. Provided so we can support pulling these from a
     * thread pool
     * in the SyncWorkQueueManager. This will allow these threads to have
     * WSTHreadLocal.
     *
     * @param sr
     * @param threadType
     * @param number
     */
    protected void createNewThread(ChannelSelector sr, int threadType, int number) {
        StartPrivilegedThread privThread = new StartPrivilegedThread(sr, threadType, number, this.tGroup);
        AccessController.doPrivileged(privThread);
    }

    /**
     * This is where the actual work involving connects is done. This should not
     * be called from a selector thread. The point is for this method to do work
     * on a side band worker thread as it has the potential to block. This will be
     * called in 1 of 3 situations.
     * (1) a nonblocking connect has started and it needs to finish.
     * (2) a getConnection from the pool is required because the original resulted
     * in having to wait for a free slot in the pool.
     * (3) the error callback needs to be called because a connect failed.
     *
     * @param ci
     * @return boolean
     */
    protected boolean attemptConnectWork(ConnectInfo ci) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "attemptConnectWork");
        }

        boolean returnConnectDone = true;

        switch (ci.action) {

            case (ConnectInfo.FINISH_CONNECTION): {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Finish_connection case for, local: " + ci.localAddress + " remote: " + ci.remoteAddress);
                }
                if (ci.channel.isConnectionPending()) {
                    try {
                        boolean connectDone = ci.channel.finishConnect();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, "Finishconnect returned " + connectDone + " for, local: " + ci.ioSocket.getSocket().getLocalSocketAddress() + " remote: "
                                         + ci.ioSocket.getSocket().getRemoteSocketAddress());
                        }
                        if (!connectDone) {
                            // Not connected yet, so just put it back in the selector.
                            // This can happen if the network connection goes down
                            // while connect is in selector
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                Tr.event(tc, "FinishConnect returned false, retrying");
                            }
                            queueConnectForSelector(ci);
                            returnConnectDone = false;
                            break;
                        }
                        if (!ci.channel.isConnected()) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                Tr.event(tc, "FinishConnect returned true, but not connected");
                            }

                            // Add local and remote address information
                            InetSocketAddress iaRemote = ci.remoteAddress;
                            InetSocketAddress iaLocal = ci.localAddress;
                            IOException e = new IOException("Connection could not be established. local=" + iaLocal + " remote=" + iaRemote);

                            ci.setError(e);
                            ci.tcpConnLink.connectFailed(e);

                            break;
                        }
                    } catch (IOException ioe) {

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, "SocketChannel connect failed, local: " + ci.ioSocket.getSocket().getLocalSocketAddress() + " remote: "
                                         + ci.ioSocket.getSocket().getRemoteSocketAddress());
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "SocketChannel.finishConnect Exception Caught: " + ioe);
                        }

                        ci.setError(ioe);
                        ci.tcpConnLink.connectFailed(ioe);
                        break;
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Connection got selected, but isConnectionPending returned false");
                    }
                    returnConnectDone = false;
                    queueConnectForSelector(ci);
                    break;
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "SocketChannel connected, local: " + ci.ioSocket.getSocket().getLocalSocketAddress() + " remote: "
                                 + ci.ioSocket.getSocket().getRemoteSocketAddress());
                }

                ci.setFinishComplete();

                try {
                    ci.tcpConnLink.connectComplete(ci.ioSocket);
                } catch (IOException ioe) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "SocketChannel connect failed, local: " + ci.ioSocket.getSocket().getLocalSocketAddress() + " remote: "
                                     + ci.ioSocket.getSocket().getRemoteSocketAddress());
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "SocketChannel.finishConnect Exception Caught: " + ioe);
                    }
                    ci.setError(ioe);
                    ci.tcpConnLink.connectFailed(ioe);
                }
                break;
            }
            case (ConnectInfo.CALL_ERROR): {
                ci.tcpConnLink.connectFailed(ci.errorException);
                break;
            }
            default: {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Should never get here - default.");
                }
                break;
            }
        } // end-switch

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "attemptConnectWork returning " + returnConnectDone);
        }

        return returnConnectDone;
    }

    /**
     * Main worker thread routine.
     *
     * @param req
     * @param ioe
     */
    void workerRun(TCPBaseRequestContext req, IOException ioe) {
        if (null == req || req.getTCPConnLink().isClosed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Ignoring IO on closed socket: " + req);
            }
            return;
        }
        try {
            if (ioe == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Worker thread processing IO request: " + req);
                }
                attemptIO(req, true);
            } else {
                if (req.isRequestTypeRead()) {
                    TCPReadRequestContextImpl readReq = (TCPReadRequestContextImpl) req;
                    if (readReq.getReadCompletedCallback() != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Worker thread processing read error: " + req.getTCPConnLink().getSocketIOChannel().getChannel());
                        }
                        readReq.getReadCompletedCallback().error(readReq.getTCPConnLink().getVirtualConnection(), readReq, ioe);
                    }
                } else {
                    TCPWriteRequestContextImpl writeReq = (TCPWriteRequestContextImpl) req;
                    if (writeReq.getWriteCompletedCallback() != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Worker thread processing write error: " + req);
                        }
                        writeReq.getWriteCompletedCallback().error(writeReq.getTCPConnLink().getVirtualConnection(), writeReq, ioe);
                    }
                }
            }
        } catch (Throwable t) {
            // Only issue an FFDC if the framework is up/valid..
            if (FrameworkState.isValid()) {
                FFDCFilter.processException(t, getClass().getName(), "workerRun(req)", new Object[] { this, req, ioe });
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected error in worker; " + t);
            }
        }
    }

    void workerRun(ConnectInfo connInfo) {
        if (connInfo != null) {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Worker thread processing connect request");
                }
                attemptConnectWork(connInfo);
            } catch (Throwable t) {
                // Only issue an FFDC if the framework is up/valid..
                if (FrameworkState.isValid()) {
                    FFDCFilter.processException(t, getClass().getName(), "workerRun(conn)", new Object[] { this, connInfo });
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Unexpected error in worker; " + t);
                }
            }
        }
    }

    /**
     * This is the entry point where work is added to the connect work list.
     * As a result, a separate thread from the caller will do the work.
     *
     * @param work
     *            information about the connect
     * @return true if request was dispatched, false if not
     */
    protected boolean dispatchConnect(ConnectInfo work) {
        if (work.getSyncObject() != null) {
            // user thread waiting for work
            work.getSyncObject().simpleNotify();
            return true;
        }
        // dispatch async work
        return dispatchWorker(new Worker(work));
    }

    /**
     * TCP channel worker object for asynchronous IO completions.
     */
    protected class Worker implements Runnable {
        private TCPBaseRequestContext req = null;
        private ConnectInfo connInfo = null;
        private IOException ioe = null;

        // Work object used for dispatching ready work from selector
        protected Worker(TCPBaseRequestContext _reqIn, IOException _ioe) {
            this.req = _reqIn;
            this.ioe = _ioe;
        }

        protected Worker(ConnectInfo ciIn) {
            this.connInfo = ciIn;
        }

        @Override
        public void run() {
            if (this.req != null) {
                workerRun(this.req, this.ioe);
            } else if (this.connInfo != null) {
                workerRun(this.connInfo);
            }
        }
    }

    private static class StartPrivilegedThread implements PrivilegedAction<Object> {
        private final ChannelSelector sr;
        private final int threadType;
        private final int number;
        private final ThreadGroup group;

        /**
         * Constructor.
         *
         * @param _sr
         * @param _threadType
         * @param _number
         * @param tg
         */
        public StartPrivilegedThread(ChannelSelector _sr, int _threadType, int _number, ThreadGroup tg) {
            this.sr = _sr;
            this.threadType = _threadType;
            this.number = _number;
            this.group = tg;
        }

        /*
         * @see java.security.PrivilegedAction#run()
         */
        @Override
        public Object run() {
            // move startSelectorThread into here.
            String threadName = null;

            if (this.threadType == CS_READ_INBOUND) {
                threadName = "Inbound Read Selector";
            } else if (this.threadType == CS_READ_OUTBOUND) {
                threadName = "Outbound Read Selector";
            } else if (this.threadType == CS_WRITE_INBOUND) {
                threadName = "Inbound Write Selector";
            } else if (this.threadType == CS_WRITE_OUTBOUND) {
                threadName = "Outbound Write Selector";
            } else if (this.threadType == CS_CONNECTOR) {
                threadName = "Connect Selector";
            }

            Thread t = new Thread(this.group, this.sr);
            t.setName(threadName + "." + this.number);

            // all TCPChannel Thread should be daemon threads
            t.setDaemon(true);

            t.start();
            return null;
        }
    }

    /**
     * Privileged creation of a thread group.
     */
    private static class PrivGroupCreation implements PrivilegedAction<ThreadGroup> {
        private String name = null;

        protected PrivGroupCreation(String newName) {
            this.name = newName;
        }

        /*
         * @see java.security.PrivilegedAction#run()
         */
        @Override
        public ThreadGroup run() {
            return new ThreadGroup(this.name);
        }
    }
}
