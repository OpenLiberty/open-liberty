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
 * RRS' retrieve log name (atr4irln) service.
 */
public final class RetrieveLogNameReturnType {

    /**
     * The service's return code.
     */
    private final int rc;

    /**
     * The Resource manager log name.
     */
    private final byte[] rmLogName;

    /**
     * The RRS log name.
     */
    private final byte[] rrsLogName;

    /**
     * Constructor
     */
    public RetrieveLogNameReturnType(int rc, byte[] rmLogName, byte[] rrsLogName) {

        this.rc = rc;
        this.rmLogName = rmLogName;
        this.rrsLogName = rrsLogName;
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
     * Retrieves the resource manager's log name.
     *
     * @return The resource manager's log name.
     */
    @Trivial
    public byte[] getRmLogName() {
        return rmLogName;
    }

    /**
     * Retrieves RRS's log name.
     *
     * @return RRS's log name.
     */
    @Trivial
    public byte[] getRRSLogName() {
        return rrsLogName;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RetrieveLogNameReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", RMLogName: ");
        sb.append((rmLogName == null) ? "null" : new ByteArray(rmLogName).toString());
        sb.append(", RRSLogName: ");
        sb.append((rrsLogName == null) ? "null" : new ByteArray(rrsLogName).toString());
        sb.append("]");
        return sb.toString();
    }
}
