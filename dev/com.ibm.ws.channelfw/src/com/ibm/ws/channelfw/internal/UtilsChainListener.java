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

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

/**
 * Chain lifecycle listener used by the ChannelUtils while blocking on chain
 * stops.
 */
public class UtilsChainListener implements ChainEventListener {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(UtilsChainListener.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /** Number of chains currently being monitored */
    private int numChainsWaiting = 0;
    /** Lock used when blocking */
    private final Object lock = new Object()
    {};

    /**
     * Constructor.
     */
    public UtilsChainListener() {
        // nothing to do
    }

    /**
     * Notify this listener to watch another chain.
     * 
     * @param chain
     */
    public void watchChain(ChainData chain) {
        ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        try {
            synchronized (this.lock) {
                this.numChainsWaiting++;
            } // end-sync
            cf.addChainEventListener(this, chain.getName());
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Watching chain; " + chain.getName());
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Unable to watch chain; " + chain.getName() + "; " + e);
            }
        }
    }

    /**
     * Block waiting for all current chains being watched to be stopped fully.
     * 
     * @param quiesceTimeout
     */
    public void waitOnChains(long quiesceTimeout) {
        // if we're still waiting on chains to stop, then sync and wait
        synchronized (this.lock) {
            if (0 < this.numChainsWaiting) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Waiting on " + this.numChainsWaiting + " chain(s) to stop");
                }
                try {
                    // wait for up to the quiescetimeout plus some padding
                    this.lock.wait(quiesceTimeout + 2345L);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        } // end-sync
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChainEventListener#chainDestroyed(com.ibm.websphere
     * .channelfw.ChainData)
     */
    @Override
    public void chainDestroyed(ChainData chainData) {
        // do nothing
    }

    /*
     * @see
     * 
     * com.ibm.wsspi.channelfw.ChainEventListener#chainInitialized(com.ibm.websphere
     * .channelfw.ChainData)
     */
    @Override
    public void chainInitialized(ChainData chainData) {
        // do nothing
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChainEventListener#chainQuiesced(com.ibm.websphere
     * .channelfw.ChainData)
     */
    @Override
    public void chainQuiesced(ChainData chainData) {
        // do nothing
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChainEventListener#chainStarted(com.ibm.websphere
     * .channelfw.ChainData)
     */
    @Override
    public void chainStarted(ChainData chainData) {
        // do nothing
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChainEventListener#chainStopped(com.ibm.websphere
     * .channelfw.ChainData)
     */
    @Override
    public void chainStopped(ChainData chainData) {
        ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        synchronized (this.lock) {
            this.numChainsWaiting--;
            if (0 == this.numChainsWaiting) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Last chain has stopped");
                }
                this.lock.notifyAll();
            }
        } // end-sync
        try {
            cf.removeChainEventListener(this, chainData.getName());
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Unable to disconnect listener from chain; " + chainData.getName());
            }
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChainEventListener#chainUpdated(com.ibm.websphere
     * .channelfw.ChainData)
     */
    @Override
    public void chainUpdated(ChainData chainData) {
        // do nothing
    }

}
