/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.spi;

import java.sql.SQLException;

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;

import com.ibm.adapter.AdapterUtil;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * This class implements the javax.resource.spi.LocalTransaction interface.
 * The interface defines the contract between an application server and
 * resource adapter for local transaction management.
 *
 * <p>The application server gets this object from the ManagedConnection,
 * and starts the local transaction, then commit or rollback the local transaction.
 */
public class LocalTransactionImpl implements LocalTransaction {

    private static final Class currClass = LocalTransactionImpl.class;
    private static final TraceComponent tc = Tr.register(currClass);

    /** Native connection */
    private final java.sql.Connection ivConnection;

    /** Managed connection */
    private final ManagedConnectionImpl ivMC;

    /** State manager */
    private final StateManager ivStateManager;

    /** Previous autocommit before starting local transaction */
    private boolean prevAutoCommit = false;

    /**
     * Constructor for LocalTransactionImpl
     *
     * @param WSRdbManagedConnectionImpl mc - the managedConnection to which this SpiLocalTransaction belongs
     * @param java.sql.Connection conn - physical connection to the database
     */

    public LocalTransactionImpl(
                                ManagedConnectionImpl mc,
                                java.sql.Connection conn) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { mc, conn });

        ivMC = mc;
        ivConnection = conn;
        ivStateManager = ivMC.stateMgr;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);

    }

    /**
     * Begin a local transaction
     *
     * @exception ResourceException - Possible causes of this exception are:
     *                1) a begin is called but there is already a transaction active for this managedConnection
     *                2) the setAutoCommit failed
     */

    @Override
    public void begin() throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "begin", new Object[] { this, ivMC });

        ResourceException re;

        synchronized (ivMC) {
            re = ivStateManager.isValid(StateManager.LT_BEGIN);
            if (re == null) {
                try {
                    prevAutoCommit = ivMC.getAutoCommit();
                    ivMC.setAutoCommit(false);
                } catch (SQLException sqle) {
                    throw new ResourceException(sqle.getMessage());
                }

                ivStateManager.transtate = StateManager.LOCAL_TRANSACTION_ACTIVE;
            }
        }

        if (re != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Cannot start local transaction: " + re.getMessage());
            if (tc.isEntryEnabled()) //138037
                Tr.exit(tc, "begin", "Exception"); //138037
            throw re;
        }

        if (tc.isEventEnabled())
            Tr.event(
                     tc,
                     "SpiLocalTransaction started.  ManagedConnection state is "
                         + ivMC.getTransactionStateAsString());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "begin");
    }

    /**
     * Commit a local transaction
     *
     * @exception ResourceException - Possible causes for this exception are:
     *                1) if there is no transaction active that can be committed
     *                2) commit was called from an invalid transaction state
     */
    @Override
    public void commit() throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commit", new Object[] { this, ivMC });

        ResourceException re = null;

        try {
            synchronized (ivMC) {
                re = ivStateManager.isValid(StateManager.LT_COMMIT);

                if (re == null) {
                    ivConnection.commit();

                    ivMC.setAutoCommit(prevAutoCommit);

                    ivStateManager.transtate = StateManager.NO_TRANSACTION_ACTIVE;
                }
            }
        } catch (SQLException se) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", "Exception");
            throw new ResourceException(se.getMessage());
        }

        if (re != null) {

            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", "Exception");
            throw new ResourceException("Cannnot commit SPI local transaction, " + re.getMessage());
        }

        // Reset so we can deferred enlist in a future global transaction.

        // LIDB????
        ivMC.isLazyEnlisted = false;

        //if (ivMC.isDynamicEnlistment()) {
        //	ivMC.ivAlreadyProcessedInteractionPendingEvent = false;
        //}

        if (tc.isEventEnabled())
            Tr.event(
                     tc,
                     "SPILocalTransaction committed. ManagedConnection state is "
                         + ivMC.getTransactionStateAsString());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commit");
    }

    /**
     * Append relevant FFDC information for this class only, formatted as a String.
     *
     * @param info the FFDC logger for reporting information.
     */
    void introspectThisClassOnly(com.ibm.ws.rsadapter.FFDCLogger info) {
        // ManagedConnection already reports all of these objects, so we only need to display
        // the hashcode, for verifying we have the right reference in this class.

        info.createFFDCHeader(this);
        info.append("Connection:", AdapterUtil.toString(ivConnection));
        info.append("ManagedConnection:", ivMC);
    }

    /**
     * Rollback a local transaction
     *
     * @exception ResourceException - Possible causes for this exception are:
     *                1) if there is no transaction active that can be rolledback
     *                2) rollback was called from an invalid transaction state
     */
    @Override
    public void rollback() throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "rollback", new Object[] { this, ivMC });

        ResourceException re;

        try {
            synchronized (ivMC) {
                re = ivStateManager.isValid(StateManager.LT_ROLLBACK);

                if (re == null) {
                    ivConnection.rollback();
                    ivMC.setAutoCommit(prevAutoCommit);
                    ivStateManager.transtate = StateManager.NO_TRANSACTION_ACTIVE;
                }
            }
        } catch (SQLException se) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", "Exception");
            throw new ResourceException(se.getMessage());
        }

        if (re != null) {

            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", "Exception");
            throw new ResourceException("Cannot rollback SPI local transaction: " + re.getMessage());
        }

        // Reset so we can deferred enlist in a future global transaction. [d137506]

        // LIDB????
        ivMC.isLazyEnlisted = false;

        // if (ivMC.isDynamicEnlistment()) {
        //	ivMC.ivAlreadyProcessedInteractionPendingEvent = false;
        //}

        if (tc.isEventEnabled())
            Tr.event(
                     tc,
                     "SpiLocalTransaction rolled back.  ManagedConnection state is "
                         + ivMC.getTransactionStateAsString());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "rollback");
    }

}
