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

import javax.transaction.xa.XAException;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.zos.tx.internal.rrs.RRSServices;
import com.ibm.ws.zos.tx.internal.rrs.RegistryException;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveSideInformationReturnType;

/**
 * Retrieves asynchronous syncpoint processing data for a particular unit of
 * recovery interest.
 */
public class AsyncURSyncpointData {

    /**
     * Backout required bit. When true, the UR was marked backout-only
     * before the syncpoint began.
     */
    boolean backoutRequired = false;

    /**
     * SDSRM initiated bit. This means that the SDSRM started the syncpoint.
     * It does NOT tell us that the SDSRM resolved the syncpoint.
     */
    boolean sdsrmInitiated = false;

    /**
     * Resolved in-doubt bit. The syncpoint was resolved by the RRS panels.
     */
    boolean resolvedInDoubt = false;

    /**
     * Terminating syncpoint bit. Syncpoint was initiated by an 'end of
     * context' condition.
     */
    boolean terminatingSyncpoint = false;

    /**
     * Application backout bit. An application initiated the backout.
     */
    boolean applicationBackout = false;

    /**
     * Committed bit. The syncpoint result was commit. If not set, the result
     * was backout.
     */
    boolean committed = false;

    /**
     * Heuristic mixed bit. The syncpoint ended with a mixed outcome.
     */
    boolean heuristicMixed = false;

    /**
     * Constructor.
     *
     * @param uriToken         The unit of recovery interest token for which the data is to retrieved. This is used to aid in debugging.
     * @param uriRegistryToken The token used to look up the unit of recovery interest token for which the data is to retrieved, in the native registry.
     * @param rrsServices      The RRSServices object reference.
     * @throws XAException
     */
    public AsyncURSyncpointData(NativeTransactionManager natvTxMgr, byte[] uriToken, byte[] uriRegistryToken, RRSServices rrsServices) throws XAException, RegistryException {
        int[] sideInfoIdArray = new int[7];
        sideInfoIdArray[0] = RRSServices.ATR_BACKOUT_REQUIRED;
        sideInfoIdArray[1] = RRSServices.ATR_SDSRM_INITIATED;
        sideInfoIdArray[2] = RRSServices.ATR_RESOLVED_BY_INSTALLATION;
        sideInfoIdArray[3] = RRSServices.ATR_TERM_SYNCPOINT;
        sideInfoIdArray[4] = RRSServices.ATR_IMMEDIATE_BACKOUT;
        sideInfoIdArray[5] = RRSServices.ATR_COMMITTED;
        sideInfoIdArray[6] = RRSServices.ATR_HEURISTIC_MIX;

        RetrieveSideInformationReturnType rtype = rrsServices.retrieveSideInformation(uriRegistryToken, sideInfoIdArray);

        int rc = rtype.getReturnCode();

        switch (rc) {
            case RRSServices.ATR_OK:
                int[] sideInfoValues = rtype.getSideInfo();
                backoutRequired = (sideInfoValues[0] == RRSServices.ATR_SIDE_VALUE_SET);
                sdsrmInitiated = (sideInfoValues[1] == RRSServices.ATR_SIDE_VALUE_SET);
                resolvedInDoubt = (sideInfoValues[2] == RRSServices.ATR_SIDE_VALUE_SET);
                terminatingSyncpoint = (sideInfoValues[3] == RRSServices.ATR_SIDE_VALUE_SET);
                applicationBackout = (sideInfoValues[4] == RRSServices.ATR_SIDE_VALUE_SET);
                committed = (sideInfoValues[5] == RRSServices.ATR_SIDE_VALUE_SET);
                heuristicMixed = (sideInfoValues[6] == RRSServices.ATR_SIDE_VALUE_SET);
                break;
            default:
                StringBuilder sb = new StringBuilder();
                sb.append("URIToken: ");
                sb.append((uriToken == null) ? "NULL" : Util.toHexString(uriToken));
                natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4RUSI", rc, XAException.XAER_RMERR, sb.toString());
        }
    }

    /**
     * Retrieves the backout required indicator.
     *
     * @return True if the backout indicator is set. False otherwise.
     */
    @Trivial
    public boolean getBackoutRequired() {
        return backoutRequired;
    }

    /**
     * Retrieves the SDSRM initiated indicator.
     *
     * @return True if the SDSRM initiated indicator is set. False otherwise.
     */
    @Trivial
    public boolean getSdsrmInitiated() {
        return sdsrmInitiated;
    }

    /**
     * Retrieves the resolved indoubt indicator.
     *
     * @return True if the resolved indoubt indicator is set. False otherwise.
     */
    @Trivial
    public boolean getResolvedInDoubt() {
        return resolvedInDoubt;
    }

    /**
     * Retrieves the terminating syncpoint indicator.
     *
     * @return True if the terminating syncpoint indicator is set. False otherwise.
     */
    @Trivial
    public boolean getTerminatingSyncpoint() {
        return terminatingSyncpoint;
    }

    /**
     * Retrieves the application backout indicator.
     *
     * @return True if the application backout indicator is set. False otherwise.
     */
    @Trivial
    public boolean getApplicationBackout() {
        return applicationBackout;
    }

    /**
     * Retrieves the committed indicator.
     *
     * @return True if the committed indicator is set. False otherwise.
     */
    @Trivial
    public boolean getCommitted() {
        return committed;
    }

    /**
     * Retrieves the heuristic mixed indicator.
     *
     * @return True if the heuristic mixed indicator is set. False otherwise.
     */
    @Trivial
    public boolean getHeuristicMixed() {
        return heuristicMixed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("AsyncURSyncpointData [");
        sb.append("IsBackoutRequired: ");
        sb.append(backoutRequired);
        sb.append(", isSDSRMInitiated: ");
        sb.append(sdsrmInitiated);
        sb.append(", isResolvedInDoubt: ");
        sb.append(resolvedInDoubt);
        sb.append(", isTerminatingSyncpoint: ");
        sb.append(terminatingSyncpoint);
        sb.append(", isApplicationBackout: ");
        sb.append(applicationBackout);
        sb.append(", isCommitted: ");
        sb.append(committed);
        sb.append(", isHeuristicMixed: ");
        sb.append(heuristicMixed);
        sb.append("]");
        return sb.toString();
    }
}