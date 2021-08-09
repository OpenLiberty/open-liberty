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

import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;

/**
 * A doubly-linked list of weak references to AILs used to keep track of the AILs
 * behind the current position of a cursor. The AILs are ordered in increasing
 * sequence.
 */
public final class BehindRefList
{
  
    // the head and tail of our linked list of AILs behind the current position
    private BehindRef _firstLinkBehind = null;
    private BehindRef _lastLinkBehind = null;

    public BehindRefList()
    {
    }

    public BehindRefList append(AbstractItemLink ail)
    {
        BehindRef br = new BehindRef(ail);
        if (_firstLinkBehind == null)
        {
            _firstLinkBehind = br;
            _lastLinkBehind = br;
        }
        else
        {
            _lastLinkBehind._next = br;
            br._prev = _lastLinkBehind;
            _lastLinkBehind = br;
        }

        return this;
    }


    /**
     * Gets the first AIL in the list of AILs behind the current position of the cursor.
     * This may modify the list since the list is a list of weak references so the original
     * head may no longer refer to an object on the heap, and the head may change
     * (or even the list might completely empty) during the call
     *
     * @param whether to remove the first AIL in the list
     * 
     * @return the first AIL in the list of AILs behind the cursor, or null if there is none
     */
    public final AbstractItemLink getFirst(boolean remove)
    {
        AbstractItemLink _ail = null;

        while ((_ail == null) && (_firstLinkBehind != null))
        {
            _ail = _firstLinkBehind.getAIL();
            if ((_ail == null) || remove)
            {
                _remove(_firstLinkBehind);
            }
        }

        return _ail;
    }


    /**
     * Inserts a reference to an AIL in the list of AILs behind the curent position of the
     * cursor. The insertion is performed in position order. The list is composed
     * of weak references and any which are discovered to refer to objects no longer on the
     * heap are removed as we go.
     * 
     * @param insertAil
     */
    public final BehindRef insert(final AbstractItemLink insertAil)
    {
        final long insertPosition = insertAil.getPosition();
        boolean inserted = false;
        BehindRef addref = null;

        // Loop backwards through the list in order of decreasing sequence number
        // (addition usually near the end) until we have inserted the entry
        BehindRef lookat = _lastLinkBehind;
        while (!inserted && (lookat != null))
        {
            AbstractItemLink lookatAil = lookat.getAIL();
            if (lookatAil != null)
            {
                long lookatPosition = lookatAil.getPosition();
                if (insertPosition > lookatPosition)
                {
                    // This is where it goes then
                    addref = new BehindRef(insertAil);
                    if (lookat._next == null)
                    {
                        // It's now the last one in the list
                        _lastLinkBehind = addref;
                    }
                    else
                    {
                        // It's going in the middle of the list
                        addref._next = lookat._next;
                        lookat._next._prev = addref;
                    }
                    addref._prev = lookat;
                    lookat._next = addref;
                    inserted = true;
                }
                else if (insertPosition == lookatPosition)
                {
                    // A duplicate. OK, it's been made reavailable more than once
                    addref = lookat;
                    inserted = true;
                }
                else
                {
                    // Need to move backwards
                    lookat = lookat._prev;
                }
            }
            else
            {
                BehindRef newlookat = lookat._prev;
                _remove(lookat);
                lookat = newlookat;
            }
        }
        
        // If we have not inserted it yet, it's going to be the first in the list
        if (!inserted)
        {
            addref = new BehindRef(insertAil);
            if (_firstLinkBehind != null)
            {
                addref._next = _firstLinkBehind;
                _firstLinkBehind._prev = addref;
                _firstLinkBehind = addref;
            }
            else
            {
                _firstLinkBehind = addref;
                _lastLinkBehind = addref;
            }
        }

        return addref;
    }


    /**
     * Removes the first AIL in the list of AILs behind the current position of the cursor.
     * The list may empty completely as a result.
     *
     */
    private final void _remove(final BehindRef removeref)
    {
        if (_firstLinkBehind != null)
        {
            if (removeref == _firstLinkBehind)
            {
                // It's the first in the list ...
                if (removeref == _lastLinkBehind)
                {
                    // ... and the only entry, the list is now empty
                    _firstLinkBehind = null;
                    _lastLinkBehind = null;
                }
                else
                {
                    // ... and there are more entries, the first will change
                    BehindRef nextref = removeref._next;
                    removeref._next = null;
                    nextref._prev = null;
                    _firstLinkBehind = nextref;
                }
            }
            else if (removeref == _lastLinkBehind)
            {
                // It's the list in the list and not the first also, the last will change
                BehindRef prevref = removeref._prev;
                removeref._prev = null;
                prevref._next = null;
                _lastLinkBehind = prevref;
            }
            else
            {
                // It's in the middle of the list
                BehindRef prevref = removeref._prev;
                BehindRef nextref = removeref._next;
                removeref._next = null;
                removeref._prev = null;
                prevref._next = nextref;
                nextref._prev = prevref;
            }
        }
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append('{');
        BehindRef br = _firstLinkBehind;
        BehindRef pbr = null;
        while (br != null)
        {
            sb.append(br);
            if (pbr != br._prev)
                sb.append('!');
            pbr = br;
            br = br._next;
        }
        sb.append('}');
        if (pbr != _lastLinkBehind)
            sb.append('!');
        return sb.toString();
    }
}
