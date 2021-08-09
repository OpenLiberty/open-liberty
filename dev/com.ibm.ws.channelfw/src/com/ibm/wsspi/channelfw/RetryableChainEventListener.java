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
 * This listener adds an additional feature beyond the basic ChainEventListener
 * to allow those that register to be notified about failed attempts to start
 * chains.
 */
public interface RetryableChainEventListener extends ChainEventListener {

    /**
     * This method is called when an attempted to start a chain fails.
     * 
     * @param chainData
     *            chain which failed to restart
     * @param attemptsMade
     *            number of attempts made so far to start the chain
     * @param attemptsLeft
     *            number of attempts remaining to start the chain before giving up.
     *            Attempts left may be -1, indicating an unlimited number of overall
     *            attempts.
     */
    void chainStartFailed(ChainData chainData, int attemptsMade, int attemptsLeft);
}
