/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.internal;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * An set for strings based on string identity.
 *
 * Based on timing tests, the standard implementation uses
 * an IdentityHashMap for storage.  This is based on two observations:
 *
 * 1) The map based implementation is faster for lookups and additions
 *    which must test for membership, and much faster when the set has
 *    more than about 256 elements (see the table below).
 *
 * 2) When the set is small the extra memory overhead is correspondingly
 *    small.
 *
 * <pre>
 *  Power  Count        Ratio(A/M)   Array (Avg Time) (Total Time)  Map (Avg Time) (Total Time)
 * -----------------------------------------------------------------------------------------------
 *  [ 00 ] [ 00000001 ] [ 1.61 ]     [ 00160848 ] [ 000.000160848 ] [ 00100058 ] [ 000.000100058 ]
 *  [ 01 ] [ 00000002 ] [ 1.31 ]     [ 00111575 ] [ 000.000223150 ] [ 00084955 ] [ 000.000169910 ]
 *  [ 02 ] [ 00000004 ] [ 1.19 ]     [ 00070041 ] [ 000.000280164 ] [ 00058808 ] [ 000.000235232 ]
 *  [ 03 ] [ 00000008 ] [ 1.10 ]     [ 00042147 ] [ 000.000337178 ] [ 00038371 ] [ 000.000306972 ]
 *  [ 04 ] [ 00000016 ] [ 1.07 ]     [ 00024896 ] [ 000.000398346 ] [ 00023339 ] [ 000.000373426 ]
 *  [ 05 ] [ 00000032 ] [ 1.02 ]     [ 00014230 ] [ 000.000455362 ] [ 00013946 ] [ 000.000446300 ]
 *  [ 06 ] [ 00000064 ] [ 1.07 ]     [ 00009014 ] [ 000.000576942 ] [ 00008448 ] [ 000.000540694 ]
 *  [ 07 ] [ 00000128 ] [ 1.33 ]     [ 00007085 ] [ 000.000906948 ] [ 00005327 ] [ 000.000681910 ]
 *  [ 08 ] [ 00000256 ] [ 1.80 ]     [ 00006508 ] [ 000.001666264 ] [ 00003619 ] [ 000.000926582 ]
 *  [ 09 ] [ 00000512 ] [ 2.88 ]     [ 00007723 ] [ 000.003954404 ] [ 00002679 ] [ 000.001372128 ]
 *  [ 10 ] [ 00001024 ] [ 6.11 ]     [ 00013275 ] [ 000.013594044 ] [ 00002171 ] [ 000.002223950 ]
 *  [ 11 ] [ 00002048 ] [ 2.37 ]     [ 00007557 ] [ 000.015477418 ] [ 00003193 ] [ 000.006540834 ]
 *  [ 12 ] [ 00004096 ] [ 2.58 ]     [ 00005394 ] [ 000.022094524 ] [ 00002090 ] [ 000.008561268 ]
 *  [ 13 ] [ 00008192 ] [ 2.52 ]     [ 00004359 ] [ 000.035714622 ] [ 00001730 ] [ 000.014174386 ]
 *  [ 14 ] [ 00016384 ] [ 3.63 ]     [ 00005846 ] [ 000.095795296 ] [ 00001608 ] [ 000.026355524 ]
 *  [ 15 ] [ 00032768 ] [ 9.12 ]     [ 00010082 ] [ 000.330385180 ] [ 00001105 ] [ 000.036234928 ]
 *  [ 16 ] [ 00065536 ] [ 20.32 ]    [ 00019872 ] [ 001.302333562 ] [ 00000978 ] [ 000.064099644 ]
 *  [ 17 ] [ 00131072 ] [ 22.20 ]    [ 00038553 ] [ 005.053261184 ] [ 00001736 ] [ 000.227662784 ]
 * -----------------------------------------------------------------------------------------------
 * </pre>
 */
public class UtilImpl_IdentityStringSet implements Set<String> {
    public UtilImpl_IdentityStringSet() {
        super();

        this.storage = new IdentityHashMap<String, String>();
    }

    public UtilImpl_IdentityStringSet(int initialStorage) {
        super();

        this.storage = new IdentityHashMap<String, String>(initialStorage);
    }

    //

    protected final IdentityHashMap<String, String> storage;

    @Trivial
    protected IdentityHashMap<String, String> getStorage() {
        return storage;
    }

    //

    @Override
    public int size() {
        return getStorage().size();
    }

    @Override
    public boolean isEmpty() {
        return ( getStorage().isEmpty() );
    }

    //

    @Override
    public boolean contains(Object candidateElement) {
        return getStorage().containsKey(candidateElement);
    }

    @Override
    public boolean containsAll(Collection<?> candidateElements) {
        IdentityHashMap<String, String> useStorage = getStorage();
        for ( Object candidateElement : candidateElements ) {
            if ( !useStorage.containsKey(candidateElement) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<String> iterator() {
        return getStorage().keySet().iterator();
    }

    @Override
    @Trivial
    public Object[] toArray() {
        return getStorage().keySet().toArray();
    }

    @Override
    @Trivial
    public <S> S[] toArray(S[] targetArray) {
        return getStorage().keySet().toArray(targetArray);
    }

    //

    @Override
    public boolean add(String i_newElement) {
        return ( getStorage().put(i_newElement, i_newElement) == null );
    }

    @Override
    public boolean addAll(Collection<? extends String> i_newElements) {
        boolean didAddAny = false;
        IdentityHashMap<String, String> useStorage = getStorage();
        for ( String i_newElement : i_newElements ) {
            if ( useStorage.put(i_newElement, i_newElement) != null ) {
                didAddAny = true;
            }
        }
        return didAddAny;
    }

    /**
     * Remove an elements from this set.
     *
     * Identify semantics are used to test for membership of the
     * candidate element.
     *
     * @param candidateElement The element of this set which is to be
     *     removed.
     *
     * @return True or false telling if the element was removed.
     */
    @Override
    public boolean remove(Object candidateElement) {
        return ( getStorage().remove(candidateElement) != null );
    }

    /**
     * Remove the elements of a specified set from this set.
     *
     * Identify semantics are used to test for membership of the
     * candidate elements.
     *
     * @param candidateElements Elements of this set which are to be
     *     removed.
     *
     * @return True or false telling if any elements were removed.
     */
    @Override
    public boolean removeAll(Collection<?> candidateElements) {
        boolean didRemoveAny = false;
        IdentityHashMap<String, String> useStorage = getStorage();
        for ( Object candidateElement : candidateElements ) {
            if ( useStorage.remove(candidateElement) != null ) {
                didRemoveAny = true;
            }
        }
        return didRemoveAny;
    }

    /**
     * Remove all elements of this set except those which are
     * contained by a specified set.
     *
     * The semantics of containment are based on the semantics
     * of {@link #contains} as implemented by the 'candidateElements'
     * parameter.
     *
     * @param candidateElements Elements of this set which are to be
     *     retained.
     */
    @Override
    public boolean retainAll(Collection<?> candidateElements) {
        boolean didOmitAny = false;
        Iterator<String> i_elements = getStorage().keySet().iterator();
        while ( i_elements.hasNext() ) {
            String i_element = i_elements.next();
            if ( !candidateElements.contains(i_element) ) {
                i_elements.remove();
                didOmitAny = true;
            }
        }
        return didOmitAny;
    }

    @Override
    public void clear() {
        getStorage().clear();
    }

    //

    public boolean i_equals(Set<String> i_others) {
        if ( i_others == null ) {
            return false;
        } else if ( i_others == this ) {
            return true;
        } else if ( i_others.size() != size() ) {
            return false;
        } else {
            return containsAll(i_others);
        }
    }
}
