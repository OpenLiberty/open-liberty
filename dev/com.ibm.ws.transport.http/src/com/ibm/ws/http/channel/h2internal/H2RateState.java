/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

/**
 * Keeps track of the rate at which HTTP/2 control frames are send and received on a given connection, 
 * with the intention of detecting misbehaving clients.  A few measures are tracked:
 * 
 * - control frames read as a ratio of the number of non-control frames read
 * - control frames written as a ratio of the number of non-control frames written
 * - number of empty (zero-length body) data frames received on a given stream
 * - number of streams reset
 */
public class H2RateState {

    private static final int maxReadControlFrameCount = 10000;
    private static final int maxResetFrameCount = 5000;
    private static final int maxEmptyFrameCount = 500;
    private volatile long controlFrameCount = 0L;
    private volatile int resetCount = 0;

    public synchronized void setStreamReset() {
        resetCount++;
    }

    public synchronized void incrementReadControlFrameCount() {
        controlFrameCount++;
    }

    public synchronized void incrementReadNonControlFrameCount() {
        controlFrameCount = controlFrameCount / 2;
    }

    /**
     * @return true if the connection is considered to be misbehaving
     */
    public synchronized boolean isControlRatioExceeded() {
        if (controlFrameCount > maxReadControlFrameCount
            || resetCount > maxResetFrameCount) {
            return true;
        }
        return false;
    }

    /**
     * @param emptyFrameCount the number of empty frames received on a stream
     * @return true if emptyFrameCount exceeds the limit for a well-behaved stream 
     */
    public synchronized boolean isStreamMisbehaving(int emptyFrameCount) {
        return emptyFrameCount > maxEmptyFrameCount;
    }
}