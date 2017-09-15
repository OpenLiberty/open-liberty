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
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Simple utility class to hold a linked list of indirections. Needed by two 
 * cache classes.
 */
final class IndirectionList
{
 
    private static TraceComponent tc = SibTr.register(IndirectionList.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    // current number of items in list
    private long _currentCount = 0;

    // number of bytes of items in the list 
    private long _currentSize = 0;

    // first item in linked list of indirections. may be null
    private Indirection _first = null;

    // last item in linked list of indirections. may be null
    private Indirection _last = null;

    synchronized final boolean append(final Indirection indirection, final AbstractItem item)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_append", indirection);

        boolean appended = false;
        if (null == indirection)
        {
            // error, but we can ignore it. If it is null then there is nothing
            // to do.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ignoring null indirection");
        }
        else if (null == item)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ignoring append of indirection to null item");
        }
        else if (null != indirection.itemCacheGetNextLink() || null != indirection.itemCacheGetPrevioustLink())
        {
            // error, but we can ignore it. If either the forward or backward 
            // pointer is set then we are adding the indirection for a second 
            // time. Which is unneccessary and dangerous.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "indirection already linked: " + indirection.getID());
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "link: " + indirection.getID() + " (count=" + _currentCount + ",size=" + _currentSize + ")");

            if (null == _first || null == _last)
            {
                // list is empty
                indirection.itemCacheSetNextLink(null);
                indirection.itemCacheSetPreviousLink(null);
                _last = indirection;
                _first = indirection;
            }
            else if (_first == _last)
            {
                if (indirection == _first)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "indirection already linked: " + indirection.getID());
                }
                else
                {
                    indirection.itemCacheSetNextLink(null);
                    indirection.itemCacheSetPreviousLink(_first);
                    _last = indirection;
                    _first.itemCacheSetNextLink(_last);
                }
            }
            else
            {
                indirection.itemCacheSetNextLink(null);
                indirection.itemCacheSetPreviousLink(_last);
                _last.itemCacheSetNextLink(indirection);
                _last = indirection;
            }
            indirection.itemCacheSetManagedReference(item);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "linked: " + indirection.getID() + " (count=" + _currentCount + ",size=" + _currentSize + ")");

            appended = true;
            _currentCount += 1;
            _currentSize += indirection.getInMemoryItemSize();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_append", Boolean.valueOf(appended));
        return appended;
    }

    /**
     * @return
     */
    synchronized final long getCurrentCount()
    {
        return _currentCount;
    }


    synchronized final long getCurrentSize()
    {
        return _currentSize;
    }

    synchronized final Indirection getFirst()
    {
        return _first;
    }


    synchronized final Indirection removeFirst()
    {
        Indirection removed = _first;
        if (null == removed)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "list empty, reset counters");

            // this should never happen. If there are no elements to remove
            // then we have no discardable memory.....
            _currentCount = 0;
            _currentSize = 0;
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "unlinking " + removed.getID());

            if (removed == _last)
            {
                // list had only one element
                _first = null;
                _last = null;
                _currentCount = 0;
                _currentSize = 0;
            }
            else
            {
                _first = removed.itemCacheGetNextLink();
                _first.itemCacheSetPreviousLink(null);
                _currentCount -= 1;
                _currentSize -= removed.getInMemoryItemSize();
            }
            removed.itemCacheSetManagedReference(null);
            removed.itemCacheSetNextLink(null);
            removed.itemCacheSetPreviousLink(null);
        }
        return removed;
    }

    synchronized final boolean unlink(final Indirection indirection) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_unlink", indirection);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "unlink: " + indirection.getID() + " (count=" + _currentCount + ",size=" + _currentSize + ")");

        boolean unlinked = false;
        if (null == _first || null == _last)
        {
            // list is empty - do nothing
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "empty list");
        }
        else if (_first == _last)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "one element list");

            // one element in list
            if (indirection == _first)
            {
                // the only element is the one we are unlinking
                // so we can empty the list
                _first = null;
                _last = null;
                unlinked = true;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "indirection not in list!");
            }
        }
        else if (null == indirection.itemCacheGetNextLink() && null == indirection.itemCacheGetPrevioustLink())
        {
            // indirection is not linked
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "indirection not linked");
        }
        else if (indirection == _first)
        {
            // list has more than one element, unlink the first element
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "indirection is first");

            if (null == indirection.itemCacheGetNextLink())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_unlink");
                throw new SevereMessageStoreException("unexpected null forward link");
            }
            _first = indirection.itemCacheGetNextLink();
            _first.itemCacheSetPreviousLink(null);
            unlinked = true;
        }
        else if (indirection == _last)
        {
            // list has more than one element, unlink the last element
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "indirection is last");
            
            if (null == indirection.itemCacheGetPrevioustLink())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_unlink");
                throw new SevereMessageStoreException("unexpected null backward link");
            }
            _last = indirection.itemCacheGetPrevioustLink();
            _last.itemCacheSetNextLink(null);
            unlinked = true;
        }
        else
        {
            // list has more than two elements, unlink an element from middle
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "indirection is in middle");

            if (null == indirection.itemCacheGetNextLink() || null == indirection.itemCacheGetPrevioustLink())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_unlink");
                throw new SevereMessageStoreException("unexpected null link");
            }
            final Indirection before = indirection.itemCacheGetPrevioustLink();
            final Indirection after = indirection.itemCacheGetNextLink();
            before.itemCacheSetNextLink(after);
            after.itemCacheSetPreviousLink(before);
            unlinked = true;
        }
        indirection.itemCacheSetNextLink(null);
        indirection.itemCacheSetPreviousLink(null);
        if (unlinked)
        {
            indirection.itemCacheSetManagedReference(null);
            _currentCount -= 1;
            _currentSize -= indirection.getInMemoryItemSize();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_unlink", Boolean.valueOf(unlinked));
        return unlinked;
    }

    final void xmlWriteOn(FormattedWriter writer) throws IOException 
    {
        Indirection ind = getFirst();
        while (null != ind)
        {
            writer.newLine();
            ind.xmlShortWriteOn(writer);
            ind = ind.itemCacheGetNextLink();
        }
    }

}
