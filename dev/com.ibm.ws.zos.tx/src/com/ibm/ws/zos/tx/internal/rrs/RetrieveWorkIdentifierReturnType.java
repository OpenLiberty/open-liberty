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
 * RRS' retrieve work identifier (atr4rwid) service.
 */
public final class RetrieveWorkIdentifierReturnType {

    /**
     * The service's return code.
     */
    private final int rc;

    /**
     * The transaction id.
     */
    private final byte[] xid;

    /**
     * Constructor
     */
    public RetrieveWorkIdentifierReturnType(int rrsReturnCode, byte[] xid) {
        this.rc = rrsReturnCode;
        this.xid = xid;
    }

    /**
     * Retrieves the service's return code.
     *
     * @return The service's return code.
     */
    @Trivial
    public int getRRSReturnCode() {
        return rc;
    }

    /**
     * Retrieves the transaction id in byte form.
     *
     * @return The transaction id in byte form.
     */
    @Trivial
    public byte[] getXid() {
        return xid;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RetrieveWorkIdentifierReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", XID: ");
        sb.append((xid == null) ? "null" : new ByteArray(xid).toString());
        sb.append("]");
        return sb.toString();
    }
}
