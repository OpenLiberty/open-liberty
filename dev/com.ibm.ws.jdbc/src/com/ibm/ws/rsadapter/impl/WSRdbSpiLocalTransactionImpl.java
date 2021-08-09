/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.rsadapter.impl;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference; 

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.LocalTransactionException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig; 
import com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException;

/**
 * This class implements the javax.resource.spi.LocalTransaction interface.
 * The interface defines the contract between an application server and
 * resource adapter for local transaction management.
 * 
 * <p>The application server gets this object from the ManagedConnection,
 * and starts the local transaction, then commit or rollback the local transaction.
 * 
 * @version 1.40
 */
public class WSRdbSpiLocalTransactionImpl implements LocalTransaction, FFDCSelfIntrospectable {
    private static final Class<?> currClass = WSRdbSpiLocalTransactionImpl.class;
    private static final TraceComponent tc = Tr.register(currClass, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    private final java.sql.Connection ivConnection;
    private final WSRdbManagedConnectionImpl ivMC;
    private final WSStateManager ivStateManager;

    /** Data source configuration. */
    private final AtomicReference<DSConfig> dsConfig;

    /**
     * Constructor for WSRdbSpiLocalTransactionImpl
     * 
     * @param WSRdbManagedConnectionImpl mc - the managedConnection to which this SpiLocalTransaction belongs
     * @param java.sql.Connection conn - physical connection to the database
     */

    public WSRdbSpiLocalTransactionImpl(
                                        WSRdbManagedConnectionImpl mc,
                                        java.sql.Connection conn) {

        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", new Object[] { mc, conn });

        ivMC = mc;
        ivConnection = conn;
        ivStateManager = ivMC.stateMgr;
        dsConfig = mc.mcf.dsConfig; 

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>", this);

    }

    /**
     * Begin a local transaction
     * 
     * @exception ResourceException - Possible causes of this exception are:
     *                1) a begin is called but there is already a transaction active for this managedConnection
     *                2) the setAutoCommit failed
     */

    public void begin() throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "begin", ivMC);

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work on the mc
        if (ivMC._mcStale) {
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "MC is stale");
            throw new DataStoreAdapterException("INVALID_CONNECTION", AdapterUtil.staleX(), WSRdbSpiLocalTransactionImpl.class);
        }

        if (dsConfig.get().enableMultithreadedAccessDetection) 
            ivMC.detectMultithreadedAccess(); 

        if (!ivMC.isTransactional()) { // do nothing if no enlistment
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "begin", "no-op.  Enlistment disabled");
            return;
        }

        if (tc.isDebugEnabled()) {
            String cId = null;
            try {
                cId = ivMC.mcf.getCorrelator(ivMC); 
            } catch (SQLException x) {
                // will just log the exception here and ignore it since its in trace
                Tr.debug(
                         tc,
                         "got an exception trying to get the correlator, exception is: ",
                         x);
            }

            if (cId != null) {
                StringBuffer stbuf = new StringBuffer(200);
                stbuf.append("Correlator: DB2, ID: ");
                stbuf.append(cId);
                stbuf.append(" Transaction : ");
                stbuf.append(this);
                stbuf.append(" BEGIN");

                Tr.debug(this, tc, stbuf.toString());
            }
        }

        ResourceException re;

        // Remove synchronization. 

        re = ivStateManager.isValid(WSStateManager.LT_BEGIN);

        if (re == null) {
            //Note the MC handles all notification of connection error event and such on setAutoCommit
            // also, it sets it only when necessary

            try {
                if (ivMC.getAutoCommit())
                    ivMC.setAutoCommit(false);
            } catch (SQLException sqle) {
                FFDCFilter.processException(
                                            sqle,
                                            currClass.getName() + ".begin",
                                            "126",
                                            this);

                throw new DataStoreAdapterException(
                                "DSA_ERROR",
                                sqle,
                                currClass);
            }

            //Note this exception is not caught - This is because
            //  1)  it is of type ResourceException so it can be thrown from the method
            //  2)  if isValid is okay, this should never fail.  If isValid is not okay
            //     an exception is thrown from there
            // We already validated the state in this sync block, so just set it. 
            ivStateManager.transtate = WSStateManager.LOCAL_TRANSACTION_ACTIVE;
        } else 
        {
            // state change was not valid         

            LocalTransactionException local_tran_excep =
                            new LocalTransactionException(re.getMessage());

            DataStoreAdapterException dsae = new DataStoreAdapterException(
                                            "WS_INTERNAL_ERROR",
                                            local_tran_excep,
                                            currClass,
                                            "Cannot start SPI local transaction.",
                                            "",
                                            local_tran_excep.getMessage());

            // Use FFDC to log the possible components list. 
            FFDCFilter.processException(
                                        dsae,
                                        "com.ibm.ws.rsadapter.spi.WSRdbSpiLocalTransactionImpl.begin",
                                        "127",
                                        this,
                                        new Object[] { "Possible components: WebSphere J2C Implementation" });
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "begin", "Exception"); 
            throw dsae;
        }

        if (tc.isEventEnabled())
            Tr.event(
                     tc,
                     "SpiLocalTransaction started.  ManagedConnection state is "
                                     + ivMC.getTransactionStateAsString());

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "begin");
    }

    /**
     * Commit a local transaction
     * 
     * @exception ResourceException - Possible causes for this exception are:
     *                1) if there is no transaction active that can be committed
     *                2) commit was called from an invalid transaction state
     */
    public void commit() throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "commit", ivMC);

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work on the mc
        if (ivMC._mcStale) {
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "MC is stale");
            throw new DataStoreAdapterException("INVALID_CONNECTION", AdapterUtil.staleX(), WSRdbSpiLocalTransactionImpl.class);
        }

        if (dsConfig.get().enableMultithreadedAccessDetection) 
            ivMC.detectMultithreadedAccess(); 

        if (!ivMC.isTransactional()) { // do nothing if no enlistment
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", "no-op.  Enlistment disabled");
            return;
        }

        if (tc.isDebugEnabled()) {
            String cId = null;
            try {
                cId = ivMC.mcf.getCorrelator(ivMC); 
            } catch (SQLException x) {
                // will just log the exception here and ignore it since its in trace
                Tr.debug(
                         tc,
                         "got an exception trying to get the correlator, exception is: ",
                         x);
            }

            if (cId != null) {
                StringBuffer stbuf = new StringBuffer(200);
                stbuf.append("Correlator: DB2, ID: ");
                stbuf.append(cId);
                stbuf.append(" Transaction : ");
                stbuf.append(this);
                stbuf.append(" COMMIT");

                Tr.debug(this, tc, stbuf.toString());
            }
        }

        ResourceException re = ivStateManager.isValid(WSStateManager.LT_COMMIT);

        if (re == null)
            try 
            {
                // If no work was done during the transaction, the autoCommit value may still
                // be on.  In this case, just no-op, since some drivers like ConnectJDBC 3.1
                // don't allow commit/rollback when autoCommit is on.  

                //  here the autocommit is always false, so we can call commit.
                ivConnection.commit();

                //Note this exception is not caught - This is because
                //  1)  it is of type ResourceException so it can be thrown from the method
                //  2)  if isValid is okay, this should never fail.  If isValid is not okay
                //     an exception is thrown from there
                // Already validated the state in this sync block, so just set it. 
                ivStateManager.transtate = WSStateManager.NO_TRANSACTION_ACTIVE;
            } 
            catch (SQLException se) {
                FFDCFilter.processException(
                                            se,
                                            "com.ibm.ws.rsadapter.spi.WSRdbSpiLocalTransactionImpl.commit",
                                            "139",
                                            this);
                ResourceException x = AdapterUtil.translateSQLException(se, ivMC, true, currClass);
                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, "commit", "Exception");
                throw x;
            }
        else 
        {
            // state change was not valid         

            LocalTransactionException local_tran_excep =
                            new LocalTransactionException(re.getMessage());
            DataStoreAdapterException ds = new DataStoreAdapterException(
                                            "WS_INTERNAL_ERROR",
                                            local_tran_excep,
                                            currClass,
                                            "Cannot commit SPI local transaction.",
                                            "",
                                            local_tran_excep.getMessage());
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", "Exception");
            throw ds;
        }

        // Reset so we can deferred enlist in a future global transaction. 
        ivMC.wasLazilyEnlistedInGlobalTran = false; 

        if (tc.isEventEnabled())
            Tr.event(
                     tc,
                     "SPILocalTransaction committed. ManagedConnection state is "
                                     + ivMC.getTransactionStateAsString());

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "commit");
    }

    /**
     * @return relevant FFDC information for this class, formatted as a String array.
     */
    public final String[] introspectSelf() {
        // Delegate to the ManagedConnection to get all relevant information.

        return ivMC.introspectSelf();
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
    public void rollback() throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "rollback", ivMC);

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work on the mc
        if (ivMC._mcStale) {
            if (tc.isDebugEnabled())
                Tr.debug(this, tc, "MC is stale");
            throw new DataStoreAdapterException("INVALID_CONNECTION", AdapterUtil.staleX(), WSRdbSpiLocalTransactionImpl.class);
        }

        if (!ivMC.isTransactional()) { // do nothing if no enlistment
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", "no-op.  Enlistment disabled");
            return;
        }

        ResourceException re;

        if (tc.isDebugEnabled()) {
            String cId = null;
            try {
                cId = ivMC.mcf.getCorrelator(ivMC); 
            } catch (SQLException x) {
                // will just log the exception here and ignore it since its in trace
                Tr.debug(
                         tc,
                         "got an exception trying to get the correlator, exception is: ",
                         x);
            }

            if (cId != null) {
                StringBuffer stbuf = new StringBuffer(200);
                stbuf.append("Correlator: DB2, ID: ");
                stbuf.append(cId);
                stbuf.append(" Transaction : ");
                stbuf.append(this);
                stbuf.append("ROLLBACK");

                Tr.debug(this, tc, stbuf.toString());
            }
        }


        re = ivStateManager.isValid(WSStateManager.LT_ROLLBACK); 

        if (re == null)
            try 
            {
                // If no work was done during the transaction, the autoCommit value may still
                // be on.  In this case, just no-op, since some drivers like ConnectJDBC 3.1
                // don't allow commit/rollback when autoCommit is on.  

                //  here the autocommit is always false, so we can call commit.
                ivConnection.rollback();

                //Note this exception is not caught - This is because
                //  1)  it is of type ResourceException so it can be thrown from the method
                //  2)  if isValid is okay, this should never fail.  If isValid is not okay
                //     an exception is thrown from there
                // Already validated the state in this sync block, so just set it. 
                ivStateManager.transtate = WSStateManager.NO_TRANSACTION_ACTIVE;
            } 
            catch (SQLException se) {
                if (!ivMC.isAborted())
                    FFDCFilter.processException(se, getClass().getName(), "192", this);

                ResourceException resX = AdapterUtil.translateSQLException(se, ivMC, true, currClass);
                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, "rollback", "Exception"); 
                throw resX;
            }
        else 
        {
            // state change was not valid

            LocalTransactionException local_tran_excep =
                            new LocalTransactionException(re.getMessage());

            DataStoreAdapterException dsae = new DataStoreAdapterException(
                                            "WS_INTERNAL_ERROR",
                                            local_tran_excep,
                                            currClass,
                                            "Cannot rollback SPI local transaction.",
                                            "",
                                            local_tran_excep.getMessage());
            // Use FFDC to log the possible components list. 
            FFDCFilter.processException(
                                        dsae,
                                        "com.ibm.ws.rsadapter.spi.WSRdbSpiLocalTransactionImpl.rollback",
                                        "291",
                                        this,
                                        new Object[] { " Possible components: WebSphere J2C Implementation" });
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", "Exception"); 
            throw dsae;
        }

        // Reset so we can deferred enlist in a future global transaction. 
        ivMC.wasLazilyEnlistedInGlobalTran = false; 

        if (tc.isEventEnabled())
            Tr.event(
                     tc,
                     "SpiLocalTransaction rolled back.  ManagedConnection state is "
                                     + ivMC.getTransactionStateAsString());

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "rollback");
    }

}