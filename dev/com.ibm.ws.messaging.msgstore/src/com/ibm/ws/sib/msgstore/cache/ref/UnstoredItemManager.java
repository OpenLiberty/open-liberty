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
 *
 * This class manages the limitation its associated items to a maximum
 * declared storage limit.
 *
 * Oversees a maximum number of bytes declared storage used by items 'in' cache.
 * The byte count is 'virtual' in that there is no dedicated buffer being consumed,
 * but just a count which is compared to a total. The difference between the count
 * and the total is called the 'free space'.
 * 
 * When an AIL is added to cache we determine if there is enough free space to add
 * it. 
 * If there isnt enough free space , we determine if there is enough discardable 
 * space to make up the difference.  If there is enough discardable space 
 * (calculated from running totals) then we attempt to free up enough space to fit 
 * in the new Indirection.  If we are successful then we can add the Indirection (and adjust total
 * counts accordingly).
 */
final class UnstoredItemManager extends IndirectionCache implements XmlConstants
{
  
    private static TraceComponent tc = SibTr.register(UnstoredItemManager.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private final IndirectionList _list = new IndirectionList();

    // maximum size of cache. passed in in constructor
    private long _maximumSize = 10000;

    private long _totalCount = 0;
    private long _totalDiscardCount = 0;
    private long _totalDiscardSize = 0;
    private long _totalRefusalCount = 0;

    UnstoredItemManager(long maximumSize, long maximumItemSize)
    {
        super();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] { Long.valueOf(maximumSize), Long.valueOf(maximumItemSize)});

        _maximumSize = maximumSize;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>", this);
    }

    private void _postAppendSlimming(final Indirection indirection) throws SevereMessageStoreException
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

                    removed.releaseIfDiscardable();
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
        boolean unlinked;
        unlinked = _list.unlink(ind);
        if (unlinked)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "unlinked: " + ind.getID() + " (size=" + _list.getCurrentCount() + ")");
        }
    }

    public final void manage(Indirection ind, AbstractItem item) throws SevereMessageStoreException
    {
        if (ind.getInMemoryItemSize() > _maximumSize)
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
        return "UnstoredItemManager(" + hashCode() + ") size=" + getCurrentSize() + " max=" + getMaximumSize();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.ref.IndirectionCache#xmlWriteOn(com.ibm.ws.sib.utils.ras.FormattedWriter)
     */
    void xmlWriteOn(FormattedWriter writer) throws IOException 
    {
        writer.newLine();
        writer.startTag(XML_UNSTORED_ITEM_MANAGER);
        writer.indent();
        _list.xmlWriteOn(writer);
        writer.outdent();
        writer.newLine();
        writer.endTag(XML_UNSTORED_ITEM_MANAGER);
    }
}
