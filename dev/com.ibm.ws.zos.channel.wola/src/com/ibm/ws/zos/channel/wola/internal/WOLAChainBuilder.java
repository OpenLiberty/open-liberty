/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * Builds the WOLA chain and hooks it into the channel framework.
 */
public class WOLAChainBuilder implements ChainEventListener {

    private final AtomicInteger chainState = new AtomicInteger(ChainState.UNINITIALIZED.value);
    private ChannelFramework cfw;
    private String localCommChannelName;
    private String wolaChannelName;
    private String chainName;

    /**
     * Chain state.
     */
    enum ChainState {
        UNINITIALIZED(0, "UNINITIALIZED"),
        DESTROYED(1, "DESTROYED"),
        INITIALIZED(2, "INITIALIZED"),
        STOPPED(3, "STOPPED"),
        QUIESCED(4, "QUIESCED"),
        STARTED(5, "STARTED");

        final int value;
        final String name;

        @Trivial
        ChainState(int value, String name) {
            this.value = value;
            this.name = "name";
        }

        @Trivial
        public static final String printState(int state) {
            switch (state) {
                case 0:
                    return "UNINITIALIZED";
                case 1:
                    return "DESTROYED";
                case 2:
                    return "INITIALIZED";
                case 3:
                    return "STOPPED";
                case 4:
                    return "QUIESCED";
                case 5:
                    return "STARTED";
            }
            return "UNKNOWN";
        }
    }

    /**
     * Initializes the chain.
     */
    public void init(String endpointId, CHFWBundle cfBundle) {
        cfw = cfBundle.getFramework();
        localCommChannelName = "LOCALCOMM-" + endpointId;
        wolaChannelName = "WOLA-" + endpointId;
        chainName = "CHAIN-" + endpointId;
    }

    /**
     * Remove the chain and all channels.
     */
    protected void removeChainAndChannels() {
        try {
            removeChain();
            removeChannels();
        } catch (ChannelException e) {
            // Not much we can do.  Let FFDC record it.
        } catch (ChainException e) {
            // Not much we can do.  Let FFDC record it.
        }
    }

    /**
     * Stop, destroy, and remove any existing chain known to the framework.
     *
     * @throws ChainException
     * @throws ChannelException
     */
    protected void removeChain() throws ChainException, ChannelException {
        ChainData cd = cfw.getChain(chainName);
        if (cd != null) {
            // Setting the 2nd parm to 0 tells CFW to stop the chain NOW (any other
            // value is used as a quiesce delay (in ms), meaning the chain isn't actually
            // stopped until after the delay.  If the chain isn't stopped immediately,
            // then the call to destroyChain will fail, since the chain isn't stopped yet.
            cfw.stopChain(cd, 0);
            cfw.destroyChain(cd);
            cfw.removeChain(cd);
        }
    }

    /**
     * Remove the LocalComm and WOLA channels from the framework.
     */
    protected void removeChannels() {

        // Remove any existing channels known to the framework. Currently there chains cannot
        // be updated dynamically.
        // TODO: We probably do not want to do this unless we need to (i.e. when config changes).
        removeChannel(localCommChannelName);
        removeChannel(wolaChannelName);
    }

    /**
     * Updates the chain.
     */
    public synchronized void update(Map<String, Object> config) {

        try {

            removeChain();
            removeChannels();

            // Make the framework aware of the Local Comm Channel if it is not already known.
            if (cfw.getChannel(localCommChannelName) == null) {
                cfw.addChannel(localCommChannelName, cfw.lookupFactory("LocalCommChannel"), new HashMap<Object, Object>());
            }

            // Make the framework aware of the WOLA channel if it is not already known.
            if (cfw.getChannel(wolaChannelName) == null) {
                cfw.addChannel(wolaChannelName, cfw.lookupFactory("WOLAChannel"), new HashMap<Object, Object>());
            }

            // Make the framework aware of this chain and initialize it.
            ChainData cd = cfw.getChain(chainName);
            if (cd == null) {
                final String[] chanList = new String[] { localCommChannelName, wolaChannelName };

                cd = cfw.addChain(chainName, FlowType.INBOUND, chanList);
                cd.setEnabled(true);
                cfw.addChainEventListener(this, chainName);
                cfw.initChain(chainName);
            }

            // Start the chain.
            cfw.startChain(chainName);

        } catch (ChannelException e) {
            // TODO.
        } catch (ChainException e) {
            // TODO.
        } catch (Exception e) {
            // TODO.
        }
    }

    /**
     * Remove a channel from the framework.
     *
     * @param name The channel to be removed.
     */
    @FFDCIgnore({ ChannelException.class, ChainException.class })
    private void removeChannel(String name) {
        // Neither of the thrown exceptions are permanent failures:
        // they usually indicate that we're the victim of a race.
        // If the CFW is also tearing down the chain at the same time
        // (for example, the SSL feature was removed), then this could
        // fail.
        try {
            cfw.removeChannel(name);
        } catch (ChannelException e) {
            // Ignore.
        } catch (ChainException e) {
            // Ignore.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void chainInitialized(ChainData chainData) {
        chainState.set(ChainState.INITIALIZED.value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void chainStarted(ChainData chainData) {
        chainState.set(ChainState.STARTED.value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void chainStopped(ChainData chainData) {
        chainState.set(ChainState.STOPPED.value);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void chainQuiesced(ChainData chainData) {
        chainState.set(ChainState.QUIESCED.value);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void chainDestroyed(ChainData chainData) {
        chainState.set(ChainState.DESTROYED.value);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void chainUpdated(ChainData chainData) {
    }

    /**
     * toString.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" [ChainName: ").append((chainName == null) ? "UNKNOWN" : chainName);
        sb.append("LocalCommChannelName: ").append((localCommChannelName == null) ? "UNKNOWN" : localCommChannelName);
        sb.append("WOLACommChannelName: ").append((wolaChannelName == null) ? "UNKNOWN" : wolaChannelName);
        sb.append(", State:").append(ChainState.printState(chainState.get()));
        sb.append("]");

        return sb.toString();
    }

}
