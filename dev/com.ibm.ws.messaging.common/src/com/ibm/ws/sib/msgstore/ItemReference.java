package com.ibm.ws.sib.msgstore;
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

import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The reference count is incremented when a reference to the item is presented
 * to an {@link ItemStream}.
 * The reference count is decremented when:
 * <ul>
 * <li>a transaction adding a reference rolls back</li>
 * <li>a transaction removing a reference commits</li>
 * </ul>
 * <p>
 * A reference to an item can only be put to a {@link ReferenceStream} in the same
 * transaction that the item is put to an {@link ItemStream}.  This is so that
 * we can perform reference counting on commits.
 * </p>
 * <p>
 * A reference can only be placed into a {@link ReferenceStream} that is contained
 * directly in the same {@link ItemStream} as the referred {@link Item}.
 * </p>
 * <p>
 * It is possible for the referred item to be removed while the reference
 * still exists.  This will result in {@link #getReferredItem()} returning null.
 * </p>
 */
public class ItemReference extends AbstractItem
{
    private static TraceComponent tc = SibTr.register(ItemReference.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);
    private Item _item;

    /* Reference to the real Item.  Note that this is an Item rather
     * than an abstractItem.  Also note that the reference may be invalid if
     * the item has been cleaned up.
     * We cannot have it final due to the lazy initialization after a restore
     * from persistence.
     */
    private ItemMembership _referredMembership;

    /** PK57207 Tuning for msg reference sizes */
    private boolean _sizeRefsByMsgSize = false;

    /**
     * Require default constructor
     */
    public ItemReference()
    {
        super();
    }

    /**
     * Creates a reference to an item.
     */
    public ItemReference(final Item item)
    {
        super();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

        _item = item;
        _referredMembership = (ItemMembership)item._getMembership();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    /**
     * @return the referred {@link Item}.  If the referred {@link Item}
     * has been removed then null will be returned.
     * @throws SevereMessageStoreException
     *
     */
    public final Item getReferredItem() throws SevereMessageStoreException
    {
        if (null == _item)
        {
            if (null == _referredMembership)
            {
                // may be because it has not been set as a result
                // of restore from DB
                ReferenceMembership m = (ReferenceMembership)_getMembership();
                if (null == m)
                {
                    throw new NotInMessageStore();
                }
                if (null != m)
                {
                    long refID = m.getReferencedID();
                    if (NO_ID != refID)
                    {
                        MessageStore messageStore = getOwningMessageStore();
                        if (null != messageStore)
                        {
                            _referredMembership = (ItemMembership) messageStore.getMembership(refID);
                        }
                    }
                }
            }
            if (null != _referredMembership)
            {
                _item = (Item) _referredMembership.getItem();
            }
        }
        return _item;
    }

    /**
     * @see AbstractItem#getPriority()
     * Overridden here to return, by default, the priority of the referred
     * item.
     */
    public int getPriority()
    {
        if (null != _item)
        {
            return _item.getPriority();
        }
        return _referredMembership.getPriority();
    }

    /**
     * @return true if the receiver is an instance of
     * {@link ItemReference},  false otherwise. Default
     * implementation returns false.
     * Overridden here to return true.
     */
    public final boolean isItemReference()
    {
        return true;
    }

    //FSIB0112b.ms.1
    public void restore(final List<DataSlice> dataSlices) throws PersistentDataEncodingException, SevereMessageStoreException
    {
        super.restore(dataSlices);
        long referredID = ((ReferenceMembership)_getMembership()).getReferencedID();
        _referredMembership = (ItemMembership) getOwningMessageStore().getMembership(referredID);
    }

    /**
     * @return The {@link ItemStream} in which the receiver is stored, or null
     * if none.
     * @throws SevereMessageStoreException
     */
    public final ReferenceStream getReferenceStream() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getReferenceStream");

        ReferenceStream referenceStream = null;
        ReferenceMembership m = ((ReferenceMembership)_getMembership());
        if (null == m)
        {
            throw new NotInMessageStore();
        }
        if (null != m)
        {
            referenceStream = m.getOwningReferenceStream();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getReferenceStream", referenceStream);
        return referenceStream;
    }

    /** PK57207 Called when adding this reference to a
     *  reference stream when to specify that
     *  sib.msgstore.jdbcSpillSizeRefsByMsgSize has been enabled */
    public void setSizeRefsByMsgSize(boolean sizeByMsgSize)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setSizeRefsByMsgSize", Boolean.valueOf(sizeByMsgSize));

        this._sizeRefsByMsgSize = sizeByMsgSize;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setSizeRefsByMsgSize");
    }

    /** PK57207
     *  @return Whether message references should return the
     *          persistent data size of the msg they reference */
    public boolean getSizeRefsByMsgSize()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSizeRefsByMsgSize");

        boolean retval = this._sizeRefsByMsgSize;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSizeRefsByMsgSize", Boolean.valueOf(retval));
        return retval;
    }

    /**
     * PK57207 Returns an estimated size for this message reference
     */
    public int getInMemoryDataSize()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getInMemoryDataSize");
        int dataSize;

        // If tuning has requested return the size of the message we reference,
        // then delegate to the message. Otherwise call our parent to get a
        // (small) default size.
        if (_sizeRefsByMsgSize)
        {
            try
            {
                dataSize = getReferredItem().getInMemoryDataSize();
            }
            catch (SevereMessageStoreException e)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(e,"com.ibm.ws.sib.msgstore.ItemReference.getInMemoryDataSize","244",this);
                // After FFDCing anything nasty, fall back to the standard answer
                dataSize = super.getInMemoryDataSize();
            }
        }
        else
        {
            dataSize = super.getInMemoryDataSize();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getInMemoryDataSize",  dataSize);
        return dataSize;
    }
}
