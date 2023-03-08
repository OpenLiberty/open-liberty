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
 * context services' context switch (ctx4swch) service.
 */
public final class SwitchContextReturnType {

    /**
     * The service's return code
     */
    private final int rc;

    /**
     * The context token displaced by the new one switched on the
     * current thread.
     */
    private final byte[] oldCtxToken;

    /**
     * Constructor
     */
    public SwitchContextReturnType(int rc, byte[] oldCtxToken) {
        this.rc = rc;
        this.oldCtxToken = oldCtxToken;
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
     * Retrieves The token representing the context displaced by the
     * new one switched on the current thread.
     *
     * @return The old context token.
     */
    public byte[] getOldCtxToken() {
        return oldCtxToken;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SwitchContextReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", OldContextToken: ");
        sb.append((oldCtxToken == null) ? "null" : new ByteArray(oldCtxToken).toString());
        sb.append("]");
        return sb.toString();
    }
}
