/*******************************************************************************
 * Copyright (c) 2014,2019 IBM Corporation and others.
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
     * Amount of time (in seconds) between updating the partition table to indicate that this instance is still running.
     */
    final long heartbeatInterval;

    /**
     * Initial delay for starting the polling task.
     */
    final long initialPollDelay;

    /**
     * JNDI name. Null if none.
     */
    final String jndiName;

    /**
     * Amount of time (in seconds) beyond a task's scheduled time after which the task is considered to be missed
     * and is eligible to be taken over by another member.
     */
    final long missedTaskThreshold;

    /**
     * Amount of time (in seconds) beyond a task's scheduled start time to reserve for running the next execution of the task.
     * Other executor instances are prevented from claiming ownership of the task prior to the expiration of this interval,
     * but if the interval elapses without successful execution of the task, then the task execution is considered to have been missed,
     * enabling another instance to attempt to run it.
     */
    final long missedTaskThreshold2;

    /**
     * Interval between polling for tasks to run. A value of -1 disables all polling after the initial poll.
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
        heartbeatInterval = (Long) properties.get("heartbeatInterval");
        initialPollDelay = (Long) properties.get("initialPollDelay");
        missedTaskThreshold = enableTaskExecution ? (Long) properties.get("missedTaskThreshold") : -1;
        missedTaskThreshold2 = (Long) properties.get("missedTaskThreshold2");
        pollInterval = enableTaskExecution ? (Long) properties.get("pollInterval") : -1;
        pollSize = enableTaskExecution ? (Integer) properties.get("pollSize") : null;
        retryInterval = (Long) properties.get("retryInterval");
        retryLimit = (Short) properties.get("retryLimit");
        xpathId = (String) properties.get("config.displayId");
        id = xpathId.contains("]/persistentExecutor[") ? null : (String) properties.get("id");

        // Range checking on duration values, which cannot be enforced via metatype
        if (heartbeatInterval != -1 && heartbeatInterval < 1)
            throw new IllegalArgumentException("heartbeatInterval: " + heartbeatInterval + "s");
        if (missedTaskThreshold != -1 && missedTaskThreshold < 1)
            throw new IllegalArgumentException("missedTaskThreshold: " + missedTaskThreshold + "s");
        if ((missedTaskThreshold2 != -1 && missedTaskThreshold2 < 1) || missedTaskThreshold2 > 86400) // disallowing above 1 day. What is a reasonable upper bound?
            throw new IllegalArgumentException("missedTaskThreshold2: " + missedTaskThreshold2 + "s");
        if (initialPollDelay < -1)
            throw new IllegalArgumentException("initialPollDelay: " + initialPollDelay + "ms");
        if (pollInterval < -1)
            throw new IllegalArgumentException("pollInterval: " + pollInterval + "ms");
        if (retryInterval < 0)
            throw new IllegalArgumentException("retryInterval: " + retryInterval + "ms");
    }

    @Override
    public String toString() {
        return new StringBuilder(300)
                        .append("instance=").append(Integer.toHexString(System.identityHashCode(this)))
                        .append(",jndiName=").append(jndiName)
                        .append(",enableTaskExecution=").append(enableTaskExecution)
                        .append(",heartbeatInterval=").append(heartbeatInterval)
                        .append("s,initialPollDelay=").append(initialPollDelay)
                        .append("ms,missedTaskThreshold=").append(missedTaskThreshold)
                        .append("s,missedTaskThreshold2=").append(missedTaskThreshold2)
                        .append("s,pollInterval=").append(pollInterval)
                        .append("ms,pollSize=").append(pollSize)
                        .append(",retryInterval=").append(retryInterval)
                        .append("ms,retryLimit=").append(retryLimit)
                        .append(",xpathId=").append(xpathId)
                        .append(",id=").append(id)
                        .toString();
    }
}
