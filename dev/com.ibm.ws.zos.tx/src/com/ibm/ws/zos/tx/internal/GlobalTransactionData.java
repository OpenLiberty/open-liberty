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

import java.util.ArrayList;
import java.util.List;

import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.impl.XidImpl;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Stores data associated with a global transaction.
 */
public class GlobalTransactionData extends TransactionData {

    /**
     * Indicates whether the transaction has been created as a result of recovery.
     */
    private final boolean isRestarted;

    /**
     * The unit of recovery ID.
     */
    private final byte[] urid;

    /**
     * The unit of recovery token.
     */
    private final byte[] urToken;

    /**
     * The transaction ID
     */
    private final XidImpl xid;

    /**
     * Indicates whether or not start has been invoked. It is set on first start.
     */
    private final boolean xaStarted;

    /**
     * An object encapsulating the context token.
     */
    private final Context contextTokenWrapper;

    /**
     * The unit of recovery interest token.
     */
    private byte[] uriToken;

    /**
     * The native registry token used to look up the URI token in the native
     * registry. This must be used to call most authorized services that
     * require a URI token, to prevent unauthorized callers from modifying
     * aribtrary URIs.
     */
    private byte[] uriRegistryToken;

    /**
     * The resource manager token.
     */
    private final byte[] rmToken;

    /**
     * The resource manager token registry token.
     */
    private final byte[] rmRegistryToken;

    /**
     * The resource manager name.
     */
    private final String rmName;

    /**
     * The resource manager name registry token.
     */
    private final byte[] rmNameRegistryToken;

    /**
     * The unit of recovery state. For main line processing it stores state
     * information based on the transaction manager perception of the state.
     * For recovery processing, it stores state information based on RRS'
     * perception of the state.
     */
    private int urState;

    /**
     * Indicates whether the unit of recovery has been forgotten.
     */
    private boolean isURForgotten;

    /**
     * The persistent interest data wrapper.
     */
    private PersistentInterestData pdataWrapper;

    /**
     * The persistent interest data version.
     */
    public static final byte CURRENT_PDATA_VERSION = 1;

    /**
     * Main line path Constructor.
     */
    public GlobalTransactionData(XidImpl xid,
                                 String rmName,
                                 byte[] rmNameRegistryToken,
                                 byte[] rmToken,
                                 byte[] rmRegistryToken,
                                 byte[] urid,
                                 byte[] urToken,
                                 Context contextTokenWrapper,
                                 int urState,
                                 boolean xaStarted) {
        super(new ArrayList<XAResource>());
        this.rmName = rmName;
        this.rmNameRegistryToken = rmNameRegistryToken;
        this.rmToken = rmToken;
        this.rmRegistryToken = rmRegistryToken;
        this.urToken = urToken;
        this.urid = urid;
        this.xid = xid;
        this.xaStarted = xaStarted;
        this.contextTokenWrapper = contextTokenWrapper;
        this.urState = urState;
        this.isRestarted = false;
    }

    /**
     * Recovery constructor.
     */
    public GlobalTransactionData(RestartURData restartURData, Context contextTokenWrapper) {
        super(null);
        this.urToken = null;
        this.urid = restartURData.getURId();
        this.uriToken = restartURData.getUriToken();
        this.uriRegistryToken = restartURData.getUriRegistryToken();
        this.xid = restartURData.getXid();
        this.urState = restartURData.getState();
        this.rmName = restartURData.getRMName();
        this.rmNameRegistryToken = restartURData.getRMNameRegistryToken();
        this.rmToken = restartURData.getRMToken();
        this.rmRegistryToken = restartURData.getRMRegistryToken();
        this.xaStarted = false;
        this.contextTokenWrapper = contextTokenWrapper;
        this.isRestarted = true;
        this.pdataWrapper = new PersistentInterestData(restartURData.getPdata());
    }

    /**
     * Retrieves the transaction ID associated with the transaction.
     *
     * @return The transaction ID associated with the transaction.
     */
    public XidImpl getXid() {
        return xid;
    }

    /**
     * Retrieves the unit of recovery ID.
     *
     * @return the Unit of recovery ID.
     */
    public byte[] getURId() {
        return urid;
    }

    /**
     * Retrieves the unit of recovery token.
     *
     * @return The unit of recovery token.
     */
    public byte[] getURToken() {
        return urToken;
    }

    /**
     * Retrieves the unit of recovery interest token.
     *
     * @return The unit of recovery interest token.
     */
    public byte[] getURIToken() {
        return uriToken;
    }

    /**
     * Retrieves the token used to look up the unit of recovery interest (URI)
     * token in the native registry. Use of the registry token is required
     * when calling services which modify the URI to prevent unauthorized
     * callers from modifying arbitrary URIs.
     *
     * @return The registry token used to look up the URI token in the native
     *         registry.
     */
    public byte[] getURIRegistryToken() {
        byte[] uriRegistryTokenCopy = null;
        if (uriRegistryToken != null) {
            uriRegistryTokenCopy = new byte[uriRegistryToken.length];
            System.arraycopy(uriRegistryToken, 0, uriRegistryTokenCopy, 0, uriRegistryToken.length);
        }
        return uriRegistryTokenCopy;
    }

    /**
     * Sets the unit of recovery interest token.
     *
     * @param uriToken         The unit of recovery interest token.
     * @param uriRegistryToken The token used to look up the URI token in the
     *                             native registry.
     */
    public void setURIToken(byte[] uriToken, byte[] uriRegistryToken) {
        if (uriToken == null) {
            throw new IllegalArgumentException("The supplied URI token was null");
        }
        if (uriRegistryToken == null) {
            throw new IllegalArgumentException("The supplied URI registry token was null");
        }

        this.uriToken = new byte[uriToken.length];
        System.arraycopy(uriToken, 0, this.uriToken, 0, uriToken.length);
        this.uriRegistryToken = new byte[uriRegistryToken.length];
        System.arraycopy(uriRegistryToken, 0, this.uriRegistryToken, 0, uriRegistryToken.length);
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
     * Retrieves the resource manager token registry token.
     *
     * @return The resource manager token registry token.
     */
    public byte[] getRMRegistryToken() {
        return rmRegistryToken;
    }

    /**
     * Retrieves the XAResource.start() called indicator.
     *
     * @return True if XAResource.start() processing for this transaction
     *         has been called. False otherwise.
     */
    public boolean getXAStarted() {
        return xaStarted;
    }

    /**
     * Retrieves the context token wrapper object associated to the context under which the transaction
     * is executing.
     *
     * @return The context token wrapper object.
     */
    public Context getContext() {
        return contextTokenWrapper;
    }

    /**
     * Retrieves the unit of recovery forgotten indicator.
     *
     * @return True if the unit of recovery has been forgotten. False otherwise.
     */
    public boolean isUrForgotten() {
        return isURForgotten;
    }

    /**
     * Sets the unit of recovery forgotten indicator.
     *
     * @param value True if the unit of recovery has been forgotten. False otherwise.
     */
    public void setUrForgotten(boolean value) {
        isURForgotten = value;
    }

    /**
     * Retrieves the unit of recovery's state.
     *
     * @return The unit of recovery's state.
     */
    public int getURState() {
        return urState;
    }

    /**
     * Sets the unit of recovery's state.
     *
     * @param state The unit of recovery's state.
     */
    public void setURState(int state) {
        urState = state;
    }

    /**
     * Retrieves the restarted transaction indicator.
     *
     * @return True if the transaction is a restarted transaction. False otherwise.
     */
    public boolean isRestarted() {
        return isRestarted;
    }

    /**
     * Sets the persistent interest data wrapper.
     *
     * @param pdataWrapper The persistent interest data wrapper.
     */
    public void setPersistentInterestDataWrapper(PersistentInterestData pdataWrapper) {
        this.pdataWrapper = pdataWrapper;
    }

    /**
     * Retrieves the persistent interest data wrapper.
     *
     * @return The persistent interest data wrapper.
     */
    public PersistentInterestData getPersistentInterestDataWrapper() {
        return pdataWrapper;
    }

    /**
     * Collects the data associated with the global transaction.
     *
     * @return A string representation of the data associated with the global transaction.
     */
    @Trivial
    @FFDCIgnore(Throwable.class)
    public String getData() {
        StringBuilder sb = new StringBuilder();

        try {
            List<XAResource> xaResourceList = getXAResourceList();
            sb.append("GlobalTransactionData [GTRID: ");
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
            sb.append("\nURToken: ");
            sb.append((urToken == null) ? "NULL" : Util.toHexString(urToken));
            sb.append("\nURIToken: ");
            sb.append((uriToken == null) ? "NULL" : Util.toHexString(uriToken));
            sb.append("\nURIRegistryToken: ");
            sb.append((uriRegistryToken == null) ? "NULL" : Util.toHexString(uriRegistryToken));
            sb.append("\nIsURForgotten: ");
            sb.append(isURForgotten);
            sb.append("\nURState: ");
            sb.append(urState);
            sb.append("\nIsRestarted: ");
            sb.append(isRestarted);
            sb.append("\nIsXAStartInvoked: ");
            sb.append(xaStarted);
            sb.append("\nContextData: ");
            sb.append((contextTokenWrapper == null) ? "NULL" : contextTokenWrapper.toString());
            sb.append("\nNativeGlobalXAResourcesParticipatingInTran: ");
            sb.append(xaResourceList.size());

            if (xaResourceList.size() > 0) {
                sb.append("\n{");
                for (XAResource resource : xaResourceList) {
                    sb.append("\n" + resource);
                    sb.append("[" + ((NativeGlobalXAResource) resource).getData() + "]");
                }
                sb.append("\n}");
            }
        } catch (Throwable t) {
            sb.append("\nGlobalTransactionData.getData error: " + t.toString());
        }

        return sb.toString();
    }
}