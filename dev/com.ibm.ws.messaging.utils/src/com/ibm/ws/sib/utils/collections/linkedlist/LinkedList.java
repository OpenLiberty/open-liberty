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
package com.ibm.ws.sib.utils.collections.linkedlist;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.UtConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Non-prioritized non transactional doubly  linked list.
 * <p> Yes, java.util provides a linked list, but I need to extend the linked
 * list behaviour. Mainly the ability to extend the {@link Link#getNextLink()}s to gain
 * specialized behaviour. </p>
 * <p>This is not rocket science.  The list is bounded by two 'dummy' links which
 * are never visible outside the list. The dummy head link and dummy tail links
 * never change and avoid the need for special case code at each end of the list.
 * They also reduce the need for null checking.</p>
 * <p>Modifications to the list are done under a lock on the list. While this may
 * initially appear so, this is efficient because the
 * changes are small (four assignments for an insert, two
 * for a remove).</p>
 * @author drphill
 *
 */
public class LinkedList {
    private static TraceComponent tc = SibTr.register(LinkedList.class, UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);

    /*
     * To ensure that we have a simple algorithm,we make sure that
     * there is always one link (dummyHead) before the first real
     * link (head) and one link (dummyTail) after after the last
     * real link (tail).
     */
    private final Link _dummyHead = new Link();
    private final Link _dummyTail = new Link();

    /**
     * Standard constructor
     */
    public LinkedList() {
        super();
        _dummyHead._setAsHead(this, _dummyTail);
        _dummyTail._setAsTail(this, _dummyHead);
    }

    final String _debugString() {
        StringBuffer buf = new StringBuffer();
        Link link = _dummyHead;
        link._shortDebugString(buf);
        link = link._getNextLink();
        int limit = 10;
        while (null != link && 0 < limit--) {
            buf.append(",");
            link._shortDebugString(buf);
            link = link._getNextLink();
        }
        return buf.toString();
    }

    /**
     * Append a link to the end of the list.
     * @param link
     */
    public final void append(Link link) {
        insertLinkBefore(link, _dummyTail);
    }

    /**
     * Slow, inneficient but accurate way to determine the size of a list.
     * Later in the development cycle when the bugs are ironed out we can
     * swap to using a counter.
     *
     * @return number of links on the list.
     */
    public final int countLinks() {
        int count = 0;
        Link look = _dummyHead._getNextLink();
        while (null != look && _dummyTail != look) {
            count++;
            look = look._getNextLink();
        }
        return count;
    }
    /**
     * @return
     */
    public final Link getDummyHead() {
        return _dummyHead;
    }

    /**
     * @return first XaLink of list, may be null if list is empty.
     */
    public final Link getHead() {
        return getNextLink(null);
    }

    /**
     * Reply the next (valid) link after the specified link. If the specified
     * link is null then start at beginning of list.
     * @param after
     * @return
     */
    public final synchronized Link getNextLink(Link after) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, "getNextLink", after);
        }
        Link replyLink = null;
        if (null == after) {
            replyLink = _dummyHead.getNextPhysicalLink();
        } else {
            replyLink = after.getNextPhysicalLink();
        }
        while (null != replyLink && replyLink.isLogicallyUnlinked()) {
            replyLink = replyLink.getNextPhysicalLink();
        }
        if (_dummyTail == replyLink) {
            replyLink = null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "getNextLink", replyLink);
        }
        return replyLink;
    }

    /** Insert the specified link into the list.
     * @param insertLink link to be inserted
     * @param followingLink link before which the
     * insertLink is inserted.
     */
    public final synchronized void insertLinkBefore(Link insertLink, Link followingLink) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(
                this,
                tc,
                "insertLinkBefore",
                new long[] { insertLink.getSequence(), followingLink.getSequence()});
        }
        Link prev = followingLink._getPreviousLink();
        insertLink._link(prev, followingLink, this);
        prev._setNextLink(insertLink);
        followingLink._setPreviousLink(insertLink);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "insertLinkBefore", _debugString());
        }
    }

}
