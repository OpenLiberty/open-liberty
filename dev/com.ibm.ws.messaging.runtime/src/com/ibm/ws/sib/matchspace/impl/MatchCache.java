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

package com.ibm.ws.sib.matchspace.impl;

/**
 * @author Neil Young
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import com.ibm.ws.sib.matchspace.utils.FFDC;
import com.ibm.ws.sib.matchspace.utils.Trace;
import com.ibm.ws.sib.matchspace.utils.TraceUtils;
import com.ibm.ws.sib.matchspace.utils.MatchSpaceConstants;

/**
 * Hashtable collision list.
 */
class FastHashtableEntry {
  int hash;
  Object key;
  Object value;
  FastHashtableEntry next;

  protected Object clone() {
    FastHashtableEntry entry = new FastHashtableEntry();
    entry.hash = hash;
    entry.key = key;
    entry.value = value;
    entry.next = (next != null) ? (FastHashtableEntry)next.clone() : null;
    return entry;
  }
}

/**
 * This class implements a hashtable, which maps keys to values. Any
 * non-<code>null</code> object can be used as a key or as a value.
 * <p>
 * To successfully store and retrieve objects from a hashtable, the
 * objects used as keys must implement the <code>hashCode</code>
 * method and the <code>equals</code> method.
 * <p>
 * An instance of <code>Hashtable</code> has two parameters that
 * affect its efficiency: its <i>capacity</i> and its <i>load
 * factor</i>. The load factor should be between 0.0 and 1.0. When
 * the number of entries in the hashtable exceeds the product of the
 * load factor and the current capacity, the capacity is increased by
 * calling the <code>rehash</code> method. Larger load factors use
 * memory more efficiently, at the expense of larger expected time
 * per lookup.
 * <p>
 * If many entries are to be made into a <code>Hashtable</code>,
 * creating it with a sufficiently large capacity may allow the
 * entries to be inserted more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.
 * <p>
 * This example creates a hashtable of numbers. It uses the names of
 * the numbers as keys:
 * <p><blockquote><pre>
 *     Hashtable numbers = new Hashtable();
 *     numbers.put("one", new Integer(1));
 *     numbers.put("two", new Integer(2));
 *     numbers.put("three", new Integer(3));
 * </pre></blockquote>
 * <p>
 * To retrieve a number, use the following code:
 * <p><blockquote><pre>
 *     Integer n = (Integer)numbers.get("two");
 *     if (n != null) {
 *         System.out.println("two = " + n);
 *     }
 * </pre></blockquote>
 *
 */
// Gryphon changes:
//   Name change.  This is a specialized Hashtable for use only as the MatchCache.
//   Removed locking.
//   Reordered some code so that concurrent gets are safe in the presence of (at most one) put or remove.
//      (puts and removes must be serialized).  NOTE: this assumption needs to be examined.
//   Add RehashFilter interface so that rehashing can be avoided if entries may be safely discarded.

public class MatchCache extends Dictionary
  implements Cloneable, java.io.Serializable {

    // Standard trace boilerplate
  private static final Class cclass = MatchCache.class;
  private static Trace tc = TraceUtils.getTrace(MatchCache.class,
      MatchSpaceConstants.MSG_GROUP_LISTS);

  /**
   * The hash table data.
   */
  protected transient FastHashtableEntry table[];

  /**
   * The total number of entries in the hash table.
   */
  protected transient int count;

  /**
   * Rehashes the table when count exceeds this threshold.
   */
  protected int threshold;

  /**
   * The load factor for the hashtable.
   */
  private float loadFactor;

  /**
   * The externally provided filter for rehash avoidance
   */
  private RehashFilter filter;

  /** The serialVersionUID */
  private static final long serialVersionUID = 1421746759512286392L;

  /**
   * Constructs a new, empty hashtable with the specified initial
   * capacity and the specified load factor.
   *
   * @param      initialCapacity   the initial capacity of the hashtable.
   * @param      loadFactor        a number between 0.0 and 1.0.
   * @exception  IllegalArgumentException  if the initial capacity is less
   *               than or equal to zero, or if the load factor is less than
   *               or equal to zero.
   * @since      JDK1.0
   */
  public MatchCache(int initialCapacity, float loadFactor) {
    if (tc.isEntryEnabled())
      tc.entry(
        cclass,
        "MatchCache",
        new Object[] { new Integer(initialCapacity), new Float(loadFactor)});
        
    if ((initialCapacity <= 0) || (loadFactor <= 0.0)) {
      throw new IllegalArgumentException();
    }
    this.loadFactor = loadFactor;
    table = new FastHashtableEntry[initialCapacity];
    threshold = (int)(initialCapacity * loadFactor);
    
    if (tc.isEntryEnabled())
      tc.exit(cclass, "MatchCache", this);
  }

  /**
   * Constructs a new, empty hashtable with the specified initial capacity
   * and default load factor.
   *
   * @param   initialCapacity   the initial capacity of the hashtable.
   * @since   JDK1.0
   */
  public MatchCache(int initialCapacity) {
    this(initialCapacity, 0.75f);
  }

  /**
   * Constructs a new, empty hashtable with a default capacity and load
   * factor.
   *
   * @since   JDK1.0
   */
  public MatchCache() {
    this(101, 0.75f);
  }

  /**
   * Returns the number of keys in this hashtable.
   *
   * @return  the number of keys in this hashtable.
   * @since   JDK1.0
   */
  public int size() {
    return count;
  }

  /**
   * Tests if this hashtable maps no keys to values.
   *
   * @return  <code>true</code> if this hashtable maps no keys to values;
   *          <code>false</code> otherwise.
   * @since   JDK1.0
   */
  public boolean isEmpty() {
    return count == 0;
  }

  /**
   * Returns an enumeration of the keys in this hashtable.
   *
   * @return  an enumeration of the keys in this hashtable.
   * @see     java.util.Enumeration
   * @see     java.util.Hashtable#elements()
   * @since   JDK1.0
   */
  public Enumeration keys() {
    return new FastHashtableEnumerator(table, true);
  }

  /**
   * Returns an enumeration of the values in this hashtable.
   * Use the Enumeration methods on the returned object to fetch the elements
   * sequentially.
   *
   * @return  an enumeration of the values in this hashtable.
   * @see     java.util.Enumeration
   * @see     java.util.Hashtable#keys()
   * @since   JDK1.0
   */
  public Enumeration elements() {
    return new FastHashtableEnumerator(table, false);
  }

  /**
   * Tests if some key maps into the specified value in this hashtable.
   * This operation is more expensive than the <code>containsKey</code>
   * method.
   *
   * @param      value   a value to search for.
   * @return     <code>true</code> if some key maps to the
   *             <code>value</code> argument in this hashtable;
   *             <code>false</code> otherwise.
   * @exception  NullPointerException  if the value is <code>null</code>.
   * @see        java.util.Hashtable#containsKey(java.lang.Object)
   * @since      JDK1.0
   */
  public boolean contains(Object value) {
    if (value == null) {
      throw new NullPointerException();
    }

    FastHashtableEntry tab[] = table;
    for (int i = tab.length ; i-- > 0 ;) {
      for (FastHashtableEntry e = tab[i] ; e != null ; e = e.next) {
        if (e.value.equals(value)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Tests if the specified object is a key in this hashtable.
   *
   * @param   key   possible key.
   * @return  <code>true</code> if the specified object is a key in this
   *          hashtable; <code>false</code> otherwise.
   * @see     java.util.Hashtable#contains(java.lang.Object)
   * @since   JDK1.0
   */
  public boolean containsKey(Object key) {
    FastHashtableEntry tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (FastHashtableEntry e = tab[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the value to which the specified key is mapped in this hashtable.
   *
   * @param   key   a key in the hashtable.
   * @return  the value to which the key is mapped in this hashtable;
   *          <code>null</code> if the key is not mapped to any value in
   *          this hashtable.
   * @see     java.util.Hashtable#put(java.lang.Object, java.lang.Object)
   * @since   JDK1.0
   */
  public Object get(Object key) {
    FastHashtableEntry tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (FastHashtableEntry e = tab[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        return e.value;
      }
    }
    return null;
  }

  /**
   * Returns the value to which the specified key is mapped in this hashtable.
   *
   * @param   key   a key in the hashtable.
   * @param   hash the hashCode of key, already computed (and trusted by
   *          the Hashtable implementation)
   * @return  the value to which the key is mapped in this hashtable;
   *          <code>null</code> if the key is not mapped to any value in
   *          this hashtable.
   * @since   JDK1.0
   */
  public Object get(Object key, int hash) {
    FastHashtableEntry tab[] = table;
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (FastHashtableEntry e = tab[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        return e.value;
      }
    }
    return null;
  }

  /**
   * Set external rehash filter.  This is an optional upcall that attempts to avoid
   * rehashing by shedding non-essential entries from the table.
   **/
  public void setRehashFilter(RehashFilter filter) {
    this.filter = filter;
  }

  /**
   * Avoids the need to rehash when some entries can be "filtered" out of the Hashtable by
   * an upcall to a separate decision routine.
   *
   * @return true if filtering was able to reduce the size of the Hashtable sufficiently,
   * false if rehashing will be needed after all.
   **/
  private boolean ableToFilter() {
    if (filter == null)
      return false;
    for (int i = table.length; i-- > 0 ;) {
      FastHashtableEntry newContents = null, next = null;
      for (FastHashtableEntry e = table[i] ; e != null ; e = next) {
        next = e.next;
        if (filter.shouldRetain(e.key, e.value)) {
          e.next = newContents;
          newContents = e;
        }
        else
          count--;
      }
      table[i] = newContents;
    }
    return count < threshold;
  }


  /**
   * Rehashes the contents of the hashtable into a hashtable with a
   * larger capacity. This method is called automatically when the
   * number of keys in the hashtable exceeds this hashtable's capacity
   * and load factor.
   *
   * @since JDK1.0 */
  protected void rehash() {
    if (ableToFilter())
      return;

    int oldCapacity = table.length;
    FastHashtableEntry oldTable[] = table;

    int newCapacity = oldCapacity * 2 + 1;
    FastHashtableEntry newTable[] = new FastHashtableEntry[newCapacity];

    threshold = (int)(newCapacity * loadFactor);

    for (int i = oldCapacity ; i-- > 0 ;) {
      for (FastHashtableEntry old = oldTable[i] ; old != null ; ) {
        FastHashtableEntry e = old;
        old = old.next;

        int index = (e.hash & 0x7FFFFFFF) % newCapacity;
        e.next = newTable[index];
        newTable[index] = e;
      }
    }
    table = newTable;
  }

  /**
   * Maps the specified <code>key</code> to the specified
   * <code>value</code> in this hashtable. Neither the key nor the
   * value can be <code>null</code>.
   * <p>
   * The value can be retrieved by calling the <code>get</code> method
   * with a key that is equal to the original key.
   *
   * @param      key     the hashtable key.
   * @param      value   the value.
   * @return     the previous value of the specified key in this hashtable,
   *             or <code>null</code> if it did not have one.
   * @exception  NullPointerException  if the key or value is
   *               <code>null</code>.
   * @see     java.lang.Object#equals(java.lang.Object)
   * @see     java.util.Hashtable#get(java.lang.Object)
   * @since   JDK1.0
   */
  public Object put(Object key, Object value) {
    // Make sure the value is not null
    if (value == null) {
      throw new NullPointerException();
    }

    // Makes sure the key is not already in the hashtable.
    FastHashtableEntry tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (FastHashtableEntry e = tab[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        Object old = e.value;
        e.value = value;
        return old;
      }
    }

    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();
      return put(key, value);
    }

    // Creates the new entry.
    FastHashtableEntry e = new FastHashtableEntry();
    e.hash = hash;
    e.key = key;
    e.value = value;
    e.next = tab[index];
    tab[index] = e;
    count++;
    return null;
  }

  /**
   * Maps the specified <code>key</code> to the specified
   * <code>value</code> in this hashtable. Neither the key nor the
   * value can be <code>null</code>.
   * <p>
   * The value can be retrieved by calling the <code>get</code> method
   * with a key that is equal to the original key.
   *
   * @param      key     the hashtable key.
   * @param      hash the hashCode of key, already computed (and trusted by
   *             the Hashtable implementation)
   * @param      value   the value.
   * @return     the previous value of the specified key in this hashtable,
   *             or <code>null</code> if it did not have one.
   * @exception  NullPointerException  if the key or value is
   *               <code>null</code>.
   * @see     java.lang.Object#equals(java.lang.Object)
   * @see     java.util.Hashtable#get(java.lang.Object)
   * @since   JDK1.0
   */
  public Object put(Object key, int hash, Object value) {
    // Make sure the value is not null
    if (value == null) {
      throw new NullPointerException();
    }

    // Makes sure the key is not already in the hashtable.
    FastHashtableEntry tab[] = table;
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (FastHashtableEntry e = tab[index] ; e != null ; e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        Object old = e.value;
        e.value = value;
        return old;
      }
    }

    if (count >= threshold) {
      // Rehash the table if the threshold is exceeded
      rehash();
      return put(key, value);
    }

    // Creates the new entry.
    FastHashtableEntry e = new FastHashtableEntry();
    e.hash = hash;
    e.key = key;
    e.value = value;
    e.next = tab[index];
    tab[index] = e;
    count++;
    return null;
  }

  /**
   * Removes the key (and its corresponding value) from this
   * hashtable. This method does nothing if the key is not in the hashtable.
   *
   * @param   key   the key that needs to be removed.
   * @return  the value to which the key had been mapped in this hashtable,
   *          or <code>null</code> if the key did not have a mapping.
   * @since   JDK1.0
   */
  public Object remove(Object key) {
    FastHashtableEntry tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (FastHashtableEntry e = tab[index], prev = null ; e != null ; prev = e, e = e.next) {
      if ((e.hash == hash) && e.key.equals(key)) {
        if (prev != null) {
          prev.next = e.next;
        } else {
          tab[index] = e.next;
        }
        count--;
        return e.value;
      }
    }
    return null;
  }

  /**
   * Clears this hashtable so that it contains no keys.
   *
   * @since   JDK1.0
   */
  public void clear() {
    FastHashtableEntry tab[] = table;
    for (int index = tab.length; --index >= 0; )
      tab[index] = null;
    count = 0;
  }

  /**
   * Creates a shallow copy of this hashtable. The keys and values
   * themselves are not cloned.
   * This is a relatively expensive operation.
   *
   * @return  a clone of the hashtable.
   * @since   JDK1.0
   */
  public Object clone() {
    try {
      MatchCache t = (MatchCache)super.clone();
      t.table = new FastHashtableEntry[table.length];
      for (int i = table.length ; i-- > 0 ; ) {
        t.table[i] = (table[i] != null)
          ? (FastHashtableEntry)table[i].clone() : null;
      }
      return t;
    } catch (CloneNotSupportedException e) 
    {
      // No FFDC Code Needed.
      // FFDC driven by wrapper class.
      FFDC.processException(this,
          cclass,
          "com.ibm.ws.sib.matchspace.impl.MatchCache.clone",
          e,
          "1:589:1.16");
      // this shouldn't happen, since we are Cloneable
      throw new IllegalStateException();
    }
  }

  /**
   * Returns a rather long string representation of this hashtable.
   *
   * @return  a string representation of this hashtable.
   * @since   JDK1.0
   */
  public String toString() {    
    int max = size() - 1;
    StringBuffer buf = new StringBuffer();
    
    buf.append("Cache:" + hashCode());
    Enumeration k = keys();
    Enumeration e = elements();
    buf.append("{");

    for (int i = 0; i <= max; i++) {
      String s1 = k.nextElement().toString();
      String s2 = e.nextElement().toString();
      buf.append(s1 + "=" + s2);
      if (i < max) {
        buf.append(", ");
      }
    }
    buf.append("}");
    return buf.toString();
  }

  /**
   * WriteObject is called to save the state of the hashtable to a stream.
   * Only the keys and values are serialized since the hash values may be
   * different when the contents are restored.
   * iterate over the contents and write out the keys and values.
   */
  private void writeObject(java.io.ObjectOutputStream s)
    throws IOException
  {
    // Write out the length, threshold, loadfactor
    s.defaultWriteObject();

        // Write out length, count of elements and then the key/value objects
    s.writeInt(table.length);
    s.writeInt(count);
    for (int index = table.length-1; index >= 0; index--) {
      FastHashtableEntry entry = table[index];

      while (entry != null) {
        s.writeObject(entry.key);
        s.writeObject(entry.value);
        entry = entry.next;
      }
    }
  }

  /**
   * readObject is called to restore the state of the hashtable from
   * a stream.  Only the keys and values are serialized since the
   * hash values may be different when the contents are restored.
   * Read count elements and insert into the hashtable.
   */
  private void readObject(java.io.ObjectInputStream s)
    throws IOException, ClassNotFoundException
  {
    // Read in the length, threshold, and loadfactor
    s.defaultReadObject();

        // Read the original length of the array and number of elements
    int origlength = s.readInt();
    int elements = s.readInt();

        // Compute new size with a bit of room 5% to grow but
        // No larger than the original size.  Make the length
        // odd if it's large enough, this helps distribute the entries.
        // Guard against the length ending up zero, that's not valid.
    int length = (int)(elements * loadFactor) + (elements / 20) + 3;
    if (length > elements && (length & 1) == 0)
      length--;
    if (origlength > 0 && length > origlength)
      length = origlength;

    table = new FastHashtableEntry[length];
    count = 0;

        // Read the number of elements and then all the key/value objects
    for (; elements > 0; elements--) {
      Object key = s.readObject();
      Object value = s.readObject();
      put(key, value);
    }
  }

  // Interface to be implemented by external rehash filters
  static interface RehashFilter {
    boolean shouldRetain(Object key, Object value);
  }
}

/**
 * A hashtable enumerator class.  This class should remain opaque
 * to the client. It will use the Enumeration interface.
 */
class FastHashtableEnumerator
  implements Enumeration {
  boolean keys;
  int index;
  FastHashtableEntry table[];
  FastHashtableEntry entry;

  FastHashtableEnumerator(FastHashtableEntry table[], boolean keys) {
    this.table = table;
    this.keys = keys;
    this.index = table.length;
  }

  public boolean hasMoreElements() {
    if (entry != null) {
      return true;
    }
    while (index-- > 0) {
      if ((entry = table[index]) != null) {
        return true;
      }
    }
    return false;
  }

  public Object nextElement() {
    if (entry == null) {
      while ((index-- > 0) && ((entry = table[index]) == null));
    }
    if (entry != null) {
      FastHashtableEntry e = entry;
      entry = e.next;
      return keys ? e.key : e.value;
    }
    throw new NoSuchElementException();
  }
}

