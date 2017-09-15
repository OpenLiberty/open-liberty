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
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Non-prioritized non transactional doubly linked list.
 * <p>
 * Yes, java.util provides a linked list, but I need to extend the linked list behaviour. Mainly the
 * ability to extend the {@link Link#getNextLink()}s to gain specialized behaviour.
 * </p>
 * <p>
 * This is not rocket science. The list is bounded by two 'dummy' links which are never visible
 * outside the list. The dummy head link and dummy tail links never change and avoid the need for
 * special case code at each end of the list. They also reduce the need for null checking.
 * </p>
 * <p>
 * Modifications to the list are done under a lock on the list. While this may initially appear so,
 * this is efficient because the changes are small (four assignments for an insert, two for a
 * remove).
 * </p>
 */
public class LinkedList
{

    private static TraceComponent tc = SibTr.register(LinkedList.class,
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    /*
     * To ensure that we have a simple algorithm, we make sure that there is always one link
     * (dummyHead) before the first real link (head) and one link (dummyTail) after after the last
     * real link (tail).
     */
    private final Link _dummyHead = new Link();

    private final Link _dummyTail = new Link();

    // The next position to issue - incremented and issued while synchronized on this object
    private long _nextPositionToIssue = 0;

    /**
     * Standard constructor
     */
    public LinkedList()
    {
        _dummyHead._setAsHead(this, _dummyTail);
        _dummyTail._setAsTail(this, _dummyHead);
    }

    final String _debugString()
    {
        StringBuffer buf = new StringBuffer();
        Link link = _dummyHead;
        link._shortDebugString(buf);
        link = link._getNextLink();
        int limit = 10;
        while (link != null && 0 < limit--)
        {
            buf.append(",");
            link._shortDebugString(buf);
            link = link._getNextLink();
        }
        return buf.toString();
    }

    /**
     * Append a link to the end of the list.
     * 
     * @param link
     */
    public synchronized final void append(Link link)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "append");

        Link prev = _dummyTail._getPreviousLink();
        link._link(prev, _dummyTail, _nextPositionToIssue++, this);
        prev._setNextLink(link);
        _dummyTail._setPreviousLink(link);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "append", _debugString());
    }

    /**
     * Method get returns the first unlocked matching object on the list.
     * 
     * @param filter
     * @return Link
     * @throws MessageStoreException
     */
    public final AbstractItem findFirstMatching(final Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "findFirstMatching", filter);

        AbstractItem found = null;

        Link link = getHead();

        while (link != null && found == null)
        {
            found = ((AbstractItemLink)link).matches(filter);

            if (found == null)
            {
                // Defect 493652/PK59872
                // We need to lock on the list at this point as our current link
                // may have been unlinked by another thread during the matches() 
                // call. In that case we need to start at the head of the list again 
                // as the next/previous pointers for the unlinked link will not be
                // set.
                synchronized(this)
                {
                    if (link.isPhysicallyUnlinked())
                    {
                        // We have been unlinked while we were doing the match. 
                        // Start again at the beginning of the list.
                        link = getHead();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current link is PhysicallyUnlinked so returning to beginning of list.");
                    }
                    else
                    {   
                        link = link.getNextLogicalLink();
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "findFirstMatching", found);
        return found;
    }

    public final Link getDummyHead()
    {
        return _dummyHead;
    }

    /**
     * @return first XaLink of list, may be null if list is empty.
     */
    public final Link getHead()
    {
        return getNextLink(null);
    }

    /**
     * Reply the next (valid) link after the specified link. If the specified link is null then
     * start at beginning of list.
     * 
     * @param after
     * @return
     */
    public final synchronized Link getNextLink(Link link)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getNextLink", link);

        Link nextLink = null;

        if (link == null)
        {
            nextLink = _dummyHead.getNextPhysicalLink();
        }
        else
        {
            // Defect 493652/PK59872
            // If the link provided is PhysicallyUnlinked then it is 
            // likely that another thread has removed it while our 
            // thread was inspecting it. In order to avoid stopping
            // in the middle of a find/removeFirstMatching search we
            // need to start from the beginning of the list again.
            if (link.isPhysicallyUnlinked())
            {
                nextLink = _dummyHead.getNextPhysicalLink();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current link is PhysicallyUnlinked so returning to beginning of list.");
            }
            else
            {
                nextLink = link.getNextPhysicalLink();
            }
        }

        while (nextLink != null && nextLink.isLogicallyUnlinked())
        {
            nextLink = nextLink.getNextPhysicalLink();
        }

        if (nextLink == _dummyTail)
        {
            nextLink = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getNextLink", nextLink);
        return nextLink;
    }

    /**
     * Method removes the first unlocked matching object on the list destructively under
     * transactional control.
     * 
     * @param filter
     * @param transaction
     * @return AbstractItem
     * @throws MessageStoreException
     */
    public final AbstractItem removeFirstMatching(final Filter filter, PersistentTransaction transaction) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "removeFirstMatching", new Object[] { filter, transaction});

        AbstractItem found = null;

        Link link = getHead();

        while (link != null && found == null)
        {
            found = ((AbstractItemLink)link).removeIfMatches(filter, transaction);

            if (found == null)
            {
                // Defect 493652/PK59872
                // We need to lock on the list at this point as our current link
                // may have been unlinked by another thread during the matches() 
                // call. In that case we need to start at the head of the list again 
                // as the next/previous pointers for the unlinked link will not be
                // set.
                synchronized(this)
                {
                    if (link.isPhysicallyUnlinked())
                    {
                        // We have been unlinked while we were doing the match. 
                        // Start again at the beginning of the list.
                        link = getHead();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current link is PhysicallyUnlinked so returning to beginning of list.");
                    }
                    else
                    {   
                        link = link.getNextLogicalLink();
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "removeFirstMatching", found);
        return found;
    }

    /**
     * Used when the transactional list is NOT part of a prioritized list.
     * 
     * @param writer
     * @param tagName
     * @throws IOException
     */
    public final void xmlWriteOn(FormattedWriter writer, String tagName) throws IOException
    {
        Link link = getHead();
        if (link != null)
        {
            writer.newLine();
            writer.startTag(tagName);
            writer.indent();

            while (link != null)
            {
                writer.newLine();
                link.xmlWriteOn(writer);
                link = link.getNextPhysicalLink();
            }

            writer.outdent();
            writer.newLine();
            writer.endTag(tagName);
        }
    }

    /**
     * This is a method used by the unit tests to determine the number of links in the list.
     * It's too inefficient for any other purpose.
     * 
     * @return the number of links
     */
    public int countLinks()
    {
        int count = 0;
        Link look = _dummyHead.getNextLogicalLink();
        while (look != null && _dummyTail != look)
        {
            count++;
            look = look._getNextLink();
        }
        return count;
    }
}
