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
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.tcpchannel.internal.ConnectionManager.ConnectInfo;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;

/**
 * Establishes out bound connections using the non-blocking API to minimise
 * thread usage.
 */
public class ConnectChannelSelector extends ChannelSelector {
    private static final TraceComponent tc = Tr.register(ConnectChannelSelector.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    protected WorkQueueManager wqm;
    private int countIndex;
    private int channelType;

    /**
     * Constructor.
     * 
     * @param _wqm
     * @param _index
     * @param _channelType
     * @throws IOException
     */
    public ConnectChannelSelector(WorkQueueManager _wqm, int _index, int _channelType) throws IOException {
        super(false);
        this.wqm = _wqm;
        this.countIndex = _index;
        this.channelType = _channelType;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Created Connect selector: " + this);
        }
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#updateSelector()
     */
    @Override
    protected void updateSelector() {
        final Queue<Object> work = getWorkQueue();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "updateSelector - processing " + work.size() + " items");
        }
        ConnectInfo ci = null;
        while (!work.isEmpty()) {
            ci = (ConnectInfo) work.remove();
            try {
                ci.channel.register(selector, SelectionKey.OP_CONNECT, ci);
                if (ci.timeout != TCPConnectRequestContext.NO_TIMEOUT) {
                    ci.nextTimeoutTime = System.currentTimeMillis() + ci.timeout;
                    if (ci.nextTimeoutTime < nextTimeoutTime) {
                        nextTimeoutTime = ci.nextTimeoutTime;
                    }
                }
                updateCount();

            } catch (ClosedChannelException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "SocketChannel connect failed, " + e + " local: " + ci.ioSocket.getSocket().getLocalSocketAddress() + " remote: "
                                       + ci.ioSocket.getSocket().getRemoteSocketAddress());
                }
                // Call a separate worker thread to call the callback. Don't hold up
                // this selector thread.
                ci.setError(e);
                if (!wqm.dispatchConnect(ci)) {
                    // error could not be dispatched, so put request back on work queue
                    // and let it fail again later
                    addWork(ci);
                }
            }
        } // process all work queue items.

        if (ci != null) {
            // if we added anything, reset quit flags
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
        // if there are no key left, then mark this selector for deletion,
        // if it is not the first selector, which we always want active.
        // This counter must only be updated by the selector thread,
        // which allows us to omit synchronized logic.
        // there can be timing windows where before we signal a deletion,
        // another request comes in, but the timeout logic will insure
        // that we don't delete an active selector.
        int selectorCount = selector.keys().size();
        if (selectorCount == 0 && countIndex != 0) {
            wqm.updateCount(countIndex, WorkQueueManager.CS_DELETE_IN_PROGRESS, channelType);
        } else {
            waitingToQuit = false; // if any keys, don't let selector die
            wqm.updateCount(countIndex, selectorCount, channelType);
        }
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
                // No FFDC code needed
            }
            // mark this channel's CS index as inactive.
            // This index will now available for a new CS entry.
            wqm.updateCount(countIndex, WorkQueueManager.CS_NULL, channelType);
        } // end-sync
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#performRequest()
     */
    @Override
    protected boolean performRequest() {

        boolean cancelledDone = false;
        // If we were worken up because we have work to do, do it.
        Set<SelectionKey> keySet = selector.selectedKeys();
        Iterator<SelectionKey> selectedIterator = keySet.iterator();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "performRequest - processing " + keySet.size() + " items");
        }
        while (selectedIterator.hasNext()) {
            SelectionKey selectedKey = selectedIterator.next();
            // safely remove key from set while looping
            selectedIterator.remove();
            ConnectInfo connectInfo = (ConnectInfo) selectedKey.attachment();
            connectInfo.setFinish();
            if (wqm.dispatchConnect(connectInfo)) {
                selectedKey.cancel();
                cancelledDone = true;
            }
        }
        return cancelledDone;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.ChannelSelector#checkForTimeouts()
     */
    @Override
    protected void checkForTimeouts() {
        // See if anything may have timed out
        if (currentTime < nextTimeoutTime) {
            return;
        }
        Set<SelectionKey> selectorKeys = selector.keys();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "checkForTimeouts - checking " + selectorKeys.size() + " keys for timeouts");
        }
        if (selectorKeys.isEmpty()) {
            // if this isn't the primary (first) selector, see if it should be closed
            if (countIndex != 0) {
                // if we have already been waiting, and still no keys, and this isn't
                // the primary
                // selector, we should close this selector/thread
                if (waitingToQuit) {
                    quit = true;
                } else {
                    wqm.updateCount(countIndex, WorkQueueManager.CS_DELETE_IN_PROGRESS, channelType);
                    waitingToQuit = true;
                    nextTimeoutTime = currentTime + TCPFactoryConfiguration.getChannelSelectorWaitToTerminate();

                }
            } else {
                nextTimeoutTime = currentTime + TCPFactoryConfiguration.getChannelSelectorIdleTimeout();
            }
        } else {
            waitingToQuit = false;
            // go through requests, timing out those that need it, and calculating new
            // timeout
            // set nexttimeouttime to whatever the selectoridletimeout is, and work
            // back from there
            nextTimeoutTime = currentTime + TCPFactoryConfiguration.getChannelSelectorIdleTimeout();

            for (SelectionKey key : selectorKeys) {
                try {
                    if (0 == key.interestOps()) {
                        // not active
                        continue;
                    }
                    // only consider keys that are currently waiting for connect
                    ConnectInfo ci = (ConnectInfo) key.attachment();

                    if (ci.timeout != TCPConnectRequestContext.NO_TIMEOUT) {
                        if (ci.nextTimeoutTime <= currentTime) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                Tr.event(this, tc, "Inactivity timeout on connect operation for channel" + ci.ioSocket.getChannel());
                            }

                            // create timeout exception to pass to callback error method
                            IOException e = new SocketTimeoutException("Socket operation timed out before it could be completed");
                            // Hand off to another thread
                            ci.setError(e);
                            if (wqm.dispatchConnect(ci)) {
                                // unable to dispatch error, handle now
                                key.cancel();
                                ci.ioSocket.close();
                            }
                        } else {
                            // see if we should timeout sooner than our current setting
                            if (ci.nextTimeoutTime < nextTimeoutTime) {
                                nextTimeoutTime = ci.nextTimeoutTime;
                            }
                        }
                    }
                } catch (CancelledKeyException cke) {
                    // either we didn't get the key, or we already dispatched the
                    // error. In either case, there is nothing more to do
                    continue;
                }
            }
        }
    }

}
