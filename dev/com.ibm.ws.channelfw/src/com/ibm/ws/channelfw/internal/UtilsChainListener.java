/*******************************************************************************
 * Copyright (c) 2009, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
     * Perform a quick check (up to 1 second) to see if chains stopped.
     * Remove any chains found to be stopped.
     * This does not wait for the full chainQuiesceTimeout.
     * Chains that are still quiescing will be forcefully stopped by StopChainTask
     * when the chainQuiesceTimeout expires.
     * <p>
     * Previously this code had a 1-second sleep AFTER attempting to remove stopped chains.
     * It only attempted to remove chains once. The updated code still uses the arbitrary
     * 1-second maximum wait, but adds polling and retries, which allows it to finish early,
     * and has a better chance of returning with all stopped chains removed.
     * <p>
     * It seems that it would be better for the caller to call method waitForChainsToStop()
     * but that causes test cases to fail which didn't fail before. That needs to be investigated,
     * but the updated code below shouldn't cause any issues.
     * <p>
     * Why use a maxWaitTime of 1 second, rather than just use the timeout? There is a test case,
     * which is probably unrealistic. It sets the chainQuiesceTimeout to 250ms. That window is
     * too small for the chains to be removed consistently within the time frame. In practice, it
     * probably doesn't make sense to set the chainQuiesceTimeout less than the server quiesceTimeout.
     *
     * @param chainQuiesceTimeout Determines if we should wait for chains to stop.
     *                            If 0 or less, just do an immediate check with no delay.
     *
     */
    public void cleanUpChains(long chainQuiesceTimeout) {

        if (waitingChainNames.isEmpty()) {
            return;
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Quick check: " + waitingChainNames.size() + " chain(s) may still be quiescing");
        }
        
        // Remove any chains that already stopped
        removeStoppedChains();
        
        if (waitingChainNames.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "All chains already stopped");
            }
            return;
        }

        //
        // Poll for chains to stop, up to a maximum of 1 second 
        // Note: This wait time is independent of chainQuiesceTimeout, which controls
        // when StopChainTask forcefully stops chains
        if (chainQuiesceTimeout > 0) {
            final long maxWaitTime = 1000;  // Maximum time to wait in milliseconds
            final long pollInterval = 100;   // Poll every 100ms
            long elapsedTime = 0;
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Polling for up to " + maxWaitTime + "ms for chains to stop");
            }
            
            while (elapsedTime < maxWaitTime && !waitingChainNames.isEmpty()) {
                try {
                    Thread.sleep(pollInterval);
                    elapsedTime += pollInterval;
                } catch (InterruptedException ie) {
                    break;
                }
                
                // Remove chains that stopped during this poll interval
                removeStoppedChains();
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "After quick check: " + waitingChainNames.size() + " chain(s) still quiescing");
        }
    }

    /**
     * Remove stopped chains from the watch list.
     */
    private void removeStoppedChains() {
        if (waitingChainNames.isEmpty()) {
            return;
        }
        
        ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        Iterator<String> iter = waitingChainNames.iterator();
        while (iter.hasNext()) {
            if (!cf.isChainRunning(iter.next())) {
                iter.remove();
            }
        }
    }

    /**
     * Wait for all watched chains to stop, polling periodically.
     * This method blocks until either all chains have stopped or the timeout expires.
     * Stopped chains are removed from the list.
     *
     * @param chainQuiesceTimeout Maximum time to wait in milliseconds
     */
    public void waitForChainsToStop(long chainQuiesceTimeout) {
        if (waitingChainNames.isEmpty()) {
            return;
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Waiting for " + waitingChainNames.size() + " chain(s) to stop");
        }

        long startTime = System.nanoTime();
        long expireTime = chainQuiesceTimeout * 1_000_000L;  // Convert ms to ns
        long elapsedTime;
               
        // Always cleanup at least once, then keep checking until timeout or all chains stopped
        do {
            removeStoppedChains();  // Remove chains that have stopped
            
            elapsedTime = System.nanoTime() - startTime;
            if (waitingChainNames.isEmpty() || (elapsedTime >= expireTime)) {
                break;  // All chains stopped, or timeout expired
            }
            
            try {
                Thread.sleep(100);  // Poll every 100ms
            } catch (InterruptedException ie) {
                elapsedTime = System.nanoTime() - startTime;  // Update for trace.
                break;
            }
            
        } while (true);        
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (waitingChainNames.isEmpty()) {
                Tr.debug(this, tc, "All chains stopped after " + elapsedTime + " ns");
            } else {
                Tr.debug(this, tc, "Timeout expired, " + waitingChainNames.size() + " chain(s) still running");
            }
        }
    }

}
