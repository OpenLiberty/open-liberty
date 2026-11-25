/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.zos.tx.internal.rrs;

import com.ibm.tx.util.ByteArray;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Holds all information returned from the call to
 * RRS' begin transaction (atr4beg) service.
 */
public final class BeginTransactionReturnType {

    /**
     * The service's return code
     */
    private final int rc;

    /**
     * The unit of recovery token.
     */
    private final byte[] urToken;

    /**
     * The unit of recovery id.
     */
    private final byte[] urid;

    /**
     * Constructor
     */
    public BeginTransactionReturnType(int rc, byte[] urToken, byte[] urid) {
        this.rc = rc;
        this.urToken = urToken;
        this.urid = urid;
    }

    /**
     * Retrieves the service's return code.
     *
     * @return The service's return code.
     */
    @Trivial
    public int getReturnCode() {
        return rc;
    }

    /**
     * Retrieves a 16 byte token representing the newly created unit of recovery.
     * It does not persist across RM, system, or RRS restarts.
     *
     * @return The unit of recovery token.
     */
    @Trivial
    public byte[] getURToken() {
        return urToken;
    }

    /**
     * Retrieves a 16 byte id representing the newly created unit of recovery.
     * It persists across RM, system, or RRS restarts.
     *
     * @return The UR Id.
     */
    @Trivial
    public byte[] getURId() {
        return urid;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("BeginTransactionReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", URToken: ");
        sb.append((urToken == null) ? "null" : new ByteArray(urToken).toString());
        sb.append(", URId: ");
        sb.append((urid == null) ? "null" : new ByteArray(urid).toString());
        sb.append("]");
        return sb.toString();
    }
}