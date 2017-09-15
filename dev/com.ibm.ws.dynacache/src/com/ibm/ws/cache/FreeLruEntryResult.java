/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

/**
 * This is the return structure for FreeLruEntry method in Cache.java.
 */
public class FreeLruEntryResult {
	
	public boolean success;      // entry evicted successful in FreeLruEntry
	public int  entriesRemoved;  // number of entries removed;
	public long bytesRemoved;    // number of bytes removed

	public FreeLruEntryResult() {
		success = false;
		entriesRemoved = 0;
		bytesRemoved = -1;
	}
}
