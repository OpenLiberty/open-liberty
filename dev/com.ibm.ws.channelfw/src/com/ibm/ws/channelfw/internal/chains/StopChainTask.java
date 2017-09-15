/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal.chains;

import java.util.TimerTask;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.ChannelFramework;

/**
 * The purpose of the class is to manage a task associated with the channel
 * framework timer. Specifically, this task will handle stopping a chain
 * after a specified amount of time. This is how the runtime handles quiescing
 * a chain. Before this task is given to the Timer (scheduler), a stop is
 * called on the chain with a length of time noting when the actual stop will
 * be asserted. This gives the chain (actually the channels within it) the
 * chance to take any appropriate actions given the upcoming closure.
 */
public class StopChainTask extends TimerTask {

    /** Trace service */
    private static final TraceComponent tc =
                    Tr.register(StopChainTask.class,
                                ChannelFrameworkConstants.BASE_TRACE_NAME,
                                ChannelFrameworkConstants.BASE_BUNDLE);
    /**
     * name of the chain.
     */
    private String chainName = null;
    /**
     * reference to framework.
     */
    private ChannelFramework framework = null;

    /**
     * Constructor.
     * 
     * @param inputChainName
     * @param inputFramework
     */
    public StopChainTask(String inputChainName, ChannelFramework inputFramework) {
        this.chainName = inputChainName;
        this.framework = inputFramework;
    }

    /**
     * This method will be called when the secondsToWait expires after this task
     * has been placed in the channel framework's timer.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "run");
        }

        try {
            framework.stopChain(chainName, 0L);
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName() + ".run", "68",
                                        this, new Object[] { chainName });
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "run");
        }
    }
}
