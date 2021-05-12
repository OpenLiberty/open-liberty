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

import java.sql.Connection;
import java.sql.SQLException;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.adapter.AdapterUtil;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.rsadapter.exceptions.TransactionException;

/**
 * This class implements the javax.transaction.xa.XAResource interface.
 *
 * <p>
 * The transaction manager uses the interface to communicate transaction
 * association, completion, and recovery to the resource manager. This class is
 * to allow one phase resources, which do not have XAResources, to participate
 * in global transactions. All actions performed in this class are performed on
 * the physical connection. For example, if a commit is issued against this
 * resource, the commit is called directly on the physical connection to the
 * database.
 *
 * <P>
 * There is only one OnePhaseXAResource instance associated with
 * WSRdbManagedConnectionImpl instance.
 *
 */
public class OnePhaseXAResourceImpl implements javax.transaction.xa.XAResource {
    /** Native connection */
    private final Connection ivSqlConn;

    /** Managed Connection */
    private final ManagedConnectionImpl ivMC;

    /** State manager */
    private final StateManager ivStateManager;

    private static final Class currClass = OnePhaseXAResourceImpl.class;
    // private static final TraceComponent tc = Tr.register(currClass);
    protected static final TraceComponent tc = Tr.register(currClass); // LIDB2110-69

    /**
     * main constructor
     *
     * @param Connection
     *            The native connection
     * @param ManagedConnection
     *            The managed connection
     */
    public OnePhaseXAResourceImpl(Connection conn, ManagedConnectionImpl mc) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { AdapterUtil.toString(conn),
                                                  mc });

        ivSqlConn = conn;
        ivMC = mc;
        ivStateManager = ivMC.stateMgr;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    /**
     * Commits this branch of the global transaction
     *
     * @param Xid
     *            xid - the identifier for the global transaction
     * @param boolean onePhase - indicates if the commit is one or two phase
     * @exception XAException
     *                - Possible causes of this exception are: 1) the onePhase
     *                parameter is false 2) calling commit on the physical
     *                connection threw a SQLException 3) commit was called from
     *                an illegal transaction state
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commit", new Object[] { this, ivMC, xid,
                                                  onePhase ? "ONE PHASE" : "TWO PHASE" });

        if (!onePhase) {
            XAException xaX = new XAException(XAException.XA_RBPROTO);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", xaX);
            throw xaX;
        }

        boolean commitFailed = false;
        boolean rollbackFailed = false;
        boolean setStateFailed = false;

        // LIDB????

        ivMC.isLazyEnlisted = false;

        // if (ivMC.isDynamicEnlistment()) {
        // Reset so we can deferred enlist in a future global transaction.
        // [d137506]
        // ivMC.ivAlreadyProcessedInteractionPendingEvent = false;
        // }

        try {

            // If no work was done during the transaction, the autoCommit value
            // may still
            // be on. In this case, just no-op, since some drivers like
            // ConnectJDBC 3.1
            // don't allow commit/rollback when autoCommit is on. [d145849]

            if (!ivMC.getAutoCommit())
                ivSqlConn.commit();
            ivStateManager.setState(StateManager.XA_COMMIT);

        } catch (SQLException sqeC) {
            commitFailed = true;
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "DSA_INTERNAL_ERROR",
                         new Object[] {
                                        "Exception caught during commit on the OnePhaseXAResource",
                                        sqeC });

            // If no work was done during the transaction, the autoCommit value
            // may still
            // be on. In this case, just no-op, since some drivers like
            // ConnectJDBC 3.1
            // don't allow commit/rollback when autoCommit is on. [d145849]

            try // autoCommit is off
            {
                if (ivMC.getAutoCommit())
                    ivSqlConn.rollback();
            } catch (SQLException sqeR) {
                rollbackFailed = true;
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "DSA_INTERNAL_ERROR",
                             new Object[] {
                                            "Exception caught during rollback on the OnePhaseXAResource",
                                            sqeR });
            }
        } catch (TransactionException te) {
            // Exception means setState failed because it was invalid to set the
            // state in this case
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "INVALID_TX_STATE",
                         new Object[] { "OnePhaseXAResource.commit()",
                                        ivMC.getTransactionStateAsString() });
            setStateFailed = false;

        }

        if (rollbackFailed) {
            XAException xae = new XAException(XAException.XAER_RMERR);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", "Exception");
            throw xae;
        } else if (commitFailed) {
            XAException xae = new XAException(XAException.XA_RBROLLBACK);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", "Exception");
            throw xae;
        } else if (setStateFailed) {
            XAException xae = new XAException(XAException.XAER_RMERR);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", "Exception");
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commit");
    }

    /**
     * End the work performed on this branch of the transaction.
     *
     * @param Xid
     *            xid - the transaction identifier
     * @param flags
     *            - indicate if failures have occurred
     * @exception XAException
     *                - Possible causes for this exception are: 1) end was
     *                called in an illegal transaction state
     */
    @Override
    public void end(Xid xid, int flags) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "end",
                     new Object[] { this, ivMC, xid,
                                    AdapterUtil.getXAResourceEndFlagString(flags) });

        try {
            if (flags == XAResource.TMFAIL) {
                ivStateManager.setState(StateManager.XA_END_FAIL);
            } else {
                ivStateManager.setState(StateManager.XA_END);
            }
        } catch (TransactionException te) {
            // Exception means setState failed because it was invalid to set the
            // state in this case
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "INVALID_TX_STATE",
                         new Object[] { "OnePhaseXAResource.end()",
                                        ivMC.getTransactionStateAsString() });
            XAException xae = new XAException(XAException.XA_RBPROTO);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "end", "Exception");
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "end");
    }

    /*
     * Tell the resource manager to forget about a heuristically completed
     * transaction branch.
     *
     * @param Xid xid - A global transaction identifier
     *
     * @exception XAException - Possible causes for this exception are: 1) it is
     * illegal to call this method on a OnePhaseXaResource so this exception is
     * always thrown
     */
    @Override
    public void forget(Xid xid) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "forget", xid);

        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "INVALID_TX_STATE",
                     new Object[] { "OnePhaseXAResource.forget()",
                                    ivMC.getTransactionStateAsString() });

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forget");

        XAException xae = new XAException(XAException.XA_RBPROTO);
        traceXAException(xae, currClass);
        throw xae;
    }

    /**
     * Returns the transaction timeout value
     *
     * @return int - always -1 as there is no transaction timeout on a
     *         OnePhaseXAResource
     *
     */

    @Override
    public int getTransactionTimeout() throws XAException {
        return -1;
    }

    /**
     * Returns a boolean indicating if this XAResource is from the same
     * ResourceManager as the one passed in as a parameter
     *
     * @param XAResource
     *            xares - an XAResource
     * @exception XAException
     *                - this will never be thrown
     *                <p>
     *                Note this method always returns false as there can only be
     *                a single OnePhaseXaResource in the transaction at a time.
     */

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        return false;
    }

    /**
     * Ask the resource manager to prepare for a transaction commit of the
     * transaction specified in xid.
     *
     * @param Xid
     *            xid - A global transaction identifier
     * @return int -A value indicating the resource manager's vote on the
     *         outcome of the transaction. The possible values are: XA_RDONLY or
     *         XA_OK. If the resource manager wants to roll back the
     *         transaction, it should do so by raising an appropriate
     *         XAException in the prepare method.
     * @exception XAException
     *                - Possible causes for this exception are: 1) this
     *                exception will always be thrown on a OnePhaseXaResource as
     *                prepare should never be called.
     */

    @Override
    public int prepare(Xid xid) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "prepare", xid);

        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "INVALID_TX_STATE",
                     new Object[] { "OnePhaseXAResource.prepare()",
                                    ivMC.getTransactionStateAsString() });

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepare");

        XAException xae = new XAException(XAException.XA_RBPROTO);
        traceXAException(xae, currClass);
        throw xae;
    }

    /**
     * Obtain a list of prepared transaction branches from a resource manager.
     * The transaction manager calls this method during recovery to obtain the
     * list of transaction branches that are currently in prepared or
     * heuristically completed states.
     *
     * @param int flag - One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS
     *            must be used when no other flags are set in flags.
     * @return Xid[] - The resource manager returns zero or more XIDs for the
     *         transaction branches that are currently in a prepared or
     *         heuristically completed state. If an error occurs during the
     *         operation, the resource manager should throw the appropriate
     *         XAException.
     * @exception XAException
     *                - Possible causes for this exception are: 1) this
     *                exception will always be thrown as it is illegal to call
     *                recover on a OnePhaseXAResource
     */

    @Override
    public Xid[] recover(int flag) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recover",
                     AdapterUtil.getXAResourceRecoverFlagString(flag));

        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "INVALID_TX_STATE",
                     new Object[] { "OnePhaseXAResource.recover()",
                                    ivMC.getTransactionStateAsString() });

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recover");

        XAException xae = new XAException(XAException.XA_RBPROTO);
        traceXAException(xae, currClass);
        throw xae;
    }

    /**
     * Inform the resource manager to roll back work done on behalf of a
     * transaction branch
     *
     * @param Xid
     *            xid - A global transaction identifier
     * @exception XAException
     *                - Possible causes for the exception are: 1) rollback on
     *                the physical connection failed causing a SQLException 2)
     *                rollback was called from an illegal transaction state
     */
    @Override
    public void rollback(Xid xid) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "rollback", new Object[] { this, ivMC, xid });

        // LIDB????
        ivMC.isLazyEnlisted = false;

        // if (ivMC.isDynamicEnlistment()) {
        // // Reset so we can deferred enlist in a future global transaction.
        // [d137506]
        // ivMC.ivAlreadyProcessedInteractionPendingEvent = false;
        // }

        try {
            ivSqlConn.rollback();
            ivStateManager.setState(StateManager.XA_ROLLBACK);

        } catch (SQLException sqe) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "DSA_INTERNAL_ERROR",
                         new Object[] {
                                        "Exception caught during rollback on the OnePhaseXAResource",
                                        sqe });

            XAException xae = new XAException(XAException.XAER_RMERR);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", "Exception");
            throw xae;
        } catch (TransactionException te) {
            // Exception means setState failed because it was invalid to set the
            // state in this case
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "INVALID_TX_STATE",
                         new Object[] { "OnePhaseXAResource.rollback()",
                                        ivMC.getTransactionStateAsString() });
            XAException xae = new XAException(XAException.XAER_RMERR);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", "Exception");
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "rollback");
    }

    /**
     * Set the transaction timeout
     *
     * @param int seconds - the number of seconds to set the timeout to
     * @return boolean - indicates if the timeout was successfully set
     * @exception XAException
     *                - never thrown from this method
     *                <p>
     *                Note this method will always return false as it is not
     *                allowed to set the transaction timeout on a
     *                OnePhaseXAResource
     */

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return false;
    }

    /**
     * Start work on behalf of a transaction branch specified in xid If TMJOIN
     * is specified, \ the start is for joining a transaction previously seen by
     * the resource manager. If TMRESUME is specified, the start is to resume a
     * suspended transaction specified in the parameter xid. If neither TMJOIN
     * nor TMRESUME is specified and the transaction specified by xid has
     * previously been seen by the resource manager, the resource manager throws
     * the XAException exception with XAER_DUPID error code.
     *
     * @param Xid
     *            xid - A global transaction identifier to be associated with
     *            the resource
     * @param int flags - One of TMNOFLAGS, TMJOIN, or TMRESUME
     * @exception XAException
     *                - Possible causes for this exception are: 1) start was
     *                called from an illegal transaction state.
     */

    @Override
    public void start(Xid xid, int flags) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "start",
                     new Object[] { this, ivMC, xid,
                                    AdapterUtil.getXAResourceStartFlagString(flags) });

        try {
            ivMC.setAutoCommit(false);
            ivStateManager.setState(StateManager.XA_START);
        } catch (SQLException sqle) {
            // Exception means autoCommit cannot be set at this time
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setAutoCommit(false) throws an exception: "
                             + sqle.getMessage());
            XAException xae = new XAException(XAException.XA_RBPROTO);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "start", "Exception");
            throw xae;

        } catch (TransactionException te) {
            // Exception means setState failed because it was invalid to set the
            // state in this case
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "Invalid transaction state: ",
                         new Object[] { "OnePhaseXAResource.start()",
                                        ivMC.getTransactionStateAsString() });
            XAException xae = new XAException(XAException.XA_RBPROTO);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "start", "Exception");
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "start");
    }

    /**
     * Method to translate the XAResource stuff, including the error code.
     */
    public static final XAException traceXAException(XAException xae,
                                                     Class callerClass) {
        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "THROW_XAEXCEPTION",
                     new Object[] {
                                    AdapterUtil.getXAExceptionCodeString(xae.errorCode),
                                    xae.getMessage() });

        return xae;
    }

    public final String getResourceName() {
        return "dummy";
    }

}
