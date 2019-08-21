/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal;

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

/**
 * Polls a list of chains until they're stopped or the quiesce timeout is hit.
 *
 */
public class UtilsChainListener {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(UtilsChainListener.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    private final ArrayList<String> waitingChainNames = new ArrayList<String>();

    /**
     * Constructor.
     */
    public UtilsChainListener() {

    }

    /**
     * Notify this listener to watch another chain.
     *
     * @param chain
     */
    public void watchChain(ChainData chain) {
        waitingChainNames.add(chain.getName());
    }

    /**
     * Poll the list of chains until they're stopped or the quiesce timeout is hit
     *
     * @param quiesceTimeout
     */
    public void waitOnChains(long quiesceTimeout) {

        ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        int elapsedTime = 0;
        if (waitingChainNames.size() > 0 && elapsedTime < quiesceTimeout) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Waiting on " + waitingChainNames.size() + " chain(s) to stop");
            }

            Iterator<String> iter = waitingChainNames.iterator();
            while (iter.hasNext()) {
                if (!cf.isChainRunning(iter.next()))
                    iter.remove();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                // ignore
            }
            elapsedTime += 1000;

        }

    }

}
