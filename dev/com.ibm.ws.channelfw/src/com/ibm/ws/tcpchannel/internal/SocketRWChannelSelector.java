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
import java.net.SocketTimeoutException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Selector used for socket read and write completions.
 */
public class SocketRWChannelSelector extends ChannelSelector implements Runnable {
    private static final TraceComponent tc = Tr.register(SocketRWChannelSelector.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    private WorkQueueManager wqm = null;
    private int countIndex = -1;
    private final int channelType;
    private final int wakeupOption;
    private boolean wakeupNeeded = false;

    /**
     * Create a new SocketRWChannelSelector.
     * 
     * @param _wakeupOption
     *            specifies the algorithm to use to decide if the
     *            selector should be woken up after adding work to its work queue
     * @param _wqm
     *            Work queue that this thread is to service.
     * @param _index
     *            the index within a group of the same type of selector that
     *            this one is. This is used for pruning.
     * @param _channelType
     *            what kind of channel this object is serving
     * @param _checkCancel
     *            pass to the super constructor
     * @throws IOException
     */
    protected SocketRWChannelSelector(int _wakeupOption, WorkQueueManager _wqm, int _index, int _channelType, boolean _checkCancel) throws IOException {
        super(_checkCancel);
        this.wqm = _wqm;
        this.countIndex = _index;
        this.channelType = _channelType;
        this.wakeupOption = _wakeupOption;
        this.wakeupNeeded = (_wakeupOption == ValidateUtils.SELECTOR_WAKEUP_WHEN_NEEDED);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Created RW selector: " + this);
        }
    }

    /*
     * @see
     * com.ibm.ws.tcpchannel.internal.ChannelSelector#addWork(java.lang.Object)
     */
    @Override
    protected void addWork(Object toAdd) {
        addToWorkQueue(toAdd);
        if (wakeupNeeded || (wakeupOption == ValidateUtils.SELECTOR_WAKEUP_IF_NO_FORCE_QUEUE && ((TCPBaseRequestContext) toAdd).isForceQueue() == false)) {
            if (wakeupPending != true) {
                wakeupPending = true;
                wakeup();
            }
        }
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#performRequest()
     */
    @Override
    protected boolean performRequest() {
        VirtualConnection vci = null;
        boolean completeOperation = true;
        TCPBaseRequestContext req = null;
        SelectionKey selectedKey = null;

        // If we were woken up because we have work to do, do it.
        Set<SelectionKey> keySet = selector.selectedKeys();
        Iterator<SelectionKey> selectedIterator = keySet.iterator();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "performRequest - processing " + keySet.size() + " items");
        }
        while (selectedIterator.hasNext()) {
            selectedKey = selectedIterator.next();
            // safely remove from set while looping
            selectedIterator.remove();
            req = (TCPBaseRequestContext) selectedKey.attachment();
            vci = req.getTCPConnLink().getVirtualConnection();
            completeOperation = true;

            // looking at the vci is messy here, but we're doing the try, catch
            // to save synchronization logic. vci could be null if connection
            // is closing before we can asked to request permission to finish
            // the read.
            if (vci == null) {
                completeOperation = false;
            } else {
                // only check if operation was an async read/write request
                if (vci.isInputStateTrackingOperational() && !req.blockedThread) {
                    completeOperation = false;
                    if (req.isRequestTypeRead()) {
                        if (vci.requestPermissionToFinishRead()) {
                            completeOperation = true;
                        }
                    } else {
                        if (vci.requestPermissionToFinishWrite()) {
                            completeOperation = true;
                        }
                    }
                }
            }

            if (completeOperation) {
                // try to dispatch request. if it fails, just leave key alone,
                // and it should get selected and tried again the next time through
                // the selector.
                if (wqm.dispatch(req, null)) {
                    // Dispatch worked, so set key's interest set to empty, so it isn't
                    // re-selected
                    // until the user asks us to do more work.
                    try {
                        selectedKey.interestOps(0);
                    } catch (CancelledKeyException cke) {
                        // ignore, since we already got the key and the data
                    }
                }
            } else {
                // permission denied, assume close has been processed on this VC
                // therefore disregard this read.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "IO cancelled on closed key " + selectedKey);
                }
                try {
                    selectedKey.interestOps(0);
                } catch (CancelledKeyException cke) {
                    // ignore, since the vc is closed/ing
                }
            }
        }

        return false;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#updateSelector()
     */
    @Override
    protected void updateSelector() {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        VirtualConnection vci = null;
        NioSocketIOChannel ioSocket = null;
        TCPBaseRequestContext work = null;

        final Queue<Object> queue = getWorkQueue();
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "updateSelector - processing " + queue.size() + " items");
        }
        while (!queue.isEmpty()) {
            work = (TCPBaseRequestContext) queue.remove();
            ioSocket = (NioSocketIOChannel) work.getTCPConnLink().getSocketIOChannel();
            vci = work.getTCPConnLink().getVirtualConnection();
            // if connection is closed from another thread (like during tcpchannel
            // stop)
            // the vc can become null. if so, ignore this request
            if (vci == null) {
                if (bTrace && tc.isEventEnabled()) {
                    SocketChannel channel = ioSocket != null ? ioSocket.getChannel() : null;
                    Tr.event(this, tc, "Ignoring due to null vc on " + ((channel != null) ? channel.toString() : "<UNKNOWN>"));
                }
                continue;
            }

            final int selectorOp;
            if (work.isRequestTypeRead()) {
                selectorOp = SelectionKey.OP_READ;
            } else {
                selectorOp = SelectionKey.OP_WRITE;
            }
            // if key is already set, then this channel is already registered
            // so, we can just change the interest ops
            SelectionKey key = getKey(ioSocket.getChannel());
            if (key != null) {
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(this, tc, "changing interest ops for channel " + ioSocket.getChannel() + " to " + selectorOp + " for key " + key);
                }

                if (vci.isInputStateTrackingOperational()) {
                    synchronized (vci.getLockObject()) {
                        // Change it interestOp from none, to the correct operation
                        // Change the request's selection key's interested
                        key.interestOps(selectorOp);

                        if (work.isRequestTypeRead()) {
                            if (((TCPReadRequestContextImpl) work).getReadCompletedCallback() != null) {
                                // only allow closes for outstanding async requests
                                vci.setReadStatetoCloseAllowedNoSync();
                            }
                        } else {
                            if (((TCPWriteRequestContextImpl) work).getWriteCompletedCallback() != null) {
                                // only allow closes for outstanding async requests
                                vci.setWriteStatetoCloseAllowedNoSync();
                            }
                        }
                        if (vci.getCloseWaiting()) {
                            vci.getLockObject().notify();
                        }
                    } // end-vc-sync
                } else {
                    // Change the request's selection key's interested
                    key.interestOps(selectorOp);
                }
            } else {
                if (work.isRequestTypeRead()) {
                    ioSocket.setChannelSelectorRead(this);
                } else {
                    ioSocket.setChannelSelectorWrite(this);
                }

                try {
                    if (vci.isInputStateTrackingOperational()) {
                        synchronized (vci.getLockObject()) {
                            SelectionKey selKey = ioSocket.register(selector, selectorOp, work);
                            if (bTrace && tc.isEventEnabled()) {
                                Tr.event(this, tc, "registered " + ioSocket.getChannel() + ", key is " + selKey);
                            }
                            updateCount();
                            if (work.isRequestTypeRead()) {
                                if (((TCPReadRequestContextImpl) work).getReadCompletedCallback() != null) {
                                    // only allow closed for outstanding async requests
                                    vci.setReadStatetoCloseAllowedNoSync();
                                }
                            } else {
                                if (((TCPWriteRequestContextImpl) work).getWriteCompletedCallback() != null) {
                                    // only allow closed for outstanding async requests
                                    vci.setWriteStatetoCloseAllowedNoSync();
                                }
                            }
                            if (vci.getCloseWaiting()) {
                                vci.getLockObject().notify();
                            }
                        } // end-vc-sync
                    } else {
                        SelectionKey selKey = ioSocket.register(selector, selectorOp, work);
                        if (bTrace && tc.isEventEnabled()) {
                            Tr.event(this, tc, "registered " + ioSocket.getChannel() + ", key is " + selKey);
                        }
                        updateCount();
                    }
                } catch (ClosedChannelException cce) {

                    boolean completeOperation = true;
                    if (bTrace && tc.isEventEnabled())
                        Tr.event(this, tc, "SocketChannel register for " + ioSocket + " failed, exception is: " + cce);

                    // only check permission for async read/write
                    if (vci.isInputStateTrackingOperational() && !work.blockedThread) {
                        completeOperation = false;
                        if (work.isRequestTypeRead()) {
                            if (vci.requestPermissionToFinishRead()) {
                                completeOperation = true;
                            }
                        } else {
                            if (vci.requestPermissionToFinishWrite()) {
                                completeOperation = true;
                            }
                        }
                    }

                    if (completeOperation) {
                        if (!wqm.dispatch(work, cce)) {
                            // error could not be dispatched, so put request back on work
                            // queue
                            // and let it fail again later
                            addWork(work);
                        }
                    }
                    continue;
                }
            }
            if (work.hasTimeout()) {
                if (work.getTimeoutTime() < nextTimeoutTime) {
                    nextTimeoutTime = work.getTimeoutTime();
                }
            }
        } // end-while

        // if we found any work, reset quit flags
        if (null != work) {
            waitingToQuit = false;
            quit = false;
        }
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#updateCount()
     */
    @Override
    protected void updateCount() {
        // set counter to the actual value, rather than to try and
        // keep track with a seperate variable.
        // This counter must only be updated by the selector thread,
        // which allows us to omit synchronized logic.
        // there can be timing windows where before we signal a deletion,
        // another request comes in, but the timeout logic will insure
        // that we don't delete an active selector.
        final int selectorCount = selector.keys().size();
        if (selectorCount > 0) {
            // if any keys, don't let selector die
            waitingToQuit = false;
        }
        wqm.updateCount(countIndex, selectorCount, channelType);
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#channelSelectorClose()
     */
    @Override
    protected void channelSelectorClose() {
        synchronized (wqm.shutdownSync) {
            try {
                selector.close();
            } catch (IOException e) {
                // No FFDC code or exception handling needed since we are shutting down
            }
            // mark this channel's CS index as inactive.
            // This index will now available for a new CS entry.
            wqm.updateCount(countIndex, WorkQueueManager.CS_NULL, channelType);
        }
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#checkForTimeouts()
     */
    @Override
    protected void checkForTimeouts() {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();

        // See if anything may have timed out
        final long now = super.currentTime;
        if (now < nextTimeoutTime) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "checkForTimeouts bypassing timeout processing");
            }
            return;
        }

        Set<SelectionKey> selectorKeys = selector.keys();
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "checkForTimeouts - checking " + selectorKeys.size() + " keys for timeouts");
        }
        if (selectorKeys.isEmpty()) {
            // if this isn't the primary (first) selector, see if it should be closed
            if (countIndex > 0) {
                // if we have already been waiting, and still no keys, and this
                // isn't the primary selector, we should close this
                if (waitingToQuit) {
                    quit = true;
                } else {
                    wqm.updateCount(countIndex, WorkQueueManager.CS_DELETE_IN_PROGRESS, channelType);
                    waitingToQuit = true;
                    nextTimeoutTime = now + TCPFactoryConfiguration.getChannelSelectorWaitToTerminate();
                }
            } else {
                nextTimeoutTime = now + TCPFactoryConfiguration.getChannelSelectorIdleTimeout();
            }
        } else {
            waitingToQuit = false;
            // go through requests, timing out those that need it, and calculating new
            // timeout
            // set nexttimeouttime to whatever the selectoridletimeout is, and work
            // back from there
            nextTimeoutTime = now + TCPFactoryConfiguration.getChannelSelectorIdleTimeout();
            for (SelectionKey key : selectorKeys) {
                try {
                    if (key.interestOps() <= 0) {
                        // skip if not waiting on read/writes
                        continue;
                    }
                    TCPBaseRequestContext req = (TCPBaseRequestContext) key.attachment();
                    if (!req.hasTimeout()) {
                        // skip if read/write does not have a timeout
                        continue;
                    }
                    if (req.getTimeoutTime() <= now) {
                        if (bTrace && tc.isEventEnabled()) {
                            Tr.event(this, tc, "Inactivity timeout on channel " + req.getTCPConnLink().getSocketIOChannel().getChannel());
                        }

                        // create timeout exception to pass to callback error method
                        IOException e = new SocketTimeoutException("Socket operation timed out before it could be completed");
                        if (wqm.dispatch(req, e)) {
                            // reset interest ops so selector won't fire again for this key
                            key.interestOps(0);
                        } else {
                            // try again in another second
                            nextTimeoutTime = now;
                        }
                    } else {
                        // adjust times for 1 second granularity of ApproxTime
                        if (req.getTimeoutTime() < nextTimeoutTime) {
                            nextTimeoutTime = req.getTimeoutTime();
                        }
                    }
                } catch (CancelledKeyException cke) {
                    // either we didn't get the key, or we already dispatched the
                    // error. In either case, there is nothing more to do
                    continue;
                }
            } // end-key-loop
        }
    }

}
