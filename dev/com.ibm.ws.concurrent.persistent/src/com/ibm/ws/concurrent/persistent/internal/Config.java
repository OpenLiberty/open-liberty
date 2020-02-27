/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.internal;

import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Persistent executor config that allows all values to be swapped atomically when using an AtomicReference.
 */
@Trivial
class Config {
    private static final TraceComponent tc = Tr.register(Config.class);

    /**
     * Indicates if this instance can run tasks.
     */
    final boolean enableTaskExecution;

    /**
     * Id. Null if none.
     */
    final String id;

    /**
     * Initial delay for starting the polling task.
     */
    final long initialPollDelay;

    /**
     * JNDI name. Null if none.
     */
    final String jndiName;

    /**
     * Amount of time (in seconds) beyond a task's scheduled start time to reserve for running the next execution of the task.
     * Other executor instances are prevented from claiming ownership of the task prior to the expiration of this interval.
     * If the interval elapses without successful execution of the task, then the task execution is considered to have been missed,
     * enabling another instance to attempt to run it.
     */
    final long missedTaskThreshold;

    /**
     * Experimental attribute that causes a single poll interval to be coordinated across multiple instances when fail over is also enabled.
     */
    final boolean pollingCoordination;

    /**
     * Interval between polling for tasks to run. A value of -1 means auto-compute a poll interval.
     * When fail over is disabled, the -1 value disables all polling after the initial poll.
     */
    final long pollInterval;

    /**
     * Maximum number of task results for a single poll. A null value means unlimited.
     */
    final Integer pollSize;

    /**
     * Number of milliseconds that occur between the second and subsequent attempts to run a failed task.
     */
    final long retryInterval;

    /**
     * Number of consecutive times we will retry a task before it is considered permanently failed.
     */
    final int retryLimit;

    /**
     * Xpath unique identifier.
     */
    final String xpathId;

    /**
     * Construct a config instance representing modifiable configuration for a persistent executor instance.
     */
    Config(Dictionary<String, ?> properties) {
        jndiName = (String) properties.get("jndiName");
        enableTaskExecution = (Boolean) properties.get("enableTaskExecution");
        boolean ignoreMin = Boolean.parseBoolean((String) properties.get("ignore.minimum.for.test.use.only")); // helps tests run faster, but NOT SUPPORTED for production use
        initialPollDelay = (Long) properties.get("initialPollDelay");
        missedTaskThreshold = (Long) properties.get("missedTaskThreshold");
        Long pollIntrvl = enableTaskExecution ? (Long) properties.get("pollInterval") : null;
        boolean pollCoordination = Boolean.parseBoolean((String) properties.get("pollingCoordination.for.test.use.only")); // NOT SUPPORTED for production use
        pollingCoordination = enableTaskExecution && missedTaskThreshold > 0 && (pollIntrvl == null || pollCoordination);
        pollSize = enableTaskExecution ? (Integer) properties.get("pollSize") : null;
        Long retryIntrvl = (Long) properties.get("retryInterval");
        retryLimit = (Short) properties.get("retryLimit");
        xpathId = (String) properties.get("config.displayId");
        id = xpathId.contains("]/persistentExecutor[") ? null : (String) properties.get("id");

        boolean pollIntervalDefaulted = false;
        if (pollIntrvl == null) {
            // 59 seconds is used to avoid continually colliding with a task that runs every minute
            if (enableTaskExecution && missedTaskThreshold > 0) {
                pollIntrvl = TimeUnit.SECONDS.toMillis(59);
                pollIntervalDefaulted = true;
            } else {
                pollIntrvl = -1l;
            }
        }
        pollInterval = pollIntrvl;

        // Default the retry interval to disabled when fail over is enabled.
        if (retryIntrvl == null) {
            if (missedTaskThreshold > 0) {
                retryInterval = -1; // disabled
            } else {
                retryInterval = TimeUnit.MINUTES.toMillis(1); // the old default for single-server, which cannot be changed
            }
        } else {
            retryInterval = retryIntrvl;
        }

        // Range checking on duration values, which cannot be enforced via metatype
        if ((missedTaskThreshold != -1 && !ignoreMin && missedTaskThreshold < 100) || missedTaskThreshold > 9000) // disallow below 100 seconds and above 2.5 hours
            throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKC1520.out.of.range",
                                                                toString(missedTaskThreshold, TimeUnit.SECONDS), "missedTaskThreshold", "100s", "2h30m"));
        if (initialPollDelay < -1)
            throw new IllegalArgumentException("initialPollDelay: " + initialPollDelay + "ms");
        if (pollInterval < -1 || missedTaskThreshold > 0 && !pollIntervalDefaulted && (!ignoreMin && pollInterval < 100000 && pollInterval != -1 || pollInterval > 9000000)) // disallow below 100 seconds and above 2.5 hours
            throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKC1520.out.of.range",
                                                                toString(pollInterval, TimeUnit.MILLISECONDS), "pollInterval", "100s", "2h30m"));
        if (retryInterval < 0 && missedTaskThreshold == -1)
            throw new IllegalArgumentException("retryInterval: " + retryInterval + "ms");
        else if (retryInterval >= 0 && missedTaskThreshold > 0) {
            // Allow the configuration of the built-in EJB persistent timers executor, but otherwise reject enablement of retryInterval when fail over is enabled.
            if (!(retryInterval == TimeUnit.SECONDS.toMillis(300) && "defaultEJBPersistentTimerExecutor".equals(id)))
                throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKC1521.not.compatible", "retryInterval", "missedTaskThreshold"));
        }
    }

    @Override
    public String toString() {
        return new StringBuilder(300)
                        .append("instance=")
                        .append(Integer.toHexString(System.identityHashCode(this)))
                        .append(",id=")
                        .append(id)
                        .append(",jndiName=")
                        .append(jndiName)
                        .append(",enableTaskExecution=")
                        .append(enableTaskExecution)
                        .append(",initialPollDelay=")
                        .append(initialPollDelay)
                        .append("ms,missedTaskThreshold=")
                        .append(missedTaskThreshold)
                        .append("s,pollingCoordination=")
                        .append(pollingCoordination)
                        .append(",pollInterval=")
                        .append(pollInterval)
                        .append("ms,pollSize=")
                        .append(pollSize)
                        .append(",retryInterval=")
                        .append(retryInterval)
                        .append("ms,retryLimit=")
                        .append(retryLimit)
                        .append(",xpathId=")
                        .append(xpathId)
                        .toString();
    }

    /**
     * Display the duration value in the least granular unit possible without losing precision.
     *
     * @param duration the duration
     * @param unit     the units
     * @return string value for display in messages.
     */
    private String toString(long duration, TimeUnit unit) {
        if (TimeUnit.MILLISECONDS.equals(unit)) {
            long s = duration / 1000;
            if (s * 1000 == duration) {
                duration = s;
                unit = TimeUnit.SECONDS;
            }
        }

        if (TimeUnit.SECONDS.equals(unit)) {
            long m = duration / 60;
            if (m * 60 == duration) {
                duration = m;
                unit = TimeUnit.MINUTES;
            }
        }

        if (TimeUnit.MINUTES.equals(unit)) {
            long h = duration / 60;
            if (h * 60 == duration) {
                duration = h;
                unit = TimeUnit.HOURS;
            }
        }

        return duration + (unit == TimeUnit.MILLISECONDS ? "ms" //
                        : unit == TimeUnit.SECONDS ? "s" //
                                        : unit == TimeUnit.MINUTES ? "m" //
                                                        : unit == TimeUnit.HOURS ? "h" //
                                                                        : "d");
    }
}
