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
 * An object that maps keys to values. This interface allows duplicate keys
 * the order of any duplicate keys being determined by the implementation,
 * typically it is insert order. This is an adaptation of the java.util.Map
 * interface that allows mapping of Tokens under the scope of Transactions.
 * 
 * @See java.util.Map
 * @see TreeMap
 */
public interface Map
{
    /**
     * Returns the number of tuples in this Map visible to the Transaction. A null Transaction indicates the size as
     * viewed by any Transaction, ie. the elements that are not currently being added by other Transactions. If the Map
     * contains more than <tt>Long.MAX_VALUE</tt> elements, returns <tt>Long.MAX_VALUE</tt>.
     * <p>
     * 
     * @param Transaction which sees the Map as this size, may be null.
     * @return long the number of key-value mappings in this map, visible to the Transaction.
     * @throws ObjectManagerException.
     */
    long size(Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Returns the number of dirty key-value mappings in this map. If the map contains more than <tt>Long.MAX_VALUE</tt>
     * elements, returns <tt>Long.MAX_VALUE</tt>.
     * <p>
     * 
     * This implementation returns <tt>entrySet().size()</tt>.
     * 
     * @return the number of key-value mappings in this map.
     * @exception ObjectManagerException.
     */
    long size()
                    throws ObjectManagerException;

    /**
     * Indicates if the Map contains no key-value pairs as seen by the Transaction.
     * <p>
     * Returns <tt>true</tt> if this map contains no key-value mappings visible to the Trasnaction.
     * 
     * @param Transaction which sees the Map as empty, may be null.
     * @return boolean true if this map contains no key-value mappings visible to the Transaction.
     * @throws ObjectManagerException.
     */
    boolean isEmpty(Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     * 
     * @param Object the key whose presence in this map is to be tested.
     * @param Transaction which controls visibility of Map Entries.
     * @return Boolean <tt>true</tt> if this map contains a mapping for the specified key.
     * 
     * @throws ObjectManagerException.
     */
    boolean containsKey(Object key,
                        Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the specified value.
     * 
     * @param Token whose presence in this map is to be tested.
     * @param Transaction which controls visibility of Map Entries.
     * @return <tt>true</tt> if this map maps one or more keys to the specified value.
     * @throws ObjectManagerException.
     */
    boolean containsValue(Token value,
                          Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Returns the Token assigned to the key or null if there is no such mapping.
     * 
     * @param Object the key mapping to the Token.
     * @param Transaction controling visibility of the Entry.
     * @return Token associated with the key or null if there is none.
     * 
     * @throws ObjectManagerException.
     */
    Token get(Object key,
              Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Associates the Token with the key and replaces any existing Token associated with the key.
     * 
     * @param Object key which the Token is to be associated with.
     * @param Token for the ManagedObject to be associated with the specified key.
     * @param Transaction controling the insertion into the Map.
     * @return Token previously associated with the key or null if there is none.
     * 
     * @throws ObjectManagerException.
     */
    Token put(Object key,
              Token value,
              Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Removes the Token associated with the key from the Map.
     * 
     * @param Object the key which accesses the required Token.
     * @param Transaction controling the removal of the mapping from the Map.
     * @return Token currently associated with the key, or null if there is none.
     * 
     * @throws ObjectManagerException.
     */
    Token remove(Object key,
                 Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Copies all of the mappings from the specified map to this map.
     * 
     * @param Map containing the Mappings to be stored in this map.
     * @param Transaction controling the insertion of entries into the Map.
     * 
     * @throws ObjectManagerException.
     */
    void putAll(Map map,
                Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Removes all mappings from this map.
     * 
     * @param Transaction controling the removal of the entries from the Map.
     * @throws ObjectManagerException.
     */
    void clear(Transaction transaction) throws ObjectManagerException;

    /**
     * Returns a Collection containing the Keys in this Map. This is a view of an existing Map, no new Map is created by
     * extending ManagedObject. Changes to the view change the underlying Map.
     * <p>
     * Note that we return a collection rather than a Set since these Maps admit duplucate entries.
     * <p>
     * 
     * @return Collection of keys (Objects) contained in this map.
     */
    Collection keyCollection();

    /**
     * The Collection of Tokens in this Map. This is a view of an existing Map, no new Map is created by extending
     * ManagedObject. Changes to the view change the underlying Map.
     * <p>
     * 
     * @return Collection of the Tokens contained in this map.
     */
    Collection values();

    /**
     * Returns a collection view of the Map.Entry entries in this Map. This is not a new Map and its does not extend
     * ManagedObject, just a view of the underlying Map.
     * <p>
     * Note that although the Set of Entries contains no duplicates the collection of Keys or Tokens may contain
     * duplicates.
     * <p>
     * 
     * @return Set which is a view of the Map.Entry's contained in this Map.
     * @throws ObjectManagerException.
     */
    Set entrySet()
                    throws ObjectManagerException;

    /**
     * A container for a tuplet as contained in Collection returned by entryCollection().
     */
    interface Entry
    {
        /**
         * @return Object which is the key used to access this Entry.
         */
        Object getKey();

        /**
         * @return the Token corresponding to this entry.
         */
        Token getValue();

        /**
         * Replaces the value corresponding to this entry with the specified
         * value (optional operation). (Writes through to the map.) The
         * behavior of this call is undefined if the mapping has already been
         * removed from the map (by the iterator's <tt>remove</tt> operation).
         * 
         * @param newValue the new Value to be associated with this Map.Entry
         *            when the transaction commits.
         * @param Transaction which controls the update.
         * @return Token currently associated with the entry.
         * 
         * @throws UnsupportedOperationException if the <tt>put</tt> operation
         *             is not supported by the backing map.
         * @throws ClassCastException if the class of the specified value
         *             prevents it from being stored in the backing map.
         * @throws IllegalArgumentException if some aspect of this value
         *             prevents it from being stored in the backing map.
         * @throws NullPointerException the backing map does not permit
         *             <tt>null</tt> values, and the specified value is
         *             <tt>null</tt>.
         * @throws ObjectManagerexception.
         */
        Token setValue(Token value,
                       Transaction transaction)
                        throws ObjectManagerException;

        public static final int stateError = 0; // A state error has occured.
        public static final int stateConstructed = 1; // Not yet part of a map.
        public static final int stateToBeAdded = 2; // Part of the map but not available to other transactions.
        public static final int stateAdded = 3; // Added.
        public static final int stateNotAdded = 4; // Added, but removed because of backout.  
        public static final int stateToBeDeleted = 5; // Will be removed from the map.
        public static final int stateRemoved = 6; // Removed from the map. 
        public static final int stateDeleted = 7; // Removed from the map and deleted. 

        /**
         * Returns the state of the Enry.
         * 
         * @return int the state of this entry.
         * @throws ObjectManagerException.
         */
        int getEntryState()
                        throws ObjectManagerException;

    } // inner interface Entry.
} // interface Map.