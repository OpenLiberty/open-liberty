/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import java.util.HashSet;
import java.util.Iterator;

import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.XidProxy;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.DispatchQueue;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Maps information about a transaction to a dispatchable. The dispatchable is
 * supplied to the receive listener dispatcher which uses it to order operations
 * carried out in the scope of the transaction.
 */
public class TransactionToDispatchableMap {
    private static final TraceComponent tc = SibTr.register(TransactionToDispatchableMap.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

    private static String CLASS_NAME = TransactionToDispatchableMap.class.getName();

    // The id to object map implementation that underpins the first level of the
    // data structure used by this class.  It maps a client side transaction id
    // to an implementation of AbstractFirstLevelMapEntry (inner classes)
    private final IdToObjectMap idToFirstLevelEntryMap = new IdToObjectMap();

    static {
        if (tc.isDebugEnabled())
            SibTr.debug(tc, "@(#) SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/TransactionToDispatchableMap.java, SIB.comms, WASX.SIB, aa1225.01 1.8");
    }

    /**
     * Adds a dispatchable for use with a specific local transaction.
     * Typically this is done by the done by the TCP channel thread
     * when it determines it is about to pass the transmission relating
     * to the start of a local transaction to the receive listener
     * dispatcher.
     * 
     * @param clientTransactionId Identifies the local transaction that
     *            the dispatchable will be used to dispatch work for.
     * @return the dispatchable to use when ordering work for the local
     *         transaction.
     * @see com.ibm.ws.sib.comms.server.clientsupport.ServerTransportReceiveListener#getThreadContext(Conversation, WsByteBuffer, int)
     */
    public Dispatchable addDispatchableForLocalTransaction(int clientTransactionId) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "addDispatchableForLocalTransaction", "" + clientTransactionId);
        if (idToFirstLevelEntryMap.containsKey(clientTransactionId)) {
            final SIErrorException exception = new SIErrorException(CommsConstants.TRANTODISPATCHMAP_ADDDISPATCHLOCALTX_01);
            FFDCFilter.processException(exception, CLASS_NAME + ".addDispatchableForLocalTransaction",
                                        CommsConstants.TRANTODISPATCHMAP_ADDDISPATCHLOCALTX_01,
                                        new Object[] { "" + clientTransactionId, idToFirstLevelEntryMap, this });
            if (tc.isEventEnabled())
                SibTr.exception(this, tc, exception);
            throw exception;
        }
        LocalFirstLevelMapEntry entry = new LocalFirstLevelMapEntry();
        Dispatchable result = entry.getDispatchable();
        idToFirstLevelEntryMap.put(clientTransactionId, entry);
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "addDispatchableForLocalTransaction", result);
        return result;
    }

    /**
     * Adds a dispatchable for use with a specific SIXAResource, which is
     * currently enlisted in a global transaction. Typically this is
     * done by the TCP channel thread when it determines that it is about
     * to pass the transmission relating to the XA_START of enlistment
     * into a global transaction to the receive listener dispatcher.
     * 
     * @param clientXAResourceId Identifies the SIXAResource that the
     *            dispatchable will be used to dispatch work for.
     * @return the dispatchable to use when ordering work for this unit
     *         of work being performed in the scope of a global transaction.
     * @see com.ibm.ws.sib.comms.server.clientsupport.ServerTransportReceiveListener#getThreadContext(Conversation, WsByteBuffer, int)
     */
    public Dispatchable addEnlistedDispatchableForGlobalTransaction(int clientXAResourceId, XidProxy xid) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "addEnlistedDispatchableForGlobalTransaction", new Object[] { "" + clientXAResourceId });
        AbstractFirstLevelMapEntry firstLevelEntry = null;

        // Locate the first level map entry - if there is one.
        if (idToFirstLevelEntryMap.containsKey(clientXAResourceId)) {
            firstLevelEntry =
                            (AbstractFirstLevelMapEntry) idToFirstLevelEntryMap.get(clientXAResourceId);
        }

        GlobalFirstLevelMapEntry entry = null;
        if (firstLevelEntry == null) {
            // If there is no first level map entry for this SIXAResource - create one
            entry = new GlobalFirstLevelMapEntry();
            idToFirstLevelEntryMap.put(clientXAResourceId, entry);
        } else {
            // This SIXAResource has already particiapted in work - as there is already
            // an instance - check that it isn't for a local transaction (which would
            // indicate a programming error).
            if (firstLevelEntry.isLocalTransaction()) {
                final SIErrorException exception = new SIErrorException(CommsConstants.TRANTODISPATCHMAP_ADDENLISTEDGLOBALTX_01);
                FFDCFilter.processException(exception, CLASS_NAME + ".addEnlistedDispatchableForGlobalTransaction",
                                            CommsConstants.TRANTODISPATCHMAP_ADDENLISTEDGLOBALTX_01,
                                            new Object[] { "" + clientXAResourceId, idToFirstLevelEntryMap, this });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }

            entry = (GlobalFirstLevelMapEntry) firstLevelEntry;
        }
        final Dispatchable result = entry.createNewEnlistedDispatchable(xid);
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "addEnlistedDispatchableForGlobalTransaction", result);
        return result;
    }

    /**
     * Obtains a dispatchable for the specified client side transaction ID.
     * The dispatchable returned will either correspond to an in-flight local
     * transaction or an inflight enlisted SIXAResource. If there is no
     * corresponding dispatchable in the table the a value of null is returned.
     * 
     * @param clientId the client id to obtain a dispatchable for
     * @return the dispatchable.
     */
    public Dispatchable getDispatchable(int clientId) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDispatchable", "" + clientId);
        AbstractFirstLevelMapEntry firstLevelEntry = null;
        if (idToFirstLevelEntryMap.containsKey(clientId)) {
            firstLevelEntry =
                            (AbstractFirstLevelMapEntry) idToFirstLevelEntryMap.get(clientId);
        }

        final Dispatchable result;

        if (firstLevelEntry == null) {
            result = null;
        } else if (firstLevelEntry.isLocalTransaction()) {
            result = ((LocalFirstLevelMapEntry) firstLevelEntry).getDispatchable();
        } else {
            result = ((GlobalFirstLevelMapEntry) firstLevelEntry).getEnlistedDispatchable();
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDispatchable", result);
        return result;
    }

    /**
     * Removes, from the table, the dispatchable corresponding to a local transaction.
     * 
     * @param clientId The client transaction id corresponding to the dispatchable to
     *            remove
     * @return the removed dispatchable or null if no entry could be found for the
     *         specified clientId parameter.
     */
    public Dispatchable removeDispatchableForLocalTransaction(int clientId) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeDispatchableForLocalTransaction", "" + clientId);
        AbstractFirstLevelMapEntry firstLevelEntry = null;
        if (idToFirstLevelEntryMap.containsKey(clientId)) {
            firstLevelEntry =
                            (AbstractFirstLevelMapEntry) idToFirstLevelEntryMap.get(clientId);
        }

        final Dispatchable result;
        if (firstLevelEntry == null) {
            result = null;
        } else {
            if (firstLevelEntry.isLocalTransaction()) {
                result = ((LocalFirstLevelMapEntry) firstLevelEntry).getDispatchable();
                idToFirstLevelEntryMap.remove(clientId);
            } else {
                final SIErrorException exception = new SIErrorException(CommsConstants.TRANTODISPATCHMAP_REMOVEFORLOCALTX_01);
                FFDCFilter.processException(exception, CLASS_NAME + ".removeDispatchableForLocalTransaction",
                                            CommsConstants.TRANTODISPATCHMAP_REMOVEFORLOCALTX_01,
                                            new Object[] { "" + clientId, firstLevelEntry, idToFirstLevelEntryMap, this });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeDispatchableForLocalTransaction", result);
        return result;
    }

    /**
     * In the event that the connection is going down, we need to ensure that the dispatchable
     * table is cleared of all references to transactions that were created by that connection.
     * 
     * @param clientId
     */
    public void removeAllDispatchablesForTransaction(int clientId) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeAllDispatchablesForTransaction", clientId);

        idToFirstLevelEntryMap.remove(clientId);

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeAllDispatchablesForTransaction");
    }

    /**
     * Removes the association between a global transaction and a dispatchable.
     * This removes the affinity between (SIXAResource, XID) pair an dispatchable from
     * the table. It is anticipated that the unit of work identified by the resource + id
     * pair will not be enlisted at the time it is removed (as this is an error - dispatchables
     * should be removed when the resource is not enlisted for the XID). If there is no
     * outstanding in-flight XIDs for the resource - it is removed completely from the
     * table.
     * 
     * @param clientId the client transaction identifier that specifies the SIXAResource that
     *            the unit of work was being carried out under.
     * @return the removed dispatchable - or null if no dispatchable matching the resource and
     *         XID parameters was present in the table.
     */
    public Dispatchable removeDispatchableForGlobalTransaction(int clientId, XidProxy xid) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeDispatchableForGlobalTransaction", new Object[] { "" + clientId });
        AbstractFirstLevelMapEntry firstLevelEntry = null;
        if (idToFirstLevelEntryMap.containsKey(clientId)) {
            firstLevelEntry =
                            (AbstractFirstLevelMapEntry) idToFirstLevelEntryMap.get(clientId);
        }

        final Dispatchable result;
        if (firstLevelEntry == null) {
            result = null;
        } else {
            if (!firstLevelEntry.isLocalTransaction()) {
                GlobalFirstLevelMapEntry globalEntry = (GlobalFirstLevelMapEntry) firstLevelEntry;
                result = globalEntry.removeDispatchable(xid);
                if (globalEntry.isEmpty())
                    idToFirstLevelEntryMap.remove(clientId);
            } else {
                final SIErrorException exception = new SIErrorException(CommsConstants.TRANTODISPATCHMAP_REMOVEFORGLOBALTX_01);
                FFDCFilter.processException(exception, CLASS_NAME + ".removeDispatchableForGlobalTransaction",
                                            CommsConstants.TRANTODISPATCHMAP_REMOVEFORGLOBALTX_01,
                                            new Object[] { "" + clientId, firstLevelEntry, idToFirstLevelEntryMap, this });
                if (tc.isEventEnabled())
                    SibTr.exception(this, tc, exception);
                throw exception;
            }
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeDispatchableForGlobalTransaction", result);
        return result;
    }

    /**
     * Adds a new dispatchable to the map for an optimized local transaction.
     * 
     * @param transactionId
     * @return
     */
    public Dispatchable addDispatchableForOptimizedLocalTransaction(int transactionId) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "addDispatchableForOptimizedLocalTransaction", "" + transactionId);
        final Dispatchable result = addDispatchableForLocalTransaction(transactionId);
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "addDispatchableForOptimizedLocalTransaction", result);
        return result;
    }

    /**
     * Inner class that is used to derive first level map entries from
     * 
     * @see idToFirstLevelEntryMap
     * @see LocalFirstLevelMapEntry
     * @see GlobalFirstLevelMapEntry
     */
    private abstract class AbstractFirstLevelMapEntry {
        public abstract boolean isLocalTransaction();
    }

    /**
     * Map entry used for dispatchables that correspond to local transactions
     * within the idToFirstLevelEntryMap.
     */
    private class LocalFirstLevelMapEntry extends AbstractFirstLevelMapEntry {
        // Dispatchable for this entry
        private final Dispatchable dispatchable = new DispatchableImpl();

        /** @return true. For this entry always represents a local transaction. */
        @Override
        public boolean isLocalTransaction() {
            return true;
        }

        /** @return the dispatchable associated wit this entry. */
        public Dispatchable getDispatchable() {
            return dispatchable;
        }
    }

    /**
     * Map entry used for dispatchables that correspond to units of work for
     * a resource enlisted into a global transaction. Instances of this
     * class are (uniquely) used by the idToFirstLevelEntryMap to act as a
     * "value" keyed from a client side "transaction id" (which for map
     * values comprised of this class will always correspond to an SIXAResource.
     */
    private class GlobalFirstLevelMapEntry extends AbstractFirstLevelMapEntry {
        private final Dispatchable dispatchable = new DispatchableImpl();
        private int refCount = 0;
        private final HashSet knownXids = new HashSet();

        /** @return false - as this is never a local transaction */
        @Override
        public boolean isLocalTransaction() {
            return false;
        }

        /**
         * @return the dispatchable associated with the unit of work for the
         *         currently enlisted SIXAResource. It is an error to invoke this method
         *         if, currently, the resource is not enlisted into a unit of work.
         */
        public Dispatchable getEnlistedDispatchable() {
            return dispatchable;
        }

        /**
         * Creates a new dispatchable for an freshly enlisted SIXAResource.
         * 
         * @return the dispatchable.
         */
        public Dispatchable createNewEnlistedDispatchable(XidProxy xid) {
            ++refCount;
            knownXids.add(xid);
            return dispatchable;
        }

        /**
         * Removes a dispatchable from the set of unenlisted dispatchables. Note that
         * every enlisted dispatchable must transition to not being enlisted before it
         * is eligable to be removed.
         * 
         * @return the dispatchable that was removed.
         */
        public Dispatchable removeDispatchable(XidProxy xid) {
            final Dispatchable result;
            if (knownXids.contains(xid)) {
                --refCount;
                result = dispatchable;
                knownXids.remove(xid);
            } else {
                result = null;
            }
            return result;
        }

        /**
         * Returns the dispatchable currently associated with the specified XID
         * 
         * @param xid the XID to return the dispatchable for
         * @return the dispatchable associated with the specified XID or null if
         *         no associated dispatchable exists.
         */
        public Dispatchable getDispatchable(Xid xid) {
            final Dispatchable result;
            if (knownXids.contains(xid))
                result = dispatchable;
            else
                result = null;
            return result;
        }

        /**
         * @return true - if (and only if) there is no enlisted dispatchable and no enlisted
         *         dispatchables for this entry. This test is used to determine whether a first
         *         level table entry should be removed from the idToFirstLevelEntryMap.
         */
        boolean isEmpty() {
            return refCount == 0;
        }
    }

    /**
     * Lightweight dispatchable implementation.
     * 
     * @see Dispatchable
     */
    private static class DispatchableImpl implements Dispatchable {
        private DispatchQueue queue;
        private final Object dispatchLockObject = new Object();
        private int refCount;

        public void setDispatchQueue(DispatchQueue queue) {
            this.queue = queue;
        }

        public DispatchQueue getDispatchQueue() {
            return queue;
        }

        public Object getDispatchLockObject() {
            return dispatchLockObject;
        }

        public void incrementDispatchQueueRefCount() {
            ++refCount;
        }

        public void decrementDispatchQueueRefCount() {
            --refCount;
        }

        public int getDispatchQueueRefCount() {
            return refCount;
        }
    }

    /**
     * For unit test use only!
     * 
     * @return the total number of dispatchables currently held within the table.
     */
    public int getTotalDispatchables() {
        int count = 0;
        Iterator i = idToFirstLevelEntryMap.iterator();
        while (i.hasNext())
            ++count;
        return count;
    }
}
