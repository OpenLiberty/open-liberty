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

import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Keeps track of the conditions necessary to start polling.
 * 
 * It is up to the caller to avoid duplicate add/remove. We could certainly add checking for it,
 * but that is not needed if we can trust our internal usage of this class.
 */
@Trivial
class PollingManager {
    private static final TraceComponent tc = Tr.register(PollingManager.class);

    /**
     * Conditions that determine if we are ready for polling.
     */
    static final int
                    SERVER_STARTED = 0x1,
                    EXECUTION_ENABLED = 0x2,
                    DS_READY = 0x4,
                    SIGNAL_RECEIVED = 0x8,
                    SIGNAL_REQUIRED = 0x10;

    /**
     * States that indicate we are ready for polling.
     */
    private static final int
                    READY_WITHOUT_SIGNAL = SERVER_STARTED + EXECUTION_ENABLED + DS_READY,
                    READY_WITH_UNNECESSARY_SIGNAL = READY_WITHOUT_SIGNAL + SIGNAL_RECEIVED,
                    READY_WITH_SIGNAL = READY_WITH_UNNECESSARY_SIGNAL + SIGNAL_REQUIRED;

    /**
     * Combination of bits that represent if we are ready for polling.
     */
    final AtomicInteger bits = new AtomicInteger();

    /**
     * Add an event without checking if we are ready to start polling.
     * 
     * @param event an event such as SERVER_STARTED or SIGNAL_RECEIVED
     */
    void add(int event) {
        int b = bits.addAndGet(event);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "add " + event + ", polling state: " + b);
    }

    /**
     * Add an event and then check if we are ready for polling.
     * 
     * @param event an event such as SERVER_STARTED or SIGNAL_RECEIVED
     * @return true if ready for polling. Otherwise false.
     */
    boolean addAndCheckIfReady(int event) {
        int b = bits.addAndGet(event);
        boolean isReady = b == READY_WITHOUT_SIGNAL || b == READY_WITH_UNNECESSARY_SIGNAL || b == READY_WITH_SIGNAL;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "addAndCheckIfReady " + event + ", polling state: " + b + ", " + isReady);
        return isReady;
    }

    /**
     * Removes an event that is necessary for polling.
     * 
     * @param event an event such as EXECUTION_ENABLED or DS_READY
     */
    void remove(int event) {
        int b = bits.addAndGet(-event);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "remove " + event + ", polling state: " + b);
    }
}
