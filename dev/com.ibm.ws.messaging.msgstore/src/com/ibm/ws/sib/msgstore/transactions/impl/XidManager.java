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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.RollbackException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.SeverePersistenceException;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.XidAlreadyKnownException;
import com.ibm.ws.sib.msgstore.XidStillAssociatedException;
import com.ibm.ws.sib.msgstore.XidUnknownException;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.PersistentMessageStore;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.ras.SibTr;


/**
 * A {@link MessageStoreImpl MessageStore} contains a single instance of the XidManager class which is used to track all
 * global transaction work currently in progress within that {@link MessageStoreImpl MessageStore}.
 */
public class XidManager
{
    private static TraceComponent tc = SibTr.register(XidManager.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private Vector<PersistentTranId> _indoubtXids = new Vector<PersistentTranId>();

    // Defect 420778
    // Need to use Hashtable here as it provide synchronized access
    // to the map members which is important for keeping our association
    // checks thread safe.
    private Hashtable<PersistentTranId, TransactionParticipant> _associatedTrans   = new Hashtable<PersistentTranId, TransactionParticipant>(); //D187601
    private Hashtable<PersistentTranId, TransactionParticipant> _unassociatedTrans = new Hashtable<PersistentTranId, TransactionParticipant>(); //D187601
    private Hashtable<PersistentTranId, TransactionParticipant> _suspendedTrans    = new Hashtable<PersistentTranId, TransactionParticipant>(); //FSIB0048c.ms.1

    // Defect 560281.1
    // Use an inner class specific to this class for locking.
    private final static class AssociationLock {}

    // Defect 455354
    // This object is used to synchronize changes to the association lists
    private final AssociationLock _associationLock = new AssociationLock();

    private MessageStoreImpl   _ms;
    private PersistenceManager _persistence;

    private int _localTranIdCounter = 0;


    /**
     * Default Constructor for the XidManager.
     *
     * @param ms The {@link MessageStoreImpl MessageStore} that this XidManager is associated with.
     */
    public XidManager(MessageStoreImpl ms)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "MessageStore="+ms);

        _ms = ms;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    // Feature SIB0048c.ms.1
    // This version of start is used to establish an association
    // with a pre-existing transaction branch. This can be done
    // using either the TMJOIN or TMRESUME flags.
    public TransactionParticipant start(PersistentTranId ptid, int flags) throws XidUnknownException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start", new Object[]{"PersistentTranId="+ptid, xaFlagsToString(flags)});

        TransactionParticipant retval = null;

        if ((flags & XAResource.TMJOIN) != 0)
        {
            // Find the existing branch in the associated
            // and return it so that the XAResource can
            // associate with it.
            retval = _associatedTrans.get(ptid);

            if (retval == null)
            {
                XidUnknownException xue = new XidUnknownException("XID_NOT_RECOGNISED_SIMS1007", new Object[]{"start(XID,TMJOIN)", ptid.toTMString()});
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot JOIN with this Xid. It has not previously been started!", xue);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
                throw xue;
            }
        }
        else if ((flags & XAResource.TMRESUME) != 0)
        {
            // Defect 455354
            // We need to make sure that any changes to the association
            // lists occur under a lock.
            synchronized(_associationLock)
            {
                // Find the existing branch in the suspended
                // list and move it to the associated list.
                retval = _suspendedTrans.remove(ptid);

                if (retval == null)
                {
                    XidUnknownException xue = new XidUnknownException("XID_NOT_RECOGNISED_SIMS1007", new Object[]{"start(XID,TMRESUME)", ptid.toTMString()});
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot RESUME this Xid. It has not previously been SUSPENDED!", xue);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
                    throw xue;
                }
                else
                {
                    _associatedTrans.put(ptid, retval);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start", "return="+retval);
        return retval;
    }

    // This version of start is used to establish an association
    // with a NEW transaction branch.
    public void start(PersistentTranId ptid, TransactionParticipant participant) throws XidAlreadyKnownException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start", new Object[]{"PersistentTranId="+ptid, "Participant="+participant});

        // Defect 455354
        // We need to make sure that any changes to the association
        // lists occur under a lock.
        //
        // We also need to check the _unassociatedTrans and _suspendedTrans lists
        // to make sure that we aren't being asked to start a new branch using an
        // Xid that is still running or in the process of being completed.
        synchronized(_associationLock)
        {
            if (!_associatedTrans.containsKey(ptid) &&
                !_unassociatedTrans.containsKey(ptid) &&
                !_suspendedTrans.containsKey(ptid))
            {
                _associatedTrans.put(ptid, participant);
            }
            else
            {
                XidAlreadyKnownException xake = new XidAlreadyKnownException("XID_ALREADY_ASSOCIATED_SIMS1009", new Object[]{"start(XID,TMNOFLAGS)", ptid.toTMString()});
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot start new association with this Xid. It is already known!", xake);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
                throw xake;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
    }

    // Feature SIB0048c.ms.1
    // Dependent on the value of flags end will disassociate the
    // supplied xid or suspend it so that it can be resumed at a
    // later point.
    public void end(PersistentTranId ptid, int flags) throws XidUnknownException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "end", new Object[]{"PersistentTranId="+ptid, xaFlagsToString(flags)});

        // Defect 455354
        // We need to make sure that any changes to the association
        // lists occur under a lock.
        synchronized(_associationLock)
        {
            TransactionParticipant tran = _associatedTrans.remove(ptid);

            if (tran != null)
            {
                // If we are suspending then we need to store the
                // tran in a different list so that it can be
                // resumed later.
                if ((flags & XAResource.TMSUSPEND) != 0)
                {
                    _suspendedTrans.put(ptid, tran);
                }
                else
                {
                    _unassociatedTrans.put(ptid, tran);
                }
            }
            else
            {
                // Feature SIB0048c.ms.1
                // The JTA spec also allows end(TMSUCCESS) on XIDs that
                // have previously been suspended so we need to check the
                // suspended list if the XID supplied is not currently
                // associated.
                if (_suspendedTrans.containsKey(ptid))
                {
                    // If the flags are TMSUCCESS or TMFAIL then we can move this
                    // XID to the unassociated list as it is now ready for
                    // completion. Otherwise we assume the flags are TMSUSPEND
                    if (((flags & XAResource.TMSUCCESS) != 0) || ((flags & XAResource.TMFAIL) != 0))
                    {
                        tran = _suspendedTrans.remove(ptid);

                        _unassociatedTrans.put(ptid, tran);
                    }
                }
                else
                {
                    // Defect 373006.3
                    // If the Xid is in the unassociated list then we can assume
                    // this is a duplicate end from a TMJOIN case. This at least
                    // allows us to maintain better error checking for a
                    // completely unknown Xid.
                    if (!_unassociatedTrans.containsKey(ptid))
                    {
                        XidUnknownException xue = new XidUnknownException("XID_NOT_RECOGNISED_SIMS1007", new Object[]{"end(XID)", ptid.toTMString()});
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot disassociate from this Xid. It is not currently associated!", xue);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "end");
                        throw xue;
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "end");
    }


    public int prepare(PersistentTranId ptid) throws XidUnknownException, XidStillAssociatedException,
                                                     ProtocolException, RollbackException, SeverePersistenceException, TransactionException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "prepare", "PersistentTranId="+ptid);

        if (_associatedTrans.containsKey(ptid))
        {
            XidStillAssociatedException xsae = new XidStillAssociatedException("XID_STILL_ASSOCIATED_SIMS1008", new Object[]{"prepare(XID)", ptid.toTMString()});
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Xid is still associated. Needs to be disassociated before completion!", xsae);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
            throw xsae;
        }

        int retval = XAResource.XA_OK;

        // If the previous test failed then we only
        // need to check the disassociated list to
        // see if this xid is known to us.
        TransactionParticipant participant = _unassociatedTrans.get(ptid);

        if (participant != null)
        {
            try
            {
                retval = participant.prepare();

                _indoubtXids.add(ptid);
            }
            catch (RollbackException rbe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(rbe, "com.ibm.ws.sib.msgstore.transactions.XidManager.prepare", "1:304:1.62");

                // We have rolled-back so we can throw
                // away the participant as we shouldn't
                // get called again regarding this XID
                synchronized(_associationLock)
                {
                    participant = _unassociatedTrans.remove(ptid);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.debug(this, tc, "Participant removed from XidManager due to RollbackException: "+participant);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "RollbackException caught during prepare phase of transaction!", rbe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
                throw rbe;
            }
        }
        else
        {
            XidUnknownException xue = new XidUnknownException("XID_NOT_RECOGNISED_SIMS1007", new Object[]{"prepare(XID)", ptid.toTMString()});
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot prepare transaction branch, Xid is unknown!", xue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
            throw xue;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare", "return="+retval);
        return retval;
    }


    public void commit(PersistentTranId ptid, boolean onePhase) throws XidUnknownException, XidStillAssociatedException,
                                                                       ProtocolException, RollbackException, SeverePersistenceException,
                                                                       PersistenceException, TransactionException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commit", "onePhase="+onePhase+", PersistentTranId="+ptid);

        if (_associatedTrans.containsKey(ptid))
        {
            XidStillAssociatedException xsae = new XidStillAssociatedException("XID_STILL_ASSOCIATED_SIMS1008", new Object[]{"commit(XID)", ptid.toTMString()});
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Xid is still associated. Needs to be disassociated before completion!", xsae);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            throw xsae;
        }

        // If the previous test failed then we only
        // need to check the disassociated list to
        // see if this xid is known to us.
        TransactionParticipant participant = _unassociatedTrans.get(ptid);

        if (participant != null)
        {
            // Defect 398385
            // Track whether we have completed this
            // xid or are going to have to retry it.
            boolean xidCompleted = false;
            try
            {
                participant.commit(onePhase);

                // We have successfully finished commit processing
                xidCompleted = true;

                // Defect 373927
                // If we are onePhase then we won't have had a
                // corresponding prepare phase and so won't need
                // to remove the ptid from the list. This will
                // remove an uneccessary lookup and should help
                // improve performance in the 1PC case.
                if (!onePhase)
                {
                    _indoubtXids.remove(ptid);
                }
            }
            catch (RollbackException rbe)
            {
                // No FFDC Code Needed.
                // We have rolled back our part of the transaction so
                // we are done with this xid.
                xidCompleted = true;
                throw rbe;
            }
            finally
            {
                // Defect 398385
                // We can only remove the xid from the list once we have
                // completed the transaction. Some types of exception will
                // trigger (and allow) the commit call to be retried and
                // in those cases we need to maintain knowledge of the
                // xid.
                if (xidCompleted)
                {
                    // Defect 455354
                    synchronized(_associationLock)
                    {
                        _unassociatedTrans.remove(ptid);
                    }
                }
            }
        }
        else
        {
            XidUnknownException xue = new XidUnknownException("XID_NOT_RECOGNISED_SIMS1007", new Object[]{"commit(XID)", ptid.toTMString()});
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot commit transaction branch, Xid is unknown!", xue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            throw xue;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
    }


    public void rollback(PersistentTranId ptid) throws XidUnknownException, XidStillAssociatedException,
                                                       ProtocolException, SeverePersistenceException,
                                                       PersistenceException, TransactionException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "rollback", "PersistentTranId="+ptid);

        if (_associatedTrans.containsKey(ptid))
        {
            XidStillAssociatedException xsae = new XidStillAssociatedException("XID_STILL_ASSOCIATED_SIMS1008", new Object[]{"rollback(XID)", ptid.toTMString()});
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Xid is still associated. Needs to be disassociated before completion!", xsae);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
            throw xsae;
        }

        // If the previous test failed then we only
        // need to check the disassociated list to
        // see if this xid is known to us.
        TransactionParticipant participant = _unassociatedTrans.get(ptid);

        if (participant != null)
        {
            participant.rollback();

            // We are here if the rollback is a sucess.
            // Remove the indoubt xid and the xid from the unassociated list
            _indoubtXids.remove(ptid);

            // Defect 455354
            synchronized(_associationLock)
            {
                _unassociatedTrans.remove(ptid);
            }
        }
        else
        {
            XidUnknownException xue = new XidUnknownException("XID_NOT_RECOGNISED_SIMS1007", new Object[]{"rollback(XID)", ptid.toTMString()});
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot rollback transaction branch, Xid is unknown!", xue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
            throw xue;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
    }


    /**
     * Called at server startup to rebuild our list of indoubt
     * transactions from the datastore.
     *
     * @param PM     The PersistentMessageStore to use to access our datastore.
     *
     * @exception TransactionException
     *                   Thrown if any unexpected exceptions occur.
     * @throws SevereMessageStoreException 
     */
    public void restart(PersistentMessageStore PM) throws TransactionException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "restart", "PersistenceManager="+PM);

        if (PM != null)
        {
            // We need to store the PM so that we can call
            // it during transaction completion.
            if (PM instanceof PersistenceManager)
            {
                _persistence = (PersistenceManager)PM;
            }
            else
            {
                // We don't have a valid PersistenceManager so we
                // are up the spout. Need to fall over in a big heap.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "No PersistenceManager provided at startup. MessageStore cannot continue!");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "restart");
                throw new SevereMessageStoreException("UNRECOVERABLE_ERROR_SIMS1499", new Object[]{});
            }


            // We need to read our indoubt xid list in preperation
            // for a call from the transaction manager at recovery
            // time.
            try
            {
                List list = PM.readIndoubtXIDs();
                Iterator iterator = list.iterator();

                while (iterator.hasNext())
                {
                    PersistentTranId ptid = (PersistentTranId)iterator.next();

                    _indoubtXids.add(ptid);

                    synchronized(_associationLock)
                    {
                        _unassociatedTrans.put(ptid, new XidParticipant(_ms, ptid, _persistence, 0, TransactionState.STATE_PREPARED));
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Indoubt Transaction Re-instated from database: "+ptid);
                }
            }
            catch (PersistenceException pe)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.transactions.XidManager.restart", "1:516:1.62");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Persistence exception caught reading indoubt transactions from database!", pe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "restart");
                throw new TransactionException("UNEXPECTED_EXCEPTION_SIMS1099", new Object[]{pe}, pe);
            }
        }
        else
        {
            // We don't have a valid PersistenceManager so we
            // are up the spout. Need to fall over in a big heap.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "PersistenceManager is null. MessageStore cannot continue!");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "restart");
            throw new SevereMessageStoreException("UNRECOVERABLE_ERROR_SIMS1499", new Object[]{});
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "restart");
    }


    /**
     * This method returns the list of indoubt Xids that the MessageStore
     * knows about at the time. This includes those recovered from the
     * datastore at restart time and any that are currently known about
     * as part of normal runtime processing.
     *
     * @return The list of prepared Xids known to the MessageStore at the
     *         time the method was called.
     */
    public Xid[] recover()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "recover");

        Xid[] xids = new Xid[_indoubtXids.size()];

        _indoubtXids.toArray(xids);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "recover", "return="+Arrays.toString(xids)+", size="+xids.length);
        return xids;
    }


    public PersistentTransaction getTransactionFromTranId(PersistentTranId ptid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getTransactionFromTranId", "TranId="+ptid);

        PersistentTransaction retval = _unassociatedTrans.get(ptid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getTransactionFromTranId", "return="+retval);
        return retval;
    }


    public boolean isTranIdKnown(PersistentTranId ptid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isTranIdKnown", "PersistentTranId="+ptid);

        boolean retval = false;

        if (_associatedTrans.containsKey(ptid) || _unassociatedTrans.containsKey(ptid))
        {
            retval = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isTranIdKnown", "return="+retval);
        return retval;
    }


    // Defect 211924
    /**
     * This method returns a list representing all Xid known by the ME which do
     * not match those currently known by the local Transaction Manager. The
     * admin console can therefore use the output of this method to display a
     * list of xids that can be completed through the ME admin console.
     *
     * @return The list of xids that the admin console should display.
     */
    public Xid[] listRemoteInDoubts()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "listRemoteInDoubts");


        Vector<PersistentTranId> xids = (Vector<PersistentTranId>)_indoubtXids.clone();
        Xid[]  retval = null;

        if (xids.size() > 0)
        {
            retval = new Xid[xids.size()];
            xids.toArray(retval);
        }
        else
        {
            // The ME doesn't know of any indoubt xids at
            // this moment in time.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "There are currently no in-doubt transaction branches within the ME.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "listRemoteInDoubts", "return="+retval);
        return retval;
    } 

    public String toXmlString()
    {
        StringBuffer retval = new StringBuffer();

        retval.append("<transaction-management>\n");
        retval.append("<associated-transactions>\n");

        Enumeration<TransactionParticipant> enumeration = _associatedTrans.elements();
        while (enumeration.hasMoreElements())
        {
            TransactionParticipant tran = enumeration.nextElement();
            retval.append(tran.toXmlString());
        }

        retval.append("</associated-transactions>\n");

        retval.append("<unassociated-transactions>\n");

        enumeration = _unassociatedTrans.elements();
        while (enumeration.hasMoreElements())
        {
            TransactionParticipant tran = enumeration.nextElement();
            retval.append(tran.toXmlString());
        }

        retval.append("</unassociated-transactions>\n");

        retval.append("<suspended-transactions>\n");

        enumeration = _suspendedTrans.elements();
        while (enumeration.hasMoreElements())
        {
            TransactionParticipant tran = enumeration.nextElement();
            retval.append(tran.toXmlString());
        }

        retval.append("</suspended-transactions>\n");

        retval.append("<indoubt-transaction-ids>\n");

        Iterator<PersistentTranId> iterator = _indoubtXids.iterator();
        while (iterator.hasNext())
        {
            PersistentTranId xid = iterator.next();
            retval.append("<xid>");
            retval.append(xid.toString());
            retval.append("</xid>\n");
        }

        retval.append("</indoubt-transaction-ids>\n");
        retval.append("</transaction-management>\n");

        return retval.toString();
    }

    public synchronized int generateLocalTranId()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "generateLocalTranId");

        int retval = _localTranIdCounter++;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "generateLocalTranId", "return="+retval);
        return retval;
    }

    // Feature SIB0048c.ms.1
    public String xaFlagsToString(int flags)
    {
        StringBuilder retval = new StringBuilder("Flags=");
        retval.append(Integer.toHexString(flags));
        retval.append("( ");

        if (flags == XAResource.TMNOFLAGS)
        {
            retval.append("TMNOFLAGS ");
        }
        else
        {
            if ((flags & XAResource.TMENDRSCAN)   != 0) retval.append("TMENDRSCAN ");
            if ((flags & XAResource.TMFAIL)       != 0) retval.append("TMFAIL ");
            if ((flags & XAResource.TMJOIN)       != 0) retval.append("TMJOIN ");
            if ((flags & XAResource.TMONEPHASE)   != 0) retval.append("TMONEPHASE ");
            if ((flags & XAResource.TMRESUME)     != 0) retval.append("TMRESUME ");
            if ((flags & XAResource.TMSTARTRSCAN) != 0) retval.append("TMSTARTRSCAN ");
            if ((flags & XAResource.TMSUCCESS)    != 0) retval.append("TMSUCCESS ");
            if ((flags & XAResource.TMSUSPEND)    != 0) retval.append("TMSUSPEND ");
        }

        retval.append(")");
        return retval.toString();
    }
}

