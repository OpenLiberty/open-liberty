package com.ibm.ws.objectManager;


/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * Extends the Map interface to support a set of ordered keys under the scope of
 * Transactions.
 * 
 * @See java.util.SortedMap
 * @see TreeMap
 */

public interface SortedMap extends Map
{

    /**
     * @return java.util.Comparator used by the map.
     */
    java.util.Comparator comparator();

    SortedMap subMap(Object fromKey, Object toKey);

    SortedMap headMap(Object toKey);

    SortedMap tailMap(Object fromKey);

    /**
     * Returns the first (lowest) key currently in this sorted map.
     * 
     * @param Transaction controling visibility of the Map.
     * @return Object the first key currently visible in this sorted map.
     * @throws ObjectManagerException.
     */
    Object firstKey(Transaction transaction) throws ObjectManagerException;

    /**
     * Returns the last (highest) key currently in this sorted map.
     * 
     * @param Transaction controling visibility of the Map.
     * @return Object the last key currently visible in this sorted map.
     * @throws ObjectManagerException.
     */
    Object lastKey(Transaction transaction) throws ObjectManagerException;
}
