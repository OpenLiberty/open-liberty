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
 * Cursor on a linked list. 
 * Remembers its position.
 * Will stop at the end of the list, but will remain valid, so
 * a subsequent append to the list will be seen by a call to next().
 * author: Phill van Leersum
 */
public class Cursor {
    private static TraceComponent tc = SibTr.register(Cursor.class, UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);

    private Link _lastLink = null;
    private final LinkedList _parent;

    public Cursor(LinkedList list) {
        _parent = list;
    }

    public void finished() {
        if (tc.isEntryEnabled()) {
            SibTr.entry(this, tc, "finished");
        }
        
        synchronized (_parent)
        {
          if (null != _lastLink) {
              _lastLink.cursorRemoved();
              _lastLink = null;
          }
        }
        
        if (tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "finished");
        }
    }

    /**
     * @return the next link in the chain, or null if none.
     */
    public final Link next() {
        if (tc.isEntryEnabled()) {
            SibTr.entry(this, tc, "next");
        }
        Link replyLink = null;
        Link newLastLink = null;
        synchronized (_parent) {
            // We start our search from either the lastLink we found, or the 
            // dummyHead.
            Link lookAt = _lastLink;
            if (null == lookAt) {
                lookAt = _parent.getDummyHead();
            }
            
            // We now move to the first link that we have not seen. 
            lookAt = lookAt.getNextPhysicalLink();

            // this loop will move through list until it finds a link that is
            // in the linked state. If non is found replyLink will be null 
            // after the loop. Note that the Tail link is not counted
            while (null != lookAt && null == replyLink) {
                if (lookAt.isTail()) {
                    // we do not count the dummy tail. Signal an end to the search with a null
                    lookAt = null;
                } else {
                    // this is the last link we have examined, so we save it, so we do not need 
                    // to traverse it again
                    newLastLink = lookAt;
                    if (lookAt.isLinked()) {
                        // the link we are looking at is valid, so we return it
                        replyLink = lookAt;
                    } else {
                        // the link we are looking at is not valid, so we move to the next
                        lookAt = lookAt.getNextPhysicalLink();
                    }
                }
            }

            if (null != newLastLink) {
                // we have found a new 'lastLink' and so must move the reference 
                // and adjust cursor counters as needed. We may not find a new
                // link - if the original 'lastLink' was at the end of a list.
                if (null != _lastLink) {
                    _lastLink.decrementCursorCount();
                }
                _lastLink = newLastLink;
                _lastLink.incrementCursorCount();
            }
        }
        
        if (tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "next", replyLink);
        }
        return replyLink;
    }

    /**
     * @return
     */
    public final Object peepLastLink() {
        return _lastLink;
    }
}
