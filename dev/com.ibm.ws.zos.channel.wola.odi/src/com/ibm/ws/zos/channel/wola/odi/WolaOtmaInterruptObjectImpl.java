/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.odi;

/**
 * This interrupt object will cancel an in-flight OTMA request (using OTMA C/I).
 */
public class WolaOtmaInterruptObjectImpl implements WolaInterruptObject {

    private final WolaInterruptObjectBridgeImpl bridge;
    private final long otmaAnchor;
    private final long otmaSessionId;
    private final int ecbPtr;
    private boolean interruptDriven = false;
    private boolean isCancelled = false;

    /**
     * Constructor
     *
     * @param bridge        The bridge that has the native methods on it.
     * @param otmaAnchor    The OTMA anchor that was used to create the OTMA session
     * @param otmaSessionId The OTMA session ID that we will cancel.
     * @param ecbPtr        The ECB pointer that we'll need to post after canceling.
     */
    WolaOtmaInterruptObjectImpl(WolaInterruptObjectBridgeImpl bridge, long otmaAnchor, long otmaSessionId, int ecbPtr) {
        this.bridge = bridge;
        this.otmaAnchor = otmaAnchor;
        this.otmaSessionId = otmaSessionId;
        this.ecbPtr = ecbPtr;
    }

    /**
     * Note that this method is synchronized with cancel.
     */
    @Override
    public synchronized boolean interrupt() {
        interruptDriven = true;

        if (isCancelled == false) {
            bridge.ntv_cancelOtmaRequest(otmaAnchor, otmaSessionId, ecbPtr);
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean queryTried() {
        return interruptDriven;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "WOLA_OTMA";
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayInfo() {
        return "Anchor " + Long.toHexString(otmaAnchor) + " Session " + Long.toHexString(otmaSessionId) + " ECB " + Integer.toHexString(ecbPtr);
    }

    /**
     * Cancel the InterruptObject. This method is synchronized with interrupt()
     * such that only one can run concurrently.
     */
    @Override
    public synchronized void cancel() {
        isCancelled = true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCancelled() {
        return isCancelled;
    }
}
