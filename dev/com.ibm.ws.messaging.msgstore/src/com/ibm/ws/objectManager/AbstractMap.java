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
 * Strarter implementation of the <tt>Map</tt> interface. This implentation 
 * creates another recoverable Map by extending ManagedObject.
 * <p>
 * To make a concrete implementation extend this class and implement.
 * <ol>
 * entrySet();
 * put();
 * remove();
 * <eol>

 * @see Map
 * @see java.util.Map
 */

/**
 * @author Andrew_Banks
 * 
 */
public abstract class AbstractMap
                extends AbstractCollection
                implements Map
{
    private static final Class cclass = AbstractMap.class;

    /**
     * Default, no argument constructor.
     */
    protected AbstractMap()
    {} // AbstractMap().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#size(com.ibm.ws.objectManager.Transaction)
     */
    public long size(Transaction transaction)
                    throws ObjectManagerException
    {
        return entrySet().size(transaction);
    } // size().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#size()
     */
    public long size()
                    throws ObjectManagerException
    {
        return entrySet().size();
    } // size().

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * 
     * @param Transaction which sees the map as empty.
     * @return <tt>true</tt> if this map contains no key-value mappings.
     * @exception ObjectManagerException.
     */
    public boolean isEmpty(Transaction transaction)
                    throws ObjectManagerException
    {
        return size(transaction) == 0;
    } // isEmpty().

    /**
     * Determines if the Map contains the Token
     * 
     * @param Token whose presence in this map is to be tested.
     * @param Transaction which determines visibility of Entries in the Map.
     * 
     * @return Boolean <tt>true</tt> if the map contains at least one mapping to the Token visible to the Transaction.
     * @exception ObjectManagerException.
     */
    public boolean containsValue(Token value,
                                 Transaction transaction)
                    throws ObjectManagerException
    {
        try {
            for (Iterator iterator = entrySet().iterator();;) {
                Entry entry = (Entry) iterator.next(transaction);
                Token entryValue = entry.getValue();
                if (value == entryValue) {
                    return true;
                }
            }
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed, just exited search.
            return false;
        } // try.

    } // containsValue().

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     * <p>
     * 
     * @param Object the key whose presence in this map is to be tested.
     * @param Transaction which determines visibility of Entries in the Map.
     * @return boolean <tt>true</tt> if this map contains a mapping for the specified key.
     * 
     * @exception ObjectManagerException.
     */
    public boolean containsKey(Object key,
                               Transaction transaction)
                    throws ObjectManagerException
    {
        try {
            for (Iterator iterator = entrySet().iterator();;) {
                Entry entry = (Entry) iterator.next(transaction);
                Object entryKey = entry.getKey();
                if (key == entryKey || key.equals(entryKey)) {
                    return true;
                }
            }
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed, just exited search.
            return false;
        } // try.
    } // containsKey().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#get(java.lang.Object, com.ibm.ws.objectManager.Transaction)
     */
    public Token get(Object key,
                     Transaction transaction)
                    throws ObjectManagerException
    {
        try {
            for (Iterator iterator = entrySet().iterator();;) {
                Entry entry = (Entry) iterator.next(transaction);
                Object entryKey = entry.getKey();
                if (key == entryKey || key.equals(entryKey)) {
                    return entry.getValue();

                }
            }
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed, just exited search.
            return null;
        } // try.
    } // get().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#put(java.lang.Object, com.ibm.ws.objectManager.Token,
     * com.ibm.ws.objectManager.Transaction)
     */
    public Token put(Object key,
                     Token value,
                     Transaction transaction)
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException();
    } // put().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#remove(java.lang.Object, com.ibm.ws.objectManager.Transaction)
     */
    public Token remove(Object key,
                        Transaction transaction)
                    throws ObjectManagerException
    {
        Token returnToken = null;
        try {
            for (Iterator iterator = entrySet().iterator();;) {
                Entry entry = (Entry) iterator.next(transaction);
                Object entryKey = entry.getKey();
                if (key == entryKey || key.equals(entryKey)) {
                    returnToken = entry.getValue();
                    iterator.remove(transaction);
                    break;
                }
            } // for...
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed, just exited search.
        } // try.

        return returnToken;
    } //  remove().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#putAll(com.ibm.ws.objectManager.Map, com.ibm.ws.objectManager.Transaction)
     */
    public void putAll(Map otherMap,
                       Transaction transaction)
                    throws ObjectManagerException
    {
        for (Iterator iterator = otherMap.entrySet().iterator(); iterator.hasNext(transaction);) {
            Entry entry = (Entry) iterator.next(transaction);
            put(entry.getKey(),
                entry.getValue(),
                transaction);
        } // for...
    } // putAll().

    /**
     * Removes all mappings from this map.
     * 
     * @param Transaction which controls removal of Entries from the Map.
     * 
     * @exception ObjectManagerException.
     */
    public void clear(Transaction transaction)
                    throws ObjectManagerException
    {
        entrySet().clear(transaction);
    }

    /**
     * Each of these fields are initialized to contain an instance of the appropriate view the first time this view is
     * requested. The views are stateless, so there's no reason to create more than one of each.
     */
    transient volatile Collection keyCollection = null;
    transient volatile Collection values = null;

    public Collection keyCollection()
    {
        if (keyCollection == null) {
            keyCollection = new AbstractCollectionView()
            {
                public Iterator iterator()
                                throws ObjectManagerException
                {
                    return new Iterator()
                    {
                        private Iterator iterator = entrySet().iterator();

                        public boolean hasNext(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            return iterator.hasNext(transaction);
                        }

                        public boolean hasNext()
                                        throws ObjectManagerException
                        {
                            return iterator.hasNext();
                        }

                        public Object next(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            return ((Entry) iterator.next(transaction)).getKey();
                        }

                        public Object next()
                                        throws ObjectManagerException
                        {
                            return ((Entry) iterator.next()).getKey();
                        }

                        public Object remove(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            return iterator.remove(transaction);
                        }
                    };
                }

                public long size(Transaction transaction)
                                throws ObjectManagerException
                {
                    return AbstractMap.this.size(transaction);
                }

                public long size()
                                throws ObjectManagerException
                {
                    return AbstractMap.this.size();
                }

                public boolean contains(Object key,
                                        Transaction transaction)
                                throws ObjectManagerException
                {
                    return AbstractMap.this.containsKey(key,
                                                        transaction);
                }
            };
        }
        return keyCollection;
    }

    /**
     * @return Collection view of the Tokens in this map.
     */
    public Collection values()
    {
        if (values == null) {
            values = new AbstractCollectionView()
            {
                public Iterator iterator()
                                throws ObjectManagerException
                {
                    return new Iterator()
                    {
                        private Iterator i = entrySet().iterator();

                        public boolean hasNext(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            return i.hasNext(transaction);
                        }

                        public boolean hasNext()
                                        throws ObjectManagerException
                        {
                            return i.hasNext();
                        }

                        public Object next(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            return ((Entry) i.next(transaction)).getValue();
                        }

                        public Object next()
                                        throws ObjectManagerException
                        {
                            return ((Entry) i.next()).getValue();
                        }

                        public Object remove(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            return i.remove(transaction);
                        }
                    };
                }

                public long size(Transaction transaction)
                                throws ObjectManagerException
                {
                    return AbstractMap.this.size(transaction);
                }

                public long size()
                                throws ObjectManagerException
                {
                    return AbstractMap.this.size();
                }

                public boolean contains(Token value,
                                        Transaction transaction)
                                throws ObjectManagerException
                {
                    return AbstractMap.this.containsValue(value,
                                                          transaction);
                }
            };
        }
        return values;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#entrySet()
     */
    public abstract Set entrySet()
                    throws ObjectManagerException;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        try {
            return new String("AbstractMap"
                              + "(size()="
                              + size()
                              + " size(null)="
                              + size(null)
                              + ")"
                              + " "
                              + super.toString());
        } catch (ObjectManagerException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                "toString",
                                                exception,
                                                "1:408:1.8");
            return super.toString() + "(" + exception + ")";
        }
    } // toString().

} // class AbstractMap.
