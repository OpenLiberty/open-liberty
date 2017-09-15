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
package com.ibm.wsspi.adaptable.module;

/**
 * Provides the ability to add new entries with given data (i.e file-like
 * entries) to the container adapted to this interface.
 * 
 * Note that these entries may not be backed by a file on disk, so invoking
 * methods such as getResource() and getPhysicalPath() on the resulting artifact
 * entry may return null (depending on the underlying implementation).
 */
public interface AddEntryToOverlay {

    /**
     * Add a new entry to the (overlay of) the container adapted to this
     * interface, with the entry containing the given data.
     * 
     * @param entryRelativePath path relative to this container at which the
     *            entry should be added (an absolute path will result in no
     *            entry being added and false being returned).
     * @param entryData data the entry should contain.
     * @return true if successful, false otherwise.
     */
    public boolean add(String entryRelativePath, String entryData);
}
