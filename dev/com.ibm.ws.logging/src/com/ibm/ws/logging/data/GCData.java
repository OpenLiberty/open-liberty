/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

import com.ibm.ws.logging.collector.LogFieldConstants;

/**
 *
 */
public class GCData extends GenericData {

    public static final String[] NAMES = {
                                           LogFieldConstants.HEAP,
                                           LogFieldConstants.USED_HEAP,
                                           LogFieldConstants.MAX_HEAP,
                                           LogFieldConstants.DURATION,
                                           LogFieldConstants.GC_TYPE,
                                           LogFieldConstants.REASON,
                                           LogFieldConstants.DATETIME,
                                           LogFieldConstants.SEQUENCE
    };

    public static final String[] NAMES1_1 = {
                                              LogFieldConstants.IBM_HEAP,
                                              LogFieldConstants.IBM_USED_HEAP,
                                              LogFieldConstants.IBM_MAX_HEAP,
                                              LogFieldConstants.IBM_DURATION,
                                              LogFieldConstants.IBM_GC_TYPE,
                                              LogFieldConstants.IBM_REASON,
                                              LogFieldConstants.IBM_DATETIME,
                                              LogFieldConstants.IBM_SEQUENCE
    };

    public GCData() {
        super(8);
    }

    public void setHeap(long l) {
        setPair(0, l);
    }

    public void setUsedHeap(long l) {
        setPair(1, l);
    }

    public void setMaxHeap(long l) {
        setPair(2, l);
    }

    public void setDuration(long l) {
        setPair(3, l);
    }

    public void setGcType(String s) {
        setPair(4, s);
    }

    public void setReason(String s) {
        setPair(5, s);
    }

    public void setDatetime(long s) {
        setPair(6, s);
    }

    public void setSequence(String s) {
        setPair(7, s);
    }

    public long getHeap() {
        return getLongValue(0);
    }

    public long getUsedHeap() {
        return getLongValue(1);
    }

    public long getMaxHeap() {
        return getLongValue(2);
    }

    public long getDuration() {
        return getLongValue(3);
    }

    public String getGcType() {
        return getStringValue(4);
    }

    public String getReason() {
        return getStringValue(5);
    }

    public long getDatetime() {
        return getLongValue(6);
    }

    public String getSequence() {
        return getStringValue(7);
    }

    public String getHeapKey() {
        return NAMES[0];
    }

    public String getUsedHeapKey() {
        return NAMES[1];
    }

    public String getMaxHeapKey() {
        return NAMES[2];
    }

    public String getDurationKey() {
        return NAMES[3];
    }

    public String getGcTypeKey() {
        return NAMES[4];
    }

    public String getReasonKey() {
        return NAMES[5];
    }

    public String getDatetimeKey() {
        return NAMES[6];
    }

    public String getSequenceKey() {
        return NAMES[7];
    }

    public String getHeapKey1_1() {
        return NAMES1_1[0];
    }

    public String getUsedHeapKey1_1() {
        return NAMES1_1[1];
    }

    public String getMaxHeapKey1_1() {
        return NAMES1_1[2];
    }

    public String getDurationKey1_1() {
        return NAMES1_1[3];
    }

    public String getGcTypeKey1_1() {
        return NAMES1_1[4];
    }

    public String getReasonKey1_1() {
        return NAMES1_1[5];
    }

    public String getDatetimeKey1_1() {
        return NAMES1_1[6];
    }

    public String getSequenceKey1_1() {
        return NAMES1_1[7];
    }

    private void setPair(int index, String s) {
        setPair(index, NAMES1_1[index], s);
    }

    private void setPair(int index, long l) {
        setPair(index, NAMES1_1[index], l);
    }
}
