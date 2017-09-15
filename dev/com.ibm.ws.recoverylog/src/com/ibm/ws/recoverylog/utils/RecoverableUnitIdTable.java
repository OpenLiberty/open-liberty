/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.utils;

import com.ibm.ws.recoverylog.spi.TraceConstants;

import com.ibm.tx.util.LongObjectHashMap;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

public final class RecoverableUnitIdTable
{
	private static final TraceComponent tc = Tr.register(RecoverableUnitIdTable.class, TraceConstants.TRACE_GROUP, null);	
	
	private long _idCount = 1;
	private LongObjectHashMap _idMap = new LongObjectHashMap(256);

	/**
	 * Returns the next available id, starting from 1, and associates
     * it with the given object. This method should be used during
     * the creation of a new recoverable object.
	 * 
	 * @param obj The object to be associated with the id
	 * 
	 * @return The next available recoverable unit id 
	 */
	public final synchronized long nextId(Object obj)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "nextId", obj);
		
		long id = _idCount++;

		// Keep incrementing the id until we
		// find one that hasn't been reserved.
		while (_idMap.get(id) != null)
		{
			id = _idCount++;
		}

		// Add the new id to the map associating
		// it with the given object.
		_idMap.put(id, obj);

		if (tc.isEntryEnabled()) Tr.exit(tc, "nextId", new Long(id));

		return id;
	}

	/**
	 * Remove the given id from the map. This method should be
	 *  called at the end of a recoverable object's lifetime.
	 * 
	 * @param id The id to remove from the table
	 */
	public final synchronized void removeId(long id)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "removeId", new Long(id));

		_idMap.remove(id);

		if (tc.isEntryEnabled()) Tr.exit(tc, "removeId");
	}

	/**
	 * Reserve the given id and associate it with
	 * the given object. This method should be used
	 * during recovery when there is a requirement to
	 * create a new object with a specific id rather than
	 * the one that is next available.
	 * 
	 * @return true if the id was successfully reserved.
	 * @param id The value of the id to be reserved
	 * @param obj The object that requires the id 
	 */
	public final synchronized boolean reserveId(long id, Object obj)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "reserveId", new Object[] {new Long(id), obj});

		boolean reserved = false;

		// The id can only be reserved if it
		// isn't already in the map
		if (_idMap.get(id) == null)
		{
			_idMap.put(id, obj);
			reserved = true;
		}
    
		if (tc.isEntryEnabled()) Tr.exit(tc, "reserveId", new Boolean(reserved));

		return reserved;
	}

	/**
	 * Return an array of all the objects currently
	 * held in the table.
	 * 
	 * @return An array of all the objects in the table 
	 */
	public final synchronized Object[] getAllObjects()
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "getAllObjects");

		Object[] values = _idMap.values();

		if (tc.isEntryEnabled()) Tr.exit(tc, "getAllObjects", values);

		return values;
	}
}
