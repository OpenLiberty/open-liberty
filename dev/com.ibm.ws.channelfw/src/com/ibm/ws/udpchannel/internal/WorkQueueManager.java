/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
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
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.bytebuffer.internal.WsByteBufferPoolManagerImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;
import com.ibm.wsspi.channelfw.objectpool.CircularObjectPool;
import com.ibm.wsspi.channelfw.objectpool.ObjectPool;
import com.ibm.wsspi.udpchannel.UDPConfigConstants;
import com.ibm.wsspi.udpchannel.UDPWriteCompletedCallback;

/**
 * @author mjohnson
 */
public class WorkQueueManager implements UDPSelectorMonitor {
    protected static final TraceComponent tc = Tr.register(WorkQueueManager.class, UDPMessages.TR_GROUP, UDPMessages.TR_MSGS);

    static final AtomicInteger numWorkerThreads = new AtomicInteger(0);

    private boolean shutdown = false;
    private Selector selector = null;
    private Thread selectorThread = null;
    private final long selectorTimeout = 10000L;
    private WsByteBufferPoolManager byteBufferManager = null;
    private final AtomicInteger refCount = new AtomicInteger(0);

    private static final int OBJ_SIZE = 100;

    private VirtualConnectionFactory vcFactory = null;
    private final Object channelRequestingToBeAddedRemovedSync = new Object()
    {
                    };
    private boolean channelRequestingToBeAddedRemoved = false;
    private final Map<DatagramChannel, SelectionKey> channelToSelectionKeyMap = new HashMap<DatagramChannel, SelectionKey>();
    private final List<NIOChannelModRequest> channelModList = new ArrayList<NIOChannelModRequest>();

    private final Object lock = new Object()
    {
                    };
    private boolean readAlways = false;

    private int numReceivesBeforeNewWorker = 10;
    private int numFailuresBeforeWorkerDie = 3;

    private UDPWriteRequestContextImpl outstandingWriteRequest = null;
    private final Object outstandingWriteLock = new Object()
    {
                    };

    private SelectorTask selectorTask = null;
    private long selectorThreadId = 0;

    private boolean isBufferDumpEnabled = false;

    private final ObjectPool multiThreadedObjectPool = new CircularObjectPool(OBJ_SIZE);

    /**
     * Constructor.
     * 
     * @param udpFactory
     * @throws IOException
     */
    public WorkQueueManager(UDPChannelFactory udpFactory) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "WorkQueueManager");
        }

        this.byteBufferManager = WsByteBufferPoolManagerImpl.getRef();
        this.vcFactory = udpFactory.getVCFactory();

        this.selector = Selector.open();
        // TODO use doPriv checks for Java2 security (like TCP)
        this.selectorTask = new SelectorTask();
        this.selectorThread = new Thread(selectorTask);
        this.selectorThread.setName("UDP WorkQueueManager Thread:" + numWorkerThreads.incrementAndGet());
        this.selectorThread.setDaemon(true);
        this.selectorThread.start();
        this.selectorThreadId = selectorThread.getId();

        String value = (String) udpFactory.getProperties().get("numReceivesBeforeNewWorker");
        if (value != null && 0 < value.length()) {
            this.numReceivesBeforeNewWorker = Integer.parseInt(value);
        }

        value = (String) udpFactory.getProperties().get("numFailuresBeforeWorkerDie");
        if (value != null && 0 < value.length()) {
            this.numFailuresBeforeWorkerDie = Integer.parseInt(value);
        }

        value = (String) udpFactory.getProperties().get("udpChannelBufferDumpEnabled");
        if (value != null && 0 < value.length()) {
            this.isBufferDumpEnabled = Boolean.parseBoolean(value);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "bufferDumpEnabled is " + value);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Creating new WQM with thread id: " + this.selectorThreadId);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "WorkQueueManager");
        }
    }

    public void addRef() {
        this.refCount.incrementAndGet();
    }

    public int decRef() {
        return this.refCount.decrementAndGet();
    }

    public void shutdown() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "shutdown");
        }
        if (decRef() <= 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Reference Count is 0 so shutting down.");
            }
            this.shutdown = true;
            this.selector.wakeup();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "shutdown");
        }
    }

    /*
     * @see
     * com.ibm.ws.udp.channel.internal.UDPSelectorMonitor#setChannel(java.nio.
     * channels.DatagramChannel, com.ibm.ws.udp.channel.internal.UDPNetworkLayer)
     */
    public synchronized void setChannel(DatagramChannel channel, UDPNetworkLayer udpNetworkLayer) throws IOException {

        int interestOps = 0;
        if (udpNetworkLayer.getUDPChannel().getConfig().isInboundChannel()) {
            interestOps = SelectionKey.OP_READ;
        }

        NIOChannelModRequest request = new NIOChannelModRequest(NIOChannelModRequest.ADD_REQUEST, channel, interestOps, udpNetworkLayer);
        synchronized (channelModList) {
            channelModList.add(request);
        }
        synchronized (channelRequestingToBeAddedRemovedSync) {
            channelRequestingToBeAddedRemoved = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Adding channel for port: " + channel.socket().getLocalPort() + " to WQM : " + hashCode());
        }

        selector.wakeup();
    }

    public synchronized void removeChannel(DatagramChannel channel) {
        NIOChannelModRequest request = new NIOChannelModRequest(NIOChannelModRequest.REMOVE_REQUEST, channel, 0, null);
        synchronized (channelModList) {
            channelModList.add(request);
        }
        synchronized (channelRequestingToBeAddedRemovedSync) {
            channelRequestingToBeAddedRemoved = true;
        }
        selector.wakeup();
    }

    private void setChannelInSelector(DatagramChannel channel, UDPNetworkLayer udpNetworkLayer, int interestMask, int interestOperand) throws IOException {

        // TODO this needs to be rewritten like the TCP workqueue mgr updating
        // to avoid the excessive locking and object creation (assuming it can
        // be rewritten)
        NIOChannelModRequest request = new NIOChannelModRequest(NIOChannelModRequest.MODIFY_REQUEST, channel, interestMask, interestOperand, udpNetworkLayer);
        synchronized (channelModList) {
            channelModList.add(request);
        }
        synchronized (channelRequestingToBeAddedRemovedSync) {
            channelRequestingToBeAddedRemoved = true;
        }
        if (Thread.currentThread().getId() != selectorThreadId) {
            selector.wakeup();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "selector.wakeup() for selector " + selector.hashCode());
            }
        }
    }

    private void handleChannelMods() throws IOException {
        synchronized (channelModList) {
            for (NIOChannelModRequest request : channelModList) {
                if (request.getRequestType() == NIOChannelModRequest.MODIFY_REQUEST) {
                    handleModRequest(request);
                } else if (request.getRequestType() == NIOChannelModRequest.ADD_REQUEST) {
                    synchronized (selector) {
                        SelectionKeyAttachment attachment = new SelectionKeyAttachment(request.getNetworkLayer());
                        SelectionKey selectionKey = request.getChannel().register(selector, request.getInterestMask(), attachment);
                        channelToSelectionKeyMap.put(request.getChannel(), selectionKey);
                    } // end-sync
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Added channel with interestOps " + request.getInterestMask());
                    }
                } else if (request.getRequestType() == NIOChannelModRequest.REMOVE_REQUEST) {
                    handleRemoveRequest(request);
                }
            }
            channelModList.clear();
        } // end-channelmod-sync
    }

    private void handleModRequest(NIOChannelModRequest request) {
        SelectionKey selectionKey = channelToSelectionKeyMap.get(request.getChannel());
        if (selectionKey == null) {
            return;
        }
        if (!selectionKey.isValid()) {
            // no longer valid (i.e. cancelled), ignore the
            // modify attempt as this should be a timing window
            // between an attempted update and a succesfull close
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Ignoring mod attempt on invalid key; " + selectionKey.hashCode());
            }
            return;
        }
        try {
            int currentOps = selectionKey.interestOps();
            int newOps = currentOps;
            if (request.getInterestOperator() == NIOChannelModRequest.OR_OPERATOR) {
                newOps |= request.getInterestMask();
            } else if (request.getInterestOperator() == NIOChannelModRequest.AND_OPERATOR) {
                newOps &= request.getInterestMask();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Modified interest ops old=" + currentOps + " new=" + newOps + " for channel " + selectionKey.hashCode());
            }
            selectionKey.interestOps(newOps);
        } catch (CancelledKeyException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error modifying cancelled key; " + selectionKey.hashCode());
            }
        }
    }

    private void handleRemoveRequest(NIOChannelModRequest request) {
        DatagramChannel channel = request.getChannel();
        if (null == channel) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Removing channel; " + channel);
        }
        synchronized (selector) {
            channelToSelectionKeyMap.remove(channel);
            SelectionKey selectionKey = channel.keyFor(selector);
            if (selectionKey != null) {
                selectionKey.cancel();
            }
        } // end-sync
    }

    private VirtualConnection processWriteRequest(UDPWriteRequestContextImpl writeRequest, boolean forceWrite) {
        VirtualConnection vc = null;
        //
        // If the writeQueue is empty and there isn't a write outstanding, then
        // just output it. If it has something in it, then add it to the
        // writeQueue and return null for it being queued.
        //
        synchronized (outstandingWriteLock) {
            boolean writeIt = false;
            if (forceWrite) {
                writeIt = true;
            } else {
                if (outstandingWriteRequest == null) {
                    writeIt = true;
                }
                if (!writeRequest.isForceQueue()) {
                    writeIt = true;
                } else {
                    writeIt = false;
                }
            }
            if (writeIt) {
                try {
                    vc = doPhysicalWrite(writeRequest);
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Error writing message, discarding message. " + e.getMessage());
                    }
                    outstandingWriteRequest = null;
                    vc = writeRequest.getConnLink().getVirtualConnection();
                }
            } else {
                outstandingWriteRequest = writeRequest;
                try {
                    setChannelInSelector(writeRequest.getConnLink().getUDPNetworkLayer().getDatagramChannel(), writeRequest.getConnLink().getUDPNetworkLayer(),
                                         SelectionKey.OP_WRITE,
                                         NIOChannelModRequest.OR_OPERATOR);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Turning on WRITE from processWriteRequest");
                    }
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Error while setChannelInSelector. " + e.getMessage());
                    }
                }
            }
        } // end-sync

        return vc;
    }

    private VirtualConnection doPhysicalWrite(UDPWriteRequestContextImpl writeRequest) throws IOException {
        VirtualConnection vc = null;

        outstandingWriteRequest = writeRequest;
        UDPConnLink connLink = writeRequest.getConnLink();

        UDPNetworkLayer udpPort = connLink.getUDPNetworkLayer();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && isBufferDumpEnabled) {
            Tr.debug(this, tc, "BUFFER TO ADDRESS " + writeRequest.getAddress());
            String dumpedBuffer = BufferDump.getHexDump(writeRequest.getBuffer().getWrappedByteBuffer(), true);
            Tr.debug(this, tc, dumpedBuffer);
        }

        //
        // This is initialzied to 1 instead of 0 because if we get an exception,
        // we want this to be treated just as if the packet were sent.
        //
        int numWritten = 1;
        try {
            numWritten = udpPort.send(writeRequest.getBuffer(), writeRequest.getAddress());
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Caught exception " + e.toString() + " while sending data.  Packet is lost.");
            }
            FFDCFilter.processException(e, getClass().getName(), "1", this);
        }

        if (numWritten != 0) {
            //
            // We don't have to worry about the numWritten being == to the
            // number of bytes in the packet because of the nature of the
            // UDP API. Everything will be written if any of it
            // is written. ie.. No partial packets will be written.
            //
            outstandingWriteRequest = null;
            vc = writeRequest.getConnLink().getVirtualConnection();
        } else {
            //
            // Need to now look for the OP_WRITE to see when we can send data
            // again.
            //
            setChannelInSelector(writeRequest.getConnLink().getUDPNetworkLayer().getDatagramChannel(), writeRequest.getConnLink().getUDPNetworkLayer(), SelectionKey.OP_WRITE,
                                 NIOChannelModRequest.OR_OPERATOR);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Turning on WRITE from doPhysicalWrite");
            }
        }

        return vc;
    }

    private void setupReadOp(UDPReadRequestContextImpl readRequest) {
        try {
            setChannelInSelector(readRequest.getConnLink().getUDPNetworkLayer().getDatagramChannel(), readRequest.getConnLink().getUDPNetworkLayer(), SelectionKey.OP_READ,
                                 NIOChannelModRequest.OR_OPERATOR);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SelectionKey selectionKey = channelToSelectionKeyMap.get(readRequest.getConnLink().getUDPNetworkLayer().getDatagramChannel());
                if (selectionKey != null) {
                    Tr.debug(this, tc, "Turning on READ from processReadRequest for channel " + selectionKey.hashCode());
                }
            }

        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "setupReadOp IOException caught. " + e);
            }
        }
    }

    private VirtualConnection processReadRequest(UDPReadRequestContextImpl readRequest) {
        VirtualConnection vc = null;
        if (readRequest.isForceQueue() || readRequest.isReadAlwaysCalled()) {
            setupReadOp(readRequest);
            if (readRequest.isReadAlwaysCalled()) {
                readAlways = true;
            }
        } else {
            if (readAlways && !readRequest.isReadAlwaysCalled()) {
                // turning off the read-always path
                readAlways = false;
                try {
                    setChannelInSelector(readRequest.getConnLink().getUDPNetworkLayer().getDatagramChannel(), readRequest.getConnLink().getUDPNetworkLayer(),
                                         ~SelectionKey.OP_READ,
                                         NIOChannelModRequest.AND_OPERATOR);
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "processReadRequest caught " + e);
                    }
                }

            } else {
                //
                // Try to read from the network layer, if it returns null, then
                // add the read op to the selector.
                //
                UDPNetworkLayer networkLayer = readRequest.getConnLink().getUDPNetworkLayer();
                WsByteBuffer buffer = byteBufferManager.allocateDirect(networkLayer.getUDPChannel().getConfig().getChannelReceiveBufferSize());

                try {
                    SocketAddress address = networkLayer.receive(buffer);
                    if (address != null) {
                        readRequest.setBuffer(buffer, address, false);
                        vc = readRequest.getConnLink().getVirtualConnection();
                    } else {
                        //
                        // If there wasn't anything there, then release the
                        // buffer and call setupReadOp.
                        //
                        buffer.release();
                        setupReadOp(readRequest);
                    }
                } catch (IOException e) {
                    buffer.release();
                }
            }
        }
        return vc;
    }

    public VirtualConnection processWork(UDPRequestContextImpl req) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "processWork");
        }
        VirtualConnection vc = null;

        if (req.isRead()) {
            UDPReadRequestContextImpl readRequest = (UDPReadRequestContextImpl) req;
            vc = processReadRequest(readRequest);
        } else {
            UDPWriteRequestContextImpl writeRequest = (UDPWriteRequestContextImpl) req;
            vc = processWriteRequest(writeRequest, false);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "processWork: " + vc);
        }

        return vc;
    }

    private void sendToDiscriminaters(VirtualConnection vc, UDPReadRequestContextImpl readRequest, UDPChannel udpChannel) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "sendToDiscriminaters");
        }

        int state = DiscriminationProcess.FAILURE;
        try {
            state = udpChannel.getDiscriminationProcess().discriminate(vc, readRequest.getUDPBuffer().getBuffer(), readRequest.getConnLink());
        } catch (DiscriminationProcessException dpe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Exception occurred while discriminating data received from client ");
            }

            readRequest.getConnLink().close(vc, new IOException("Discrimination failed", dpe));
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Discrimination returned " + state);
        }

        if (state == DiscriminationProcess.SUCCESS) {

            ConnectionReadyCallback cb = readRequest.getConnLink().getApplicationCallback();
            // If cb is null, then connlink may have been destroyed by channel
            // stop if so, nothing more needs to be done
            if (cb != null) {
                dispatchWorker(new Worker(cb, vc));
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "No application callback found, closing connection");
                }
                readRequest.getConnLink().close(vc, null);
            }
        } else if (state == DiscriminationProcess.AGAIN) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Discrimination failed, no one claimed data even after 1 complete buffer presented - probably garbage passed in");
            }
            readRequest.getConnLink().close(vc, null);
        } else {
            // FAILURE
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error occurred while discriminating data received from client");
            }
            readRequest.getConnLink().close(vc, null);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "sendToDiscriminaters");
        }
    }

    public class SelectorTask implements Runnable {
        public void run() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.entry(WorkQueueManager.this, tc, "SelectorTask.run");
            }
            synchronized (lock) {
                final long DIAG_TIME = 10000L;
                // Counter used for diagnostics when the UDP Channel isn't
                // receiving any data.
                long lastPacketReceivedTime = 0;
                int failureCount = 0;
                while (!shutdown) {
                    try {
                        int numReady = 0;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(WorkQueueManager.this, tc, "Calling select: " + selectorTimeout);
                        synchronized (selector) {
                            numReady = selector.select(selectorTimeout);
                        }

                        if (numReady == 0) {
                            //
                            // Yes I only want to do this if event is
                            // enabled.... its just diagnostics
                            //
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && !shutdown) {
                                // It should print out diagsnotics when no
                                // packets have been received
                                long currentTime = System.currentTimeMillis();
                                if (lastPacketReceivedTime > 0 && (currentTime > (lastPacketReceivedTime + DIAG_TIME))) {
                                    lastPacketReceivedTime = 0;
                                    Set<SelectionKey> set = selector.keys();
                                    if (set != null) {
                                        Tr.event(WorkQueueManager.this, tc, "*** current interest ops ");
                                        for (SelectionKey key : set) {
                                            if (key != null) {
                                                Tr.event(WorkQueueManager.this, tc, "channel = " + key.hashCode() + " interestOps " + key.interestOps());
                                            }
                                        }
                                        Tr.event(WorkQueueManager.this, tc, "*** end current interest ops ");
                                    }
                                }
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(WorkQueueManager.this, tc, "returned from select = " + numReady);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                // Reset the counter since I have some data now.
                                lastPacketReceivedTime = System.currentTimeMillis();
                            }

                            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                            while (it.hasNext()) {
                                SelectionKey key = it.next();
                                try {
                                    SelectionKeyAttachment attachment = (SelectionKeyAttachment) key.attachment();
                                    UDPNetworkLayer networkLayer = attachment.udpNetworkLayer;
                                    UDPConnLink udpConnLink = networkLayer.getConnLink();
                                    if (!key.isValid()) {
                                        it.remove();
                                        continue;
                                    }
                                    if (readAlways && udpConnLink != null) {
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                            Tr.debug(WorkQueueManager.this, tc, "Starting worker thread from WQM: " + attachment.getNumThreadsProcessing());
                                        }
                                        MultiThreadedWorker worker = getMultiThreadedWorker(key, Thread.currentThread().getId());
                                        dispatchWorker(worker);
                                    } else {
                                        //
                                        // Make sure this is false here
                                        //
                                        readAlways = false;
                                        if (key.isReadable()) {
                                            handleRead(key, networkLayer);
                                        }
                                        if (key.isWritable()) {
                                            handleWrite(key);
                                        }
                                    }
                                } catch (CancelledKeyException e) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(WorkQueueManager.this, tc, "Cancelled key exception.");
                                    }
                                }

                                it.remove();
                            }
                            if (readAlways) {
                                try {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(WorkQueueManager.this, tc, "Waiting on lock.");
                                    }
                                    lock.wait();
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(WorkQueueManager.this, tc, "After wait on lock.");
                                    }
                                } catch (InterruptedException e) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(WorkQueueManager.this, tc, "Caught InterruptedException waiting on lock.");
                                    }
                                }
                            }
                        }
                        //
                        // check and see if there are any updates before getting back into
                        // the select call. This may or may not have been done
                        // in this thread's context,
                        // but why not take advatage of it instead of having to
                        // worry about breaking out of the select with the
                        synchronized (channelRequestingToBeAddedRemovedSync) {
                            if (channelRequestingToBeAddedRemoved) {
                                channelRequestingToBeAddedRemoved = false;
                                handleChannelMods();
                            }
                        }
                        //
                        // Reset the failure count since we got down this far
                        // and haven't received an IOException
                        //
                        failureCount = 0;
                    } catch (Throwable e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(WorkQueueManager.this, tc, "Error while selecting. " + e.getMessage());
                        }

                        failureCount++;
                        //
                        // If I have recieved more than 5 IOException's in a
                        // row, then we need to start sleeping some in between.
                        //
                        // if (failureCount > 5) {
                        // //TODO bad idea inside the lock
                        // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        // {
                        // Tr.debug(WorkQueueManager.this, tc,
                        // "Sleeping for 5 seconds so we don't spin out of control. ");
                        // }
                        // try {
                        // Thread.sleep(5000);
                        // } catch (InterruptedException e1) {
                        // // do nothing
                        // }
                        // }
                    }
                }
            } // end-sync

            try {
                selector.close();
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(WorkQueueManager.this, tc, "Error closing selector. " + e.getMessage());
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(WorkQueueManager.this, tc, "SelectorTask.run");
            }
        }
    }

    protected void handleWrite(SelectionKey key) {
        //
        // For the UDP send request that just finished, issue the callback to it
        // and start looking for only READ_EVENTS.
        //
        synchronized (outstandingWriteLock) {
            try {
                //
                // Okay now try and see if we can write the outstandingWriteRequest,
                // if we can, then send up the write completed callback
                //
                if (outstandingWriteRequest != null) {
                    UDPWriteRequestContextImpl request = outstandingWriteRequest;
                    VirtualConnection vc = processWriteRequest(outstandingWriteRequest, true);
                    if (vc != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Turning off WRITE from selector thread");
                        }
                        setChannelInSelector(request.getConnLink().getUDPNetworkLayer().getDatagramChannel(), request.getConnLink().getUDPNetworkLayer(), ~SelectionKey.OP_WRITE,
                                             NIOChannelModRequest.AND_OPERATOR);
                        UDPWriteCompletedCallback callback = request.getWriteCallback();
                        if (callback != null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Calling completed callback.");
                            }
                            callback.complete(vc, request);
                        }
                    }
                }

            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Error while setChannelInSelector. " + e);
                }
            }
        } // end-sync
    }

    protected boolean handleRead(SelectionKey key, UDPNetworkLayer networkLayer) {

        boolean returnValue = true;

        try {
            WsByteBuffer buffer = byteBufferManager.allocateDirect(networkLayer.getUDPChannel().getConfig().getChannelReceiveBufferSize());
            SocketAddress address = networkLayer.receive(buffer);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && isBufferDumpEnabled && address != null) {
                Tr.debug(this, tc, "BUFFER FROM ADDRESS " + address);
                String dumpedBuffer = BufferDump.getHexDump(buffer.getWrappedByteBuffer(), false);
                Tr.debug(this, tc, dumpedBuffer);
            }

            if (address != null) {
                UDPConnLink udpConnLink = networkLayer.getConnLink();

                if (!readAlways) {
                    setChannelInSelector(networkLayer.getDatagramChannel(), networkLayer, ~SelectionKey.OP_READ, NIOChannelModRequest.AND_OPERATOR);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Turning off READ from selector thread");
                    }
                }

                if (udpConnLink == null) {
                    try {
                        VirtualConnection vc = vcFactory.createConnection();
                        udpConnLink = (UDPConnLink) networkLayer.getUDPChannel().getConnectionLink(vc);
                        networkLayer.setConnLink(udpConnLink);
                        udpConnLink.setUDPNetworkLayer(networkLayer);

                        vc.getStateMap().put(UDPConfigConstants.CONFIGURED_HOST_INTERFACE_VC_MAP, networkLayer.getConfiguredBindAddress());
                        vc.getStateMap().put(UDPConfigConstants.CONFIGURED_PORT_VC_MAP, Integer.valueOf(networkLayer.getListenPort()));

                        UDPReadRequestContextImpl readRequest = (UDPReadRequestContextImpl) udpConnLink.getReadInterface();

                        readRequest.setBuffer(buffer, address, true);

                        sendToDiscriminaters(vc, readRequest, networkLayer.getUDPChannel());

                    } catch (ChainException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Error creating VC " + e.getMessage());
                        }
                    } catch (ChannelException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Error creating VC " + e.getMessage());
                        }
                    }
                } else {
                    if (!readAlways) {

                        UDPReadRequestContextImpl readRequest = (UDPReadRequestContextImpl) udpConnLink.getReadInterface();
                        boolean bufferSet = readRequest.setBuffer(buffer, address, false);
                        if (bufferSet) {
                            dispatchWorker(new Worker(readRequest));
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "setBuffer returned false, not calling back with buffer");
                            }
                        }
                    }
                    // If readAlways is true
                    else {
                        UDPReadRequestContextImpl readRequest = (UDPReadRequestContextImpl) udpConnLink.getReadInterface();
                        readRequest.complete(UDPBufferFactory.getUDPBuffer(buffer, address));
                    }
                }
            } else {
                //
                // Need to release the buffer that was allocated
                //
                buffer.release();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Read event but there was nothing to read");
                }
                returnValue = false;
            }
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error in handleRead. " + e);
            }
            returnValue = false;
        } catch (Throwable e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error in handleRead. " + e);
            }
            returnValue = false;
        }

        return returnValue;
    }

    protected static class SelectionKeyAttachment {
        private final AtomicInteger numThreadsProcessing = new AtomicInteger(0);
        protected UDPNetworkLayer udpNetworkLayer = null;

        SelectionKeyAttachment(UDPNetworkLayer udpNetworkLayer) {
            this.udpNetworkLayer = udpNetworkLayer;
        }

        public int getNumThreadsProcessing() {
            return this.numThreadsProcessing.get();
        }

        public void incNumThreadsProcessing() {
            this.numThreadsProcessing.incrementAndGet();
        }

        public int decNumThreadsProcessing() {
            return this.numThreadsProcessing.decrementAndGet();
        }

        public UDPNetworkLayer getUdpNetworkLayer() {
            return this.udpNetworkLayer;
        }

    }

    protected class MultiThreadedWorker implements Runnable {

        private SelectionKey key = null;
        private WorkQueueManager factory = null;

        protected MultiThreadedWorker(SelectionKey key) {
            this.key = key;
        }

        protected MultiThreadedWorker(WorkQueueManager factory) {
            this.factory = factory;
        }

        public void set(SelectionKey key) {
            this.key = key;
        }

        public void release() {
            if (factory != null) {
                factory.release(this);
            }
        }

        public void clear() {
            this.key = null;
        }

        public void run() {
            SelectionKeyAttachment attachment = (SelectionKeyAttachment) key.attachment();
            try {
                // Should be the first thing done.
                attachment.incNumThreadsProcessing();
                boolean stopping = false;
                int numFailedReadsInRow = 0;
                int numSuccessfulReadsInRow = 0;
                int maxSuccessfulReadsinRow = 0;
                int realMaxSuccessfulReadsinRow = 0;
                //
                // Current design is to start up a new worker thread IF this
                // worker thread has read
                // numReceivesBeforeNewWorker times without a read failure. If
                // it receives a read failure
                // the count starts over. If it has numFailuresBeforeWorkerDie
                // in a row, this worker thread
                // will die off and be put back into the pool.
                //
                while (!stopping) {
                    boolean readSomething = handleRead(key, attachment.udpNetworkLayer);
                    handleWrite(key);
                    if (!readSomething) {
                        if (maxSuccessfulReadsinRow > realMaxSuccessfulReadsinRow)
                            realMaxSuccessfulReadsinRow = maxSuccessfulReadsinRow;

                        numSuccessfulReadsInRow = 0;
                        numFailedReadsInRow++;

                        //
                        // 3 unsuccessful reads will cause this thread to go
                        // away.
                        //
                        if (numFailedReadsInRow == numFailuresBeforeWorkerDie) {
                            stopping = true;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(WorkQueueManager.this, tc, "Stopping this worker thread: " + attachment.getNumThreadsProcessing() + ":" + realMaxSuccessfulReadsinRow);
                            }
                        }
                    } else {
                        numSuccessfulReadsInRow++;
                        // Diagnostic purposes only.
                        maxSuccessfulReadsinRow++;
                        numFailedReadsInRow = 0;
                        //
                        // If this we have read more than
                        // numReceivesBeforeNewWorker, then lets startup a new
                        // worker thread.
                        // then reset the numSuccessfulReadsInRow to zero.
                        //
                        if (numSuccessfulReadsInRow > numReceivesBeforeNewWorker) {
                            numSuccessfulReadsInRow = 0;
                            //
                            // If we are already maxed out on worker threads,
                            // then lets not do this
                            //
                            // TODO remove, replace?
                            // if (attachment.getNumThreadsProcessing() <
                            // threadPool.getMaximumPoolSize()) {
                            // try {
                            // int rc = threadPool.execute(
                            // getMultiThreadedWorker(key,
                            // threadIdfWQM),
                            // ThreadPool.EXPAND_WHEN_AT_CAPACITY_REJECT_AT_LIMIT);
                            // if (rc != ThreadPool.DISPATCH_SUCCESSFUL) {
                            // if (TraceComponent.isAnyTracingEnabled() &&
                            // tc.isDebugEnabled()) {
                            // Tr.debug(this, tc, threadIdfWQM
                            // + ":Failed to get thread from thread pool.  rc = "
                            // + rc);
                            // }
                            // } else {
                            // if (TraceComponent.isAnyTracingEnabled() &&
                            // tc.isDebugEnabled()) {
                            // Tr.debug(this, tc, threadIdfWQM
                            // + ":Starting up new worker thread from worker thread: "
                            // + attachment.getNumThreadsProcessing()
                            // + " after processing "
                            // + numSuccessfulReadsInRow
                            // + " packets.");
                            // }
                            // }
                            // } catch (Throwable e) {
                            // if (TraceComponent.isAnyTracingEnabled() &&
                            // tc.isDebugEnabled())
                            // Tr.debug(this, tc, threadIdfWQM
                            // + ":Caught throwable while executing new worker thread = "
                            // + e.getMessage());
                            // }
                            // }
                        }
                    }
                }
            } catch (Throwable e) {
                FFDCFilter.processException(e, getClass().getName(), "2", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(WorkQueueManager.this, tc, "Caught throwable while in worker thread = " + e);
            } finally {
                if (0 >= attachment.decNumThreadsProcessing()) {
                    synchronized (lock) {
                        lock.notify();
                    }
                }
                release();
            }
        }
    }

    /**
     * Work object used for dispatching ready work from selector to runtime
     * threads.
     */
    protected class Worker implements Runnable {
        private UDPReadRequestContextImpl req = null;
        private ConnectionReadyCallback cb = null;
        private VirtualConnection vc = null;

        /**
         * Constructor used on IO completions.
         * 
         * @param reqIn
         */
        protected Worker(UDPReadRequestContextImpl reqIn) {
            this.req = reqIn;
        }

        /**
         * Constructor used on connect completions.
         * 
         * @param cb
         * @param vc
         */
        protected Worker(ConnectionReadyCallback cb, VirtualConnection vc) {
            this.cb = cb;
            this.vc = vc;
        }

        /*
         * @see java.lang.Runnable#run()
         */
        public void run() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(WorkQueueManager.this, tc, "Running " + this);
            }

            if (req != null) {
                req.complete();
            } else {
                if (cb != null) {
                    cb.ready(vc);
                }
            }
        }
    }

    protected boolean dispatchWorker(Runnable worker) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Dispatching: " + worker);
        }

        ExecutorService executorService = CHFWBundle.getExecutorService();
        if (null == executorService) {
            Tr.error(tc, "EXECUTOR_SVC_MISSING");
            throw new RuntimeException("Missing executor service");
        }

        executorService.execute(worker);

        return true;
    }

    /**
     * Retrieve a MultiThreadedWorker object from the object pool.
     * 
     * @param key
     * @param threadIdfWQM
     * @return MultiThreadedWorker
     */
    protected MultiThreadedWorker getMultiThreadedWorker(SelectionKey key, long threadIdfWQM) {

        MultiThreadedWorker worker = null;
        synchronized (multiThreadedObjectPool) {
            worker = (MultiThreadedWorker) multiThreadedObjectPool.get();
        }

        if (worker == null) {
            worker = new MultiThreadedWorker(this);
        }

        worker.set(key);
        return worker;
    }

    /**
     * Return an MultiThreadedWorker object from the object pool
     * 
     * @param MultiThreadedWorker
     */
    protected void release(MultiThreadedWorker object) {
        object.clear();
        synchronized (multiThreadedObjectPool) {
            multiThreadedObjectPool.put(object);
        }
    }

}
