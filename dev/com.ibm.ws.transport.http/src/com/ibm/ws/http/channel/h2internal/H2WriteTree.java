/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.util.concurrent.ExecutorService;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.exceptions.FlowControlException;
import com.ibm.ws.http.channel.h2internal.priority.Node;
import com.ibm.ws.http.channel.h2internal.priority.Node.NODE_STATUS;
import com.ibm.ws.http.channel.h2internal.priority.Node.WRITE_COUNT_ACTION;
import com.ibm.ws.http.channel.h2internal.priority.Tree;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

// WDW - Improvements
// cut down on find the node in the tree by finding and returning the node, not the entry in findNext
// and change tree.updateNode to take a Node as input

/**
 *
 */
public class H2WriteTree implements H2WorkQInterface {

    private static final TraceComponent tc = Tr.register(H2WriteTree.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    TCPWriteRequestContext writeReqContext = null;
    H2MuxTCPWriteCallback muxCallback = null;
    Tree tree = null;

    Object qSync = new Object() {};
    Q_STATUS qStatus = Q_STATUS.NOT_IN_USE;

    // when told to quit, code will drain the queue, or not, depends on this flag
    boolean drainQ = false;

    // since there is one Tree per Connection, the tree will keep track of the connection window update parameters.
    private final int connectionWindowUpdateWriteInitialSize = 65535;
    private int connectionWindowUpdateWriteLimit = 65535;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.h2internal.H2WorkQInterface#init(com.ibm.wsspi.tcpchannel.TCPWriteRequestContext, com.ibm.ws.http.channel.h2internal.H2MuxTCPWriteCallback)
     */
    @Override
    public void init(TCPWriteRequestContext x, H2MuxTCPWriteCallback c) {
        writeReqContext = x;
        muxCallback = c;

        // callback will need to know how to get back to this write queue.
        // This means one and only one H2WriteTree per H2InboundLink which has just one true TCP Channel facing write callback
        muxCallback.setH2WorkQ(this);

        tree = new Tree();

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.h2internal.H2WorkQInterface#notifyStandBy()
     */
    @Override
    public void notifyStandBy() {
        synchronized (qSync) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "notifyStandBy called with qStatus: " + qStatus);
            }
            if (qStatus == Q_STATUS.BYPASSED) {
                // no other writes waiting, so no queue thread in flight.  Set the state for more caller thread processing
                qStatus = Q_STATUS.NOT_IN_USE;
            } else if (qStatus == Q_STATUS.STAND_BY) {
                // Requests have been put on the Queue while we were writing, The Queue thread waiting to service them can start now
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "notifyStandBy doing notify");
                }
                qSync.notify();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.h2internal.H2WorkQInterface#setToQuit(boolean)
     */
    @Override
    public void setToQuit(boolean inDrainQ) {
        synchronized (qSync) {
            qStatus = Q_STATUS.QUIT;
            drainQ = inDrainQ;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.h2internal.H2WorkQInterface#writeOrAddToQ(com.ibm.ws.http.channel.h2internal.H2WriteQEntry)
     */
    @Override
    public WRITE_ACTION writeOrAddToQ(H2WriteQEntry entry) throws FlowControlException {
        synchronized (qSync) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "process write entry with qStatus: " + qStatus + " entry: " + entry.hashCode());
            }

            if (qStatus == Q_STATUS.NOT_IN_USE) {
                // perform the write on this thread, outside the sync block
                qStatus = Q_STATUS.BYPASSED;
                entry.setServicedOnQ(false);

            } else if ((qStatus == Q_STATUS.ACTIVE) || (qStatus == Q_STATUS.STAND_BY)) {
                // queue is in use, so add this write entry to the queue/tree and leave
                entry.setServicedOnQ(true);
                addEntryToQ(entry);
                return WRITE_ACTION.QUEUED;

            } else if (qStatus == Q_STATUS.BYPASSED) {
                // queue is not in use, but needs to be now, since we are want to write with a write outstanding.
                entry.setServicedOnQ(true);
                qStatus = Q_STATUS.STAND_BY;
                addEntryToQ(entry);
                // wait for outstanding request to complete before allowing the queue thread to make more write requests
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "waiting to start write Q thread");
                    }
                    qSync.wait();
                    qStatus = Q_STATUS.ACTIVE;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "start write Q thread");
                    }
                    startQThread();
                    return WRITE_ACTION.QUEUED;
                } catch (InterruptedException e) {
                    // TODO: handle this somehow
                }

            } else if ((qStatus == Q_STATUS.QUIT) || (qStatus == Q_STATUS.FINISHED)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "do not process write - Q told to quit");
                }
                return WRITE_ACTION.CONNECTION_QUIT;
            }
        } // end synchronized

        // Determined from above that we are to handle this write on the callers thread
        if (!(entry.getServicedOnQ())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "perform write on caller's thread");
            }
            return writeEntry(entry);
        }

        // should never get here
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "uh-oh we're confused: WRITE_ACTION.CONFUSED");
        }
        return WRITE_ACTION.CONFUSED;
    }

    @Override
    public void addNewNodeToQ(int streamID, int parentStreamID, int priority, boolean exclusive) {

        boolean bNode = tree.findNode(streamID);

        if (bNode) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "could not add a new node, was already in the tree");
            }
            return;
        }

        Node node = new Node(streamID, priority);
        tree.addNode(node, parentStreamID, exclusive);
        return;

    }

    @Override
    public boolean removeNodeFromQ(int streamID) {

        return tree.removeNode(streamID);

    }

    private void addEntryToQ(H2WriteQEntry entry) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addEntryToQ entry: " + entry.hashCode());
        }

        // Find the node/streamID in the tree, if it is not in the tree then add the Node to the tree.
        int streamID = entry.getStreamID();
        boolean bNode = tree.findNode(streamID);
        if (!bNode) {
            // Assume no parents for nodes right now other than root.
            // Assume exclusive not being used right now.
            Node node = new Node(streamID, Node.DEFAULT_NODE_PRIORITY);
            tree.addNode(node, Node.ROOT_STREAM_ID, false);
        }

        // update node with current write object and signal that node wants to write
        tree.updateNode(streamID, NODE_STATUS.REQUESTING_WRITE, WRITE_COUNT_ACTION.NO_ACTION, entry);

    }

    private WRITE_ACTION writeEntry(H2WriteQEntry e) throws FlowControlException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "H2WriteTree entered with stream-id: " + e.getStreamID() + " entry hc:" + e.hashCode());
        }

        if (e.getFrameType() == FrameTypes.DATA) {
            if (connectionWindowUpdateWriteLimit - e.getPayloadLength() < 0) {
                // would exceed window update limit
                String s = " connectionWindowUpdateWriteLimit " + connectionWindowUpdateWriteLimit +
                           " connectionWindowUpdateWriteInitialSize " + connectionWindowUpdateWriteInitialSize;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Can not write Data would exceed connection window update limit: " + s);
                }
                FlowControlException ex = new FlowControlException("Can not write Data would exceed connection window update limit: " + s);
                throw ex;
            }
        }

        // where we actually do the write at the TCP Channel layer
        VirtualConnection vc = null;

        // Since only one write can be outstanding at one time, we can tell the callback what entry is now being written.  callback will need this later
        muxCallback.setCurrentQEntry(e);

        // put the write buffers into the TCP Channel write context, so they can be written on the wire
        if (e.getBuffer() != null) {
            writeReqContext.setBuffer(e.getBuffer());
        } else {
            writeReqContext.setBuffers(e.getBuffers());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "update node for stream-id: " + e.getStreamID());
        }

        // attempt to write will count as a write action for this entry
        tree.updateNode(e.getStreamID(), NODE_STATUS.ACTION_NO_CHANGE, WRITE_COUNT_ACTION.INCREMENT, null);

        // The write is always async at the TCP Channel so we are not hanging threads
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "tell device channel to write the data on stream-id: " + e.getStreamID());
        }

        vc = writeReqContext.write(e.getMinToWrite(), muxCallback, e.getForceQueue(), e.getTimeout());

        if (vc != null) {
            // write worked right away
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "write worked right away");
            }

            if (e.getFrameType() == FrameTypes.DATA) {
                connectionWindowUpdateWriteLimit -= e.getPayloadLength();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Data payload written - new connectionWindowUpdateWriteLimit: " + connectionWindowUpdateWriteLimit);
                }

            }

            if (!e.getServicedOnQ() && (e.getForceQueue() == false)) {

                // this is the caller's thread and force queue is not true
                // so return with the successful write after processing the queue state
                tree.updateNode(e.getStreamID(), NODE_STATUS.NOT_REQUESTING, WRITE_COUNT_ACTION.NO_ACTION, null);

                notifyStandBy();

                return WRITE_ACTION.COMPLETED;
            }

            // on the queue servicing thread  and/or forcequeue (which is only allowed on for Async) is true
            // if sync write completed right away, then release the thread that is waiting for this write to complete
            if (e.getWriteType() == H2WriteQEntry.WRITE_TYPE.SYNC) {

                tree.updateNode(e.getStreamID(), NODE_STATUS.NOT_REQUESTING, WRITE_COUNT_ACTION.NO_ACTION, null);
                e.hitWriteCompleteLatch();

                return WRITE_ACTION.COMPLETED;

            } else {
                // an Async write completed right away
                // we are either on the queue service thread, or forceQueue is true

                // call the callback for this write using a new thread, the complete/error methods on the callback will check if the
                // queue service thread is in standby/wait (only relevant here for the force queue case)

                // TODO: need to give this thread the right metaData/context?

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "start a new thread to service the async callback");
                }

                // callback will update the entry status
                ExecutorService executorService = CHFWBundle.getExecutorService();
                AsyncCallback ac = new AsyncCallback(e);
                executorService.execute(ac);

                // if not on the service queue, notify in case service queue thread is now waiting
                if (!e.getServicedOnQ()) {
                    notifyStandBy();
                }

                // channel write is complete and callback is now asynchronous to that Q service thread, so Q service thread can do another write.
                return WRITE_ACTION.COMPLETED;
            }

        } else {
            // not all the data written right away
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "write did not work right away");
            }
            if (e.getWriteType() == H2WriteQEntry.WRITE_TYPE.SYNC) {
                // caller or queue service thread needs to wait until this write finishes since it is a sync write

                // wait for the mux write callback to be processed, which will also update that Q status
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeEntry - WRITE_TYPE.SYNC - call entry.waitWriteCompleteLatch");
                }

                e.waitWriteCompleteLatch();
                tree.updateNode(e.getStreamID(), NODE_STATUS.NOT_REQUESTING, WRITE_COUNT_ACTION.NO_ACTION, null);

                // caller or queue service thread moves on to next piece of work
                return WRITE_ACTION.COMPLETED;

            } else {
                // Async Write, and it did not write all the data right away

                if (!e.getServicedOnQ()) {

                    // this is the caller's thread, so return with the write pending.
                    // mux callback will update the Q status and call caller's callback  when it completes
                    return WRITE_ACTION.PENDING_CALLBACK;

                } else {

                    // queue service thread needs to wait for async write to complete before performing next write (can not have two writes
                    // outstanding on the TCP Channel)
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "writeEntry - WRITE_TYPE.ASYNC - call entry.waitWriteCompleteLatch");
                    }
                    e.waitWriteCompleteLatch();
                    return WRITE_ACTION.COMPLETED;
                }
            }
        }

    }

    @Override
    public boolean updateNodeFrameParameters(int streamID, int newPriority, int newParentStreamID, boolean exclusive) {
        return tree.updateNodeFrameParameters(streamID, newPriority, newParentStreamID, exclusive);
    }

    @Override
    public void asyncCallbackComplete(H2WriteQEntry e) {

        tree.updateNode(e.getStreamID(), NODE_STATUS.NOT_REQUESTING, WRITE_COUNT_ACTION.NO_ACTION, null);

    }

    @Override
    public void incrementConnectionWindowUpdateLimit(int x) throws FlowControlException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "incrementConnectionWindowUpdateLimit entry: inc value: " + x
                         + "current connectionWindowUpdateWriteLimit: " + connectionWindowUpdateWriteLimit);
        }
        int temp = connectionWindowUpdateWriteLimit + x;
        temp = temp & 0x10000000;
        if (temp != 0) {
            String s = "processWindowUpdateFrame: out of bounds increment, current connection write limit: " + connectionWindowUpdateWriteLimit
                       + " total would have been: " + temp;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, s);
            }

            FlowControlException e = new FlowControlException(s);
            throw e;
        }

        connectionWindowUpdateWriteLimit += x;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "connectionWindowUpdateWriteLimit updated to: " + connectionWindowUpdateWriteLimit);
        }
    }

    @Override
    public void decreaseConnectionWindowUpdateWriteLimit(int x) {

        connectionWindowUpdateWriteLimit -= x;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "decreaseConnectionWindowUpdateWriteLimit: dec value: " + x
                         + "connectionWindowUpdateWriteLimit decreased to: " + connectionWindowUpdateWriteLimit);
        }

    }

    protected void startQThread() {
        ExecutorService executorService = CHFWBundle.getExecutorService();
        QOwner qOwner = new QOwner();
        executorService.execute(qOwner);
    }

    protected class QOwner implements Runnable {

        @Override
        public void run() {

            try {
                while (true) {

                    H2WriteQEntry e = null;

                    synchronized (qSync) {
                        if ((qStatus == Q_STATUS.QUIT) && (drainQ == false)) {
                            // quit immediately if told to do so
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Write Q thread told to quite without drain - finished");
                            }
                            qStatus = Q_STATUS.FINISHED;
                            return;
                        }

                        // on to the next entry.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Write Q getting next entry");
                        }
                        e = findNext();

                        // if no more entries are waiting to write then leave
                        if (e == null) {
                            if (qStatus == Q_STATUS.QUIT) {
                                // done for good.
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Write Q thread told to quite and queue is empty - finished");
                                }
                                qStatus = Q_STATUS.FINISHED;
                                return;
                            }

                            // queue servicing thread will leave now, of course another one could start up again later
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Write Q empty so thread is leaving");
                            }

                            qStatus = Q_STATUS.NOT_IN_USE;
                            return;
                        }
                    }

                    // we have an entry to write, want to do so outside the synchronized block
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Write Q perform write Q thread");
                    }

                    writeEntry(e);

                }

            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Write Q caught a Throwable.  Set Q status to Q_STATUS.FINISHED and leave: " + t);
                }
                // add debug
                // something went really wrong, log and leave
                qStatus = Q_STATUS.FINISHED;
            }
        }
    }

    private H2WriteQEntry findNext() {
        // look in the tree for the next one give the priorities and write counts per stream
        H2WriteQEntry e = tree.findNextWriteEntry();
        return e;
    }

    protected class AsyncCallback implements Runnable {

        // A seperate thread is needed to call the user's complete callback for this async request the finished either on the queue servicing thread or
        // had forceQueue set to true.

        H2WriteQEntry e;

        protected AsyncCallback(H2WriteQEntry x) {
            e = x;
        }

        @Override
        public void run() {

            // this is used instead of the H2MuxTCPWriteCallback for when the async was force queue and completed right away and/or we
            // are on the Q service thread, and the async write complete right away.

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "New thread to service callback for entry: " + e.hashCode());
            }

            try {
                // use the VC and context that the calling thread/H2 Stream is using, not the mux ones.
                VirtualConnection eVC = e.getConnectionContext().getVC();
                TCPWriteRequestContext eTWC = e.getConnectionContext().getWriteInterface();

                e.getCallback().complete(eVC, eTWC);
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "caught a Throwable. log and leave: " + t);
                }
                // debug, not much else to do but stop the exception here
            }

            // update the entry status, write was already incremented
            asyncCallbackComplete(e);

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.h2internal.H2WorkQInterface#getConnectionWriteLimit()
     */
    @Override
    public int getConnectionWriteLimit() {
        return connectionWindowUpdateWriteLimit;
    }

}
