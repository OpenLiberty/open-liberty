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
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;

/**
 * This class implements a nonlocking cursor on an unprioritized list. It simply delegates to
 * the subcursor that it contains. 
 */
public class UnprioritizedNonlockingCursor implements NonLockingCursor
{
  
    private boolean _allowUnavailable = false;
    private final Subcursor _cursor;

    public UnprioritizedNonlockingCursor(LinkedList parent, final Filter filter)
    {
        // No jumpback on this type of cursor
        _cursor = new Subcursor(parent, filter, false);
    }

    public void allowUnavailableItems()
    {
        _allowUnavailable = true;
    }

    public void finished()
    {
        _cursor.finished();
    }

    public Filter getFilter()
    {
        return _cursor.getFilter();
    }

    public AbstractItem next() throws MessageStoreException
    {
        return _cursor.next(_allowUnavailable);
    }
//673411-start
@Override 
   public AbstractItem next(int fromIndex)	throws MessageStoreException
   {
	return _cursor.next(_allowUnavailable);     
   }
// 673411-ends
}
