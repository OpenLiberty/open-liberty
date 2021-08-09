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
package com.ibm.ws.sib.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * <p>An implementation of the map interface that removes the entries from the
 *   map after a defined period of time.
 * </p>
 * 
 * <p>It should be noted that this Map implementation is not thread safe or 
 *   serializable and the methods returning collections do not expire the 
 *   content even if a call to a get method would result in null being 
 *   returned.
 * </p>
 *
 * <p>SIB build component: sib.security.impl</p>
 *
 * @author nottinga
 * @version 1.3
 * @param <K> The type of the Key in the map.
 * @param <V> The type of the value in the map.
 * @since 1.0
 */
public class TimedMap<K,V> implements Map<K,V>
{
  /** The serial version UID for this class */
  private static final long serialVersionUID = 4496813777386763606L;
  /** The trace component */
  private static final TraceComponent _tc = SibTr.register(TimedMap.class, 
      TraceGroups.TRGRP_UTILS, null);
  /** How long the value should be kept for */
  private long _timeout;
  /** The underlying map */
  private Map<K,TimedValue<V>> _realMap = new HashMap<K,TimedValue<V>>();
  
  /**
   * <p>This class holds the value and works out if the value has expired.</p>
   * @param <V> The type of the value held by this TimedValue
   */
  private static final class TimedValue<V>
  {
    /** The time the value should expire */
    private long _expireTime;
    /** The value of the object in the map */
    private V _value; 
    /** the trace component */
    private static final TraceComponent _tc2 = SibTr.register(TimedValue.class, 
        null, null);
    
    /* ------------------------------------------------------------------------ */
    /* TimedValue method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @param obj     the object to be represented.
     * @param timeout the timeout value.
     */
    public TimedValue(V obj, long timeout)
    {
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.entry(this, _tc2, "TimedValue", new Object[]{obj, Long.valueOf(timeout)});
      set(obj, timeout);
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.exit(this, _tc2, "TimedValue", this);
    }
    
    /* ------------------------------------------------------------------------ */
    /* get method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @return the underlying object
     */
    public V get()
    {
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.entry(this, _tc2, "get");
      V value = null;
      
      if (!hasExipred())
      {
        value = _value;
      }
      
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.exit(this, _tc2, "get", value);
      return value;
    }
    
    /* ------------------------------------------------------------------------ */
    /* set method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * Sets the value and resets the timeout.
     * @param obj     The object to set.
     * @param timeout How long until the object should expire.
     */
    public void set(V obj, long timeout)
    {
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.entry(this, _tc2, "set", new Object[]{obj, Long.valueOf(timeout)});
      _value = obj;
      _expireTime = timeout + System.currentTimeMillis();
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.exit(this, _tc2, "set");
    }
    
    /* ------------------------------------------------------------------------ */
    /* hasExipred method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @return true if the value has expired and null should be returned. 
     */
    public boolean hasExipred()
    {
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.entry(this, _tc2, "hasExipred");
      long currentTime = System.currentTimeMillis();
      
      boolean result = (currentTime > _expireTime);
      
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.exit(this, _tc2, "hasExipred", Boolean.valueOf(result));
      return result;
    }
    
    /* ------------------------------------------------------------------------ */
    /* equals method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj)
    {
      if (obj == null) return false;
      if (obj == this) return true;
      if (obj instanceof TimedValue)
      {
        return _value.equals(((TimedValue)obj)._value);
      }
      
      return false;
    }
    
    /* ------------------------------------------------------------------------ */
    /* hashCode method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
      return _value.hashCode();
    }
  }
  
  /**
   * <p>The map entry for the Map.</p>
   * @param <K> The type of the key.
   * @param <V> The type of the value.
   */
  public static final class TimedMapEntry<K,V> implements Map.Entry<K,V>
  {
    /** The key for the map */
    private K _key;
    /** The timed value in the entry */
    private TimedValue<V> _value;
    /** The timeout period for entries in the map */
    private long _timeout;
    
    /** A prime */
    private static final int PRIME = 10003;
    /** The trace component */
    private static final TraceComponent _tc2 = SibTr.register(TimedMapEntry.class, 
        null, null);
    
    /* ------------------------------------------------------------------------ */
    /* TimedMapEntry method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * Constructs the map entry
     * 
     * @param key     The key
     * @param timeout The timeout period
     * @param value   The value.
     */
    public TimedMapEntry(K key, long timeout, TimedValue<V> value)
    {
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.entry(this, _tc2, "TimedMapEntry", new Object[]{key, Long.valueOf(timeout), value});
      _key = key;
      _value = value;
      _timeout = timeout;
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.exit(this, _tc2, "TimedMapEntry", this);
    }
    
    /* ------------------------------------------------------------------------ */
    /* getKey method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @see Map.Entry#getKey()
     */
    public K getKey()
    {
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.entry(this, _tc2, "getKey");
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.exit(this, _tc2, "getKey", _key);
      return _key;
    }

    /* ------------------------------------------------------------------------ */
    /* getValue method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @see Map.Entry#getValue()
     */
    public V getValue()
    {
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.entry(this, _tc2, "getValue");
      V result = _value.get();
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.exit(this, _tc2, "getValue", result);
      return result;
    }

    /* ------------------------------------------------------------------------ */
    /* setValue method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @param value The value in the map entry.
     * @return the previous value.
     * @see Map.Entry#setValue(Object)
     */
    public V setValue(V value)
    {
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.entry(this, _tc2, "setValue", value);
      V oldValue = _value.get();
      _value.set(value, _timeout);
      if (TraceComponent.isAnyTracingEnabled() && _tc2.isEntryEnabled()) SibTr.exit(this, _tc2, "setValue", oldValue);
      return oldValue;
    }
    
    /* ------------------------------------------------------------------------ */
    /* equals method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @see Map.Entry#equals(Object)
     */
    public boolean equals(Object obj)
    {
      if (obj == null) return false;
      if (obj == this) return true;
      if (obj instanceof TimedMapEntry)
      {
        TimedMapEntry entry = (TimedMapEntry)obj;
        
        if (_key.equals(entry.getKey()))
        {
          Object thisValue = _value.get();
          Object otherValue = entry._value.get();
          
          if (thisValue == null && otherValue == null) return true;
          if (thisValue == null || otherValue == null) return false;
          if (thisValue == otherValue) return true;
          return thisValue.equals(otherValue);
        }
      }
      return false;
    }
    
    /* ------------------------------------------------------------------------ */
    /* hashCode method                                    
    /* ------------------------------------------------------------------------ */
    /**
     * @see Map.Entry#hashCode()
     */
    public int hashCode()
    {
      int hash = _key.hashCode();
      hash = (hash * PRIME) + _value.hashCode();
      return hash; 
    }
  }
  
  /* ------------------------------------------------------------------------ */
  /* TimedHashMap method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * The constructor.
   * 
   * @param timeout how long the entries should remain.
   */
  public TimedMap(long timeout)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "TimedMap", Long.valueOf(timeout));
    _timeout = timeout;
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "TimedMap", this);
  }

  /* ------------------------------------------------------------------------ */
  /* get method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see Map#get(Object)
   */
  public V get(Object arg0)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "get", arg0);
    TimedValue<V> obj = _realMap.get(arg0);
    V value = null;
    
    if (obj != null)
    {
      if (obj.hasExipred())
      {
        _realMap.remove(arg0);
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled())
        {
          SibTr.debug(_tc, "The value with the key " + arg0 + " has expired");
        }
      }
      else
      {
        value = obj.get();
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "get", value);
    return value;
  }

  /* ------------------------------------------------------------------------ */
  /* put method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @param arg0 The key
   * @param arg1 The value
   * @return The previous value bound with the key.
   * @see Map#put(Object, Object)
   */
  public V put(K arg0, V arg1)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "put", new Object[]{arg0, arg1});
    TimedValue<V> newValue = new TimedValue<V>(arg1, _timeout);
    TimedValue<V> oldValue = _realMap.put(arg0, newValue);
    
    V value = null;
    if (oldValue != null && !oldValue.hasExipred())
    {
      value = oldValue.get();
    }
   
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "put", value);
    return value;
  }

  /* ------------------------------------------------------------------------ */
  /* putAll method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see Map#putAll(Map)
   */
  public void putAll(Map<? extends K, ? extends V> arg0)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "putAll", arg0);
    
    for (Map.Entry<? extends K,? extends V> entry : arg0.entrySet())
    {
      put(entry.getKey(), entry.getValue());
      
    }
    
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "putAll");
  }

  /* ------------------------------------------------------------------------ */
  /* values method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method is NOT supported and always throws UnsupportedOperationException
   * 
   * @see Map#values()
   */
  public Collection<V> values()
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "values");
    
    UnsupportedOperationException uoe = new UnsupportedOperationException();
    
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "values", uoe);
    throw uoe;
  }

  /* ------------------------------------------------------------------------ */
  /* entrySet method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method is NOT supported and always throws UnsupportedOperationException
   * 
   * @see Map#entrySet()
   */
  public Set<Map.Entry<K,V>> entrySet()
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "entrySet");
    
    UnsupportedOperationException uoe = new UnsupportedOperationException();
    
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "entrySet", uoe);
    throw uoe;
  }

  /* ------------------------------------------------------------------------ */
  /* containsValue method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see Map#containsValue(Object)
   */
  public boolean containsValue(Object value)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "containsValue", value);
    purge();
    
    boolean result = false;
    
    result = _realMap.containsValue(new TimedValue<Object>(value, _timeout));
    
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "containsValue", Boolean.valueOf(result));
    return result;
  }

  /* ------------------------------------------------------------------------ */
  /* remove method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see Map#remove(Object)
   */
  public V remove(Object key)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "remove", key);
    TimedValue<V> value = _realMap.remove(key);
    V deadValue = null;
    
    if (value != null)
    {
      deadValue = value.get();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "remove", deadValue);
    return deadValue;
  }

  /* ------------------------------------------------------------------------ */
  /* clear method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see Map#clear()
   */
  public void clear()
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "clear");
    _realMap.clear();
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "clear");
  }

  /* ------------------------------------------------------------------------ */
  /* containsKey method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see Map#containsKey(Object)
   */
  public boolean containsKey(Object key)
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "containsKey", key);
    purge();
    boolean result = _realMap.containsKey(key);
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "containsKey", Boolean.valueOf(result));
    return result;
  }

  /* ------------------------------------------------------------------------ */
  /* isEmpty method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see Map#isEmpty()
   */
  public boolean isEmpty()
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "isEmpty");
    purge();
    boolean result = _realMap.isEmpty();
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "isEmpty", Boolean.valueOf(result));
    return result;
  }

  /* ------------------------------------------------------------------------ */
  /* keySet method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method is NOT supported and always throws UnsupportedOperationException
   * 
   * @see Map#keySet()
   */
  public Set<K> keySet()
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "keySet");
    
    UnsupportedOperationException uoe = new UnsupportedOperationException();
    
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "keySet", uoe);
    throw uoe;
  }

  /* ------------------------------------------------------------------------ */
  /* size method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @see java.util.Map#size()
   */
  public int size()
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "size");
    purge();
    int size = _realMap.size();
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "size", Integer.valueOf(size));
    return size;
  }
  
  /* ------------------------------------------------------------------------ */
  /* purge method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * This method removes any expired data from the map. 
   */
  private void purge()
  {
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.entry(this, _tc, "purge");

    Iterator<Map.Entry<K,TimedValue<V>>> it = _realMap.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry<K,TimedValue<V>> entry = it.next();
      TimedValue<V> value = entry.getValue();
      if (value.hasExipred())
      {
        it.remove();
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled())
        {
          SibTr.debug(_tc, "The value with the key " + entry.getKey() + " has expired");
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled()) SibTr.exit(this, _tc, "purge");
  }

  /* ------------------------------------------------------------------------ */
  /* getTimeout method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Get the timeout value for the TimedMap
   * 
   * @return the timeout value
   */
  public long getTimeout() {
    return _timeout;
  }

  /* ------------------------------------------------------------------------ */
  /* setTimeout method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * Set the timeout value for the TimedMap
   * 
   * @param the new timeout value
   */
  public void setTimeout(long timeout) {
    _timeout = timeout;
  }

}
