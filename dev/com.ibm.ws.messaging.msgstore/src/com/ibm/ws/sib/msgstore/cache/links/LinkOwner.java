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
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.statemodel.ListStatistics;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.Persistable;

/**
 * Defines the responsibility of a link owning other links (ie a parent node 
 * in a tree of links).
 * Allows us to flatten the inheritance hierarchy.
 * @author DrPhill
 *
 */
public abstract class LinkOwner extends AbstractItemLink
{
    private MessageStoreImpl _messageStoreImpl = null;

    /**
     * @param item
     * @param owningStreamLink
     * @param persistable
     * @throws OutOfCacheSpace
     */
    public LinkOwner(AbstractItem item, LinkOwner owningStreamLink, Persistable persistable) throws OutOfCacheSpace 
    {
        super(item, owningStreamLink, persistable);
    }

    /**
     * @param owningStreamLink
     * @param persistable
     */
    public LinkOwner(LinkOwner owningStreamLink, Persistable persistable)
    {
        super(owningStreamLink, persistable);
    }

    /**
     * @param persistable
     * @param isRootLink
     */
    public LinkOwner(Persistable persistable, boolean isRootLink)
    {
        super(persistable, isRootLink);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink#getMessageStoreImpl()
     * 
     * overridden here to cache the value at each stream, saving a lot
     * of navigation up the tree to get it.
     */
    public MessageStoreImpl getMessageStoreImpl()
    {
        if (null == _messageStoreImpl)
        {
            // lazy initialise the reference. 
            _messageStoreImpl = super.getMessageStoreImpl();
        }
        return _messageStoreImpl;
    }

    public abstract void append(final AbstractItemLink link) throws SevereMessageStoreException;
    public abstract void checkSpillLimits();    // Defect 484799
    public abstract void eventWatermarkBreached() throws SevereMessageStoreException;
    public abstract ListStatistics getListStatistics();
    public abstract void linkAvailable(AbstractItemLink link) throws SevereMessageStoreException;

    /**
     * request the receiver to reload any owned items.
     * 
     * @return true if the items were reloaded in response to
     *         this call, false otherwise.
     */
    public abstract boolean loadOwnedLinks() throws SevereMessageStoreException;
    public abstract long nextSequence();
}
