/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.XidProxy;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * A mapping between Core SPI transaction and numeric ID used, by the communications
 * component, to refer to the transaction. This table is required because,
 * for reasons of brievity and convinience - Core SPI transaction objects are
 * refered to using "unique" numeric identifiers when transmitted using the FAP
 * protocol.
 * <p>
 * This table provides the capability for an association to be made between a
 * Core SPI transaction object and a numeric identifier. It also provides a
 * mechanism by which a Core SPI transaction object may be retrieved, given its
 * unique identifier.
 * <p>
 * <strong>Limitations</strong>
 * <br>
 * For historical reasons (i.e. we did it this way in JFAP version 1 and it is
 * too hard to change) the following limitations / scoping is applied:
 * <ul>
 * <li>The range of unique numeric IDs used to identify Core SPI transactions is
 * scoped by the underlying network link. In Communications parlance - the
 * "link level attachment" is used to anchor an instance of this object.</li>
 * <li>The range of unique numeric IDs used to identify Core SPI transactions is
 * used for both SIUncoordinatedTransaction and SIXAResource type transaction
 * objects. It would be simpler if we had used different ranges - but it is
 * too late now... </li>
 * </ul>
 * 
 * @author Gareth Matthews
 */
public class IdToTransactionTable {
// @start_class_string_prolog@
    public static final String $sccsid = "@(#) 1.22 SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/IdToTransactionTable.java, SIB.comms, WASX.SIB, aa1225.01 10/10/06 07:02:43 [7/2/12 05:58:59]";
// @end_class_string_prolog@

    /** Class name for FFDC's */
    private static String CLASS_NAME = IdToTransactionTable.class.getName();

    /** Trace */
    private static final TraceComponent tc = SibTr.register(IdToTransactionTable.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /** Log class info on static load */
    static {
        if (tc.isDebugEnabled())
            SibTr.debug(tc, "@(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/IdToTransactionTable.java, SIB.comms, WASX.SIB, aa1225.01 1.22");
    }

    /** Maps integer id's to sub-classes of AbstractTransactionEntry */
    private final IdToObjectMap map = new IdToObjectMap();

    /**
     * A special "invalid" transaction. This is used when "optimized" transactions are
     * being used (i.e. transactions are created at the point they are first used) and
     * the transaction creation operation fails. Communications code that uses
     * transactions should check if this value has been returned from this table and, if
     * so, carry out no work.
     */
    public static final InvalidTransaction INVALID_TRANSACTION = new InvalidTransaction();

    /**
     * An invalid transaction type. This class implements the interfaces required to be
     * a Platform Messaging transaction - yet will blow up horribly if anyone attempts
     * to use it.
     */
    private static class InvalidTransaction implements SIXAResource, SIUncoordinatedTransaction {
        private void selfDestruct() throws SIErrorException {
            final SIErrorException exception = new SIErrorException();
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".InvalidTransaction",
                                        CommsConstants.IDTOTXTABLE_SELFDESTRUCT_01,
                                        this);
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        }

        public boolean isEnlisted() {
            selfDestruct();
            return false;
        }

        public void commit(Xid arg0, boolean arg1) throws XAException {
            selfDestruct();
        }

        public void end(Xid arg0, int arg1) throws XAException {
            selfDestruct();
        }

        public void forget(Xid arg0) throws XAException {
            selfDestruct();
        }

        public int getTransactionTimeout() throws XAException {
            selfDestruct();
            return 0;
        }

        public boolean isSameRM(XAResource arg0) throws XAException {
            selfDestruct();
            return false;
        }

        public int prepare(Xid arg0) throws XAException {
            selfDestruct();
            return 0;
        }

        public Xid[] recover(int arg0) throws XAException {
            selfDestruct();
            return new Xid[] {};
        }

        public void rollback(Xid arg0) throws XAException {
            selfDestruct();
        }

        public boolean setTransactionTimeout(int arg0) throws XAException {
            selfDestruct();
            return false;
        }

        public void start(Xid arg0, int arg1) throws XAException {
            selfDestruct();
        }

        public void commit() throws SIIncorrectCallException, SIRollbackException, SIResourceException, SIConnectionLostException {
            selfDestruct();
        }

        public void rollback() throws SIIncorrectCallException, SIResourceException, SIConnectionLostException {
            selfDestruct();
        }
    }

    /**
     * Hashmap which maps conversationId to a HashSet of transaction Id's. This is used
     * to perform the appropriate cleanup when a conversation (i.e. an SICoreConnection
     * is closed).
     */
    private final HashMap conversationIdToTranIdSet = new HashMap();

    // begin D297060
    /**
     * Adds a local transaction into the table.
     * 
     * @param id the client side id used by the client to identify the
     *            local transaction
     * @param owningConversation the id of the conversation that owns the transaction.
     * @param tran the transaction to add
     */
    public synchronized void addLocalTran(int id,
                                          int owningConversation,
                                          SIUncoordinatedTransaction tran) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "add", new Object[] { "" + id, "" + owningConversation, tran });

        LocalTransactionEntry tx = new LocalTransactionEntry(owningConversation, tran);
        if (tran != INVALID_TRANSACTION) {
            associateTransactionWithConversation(owningConversation, id);
        }
        map.put(id, tx);
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "add");
    }

    // end D297060

    // begin D297060
    /**
     * Adds work done by a resource in the scope of a global transaction
     * into the table.
     * 
     * @param transactionId the client side id used by the client to identify
     *            SIXAResource which is enlisted into the global transaction
     * @param owningConversation the conversation which "owns" the resource - that
     *            is to say, the conversation who's lifetime scopes the lifetime of the resource.
     * @param resource the SIXAResource which is enlisted into the global
     *            transaction
     * @param xid the XID relating to the resources participation in
     *            the global transaction.
     * @param isOperationOptimized does the transaction represent an "optimized"
     *            transaction (i.e. one which does not receive explicit start and
     *            end flows).
     */
    public synchronized void addGlobalTransactionBranch(int transactionId,
                                                        int owningConversation,
                                                        SIXAResource resource,
                                                        XidProxy xid,
                                                        boolean isOperationOptimized) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "addGlobalTransactionBranch", new Object[] { "" + transactionId, "" + owningConversation, resource, xid, "" + isOperationOptimized });

        ResourceManagerEntry resourceManagerEntry = null;

        // Find the entry corresponding to the resource being enlisted
        // into the global transaction.
        if (map.containsKey(transactionId)) {
            resourceManagerEntry = (ResourceManagerEntry) map.get(transactionId);
        } else {
            // We don't already have an entry for this resource manager - add one.
            resourceManagerEntry = new ResourceManagerEntry(owningConversation, resource);
            map.put(transactionId, resourceManagerEntry);

            // Associate the resource with the owning connection so that the transaction
            // can be cleaned up if the connection closes or fails.
            if (resource != INVALID_TRANSACTION) {
                associateTransactionWithConversation(owningConversation, transactionId);
            }
        }

        // Mark the resource as being in doubt for the XID used to enlist it into
        // the global transaction.
        resourceManagerEntry.setInDoubt(xid, isOperationOptimized);

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "addGlobalTransactionBranch");
    }

    /**
     * @param transactionId the transaction ID to query the table for.
     * @param tollerateAbsence should the absence of the ID be tolerated or not. If a value
     *            of true is specified and there is no entry for the specified transactionId then a value
     *            of null will be returned. Otherwise a runtime exception will be thrown.
     * @return the transaction object associated with the specified transactionId. This may be
     *         null if:
     *         <ul>
     *         <li>The transactionId specified was equal to <code>CommsConstants.NO_TRANSACTION</code>.</li>
     *         <li>The tollerateAbsence parameter was set to true and there was no corresponding
     *         entry in the table.</li>
     *         </ul>
     */
    // begin D297060
    public synchronized SITransaction get(int transactionId, boolean tollerateAbsence) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "get", new Object[] { "" + transactionId, "" + tollerateAbsence });

        SITransaction result = null;
        if (transactionId != CommsConstants.NO_TRANSACTION) {
            if (tollerateAbsence) {
                if (map.containsKey(transactionId)) {
                    AbstractTransactionEntry tx = (AbstractTransactionEntry) map.get(transactionId);
                    result = tx.getSITransaction();
                }
            } else {
                AbstractTransactionEntry tx = (AbstractTransactionEntry) map.get(transactionId);
                result = tx.getSITransaction();
            }
        }
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "get", result);
        return result;
    }

    // end D297060

    /**
     * @param transactionId the transaction ID corresponding to an SIXAResource.
     * @param xid the XID with which the resource was enlisted into a global transaction.
     * @return the SIXAResource implementation used to start a global transaction branch or
     *         null if there is no corresponding entry in the table (for example if recovery processing
     *         is occurring and thus there is no record for the resource that was originally enlisted
     *         into the global transaction).
     */
    public synchronized SIXAResource getResourceForGlobalTransactionBranch(int transactionId,
                                                                           XidProxy xid) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getResourceForGlobalTransactionBranch", new Object[] { "" + transactionId, xid });

        SIXAResource result = null;
        if (map.containsKey(transactionId)) {
            AbstractTransactionEntry uow = (AbstractTransactionEntry) map.get(transactionId);
            if (uow.isForLocalTransaction()) {
                // We were anticipating a global transaction branch as a table entry.
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".getResourceForGlobalTransactionBranch",
                                            CommsConstants.IDTOTXTABLE_GETTXGLOBALTXBRANCH_01,
                                            new Object[] { this, map, "" + transactionId, xid });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            } else {
                ResourceManagerEntry globalUow = (ResourceManagerEntry) uow;
                if (globalUow.isXidProxyInDoubt(xid))
                    result = (SIXAResource) globalUow.getSITransaction();
            }
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getResourceForGlobalTransactionBranch", result);
        return result;
    }

    // begin D297060
    /**
     * Identical function to invoking get(transactionId, false).
     * 
     * @see IdToTransactionTable#get(int, boolean)
     */
    public SITransaction get(int transactionId) {
        return get(transactionId, false);
    }

    // end D297060

    // begin D297060
    /**
     * Removes a local transaction from the table. This is done at
     * the point the local transaction is no longer in flight. The
     * removal process disassociates the local transaction identifier
     * from the connection which started it (as there is no longer
     * any requirement to clean up resources for this transaction when
     * the connection closes).
     * 
     * @param transactionId the transaction identifier to remove.
     */
    public synchronized void removeLocalTransaction(int transactionId) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "removeLocalTransaction", "" + transactionId);

        AbstractTransactionEntry uow = (AbstractTransactionEntry) map.get(transactionId);
        if (uow.isForLocalTransaction()) {
            map.remove(transactionId);
            if (uow.getSITransaction() != INVALID_TRANSACTION) {
                disassociateTransaction(uow.getOwningConnection(), transactionId);
            }
        } else {
            // We were anticipating a local transaction as a table entry.
            // Throw an error exception.
            final SIErrorException exception = new SIErrorException();
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".removeLocalTransaction",
                                        CommsConstants.IDTOTXTABLE_REMOVELOCALTX_01,
                                        new Object[] { this, map, "" + transactionId });
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        }
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "removeLocalTransaction");
    }

    // end D297060

    // begin D297060
    /**
     * Removes work done in the scope of a global transaction from
     * the table. This is done at the point the transaction is no
     * longer in flight - i.e. when it is commited, rolled back or
     * prepared. The removal process marks the work done for the
     * specified XID as complete. If all the units of work for a
     * specific resource are completed then reference to the CATTransaction
     * for the related SIXAResource is removed from the table.
     * 
     * @param transactionId the transaction identifier to remove
     * @param xid identifies the unit of work to remove.
     */
    public synchronized void removeGlobalTransactionBranch(int transactionId, XidProxy xid) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeGlobalTransactionBranch", new Object[] { "" + transactionId, xid });

        AbstractTransactionEntry uow = (AbstractTransactionEntry) map.get(transactionId);
        if (uow.isForLocalTransaction()) {
            // We were anticipating a global transaction branch as a table entry.
            // Throw an error exception.
            final SIErrorException exception = new SIErrorException();
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".removeGlobalTransactionBranch",
                                        CommsConstants.IDTOTXTABLE_REMOVEGLOBALTXBRANCH_01,
                                        new Object[] { this, map, "" + transactionId, xid });
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        } else {
            ResourceManagerEntry globalUow = (ResourceManagerEntry) uow;
            globalUow.completeXid(xid);
            if (!globalUow.hasInDoubtXids()) {
                map.remove(transactionId);
                if (globalUow.getSITransaction() != INVALID_TRANSACTION) {
                    disassociateTransaction(globalUow.getOwningConnection(), transactionId);
                }
            }
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeGlobalTransactionBranch");
    }

    // end D297060

    // Start D254870
    /**
     * Removes entries from the table based on the conversation with which the
     * entries are associated.
     * 
     * @param conversation all transactions specified as being in the scope of
     *            this conversation will be removed.
     * @param dispatchableMap The map of dispatchables associated with this conversation
     *            that should also be cleaned up.
     */
    public synchronized void removeTransactions(Conversation conversation, TransactionToDispatchableMap dispatchableMap) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeTransactions",
                                           new Object[] { conversation, dispatchableMap });

        HashSet tranSet = (HashSet) conversationIdToTranIdSet.remove(Integer.valueOf(conversation.getId()));
        if (tranSet != null) {
            Iterator iterator = tranSet.iterator();
            while (iterator.hasNext()) {
                int id = ((Integer) iterator.next()).intValue();
                if (tc.isDebugEnabled())
                    SibTr.debug(this, tc, "removing transaction with id=" + id);
                map.remove(id);

                // Now clean up the dispatchables for this transaction Id
                dispatchableMap.removeAllDispatchablesForTransaction(id);
            }
        }

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "removeTransactions");
    }

    // End D254870

    /**
     * Marks a transaction, or transaction branch, as rollback-only.
     * This prevents the future successful completion of the transaction or
     * transaction branch.
     * <br>
     * <strong>Note:</strong> that when this method is applied to a transaction
     * Id that corresponds to an SIXAResource - it is the currently enlisted
     * XID that is marked as being rollback only. If there is no currently
     * enlisted XID then a runtime exception is thrown.
     * 
     * @param transactionId identifies the transaction, or transaction branch
     *            to mark as rollback only.
     * @param throwable the throwable that has resulted in the transaction or
     *            transaction branch being marked as rollback only.
     */
    public synchronized void markAsRollbackOnly(int transactionId,
                                                Throwable throwable) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "markAsRollbackOnly", new Object[] { "" + transactionId, throwable });
        AbstractTransactionEntry tx = (AbstractTransactionEntry) map.get(transactionId);
        tx.markAsRollbackOnly(throwable);
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "markAsRollbackOnly");
    }

    /**
     * Ends a global transaction branch. This has the effect of marking a resource as
     * no longer being enlisted for a global transaction using the specified XID.
     * Ending a transaction branch does not remove the transaction from the table as we
     * are still required to track transactions up to the point they are resolved by
     * either a commit, prepare or rollback.
     * <p>
     * Note that an exception will be thrown if, at the time of the call, the resource
     * specified was not enlisted using the specified XID.
     * 
     * @param transactionId is the ID corresponding to the resource that will have its
     *            global transaction branch ended.
     * @param xid the XID that the resource is currently enlisted using (and will no
     *            longer be enlisted using after this call has run to completion).
     */
    public synchronized void endGlobalTransactionBranch(int transactionId, XidProxy xid) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "endGlobalTransactionBranch", new Object[] { "" + transactionId, xid });
        AbstractTransactionEntry tx = (AbstractTransactionEntry) map.get(transactionId);
        if (tx.isForLocalTransaction()) {
            // We were anticipating a global transaction branch as a table entry.
            // Throw an error exception.
            final SIErrorException exception = new SIErrorException();
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".endGlobalTransactionBranch",
                                        CommsConstants.IDTOTXTABLE_ENDGLOBALTXBRANCH_01,
                                        new Object[] { this, map, "" + transactionId, xid });
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        } else {
            ResourceManagerEntry resource = (ResourceManagerEntry) tx;
            resource.endTransactionBranch(xid);
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "endGlobalTransactionBranch");
    }

    /**
     * @param transactionId the numeric identifier that corresponds to the local transaction.
     * @return the exception specified when the local transaction was marked as rollback only.
     *         An exception will be thrown if the transaction is not local, or not rollback only.
     */
    public synchronized Throwable getExceptionForRollbackOnlyLocalTransaction(int transactionId) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getExceptionForRollbackOnlyTransaction", "" + transactionId);
        AbstractTransactionEntry tx = (AbstractTransactionEntry) map.get(transactionId);
        Throwable result = null;
        if (tx.isForLocalTransaction()) {
            LocalTransactionEntry localTx = (LocalTransactionEntry) tx;
            result = localTx.getException();
        } else {
            // We were anticipating a local transaction as a table entry.
            // Throw an error exception.
            final SIErrorException exception = new SIErrorException();
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".getExceptionForRollbackOnlyLocalTransaction",
                                        CommsConstants.IDTOTXTABLE_GETEXCEPFORROLLBACKLOCALTX_01,
                                        new Object[] { this, map, "" + transactionId });
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getExceptionForRollbackOnlyTransaction", result);
        return result;
    }

    /**
     * @param transactionId numeric identifier for the resource.
     * @param xid the XID corresponding to the global transaction branch.
     * @return true if the global transaction branch has been marked rollback only. False otherwise.
     *         An exception will be thrown if this method is invoked for a local transaction.
     */
    public synchronized boolean isGlobalTransactionBranchRollbackOnly(int transactionId, XidProxy xid) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "isGlobalTransactionBranchRollbackOnly", new Object[] { "" + transactionId, xid });
        AbstractTransactionEntry tx = (AbstractTransactionEntry) map.get(transactionId);
        boolean result = false;
        if (tx.isForLocalTransaction()) {
            // We were anticipating a global transaction branch as a table entry.
            // Throw an error exception.
            final SIErrorException exception = new SIErrorException();
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".endGlobalTransactionBranch",
                                        CommsConstants.IDTOTXTABLE_ENDGLOBALTXBRANCH_01,
                                        new Object[] { this, map, "" + transactionId, xid });
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        } else {
            ResourceManagerEntry resource = (ResourceManagerEntry) tx;
            result = resource.isRollbackOnly(xid);
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "isGlobalTransactionBranchRollbackOnly", "" + result);
        return result;
    }

    /**
     * @param transactionId the transaction id for the local transaction being
     *            tested.
     * @return true if the local transaction has been marked rollback only. False
     *         otherwise. If the id specified does not correspond to a local transaction,
     *         or is not present in this table, an exception will be thrown.
     */
    public synchronized boolean isLocalTransactionRollbackOnly(int transactionId) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "isLocalTransactionRollbackOnly", "" + transactionId);
        AbstractTransactionEntry tx = (AbstractTransactionEntry) map.get(transactionId);
        boolean result = false;
        if (!tx.isForLocalTransaction()) {
            // We were anticipating a local transaction as a table entry.
            // Throw an error exception.
            final SIErrorException exception = new SIErrorException();
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".isLocalTransactionRollbackOnly",
                                        CommsConstants.IDTOTXTABLE_ISLOCALTXROLLBACKONLY_01,
                                        new Object[] { this, map, "" + transactionId });
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        } else {
            LocalTransactionEntry localTx = (LocalTransactionEntry) tx;
            result = localTx.isRollbackOnly();
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "isLocalTransactionRollbackOnly", "" + result);
        return result;
    }

    /**
     * Ends the enlistment of the currently enlisted XID with the current global
     * transaction branch.
     * 
     * @param transactionId the transaction identifier which corresponds to a global
     *            transaction branch previously added to the table.
     * @param flags the flags to specify when ending the resources enlistment.
     */
    public synchronized void endOptimizedGlobalTransactionBranch(int transactionId, int flags) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "endOptimizedGlobalTransactionBranch", new Object[] { "" + transactionId, "" + flags });
        AbstractTransactionEntry tx = (AbstractTransactionEntry) map.get(transactionId);

        if (tx.isForLocalTransaction()) {
            // We were anticipating a global transaction branch as a table entry.
            // Throw an error exception.
            final SIErrorException exception = new SIErrorException();
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".endOptimizedGlobalTransactionBranch",
                                        CommsConstants.IDTOTXTABLE_ENDOPTGLOBALTXBRANCH_01,
                                        new Object[] { this, map, "" + transactionId });
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        } else {
            ResourceManagerEntry resource = (ResourceManagerEntry) tx;
            resource.endCurrentTransactionBranch(flags, true);
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "endOptimizedGlobalTransactionBranch");
    }

    /**
     * @param transactionId the transaction id, corresponding to a global transaction
     *            branch.
     * @param xid the XID corresponding to a global transaction branch.
     * @return the exception which caused a global transaction branch to be marked as
     *         rollback only. An exception will be thrown if the transaction branch (identified
     *         by the transactionId parameter, taken in combination with the xid parameter) is
     *         not a global transaction branch, or is not rollback only.
     */
    public synchronized Throwable getExceptionForRollbackOnlyGlobalTransactionBranch(int transactionId, XidProxy xid) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getExceptionForRollbackOnlyGlobalTransactionBranch", new Object[] { "" + transactionId, xid });
        AbstractTransactionEntry tx = (AbstractTransactionEntry) map.get(transactionId);
        Throwable result = null;
        if (tx.isForLocalTransaction()) {
            // We were anticipating a global transaction branch as a table entry.
            // Throw an error exception.
            final SIErrorException exception = new SIErrorException();
            FFDCFilter.processException(exception,
                                        CLASS_NAME + ".getExceptionForRollbackOnlyGlobalTransactionBranch",
                                        CommsConstants.IDTOTXTABLE_GETEXPFORROLLBACKONLYGLOBALTX_01,
                                        new Object[] { this, map, "" + transactionId });
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        } else {
            ResourceManagerEntry resource = (ResourceManagerEntry) tx;
            if (!resource.isRollbackOnly(xid)) {
                // The resource is not marked rollback only - thus it is invalid to query it
                // for the exception which caused it to become rollback only!
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".getExceptionForRollbackOnlyGlobalTransactionBranch",
                                            CommsConstants.IDTOTXTABLE_GETEXPFORROLLBACKONLYGLOBALTX_02,
                                            new Object[] { this, map, "" + transactionId });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }
            result = resource.getException(xid);
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getExceptionForRollbackOnlyGlobalTransactionBranch", result);
        return result;
    }

    /**
     * Rolls back any in doubt transactions or tranasction branches associated with the
     * conversation which have not been given a completion direction. This will be any
     * transaction which has not been committed, prepared or rolled back.
     * 
     * @param conversation the conversation to rollback transactions for.
     */
    public synchronized void rollbackTxWithoutCompletionDirection(Conversation conversation) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "rollbackTxWithoutCompletionDirection", conversation);
        HashSet tranSet = (HashSet) conversationIdToTranIdSet.get(Integer.valueOf(conversation.getId()));
        if (tranSet != null) {
            Iterator iterator = tranSet.iterator();
            while (iterator.hasNext()) {
                int transactionId = ((Integer) iterator.next()).intValue();
                AbstractTransactionEntry transaction = (AbstractTransactionEntry) map.get(transactionId);
                if (tc.isDebugEnabled())
                    SibTr.debug(this, tc, "rolling back transaction without completion direction id=" + transactionId);
                transaction.rollbackTxWithoutCompletionDirection();
            }
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "rollbackTxWithoutCompletionDirection");
    }

    /**
     * Rolls back any enlisted XIDs for the transactions identified by the set of
     * transaction IDs argument. Enlisted XIDs are defined as those which have
     * been started but not ended.
     * 
     * @param transactionIdArray a set of transaction IDs that identify resources
     *            to be rolledback from enlisted state.
     */
    public synchronized void rollbackEnlisted(Conversation conversation) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "rollbackEnlisted", conversation);
        HashSet tranSet = (HashSet) conversationIdToTranIdSet.get(Integer.valueOf(conversation.getId()));
        if (tranSet != null) {
            Iterator iterator = tranSet.iterator();
            while (iterator.hasNext()) {
                int transactionId = ((Integer) iterator.next()).intValue();
                AbstractTransactionEntry transaction = (AbstractTransactionEntry) map.get(transactionId);
                if (tc.isDebugEnabled())
                    SibTr.debug(this, tc, "rolling back transaction with id=" + transactionId);
                transaction.rollbackEnlisted();
            }
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "rollbackEnlisted");
    }

    /**
     * Base type for entries in the map of transaction Id to transaction entry.
     * This class contains common function to both the local transaction and
     * global transaction branch varients.
     */
    private abstract class AbstractTransactionEntry {
        private final int connection;
        private final SITransaction transaction;

        /**
         * Constructor
         * 
         * @param connection connection that "owns" the transaction.
         * @param transaction Core SPI transaction implementation.
         */
        protected AbstractTransactionEntry(int connection, SITransaction transaction) {
            this.connection = connection;
            this.transaction = transaction;
        }

        /** @return the Core SPI transaction implementation */
        public SITransaction getSITransaction() {
            return transaction;
        }

        /** @return the connection that "owns" the transaction */
        public int getOwningConnection() {
            return connection;
        }

        /** @return true if, and only if, the entry represents a local transaction. */
        public abstract boolean isForLocalTransaction();

        /**
         * Rolls back the transaction branch if the resource represented by this
         * entry is enlisted.
         */
        public abstract void rollbackEnlisted();

        /**
         * Rolls back any transactions which do not have a completion direction.
         * I.e. those which have not been rolled back, comitted or prepared.
         */
        public abstract void rollbackTxWithoutCompletionDirection();

        /**
         * Marks the transaction or resource represented by this entry as rollback
         * only.
         * 
         * @param throwable the throwable that caused this transaction, or
         *            transaction branch to fail.
         */
        public abstract void markAsRollbackOnly(Throwable throwable);
    }

    /**
     * Table entry that represents a local transaction. This is used to
     * track state information about the local transaction.
     */
    private class LocalTransactionEntry extends AbstractTransactionEntry {
        // Set when the local transaction is marked rollback only.  When this
        // occurres the value used to set this attribute is the exception which
        // caused the transaction to be marked rollback only.
        private Throwable throwable;

        /**
         * Constructor
         * 
         * @param connnection
         * @param transaction
         */
        public LocalTransactionEntry(int connection, SIUncoordinatedTransaction transaction) {
            super(connection, transaction);
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".LocalTransactionEntry.<init>", new Object[] { "" + connection, transaction });
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".LocalTransactionEntry.<init>");
        }

        /**
         * @return true if, and only if, the transaction has been marked
         *         rollback only.
         */
        public boolean isRollbackOnly() {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".LocalTransactionEntry.isRollbackOnly");
            final boolean result = throwable != null;
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".LocalTransactionEntry.isRollbackOnly", "" + result);
            return result;
        }

        /**
         * @return the exception used to mark this transaction as rollback only.
         *         If this transaction is not rollback only then an exception will be
         *         thrown when this method is invoked.
         */
        public Throwable getException() {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".LocalTransactionEntry.getException");
            final Throwable result = throwable;
            if (result == null) {
                // There is no exception for this global transaction branch
                // (it has not marked in error) invoking this method is erronious!
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".LocalTransactionEntry.getException",
                                            CommsConstants.IDTOTXTABLE_LOCALTXENTRY_GETEXCEPTION_01,
                                            new Object[] { this, map, throwable });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".LocalTransactionEntry.getException", result);
            return result;
        }

        /** @see com.ibm.ws.sib.comms.server.IdToTransactionTable.AbstractTransactionEntry#isForLocalTransaction() */
        @Override
        public boolean isForLocalTransaction() {
            return true;
        }

        /** @see com.ibm.ws.sib.comms.server.IdToTransactionTable.AbstractTransactionEntry#rollbackTxWithoutCompletionDirection() */
        @Override
        public void rollbackTxWithoutCompletionDirection() {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".LocalTransactionEntry.rollbackTxWithoutCompletionDirection");
            SIUncoordinatedTransaction tran = (SIUncoordinatedTransaction) getSITransaction();
            try {
                tran.rollback();
            } catch (SIException e) {
                // No FFDC code needed.
                if (tc.isDebugEnabled())
                    SibTr.debug(this, tc, "rollback failed with exception: " + e);
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".LocalTransactionEntry.rollbackTxWithoutCompletionDirection");
        }

        /** @see com.ibm.ws.sib.comms.server.IdToTransactionTable.AbstractTransactionEntry#markAsRollbackOnly(java.lang.Throwable) */
        @Override
        public void markAsRollbackOnly(Throwable throwable) {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".LocalTransactionEntry.markAsRollbackOnly", throwable);
            if (this.throwable == null)
                this.throwable = throwable;
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".LocalTransactionEntry.markAsRollbackOnly");
        }

        /** @see com.ibm.ws.sib.comms.server.IdToTransactionTable.AbstractUnitOfWorkEntry#rollbackEnlisted() */
        @Override
        public void rollbackEnlisted() {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".LocalTransactionEntry.rollbackEnlisted");
            SIUncoordinatedTransaction tran = (SIUncoordinatedTransaction) getSITransaction();
            try {
                tran.rollback();
            } catch (SIException e) {
                // No FFDC code needed
                if (tc.isDebugEnabled())
                    SibTr.debug(this, tc, "rollback failed with exception: " + e);
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".LocalTransactionEntry.rollbackEnlisted");
        }

    }

    /**
     * Table entry that represents an XA resource. This is used to
     * track state information about the resource and the global
     * transaction branches it participates in.
     */
    private class ResourceManagerEntry extends AbstractTransactionEntry {
        // Set to the XID that the resource is currently enlisted using
        // or null if the resource is not enlisted.
        private XidProxy enlistedXid = null;

        // Set to the throwable that caused the enlisted XID to be marked
        // rollback only.  Or null if there is no enlisted XID or the
        // enlisted XID is not rollback only.
        private Throwable enlistedThrowable;

        // Set to true if the currently enlisted XID is for an optimized
        // transaction.  False otherwise.
        private boolean enlistedWorkIsOptimized;

        // Map of XID to (rollback only) throwable used to track global
        // transaction branches that the resource has done work in but
        // is neither currently enlisted for or has completed.
        private final HashMap xidProxyToRollbackOnlyThrowableMap = new HashMap();

        /**
         * Constructor
         * 
         * @param connection
         * @param transaction
         */
        public ResourceManagerEntry(int connection, SIXAResource transaction) {
            super(connection, transaction);
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.<init>", new Object[] { "" + connection, transaction });
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.<init>");
        }

        /**
         * Ends the transaction branch which this resource is currently enlisted
         * in. For an optimized resource - this invokes the .end() method on the
         * XAResource itself.
         */
        public void endCurrentTransactionBranch(int flags, boolean optimized) {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.endCurrentTransactionBranch", new Object[] { "" + flags, "" + optimized });

            if (enlistedXid == null) {
                // There is currently no enlisted XID to end.
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".ResourceManagerEntry.endCurrentTransactionBranch",
                                            CommsConstants.IDTOTXTABLE_RMENTRY_ENDCURRENT_01,
                                            new Object[] { this, map });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }

            if (optimized != enlistedWorkIsOptimized) {
                // The optimized flag is set - yet the enlisted work is not optimized
                // (or t'other way round)
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".ResourceManagerEntry.endCurrentTransactionBranch",
                                            CommsConstants.IDTOTXTABLE_RMENTRY_ENDCURRENT_02,
                                            new Object[] { this, map, "" + optimized, "" + enlistedWorkIsOptimized, enlistedXid });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }

            if (enlistedWorkIsOptimized) {
                try {
                    ((SIXAResource) getSITransaction()).end(enlistedXid, flags);
                } catch (XAException e) {
                    // We have failed to perform the end.
                    // We must mark the transaction rollback only.
                    // We should FFDC at this point as well
                    FFDCFilter.processException(e,
                                                CLASS_NAME + ".ResourceManagerEntry.endCurrentTransactionBranch",
                                                CommsConstants.IDTOTXTABLE_RMENTRY_ENDCURRENT_03,
                                                new Object[] { this, map, "errorCode=" + e.errorCode, enlistedXid });
                    markAsRollbackOnly(e);
                }
            }

            xidProxyToRollbackOnlyThrowableMap.put(enlistedXid, enlistedThrowable);
            enlistedXid = null;
            enlistedThrowable = null;
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.endCurrentTransactionBranch");
        }

        /**
         * @param xid the XID to test for this resource.
         * @return true if, and only if, the resource is in doubt for the
         *         specified XID.
         */
        public boolean isXidProxyInDoubt(XidProxy xid) {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.isXidProxyInDoubt", xid);
            final boolean result = xidProxyToRollbackOnlyThrowableMap.containsKey(xid) ||
                                   (xid.equals(enlistedXid) && enlistedWorkIsOptimized);
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.isXidProxyInDoubt", "" + result);
            return result;
        }

        /**
         * @param xid identifies the transaction branch for this resource
         * @return the exception used to mark this transaction branch as
         *         rollback only.
         */
        public Throwable getException(XidProxy xid) {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.getException", xid);
            Throwable result = null;
            if (xid.equals(enlistedXid)) {
                result = enlistedThrowable;
                if (result == null) {
                    // There is currently no exception for this transaction branch.
                    // Presumably it has never been marked as rollback only.
                    // Throw an error exception.
                    final SIErrorException exception = new SIErrorException();
                    FFDCFilter.processException(exception,
                                                CLASS_NAME + ".ResourceManagerEntry.getException",
                                                CommsConstants.IDTOTXTABLE_RMENTRY_GETEXCEPTION_01,
                                                new Object[] { this, map, xid });
                    throw exception;
                }
            } else if (xidProxyToRollbackOnlyThrowableMap.containsKey(xid)) {
                result = (Throwable) xidProxyToRollbackOnlyThrowableMap.get(xid);
                if (result == null) {
                    // There is no record that the transaction branch was ever marked
                    // as being rollback only.
                    // Throw an error exception.
                    final SIErrorException exception = new SIErrorException();
                    FFDCFilter.processException(exception,
                                                CLASS_NAME + ".ResourceManagerEntry.getException",
                                                CommsConstants.IDTOTXTABLE_RMENTRY_GETEXCEPTION_02,
                                                new Object[] { this, map, xid });
                    if (tc.isEventEnabled())
                        SibTr.exception(this, tc, exception);
                    throw exception;
                }
            } else {
                // The XID does not correspond to the current, enlisted, transaction branch
                // or a transaction branch that this resource has participated in previously.
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".ResourceManagerEntry.getException",
                                            CommsConstants.IDTOTXTABLE_RMENTRY_GETEXCEPTION_03,
                                            new Object[] { this, map, xid });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.getException", "" + result);
            return result;
        }

        /**
         * @param xid identifies a transaction branch for this resource.
         * @return true if, and only if, the transaction branch has been marked
         *         rollback only.
         */
        public boolean isRollbackOnly(XidProxy xid) {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.isRollbackOnly", xid);
            boolean result = false;
            if (xid.equals(enlistedXid)) {
                result = enlistedThrowable != null;
            } else if (xidProxyToRollbackOnlyThrowableMap.containsKey(xid)) {
                result = xidProxyToRollbackOnlyThrowableMap.get(xid) != null;
            } else {
                // The XID specified does not correspond to either the currently enlisted
                // XID - or an XID that this resource has participated with for an
                // unresolved transaction branch.
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".ResourceManagerEntry.isRollbackOnly",
                                            CommsConstants.IDTOTXTABLE_RMENTRY_ISROLLBACKONLY_01,
                                            new Object[] { this, map, xid });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.isRollbackOnly", "" + result);
            return result;
        }

        /**
         * @return true if this resource has indoubt transaction branches - where
         *         an indoubt transaction branch is a transaction branch which has not
         *         progressed as far as being prepared, committed or rolledback.
         */
        public boolean hasInDoubtXids() {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.hasInDoubtXids");
            final boolean result = (enlistedXid != null) || (!xidProxyToRollbackOnlyThrowableMap.isEmpty());
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.hasInDoubtXids", "" + result);
            return result;
        }

        /**
         * Completes the transaction branch for the specified XID. This has
         * the effect of removing information about the transaction branch
         * from the table entry.
         * 
         * @param xid
         */
        public void completeXid(XidProxy xid) {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.completeXid", xid);
            if (xid.equals(enlistedXid)) {
                if (enlistedWorkIsOptimized) {
                    enlistedThrowable = null;
                    enlistedXid = null;
                } else {
                    // We have been asked to complete an XID (for a non-optimized transaction)
                    // for which the XID specified is currently enlisted.  This is a protocol
                    // error - as the transaction branch should have ended the resource prior
                    // to this.
                    // Throw an error exception.
                    final SIErrorException exception = new SIErrorException();
                    FFDCFilter.processException(exception,
                                                CLASS_NAME + ".ResourceManagerEntry.completeXid",
                                                CommsConstants.IDTOTXTABLE_RMENTRY_COMPLETEXID_01,
                                                new Object[] { this, map, xid });
                    if (tc.isEventEnabled())
                        SibTr.exception(this, tc, exception);
                    throw exception;
                }
            } else {
                if (xidProxyToRollbackOnlyThrowableMap.containsKey(xid)) {
                    xidProxyToRollbackOnlyThrowableMap.remove(xid);
                } else {
                    // An attempt to remove the XID from the list of in doubt transaction
                    // branches failed.  We can only conclude that the transaction branch
                    // was never created in the first place.  This shouldn't happen...
                    // Throw an error exception.
                    final SIErrorException exception = new SIErrorException();
                    FFDCFilter.processException(exception,
                                                CLASS_NAME + ".ResourceManagerEntry.completeXid",
                                                CommsConstants.IDTOTXTABLE_RMENTRY_COMPLETEXID_02,
                                                new Object[] { this, map, xid });
                    if (tc.isEventEnabled())
                        SibTr.exception(this, tc, exception);
                    throw exception;
                }
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.completeXid");
        }

        /**
         * Ends the transaction branch (as specified by the XID parameter) for the
         * resource.
         * 
         * @param xid identifies the transaction branch.
         */
        public void endTransactionBranch(XidProxy xid) {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.endTransactionBranch", xid);
            if (xid.equals(enlistedXid)) {
                xidProxyToRollbackOnlyThrowableMap.put(enlistedXid, enlistedThrowable);
                enlistedXid = null;
            } else {
                // An attempt was made to end a transaction branch for an XID other than
                // the enlisted XID (or the resource was not enlisted).
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".ResourceManagerEntry.endTransactionBranch",
                                            CommsConstants.IDTOTXTABLE_RMENTRY_ENDTXBRANCH_01,
                                            new Object[] { this, map, xid });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.endTransactionBranch");
        }

        /**
         * Marks the transaction branch as being "in doubt". I.e. started but not
         * commited, prepared or rolledback.
         * 
         * @param xid identifies the transaction branch for this resource
         * @param isOptimized is the transaction branch for an optimized
         *            transaction?
         */
        public void setInDoubt(XidProxy xid, boolean isOptimized) {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.setInDoubt", new Object[] { xid, "" + isOptimized });
            if (enlistedXid != null) {
                // There is already an enlisted XID for this resource.  It is invalid
                // to attempt to enlist a second XID.
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".ResourceManagerEntry.setInDoubt",
                                            CommsConstants.IDTOTXTABLE_RMENTRY_SETINDOUBT_01,
                                            new Object[] { this, map, xid });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            } else if (xidProxyToRollbackOnlyThrowableMap.containsKey(xid)) {
                // An attempt was made to enlist using an XID that there already exists
                // an unresolved transaction branch for (using this resource).
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".ResourceManagerEntry.setInDoubt",
                                            CommsConstants.IDTOTXTABLE_RMENTRY_SETINDOUBT_02,
                                            new Object[] { this, map, xid });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            } else {
                enlistedXid = xid;
                enlistedWorkIsOptimized = isOptimized;
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.setInDoubt");
        }

        /** @see com.ibm.ws.sib.comms.server.IdToTransactionTable.AbstractTransactionEntry#isForLocalTransaction() */
        public boolean isForLocalTransaction() {
            return false;
        }

        /** @see com.ibm.ws.sib.comms.server.IdToTransactionTable.AbstractUnitOfWorkEntry#markInError(java.lang.Throwable) */
        public void markAsRollbackOnly(Throwable throwable) {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.markAsRollbackOnly", throwable);
            if (enlistedXid == null) {
                // There is no enlisted XID to mark as rollback only.
                // Throw an error exception.
                final SIErrorException exception = new SIErrorException();
                FFDCFilter.processException(exception,
                                            CLASS_NAME + ".ResourceManagerEntry.markAsRollbackOnly",
                                            CommsConstants.IDTOTXTABLE_RMENTRY_MARKROLLBACKONLY_01,
                                            new Object[] { this, map, throwable });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            } else {
                enlistedThrowable = throwable;
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.markAsRollbackOnly");
        }

        /** @see com.ibm.ws.sib.comms.server.IdToTransactionTable.AbstractUnitOfWorkEntry#rollbackTxWithoutCompletionDirection() */
        public synchronized void rollbackTxWithoutCompletionDirection() {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.rollbackTxWithoutCompletionDirection");
            SIXAResource resource = (SIXAResource) getSITransaction();
            if (enlistedXid != null) {
                try {
                    resource.end(enlistedXid, SIXAResource.TMNOFLAGS);
                } catch (XAException e) {
                    // No FFDC code needed.
                    if (tc.isDebugEnabled())
                        SibTr.debug(this, tc, "end failed with exception: " + e);
                }

                endTransactionBranch(enlistedXid);
            }

            Iterator i = xidProxyToRollbackOnlyThrowableMap.keySet().iterator();
            while (i.hasNext()) {
                XidProxy xidProxy = (XidProxy) i.next();
                try {
                    resource.rollback(xidProxy);
                } catch (XAException e) {
                    // No FFDC code needed.
                    if (tc.isDebugEnabled())
                        SibTr.debug(this, tc, "rollback failed with exception: " + e);
                }
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.rollbackTxWithoutCompletionDirection");
        }

        /** @see com.ibm.ws.sib.comms.server.IdToTransactionTable.AbstractTransactionEntry#rollbackEnlisted() */
        public void rollbackEnlisted() {
            if (tc.isEntryEnabled())
                SibTr.entry(this, tc, ".ResourceManagerEntry.rollbackEnlisted");
            if (enlistedXid != null) {
                SIXAResource resource = (SIXAResource) getSITransaction();
                try {
                    resource.end(enlistedXid, XAResource.TMFAIL);
                } catch (XAException e) {
                    // No FFDC Code Needed.
                }

                try {
                    resource.rollback(enlistedXid);
                } catch (XAException e) {
                    // No FFDC Code Needed
                }
                enlistedXid = null;
                enlistedThrowable = null;
                enlistedWorkIsOptimized = false;
            }
            if (tc.isEntryEnabled())
                SibTr.exit(this, tc, ".ResourceManagerEntry.rollbackEnlisted");
        }
    }

    /**
     * toString()
     * 
     * @return String
     */
    public String toString() {
        return map.toString();
    }

    /**
     * Associates a transaction, or transaction branch, with a conversation.
     * This is done so that the transaction, or branch, will be cleaned up if the
     * conversation is closed prior to the transaction being resolved.
     * 
     * @param conversationId
     * @param tranId
     */
    private void associateTransactionWithConversation(int conversationId, int tranId) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "associateTransactionWithConversation", new Object[] { "conversationId=" + conversationId, "tranId=" + tranId });
        HashSet tranSet = (HashSet) conversationIdToTranIdSet.get(Integer.valueOf(conversationId));
        if (tranSet == null) {
            tranSet = new HashSet();
            tranSet.add(Integer.valueOf(tranId));
            conversationIdToTranIdSet.put(Integer.valueOf(conversationId), tranSet);
        } else {
            tranSet.add(Integer.valueOf(tranId));
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "associateTransactionWithConversation");
    }

    /**
     * Disassociates a transaction, or transaction branch, from a conversation.
     * 
     * @param conversationId
     * @param tranId
     */
    private void disassociateTransaction(int conversationId, int tranId) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "disassociateTransaction", new Object[] { "conversationId=" + conversationId, "tranId=" + tranId });
        HashSet tranSet = (HashSet) conversationIdToTranIdSet.get(Integer.valueOf(conversationId));
        if (tranSet != null) {
            tranSet.remove(Integer.valueOf(tranId));
            if (tranSet.isEmpty())
                conversationIdToTranIdSet.remove(Integer.valueOf(conversationId));
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "disassociateTransaction");
    }
}
