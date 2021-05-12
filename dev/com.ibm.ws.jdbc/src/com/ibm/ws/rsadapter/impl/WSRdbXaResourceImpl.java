/*******************************************************************************
 * Copyright (c) 1997, 2021 IBM Corporation and others.
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

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.SecurityException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.jca.adapter.WSXAResource;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig; 
import com.ibm.ws.rsadapter.exceptions.TransactionException;

/**
 * This class implements the javax.transaction.xa.XAResource interface.
 * It is also a wrapper of the JDBC driver provided XAResource.
 * The transaction manager uses the interface to communicate transaction association,
 * completion, and recovery to the resource manager.
 * There is only one XAResource instance associates with WSRdbManagedConnectionImpl instance.
 */

public class WSRdbXaResourceImpl implements WSXAResource, FFDCSelfIntrospectable 
{
    //internal variables
    private XAResource ivXaRes; //  Unit tests might override to cause in-doubt transactions
    private final WSRdbManagedConnectionImpl ivManagedConnection;
    private final WSStateManager ivStateManager;

    //This xid is kept because there is a case when end and rollback may be called without an Xid.  This case
    //  is when ManagedConnection.cleanup is called and there is still a connection in a transaction open.
    Xid ivXid;

    /** Data source configuration. */
    private final AtomicReference<DSConfig> dsConfig;

    private static final Class<?> currClass = WSRdbXaResourceImpl.class; 
    private static final TraceComponent tc = Tr.register(currClass, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * Constructor
     * 
     * @param xaRes The XAResource object obtained from the java.sql.Connection
     * @param mc The ManagedConnection instance the sqlConn and XAResource belongs to.
     */
    public WSRdbXaResourceImpl(XAResource xaRes, WSRdbManagedConnectionImpl mc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "<init>", new Object[] { xaRes, mc });

        ivXaRes = xaRes;
        ivManagedConnection = mc;
        ivStateManager = mc.stateMgr;
        dsConfig = mc.mcf.dsConfig; 

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "<init>", this);
    }

    /**
     * Commit the global transaction specified by xid.
     * <p>If the resource manager did not commit the transaction and the paramether onePhase is set to true, the
     * resource manager may throw one of the XA_RB* exceptions. Upon return, the resource manager has rolled
     * back the branch's work and has released all held resources.
     * 
     * @param - Xid xid - a global transaction identifier
     * @param - boolean onePhase - If true, the resource manager should use a one-phase commit protocol to commit the work done on behalf of
     *        xid.
     * @exception XAException - An error has occurred. Possible XAExceptions are XA_HEURHAZ, XA_HEURCOM, XA_HEURRB,
     *                XA_HEURMIX, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     *                Possible causes for this exception are:
     *                1) the commit call on the actual XAResource failed
     *                2) the commit was called while in an invalid transaction state
     */

    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "commit", new Object[]
            {
             ivManagedConnection,
             AdapterUtil.toString(xid), 
             onePhase ? "ONE PHASE" : "TWO PHASE"
            });

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work to continue.  In this case, we throw XAER_RMFAIL xa error
        // which indicates that the resource manager is not available
        if (ivManagedConnection._mcStale) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "MC is stale throwing XAER_RMFAIL", ivManagedConnection);
            Tr.error(tc, "INVALID_CONNECTION");
            throw new XAException(XAException.XAER_RMFAIL);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
        {
            String cId = null;
            try {
                cId = ivManagedConnection.mcf.getCorrelator(ivManagedConnection); 
            } catch (SQLException x) {
                // will just log the exception here and ignore it since its in trace
                Tr.debug(this, tc, "got an exception trying to get the correlator in commit, exception is: ", x);
            }

            if (cId != null) {
                StringBuffer stbuf = new StringBuffer(200);
                stbuf.append("Correlator: DB2, ID: ");
                stbuf.append(cId);
                if (xid != null) {
                    stbuf.append("Transaction ID : ");
                    stbuf.append(xid);
                }
                stbuf.append("COMMIT");
                Tr.debug(this, tc, stbuf.toString());
            }
        }

        if (dsConfig.get().enableMultithreadedAccessDetection) 
            ivManagedConnection.detectMultithreadedAccess(); 

        // Reset so we can deferred enlist in a future global transaction. 
        ivManagedConnection.wasLazilyEnlistedInGlobalTran = false; 

        if (ivXid == null) { 
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "XAResource.start was never issued; allowing commit for recovery.");
            try {

                ivStateManager.setState(WSStateManager.XA_RECOVER);
            } catch (TransactionException te) {
                //Exception means setState failed because it was invalid to set the state in this case
                FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.commit", "120", this);

                XAException xae = AdapterUtil.createXAException(
                                                                "INVALID_TX_STATE",
                                                                new Object[] { "XAResource.commit", ivManagedConnection.getTransactionStateAsString() },
                                                                XAException.XAER_PROTO); 

                traceXAException(xae, currClass);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(this, tc, "commit", xae);
                throw xae;
            }
            ivXid = xid;
        } else if (!xid.equals(ivXid)) { 
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "Xid does not match.", new Object[]
                {
                 "XAResource.start:  ",
                 AdapterUtil.toString(ivXid), 
                 "XAResource.commit: ",
                 AdapterUtil.toString(xid) 
                         });

            XAException xaX = AdapterUtil.createXAException(
                                                            "XID_MISMATCH",
                                                            new Object[] { AdapterUtil.toString(ivXid), "commit", AdapterUtil.toString(xid) },
                                                            XAException.XAER_NOTA); 

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", xaX);
            throw xaX;
        }

        // Defer two-phase xa.commit if a stale connection occurred and the data source property
        // oracleRACXARetryDelay is configured.

        WSManagedConnectionFactoryImpl mcf = ivManagedConnection.mcf;
        long oracleRACLastStale = mcf.oracleRACLastStale.get();

        if (!onePhase && oracleRACLastStale > 0l) {
            // Determine if the current time falls within the delay period from the latest
            // stale connection. If so, we need to defer commit/rollback.

            long timeSinceLastStale = System.currentTimeMillis() - oracleRACLastStale;

            if (timeSinceLastStale > mcf.oracleRACXARetryDelay // Current time exceeds the delay period 
                && mcf.oracleRACLastStale.compareAndSet(oracleRACLastStale, 0l)) // Reset indicator as non-stale
            {
                if (tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "Time since last stale, " + timeSinceLastStale +
                                             " ms, exceeds the oracleRACXARetryDelay. Allowing commit.");
            } else {
                // Current time falls with the delay period.
                // Defer the two-phase xa.commit operation by raising an XAException with
                // error code of XA_RETRY. The transaction manager will retry the operation
                // after a configurable delay.

                if (tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "Time since last stale, " + timeSinceLastStale +
                                             " ms, falls within the oracleRACXARetryDelay. Deferring commit.");

                XAException x = AdapterUtil.createXAException(
                                                              "ORACLE_RAC_RETRY",
                                                              new Object[] { "XAResource.commit", timeSinceLastStale, mcf.oracleRACXARetryDelay },
                                                              XAException.XA_RETRY);

                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, "commit", x);
                throw x;
            }
        }

        ivXid = null;

        try {
            ivXaRes.commit(xid, onePhase);
            ivStateManager.setState(WSStateManager.XA_COMMIT);
        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.commit", "113", this);

            XAException xae = AdapterUtil.createXAException(
                                                            "INVALID_TX_STATE",
                                                            new Object[] { "XAResource.commit", ivManagedConnection.getTransactionStateAsString() },
                                                            XAException.XAER_PROTO); 

            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", xae);
            throw xae;
        } catch (XAException xae) {
            if (ivStateManager.getState() == WSStateManager.RECOVERY_IN_PROGRESS
                && xae.errorCode == XAException.XAER_NOTA) {
                // XA recovery gets this exception because this XA resource previously
                // committed successfully.  Just print the details to debug trace instead
                // of logging warnings.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "XA resource got XAER_NOTA (" + XAException.XAER_NOTA + ") during recovery. This happens when the XA resource previously committed successfully.",
                             ivManagedConnection.helper.getXAExceptionContents(xae));
            } else {
                FFDCFilter.processException(xae, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.commit", "126", this);
                traceXAException(xae, currClass);
            }
            checkXAException(xae); 
            if (xae.errorCode == XAException.XA_HEURCOM ||
                xae.errorCode == XAException.XA_HEURHAZ ||
                xae.errorCode == XAException.XA_HEURMIX ||
                xae.errorCode == XAException.XA_HEURRB) {

                try {
                    // we won't call forget() here, instead, we will forget the tran only when the TM manager calls it on us.
                    // this way if the server crashes after the adapter receives heuristic and before the TM logging it. TM could still
                    // as the database for the in doubt trans since its not forgotten yet

                    ivStateManager.setState(WSStateManager.HEURISTIC_END);
                } catch (TransactionException te) {
                    FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.commit", "142", this);
                    Tr.warning(tc, "DSA_INTERNAL_WARNING", new Object[] { "Exception setting the transaction state to WSStateManager.HEURISTIC_END from ",
                                                                         ivManagedConnection.getTransactionStateAsString(), te });
                    //should never happen so just eat it
                }
            } 
              // In DB2, xaNetworkOptimization allows xa.end to be cached until commit time.
              // When xaNetworkOptimization is enabled and a deadlock occurs, xa.commit will end
              // up failing with XAER_NOTA with a chained XA_RBDEADLOCK. Update the state manager
              // to indicate no transaction is active (because it was rolled back on account of 
              // the deadlock). Otherwise, we would end up with a MC_CLEANUP_ERROR warning when
              // the managed connection is cleaned up.
            else if (xae.errorCode == XAException.XAER_NOTA
                     && xae.getCause() instanceof XAException
                     && ((XAException) xae.getCause()).errorCode == XAException.XA_RBDEADLOCK)
                try {
                    ivStateManager.setState(WSStateManager.XA_END_FAIL);
                } catch (TransactionException tranX) {
                    FFDCFilter.processException(tranX, getClass().getName() + ".commit", "341", this);

                    XAException x = AdapterUtil.createXAException(
                                                                  "INVALID_TX_STATE",
                                                                  new Object[] { "XAResource.commit", ivManagedConnection.getTransactionStateAsString() },
                                                                  XAException.XAER_PROTO);

                    traceXAException(x, currClass);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(this, tc, "commit", x);
                    throw x;
                }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", xae);
            throw xae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
        {
            String cId = null;
            try {
                cId = ivManagedConnection.mcf.getCorrelator(ivManagedConnection); 
            } catch (SQLException x) {
                // will just log the exception here and ignore it since its in trace
                Tr.debug(this, tc, "got an exception trying to get the correlator in commit, exception is: ", x);
            }

            if (cId != null) {
                StringBuffer stbuf = new StringBuffer(200);
                stbuf.append("Correlator: DB2, ID: ");
                stbuf.append(cId);
                if (xid != null) {
                    stbuf.append("Transaction ID : ");
                    stbuf.append(xid);
                }
                stbuf.append("COMMIT");
                Tr.debug(this, tc, stbuf.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "commit");
    }

    /**
     * Only used by ManagedConnection.cleanup when it needs to close a connection that is
     * still in an active global transaction.
     * 
     * @exception XAException
     */
    protected final void end() throws XAException {
        end(ivXid, XAResource.TMFAIL); // Use the xid from XAResource.start 
    }

    /**
     * Ends the work performed on behalf of a transaction branch. The resource manager
     * disassociates the XA resource from the transaction branch specified and lets the
     * transaction be completed.
     * 
     * <p>If TMSUSPEND is specified in flags, the transaction branch is temporarily suspended
     * in incomplete state. The transaction context is in suspened state and must be resumed
     * via start with TMRESUME specified.
     * 
     * <p>If TMFAIL is specified, the portion of work has failed. The resource manager may mark
     * the transaction as rollback-only
     * 
     * <p>If the end method is called with the TMFAIL flag, the wrappered XAResource is allowed
     * (according to the JTA specification) to throw an XAException with any of the XA_RB* rollback
     * error Codes as part of its normal processing. This does not constitute an error but
     * indicates merely that the RM has marked its branch rollback_only as a result of the TMFAIL.
     * 
     * The exception should not be rethrown and should certainly not result in FFDC because
     * the TM will always call rollback after it has called end(TMFAIL).
     * 
     * <p>If TMSUCCESS is specified, the portion of work has completed successfully.
     * 
     * @param xid Global transaction identifier that must be the same as was used to start the transaction.
     * @param flags One of TMSUCCESS, TMFAIL, or TMSUSPEND
     * @exception XAException
     *                An error has occurred. Possible XAException values are XAER_RMERR, XAER_RMFAILED,
     *                XAER_NOTA, XAER_INVAL, XAER_PROTO, or XA_RB*.
     *                Possible causes for this exception are:
     *                <ol>
     *                <li>end call on physical XAResource failed
     *                <li>end was called in an illegal transaction state
     *                </ol>
     */

    public void end(Xid xid, int flags) throws XAException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "end", new Object[]
            {
             ivManagedConnection,
             AdapterUtil.toString(xid), 
             AdapterUtil.getXAResourceEndFlagString(flags)
            });

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work to continue.  In this case, we throw XAER_RMFAIL xa error
        // which indicates that the resource manager is not available
        if (ivManagedConnection._mcStale) {
            XAException x = new XAException(XAException.XAER_RMFAIL);;
            Tr.error(tc, "INVALID_CONNECTION");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "end", new Object[] { "MC is stale throwing XAER_RMFAIL", ivManagedConnection });
            throw x;
        }

        if (!xid.equals(ivXid)) { 
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "Xid does not match.", new Object[]
                {
                 "XAResource.start: ",
                 AdapterUtil.toString(ivXid), 
                 "XAResource.end:   ",
                 AdapterUtil.toString(xid) 
                         });

            XAException xaX = AdapterUtil.createXAException(
                                                            "XID_MISMATCH",
                                                            new Object[] { AdapterUtil.toString(ivXid), "end", AdapterUtil.toString(xid) },
                                                            XAException.XAER_NOTA); 

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "end", xaX);
            throw xaX;
        }

        try {
            ivStateManager.setState(WSStateManager.XA_END);
            ivXaRes.end(xid, flags);
            if (ivManagedConnection.helper.xaEndResetsAutoCommit)
                ivManagedConnection.refreshCachedAutoCommit();
        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.end", "228", this);
            Tr.error(tc, "INVALID_TX_STATE", new Object[] { "XAResource.end()", ivManagedConnection.getTransactionStateAsString() });
            try {
                ivXaRes.rollback(xid);
            } catch (XAException eatXA) {
                FFDCFilter.processException(eatXA, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.end", "236", this);
                traceXAException(eatXA, currClass);
                //eat the exception because we will throw the next one
            }

            XAException xae = AdapterUtil.createXAException(
                                                            "INVALID_TX_STATE",
                                                            new Object[] { "XAResource.end", ivManagedConnection.getTransactionStateAsString() },
                                                            XAException.XA_RBROLLBACK); 

            traceXAException(xae, currClass);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            {
                String cId = null;
                try {
                    cId = ivManagedConnection.mcf.getCorrelator(ivManagedConnection); 
                } catch (SQLException x) {
                    // will just log the exception here and ignore it since its in trace
                    Tr.debug(this, tc, "got an exception trying to get the correlator in rollback during xa end fails, exception is: ", x);
                }

                if (cId != null) {
                    StringBuffer stbuf = new StringBuffer(200);
                    stbuf.append("Correlator: DB2, ID: ");
                    stbuf.append(cId);
                    stbuf.append("Transaction ID : ");
                    stbuf.append(xid);
                    stbuf.append("ROLLBACK");
                    Tr.debug(this, tc, stbuf.toString());
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "end", "Exception");
            throw xae;
        } catch (XAException xae) {
            FFDCFilter.processException(xae, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.end", "438", this);
            checkXAException(xae); 
            // - deferred setting of state because this is not a normal case
            try {
                ivStateManager.setState(WSStateManager.XA_END_FAIL);
            } catch (TransactionException te1) {
                Tr.warning(tc, "DSA_INTERNAL_ERROR", new Object[] { "Error setting the state to XA_END_FAIL from ",
                                                                   ivManagedConnection.getTransactionStateAsString(), te1 });
            }
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "end", xae); 
            throw xae; // @HMP ,moved the throw here to avoid throwing if TMFAIL
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "end");
    }

    /**
     * Tell the resource manager to forget about a heuristically completed transaction branch.
     * 
     * @param Xid xid - A global transaction identifier
     * @exception XAException - An error has occurred. Possible exception values are XAER_RMERR, XAER_RMFAIL,
     *                XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     *                Possible causes for this exception are:
     *                1) the forget call on the physical XAResource failed
     *                2) forget was called in an invalid transaction state
     */

    public void forget(Xid xid) throws XAException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "forget", new Object[]
            {
             ivManagedConnection,
             AdapterUtil.toString(xid) 
                     });

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work to continue.  In this case, we throw XAER_RMFAIL xa error
        // which indicates that the resource manager is not available
        if (ivManagedConnection._mcStale) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "MC is stale throwing XAER_RMFAIL", ivManagedConnection);
            Tr.error(tc, "INVALID_CONNECTION");
            throw new XAException(XAException.XAER_RMFAIL);
        }

        if (ivXid == null) { 
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "XAResource.start was never issued; allowing to forget for recovery.");
        }

        // For XAResource.forget, only trace an Xid mismatch. 
        if (TraceComponent.isAnyTracingEnabled() && !xid.equals(ivXid) && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "Xid does not match.", new Object[]
            {
             "XAResource.start:  ",
             AdapterUtil.toString(ivXid), 
             "XAResource.forget: ",
             AdapterUtil.toString(xid) 
                     });

        try {
            // since we are not forgetting the tran when we get heuristic exception in commit/rollback, we will really pass forget
            // to the database here.
            ivXaRes.forget(xid);

            ivStateManager.setState(WSStateManager.XA_FORGET);
        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.forget", "284", this);
            Tr.error(tc, "INVALID_TX_STATE", new Object[] { "XAResource.forget()", ivManagedConnection.getTransactionStateAsString() });
            traceXAException(new XAException(XAException.XA_RBPROTO), currClass);
        }
        catch (XAException xaE) {
            FFDCFilter.processException(xaE, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.forget", "489", this);
            traceXAException(xaE, currClass);
            checkXAException(xaE); 
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "forget", xaE);
            throw xaE;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "forget");
    }

    /**
     * Returns the managed connection that created this XA resource.
     * 
     * @return the managed connection that created this XA resource.
     */
    public final ManagedConnection getManagedConnection() {
        return ivManagedConnection;
    }

    /**
     * Obtain the current transaction timeout value set for this XAResource instance. If XAResource.setTransactionTimeout was
     * not use prior to invoking this method, the return value is the default timeout set for the resource manager; otherwise, the value
     * used in the previous setTransactionTimeout call is returned.
     * 
     * @return int - the transaction timeout value in seconds.
     * @exception XAException - An error has occurred. Possible exception values are XAER_RMERR, XAER_RMFAIL.
     *                Possible causes for this exception are:
     *                1) getTransactionTimeout on the physical XAResource failed
     */

    public final int getTransactionTimeout() throws XAException {

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work to continue.  In this case, we throw XAER_RMFAIL xa error
        // which indicates that the resource manager is not available
        if (ivManagedConnection._mcStale) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "MC is stale throwing XAER_RMFAIL", ivManagedConnection);
            Tr.error(tc, "INVALID_CONNECTION");
            throw new XAException(XAException.XAER_RMFAIL);
        }

        return ivXaRes.getTransactionTimeout();
    }

    /**
     * @return relevant FFDC information for this class, formatted as a String array.
     */
    public final String[] introspectSelf() {
        // Delegate to the ManagedConnection to get all relevant information.

        return ivManagedConnection.introspectSelf();
    }

    /**
     * Append relevant FFDC information for this class only, formatted as a String.
     * 
     * @param info the FFDC logger for reporting information.
     */
    void introspectThisClassOnly(com.ibm.ws.rsadapter.FFDCLogger info) {
        info.createFFDCHeader(this);

        info.append("ManagedConnection:", ivManagedConnection);
        info.append("Underlying XAResource Object: " + AdapterUtil.toString(ivXaRes), ivXaRes);
        info.append("Xid: ", AdapterUtil.toString(ivXid)); 
    }

    /**
     * This method is called to determine if the resource manager instance represented by the
     * target object is the same as the resouce manager instance represented by the parameter
     * xares.
     * 
     * @param xaRes An XAResource object whose resource manager instance is to be compared with the
     *            resource manager instance of the target object.
     * @return true if it's the same RM instance; otherwise false.
     * @exception XAException
     *                Possible exception values are XAER_RMERR, XAER_RMFAIL.
     *                Possible causes for this exception are:
     *                1) isSameRM call on physical XAResource failed
     */
    public final boolean isSameRM(XAResource xaRes) throws XAException {
        boolean isSame = false;
        WSRdbXaResourceImpl inputXA;
        try {
            inputXA = (WSRdbXaResourceImpl) xaRes;
        } catch (ClassCastException ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "The Input XAResource is not the same type as the current XAResource", xaRes);
            // eat the exception and return false..
            return isSame;
        }
        isSame = ivXaRes.isSameRM(inputXA.ivXaRes);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "isSameRM?", new Object[] { xaRes, isSame ? Boolean.TRUE : Boolean.FALSE }); 

        return isSame;
    }

    /**
     * Ask the resource manager to prepare for a transaction commit of the transaction specified in xid.
     * 
     * @param Xid xid - A global transaction identifier
     * @return int -A value indicating the resource manager's vote on the outcome of the transaction. The possible values are:
     *         XA_RDONLY or XA_OK. If the resource manager wants to roll back the transaction, it should do so by raising an
     *         appropriate XAException in the prepare method.
     * @exception XAException - An error has occurred. Possible exception values are: XA_RB*, XAER_RMERR, XAER_RMFAIL,
     *                XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     *                Possible causes for this exception are:
     *                1) prepare call on physical XAResource failed
     */

    public int prepare(Xid xid) throws XAException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "prepare", new Object[]
            {
             ivManagedConnection,
             AdapterUtil.toString(xid) 
                     });

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work to continue.  In this case, we throw XAER_RMFAIL xa error
        // which indicates that the resource manager is not available
        if (ivManagedConnection._mcStale) {
            Tr.error(tc, "INVALID_CONNECTION");
            XAException x = new XAException(XAException.XAER_RMFAIL);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "prepare", new Object[] { "MC is stale throwing XAER_RMFAIL", ivManagedConnection });
            throw x;
        }

        if (!xid.equals(ivXid)) { 
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "Xid does not match.", new Object[]
                {
                 "XAResource.start:   ",
                 AdapterUtil.toString(ivXid), 
                 "XAResource.prepare: ",
                 AdapterUtil.toString(xid) 
                         });

            XAException xaX = AdapterUtil.createXAException(
                                                            "XID_MISMATCH",
                                                            new Object[] { AdapterUtil.toString(ivXid), "prepare", AdapterUtil.toString(xid) },
                                                            XAException.XAER_NOTA); 

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepare", xaX);
            throw xaX;
        }

        int returnValue;

        try {
            returnValue = ivXaRes.prepare(xid);
            //need to check the return value here and determine if we need to clean up the transaction state
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            {
                Tr.debug(this, tc, "xa.prepare() status:", new Object[]
                {
                 AdapterUtil.getXAResourceVoteString(returnValue),
                 AdapterUtil.toString(xid) 
                         });
            }

            if (returnValue == XAException.XA_RDONLY) {
                try {
                    //  flag is reset since no commit/rollbak is issued here
                    // adn we don't want inGlobalTransaction to return the wrong value
                    ivManagedConnection.wasLazilyEnlistedInGlobalTran = false;

                    ivStateManager.setState(WSStateManager.XA_READONLY);
                } catch (TransactionException te) {
                    //Exception means setState failed because it was invalid to set the state in this case
                    FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.prepare", "373", this);

                    XAException xae = AdapterUtil.createXAException(
                                                                    "INVALID_TX_STATE",
                                                                    new Object[] { "XAResource.prepare", ivManagedConnection.getTransactionStateAsString() },
                                                                    XAException.XA_RBPROTO); 

                    traceXAException(xae, currClass);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(this, tc, "prepare", xae); 
                    throw xae;
                }
            }

        } catch (XAException xae) {
            FFDCFilter.processException(xae, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.prepare", "386", this);
            traceXAException(xae, currClass);
            checkXAException(xae); 
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "prepare", "Exception");
            throw xae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "prepare", returnValue);
        return returnValue;
    }

    /**
     * Obtain a list of prepared transaction branches from a resource manager. The transaction manager calls this method during
     * recovery to obtain the list of transaction branches that are currently in prepared or heuristically completed states.
     * 
     * @param int flag - One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS must be used when no other flags
     *        are set in flags.
     * @return Xid[] - The resource manager returns zero or more XIDs for the transaction branches that are currently in a prepared or
     *         heuristically completed state. If an error occurs during the operation, the resource manager should throw the appropriate
     *         XAException.
     * @exception XAException - An error has occurred. Possible values are XAER_RMERR, XAER_RMFAIL, XAER_INVAL, and
     *                XAER_PROTO.
     */

    public Xid[] recover(int flag) throws XAException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "recover", ivManagedConnection, AdapterUtil.getXAResourceRecoverFlagString(flag));
        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work to continue.  In this case, we throw XAER_RMFAIL xa error
        // which indicates that the resource manager is not available
        if (ivManagedConnection._mcStale) {
            Tr.error(tc, "INVALID_CONNECTION");
            XAException x = new XAException(XAException.XAER_RMFAIL);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "recover", new Object[] { "MC is stale throwing XAER_RMFAIL", ivManagedConnection });
            throw x;
        }

        Xid[] xids = null;

        try {
            xids = ivXaRes.recover(flag);
            if (xids == null || xids.length == 0) 
            { 
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) 
                    Tr.event(this, tc, "No oustanding transactions to recover.  Transaction state does not change.");
            } else {
                ivStateManager.setState(WSStateManager.XA_RECOVER);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) 
                    Tr.event(this, tc, "Outstanding transactions to recover.  Transaction state is changing to " +
                                       ivManagedConnection.getTransactionStateAsString());
            }
        } catch (TransactionException te) {
            FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.recover", "438", this);
            Tr.warning(tc, "DSA_INTERNAL_WARNING", new Object[] { "Exception setting the transaction state to WSStateManager.XA_RECOVER from ",
                                                                 ivManagedConnection.getTransactionStateAsString(), te });
            //should never happen so just eat it
        } catch (XAException xae) {
            FFDCFilter.processException(xae, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.recover", "444", this);
            traceXAException(xae, currClass);
            checkXAException(xae); 
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "recover", "Exception");
            throw xae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "recover", xids);
        return xids;
    }

    /**
     * Inform the resource manager to roll back work done on behalf of a transaction branch.
     * This method should only be used by the MC on cleanup.
     * 
     * @exception XAException
     */
    protected final void rollback() throws XAException {
        rollback(ivXid); // Use the xid from XAResource.start 
    }

    /**
     * Inform the resource manager to roll back work done on behalf of a transaction branch
     * 
     * @param xid A global transaction identifier
     * @exception XAException
     *                Possible causes for the exception are:
     *                <ol>
     *                <li>rollback on the physical XAResource failed
     *                <li>rollback was called from an illegal transaction state
     *                </ol>
     */
    public void rollback(Xid xid) throws XAException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "rollback", ivManagedConnection, AdapterUtil.toString(xid));

        // Reset so we can deferred enlist in a future global transaction. 
        ivManagedConnection.wasLazilyEnlistedInGlobalTran = false; 

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work to continue.  In this case, we throw XAER_RMFAIL xa error
        // which indicates that the resource manager is not available
        if (ivManagedConnection._mcStale) {
            Tr.error(tc, "INVALID_CONNECTION");
            XAException x = new XAException(XAException.XAER_RMFAIL);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "rollback", new Object[] { "MC is stale throwing XAER_RMFAIL", ivManagedConnection });
            throw x;
        }

        if (ivXid == null) { 
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc,
                         "XAResource.start was never issued; allowing rollback for recovery."); 
            try {
                ivStateManager.setState(WSStateManager.XA_RECOVER);
            } catch (TransactionException te) {
                //Exception means setState failed because it was invalid to set the state in this case
                FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.rollback", "614", this);

                XAException xae = AdapterUtil.createXAException(
                                                                "INVALID_TX_STATE",
                                                                new Object[] { "XAResource.rollback", ivManagedConnection.getTransactionStateAsString() },
                                                                XAException.XAER_PROTO); 

                traceXAException(xae, currClass);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(this, tc, "rollback", xae);
                throw xae;
            }
            ivXid = xid;

        } else if (!xid.equals(ivXid)) { 
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "Xid does not match.", new Object[]
                {
                 "XAResource.start:    ",
                 AdapterUtil.toString(ivXid), 
                 "XAResource.rollback: ",
                 AdapterUtil.toString(xid) 
                         });

            XAException xaX = AdapterUtil.createXAException(
                                                            "XID_MISMATCH",
                                                            new Object[] { AdapterUtil.toString(ivXid), "rollback", AdapterUtil.toString(xid) },
                                                            XAException.XAER_NOTA); 

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", xaX);
            throw xaX;
        }

        // Defer xa.rollback if a stale connection occurred and the data source property
        // oracleRACXARetryDelay is configured.

        WSManagedConnectionFactoryImpl mcf = ivManagedConnection.mcf;
        long oracleRACLastStale = mcf.oracleRACLastStale.get();

        if (oracleRACLastStale > 0l) {
            // Determine if the current time falls within the delay period from the latest
            // stale connection. If so, we need to defer commit/rollback.

            long timeSinceLastStale = System.currentTimeMillis() - oracleRACLastStale;

            if (timeSinceLastStale > mcf.oracleRACXARetryDelay // Current time exceeds the delay period 
                && mcf.oracleRACLastStale.compareAndSet(oracleRACLastStale, 0l)) // Reset indicator as non-stale
            {
                if (tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "Time since last stale, " + timeSinceLastStale +
                                             " ms, exceeds the oracleRACXARetryDelay. Allowing rollback.");
            } else {
                // Current time falls with the delay period.
                // Defer the xa.rollback operation by raising an XAException with
                // error code of XA_RETRY. The transaction manager will retry the operation
                // after a configurable delay.

                if (tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "Time since last stale, " + timeSinceLastStale +
                                             " ms, falls within the oracleRACXARetryDelay. Deferring rollback.");

                XAException x = AdapterUtil.createXAException(
                                                              "ORACLE_RAC_RETRY",
                                                              new Object[] { "XAResource.rollback", timeSinceLastStale, mcf.oracleRACXARetryDelay },
                                                              XAException.XA_RETRY);

                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, "rollback", x);
                throw x;
            }
        }

        // The state should be changed back whether or not the rollback throws an exception,
        // unless it's a hueristic exception.
        boolean doChangeState = true;

        // Saved exception to throw later.
        XAException throwX = null;

        try {
            ivXaRes.rollback(xid);
        } catch (XAException xae) {
            FFDCFilter.processException(xae, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.rollback", "524", this);
            traceXAException(xae, currClass);
            checkXAException(xae); 
            if (xae.errorCode == XAException.XA_HEURCOM ||
                xae.errorCode == XAException.XA_HEURHAZ ||
                xae.errorCode == XAException.XA_HEURMIX ||
                xae.errorCode == XAException.XA_HEURRB) {
                doChangeState = false;

                try {
                    // we won't call forget() here, instead, we will forget the tran only when the TM manager calls it on us.
                    // this way if the server crashes after the adapter receives heuristic and before the TM logging it. TM could still
                    // as the database for the in doubt trans since its not forgotten yet

                    ivStateManager.setState(WSStateManager.HEURISTIC_END);
                } catch (TransactionException te) {
                    FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.rollback", "540", this);
                    Tr.warning(tc, "DSA_INTERNAL_WARNING", new Object[] { "Exception setting the transaction state to WSStateManager.HEURISTIC_END from ",
                                                                         ivManagedConnection.getTransactionStateAsString(), te });
                    //should never happen so just eat it
                }
            } 
              // Throw the exception later.
            throwX = xae;
        }

        if (doChangeState && !ivManagedConnection.isAborted())
            try {
                ivStateManager.setState(WSStateManager.XA_ROLLBACK);
            } catch (TransactionException te) {
                //Exception means setState failed because it was invalid to set the state in this case
                FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.rollback", "510", this);
                Tr.error(tc, "INVALID_TX_STATE", new Object[] { "XAResource.rollback()", ivManagedConnection.getTransactionStateAsString() });

                XAException xae = AdapterUtil.createXAException(
                                                                "INVALID_TX_STATE",
                                                                new Object[] { "XAResource.rollback", ivManagedConnection.getTransactionStateAsString() },
                                                                XAException.XAER_PROTO); 

                traceXAException(xae, currClass);

                if (throwX == null)
                    throwX = xae;
            }
        ivXid = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
        {
            String cId = null;
            try {
                cId = ivManagedConnection.mcf.getCorrelator(ivManagedConnection); 
            } catch (SQLException x) {
                // will just log the exception here and ignore it since its in trace
                Tr.debug(this, tc, "got an exception trying to get the correlator in rollback, exception is: ", x);
            }
            if (cId != null) {
                StringBuffer stbuf = new StringBuffer(200);
                stbuf.append("Correlator: DB2, ID: ");
                stbuf.append(cId);
                if (xid != null) {
                    stbuf.append("Transaction ID : ");
                    stbuf.append(xid);
                }
                stbuf.append(" ROLLBACK");

                Tr.debug(this, tc, stbuf.toString());
            }
        }
        if (throwX != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", throwX);
            throw throwX;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "rollback");
    }

    /**
     * Set the current transaction timeout value for this XAResource instance. Once set, this
     * timeout value is effective until setTransactionTimeout is invoked again with a different
     * value. To reset the timeout value to the default value used by the resource manager, set
     * the value to zero. If the timeout operation is performed successfully, the method returns
     * true; otherwise false.
     * <p>If a resource manager does not support transaction timeout value to be set explicitly,
     * this method returns false.
     * 
     * @param seconds
     * @return true if transaction timeout value is set successfully; otherwise false.
     * @exception XAException
     *                Possible exception values are XAER_RMERR, XAER_RMFAIL, or XAER_INVAL.
     */
    public final boolean setTransactionTimeout(int seconds) throws XAException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) 
            Tr.event(this, tc, "setTransactionTimeout", seconds);

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work to continue.  In this case, we throw XAER_RMFAIL xa error
        // which indicates that the resource manager is not available
        if (ivManagedConnection._mcStale) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "MC is stale throwing XAER_RMFAIL", ivManagedConnection);
            Tr.error(tc, "INVALID_CONNECTION");
            throw new XAException(XAException.XAER_RMFAIL);
        }

        return ivXaRes.setTransactionTimeout(seconds);
    }

    /**
     * Start work on behalf of a transaction branch specified in xid If TMJOIN is specified, the start is for joining a transaction
     * previously seen by the resource manager. If TMRESUME is specified, the start is to resume a suspended transaction specified
     * in the parameter xid. If neither TMJOIN nor TMRESUME is specified and the transaction specified by xid has previously
     * been seen by the resource manager, the resource manager throws the XAException exception with XAER_DUPID error code.
     * 
     * @param Xid xid - A global transaction identifier to be associated with the resource
     * @param int flags - One of TMNOFLAGS, TMJOIN, or TMRESUME
     * @exception XAException - An error has occurred. Possible exceptions are XA_RB*, XAER_RMERR, XAER_RMFAIL,
     *                XAER_DUPID, XAER_OUTSIDE, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     */

    public void start(Xid xid, int flags) throws XAException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "start", new Object[]
            {
             ivManagedConnection,
             AdapterUtil.toString(xid), 
             AdapterUtil.getXAResourceStartFlagString(flags)
            });

        // if the MC marked Stale, it means the user requested a purge pool with an immediate option
        // so don't allow any work to continue.  In this case, we throw XAER_RMFAIL xa error
        // which indicates that the resource manager is not available
        if (ivManagedConnection._mcStale) {
            Tr.error(tc, "INVALID_CONNECTION");
            XAException x = new XAException(XAException.XAER_RMFAIL);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
                Tr.exit(this, tc, "start", new Object[] { "MC is stale throwing XAER_RMFAIL", ivManagedConnection });
            throw x;
        }

        if (dsConfig.get().enableMultithreadedAccessDetection) 
            ivManagedConnection.detectMultithreadedAccess(); 

        this.ivXid = xid;

        try {
            ivXaRes.start(xid, flags);
            ivStateManager.setState(WSStateManager.XA_START);
        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.start", "615", this);
            Tr.error(tc, "INVALID_TX_STATE", new Object[] { "XAResource.start()", ivManagedConnection.getTransactionStateAsString() });
            try {
                ivXaRes.end(xid, XAResource.TMNOFLAGS);
                ivXaRes.rollback(xid);
            } catch (XAException eatXA) {
                FFDCFilter.processException(eatXA, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.start", "624", this);
                traceXAException(eatXA, currClass);
                //eat this exception because in the next line we will throw one
            }

            XAException xae = AdapterUtil.createXAException(
                                                            "INVALID_TX_STATE",
                                                            new Object[] { "XAResource.start", ivManagedConnection.getTransactionStateAsString() },
                                                            XAException.XA_RBPROTO); 

            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "start", "Exception");
            throw xae;
        } catch (XAException xae) {
            FFDCFilter.processException(xae, "com.ibm.ws.rsadapter.spi.WSRdbXaResourceImpl.start", "639", this);
            traceXAException(xae, currClass);
            checkXAException(xae); 
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "start", "Exception");
            throw xae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
        {
            String cId = null;
            try {
                cId = ivManagedConnection.mcf.getCorrelator(ivManagedConnection); 
            } catch (SQLException x) {
                // will just log the exception here and ignore it since its in trace
                Tr.debug(this, tc, "got an exception trying to get the correlator in rollback, exception is: ", x);
            }
            if (cId != null) {
                StringBuffer stbuf = new StringBuffer(200);
                stbuf.append("Correlator: DB2, ID: ");
                stbuf.append(cId);
                if (xid != null) {
                    stbuf.append("Transaction ID : ");
                    stbuf.append(xid);
                }
                stbuf.append(" BEGIN");

                Tr.debug(this, tc, stbuf.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "start");
    }

    /**
     * Method to translate the XAResource stuff, including the error code.
     * 
     * @param xae
     * @param callerClass
     * @return
     */
    public final XAException traceXAException(XAException xae, Class<?> callerClass) { 

        String detailedMessage = ivManagedConnection.helper.getXAExceptionContents(xae);
        Tr.error(tc, "DISPLAY_XAEX_CONTENT", detailedMessage);

        Tr.error(tc, "THROW_XAEXCEPTION", new Object[]
        { AdapterUtil.getXAExceptionCodeString(xae.errorCode), xae.getMessage() });

        return xae;
    }

    private void checkXAException(XAException xae) {

        /*
         * We need to check the XAException error code for XAER_RMFAIL. If the
         * error code is XAER_RMFAIL or there was a StaleConnection, the database is down and we
         * need to call processConnectionErrorOccurredEvent to process the
         * connection pool's purge policy.
         */
        boolean connError = false;
        boolean authError = false; 
        boolean containsCause = false; // this indicates that XAException has cause
        SQLException cause = null; 
        ResourceException dsae = null; 
        //Check next exception in all cases, not just the non XAER_RMFAIL.  Reason for this is
        // that the TrustedContext may throw an RMFAIL which contains an authorization exception
        // which when happen, we don't want to purge all connections, we only need to do a subset.
        // of course, the other reason we are checking the cause is for clientReroute.

        try {
            cause = (SQLException) xae.getCause();
        } catch (Throwable t) {
            // just log it and assume no cause
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            {
                Tr.debug(this, tc, "cause is not an SQLException so it will stay null");
            }
        }

        if (cause != null) {
            containsCause = true;
            dsae = AdapterUtil.translateSQLException(cause, ivManagedConnection, true, currClass);

            //check for authorization first.  Order matters here
            if (dsae instanceof SecurityException) {
                authError = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                {
                    Tr.debug(this, tc, "Authorization Exception is chanined to the XAException");
                }
            } else if (ivManagedConnection.helper.isConnectionError(cause)) {
                connError = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                {
                    Tr.debug(this, tc, "Stale connection error is chanined to the XAException");
                }
            }
        }

        switch (xae.errorCode) {
            case XAException.XAER_RMFAIL:

                if (!containsCause) // no cause in XAException so its automatically conn error (i.e. no need to check cause
                {
                    connError = true;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                    {
                        Tr.debug(this, tc, "XAER_RMFAIL connection error occurred");
                    }
                }

                break;

            default:
                break;// nothing needed here

        }

        if (connError || authError) {
            try {
                if (connError && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "fire normal connection error occurred event for XAResource " + this);
                }
                if (authError && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "fire Single connection error occurred event for XAResource " + this);
                }
                // Let the Managed Connection handle any duplicate events.
                ivManagedConnection.processConnectionErrorOccurredEvent(null, containsCause ? cause : xae);
            } catch (NullPointerException nullX) {
                // No FFDC code needed; we might be closed.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) 
                    Tr.event(tc
                             , "Handle CLOSED or INACTIVE. Not sending CONNECTION_ERROR_OCCURRED. (caller, mc)"
                             , new Object[] { this, ivManagedConnection }
                                    );
            }
        }

        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            {
                Tr.debug(this, tc, "XAException was not a connection error for XAResource " + this);
            }
        }

        return;

    }

}