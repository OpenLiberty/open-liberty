/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This is a simple struct object that is an element in the
 * NonSyncHashtable collision list.
 */
class NonSyncHashtableEntry implements Serializable {
   private static final long serialVersionUID = 1342185474L;
   int hash;
   Object key;
   Object value;
   NonSyncHashtableEntry next;
}

/**
 * This class implements a hashtable that is the same as the 
 * java.util.Hashtable except for the following differences:
 * <ul>
 *     <li>Its methods are not synchronized.
 *         It should be used whenever synchronization is done 
 *         at a larger granularity by the client.  
 *         In this case, it removes the overhead of an unnecessary 
 *         level of synchronization.
 *     <li>This class never rehashes. 
 *         It should be used when the size of the hash table 
 *         is nearly constant, so it removes the overhead of 
 *         checking for the need for rehash.  
 *     <li>It is not Serializable or Cloneable. 
 * </ul>
 */
public class NonSyncHashtable implements Serializable {
    private static final long serialVersionUID = 1342185474L;

   /**
    * The hash table data.
    */
   protected NonSyncHashtableEntry[] table;

   /**
    * The total number of entries in the hash table.
    */
   protected int count;

   /**
    * Constructs a new, empty hashtable with the specified capacity. 
    * @param capacity the initial capacity of the hashtable.
    */
   public NonSyncHashtable(int capacity) {
      table = new NonSyncHashtableEntry[capacity];
   }

   /**
    * Returns the number of keys in this hashtable.
    * @return The number of keys in this hashtable.
    */
   @Trivial
   public int size() {
      return count;
   }

   /**
    * Tests if this hashtable maps no keys to values.
    *
    * @return  <code>true</code> if this hashtable maps no keys to values;
    *          <code>false</code> otherwise.
    */
   public boolean isEmpty() {
      return count == 0;
   }

   /**
    * Returns an enumeration of the keys in this hashtable.
    * @return  An enumeration of the keys in this hashtable.
    */
   public Enumeration keys() {
      return new NonSyncHashtableEnumerator(table, true);
   }

   /**
    * Returns an enumeration of the values in this hashtable.
    * Use the Enumeration methods on the returned object to fetch the elements
    * sequentially.
    * @return  An enumeration of the values in this hashtable.
    * @see     java.util.Enumeration
    */
   public Enumeration elements() {
      return new NonSyncHashtableEnumerator(table, false);
   }

   /**
    * Tests if some key maps into the specified value in this hashtable.
    * This operation is more expensive than the <code>containsKey</code>
    * method.
    * @param      value   a value to search for.
    * @return     <code>true</code> if some key maps to the
    *             <code>value</code> argument in this hashtable;
    *             <code>false</code> otherwise.
    * @exception  NullPointerException  if the value is <code>null</code>.
    */
   public boolean contains(Object value) {
      if (value == null) {
         throw new NullPointerException();
      }
      NonSyncHashtableEntry[] tab = table;
      for (int i = tab.length; i-- > 0;) {
         for (NonSyncHashtableEntry e = tab[i]; e != null; e = e.next) {
            if (e.value.equals(value)) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Tests if the specified object is a key in this hashtable.
    * @param   key   possible key.
    * @return  <code>true</code> if the specified object is a key in this
    *          hashtable; <code>false</code> otherwise.
    */
   public boolean containsKey(Object key) {
      NonSyncHashtableEntry tab[] = table;
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      for (NonSyncHashtableEntry e = tab[index]; e != null; e = e.next) {
         if ((e.hash == hash) && e.key.equals(key)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns the value to which the specified key is mapped in this hashtable.
    * @param   key a key in the hashtable.
    * @return  The value to which the key is mapped in this hashtable;
    *          <code>null</code> if the key is not mapped to any value in
    *          this hashtable.
    */
   public Object get(Object key) {
      NonSyncHashtableEntry tab[] = table;
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      for (NonSyncHashtableEntry e = tab[index]; e != null; e = e.next) {
         if ((e.hash == hash) && e.key.equals(key)) {
            return e.value;
         }
      }
      return null;
   }

   /**
    * Maps the specified <code>key</code> to the specified 
    * <code>value</code> in this hashtable. Neither the key nor the 
    * value can be <code>null</code>. 
    * <p>
    * The value can be retrieved by calling the <code>get</code> method 
    * with a key that is equal to the original key. 
    * @param      key     the hashtable key.
    * @param      value   the value.
    * @return     The previous value of the specified key in this hashtable,
    *             or <code>null</code> if it did not have one.
    * @exception  NullPointerException  if the key or value is
    *               <code>null</code>.
    */
   public Object put(Object key, Object value) {
      if (value == null) {
         throw new NullPointerException();
      }

      // Makes sure the key is not already in the hashtable.
      NonSyncHashtableEntry tab[] = table;
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      for (NonSyncHashtableEntry e = tab[index]; e != null; e = e.next) {
         if ((e.hash == hash) && e.key.equals(key)) {
            Object old = e.value;
            e.value = value;
            return old;
         }
      }

      // Creates the new entry.
      NonSyncHashtableEntry e = new NonSyncHashtableEntry();
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
    * @param   key   the key that needs to be removed.
    * @return  The value to which the key had been mapped in this hashtable,
    *          or <code>null</code> if the key did not have a mapping.
    */
   public Object remove(Object key) {
      NonSyncHashtableEntry tab[] = table;
      int hash = key.hashCode();
      int index = (hash & 0x7FFFFFFF) % tab.length;
      for (NonSyncHashtableEntry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
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
    */
   public void clear() {
      NonSyncHashtableEntry tab[] = table;
      for (int index = tab.length; --index >= 0;) {
         tab[index] = null;
      }
      count = 0;
   }

   /**
    * Returns a rather long string representation of this hashtable.
    * @return A string representation of this hashtable.
    */
   public String toString() {
      int max = size() - 1;
      StringBuffer buf = new StringBuffer();
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
}

/**
 * A hashtable enumerator class.  This class should remain opaque 
 * to the client. It will use the Enumeration interface. 
 * Notice, that this is Serializable unlike its java.util cousins.
 */
class NonSyncHashtableEnumerator implements Enumeration, Serializable {
   private static final long serialVersionUID = 1342185474L;
   boolean keys = false;
   int index = 0;
   NonSyncHashtableEntry table[] = null;
   NonSyncHashtableEntry entry = null;

   NonSyncHashtableEnumerator(NonSyncHashtableEntry table[], boolean keys) {
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
         NonSyncHashtableEntry e = entry;
         entry = e.next;
         return keys ? e.key : e.value;
      }
      throw new NoSuchElementException("HashtableEnumerator");
   }
}