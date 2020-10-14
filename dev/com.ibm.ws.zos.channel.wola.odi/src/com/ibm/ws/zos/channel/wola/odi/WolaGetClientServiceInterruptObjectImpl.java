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
 * This will interrupt regular WOLA requests (non-OTMA).
 */
public class WolaGetClientServiceInterruptObjectImpl implements WolaInterruptObject {

    private final WolaInterruptObjectBridgeImpl bridgeImpl;
    private final byte[] wolaGroupBytes;
    private final byte[] registerNameBytes;
    private final long waiterToken;
    private boolean interrupted = false;
    private boolean isCancelled = false;

    /**
     * Constructor.
     *
     * @param bridgeImpl        The bridge to the native methods.
     * @param wolaGroupBytes    The WOLA group that this server connected to
     * @param registerNameBytes The register name we want to send the request to
     * @param waiterToken       The token used when we created the waiter.
     */
    WolaGetClientServiceInterruptObjectImpl(WolaInterruptObjectBridgeImpl bridgeImpl, byte[] wolaGroupBytes, byte[] registerNameBytes, long waiterToken) {
        this.bridgeImpl = bridgeImpl;
        this.wolaGroupBytes = wolaGroupBytes;
        this.registerNameBytes = registerNameBytes;
        this.waiterToken = waiterToken;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean interrupt() {
        interrupted = true;

        if (isCancelled == false) {
            bridgeImpl.ntv_cancelWolaClientWaiter(wolaGroupBytes, registerNameBytes, waiterToken);
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean queryTried() {
        return interrupted;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "WOLA_GET_CLIENT";
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayInfo() {
        try {
            return "GROUP " + new String(wolaGroupBytes, "Cp1047").trim() + " RGE " + new String(registerNameBytes, "Cp1047").trim() + " Token " + Long.toHexString(waiterToken);
        } catch (Throwable t) {
            return "UNAVAILABLE (" + t.getClass().getSimpleName() + ")";
        }
    }

    /** {@inheritDoc} */
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
