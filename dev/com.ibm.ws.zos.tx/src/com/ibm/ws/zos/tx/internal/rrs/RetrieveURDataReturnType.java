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
 * RRS' retrieve UR (Unit of Recovery) data (atr4rurd) service.
 */
public final class RetrieveURDataReturnType {

    /**
     * The service's return code.
     */
    private final int rc;

    /**
     * The UR identifier
     */
    private final byte[] urid;

    /**
     * The UR state
     */
    private final int state;

    /**
     * The UR token
     */
    private final byte[] urToken;

    /**
     * Constructor
     */
    public RetrieveURDataReturnType(int rc,
                                    byte[] urid,
                                    int state,
                                    byte[] urToken) {
        this.rc = rc;
        this.urid = urid;
        this.state = state;
        this.urToken = urToken;
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
     * Retrieves a 16 byte unit of recovery id.
     *
     * @return The unit of recovery id.
     */
    @Trivial
    public byte[] getURID() {
        return urid;
    }

    /**
     * Retrieves the unit of recovery state.
     *
     * @return The unit of recovery state.
     */
    @Trivial
    public int getState() {
        return state;
    }

    /**
     * Retrieves a 16 byte unit of recovery token.
     *
     * @return The unit of recovery token.
     */
    @Trivial
    public byte[] getUrToken() {
        return urToken;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RetrieveURDataReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", URId: ");
        sb.append((urid == null) ? "null" : new ByteArray(urid).toString());
        sb.append(", URState: ");
        sb.append(state);
        sb.append(", URToken: ");
        sb.append((urToken == null) ? "null" : new ByteArray(urToken).toString());
        sb.append("]");
        return sb.toString();
    }
}
