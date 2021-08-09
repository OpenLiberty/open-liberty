package com.ibm.ws.sib.msgstore.transactions.impl;
/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.Arrays;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.RollbackException;
import com.ibm.ws.sib.msgstore.SeverePersistenceException;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.XidAlreadyKnownException;
import com.ibm.ws.sib.msgstore.XidStillAssociatedException;
import com.ibm.ws.sib.msgstore.XidUnknownException;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.transactions.ExternalXAResource;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This is the MessageStores implementation of the XAResource interface. This class
 * is used as the interface between the JMS resource manager and XA transaction manager to
 * coordinate two-phase transaction work. This class is also an implementation of the
 * Transaction interface and as such can be passed as part of a call to the
 * ItemStream interfaces. However when it is used on these calls it delegates the
 * real work down to individual {@link XidParticipant} instances. A single
 * MSDelegatingXAResource object can be used to participate in many transactions over time
 * but only one at any one time. However when not associated with any particular transaction
 * a single instance can be used to complete work associated with any previously known XID.
 */
public class MSDelegatingXAResource implements ExternalXAResource, PersistentTransaction
{
    private static TraceComponent tc = SibTr.register(MSDelegatingXAResource.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private MessageStoreImpl       _ms;
    private JsMessagingEngine      _me;
    private PersistenceManager     _persistence;

    private PersistentTranId       _currentPtid;
    private TransactionParticipant _currentTran;

    private XidManager             _manager;

    private int                    _maxSize;

    private BatchingContext        _bc;


    /**
     * The Default constructor for MSDelegatingXAResource.
     *
     * @param ms      The (@link MessageStorImpl MessageStore} that this transaction is a part of.
     * @param persistence The {@link PersistenceManager} implementation to be used this transaction.
     * @param maxSize The number of operations allowed in this transaction.
     */
    public MSDelegatingXAResource(MessageStoreImpl ms, PersistenceManager persistence, int maxSize)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "MSDelegatingXAResource", "MessageStore="+ms+", Persistence="+persistence+", MaxSize="+maxSize);

        _ms          = ms;
        _manager     = ms.getXidManager();
        _persistence = persistence;
        _maxSize     = maxSize;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "MSDelegatingXAResource");
    }


    /*************************************************************************/
    /*                        Transaction Implementation                     */
    /*************************************************************************/


    public synchronized void addWork(WorkItem item) throws ProtocolException, TransactionException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addWork", "WorkItem="+item);

        if (_currentTran != null)
        {
            _currentTran.addWork(item);
        }
        else
        {
            ProtocolException pe = new ProtocolException("TRAN_PROTOCOL_ERROR_SIMS1001");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "No Xid currently associated with this XAResource!", pe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addWork");
            throw pe;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addWork");
    }

    // Defect 17856
    public WorkList getWorkList()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getWorkList");

        WorkList workList = null;

        if (_currentTran != null)
        {
            workList = _currentTran.getWorkList();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getWorkList", "return="+workList);
        return workList;
    }

    // Defect 178563
    public void registerCallback(TransactionCallback callback)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "registerCallback", "Callback="+callback);

        if (_currentTran != null)
        {
            _currentTran.registerCallback(callback);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No Xid currently associated with this XAResource!");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "registerCallback");
    }

    /**
     * We don't need to delegate this method as
     * we know the answer!
     */
    public final boolean isAutoCommit()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isAutoCommit");
            SibTr.exit(this, tc, "isAutoCommit", "return=false");
        }
        return false;
    }

    /**
     * @return True if (and only if) the transaction has (or can have)
     * subordinates.
     */
    public boolean hasSubordinates()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "hasSubordinates");

        boolean retval = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "hasSubordinates", "return="+retval);
        return retval;
    }

    // Feature 184806.3.2
    public PersistentTranId getPersistentTranId()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getPersistentTranId");

        PersistentTranId ptid = null;

        if (_currentTran != null)
        {
            ptid = _currentTran.getPersistentTranId();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getPersistentTranId", "return="+ptid);
        return ptid;
    }

    // Feature 199334.1
    public void incrementCurrentSize() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "incrementCurrentSize");

        if (_currentTran != null)
        {
            _currentTran.incrementCurrentSize();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "incrementCurrentSize");
    }

    // Defect 186657.4
    public boolean isAlive()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isAlive");

        boolean retval = false;

        if (_currentTran != null)
        {
            retval = _currentTran.isAlive();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isAlive", "return="+retval);
        return retval;
    }


    /*************************************************************************/
    /*                        XAResource Implementation                      */
    /*************************************************************************/

    /**
     * Associates a resource with a transaction branch. In our case each XID
     * will map to a MessageStore {@link XidParticipant} object.
     *
     * @param xid    The identifier of the global transaction branch to associate this resource with.
     * @param flags  Association flag
     *               <BR>TMNOFLAGS - New XID, start a new MS transaction.
     *               <BR>TMJOIN - Join an existing global transaction.
     *               <BR>TMRESUME - Resume a previously suspended transaction.
     *
     * @exception XAException
     *                   Thrown if the XID is already known or this XAResource
     *                   is currently associated with a different XID.
     */
    public void start(Xid xid, int flags) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start", new Object[]{"XID="+xid, _manager.xaFlagsToString(flags)});

        if (_currentTran != null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot associate with XAResource. It is already associated with an Xid!", "CurrentXID="+_currentPtid);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
            throw new XAException(XAException.XAER_PROTO);
        }

        try
        {
            _currentPtid = new PersistentTranId(xid);

            // Feature SIB0048c.ms.1
            // Check our flags and either start a new association or
            // resume/join a previously created branch.
            if (flags == TMNOFLAGS)
            {
                _currentTran = new XidParticipant(_ms, _currentPtid, _persistence, _maxSize);

                _manager.start(_currentPtid, _currentTran);

            }
            else
            {
                _currentTran = _manager.start(_currentPtid, flags);
            }
        }
        catch (XidUnknownException xue)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(xue, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.start", "1:316:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot TMJOIN with this Xid. It is not known!", xue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
            XAException xaException = new XAException(XAException.XAER_PROTO);
            xaException.initCause(xue);
            throw xaException;
        }
        catch (XidAlreadyKnownException xake)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(xake, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.start", "1:325:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot start new association with this Xid. It is already known!", xake);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
            XAException xaException = new XAException(XAException.XAER_DUPID);
            xaException.initCause(xake);
            throw xaException;
        }
        catch (Exception e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.start", "1:334:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught during transaction association!", e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
            XAException xaException = new XAException(XAException.XAER_RMERR);
            xaException.initCause(e);
            throw xaException;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
    }

    /**
     * Ends the association that this resource has with the passed transaction
     * branch. Either temporarily via a TMSUSPEND or permanently via TMSUCCESS or
     * TMFAIL.
     *
     * @param xid    The identifier of the global transaction to dis-associate from. This must
     *               match a value previously passed on a call to start.
     * @param flags  Completion flag
     *               <BR>TMSUCCESS - Transaction completed successfully.
     *               <BR>TMFAIL - Transaction completed unsuccessfully.
     *               <BR>TMSUSPEND - Suspend a previously begun Transaction.
     *
     * @exception XAException
     *                   Thrown if the XID is unknown or if this XAResource is
     *                   nor currently associated with an XID.
     */
    public void end(Xid xid, int flags) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "end", new Object[]{"XID="+xid, _manager.xaFlagsToString(flags)});

        try
        {
            _manager.end(new PersistentTranId(xid), flags);

            // Reset our instance variables.
            _currentTran = null;
            _currentPtid = null;
        }
        catch (XidUnknownException xue)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(xue, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.end", "1:375:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot dis-associate from this Xid. It is unknown!", xue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "end");
            XAException xaException = new XAException(XAException.XAER_NOTA);
            xaException.initCause(xue);
            throw xaException;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "end");
    }

    /**
     * The first stage of completion in the two-phase-commit protocol. Once this method has
     * returned the participant should know whether it is capable of successfully committing
     * the work associated with it. If successful completion is possible then a vote of XA_OK
     * is returned. If completion is not possible an exception is thrown.
     *
     * @param xid    The identifier of the global transaction branch to prepare for completion.
     *
     * @return The Prepare vote for this transaction:
     *         <BR>XAResource.XA_OK - Participant is ready to commit its work successfully.
     *         <BR>XAResource.XA_RDONLY - Participant has only done reads as part of this
     *         transaction so it has no work to commit.
     *
     * @exception XAException
     *                   Thrown if an error occurs whilst preparing the Resource Manager for completion
     *                   of this transaction branch. Throwing this exception is the method used by the
     *                   Resource Manager to vote for rollback to be used in completion of this transaction.
     */
    public int prepare(Xid xid) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "prepare", "XID="+xid);

        int retval = XAResource.XA_OK;

        try
        {
            retval = _manager.prepare(new PersistentTranId(xid));

         
        }
        catch (XidStillAssociatedException xsae)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(xsae, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.prepare", "1:421:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot prepare transaction branch, resources are still associated with this Xid!", xsae);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
            XAException xaException = new XAException(XAException.XAER_PROTO);
            xaException.initCause(xsae);
            throw xaException;
        }
        catch (XidUnknownException xue)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(xue, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.prepare", "1:430:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot prepare transaction branch, Xid is unknown!", xue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
            XAException xaException = new XAException(XAException.XAER_NOTA);
            xaException.initCause(xue);
            throw xaException;
        }
        catch (ProtocolException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.prepare", "1:439:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "A Transaction protocol error occurred during prepare of transaction branch!", pe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
            XAException xaException = new XAException(XAException.XAER_PROTO);
            xaException.initCause(pe);
            throw xaException;
        }
        catch (RollbackException rbe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(rbe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.prepare", "1:448:1.51.1.7", this);

    
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "RollbackException caught during prepare of transaction branch!", rbe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
            XAException xaException = new XAException(XAException.XA_RBROLLBACK);
            xaException.initCause(rbe);
            throw xaException;
        }
        catch (SeverePersistenceException spe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(spe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.prepare", "1:463:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected error occurred whilst persisting transaction work!", spe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
            XAException xaException = new XAException(XAException.XAER_RMFAIL);
            xaException.initCause(spe);
            throw xaException;
        }
        catch (TransactionException te)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(te, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.prepare", "1:472:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "TransactionException occurred whilst preparing transaction work!", te);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
            XAException xaException = new XAException(XAException.XAER_RMFAIL);
            xaException.initCause(te);
            throw xaException;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            String retstr;
            switch (retval)
            {
            case XAResource.XA_OK:
                    retstr = "return=XA_OK";
                    break;
            case XAResource.XA_RDONLY:
                    retstr = "return=XA_RDONLY";
                    break;
            default:
                    retstr = "return=XA_UNKNOWN";
            }
            SibTr.exit(this, tc, "prepare", retstr);
        }
        return retval;
    }

    /**
     * The second stage of completion in the two-phase-commit protocol. Once this method has
     * returned all work associated with this transaction participant should be complete. If
     * the onePhase parameter is true then the one-phase completion protocol will be used
     * and commit can be called without a preceding call to prepare.
     *
     * @param xid      The identifier of the global transaction branch to commit.
     * @param onePhase Determines whether this commit action is one-phase
     *                 or two-phase. If two-phase then a previously successful
     *                 prepare call must have been made.
     *
     * @exception XAException
     *                   Thrown if an error occurs during completion. Check the JTA specification for
     *                   details of the individual exception codes.
     */
    public void commit(Xid xid, boolean onePhase) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commit", "XID="+xid+", OnePhase="+onePhase);

        try
        {
            // Commit the MessageStore Transaction to
            // ensure our in-memory model is in-line
            // with the persistent store.
            _manager.commit(new PersistentTranId(xid), onePhase);

        }
        catch (XidUnknownException xue)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(xue, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.commit", "1:536:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot commit transaction branch, Xid is unknown!", xue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            XAException xaException = new XAException(XAException.XAER_NOTA);
            xaException.initCause(xue);
            throw xaException;
        }
        catch (XidStillAssociatedException xsae)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(xsae, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.commit", "1:545:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot commit transaction branch, resources are still associated with this Xid!", xsae);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            XAException xaException = new XAException(XAException.XAER_PROTO);
            xaException.initCause(xsae);
            throw xaException;
        }
        catch (ProtocolException tpe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(tpe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.commit", "1:554:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "A Transaction protocol error occurred during prepare of transaction branch!", tpe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            XAException xaException = new XAException(XAException.XAER_PROTO);
            xaException.initCause(tpe);
            throw xaException;
        }
        catch (RollbackException rbe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(rbe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.commit", "1:563:1.51.1.7", this);


            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "RollbackException caught during commit of transaction branch!", rbe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            XAException xaException = new XAException(XAException.XA_RBROLLBACK);
            xaException.initCause(rbe);
            throw xaException;
        }
        catch (SeverePersistenceException spe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(spe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.commit", "1:582:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected error occurred during commit of transaction branch!", spe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            XAException xaException = new XAException(XAException.XAER_RMFAIL);
            xaException.initCause(spe);
            throw xaException;
        }
        catch (PersistenceException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.commit", "1:591:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected error occurred whilst persisting transaction work!", pe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            XAException xaException = new XAException(XAException.XA_RETRY);
            xaException.initCause(pe);
            throw xaException;
        }
        catch (TransactionException te)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(te, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.commit", "1:600:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected error occurred during commit of transaction branch!", te);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            XAException xaException = new XAException(XAException.XAER_RMFAIL);
            xaException.initCause(te);
            throw xaException;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
    }

    /**
     * Aborts all work associated with the supplied XID. Once this method has
     * returned all changes carried out as part of that transaction branch should
     * be undone.
     *
     * @param xid    The identifier of the global transaction branch to rollback.
     *
     * @exception XAException
     *                   Thrown if an error occurs during completion. Check the JTA specification for
     *                   details of the individual exception codes.
     */
    public void rollback(Xid xid) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "rollback", "XID="+xid);

        try
        {
            // Rollback the MessageStore Transaction to
            // ensure our in-memory model is in-line
            // with the persistent store.
            _manager.rollback(new PersistentTranId(xid));

         
        }
        catch (XidUnknownException xue)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(xue, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.rollback", "1:640:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot rollback transaction branch, Xid is unknown!", xue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
            XAException xaException = new XAException(XAException.XAER_NOTA);
            xaException.initCause(xue);
            throw xaException;
        }
        catch (XidStillAssociatedException xsae)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(xsae, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.rollback", "1:649:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot rollback transaction branch, resources are still associated with this Xid!", xsae);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
            XAException xaException = new XAException(XAException.XAER_PROTO);
            xaException.initCause(xsae);
            throw xaException;
        }
        catch (ProtocolException tpe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(tpe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.rollback", "1:658:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "A Transaction protocol error occurred during rollback of transaction branch!", tpe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
            XAException xaException = new XAException(XAException.XAER_PROTO);
            xaException.initCause(tpe);
            throw xaException;
        }
        catch (TransactionException te)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(te, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.rollback", "1:667:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected error occurred during rollback of transaction branch!", te);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
            XAException xaException = new XAException(XAException.XAER_RMFAIL);
            xaException.initCause(te);
            throw xaException;
        }
        catch (PersistenceException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingXAResource.rollback", "1:676:1.51.1.7", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected error occurred whilst persisting transaction work!", pe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
            XAException xaException = new XAException(XAException.XAER_RMFAIL); // PK53360
            xaException.initCause(pe);
            throw xaException;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
    }

    /**
     * Used at transaction recovery time to retrieve the list on indoubt XIDs known
     * to the MessageStore instance associated with this MSDelegatingXAResource.
     *
     * @param recoveryId The recovery id of the RM to return indoubt XIDs from
     *
     * @return The list of indoubt XIDs currently known by the MessageStore associated with this XAResource
     * @exception XAException
     *                   Thrown if an unexpected error occurs.
     */
    public Xid[] recover(int recoveryId) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "recover", "Recovery ID="+recoveryId);

        Xid[] list = _manager.recover();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "recover", "return="+Arrays.toString(list));
        return list;
    }

    public void forget(Xid xid) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "forget", "XID="+xid);
            SibTr.exit(this, tc, "forget");
        }
    }

    public int getTransactionTimeout() throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTransactionTimeout");
            SibTr.exit(this, tc, "getTransactionTimeout", "return=0");
        }
        return 0;
    }

    public boolean setTransactionTimeout(int timeout) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "setTransactionTimeout", "Timeout="+timeout);
            SibTr.exit(this, tc, "setTransactionTimeout", "return=false");
        }
        return false;
    }

    public boolean isSameRM(XAResource resource) throws XAException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isSameRM", "Resource="+resource);

        boolean retval = false;

        if (resource != null)
        {
            if (resource instanceof MSDelegatingXAResource)
            {
                MSDelegatingXAResource msResource = (MSDelegatingXAResource) resource;

                // Get the local ME object if we don't
                // already have it.
                if (_me == null)
                {
                    _me = _ms._getMessagingEngine();
                }

                // Get the other ME object.
                JsMessagingEngine ME = msResource._ms._getMessagingEngine();

                // We have to check for the presence of the ME
                // objects as in a unit test we may not have one.
                if (_me != null && ME != null)
                {
                    // If we have ME's to check then we will
                    // base out return value on whether the UUIDs
                    // of the two resources are the same.
                    if (_me.getUuid().equals(ME.getUuid()))
                    {
                        retval = true;
                    }
                }
                else
                {
                    // If we do not have ME's to check then we are
                    // most likely in a unit test environment so
                    // we will rely on a weaker check based on
                    // the type of the XAResource.
                    retval = true;

                    // Let's output something to the trace just in case
                    // we get here erroneously.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "MessagingEngine reference is null so using weaker isSameRM test!");
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isSameRM", "return="+retval);
        return retval;
    }


    /*************************************************************************/
    /*                  PersistentTransaction Implementation                 */
    /*************************************************************************/


    public final int getTransactionType()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTransactionType");
            SibTr.exit(this, tc, "getTransactionType", "return=TX_GLOBAL");
        }
        return TX_GLOBAL;
    }

    public void setTransactionState(TransactionState state)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setTransactionState", "State="+state);

        if (_currentTran != null)
        {
            _currentTran.setTransactionState(state);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setTransactionState");
    }

    public TransactionState getTransactionState()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getTransactionState");

        TransactionState retval;
        if (_currentTran != null)
        {
            retval = _currentTran.getTransactionState();
        }
        else
        {
            retval = TransactionState.STATE_NONE;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getTransactionState", "return="+retval);
        return retval;
    }

    public BatchingContext getBatchingContext()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getBatchingContext");
            SibTr.exit(this, tc, "getBatchingContext", "return="+_bc);
        }
        return _bc;
    }

    public void setBatchingContext(BatchingContext bc)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBatchingContext", "BatchingContext="+bc);

        _bc = bc;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBatchingContext");
    }

    // Defect 410652
    /**
     * This method is used to check the MessageStore instance that an implementing
     * transaction object originated from. This is used to check that a transaction
     * is being used to add Items to the same MessageStore as that it came from.
     *
     * @return The MessageStore instance where this transaction originated from.
     */
    public MessageStore getOwningMessageStore()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getOwningMessageStore");
            SibTr.exit(this, tc, "getOwningMessageStore", "return="+_ms);
        }
        return _ms;
    }

    /*************************************************************************/
    /*                       SIXAResource Implementation                     */
    /*************************************************************************/


    public boolean isEnlisted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isEnlisted");

        boolean retval = false;

        if (_currentTran != null)
        {
            retval = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isEnlisted", "return="+retval);
        return retval;
    }

    /**
     * The implementation of hasCode for this object needs to reflect the identity
     * of the current threads associated XidParticipant object if one exists or
     * of the managing XAResource if not. This is so that the XAResource can be
     * passed on the ItemStream interface and still used in a hashtable as if it
     * were several individual objects.
     *
     * @return The hashCode for the XidParticipant associated with the current thread if one
     *         exists.
     */
    public int hashCode()
    {
        int hash = 0;
        if (_currentTran != null)
        {
            hash = _currentTran.hashCode();
        }
        else
        {
            hash = super.hashCode();
        }

        return hash;
    }
}
