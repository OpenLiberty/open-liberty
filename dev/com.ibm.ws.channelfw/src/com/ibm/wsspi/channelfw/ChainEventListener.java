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
package com.ibm.wsspi.channelfw;

import com.ibm.websphere.channelfw.ChainData;

/**
 * A ChainEventListener is notified whenever lifecycle events such as stop and
 * start
 * happen on a chain. A class implementing this interface needs to be registered
 * into
 * the ChannelFramework in order for the events to be called. This may be
 * registered
 * all chains using the defined name below.
 * 
 */
public interface ChainEventListener {

    /** Identifier used to register a listener for all chains in the framework. */
    String ALL_CHAINS = "all_chains";

    /**
     * Event marking the chain initialization stage.
     * 
     * @param chainData
     */
    void chainInitialized(ChainData chainData);

    /**
     * Event marking the chain started stage.
     * 
     * @param chainData
     */
    void chainStarted(ChainData chainData);

    /**
     * Event marking the chain stopped stage.
     * 
     * @param chainData
     */
    void chainStopped(ChainData chainData);

    /**
     * Event marking the chain quiesced stage.
     * 
     * @param chainData
     */
    void chainQuiesced(ChainData chainData);

    /**
     * Event marking the chain destroyed stage.
     * 
     * @param chainData
     */
    void chainDestroyed(ChainData chainData);

    /**
     * Event marking the chain configuration updated stage.
     * 
     * @param chainData
     */
    void chainUpdated(ChainData chainData);

}
