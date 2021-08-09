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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference; 

import javax.resource.spi.ManagedConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.OnePhaseXAResource; 
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
 * 
 * <p>The transaction manager uses the interface to communicate transaction association,
 * completion, and recovery to the resource manager. This class is to allow one phase resources,
 * which do not have XAResources, to participate in global transactions. All actions performed
 * in this class are performed on the physical connection. For example, if a commit is issued
 * against this resource, the commit is called directly on the physical connection to the database.
 * 
 * <P>There is only one OnePhaseXAResource instance associated with WSRdbManagedConnectionImpl instance.
 * 
 * @version 1.32
 */
public class WSRdbOnePhaseXaResourceImpl implements WSXAResource, OnePhaseXAResource, FFDCSelfIntrospectable { 
    //internal variables
    private final Connection ivSqlConn;
    private final WSRdbManagedConnectionImpl ivManagedConnection;
    private final WSStateManager ivStateManager;

    /** Data source configuration. */
    private final AtomicReference<DSConfig> dsConfig;

    private static final Class<?> currClass = WSRdbOnePhaseXaResourceImpl.class;
    private static final TraceComponent tc = Tr.register(currClass, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * main ctor
     * 
     * @param conn
     * @param databaseType
     * @param mc
     */
    public WSRdbOnePhaseXaResourceImpl(Connection conn, WSRdbManagedConnectionImpl mc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", new Object[] 
                     { AdapterUtil.toString(conn), mc });

        ivSqlConn = conn;
        ivManagedConnection = mc;
        ivStateManager = ivManagedConnection.stateMgr;
        dsConfig = mc.mcf.dsConfig; 

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "<init>", this);
    }

    /*
     * Commits this branch of the global transaction
     * 
     * @param Xid xid - the identifier for the global transaction
     * 
     * @param boolean onePhase - indicates if the commit is one or two phase
     * 
     * @exception XAException - Possible causes of this exception are:
     * 1) the onePhase parameter is false
     * 2) calling commit on the physical connection threw a SQLException
     * 3) commit was called from an illegal transaction state
     */

    // 10/29/01 Note:  for xa_commit - if sql.connection.commit fails,then try to rollback, 
    // if rollback succeeds, then return XA_RBROLLBACK, if rollback failed, then return XAER_RMERR 

    public void commit(Xid xid, boolean onePhase) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "commit", new Object[] 
                     {
                      ivManagedConnection,
                      AdapterUtil.toString(xid), 
                      onePhase ? "ONE PHASE" : "TWO PHASE" }
                            );

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
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
                stbuf.append(" COMMIT");
                Tr.debug(this, tc, stbuf.toString());
            }
        }

        if (dsConfig.get().enableMultithreadedAccessDetection) 
            ivManagedConnection.detectMultithreadedAccess(); 

        if (!onePhase) {
            XAException xaX = new XAException(XAException.XA_RBPROTO);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", xaX); 
            throw xaX;
        }

        boolean commitFailed = false;
        boolean rollbackFailed = false;
        boolean setStateFailed = false;

        // Reset so we can deferred enlist in a future global transaction. 
        ivManagedConnection.wasLazilyEnlistedInGlobalTran = false; 

        try {
            // If no work was done during the transaction, the autoCommit value may still
            // be on.  In this case, just no-op, since some drivers like ConnectJDBC 3.1
            // don't allow commit/rollback when autoCommit is on.  

            ivSqlConn.commit(); 
            ivStateManager.setState(WSStateManager.XA_COMMIT);
        } catch (SQLException sqeC)
        {
            FFDCFilter.processException(sqeC, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.commit", "105", this);
            commitFailed = true;
            Tr.error(tc, "DSA_INTERNAL_ERROR", new Object[] { "Exception caught during commit on the OnePhaseXAResource", sqeC });

            // If no work was done during the transaction, the autoCommit value may still
            // be on.  In this case, just no-op, since some drivers like ConnectJDBC 3.1
            // don't allow commit/rollback when autoCommit is on.  

            try
            { // autoCommit is off
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())  
                    Tr.debug(this, tc, "issue a rollback due to commit failure");
                ivSqlConn.rollback();
            } catch (SQLException sqeR)
            {
                FFDCFilter.processException(sqeR, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.commit", "197", this);
                rollbackFailed = true;
                Tr.error(tc, "DSA_INTERNAL_ERROR", new Object[] { "Exception caught during rollback on the OnePhaseXAResource after a commit failed", sqeR });
            }
            catch (java.lang.RuntimeException x)
            {
                FFDCFilter.processException(x, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.commit", "204", this);
                rollbackFailed = true;
                Tr.error(tc, "DSA_INTERNAL_ERROR", new Object[] { "Exception caught during rollback on the OnePhaseXAResource after a commit failed", x });
            }

        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.commit", "123", this);
            Tr.error(tc, "INVALID_TX_STATE", new Object[] { "OnePhaseXAResource.commit()", ivManagedConnection.getTransactionStateAsString() });
            setStateFailed = false;

        }
        catch (java.lang.RuntimeException x)
        {
            FFDCFilter.processException(x, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.commit", "221", this);
            Tr.error(tc, "DSA_INTERNAL_ERROR", new Object[] { "Exception caught during commit on the OnePhaseXAResource", x });

            XAException xae = new XAException(XAException.XA_HEURHAZ); // throwing Heurhaz sine we don't really know f the commit failed or not.             
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", "Exception"); 
            throw xae;

        }

        if (rollbackFailed) {
            XAException xae = new XAException(XAException.XAER_RMERR);
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", "Exception"); 
            throw xae;
        } else if (commitFailed) {
            XAException xae = new XAException(XAException.XA_HEURHAZ); //  we don't know if the commit really failed, or the failure in communication after the commit succeeded             
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", "Exception"); 
            throw xae;
        } else if (setStateFailed) {
            XAException xae = new XAException(XAException.XAER_RMERR);
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "commit", "Exception"); 
            throw xae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "commit"); 
    }

    /*
     * End the work performed on this branch of the transaction.
     * 
     * @param Xid xid - the transaction identifier
     * 
     * @param flags - indicate if failures have occurred
     * 
     * @exception XAException - Possible causes for this exception are:
     * 1) end was called in an illegal transaction state
     */

    //10/29/01 Note:  for xa_end, there is no heuristic exception, so you need to throw the 
    //XAException with return code XA_RBROLLBACK

    public void end(Xid xid, int flags) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "end", new Object[] 
                     {
                      ivManagedConnection,
                      AdapterUtil.toString(xid), 
                      AdapterUtil.getXAResourceEndFlagString(flags)
            });

        try {
            if (flags == XAResource.TMFAIL) {
                ivStateManager.setState(WSStateManager.XA_END_FAIL);
            } else {
                ivStateManager.setState(WSStateManager.XA_END);
            }
        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.end", "189", this);
            Tr.error(tc, "INVALID_TX_STATE", new Object[] { "OnePhaseXAResource.end()", ivManagedConnection.getTransactionStateAsString() });
            XAException xae = new XAException(XAException.XA_RBPROTO);
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "end", "Exception"); 
            throw xae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "end"); 
    }

    /*
     * Tell the resource manager to forget about a heuristically completed transaction branch.
     * 
     * @param Xid xid - A global transaction identifier
     * 
     * @exception XAException - Possible causes for this exception are:
     * 1) it is illegal to call this method on a OnePhaseXaResource so this exception is always thrown
     */

    public void forget(Xid xid)
                    throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "forget", AdapterUtil.toString(xid)); 
        Tr.error(tc, "INVALID_TX_STATE", new Object[] { "OnePhaseXAResource.forget()", ivManagedConnection.getTransactionStateAsString() });

        XAException xae = new XAException(XAException.XA_RBPROTO);
        traceXAException(xae, currClass);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "forget", xae);
        throw xae;
    }

    /**
     * Returns the managed connection that created this XA resource.
     * 
     * @return the managed connection that created this XA resource.
     */
    public final ManagedConnection getManagedConnection()
    {
        return ivManagedConnection;
    }

    /**
     * @return the name of the OnePhaseXAResource
     */
    @Override
    public final String getResourceName() {
        return ivManagedConnection.helper.getDatabaseProductName();
    }

    /*
     * Returns the transaction timeout value
     * 
     * @return int - always -1 as there is no transaction timeout on a OnePhaseXAResource
     */

    public int getTransactionTimeout()
                    throws XAException
    {
        return -1;
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
        info.append("Connection:", AdapterUtil.toString(ivSqlConn));
        info.append("ManagedConnection:", ivManagedConnection);
    }

    /*
     * Returns a boolean indicating if this XAResource is from the same ResourceManager
     * as the one passed in as a parameter
     * 
     * @param XAResource xares - an XAResource
     * 
     * @exception XAException - this will never be thrown
     * <p> Note this method always returns false as there can only be a single OnePhaseXaResource in
     * the transaction at a time.
     */

    public boolean isSameRM(XAResource xares)
                    throws XAException
    {
        return false;
    }

    /*
     * Ask the resource manager to prepare for a transaction commit of the transaction specified in xid.
     * 
     * @param Xid xid - A global transaction identifier
     * 
     * @return int -A value indicating the resource manager's vote on the outcome of the transaction. The possible values are:
     * XA_RDONLY or XA_OK. If the resource manager wants to roll back the transaction, it should do so by raising an
     * appropriate XAException in the prepare method.
     * 
     * @exception XAException - Possible causes for this exception are:
     * 1) this exception will always be thrown on a OnePhaseXaResource as prepare should never be called.
     */

    public int prepare(Xid xid)
                    throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            Tr.entry(this, tc, "prepare", AdapterUtil.toString(xid)); 
        Tr.error(tc, "INVALID_TX_STATE", new Object[] { "OnePhaseXAResource.prepare()", ivManagedConnection.getTransactionStateAsString() });
        XAException xae = new XAException(XAException.XA_RBPROTO);
        traceXAException(xae, currClass);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            Tr.exit(this, tc, "prepare", xae);
        throw xae;
    }

    /**
     * Obtain a list of prepared transaction branches from a resource manager. The transaction manager calls this method during
     * recovery to obtain the list of transaction branches that are currently in prepared or heuristically completed states.
     * 
     * @param int flag - One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS must be used when no other flags
     *        are set in flags.
     * 
     * @return an empty list of Xid because this is a one-phase resource.
     * 
     * @throws XAException never, because we always return an empty list for one-phase.
     */
    public Xid[] recover(int flag) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "recover", 
                     AdapterUtil.getXAResourceRecoverFlagString(flag));

        // In JCA 1.5, we are required to return an empty list.

        Xid[] emptyList = new Xid[] {};

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "recover", emptyList); 
        return emptyList;
    }

    /*
     * Inform the resource manager to roll back work done on behalf of a transaction branch
     * 
     * @param Xid xid - A global transaction identifier
     * 
     * @exception XAException - Possible causes for the exception are:
     * 1) rollback on the physical connection failed causing a SQLException
     * 2) rollback was called from an illegal transaction state
     */

    //10/29/01 Note:  for xa_rollback - if conneciton.rollback fails, then return 
    // XAER_RMERR return code in XAException

    public void rollback(Xid xid) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "rollback", new Object[] 
                     {
                      ivManagedConnection,
                      AdapterUtil.toString(xid) 
            });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
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
                stbuf.append(" ROLLBACK");
                Tr.debug(this, tc, stbuf.toString());
            }
        }

        // Reset so we can deferred enlist in a future global transaction. 
        ivManagedConnection.wasLazilyEnlistedInGlobalTran = false; 

        try {
            // If no work was done during the transaction, the autoCommit value may still
            // be on.  In this case, just no-op, since some drivers like ConnectJDBC 3.1
            // don't allow commit/rollback when autoCommit is on.  

            ivSqlConn.rollback();
            ivStateManager.setState(WSStateManager.XA_ROLLBACK);
        } catch (SQLException sqe) {
            FFDCFilter.processException(sqe, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.rollback", "342", this);
            Tr.error(tc, "DSA_INTERNAL_ERROR", new Object[] { "Exception caught during rollback on the OnePhaseXAResource", sqe });
            XAException xae = new XAException(XAException.XAER_RMERR);
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", "Exception"); 
            throw xae;
        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.rollback", "351", this);
            Tr.error(tc, "INVALID_TX_STATE", new Object[] { "OnePhaseXAResource.rollback()", ivManagedConnection.getTransactionStateAsString() });
            XAException xae = new XAException(XAException.XAER_RMERR);
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "rollback", "Exception"); 
            throw xae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "rollback"); 
    }

    /*
     * Set the transaction timeout
     * 
     * @param int seconds - the number of seconds to set the timeout to
     * 
     * @return boolean - indicates if the timeout was successfully set
     * 
     * @exception XAException - never thrown from this method
     * <p>Note this method will always return false as it is not allowed to set the transaction timeout
     * on a OnePhaseXAResource
     */

    public boolean setTransactionTimeout(int seconds)
                    throws XAException
    {
        return false;
    }

    /*
     * Start work on behalf of a transaction branch specified in xid If TMJOIN is specified, the start is for joining a transaction
     * previously seen by the resource manager. If TMRESUME is specified, the start is to resume a suspended transaction specified
     * in the parameter xid. If neither TMJOIN nor TMRESUME is specified and the transaction specified by xid has previously
     * been seen by the resource manager, the resource manager throws the XAException exception with XAER_DUPID error code.
     * 
     * @param Xid xid - A global transaction identifier to be associated with the resource
     * 
     * @param int flags - One of TMNOFLAGS, TMJOIN, or TMRESUME
     * 
     * @exception XAException - Possible causes for this exception are:
     * 1) start was called from an illegal transaction state.
     */

    public void start(Xid xid, int flags) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "start", new Object[] 
                     {
                      ivManagedConnection,
                      AdapterUtil.toString(xid), 
                      AdapterUtil.getXAResourceStartFlagString(flags)
            });

        if (dsConfig.get().enableMultithreadedAccessDetection) 
            ivManagedConnection.detectMultithreadedAccess(); 

        try {
            ivManagedConnection.setAutoCommit(false);
            ivStateManager.setState(WSStateManager.XA_START);
        } catch (SQLException sqle) {  
            FFDCFilter.processException(sqle, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.start", "406", this);
            Tr.error(tc, "DSA_ERROR", new Object[] { sqle, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.start" });
            XAException xae = new XAException(XAException.XA_RBPROTO);
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "start", "Exception"); 
            throw xae;
        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            FFDCFilter.processException(te, "com.ibm.ws.rsadapter.spi.WSRdbOnePhaseXaResourceImpl.start", "407", this);
            Tr.error(tc, "INVALID_TX_STATE", new Object[] { "OnePhaseXAResource.start()", ivManagedConnection.getTransactionStateAsString() });
            XAException xae = new XAException(XAException.XA_RBPROTO);
            traceXAException(xae, currClass);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "start", "Exception"); 
            throw xae;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "start"); 
    }

    /**
     * Method to translate the XAResource stuff, including the error code.
     */
    public static final XAException traceXAException(XAException xae, Class<?> callerClass) {
        Tr.warning(tc, "THROW_XAEXCEPTION", new Object[]
        { AdapterUtil.getXAExceptionCodeString(xae.errorCode), xae.getMessage() });

        return xae;
    }
}