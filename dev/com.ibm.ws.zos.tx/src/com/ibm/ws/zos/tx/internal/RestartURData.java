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
package com.ibm.ws.zos.tx.internal;

import com.ibm.tx.jta.impl.XidImpl;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.JTA.Util;

/**
 * Stores restarted unit of recovery (UR) information.
 */
public class RestartURData {

    /**
     * The transaction ID.
     */
    final XidImpl xid;

    /**
     * The Unit of recovery ID.
     */
    final byte[] urid;

    /**
     * The Unit of recovery interest token.
     */
    final byte[] uriToken;

    /**
     * The token used to look up the Unit of recovery interest token in the
     * native registry.
     */
    byte[] uriRegistryToken;

    /**
     * Persistent interest data.
     */
    final byte[] pdata;

    /**
     * The resource manager token.
     */
    final byte[] rmToken;

    /**
     * The resource manager token registry token.
     */
    final byte[] rmRegistryToken;

    /**
     * The resource manager name.
     */
    final String rmName;

    /**
     * The resource manager name registry token
     */
    final byte[] rmNameRegistryToken;

    /**
     * The UR has a heuristic outcome.
     */
    final boolean heuristic;

    /**
     * Unit of recovery state.
     */
    final int urState;

    /**
     * Constructor.
     */
    public RestartURData(XidImpl xid,
                         String rmName,
                         byte[] rmNameRegistryToken,
                         byte[] rmToken,
                         byte[] rmRegistryToken,
                         byte[] urid,
                         byte[] uriToken,
                         byte[] uriRegistryToken,
                         boolean heuristic,
                         int urState,
                         byte[] pdata) {
        this.xid = xid;
        this.rmName = rmName;
        this.rmNameRegistryToken = rmNameRegistryToken;
        this.rmToken = rmToken;
        this.rmRegistryToken = rmRegistryToken;
        this.urid = urid;
        this.uriToken = uriToken;
        this.heuristic = heuristic;
        this.urState = urState;
        this.pdata = pdata;

        if (uriRegistryToken != null) {
            this.uriRegistryToken = new byte[uriRegistryToken.length];
            System.arraycopy(uriRegistryToken, 0, this.uriRegistryToken, 0, uriRegistryToken.length);
        } else {
            this.uriRegistryToken = null;
        }
    }

    /**
     * Retrieves the transaction ID for this unit of recovery (UR).
     *
     * @return The transaction ID for this UR.
     */
    public XidImpl getXid() {
        return xid;
    }

    /**
     * Retrieves the unit of recovery ID for this unit of recovery (UR).
     *
     * @return The unit of recovery ID for this UR.
     */
    public byte[] getURId() {
        return urid;
    }

    /**
     * Retrieves the unit of recovery interest token for this unit of recovery (UR).
     *
     * @return The unit of recovery interest token for this UR.
     */
    public byte[] getUriToken() {
        return uriToken;
    }

    /**
     * Gets the token used to look up the URI token in the native registry.
     *
     * @return The token used to look up the URI token in the native registry.
     */
    public byte[] getUriRegistryToken() {
        byte[] uriRegistryTokenCopy = null;
        if (uriRegistryToken != null) {
            uriRegistryTokenCopy = new byte[uriRegistryToken.length];
            System.arraycopy(uriRegistryToken, 0, uriRegistryTokenCopy, 0, uriRegistryToken.length);
        }
        return uriRegistryTokenCopy;
    }

    /**
     * Specifies whether or not this unit of recovery (UR) has a heuristic outcome.
     *
     * @return True if the UR has a heuristic outcome. False otherwise.
     */
    public boolean isHeuristic() {
        return heuristic;
    }

    /**
     * Retrieves the state for this unit of recovery (UR).
     *
     * @return The state of this UR.
     */
    public int getState() {
        return urState;
    }

    /**
     * Retrieves the persistent interest data for this unit of recovery (UR).
     *
     * @return The persistent interest data for this UR.
     */
    public byte[] getPdata() {
        return pdata;
    }

    /**
     * Retrieves the resource manager name.
     *
     * @return The resource manager name.
     */
    public String getRMName() {
        return rmName;
    }

    /**
     * Retrieves the resource manager name registry token.
     *
     * @return The resource manager name registry token.
     */
    protected byte[] getRMNameRegistryToken() {
        return rmNameRegistryToken;
    }

    /**
     * Retrieves the resource manager token.
     *
     * @return The resource manager token.
     */
    public byte[] getRMToken() {
        return rmToken;
    }

    /**
     * Retrieves the resource manager registry token.
     *
     * @return The resource manager registry token.
     */
    public byte[] getRMRegistryToken() {
        return rmRegistryToken;
    }

    /**
     * Prints the data associated with the restarted global transaction.
     *
     * @return A string representation of the data associated with the restarted global transaction.
     */
    @Trivial
    public String printData() {
        StringBuilder sb = new StringBuilder();
        sb.append("RestartURData [GTRID: ");
        sb.append((xid == null) ? "NULL" : Util.toHexString((xid.getGlobalTransactionId())) + "]");
        sb.append("\nResourceManagerName: ");
        sb.append(rmName);
        sb.append("\nResourceManagerNameRegistryToken: ");
        sb.append((rmNameRegistryToken == null) ? "NULL" : Util.toHexString(rmNameRegistryToken));
        sb.append("\nResourceManagerToken: ");
        sb.append((rmToken == null) ? "NULL" : Util.toHexString(rmToken));
        sb.append("\nResourceManagerRegistryToken: ");
        sb.append((rmRegistryToken == null) ? "NULL" : Util.toHexString(rmRegistryToken));
        sb.append("\nXid: ");
        sb.append((xid == null) ? "NULL" : xid.toString());
        sb.append("\nURId: ");
        sb.append((urid == null) ? "NULL" : Util.toHexString(urid));
        sb.append("\nURIToken: ");
        sb.append((uriToken == null) ? "NULL" : Util.toHexString(uriToken));
        sb.append("\nURIRegistryToken: ");
        sb.append((uriRegistryToken == null) ? "NULL" : Util.toHexString(uriRegistryToken));
        sb.append("\nIsHeuristic: ");
        sb.append(heuristic);
        sb.append("\nURState: ");
        sb.append(urState);
        sb.append("\nPersistentData: ");
        sb.append((pdata == null) ? "NULL" : Util.toHexString(pdata));

        return sb.toString();
    }
}
