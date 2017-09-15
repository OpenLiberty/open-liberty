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
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.cache.links.Priorities;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class implements an item cursor that is aware of priorities. The cursor operates on a
 * PrioritizedList. Each sublist in the prioritized list is represented by a subcursor. Since
 * sublists are lazily initialized, subcursors must be as well.
 * 
 * This class also provides support for building singly-linked lists of itself. These lists are
 * used by the owning prioritized lists to manage the cursors open on it.
 * 
 * This class can operate as a browse (non-locking) or a get (locking) cursor. In browse mode it can
 * operate in a mode where all items (including those locked by transactions or get cursors). In get
 * mode it can have the 'jumpBack' functionality disabled (but not re-enabled).
 */
public class PrioritizedCursor implements LockingCursor, NonLockingCursor
{
   
    private static TraceComponent tc = SibTr.register(PrioritizedCursor.class,
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private boolean _allowUnavailableItems = false;

    private final Filter _itemFilter;

    private boolean _jumpbackEnabled;

    private final long _lockID;

    // references to previous and next cursors owned by the same list
    private PrioritizedCursor _previousCursor = null;
    private PrioritizedCursor _nextCursor = null;

    // the owning list
    private final PrioritizedList _owningList;

    // subcursors are in numerical priority order 0..9.
    // so we need to scan from top downwards to get correct (highest to lowest) priority order.
    // Lazily initialised under the monitor for this object
    private final Subcursor[] _subCursors;

    /**
     * create a cursor
     */
    PrioritizedCursor(PrioritizedList parentStream, Filter itemFilter, long lockID, boolean jumpbackEnabled)
    {
        super();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", parentStream);

        _owningList = parentStream;
        _itemFilter = itemFilter;
        _lockID = lockID;
        _jumpbackEnabled = jumpbackEnabled;
        _subCursors = new Subcursor[Priorities.NUMBER_OF_PRIORITIES];

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    private final Subcursor _getSubCursor(int priority)
    {
        Subcursor subCursor = _subCursors[priority];
        // lazy initialize if necessary
        if (subCursor == null)
        {
            LinkedList subList = _owningList.getPrioritySublist(priority);
            if (subList != null)
            {
                synchronized(_subCursors)
                {
                    // Defect 532041/PK75501
                    // Check the cursor again first and set it in our 
                    // return variable as if it now exists (due to another 
                    // thread creating it before we got into the 
                    // synchronized block) we should be returning it.
                    subCursor = _subCursors[priority];

                    if (subCursor == null)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Lazy initialize sub cursor for priority: "+priority);

                        subCursor = new Subcursor(subList, _itemFilter, _jumpbackEnabled);
                        _subCursors[priority] = subCursor;
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Using sub cursor: "+subCursor+" for priority: "+priority);
        return subCursor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.NonLockingCursor#allowUnavailableItems()
     */
    public final void allowUnavailableItems()
    {
        _allowUnavailableItems = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Cursor#finished()
     */
    public final void finished()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "finished");

        synchronized(_subCursors)
        {
            for (int priority = Priorities.LOWEST_PRIORITY; priority <= Priorities.HIGHEST_PRIORITY; priority++)
            {
                if (null != _subCursors[priority])
                {
                    _subCursors[priority].finished();
                    _subCursors[priority] = null;
                }
            }
        }
        _owningList._removeCursor(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "finished");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Cursor#getFilter()
     */
    public final Filter getFilter()
    {
        return _itemFilter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Cursor#getLockID()
     */
    public final long getLockID()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getLockID");
            SibTr.exit(this, tc, "getLockID", Long.valueOf(_lockID));
        }
        return _lockID;
    }

    /**
     * Get the following cursor. Should only be called under owning list's monitor
     */
    final PrioritizedCursor getNextCursor()
    {
        return _nextCursor;
    }

    /**
     * Get the previous cursor. Should only be called under owning list's monitor
     */
    final PrioritizedCursor getPreviousCursor()
    {
        return _previousCursor;
    }

    /**
     * @param link
     * @throws SevereMessageStoreException 
     */
    final void linkAvailable(AbstractItemLink link) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "linkAvailable", link);

        int priority = link.getPriority();
        Subcursor subCursor = _getSubCursor(priority);
        if (null != subCursor)
        {
            subCursor.available(link);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "linkAvailable");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Cursor#next()
     */
    public final AbstractItem next() throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "next");

        AbstractItem found = null;
        for (int priority = Priorities.HIGHEST_PRIORITY;
             null == found && priority >= Priorities.LOWEST_PRIORITY;
             priority--)
        {
            Subcursor subCursor = _getSubCursor(priority);
            if (null != subCursor)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Search on subcursor for priority: "+priority);

                if (AbstractItem.NO_LOCK_ID == _lockID)
                {
                    found = subCursor.next(_allowUnavailableItems);
                }
                else
                {
                    found = subCursor.next(_lockID);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "next", found);
        return found;
    }

    /**
     * Set the following cursor. Should only be called under owning list's monitor
     * 
     * @param nextCursor
     */
    final void setNextCursor(PrioritizedCursor nextCursor)
    {
        _nextCursor = nextCursor;
    }

    /**
     * Set the previous cursor. Should only be called under owning list's monitor
     * 
     * @param prevCursor
     */
    final void setPreviousCursor(PrioritizedCursor prevCursor)
    {
        _previousCursor = prevCursor;
    }

    /**
     * @param writer
     */
    public final void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        writer.write("<cursor");
        writer.write(" sihc=\"");
        int sihc = System.identityHashCode(this);
        writer.write(Integer.toHexString(sihc));
        writer.write('"');

        if (AbstractItem.NO_LOCK_ID != _lockID)
        {
            writer.write(" lockId=\"");
            writer.write(Long.toString(_lockID));
            writer.write('"');
        }

        if (_allowUnavailableItems)
        {
            writer.write(" allowUnavailable=\"true\"");
        }

        if (_jumpbackEnabled)
        {
            writer.write(" jumpbackEnabled=\"true\"");
        }

        boolean closedTag = false;

        for (int priority = Priorities.HIGHEST_PRIORITY; priority >= Priorities.LOWEST_PRIORITY; priority--)
        {
            Subcursor cursor = _subCursors[priority];
            if (null != cursor)
            {
                Object link = cursor.peepLastLink();
                if (null != link)
                {
                    if (!closedTag)
                    {
                        writer.write(">");
                        writer.indent();

                        closedTag = true;
                    }
                    writer.newLine();
                    writer.write("<priority_");
                    writer.write(Integer.toString(priority));
                    writer.write(" lastLink=\"");
                    int linkId = System.identityHashCode(link);
                    writer.write(Integer.toHexString(linkId));
                    writer.write('"');
                    writer.write(" />");
                }
            }
        }

        if (closedTag)
        {
            writer.outdent();
            writer.newLine();
            writer.write("</cursor>");
        }
        else
        {
            writer.write("/>");
        }
    }
//673411--starts
    
    public final AbstractItem next(int fromIndex) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "next( fromIndex)"+fromIndex);

        AbstractItem found = null;
        for (int priority = Priorities.HIGHEST_PRIORITY;
             null == found && priority >= Priorities.LOWEST_PRIORITY;
             priority--)
        {
            Subcursor subCursor = _getSubCursor(priority);
            if (null != subCursor)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Search on subcursor for priority: "+priority);

                if (AbstractItem.NO_LOCK_ID == _lockID)
                {
                    found = subCursor.next(_allowUnavailableItems,fromIndex);// 673411
                }
                else
                {
                    found = subCursor.next(_lockID,fromIndex); //673411
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "next( fromIndex)"+fromIndex, found);
        return found;
    }
//673411-ends 
    
}
