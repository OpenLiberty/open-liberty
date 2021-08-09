/*******************************************************************************
 * Copyright (c) 1997,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.cm.handle.HandleList;
import com.ibm.ws.jca.cm.handle.HandleListInterface;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threadContext.ConnectionHandleAccessorImpl;

public final class ConnectionHandleManager implements UOWCallback {
    private static final TraceComponent tc = Tr.register(ConnectionHandleManager.class, J2CConstants.traceSpec, J2CConstants.messageFile);

    final static ConnectionHandleManager ltcHandleCollaborator = new ConnectionHandleManager();

    /**
     * Typically called by ConnectionManager to add
     */
    public static HandleList addHandle(HCMDetails hcmd) {
        ComponentMetaData cmd = getComponentMetaData();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "addHandle", hcmd._handle);

        HandleListInterface hli = getHandleList(cmd);
        HandleList hl;

        if (hli != null) {
            hl = hli.addHandle(hcmd);
        } else {
            hl = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "addHandle", hl);

        return hl;
    }

    /**
     * Notification from the Tx Service that a transaction context change is in
     * progress. Note that these are specifically UserTransaction and
     * UserActivitySession API-related events and are not generally by SPI
     * interfaces such as TransactionManager or Current.
     *
     * @parameter int typeOfChange - PRE_BEGIN, POST_BEGIN, PRE_END, or POST_END
     * @return None
     * @exception IllegalStateException thrown if handle management cannot complete
     *                                      successfully
     */
    @Override
    public void contextChange(int typeOfChange, UOWCoordinator coord) {
        // Determine the Tx change type and process
        switch (typeOfChange) {
            case PRE_BEGIN:
                preUserTranBegin();
                break;

            case POST_BEGIN:
                userTranBegin();
                break;

            case PRE_END:
                preUserTranCommit();
                break;

            case POST_END:
                userTranCommit();
                break;
        }
    }

    /**
     * Called in response to the Connection.close() event
     */
    public static void removeHandle(Object connection, HandleListInterface hlFromMCW) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "removeHandle: " + connection + ", " + hlFromMCW);
        }

        if (hlFromMCW != null) {
            hlFromMCW.removeHandle(connection);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "removeHandle");
        }
    }

    /**
     * Signal from Tx service that LTC has ended and UserTran begin() is about to start
     */
    private static void preUserTranBegin() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "preUserTranBegin");

        HandleListInterface hl = getHandleList(getComponentMetaData());

        try {
            if (hl != null) {
                hl.parkHandle();
            }
        } catch (Exception re) {
            FFDCFilter.processException(re, "com.ibm.ejs.j2c.ConnectionHandleManager.preUserTranBegin", "167");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "preUserTranBegin", "unexpected - error manipulating connection handle. See any previous errors related to Managed Connection.");

            // Tx Service will return appropriate application error since handle
            // management has encountered a problem.
            throw new IllegalStateException(re);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "preUserTranBegin");
    }

    /**
     * Signal from Tx service UserTran begin() has been started.(User will get control right after this)
     */
    private static void userTranBegin() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "userTranBegin");

        HandleListInterface hl = getHandleList(getComponentMetaData());

        try {
            if (hl != null) {
                hl.reAssociate();
            }
        } catch (Exception re) {
            FFDCFilter.processException(re, "com.ibm.ejs.j2c.ConnectionHandleManager.userTranBegin", "196");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) //Defect 736528 737787
                Tr.exit(tc, "userTranBegin", "unexpected - error during connection handle re-association. See any previous errors related to Managed Connection.");

            // Tx Service will return appropriate application error since handle
            // management has encountered a problem.
            throw new IllegalStateException(re);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "userTranBegin");
    }

    /**
     * Signal from Tx service that and UserTran commit() is about to start. i.e UserTran
     * commit has been called
     */
    private static void preUserTranCommit() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "preUserTranCommit");

        HandleListInterface hl = getHandleList(getComponentMetaData());

        try {
            if (hl != null) {
                hl.parkHandle();
            }
        } catch (Exception re) {
            FFDCFilter.processException(re, "com.ibm.ejs.j2c.ConnectionHandleManager.preUserTranCommit", "227");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "preUserTranCommit", "unexpected - error while manipulating connection handle. See any previous errors related to Managed Connection.");

            // Tx Service will return appropriate application error since handle
            // management has encountered a problem.
            throw new IllegalStateException(re);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "preUserTranCommit");
    }

    /**
     * Signal from Tx service that UserTran commit has ended.
     */
    private static void userTranCommit() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "userTranCommit");

        HandleListInterface hl = getHandleList(getComponentMetaData());

        try {
            if (hl != null) {
                hl.reAssociate();
            }
        } catch (Exception re) {
            //Absorbing this exception. It will get cleaned up by CM. An Error is Logged by CM
            FFDCFilter.processException(re, "com.ibm.ejs.j2c.ConnectionHandleManager.userTranCommit", "256");

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) //Defect 736528 737787
                Tr.debug(tc, "userTranComimt", "unexpected - error during connection handle re-association. See any previous errors related to Managed Connection.");

            // Tx Service will return appropriate application error since handle
            // management has encountered a problem.
            throw new IllegalStateException(re);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "userTranCommit");
    }

    /**
     * The getHandleList method attempts to get a handlelist from the current thread context.
     * If there is no handle list, null will be returned. Trace entries are made for error
     * conditions.
     */
    private static HandleListInterface getHandleList(ComponentMetaData cmd) {
        HandleListInterface hl = ConnectionHandleAccessorImpl.getConnectionHandleAccessor().getHandleList();
        if (hl == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "handleList is null (informational message) for " + (cmd == null ? null : cmd.getName()));
            // Extra debug trace for when EJBComponentMetaData is neither STATELESS_SESSION nor MESSAGE_DRIVEN
            // is not ported to Liberty because this bundle lacks access to EJB container classes.
        }
        return (hl);
    }

    /**
     * Private method to get ComponentMetaData on Thread
     */
    private static ComponentMetaData getComponentMetaData() {
        ComponentMetaData cmd = null;
        try {
            cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        } catch (Exception e) {
            //ignore
            FFDCFilter.processException(e, "com.ibm.ejs.j2c.ConnectionHandleManager.getComponentMetaData", "319");
        }
        return cmd;
    }
}