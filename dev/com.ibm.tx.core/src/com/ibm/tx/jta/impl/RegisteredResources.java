package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 1997, 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.AbortableXAResource;
import com.ibm.tx.jta.OnePhaseXAResource;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.HeuristicHazardException;
import com.ibm.ws.Transaction.JTA.JTAResource;
import com.ibm.ws.Transaction.JTA.JTAResourceBase;
import com.ibm.ws.Transaction.JTA.JTAXAResource;
import com.ibm.ws.Transaction.JTA.ResourceSupportsOnePhaseCommit;
import com.ibm.ws.Transaction.JTA.ResourceWrapper;
import com.ibm.ws.Transaction.JTA.StatefulResource;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.JTA.XAReturnCodeHelper;
import com.ibm.ws.Transaction.test.XAFlowCallback;
import com.ibm.ws.Transaction.test.XAFlowCallbackControl;
import com.ibm.ws.recoverylog.spi.InternalLogException;
import com.ibm.ws.recoverylog.spi.LogCursor;
import com.ibm.ws.recoverylog.spi.RecoverableUnit;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;

/**
 * The RegisteredResources class provides operations that manage a list
 * of JTAResource objects involved in a transaction, and their states relative
 * to the transaction. There are 4 types of JTAResource implementation:
 * 1) OnePhaseResourceImpl - one phase only resources, eg JDBC connections
 * 2) JTAXAResourceImpl - recoverable XA resource
 * 3) CorbaResourceWrapper - wrapped OMG CosTransactions subordinate coordinator Resource
 * 4) WSCoordinatorWrapper - wrapped private distributed subordinate coordinator Resource
 * 
 * Even though an instance of this class may be accessed from multiple threads
 * within a process, there is no serialisation for thread-safety in the
 * implementation. The operation of the controlling TransactionImpl should
 * ensure that this object is not accessed by more than one thread at a time.
 * 
 * The information recorded in an instance of this class needs to be
 * reconstructible in the case of a system failure.
 */
public class RegisteredResources implements Comparator<JTAResource> {
    static final TraceComponent tc = Tr.register(RegisteredResources.class
                                                 , TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    // d159569: flag to indicate XA Flow callbacks for test enabled
    protected static final boolean xaFlowCallbackEnabled =
                    XAFlowCallbackControl.isEnabled();

    /**
     * The maximum size that the pi-data generated from this unit
     * of work can be. If the size gets beyond this value, it cannot
     * be stored within the RRS UR.
     */
    protected final static int MAX_PDATA_SIZE = 3072;

    /**
     * A list to store the XA resources for this unit of work.
     */
    protected final ArrayList<JTAResource> _resourceObjects;

    /**
     * A spot to hold the "last agent"/1PC registered resource
     */
    protected OnePhaseResourceImpl _onePhaseResourceEnlisted;

    /**
     * A list of resources that need to have the outcome or forget
     * processing retried.
     */
    protected ArrayList<JTAXAResource> _failedResourceList;

    /**
     * The Xid assigned to this transaction branch by the underlying
     * transaction service.
     * 
     * The Xid serves only as a base for future transaction identifiers.
     */
    protected Xid _txServiceXid;

    /**
     * The current branch number for this unit of work. Branch number
     * is represented in the Xid as a three byte field starting at 1 and
     * increasing by one for each unique branch of the transaction.
     * Each time the unit of work hands out a new Xid, it increments
     * this number by one.
     */
    protected int _branchCount;

    /**
     * Meta Data containing a boolean value for each application installed
     * in the server. The boolean determines whether or not LAO is
     * enabled for that application.
     */
    // private static MetaDataSlot _applicationSlotLPSEnabled;

    /**
     * Flag to indicate if any of the applications that have enlisted resources with
     * the transaction do not allow the LPS function. This flag is false until we
     * detect that one of the applications does not allow LPS at which point its set
     * to true. For a given transaction, once this flag is set to true, it can never
     * be set back to false. Also, if LPS is not available, this will be set to true.
     */
    protected boolean _LPSProhibited;

    protected boolean _LPSEnabledTx;

    /**
     * Flag to indicate if this is the first or a retry of completion
     * processing.
     */
    protected boolean _retryCompletion;

    /**
     * The JTA transaction that this <code>RegisterdResources</code> is
     * associated with.
     */
    protected TransactionImpl _transaction;

    /**
     * Lookup table of XAResource to JTAResource for 2PC resources
     */
    private HashMap<XAResource, JTAXAResource> _resourceTable;

    protected RecoverableUnit _logUnit;
    protected RecoverableUnitSection _logSection;
    protected RecoverableUnitSection _xalogSection;
    protected RecoverableUnitSection _hoSection;

    protected boolean _resourcesLogged;

    protected int _heuristicOutcome; // = StatefulResource.NONE; (everything assumes StatefulResource.NONE == 0)
    private int _loggedHeuristicOutcome;
    protected Throwable _systemException;
    protected boolean _retryRequired;
    protected boolean _outcome; // true=commit false=rollback

    // Class copy of xa exception error code for FFDC traceback
    protected int _errorCode;

    public final static int XA_OK = XAResource.XA_OK; //Defect 1451
    public final static int XA_RDONLY = XAResource.XA_RDONLY; //Defect 1451
    public final static int ONE_PHASE_OPT = 10;
    public final static int ONE_PHASE_OPT_ROLLBACK = 11;
    public final static int ONE_PHASE_OPT_FAILED = 12; //LIDB1673-13

    protected boolean _disableTwoPhase;
    protected boolean _diagnosticsRequired;

    /**
     * Flag to indicate that we have resources enlisted which specify a priority
     */
    protected boolean _gotPriorityResourcesEnlisted;

    /**
     * Flag to indicate whether we are checking that the enlisted resources are for the same
     * RM instance as the first one.
     */
    protected boolean _isCheckingSameRM;

    /**
     * If we are performing the isSameRM check, this is the JTAXAResourceImpl against which
     * the checking is performed.
     */
    protected JTAXAResourceImpl _sameRMMasterResource;

    /**
     * The second XA resource which has the same RM instance as the first JTAXAResource.
     * This XAResource is NOT in the _resourceObjects.
     */
    protected JTAXAResourceImpl _sameRMResource;

    /**
     * Flag to check if we have sorted resources based on commit priority ordering,
     * or for WAS to also exclude JMS resources until end of list.
     * We need to call sortResources just before we commit as sorting may depend on
     * deserializing recovery data.
     */
    protected boolean _sorted;

    /**
     * Indicates whether we've already waited for async resources to complete
     */
    protected boolean _retryImmediately;

    protected int _prepareResult;

    // A count of how many Resources have voted as XA_OK and not read-only or rollback.
    protected int _okVoteCount;

    /**
     * Initialises the list of RegisteredResources to be empty.
     * <p>
     * 
     * @param tran
     */
    public RegisteredResources(TransactionImpl tran, boolean disableTwoPhase) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "RegisteredResources", new Object[] { tran, disableTwoPhase });

        _resourceObjects = new ArrayList<JTAResource>();

        _transaction = tran;
        _txServiceXid = tran.getXid();
        _disableTwoPhase = disableTwoPhase;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "RegisteredResources");
    }

    /**
     * Attempts to add a one-Phase XA Resource to this unit of work.
     * 
     * @param xaRes The XAResource to add to this unit of work
     * 
     * @return true if the resource was added, false if not.
     * 
     * @throws RollbackException if enlistment fails
     * @throws SystemException if unexpected error occurs
     */
    public boolean enlistResource(XAResource xaRes) throws RollbackException, SystemException, IllegalStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "enlistResource", xaRes);

        // Determine if we are attempting to enlist a second resource within a transaction
        // that can't support 2PC anyway.
        if (_disableTwoPhase && (_resourceObjects.size() > 0)) {
            final String msg = "Unable to enlist a second resource within the transaction. Two phase support is disabled " +
                               "as the recovery log was not available at transaction start";
            final IllegalStateException ise = new IllegalStateException(msg);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlistResource", new Object[] {"(SPI)", ise});
            throw ise;
        }

        // Create the resource wrapper and see if it already exists in the table
        OnePhaseResourceImpl jtaRes = new OnePhaseResourceImpl((OnePhaseXAResource) xaRes, _txServiceXid);
        boolean register = true;

        // See if any other resource has been enlisted
        // Allow 1PC and 2PC to be enlisted, it will be rejected at prepare time if LPS is not enabled
        // Reject multiple 1PC enlistments
        if (_onePhaseResourceEnlisted != null) {
            if (_onePhaseResourceEnlisted.equals(jtaRes)) {
                register = false;
                jtaRes = _onePhaseResourceEnlisted;
            } else {
                Tr.error(tc, "WTRN0062_ILLEGAL_ENLIST_FOR_MULTIPLE_1PC_RESOURCES");
                final String msg = "Illegal attempt to enlist multiple 1PC XAResources";
                final IllegalStateException ise = new IllegalStateException(msg);
                // FFDC in TransactionImpl
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "enlistResource", new Object[] {"(SPI)", ise});
                throw ise;
            }
        }

        //
        // start association of Resource object and register if first time with JTA
        // and record that a 1PC resource has now been registered with the transaction.
        //
        try {
            this.startRes(jtaRes);

            if (register) {
                jtaRes.setResourceStatus(StatefulResource.REGISTERED);

                // This is 1PC then we need to insert at element 0
                // of the list so we can ensure it is processed last
                // at completion time.
                _resourceObjects.add(0, jtaRes);

                // Check and update LPS enablement state for the application - LIDB1673.22
                checkLPSEnablement();

                if (tc.isEventEnabled())
                    Tr.event(tc, "(SPI) RESOURCE registered with Transaction. TX: " + _transaction.getLocalTID() + ", Resource: " + jtaRes);
                _onePhaseResourceEnlisted = jtaRes;
            }
        } catch (RollbackException rbe) {
            FFDCFilter.processException(rbe, "com.ibm.tx.jta.impl.RegisteredResources.enlistResource", "480", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlistResource", rbe);
            throw rbe;
        } catch (SystemException se) {
            FFDCFilter.processException(se, "com.ibm.tx.jta.impl.RegisteredResources.enlistResource", "487", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlistResource", se);
            throw se;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "enlistResource", Boolean.TRUE);
        return true;
    }

    /**
     * Attempts to add an XA Resource to this unit of work.
     * One of two things will happen. Either the resource will
     * successfully be added to an existing XA Group, or a new XA Group
     * will be created along with a new XID branch.
     * 
     * @param xaRes The XAResource to add to this unit of work
     * @param recoveryId The recovery ID for the xaresource represented by
     *            xaRes.
     * 
     * @return true if the resource was added, false if not.
     * 
     * @throws RollbackException if enlistment fails
     * @throws SystemException if unexpected error occurs
     */
    public boolean enlistResource(XAResource xaRes, XARecoveryData recData, int branchCoupling) throws RollbackException, SystemException, IllegalStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "enlistResource", new Object[] {
                                                         xaRes,
                                                         recData,
                                                         branchCoupling });

        // Determine if we are attempting to enlist a second resource within a transaction
        // that can't support 2PC anyway.
        if (_disableTwoPhase && (_resourceObjects.size() > 0)) {
            final String msg = "Unable to enlist a second resource within the transaction. Two phase support is disabled " +
                               "as the recovery log was not available at transaction start";
            final IllegalStateException ise = new IllegalStateException(msg);
            throw ise;
        }

        boolean register = false;

        boolean matchedSameRM = false;

        JTAXAResourceImpl jtaRes = (JTAXAResourceImpl) getResourceTable().get(xaRes);
        if (jtaRes == null) {
            if (tc.isEventEnabled())
                Tr.event(tc, "enlisting XAResource");

            Xid xid;

            boolean supportsIsSameRM = recData.supportsIsSameRM();

            try {
                // This code will only be driven in the ESB scenario. There will only ever
                // be two resources enlisted that will qualify for the isSameRM optimization 
                // so we perform our check to see if this is the first resource we are enlisting
                // in the tran. If it is, it could qualify for the optimization, So we check.
                // If we have enlisted a resource before, then we check to see if the the first 
                // resource that was enlisted can perform the isSameRM check.

                if (_resourceObjects.isEmpty()) {
                    if (tc.isEventEnabled())
                        Tr.event(tc, "First resource - so we have to make a new branch xid");
                    xid = generateNewBranch();

                    // Store the recovery data reference within the resource in order that we can determine the "log early"
                    // behaviour prepare().

                    if (supportsIsSameRM) {
                        if (tc.isEventEnabled())
                            Tr.event(tc, "First resource - does implement ResourceManagerSupportsIsSameRM");

                        _isCheckingSameRM = true;
                    } else {
                        if (tc.isEventEnabled())
                            Tr.event(tc, "First resource - does not implement ResourceManagerSupportsIsSameRM");
                    }
                } else {
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Second resource or later, so we should check for isSameRM");

                    if (_isCheckingSameRM) {
                        if (xaRes.isSameRM(_sameRMMasterResource.XAResource()) &&
                            // Make sure _sameRMMasterResource also compatible with branchCoupling start flag
                            (_sameRMMasterResource.getBranchCoupling() == branchCoupling)) {
                            if (tc.isEventEnabled())
                                Tr.event(tc, "isSameRM match successful");
                            matchedSameRM = true;
                            _isCheckingSameRM = false;

                            xid = _sameRMMasterResource.getXID();
                        } else {
                            if (tc.isEventEnabled())
                                Tr.event(tc, "isSameRM match was not successful, so we create a new branch xid");

                            xid = generateNewBranch();
                        }
                    } else {
                        if (tc.isEventEnabled())
                            Tr.event(tc, "We are not matching using isSameRM so we create a new branch xid");

                        xid = generateNewBranch();
                    }
                }
            } catch (XAException xae) {
                _errorCode = xae.errorCode; // Save locally for FFDC

                throw (SystemException) new SystemException("XAResource RM instance matching error:" + XAReturnCodeHelper.convertXACode(_errorCode)).initCause(xae);
            }

            // Store the recovery data reference within the resource in order that we can determine the "log early"
            // behaviour prepare().
            jtaRes = new JTAXAResourceImpl(xid, xaRes, recData);

            // We set the state on the jta resource here so that when start is sent to it
            // we pass the neccesary flags to perfrom a TMJOIN

            if (matchedSameRM) {
                jtaRes.setState(JTAResource.NOT_ASSOCIATED_AND_TMJOIN);
            }

            // If we are enlisting the first resource to the tran and it is capable to
            // perform isSameRM, we set it as the master resource that we make our
            // isSameRM processing against.

            if (_isCheckingSameRM && (_sameRMMasterResource == null)) {
                _sameRMMasterResource = jtaRes;
            }

            jtaRes.setBranchCoupling(branchCoupling);

            register = true;
            getResourceTable().put(xaRes, jtaRes);
        }

        // Remove logEarly checks - PK66133.1
        // Always log at enlist time.  We could delay to prepare/c1p but some versions of db/2 go semi-in-doubt
        // as soon as xa_end is called. Note that the call to logRecoveryEntry will establish the identity
        // of the associated RU. This means that recData.getRecoveryId() below will return the correct RU value
        // rather than 0.       
        try {
            recData.logRecoveryEntry();
        } catch (IllegalStateException ise) // d172471
        {
            FFDCFilter.processException(ise, "com.ibm.tx.jta.impl.RegisteredResources.enlistResource", "483", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlistResource", new Object[] {"(SPI)", ise});
            throw ise;
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.tx.jta.impl.RegisteredResources.enlistResource", "489", this);
            final Throwable toThrow = new SystemException(e.getLocalizedMessage()).initCause(e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlistResource", new Object[] {"(SPI)", toThrow});
            throw (SystemException) toThrow;
        }

        //
        // start association and register JTAXAResource object with Transaction if required
        //
        try {
            setXAResourceTransactionTimeout(jtaRes, recData, _transaction.getExpirationTime());

            this.startRes(jtaRes);

            if (register) {
                jtaRes.setResourceStatus(StatefulResource.REGISTERED);

                if (matchedSameRM) {
                    _sameRMResource = jtaRes;
                } else {
                    _resourceObjects.add(jtaRes);
                    if (jtaRes.getPriority() != JTAResource.DEFAULT_COMMIT_PRIORITY)
                        _gotPriorityResourcesEnlisted = true;
                }

                // Check and update LPS enablement state for the application - LIDB1673.22
                checkLPSEnablement();

                if (tc.isEventEnabled())
                    Tr.event(tc, "(SPI) RESOURCE registered with Transaction. TX: " + _transaction.getLocalTID() + ", Resource: " + jtaRes);
            }
        } catch (RollbackException rbe) {
            FFDCFilter.processException(rbe, "com.ibm.tx.jta.impl.RegisteredResources.enlistResource", "517", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlistResource", rbe);
            throw rbe;
        } catch (SystemException se) {
            FFDCFilter.processException(se, "com.ibm.tx.jta.impl.RegisteredResources.enlistResource", "523", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlistResource", se);
            throw se;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "enlistResource", Boolean.TRUE);
        return true;
    }

    /**
     * If configured to do so, calculate the remaining time in the transaction from
     * the given expiration time and send it to the XAResource via setTransactionTimeout().
     * If this fails for any reason, we assume the RM does not support setTransactionTimeout()
     * and we disable any further attempts (unless we've explicitly configured to continue)
     * 
     * @param jtaRes
     * @param recData
     */
    protected void setXAResourceTransactionTimeout(JTAResource jtaRes, XARecoveryData recData, long expirationTime) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setXAResourceTransactionTimeout", new Object[] { jtaRes, recData, new Date(expirationTime) });

        if (recData.propagateXAResourceTransactionTimeout()) {
            final int timeout = (int) Math.ceil(((double) (expirationTime - System.currentTimeMillis())) / (double) 1000);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "XAResource.setTransactionTimeout(" + timeout + ")");

            try {
                final boolean result = jtaRes.XAResource().setTransactionTimeout(timeout);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "setXAResourceTransactionTimeout", result);
            } catch (XAException e) {
                FFDCFilter.processException(e, "com.ibm.tx.jta.RegisteredResources.setXAResourceTransactionTimeout", "710", this);
                if (!recData.continuePropagatingXAResourceTimeout()) {
                    recData.disablePropagatingXAResourceTimeout();
                }
            }
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setXAResourceTransactionTimeout");
        }
    }

    /**
     * Delist the specified resource from the transaction.
     * 
     * @param xaRes the XAResource to delist.
     * @param flag the XA flags to pass to the resource.
     *            TMSUSPEND, TMFAIL or TMSUCCESS flag to xa_end
     * 
     * @throws SystemException if the resource is not successfully
     *             disassociated from the transaction branch.
     */
    protected boolean delistResource(XAResource xaRes, int flag) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "delistResource", new Object[] { xaRes, Util.printFlag(flag) });

        // get resource manager instance
        JTAResourceBase jtaRes = (JTAResourceBase) getResourceTable().get(xaRes);
        if (jtaRes == null && _onePhaseResourceEnlisted != null) {
            if (_onePhaseResourceEnlisted.XAResource().equals(xaRes))
                jtaRes = _onePhaseResourceEnlisted;
        }

        if (jtaRes == null) {
            Tr.error(tc, "WTRN0065_XARESOURCE_NOT_KNOWN", xaRes);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "delistResource", Boolean.FALSE);
            return false;
        }

        // try to end transaction association using specified flag.
        try {
            jtaRes.end(flag);
        } catch (XAException xae) {
            _errorCode = xae.errorCode; // Save locally for FFDC
            FFDCFilter.processException(xae, "com.ibm.tx.jta.impl.RegisteredResources.delistResource", "711", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(_errorCode), xae);

            Throwable toThrow = null;

            if (_errorCode >= XAException.XA_RBBASE &&
                _errorCode <= XAException.XA_RBEND) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Transaction branch has been marked rollback-only by the RM");
            } else if (_errorCode == XAException.XAER_RMFAIL) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "RM has failed");
                // Resource has rolled back
                jtaRes.setResourceStatus(StatefulResource.ROLLEDBACK);
                jtaRes.destroy();
            } else // XAER_RMERR, XAER_INVAL, XAER_PROTO, XAER_NOTA
            {
                Tr.error(tc, "WTRN0079_END_FAILED", new Object[] { XAReturnCodeHelper.convertXACode(_errorCode), xae });
                toThrow = new SystemException("XAResource end association error:" + XAReturnCodeHelper.convertXACode(_errorCode)).initCause(xae);
            }

            // Mark transaction as rollback only.
            try {
                _transaction.setRollbackOnly();
                if (tc.isEventEnabled())
                    Tr.event(tc, "Transaction marked as rollback only.");
            } catch (IllegalStateException e) {
                FFDCFilter.processException(e, "com.ibm.tx.jta.impl.RegisteredResources.delistResource", "742", this);
                toThrow = new SystemException(e.getLocalizedMessage()).initCause(e);
            }

            if (toThrow != null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "delistResource", toThrow);
                throw (SystemException) toThrow;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "delistResource", Boolean.TRUE);
        return true;
    }

    /**
     * Generates a new XidImpl to represent a new branch of this
     * transaction.
     * 
     * @return A new XidImpl representing a new branch of this transaction.
     */
    protected Xid generateNewBranch() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "generateNewBranch");

        // Create a new Xid branch
        final XidImpl result = new XidImpl(_txServiceXid, ++_branchCount);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "generateNewBranch", result);
        return result;
    }

    protected HashMap<XAResource, JTAXAResource> getResourceTable() {
        if (_resourceTable == null) {
            _resourceTable = new HashMap<XAResource, JTAXAResource>();
        }
        return _resourceTable;
    }

    /**
     * Determine if LPS is enabled for this enlist (ie this application) and
     * update the overall transaction LPS availability. The class instance
     * variable _LPSProhibited which defines the overall LPS availability is
     * set to true as soon as possible to avoid further MetaData lookups unless
     * debug trace is enabled.
     */
    protected void checkLPSEnablement() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "checkLPSEnablement");

        // If this is in a tran enabled for LPS via beginLPSEnabledTx return without
        // checking for LPS enablement
        if (_LPSEnabledTx) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "checkLPSEnablement - LPSEnabledTx");
            return;
        }

        final boolean debug = tc.isDebugEnabled();

        // If we are not debugging and LPS is disabled, return immediately.
        if (!debug) {
            if (_LPSProhibited) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "checkLPSEnablement");
                return;
            }
        }

        final boolean isLPSEnabled = _transaction._configProvider.getRuntimeMetaDataProvider().isHeuristicHazardAccepted();

        if (debug)
            Tr.debug(tc, "LPSEnabled", isLPSEnabled);

        // If this application does not allow the LPS function then ensure that we record
        // this fact. This flag will remain false until the first application that does
        // not allow LPS drives an enlist. At this point it will be set to true and is
        // not changed back to false.
        if (!isLPSEnabled) {
            _LPSProhibited = true;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "checkLPSEnablement : LPSEnabled=" + isLPSEnabled + " LPSProhibited=" + _LPSProhibited);
    }

    protected void reconstructHeuristics(RecoverableUnit log) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "reconstructHeuristics", log);

        // Reconstruct our state from RecoverableUnit object.
        _hoSection = log.lookupSection(TransactionImpl.HEURISTIC_OUTCOME_SECTION);
        if (_hoSection != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Recovering heuristic outcome");

            try {
                final byte[] logData = _hoSection.lastData();
                if (logData.length == 1) {
                    final int ho = logData[0] & 0xff;
                    // Set the state value to be returned from the reconstruct method

                    setHeuristicOutcome(ho);
                    _loggedHeuristicOutcome = _heuristicOutcome;
                } else {
                    // If the log record data is invalid, then exit immediately.
                    throw new SystemException("Invalid heuristic outcome record data in log");
                }
            } catch (Throwable e) {
                FFDCFilter.processException(e, "com.ibm.tx.jta.impl.RegisteredResources.reconstructHeuristics", "764", this);
                Tr.fatal(tc, "WTRN0000_ERR_INT_ERROR", new Object[] { "reconstructHeuristics", "com.ibm.tx.jta.impl.RegisteredResources", e });
                if (tc.isEventEnabled())
                    Tr.event(tc, "Unable to access heuristic outcome log record data");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "reconstructHeuristics", e);
                throw (SystemException) new SystemException(e.toString()).initCause(e);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "reconstructHeuristics");
        return;
    }

    /**
     * Directs the RegisteredResources to recover its state after a failure.
     * <p>
     * This is based on the given RecoverableUnit object. The participant list is reconstructed.
     * 
     * @param log The RecoverableUnit holding the RegisteredResources state.
     */
    public void reconstruct(RecoveryManager recoveryManager, RecoverableUnit log) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "reconstruct", new Object[] { recoveryManager, log });

        _retryCompletion = true;

        reconstructHeuristics(log);

        // Read in XAResources and Corba resources (subordinate coords) from the log
        // We save the sections and logUnit although they are never needed again..

        // Get section id for XAResources registered as part of transaction
        _xalogSection = log.lookupSection(TransactionImpl.XARESOURCE_SECTION);
        if (_xalogSection != null) // We have some resources to recover
        {
            final byte[] tid = _transaction.getXidImpl().toBytes();

            LogCursor logData = null;
            try {
                logData = _xalogSection.data();
                while (logData.hasNext()) {
                    final byte[] data = (byte[]) logData.next();
                    try {
                        final JTAXAResourceImpl res = new JTAXAResourceImpl(recoveryManager.getPartnerLogTable(), tid, data);
                        res.setResourceStatus(StatefulResource.PREPARED);
                        _resourceObjects.add(res);
                        if (res.getPriority() != JTAResource.DEFAULT_COMMIT_PRIORITY)
                            _gotPriorityResourcesEnlisted = true;
                    } catch (Throwable exc) {
                        FFDCFilter.processException(exc, "com.ibm.tx.jta.impl.RegisteredResources.reconstruct", "843", this);
                        Tr.error(tc, "WTRN0045_CANNOT_RECOVER_RESOURCE",
                                 new Object[] { Util.toHexString(data), exc });
                        throw exc;
                    }
                }
                logData.close();
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.tx.jta.impl.RegisteredResources.reconstruct", "853", this);
                Tr.fatal(tc, "WTRN0000_ERR_INT_ERROR", new Object[] { "reconstruct", "com.ibm.tx.jta.impl.RegisteredResources", exc });
                if (logData != null)
                    logData.close();
                if (tc.isEventEnabled())
                    Tr.event(tc, "Exception raised reconstructing XA resource");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "reconstruct");
                throw (SystemException) new SystemException(exc.toString()).initCause(exc);
            }
        }

        _logUnit = log;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "reconstruct");
    }

    /**
     * Adds a reference to a Resource object to the list in the registered state.
     * 
     * This is intended to be used for registering Remote objects which do not need any start association.
     * 
     * @param resource
     * 
     * @return
     */
    protected void addRes(JTAResource resource) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addRes", new Object[] { this, resource });

        // Add the reference to the list (which was created when this object was
        // created), with the "registered" status.
        resource.setResourceStatus(StatefulResource.REGISTERED);
        _resourceObjects.add(resource);

        if (tc.isEventEnabled())
            Tr.event(tc, "(SPI) SERVER registered with Transaction. TX: " + _transaction.getLocalTID() + ", Resource: " + resource);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "addRes");
    }

    /**
     * Starts association of the resource with the current transaction and if required
     * adds a reference to a Resource object to the list in the registered state.
     * 
     * This method is intended to be used for registering local resources.
     * 
     * @param resource
     * 
     * @return
     */
    protected void startRes(JTAResource resource) throws RollbackException, SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "startRes", new Object[] { this, resource });

        try {
            resource.start();
        } catch (XAException xae) {
            _errorCode = xae.errorCode; // Save locally for FFDC
            FFDCFilter.processException(xae, "com.ibm.tx.jta.impl.RegisteredResources.startRes", "1053", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(_errorCode), xae);

            final Throwable toThrow;
            //
            // the XAResource object is doing work outside global
            // transaction. This means that XAResource is doing local
            // transaction. Local transaction has to be completed before
            // start global transaction or XA transaction.
            //
            if (_errorCode == XAException.XAER_OUTSIDE) {
                toThrow = new RollbackException("XAResource working outside transaction").initCause(xae);
                if (tc.isEventEnabled())
                    Tr.event(tc, "XAResource is doing work outside of the transaction.", toThrow);
                throw (RollbackException) toThrow;
            } else if (_errorCode >= XAException.XA_RBBASE &&
                       _errorCode <= XAException.XA_RBEND) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Transaction branch has been marked rollback-only by the RM");
                //
                // Transaction branch has been rolled back, so we
                // mark transaction as rollback only.
                //
                try {
                    _transaction.setRollbackOnly();
                } catch (IllegalStateException e) {
                    FFDCFilter.processException(e, "com.ibm.tx.jta.impl.RegisteredResources.startRes", "1085", this);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Exception caught marking Transaction rollback only", e);
                    throw (SystemException) new SystemException(e.getLocalizedMessage()).initCause(e);
                }

                toThrow = new RollbackException("Transaction has been marked as rollback only.").initCause(xae);
                if (tc.isEventEnabled())
                    Tr.event(tc, "Marked transaction as rollback only.", toThrow);
                throw (RollbackException) toThrow;
            }
            //
            // Any other error is a protocol violation or the RM is broken, and we throw
            // SystemException.
            //
            else // XAER_RMERR, XAER_RMFAIL, XAER_INVAL, XAER_PROTO, XAER_DUPID
            {
                Tr.error(tc, "WTRN0078_START_FAILED", new Object[] { XAReturnCodeHelper.convertXACode(_errorCode), xae });
                throw (SystemException) new SystemException("XAResource start association error:" + XAReturnCodeHelper.convertXACode(_errorCode)).initCause(xae);
            }
        } finally {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "startRes");
        }
    }

    /**
     * Returns the number of Resources currently in the list.
     * 
     * @return The number of registered Resources.
     */
    public int numRegistered() {
        final int result = _resourceObjects.size();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "numRegistered", result);
        return result;
    }

    /**
     * Send end to all registered resources
     * 
     * @param flags
     * 
     * @return whether we managed to successfully end all the resources
     */
    public boolean distributeEnd(int flags) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "distributeEnd", Util.printFlag(flags));

        boolean result = true;

        for (int i = _resourceObjects.size(); --i >= 0;) {
            final JTAResource resource = _resourceObjects.get(i);

            if (!sendEnd(resource, flags)) {
                result = false;
            }
        }

        if (_sameRMResource != null) {
            if (!sendEnd(_sameRMResource, flags)) {
                result = false;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "distributeEnd", result);
        return result;
    }

    boolean sendEnd(JTAResource resource, int flags) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "sendEnd", new Object[] { resource, flags, this });

        try {
            resource.end(flags);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "sendEnd", Boolean.TRUE);
            return true;
        } catch (XAException xae) {
            // Only FFDC non-rollback cases
            _errorCode = xae.errorCode;
            if (_errorCode >= XAException.XA_RBBASE && _errorCode <= XAException.XA_RBEND) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "XA_RB* from end", xae);
            } else {
                FFDCFilter.processException(xae, "com.ibm.tx.jta.impl.RegisteredResources.sendEnd", "1157", this);
                if (tc.isEventEnabled())
                    Tr.event(tc, "XAException (not XA_RB*) from end", xae);

                // Set the resource's state to completed.
                // It has already rolledback so we must not contact it during rollback processing.
                resource.setResourceStatus(StatefulResource.ROLLEDBACK);
                resource.destroy();
            }

            // We are going to rollback so squirrel away this exception for later use as the cause for the RollbackException
            _transaction.setOriginalException(xae);
        } catch (Throwable t) {
            FFDCFilter.processException(t, "com.ibm.tx.jta.impl.RegisteredResources.sendEnd", "1171", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "Throwable (not XAException) from end", t);

            // We are going to rollback so squirrel away this exception for later use as the cause for the RollbackException
            _transaction.setOriginalException(t);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "sendEnd", Boolean.FALSE);
        return false;
    }

    protected int prepareResource(JTAResource currResource) throws RollbackException, SystemException,
                    HeuristicMixedException, HeuristicHazardException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "prepareResource", new Object[] { this, currResource });

        int currResult = XAResource.XA_RDONLY;

        try {
            boolean informResource = true;

            if (xaFlowCallbackEnabled) {
                informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.PREPARE, XAFlowCallback.PREPARE_NORMAL);
            }

            if (informResource) {
                currResult = currResource.prepare();
            }

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(XAFlowCallback.PREPARE, XAFlowCallback.AFTER_SUCCESS);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareResource", XAReturnCodeHelper.convertXACode(currResult));
            return currResult;
        } catch (XAException xae) {
            _errorCode = xae.errorCode; // Save locally for FFDC
            FFDCFilter.processException(xae, "com.ibm.tx.jta.impl.RegisteredResources.prepareResource", "1216", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(_errorCode), xae);

            final Throwable toThrow;

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(XAFlowCallback.PREPARE, XAFlowCallback.AFTER_FAIL);
            }

            if ((_errorCode >= XAException.XA_RBBASE && _errorCode <= XAException.XA_RBEND) || _errorCode == XAException.XAER_NOTA) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "XA_RB* or XAER_NOTA on prepare. Marking resource as complete. Rollback tx");

                // Either the resource manager has rolled back the resource, or it knew nothing
                // about this transaction in which case we can assume that it has already
                // rolled it back.

                // Set the resource's state to completed.
                // It has already rolledback so we must not contact it during rollback processing.
                currResource.setResourceStatus(StatefulResource.ROLLEDBACK);
                currResource.destroy();

                toThrow = new RollbackException().initCause(xae);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "prepareResource", toThrow);
                throw (RollbackException) toThrow;
            } else if (_errorCode == XAException.XA_HEURMIX) {
                // Record that this resource has completed with heuristics
                currResource.setResourceStatus(StatefulResource.HEURISTIC_MIXED);
                updateHeuristicOutcome(StatefulResource.HEURISTIC_MIXED);

                // report if appropriate
                _diagnosticsRequired = true;

                toThrow = new HeuristicMixedException().initCause(xae);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "prepareResource", toThrow);
                throw (HeuristicMixedException) toThrow;
            } else if (_errorCode == XAException.XA_HEURHAZ) {
                // Record that this resource has completed with heuristics
                currResource.setResourceStatus(StatefulResource.HEURISTIC_HAZARD);
                updateHeuristicOutcome(StatefulResource.HEURISTIC_HAZARD);

                // report if appropriate
                _diagnosticsRequired = true;

                toThrow = new HeuristicHazardException().initCause(xae);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "prepareResource", toThrow);
                throw (HeuristicHazardException) toThrow;
            }

            // XAER_RMFAIL, XAER_RMERR, XAER_INVAL, XAER_PROTO

            logRmfailOnPreparing(xae);

            // Following a RMFAIL or RMERR on a prepare flow we do not know whether or not the
            // resource was successfully prepared.   If we receive an INVAL or PROTO then
            // something's gone wrong with our internal logic or with the resource manager.
            // As a result of this we must rollback the entire transaction.
            // Any other XA errors are entirely unexpected and we must roll the transaction back.

            // We do not change the resource's status to completed as it needs to be rolledback.
            if ((_errorCode == XAException.XAER_RMERR) || (_errorCode == XAException.XAER_RMFAIL)) {
                currResource.setResourceStatus(StatefulResource.PREPARED);
                currResource.setState(JTAResource.FAILED);
            }

            // Treat RMFAIL just as a rollback
            if (_errorCode == XAException.XAER_RMFAIL) {
                toThrow = new RollbackException().initCause(xae);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "prepareResource", toThrow);
                throw (RollbackException) toThrow;
            }

            // Throw a SystemException to indicate that this is unexpected rather than a rollback
            // vote from the prepare flow.
            toThrow = new SystemException().initCause(xae);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareResource", toThrow);
            throw (SystemException) toThrow;
        } catch (Throwable exc) {
            // Treat like RMERR
            FFDCFilter.processException(exc, "com.ibm.tx.jta.impl.RegisteredResources.prepareResource", "1297", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RuntimeException", exc);

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(XAFlowCallback.PREPARE,
                                                  XAFlowCallback.AFTER_FAIL);
            }

            _diagnosticsRequired = true;
            Tr.error(tc, "WTRN0046_PREPARE_FAILED", new Object[] { exc.getLocalizedMessage(), exc });

            // We do not change the resource's status to completed as it needs to be rolledback.
            currResource.setResourceStatus(StatefulResource.PREPARED);

            // Throw a SystemException to indicate that this is unexpected rather than a rollback
            // vote from the prepare flow.
            final Throwable toThrow = new SystemException(exc.toString()).initCause(exc);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepareResource", toThrow);
            throw (SystemException) toThrow;
        }
    }

    /**
     * Distributes prepare messages to all Resources in the registered state.
     * <p>
     * Resource objects that vote to commit the transaction are added to the
     * RegisteredResources section in the CoordinatorLog.
     * <p>
     * All Resources that return VoteReadOnly have their state set to completed.
     * The consolidated result is returned.
     * <P>
     * If all but one Resources return VoteReadOnly then the
     * remaining Resource is completed via CommitOnePhase and
     * TMONEPHASE is returned.
     * 
     * @return The vote for the transaction.
     *         <UL>
     *         <LI>XA_OK - Transaction is ok to Commit</LI>
     *         <LI>XA_RDONLY - Transaction participants were Read-Only</LI>
     *         <LI>ONE_PHASE_OPT - Single non read-only Resource has been committed as
     *         part of 1PC optimization</LI>
     *         <LI>ONE_PHASE_OPT_ROLLBACK - 1PC optimization was attempted on only non
     *         read-only Resource but it rolled back</LI>
     *         <LI>ONE_PHASE_OPT_FAILED - need to consult lpsHeuristicCompletion flag</LI>
     *         </UL>
     * @param subordinate true indicates this is a subordinate branch
     * @param optimise indicate whether optimisations are allowed
     * @exception RollbackException
     *                Thrown if a participant has voted to Rollback the transaction
     * @exception HeuristicMixedException
     *                Thrown if one phase optimisation results in heuristic
     * @exception SystemException
     *                Thrown if an unknown error occurs
     */
    public int distributePrepare(boolean subordinate, boolean optimise)
                    throws RollbackException, SystemException, HeuristicMixedException,
                    HeuristicHazardException, HeuristicRollbackException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "distributePrepare", new Object[] { this, subordinate, optimise });

        _diagnosticsRequired = false;

        _prepareResult = XA_RDONLY;

        try {
            //
            // LIDB1673.22/PQ72718
            // If we are a subordinate and we have a OnePhaseResource enlisted, then we cannot prepare
            // the 1PC resource, so rollback the transaction.  Note: if we are a "real" subordinate but
            // have received a C1P from the superior, the subordinate flag is set to false, ie we appear
            // as a superior for voting and logging as the superior has handed overall responsibility.
            //
            if (subordinate && (_onePhaseResourceEnlisted != null)) {
                Tr.error(tc, "WTRN0064_SUBORDINATE_COMMIT_OF_1PC_RESOURCE");

                if (tc.isEventEnabled())
                    Tr.event(tc, "Subordinate contains 1PC resource, rolling back transaction");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "distributePrepare", "RollbackException");
                throw new RollbackException();
            }

            final long startTime = System.currentTimeMillis();

            // Prepare the async resources if needed
            // Two calls are needed before and after preparing resources to enable ordering if required
            if (gotAsyncResources()) {
                prePreparePrepareAsyncResources();
                prePrepareGetAsyncPrepareResults(startTime);
            }

            // If we have priority defined resources, sort the list prior to prepare
            if (_gotPriorityResourcesEnlisted) {
                // Take the one phase resource out of the list while we sort
                if (_onePhaseResourceEnlisted != null)
                    _resourceObjects.remove(0);
                sortPreparePriorityResources();
                if (_onePhaseResourceEnlisted != null)
                    _resourceObjects.add(0, _onePhaseResourceEnlisted);
            }

            // Browse through the participants, preparing them, and obtain a consolidated
            // result.  We should stop after the first rollback vote.  If there is only a
            // single resource that remains we issue a commit_one_phase.  If only one resource
            // votes commit, commit is issued immediately with no logging.
            // If there are no Resource references, return the read-only vote.
            for (int i = _resourceObjects.size(); --i >= 0;) {
                final JTAResource currResource = _resourceObjects.get(i);

                if ((i == 0)
                    && (_okVoteCount == 0)
                    && (optimise) // (!subordinate)
                    && !gotAsyncResources()
                    && (currResource instanceof ResourceSupportsOnePhaseCommit)
                    && (_onePhaseResourceEnlisted == null)) {
                    // This is the last resource to be processed (i==0), no other resources have voted to commit
                    // (okVoteCount==0), this node is in control of the transaction (!subordinate) and the resource
                    // supports processing of a one phase commit flow (instance of RSOPC). Under these conditions,
                    // we can drive a 1PC flow to the resource without any need to LPS logging.

                    try {
                        flowCommitOnePhase(false);

                        //
                        // Set return value to ONEPHASE so that the TransactionImpl knows
                        // we have optimized.
                        //
                        _prepareResult = ONE_PHASE_OPT;
                    } catch (RollbackException rbe) {
                        // No FFDC Code Needed.

                        // We must intercept a RollbackException and convert it into a ONE_PHASE_OPT_ROLLBACK
                        // return so that TransactionImpl knows that the only resource not to vote read only
                        // has been rolledback so there is no need to distribute rollback.
                        _prepareResult = ONE_PHASE_OPT_ROLLBACK;
                    } finally {
                        // Heuristic cases are thrown to caller
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "distributePrepare",
                                    (_prepareResult == ONE_PHASE_OPT ? "ONE_PHASE_OPT" :
                                                    (_prepareResult == ONE_PHASE_OPT_ROLLBACK ? "ONE_PHASE_OPT_ROLLBACK" : "HEURISTIC")));
                    }

                    return _prepareResult;
                } else if ((i == 0) && (_onePhaseResourceEnlisted != null) && (!subordinate)) {
                    // Process OnePhaseResource later by call to commitLastAgent from TransactionImpl
                } else {
                    final int currResult = prepareResource(currResource);

                    // Take an action depending on the participant's vote.
                    if (currResult == XAResource.XA_OK) {
                        //
                        // Update the resource state to prepared.
                        //
                        currResource.setResourceStatus(StatefulResource.PREPARED);

                        if (_prepareResult == XA_RDONLY) {
                            _prepareResult = XA_OK;
                        }

                        _okVoteCount++;
                    } else {
                        //
                        // Set the state of a participant that votes read-only to completed as it
                        // replies.  The consolidated vote does not change.
                        //
                        currResource.setResourceStatus(StatefulResource.COMPLETED);
                    }

                    // Check if the transaction has been marked rollback by operator interface
                    if (_transaction.getRollbackOnly()) {
                        final Throwable toThrow = new RollbackException();
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "distributePrepare", toThrow);
                        throw (RollbackException) toThrow;
                    }
                }
            } // end for..

            // Get the asynchronous results
            if (gotAsyncResources()) {
                postPreparePrepareAsyncResources();
                postPrepareGetAsyncPrepareResults(startTime);
            }

            // Have we only had one XA_OK vote? If so we can commit the single resource without
            // writing any prepare information to the recovery log.

            if ((_okVoteCount == 1) && optimise && _onePhaseResourceEnlisted == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Transaction contains one non-READ-ONLY Resource, attempting to Optimize.");
                _prepareResult = ONE_PHASE_OPT;
                distributeCommit();
            } else if (_okVoteCount > 0) {
                // We have more than one prepared Resource so we can't optimize
                // and must write to the recovery log as usual.  (Distributed Only)
                logResources();

                // Sort the resources now for z/OS logging.  Note this will update
                // resource priorities for commitLastInPhase cases.
                // Take the one phase resource out of the list while we sort
                // as on z/OS we have not yet called commitlastAgent.
                if (_onePhaseResourceEnlisted != null)
                    _resourceObjects.remove(0);
                sortResources();
                if (_onePhaseResourceEnlisted != null)
                    _resourceObjects.add(0, _onePhaseResourceEnlisted);
            }
        } finally {
            if (_diagnosticsRequired) {
                logDiagnostics(JTAResourceBase.PREPARE_DIAGNOSTICS);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "distributePrepare", XAReturnCodeHelper.convertXACode(_prepareResult));
        return _prepareResult;
    }

    protected boolean gotAsyncResources() {
        return false;
    }

    protected void prePrepareGetAsyncPrepareResults(long startTime) throws HeuristicHazardException, RollbackException, SystemException, HeuristicMixedException {
        // Not used in JTM
    }

    protected void postPrepareGetAsyncPrepareResults(long startTime) throws HeuristicHazardException, RollbackException, SystemException, HeuristicMixedException {
        // Not used in JTM
    }

    protected void prePreparePrepareAsyncResources() throws SystemException, RollbackException {
        // Not used in JTM
    }

    protected void postPreparePrepareAsyncResources() throws SystemException, RollbackException {
        // Not used in JTM
    }

    /**
     * Log any prepared resources
     */
    protected void logResources() throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "logResources", _resourcesLogged);

        if (!_resourcesLogged) {
            for (int i = 0; i < _resourceObjects.size(); i++) {
                final JTAResource resource = _resourceObjects.get(i);

                if (resource.getResourceStatus() == StatefulResource.PREPARED) {
                    recordLog(resource);
                }
            }

            _resourcesLogged = true;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "logResources");
    }

    /**
     * Deliver commit or rollback to a resource - for sync based
     * resources, this will perform the request and process the
     * response. For async based resources, the request has already
     * been performed, and only the response is processed here.
     * 
     * @return boolean value to indicate whether retries are necessary.
     */
    protected boolean deliverOutcome(JTAResource currResource) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deliverOutcome", currResource);

        boolean retryRequired = false; // indicates whether retry necessary
        boolean informResource = true;
        int flowType = -1;
        boolean preparedResource = true;
        boolean auditing = false;

        try {
            switch (currResource.getResourceStatus()) {
                case StatefulResource.PREPARED:
                    currResource.setResourceStatus(StatefulResource.COMPLETING);
                    // NB. no break
                case StatefulResource.COMPLETING: // retry case
                    auditing = _transaction.auditSendCompletion(currResource, _outcome);
                    if (_outcome) {
                        if (xaFlowCallbackEnabled) {
                            informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.COMMIT, XAFlowCallback.COMMIT_2PC);
                            flowType = XAFlowCallback.COMMIT;
                        }

                        if (informResource) {
                            currResource.commit();
                        }

                        currResource.setResourceStatus(StatefulResource.COMMITTED);
                    } else {
                        if (xaFlowCallbackEnabled) {
                            informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.ROLLBACK, XAFlowCallback.ROLLBACK_NORMAL);
                            flowType = XAFlowCallback.ROLLBACK;
                        }

                        if (informResource) {
                            currResource.rollback();
                        }

                        currResource.setResourceStatus(StatefulResource.ROLLEDBACK);
                    }
                    if (auditing)
                        _transaction.auditCompletionResponse(XAResource.XA_OK, currResource, _outcome);

                    break;

                case StatefulResource.COMPLETING_ONE_PHASE: // retry remote Corba resource only
                    preparedResource = false;
                    if (_outcome) {
                        if (xaFlowCallbackEnabled) {
                            informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.COMMIT, XAFlowCallback.COMMIT_1PC_OPT);
                            flowType = XAFlowCallback.COMMIT;
                        }

                        if (informResource) {
                            currResource.commit_one_phase();
                        }

                        currResource.setResourceStatus(StatefulResource.COMMITTED); // d396035
                    } else {
                        if (xaFlowCallbackEnabled) {
                            informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.ROLLBACK, XAFlowCallback.ROLLBACK_NORMAL);
                            flowType = XAFlowCallback.ROLLBACK;
                        }

                        if (informResource) {
                            currResource.rollback();
                        }

                        currResource.setResourceStatus(StatefulResource.ROLLEDBACK);
                    }

                    break;

                case StatefulResource.REGISTERED:
                    preparedResource = false;
                    if (!_outcome) // else error?
                    {
                        currResource.setResourceStatus(StatefulResource.COMPLETING);

                        if (xaFlowCallbackEnabled) {
                            informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.ROLLBACK, XAFlowCallback.ROLLBACK_NORMAL);
                            flowType = XAFlowCallback.ROLLBACK;
                        }

                        if (informResource) {
                            currResource.rollback();
                        }

                        currResource.setResourceStatus(StatefulResource.ROLLEDBACK);
                    }

                    break;

                default: // do nothing
                    break;

            } // end switch

            if (flowType != -1) {
                if (xaFlowCallbackEnabled) {
                    XAFlowCallbackControl.afterXAFlow(flowType, XAFlowCallback.AFTER_SUCCESS);
                }
            }
        } catch (XAException xae) {
            _errorCode = xae.errorCode; // Save locally for FFDC
            FFDCFilter.processException(xae, "com.ibm.tx.jta.impl.RegisteredResources.deliverOutcome", "1923", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(_errorCode), xae);
            if (auditing)
                _transaction.auditCompletionResponse(_errorCode, currResource, _outcome);

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(flowType, XAFlowCallback.AFTER_FAIL);
            }

            if (_errorCode == XAException.XA_HEURRB) {
                // Record that this resource has completed with heuristics
                currResource.setResourceStatus(StatefulResource.HEURISTIC_ROLLBACK);

                // report if appropriate
                if (_outcome && !auditing) {
                    _diagnosticsRequired = true;
                    Tr.error(tc, "WTRN0075_HEURISTIC_ON_COMMIT", _transaction.getTranName());
                }

            } else if (_errorCode == XAException.XA_HEURCOM) {
                // Record that this resource has completed with heuristics
                currResource.setResourceStatus(StatefulResource.HEURISTIC_COMMIT);

                // report if appropriate
                if (!_outcome && !auditing) {
                    _diagnosticsRequired = true;
                    Tr.error(tc, "WTRN0076_HEURISTIC_ON_ROLLBACK", _transaction.getTranName());
                }

            } else if (_errorCode == XAException.XA_HEURMIX) {
                // Record that this resource has completed with heuristics
                currResource.setResourceStatus(StatefulResource.HEURISTIC_MIXED);

                // report if appropriate
                if (!auditing) {
                    _diagnosticsRequired = true;
                    if (_outcome) {
                        Tr.error(tc, "WTRN0075_HEURISTIC_ON_COMMIT", _transaction.getTranName());
                    } else {
                        Tr.error(tc, "WTRN0076_HEURISTIC_ON_ROLLBACK", _transaction.getTranName());
                    }
                }

            } else if (_errorCode == XAException.XA_HEURHAZ) {
                // Record that this resource has completed with heuristics
                currResource.setResourceStatus(StatefulResource.HEURISTIC_HAZARD);

                // report if appropriate
                if (!auditing) {
                    _diagnosticsRequired = true;
                    if (_outcome) {
                        Tr.error(tc, "WTRN0075_HEURISTIC_ON_COMMIT", _transaction.getTranName());
                    } else {
                        Tr.error(tc, "WTRN0076_HEURISTIC_ON_ROLLBACK", _transaction.getTranName());
                    }
                }
            } else if (_errorCode == XAException.XAER_RMERR) {
                //
                // According to XA, XAER_RMERR occured in committing the
                // work performed on behalf of the transaction branch and
                // the branch's work has been rolled back. Note that this
                // error signals a catatrophic event to the TM since other
                // resource managers may successfully commit their work.
                // This error should be returned only when a resource manager
                // concludes that it can never commit the branch and that it
                // can NOT hold the branch's resources in a prepared state.
                // system administrator's manual intervention is necessary.
                //
                updateHeuristicOutcome(StatefulResource.HEURISTIC_ROLLBACK);
                currResource.setResourceStatus(StatefulResource.ROLLEDBACK);
                currResource.destroy();

                if (_outcome && !auditing) {
                    _diagnosticsRequired = true;
                    Tr.error(tc, "WTRN0047_XAER_RMERR_ON_COMMIT", currResource);
                }
            } else if (_errorCode == XAException.XAER_RMFAIL) {
                // If we are rolling back and have not prepared, then no need to retry, we are done
                if (!_outcome && !preparedResource) {
                    currResource.setResourceStatus(StatefulResource.ROLLEDBACK);
                    currResource.destroy();
                } else // we're prepared and/or its a Corba/WSCoord resource
                {
                    if (!auditing)
                        logRmfailOnCompleting(currResource, xae);

                    // Set the resource's state to failed so that
                    // we will attempt to reconnect to the resource
                    // manager upon retrying.
                    currResource.setState(JTAResource.FAILED);
                    updateHeuristicOutcome(StatefulResource.HEURISTIC_HAZARD);

                    // Retry the commit/rollback flow
                    addToFailedResources(currResource);
                    retryRequired = true;
                }
            } else if (_errorCode == XAException.XA_RETRY) {
                // Either we are retrying during recovery when trying to access the RM, or
                // the resource manager has ran out of steam and we need to try again later.
                // We do not need to set the resource to failed as it is still useable.
                // This latter error only occurs on a 2PC commit.  Retry the flow

                // We will set the resouce to failed so that after_completion
                // can be driven and retry will get a new connection
                // ************************************************************************
                // NOTE XA SPEC defines this on xa_commit only.  However, RRA will send  // @D6506080A
                // XA_RETRY on commit and rollback as part of Oracle RAC recovery retries.
                // The support for this in RRA also requires us to ensure that we set the
                // resource to failed and drive reconnectRM to get a new XAResource on retry.
                // ************************************************************************
                currResource.setState(JTAResource.FAILED);
                updateHeuristicOutcome(StatefulResource.HEURISTIC_HAZARD);
                addToFailedResources(currResource);
                retryRequired = true;
            } else if (_errorCode == XAException.XAER_NOTA) {
                if (currResource.getResourceStatus() == StatefulResource.COMPLETING_ONE_PHASE) {
                    // NOTA returned from a single Corba resource retrying commit_one_phase
                    // We have no idea what the outcome was. The remote resource may have received
                    // the c1p and committed or rolledback or the remote server may have died and
                    // rolled back the transaction.  Need to return heuristic.
                    // Note: local resources are not retried for commit_one_phase.)
                    updateHeuristicOutcome(StatefulResource.HEURISTIC_HAZARD);
                    // We do not need to forget as the remote resource has already gone away
                    // so just clean up and return...
                } else if (_outcome && !_retryCompletion) {
                    // NOTA returned on an initial commit after a prepare.
                    // We should never get this unless someone has heuristically forgotten the
                    // branch.  Return hazard as we dont know if it committed or rolled back.
                    updateHeuristicOutcome(StatefulResource.HEURISTIC_HAZARD);
                }
                // The resource manager had no knowledge of this transaction.
                // Perform some cleanup and return normally.
                currResource.setResourceStatus(StatefulResource.COMPLETED);
                currResource.destroy();
            } else if (_errorCode >= XAException.XA_RBBASE && _errorCode <= XAException.XA_RBEND) {
                // These can be returned on a rollback - all is ok
                currResource.setResourceStatus(StatefulResource.ROLLEDBACK);
                currResource.destroy();
            } else // XAER_PROTO, XAER_INVAL
            {
                currResource.setResourceStatus(StatefulResource.COMPLETED);
                currResource.destroy();

                if (!auditing) {
                    _diagnosticsRequired = true;
                    if (_outcome) {
                        Tr.error(tc, "WTRN0050_UNEXPECTED_XA_ERROR_ON_COMMIT", XAReturnCodeHelper.convertXACode(_errorCode));
                    } else {
                        Tr.error(tc, "WTRN0051_UNEXPECTED_XA_ERROR_ON_ROLLBACK", XAReturnCodeHelper.convertXACode(_errorCode));
                    }
                }

                // An internal logic error has occured.
                _systemException = xae;
            }
        } catch (Throwable t) {
            // Treat like RMERR
            FFDCFilter.processException(t, "com.ibm.tx.jta.impl.RegisteredResources.deliverOutcome", "2111", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RuntimeException", t);

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(flowType, XAFlowCallback.AFTER_FAIL);
            }

            updateHeuristicOutcome(StatefulResource.HEURISTIC_ROLLBACK);
            currResource.setResourceStatus(StatefulResource.COMPLETED);
            currResource.destroy();

            if (_outcome) {
                if (!auditing)
                    _diagnosticsRequired = true;
                Tr.error(tc, "WTRN0068_COMMIT_FAILED", t);
            } else if (auditing) {
                Tr.error(tc, "WTRN0071_ROLLBACK_FAILED", t);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deliverOutcome", retryRequired);
        return retryRequired;
    }

    /**
     * Distributes commit/rollback messages to all Resources in
     * the appropriate state. Called during retry and mainline.
     * <p>
     * (i.e. not including those that voted VoteReadOnly). All Resources that
     * return successfully have their state set to completed;
     * <p>
     * All Resources that throw a heuristic exception are marked and
     * completed during distributeForget.
     * 
     * @return boolean value to indicate whether retries are necessary.
     */
    protected boolean distributeOutcome() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "distributeOutcome", this);

        boolean retryRequired = false; // indicates whether retry necessary

        _diagnosticsRequired = false;

        try {
            final long startTime = System.currentTimeMillis();

            if (gotAsyncResources()) {
                retryRequired = completeAsyncResources();
            }

            boolean priorityResourceHasFailed = false;
            int failedPriority = 0;
            final int resourceCount = _resourceObjects.size();

            // Browse through the non-async participants, processing them as appropriate
            for (int i = 0; i < resourceCount; i++) {
                final JTAResource currResource = _resourceObjects.get(i);

                if (deliverOutcome(currResource)) {
                    retryRequired = true;

                    if (_gotPriorityResourcesEnlisted && _outcome && (currResource instanceof JTAXAResource)) {
                        // Only bail out of we are a 2-PC XAResource - we can carry on if a remote one...
                        if (tc.isEventEnabled())
                            Tr.event(tc, "Bailing because we are committing with priority resources");
                        priorityResourceHasFailed = true;
                        failedPriority = currResource.getPriority();
                    }
                }

                // If an XA resource has failed, process remaining XA resources with same priority
                // and any other non-XA resources (eg Corba) with any priority.
                if (priorityResourceHasFailed) {
                    if (i < (resourceCount - 1)) {
                        final JTAResource nextResource = _resourceObjects.get(i + 1);
                        if (nextResource.getPriority() != failedPriority && (nextResource instanceof JTAXAResource)) {
                            for (int j = i + 1; j < resourceCount; j++) // Need to retry all other resources on z/os
                            {
                                addToFailedResources(_resourceObjects.get(j));
                            }
                            break;
                        }
                    }
                }
            }

            if (gotAsyncResources()) {
                retryRequired = getAsyncCompletionResults(startTime, retryRequired);
            }
        } finally {
            updateHeuristicOutcome(calculateHeuristicOutcome());

            _retryCompletion = true; // Any further calls are retries

            if (_diagnosticsRequired) {
                logDiagnostics(JTAResourceBase.OUTCOME_DIAGNOSTICS);
                // Reset in case commit called direct from prepare else we get diagnostics twice.
                _diagnosticsRequired = false;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "distributeOutcome", retryRequired);
        return retryRequired;
    }

    @SuppressWarnings("unused")
    protected boolean getAsyncCompletionResults(long startTime, boolean retryRequired) {
        // TODO Auto-generated method stub
        return retryRequired;
    }

    protected boolean completeAsyncResources() {
        // Not used in JTM
        return false;
    }

    /**
     * @return
     */
    protected int calculateHeuristicOutcome() {
        int result = StatefulResource.NONE;

        for (int i = 0; i < _resourceObjects.size(); i++) {
            final JTAResource currResource = _resourceObjects.get(i);

            result = HeuristicOutcome.combineStates(result, currResource.getResourceStatus());
        }

        return result;
    }

    /**
     * Possibly update the heuristic state of the transaction.
     * This is only required if this is a subordinate.
     * If we are a subordinate we need to update the state and log it for recovery.
     * 
     * @param commit - true if requested to commit, else false
     */
    private void updateHeuristicState(boolean commit) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "updateHeuristicState", commit);

        if (_transaction.isSubordinate()) {
            // Get the current transaction state.  Need to do this in case we are
            // in recovery and have already logged a heuristic
            final TransactionState ts = _transaction.getTransactionState();
            final int state = ts.getState();
            if (commit) {
                if (state != TransactionState.STATE_HEURISTIC_ON_COMMIT)
                    ts.setState(TransactionState.STATE_HEURISTIC_ON_COMMIT);
            } else {
                // if state is ACTIVE, then this is called via
                // rollbackResources, so do not change state
                if (state != TransactionState.STATE_HEURISTIC_ON_ROLLBACK
                    && state != TransactionState.STATE_ACTIVE)
                    ts.setState(TransactionState.STATE_HEURISTIC_ON_ROLLBACK);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "updateHeuristicState");
    }

    /**
     * Distributes forget messages to all Resources in
     * the appropriate state. Called during retry and mainline.
     * 
     * @return boolean value to indicate whether retries are necessary.
     */
    public boolean distributeForget() throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "distributeForget", this);

        boolean retryRequired = false; // indicates whether retry necessary
        final int resourceCount = _resourceObjects.size();

        // Browse through the participants, processing them as appropriate
        for (int i = 0; i < resourceCount; i++) {
            final JTAResource currResource = _resourceObjects.get(i);

            switch (currResource.getResourceStatus()) {
                case StatefulResource.HEURISTIC_COMMIT:
                case StatefulResource.HEURISTIC_ROLLBACK:
                case StatefulResource.HEURISTIC_MIXED:
                case StatefulResource.HEURISTIC_HAZARD:
                    if (forgetResource(currResource)) {
                        retryRequired = true;
                        _retryRequired = true;
                    }
                    break;

                default: // do nothing
                    break;

            } // end switch

        } // end for

        if (_systemException != null) {
            final Throwable toThrow = new SystemException().initCause(_systemException);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "distributeForget", toThrow);
            throw (SystemException) toThrow;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "distributeForget", retryRequired);
        return retryRequired;
    }

    /**
     * Distribute forget flow to given resource.
     * Used internally when resource indicates a heuristic condition.
     * May result in retries if resource cannot be contacted.
     * 
     * @param resource - the resource to issue forget to
     * @param index - the index of this resource in the arrays
     * @return boolean to indicate whether retries are necessary
     */

    protected boolean forgetResource(JTAResource resource) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "forgetResource", resource);

        boolean result = false; // indicates whether retry necessary
        boolean auditing = false;

        try {
            boolean informResource = true;

            auditing = _transaction.auditSendForget(resource);
            if (xaFlowCallbackEnabled) {
                informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.FORGET,
                                                                    XAFlowCallback.FORGET_NORMAL);
            }

            if (informResource) {
                resource.forget();
            }

            // Set the state of the Resource to completed after a successful forget.
            resource.setResourceStatus(StatefulResource.COMPLETED);

            if (auditing)
                _transaction.auditForgetResponse(XAResource.XA_OK, resource);
            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(XAFlowCallback.FORGET,
                                                  XAFlowCallback.AFTER_SUCCESS);
            }
        } catch (XAException xae) {
            _errorCode = xae.errorCode; // Save locally for FFDC
            FFDCFilter.processException(xae, "com.ibm.tx.jta.impl.RegisteredResources.forgetResource", "2859", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(_errorCode), xae);
            if (auditing)
                _transaction.auditForgetResponse(_errorCode, resource);

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(XAFlowCallback.FORGET,
                                                  XAFlowCallback.AFTER_FAIL);
            }

            if (_errorCode == XAException.XAER_RMERR) {
                // An internal error has occured within
                // the resource manager and it was unable
                // to forget the transaction.
                //
                // Retry the forget flow.
                result = true;
                addToFailedResources(resource);
            } else if (_errorCode == XAException.XAER_RMFAIL) {
                // The resource manager is unavailable.
                // Set the resource's state to failed so that
                // we will attempt to reconnect to the resource
                // manager upon retrying.
                resource.setState(JTAResource.FAILED);

                // Retry the forget flow.
                result = true;
                addToFailedResources(resource);
            } else if (_errorCode == XAException.XAER_NOTA) {
                // The resource manager had no knowledge
                // of this transaction. Perform some cleanup
                // and return normally.
                resource.setResourceStatus(StatefulResource.COMPLETED);
                resource.destroy();
            } else // XAER_INVAL, XAER_PROTO
            {
                if (!auditing)
                    Tr.error(tc, "WTRN0054_XA_FORGET_ERROR", new Object[] { XAReturnCodeHelper.convertXACode(_errorCode), xae });

                resource.setResourceStatus(StatefulResource.COMPLETED);

                // An internal logic error has occured.
                _systemException = xae;
            }
        } catch (Throwable t) {
            // treat like RMERR
            FFDCFilter.processException(t, "com.ibm.tx.jta.impl.RegisteredResources.forgetResource", "2935", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RuntimeException", t);

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(XAFlowCallback.FORGET,
                                                  XAFlowCallback.AFTER_FAIL);
            }

            // An internal error has occured within
            // the resource manager and it was unable
            // to forget the transaction.
            //
            // Retry the forget flow.
            result = true;
            addToFailedResources(resource);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forgetResource", result);
        return result;
    }

    /**
     * Distributes commit messages to all Resources in the registered state.
     * 
     */
    public void distributeCommit()
                    throws SystemException, HeuristicHazardException, HeuristicMixedException, HeuristicRollbackException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "distributeCommit");

        final TransactionState ts = _transaction.getTransactionState();
        ts.setCommittingStateUnlogged();

        // Shuffle resources around according to commit priority order
        _retryRequired = sortResources();
        if (!_retryRequired) {
            _outcome = true;
            _retryRequired = distributeOutcome();
        } else {
            updateHeuristicOutcome(StatefulResource.HEURISTIC_HAZARD);
        }

        if (_systemException != null) {
            final Throwable toThrow = new SystemException().initCause(_systemException);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "distributeCommit", toThrow);
            throw (SystemException) toThrow;
        }

        // check for heuristics ... as we will move forget processing to TransactionImpl later
        if (HeuristicOutcome.isHeuristic(_heuristicOutcome)) {
            switch (_heuristicOutcome) {
                case StatefulResource.HEURISTIC_COMMIT:
                    break;

                case StatefulResource.HEURISTIC_ROLLBACK:
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "distributeCommit", "HeuristicRollbackException");
                    throw new HeuristicRollbackException();

                case StatefulResource.HEURISTIC_HAZARD:
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "distributeCommit", "HeuristicHazardException");
                    throw new HeuristicHazardException();

                default:
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "distributeCommit", "HeuristicMixedException");
                    throw new HeuristicMixedException();
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "distributeCommit");
    }

    /**
     * Internal helper method to flow commitOnePhase to the resource in the registered list.
     * It is called from distributePrepare when all bar one of the resources votes read only.
     * 
     * flowCommitOnePhase flows commit_one_phase to the given resource and, if necessary,
     * performs the required XAException handling.
     * 
     * This method is never called for a subordinate.
     * 
     * @param lastParticipant
     *            Is the resource being committed the Last Participant in an LPS enabled 2PC
     *            transaction. If so then we will not be waiting for a retry or setting the
     *            transaction state to completed after completion of the 1PC.
     * 
     * @exception RollbackException
     * @exception SystemException
     * @exception HeuristicMixedException
     * @exception HeuristicHazardException
     * @exception HeuristicRollbackException
     */
    protected void flowCommitOnePhase(boolean lastParticipant)
                    throws RollbackException, SystemException, HeuristicMixedException,
                    HeuristicHazardException, HeuristicRollbackException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "flowCommitOnePhase", lastParticipant);

        final JTAResource resource = _resourceObjects.get(0);
        boolean informResource = true;
        boolean retryNeeded = false;
        Throwable _xaeCaught = null;
        try {
            resource.setResourceStatus(StatefulResource.COMPLETING_ONE_PHASE);

            if (xaFlowCallbackEnabled) {
                informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.PREPARE,
                                                                    XAFlowCallback.PREPARE_1PC_OPT);
            }

            if (informResource) {
                resource.commit_one_phase();
            }

            resource.setResourceStatus(StatefulResource.COMMITTED);

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(XAFlowCallback.PREPARE,
                                                  XAFlowCallback.AFTER_SUCCESS);
            }
        } catch (XAException xae) {
            _xaeCaught = xae;
            _errorCode = xae.errorCode; // Save locally for FFDC
            FFDCFilter.processException(xae, "com.ibm.tx.jta.impl.RegisteredResources.flowCommitOnePhase", "3031", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(_errorCode), xae);

            final Throwable toThrow;

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(XAFlowCallback.PREPARE,
                                                  XAFlowCallback.AFTER_FAIL);
            }

            if (_errorCode >= XAException.XA_RBBASE &&
                _errorCode <= XAException.XA_RBEND) {
                resource.setResourceStatus(StatefulResource.ROLLEDBACK);
                resource.destroy();

                toThrow = new RollbackException().initCause(xae);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "flowCommitOnePhase", toThrow);
                throw (RollbackException) toThrow;
            } else if (_errorCode == XAException.XA_HEURCOM) {
                resource.setResourceStatus(StatefulResource.HEURISTIC_COMMIT);
                updateHeuristicOutcome(StatefulResource.HEURISTIC_COMMIT);
            } else if (_errorCode == XAException.XA_HEURRB) {
                resource.setResourceStatus(StatefulResource.HEURISTIC_ROLLBACK);
                Tr.error(tc, "WTRN0075_HEURISTIC_ON_COMMIT", _transaction.getTranName());
                updateHeuristicOutcome(StatefulResource.HEURISTIC_ROLLBACK);
            } else if (_errorCode == XAException.XA_HEURMIX) {
                resource.setResourceStatus(StatefulResource.HEURISTIC_MIXED);
                Tr.error(tc, "WTRN0075_HEURISTIC_ON_COMMIT", _transaction.getTranName());
                updateHeuristicOutcome(StatefulResource.HEURISTIC_MIXED);
            } else if (_errorCode == XAException.XA_HEURHAZ) {
                resource.setResourceStatus(StatefulResource.HEURISTIC_HAZARD);
                Tr.error(tc, "WTRN0075_HEURISTIC_ON_COMMIT", _transaction.getTranName());
                updateHeuristicOutcome(StatefulResource.HEURISTIC_HAZARD);
            } else if (_errorCode == XAException.XAER_RMERR) {
                //
                // According to XA, XAER_RMERR occured in committing the
                // work performed on behalf of the transaction branch and
                // the branch's work has been rolled back. Note that this
                // error signals a catatrophic event to the TM since other
                // resource managers may successfully commit their work.
                // This error should be returned only when a resource manager
                // concludes that it can never commit the branch and that it
                // can NOT hold the branch's resources in a prepared state.
                // system administrator's manual intervention is necessary.
                //
                Tr.error(tc, "WTRN0047_XAER_RMERR_ON_COMMIT", resource);
                resource.setResourceStatus(StatefulResource.ROLLEDBACK);
                resource.destroy();

                toThrow = new RollbackException().initCause(xae);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "flowCommitOnePhase", toThrow);
                throw (RollbackException) toThrow;
            } else if (_errorCode == XAException.XAER_RMFAIL) {
                if (resourceNeedsRetrying(resource)) {
                    // Kick-off retry thread and return normally.
                    retryNeeded = true;

                    // Set the resource's state to failed so that
                    // we will attempt to reconnect to the resource
                    // manager upon retrying.
                    // resource.setState(JTAResource.FAILED); // This is a no-op for Corba resources
                } else // Local resource - no retry to commit_one_phase
                {
                    Tr.warning(tc, "WTRN0052_XAER_RMFAIL_ON_COMMIT_ONE_PHASE", resource);
                    resource.setResourceStatus(StatefulResource.COMPLETED);
                    resource.destroy();
                }

                // Return a heuristic outcome as we have no idea what happenned to the resource
                updateHeuristicOutcome(StatefulResource.HEURISTIC_HAZARD);
            } else if (_errorCode == XAException.XAER_NOTA) {
                // The resource manager had no knowledge
                // of this transaction.
                // For local XA resource and first time through on a remote resource, if we
                // get NOTA, we can make the assumption that the d/b rolled back the branch or
                // the remote server died discarding the branch.
                resource.setResourceStatus(StatefulResource.COMPLETED);
                resource.destroy();
                toThrow = new RollbackException().initCause(xae);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "flowCommitOnePhase", toThrow);
                throw (RollbackException) toThrow;
            } else // XAER_INVAL, XAER_PROTO
            {
                Tr.error(tc, "WTRN0053_UNEXPECTED_XA_ERROR_ON_COMMIT_ONE_PHASE", XAReturnCodeHelper.convertXACode(_errorCode));

                resource.setResourceStatus(StatefulResource.COMPLETED);
                resource.destroy();

                // An internal logic error has occured
                toThrow = new SystemException().initCause(xae);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "flowCommitOnePhase", toThrow);
                throw (SystemException) toThrow;
            }
        } catch (Throwable t) {
            // Treat like RMERR
            FFDCFilter.processException(t, "com.ibm.tx.jta.impl.RegisteredResources.flowCommitOnePhase", "3172", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RuntimeException", t);

            final Throwable toThrow;

            if (xaFlowCallbackEnabled) {
                XAFlowCallbackControl.afterXAFlow(XAFlowCallback.PREPARE,
                                                  XAFlowCallback.AFTER_FAIL);
            }

            Tr.error(tc, "WTRN0070_ONE_PHASE_COMMIT_FAILED", t);
            resource.setResourceStatus(StatefulResource.COMPLETED);
            resource.destroy();

            toThrow = new RollbackException().initCause(t);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "flowCommitOnePhase", toThrow);
            throw (RollbackException) toThrow;
        }

        if (_systemException != null) {
            final Throwable toThrow = new SystemException().initCause(_systemException);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "flowCommitOnePhase", toThrow);
            throw (SystemException) toThrow;
        }

        // the following assumes that this is NOT a subordinate
        // If LPS, we do not need to retry as we will effectively return a "vote" and then
        // distributeOutcome which will retry.
        if ((retryNeeded) && (!lastParticipant)) {
            _outcome = true;
            _retryRequired = true;

            // check for heuristics ...
            if (HeuristicOutcome.isHeuristic(_heuristicOutcome)) {
                switch (_heuristicOutcome) {
                    case StatefulResource.HEURISTIC_COMMIT:
                        break;

                    case StatefulResource.HEURISTIC_ROLLBACK:
                        final Throwable toThrow;
                        toThrow = new HeuristicRollbackException();
                        toThrow.initCause(_xaeCaught); // set xaexception as cause
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "flowCommitOnePhase", toThrow);
                        throw (HeuristicRollbackException) toThrow;

                    case StatefulResource.HEURISTIC_HAZARD:
                        toThrow = new HeuristicHazardException();
                        toThrow.initCause(_xaeCaught); // set xaexception as cause
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "flowCommitOnePhase", toThrow);
                        throw (HeuristicHazardException) toThrow;

                    default:
                        toThrow = new HeuristicMixedException();
                        toThrow.initCause(_xaeCaught); // set xaexception as cause
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "flowCommitOnePhase", toThrow);
                        throw (HeuristicMixedException) toThrow;
                }
            }
        } else // synchronous outcome
        {
            if (HeuristicOutcome.isHeuristic(_heuristicOutcome)) {
                switch (_heuristicOutcome) {
                    case StatefulResource.HEURISTIC_COMMIT:
                        break;
                    case StatefulResource.HEURISTIC_ROLLBACK:
                        final Throwable toThrow;
                        if (!lastParticipant) {
                            toThrow = new HeuristicRollbackException();
                            toThrow.initCause(_xaeCaught); // set xaexception as cause
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "flowCommitOnePhase", toThrow);
                            throw (HeuristicRollbackException) toThrow;
                        }
                        if (tc.isEventEnabled())
                            Tr.event(tc, "LPS resource rolled-back heuristically.  Throwing RollbackException.");
                        toThrow = new RollbackException();
                        toThrow.initCause(_xaeCaught); // set xaexception as cause
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "flowCommitOnePhase", toThrow);
                        throw (RollbackException) toThrow;
                    case StatefulResource.HEURISTIC_HAZARD:
                        toThrow = new HeuristicHazardException();
                        toThrow.initCause(_xaeCaught); // set xaexception as cause
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "flowCommitOnePhase", toThrow);
                        throw (HeuristicHazardException) toThrow;
                    default: // Heuristic
                        toThrow = new HeuristicMixedException();
                        toThrow.initCause(_xaeCaught); // set xaexception as cause
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "flowCommitOnePhase", toThrow);
                        throw (HeuristicMixedException) toThrow;
                } // end switch
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "flowCommitOnePhase");
    }

    @SuppressWarnings("unused")
    protected boolean resourceNeedsRetrying(JTAResource resource) {
        return false;
    }

    /**
     * Distributes rollback messages to all Resources in the registered state.
     * <p>
     * 
     * @exception HeuristicMixedException
     * @exception SystemException
     */
    public void distributeRollback()
                    throws HeuristicMixedException, HeuristicHazardException, HeuristicCommitException, SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "distributeRollback");

        final TransactionState ts = _transaction.getTransactionState();
        ts.setRollingBackStateUnlogged();

        // save heuristic status (could be set by prepare)
        final int savedHeuristic = _heuristicOutcome;

        _outcome = false;
        _retryRequired = distributeOutcome();

        if (_systemException != null) {
            final Throwable toThrow = new SystemException().initCause(_systemException);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "distributeRollback", toThrow);
            throw (SystemException) toThrow;
        }

        // check for heuristics ... as we will check for forget in TransactionImpl later
        // don't throw exception if previous heuristic from prepare
        if (HeuristicOutcome.isHeuristic(_heuristicOutcome) &&
            savedHeuristic == StatefulResource.NONE) {
            final Throwable toThrow;

            switch (_heuristicOutcome) {
                case StatefulResource.HEURISTIC_ROLLBACK:
                    break;

                case StatefulResource.HEURISTIC_HAZARD:
                    toThrow = new HeuristicHazardException();
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "distributeRollback", toThrow);
                    throw (HeuristicHazardException) toThrow;

                case StatefulResource.HEURISTIC_COMMIT:
                    toThrow = new HeuristicCommitException();
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "distributeRollback", toThrow);
                    throw (HeuristicCommitException) toThrow;

                default:
                    toThrow = new HeuristicMixedException();
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "distributeRollback", toThrow);
                    throw (HeuristicMixedException) toThrow;
            }
        }

        // restore heuristic state
        if (savedHeuristic != StatefulResource.NONE)
            _heuristicOutcome = savedHeuristic;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "distributeRollback");
    }

    /**
     * Records information in the transaction log about a JTAResource object in the appropriate
     * log section. This indicates that the object has prepared to commit.
     * 
     * @param resource The resource object to log.
     */
    protected void recordLog(JTAResource resource) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recordLog", new Object[] { this, resource });

        //
        // If we do not yet have a log unit, try to get one for this transaction
        //
        if (_logUnit == null) {
            _logUnit = _transaction.getLog();
        }

        //
        // If we now have a log unit, then log the resource
        // (If there is no log unit now, we are running with logging disabled
        // or on z/OS which does not use the RLS logging for transactions).
        //
        if (_logUnit != null) {
            final RecoverableUnitSection rus;
            if (resource instanceof JTAXAResource) {
                if (_xalogSection == null) {
                    _xalogSection = createLogSection(TransactionImpl.XARESOURCE_SECTION, _logUnit);
                }
                rus = _xalogSection;
            } else {
                rus = recordOtherResourceTypes(resource);
            }

            resource.log(rus);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recordLog");
    }

    @SuppressWarnings("unused")
    protected RecoverableUnitSection recordOtherResourceTypes(JTAResource resource) throws SystemException {
        // Not used in JTM
        return null;
    }

    protected RecoverableUnitSection createLogSection(int sectionId, RecoverableUnit ru) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createLogSection", new Object[] { sectionId, ru });

        RecoverableUnitSection logSection = null;

        try {
            // Create a RecoverableUnitSection for multiple data entries for each prepared resource
            logSection = ru.createSection(sectionId, false);
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.tx.jta.impl.RegisteredResources.createLogSection", "2349", this);
            final Throwable toThrow = new SystemException(e.toString()).initCause(e);
            if (tc.isEventEnabled())
                Tr.event(tc, "Exception raised creating log section", e);
            throw (SystemException) toThrow;
        } finally {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createLogSection", logSection);
        }

        return logSection;
    }

    public ArrayList<JTAResource> getResourceObjects() {
        return _resourceObjects;
    }

    /**
     * Informs the caller if a single 1PC CAPABLE resource is enlisted in this unit of work.
     */
    public boolean isOnlyAgent() {
        final boolean result = (_resourceObjects.size() == 1 &&
                        _resourceObjects.get(0) instanceof ResourceSupportsOnePhaseCommit);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "isOnlyAgent", result);
        return result;
    }

    /**
     * Add an XAResource to te list of resources that must have the
     * transaction outcome or forget processing redriven after RM
     * failure. Only relevant for z/OS split processing.
     * 
     * @param jtaRes the XAResource to add to the list
     */
    protected void addToFailedResources(JTAResource jtaRes) {
        // Not used in JTM
    }

    /**
     * Cleanup resources that have not yet been completed.
     * A utility function called when transaction completion has been
     * abandonned either when retries have been exhausted or the operator
     * has cancelled the transaction.
     */
    public void destroyResources() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "destroyResources");

        // Browse through the participants, processing them as appropriate
        final ArrayList<JTAResource> resources = getResourceObjects();
        for (JTAResource resource : resources) {
            destroyResource(resource);
        }

        if (_sameRMResource != null) {
            destroyResource(_sameRMResource);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "destroyResources");
    }

    void destroyResource(JTAResource resource) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "destroyResource", new Object[] { resource, this });

        final int resourceStatus = resource.getResourceStatus();

        if (resourceStatus != StatefulResource.COMPLETED &&
            resourceStatus != StatefulResource.ROLLEDBACK &&
            resourceStatus != StatefulResource.COMMITTED) {
            resource.destroy();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "destroyResource");
    }

    /**
     * @param diagType
     */
    protected void logDiagnostics(int diagType) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "logDiagnostics", diagType);

        final String name = _transaction.getTranName();
        switch (diagType) {
            case JTAResourceBase.PREPARE_DIAGNOSTICS:
                Tr.error(tc, "WTRN0086_PREPARE_DIAG", name);
                break;
            case JTAResourceBase.OUTCOME_DIAGNOSTICS:
                Tr.error(tc, "WTRN0087_COMPLETION_DIAG", name);
                break;
        }

        // Get the resources to log what they did
        final ArrayList<JTAResource> resources = getResourceObjects();
        for (JTAResource r : resources) {
            diagnoseResource(r, diagType);
        }

        if (_sameRMResource != null) {
            _sameRMResource.copyDiagnostics(_sameRMMasterResource);
            diagnoseResource(_sameRMResource, diagType);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "logDiagnostics");
    }

    private void diagnoseResource(JTAResource resource, int diagType) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "diagnoseResource", new Object[] { resource, diagType, this });

        // Only care about JTXAResourceImpls or OnePhaseResourceImpls
        if (resource instanceof JTAResourceBase) {
            ((JTAResourceBase) resource).diagnose(diagType);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "diagnoseResource");
    }

    void logHeuristicOutcome() throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "logHeuristicOutcome");

        if (_transaction.isSubordinate() && _loggedHeuristicOutcome != _heuristicOutcome) {
            boolean force = false;
            if (_hoSection == null) {
                //
                // If we do not yet have a log unit, try to get one for this transaction
                //
                if (_logUnit == null) {
                    _logUnit = _transaction.getLog();
                }

                //
                // If we now have a log unit, then log the resource
                // (If there is no log unit now, we are running with logging disabled.)
                //
                if (_logUnit != null) {
                    _hoSection = createLogSection(TransactionImpl.HEURISTIC_OUTCOME_SECTION, _logUnit);
                }
            } else {
                // We already have a section - so this time we update it and need to force it as we
                // will not be updating the transaction state which would have forced the original
                // heuristic outcome
                force = true;
            }

            if (_hoSection != null) {
                final byte[] byte_data = new byte[] { (byte) _heuristicOutcome };

                try {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "logHeuristicOutcome", ResourceWrapper.printResourceStatus(_heuristicOutcome));

                    _hoSection.addData(byte_data);
                    if (force)
                        _hoSection.force();
                    _loggedHeuristicOutcome = _heuristicOutcome;
                } catch (InternalLogException e) {
                    FFDCFilter.processException(e, "com.ibm.tx.jta.impl.RegisteredResources.logHeuristicOutcome", "1576", this);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "addData failed during logHeuristicOutcome", e);
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "logHeuristicOutcome");
    }

    /**
     * @return
     */
    public int getHeuristicOutcome() {
        return _heuristicOutcome;
    }

    public void updateHeuristicOutcome(int status) {
        final int ho = HeuristicOutcome.combineStates(_heuristicOutcome, status);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "updateHeuristicOutcome",
                     "combining " +
                                     ResourceWrapper.printResourceStatus(_heuristicOutcome) +
                                     " with " +
                                     ResourceWrapper.printResourceStatus(status) +
                                     " to get " +
                                     ResourceWrapper.printResourceStatus(ho)
                            );

        _heuristicOutcome = ho;
    }

    public void setHeuristicOutcome(int status) {
        _heuristicOutcome = status;
    }

    /**
     * @param heuristic
     */
    public void logHeuristic(boolean commit) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "logHeuristic", commit);

        logHeuristicOutcome();

        updateHeuristicState(commit);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "logHeuristic");
    }

    public boolean requireRetry() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "requireRetry", _retryRequired);
        return _retryRequired;
    }

    // There are two comparators included in this class, one for prepare ordering 
    // and one for commit ordering of resources.  The distributePrepare method
    // loops through the list of resources in reverse order, so the sort needs to
    // be in ascending order of priority, wherease distributeOutcome loops through
    // the list in ascending order, so the sort list needs to be in descending order
    // to ensure highest priority resources are processed first.  Resources with
    // the same priority value will be prepared in reverse order to that for
    // commit.  This ensures compatability in ordering with previous releases 
    // where enlisted resources are prepared in reverse order to enslist but
    // committed in the same order.  So if we have 2 pairs of resources of the
    // same priority, ie 1a, 1b, 2a, 2b, where a is enslisted before b, then
    // they will be prepared in order 2b, 2a, 1b, 1a and committed in order
    // 2a, 2b, 1a, 1b.

    // Commit based comparator impementation as part of this class.
    // Sort priority in descending order
    // Comparator returning 0 should leave elements in list alone, preserving original order.
    @Override
    public int compare(JTAResource o1, JTAResource o2) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "compare", new Object[] { o1, o2, this });
        int result = 0;
        int p1 = o1.getPriority();
        int p2 = o2.getPriority();
        if (p1 < p2)
            result = 1;
        else if (p1 > p2)
            result = -1;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "compare", result);
        return result;
    }

    /**
     * Shuffle commitInLastPhase resources to the end of the list preserving their ordering
     * or reorder resources based on commitPriority in descending order for commit phase.
     */
    protected boolean sortResources() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "sortResources", _resourceObjects.toArray());

        if (!_sorted) {
            final int resourceCount = _resourceObjects.size();

            if (_gotPriorityResourcesEnlisted) {
                if (resourceCount > 1)
                    Collections.sort(_resourceObjects, this);
            }
            _sorted = true;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "sortResources", _resourceObjects.toArray());
        return false;
    }

    private final static Comparator<JTAResource> prepareComparator = new Comparator<JTAResource>()
    {
        // Sort priority in ascending order
        // Comparator returning 0 should leave elements in list alone, preserving original order.
        @Override
        public int compare(JTAResource o1, JTAResource o2)
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "compare", new Object[] { o1, o2, this });
            int result = 0;
            int p1 = o1.getPriority();
            int p2 = o2.getPriority();
            if (p1 < p2)
                result = -1;
            else if (p1 > p2)
                result = 1;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "compare", result);
            return result;
        }
    };

    /**
     * Reorder resources based on commitPriority in asccending order for prepare phase.
     */
    protected void sortPreparePriorityResources() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "sortPreparePriorityResources", _resourceObjects.toArray());

        Collections.sort(_resourceObjects, prepareComparator);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "sortPreparePriorityResources", _resourceObjects.toArray());
        return;
    }

    boolean retryImmediately() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "sortPreparePriorityResources", _retryImmediately);
        return _retryImmediately;
    }

    /*
     * commitLastAgent.
     * only called in root
     */
    public int commitLastAgent(boolean isLPS, boolean xaOKVote)
                    throws RollbackException, SystemException, HeuristicMixedException,
                    HeuristicHazardException, HeuristicRollbackException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commitLastAgent", new Object[] { this, isLPS, xaOKVote });

        int result = XA_RDONLY;

        if (!isLPS) {
            // This is the last resource to be processed (i==0), no other resources have voted to commit
            // (okVoteCount==0), this node is in control of the transaction (!subordinate) and the resource
            // supports processing of a one phase commit flow (instance of RSOPC). Under these conditions,
            // we can drive a 1PC flow to the resource without any need to LPS logging.

            try {
                flowCommitOnePhase(false);

                //
                // Set return value to ONEPHASE so that the TransactionImpl knows
                // we have optimized.
                //
                result = ONE_PHASE_OPT;
            } catch (RollbackException rbe) {
                // No FFDC Code Needed.

                // We must intercept a RollbackException and convert it into a ONE_PHASE_OPT_ROLLBACK
                // return so that TransactionImpl knows that the only resource not to vote read only
                // has been rolledback so there is no need to distribute rollback.
                result = ONE_PHASE_OPT_ROLLBACK;
            } finally {
                // Heuristic cases are thrown to caller
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commitLastAgent",
                            (result == ONE_PHASE_OPT ? "ONE_PHASE_OPT" :
                                            (result == ONE_PHASE_OPT_ROLLBACK ? "ONE_PHASE_OPT_ROLLBACK" : "HEURISTIC")));
            }
            return result;
        }

        // LPS case: We must be a superior or a single involved subordinate if we get to this case.
        // We have a 1PC resource enlisted at position 0 and we already have some ok votes
        // so we need to drive 1PC to find our final vote.  Check that LPS is enabled, if not
        // we will throw a RollbackException to rollback the transaction as we cannot complete it.
        try {
            // LIDB1673.22/PQ72718 - check LPS at prepare time
            if (_LPSProhibited && xaOKVote) // d250698
            {
                Tr.error(tc, "WTRN0063_ILLEGAL_COMMIT_OF_1PC_RESOURCE");
                logDiagnostics(JTAResourceBase.PREPARE_DIAGNOSTICS);
                if (tc.isEventEnabled())
                    Tr.event(tc, "LPS not enabled, rolling back transaction");
                throw new RollbackException();
            }

            // As we have reached element 0 all of our 2PC resource must have prepared so at this
            // point we need to force the already written resources to the log via a call to transactionImpl.
            _transaction.logLPSState();

            // LIDB1673-13 lps heuristic completion
            flowCommitOnePhase(true);
            // 1PC was successful so we can return a vote of XA_OK
            result = XA_OK;
        } catch (RollbackException rbe) {
            // No FFDC Code Needed.

            // 1PC resource has decided to rollback so we return our vote by re-throwing the
            // exception.
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commitLastAgent", rbe);
            throw rbe;
        } catch (HeuristicMixedException hme) {
            // No FFDC Code Needed.

            // 1PC resource has returned heuristic so return failed and let TI determine next action
            // from lpsHeuristicCompletion value
            result = ONE_PHASE_OPT_FAILED;
        } catch (HeuristicHazardException hhe) {
            // No FFDC Code Needed.

            // 1PC resource has returned heuristic so return failed and let TI determine next action
            // from lpsHeuristicCompletion value
            result = ONE_PHASE_OPT_FAILED;
            // 1PC resource has returned heuristic so we need to rollback all other resources and
            // rethrow the exception.
        }
        // LPS case should never throw heuristRB
        // catch (HeuristicRollbackException hre)

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commitLastAgent", XAReturnCodeHelper.convertXACode(result));
        return result;
    }

    /**
     * Informs the caller if a 1PC resource is enlisted in this unit of work.
     */
    public boolean isLastAgentEnlisted() {
        final boolean lastAgentEnlisted = (_onePhaseResourceEnlisted != null);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "isLastAgentEnlisted", lastAgentEnlisted);
        return lastAgentEnlisted;
    }

    public void logRmfailOnCompleting(JTAResource currResource, @SuppressWarnings("unused") XAException xae) {
        if (_outcome) {
            Tr.warning(tc, "WTRN0048_XAER_RMFAIL_ON_COMMIT", currResource);
        } else {
            Tr.warning(tc, "WTRN0049_XAER_RMFAIL_ON_ROLLBACK", currResource);
        }
    }

    public void logRmfailOnPreparing(XAException xae) {
        _diagnosticsRequired = true;
        Tr.error(tc, "WTRN0046_PREPARE_FAILED", new Object[] { XAReturnCodeHelper.convertXACode(_errorCode), xae });
    }

    /**
     * 
     * abort resources that can be aborted.
     */
    protected void abort() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "abort", _resourcesLogged);

        for (int i = 0; i < _resourceObjects.size(); i++) {
            final JTAResource resource = _resourceObjects.get(i);

            // Is resource an instance of abortableXAResource?
            if (resource != null)
            {
                XAResource xares = resource.XAResource();

                if (xares instanceof AbortableXAResource)
                {
                    // Class xaresClass = xares.getClass();
                    // String xaresClassStr = xaresClass.getName();
                    // System.out.println("Attempt to abort class: " + xaresClassStr);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "The resource is abortable, so drive abort on XAResource " + resource.XAResource() + ", for transaction " + _transaction.getTranName());
                    AbortableXAResource abortablexares = (AbortableXAResource) xares;
                    abortablexares.abort(_txServiceXid);
                }
                else
                {
                    if (tc.isEventEnabled())
                        Tr.info(tc, "The resource is NOT abortable: " + resource.XAResource() + ", for transaction " + _transaction.getTranName());
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "abort");
    }
}
