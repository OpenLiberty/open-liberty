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
package com.ibm.ws.sib.processor.utils.index;

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.utils.linkedlist.Cursor;
import com.ibm.ws.sib.processor.utils.linkedlist.LinkedList;
import com.ibm.ws.sib.processor.utils.linkedlist.SimpleLinkedListEntry;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * An index class
 */ 
public class Index
{
  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  
    
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      Index.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
 
  
  public static class Type implements Cloneable
  {
    public Object clone()
    {
      Object clone = null;
      try
      {
        clone = super.clone();
      }
      catch (CloneNotSupportedException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.utils.index.Type.clone",
          "1:80:1.17",
          this);
          
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.utils.index.Index",
            "1:86:1.17" });
          
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.utils.LockManager",
              "1:93:1.17",
              e },
            null),
            e);
      }
      return clone;
    }
  }
  
  public static class Entry extends SimpleLinkedListEntry
  {
    public Object key;
    public Type type;
    
    protected Entry(Object key, Object data, Type type)
    {
      super(data);
      this.key = key;
      this.type = type;
    }
  }
 
  //The full list of destinations
  protected  LinkedList list;
  //a full map of uuids to destinations
  protected HashMap index;
 
  public Index()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "Index");

    list = new LinkedList();
    index = new HashMap(10);
      
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "Index", this);       
  }
  
  public synchronized Entry put(Object key, Object data)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "put", new Object[] { data });

    Entry entry = put(key, data, new Type());
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "put", entry);
      
    return entry;      
  }
  
  protected synchronized Entry put(Object key, Object data, Type type)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "put", new Object[] { data, type });

    Entry entry = new Entry(key, data, type);
    add(entry);
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "put", entry);
    
    return entry;      
  }
  
  public synchronized Entry add(Entry entry)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "add", new Object[] { entry });

    Entry old = (Entry) index.get(entry.key);
    if(old != null)
    {
      if(tc.isDebugEnabled())
        SibTr.debug(tc, "removing old entry from index");
      
      list.remove(old);
    }
    list.insertAtBottom(entry);
    index.put(entry.key, entry);
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "add", entry);
    
    return entry;      
  }
  
  public synchronized Object remove(Object key)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { key });

    Object data = null;
    Entry entry = (Entry) index.remove(key);
    if(entry != null)
    {
      list.remove(entry);
      data = entry.data;
    }    
    else
    {
      if(tc.isDebugEnabled())
        SibTr.debug(tc, "null entry - cannot remove from list");
    }
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "remove", data);
    
    return data;
  }
  
  public synchronized boolean containsKey(Object key)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "containsKey", new Object[] { key });

    boolean res = index.containsKey(key);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "containsKey", new Boolean(res));
    return res;    
  }
  
  public synchronized Object get(Object key)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "get", new Object[] { key });
    
    Object data = get(key, null);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "get", data);

    return data;
  }
  
  public synchronized Object get(Object key, IndexFilter filter)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "get", new Object[] { key, filter });
  
    Object data = null;
  
    Entry entry = (Entry) index.get(key);
    if(entry != null && (filter == null || filter.matches(entry.type)))
    {
      data = entry.data;
    }
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "get", data);

    return data;
  }  
    
  public synchronized Type getType(Object key)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getType", new Object[] { key });

    Entry entry =
      (Entry) index.get(key);
    Type type = null;
    if(entry != null) type = entry.type;  
          
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getType", type);
  
    return type;
  }
  
  public synchronized void setType(Object key, Type type)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setType", new Object[] { key, type });

    Entry entry =
      (Entry) index.get(key);
    entry.type = (Type) type.clone();  
      
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setType");
  }
      
  public synchronized Cursor newCursor()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "newCursor");
      
    Cursor cursor = list.newCursor("Index Cursor");
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "newCursor", cursor);
      
    return cursor;
  }

  public synchronized SIMPIterator iterator()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "iterator");
    
    SIMPIterator itr = new FilteredIndexIterator(this, null);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "iterator", itr);
    
    return itr;
  }
  
  public synchronized SIMPIterator iterator(IndexFilter filter)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "iterator", filter);
  
    SIMPIterator itr = new FilteredIndexIterator(this, filter);
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "iterator", itr);
  
    return itr;
  }
    
  protected synchronized void remove(Entry entry)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { entry });

    list.remove(entry);
    index.remove(entry.key);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }
  
  public synchronized int size()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "size");
 
    int size = index.size();
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "size", new Integer(size));
      
    return size;
  }
  
  public synchronized boolean isEmpty()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "isEmpty");
 
    boolean isEmpty = index.isEmpty();
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "isEmpty", new Boolean(isEmpty));
    
    return isEmpty;
  }
  
  public String toString()
  {
    StringBuffer buffer = new StringBuffer("Index\n");
    synchronized(this)
    {
      Iterator itr = index.keySet().iterator();
      while(itr.hasNext())
      {
        Object key = itr.next();
        buffer.append(key);
        buffer.append(" --> ");
        buffer.append(index.get(key));
        buffer.append("\n");
      }
      buffer.append("------------\n");
      buffer.append("Full List\n");
      Entry entry = (com.ibm.ws.sib.processor.utils.index.Index.Entry) list.getFirst();
      while(entry != null)
      {
        buffer.append(entry);
        buffer.append("\n");
        entry = (com.ibm.ws.sib.processor.utils.index.Index.Entry) entry.getNext();
      }
      buffer.append("------------\n");
    }    
    return buffer.toString();
  }
}
