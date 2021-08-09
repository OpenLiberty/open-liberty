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

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Cursor on a linked list that adds the idea of selecting depending on link state. This cursor selects
 * links based upon a filter. It is capable of acting as a locking cursor or nonlocking cursor. It
 * can use jump-back.
 * 
 * <p>Note that this class is an amalgamation of previous classes called GetCursor and BrowseCursor,
 * and can act in either capacity according to the choice of next() called, either with or without a
 * lock ID.
 */
public final class Subcursor
{
    private static TraceComponent tc = SibTr.register(Subcursor.class,
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    // the last link seen during traversal
    private Link _lastLink = null;

    // the filter - may be null
    private final Filter _filter;

    // flag to control whether items becoming available behind the cursor
    // are noticed. Jumpback intentionally does not work for browse
    // but could be enabled with a small code change in next(boolean)
    private boolean _jumpbackEnabled;

    // list of AILs behind the current position used if jump-back is enabled
    private BehindRefList _behindList;

    // the highest position seen during traversal, so that we know
    // which re-available links are behind the cursor
    private long _highestPosition = -1;

    // the owning list
    private final LinkedList _parent;

    public Subcursor(LinkedList parent, final Filter filter, boolean jumpbackEnabled)
    {
        _parent = parent;
        _filter = filter;
        _jumpbackEnabled = jumpbackEnabled;
        if (_jumpbackEnabled)
        {
            _behindList = new BehindRefList();
        }
    }

    /**
     * Finds and locks the next matching item.
     * 
     * <p>This method MUST NOT be synchronized. It can cause reading from the
     * persistence layer. For this reason, and because it contains a loop to
     * find a suitable item, the execution time of this method can be long
     * and synchronization would prevent concurrent addition of entries to the
     * behind list.
     * 
     * @param lockID
     * @return
     * @throws SevereMessageStoreException 
     */
    private final AbstractItemLink _next(long lockID) throws SevereMessageStoreException
    {
        // first look for an available link that we can lock. We scan the tree
        // removing each element as we find it. If we are able to lock the link
        // we use it.
        AbstractItemLink lockedMatchingLink = null;
        if (_jumpbackEnabled)
        {
            while (null == lockedMatchingLink)
            {
                AbstractItemLink link;

                // Get and remove the first AIL in the list of AILs behind the current position
                synchronized(this)
                {
                    link = _behindList.getFirst(true);
                }

                if (link == null)
                {
                    break;
                }
                else if (link.lockItemIfAvailable(lockID))
                {
                    lockedMatchingLink = link;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "using available: " + lockedMatchingLink);
                }
            }
        }

        if (null == lockedMatchingLink)
        {
            // we didn't find and lock an available link, so we must now resume
            // the traverse of the linked list
            AbstractItemLink lookAtLink;
            synchronized(this)
            {
                lookAtLink = (AbstractItemLink)advance();
            }

            while (null != lookAtLink && null == lockedMatchingLink)
            {
                // update our position for each link we examine in the chain
                long pos = lookAtLink.getPosition();
                synchronized(this)
                {
                    if (pos > _highestPosition)
                    {
                        _highestPosition = pos;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "examine " + lookAtLink + "(seq = " + _highestPosition + ")");
                    }
                }

                // try to lock the link
                //
                // PAY ATTENTION
                //
                // This can read from the persistence layer so we MUST not be synchronized
                // on this subcursor for the sake of concurrency (although it will not deadlock).
                // Defect 298364 (sev 1) was raised to reflect a delay of several minutes due to this
                if (lookAtLink.lockIfMatches(_filter, lockID))
                {
                    // we matched and locked the link
                    lockedMatchingLink = lookAtLink;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "found: " + lockedMatchingLink);
                }
                else
                {
                    // we didn't get the link - it didn't match or didn't lock. Advance
                    synchronized(this)
                    {
                        lookAtLink = (AbstractItemLink)advance();
                    }
                }
            }
        }

        return lockedMatchingLink;
    }

    /**
     * The cursor is being told that a link has become available. Add it to the list to revisit, but
     * only if it is older than the current cursor position.
     * 
     * @param link
     * @throws SevereMessageStoreException 
     */
    public final void available(AbstractItemLink link) throws SevereMessageStoreException
    {
        if (_jumpbackEnabled)
        {
            final long newPos = link.getPosition();

            // we need to synchronize the read of the position
            // or we can miss out items when reading an empty queue fast.
            // More importantly, the removal of the next from the cursor cannot
            // happen while we are adding the available
            synchronized(this)
            {
                if (newPos <= _highestPosition)
                {
                    // Only store matching links, 'coz its quicker
                    if (null != link.matches(_filter))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "availableLink adding: " + link);

                        _behindList.insert(link);
                    }
                    else
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "availableLink does not match: " + link);
                    }
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "availableLink seq(" + newPos + ") too large (" + _highestPosition + ")");
                }
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "available link - jumpbackDisabled: " + link);
        }
    }

    /**
     * @return
     */
    public final Filter getFilter()
    {
        return _filter;
    }

    /**
     * Reply the next stored object that is deemed a match by the filter specified when the cursor
     * was created.
     * <p>
     * Objects locked by an unresolved transactional get or put, or by a getCursor are not visible
     * to this method unless the allowUnavailable parameter is true.
     * </p>
     * <p>
     * Objects returned by this method are not locked .
     * </p>
     * <p>
     * This is used for browse cursors so does not perform jumpback.
     * </p>
     * @throws SevereMessageStoreException 
     */
    public final AbstractItem next(boolean allowUnavailable) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "next", Boolean.valueOf(allowUnavailable));

        AbstractItem found = null;

        // check from current position
        AbstractItemLink lookAt = (AbstractItemLink)advance();
        while (null != lookAt && null == found)
        {
            found = lookAt.matches(_filter, allowUnavailable);
            if (null == found)
            {
                lookAt = (AbstractItemLink)advance();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "next", found);
        return found;
    }

    /**
     * Return the next item that is deemed a match by the filter specified when the cursor
     * was created. Items returned by this method are locked.
     * @throws SevereMessageStoreException 
     */
    public final AbstractItem next(long lockID) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "next", Long.valueOf(lockID));

        final AbstractItemLink lockedMatchingLink = _next(lockID);

        // retrieve the item from the link
        AbstractItem lockedMatchingItem = null;
        if (null != lockedMatchingLink)
        {
            lockedMatchingItem = lockedMatchingLink.getItem();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "next", lockedMatchingItem);
        return lockedMatchingItem;
    }

    /**
     * Signals that use of the cursor has finished. The cursor is removed from the AIL that it currently
     * rests on.
     */
    public final void finished()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "finished");

        if (null != _lastLink)
        {
            _lastLink.cursorRemoved();
            _lastLink = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "finished");
    }

    /**
     * Advances the cursor to the next physical link. The method name is specifically not next()
     * because that method name has already been used extensively in this class and it was getting
     * confusing. This would be a private method if it were not used by unit tests.
     * 
     * @return the next link in the chain, or null if none.
     */
    public final Link advance()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "advance");

        Link replyLink = null;
        Link newLastLink = null;
        synchronized (_parent)
        {
            // We start our search from either the lastLink we found, or the dummyHead.
            Link lookAt = _lastLink;
            if (null == lookAt)
            {
                lookAt = _parent.getDummyHead();
            }

            // We now move to the first link that we have not seen.
            lookAt = lookAt.getNextPhysicalLink();

            // this loop will move through list until it finds a link that is
            // in the linked state. If none is found replyLink will be null
            // after the loop. Note that the Tail link is not counted
            while (null != lookAt && null == replyLink)
            {
                if (lookAt.isTail())
                {
                    // we do not count the dummy tail. Signal an end to the search with a null
                    lookAt = null;
                }
                else
                {
                    // this is the last link we have examined, so we save it, so we do not need
                    // to traverse it again
                    newLastLink = lookAt;
                    if (lookAt.isLinked())
                    {
                        // the link we are looking at is valid, so we return it
                        replyLink = lookAt;
                    }
                    else
                    {
                        // the link we are looking at is not valid, so we move to the next
                        lookAt = lookAt.getNextPhysicalLink();
                    }
                }
            }

            if (null != newLastLink)
            {
                // we have found a new 'lastLink' and so must move the reference
                // and adjust cursor counters as needed. We may not find a new
                // link - if the original 'lastLink' was at the end of a list.
                //
                // NOTE that we do not move in the special case that jumpback has been
                // disabled and we don't have a link to return. This makes it less likely
                // that items which are changing state will be skipped over by a cursor
                // with jumpback disabled, obviously at the cost of looking at them
                // again and again.
                if (_jumpbackEnabled || (replyLink != null))
                {
                    if (null != _lastLink)
                    {
                        _lastLink.decrementCursorCount();
                    }
                    _lastLink = newLastLink;
                    _lastLink.incrementCursorCount();
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "advance", replyLink);
        return replyLink;
    }

    public final Object peepLastLink()
    {
        return _lastLink;
    }
        /* The method when called for first time will iterate on linkedlist by calling advance() method till it reaches a 
     * position in the linkedlist equal to fromIndex . Once it reaches position at fromIndex , it breaks from loop and 
     * then returns the AbstractItem reference held by the AbstractItemLink at the given position in list. All subsequest 
     * calls made to this method in a single flow of control , will then return AbstactItem references after the fromIndex 
     * in the linkedlist.
     */
    public final AbstractItem next(boolean allowUnavailable,int fromIndex)throws SevereMessageStoreException
    {
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "next  fromIndex="+fromIndex, Boolean.valueOf(allowUnavailable));

	        AbstractItem found = null;
	        // check from current position
	        AbstractItemLink lookAt=(AbstractItemLink)advance();
	       
		while(lookAt !=null && lookAt.getPosition()<fromIndex) 
		{
            		lookAt=(AbstractItemLink)advance();													
		}
					
		while(null == found && lookAt!=null)
		{
			found = lookAt.matches(_filter, allowUnavailable);					
		}
		          
				
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "next(boolean)  fromIndex="+fromIndex, found);
	        return found;
     }
    /* The method when called for first time will iterate on linkedlist by calling _next(lockID) method till it reaches a 
     * position in the linkedlist equal to fromIndex . Once it reaches position at fromIndex , it breaks from loop and 
     * then returns the AbstractItem reference held by the AbstractItemLink at the given position in list. All subsequest 
     * calls made to this method in a single flow of control , will then return AbstactItem references after the fromIndex 
     * in the linkedlist.
     */
	public AbstractItem next(long lockID, int fromIndex) throws SevereMessageStoreException 
	{
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "next  fromIndex="+fromIndex, Long.valueOf(lockID));
		 
		AbstractItemLink lockedMatchingLink = _next(lockID);
		AbstractItem lockedMatchingItem = null;
				
		while(lockedMatchingLink!=null && lockedMatchingLink.getPosition()<fromIndex)
		{					
		lockedMatchingLink = _next(lockID);
		}
				
		while(lockedMatchingItem==null && lockedMatchingLink !=null)
		{
			lockedMatchingItem = lockedMatchingLink.getItem();			
		}
			
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	{
	 SibTr.exit(this, tc, "next(lockId)  fromIndex="+fromIndex, lockedMatchingItem);
	}
	    return lockedMatchingItem;
	}
	////673411  ends

}
