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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import javax.faces.context.FacesContext;

/**
 * This class handle deltas on facesListener and validatorList.
 * 
 * It is only used by this methods on UIComponentBase:
 * 
 * addFacesListener
 * broadcast
 * getFacesListeners
 * removeFacesListener
 * 
 * A facesListener could hold PartialStateHolder instances, so it 
 * is necessary to provide convenient methods to track deltas.
 */
class _DeltaList<T> implements List<T>, PartialStateHolder, RandomAccess
{

    private List<T> _delegate;
    private boolean _initialStateMarked;
    
    public _DeltaList()
    {
    }
    
    public _DeltaList(List<T> delegate)
    {
        _delegate = delegate;
    }
    
    public void add(int index, T element)
    {
        clearInitialState();
        _delegate.add(index, element);
    }

    public boolean add(T e)
    {
        clearInitialState();
        return _delegate.add(e);
    }

    public boolean addAll(Collection<? extends T> c)
    {
        clearInitialState();
        return _delegate.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends T> c)
    {
        clearInitialState();
        return _delegate.addAll(index, c);
    }

    public void clear()
    {
        clearInitialState();
        _delegate.clear();
    }

    public boolean contains(Object o)
    {
        return _delegate.contains(o);
    }

    public boolean containsAll(Collection<?> c)
    {
        return _delegate.containsAll(c);
    }

    public boolean equals(Object o)
    {
        return _delegate.equals(o);
    }

    public T get(int index)
    {
        return _delegate.get(index);
    }

    public int hashCode()
    {
        return _delegate.hashCode();
    }

    public int indexOf(Object o)
    {
        return _delegate.indexOf(o);
    }

    public boolean isEmpty()
    {
        return _delegate.isEmpty();
    }

    public Iterator<T> iterator()
    {
        return _delegate.iterator();
    }

    public int lastIndexOf(Object o)
    {
        return _delegate.lastIndexOf(o);
    }

    public ListIterator<T> listIterator()
    {
        return _delegate.listIterator();
    }

    public ListIterator<T> listIterator(int index)
    {
        return _delegate.listIterator(index);
    }

    public T remove(int index)
    {
        clearInitialState();
        return _delegate.remove(index);
    }

    public boolean remove(Object o)
    {
        clearInitialState();
        return _delegate.remove(o);
    }

    public boolean removeAll(Collection<?> c)
    {
        clearInitialState();
        return _delegate.removeAll(c);
    }

    public boolean retainAll(Collection<?> c)
    {
        clearInitialState();
        return _delegate.retainAll(c);
    }

    public T set(int index, T element)
    {
        clearInitialState();
        return _delegate.set(index, element);
    }

    public int size()
    {
        return _delegate == null ? 0 : _delegate.size();
    }

    public List<T> subList(int fromIndex, int toIndex)
    {
        return _delegate.subList(fromIndex, toIndex);
    }

    public Object[] toArray()
    {
        return _delegate.toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        return _delegate.toArray(a);
    }

    public boolean isTransient()
    {
        return false;
    }

    public void setTransient(boolean newTransientValue)
    {
        throw new UnsupportedOperationException();
    }

    public void restoreState(FacesContext context, Object state)
    {
        if (state == null)
        {
            return;
        }
        
        if (initialStateMarked())
        {            
            //Restore delta
            Object[] lst = (Object[]) state;
            int j = 0;
            int i = 0;
            while (i < lst.length)
            {
                if (lst[i] instanceof _AttachedDeltaWrapper)
                {
                    //Delta
                    ((StateHolder)_delegate.get(j)).restoreState(context,
                            ((_AttachedDeltaWrapper) lst[i]).getWrappedStateObject());
                    j++;
                }
                else if (lst[i] != null)
                {
                    //Full
                    _delegate.set(j, (T) UIComponentBase.restoreAttachedState(context, lst[i]));
                    j++;
                }
                else
                {
                    _delegate.remove(j);
                }
                i++;
            }
            if (i != j)
            {
                // StateHolder transient objects found, next time save and restore it fully
                //because the size of the list changes.
                clearInitialState();
            }
        }
        else
        {
            //Restore delegate
            Object[] lst = (Object[]) state;
            _delegate = new ArrayList<T>(lst.length);
            for (int i = 0; i < lst.length; i++)
            {
                T value = (T) UIComponentBase.restoreAttachedState(context, lst[i]);
                if (value != null)
                {
                    _delegate.add(value);
                }
            }
        }
    }

    public Object saveState(FacesContext context)
    {
        if (initialStateMarked())
        {
            Object [] lst = new Object[_delegate.size()];
            boolean nullDelta = true;
            for (int i = 0; i < _delegate.size(); i++)
            {
                Object value = _delegate.get(i);
                if (value instanceof PartialStateHolder)
                {
                    //Delta
                    PartialStateHolder holder = (PartialStateHolder) value;
                    if (!holder.isTransient())
                    {
                        Object attachedState = holder.saveState(context);
                        if (attachedState != null)
                        {
                            nullDelta = false;
                        }
                        lst[i] = new _AttachedDeltaWrapper(value.getClass(),
                            attachedState);
                    }
                }
                else
                {
                    //Full
                    lst[i] = UIComponentBase.saveAttachedState(context, value);
                    if (value instanceof StateHolder || value instanceof List)
                    {
                        nullDelta = false;
                    }
                }
            }
            if (nullDelta)
            {
                return null;
            }
            return lst;
        }
        else
        {
            Object [] lst = new Object[_delegate.size()];
            for (int i = 0; i < _delegate.size(); i++)
            {
                lst[i] = UIComponentBase.saveAttachedState(context, _delegate.get(i));
            }
            return lst;
        }
    }

    public void clearInitialState()
    {
        //Reset delta setting to null
        if (_initialStateMarked)
        {
            _initialStateMarked = false;
            if (_delegate != null)
            {
                for (T value : _delegate)
                {
                    if (value instanceof PartialStateHolder)
                    {
                        ((PartialStateHolder)value).clearInitialState();
                    }
                }
            }
        }
    }

    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    public void markInitialState()
    {
        _initialStateMarked = true;
        if (_delegate != null)
        {
            int size = _delegate.size();
            for (int i = 0; i < size; i++)
            {
                T value = _delegate.get(i);
                if (value instanceof PartialStateHolder)
                {
                    ((PartialStateHolder)value).markInitialState();
                }
            }
        }
    }
}
