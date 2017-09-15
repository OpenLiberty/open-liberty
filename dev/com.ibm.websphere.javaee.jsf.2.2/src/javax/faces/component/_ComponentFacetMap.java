/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.component;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class _ComponentFacetMap<V extends UIComponent> implements Map<String, V>, Serializable
{
    private static final long serialVersionUID = -3456937594422167629L;
    private UIComponent _component;
    private Map<String, V> _map = new _ArrayMap<String, V>(0,5);
    private Set<Entry<String, V>> _entrySet = null;
    private Set<String> _keySet = null;
    private Collection<V> _valueCollection = null;

    _ComponentFacetMap(UIComponent component)
    {
        _component = component;
    }

    public int size()
    {
        return _map.size();
    }

    public void clear()
    {
        UIComponent[] values = _map.values().toArray(new UIComponent[_map.size()]);
        //remove all elements from underlying map
        _map.clear();
        //Set parent to null
        for (int i = 0; i < values.length; i++)
        {
            values[i].setParent(null);
        }
    }

    public boolean isEmpty()
    {
        return _map.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        checkKey(key);
        return _map.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        checkValue(value);
        return _map.containsValue(value);
    }

    public Collection<V> values()
    {
        if (_valueCollection == null)
        {
            _valueCollection= new ComponentFacetValueCollection();
        }
        return _valueCollection;
    }

    public void putAll(Map<? extends String, ? extends V> t)
    {
        for (Map.Entry<? extends String, ? extends V> entry : t.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    public Set<Entry<String, V>> entrySet()
    {
        if (_entrySet == null)
        {
            _entrySet = new ComponentFacetEntrySet();
        }
        return _entrySet;
    }

    public Set<String> keySet()
    {
        if (_keySet == null)
        {
            _keySet = new ComponentFacetKeySet();
        }
        return _keySet;
    }

    public V get(Object key)
    {
        checkKey(key);
        return _map.get(key);
    }

    public V remove(Object key)
    {
        checkKey(key);
        V facet = _map.remove(key);
        if (facet != null)
        {
            facet.setParent(null);
        }
        return facet;
    }

    public V put(String key, V value)
    {
        //checkKey(key);
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        //checkValue(value);
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        setNewParent(key, value);
        V previousValue = _map.put(key, value);
        if (previousValue != null)
        {
            previousValue.setParent(null);
        }
        return previousValue; 
    }

    private void setNewParent(String facetName, UIComponent facet)
    {
        UIComponent oldParent = facet.getParent();
        if (oldParent != null)
        {
            if (!oldParent.getChildren().remove(facet))
            {
                // Check if the component is inside a facet and remove from there
                if (oldParent.getFacetCount() > 0)
                {
                    for (Iterator< Map.Entry<String, UIComponent > > it = 
                        oldParent.getFacets().entrySet().iterator() ; it.hasNext() ; )
                    {
                        Map.Entry<String, UIComponent > entry = it.next();
                        
                        if (entry.getValue().equals(facet))
                        {
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }
        facet.setParent(_component);
    }

    private void checkKey(Object key)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        if (!(key instanceof String))
        {
            throw new ClassCastException("key is not a String");
        }
    }

    private void checkValue(Object value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        if (!(value instanceof UIComponent))
        {
            throw new ClassCastException("value is not a UIComponent");
        }
    }

    private class ComponentFacetEntrySet extends AbstractSet<Entry<String, V>>
    {
        public ComponentFacetEntrySet()
        {
        }
        
        public int size()
        {
            return _map.size();
        }

        public boolean isEmpty()
        {
            return _map.isEmpty();
        }

        public boolean contains(Object o)
        {
            return _map.entrySet().contains(o);
        }

        public Iterator<java.util.Map.Entry<String, V>> iterator()
        {
            return new ComponentFacetEntryIterator(_map.entrySet().iterator());
        }

        public Object[] toArray()
        {
            return _map.entrySet().toArray();
        }

        public <T> T[] toArray(T[] a)
        {
            return _map.entrySet().toArray(a);
        }

        public boolean add(java.util.Map.Entry<String, V> o)
        {
            // Add over the entry set is not allowed, because this should be done
            // through the outer Map instance.
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        public boolean remove(Object o)
        {
            if (_map.entrySet().remove(o))
            {
                if (o instanceof Map.Entry)
                {
                    Object value = ((Map.Entry<String, V>) o).getValue();
                    
                    if (value != null && value instanceof UIComponent)
                    {
                        ((UIComponent) value).setParent(null);
                    }
                }
                return true;
            }
            else
            {
                return false;
            }
        }

        public boolean containsAll(Collection<?> c)
        {
            return _map.entrySet().containsAll(c);
        }

        public boolean addAll(
                Collection<? extends java.util.Map.Entry<String, V>> c)
        {
            // Add over the entry set is not allowed, because this should be done
            // through the outer Map instance.
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj)
        {
            return _map.entrySet().equals(obj);
        }

        @Override
        public int hashCode()
        {
            return _map.entrySet().hashCode();
        }

        @Override
        public String toString()
        {
            return _map.entrySet().toString();
        }
    }
    
    private class ComponentFacetEntryIterator implements Iterator<Map.Entry<String, V>>
    {
        private Iterator<Map.Entry<String, V>> _delegate;
        private V _currentEntryValue;
        
        public ComponentFacetEntryIterator(Iterator<Map.Entry<String, V>> it)
        {
            _delegate = it;
            _currentEntryValue = null;
        }
        
        public boolean hasNext()
        {
            return _delegate.hasNext();
        }

        public java.util.Map.Entry<String, V> next()
        {
            java.util.Map.Entry<String, V> next = _delegate.next(); 
            _currentEntryValue = next.getValue();
            return new ComponentFacetEntry(next);
        }

        public void remove()
        {
            _delegate.remove();
            if (_currentEntryValue != null)
            {
                _currentEntryValue.setParent(null);
            }
        }
    }

    /**
     * Wrapper used to override setValue() method
     * 
     */
    private class ComponentFacetEntry implements Map.Entry<String, V>
    {
        private java.util.Map.Entry<String, V> _entry;
        
        public ComponentFacetEntry(java.util.Map.Entry<String, V> entry)
        {
            _entry = entry;
        }

        public String getKey()
        {
            return _entry.getKey();
        }

        public V getValue()
        {
            return _entry.getValue();
        }

        public V setValue(V value)
        {
            setNewParent(_entry.getKey(), value);
            V previousValue = _entry.setValue(value);
            if (previousValue != null)
            {
                previousValue.setParent(null);
            }
            return previousValue;
        }

        @Override
        public int hashCode()
        {
            return _entry.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return _entry.equals(obj);
        }

        @Override
        public String toString()
        {
            return _entry.toString();
        }
    }
    
    private class ComponentFacetKeySet extends AbstractSet<String>
    {

        public ComponentFacetKeySet()
        {
        }
        
        public int size()
        {
            return _map.keySet().size();
        }

        public boolean isEmpty()
        {
            return _map.keySet().isEmpty();
        }

        public boolean contains(Object o)
        {
            return _map.keySet().contains(o);
        }

        public Iterator<String> iterator()
        {
            // Iterate over entrySet is equals to iterate over keySet, but
            // in this case is better use entrySet iterator, because we can
            // get the value directly and call setParent(null) if the entry is
            // removed
            return new ComponentFacetKeyIterator(_map.entrySet().iterator());
        }

        public Object[] toArray()
        {
            return _map.keySet().toArray();
        }

        public <T> T[] toArray(T[] a)
        {
            return _map.keySet().toArray(a);
        }

        public boolean add(String o)
        {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o)
        {
            V previousValue = _map.get(o);
            if (_map.keySet().remove(o))
            {
                if (previousValue != null)
                {
                    ((UIComponent) previousValue).setParent(null);
                }
                return true;
            }
            else
            {
                return false;
            }
        }

        public boolean containsAll(Collection<?> c)
        {
            return _map.keySet().containsAll(c);
        }

        public boolean addAll(Collection<? extends String> c)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj)
        {
            return _map.keySet().equals(obj);
        }

        @Override
        public int hashCode()
        {
            return _map.keySet().hashCode();
        }

        @Override
        public String toString()
        {
            return _map.keySet().toString();
        }
    }
    
    private class ComponentFacetKeyIterator implements Iterator<String>
    {
        private Iterator<Map.Entry<String, V>> _delegate;
        private V _currentEntryValue;
        
        public ComponentFacetKeyIterator(Iterator<Map.Entry<String, V>> it)
        {
            _delegate = it;
            _currentEntryValue = null;
        }
        
        public boolean hasNext()
        {
            return _delegate.hasNext();
        }

        public String next()
        {
            java.util.Map.Entry<String, V> next = _delegate.next(); 
            _currentEntryValue = next.getValue();
            return next.getKey();
        }

        public void remove()
        {
            _delegate.remove();
            if (_currentEntryValue != null)
            {
                _currentEntryValue.setParent(null);
            }
        }
    }

    private class ComponentFacetValueCollection extends AbstractCollection<V>
    {
        public ComponentFacetValueCollection()
        {
        }

        public int size()
        {
            return _map.values().size();
        }

        public boolean isEmpty()
        {
            return _map.values().isEmpty();
        }

        public boolean contains(Object o)
        {
            return _map.values().contains(o);
        }

        public Iterator<V> iterator()
        {
            return new ComponentFacetValueIterator(_map.entrySet().iterator());
        }

        public Object[] toArray()
        {
            return _map.values().toArray();
        }

        public <T> T[] toArray(T[] a)
        {
            return _map.values().toArray(a);
        }

        public boolean add(V o)
        {
            // Add over the entry set is not allowed, because this should be done
            // through the outer Map instance.
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection<?> c)
        {
            return _map.values().containsAll(c);
        }

        public boolean addAll(Collection<? extends V> c)
        {
            // Add over the entry set is not allowed, because this should be done
            // through the outer Map instance.
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj)
        {
            return _map.values().equals(obj);
        }

        @Override
        public int hashCode()
        {
            return _map.values().hashCode();
        }

        @Override
        public String toString()
        {
            return _map.values().toString();
        }
    }
    
    private class ComponentFacetValueIterator implements Iterator<V>
    {
        private Iterator<Map.Entry<String, V>> _delegate;
        private V _currentEntryValue;
        
        public ComponentFacetValueIterator(Iterator<Map.Entry<String, V>> it)
        {
            _delegate = it;
            _currentEntryValue = null;
        }
        
        public boolean hasNext()
        {
            return _delegate.hasNext();
        }

        public V next()
        {
            java.util.Map.Entry<String, V> next = _delegate.next(); 
            _currentEntryValue = next.getValue();
            return next.getValue();
        }

        public void remove()
        {
            _delegate.remove();
            if (_currentEntryValue != null)
            {
                _currentEntryValue.setParent(null);
            }
        }
    }
}
