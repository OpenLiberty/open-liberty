/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
 * the system's retrieve current context token (ctx4rcc) service.
 */
public class RetrieveCurrentContextTokenReturnType {

    /**
     * The service's return code.
     */
    private final int rc;

    /**
     * The context token.
     */
    private final byte[] ctxToken;

    /**
     * Constructor
     *
     * @param rc       The return code from the RetrieveCurrentContextToken service.
     * @param ctxToken The current context token.
     */
    public RetrieveCurrentContextTokenReturnType(int rc, byte[] ctxToken) {
        this.rc = rc;

        if (ctxToken != null) {
            this.ctxToken = new byte[ctxToken.length];
            System.arraycopy(ctxToken, 0, this.ctxToken, 0, ctxToken.length);
        } else {
            this.ctxToken = null;
        }
    }

    /**
     * Gets the return code from the RetrieveCurrentContextToken service.
     *
     * @return The return code from the RetrieveCurrentContextToken service.
     */
    @Trivial
    public int getReturnCode() {
        return rc;
    }

    /**
     * Gets the context token from the RetrieveCurrentContextToken service.
     *
     * @return The context token from the RetrieveCurrentContextToken service.
     */
    @Trivial
    public byte[] getContextToken() {
        byte[] ctxTokenCopy = null;

        if (ctxToken != null) {
            ctxTokenCopy = new byte[ctxToken.length];
            System.arraycopy(ctxToken, 0, ctxTokenCopy, 0, ctxToken.length);
        }

        return ctxTokenCopy;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RetrieveCurrentContextTokenReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", ContextToken: ");
        sb.append((ctxToken == null) ? "null" : new ByteArray(ctxToken).toString());
        sb.append("]");
        return sb.toString();
    }
}
