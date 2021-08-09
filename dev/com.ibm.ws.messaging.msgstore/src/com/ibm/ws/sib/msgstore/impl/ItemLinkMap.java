package com.ibm.ws.sib.msgstore.impl;
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

import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

import java.util.concurrent.atomic.AtomicInteger;

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
 */
public final class ItemLinkMap implements Map, XmlConstants
{
    /* Use a named subclass of Object as a lock so we can easily spot them in
     * lock analysis experiments.
     * Instances of this class are used to synchronize access to sublists in
     * the map.
     */
    private static final class Lock {}

    /* Capacity mask is a bit-mask that allows us to quickly convert to the
     * index from any arbitrary value. It is one less than the size of the
     * sublist array. Since the sublist array size is always a power of two,
     * the mask is always a bitmask with all the lower bits set, and the
     * higher bits cleared. This gives us a fast method of doing 'remainder'
     * on the id to fit into the index.
     * Since ids increase by one each time, each successive new item will go
     * in a different slot to the last, resulting in a perfect hash distribution.
     * (Better in fact than the result of rehashing used by java.util.Hashmap!).
     * Negative numbers (used by non-persistent messages) will form a separate,
     * interleaved, series.
     */
    private final int _capacityMask;

    /* Lock mask is a bit-mask that allows us to quickly convert to the
     * index from any arbitrary value. It is one less than the size of the
     * lock array. Since the lock array size is always a power of two,
     * the mask is always a bitmask with all the lower bits set, and the
     * higher bits cleared. This gives us a fast method of doing 'remainder'
     * on the id to fit into the lock array.
     * Since ids increase by one each time, each successive new item will go
     * in a different slot to the last, resulting in a perfect hash distribution.
     * (Better in fact than the result of rehashing used by java.util.Hashmap!).
     * Negative numbers (used by non-persistent messages) will form a separate,
     * interleaved, series.
     */
    private final int _lockMask;

    /* Sublists are linked lists of itemLinks. Each array entry may be null.
     * Access to or modification of an array entry should only occur
     * within the monitor of the matching lock.
     */
    private volatile AbstractItemLink[] _entry; // 666212
    private final Integer _linkCapacity; // 666212
    private final Object _entryCreationLock = new Object(){};// 666212


    /* We use a table of locks, one for each sublist. This way we can have
     * a separate lock for each sublist. The lock objects are lazily
     * initialized on first access.
     */
    private final Lock[] _lock;

    /* The number of key-value mappings contained in this identity hash map.
     */
    // Defect 597160
    // Use an AtomicInteger here to make changes to the size of the map
    // atomic no matter which sub-lock is taken to control access to the map
    // contents.
    private AtomicInteger _size;

    /**
     * @param magnitude relative size of the internal table. The table is
     * allocated at startup and never resized. The table is allocated at
     * a size of 2^^magnitude. The use of a power of 2 allows us to map
     * keys quickly into the table.
     */
    ItemLinkMap(int magnitude)
    {
        this(magnitude, 8);
    }

    /**
     * @param magnitude relative size of the internal table. The table is
     * allocated at startup and never resized. The table is allocated at
     * a size of 2^^magnitude. The use of a power of 2 allows us to map
     * keys quickly into the table.
     */
    ItemLinkMap(int magnitude, int parallelism)
    {
        super();

        if (magnitude > 30)
        {
            magnitude = 30;
        }
        else if (magnitude < 8)
        {
            magnitude = 8;
        }

        if (parallelism > 15)
        {
            parallelism = 15;
        }
        else if (parallelism < 0)
        {
            parallelism = 0;
        }

        int capacity = 2 << magnitude;
        _entry = null; //666212 - Lazily construct this later
        _capacityMask = capacity - 1;
        _linkCapacity=new Integer(capacity); // 666212


        capacity = 2 << parallelism;
        _lock = new Lock[capacity];
        _lockMask = capacity - 1;

        // Defect 597160
        _size = new AtomicInteger(0);
    }


    /**
     * @param i index of sublist to be manipulated
     * @return lock object for the list
     */
    private final Lock _getLock(final long key)
    {
        final int i = (int)(_lockMask & key);
        Lock lock = _lock[i];
        // the lockObject will only be assigned once, so if it is non-null
        // we do not to contest the monitor. If the lockObject is null we
        // will lazily initialize it.
        if (null == lock)
        {
            // we need to synchronize on the monitor to ensure that we are not
            // in a race to lazily initialize
            synchronized (_lock)
            {
                lock = _lock[i];
                // check for null again now we have the monitor
                if (null == lock)
                {
                    lock = new Lock();
                    _lock[i] = lock;
                }
            }
        }
        return lock;
    }

    /**
     * @param key
     * @return integer index of id in _entry array
     */
    private final int _indexOfKey(final long key)
    {
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
    public final AbstractItemLink get(final long key)
    {
        AbstractItemLink link = null;
        synchronized (_getLock(key))
        {
            if(null != _entry) // 666212
            { // 666212
              int i = _indexOfKey(key);
              AbstractItemLink entry = _entry[i];
              while (null == link && null != entry)
              {
                  if (key == entry.getID())
                  {
                      link = entry;
                  }
                  else
                  {
                      entry = entry.getNextMappedLink();
                  }
              }
            } // 666212
        }
        return link;
    }

    public final int getSize()
    {
        // Defect 597160
        return _size.get();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public final boolean isEmpty()
    {
        // Defect 597160
        return _size.get() == 0;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param link link to be associated with the specified key.
     */
    public final void put(long key, AbstractItemLink link)
    {

        // 666212 starts
        // we do not want to contest the monitor. If the lockObject is null we
        // will lazily initialize it.
        if (null == _entry)
        {
            // we need to synchronize on the monitor to ensure that we are not
            // in a race to lazily initialize
            synchronized ( _entryCreationLock )
            {
                if ( null == _entry ){
                    _entry = new AbstractItemLink[_linkCapacity.intValue()];
                }
            }
        }
        // 666212 ends

        synchronized (_getLock(key))
        {
            // NOTE: this pushes the new entry onto the front of the list. Means we
            // will retrieve in lifo order. This may not be optimal
            int i = _indexOfKey(key);
            AbstractItemLink nextEntry = _entry[i];
            _entry[i] = link;
            link.setNextMappedLink(nextEntry);
            _size.incrementAndGet(); // Defect 597160
        }
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.
     */
    public final AbstractItemLink remove(long key)
    {
        AbstractItemLink link = null;
        synchronized (_getLock(key))
        {
            if(null != _entry) /// 666212
            {// 666212
               int i = _indexOfKey(key);
               AbstractItemLink previousEntry = _entry[i];
               AbstractItemLink thisEntry = previousEntry;
               while (null != thisEntry && null == link)
               {
                   AbstractItemLink nextEntry = thisEntry.getNextMappedLink();
                   if (key == thisEntry.getID())
                   {
                       link = thisEntry;
                       _size.decrementAndGet(); // Defect 597160
                       if (previousEntry == thisEntry)
                       {
                           _entry[i] = nextEntry;
                       }
                       else
                       {
                           previousEntry.setNextMappedLink(nextEntry);
                       }
                   }
                   previousEntry = thisEntry;
                   thisEntry = nextEntry;
               }
            }// 666212
        }
        return link;
    }

    public final void clear()
    {
        for (int i = 0; null != _entry && i < _entry.length; i++) // 666212
        {
            _entry[i] = null;
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.Map#xmlWriteOn(com.ibm.ws.sib.utils.ras.FormattedWriter)
     */
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        writer.newLine();
        writer.startTag(XML_ITEM_MAP);
        writer.indent();
        for (int i = 0; null != _entry && i < _entry.length; i++) // 666212
        {
            AbstractItemLink ail = _entry[i];
            while (null != ail)
            {
                writer.newLine();
                ail.xmlShortWriteOn(writer);
                ail = ail.getNextMappedLink();
            }
        }
        writer.outdent();
        writer.newLine();
        writer.endTag(XML_ITEM_MAP);
    }
}
