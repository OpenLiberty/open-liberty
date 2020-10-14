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

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.adapter.AdapterUtil;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.rsadapter.exceptions.TransactionException;

/**
 * Implementation class for XAResource. <p>
 */
public class XAResourceImpl implements XAResource {
    //private static final TraceComponent tc = Tr.register(XAResourceImpl.class);
    protected static final TraceComponent tc = Tr.register(XAResourceImpl.class); //LIDB2110-69

    /** native xa resource */
    private final XAResource ivXaRes;

    /** Managed connection */
    private final ManagedConnectionImpl ivMC;

    /** State manager */
    private final StateManager ivStateManager;

    private static final Class currClass = XAResourceImpl.class;

    //This xid is kept because there is a case when end and rollback may be called without an Xid.  This case
    //  is when ManagedConnection.cleanup is called and there is still a connection in a transaction open.
    private Xid ivXid;

    //used for XARecovery
    private boolean killJVMBeforeCommit = false;

    /**
     * Constructor
     *
     * @param xaRes The XAResource object obtained from the java.sql.Connection
     * @param mc The ManagedConnection instance the sqlConn and XAResource belongs to.
     */
    public XAResourceImpl(XAResource xaRes, ManagedConnectionImpl mc) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { xaRes, mc });

        ivXaRes = xaRes;
        ivMC = mc;
        ivStateManager = mc.stateMgr;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    /**
     * Commit the global transaction specified by xid.
     * <p>If the resource manager did not commit the transaction and the paramether onePhase is set to true, the
     * resource manager may throw one of the XA_RB* exceptions. Upon return, the resource manager has rolled
     * back the branch's work and has released all held resources.
     *
     * @param Xid A global transaction identifier
     * @param boolean If true, the resource manager should use a one-phase commit protocol to
     *            commit the work done on behalf of xid.
     * @exception XAException - An error has occurred. Possible XAExceptions are XA_HEURHAZ, XA_HEURCOM, XA_HEURRB,
     *                XA_HEURMIX, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     *                Possible causes for this exception are:
     *                1) the commit call on the actual XAResource failed
     *                2) the commit was called while in an invalid transaction state
     */

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "commit",
                     new Object[] {
                                    this,
                                    ivMC,
                                    xid,
                                    onePhase ? "ONE PHASE" : "TWO PHASE" });

        // @alvinso.1
        /*
         * if ( AdapterUtil.getKillServer() )
         * {
         * if (tc.isDebugEnabled())
         * Tr.debug(tc, "Try to kill server process.");
         * AdapterUtil.killServerProcess();
         * }
         */
        //This variable is ONLY used to trigger XARecovery.  Under normal circumstances, of course we want to allow
        //this .commit() method to execute, which in turn will flow the commit to the database and actually commit the transaction.
        //However, in the one single case where we are trying to create a situation in which XARecovery should take place,
        //we set this switch to 'true', which will causes us to do a System.exit() - ie, we simulate
        //a Server crash - and this should result in an 'in-doubt' transaction (because we are assuming that the TransactionManager
        //has already invoked the .end() and .prepare() methods...but we stopped the commit from happening...and end and prepare, with no commit,
        //is the definition of an in-doubt transaction)...which therefore will cause XARecovery to take place when the Server restarts.
        if (killJVMBeforeCommit) {
            System.out.println("The 'killJVMBeforeCommit' switch is set to TRUE.  Doing a System.exit() to kill the server, and generate " +
                               "an in-doubt transaction that should cause XARecovery to take place.");
            System.exit(1);
        } //end else
        else {
            //The switch is set to false, so allow the commit to happen. Since this is the flow 99.99% of the time, and we go through this
            //code path a ton, we won't clutter up the log with a trace message here.
        } //end else

        // LIDB????
        ivMC.isLazyEnlisted = false;

        //if (ivMC.isDynamicEnlistment() ) {
        //	// Reset so we can deferred enlist in a future global transaction. [d137506]
        //	ivMC.ivAlreadyProcessedInteractionPendingEvent = false;
        //}

        if (ivXid == null) { // d131094
            if (tc.isDebugEnabled())
                Tr.debug(tc, "XAResource.start was never issued; allowing commit for recovery.");

            try {
                ivStateManager.setState(StateManager.XA_RECOVER);
            } catch (TransactionException te) {
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "INVALID_TX_STATE",
                             new Object[] {
                                            "XAResource.commit()",
                                            ivMC.getTransactionStateAsString() });
                XAException xae = new XAException(XAException.XA_HEURCOM);
                traceXAException(xae, currClass);

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit", xae);
                throw xae;
            }
            ivXid = xid;
        } else if (!xid.equals(ivXid)) { // d129064.1
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "Xid does not match.",
                         new Object[] {
                                        "XAResource.start:  " + ivXid,
                                        "XAResource.commit: " + xid });

            XAException xaX = new XAException(XAException.XAER_NOTA);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", xaX);
            throw xaX;
        }

        ivXid = null;

        try {
            ivXaRes.commit(xid, onePhase);
            ivStateManager.setState(StateManager.XA_COMMIT);
        } catch (TransactionException te) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "INVALID_TX_STATE",
                         new Object[] {
                                        "XAResource.commit()",
                                        ivMC.getTransactionStateAsString() });
            XAException xae = new XAException(XAException.XA_HEURCOM);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", xae);
            throw xae;
        } catch (XAException xae) {
            traceXAException(xae, currClass);
            //112946
            if (xae.errorCode == XAException.XA_HEURCOM
                || xae.errorCode == XAException.XA_HEURHAZ
                || xae.errorCode == XAException.XA_HEURMIX
                || xae.errorCode == XAException.XA_HEURRB) {

                try {
                    ivXaRes.forget(xid);
                    ivStateManager.setState(StateManager.HEURISTIC_END);
                } catch (TransactionException te) {
                    if (tc.isDebugEnabled())
                        Tr.debug(
                                 tc,
                                 "DSA_INTERNAL_WARNING",
                                 new Object[] {
                                                "Exception setting the transaction state to WAS.database=disabled:com.ibm.ws.db2.logwriter=enabledStateManager.HEURISTIC_END from ",
                                                ivMC.getTransactionStateAsString(),
                                                te });
                    //should never happen so just eat it
                } catch (XAException eatXA) {
                    traceXAException(eatXA, currClass);
                    //eat the exception because we are throwing xae instead
                }
            } //d153590
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", xae);
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commit");
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

    @Override
    public void end(Xid xid, int flags) throws XAException {

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "end:MHD",
                     new Object[] {
                                    this,
                                    ivMC,
                                    xid,
                                    AdapterUtil.getXAResourceEndFlagString(flags) });

        if (!xid.equals(ivXid)) { // d129064.1
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "Xid does not match.",
                         new Object[] {
                                        "XAResource.start: " + ivXid,
                                        "XAResource.end:   " + xid });

            XAException xaX = new XAException(XAException.XAER_NOTA);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "end", xaX);
            throw xaX;
        }

        try {
            //121603 - only set the flag if it is not TMFAIL
            if (flags != XAResource.TMFAIL) {
                System.out.println("MHD: Setting State to end...");
                ivStateManager.setState(StateManager.XA_END);
            }
            System.out.println("MHD: Before invoking end...");
            ivXaRes.end(xid, flags);
            System.out.println("MHD: After invoking end...");

            // Set the state now if it wasn't set above. [d129064.1]
            if (flags == XAResource.TMFAIL) {
                System.out.println("MHD: XA-A...");
                ivStateManager.setState(StateManager.XA_END);
            }
        } //end if
        catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            System.out.println("MHD: Got this transaction exception....");
            te.printStackTrace();
            if (tc.isDebugEnabled()) {
                Tr.debug(
                         tc,
                         "INVALID_TX_STATE",
                         new Object[] {
                                        "XAResource.end()",
                                        ivMC.getTransactionStateAsString() });
            } //end if
            try {
                ivXaRes.rollback(xid);
            } catch (XAException eatXA) {
                traceXAException(eatXA, currClass);
                //eat the exception because we will throw the next one
            }
            XAException xae = new XAException(XAException.XA_RBROLLBACK);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "end", "Exception");
            throw xae;
        } catch (XAException xae) {
            // No FFDC code needed; this is a normal case.

            //>>d116829 - Don't log the error message if the flags = TMFAIL and
            //  the error code from the exception is XA_RBROLLBACK

            // d132821 - change errorCode check to look for range between XA_RBBASE(The inclusive lower bound of the rollback codes.)
            // and XA_RBEND(The inclusive upper bound of the rollback codes.)
            // Also eat the exception because TM is going to call rollback() anyway  @HMP

            System.out.println("MHD: Got XAException...");
            xae.printStackTrace();
            if (flags == XAResource.TMFAIL
                && ((xae.errorCode >= XAException.XA_RBBASE)
                    && (xae.errorCode <= XAException.XA_RBEND))) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(
                             tc,
                             "XAException caught on XAResource.end(TMFAIL) with errorCode of ",
                             new Integer(xae.errorCode));
                }
                //121603 - deferred setting of state because this is a normal case
                try {
                    ivStateManager.setState(StateManager.XA_END);
                } catch (TransactionException te1) {
                    if (tc.isDebugEnabled())
                        Tr.debug(
                                 tc,
                                 "DSA_INTERNAL_ERROR",
                                 new Object[] {
                                                "Error setting the state to XA_END from ",
                                                ivMC.getTransactionStateAsString(),
                                                te1 });
                }
            } else {
                //121603 - deferred setting of state because this is not a normal case
                try {
                    ivStateManager.setState(StateManager.XA_END_FAIL);
                } catch (TransactionException te1) {
                    if (tc.isDebugEnabled())
                        Tr.debug(
                                 tc,
                                 "DSA_INTERNAL_ERROR",
                                 new Object[] {
                                                "Error setting the state to XA_END_FAIL from ",
                                                ivMC.getTransactionStateAsString(),
                                                te1 });
                }
                traceXAException(xae, currClass);
                if (tc.isEntryEnabled())
                    Tr.entry(tc, "end", xae); //144722 changed to entry
                throw xae;
                // @HMP ,moved the throw here to avoid throwing if TMFAIL
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "end");
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

    @Override
    public void forget(Xid xid) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "forget",
                     new Object[] { this, ivMC, xid });

        if (ivXid == null) { // d131094
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "XAResource.start was never issued; allowing to forget for recovery.");
        }

        // For XAResource.forget, only trace an Xid mismatch. [d129064.1]
        if (!xid.equals(ivXid) && tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "Xid does not match.",
                     new Object[] {
                                    "XAResource.start:  " + ivXid,
                                    "XAResource.forget: " + xid });

        try {
            //112946
            //Note that there is no call to the physical xaresource.forget.  This is because only
            //  heuristic exceptions cause the forget to get called.  If I catch a heuristic exception
            //  I will have already called forget.
            ivStateManager.setState(StateManager.XA_FORGET);
        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "INVALID_TX_STATE",
                         new Object[] {
                                        "XAResource.forget()",
                                        ivMC.getTransactionStateAsString() });
            traceXAException(
                             new XAException(XAException.XA_RBPROTO),
                             currClass);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forget");
    }

    /**
     * @see javax.transaction.xa.XAResource#getTransactionTimeout()
     */
    @Override
    public int getTransactionTimeout() throws XAException {
        return ivXaRes.getTransactionTimeout();
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
    @Override
    public final boolean isSameRM(XAResource xaRes) throws XAException {
        boolean isSame = ivXaRes.isSameRM(xaRes);

        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "isSameRM?",
                     new Object[] { xaRes, isSame ? Boolean.TRUE : Boolean.FALSE });

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

    @Override
    public int prepare(Xid xid) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "prepare",
                     new Object[] { this, ivMC, xid });

        if (!xid.equals(ivXid)) { // d129064.1
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "Xid does not match.",
                         new Object[] {
                                        "XAResource.start:   " + ivXid,
                                        "XAResource.prepare: " + xid });

            XAException xaX = new XAException(XAException.XAER_NOTA);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepare", xaX);
            throw xaX;
        }

        int returnValue;

        try {
            returnValue = ivXaRes.prepare(xid);
            //d117074 >
            //need to check the return value here and determine if we need to clean up the transaction state
            if (tc.isDebugEnabled()) {
                Tr.debug(
                         tc,
                         "xa.prepare() status:",
                         new Object[] {
                                        AdapterUtil.getXAResourceVoteString(returnValue),
                                        xid });
            }

            if (returnValue == XAException.XA_RDONLY) {
                try {
                    ivStateManager.setState(StateManager.XA_READONLY);
                } catch (TransactionException te) {
                    //Exception means setState failed because it was invalid to set the state in this case
                    if (tc.isDebugEnabled())
                        Tr.debug(
                                 tc,
                                 "INVALID_TX_STATE",
                                 new Object[] {
                                                "XAResource.prepare()",
                                                ivMC.getTransactionStateAsString() });
                    XAException xae = new XAException(XAException.XA_RBPROTO);
                    traceXAException(xae, currClass);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "prepare", "Exception");
                    throw xae;
                }
            }
            //d117074 <

        } catch (XAException xae) {
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepare", "Exception");
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepare", new Integer(returnValue));
        return returnValue;
    }

    /**
     * Obtain a list of prepared transaction branches from a resource manager. The transaction manager
     * calls this method during recovery to obtain the list of transaction branches that are currently
     * in prepared or heuristically completed states.
     *
     * @param int flag - One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS must be used when no other flags
     *            are set in flags.
     * @return Xid[] - The resource manager returns zero or more XIDs for the transaction branches that
     *         are currently in a prepared or heuristically completed state. If an error occurs during the operation,
     *         the resource manager should throw the appropriate XAException.
     * @exception XAException - An error has occurred. Possible values are XAER_RMERR, XAER_RMFAIL, XAER_INVAL,
     *                and XAER_PROTO.
     */

    @Override
    public Xid[] recover(int flag) throws XAException {

        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "recover",
                     new Object[] {
                                    this,
                                    ivMC,
                                    AdapterUtil.getXAResourceRecoverFlagString(flag) });

        Xid[] xids = null;

        try {
            //LIDB1181.8.1
            xids = ivXaRes.recover(flag);
            if (xids.length == 0) { //d1270780
                if (tc.isEventEnabled())
                    Tr.event(
                             tc,
                             "No oustanding transactions to recover.  Transaction state does not change.");
            } else {
                ivStateManager.setState(StateManager.XA_RECOVER);
                if (tc.isEventEnabled())
                    Tr.event(
                             tc,
                             "Outstanding transactions to recover.  Transaction state is changing to "
                                 + ivMC.getTransactionStateAsString());
            }
        } catch (TransactionException te) {
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "DSA_INTERNAL_WARNING",
                         new Object[] {
                                        "Exception setting the transaction state to StateManager.XA_RECOVER from ",
                                        ivMC.getTransactionStateAsString(),
                                        te });
            //should never happen so just eat it
        } catch (XAException xae) {
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", "Exception");
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recover", xids);
        return xids;
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
    @Override
    public void rollback(Xid xid) throws XAException {
        if (tc.isEntryEnabled())
            Tr.entry(
                     tc,
                     "rollback",
                     new Object[] { this, ivMC, xid });

        // LIDB????
        ivMC.isLazyEnlisted = false;

        //if (ivMC.isDynamicEnlistment() ) {
        //  // Reset so we can deferred enlist in a future global transaction. [d137506]
        //  ivMC.ivAlreadyProcessedInteractionPendingEvent = false;
        //}

        if (ivXid == null) { // d131094
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "XAResource.start was never issued; allowing commit for recovery.");
            try {
                ivStateManager.setState(StateManager.XA_RECOVER);
            } catch (TransactionException te) {
                //Exception means setState failed because it was invalid to set the state in this case
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "INVALID_TX_STATE",
                             new Object[] {
                                            "XAResource.rollback()",
                                            ivMC.getTransactionStateAsString() });
                XAException xae = new XAException(XAException.XA_HEURRB);
                traceXAException(xae, currClass);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "rollback", xae);

                throw xae;
            }
            ivXid = xid;

        } else if (!xid.equals(ivXid)) { // d129064.1
            if (tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "Xid does not match.",
                         new Object[] {
                                        "XAResource.start:    " + ivXid,
                                        "XAResource.rollback: " + xid });

            XAException xaX = new XAException(XAException.XAER_NOTA);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", xaX);
            throw xaX;
        }

        // d127426
        // The state should be changed back whether or not the rollback throws an exception,
        // unless it's a hueristic exception.
        boolean doChangeState = true;

        // Saved exception to throw later.
        XAException throwX = null;

        try {
            System.out.println("MHD: Doing rollback on transaction **" + xid.getFormatId() + "**");
            ivXaRes.rollback(xid);
        } catch (XAException xae) {
            traceXAException(xae, currClass);
            //112946
            if (xae.errorCode == XAException.XA_HEURCOM
                || xae.errorCode == XAException.XA_HEURHAZ
                || xae.errorCode == XAException.XA_HEURMIX
                || xae.errorCode == XAException.XA_HEURRB) {
                doChangeState = false;

                try {
                    ivXaRes.forget(xid);
                    ivStateManager.setState(StateManager.HEURISTIC_END);
                } catch (TransactionException te) {
                    if (tc.isDebugEnabled())
                        Tr.debug(
                                 tc,
                                 "DSA_INTERNAL_WARNING",
                                 new Object[] {
                                                "Exception setting the transaction state to StateManager.HEURISTIC_END from ",
                                                ivMC.getTransactionStateAsString(),
                                                te });
                    //should never happen so just eat it
                } catch (XAException eatXA) {
                    traceXAException(eatXA, currClass);
                    //eat this exception because we will throw xae
                }
            } //d153590
              // Throw the exception later.
            throwX = xae;
        }

        if (doChangeState)
            try {
                ivStateManager.setState(StateManager.XA_ROLLBACK);
            } catch (TransactionException te) {
                //Exception means setState failed because it was invalid to set the state in this case
                if (tc.isDebugEnabled())
                    Tr.debug(
                             tc,
                             "INVALID_TX_STATE",
                             new Object[] {
                                            "XAResource.rollback()",
                                            ivMC.getTransactionStateAsString() });
                XAException xae = new XAException(XAException.XA_HEURRB);
                traceXAException(xae, currClass);

                if (throwX == null)
                    throwX = xae;
            }

        ivXid = null;

        if (tc.isEntryEnabled())
            if (throwX == null)
                Tr.exit(tc, "rollback");
            else
                Tr.exit(tc, "rollback", throwX);

        if (throwX != null)
            throw throwX;
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
    @Override
    public final boolean setTransactionTimeout(int seconds) throws XAException {
        if (tc.isEventEnabled())
            Tr.event(tc, "setTransactionTimeout", new Integer(seconds));

        return ivXaRes.setTransactionTimeout(seconds);
    }

    /**
     * Start work on behalf of a transaction branch specified in xid If TMJOIN is specified,
     * the start is for joining a transaction previously seen by the resource manager. If
     * TMRESUME is specified, the start is to resume a suspended transaction specified
     * in the parameter xid. If neither TMJOIN nor TMRESUME is specified and the transaction
     * specified by xid has previously been seen by the resource manager, the resource manager
     * throws the XAException exception with XAER_DUPID error code.
     *
     * @param Xid xid - A global transaction identifier to be associated with the resource
     * @param int flags - One of TMNOFLAGS, TMJOIN, or TMRESUME
     * @exception XAException - An error has occurred. Possible exceptions are XA_RB*, XAER_RMERR, XAER_RMFAIL,
     *                XAER_DUPID, XAER_OUTSIDE, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     */

    @Override
    public void start(Xid xid, int flags) throws XAException {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "start", new Object[] { this, ivMC, xid, AdapterUtil.getXAResourceStartFlagString(flags) });

        this.ivXid = xid;

        try {
            ivXaRes.start(xid, flags);
            ivStateManager.setState(StateManager.XA_START);
        } catch (TransactionException te) {
            //Exception means setState failed because it was invalid to set the state in this case
            if (tc.isDebugEnabled())
                Tr.debug(tc, "INVALID_TX_STATE", new Object[] { "XAResource.start()", ivMC.getTransactionStateAsString() });
            try {
                ivXaRes.end(xid, XAResource.TMNOFLAGS);
                ivXaRes.rollback(xid);
            } catch (XAException eatXA) {
                traceXAException(eatXA, currClass);
                //eat this exception because in the next line we will throw one
            }
            XAException xae = new XAException(XAException.XA_RBPROTO);
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "start", "Exception");
            throw xae;
        } catch (XAException xae) {
            traceXAException(xae, currClass);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "start", "Exception");
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "start");
    }

    /**
     * Only used by ManagedConnection.cleanup when it needs to close a connection that is
     * still in an active global transaction.
     *
     * @exception XAException
     */
    protected final void end() throws XAException {
        end(ivXid, XAResource.TMFAIL); // Use the xid from XAResource.start [d129064.1]
    }

    /**
     * Inform the resource manager to roll back work done on behalf of a transaction branch.
     * This method should only be used by the MC on cleanup.
     *
     * @exception XAException
     */
    protected final void rollback() throws XAException {
        rollback(ivXid); // Use the xid from XAResource.start [d129064.1]
    }

    /**
     * Method to translate the XAResource stuff, including the error code.
     *
     * @param xae
     * @param callerClass
     * @return
     */
    public static final XAException traceXAException(
                                                     XAException xae,
                                                     Class callerClass) {

        if (tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "XAException is thrown. Error code is " +
                         AdapterUtil.getXAExceptionCodeString(xae.errorCode) +
                         ", Message is " +
                         xae.getMessage());

        return xae;
    }

    public Xid getXIDThatXAResourceIsUsing() {
        return ivXid;
    }//end getXIDThatXAResourceIsUsing()

    public void setkillJVMBeforeCommitSwitch(boolean value) {
        killJVMBeforeCommit = value;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Set the .killJVMBeforeCommit switch to **" + killJVMBeforeCommit + "**");
        } //end if
    }//end setkillJVMBeforeCommitSwitch()

}
