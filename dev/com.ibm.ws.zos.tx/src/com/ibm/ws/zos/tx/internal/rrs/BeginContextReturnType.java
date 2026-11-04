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
 * context services' ctx4begc service.
 */
public final class BeginContextReturnType {

    /**
     * The service's return code.
     */
    private final int rc;

    /**
     * The newly created privately-managed context.
     */
    private final byte[] ctxToken;

    /**
     * The registry token where the context token is stored. The registry
     * token is used on switch and end context calls.
     */
    private final byte[] ctxRegistryToken;

    /**
     * Constructor
     */
    public BeginContextReturnType(int rc, byte[] ctxToken, byte[] ctxRegistryToken) {
        this.rc = rc;
        this.ctxToken = ctxToken;
        this.ctxRegistryToken = ctxRegistryToken;
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
     * Retrieves the newly created privately-managed context.
     *
     * @return The new context token.
     */
    @Trivial
    public byte[] getContextToken() {
        return ctxToken;
    }

    /**
     * Retrieves the context registry token, used for switch and end context.
     *
     * @return The context registry token.
     */
    @Trivial
    public byte[] getContextRegistryToken() {
        return ctxRegistryToken;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("BeginContextReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", ContextToken: ");
        sb.append((ctxToken == null) ? "null" : new ByteArray(ctxToken).toString());
        sb.append(", ContextRegistryToken: ");
        sb.append((ctxRegistryToken == null) ? "null" : new ByteArray(ctxRegistryToken).toString());
        sb.append("]");
        return sb.toString();
    }
}
