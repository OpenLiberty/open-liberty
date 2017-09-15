package com.ibm.ws.sib.msgstore.cache.ref;
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
import java.io.IOException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.CacheStatistics;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;

import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class controls the choice of cache which manages item memory. Indirections
 * {@link #register(Indirection, int)} with this and receive a reference to a
 * {@link com.ibm.ws.sib.msgstore.cache.ref.IndirectionCache}. The indirection
 * then interacts with the indirection cache, and so the reference should be 
 * stored for future use. 
 * 
 * This class selects the particular storage manager to be used, and acts as
 * a central point to collect statistics.
 * 
 * We have two separate caches, one for STORE_NEVER, and one for the rest.
 * For want of a better name we will call them 'unstored' and 'stored'.
 */
public final class ItemStorageManager implements MessageStoreConstants, CacheStatistics, XmlConstants
{
   
    /* We are very bad at calculating the amount of memory consumed per message
     * cached. As a result we use up a lot more than we claim in the 
     *     PROP_STORED_CACHE_SIZE
     * and
     *     PROP_UNSTORED_CACHE_SIZE
     * variables.
     */
    private static TraceComponent tc = SibTr.register(ItemStorageManager.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);
   
    private StoredItemManager _storedItemManager = null;
    private UnstoredItemManager _unstoredItemManager = null;

    /**
     * This method is used to provide an Item with the correct storage
     * manager to use throughout its lifetime dependant on its storage
     * strategy.
     * 
     * @param storageStrategy
     *               The storage strategy of the item that is registering.
     * 
     * @return a reference to the cache where this Indirection is managed.
     *         This reference should be stored for future use.
     */
    public final IndirectionCache register(int storageStrategy)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "register", "StorageStrategy="+storageStrategy);

        IndirectionCache cache = null;

        if (AbstractItem.STORE_NEVER == storageStrategy)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Using UnstoredItemManager");

            // Defect 601995
            cache = _unstoredItemManager;
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Using StoredItemManager");

            // Defect 601995
            cache = _storedItemManager;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "register", cache);
        return cache;
    }



    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.CacheStatistics#getCurrentCount()
     */
    public final long getCurrentCount()
    {
        long result = 0;
        result = result + _storedItemManager.getCurrentCount();
        result = result + _unstoredItemManager.getCurrentCount();
        return result;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.CacheStatistics#getCurrentSize()
     */
    public final long getCurrentSize()
    {
        long result = 0;
        result = result + _storedItemManager.getCurrentSize();
        result = result + _unstoredItemManager.getCurrentSize();
        return result;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getDiscardableSize()
     */
    public final long getDiscardableSize()
    {
        long result = 0;
        result = result + _storedItemManager.getStatistics().getDiscardableSize();
        result = result + _unstoredItemManager.getStatistics().getDiscardableSize();
        return result;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.CacheStatistics#getMaximumSize()
     */
    public final long getMaximumSize()
    {
        long result = 0;
        result = result + _storedItemManager.getMaximumSize();
        result = result + _unstoredItemManager.getMaximumSize();
        return result;
    }

    public final CacheStatistics getNonStoredCacheStatistics()
    {
        return _unstoredItemManager.getStatistics();
    }

    public final CacheStatistics getStoredCacheStatistics()
    {
        return _storedItemManager.getStatistics();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getTotalCount()
     */
    public final long getTotalCount()
    {
        long result = 0;
        result = result + _storedItemManager.getStatistics().getTotalCount();
        result = result + _unstoredItemManager.getStatistics().getTotalCount();
        return result;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.CacheStatistics#getTotalDiscardCount()
     */
    public final long getTotalDiscardCount()
    {
        long result = 0;
        result = result + _storedItemManager.getTotalDiscardCount();
        result = result + _unstoredItemManager.getTotalDiscardCount();
        return result;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.CacheStatistics#getTotalDiscardSize()
     */
    public final long getTotalDiscardSize()
    {
        long result = 0;
        result = result + _storedItemManager.getTotalDiscardSize();
        result = result + _unstoredItemManager.getTotalDiscardSize();
        return result;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getTotalRefusalCount()
     */
    public final long getTotalRefusalCount()
    {
        long result = 0;
        result = result + _storedItemManager.getStatistics().getTotalRefusalCount();
        result = result + _unstoredItemManager.getStatistics().getTotalRefusalCount();
        return result;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getTotalSize()
     */
    public final long getTotalSize()
    {
        long result = 0;
        result = result + _storedItemManager.getStatistics().getTotalSize();
        result = result + _unstoredItemManager.getStatistics().getTotalSize();
        return result;
    }

    public final void initialize(MessageStoreImpl messageStore) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initialize");

        String str = messageStore.getProperty(PROP_STORED_CACHE_SIZE, PROP_STORED_CACHE_SIZE_DEFAULT);
        long maximumSize = Long.parseLong(str);

        str = messageStore.getProperty(PROP_STORED_CACHE_MAXIMUM_ITEM_SIZE, PROP_STORED_CACHE_MAXIMUM_ITEM_SIZE_DEFAULT);
        long maximumItemSize = Long.parseLong(str);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(this, tc, "StoredItemManager size = " + maximumSize);
            SibTr.debug(this, tc, "StoredItemManager max size = " + maximumItemSize);
        }

        if (maximumSize > 0)
        {
            _storedItemManager = new StoredItemManager(maximumSize, maximumItemSize);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initialize");
            throw new SevereMessageStoreException("invalid setting: " + PROP_STORED_CACHE_SIZE + "=" + maximumSize); // Defect 585163
        }

        str = messageStore.getProperty(PROP_UNSTORED_CACHE_SIZE, PROP_UNSTORED_CACHE_SIZE_DEFAULT);
        maximumSize = Long.parseLong(str);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            SibTr.debug(this, tc, "UnstoredItemManager size = " + maximumSize);
            SibTr.debug(this, tc, "UnstoredItemManager max size = " + maximumItemSize);
        }
        if (maximumSize > 0)
        {
            _unstoredItemManager = new UnstoredItemManager(maximumSize, maximumItemSize);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initialize");
            throw new SevereMessageStoreException("invalid setting: " + PROP_UNSTORED_CACHE_SIZE + "=" + maximumSize); // Defect 585163
        }

        

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initialize");
    }

   
    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#resetTotals()
     */
    public final void resetTotals()
    {
        _storedItemManager.getStatistics().resetTotals();
        _unstoredItemManager.getStatistics().resetTotals();
    }

    /**
     * @param writer
     */
    public final void xmlWriteOn(FormattedWriter writer) throws IOException 
    {
        writer.newLine();
        writer.startTag(XML_ITEM_STORAGE_MANAGER);
        writer.indent();
        _storedItemManager.xmlWriteOn(writer);
        _unstoredItemManager.xmlWriteOn(writer);
        writer.outdent();
        writer.newLine();
        writer.endTag(XML_ITEM_STORAGE_MANAGER);
    }
}
