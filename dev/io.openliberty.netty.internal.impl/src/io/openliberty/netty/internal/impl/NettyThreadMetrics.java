/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.impl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.util.concurrent.AutoScalingEventExecutorChooserFactory.AutoScalingUtilizationMetric;

/**
 * Logs periodic thread utilization metrics for the Netty worker group {@link EventLoopGroup}.
 *
 * <p>This class has its own {@link TraceComponent} registered solely by class name, with no
 * named trace group. Tracing can therefore only be enabled independently via package-level
 * or class-level trace specifications, for example:
 * <pre>
 *   io.openliberty.netty.internal.impl.NettyThreadMetrics=all
 *   io.openliberty.netty.internal.impl.*=all
 * </pre>
 * This allows thread-metric tracing to be isolated from the broader {@code Netty} trace group.
 */
class NettyThreadMetrics {

    static final TraceComponent tc = Tr.register(NettyThreadMetrics.class);

    /**
     * Logs the current active-thread count and per-thread utilization for the given
     * {@link MultiThreadIoEventLoopGroup} at debug level.
     *
     * <p>This method is intended to be called from a scheduled task. It is a no-op when
     * debug tracing for this class is not enabled.
     *
     * @param childGroup the child event loop group whose utilization metrics to log
     */
    static void logMetrics(EventLoopGroup childGroup) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            MultiThreadIoEventLoopGroup group = (MultiThreadIoEventLoopGroup) childGroup;
            StringBuilder sb = new StringBuilder("Getting metrics from MultiThreadIoEventLoopGroup with active threads ")
                    .append(group.activeExecutorCount())
                    .append(" : ");

            for (AutoScalingUtilizationMetric metric : group.executorUtilizations()) {
                sb.append("Thread@")
                .append(Integer.toHexString(metric.executor().hashCode()))
                .append(" -> ")
                .append(String.format("%.2f", metric.utilization() * 100.0))
                .append("%, ");
            }

            Tr.debug(tc, sb.toString());
        }
    }
}
