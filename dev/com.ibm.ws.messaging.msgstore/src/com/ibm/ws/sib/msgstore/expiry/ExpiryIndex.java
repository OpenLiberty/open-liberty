package com.ibm.ws.sib.msgstore.expiry;
/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;

import com.ibm.websphere.ras.TraceComponent;

import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Provides an index for ExpirableReferences by wrapping the underlying tree
 * indexing mechanism.
 */
public class ExpiryIndex
{
    private static TraceComponent tc = SibTr.register(ExpiryIndex.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private final ConcurrentSkipListSet<ExpirableReference> tree;

    /**
     * Create an empty expiry index.
     */
    public ExpiryIndex() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

        tree = new ConcurrentSkipListSet<ExpirableReference>(new ExpiryComparator());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>", new Object[] {tree});
    }

    /**
     * Add an ExpirableReference to the expiry index.
     * @param expirable an ExpirableReference.
     * @return true if the ExpirableReference was not already in the set of ExpirableReferences.
     */
    public boolean put(ExpirableReference expirable) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "put", "ObjecId=" + expirable.getID() + " ExpiryTime=" + expirable.getExpiryTime());

        boolean added = tree.add(expirable);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "put", "added=" + added);
        return added;
    }

    /**
     * @return the first ExpirableReference in the expiry index, this is the next ExpirableReference to expire.
     * @throws NoSuchElementException if the index is empty.
     */
    public ExpirableReference first() throws NoSuchElementException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "first");

        ExpirableReference first = tree.first();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "first", first);
        return first;
    }

    /**
     * Remove an ExpirableReference from this ExpiryIndex.
     *
     * @param expirableReference the ExpirableReference to be removed.
     * @return true if the ExpirableReference was removed from the index.
     */
    public boolean remove(ExpirableReference expirableReference)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "remove", (expirableReference == null ? "null" : "ObjectId=" + expirableReference.getID() + " ExpiryTime=" + expirableReference.getExpiryTime()));

        if (expirableReference == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "remove", " expirableReference=null");
            return false;
        }

        boolean removed = tree.remove(expirableReference);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "remove", "removed=" + removed);
        return removed;
    }

    /**
     * Scan all of the ExpirableReferences in the index. If the weak reference is null or if the item indicates
     * that it has gone from the store, then remove the expirableReferece from the index.
     */
    long cleaned = 0;
    Object cleanLock = new Object();
    public long clean() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "clean", "isEmpty="+tree.isEmpty());

        synchronized (cleanLock) {
            cleaned = 0;
            for (ExpirableReference expirableReference : tree) {
                Expirable expirable = expirableReference.get();
                if (expirable == null || !(expirable.expirableIsInStore())) {
                    boolean removed = tree.remove(expirableReference);
                    if (removed)
                        cleaned++;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Removed (cleaned) removed=" + removed + " ExpiryTime=" + expirableReference.getExpiryTime() + " objectId=" + expirableReference.getID());
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "clean", "isEmpty="+tree.isEmpty());
        return cleaned;
    }

    /**
     * Returns {@code true} if there are no ExpirableReferences in the index
     * @return {@code true} if this tree contains no ExpirableReferences
     */
    public boolean isEmpty() {
        return tree.isEmpty();
    }

    /**
     * Return the number of ExpirableReferences in the tree.
     *
     * @return the number of ExpirableReferences
     */
    public int size() {
        return tree.size();
    }

    /**
     * The comparator to be used by the ConcurrentSkipListset of ExpirableReferences.
     */
    private static class ExpiryComparator implements Comparator<ExpirableReference> {
        /**
         * Compare the expiry times of ExpirableReferences,
         * in the case of equal times compare the IDs of two ExpirableReferences.
         * This allows insertion of ExpirableRefrences with identical expiry times and presents
         * the set ordered by expiry time with soonest times first.
         *
         * @param ref1 the first ExpirableReference.
         * @param ref2 the second ExpirableReference.
         * @return zero if the ExpirableReferences are equal, -1 if ref1 has a earlier expiry time than ref2 or the expiry times are equal and ref1.Id is less than ref2.Id,
         * otherwise return 1.
         */
    	@Override
        public int compare(ExpirableReference ref1, ExpirableReference ref2) {
            long time1 = ref1.getExpiryTime();
            long time2 = ref2.getExpiryTime();

            if (time1 == time2)
            {
                long id1 = ref1.getID();
                long id2 = ref2.getID();

                if (id1 == id2)
                {
                    return 0;
                }
                else if (id1 < id2)
                {
                    return -1;
                }
                else
                {
                    return 1;
                }
            }
            else if (time1 < time2)
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
    }
}