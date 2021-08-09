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
package com.ibm.ws.sib.msgstore.impl;

import java.io.IOException;

import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

/**
 * This implements a simple hashmap, which associates longs with abstract 
 * item links.
 * It has several advantages over the java.util.Hashmap:
 * <ul> 
 * <li> It maps a long (primitive) to an abstract item, thereby eliminating
 *      the need to create instances of java.lang.Long both for storage and
 *      for retrieval/removal.
 * </li>
 * <li> It maps the key to an AbstractItemLink, so we do not need to cast
 *      return values from objects all the time.
 * </li>
 * <li> It uses the long value rather than a calculated (and recalculated) 
 *      hashCode. This eliminates some processing overhead.
 * </li>
 * <li> It is fixed size. This means that it does not waste time growing.
 * </li>
 * <li> It is thread safe, synchronized at the level of the individual
 *      sublists. This means that it is effectively multithreaded to a
 *      maximum number of threads equal to the number of sublists.
 * </li>
 * </ul>
 *  
 * @author DrPhill
 */
public final class ItemMap implements Map, XmlConstants {
    /* Each sublist in the map is a linked list of the AbstractItems
     * object allocation.
     */
    private static final class Entry {
        private final long id;
        private final AbstractItemLink link;
        private Entry next;

        Entry(long i, AbstractItemLink l, Entry n) {
            link = l;
            next = n;
            id = i;
        }

        public String toString() {
            return "" + id + "=" + link;
        }
    }
    
    /* Use a named subclass of Object as a lock so we can easily spot them in
     * lock analysis experiments.
     * Instances of this class are used to synchronize access to sublists in
     * the map.
     */
    private static final class Lock {
    }
    
    /* Capacity mask is a bit-mask that allows us to quickly convert to the
     * index from any arbitrary value. 
     */
    private final int _capacityMask;
    
    /* Sublists are linked lists of entries. Each array entry may be null.
     * Access to or modification of an array entry should only occur
     * within the monitor of the matching lock.
     */
    private final Entry[] _entry;
    
    /* We use a table of locks, one for each sublist. This way we can have
     * a separate lock for each sublist. The lock objects are lazily 
     * initialized on first access.
     */
    private final Lock[] _lock;

    /* The number of key-value mappings contained in this identity hash map.
     */
    private int _size;

    /**
     * @param magnitude relative size of the internal table. The table is
     * allocated at startup and never resized. The table is allocated at
     * a size of 2^^magnitude. The use of a power of 2 allows us to map
     * keys quickly into the table.
     */
    ItemMap(int magnitude) {
        super();
        if (magnitude > 30) {
            magnitude = 30;
        } else if (magnitude < 8) {
            magnitude = 8;
        }
        int capacity = 2 << magnitude;
        _entry = new Entry[capacity];
        _lock = new Lock[capacity];
        _capacityMask = capacity - 1;
    }

    /**
     * @param i index of sublist to be manipulated
     * @return lock object for the list
     */
    private final Lock _getLock(int i) {
        Lock lock = _lock[i];
        if (null == lock) {
            synchronized (_lock) {
                lock = new Lock();
                _lock[i] = lock;
            }
        }
        return lock;
    }

    /**
     * @param key
     * @return integer index of id in _entry array
     */
    private int _indexOfKey(final long key) {
        int i = (int) (key & _capacityMask);
        return i;
    }

    /**
     * Returns the AbstractItemLink to which the specified key is mapped in this map
     * or <tt>null</tt> if the map contains no mapping for this key.
     *
     * @param   key the key whose associated value is to be returned.
     * @return  the value to which this map maps the specified key, or
     *          <tt>null</tt> if the map contains no mapping for this key.
     * @see #put(long, com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink)
     */
    public final AbstractItemLink get(final long key) {
        AbstractItemLink link = null;
        int i = _indexOfKey(key);
        synchronized (_getLock(i)) {
            Entry entry = _entry[i];
            while (null == link && null != entry) {
                if (key == entry.id) {
                    link = entry.link;
                } else {
                    entry = entry.next;
                }
            }
        }
        return link;
    }

    public final int getSize() {
        return _size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return _size == 0;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param link link to be associated with the specified key.
     */
    public final void put(long key, AbstractItemLink link) {
        int i = _indexOfKey(key);

        synchronized (_getLock(i)) {
            // NOTE: this pushes the new entry onto the front of the list. Means we
            // will retrieve in lifo order. This may not be optimal
            Entry nextEntry = _entry[i];
            Entry newEntry = new Entry(key, link, nextEntry);
            _entry[i] = newEntry;
            _size++;
        }
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.
     */
    public final AbstractItemLink remove(long key) {
        AbstractItemLink link = null;

        int i = _indexOfKey(key);

        synchronized (_getLock(i)) {
            Entry previousEntry = _entry[i];
            Entry thisEntry = previousEntry;
            while (null != thisEntry && null == link) {
                Entry nextEntry = thisEntry.next;
                if (key == thisEntry.id) {
                    link = thisEntry.link;
                    _size--;
                    if (previousEntry == thisEntry) {
                        _entry[i] = nextEntry;
                    } else {
                        previousEntry.next = nextEntry;
                    }
                }
                previousEntry = thisEntry;
                thisEntry = nextEntry;
            }
        }
        return link;
    }

    /**
     * 
     */
    public final void clear() {
        for (int i = 0; i < _entry.length; i++) {
            _entry[i] = null;
        }
        
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.Map#xmlWriteOn(com.ibm.ws.sib.utils.ras.FormattedWriter)
     */
    public void xmlWriteOn(FormattedWriter writer) throws IOException {
        writer.newLine();
        writer.startTag(XML_ITEM_MAP);
        writer.indent();
        for (int i = 0; i < _entry.length; i++) {
            Entry entry =  _entry[i];
            while (null != entry) {
                AbstractItemLink ail = entry.link;
                writer.newLine();
                ail.xmlShortWriteOn(writer);
                entry = entry.next;
            }
        }
        writer.outdent();
        writer.newLine();
        writer.endTag(XML_ITEM_MAP);
    }

}
