/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.jandex.internal;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
// import java.util.Spliterator;

/**
 * An un-modifiable singleton list implementation.
 */
public class Singleton<T> implements List<T> {

    public Singleton(T value) {
        this.value = value;
    }

    //

    private final T value;

    public T getValue() {
        return value;
    }

    //

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        T useValue = getValue();
        if ( useValue == null ) {
            return (o == null);
        } else {
            return useValue.equals(0);
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private final T iteratorValue = Singleton.this.getValue();
            private boolean afterFirst;

            @Override
            public boolean hasNext() {
                return !afterFirst;
            }

            @Override
            public T next() {
                if ( afterFirst ) {
                    throw new NoSuchElementException(); 
                }
                afterFirst = true;
                return iteratorValue;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Object[] toArray() {
        return new Object[] { getValue() };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E[] toArray(E[] a) {
        if (a.length == 0 ) {
            Class<?> componentType = a.getClass().getComponentType();
            if ( componentType == Object.class ) {
                return (E[]) new Object[] { getValue() };
            } else {
                Object[] newArray = (Object[]) Array.newInstance(componentType, 1);
                newArray[0] = getValue();
                return (E[]) newArray;
            }
        } else {
            a[0] = (E) getValue();
            if ( a.length > 1 ) {
                a[1] = null;
            }
            return a;
        }
    }

    @Override
    public boolean add(T e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for ( Object o : c ) {
            if ( !contains(o) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T get(int index) {
        if ( index == 0 ) {
            return getValue();
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        T useValue = getValue();
        if ( useValue == null ) {
            return ( (o == null) ? 0 : -1 );
        } else {
            return ( useValue.equals(o) ? 0 : -1 );
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(0);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new ListIterator<T>() {
            private final T iteratorValue = Singleton.this.getValue();
            private boolean afterFirst;

            @Override
            public boolean hasNext() {
                return !afterFirst;
            }

            @Override
            public T next() {
                if ( afterFirst ) {
                    throw new NoSuchElementException(); 
                }
                afterFirst = true;
                return iteratorValue;
            }

            @Override
            public boolean hasPrevious() {
                return afterFirst;
            }

            @Override
            public T previous() {
                if ( !afterFirst ) {
                    throw new NoSuchElementException(); 
                }
                afterFirst = false;
                return iteratorValue;
            }

            @Override
            public int nextIndex() {
                return ( !afterFirst ? 0 : 1 );
            }

            @Override
            public int previousIndex() {
                return ( afterFirst ? 0 : -1 );
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(T e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(T e) {
                throw new UnsupportedOperationException();
            }
            
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListIterator<T> listIterator(int index) {
        if ( index == 0 ) {
            return listIterator();
        } else {
            return (ListIterator<T>) Collections.emptyIterator();
        }
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        if ( (fromIndex == 0) && (toIndex == 1) ) {
            return this;
        } else {
            return Collections.emptyList();
        }
    }

// Added by java8.
//    @Override
//    public Spliterator<T> spliterator() {
//        return List.super.spliterator();
//    }
}
