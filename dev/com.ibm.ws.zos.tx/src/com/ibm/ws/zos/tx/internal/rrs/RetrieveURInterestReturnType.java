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
 * RRS' retrieve UR (Unit of Recovery) interest (atr4irni) service.
 */
public final class RetrieveURInterestReturnType {

    /**
     * The RRS service's return code.
     */
    private final int rc;

    /**
     * The context token.
     */
    private final byte[] ctxToken;

    /**
     * The unit of recovery Interest token.
     */
    private final byte[] uriToken;

    /**
     * The registry token for the URI token.
     */
    private final byte[] uriRegistryToken;

    /**
     * The unit of recovery identifier.
     */
    private final byte[] urid;

    /**
     * The resource manager role
     */
    private final int role;

    /**
     * The unit of recovery state.
     */
    private final int state;

    /**
     * The persistent interest data.
     */
    private final byte[] pdata;

    /**
     * Constructor
     */
    public RetrieveURInterestReturnType(int rc,
                                        byte[] ctxToken,
                                        byte[] uriToken,
                                        byte[] uriRegistryToken,
                                        byte[] urid,
                                        int role,
                                        int state,
                                        byte[] pdata) {
        this.rc = rc;
        this.ctxToken = ctxToken;
        this.uriToken = uriToken;
        this.urid = urid;
        this.role = role;
        this.state = state;
        this.pdata = pdata;
        if (uriRegistryToken != null) {
            this.uriRegistryToken = new byte[uriRegistryToken.length];
            System.arraycopy(uriRegistryToken, 0, this.uriRegistryToken, 0, uriRegistryToken.length);
        } else {
            this.uriRegistryToken = null;
        }
    }

    /**
     * Retrieves the RRS service's return code.
     *
     * @return The service's return code.
     */
    @Trivial
    public int getReturnCode() {
        return rc;
    }

    /**
     * Retrieves a 16 byte context token.
     *
     * @return The context token.
     */
    @Trivial
    public byte[] getContextToken() {
        return ctxToken;
    }

    /**
     * Retrieves a 16 byte unit of recovery interest token.
     *
     * @return The unit of recovery interest token.
     */
    @Trivial
    public byte[] getUriToken() {
        return uriToken;
    }

    /**
     * Retrieves the token used to look up the URI token in the native registry.
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
     * Retrieves a 16 byte unit of recovery id.
     *
     * @return The unit of recovery id.
     */
    @Trivial
    public byte[] getURID() {
        return urid;
    }

    /**
     * Retrieves the Resource manager role for the unit
     * of recovery.
     *
     * @return The resource manager role.
     */
    @Trivial
    public int getRole() {
        return role;
    }

    /**
     * Retrieves the unit of recovery's state.
     *
     * @return The unit of recovery's state.
     */
    @Trivial
    public int getState() {
        return state;
    }

    /**
     * Retrieves the persistent interest data.
     *
     * @return The persistent interest data.
     */
    @Trivial
    public byte[] getPdata() {
        return pdata;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RetrieveURInterestReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", CTXToken: ");
        sb.append((ctxToken == null) ? "null" : new ByteArray(ctxToken).toString());
        sb.append(", URIToken: ");
        sb.append((uriToken == null) ? "null" : new ByteArray(uriToken).toString());
        sb.append(", URIRegistryToken: ");
        sb.append((uriRegistryToken == null) ? "null" : new ByteArray(uriRegistryToken).toString());
        sb.append(", URId: ");
        sb.append((urid == null) ? "null" : new ByteArray(urid).toString());
        sb.append(", PData: ");
        sb.append((pdata == null) ? "null" : new ByteArray(pdata).toString());
        sb.append(", Role: ");
        sb.append(role);
        sb.append(", State: ");
        sb.append(state);
        sb.append("]");
        return sb.toString();
    }
}