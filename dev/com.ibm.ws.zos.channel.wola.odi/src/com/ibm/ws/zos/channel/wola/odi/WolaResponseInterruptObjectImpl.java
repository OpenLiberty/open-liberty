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

import java.util.concurrent.Future;

/**
 * To note in this class - cancel does not need to be synchronized with interrupt
 * since the backing future object can support concurrent callers.
 */
public class WolaResponseInterruptObjectImpl implements WolaInterruptObject {

    private final Future<?> responseFuture;
    private boolean triedIt = false;
    private boolean isCancelled = false;

    WolaResponseInterruptObjectImpl(Future<?> responseFuture) {
        this.responseFuture = responseFuture;
    }

    /** {@inheritDoc} */
    @Override
    public boolean interrupt() {
        triedIt = true;
        if (isCancelled == false) {
            return responseFuture.cancel(true);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean queryTried() {
        return triedIt;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "WOLA_RESPONSE";
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayInfo() {
        return "ResponseFuture: " + responseFuture.toString();
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        isCancelled = true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

}
