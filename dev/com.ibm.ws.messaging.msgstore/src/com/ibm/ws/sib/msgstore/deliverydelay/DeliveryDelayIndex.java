package com.ibm.ws.sib.msgstore.deliverydelay;

/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.Comparator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.gbs.GBSTree;
import com.ibm.ws.sib.msgstore.gbs.GBSTree.Iterator;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Provides an index for DeliveryDelayableReference by wrapping the underlying tree
 * indexing mechanism. The operations are modelled on those of java.util.Treemap
 * but will be implemented by the use of a Generalised Binary Tree algorithm.
 */
public class DeliveryDelayIndex
{
    private static TraceComponent tc = SibTr.register(DeliveryDelayIndex.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private GBSTree tree = null;
    private Iterator iterator = null;
    private int size = 0;

    /**
     * Constructor to create an empty expiry index.
     */
    public DeliveryDelayIndex()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");

        // Create new GBS tree with K-factor=2 and NodeWidth=10
        tree = new GBSTree(2, 10, new DeliveryDelayComparator(), new DeliveryDelayComparator());

        iterator = tree.iterator();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Add an DeliveryDelayableReference to the DeliveryDelay index.
     * 
     * @param deliveryDelayableReference an DeliveryDelayableReference.
     * @return true if the object was added to the index successfully.
     */
    public boolean put(DeliveryDelayableReference deliveryDelayableReference)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "put", "ObjId=" + deliveryDelayableReference.getID() + " ET=" + deliveryDelayableReference.getDeliveryDelayTime());

        boolean reply = tree.insert(deliveryDelayableReference);
        if (reply)
        {
            size++;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "put", "reply=" + reply);
        return reply;
    }

    /**
     * Remove an DeliveryDelayableReference from the delivery delay index. This method
     * removes the object referenced by the preceding call to next().
     * 
     * @return true if the object was removed from the index successfully.
     */
    public boolean remove()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "remove");

        boolean reply = iterator.remove();

        if (reply)
        {
            size--;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "remove", "reply=" + reply);
        return reply;
    }

    /**
     * Remove a specific DeliveryDelayableReference from the delivery delay index. This method
     * removes the object directly from the tree and does not use the iterator.
     * It does not therefore require a prior call to next().
     * 
     * @param deliveryDelayableReference the DeliveryDelayableReference to be removed.
     * @return true if the object was removed from the index successfully.
     */
    public boolean remove(DeliveryDelayableReference deliveryDelayableReference)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "remove",
                        (deliveryDelayableReference == null ? "null" : "ObjId=" + deliveryDelayableReference.getID() + " ET=" + deliveryDelayableReference.getDeliveryDelayTime()));

        boolean reply = tree.delete(deliveryDelayableReference);
        if (reply)
        {
            size--;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "remove", "reply=" + reply);
        return reply;
    }

    /**
     * Re-create the iterator so that a subsequent call to next() will
     * start at the beginning of the index.
     */
    public void resetIterator()
    {
        iterator.reset();
        return;
    }

    /**
     * Return the number of objects in the tree
     * 
     * @return the number of objects
     */
    public int size()
    {
        return size;
    }

    /**
     * Return the next DeliveryDelayable reference from the DeliveryDelay index via the iterator.
     * 
     * @return the DeliveryDelayable reference, or null if there are no more
     *         items in the index.
     */
    public DeliveryDelayableReference next()
    {
        return (DeliveryDelayableReference) iterator.next();
    }

    /**
     * This class provides a comparator to be used by the GBS tree algorithms. It
     * compares the delivery delay time and (if necessary) the object IDs of two objects.
     * The same comparator is used for inserts, deletes and searches.
     */
    private static class DeliveryDelayComparator implements Comparator
    {
        /**
         * Compare two objects and return a value representing their respective
         * collating sequence. This uses 'expiry time' as the primary key and 'object ID'
         * as the secondary key.
         * 
         * @param o1 the first object.
         * @param o2 the second object.
         * @return zero if the objects are equal, -1 if o1 is less than o2, else 1.
         */
        @Override
        public int compare(Object o1, Object o2)
        {
            DeliveryDelayableReference ref1 = (DeliveryDelayableReference) o1;
            DeliveryDelayableReference ref2 = (DeliveryDelayableReference) o2;

            long time1 = ref1.getDeliveryDelayTime();
            long time2 = ref2.getDeliveryDelayTime();

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
