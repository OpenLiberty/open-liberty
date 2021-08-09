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

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.PasswordUtils;
import com.ibm.ws.sib.utils.PasswordSuppressingHashMap;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/*
 * This class provides a wrapper around a pair of JMF lists to provide a Map
 * that can be freely modified by the caller, but that can be easily converted
 * back to a pair of Lists again when the data needs to be written back to JMF.
 */

// This class extends AbstractMap and implements by delegation to a 'backingMap'.
// This backingMap is initially generated as a view on a pair of underlying JMFLists
// and will continue to be used as long as no changes need to be made to the map.
// If any such a changes are attempted we must first change the backingMap to a HashMap
// constructed from the original data.  The caller is then responsible to writing this
// new map (as a pair of lists) back to JMF when required.

// Note that whenever the backingMap is a ListMap instance, it is backed by two
// real Lists which were passed into the constructor. Hence the cast of keyList/valueList
// to List in JsMsgMap$ListMap$ListSet$ListEntry.getKey/Value is safe.
// When keyList and valueList are instances of KeyList/ValueList, the backingMap
// is a standard Java HashMap, rather than a ListMap. Hence the cast will never happen.
// Once the backingMap is a HashMap, the keyList & valueList are only used by the
// getKeyList/getValueList methods, to extract the content into lists.

// In summary, the ListMap set of inner classes are only used when the JsMsgMap
// has been constructed from 2 underlying lists, and has not bee subsequently updated.
// The MapList set of inner classes are only used if the JsMsgMap is cinstructed from nulls,
// or after it has been updated - in both cases the backingMap is now a HashMap.

// The only way to get the backingMap to be a ListMap but the lists to be Key/ValueLists,
// would be if multiple threads were operating on the same instance, and the backingMap
// value had not been written back into main memory by the updating thread. This is
// not a valid scenario, as a JsMsgMap is private to a JsMessageImpl instance, which
// is not allowed to be operated on by multiple user threads. (See the JMS spec).
// Hence there is no synchronization in this class as it does not need to be thread-safe.

public class JsMsgMap extends AbstractMap<String,Object> implements FFDCSelfIntrospectable {
  private static TraceComponent tc = SibTr.register(JsMsgMap.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
 
  JsMsgMap(List<String> keyList, List<Object> valueList) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{keyList, traceableValueList(keyList, valueList)});

    // we start out with a backing map based on the original pair if JMF lists
    if (keyList != null) {
      this.keyList = keyList;
      this.valueList = valueList;
      backingMap = new ListMap();
      fluffed = false;                                      // d317373.1
    }
    else {
      backingMap = new PasswordSuppressingHashMap<String,Object>();
      this.keyList = new KeyList();
      this.valueList = new ValueList();
      fluffed = true;                                       // d317373.1
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  private Collection keyList, valueList;
  private Map<String,Object> backingMap;
  private boolean changed = false;     // Is the map content different from JMF?
  private boolean fluffed;             // Is the map 'fluffed up' into a HashMap? d317373.1

  public boolean isChanged() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isChanged");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isChanged",  changed);
    return changed;
  }

  // Set the changed flag back to false if the map is written back to JMF
  public void setUnChanged() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setUnChanged");
    changed = false;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setUnChanged");
  }

  // Set the changed flag to true if the caller knows better than the map itself
  public void setChanged() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setChanged");
    changed = true;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setChanged");
  }

  public Collection getKeyList() {
    return keyList;
  }

  public Collection getValueList() {
    return valueList;
  }

  private Map<String,Object> copyMap() {
    if (!fluffed) {                                   // d317373.1
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "copyMap making copy => fluffed");
      backingMap = new HashMap<String,Object>(backingMap);
      keyList = new KeyList();
      valueList = new ValueList();
      fluffed = true;                                 // d317373.1
    }
    return backingMap;
  }

  /*
   * Methods needed to implement a modifiable AbstractMap.  We only need to
   * make a copy if the map data is to be changed.
   */
  public Set<Map.Entry<String,Object>> entrySet() {
    return backingMap.entrySet();
  }

  // The first update to a map causes us to have to write the whole map out again
  // during encode. Therefore don't do a 'first update' unless necessary.
  // Once we've made one update, others don't matter so save time by not checking.  d317373.1
  public Object put(String key, Object value) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "put", new Object[]{key, PasswordUtils.replaceValueIfKeyIsPassword(key,value)});

    // If the value is null (which is allowed for a Map Message) we can't tell
    // quickly whether the item is already in the map as null, or whether it doesn't exist,
    // so we just assume it will cause a change.
    // For properties we will never get a value of null, and it is properties we are really concerned with.
    if ((!changed)&& (value != null)) {
      Object old = get(key);
      if (value.equals(old)) {          // may as well call equals immediately, as it checks for == and we know value!=null
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "put",  "unchanged");
        return old;
      }
      else {
        changed = true;
        Object result = copyMap().put(key, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "put", PasswordUtils.replaceValueIfKeyIsPassword(key,result));
        return result;
      }
    }
    else {
      changed = true;
      Object result = copyMap().put(key, value);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "put", PasswordUtils.replaceValueIfKeyIsPassword(key,result));
      return result;
    }
  }

  public Object remove(String key) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "remove", key);
    changed = true;                                             // d317373.1
    Object result = copyMap().remove(key);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "remove", PasswordUtils.replaceValueIfKeyIsPassword(key,result));
    return result;
  }

  public void clear() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "clear");
    changed = true;                                             // d317373.1
    copyMap().clear();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "clear");
  }

  // we can use a cunning assortment of inner classes to provide a read-only Map-like
  // view of the data from two lists.

  private class ListMap extends AbstractMap<String,Object> {

    Set<Map.Entry<String,Object>> entries = new ListSet();

    public Set<Map.Entry<String,Object>> entrySet() {
      return entries;
    }

    private class ListSet extends AbstractSet<Map.Entry<String,Object>> {
      public int size() {
        return keyList.size();
      }

      public Iterator<Map.Entry<String,Object>> iterator() {
        return new ListIterator();
      }

      private class ListIterator implements Iterator<Map.Entry<String,Object>> {
        int index = 0;
        public boolean hasNext() {
          return index != keyList.size();
        }
        public Map.Entry<String,Object> next() {
          return new ListEntry(index++);
        }
        public void remove() {
          throw new UnsupportedOperationException();
        }
      }

      private class ListEntry implements Map.Entry<String,Object> {
        int index;
        public ListEntry(int index) {
          this.index = index;
        }
        public String getKey() {
          return (String)((List)keyList).get(index);
        }
        public Object getValue() {
          return ((List)valueList).get(index);
        }
        public Object setValue(Object value) {
          throw new UnsupportedOperationException();
        }
      }
    }
  }

  // We can use a cunning assortment of inner classes to provide the two List-like
  // views of the data in a map.  This ensures the two Lists are actually using
  // the Map's data (no copies need to be made) and ensures the two Lists contain
  // corresponding key and value pairs.

  private abstract class MapList<T> extends AbstractCollection<T> {
    Set<Map.Entry<String,Object>> entries = backingMap.entrySet();
    public int size() {
      return entries.size();
    }

    abstract class MapIterator<U> implements Iterator<U> {
      Iterator<Map.Entry<String,Object>> iter = entries.iterator();
      public boolean hasNext() {
        return iter.hasNext();
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }

    class KeyIterator extends MapIterator<String> {
      public String next() {
        return iter.next().getKey();
      }
    }

    class ValueIterator extends MapIterator<Object> implements FFDCSelfIntrospectable {
      public Object next() {
        return iter.next().getValue();
      }
      public String toString() {
        return "[/* valueIterator of size "+size()+"*/]"; /* Must suppress contents in case of passwords */
      }
      public String[] introspectSelf() {
        return new String[] { toString() };
      }
    }
  }

  private class KeyList extends MapList<String> {
    public Iterator<String> iterator() {
      return new KeyIterator();
    }
  }

  private class ValueList extends MapList<Object> implements FFDCSelfIntrospectable {
    public Iterator<Object> iterator() {
      return new ValueIterator();
    }
    public String toString() {
      return "[/* ValueList of size "+size()+"*/]"; /* Must suppress contents in case of passwords */
    }
    public String[] introspectSelf() {
      return new String[] { toString() };
    }
  }

  private class BackingMap extends HashMap<String,Object> implements FFDCSelfIntrospectable {
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      boolean first = true;
      for(Map.Entry<String,Object> e : this.entrySet()) {
        if (!first) {
          sb.append(", ");
          first = false;
        }
        sb.append(e.getKey()+"="+PasswordUtils.replaceValueIfKeyIsPassword(e.getKey(),e.getValue()));
      }
      sb.append("}");
      return sb.toString();
    }
    public String[] introspectSelf() {
      return new String[] { toString() };
    }
  }

  public String[] introspectSelf() {
    return new String[] { "keyList = "+keyList,
                          "valueList = "+traceableValueList(keyList,valueList),
                          "backingMap = "+backingMap,
                          "changed = "+changed,
                          "fluffed = "+fluffed };
  }

  private List<Object> traceableValueList(Collection keyList, Collection valueList) {
    List<Object> traceableValueList = null;

    if (keyList != null) {
      /* We need to sanitise the values */
      traceableValueList = new ArrayList<Object>(valueList.size());

      Iterator kit = keyList.iterator();
      Iterator vit = valueList.iterator();

      while(kit.hasNext()) {
        Object k = kit.next();
        Object v = vit.next();

        String ks;
        if (k instanceof String)
          ks = (String)k;
        else
          ks = "";

        traceableValueList.add(PasswordUtils.replaceValueIfKeyIsPassword(ks,v));
      }
    }

    return traceableValueList;
  }
}
