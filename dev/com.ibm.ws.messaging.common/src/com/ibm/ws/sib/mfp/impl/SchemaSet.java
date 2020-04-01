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
package com.ibm.ws.sib.mfp.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.ibm.ws.sib.mfp.ConnectionSchemaSet;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.mfp.util.HexUtil;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.Traceable;
import com.ibm.websphere.ras.TraceComponent;

/*
 * This class provides a semi-synchronized hash Set which holds the Ids of the
 * Schemas known by the other end of a Connection.
 *
 * The class only implements the add(), contains() and size() methods of the Set interface
 * as these are the only ones required.
 *   add() is synchronized.
 *   contains() is written so as to be safe not to be synchronized
 *      - if an add() is in progress while a contains() is operating, the worst
 *        that can happen is that the item being added isn't seen by contains.
 *        (A 'safe' reference is taken to the table, so that a resize doesn't
 *        cause the table to become a different array under us.)
 *      - is an Entry is added to the table, it is guaranteed to be fully
 *        initialized, and its fields will already be written back to memory
 *        as they are final & immutable. Hence then Entry.contains() method can not NPE.
 *   size() is also written so as to be safe not to be synchronized, using the same
 *        mechanism as contains(). size() is only used by unit tests, so is less important.
 *
 * This class only officially supports SchemaIds. However, as a SchemaId is a
 * Long, it will cater for any Long in reality..
 */
public final class SchemaSet implements ConnectionSchemaSet, Set, Traceable {

  private static TraceComponent tc = SibTr.register(SchemaSet.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
 
  private final static int DEFAULT_TABLE_SIZE = 28; // Best spread at the moment
  private final static int MAX_ENTRY_DEPTH    = 3;  // Max depth for a linked list of Entrys

  private Entry[] table;

  public SchemaSet() {
    this(DEFAULT_TABLE_SIZE);
  }

  public SchemaSet(int size) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "SchemaSet", size);
    table = new Entry[size];
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "SchemaSet");
  }


  /**
   * Returns <tt>true</tt> if this set contains the specified element.  More
   * formally, only returns <tt>true</tt> if this set contains an
   * element <code>e</code> such that <code>(o==null ? e==null :
   * o.equals(e))</code>.
   *
   * It is possible for a false negative to be returned, if the element is added
   * after the safeTable reference has been set, or if memory hasn't yet been
   * written back. However, this is benign & will merely cause a schema to be sent
   * unnecessarily. When the add method is called to add that element the existing
   * entry will be found, so we won't end up with a duplicate.
   * Synchronizing to avoid the possibility of false negatives would be
   * unnecessarily expensive.
   *
   * @param o element whose presence in this set is to be tested.
   * @return <tt>true</tt> if this set contains the specified element.
   * @throws ClassCastException if the type of the specified element
   *           is incompatible with this set (optional).
   * @throws NullPointerException if the specified element is null and this
   *         set does not support null elements (optional).
   */
  public boolean contains(Object o) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "contains", debugId(o));

    boolean result = false;

    /* It should always be an Id so just cast it. If someone is using the     */
    /* class for the wrong purpose they will get a ClassCastException, which  */
    /* is permissable.                                                        */
    Long id = (Long)o;

    /* Because the table could be 'resized' (and therefore replaced) during   */
    /* the method we get a local ref to the current one & use it throughout.  */
    Entry[] safeTable = table;

    /* NPE is also permissable, if someone is using the class incorrectly.    */
    int i = hashToTable(id, safeTable);

    /* Search the appropriate Entry list from the table.                      */
    if (safeTable[i] != null) {
      Entry current = safeTable[i];
      if (current.contains(id, 0) == 0) {
        result = true;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "contains",  result);
    return result;

  }


  /**
   * Adds the specified element to this set if it is not already present
   * (optional operation).  More formally, adds the specified element,
   * <code>o</code>, to this set if this set contains no element
   * <code>e</code> such that <code>(o==null ? e==null :
   * o.equals(e))</code>.  If this set already contains the specified
   * element, the call leaves this set unchanged and returns <tt>false</tt>.
   * In combination with the restriction on constructors, this ensures that
   * sets never contain duplicate elements.<p>
   *
   * Note: This method is synchronized, so two elements can not be added
   * simultaneously.
   *
   * @param o element to be added to this set.
   * @return <tt>true</tt> if this set did not already contain the specified
   *         element.
   *
   * @throws ClassCastException if the class of the specified element
   *           prevents it from being added to this set.
   * @throws NullPointerException if the specified element is null and this
   *         set does not support null elements.
   */
  public synchronized boolean add(Object o) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "add", debugId(o));

    boolean result = false;

    /* It should always be an Id so just cast it. If someone is using the     */
    /* class for the wrong purpose they will get a ClassCastException, which  */
    /* is permissable.                                                        */
    Long id = (Long)o;

    /* NPE is also permissable, if someone is using the class incorrectly.    */
    int i = hashToTable(id, table);

    /* If there is no Entry in the table slot, just add it.                   */
    if (table[i] == null) {
      table[i] = new Entry(id);
      result = true;
    }

    /* Otherwise, we have to search down the Entry list for the Schema Id.    */
    else {
      Entry current = table[i];
      int depth = current.contains(id, 0);

      /* If depth > 0, we didn't find the Schema so we must add it            */
      if (depth > 0) {

        /* If the depth searched was less than the MAX, just make a new Entry */
        /* at  of the list, and put it in the table.                          */
        if (depth < MAX_ENTRY_DEPTH) {
          Entry newEntry = new Entry(id, current);
          table[i] = newEntry;
          result = true;
        }

        /* Otherwise, the depth is too big so we must resize first.           */
        else {
          resize();

          /* Now we have to get the new hash value & look in the new table    */
          i = hashToTable(id, table);
          current = table[i];

          /* Make the new Entry and add it                                    */
          Entry newEntry = new Entry(id, current);
          table[i] = newEntry;
          result = true;
        }

      }

      /* If depth = 0, the SchemaId is already in the SchemaSet so do nothing */
      else {}
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "add",  result);
    return result;

  }


  /**
   * Returns the number of elements in this set (its cardinality).  If this
   * set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
   * <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of elements in this set (its cardinality).
   */
  public int size() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "size");

    /* Because the table could be 'resized' (and therefore replaced) during   */
    /* the method we get a local ref to the current one & use it throughout.  */
    Entry[] safeTable = table;

    long count = 0;
    Entry current;

    /* Go through the linked lists in the table, counting non-null entries    */
    for (int i = 0; i < safeTable.length; i++) {
      current = safeTable[i];
      while (current != null) {
        count++;
        current = current.next;
      }
    }

    if (count > Integer.MAX_VALUE) {
      count = Integer.MAX_VALUE;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "size",  (int)count);
    return (int)count;
  }


  /*
   * The following methods are not implemented -
   *   they all throw UnsupportedOperationException.
   */
  public boolean isEmpty() {
    throw new UnsupportedOperationException();
  }

  public Iterator iterator() {
    throw new UnsupportedOperationException();
  }

  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  public Object[] toArray(Object a[]) {
    throw new UnsupportedOperationException();
  }

  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  public boolean containsAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean addAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean removeAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public boolean equals(Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * The Long Id is already effectively a hashcode for the Schema and should
   * be unique. All we should need to do is get a positive integer version of it
   * & divide by the table size.
   */
  private int hashToTable(Long id, Entry[] table) {
    int posVal = id.intValue() & Integer.MAX_VALUE;
    return (posVal % table.length);
  }


  /**
   * Resize the SchemaSet, adding 50% to the size.
   *
   * This method must only be called from add() which is synchronized.
   */
  private void resize() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "resize", table.length);

    Entry[] newTable = new Entry[table.length + (table.length / 2)];

    /* We have to walk the entire SchemaSet, rehashing each Entry and putting */
    /* it in its new home. As contains() may be running at the same time, we  */
    /* have to create a new Entry instance for each.                          */
    for (int i = 0; i < table.length; i++) {
      Entry ent = table[i];
      Entry newEntry;
      int j;
      while (ent != null) {
        j = hashToTable(ent.schemaId, newTable);
        newEntry = new Entry(ent.schemaId, newTable[j]);
        newTable[j] = newEntry;
        ent = ent.next;
      }
    }

    /* Now replace the old table with the new one. */
    table = newTable;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "resize", table.length);
  }


  /**
   * Returns a printable view of the SchemaSet and contents for use in
   * debugging and Unit Tests.
   *
   * @return String Printable view of the SchemaSet and contents
   */
  public String toVerboseString() {

    /* Take a local reference to the table in case a resize happens. */
    Entry[] tempTable = table;

    StringBuffer buf = new StringBuffer();

    /* Include the SchemaSet hashcode just in case we need to distinguish them */
    buf.append("SchemaSet ");
    buf.append(this.hashCode());
    buf.append(" {\n");

    /* For each table entry, write out all the SchemaIds */
    for (int i=0; i < tempTable.length;  i++) {
      buf.append("  [");
      buf.append(i);
      buf.append("] ");
      Entry ent = tempTable[i];
      buf.append(ent);
      while (ent != null) {
        ent = ent.next;
        buf.append(" ");
        buf.append(ent);
      }
      buf.append("\n");
    }

    buf.append(" }\n");

    return buf.toString();
  }

  @Override
  public String toTraceString() {
      return toString();
  }

  /**
   * Write a Schema Id out as a hex string.
   *
   * @param o      The SchemaId
   *
   * @return String A hex string representaion of the SchemaId's long value.
   */
  private static final String debugId(Object o) {
    Long id = (Long)o;
    byte[] buf = new byte[8];
    ArrayUtil.writeLong(buf, 0, id.longValue());
    return HexUtil.toString(buf);
  }

  /****************************************************************************/
  /* Inner classes                                                            */
  /****************************************************************************/

  /**
   * Entry is a private class representing a schemaId entry in the Set.
   * Each Entry contains the SchemaId, and a pointer to the next Entry
   * hashed to the same table entry.
   *
   * The two instance variables must be final so that Entry is immutable. This
   * is essential as an Entry must be completely initialized before being added
   * to the table. This allows the SchemaSet contains() and size() methods to be
   * thread-safe even though they are not synchronized.
   */
  private static final class Entry {

    private final Long schemaId;
    private final Entry next;

    Entry(Long id) {
      schemaId = id;
      next = null;
    }

    Entry(Long id, Entry nextEntry) {
      schemaId = id;
      next = nextEntry;
    }

    /**
     * Search down the linked list of Entry, starting with the current
     * instance, to determine whether a SchemaId is included.
     *
     * @param id          The Schema Id to be searched for
     * @param depth       The depth of linked entries checked so far
     *
     * @return int  If the Id is found, 0 is returned, otherwise the number
     *              of linked Entrys searched is returned.
     */
    private int contains(Long id, int depth) {
      depth++;
      if (schemaId.equals(id)) {
        return 0;
      }
      else if (next != null) {
        return next.contains(id, depth);
      }
      else return depth;
    }

    /**
     * Return the String value of the Entry's schemaId.
     *
     * @return String The Entry's schemaId
     */
    public String toString() {
      return debugId(schemaId);
    }
  }

}
