/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
     * Initial delay for starting the polling task.
     */
    final long initialPollDelay;

    /**
     * JNDI name. Null if none.
     */
    final String jndiName;

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
        initialPollDelay = (Long) properties.get("initialPollDelay");
        pollInterval = enableTaskExecution ? (Long) properties.get("pollInterval") : -1;
        pollSize = enableTaskExecution ? (Integer) properties.get("pollSize") : null;
        retryInterval = (Long) properties.get("retryInterval");
        retryLimit = (Short) properties.get("retryLimit");
        xpathId = (String) properties.get("config.displayId");
        id = xpathId.contains("]/persistentExecutor[") ? null : (String) properties.get("id");

        // Range checking on duration values, which cannot be enforced via metatype
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
                        .append(",initialPollDelay=").append(initialPollDelay)
                        .append(",pollInterval=").append(pollInterval)
                        .append(",pollSize=").append(pollSize)
                        .append(",retryInterval=").append(retryInterval)
                        .append(",retryLimit=").append(retryLimit)
                        .append(",xpathId=").append(xpathId)
                        .append(",id=").append(id)
                        .toString();
    }
}
