package com.ibm.ws.sib.msgstore.list;
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
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.cache.links.Priorities;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

public final class PrioritizedList 
{
   
    private static TraceComponent tc = SibTr.register(PrioritizedList.class,
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    // Head of the list of cursors for this list. Updates to the list are performed underneath
    // this object's monitor
    private PrioritizedCursor _firstCursor = null;

    // Read-write lock for the list of cursors. Updates to the structure of the list must lock
    // exclusively. Reads of the structure of the list need only lock shared.
    private ReadWriteLock _cursorLock = new ReadWriteLock();

    // Sublists are in numerical priority order 0..9 so we need to scan from top downwards to get
    // correct (highest to lowest) priority order.
    private final LinkedList[] _prioritySublists = new LinkedList[Priorities.NUMBER_OF_PRIORITIES];

    public PrioritizedList() {}

    final void _removeCursor(final PrioritizedCursor removeCursor)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "_removeCursor", removeCursor);
        }

        boolean locked = false;
        try
        {
            _cursorLock.lockExclusive();
            locked = true;

            PrioritizedCursor prev = removeCursor.getPreviousCursor(); 
            PrioritizedCursor next = removeCursor.getNextCursor();
            if (prev != null)
            {
                prev.setNextCursor(next);
                removeCursor.setPreviousCursor(null);
            }
            if (next != null)
            {
                next.setPreviousCursor(prev);
                removeCursor.setNextCursor(null);
            }
            if (_firstCursor == removeCursor)
            {
                _firstCursor = next;
            }
        }
        finally
        {
            if (locked)
            {
                _cursorLock.unlockExclusive();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(this, tc, "_removeCursor");
        }
    }

    /**
     * @param _link
     */
    public final void append(final AbstractItemLink link)
    {
        int priority = link.getPriority();
        LinkedList tl = _prioritySublists[priority];
        if (null == tl)
        {
            synchronized (_prioritySublists)
            {
                // only bother synchronizing if we did not find a list.
                // But we need to test again
                tl = _prioritySublists[priority];
                if (null == tl)
                {
                    tl = new LinkedList();
                    _prioritySublists[priority] = tl;
                }
            }
        }
        tl.append(link);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemCollection#findFirstMatching(com.ibm.ws.sib.msgstore.Filter)
     */
    public final AbstractItem findFirstMatching(Filter filter) throws MessageStoreException
    {
        AbstractItem item = null;
        for (int priority = Priorities.HIGHEST_PRIORITY; null == item
                && priority >= Priorities.LOWEST_PRIORITY; priority--)
        {
            LinkedList tl = _prioritySublists[priority];
            if (null != tl)
            {
                item = tl.findFirstMatching(filter);
            }
        }
        return item;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemCollection#findOldestItem()
     */
    public final AbstractItem findOldestItem() throws MessageStoreException
    {
        AbstractItem item = null;
        AbstractItemLink oldestLink = null;
        long oldestPositionSoFar = Long.MAX_VALUE;
        for (int priority = Priorities.LOWEST_PRIORITY; priority <= Priorities.HIGHEST_PRIORITY; priority++)
        {
            LinkedList list = _prioritySublists[priority];
            if (null != list)
            {
                AbstractItemLink link = (AbstractItemLink) list.getHead();
                if (null != link)
                {
                    final long pos = link.getPosition();
                    if (pos < oldestPositionSoFar)
                    {
                        oldestPositionSoFar = pos;
                        oldestLink = link;
                    }
                }
            }
        }
        if (null != oldestLink)
        {
            item = oldestLink.getItem();
        }
        return item;
    }

    /**
     * @param priority
     * @return the transactional list associated with the given priority.
     */
    public final LinkedList getPrioritySublist(int priority)
    {
        return _prioritySublists[priority];
    }

    public final void linkAvailable(AbstractItemLink link) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "linkReavailable", link);
        }

        boolean locked = false;
        try
        {
            _cursorLock.lock();
            locked = true;

            PrioritizedCursor cursor = _firstCursor;
            while (null != cursor)
            {
                cursor.linkAvailable(link);
                cursor = cursor.getNextCursor();
            }
        }
        finally
        {
            if (locked)
            {
                _cursorLock.unlock();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(this, tc, "linkReavailable");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemCollection#newLockingCursor(com.ibm.ws.sib.msgstore.Filter)
     */
    public final PrioritizedCursor newCursor(final Filter itemFilter, final long lockID, boolean jumpbackEnabled)
    {
        PrioritizedCursor cursor = new PrioritizedCursor(this, itemFilter, lockID, jumpbackEnabled);
        boolean locked = false;
        try
        {
            _cursorLock.lockExclusive();
            locked = true;

            if (_firstCursor != null)
            {
                _firstCursor.setPreviousCursor(cursor);
                cursor.setNextCursor(_firstCursor);
            }
            _firstCursor = cursor;
        }
        finally
        {
            if (locked)
            {
                _cursorLock.unlockExclusive();
            }
        }
        return cursor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.ItemCollection#removeFirstMatching(com.ibm.ws.sib.msgstore.Filter,
     *      com.ibm.ws.sib.msgstore.Transaction)
     */
    public final AbstractItem removeFirstMatching(Filter filter, PersistentTransaction transaction)
            throws MessageStoreException
    {
        AbstractItem found = null;
        for (int priority = Priorities.HIGHEST_PRIORITY; null == found
                && priority >= Priorities.LOWEST_PRIORITY; priority--)
        {
            LinkedList tl = _prioritySublists[priority];
            if (null != tl)
            {
                found = tl.removeFirstMatching(filter, transaction);
                if (null != found && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    SibTr.debug(this, tc, "found item: " + found);
                }
            }
        }
        return found;
    }

    public final boolean xmlHasChildren()
    {
        for (int priority = Priorities.LOWEST_PRIORITY; priority <= Priorities.HIGHEST_PRIORITY; priority++)
        {
            if (null != _prioritySublists[priority])
            {
                return true;
            }
        }
        return false;
    }

    public final void xmlWriteChildrenOn(FormattedWriter writer, String tagName) throws IOException
    {
        writer.newLine();
        writer.write("<");
        writer.write(tagName);
        writer.write(">");
        writer.indent();

        if (null != _firstCursor)
        {
            writer.newLine();
            writer.write("<cursors>");
            writer.indent();
            PrioritizedCursor cursor = _firstCursor;
            while (null != cursor)
            {
                writer.newLine();
                cursor.xmlWriteOn(writer);
                cursor = cursor.getNextCursor();
            }
            writer.outdent();
            writer.newLine();
            writer.write("</cursors>");
        }

        writer.newLine();
        writer.write("<children>");
        writer.indent();
        for (int priority = Priorities.HIGHEST_PRIORITY; priority >= Priorities.LOWEST_PRIORITY; priority--)
        {
            if (null != _prioritySublists[priority])
            {
                Link link = _prioritySublists[priority].getDummyHead();
                while (null != link)
                {
                    writer.newLine();
                    link.xmlWriteOn(writer);
                    link = link.getNextPhysicalLink();
                }
            }
        }
        writer.outdent();
        writer.newLine();
        writer.write("</children>");

        writer.outdent();
        writer.newLine();
        writer.write("</");
        writer.write(tagName);
        writer.write(">");
    }
}
