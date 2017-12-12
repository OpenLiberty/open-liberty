/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.health.center.data;

public class HCGCData {
    private final long heap;
    private final long usage;
    private final long maxHeap;
    private final long time;
    private final double duration;
    private final String type;
    private final String reason;
    private final String sequence;

    public HCGCData(long heap, long usage, long time, double duration, String type, String reason, String sequence) {
        this(heap, usage, 0, time, duration, type, reason, sequence);
    }

    public HCGCData(long heap, long usage, long maxHeap, long time, double duration, String type, String reason, String sequence) {
        this.heap = heap;
        this.usage = usage;
        this.maxHeap = maxHeap;
        this.time = time;
        this.duration = duration;
        this.type = type;
        this.reason = reason;
        this.sequence = sequence;

    }

    public String getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public double getHeap() {
        return heap;
    }

    public double getUsage() {
        return usage;
    }

    /**
     * Returns the max heap for the JVM. If the maximum heap was not
     * initialized for this instance, then a value of 0 will be returned
     *
     * @return The maximum heap allowed for this JVM process
     */
    public long getMaxHeap() {
        return maxHeap;
    }

    public long getTime() {
        return time;
    }

    public double getDuration() {
        return duration;
    }

    public String getSequence() {
        return sequence;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "HCGCData [\nheap=" + heap + ", \nusage=" + usage + ", \nmaxHeap=" + maxHeap + ", \ntime=" + time + ", \nduration=" + duration + ", \ntype=" + type + ", \nreason="
               + reason + ", \nsequence="
               + sequence + "\n]";
    }

}
