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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * Basic selector class used by the TCP channel.
 */
public abstract class ChannelSelector implements Runnable, FFDCSelfIntrospectable {

    private static final TraceComponent tc = Tr.register(ChannelSelector.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    protected static final long TEN_MINUTES = 600000L;

    // Set to true to cause us to drop out of the loop in the run method.
    protected volatile boolean quit = false;

    // Selector that this thread uses for managing IO requests
    protected Selector selector = null;

    // Should this thread yield after each time through the select loop
    // In some cases, this can help performance.
    protected boolean selectorYield = false;

    // The time at which the next thing to expire (see nextThingToExpire)
    // will expire. The unit for this value is milliseconds elapsed since
    // Jan 1st 1970 (same as System.currentTimeMillis()).
    protected long nextTimeoutTime;

    // Set to true when the thread is waiting to quit (has decided that
    // there is insufficent work to do, has waited for the prescribed
    // time for more work, but yet cannot quit as it currently has a
    // slave).
    boolean waitingToQuit = false;

    // An approximation to the value of System.currentTimeMillis() in
    // the selector loop (avoids repeatedly querying the system).
    protected long currentTime;

    private final List<CancelRequest> cancelList = new ArrayList<CancelRequest>();

    // pending updates will go onto the primary work queue under the lock,
    // while the selector thread will periodically swap them and process
    // the pending updates (while new updates will go on the other list)
    // removes from the queues are only performed by the single selector thread
    private Queue<Object> workQueue1 = null;
    private Queue<Object> workQueue2 = null;
    private final Object queueLock = new QueueLock();

    protected boolean wakeupPending = false;

    boolean checkCancel = false;

    boolean waitToAccept = false;

    /**
     * Create a new ChannelSelector.
     *
     * @param _checkCancel
     * @throws IOException
     */
    public ChannelSelector(boolean _checkCancel) throws IOException {
        this.selector = Selector.open();
        this.selectorYield = TCPFactoryConfiguration.getSelectorYield();
        this.checkCancel = _checkCancel;
        this.workQueue1 = new LinkedList<Object>();
        this.workQueue2 = new LinkedList<Object>();
    }

    /**
     * Create a new ChannelSelector.
     *
     * @param _checkCancel
     * @throws IOException
     */
    public ChannelSelector(boolean _checkCancel, boolean _waitToAccept) throws IOException {
        this.selector = Selector.open();
        this.selectorYield = TCPFactoryConfiguration.getSelectorYield();
        this.checkCancel = _checkCancel;
        this.workQueue1 = new LinkedList<Object>();
        this.workQueue2 = new LinkedList<Object>();
        waitToAccept = _waitToAccept;
    }

    /**
     * Loops, servicing requests
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        boolean cancelledDone = false;
        boolean forceSelect = false;
        long selectTimeoutValue = 0L;
        int numEmptySelects = 0;
        int numExceptions = 0;
        boolean nothingTimedOut = true;
        long firstEmptySelectorTime = 0L;
        int numKeysOnEmptySelect = -1;
        long lastEmptySelectorFFDCTime = 0L;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "selector thread started for " + Thread.currentThread().getName() + " ");
        }

        if (!waitToAccept) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "waiting for server started signal");
            }
            CHFWBundle.waitServerCompletelyStarted();
        }

        nextTimeoutTime = currentTime + TCPFactoryConfiguration.getChannelSelectorIdleTimeout();

        while (!quit) {
            currentTime = CHFWBundle.getApproxTime();
            try {
                nothingTimedOut = true;
                // yielding increases performance by not doing too many selects
                // this gives 10% performance boost on Daves system, 10% performance
                // loss on Tony's
                if (selectorYield) {
                    Thread.yield();
                }

                wakeupPending = false;
                if (checkCancel && forceSelect) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "selectNow() forced");
                    }
                    selector.selectNow();
                    forceSelect = false;
                    numEmptySelects = 0;
                } else if (nextTimeoutTime > currentTime) {
                    // don't use a timeout of less than 1 second, since thats the
                    // granularity of ApproxTime
                    selectTimeoutValue = nextTimeoutTime - currentTime;
                    if (selectTimeoutValue < 1000L) {
                        selectTimeoutValue = 1000L;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "select() with timeout = " + selectTimeoutValue);
                    }
                    selector.select(selectTimeoutValue);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "selectNow()");
                    }
                    selector.selectNow();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                      Tr.debug(this, tc, "selectNow() returned. quit="+quit);
                    }
                    nothingTimedOut = false;
                    numEmptySelects = 0;
                }
                wakeupPending = false;
                // if any keys were cancelled on the last time through
                // the selector should now be updated, so notfiy anyone
                // anyone waiting on the cancels
                if (checkCancel && cancelledDone) {
                    notifyCancelRequests();
                    cancelledDone = false;
                }

                currentTime = CHFWBundle.getApproxTime();

                // need to process cancel requests before add requests, because we might
                // be trying to add one back in that we previously cancelled. We also don't want
                // to include keys that have been cancelled in our next-to-timeout logic
                if (checkCancel) {
                    cancelledDone = processCancelRequests();
                    if (cancelledDone) {
                        forceSelect = true;
                        continue;
                    }
                }

                // the following logic is for detecting looping problems in the JDK
                // it will log an ffdc event if the selector fires in quick succession
                if (selector.selectedKeys().isEmpty() && nothingTimedOut && areQueuesEmpty()) {
                    if (numEmptySelects == 0) {
                        firstEmptySelectorTime = currentTime;
                        numKeysOnEmptySelect = selector.keys().size();
                    }
                    numEmptySelects++;
                    if (numEmptySelects == 40) {
                        // selector fired 40 times with nothing selected, see if it was in quick succession and key set hasn't changed

                        // don't ffdc until it has been going on for a while
                        if (lastEmptySelectorFFDCTime != 0) {
                            if (currentTime < (firstEmptySelectorTime + 10000L) && selector.keys().size() == numKeysOnEmptySelect) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                    Tr.event(this, tc, "Selector fired 40 times in a row with no keys selected - possible loop in JDK detected");
                                }
                                // TODO seems to happen sometimes if the socket is closed but not
                                // removed from the keys (Windows JDK 1.6 SR7 at least)... could scan and remove
                                // closed sockets the SocketChannel.close() is supposed to remove it from
                                // selectors but TCP has a checkcancel config option to manually do it (since it's
                                // been broken in the past apparently)

                                // create an exception to be logged in FFDC, but limit to once
                                // every 5 minutes
                                if (currentTime > (lastEmptySelectorFFDCTime + 300000L)) {
                                    ChannelException e = new ChannelException("TCP Channel detected a possible loop on thread: " + Thread.currentThread().getName());
                                    FFDCFilter.processException(e,
                                                                getClass().getName(),
                                                                "186",
                                                                this,
                                                                new Object[] { "Last FFDC time=" + lastEmptySelectorFFDCTime, "Current time=" + currentTime,
                                                                               "Next timeout time=" + nextTimeoutTime,
                                                                               "First empty select time=" + firstEmptySelectorTime,
                                                                               "First empty select keys=" + numKeysOnEmptySelect,
                                                                               "Number empty selects=" + numEmptySelects, "Thread interrupted=" + Thread.interrupted(),
                                                                               this.workQueue1,
                                                                               this.workQueue2 });
                                    lastEmptySelectorFFDCTime = currentTime;
                                }
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                Tr.event(this, tc, "Selector fired 40 times in a row with no keys selected - possible loop in JDK detected");
                            }
                            lastEmptySelectorFFDCTime = currentTime;
                        }
                        numEmptySelects = 0;
                    } // end-if-20
                } else {
                    numEmptySelects = 0;
                    lastEmptySelectorFFDCTime = 0;
                }
                // end of loop detection code

                if (performRequest()) {
                    // connect logic cancels keys and returns true
                    continue;
                }

                updateCount();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                  Tr.debug(this, tc, "ChannelSelector.quit="+quit+" before call to checkForTimeouts()");
                }
                checkForTimeouts();
                updateSelector();

                // if we got here, we didn't get an IOException
                numExceptions = 0;
            } catch (IOException e) {
                // an IOException can happen due to jdk errors or if we run out of sockets
                // Don't log the exception during server shutdown
                if (!!!com.ibm.wsspi.kernel.service.utils.FrameworkState.isStopping()) {
                    FFDCFilter.processException(e, getClass().getName(), "288", this);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "IOException (" + numExceptions + ") in run " + e);
                }
                numExceptions++;

            } catch (Throwable unexpectedException) {
                // Don't log the exception during server shutdown
                if (!!!com.ibm.wsspi.kernel.service.utils.FrameworkState.isStopping()) {
                    FFDCFilter.processException(unexpectedException, getClass().getName(), "254", this);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Exception (" + numExceptions + ") in run " + unexpectedException);
                }
                // force selector to fire immediately, since an event or wakeup call
                // could be missed
                nextTimeoutTime = currentTime;
                numExceptions++;
            }

            // if we don't recover after 5 minutes, stop accepting for a long-ish while.
            // allow for recovery of the system, reconfiguration, or server stop.
            if (numExceptions >= 400) {
                if (pauseAccept()) {
                    numExceptions = 0; // start counting over again
                }
            } else if (numExceptions >= 100) {
                // if we get exceptions repeatedly, wait a second to give other
                // threads a chance to close sockets or update selector
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // ignore it
                }
            }
        } // end-while

        channelSelectorClose();
    }

    protected boolean pauseAccept() {
        Tr.error(tc, "TCP_NOT_ACCEPTING");
        ChannelException exitEx = new ChannelException("TCP Channel detected continuous exceptions while trying to accept connections and is pausing accepts for thread: "
                                                       + Thread.currentThread().getName());
        FFDCFilter.processException(exitEx, getClass().getName(), "278", this);
        try {
            Thread.sleep(TEN_MINUTES);
            return true;
        } catch (InterruptedException ie) {
            return false;
        }
    }

    abstract void updateCount();

    protected void shutDown() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
          StringBuilder sb = new StringBuilder();
          StackTraceElement st[] = Thread.currentThread().getStackTrace();
          for (StackTraceElement ste:st) sb.append(ste+"\n");
          Tr.debug(this, tc, "ChannelSelector.shutDown called from "+sb.toString());
        }
        this.quit = true;
        this.selector.wakeup();
    }

    /**
     * Check whether the pending work queue(s) are empty or not.
     *
     * @return boolean
     */
    private boolean areQueuesEmpty() {
        synchronized (this.queueLock) {
            return this.workQueue1.isEmpty() && this.workQueue2.isEmpty();
        } // end-sync
    }

    /**
     * Add a work item to the proper queue of pending updates.
     *
     * @param work
     */
    protected void addToWorkQueue(Object work) {
        synchronized (this.queueLock) {
            this.workQueue1.add(work);
        } // end-sync
    }

    /**
     * Access the possible SelectionKey on this selector for the provided
     * channel.
     *
     * @param channel
     * @return SelectionKey, null if not registered
     */
    final public SelectionKey getKey(SocketChannel channel) {
        if (null == channel) {
            return null;
        }
        return channel.keyFor(this.selector);
    }

    /**
     * Access the current work queue of pending updates for the selector. This
     * will cause further updates to go onto another queue until this method
     * is called again.
     *
     * @return Queue<Object>
     */
    protected Queue<Object> getWorkQueue() {
        synchronized (this.queueLock) {
            // swap the primary and secondary queues
            Queue<Object> tmp = this.workQueue1;
            this.workQueue1 = this.workQueue2;
            this.workQueue2 = tmp;
            return tmp;
        } // end-sync
    }

    /**
     * Override this to update/sync with specific channel information at the
     * close.
     */
    abstract void channelSelectorClose();

    /**
     * Process the keys that returned from a select, as appropriate to the
     * individual selector type.
     *
     * @return true if a key was cancelled, false if no keys were cancelled.
     */
    abstract boolean performRequest();

    /**
     * Process pending updates to the selector.
     */
    abstract void updateSelector();

    /**
     * Perform timeout logic on current keys.
     */
    abstract void checkForTimeouts();

    /*
     * @see com.ibm.ws.ffdc.FFDCSelfIntrospectable#introspectSelf()
     */
    @Override
    public String[] introspectSelf() {
        List<String> rc = new ArrayList<String>();
        rc.add(Thread.currentThread().getName());
        rc.add("quit: " + this.quit);
        rc.add("waitingToQuit: " + this.waitingToQuit);
        rc.add("# of keys=" + this.selector.keys().size());

        try {
            for (SelectionKey key : this.selector.keys()) {
                rc.add("key: " + key.hashCode() + " valid=" + key.isValid() + " ops=" + key.interestOps() + " " + key.channel());
            }

        } catch (Throwable x) {
            // If we get any exception, just return what we have so far.
            // This routine is not thread safe, and could get an exception, but
            // since it is only invoked when we are dumping data, due to a problem or
            // during debug this should be acceptable.
            rc.add("Exception Occurred Gathering Dump Data: " + x);
        }
        return rc.toArray(new String[rc.size()]);
    }

    /**
     * Add a new work item to the update queues waiting for the selector
     * to process.
     *
     * @param toAdd
     */
    protected void addWork(Object toAdd) {
        addToWorkQueue(toAdd);
        wakeup();
    }

    private void notifyCancelRequests() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "notifyCancelRequests");
        }
        CancelRequest req = null;

        synchronized (cancelList) {
            Iterator<CancelRequest> it = cancelList.iterator();
            while (it.hasNext()) {
                req = it.next();
                if (req.state == CancelRequest.Ready_To_Signal_Done) {
                    // safely remove the current item from the list
                    it.remove();
                    synchronized (req) {
                        req.state = CancelRequest.Reset;
                        req.notify();
                    }
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "notifyCancelRequests");
        }
    }

    /**
     * Process the actual key cancel requests now. This does not
     * remove the item from the list as it needs a second pass
     * through select() before notifyCancelRequests handles that step.
     *
     * @return boolean, true if any key was cancelled
     */
    private boolean processCancelRequests() {
        // Try to make this as fast as possible. Don't need to sync
        // since if a cancel gets added after checking the size, we will
        // see it next time.
        if (cancelList.size() == 0) {
            return false;
        }

        boolean needSelectToOccur = false;

        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        synchronized (cancelList) {
            for (CancelRequest req : this.cancelList) {
                if (req.state == CancelRequest.Ready_To_Cancel) {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "cancelling key " + req.key);
                    }
                    req.key.cancel();
                    req.state = CancelRequest.Ready_To_Signal_Done;
                    needSelectToOccur = true;
                }
            }
        } // end-sync
        return needSelectToOccur;
    }

    /**
     * Add a request to cancel a specific key.
     *
     * @param cr
     */
    protected void addCancelRequest(CancelRequest cr) {
        synchronized (this.cancelList) {
            this.cancelList.add(cr);
        }

        // make sure selector sees this.
        wakeup();
    }

    /**
     * Wakeup the selector.
     */
    protected void wakeup() {
        this.selector.wakeup();
    }

    /**
     * Set the next timeout marker to the provided value if appropriate.
     *
     * @param newTimeoutTime
     */
    protected void resetTimeout(long newTimeoutTime) {
        // See if anything may have timed out
        if (newTimeoutTime < this.nextTimeoutTime) {
            this.nextTimeoutTime = newTimeoutTime;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "resetTimeout waking up selector");
            }
            wakeup();
        }
    }

    /**
     * Lock for the queue access.
     */
    private static class QueueLock {
        protected QueueLock() {
            // nothing to do
        }

        /*
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Selector queue lock";
        }
    }
}
