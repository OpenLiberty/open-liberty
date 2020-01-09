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

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Persistent executor config that allows all values to be swapped atomically when using an AtomicReference.
 */
@Trivial
class Config {
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
        pollSize = enableTaskExecution ? (Integer) properties.get("pollSize") : null;
        Long retryIntrvl = (Long) properties.get("retryInterval");
        retryLimit = (Short) properties.get("retryLimit");
        xpathId = (String) properties.get("config.displayId");
        id = xpathId.contains("]/persistentExecutor[") ? null : (String) properties.get("id");

        if (pollIntrvl == null) {
            // TODO come up with better auto-compute logic.
            // For now, we default poll interval to a value between 5m and 30m, matching the missedTaskThreshold, or 5m if less, or 30m if higher.
            if (enableTaskExecution && missedTaskThreshold > 0) {
                if (missedTaskThreshold < TimeUnit.MINUTES.toSeconds(5) && !ignoreMin)
                    pollIntrvl = TimeUnit.MINUTES.toMillis(5);
                else if (missedTaskThreshold < TimeUnit.MINUTES.toSeconds(30))
                    pollIntrvl = TimeUnit.SECONDS.toMillis(missedTaskThreshold);
                else
                    pollIntrvl = TimeUnit.MINUTES.toMillis(30);
            } else {
                pollIntrvl = -1l;
            }
        }
        pollInterval = pollIntrvl;

        // Default the retry interval to match the poll interval (or lacking that, the missed task threshold) when fail over is enabled.
        if (retryIntrvl == null) {
            if (missedTaskThreshold > 0) {
                retryInterval = enableTaskExecution && pollInterval > 0 ? pollInterval : TimeUnit.SECONDS.toMillis(missedTaskThreshold);
            } else {
                retryInterval = TimeUnit.MINUTES.toMillis(1); // the old default for single-server, which cannot be changed
            }
        } else {
            retryInterval = retryIntrvl;
        }

        // TODO translatable messages for the following
        // Range checking on duration values, which cannot be enforced via metatype
        if ((missedTaskThreshold != -1 && !ignoreMin && missedTaskThreshold < 100) || missedTaskThreshold > 9000) // disallow below 100 seconds and above 2.5 hours
            throw new IllegalArgumentException("missedTaskThreshold: " + missedTaskThreshold + "s");
        if (initialPollDelay < -1)
            throw new IllegalArgumentException("initialPollDelay: " + initialPollDelay + "ms");
        if (pollInterval < -1 || missedTaskThreshold > 0 && (!ignoreMin && pollInterval < 100000 && pollInterval != -1 || pollInterval > 9000000)) // disallow below 100 seconds and above 2.5 hours
            throw new IllegalArgumentException("pollInterval: " + pollInterval + "ms");
        if (retryInterval < 0 || missedTaskThreshold > 0 && retryIntrvl != null && retryLimit != 0 && !ignoreMin && retryIntrvl < missedTaskThreshold * 1000)
            throw new IllegalArgumentException("retryInterval: " + retryInterval + "ms");
    }

    @Override
    public String toString() {
        return new StringBuilder(300)
                        .append("instance=")
                        .append(Integer.toHexString(System.identityHashCode(this)))
                        .append(",jndiName=")
                        .append(jndiName)
                        .append(",enableTaskExecution=")
                        .append(enableTaskExecution)
                        .append(",initialPollDelay=")
                        .append(initialPollDelay)
                        .append("ms,missedTaskThreshold=")
                        .append(missedTaskThreshold)
                        .append("s,pollInterval=")
                        .append(pollInterval)
                        .append("ms,pollSize=")
                        .append(pollSize)
                        .append(",retryInterval=")
                        .append(retryInterval)
                        .append("ms,retryLimit=")
                        .append(retryLimit)
                        .append(",xpathId=")
                        .append(xpathId)
                        .append(",id=")
                        .append(id)
                        .toString();
    }
}
