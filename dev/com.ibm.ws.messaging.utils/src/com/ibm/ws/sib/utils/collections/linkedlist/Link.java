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

import java.io.IOException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.UtConstants;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A link in the linked list.  Note that the link is
 * completely useless on its own - a subclass is needed to
 * hold the appropriate data.  See com.ibm.ws.sib.msgstore.cache.linkedlist.ObjectList
 * for an example.
 * <p>
 * Concurrency: Note we always use the parent lock to make
 * modifications to the list.  This is efficient because the
 * changes are small (four assignments for an insert, two
 * for a remove).
 * </p>
 * <p>
 * Each link keeps a count of the number of cursors looking at it. The count
 * is changed under the parent monitor. The link is only unlinked when it has been
 * logically deleted and there are no cursors looking at it (cursor count = 0).
 * This means that there are three events that might cause a link to be removed.
 * <ul>
 * <li>unlink(): if there are no cursors looking at the link then it can be unlinked
 *     immediately.</li>
 * <li>cursorRemoved(): if the link the cursor is looking at is logically deleted
 *     and this is the last cursor looking at the link.</li>
 * <li>_jumpForward(): when a cursor moves forward from one link to another, if the
 * link being left is logically deleted and the cursor count drops to zero.</li>
 * </ul>
 * All the unlink logic is performed in _tryUnlink().
 * </p>
 *
 * <p>
 * The link maintains its state which controls allowed actions. State can be:
 * <ul>
 * <li> DUMMY - not part of real list. Cannot be linked or unlinked. Cursor
 *      can start on head Dummy, but should never reach tail dummy.</li>
 * <li> LINKED - currently linked and part of the list.</li>
 * <li> LOGICALLY_UNLINKED - still physically linked, but not part of the list.
 *      The forward and backward links are still in place so that cursors can
 *      navigate, but link should be ignored for all other purposes.
 * </li>
 * <li> PHYSICALLY_UNLINKED - not currently linked and not part of the list.</li>
 * </ul>
 * </p>
 * author: Phill van Leersum
 */
public class Link {

    /* Head of List - it does not participate in the list proper */
    private static final String HEAD = "Head";
    /* the link has been linked */
    private static final String LINKED = "Linked";
    /* the link been logically unlinked, but is still part of the list */
    private static final String LOGICALLY_UNLINKED = "LogicallyUnlinked";
    /* the link been removed from the list */
    private static final String PHYSICALLY_UNLINKED = "PhysicallyUnlinked";
    /* Tail of List - it does not participate in the list proper */
    private static final String TAIL = "Tail";

    private static TraceComponent tc = SibTr.register(Link.class, UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);
    private int _cursorCount = 0;
    private Link _nextLink;
    private LinkedList _parent;
    private Link _previousLink;

    /* State described above */
    private String _state = PHYSICALLY_UNLINKED;

    /**
     * Constructor for normal links
     */
    public Link() {
        super();
    }

    /*
     * Internal navigation, not for public use.
     * @return
     */
    final Link _getNextLink() {
        return _nextLink;
    }

    /* only used for deriving real tail from dummy tail.
     * @return
     */
    final Link _getPreviousLink() {
        return _previousLink;
    }

    final void _link(Link prev, Link next, LinkedList list ) {
        if (PHYSICALLY_UNLINKED != _state) {
            throw new RuntimeException(_state);
        }
        _nextLink = next;
        _previousLink = prev;
        _parent = list;
        _state = LINKED;
    }

    final String _positionString() {
        StringBuffer buf = new StringBuffer();
        _shortDebugString(buf);
        buf.append(" Forward:");
        Link link = _getNextLink();
        int limit = 10;
        while (null != link && 0 < limit--) {
            buf.append(" ");
            link._shortDebugString(buf);
            link = link._getNextLink();
        }
        buf.append("; Backward:");
        link = _getPreviousLink();
        limit = 10;
        while (null != link && 0 < limit--) {
            buf.append(" ");
            link._shortDebugString(buf);
            link = link._getPreviousLink();
        }
        return buf.toString();
    }

    /* Only used for dummy head
     * @param link
     */
    final void _setAsHead(LinkedList parent, Link tail) {
        _nextLink = tail;
        _parent = parent;
        _state = HEAD;
    }

    /* Only used for dummy tail
     * @param link
     */
    final void _setAsTail(LinkedList parent, Link head) {
        _previousLink = head;
        _parent = parent;
        _state = TAIL;
    }

    /**
     * @param link
     */
    final void _setNextLink(Link link) {
        _nextLink = link;
    }


    /**
     * @param link
     */
    final void _setPreviousLink(Link link) {
        _previousLink = link;
    }

    final void _shortDebugString(StringBuffer buf) {
        long s = getSequence();
        if (-1 != s) {
            buf.append(s);
        }
        buf.append("(");
        buf.append(_state);
        buf.append(")");
    }

    /**
     * Attempt to physically unlink the receiver if appropriate.
     * MUST BE CALLED UNDER _parent MONITOR.
     */
    private final void _tryUnlink() {
        if (0 >= _cursorCount && _state == LOGICALLY_UNLINKED) {
            _previousLink._nextLink = _nextLink;
            _nextLink._previousLink = _previousLink;
            _previousLink = null;
            _nextLink = null;
            // Defect 240039
            //_parent = null;
            _state = PHYSICALLY_UNLINKED;
        }
    }

    final void cursorRemoved() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, "cursorRemoved");
        }
        LinkedList parent = _parent;
        if (null != parent) {
            synchronized (parent) {
                if (LINKED == _state || LOGICALLY_UNLINKED == _state) {
                    _cursorCount--;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        SibTr.debug(this, tc, "cursorCount decrement to "+_cursorCount);
                    }
                    _tryUnlink();
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "cursorRemoved");
        }
    }

    /**
     * called only from cursor, using parent list synchronization.
     */
    final void decrementCursorCount() {
        _cursorCount--;
        _tryUnlink();
    }

    /**
     * @return current cursor count
     */
    public final int getCursorCount() {
        return _cursorCount;
    }

    /**
     * Navigate to the next logical link.
     * This version is for use with non-cursored navigation.
     *
     * @return the next link object or null if nothing left in the list to return
     * @ deprecated. Use list.getNextLink();
     */
    public final Link getNextLink() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, "getNextLink", _positionString());
        }
        Link nextLink = null;
        LinkedList parent = _parent;
        if (null != parent) {
            nextLink = _parent.getNextLink(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "getNextLink", nextLink);
        }
        return nextLink;
    }

    /**
     * This version returns the next physical link (which may be logically
     * unlinked)
     *
     * @return the next link object or null if nothing left in the list to return
     */
    public final Link getNextPhysicalLink() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, "getNextPhysicalLink", _positionString());
        }
        Link nextLink = _nextLink;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "getNextPhysicalLink", nextLink);
        }
        return nextLink;
    }

    /**
     * @return the owning list, or null if none.
     */
    public final LinkedList getOwningList() {
        return _parent;
    }

    /**
     * @return a sequence within this list.  Not required to be contiguous,
     * used for string representation and identification in trace.
     * Subclasses should overide.
     */
    public long getSequence() {
        return -1;
    }

    /**
     * called only from cursor, using parent list synchronization.
     */
    final void incrementCursorCount() {
        _cursorCount++;
    }

    /**
     * @return true if the receiver is linked in a list, false otherwise.
     * A link that returns true to this method cannot be added to a list.
     */
    public final boolean isLinked() {
        return LINKED == _state;
    }

    /**
     * @return true if the receiver logically unlinked, false otherwise.
     * A return of true means that an attempt has been made to unlink the
     * receiver, and it should be considered unlinked. The link is,
     * however, still physically linked in the list, and is probably being
     * retained because it has a cursor pointing at it.
     * A link that returns true to this method cannot be added to a list.
     */
    public final boolean isLogicallyUnlinked() {
        return LOGICALLY_UNLINKED == _state;
    }

    /**
     * @return true if the receiver is not currently linked in a list, false
     * otherwise.
     * A link that returns true to this method can be added to a list.
     * Changes to the state reported by this method are synchronised under the
     * monitor of the owning linked list.
     */
    public final boolean isPhysicallyUnlinked() {
        return PHYSICALLY_UNLINKED == _state;
    }

    /**
     * @return true if the receiver is the tail of the list, false otherwise.
     * The tail of the list is the position just beyond the end of the list
     * and should never be visible outside this package.
     * A link that returns true to this method cannot be added to a list.
     */
    public final boolean isTail() {
        return TAIL == _state;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        Link link = _previousLink;
        if (null != link) {
            link._shortDebugString(buf);
            buf.append("?<-");
        }
        _shortDebugString(buf);
        link = _nextLink;
        if (null != link) {
            buf.append("?->");
            link._shortDebugString(buf);
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * Request that the receiver be unlinked from the list. If the receiver
     * is linked it will be marked as logically unlinked.
     * Note that this will perform a logical unlink, which may result
     * in a physical unlink
     */
    public final boolean unlink() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, "unlink", _positionString());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(this, tc, "cursor count = "+_cursorCount);
        }
        boolean unlinked = false;
        LinkedList parent = _parent;
        if (null != parent) {
            synchronized (parent) {
                if (LINKED == _state) {
                    _state = LOGICALLY_UNLINKED;
                    _tryUnlink();
                    unlinked = true;
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        SibTr.debug(this, tc, "unlink while " + _state);
                    }
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "unlink", new Object[] { Boolean.valueOf(unlinked), _positionString()});
        }
        return unlinked;
    }

    protected void xmlWriteAttributesOn(FormattedWriter writer) throws IOException {
        writer.write(" linkState=\"");
        writer.write(_state);
        writer.write("\" cursorCount=\"");
        writer.write(Integer.toString(_cursorCount));
        writer.write("\" sihc=\""); // matches trace identifier
        writer.write(Integer.toHexString(System.identityHashCode(this)));
        writer.write("\" ");
    }

    /**
     * Default XML output.
     * @param buffer
     * @throws IOException
     */
    public void xmlWriteOn(FormattedWriter writer) throws IOException {
        String name = "link";
        writer.write("<");
        writer.write(name);
        xmlWriteAttributesOn(writer);
        writer.write(" />");
    }

}
