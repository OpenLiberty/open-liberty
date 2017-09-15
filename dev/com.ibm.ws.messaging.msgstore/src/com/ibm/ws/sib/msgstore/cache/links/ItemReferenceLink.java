package com.ibm.ws.sib.msgstore.cache.links;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemReference;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.ReferenceMembership;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

final class ItemReferenceLink extends AbstractItemLink implements ReferenceMembership
{
   

    private static TraceComponent tc = SibTr.register(ItemReferenceLink.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    /**
     * Create new Item link
     * 
     * @param itemReference
     * @param owningStreamLink
     * @param persistable
     * 
     * @exception OutOfCacheSpace
     * @throws SevereMessageStoreException 
     */
    ItemReferenceLink(final ItemReference itemReference, final ReferenceStreamLink owningStreamLink, final Persistable persistable) throws OutOfCacheSpace, SevereMessageStoreException 
    {
        super(itemReference, owningStreamLink, persistable);

        // note the referred item ID but dont increment the reference count yet.
        // we can incremment the count when we commit the add.
        Item referredItem = itemReference.getReferredItem();
        // check these here so we can do it without a compile-time error
        // if we get away with this we can try for a check in ItemReference constructor
        if (referredItem.isItemStream())
        {
            throw new SevereMessageStoreException("cannot have reference to itemStream");
        }
        else if (referredItem.isReferenceStream())
        {
            throw new SevereMessageStoreException("cannot have reference to referenceStream");
        }
        else if (referredItem.isItemReference())
        {
            throw new SevereMessageStoreException("cannot have reference to reference");
        }

        long _referredID = referredItem.getID();
        getTuple().setReferredID(_referredID);
        if (AbstractItem.NO_ID == _referredID)
        {
            throw new SevereMessageStoreException("Restored reference with invalid ID");
        }
        else
        {
            MessageStoreImpl msi = (MessageStoreImpl) getMessageStore();
            ItemLink itemLink = (ItemLink) msi.getMembership(_referredID);
            if (null != itemLink)
            {
                itemLink.incrementReferenceCount();
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "reference to non-existent item: " + _referredID);
            }
        }
    }

    /**
     * Recreate a link from a tuple.  Note that we only create the
     * link, and do not necessarily create the item.
     * 
     * @param owningStreamLink
     * @param persistable
     * @throws SevereMessageStoreException 
     */
    ItemReferenceLink(final ReferenceStreamLink owningStreamLink, final Persistable persistable) throws SevereMessageStoreException
    {
        super(owningStreamLink, persistable);
        long _referredID = persistable.getReferredID();
        getTuple().setReferredID(_referredID);
        if (AbstractItem.NO_ID == _referredID)
        {
            throw new SevereMessageStoreException("Restored reference with invalid ID");
        }
        else
        {
            MessageStoreImpl msi = (MessageStoreImpl) getMessageStore();
            ItemLink itemLink = (ItemLink) msi.getMembership(_referredID);
            if (null != itemLink)
            {
                itemLink.incrementReferenceCount();
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "reference to non-existent item: " + _referredID);
            }
        }
    }

    // if we are a reference then keep the id of the referred item
    // we may be able to do this in a neater fashion later. 
    //private long _referredID = AbstractItem.NO_ID;

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#postAbort(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void abortAdd(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "abortAdd", transaction);

        long _referredID = getTuple().getReferredID();
        super.abortAdd(transaction);
        if (AbstractItem.NO_ID != _referredID)
        {
            MessageStoreImpl msi = getMessageStoreImpl();
            ItemLink itemLink = (ItemLink) msi.getMembership(_referredID);
            if (null != itemLink)
            {
                itemLink.rollbackIncrementReferenceCount(transaction);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    SibTr.debug(this, tc, "reference to non-existent item: " + _referredID);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "abortAdd");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemMembership#getOwningItemStream()
     */
    public final ReferenceStream getOwningReferenceStream() throws SevereMessageStoreException
    {
        return((ReferenceStreamLink) getOwningStreamLink()).getReferenceStream();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.store.MessageStore.Membership#getReferencedID()
     */
    public final long getReferencedID()
    {
        return getTuple().getReferredID();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getReferredID()
     */
    public final long getReferredID()
    {
        return getTuple().getReferredID();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.TransactionalList.Link#eventCommittedGet(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void internalCommitRemove(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "internalCommitRemove");

        long _referredID = getTuple().getReferredID();
        super.internalCommitRemove(transaction);
        if (AbstractItem.NO_ID != _referredID)
        {
            MessageStoreImpl msi = getMessageStoreImpl();
            ItemLink itemLink = (ItemLink) msi.getMembership(_referredID);
            if (null != itemLink)
            {
                itemLink.commitDecrementReferenceCount(transaction);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    SibTr.debug(this, tc, "reference to non-existent item: " + _referredID);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "internalCommitRemove");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "ItemReferenceLink(" 
        + getID() 
        + " --> " 
        + getReferencedID() 
        + ")" 
        + super.toString()
        + " state=" 
        + getState();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink#xmlTagName()
     */
    protected final String xmlTagName()
    {
        return XML_REFERENCE;
    }

}
