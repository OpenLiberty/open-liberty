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
package com.ibm.ws.sib.msgstore;


/**
 * 
 * A cursor is used to step through a matching subset of {@link AbstractItem}s 
 * in a {@link ItemStream}.  A cursor has an optional {@link Filter} specified 
 * at creation time.  A cursor without a filter will match all 
 * {@link AbstractItem}s.
 * <p>
 * Notes on item ordering (note the term 'stream' includes {@link ItemStream} and {@link ReferenceStream}
 * as appropriate):
 * <ul> 
 * <li>
 * {@link AbstractItem}s are stored in priority order within streams, with the highest priority items being first. Within a priority, messages 
 * are stored in the order in which they are added to the stream.
 * A cursor is used to provide a position within the stream. {@link AbstractItem}s may be available or not available to a cursor. An item 
 * being added to the stream is not available to the cursor until the add is committed.
 * </li><li>
 * An {@link AbstractItem} is not available when it is locked  by a get cursor, or a remove begins. An item becomes available when it is unlocked, 
 * or when an add is committed, or when a remove is rolled back.
 * </li><li>
 * A cursor will return available {@link AbstractItem}s sequentially from the stream in priority order, highest first.
 * </li><li>
 * {@link AbstractItem} which are not available become invisible to the cursor and will not be returned.
 * </li><li>
 * When an unavailable item becomes available again, it will be visible to the cursor once more. If the {@link AbstractItem} is behind the 
 * cursor, either at the same priority or higher, then it will be returned next, before any further {@link AbstractItem}s are returned from the current 
 * cursor position.
 * </li><li>
 * If several {@link AbstractItem}s become available at the same time, then those of the highest priority will be returned first, followed by any 
 * others of higher or equal priority to the {@link AbstractItem} at the current cursor position. Once all such re-available {@link AbstractItem}s have been returned, 
 * then the cursor will resume where it left off. Any re-available {@link AbstractItem}s ahead of the cursor (either at the same priority or a lower 
 * priority) will be returned in sequence as the cursor progresses through the remainder of the stream.
 * </li>
 * </ul>
 * </p>
 * <p>A cursor will return null when there are no more available {@link AbstractItem}s.  
 * The cursor
 * can continue to be polled, and will return any {@link AbstractItem} put to the 
 * {@link ItemStream}. </p>
 *
 * Get Cursor: used for performing speculative get operations.
 * When an {@link AbstractItem} is returned from next, it is locked for the use
 * of the cursor.<br> {@link AbstractItem}s locked by a cursor are invisible 
 * to other cursors and 
 * to get operations.  Locked {@link AbstractItem}s can be destructively
 * removed under a transaction, or unlocked.  Unlocking an {@link AbstractItem} 
 * makes it available to other cursors and get operations.
 * A get cursor will jump over locked {@link AbstractItem}s as though they did not exist.
 * Should a matching locked {@link AbstractItem} later become available (eg from a backout 
 * of a get, or from an unlock) then it will be returned by the GetCursor.  The order in which
 * backed-out gets are seen by the GetCursor is undefined.  They will 
 * generally be seen in priority order, but within priority order the sequence
 * may not be preserved.  Since timing issues can affect the order, no assumptions
 * about this order should be made.
 * 
 * <p><b><u>Backout - Reavailability</u></b><br>
 * </p>
 * The combination of the above specifications means that
 * <ul>
 * <li>No available {@link AbstractItem} will be missed</li>
 * <li>Reasonable ordering is provided.</li>
 * <li>If an {@link AbstractItem} is unlocked, it is very likely to be seen
 * again, if it is unlocked again, and again and again it will be seen again, 
 * and again, and......</li>
 * </ul>
 */
public interface LockingCursor {
    
    /**
     * Declare that this cursor is no longer required.  This allows the message 
     * store to release resources.  Once the cursor has been released it is
     * unlikely to work correctly.
     */
    public void finished();

    /**
     * @return the {@link Filter} that was specified at cursor creation time.
     */
    public Filter getFilter();

    /**
	 * @return the lockID used by the cursor. Note that a value is returned
     * even from a browse cursor, though the browse cursor does not perform
     * any locking.  The lockID for a get cursor can be set at creation 
     * time @see ItemStream#newGetCursor(Filter). 
     */
	public long getLockID();

    /**
     * Reply the next {@link AbstractItem} that matches the filter specified when
     * the cursor was created.
     * Method next.
     * @return the next matching {@link AbstractItem}, or null if there is none.
     */
    public AbstractItem next() throws MessageStoreException;
}
