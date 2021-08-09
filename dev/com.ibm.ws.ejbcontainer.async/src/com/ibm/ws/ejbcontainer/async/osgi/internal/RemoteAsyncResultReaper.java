/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.osgi.internal;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * <code>RemoteAsyncResultReaper</code> runs as an alarm, which will cleanup
 * server-side Future objects as their timeouts expire. <p>
 *
 * The class keeps a list of all the server-side Future objects and their timeout
 * values. Whenever a server-side Future object results are available to the client
 * it is enlisted in this class. <p>
 *
 * The alarm is set to execute when the oldest element in the list of server-side
 * Future objects will timeout or 30 seconds (whichever is greater), a check is
 * performed in the order the elements were added to the list and removed if the
 * timeout was exceeded or until the oldest element in the list has not reached
 * the timeout. At that time if any elements exist in the list a new alarm is set. <p>
 */
@Trivial
public final class RemoteAsyncResultReaper implements Runnable {
    private static final TraceComponent tc = Tr.register(RemoteAsyncResultReaper.class, "EJBContainer", "com.ibm.ejs.container.container");

    private final ScheduledExecutorService ivScheduledExecutor;

    /**
     * List of server-side Future objects and their associated timeouts.
     */
    private final LinkedHashSet<RemoteAsyncResultImpl> ivAllRemoteAsyncResults;

    /**
     * The maximum number of unclaimed results before the oldest result will be
     * discarded.
     */
    private int ivMaxUnclaimedResults;

    /**
     * If the number of results exceeds this value, a warning will be issued
     * stating that the number of results is nearing the maximum. If the number
     * of results drops below below this value, then the warning for exceeding
     * the maximum will be issued if it is exceeded again.
     *
     * <p>This value is 3/4 the value of the maximum, or the maximum integer
     * value if the configured maximum is less than 1.
     */
    private int ivNearMaxResultsThreshold;

    /**
     * If the number of results drops below this value, then the warning for
     * nearing the maximum will be issued if the threshold is reached again.
     *
     * <p>This value is 1/2 the value of the maximum.
     */
    private int ivSafeResultsThreshold;

    /**
     * Timeout value for server-side Future objects, set in milliseconds.
     */
    private long ivFutureObjectTimeoutMillis;

    /**
     * True if a warning has already been issued because the number of unclaimed
     * results is near the configured maximum.
     */
    private boolean ivWarnedNearMax; // d690014.1

    /**
     * True if a warning has already been issued because the number of unclaimed
     * results has exceeded the configured maximum.
     */
    private boolean ivWarnedExceededMax; // d690014.1

    private static final long MINIMUM_ALARM_INTERVAL_MILLIS = 1000;

    /**
     * Alarm interval set in milliseconds.
     */
    private long ivAlarmIntervalMillis;

    /**
     * Used to cancel the alarm which reaps thru the list of server-side Future objects.
     */
    private boolean ivIsCanceled = false;
    private Future<?> ivFuture;

    /**
     * Constructs a <code>RemoteAsyncResultReaper</code> object, which
     * cleans up server-side Future objects whose session timeout has expired. The
     * thread runs at the specified interval (does not run more frequently
     * than once every minute). <p>
     *
     * @param timeout the timeout value for server-side Future objects in milliseconds.
     **/
    public RemoteAsyncResultReaper(ScheduledExecutorService scheduledExecutor) {
        ivScheduledExecutor = scheduledExecutor;
        ivAllRemoteAsyncResults = new LinkedHashSet<RemoteAsyncResultImpl>();
    }

    public synchronized void configure(long unclaimedResultTimeoutMillis, int maxUnclaimedResults) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "configure: unclaimedResultTimeoutMillis=" + unclaimedResultTimeoutMillis +
                         ", maxUnclaimedResults=" + maxUnclaimedResults);

        ivFutureObjectTimeoutMillis = unclaimedResultTimeoutMillis;
        ivAlarmIntervalMillis = Math.max(MINIMUM_ALARM_INTERVAL_MILLIS, ivFutureObjectTimeoutMillis);
        // We don't bother to dynamically reschedule the current alarm, but the
        // new timeout will be used for future alarms.

        ivMaxUnclaimedResults = maxUnclaimedResults;
        ivNearMaxResultsThreshold = maxUnclaimedResults / 2 + maxUnclaimedResults / 4; // d690014.1
        ivSafeResultsThreshold = maxUnclaimedResults / 2; // d690014.1

        int size = ivAllRemoteAsyncResults.size();
        if (size > maxUnclaimedResults) {
            int excess = size - maxUnclaimedResults;
            Iterator<RemoteAsyncResultImpl> iterator = ivAllRemoteAsyncResults.iterator();
            for (int i = 0; i < excess; i++) {
                RemoteAsyncResultImpl oldest = iterator.next();
                releaseResources(oldest);
                iterator.remove();
            }

            ivWarnedNearMax = false;
            ivWarnedExceededMax = false;
        }
    }

    /**
     * This method is called when an alarm scheduled with the
     * AlarmManager has expired. <p>
     *
     * This method will always be invoked on a separate thread. <p>
     *
     * Looks up a list of server-side Future objects and their associated timeouts,
     * looking for server-side Future objects whose timeouts have expired. <p>
     **/
    @Override
    public synchronized void run() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "run: size=" + ivAllRemoteAsyncResults.size());

        if (ivIsCanceled) {
            //if this instance has been canceled, we do no more processing.
            // this also guarantees that a future alarm is not created.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "run: cancelled");
            return;
        }

        ivFuture = null;

        int numRemoved = 0;

        // Re-write the alarm to use the LinkedHashSet, iterating thru the oldest entries removing
        // the server-side Future objects that have timed out until an object that hasn't reached
        // timeout or the list is empty.  This will avoid iterating over every entry in the list.  d623593
        if (!ivAllRemoteAsyncResults.isEmpty()) {
            long currentTime = System.currentTimeMillis();

            for (Iterator<RemoteAsyncResultImpl> iterator = ivAllRemoteAsyncResults.iterator(); iterator.hasNext();) {
                RemoteAsyncResultImpl asyncResult = iterator.next();

                long staleDuration = currentTime - asyncResult.getTimeoutStartTime();
                if (staleDuration >= ivFutureObjectTimeoutMillis) {
                    releaseResources(asyncResult); // d690014.3
                    iterator.remove();
                    numRemoved++;
                } else {
                    // d690014 - Schedule an alarm to be fired at the time that the
                    // next future should time out.  If that is soon, then use a
                    // minimum interval to avoid scheduling too many alarms.
                    long alarmTime = Math.max(ivFutureObjectTimeoutMillis - staleDuration, MINIMUM_ALARM_INTERVAL_MILLIS);
                    ivFuture = ivScheduledExecutor.schedule(this, alarmTime, TimeUnit.MILLISECONDS);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "next " + asyncResult + "; alarm=" + alarmTime);
                    break;
                }
            }

            // d690014.1 - If we previously issued a warning because there were
            // too many results, check if this sweep reduced the number of results
            // enough that we should warn again if the number of we get too many
            // results again in the future.
            if (ivWarnedNearMax) {
                int size = ivAllRemoteAsyncResults.size();
                ivWarnedNearMax &= size >= ivSafeResultsThreshold;
                ivWarnedExceededMax &= size >= ivNearMaxResultsThreshold;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "run: size=" + ivAllRemoteAsyncResults.size() + ", removed=" + numRemoved);
    }

    /**
     * This method is invoked just before container termination to clean
     * up server-side Future objects.
     */
    public synchronized void finalReap() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "finalReap : Remote Async Results = " + ivAllRemoteAsyncResults.size());

        // Re-write the finalReap to use the LinkedHashSet, iterating thru the list removing
        // all the entries no matter if they have timed out or not.  Cancel alarm if set.    d623593
        if (ivFuture != null) {
            ivIsCanceled = true;
            ivFuture.cancel(false);
            ivFuture = null;
        }

        for (Iterator<RemoteAsyncResultImpl> iterator = ivAllRemoteAsyncResults.iterator(); iterator.hasNext();) {
            RemoteAsyncResultImpl asyncResult = iterator.next();
            releaseResources(asyncResult); // d690014.3
            iterator.remove();
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "finalReap : Remote Async Results = " + ivAllRemoteAsyncResults.size());
    }

    /**
     * Add a new server-side Future object to the list to be checked for timeouts
     */
    public synchronized void add(RemoteAsyncResultImpl asyncResult) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        ivAllRemoteAsyncResults.add(asyncResult);
        if (asyncResult.ivPmiBean != null) { // d690014.3
            asyncResult.ivPmiBean.asyncFutureObjectIncrement();
        }

        int size = ivAllRemoteAsyncResults.size();

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "add " + asyncResult + "; size=" + size);

        // If this is the only entry in the list, setup the alarm.  d623593
        if (size == 1) {
            ivFuture = ivScheduledExecutor.schedule(this, ivAlarmIntervalMillis, TimeUnit.MILLISECONDS);
        } else if (size >= ivNearMaxResultsThreshold) {
            // d690014.1 - Remove the oldest entry if we have exceeded the maximum
            // number of results.  Otherwise, issue a warning since we are near
            // the maximum.
            boolean warn = false;
            if (size > ivMaxUnclaimedResults) {
                if (!ivWarnedExceededMax) {
                    warn = ivWarnedExceededMax = true;
                }

                Iterator<RemoteAsyncResultImpl> iterator = ivAllRemoteAsyncResults.iterator();
                RemoteAsyncResultImpl oldest = iterator.next();
                releaseResources(oldest); // d690014.3
                iterator.remove();
            } else {
                if (!ivWarnedNearMax) {
                    warn = ivWarnedNearMax = true;
                }
            }

            if (warn) {
                Tr.warning(tc, "MAXIMUM_UNCLAIMED_ASYNC_RESULTS_CNTR0328W",
                           size, ivMaxUnclaimedResults);
            }
        }
    } // end add()

    /**
     * Remove a server-side Future object from the reaper and release its
     * resources.
     */
    public synchronized void remove(RemoteAsyncResultImpl asyncResult) {
        ivAllRemoteAsyncResults.remove(asyncResult);
        releaseResources(asyncResult); // d690014.3

        // If no server-side Future objects and an alarm is set then cancel the alarm.  d623593
        if (ivAllRemoteAsyncResults.isEmpty() && ivFuture != null) {
            ivFuture.cancel(false);
            ivFuture = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "remove " + asyncResult + "; size=" + ivAllRemoteAsyncResults.size());
    } // end remove()

    private void releaseResources(RemoteAsyncResultImpl asyncResult) { // d690014.3
        asyncResult.unexportObject();
        if (asyncResult.ivPmiBean != null) {
            asyncResult.ivPmiBean.asyncFutureObjectDecrement();
        }
    }

} // end RemoteAsyncResultReaper
