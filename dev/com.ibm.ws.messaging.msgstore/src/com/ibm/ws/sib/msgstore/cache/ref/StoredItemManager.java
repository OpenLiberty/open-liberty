package com.ibm.ws.sib.msgstore.cache.ref;
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
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.CacheStatistics;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The StoredItemManager is designed to hold (temporarily) in memory 
 * items which could safely be discarded because they have a persistent 
 * representation. The items are retained in the hope that this will save a 
 * disk read at some time in the near future.
 * The cache should only be used for items that are stored persistently.  
 * This is for  two reasons:
 * <ul> 
 * <li>Since we have a disk representation we will retain the item link in
 * the parent item stream.  Thus we do not need to invoke any unlinking in
 * the stream and can avoid some lock-taking. This can only help improve 
 * performance</li>
 * <li>Since we have a disk representation, any minor glitch, or thread race
 * condition that causes us to spill can will result in loss of time rather
 * than in a loss of data. (We will pay a penalty for an 'extra' read.)</li>
 * </ul>
 *
 * When indirections become discardable they are added to the internal list, 
 * and their size is added to the discardable size.  When indirections become
 * non-discardable they are removed from the internal list and their size is
 * subtracted from the discardable size.
 * 
 * When there is insufficient discardable size, some indirections are triggered
 * to release their items and the discardable size can then be reduced.
 * 
 * Discarding is done when an indirection is added:
 * <ul> 
 * <li>If the new item is too large, then it is discarded. 'Too Large' is defined
 * by a parameter passed in at creation time and defaults to 100k.</li>
 * <li>If adding the new item will not take cause the discardable size to exceed 
 * the maximum, then the item is added, and the discardable size is increased.</li>
 * <li>If adding the new item will take cause the discardable size to exceed 
 * the maximum, then the cache is slimmed until the new item will fit.</li>
 * </ul>
 *  
 * This version only worries about discardable memory - the limit
 * does not apply to non-discardable memory.
 * 
 * @author DrPhill
 *
 */
final class StoredItemManager extends IndirectionCache implements XmlConstants
{
 
    private static TraceComponent tc = SibTr.register(StoredItemManager.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private final IndirectionList _list = new IndirectionList();

    // maximum size of cache. passed in in constructor
    private long _maximumSize = 10000;

    // Maximum size of item allowed in cache. Passed in in constructor.
    private long _maximumItemSize = 100000;

    private long _totalCount = 0;
    private long _totalDiscardCount = 0;
    private long _totalDiscardSize = 0;
    private long _totalRefusalCount = 0;

    StoredItemManager(long maximumSize, long maximumItemSize)
    {
        super();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] { Long.valueOf(maximumSize), Long.valueOf(maximumItemSize)});

        _maximumSize = maximumSize;
        _maximumItemSize = maximumItemSize;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>", this);
    }

    private void _postAppendSlimming(final Indirection indirection)
    {
        if (_list.getCurrentSize() > _maximumSize)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "begin discarding indirections as size (" + _list.getCurrentSize() + ") > max (" + _maximumSize + ")");

            while (_list.getCurrentSize() > _maximumSize)
            {
                final Indirection removed = _list.removeFirst();
                if (null == removed)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "list empty");
                    break;
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "unlinking " + removed.getID());

                    _totalDiscardCount += 1;
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "end discarding indirections new size = " + _list.getCurrentSize());
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getCurrentCount()
     */
    public final long getCurrentCount()
    {
        return _list.getCurrentCount();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getCurrentSize()
     */
    public final long getCurrentSize()
    {
        return _list.getCurrentSize();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getDiscardableSize()
     */
    public final long getDiscardableSize()
    {
        return _list.getCurrentSize();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getMaximumSize()
     */
    public final long getMaximumSize()
    {
        return _maximumSize;
    }

    final CacheStatistics getStatistics()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getTotalCount()
     */
    public final long getTotalCount()
    {
        return _totalCount;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getTotalDiscardCount()
     */
    public final long getTotalDiscardCount()
    {
        return _totalDiscardCount;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getTotalDiscardSize()
     */
    public final long getTotalDiscardSize()
    {
        return _totalDiscardSize;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getTotalRefusalCount()
     */
    public final long getTotalRefusalCount()
    {
        return _totalRefusalCount;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#getTotalSize()
     */
    public final long getTotalSize()
    {
        return _totalDiscardSize;
    }

    public final void unmanage(Indirection ind) throws SevereMessageStoreException
    {
        boolean unlinked = _list.unlink(ind);
        if (unlinked)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "unlinked: " + ind.getID() + " (size=" + _list.getCurrentCount() + ")");
        }
    }

    public final void manage(Indirection ind, AbstractItem item)
    {
        if (ind.getInMemoryItemSize() > _maximumItemSize)
        {
            // if the item is too big we will just discard the item
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "item too large to cache, so just hold with weak reference");

            synchronized (this)
            {
                _totalRefusalCount += 1;
            }
        }
        else
        {
            boolean appended = _list.append(ind, item);
            if (appended)
            {
                _postAppendSlimming(ind);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.CacheStatistics#resetTotals()
     */
    public synchronized final void resetTotals()
    {
        _totalDiscardCount = 0;
        _totalDiscardSize = 0;
        _totalCount = 0;
        _totalRefusalCount = 0;
    }

    public final String toString()
    {
        return "StoredItemManager(" + hashCode() + ") size=" + getCurrentSize() + " max=" + getMaximumSize();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.ref.IndirectionCache#xmlWriteOn(com.ibm.ws.sib.utils.ras.FormattedWriter)
     */
    final void xmlWriteOn(FormattedWriter writer) throws IOException 
    {
        writer.newLine();
        writer.startTag(XML_STORED_ITEM_MANAGER);
        writer.indent();
        _list.xmlWriteOn(writer);
        writer.outdent();
        writer.newLine();
        writer.endTag(XML_STORED_ITEM_MANAGER);
    }
}
