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
package com.ibm.io.async;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;

/**
 * AIO timer class.
 */
public final class Timer extends Thread {

    /**
     * Time in milliseconds for which a slot will be pruned if:
     * 1. no entries have been added in this amount of time
     * 2. there are not "Active" entries in the list.
     */
    public final static long TIMESLOT_PRUNING_THRESHOLD = 4096;

    /**
     * Since the approx timer has a 1000 msec resolution, if this
     * value is more than 1000, then we end up checking only once
     * every 2 seconds.
     */
    public final static long TIMEOUT_CHECKING_THRESHOLD = 1000;

    /**
     * First element in our slot list. List is maintained by this class,
     * and only the run thread can access this list.
     */
    private TimeSlot firstSlot = null;

    /**
     * Last element in our slot list. List is maintained by this class,
     * and only the run thread can access this list.
     */
    private TimeSlot lastSlot = null;

    /**
     * The resolution that timeouts and slots will be aware of. A value of
     * 0xFFFFFC00 represents 1024 mseconds resolution.
     */
    static public final long timeoutResolution = 0xFFFFFC00;

    /**
     * The round-up value that corresponds to the above resolution.
     * 
     */
    static public final long timeoutRoundup = 0x00000400;

    /**
     * the work queue from which the Timer task we receive work.
     */
    private final TimerLinkedList requestQueue1 = new TimerLinkedList();
    private final TimerLinkedList requestQueue2 = new TimerLinkedList();

    private int queueToUse = 1;
    static private int numQueues = AsyncProperties.numTimerQueues;

    static {
        // if numQueues value is more than supported, change it to the max (2)
        if (numQueues > 2) {
            numQueues = 2;
        }
    }

    private static final TraceComponent tc = Tr.register(Timer.class,
                                                         TCPChannelMessageConstants.TCP_TRACE_NAME,
                                                         TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     */
    public Timer() {
        // if timeouts are disabled, do nothing
        if (AsyncProperties.disableTimeouts) {
            return;
        }

        this.setName("AIO Timer Thread 1");
        this.setDaemon(true);
        this.start();
    }

    /**
     * Creates a work item and puts it on the work queue for requesting a
     * timeout to be started.
     * 
     * @param timeoutTime
     *            how long this timeout is for in milliseconds.
     * @param _callback
     *            the routine to be called when/if the timeout triggers
     * @param _future
     *            attachment to be passed to the callback routine
     * @return the work item that was queue'd to the timer task. This item will
     *         be needed if it is to be cancelled later.
     */
    public TimerWorkItem createTimeoutRequest(long timeoutTime, TimerCallback _callback, IAbstractAsyncFuture _future) {

        TimerWorkItem wi = new TimerWorkItem(timeoutTime, _callback, _future, _future.getReuseCount());

        _future.setTimeoutWorkItem(wi);

        // put this to the Timer's work queue. Use the queue that the Timer
        // thread is not using.
        if ((this.queueToUse == 1) || (numQueues == 1)) {
            synchronized (this.requestQueue1) {
                // add the request to the Timer work queue
                this.requestQueue1.add(wi);
            }
        } else {
            synchronized (this.requestQueue2) {
                // add the request to the Timer work queue
                this.requestQueue2.add(wi);
            }
        }

        return wi;
    }

    /**
     * Timer thread that does the following:
     * 1. check for timeouts and call the callback routines of the timed-out
     * request
     * 2. check if there are slots in the time slot list that can be deleted
     * 3. dequeue work items and perform the necessary work. The current
     * work that can be done is to start time out requests or to cancel
     * time out requests.
     */
    @Override
    public void run() {

        int numItemsToWorkOn = 0;
        long lastPruneTime = 0;
        long lastCheckTime = 0;
        int listSize = 1;
        int i = 0;
        int sleepTime = AsyncProperties.timerSleepTime;
        int batchLimitSize = AsyncProperties.timerBatchSize;
        boolean sleepAlways = AsyncProperties.timerSleepAlways;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Timeout thread running with the following parameters");
            Tr.debug(tc, "numQueues:      " + numQueues);
            Tr.debug(tc, "sleepAlways:    " + sleepAlways);
            Tr.debug(tc, "sleepTime:      " + sleepTime);
            Tr.debug(tc, "batchLimitSize: " + batchLimitSize);
        }

        TimerWorkItem[] workItems = new TimerWorkItem[batchLimitSize];

        long currentTime = CHFWBundle.getApproxTime();

        while (true) {
            if ((listSize == 0) || sleepAlways) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                    // ignore and go on
                }
            }

            numItemsToWorkOn = 0;

            if ((this.queueToUse == 1) || (numQueues == 1)) {
                this.queueToUse = 2;
                synchronized (this.requestQueue1) {
                    listSize = this.requestQueue1.size();
                    if (listSize != 0) {
                        i = 0;
                        while (i < listSize && numItemsToWorkOn < batchLimitSize) {
                            workItems[numItemsToWorkOn] = this.requestQueue1.removeFirst();

                            // only work on the entry if it hasn't been cancelled
                            if (workItems[numItemsToWorkOn].state != TimerWorkItem.ENTRY_CANCELLED) {
                                numItemsToWorkOn++;
                            }
                            i++;
                        }
                    }
                } // end synchronized (requestQueue1)
            } else {
                this.queueToUse = 1;
                synchronized (this.requestQueue2) {
                    listSize = this.requestQueue2.size();
                    if (listSize != 0) {
                        i = 0;
                        while (i < listSize && numItemsToWorkOn < batchLimitSize) {
                            workItems[numItemsToWorkOn] = this.requestQueue2.removeFirst();

                            // only work on the entry if it hasn't been cancelled
                            if (workItems[numItemsToWorkOn].state != TimerWorkItem.ENTRY_CANCELLED) {
                                numItemsToWorkOn++;
                            }
                            i++;
                        }
                    }
                } // end synchronized (requestQueue2)
            }

            // process what we took off the list
            for (i = 0; i < numItemsToWorkOn; i++) {
                // check again, since looping takes time
                if (workItems[i].state != TimerWorkItem.ENTRY_CANCELLED) {
                    insertWorkItem(workItems[i], currentTime);
                }
            }

            currentTime = CHFWBundle.getApproxTime();

            if ((currentTime - lastCheckTime > TIMEOUT_CHECKING_THRESHOLD)) {
                checkForTimeouts(currentTime);
                lastCheckTime = currentTime;
            }

            if ((currentTime - lastPruneTime) > TIMESLOT_PRUNING_THRESHOLD) {
                timeSlotPruning(currentTime);
                lastPruneTime = currentTime;
            }

        } // end while(true)
    }

    /**
     * Remove slots which have no active requests, and no new requests
     * have been added in a set amount of time.
     * 
     * @param curTime
     *            the current time in msec.
     */
    public void timeSlotPruning(long curTime) {
        // if a bucket has not been accessed in a while, and it only has
        // dead entries then get rid of it
        TimeSlot slotEntry = this.firstSlot;
        TimeSlot nextSlot = null;
        int endIndex = 0;
        int i;

        while (slotEntry != null) {
            nextSlot = slotEntry.nextEntry;

            if (curTime - slotEntry.mostRecentlyAccessedTime > TIMESLOT_PRUNING_THRESHOLD) {
                endIndex = slotEntry.lastEntryIndex;

                // entries added last are more likely to be active, so
                // go from last to first
                for (i = endIndex; i >= 0; i--) {
                    if (slotEntry.entries[i].state == TimerWorkItem.ENTRY_ACTIVE) {
                        break;
                    }
                }

                if (i < 0) {
                    // no entries are active
                    removeSlot(slotEntry);
                }
            }

            slotEntry = nextSlot;
        }
    }

    /**
     * Put a work item into an existing time slot, or create a new time
     * slot and put the work item into that time slot.
     * 
     * @param work
     * @param curTime
     */
    public void insertWorkItem(TimerWorkItem work, long curTime) {
        // find the time slot, or create a new one
        long insertTime = work.timeoutTime;
        TimeSlot nextSlot = this.firstSlot;

        while (nextSlot != null) {
            if ((insertTime == nextSlot.timeoutTime) && (nextSlot.lastEntryIndex != TimeSlot.TIMESLOT_LAST_ENTRY)) {
                nextSlot.addEntry(work, curTime);
                break;
            } else if (insertTime < nextSlot.timeoutTime) {
                nextSlot = insertSlot(insertTime, nextSlot);
                nextSlot.addEntry(work, curTime);
                break;
            } else {
                nextSlot = nextSlot.nextEntry;
            }
        }

        if (nextSlot == null) {
            nextSlot = insertSlotAtEnd(insertTime);
            nextSlot.addEntry(work, curTime);
        }
    }

    /**
     * Create a new time slot with a given timeout time, and add this new
     * time slot in front of an existing time slot in the list.
     * 
     * @param newSlotTimeout
     *            - timeout time for the new time slot
     * @param slot
     *            - existing time slot
     * @return TimeSlot - time slot that was created and added
     */
    public TimeSlot insertSlot(long newSlotTimeout, TimeSlot slot) {

        // this routine assumes the list is not empty
        TimeSlot retSlot = new TimeSlot(newSlotTimeout);
        retSlot.nextEntry = slot;
        retSlot.prevEntry = slot.prevEntry;

        if (retSlot.prevEntry != null) {
            retSlot.prevEntry.nextEntry = retSlot;
        } else {
            // no prev, so this is now the first slot
            this.firstSlot = retSlot;
        }

        slot.prevEntry = retSlot;
        return retSlot;
    }

    /**
     * Create a new time slot with a given timeout time, and add this new
     * time slot at the end of the time slot list.
     * 
     * @param newSlotTimeout
     *            - timeout time for the new slot
     * @return time slot that was created and added
     */
    public TimeSlot insertSlotAtEnd(long newSlotTimeout) {
        // this routine assumes that list could be empty
        TimeSlot retSlot = new TimeSlot(newSlotTimeout);
        if (this.lastSlot == null) {
            // list was empty
            this.lastSlot = retSlot;
            this.firstSlot = retSlot;
        } else {
            retSlot.prevEntry = this.lastSlot;
            this.lastSlot.nextEntry = retSlot;
            this.lastSlot = retSlot;
        }
        return retSlot;
    }

    /**
     * Remove a time slot from the list.
     * 
     * @param oldSlot
     */
    public void removeSlot(TimeSlot oldSlot) {

        if (oldSlot.nextEntry != null) {
            oldSlot.nextEntry.prevEntry = oldSlot.prevEntry;
        } else {
            // old slot was tail.
            this.lastSlot = oldSlot.prevEntry;
        }

        if (oldSlot.prevEntry != null) {
            oldSlot.prevEntry.nextEntry = oldSlot.nextEntry;
        } else {
            // oldSlot was head.
            this.firstSlot = oldSlot.nextEntry;
        }

    }

    /**
     * Check for timeouts in the time slot list.
     * 
     * @param checkTime
     *            to use for calculation if time outs have occurred.
     */
    public void checkForTimeouts(long checkTime) {
        TimeSlot nextSlot = this.firstSlot;
        TimerWorkItem entry = null;
        TimeSlot oldSlot = null;

        while (nextSlot != null && checkTime >= nextSlot.timeoutTime) {
            // Timeout all entries here
            int endIndex = nextSlot.lastEntryIndex;
            for (int i = 0; i <= endIndex; i++) {
                // invoke callback if not already cancelled
                entry = nextSlot.entries[i];

                if (entry.state == TimerWorkItem.ENTRY_ACTIVE) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found a timeout, calling timerTriggered");
                    }
                    entry.state = TimerWorkItem.ENTRY_CANCELLED;
                    entry.callback.timerTriggered(entry);
                }
            }

            // since we timed out all slot entries, remove it
            oldSlot = nextSlot;
            removeSlot(oldSlot);
            nextSlot = nextSlot.nextEntry;
        }
    }
}
