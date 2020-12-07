/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;
import com.ibm.ws.rsadapter.FFDCLogger;

/**
 * This class implements the javax.resource.spi.LocalTransaction interface.
 * The interface defines the contract between an application server and
 * resource adapter for local transaction management.
 *
 * <p>The application server gets this object from the ManagedConnection,
 * and starts the local transaction, then commit or rollback the local transaction.
 *
 * @version 1.30
 * @since WAS 5.0
 */
public class LocalTransactionImpl implements LocalTransaction {
    private final static String CLASSNAME = LocalTransactionImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

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
    public LocalTransactionImpl(ManagedConnectionImpl mc, Connection conn) {
        svLogger.entering(CLASSNAME, "<init>", new Object[] { mc, conn });
        ivMC = mc;
        ivConnection = conn;
        ivStateManager = ivMC.stateMgr;
        svLogger.exiting(CLASSNAME, "<init>", this);
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
        svLogger.entering(CLASSNAME, "begin", new Object[] { this, ivMC });
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
            svLogger.info("Cannot start local transaction: " + re.getMessage());
            //138037
            svLogger.exiting(CLASSNAME, "begin", "ResourceException");
            throw re;
        }

        svLogger.info("SpiLocalTransaction started.  ManagedConnection state is " + ivMC.getTransactionStateAsString());
        svLogger.exiting(CLASSNAME, "begin");
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
        svLogger.entering(CLASSNAME, "commit", new Object[] { this, ivMC });
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
            svLogger.exiting(CLASSNAME, "commit", "SQLException");
            throw new ResourceException(se.getMessage());
        }

        if (re != null) {
            svLogger.exiting(CLASSNAME, "commit", "ResourceException");
            throw new ResourceException("Cannnot commit SPI local transaction, " + re.getMessage());
        }

        // Reset so we can deferred enlist in a future global transaction.
        ivMC.isLazyEnlisted = false;
        svLogger.info("SPILocalTransaction committed. ManagedConnection state is " + ivMC.getTransactionStateAsString());
        svLogger.exiting(CLASSNAME, "commit");
    }

    /**
     * Append relevant FFDC information for this class only, formatted as a String.
     *
     * @param info the FFDC logger for reporting information.
     */
    void introspectThisClassOnly(FFDCLogger info) {
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
        svLogger.entering(CLASSNAME, "rollback", new Object[] { this, ivMC });
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
            svLogger.exiting(CLASSNAME, "rollback", "SQLException");
            throw new ResourceException(se.getMessage());
        }

        if (re != null) {
            svLogger.exiting(CLASSNAME, "rollback", "ResourceException");
            throw new ResourceException("Cannot rollback SPI local transaction: " + re.getMessage());
        }

        // Reset so we can deferred enlist in a future global transaction. [d137506]
        ivMC.isLazyEnlisted = false;
        svLogger.info("SpiLocalTransaction rolled back.  ManagedConnection state is " + ivMC.getTransactionStateAsString());
        svLogger.exiting(CLASSNAME, "rollback");
    }
}