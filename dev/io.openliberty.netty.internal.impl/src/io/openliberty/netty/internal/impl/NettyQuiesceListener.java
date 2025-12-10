/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.impl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

public class NettyQuiesceListener {

    /** Trace service */
    private static final TraceComponent tc = Tr.register(NettyQuiesceListener.class, NettyConstants.NETTY_TRACE_NAME, NettyConstants.BASE_BUNDLE);

    private ScheduledExecutorService scheduler;

    private ScheduledFuture<?> quiesceFinishTask;
    private ScheduledFuture<?> quiesceVerifierTask;
    private long quiesceTimeout = 0;

    private Runnable quiesceTask = null;

    private NettyFrameworkImpl bundle;

    /**
     * Constructor
     */
    public NettyQuiesceListener(NettyFrameworkImpl nettyBundle, ScheduledExecutorService scheduler, long timeout) {
        this.bundle = nettyBundle;
        this.scheduler = scheduler;
        this.quiesceTimeout = (timeout > 0) ? timeout : 0;
    }

    /**
     * Runs the Quiesce verifier task at fixed intervals until the active channels have
     * been stopped or the Quiesce finish task finishes. The Quiesce finish task will
     * be ran once after the Quiesce time has elapsed.
     *
     * @return
     */
    public ScheduledFuture<?> startTasks() {

        this.quiesceFinishTask = scheduler.schedule(() -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The quiesceFinishTask has started for " + this.bundle + ". Quiesce task will be scheduled to run for: " + this.bundle);
            }
            if (quiesceTask != null)
                scheduler.schedule(quiesceTask, 0, TimeUnit.MILLISECONDS);
            if (!this.quiesceVerifierTask.isDone()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The quiesceVerifierTask was found not to be done. Cancelling it...");
                }
                this.quiesceVerifierTask.cancel(true);
            }
        }, quiesceTimeout, TimeUnit.MILLISECONDS);

        this.quiesceVerifierTask = scheduler.scheduleAtFixedRate(() -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The quiesceVerifierTask has started,  checking endpoints...");
            }
            Map<Channel, ChannelGroup> activeChannelsMap = this.bundle.getActiveChannelsMap();
            if (activeChannelsMap.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No active connections have been found. Scheduling quiesce task and cancelling quiesce verifiers for: " + this.bundle);
                }

                if (quiesceTask != null)
                    scheduler.schedule(quiesceTask, 0, TimeUnit.MILLISECONDS);

                if (!this.quiesceFinishTask.isDone()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The quiesceFinishTask was found not to be done. Cancelling it...");
                    }
                    this.quiesceFinishTask.cancel(true);
                }
                if (!this.quiesceVerifierTask.isDone()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The quiesceVerifierTask was found not to be done. Cancelling it...");
                    }
                    this.quiesceVerifierTask.cancel(true);
                }

            } else {
                // Check if endpoint has open connections
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Remaining endpoints still running. Verifying which can be closed...");
                }
                // Iterate through active endpoints via Map Entries
                for (Map.Entry<Channel, ChannelGroup> entry : activeChannelsMap.entrySet()) {
                    Channel endpoint = entry.getKey();
                    ChannelGroup group = entry.getValue();

                    // Verify if there are still active connections for that endpoint
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found group " + group + " for endpoint " + endpoint + "containing " + group.size() + " active connections");
                    }

                    if (group != null && group.isEmpty()) {
                        // Close endpoint if no active connections left
                        this.bundle.stop(endpoint);
                    }
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS); // Schedule to run every second starting one second after creation. See https://github.com/OpenLiberty/open-liberty/issues/25800

        return quiesceFinishTask;
    }

    public void setQuiesceTask(Runnable task) {
        this.quiesceTask = task;
    }

    public void cancelTasks() {
        if (quiesceFinishTask != null) {
            this.quiesceFinishTask.cancel(true);
        }
        if (quiesceVerifierTask != null) {
            this.quiesceVerifierTask.cancel(true);
        }
    }

}
