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
 * RRS' express UR interest (atr4eint) service.
 */
public final class ExpressInterestReturnType {

    /**
     * The RRS service's return code.
     */
    private final int rc;

    /**
     * The UR interest token.
     */
    private final byte[] uriToken;

    /**
     * The token used to look up the UR interest token in the native registry.
     */
    private final byte[] uriRegistryToken;

    /**
     * The current context token.
     */
    private final byte[] ctxToken;

    /**
     * The unit of recovery id.
     */
    private final byte[] urid;

    /**
     * The current non-persistent interest data.
     */
    private final byte[] nonPData;

    /**
     * The diagnostic area.
     */
    private final byte[] diag;

    /**
     * The transaction mode.
     */
    private final int mode;

    /**
     * The unit of recovery Token
     */
    private final byte[] urToken;

    /**
     * Constructor
     */
    public ExpressInterestReturnType(int rrsReturnCode,
                                     byte[] uriToken,
                                     byte[] uriRegistryToken,
                                     byte[] ctxToken,
                                     byte[] urid,
                                     byte[] nonPData,
                                     byte[] diag,
                                     int mode,
                                     byte[] urToken) {
        this.rc = rrsReturnCode;
        this.uriToken = uriToken;
        this.ctxToken = ctxToken;
        this.urid = urid;
        this.nonPData = nonPData;
        this.diag = diag;
        this.mode = mode;
        this.urToken = urToken;
        if (uriRegistryToken != null) {
            this.uriRegistryToken = new byte[uriRegistryToken.length];
            System.arraycopy(uriRegistryToken, 0, this.uriRegistryToken, 0, uriRegistryToken.length);
        } else {
            this.uriRegistryToken = null;
        }
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
     *
     * @return
     */
    @Trivial
    public byte[] getUriToken() {
        return uriToken;
    }

    /**
     * Retrieves the token used to look up the URI token in the native registry.
     * The registry token is required to call services which run authorized and
     * require the URI token.
     *
     * @return The token used to look up the URI token in the native registry.
     */
    @Trivial
    public byte[] getUriRegistryToken() {
        byte[] uriRegistryTokenCopy = null;
        if (uriRegistryToken != null) {
            uriRegistryTokenCopy = new byte[uriRegistryToken.length];
            System.arraycopy(uriRegistryToken, 0, uriRegistryTokenCopy, 0, uriRegistryToken.length);
        }
        return uriRegistryTokenCopy;
    }

    /**
     * Retrieves a 16 byte context token
     *
     * @return The context token.
     */
    @Trivial
    public byte[] getCtxToken() {
        return ctxToken;
    }

    /**
     * Retrieves a 16 byte unit of recovery id.
     *
     * @return The unit of recovery id.
     */
    @Trivial
    public byte[] getUrid() {
        return urid;
    }

    /**
     * Retrieves the non persistent interest data.
     *
     * @return The non persistent interest data.
     */
    @Trivial
    public byte[] getNonPData() {
        return nonPData;
    }

    /**
     * Retrieves that diagnostic information.
     * Useful when a service fails to complete successfully.
     *
     * @return the diagnostic area.
     */
    @Trivial
    public byte[] getDiagArea() {
        return diag;
    }

    /**
     * Retrieves the transaction mode.
     *
     * @return The transaction mode.
     */
    @Trivial
    public int getTransactionMode() {
        return mode;
    }

    /**
     * Retrieves a 16 by unit of recovery token.
     *
     * @return the unit of recovery token.
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
        sb.append("ExpressInterestReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", URIToken: ");
        sb.append((uriToken == null) ? "null" : new ByteArray(uriToken).toString());
        sb.append(", URIRegistryToken: ");
        sb.append((uriRegistryToken == null) ? "null" : new ByteArray(uriRegistryToken).toString());
        sb.append(", CTXToken: ");
        sb.append((ctxToken == null) ? "null" : new ByteArray(ctxToken).toString());
        sb.append(", URId: ");
        sb.append((urid == null) ? "null" : new ByteArray(urid).toString());
        sb.append(", NonPData: ");
        sb.append((nonPData == null) ? "null" : new ByteArray(nonPData).toString());
        sb.append(", DiagData: ");
        sb.append((diag == null) ? "null" : new ByteArray(diag).toString());
        sb.append(", TransactionMode: ");
        sb.append(mode);
        sb.append(", URToken: ");
        sb.append((urToken == null) ? "null" : new ByteArray(urToken).toString());
        sb.append("]");
        return sb.toString();
    }
}
