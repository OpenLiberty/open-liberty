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

import java.io.IOException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemMembership;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.utils.ras.SibTr;

/*
 * <p>implement Transaction callback so we can use the transaction to notify
 * us at completion when we want to tell the item that item reference count
 * has dropped to zero.</p>
 */
public final class ItemLink extends AbstractItemLink implements ItemMembership, TransactionCallback
{
  
    private static TraceComponent tc = SibTr.register(ItemLink.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    // The reference count increments when the add is added to the transaction, and decrements when the remove commits - 258179
    private volatile int _referenceCount = 0;
    private boolean _referenceCountIsDecreasing = false;

    /**
     * 
     * @param item
     * @param owningStreamLink
     * @param persistable
     * 
     * @throws MessageStoreException
     * @throws OutOfCacheSpace
     */
    public ItemLink(final AbstractItem item, final LinkOwner owningStreamLink, final Persistable persistable) throws OutOfCacheSpace 
    {
        super(item, owningStreamLink, persistable);
    }

    /**
     * 
     * @param owningStreamLink
     * @param persistable
     * 
     * @throws MessageStoreException
     */
    public ItemLink(final LinkOwner owningStreamLink, final Persistable persistable)
    {
        super(owningStreamLink, persistable);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Link#assertCanDelete(long)
     */
    public SevereMessageStoreException assertCanDelete(final PersistentTransaction transaction)
    {
    	SevereMessageStoreException ex = super.assertCanDelete(transaction);
        if (null == ex)
        {
            if (0 < _referenceCount)
            {
                ex = new SevereMessageStoreException("Cannot delete Item with references");
                FFDCFilter.processException(ex, "ItemLink.delete", "1:111:1.104.1.1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception :  Cannot delete Item with references");
            }
        }
        return ex;
    }

    /**
     * This method is called when committing the removal of a reference.
     * It should only be called by the message store code.
     * @param transaction The transaction
     * @throws SevereMessageStoreException 
     */
    public final synchronized void commitDecrementReferenceCount(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commitDecrementReferenceCount");

        if (_referenceCount < 1)
        {
            SevereMessageStoreException e = new SevereMessageStoreException("Reference count decrement cannot be committed");
            FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.ItemLink.commitDecrementReferenceCount", "1:131:1.104.1.1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Reference count decrement cannot be committed");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commitDecrementReferenceCount");
            throw e;
        }
        _referenceCount--;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "reference count dropped to: " + _referenceCount);

        if (0 == _referenceCount)
        {
            // This causes the item's itemReferencesDroppedToZero to be called after
            // completion of this transaction
            transaction.registerCallback(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commitDecrementReferenceCount");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemMembership#getOwningItemStream()
     */
    public final ItemStream getOwningItemStream() throws SevereMessageStoreException
    {
        return((ItemStreamLink) getOwningStreamLink()).getItemStream();
    }

    public final int getReferenceCount()
    {
        return _referenceCount;
    }

    /**
     * This method is called when a reference is being added by an active transaction
     * and when a reference is being restored.
     * It should only be called by the message store code. 
     * @throws SevereMessageStoreException 
     */
    public final synchronized void incrementReferenceCount() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "incrementReferenceCount");

        if (_referenceCountIsDecreasing)
        {
        	if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot increment! Reference count has begun decreasing.");
        	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "incrementReferenceCount");
            throw new SevereMessageStoreException("Cannot add more references to an item after one has been removed");
        }
        _referenceCount++;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "incrementReferenceCount");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink#internalCanExpire()
     */
    protected boolean internalCanExpire()
    {
        boolean can = super.internalCanExpire();
        if (can && _referenceCount > 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "preventing expiry as references remain");

            can = false;
        }
        return can;
    }

    /**
     * This method is called when rolling back the addition of a reference.
     * It should only be called by the message store code. 
     * @param transaction The transaction
     * @throws SevereMessageStoreException 
     */
    public final synchronized void rollbackIncrementReferenceCount(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "rollbackIncrementReferenceCount");

        if (_referenceCount < 1)
        {
        	SevereMessageStoreException e = new SevereMessageStoreException("Reference count increment cannot be rolled back");
            FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.ItemLink.rollbackIncrementReferenceCount", "1:212:1.104.1.1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Reference count increment cannot be rolled back");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollbackIncrementReferenceCount");
            throw e;
        }
        _referenceCount--;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollbackIncrementReferenceCount");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "ItemLink(" 
        + getID() 
        + ")" 
        + super.toString()
        + " state=" 
        + getState() 
        + " refCount="
        + _referenceCount
        + " refCountDecreasing="
        + Boolean.toString(_referenceCountIsDecreasing)
        ;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink#xmlTagName()
     */
    protected final String xmlTagName()
    {
        return XML_ITEM;
    }

    protected void xmlWriteAttributesOn(FormattedWriter writer) throws IOException 
    {
        super.xmlWriteAttributesOn(writer);
        writer.write(" refCount=\"");
        writer.write(Integer.toString(_referenceCount));
        writer.write("\" refCountDecreasing=\"");
        writer.write(Boolean.toString(_referenceCountIsDecreasing));
        writer.write('"');
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public void beforeCompletion(TransactionCommon transaction)
    {
        // do nothing - this callback is not used but its afterCompletion partner is
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#afterCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction, boolean)
     */
    public void afterCompletion(TransactionCommon transaction, boolean committed)
    {
        // When the reference count is about to reach zero, this class registers *itself* as
        // a transaction callback. This method then gets called after the transaction
        // completes and the item's itemReferencesDroppedToZero callback is called.
        if (isAvailable())
        {
        	try
        	{
        		Item item = (Item) getItem();
        		if (null != item)
        		{
        			item.itemReferencesDroppedToZero();
        		}
			} 
        	catch (SevereMessageStoreException e) 
        	{
	            FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.ItemLink.afterCompletion", "1:286:1.104.1.1");
	            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught attempting to drop item reference count to zero!", e);
			}

        }
    }
}
